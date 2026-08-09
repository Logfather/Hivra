package de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate

data class CanonicalExpansionCandidateCategoryResult(
    val category: String,

    val baselineEntryCount: Int,
    val derivedTargetEntryCount: Int,
    val requiredExpansionEntryCount: Int,

    val productFamilyCount: Int,

    val families:
    List<CanonicalExpansionCandidateFamilyResult>,

    val generatedCandidateCount: Int,

    val complete: Boolean
) {

    init {
        require(category.isNotBlank())

        require(baselineEntryCount >= 0)
        require(derivedTargetEntryCount >= baselineEntryCount)

        require(
            requiredExpansionEntryCount ==
                    derivedTargetEntryCount -
                    baselineEntryCount
        )

        require(productFamilyCount == families.size)
        require(productFamilyCount > 0)

        require(
            families ==
                    families.sortedBy {
                        it.familyKey
                    }
        )

        require(
            families.map { it.familyKey }
                .distinct()
                .size ==
                    families.size
        )

        require(
            families.all {
                it.category == category
            }
        )

        require(
            families.sumOf {
                it.estimatedBaselineFamilyEntryCount
            } ==
                    baselineEntryCount
        )

        require(
            families.sumOf {
                it.derivedTargetEntryCount
            } ==
                    derivedTargetEntryCount
        )

        require(
            families.sumOf {
                it.requiredExpansionEntryCount
            } ==
                    requiredExpansionEntryCount
        )

        require(
            generatedCandidateCount ==
                    families.sumOf {
                        it.generatedCandidateCount
                    }
        )

        require(
            complete ==
                    (
                            generatedCandidateCount ==
                                    requiredExpansionEntryCount &&
                                    families.all {
                                        it.complete
                                    }
                            )
        )
    }
}