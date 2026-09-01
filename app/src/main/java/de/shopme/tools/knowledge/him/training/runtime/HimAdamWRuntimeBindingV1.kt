package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.math.BigDecimal
import java.security.MessageDigest

private val RUNTIME_BINDING_REFERENCE_PATTERN = Regex("adamw-runtime-binding:v1:[0-9a-f]{64}")
private val ZERO = BigDecimal.ZERO
private val ONE = BigDecimal.ONE

/**
 * The complete, immutable and framework-independent AdamW semantic binding for one mission.
 * It specifies values for a future runtime but never executes an optimizer.
 */
class HimAdamWRuntimeBindingV1 private constructor(
    val trainingMissionDigest: HimSha256,
    val trainingMissionReference: String,
    val trainingConfigurationDigest: HimSha256,
    val trainingConfigurationReference: String,
    val optimizerMappingDigest: HimSha256,
    val optimizerMappingReference: String,
    val optimizerId: String,
    val algorithmKind: HimOptimizerMappingV1.AlgorithmKind,
    val adamwParametersDigest: HimSha256,
    val adamwParametersReference: String,
    val learningRate: BigDecimal,
    val beta1: BigDecimal,
    val beta2: BigDecimal,
    val epsilon: BigDecimal,
    val weightDecay: BigDecimal,
    val logicalDigest: HimSha256,
    val runtimeBindingReference: String,
) {
    val contractId: String
        get() = CONTRACT_ID

    val version: String
        get() = VERSION

    val state: String
        get() = STATE

    val algorithm: HimOptimizerMappingV1.AlgorithmKind
        get() = algorithmKind

    val decoupledWeightDecay: Boolean
        get() = DECOUPLED_WEIGHT_DECAY

    val biasCorrection: Boolean
        get() = BIAS_CORRECTION

    val profileCompleteness: ProfileCompleteness
        get() = ProfileCompleteness.COMPLETE

    val runtimeReady: Boolean
        get() = true

    val numericallyRelevantUnboundOptimizerSemantics: List<String>
        get() = emptyList()

    init {
        require(optimizerId == BOUND_OPTIMIZER_ID)
        require(algorithmKind == HimOptimizerMappingV1.AlgorithmKind.ADAMW)
        require(decoupledWeightDecay)
        require(biasCorrection)
        require(profileCompleteness == ProfileCompleteness.COMPLETE)
        require(runtimeReady)
        require(numericallyRelevantUnboundOptimizerSemantics.isEmpty())
        require(learningRate == learningRate.stripTrailingZeros())
        require(beta1.compareTo(ZERO) >= 0 && beta1.compareTo(ONE) < 0)
        require(beta2.compareTo(ZERO) >= 0 && beta2.compareTo(ONE) < 0)
        require(epsilon.compareTo(ZERO) > 0)
        require(weightDecay.compareTo(ZERO) >= 0)
        require(RUNTIME_BINDING_REFERENCE_PATTERN.matches(runtimeBindingReference)) {
            "AdamW-runtime-binding reference is invalid."
        }
        require(runtimeBindingReference == "$RUNTIME_BINDING_REFERENCE_PREFIX${logicalDigest.value}") {
            "AdamW-runtime-binding reference is not bound to the logical digest."
        }
    }

    override fun equals(other: Any?): Boolean =
        other is HimAdamWRuntimeBindingV1 && logicalDigest == other.logicalDigest

    override fun hashCode(): Int = logicalDigest.hashCode()

    enum class ProfileCompleteness {
        COMPLETE,
    }

    companion object {
        const val CONTRACT_ID = "HIM_ADAMW_RUNTIME_BINDING_V1"
        const val VERSION = "1"
        const val STATE = "ADAMW_RUNTIME_BINDING_VALIDATED"
        const val BOUND_OPTIMIZER_ID = "optimizer:adamw:v1"
        const val DECOUPLED_WEIGHT_DECAY = true
        const val BIAS_CORRECTION = true
        const val ADAMW_RUNTIME_BINDING_REQUIRES_TRAINING_MISSION = "YES"

        private const val RUNTIME_BINDING_REFERENCE_PREFIX = "adamw-runtime-binding:v1:"

        @JvmStatic
        fun create(
            mission: HimTrainingMissionV1.Mission,
            configuration: HimTrainingConfigurationV1,
            mapping: HimOptimizerMappingV1,
            parameters: HimAdamWParametersV1,
        ): HimAdamWRuntimeBindingV1 {
            require(configuration.logicalDigest == mission.trainingConfiguration.digest) {
                "MISSION_CONFIGURATION_BINDING_MISMATCH"
            }
            require(configuration.configurationReference == "training-configuration:v1:${configuration.logicalDigest.value}") {
                "TRAINING_CONFIGURATION_REFERENCE_MISMATCH"
            }
            val resolved = mapping.resolve(configuration)
            require(resolved.optimizerId == BOUND_OPTIMIZER_ID) {
                "BOUND_OPTIMIZER_ID_MISMATCH"
            }
            require(resolved.algorithmKind == HimOptimizerMappingV1.AlgorithmKind.ADAMW) {
                "OPTIMIZER_ALGORITHM_MISMATCH"
            }
            require(mapping.profileCompleteness == HimOptimizerMappingV1.ProfileCompleteness.INCOMPLETE) {
                "OPTIMIZER_MAPPING_PROFILE_MISMATCH"
            }
            require(parameters.parametersReference == "adamw-parameters:v1:${parameters.logicalDigest.value}") {
                "ADAMW_PARAMETERS_REFERENCE_MISMATCH"
            }
            require(configuration.learningRate == configuration.learningRate.stripTrailingZeros()) {
                "LEARNING_RATE_NOT_CANONICAL"
            }
            val digest = digest(
                mission = mission,
                configuration = configuration,
                mapping = mapping,
                resolved = resolved,
                parameters = parameters,
            )
            return HimAdamWRuntimeBindingV1(
                trainingMissionDigest = mission.logicalDigest,
                trainingMissionReference = mission.missionReference,
                trainingConfigurationDigest = configuration.logicalDigest,
                trainingConfigurationReference = configuration.configurationReference,
                optimizerMappingDigest = mapping.logicalDigest,
                optimizerMappingReference = mapping.mappingReference,
                optimizerId = resolved.optimizerId,
                algorithmKind = resolved.algorithmKind,
                adamwParametersDigest = parameters.logicalDigest,
                adamwParametersReference = parameters.parametersReference,
                learningRate = configuration.learningRate,
                beta1 = parameters.beta1,
                beta2 = parameters.beta2,
                epsilon = parameters.epsilon,
                weightDecay = parameters.weightDecay,
                logicalDigest = digest,
                runtimeBindingReference = "$RUNTIME_BINDING_REFERENCE_PREFIX${digest.value}",
            )
        }

        private fun digest(
            mission: HimTrainingMissionV1.Mission,
            configuration: HimTrainingConfigurationV1,
            mapping: HimOptimizerMappingV1,
            resolved: HimOptimizerMappingV1.Resolved,
            parameters: HimAdamWParametersV1,
        ): HimSha256 = HimSha256(
            MessageDigest.getInstance("SHA-256")
                .digest(
                    buildString {
                        field("contract", CONTRACT_ID)
                        field("version", VERSION)
                        field("state", STATE)
                        field("mission-digest", mission.logicalDigest.value)
                        field("mission-reference", mission.missionReference)
                        field("configuration-digest", configuration.logicalDigest.value)
                        field("configuration-reference", configuration.configurationReference)
                        field("mapping-digest", mapping.logicalDigest.value)
                        field("mapping-reference", mapping.mappingReference)
                        field("optimizer-id", resolved.optimizerId)
                        field("algorithm", resolved.algorithmKind.name)
                        field("parameters-digest", parameters.logicalDigest.value)
                        field("parameters-reference", parameters.parametersReference)
                        field("learning-rate", configuration.learningRate.toPlainString())
                        field("beta1", parameters.beta1.toPlainString())
                        field("beta2", parameters.beta2.toPlainString())
                        field("epsilon", parameters.epsilon.toPlainString())
                        field("weight-decay", parameters.weightDecay.toPlainString())
                        field("decoupled-weight-decay", DECOUPLED_WEIGHT_DECAY.toString())
                        field("bias-correction", BIAS_CORRECTION.toString())
                    }.toByteArray(Charsets.UTF_8),
                )
                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )

        private fun StringBuilder.field(key: String, value: String) {
            append(key).append('=').append(value.length).append(':').append(value).append('\n')
        }
    }
}
