package de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement

import kotlin.collections.sortedWith

data class CanonicalFoodCatalogRefinedTargetDistribution(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val sourceTargetEntryCount: Int,
    val refinedTargetEntryCount: Int,

    val baselineEntryCount: Int,
    val requiredExpansionEntryCount: Int,

    val categoryCount: Int,
    val changedCategoryCount: Int,

    val methodology:
    CanonicalFoodCatalogTargetMethodology,

    val categories:
    List<CanonicalCatalogCategoryScope>,

    val refinedTargetCountsByCategory:
    Map<String, Int>,

    val validationIssueCount: Int,
    val validationIssueCountsByType:
    Map<CanonicalTargetDistributionIssueType, Int>,

    val issues:
    List<CanonicalTargetDistributionIssue>,

    val blockers:
    List<String>,

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

        require(sourceTargetEntryCount == 10_000)
        require(refinedTargetEntryCount == 10_000)

        require(baselineEntryCount > 0)

        require(
            requiredExpansionEntryCount ==
                    refinedTargetEntryCount -
                    baselineEntryCount
        )

        require(categoryCount == categories.size)

        require(
            changedCategoryCount ==
                    categories.count {
                        it.targetChanged
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
            refinedTargetCountsByCategory ==
                    refinedTargetCountsByCategory
                        .toSortedMap()
        )

        require(
            refinedTargetCountsByCategory.values.sum() ==
                    refinedTargetEntryCount
        )

        require(
            refinedTargetCountsByCategory ==
                    categories.associate {
                        it.category to
                                it.refinedTargetEntryCount
                    }.toSortedMap()
        )

        require(validationIssueCount == issues.size)

        require(
            validationIssueCountsByType.values.sum() ==
                    validationIssueCount
        )

        require(
            issues ==
                    issues.sortedWith(
                        compareBy<
                                CanonicalTargetDistributionIssue
                                > {
                            it.severity.name
                        }.thenBy {
                            it.type.name
                        }.thenBy {
                            it.category.orEmpty()
                        }.thenBy {
                            it.message
                        }
                    )
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
            valid ==
                    (
                            blockers.isEmpty() &&
                                    issues.none {
                                        it.severity ==
                                                CanonicalTargetDistributionIssueSeverity
                                                    .ERROR
                                    }
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}