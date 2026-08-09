package de.shopme.testing.system.tools.knowledge.catalog.review.reclassification

data class CatalogMisclassifiedTypoReclassificationResult(
    val version: Int,

    val inputCandidateCount: Int,
    val reclassifiedEntryCount: Int,

    val preserveSeparateFoodCount: Int,
    val manualReviewRequiredCount: Int,

    val countsByClass:
    Map<CatalogMisclassifiedTypoClass, Int>,

    val countsByRecommendation:
    Map<CatalogMisclassifiedTypoRecommendation, Int>,

    val countsBySourceCategory:
    Map<String, Int>,

    val entries:
    List<CatalogMisclassifiedTypoEntry>,

    val valid: Boolean
) {

    init {
        require(version > 0)
        require(inputCandidateCount >= 0)
        require(reclassifiedEntryCount >= 0)

        require(
            inputCandidateCount ==
                    reclassifiedEntryCount
        ) {
            "Every input candidate must be reclassified."
        }

        require(
            reclassifiedEntryCount ==
                    entries.size
        )

        require(
            preserveSeparateFoodCount ==
                    entries.count {
                        it.recommendation ==
                                CatalogMisclassifiedTypoRecommendation
                                    .PRESERVE_AS_SEPARATE_FOODS
                    }
        )

        require(
            manualReviewRequiredCount ==
                    entries.count {
                        it.recommendation ==
                                CatalogMisclassifiedTypoRecommendation
                                    .MANUAL_REVIEW_REQUIRED
                    }
        )

        require(
            countsByClass.values.sum() ==
                    reclassifiedEntryCount
        )

        require(
            countsByRecommendation.values.sum() ==
                    reclassifiedEntryCount
        )

        require(
            countsBySourceCategory.values.sum() ==
                    reclassifiedEntryCount
        )

        require(
            entries ==
                    entries.sortedBy { it.sourceIndex }
        )

        require(
            entries.map { it.sourceIndex }
                .distinct()
                .size ==
                    entries.size
        )

        require(valid)
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}