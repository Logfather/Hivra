package de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement

data class CanonicalFoodCatalogTargetMethodology(
    val catalogConcept: String,

    val canonicalUnitDefinition: String,

    val includedVariationRule: String,

    val excludedVariationRule: String,

    val targetInterpretation: String,

    val marketCoverageClaim: String
) {

    init {
        require(catalogConcept == CANONICAL_CONCEPT)

        require(canonicalUnitDefinition.isNotBlank())
        require(includedVariationRule.isNotBlank())
        require(excludedVariationRule.isNotBlank())
        require(targetInterpretation.isNotBlank())
        require(marketCoverageClaim.isNotBlank())
    }

    companion object {
        const val CANONICAL_CONCEPT =
            "CANONICAL_FOOD_TYPES_NOT_RETAIL_SKUS"
    }
}