package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.security.MessageDigest

private const val RESULT_REFERENCE_PREFIX = "external-trainer-process-result:v1:"
private const val REQUEST_REFERENCE_PREFIX = "external-trainer-process-request-serialization:v1:"
private const val OUTPUT_REFERENCE_PREFIX = "external-trainer-output:v1:"
private val ZERO_DIGEST = "0".repeat(64)

/**
 * Framework-neutral, deterministic result domain for one external trainer process.
 * This type records a claimed outcome only; it performs no parsing, I/O, or acceptance.
 */
object HimExternalTrainerProcessResultV1 {
    enum class ProcessStatus {
        COMPLETED,
        FAILED,
    }

    enum class TrainingOutcome {
        TRAINING_COMPLETED,
        TRAINING_FAILED,
        NO_TRAINING_RESULT,
    }

    enum class FailureReason {
        PROCESS_FAILED,
        TRAINING_FAILED,
        INVALID_RESULT,
    }

    enum class OutputArtifactRole {
        FINAL_MODEL,
        CHECKPOINT,
        TRAINING_METADATA,
    }

    data class RequestSerializationIdentity(
        val reference: String,
        val logicalDigest: HimSha256,
    ) {
        init {
            require(reference == "$REQUEST_REFERENCE_PREFIX${logicalDigest.value}") {
                "REQUEST_SERIALIZATION_REFERENCE_MISMATCH"
            }
        }
    }

    data class OutputArtifact(
        val role: OutputArtifactRole,
        val reference: String,
        val sha256: HimSha256,
        val byteSize: Long?,
        val operationalPath: String?,
    ) {
        init {
            require(reference == "$OUTPUT_REFERENCE_PREFIX${role.name}:${sha256.value}") {
                "OUTPUT_ARTIFACT_REFERENCE_MISMATCH"
            }
            require(byteSize == null || byteSize >= 0) {
                "OUTPUT_ARTIFACT_SIZE_INVALID"
            }
            require(operationalPath == null || operationalPath.isNotBlank()) {
                "OUTPUT_ARTIFACT_PATH_INVALID"
            }
        }
    }

    sealed interface Result {
        val processBinding: HimExternalTrainerProcessBindingV1
        val requestSerialization: RequestSerializationIdentity
        val processStatus: ProcessStatus
        val processExitCode: Int
        val trainingOutcome: TrainingOutcome
        val failureReason: FailureReason?
        val diagnosticsDigest: HimSha256
        val outputArtifacts: List<OutputArtifact>
        val runtimeBindingDigest: HimSha256
        val runtimeBindingReference: String
        val runtimeEnvironmentDigest: HimSha256
        val runtimeEnvironmentReference: String
        val executionRequestDigest: HimSha256
        val executionRequestReference: String
        val trainingMissionDigest: HimSha256
        val trainingMissionReference: String
        val trainingConfigurationDigest: HimSha256
        val trainingConfigurationReference: String
        val modelBindingDigest: HimSha256
        val modelBindingReference: String
        val trainerModule: String
        val trainerImplementationFingerprint: HimSha256
        val trainerImplementationReference: String
        val processAdapterImplementationFingerprint: HimSha256
        val device: HimExternalTrainerProcessBindingV1.Device
        val logicalDigest: HimSha256
        val resultReference: String

