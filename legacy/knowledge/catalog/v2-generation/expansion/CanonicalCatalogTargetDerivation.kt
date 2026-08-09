package de.shopme.testing.system.tools.knowledge.catalog.expansion.target

data class CanonicalCatalogTargetDerivation(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val baselineEntryCount: Int,

    val sourceTargetEntryCount: Int,
    val derivedTargetEntryCount: Int,

    val minimumAllowedTargetEntryCount: Int,
    val maximumAllowedTargetEntryCount: Int,

    val upliftBasisPoints: Int,
    val upliftShare: Double,

    val additionalDerivedEntryCount: Int,
    val requiredExpansionFromBaselineEntryCount: Int,

    val categoryCount: Int,
    val productFamilyCount: Int,

    val totalAxisRequirementCount: Int,
    val totalRecommendedVariantValueCoverageCount: Int,

    val requiredAxisCount: Int,
    val recommendedAxisCount: Int,
    val optionalAxisCount: Int,

    val crossAxisCombinationAllowedCount: Int,
    val crossAxisCombinationRestrictedCount: Int,

    val categories:
    List<CanonicalDerivedCategoryTarget>,

    val derivedTargetCountsByCategory:
    Map<String, Int>,

    val additionalCountsByCategory:
    Map<String, Int>,

    val completeAdditionalAllocation: Boolean,
    val withinAllowedTargetRange: Boolean,

    val derivationMethod: String,

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
        require(sourceTargetEntryCount >= baselineEntryCount)

        require(
            minimumAllowedTargetEntryCount ==
                    CanonicalCatalogTargetDerivationPolicy
                        .MINIMUM_TARGET_ENTRY_COUNT
        )

        require(
            maximumAllowedTargetEntryCount ==
                    CanonicalCatalogTargetDerivationPolicy
                        .MAXIMUM_TARGET_ENTRY_COUNT
        )

        require(
            upliftBasisPoints in
                    0..
                    CanonicalCatalogTargetDerivationPolicy
                        .MAXIMUM_TOTAL_UPLIFT_BASIS_POINTS
        )

        require(upliftShare in 0.0..0.5)

        require(
            additionalDerivedEntryCount ==
                    derivedTargetEntryCount -
                    sourceTargetEntryCount
        )

        require(
            requiredExpansionFromBaselineEntryCount ==
                    derivedTargetEntryCount -
                    baselineEntryCount
        )

        require(categoryCount == categories.size)

        require(
            productFamilyCount ==
                    categories.sumOf {
                        it.productFamilyCount
                    }
        )

        require(totalAxisRequirementCount > 0)

        require(
            totalRecommendedVariantValueCoverageCount >=
                    totalAxisRequirementCount
        )

        require(requiredAxisCount >= 0)
        require(recommendedAxisCount >= 0)
        require(optionalAxisCount >= 0)

        require(
            totalAxisRequirementCount ==
                    requiredAxisCount +
                    recommendedAxisCount +
                    optionalAxisCount
        )

        require(crossAxisCombinationAllowedCount >= 0)
        require(crossAxisCombinationRestrictedCount >= 0)

        require(
            totalAxisRequirementCount ==
                    crossAxisCombinationAllowedCount +
                    crossAxisCombinationRestrictedCount
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
            categories.sumOf {
                it.sourceTargetEntryCount
            } ==
                    sourceTargetEntryCount
        )

        require(
            categories.sumOf {
                it.additionalDerivedEntryCount
            } ==
                    additionalDerivedEntryCount
        )

        require(
            categories.sumOf {
                it.derivedTargetEntryCount
            } ==
                    derivedTargetEntryCount
        )

        require(
            derivedTargetCountsByCategory ==
                    derivedTargetCountsByCategory
                        .toSortedMap()
        )

        require(
            derivedTargetCountsByCategory.values.sum() ==
                    derivedTargetEntryCount
        )

        require(
            additionalCountsByCategory ==
                    additionalCountsByCategory
                        .toSortedMap()
        )

        require(
            additionalCountsByCategory.values.sum() ==
                    additionalDerivedEntryCount
        )

        require(
            completeAdditionalAllocation ==
                    (
                            categories.sumOf {
                                it.additionalDerivedEntryCount
                            } ==
                                    additionalDerivedEntryCount
                            )
        )

        require(
            withinAllowedTargetRange ==
                    (
                            derivedTargetEntryCount in
                                    minimumAllowedTargetEntryCount..
                                    maximumAllowedTargetEntryCount
                            )
        )

        require(derivationMethod.isNotBlank())

        require(
            blockers ==
                    blockers
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        require(
            valid ==
                    (
                            blockers.isEmpty() &&
                                    completeAdditionalAllocation &&
                                    withinAllowedTargetRange
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}