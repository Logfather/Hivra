package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.security.MessageDigest

private const val EXECUTION_REQUEST_REFERENCE_PREFIX = "external-trainer-execution-request:v1:"

/**
 * Framework-neutral authorization package for a future external trainer.
 * It composes only already validated upstream bindings and performs no execution.
 */
class HimExternalTrainerExecutionRequestV1 private constructor(
    val protocolAdamWBinding: HimTrainerProtocolAdamWRuntimeBindingV1,
    val verifiedModelArtifacts: HimLocalModelArtifactResolverV1.Resolution,
    val logicalDigest: HimSha256,
    val requestReference: String,
) {
    val contractId: String
        get() = CONTRACT_ID

    val version: String
        get() = VERSION

    val state: String
        get() = STATE

    val protocolRequest: HimTrainerProtocolV1.Request
        get() = protocolAdamWBinding.protocolRequest

    val trainingMissionDigest: HimSha256
        get() = protocolAdamWBinding.trainingMissionDigest

    val trainingMissionReference: String
        get() = protocolAdamWBinding.trainingMissionReference

    val trainingConfigurationDigest: HimSha256
        get() = protocolAdamWBinding.trainingConfigurationDigest

    val trainingConfigurationReference: String
        get() = protocolAdamWBinding.trainingConfigurationReference

    val modelBindingDigest: HimSha256
        get() = protocolAdamWBinding.protocolRequest.modelBinding.logicalDigest

    val modelBindingReference: String
        get() = protocolAdamWBinding.protocolRequest.modelBinding.modelBindingReference

    val trainerProtocolPartition: String
        get() = HimTrainerProtocolV1.TRAINER_PROTOCOL_PARTITION

    val optimizerId: String
        get() = protocolAdamWBinding.optimizerId

    val algorithm: HimOptimizerMappingV1.AlgorithmKind
        get() = protocolAdamWBinding.algorithm

    val learningRate = protocolAdamWBinding.learningRate
    val beta1 = protocolAdamWBinding.beta1
    val beta2 = protocolAdamWBinding.beta2
    val epsilon = protocolAdamWBinding.epsilon
    val weightDecay = protocolAdamWBinding.weightDecay
    val decoupledWeightDecay: Boolean
        get() = protocolAdamWBinding.decoupledWeightDecay
    val biasCorrection: Boolean
        get() = protocolAdamWBinding.biasCorrection

    init {
        require(contractId == CONTRACT_ID)
        require(version == VERSION)
        require(state == STATE)
        require(protocolAdamWBinding.protocolRequest.requestReference ==
            "trainer-request:v1:${protocolAdamWBinding.protocolRequest.logicalDigest.value}") {
            "PROTOCOL_REQUEST_REFERENCE_MISMATCH"
        }
        require(protocolAdamWBinding.protocolRequest.records.isNotEmpty()) { "TRAIN_PROTOCOL_INPUT_EMPTY" }
        require(protocolAdamWBinding.protocolRequest.records.all { it.partition == TRAINER_PROTOCOL_PARTITION }) {
            "TRAINER_PROTOCOL_PARTITION_MISMATCH"
        }
        require(protocolAdamWBinding.adamwRuntimeBinding.profileCompleteness ==
            HimAdamWRuntimeBindingV1.ProfileCompleteness.COMPLETE) {
            "INCOMPLETE_ADAMW_RUNTIME_BINDING"
        }
        require(protocolAdamWBinding.adamwRuntimeBinding.runtimeReady) {
            "ADAMW_RUNTIME_BINDING_NOT_READY"
        }
        require(protocolAdamWBinding.trainingMissionDigest ==
            protocolAdamWBinding.protocolRequest.trainingMissionDigest) {
            "MISSION_IDENTITY_MISMATCH"
        }
        require(protocolAdamWBinding.trainingMissionReference ==
            protocolAdamWBinding.protocolRequest.trainingMissionReference) {
            "MISSION_IDENTITY_MISMATCH"
        }
        require(protocolAdamWBinding.trainingConfigurationDigest ==
            protocolAdamWBinding.protocolRequest.configuration.logicalDigest) {
            "CONFIGURATION_IDENTITY_MISMATCH"
        }
        require(protocolAdamWBinding.trainingConfigurationReference ==
            protocolAdamWBinding.protocolRequest.configuration.configurationReference) {
            "CONFIGURATION_IDENTITY_MISMATCH"
        }
        require(protocolAdamWBinding.protocolRequest.modelBinding.logicalDigest ==
            verifiedModelArtifacts.modelBinding.logicalDigest) {
            "MODEL_BINDING_IDENTITY_MISMATCH"
        }
        require(protocolAdamWBinding.protocolRequest.modelBinding.modelBindingReference ==
            verifiedModelArtifacts.modelBinding.modelBindingReference) {
            "MODEL_BINDING_IDENTITY_MISMATCH"
        }
        require(verifiedModelArtifacts.baseModel.role == HimLocalModelArtifactResolverV1.ArtifactRole.BASE_MODEL)
        require(verifiedModelArtifacts.tokenizer.role == HimLocalModelArtifactResolverV1.ArtifactRole.TOKENIZER)
        require(verifiedModelArtifacts.modelConfiguration.role ==
            HimLocalModelArtifactResolverV1.ArtifactRole.MODEL_CONFIGURATION)
        require(verifiedModelArtifacts.baseModel.expectedDigest ==
            verifiedModelArtifacts.baseModel.actualDigest)
        require(verifiedModelArtifacts.tokenizer.expectedDigest ==
            verifiedModelArtifacts.tokenizer.actualDigest)
        require(verifiedModelArtifacts.modelConfiguration.expectedDigest ==
            verifiedModelArtifacts.modelConfiguration.actualDigest)
        require(requestReference == "$EXECUTION_REQUEST_REFERENCE_PREFIX${logicalDigest.value}") {
            "EXECUTION_REQUEST_REFERENCE_MISMATCH"
        }
        require(logicalDigest == requestDigest(protocolAdamWBinding, verifiedModelArtifacts)) {
            "EXECUTION_REQUEST_DIGEST_MISMATCH"
        }
    }

    override fun equals(other: Any?): Boolean =
        other is HimExternalTrainerExecutionRequestV1 && logicalDigest == other.logicalDigest

    override fun hashCode(): Int = logicalDigest.hashCode()

    companion object {
        const val CONTRACT_ID = "HIM_EXTERNAL_TRAINER_EXECUTION_REQUEST_V1"
        const val VERSION = "1"
        const val STATE = "EXTERNAL_TRAINER_EXECUTION_REQUEST_AUTHORIZED"
        const val EXECUTION_REQUEST_REQUIRES_PROTOCOL_ADAMW_BINDING = "YES"
        const val EXECUTION_REQUEST_REQUIRES_VERIFIED_MODEL_ARTIFACTS = "YES"
        const val ALTERNATE_EXECUTION_AUTHORIZATION_PATHS = 0
        const val MODEL_BINDING_IDENTITY_MATCH_ENFORCED = "YES"
        const val TRAINING_MISSION_IDENTITY_PRESERVED = "YES"
        const val TRAINING_CONFIGURATION_IDENTITY_PRESERVED = "YES"
        const val TRAINER_PROTOCOL_PARTITION = "TRAIN_ONLY"
        const val TRAIN_RECORDS_REPROJECTED = 0
        const val TRAIN_RECORD_ORDER_CHANGED = 0
        const val MANY_TO_ONE_COLLAPSE = 0
        const val OBJECTIVE_REIMPLEMENTATION = 0
        const val TARGET_ENCODING_REIMPLEMENTATION = 0
        const val COMPLETE_OPTIMIZER_RUNTIME_SPEC_PRESERVED = "YES"
        const val HIDDEN_FRAMEWORK_DEFAULTS = 0
        const val UNVERIFIED_RUNTIME_ARTIFACT_PATHS_ACCEPTED = 0
        const val EXECUTION_REQUEST_ARTIFACT_REHASH = 0
        const val ABSOLUTE_PATH_IN_EXECUTION_REQUEST_LOGICAL_DIGEST = "NO"
        const val FRAMEWORK_BINDING = "NONE"
        const val PROCESS_EXECUTION = 0
        const val TOKENIZATION_EXECUTION = 0
        const val TENSORIZATION = 0
        const val MODEL_LOAD_EXECUTION = 0
        const val NUMERICAL_TRAINING_EXECUTION = 0
        const val EXECUTION_REQUEST_PERSISTENCE = 0
        const val FROZEN_UPSTREAM_CONTRACT_MODIFICATIONS = 0

        @JvmStatic
        fun create(
            protocolAdamWBinding: HimTrainerProtocolAdamWRuntimeBindingV1,
            verifiedModelArtifacts: HimLocalModelArtifactResolverV1.Resolution,
        ): HimExternalTrainerExecutionRequestV1 {
            require(protocolAdamWBinding.protocolRequest.modelBinding.logicalDigest ==
                verifiedModelArtifacts.modelBinding.logicalDigest) {
                "MODEL_BINDING_IDENTITY_MISMATCH"
            }
            require(protocolAdamWBinding.protocolRequest.modelBinding.modelBindingReference ==
                verifiedModelArtifacts.modelBinding.modelBindingReference) {
                "MODEL_BINDING_IDENTITY_MISMATCH"
            }
            val digest = requestDigest(protocolAdamWBinding, verifiedModelArtifacts)
            return HimExternalTrainerExecutionRequestV1(
                protocolAdamWBinding = protocolAdamWBinding,
                verifiedModelArtifacts = verifiedModelArtifacts,
                logicalDigest = digest,
                requestReference = "$EXECUTION_REQUEST_REFERENCE_PREFIX${digest.value}",
            )
        }

        private fun requestDigest(
            protocolAdamWBinding: HimTrainerProtocolAdamWRuntimeBindingV1,
            verifiedModelArtifacts: HimLocalModelArtifactResolverV1.Resolution,
        ): HimSha256 {
            val canonical = buildString {
                field("contract", CONTRACT_ID)
                field("version", VERSION)
                field("state", STATE)
                field("protocol-adamw-binding", protocolAdamWBinding.logicalDigest.value)
                field("protocol-adamw-reference", protocolAdamWBinding.bindingReference)
                field("artifact-resolution", verifiedModelArtifacts.logicalDigest.value)
                field("artifact-resolution-reference", verifiedModelArtifacts.resolutionReference)
                field("mission", protocolAdamWBinding.trainingMissionDigest.value)
                field("mission-reference", protocolAdamWBinding.trainingMissionReference)
                field("configuration", protocolAdamWBinding.trainingConfigurationDigest.value)
                field("configuration-reference", protocolAdamWBinding.trainingConfigurationReference)
                field("model-binding", protocolAdamWBinding.protocolRequest.modelBinding.logicalDigest.value)
                field("model-binding-reference", protocolAdamWBinding.protocolRequest.modelBinding.modelBindingReference)
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
}
