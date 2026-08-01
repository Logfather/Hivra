package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage

import java.io.File

data class ResultingNutritionCoverageAnalysis(
    val aggregateFile: File,
    val aggregateFileSizeBytes: Long,
    val runtimeFile: File,
    val runtimeFileSizeBytes: Long,
    val aggregateEntryCount: Long,
    val runtimeEntryCount: Long,
    val coveredAggregateEntryCount: Long,
    val missingRuntimeEntryCount: Long,
    val additionalRuntimeEntryCount: Long,
    val aggregateCoverageRate: Double,
    val missingRuntimeCanonicalIds: List<String>,
    val additionalRuntimeCanonicalIds: List<String>,
    val omittedMissingRuntimeCanonicalIdCount: Long,
    val omittedAdditionalRuntimeCanonicalIdCount: Long,
    val durationMillis: Long
) {

    val isComplete: Boolean
        get() =
            missingRuntimeEntryCount == 0L

    init {
        require(aggregateFileSizeBytes >= 0L)
        require(runtimeFileSizeBytes >= 0L)
        require(aggregateEntryCount >= 0L)
        require(runtimeEntryCount >= 0L)
        require(coveredAggregateEntryCount >= 0L)
        require(missingRuntimeEntryCount >= 0L)
        require(additionalRuntimeEntryCount >= 0L)
        require(aggregateCoverageRate in 0.0..1.0)
        require(omittedMissingRuntimeCanonicalIdCount >= 0L)
        require(omittedAdditionalRuntimeCanonicalIdCount >= 0L)
        require(durationMillis >= 0L)

        require(
            coveredAggregateEntryCount +
                    missingRuntimeEntryCount ==
                    aggregateEntryCount
        ) {
            "Covered and missing aggregate entries must equal " +
                    "aggregateEntryCount."
        }

        require(
            coveredAggregateEntryCount +
                    additionalRuntimeEntryCount ==
                    runtimeEntryCount
        ) {
            "Covered and additional runtime entries must equal " +
                    "runtimeEntryCount."
        }

        require(
            missingRuntimeCanonicalIds ==
                    missingRuntimeCanonicalIds.distinct().sorted()
        ) {
            "Reported missing runtime canonical IDs must be unique " +
                    "and sorted."
        }

        require(
            additionalRuntimeCanonicalIds ==
                    additionalRuntimeCanonicalIds.distinct().sorted()
        ) {
            "Reported additional runtime canonical IDs must be unique " +
                    "and sorted."
        }

        require(
            missingRuntimeCanonicalIds.size.toLong() +
                    omittedMissingRuntimeCanonicalIdCount ==
                    missingRuntimeEntryCount
        ) {
            "Reported and omitted missing runtime IDs must equal " +
                    "missingRuntimeEntryCount."
        }

        require(
            additionalRuntimeCanonicalIds.size.toLong() +
                    omittedAdditionalRuntimeCanonicalIdCount ==
                    additionalRuntimeEntryCount
        ) {
            "Reported and omitted additional runtime IDs must equal " +
                    "additionalRuntimeEntryCount."
        }
    }
}