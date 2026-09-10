package de.shopme.tools.knowledge.him.training.checkpoint

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * The immutable semantic authority for one reloadable trained HIM state.
 *
 * The contract deliberately describes opaque model/optimizer artifacts rather
 * than depending on a Python or PyTorch serialization library. The host
 * persistence implementation owns the filesystem protocol; this type owns
 * the identity, binding and digest semantics shared by every host.
 */
object HimTrainingCheckpointContractV1 {
    const val CONTRACT_ID = "HIM_TRAINING_CHECKPOINT_V1"
    const val VERSION = "1"
    const val STATE = "TRAINING_CHECKPOINT_VALIDATED"
    const val CHECKPOINT_DIRECTORY_NAME = "checkpoint-v1"
    const val MANIFEST_FILE_NAME = "checkpoint-manifest.v1.json"
    const val MODEL_STATE_FILE_NAME = "model-state.bin"
    const val OPTIMIZER_STATE_FILE_NAME = "optimizer-state.bin"
    const val MODEL_STATE_FORMAT = "PYTORCH_SAFE_TENSOR_STATE_V1"
    const val OPTIMIZER_STATE_FORMAT = "PYTORCH_COMPATIBLE_OPTIMIZER_STATE_V1"
    const val CREATION_IDENTITY_POLICY = "DETERMINISTIC_NO_TIMESTAMP"
    const val CHECKPOINT_DIGEST_ALGORITHM = "SHA-256"
    const val ATOMIC_PERSISTENCE_POLICY = "TEMPORARY_SIBLING_FSYNC_VERIFY_ATOMIC_PROMOTION"
    const val OVERWRITE_POLICY = "FAIL_CLOSED"
    const val RELOAD_POLICY = "VALIDATE_ALL_BINDINGS_AND_ARTIFACT_DIGESTS_BEFORE_USE"

    private val DIGEST = Regex("[0-9a-f]{64}")
    private val REFERENCE = Regex("[^\\u0000-\\r\\n]{1,512}")

    data class BindingV1(
        val productiveRequestDigest: HimSha256,
        val productiveRequestReference: String,
        val durableManifestSha256: HimSha256,
        val baseModelReference: String,
        val baseModelRevision: String,
        val baseModelWeightsSha256: HimSha256,
        val modelBindingDigest: HimSha256,
        val tokenizerId: String,
        val corpusIdentity: String,
        val partitionIdentity: String,
        val point9Identity: String,
        val point13Identity: String,
        val cudaDeviceAuthority: String,
        val seed: Long,
        val numericalPolicyIdentity: String,
        val completedOptimizerSteps: Int,
        val trainingRunId: String,
    ) {
        init {
            require(DIGEST.matches(productiveRequestDigest.value))
            require(REFERENCE.matches(productiveRequestReference))
            require(DIGEST.matches(durableManifestSha256.value))
            require(REFERENCE.matches(baseModelReference))
            require(REFERENCE.matches(baseModelRevision))
            require(DIGEST.matches(baseModelWeightsSha256.value))
            require(DIGEST.matches(modelBindingDigest.value))
            require(REFERENCE.matches(tokenizerId))
            require(REFERENCE.matches(corpusIdentity))
            require(REFERENCE.matches(partitionIdentity))
            require(REFERENCE.matches(point9Identity))
            require(REFERENCE.matches(point13Identity))
            require(cudaDeviceAuthority == "CUDA:0")
            require(seed >= 0)
            require(REFERENCE.matches(numericalPolicyIdentity))
            require(completedOptimizerSteps >= 0)
            require(REFERENCE.matches(trainingRunId))
        }
    }

    data class ArtifactV1(
        val relativePath: String,
        val byteCount: Long,
        val sha256: HimSha256,
    ) {
        init {
            require(relativePath == MODEL_STATE_FILE_NAME || relativePath == OPTIMIZER_STATE_FILE_NAME)
            require(byteCount >= 0)
            require(DIGEST.matches(sha256.value))
            require('/' !in relativePath && '\\' !in relativePath)
        }
    }

    data class ManifestV1(
        val contractId: String,
        val version: String,
        val state: String,
        val binding: BindingV1,
        val modelState: ArtifactV1,
        val optimizerState: ArtifactV1,
        val checkpointDigest: HimSha256,
    ) {
        init {
            require(contractId == CONTRACT_ID)
            require(version == VERSION)
            require(state == STATE)
            require(modelState.relativePath == MODEL_STATE_FILE_NAME)
            require(optimizerState.relativePath == OPTIMIZER_STATE_FILE_NAME)
            require(modelState.relativePath != optimizerState.relativePath)
            require(DIGEST.matches(checkpointDigest.value))
            require(checkpointDigest == digestFor(binding, modelState, optimizerState))
        }
    }

    fun createManifest(
        binding: BindingV1,
        modelState: ArtifactV1,
        optimizerState: ArtifactV1,
    ): ManifestV1 = ManifestV1(
        contractId = CONTRACT_ID,
        version = VERSION,
        state = STATE,
        binding = binding,
        modelState = modelState,
        optimizerState = optimizerState,
        checkpointDigest = digestFor(binding, modelState, optimizerState),
    )

    fun digestFor(
        binding: BindingV1,
        modelState: ArtifactV1,
        optimizerState: ArtifactV1,
    ): HimSha256 = HimSha256(sha256(canonicalDigestPayload(binding, modelState, optimizerState)))

    /** Public so non-JVM trainer implementations can reproduce the authority exactly. */
    fun canonicalDigestPayload(
        binding: BindingV1,
        modelState: ArtifactV1,
        optimizerState: ArtifactV1,
    ): String = buildString {
        field("contract-id", CONTRACT_ID)
        field("version", VERSION)
        field("state", STATE)
        field("productive-request-digest", binding.productiveRequestDigest.value)
        field("productive-request-reference", binding.productiveRequestReference)
        field("durable-manifest-sha256", binding.durableManifestSha256.value)
        field("base-model-reference", binding.baseModelReference)
        field("base-model-revision", binding.baseModelRevision)
        field("base-model-weights-sha256", binding.baseModelWeightsSha256.value)
        field("model-binding-digest", binding.modelBindingDigest.value)
        field("tokenizer-id", binding.tokenizerId)
        field("corpus-identity", binding.corpusIdentity)
        field("partition-identity", binding.partitionIdentity)
        field("point-9-identity", binding.point9Identity)
        field("point-13-identity", binding.point13Identity)
        field("cuda-device-authority", binding.cudaDeviceAuthority)
        field("seed", binding.seed.toString())
        field("numerical-policy-identity", binding.numericalPolicyIdentity)
        field("completed-optimizer-steps", binding.completedOptimizerSteps.toString())
        field("training-run-id", binding.trainingRunId)
        field("model-state-path", modelState.relativePath)
        field("model-state-byte-count", modelState.byteCount.toString())
        field("model-state-sha256", modelState.sha256.value)
        field("optimizer-state-path", optimizerState.relativePath)
        field("optimizer-state-byte-count", optimizerState.byteCount.toString())
        field("optimizer-state-sha256", optimizerState.sha256.value)
    }

    private fun StringBuilder.field(key: String, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        append(key).append('=').append(bytes.size).append(':').append(value).append('\n')
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
