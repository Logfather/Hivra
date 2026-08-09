package de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis

data class CanonicalExpansionCandidateVariantValue(
    val axis: CanonicalProductFamilyVariantAxis,
    val valueKey: String,
    val displayName: String
) {

    init {
        require(valueKey.isNotBlank())
        require(displayName.isNotBlank())
        require(displayName == displayName.trim())
    }
}