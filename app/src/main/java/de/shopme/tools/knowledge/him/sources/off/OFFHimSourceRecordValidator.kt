package de.shopme.tools.knowledge.him.sources.off

object OFFHimSourceRecordValidator {

    fun validate(
        record: OFFHimSourceRecord
    ): OFFHimSourceRecordValidationResult {

        val failures =
            mutableListOf<OFFHimSourceRecordValidationFailure>()

        validateSource(
            record = record,
            failures = failures
        )

        validateIdentity(
            record = record,
            failures = failures
        )

        validateIngredients(
            record = record,
            failures = failures
        )

        validateNutrition(
            record = record,
            failures = failures
        )

        validateClassification(
            record = record,
            failures = failures
        )

        validateEnvironmental(
            record = record,
            failures = failures
        )

        validatePackaging(
            record = record,
            failures = failures
        )

        validateQuality(
            record = record,
            failures = failures
        )

        return OFFHimSourceRecordValidationResult(
            valid = failures.isEmpty(),
            failures = failures.toList()
        )
    }

    private fun validateSource(
        record: OFFHimSourceRecord,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        val code =
            record.source.code

        if (code.isBlank()) {
            failures +=
                failure(
                    reason =
                        OFFHimSourceRecordValidationReason.INVALID_SOURCE_ID,
                    path = "source.code",
                    message = "Source code must not be blank."
                )

            return
        }

        if (code.length > 64) {
            failures +=
                failure(
                    reason =
                        OFFHimSourceRecordValidationReason.INVALID_SOURCE_ID,
                    path = "source.code",
                    message =
                        "Source code is unexpectedly long: ${code.length}"
                )
        }
    }

    private fun validateIdentity(
        record: OFFHimSourceRecord,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        val identity =
            record.identity

        validateOptionalText(
            value = identity.productName,
            path = "identity.productName",
            failures = failures
        )

        validateOptionalText(
            value = identity.productNameGerman,
            path = "identity.productNameGerman",
            failures = failures
        )

        validateOptionalText(
            value = identity.productNameEnglish,
            path = "identity.productNameEnglish",
            failures = failures
        )

        validateOptionalText(
            value = identity.genericName,
            path = "identity.genericName",
            failures = failures
        )

        validateStringList(
            values = identity.brands,
            path = "identity.brands",
            failures = failures
        )
    }

    private fun validateIngredients(
        record: OFFHimSourceRecord,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        val ingredients =
            record.ingredients
                ?: return

        ingredients.knownIngredientCount
            ?.let { count ->
                if (count < 0) {
                    failures +=
                        failure(
                            reason =
                                OFFHimSourceRecordValidationReason.INVALID_COUNT,
                            path =
                                "ingredients.knownIngredientCount",
                            message =
                                "Ingredient count must not be negative."
                        )
                }
            }

        ingredients.unknownIngredientCount
            ?.let { count ->
                if (count < 0) {
                    failures +=
                        failure(
                            reason =
                                OFFHimSourceRecordValidationReason.INVALID_COUNT,
                            path =
                                "ingredients.unknownIngredientCount",
                            message =
                                "Ingredient count must not be negative."
                        )
                }
            }

        validateIngredientTree(
            ingredients = ingredients.items,
            path = "ingredients.items",
            depth = 0,
            failures = failures
        )
    }

    private fun validateIngredientTree(
        ingredients: List<OFFHimIngredient>,
        path: String,
        depth: Int,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        if (depth > MAX_INGREDIENT_DEPTH) {
            failures +=
                failure(
                    reason =
                        OFFHimSourceRecordValidationReason.INGREDIENT_TREE_TOO_DEEP,
                    path = path,
                    message =
                        "Ingredient tree exceeds maximum depth " +
                                "$MAX_INGREDIENT_DEPTH."
                )

            return
        }

        ingredients.forEachIndexed { index, ingredient ->

            val ingredientPath =
                "$path[$index]"

            ingredient.percent
                ?.let {
                    validatePercent(
                        value = it,
                        path = "$ingredientPath.percent",
                        failures = failures
                    )
                }

            ingredient.percentMin
                ?.let {
                    validatePercent(
                        value = it,
                        path = "$ingredientPath.percentMin",
                        failures = failures
                    )
                }

            ingredient.percentMax
                ?.let {
                    validatePercent(
                        value = it,
                        path = "$ingredientPath.percentMax",
                        failures = failures
                    )
                }

            ingredient.percentEstimate
                ?.let {
                    validatePercent(
                        value = it,
                        path = "$ingredientPath.percentEstimate",
                        failures = failures
                    )
                }

            val min =
                ingredient.percentMin

            val max =
                ingredient.percentMax

            if (
                min != null &&
                max != null &&
                min > max
            ) {
                failures +=
                    failure(
                        reason =
                            OFFHimSourceRecordValidationReason.INVALID_INGREDIENT_RANGE,
                        path = ingredientPath,
                        message =
                            "percentMin must not exceed percentMax: " +
                                    "min=$min max=$max.",
                        observedValues =
                            listOf(
                                min,
                                max
                            )
                    )
            }

            validateIngredientTree(
                ingredients = ingredient.ingredients,
                path = "$ingredientPath.ingredients",
                depth = depth + 1,
                failures = failures
            )
        }
    }

