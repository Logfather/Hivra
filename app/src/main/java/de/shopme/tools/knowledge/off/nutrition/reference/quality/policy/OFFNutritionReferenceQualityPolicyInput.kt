package de.shopme.tools.knowledge.off.nutrition.reference.quality.policy

data class OFFNutritionReferenceQualityPolicyInput(
    val inputFile: String,
    val inputFileSizeBytes: Long,
    val qualityReportVersion: Int,
    val distributionReportVersion: Int
) {

    init {
        require(inputFile.isNotBlank())
        require(inputFileSizeBytes > 0L)
        require(qualityReportVersion > 0)
        require(distributionReportVersion > 0)
    }
}