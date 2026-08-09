package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

data class CanonicalProductFamilyConcreteValueCoverage(
    val familyKey: String,
    val category: String,
    val displayName: String,

    val allocatedTargetEntryCount: Int,

    val axisCoverageCount: Int,

    val axisCoverages:
    List<CanonicalFamilyAxisValueCoverage>,

    val totalSelectedValueCount: Int,

    val allMinimumCoveragesReached: Boolean,
    val allRecommendedCoveragesReached: Boolean,
    val allMaximumLimitsRespected: Boolean,

    val valid: Boolean
) {

    init {
        require(familyKey.isNotBlank())
        require(category.isNotBlank())
        require(displayName.isNotBlank())

        require(allocatedTargetEntryCount > 0)

        require(axisCoverageCount == axisCoverages.size)
        require(axisCoverageCount > 0)

        require(
            axisCoverages ==
                    axisCoverages.sortedBy {
                        it.axis.name
                    }
        )

        require(
            axisCoverages.map { it.axis }
                .distinct()
                .size ==
                    axisCoverages.size
        )

        require(
            totalSelectedValueCount ==
                    axisCoverages.sumOf {
                        it.selectedValueCount
                    }
        )

        require(
            allMinimumCoveragesReached ==
                    axisCoverages.all {
                        it.minimumCoverageReached
                    }
        )

        require(
            allRecommendedCoveragesReached ==
                    axisCoverages.all {
                        it.recommendedCoverageReached
                    }
        )

        require(
            allMaximumLimitsRespected ==
                    axisCoverages.all {
                        it.maximumLimitRespected
                    }
        )

        require(
            valid ==
                    (
                            axisCoverages.all {
                                it.valid
                            } &&
                                    allMinimumCoveragesReached &&
                                    allRecommendedCoveragesReached &&
                                    allMaximumLimitsRespected
                            )
        )
    }
}