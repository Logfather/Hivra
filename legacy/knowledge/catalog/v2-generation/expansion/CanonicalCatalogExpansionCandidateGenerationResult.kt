package de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate

data class CanonicalCatalogExpansionCandidateGenerationResult(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val baselineEntryCount: Int,
    val derivedTargetEntryCount: Int,
    val requiredExpansionEntryCount: Int,

    val categoryCount: Int,
    val productFamilyCount: Int,

    val generatedCandidateCount: Int,

    val categories:
    List<CanonicalExpansionCandidateCategoryResult>,

    val candidates:
    List<CanonicalCatalogExpansionCandidate>,

    val uniqueCandidateKeyCount: Int,
    val uniqueProposedNormalizedKeyCount: Int,

    val candidatesRequiringSemanticValidationCount: Int,

    val exactExpansionCountReached: Boolean,
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

        require(baselineEntryCount > 0)
        require(derivedTargetEntryCount > baselineEntryCount)

        require(
            requiredExpansionEntryCount ==
                    derivedTargetEntryCount -
                    baselineEntryCount
        )

        require(categoryCount == categories.size)

        require(
            productFamilyCount ==
                    categories.sumOf {
                        it.productFamilyCount
                    }
        )

        require(generatedCandidateCount == candidates.size)

        require(
            generatedCandidateCount ==
                    categories.sumOf {
                        it.generatedCandidateCount
                    }
        )

        require(
            candidates.map {
                it.candidateIndex
            } ==
                    (1..candidates.size).toList()
        )

        require(
            uniqueCandidateKeyCount ==
                    candidates.map {
                        it.candidateKey
                    }.distinct().size
        )

        require(
            uniqueProposedNormalizedKeyCount ==
                    candidates.map {
                        it.proposedNormalizedKey
                    }.distinct().size
        )

        require(
            candidatesRequiringSemanticValidationCount ==
                    candidates.count {
                        it.requiresSemanticValidation
                    }
        )

        require(
            exactExpansionCountReached ==
                    (
                            generatedCandidateCount ==
                                    requiredExpansionEntryCount
                            )
        )

        require(
            deterministicOrderValid ==
                    (
                            candidates ==
                                    candidates.sortedWith(
                                        compareBy<
                                                CanonicalCatalogExpansionCandidate
                                                > {
                                            it.category
                                        }.thenBy {
                                            it.familyKey
                                        }.thenBy {
                                            it.familyCandidateIndex
                                        }
                                    )
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
                                    exactExpansionCountReached &&
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