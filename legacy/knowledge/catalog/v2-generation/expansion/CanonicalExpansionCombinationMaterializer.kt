package de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate

import de.shopme.testing.system.tools.knowledge.catalog.expansion.combination
.CanonicalVariantCombinationTemplate
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value
.CanonicalProductFamilyConcreteValueCoverage
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value
.CanonicalVariantValue

class CanonicalExpansionCombinationMaterializer {

    fun materialize(
        family:
        CanonicalProductFamilyConcreteValueCoverage,

        template:
        CanonicalVariantCombinationTemplate
    ): List<List<CanonicalExpansionCandidateVariantValue>> {
        require(family.valid)

        val valuesByAxis =
            family.axisCoverages
                .associate { axisCoverage ->
                    axisCoverage.axis to
                            axisCoverage.selectedValues
                }

        val axisValueLists =
            template.axes.map { axis ->
                axis to
                        requireNotNull(
                            valuesByAxis[axis]
                        ) {
                            "Template '${template.key}' references unavailable " +
                                    "axis '$axis' for family '${family.familyKey}'."
                        }
            }

        val combinations =
            when (axisValueLists.size) {
                1 ->
                    axisValueLists[0]
                        .second
                        .map { value ->
                            listOf(
                                candidateValue(
                                    axisValueLists[0].first,
                                    value
                                )
                            )
                        }

                2 ->
                    axisValueLists[0].second
                        .flatMap { firstValue ->
                            axisValueLists[1].second
                                .map { secondValue ->
                                    listOf(
                                        candidateValue(
                                            axisValueLists[0].first,
                                            firstValue
                                        ),
                                        candidateValue(
                                            axisValueLists[1].first,
                                            secondValue
                                        )
                                    )
                                }
                        }

                3 ->
                    axisValueLists[0].second
                        .flatMap { firstValue ->
                            axisValueLists[1].second
                                .flatMap { secondValue ->
                                    axisValueLists[2].second
                                        .map { thirdValue ->
                                            listOf(
                                                candidateValue(
                                                    axisValueLists[0].first,
                                                    firstValue
                                                ),
                                                candidateValue(
                                                    axisValueLists[1].first,
                                                    secondValue
                                                ),
                                                candidateValue(
                                                    axisValueLists[2].first,
                                                    thirdValue
                                                )
                                            )
                                        }
                                }
                        }

                else ->
                    error(
                        "Unsupported template axis count: " +
                                axisValueLists.size
                    )
            }

        return combinations
            .map { values ->
                values.sortedBy {
                    it.axis.name
                }
            }
            .distinctBy { values ->
                values.joinToString("|") {
                    "${it.axis.name}:${it.valueKey}"
                }
            }
            .sortedBy { values ->
                values.joinToString("|") {
                    "${it.axis.name}:${it.valueKey}"
                }
            }
            .take(
                template.maximumMaterializedCombinationCount
            )
    }

    private fun candidateValue(
        axis:
        de.shopme.testing.system.tools.knowledge.catalog.expansion.family
        .CanonicalProductFamilyVariantAxis,

        value: CanonicalVariantValue
    ): CanonicalExpansionCandidateVariantValue =
        CanonicalExpansionCandidateVariantValue(
            axis = axis,
            valueKey = value.key,
            displayName = value.displayName
        )
}