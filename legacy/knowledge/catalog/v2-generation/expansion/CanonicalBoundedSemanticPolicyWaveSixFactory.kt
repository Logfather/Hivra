package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatch
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatchResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation
.CanonicalCuratedSemanticPolicyBatch
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyType

class CanonicalBoundedSemanticPolicyWaveSixFactory(
    private val overrideRegistry:
    CanonicalBoundedSemanticPolicyOverrideRegistry =
        CanonicalBoundedSemanticPolicyOverrideRegistry()
) {

    fun createBatches(
        openImplementationBatches:
        CanonicalSemanticPolicyImplementationBatchResult,

        concreteValueIndex:
        CanonicalConcreteFamilyAxisValueIndex
    ): List<CanonicalCuratedSemanticPolicyBatch> {
        require(openImplementationBatches.valid)
        require(openImplementationBatches.completeGapCoverage)
        require(openImplementationBatches.missingPolicyGapCount > 0)

        val batches =
            openImplementationBatches.batches
                .sortedBy { batch ->
                    batch.batchKey
                }
                .map { batch ->
                    createBatch(
                        openBatch =
                            batch,

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
                    openImplementationBatches.missingPolicyGapCount
        ) {
            "Wave 6 must curate every currently open policy gap: " +
                    "expected=${openImplementationBatches.missingPolicyGapCount}, " +
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
            "Wave 6 contains duplicate family-axis policy identities."
        }

        require(
            batches.all { batch ->
                batch.valid &&
                        batch.complete
            }
        ) {
            "Wave 6 contains an invalid or incomplete curated batch."
        }

        require(
            policies.all { policy ->
                policy.active &&
                        policy.complete
            }
        ) {
            "Wave 6 contains an inactive or incomplete policy."
        }

        return batches
    }

    private fun createBatch(
        openBatch:
        CanonicalSemanticPolicyImplementationBatch,

        concreteValueIndex:
        CanonicalConcreteFamilyAxisValueIndex
    ): CanonicalCuratedSemanticPolicyBatch {
        require(openBatch.complete)
        require(openBatch.gaps.isNotEmpty())

        val policies =
            openBatch.gaps
                .sortedBy { gap ->
                    gap.gapKey
                }
                .map { gap ->
                    val familyKey =
                        familyKeyFrom(
                            gapKey =
                                gap.gapKey,

                            expectedAxis =
                                openBatch.axis.name
                        )

                    createPolicy(
                        familyKey =
                            familyKey,

                        axis =
                            openBatch.axis,

                        gapKey =
                            gap.gapKey,

                        concreteValueIndex =
                            concreteValueIndex
                    )
                }

        require(
            policies.size ==
                    openBatch.gaps.size
        )

        require(
            policies
                .map { policy ->
                    policy.identityKey
                }
                .distinct()
                .size ==
                    policies.size
        ) {
            "Wave-6 batch '${openBatch.batchKey}' contains duplicate " +
                    "policy identities."
        }

        return CanonicalCuratedSemanticPolicyBatch(
            version =
                CanonicalCuratedSemanticPolicyBatch
                    .CURRENT_VERSION,

            curationId =
                "$CURATION_ID_PREFIX${openBatch.batchKey}",

            sourceImplementationBatchKey =
                openBatch.batchKey,

            category =
                openBatch.category,

            axis =
                openBatch.axis.name,

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
        familyKey: String,

        axis:
        de.shopme.testing.system.tools.knowledge.catalog.expansion.family
        .CanonicalProductFamilyVariantAxis,

        gapKey: String,

        concreteValueIndex:
        CanonicalConcreteFamilyAxisValueIndex
    ): CanonicalFamilyAxisSemanticPolicyEntry {
        val override =
            overrideRegistry.findOverride(
                familyKey =
                    familyKey,

                axis =
                    axis
            )

        if (override != null) {
            return override
        }

        require(
            concreteValueIndex.contains(
                familyKey =
                    familyKey,

                axis =
                    axis.name
            )
        ) {
            "No concrete canonical value coverage exists for final " +
                    "Wave-6 gap '$gapKey'."
        }

        val allowedValues =
            requireNotNull(
                concreteValueIndex.values(
                    familyKey =
                        familyKey,

                    axis =
                        axis.name
                )
            )

        return if (allowedValues.isEmpty()) {
            CanonicalFamilyAxisSemanticPolicyEntry(
                familyKey =
                    familyKey,

                axis =
                    axis,

                policyType =
                    CanonicalFamilyAxisSemanticPolicyType
                        .NOT_APPLICABLE,

                allowedValues =
                    emptyList(),

                rationale =
                    "No applicable canonical variant values remain for " +
                            "family '$familyKey' on axis '${axis.name}'.",

                source =
                    WAVE_SOURCE,

                active =
                    true
            )
        } else {
            CanonicalFamilyAxisSemanticPolicyEntry(
                familyKey =
                    familyKey,

                axis =
                    axis,

                policyType =
                    CanonicalFamilyAxisSemanticPolicyType
                        .CURATED_ALLOWED_VALUES,

                allowedValues =
                    allowedValues,

                rationale =
                    "Allowed values are derived from validated canonical " +
                            "concrete variant-value coverage for final closure " +
                            "of family '$familyKey' on axis '${axis.name}'.",

                source =
                    WAVE_SOURCE,

                active =
                    true
            )
        }
    }

    private fun familyKeyFrom(
        gapKey: String,
        expectedAxis: String
    ): String {
        require(gapKey.isNotBlank())

        val separatorIndex =
            gapKey.lastIndexOf(
                IDENTITY_SEPARATOR
            )

        require(separatorIndex > 0) {
            "Invalid family-axis gap identity '$gapKey'."
        }

        val familyKey =
            gapKey
                .substring(
                    startIndex =
                        0,

                    endIndex =
                        separatorIndex
                )
                .trim()

        val axis =
            gapKey
                .substring(
                    startIndex =
                        separatorIndex +
                                IDENTITY_SEPARATOR.length
                )
                .trim()

        require(familyKey.isNotBlank())
        require(axis == expectedAxis) {
            "Gap '$gapKey' belongs to axis '$axis', but batch expects " +
                    "'$expectedAxis'."
        }

        return familyKey
    }

    companion object {

        const val WAVE_NUMBER =
            6

        const val CURATION_ID_PREFIX =
            "curated-wave-006-"

        const val WAVE_SOURCE =
            "ShopMe bounded semantic policy curation wave 6"

        private const val IDENTITY_SEPARATOR =
            "::"
    }
}