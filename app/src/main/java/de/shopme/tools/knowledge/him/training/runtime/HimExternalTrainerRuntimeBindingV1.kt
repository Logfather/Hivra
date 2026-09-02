package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.security.MessageDigest

private const val BINDING_REFERENCE_PREFIX = "external-trainer-runtime-binding:v1:"
private const val EXECUTION_REQUEST_REFERENCE_PREFIX = "external-trainer-execution-request:v1:"
private val BINDING_REFERENCE_PATTERN = Regex("external-trainer-runtime-binding:v1:[0-9a-f]{64}")

/**
 * Immutable authorization binding between one external-trainer request and
 * one already validated stable Python/PyTorch runtime environment.
 * This type performs no runtime probing or execution.
 */
class HimExternalTrainerRuntimeBindingV1 private constructor(
    val executionRequest: HimExternalTrainerExecutionRequestV1,
    val runtimeEnvironment: HimPythonPyTorchRuntimeEnvironmentV1,
    val executionRequestDigest: HimSha256,
    val executionRequestReference: String,
    val runtimeEnvironmentDigest: HimSha256,
    val runtimeEnvironmentReference: String,
    val logicalDigest: HimSha256,
    val bindingReference: String,
) {
    val contractId: String
        get() = CONTRACT_ID

    val version: String
        get() = VERSION

    val state: String
        get() = STATE

    init {
        require(contractId == CONTRACT_ID)
        require(version == VERSION)
        require(state == STATE)
        require(executionRequest.contractId == HimExternalTrainerExecutionRequestV1.CONTRACT_ID)
        require(executionRequest.version == HimExternalTrainerExecutionRequestV1.VERSION)
        require(executionRequest.state == HimExternalTrainerExecutionRequestV1.STATE)
        require(executionRequest.requestReference ==
            "$EXECUTION_REQUEST_REFERENCE_PREFIX${executionRequest.logicalDigest.value}") {
            "EXECUTION_REQUEST_REFERENCE_MISMATCH"
        }
        require(executionRequestDigest == executionRequest.logicalDigest) {
            "EXECUTION_REQUEST_DIGEST_MISMATCH"
        }
        require(executionRequestReference == executionRequest.requestReference) {
            "EXECUTION_REQUEST_REFERENCE_MISMATCH"
        }
        require(runtimeEnvironment.runtimeChannel == HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.STABLE) {
            "RUNTIME_CHANNEL_REQUIRED_STABLE"
        }
        require(runtimeEnvironment.contractId == HimPythonPyTorchRuntimeEnvironmentV1.CONTRACT_ID)
        require(runtimeEnvironment.version == HimPythonPyTorchRuntimeEnvironmentV1.VERSION)
        require(runtimeEnvironment.state == HimPythonPyTorchRuntimeEnvironmentV1.STATE)
        require(runtimeEnvironment.environmentReference ==
            "python-pytorch-runtime-environment:v1:${runtimeEnvironment.logicalDigest.value}") {
            "RUNTIME_ENVIRONMENT_REFERENCE_MISMATCH"
        }
        require(runtimeEnvironmentDigest == runtimeEnvironment.logicalDigest) {
            "RUNTIME_ENVIRONMENT_DIGEST_MISMATCH"
        }
        require(runtimeEnvironmentReference == runtimeEnvironment.environmentReference) {
            "RUNTIME_ENVIRONMENT_REFERENCE_MISMATCH"
        }
        require(BINDING_REFERENCE_PATTERN.matches(bindingReference)) {
            "RUNTIME_BINDING_REFERENCE_INVALID"
        }
        require(bindingReference == "$BINDING_REFERENCE_PREFIX${logicalDigest.value}") {
            "RUNTIME_BINDING_REFERENCE_MISMATCH"
        }
        require(logicalDigest == bindingDigest(executionRequest, runtimeEnvironment)) {
            "RUNTIME_BINDING_DIGEST_MISMATCH"
        }
    }

    override fun equals(other: Any?): Boolean =
        other is HimExternalTrainerRuntimeBindingV1 && logicalDigest == other.logicalDigest

    override fun hashCode(): Int = logicalDigest.hashCode()

    companion object {
        const val CONTRACT_ID = "HIM_EXTERNAL_TRAINER_RUNTIME_BINDING_V1"
        const val VERSION = "1"
        const val STATE = "EXTERNAL_TRAINER_RUNTIME_BOUND"
        const val RUNTIME_BINDING_REQUIRES_EXECUTION_REQUEST = "YES"
        const val RUNTIME_BINDING_REQUIRES_RUNTIME_ENVIRONMENT = "YES"
        const val ALTERNATE_RUNTIME_BINDING_AUTHORIZATION_PATHS = 0
        const val EXECUTION_REQUEST_IDENTITY_PRESERVED = "YES"
        const val EXECUTION_REQUEST_REIMPLEMENTATION = 0
        const val RUNTIME_ENVIRONMENT_IDENTITY_PRESERVED = "YES"
        const val RUNTIME_ENVIRONMENT_REIMPLEMENTATION = 0
        const val RUNTIME_CHANNEL_REQUIRED = "STABLE"
        const val ENVIRONMENT_IMPLEMENTATION_FINGERPRINT_CONTINUITY = "TRANSITIVE"
        const val TRAINER_IMPLEMENTATION_BOUND = "NO"
        const val DEVICE_SELECTION_BOUND = "NO"
        const val MPS_DEVICE_SELECTED = "NO"
        const val MODEL_ARTIFACT_REVERIFICATION = 0
        const val TRAINING_SEMANTICS_REIMPLEMENTATION = 0
        const val PYTORCH_RUNTIME_BOUND = "YES"
        const val PYTHON_RUNTIME_BOUND = "YES"
        const val ABSOLUTE_RUNTIME_PATH_IN_BINDING_DIGEST = "NO"
        const val PROCESS_COMMAND_BOUND = "NO"
        const val PROCESS_EXECUTION = 0
        const val RUNTIME_PROBING = 0
        const val NETWORK_ACCESS = 0
        const val PACKAGE_INSTALLATION = 0
        const val MODEL_LOAD_EXECUTION = 0
        const val TOKENIZATION_EXECUTION = 0
        const val TENSORIZATION = 0
        const val NUMERICAL_TRAINING_EXECUTION = 0
        const val RUNTIME_BINDING_PERSISTENCE = 0
        const val FROZEN_UPSTREAM_CONTRACT_MODIFICATIONS = 0
        const val PYTORCH_PROCESS_STARTED = "NO"
        const val PYTORCH_TRAINING_EXECUTED = "NO"
        const val PYTHON_PROCESS_STARTED = "NO"
        const val PROCESS_ADAPTER_CAN_REQUIRE_RUNTIME_BINDING = "YES"

        @JvmStatic
        fun create(
            executionRequest: HimExternalTrainerExecutionRequestV1,
            runtimeEnvironment: HimPythonPyTorchRuntimeEnvironmentV1,
        ): HimExternalTrainerRuntimeBindingV1 {
            require(runtimeEnvironment.runtimeChannel == HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.STABLE) {
                "RUNTIME_CHANNEL_REQUIRED_STABLE"
            }
            val digest = bindingDigest(executionRequest, runtimeEnvironment)
            return HimExternalTrainerRuntimeBindingV1(
                executionRequest = executionRequest,
                runtimeEnvironment = runtimeEnvironment,
                executionRequestDigest = executionRequest.logicalDigest,
                executionRequestReference = executionRequest.requestReference,
                runtimeEnvironmentDigest = runtimeEnvironment.logicalDigest,
                runtimeEnvironmentReference = runtimeEnvironment.environmentReference,
                logicalDigest = digest,
                bindingReference = "$BINDING_REFERENCE_PREFIX${digest.value}",
            )
        }

        private fun bindingDigest(
            executionRequest: HimExternalTrainerExecutionRequestV1,
            runtimeEnvironment: HimPythonPyTorchRuntimeEnvironmentV1,
        ): HimSha256 = HimSha256(
            MessageDigest.getInstance("SHA-256")
                .digest(
                    buildString {
                        field("contract", CONTRACT_ID)
                        field("version", VERSION)
                        field("state", STATE)
                        field("execution-request-digest", executionRequest.logicalDigest.value)
                        field("execution-request-reference", executionRequest.requestReference)
                        field("runtime-environment-digest", runtimeEnvironment.logicalDigest.value)
                        field("runtime-environment-reference", runtimeEnvironment.environmentReference)
                    }.toByteArray(Charsets.UTF_8),
                )
                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )

        private fun StringBuilder.field(key: String, value: String) {
            append(key).append('=').append(value.length).append(':').append(value).append('\n')
        }
    }
}
