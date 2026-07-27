package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalRequest
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.OFFNutritionRetrievalTextNormalizer

class OFFNutritionSourceCoverageDiagnoser {

    fun diagnose(
        allRequests: List<CatalogOFFNutritionRetrievalRequest>,
        rawSourceCoverage: OFFNutritionRawSourceCoverage,
        referenceCandidates: List<OFFNutritionCoverageAliasEntry>,
        referenceAggregates: List<OFFNutritionCoverageAliasEntry>,
        matcherCandidates: List<OFFNutritionCoverageAliasEntry>
    ): OFFNutritionSourceCoverageReport {

        val missingRequests =
            allRequests
                .filter { request ->
                    request.candidates.isEmpty()
                }
                .sortedWith(
                    compareBy<CatalogOFFNutritionRetrievalRequest>(
                        { it.catalogIndex },
                        { it.catalogKey }
                    )
                )

        val findings =
            missingRequests.map { request ->
                diagnoseRequest(
                    request = request,
                    rawCoverage =
                        rawSourceCoverage
                            .entriesByCatalogIndex[
                            request.catalogIndex
                        ]
                            ?: EMPTY_RAW_COVERAGE,
                    referenceCandidates =
                        referenceCandidates,
                    referenceAggregates =
                        referenceAggregates,
                    matcherCandidates =
                        matcherCandidates
                )
            }

        val countsByFirstMissingStage =
            OFFNutritionSourceCoverageStage.entries
                .associateWith { stage ->
                    findings.count { finding ->
                        finding.firstMissingStage == stage
                    }
                }
                .filterValues { count ->
                    count > 0
                }

        return OFFNutritionSourceCoverageReport(
            version =
                OFFNutritionSourceCoverageReport.CURRENT_VERSION,
            requestCount =
                allRequests.size,
            missingRequestCount =
                missingRequests.size,
            rawOFFScannedProductCount =
                rawSourceCoverage.scannedProductCount,
            countsByFirstMissingStage =
                countsByFirstMissingStage,
            findings =
                findings
        )
    }

    private fun diagnoseRequest(
        request: CatalogOFFNutritionRetrievalRequest,
        rawCoverage: OFFNutritionRawSourceCoverageEntry,
        referenceCandidates: List<OFFNutritionCoverageAliasEntry>,
        referenceAggregates: List<OFFNutritionCoverageAliasEntry>,
        matcherCandidates: List<OFFNutritionCoverageAliasEntry>
    ): OFFNutritionSourceCoverageFinding {

        val retrievalTerms =
            buildSet {
                add(request.catalogKey)
                add(request.normalizedEnglish)
                addAll(request.catalogTerms)
            }
                .asSequence()
                .map(
                    OFFNutritionRetrievalTextNormalizer::normalize
                )
                .filter(String::isNotBlank)
                .toSortedSet()

        val matchingReferenceCandidates =
            findMatchingEntries(
                retrievalTerms = retrievalTerms,
                entries = referenceCandidates
            )

        val matchingReferenceAggregates =
            findMatchingEntries(
                retrievalTerms = retrievalTerms,
                entries = referenceAggregates
            )

        val matchingMatcherCandidates =
            findMatchingEntries(
                retrievalTerms = retrievalTerms,
                entries = matcherCandidates
            )

        val firstMissingStage =
            determineFirstMissingStage(
                rawCoverage = rawCoverage,
                referenceCandidateCount =
                    matchingReferenceCandidates.size,
                referenceAggregateCount =
                    matchingReferenceAggregates.size,
                matcherCandidateCount =
                    matchingMatcherCandidates.size
            )

        return OFFNutritionSourceCoverageFinding(
            catalogIndex =
                request.catalogIndex,
            catalogKey =
                request.catalogKey,
            normalizedEnglish =
                request.normalizedEnglish,
            retrievalTerms =
                retrievalTerms.toList(),

            rawOFFProductMatchCount =
                rawCoverage.productMatchCount,
            rawOFFProductWithAnyNutritionCount =
                rawCoverage.productWithAnyNutritionCount,
            rawOFFProductWithUsableNutritionCount =
                rawCoverage.productWithUsableNutritionCount,

            referenceCandidateMatchCount =
                matchingReferenceCandidates.size,
            referenceAggregateMatchCount =
                matchingReferenceAggregates.size,
            matcherCandidateMatchCount =
                matchingMatcherCandidates.size,

            firstMissingStage =
                firstMissingStage,

            matchedRawProductNames =
                rawCoverage.matchedProductNames,

            matchedReferenceCandidateAliases =
                matchingReferenceCandidates
                    .flatMap { entry ->
                        entry.aliases
                    }
                    .distinct()
                    .sorted()
                    .take(MAXIMUM_MATCHED_ALIASES),

            matchedReferenceAggregateAliases =
                matchingReferenceAggregates
                    .flatMap { entry ->
                        entry.aliases
                    }
                    .distinct()
                    .sorted()
                    .take(MAXIMUM_MATCHED_ALIASES),

            matchedMatcherCandidateAliases =
                matchingMatcherCandidates
                    .flatMap { entry ->
                        entry.aliases
                    }
                    .distinct()
                    .sorted()
                    .take(MAXIMUM_MATCHED_ALIASES),

            reasons =
                listOf(
                    reasonFor(
                        stage = firstMissingStage
                    )
                )
        )
    }

