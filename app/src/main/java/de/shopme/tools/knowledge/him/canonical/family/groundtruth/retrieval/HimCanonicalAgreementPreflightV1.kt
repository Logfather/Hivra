package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority

/**
 * Read-only validation of canonical display names against their persisted keys.
 * It deliberately does not open evidence stores or perform retrieval.
 */
object HimCanonicalAgreementPreflightV1 {
    const val VERSION = "HIM_CANONICAL_AGREEMENT_PREFLIGHT_V1"

    fun evaluate(
        authority: HimCanonicalFamilyAuthority,
    ): HimCanonicalAgreementPreflightResultV1 {
        val orderedFamilies = authority.families.sortedWith(
            compareBy<HimCanonicalFamily> { it.canonicalId.value }
                .thenBy { it.canonicalName },
        )
        val mismatches = orderedFamilies.mapNotNull { family ->
            try {
                HimEvidenceAlignmentContractV1.validateCanonicalAgreement(family)
                null
            } catch (_: IllegalArgumentException) {
                HimCanonicalAgreementMismatchV1(
                    entityId = family.canonicalId.value,
                    canonicalName = family.canonicalName,
                    normalizedName = family.normalizedName,
                )
            }
        }
        return HimCanonicalAgreementPreflightResultV1(
            canonicalCount = orderedFamilies.size,
            mismatches = mismatches,
        )
    }
}

data class HimCanonicalAgreementPreflightResultV1(
    val canonicalCount: Int,
    val mismatches: List<HimCanonicalAgreementMismatchV1>,
) {
    init {
        require(canonicalCount >= 0)
        require(mismatches == mismatches.sortedWith(compareBy { it.entityId }))
        require(mismatches.map { it.entityId }.distinct().size == mismatches.size)
    }

    val passed: Boolean
        get() = mismatches.isEmpty()
}

data class HimCanonicalAgreementMismatchV1(
    val entityId: String,
    val canonicalName: String,
    val normalizedName: String,
)
