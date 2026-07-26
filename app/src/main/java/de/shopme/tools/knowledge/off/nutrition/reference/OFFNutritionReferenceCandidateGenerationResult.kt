package de.shopme.tools.knowledge.off.nutrition.reference

data class OFFNutritionReferenceCandidateGenerationResult(
    val inputCandidateCount: Int,
    val generatedCandidateCount: Int,
    val skippedWithoutNutritionCount: Int,
    val skippedInvalidIdentityCount: Int,
    val skippedInvalidNutritionPayloadCount: Int,
    val candidates: List<CanonicalOFFNutritionReferenceCandidate>
) {

    init {
        require(inputCandidateCount >= 0) {
            "inputCandidateCount must not be negative."
        }

        require(generatedCandidateCount >= 0) {
            "generatedCandidateCount must not be negative."
        }

        require(skippedWithoutNutritionCount >= 0) {
            "skippedWithoutNutritionCount must not be negative."
        }

        require(skippedInvalidIdentityCount >= 0) {
            "skippedInvalidIdentityCount must not be negative."
        }

        require(skippedInvalidNutritionPayloadCount >= 0) {
            "skippedInvalidNutritionPayloadCount must not be negative."
        }

        require(generatedCandidateCount == candidates.size) {
            "generatedCandidateCount must equal candidates.size."
        }

        require(
            inputCandidateCount ==
                    generatedCandidateCount +
                    skippedWithoutNutritionCount +
                    skippedInvalidIdentityCount +
                    skippedInvalidNutritionPayloadCount
        ) {
            "Generation counts do not cover all input candidates."
        }
    }
}