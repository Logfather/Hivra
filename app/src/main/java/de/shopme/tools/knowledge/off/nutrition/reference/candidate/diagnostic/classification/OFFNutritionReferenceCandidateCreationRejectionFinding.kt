package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.classification

data class OFFNutritionReferenceCandidateCreationRejectionFinding(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,

    val rawOFFProductMatchCount: Int,
    val rawOFFProductWithUsableNutritionCount: Int,

    val matchedRawProductIds: List<String>,
    val matchedRawProductWithUsableNutritionIds: List<String>,

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

        require(
            rawOFFProductWithUsableNutritionCount <=
                    rawOFFProductMatchCount
        ) {
            "Usable raw OFF product count must not exceed " +
                    "raw OFF product match count."
        }

        requireSortedDistinct(
            values =
                matchedRawProductIds,
            fieldName =
                "matchedRawProductIds"
        )

        requireSortedDistinct(
            values =
                matchedRawProductWithUsableNutritionIds,
            fieldName =
                "matchedRawProductWithUsableNutritionIds"
        )

        require(
            matchedRawProductWithUsableNutritionIds.all(
                matchedRawProductIds::contains
            )
        ) {
            "matchedRawProductWithUsableNutritionIds must be " +
                    "contained in matchedRawProductIds."
        }

        require(
            matchedRawProductIds.size <=
                    rawOFFProductMatchCount
        ) {
            "Persisted raw OFF product IDs must not exceed " +
                    "raw OFF product match count."
        }

        require(
            matchedRawProductWithUsableNutritionIds.size <=
                    rawOFFProductWithUsableNutritionCount
        ) {
            "Persisted usable raw OFF product IDs must not exceed " +
                    "usable raw OFF product count."
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
            countsByRejectionStage.keys.none { stage ->
                stage ==
                        OFFNutritionReferenceCandidateCreationRejectionStage
                            .TRACE_NOT_FOUND
            }
        ) {
            "TRACE_NOT_FOUND must not appear in countsByRejectionStage."
        }

        require(
            countsByRejectionStage.values.all { count ->
                count > 0
            }
        ) {
            "Rejection-stage counts must be positive."
        }

        require(
            countsByRejectionReason.keys.all { reason ->
                reason.isNotBlank()
            }
        ) {
            "Rejection reasons must not be blank."
        }

        require(
            countsByRejectionReason.values.all { count ->
                count > 0
            }
        ) {
            "Rejection-reason counts must be positive."
        }

        requireSortedDistinct(
            values =
                matchedSourceProductIds,
            fieldName =
                "matchedSourceProductIds"
        )

        requireSortedDistinct(
            values =
                matchedProductNames,
            fieldName =
                "matchedProductNames"
        )

        require(
            matchedSourceProductIds.size <=
                    matchedTraceCount
        ) {
            "Matched source product IDs must not exceed matched trace count."
        }

        if (
            firstRejectionStage ==
            OFFNutritionReferenceCandidateCreationRejectionStage
                .TRACE_NOT_FOUND
        ) {
            require(matchedTraceCount == 0) {
                "TRACE_NOT_FOUND requires matchedTraceCount == 0."
            }

            require(matchedSourceProductIds.isEmpty()) {
                "TRACE_NOT_FOUND requires empty matchedSourceProductIds."
            }

            require(matchedProductNames.isEmpty()) {
                "TRACE_NOT_FOUND requires empty matchedProductNames."
            }
        } else {
            require(matchedTraceCount > 0) {
                "A classified rejection stage requires matched traces."
            }
        }
    }

    private fun requireSortedDistinct(
        values: List<String>,
        fieldName: String
    ) {
        require(
            values ==
                    values
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        ) {
            "$fieldName must contain non-blank values and be " +
                    "deterministically sorted and distinct."
        }
    }
}