        class Completed private constructor(
            private val values: Values,
        ) : Result {
            override val processBinding get() = values.processBinding
            override val requestSerialization get() = values.requestSerialization
            override val processStatus get() = ProcessStatus.COMPLETED
            override val processExitCode get() = values.processExitCode
            override val trainingOutcome get() = values.trainingOutcome
            override val failureReason get() = values.failureReason
            override val diagnosticsDigest get() = values.diagnosticsDigest
            override val outputArtifacts get() = values.outputArtifacts
            override val runtimeBindingDigest get() = values.runtimeBindingDigest
            override val runtimeBindingReference get() = values.runtimeBindingReference
            override val runtimeEnvironmentDigest get() = values.runtimeEnvironmentDigest
            override val runtimeEnvironmentReference get() = values.runtimeEnvironmentReference
            override val executionRequestDigest get() = values.executionRequestDigest
            override val executionRequestReference get() = values.executionRequestReference
            override val trainingMissionDigest get() = values.trainingMissionDigest
            override val trainingMissionReference get() = values.trainingMissionReference
            override val trainingConfigurationDigest get() = values.trainingConfigurationDigest
            override val trainingConfigurationReference get() = values.trainingConfigurationReference
            override val modelBindingDigest get() = values.modelBindingDigest
            override val modelBindingReference get() = values.modelBindingReference
            override val trainerModule get() = values.trainerModule
            override val trainerImplementationFingerprint get() = values.trainerImplementationFingerprint
            override val trainerImplementationReference get() = values.trainerImplementationReference
            override val processAdapterImplementationFingerprint get() = values.processAdapterImplementationFingerprint
            override val device get() = values.device
            override val logicalDigest get() = values.logicalDigest
            override val resultReference get() = values.resultReference

            override fun equals(other: Any?): Boolean =
                other is Completed && logicalDigest == other.logicalDigest

            override fun hashCode(): Int = logicalDigest.hashCode()

            override fun toString(): String =
                "Completed(processStatus=$processStatus, trainingOutcome=$trainingOutcome, logicalDigest=$logicalDigest)"

            companion object {
                internal fun create(values: Values): Completed = Completed(values)
            }
        }

        class Failed private constructor(
            private val values: Values,
        ) : Result {
            override val processBinding get() = values.processBinding
            override val requestSerialization get() = values.requestSerialization
            override val processStatus get() = ProcessStatus.FAILED
            override val processExitCode get() = values.processExitCode
            override val trainingOutcome get() = values.trainingOutcome
            override val failureReason get() = values.failureReason
            override val diagnosticsDigest get() = values.diagnosticsDigest
            override val outputArtifacts get() = values.outputArtifacts
            override val runtimeBindingDigest get() = values.runtimeBindingDigest
            override val runtimeBindingReference get() = values.runtimeBindingReference
            override val runtimeEnvironmentDigest get() = values.runtimeEnvironmentDigest
            override val runtimeEnvironmentReference get() = values.runtimeEnvironmentReference
            override val executionRequestDigest get() = values.executionRequestDigest
            override val executionRequestReference get() = values.executionRequestReference
            override val trainingMissionDigest get() = values.trainingMissionDigest
            override val trainingMissionReference get() = values.trainingMissionReference
            override val trainingConfigurationDigest get() = values.trainingConfigurationDigest
            override val trainingConfigurationReference get() = values.trainingConfigurationReference
            override val modelBindingDigest get() = values.modelBindingDigest
            override val modelBindingReference get() = values.modelBindingReference
            override val trainerModule get() = values.trainerModule
            override val trainerImplementationFingerprint get() = values.trainerImplementationFingerprint
            override val trainerImplementationReference get() = values.trainerImplementationReference
            override val processAdapterImplementationFingerprint get() = values.processAdapterImplementationFingerprint
            override val device get() = values.device
            override val logicalDigest get() = values.logicalDigest
            override val resultReference get() = values.resultReference

            override fun equals(other: Any?): Boolean =
                other is Failed && logicalDigest == other.logicalDigest

            override fun hashCode(): Int = logicalDigest.hashCode()

            override fun toString(): String =
                "Failed(processStatus=$processStatus, failureReason=$failureReason, logicalDigest=$logicalDigest)"

            companion object {
                internal fun create(values: Values): Failed = Failed(values)
            }
        }
    }

