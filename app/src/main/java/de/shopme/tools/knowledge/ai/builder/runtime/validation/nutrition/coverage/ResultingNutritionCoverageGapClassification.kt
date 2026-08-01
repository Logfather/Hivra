package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage

import java.io.File

data class ResultingNutritionCoverageGapClassification(
    val aggregateFile: File,
    val runtimeFile: File,
    val aggregateEntryCount: Long,
    val runtimeEntryCount: Long,
    val exactMatchCount: Long,
    val normalizationEquivalentMatchCount: Long,
    val trueMissingRuntimeEntryCount: Long,
    val trueAdditionalRuntimeEntryCount: Long,
    val exactCoverageRate: Double,
    val effectiveCoverageRate: Double,
    val normalizationCollisionGroupCount: Long,
    val normalizationEquivalentExamples:
    List<ResultingNutritionCoverageNormalizationEquivalentPair>,
    val trueMissingRuntimeCanonicalIds: List<String>,
    val trueAdditionalRuntimeCanonicalIds: List<String>,
    val omittedNormalizationEquivalentExampleCount: Long,
    val omittedTrueMissingRuntimeCanonicalIdCount: Long,
    val omittedTrueAdditionalRuntimeCanonicalIdCount: Long,
    val durationMillis: Long
) {

    val effectiveCoveredAggregateEntryCount: Long
        get() =
            exactMatchCount +
                    normalizationEquivalentMatchCount

    val complete: Boolean
        get() =
            trueMissingRuntimeEntryCount == 0L

    init {
        require(aggregateEntryCount >= 0L)
        require(runtimeEntryCount >= 0L)
        require(exactMatchCount >= 0L)
        require(normalizationEquivalentMatchCount >= 0L)
        require(trueMissingRuntimeEntryCount >= 0L)
        require(trueAdditionalRuntimeEntryCount >= 0L)
        require(exactCoverageRate in 0.0..1.0)
        require(effectiveCoverageRate in 0.0..1.0)
        require(normalizationCollisionGroupCount >= 0L)
        require(omittedNormalizationEquivalentExampleCount >= 0L)
        require(omittedTrueMissingRuntimeCanonicalIdCount >= 0L)
        require(omittedTrueAdditionalRuntimeCanonicalIdCount >= 0L)
        require(durationMillis >= 0L)

        require(
            exactMatchCount +
                    normalizationEquivalentMatchCount +
                    trueMissingRuntimeEntryCount ==
                    aggregateEntryCount
        ) {
            "Exact, normalization-equivalent and truly missing " +
                    "aggregate entries must equal aggregateEntryCount."
        }

        require(
            exactMatchCount +
                    normalizationEquivalentMatchCount +
                    trueAdditionalRuntimeEntryCount ==
                    runtimeEntryCount
        ) {
            "Exact, normalization-equivalent and truly additional " +
                    "runtime entries must equal runtimeEntryCount."
        }

        require(
            effectiveCoveredAggregateEntryCount <=
                    aggregateEntryCount
        )

        require(
            normalizationEquivalentExamples.size.toLong() +
                    omittedNormalizationEquivalentExampleCount ==
                    normalizationEquivalentMatchCount
        ) {
            "Reported and omitted normalization-equivalent examples " +
                    "must equal normalizationEquivalentMatchCount."
        }

        require(
            trueMissingRuntimeCanonicalIds.size.toLong() +
                    omittedTrueMissingRuntimeCanonicalIdCount ==
                    trueMissingRuntimeEntryCount
        ) {
            "Reported and omitted true missing IDs must equal " +
                    "trueMissingRuntimeEntryCount."
        }

        require(
            trueAdditionalRuntimeCanonicalIds.size.toLong() +
                    omittedTrueAdditionalRuntimeCanonicalIdCount ==
                    trueAdditionalRuntimeEntryCount
        ) {
            "Reported and omitted true additional IDs must equal " +
                    "trueAdditionalRuntimeEntryCount."
        }

        require(
            normalizationEquivalentExamples ==
                    normalizationEquivalentExamples
                        .distinct()
                        .sortedWith(
                            compareBy<
                                    ResultingNutritionCoverageNormalizationEquivalentPair
                                    > { pair ->
                                pair.normalizedCanonicalId
                            }
                                .thenBy { pair ->
                                    pair.aggregateCanonicalId
                                }
                                .thenBy { pair ->
                                    pair.runtimeCanonicalId
                                }
                        )
        ) {
            "Normalization-equivalent examples must be unique and sorted."
        }

        require(
            trueMissingRuntimeCanonicalIds ==
                    trueMissingRuntimeCanonicalIds
                        .distinct()
                        .sorted()
        ) {
            "True missing canonical IDs must be unique and sorted."
        }

        require(
            trueAdditionalRuntimeCanonicalIds ==
                    trueAdditionalRuntimeCanonicalIds
                        .distinct()
                        .sorted()
        ) {
            "True additional canonical IDs must be unique and sorted."
        }
    }
}

data class ResultingNutritionCoverageNormalizationEquivalentPair(
    val normalizedCanonicalId: String,
    val aggregateCanonicalId: String,
    val runtimeCanonicalId: String
) {

    init {
        require(normalizedCanonicalId.isNotBlank())
        require(aggregateCanonicalId.isNotBlank())
        require(runtimeCanonicalId.isNotBlank())

        require(
            aggregateCanonicalId !=
                    runtimeCanonicalId
        ) {
            "Normalization-equivalent pair must not be an exact match."
        }
    }
}