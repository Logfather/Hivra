package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
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
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWParametersV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerExecutionRequestV1
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimLocalModelArtifactResolverV1
import de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimOptimizerMappingV1
import de.shopme.tools.knowledge.him.training.runtime.HimPythonPyTorchRuntimeEnvironmentV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerPortV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolAdamWRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainerProtocolV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingMissionV1
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private fun digest(seed: String): HimSha256 = HimSha256(
    MessageDigest.getInstance("SHA-256")
        .digest(seed.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) },
)

private val BASE_BYTES = "base-model-fixture".toByteArray()
private val TOKENIZER_BYTES = "tokenizer-fixture".toByteArray()
private val CONFIGURATION_BYTES = "configuration-fixture".toByteArray()

class RunHimExternalTrainerRuntimeBindingV1Test {
    @Test
    fun validRequestAndStableEnvironmentCreateBinding() = withFixture { fixture ->
        val binding = binding(fixture)
        assertEquals("HIM_EXTERNAL_TRAINER_RUNTIME_BINDING_V1", binding.contractId)
        assertEquals("1", binding.version)
        assertEquals("EXTERNAL_TRAINER_RUNTIME_BOUND", binding.state)
    }

    @Test
    fun publicFactoryRequiresExactlyRequestAndEnvironment() {
        val method = publicCreate()
        assertEquals(2, method.parameterTypes.size)
        assertEquals(HimExternalTrainerExecutionRequestV1::class.java, method.parameterTypes[0])
        assertEquals(HimPythonPyTorchRuntimeEnvironmentV1::class.java, method.parameterTypes[1])
    }

    @Test
    fun executionRequestIsMandatory() {
        withFixture { fixture ->
        assertFailsWith<NullPointerException> {
            HimExternalTrainerRuntimeBindingV1.create(
                null as HimExternalTrainerExecutionRequestV1,
                environment(),
            )
        }
        }
    }

    @Test
    fun runtimeEnvironmentIsMandatory() {
        withFixture { fixture ->
        assertFailsWith<NullPointerException> {
            HimExternalTrainerRuntimeBindingV1.create(
                request(fixture),
                null as HimPythonPyTorchRuntimeEnvironmentV1,
            )
        }
        }
    }

    @Test
    fun noMissionOnlyConstructionPathExists() {
        assertTrue(publicCreate().parameterTypes.none { it == HimTrainingMissionV1.Mission::class.java })
    }

    @Test
    fun noEnvironmentOnlyConstructionPathExists() {
        assertEquals(2, publicCreate().parameterTypes.size)
        assertTrue(publicCreate().parameterTypes.contains(HimExternalTrainerExecutionRequestV1::class.java))
    }

    @Test
    fun noRawPythonVersionConstructionPathExists() {
        assertTrue(publicCreate().parameterTypes.none { it == String::class.java })
    }

    @Test
    fun noRawPyTorchVersionConstructionPathExists() {
        assertTrue(publicCreate().parameterTypes.none { it == String::class.java })
    }

    @Test
    fun noRawPathConstructionPathExists() {
        assertTrue(publicCreate().parameterTypes.none { it == Path::class.java })
    }

    @Test
    fun exactExecutionRequestIdentityIsPreserved() = withFixture { fixture ->
        val request = request(fixture)
        val binding = HimExternalTrainerRuntimeBindingV1.create(request, environment())
        assertSame(request, binding.executionRequest)
        assertEquals(request.logicalDigest, binding.executionRequestDigest)
        assertEquals(request.requestReference, binding.executionRequestReference)
    }

    @Test
    fun exactRuntimeEnvironmentIdentityIsPreserved() = withFixture { fixture ->
        val environment = environment()
        val binding = HimExternalTrainerRuntimeBindingV1.create(request(fixture), environment)
        assertSame(environment, binding.runtimeEnvironment)
        assertEquals(environment.logicalDigest, binding.runtimeEnvironmentDigest)
        assertEquals(environment.environmentReference, binding.runtimeEnvironmentReference)
    }

