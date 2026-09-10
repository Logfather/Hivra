package de.shopme.tools.knowledge.him.training.authority

import de.shopme.tools.knowledge.him.training.runtime.HimAdamWParametersV1
import de.shopme.tools.knowledge.him.training.runtime.HimExternalTrainerProcessBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1
import de.shopme.tools.knowledge.him.training.runtime.HimTrainingConfigurationV1
import de.shopme.tools.knowledge.him.training.runtime.HimXlmRBaseModelArtifactManifestV1
import java.math.BigDecimal

/** Server-side production assembler for the eight Point-13 authority bindings. */
object HimP1ProductiveTrainingAuthorityAssemblerV1 {
    const val P1_EXECUTION_DEVICE = "CUDA"
    const val P1_EXECUTION_DEVICE_INDEX = 0
    const val P1_EXECUTION_DEVICE_REFERENCE = "cuda:0"

    /** Explicit server-owned selection for the frozen P1 A100 execution path. */
    fun currentP1ExecutionDevice(): HimExternalTrainerProcessBindingV1.Device =
        HimExternalTrainerProcessBindingV1.Device.CUDA

    data class Inputs(
        val configuration: HimTrainingConfigurationV1,
        val modelBinding: HimModelBindingV1,
        val adamWParameters: HimAdamWParametersV1,
    )

    fun currentP1Inputs(): Inputs {
        val manifest = HimXlmRBaseModelArtifactManifestV1.FROZEN_MANIFEST
        val configuration = HimTrainingConfigurationV1.create(
            seed = 7L,
            epochs = 3,
            microBatchSize = 8,
            gradientAccumulationSteps = 1,
            learningRate = BigDecimal("0.0001"),
            optimizerId = "optimizer:adamw:v1",
        )
        val modelBinding = HimModelBindingV1.create(
            modelFamilyId = manifest.modelFamily,
            baseModelId = manifest.repositoryId,
            baseModelArtifactDigest = manifest.artifacts
                .single { it.role == HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_WEIGHTS }
                .sha256,
            tokenizerId = "xlm-roberta-base-tokenizer",
            tokenizerArtifactDigest = manifest.artifacts
                .single { it.role == HimXlmRBaseModelArtifactManifestV1.ArtifactRole.TOKENIZER_DEFINITION }
                .sha256,
            modelConfigurationArtifactDigest = manifest.artifacts
                .single { it.role == HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_CONFIG }
                .sha256,
        )
        val parameters = HimAdamWParametersV1.create(
            beta1 = BigDecimal("0.9"), beta2 = BigDecimal("0.999"),
            epsilon = BigDecimal("0.00000001"), weightDecay = BigDecimal("0.01"),
        )
        return Inputs(configuration, modelBinding, parameters)
    }

    fun create(inputs: Inputs): HimP1ProductiveTrainingAuthoritiesV1.Bundle {
        val model = HimP1ProductiveTrainingAuthoritiesV1.ModelExecutionBindingV1.create(
            inputs.modelBinding,
            inputs.configuration,
        )
        val rng = HimP1ProductiveTrainingAuthoritiesV1.ForwardRngAuthorityV1.create(inputs.configuration, P1_EXECUTION_DEVICE_REFERENCE)
        val loss = HimP1ProductiveTrainingAuthoritiesV1.LossAuthorityV1.create()
        val trainability = HimP1ProductiveTrainingAuthoritiesV1.TrainabilityPolicyV1.create(model)
        val optimizer = HimP1ProductiveTrainingAuthoritiesV1.OptimizerExecutionPolicyV1.create(
            inputs.configuration,
            inputs.adamWParameters,
            P1_EXECUTION_DEVICE,
        )
        val corpus = HimP1ProductiveTrainingAuthoritiesV1.corpusBinding()
        val partition = HimP1ProductiveTrainingAuthoritiesV1.partitionBinding()
        val readiness = HimP1ProductiveTrainingAuthoritiesV1.readinessBinding(partition)
        val bundleDigest = HimP1ProductiveTrainingAuthoritiesV1.bundleDigest(
            model, rng, loss, trainability, optimizer, corpus, partition, readiness,
        )
        return HimP1ProductiveTrainingAuthoritiesV1.Bundle(
            HimP1ProductiveTrainingAuthoritiesV1.CONTRACT_ID,
            HimP1ProductiveTrainingAuthoritiesV1.VERSION,
            HimP1ProductiveTrainingAuthoritiesV1.STATE,
            model, rng, loss, trainability, optimizer, corpus, partition, readiness,
            bundleDigest, "training-authorities:v1:${bundleDigest.value}",
        ).also { it.validateCompatibility() }
    }

    /** The current frozen P1 input set; it does not read files or execute a runtime. */
    fun currentP1(): HimP1ProductiveTrainingAuthoritiesV1.Bundle {
        return create(currentP1Inputs())
    }

}
