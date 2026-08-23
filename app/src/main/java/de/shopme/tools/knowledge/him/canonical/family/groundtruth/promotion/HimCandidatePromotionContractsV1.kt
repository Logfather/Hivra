package de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import java.security.MessageDigest

object HimCandidatePromotionContractV1 {
    const val VERSION =
        "HIM_CANDIDATE_PROMOTION_V1"

    const val PROMOTION_REFERENCE_CONTRACT =
        "HIM_CANDIDATE_PROMOTION_REFERENCE_V1"
}

enum class HimCandidatePromotionType {
    ADD_IDENTITY,
    ADD_VARIANT,
    ADD_ALIAS,
    CREATE_CANONICAL,
}

sealed interface HimCandidatePromotionTargetV1 {

    val promotionType: HimCandidatePromotionType

    data class AddIdentity(
        val parentCanonicalId: HimEntityId,
        val identityName: String,
    ) : HimCandidatePromotionTargetV1 {
        override val promotionType =
            HimCandidatePromotionType.ADD_IDENTITY

        init {
            require(identityName.isNotBlank())
        }
    }

    data class AddVariant(
        val scope: HimFamilyEntityReference,
        val variantName: String,
    ) : HimCandidatePromotionTargetV1 {
        override val promotionType =
            HimCandidatePromotionType.ADD_VARIANT

        init {
            require(variantName.isNotBlank())
        }
    }

    data class AddAlias(
        val equivalentEntity: HimFamilyEntityReference,
        val aliasName: String,
    ) : HimCandidatePromotionTargetV1 {
        override val promotionType =
            HimCandidatePromotionType.ADD_ALIAS

        init {
            require(aliasName.isNotBlank())
        }
    }

    data class CreateCanonical(
        val canonicalName: String,
    ) : HimCandidatePromotionTargetV1 {
        override val promotionType =
            HimCandidatePromotionType.CREATE_CANONICAL

        init {
            require(canonicalName.isNotBlank())
        }
    }
}

data class HimCandidatePromotionReference(
    val value: String,
) {
    init {
        require(
            value.matches(
                Regex("promotion:v1:[0-9a-f]{64}")
            )
        )
    }
}

data class HimCandidatePromotionProposalV1(
    val contractVersion: String =
        HimCandidatePromotionContractV1.VERSION,

    val promotionReference:
    HimCandidatePromotionReference,

    val candidateReference:
    HimCandidateReference,

    val validationReference:
    HimCandidateValidationDecisionReference,

    val candidateDatasetDigest:
    HimSha256,

    val authorityDigestBefore:
    HimSha256,

    val entityIdRegistryDigestBefore:
    HimSha256,

    val target:
    HimCandidatePromotionTargetV1,
) {
    init {
        require(
            contractVersion ==
                    HimCandidatePromotionContractV1.VERSION
        )
    }
}

object HimCandidatePromotionIdentityV1 {

    fun promotion(
        candidateReference: HimCandidateReference,
        validationReference: HimCandidateValidationDecisionReference,
        candidateDatasetDigest: HimSha256,
        authorityDigestBefore: HimSha256,
        entityIdRegistryDigestBefore: HimSha256,
        target: HimCandidatePromotionTargetV1,
    ): HimCandidatePromotionReference {
        val canonical = buildString {
            appendLine(
                "contract=" +
                        HimCandidatePromotionContractV1
                            .PROMOTION_REFERENCE_CONTRACT
            )

            appendLine(
                "candidate=${candidateReference.value}"
            )

            appendLine(
                "validation=${validationReference.value}"
            )

            appendLine(
                "candidate-dataset-digest=" +
                        candidateDatasetDigest.value
            )

            appendLine(
                "authority-digest-before=" +
                        authorityDigestBefore.value
            )

            appendLine(
                "entity-id-registry-digest-before=" +
                        entityIdRegistryDigestBefore.value
            )

            appendLine(
                "promotion-type=${target.promotionType.name}"
            )

            when (target) {
                is HimCandidatePromotionTargetV1.AddIdentity -> {
                    appendLine(
                        "parent-canonical-id=" +
                                target.parentCanonicalId.value
                    )

                    appendLine(
                        "identity-name=" +
                                target.identityName
                    )
                }

                is HimCandidatePromotionTargetV1.AddVariant -> {
                    appendEntity(
                        prefix = "scope",
                        reference = target.scope,
                    )

                    appendLine(
                        "variant-name=" +
                                target.variantName
                    )
                }

                is HimCandidatePromotionTargetV1.AddAlias -> {
                    appendEntity(
                        prefix = "equivalent",
                        reference =
                            target.equivalentEntity,
                    )

                    appendLine(
                        "alias-name=" +
                                target.aliasName
                    )
                }

                is HimCandidatePromotionTargetV1.CreateCanonical -> {
                    appendLine(
                        "canonical-name=" +
                                target.canonicalName
                    )
                }
            }
        }

        return HimCandidatePromotionReference(
            "promotion:v1:${sha256(canonical)}"
        )
    }