    @Test
    fun identicalInputsProduceIdenticalDigestAndReference() = withFixture { fixture ->
        val first = binding(fixture)
        val second = binding(fixture)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.bindingReference, second.bindingReference)
        assertEquals(first, second)
    }

    @Test
    fun changingExecutionRequestIdentityChangesBindingDigest() = withTwoFixtures { first, second ->
        assertNotEquals(binding(first).logicalDigest, binding(second).logicalDigest)
    }

    @Test
    fun changingEnvironmentIdentityChangesBindingDigest() = withFixture { fixture ->
        assertNotEquals(
            binding(fixture).logicalDigest,
            HimExternalTrainerRuntimeBindingV1.create(request(fixture), environment(pytorchVersion = "2.13.1")).logicalDigest,
        )
    }

    @Test
    fun stableRuntimeChannelIsAccepted() = withFixture { fixture ->
        assertEquals(
            HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.STABLE,
            binding(fixture).runtimeEnvironment.runtimeChannel,
        )
    }

    @Test
    fun nonStableRuntimeChannelIsRejected() {
        withFixture { fixture ->
        assertFailsWith<IllegalArgumentException> {
            HimExternalTrainerRuntimeBindingV1.create(
                request(fixture),
                environment(HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.RC_PILOT),
            )
        }
        }
    }

    @Test
    fun pythonRuntimeIsBoundTransitively() = withFixture { fixture ->
        assertEquals("YES", HimExternalTrainerRuntimeBindingV1.PYTHON_RUNTIME_BOUND)
        assertEquals("3.13.14", binding(fixture).runtimeEnvironment.pythonVersion)
        assertEquals("CPython", binding(fixture).runtimeEnvironment.pythonImplementation)
    }

    @Test
    fun pytorchRuntimeIsBoundTransitively() = withFixture { fixture ->
        assertEquals("YES", HimExternalTrainerRuntimeBindingV1.PYTORCH_RUNTIME_BOUND)
        assertEquals("2.13.0", binding(fixture).runtimeEnvironment.pytorchVersion)
        assertEquals("torch-2.13.0-cp313-cp313-macosx_14_0_arm64.whl", binding(fixture).runtimeEnvironment.pytorchWheelIdentity)
    }

    @Test
    fun environmentImplementationFingerprintContinuityIsTransitive() = withFixture { fixture ->
        assertEquals("TRANSITIVE", HimExternalTrainerRuntimeBindingV1.ENVIRONMENT_IMPLEMENTATION_FINGERPRINT_CONTINUITY)
        assertEquals(
            environment().environmentImplementationFingerprint,
            binding(fixture).runtimeEnvironment.environmentImplementationFingerprint,
        )
    }

    @Test
    fun trainerImplementationRemainsUnbound() {
        assertEquals("NO", HimExternalTrainerRuntimeBindingV1.TRAINER_IMPLEMENTATION_BOUND)
        assertFalse(declaredNames().any { it.contains("trainerimplementation") })
    }

    @Test
    fun deviceSelectionRemainsUnbound() {
        assertEquals("NO", HimExternalTrainerRuntimeBindingV1.DEVICE_SELECTION_BOUND)
        assertFalse(instanceNames().any { it.contains("device") || it.contains("cuda") })
    }

    @Test
    fun mpsIsNotSelected() {
        assertEquals("NO", HimExternalTrainerRuntimeBindingV1.MPS_DEVICE_SELECTED)
        assertFalse(instanceNames().any { it.contains("mps") })
    }

    @Test
    fun verifiedModelArtifactsArePreservedTransitively() = withFixture { fixture ->
        val request = request(fixture)
        assertSame(fixture.resolution, binding(fixture).executionRequest.verifiedModelArtifacts)
        assertEquals(request.verifiedModelArtifacts.baseModel.path, binding(fixture).executionRequest.verifiedModelArtifacts.baseModel.path)
        assertEquals(request.verifiedModelArtifacts.tokenizer.path, binding(fixture).executionRequest.verifiedModelArtifacts.tokenizer.path)
        assertEquals(request.verifiedModelArtifacts.modelConfiguration.path, binding(fixture).executionRequest.verifiedModelArtifacts.modelConfiguration.path)
    }

    @Test
    fun trainOnlyPartitionIsPreservedTransitively() = withFixture { fixture ->
        assertTrue(binding(fixture).executionRequest.protocolRequest.records.all { it.partition == "TRAIN_ONLY" })
    }

    @Test
    fun trainRecordOrderingIsPreservedTransitively() = withFixture { fixture ->
        val records = binding(fixture).executionRequest.protocolRequest.records
        assertEquals(records.indices.toList(), records.map { it.assignmentIndex })
        assertEquals(fixture.protocolRequest.records.map { it.recordReference }, records.map { it.recordReference })
    }

    @Test
    fun manyToOneSemanticsArePreservedTransitively() = withFixture { fixture ->
        val records = binding(fixture).executionRequest.protocolRequest.records
        assertEquals(3, records.size)
        assertEquals(1, records.map { it.positiveExampleReference }.distinct().size)
    }

    @Test
    fun objectiveIdentityIsPreservedTransitively() = withFixture { fixture ->
        val request = binding(fixture).executionRequest.protocolRequest
        assertEquals(fixture.protocolRequest.objectiveDigest, request.objectiveDigest)
        assertEquals(fixture.protocolRequest.objectiveReference, request.objectiveReference)
    }

    @Test
    fun targetEncodingIdentityIsPreservedTransitively() = withFixture { fixture ->
        val request = binding(fixture).executionRequest.protocolRequest
        assertEquals(fixture.protocolRequest.targetEncodingDigest, request.targetEncodingDigest)
        assertEquals(fixture.protocolRequest.targetEncodingReference, request.targetEncodingReference)
    }

    @Test
    fun completeAdamWSemanticsArePreservedTransitively() = withFixture { fixture ->
        val result = binding(fixture).executionRequest
        assertEquals(fixture.protocolAdamWBinding.optimizerId, result.optimizerId)
        assertEquals(fixture.protocolAdamWBinding.algorithm, result.algorithm)
        assertEquals(fixture.protocolAdamWBinding.learningRate, result.learningRate)
        assertEquals(fixture.protocolAdamWBinding.beta1, result.beta1)
        assertEquals(fixture.protocolAdamWBinding.beta2, result.beta2)
        assertEquals(fixture.protocolAdamWBinding.epsilon, result.epsilon)
        assertEquals(fixture.protocolAdamWBinding.weightDecay, result.weightDecay)
        assertEquals(fixture.protocolAdamWBinding.decoupledWeightDecay, result.decoupledWeightDecay)
        assertEquals(fixture.protocolAdamWBinding.biasCorrection, result.biasCorrection)
    }

    @Test
    fun nestedTrainingSemanticsAreNotReimplemented() {
        val names = instanceNames()
        assertFalse(names.any { it.contains("optimizer") || it.contains("learningrate") || it.contains("weightdecay") })
        assertFalse(names.any { it.contains("objective") || it.contains("targetencoding") || it.contains("artifact") })
    }

    @Test
    fun bindingHasNoAbsoluteRuntimePathField() {
        assertEquals("NO", HimExternalTrainerRuntimeBindingV1.ABSOLUTE_RUNTIME_PATH_IN_BINDING_DIGEST)
        assertFalse(instanceNames().any { it.contains("path") || it.contains("directory") })
    }

    @Test
    fun bindingHasNoVenvPath() {
        assertFalse(declaredNames().any { it.contains("venv") })
    }

    @Test
    fun bindingHasNoPythonExecutablePath() {
        assertFalse(declaredNames().any { it.contains("executable") })
    }

    @Test
    fun processCommandIsNotBound() {
        assertEquals("NO", HimExternalTrainerRuntimeBindingV1.PROCESS_COMMAND_BOUND)
        assertFalse(instanceNames().any { it.contains("command") || it.contains("argument") })
    }

    @Test
    fun processBuilderIsNotUsed() {
        assertFalse(declaredNames().any { it.contains("processbuilder") })
    }

    @Test
    fun pythonInvocationIsNotUsed() {
        assertEquals("NO", HimExternalTrainerRuntimeBindingV1.PYTHON_PROCESS_STARTED)
        assertFalse(instanceNames().any { it.contains("python") && !it.contains("runtime") })
    }

    @Test
    fun uvInvocationIsNotUsed() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.PACKAGE_INSTALLATION)
        assertFalse(declaredNames().any { it == "uv" || it.contains("uvinvoke") })
    }

    @Test
    fun torchInvocationIsNotUsed() {
        assertFalse(declaredNames().any { it == "torch" || it.contains("torchinvoke") })
    }

    @Test
    fun environmentProbingIsDisabled() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.RUNTIME_PROBING)
    }

    @Test
    fun networkAccessIsDisabled() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.NETWORK_ACCESS)
    }

    @Test
    fun packageInstallationIsDisabled() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.PACKAGE_INSTALLATION)
    }

    @Test
    fun modelLoadingIsDisabled() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.MODEL_LOAD_EXECUTION)
        assertFalse(instanceNames().any { it.contains("load") })
    }

    @Test
    fun tokenizationIsDisabled() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.TOKENIZATION_EXECUTION)
        assertFalse(instanceNames().any { it.contains("token") })
    }

    @Test
    fun tensorizationIsDisabled() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.TENSORIZATION)
        assertFalse(instanceNames().any { it.contains("tensor") })
    }

    @Test
    fun numericalTrainingIsDisabled() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.NUMERICAL_TRAINING_EXECUTION)
        assertEquals("NO", HimExternalTrainerRuntimeBindingV1.PYTORCH_TRAINING_EXECUTED)
    }

    @Test
    fun runtimeBindingPersistenceIsDisabled() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.RUNTIME_BINDING_PERSISTENCE)
        assertFalse(instanceNames().any { it.contains("persist") || it.contains("database") || it.contains("sqlite") })
    }

    @Test
    fun upstreamModificationIsNotRequired() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.FROZEN_UPSTREAM_CONTRACT_MODIFICATIONS)
        assertEquals("YES", HimExternalTrainerRuntimeBindingV1.RUNTIME_BINDING_REQUIRES_EXECUTION_REQUEST)
        assertEquals("YES", HimExternalTrainerRuntimeBindingV1.RUNTIME_BINDING_REQUIRES_RUNTIME_ENVIRONMENT)
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.ALTERNATE_RUNTIME_BINDING_AUTHORIZATION_PATHS)
    }

    @Test
    fun bindingDigestUsesAuthoritativeNestedIdentities() = withFixture { fixture ->
        val request = request(fixture)
        val environment = environment()
        val binding = HimExternalTrainerRuntimeBindingV1.create(request, environment)
        assertEquals(request.logicalDigest, binding.executionRequestDigest)
        assertEquals(request.requestReference, binding.executionRequestReference)
        assertEquals(environment.logicalDigest, binding.runtimeEnvironmentDigest)
        assertEquals(environment.environmentReference, binding.runtimeEnvironmentReference)
    }

    @Test
    fun bindingReferenceIsDeterministicallyDerivedFromLogicalDigest() = withFixture { fixture ->
        val result = binding(fixture)
        assertEquals("external-trainer-runtime-binding:v1:${result.logicalDigest.value}", result.bindingReference)
        assertTrue(result.bindingReference.matches(Regex("external-trainer-runtime-binding:v1:[0-9a-f]{64}")))
    }

    @Test
    fun differentArtifactRootsDoNotEnterBindingIdentity() = withTwoFixtures(sameObservedTerm = true) { first, second ->
        assertEquals(binding(first).logicalDigest, binding(second).logicalDigest)
        assertNotEquals(
            first.resolution.baseModel.path,
            second.resolution.baseModel.path,
        )
    }

    @Test
    fun futureProcessAdapterCanRequireRuntimeBinding() {
        assertEquals("YES", HimExternalTrainerRuntimeBindingV1.PROCESS_ADAPTER_CAN_REQUIRE_RUNTIME_BINDING)
    }

    @Test
    fun futureProcessAdapterInputsRemainOutsideBinding() {
        val names = declaredNames()
        assertFalse(names.any { it.contains("implementationidentity") || it.contains("runtimepath") })
        assertFalse(names.any { it.contains("devicebinding") || it.contains("operationalconfiguration") })
    }

    @Test
    fun pytorchProcessIsNotStarted() {
        assertEquals("NO", HimExternalTrainerRuntimeBindingV1.PYTORCH_PROCESS_STARTED)
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.PROCESS_EXECUTION)
    }

    @Test
    fun pythonProcessIsNotStarted() {
        assertEquals("NO", HimExternalTrainerRuntimeBindingV1.PYTHON_PROCESS_STARTED)
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.PROCESS_EXECUTION)
    }

    @Test
    fun executionRequestReimplementationIsZero() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.EXECUTION_REQUEST_REIMPLEMENTATION)
        assertEquals("YES", HimExternalTrainerRuntimeBindingV1.EXECUTION_REQUEST_IDENTITY_PRESERVED)
    }

    @Test
    fun runtimeEnvironmentReimplementationIsZero() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.RUNTIME_ENVIRONMENT_REIMPLEMENTATION)
        assertEquals("YES", HimExternalTrainerRuntimeBindingV1.RUNTIME_ENVIRONMENT_IDENTITY_PRESERVED)
    }

    @Test
    fun modelArtifactReverificationIsZero() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.MODEL_ARTIFACT_REVERIFICATION)
    }

    @Test
    fun trainingSemanticsReimplementationIsZero() {
        assertEquals(0, HimExternalTrainerRuntimeBindingV1.TRAINING_SEMANTICS_REIMPLEMENTATION)
    }

    @Test
    fun stableEnvironmentIdentityIncludesFrameworkFactsTransitively() = withFixture { fixture ->
        val environment = binding(fixture).runtimeEnvironment
        assertEquals("https://download.pytorch.org/whl/cpu", environment.pytorchPackageSource)
        assertEquals("cf30153c4c131c8164ee7798e5022d810682e2cb", environment.pytorchGitVersion)
        assertEquals("cpython-313-darwin", environment.pythonAbi)
    }

    @Test
    fun publicFactoryHasNoAlternateRawEnvironmentPath() {
        val parameterTypes = publicCreate().parameterTypes.toSet()
        assertFalse(parameterTypes.contains(Path::class.java))
        assertFalse(parameterTypes.contains(String::class.java))
        assertFalse(parameterTypes.contains(HimTrainingMissionV1.Mission::class.java))
    }

    @Test
    fun failureForRcIsFailClosed() = withFixture { fixture ->
        val failure = assertFailsWith<IllegalArgumentException> {
            HimExternalTrainerRuntimeBindingV1.create(
                request(fixture),
                environment(HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.RC_PILOT),
            )
        }
        assertTrue(failure.message!!.contains("RUNTIME_CHANNEL_REQUIRED_STABLE"))
    }

    private fun binding(fixture: Fixture): HimExternalTrainerRuntimeBindingV1 =
        HimExternalTrainerRuntimeBindingV1.create(request(fixture), environment())

    private fun request(fixture: Fixture): HimExternalTrainerExecutionRequestV1 =
        HimExternalTrainerExecutionRequestV1.create(fixture.protocolAdamWBinding, fixture.resolution)

    private fun publicCreate() = HimExternalTrainerRuntimeBindingV1::class.java.declaredMethods.single {
        it.name == "create" && Modifier.isPublic(it.modifiers)
    }

    private fun declaredNames(): Set<String> = buildSet {
        addAll(HimExternalTrainerRuntimeBindingV1::class.java.declaredMethods.map { it.name.lowercase() })
        addAll(HimExternalTrainerRuntimeBindingV1::class.java.declaredFields.map { it.name.lowercase() })
    }

    private fun instanceNames(): Set<String> = buildSet {
        addAll(
            HimExternalTrainerRuntimeBindingV1::class.java.declaredFields
                .filterNot { Modifier.isStatic(it.modifiers) }
                .map { it.name.lowercase() },
        )
        addAll(HimExternalTrainerRuntimeBindingV1::class.java.declaredMethods.map { it.name.lowercase() })
    }

    private fun <T> withFixture(block: (Fixture) -> T): T {
        val root = Files.createTempDirectory("him-external-runtime-binding-")
        return try {
            block(Fixture.create(root))
        } finally {
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }

    private fun <T> withTwoFixtures(
        sameObservedTerm: Boolean = false,
        block: (Fixture, Fixture) -> T,
    ): T = withFixture { first ->
        val secondRoot = Files.createTempDirectory("him-external-runtime-binding-second-")
        try {
            block(
                first,
                Fixture.create(
                    secondRoot,
                    if (sameObservedTerm) "Fixture variant" else "Other fixture variant",
                ),
            )
        } finally {
            Files.walk(secondRoot).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }

    private data class Fixture(
        val root: Path,
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
                    baseModelArtifactDigest = digest("base-model-fixture"),
                    tokenizerId = "fixture:tokenizer",
                    tokenizerArtifactDigest = digest("tokenizer-fixture"),
                    modelConfigurationArtifactDigest = digest("configuration-fixture"),
                )
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
                        snapshotId = "training-corpus:v1:${digest("snapshot").value}",
                        corpusLogicalDigest = digest("snapshot"),
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
            private val FIXTURE_ARTIFACT_DIGEST = digest("fixture-artifact")
        }
    }
}

