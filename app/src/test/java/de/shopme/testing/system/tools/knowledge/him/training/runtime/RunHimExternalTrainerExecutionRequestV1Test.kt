package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWParametersV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerExecutionRequestV1
import de.shopme.tools.knowledge.him.training.runtime.HimLocalModelArtifactResolverV1
import de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimOptimizerMappingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerPortV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolAdamWRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingMissionV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionAssignmentV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionManifestV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private fun executionDigest(seed: String): HimSha256 = HimSha256(
    MessageDigest.getInstance("SHA-256")
        .digest(seed.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) },
)

private val BASE_BYTES = "base-model-fixture".toByteArray()
private val TOKENIZER_BYTES = "tokenizer-fixture".toByteArray()
private val CONFIGURATION_BYTES = "configuration-fixture".toByteArray()

private fun resolvedArtifacts(
    root: Path,
    modelBinding: HimModelBindingV1,
    basePath: Path = Path.of("base-model.artifact"),
    tokenizerPath: Path = Path.of("tokenizer.artifact"),
    configurationPath: Path = Path.of("configuration.artifact"),
): HimLocalModelArtifactResolverV1.Resolution {
    write(root, basePath, BASE_BYTES)
    write(root, tokenizerPath, TOKENIZER_BYTES)
    write(root, configurationPath, CONFIGURATION_BYTES)
    return assertIs<HimLocalModelArtifactResolverV1.Result.Completed>(
        HimLocalModelArtifactResolverV1.resolve(
            HimLocalModelArtifactResolverV1.Request(
                modelBinding = modelBinding,
                artifactRoot = root,
                baseModelPath = basePath,
                tokenizerPath = tokenizerPath,
                modelConfigurationPath = configurationPath,
            ),
        ),
    ).value
}

private fun write(root: Path, relativePath: Path, bytes: ByteArray) {
    relativePath.parent?.let { Files.createDirectories(root.resolve(it)) }
    Files.write(root.resolve(relativePath), bytes)
}

class RunHimExternalTrainerExecutionRequestV1Test {
    @Test
    fun validProtocolAdamWBindingAndVerifiedArtifactsCreateRequest() = withFixture { fixture ->
        val result = executionRequest(fixture)

        assertEquals("HIM_EXTERNAL_TRAINER_EXECUTION_REQUEST_V1", result.contractId)
        assertEquals("1", result.version)
        assertEquals("EXTERNAL_TRAINER_EXECUTION_REQUEST_AUTHORIZED", result.state)
    }

    @Test
    fun protocolAdamWBindingIsMandatory() {
        assertEquals(
            HimTrainerProtocolAdamWRuntimeBindingV1::class.java,
            publicCreate().parameterTypes[0],
        )
    }

    @Test
    fun verifiedArtifactResolutionIsMandatory() {
        assertEquals(
            HimLocalModelArtifactResolverV1.Resolution::class.java,
            publicCreate().parameterTypes[1],
        )
    }

    @Test
    fun noMissionOnlyConstructionPathExists() {
        assertEquals(2, publicCreate().parameterTypes.size)
        assertFalse(publicCreate().parameterTypes.contains(HimTrainingMissionV1.Mission::class.java))
    }

    @Test
    fun noProtocolOnlyConstructionPathExists() {
        assertEquals(2, publicCreate().parameterTypes.size)
        assertFalse(publicCreate().parameterTypes.contains(HimTrainerProtocolV1.Request::class.java))
    }

    @Test
    fun noModelBindingOnlyConstructionPathExists() {
        assertFalse(publicCreate().parameterTypes.contains(HimModelBindingV1::class.java))
    }

    @Test
    fun noRawPathConstructionPathExists() {
        assertTrue(publicCreate().parameterTypes.none { it == Path::class.java })
    }

