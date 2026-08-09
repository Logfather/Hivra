package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatch
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType

class CanonicalBoundedSemanticPolicyWaveFiveFactory(
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
                closurePlan.waves.singleOrNull { candidateWave ->
                    candidateWave.waveNumber ==
                            WAVE_NUMBER
                }
            ) {
                "Bounded closure plan contains no Wave 5."
            }

        require(wave.valid)
        require(wave.complete)
        require(wave.assignments.isNotEmpty())

        require(
            wave.assignments
                .map { assignment ->
                    assignment.identityKey
                }
                .distinct()
                .size ==
                    wave.assignedGapCount
        ) {
            "Wave-5 closure assignments contain duplicate identities."
        }

        val batches =
            wave.assignments
                .groupBy { assignment ->
                    assignment.sourceBatchKey
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
            batches.flatMap { batch ->
                batch.policies
            }

        require(
            policies.size ==
                    wave.assignedGapCount
        ) {
            "Wave-5 policy count does not match closure assignments: " +
                    "expected=${wave.assignedGapCount}, " +
                    "actual=${policies.size}."
        }

        require(
            policies
                .map { policy ->
                    policy.identityKey
                }
                .distinct()
                .size ==
                    policies.size
        ) {
            "Wave 5 contains duplicate policy identities."
        }

        require(
            policies.all { policy ->
                policy.active &&
                        policy.complete
            }
        ) {
            "Wave 5 contains an inactive or incomplete policy."
        }

        val expectedBatchKeys =
            wave.assignments
                .map { assignment ->
                    assignment.sourceBatchKey
                }
                .toSet()

        val actualBatchKeys =
            batches
                .map { batch ->
                    batch.sourceImplementationBatchKey
                }
                .toSet()

        require(
            actualBatchKeys ==
                    expectedBatchKeys
        ) {
            "Wave-5 curated batches do not exactly cover the planned " +
                    "source implementation batches."
        }

        return batches
    }

    private fun createBatch(
        sourceBatchKey: String,

        assignments:
        List<CanonicalBoundedSemanticPolicyClosureAssignment>,

        concreteValueIndex:
        CanonicalConcreteFamilyAxisValueIndex
    ): CanonicalCuratedSemanticPolicyBatch {
        require(sourceBatchKey.isNotBlank())
        require(assignments.isNotEmpty())

        require(
            assignments.all { assignment ->
                assignment.sourceBatchKey ==
                        sourceBatchKey
            }
        ) {
            "Wave-5 batch '$sourceBatchKey' contains assignments from " +
                    "another source batch."
        }

        val categories =
            assignments
                .map { assignment ->
                    assignment.category
                }
                .distinct()

        val axes =
            assignments
                .map { assignment ->
                    assignment.axis
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
                            > { assignment ->
                        assignment.familyKey
                    }.thenBy { assignment ->
                        assignment.axis
                    }.thenBy { assignment ->
                        assignment.gapKey
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

        require(
            policies
                .map { policy ->
                    policy.identityKey
                }
                .distinct()
                .size ==
                    policies.size
        ) {
            "Curated Wave-5 batch '$sourceBatchKey' contains duplicate " +
                    "policy identities."
        }

        return CanonicalCuratedSemanticPolicyBatch(
            version =
                CanonicalCuratedSemanticPolicyBatch
                    .CURRENT_VERSION,

            curationId =
                "curated-wave-005-$sourceBatchKey",

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
                policies.all { policy ->
                    policy.active &&
                            policy.complete
                },

            valid =
                policies.isNotEmpty() &&
                        policies.all { policy ->
                            policy.active &&
                                    policy.complete
                        } &&
                        policies
                            .map { policy ->
                                policy.identityKey
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
            "No canonical concrete value coverage exists for Wave-5 " +
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
                    "Allowed values are derived from validated canonical " +
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
            5

        const val WAVE_SOURCE =
            "ShopMe bounded semantic policy curation wave 5"
    }
}