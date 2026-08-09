package de.shopme.testing.system.tools.knowledge.catalog.expansion.approval

import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem

data class CanonicalApprovedCatalogExpansionResult(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val baselineEntryCount: Int,
    val generatedCandidateCount: Int,

    val semanticallyAcceptedCandidateCount: Int,
    val semanticallyRejectedCandidateCount: Int,
    val reviewRequiredCandidateCount: Int,

    val materializedEntryCount: Int,

    val baselineKeyCollisionCount: Int,
    val expansionKeyCollisionCount: Int,

    val expandedCatalogEntryCount: Int,

    val materializedItems:
    List<CatalogFoodItem>,

    val expandedCatalogItems:
    List<CatalogFoodItem>,

    val entries:
    List<CanonicalExpansionMaterializationEntry>,

    val completeCandidateCoverage: Boolean,
    val exactCatalogArithmeticValid: Boolean,
    val uniqueNormalizedKeys: Boolean,
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
        require(generatedCandidateCount >= 0)

        require(semanticallyAcceptedCandidateCount >= 0)
        require(semanticallyRejectedCandidateCount >= 0)
        require(reviewRequiredCandidateCount >= 0)

        require(
            generatedCandidateCount ==
                    semanticallyAcceptedCandidateCount +
                    semanticallyRejectedCandidateCount +
                    reviewRequiredCandidateCount
        )

        require(materializedEntryCount == materializedItems.size)

        require(
            materializedEntryCount ==
                    entries.count {
                        it.status ==
                                CanonicalExpansionMaterializationStatus
                                    .MATERIALIZED
                    }
        )

        require(
            baselineKeyCollisionCount ==
                    entries.count {
                        it.status ==
                                CanonicalExpansionMaterializationStatus
                                    .BLOCKED_BASELINE_KEY_COLLISION
                    }
        )

        require(
            expansionKeyCollisionCount ==
                    entries.count {
                        it.status ==
                                CanonicalExpansionMaterializationStatus
                                    .BLOCKED_EXPANSION_KEY_COLLISION
                    }
        )

        require(expandedCatalogEntryCount == expandedCatalogItems.size)

        require(
            entries ==
                    entries.sortedBy {
                        it.candidateIndex
                    }
        )

        require(
            entries.map {
                it.candidateIndex
            } ==
                    (1..generatedCandidateCount).toList()
        )

        require(
            completeCandidateCoverage ==
                    (
                            entries.size ==
                                    generatedCandidateCount
                            )
        )

        require(
            exactCatalogArithmeticValid ==
                    (
                            expandedCatalogEntryCount ==
                                    baselineEntryCount +
                                    materializedEntryCount
                            )
        )

        require(
            uniqueNormalizedKeys ==
                    (
                            expandedCatalogItems
                                .map {
                                    requireNotNull(it.normalized)
                                }
                                .distinct()
                                .size ==
                                    expandedCatalogItems.size
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
                                    exactCatalogArithmeticValid &&
                                    uniqueNormalizedKeys &&
                                    deterministicOrderValid &&
                                    baselineKeyCollisionCount == 0 &&
                                    expansionKeyCollisionCount == 0
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}