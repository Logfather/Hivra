package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility

import com.google.gson.JsonArray

class MarketPlausibilityAnalyzer {

    fun analyze(
        catalog: JsonArray,
        inputFile: String
    ): MarketPlausibilityAuditReport {

        val validator =
            MarketPlausibilityValidator()

        val results =
            catalog
                .mapNotNull { element ->
                    element
                        .takeIf { it.isJsonObject }
                        ?.asJsonObject
                }
                .mapNotNull { json ->
                    validator.validate(json)
                }
                .sortedWith(
                    compareBy<MarketPlausibilityResult>(
                        { it.category },
                        { it.family },
                        { it.normalizedItem }
                    )
                )

        val accepted =
            results.filter {
                it.decision ==
                        MarketPlausibilityDecision.ACCEPT
            }

        val rejected =
            results.filter {
                it.decision ==
                        MarketPlausibilityDecision.REJECT
            }

        val review =
            results.filter {
                it.decision ==
                        MarketPlausibilityDecision.REVIEW
            }

        val countsByReason =
            results
                .groupingBy {
                    it.reason.name
                }
                .eachCount()
                .toSortedMap()

        val rejectedCountsByCategory =
            rejected
                .groupingBy {
                    it.category
                }
                .eachCount()
                .toSortedMap()

        val reviewCountsByCategory =
            review
                .groupingBy {
                    it.category
                }
                .eachCount()
                .toSortedMap()

        val reviewGaps =
            review
                .groupBy {
                    Triple(
                        it.family,
                        it.category,
                        it.identityVariantKeys
                    )
                }
                .map { (key, values) ->

                    MarketPlausibilityReviewGap(
                        family = key.first,
                        category = key.second,
                        occurrenceCount = values.size,
                        variantKeys =
                            key.third.sorted(),
                        exampleItems =
                            values
                                .map { it.itemName }
                                .distinct()
                                .sorted()
                                .take(MAX_EXAMPLES)
                    )
                }
                .sortedWith(
                    compareByDescending<MarketPlausibilityReviewGap> {
                        it.occurrenceCount
                    }
                        .thenBy {
                            it.category
                        }
                        .thenBy {
                            it.family
                        }
                )

        return MarketPlausibilityAuditReport(
            schemaVersion = 1,
            inputFile = inputFile,

            catalogEntryCount =
                catalog.size(),

            acceptedEntryCount =
                accepted.size,

            rejectedEntryCount =
                rejected.size,

            reviewEntryCount =
                review.size,

            countsByReason =
                countsByReason,

            rejectedCountsByCategory =
                rejectedCountsByCategory,

            reviewCountsByCategory =
                reviewCountsByCategory,

            reviewGaps =
                reviewGaps,

            results =
                results
        )
    }

    private companion object {

        const val MAX_EXAMPLES =
            5
    }
}