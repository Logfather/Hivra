package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerationResult
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilterResult
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityRejectionReason

class OFFNutritionReferenceQualityStatisticsAccumulator {

    private var extractedCandidateCount =
        0L

    private var generatedCandidateCount =
        0L

    private var skippedWithoutNutritionCount =
        0L

    private var skippedInvalidIdentityCount =
        0L

    private var skippedInvalidNutritionPayloadCount =
        0L

    private var acceptedCandidateCount =
        0L

    private var rejectedCandidateCount =
        0L

    private val mutableCountsByReason =
        OFFNutritionReferenceQualityRejectionReason
            .entries
            .associateWith { 0L }
            .toMutableMap()

    fun recordExtractedCandidate() {
        extractedCandidateCount++
    }

    fun addGenerationResult(
        result: OFFNutritionReferenceCandidateGenerationResult
    ) {
        generatedCandidateCount +=
            result.generatedCandidateCount.toLong()

        skippedWithoutNutritionCount +=
            result.skippedWithoutNutritionCount.toLong()

        skippedInvalidIdentityCount +=
            result.skippedInvalidIdentityCount.toLong()

        skippedInvalidNutritionPayloadCount +=
            result.skippedInvalidNutritionPayloadCount.toLong()
    }

    fun addQualityResult(
        result: OFFNutritionReferenceQualityFilterResult
    ) {
        acceptedCandidateCount +=
            result.acceptedCandidateCount.toLong()

        rejectedCandidateCount +=
            result.rejectedCandidateCount.toLong()

        result.countsByReason.forEach { (reason, count) ->
            mutableCountsByReason[reason] =
                mutableCountsByReason.getValue(reason) +
                        count.toLong()
        }
    }

    fun snapshot():
            StreamingOFFNutritionReferenceQualityStatistics {

        val countsByReason =
            OFFNutritionReferenceQualityRejectionReason
                .entries
                .associateWith { reason ->
                    mutableCountsByReason.getValue(reason)
                }
                .toSortedMap(
                    compareBy(Enum<*>::name)
                )

        return StreamingOFFNutritionReferenceQualityStatistics(
            extractedCandidateCount =
                extractedCandidateCount,
            generatedCandidateCount =
                generatedCandidateCount,
            skippedWithoutNutritionCount =
                skippedWithoutNutritionCount,
            skippedInvalidIdentityCount =
                skippedInvalidIdentityCount,
            skippedInvalidNutritionPayloadCount =
                skippedInvalidNutritionPayloadCount,
            acceptedCandidateCount =
                acceptedCandidateCount,
            rejectedCandidateCount =
                rejectedCandidateCount,
            countsByReason =
                countsByReason
        )
    }
}