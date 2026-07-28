package de.shopme.tools.knowledge.off.nutrition.reference.quality

data class OFFNutritionReferenceQualityRejection(
    val sourceId: String,
    val canonicalId: String,
    val reasons: List<OFFNutritionReferenceQualityRejectionReason>
) {

    init {
        require(sourceId.isNotBlank()) {
            "Rejected OFF nutrition reference candidate sourceId " +
                    "must not be blank."
        }

        require(canonicalId.isNotBlank()) {
            "Rejected OFF nutrition reference candidate canonicalId " +
                    "must not be blank."
        }

        require(reasons.isNotEmpty()) {
            "Rejected OFF nutrition reference candidate must have " +
                    "at least one rejection reason."
        }

        require(
            reasons == reasons.distinct()
        ) {
            "OFF nutrition reference rejection reasons must not " +
                    "contain duplicates."
        }

        require(
            reasons ==
                    reasons.sortedBy(
                        OFFNutritionReferenceQualityRejectionReason::name
                    )
        ) {
            "OFF nutrition reference rejection reasons must be " +
                    "sorted deterministically."
        }

        require(sourceId == sourceId.trim()) {
            "Rejected OFF nutrition reference candidate sourceId " +
                    "must already be normalized."
        }

        require(canonicalId == canonicalId.trim()) {
            "Rejected OFF nutrition reference candidate canonicalId " +
                    "must already be normalized."
        }
    }
}