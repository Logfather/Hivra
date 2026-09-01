package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.security.MessageDigest

private val OPTIMIZER_ID_PATTERN = Regex("[a-z][a-z0-9]*(?::[a-z][a-z0-9]*)*")
private val MAPPING_REFERENCE_PATTERN = Regex("optimizer-mapping:v1:[0-9a-f]{64}")

/**
 * The deterministic, framework-independent optimizer semantic boundary for Training V1.
 * It identifies an algorithm without constructing or executing an optimizer.
 */
class HimOptimizerMappingV1 private constructor(
    val contractId: String,
    val version: String,
    val state: String,
    val supportedOptimizerIds: List<String>,
    val supportedAlgorithmKinds: List<AlgorithmKind>,
    val profileCompleteness: ProfileCompleteness,
    val unresolvedMandatoryParameters: List<String>,
    val logicalDigest: HimSha256,
    val mappingReference: String,
) {
    init {
        require(contractId == CONTRACT_ID)
        require(version == VERSION)
        require(state == STATE)
        require(supportedOptimizerIds == SUPPORTED_OPTIMIZER_IDS)
        require(supportedAlgorithmKinds == SUPPORTED_ALGORITHM_KINDS)
        require(profileCompleteness == ProfileCompleteness.INCOMPLETE)
        require(unresolvedMandatoryParameters == UNRESOLVED_MANDATORY_PARAMETERS)
        require(MAPPING_REFERENCE_PATTERN.matches(mappingReference)) {
            "Optimizer-mapping reference is invalid."
        }
        require(mappingReference == "$MAPPING_REFERENCE_PREFIX${logicalDigest.value}") {
            "Optimizer-mapping reference is not bound to the logical digest."
        }
    }

    override fun equals(other: Any?): Boolean =
        other is HimOptimizerMappingV1 && logicalDigest == other.logicalDigest

    override fun hashCode(): Int = logicalDigest.hashCode()

    /** Resolves an exact semantic optimizer identifier; no normalization or fallback is applied. */
    fun resolve(optimizerId: String): Resolved {
        require(optimizerId.matches(OPTIMIZER_ID_PATTERN)) { "INVALID_OPTIMIZER_ID" }
        return when (optimizerId) {
            ADAMW_ID -> Resolved.create(
                optimizerId = optimizerId,
                algorithmKind = AlgorithmKind.ADAMW,
                mappingVersion = version,
                mappingDigest = logicalDigest,
                profileCompleteness = profileCompleteness,
                unresolvedMandatoryParameters = unresolvedMandatoryParameters,
            )
            else -> error("UNKNOWN_OPTIMIZER")
        }
    }

    /** Resolves the already validated optimizer identifier carried by Training Configuration V1. */
    fun resolve(configuration: HimTrainingConfigurationV1): Resolved = resolve(configuration.optimizerId)

    enum class AlgorithmKind {
        ADAMW,
    }

    enum class ProfileCompleteness {
        INCOMPLETE,
    }

    data class Resolved private constructor(
        val optimizerId: String,
        val algorithmKind: AlgorithmKind,
        val mappingVersion: String,
        val mappingDigest: HimSha256,
        val profileCompleteness: ProfileCompleteness,
        val unresolvedMandatoryParameters: List<String>,
        val runtimeReady: Boolean,
        val logicalDigest: HimSha256,
        val optimizerReference: String,
    ) {
        init {
            require(optimizerId.matches(OPTIMIZER_ID_PATTERN))
            require(mappingVersion == VERSION)
            require(profileCompleteness == ProfileCompleteness.INCOMPLETE)
            require(unresolvedMandatoryParameters == UNRESOLVED_MANDATORY_PARAMETERS)
            require(!runtimeReady)
            require(optimizerReference == "$OPTIMIZER_REFERENCE_PREFIX${logicalDigest.value}")
        }

        companion object {
            internal fun create(
                optimizerId: String,
                algorithmKind: AlgorithmKind,
                mappingVersion: String,
                mappingDigest: HimSha256,
                profileCompleteness: ProfileCompleteness,
                unresolvedMandatoryParameters: List<String>,
            ): Resolved {
                val digest = resolvedDigest(
                    optimizerId = optimizerId,
                    algorithmKind = algorithmKind,
                    mappingVersion = mappingVersion,
                    mappingDigest = mappingDigest,
                    profileCompleteness = profileCompleteness,
                    unresolvedMandatoryParameters = unresolvedMandatoryParameters,
                )
                return Resolved(
                    optimizerId = optimizerId,
                    algorithmKind = algorithmKind,
                    mappingVersion = mappingVersion,
                    mappingDigest = mappingDigest,
                    profileCompleteness = profileCompleteness,
                    unresolvedMandatoryParameters = unresolvedMandatoryParameters.toList(),
                    runtimeReady = false,
                    logicalDigest = digest,
                    optimizerReference = "$OPTIMIZER_REFERENCE_PREFIX${digest.value}",
                )
            }

            private fun resolvedDigest(
                optimizerId: String,
                algorithmKind: AlgorithmKind,
                mappingVersion: String,
                mappingDigest: HimSha256,
                profileCompleteness: ProfileCompleteness,
                unresolvedMandatoryParameters: List<String>,
            ): HimSha256 = sha256(buildString {
                field("contract", CONTRACT_ID)
                field("version", VERSION)
                field("state", STATE)
                field("optimizer-id", optimizerId)
                field("algorithm-kind", algorithmKind.name)
                field("mapping-version", mappingVersion)
                field("mapping-digest", mappingDigest.value)
                field("profile-completeness", profileCompleteness.name)
                unresolvedMandatoryParameters.forEachIndexed { index, parameter ->
                    field("unresolved-$index", parameter)
                }
            })
        }
    }

    companion object {
        const val CONTRACT_ID = "HIM_OPTIMIZER_MAPPING_V1"
        const val VERSION = "1"
        const val STATE = "OPTIMIZER_MAPPING_DEFINED"
        const val ADAMW_ID = "optimizer:adamw:v1"

        private const val MAPPING_REFERENCE_PREFIX = "optimizer-mapping:v1:"
        private const val OPTIMIZER_REFERENCE_PREFIX = "optimizer-spec:v1:"

        val SUPPORTED_OPTIMIZER_IDS = listOf(ADAMW_ID)
        val SUPPORTED_ALGORITHM_KINDS = listOf(AlgorithmKind.ADAMW)
        val UNRESOLVED_MANDATORY_PARAMETERS = listOf(
            "beta1",
            "beta2",
            "epsilon",
            "weight-decay",
            "decoupled-weight-decay",
            "bias-correction",
        )

        fun create(): HimOptimizerMappingV1 {
            val digest = mappingDigest()
            return HimOptimizerMappingV1(
                contractId = CONTRACT_ID,
                version = VERSION,
                state = STATE,
                supportedOptimizerIds = SUPPORTED_OPTIMIZER_IDS.toList(),
                supportedAlgorithmKinds = SUPPORTED_ALGORITHM_KINDS.toList(),
                profileCompleteness = ProfileCompleteness.INCOMPLETE,
                unresolvedMandatoryParameters = UNRESOLVED_MANDATORY_PARAMETERS.toList(),
                logicalDigest = digest,
                mappingReference = "$MAPPING_REFERENCE_PREFIX${digest.value}",
            )
        }

        private fun mappingDigest(): HimSha256 = sha256(buildString {
            field("contract", CONTRACT_ID)
            field("version", VERSION)
            field("state", STATE)
            SUPPORTED_OPTIMIZER_IDS.forEachIndexed { index, optimizerId ->
                field("optimizer-$index", optimizerId)
            }
            SUPPORTED_ALGORITHM_KINDS.forEachIndexed { index, kind ->
                field("algorithm-$index", kind.name)
            }
            field("profile-completeness", ProfileCompleteness.INCOMPLETE.name)
            UNRESOLVED_MANDATORY_PARAMETERS.forEachIndexed { index, parameter ->
                field("unresolved-$index", parameter)
            }
        })

        private fun sha256(value: String): HimSha256 = HimSha256(
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )

        private fun StringBuilder.field(key: String, value: String) {
            append(key).append('=').append(value.length).append(':').append(value).append('\n')
        }
    }
}
