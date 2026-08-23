package de.shopme.tools.knowledge.him.sources.off

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.Locale

/**
 * Deterministic projection policy for Open Food Facts raw products.
 *
 * Responsibility:
 *
 * Raw OFF JsonObject
 *      ↓
 * deterministic extraction / normalization
 *      ↓
 * OFFHimSourceRecord
 *
 * Explicitly out of scope:
 *
 * - canonical ShopMe identity resolution
 * - catalog matching
 * - knowledge merging
 * - LLM enrichment
 * - embedded Agribalyse projection
 * - historical OFF calculation structures
 * - OFF operational metadata
 * - export / persistence
 */
object OFFHimProjectionPolicy {

    fun project(
        raw: JsonObject
    ): OFFHimSourceRecord? {

        val code =
            raw.string("code")
                ?.normalizedText()
                ?: raw.string("_id")
                    ?.normalizedText()
                ?: return null

        return OFFHimSourceRecord(
            source =
                OFFHimSourceIdentity(
                    code = code
                ),

            identity =
                projectIdentity(raw),

            taxonomy =
                projectTaxonomy(raw),

            ingredients =
                projectIngredients(raw),

            nutrition =
                projectNutrition(raw),

            classification =
                projectClassification(raw),

            allergens =
                projectAllergens(raw),

            geography =
                projectGeography(raw),

            environmentalEvidence =
                projectEnvironmental(raw),

            packagingEvidence =
                projectPackaging(raw),

            quality =
                projectQuality(raw),

            provenance =
                projectProvenance(raw)
        )
    }

    private const val PERCENT_EPSILON =
        1e-9

    private fun sanitizePercent(
        value: Double?
    ): Double? {

        value ?: return null

        if (!value.isFinite()) {
            return null
        }

        val normalized =
            when {
                value in -PERCENT_EPSILON..0.0 ->
                    0.0

                value in 100.0..(100.0 + PERCENT_EPSILON) ->
                    100.0

                else ->
                    value
            }

        return normalized
            .takeIf {
                it in 0.0..100.0
            }
    }

    private fun sanitizeNonNegative(
        value: Double?
    ): Double? =
        value
            ?.takeIf {
                it.isFinite() &&
                        it >= 0.0
            }

    private fun sanitizeCompleteness(
        value: Double?
    ): Double? =
        value
            ?.takeIf {
                it.isFinite() &&
                        it in 0.0..1.0
            }

    // ---------------------------------------------------------------------
    // Identity
    // ---------------------------------------------------------------------

    private fun projectIdentity(
        raw: JsonObject
    ): OFFHimProductIdentity =
        OFFHimProductIdentity(
            productName =
                raw.string("product_name")
                    ?.normalizedText(),

            productNameGerman =
                raw.string("product_name_de")
                    ?.normalizedText(),

            productNameEnglish =
                raw.string("product_name_en")
                    ?.normalizedText(),

            genericName =
                raw.string("generic_name")
                    ?.normalizedText(),

            genericNameGerman =
                raw.string("generic_name_de")
                    ?.normalizedText(),

            genericNameEnglish =
                raw.string("generic_name_en")
                    ?.normalizedText(),

            brands =
                raw.stringOrArrayValues(
                    textField = "brands",
                    arrayField = "brands_tags"
                ),

            productType =
                raw.string("product_type")
                    ?.normalizedToken(),

            quantity =
                raw.string("quantity")
                    ?.normalizedText(),

            servingSize =
                raw.string("serving_size")
                    ?.normalizedText()
        )

    // ---------------------------------------------------------------------
    // Taxonomy
    // ---------------------------------------------------------------------

    private fun projectTaxonomy(
        raw: JsonObject
    ): OFFHimTaxonomyEvidence? {

        val categories =
            raw.stringArray("categories_tags")

        val hierarchy =
            raw.stringArray("categories_hierarchy")

        val foodGroups =
            raw.stringArray("food_groups_tags")

        val pnnsGroups =
            buildList {
                raw.string("pnns_groups_1")
                    ?.normalizedToken()
                    ?.let(::add)

                raw.string("pnns_groups_2")
                    ?.normalizedToken()
                    ?.let(::add)
            }
                .distinct()

        val mainCategory =
            raw.string("main_category")
                ?.normalizedToken()

        val ciqualReferences =
            raw.stringArray(
                "ciqual_food_name_tags"
            )

        if (
            categories.isEmpty() &&
            hierarchy.isEmpty() &&
            foodGroups.isEmpty() &&
            pnnsGroups.isEmpty() &&
            mainCategory == null &&
            ciqualReferences.isEmpty()
        ) {
            return null
        }

        return OFFHimTaxonomyEvidence(
            categories = categories,
            categoryHierarchy = hierarchy,
            foodGroups = foodGroups,
            pnnsGroups = pnnsGroups,
            mainCategory = mainCategory,
            ciqualReferences = ciqualReferences
        )
    }

