package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlan
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateGroup
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.CatalogNonFoodCandidate
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationResult
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogQualityValidationResult
import java.io.File

data class CatalogAuditPipelineResult(
    val inputCatalogFile: File,
    val outputDirectory: File,
    val inputEntryCount: Int,
    val entries: List<IndexedCatalogFoodItem>,
    val normalizations: List<CatalogNormalizationResult>,
    val qualityResult: CatalogQualityValidationResult,
    val duplicateGroups: List<CatalogDuplicateGroup>,
    val categoryResult: CatalogCategoryValidationResult,
    val languageResult: CatalogLanguageValidationResult,
    val nonFoodCandidates: List<CatalogNonFoodCandidate>,
    val canonicalizationPlan: CatalogCanonicalizationPlan,
    val reportFiles: Map<String, File>,
    val valid: Boolean
) {

    init {
        require(inputCatalogFile.path.isNotBlank()) {
            "inputCatalogFile path must not be blank."
        }

        require(outputDirectory.path.isNotBlank()) {
            "outputDirectory path must not be blank."
        }

        require(inputEntryCount >= 0) {
            "inputEntryCount must not be negative."
        }

        require(normalizations.size == inputEntryCount) {
            "normalizations size must equal inputEntryCount."
        }

        require(
            normalizations.map { it.sourceIndex }.distinct().size ==
                    normalizations.size
        ) {
            "normalizations must have unique sourceIndex values."
        }

        require(
            qualityResult.inputEntryCount == inputEntryCount
        ) {
            "qualityResult inputEntryCount must equal inputEntryCount."
        }

        require(
            categoryResult.inputEntryCount == inputEntryCount
        ) {
            "categoryResult inputEntryCount must equal inputEntryCount."
        }

        require(
            languageResult.inputEntryCount == inputEntryCount
        ) {
            "languageResult inputEntryCount must equal inputEntryCount."
        }

        require(
            canonicalizationPlan.inputEntryCount == inputEntryCount
        ) {
            "canonicalizationPlan inputEntryCount must equal inputEntryCount."
        }

        require(
            canonicalizationPlan.planEntryCount == inputEntryCount
        ) {
            "canonicalizationPlan must contain exactly one entry per input."
        }

        require(
            duplicateGroups
                .flatMap { group ->
                    group.members.map { it.sourceIndex }
                }
                .groupingBy { it }
                .eachCount()
                .values
                .all { it == 1 }
        ) {
            "A sourceIndex must not belong to multiple duplicate groups."
        }

        require(
            nonFoodCandidates
                .map { it.sourceIndex }
                .distinct()
                .size ==
                    nonFoodCandidates.size
        ) {
            "nonFoodCandidates must have unique sourceIndex values."
        }

        require(reportFiles.keys.toList() == reportFiles.keys.sorted()) {
            "reportFiles must be sorted by key."
        }

        require(reportFiles.values.distinct().size == reportFiles.size) {
            "reportFiles must not contain duplicate file targets."
        }

        require(
            valid ==
                    (
                            canonicalizationPlan.valid &&
                                    normalizations.size == inputEntryCount &&
                                    canonicalizationPlan.planEntryCount ==
                                    inputEntryCount
                            )
        ) {
            "valid must reflect structural pipeline completeness."
        }
    }
}