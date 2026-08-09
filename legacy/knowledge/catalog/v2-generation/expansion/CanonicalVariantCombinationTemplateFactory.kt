package de.shopme.testing.system.tools.knowledge.catalog.expansion.combination

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalProductFamilyConcreteValueCoverage

class CanonicalVariantCombinationTemplateFactory {

    fun create(
        family:
        CanonicalProductFamilyConcreteValueCoverage
    ): List<CanonicalVariantCombinationTemplate> {
        require(family.valid)

        val selectedValueCounts =
            family.axisCoverages
                .associate {
                    it.axis to it.selectedValueCount
                }

        val availableAxes =
            family.axisCoverages
                .map { it.axis }
                .distinct()
                .sortedBy { it.name }

        val templates =
            mutableListOf<CanonicalVariantCombinationTemplate>()

        availableAxes.forEach { axis ->
            templates +=
                template(
                    mode =
                        CanonicalVariantCombinationMode.SINGLE_AXIS,

                    axes =
                        listOf(axis),

                    maximumMaterializedCombinationCount =
                        requireNotNull(
                            selectedValueCounts[axis]
                        ),

                    required = true,

                    rationale =
                        "Single-axis template preserves direct canonical " +
                                "coverage for axis '$axis'."
                )
        }

        val anchorAxes =
            CanonicalVariantAxisCombinationPolicy
                .anchorAxesFrom(availableAxes)

        val qualifierAxes =
            CanonicalVariantAxisCombinationPolicy
                .qualifierAxesFrom(availableAxes)

        anchorAxes.forEach { anchor ->
            qualifierAxes.forEach { qualifier ->
                if (
                    CanonicalVariantAxisCombinationPolicy
                        .canCombine(anchor, qualifier)
                ) {
                    val anchorCount =
                        requireNotNull(
                            selectedValueCounts[anchor]
                        )

                    val qualifierCount =
                        requireNotNull(
                            selectedValueCounts[qualifier]
                        )

                    templates +=
                        template(
                            mode =
                                CanonicalVariantCombinationMode
                                    .ANCHORED_PAIR,

                            axes =
                                listOf(anchor, qualifier),

                            maximumMaterializedCombinationCount =
                                minOf(
                                    anchorCount *
                                            qualifierCount,

                                    MAXIMUM_PAIR_MATERIALIZATION
                                ),

                            required = false,

                            rationale =
                                "Anchored pair combines identity axis " +
                                        "'$anchor' with qualifier '$qualifier' " +
                                        "without full Cartesian expansion."
                        )
                }
            }
        }

        var currentCapacity =
            templates.sumOf {
                it.maximumMaterializedCombinationCount
            }

        if (currentCapacity < family.allocatedTargetEntryCount) {
            val compatibleTriples =
                buildCompatibleTriples(
                    anchorAxes =
                        anchorAxes,

                    qualifierAxes =
                        qualifierAxes
                )

            for (axes in compatibleTriples) {
                if (
                    currentCapacity >=
                    family.allocatedTargetEntryCount
                ) {
                    break
                }

                val theoreticalCount =
                    axes.fold(1L) { product, axis ->
                        product *
                                requireNotNull(
                                    selectedValueCounts[axis]
                                ).toLong()
                    }

                val materializationCount =
                    minOf(
                        theoreticalCount,
                        MAXIMUM_TRIPLE_MATERIALIZATION.toLong()
                    ).toInt()

                templates +=
                    template(
                        mode =
                            CanonicalVariantCombinationMode
                                .CURATED_TRIPLE,

                        axes =
                            axes,

                        maximumMaterializedCombinationCount =
                            materializationCount,

                        required = false,

                        rationale =
                            "Curated triple increases family capacity using " +
                                    "one anchor and two compatible qualifiers. " +
                                    "Materialization remains explicitly capped."
                    )

                currentCapacity +=
                    materializationCount
            }
        }

        return templates
            .distinctBy { it.key }
            .sortedBy { it.key }
    }

    private fun buildCompatibleTriples(
        anchorAxes:
        List<CanonicalProductFamilyVariantAxis>,

        qualifierAxes:
        List<CanonicalProductFamilyVariantAxis>
    ): List<List<CanonicalProductFamilyVariantAxis>> =
        buildList {
            anchorAxes.forEach { anchor ->
                qualifierAxes.forEachIndexed { index, firstQualifier ->
                    qualifierAxes
                        .drop(index + 1)
                        .forEach { secondQualifier ->
                            val axes =
                                listOf(
                                    anchor,
                                    firstQualifier,
                                    secondQualifier
                                )
                                    .distinct()
                                    .sortedBy { it.name }

                            if (
                                axes.size == 3 &&
                                CanonicalVariantAxisCombinationPolicy
                                    .canCombine(axes)
                            ) {
                                add(axes)
                            }
                        }
                }
            }
        }
            .distinct()
            .sortedBy { axes ->
                axes.joinToString("-") {
                    it.name
                }
            }

    private fun template(
        mode: CanonicalVariantCombinationMode,
        axes: List<CanonicalProductFamilyVariantAxis>,
        maximumMaterializedCombinationCount: Int,
        required: Boolean,
        rationale: String
    ): CanonicalVariantCombinationTemplate {
        val sortedAxes =
            axes
                .distinct()
                .sortedBy { it.name }

        val key =
            buildString {
                append(
                    mode.name
                        .lowercase()
                        .replace('_', '-')
                )

                append("-")

                append(
                    sortedAxes.joinToString("-") {
                        it.name
                            .lowercase()
                            .replace('_', '-')
                    }
                )
            }

        return CanonicalVariantCombinationTemplate(
            key = key,
            mode = mode,
            axes = sortedAxes,
            maximumMaterializedCombinationCount =
                maximumMaterializedCombinationCount,
            required = required,
            rationale = rationale
        )
    }

    private companion object {
        const val MAXIMUM_PAIR_MATERIALIZATION = 48
        const val MAXIMUM_TRIPLE_MATERIALIZATION = 72
    }
}