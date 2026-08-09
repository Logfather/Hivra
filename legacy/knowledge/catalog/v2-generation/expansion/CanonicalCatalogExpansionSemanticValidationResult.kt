package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

data class CanonicalCatalogExpansionSemanticValidationResult(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val generatedCandidateCount: Int,
    val evaluatedCandidateCount: Int,

    val acceptedCandidateCount: Int,
    val rejectedCandidateCount: Int,
    val reviewRequiredCandidateCount: Int,

    val acceptanceShare: Double,
    val rejectionShare: Double,
    val reviewRequiredShare: Double,

    val decisionCounts:
    Map<CanonicalExpansionSemanticDecision, Int>,

    val findingCountsByRuleType:
    Map<CanonicalExpansionSemanticRuleType, Int>,

    val categoryCount: Int,

    val categories:
    List<CanonicalExpansionSemanticCategoryResult>,

    val entries:
    List<CanonicalExpansionCandidateSemanticValidationEntry>,

    val completeCandidateCoverage: Boolean,
    val deterministicOrderValid: Boolean,

    val blockers: List<String>,

    val valid: Boolean
) {

    init {
        require(version > 0)
        require(sourceBaselineId.isNotBlank())

        require(
            SHA_256_REGEX.matches(
                sourceBaselineCatalogSha256
            )
        )

        require(generatedCandidateCount >= 0)
        require(evaluatedCandidateCount == entries.size)

        require(
            acceptedCandidateCount ==
                    entries.count {
                        it.accepted
                    }
        )

        require(
            rejectedCandidateCount ==
                    entries.count {
                        it.rejected
                    }
        )

        require(
            reviewRequiredCandidateCount ==
                    entries.count {
                        it.reviewRequired
                    }
        )

        require(
            evaluatedCandidateCount ==
                    acceptedCandidateCount +
                    rejectedCandidateCount +
                    reviewRequiredCandidateCount
        )

        require(acceptanceShare in 0.0..1.0)
        require(rejectionShare in 0.0..1.0)
        require(reviewRequiredShare in 0.0..1.0)

        if (evaluatedCandidateCount == 0) {
            require(acceptanceShare == 0.0)
            require(rejectionShare == 0.0)
            require(reviewRequiredShare == 0.0)
        }

        require(
            decisionCounts.values.sum() ==
                    evaluatedCandidateCount
        )

        require(
            decisionCounts ==
                    decisionCounts
                        .toList()
                        .sortedBy {
                            it.first.name
                        }
                        .associate {
                            it
                        }
        )

        require(
            findingCountsByRuleType ==
                    findingCountsByRuleType
                        .toList()
                        .sortedBy {
                            it.first.name
                        }
                        .associate {
                            it
                        }
        )

        require(
            findingCountsByRuleType.values.sum() ==
                    entries.sumOf {
                        it.findingCount
                    }
        )

        require(categoryCount == categories.size)

        require(
            categories ==
                    categories.sortedBy {
                        it.category
                    }
        )

        require(
            categories.sumOf {
                it.evaluatedCandidateCount
            } ==
                    evaluatedCandidateCount
        )

        require(
            entries ==
                    entries.sortedBy {
                        it.candidateIndex
                    }
        )

        require(
            completeCandidateCoverage ==
                    (
                            evaluatedCandidateCount ==
                                    generatedCandidateCount
                            )
        )

        require(
            deterministicOrderValid ==
                    (
                            entries ==
                                    entries.sortedBy {
                                        it.candidateIndex
                                    }
                            )
        )

        require(
            blockers ==
                    blockers
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        require(
            valid ==
                    (
                            blockers.isEmpty() &&
                                    completeCandidateCoverage &&
                                    deterministicOrderValid &&
                                    categories.all {
                                        it.complete
                                    }
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}