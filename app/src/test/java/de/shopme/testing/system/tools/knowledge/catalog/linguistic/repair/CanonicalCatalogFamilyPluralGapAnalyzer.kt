package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

import com.google.gson.JsonObject
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution.CanonicalProductFamilyIdentityPolicy
import java.text.Normalizer

class CanonicalCatalogFamilyPluralGapAnalyzer {

    fun analyze(
        catalog: List<JsonObject>
    ): CanonicalCatalogFamilyPluralGapAuditReport {

        val familyPluralByName =
            buildFamilyPluralIndex(
                catalog
            )

        val variantEntries =
            catalog
                .mapNotNull { entry ->

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
                        return@mapNotNull null
                    }

                    val family =
                        parts.first()

                    val identityPlural =
                        CanonicalProductFamilyIdentityPolicy
                            .canonicalPlural(
                                family
                            )

                    val policyPlural =
                        CanonicalProductFamilyPluralPolicy
                            .pluralFor(
                                family
                            )

                    val baselinePlural =
                        familyPluralByName[
                            normalizeLookupKey(
                                family
                            )
                        ]

                    VariantEntry(
                        itemName = itemName,
                        family = family,
                        pluralResolved =
                            !identityPlural.isNullOrBlank() ||
                                    !policyPlural.isNullOrBlank() ||
                                    !baselinePlural.isNullOrBlank()
                    )
                }

        val resolvedCount =
            variantEntries.count {
                it.pluralResolved
            }

        val fallbackEntries =
            variantEntries.filterNot {
                it.pluralResolved
            }

        val gaps =
            fallbackEntries
                .groupBy {
                    normalizeLookupKey(
                        it.family
                    )
                }
                .map { (normalizedFamily, entries) ->

                    CanonicalCatalogFamilyPluralGap(
                        family =
                            entries
                                .map { it.family }
                                .distinct()
                                .sorted()
                                .first(),

                        normalizedFamily =
                            normalizedFamily,

                        occurrenceCount =
                            entries.size,

                        exampleItems =
                            entries
                                .map {
                                    it.itemName
                                }
                                .distinct()
                                .sorted()
                                .take(MAX_EXAMPLES)
                    )
                }
                .sortedWith(
                    compareByDescending<CanonicalCatalogFamilyPluralGap> {
                        it.occurrenceCount
                    }
                        .thenBy {
                            it.normalizedFamily
                        }
                )

        return CanonicalCatalogFamilyPluralGapAuditReport(
            schemaVersion = 1,

            inputEntryCount =
                catalog.size,

            variantEntryCount =
                variantEntries.size,

            familyPluralResolvedCount =
                resolvedCount,

            familyPluralFallbackCount =
                fallbackEntries.size,

            distinctFallbackFamilyCount =
                gaps.size,

            gaps =
                gaps
        )
    }

    private fun buildFamilyPluralIndex(
        catalog: List<JsonObject>
    ): Map<String, String> {

        return catalog
            .filter { entry ->

                val itemName =
                    requireString(
                        entry,
                        "itemname"
                    )

                VARIANT_SEPARATOR !in
                        itemName
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

    private fun normalizeLookupKey(
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
    ): String =
        requireNotNull(
            optionalString(
                json,
                key
            )
        ) {
            "Missing or blank '$key' in catalog entry: $json"
        }

    private data class VariantEntry(
        val itemName: String,
        val family: String,
        val pluralResolved: Boolean
    )

    private companion object {

        const val VARIANT_SEPARATOR =
            " – "

        const val MAX_EXAMPLES =
            5

        val COMBINING_MARKS_REGEX =
            Regex("\\p{M}+")

        val NON_ALPHANUMERIC_REGEX =
            Regex("[^a-z0-9]+")

        val WHITESPACE_REGEX =
            Regex("\\s+")
    }
}