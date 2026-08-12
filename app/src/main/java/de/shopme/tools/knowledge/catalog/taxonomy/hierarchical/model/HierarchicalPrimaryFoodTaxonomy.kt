package de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.model

data class HierarchicalPrimaryFoodTaxonomy(
    val version: Int,
    val domain: HierarchicalFoodDomain,
    val departments: List<HierarchicalFoodDepartment>
)

data class HierarchicalFoodDomain(
    val id: String,
    val name: String
)

data class HierarchicalFoodDepartment(
    val id: String,
    val name: String,
    val children: List<HierarchicalFoodTaxonomyNode>
)

data class HierarchicalFoodTaxonomyNode(
    val id: String,
    val name: String,
    val children: List<HierarchicalFoodTaxonomyNode>
)