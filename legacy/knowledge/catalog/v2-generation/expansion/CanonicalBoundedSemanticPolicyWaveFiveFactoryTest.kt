package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatch
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalConcreteVariantValueCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBoundedSemanticPolicyWaveFiveFactoryTest {

    @Test
    fun curateEveryAssignedWaveFiveGap() {
        val closurePlan =
            readClosurePlan()

        val expectedWave =
            closurePlan.waves
                .single { wave ->
                    wave.waveNumber ==
                            CanonicalBoundedSemanticPolicyWaveFiveFactory
                                .WAVE_NUMBER
                }

        val batches =
            createBatches()

        val policies =
            batches.flatMap { batch ->
                batch.policies
            }

        assertEquals(
            expectedWave.assignedBatchCount,
            batches.size
        )

        assertEquals(
            expectedWave.assignedGapCount,
            policies.size
        )

        assertEquals(
            expectedWave.assignedGapCount,
            policies
                .map { policy ->
                    policy.identityKey
                }
                .distinct()
                .size
        )

        assertTrue(
            batches.all { batch ->
                batch.valid &&
                        batch.complete
            }
        )

        assertTrue(
            policies.all { policy ->
                policy.active &&
                        policy.complete
            }
        )

        val supportedSources =
            setOf(
                CanonicalBoundedSemanticPolicyWaveFiveFactory
                    .WAVE_SOURCE,

                CanonicalBoundedSemanticPolicyOverrideRegistry
                    .OVERRIDE_SOURCE
            )

        assertTrue(
            policies.all { policy ->
                policy.source in supportedSources
            },
            "Wave 5 contains a policy with an unsupported source."
        )
    }

    @Test
    fun preserveEveryClosureAssignmentIdentity() {
        val expectedIdentities =
            readClosurePlan()
                .waves
                .single { wave ->
                    wave.waveNumber ==
                            CanonicalBoundedSemanticPolicyWaveFiveFactory
                                .WAVE_NUMBER
                }
                .assignments
                .map { assignment ->
                    assignment.identityKey
                }
                .sorted()

        val actualIdentities =
            createBatches()
                .flatMap { batch ->
                    batch.policies
                }
                .map { policy ->
                    policy.identityKey
                }
                .sorted()

        assertEquals(
            expectedIdentities,
            actualIdentities
        )
    }

    @Test
    fun preserveEveryCompleteSourceBatch() {
        val expectedBatchKeys =
            readClosurePlan()
                .waves
                .single { wave ->
                    wave.waveNumber ==
                            CanonicalBoundedSemanticPolicyWaveFiveFactory
                                .WAVE_NUMBER
                }
                .assignments
                .map { assignment ->
                    assignment.sourceBatchKey
                }
                .toSet()

        val actualBatchKeys =
            createBatches()
                .map { batch ->
                    batch.sourceImplementationBatchKey
                }
                .toSet()

        assertEquals(
            expectedBatchKeys,
            actualBatchKeys
        )
    }

    @Test
    fun useOnlyClosedPolicyTypes() {
        val policies =
            createBatches()
                .flatMap { batch ->
                    batch.policies
                }

        assertTrue(
            policies.all { policy ->
                policy.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES ||
                        policy.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE
            }
        )

        assertTrue(
            policies
                .filter { policy ->
                    policy.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .CURATED_ALLOWED_VALUES
                }
                .all { policy ->
                    policy.allowedValues.isNotEmpty()
                }
        )

        assertTrue(
            policies
                .filter { policy ->
                    policy.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .NOT_APPLICABLE
                }
                .all { policy ->
                    policy.allowedValues.isEmpty()
                }
        )
    }

    @Test
    fun referenceOnlyCanonicalVariantValues() {
        val policies =
            createBatches()
                .flatMap { batch ->
                    batch.policies
                }

        val result =
            CanonicalSemanticPolicyBatchVocabularyValidator()
                .validate(
                    entries =
                        policies
                )

        assertTrue(
            result.valid,
            result.issues.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        assertEquals(
            0,
            result.issueCount
        )
    }

    @Test
    fun createDeterministically() {
        assertEquals(
            createBatches(),
            createBatches()
        )
    }

    private fun createBatches():
            List<CanonicalCuratedSemanticPolicyBatch> {
        val projectDirectory =
            resolveProjectDirectory()

        val closurePlan =
            CanonicalBoundedSemanticPolicyClosurePlanReader()
                .read(
                    File(
                        projectDirectory,
                        CLOSURE_PLAN_PATH
                    )
                )

        val concreteValueCoverage =
            CanonicalConcreteVariantValueCoverageReader()
                .read(
                    File(
                        projectDirectory,
                        CONCRETE_VALUE_COVERAGE_PATH
                    )
                )

        val concreteValueIndex =
            CanonicalConcreteFamilyAxisValueIndexFactory()
                .create(
                    coverage =
                        concreteValueCoverage
                )

        return CanonicalBoundedSemanticPolicyWaveFiveFactory()
            .createBatches(
                closurePlan =
                    closurePlan,

                concreteValueIndex =
                    concreteValueIndex
            )
    }

    private fun readClosurePlan():
            CanonicalBoundedSemanticPolicyClosurePlan =
        CanonicalBoundedSemanticPolicyClosurePlanReader()
            .read(
                File(
                    resolveProjectDirectory(),
                    CLOSURE_PLAN_PATH
                )
            )

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                )
            ).canonicalFile

        return when {
            REQUIRED_PATHS.all { relativePath ->
                File(
                    workingDirectory,
                    relativePath
                ).isFile
            } ->
                workingDirectory

            workingDirectory.name ==
                    "app" ->
                requireNotNull(
                    workingDirectory.parentFile
                )

            else ->
                error(
                    "Could not resolve ShopMe project directory from: " +
                            workingDirectory.absolutePath
                )
        }
    }

    private companion object {

        const val CLOSURE_PLAN_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-closure/" +
                    "canonical-bounded-semantic-policy-closure-plan.json"

        const val CONCRETE_VALUE_COVERAGE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-concrete-variant-value-coverage.json"

        val REQUIRED_PATHS =
            listOf(
                CLOSURE_PLAN_PATH,
                CONCRETE_VALUE_COVERAGE_PATH
            )
    }
}