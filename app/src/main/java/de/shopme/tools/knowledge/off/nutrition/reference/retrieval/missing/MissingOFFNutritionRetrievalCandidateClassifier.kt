package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.missing

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalRequest
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.OFFNutritionRetrievalTermExpander
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.OFFNutritionRetrievalTextNormalizer

class MissingOFFNutritionRetrievalCandidateClassifier(
    private val termExpander: OFFNutritionRetrievalTermExpander =
        OFFNutritionRetrievalTermExpander()
) {

    fun classify(
        requests: List<CatalogOFFNutritionRetrievalRequest>,
        sourceAliases: Set<String>
    ): MissingOFFNutritionRetrievalCandidateReport {

        val normalizedSourceAliases =
            sourceAliases
                .asSequence()
                .map(
                    OFFNutritionRetrievalTextNormalizer::normalize
                )
                .filter(String::isNotBlank)
                .toSortedSet()

        val missingRequests =
            requests
                .filter { request ->
                    request.candidates.isEmpty()
                }
                .sortedBy { request ->
                    request.catalogIndex
                }

        val findings =
            missingRequests.map { request ->
                classifyRequest(
                    request = request,
                    sourceAliases = normalizedSourceAliases
                )
            }

        val countsByType =
            MissingOFFNutritionRetrievalCandidateType.entries
                .associateWith { type ->
                    findings.count { finding ->
                        finding.primaryType == type
                    }
                }
                .filterValues { count ->
                    count > 0
                }

        return MissingOFFNutritionRetrievalCandidateReport(
            version =
                MissingOFFNutritionRetrievalCandidateReport.CURRENT_VERSION,
            requestCount =
                requests.size,
            missingRequestCount =
                findings.size,
            countsByType =
                countsByType,
            findings =
                findings
        )
    }

    private fun classifyRequest(
        request: CatalogOFFNutritionRetrievalRequest,
        sourceAliases: Set<String>
    ): MissingOFFNutritionRetrievalCandidateFinding {

        val normalizedTerms =
            request.catalogTerms
                .asSequence()
                .map(
                    OFFNutritionRetrievalTextNormalizer::normalize
                )
                .filter(String::isNotBlank)
                .toSortedSet()

        val expandedTerms =
            termExpander
                .expand(
                    terms = request.catalogTerms
                )
                .toSet()

        val directAliasMatches =
            sourceAliases
                .filter { alias ->
                    alias in normalizedTerms
                }
                .take(MAXIMUM_RELATED_ALIASES)

        if (directAliasMatches.isNotEmpty()) {
            return finding(
                request = request,
                type =
                    MissingOFFNutritionRetrievalCandidateType.ALIAS_NOT_INDEXED,
                relatedAliases =
                    directAliasMatches,
                reason =
                    "A catalog retrieval term exists as a source alias but was not retrieved."
            )
        }

        val inflectionMatches =
            sourceAliases
                .filter { alias ->
                    alias in expandedTerms &&
                            alias !in normalizedTerms
                }
                .take(MAXIMUM_RELATED_ALIASES)

        if (inflectionMatches.isNotEmpty()) {
            return finding(
                request = request,
                type =
                    MissingOFFNutritionRetrievalCandidateType.SINGULAR_PLURAL_MISMATCH,
                relatedAliases =
                    inflectionMatches,
                reason =
                    "A singular or plural retrieval variant exists in the source aliases."
            )
        }

        val normalizedEnglish =
            OFFNutritionRetrievalTextNormalizer
                .normalize(
                    request.normalizedEnglish
                )

        val compactNormalizedEnglish =
            normalizedEnglish
                .replace(
                    " ",
                    ""
                )

        val normalizationMatches =
            sourceAliases
                .filter { alias ->
                    alias.replace(" ", "") ==
                            compactNormalizedEnglish
                }
                .take(MAXIMUM_RELATED_ALIASES)

        if (normalizationMatches.isNotEmpty()) {
            return finding(
                request = request,
                type =
                    MissingOFFNutritionRetrievalCandidateType.NORMALIZATION_MISMATCH,
                relatedAliases =
                    normalizationMatches,
                reason =
                    "A source alias matches after removing token boundaries."
            )
        }

        val catalogCoreTokens =
            normalizedTerms
                .flatMap(::tokens)
                .filterNot(GENERIC_TOKENS::contains)
                .toSet()

        val compoundMatches =
            sourceAliases
                .asSequence()
                .map { alias ->
                    alias to
                            tokens(alias)
                                .filterNot(GENERIC_TOKENS::contains)
                                .toSet()
                }
                .filter { (_, aliasTokens) ->
                    catalogCoreTokens.isNotEmpty() &&
                            catalogCoreTokens
                                .intersect(aliasTokens)
                                .size >=
                            minimumCompoundIntersection(
                                catalogCoreTokens.size
                            )
                }
                .sortedWith(
                    compareByDescending<Pair<String, Set<String>>> {
                        catalogCoreTokens.intersect(it.second).size
                    }
                        .thenBy { pair ->
                            pair.first
                        }
                )
                .map(Pair<String, Set<String>>::first)
                .take(MAXIMUM_RELATED_ALIASES)
                .toList()

        if (compoundMatches.isNotEmpty()) {
            return finding(
                request = request,
                type =
                    MissingOFFNutritionRetrievalCandidateType.COMPOUND_TERM_MISMATCH,
                relatedAliases =
                    compoundMatches,
                reason =
                    "Related source aliases share the compound term's core tokens."
            )
        }

        if (
            request.category == "Gemüse" &&
            normalizedEnglish in NON_FOOD_TERMS
        ) {
            return finding(
                request = request,
                type =
                    MissingOFFNutritionRetrievalCandidateType.NON_FOOD_OR_CATALOG_ANOMALY,
                relatedAliases =
                    emptyList(),
                reason =
                    "The catalog term appears to describe a non-food item or catalog anomaly."
            )
        }

        if (
            normalizedEnglish in
            BRAND_OR_REGIONAL_TERMS
        ) {
            return finding(
                request = request,
                type =
                    MissingOFFNutritionRetrievalCandidateType.BRAND_OR_REGIONAL_TERM,
                relatedAliases =
                    emptyList(),
                reason =
                    "The catalog term is brand-specific, regional or language-specific."
            )
        }

        val looseMatches =
            sourceAliases
                .filter { alias ->
                    catalogCoreTokens
                        .intersect(
                            tokens(alias).toSet()
                        )
                        .isNotEmpty()
                }
                .take(MAXIMUM_RELATED_ALIASES)

        if (looseMatches.isEmpty()) {
            return finding(
                request = request,
                type =
                    MissingOFFNutritionRetrievalCandidateType.SOURCE_TERM_MISSING,
                relatedAliases =
                    emptyList(),
                reason =
                    "No related normalized source alias was found."
            )
        }

        return finding(
            request = request,
            type =
                MissingOFFNutritionRetrievalCandidateType.UNKNOWN,
            relatedAliases =
                looseMatches,
            reason =
                "Related aliases exist, but no deterministic failure type applies."
        )
    }

    private fun finding(
        request: CatalogOFFNutritionRetrievalRequest,
        type: MissingOFFNutritionRetrievalCandidateType,
        relatedAliases: List<String>,
        reason: String
    ): MissingOFFNutritionRetrievalCandidateFinding {

        return MissingOFFNutritionRetrievalCandidateFinding(
            catalogIndex =
                request.catalogIndex,
            catalogKey =
                request.catalogKey,
            normalizedEnglish =
                request.normalizedEnglish,
            itemName =
                request.itemName,
            category =
                request.category,
            production =
                request.production,
            primaryType =
                type,
            relatedSourceAliases =
                relatedAliases
                    .distinct()
                    .sorted(),
            reasons =
                listOf(reason)
                    .distinct()
                    .sorted()
        )
    }

    private fun tokens(
        value: String
    ): List<String> {

        return OFFNutritionRetrievalTextNormalizer
            .normalize(value)
            .split(' ')
            .filter(String::isNotBlank)
    }

    private fun minimumCompoundIntersection(
        catalogTokenCount: Int
    ): Int {

        return when {
            catalogTokenCount <= 1 -> 1
            catalogTokenCount == 2 -> 1
            else -> 2
        }
    }

    companion object {

        private const val MAXIMUM_RELATED_ALIASES =
            10

        private val GENERIC_TOKENS =
            setOf(
                "bio",
                "canned",
                "fresh",
                "frozen",
                "organic",
                "plain",
                "prepared",
                "raw",
                "ready",
                "standard"
            )

        private val NON_FOOD_TERMS =
            setOf(
                "tulip bulbs"
            )

        private val BRAND_OR_REGIONAL_TERMS =
            setOf(
                "ajvar",
                "maultaschen",
                "orangina",
                "radler",
                "teewurst",
                "toffifee"
            )
    }
}