package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

enum class CanonicalExpansionSemanticDecision {
    ACCEPT,
    REJECT_IMPOSSIBLE_COMBINATION,
    REJECT_WRONG_FAMILY_VALUE,
    REJECT_REDUNDANT_VARIANT,
    REVIEW_REQUIRED
}