package de.shopme.tools.knowledge.rebuild.nutrition.coverage

class NutritionCoverageGapAnalyzer {

    fun analyze(
        report: NutritionCoverageGapReport,
        investigatedCatalogKeys: Set<String>
    ): NutritionCoverageGapAnalysis {

        val sortedGaps =
            report.gaps
                .sortedBy {
                    it.catalogKey
                }

        require(
            sortedGaps
                .map {
                    it.catalogKey
                }
                .distinct()
                .size ==
                    sortedGaps.size
        ) {
            "Nutrition coverage-gap report contains duplicate catalog " +
                    "keys."
        }

        require(
            sortedGaps.size ==
                    report.missingCatalogItemCount
        ) {
            "Nutrition coverage-gap count differs from the report " +
                    "header: gaps=${sortedGaps.size}, " +
                    "missing=${report.missingCatalogItemCount}."
        }

        val calculatedCountsByType =
            sortedGaps
                .groupingBy {
                    it.type.name
                }
                .eachCount()
                .toSortedMap()

        require(
            calculatedCountsByType ==
                    report.countsByType
        ) {
            "Calculated gap-type counts differ from the persisted " +
                    "coverage report: calculated=$calculatedCountsByType, " +
                    "persisted=${report.countsByType}."
        }

        val gapsByCatalogKey =
            sortedGaps.associateBy {
                it.catalogKey
            }

        val exactMatchNotInRuntimeCatalogKeys =
            catalogKeysForType(
                gaps =
                    sortedGaps,
                type =
                    NutritionCoverageGapType
                        .EXACT_MATCH_NOT_IN_RUNTIME
            )

        val noRequestCatalogKeys =
            catalogKeysForType(
                gaps =
                    sortedGaps,
                type =
                    NutritionCoverageGapType.NO_REQUEST
            )

        val matchNotPersistedCatalogKeys =
            catalogKeysForType(
                gaps =
                    sortedGaps,
                type =
                    NutritionCoverageGapType.MATCH_NOT_PERSISTED
            )

        val noDecisionCatalogKeys =
            catalogKeysForType(
                gaps =
                    sortedGaps,
                type =
                    NutritionCoverageGapType.NO_DECISION
            )

        val noCandidatesCatalogKeys =
            catalogKeysForType(
                gaps =
                    sortedGaps,
                type =
                    NutritionCoverageGapType.NO_CANDIDATES
            )

        val noMatchGaps =
            sortedGaps
                .filter {
                    it.type ==
                            NutritionCoverageGapType.NO_MATCH
                }

        val countsByNoMatchCause =
            noMatchGaps
                .groupingBy { gap ->

                    requireNotNull(
                        gap.noMatchCause
                    ) {
                        "NO_MATCH gap '${gap.catalogKey}' contains no " +
                                "deterministic no-match cause."
                    }
                        .name
                }
                .eachCount()
                .toSortedMap()

        val investigatedGaps =
            investigatedCatalogKeys
                .asSequence()
                .map(
                    ::normalizeKey
                )
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .sorted()
                .map { catalogKey ->

                    val gap =
                        gapsByCatalogKey[
                            catalogKey
                        ]

                    if (gap == null) {
                        InvestigatedNutritionCoverageGap(
                            catalogKey =
                                catalogKey,
                            presentInReport =
                                false,
                            type =
                                null,
                            noMatchCause =
                                null,
                            requestExists =
                                null,
                            decisionExists =
                                null,
                            decisionType =
                                null,
                            selectedServerKey =
                                null,
                            decisionConfidence =
                                null,
                            candidateCount =
                                null,
                            topCandidateKey =
                                null,
                            topCandidateScore =
                                null,
                            details =
                                null
                        )
                    } else {
                        InvestigatedNutritionCoverageGap(
                            catalogKey =
                                gap.catalogKey,
                            presentInReport =
                                true,
                            type =
                                gap.type.name,
                            noMatchCause =
                                gap.noMatchCause
                                    ?.name,
                            requestExists =
                                gap.requestExists,
                            decisionExists =
                                gap.decisionExists,
                            decisionType =
                                gap.decisionType,
                            selectedServerKey =
                                gap.selectedServerKey,
                            decisionConfidence =
                                gap.decisionConfidence,
                            candidateCount =
                                gap.candidateCount,
                            topCandidateKey =
                                gap.topCandidateKey,
                            topCandidateScore =
                                gap.topCandidateScore,
                            details =
                                gap.details
                        )
                    }
                }
                .toList()

        return NutritionCoverageGapAnalysis(
            version =
                NutritionCoverageGapAnalysis.CURRENT_VERSION,
            catalogItemCount =
                report.catalogItemCount,
            coveredCatalogItemCount =
                report.coveredCatalogItemCount,
            missingCatalogItemCount =
                report.missingCatalogItemCount,
            classifiedGapCount =
                report.classifiedGapCount,
            unclassifiedGapCount =
                report.unclassifiedGapCount,
            countsByType =
                calculatedCountsByType,
            exactMatchNotInRuntimeCount =
                exactMatchNotInRuntimeCatalogKeys.size,
            exactMatchNotInRuntimeCatalogKeys =
                exactMatchNotInRuntimeCatalogKeys,
            noRequestCount =
                noRequestCatalogKeys.size,
            noRequestCatalogKeys =
                noRequestCatalogKeys,
            matchNotPersistedCount =
                matchNotPersistedCatalogKeys.size,
            matchNotPersistedCatalogKeys =
                matchNotPersistedCatalogKeys,
            noDecisionCount =
                noDecisionCatalogKeys.size,
            noDecisionCatalogKeys =
                noDecisionCatalogKeys,
            noCandidatesCount =
                noCandidatesCatalogKeys.size,
            noCandidatesCatalogKeys =
                noCandidatesCatalogKeys,
            noMatchCount =
                noMatchGaps.size,
            countsByNoMatchCause =
                countsByNoMatchCause,
            investigatedCatalogKeys =
                investigatedGaps
        )
    }

    private fun catalogKeysForType(
        gaps: List<NutritionCoverageGap>,
        type: NutritionCoverageGapType
    ): List<String> {

        return gaps
            .asSequence()
            .filter {
                it.type ==
                        type
            }
            .map {
                it.catalogKey
            }
            .distinct()
            .sorted()
            .toList()
    }

    private fun normalizeKey(
        value: String
    ): String {

        return value
            .trim()
            .lowercase()
            .replace(
                "-",
                " "
            )
            .replace(
                "_",
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()
    }

    private companion object {

        val WHITESPACE_REGEX =
            Regex("\\s+")
    }
}
