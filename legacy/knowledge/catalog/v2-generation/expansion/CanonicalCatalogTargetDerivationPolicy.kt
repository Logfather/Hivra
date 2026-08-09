package de.shopme.testing.system.tools.knowledge.catalog.expansion.target

import kotlin.math.max
import kotlin.math.min

object CanonicalCatalogTargetDerivationPolicy {

    const val MINIMUM_TARGET_ENTRY_COUNT = 10_000
    const val MAXIMUM_TARGET_ENTRY_COUNT = 15_000

    /*
     * Ein konservativer Grundaufschlag von fünf Prozent berücksichtigt,
     * dass die bisherige 10.000er-Verteilung vor der konkreten
     * Variantenanalyse entstanden ist.
     */
    const val BASE_UPLIFT_BASIS_POINTS = 500

    /*
     * Höchstens 15 Prozentpunkte dürfen aus der durchschnittlichen
     * Variantenwertdichte entstehen.
     */
    const val MAX_VARIANT_DENSITY_CONTRIBUTION_BASIS_POINTS =
        1_500

    /*
     * Pflichtachsen können höchstens acht Prozentpunkte beitragen.
     */
    const val REQUIRED_AXIS_CONTRIBUTION_BASIS_POINTS =
        800

    /*
     * Kombinierbare Achsen können höchstens fünf Prozentpunkte beitragen.
     */
    const val CROSS_AXIS_CONTRIBUTION_BASIS_POINTS =
        500

    /*
     * Optionale Achsen reduzieren den Aufschlag geringfügig, da ihre
     * Ausprägungen nicht zwingend vollständig materialisiert werden.
     */
    const val OPTIONAL_AXIS_PENALTY_BASIS_POINTS =
        300

    const val MAXIMUM_TOTAL_UPLIFT_BASIS_POINTS =
        5_000

    fun deriveUpliftBasisPoints(
        totalAxisRequirementCount: Int,
        totalRecommendedVariantValueCoverageCount: Int,
        requiredAxisCount: Int,
        optionalAxisCount: Int,
        crossAxisCombinationAllowedCount: Int
    ): Int {
        require(totalAxisRequirementCount > 0)

        require(
            totalRecommendedVariantValueCoverageCount >=
                    totalAxisRequirementCount
        )

        require(requiredAxisCount >= 0)
        require(optionalAxisCount >= 0)
        require(crossAxisCombinationAllowedCount >= 0)

        val excessRecommendedVariantValues =
            totalRecommendedVariantValueCoverageCount -
                    totalAxisRequirementCount

        val variantDensityContribution =
            min(
                MAX_VARIANT_DENSITY_CONTRIBUTION_BASIS_POINTS,
                (
                        excessRecommendedVariantValues.toLong() *
                                500L /
                                totalAxisRequirementCount.toLong()
                        ).toInt()
            )

        val requiredAxisContribution =
            (
                    requiredAxisCount.toLong() *
                            REQUIRED_AXIS_CONTRIBUTION_BASIS_POINTS.toLong() /
                            totalAxisRequirementCount.toLong()
                    ).toInt()

        val crossAxisContribution =
            (
                    crossAxisCombinationAllowedCount.toLong() *
                            CROSS_AXIS_CONTRIBUTION_BASIS_POINTS.toLong() /
                            totalAxisRequirementCount.toLong()
                    ).toInt()

        val optionalAxisPenalty =
            (
                    optionalAxisCount.toLong() *
                            OPTIONAL_AXIS_PENALTY_BASIS_POINTS.toLong() /
                            totalAxisRequirementCount.toLong()
                    ).toInt()

        return min(
            MAXIMUM_TOTAL_UPLIFT_BASIS_POINTS,
            max(
                0,
                BASE_UPLIFT_BASIS_POINTS +
                        variantDensityContribution +
                        requiredAxisContribution +
                        crossAxisContribution -
                        optionalAxisPenalty
            )
        )
    }

    fun deriveTargetEntryCount(
        currentTargetEntryCount: Int,
        upliftBasisPoints: Int
    ): Int {
        require(
            currentTargetEntryCount in
                    MINIMUM_TARGET_ENTRY_COUNT..
                    MAXIMUM_TARGET_ENTRY_COUNT
        )

        require(
            upliftBasisPoints in
                    0..MAXIMUM_TOTAL_UPLIFT_BASIS_POINTS
        )

        val numerator =
            currentTargetEntryCount.toLong() *
                    (
                            BASIS_POINT_DENOMINATOR +
                                    upliftBasisPoints
                            ).toLong()

        val roundedTarget =
            (
                    numerator +
                            BASIS_POINT_DENOMINATOR / 2L
                    ) /
                    BASIS_POINT_DENOMINATOR.toLong()

        return roundedTarget
            .toInt()
            .coerceIn(
                MINIMUM_TARGET_ENTRY_COUNT,
                MAXIMUM_TARGET_ENTRY_COUNT
            )
    }

    private const val BASIS_POINT_DENOMINATOR =
        10_000
}