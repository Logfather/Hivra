package de.shopme.tools.knowledge.rebuild.nutrition.coverage

data class NutritionCoverageGapAnalysis(
    val version: Int,
    val catalogItemCount: Int,
    val coveredCatalogItemCount: Int,
    val missingCatalogItemCount: Int,
    val classifiedGapCount: Int,
    val unclassifiedGapCount: Int,
    val countsByType: Map<String, Int>,
    val exactMatchNotInRuntimeCount: Int,
    val exactMatchNotInRuntimeCatalogKeys: List<String>,
    val noRequestCount: Int,
    val noRequestCatalogKeys: List<String>,
    val matchNotPersistedCount: Int,
    val matchNotPersistedCatalogKeys: List<String>,
    val noDecisionCount: Int,
    val noDecisionCatalogKeys: List<String>,
    val noCandidatesCount: Int,
    val noCandidatesCatalogKeys: List<String>,
    val noMatchCount: Int,
    val countsByNoMatchCause: Map<String, Int>,
    val investigatedCatalogKeys: List<InvestigatedNutritionCoverageGap>
) {

    init {
        require(version == CURRENT_VERSION) {
            "Unsupported nutrition coverage-gap analysis version: " +
                    version
        }

        require(catalogItemCount >= 0) {
            "Catalog item count must not be negative."
        }

        require(coveredCatalogItemCount >= 0) {
            "Covered catalog item count must not be negative."
        }

        require(missingCatalogItemCount >= 0) {
            "Missing catalog item count must not be negative."
        }

        require(
            coveredCatalogItemCount +
                    missingCatalogItemCount ==
                    catalogItemCount
        ) {
            "Covered and missing catalog counts must add up to the " +
                    "catalog item count."
        }

        require(
            classifiedGapCount +
                    unclassifiedGapCount ==
                    missingCatalogItemCount
        ) {
            "Classified and unclassified gap counts must add up to " +
                    "the missing catalog item count."
        }

        require(
            countsByType.values.sum() ==
                    missingCatalogItemCount
        ) {
            "Gap-type counts must add up to the missing catalog item " +
                    "count."
        }

        require(
            exactMatchNotInRuntimeCount ==
                    exactMatchNotInRuntimeCatalogKeys.size
        ) {
            "EXACT_MATCH_NOT_IN_RUNTIME count differs from its key list."
        }

        require(
            noRequestCount ==
                    noRequestCatalogKeys.size
        ) {
            "NO_REQUEST count differs from its key list."
        }

        require(
            matchNotPersistedCount ==
                    matchNotPersistedCatalogKeys.size
        ) {
            "MATCH_NOT_PERSISTED count differs from its key list."
        }

        require(
            noDecisionCount ==
                    noDecisionCatalogKeys.size
        ) {
            "NO_DECISION count differs from its key list."
        }

        require(
            noCandidatesCount ==
                    noCandidatesCatalogKeys.size
        ) {
            "NO_CANDIDATES count differs from its key list."
        }

        require(
            countsByNoMatchCause.values.sum() ==
                    noMatchCount
        ) {
            "NO_MATCH cause counts must add up to the NO_MATCH count."
        }

        requireSortedDistinct(
            values =
                exactMatchNotInRuntimeCatalogKeys,
            fieldName =
                "exactMatchNotInRuntimeCatalogKeys"
        )

        requireSortedDistinct(
            values =
                noRequestCatalogKeys,
            fieldName =
                "noRequestCatalogKeys"
        )

        requireSortedDistinct(
            values =
                matchNotPersistedCatalogKeys,
            fieldName =
                "matchNotPersistedCatalogKeys"
        )

        requireSortedDistinct(
            values =
                noDecisionCatalogKeys,
            fieldName =
                "noDecisionCatalogKeys"
        )

        requireSortedDistinct(
            values =
                noCandidatesCatalogKeys,
            fieldName =
                "noCandidatesCatalogKeys"
        )

        require(
            investigatedCatalogKeys ==
                    investigatedCatalogKeys.sortedBy {
                        it.catalogKey
                    }
        ) {
            "Investigated catalog keys must be sorted."
        }

        require(
            investigatedCatalogKeys
                .map {
                    it.catalogKey
                }
                .distinct()
                .size ==
                    investigatedCatalogKeys.size
        ) {
            "Investigated catalog keys must be unique."
        }
    }

    private fun requireSortedDistinct(
        values: List<String>,
        fieldName: String
    ) {
        require(
            values ==
                    values
                        .distinct()
                        .sorted()
        ) {
            "$fieldName must be sorted and contain no duplicates."
        }
    }

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

data class InvestigatedNutritionCoverageGap(
    val catalogKey: String,
    val presentInReport: Boolean,
    val type: String?,
    val noMatchCause: String?,
    val requestExists: Boolean?,
    val decisionExists: Boolean?,
    val decisionType: String?,
    val selectedServerKey: String?,
    val decisionConfidence: Double?,
    val candidateCount: Int?,
    val topCandidateKey: String?,
    val topCandidateScore: Double?,
    val details: String?
) {

    init {
        require(catalogKey.isNotBlank()) {
            "Investigated catalog key must not be blank."
        }

        if (presentInReport) {
            require(!type.isNullOrBlank()) {
                "A present investigated gap requires a type."
            }

            require(requestExists != null) {
                "A present investigated gap requires requestExists."
            }

            require(decisionExists != null) {
                "A present investigated gap requires decisionExists."
            }

            require(candidateCount != null) {
                "A present investigated gap requires candidateCount."
            }

            require(!details.isNullOrBlank()) {
                "A present investigated gap requires details."
            }
        } else {
            require(type == null) {
                "An absent investigated key must not have a type."
            }

            require(noMatchCause == null) {
                "An absent investigated key must not have a noMatchCause."
            }

            require(requestExists == null) {
                "An absent investigated key must not have request state."
            }

            require(decisionExists == null) {
                "An absent investigated key must not have decision state."
            }

            require(decisionType == null) {
                "An absent investigated key must not have a decision type."
            }

            require(selectedServerKey == null) {
                "An absent investigated key must not have a selected key."
            }

            require(decisionConfidence == null) {
                "An absent investigated key must not have confidence."
            }

            require(candidateCount == null) {
                "An absent investigated key must not have candidates."
            }

            require(topCandidateKey == null) {
                "An absent investigated key must not have a top candidate."
            }

            require(topCandidateScore == null) {
                "An absent investigated key must not have a top score."
            }

            require(details == null) {
                "An absent investigated key must not have details."
            }
        }
    }
}