    private fun validateNutrition(
        record: OFFHimSourceRecord,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        val nutrition =
            record.nutrition
                ?: return

        nutrition.declared
            .forEach { (name, nutrient) ->

                val path =
                    "nutrition.declared.$name"

                validateFiniteOptional(
                    value = nutrient.value,
                    path = "$path.value",
                    failures = failures
                )

                validateFiniteOptional(
                    value = nutrient.per100g,
                    path = "$path.per100g",
                    failures = failures
                )

                validateFiniteOptional(
                    value = nutrient.perServing,
                    path = "$path.perServing",
                    failures = failures
                )

                if (
                    nutrient.per100g != null &&
                    nutrient.per100g < 0.0
                ) {
                    failures +=
                        failure(
                            reason =
                                OFFHimSourceRecordValidationReason.NEGATIVE_NUTRIENT_VALUE,
                            path = "$path.per100g",
                            message =
                                "Nutrient values must not be negative."
                        )
                }

                if (
                    nutrient.perServing != null &&
                    nutrient.perServing < 0.0
                ) {
                    failures +=
                        failure(
                            reason =
                                OFFHimSourceRecordValidationReason.NEGATIVE_NUTRIENT_VALUE,
                            path = "$path.perServing",
                            message =
                                "Nutrient values must not be negative."
                        )
                }
            }

        nutrition.structured
            ?.let {
                validateStructuredNutrition(
                    nutrition = it,
                    failures = failures
                )
            }

        nutrition.estimated
            ?.fruitsVegetablesNutsPercentEstimate
            ?.let {
                validatePercent(
                    value = it,
                    path =
                        "nutrition.estimated." +
                                "fruitsVegetablesNutsPercentEstimate",
                    failures = failures
                )
            }
    }

    private fun validateStructuredNutrition(
        nutrition: OFFHimStructuredNutrition,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        nutrition.aggregated
            .forEach { (name, nutrient) ->
                validateStructuredNutrient(
                    nutrient = nutrient,
                    path =
                        "nutrition.structured.aggregated.$name",
                    failures = failures
                )
            }

        nutrition.inputSets
            .forEachIndexed { index, input ->

                input.nutrients
                    .forEach { (name, nutrient) ->
                        validateStructuredNutrient(
                            nutrient = nutrient,
                            path =
                                "nutrition.structured.inputSets" +
                                        "[$index].nutrients.$name",
                            failures = failures
                        )
                    }
            }
    }

    private fun validateStructuredNutrient(
        nutrient: OFFHimStructuredNutrient,
        path: String,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        validateFiniteOptional(
            value = nutrient.value,
            path = "$path.value",
            failures = failures
        )

        nutrient.sourceIndex
            ?.let { index ->
                if (index < 0) {
                    failures +=
                        failure(
                            reason =
                                OFFHimSourceRecordValidationReason.INVALID_SOURCE_INDEX,
                            path = "$path.sourceIndex",
                            message =
                                "Structured nutrition source index " +
                                        "must not be negative."
                        )
                }
            }
    }

    private fun validateClassification(
        record: OFFHimSourceRecord,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        record.classification
            ?.nova
            ?.group
            ?.let { nova ->

                if (nova !in 1..4) {
                    failures +=
                        failure(
                            reason =
                                OFFHimSourceRecordValidationReason.INVALID_NOVA_GROUP,
                            path =
                                "classification.nova.group",
                            message =
                                "NOVA group must be in 1..4, was $nova."
                        )
                }
            }
    }

