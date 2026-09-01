package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.security.MessageDigest

private val MODEL_BINDING_REFERENCE_PATTERN = Regex("model-binding:v1:[0-9a-f]{64}")

private fun requireStableIdentifier(name: String, value: String) {
    require(value.isNotBlank()) { "$name must not be blank." }
    require(value.none { it.isWhitespace() || it.isISOControl() }) {
        "$name must not contain whitespace or control characters."
    }
}

/**
 * The immutable identity of the model input before training updates.
 * It binds model and tokenizer artifacts without selecting a framework or runtime.
 */
class HimModelBindingV1 private constructor(
    val modelFamilyId: String,
    val baseModelId: String,
    val baseModelArtifactDigest: HimSha256,
    val tokenizerId: String,
    val tokenizerArtifactDigest: HimSha256,
    val modelConfigurationArtifactDigest: HimSha256,
    val logicalDigest: HimSha256,
    val modelBindingReference: String,
) {
    init {
        requireStableIdentifier("modelFamilyId", modelFamilyId)
        requireStableIdentifier("baseModelId", baseModelId)
        requireStableIdentifier("tokenizerId", tokenizerId)
        require(MODEL_BINDING_REFERENCE_PATTERN.matches(modelBindingReference)) {
            "Model-binding reference is invalid."
        }
        require(modelBindingReference == "model-binding:v1:${logicalDigest.value}") {
            "Model-binding reference is not bound to the logical digest."
        }
    }

    override fun equals(other: Any?): Boolean =
        other is HimModelBindingV1 && logicalDigest == other.logicalDigest

    override fun hashCode(): Int = logicalDigest.hashCode()

    companion object {
        const val CONTRACT_ID = "HIM_MODEL_BINDING_V1"
        const val VERSION = "1"
        const val STATE = "MODEL_BINDING_VALIDATED"

        fun create(
            modelFamilyId: String,
            baseModelId: String,
            baseModelArtifactDigest: HimSha256,
            tokenizerId: String,
            tokenizerArtifactDigest: HimSha256,
            modelConfigurationArtifactDigest: HimSha256,
        ): HimModelBindingV1 {
            requireStableIdentifier("modelFamilyId", modelFamilyId)
            requireStableIdentifier("baseModelId", baseModelId)
            requireStableIdentifier("tokenizerId", tokenizerId)

            val logicalDigest = digest(
                modelFamilyId = modelFamilyId,
                baseModelId = baseModelId,
                baseModelArtifactDigest = baseModelArtifactDigest,
                tokenizerId = tokenizerId,
                tokenizerArtifactDigest = tokenizerArtifactDigest,
                modelConfigurationArtifactDigest = modelConfigurationArtifactDigest,
            )
            return HimModelBindingV1(
                modelFamilyId = modelFamilyId,
                baseModelId = baseModelId,
                baseModelArtifactDigest = baseModelArtifactDigest,
                tokenizerId = tokenizerId,
                tokenizerArtifactDigest = tokenizerArtifactDigest,
                modelConfigurationArtifactDigest = modelConfigurationArtifactDigest,
                logicalDigest = logicalDigest,
                modelBindingReference = "model-binding:v1:${logicalDigest.value}",
            )
        }

        private fun digest(
            modelFamilyId: String,
            baseModelId: String,
            baseModelArtifactDigest: HimSha256,
            tokenizerId: String,
            tokenizerArtifactDigest: HimSha256,
            modelConfigurationArtifactDigest: HimSha256,
        ): HimSha256 {
            val canonical = buildString {
                field("contract", CONTRACT_ID)
                field("version", VERSION)
                field("state", STATE)
                field("model-family-id", modelFamilyId)
                field("base-model-id", baseModelId)
                field("base-model-artifact-digest", baseModelArtifactDigest.value)
                field("tokenizer-id", tokenizerId)
                field("tokenizer-artifact-digest", tokenizerArtifactDigest.value)
                field("model-configuration-artifact-digest", modelConfigurationArtifactDigest.value)
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