    // ---------------------------------------------------------------------
    // Ingredients
    // ---------------------------------------------------------------------

    private fun projectIngredients(
        raw: JsonObject
    ): OFFHimIngredientsEvidence? {

        val text =
            raw.string("ingredients_text")
                ?.normalizedText()

        val tags =
            raw.stringArray("ingredients_tags")

        val hierarchy =
            raw.stringArray(
                "ingredients_hierarchy"
            )

        val items =
            raw.array("ingredients")
                ?.mapObjects {
                    projectIngredient(it)
                }
                .orEmpty()

        val knownCount =
            raw.int("known_ingredients_n")

        val unknownCount =
            raw.int("unknown_ingredients_n")

        if (
            text == null &&
            tags.isEmpty() &&
            hierarchy.isEmpty() &&
            items.isEmpty() &&
            knownCount == null &&
            unknownCount == null
        ) {
            return null
        }

        return OFFHimIngredientsEvidence(
            text = text,
            tags = tags,
            hierarchy = hierarchy,
            items = items,
            knownIngredientCount = knownCount,
            unknownIngredientCount = unknownCount
        )
    }

    private fun projectIngredient(
        raw: JsonObject
    ): OFFHimIngredient {

        val percent =
            sanitizePercent(
                raw.double("percent")
            )

        val rawPercentMin =
            sanitizePercent(
                raw.double("percent_min")
            )

        val rawPercentMax =
            sanitizePercent(
                raw.double("percent_max")
            )

        val validRange =
            rawPercentMin == null ||
                    rawPercentMax == null ||
                    rawPercentMin <= rawPercentMax

        val percentMin =
            if (validRange) {
                rawPercentMin
            } else {
                null
            }

        val percentMax =
            if (validRange) {
                rawPercentMax
            } else {
                null
            }

        val percentEstimate =
            sanitizePercent(
                raw.double(
                    "percent_estimate"
                )
            )

        return OFFHimIngredient(
            id =
                raw.string("id")
                    ?.normalizedToken(),

            text =
                raw.string("text")
                    ?.normalizedText(),

            percent =
                percent,

            percentMin =
                percentMin,

            percentMax =
                percentMax,

            percentEstimate =
                percentEstimate,

            vegetarian =
                raw.string("vegetarian")
                    ?.normalizedToken(),

            vegan =
                raw.string("vegan")
                    ?.normalizedToken(),

            ciqualFoodCode =
                raw.string(
                    "ciqual_food_code"
                )?.normalizedToken(),

            ecobalyseCode =
                raw.string(
                    "ecobalyse_code"
                )?.normalizedToken(),

            ingredients =
                raw.array("ingredients")
                    ?.mapObjects {
                        projectIngredient(it)
                    }
                    .orEmpty()
        )
    }

    // ---------------------------------------------------------------------
    // Nutrition
    // ---------------------------------------------------------------------

    private fun projectNutrition(
        raw: JsonObject
    ): OFFHimNutritionEvidence? {

        val declared =
            projectDeclaredNutrients(
                raw.objectValue(
                    "nutriments"
                )
            )

        val structured =
            projectStructuredNutrition(
                raw.objectValue(
                    "nutrition"
                )
            )

        val nutrientLevels =
            projectStringMap(
                raw.objectValue(
                    "nutrient_levels"
                )
            )

        val vitamins =
            raw.stringArray(
                "vitamins_tags"
            )

        val minerals =
            raw.stringArray(
                "minerals_tags"
            )

        val aminoAcids =
            raw.stringArray(
                "amino_acids_tags"
            )

        val nucleotides =
            raw.stringArray(
                "nucleotides_tags"
            )

        val otherSubstances =
            raw.stringArray(
                "other_nutritional_substances_tags"
            )

        val estimate =
            projectEstimatedNutrition(raw)

        if (
            declared.isEmpty() &&
            structured == null &&
            nutrientLevels.isEmpty() &&
            vitamins.isEmpty() &&
            minerals.isEmpty() &&
            aminoAcids.isEmpty() &&
            nucleotides.isEmpty() &&
            otherSubstances.isEmpty() &&
            estimate == null
        ) {
            return null
        }

        return OFFHimNutritionEvidence(
            declared = declared,
            structured = structured,
            nutrientLevels = nutrientLevels,
            vitamins = vitamins,
            minerals = minerals,
            aminoAcids = aminoAcids,
            nucleotides = nucleotides,
            otherNutritionalSubstances =
                otherSubstances,
            estimated = estimate
        )
    }

