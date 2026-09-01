package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.security.MessageDigest

private val BINDING_REFERENCE_PATTERN = Regex("trainer-protocol-adamw-runtime-binding:v1:[0-9a-f]{64}")

/**
 * Additive proof that one already validated Trainer Protocol request and one
 * complete AdamW runtime binding belong to the same authorized training run.
 * This type does not rebuild protocol records or execute a trainer.
 */
class HimTrainerProtocolAdamWRuntimeBindingV1 private constructor(
    val protocolRequest: HimTrainerProtocolV1.Request,
    val adamwRuntimeBinding: HimAdamWRuntimeBindingV1,
    val logicalDigest: HimSha256,
    val bindingReference: String,
) {
    val contractId: String
        get() = CONTRACT_ID

    val version: String
        get() = VERSION

    val state: String
        get() = STATE

    val protocolRequestDigest: HimSha256
        get() = protocolRequest.logicalDigest

    val protocolRequestReference: String
        get() = protocolRequest.requestReference

    val adamwRuntimeBindingDigest: HimSha256
        get() = adamwRuntimeBinding.logicalDigest

    val adamwRuntimeBindingReference: String
        get() = adamwRuntimeBinding.runtimeBindingReference

    val trainingMissionDigest: HimSha256
        get() = adamwRuntimeBinding.trainingMissionDigest

    val trainingMissionReference: String
        get() = adamwRuntimeBinding.trainingMissionReference

    val trainingConfigurationDigest: HimSha256
        get() = adamwRuntimeBinding.trainingConfigurationDigest

    val trainingConfigurationReference: String
        get() = adamwRuntimeBinding.trainingConfigurationReference

    val implementationFingerprint: HimSha256
        get() = protocolRequest.implementationFingerprint

    val optimizerId: String
        get() = adamwRuntimeBinding.optimizerId

    val algorithm: HimOptimizerMappingV1.AlgorithmKind
        get() = adamwRuntimeBinding.algorithm

    val learningRate = adamwRuntimeBinding.learningRate
    val beta1 = adamwRuntimeBinding.beta1
    val beta2 = adamwRuntimeBinding.beta2
    val epsilon = adamwRuntimeBinding.epsilon
    val weightDecay = adamwRuntimeBinding.weightDecay
    val decoupledWeightDecay = adamwRuntimeBinding.decoupledWeightDecay
    val biasCorrection = adamwRuntimeBinding.biasCorrection

    init {
        require(contractId == CONTRACT_ID)
        require(version == VERSION)
        require(state == STATE)
        require(protocolRequest.requestReference == "trainer-request:v1:${protocolRequest.logicalDigest.value}") {
            "PROTOCOL_REQUEST_REFERENCE_MISMATCH"
        }
        require(protocolRequest.trainingMissionDigest == adamwRuntimeBinding.trainingMissionDigest) {
            "MISSION_IDENTITY_MISMATCH"
        }
        require(protocolRequest.trainingMissionReference == adamwRuntimeBinding.trainingMissionReference) {
            "MISSION_IDENTITY_MISMATCH"
        }
        require(protocolRequest.configuration.logicalDigest == adamwRuntimeBinding.trainingConfigurationDigest) {
            "CONFIGURATION_IDENTITY_MISMATCH"
        }
        require(protocolRequest.configuration.configurationReference == adamwRuntimeBinding.trainingConfigurationReference) {
            "CONFIGURATION_IDENTITY_MISMATCH"
        }
        require(protocolRequest.configuration.optimizerId == adamwRuntimeBinding.optimizerId) {
            "OPTIMIZER_ID_MISMATCH"
        }
        require(protocolRequest.configuration.learningRate == adamwRuntimeBinding.learningRate.toPlainString()) {
            "LEARNING_RATE_MISMATCH"
        }
        require(adamwRuntimeBinding.profileCompleteness == HimAdamWRuntimeBindingV1.ProfileCompleteness.COMPLETE) {
            "INCOMPLETE_ADAMW_RUNTIME_BINDING"
        }
        require(adamwRuntimeBinding.runtimeReady) {
            "ADAMW_RUNTIME_BINDING_NOT_READY"
        }
        require(adamwRuntimeBinding.numericallyRelevantUnboundOptimizerSemantics.isEmpty()) {
            "UNBOUND_ADAMW_RUNTIME_SEMANTICS"
        }
        require(BINDING_REFERENCE_PATTERN.matches(bindingReference)) {
            "Binding reference is invalid."
        }
        require(bindingReference == "$BINDING_REFERENCE_PREFIX${logicalDigest.value}") {
            "Binding reference is not bound to the logical digest."
        }
    }

    override fun equals(other: Any?): Boolean =
        other is HimTrainerProtocolAdamWRuntimeBindingV1 && logicalDigest == other.logicalDigest

    override fun hashCode(): Int = logicalDigest.hashCode()

    companion object {
        const val CONTRACT_ID = "HIM_TRAINER_PROTOCOL_ADAMW_RUNTIME_BINDING_V1"
        const val VERSION = "1"
        const val STATE = "TRAINER_PROTOCOL_ADAMW_RUNTIME_BOUND"
        const val ADAMW_PROTOCOL_BINDING_REQUIRES_TRAINER_PROTOCOL_REQUEST = "YES"

        private const val BINDING_REFERENCE_PREFIX = "trainer-protocol-adamw-runtime-binding:v1:"

        @JvmStatic
        fun create(
            protocolRequest: HimTrainerProtocolV1.Request,
            adamwRuntimeBinding: HimAdamWRuntimeBindingV1,
        ): HimTrainerProtocolAdamWRuntimeBindingV1 {
            val digest = digest(protocolRequest, adamwRuntimeBinding)
            return HimTrainerProtocolAdamWRuntimeBindingV1(
                protocolRequest = protocolRequest,
                adamwRuntimeBinding = adamwRuntimeBinding,
                logicalDigest = digest,
                bindingReference = "$BINDING_REFERENCE_PREFIX${digest.value}",
            )
        }

        private fun digest(
            protocolRequest: HimTrainerProtocolV1.Request,
            adamwRuntimeBinding: HimAdamWRuntimeBindingV1,
        ): HimSha256 = HimSha256(
            MessageDigest.getInstance("SHA-256")
                .digest(
                    buildString {
                        field("contract", CONTRACT_ID)
                        field("version", VERSION)
                        field("state", STATE)
                        field("protocol-request-digest", protocolRequest.logicalDigest.value)
                        field("protocol-request-reference", protocolRequest.requestReference)
                        field("runtime-binding-digest", adamwRuntimeBinding.logicalDigest.value)
                        field("runtime-binding-reference", adamwRuntimeBinding.runtimeBindingReference)
                        field("mission-digest", adamwRuntimeBinding.trainingMissionDigest.value)
                        field("mission-reference", adamwRuntimeBinding.trainingMissionReference)
                        field("configuration-digest", adamwRuntimeBinding.trainingConfigurationDigest.value)
                        field("configuration-reference", adamwRuntimeBinding.trainingConfigurationReference)
                        field("implementation-fingerprint", protocolRequest.implementationFingerprint.value)
                        field("optimizer-id", adamwRuntimeBinding.optimizerId)
                        field("algorithm", adamwRuntimeBinding.algorithm.name)
                        field("learning-rate", adamwRuntimeBinding.learningRate.toPlainString())
                        field("beta1", adamwRuntimeBinding.beta1.toPlainString())
                        field("beta2", adamwRuntimeBinding.beta2.toPlainString())
                        field("epsilon", adamwRuntimeBinding.epsilon.toPlainString())
                        field("weight-decay", adamwRuntimeBinding.weightDecay.toPlainString())
                        field("decoupled-weight-decay", adamwRuntimeBinding.decoupledWeightDecay.toString())
                        field("bias-correction", adamwRuntimeBinding.biasCorrection.toString())
                    }.toByteArray(Charsets.UTF_8),
                )
                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )

        private fun StringBuilder.field(key: String, value: String) {
            append(key).append('=').append(value.length).append(':').append(value).append('\n')
        }
    }
}
