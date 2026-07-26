package de.shopme.tools.knowledge.off.extractor

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.ki_candidates.CandidateMetadata
import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import de.shopme.tools.knowledge.locality.Locality
import de.shopme.tools.knowledge.off.identity.OFFProductIdentityResolver
import java.io.File
import java.util.zip.GZIPInputStream

class OFFCandidateExtractor(
    private val identityResolver: OFFProductIdentityResolver =
        OFFProductIdentityResolver(),
    private val singleIngredientNutritionAliasExtractor:
    OFFSingleIngredientNutritionAliasExtractor =
        OFFSingleIngredientNutritionAliasExtractor(),
) {

    private companion object {

        val WHITESPACE_REGEX =
            Regex("\\s+")
    }

    fun forEachCandidate(
        file: File,
        maxCandidates: Int? = null,
        consumer: (CanonicalKnowledgeCandidate) -> Unit
    ) {
        require(file.exists()) {
            "OFF slim dump not found: ${file.absolutePath}"
        }

        require(maxCandidates == null || maxCandidates > 0) {
            "maxCandidates must be null or greater than zero."
        }

        var count = 0

        GZIPInputStream(file.inputStream())
            .bufferedReader()
            .use { reader ->

                while (maxCandidates == null || count < maxCandidates) {
                    val line =
                        reader.readLine()
                            ?: break

                    val candidate =
                        parseCandidate(line)
                            ?: continue

                    consumer(candidate)

                    count++
                }
            }
    }

    fun extract(
        file: File,
        maxCandidates: Int? = null
    ): List<CanonicalKnowledgeCandidate> {

        val candidates =
            mutableListOf<CanonicalKnowledgeCandidate>()

        forEachCandidate(
            file = file,
            maxCandidates = maxCandidates
        ) { candidate ->
            candidates += candidate
        }

        return candidates
    }

    private fun parseCandidate(
        line: String
    ): CanonicalKnowledgeCandidate? {

        val json =
            runCatching {
                JsonParser
                    .parseString(line)
                    .asJsonObject
            }.getOrNull()
                ?: return null

        val englishName =
            json.string("product_name_en")

        val germanName =
            json.string("product_name_de")

        val productName =
            englishName
                ?: germanName
                ?: return null

        val canonical =
            identityResolver.resolve(
                productName = productName,
                brand = json.string("brands"),
                categories = json.string("categories")
            )

        val categories =
            json.string("categories")

        val nutritionDimension =
            parseNutritionDimension(
                json =
                    json,
            )

        val aliases =
            listOfNotNull(
                englishName,
                germanName
            )
                .toSortedSet()

        val categoryAliases =
            extractCategoryAliases(
                categories =
                    categories,
            )

        val singleIngredientNutritionAliases =
            if (nutritionDimension != null) {
                singleIngredientNutritionAliasExtractor
                    .extract(
                        product =
                            json,
                    )
            } else {
                emptySet()
            }

        val matchAliases =
            (
                    categoryAliases +
                            singleIngredientNutritionAliases
                    )
                .filterNot { alias ->
                    alias in aliases
                }
                .toSortedSet()

        return CanonicalKnowledgeCandidate(
            canonicalId = canonical,
            aliases = aliases,
            matchAliases = matchAliases,
            dimensions =
                listOfNotNull(
                    nutritionDimension,
                    parseIngredientsDimension(json),
                    parseAllergensDimension(json),
                    parsePackagingDimension(json),
                    parseTaxonomyDimension(json),
                    parseProcessingDimension(json),
                    parseProductionDimension(json),
                    parseFoodMilesDimension(json),
                    parseLocalityDimension(json),
                    parseNutriScoreDimension(json),
                    parseSeasonalityDimension(json),
                    parseDietDimension(json),
                    parseFairTradeDimension(json),
                    parseAnimalWelfareDimension(json),
                    parseRecipeDimension(json),
                    parseIngredientGraphDimension(json),
                    parseRecipeGraphDimension(json)
                ),
            metadata = CandidateMetadata(
                source = "open_food_facts",
                sourceId = json.string("code")
                    ?: canonical,
                confidence = 1.0,
                version = "1",
                attributes =
                    mapOf(
                        "productName" to
                                productName,
                        "brand" to
                                json.string(
                                    "brands",
                                ),
                        "categories" to
                                categories,
                        "singleIngredientNutritionAliases" to
                                singleIngredientNutritionAliases
                                    .joinToString(
                                        separator =
                                            "|",
                                    )
                                    .takeIf(String::isNotBlank),
                    )
                        .filterValues {
                            it != null
                        }
                        .mapValues {
                            it.value!!
                        }
            )
        )
    }

    private fun extractCategoryAliases(
        categories: String?
    ): Set<String> {

        if (categories.isNullOrBlank()) {
            return emptySet()
        }

        if (categories.length > 10_000) {
            return emptySet()
        }

        val cleaned =
            categories
                .split(",", ";")
                .map { category ->
                    category
                        .substringAfterLast(":")
                        .trim()
                        .lowercase()
                        .replace("-", " ")
                        .collapseWhitespace()
                        .removeSuffix("s")
                        .trim()
                }
                .filter { category ->
                    category.isSafeCategoryAlias()
                }

        return cleaned
            .takeLast(3)
            .toSet()
    }

    private fun String.isSafeCategoryAlias(): Boolean {

        if (isBlank()) {
            return false
        }

        if (length < 4) {
            return false
        }

        if (this in unsafeCategoryAliases) {
            return false
        }

        if (contains("undefined")) {
            return false
        }

        if (contains("food")) {
            return false
        }

        if (contains("beverage")) {
            return false
        }

        if (contains("product")) {
            return false
        }

        if (contains("grocer")) {
            return false
        }

        return true
    }

    private val unsafeCategoryAliases =
        setOf(
            "produce",
            "fruit",
            "vegetable",
            "vegetables",
            "meat",
            "fish",
            "seafood",
            "pork",
            "beef",
            "chicken",
            "turkey",
            "fat",
            "oil",
            "sauce",
            "condiment",
            "dressing",
            "dip",
            "spread",
            "seed",
            "seeds",
            "nut",
            "nuts",
            "snack",
            "sweet snack",
            "dessert",
            "dairie",
            "dairy",
            "cheese",
            "milk",
            "yogurt",
            "cream",
            "drink",
            "water",
            "juice",
            "tea",
            "coffee",
            "cereal",
            "cereals",
            "pasta",
            "rice",
            "bread",
            "bakery",
            "pantry",
            "pantry essential",
            "fresh",
            "frozen",
            "raw",
            "cooked",
            "dried",
            "dehydrated",
            "null",
            "undefined"
        )

    private fun parseNutritionDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val nutriments =
            json.getAsJsonObject("nutriments")
                ?: return null

        val values =
            mapOf(
                "energyKcalPer100g" to nutriments.number("energy-kcal_100g"),
                "fatPer100g" to nutriments.number("fat_100g"),
                "saturatedFatPer100g" to nutriments.number("saturated-fat_100g"),
                "carbohydratesPer100g" to nutriments.number("carbohydrates_100g"),
                "sugarsPer100g" to nutriments.number("sugars_100g"),
                "fiberPer100g" to nutriments.number("fiber_100g"),
                "proteinsPer100g" to nutriments.number("proteins_100g"),
                "saltPer100g" to nutriments.number("salt_100g")
            )
                .filterValues {
                    it != null
                }

        if (values.isEmpty()) {
            return null
        }

        if (!isValidNutrition(values)) {
            return null
        }

        return KnowledgeDimensionCandidate(
            dimension = KnowledgeDimensionCandidateType.NUTRITION,
            payload = values
        )
    }

    private fun parseIngredientsDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {
        val ingredientsText =
            firstNonBlankString(
                json,
                "ingredients_text",
                "ingredients_text_de",
                "ingredients_text_en",
                "ingredients_text_fr"
            )

        val ingredientsTags =
            json.stringList("ingredients_tags")
                .map { it.removePrefix("en:") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

        if (ingredientsText == null && ingredientsTags.isEmpty()) {
            return null
        }

        val payload =
            linkedMapOf<String, Any>()

        ingredientsText?.let {
            payload["ingredientsText"] = it
        }

        if (ingredientsTags.isNotEmpty()) {
            payload["ingredients"] = ingredientsTags
        }

        return KnowledgeDimensionCandidate(
            dimension = KnowledgeDimensionCandidateType.INGREDIENTS,
            payload = payload
        )
    }

    private fun isValidNutrition(
        values: Map<String, Double?>
    ): Boolean {

        fun value(
            key: String
        ): Double {
            return values[key]
                ?: 0.0
        }

        val calories =
            value("energyKcalPer100g")

        val fat =
            value("fatPer100g")

        val saturatedFat =
            value("saturatedFatPer100g")

        val carbohydrates =
            value("carbohydratesPer100g")

        val sugars =
            value("sugarsPer100g")

        val fiber =
            value("fiberPer100g")

        val proteins =
            value("proteinsPer100g")

        val salt =
            value("saltPer100g")


        if (calories < 0 || calories > 950) {
            return false
        }

        listOf(
            fat,
            saturatedFat,
            carbohydrates,
            sugars,
            fiber,
            proteins
        ).forEach {

            if (it < 0 || it > 100) {
                return false
            }
        }

        if (salt < 0 || salt > 100) {
            return false
        }


        if (saturatedFat > fat && fat > 0) {
            return false
        }

        if (sugars > carbohydrates && carbohydrates > 0) {
            return false
        }


        val macroSum =
            fat +
                    carbohydrates +
                    fiber +
                    proteins

        if (macroSum > 120) {
            return false
        }


        return true
    }

    private fun JsonObject.string(
        key: String
    ): String? {
        return get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive
            }
            ?.asString
            ?.trim()
            ?.lowercase()
            ?.takeIf {
                it.isNotBlank()
            }
    }

    private fun JsonObject.number(
        key: String
    ): Double? {
        return runCatching {
            get(key)
                ?.takeIf {
                    !it.isJsonNull &&
                            it.isJsonPrimitive
                }
                ?.asDouble
        }.getOrNull()
    }

    internal fun extractCategoryAliasesForTest(
        categories: String?
    ): Set<String> {
        return extractCategoryAliases(categories)
    }

    private fun firstNonBlankString(
        json: JsonObject,
        vararg keys: String
    ): String? {
        return keys
            .asSequence()
            .mapNotNull { key ->
                json.string(key)
            }
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
    }

    private fun JsonObject.stringList(
        key: String
    ): List<String> {
        return get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonArray
            }
            ?.asJsonArray
            ?.mapNotNull { element ->
                element
                    ?.takeIf {
                        !it.isJsonNull &&
                                it.isJsonPrimitive
                    }
                    ?.asString
                    ?.trim()
                    ?.lowercase()
                    ?.takeIf {
                        it.isNotBlank()
                    }
            }
            ?: emptyList()
    }

    private fun List<String>.normalizeTags(): List<String> {

        if (isEmpty()) {
            return emptyList()
        }

        val result =
            linkedSetOf<String>()

        for (raw in this) {

            if (raw.length > 200) {
                continue
            }

            val normalized =
                raw
                    .trim()
                    .lowercase()
                    .replace("-", " ")
                    .replace("_", " ")
                    .collapseWhitespace()
                    .trim()

            if (normalized.isNotBlank()) {
                result += normalized
            }

            if (result.size >= 50) {
                break
            }
        }

        return result
            .toList()
            .sorted()
    }

    private fun parseAllergensDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val allergens =
            json.stringList("allergens_tags")
                .map {
                    it.removePrefix("en:")
                }
                .normalizeTags()

        val traces =
            json.stringList("traces_tags")
                .map {
                    it.removePrefix("en:")
                }
                .normalizeTags()

        if (allergens.isEmpty() && traces.isEmpty()) {
            return null
        }

        val payload =
            linkedMapOf<String, Any>()

        if (allergens.isNotEmpty()) {
            payload["allergens"] = allergens
        }

        if (traces.isNotEmpty()) {
            payload["traces"] = traces
        }

        return KnowledgeDimensionCandidate(
            dimension = KnowledgeDimensionCandidateType.ALLERGENS,
            payload = payload
        )
    }

    private fun parsePackagingDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val packagingText =
            firstNonBlankString(
                json,
                "packaging",
                "packaging_text"
            )

        val packagingTags =
            json.stringList("packaging_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val packagingMaterials =
            json.stringList("packaging_materials_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val packagingShapes =
            json.stringList("packaging_shapes_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        if (
            packagingText == null &&
            packagingTags.isEmpty() &&
            packagingMaterials.isEmpty() &&
            packagingShapes.isEmpty()
        ) {
            return null
        }

        val payload =
            linkedMapOf<String, Any>()

        packagingText?.let {
            payload["packagingText"] = it
        }

        if (packagingTags.isNotEmpty()) {
            payload["packaging"] = packagingTags
        }

        if (packagingMaterials.isNotEmpty()) {
            payload["materials"] = packagingMaterials
        }

        if (packagingShapes.isNotEmpty()) {
            payload["shapes"] = packagingShapes
        }

        return KnowledgeDimensionCandidate(
            dimension = KnowledgeDimensionCandidateType.PACKAGING,
            payload = payload
        )
    }

    private fun parseTaxonomyDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val categories =
            json.stringList("categories_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val hierarchy =
            json.stringList("categories_hierarchy")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val mainCategory =
            firstNonBlankString(
                json,
                "main_category"
            )
                ?.removePrefix("en:")
                ?.trim()
                ?.lowercase()
                ?.replace("-", " ")
                ?.replace("_", " ")
                ?.collapseWhitespace()
                ?.trim()

        if (
            categories.isEmpty() &&
            hierarchy.isEmpty() &&
            mainCategory.isNullOrBlank()
        ) {
            return null
        }

        val payload =
            linkedMapOf<String, Any>()

        if (categories.isNotEmpty()) {
            payload["categories"] = categories
        }

        if (hierarchy.isNotEmpty()) {
            payload["hierarchy"] = hierarchy
        }

        mainCategory
            ?.takeIf { it.isNotBlank() }
            ?.let {
                payload["mainCategory"] = it
            }

        return KnowledgeDimensionCandidate(
            dimension = KnowledgeDimensionCandidateType.TAXONOMY,
            payload = payload
        )
    }

    private fun parseProcessingDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val novaGroup =
            json.number("nova_group")
                ?.toInt()
                ?.takeIf { it in 1..4 }

        val labels =
            json.stringList("labels_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val categories =
            json.stringList("categories_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        if (
            novaGroup == null &&
            labels.isEmpty() &&
            categories.isEmpty()
        ) {
            return null
        }

        val payload =
            linkedMapOf<String, Any>()

        novaGroup?.let {
            payload["novaGroup"] = it
        }

        if (labels.isNotEmpty()) {
            payload["labels"] = labels
        }

        if (categories.isNotEmpty()) {
            payload["categories"] = categories
        }

        return KnowledgeDimensionCandidate(
            dimension = KnowledgeDimensionCandidateType.PROCESSING,
            payload = payload
        )
    }

    private fun parseWaterDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val value =
            firstNumber(
                json,
                "water_footprint_liters_per_kg",
                "water_footprint_l_per_kg",
                "water_liters_per_kg"
            )
                ?.takeIf {
                    it > 0.0
                }
                ?: return null

        return KnowledgeDimensionCandidate(
            dimension = KnowledgeDimensionCandidateType.WATER,
            payload =
                linkedMapOf(
                    "litersPerKilogram" to value
                )
        )
    }

    private fun parseProductionDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val novaGroup =
            json.number("nova_group")
                ?.toInt()

        val labels =
            json.stringList("labels_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val categories =
            json.stringList("categories_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val values =
            buildSet {
                when (novaGroup) {
                    4 -> add("ULTRA_PROCESSED")
                    3 -> add("PROCESSED")
                }

                val text =
                    (labels + categories)
                        .joinToString(" ")

                if (
                    text.contains("raw") ||
                    text.contains("fresh")
                ) {
                    add("RAW")
                }

                if (
                    text.contains("fermented") ||
                    text.contains("fermentation")
                ) {
                    add("FERMENTED")
                }

                if (
                    text.contains("dried") ||
                    text.contains("dry")
                ) {
                    add("DRIED")
                }

                if (
                    text.contains("baked") ||
                    text.contains("bakery")
                ) {
                    add("BAKED")
                }

                if (
                    text.contains("fried")
                ) {
                    add("FRIED")
                }

                if (
                    text.contains("smoked")
                ) {
                    add("SMOKED")
                }

                if (
                    text.contains("canned") ||
                    text.contains("preserved")
                ) {
                    add("CANNED")
                }

                if (
                    text.contains("frozen")
                ) {
                    add("FROZEN")
                }
            }
                .toList()
                .sorted()

        if (values.isEmpty()) {
            return null
        }

        return KnowledgeDimensionCandidate(
            dimension = KnowledgeDimensionCandidateType.PRODUCTION,
            payload =
                linkedMapOf(
                    "production" to values
                )
        )
    }

    private fun parseFoodMilesDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val origins =
            firstNonBlankString(
                json,
                "origins",
                "manufacturing_places"
            )

        val countries =
            firstNonBlankString(
                json,
                "countries"
            )

        if (
            origins == null &&
            countries == null
        ) {
            return null
        }

        val text =
            listOfNotNull(
                origins,
                countries
            )
                .joinToString(" ")
                .lowercase()

        val kilometers =
            estimateFoodMiles(text)
                ?: return null

        return KnowledgeDimensionCandidate(
            dimension =
                KnowledgeDimensionCandidateType.FOOD_MILES,
            payload =
                mapOf(
                    "kilometers" to kilometers
                )
        )
    }

    private fun estimateFoodMiles(
        text: String
    ): Double? {

        return when {

            text.contains("germany") ||
                    text.contains("deutschland") ||
                    text.contains("france") ||
                    text.contains("italy") ||
                    text.contains("spain") ->
                1000.0

            text.contains("europe") ||
                    text.contains("eu") ->
                1500.0

            text.contains("china") ||
                    text.contains("asia") ||
                    text.contains("india") ->
                8000.0

            text.contains("usa") ||
                    text.contains("united states") ||
                    text.contains("america") ||
                    text.contains("brazil") ||
                    text.contains("argentina") ->
                10000.0

            text.isNotBlank() ->
                5000.0

            else ->
                null
        }
    }

    private fun parseLocalityDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val origins =
            firstNonBlankString(
                json,
                "origins",
                "manufacturing_places"
            )

        val countries =
            firstNonBlankString(
                json,
                "countries"
            )

        if (
            origins == null &&
            countries == null
        ) {
            return null
        }

        val text =
            listOfNotNull(
                origins,
                countries
            )
                .joinToString(" ")
                .lowercase()

        val locality =
            estimateLocality(text)
                ?: return null

        return KnowledgeDimensionCandidate(
            dimension =
                KnowledgeDimensionCandidateType.LOCALITY,
            payload =
                mapOf(
                    "locality" to locality.name
                )
        )
    }

    private fun estimateLocality(
        text: String
    ): Locality? {

        return when {

            text.contains("germany") ||
                    text.contains("deutschland") ->
                Locality.NATIONWIDE

            text.contains("france") ||
                    text.contains("italy") ||
                    text.contains("spain") ||
                    text.contains("netherlands") ||
                    text.contains("belgium") ||
                    text.contains("austria") ||
                    text.contains("poland") ||
                    text.contains("europe") ||
                    text.contains("eu") ->
                Locality.EUROPE

            text.contains("usa") ||
                    text.contains("united states") ||
                    text.contains("america") ||
                    text.contains("brazil") ||
                    text.contains("argentina") ||
                    text.contains("china") ||
                    text.contains("india") ||
                    text.contains("asia") ->
                Locality.OVERSEAS

            text.isNotBlank() ->
                Locality.OVERSEAS

            else ->
                null
        }
    }

    private fun parseNutriScoreDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val score =
            firstNonBlankString(
                json,
                "nutrition_grade_fr",
                "nutriscore_grade"
            )
                ?.trim()
                ?.uppercase()
                ?.takeIf {
                    it in setOf("A", "B", "C", "D", "E")
                }
                ?: return null

        return KnowledgeDimensionCandidate(
            dimension =
                KnowledgeDimensionCandidateType.NUTRI_SCORE,
            payload =
                mapOf(
                    "score" to score
                )
        )
    }

    private fun parseSeasonalityDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val categories =
            json.stringList("categories_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val hierarchy =
            json.stringList("categories_hierarchy")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val mainCategory =
            firstNonBlankString(
                json,
                "main_category"
            )
                ?.removePrefix("en:")
                ?.trim()
                ?.lowercase()
                ?.replace("-", " ")
                ?.replace("_", " ")
                ?.collapseWhitespace()
                ?.trim()

        val productName =
            firstNonBlankString(
                json,
                "product_name_en",
                "product_name"
            )

        val ingredients =
            firstNonBlankString(
                json,
                "ingredients_text_en",
                "ingredients_text"
            )

        val text =
            buildList {
                addAll(categories)
                addAll(hierarchy)

                if (!mainCategory.isNullOrBlank()) {
                    add(mainCategory)
                }

                if (!productName.isNullOrBlank()) {
                    add(productName)
                }

                if (!ingredients.isNullOrBlank()) {
                    add(ingredients)
                }
            }
                .joinToString(" ")
                .lowercase()

        val months =
            estimateSeasonalityMonths(text)
                ?: return null

        return KnowledgeDimensionCandidate(
            dimension =
                KnowledgeDimensionCandidateType.SEASONALITY,
            payload =
                mapOf(
                    "months" to months
                )
        )
    }

    private fun estimateSeasonalityMonths(
        text: String
    ): List<Int>? {

        val months =
            when {

                text.contains("strawberr") ->
                    listOf(5, 6, 7)

                text.contains("raspberr") ->
                    listOf(6, 7, 8)

                text.contains("blueberr") ->
                    listOf(6, 7, 8)

                text.contains("berr") ||
                        text.contains("berries") ->
                    listOf(5, 6, 7, 8)

                text.contains("apple") ||
                        text.contains("apples") ->
                    listOf(8, 9, 10, 11)

                text.contains("pear") ||
                        text.contains("pears") ->
                    listOf(8, 9, 10)

                text.contains("asparagus") ->
                    listOf(4, 5, 6)

                text.contains("tomato") ||
                        text.contains("tomatoes") ->
                    listOf(6, 7, 8, 9)

                text.contains("pumpkin") ||
                        text.contains("pumpkins") ->
                    listOf(9, 10, 11)

                text.contains("orange") ||
                        text.contains("oranges") ||
                        text.contains("lemon") ||
                        text.contains("lemons") ||
                        text.contains("citrus") ->
                    listOf(1, 2, 3, 11, 12)

                text.contains("fruit") ||
                        text.contains("fruits") ->
                    listOf(6, 7, 8, 9)

                text.contains("vegetable") ||
                        text.contains("vegetables") ->
                    listOf(6, 7, 8, 9, 10)

                else ->
                    null
            }

        return months
            ?.distinct()
            ?.sorted()
    }

    private fun parseDietDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val labels =
            json.stringList("labels_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val categories =
            json.stringList("categories_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val ingredients =
            json.stringList("ingredients_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val ingredientsText =
            firstNonBlankString(
                json,
                "ingredients_text_en",
                "ingredients_text"
            )
                .orEmpty()
                .lowercase()

        val allergens =
            json.stringList("allergens_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val text =
            buildList {
                addAll(labels)
                addAll(categories)
                addAll(ingredients)
                addAll(allergens)

                if (ingredientsText.isNotBlank()) {
                    add(ingredientsText)
                }
            }
                .joinToString(" ")
                .lowercase()

        val classifications =
            estimateDietClassifications(text)

        if (classifications.isEmpty()) {
            return null
        }

        return KnowledgeDimensionCandidate(
            dimension =
                KnowledgeDimensionCandidateType.DIET,
            payload =
                mapOf(
                    "classifications" to classifications
                )
        )
    }

    private fun estimateDietClassifications(
        text: String
    ): List<String> {

        val containsMeat =
            listOf(
                "meat",
                "beef",
                "pork",
                "chicken",
                "turkey",
                "bacon",
                "ham",
                "gelatin"
            ).any { text.contains(it) }

        val containsFish =
            listOf(
                "fish",
                "salmon",
                "tuna",
                "cod",
                "shrimp",
                "prawn",
                "crab",
                "shellfish"
            ).any { text.contains(it) }

        val containsAnimalProducts =
            containsMeat ||
                    containsFish ||
                    listOf(
                        "milk",
                        "egg",
                        "eggs",
                        "cheese",
                        "butter",
                        "cream",
                        "honey",
                        "whey",
                        "casein"
                    ).any { text.contains(it) }

        return buildSet {
            if (
                text.contains("vegan") &&
                !containsAnimalProducts
            ) {
                add("VEGAN")
            }

            if (
                (text.contains("vegetarian") ||
                        text.contains("vegan")) &&
                !containsMeat &&
                !containsFish
            ) {
                add("VEGETARIAN")
            }

            if (
                text.contains("pescetarian") &&
                !containsMeat
            ) {
                add("PESCETARIAN")
            }

            if (
                text.contains("gluten free") ||
                text.contains("gluten-free") ||
                text.contains("without gluten")
            ) {
                add("GLUTEN_FREE")
            }

            if (
                text.contains("lactose free") ||
                text.contains("lactose-free") ||
                text.contains("without lactose")
            ) {
                add("LACTOSE_FREE")
            }

            if (text.contains("halal")) {
                add("HALAL")
            }

            if (text.contains("kosher")) {
                add("KOSHER")
            }
        }
            .toList()
            .sorted()
    }

    private fun parseFairTradeDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val labels =
            json.stringList("labels_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val labelsText =
            firstNonBlankString(
                json,
                "labels"
            )
                .orEmpty()
                .lowercase()

        val text =
            buildList {
                addAll(labels)

                if (labelsText.isNotBlank()) {
                    add(labelsText)
                }
            }
                .joinToString(" ")
                .lowercase()

        val score =
            estimateFairTradeScore(text)
                ?: return null

        return KnowledgeDimensionCandidate(
            dimension =
                KnowledgeDimensionCandidateType.FAIRTRADE,
            payload =
                mapOf(
                    "score" to score
                )
        )
    }

    private fun estimateFairTradeScore(
        text: String
    ): Double? {

        if (text.isBlank()) {
            return null
        }

        return when {

            text.contains("fair trade") ||
                    text.contains("fairtrade") ||
                    text.contains("max havelaar") ||
                    text.contains("commerce equitable") ||
                    text.contains("fair for life") ->
                1.0

            else ->
                null
        }
    }

    private fun parseAnimalWelfareDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val labels =
            json.stringList("labels_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val categories =
            json.stringList("categories_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val text =
            buildList {
                addAll(labels)
                addAll(categories)
            }
                .joinToString(" ")
                .lowercase()

        val score =
            estimateAnimalWelfareScore(text)
                ?: return null

        return KnowledgeDimensionCandidate(
            dimension =
                KnowledgeDimensionCandidateType.ANIMAL_WELFARE,
            payload =
                mapOf(
                    "score" to score
                )
        )
    }

    private fun estimateAnimalWelfareScore(
        text: String
    ): Double? {

        if (text.isBlank()) {
            return null
        }

        return when {

            // sehr gute Tierwohl-Indikatoren
            text.contains("animal welfare") ||
                    text.contains("animal wellbeing") ||
                    text.contains("high animal welfare") ->
                1.0


            // Bio-Haltung als positiver Proxy
            text.contains("organic") ||
                    text.contains("bio") ||
                    text.contains("eu organic") ->
                0.8


            // Freilandhaltung
            text.contains("free range") ||
                    text.contains("free-range") ||
                    text.contains("pasture raised") ||
                    text.contains("grass fed") ->
                0.9


            // Käfig / intensive Haltung
            text.contains("caged") ||
                    text.contains("battery cage") ||
                    text.contains("intensive farming") ->
                0.2


            else ->
                null
        }
    }

    private fun parseRecipeDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val categories =
            json.stringList("categories_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val hierarchy =
            json.stringList("categories_hierarchy")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val mainCategory =
            firstNonBlankString(
                json,
                "main_category"
            )
                ?.removePrefix("en:")
                ?.trim()
                ?.lowercase()
                ?.replace("-", " ")
                ?.replace("_", " ")
                ?.collapseWhitespace()
                ?.trim()

        val productName =
            firstNonBlankString(
                json,
                "product_name_en",
                "product_name"
            )

        val text =
            buildList {
                addAll(categories)
                addAll(hierarchy)

                if (!mainCategory.isNullOrBlank()) {
                    add(mainCategory)
                }

                if (!productName.isNullOrBlank()) {
                    add(productName)
                }
            }
                .joinToString(" ")
                .lowercase()

        val recipes =
            estimateRecipeReferences(text)

        if (recipes.isEmpty()) {
            return null
        }

        return KnowledgeDimensionCandidate(
            dimension =
                KnowledgeDimensionCandidateType.RECIPE,
            payload =
                mapOf(
                    "recipes" to recipes
                )
        )
    }

    private fun estimateRecipeReferences(
        text: String
    ): List<String> {

        if (text.isBlank()) {
            return emptyList()
        }

        return buildSet {

            if (
                text.contains("tomato") ||
                text.contains("tomatoes")
            ) {
                add("tomato_sauce")
                add("bruschetta")
            }

            if (
                text.contains("pasta") ||
                text.contains("spaghetti") ||
                text.contains("noodle")
            ) {
                add("pasta_with_tomato_sauce")
            }

            if (
                text.contains("rice")
            ) {
                add("fried_rice")
                add("rice_bowl")
            }

            if (
                text.contains("potato") ||
                text.contains("potatoes")
            ) {
                add("roasted_potatoes")
                add("potato_gratin")
            }

            if (
                text.contains("apple") ||
                text.contains("apples")
            ) {
                add("apple_pie")
                add("apple_crumble")
            }

            if (
                text.contains("strawberry") ||
                text.contains("strawberries")
            ) {
                add("strawberry_jam")
                add("strawberry_cake")
            }

            if (
                text.contains("banana") ||
                text.contains("bananas")
            ) {
                add("banana_bread")
                add("banana_smoothie")
            }

            if (
                text.contains("milk")
            ) {
                add("pancakes")
                add("bechamel_sauce")
            }

            if (
                text.contains("egg") ||
                text.contains("eggs")
            ) {
                add("omelette")
                add("pancakes")
            }

            if (
                text.contains("cheese")
            ) {
                add("gratin")
                add("cheese_sandwich")
            }

            if (
                text.contains("chicken")
            ) {
                add("chicken_curry")
                add("roast_chicken")
            }

            if (
                text.contains("beef")
            ) {
                add("beef_stew")
                add("bolognese")
            }

            if (
                text.contains("fish") ||
                text.contains("salmon") ||
                text.contains("tuna")
            ) {
                add("fish_with_rice")
                add("fish_tacos")
            }

            if (
                text.contains("flour")
            ) {
                add("bread")
                add("pancakes")
            }

            if (
                text.contains("bread")
            ) {
                add("sandwich")
                add("bruschetta")
            }

            if (
                text.contains("chocolate")
            ) {
                add("chocolate_cake")
                add("brownies")
            }
        }
            .toList()
            .sorted()
    }

    private fun parseIngredientGraphDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val taggedIngredients =
            json.stringList("ingredients_tags")
                .map {
                    it.removePrefix("en:")
                }
                .normalizeTags()
                .filter {
                    it.isSafeIngredientGraphNode()
                }

        val textIngredients =
            if (taggedIngredients.isEmpty()) {
                firstNonBlankString(
                    json,
                    "ingredients_text_en",
                    "ingredients_text_de",
                    "ingredients_text_fr",
                    "ingredients_text"
                )
                    ?.toIngredientGraphNodes()
                    ?: emptyList()
            } else {
                emptyList()
            }

        val ingredients =
            (taggedIngredients + textIngredients)
                .distinct()
                .sorted()

        if (ingredients.isEmpty()) {
            return null
        }

        return KnowledgeDimensionCandidate(
            dimension =
                KnowledgeDimensionCandidateType.INGREDIENT_GRAPH,
            payload =
                mapOf(
                    "ingredients" to ingredients
                )
        )
    }

    private fun String.toIngredientGraphNodes(): List<String> {

        return split(
            ",",
            ";",
            ":",
            ".",
            "(",
            ")",
            "[",
            "]"
        )
            .map { value ->
                value
                    .trim()
                    .lowercase()
                    .replace("-", " ")
                    .replace("_", " ")
                    .collapseWhitespace()
                    .trim()
            }
            .map { value ->
                value
                    .removePrefix("contains ")
                    .removePrefix("may contain ")
                    .removePrefix("and ")
                    .removePrefix("or ")
                    .removePrefix("with ")
                    .removePrefix("of ")
                    .trim()
            }
            .filter {
                it.isSafeIngredientGraphNode()
            }
            .distinct()
            .sorted()
    }

    private fun String.isSafeIngredientGraphNode(): Boolean {

        if (isBlank()) {
            return false
        }

        if (length < 3) {
            return false
        }

        if (length > 60) {
            return false
        }

        if (contains("unknown")) {
            return false
        }

        if (contains("undefined")) {
            return false
        }

        if (contains("ingredient")) {
            return false
        }

        if (contains("may contain")) {
            return false
        }

        if (contains("trace")) {
            return false
        }

        if (contains("%")) {
            return false
        }

        if (any { it.isDigit() }) {
            return false
        }

        return true
    }

    private fun parseRecipeGraphDimension(
        json: JsonObject
    ): KnowledgeDimensionCandidate? {

        val categories =
            json.stringList("categories_tags")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val hierarchy =
            json.stringList("categories_hierarchy")
                .map { it.removePrefix("en:") }
                .normalizeTags()

        val mainCategory =
            firstNonBlankString(
                json,
                "main_category"
            )
                ?.removePrefix("en:")
                ?.trim()
                ?.lowercase()
                ?.replace("-", " ")
                ?.replace("_", " ")
                ?.collapseWhitespace()
                ?.trim()

        val productName =
            firstNonBlankString(
                json,
                "product_name_en",
                "product_name"
            )

        val text =
            buildList {
                addAll(categories)
                addAll(hierarchy)

                if (!mainCategory.isNullOrBlank()) {
                    add(mainCategory)
                }

                if (!productName.isNullOrBlank()) {
                    add(productName)
                }
            }
                .joinToString(" ")
                .lowercase()

        val recipes =
            estimateRecipeReferences(text)

        if (recipes.isEmpty()) {
            return null
        }

        return KnowledgeDimensionCandidate(
            dimension =
                KnowledgeDimensionCandidateType.RECIPE_GRAPH,
            payload =
                mapOf(
                    "recipes" to recipes
                )
        )
    }

    private fun String.collapseWhitespace(): String {

        if (isEmpty()) {
            return this
        }

        val builder =
            StringBuilder(length)

        var previousWasWhitespace =
            false

        for (char in this) {
            if (char.isWhitespace()) {
                if (!previousWasWhitespace) {
                    builder.append(' ')
                    previousWasWhitespace = true
                }
            } else {
                builder.append(char)
                previousWasWhitespace = false
            }
        }

        return builder.toString()
    }

    private fun firstNumber(
        json: JsonObject,
        vararg keys: String
    ): Double? =
        keys
            .asSequence()
            .mapNotNull { key ->
                json.number(key)
            }
            .firstOrNull()
}