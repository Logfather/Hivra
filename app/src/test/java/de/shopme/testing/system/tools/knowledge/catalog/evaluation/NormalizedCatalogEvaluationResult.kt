package de.shopme.testing.system.tools.knowledge.catalog.evaluation

data class NormalizedCatalogEvaluationResult(
    val version: Int,
    val normalizedCatalogFile: String,
    val canonicalizationPlanFile: String,

    val entryCount: Int,
    val categoryCount: Int,

    val validationIssueCount: Int,
    val validationIssueCountsByType: Map<String, Int>,

    val duplicateNormalizedKeyGroupCount: Int,
    val duplicateNormalizedKeyEntryCount: Int,

    val duplicateItemNameGroupCount: Int,
    val duplicateItemNameEntryCount: Int,

    val missingNormalizedKeyCount: Int,
    val invalidNormalizedKeyCount: Int,

    val missingCategoryCount: Int,
    val unknownCategoryCount: Int,

    val missingPluralCount: Int,
    val lowercasePluralCount: Int,

    val emptyColloquialCount: Int,
    val emptyPhoneticTokensCount: Int,
    val emptyAutocompleteTokensCount: Int,

    val manualReviewActionCount: Int,
    val explicitReviewActionCount: Int,
    val mergeActionCount: Int,
    val removalActionCount: Int,
    val splitActionCount: Int,

    val categoryCounts: Map<String, Int>,
    val canonicalizationActionCounts: Map<String, Int>,

    val blockingReasons: List<String>,
    val reviewReasons: List<String>,

    val examples: List<NormalizedCatalogEvaluationExample>,

    val readinessStatus: NormalizedCatalogReadinessStatus
) {

    init {
        require(version > 0)

        require(normalizedCatalogFile.isNotBlank())
        require(canonicalizationPlanFile.isNotBlank())

        require(entryCount >= 0)
        require(categoryCount >= 0)

        require(validationIssueCount >= 0)
        require(
            validationIssueCountsByType.values.sum() ==
                    validationIssueCount
        ) {
            "validationIssueCountsByType must sum to validationIssueCount."
        }

        require(duplicateNormalizedKeyGroupCount >= 0)
        require(duplicateNormalizedKeyEntryCount >= 0)
        require(duplicateItemNameGroupCount >= 0)
        require(duplicateItemNameEntryCount >= 0)

        require(missingNormalizedKeyCount >= 0)
        require(invalidNormalizedKeyCount >= 0)
        require(missingCategoryCount >= 0)
        require(unknownCategoryCount >= 0)
        require(missingPluralCount >= 0)
        require(lowercasePluralCount >= 0)

        require(emptyColloquialCount >= 0)
        require(emptyPhoneticTokensCount >= 0)
        require(emptyAutocompleteTokensCount >= 0)

        require(manualReviewActionCount >= 0)
        require(explicitReviewActionCount >= 0)
        require(mergeActionCount >= 0)
        require(removalActionCount >= 0)
        require(splitActionCount >= 0)

        require(categoryCounts.values.sum() == entryCount) {
            "categoryCounts must cover every normalized catalog entry."
        }

        require(blockingReasons.none(String::isBlank))
        require(blockingReasons.distinct().size == blockingReasons.size)
        require(blockingReasons == blockingReasons.sorted())

        require(reviewReasons.none(String::isBlank))
        require(reviewReasons.distinct().size == reviewReasons.size)
        require(reviewReasons == reviewReasons.sorted())

        require(
            readinessStatus != NormalizedCatalogReadinessStatus.READY ||
                    (
                            validationIssueCount == 0 &&
                                    manualReviewActionCount == 0
                            )
        ) {
            "READY requires zero validation issues and zero manual reviews."
        }

        require(
            readinessStatus != NormalizedCatalogReadinessStatus.BLOCKED ||
                    validationIssueCount > 0
        ) {
            "BLOCKED requires at least one validation issue."
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}