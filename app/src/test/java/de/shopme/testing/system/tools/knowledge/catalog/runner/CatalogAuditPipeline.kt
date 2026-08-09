package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlanner
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryValidator
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateDetector
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageValidator
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.CatalogNonFoodDetector
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogFoodItemNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.reader.CatalogFoodItemReader
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogQualityValidator
import java.io.File

class CatalogAuditPipeline(
    private val reader: CatalogFoodItemReader,
    private val normalizer: CatalogFoodItemNormalizer,
    private val qualityValidator: CatalogQualityValidator,
    private val duplicateDetector: CatalogDuplicateDetector,
    private val categoryValidator: CatalogCategoryValidator,
    private val languageValidator: CatalogLanguageValidator,
    private val nonFoodDetector: CatalogNonFoodDetector,
    private val canonicalizationPlanner: CatalogCanonicalizationPlanner,
    private val reportWriters: CatalogAuditReportWriters,
    private val categoryRegistry: CanonicalFoodCategoryRegistry =
        CanonicalFoodCategoryRegistry()
) {

    fun run(
        catalogFile: File,
        outputDirectory: File
    ): CatalogAuditPipelineResult {
        validateInputPaths(
            catalogFile = catalogFile,
            outputDirectory = outputDirectory
        )

        val entries = reader.read(catalogFile)

        validateEntries(entries)

        val normalizations = normalizer.normalizeAll(entries)

        validateNormalizationCoverage(
            entries = entries,
            normalizationSourceIndices =
                normalizations.map { it.sourceIndex }
        )

        val qualityResult = qualityValidator.validate(
            entries = entries,
            normalizations = normalizations
        )

        val duplicateGroups = duplicateDetector.detect(
            entries = entries,
            normalizations = normalizations
        )

        val categoryResult = categoryValidator.validate(
            entries = entries,
            registry = categoryRegistry
        )

        val languageResult = languageValidator.validate(
            entries = entries,
            normalizations = normalizations
        )

        val nonFoodCandidates = nonFoodDetector.detect(entries)

        val canonicalizationPlan =
            canonicalizationPlanner.createPlan(
                entries = entries,
                normalizations = normalizations,
                duplicateGroups = duplicateGroups,
                categoryResult = categoryResult,
                languageResult = languageResult,
                nonFoodCandidates = nonFoodCandidates
            )

        validatePipelineConsistency(
            inputEntryCount = entries.size,
            qualityInputEntryCount =
                qualityResult.inputEntryCount,
            categoryInputEntryCount =
                categoryResult.inputEntryCount,
            languageInputEntryCount =
                languageResult.inputEntryCount,
            planInputEntryCount =
                canonicalizationPlan.inputEntryCount,
            planEntryCount =
                canonicalizationPlan.planEntryCount
        )

        val reportFiles = createReportFiles(outputDirectory)

        reportWriters.qualityReportWriter.write(
            result = qualityResult,
            outputFile = reportFiles.getValue(
                REPORT_KEY_QUALITY
            )
        )

        reportWriters.duplicateGroupsReportWriter.write(
            groups = duplicateGroups,
            outputFile = reportFiles.getValue(
                REPORT_KEY_DUPLICATES
            )
        )

        reportWriters.categoryReportWriter.write(
            result = categoryResult,
            outputFile = reportFiles.getValue(
                REPORT_KEY_CATEGORY
            )
        )

        reportWriters.languageIssuesReportWriter.write(
            result = languageResult,
            outputFile = reportFiles.getValue(
                REPORT_KEY_LANGUAGE
            )
        )

        reportWriters.nonFoodCandidatesReportWriter.write(
            candidates = nonFoodCandidates,
            outputFile = reportFiles.getValue(
                REPORT_KEY_NON_FOOD
            )
        )

        reportWriters.canonicalizationPlanReportWriter.write(
            plan = canonicalizationPlan,
            outputFile = reportFiles.getValue(
                REPORT_KEY_CANONICALIZATION
            )
        )

        reportWriters.normalizationReportWriter.write(
            normalizations = normalizations,
            outputFile = reportFiles.getValue(
                REPORT_KEY_NORMALIZATION
            )
        )

        reportWriters.taxonomyGapReportWriter.write(
            registry = categoryRegistry,
            categoryResult = categoryResult,
            outputFile = reportFiles.getValue(
                REPORT_KEY_TAXONOMY_GAPS
            )
        )

        validateWrittenReports(reportFiles)

        val structurallyValid =
            canonicalizationPlan.valid &&
                    normalizations.size == entries.size &&
                    canonicalizationPlan.planEntryCount ==
                    entries.size

        return CatalogAuditPipelineResult(
            inputCatalogFile = catalogFile.canonicalFile,
            outputDirectory = outputDirectory.canonicalFile,
            inputEntryCount = entries.size,
            entries = entries,
            normalizations = normalizations,
            qualityResult = qualityResult,
            duplicateGroups = duplicateGroups,
            categoryResult = categoryResult,
            languageResult = languageResult,
            nonFoodCandidates = nonFoodCandidates,
            canonicalizationPlan = canonicalizationPlan,
            reportFiles = reportFiles,
            valid = canonicalizationPlan.valid
        )
    }

    private fun validateInputPaths(
        catalogFile: File,
        outputDirectory: File
    ) {
        require(catalogFile.path.isNotBlank()) {
            "Catalog file path must not be blank."
        }

        require(catalogFile.exists()) {
            "Catalog file does not exist: ${catalogFile.path}"
        }

        require(catalogFile.isFile) {
            "Catalog input path is not a file: ${catalogFile.path}"
        }

        require(catalogFile.canRead()) {
            "Catalog file is not readable: ${catalogFile.path}"
        }

        require(outputDirectory.path.isNotBlank()) {
            "Output directory path must not be blank."
        }

        require(
            !outputDirectory.exists() ||
                    outputDirectory.isDirectory
        ) {
            "Audit output path is not a directory: " +
                    outputDirectory.path
        }

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Failed to create audit output directory: " +
                        outputDirectory.path
            }
        }

        require(outputDirectory.canWrite()) {
            "Audit output directory is not writable: " +
                    outputDirectory.path
        }

        require(
            catalogFile.absoluteFile !=
                    outputDirectory.absoluteFile
        ) {
            "Catalog file and output directory must be different paths."
        }
    }

    private fun validateEntries(
        entries: List<
                de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
                >
    ) {
        val duplicateSourceIndices = entries
            .groupBy { it.sourceIndex }
            .filterValues { it.size > 1 }
            .keys
            .sorted()

        require(duplicateSourceIndices.isEmpty()) {
            "Catalog reader returned duplicate sourceIndex values: " +
                    duplicateSourceIndices.joinToString(", ")
        }

        entries.forEach { entry ->
            require(entry.sourceIndex >= 0) {
                "Catalog entry sourceIndex must not be negative."
            }

            require(entry.item.itemname.isNotBlank()) {
                "Catalog entry itemname must not be blank at sourceIndex " +
                        "${entry.sourceIndex}."
            }
        }
    }

    private fun validateNormalizationCoverage(
        entries: List<
                de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
                >,
        normalizationSourceIndices: List<Int>
    ) {
        val entryIndices = entries
            .mapTo(sortedSetOf()) { it.sourceIndex }

        val normalizationIndices =
            normalizationSourceIndices.toSortedSet()

        require(
            normalizationSourceIndices.distinct().size ==
                    normalizationSourceIndices.size
        ) {
            "Catalog normalizations contain duplicate sourceIndex values."
        }

        val missingNormalizationIndices =
            entryIndices - normalizationIndices

        val unexpectedNormalizationIndices =
            normalizationIndices - entryIndices

        require(missingNormalizationIndices.isEmpty()) {
            "Missing normalizations for source indices: " +
                    missingNormalizationIndices.joinToString(", ")
        }

        require(unexpectedNormalizationIndices.isEmpty()) {
            "Normalizations reference unknown source indices: " +
                    unexpectedNormalizationIndices.joinToString(", ")
        }
    }

    private fun validatePipelineConsistency(
        inputEntryCount: Int,
        qualityInputEntryCount: Int,
        categoryInputEntryCount: Int,
        languageInputEntryCount: Int,
        planInputEntryCount: Int,
        planEntryCount: Int
    ) {
        require(qualityInputEntryCount == inputEntryCount) {
            "Quality validation input count $qualityInputEntryCount " +
                    "does not match catalog count $inputEntryCount."
        }

        require(categoryInputEntryCount == inputEntryCount) {
            "Category validation input count $categoryInputEntryCount " +
                    "does not match catalog count $inputEntryCount."
        }

        require(languageInputEntryCount == inputEntryCount) {
            "Language validation input count $languageInputEntryCount " +
                    "does not match catalog count $inputEntryCount."
        }

        require(planInputEntryCount == inputEntryCount) {
            "Canonicalization plan input count $planInputEntryCount " +
                    "does not match catalog count $inputEntryCount."
        }

        require(planEntryCount == inputEntryCount) {
            "Canonicalization plan entry count $planEntryCount " +
                    "does not match catalog count $inputEntryCount."
        }
    }

    private fun createReportFiles(
        outputDirectory: File
    ): Map<String, File> =
        listOf(
            REPORT_KEY_CANONICALIZATION to
                    File(
                        outputDirectory,
                        FILE_CANONICALIZATION_PLAN
                    ),

            REPORT_KEY_CATEGORY to
                    File(
                        outputDirectory,
                        FILE_CATEGORY_REPORT
                    ),

            REPORT_KEY_DUPLICATES to
                    File(
                        outputDirectory,
                        FILE_DUPLICATE_GROUPS
                    ),

            REPORT_KEY_LANGUAGE to
                    File(
                        outputDirectory,
                        FILE_LANGUAGE_ISSUES
                    ),

            REPORT_KEY_NON_FOOD to
                    File(
                        outputDirectory,
                        FILE_NON_FOOD_CANDIDATES
                    ),

            REPORT_KEY_NORMALIZATION to
                    File(
                        outputDirectory,
                        FILE_NORMALIZATION_REPORT
                    ),

            REPORT_KEY_QUALITY to
                    File(
                        outputDirectory,
                        FILE_QUALITY_REPORT
                    ),

            REPORT_KEY_TAXONOMY_GAPS to
                    File(
                        outputDirectory,
                        FILE_TAXONOMY_GAPS
                    )
        )
            .sortedBy { it.first }
            .associate { it }

    private fun validateWrittenReports(
        reportFiles: Map<String, File>
    ) {
        val missingReports = reportFiles
            .filterValues { !it.exists() }
            .keys
            .sorted()

        require(missingReports.isEmpty()) {
            "Catalog audit did not create reports: " +
                    missingReports.joinToString(", ")
        }

        val invalidReports = reportFiles
            .filterValues { file ->
                !file.isFile ||
                        !file.canRead() ||
                        file.length() <= 0L
            }
            .keys
            .sorted()

        require(invalidReports.isEmpty()) {
            "Catalog audit created invalid or empty reports: " +
                    invalidReports.joinToString(", ")
        }
    }

    private companion object {

        const val REPORT_KEY_QUALITY = "quality"
        const val REPORT_KEY_DUPLICATES = "duplicates"
        const val REPORT_KEY_CATEGORY = "category"
        const val REPORT_KEY_LANGUAGE = "language"
        const val REPORT_KEY_NON_FOOD = "non-food"
        const val REPORT_KEY_CANONICALIZATION =
            "canonicalization"
        const val REPORT_KEY_NORMALIZATION =
            "normalization"
        const val REPORT_KEY_TAXONOMY_GAPS =
            "taxonomy-gaps"

        const val FILE_QUALITY_REPORT =
            "catalog-quality-report.json"

        const val FILE_DUPLICATE_GROUPS =
            "catalog-duplicate-groups.json"

        const val FILE_CATEGORY_REPORT =
            "catalog-category-report.json"

        const val FILE_LANGUAGE_ISSUES =
            "catalog-language-issues.json"

        const val FILE_NON_FOOD_CANDIDATES =
            "catalog-non-food-candidates.json"

        const val FILE_CANONICALIZATION_PLAN =
            "catalog-canonicalization-plan.json"

        const val FILE_NORMALIZATION_REPORT =
            "catalog-normalization-report.json"

        const val FILE_TAXONOMY_GAPS =
            "catalog-taxonomy-gaps.json"
    }
}