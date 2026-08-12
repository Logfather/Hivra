package de.shopme.tools.knowledge.catalog.truecanonical.assignment.model

data class TaxonomyAssignedCanonicalFoodIdentity(
    val itemname: String,
    val normalized: String,
    val departmentId: String,
    val groupId: String,
    val variants: List<String>,
    val sourceVariants: List<String>
)