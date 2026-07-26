package de.shopme.tools.knowledge.off.nutrition.reference.deduplication

data class OFFNutritionReferenceDuplicateGroup(
    val canonicalId: String,
    val nutritionFingerprint: String,
    val representativeSourceId: String,
    val mergedSourceIds: List<String>
) {

    init {
        require(canonicalId.isNotBlank()) {
            "Duplicate group canonicalId must not be blank."
        }

        require(nutritionFingerprint.isNotBlank()) {
            "Duplicate group nutritionFingerprint must not be blank."
        }

        require(representativeSourceId.isNotBlank()) {
            "Duplicate group representativeSourceId must not be blank."
        }

        require(mergedSourceIds.size >= 2) {
            "A duplicate group must contain at least two source IDs."
        }

        require(
            mergedSourceIds ==
                    mergedSourceIds.distinct().sorted()
        ) {
            "Duplicate group source IDs must be unique and sorted."
        }

        require(representativeSourceId in mergedSourceIds) {
            "Representative source ID must be part of mergedSourceIds."
        }
    }
}