package de.shopme.tools.knowledge.off.extractor

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.text.Normalizer
import java.util.Locale

/**
 * Extracts a conservative match alias from Open Food Facts products that
 * represent exactly one ingredient.
 *
 * The result may only be used as an additional retrieval alias for an already
 * validated nutrition candidate. It must never create or synthesize nutrition
 * values.
 */
class OFFSingleIngredientNutritionAliasExtractor {

    private companion object {

        val WHITESPACE_REGEX =
            Regex("\\s+")

        val MULTI_COMPONENT_SEPARATOR_REGEX =
            Regex("[,;:+/&]")

        val PERCENTAGE_REGEX =
            Regex("""\d+(?:[.,]\d+)?\s*%""")

        val QUANTITY_REGEX =
            Regex(
                """\b\d+(?:[.,]\d+)?\s*""" +
                        """(?:g|kg|mg|ml|cl|l|oz|lb)\b""",
            )

        val PARENTHESIS_REGEX =
            Regex("""[()\[\]{}]""")

        val UNSAFE_WORD_REGEX =
            Regex(
                """\b(""" +
                        """and|with|contains|containing|including|""" +
                        """mixture|mix|blend|preparation|seasoning|""" +
                        """flavour|flavor|aroma|extract|powdered mix|""" +
                        """salted|sweetened|syrup|sauce|oil|vinegar|""" +
                        """sugar|salt|water|preservative|stabilizer|""" +
                        """stabiliser|emulsifier|colour|color|""" +
                        """acid|antioxidant""" +
                        """)\b""",
            )

        val UNSAFE_TAG_PREFIXES =
            setOf(
                "en:e",
                "de:e",
                "fr:e",
            )

        val UNSAFE_EXACT_VALUES =
            setOf(
                "ingredient",
                "ingredients",
                "unknown",
                "undefined",
                "null",
            )
    }

    fun extract(
        product: JsonObject,
    ): Set<String> {

        val taggedAliases =
            extractFromIngredientTags(
                product =
                    product,
            )

        if (taggedAliases.size == 1) {
            return taggedAliases
        }

        if (taggedAliases.size > 1) {
            return emptySet()
        }

        val textAlias =
            extractFromIngredientText(
                product =
                    product,
            )
                ?: return emptySet()

        return setOf(textAlias)
    }

    private fun extractFromIngredientTags(
        product: JsonObject,
    ): Set<String> {

        val rawTags =
            product
                .get("ingredients_tags")
                ?.takeIf(JsonElement::isJsonArray)
                ?.asJsonArray
                ?.mapNotNull { element ->

                    element
                        .takeIf {
                            it.isJsonPrimitive &&
                                    it.asJsonPrimitive.isString
                        }
                        ?.asString
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                }
                .orEmpty()

        if (rawTags.isEmpty()) {
            return emptySet()
        }

        val normalizedTags =
            rawTags
                .asSequence()
                .filterNot { value ->
                    UNSAFE_TAG_PREFIXES.any { prefix ->
                        value.startsWith(
                            prefix =
                                prefix,
                            ignoreCase =
                                true,
                        )
                    }
                }
                .map { value ->
                    value.substringAfter(
                        delimiter =
                            ":",
                        missingDelimiterValue =
                            value,
                    )
                }
                .map(::normalize)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()
                .toList()

        /*
         * The product must contain exactly one distinct ingredient tag before
         * semantic safety filtering. Otherwise a multi-ingredient product could
         * become a false single-ingredient product merely because tags such as
         * salt, sugar or water are filtered out.
         */
        if (normalizedTags.size != 1) {
            return emptySet()
        }

        val alias =
            normalizedTags.single()

        if (!isSafeAlias(alias)) {
            return emptySet()
        }

        return setOf(alias)
    }

    private fun extractFromIngredientText(
        product: JsonObject,
    ): String? {

        val rawText =
            sequenceOf(
                "ingredients_text_en",
                "ingredients_text_de",
                "ingredients_text_fr",
                "ingredients_text",
            )
                .mapNotNull { key ->
                    product
                        .get(key)
                        ?.takeIf {
                            it.isJsonPrimitive &&
                                    it.asJsonPrimitive.isString
                        }
                        ?.asString
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                }
                .firstOrNull()
                ?: return null

        if (
            MULTI_COMPONENT_SEPARATOR_REGEX.containsMatchIn(
                rawText,
            )
        ) {
            return null
        }

        if (
            PERCENTAGE_REGEX.containsMatchIn(
                rawText,
            )
        ) {
            return null
        }

        if (
            QUANTITY_REGEX.containsMatchIn(
                rawText,
            )
        ) {
            return null
        }

        if (
            PARENTHESIS_REGEX.containsMatchIn(
                rawText,
            )
        ) {
            return null
        }

        val normalized =
            normalize(
                value =
                    rawText,
            )

        if (!isSafeAlias(normalized)) {
            return null
        }

        return normalized
    }

    private fun isSafeAlias(
        value: String,
    ): Boolean {

        if (value.isBlank()) {
            return false
        }

        if (value in UNSAFE_EXACT_VALUES) {
            return false
        }

        if (value.length !in 3..80) {
            return false
        }

        if (value.any(Char::isDigit)) {
            return false
        }

        if (
            UNSAFE_WORD_REGEX.containsMatchIn(
                value,
            )
        ) {
            return false
        }

        val tokenCount =
            value
                .split(
                    WHITESPACE_REGEX,
                )
                .count(String::isNotBlank)

        if (tokenCount !in 1..5) {
            return false
        }

        return true
    }

    private fun normalize(
        value: String,
    ): String {

        val decomposed =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFD,
            )

        return decomposed
            .replace(
                Regex("\\p{M}+"),
                "",
            )
            .lowercase(
                Locale.ROOT,
            )
            .replace(
                Regex("[^\\p{L}\\s-]"),
                " ",
            )
            .replace(
                "-",
                " ",
            )
            .replace(
                WHITESPACE_REGEX,
                " ",
            )
            .trim()
    }
}