package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.validation

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecision
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationLedgerPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationLedgerV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationRecord
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class RunHimCandidateValidationLedgerPersistenceV1Test {

    @Test
    fun `validation ledger persists reloads and replays deterministically`() {
        val digest =
            HimSha256(
                "a".repeat(64)
            )

        val first =
            approved(
                candidate = candidate("1"),
                digest = digest,
            )

        val second =
            rejected(
                candidate = candidate("2"),
                replacement = candidate("3"),
                digest = digest,
            )

        val initial =
            HimCandidateValidationLedgerV1()

        val afterFirst =
            HimCandidateValidationLedgerPersistenceV1
                .addValidation(
                    ledger = initial,
                    validation = first,
                )

        val combined =
            HimCandidateValidationLedgerPersistenceV1
                .addValidation(
                    ledger = afterFirst,
                    validation = second,
                )

        assertEquals(
            2,
            combined.validations.size,
        )

        assertEquals(
            combined.validations
                .map {
                    it.validationReference.value
                }
                .sorted(),
            combined.validations
                .map {
                    it.validationReference.value
                },
        )

        val serialized =
            HimCandidateValidationLedgerPersistenceV1
                .serialize(combined)

        val temporaryDirectory =
            Files.createTempDirectory(
                "him-validation-ledger-v1-"
            ).toFile()

        try {
            val ledgerFile =
                temporaryDirectory.resolve(
                    "candidate-validation-ledger.v1.json"
                )

            HimCandidateValidationLedgerPersistenceV1
                .write(
                    file = ledgerFile,
                    ledger = combined,
                )

            assertTrue(
                ledgerFile.isFile
            )

            assertArrayEquals(
                serialized,
                ledgerFile.readBytes(),
            )

            val reloaded =
                HimCandidateValidationLedgerPersistenceV1
                    .read(ledgerFile)

            assertEquals(
                combined,
                reloaded,
            )

            val serializedReload =
                HimCandidateValidationLedgerPersistenceV1
                    .serialize(reloaded)

            assertArrayEquals(
                serialized,
                serializedReload,
            )

            val replayed =
                HimCandidateValidationLedgerPersistenceV1
                    .addValidation(
                        ledger = reloaded,
                        validation = first,
                    )

            assertEquals(
                reloaded,
                replayed,
            )

            assertArrayEquals(
                serialized,
                HimCandidateValidationLedgerPersistenceV1
                    .serialize(replayed),
            )
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    @Test
    fun `candidate validation is immutable once recorded`() {
        val candidate =
            candidate("4")

        val digest =
            HimSha256(
                "b".repeat(64)
            )

        val approval =
            approved(
                candidate = candidate,
                digest = digest,
            )

        val ledger =
            HimCandidateValidationLedgerPersistenceV1
                .addValidation(
                    ledger =
                        HimCandidateValidationLedgerV1(),
                    validation =
                        approval,
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
                    "Later incompatible decision.",
            )

        val failure =
            runCatching {
                HimCandidateValidationLedgerPersistenceV1
                    .addValidation(
                        ledger = ledger,
                        validation = deferred,
                    )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )

        assertTrue(
            failure
                ?.message
                .orEmpty()
                .contains(
                    "Immutable Candidate validation already exists"
                )
        )
    }

    @Test
    fun `validation reference collision across different candidates is rejected`() {
        val digest =
            HimSha256(
                "c".repeat(64)
            )

        val first =
            approved(
                candidate = candidate("5"),
                digest = digest,
            )

        val conflicting =
            approved(
                candidate = candidate("6"),
                digest = digest,
            ).copy(
                validationReference =
                    first.validationReference
            )

        val ledger =
            HimCandidateValidationLedgerPersistenceV1
                .addValidation(
                    ledger =
                        HimCandidateValidationLedgerV1(),
                    validation =
                        first,
                )

        val failure =
            runCatching {
                HimCandidateValidationLedgerPersistenceV1
                    .addValidation(
                        ledger = ledger,
                        validation = conflicting,
                    )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )

        assertEquals(
            "Validation reference collision.",
            failure?.message,
        )
    }

    @Test
    fun `missing ledger file loads as empty ledger`() {
        val temporaryDirectory =
            Files.createTempDirectory(
                "him-validation-ledger-empty-"
            ).toFile()

        try {
            val file =
                temporaryDirectory.resolve(
                    "missing-ledger.json"
                )

            val ledger =
                HimCandidateValidationLedgerPersistenceV1
                    .read(file)

            assertTrue(
                ledger.validations.isEmpty()
            )
        } finally {
            temporaryDirectory.deleteRecursively()
        }
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

    private fun rejected(
        candidate: HimCandidateReference,
        replacement: HimCandidateReference,
        digest: HimSha256,
    ): HimCandidateValidationRecord {
        val reference =
            HimCandidateValidationIdentityV1.validation(
                candidateReference =
                    candidate,
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

        return HimCandidateValidationRecord(
            validationReference =
                reference,
            candidateReference =
                candidate,
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
                "Candidate was superseded by a better semantic candidate.",
        )
    }

    private fun candidate(
        digit: String,
    ): HimCandidateReference =
        HimCandidateReference(
            "candidate:v1:" +
                    digit.repeat(64)
        )
}