    internal data class Values(
        val processBinding: HimExternalTrainerProcessBindingV1,
        val requestSerialization: RequestSerializationIdentity,
        val processExitCode: Int,
        val trainingOutcome: TrainingOutcome,
        val failureReason: FailureReason?,
        val diagnosticsDigest: HimSha256,
        val outputArtifacts: List<OutputArtifact>,
        val runtimeBindingDigest: HimSha256,
        val runtimeBindingReference: String,
        val runtimeEnvironmentDigest: HimSha256,
        val runtimeEnvironmentReference: String,
        val executionRequestDigest: HimSha256,
        val executionRequestReference: String,
        val trainingMissionDigest: HimSha256,
        val trainingMissionReference: String,
        val trainingConfigurationDigest: HimSha256,
        val trainingConfigurationReference: String,
        val modelBindingDigest: HimSha256,
        val modelBindingReference: String,
        val trainerModule: String,
        val trainerImplementationFingerprint: HimSha256,
        val trainerImplementationReference: String,
        val processAdapterImplementationFingerprint: HimSha256,
        val device: HimExternalTrainerProcessBindingV1.Device,
        val logicalDigest: HimSha256,
        val resultReference: String,
    )

    const val CONTRACT_ID = "HIM_EXTERNAL_TRAINER_PROCESS_RESULT_V1"
    const val VERSION = "1"
    const val STATE = "EXTERNAL_TRAINER_PROCESS_RESULT_REPORTED"
    const val RESULT_REFERENCE_FORMAT = "external-trainer-process-result:v1:<64-hex-logicalDigest>"
    const val PROCESS_RESULT_REQUIRES_PROCESS_BINDING = "YES"
    const val ALTERNATE_PROCESS_RESULT_AUTHORIZATION_PATHS = 0
    const val PROCESS_REQUEST_IDENTITY_INCLUDED = "YES"
    const val RUNTIME_BINDING_IDENTITY_INCLUDED = "YES"
    const val EXECUTION_REQUEST_IDENTITY_INCLUDED = "YES"
    const val TRAINER_IMPLEMENTATION_IDENTITY_INCLUDED = "YES"
    const val PROCESS_ADAPTER_IMPLEMENTATION_IDENTITY_INCLUDED = "YES"
    const val DIAGNOSTICS_DIGEST_BOUND = "YES"
    const val PATH_ONLY_MODEL_RESULT_ACCEPTED = "NO"
    const val ABSOLUTE_OUTPUT_PATH_IN_RESULT_LOGICAL_DIGEST = "NO"
    const val TRAINING_PROCESS_RESULT_PERFORMS_MODEL_PROMOTION = "NO"
    const val HOLDOUT_ACCEPTANCE_IN_PROCESS_RESULT = "NO"
    const val PROCESS_RESULT_IS_FINAL_MODEL_ACCEPTANCE = "NO"
    const val TRAINING_CONFIGURATION_REIMPLEMENTATION = 0
    const val JVM_SPECIFIC_RESULT_SEMANTICS = 0
    const val RAW_STDERR_IN_RESULT_IDENTITY = "NO"
    const val EXTERNAL_TRAINER_PROCESS_RESULT_DETERMINISTIC = "YES"
    const val PROCESS_RESULT_FILESYSTEM_IO = 0
    const val PROCESS_EXECUTION = 0
    const val PYTHON_EXECUTION = 0
    const val UV_EXECUTION = 0
    const val PYTORCH_EXECUTION = 0
    const val NETWORK_ACCESS = 0
    const val NUMERICAL_TRAINING_EXECUTION = 0
    const val PROCESS_RESULT_PERSISTENCE = 0
    const val CONTENT_TRANSPORT_HASHING = "DEFERRED_TO_RESULT_SERIALIZATION_V1"
    const val DEDICATED_TRAINING_RESULT_CONTRACT_REQUIRED = "YES"
    const val REASON_COUNT = 3
    const val DEAD_REASONS = 0

    @JvmStatic
    fun completed(
        processBinding: HimExternalTrainerProcessBindingV1,
        requestSerialization: RequestSerializationIdentity,
        diagnosticsDigest: HimSha256,
        outputArtifacts: List<OutputArtifact> = emptyList(),
        trainingOutcome: TrainingOutcome = TrainingOutcome.NO_TRAINING_RESULT,
        processExitCode: Int = 0,
    ): Result.Completed =
        create(
            processBinding = processBinding,
            requestSerialization = requestSerialization,
            processStatus = ProcessStatus.COMPLETED,
            processExitCode = processExitCode,
            trainingOutcome = trainingOutcome,
            failureReason = null,
            diagnosticsDigest = diagnosticsDigest,
            outputArtifacts = outputArtifacts,
        ) as Result.Completed