    @Test
    fun exactModelBindingMatchSucceeds() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertEquals(fixture.modelBinding.logicalDigest, result.modelBindingDigest)
        assertEquals(fixture.modelBinding.modelBindingReference, result.modelBindingReference)
    }

    @Test
    fun modelBindingMismatchFailsClosed(): Unit = withFixture { fixture ->
        val otherBinding = HimModelBindingV1.create(
            modelFamilyId = "fixture:other-family",
            baseModelId = fixture.modelBinding.baseModelId,
            baseModelArtifactDigest = fixture.modelBinding.baseModelArtifactDigest,
            tokenizerId = fixture.modelBinding.tokenizerId,
            tokenizerArtifactDigest = fixture.modelBinding.tokenizerArtifactDigest,
            modelConfigurationArtifactDigest = fixture.modelBinding.modelConfigurationArtifactDigest,
        )
        val otherResolution = resolvedArtifacts(fixture.root, otherBinding)

        assertFailsWith<IllegalArgumentException> {
            HimExternalTrainerExecutionRequestV1.create(fixture.protocolAdamWBinding, otherResolution)
        }
    }

    @Test
    fun missionIdentityIsPreserved() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertEquals(fixture.mission.logicalDigest, result.trainingMissionDigest)
        assertEquals(fixture.mission.missionReference, result.trainingMissionReference)
        assertEquals(fixture.protocolAdamWBinding.trainingMissionDigest, result.trainingMissionDigest)
    }

    @Test
    fun configurationIdentityIsPreserved() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertEquals(fixture.configuration.logicalDigest, result.trainingConfigurationDigest)
        assertEquals(fixture.configuration.configurationReference, result.trainingConfigurationReference)
    }

    @Test
    fun modelBindingIdentityIsPreserved() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertEquals(fixture.modelBinding.modelFamilyId, result.verifiedModelArtifacts.modelBinding.modelFamilyId)
        assertEquals(fixture.modelBinding.baseModelId, result.verifiedModelArtifacts.modelBinding.baseModelId)
        assertEquals(fixture.modelBinding.tokenizerId, result.verifiedModelArtifacts.modelBinding.tokenizerId)
        assertEquals(fixture.modelBinding.logicalDigest, result.verifiedModelArtifacts.modelBinding.logicalDigest)
    }

    @Test
    fun protocolRequestIdentityIsPreserved() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertSame(fixture.protocolRequest, result.protocolRequest)
        assertEquals(fixture.protocolRequest.logicalDigest, result.protocolRequest.logicalDigest)
        assertEquals(fixture.protocolRequest.requestReference, result.protocolRequest.requestReference)
    }

    @Test
    fun trainOnlyPartitionIsPreserved() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertEquals("TRAIN_ONLY", result.trainerProtocolPartition)
        assertTrue(result.protocolRequest.records.all { it.partition == "TRAIN_ONLY" })
    }

    @Test
    fun trainRecordsAreNotRebuilt() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertSame(fixture.protocolRequest.records, result.protocolRequest.records)
    }

    @Test
    fun trainRecordOrderIsUnchanged() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertEquals(
            fixture.protocolRequest.records.map { it.assignmentIndex },
            result.protocolRequest.records.map { it.assignmentIndex },
        )
    }

    @Test
    fun manyToOneCollapseRemainsZero() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertEquals(0, HimExternalTrainerExecutionRequestV1.MANY_TO_ONE_COLLAPSE)
        assertEquals(1, result.protocolRequest.records.map { it.groupReference }.distinct().size)
        assertEquals(3, result.protocolRequest.records.map { it.recordReference }.distinct().size)
    }

    @Test
    fun objectiveIsNotReimplemented() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertEquals(fixture.protocolRequest.objectiveDigest, result.protocolRequest.objectiveDigest)
        assertEquals(fixture.protocolRequest.objectiveReference, result.protocolRequest.objectiveReference)
    }

    @Test
    fun targetEncodingIsNotReimplemented() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertEquals(fixture.protocolRequest.targetEncodingDigest, result.protocolRequest.targetEncodingDigest)
        assertEquals(fixture.protocolRequest.targetEncodingReference, result.protocolRequest.targetEncodingReference)
    }

    @Test
    fun completeAdamWRuntimeSemanticsArePreserved() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertEquals(fixture.protocolAdamWBinding.optimizerId, result.optimizerId)
        assertEquals(fixture.protocolAdamWBinding.algorithm, result.algorithm)
        assertEquals(fixture.protocolAdamWBinding.learningRate, result.learningRate)
        assertEquals(fixture.protocolAdamWBinding.beta1, result.beta1)
        assertEquals(fixture.protocolAdamWBinding.beta2, result.beta2)
        assertEquals(fixture.protocolAdamWBinding.epsilon, result.epsilon)
        assertEquals(fixture.protocolAdamWBinding.weightDecay, result.weightDecay)
    }

    @Test
    fun optimizerIdIsPreserved() = withFixture { fixture ->
        assertEquals("optimizer:adamw:v1", executionRequest(fixture).optimizerId)
    }

    @Test
    fun learningRateIsPreserved() = withFixture { fixture ->
        assertEquals(BigDecimal("0.01"), executionRequest(fixture).learningRate)
    }

    @Test
    fun beta1IsPreserved() = withFixture { fixture ->
        assertEquals(fixture.protocolAdamWBinding.beta1, executionRequest(fixture).beta1)
    }

    @Test
    fun beta2IsPreserved() = withFixture { fixture ->
        assertEquals(fixture.protocolAdamWBinding.beta2, executionRequest(fixture).beta2)
    }

    @Test
    fun epsilonIsPreserved() = withFixture { fixture ->
        assertEquals(fixture.protocolAdamWBinding.epsilon, executionRequest(fixture).epsilon)
    }

    @Test
    fun weightDecayIsPreserved() = withFixture { fixture ->
        assertEquals(fixture.protocolAdamWBinding.weightDecay, executionRequest(fixture).weightDecay)
    }

    @Test
    fun decoupledWeightDecayRemainsTrue() = withFixture { fixture ->
        assertTrue(executionRequest(fixture).decoupledWeightDecay)
    }

    @Test
    fun biasCorrectionRemainsTrue() = withFixture { fixture ->
        assertTrue(executionRequest(fixture).biasCorrection)
    }

    @Test
    fun exactlyThreeVerifiedArtifactRolesArePreserved() = withFixture { fixture ->
        val artifacts = executionRequest(fixture).verifiedModelArtifacts
        assertEquals(3, HimLocalModelArtifactResolverV1.REQUIRED_ARTIFACT_ROLE_COUNT)
        assertEquals(
            setOf(
                HimLocalModelArtifactResolverV1.ArtifactRole.BASE_MODEL,
                HimLocalModelArtifactResolverV1.ArtifactRole.TOKENIZER,
                HimLocalModelArtifactResolverV1.ArtifactRole.MODEL_CONFIGURATION,
            ),
            setOf(artifacts.baseModel.role, artifacts.tokenizer.role, artifacts.modelConfiguration.role),
        )
    }

    @Test
    fun baseModelVerifiedPathIsPreserved() = withFixture { fixture ->
        assertEquals(
            fixture.resolution.baseModel.path,
            executionRequest(fixture).verifiedModelArtifacts.baseModel.path,
        )
    }

    @Test
    fun tokenizerVerifiedPathIsPreserved() = withFixture { fixture ->
        assertEquals(
            fixture.resolution.tokenizer.path,
            executionRequest(fixture).verifiedModelArtifacts.tokenizer.path,
        )
    }

    @Test
    fun configurationVerifiedPathIsPreserved() = withFixture { fixture ->
        assertEquals(
            fixture.resolution.modelConfiguration.path,
            executionRequest(fixture).verifiedModelArtifacts.modelConfiguration.path,
        )
    }

    @Test
    fun unverifiedRuntimePathApiDoesNotExist() {
        assertTrue(publicCreate().parameterTypes.none { it == Path::class.java })
        assertEquals(0, HimExternalTrainerExecutionRequestV1.UNVERIFIED_RUNTIME_ARTIFACT_PATHS_ACCEPTED)
    }

    @Test
    fun expectedAndActualArtifactDigestsArePreserved() = withFixture { fixture ->
        val artifacts = executionRequest(fixture).verifiedModelArtifacts
        listOf(artifacts.baseModel, artifacts.tokenizer, artifacts.modelConfiguration).forEach { artifact ->
            assertEquals(artifact.expectedDigest, artifact.actualDigest)
        }
    }

    @Test
    fun resolverIdentityIsPreserved() = withFixture { fixture ->
        val result = executionRequest(fixture)
        assertEquals(fixture.resolution.logicalDigest, result.verifiedModelArtifacts.logicalDigest)
        assertEquals(fixture.resolution.resolutionReference, result.verifiedModelArtifacts.resolutionReference)
    }

    @Test
    fun identicalInputsProduceIdenticalDigestAndReference() = withFixture { fixture ->
        val first = executionRequest(fixture)
        val second = executionRequest(fixture)
        assertEquals(first, second)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.requestReference, second.requestReference)
    }

    @Test
    fun differentProtocolAdamWBindingChangesRequestDigest() = withFixture { fixture ->
        val first = executionRequest(fixture)
        val changedFixture = Fixture.create(fixture.root, observedTerm = "Changed input")
        val second = executionRequest(changedFixture)
        assertNotEquals(first.protocolAdamWBinding.logicalDigest, second.protocolAdamWBinding.logicalDigest)
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun differentVerifiedArtifactResolutionChangesRequestDigest() = withFixture { fixture ->
        val first = executionRequest(fixture)
        val changedResolution = resolvedArtifacts(
            fixture.root,
            fixture.modelBinding,
            basePath = Path.of("other-base.artifact"),
        )
        val second = HimExternalTrainerExecutionRequestV1.create(fixture.protocolAdamWBinding, changedResolution)
        assertNotEquals(first.verifiedModelArtifacts.logicalDigest, second.verifiedModelArtifacts.logicalDigest)
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun absoluteMachinePathsAreExcludedFromRequestDigest() = withFixture { firstFixture ->
        withFixture { secondFixture ->
            val first = executionRequest(firstFixture)
            val second = executionRequest(secondFixture)
            assertEquals(first.logicalDigest, second.logicalDigest)
            assertNotEquals(first.verifiedModelArtifacts.baseModel.path, second.verifiedModelArtifacts.baseModel.path)
        }
    }

    @Test
    fun noFrameworkBindingExists() {
        assertEquals("NONE", HimExternalTrainerExecutionRequestV1.FRAMEWORK_BINDING)
        assertTrue(declaredNames().none { it.contains("torch", ignoreCase = true) })
        assertTrue(declaredNames().none { it.contains("python", ignoreCase = true) })
    }

    @Test
    fun noProcessExecutionExists() {
        assertEquals(0, HimExternalTrainerExecutionRequestV1.PROCESS_EXECUTION)
        val forbidden = setOf("process", "subprocess", "command", "stdout", "stderr", "exit")
        assertTrue(declaredMethodNames().none { name -> forbidden.any { token -> token in name } })
    }

    @Test
    fun noTokenizationExists() {
        assertEquals(0, HimExternalTrainerExecutionRequestV1.TOKENIZATION_EXECUTION)
        assertTrue(declaredMethodNames().none { it.contains("token", ignoreCase = true) })
    }

    @Test
    fun noTensorizationExists() {
        assertEquals(0, HimExternalTrainerExecutionRequestV1.TENSORIZATION)
        assertTrue(declaredMethodNames().none { it.contains("tensor", ignoreCase = true) })
    }

    @Test
    fun noModelLoadExists() {
        assertEquals(0, HimExternalTrainerExecutionRequestV1.MODEL_LOAD_EXECUTION)
        val forbidden = setOf("load", "parse", "deserialize", "safetensor")
        assertTrue(declaredMethodNames().none { name -> forbidden.any { token -> token in name } })
    }

    @Test
    fun noNumericalTrainingExists() {
        assertEquals(0, HimExternalTrainerExecutionRequestV1.NUMERICAL_TRAINING_EXECUTION)
        val forbidden = setOf("train", "forward", "backward", "checkpoint", "evaluate")
        assertTrue(declaredMethodNames().none { name -> forbidden.any { token -> name == token } })
    }

    @Test
    fun noPersistenceExists() {
        assertEquals(0, HimExternalTrainerExecutionRequestV1.EXECUTION_REQUEST_PERSISTENCE)
        val forbidden = setOf("persist", "json", "database", "sqlite", "report", "save", "store")
        assertTrue(declaredMethodNames().none { name -> forbidden.any { token -> token in name } })
    }

    @Test
    fun noFrozenUpstreamModificationIsRequired() {
        assertEquals(0, HimExternalTrainerExecutionRequestV1.FROZEN_UPSTREAM_CONTRACT_MODIFICATIONS)
        assertEquals(2, publicCreate().parameterTypes.size)
    }

    @Test
    fun executionRequestArtifactRehashIsZero() {
        assertEquals(0, HimExternalTrainerExecutionRequestV1.EXECUTION_REQUEST_ARTIFACT_REHASH)
        assertEquals("YES", HimLocalModelArtifactResolverV1.VERIFIED_AT_RESOLUTION_TIME)
    }

    @Test
    fun frameworkAndRuntimeBoundariesAreExplicit() {
        assertEquals("YES", HimExternalTrainerExecutionRequestV1.COMPLETE_OPTIMIZER_RUNTIME_SPEC_PRESERVED)
        assertEquals(0, HimExternalTrainerExecutionRequestV1.HIDDEN_FRAMEWORK_DEFAULTS)
        assertEquals("NO", HimExternalTrainerExecutionRequestV1.ABSOLUTE_PATH_IN_EXECUTION_REQUEST_LOGICAL_DIGEST)
    }

    private fun executionRequest(fixture: Fixture): HimExternalTrainerExecutionRequestV1 =
        HimExternalTrainerExecutionRequestV1.create(fixture.protocolAdamWBinding, fixture.resolution)

    private fun publicCreate() = HimExternalTrainerExecutionRequestV1::class.java.declaredMethods.single {
        it.name == "create" && Modifier.isPublic(it.modifiers)
    }

    private fun declaredMethodNames(): Set<String> =
        HimExternalTrainerExecutionRequestV1::class.java.declaredMethods.map { it.name.lowercase() }.toSet()

    private fun declaredNames(): Set<String> = buildSet {
        addAll(declaredMethodNames())
        addAll(HimExternalTrainerExecutionRequestV1::class.java.declaredFields.map { it.name.lowercase() })
    }

    private data class Fixture(
        val root: Path,
        val configuration: HimTrainingConfigurationV1,
        val mission: HimTrainingMissionV1.Mission,
        val modelBinding: HimModelBindingV1,
        val protocolRequest: HimTrainerProtocolV1.Request,
        val protocolAdamWBinding: HimTrainerProtocolAdamWRuntimeBindingV1,
        val resolution: HimLocalModelArtifactResolverV1.Resolution,
    ) {
        companion object {
            fun create(root: Path, observedTerm: String = "Fixture variant"): Fixture {
                val positive = positiveExample(observedTerm)
                val negatives = HimNegativeTrainingExamplePolicyV1.derive(positive).take(2)
                val records = listOf(HimTrainingPartitionRecordV1.Positive(positive)) +
                    negatives.map { HimTrainingPartitionRecordV1.Negative(it) }
                val group = HimTrainingFamilyGroupReferenceV1.canonical(CANONICAL_ID)
                val manifest = HimTrainingPartitionManifestV1.create(
                    records.map { HimTrainingPartitionAssignmentV1(it, group, HimTrainingPartitionV1.TRAIN) },
                )
                val configuration = HimTrainingConfigurationV1.create(
                    seed = 7L,
                    epochs = 3,
                    microBatchSize = 2,
                    gradientAccumulationSteps = 1,
                    learningRate = BigDecimal("0.01"),
                    optimizerId = "optimizer:adamw:v1",
                )
                val modelBinding = HimModelBindingV1.create(
                    modelFamilyId = "fixture:model-family",
                    baseModelId = "fixture:base-model",
                    baseModelArtifactDigest = executionDigest("base-model-fixture"),
                    tokenizerId = "fixture:tokenizer",
                    tokenizerArtifactDigest = executionDigest("tokenizer-fixture"),
                    modelConfigurationArtifactDigest = executionDigest("configuration-fixture"),
                )
                val mission = HimTrainingMissionV1.create(
                    HimTrainingMissionV1.Request(
                        readiness = readiness(manifest),
                        trainingConfiguration = HimTrainingMissionV1.TrainingConfigurationReference(
                            configuration.logicalDigest,
                        ),
                        modelBinding = HimTrainingMissionV1.ModelBindingReference(modelBinding.logicalDigest),
                        implementationBinding = HimTrainingMissionV1.ImplementationBindingReference(
                            executionDigest("implementation"),
                        ),
                    ),
                )
                val portRequest = HimTrainerPortV1.Request.create(
                    mission,
                    configuration,
                    modelBinding,
                    manifest,
                )
                val protocolRequest = HimTrainerProtocolV1.create().createRequest(portRequest)
                val adamwBinding = HimAdamWRuntimeBindingV1.create(
                    mission,
                    configuration,
                    HimOptimizerMappingV1.create(),
                    HimAdamWParametersV1.create(
                        beta1 = BigDecimal("0.9"),
                        beta2 = BigDecimal("0.999"),
                        epsilon = BigDecimal("0.00000001"),
                        weightDecay = BigDecimal("0.01"),
                    ),
                )
                return Fixture(
                    root = root,
                    configuration = configuration,
                    mission = mission,
                    modelBinding = modelBinding,
                    protocolRequest = protocolRequest,
                    protocolAdamWBinding = HimTrainerProtocolAdamWRuntimeBindingV1.create(
                        protocolRequest,
                        adamwBinding,
                    ),
                    resolution = resolvedArtifacts(root, modelBinding),
                )
            }

            private fun positiveExample(observedTerm: String): HimTrainingExampleV1 {
                val input = HimTrainingInputV1(
                    observedTerm = observedTerm,
                    normalizedObservedTerm = observedTerm.lowercase(),
                    canonicalContext = listOf(
                        HimCandidateCanonicalContext(1, CANONICAL_ID, "Fixture variant", null),
                        HimCandidateCanonicalContext(2, HimEntityId("Def456"), "Other canonical", null),
                    ),
                    evidence = listOf(
                        de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1(
                            HimEvidenceReference("OPEN_FOOD_FACTS", FIXTURE_ARTIFACT_DIGEST, "off:product:fixture"),
                            "fixture-evidence",
                            1,
                        ),
                        de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1(
                            HimEvidenceReference("CIQUAL", FIXTURE_ARTIFACT_DIGEST, "ciqual:food:fixture"),
                            "fixture-evidence",
                            1,
                        ),
                    ),
                )
                return HimTrainingExampleV1.create(
                    taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
                    input = input,
                    target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID)),
                    provenance = HimTrainingProvenanceV1(
                        sourceEvidenceReferences = input.evidence.map { it.reference },
                        sourceArtifactDigests = listOf(FIXTURE_ARTIFACT_DIGEST),
                    ),
                )
            }

            private fun readiness(manifest: HimTrainingPartitionManifestV1) =
                HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready(
                    snapshotBinding = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1(
                        snapshotId = "training-corpus:v1:${executionDigest("snapshot").value}",
                        corpusLogicalDigest = executionDigest("snapshot"),
                    ),
                    partitionManifestLogicalDigest = manifest.logicalDigest,
                    partitionCounters = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters.from(
                        manifest.assignments,
                    ),
                    partitionPolicyVersion = HimTrainingPartitionContractV1.POLICY_VERSION,
                    leakageValidationResult = de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionValidationResultV1(
                        valid = true,
                        diagnostics = emptyList(),
                    ),
                )

            private val CANONICAL_ID = HimEntityId("Abc123")
            private val FIXTURE_ARTIFACT_DIGEST = executionDigest("fixture-artifact")
        }
    }

    private fun <T> withFixture(block: (Fixture) -> T): T {
        val root = Files.createTempDirectory("him-external-execution-")
        return try {
            block(Fixture.create(root))
        } finally {
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }

}