    private fun validateEnvironmental(
        record: OFFHimSourceRecord,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        val environmental =
            record.environmentalEvidence
                ?: return

        environmental.score
            ?.score
            ?.let {
                validateFinite(
                    value = it,
                    path =
                        "environmentalEvidence.score.score",
                    failures = failures
                )
            }

        environmental.score
            ?.legacyEcoScoreScore
            ?.let {
                validateFinite(
                    value = it,
                    path =
                        "environmentalEvidence.score." +
                                "legacyEcoScoreScore",
                    failures = failures
                )
            }

        environmental.origin
            ?.let { origin ->

                origin.epiScore
                    ?.let {
                        validateFinite(
                            value = it,
                            path =
                                "environmentalEvidence.origin.epiScore",
                            failures = failures
                        )
                    }

                origin.epiValue
                    ?.let {
                        validateFinite(
                            value = it,
                            path =
                                "environmentalEvidence.origin.epiValue",
                            failures = failures
                        )
                    }

                origin.aggregatedOrigins
                    .forEachIndexed { index, share ->

                        if (share.origin.isBlank()) {
                            failures +=
                                failure(
                                    reason =
                                        OFFHimSourceRecordValidationReason.EMPTY_NORMALIZED_VALUE,
                                    path =
                                        "environmentalEvidence.origin." +
                                                "aggregatedOrigins[$index].origin",
                                    message =
                                        "Origin must not be blank."
                                )
                        }

                        share.percent
                            ?.let {
                                validatePercent(
                                    value = it,
                                    path =
                                        "environmentalEvidence.origin." +
                                                "aggregatedOrigins[$index].percent",
                                    failures = failures
                                )
                            }
                    }
            }

        environmental.forestFootprint
            ?.footprintPerKg
            ?.let { value ->

                validateFinite(
                    value = value,
                    path =
                        "environmentalEvidence." +
                                "forestFootprint.footprintPerKg",
                    failures = failures
                )

                if (value < 0.0) {
                    failures +=
                        failure(
                            reason =
                                OFFHimSourceRecordValidationReason.NEGATIVE_ENVIRONMENTAL_VALUE,
                            path =
                                "environmentalEvidence." +
                                        "forestFootprint.footprintPerKg",
                            message =
                                "Forest footprint must not be negative: $value.",
                            observedValues =
                                listOf(value)
                        )
                }
            }

        environmental
            .legacyCarbonKnownIngredientPercent
            ?.let {
                validatePercent(
                    value = it,
                    path =
                        "environmentalEvidence." +
                                "legacyCarbonKnownIngredientPercent",
                    failures = failures
                )
            }
    }

    private fun validatePackaging(
        record: OFFHimSourceRecord,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        val packaging =
            record.packagingEvidence
                ?: return

        packaging.items
            .forEachIndexed { index, item ->

                val path =
                    "packagingEvidence.items[$index]"

                item.numberOfUnits
                    ?.let { value ->

                        validateFinite(
                            value = value,
                            path = "$path.numberOfUnits",
                            failures = failures
                        )

                        if (value < 0.0) {
                            failures +=
                                failure(
                                    reason =
                                        OFFHimSourceRecordValidationReason.INVALID_COUNT,
                                    path = "$path.numberOfUnits",
                                    message =
                                        "Packaging unit count must not be negative."
                                )
                        }
                    }

                item.quantityPerUnit
                    ?.value
                    ?.let { value ->

                        validateFinite(
                            value = value,
                            path =
                                "$path.quantityPerUnit.value",
                            failures = failures
                        )

                        if (value < 0.0) {
                            failures +=
                                failure(
                                    reason =
                                        OFFHimSourceRecordValidationReason.NEGATIVE_PACKAGING_VALUE,
                                    path =
                                        "$path.quantityPerUnit.value",
                                    message =
                                        "Packaging quantity must not be negative."
                                )
                        }
                    }

                item.measuredWeight
                    ?.let {
                        validateNonNegativeFinite(
                            value = it,
                            path = "$path.measuredWeight",
                            failures = failures
                        )
                    }

                item.specifiedWeight
                    ?.let {
                        validateNonNegativeFinite(
                            value = it,
                            path = "$path.specifiedWeight",
                            failures = failures
                        )
                    }
            }
    }

