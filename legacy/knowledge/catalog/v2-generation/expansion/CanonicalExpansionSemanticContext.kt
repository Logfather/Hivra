package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidate
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis

data class CanonicalExpansionSemanticContext(
    val candidate:
    CanonicalCatalogExpansionCandidate,

    val valuesByAxis:
    Map<CanonicalProductFamilyVariantAxis, String>
) {

    init {
        require(
            valuesByAxis.size ==
                    candidate.variantValues.size
        )

        require(
            candidate.variantValues.all {
                valuesByAxis[it.axis] ==
                        it.valueKey
            }
        )
    }

    fun hasAxis(
        axis: CanonicalProductFamilyVariantAxis
    ): Boolean =
        axis in valuesByAxis

    fun valueFor(
        axis: CanonicalProductFamilyVariantAxis
    ): String? =
        valuesByAxis[axis]

    fun hasValue(
        axis: CanonicalProductFamilyVariantAxis,
        valueKey: String
    ): Boolean =
        valuesByAxis[axis] == valueKey

    fun containsAnyValue(
        axis: CanonicalProductFamilyVariantAxis,
        values: Set<String>
    ): Boolean =
        valuesByAxis[axis] in values
}