    @JvmStatic
    fun failed(
        processBinding: HimExternalTrainerProcessBindingV1,
        requestSerialization: RequestSerializationIdentity,
        diagnosticsDigest: HimSha256,
        failureReason: FailureReason,
        trainingOutcome: TrainingOutcome = TrainingOutcome.NO_TRAINING_RESULT,
        outputArtifacts: List<OutputArtifact> = emptyList(),
        processExitCode: Int = 1,
    ): Result.Failed =
        create(
            processBinding = processBinding,
            requestSerialization = requestSerialization,
            processStatus = ProcessStatus.FAILED,
            processExitCode = processExitCode,
            trainingOutcome = trainingOutcome,
            failureReason = failureReason,
            diagnosticsDigest = diagnosticsDigest,
            outputArtifacts = outputArtifacts,
        ) as Result.Failed

    @JvmStatic
    fun create(
        processBinding: HimExternalTrainerProcessBindingV1,
        requestSerialization: RequestSerializationIdentity,
        processStatus: ProcessStatus,
        processExitCode: Int,
        trainingOutcome: TrainingOutcome,
        failureReason: FailureReason?,
        diagnosticsDigest: HimSha256,
        outputArtifacts: List<OutputArtifact>,
    ): Result {
        validate(
            processBinding = processBinding,
            requestSerialization = requestSerialization,
            processStatus = processStatus,
            processExitCode = processExitCode,
            trainingOutcome = trainingOutcome,
            failureReason = failureReason,
            diagnosticsDigest = diagnosticsDigest,
            outputArtifacts = outputArtifacts,
        )
        val values = values(
            processBinding = processBinding,
            requestSerialization = requestSerialization,
            processStatus = processStatus,
            processExitCode = processExitCode,
            trainingOutcome = trainingOutcome,
            failureReason = failureReason,
            diagnosticsDigest = diagnosticsDigest,
            outputArtifacts = outputArtifacts,
        )
        return when (processStatus) {
            ProcessStatus.COMPLETED -> Result.Completed.create(values)
            ProcessStatus.FAILED -> Result.Failed.create(values)
        }
    }

    @JvmStatic
    fun requestSerializationIdentity(
        serializedRequest: HimExternalTrainerProcessRequestSerializationV1.SerializedRequest,
    ): RequestSerializationIdentity = RequestSerializationIdentity(
        reference = serializedRequest.serializationReference,
        logicalDigest = serializedRequest.contentSha256,
    )

