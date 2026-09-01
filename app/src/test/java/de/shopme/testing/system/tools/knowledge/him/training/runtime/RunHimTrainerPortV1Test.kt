package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionAssignmentV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionManifestV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionValidationResultV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1
import de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerPortV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingMissionV1
import java.lang.reflect.Modifier
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private fun digest(seed: String): HimSha256 = HimSha256(
    java.security.MessageDigest.getInstance("SHA-256")
        .digest(seed.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) },
)

class RunHimTrainerPortV1Test {
    @Test
    fun validMissionConfigurationModelAndTrainInputCreateRequest() {
        val fixture = Fixture.allTrain()
        val request = fixture.request()

        assertEquals(HimTrainerPortV1.CONTRACT_ID, requestContractId())
        assertEquals(HimTrainerPortV1.VERSION, requestVersion())
        assertEquals(HimTrainerPortV1.STATE, requestState())
        assertEquals(fixture.mission, request.mission)
        assertEquals(fixture.configuration, request.configuration)
        assertEquals(fixture.modelBinding, request.modelBinding)
        assertEquals(3, request.trainAssignments.size)
        assertTrue(request.trainAssignments.all { it.partition == HimTrainingPartitionV1.TRAIN })
    }

    @Test
    fun identicalInputsProduceIdenticalRequestIdentity() {
        val fixture = Fixture.allTrain()

        assertEquals(fixture.request().logicalDigest, fixture.request().logicalDigest)
        assertEquals(fixture.request().requestReference, fixture.request().requestReference)
    }

    @Test
    fun configurationMismatchFailsClosed() {
        val fixture = Fixture.allTrain()

        assertFailsWith<IllegalArgumentException> {
            fixture.request(configuration = Fixture.configuration("different-configuration"))
        }
    }

    @Test
    fun modelBindingMismatchFailsClosed() {
        val fixture = Fixture.allTrain()

        assertFailsWith<IllegalArgumentException> {
            fixture.request(modelBinding = Fixture.modelBinding("different-model"))
        }
    }

    @Test
    fun requestCannotBeConstructedWithoutTrainingMission() {
        val constructors = HimTrainerPortV1.Request::class.java.declaredConstructors
            .filterNot { it.isSynthetic }

        assertEquals(1, constructors.size)
        assertTrue(Modifier.isPrivate(constructors.single().modifiers))
        assertEquals(HimTrainingMissionV1.Mission::class.java, constructors.single().parameterTypes.first())
    }

    @Test
    fun directReadyAuthorizationFactoryDoesNotExist() {
        val parameterTypes = requestFactory().parameterTypes.toSet()

        assertFalse(parameterTypes.contains(HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready::class.java))
    }

    @Test
    fun directCorpusPartitionOrLeakageAuthorizationFactoryDoesNotExist() {
        val parameterTypes = requestFactory().parameterTypes.toSet()

        assertFalse(parameterTypes.any { it.simpleName.contains("Corpus") })
        assertTrue(parameterTypes.contains(HimTrainingPartitionManifestV1::class.java))
        assertFalse(parameterTypes.any { it.simpleName.contains("Leakage") })
    }

    @Test
    fun emptyTrainInputFailsClosed() {
        val fixture = Fixture.allTrain()
        val emptyTrainManifest = HimTrainingPartitionManifestV1.create(
            fixture.manifest.assignments.map { it.copy(partition = HimTrainingPartitionV1.HOLDOUT) },
        )

        assertFailsWith<IllegalArgumentException> {
            fixture.request(partitionManifest = emptyTrainManifest)
        }
    }

    @Test
    fun nonTrainPartitionInputCannotEnterRequest() {
        val fixture = Fixture.mixedPartitions()
        val request = fixture.request()

        assertTrue(request.trainAssignments.isNotEmpty())
        assertTrue(request.trainAssignments.all { it.partition == HimTrainingPartitionV1.TRAIN })
        assertEquals(1, request.trainAssignments.size)
    }

    @Test
    fun validationInputCannotEnterRequest() {
        val fixture = Fixture.mixedPartitions()
        val request = fixture.request()

        assertFalse(request.trainAssignments.any { it.partition == HimTrainingPartitionV1.VALIDATION })
    }

    @Test
    fun holdoutInputCannotEnterRequest() {
        val fixture = Fixture.mixedPartitions()
        val request = fixture.request()

        assertFalse(request.trainAssignments.any { it.partition == HimTrainingPartitionV1.HOLDOUT })
    }

    @Test
    fun trainOrderingIsPreserved() {
        val fixture = Fixture.allTrain()
        val request = fixture.request()

        assertEquals(
            fixture.manifest.assignments.map { it.record.recordReference },
            request.trainAssignments.map { it.record.recordReference },
        )
    }

    @Test
    fun positiveAndNegativeMembershipIdentityIsPreserved() {
        val request = Fixture.allTrain().request()

        assertEquals(1, request.trainAssignments.count { it.record is HimTrainingPartitionRecordV1.Positive })
        assertEquals(2, request.trainAssignments.count { it.record is HimTrainingPartitionRecordV1.Negative })
    }

