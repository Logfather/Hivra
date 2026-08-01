package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage.ResultingNutritionCoverageCanonicalIdNormalizer
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.PriorityQueue
import kotlin.math.abs

class ResultingNutritionConflictAnalyzer(
    private val canonicalIdNormalizer:
    ResultingNutritionCoverageCanonicalIdNormalizer =
        ResultingNutritionCoverageCanonicalIdNormalizer(),
    private val maximumReportedConflictExamples: Int =
        DEFAULT_MAXIMUM_REPORTED_CONFLICT_EXAMPLES
) {

    init {
        require(maximumReportedConflictExamples >= 0)
    }

    fun analyze(
        aggregateFile: File,
        runtimeFile: File
    ): ResultingNutritionConflictAnalysis {

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

        val runtimeEntries =
            readRuntimeEntries(
                runtimeFile = runtimeFile
            )
                .toMutableMap()

        val runtimeEntryCount =
            runtimeEntries.size.toLong()

        val unmatchedAggregates =
            mutableListOf<NutritionConflictSnapshot>()

        val accumulator =
            ConflictAccumulator(
                maximumReportedConflictExamples =
                    maximumReportedConflictExamples
            )

        var aggregateEntryCount =
            0L

        readAggregateEntries(
            aggregateFile = aggregateFile
        ) { aggregate ->

            aggregateEntryCount++

            val runtime =
                runtimeEntries.remove(
                    aggregate.canonicalId
                )

            if (runtime != null) {
                accumulator.compare(
                    aggregate = aggregate,
                    runtime = runtime,
                    matchType =
                        ResultingNutritionConflictMatchType.EXACT
                )
            } else {
                unmatchedAggregates +=
                    aggregate
            }
        }

        val aggregateGroups =
            unmatchedAggregates
                .groupBy { aggregate ->
                    normalizeCanonicalId(
                        aggregate.canonicalId
                    )
                }
                .mapValues { (_, entries) ->
                    entries.sortedBy { entry ->
                        entry.canonicalId
                    }
                }
                .toSortedMap()

        val runtimeGroups =
            runtimeEntries
                .values
                .groupBy { runtime ->
                    normalizeCanonicalId(
                        runtime.canonicalId
                    )
                }
                .mapValues { (_, entries) ->
                    entries.sortedBy { entry ->
                        entry.canonicalId
                    }
                }
                .toSortedMap()

        aggregateGroups.keys
            .intersect(
                runtimeGroups.keys
            )
            .sorted()
            .forEach { normalizedCanonicalId ->

                val aggregates =
                    aggregateGroups
                        .getValue(
                            normalizedCanonicalId
                        )

                val runtimes =
                    runtimeGroups
                        .getValue(
                            normalizedCanonicalId
                        )

                val matchedCount =
                    minOf(
                        aggregates.size,
                        runtimes.size
                    )

                repeat(
                    matchedCount
                ) { index ->

                    accumulator.compare(
                        aggregate =
                            aggregates[index],
                        runtime =
                            runtimes[index],
                        matchType =
                            ResultingNutritionConflictMatchType
                                .NORMALIZATION_EQUIVALENT
                    )
                }
            }

        return accumulator.toAnalysis(
            aggregateFile =
                aggregateFile,
            runtimeFile =
                runtimeFile,
            aggregateEntryCount =
                aggregateEntryCount,
            runtimeEntryCount =
                runtimeEntryCount,
            durationMillis =
                System.currentTimeMillis() -
                        startedAt
        )
    }

    private fun readRuntimeEntries(
        runtimeFile: File
    ): Map<String, NutritionConflictSnapshot> {

        val entries =
            linkedMapOf<String, NutritionConflictSnapshot>()

        jsonReader(
            file = runtimeFile
        ).use { reader ->

            reader.beginObject()

            var entriesFound =
                false

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    ENTRIES_PROPERTY_NAME -> {
                        require(!entriesFound) {
                            "Runtime Nutrition artifact contains " +
                                    "multiple entries objects."
                        }

                        entriesFound =
                            true

                        reader.beginObject()

                        var previousCanonicalId: String? =
                            null

                        while (reader.hasNext()) {
                            val canonicalId =
                                reader.nextName()

                            validateCanonicalIdOrder(
                                canonicalId =
                                    canonicalId,
                                previousCanonicalId =
                                    previousCanonicalId,
                                artifactDescription =
                                    "Runtime Nutrition artifact"
                            )

                            val payload =
                                readJsonObject(
                                    reader = reader
                                )

                            val snapshot =
                                runtimeSnapshot(
                                    canonicalId =
                                        canonicalId,
                                    payload =
                                        payload
                                )

                            require(
                                entries.put(
                                    canonicalId,
                                    snapshot
                                ) == null
                            ) {
                                "Runtime Nutrition artifact contains " +
                                        "duplicate canonicalId: " +
                                        canonicalId
                            }

                            previousCanonicalId =
                                canonicalId
                        }

                        reader.endObject()
                    }

                    else ->
                        reader.skipValue()
                }
            }

            reader.endObject()

            require(entriesFound) {
                "Runtime Nutrition artifact contains no entries object."
            }
        }

        require(entries.isNotEmpty()) {
            "Runtime Nutrition artifact must not be empty."
        }

        return entries
    }

    private fun readAggregateEntries(
        aggregateFile: File,
        consumer: (NutritionConflictSnapshot) -> Unit
    ) {
        jsonReader(
            file = aggregateFile
        ).use { reader ->

            reader.beginArray()

            var previousCanonicalId: String? =
                null

            var entryCount =
                0L

            while (reader.hasNext()) {
                val entry =
                    readJsonObject(
                        reader = reader
                    )

                val canonicalId =
                    entry
                        .get(
                            CANONICAL_ID_PROPERTY_NAME
                        )
                        ?.takeIf { element ->
                            element.isJsonPrimitive
                        }
                        ?.asString
                        ?: error(
                            "OFF Nutrition aggregate entry has no " +
                                    "canonicalId."
                        )

                validateCanonicalIdOrder(
                    canonicalId =
                        canonicalId,
                    previousCanonicalId =
                        previousCanonicalId,
                    artifactDescription =
                        "OFF Nutrition aggregate dataset"
                )

                val nutrition =
                    entry
                        .getAsJsonObject(
                            NUTRITION_PROPERTY_NAME
                        )
                        ?: error(
                            "OFF Nutrition aggregate entry has no " +
                                    "nutrition payload: $canonicalId"
                        )

                val values =
                    ResultingNutritionConflictNutrient
                        .entries
                        .mapNotNull { nutrient ->

                            readFiniteDouble(
                                payload =
                                    nutrition,
                                key =
                                    nutrient.aggregateKey
                            )
                                ?.let { value ->
                                    nutrient to value
                                }
                        }
                        .toMap()

                consumer(
                    NutritionConflictSnapshot(
                        canonicalId =
                            canonicalId,
                        values =
                            values
                    )
                )

                previousCanonicalId =
                    canonicalId

                entryCount++
            }

            reader.endArray()

            require(entryCount > 0L) {
                "OFF Nutrition aggregate dataset must not be empty."
            }
        }
    }

    private fun runtimeSnapshot(
        canonicalId: String,
        payload: JsonObject
    ): NutritionConflictSnapshot {

        val presentNutrients =
            payload
                .get(
                    PRESENT_NUTRIENTS_PROPERTY_NAME
                )
                ?.takeIf { element ->
                    element.isJsonArray
                }
                ?.asJsonArray
                ?.asSequence()
                ?.filter { item ->
                    item.isJsonPrimitive &&
                            item.asJsonPrimitive.isString
                }
                ?.map { item ->
                    item.asString
                }
                ?.toSet()

        val values =
            ResultingNutritionConflictNutrient
                .entries
                .mapNotNull { nutrient ->

                    if (
                        presentNutrients != null &&
                        nutrient.runtimeKey !in presentNutrients
                    ) {
                        return@mapNotNull null
                    }

                    readFiniteDouble(
                        payload =
                            payload,
                        key =
                            nutrient.runtimeKey
                    )
                        ?.let { value ->
                            nutrient to value
                        }
                }
                .toMap()

        return NutritionConflictSnapshot(
            canonicalId =
                canonicalId,
            values =
                values
        )
    }

    private fun readJsonObject(
        reader: JsonReader
    ): JsonObject {

        val element =
            JsonParser.parseReader(
                reader
            )

        require(element.isJsonObject) {
            "Nutrition entry must be a JSON object."
        }

        return element.asJsonObject
    }

    private fun readFiniteDouble(
        payload: JsonObject,
        key: String
    ): Double? {

        val element =
            payload.get(key)
                ?: return null

        if (
            !element.isJsonPrimitive ||
            !element.asJsonPrimitive.isNumber
        ) {
            return null
        }

        return runCatching {
            element.asDouble
        }
            .getOrNull()
            ?.takeIf(
                Double::isFinite
            )
    }

    private fun normalizeCanonicalId(
        canonicalId: String
    ): String =
        canonicalIdNormalizer
            .normalize(
                canonicalId =
                    canonicalId
            )
            .also { normalized ->
                require(normalized.isNotBlank()) {
                    "Canonical ID becomes blank after normalization: " +
                            canonicalId
                }
            }

    private fun validateCanonicalIdOrder(
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

    private data class NutritionConflictSnapshot(
        val canonicalId: String,
        val values:
        Map<ResultingNutritionConflictNutrient, Double>
    )

    private class ConflictAccumulator(
        private val maximumReportedConflictExamples: Int
    ) {

        private var exactMatchedEntryCount =
            0L

        private var normalizationEquivalentMatchedEntryCount =
            0L

        private var comparableEntryCount =
            0L

        private var nonComparableEntryCount =
            0L

        private var conflictEntryCount =
            0L

        private var conflictFreeEntryCount =
            0L

        private var nutrientComparisonCount =
            0L

        private var nutrientConflictCount =
            0L

        private val comparisonCountsByNutrient =
            ResultingNutritionConflictNutrient
                .entries
                .associateWith { 0L }
                .toMutableMap()

        private val conflictCountsByNutrient =
            ResultingNutritionConflictNutrient
                .entries
                .associateWith { 0L }
                .toMutableMap()

        private val maximumAbsoluteDifferenceByNutrient =
            ResultingNutritionConflictNutrient
                .entries
                .associateWith { 0.0 }
                .toMutableMap()

        private val conflictExamples =
            PriorityQueue(
                compareBy<ResultingNutritionConflictExample> {
                    it.maximumAbsoluteDifference
                }
                    .thenByDescending {
                        it.aggregateCanonicalId
                    }
            )

        fun compare(
            aggregate: NutritionConflictSnapshot,
            runtime: NutritionConflictSnapshot,
            matchType: ResultingNutritionConflictMatchType
        ) {
            when (matchType) {
                ResultingNutritionConflictMatchType.EXACT ->
                    exactMatchedEntryCount++

                ResultingNutritionConflictMatchType
                    .NORMALIZATION_EQUIVALENT ->
                    normalizationEquivalentMatchedEntryCount++
            }

            val commonNutrients =
                aggregate.values.keys
                    .intersect(
                        runtime.values.keys
                    )
                    .sortedBy { nutrient ->
                        nutrient.name
                    }

            if (commonNutrients.isEmpty()) {
                nonComparableEntryCount++
                return
            }

            comparableEntryCount++

            val conflicts =
                mutableListOf<ResultingNutritionNutrientConflict>()

            commonNutrients.forEach { nutrient ->

                val aggregateValue =
                    aggregate.values
                        .getValue(
                            nutrient
                        )

                val runtimeValue =
                    runtime.values
                        .getValue(
                            nutrient
                        )

                val absoluteDifference =
                    abs(
                        aggregateValue -
                                runtimeValue
                    )

                nutrientComparisonCount++

                comparisonCountsByNutrient.compute(
                    nutrient
                ) { _, current ->
                    (current ?: 0L) + 1L
                }

                maximumAbsoluteDifferenceByNutrient[
                    nutrient
                ] =
                    maxOf(
                        maximumAbsoluteDifferenceByNutrient
                            .getValue(
                                nutrient
                            ),
                        absoluteDifference
                    )

                if (
                    absoluteDifference >
                    nutrient.absoluteTolerance
                ) {
                    nutrientConflictCount++

                    conflictCountsByNutrient.compute(
                        nutrient
                    ) { _, current ->
                        (current ?: 0L) + 1L
                    }

                    conflicts +=
                        ResultingNutritionNutrientConflict(
                            nutrient =
                                nutrient,
                            aggregateValue =
                                aggregateValue,
                            runtimeValue =
                                runtimeValue,
                            absoluteDifference =
                                absoluteDifference,
                            tolerance =
                                nutrient.absoluteTolerance
                        )
                }
            }

            if (conflicts.isEmpty()) {
                conflictFreeEntryCount++
                return
            }

            conflictEntryCount++

            retainExample(
                ResultingNutritionConflictExample(
                    aggregateCanonicalId =
                        aggregate.canonicalId,
                    runtimeCanonicalId =
                        runtime.canonicalId,
                    matchType =
                        matchType,
                    conflictCount =
                        conflicts.size,
                    maximumAbsoluteDifference =
                        conflicts.maxOf { conflict ->
                            conflict.absoluteDifference
                        },
                    conflicts =
                        conflicts.sortedBy { conflict ->
                            conflict.nutrient.name
                        }
                )
            )
        }

        private fun retainExample(
            example: ResultingNutritionConflictExample
        ) {
            if (maximumReportedConflictExamples == 0) {
                return
            }

            conflictExamples +=
                example

            if (
                conflictExamples.size >
                maximumReportedConflictExamples
            ) {
                conflictExamples.remove()
            }
        }

        fun toAnalysis(
            aggregateFile: File,
            runtimeFile: File,
            aggregateEntryCount: Long,
            runtimeEntryCount: Long,
            durationMillis: Long
        ): ResultingNutritionConflictAnalysis {

            val matchedEntryCount =
                exactMatchedEntryCount +
                        normalizationEquivalentMatchedEntryCount

            return ResultingNutritionConflictAnalysis(
                aggregateFile =
                    aggregateFile.canonicalFile,
                runtimeFile =
                    runtimeFile.canonicalFile,
                aggregateEntryCount =
                    aggregateEntryCount,
                runtimeEntryCount =
                    runtimeEntryCount,
                exactMatchedEntryCount =
                    exactMatchedEntryCount,
                normalizationEquivalentMatchedEntryCount =
                    normalizationEquivalentMatchedEntryCount,
                matchedEntryCount =
                    matchedEntryCount,
                comparableEntryCount =
                    comparableEntryCount,
                nonComparableEntryCount =
                    nonComparableEntryCount,
                conflictEntryCount =
                    conflictEntryCount,
                conflictFreeEntryCount =
                    conflictFreeEntryCount,
                nutrientComparisonCount =
                    nutrientComparisonCount,
                nutrientConflictCount =
                    nutrientConflictCount,
                entryConflictRate =
                    rate(
                        numerator =
                            conflictEntryCount,
                        denominator =
                            comparableEntryCount
                    ),
                nutrientConflictRate =
                    rate(
                        numerator =
                            nutrientConflictCount,
                        denominator =
                            nutrientComparisonCount
                    ),
                comparisonCountsByNutrient =
                    comparisonCountsByNutrient
                        .filterValues { count ->
                            count > 0L
                        }
                        .toSortedMap(
                            compareBy { nutrient ->
                                nutrient.name
                            }
                        ),
                conflictCountsByNutrient =
                    conflictCountsByNutrient
                        .filterValues { count ->
                            count > 0L
                        }
                        .toSortedMap(
                            compareBy { nutrient ->
                                nutrient.name
                            }
                        ),
                maximumAbsoluteDifferenceByNutrient =
                    maximumAbsoluteDifferenceByNutrient
                        .filterValues { difference ->
                            difference > 0.0
                        }
                        .toSortedMap(
                            compareBy { nutrient ->
                                nutrient.name
                            }
                        ),
                conflictExamples =
                    conflictExamples
                        .toList()
                        .sortedWith(
                            compareByDescending<
                                    ResultingNutritionConflictExample
                                    > { example ->
                                example.maximumAbsoluteDifference
                            }
                                .thenBy { example ->
                                    example.aggregateCanonicalId
                                }
                        ),
                omittedConflictExampleCount =
                    conflictEntryCount -
                            conflictExamples.size.toLong(),
                durationMillis =
                    durationMillis
            )
        }

        private fun rate(
            numerator: Long,
            denominator: Long
        ): Double =
            if (denominator == 0L) {
                0.0
            } else {
                numerator.toDouble() /
                        denominator.toDouble()
            }
    }

    private companion object {

        const val ENTRIES_PROPERTY_NAME =
            "entries"

        const val CANONICAL_ID_PROPERTY_NAME =
            "canonicalId"

        const val NUTRITION_PROPERTY_NAME =
            "nutrition"

        const val PRESENT_NUTRIENTS_PROPERTY_NAME =
            "presentNutrients"

        const val DEFAULT_MAXIMUM_REPORTED_CONFLICT_EXAMPLES =
            250
    }
}