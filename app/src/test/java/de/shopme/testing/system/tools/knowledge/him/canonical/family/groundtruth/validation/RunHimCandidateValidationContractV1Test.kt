package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.validation

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecision
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationLedgerV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimCandidateValidationContractV1Test {

    @Test
    fun `validation references are deterministic and dataset bound`() {
        val candidate =
            candidate("1")

        val digestA =
            HimSha256("a".repeat(64))

        val digestB =
            HimSha256("b".repeat(64))

        val first =
            HimCandidateValidationIdentityV1.validation(
                candidateReference = candidate,
                candidateDatasetDigest = digestA,
                decision =
                    HimCandidateValidationDecision.APPROVE,
                reason =
                    HimCandidateValidationReason.SEMANTICALLY_CORRECT,
                supersededByCandidateReference = null,
            )

        val replay =
            HimCandidateValidationIdentityV1.validation(
                candidateReference = candidate,
                candidateDatasetDigest = digestA,
                decision =
                    HimCandidateValidationDecision.APPROVE,
                reason =
                    HimCandidateValidationReason.SEMANTICALLY_CORRECT,
                supersededByCandidateReference = null,
            )

        val otherDataset =
            HimCandidateValidationIdentityV1.validation(
                candidateReference = candidate,
                candidateDatasetDigest = digestB,
                decision =
                    HimCandidateValidationDecision.APPROVE,
                reason =
                    HimCandidateValidationReason.SEMANTICALLY_CORRECT,
                supersededByCandidateReference = null,
            )

        assertEquals(first, replay)
        assertNotEquals(
            first,
            otherDataset,
        )
    }

    @Test
    fun `superseded historical candidate may be rejected in favor of better candidate`() {
        val historical =
            candidate("2")

        val replacement =
            candidate("3")

        val digest =
            HimSha256("c".repeat(64))

        val reference =
            HimCandidateValidationIdentityV1.validation(
                candidateReference =
                    historical,
                candidateDatasetDigest =
                    digest,
                decision =
                    HimCandidateValidationDecision.REJECT,
                reason =
                    HimCandidateValidationReason
                        .SUPERSEDED_BY_BETTER_CANDIDATE,
                supersededByCandidateReference =
                    replacement,
            )

        val record =
            HimCandidateValidationRecord(
                validationReference =
                    reference,
                candidateReference =
                    historical,
                candidateDatasetDigest =
                    digest,
                decision =
                    HimCandidateValidationDecision.REJECT,
                reason =
                    HimCandidateValidationReason
                        .SUPERSEDED_BY_BETTER_CANDIDATE,
                supersededByCandidateReference =
                    replacement,
                rationale =
                    "A later validated candidate preserves the existing canonical identity.",
            )

        assertEquals(
            historical,
            record.candidateReference,
        )
        assertEquals(
            replacement,
            record.supersededByCandidateReference,
        )
    }

    @Test
    fun `approval cannot carry supersession`() {
        val failure =
            runCatching {
                val candidate =
                    candidate("4")

                HimCandidateValidationRecord(
                    validationReference =
                        HimCandidateValidationIdentityV1.validation(
                            candidateReference =
                                candidate,
                            candidateDatasetDigest =
                                HimSha256(
                                    "d".repeat(64)
                                ),
                            decision =
                                HimCandidateValidationDecision.APPROVE,
                            reason =
                                HimCandidateValidationReason
                                    .SEMANTICALLY_CORRECT,
                            supersededByCandidateReference =
                                candidate("5"),
                        ),
                    candidateReference =
                        candidate,
                    candidateDatasetDigest =
                        HimSha256(
                            "d".repeat(64)
                        ),
                    decision =
                        HimCandidateValidationDecision.APPROVE,
                    reason =
                        HimCandidateValidationReason
                            .SEMANTICALLY_CORRECT,
                    supersededByCandidateReference =
                        candidate("5"),
                    rationale =
                        "Invalid approval.",
                )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    @Test
    fun `ledger permits one immutable validation per candidate`() {
        val digest =
            HimSha256("e".repeat(64))

        val firstCandidate =
            candidate("6")

        val secondCandidate =
            candidate("7")

        val first =
            approved(
                firstCandidate,
                digest,
            )

        val second =
            approved(
                secondCandidate,
                digest,
            )

        val ledger =
            HimCandidateValidationLedgerV1(
                validations =
                    listOf(
                        first,
                        second,
                    ).sortedBy {
                        it.validationReference.value
                    },
            )

        assertEquals(
            HimCandidateValidationContractV1
                .LEDGER_VERSION,
            ledger.contractVersion,
        )

        assertEquals(
            2,
            ledger.validations.size,
        )
    }

    @Test
    fun `same candidate cannot have two validation records in one ledger`() {
        val digest =
            HimSha256("f".repeat(64))

        val candidate =
            candidate("8")

        val approval =
            approved(
                candidate,
                digest,
            )

        val deferredReference =
            HimCandidateValidationIdentityV1.validation(
                candidateReference =
                    candidate,
                candidateDatasetDigest =
                    digest,
                decision =
                    HimCandidateValidationDecision.DEFER,
                reason =
                    HimCandidateValidationReason
                        .REQUIRES_FURTHER_REVIEW,
                supersededByCandidateReference =
                    null,
            )

        val deferred =
            HimCandidateValidationRecord(
                validationReference =
                    deferredReference,
                candidateReference =
                    candidate,
                candidateDatasetDigest =
                    digest,
                decision =
                    HimCandidateValidationDecision.DEFER,
                reason =
                    HimCandidateValidationReason
                        .REQUIRES_FURTHER_REVIEW,
                rationale =
                    "Further review required.",
            )

        val failure =
            runCatching {
                HimCandidateValidationLedgerV1(
                    validations =
                        listOf(
                            approval,
                            deferred,
                        ).sortedBy {
                            it.validationReference.value
                        },
                )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    private fun approved(
        candidate: HimCandidateReference,
        digest: HimSha256,
    ): HimCandidateValidationRecord {
        val reference =
            HimCandidateValidationIdentityV1.validation(
                candidateReference =
                    candidate,
                candidateDatasetDigest =
                    digest,
                decision =
                    HimCandidateValidationDecision.APPROVE,
                reason =
                    HimCandidateValidationReason
                        .SEMANTICALLY_CORRECT,
                supersededByCandidateReference =
                    null,
            )

        return HimCandidateValidationRecord(
            validationReference =
                reference,
            candidateReference =
                candidate,
            candidateDatasetDigest =
                digest,
            decision =
                HimCandidateValidationDecision.APPROVE,
            reason =
                HimCandidateValidationReason
                    .SEMANTICALLY_CORRECT,
            rationale =
                "Candidate is semantically correct.",
        )
    }

    private fun candidate(
        digit: String,
    ) =
        HimCandidateReference(
            "candidate:v1:" +
                    digit.repeat(64)
        )
}