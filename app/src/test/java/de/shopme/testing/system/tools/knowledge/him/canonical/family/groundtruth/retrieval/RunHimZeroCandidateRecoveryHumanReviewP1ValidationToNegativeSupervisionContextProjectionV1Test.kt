package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.EvidenceBinding
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.FailureReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.Result
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1Test {
    @Test
    fun validProjectionProducesProvenProofWithAllLineage() {
        val proof = completed(Fixture.request())
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.CONTRACT_ID, proof.contractId)
        assertEquals("PROVEN", proof.state)
        assertEquals(Fixture.record.recordId, proof.negativeSupervisionRecordId)
        assertEquals(Fixture.positive.exampleReference, proof.positiveTrainingExampleReference)
        assertEquals(Fixture.rejectedTarget, proof.rejectedTarget)
        assertEquals(Fixture.boundaryType, proof.boundaryType)
        assertEquals(Fixture.evidenceBindings, proof.evidenceBindings)
        assertEquals(Fixture.validationRecord.validationRecordId, proof.validationRecordId)
        assertEquals(Fixture.reviewUnit.reviewUnitId, proof.reviewUnitId)
    }

    @Test
    fun bindingAndLogicalDigestsAreDeterministicAndDistinct() {
        val first = completed(Fixture.request())
        val second = completed(Fixture.request())
        assertEquals(first.bindingDigest, second.bindingDigest)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertNotEquals(first.bindingDigest, first.logicalDigest)
        assertEquals(first.logicalDigest, HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.logicalDigest(first))
    }

    @Test
    fun negativeRecordBindingKeyMustMatch() = assertReason(
        Fixture.request(referenceBinding = Fixture.binding.copy(bindingKey = "b".repeat(64))),
        FailureReason.INVALID_NEGATIVE_SUPERVISION_CONTEXT,
    )

    @Test
    fun validationRecordMismatchFailsClosed() = assertReason(
        Fixture.request(validationRecord = Fixture.validationRecord.copy(validationRecordId = "b".repeat(64))),
        FailureReason.VALIDATION_LINEAGE_MISMATCH,
    )

    @Test
    fun reviewUnitMismatchFailsClosed() = assertReason(
        Fixture.request(reviewUnit = HimZeroCandidateRecoveryHumanReviewReviewUnitV1("f".repeat(64), "AbCd12")),
        FailureReason.REVIEW_LINEAGE_MISMATCH,
    )

    @Test
    fun stableEntryMismatchFailsClosed() = assertReason(
        Fixture.request(reviewUnit = HimZeroCandidateRecoveryHumanReviewReviewUnitV1("f".repeat(64), "AbCd12")),
        FailureReason.REVIEW_LINEAGE_MISMATCH,
    )

    @Test
    fun canonicalLineageMismatchFailsClosed() = assertReason(
        Fixture.request(validationRecord = Fixture.validationRecord.copy(originalDecision = Fixture.selection.copy(canonicalEntityId = "ZzYyXx"))),
        FailureReason.VALIDATION_LINEAGE_MISMATCH,
    )

    @Test
    fun positiveOccurrenceMismatchFailsClosed() = assertReason(
        Fixture.request(materializationDecision = Fixture.materialization.copy(occurrenceReference = HimCandidateOccurrenceReference(Fixture.otherOccurrence))),
        FailureReason.MATERIALIZATION_LINEAGE_MISMATCH,
    )

    @Test
    fun referenceBindingMismatchFailsClosed() = assertReason(
        Fixture.request(referenceBinding = Fixture.binding.copy(materializationDecisionId = "f".repeat(64))),
        FailureReason.POSITIVE_REFERENCE_MISMATCH,
    )

    @Test
    fun materializationDecisionMismatchFailsClosed() = assertReason(
        Fixture.request(materializationDecision = Fixture.materialization.copy(decisionId = "f".repeat(64))),
        FailureReason.POSITIVE_REFERENCE_MISMATCH,
    )

    @Test
    fun trainingExampleReferenceMismatchFailsClosed() = assertReason(
        Fixture.request(referenceBinding = Fixture.binding.copy(trainingExampleReference = HimTrainingExampleReference("example:v1:${"f".repeat(64)}"))),
        FailureReason.POSITIVE_REFERENCE_MISMATCH,
    )

    @Test
    fun canonicalEqualityWithoutCanonicalContextCannotProveIdentity() = assertReason(
        Fixture.request(positiveTrainingExample = Fixture.positiveWithoutCanonicalContext),
        FailureReason.INPUT_IDENTITY_NOT_PROVABLE,
    )

    @Test
    fun positiveReferenceMustBeMaterializedFromSameExample() = assertReason(
        Fixture.request(positiveTrainingExample = Fixture.otherPositive),
        FailureReason.MATERIALIZATION_LINEAGE_MISMATCH,
    )

    @Test
    fun rejectedTargetEqualToPositiveFailsClosed() = assertReason(
        Fixture.request(rejectedTarget = Fixture.positive.target),
        FailureReason.REJECTED_TARGET_INVALID_OR_UNBOUND,
    )

    @Test
    fun rejectedTargetWithBoundCanonicalIsRejected() = assertReason(
        Fixture.request(rejectedTarget = HimTrainingTargetV1.Identity(Fixture.canonicalId)),
        FailureReason.REJECTED_TARGET_INVALID_OR_UNBOUND,
    )

    @Test
    fun explicitBoundaryMustBeAdmissible() = assertReason(
        Fixture.request(boundaryType = HimNegativeBoundaryTypeV1.WRONG_SCOPE),
        FailureReason.INVALID_BOUNDARY_TYPE,
    )

    @Test
    fun boundaryIsNotInferred() = assertReason(
        Fixture.request(boundaryType = HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION),
        FailureReason.INVALID_BOUNDARY_TYPE,
    )

    @Test
    fun completeOneToOneEvidenceBindingIsAccepted() {
        assertEquals(1, completed(Fixture.request()).evidenceBindings.size)
    }

    @Test
    fun missingEvidenceBindingFailsClosed() = assertReason(
        Fixture.request(evidenceBindings = emptyList()),
        FailureReason.MISSING_EVIDENCE_BINDING,
    )

    @Test
    fun additionalEvidenceBindingFailsClosed() = assertReason(
        Fixture.request(evidenceBindings = Fixture.evidenceBindings + EvidenceBinding("extra", Fixture.evidence)),
        FailureReason.EXTRA_EVIDENCE_BINDING,
    )

    @Test
    fun duplicateNegativeEvidenceIdFailsClosed() = assertReason(
        Fixture.request(evidenceBindings = listOf(Fixture.evidenceBinding, Fixture.evidenceBinding.copy(evidence = Fixture.otherEvidence))),
        FailureReason.DUPLICATE_EVIDENCE_BINDING,
    )

    @Test
    fun evidenceNotPresentInPositiveExampleFailsClosed() = assertReason(
        Fixture.request(evidenceBindings = listOf(EvidenceBinding("negative-evidence-1", Fixture.otherEvidence))),
        FailureReason.EVIDENCE_REFERENCE_MISMATCH,
    )

    @Test
    fun wrongSourceFailsClosed() = assertReason(
        Fixture.request(evidenceBindings = listOf(EvidenceBinding("negative-evidence-1", Fixture.evidence.copy(source = "AGRIBALYSE")))),
        FailureReason.EVIDENCE_REFERENCE_MISMATCH,
    )

    @Test
    fun wrongArtifactDigestFailsClosed() = assertReason(
        Fixture.request(evidenceBindings = listOf(EvidenceBinding("negative-evidence-1", Fixture.evidence.copy(sourceArtifactSha256 = HimSha256("f".repeat(64)))))),
        FailureReason.EVIDENCE_REFERENCE_MISMATCH,
    )

    @Test
    fun wrongSourceRecordIdentityFailsClosed() = assertReason(
        Fixture.request(evidenceBindings = listOf(EvidenceBinding("negative-evidence-1", Fixture.evidence.copy(sourceRecordIdentity = "other-record")))),
        FailureReason.EVIDENCE_REFERENCE_MISMATCH,
    )

    @Test
    fun evidenceOrderingIsCanonical() {
        val proof = completed(Fixture.request())
        assertEquals(proof.evidenceBindings.sortedBy { it.negativeEvidenceReferenceId }, proof.evidenceBindings)
    }

    @Test
    fun noPositionalOrFuzzyEvidenceMatchingIsUsed() = assertReason(
        Fixture.request(evidenceBindings = listOf(EvidenceBinding("different-id", Fixture.evidence))),
        FailureReason.EXTRA_EVIDENCE_BINDING,
    )

    @Test
    fun validationAndReviewLineageAreExplicitlyCopied() {
        val proof = completed(Fixture.request())
        assertEquals(Fixture.record.validatorReviewerRef, proof.validatorReviewerRef)
        assertEquals(Fixture.record.validationRound, proof.validationRound)
        assertEquals(Fixture.record.validationRevision, proof.validationRevision)
        assertEquals(Fixture.record.stableEntryId, proof.stableEntryId)
    }

    @Test
    fun serializeAndDeserializeAreByteStable() {
        val proof = completed(Fixture.request())
        val bytes = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.serializeRecord(proof)
        assertContentEquals(bytes, HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.serializeRecord(
            HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.deserializeRecord(bytes),
        ))
        assertEquals(proof, HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.deserializeRecord(bytes))
    }

    @Test
    fun persistenceCreatesAndReadsExactRecord() = withTempRoot { root ->
        val result = persist(Fixture.request(), root)
        val completed = assertIs<HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceResult.Completed>(result)
        assertEquals(PersistenceStatus.CREATED, completed.status)
        assertEquals(completed.record, HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.readByNegativeSupervisionRecordId(Fixture.record.recordId, root))
    }

    @Test
    fun persistenceIsIdempotentWithoutRewrite() = withTempRoot { root ->
        val first = assertIs<HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceResult.Completed>(persist(Fixture.request(), root))
        val bytes = Files.readAllBytes(first.path.toPath())
        val second = assertIs<HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceResult.Completed>(persist(Fixture.request(), root))
        assertEquals(PersistenceStatus.ALREADY_PRESENT_IDENTICAL, second.status)
        assertContentEquals(bytes, Files.readAllBytes(first.path.toPath()))
    }

    @Test
    fun changedRejectedTargetConflicts() = withTempRoot { root ->
        persist(Fixture.request(), root)
        val changed = Fixture.proof().copy(rejectedTarget = HimTrainingTargetV1.NewCanonical("different"))
            .let { it.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.bindingDigest(it)) }
            .let { it.copy(logicalDigest = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.logicalDigest(it)) }
        assertEquals(FailureReason.EXISTING_ARTIFACT_CONFLICT, assertIs<HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceResult.Failed>(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.persist(changed, root)).reason)
    }

    @Test
    fun changedBoundaryConflicts() = withTempRoot { root ->
        persist(Fixture.request(), root)
        val changed = Fixture.proof().copy(boundaryType = HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY)
            .let { it.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.bindingDigest(it)) }
            .let { it.copy(logicalDigest = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.logicalDigest(it)) }
        assertEquals(FailureReason.EXISTING_ARTIFACT_CONFLICT, assertIs<HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceResult.Failed>(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.persist(changed, root)).reason)
    }

    @Test
    fun changedEvidenceConflicts() = withTempRoot { root ->
        persist(Fixture.request(), root)
        val changed = Fixture.proof().copy(evidenceBindings = listOf(EvidenceBinding("negative-evidence-1", Fixture.otherEvidence)))
            .let { it.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.bindingDigest(it)) }
            .let { it.copy(logicalDigest = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.logicalDigest(it)) }
        assertEquals(FailureReason.EXISTING_ARTIFACT_CONFLICT, assertIs<HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceResult.Failed>(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.persist(changed, root)).reason)
    }

    @Test
    fun malformedAndTruncatedArtifactsFailClosed() = withTempRoot { root ->
        val path = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.pathFor(Fixture.record.recordId, root).toPath()
        Files.createDirectories(path.parent)
        Files.write(path, "{}\n".toByteArray())
        assertFailsWith<IllegalArgumentException> {
            HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.readByNegativeSupervisionRecordId(Fixture.record.recordId, root)
        }
        Files.write(path, byteArrayOf('{'.code.toByte()))
        assertFailsWith<IllegalArgumentException> {
            HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.readByNegativeSupervisionRecordId(Fixture.record.recordId, root)
        }
    }

    @Test
    fun bindingDigestTamperingFailsClosed() = withTempRoot { root ->
        val proof = Fixture.proof()
        val tampered = proof.copy(bindingDigest = "0".repeat(64))
        assertEquals(FailureReason.BINDING_DIGEST_MISMATCH, assertIs<HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceResult.Failed>(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.persist(tampered, root)).reason)
    }

    @Test
    fun logicalDigestTamperingFailsClosed() = withTempRoot { root ->
        val proof = Fixture.proof()
        val tampered = proof.copy(logicalDigest = "0".repeat(64))
        assertEquals(FailureReason.LOGICAL_DIGEST_MISMATCH, assertIs<HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceResult.Failed>(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.persist(tampered, root)).reason)
    }

    @Test
    fun unknownFieldFailsClosed() = withTempRoot { root ->
        val proof = Fixture.proof()
        val bytes = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.serializeRecord(proof)
        val changed = bytes.toString(Charsets.UTF_8).replaceFirst("{", "{\"unknown\":1,", false).toByteArray()
        val path = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.pathFor(proof.negativeSupervisionRecordId, root).toPath()
        Files.createDirectories(path.parent)
        Files.write(path, changed)
        assertFailsWith<IllegalArgumentException> { HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.readByNegativeSupervisionRecordId(proof.negativeSupervisionRecordId, root) }
    }

    @Test
    fun duplicateJsonFieldFailsClosed() = withTempRoot { root ->
        val proof = Fixture.proof()
        val json = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.serializeRecord(proof).toString(Charsets.UTF_8)
        val changed = json.replaceFirst("{", "{\"contractId\":\"duplicate\",", false).toByteArray()
        val path = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.pathFor(proof.negativeSupervisionRecordId, root).toPath()
        Files.createDirectories(path.parent)
        Files.write(path, changed)
        assertFailsWith<IllegalArgumentException> { HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.readByNegativeSupervisionRecordId(proof.negativeSupervisionRecordId, root) }
    }

    @Test
    fun wrongContractVersionAndStateFailClosed() {
        val proof = Fixture.proof()
        assertFailsWith<IllegalArgumentException> { HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.validateRecord(proof.copy(contractId = "wrong")) }
        assertFailsWith<IllegalArgumentException> { HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.validateRecord(proof.copy(version = "2")) }
        assertFailsWith<IllegalArgumentException> { HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.validateRecord(proof.copy(state = "PENDING")) }
    }

    @Test
    fun manyToOneKeepsNegativeProofFilesDistinct() = withTempRoot { root ->
        val first = Fixture.proof()
        val second = first.copy(negativeSupervisionRecordId = "f".repeat(64), identityProof = first.identityProof.copy(negativeSupervisionRecordId = "f".repeat(64)))
            .let { it.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.bindingDigest(it)) }
            .let { it.copy(logicalDigest = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.logicalDigest(it)) }
        assertIs<HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceResult.Completed>(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.persist(first, root))
        assertIs<HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceResult.Completed>(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.persist(second, root))
        assertTrue(Files.exists(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.pathFor(first.negativeSupervisionRecordId, root).toPath()))
        assertTrue(Files.exists(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.pathFor(second.negativeSupervisionRecordId, root).toPath()))
        assertEquals(first.positiveTrainingExampleReference, second.positiveTrainingExampleReference)
    }

    private fun completed(request: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.Request): ProofRecord =
        assertIs<Result.Completed>(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.evaluate(request)).value

    private fun assertReason(
        request: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.Request,
        reason: FailureReason,
    ) {
        val failure = assertIs<Result.Failed>(HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.evaluate(request))
        assertEquals(reason, failure.reason)
    }

    private fun persist(request: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.Request, root: java.io.File) =
        HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.PersistenceRequest(request, root),
        )

    private fun withTempRoot(block: (java.io.File) -> Unit) {
        val root = createTempDirectory("him-context-proof-").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }

    private object Fixture {
        val canonicalId = HimEntityId("AbCd12")
        val stableEntryId = "a".repeat(64)
        val reviewUnit = HimZeroCandidateRecoveryHumanReviewReviewUnitV1(stableEntryId, canonicalId.value)
        val occurrence = "occurrence:v1:${"b".repeat(64)}"
        val otherOccurrence = "occurrence:v1:${"c".repeat(64)}"
        val evidence = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("d".repeat(64)), "off:product:row:1")
        val otherEvidence = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("e".repeat(64)), "off:product:row:2")
        val evidenceBinding = EvidenceBinding("negative-evidence-1", evidence)
        val evidenceBindings = listOf(evidenceBinding)
        val positive = HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = HimTrainingInputV1(
                observedTerm = "unknown food",
                normalizedObservedTerm = "unknown food",
                canonicalContext = listOf(de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(1, canonicalId, "Canonical", null)),
                evidence = listOf(HimTrainingEvidenceInputV1(evidence, "FOOD", 1)),
            ),
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(canonicalId)),
            provenance = HimTrainingProvenanceV1(sourceEvidenceReferences = listOf(evidence)),
        )
        val otherPositive = HimTrainingExampleV1.create(
            taskType = positive.taskType,
            input = positive.input.copy(observedTerm = "other food"),
            target = positive.target,
            provenance = positive.provenance,
        )
        val positiveWithoutCanonicalContext = HimTrainingExampleV1.create(
            taskType = positive.taskType,
            input = positive.input.copy(canonicalContext = emptyList()),
            target = positive.target,
            provenance = positive.provenance,
        )
        val rejectedTarget = HimTrainingTargetV1.NewCanonical("new canonical")
        val boundaryType = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY
        val selection = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1(
            reviewUnitId = reviewUnit.reviewUnitId,
            stableEntryId = stableEntryId,
            canonicalEntityId = canonicalId.value,
            decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            reasonCodes = listOf(de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED),
            evidenceReferenceIds = listOf(evidenceBinding.negativeEvidenceReferenceId),
            revision = 1,
            alternativeCanonicalProposal = null,
            reviewerNote = null,
        )
        val validationRecord = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1(
            validationRecordId = "e".repeat(64),
            originalDecision = selection,
            validatorReviewerRef = "reviewer:validator",
            validationRound = 1,
            validationRevision = 1,
            assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
            reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION),
            evidenceReferenceIds = listOf(evidenceBinding.negativeEvidenceReferenceId),
            rationale = "The direct evidence contradicts the association.",
        )
        val record = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record(
            recordId = "f".repeat(64),
            negativeCandidateId = "a".repeat(64),
            readinessDecisionId = "b".repeat(64),
            readinessState = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
            eligibilityBatchId = "eligibility-batch",
            eligibilityInputBindingDigest = "c".repeat(64),
            eligibilityDecisionId = "c".repeat(64),
            eligibilityState = HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE,
            validationBatchId = "validation-batch",
            validationBatchBindingDigest = "d".repeat(64),
            validationBatchLogicalDigest = "e".repeat(64),
            validationRecordId = validationRecord.validationRecordId,
            reviewUnitId = reviewUnit.reviewUnitId,
            stableEntryId = stableEntryId,
            canonicalEntityId = canonicalId.value,
            originalReviewerRef = "reviewer:original",
            validatorReviewerRef = validationRecord.validatorReviewerRef,
            validationRound = 1,
            validationRevision = 1,
            originalDecision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
            validationReasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION),
            evidenceReferenceIds = listOf(evidenceBinding.negativeEvidenceReferenceId),
            downstreamRoute = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE,
            candidateState = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE,
        )
        val materialization = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationDecision(
            decisionId = "1".repeat(64),
            preMaterializationIdentity = de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrenceReference(occurrence),
            occurrenceReference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrenceReference(occurrence),
            projectionInputBindingDigest = HimSha256("2".repeat(64)),
            state = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationState.PROJECTED,
            reasons = emptyList(),
            projectedTrainingExample = positive,
            trainingExampleReference = positive.exampleReference,
        )
        val binding = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1.BindingRecord(
            contractId = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1.CONTRACT_ID,
            version = "1",
            state = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1.BindingState.BOUND,
            bindingKey = record.recordId,
            negativeSupervisionRecordId = record.recordId,
            materializationDecisionId = materialization.decisionId,
            trainingExampleReference = positive.exampleReference,
            bindingDecisionId = "3".repeat(64),
            reasons = emptyList(),
            logicalDigest = "4".repeat(64),
        )
        val request = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.Request(
            negativeSupervisionRecord = record,
            validationRecord = validationRecord,
            reviewUnit = reviewUnit,
            positiveTrainingExample = positive,
            materializationDecision = materialization,
            referenceBinding = binding,
            rejectedTarget = rejectedTarget,
            boundaryType = boundaryType,
            evidenceBindings = evidenceBindings,
        )

        fun request(
            referenceBinding: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1.BindingRecord = binding,
            validationRecord: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1 = this.validationRecord,
            reviewUnit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1 = this.reviewUnit,
            positiveTrainingExample: HimTrainingExampleV1 = positive,
            materializationDecision: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationDecision = materialization,
            rejectedTarget: HimTrainingTargetV1 = this.rejectedTarget,
            boundaryType: HimNegativeBoundaryTypeV1 = this.boundaryType,
            evidenceBindings: List<EvidenceBinding> = this.evidenceBindings,
        ) = this.request.copy(referenceBinding = referenceBinding, validationRecord = validationRecord, reviewUnit = reviewUnit, positiveTrainingExample = positiveTrainingExample, materializationDecision = materializationDecision, rejectedTarget = rejectedTarget, boundaryType = boundaryType, evidenceBindings = evidenceBindings)

        fun proof() = when (val result = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.evaluate(request)) {
            is Result.Completed -> result.value
            is Result.Failed -> error(result.reason)
        }
    }
}
