package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis

data class CanonicalVariantAxisValueSet(
    val axis: CanonicalProductFamilyVariantAxis,

    val values: List<CanonicalVariantValue>,

    val valueCount: Int,

    val completeCanonicalVocabulary: Boolean,

    val rationale: String
) {

    init {
        require(values.isNotEmpty())

        require(valueCount == values.size)

        require(
            values ==
                    values.sortedBy {
                        it.key
                    }
        ) {
            "Values for axis '$axis' must be sorted by canonical key."
        }

        require(
            values.map { it.key }
                .distinct()
                .size ==
                    values.size
        ) {
            "Values for axis '$axis' must have unique keys."
        }

        require(rationale.isNotBlank())
        require(rationale == rationale.trim())

        require(completeCanonicalVocabulary)
    }
}