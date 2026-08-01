package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityRejectionReason

data class StreamingOFFNutritionReferenceQualityStatistics(
    val extractedCandidateCount: Long,
    val generatedCandidateCount: Long,
    val skippedWithoutNutritionCount: Long,
    val skippedInvalidIdentityCount: Long,
    val skippedInvalidNutritionPayloadCount: Long,
    val acceptedCandidateCount: Long,
    val rejectedCandidateCount: Long,
    val countsByReason:
    Map<OFFNutritionReferenceQualityRejectionReason, Long>
) {

    val rejectionReasonOccurrenceCount: Long
        get() =
            countsByReason.values.sum()

    val acceptanceRate: Double
        get() =
            if (generatedCandidateCount == 0L) {
                0.0
            } else {
                acceptedCandidateCount.toDouble() /
                        generatedCandidateCount.toDouble()
            }

    init {
        require(extractedCandidateCount >= 0L)
        require(generatedCandidateCount >= 0L)
        require(skippedWithoutNutritionCount >= 0L)
        require(skippedInvalidIdentityCount >= 0L)
        require(skippedInvalidNutritionPayloadCount >= 0L)
        require(acceptedCandidateCount >= 0L)
        require(rejectedCandidateCount >= 0L)

        require(
            generatedCandidateCount ==
                    acceptedCandidateCount + rejectedCandidateCount
        ) {
            "Generated candidates must equal accepted plus rejected: " +
                    "generated=$generatedCandidateCount, " +
                    "accepted=$acceptedCandidateCount, " +
                    "rejected=$rejectedCandidateCount."
        }

        require(
            countsByReason.keys ==
                    OFFNutritionReferenceQualityRejectionReason
                        .entries
                        .toSet()
        ) {
            "countsByReason must contain every rejection reason."
        }

        require(
            countsByReason.values.all { count ->
                count >= 0L
            }
        ) {
            "Rejection reason counts must not be negative."
        }
    }
}