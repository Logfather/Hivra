package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution

data class CatalogDuplicateResolutionDecision(
    val sourceIndex: Int,
    val targetSourceIndex: Int,
    val normalizedKey: String,
    val reason: CatalogDuplicateResolutionReason
) {

    init {
        require(sourceIndex >= 0) {
            "sourceIndex must not be negative."
        }

        require(targetSourceIndex >= 0) {
            "targetSourceIndex must not be negative."
        }

        require(sourceIndex != targetSourceIndex) {
            "Duplicate source must not target itself."
        }

        require(normalizedKey.isNotBlank()) {
            "normalizedKey must not be blank."
        }
    }
}