    private fun projectDeclaredNutrients(
        nutriments: JsonObject?
    ): Map<String, OFFHimNutrientValue> {

        if (nutriments == null) {
            return emptyMap()
        }

        val nutrientNames =
            nutriments.entrySet()
                .map { it.key }
                .mapNotNull {
                    nutrientBaseName(it)
                }
                .toSortedSet()

        return nutrientNames
            .mapNotNull { nutrient ->

                val value =
                    sanitizeNonNegative(
                        nutriments.double(
                            nutrient
                        )
                    )

                val per100g =
                    sanitizeNonNegative(
                        nutriments.double(
                            "${nutrient}_100g"
                        )
                    )

                val perServing =
                    sanitizeNonNegative(
                        nutriments.double(
                            "${nutrient}_serving"
                        )
                    )

                val unit =
                    nutriments.string(
                        "${nutrient}_unit"
                    )?.normalizedUnit()

                if (
                    value == null &&
                    per100g == null &&
                    perServing == null
                ) {
                    return@mapNotNull null
                }

                nutrient to
                        OFFHimNutrientValue(
                            value = value,
                            unit = unit,
                            per100g = per100g,
                            perServing = perServing
                        )
            }
            .toMap()
    }

    private fun nutrientBaseName(
        field: String
    ): String? {

        val suffixes =
            listOf(
                "_100g",
                "_serving",
                "_unit",
                "_value",
                "_prepared_100g",
                "_prepared_serving",
                "_prepared_unit",
                "_prepared_value"
            )

        val base =
            suffixes
                .firstOrNull {
                    field.endsWith(it)
                }
                ?.let {
                    field.removeSuffix(it)
                }
                ?: field

        val normalized =
            base
                .trim()
                .lowercase(Locale.ROOT)

        if (normalized.isBlank()) {
            return null
        }

        if (
            normalized.endsWith("_label") ||
            normalized.endsWith("_modifier")
        ) {
            return null
        }

        if (
            normalized.startsWith("nutrition-score") ||
            normalized.startsWith("nutriscore") ||
            normalized.startsWith("nova-") ||
            normalized.startsWith("nova_") ||
            normalized.startsWith("ecoscore") ||
            normalized.startsWith("environmental-score")
        ) {
            return null
        }

        return normalized
    }

    private fun projectStructuredNutrition(
        nutrition: JsonObject?
    ): OFFHimStructuredNutrition? {

        if (nutrition == null) {
            return null
        }

        val inputSets =
            nutrition.array("input_sets")
                ?.mapObjects { input ->
                    OFFHimNutritionInputSet(
                        source =
                            input.string(
                                "source"
                            )?.normalizedText(),

                        sourceDescription =
                            input.string(
                                "source_description"
                            )?.normalizedText(),

                        per =
                            input.string("per")
                                ?.normalizedToken(),

                        preparation =
                            input.string(
                                "preparation"
                            )?.normalizedToken(),

                        nutrients =
                            projectStructuredNutrients(
                                input.objectValue(
                                    "nutrients"
                                )
                            )
                    )
                }
                .orEmpty()

        val aggregated =
            nutrition
                .objectValue("aggregated_set")
                ?.objectValue("nutrients")
                ?.let(
                    ::projectStructuredNutrients
                )
                .orEmpty()

        if (
            inputSets.isEmpty() &&
            aggregated.isEmpty()
        ) {
            return null
        }

        return OFFHimStructuredNutrition(
            inputSets = inputSets,
            aggregated = aggregated
        )
    }

    private fun projectStructuredNutrients(
        raw: JsonObject?
    ): Map<String, OFFHimStructuredNutrient> {

        if (raw == null) {
            return emptyMap()
        }

        return raw.entrySet()
            .mapNotNull { (key, value) ->

                if (!value.isJsonObject) {
                    return@mapNotNull null
                }

                val nutrient =
                    value.asJsonObject

                key.normalizedToken() to
                        OFFHimStructuredNutrient(
                            value =
                                sanitizeNonNegative(
                                    nutrient.double(
                                        "value"
                                    )
                                ),

                            unit =
                                nutrient.string(
                                    "unit"
                                )?.normalizedUnit(),

                            source =
                                nutrient.string(
                                    "source"
                                )?.normalizedText(),

                            sourceIndex =
                                nutrient.int(
                                    "source_index"
                                ),

                            sourcePer =
                                nutrient.string(
                                    "source_per"
                                )?.normalizedToken()
                        )
            }
            .toMap()
    }

    private fun projectEstimatedNutrition(
        raw: JsonObject
    ): OFFHimEstimatedNutritionEvidence? {

        val fruitEstimate =
            sanitizePercent(
                raw.double(
                    "fruits-vegetables-nuts_100g_estimate"
                )
            )

        val recipeEstimatorAvailable =
            raw.objectValue(
                "recipe_estimator"
            ) != null

        if (
            fruitEstimate == null &&
            !recipeEstimatorAvailable
        ) {
            return null
        }

        return OFFHimEstimatedNutritionEvidence(
            fruitsVegetablesNutsPercentEstimate =
                fruitEstimate,
            recipeEstimatorAvailable =
                recipeEstimatorAvailable
        )
    }

    // ---------------------------------------------------------------------
    // Classification
    // ---------------------------------------------------------------------

