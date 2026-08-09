
package de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry

class ProductFamilyVariantCompatibilityAnalyzer {

    fun analyze(
        catalog: JsonArray,
        inputFile: String
    ): ProductFamilyVariantCompatibilityAuditReport {

        val occurrences =
            catalog
                .mapNotNull { element ->
                    element
                        .takeIf { it.isJsonObject }
                        ?.asJsonObject
                }
                .flatMap(::analyzeEntry)
                .sortedWith(
                    compareBy<ProductFamilyVariantCompatibilityOccurrence>(
                        { it.category },
                        { it.normalizedFamily },
                        { it.normalizedItem },
                        { it.variantCanonicalKey }
                    )
                )

        val variantBearingEntryCount =
            occurrences
                .map { it.normalizedItem }
                .distinct()
                .size

        val allowed =
            occurrences.filter {
                it.decision ==
                        ProductFamilyVariantCompatibilityDecision.ALLOW
            }

        val rejected =
            occurrences.filter {
                it.decision ==
                        ProductFamilyVariantCompatibilityDecision.REJECT
            }

        val review =
            occurrences.filter {
                it.decision ==
                        ProductFamilyVariantCompatibilityDecision.REVIEW
            }

        val allowedEntries =
            allowed
                .map { it.normalizedItem }
                .toSet()

        val rejectedEntries =
            rejected
                .map { it.normalizedItem }
                .toSet()

        val reviewEntries =
            review
                .map { it.normalizedItem }
                .toSet()

        val countsByDecision =
            occurrences
                .groupingBy {
                    it.decision.name
                }
                .eachCount()
                .toSortedMap()

        val countsByReason =
            occurrences
                .groupingBy {
                    it.reason.name
                }
                .eachCount()
                .toSortedMap()

        val rejectedCountsByFamily =
            rejected
                .groupingBy {
                    it.family
                }
                .eachCount()
                .toList()
                .sortedWith(
                    compareByDescending<Pair<String, Int>> {
                        it.second
                    }
                        .thenBy {
                            it.first
                        }
                )
                .toMap()

        val rejectedCountsByVariant =
            rejected
                .groupingBy {
                    it.variantCanonicalKey
                }
                .eachCount()
                .toList()
                .sortedWith(
                    compareByDescending<Pair<String, Int>> {
                        it.second
                    }
                        .thenBy {
                            it.first
                        }
                )
                .toMap()

        val reviewGaps =
            review
                .groupBy {
                    Triple(
                        it.normalizedFamily,
                        it.family,
                        it.category
                    )
                }
                .map { (key, values) ->

                    ProductFamilyVariantCompatibilityGap(
                        family = key.second,
                        normalizedFamily = key.first,
                        category = key.third,
                        occurrenceCount = values.size,
                        variantKeys =
                            values
                                .map {
                                    it.variantCanonicalKey
                                }
                                .distinct()
                                .sorted(),
                        exampleItems =
                            values
                                .map {
                                    it.itemName
                                }
                                .distinct()
                                .sorted()
                                .take(
                                    MAX_EXAMPLES_PER_GAP
                                )
                    )
                }
                .sortedWith(
                    compareByDescending<ProductFamilyVariantCompatibilityGap> {
                        it.occurrenceCount
                    }
                        .thenBy {
                            it.category
                        }
                        .thenBy {
                            it.normalizedFamily
                        }
                )

        return ProductFamilyVariantCompatibilityAuditReport(
            schemaVersion = 1,
            inputFile = inputFile,

            catalogEntryCount =
                catalog.size(),

            variantBearingEntryCount =
                variantBearingEntryCount,

            variantOccurrenceCount =
                occurrences.size,

            allowOccurrenceCount =
                allowed.size,

            rejectOccurrenceCount =
                rejected.size,

            reviewOccurrenceCount =
                review.size,

            allowedEntryCount =
                allowedEntries.size,

            rejectedEntryCount =
                rejectedEntries.size,

            reviewEntryCount =
                reviewEntries.size,

            countsByDecision =
                countsByDecision,

            countsByReason =
                countsByReason,

            rejectedCountsByFamily =
                rejectedCountsByFamily,

            rejectedCountsByVariant =
                rejectedCountsByVariant,

            reviewGaps =
                reviewGaps,

            occurrences =
                occurrences
        )
    }

    private fun analyzeEntry(
        json: JsonObject
    ): List<ProductFamilyVariantCompatibilityOccurrence> {

        val itemName =
            json.string("itemname")
                ?: return emptyList()

        val normalizedItem =
            json.string("normalized")
                ?: return emptyList()

        val category =
            json.string("category")
                ?: return emptyList()

        val parts =
            itemName
                .split(VARIANT_SEPARATOR)
                .map(String::trim)
                .filter(String::isNotBlank)

        if (parts.size < 2) {
            return emptyList()
        }

        val family =
            parts.first()

        return parts
            .drop(1)
            .mapNotNull { rawVariant ->

                val definition =
                    SemanticVariantTaxonomyRegistry
                        .definitionFor(rawVariant)
                        ?: return@mapNotNull null

                val compatibility =
                    ProductFamilyVariantCompatibilityRegistry
                        .evaluate(
                            family = family,
                            category = category,
                            variant = definition
                        )

                ProductFamilyVariantCompatibilityOccurrence(
                    itemName =
                        itemName,
                    normalizedItem =
                        normalizedItem,
                    category =
                        category,
                    family =
                        family,
                    normalizedFamily =
                        compatibility.normalizedFamily,
                    variantRawValue =
                        rawVariant,
                    variantCanonicalKey =
                        definition.canonicalKey,
                    variantType =
                        definition.type,
                    decision =
                        compatibility.decision,
                    reason =
                        compatibility.reason
                )
            }
    }

    private fun JsonObject.string(
        key: String
    ): String? {

        val value =
            get(key)
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
            .takeIf(String::isNotBlank)
    }

    private companion object {

        const val VARIANT_SEPARATOR =
            " – "

        const val MAX_EXAMPLES_PER_GAP =
            5
    }
}