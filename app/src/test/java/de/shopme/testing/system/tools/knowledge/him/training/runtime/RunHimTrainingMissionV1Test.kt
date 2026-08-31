package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionLeakageDiagnosticV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionLeakageLevelV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionValidationResultV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingMissionV1
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private fun digest(seed: String): String =
    java.security.MessageDigest.getInstance("SHA-256")
        .digest(seed.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

class RunHimTrainingMissionV1Test {
    @Test
    fun validReadyAuthorizesMission() {
        val mission = create(Fixture.ready)

        assertEquals(HimTrainingMissionV1.CONTRACT_ID, mission.contractId)
        assertEquals(HimTrainingMissionV1.VERSION, mission.version)
        assertEquals(HimTrainingMissionV1.STATE, mission.state)
        assertEquals("training-mission:v1:${mission.logicalDigest.value}", mission.missionReference)
    }

    @Test
    fun equivalentInputsProduceIdenticalMissionIdentity() {
        val first = create(Fixture.ready)
        val second = create(Fixture.ready)

        assertEquals(first.missionReference, second.missionReference)
        assertEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun changingTrainingConfigurationChangesMissionIdentity() {
        val first = create(Fixture.ready)
        val second = create(Fixture.ready, configurationSeed = "different-configuration")

        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun changingModelBindingChangesMissionIdentity() {
        val first = create(Fixture.ready)
        val second = create(Fixture.ready, modelSeed = "different-model")

        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun changingImplementationBindingChangesMissionIdentity() {
        val first = create(Fixture.ready)
        val second = create(Fixture.ready, implementationSeed = "different-implementation")

        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun changingReadyDatasetBindingChangesMissionIdentity() {
        val changedReady = Fixture.ready.copy(
            snapshotBinding = Fixture.snapshot("different-dataset"),
        )

        assertNotEquals(create(Fixture.ready).logicalDigest, create(changedReady).logicalDigest)
    }

    @Test
    fun blankConfigurationBindingFailsClosed() {
        assertFailsWith<IllegalArgumentException> {
            HimTrainingMissionV1.TrainingConfigurationReference(HimSha256(""))
        }
    }

    @Test
    fun invalidModelBindingFailsClosed() {
        assertFailsWith<IllegalArgumentException> {
            HimTrainingMissionV1.ModelBindingReference(HimSha256("not-a-sha256"))
        }
    }

    @Test
    fun invalidImplementationBindingFailsClosed() {
        assertFailsWith<IllegalArgumentException> {
            HimTrainingMissionV1.ImplementationBindingReference(HimSha256("0".repeat(64)))
        }
    }

    @Test
    fun publicAuthorizationFactoryAcceptsOnlyTypedReadyRequest() {
        val createMethods = HimTrainingMissionV1::class.java.declaredMethods.filter {
            it.name == "create" && Modifier.isPublic(it.modifiers)
        }

        assertEquals(1, createMethods.size)
        assertContentEquals(
            arrayOf(HimTrainingMissionV1.Request::class.java),
            createMethods.single().parameterTypes,
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready::class.java,
            HimTrainingMissionV1.Request::class.java.declaredConstructors
                .single { it.parameterTypes.size == 4 }
                .parameterTypes.first(),
        )
    }

    @Test
    fun readyBindingsArePreservedWithoutReconstruction() {
        val mission = create(Fixture.ready)
        val binding = mission.readinessBinding

        assertEquals(Fixture.ready.snapshotBinding, binding.snapshotBinding)
        assertEquals(Fixture.ready.partitionManifestLogicalDigest, binding.partitionManifestLogicalDigest)
        assertEquals(Fixture.ready.partitionCounters, binding.partitionCounters)
        assertEquals(Fixture.ready.partitionPolicyVersion, binding.partitionPolicyVersion)
        assertEquals(Fixture.ready.leakageValidationResult.valid, binding.leakageValidationValid)
        assertTrue(binding.leakageValidationDigest.value.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun invalidReadyLeakageAuthorizationFailsClosed() {
        val invalidReady = Fixture.ready.copy(
            leakageValidationResult = HimTrainingPartitionValidationResultV1(
                valid = false,
                diagnostics = listOf(
                    HimTrainingPartitionLeakageDiagnosticV1(
                        level = HimTrainingPartitionLeakageLevelV1.INVALID_DETERMINISTIC_ASSIGNMENT,
                        fatal = true,
                        key = "invalid-ready",
                        recordReferences = listOf("record:v1"),
                        message = "invalid readiness fixture",
                    ),
                ),
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            create(invalidReady)
        }
    }

    @Test
    fun missionCreationHasNoPersistenceTrainingOrInferenceEntryPoint() {
        val forbiddenNames = setOf("persist", "write", "train", "infer", "execute")
        assertTrue(
            HimTrainingMissionV1::class.java.declaredMethods.none { method ->
                Modifier.isPublic(method.modifiers) && method.name in forbiddenNames
            },
        )
        assertFalse(Fixture.ready.leakageValidationResult.diagnostics.any { it.fatal })
    }

    private fun create(
        ready: HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready,
        configurationSeed: String = "configuration",
        modelSeed: String = "model",
        implementationSeed: String = "implementation",
    ): HimTrainingMissionV1.Mission =
        HimTrainingMissionV1.create(
            HimTrainingMissionV1.Request(
                readiness = ready,
                trainingConfiguration = HimTrainingMissionV1.TrainingConfigurationReference(
                    HimSha256(digest(configurationSeed)),
                ),
                modelBinding = HimTrainingMissionV1.ModelBindingReference(
                    HimSha256(digest(modelSeed)),
                ),
                implementationBinding = HimTrainingMissionV1.ImplementationBindingReference(
                    HimSha256(digest(implementationSeed)),
                ),
            ),
        )

    private object Fixture {
        val ready = HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready(
            snapshotBinding = snapshot("default"),
            partitionManifestLogicalDigest = HimSha256("b".repeat(64)),
            partitionCounters = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters(
                total = 4,
                train = 2,
                validation = 1,
                holdout = 1,
            ),
            partitionPolicyVersion = HimTrainingPartitionContractV1.POLICY_VERSION,
            leakageValidationResult = HimTrainingPartitionValidationResultV1(
                valid = true,
                diagnostics = emptyList(),
            ),
        )

        fun snapshot(seed: String) =
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1(
                snapshotId = "training-corpus:v1:${digest(seed)}",
                corpusLogicalDigest = HimSha256(digest(seed)),
            )

    }
}
