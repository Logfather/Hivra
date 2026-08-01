package de.shopme.tools.knowledge.off.nutrition.reference.quality

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate

class OFFNutritionReferenceQualityFilter {

    fun filter(
        candidates: List<CanonicalOFFNutritionReferenceCandidate>
    ): OFFNutritionReferenceQualityFilterResult {

        val acceptedCandidates =
            mutableListOf<CanonicalOFFNutritionReferenceCandidate>()

        val rejections =
            mutableListOf<OFFNutritionReferenceQualityRejection>()

        candidates.forEach { candidate ->

            val rejectionReasons =
                determineRejectionReasons(
                    candidate =
                        candidate
                )

            if (rejectionReasons.isEmpty()) {
                acceptedCandidates +=
                    candidate
            } else {
                rejections +=
                    OFFNutritionReferenceQualityRejection(
                        sourceId =
                            candidate.sourceId,
                        canonicalId =
                            candidate.canonicalId,
                        reasons =
                            rejectionReasons
                    )
            }
        }

        val sortedAcceptedCandidates =
            acceptedCandidates.sortedWith(
                CANDIDATE_COMPARATOR
            )

        val sortedRejections =
            rejections.sortedWith(
                REJECTION_COMPARATOR
            )

        val countsByReason =
            sortedRejections
                .flatMap { rejection ->
                    rejection.reasons
                }
                .groupingBy { reason ->
                    reason
                }
                .eachCount()
                .toSortedMap(
                    compareBy { reason ->
                        reason.name
                    }
                )

        return OFFNutritionReferenceQualityFilterResult(
            inputCandidateCount =
                candidates.size,
            acceptedCandidateCount =
                sortedAcceptedCandidates.size,
            rejectedCandidateCount =
                sortedRejections.size,
            countsByReason =
                countsByReason,
            acceptedCandidates =
                sortedAcceptedCandidates,
            rejections =
                sortedRejections
        )
    }

    private fun determineRejectionReasons(
        candidate: CanonicalOFFNutritionReferenceCandidate
    ): List<OFFNutritionReferenceQualityRejectionReason> {

        val reasons =
            mutableSetOf<OFFNutritionReferenceQualityRejectionReason>()

        val nutrition =
            candidate.nutrition

        if (nutrition.isEmpty()) {
            reasons +=
                OFFNutritionReferenceQualityRejectionReason
                    .EMPTY_NUTRITION_PAYLOAD

            return reasons
                .sortedBy { reason ->
                    reason.name
                }
        }

        if (
            nutrition.keys.any { key ->
                key !in MAXIMUM_VALUE_BY_NUTRITION_KEY
            }
        ) {
            reasons +=
                OFFNutritionReferenceQualityRejectionReason
                    .UNSUPPORTED_NUTRITION_KEY
        }

        if (
            nutrition.values.any { value ->
                !value.isFinite()
            }
        ) {
            reasons +=
                OFFNutritionReferenceQualityRejectionReason
                    .NON_FINITE_NUTRITION_VALUE
        }

        if (
            nutrition.values.any { value ->
                value < 0.0
            }
        ) {
            reasons +=
                OFFNutritionReferenceQualityRejectionReason
                    .NEGATIVE_NUTRITION_VALUE
        }

        if (
            nutrition.any { (key, value) ->
                exceedsMaximum(
                    key =
                        key,
                    value =
                        value
                )
            }
        ) {
            reasons +=
                OFFNutritionReferenceQualityRejectionReason
                    .NUTRITION_VALUE_ABOVE_MAXIMUM
        }

        if (
            nutrition.values.all { value ->
                value == 0.0
            }
        ) {
            reasons +=
                OFFNutritionReferenceQualityRejectionReason
                    .ZERO_ONLY_NUTRITION_PAYLOAD
        }

        addRelationshipRejectionReasons(
            nutrition =
                nutrition,
            reasons =
                reasons
        )

        addMacronutrientSumRejectionReason(
            nutrition =
                nutrition,
            reasons =
                reasons
        )

        return reasons
            .sortedBy { reason ->
                reason.name
            }
    }

    private fun exceedsMaximum(
        key: String,
        value: Double
    ): Boolean {

        val maximum =
            MAXIMUM_VALUE_BY_NUTRITION_KEY[key]
                ?: return false

        return value > maximum
    }