    private fun validate(
        processBinding: HimExternalTrainerProcessBindingV1,
        requestSerialization: RequestSerializationIdentity,
        processStatus: ProcessStatus,
        processExitCode: Int,
        trainingOutcome: TrainingOutcome,
        failureReason: FailureReason?,
        diagnosticsDigest: HimSha256,
        outputArtifacts: List<OutputArtifact>,
    ) {
        require(processBinding.contractId == HimExternalTrainerProcessBindingV1.CONTRACT_ID) {
            "PROCESS_BINDING_CONTRACT_MISMATCH"
        }
        require(processBinding.version == HimExternalTrainerProcessBindingV1.VERSION) {
            "PROCESS_BINDING_VERSION_MISMATCH"
        }
        require(processBinding.state == HimExternalTrainerProcessBindingV1.STATE) {
            "PROCESS_BINDING_STATE_MISMATCH"
        }
        require(processBinding.resultProtocolContractId == CONTRACT_ID) {
            "RESULT_PROTOCOL_CONTRACT_MISMATCH"
        }
        require(processBinding.resultProtocolVersion == VERSION) {
            "RESULT_PROTOCOL_VERSION_MISMATCH"
        }
        require(processBinding.logicalDigest.value != ZERO_DIGEST) {
            "PROCESS_BINDING_REQUIRED"
        }
        require(requestSerialization.logicalDigest.value != ZERO_DIGEST) {
            "REQUEST_SERIALIZATION_DIGEST_REQUIRED"
        }
        require(diagnosticsDigest.value != ZERO_DIGEST) {
            "DIAGNOSTICS_DIGEST_REQUIRED"
        }
        require(
            (processStatus == ProcessStatus.COMPLETED) == (processExitCode == 0),
        ) {
            "PROCESS_EXIT_STATUS_MISMATCH"
        }
        when (trainingOutcome) {
            TrainingOutcome.TRAINING_COMPLETED -> {
                require(processStatus == ProcessStatus.COMPLETED) { "TRAINING_OUTCOME_STATUS_MISMATCH" }
                require(failureReason == null) { "TRAINING_COMPLETED_FAILURE_PRESENT" }
                require(outputArtifacts.count { it.role == OutputArtifactRole.FINAL_MODEL } == 1) {
                    "FINAL_MODEL_ARTIFACT_REQUIRED"
                }
            }
            TrainingOutcome.TRAINING_FAILED -> {
                require(failureReason == FailureReason.TRAINING_FAILED) {
                    "TRAINING_FAILED_REASON_MISMATCH"
                }
                require(outputArtifacts.isEmpty()) { "FAILED_RESULT_ARTIFACTS_PRESENT" }
            }
            TrainingOutcome.NO_TRAINING_RESULT -> {
                require(outputArtifacts.isEmpty()) { "NO_TRAINING_RESULT_ARTIFACTS_PRESENT" }
                if (processStatus == ProcessStatus.FAILED) {
                    require(failureReason != null) { "FAILED_RESULT_REASON_REQUIRED" }
                } else {
                    require(failureReason == null) { "NO_TRAINING_RESULT_FAILURE_PRESENT" }
                }
            }
        }
        if (processStatus == ProcessStatus.FAILED) {
            require(failureReason != null) { "FAILED_RESULT_REASON_REQUIRED" }
            require(trainingOutcome != TrainingOutcome.TRAINING_COMPLETED) {
                "FAILED_RESULT_CLAIMS_TRAINING_COMPLETED"
            }
        }
        require(outputArtifacts.map { it.role.ordinal } == outputArtifacts.map { it.role.ordinal }.sorted()) {
            "OUTPUT_ARTIFACT_ORDER_MISMATCH"
        }
        require(outputArtifacts.map { it.role }.distinct().size == outputArtifacts.size) {
            "OUTPUT_ARTIFACT_ROLE_DUPLICATE"
        }
    }

    private fun values(
        processBinding: HimExternalTrainerProcessBindingV1,
        requestSerialization: RequestSerializationIdentity,
        processStatus: ProcessStatus,
        processExitCode: Int,
        trainingOutcome: TrainingOutcome,
        failureReason: FailureReason?,
        diagnosticsDigest: HimSha256,
        outputArtifacts: List<OutputArtifact>,
    ): Values {
        val runtimeBinding = processBinding.runtimeBinding
        val executionRequest = runtimeBinding.executionRequest
        val logicalDigest = resultDigest(
            processBinding = processBinding,
            requestSerialization = requestSerialization,
            processStatus = processStatus,
            processExitCode = processExitCode,
            trainingOutcome = trainingOutcome,
            failureReason = failureReason,
            diagnosticsDigest = diagnosticsDigest,
            outputArtifacts = outputArtifacts,
        )
        return Values(
            processBinding = processBinding,
            requestSerialization = requestSerialization,
            processExitCode = processExitCode,
            trainingOutcome = trainingOutcome,
            failureReason = failureReason,
            diagnosticsDigest = diagnosticsDigest,
            outputArtifacts = outputArtifacts.toList(),
            runtimeBindingDigest = runtimeBinding.logicalDigest,
            runtimeBindingReference = runtimeBinding.bindingReference,
            runtimeEnvironmentDigest = runtimeBinding.runtimeEnvironment.logicalDigest,
            runtimeEnvironmentReference = runtimeBinding.runtimeEnvironment.environmentReference,
            executionRequestDigest = executionRequest.logicalDigest,
            executionRequestReference = executionRequest.requestReference,
            trainingMissionDigest = executionRequest.trainingMissionDigest,
            trainingMissionReference = executionRequest.trainingMissionReference,
            trainingConfigurationDigest = executionRequest.trainingConfigurationDigest,
            trainingConfigurationReference = executionRequest.trainingConfigurationReference,
            modelBindingDigest = executionRequest.modelBindingDigest,
            modelBindingReference = executionRequest.modelBindingReference,
            trainerModule = processBinding.trainerModule,
            trainerImplementationFingerprint = processBinding.trainerImplementationFingerprint,
            trainerImplementationReference = processBinding.trainerImplementationReference,
            processAdapterImplementationFingerprint = processBinding.processAdapterImplementationFingerprint,
            device = processBinding.device,
            logicalDigest = logicalDigest,
            resultReference = "$RESULT_REFERENCE_PREFIX${logicalDigest.value}",
        )
    }

