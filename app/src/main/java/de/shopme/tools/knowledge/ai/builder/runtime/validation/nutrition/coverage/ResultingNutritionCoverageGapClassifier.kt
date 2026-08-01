package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage

import com.google.gson.stream.JsonReader
import java.io.File
import java.nio.charset.StandardCharsets

class ResultingNutritionCoverageGapClassifier(
    private val normalizer:
    ResultingNutritionCoverageCanonicalIdNormalizer =
        ResultingNutritionCoverageCanonicalIdNormalizer(),
    private val maximumReportedNormalizationEquivalentExamples: Int =
        DEFAULT_MAXIMUM_REPORTED_ITEMS,
    private val maximumReportedTrueMissingRuntimeCanonicalIds: Int =
        DEFAULT_MAXIMUM_REPORTED_ITEMS,
    private val maximumReportedTrueAdditionalRuntimeCanonicalIds: Int =
        DEFAULT_MAXIMUM_REPORTED_ITEMS
) {

    init {
        require(
            maximumReportedNormalizationEquivalentExamples >= 0
        )

        require(
            maximumReportedTrueMissingRuntimeCanonicalIds >= 0
        )

        require(
            maximumReportedTrueAdditionalRuntimeCanonicalIds >= 0
        )
    }

    fun classify(
        aggregateFile: File,
        runtimeFile: File
    ): ResultingNutritionCoverageGapClassification {

        require(aggregateFile.isFile) {
            "OFF Nutrition aggregate file does not exist: " +
                    aggregateFile.absolutePath
        }

        require(runtimeFile.isFile) {
            "Resulting Nutrition runtime file does not exist: " +
                    runtimeFile.absolutePath
        }

        val startedAt =
            System.currentTimeMillis()

        val aggregateCanonicalIds =
            readAggregateCanonicalIds(
                aggregateFile =
                    aggregateFile
            )

        val runtimeCanonicalIds =
            readRuntimeCanonicalIds(
                runtimeFile =
                    runtimeFile
            )

        val exactCanonicalIds =
            aggregateCanonicalIds
                .intersect(
                    runtimeCanonicalIds
                )

        val unmatchedAggregateCanonicalIds =
            aggregateCanonicalIds
                .asSequence()
                .filterNot(
                    exactCanonicalIds::contains
                )
                .toList()

        val unmatchedRuntimeCanonicalIds =
            runtimeCanonicalIds
                .asSequence()
                .filterNot(
                    exactCanonicalIds::contains
                )
                .toList()

        val aggregateGroups =
            groupByNormalizedCanonicalId(
                canonicalIds =
                    unmatchedAggregateCanonicalIds
            )

        val runtimeGroups =
            groupByNormalizedCanonicalId(
                canonicalIds =
                    unmatchedRuntimeCanonicalIds
            )

        val allNormalizedCanonicalIds =
            (
                    aggregateGroups.keys +
                            runtimeGroups.keys
                    )
                .toSortedSet()

        val normalizationEquivalentExamples =
            mutableListOf<
                    ResultingNutritionCoverageNormalizationEquivalentPair
                    >()

        val trueMissingRuntimeCanonicalIds =
            mutableListOf<String>()

        val trueAdditionalRuntimeCanonicalIds =
            mutableListOf<String>()

        var normalizationEquivalentMatchCount =
            0L

        var trueMissingRuntimeEntryCount =
            0L

        var trueAdditionalRuntimeEntryCount =
            0L

        var normalizationCollisionGroupCount =
            0L

        allNormalizedCanonicalIds.forEach { normalizedCanonicalId ->

            val aggregateGroup =
                aggregateGroups[
                    normalizedCanonicalId
                ]
                    .orEmpty()
                    .sorted()

            val runtimeGroup =
                runtimeGroups[
                    normalizedCanonicalId
                ]
                    .orEmpty()
                    .sorted()

            if (
                aggregateGroup.size > 1 ||
                runtimeGroup.size > 1
            ) {
                normalizationCollisionGroupCount++
            }

            val matchedCount =
                minOf(
                    aggregateGroup.size,
                    runtimeGroup.size
                )

            repeat(
                matchedCount
            ) { index ->

                val aggregateCanonicalId =
                    aggregateGroup[
                        index
                    ]

                val runtimeCanonicalId =
                    runtimeGroup[
                        index
                    ]

                normalizationEquivalentMatchCount++

                if (
                    normalizationEquivalentExamples.size <
                    maximumReportedNormalizationEquivalentExamples
                ) {
                    normalizationEquivalentExamples +=
                        ResultingNutritionCoverageNormalizationEquivalentPair(
                            normalizedCanonicalId =
                                normalizedCanonicalId,
                            aggregateCanonicalId =
                                aggregateCanonicalId,
                            runtimeCanonicalId =
                                runtimeCanonicalId
                        )
                }
            }

            aggregateGroup
                .drop(
                    matchedCount
                )
                .forEach { canonicalId ->

                    trueMissingRuntimeEntryCount++

                    if (
                        trueMissingRuntimeCanonicalIds.size <
                        maximumReportedTrueMissingRuntimeCanonicalIds
                    ) {
                        trueMissingRuntimeCanonicalIds +=
                            canonicalId
                    }
                }

            runtimeGroup
                .drop(
                    matchedCount
                )
                .forEach { canonicalId ->

                    trueAdditionalRuntimeEntryCount++

                    if (
                        trueAdditionalRuntimeCanonicalIds.size <
                        maximumReportedTrueAdditionalRuntimeCanonicalIds
                    ) {
                        trueAdditionalRuntimeCanonicalIds +=
                            canonicalId
                    }
                }
        }

        val aggregateEntryCount =
            aggregateCanonicalIds.size.toLong()

        val runtimeEntryCount =
            runtimeCanonicalIds.size.toLong()

        val exactMatchCount =
            exactCanonicalIds.size.toLong()

        val exactCoverageRate =
            coverageRate(
                coveredEntryCount =
                    exactMatchCount,
                aggregateEntryCount =
                    aggregateEntryCount
            )

        val effectiveCoverageRate =
            coverageRate(
                coveredEntryCount =
                    exactMatchCount +
                            normalizationEquivalentMatchCount,
                aggregateEntryCount =
                    aggregateEntryCount
            )

        return ResultingNutritionCoverageGapClassification(
            aggregateFile =
                aggregateFile.canonicalFile,
            runtimeFile =
                runtimeFile.canonicalFile,
            aggregateEntryCount =
                aggregateEntryCount,
            runtimeEntryCount =
                runtimeEntryCount,
            exactMatchCount =
                exactMatchCount,
            normalizationEquivalentMatchCount =
                normalizationEquivalentMatchCount,
            trueMissingRuntimeEntryCount =
                trueMissingRuntimeEntryCount,
            trueAdditionalRuntimeEntryCount =
                trueAdditionalRuntimeEntryCount,
            exactCoverageRate =
                exactCoverageRate,
            effectiveCoverageRate =
                effectiveCoverageRate,
            normalizationCollisionGroupCount =
                normalizationCollisionGroupCount,
            normalizationEquivalentExamples =
                normalizationEquivalentExamples
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
                    ),
            trueMissingRuntimeCanonicalIds =
                trueMissingRuntimeCanonicalIds.sorted(),
            trueAdditionalRuntimeCanonicalIds =
                trueAdditionalRuntimeCanonicalIds.sorted(),
            omittedNormalizationEquivalentExampleCount =
                normalizationEquivalentMatchCount -
                        normalizationEquivalentExamples.size.toLong(),
            omittedTrueMissingRuntimeCanonicalIdCount =
                trueMissingRuntimeEntryCount -
                        trueMissingRuntimeCanonicalIds.size.toLong(),
            omittedTrueAdditionalRuntimeCanonicalIdCount =
                trueAdditionalRuntimeEntryCount -
                        trueAdditionalRuntimeCanonicalIds.size.toLong(),
            durationMillis =
                System.currentTimeMillis() -
                        startedAt
        )
    }

    private fun groupByNormalizedCanonicalId(
        canonicalIds: List<String>
    ): Map<String, List<String>> {

        val groups =
            mutableMapOf<
                    String,
                    MutableList<String>
                    >()

        canonicalIds.forEach { canonicalId ->

            val normalizedCanonicalId =
                normalizer.normalize(
                    canonicalId =
                        canonicalId
                )

            require(normalizedCanonicalId.isNotBlank()) {
                "Canonical ID becomes blank after coverage " +
                        "normalization: $canonicalId"
            }

            groups
                .getOrPut(
                    normalizedCanonicalId
                ) {
                    mutableListOf()
                }
                .add(
                    canonicalId
                )
        }

        return groups
            .mapValues { (_, values) ->
                values.sorted()
            }
            .toSortedMap()
    }

    private fun readAggregateCanonicalIds(
        aggregateFile: File
    ): Set<String> {

        val canonicalIds =
            linkedSetOf<String>()

        jsonReader(
            file =
                aggregateFile
        ).use { reader ->

            reader.beginArray()

            var previousCanonicalId: String? =
                null

            while (reader.hasNext()) {
                reader.beginObject()

                var canonicalId: String? =
                    null

                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        CANONICAL_ID_PROPERTY_NAME -> {
                            require(canonicalId == null) {
                                "OFF Nutrition aggregate contains " +
                                        "multiple canonicalId properties."
                            }

                            canonicalId =
                                reader.nextString()
                        }

                        else ->
                            reader.skipValue()
                    }
                }

                reader.endObject()

                val requiredCanonicalId =
                    requireNotNull(
                        canonicalId
                    ) {
                        "OFF Nutrition aggregate entry has no " +
                                "canonicalId."
                    }

                validateCanonicalId(
                    canonicalId =
                        requiredCanonicalId,
                    previousCanonicalId =
                        previousCanonicalId,
                    artifactDescription =
                        "OFF Nutrition aggregate dataset"
                )

                require(
                    canonicalIds.add(
                        requiredCanonicalId
                    )
                ) {
                    "OFF Nutrition aggregate contains duplicate " +
                            "canonicalId: $requiredCanonicalId"
                }

                previousCanonicalId =
                    requiredCanonicalId
            }

            reader.endArray()
        }

        require(canonicalIds.isNotEmpty()) {
            "OFF Nutrition aggregate dataset must not be empty."
        }

        return canonicalIds
    }

    private fun readRuntimeCanonicalIds(
        runtimeFile: File
    ): Set<String> {

        val canonicalIds =
            linkedSetOf<String>()

        jsonReader(
            file =
                runtimeFile
        ).use { reader ->

            reader.beginObject()

            var entriesFound =
                false

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    ENTRIES_PROPERTY_NAME -> {
                        require(!entriesFound) {
                            "Runtime Nutrition artifact contains " +
                                    "multiple entries properties."
                        }

                        entriesFound =
                            true

                        reader.beginObject()

                        var previousCanonicalId: String? =
                            null

                        while (reader.hasNext()) {
                            val canonicalId =
                                reader.nextName()

                            validateCanonicalId(
                                canonicalId =
                                    canonicalId,
                                previousCanonicalId =
                                    previousCanonicalId,
                                artifactDescription =
                                    "Runtime Nutrition artifact"
                            )

                            require(
                                canonicalIds.add(
                                    canonicalId
                                )
                            ) {
                                "Runtime Nutrition artifact contains " +
                                        "duplicate canonicalId: " +
                                        canonicalId
                            }

                            previousCanonicalId =
                                canonicalId

                            reader.skipValue()
                        }

                        reader.endObject()
                    }

                    else ->
                        reader.skipValue()
                }
            }

            reader.endObject()

            require(entriesFound) {
                "Runtime Nutrition artifact contains no entries property."
            }
        }

        require(canonicalIds.isNotEmpty()) {
            "Runtime Nutrition artifact must not be empty."
        }

        return canonicalIds
    }

    private fun validateCanonicalId(
        canonicalId: String,
        previousCanonicalId: String?,
        artifactDescription: String
    ) {
        require(canonicalId.isNotBlank()) {
            "$artifactDescription contains blank canonicalId."
        }

        if (previousCanonicalId != null) {
            require(
                previousCanonicalId <
                        canonicalId
            ) {
                "$artifactDescription canonical IDs must be globally " +
                        "unique and strictly ordered. " +
                        "previous=$previousCanonicalId, " +
                        "current=$canonicalId"
            }
        }
    }

    private fun coverageRate(
        coveredEntryCount: Long,
        aggregateEntryCount: Long
    ): Double =
        if (aggregateEntryCount == 0L) {
            0.0
        } else {
            coveredEntryCount.toDouble() /
                    aggregateEntryCount.toDouble()
        }

    private fun jsonReader(
        file: File
    ): JsonReader =
        JsonReader(
            file
                .inputStream()
                .buffered()
                .reader(
                    StandardCharsets.UTF_8
                )
        )

    private companion object {

        const val CANONICAL_ID_PROPERTY_NAME =
            "canonicalId"

        const val ENTRIES_PROPERTY_NAME =
            "entries"

        const val DEFAULT_MAXIMUM_REPORTED_ITEMS =
            250
    }
}