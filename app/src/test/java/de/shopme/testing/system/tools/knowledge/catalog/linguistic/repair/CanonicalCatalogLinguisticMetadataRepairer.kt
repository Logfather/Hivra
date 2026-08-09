package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution.CanonicalProductFamilyIdentityPolicy
import java.text.Normalizer

class CanonicalCatalogLinguisticMetadataRepairer {

    fun repair(
        catalog: List<JsonObject>
    ): Pair<List<JsonObject>, CanonicalCatalogLinguisticRepairStats> {

        val familyPluralByName =
            buildFamilyPluralIndex(
                catalog
            )

        var pluralChangedCount = 0
        var colloquialChangedCount = 0
        var phoneticTokensChangedCount = 0
        var autocompleteTokensChangedCount = 0

        var removedVariantColloquialCount = 0
        var preservedColloquialAliasCount = 0

        var familyPluralResolvedCount = 0
        var familyPluralFallbackCount = 0

        val repaired =
            catalog.map { source ->

                val result =
                    repairEntry(
                        source = source,
                        familyPluralByName =
                            familyPluralByName
                    )

                if (result.pluralChanged) {
                    pluralChangedCount++
                }

                if (result.colloquialChanged) {
                    colloquialChangedCount++
                }

                if (result.phoneticChanged) {
                    phoneticTokensChangedCount++
                }

                if (result.autocompleteChanged) {
                    autocompleteTokensChangedCount++
                }

                removedVariantColloquialCount +=
                    result.removedVariantColloquialCount

                preservedColloquialAliasCount +=
                    result.preservedColloquialAliasCount

                if (result.familyPluralResolved) {
                    familyPluralResolvedCount++
                }

                if (result.familyPluralFallback) {
                    familyPluralFallbackCount++
                }

                result.entry
            }
                .sortedWith(
                    compareBy<JsonObject>(
                        {
                            requireString(
                                it,
                                "category"
                            )
                        },
                        {
                            requireString(
                                it,
                                "normalized"
                            )
                        }
                    )
                )

        return repaired to
                CanonicalCatalogLinguisticRepairStats(
                    repairedEntryCount =
                        repaired.size,

                    pluralChangedCount =
                        pluralChangedCount,

                    colloquialChangedCount =
                        colloquialChangedCount,

                    phoneticTokensChangedCount =
                        phoneticTokensChangedCount,

                    autocompleteTokensChangedCount =
                        autocompleteTokensChangedCount,

                    removedVariantColloquialCount =
                        removedVariantColloquialCount,

                    preservedColloquialAliasCount =
                        preservedColloquialAliasCount,

                    familyPluralResolvedCount =
                        familyPluralResolvedCount,

                    familyPluralFallbackCount =
                        familyPluralFallbackCount
                )
    }

    private fun repairEntry(
        source: JsonObject,
        familyPluralByName: Map<String, String>
    ): EntryRepairResult {

        val itemName =
            requireString(
                source,
                "itemname"
            )

        val normalized =
            requireString(
                source,
                "normalized"
            )

        val originalPlural =
            optionalString(
                source,
                "plural"
            )
                ?: itemName

        val originalColloquial =
            stringList(
                source,
                "colloquial"
            )

        val originalPhonetic =
            stringList(
                source,
                "phoneticTokens"
            )

        val originalAutocomplete =
            stringList(
                source,
                "autocompleteTokens"
            )

        val nameParts =
            itemName
                .split(VARIANT_SEPARATOR)
                .map(String::trim)
                .filter(String::isNotBlank)

        val family =
            nameParts.first()

        val variants =
            nameParts
                .drop(1)

        val pluralResolution =
            buildPlural(
                itemName = itemName,
                family = family,
                variants = variants,
                existingPlural = originalPlural,
                familyPluralByName =
                    familyPluralByName
            )

        val colloquialResolution =
            repairColloquial(
                itemName = itemName,
                plural =
                    pluralResolution.value,
                variants = variants,
                original =
                    originalColloquial
            )

        val phoneticTokens =
            buildPhoneticTokens(
                itemName =
                    itemName,
                colloquial =
                    colloquialResolution.values
            )

        val autocompleteTokens =
            buildAutocompleteTokens(
                itemName = itemName,
                normalized = normalized,
                family = family,
                colloquial =
                    colloquialResolution.values
            )

        val repaired =
            source.deepCopy()

        repaired.addProperty(
            "plural",
            pluralResolution.value
        )

        repaired.add(
            "colloquial",
            jsonArray(
                colloquialResolution.values
            )
        )

        repaired.add(
            "phoneticTokens",
            jsonArray(
                phoneticTokens
            )
        )

        repaired.add(
            "autocompleteTokens",
            jsonArray(
                autocompleteTokens
            )
        )

        return EntryRepairResult(
            entry = repaired,

            pluralChanged =
                originalPlural !=
                        pluralResolution.value,

            colloquialChanged =
                originalColloquial !=
                        colloquialResolution.values,

            phoneticChanged =
                originalPhonetic !=
                        phoneticTokens,

            autocompleteChanged =
                originalAutocomplete !=
                        autocompleteTokens,

            removedVariantColloquialCount =
                colloquialResolution
                    .removedVariantCount,

            preservedColloquialAliasCount =
                colloquialResolution
                    .values
                    .size,

            familyPluralResolved =
                pluralResolution
                    .familyPluralResolved,

            familyPluralFallback =
                pluralResolution
                    .familyPluralFallback
        )
    }

