package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalVariantAxisCoverageRequirement

class CanonicalFamilyVariantValueSelector {

    fun select(
        familyKey: String,
        category: String,
        requirement:
        CanonicalVariantAxisCoverageRequirement
    ): List<CanonicalVariantValue> {
        require(familyKey.isNotBlank())
        require(category.isNotBlank())

        val availableValues =
            CanonicalVariantValuePolicy
                .valuesFor(requirement.axis)

        require(
            availableValues.size >=
                    requirement.minimumRelevantValueCount
        ) {
            "Axis '${requirement.axis}' does not contain enough concrete " +
                    "values for family '$familyKey'."
        }

        val desiredCount =
            minOf(
                requirement.recommendedRelevantValueCount,
                requirement.maximumRelevantValueCount,
                availableValues.size
            )

        require(
            desiredCount >=
                    requirement.minimumRelevantValueCount
        )

        /*
         * Die Werteliste ist bereits deterministisch sortiert.
         *
         * Eine spätere familiensemantische Kuratierung darf diese Auswahl
         * weiter einschränken oder durch passendere Werte ersetzen.
         */
        return availableValues
            .take(desiredCount)
            .sortedBy {
                it.key
            }
    }
}