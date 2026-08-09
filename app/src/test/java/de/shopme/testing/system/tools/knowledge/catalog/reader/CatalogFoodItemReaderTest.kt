package de.shopme.testing.system.tools.knowledge.catalog.reader

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class CatalogFoodItemReaderTest {

    private val reader = CatalogFoodItemReader()

    private val categoryRegistry =
        CanonicalFoodCategoryRegistry()

    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .serializeNulls()
            .create()

    @Test
    fun readCatalogForCanonicalizationAudit() {
        val catalogFile = resolveCatalogFile()

        val entries = reader.read(catalogFile)

        assertTrue(
            entries.isNotEmpty(),
            "Catalog audit input must contain at least one entry."
        )

        validateSourceIndices(entries)
        validateItemNames(entries)
        validateListFields(entries)
        validateReaderOrder(entries)
        validateJsonEntryCount(
            catalogFile = catalogFile,
            entries = entries
        )
    }

    @Test
    fun readCanonicalCatalogDeterministically() {
        val catalogFile = resolveCatalogFile()

        val firstRead = reader.read(catalogFile)
        val secondRead = reader.read(catalogFile)
        val thirdRead = reader.read(catalogFile)

        assertEquals(
            firstRead,
            secondRead,
            "Reading the same catalog twice must produce equal results."
        )

        assertEquals(
            firstRead,
            thirdRead,
            "Repeated catalog reads must remain deterministic."
        )

        assertContentEquals(
            firstRead.map { it.sourceIndex },
            secondRead.map { it.sourceIndex },
            "Repeated reads must preserve source-index order."
        )

        assertContentEquals(
            firstRead.map { it.item.itemname },
            secondRead.map { it.item.itemname },
            "Repeated reads must preserve item-name order."
        )

        assertContentEquals(
            firstRead.map { it.item.normalized },
            secondRead.map { it.item.normalized },
            "Repeated reads must preserve normalized-key order."
        )
    }

    @Test
    fun preserveCatalogOrder() {
        val catalogFile = resolveCatalogFile()

        val entries = reader.read(catalogFile)

        val expectedIndices = entries.indices.toList()
        val actualIndices = entries.map { it.sourceIndex }

        assertEquals(
            expectedIndices,
            actualIndices,
            "sourceIndex values must represent the original JSON array order."
        )

        val secondRead = reader.read(catalogFile)

        assertContentEquals(
            entries.map { it.item },
            secondRead.map { it.item },
            "Reader must preserve catalog item order across repeated reads."
        )
    }

    @Test
    fun matchNumberOfJsonEntries() {
        val catalogFile = resolveCatalogFile()

        val entries = reader.read(catalogFile)
        val jsonEntryCount = readJsonArray(catalogFile).size()

        assertEquals(
            jsonEntryCount,
            entries.size,
            "Reader result size must equal the number of objects in catalog.json."
        )
    }

    @Test
    fun preserveDuplicateNormalizedKeysForAudit() {
        val catalogFile = resolveCatalogFile()

        val entries = reader.read(catalogFile)

        val duplicateNormalizedKeys = entries
            .asSequence()
            .mapNotNull { entry ->
                entry.item.normalized
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?.let { normalizedKey ->
                        normalizedKey to entry.sourceIndex
                    }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .filterValues { sourceIndices ->
                sourceIndices.size > 1
            }

        /*
         * Der Reader darf Dubletten weder entfernen noch zusammenführen.
         * Sie müssen vollständig an CatalogDuplicateDetector und
         * CatalogQualityValidator weitergereicht werden.
         */
        duplicateNormalizedKeys.forEach {
                (normalizedKey, sourceIndices) ->

            sourceIndices.forEach { sourceIndex ->
                val entry = entries.single {
                    it.sourceIndex == sourceIndex
                }

                assertEquals(
                    normalizedKey,
                    entry.item.normalized?.trim(),
                    "Reader changed duplicate normalized key at " +
                            "sourceIndex $sourceIndex."
                )
            }
        }

        assertEquals(
            entries.size,
            entries.map { it.sourceIndex }.distinct().size,
            "Duplicate normalized keys must not cause entry loss."
        )
    }

    @Test
    fun rejectDuplicateSourceIndices() {
        val catalogFile = resolveCatalogFile()

        val entries = reader.read(catalogFile)

        val duplicateSourceIndices = entries
            .groupBy { it.sourceIndex }
            .filterValues { it.size > 1 }
            .toSortedMap()

        assertTrue(
            duplicateSourceIndices.isEmpty(),
            buildString {
                appendLine(
                    "Reader returned duplicate sourceIndex values."
                )

                duplicateSourceIndices.forEach {
                        (sourceIndex, matchingEntries) ->
                    appendLine(
                        "- $sourceIndex: " +
                                matchingEntries.joinToString {
                                    it.item.itemname
                                }
                    )
                }
            }
        )
    }

    @Test
    fun rejectEmptyItemNames() {
        val catalogFile = resolveCatalogFile()

        val entries = reader.read(catalogFile)

        val invalidEntries = entries
            .filter { entry ->
                entry.item.itemname.isBlank()
            }

        assertTrue(
            invalidEntries.isEmpty(),
            buildString {
                appendLine(
                    "Canonical catalog contains blank item names."
                )

                invalidEntries.forEach { entry ->
                    appendLine("- sourceIndex=${entry.sourceIndex}")
                }
            }
        )
    }

    @Test
    fun preserveUnknownCategoriesForAudit() {
        val catalogFile = resolveCatalogFile()

        val entries = reader.read(catalogFile)

        val unknownCategoryEntries = entries.filter { entry ->
            val category = entry.item.category
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: return@filter false

            !categoryRegistry.contains(category)
        }

        /*
         * Unbekannte Kategorien sind ein Audit-Befund. Der Reader darf sie
         * nicht verwerfen, umschreiben oder still auf eine andere Kategorie
         * abbilden.
         */
        unknownCategoryEntries.forEach { entry ->
            val originalCategory = entry.item.category

            assertTrue(
                !originalCategory.isNullOrBlank(),
                "Unknown-category audit entry must retain its original value."
            )

            assertEquals(
                originalCategory,
                entries.single {
                    it.sourceIndex == entry.sourceIndex
                }.item.category,
                "Reader changed category at sourceIndex " +
                        "${entry.sourceIndex}."
            )
        }

        assertEquals(
            entries.size,
            entries.map { it.sourceIndex }.distinct().size,
            "Unknown categories must not cause entry loss."
        )
    }

    @Test
    fun listFieldsAreNeverNullAfterReading() {
        val catalogFile = resolveCatalogFile()

        val entries = reader.read(catalogFile)

        /*
         * These accesses deliberately exercise all list properties. Kotlin
         * non-null types alone are insufficient when Gson creates objects:
         * malformed JSON can still result in null-backed platform values.
         */
        entries.forEach { entry ->
            val sourceIndex = entry.sourceIndex

            assertNotNull(
                entry.item.colloquial,
                "colloquial must not be null at sourceIndex $sourceIndex."
            )

            assertNotNull(
                entry.item.phoneticTokens,
                "phoneticTokens must not be null at sourceIndex $sourceIndex."
            )

            assertNotNull(
                entry.item.autocompleteTokens,
                "autocompleteTokens must not be null at sourceIndex " +
                        "$sourceIndex."
            )

            assertTrue(
                entry.item.colloquial.none(String::isBlank),
                "colloquial must not contain blank values at sourceIndex " +
                        "$sourceIndex."
            )

            assertTrue(
                entry.item.phoneticTokens.none(String::isBlank),
                "phoneticTokens must not contain blank values at sourceIndex " +
                        "$sourceIndex."
            )

            assertTrue(
                entry.item.autocompleteTokens.none(String::isBlank),
                "autocompleteTokens must not contain blank values at " +
                        "sourceIndex $sourceIndex."
            )
        }
    }

    @Test
    fun roundTripCatalogWithoutDataLoss() {
        val catalogFile = resolveCatalogFile()
        val originalEntries = reader.read(catalogFile)

        val temporaryDirectory = Files.createTempDirectory(
            "canonical-food-catalog-reader-roundtrip-"
        ).toFile()

        val roundTripFile = File(
            temporaryDirectory,
            "catalog.roundtrip.json"
        )

        try {
            val originalItems = originalEntries
                .sortedBy { it.sourceIndex }
                .map { it.item }

            writeCatalogItems(
                items = originalItems,
                outputFile = roundTripFile
            )

            assertTrue(
                roundTripFile.isFile,
                "Roundtrip catalog file was not created."
            )

            assertTrue(
                roundTripFile.length() > 0L,
                "Roundtrip catalog file must not be empty."
            )

            val roundTripEntries = reader.read(roundTripFile)

            assertEquals(
                originalEntries.size,
                roundTripEntries.size,
                "Roundtrip must preserve the number of catalog entries."
            )

            assertEquals(
                originalEntries,
                roundTripEntries,
                "Writing and rereading catalog items must preserve all data."
            )

            assertContentEquals(
                originalEntries.map { it.sourceIndex },
                roundTripEntries.map { it.sourceIndex },
                "Roundtrip must preserve source-index order."
            )

            assertContentEquals(
                originalEntries.map { it.item.itemname },
                roundTripEntries.map { it.item.itemname },
                "Roundtrip must preserve item-name order."
            )

            assertContentEquals(
                originalEntries.map { it.item.normalized },
                roundTripEntries.map { it.item.normalized },
                "Roundtrip must preserve normalized-key order."
            )

            assertContentEquals(
                originalEntries.map { it.item.category },
                roundTripEntries.map { it.item.category },
                "Roundtrip must preserve category order."
            )

            assertEquals(
                originalEntries.size,
                readJsonArray(roundTripFile).size(),
                "Roundtrip JSON array size must equal reader result size."
            )
        } finally {
            deleteRecursivelyOrFail(temporaryDirectory)
        }
    }

    private fun validateSourceIndices(
        entries: List<IndexedCatalogFoodItem>
    ) {
        val expectedIndices = entries.indices.toList()
        val actualIndices = entries.map { it.sourceIndex }

        assertEquals(
            expectedIndices,
            actualIndices,
            "Catalog sourceIndex values must be continuous and zero-based."
        )

        assertEquals(
            entries.size,
            actualIndices.distinct().size,
            "Catalog sourceIndex values must be unique."
        )

        assertTrue(
            actualIndices.all { it >= 0 },
            "Catalog sourceIndex values must not be negative."
        )
    }

    private fun validateItemNames(
        entries: List<IndexedCatalogFoodItem>
    ) {
        entries.forEach { entry ->
            val itemName = entry.item.itemname

            assertTrue(
                itemName.isNotBlank(),
                "itemname must not be blank at sourceIndex " +
                        "${entry.sourceIndex}."
            )

            assertEquals(
                itemName.trim(),
                itemName,
                "itemname must not contain surrounding whitespace at " +
                        "sourceIndex ${entry.sourceIndex}: '$itemName'."
            )

            assertFalse(
                MULTIPLE_WHITESPACE_REGEX.containsMatchIn(itemName),
                "itemname must not contain repeated whitespace at " +
                        "sourceIndex ${entry.sourceIndex}: '$itemName'."
            )
        }
    }

    private fun validateListFields(
        entries: List<IndexedCatalogFoodItem>
    ) {
        entries.forEach { entry ->
            validateListField(
                sourceIndex = entry.sourceIndex,
                fieldName = "colloquial",
                values = entry.item.colloquial
            )

            validateListField(
                sourceIndex = entry.sourceIndex,
                fieldName = "phoneticTokens",
                values = entry.item.phoneticTokens
            )

            validateListField(
                sourceIndex = entry.sourceIndex,
                fieldName = "autocompleteTokens",
                values = entry.item.autocompleteTokens
            )
        }
    }

    private fun validateListField(
        sourceIndex: Int,
        fieldName: String,
        values: List<String>
    ) {
        assertTrue(
            values.none(String::isBlank),
            "$fieldName must not contain blank values at sourceIndex " +
                    "$sourceIndex."
        )

        assertEquals(
            values.size,
            values.distinct().size,
            "$fieldName must not contain exact duplicates at sourceIndex " +
                    "$sourceIndex."
        )
    }

    private fun validateReaderOrder(
        entries: List<IndexedCatalogFoodItem>
    ) {
        entries.forEachIndexed { expectedIndex, entry ->
            assertEquals(
                expectedIndex,
                entry.sourceIndex,
                "Reader changed JSON array order at position $expectedIndex."
            )
        }
    }

    private fun validateJsonEntryCount(
        catalogFile: File,
        entries: List<IndexedCatalogFoodItem>
    ) {
        val jsonEntries = readJsonArray(catalogFile)

        assertEquals(
            jsonEntries.size(),
            entries.size,
            "Number of reader entries must equal JSON array size."
        )
    }

    private fun readJsonArray(
        catalogFile: File
    ): JsonArray {
        val json = catalogFile.readText(
            StandardCharsets.UTF_8
        )

        val rootElement = try {
            JsonParser.parseString(json)
        } catch (exception: RuntimeException) {
            fail(
                "catalog.json is not valid JSON: " +
                        exception.message
            )
        }

        assertTrue(
            rootElement.isJsonArray,
            "catalog.json root element must be a JSON array."
        )

        return rootElement.asJsonArray
    }

    private fun writeCatalogItems(
        items: List<*>,
        outputFile: File
    ) {
        val parentDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        )

        if (!parentDirectory.exists()) {
            require(parentDirectory.mkdirs()) {
                "Could not create roundtrip output directory: " +
                        parentDirectory.absolutePath
            }
        }

        outputFile.writeText(
            gson.toJson(items).trimEnd() +
                    System.lineSeparator(),
            StandardCharsets.UTF_8
        )
    }

    private fun resolveCatalogFile(): File {
        val projectRoot = resolveProjectRoot()

        val paths =
            KnowledgeBuildPaths.default()

        val candidates =
            listOf(
                paths.canonicalFoodCatalog,
                File(
                    projectRoot,
                    "data/catalog/catalog.json"
                ),
                File(
                    projectRoot,
                    "catalog.json"
                )
            )

        val existingCandidates = candidates
            .filter(File::isFile)

        require(existingCandidates.isNotEmpty()) {
            buildString {
                appendLine("Could not locate catalog.json.")
                appendLine("Checked paths:")

                candidates.forEach { candidate ->
                    appendLine("- ${candidate.absolutePath}")
                }
            }
        }

        require(existingCandidates.size == 1) {
            buildString {
                appendLine(
                    "Multiple catalog.json files were found. " +
                            "Reader integration tests require one unambiguous " +
                            "productive catalog."
                )

                existingCandidates.forEach { candidate ->
                    appendLine("- ${candidate.absolutePath}")
                }
            }
        }

        return existingCandidates.single()
    }

    private fun resolveProjectRoot(): File {
        val workingDirectory = File(
            requireNotNull(
            System.getProperty("user.dir")
            )
        )
            .absoluteFile
            .normalize()

        return generateSequence(workingDirectory) {
                directory -> directory.parentFile
        }
            .take(MAXIMUM_PARENT_SEARCH_DEPTH)
            .firstOrNull(::looksLikeProjectRoot)
            ?: error(
                "Could not locate ShopMe project root starting from " +
                        "'${workingDirectory.absolutePath}'."
            )
    }

    private fun looksLikeProjectRoot(
        directory: File
    ): Boolean =
        File(directory, "settings.gradle").isFile ||
                File(directory, "settings.gradle.kts").isFile

    private fun deleteRecursivelyOrFail(
        directory: File
    ) {
        if (!directory.exists()) {
            return
        }

        directory
            .walkBottomUp()
            .forEach { file ->
                require(file.delete()) {
                    "Could not delete temporary roundtrip path: " +
                            file.absolutePath
                }
            }
    }

    private companion object {

        const val MAXIMUM_PARENT_SEARCH_DEPTH = 8
        const val MAXIMUM_DIAGNOSTIC_ITEMS_PER_GROUP = 20

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s{2,}")
    }
}