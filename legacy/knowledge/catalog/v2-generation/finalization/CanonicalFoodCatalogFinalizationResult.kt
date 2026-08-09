package de.shopme.testing.system.tools.knowledge.catalog.finalization

data class CanonicalFoodCatalogFinalizationResult(
    val version: Int,

    val finalizationId: String,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val sourcePolicySetId: String,
    val sourceWaveSixImpactKey: String,

    val baselineEntryCount: Int,
    val materializedEntryCount: Int,
    val finalCatalogEntryCount: Int,

    val semanticAcceptedCandidateCount: Int,
    val semanticRejectedCandidateCount: Int,
    val semanticReviewRequiredCandidateCount: Int,

    val categoryCount: Int,
    val uniqueCanonicalNameCount: Int,
    val uniqueNormalizedKeyCount: Int,

    val openImplementationBatchCount: Int,
    val openPolicyGapCount: Int,

    val approvedExpansionValid: Boolean,
    val waveSixImpactValid: Boolean,
    val terminalPolicyClosureReached: Boolean,

    val normalizedCatalogValidationValid: Boolean,
    val uniqueCanonicalNames: Boolean,
    val uniqueNormalizedKeys: Boolean,
    val deterministicOrderValid: Boolean,
    val exactCatalogArithmeticValid: Boolean,
    val persistedCatalogRoundtripValid: Boolean,

    val finalCatalogSha256: String,

    val blockers: List<String>,

    val valid: Boolean
) {

    init {
        require(version > 0)

        require(finalizationId.isNotBlank())

        require(sourceBaselineId.isNotBlank())
        require(SHA_256_REGEX.matches(sourceBaselineCatalogSha256))

        require(sourcePolicySetId.isNotBlank())
        require(sourceWaveSixImpactKey.isNotBlank())

        require(baselineEntryCount > 0)
        require(materializedEntryCount >= 0)
        require(finalCatalogEntryCount > 0)

        require(
            finalCatalogEntryCount ==
                    baselineEntryCount +
                    materializedEntryCount
        )

        require(semanticAcceptedCandidateCount >= 0)
        require(semanticRejectedCandidateCount >= 0)
        require(semanticReviewRequiredCandidateCount >= 0)

        require(categoryCount > 0)

        require(
            uniqueCanonicalNameCount in
                    1..finalCatalogEntryCount
        )

        require(
            uniqueNormalizedKeyCount in
                    1..finalCatalogEntryCount
        )

        require(openImplementationBatchCount >= 0)
        require(openPolicyGapCount >= 0)

        require(SHA_256_REGEX.matches(finalCatalogSha256))

        require(
            blockers ==
                    blockers
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        require(
            terminalPolicyClosureReached ==
                    (
                            openImplementationBatchCount == 0 &&
                                    openPolicyGapCount == 0
                            )
        )

        require(
            valid ==
                    (
                            blockers.isEmpty() &&
                                    approvedExpansionValid &&
                                    waveSixImpactValid &&
                                    terminalPolicyClosureReached &&
                                    normalizedCatalogValidationValid &&
                                    uniqueCanonicalNames &&
                                    uniqueNormalizedKeys &&
                                    deterministicOrderValid &&
                                    exactCatalogArithmeticValid &&
                                    persistedCatalogRoundtripValid
                            )
        )
    }

    companion object {

        const val CURRENT_VERSION =
            1

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}