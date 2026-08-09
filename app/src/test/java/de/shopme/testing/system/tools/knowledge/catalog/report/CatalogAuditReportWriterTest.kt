package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlanner
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryValidator
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateDetector
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageValidator
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.CatalogNonFoodDetector
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CanonicalFoodKeyNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CanonicalFoodNameNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogFoodItemNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogTokenNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.normalization.GermanFoodPluralNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.reader.CatalogFoodItemReader
import de.shopme.testing.system.tools.knowledge.catalog.runner.CatalogAuditPipeline
import de.shopme.testing.system.tools.knowledge.catalog.runner.CatalogAuditPipelineResult
import de.shopme.testing.system.tools.knowledge.catalog.runner.CatalogAuditReportWriters
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogQualityValidator
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatalogAuditReportWriterTest {

    private val gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .serializeNulls()
            .create()

    @Test
    fun writeCompleteCatalogAuditReportSet() {
        withTemporaryAuditEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            assertCompletePipelineResult(result)
            assertExpectedReportFiles(result)
            assertAllReportsAreValidJson(result)
            assertRequiredTopLevelFields(result)
        }
    }

    @Test
    fun writeReportsDeterministically() {
        withTemporaryAuditEnvironment { environment ->
            val firstResult = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val firstHashes = calculateReportHashes(
                firstResult.reportFiles
            )

            val secondResult = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val secondHashes = calculateReportHashes(
                secondResult.reportFiles
            )

            assertEquals(
                firstHashes,
                secondHashes,
                "Repeated audit runs must produce byte-identical reports."
            )

            EXPECTED_REPORT_FILES.keys.forEach { reportKey ->
                val firstFile =
                    requireNotNull(firstResult.reportFiles[reportKey])

                val secondFile =
                    requireNotNull(secondResult.reportFiles[reportKey])

                assertEquals(
                    firstFile.readText(StandardCharsets.UTF_8),
                    secondFile.readText(StandardCharsets.UTF_8),
                    "Report '$reportKey' changed between identical runs."
                )
            }
        }
    }

    @Test
    fun overwriteExistingReportsAtomically() {
        withTemporaryAuditEnvironment { environment ->
            EXPECTED_REPORT_FILES.values.forEach { fileName ->
                File(
                    environment.outputDirectory,
                    fileName
                ).writeText(
                    "obsolete-content",
                    StandardCharsets.UTF_8
                )
            }

            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            result.reportFiles.forEach { (reportKey, reportFile) ->
                val content = reportFile.readText(
                    StandardCharsets.UTF_8
                )

                assertFalse(
                    content.contains("obsolete-content"),
                    "Report '$reportKey' was not replaced."
                )

                assertTrue(
                    content.trimStart().startsWith("{"),
                    "Report '$reportKey' must contain a JSON object."
                )
            }

            val temporaryArtifacts = environment.outputDirectory
                .listFiles()
                .orEmpty()
                .filter { file ->
                    file.name.startsWith(".") &&
                            file.name.endsWith(".tmp")
                }

            assertTrue(
                temporaryArtifacts.isEmpty(),
                "Atomic report writers left temporary files behind: " +
                        temporaryArtifacts.joinToString {
                            it.absolutePath
                        }
            )
        }
    }

    @Test
    fun useCanonicalReportFileNames() {
        withTemporaryAuditEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            assertEquals(
                EXPECTED_REPORT_FILES.keys.sorted(),
                result.reportFiles.keys.sorted()
            )

            EXPECTED_REPORT_FILES.forEach {
                    (reportKey, expectedFileName) ->

                val file = requireNotNull(
                    result.reportFiles[reportKey]
                ) {
                    "Missing report mapping '$reportKey'."
                }

                assertEquals(
                    expectedFileName,
                    file.name,
                    "Unexpected filename for report '$reportKey'."
                )

                assertEquals(
                    environment.outputDirectory.canonicalFile,
                    file.parentFile.canonicalFile,
                    "Report '$reportKey' was written outside the requested " +
                            "output directory."
                )
            }
        }
    }

    @Test
    fun qualityReportContainsConsistentSummary() {
        withTemporaryAuditEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val report = readJsonObject(
                result.reportFiles.getValue(REPORT_KEY_QUALITY)
            )

            assertEquals(
                result.inputEntryCount,
                report.requiredInt("inputEntryCount")
            )

            assertEquals(
                result.qualityResult.issueCount,
                report.requiredInt("issueCount")
            )

            assertEquals(
                result.qualityResult.errorCount,
                report.requiredInt("errorCount")
            )

            assertEquals(
                result.qualityResult.warningCount,
                report.requiredInt("warningCount")
            )

            assertEquals(
                result.qualityResult.infoCount,
                report.requiredInt("infoCount")
            )

            assertEquals(
                result.qualityResult.valid,
                report.requiredBoolean("valid")
            )

            assertEquals(
                report.requiredInt("issueCount"),
                report.requiredArray("issues").size()
            )
        }
    }

    @Test
    fun duplicateReportContainsConsistentSummary() {
        withTemporaryAuditEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val report = readJsonObject(
                result.reportFiles.getValue(REPORT_KEY_DUPLICATES)
            )

            assertEquals(
                result.duplicateGroups.size,
                report.requiredInt("groupCount")
            )

            assertEquals(
                result.duplicateGroups.sumOf {
                        group -> group.members.size
                },
                report.requiredInt("duplicateEntryCount")
            )

            assertEquals(
                report.requiredInt("groupCount"),
                report.requiredArray("groups").size()
            )
        }
    }

    @Test
    fun categoryReportContainsConsistentSummary() {
        withTemporaryAuditEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val report = readJsonObject(
                result.reportFiles.getValue(REPORT_KEY_CATEGORY)
            )

            assertEquals(
                result.inputEntryCount,
                report.requiredInt("inputEntryCount")
            )

            assertEquals(
                result.categoryResult.validCategoryCount,
                report.requiredInt("validCategoryCount")
            )

            assertEquals(
                result.categoryResult.unknownCategoryCount,
                report.requiredInt("unknownCategoryCount")
            )

            assertEquals(
                result.categoryResult.missingCategoryCount,
                report.requiredInt("missingCategoryCount")
            )

            assertEquals(
                result.categoryResult.issues.size,
                report.requiredInt("issueCount")
            )
        }
    }

    @Test
    fun languageReportContainsConsistentSummary() {
        withTemporaryAuditEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val report = readJsonObject(
                result.reportFiles.getValue(REPORT_KEY_LANGUAGE)
            )

            assertEquals(
                result.inputEntryCount,
                report.requiredInt("inputEntryCount")
            )

            assertEquals(
                result.languageResult.issueCount,
                report.requiredInt("issueCount")
            )

            assertEquals(
                result.languageResult.affectedEntryCount,
                report.requiredInt("affectedEntryCount")
            )

            assertEquals(
                result.languageResult.errorCount,
                report.requiredInt("errorCount")
            )

            assertEquals(
                result.languageResult.warningCount,
                report.requiredInt("warningCount")
            )
        }
    }

    @Test
    fun nonFoodReportContainsConsistentSummary() {
        withTemporaryAuditEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val report = readJsonObject(
                result.reportFiles.getValue(REPORT_KEY_NON_FOOD)
            )

            assertEquals(
                result.nonFoodCandidates.size,
                report.requiredInt("candidateCount")
            )

            assertEquals(
                result.nonFoodCandidates.size,
                report.requiredArray("candidates").size()
            )

            val recommendationSummary =
                report.requiredInt("automaticRemovalCount") +
                        report.requiredInt("removalAfterReviewCount") +
                        report.requiredInt("reviewCount") +
                        report.requiredInt("keepCount")

            assertEquals(
                result.nonFoodCandidates.size,
                recommendationSummary
            )
        }
    }

    @Test
    fun normalizationReportContainsConsistentSummary() {
        withTemporaryAuditEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val report = readJsonObject(
                result.reportFiles.getValue(REPORT_KEY_NORMALIZATION)
            )

            assertEquals(
                result.inputEntryCount,
                report.requiredInt("inputEntryCount")
            )

            assertEquals(
                result.normalizations.size,
                report.requiredArray("entries").size()
            )

            assertEquals(
                result.inputEntryCount,
                report.requiredInt("changedEntryCount") +
                        report.requiredInt("unchangedEntryCount")
            )

            assertEquals(
                result.normalizations.sumOf {
                        normalization -> normalization.changes.size
                },
                report.requiredInt("totalChangeCount")
            )
        }
    }

    @Test
    fun canonicalizationReportContainsConsistentSummary() {
        withTemporaryAuditEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val report = readJsonObject(
                result.reportFiles.getValue(
                    REPORT_KEY_CANONICALIZATION
                )
            )

            assertEquals(
                result.inputEntryCount,
                report.requiredInt("inputEntryCount")
            )

            assertEquals(
                result.canonicalizationPlan.planEntryCount,
                report.requiredInt("planEntryCount")
            )

            assertEquals(
                result.canonicalizationPlan.entries.size,
                report.requiredArray("entries").size()
            )

            assertEquals(
                result.canonicalizationPlan.automaticActionCount,
                report.requiredInt("automaticActionCount")
            )

            assertEquals(
                result.canonicalizationPlan.unchangedEntryCount,
                report.requiredInt("unchangedEntryCount")
            )

            assertEquals(
                result.canonicalizationPlan.valid,
                report.requiredBoolean("valid")
            )
        }
    }

    @Test
    fun taxonomyGapReportContainsRegistrySummary() {
        withTemporaryAuditEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val report = readJsonObject(
                result.reportFiles.getValue(
                    REPORT_KEY_TAXONOMY_GAPS
                )
            )

            val registryCategoryCount =
                environment.categoryRegistry.definitions().size

            assertEquals(
                registryCategoryCount,
                report.requiredInt("registryCategoryCount")
            )

            assertEquals(
                registryCategoryCount,
                report.requiredArray("categories").size()
            )

            assertEquals(
                registryCategoryCount,
                report.requiredInt("assignedCategoryCount") +
                        report.requiredInt("unusedCategoryCount")
            )

            assertTrue(
                report.requiredDouble("taxonomyCoverage") in 0.0..1.0
            )

            assertTrue(
                report.requiredDouble("leafTaxonomyCoverage") in 0.0..1.0
            )

            assertTrue(
                report.requiredDouble("entryLeafSpecificity") in 0.0..1.0
            )
        }
    }

    @Test
    fun everyReportEndsWithSingleLineSeparator() {
        withTemporaryAuditEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            result.reportFiles.forEach { (reportKey, file) ->
                val content = file.readText(StandardCharsets.UTF_8)

                assertTrue(
                    content.endsWith(System.lineSeparator()),
                    "Report '$reportKey' must end with a line separator."
                )

                assertFalse(
                    content.endsWith(
                        System.lineSeparator() +
                                System.lineSeparator()
                    ),
                    "Report '$reportKey' must not contain multiple trailing " +
                            "blank lines."
                )
            }
        }
    }

    private fun assertCompletePipelineResult(
        result: CatalogAuditPipelineResult
    ) {
        assertEquals(
            TEST_CATALOG_ENTRY_COUNT,
            result.inputEntryCount
        )

        assertEquals(
            result.inputEntryCount,
            result.normalizations.size
        )

        assertEquals(
            result.inputEntryCount,
            result.qualityResult.inputEntryCount
        )

        assertEquals(
            result.inputEntryCount,
            result.categoryResult.inputEntryCount
        )

        assertEquals(
            result.inputEntryCount,
            result.languageResult.inputEntryCount
        )

        assertEquals(
            result.inputEntryCount,
            result.canonicalizationPlan.inputEntryCount
        )

        assertEquals(
            result.inputEntryCount,
            result.canonicalizationPlan.planEntryCount
        )

        assertTrue(
            result.valid,
            "Audit result must be structurally valid."
        )
    }

    private fun assertExpectedReportFiles(
        result: CatalogAuditPipelineResult
    ) {
        assertEquals(
            EXPECTED_REPORT_FILES.keys.sorted(),
            result.reportFiles.keys.sorted()
        )

        EXPECTED_REPORT_FILES.forEach {
                (reportKey, expectedFileName) ->

            val reportFile =
                result.reportFiles[reportKey]

            assertNotNull(
                reportFile,
                "Missing report '$reportKey'."
            )

            assertEquals(
                expectedFileName,
                reportFile.name
            )

            assertTrue(
                reportFile.isFile,
                "Report '$reportKey' was not created."
            )

            assertTrue(
                reportFile.canRead(),
                "Report '$reportKey' is not readable."
            )

            assertTrue(
                reportFile.length() > 0L,
                "Report '$reportKey' is empty."
            )
        }
    }

    private fun assertAllReportsAreValidJson(
        result: CatalogAuditPipelineResult
    ) {
        result.reportFiles.forEach { (reportKey, reportFile) ->
            val jsonElement = parseJson(reportFile)

            assertTrue(
                jsonElement.isJsonObject,
                "Report '$reportKey' root must be a JSON object."
            )
        }
    }

    private fun assertRequiredTopLevelFields(
        result: CatalogAuditPipelineResult
    ) {
        result.reportFiles.forEach { (reportKey, reportFile) ->
            val report = readJsonObject(reportFile)

            assertTrue(
                report.has("version"),
                "Report '$reportKey' must contain version."
            )

            assertTrue(
                report.requiredInt("version") > 0,
                "Report '$reportKey' version must be positive."
            )
        }

        assertFieldsPresent(
            result.reportFiles.getValue(REPORT_KEY_QUALITY),
            "inputEntryCount",
            "issueCount",
            "issues",
            "valid"
        )

        assertFieldsPresent(
            result.reportFiles.getValue(REPORT_KEY_DUPLICATES),
            "groupCount",
            "duplicateEntryCount",
            "groups"
        )

        assertFieldsPresent(
            result.reportFiles.getValue(REPORT_KEY_CATEGORY),
            "inputEntryCount",
            "categoryCounts",
            "issues",
            "valid"
        )

        assertFieldsPresent(
            result.reportFiles.getValue(REPORT_KEY_LANGUAGE),
            "inputEntryCount",
            "issueCount",
            "issues",
            "valid"
        )

        assertFieldsPresent(
            result.reportFiles.getValue(REPORT_KEY_NON_FOOD),
            "candidateCount",
            "recommendationCounts",
            "candidates"
        )

        assertFieldsPresent(
            result.reportFiles.getValue(REPORT_KEY_NORMALIZATION),
            "inputEntryCount",
            "totalChangeCount",
            "entries",
            "valid"
        )

        assertFieldsPresent(
            result.reportFiles.getValue(
                REPORT_KEY_CANONICALIZATION
            ),
            "inputEntryCount",
            "planEntryCount",
            "actionCounts",
            "entries",
            "valid"
        )

        assertFieldsPresent(
            result.reportFiles.getValue(
                REPORT_KEY_TAXONOMY_GAPS
            ),
            "registryCategoryCount",
            "taxonomyCoverage",
            "rootSummaries",
            "categories",
            "valid"
        )
    }

    private fun assertFieldsPresent(
        file: File,
        vararg fieldNames: String
    ) {
        val report = readJsonObject(file)

        fieldNames.forEach { fieldName ->
            assertTrue(
                report.has(fieldName),
                "Report '${file.name}' is missing field '$fieldName'."
            )
        }
    }

    private fun calculateReportHashes(
        reportFiles: Map<String, File>
    ): Map<String, String> =
        reportFiles
            .toSortedMap()
            .mapValues { (_, reportFile) ->
                sha256(reportFile.readBytes())
            }

    private fun sha256(
        bytes: ByteArray
    ): String {
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)

        return digest.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    private fun createPipeline(
        categoryRegistry: CanonicalFoodCategoryRegistry
    ): CatalogAuditPipeline =
        CatalogAuditPipeline(
            reader = CatalogFoodItemReader(),
            normalizer = CatalogFoodItemNormalizer(
                foodNameNormalizer =
                    CanonicalFoodNameNormalizer(),
                keyNormalizer =
                    CanonicalFoodKeyNormalizer(),
                tokenNormalizer =
                    CatalogTokenNormalizer(),
                pluralNormalizer =
                    GermanFoodPluralNormalizer()
            ),
            qualityValidator = CatalogQualityValidator(),
            duplicateDetector = CatalogDuplicateDetector(),
            categoryValidator = CatalogCategoryValidator(),
            languageValidator = CatalogLanguageValidator(),
            nonFoodDetector = CatalogNonFoodDetector(),
            canonicalizationPlanner =
                CatalogCanonicalizationPlanner(),
            reportWriters = CatalogAuditReportWriters(
                qualityReportWriter =
                    CatalogQualityReportWriter(),
                duplicateGroupsReportWriter =
                    CatalogDuplicateGroupsReportWriter(),
                categoryReportWriter =
                    CatalogCategoryReportWriter(),
                languageIssuesReportWriter =
                    CatalogLanguageIssuesReportWriter(),
                nonFoodCandidatesReportWriter =
                    CatalogNonFoodCandidatesReportWriter(),
                canonicalizationPlanReportWriter =
                    CatalogCanonicalizationPlanReportWriter(),
                normalizationReportWriter =
                    CatalogNormalizationReportWriter(),
                taxonomyGapReportWriter =
                    CatalogTaxonomyGapReportWriter()
            ),
            categoryRegistry = categoryRegistry
        )

    private fun createCatalogItems(
        registry: CanonicalFoodCategoryRegistry
    ): List<CatalogFoodItem> {
        val leafDefinitions = registry.definitions()
            .filter { definition ->
                registry.children(definition.key).isEmpty()
            }

        require(leafDefinitions.isNotEmpty()) {
            "Category registry must contain at least one leaf category."
        }

        val firstCategory = leafDefinitions.first().key

        val secondCategory = leafDefinitions
            .drop(1)
            .firstOrNull()
            ?.key
            ?: firstCategory

        return listOf(
            CatalogFoodItem(
                itemname = "Apfel",
                category = firstCategory,
                production = null,
                normalized = "apfel",
                plural = "Äpfel",
                colloquial = emptyList(),
                phoneticTokens = listOf("apfel"),
                autocompleteTokens = listOf("apfel"),
                normalizedEnglish = "apple"
            ),
            CatalogFoodItem(
                itemname = " APFEL ",
                category = firstCategory,
                production = null,
                normalized = "APFEL",
                plural = "Äpfel",
                colloquial = listOf("Apfel"),
                phoneticTokens = listOf("APFEL"),
                autocompleteTokens = listOf("Apfel"),
                normalizedEnglish = "apple"
            ),
            CatalogFoodItem(
                itemname = "Paprika rot",
                category = secondCategory,
                production = null,
                normalized = "paprika_rot",
                plural = "Paprika rot",
                colloquial = listOf("Rote Paprika"),
                phoneticTokens = listOf("paprika", "rot"),
                autocompleteTokens = listOf(
                    "Paprika",
                    "Rot"
                ),
                normalizedEnglish = "red-pepper"
            )
        )
    }

    private fun writeCatalog(
        catalogFile: File,
        items: List<CatalogFoodItem>
    ) {
        val parentDirectory =
            requireNotNull(catalogFile.parentFile)

        if (!parentDirectory.exists()) {
            require(parentDirectory.mkdirs()) {
                "Could not create temporary catalog directory."
            }
        }

        catalogFile.writeText(
            gson.toJson(items).trimEnd() +
                    System.lineSeparator(),
            StandardCharsets.UTF_8
        )
    }

    private fun readJsonObject(
        file: File
    ): JsonObject =
        parseJson(file).asJsonObject

    private fun parseJson(
        file: File
    ): JsonElement =
        JsonParser.parseString(
            file.readText(StandardCharsets.UTF_8)
        )

    private fun JsonObject.requiredInt(
        fieldName: String
    ): Int {
        val element = get(fieldName)

        assertNotNull(
            element,
            "Missing integer field '$fieldName'."
        )

        assertTrue(
            element.isJsonPrimitive &&
                    element.asJsonPrimitive.isNumber,
            "Field '$fieldName' must be numeric."
        )

        return element.asInt
    }

    private fun JsonObject.requiredDouble(
        fieldName: String
    ): Double {
        val element = get(fieldName)

        assertNotNull(
            element,
            "Missing decimal field '$fieldName'."
        )

        assertTrue(
            element.isJsonPrimitive &&
                    element.asJsonPrimitive.isNumber,
            "Field '$fieldName' must be numeric."
        )

        return element.asDouble
    }

    private fun JsonObject.requiredBoolean(
        fieldName: String
    ): Boolean {
        val element = get(fieldName)

        assertNotNull(
            element,
            "Missing Boolean field '$fieldName'."
        )

        assertTrue(
            element.isJsonPrimitive &&
                    element.asJsonPrimitive.isBoolean,
            "Field '$fieldName' must be Boolean."
        )

        return element.asBoolean
    }

    private fun JsonObject.requiredArray(
        fieldName: String
    ) =
        get(fieldName).also { element ->
            assertNotNull(
                element,
                "Missing array field '$fieldName'."
            )

            assertTrue(
                element.isJsonArray,
                "Field '$fieldName' must be an array."
            )
        }.asJsonArray

    private inline fun withTemporaryAuditEnvironment(
        block: (TemporaryAuditEnvironment) -> Unit
    ) {
        val temporaryDirectory = Files
            .createTempDirectory(
                "catalog-audit-report-writer-test-"
            )
            .toFile()

        try {
            val catalogFile = File(
                temporaryDirectory,
                "catalog.json"
            )

            val outputDirectory = File(
                temporaryDirectory,
                "audit"
            )

            require(outputDirectory.mkdirs()) {
                "Could not create temporary audit directory."
            }

            val categoryRegistry =
                CanonicalFoodCategoryRegistry()

            writeCatalog(
                catalogFile = catalogFile,
                items = createCatalogItems(categoryRegistry)
            )

            block(
                TemporaryAuditEnvironment(
                    catalogFile = catalogFile,
                    outputDirectory = outputDirectory,
                    categoryRegistry = categoryRegistry,
                    pipeline = createPipeline(categoryRegistry)
                )
            )
        } finally {
            deleteRecursivelyOrFail(temporaryDirectory)
        }
    }

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
                    "Could not delete temporary path: " +
                            file.absolutePath
                }
            }
    }

    private data class TemporaryAuditEnvironment(
        val catalogFile: File,
        val outputDirectory: File,
        val categoryRegistry: CanonicalFoodCategoryRegistry,
        val pipeline: CatalogAuditPipeline
    )

    private companion object {

        const val TEST_CATALOG_ENTRY_COUNT = 3

        const val REPORT_KEY_QUALITY = "quality"
        const val REPORT_KEY_DUPLICATES = "duplicates"
        const val REPORT_KEY_CATEGORY = "category"
        const val REPORT_KEY_LANGUAGE = "language"
        const val REPORT_KEY_NON_FOOD = "non-food"
        const val REPORT_KEY_NORMALIZATION = "normalization"
        const val REPORT_KEY_CANONICALIZATION =
            "canonicalization"
        const val REPORT_KEY_TAXONOMY_GAPS =
            "taxonomy-gaps"

        val EXPECTED_REPORT_FILES = sortedMapOf(
            REPORT_KEY_CANONICALIZATION to
                    "catalog-canonicalization-plan.json",
            REPORT_KEY_CATEGORY to
                    "catalog-category-report.json",
            REPORT_KEY_DUPLICATES to
                    "catalog-duplicate-groups.json",
            REPORT_KEY_LANGUAGE to
                    "catalog-language-issues.json",
            REPORT_KEY_NON_FOOD to
                    "catalog-non-food-candidates.json",
            REPORT_KEY_NORMALIZATION to
                    "catalog-normalization-report.json",
            REPORT_KEY_QUALITY to
                    "catalog-quality-report.json",
            REPORT_KEY_TAXONOMY_GAPS to
                    "catalog-taxonomy-gaps.json"
        )
    }
}