    private fun resultDigest(
        processBinding: HimExternalTrainerProcessBindingV1,
        requestSerialization: RequestSerializationIdentity,
        processStatus: ProcessStatus,
        processExitCode: Int,
        trainingOutcome: TrainingOutcome,
        failureReason: FailureReason?,
        diagnosticsDigest: HimSha256,
        outputArtifacts: List<OutputArtifact>,
    ): HimSha256 {
        val runtimeBinding = processBinding.runtimeBinding
        val executionRequest = runtimeBinding.executionRequest
        val canonical = buildString {
            field("contract", CONTRACT_ID)
            field("version", VERSION)
            field("state", STATE)
            field("process-binding-digest", processBinding.logicalDigest.value)
            field("process-binding-reference", processBinding.bindingReference)
            field("request-serialization-digest", requestSerialization.logicalDigest.value)
            field("request-serialization-reference", requestSerialization.reference)
            field("runtime-binding-digest", runtimeBinding.logicalDigest.value)
            field("runtime-binding-reference", runtimeBinding.bindingReference)
            field("runtime-environment-digest", runtimeBinding.runtimeEnvironment.logicalDigest.value)
            field("runtime-environment-reference", runtimeBinding.runtimeEnvironment.environmentReference)
            field("execution-request-digest", executionRequest.logicalDigest.value)
            field("execution-request-reference", executionRequest.requestReference)
            field("training-mission-digest", executionRequest.trainingMissionDigest.value)
            field("training-mission-reference", executionRequest.trainingMissionReference)
            field("training-configuration-digest", executionRequest.trainingConfigurationDigest.value)
            field("training-configuration-reference", executionRequest.trainingConfigurationReference)
            field("model-binding-digest", executionRequest.modelBindingDigest.value)
            field("model-binding-reference", executionRequest.modelBindingReference)
            field("trainer-module", processBinding.trainerModule)
            field("trainer-fingerprint", processBinding.trainerImplementationFingerprint.value)
            field("trainer-reference", processBinding.trainerImplementationReference)
            field("adapter-fingerprint", processBinding.processAdapterImplementationFingerprint.value)
            field("device", processBinding.device.name)
            field("process-status", processStatus.name)
            field("process-exit-code", processExitCode.toString())
            field("training-outcome", trainingOutcome.name)
            field("failure-reason", failureReason?.name ?: "NONE")
            field("diagnostics-digest", diagnosticsDigest.value)
            field("artifact-count", outputArtifacts.size.toString())
            outputArtifacts.forEachIndexed { index, artifact ->
                field("artifact-$index-role", artifact.role.name)
                field("artifact-$index-reference", artifact.reference)
                field("artifact-$index-sha256", artifact.sha256.value)
                field("artifact-$index-byte-size", artifact.byteSize?.toString() ?: "NONE")
            }
        }
        return HimSha256(
            MessageDigest.getInstance("SHA-256")
                .digest(canonical.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )
    }

    private fun StringBuilder.field(key: String, value: String) {
        append(key).append('=').append(value.length).append(':').append(value).append('\n')
    }
}
