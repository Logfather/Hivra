package de.shopme.testing.system.tools.knowledge.him.training.authority

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.authority.HimP1ProductiveTrainingAuthoritiesV1
import de.shopme.tools.knowledge.him.training.authority.HimP1HeadInitializationAuthorityV1
import de.shopme.tools.knowledge.him.training.runtime.HimAdamWParametersV1
import de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1
import de.shopme.tools.knowledge.him.training.runtime.HimXlmRBaseModelArtifactManifestV1
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RunHimP1CompleteNumericalAuthorityV1Test {
    @Test
    fun modelExecutionCoversTheStrictTenFieldBinding() {
        val model = bundle().modelExecutionBinding

        assertEquals("HIM_MODEL_EXECUTION_BINDING_V1", model.contractId)
        assertEquals("1", model.version)
        assertEquals("FacebookAI/xlm-roberta-base", model.modelId)
        assertEquals("e73636d4f797dec63c3081bb6ed5c7b0bb3f2089", model.modelRevision)
        assertEquals(768, model.hiddenWidth)
        assertEquals(514, model.maxPositionEmbeddings)
        assertEquals(5, model.primaryOutputClasses)
        assertEquals(2, model.secondaryOutputClasses)
        assertEquals(
            "him-head-initialization:v1:5d2a22297eae779929aa0ddc478255b09b992474e63b81bced97ab9f8727ccc0",
            model.headInitializationBinding.reference,
        )
        assertEquals(
            "him-full-initial-state:v1:3c3771b5a0864d578f7263d2a602eac3cfc5e0f45f3c4a68b5480603869a6809",
            model.fullInitialStateReference,
        )
        assertEquals("75fcb7c12864c36a002dbbf3080682e5f92ee4a278e7500c2e3466ff06833aca", model.logicalDigest.value)
        assertEquals("him-model-execution-binding:v1:${model.logicalDigest.value}", model.reference)
    }

    @Test
    fun modelExecutionRejectsBindingTampering() {
        val model = bundle().modelExecutionBinding

        assertThrows(IllegalArgumentException::class.java) { model.copy(headContractReference = "wrong") }
        assertThrows(IllegalArgumentException::class.java) { model.copy(fullInitialStateLogicalDigest = HimSha256("0".repeat(64))) }
        assertThrows(IllegalArgumentException::class.java) { model.copy(logicalDigest = HimSha256("0".repeat(64))) }
    }

    @Test
    fun forwardRngCoversTheStrictFourteenFieldBinding() {
        val rng = bundle().forwardRngAuthority

        assertEquals("HIM_TRAIN_FORWARD_RNG_CONTRACT_V1", rng.contractId)
        assertEquals("1", rng.version)
        assertEquals("TRAIN_FORWARD_RNG_AUTHORITY_DEFINED", rng.state)
        assertEquals("training-configuration.seed", rng.rootSeedSource)
        assertEquals(7L, rng.rootSeed)
        assertEquals("HIM_TRAIN_FORWARD_RNG_V1:MODEL_FORWARD", rng.domainSeparator)
        assertEquals(4662728920316013335L, rng.derivedForwardSeed)
        assertEquals("cpu", rng.device)
        assertEquals("ONCE_PER_RUN", rng.initialization)
        assertEquals("NO_RESET_BEFORE_BATCH", rng.resetPolicy)
        assertEquals("SEQUENTIAL_STATE_ADVANCE", rng.streamSemantics)
        assertEquals("FORKED_CPU_RNG_STATE_RESTORED", rng.globalStatePolicy)
        assertEquals("d7697ab12d618d6a7f7287c067b75b4dbf249c0c2903a90ba42c0aad64cfad44", rng.logicalDigest.value)
    }

    @Test
    fun forwardRngRejectsSeedPolicyAndIdentityTampering() {
        val rng = bundle().forwardRngAuthority

        assertThrows(IllegalArgumentException::class.java) { rng.copy(rootSeed = 8L) }
        assertThrows(IllegalArgumentException::class.java) { rng.copy(state = "OTHER") }
        assertThrows(IllegalArgumentException::class.java) { rng.copy(logicalDigest = HimSha256("0".repeat(64))) }
    }

    @Test
    fun lossCoversTheStrictTwentySevenFieldBinding() {
        val loss = bundle().lossAuthority

        assertEquals("HIM_MASKED_MULTI_OBJECTIVE_LOSS_CONTRACT_V1", loss.contractId)
        assertEquals("MASKED_MULTI_OBJECTIVE_LOSS_DEFINED", loss.state)
        assertEquals(listOf("EXISTING_CANONICAL", "IDENTITY", "VARIANT", "ALIAS", "NEW_CANONICAL"), loss.primaryClassOrder)
        assertEquals(listOf(1, 2, 3, 4, 5), loss.primaryTargetCodes)
        assertEquals(listOf("COMPATIBLE", "REJECT"), loss.secondaryClassOrder)
        assertEquals(listOf(0, 1), loss.secondaryTargetCodes)
        assertEquals(listOf(0.0, 1.0), loss.objectiveMaskAllowedValues)
        assertEquals("PRIMARY_WEIGHTED_PLUS_SECONDARY_WEIGHTED", loss.totalLossFormula)
        assertEquals("ACTIVE_MEAN", loss.maskedLossNormalization)
        assertEquals("93cad16ebce71ea6ddc91b4799b2326f6069b95c494b3d0654f79c71b817e4ea", loss.logicalDigest.value)
        loss.requireActiveObjectives(listOf("TARGET_KIND", "CANDIDATE_COMPATIBILITY"))
        assertThrows(IllegalArgumentException::class.java) { loss.requireActiveObjectives(emptyList()) }
    }

    @Test
    fun lossRejectsMappingMaskFormulaAndIdentityTampering() {
        val loss = bundle().lossAuthority

        assertThrows(IllegalArgumentException::class.java) { loss.copy(primaryTargetCodes = listOf(0, 1, 2, 3, 4)) }
        assertThrows(IllegalArgumentException::class.java) { loss.copy(objectiveMaskDtype = "FLOAT64") }
        assertThrows(IllegalArgumentException::class.java) { loss.copy(totalLossFormula = "SECONDARY_ONLY") }
        assertThrows(IllegalArgumentException::class.java) { loss.copy(reference = "wrong") }
    }

    @Test
    fun trainabilityCoversTheStrictSixteenFieldBinding() {
        val policy = bundle().trainabilityPolicy

        assertEquals("HIM_BASE_ENCODER_TRAINABILITY_POLICY_V1", policy.contractId)
        assertEquals("BASE_ENCODER_TRAINABILITY_POLICY_VALIDATED", policy.state)
        assertEquals("FULL_FINE_TUNE", policy.policyKind)
        assertEquals(true, policy.baseEncoderTrainable)
        assertEquals("ALL_BASE_ENCODER_PARAMETERS", policy.baseEncoderScope)
        assertEquals(listOf("base_model.**", "primary_head.**", "secondary_head.**"), policy.trainableParameterScopes)
        assertEquals(emptyList<String>(), policy.frozenParameterScopes)
        assertEquals(true, policy.embeddingsTrainable)
        assertEquals(277_458_439L, policy.trainableParameterCount)
        assertEquals(0L, policy.frozenParameterCount)
        assertEquals("c94420c89a46fd703706dfeb7bd112b6bd0e44e4d8a29cdeba29f4028b56e8e1", policy.logicalDigest.value)
    }

    @Test
    fun trainabilityRejectsPartialFreezeAndIdentityTampering() {
        val policy = bundle().trainabilityPolicy

        assertThrows(IllegalArgumentException::class.java) { policy.copy(baseEncoderTrainable = false) }
        assertThrows(IllegalArgumentException::class.java) { policy.copy(frozenParameterScopes = listOf("base_model.**")) }
        assertThrows(IllegalArgumentException::class.java) { policy.copy(logicalDigest = HimSha256("0".repeat(64))) }
    }

    @Test
    fun optimizerCoversTheStrictThirtyFourFieldBinding() {
        val optimizer = bundle().optimizerExecutionPolicy

        assertEquals("HIM_OPTIMIZER_EXECUTION_POLICY_V1", optimizer.contractId)
        assertEquals("OPTIMIZER_EXECUTION_POLICY_VALIDATED", optimizer.state)
        assertEquals("optimizer:adamw:v1", optimizer.optimizerId)
        assertEquals("ADAMW", optimizer.optimizerType)
        assertEquals("0.0001", optimizer.learningRate)
        assertEquals("0.9", optimizer.beta1)
        assertEquals("0.999", optimizer.beta2)
        assertEquals("0.00000001", optimizer.epsilon)
        assertEquals("0.01", optimizer.weightDecay)
        assertEquals(true, optimizer.decoupledWeightDecay)
        assertEquals(true, optimizer.biasCorrection)
        assertEquals("STANDARD_BIAS_AND_LAYERNORM_NO_DECAY", optimizer.parameterGroupPolicy)
        assertEquals("SET_TO_NONE", optimizer.zeroGradPolicy)
        assertEquals(1, optimizer.gradientAccumulationSteps)
        assertEquals("NONE", optimizer.gradientClippingPolicy)
        assertEquals("NONE", optimizer.lrSchedulerPolicy)
        assertEquals("NONE", optimizer.warmupPolicy)
        assertEquals("FP32", optimizer.precisionPolicy)
        assertEquals(false, optimizer.ampEnabled)
        assertEquals(false, optimizer.gradScalerEnabled)
        assertEquals("33bcb06598d58f13093e181a3e20c880063abba12662298358210df32d6752c2", optimizer.logicalDigest.value)
    }

    @Test
    fun optimizerRejectsHyperparameterExecutionAndIdentityTampering() {
        val optimizer = bundle().optimizerExecutionPolicy

        assertThrows(IllegalArgumentException::class.java) { optimizer.copy(learningRate = "0.001") }
        assertThrows(IllegalArgumentException::class.java) { optimizer.copy(beta1 = "0.8") }
        assertThrows(IllegalArgumentException::class.java) { optimizer.copy(gradientAccumulationSteps = 2) }
        assertThrows(IllegalArgumentException::class.java) { optimizer.copy(precisionPolicy = "BF16") }
        assertThrows(IllegalArgumentException::class.java) { optimizer.copy(gradScalerEnabled = true) }
        assertThrows(IllegalArgumentException::class.java) { optimizer.copy(logicalDigest = HimSha256("0".repeat(64))) }
    }

    @Test
    fun headInitializationRemainsTheFrozenD1Predecessor() {
        val initialization = bundle().modelExecutionBinding.headInitializationBinding

        assertEquals("HIM_HEAD_INITIALIZATION_BINDING_V1", initialization.contractId)
        assertEquals("1", initialization.version)
        assertEquals("HEAD_INITIALIZATION_VALIDATED", HimP1HeadInitializationAuthorityV1.STATE)
        assertEquals("0", initialization.mean)
        assertEquals("0.02", initialization.std)
        assertEquals("0", initialization.bias)
        assertEquals("ISOLATED_TORCH_GENERATOR_CPU_V1", initialization.rng)
        assertEquals(listOf("PRIMARY", "SECONDARY"), initialization.initializationOrder)
        assertEquals("5d2a22297eae779929aa0ddc478255b09b992474e63b81bced97ab9f8727ccc0", initialization.logicalDigest.value)
    }

    @Test
    fun allAuthoritiesAreDeterministicAndRemainInTheEightAuthorityBundle() {
        val first = bundle()
        val second = bundle()

        assertEquals(first, second)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.reference, second.reference)
        assertNotEquals(first.forwardRngAuthority.logicalDigest, first.modelExecutionBinding.logicalDigest)
        assertEquals(8, listOf(first.modelExecutionBinding, first.forwardRngAuthority, first.lossAuthority, first.trainabilityPolicy,
            first.optimizerExecutionPolicy, first.corpusBinding, first.partitionBinding, first.readinessBinding).size)
    }

    private fun bundle(): HimP1ProductiveTrainingAuthoritiesV1.Bundle {
        val configuration = HimTrainingConfigurationV1.create(7L, 3, 8, 1, BigDecimal("0.0001"), "optimizer:adamw:v1")
        val model = HimP1ProductiveTrainingAuthoritiesV1.ModelExecutionBindingV1.create(modelBinding(), configuration)
        val rng = HimP1ProductiveTrainingAuthoritiesV1.ForwardRngAuthorityV1.create(configuration)
        val loss = HimP1ProductiveTrainingAuthoritiesV1.LossAuthorityV1.create()
        val trainability = HimP1ProductiveTrainingAuthoritiesV1.TrainabilityPolicyV1.create(model)
        val parameters = HimAdamWParametersV1.create(BigDecimal("0.9"), BigDecimal("0.999"), BigDecimal("0.00000001"), BigDecimal("0.01"))
        val optimizer = HimP1ProductiveTrainingAuthoritiesV1.OptimizerExecutionPolicyV1.create(configuration, parameters)
        val corpus = HimP1ProductiveTrainingAuthoritiesV1.corpusBinding()
        val partition = HimP1ProductiveTrainingAuthoritiesV1.partitionBinding()
        val readiness = HimP1ProductiveTrainingAuthoritiesV1.readinessBinding(partition)
        val digest = HimP1ProductiveTrainingAuthoritiesV1.bundleDigest(model, rng, loss, trainability, optimizer, corpus, partition, readiness)
        return HimP1ProductiveTrainingAuthoritiesV1.Bundle(
            HimP1ProductiveTrainingAuthoritiesV1.CONTRACT_ID, HimP1ProductiveTrainingAuthoritiesV1.VERSION,
            HimP1ProductiveTrainingAuthoritiesV1.STATE, model, rng, loss, trainability, optimizer, corpus, partition,
            readiness, digest, "training-authorities:v1:${digest.value}",
        )
    }

    private fun modelBinding(): HimModelBindingV1 = HimModelBindingV1.create(
        modelFamilyId = HimXlmRBaseModelArtifactManifestV1.FROZEN_MANIFEST.modelFamily,
        baseModelId = HimXlmRBaseModelArtifactManifestV1.FROZEN_MANIFEST.repositoryId,
        baseModelArtifactDigest = HimXlmRBaseModelArtifactManifestV1.FROZEN_MANIFEST.artifacts
            .single { it.role == HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_WEIGHTS }.sha256,
        tokenizerId = "xlm-roberta-base-tokenizer",
        tokenizerArtifactDigest = HimXlmRBaseModelArtifactManifestV1.FROZEN_MANIFEST.artifacts
            .single { it.role == HimXlmRBaseModelArtifactManifestV1.ArtifactRole.TOKENIZER_DEFINITION }.sha256,
        modelConfigurationArtifactDigest = HimXlmRBaseModelArtifactManifestV1.FROZEN_MANIFEST.artifacts
            .single { it.role == HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_CONFIG }.sha256,
    )
}
