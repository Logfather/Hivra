package de.shopme.tools.knowledge.catalog.taxonomy.model

data class PrimaryFoodTaxonomy(
    val version: Int,
    val domain: PrimaryFoodTaxonomyDomain,
    val departments: List<PrimaryFoodDepartment>
)

data class PrimaryFoodTaxonomyDomain(
    val id: String,
    val name: String
)

data class PrimaryFoodDepartment(
    val id: String,
    val name: String,
    val groups: List<PrimaryFoodGroup>
)

data class PrimaryFoodGroup(
    val id: String,
    val name: String
)