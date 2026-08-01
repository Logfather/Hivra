package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage

import com.google.gson.stream.JsonReader
import java.io.File
import java.nio.charset.StandardCharsets

class ResultingNutritionCoverageAnalyzer(
    private val maximumReportedMissingRuntimeCanonicalIds: Int =
        DEFAULT_MAXIMUM_REPORTED_IDS,
    private val maximumReportedAdditionalRuntimeCanonicalIds: Int =
        DEFAULT_MAXIMUM_REPORTED_IDS
) {

    init {
        require(
            maximumReportedMissingRuntimeCanonicalIds >= 0
        ) {
            "maximumReportedMissingRuntimeCanonicalIds must not " +
                    "be negative."
        }

        require(
            maximumReportedAdditionalRuntimeCanonicalIds >= 0
        ) {
            "maximumReportedAdditionalRuntimeCanonicalIds must not " +
                    "be negative."
        }
    }

    fun analyze(
        aggregateFile: File,
        runtimeFile: File
    ): ResultingNutritionCoverageAnalysis {

        require(aggregateFile.isFile) {
            "OFF nutrition aggregate file does not exist: " +
                    aggregateFile.absolutePath
        }

        require(runtimeFile.isFile) {
            "Resulting Nutrition runtime file does not exist: " +
                    runtimeFile.absolutePath
        }

        val startedAt =
            System.currentTimeMillis()

        val runtimeCanonicalIds =
            readRuntimeCanonicalIds(
                runtimeFile =
                    runtimeFile
            )

        val unmatchedRuntimeCanonicalIds =
            runtimeCanonicalIds.toMutableSet()

        val missingRuntimeCanonicalIds =
            mutableListOf<String>()

        var aggregateEntryCount =
            0L

        var coveredAggregateEntryCount =
            0L

        var missingRuntimeEntryCount =
            0L

        readAggregateCanonicalIds(
            aggregateFile =
                aggregateFile
        ) { canonicalId ->

            aggregateEntryCount++

            if (
                unmatchedRuntimeCanonicalIds.remove(
                    canonicalId
                )
            ) {
                coveredAggregateEntryCount++
            } else {
                missingRuntimeEntryCount++

                if (
                    missingRuntimeCanonicalIds.size <
                    maximumReportedMissingRuntimeCanonicalIds
                ) {
                    missingRuntimeCanonicalIds +=
                        canonicalId
                }
            }
        }

        val additionalRuntimeEntryCount =
            unmatchedRuntimeCanonicalIds.size.toLong()

        val additionalRuntimeCanonicalIds =
            unmatchedRuntimeCanonicalIds
                .asSequence()
                .sorted()
                .take(
                    maximumReportedAdditionalRuntimeCanonicalIds
                )
                .toList()

        val aggregateCoverageRate =
            if (aggregateEntryCount == 0L) {
                0.0
            } else {
                coveredAggregateEntryCount.toDouble() /
                        aggregateEntryCount.toDouble()
            }

        return ResultingNutritionCoverageAnalysis(
            aggregateFile =
                aggregateFile.canonicalFile,
            aggregateFileSizeBytes =
                aggregateFile.length(),
            runtimeFile =
                runtimeFile.canonicalFile,
            runtimeFileSizeBytes =
                runtimeFile.length(),
            aggregateEntryCount =
                aggregateEntryCount,
            runtimeEntryCount =
                runtimeCanonicalIds.size.toLong(),
            coveredAggregateEntryCount =
                coveredAggregateEntryCount,
            missingRuntimeEntryCount =
                missingRuntimeEntryCount,
            additionalRuntimeEntryCount =
                additionalRuntimeEntryCount,
            aggregateCoverageRate =
                aggregateCoverageRate,
            missingRuntimeCanonicalIds =
                missingRuntimeCanonicalIds.sorted(),
            additionalRuntimeCanonicalIds =
                additionalRuntimeCanonicalIds,
            omittedMissingRuntimeCanonicalIdCount =
                missingRuntimeEntryCount -
                        missingRuntimeCanonicalIds.size.toLong(),
            omittedAdditionalRuntimeCanonicalIdCount =
                additionalRuntimeEntryCount -
                        additionalRuntimeCanonicalIds.size.toLong(),
            durationMillis =
                System.currentTimeMillis() -
                        startedAt
        )
    }

    private fun readRuntimeCanonicalIds(
        runtimeFile: File
    ): Set<String> {

        val canonicalIds =
            HashSet<String>()

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
                "Runtime Nutrition artifact contains no entries " +
                        "property: ${runtimeFile.absolutePath}"
            }
        }

        require(canonicalIds.isNotEmpty()) {
            "Runtime Nutrition artifact must not be empty: " +
                    runtimeFile.absolutePath
        }

        return canonicalIds
    }

    private fun readAggregateCanonicalIds(
        aggregateFile: File,
        consumer: (String) -> Unit
    ) {

        jsonReader(
            file =
                aggregateFile
        ).use { reader ->

            reader.beginArray()

            var previousCanonicalId: String? =
                null

            var aggregateEntryCount =
                0L

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

                previousCanonicalId =
                    requiredCanonicalId

                aggregateEntryCount++

                consumer(
                    requiredCanonicalId
                )
            }

            reader.endArray()

            require(aggregateEntryCount > 0L) {
                "OFF Nutrition aggregate dataset must not be empty: " +
                        aggregateFile.absolutePath
            }
        }
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

        const val ENTRIES_PROPERTY_NAME =
            "entries"

        const val CANONICAL_ID_PROPERTY_NAME =
            "canonicalId"

        const val DEFAULT_MAXIMUM_REPORTED_IDS =
            250
    }
}