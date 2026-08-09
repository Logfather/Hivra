package de.shopme.testing.system.tools.knowledge.catalog.expansion.variant

data class CanonicalProductFamilyVariantCoverage(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val baselineEntryCount: Int,

    val sourceProductFamilyTargetEntryCount: Int,

    /**
     * Noch unveränderter Zielwert aus der Produktfamilienabdeckung.
     *
     * Dieser Commit definiert die Variantenabdeckung, leitet aber noch
     * keinen neuen endgültigen Zielbestand ab.
     */
    val currentRecommendedTargetEntryCount: Int,

    val minimumAllowedTargetEntryCount: Int,
    val maximumAllowedTargetEntryCount: Int,

    val categoryCount: Int,
    val productFamilyCount: Int,

    val categories:
    List<CanonicalCategoryVariantCoverage>,

    val totalAxisRequirementCount: Int,
    val totalRecommendedVariantValueCoverageCount: Int,

    val requiredAxisCount: Int,
    val recommendedAxisCount: Int,
    val optionalAxisCount: Int,

    val familiesWithCompleteVariantCoverageCount: Int,
    val familiesWithIncompleteVariantCoverageCount: Int,

    /**
     * true bedeutet:
     * Die Variantenstruktur ist vollständig genug, damit ein nachgelagerter
     * Planner einen fachlich begründeten Gesamtzielbestand zwischen 10.000
     * und 15.000 ableiten kann.
     */
    val readyForTargetDerivation: Boolean,

    val blockers: List<String>,

    val valid: Boolean
) {

    init {
        require(version > 0)
        require(sourceBaselineId.isNotBlank())

        require(
            SHA_256_REGEX.matches(
                sourceBaselineCatalogSha256
            )
        )

        require(baselineEntryCount > 0)

        require(sourceProductFamilyTargetEntryCount > baselineEntryCount)

        require(
            currentRecommendedTargetEntryCount ==
                    sourceProductFamilyTargetEntryCount
        )

        require(
            minimumAllowedTargetEntryCount == 10_000
        )

        require(
            maximumAllowedTargetEntryCount == 15_000
        )

        require(
            currentRecommendedTargetEntryCount in
                    minimumAllowedTargetEntryCount..
                    maximumAllowedTargetEntryCount
        )

        require(categoryCount == categories.size)

        require(
            productFamilyCount ==
                    categories.sumOf {
                        it.productFamilyCount
                    }
        )

        require(
            categories ==
                    categories.sortedBy {
                        it.category
                    }
        )

        require(
            categories.map { it.category }
                .distinct()
                .size ==
                    categories.size
        )

        require(
            totalAxisRequirementCount ==
                    categories.sumOf {
                        it.totalAxisRequirementCount
                    }
        )

        require(
            totalRecommendedVariantValueCoverageCount ==
                    categories.sumOf {
                        it.totalRecommendedVariantValueCoverageCount
                    }
        )

        val allFamilies =
            categories.flatMap {
                it.families
            }

        require(
            requiredAxisCount ==
                    allFamilies.sumOf {
                        it.requiredAxisCount
                    }
        )

        require(
            recommendedAxisCount ==
                    allFamilies.sumOf {
                        it.recommendedAxisCount
                    }
        )

        require(
            optionalAxisCount ==
                    allFamilies.sumOf {
                        it.optionalAxisCount
                    }
        )

        require(
            totalAxisRequirementCount ==
                    requiredAxisCount +
                    recommendedAxisCount +
                    optionalAxisCount
        )

        require(
            familiesWithCompleteVariantCoverageCount ==
                    allFamilies.count {
                        it.completeAxisCoverage
                    }
        )

        require(
            familiesWithIncompleteVariantCoverageCount ==
                    allFamilies.count {
                        !it.completeAxisCoverage
                    }
        )

        require(
            productFamilyCount ==
                    familiesWithCompleteVariantCoverageCount +
                    familiesWithIncompleteVariantCoverageCount
        )

        require(
            blockers ==
                    blockers
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        require(
            readyForTargetDerivation ==
                    (
                            blockers.isEmpty() &&
                                    familiesWithIncompleteVariantCoverageCount == 0
                            )
        )

        require(
            valid ==
                    readyForTargetDerivation
        )
    }

    companion object {
        const val CURRENT_VERSION = 1

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}