package de.shopme.tools.knowledge.him.training.authority

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.security.MessageDigest

/**
 * Immutable model-visible input binding for the two already-authorized P1
 * negative pairs.  The term is source-faithful evidence, never a heuristic
 * projection of the candidate or of the negative-example reference.
 */
object HimModelVisibleObservedTermAuthorityV2 {
    const val CONTRACT_ID = "HIM_MODEL_VISIBLE_OBSERVED_TERM_AUTHORITY_V2"
    const val VERSION = "2"
    const val STATE = "MODEL_VISIBLE_OBSERVED_TERM_AUTHORITY_PERSISTED"
    const val OBSERVED_TERM = "Brie double crème"
    const val HUMAN_AUTHORITY_REFERENCE = "user-response:training-input-authority-v2-batch-1"
    const val CANDIDATE_COMPATIBILITY_AUTHORITY_REFERENCE =
        "training-input-authority:v2:p1-training-input-authority-v2-human-confirmation-v1"
    const val SOURCE_AUTHORITY = "ORIGINAL_V1_HUMAN_AUTHORITY"

    data class BindingV1(
        val exampleReference: String,
        val candidateReference: String,
        val evidenceReference: String,
        val observedTerm: String,
        val humanAuthorityReference: String,
        val candidateCompatibilityAuthorityReference: String,
        val sourceAuthority: String,
        val sourceArtifactSha256: HimSha256,
        val candidateCompatibility: String,
        val secondaryTarget: Int,
        val secondaryMask: Int,
        val logicalDigest: HimSha256,
        val reference: String,
    ) {
        init {
            require(exampleReference.startsWith("negative-example:v1:"))
            require(candidateReference == "uVfHe4")
            require(evidenceReference.matches(HEX_PATTERN))
            require(observedTerm == OBSERVED_TERM)
            require(humanAuthorityReference == HUMAN_AUTHORITY_REFERENCE)
            require(candidateCompatibilityAuthorityReference == CANDIDATE_COMPATIBILITY_AUTHORITY_REFERENCE)
            require(sourceAuthority == SOURCE_AUTHORITY)
            require(candidateCompatibility == "REJECT")
            require(secondaryTarget == 1 && secondaryMask == 1)
            require(logicalDigest == digest(identityPayload()))
            require(reference == "model-visible-observed-term-binding:v2:${logicalDigest.value}")
        }

        fun identityPayload(): String = lengthPrefixed(
            "candidate-compatibility-authority" to candidateCompatibilityAuthorityReference,
            "candidate-reference" to candidateReference,
            "evidence-reference" to evidenceReference,
            "example-reference" to exampleReference,
            "human-authority-reference" to humanAuthorityReference,
            "observed-term" to observedTerm,
            "secondary-mask" to secondaryMask.toString(),
            "secondary-target" to secondaryTarget.toString(),
            "source-artifact-sha256" to sourceArtifactSha256.value,
            "source-authority" to sourceAuthority,
            "version" to VERSION,
        )
    }

    data class AuthorityV2(
        val contractId: String,
        val version: String,
        val state: String,
        val bindings: List<BindingV1>,
        val logicalDigest: HimSha256,
        val reference: String,
    ) {
        init {
            require(contractId == CONTRACT_ID && version == VERSION && state == STATE)
            require(bindings.size == 2)
            require(bindings.map { it.exampleReference }.distinct().size == 2)
            require(bindings.all { it.observedTerm == OBSERVED_TERM && it.candidateCompatibility == "REJECT" })
            require(logicalDigest == digest(identityPayload()))
            require(reference == "model-visible-observed-term-authority:v2:${logicalDigest.value}")
        }

        fun identityPayload(): String = lengthPrefixed(
            "binding-0" to bindings[0].logicalDigest.value,
            "binding-1" to bindings[1].logicalDigest.value,
            "contract" to contractId,
            "state" to state,
            "version" to version,
        )
    }

    fun create(): AuthorityV2 {
        val bindings = listOf(
            binding(
                exampleReference = "negative-example:v1:113a5ce116991c864581175a486ff1f65ce89c5c9b892f5c0399c40ed9c84146",
                evidenceReference = "280b91e4c4e10f95288308d50a0df31e3be773385c11afc104e8525127cc5b1e",
                sourceArtifactSha256 = "194b20a9b3fb57dcc0b1a0883bafae556b390d8321089468efc063ec554bcd0e",
            ),
            binding(
                exampleReference = "negative-example:v1:bc4960dcca492cd8fe8f9181d4eb95561fdb254605598403aea70d7efc620107",
                evidenceReference = "045ee67398a6f7355b0faef3760ceb0d24bf22515b9e67d84c7931e96d2c9fba",
                sourceArtifactSha256 = "6dbe77456d066dc32ec8dc2e6ab0c2b6c8f43d934833b0d78ac295cb1e7affd8",
            ),
        )
        val authorityDigest = digest(
            lengthPrefixed(
                "binding-0" to bindings[0].logicalDigest.value,
                "binding-1" to bindings[1].logicalDigest.value,
                "contract" to CONTRACT_ID,
                "state" to STATE,
                "version" to VERSION,
            ),
        )
        return AuthorityV2(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            bindings = bindings,
            logicalDigest = authorityDigest,
            reference = "model-visible-observed-term-authority:v2:${authorityDigest.value}",
        )
    }

    private fun binding(
        exampleReference: String,
        evidenceReference: String,
        sourceArtifactSha256: String,
    ): BindingV1 {
        val identityPayload = lengthPrefixed(
            "candidate-compatibility-authority" to CANDIDATE_COMPATIBILITY_AUTHORITY_REFERENCE,
            "candidate-reference" to "uVfHe4",
            "evidence-reference" to evidenceReference,
            "example-reference" to exampleReference,
            "human-authority-reference" to HUMAN_AUTHORITY_REFERENCE,
            "observed-term" to OBSERVED_TERM,
            "secondary-mask" to "1",
            "secondary-target" to "1",
            "source-artifact-sha256" to sourceArtifactSha256,
            "source-authority" to SOURCE_AUTHORITY,
            "version" to VERSION,
        )
        val logicalDigest = digest(identityPayload)
        return BindingV1(
            exampleReference = exampleReference,
            candidateReference = "uVfHe4",
            evidenceReference = evidenceReference,
            observedTerm = OBSERVED_TERM,
            humanAuthorityReference = HUMAN_AUTHORITY_REFERENCE,
            candidateCompatibilityAuthorityReference = CANDIDATE_COMPATIBILITY_AUTHORITY_REFERENCE,
            sourceAuthority = SOURCE_AUTHORITY,
            sourceArtifactSha256 = HimSha256(sourceArtifactSha256),
            candidateCompatibility = "REJECT",
            secondaryTarget = 1,
            secondaryMask = 1,
            logicalDigest = logicalDigest,
            reference = "model-visible-observed-term-binding:v2:${logicalDigest.value}",
        )
    }

    private val HEX_PATTERN = Regex("[0-9a-f]{64}")

    private fun digest(value: String): HimSha256 = HimSha256(
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    private fun lengthPrefixed(vararg fields: Pair<String, String>): String = buildString {
        fields.forEach { (name, value) ->
            append(name.length).append(':').append(name)
            append(value.length).append(':').append(value).append('|')
        }
    }
}
