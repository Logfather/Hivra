package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

class CanonicalExpansionSemanticDecisionResolver {

    fun resolve(
        findings:
        List<CanonicalExpansionSemanticFinding>
    ): CanonicalExpansionSemanticDecision {
        if (findings.isEmpty()) {
            return CanonicalExpansionSemanticDecision.ACCEPT
        }

        return findings
            .map {
                it.decision
            }
            .minBy {
                priority(it)
            }
    }

    private fun priority(
        decision: CanonicalExpansionSemanticDecision
    ): Int =
        when (decision) {
            CanonicalExpansionSemanticDecision
                .REJECT_IMPOSSIBLE_COMBINATION ->
                0

            CanonicalExpansionSemanticDecision
                .REJECT_WRONG_FAMILY_VALUE ->
                1

            CanonicalExpansionSemanticDecision
                .REJECT_REDUNDANT_VARIANT ->
                2

            CanonicalExpansionSemanticDecision
                .REVIEW_REQUIRED ->
                3

            CanonicalExpansionSemanticDecision.ACCEPT ->
                4
        }
}