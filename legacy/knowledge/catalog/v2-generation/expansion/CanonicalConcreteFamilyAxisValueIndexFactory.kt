package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalConcreteVariantValueCoverage

class CanonicalConcreteFamilyAxisValueIndexFactory {

    fun create(
        coverage: CanonicalConcreteVariantValueCoverage
    ): CanonicalConcreteFamilyAxisValueIndex {

        val entries =
            coverage.categories
                .flatMap { category ->
                    category.families
                }
                .flatMap { family ->

                    family.axisCoverages.map { axisCoverage ->

                        CanonicalConcreteFamilyAxisValueIndex.identityKey(
                            familyKey = family.familyKey,
                            axis = axisCoverage.axis.name
                        ) to
                                axisCoverage.selectedValues
                                    .map { it.key }
                    }
                }
                .toMap()

        return CanonicalConcreteFamilyAxisValueIndex(
            entries = entries
        )
    }
}