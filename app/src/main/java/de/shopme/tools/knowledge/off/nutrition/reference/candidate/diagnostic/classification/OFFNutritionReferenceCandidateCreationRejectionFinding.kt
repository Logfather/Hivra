package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.classification

data class OFFNutritionReferenceCandidateCreationRejectionFinding(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,
    val rawOFFProductMatchCount: Int,
    val rawOFFProductWithUsableNutritionCount: Int,
    val matchedTraceCount: Int,
    val rejectedTraceCount: Int,
    val createdTraceCount: Int,
    val firstRejectionStage:
    OFFNutritionReferenceCandidateCreationRejectionStage,
    val countsByRejectionStage:
    Map<OFFNutritionReferenceCandidateCreationRejectionStage, Int>,
    val countsByRejectionReason: Map<String, Int>,
    val matchedSourceProductIds: List<String>,
    val matchedProductNames: List<String>
) {

    init {
        require(catalogIndex >= 0) {
            "catalogIndex must not be negative."
        }

        require(catalogKey.isNotBlank()) {
            "catalogKey must not be blank."
        }

        require(normalizedEnglish.isNotBlank()) {
            "normalizedEnglish must not be blank."
        }

        require(rawOFFProductMatchCount >= 0) {
            "rawOFFProductMatchCount must not be negative."
        }

        require(rawOFFProductWithUsableNutritionCount >= 0) {
            "rawOFFProductWithUsableNutritionCount must not be negative."
        }

        require(matchedTraceCount >= 0) {
            "matchedTraceCount must not be negative."
        }

        require(rejectedTraceCount >= 0) {
            "rejectedTraceCount must not be negative."
        }

        require(createdTraceCount >= 0) {
            "createdTraceCount must not be negative."
        }

        require(
            rejectedTraceCount + createdTraceCount ==
                    matchedTraceCount
        ) {
            "Rejected and created traces must cover all matched traces."
        }

        require(
            countsByRejectionStage.values.sum() ==
                    rejectedTraceCount
        ) {
            "Rejection-stage counts must cover all rejected traces."
        }

        require(
            matchedSourceProductIds ==
                    matchedSourceProductIds.distinct().sorted()
        ) {
            "matchedSourceProductIds must be distinct and sorted."
        }

        require(
            matchedProductNames ==
                    matchedProductNames.distinct().sorted()
        ) {
            "matchedProductNames must be distinct and sorted."
        }
    }
}