    private fun validateQuality(
        record: OFFHimSourceRecord,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        record.quality.completeness
            ?.let { value ->

                validateFinite(
                    value = value,
                    path = "quality.completeness",
                    failures = failures
                )

                if (value !in 0.0..1.0) {
                    failures +=
                        failure(
                            reason =
                                OFFHimSourceRecordValidationReason.INVALID_COMPLETENESS,
                            path = "quality.completeness",
                            message =
                                "Completeness must be in 0.0..1.0, was $value.",
                            observedValues =
                                listOf(value)
                        )
                }
            }

        record.quality.scans
            ?.let {
                if (it < 0L) {
                    failures +=
                        failure(
                            reason =
                                OFFHimSourceRecordValidationReason.INVALID_COUNT,
                            path = "quality.scans",
                            message =
                                "Scan count must not be negative."
                        )
                }
            }

        record.quality.uniqueScans
            ?.let {
                if (it < 0L) {
                    failures +=
                        failure(
                            reason =
                                OFFHimSourceRecordValidationReason.INVALID_COUNT,
                            path = "quality.uniqueScans",
                            message =
                                "Unique scan count must not be negative."
                        )
                }
            }
    }

    private fun validatePercent(
        value: Double,
        path: String,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        validateFinite(
            value = value,
            path = path,
            failures = failures
        )

        if (value !in 0.0..100.0) {
            failures +=
                failure(
                    reason =
                        OFFHimSourceRecordValidationReason.INVALID_PERCENT,
                    path = path,
                    message =
                        "Percentage must be in 0..100, was $value.",
                    observedValues =
                        listOf(value)
                )
        }
    }

    private fun validateFiniteOptional(
        value: Double?,
        path: String,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        value?.let {
            validateFinite(
                value = it,
                path = path,
                failures = failures
            )
        }
    }

    private fun validateFinite(
        value: Double,
        path: String,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        if (!value.isFinite()) {
            failures +=
                failure(
                    reason =
                        OFFHimSourceRecordValidationReason.NON_FINITE_NUMBER,
                    path = path,
                    message =
                        "Numeric value must be finite."
                )
        }
    }

    private fun validateNonNegativeFinite(
        value: Double,
        path: String,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        validateFinite(
            value = value,
            path = path,
            failures = failures
        )

        if (value < 0.0) {
            failures +=
                failure(
                    reason =
                        OFFHimSourceRecordValidationReason.NEGATIVE_PACKAGING_VALUE,
                    path = path,
                    message =
                        "Value must not be negative."
                )
        }
    }

    private fun validateOptionalText(
        value: String?,
        path: String,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        if (
            value != null &&
            value.isBlank()
        ) {
            failures +=
                failure(
                    reason =
                        OFFHimSourceRecordValidationReason.EMPTY_NORMALIZED_VALUE,
                    path = path,
                    message =
                        "Normalized text must not be blank."
                )
        }
    }

    private fun validateStringList(
        values: List<String>,
        path: String,
        failures: MutableList<OFFHimSourceRecordValidationFailure>
    ) {

        values.forEachIndexed { index, value ->
            if (value.isBlank()) {
                failures +=
                    failure(
                        reason =
                            OFFHimSourceRecordValidationReason.EMPTY_NORMALIZED_VALUE,
                        path = "$path[$index]",
                        message =
                            "Normalized list value must not be blank."
                    )
            }
        }
    }

    private fun failure(
        reason: OFFHimSourceRecordValidationReason,
        path: String,
        message: String,
        observedValues: List<Double> = emptyList()
    ): OFFHimSourceRecordValidationFailure =
        OFFHimSourceRecordValidationFailure(
            reason = reason,
            path = path,
            message = message,
            observedValues = observedValues
        )

    private const val MAX_INGREDIENT_DEPTH =
        32
}

data class OFFHimSourceRecordValidationResult(
    val valid: Boolean,
    val failures: List<OFFHimSourceRecordValidationFailure>
)

data class OFFHimSourceRecordValidationFailure(
    val reason: OFFHimSourceRecordValidationReason,
    val path: String,
    val message: String,
    val observedValues: List<Double> = emptyList()
)

enum class OFFHimSourceRecordValidationReason {
    INVALID_SOURCE_ID,
    EMPTY_NORMALIZED_VALUE,
    INVALID_COUNT,
    INVALID_PERCENT,
    INVALID_INGREDIENT_RANGE,
    INGREDIENT_TREE_TOO_DEEP,
    NON_FINITE_NUMBER,
    NEGATIVE_NUTRIENT_VALUE,
    INVALID_SOURCE_INDEX,
    INVALID_NOVA_GROUP,
    NEGATIVE_ENVIRONMENTAL_VALUE,
    NEGATIVE_PACKAGING_VALUE,
    INVALID_COMPLETENESS
}