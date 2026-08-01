package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition

import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.policy.ResultingNutritionMacronutrientWarningPolicy
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.math.max

class ResultingNutritionKnowledgeValidator(
    private val maximumReportedWarnings: Int =
        DEFAULT_MAXIMUM_REPORTED_WARNINGS,
    private val maximumReportedErrors: Int =
        DEFAULT_MAXIMUM_REPORTED_ERRORS,
    private val macronutrientWarningPolicy:
    ResultingNutritionMacronutrientWarningPolicy =
        ResultingNutritionMacronutrientWarningPolicy()
) {

    init {
        require(maximumReportedWarnings >= 0)
        require(maximumReportedErrors >= 0)
    }

    fun validate(
        inputFile: File
    ): ResultingNutritionKnowledgeValidationResult {

        require(inputFile.isFile) {
            "Nutrition knowledge artifact does not exist: " +
                    inputFile.absolutePath
        }

        val startedAt =
            System.currentTimeMillis()

        var entryCount =
            0L

        var validEntryCount =
            0L

        var warningEntryCount =
            0L

        var rejectedEntryCount =
            0L

        var warningCount =
            0L

        var errorCount =
            0L

        val countsByReason =
            ResultingNutritionKnowledgeValidationReason
                .entries
                .associateWith { 0L }
                .toMutableMap()

        val warningIssues =
            mutableListOf<
                    ResultingNutritionKnowledgeValidationIssue
                    >()

        val errorIssues =
            mutableListOf<
                    ResultingNutritionKnowledgeValidationIssue
                    >()

        var previousCanonicalId: String? =
            null

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
                when (
                    reader.nextName()
                ) {
                    ENTRIES_PROPERTY_NAME -> {
                        require(!entriesFound) {
                            "Nutrition artifact contains multiple entries objects."
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

                            val issues =
                                validateEntry(
                                    canonicalId =
                                        canonicalId,
                                    entry =
                                        entry,
                                    previousCanonicalId =
                                        previousCanonicalId
                                )

                            previousCanonicalId =
                                canonicalId

                            val entryWarnings =
                                issues.filter {
                                    it.severity ==
                                            ResultingNutritionKnowledgeValidationSeverity.WARNING
                                }

                            val entryErrors =
                                issues.filter {
                                    it.severity ==
                                            ResultingNutritionKnowledgeValidationSeverity.ERROR
                                }

                            issues.forEach { issue ->
                                countsByReason.compute(
                                    issue.reason
                                ) { _, count ->
                                    (count ?: 0L) + 1L
                                }
                            }

                            warningCount +=
                                entryWarnings.size

                            errorCount +=
                                entryErrors.size

                            entryWarnings
                                .take(
                                    max(
                                        0,
                                        maximumReportedWarnings -
                                                warningIssues.size
                                    )
                                )
                                .let(
                                    warningIssues::addAll
                                )

                            entryErrors
                                .take(
                                    max(
                                        0,
                                        maximumReportedErrors -
                                                errorIssues.size
                                    )
                                )
                                .let(
                                    errorIssues::addAll
                                )

                            when {
                                entryErrors.isNotEmpty() ->
                                    rejectedEntryCount++

                                entryWarnings.isNotEmpty() ->
                                    warningEntryCount++

                                else ->
                                    validEntryCount++
                            }
                        }

                        reader.endObject()
                    }

                    else -> {
                        /*
                         * Versionen und weitere Metadaten bleiben erlaubt.
                         */
                        reader.skipValue()
                    }
                }
            }

            reader.endObject()

            require(entriesFound) {
                "Nutrition artifact contains no entries object: " +
                        inputFile.absolutePath
            }
        }

        require(entryCount > 0L) {
            "Nutrition artifact must not be empty."
        }

        return ResultingNutritionKnowledgeValidationResult(
            inputFile =
                inputFile.canonicalFile,
            inputFileSizeBytes =
                inputFile.length(),
            entryCount =
                entryCount,
            validEntryCount =
                validEntryCount,
            warningEntryCount =
                warningEntryCount,
            rejectedEntryCount =
                rejectedEntryCount,
            warningCount =
                warningCount,
            errorCount =
                errorCount,
            countsByReason =
                countsByReason
                    .filterValues { count ->
                        count > 0L
                    }
                    .toSortedMap(
                        compareBy {
                            it.name
                        }
                    ),
            warningIssues =
                warningIssues.toList(),
            errorIssues =
                errorIssues.toList(),
            durationMillis =
                System.currentTimeMillis() -
                        startedAt
        )
    }

    private fun readEntry(
        reader: JsonReader
    ): JsonObject {

        val element =
            com.google.gson.JsonParser
                .parseReader(
                    reader
                )

        require(element.isJsonObject) {
            "Nutrition artifact entry must be a JSON object."
        }

        return element.asJsonObject
    }

    private fun validateEntry(
        canonicalId: String,
        entry: JsonObject,
        previousCanonicalId: String?
    ): List<ResultingNutritionKnowledgeValidationIssue> {

        val issues =
            mutableListOf<
                    ResultingNutritionKnowledgeValidationIssue
                    >()

        if (canonicalId.isBlank()) {
            issues +=
                error(
                    canonicalId =
                        EMPTY_CANONICAL_ID_PLACEHOLDER,
                    reason =
                        ResultingNutritionKnowledgeValidationReason
                            .BLANK_CANONICAL_ID,
                    message =
                        "Canonical ID must not be blank."
                )

            return issues
        }

        if (
            previousCanonicalId != null &&
            canonicalId <= previousCanonicalId
        ) {
            val reason =
                if (canonicalId == previousCanonicalId) {
                    ResultingNutritionKnowledgeValidationReason
                        .DUPLICATE_CANONICAL_ID
                } else {
                    ResultingNutritionKnowledgeValidationReason
                        .NON_DETERMINISTIC_CANONICAL_ID_ORDER
                }

            issues +=
                error(
                    canonicalId =
                        canonicalId,
                    reason =
                        reason,
                    message =
                        "Canonical IDs must be globally unique and " +
                                "strictly ordered. previous=" +
                                previousCanonicalId +
                                ", current=" +
                                canonicalId
                )
        }

        val nutritionPayload =
            resolveNutritionPayload(
                entry =
                    entry
            )

        if (nutritionPayload == null) {
            issues +=
                error(
                    canonicalId =
                        canonicalId,
                    reason =
                        ResultingNutritionKnowledgeValidationReason
                            .MISSING_NUTRITION_PAYLOAD,
                    message =
                        "Nutrition payload is missing."
                )

            return issues
        }

        val values =
            linkedMapOf<String, Double>()

        SUPPORTED_NUTRIENT_KEYS
            .forEach { nutrientKey ->
                val element =
                    nutritionPayload.get(
                        nutrientKey
                    )
                        ?: return@forEach

                if (
                    !element.isJsonPrimitive ||
                    !element.asJsonPrimitive.isNumber
                ) {
                    issues +=
                        error(
                            canonicalId =
                                canonicalId,
                            reason =
                                ResultingNutritionKnowledgeValidationReason
                                    .NON_NUMERIC_NUTRIENT_VALUE,
                            nutrientKey =
                                nutrientKey,
                            message =
                                "Nutrient value must be numeric."
                        )

                    return@forEach
                }

                val value =
                    runCatching {
                        element.asDouble
                    }
                        .getOrNull()

                if (
                    value == null ||
                    !value.isFinite()
                ) {
                    issues +=
                        error(
                            canonicalId =
                                canonicalId,
                            reason =
                                ResultingNutritionKnowledgeValidationReason
                                    .NON_FINITE_NUTRIENT_VALUE,
                            nutrientKey =
                                nutrientKey,
                            message =
                                "Nutrient value must be finite."
                        )

                    return@forEach
                }

                values[nutrientKey] =
                    value
            }

        if (values.isEmpty()) {
            issues +=
                error(
                    canonicalId =
                        canonicalId,
                    reason =
                        ResultingNutritionKnowledgeValidationReason
                            .NO_SUPPORTED_NUTRIENTS,
                    message =
                        "Nutrition entry contains no supported nutrients."
                )

            return issues
        }

        values.forEach {
                (nutrientKey, value) ->

            if (value < 0.0) {
                issues +=
                    error(
                        canonicalId =
                            canonicalId,
                        reason =
                            ResultingNutritionKnowledgeValidationReason
                                .NEGATIVE_NUTRIENT_VALUE,
                        nutrientKey =
                            nutrientKey,
                        actualValue =
                            value,
                        message =
                            "Nutrient value must not be negative."
                    )
            }

            if (
                nutrientKey ==
                ENERGY_KEY &&
                value >
                MAXIMUM_ENERGY_KCAL_PER_100G
            ) {
                issues +=
                    error(
                        canonicalId =
                            canonicalId,
                        reason =
                            ResultingNutritionKnowledgeValidationReason
                                .ENERGY_ABOVE_MAXIMUM,
                        nutrientKey =
                            nutrientKey,
                        actualValue =
                            value,
                        message =
                            "Energy exceeds maximum allowed value."
                    )
            }

            if (
                nutrientKey !=
                ENERGY_KEY &&
                value >
                MAXIMUM_COMPONENT_GRAMS_PER_100G
            ) {
                issues +=
                    error(
                        canonicalId =
                            canonicalId,
                        reason =
                            ResultingNutritionKnowledgeValidationReason
                                .COMPONENT_ABOVE_MAXIMUM,
                        nutrientKey =
                            nutrientKey,
                        actualValue =
                            value,
                        message =
                            "Nutrition component exceeds 100 g per 100 g."
                    )
            }
        }

        validateRelationships(
            canonicalId =
                canonicalId,
            entry =
                entry,
            values =
                values,
            issues =
                issues
        )

        return issues
    }

    private fun resolveNutritionPayload(
        entry: JsonObject
    ): JsonObject? =
        entry.takeIf(
            ::containsSupportedNutrient
        )

    private fun containsSupportedNutrient(
        objectValue: JsonObject
    ): Boolean =
        SUPPORTED_NUTRIENT_KEYS.any(
            objectValue::has
        )

    private fun isNutrientPresent(
        entry: JsonObject,
        nutrientKey: String
    ): Boolean {

        val presenceElement =
            entry.get(
                PRESENT_NUTRIENTS_PROPERTY_NAME
            )
                ?: return true

        if (!presenceElement.isJsonArray) {
            return false
        }

        return presenceElement
            .asJsonArray
            .any { element ->
                element.isJsonPrimitive &&
                        element.asJsonPrimitive.isString &&
                        element.asString ==
                        nutrientKey
            }
    }


    private fun validateRelationships(
        canonicalId: String,
        entry: JsonObject,
        values: Map<String, Double>,
        issues:
        MutableList<
                ResultingNutritionKnowledgeValidationIssue
                >
    ) {

        val fat =
            values[FAT_KEY]

        val saturatedFat =
            values[SATURATED_FAT_KEY]

        if (
            isNutrientPresent(
                entry =
                    entry,
                nutrientKey =
                    FAT_KEY
            ) &&
            isNutrientPresent(
                entry =
                    entry,
                nutrientKey =
                    SATURATED_FAT_KEY
            ) &&
            fat != null &&
            saturatedFat != null &&
            saturatedFat >
            fat +
            RELATIONSHIP_TOLERANCE_GRAMS
        ) {
            issues +=
                error(
                    canonicalId =
                        canonicalId,
                    reason =
                        ResultingNutritionKnowledgeValidationReason
                            .SATURATED_FAT_EXCEEDS_TOTAL_FAT,
                    nutrientKey =
                        SATURATED_FAT_KEY,
                    actualValue =
                        saturatedFat,
                    relatedValue =
                        fat,
                    message =
                        "Saturated fat exceeds total fat."
                )
        }

        val carbohydrates =
            values[CARBOHYDRATES_KEY]

        val sugars =
            values[SUGARS_KEY]

        if (
            isNutrientPresent(
                entry =
                    entry,
                nutrientKey =
                    CARBOHYDRATES_KEY
            ) &&
            isNutrientPresent(
                entry =
                    entry,
                nutrientKey =
                    SUGARS_KEY
            ) &&
            carbohydrates != null &&
            sugars != null &&
            sugars >
            carbohydrates +
            RELATIONSHIP_TOLERANCE_GRAMS
        ) {
            issues +=
                error(
                    canonicalId =
                        canonicalId,
                    reason =
                        ResultingNutritionKnowledgeValidationReason
                            .SUGARS_EXCEED_CARBOHYDRATES,
                    nutrientKey =
                        SUGARS_KEY,
                    actualValue =
                        sugars,
                    relatedValue =
                        carbohydrates,
                    message =
                        "Sugars exceed carbohydrates."
                )
        }

        val macronutrientDecision =
            macronutrientWarningPolicy.evaluate(
                fat =
                    values[FAT_KEY]
                        ?.takeIf {
                            isNutrientPresent(
                                entry =
                                    entry,
                                nutrientKey =
                                    FAT_KEY
                            )
                        },
                carbohydrates =
                    values[CARBOHYDRATES_KEY]
                        ?.takeIf {
                            isNutrientPresent(
                                entry =
                                    entry,
                                nutrientKey =
                                    CARBOHYDRATES_KEY
                            )
                        },
                protein =
                    values[PROTEINS_KEY]
                        ?.takeIf {
                            isNutrientPresent(
                                entry =
                                    entry,
                                nutrientKey =
                                    PROTEINS_KEY
                            )
                        }
            )

        if (macronutrientDecision.reportWarning) {
            issues +=
                warning(
                    canonicalId =
                        canonicalId,
                    reason =
                        ResultingNutritionKnowledgeValidationReason
                            .MACRONUTRIENT_SUM_EXCEEDS_MAXIMUM,
                    actualValue =
                        macronutrientDecision
                            .coreMacronutrientSumGrams,
                    message =
                        "Core macronutrient sum exceeds the approved " +
                                "policy maximum."
                )
        }
    }

    private fun error(
        canonicalId: String,
        reason: ResultingNutritionKnowledgeValidationReason,
        nutrientKey: String? = null,
        actualValue: Double? = null,
        relatedValue: Double? = null,
        message: String
    ): ResultingNutritionKnowledgeValidationIssue =
        ResultingNutritionKnowledgeValidationIssue(
            canonicalId =
                canonicalId,
            severity =
                ResultingNutritionKnowledgeValidationSeverity.ERROR,
            reason =
                reason,
            nutrientKey =
                nutrientKey,
            actualValue =
                actualValue,
            relatedValue =
                relatedValue,
            message =
                message
        )

    private fun warning(
        canonicalId: String,
        reason: ResultingNutritionKnowledgeValidationReason,
        actualValue: Double? = null,
        message: String
    ): ResultingNutritionKnowledgeValidationIssue =
        ResultingNutritionKnowledgeValidationIssue(
            canonicalId =
                canonicalId,
            severity =
                ResultingNutritionKnowledgeValidationSeverity.WARNING,
            reason =
                reason,
            actualValue =
                actualValue,
            message =
                message
        )

    private companion object {

        const val ENTRIES_PROPERTY_NAME =
            "entries"

        const val PRESENT_NUTRIENTS_PROPERTY_NAME =
            "presentNutrients"

        const val EMPTY_CANONICAL_ID_PLACEHOLDER =
            "<blank>"

        const val ENERGY_KEY =
            "calories"

        const val PROTEINS_KEY =
            "protein"

        const val FAT_KEY =
            "fat"

        const val SATURATED_FAT_KEY =
            "saturatedFat"

        const val CARBOHYDRATES_KEY =
            "carbohydrates"

        const val SUGARS_KEY =
            "sugar"

        const val FIBER_KEY =
            "fiber"

        const val SALT_KEY =
            "salt"

        const val MAXIMUM_ENERGY_KCAL_PER_100G =
            950.0

        const val MAXIMUM_COMPONENT_GRAMS_PER_100G =
            100.0

        const val RELATIONSHIP_TOLERANCE_GRAMS =
            0.5

        const val DEFAULT_MAXIMUM_REPORTED_WARNINGS =
            100

        const val DEFAULT_MAXIMUM_REPORTED_ERRORS =
            100

        val SUPPORTED_NUTRIENT_KEYS =
            listOf(
                ENERGY_KEY,
                FAT_KEY,
                SATURATED_FAT_KEY,
                CARBOHYDRATES_KEY,
                SUGARS_KEY,
                FIBER_KEY,
                PROTEINS_KEY,
                SALT_KEY
            )
    }
}