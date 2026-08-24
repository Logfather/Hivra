package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation
import java.text.Normalizer
import java.util.Locale

/**
 * Source-neutral evidence alignment. Source adapters must provide the extracted
 * identity/modifier boundary; this contract does not infer that boundary from
 * source text.
 */
object HimEvidenceAlignmentContractV1 {
    const val VERSION = "HIM_SOURCE_AGNOSTIC_EVIDENCE_ALIGNMENT_CONTRACT_V1"

    fun evaluate(
        input: HimEvidenceAlignmentInputV1,
        canonicalFamily: HimCanonicalFamily,
    ): HimEvidenceAlignmentResultV1 {
        val canonicalName = normalize(canonicalFamily.canonicalName)
        val canonicalStoredName = normalize(canonicalFamily.normalizedName)
        require(canonicalName.isNotEmpty() && canonicalStoredName.isNotEmpty())
        require(canonicalName == canonicalStoredName) {
            "Canonical name and normalized name do not agree."
        }

        val boundTerms = authorityBoundTerms(canonicalFamily)
        val normalizedPrimary = input.primaryIdentity?.let(::normalize)
        val normalizedModifiers = input.modifiers.map(::normalize)
        val canonicalAppearsAsModifier = normalizedModifiers.any { it == canonicalName }

        val classification: HimEvidenceAlignmentClassificationV1
        val matchedEntity: HimEvidenceAuthorityMatchV1?
        when (input.primaryIdentityState) {
            HimPrimaryIdentityStateV1.MISSING -> {
                classification = HimEvidenceAlignmentClassificationV1.MISSING_PRIMARY_IDENTITY
                matchedEntity = null
            }
            HimPrimaryIdentityStateV1.UNRESOLVED -> {
                classification = HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY
                matchedEntity = null
            }
            HimPrimaryIdentityStateV1.RESOLVED -> {
                require(!normalizedPrimary.isNullOrEmpty())
                when {
                    normalizedPrimary == canonicalName -> {
                        classification = HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH
                        matchedEntity = HimEvidenceAuthorityMatchV1(
                            HimEntityType.CANONICAL,
                            canonicalFamily.canonicalId,
                            canonicalFamily.canonicalName,
                        )
                    }
                    boundTerms.count { it.normalizedName == normalizedPrimary } == 1 -> {
                        val term = boundTerms.single { it.normalizedName == normalizedPrimary }
                        classification = HimEvidenceAlignmentClassificationV1.PRIMARY_AUTHORITY_BOUND_MATCH
                        matchedEntity = term.toMatch()
                    }
                    boundTerms.any { it.normalizedName == normalizedPrimary } -> {
                        classification = HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY
                        matchedEntity = null
                    }
                    canonicalAppearsAsModifier -> {
                        classification = HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER
                        matchedEntity = null
                    }
                    else -> {
                        classification = HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY
                        matchedEntity = null
                    }
                }
            }
        }

        val directEvidenceSupported = classification.supportsDirectEvidence
        val modifierCoverage = normalizedModifiers.mapIndexed { index, normalizedModifier ->
            HimEvidenceModifierCoverageV1(
                modifier = input.modifiers[index],
                normalizedModifier = normalizedModifier,
                coveredByAuthority = boundTerms.any { it.normalizedName == normalizedModifier },
            )
        }

        return HimEvidenceAlignmentResultV1(
            sourceRecordIdentity = input.sourceRecordIdentity,
            primaryIdentity = input.primaryIdentity,
            modifiers = input.modifiers,
            classification = classification,
            matchedAuthorityEntity = matchedEntity,
            directEvidenceSupported = directEvidenceSupported,
            effectiveEvidenceRelation =
                if (directEvidenceSupported) HimSemanticEvidenceRelation.DIRECT else null,
            claimedEvidenceRelation = input.claimedEvidenceRelation,
            modifierCoverage = modifierCoverage,
        )
    }

