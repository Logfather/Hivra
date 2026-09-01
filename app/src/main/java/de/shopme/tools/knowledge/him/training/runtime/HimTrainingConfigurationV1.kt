package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.math.BigDecimal
import java.security.MessageDigest

private val OPTIMIZER_ID_PATTERN = Regex("[a-z][a-z0-9]*(?::[a-z][a-z0-9]*)*")
private val CONFIGURATION_REFERENCE_PATTERN = Regex("training-configuration:v1:[0-9a-f]{64}")

/**
 * The deterministic numerical configuration boundary for Training V1.
 * It describes no dataset, model, implementation, persistence, or execution.
 */
class HimTrainingConfigurationV1 private constructor(
    val seed: Long,
    val epochs: Int,
    val microBatchSize: Int,
    val gradientAccumulationSteps: Int,
    val learningRate: BigDecimal,
    val optimizerId: String,
    val logicalDigest: HimSha256,
    val configurationReference: String,
) {
    val effectiveBatchSize: Long
        get() = Math.multiplyExact(microBatchSize.toLong(), gradientAccumulationSteps.toLong())

    init {
        require(epochs > 0) { "Epochs must be positive." }
        require(microBatchSize > 0) { "Micro-batch size must be positive." }
        require(gradientAccumulationSteps > 0) { "Gradient accumulation steps must be positive." }
        require(learningRate.signum() > 0) { "Learning rate must be positive." }
        require(learningRate == learningRate.stripTrailingZeros()) {
            "Learning rate must use its canonical representation."
        }
        require(optimizerId.matches(OPTIMIZER_ID_PATTERN)) { "Optimizer identifier is invalid." }
        require(CONFIGURATION_REFERENCE_PATTERN.matches(configurationReference)) {
            "Configuration reference is invalid."
        }
        require(configurationReference == "training-configuration:v1:${logicalDigest.value}") {
            "Configuration reference is not bound to the logical digest."
        }
    }

    override fun equals(other: Any?): Boolean =
        other is HimTrainingConfigurationV1 && logicalDigest == other.logicalDigest

    override fun hashCode(): Int = logicalDigest.hashCode()

    companion object {
        const val CONTRACT_ID = "HIM_TRAINING_CONFIGURATION_V1"
        const val VERSION = "1"
        const val STATE = "TRAINING_CONFIGURATION_VALIDATED"

        fun create(
            seed: Long,
            epochs: Int,
            microBatchSize: Int,
            gradientAccumulationSteps: Int,
            learningRate: BigDecimal,
            optimizerId: String,
        ): HimTrainingConfigurationV1 {
            require(epochs > 0) { "Epochs must be positive." }
            require(microBatchSize > 0) { "Micro-batch size must be positive." }
            require(gradientAccumulationSteps > 0) { "Gradient accumulation steps must be positive." }
            require(learningRate.signum() > 0) { "Learning rate must be positive." }
            require(optimizerId.matches(OPTIMIZER_ID_PATTERN)) { "Optimizer identifier is invalid." }

            val canonicalLearningRate = learningRate.stripTrailingZeros()
            val logicalDigest = digest(
                seed = seed,
                epochs = epochs,
                microBatchSize = microBatchSize,
                gradientAccumulationSteps = gradientAccumulationSteps,
                learningRate = canonicalLearningRate,
                optimizerId = optimizerId,
            )
            return HimTrainingConfigurationV1(
                seed = seed,
                epochs = epochs,
                microBatchSize = microBatchSize,
                gradientAccumulationSteps = gradientAccumulationSteps,
                learningRate = canonicalLearningRate,
                optimizerId = optimizerId,
                logicalDigest = logicalDigest,
                configurationReference = "training-configuration:v1:${logicalDigest.value}",
            )
        }

        private fun digest(
            seed: Long,
            epochs: Int,
            microBatchSize: Int,
            gradientAccumulationSteps: Int,
            learningRate: BigDecimal,
            optimizerId: String,
        ): HimSha256 {
            val canonical = buildString {
                field("contract", CONTRACT_ID)
                field("version", VERSION)
                field("state", STATE)
                field("seed", seed.toString())
                field("epochs", epochs.toString())
                field("micro-batch-size", microBatchSize.toString())
                field("gradient-accumulation-steps", gradientAccumulationSteps.toString())
                field("learning-rate", learningRate.toPlainString())
                field("optimizer-id", optimizerId)
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