    private fun projectClassification(
        raw: JsonObject
    ): OFFHimClassificationEvidence? {

        val nutriScore =
            projectNutriScore(raw)

        val nova =
            projectNova(raw)

        val additives =
            raw.stringArray(
                "additives_tags"
            )

        val labels =
            raw.stringArray(
                "labels_tags"
            )

        if (
            nutriScore == null &&
            nova == null &&
            additives.isEmpty() &&
            labels.isEmpty()
        ) {
            return null
        }

        return OFFHimClassificationEvidence(
            nutriScore = nutriScore,
            nova = nova,
            additives = additives,
            labels = labels
        )
    }

    private fun projectNutriScore(
        raw: JsonObject
    ): OFFHimNutriScoreEvidence? {

        val grade =
            raw.string("nutrition_grade_fr")
                ?.normalizedToken()
                ?: raw.string(
                    "nutrition_grades"
                )?.normalizedToken()

        val score =
            raw.int(
                "nutriscore_score"
            )

        val version =
            raw.string(
                "nutriscore_version"
            )?.normalizedToken()

        if (
            grade == null &&
            score == null &&
            version == null
        ) {
            return null
        }

        return OFFHimNutriScoreEvidence(
            grade = grade,
            score = score,
            version = version
        )
    }

    private fun projectNova(
        raw: JsonObject
    ): OFFHimNovaEvidence? {

        val group =
            raw.int("nova_group")

        val tags =
            raw.stringArray(
                "nova_groups_tags"
            )

        val markers =
            raw.objectValue(
                "nova_groups_markers"
            )
                ?.flattenPrimitiveStrings()
                .orEmpty()

        if (
            group == null &&
            tags.isEmpty() &&
            markers.isEmpty()
        ) {
            return null
        }

        return OFFHimNovaEvidence(
            group = group,
            tags = tags,
            markers = markers
        )
    }

    // ---------------------------------------------------------------------
    // Allergens
    // ---------------------------------------------------------------------

    private fun projectAllergens(
        raw: JsonObject
    ): OFFHimAllergenEvidence? {

        val allergens =
            raw.stringArray(
                "allergens_tags"
            )

        val traces =
            raw.stringArray(
                "traces_tags"
            )

        val fromIngredients =
            raw.stringArray(
                "allergens_from_ingredients"
            )

        val fieldSources =
            projectFieldSources(
                raw.objectValue(
                    "tags_sources"
                )
            )

        if (
            allergens.isEmpty() &&
            traces.isEmpty() &&
            fromIngredients.isEmpty() &&
            fieldSources.isEmpty()
        ) {
            return null
        }

        return OFFHimAllergenEvidence(
            allergens = allergens,
            traces = traces,
            allergensFromIngredients =
                fromIngredients,
            fieldSources = fieldSources
        )
    }

    // ---------------------------------------------------------------------
    // Geography
    // ---------------------------------------------------------------------

    private fun projectGeography(
        raw: JsonObject
    ): OFFHimGeographyEvidence? {

        val origins =
            raw.stringArray(
                "origins_tags"
            )

        val manufacturing =
            raw.stringArray(
                "manufacturing_places_tags"
            )

        val countries =
            raw.stringArray(
                "countries_tags"
            )

        val purchasePlaces =
            raw.stringOrArrayValues(
                textField = "purchase_places",
                arrayField = "purchase_places_tags"
            )

        val regulatoryCodes =
            raw.stringArray(
                "emb_codes_tags"
            )

        if (
            origins.isEmpty() &&
            manufacturing.isEmpty() &&
            countries.isEmpty() &&
            purchasePlaces.isEmpty() &&
            regulatoryCodes.isEmpty()
        ) {
            return null
        }

        return OFFHimGeographyEvidence(
            origins = origins,
            manufacturingPlaces =
                manufacturing,
            countries = countries,
            purchasePlaces = purchasePlaces,
            regulatoryCodes =
                regulatoryCodes
        )
    }

    // ---------------------------------------------------------------------
    // Environmental evidence
    // ---------------------------------------------------------------------

