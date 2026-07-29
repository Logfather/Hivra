package de.shopme.tools.knowledge.off.nutrition.reference.quality.policy

data class OFFNutritionReferenceQualityPolicy(
    val version: Int,
    val policyStatus: OFFNutritionReferenceQualityPolicyStatus,
    val thresholdDecision:
    OFFNutritionReferenceQualityThresholdDecision,
    val zeroOnlyDecision:
    OFFNutritionReferenceQualityZeroOnlyDecision,
    val input: OFFNutritionReferenceQualityPolicyInput,
    val thresholds:
    OFFNutritionReferenceQualityPolicyThresholds,
    val evidence:
    OFFNutritionReferenceQualityPolicyEvidence,
    val rationale:
    OFFNutritionReferenceQualityPolicyRationale
) {

    init {
        require(version > 0)
    }

    companion object {

        const val CURRENT_VERSION =
            1
    }
}