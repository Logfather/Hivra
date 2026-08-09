package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

import com.google.gson.JsonObject
import java.text.Normalizer

class CanonicalCatalogLinguisticRepairValidator {

    fun validate(
        catalog: List<JsonObject>,
        stats: CanonicalCatalogLinguisticRepairStats,
        deterministic: Boolean
    ): CanonicalCatalogLinguisticRepairReport {

        val normalizedKeys =
            catalog.map {
                requireString(
                    it,
                    "normalized"
                )
            }

        val itemNames =
            catalog.map {
                requireString(
                    it,
                    "itemname"
                )
            }

        val blankPluralCount =
            catalog.count { entry ->
                optionalString(
                    entry,
                    "plural"
                )
                    .isNullOrBlank()
            }

        val variantLeakCount =
            catalog.sumOf { entry ->
                countVariantLeaks(
                    entry
                )
            }

        val duplicateColloquialValueCount =
            catalog.sumOf { entry ->

                val aliases =
                    stringList(
                        entry,
                        "colloquial"
                    )

                aliases.size -
                        aliases
                            .map(::normalize)
                            .distinct()
                            .size
            }

        val emptyAutocompleteEntryCount =
            catalog.count { entry ->

                stringList(
                    entry,
                    "autocompleteTokens"
                )
                    .isEmpty()
            }

        val uniqueNormalizedKeyCount =
            normalizedKeys
                .distinct()
                .size

        val uniqueItemNameCount =
            itemNames
                .map(::normalize)
                .distinct()
                .size

        val valid =
            catalog.isNotEmpty() &&
                    uniqueNormalizedKeyCount ==
                    catalog.size &&
                    uniqueItemNameCount ==
                    catalog.size &&
                    blankPluralCount == 0 &&
                    variantLeakCount == 0 &&
                    duplicateColloquialValueCount == 0 &&
                    emptyAutocompleteEntryCount == 0 &&
                    deterministic

        return CanonicalCatalogLinguisticRepairReport(
            schemaVersion = 1,

            inputEntryCount =
                catalog.size,

            outputEntryCount =
                catalog.size,

            uniqueNormalizedKeyCount =
                uniqueNormalizedKeyCount,

            uniqueItemNameCount =
                uniqueItemNameCount,

            blankPluralCount =
                blankPluralCount,

            variantLeakedIntoColloquialCount =
                variantLeakCount,

            duplicateColloquialValueCount =
                duplicateColloquialValueCount,

            emptyAutocompleteEntryCount =
                emptyAutocompleteEntryCount,

            deterministic =
                deterministic,

            valid =
                valid,

            stats =
                stats
        )
    }

    private fun countVariantLeaks(
        entry: JsonObject
    ): Int {

        val itemName =
            requireString(
                entry,
                "itemname"
            )

        val parts =
            itemName
                .split(VARIANT_SEPARATOR)
                .map(String::trim)
                .filter(String::isNotBlank)

        if (parts.size < 2) {
            return 0
        }

        val variants =
            parts
                .drop(1)
                .map(::normalize)
                .toSet()

        return stringList(
            entry,
            "colloquial"
        )
            .count {
                normalize(it) in
                        variants
            }
    }

    private fun normalize(
        value: String
    ): String {

        val decomposed =
            Normalizer.normalize(
                value
                    .trim()
                    .lowercase(),
                Normalizer.Form.NFD
            )

        return decomposed
            .replace(
                Regex("\\p{M}+"),
                ""
            )
            .replace(
                "ß",
                "ss"
            )
            .replace(
                Regex("[^a-z0-9]+"),
                " "
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    private fun stringList(
        json: JsonObject,
        key: String
    ): List<String> {

        val element =
            json.get(key)
                ?: return emptyList()

        if (
            !element.isJsonArray
        ) {
            return emptyList()
        }

        return element
            .asJsonArray
            .mapNotNull {
                it
                    .takeIf { value ->
                        value.isJsonPrimitive
                    }
                    ?.asString
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
            }
    }

    private fun optionalString(
        json: JsonObject,
        key: String
    ): String? =
        json
            .get(key)
            ?.takeIf {
                it.isJsonPrimitive
            }
            ?.asString
            ?.trim()

    private fun requireString(
        json: JsonObject,
        key: String
    ): String =
        requireNotNull(
            optionalString(
                json,
                key
            )
                ?.takeIf {
                    it.isNotBlank()
                }
        ) {
            "Missing or blank '$key' in catalog entry."
        }

    private companion object {

        const val VARIANT_SEPARATOR =
            " – "
    }
}