    private fun projectEnvironmental(
        raw: JsonObject
    ): OFFHimEnvironmentalEvidence? {

        val current =
            raw.objectValue(
                "environmental_score_data"
            )

        val legacy =
            raw.objectValue(
                "ecoscore_data"
            )

        val score =
            projectEnvironmentalScore(
                raw = raw,
                current = current,
                legacy = legacy
            )

        val origin =
            projectEnvironmentalOrigin(
                current
                    ?.objectValue("adjustments")
                    ?.objectValue(
                        "origins_of_ingredients"
                    )
            )

        val packaging =
            projectEnvironmentalPackaging(
                current
                    ?.objectValue("adjustments")
                    ?.objectValue("packaging")
            )

        val production =
            projectProductionSystem(
                current
                    ?.objectValue("adjustments")
                    ?.objectValue(
                        "production_system"
                    )
            )

        val threatened =
            projectThreatenedSpecies(
                current
                    ?.objectValue("adjustments")
                    ?.objectValue(
                        "threatened_species"
                    )
            )

        val forest =
            projectForestFootprint(
                raw.objectValue(
                    "forest_footprint_data"
                )
            )

        val diagnostics =
            projectEnvironmentalDiagnostics(
                current
            )

        val legacyCarbon =
            sanitizePercent(
                raw.double(
                    "carbon_footprint_percent_of_known_ingredients"
                )
            )

        if (
            score == null &&
            origin == null &&
            packaging == null &&
            production == null &&
            threatened == null &&
            forest == null &&
            diagnostics == null &&
            legacyCarbon == null
        ) {
            return null
        }

        return OFFHimEnvironmentalEvidence(
            score = score,
            origin = origin,
            packaging = packaging,
            productionSystem = production,
            threatenedSpecies = threatened,
            forestFootprint = forest,
            diagnostics = diagnostics,
            legacyCarbonKnownIngredientPercent =
                legacyCarbon
        )
    }

    private fun projectEnvironmentalScore(
        raw: JsonObject,
        current: JsonObject?,
        legacy: JsonObject?
    ): OFFHimEnvironmentalScoreEvidence? {

        val grade =
            raw.string(
                "environmental_score_grade"
            )?.normalizedToken()
                ?: current
                    ?.string("grade")
                    ?.normalizedToken()

        val score =
            raw.double(
                "environmental_score_score"
            )
                ?: current
                    ?.double("score")

        val tags =
            raw.stringArray(
                "environmental_score_tags"
            )

        val legacyGrade =
            raw.string("ecoscore_grade")
                ?.normalizedToken()
                ?: legacy
                    ?.string("grade")
                    ?.normalizedToken()

        val legacyScore =
            raw.double("ecoscore_score")
                ?: legacy
                    ?.double("score")

        if (
            grade == null &&
            score == null &&
            tags.isEmpty() &&
            legacyGrade == null &&
            legacyScore == null
        ) {
            return null
        }

        return OFFHimEnvironmentalScoreEvidence(
            grade = grade,
            score = score,
            tags = tags,
            legacyEcoScoreGrade =
                legacyGrade,
            legacyEcoScoreScore =
                legacyScore
        )
    }

    private fun projectEnvironmentalOrigin(
        raw: JsonObject?
    ): OFFHimEnvironmentalOriginEvidence? {

        if (raw == null) {
            return null
        }

        val aggregatedOrigins =
            raw.array(
                "aggregated_origins"
            )
                ?.mapObjects { origin ->
                    OFFHimOriginShare(
                        origin =
                            origin.string(
                                "origin"
                            )
                                ?.normalizedToken()
                                .orEmpty(),

                        percent =
                            sanitizePercent(
                                origin.double(
                                    "percent"
                                )
                            )
                    )
                }
                ?.filter {
                    it.origin.isNotBlank()
                }
                .orEmpty()

        val categories =
            raw.stringArray(
                "origins_from_categories"
            )

        val sourceOrigins =
            raw.stringArray(
                "origins_from_origins_field"
            )

        val epiScore =
            raw.double(
                "epi_score"
            )

        val epiValue =
            raw.double(
                "epi_value"
            )

        val warning =
            raw.string("warning")
                ?.normalizedText()

        if (
            aggregatedOrigins.isEmpty() &&
            categories.isEmpty() &&
            sourceOrigins.isEmpty() &&
            epiScore == null &&
            epiValue == null &&
            warning == null
        ) {
            return null
        }

        return OFFHimEnvironmentalOriginEvidence(
            aggregatedOrigins =
                aggregatedOrigins,
            originsFromCategories =
                categories,
            originsFromSourceField =
                sourceOrigins,
            epiScore = epiScore,
            epiValue = epiValue,
            warning = warning
        )
    }

    private fun projectEnvironmentalPackaging(
        raw: JsonObject?
    ): OFFHimEnvironmentalPackagingEvidence? {

        if (raw == null) {
            return null
        }

        val value =
            raw.double("value")

        val score =
            raw.double("score")

        val nonRecyclable =
            raw.boolean(
                "non_recyclable_and_non_biodegradable_materials"
            )

        val warning =
            raw.string("warning")
                ?.normalizedText()

        if (
            value == null &&
            score == null &&
            nonRecyclable == null &&
            warning == null
        ) {
            return null
        }

        return OFFHimEnvironmentalPackagingEvidence(
            value = value,
            score = score,
            nonRecyclableAndNonBiodegradableMaterials =
                nonRecyclable,
            warning = warning
        )
    }