    /*
     * -------------------------------------------------------------
     * PLURAL
     * -------------------------------------------------------------
     *
     * Baseline identity:
     * vorhandenen, nichtleeren Plural beibehalten.
     *
     * Variant identity:
     * Family-Plural + unveränderte Identity-Variants.
     *
     * Brot – Buchweizen
     * ->
     * Brote – Buchweizen
     */
    private fun buildPlural(
        itemName: String,
        family: String,
        variants: List<String>,
        existingPlural: String,
        familyPluralByName: Map<String, String>
    ): PluralResolution {

        if (variants.isEmpty()) {

            return PluralResolution(
                value =
                    existingPlural
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: itemName,

                familyPluralResolved =
                    false,

                familyPluralFallback =
                    false
            )
        }

        val familyPlural =
            CanonicalProductFamilyIdentityPolicy
                .canonicalPlural(
                    family
                )
                ?: CanonicalProductFamilyPluralPolicy
                    .pluralFor(
                        family
                    )
                ?: familyPluralByName[
                    normalizeLookupKey(
                        family
                    )
                ]

        val resolvedFamilyPlural =
            familyPlural
                ?.takeIf {
                    it.isNotBlank()
                }

        val pluralFamily =
            resolvedFamilyPlural
                ?: family

        val value =
            buildList {
                add(pluralFamily)
                addAll(variants)
            }
                .joinToString(
                    separator =
                        VARIANT_SEPARATOR
                )

        return PluralResolution(
            value = value,

            familyPluralResolved =
                resolvedFamilyPlural != null,

            familyPluralFallback =
                resolvedFamilyPlural == null
        )
    }

    private fun buildFamilyPluralIndex(
        catalog: List<JsonObject>
    ): Map<String, String> {

        return catalog
            /*
             * Nur echte standalone identities dürfen
             * Family-Plural liefern.
             */
            .filter { entry ->

                val itemName =
                    requireString(
                        entry,
                        "itemname"
                    )

                VARIANT_SEPARATOR !in itemName
            }
            .associate { entry ->

                val itemName =
                    requireString(
                        entry,
                        "itemname"
                    )

                val plural =
                    optionalString(
                        entry,
                        "plural"
                    )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: itemName

                normalizeLookupKey(
                    itemName
                ) to plural
            }
    }

    /*
     * -------------------------------------------------------------
     * COLLOQUIAL
     * -------------------------------------------------------------
     *
     * Wichtigste Invariante:
     *
     * Identity-Variant-Werte werden niemals automatisch als
     * colloquial alias übernommen.
     */
    private fun repairColloquial(
        itemName: String,
        plural: String,
        variants: List<String>,
        original: List<String>
    ): ColloquialResolution {

        val forbidden =
            buildSet {

                add(
                    normalizeLookupKey(
                        itemName
                    )
                )

                add(
                    normalizeLookupKey(
                        plural
                    )
                )

                variants.forEach { variant ->
                    add(
                        normalizeLookupKey(
                            variant
                        )
                    )
                }
            }

        var removedVariantCount =
            0

        val cleaned =
            original
                .map(String::trim)
                .filter(String::isNotBlank)
                .filter { alias ->

                    val normalizedAlias =
                        normalizeLookupKey(
                            alias
                        )

                    val remove =
                        normalizedAlias in
                                forbidden

                    if (
                        remove &&
                        variants.any { variant ->
                            normalizeLookupKey(
                                variant
                            ) ==
                                    normalizedAlias
                        }
                    ) {
                        removedVariantCount++
                    }

                    !remove
                }
                .distinctBy {
                    normalizeLookupKey(
                        it
                    )
                }
                .sortedWith(
                    compareBy(
                        {
                            normalizeLookupKey(
                                it
                            )
                        },
                        {
                            it
                        }
                    )
                )

        return ColloquialResolution(
            values = cleaned,
            removedVariantCount =
                removedVariantCount
        )
    }

    /*
     * -------------------------------------------------------------
     * PHONETIC TOKENS
     * -------------------------------------------------------------
     *
     * Kein phonetischer Algorithmus wie Soundex:
     * Für deutsche Lebensmittel wäre das zu aggressiv.
     *
     * Stattdessen erzeugen wir deterministische,
     * diakritikfreie Search-Tokens.
     */
    private fun buildPhoneticTokens(
        itemName: String,
        colloquial: List<String>
    ): List<String> {

        val sourceValues =
            buildList {
                add(itemName)
                addAll(colloquial)
            }

        return sourceValues
            .flatMap { value ->
                lexicalTokens(
                    value
                )
            }
            .map(::asciiSearchToken)
            .filter {
                it.length >=
                        MIN_PHONETIC_TOKEN_LENGTH
            }
            .distinct()
            .sorted()
    }

