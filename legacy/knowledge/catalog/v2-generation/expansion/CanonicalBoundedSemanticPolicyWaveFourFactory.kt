package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatch
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType

class CanonicalBoundedSemanticPolicyWaveFourFactory(
    private val overrideRegistry:
    CanonicalBoundedSemanticPolicyOverrideRegistry =
        CanonicalBoundedSemanticPolicyOverrideRegistry()
) {

    fun createBatches(
        closurePlan:
        CanonicalBoundedSemanticPolicyClosurePlan,

        concreteValueIndex:
        CanonicalConcreteFamilyAxisValueIndex
    ): List<CanonicalCuratedSemanticPolicyBatch> {
        require(closurePlan.valid)
        require(closurePlan.boundedByFinalWave)

        val wave =
            requireNotNull(
                closurePlan.waves.singleOrNull {
                    it.waveNumber ==
                            WAVE_NUMBER
                }
            ) {
                "Bounded closure plan contains no Wave 4."
            }

        val batches =
            wave.assignments
                .groupBy {
                    it.sourceBatchKey
                }
                .toSortedMap()
                .map { (sourceBatchKey, assignments) ->
                    createBatch(
                        sourceBatchKey =
                            sourceBatchKey,

                        assignments =
                            assignments,

                        concreteValueIndex =
                            concreteValueIndex
                    )
                }

        val policies =
            batches.flatMap {
                it.policies
            }

        require(
            policies.size ==
                    wave.assignedGapCount
        ) {
            "Wave-4 policy count does not match its closure assignments."
        }

        require(
            policies
                .map {
                    it.identityKey
                }
                .distinct()
                .size ==
                    policies.size
        ) {
            "Wave-4 contains duplicate policy identities."
        }

        require(
            policies.all {
                it.active &&
                        it.complete
            }
        )

        return batches
    }

    private fun createBatch(
        sourceBatchKey: String,

        assignments:
        List<CanonicalBoundedSemanticPolicyClosureAssignment>,

        concreteValueIndex:
        CanonicalConcreteFamilyAxisValueIndex
    ): CanonicalCuratedSemanticPolicyBatch {
        require(assignments.isNotEmpty())

        val categories =
            assignments
                .map {
                    it.category
                }
                .distinct()

        val axes =
            assignments
                .map {
                    it.axis
                }
                .distinct()

        require(categories.size == 1) {
            "Source batch '$sourceBatchKey' spans multiple categories: " +
                    categories.sorted()
        }

        require(axes.size == 1) {
            "Source batch '$sourceBatchKey' spans multiple axes: " +
                    axes.sorted()
        }

        val policies =
            assignments
                .sortedWith(
                    compareBy<
                            CanonicalBoundedSemanticPolicyClosureAssignment
                            > {
                        it.familyKey
                    }.thenBy {
                        it.axis
                    }.thenBy {
                        it.gapKey
                    }
                )
                .map { assignment ->
                    createPolicy(
                        assignment =
                            assignment,

                        concreteValueIndex =
                            concreteValueIndex
                    )
                }

        return CanonicalCuratedSemanticPolicyBatch(
            version =
                CanonicalCuratedSemanticPolicyBatch
                    .CURRENT_VERSION,

            curationId =
                "curated-wave-004-$sourceBatchKey",

            sourceImplementationBatchKey =
                sourceBatchKey,

            category =
                categories.single(),

            axis =
                axes.single(),

            policyCount =
                policies.size,

            policies =
                policies,

            complete =
                policies.all {
                    it.active &&
                            it.complete
                },

            valid =
                policies.isNotEmpty() &&
                        policies.all {
                            it.active &&
                                    it.complete
                        } &&
                        policies
                            .map {
                                it.identityKey
                            }
                            .distinct()
                            .size ==
                        policies.size
        )
    }

    private fun createPolicy(
        assignment:
        CanonicalBoundedSemanticPolicyClosureAssignment,

        concreteValueIndex:
        CanonicalConcreteFamilyAxisValueIndex
    ): CanonicalFamilyAxisSemanticPolicyEntry {

        val axis =
            CanonicalProductFamilyVariantAxis
                .valueOf(
                    assignment.axis
                )

        val curatedOverride =
            overrideRegistry.findOverride(
                familyKey =
                    assignment.familyKey,

                axis =
                    axis
            )

        if (curatedOverride != null) {
            return curatedOverride
        }
        require(
            concreteValueIndex.contains(
                familyKey =
                    assignment.familyKey,

                axis =
                    assignment.axis
            )
        ) {
            "No canonical concrete value coverage exists for Wave-4 " +
                    "assignment '${assignment.gapKey}'."
        }

        val allowedValues =
            requireNotNull(
                concreteValueIndex.values(
                    familyKey =
                        assignment.familyKey,

                    axis =
                        assignment.axis
                )
            )

        return if (allowedValues.isEmpty()) {
            CanonicalFamilyAxisSemanticPolicyEntry(
                familyKey =
                    assignment.familyKey,

                axis =
                    axis,

                policyType =
                    CanonicalFamilyAxisSemanticPolicyType
                        .NOT_APPLICABLE,

                allowedValues =
                    emptyList(),

                rationale =
                    "Canonical concrete variant-value coverage contains " +
                            "no applicable values for family " +
                            "'${assignment.familyKey}' on axis " +
                            "'${assignment.axis}'.",

                source =
                    WAVE_SOURCE,

                active =
                    true
            )
        } else {
            CanonicalFamilyAxisSemanticPolicyEntry(
                familyKey =
                    assignment.familyKey,

                axis =
                    axis,

                policyType =
                    CanonicalFamilyAxisSemanticPolicyType
                        .CURATED_ALLOWED_VALUES,

                allowedValues =
                    allowedValues,

                rationale =
                    "Allowed values are derived from the validated canonical " +
                            "concrete variant-value coverage for family " +
                            "'${assignment.familyKey}' on axis " +
                            "'${assignment.axis}'.",

                source =
                    WAVE_SOURCE,

                active =
                    true
            )
        }
    }

    companion object {

        const val WAVE_NUMBER =
            4

        const val WAVE_SOURCE =
            "ShopMe bounded semantic policy curation wave 4"
    }
}