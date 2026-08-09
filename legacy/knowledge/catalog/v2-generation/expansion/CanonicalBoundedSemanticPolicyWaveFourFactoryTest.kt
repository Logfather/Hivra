package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatch
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalConcreteVariantValueCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBoundedSemanticPolicyWaveFourFactoryTest {

    @Test
    fun curateEveryAssignedWaveFourGap() {
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

        val expectedPolicyCount =
            closurePlan.waves
                .single {
                    it.waveNumber ==
                            CanonicalBoundedSemanticPolicyWaveFourFactory
                                .WAVE_NUMBER
                }
                .assignedGapCount

        val batches =
            createBatches()

        val policies =
            batches.flatMap {
                it.policies
            }

        assertEquals(
            expectedPolicyCount,
            policies.size
        )

        assertEquals(
            expectedPolicyCount,
            policies
                .map {
                    it.identityKey
                }
                .distinct()
                .size
        )

        assertTrue(
            batches.all {
                it.valid &&
                        it.complete
            }
        )

        assertTrue(
            policies.all {
                it.active &&
                        it.complete
            }
        )

        val supportedSources =
            setOf(
                CanonicalBoundedSemanticPolicyWaveFourFactory
                    .WAVE_SOURCE,

                CanonicalBoundedSemanticPolicyOverrideRegistry
                    .OVERRIDE_SOURCE
            )

        assertTrue(
            policies.all { policy ->
                policy.source in supportedSources
            },
            "Wave 4 contains a policy with an unsupported source."
        )

        val policiesByIdentity =
            policies.associateBy { policy ->
                policy.identityKey
            }

        val expectedOverridePolicies:
                List<CanonicalFamilyAxisSemanticPolicyEntry> =
            CanonicalBoundedSemanticPolicyOverrideRegistry()
                .allOverrides()
                .filter { overridePolicy ->
                    overridePolicy.identityKey in
                            policiesByIdentity
                }
                .sortedWith(
                    compareBy<
                            CanonicalFamilyAxisSemanticPolicyEntry
                            > { policy ->
                        policy.familyKey
                    }.thenBy { policy ->
                        policy.axis.name
                    }
                )

        val actualOverridePolicies:
                List<CanonicalFamilyAxisSemanticPolicyEntry> =
            policies
                .filter { policy ->
                    policy.source ==
                            CanonicalBoundedSemanticPolicyOverrideRegistry
                                .OVERRIDE_SOURCE
                }
                .sortedWith(
                    compareBy<
                            CanonicalFamilyAxisSemanticPolicyEntry
                            > { policy ->
                        policy.familyKey
                    }.thenBy { policy ->
                        policy.axis.name
                    }
                )

        assertEquals(
            expectedOverridePolicies,
            actualOverridePolicies,
            "Wave 4 must contain exactly the overrides registered " +
                    "for its closure identities."
        )

    }

    @Test
    fun useOnlySupportedPolicyTypes() {
        val policies =
            createBatches()
                .flatMap {
                    it.policies
                }

        assertTrue(
            policies.all {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES ||
                        it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE
            }
        )

        assertTrue(
            policies
                .filter {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .CURATED_ALLOWED_VALUES
                }
                .all {
                    it.allowedValues.isNotEmpty()
                }
        )

        assertTrue(
            policies
                .filter {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .NOT_APPLICABLE
                }
                .all {
                    it.allowedValues.isEmpty()
                }
        )
    }

    @Test
    fun referenceOnlyCanonicalVariantValues() {
        val policies =
            createBatches()
                .flatMap {
                    it.policies
                }

        val result =
            CanonicalSemanticPolicyBatchVocabularyValidator()
                .validate(
                    entries = policies
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
    fun preserveEveryClosureAssignmentIdentity() {
        val projectDirectory =
            resolveProjectDirectory()

        val plan =
            CanonicalBoundedSemanticPolicyClosurePlanReader()
                .read(
                    File(
                        projectDirectory,
                        CLOSURE_PLAN_PATH
                    )
                )

        val expectedIdentities =
            plan.waves
                .single {
                    it.waveNumber == 4
                }
                .assignments
                .map {
                    it.identityKey
                }
                .sorted()

        val actualIdentities =
            createBatches()
                .flatMap {
                    it.policies
                }
                .map {
                    it.identityKey
                }
                .sorted()

        assertEquals(
            expectedIdentities,
            actualIdentities
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

        val manifestFile =
            File(
                projectDirectory,
                CURATED_MANIFEST_PATH
            )

        if (
            manifestFile.isFile &&
            manifestFile.length() > 0L
        ) {
            val closurePlan =
                CanonicalBoundedSemanticPolicyClosurePlanReader()
                    .read(
                        File(
                            projectDirectory,
                            CLOSURE_PLAN_PATH
                        )
                    )

            val wave =
                closurePlan.waves
                    .single { candidateWave ->
                        candidateWave.waveNumber ==
                                CanonicalBoundedSemanticPolicyWaveFourFactory
                                    .WAVE_NUMBER
                    }

            val expectedBatchKeys =
                wave.assignments
                    .map { assignment ->
                        assignment.sourceBatchKey
                    }
                    .toSet()

            val persistedWaveBatches =
                CanonicalCuratedSemanticPolicyBatchManifestReader()
                    .read(manifestFile)
                    .batches
                    .filter { batch ->
                        batch.sourceImplementationBatchKey in
                                expectedBatchKeys
                    }
                    .sortedBy { batch ->
                        batch.sourceImplementationBatchKey
                    }

            if (persistedWaveBatches.isNotEmpty()) {
                require(
                    persistedWaveBatches.size ==
                            wave.assignedBatchCount
                ) {
                    "Persisted Wave 4 is incomplete: " +
                            "expectedBatches=${wave.assignedBatchCount}, " +
                            "actualBatches=${persistedWaveBatches.size}."
                }

                require(
                    persistedWaveBatches.sumOf { batch ->
                        batch.policyCount
                    } ==
                            wave.assignedGapCount
                ) {
                    "Persisted Wave 4 has an unexpected policy count."
                }

                require(
                    persistedWaveBatches.all { batch ->
                        batch.valid &&
                                batch.complete
                    }
                ) {
                    "Persisted Wave 4 contains an invalid curated batch."
                }

                return persistedWaveBatches
            }
        }

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

        return CanonicalBoundedSemanticPolicyWaveFourFactory()
            .createBatches(
                closurePlan =
                    closurePlan,

                concreteValueIndex =
                    concreteValueIndex
            )
    }

    @Test
    fun applyEveryRegisteredOverrideAssignedToWaveFourExactly() {
        val waveFourPolicies =
            createBatches()
                .flatMap { batch ->
                    batch.policies
                }

        val policiesByIdentity =
            waveFourPolicies
                .associateBy { policy ->
                    policy.identityKey
                }

        val expectedOverrides:
                List<CanonicalFamilyAxisSemanticPolicyEntry> =
            CanonicalBoundedSemanticPolicyOverrideRegistry()
                .allOverrides()
                .filter { overridePolicy ->
                    overridePolicy.identityKey in
                            policiesByIdentity
                }
                .sortedWith(
                    compareBy<
                            CanonicalFamilyAxisSemanticPolicyEntry
                            > { policy ->
                        policy.familyKey
                    }.thenBy { policy ->
                        policy.axis.name
                    }
                )

        val actualOverrides:
                List<CanonicalFamilyAxisSemanticPolicyEntry> =
            waveFourPolicies
                .filter { policy ->
                    policy.source ==
                            CanonicalBoundedSemanticPolicyOverrideRegistry
                                .OVERRIDE_SOURCE
                }
                .sortedWith(
                    compareBy<
                            CanonicalFamilyAxisSemanticPolicyEntry
                            > { policy ->
                        policy.familyKey
                    }.thenBy { policy ->
                        policy.axis.name
                    }
                )

        assertEquals(
            expectedOverrides,
            actualOverrides,
            "Wave 4 must contain exactly the registered overrides " +
                    "whose identities are assigned to Wave 4."
        )

        expectedOverrides.forEach { expectedOverride ->
            val actualPolicy =
                requireNotNull(
                    policiesByIdentity[
                        expectedOverride.identityKey
                    ]
                ) {
                    "Wave 4 contains no policy for assigned override " +
                            "'${expectedOverride.identityKey}'."
                }

            assertEquals(
                expectedOverride,
                actualPolicy,
                "Applied Wave-4 override diverges from the override registry."
            )

            assertTrue(actualPolicy.active)
            assertTrue(actualPolicy.complete)
        }
    }

    fun containsRequiredArtifacts(
        directory: File
    ): Boolean {
        val closurePlanExists =
            File(
                directory,
                CLOSURE_PLAN_PATH
            ).isFile

        val persistedManifestExists =
            File(
                directory,
                CURATED_MANIFEST_PATH
            ).isFile

        val concreteCoverageExists =
            File(
                directory,
                CONCRETE_VALUE_COVERAGE_PATH
            ).isFile

        return closurePlanExists &&
                (
                        persistedManifestExists ||
                                concreteCoverageExists
                        )
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                )
            ).canonicalFile

        return when {
            containsRequiredArtifacts(
                workingDirectory
            ) ->
                workingDirectory

            workingDirectory.name ==
                    "app" &&
                    containsRequiredArtifacts(
                        requireNotNull(
                            workingDirectory.parentFile
                        )
                    ) ->
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

        const val CURATED_MANIFEST_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-curation/" +
                    "canonical-curated-semantic-policy-batches.json"

        const val CLOSURE_PLAN_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-closure/" +
                    "canonical-bounded-semantic-policy-closure-plan.json"

        const val CONCRETE_VALUE_COVERAGE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-concrete-variant-value-coverage.json"
    }
}