    private fun projectProductionSystem(
        raw: JsonObject?
    ): OFFHimProductionSystemEvidence? {

        if (raw == null) {
            return null
        }

        val labels =
            raw.stringArray("labels")

        val value =
            raw.double("value")

        val warning =
            raw.string("warning")
                ?.normalizedText()

        if (
            labels.isEmpty() &&
            value == null &&
            warning == null
        ) {
            return null
        }

        return OFFHimProductionSystemEvidence(
            labels = labels,
            value = value,
            warning = warning
        )
    }

    private fun projectThreatenedSpecies(
        raw: JsonObject?
    ): OFFHimThreatenedSpeciesEvidence? {

        if (raw == null) {
            return null
        }

        val ingredient =
            raw.string("ingredient")
                ?.normalizedToken()

        val value =
            raw.double("value")

        val warning =
            raw.string("warning")
                ?.normalizedText()

        if (
            ingredient == null &&
            value == null &&
            warning == null
        ) {
            return null
        }

        return OFFHimThreatenedSpeciesEvidence(
            ingredient = ingredient,
            value = value,
            warning = warning
        )
    }

    private fun projectForestFootprint(
        raw: JsonObject?
    ): OFFHimForestFootprintEvidence? {

        if (raw == null) {
            return null
        }

        val footprint =
            sanitizeNonNegative(
                raw.double(
                    "footprint_per_kg"
                )
            )

        val grade =
            raw.string("grade")
                ?.normalizedToken()

        if (
            footprint == null &&
            grade == null
        ) {
            return null
        }

        return OFFHimForestFootprintEvidence(
            footprintPerKg = footprint,
            grade = grade
        )
    }

    private fun projectEnvironmentalDiagnostics(
        raw: JsonObject?
    ): OFFHimEnvironmentalDiagnostics? {

        if (raw == null) {
            return null
        }

        val missing =
            raw.objectValue("missing")

        val status =
            raw.string("status")
                ?.normalizedToken()

        val missingKeyData =
            raw.booleanLike(
                "missing_key_data"
            )

        val missingAgribalyse =
            raw.booleanLike(
                "missing_agribalyse_match_warning"
            )

        val warning =
            raw.string(
                "missing_data_warning"
            )?.normalizedText()

        val result =
            OFFHimEnvironmentalDiagnostics(
                status = status,
                missingKeyData =
                    missingKeyData,
                missingAgribalyseMatch =
                    missingAgribalyse,
                missingDataWarning =
                    warning,
                missingCategories =
                    missing
                        ?.presenceBoolean(
                            "categories"
                        ),
                missingIngredients =
                    missing
                        ?.presenceBoolean(
                            "ingredients"
                        ),
                missingLabels =
                    missing
                        ?.presenceBoolean(
                            "labels"
                        ),
                missingOrigins =
                    missing
                        ?.presenceBoolean(
                            "origins"
                        ),
                missingPackaging =
                    missing
                        ?.presenceBoolean(
                            "packagings"
                        ),
                missingAgribalyseCategory =
                    missing
                        ?.presenceBoolean(
                            "agb_category"
                        ),
                notApplicableForCategory =
                    raw.booleanLike(
                        "environmental_score_not_applicable_for_category"
                    )
                        ?: raw.booleanLike(
                            "ecoscore_not_applicable_for_category"
                        )
            )

        return if (
            result.status == null &&
            result.missingKeyData == null &&
            result.missingAgribalyseMatch == null &&
            result.missingDataWarning == null &&
            result.missingCategories == null &&
            result.missingIngredients == null &&
            result.missingLabels == null &&
            result.missingOrigins == null &&
            result.missingPackaging == null &&
            result.missingAgribalyseCategory == null &&
            result.notApplicableForCategory == null
        ) {
            null
        } else {
            result
        }
    }

    // ---------------------------------------------------------------------
    // Packaging
    // ---------------------------------------------------------------------

    private fun projectPackaging(
        raw: JsonObject
    ): OFFHimPackagingEvidence? {

        val taxonomy =
            raw.stringArray(
                "packaging_tags"
            )

        val hierarchy =
            raw.stringArray(
                "packaging_hierarchy"
            )

        val materials =
            raw.stringArray(
                "packaging_materials_tags"
            )

        val shapes =
            raw.stringArray(
                "packaging_shapes_tags"
            )

        val recycling =
            raw.stringArray(
                "packaging_recycling_tags"
            )

        val items =
            raw.array("packagings")
                ?.mapObjects {
                    projectPackagingItem(it)
                }
                .orEmpty()

        val complete =
            raw.booleanLike(
                "packagings_complete"
            )

        if (
            taxonomy.isEmpty() &&
            hierarchy.isEmpty() &&
            materials.isEmpty() &&
            shapes.isEmpty() &&
            recycling.isEmpty() &&
            items.isEmpty() &&
            complete == null
        ) {
            return null
        }

        return OFFHimPackagingEvidence(
            taxonomy = taxonomy,
            hierarchy = hierarchy,
            materials = materials,
            shapes = shapes,
            recycling = recycling,
            items = items,
            complete = complete
        )
    }

