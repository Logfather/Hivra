package de.shopme.testing.system.tools.knowledge.catalog.expansion.approval

enum class CanonicalExpansionMaterializationStatus {
    MATERIALIZED,
    REJECTED_SEMANTICALLY,
    REVIEW_REQUIRED,
    BLOCKED_BASELINE_KEY_COLLISION,
    BLOCKED_EXPANSION_KEY_COLLISION
}