package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlanner
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryValidator
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateDetector
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageValidator
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
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalFoodCatalogAuditTest {

    @Test
    fun runCanonicalFoodCatalogAudit() {
        val projectRoot = resolveProjectRoot()

        val catalogFile = resolveCatalogFile(projectRoot)

        val outputDirectory = File(
            projectRoot,
            "data/generated/knowledge/catalog/audit"
        )

        prepareOutputDirectory(outputDirectory)

        val pipeline = CatalogAuditPipeline(
            reader = CatalogFoodItemReader(),
            normalizer = CatalogFoodItemNormalizer(
                foodNameNormalizer = CanonicalFoodNameNormalizer(),
                keyNormalizer = CanonicalFoodKeyNormalizer(),
                tokenNormalizer = CatalogTokenNormalizer(),
                pluralNormalizer = GermanFoodPluralNormalizer()
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
            categoryRegistry =
                CanonicalFoodCategoryRegistry()
        )

        val result = pipeline.run(
            catalogFile = catalogFile,
            outputDirectory = outputDirectory
        )

        assertPipelineResult(result)
        assertReportsExist(result)
        printSummary(result)
    }

    private fun assertPipelineResult(
        result: CatalogAuditPipelineResult
    ) {
        assertTrue(
            result.inputCatalogFile.exists(),
            "Input catalog file must exist."
        )

        assertTrue(
            result.inputCatalogFile.isFile,
            "Input catalog path must be a file."
        )

        assertTrue(
            result.inputEntryCount > 0,
            "Catalog audit must process at least one entry."
        )

        assertEquals(
            result.inputEntryCount,
            result.normalizations.size,
            "Every catalog entry must have exactly one normalization result."
        )

        assertEquals(
            result.inputEntryCount,
            result.qualityResult.inputEntryCount,
            "Quality validation must cover the complete catalog."
        )

        assertEquals(
            result.inputEntryCount,
            result.categoryResult.inputEntryCount,
            "Category validation must cover the complete catalog."
        )

        assertEquals(
            result.inputEntryCount,
            result.languageResult.inputEntryCount,
            "Language validation must cover the complete catalog."
        )

        assertEquals(
            result.inputEntryCount,
            result.canonicalizationPlan.inputEntryCount,
            "Canonicalization plan input count must match the catalog."
        )

        assertEquals(
            result.inputEntryCount,
            result.canonicalizationPlan.planEntryCount,
            "Every catalog entry must have exactly one canonicalization action."
        )

        assertEquals(
            result.inputEntryCount,
            result.canonicalizationPlan.entries.size,
            "Canonicalization plan entries must cover the complete catalog."
        )

        assertEquals(
            result.inputEntryCount,
            result.normalizations
                .map { it.sourceIndex }
                .distinct()
                .size,
            "Normalization source indices must be unique."
        )

        assertEquals(
            result.inputEntryCount,
            result.canonicalizationPlan.entries
                .map { it.sourceIndex }
                .distinct()
                .size,
            "Canonicalization plan source indices must be unique."
        )

        assertTrue(
            result.canonicalizationPlan.valid,
            "Canonicalization plan must be structurally valid."
        )

        assertTrue(
            result.valid,
            "Catalog audit pipeline result must be structurally valid."
        )
    }

    private fun assertReportsExist(
        result: CatalogAuditPipelineResult
    ) {
        assertEquals(
            EXPECTED_REPORT_FILES.keys.sorted(),
            result.reportFiles.keys.sorted(),
            "Catalog audit must expose exactly the expected report keys."
        )

        EXPECTED_REPORT_FILES.forEach { (reportKey, expectedFileName) ->
            val reportFile = result.reportFiles[reportKey]

            assertTrue(
                reportFile != null,
                "Missing report file mapping for '$reportKey'."
            )

            requireNotNull(reportFile)

            assertEquals(
                expectedFileName,
                reportFile.name,
                "Unexpected filename for report '$reportKey'."
            )

            assertTrue(
                reportFile.exists(),
                "Report '$reportKey' was not written: " +
                        reportFile.absolutePath
            )

            assertTrue(
                reportFile.isFile,
                "Report '$reportKey' path is not a file: " +
                        reportFile.absolutePath
            )

            assertTrue(
                reportFile.canRead(),
                "Report '$reportKey' is not readable: " +
                        reportFile.absolutePath
            )

            assertTrue(
                reportFile.length() > 0L,
                "Report '$reportKey' is empty: " +
                        reportFile.absolutePath
            )
        }
    }

    private fun prepareOutputDirectory(
        outputDirectory: File
    ) {
        require(
            !outputDirectory.exists() ||
                    outputDirectory.isDirectory
        ) {
            "Catalog audit output path is not a directory: " +
                    outputDirectory.absolutePath
        }

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Could not create catalog audit output directory: " +
                        outputDirectory.absolutePath
            }
        }

        EXPECTED_REPORT_FILES.values.forEach { fileName ->
            val existingReport = File(
                outputDirectory,
                fileName
            )

            if (existingReport.exists()) {
                require(existingReport.delete()) {
                    "Could not remove existing catalog audit report: " +
                            existingReport.absolutePath
                }
            }
        }
    }

    private fun resolveProjectRoot(): File {
        val workingDirectory = File(
            requireNotNull(
                System.getProperty("user.dir")
            )
        ).absoluteFile.normalize()

        val candidates = generateSequence(workingDirectory) {
            it.parentFile
        }
            .take(MAXIMUM_PARENT_SEARCH_DEPTH)
            .toList()

        return candidates.firstOrNull(::looksLikeProjectRoot)
            ?: error(
                "Could not determine ShopMe project root from " +
                        "'${workingDirectory.absolutePath}'."
            )
    }

    private fun looksLikeProjectRoot(
        directory: File
    ): Boolean =
        File(directory, "settings.gradle").isFile ||
                File(directory, "settings.gradle.kts").isFile

    private fun resolveCatalogFile(
        projectRoot: File
    ): File {
        val catalogFile = KnowledgeBuildPaths
            .default()
            .canonicalFoodCatalog

        require(catalogFile.isFile) {
            "Canonical food catalog does not exist: " +
                    catalogFile.absolutePath
        }

        require(catalogFile.canRead()) {
            "Canonical food catalog is not readable: " +
                    catalogFile.absolutePath
        }

        return catalogFile
    }

    private fun printSummary(
        result: CatalogAuditPipelineResult
    ) {
        println()
        println("Canonical food catalog audit completed.")
        println("---------------------------------------")
        println(
            "Input catalog: ${result.inputCatalogFile.absolutePath}"
        )
        println(
            "Output directory: ${result.outputDirectory.absolutePath}"
        )
        println("Catalog entries: ${result.inputEntryCount}")
        println(
            "Normalization results: ${result.normalizations.size}"
        )
        println(
            "Quality issues: ${result.qualityResult.issueCount}"
        )
        println(
            "Quality errors: ${result.qualityResult.errorCount}"
        )
        println(
            "Quality warnings: ${result.qualityResult.warningCount}"
        )
        println(
            "Duplicate groups: ${result.duplicateGroups.size}"
        )
        println(
            "Category issues: ${result.categoryResult.issues.size}"
        )
        println(
            "Unknown categories: " +
                    result.categoryResult.unknownCategoryCount
        )
        println(
            "Language issues: ${result.languageResult.issueCount}"
        )
        println(
            "Non-food candidates: ${result.nonFoodCandidates.size}"
        )
        println(
            "Canonicalization actions: " +
                    result.canonicalizationPlan.planEntryCount
        )
        println(
            "Automatic actions: " +
                    result.canonicalizationPlan.automaticActionCount
        )
        println(
            "Review actions: " +
                    result.canonicalizationPlan.reviewActionCount
        )
        println(
            "Unchanged entries: " +
                    result.canonicalizationPlan.unchangedEntryCount
        )
        println("Reports:")

        result.reportFiles
            .toSortedMap()
            .forEach { (key, file) ->
                println(
                    "- $key: ${file.absolutePath} " +
                            "(${file.length()} bytes)"
                )
            }

        println("---------------------------------------")
        println("Structurally valid: ${result.valid}")
    }

    private companion object {

        const val MAXIMUM_PARENT_SEARCH_DEPTH = 8

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