    private fun StringBuilder.appendEntity(
        prefix: String,
        reference: HimFamilyEntityReference,
    ) {
        appendLine(
            "$prefix-canonical-id=" +
                    reference.canonicalId.value
        )

        appendLine(
            "$prefix-identity-id=" +
                    (
                            reference as?
                                    HimFamilyEntityReference.Identity
                            )
                        ?.identityId
                        ?.value
                        .orEmpty()
        )
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )
            .joinToString("") {
                "%02x".format(
                    it.toInt() and 0xff
                )
            }
}

object HimCandidatePromotionProposalFactoryV1 {

    fun create(
        candidateReference: HimCandidateReference,
        candidateRelation: HimCandidateRelation,
        candidateTerm: String,
        candidateDatasetDigest: HimSha256,
        authorityDigestBefore: HimSha256,
        entityIdRegistryDigestBefore: HimSha256,
        eligibility:
        HimCandidatePromotionEligibilityResultV1,
    ): HimCandidatePromotionProposalV1 {

        require(
            eligibility.status ==
                    HimCandidatePromotionEligibilityStatus
                        .PROMOTION_ELIGIBLE
        ) {
            "Candidate is not promotion eligible: ${eligibility.status}"
        }

        require(
            eligibility.candidateReference ==
                    candidateReference
        ) {
            "Promotion eligibility belongs to a different candidate."
        }

        val validationReference =
            requireNotNull(
                eligibility.validationReference
            )

        require(candidateTerm.isNotBlank())

        val target =
            when (candidateRelation) {
                is HimCandidateRelation.Identity ->
                    HimCandidatePromotionTargetV1.AddIdentity(
                        parentCanonicalId =
                            candidateRelation.parentCanonicalId,
                        identityName =
                            candidateTerm,
                    )

                is HimCandidateRelation.Variant ->
                    HimCandidatePromotionTargetV1.AddVariant(
                        scope =
                            candidateRelation.scope,
                        variantName =
                            candidateTerm,
                    )

                is HimCandidateRelation.Alias ->
                    HimCandidatePromotionTargetV1.AddAlias(
                        equivalentEntity =
                            candidateRelation.equivalentEntity,
                        aliasName =
                            candidateTerm,
                    )

                HimCandidateRelation.CreateNewCanonical ->
                    HimCandidatePromotionTargetV1.CreateCanonical(
                        canonicalName =
                            candidateTerm,
                    )
            }

        val promotionReference =
            HimCandidatePromotionIdentityV1.promotion(
                candidateReference =
                    candidateReference,
                validationReference =
                    validationReference,
                candidateDatasetDigest =
                    candidateDatasetDigest,
                authorityDigestBefore =
                    authorityDigestBefore,
                entityIdRegistryDigestBefore =
                    entityIdRegistryDigestBefore,
                target =
                    target,
            )

        return HimCandidatePromotionProposalV1(
            promotionReference =
                promotionReference,
            candidateReference =
                candidateReference,
            validationReference =
                validationReference,
            candidateDatasetDigest =
                candidateDatasetDigest,
            authorityDigestBefore =
                authorityDigestBefore,
            entityIdRegistryDigestBefore =
                entityIdRegistryDigestBefore,
            target =
                target,
        )
    }
}