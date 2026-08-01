package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.analysis

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.PriorityQueue

class ResultingNutritionMacronutrientWarningAnalyzer(
    private val maximumReportedExamples: Int =
        DEFAULT_MAXIMUM_REPORTED_EXAMPLES
) {

    init {
        require(maximumReportedExamples >= 0)
    }

    fun analyze(
        inputFile: File
    ): ResultingNutritionMacronutrientWarningAnalysis {

        require(inputFile.isFile) {
            "Resulting nutrition artifact does not exist: " +
                    inputFile.absolutePath
        }

        val startedAt =
            System.currentTimeMillis()

        var entryCount =
            0L

        var warningEntryCount =
            0L

        var fiberDoubleCountingCandidateCount =
            0L

        var coreMacronutrientExcessCount =
            0L

        var maximumMacronutrientSum =
            0.0

        var maximumExcessGrams =
            0.0

        val countsByClassification =
            MacronutrientWarningClassification
                .entries
                .associateWith { 0L }
                .toMutableMap()

        val countsByExcessBucket =
            MacronutrientWarningExcessBucket
                .entries
                .associateWith { 0L }
                .toMutableMap()

        val exampleQueue =
            PriorityQueue(
                compareBy<
                        ResultingNutritionMacronutrientWarningExample
                        > { example ->
                    example.excessGrams
                }
                    .thenBy { example ->
                        example.canonicalId
                    }
            )

        JsonReader(
            inputFile
                .inputStream()
                .buffered()
                .reader(
                    StandardCharsets.UTF_8
                )
        ).use { reader ->

            reader.beginObject()

            var entriesFound =
                false

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    ENTRIES_PROPERTY_NAME -> {
                        require(!entriesFound) {
                            "Nutrition artifact contains multiple " +
                                    "entries objects."
                        }

                        entriesFound =
                            true

                        reader.beginObject()

                        while (reader.hasNext()) {
                            val canonicalId =
                                reader.nextName()

                            val entry =
                                readEntry(
                                    reader =
                                        reader
                                )

                            entryCount++

                            val warning =
                                analyzeEntry(
                                    canonicalId =
                                        canonicalId,
                                    entry =
                                        entry
                                )
                                    ?: continue

                            warningEntryCount++

                            when (warning.classification) {
                                MacronutrientWarningClassification
                                    .FIBER_DOUBLE_COUNTING_CANDIDATE -> {

                                    fiberDoubleCountingCandidateCount++
                                }

                                MacronutrientWarningClassification
                                    .CORE_MACRONUTRIENT_SUM_EXCEEDS_MAXIMUM -> {

                                    coreMacronutrientExcessCount++
                                }
                            }

                            countsByClassification.compute(
                                warning.classification
                            ) { _, current ->
                                (current ?: 0L) + 1L
                            }

                            val excessBucket =
                                classifyExcess(
                                    excessGrams =
                                        warning.excessGrams
                                )

                            countsByExcessBucket.compute(
                                excessBucket
                            ) { _, current ->
                                (current ?: 0L) + 1L
                            }

                            maximumMacronutrientSum =
                                maxOf(
                                    maximumMacronutrientSum,
                                    warning.macronutrientSum
                                )

                            maximumExcessGrams =
                                maxOf(
                                    maximumExcessGrams,
                                    warning.excessGrams
                                )

                            retainExample(
                                queue =
                                    exampleQueue,
                                example =
                                    warning
                            )
                        }

                        reader.endObject()
                    }

                    else ->
                        reader.skipValue()
                }
            }

            reader.endObject()

            require(entriesFound) {
                "Nutrition artifact contains no entries object: " +
                        inputFile.absolutePath
            }
        }

        require(entryCount > 0L) {
            "Nutrition artifact must contain at least one entry."
        }

        val examples =
            exampleQueue
                .toList()
                .sortedWith(
                    compareByDescending<
                            ResultingNutritionMacronutrientWarningExample
                            > { example ->
                        example.excessGrams
                    }
                        .thenBy { example ->
                            example.canonicalId
                        }
                )

        return ResultingNutritionMacronutrientWarningAnalysis(
            inputFile =
                inputFile.canonicalFile,
            inputFileSizeBytes =
                inputFile.length(),
            entryCount =
                entryCount,
            warningEntryCount =
                warningEntryCount,
            fiberDoubleCountingCandidateCount =
                fiberDoubleCountingCandidateCount,
            coreMacronutrientExcessCount =
                coreMacronutrientExcessCount,
            countsByClassification =
                countsByClassification
                    .filterValues { count ->
                        count > 0L
                    }
                    .toSortedMap(
                        compareBy { classification ->
                            classification.name
                        }
                    ),
            countsByExcessBucket =
                countsByExcessBucket
                    .filterValues { count ->
                        count > 0L
                    }
                    .toSortedMap(
                        compareBy { bucket ->
                            bucket.name
                        }
                    ),
            maximumMacronutrientSum =
                maximumMacronutrientSum,
            maximumExcessGrams =
                maximumExcessGrams,
            examples =
                examples,
            durationMillis =
                System.currentTimeMillis() -
                        startedAt
        )
    }

    private fun analyzeEntry(
        canonicalId: String,
        entry: JsonObject
    ): ResultingNutritionMacronutrientWarningExample? {

        if (canonicalId.isBlank()) {
            return null
        }

        val presentNutrients =
            readPresentNutrients(
                entry =
                    entry
            )

        val fat =
            readPresentDouble(
                entry =
                    entry,
                presentNutrients =
                    presentNutrients,
                nutrientKey =
                    FAT_KEY
            )

        val carbohydrates =
            readPresentDouble(
                entry =
                    entry,
                presentNutrients =
                    presentNutrients,
                nutrientKey =
                    CARBOHYDRATES_KEY
            )

        val fiber =
            readPresentDouble(
                entry =
                    entry,
                presentNutrients =
                    presentNutrients,
                nutrientKey =
                    FIBER_KEY
            )

        val protein =
            readPresentDouble(
                entry =
                    entry,
                presentNutrients =
                    presentNutrients,
                nutrientKey =
                    PROTEIN_KEY
            )

        val presentValues =
            listOfNotNull(
                fat,
                carbohydrates,
                fiber,
                protein
            )

        if (
            presentValues.size <
            MINIMUM_MACRONUTRIENTS_FOR_SUM_CHECK
        ) {
            return null
        }

        val coreMacronutrientSum =
            listOfNotNull(
                fat,
                carbohydrates,
                protein
            )
                .sum()

        val macronutrientSum =
            presentValues.sum()

        if (
            macronutrientSum <=
            MAXIMUM_MACRONUTRIENT_SUM_GRAMS
        ) {
            return null
        }

        val classification =
            if (
                coreMacronutrientSum <=
                MAXIMUM_MACRONUTRIENT_SUM_GRAMS
            ) {
                MacronutrientWarningClassification
                    .FIBER_DOUBLE_COUNTING_CANDIDATE
            } else {
                MacronutrientWarningClassification
                    .CORE_MACRONUTRIENT_SUM_EXCEEDS_MAXIMUM
            }

        return ResultingNutritionMacronutrientWarningExample(
            canonicalId =
                canonicalId,
            classification =
                classification,
            presentMacronutrientCount =
                presentValues.size,
            fat =
                fat,
            carbohydrates =
                carbohydrates,
            fiber =
                fiber,
            protein =
                protein,
            coreMacronutrientSum =
                coreMacronutrientSum,
            macronutrientSum =
                macronutrientSum,
            excessGrams =
                macronutrientSum -
                        MAXIMUM_MACRONUTRIENT_SUM_GRAMS
        )
    }

    private fun readEntry(
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

    private fun readPresentNutrients(
        entry: JsonObject
    ): Set<String>? {

        val element =
            entry.get(
                PRESENT_NUTRIENTS_PROPERTY_NAME
            )
                ?: return null

        if (!element.isJsonArray) {
            return emptySet()
        }

        return element
            .asJsonArray
            .asSequence()
            .filter { item ->
                item.isJsonPrimitive &&
                        item.asJsonPrimitive.isString
            }
            .map { item ->
                item.asString
            }
            .toSortedSet()
    }

    private fun readPresentDouble(
        entry: JsonObject,
        presentNutrients: Set<String>?,
        nutrientKey: String
    ): Double? {

        if (
            presentNutrients != null &&
            nutrientKey !in presentNutrients
        ) {
            return null
        }

        val element =
            entry.get(
                nutrientKey
            )
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

    private fun classifyExcess(
        excessGrams: Double
    ): MacronutrientWarningExcessBucket =
        when {
            excessGrams <= 1.0 ->
                MacronutrientWarningExcessBucket
                    .UP_TO_1_GRAM

            excessGrams <= 5.0 ->
                MacronutrientWarningExcessBucket
                    .UP_TO_5_GRAMS

            excessGrams <= 10.0 ->
                MacronutrientWarningExcessBucket
                    .UP_TO_10_GRAMS

            excessGrams <= 20.0 ->
                MacronutrientWarningExcessBucket
                    .UP_TO_20_GRAMS

            else ->
                MacronutrientWarningExcessBucket
                    .MORE_THAN_20_GRAMS
        }

    private fun retainExample(
        queue:
        PriorityQueue<
                ResultingNutritionMacronutrientWarningExample
                >,
        example:
        ResultingNutritionMacronutrientWarningExample
    ) {

        if (maximumReportedExamples == 0) {
            return
        }

        queue +=
            example

        if (
            queue.size >
            maximumReportedExamples
        ) {
            queue.remove()
        }
    }

    private companion object {

        const val ENTRIES_PROPERTY_NAME =
            "entries"

        const val PRESENT_NUTRIENTS_PROPERTY_NAME =
            "presentNutrients"

        const val FAT_KEY =
            "fat"

        const val CARBOHYDRATES_KEY =
            "carbohydrates"

        const val FIBER_KEY =
            "fiber"

        const val PROTEIN_KEY =
            "protein"

        const val MAXIMUM_MACRONUTRIENT_SUM_GRAMS =
            105.0

        const val MINIMUM_MACRONUTRIENTS_FOR_SUM_CHECK =
            3

        const val DEFAULT_MAXIMUM_REPORTED_EXAMPLES =
            250
    }
}