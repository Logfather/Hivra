package de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilterResult
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityRejection
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityRejectionReason
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityThresholds

class OFFNutritionReferenceQualityDistributionAnalyzer(
    private val maximumExamplesPerReason: Int =
        DEFAULT_MAXIMUM_EXAMPLES_PER_REASON
) {

    private var analyzedCandidateCount =
        0L

    private var acceptedCandidateCount =
        0L

    private var rejectedCandidateCount =
        0L

    private var multipleReasonRejectionCount =
        0L

    private val countsByReason =
        mutableMapOf<String, Long>()

    private val countsByReasonCombination =
        mutableMapOf<String, Long>()

    private val countsByReasonCount =
        mutableMapOf<String, Long>()

    private val negativeValueCountsByNutritionKey =
        mutableMapOf<String, Long>()

    private val aboveMaximumCountsByNutritionKey =
        mutableMapOf<String, Long>()

    private val aboveMaximumExcessBucketsByNutritionKey =
        mutableMapOf<String, MutableMap<String, Long>>()

    private val macronutrientSumBuckets =
        mutableMapOf<String, Long>()

    private val sugarExcessBuckets =
        mutableMapOf<String, Long>()

    private val saturatedFatExcessBuckets =
        mutableMapOf<String, Long>()

    private val zeroOnlyCountsByPresentKeyCount =
        mutableMapOf<String, Long>()

    private val valueRangeAccumulators =
        mutableMapOf<String, MutableValueRange>()

    private val examplesByReason =
        mutableMapOf<
                String,
                MutableList<OFFNutritionReferenceQualityDistributionExample>
                >()

    init {
        require(maximumExamplesPerReason >= 0)
    }

    fun observe(
        candidates: List<CanonicalOFFNutritionReferenceCandidate>,
        qualityResult: OFFNutritionReferenceQualityFilterResult
    ) {
        require(
            candidates.size ==
                    qualityResult.inputCandidateCount
        ) {
            "Analyzer candidates must match quality filter input."
        }

        analyzedCandidateCount +=
            candidates.size.toLong()

        acceptedCandidateCount +=
            qualityResult.acceptedCandidateCount.toLong()

        rejectedCandidateCount +=
            qualityResult.rejectedCandidateCount.toLong()

        candidates.forEach { candidate ->
            observeRanges(candidate)
        }

        val candidatesByIdentity =
            candidates.associateBy { candidate ->
                CandidateIdentity(
                    sourceId =
                        candidate.sourceId,
                    canonicalId =
                        candidate.canonicalId
                )
            }

        qualityResult.rejections.forEach { rejection ->
            val identity =
                CandidateIdentity(
                    sourceId =
                        rejection.sourceId,
                    canonicalId =
                        rejection.canonicalId
                )

            val candidate =
                requireNotNull(
                    candidatesByIdentity[identity]
                ) {
                    "Rejected candidate not found in analyzed batch: " +
                            "${rejection.sourceId}/${rejection.canonicalId}"
                }

            observeRejection(
                candidate =
                    candidate,
                rejection =
                    rejection
            )
        }
    }

    fun createReport():
            OFFNutritionReferenceQualityDistributionReport {

        val allReasons =
            OFFNutritionReferenceQualityRejectionReason
                .entries
                .map { reason ->
                    reason.name
                }
                .sorted()

        val normalizedCountsByReason =
            allReasons.associateWith { reason ->
                countsByReason[reason] ?: 0L
            }

        return OFFNutritionReferenceQualityDistributionReport(
            version =
                OFFNutritionReferenceQualityDistributionReport
                    .CURRENT_VERSION,
            analyzedCandidateCount =
                analyzedCandidateCount,
            acceptedCandidateCount =
                acceptedCandidateCount,
            rejectedCandidateCount =
                rejectedCandidateCount,
            rejectionReasonOccurrenceCount =
                normalizedCountsByReason.values.sum(),
            multipleReasonRejectionCount =
                multipleReasonRejectionCount,
            countsByReason =
                normalizedCountsByReason.toSortedMap(),
            countsByReasonCombination =
                countsByReasonCombination.toSortedMap(),
            countsByReasonCount =
                countsByReasonCount.toSortedMap(),
            negativeValueCountsByNutritionKey =
                negativeValueCountsByNutritionKey.toSortedMap(),
            aboveMaximumCountsByNutritionKey =
                aboveMaximumCountsByNutritionKey.toSortedMap(),
            aboveMaximumExcessBucketsByNutritionKey =
                aboveMaximumExcessBucketsByNutritionKey
                    .toSortedMap()
                    .mapValues { (_, buckets) ->
                        buckets.toSortedMap()
                    },
            macronutrientSumBuckets =
                macronutrientSumBuckets.toSortedMap(),
            sugarExcessBuckets =
                sugarExcessBuckets.toSortedMap(),
            saturatedFatExcessBuckets =
                saturatedFatExcessBuckets.toSortedMap(),
            zeroOnlyCountsByPresentKeyCount =
                zeroOnlyCountsByPresentKeyCount.toSortedMap(),
            valueRangesByNutritionKey =
                valueRangeAccumulators
                    .toSortedMap()
                    .mapValues { (_, accumulator) ->
                        accumulator.snapshot()
                    },
            examplesByReason =
                examplesByReason
                    .toSortedMap()
                    .mapValues { (_, examples) ->
                        examples.toList()
                    }
        )
    }

    private fun observeRanges(
        candidate: CanonicalOFFNutritionReferenceCandidate
    ) {
        candidate.nutrition.forEach { (key, value) ->
            if (!value.isFinite()) {
                return@forEach
            }

            valueRangeAccumulators
                .getOrPut(key) {
                    MutableValueRange()
                }
                .observe(value)
        }
    }

    private fun observeRejection(
        candidate: CanonicalOFFNutritionReferenceCandidate,
        rejection: OFFNutritionReferenceQualityRejection
    ) {
        val reasonNames =
            rejection.reasons
                .map { reason ->
                    reason.name
                }
                .sorted()

        reasonNames.forEach { reason ->
            countsByReason.increment(reason)

            addExample(
                reason =
                    reason,
                candidate =
                    candidate,
                reasonNames =
                    reasonNames
            )
        }

        val reasonCombination =
            reasonNames.joinToString("+")

        countsByReasonCombination.increment(
            reasonCombination
        )

        countsByReasonCount.increment(
            reasonNames.size.toString()
        )

        if (reasonNames.size > 1) {
            multipleReasonRejectionCount++
        }

        if (
            OFFNutritionReferenceQualityRejectionReason
                .NEGATIVE_NUTRITION_VALUE in rejection.reasons
        ) {
            observeNegativeValues(candidate)
        }

        if (
            OFFNutritionReferenceQualityRejectionReason
                .NUTRITION_VALUE_ABOVE_MAXIMUM in rejection.reasons
        ) {
            observeValuesAboveMaximum(candidate)
        }

        if (
            OFFNutritionReferenceQualityRejectionReason
                .MACRONUTRIENT_SUM_EXCEEDS_100_GRAMS in
            rejection.reasons
        ) {
            observeMacronutrientSum(candidate)
        }

        if (
            OFFNutritionReferenceQualityRejectionReason
                .SUGARS_EXCEED_CARBOHYDRATES in rejection.reasons
        ) {
            observeSugarExcess(candidate)
        }

        if (
            OFFNutritionReferenceQualityRejectionReason
                .SATURATED_FAT_EXCEEDS_TOTAL_FAT in rejection.reasons
        ) {
            observeSaturatedFatExcess(candidate)
        }

        if (
            OFFNutritionReferenceQualityRejectionReason
                .ZERO_ONLY_NUTRITION_PAYLOAD in rejection.reasons
        ) {
            val bucket =
                OFFNutritionReferenceQualityDistributionBuckets
                    .keyCountBucket(
                        candidate.nutrition.size
                    )

            zeroOnlyCountsByPresentKeyCount.increment(
                bucket
            )
        }
    }

    private fun observeNegativeValues(
        candidate: CanonicalOFFNutritionReferenceCandidate
    ) {
        candidate.nutrition
            .filterValues { value ->
                value < 0.0
            }
            .keys
            .forEach { key ->
                negativeValueCountsByNutritionKey.increment(
                    key
                )
            }
    }

    private fun observeValuesAboveMaximum(
        candidate: CanonicalOFFNutritionReferenceCandidate
    ) {
        candidate.nutrition.forEach { (key, value) ->
            val maximum =
                maximumForKey(key)
                    ?: return@forEach

            if (value <= maximum) {
                return@forEach
            }

            aboveMaximumCountsByNutritionKey.increment(
                key
            )

            val bucket =
                OFFNutritionReferenceQualityDistributionBuckets
                    .maximumExcessBucket(
                        value - maximum
                    )

            aboveMaximumExcessBucketsByNutritionKey
                .getOrPut(key) {
                    mutableMapOf()
                }
                .increment(bucket)
        }
    }

    private fun observeMacronutrientSum(
        candidate: CanonicalOFFNutritionReferenceCandidate
    ) {
        val sum =
            MACRONUTRIENT_SUM_KEYS
                .mapNotNull { key ->
                    candidate.nutrition[key]
                }
                .sum()

        val bucket =
            OFFNutritionReferenceQualityDistributionBuckets
                .macronutrientSumBucket(sum)

        macronutrientSumBuckets.increment(bucket)
    }

    private fun observeSugarExcess(
        candidate: CanonicalOFFNutritionReferenceCandidate
    ) {
        val sugars =
            candidate.nutrition[SUGARS_KEY]
                ?: return

        val carbohydrates =
            candidate.nutrition[CARBOHYDRATES_KEY]
                ?: return

        val bucket =
            OFFNutritionReferenceQualityDistributionBuckets
                .relationshipExcessBucket(
                    sugars - carbohydrates
                )

        sugarExcessBuckets.increment(bucket)
    }

    private fun observeSaturatedFatExcess(
        candidate: CanonicalOFFNutritionReferenceCandidate
    ) {
        val saturatedFat =
            candidate.nutrition[SATURATED_FAT_KEY]
                ?: return

        val totalFat =
            candidate.nutrition[FAT_KEY]
                ?: return

        val bucket =
            OFFNutritionReferenceQualityDistributionBuckets
                .relationshipExcessBucket(
                    saturatedFat - totalFat
                )

        saturatedFatExcessBuckets.increment(bucket)
    }

    private fun addExample(
        reason: String,
        candidate: CanonicalOFFNutritionReferenceCandidate,
        reasonNames: List<String>
    ) {
        if (maximumExamplesPerReason == 0) {
            return
        }

        val examples =
            examplesByReason.getOrPut(reason) {
                mutableListOf()
            }

        if (examples.size >= maximumExamplesPerReason) {
            return
        }

        examples +=
            OFFNutritionReferenceQualityDistributionExample(
                sourceId =
                    candidate.sourceId,
                canonicalId =
                    candidate.canonicalId,
                productName =
                    candidate.productName,
                nutrition =
                    candidate.nutrition.toSortedMap(),
                reasons =
                    reasonNames
            )
    }

    private fun maximumForKey(
        key: String
    ): Double? =
        when (key) {
            ENERGY_KEY ->
                OFFNutritionReferenceQualityThresholds
                    .MAXIMUM_ENERGY_KCAL_PER_100G

            in COMPONENT_KEYS ->
                OFFNutritionReferenceQualityThresholds
                    .MAXIMUM_COMPONENT_GRAMS_PER_100G

            else ->
                null
        }

    private fun MutableMap<String, Long>.increment(
        key: String
    ) {
        this[key] =
            getOrDefault(key, 0L) + 1L
    }

    private data class CandidateIdentity(
        val sourceId: String,
        val canonicalId: String
    )

    private class MutableValueRange {

        private var count =
            0L

        private var minimum =
            Double.POSITIVE_INFINITY

        private var maximum =
            Double.NEGATIVE_INFINITY

        fun observe(
            value: Double
        ) {
            count++

            if (value < minimum) {
                minimum = value
            }

            if (value > maximum) {
                maximum = value
            }
        }

        fun snapshot():
                OFFNutritionValueRange =
            if (count == 0L) {
                OFFNutritionValueRange(
                    observationCount = 0L,
                    minimum = null,
                    maximum = null
                )
            } else {
                OFFNutritionValueRange(
                    observationCount = count,
                    minimum = minimum,
                    maximum = maximum
                )
            }
    }

    private companion object {

        const val DEFAULT_MAXIMUM_EXAMPLES_PER_REASON =
            10

        const val ENERGY_KEY =
            "energyKcalPer100g"

        const val FAT_KEY =
            "fatPer100g"

        const val SATURATED_FAT_KEY =
            "saturatedFatPer100g"

        const val CARBOHYDRATES_KEY =
            "carbohydratesPer100g"

        const val SUGARS_KEY =
            "sugarsPer100g"

        val COMPONENT_KEYS =
            setOf(
                FAT_KEY,
                SATURATED_FAT_KEY,
                CARBOHYDRATES_KEY,
                SUGARS_KEY,
                "fiberPer100g",
                "proteinsPer100g",
                "saltPer100g"
            )

        val MACRONUTRIENT_SUM_KEYS =
            listOf(
                FAT_KEY,
                CARBOHYDRATES_KEY,
                "fiberPer100g",
                "proteinsPer100g"
            )
    }
}