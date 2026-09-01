package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionLeakageDiagnosticV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionLeakageLevelV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionValidationResultV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWParametersV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWRuntimeBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimOptimizerMappingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingMissionV1
import java.lang.reflect.Modifier
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private fun digest(seed: String): String =
    java.security.MessageDigest.getInstance("SHA-256")
        .digest(seed.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

class RunHimAdamWRuntimeBindingV1Test {
    @Test
    fun validMissionConfigurationMappingAndParametersCreateBinding() {
        val binding = binding()

        assertEquals("HIM_ADAMW_RUNTIME_BINDING_V1", binding.contractId)
        assertEquals("1", binding.version)
        assertEquals("ADAMW_RUNTIME_BINDING_VALIDATED", binding.state)
    }

    @Test
    fun bindingRequiresTrainingMission() {
        val create = HimAdamWRuntimeBindingV1::class.java.declaredMethods.single {
            it.name == "create" && Modifier.isPublic(it.modifiers)
        }

        assertEquals(HimTrainingMissionV1.Mission::class.java, create.parameterTypes.first())
    }

    @Test
    fun missionConfigurationMismatchFailsClosed() {
        val configuration = configuration(seed = 8L)

        assertFailsWith<IllegalArgumentException> {
            HimAdamWRuntimeBindingV1.create(Fixture.mission, configuration, mapping(), parameters())
        }
    }

    @Test
    fun nonAdamWOptimizerFailsClosed() {
        val configuration = configuration(optimizerId = "optimizer:sgd:v1")
        val mission = Fixture.mission(configuration)

        assertFailsWith<IllegalStateException> {
            HimAdamWRuntimeBindingV1.create(mission, configuration, mapping(), parameters())
        }
    }

    @Test
    fun algorithmIsExactlyAdamW() {
        assertEquals(HimOptimizerMappingV1.AlgorithmKind.ADAMW, binding().algorithmKind)
    }

    @Test
    fun mappingIdentityIsReused() {
        val mapping = mapping()
        val binding = binding(mapping = mapping)

        assertEquals(mapping.logicalDigest, binding.optimizerMappingDigest)
        assertEquals(mapping.mappingReference, binding.optimizerMappingReference)
    }

    @Test
    fun parametersDigestIsBound() {
        val parameters = parameters()

        assertEquals(parameters.logicalDigest, binding(parameters = parameters).adamwParametersDigest)
        assertEquals(parameters.parametersReference, binding(parameters = parameters).adamwParametersReference)
    }

    @Test
    fun configurationDigestIsBound() {
        val configuration = configuration()

        assertEquals(configuration.logicalDigest, binding(configuration = configuration).trainingConfigurationDigest)
    }

    @Test
    fun missionDigestIsBound() {
        assertEquals(Fixture.mission.logicalDigest, binding().trainingMissionDigest)
        assertEquals(Fixture.mission.missionReference, binding().trainingMissionReference)
    }

    @Test
    fun learningRateIsProjectedFromConfiguration() {
        val configuration = configuration(learningRate = "0.0002")

        assertEquals(BigDecimal("0.0002"), binding(configuration = configuration).learningRate)
    }

    @Test
    fun beta1IsProjectedExactly() {
        assertEquals(BigDecimal("0.8"), binding(parameters = parameters(beta1 = "0.8")).beta1)
    }

    @Test
    fun beta2IsProjectedExactly() {
        assertEquals(BigDecimal("0.998"), binding(parameters = parameters(beta2 = "0.998")).beta2)
    }

    @Test
    fun epsilonIsProjectedExactly() {
        assertEquals(BigDecimal("0.000000001"), binding(parameters = parameters(epsilon = "0.000000001")).epsilon)
    }

    @Test
    fun weightDecayIsProjectedExactly() {
        assertEquals(BigDecimal("0.02"), binding(parameters = parameters(weightDecay = "0.02")).weightDecay)
    }

    @Test
    fun decoupledWeightDecayIsExactlyTrue() {
        assertTrue(binding().decoupledWeightDecay)
    }

    @Test
    fun biasCorrectionIsExactlyTrue() {
        assertTrue(binding().biasCorrection)
    }

    @Test
    fun invariantsAreNotCallerConfigurable() {
        val constructorTypes = HimAdamWRuntimeBindingV1::class.java.declaredConstructors
            .flatMap { it.parameterTypes.toList() }

        assertFalse(constructorTypes.contains(Boolean::class.javaPrimitiveType))
    }

    @Test
    fun identicalInputsCreateIdenticalIdentity() {
        assertEquals(binding(), binding())
        assertEquals(binding().runtimeBindingReference, binding().runtimeBindingReference)
    }

    @Test
    fun changingLearningRateChangesIdentity() {
        assertNotEquals(
            binding().logicalDigest,
            binding(configuration = configuration(learningRate = "0.0002")).logicalDigest,
        )
    }

    @Test
    fun changingBeta1ChangesIdentity() {
        assertNotEquals(binding().logicalDigest, binding(parameters = parameters(beta1 = "0.8")).logicalDigest)
    }

    @Test
    fun changingBeta2ChangesIdentity() {
        assertNotEquals(binding().logicalDigest, binding(parameters = parameters(beta2 = "0.998")).logicalDigest)
    }

    @Test
    fun changingEpsilonChangesIdentity() {
        assertNotEquals(
            binding().logicalDigest,
            binding(parameters = parameters(epsilon = "0.000000001")).logicalDigest,
        )
    }

    @Test
    fun changingWeightDecayChangesIdentity() {
        assertNotEquals(
            binding().logicalDigest,
            binding(parameters = parameters(weightDecay = "0.02")).logicalDigest,
        )
    }

    @Test
    fun mappingRemainsIndependentlyIncomplete() {
        assertEquals(
            HimOptimizerMappingV1.ProfileCompleteness.INCOMPLETE,
            mapping().profileCompleteness,
        )
        assertEquals(6, mapping().unresolvedMandatoryParameters.size)
    }

    @Test
    fun composedBindingIsComplete() {
        assertEquals(HimAdamWRuntimeBindingV1.ProfileCompleteness.COMPLETE, binding().profileCompleteness)
    }

    @Test
    fun composedBindingReportsRuntimeReady() {
        assertTrue(binding().runtimeReady)
    }

    @Test
    fun noNumericallyRelevantSemanticsRemainUnbound() {
        assertTrue(binding().numericallyRelevantUnboundOptimizerSemantics.isEmpty())
    }

    @Test
    fun noFrameworkSpecificOptimizerClassExists() {
        val names = HimAdamWRuntimeBindingV1::class.java.declaredFields.map { it.name }

        assertTrue(names.none { it.contains("torch", ignoreCase = true) })
        assertTrue(names.none { it.contains("python", ignoreCase = true) })
        assertTrue(names.none { it.contains("tensorflow", ignoreCase = true) })
    }

    @Test
    fun noPythonOrPyTorchBindingExists() {
        val names = HimAdamWRuntimeBindingV1::class.java.declaredMethods.map { it.name }

        assertTrue(names.none { it.contains("python", ignoreCase = true) })
        assertTrue(names.none { it.contains("pytorch", ignoreCase = true) })
        assertTrue(names.none { it.contains("process", ignoreCase = true) })
    }

    @Test
    fun noOptimizerExecutionApiExists() {
        val methodNames = HimAdamWRuntimeBindingV1::class.java.declaredMethods.map { it.name }

        assertTrue(methodNames.none { it.contains("execute", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("step", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("update", ignoreCase = true) })
    }

    @Test
    fun noTrainerProtocolMutationApiExists() {
        val methodNames = HimAdamWRuntimeBindingV1::class.java.declaredMethods.map { it.name }

        assertTrue(methodNames.none { it.contains("protocol", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("mutat", ignoreCase = true) })
    }

    @Test
    fun noPersistenceNetworkOrTrainingExecutionApiExists() {
        val methodNames = HimAdamWRuntimeBindingV1::class.java.declaredMethods.map { it.name }

        assertTrue(methodNames.none { it.contains("persist", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("network", ignoreCase = true) })
        assertTrue(methodNames.none { it == "train" || it == "executeTraining" })
    }

    @Test
    fun noConfigurationOnlyAuthorizationPathExists() {
        val createMethods = HimAdamWRuntimeBindingV1::class.java.declaredMethods.filter {
            it.name == "create" && Modifier.isPublic(it.modifiers)
        }

        assertEquals(1, createMethods.size)
        assertEquals(4, createMethods.single().parameterTypes.size)
        assertEquals(HimTrainingMissionV1.Mission::class.java, createMethods.single().parameterTypes.first())
    }

    @Test
    fun noParametersOnlyAuthorizationPathExists() {
        val create = HimAdamWRuntimeBindingV1::class.java.declaredMethods.single {
            it.name == "create" && Modifier.isPublic(it.modifiers)
        }

        assertFalse(create.parameterTypes.any { it == HimAdamWParametersV1::class.java && create.parameterTypes.size == 1 })
    }

    @Test
    fun runtimeBindingReferenceBindsDigest() {
        val binding = binding()

        assertEquals("adamw-runtime-binding:v1:${binding.logicalDigest.value}", binding.runtimeBindingReference)
    }

    @Test
    fun runtimeBindingDigestIsValidatedSha256() {
        assertTrue(binding().logicalDigest.value.matches(Regex("[0-9a-f]{64}")))
    }

    private fun binding(
        configuration: HimTrainingConfigurationV1 = configuration(),
        mapping: HimOptimizerMappingV1 = mapping(),
        parameters: HimAdamWParametersV1 = parameters(),
    ): HimAdamWRuntimeBindingV1 = HimAdamWRuntimeBindingV1.create(
        mission = Fixture.mission(configuration),
        configuration = configuration,
        mapping = mapping,
        parameters = parameters,
    )

    private fun configuration(
        seed: Long = 7L,
        learningRate: String = "0.0001",
        optimizerId: String = "optimizer:adamw:v1",
    ): HimTrainingConfigurationV1 = HimTrainingConfigurationV1.create(
        seed = seed,
        epochs = 3,
        microBatchSize = 8,
        gradientAccumulationSteps = 4,
        learningRate = BigDecimal(learningRate),
        optimizerId = optimizerId,
    )

    private fun mapping(): HimOptimizerMappingV1 = HimOptimizerMappingV1.create()

    private fun parameters(
        beta1: String = "0.9",
        beta2: String = "0.999",
        epsilon: String = "0.00000001",
        weightDecay: String = "0.01",
    ): HimAdamWParametersV1 = HimAdamWParametersV1.create(
        beta1 = BigDecimal(beta1),
        beta2 = BigDecimal(beta2),
        epsilon = BigDecimal(epsilon),
        weightDecay = BigDecimal(weightDecay),
    )

    private object Fixture {
        private val ready = HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready(
            snapshotBinding = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1(
                snapshotId = "training-corpus:v1:${digest("snapshot")}",
                corpusLogicalDigest = HimSha256(digest("snapshot")),
            ),
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
                diagnostics = emptyList<HimTrainingPartitionLeakageDiagnosticV1>(),
            ),
        )

        val mission: HimTrainingMissionV1.Mission
            get() = mission(defaultConfiguration())

        private fun defaultConfiguration(): HimTrainingConfigurationV1 = HimTrainingConfigurationV1.create(
            seed = 7L,
            epochs = 3,
            microBatchSize = 8,
            gradientAccumulationSteps = 4,
            learningRate = BigDecimal("0.0001"),
            optimizerId = "optimizer:adamw:v1",
        )

        fun mission(configuration: HimTrainingConfigurationV1): HimTrainingMissionV1.Mission =
            HimTrainingMissionV1.create(
                HimTrainingMissionV1.Request(
                    readiness = ready,
                    trainingConfiguration = HimTrainingMissionV1.TrainingConfigurationReference(
                        configuration.logicalDigest,
                    ),
                    modelBinding = HimTrainingMissionV1.ModelBindingReference(
                        HimSha256(digest("model")),
                    ),
                    implementationBinding = HimTrainingMissionV1.ImplementationBindingReference(
                        HimSha256(digest("implementation")),
                    ),
                ),
            )
    }

}
