package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic

data class MissingOFFNutritionReferenceCandidateFinding(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,
    val retrievalTerms: List<String>,

    val matchingTraceCount: Int,
    val identityAcceptedCount: Int,
    val nutritionAcceptedCount: Int,
    val referenceEligibleCount: Int,
    val candidateCreatedCount: Int,
    val candidatePersistedCount: Int,

    val firstMissingStage:
    MissingOFFNutritionReferenceCandidateStage,

    val countsByIdentityRejectionReason:
    Map<String, Int>,

    val countsByNutritionRejectionReason:
    Map<String, Int>,

    val countsByReferenceEligibilityRejectionReason:
    Map<String, Int>,

    val matchedProductNames:
    List<String>,

    val createdCandidateIds:
    List<String>,

    val persistedCandidateIds:
    List<String>,

    val reasons:
    List<String>
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

        require(matchingTraceCount >= 0) {
            "matchingTraceCount must not be negative."
        }

        require(
            identityAcceptedCount in 0..matchingTraceCount
        ) {
            "identityAcceptedCount must be between zero and matchingTraceCount."
        }

        require(
            nutritionAcceptedCount in 0..identityAcceptedCount
        ) {
            "nutritionAcceptedCount must not exceed identityAcceptedCount."
        }

        require(
            referenceEligibleCount in 0..nutritionAcceptedCount
        ) {
            "referenceEligibleCount must not exceed nutritionAcceptedCount."
        }

        require(
            candidateCreatedCount in 0..referenceEligibleCount
        ) {
            "candidateCreatedCount must not exceed referenceEligibleCount."
        }

        require(
            candidatePersistedCount in 0..candidateCreatedCount
        ) {
            "candidatePersistedCount must not exceed candidateCreatedCount."
        }

        requireSortedDistinct(
            values =
                retrievalTerms,
            fieldName =
                "retrievalTerms"
        )

        requireSortedDistinct(
            values =
                matchedProductNames,
            fieldName =
                "matchedProductNames"
        )

        requireSortedDistinct(
            values =
                createdCandidateIds,
            fieldName =
                "createdCandidateIds"
        )

        requireSortedDistinct(
            values =
                persistedCandidateIds,
            fieldName =
                "persistedCandidateIds"
        )

        requireSortedDistinct(
            values =
                reasons,
            fieldName =
                "reasons"
        )

        requireReasonCounts(
            values =
                countsByIdentityRejectionReason,
            fieldName =
                "countsByIdentityRejectionReason",
            maximumTotalCount =
                matchingTraceCount
        )

        requireReasonCounts(
            values =
                countsByNutritionRejectionReason,
            fieldName =
                "countsByNutritionRejectionReason",
            maximumTotalCount =
                matchingTraceCount
        )

        requireReasonCounts(
            values =
                countsByReferenceEligibilityRejectionReason,
            fieldName =
                "countsByReferenceEligibilityRejectionReason",
            maximumTotalCount =
                matchingTraceCount
        )

        require(
            createdCandidateIds.size <=
                    candidateCreatedCount
        ) {
            "createdCandidateIds must not contain more entries than candidateCreatedCount."
        }

        require(
            persistedCandidateIds.size <=
                    candidatePersistedCount
        ) {
            "persistedCandidateIds must not contain more entries than candidatePersistedCount."
        }

        require(
            persistedCandidateIds.all(
                createdCandidateIds::contains
            )
        ) {
            "persistedCandidateIds must be contained in createdCandidateIds."
        }
    }

    private fun requireSortedDistinct(
        values: List<String>,
        fieldName: String
    ) {

        require(
            values ==
                    values
                        .asSequence()
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
                        .toList()
        ) {
            "$fieldName must be trimmed, sorted, distinct and non-blank."
        }
    }

    private fun requireReasonCounts(
        values: Map<String, Int>,
        fieldName: String,
        maximumTotalCount: Int
    ) {

        require(
            values.keys.all { key ->
                key.isNotBlank() &&
                        key == key.trim()
            }
        ) {
            "$fieldName keys must be trimmed and non-blank."
        }

        require(
            values.values.all { count ->
                count > 0
            }
        ) {
            "$fieldName counts must be greater than zero."
        }

        require(
            values.values.sum() <=
                    maximumTotalCount
        ) {
            "$fieldName total must not exceed matchingTraceCount."
        }
    }
}