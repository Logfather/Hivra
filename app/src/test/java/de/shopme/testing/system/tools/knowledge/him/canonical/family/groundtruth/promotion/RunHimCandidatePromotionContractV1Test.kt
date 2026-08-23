package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.promotion

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionProposalFactoryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionTargetV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimCandidatePromotionContractV1Test {

    @Test
    fun `approved Hering eingelegt variant produces deterministic promotion proposal`() {
        val candidate =
            candidate("1")

        val validation =
            validation("2")

        val datasetDigest =
            HimSha256("a".repeat(64))

        val authorityDigest =
            HimSha256("b".repeat(64))

        val registryDigest =
            HimSha256("c".repeat(64))

        val eligibility =
            eligible(
                candidate =
                    candidate,
                validation =
                    validation,
            )

        val first =
            HimCandidatePromotionProposalFactoryV1
                .create(
                    candidateReference =
                        candidate,
                    candidateRelation =
                        HimCandidateRelation.Variant(
                            HimFamilyEntityReference.Canonical(
                                canonicalId =
                                    HimEntityId("OzlByp")
                            )
                        ),
                    candidateTerm =
                        "Hering eingelegt",
                    candidateDatasetDigest =
                        datasetDigest,
                    authorityDigestBefore =
                        authorityDigest,
                    entityIdRegistryDigestBefore =
                        registryDigest,
                    eligibility =
                        eligibility,
                )

        val replay =
            HimCandidatePromotionProposalFactoryV1
                .create(
                    candidateReference =
                        candidate,
                    candidateRelation =
                        HimCandidateRelation.Variant(
                            HimFamilyEntityReference.Canonical(
                                canonicalId =
                                    HimEntityId("OzlByp")
                            )
                        ),
                    candidateTerm =
                        "Hering eingelegt",
                    candidateDatasetDigest =
                        datasetDigest,
                    authorityDigestBefore =
                        authorityDigest,
                    entityIdRegistryDigestBefore =
                        registryDigest,
                    eligibility =
                        eligibility,
                )

        assertEquals(
            first,
            replay,
        )

        assertEquals(
            HimCandidatePromotionType.ADD_VARIANT,
            first.target.promotionType,
        )

        val target =
            first.target as
                    HimCandidatePromotionTargetV1.AddVariant

        assertEquals(
            "Hering eingelegt",
            target.variantName,
        )

        assertEquals(
            HimEntityId("OzlByp"),
            target.scope.canonicalId,
        )
    }

    @Test
    fun `promotion reference changes when authority precondition changes`() {
        val target =
            HimCandidatePromotionTargetV1.AddVariant(
                scope =
                    HimFamilyEntityReference.Canonical(
                        canonicalId =
                            HimEntityId("OzlByp")
                    ),
                variantName =
                    "Hering eingelegt",
            )

        val first =
            HimCandidatePromotionIdentityV1
                .promotion(
                    candidateReference =
                        candidate("3"),
                    validationReference =
                        validation("4"),
                    candidateDatasetDigest =
                        HimSha256("d".repeat(64)),
                    authorityDigestBefore =
                        HimSha256("e".repeat(64)),
                    entityIdRegistryDigestBefore =
                        HimSha256("f".repeat(64)),
                    target =
                        target,
                )

        val changedAuthority =
            HimCandidatePromotionIdentityV1
                .promotion(
                    candidateReference =
                        candidate("3"),
                    validationReference =
                        validation("4"),
                    candidateDatasetDigest =
                        HimSha256("d".repeat(64)),
                    authorityDigestBefore =
                        HimSha256("1".repeat(64)),
                    entityIdRegistryDigestBefore =
                        HimSha256("f".repeat(64)),
                    target =
                        target,
                )

        assertNotEquals(
            first,
            changedAuthority,
        )
    }

    @Test
    fun `promotion reference changes when registry precondition changes`() {
        val target =
            HimCandidatePromotionTargetV1.CreateCanonical(
                canonicalName =
                    "Diagnostic Canonical",
            )

        val first =
            HimCandidatePromotionIdentityV1
                .promotion(
                    candidateReference =
                        candidate("5"),
                    validationReference =
                        validation("6"),
                    candidateDatasetDigest =
                        HimSha256("2".repeat(64)),
                    authorityDigestBefore =
                        HimSha256("3".repeat(64)),
                    entityIdRegistryDigestBefore =
                        HimSha256("4".repeat(64)),
                    target =
                        target,
                )

        val changedRegistry =
            HimCandidatePromotionIdentityV1
                .promotion(
                    candidateReference =
                        candidate("5"),
                    validationReference =
                        validation("6"),
                    candidateDatasetDigest =
                        HimSha256("2".repeat(64)),
                    authorityDigestBefore =
                        HimSha256("3".repeat(64)),
                    entityIdRegistryDigestBefore =
                        HimSha256("7".repeat(64)),
                    target =
                        target,
                )

        assertNotEquals(
            first,
            changedRegistry,
        )
    }

    @Test
    fun `non eligible candidate cannot produce promotion proposal`() {
        val candidate =
            candidate("7")

        val failure =
            runCatching {
                HimCandidatePromotionProposalFactoryV1
                    .create(
                        candidateReference =
                            candidate,
                        candidateRelation =
                            HimCandidateRelation.CreateNewCanonical,
                        candidateTerm =
                            "Blocked Candidate",
                        candidateDatasetDigest =
                            HimSha256("5".repeat(64)),
                        authorityDigestBefore =
                            HimSha256("6".repeat(64)),
                        entityIdRegistryDigestBefore =
                            HimSha256("7".repeat(64)),
                        eligibility =
                            HimCandidatePromotionEligibilityResultV1(
                                candidateReference =
                                    candidate,
                                status =
                                    HimCandidatePromotionEligibilityStatus
                                        .BLOCKED_REJECTED,
                                validationReference =
                                    validation("8"),
                            ),
                    )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )

        assertEquals(
            "Candidate is not promotion eligible: BLOCKED_REJECTED",
            failure?.message,
        )
    }

    @Test
    fun `eligibility cannot be reused for another candidate`() {
        val eligibleCandidate =
            candidate("9")

        val otherCandidate =
            candidate("a")

        val failure =
            runCatching {
                HimCandidatePromotionProposalFactoryV1
                    .create(
                        candidateReference =
                            otherCandidate,
                        candidateRelation =
                            HimCandidateRelation.CreateNewCanonical,
                        candidateTerm =
                            "Other Candidate",
                        candidateDatasetDigest =
                            HimSha256("8".repeat(64)),
                        authorityDigestBefore =
                            HimSha256("9".repeat(64)),
                        entityIdRegistryDigestBefore =
                            HimSha256("a".repeat(64)),
                        eligibility =
                            eligible(
                                candidate =
                                    eligibleCandidate,
                                validation =
                                    validation("b"),
                            ),
                    )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )

        assertEquals(
            "Promotion eligibility belongs to a different candidate.",
            failure?.message,
        )
    }

    @Test
    fun `all candidate relations map deterministically to promotion targets`() {
        val canonical =
            HimEntityId("OzlByp")

        val identity =
            HimEntityId("AbCd12")

        val cases =
            listOf(
                HimCandidateRelation.Identity(
                    parentCanonicalId =
                        canonical
                ) to
                        HimCandidatePromotionType.ADD_IDENTITY,

                HimCandidateRelation.Variant(
                    scope =
                        HimFamilyEntityReference.Canonical(
                            canonicalId =
                                canonical
                        )
                ) to
                        HimCandidatePromotionType.ADD_VARIANT,

                HimCandidateRelation.Alias(
                    equivalentEntity =
                        HimFamilyEntityReference.Identity(
                            canonicalId =
                                canonical,
                            identityId =
                                identity,
                        )
                ) to
                        HimCandidatePromotionType.ADD_ALIAS,

                HimCandidateRelation.CreateNewCanonical to
                        HimCandidatePromotionType.CREATE_CANONICAL,
            )

        cases.forEachIndexed { index, case ->
            val candidate =
                candidate(
                    (index + 1)
                        .toString(16)
                )

            val proposal =
                HimCandidatePromotionProposalFactoryV1
                    .create(
                        candidateReference =
                            candidate,
                        candidateRelation =
                            case.first,
                        candidateTerm =
                            "Candidate ${index + 1}",
                        candidateDatasetDigest =
                            HimSha256("b".repeat(64)),
                        authorityDigestBefore =
                            HimSha256("c".repeat(64)),
                        entityIdRegistryDigestBefore =
                            HimSha256("d".repeat(64)),
                        eligibility =
                            eligible(
                                candidate =
                                    candidate,
                                validation =
                                    validation(
                                        (index + 1)
                                            .toString(16)
                                    ),
                            ),
                    )

            assertEquals(
                case.second,
                proposal.target.promotionType,
            )
        }
    }

    private fun eligible(
        candidate: HimCandidateReference,
        validation:
        HimCandidateValidationDecisionReference,
    ) =
        HimCandidatePromotionEligibilityResultV1(
            candidateReference =
                candidate,
            status =
                HimCandidatePromotionEligibilityStatus
                    .PROMOTION_ELIGIBLE,
            validationReference =
                validation,
        )

    private fun candidate(
        digit: String,
    ) =
        HimCandidateReference(
            "candidate:v1:" +
                    digit.repeat(64)
        )

    private fun validation(
        digit: String,
    ) =
        HimCandidateValidationDecisionReference(
            "validation:v1:" +
                    digit.repeat(64)
        )
}