package de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution

object OFFNutritionReferenceQualityDistributionBuckets {

    fun maximumExcessBucket(
        excess: Double
    ): String =
        when {
            excess <= 0.0 ->
                "NOT_ABOVE_MAXIMUM"

            excess <= 0.5 ->
                "GT_0_TO_0_5"

            excess <= 1.0 ->
                "GT_0_5_TO_1"

            excess <= 5.0 ->
                "GT_1_TO_5"

            excess <= 20.0 ->
                "GT_5_TO_20"

            excess <= 100.0 ->
                "GT_20_TO_100"

            else ->
                "GT_100"
        }

    fun relationshipExcessBucket(
        excess: Double
    ): String =
        when {
            excess <= 0.5 ->
                "LTE_TOLERANCE"

            excess <= 1.0 ->
                "GT_0_5_TO_1"

            excess <= 5.0 ->
                "GT_1_TO_5"

            excess <= 20.0 ->
                "GT_5_TO_20"

            else ->
                "GT_20"
        }

    fun macronutrientSumBucket(
        sum: Double
    ): String =
        when {
            sum <= 100.0 ->
                "LTE_100"

            sum <= 105.0 ->
                "GT_100_TO_105"

            sum <= 110.0 ->
                "GT_105_TO_110"

            sum <= 120.0 ->
                "GT_110_TO_120"

            sum <= 150.0 ->
                "GT_120_TO_150"

            else ->
                "GT_150"
        }

    fun keyCountBucket(
        keyCount: Int
    ): String =
        when (keyCount) {
            0 ->
                "0"

            1 ->
                "1"

            2 ->
                "2"

            3 ->
                "3"

            4 ->
                "4"

            5 ->
                "5"

            6 ->
                "6"

            7 ->
                "7"

            else ->
                "8_OR_MORE"
        }
}