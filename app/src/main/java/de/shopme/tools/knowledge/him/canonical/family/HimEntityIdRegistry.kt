package de.shopme.tools.knowledge.him.canonical.family

data class HimEntityIdRegistry(
    val entries: List<HimEntityIdRegistryEntry>,
)

data class HimEntityIdRegistryEntry(
    val entityId: HimEntityId,
    val entityType: HimEntityType,
    val sourceReferenceType: HimEntitySourceReferenceType,
    val sourceReference: String,
)

enum class HimEntitySourceReferenceType {
    PRODUCT_ONLY_CANONICAL_NORMALIZED,
    GROUND_TRUTH_PROMOTION,
}
