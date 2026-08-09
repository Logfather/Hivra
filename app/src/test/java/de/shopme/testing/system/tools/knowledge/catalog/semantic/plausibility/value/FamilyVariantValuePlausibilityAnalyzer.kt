package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.value

import com.google.gson.JsonObject
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.SemanticVariantIdentityDecision
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.SemanticVariantIdentityPolicy
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry

class FamilyVariantValuePlausibilityAnalyzer {

    fun analyze(
        catalog: List<JsonObject>
    ): FamilyVariantValuePlausibilityAuditReport {

        val occurrences =
            catalog
                .flatMap(::analyzeEntry)
                .sortedWith(
                    compareBy<FamilyVariantValuePlausibilityOccurrence>(
                        { it.category },
                        { it.family },
                        { it.variantCanonicalKey },
                        { it.normalizedItem }
                    )
                )

        val allowed =
            occurrences.filter {
                it.decision ==
                        FamilyVariantValuePlausibilityDecision.ALLOW
            }

        val rejected =
            occurrences.filter {
                it.decision ==
                        FamilyVariantValuePlausibilityDecision.REJECT
            }

        val review =
            occurrences.filter {
                it.decision ==
                        FamilyVariantValuePlausibilityDecision.REVIEW
            }

        val rejectedValues =
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
                    Pair(
                        it.family,
                        it.variantCanonicalKey
                    )
                }
                .map { (key, values) ->

                    FamilyVariantValuePlausibilityGap(
                        family =
                            key.first,
                        variantCanonicalKey =
                            key.second,
                        occurrenceCount =
                            values.size,
                        exampleItems =
                            values
                                .map {
                                    it.itemName
                                }
                                .distinct()
                                .sorted()
                                .take(
                                    MAX_EXAMPLES
                                )
                    )
                }
                .sortedWith(
                    compareByDescending<FamilyVariantValuePlausibilityGap> {
                        it.occurrenceCount
                    }
                        .thenBy {
                            it.family
                        }
                        .thenBy {
                            it.variantCanonicalKey
                        }
                )

        return FamilyVariantValuePlausibilityAuditReport(
            schemaVersion = 1,

            inputEntryCount =
                catalog.size,

            variantBearingEntryCount =
                occurrences
                    .map {
                        it.normalizedItem
                    }
                    .distinct()
                    .size,

            variantOccurrenceCount =
                occurrences.size,

            allowOccurrenceCount =
                allowed.size,

            rejectOccurrenceCount =
                rejected.size,

            reviewOccurrenceCount =
                review.size,

            affectedRejectEntryCount =
                rejected
                    .map {
                        it.normalizedItem
                    }
                    .distinct()
                    .size,

            affectedReviewEntryCount =
                review
                    .map {
                        it.normalizedItem
                    }
                    .distinct()
                    .size,

            countsByReason =
                occurrences
                    .groupingBy {
                        it.reason.name
                    }
                    .eachCount()
                    .toSortedMap(),

            rejectedValues =
                rejectedValues,

            reviewGaps =
                reviewGaps,

            occurrences =
                occurrences
        )
    }

    private fun analyzeEntry(
        entry: JsonObject
    ): List<FamilyVariantValuePlausibilityOccurrence> {

        val itemName =
            requireString(
                entry,
                "itemname"
            )

        val normalized =
            requireString(
                entry,
                "normalized"
            )

        val category =
            requireString(
                entry,
                "category"
            )

        val parts =
            itemName
                .split(
                    VARIANT_SEPARATOR
                )
                .map(
                    String::trim
                )
                .filter(
                    String::isNotBlank
                )

        if (parts.size < 2) {
            return emptyList()
        }

        val family =
            parts.first()

        return parts
            .drop(1)
            .mapNotNull { rawVariant ->

                val definition =
                    requireNotNull(
                        SemanticVariantTaxonomyRegistry
                            .definitionFor(
                                rawVariant
                            )
                    ) {
                        "Missing semantic variant definition for " +
                                "'$rawVariant' in '$itemName'."
                    }

                /*
                 * Knowledge-only Values werden hier nicht erneut
                 * bewertet. Dieser Audit behandelt ausschließlich
                 * produktidentitätsrelevante Values.
                 */
                if (
                    SemanticVariantIdentityPolicy
                        .evaluate(
                            definition.type
                        )
                        .decision ==
                    SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
                ) {
                    return@mapNotNull null
                }

                val result =
                    FamilyVariantValuePlausibilityRegistry
                        .evaluate(
                            family = family,
                            category = category,
                            variant = definition
                        )

                FamilyVariantValuePlausibilityOccurrence(
                    itemName =
                        itemName,
                    normalizedItem =
                        normalized,
                    category =
                        category,
                    family =
                        family,

                    variantDisplayName =
                        definition.displayName,
                    variantCanonicalKey =
                        definition.canonicalKey,

                    decision =
                        result.decision,
                    reason =
                        result.reason
                )
            }
    }

    private fun requireString(
        json: JsonObject,
        key: String
    ): String =
        requireNotNull(
            json
                .get(key)
                ?.takeIf {
                    it.isJsonPrimitive
                }
                ?.asString
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
        ) {
            "Missing or blank '$key' in catalog entry: $json"
        }

    private companion object {

        const val VARIANT_SEPARATOR =
            " – "

        const val MAX_EXAMPLES =
            5
    }
}