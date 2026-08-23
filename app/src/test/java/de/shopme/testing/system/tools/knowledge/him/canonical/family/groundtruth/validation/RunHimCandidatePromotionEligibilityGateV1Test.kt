package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.validation

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDataset
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateHypothesis
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateMasterRecord
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityGateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecision
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationLedgerV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimCandidatePromotionEligibilityGateV1Test {

    @Test
    fun `approved candidate bound to current dataset is promotion eligible`() {
        val candidate =
            candidateReference("1")

        val digest =
            HimSha256("a".repeat(64))

        val dataset =
            dataset(candidate)

        val ledger =
            ledger(
                approved(
                    candidate = candidate,
                    digest = digest,
                )
            )

        val result =
            HimCandidatePromotionEligibilityGateV1.evaluate(
                candidateReference = candidate,
                candidateDataset = dataset,
                currentCandidateDatasetDigest = digest,
                validationLedger = ledger,
            )

        assertEquals(
            HimCandidatePromotionEligibilityStatus.PROMOTION_ELIGIBLE,
            result.status,
        )

        assertTrue(
            result.promotionEligible
        )

        assertTrue(
            result.validationReference != null
        )
    }

    @Test
    fun `candidate without validation is blocked`() {
        val candidate =
            candidateReference("2")

        val result =
            HimCandidatePromotionEligibilityGateV1.evaluate(
                candidateReference = candidate,
                candidateDataset =
                    dataset(candidate),
                currentCandidateDatasetDigest =
                    HimSha256("b".repeat(64)),
                validationLedger =
                    HimCandidateValidationLedgerV1(),
            )

        assertEquals(
            HimCandidatePromotionEligibilityStatus
                .BLOCKED_VALIDATION_MISSING,
            result.status,
        )

        assertFalse(
            result.promotionEligible
        )
    }

    @Test
    fun `rejected candidate is blocked`() {
        val candidate =
            candidateReference("3")

        val digest =
            HimSha256("c".repeat(64))

        val validation =
            validation(
                candidate = candidate,
                digest = digest,
                decision =
                    HimCandidateValidationDecision.REJECT,
                reason =
                    HimCandidateValidationReason
                        .SEMANTICALLY_INCORRECT,
                rationale =
                    "Candidate is semantically incorrect.",
            )

        val result =
            HimCandidatePromotionEligibilityGateV1.evaluate(
                candidateReference = candidate,
                candidateDataset =
                    dataset(candidate),
                currentCandidateDatasetDigest =
                    digest,
                validationLedger =
                    ledger(validation),
            )

        assertEquals(
            HimCandidatePromotionEligibilityStatus
                .BLOCKED_REJECTED,
            result.status,
        )

        assertFalse(
            result.promotionEligible
        )
    }

    @Test
    fun `deferred candidate is blocked`() {
        val candidate =
            candidateReference("4")

        val digest =
            HimSha256("d".repeat(64))

        val validation =
            validation(
                candidate = candidate,
                digest = digest,
                decision =
                    HimCandidateValidationDecision.DEFER,
                reason =
                    HimCandidateValidationReason
                        .REQUIRES_FURTHER_REVIEW,
                rationale =
                    "Candidate requires further review.",
            )

        val result =
            HimCandidatePromotionEligibilityGateV1.evaluate(
                candidateReference = candidate,
                candidateDataset =
                    dataset(candidate),
                currentCandidateDatasetDigest =
                    digest,
                validationLedger =
                    ledger(validation),
            )

        assertEquals(
            HimCandidatePromotionEligibilityStatus
                .BLOCKED_DEFERRED,
            result.status,
        )

        assertFalse(
            result.promotionEligible
        )
    }

    @Test
    fun `validation bound to different dataset digest is blocked`() {
        val candidate =
            candidateReference("5")

        val validationDigest =
            HimSha256("e".repeat(64))

        val currentDigest =
            HimSha256("f".repeat(64))

        val validation =
            approved(
                candidate = candidate,
                digest = validationDigest,
            )

        val result =
            HimCandidatePromotionEligibilityGateV1.evaluate(
                candidateReference = candidate,
                candidateDataset =
                    dataset(candidate),
                currentCandidateDatasetDigest =
                    currentDigest,
                validationLedger =
                    ledger(validation),
            )

        assertEquals(
            HimCandidatePromotionEligibilityStatus
                .BLOCKED_DATASET_MISMATCH,
            result.status,
        )

        assertFalse(
            result.promotionEligible
        )
    }

    @Test
    fun `validation cannot authorize candidate missing from dataset`() {
        val candidate =
            candidateReference("6")

        val digest =
            HimSha256("1".repeat(64))

        val validation =
            approved(
                candidate = candidate,
                digest = digest,
            )

        val result =
            HimCandidatePromotionEligibilityGateV1.evaluate(
                candidateReference = candidate,
                candidateDataset =
                    HimCandidateDataset(),
                currentCandidateDatasetDigest =
                    digest,
                validationLedger =
                    ledger(validation),
            )

        assertEquals(
            HimCandidatePromotionEligibilityStatus
                .BLOCKED_CANDIDATE_MISSING,
            result.status,
        )

        assertFalse(
            result.promotionEligible
        )
    }

    @Test
    fun `require eligible fails closed for non approved candidate`() {
        val candidate =
            candidateReference("7")

        val digest =
            HimSha256("2".repeat(64))

        val failure =
            runCatching {
                HimCandidatePromotionEligibilityGateV1.requireEligible(
                    candidateReference =
                        candidate,
                    candidateDataset =
                        dataset(candidate),
                    currentCandidateDatasetDigest =
                        digest,
                    validationLedger =
                        HimCandidateValidationLedgerV1(),
                )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )

        assertEquals(
            "Candidate is not promotion eligible: BLOCKED_VALIDATION_MISSING",
            failure?.message,
        )
    }

    private fun approved(
        candidate: HimCandidateReference,
        digest: HimSha256,
    ): HimCandidateValidationRecord =
        validation(
            candidate = candidate,
            digest = digest,
            decision =
                HimCandidateValidationDecision.APPROVE,
            reason =
                HimCandidateValidationReason
                    .SEMANTICALLY_CORRECT,
            rationale =
                "Candidate is semantically correct.",
        )

    private fun validation(
        candidate: HimCandidateReference,
        digest: HimSha256,
        decision: HimCandidateValidationDecision,
        reason: HimCandidateValidationReason,
        rationale: String,
    ): HimCandidateValidationRecord {
        val validationReference =
            HimCandidateValidationIdentityV1.validation(
                candidateReference =
                    candidate,
                candidateDatasetDigest =
                    digest,
                decision =
                    decision,
                reason =
                    reason,
                supersededByCandidateReference =
                    null,
            )

        return HimCandidateValidationRecord(
            validationReference =
                validationReference,
            candidateReference =
                candidate,
            candidateDatasetDigest =
                digest,
            decision =
                decision,
            reason =
                reason,
            rationale =
                rationale,
        )
    }

    private fun ledger(
        validation: HimCandidateValidationRecord,
    ): HimCandidateValidationLedgerV1 =
        HimCandidateValidationLedgerV1(
            validations =
                listOf(validation)
        )

    private fun dataset(
        candidateReference: HimCandidateReference,
    ): HimCandidateDataset {
        val candidate =
            HimCandidateHypothesis(
                candidateReference =
                    candidateReference,
                candidateTerm =
                    "Hering eingelegt",
                normalizedCandidateTerm =
                    "hering eingelegt",
                relation =
                    HimCandidateRelation.Variant(
                        HimFamilyEntityReference.Canonical(
                            canonicalId =
                                HimEntityId("OzlByp")
                        )
                    ),
                confidence =
                    HimCandidateConfidence.HIGH,
                evidenceOrigin =
                    HimSemanticEvidenceOrigin.MIXED,
                shortRationale =
                    "Preservation state of Hering.",
            )

        return HimCandidateDataset(
            candidates =
                listOf(
                    HimCandidateMasterRecord(
                        candidate =
                            candidate,
                        occurrences =
                            emptyList(),
                    )
                )
        )
    }

    private fun candidateReference(
        digit: String,
    ): HimCandidateReference =
        HimCandidateReference(
            "candidate:v1:" +
                    digit.repeat(64)
        )
}