    private fun addRelationshipRejectionReasons(
        nutrition: Map<String, Double>,
        reasons:
        MutableSet<OFFNutritionReferenceQualityRejectionReason>
    ) {

        val totalFat =
            nutrition[FAT_PER_100G]

        val saturatedFat =
            nutrition[SATURATED_FAT_PER_100G]

        if (
            totalFat != null &&
            saturatedFat != null &&
            saturatedFat >
            totalFat +
            OFFNutritionReferenceQualityThresholds
                .RELATIONSHIP_TOLERANCE_GRAMS
        ) {
            reasons +=
                OFFNutritionReferenceQualityRejectionReason
                    .SATURATED_FAT_EXCEEDS_TOTAL_FAT
        }

        val carbohydrates =
            nutrition[CARBOHYDRATES_PER_100G]

        val sugars =
            nutrition[SUGARS_PER_100G]

        if (
            carbohydrates != null &&
            sugars != null &&
            sugars >
            carbohydrates +
            OFFNutritionReferenceQualityThresholds
                .RELATIONSHIP_TOLERANCE_GRAMS
        ) {
            reasons +=
                OFFNutritionReferenceQualityRejectionReason
                    .SUGARS_EXCEED_CARBOHYDRATES
        }
    }

    private fun addMacronutrientSumRejectionReason(
        nutrition: Map<String, Double>,
        reasons:
        MutableSet<OFFNutritionReferenceQualityRejectionReason>
    ) {

        val availableMacronutrients =
            MACRONUTRIENT_KEYS
                .mapNotNull { key ->
                    nutrition[key]
                }

        if (
            availableMacronutrients.size <
            OFFNutritionReferenceQualityThresholds
                .MINIMUM_MACRONUTRIENTS_FOR_SUM_CHECK
        ) {
            return
        }

        val macronutrientSum =
            availableMacronutrients.sum()

        if (
            macronutrientSum >
            OFFNutritionReferenceQualityThresholds
                .MAXIMUM_MACRONUTRIENT_SUM_GRAMS
        ) {
            reasons +=
                OFFNutritionReferenceQualityRejectionReason
                    .MACRONUTRIENT_SUM_EXCEEDS_100_GRAMS
        }
    }

    companion object {

        val CANDIDATE_COMPARATOR:
                Comparator<CanonicalOFFNutritionReferenceCandidate> =
            compareBy<CanonicalOFFNutritionReferenceCandidate>(
                {
                    it.sourceId
                },
                {
                    it.canonicalId
                }
            )

        private val REJECTION_COMPARATOR:
                Comparator<OFFNutritionReferenceQualityRejection> =
            compareBy<OFFNutritionReferenceQualityRejection>(
                {
                    it.sourceId
                },
                {
                    it.canonicalId
                }
            )

        private const val ENERGY_KCAL_PER_100G =
            "energyKcalPer100g"

        private const val FAT_PER_100G =
            "fatPer100g"

        private const val SATURATED_FAT_PER_100G =
            "saturatedFatPer100g"

        private const val CARBOHYDRATES_PER_100G =
            "carbohydratesPer100g"

        private const val SUGARS_PER_100G =
            "sugarsPer100g"

        private const val FIBER_PER_100G =
            "fiberPer100g"

        private const val PROTEINS_PER_100G =
            "proteinsPer100g"

        private const val SALT_PER_100G =
            "saltPer100g"

        private val MACRONUTRIENT_KEYS =
            listOf(
                FAT_PER_100G,
                CARBOHYDRATES_PER_100G,
                PROTEINS_PER_100G
            )

        private val MAXIMUM_VALUE_BY_NUTRITION_KEY =
            mapOf(
                ENERGY_KCAL_PER_100G to
                        OFFNutritionReferenceQualityThresholds
                            .MAXIMUM_ENERGY_KCAL_PER_100G,
                FAT_PER_100G to
                        OFFNutritionReferenceQualityThresholds
                            .MAXIMUM_COMPONENT_GRAMS_PER_100G,
                SATURATED_FAT_PER_100G to
                        OFFNutritionReferenceQualityThresholds
                            .MAXIMUM_COMPONENT_GRAMS_PER_100G,
                CARBOHYDRATES_PER_100G to
                        OFFNutritionReferenceQualityThresholds
                            .MAXIMUM_COMPONENT_GRAMS_PER_100G,
                SUGARS_PER_100G to
                        OFFNutritionReferenceQualityThresholds
                            .MAXIMUM_COMPONENT_GRAMS_PER_100G,
                FIBER_PER_100G to
                        OFFNutritionReferenceQualityThresholds
                            .MAXIMUM_COMPONENT_GRAMS_PER_100G,
                PROTEINS_PER_100G to
                        OFFNutritionReferenceQualityThresholds
                            .MAXIMUM_COMPONENT_GRAMS_PER_100G,
                SALT_PER_100G to
                        OFFNutritionReferenceQualityThresholds
                            .MAXIMUM_COMPONENT_GRAMS_PER_100G
            )
    }
}