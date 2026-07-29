package de.shopme.tools.knowledge.off.nutrition.reference.quality.policy

data class OFFNutritionReferenceQualityPolicyRationale(
    val summary: String,
    val thresholdRationale: List<String>,
    val zeroOnlyRationale: List<String>,
    val deferredActions: List<String>
) {

    init {
        require(summary.isNotBlank())
        require(thresholdRationale.isNotEmpty())
        require(zeroOnlyRationale.isNotEmpty())

        require(
            thresholdRationale.none(String::isBlank)
        )

        require(
            zeroOnlyRationale.none(String::isBlank)
        )

        require(
            deferredActions.none(String::isBlank)
        )
    }
}