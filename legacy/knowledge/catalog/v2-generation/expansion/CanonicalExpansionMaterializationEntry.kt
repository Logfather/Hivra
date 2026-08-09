package de.shopme.testing.system.tools.knowledge.catalog.expansion.approval

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticDecision

data class CanonicalExpansionMaterializationEntry(
    val candidateIndex: Int,
    val candidateKey: String,

    val category: String,
    val familyKey: String,

    val proposedCanonicalName: String,
    val proposedNormalizedKey: String,

    val semanticDecision:
    CanonicalExpansionSemanticDecision,

    val status:
    CanonicalExpansionMaterializationStatus,

    val resultingCanonicalName: String?,
    val resultingNormalizedKey: String?,

    val reasons: List<String>
) {

    init {
        require(candidateIndex > 0)
        require(candidateKey.isNotBlank())

        require(category.isNotBlank())
        require(familyKey.isNotBlank())

        require(proposedCanonicalName.isNotBlank())
        require(proposedNormalizedKey.isNotBlank())

        require(
            reasons ==
                    reasons
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        when (status) {
            CanonicalExpansionMaterializationStatus.MATERIALIZED -> {
                require(
                    semanticDecision ==
                            CanonicalExpansionSemanticDecision.ACCEPT
                )

                require(!resultingCanonicalName.isNullOrBlank())
                require(!resultingNormalizedKey.isNullOrBlank())
            }

            CanonicalExpansionMaterializationStatus
                .REJECTED_SEMANTICALLY -> {
                require(
                    semanticDecision in
                            REJECTION_DECISIONS
                )

                require(resultingCanonicalName == null)
                require(resultingNormalizedKey == null)
            }

            CanonicalExpansionMaterializationStatus
                .REVIEW_REQUIRED -> {
                require(
                    semanticDecision ==
                            CanonicalExpansionSemanticDecision
                                .REVIEW_REQUIRED
                )

                require(resultingCanonicalName == null)
                require(resultingNormalizedKey == null)
            }

            CanonicalExpansionMaterializationStatus
                .BLOCKED_BASELINE_KEY_COLLISION,

            CanonicalExpansionMaterializationStatus
                .BLOCKED_EXPANSION_KEY_COLLISION -> {
                require(
                    semanticDecision ==
                            CanonicalExpansionSemanticDecision.ACCEPT
                )

                require(resultingCanonicalName == null)
                require(resultingNormalizedKey == null)
            }
        }
    }

    private companion object {
        val REJECTION_DECISIONS =
            setOf(
                CanonicalExpansionSemanticDecision
                    .REJECT_IMPOSSIBLE_COMBINATION,

                CanonicalExpansionSemanticDecision
                    .REJECT_WRONG_FAMILY_VALUE,

                CanonicalExpansionSemanticDecision
                    .REJECT_REDUNDANT_VARIANT
            )
    }
}