    @Test
    fun manyToOneRelationshipsAreNotCollapsed() {
        val request = Fixture.allTrain().request()
        val negatives = request.trainAssignments
            .mapNotNull { it.record as? HimTrainingPartitionRecordV1.Negative }

        assertEquals(2, negatives.size)
        assertEquals(2, negatives.map { it.recordReference }.distinct().size)
        assertEquals(1, request.trainAssignments.map { it.groupReference }.distinct().size)
    }

    @Test
    fun fakeTrainerReceivesExactlyTheAuthorizedRequest() {
        val request = Fixture.allTrain().request()
        var received: HimTrainerPortV1.Request? = null
        val trainer = HimTrainerPortV1 { received = it; HimTrainerPortV1.Result.Completed(it.logicalDigest) }

        trainer.execute(request)

        assertEquals(request, received)
    }

    @Test
    fun fakeTrainerCanReturnTypedCompletedResult() {
        val request = Fixture.allTrain().request()
        val result = HimTrainerPortV1 { HimTrainerPortV1.Result.Completed(it.logicalDigest) }.execute(request)

        assertIs<HimTrainerPortV1.Result.Completed>(result)
        assertEquals(request.logicalDigest, result.requestDigest)
    }

    @Test
    fun fakeTrainerCanReturnTypedFailedResult() {
        val result = HimTrainerPortV1 {
            HimTrainerPortV1.Result.Failed(HimTrainerPortV1.FailureReasonV1.TRAINER_EXECUTION_FAILED, "fixture")
        }.execute(Fixture.allTrain().request())

        assertIs<HimTrainerPortV1.Result.Failed>(result)
        assertEquals(HimTrainerPortV1.FailureReasonV1.TRAINER_EXECUTION_FAILED, result.reason)
        assertEquals("fixture", result.safeContext)
    }

    @Test
    fun noPersistenceApiExists() {
        assertFalse(publicNames().any { it.contains("persist", ignoreCase = true) })
        assertFalse(publicNames().any { it.contains("write", ignoreCase = true) })
    }

    @Test
    fun noCheckpointOrModelWriteApiExists() {
        assertFalse(publicNames().any { it.contains("checkpoint", ignoreCase = true) })
        assertFalse(publicNames().any { it.contains("model", ignoreCase = true) && it.contains("write", ignoreCase = true) })
    }

    @Test
    fun noEvaluationApiExists() {
        assertFalse(publicNames().any { it.contains("evaluat", ignoreCase = true) })
    }

    @Test
    fun noMlFrameworkOrDeviceSymbolsExistInPublicApi() {
        val publicText = publicNames().joinToString(" ")
        assertFalse(publicText.contains("pytorch", ignoreCase = true))
        assertFalse(publicText.contains("tensorflow", ignoreCase = true))
        assertFalse(publicText.contains("cuda", ignoreCase = true))
        assertFalse(publicText.contains("mps", ignoreCase = true))
        assertFalse(publicText.contains("device", ignoreCase = true))
    }

    @Test
    fun missionConfigurationReferenceCompatibilityIsEnforced() {
        val fixture = Fixture.allTrain()

        assertEquals(fixture.configuration.logicalDigest, fixture.mission.trainingConfiguration.digest)
        assertFailsWith<IllegalArgumentException> {
            fixture.request(configuration = Fixture.configuration("unbound"))
        }
    }

    @Test
    fun missionModelReferenceCompatibilityIsEnforced() {
        val fixture = Fixture.allTrain()

        assertEquals(fixture.modelBinding.logicalDigest, fixture.mission.modelBinding.digest)
        assertFailsWith<IllegalArgumentException> {
            fixture.request(modelBinding = Fixture.modelBinding("unbound"))
        }
    }

    @Test
    fun requestExposesOnlyTrainPartitionContract() {
        assertEquals("YES", HimTrainerPortV1.TRAINER_PORT_MUST_REQUIRE_TRAINING_MISSION)
        assertEquals("TRAIN_ONLY", HimTrainerPortV1.TRAINER_REQUEST_PARTITION)
        assertEquals(0, HimTrainerPortV1.VALIDATION_INPUTS_IN_TRAINER_REQUEST)
        assertEquals(0, HimTrainerPortV1.HOLDOUT_INPUTS_IN_TRAINER_REQUEST)
    }

    @Test
    fun resultDoesNotClaimDurableModelArtifact() {
        val names = HimTrainerPortV1.Result.Completed::class.java.declaredFields.map { it.name }

        assertTrue(names.contains("requestDigest"))
        assertFalse(names.any { it.contains("artifact", ignoreCase = true) })
        assertFalse(names.any { it.contains("checkpoint", ignoreCase = true) })
    }

    private fun requestFactory() = HimTrainerPortV1.Request.Companion::class.java.declaredMethods
        .single { it.name == "create" }

