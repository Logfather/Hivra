package de.shopme.testing.system.tools.knowledge.catalog.semantic.combination

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry

class SemanticCombinationConstraintAnalyzer {

    fun analyze(
        catalog: JsonArray,
        inputFile: String
    ): SemanticCombinationConstraintAuditReport {

        val entries =
            catalog
                .mapNotNull { element ->
                    element
                        .takeIf { it.isJsonObject }
                        ?.asJsonObject
                }
                .mapNotNull(::analyzeEntry)
                .sortedWith(
                    compareBy<SemanticCombinationAuditEntry>(
                        { it.category },
                        { it.normalizedFamily },
                        { it.normalizedItem }
                    )
                )

        val allowed =
            entries.filter {
                it.decision ==
                        SemanticCombinationDecision.ALLOW
            }

        val rejected =
            entries.filter {
                it.decision ==
                        SemanticCombinationDecision.REJECT
            }

        val review =
            entries.filter {
                it.decision ==
                        SemanticCombinationDecision.REVIEW
            }

        val countsByDecision =
            entries
                .groupingBy {
                    it.decision.name
                }
                .eachCount()
                .toSortedMap()

        val countsByReason =
            entries
                .groupingBy {
                    it.reason.name
                }
                .eachCount()
                .toSortedMap()

        val countsByProfile =
            entries
                .groupingBy {
                    it.profile.name
                }
                .eachCount()
                .toSortedMap()

        val rejectedCombinationSignatures =
            rejected
                .groupingBy {
                    signature(
                        it.variantTypes
                    )
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
                .groupBy { entry ->
                    Pair(
                        entry.profile,
                        signature(
                            entry.variantTypes
                        )
                    )
                }
                .map { (key, values) ->
                    SemanticCombinationReviewGap(
                        profile = key.first,
                        typeSignature = key.second,
                        occurrenceCount = values.size,
                        exampleItems =
                            values
                                .map { it.itemName }
                                .distinct()
                                .sorted()
                                .take(MAX_EXAMPLES)
                    )
                }
                .sortedWith(
                    compareByDescending<SemanticCombinationReviewGap> {
                        it.occurrenceCount
                    }
                        .thenBy {
                            it.profile.name
                        }
                        .thenBy {
                            it.typeSignature
                        }
                )

        return SemanticCombinationConstraintAuditReport(
            schemaVersion = 1,
            inputFile = inputFile,
            catalogEntryCount = catalog.size(),

            combinationEntryCount =
                entries.size,

            combinationVariantOccurrenceCount =
                entries.sumOf {
                    it.variantKeys.size
                },

            allowEntryCount =
                allowed.size,

            rejectEntryCount =
                rejected.size,

            reviewEntryCount =
                review.size,

            countsByDecision =
                countsByDecision,

            countsByReason =
                countsByReason,

            countsByProfile =
                countsByProfile,

            rejectedCombinationSignatures =
                rejectedCombinationSignatures,

            reviewGaps =
                reviewGaps,

            entries =
                entries
        )
    }

    private fun analyzeEntry(
        json: JsonObject
    ): SemanticCombinationAuditEntry? {

        val itemName =
            json.string("itemname")
                ?: return null

        val normalizedItem =
            json.string("normalized")
                ?: return null

        val category =
            json.string("category")
                ?: return null

        val parts =
            itemName
                .split(VARIANT_SEPARATOR)
                .map(String::trim)
                .filter(String::isNotBlank)

        /*
         * Family + mindestens zwei Variants.
         */
        if (parts.size < 3) {
            return null
        }

        val family =
            parts.first()

        val definitions =
            parts
                .drop(1)
                .map { rawVariant ->
                    requireNotNull(
                        SemanticVariantTaxonomyRegistry
                            .definitionFor(rawVariant)
                    ) {
                        "Missing semantic variant taxonomy for " +
                                "'$rawVariant' in '$itemName'."
                    }
                }

        val result =
            SemanticCombinationConstraintRegistry
                .evaluate(
                    family = family,
                    category = category,
                    variants = definitions
                )

        return SemanticCombinationAuditEntry(
            itemName = itemName,
            normalizedItem = normalizedItem,
            category = category,
            family = family,
            normalizedFamily =
                result.normalizedFamily,
            profile =
                result.profile,
            variantKeys =
                definitions
                    .map { it.canonicalKey },
            variantTypes =
                definitions
                    .map { it.type.name },
            decision =
                result.decision,
            reason =
                result.reason
        )
    }

    private fun signature(
        variantTypes: List<String>
    ): String =
        variantTypes
            .sorted()
            .joinToString(" + ")

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

        const val MAX_EXAMPLES =
            5
    }
}