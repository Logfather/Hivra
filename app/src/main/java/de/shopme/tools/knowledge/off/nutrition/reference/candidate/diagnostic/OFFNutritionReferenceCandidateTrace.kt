package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic

data class OFFNutritionReferenceCandidateTrace(
    val sourceProductId: String,
    val productName: String,
    val normalizedProductIdentities: List<String>,

    val identityAccepted: Boolean,
    val identityRejectionReasons: List<String>,

    val nutritionAccepted: Boolean,
    val nutritionRejectionReasons: List<String>,

    val referenceEligible: Boolean,
    val referenceEligibilityRejectionReasons: List<String>,

    val candidateCreated: Boolean,
    val createdCandidateId: String?
) {

    init {
        require(sourceProductId.isNotBlank()) {
            "sourceProductId must not be blank."
        }

        require(sourceProductId == sourceProductId.trim()) {
            "sourceProductId must be trimmed."
        }

        require(productName.isNotBlank()) {
            "productName must not be blank."
        }

        require(productName == productName.trim()) {
            "productName must be trimmed."
        }

        requireDeterministicStrings(
            values =
                normalizedProductIdentities,
            fieldName =
                "normalizedProductIdentities"
        )

        requireDeterministicStrings(
            values =
                identityRejectionReasons,
            fieldName =
                "identityRejectionReasons"
        )

        requireDeterministicStrings(
            values =
                nutritionRejectionReasons,
            fieldName =
                "nutritionRejectionReasons"
        )

        requireDeterministicStrings(
            values =
                referenceEligibilityRejectionReasons,
            fieldName =
                "referenceEligibilityRejectionReasons"
        )

        require(
            !identityAccepted ||
                    normalizedProductIdentities.isNotEmpty()
        ) {
            "Accepted identity requires at least one normalized product identity."
        }

        requireAcceptanceReasonConsistency(
            stageName =
                "Identity",
            stageWasEvaluated =
                true,
            accepted =
                identityAccepted,
            rejectionReasons =
                identityRejectionReasons
        )

        requireAcceptanceReasonConsistency(
            stageName =
                "Nutrition",
            stageWasEvaluated =
                identityAccepted,
            accepted =
                nutritionAccepted,
            rejectionReasons =
                nutritionRejectionReasons
        )

        requireAcceptanceReasonConsistency(
            stageName =
                "Reference eligibility",
            stageWasEvaluated =
                nutritionAccepted,
            accepted =
                referenceEligible,
            rejectionReasons =
                referenceEligibilityRejectionReasons
        )

        require(
            identityAccepted ||
                    !nutritionAccepted
        ) {
            "Nutrition cannot be accepted when identity was rejected."
        }

        require(
            nutritionAccepted ||
                    !referenceEligible
        ) {
            "Reference eligibility cannot be accepted when nutrition was rejected."
        }

        require(
            referenceEligible ||
                    !candidateCreated
        ) {
            "A candidate cannot be created from an ineligible product."
        }

        require(
            candidateCreated ==
                    (createdCandidateId != null)
        ) {
            "createdCandidateId must exist exactly when candidateCreated is true."
        }

        require(
            createdCandidateId == null ||
                    createdCandidateId.isNotBlank()
        ) {
            "createdCandidateId must not be blank."
        }

        require(
            createdCandidateId == null ||
                    createdCandidateId == createdCandidateId.trim()
        ) {
            "createdCandidateId must be trimmed."
        }
    }

    private fun requireAcceptanceReasonConsistency(
        stageName: String,
        stageWasEvaluated: Boolean,
        accepted: Boolean,
        rejectionReasons: List<String>
    ) {

        if (!stageWasEvaluated) {
            require(!accepted) {
                "$stageName cannot be accepted when the preceding stage was rejected."
            }

            require(rejectionReasons.isEmpty()) {
                "$stageName must have no rejection reasons when the stage was not evaluated."
            }

            return
        }

        require(
            if (accepted) {
                rejectionReasons.isEmpty()
            } else {
                rejectionReasons.isNotEmpty()
            }
        ) {
            "$stageName must have no rejection reasons when accepted " +
                    "and at least one rejection reason when evaluated and rejected."
        }
    }

    private fun requireDeterministicStrings(
        values: List<String>,
        fieldName: String
    ) {

        require(
            values ==
                    values
                        .filter(String::isNotBlank)
                        .map(String::trim)
                        .distinct()
                        .sorted()
        ) {
            "$fieldName must be trimmed, sorted, distinct and non-blank."
        }
    }
}