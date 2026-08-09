package de.shopme.testing.system.tools.knowledge.catalog.evaluation

data class NormalizedCatalogEvaluationExample(
    val sourceIndex: Int,
    val itemName: String,
    val normalizedKey: String?,
    val category: String?,
    val plural: String?,
    val reason: String
) {

    init {
        require(sourceIndex >= 0) {
            "sourceIndex must not be negative."
        }

        require(itemName.isNotBlank()) {
            "itemName must not be blank."
        }

        require(reason.isNotBlank()) {
            "reason must not be blank."
        }
    }
}