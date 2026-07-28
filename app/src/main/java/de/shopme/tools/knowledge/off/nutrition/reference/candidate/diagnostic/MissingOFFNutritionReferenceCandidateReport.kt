package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic

data class MissingOFFNutritionReferenceCandidateReport(
    val version: Int,
    val requestCount: Int,
    val referenceCandidateGapCount: Int,
    val traceCount: Int,

    val countsByFirstMissingStage:
    Map<MissingOFFNutritionReferenceCandidateStage, Int>,

    val countsByIdentityRejectionReason:
    Map<String, Int>,

    val countsByNutritionRejectionReason:
    Map<String, Int>,

    val countsByReferenceEligibilityRejectionReason:
    Map<String, Int>,

    val findings:
    List<MissingOFFNutritionReferenceCandidateFinding>
) {

    init {
        require(version == CURRENT_VERSION) {
            "Unsupported report version: $version."
        }

        require(requestCount >= 0)
        require(referenceCandidateGapCount >= 0)
        require(traceCount >= 0)

        require(referenceCandidateGapCount == findings.size) {
            "referenceCandidateGapCount must equal findings.size."
        }

        require(
            countsByFirstMissingStage.values.sum() ==
                    referenceCandidateGapCount
        ) {
            "Stage counts must equal referenceCandidateGapCount."
        }

        require(
            findings ==
                    findings.sortedWith(
                        compareBy<MissingOFFNutritionReferenceCandidateFinding>(
                            { it.catalogIndex },
                            { it.catalogKey }
                        )
                    )
        ) {
            "findings must be deterministically sorted."
        }

        require(
            findings
                .map { it.catalogIndex }
                .distinct()
                .size == findings.size
        ) {
            "Duplicate catalogIndex values detected."
        }

        require(
            findings
                .map { it.catalogKey }
                .distinct()
                .size == findings.size
        ) {
            "Duplicate catalogKey values detected."
        }

        requireReasonCounts(
            countsByIdentityRejectionReason,
            "countsByIdentityRejectionReason"
        )

        requireReasonCounts(
            countsByNutritionRejectionReason,
            "countsByNutritionRejectionReason"
        )

        requireReasonCounts(
            countsByReferenceEligibilityRejectionReason,
            "countsByReferenceEligibilityRejectionReason"
        )
    }

    private fun requireReasonCounts(
        values: Map<String, Int>,
        fieldName: String
    ) {

        require(
            values.keys.all {
                it.isNotBlank() &&
                        it == it.trim()
            }
        ) {
            "$fieldName contains blank keys."
        }

        require(
            values.values.all { it > 0 }
        ) {
            "$fieldName contains non-positive counts."
        }
    }

    companion object {

        const val CURRENT_VERSION =
            1
    }
}