    private fun authorityBoundTerms(family: HimCanonicalFamily): List<BoundTerm> = buildList {
        family.identities.forEach { identity ->
            add(BoundTerm(HimEntityType.IDENTITY, identity.identityId, identity.identityName, identity.normalizedName))
            identity.variants.forEach { variant ->
                add(BoundTerm(HimEntityType.VARIANT, variant.variantId, variant.variantName, variant.normalizedName))
            }
            identity.aliases.forEach { alias ->
                add(BoundTerm(HimEntityType.ALIAS, alias.aliasId, alias.aliasName, alias.normalizedName))
            }
        }
        family.variants.forEach { variant ->
            add(BoundTerm(HimEntityType.VARIANT, variant.variantId, variant.variantName, variant.normalizedName))
        }
        family.aliases.forEach { alias ->
            add(BoundTerm(HimEntityType.ALIAS, alias.aliasId, alias.aliasName, alias.normalizedName))
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC)
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)

    private data class BoundTerm(
        val entityType: HimEntityType,
        val entityId: HimEntityId,
        val name: String,
        val normalizedName: String,
    ) {
        fun toMatch() = HimEvidenceAuthorityMatchV1(entityType, entityId, name)
    }
}

enum class HimPrimaryIdentityStateV1 {
    RESOLVED,
    MISSING,
    UNRESOLVED,
}

data class HimEvidenceAlignmentInputV1(
    val sourceRecordIdentity: String,
    val primaryIdentity: String?,
    val primaryIdentityState: HimPrimaryIdentityStateV1,
    val modifiers: List<String> = emptyList(),
    val claimedEvidenceRelation: HimSemanticEvidenceRelation? = null,
) {
    init {
        require(sourceRecordIdentity.isNotBlank())
        when (primaryIdentityState) {
            HimPrimaryIdentityStateV1.RESOLVED -> require(!primaryIdentity.isNullOrBlank())
            HimPrimaryIdentityStateV1.MISSING,
            HimPrimaryIdentityStateV1.UNRESOLVED,
            -> require(primaryIdentity == null)
        }
        require(modifiers.all(String::isNotBlank))
    }
}

enum class HimEvidenceAlignmentClassificationV1(
    val supportsDirectEvidence: Boolean,
) {
    PRIMARY_CANONICAL_MATCH(true),
    PRIMARY_AUTHORITY_BOUND_MATCH(true),
    CANONICAL_ONLY_AS_MODIFIER(false),
    OTHER_PRIMARY_IDENTITY(false),
    MISSING_PRIMARY_IDENTITY(false),
    UNRESOLVED_PRIMARY_IDENTITY(false),
}

data class HimEvidenceAuthorityMatchV1(
    val entityType: HimEntityType,
    val entityId: HimEntityId,
    val name: String,
) {
    init { require(name.isNotBlank()) }
}

data class HimEvidenceModifierCoverageV1(
    val modifier: String,
    val normalizedModifier: String,
    val coveredByAuthority: Boolean,
) {
    init {
        require(modifier.isNotBlank() && normalizedModifier.isNotBlank())
    }
}

data class HimEvidenceAlignmentResultV1(
    val sourceRecordIdentity: String,
    val primaryIdentity: String?,
    val modifiers: List<String>,
    val classification: HimEvidenceAlignmentClassificationV1,
    val matchedAuthorityEntity: HimEvidenceAuthorityMatchV1?,
    val directEvidenceSupported: Boolean,
    val effectiveEvidenceRelation: HimSemanticEvidenceRelation?,
    val claimedEvidenceRelation: HimSemanticEvidenceRelation?,
    val modifierCoverage: List<HimEvidenceModifierCoverageV1>,
) {
    init {
        require(sourceRecordIdentity.isNotBlank())
        require(modifiers.size == modifierCoverage.size)
        require(directEvidenceSupported == classification.supportsDirectEvidence)
        require((effectiveEvidenceRelation == HimSemanticEvidenceRelation.DIRECT) == directEvidenceSupported)
        require(!directEvidenceSupported || matchedAuthorityEntity != null)
        require(directEvidenceSupported || matchedAuthorityEntity == null)
    }

    val uncoveredModifiers: List<String>
        get() = modifierCoverage.filterNot { it.coveredByAuthority }.map { it.modifier }
}
