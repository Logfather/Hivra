package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.math.BigDecimal
import java.security.MessageDigest

private val ZERO = BigDecimal.ZERO
private val ONE = BigDecimal.ONE
private val PARAMETERS_REFERENCE_PATTERN = Regex("adamw-parameters:v1:[0-9a-f]{64}")

private fun canonicalDecimal(value: BigDecimal): BigDecimal = value.stripTrailingZeros()

/**
 * The immutable, framework-independent run-level AdamW parameter boundary.
 * It carries values only; it does not implement or execute an optimizer.
 */
class HimAdamWParametersV1 private constructor(
    val beta1: BigDecimal,
    val beta2: BigDecimal,
    val epsilon: BigDecimal,
    val weightDecay: BigDecimal,
    val logicalDigest: HimSha256,
    val parametersReference: String,
) {
    init {
        require(beta1 == canonicalDecimal(beta1)) { "BETA1_NOT_CANONICAL" }
        require(beta2 == canonicalDecimal(beta2)) { "BETA2_NOT_CANONICAL" }
        require(epsilon == canonicalDecimal(epsilon)) { "EPSILON_NOT_CANONICAL" }
        require(weightDecay == canonicalDecimal(weightDecay)) { "WEIGHT_DECAY_NOT_CANONICAL" }
        require(beta1.compareTo(ZERO) >= 0 && beta1.compareTo(ONE) < 0) { "BETA1_OUT_OF_RANGE" }
        require(beta2.compareTo(ZERO) >= 0 && beta2.compareTo(ONE) < 0) { "BETA2_OUT_OF_RANGE" }
        require(epsilon.compareTo(ZERO) > 0) { "EPSILON_OUT_OF_RANGE" }
        require(weightDecay.compareTo(ZERO) >= 0) { "WEIGHT_DECAY_OUT_OF_RANGE" }
        require(PARAMETERS_REFERENCE_PATTERN.matches(parametersReference)) {
            "AdamW-parameters reference is invalid."
        }
        require(parametersReference == "$PARAMETERS_REFERENCE_PREFIX${logicalDigest.value}") {
            "AdamW-parameters reference is not bound to the logical digest."
        }
    }

    override fun equals(other: Any?): Boolean =
        other is HimAdamWParametersV1 && logicalDigest == other.logicalDigest

    override fun hashCode(): Int = logicalDigest.hashCode()

    companion object {
        const val CONTRACT_ID = "HIM_ADAMW_PARAMETERS_V1"
        const val VERSION = "1"
        const val STATE = "ADAMW_PARAMETERS_VALIDATED"
        const val DECOUPLED_WEIGHT_DECAY = true
        const val BIAS_CORRECTION = true

        private const val PARAMETERS_REFERENCE_PREFIX = "adamw-parameters:v1:"

        fun create(
            beta1: BigDecimal,
            beta2: BigDecimal,
            epsilon: BigDecimal,
            weightDecay: BigDecimal,
        ): HimAdamWParametersV1 {
            val canonicalBeta1 = canonicalDecimal(beta1)
            val canonicalBeta2 = canonicalDecimal(beta2)
            val canonicalEpsilon = canonicalDecimal(epsilon)
            val canonicalWeightDecay = canonicalDecimal(weightDecay)
            require(canonicalBeta1.compareTo(ZERO) >= 0 && canonicalBeta1.compareTo(ONE) < 0) {
                "BETA1_OUT_OF_RANGE"
            }
            require(canonicalBeta2.compareTo(ZERO) >= 0 && canonicalBeta2.compareTo(ONE) < 0) {
                "BETA2_OUT_OF_RANGE"
            }
            require(canonicalEpsilon.compareTo(ZERO) > 0) { "EPSILON_OUT_OF_RANGE" }
            require(canonicalWeightDecay.compareTo(ZERO) >= 0) { "WEIGHT_DECAY_OUT_OF_RANGE" }
            val digest = digest(
                beta1 = canonicalBeta1,
                beta2 = canonicalBeta2,
                epsilon = canonicalEpsilon,
                weightDecay = canonicalWeightDecay,
            )
            return HimAdamWParametersV1(
                beta1 = canonicalBeta1,
                beta2 = canonicalBeta2,
                epsilon = canonicalEpsilon,
                weightDecay = canonicalWeightDecay,
                logicalDigest = digest,
                parametersReference = "$PARAMETERS_REFERENCE_PREFIX${digest.value}",
            )
        }

        private fun digest(
            beta1: BigDecimal,
            beta2: BigDecimal,
            epsilon: BigDecimal,
            weightDecay: BigDecimal,
        ): HimSha256 = HimSha256(
            MessageDigest.getInstance("SHA-256")
                .digest(
                    buildString {
                        field("contract", CONTRACT_ID)
                        field("version", VERSION)
                        field("state", STATE)
                        field("beta1", beta1.toPlainString())
                        field("beta2", beta2.toPlainString())
                        field("epsilon", epsilon.toPlainString())
                        field("weight-decay", weightDecay.toPlainString())
                    }.toByteArray(Charsets.UTF_8),
                )
                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )

        private fun StringBuilder.field(key: String, value: String) {
            append(key).append('=').append(value.length).append(':').append(value).append('\n')
        }
    }
}