    /*
     * -------------------------------------------------------------
     * AUTOCOMPLETE
     * -------------------------------------------------------------
     */
    private fun buildAutocompleteTokens(
        itemName: String,
        normalized: String,
        family: String,
        colloquial: List<String>
    ): List<String> {

        val canonicalNameToken =
            slugify(
                itemName
            )

        val familyToken =
            slugify(
                family
            )

        val compactName =
            compactToken(
                itemName
            )

        val lexical =
            lexicalTokens(
                itemName
            )
                .map(::slugify)
                .filter {
                    it.length >=
                            MIN_AUTOCOMPLETE_TOKEN_LENGTH
                }

        val colloquialTokens =
            colloquial
                .flatMap { alias ->
                    listOf(
                        slugify(alias),
                        compactToken(alias)
                    )
                }
                .filter {
                    it.length >=
                            MIN_AUTOCOMPLETE_TOKEN_LENGTH
                }

        return buildList {

            add(normalized)

            add(canonicalNameToken)

            if (
                familyToken.isNotBlank()
            ) {
                add(familyToken)
            }

            if (
                compactName.isNotBlank()
            ) {
                add(compactName)
            }

            addAll(
                lexical
            )

            addAll(
                colloquialTokens
            )
        }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
    }

    private fun lexicalTokens(
        value: String
    ): List<String> =
        normalizeText(
            value
        )
            .split(" ")
            .filter(String::isNotBlank)

    private fun slugify(
        value: String
    ): String =
        normalizeText(
            value
        )
            .replace(
                WHITESPACE_REGEX,
                "-"
            )
            .trim('-')

    private fun compactToken(
        value: String
    ): String =
        normalizeText(
            value
        )
            .replace(
                " ",
                ""
            )

    private fun asciiSearchToken(
        value: String
    ): String =
        normalizeText(
            value
        )
            .replace(
                " ",
                ""
            )

    private fun normalizeLookupKey(
        value: String
    ): String =
        normalizeText(
            value
        )

    private fun normalizeText(
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
                COMBINING_MARKS_REGEX,
                ""
            )
            .replace(
                "ß",
                "ss"
            )
            .replace(
                NON_ALPHANUMERIC_REGEX,
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()
    }

    private fun jsonArray(
        values: List<String>
    ): JsonArray =
        JsonArray().apply {
            values.forEach(::add)
        }

    private fun stringList(
        json: JsonObject,
        key: String
    ): List<String> {

        val element =
            json.get(key)
                ?: return emptyList()

        if (
            element.isJsonNull ||
            !element.isJsonArray
        ) {
            return emptyList()
        }

        return element
            .asJsonArray
            .mapNotNull { value ->

                value
                    .takeIf {
                        it.isJsonPrimitive
                    }
                    ?.asString
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
            }
    }

    private fun optionalString(
        json: JsonObject,
        key: String
    ): String? {

        val value =
            json.get(key)
                ?: return null

        if (
            value.isJsonNull ||
            !value.isJsonPrimitive
        ) {
            return null
        }

        return value
            .asString
            .trim()
            .takeIf {
                it.isNotBlank()
            }
    }

    private fun requireString(
        json: JsonObject,
        key: String
    ): String {

        return requireNotNull(
            optionalString(
                json,
                key
            )
        ) {
            "Missing or blank '$key' in catalog entry: $json"
        }
    }

    private data class EntryRepairResult(
        val entry: JsonObject,

        val pluralChanged: Boolean,
        val colloquialChanged: Boolean,
        val phoneticChanged: Boolean,
        val autocompleteChanged: Boolean,

        val removedVariantColloquialCount: Int,
        val preservedColloquialAliasCount: Int,

        val familyPluralResolved: Boolean,
        val familyPluralFallback: Boolean
    )

    private data class PluralResolution(
        val value: String,
        val familyPluralResolved: Boolean,
        val familyPluralFallback: Boolean
    )

    private data class ColloquialResolution(
        val values: List<String>,
        val removedVariantCount: Int
    )

    private companion object {

        const val VARIANT_SEPARATOR =
            " – "

        const val MIN_PHONETIC_TOKEN_LENGTH =
            3

        const val MIN_AUTOCOMPLETE_TOKEN_LENGTH =
            2

        val COMBINING_MARKS_REGEX =
            Regex("\\p{M}+")

        val NON_ALPHANUMERIC_REGEX =
            Regex("[^a-z0-9]+")

        val WHITESPACE_REGEX =
            Regex("\\s+")
    }
}