    private fun projectPackagingItem(
        raw: JsonObject
    ): OFFHimPackagingItem {

        val quantityRaw =
            raw.string(
                "quantity_per_unit"
            )?.normalizedText()

        val quantityValue =
            sanitizeNonNegative(
                raw.double(
                    "quantity_per_unit_value"
                )
            )

        val quantityUnit =
            raw.string(
                "quantity_per_unit_unit"
            )?.normalizedUnit()

        val quantity =
            if (
                quantityRaw == null &&
                quantityValue == null &&
                quantityUnit == null
            ) {
                null
            } else {
                OFFHimQuantity(
                    value = quantityValue,
                    unit = quantityUnit,
                    raw = quantityRaw
                )
            }

        return OFFHimPackagingItem(
            material =
                raw.string("material")
                    ?.normalizedToken(),

            shape =
                raw.string("shape")
                    ?.normalizedToken(),

            numberOfUnits =
                sanitizeNonNegative(
                    raw.double(
                        "number_of_units"
                    )
                ),

            quantityPerUnit =
                quantity,

            measuredWeight =
                sanitizeNonNegative(
                    raw.double(
                        "weight_measured"
                    )
                ),

            specifiedWeight =
                sanitizeNonNegative(
                    raw.double(
                        "weight_specified"
                    )
                ),

            recycling =
                raw.string("recycling")
                    ?.normalizedToken(),

            foodContact =
                raw.booleanLike(
                    "food_contact"
                )
        )
    }

    // ---------------------------------------------------------------------
    // Quality
    // ---------------------------------------------------------------------

    private fun projectQuality(
        raw: JsonObject
    ): OFFHimQualityEvidence =
        OFFHimQualityEvidence(
            completeness =
                sanitizeCompleteness(
                    raw.double(
                        "completeness"
                    )
                ),

            complete =
                raw.booleanLike("complete"),

            obsolete =
                raw.booleanLike("obsolete"),

            warnings =
                raw.stringArray(
                    "data_quality_warnings_tags"
                ),

            errors =
                raw.stringArray(
                    "data_quality_errors_tags"
                ),

            info =
                raw.stringArray(
                    "data_quality_info_tags"
                ),

            unknownNutrients =
                raw.stringArray(
                    "unknown_nutrients_tags"
                ),

            scans =
                raw.long("scans_n"),

            uniqueScans =
                raw.long(
                    "unique_scans_n"
                )
        )

    // ---------------------------------------------------------------------
    // Provenance
    // ---------------------------------------------------------------------

    private fun projectProvenance(
        raw: JsonObject
    ): OFFHimProvenance =
        OFFHimProvenance(
            language =
                raw.string("lang")
                    ?.normalizedToken()
                    ?: raw.string("lc")
                        ?.normalizedToken(),

            languages =
                raw.stringArray(
                    "languages_tags"
                ),

            sources =
                raw.array("sources")
                    ?.mapObjects {
                        projectSourceReference(it)
                    }
                    .orEmpty(),

            fieldSources =
                projectFieldSources(
                    raw.objectValue(
                        "tags_sources"
                    )
                ),

            createdAtEpochSeconds =
                raw.long("created_t"),

            lastModifiedAtEpochSeconds =
                raw.long(
                    "last_modified_t"
                ),

            lastUpdatedAtEpochSeconds =
                raw.long(
                    "last_updated_t"
                )
        )

    private fun projectSourceReference(
        raw: JsonObject
    ): OFFHimSourceReference =
        OFFHimSourceReference(
            id =
                raw.string("id")
                    ?.normalizedToken(),

            url =
                raw.string("url")
                    ?.normalizedText(),

            fields =
                raw.stringArray("fields"),

            importedAtEpochSeconds =
                raw.long("import_t")
        )

    private fun projectFieldSources(
        raw: JsonObject?
    ): Map<String, List<String>> {

        if (raw == null) {
            return emptyMap()
        }

        return raw.entrySet()
            .associate { (key, value) ->
                key.normalizedToken() to
                        value.flattenPrimitiveStrings()
            }
            .filterValues {
                it.isNotEmpty()
            }
    }

    // ---------------------------------------------------------------------
    // Generic deterministic normalization helpers
    // ---------------------------------------------------------------------

    private fun projectStringMap(
        raw: JsonObject?
    ): Map<String, String> {

        if (raw == null) {
            return emptyMap()
        }

        return raw.entrySet()
            .mapNotNull { (key, value) ->

                if (
                    !value.isJsonPrimitive ||
                    !value.asJsonPrimitive.isString
                ) {
                    return@mapNotNull null
                }

                val normalizedValue =
                    value.asString
                        .normalizedToken()
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: return@mapNotNull null

                key.normalizedToken() to
                        normalizedValue
            }
            .toMap()
    }

    private fun String.normalizedText(): String? =
        trim()
            .replace(
                Regex("\\s+"),
                " "
            )
            .takeIf {
                it.isNotBlank()
            }

