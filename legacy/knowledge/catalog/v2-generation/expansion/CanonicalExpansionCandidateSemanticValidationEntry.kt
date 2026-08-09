package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

data class CanonicalExpansionCandidateSemanticValidationEntry(
    val candidateIndex: Int,
    val candidateKey: String,

    val category: String,
    val familyKey: String,

    val proposedCanonicalName: String,
    val proposedNormalizedKey: String,

    val decision: CanonicalExpansionSemanticDecision,

    val findings:
    List<CanonicalExpansionSemanticFinding>,

    val findingCount: Int,

    val accepted: Boolean,
    val rejected: Boolean,
    val reviewRequired: Boolean
) {

    init {
        require(candidateIndex > 0)
        require(candidateKey.isNotBlank())

        require(category.isNotBlank())
        require(familyKey.isNotBlank())

        require(proposedCanonicalName.isNotBlank())
        require(proposedNormalizedKey.isNotBlank())

        require(findingCount == findings.size)

        require(
            findings ==
                    findings.sortedWith(
                        compareBy<
                                CanonicalExpansionSemanticFinding
                                > {
                            it.decision.name
                        }.thenBy {
                            it.ruleType.name
                        }.thenBy {
                            it.ruleKey
                        }.thenBy {
                            it.message
                        }
                    )
        )

        require(
            accepted ==
                    (
                            decision ==
                                    CanonicalExpansionSemanticDecision.ACCEPT
                            )
        )

        require(
            rejected ==
                    (
                            decision in
                                    REJECTION_DECISIONS
                            )
        )

        require(
            reviewRequired ==
                    (
                            decision ==
                                    CanonicalExpansionSemanticDecision
                                        .REVIEW_REQUIRED
                            )
        )

        require(
            listOf(
                accepted,
                rejected,
                reviewRequired
            ).count { it } == 1
        ) {
            "Semantic validation entry must have exactly one terminal state."
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