    private fun publicNames(): Set<String> = buildSet {
        addAll(HimTrainerPortV1::class.java.methods.filter { Modifier.isPublic(it.modifiers) }.map { it.name })
        addAll(HimTrainerPortV1.Request::class.java.methods.filter { Modifier.isPublic(it.modifiers) }.map { it.name })
        addAll(HimTrainerPortV1.Result.Completed::class.java.methods.filter { Modifier.isPublic(it.modifiers) }.map { it.name })
        addAll(HimTrainerPortV1.Result.Failed::class.java.methods.filter { Modifier.isPublic(it.modifiers) }.map { it.name })
    }

    private fun requestContractId() = HimTrainerPortV1.CONTRACT_ID
    private fun requestVersion() = HimTrainerPortV1.VERSION
    private fun requestState() = HimTrainerPortV1.STATE

    private data class Fixture(
        val configuration: HimTrainingConfigurationV1,
        val modelBinding: HimModelBindingV1,
        val mission: HimTrainingMissionV1.Mission,
        val manifest: HimTrainingPartitionManifestV1,
    ) {
        fun request(
            configuration: HimTrainingConfigurationV1 = this.configuration,
            modelBinding: HimModelBindingV1 = this.modelBinding,
            partitionManifest: HimTrainingPartitionManifestV1 = this.manifest,
        ) = HimTrainerPortV1.Request.create(mission, configuration, modelBinding, partitionManifest)

        companion object {
            fun allTrain() = create(listOf(HimTrainingPartitionV1.TRAIN, HimTrainingPartitionV1.TRAIN, HimTrainingPartitionV1.TRAIN))

            fun mixedPartitions() = create(
                listOf(HimTrainingPartitionV1.TRAIN, HimTrainingPartitionV1.VALIDATION, HimTrainingPartitionV1.HOLDOUT),
            )

            private fun create(partitions: List<HimTrainingPartitionV1>): Fixture {
                val positive = positiveExample()
                val negatives = HimNegativeTrainingExamplePolicyV1.derive(positive).take(2)
                require(negatives.size == 2)
                val group = HimTrainingFamilyGroupReferenceV1.canonical(CANONICAL_ID)
                val records = listOf(HimTrainingPartitionRecordV1.Positive(positive)) +
                    negatives.map { HimTrainingPartitionRecordV1.Negative(it) }
                val manifest = HimTrainingPartitionManifestV1.create(
                    records.zip(partitions).map { (record, partition) ->
                        HimTrainingPartitionAssignmentV1(record, group, partition)
                    },
                )
                val configuration = configuration("configuration")
                val modelBinding = modelBinding("model")
                val mission = HimTrainingMissionV1.create(
                    HimTrainingMissionV1.Request(
                        readiness = readiness(manifest),
                        trainingConfiguration = HimTrainingMissionV1.TrainingConfigurationReference(
                            configuration.logicalDigest,
                        ),
                        modelBinding = HimTrainingMissionV1.ModelBindingReference(modelBinding.logicalDigest),
                        implementationBinding = HimTrainingMissionV1.ImplementationBindingReference(
                            digest("implementation"),
                        ),
                    ),
                )
                return Fixture(configuration, modelBinding, mission, manifest)
            }

            private fun positiveExample() = HimTrainingExampleV1.create(
                taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
                input = HimTrainingInputV1(
                    observedTerm = "Fixture variant",
                    normalizedObservedTerm = "fixture variant",
                ),
                target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID)),
            )

            private fun readiness(manifest: HimTrainingPartitionManifestV1) =
                HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready(
                    snapshotBinding = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1(
                        snapshotId = "training-corpus:v1:${digest("snapshot").value}",
                        corpusLogicalDigest = digest("snapshot"),
                    ),
                    partitionManifestLogicalDigest = manifest.logicalDigest,
                    partitionCounters = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters.from(
                        manifest.assignments,
                    ),
                    partitionPolicyVersion = HimTrainingPartitionContractV1.POLICY_VERSION,
                    leakageValidationResult = HimTrainingPartitionValidationResultV1(true, emptyList()),
                )

            fun configuration(seed: String) = HimTrainingConfigurationV1.create(
                seed = 7,
                epochs = 3,
                microBatchSize = 2,
                gradientAccumulationSteps = 1,
                learningRate = BigDecimal("0.01"),
                optimizerId = "fixture:optimizer:$seed",
            )

            fun modelBinding(seed: String) = HimModelBindingV1.create(
                modelFamilyId = "fixture:model-family:$seed",
                baseModelId = "fixture:base-model:$seed",
                baseModelArtifactDigest = digest("$seed-base"),
                tokenizerId = "fixture:tokenizer:$seed",
                tokenizerArtifactDigest = digest("$seed-tokenizer"),
                modelConfigurationArtifactDigest = digest("$seed-config"),
            )

            private val CANONICAL_ID = HimEntityId("Abc123")
        }
    }
}