    private fun String.normalizedToken(): String =
        trim()
            .lowercase(Locale.ROOT)

    private fun String.normalizedUnit(): String? =
        trim()
            .lowercase(Locale.ROOT)
            .takeIf {
                it.isNotBlank()
            }

    // ---------------------------------------------------------------------
    // JsonObject helpers
    // ---------------------------------------------------------------------

    private fun JsonObject.string(
        field: String
    ): String? =
        get(field)
            ?.takeUnless {
                it.isJsonNull
            }
            ?.takeIf {
                it.isJsonPrimitive &&
                        it.asJsonPrimitive.isString
            }
            ?.asString

    private fun JsonObject.double(
        field: String
    ): Double? =
        get(field)
            ?.takeUnless {
                it.isJsonNull
            }
            ?.takeIf {
                it.isJsonPrimitive
            }
            ?.runCatching {
                asDouble
            }
            ?.getOrNull()
            ?.takeIf {
                it.isFinite()
            }

    private fun JsonObject.int(
        field: String
    ): Int? =
        double(field)
            ?.toInt()

    private fun JsonObject.long(
        field: String
    ): Long? =
        double(field)
            ?.toLong()

    private fun JsonObject.boolean(
        field: String
    ): Boolean? {

        val element =
            get(field)
                ?: return null

        if (
            !element.isJsonPrimitive ||
            !element.asJsonPrimitive.isBoolean
        ) {
            return null
        }

        return element.asBoolean
    }

    private fun JsonObject.booleanLike(
        field: String
    ): Boolean? {

        val element =
            get(field)
                ?: return null

        if (element.isJsonNull) {
            return null
        }

        if (!element.isJsonPrimitive) {
            return null
        }

        val primitive =
            element.asJsonPrimitive

        return when {
            primitive.isBoolean ->
                primitive.asBoolean

            primitive.isNumber ->
                primitive.asDouble != 0.0

            primitive.isString ->
                when (
                    primitive.asString
                        .trim()
                        .lowercase(Locale.ROOT)
                ) {
                    "1",
                    "true",
                    "yes",
                    "y" ->
                        true

                    "0",
                    "false",
                    "no",
                    "n" ->
                        false

                    else ->
                        null
                }

            else ->
                null
        }
    }

    private fun JsonObject.presenceBoolean(
        field: String
    ): Boolean? {

        val element =
            get(field)
                ?: return null

        if (element.isJsonNull) {
            return null
        }

        return when {
            element.isJsonPrimitive ->
                booleanLike(field)
                    ?: true

            element.isJsonArray ->
                element.asJsonArray.size() > 0

            element.isJsonObject ->
                element.asJsonObject.size() > 0

            else ->
                true
        }
    }

    private fun JsonObject.objectValue(
        field: String
    ): JsonObject? =
        get(field)
            ?.takeIf {
                it.isJsonObject
            }
            ?.asJsonObject

    private fun JsonObject.array(
        field: String
    ): JsonArray? =
        get(field)
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray

    private fun JsonObject.stringArray(
        field: String
    ): List<String> =
        array(field)
            ?.mapNotNull { element ->
                if (
                    element.isJsonPrimitive &&
                    element.asJsonPrimitive.isString
                ) {
                    element.asString
                        .normalizedToken()
                        .takeIf {
                            it.isNotBlank()
                        }
                } else {
                    null
                }
            }
            ?.distinct()
            .orEmpty()

    private fun JsonObject.stringOrArrayValues(
        textField: String,
        arrayField: String
    ): List<String> {

        val fromArray =
            stringArray(arrayField)

        if (fromArray.isNotEmpty()) {
            return fromArray
        }

        return string(textField)
            ?.split(',')
            ?.mapNotNull {
                it.normalizedText()
            }
            ?.distinct()
            .orEmpty()
    }

    private fun <T> JsonArray.mapObjects(
        mapper: (JsonObject) -> T
    ): List<T> =
        mapNotNull { element ->
            element
                .takeIf {
                    it.isJsonObject
                }
                ?.asJsonObject
                ?.let(mapper)
        }

    private fun JsonElement.flattenPrimitiveStrings(): List<String> {

        val result =
            mutableListOf<String>()

        fun walk(
            element: JsonElement
        ) {

            when {
                element.isJsonPrimitive -> {

                    val primitive =
                        element.asJsonPrimitive

                    if (primitive.isString) {
                        primitive.asString
                            .normalizedToken()
                            .takeIf {
                                it.isNotBlank()
                            }
                            ?.let(result::add)
                    }
                }

                element.isJsonArray ->
                    element.asJsonArray
                        .forEach(::walk)

                element.isJsonObject ->
                    element.asJsonObject
                        .entrySet()
                        .forEach {
                            walk(it.value)
                        }
            }
        }

        walk(this)

        return result
            .distinct()
    }
}