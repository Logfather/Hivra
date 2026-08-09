package de.shopme.testing.system.tools.knowledge.catalog.semantic.variant

import com.google.gson.JsonArray
import com.google.gson.JsonObject

class SemanticVariantTaxonomyAnalyzer {

    fun analyze(
        catalog: JsonArray,
        inputFile: String
    ): SemanticVariantTaxonomyAuditReport {

        val entries =
            catalog
                .mapNotNull { element ->
                    element
                        .takeIf { it.isJsonObject }
                        ?.asJsonObject
                }

        val occurrences =
            entries.flatMap(::extractOccurrences)

        val groupedByVariant =
            occurrences
                .groupBy {
                    it.normalizedLookupKey
                }
                .toSortedMap()

        val distinctVariantCount =
            groupedByVariant.size

        val classifiedGroups =
            groupedByVariant.filterValues { values ->
                values.any {
                    it.canonicalKey != null
                }
            }

        val unknownGroups =
            groupedByVariant.filterValues { values ->
                values.all {
                    it.canonicalKey == null
                }
            }

        val unknownVariants =
            unknownGroups
                .map { (_, values) ->

                    val first =
                        values.first()

                    UnknownSemanticVariant(
                        rawValue = first.rawValue,
                        normalizedLookupKey =
                            first.normalizedLookupKey,
                        occurrenceCount = values.size,
                        categories =
                            values
                                .map { it.category }
                                .distinct()
                                .sorted(),
                        exampleItems =
                            values
                                .map { it.itemName }
                                .distinct()
                                .sorted()
                                .take(MAX_EXAMPLES_PER_UNKNOWN_VARIANT)
                    )
                }
                .sortedWith(
                    compareByDescending<UnknownSemanticVariant> {
                        it.occurrenceCount
                    }
                        .thenBy {
                            it.normalizedLookupKey
                        }
                )

        val countsByType =
            occurrences
                .mapNotNull { it.type }
                .groupingBy { it.name }
                .eachCount()
                .toSortedMap()

        val variantBearingEntryCount =
            occurrences
                .map { it.normalizedItem }
                .distinct()
                .size

        val classifiedDistinctVariantCount =
            classifiedGroups.size

        val classificationCoverage =
            if (distinctVariantCount == 0) {
                1.0
            } else {
                classifiedDistinctVariantCount
                    .toDouble() /
                        distinctVariantCount.toDouble()
            }

        return SemanticVariantTaxonomyAuditReport(
            schemaVersion = 1,
            inputFile = inputFile,
            catalogEntryCount = catalog.size(),
            variantBearingEntryCount =
                variantBearingEntryCount,
            variantOccurrenceCount =
                occurrences.size,
            distinctVariantCount =
                distinctVariantCount,
            classifiedDistinctVariantCount =
                classifiedDistinctVariantCount,
            unknownDistinctVariantCount =
                unknownGroups.size,
            classificationCoverage =
                classificationCoverage,
            countsByType =
                countsByType,
            unknownVariants =
                unknownVariants
        )
    }

    private fun extractOccurrences(
        json: JsonObject
    ): List<SemanticVariantOccurrence> {

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

        return parts
            .drop(1)
            .map { rawVariant ->

                val lookupKey =
                    SemanticVariantTaxonomyRegistry
                        .normalizeLookupKey(rawVariant)

                val definition =
                    SemanticVariantTaxonomyRegistry
                        .definitionFor(rawVariant)

                SemanticVariantOccurrence(
                    rawValue = rawVariant,
                    normalizedLookupKey = lookupKey,
                    canonicalKey =
                        definition?.canonicalKey,
                    type =
                        definition?.type,
                    itemName = itemName,
                    normalizedItem = normalizedItem,
                    category = category
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

        const val MAX_EXAMPLES_PER_UNKNOWN_VARIANT =
            5
    }
}