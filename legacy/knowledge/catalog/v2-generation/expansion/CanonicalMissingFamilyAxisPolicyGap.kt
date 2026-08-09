package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.analysis

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis

data class CanonicalMissingFamilyAxisPolicyGap(
    val rank: Int,

    val category: String,
    val familyKey: String,
    val familyDisplayName: String,

    val axis:
    CanonicalProductFamilyVariantAxis,

    /**
     * Anzahl der Review-Kandidaten, die von dieser fehlenden Policy
     * betroffen sind.
     *
     * Ein Kandidat kann mehrere Policy-Lücken besitzen und deshalb in
     * mehreren Gap-Einträgen vorkommen.
     */
    val affectedCandidateCount: Int,

    val affectedReviewShare: Double,

    val distinctValueCount: Int,

    val affectedValueCounts:
    Map<String, Int>,

    val priority:
    CanonicalSemanticPolicyPriority,

    val recommendation:
    CanonicalSemanticBacklogRecommendation,

    val implementationKey: String,

    val rationale: String
) {

    init {
        require(rank > 0)

        require(category.isNotBlank())
        require(familyKey.isNotBlank())
        require(familyDisplayName.isNotBlank())

        require(affectedCandidateCount > 0)
        require(affectedReviewShare in 0.0..1.0)

        require(distinctValueCount > 0)

        require(
            distinctValueCount ==
                    affectedValueCounts.size
        )

        require(
            affectedValueCounts.values.all {
                it > 0
            }
        )

        require(
            affectedValueCounts ==
                    affectedValueCounts
                        .toSortedMap()
        )

        require(
            IMPLEMENTATION_KEY_REGEX.matches(
                implementationKey
            )
        ) {
            "Invalid implementation key: '$implementationKey'."
        }

        require(rationale.isNotBlank())
        require(rationale == rationale.trim())
    }

    private companion object {
        val IMPLEMENTATION_KEY_REGEX =
            Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
    }
}