package de.shopme.tools.knowledge.off.nutrition.reference.deduplication

data class OFFNutritionReferenceDeduplicationKey(
    val canonicalId: String,
    val nutritionFingerprint: String
) {

    init {
        require(canonicalId.isNotBlank()) {
            "OFF nutrition deduplication canonicalId must not be blank."
        }

        require(nutritionFingerprint.isNotBlank()) {
            "OFF nutrition deduplication fingerprint must not be blank."
        }
    }
}