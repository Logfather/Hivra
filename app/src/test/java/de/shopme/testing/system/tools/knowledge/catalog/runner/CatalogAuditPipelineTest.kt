package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
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
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogCanonicalizationPlanReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogCategoryReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogDuplicateGroupsReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogLanguageIssuesReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogNonFoodCandidatesReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogNormalizationReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogQualityReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogTaxonomyGapReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogQualityValidator
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatalogAuditPipelineTest {

    private val gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .serializeNulls()
            .create()

    @Test
    fun runCompleteCatalogAuditPipeline() {
        withTemporaryEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

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

            assertEquals(
                result.inputEntryCount,
                result.canonicalizationPlan.entries.size
            )

            assertTrue(result.canonicalizationPlan.valid)
            assertTrue(result.valid)
        }
    }

    @Test
    fun createExactlyOneNormalizationPerCatalogEntry() {
        withTemporaryEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            assertEquals(
                result.inputEntryCount,
                result.normalizations.size
            )

            assertEquals(
                result.inputEntryCount,
                result.normalizations
                    .map { it.sourceIndex }
                    .distinct()
                    .size
            )

            assertEquals(
                listOf(0, 1, 2, 3),
                result.normalizations.map { it.sourceIndex }
            )
        }
    }

    @Test
    fun createExactlyOneCanonicalizationActionPerCatalogEntry() {
        withTemporaryEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val plan = result.canonicalizationPlan

            assertEquals(
                result.inputEntryCount,
                plan.entries.size
            )

            assertEquals(
                result.inputEntryCount,
                plan.entries
                    .map { it.sourceIndex }
                    .distinct()
                    .size
            )

            assertEquals(
                result.inputEntryCount,
                plan.actionCounts.values.sum()
            )

            assertEquals(
                plan.entries.count { it.automatic },
                plan.automaticActionCount
            )

            assertEquals(
                plan.entries.count {
                    it.action ==
                            CatalogCanonicalizationAction.REVIEW
                },
                plan.reviewActionCount
            )

            assertEquals(
                plan.entries.count {
                    it.action ==
                            CatalogCanonicalizationAction.KEEP
                },
                plan.unchangedEntryCount
            )
        }
    }

    @Test
    fun detectDuplicateCatalogEntries() {
        withTemporaryEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            assertTrue(
                result.duplicateGroups.isNotEmpty(),
                "Test catalog must produce at least one duplicate group."
            )

            val apfelGroup = result.duplicateGroups.firstOrNull {
                    group ->
                group.members.any {
                    it.itemName.trim().equals(
                        "Apfel",
                        ignoreCase = true
                    )
                }
            }

            assertNotNull(
                apfelGroup,
                "Expected a duplicate group for Apfel."
            )

            assertEquals(
                setOf(0, 1),
                apfelGroup.members
                    .map { it.sourceIndex }
                    .toSet()
            )

            assertTrue(
                apfelGroup.members.size >= 2
            )

            assertTrue(
                apfelGroup.confidence in 0.0..1.0
            )
        }
    }

    @Test
    fun detectNonFoodCandidate() {
        withTemporaryEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val nonFoodCandidate =
                result.nonFoodCandidates.firstOrNull {
                    it.sourceIndex == NON_FOOD_SOURCE_INDEX
                }

            assertNotNull(
                nonFoodCandidate,
                "Spülmittel must be detected as a non-food candidate."
            )

            assertEquals(
                "Spülmittel",
                nonFoodCandidate.itemName
            )

            assertTrue(
                nonFoodCandidate.reasons.isNotEmpty()
            )

            assertTrue(
                nonFoodCandidate.matchedTerms.isNotEmpty()
            )

            assertTrue(
                nonFoodCandidate.confidence in 0.0..1.0
            )
        }
    }

    @Test
    fun prioritizeNonFoodRemovalInCanonicalizationPlan() {
        withTemporaryEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val planEntry =
                result.canonicalizationPlan.entries.single {
                    it.sourceIndex == NON_FOOD_SOURCE_INDEX
                }

            assertTrue(
                planEntry.action ==
                        CatalogCanonicalizationAction.REMOVE_NON_FOOD ||
                        planEntry.action ==
                        CatalogCanonicalizationAction.REVIEW,
                "Non-food candidate must result in removal or review."
            )

            assertFalse(
                planEntry.action ==
                        CatalogCanonicalizationAction.KEEP
            )
        }
    }

    @Test
    fun reportAllPipelineStageInputCountsConsistently() {
        withTemporaryEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val expectedCount = result.inputEntryCount

            assertEquals(
                expectedCount,
                result.qualityResult.inputEntryCount
            )

            assertEquals(
                expectedCount,
                result.categoryResult.inputEntryCount
            )

            assertEquals(
                expectedCount,
                result.languageResult.inputEntryCount
            )

            assertEquals(
                expectedCount,
                result.canonicalizationPlan.inputEntryCount
            )

            assertEquals(
                expectedCount,
                result.canonicalizationPlan.planEntryCount
            )
        }
    }

    @Test
    fun writeAllExpectedAuditReports() {
        withTemporaryEnvironment { environment ->
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

                val reportFile = result.reportFiles[reportKey]

                assertNotNull(
                    reportFile,
                    "Missing report mapping '$reportKey'."
                )

                assertEquals(
                    expectedFileName,
                    reportFile.name
                )

                assertEquals(
                    environment.outputDirectory.canonicalFile,
                    requireNotNull(
                        reportFile.parentFile.canonicalFile
                    )
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

                val content = reportFile.readText(
                    StandardCharsets.UTF_8
                )

                assertTrue(
                    content.trimStart().startsWith("{"),
                    "Report '$reportKey' must contain a JSON object."
                )

                assertTrue(
                    content.endsWith(System.lineSeparator()),
                    "Report '$reportKey' must end with a line separator."
                )
            }
        }
    }

    @Test
    fun runPipelineDeterministically() {
        withTemporaryEnvironment { environment ->
            val firstResult = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val firstReportContents = firstResult.reportFiles
                .toSortedMap()
                .mapValues { (_, file) ->
                    file.readText(StandardCharsets.UTF_8)
                }

            val secondResult = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            val secondReportContents = secondResult.reportFiles
                .toSortedMap()
                .mapValues { (_, file) ->
                    file.readText(StandardCharsets.UTF_8)
                }

            assertEquals(
                firstResult.inputEntryCount,
                secondResult.inputEntryCount
            )

            assertEquals(
                firstResult.normalizations,
                secondResult.normalizations
            )

            assertEquals(
                firstResult.qualityResult,
                secondResult.qualityResult
            )

            assertEquals(
                firstResult.duplicateGroups,
                secondResult.duplicateGroups
            )

            assertEquals(
                firstResult.categoryResult,
                secondResult.categoryResult
            )

            assertEquals(
                firstResult.languageResult,
                secondResult.languageResult
            )

            assertEquals(
                firstResult.nonFoodCandidates,
                secondResult.nonFoodCandidates
            )

            assertEquals(
                firstResult.canonicalizationPlan,
                secondResult.canonicalizationPlan
            )

            assertEquals(
                firstReportContents,
                secondReportContents,
                "Repeated pipeline runs must produce byte-identical reports."
            )
        }
    }

    @Test
    fun overwriteExistingReports() {
        withTemporaryEnvironment { environment ->
            EXPECTED_REPORT_FILES.values.forEach { fileName ->
                File(
                    environment.outputDirectory,
                    fileName
                ).writeText(
                    "obsolete",
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
                    content.contains("obsolete"),
                    "Report '$reportKey' was not overwritten."
                )
            }

            val temporaryFiles =
                environment.outputDirectory
                    .listFiles()
                    .orEmpty()
                    .filter {
                        it.name.endsWith(".tmp")
                    }

            assertTrue(
                temporaryFiles.isEmpty(),
                "Pipeline left temporary report files: " +
                        temporaryFiles.joinToString {
                            it.name
                        }
            )
        }
    }

    @Test
    fun preserveSourceIndexOrderingAcrossPipeline() {
        withTemporaryEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            assertEquals(
                listOf(0, 1, 2, 3),
                result.normalizations.map { it.sourceIndex }
            )

            assertEquals(
                listOf(0, 1, 2, 3),
                result.canonicalizationPlan.entries
                    .map { it.sourceIndex }
            )

            assertEquals(
                result.canonicalizationPlan.affectedSourceIndices.sorted(),
                result.canonicalizationPlan.affectedSourceIndices
            )
        }
    }

    @Test
    fun rejectMissingCatalogFile() {
        withTemporaryEnvironment(
            createCatalog = false
        ) { environment ->
            val exception =
                assertFailsWith<IllegalArgumentException> {
                    environment.pipeline.run(
                        catalogFile = environment.catalogFile,
                        outputDirectory =
                            environment.outputDirectory
                    )
                }

            assertTrue(
                exception.message
                    ?.contains("does not exist") == true
            )
        }
    }

    @Test
    fun rejectCatalogInputDirectory() {
        withTemporaryEnvironment(
            createCatalog = false
        ) { environment ->
            require(environment.catalogFile.mkdirs())

            val exception =
                assertFailsWith<IllegalArgumentException> {
                    environment.pipeline.run(
                        catalogFile = environment.catalogFile,
                        outputDirectory =
                            environment.outputDirectory
                    )
                }

            assertTrue(
                exception.message
                    ?.contains("not a file") == true
            )
        }
    }

    @Test
    fun createMissingOutputDirectory() {
        withTemporaryEnvironment { environment ->
            require(
                environment.outputDirectory.deleteRecursively()
            )

            assertFalse(environment.outputDirectory.exists())

            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            assertTrue(environment.outputDirectory.isDirectory)

            assertTrue(
                result.reportFiles.values.all(File::isFile)
            )
        }
    }

    @Test
    fun rejectOutputPathThatIsFile() {
        withTemporaryEnvironment { environment ->
            require(
                environment.outputDirectory.deleteRecursively()
            )

            environment.outputDirectory.writeText(
                "not-a-directory",
                StandardCharsets.UTF_8
            )

            val exception =
                assertFailsWith<IllegalArgumentException> {
                    environment.pipeline.run(
                        catalogFile = environment.catalogFile,
                        outputDirectory =
                            environment.outputDirectory
                    )
                }

            assertTrue(
                exception.message
                    ?.contains("not a directory") == true
            )
        }
    }

    @Test
    fun processEmptyCatalogDeterministically() {
        withTemporaryEnvironment(
            catalogItems = emptyList()
        ) { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            assertEquals(0, result.inputEntryCount)
            assertTrue(result.normalizations.isEmpty())
            assertTrue(result.duplicateGroups.isEmpty())
            assertTrue(result.nonFoodCandidates.isEmpty())
            assertEquals(
                0,
                result.canonicalizationPlan.planEntryCount
            )
            assertTrue(result.canonicalizationPlan.entries.isEmpty())
            assertTrue(result.canonicalizationPlan.valid)
            assertTrue(result.valid)

            assertEquals(
                EXPECTED_REPORT_FILES.size,
                result.reportFiles.size
            )

            assertTrue(
                result.reportFiles.values.all {
                    it.isFile && it.length() > 0L
                }
            )
        }
    }

    @Test
    fun exposeCanonicalInputAndOutputPathsInResult() {
        withTemporaryEnvironment { environment ->
            val result = environment.pipeline.run(
                catalogFile = environment.catalogFile,
                outputDirectory = environment.outputDirectory
            )

            assertEquals(
                environment.catalogFile.canonicalFile,
                result.inputCatalogFile.canonicalFile
            )

            assertEquals(
                environment.outputDirectory.canonicalFile,
                result.outputDirectory.canonicalFile
            )
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
            qualityValidator =
                CatalogQualityValidator(),
            duplicateDetector =
                CatalogDuplicateDetector(),
            categoryValidator =
                CatalogCategoryValidator(),
            languageValidator =
                CatalogLanguageValidator(),
            nonFoodDetector =
                CatalogNonFoodDetector(),
            canonicalizationPlanner =
                CatalogCanonicalizationPlanner(),
            reportWriters =
                CatalogAuditReportWriters(
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

    private fun createTestCatalogItems(
        registry: CanonicalFoodCategoryRegistry
    ): List<CatalogFoodItem> {
        val leafCategories = registry.definitions()
            .filter { definition ->
                registry.children(definition.key).isEmpty()
            }

        require(leafCategories.isNotEmpty()) {
            "Category registry must contain at least one leaf category."
        }

        val firstFoodCategory =
            leafCategories.first().key

        val secondFoodCategory =
            leafCategories
                .drop(1)
                .firstOrNull()
                ?.key
                ?: firstFoodCategory

        return listOf(
            CatalogFoodItem(
                itemname = "Apfel",
                category = firstFoodCategory,
                production = null,
                normalized = "apfel",
                plural = "Äpfel",
                colloquial = emptyList(),
                phoneticTokens = listOf("apfel"),
                autocompleteTokens = listOf("apfel"),
                normalizedEnglish = "apple"
            ),
            CatalogFoodItem(
                itemname = "APFEL",
                category = firstFoodCategory,
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
                category = secondFoodCategory,
                production = null,
                normalized = "paprika_rot",
                plural = "Paprika rot",
                colloquial = listOf("Rote Paprika"),
                phoneticTokens = listOf(
                    "paprika",
                    "rot"
                ),
                autocompleteTokens = listOf(
                    "Paprika",
                    "Rot"
                ),
                normalizedEnglish = "red-pepper"
            ),
            CatalogFoodItem(
                itemname = "Spülmittel",
                category = firstFoodCategory,
                production = null,
                normalized = "spuelmittel",
                plural = "Spülmittel",
                colloquial = emptyList(),
                phoneticTokens = listOf("spuelmittel"),
                autocompleteTokens = listOf("spuelmittel"),
                normalizedEnglish = "dishwashing-liquid"
            )
        )
    }

    private fun writeCatalog(
        catalogFile: File,
        items: List<CatalogFoodItem>
    ) {
        val parentDirectory =
            requireNotNull(catalogFile.absoluteFile.parentFile)

        if (!parentDirectory.exists()) {
            require(parentDirectory.mkdirs()) {
                "Could not create catalog parent directory."
            }
        }

        catalogFile.writeText(
            gson.toJson(items).trimEnd() +
                    System.lineSeparator(),
            StandardCharsets.UTF_8
        )
    }

    private inline fun withTemporaryEnvironment(
        createCatalog: Boolean = true,
        catalogItems: List<CatalogFoodItem>? = null,
        block: (TemporaryEnvironment) -> Unit
    ) {
        val temporaryRoot = Files.createTempDirectory(
            "catalog-audit-pipeline-test-"
        ).toFile()

        try {
            val catalogFile = File(
                temporaryRoot,
                "catalog.json"
            )

            val outputDirectory = File(
                temporaryRoot,
                "audit"
            )

            require(outputDirectory.mkdirs()) {
                "Could not create temporary output directory."
            }

            val categoryRegistry =
                CanonicalFoodCategoryRegistry()

            if (createCatalog) {
                writeCatalog(
                    catalogFile = catalogFile,
                    items = catalogItems
                        ?: createTestCatalogItems(categoryRegistry)
                )
            }

            block(
                TemporaryEnvironment(
                    catalogFile = catalogFile,
                    outputDirectory = outputDirectory,
                    categoryRegistry = categoryRegistry,
                    pipeline = createPipeline(categoryRegistry)
                )
            )
        } finally {
            deleteRecursivelyOrFail(temporaryRoot)
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

    private data class TemporaryEnvironment(
        val catalogFile: File,
        val outputDirectory: File,
        val categoryRegistry: CanonicalFoodCategoryRegistry,
        val pipeline: CatalogAuditPipeline
    )

    private companion object {

        const val TEST_CATALOG_ENTRY_COUNT = 4
        const val NON_FOOD_SOURCE_INDEX = 3

        val EXPECTED_REPORT_FILES = sortedMapOf(
            "canonicalization" to
                    "catalog-canonicalization-plan.json",
            "category" to
                    "catalog-category-report.json",
            "duplicates" to
                    "catalog-duplicate-groups.json",
            "language" to
                    "catalog-language-issues.json",
            "non-food" to
                    "catalog-non-food-candidates.json",
            "normalization" to
                    "catalog-normalization-report.json",
            "quality" to
                    "catalog-quality-report.json",
            "taxonomy-gaps" to
                    "catalog-taxonomy-gaps.json"
        )
    }
}