    private fun findMatchingEntries(
        retrievalTerms: Set<String>,
        entries: List<OFFNutritionCoverageAliasEntry>
    ): List<OFFNutritionCoverageAliasEntry> {

        return entries
            .filter { entry ->
                entry.aliases.any { alias ->
                    retrievalTerms.any { retrievalTerm ->
                        aliasesMatch(
                            left = retrievalTerm,
                            right = alias
                        )
                    }
                }
            }
            .sortedBy { entry ->
                entry.identity
            }
    }

    private fun aliasesMatch(
        left: String,
        right: String
    ): Boolean {

        if (left == right) {
            return true
        }

        return " $left ".contains(" $right ") ||
                " $right ".contains(" $left ")
    }

    private fun determineFirstMissingStage(
        rawCoverage: OFFNutritionRawSourceCoverageEntry,
        referenceCandidateCount: Int,
        referenceAggregateCount: Int,
        matcherCandidateCount: Int
    ): OFFNutritionSourceCoverageStage {

        return when {
            rawCoverage.productMatchCount == 0 -> {
                OFFNutritionSourceCoverageStage.RAW_OFF_PRODUCT
            }

            rawCoverage.productWithUsableNutritionCount == 0 -> {
                OFFNutritionSourceCoverageStage.RAW_OFF_USABLE_NUTRITION
            }

            referenceCandidateCount == 0 -> {
                OFFNutritionSourceCoverageStage.REFERENCE_CANDIDATE
            }

            referenceAggregateCount == 0 -> {
                OFFNutritionSourceCoverageStage.REFERENCE_AGGREGATE
            }

            matcherCandidateCount == 0 -> {
                OFFNutritionSourceCoverageStage.MATCHER_CANDIDATE
            }

            else -> {
                OFFNutritionSourceCoverageStage.RETRIEVAL_INDEX
            }
        }
    }

    private fun reasonFor(
        stage: OFFNutritionSourceCoverageStage
    ): String {

        return when (stage) {
            OFFNutritionSourceCoverageStage.RAW_OFF_PRODUCT ->
                "No matching product identity was found in the OFF slim dump."

            OFFNutritionSourceCoverageStage.RAW_OFF_USABLE_NUTRITION ->
                "Matching OFF products exist, but none contains usable core nutrition."

            OFFNutritionSourceCoverageStage.REFERENCE_CANDIDATE ->
                "Usable OFF products exist, but no canonical nutrition reference candidate matches."

            OFFNutritionSourceCoverageStage.REFERENCE_AGGREGATE ->
                "A canonical nutrition reference candidate exists, but no validated aggregate matches."

            OFFNutritionSourceCoverageStage.MATCHER_CANDIDATE ->
                "A validated nutrition aggregate exists, but no matcher candidate matches."

            OFFNutritionSourceCoverageStage.RETRIEVAL_INDEX ->
                "A matcher candidate with a matching alias exists, but retrieval returned no candidate."

            OFFNutritionSourceCoverageStage.NONE ->
                "All inspected source coverage stages are present."
        }
    }

    companion object {

        private const val MAXIMUM_MATCHED_ALIASES =
            20

        private val EMPTY_RAW_COVERAGE =
            OFFNutritionRawSourceCoverageEntry(
                productMatchCount =
                    0,
                productWithAnyNutritionCount =
                    0,
                productWithUsableNutritionCount =
                    0,
                matchedProductNames =
                    emptyList()
            )
    }
}