private fun environment(
    runtimeChannel: HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel =
        HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.STABLE,
    pytorchVersion: String = "2.13.0",
): HimPythonPyTorchRuntimeEnvironmentV1 = HimPythonPyTorchRuntimeEnvironmentV1.create(
    runtimeChannel = runtimeChannel,
    pythonVersion = "3.13.14",
    pythonImplementation = "CPython",
    uvVersion = "0.12.2",
    targetOsFamily = "macOS",
    targetArchitecture = "arm64",
    pythonAbi = "cpython-313-darwin",
    pytorchVersion = pytorchVersion,
    pytorchPackageSource = "https://download.pytorch.org/whl/cpu",
    pytorchWheelIdentity = "torch-2.13.0-cp313-cp313-macosx_14_0_arm64.whl",
    pytorchPackageSha256 = digest("package"),
    pytorchGitVersion = "cf30153c4c131c8164ee7798e5022d810682e2cb",
    pyprojectSha256 = digest("pyproject"),
    uvLockSha256 = digest("uv-lock"),
    pythonVersionFileSha256 = digest("python-version-file"),
    environmentImplementationFingerprint = digest("environment-implementation"),
)

private fun resolvedArtifacts(
    root: Path,
    modelBinding: HimModelBindingV1,
): HimLocalModelArtifactResolverV1.Resolution {
    write(root, Path.of("base-model.artifact"), BASE_BYTES)
    write(root, Path.of("tokenizer.artifact"), TOKENIZER_BYTES)
    write(root, Path.of("configuration.artifact"), CONFIGURATION_BYTES)
    return (HimLocalModelArtifactResolverV1.resolve(
        HimLocalModelArtifactResolverV1.Request(
            modelBinding = modelBinding,
            artifactRoot = root,
            baseModelPath = Path.of("base-model.artifact"),
            tokenizerPath = Path.of("tokenizer.artifact"),
            modelConfigurationPath = Path.of("configuration.artifact"),
        ),
    ) as HimLocalModelArtifactResolverV1.Result.Completed).value
}

private fun write(root: Path, relativePath: Path, bytes: ByteArray) {
    relativePath.parent?.let { Files.createDirectories(root.resolve(it)) }
    Files.write(root.resolve(relativePath), bytes)
}
