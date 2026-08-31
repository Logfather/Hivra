package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.FailureReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.EvidenceRelationship
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.InputIdentity
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.Result
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.ResolutionState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.EvidenceBinding
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1Test {
    @Test
    fun validProvenContextProofResolves() {
        val resolved = completed(Fixture.request())
        assertEquals(ResolutionState.RESOLVED, resolved.bindingDecision.state)
        assertEquals(InputIdentity.PROVABLE, resolved.bindingDecision.inputIdentity)
        assertEquals(EvidenceRelationship.COMPATIBLE, resolved.bindingDecision.evidenceRelationship)
    }

    @Test
    fun resolvedResultBindsExactRecordPositiveAndProof() {
        val resolved = completed(Fixture.request())
        assertEquals(Fixture.record, resolved.negativeSupervisionRecord)
        assertEquals(Fixture.positive, resolved.positiveTrainingExample)
        assertEquals(Fixture.proof(), resolved.contextProof)
        assertEquals(Fixture.record.recordId, resolved.bindingDecision.recordId)
    }

    @Test
    fun resolvedOutputRetainsTargetBoundaryEvidenceAndProvenance() {
        val resolved = completed(Fixture.request())
        val decision = resolved.bindingDecision
        assertEquals(Fixture.rejectedTarget, decision.rejectedTarget)
        assertEquals(Fixture.boundaryType, decision.boundaryType)
        assertEquals(Fixture.positive.provenance, decision.trainingProvenance)
        assertEquals(1, decision.evidenceBindings.size)
        assertEquals(Fixture.evidence, decision.evidenceBindings.single().trainingEvidence.reference)
        assertEquals(Fixture.record.evidenceReferenceIds.single(), decision.evidenceBindings.single().persistedEvidenceReferenceId)
        assertEquals(Fixture.record, decision.trainingBinding.record)
    }

    @Test
    fun repeatedExecutionIsDeterministic() {
        val first = completed(Fixture.request())
        val second = completed(Fixture.request())
        assertEquals<HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.Resolved>(first, second)
        assertEquals<HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.BindingDecision>(first.bindingDecision, second.bindingDecision)
    }

    @Test
    fun manyToOneKeepsIndependentNegativeBindings() {
        val first = completed(Fixture.request())
        val secondRecord = Fixture.record.copy(
            recordId = "9".repeat(64),
            negativeCandidateId = "e".repeat(64),
            readinessDecisionId = "d".repeat(64),
            validationRecordId = "c".repeat(64),
        )
        val secondProof = Fixture.proof().copy(
            negativeSupervisionRecordId = secondRecord.recordId,
            negativeCandidateId = secondRecord.negativeCandidateId,
            readinessDecisionId = secondRecord.readinessDecisionId,
            validationRecordId = secondRecord.validationRecordId,
            identityProof = Fixture.proof().identityProof.copy(
                negativeSupervisionRecordId = secondRecord.recordId,
                validationRecordId = secondRecord.validationRecordId,
            ),
        )
        val second = completed(Fixture.request(record = secondRecord, proof = secondProof))
        assertEquals(Fixture.positive.exampleReference, first.positiveTrainingExample.exampleReference)
        assertEquals(Fixture.positive.exampleReference, second.positiveTrainingExample.exampleReference)
        assertNotEquals<String>(first.bindingDecision.recordId, second.bindingDecision.recordId)
    }

    @Test
    fun invalidNegativeRecordFailsClosed() = assertReason(
        Fixture.request(record = Fixture.record.copy(candidateState = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NOT_APPLICABLE_NON_NEGATIVE)),
        FailureReason.INVALID_NEGATIVE_SUPERVISION_RECORD,
    )

    @Test
    fun invalidContextProofFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(state = "INVALID")),
        FailureReason.INVALID_CONTEXT_PROOF,
    )

    @Test
    fun proofNegativeRecordIdMismatchFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(negativeSupervisionRecordId = "b".repeat(64))),
        FailureReason.CONTEXT_PROOF_RECORD_MISMATCH,
    )

    @Test
    fun proofNegativeCandidateIdMismatchFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(negativeCandidateId = "b".repeat(64))),
        FailureReason.CONTEXT_PROOF_RECORD_MISMATCH,
    )

    @Test
    fun proofReadinessLineageMismatchFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(readinessDecisionId = "8".repeat(64))),
        FailureReason.CONTEXT_PROOF_RECORD_MISMATCH,
    )

    @Test
    fun proofEligibilityLineageMismatchFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(eligibilityDecisionId = "b".repeat(64))),
        FailureReason.CONTEXT_PROOF_RECORD_MISMATCH,
    )

    @Test
    fun proofValidationBatchLineageMismatchFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(validationBatchLogicalDigest = "b".repeat(64))),
        FailureReason.CONTEXT_PROOF_RECORD_MISMATCH,
    )

    @Test
    fun proofReviewLineageMismatchFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(reviewUnitId = "other-review")),
        FailureReason.CONTEXT_PROOF_RECORD_MISMATCH,
    )

    @Test
    fun proofStableEntryMismatchFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(stableEntryId = "other-entry")),
        FailureReason.CONTEXT_PROOF_RECORD_MISMATCH,
    )

    @Test
    fun proofCanonicalMismatchFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(canonicalEntityId = "ZzYyXx")),
        FailureReason.CONTEXT_PROOF_RECORD_MISMATCH,
    )

    @Test
    fun positiveReferenceMismatchFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(positiveTrainingExampleReference = HimTrainingExampleReference("example:v1:${"f".repeat(64)}"))),
        FailureReason.POSITIVE_TRAINING_EXAMPLE_REFERENCE_MISMATCH,
    )

    @Test
    fun positiveTargetWithoutMatchingCanonicalContextFailsClosed() = assertReason(
        Fixture.request(positive = Fixture.positiveWithCanonicalContext("ZzYyXx")),
        FailureReason.V1_STRUCTURAL_PRECONDITION_FAILURE,
    )

    @Test
    fun canonicalEqualityAloneCannotCreateResolved() = assertReason(
        Fixture.request(positive = Fixture.positiveWithoutCanonicalContext),
        FailureReason.V1_STRUCTURAL_PRECONDITION_FAILURE,
    )

    @Test
    fun invalidPositiveExampleReferenceFailsClosedAsV1StructuralFailure() {
        assertFails {
            HimTrainingExampleV1(
                exampleReference = HimTrainingExampleReference("example:v1:${"f".repeat(64)}"),
                taskType = Fixture.positive.taskType,
                input = Fixture.positive.input,
                target = Fixture.positive.target,
                provenance = Fixture.positive.provenance,
            )
        }
    }

    @Test
    fun identityProofTamperingFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(identityProof = Fixture.proof().identityProof.copy(canonicalEntityId = "ZzYyXx"))),
        FailureReason.INPUT_IDENTITY_PROOF_MISMATCH,
    )

    @Test
    fun wrongOccurrenceProofFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(occurrenceReference = Fixture.otherOccurrence)),
        FailureReason.INPUT_IDENTITY_PROOF_MISMATCH,
    )

    @Test
    fun wrongMaterializationBindingProofFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(materializationDecisionId = "f".repeat(64))),
        FailureReason.INPUT_IDENTITY_PROOF_MISMATCH,
    )

    @Test
    fun canonicalEqualityWithoutIdentityProofCannotResolve() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(identityProof = Fixture.proof().identityProof.copy(occurrenceReference = Fixture.otherOccurrence))),
        FailureReason.INPUT_IDENTITY_PROOF_MISMATCH,
    )

    @Test
    fun completeEvidenceRelationIsAccepted() {
        assertEquals(1, completed(Fixture.request()).bindingDecision.evidenceBindings.size)
    }

    @Test
    fun missingEvidenceBindingFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(evidenceBindings = emptyList())),
        FailureReason.EVIDENCE_RELATION_PROOF_MISMATCH,
    )

    @Test
    fun additionalEvidenceBindingFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(evidenceBindings = Fixture.proof().evidenceBindings + EvidenceBinding("extra", Fixture.otherEvidence))),
        FailureReason.EVIDENCE_RELATION_PROOF_MISMATCH,
    )

    @Test
    fun duplicateEvidenceBindingFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(evidenceBindings = listOf(Fixture.proof().evidenceBindings.single(), Fixture.proof().evidenceBindings.single()))),
        FailureReason.EVIDENCE_RELATION_PROOF_MISMATCH,
    )

    @Test
    fun wrongSourceFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(evidenceBindings = listOf(EvidenceBinding(Fixture.evidenceId, Fixture.evidence.copy(source = "AGRIBALYSE"))))),
        FailureReason.EVIDENCE_RELATION_PROOF_MISMATCH,
    )

    @Test
    fun wrongArtifactDigestFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(evidenceBindings = listOf(EvidenceBinding(Fixture.evidenceId, Fixture.evidence.copy(sourceArtifactSha256 = HimSha256("f".repeat(64))))))),
        FailureReason.EVIDENCE_RELATION_PROOF_MISMATCH,
    )

    @Test
    fun wrongSourceRecordIdentityFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(evidenceBindings = listOf(EvidenceBinding(Fixture.evidenceId, Fixture.evidence.copy(sourceRecordIdentity = "other-record"))))),
        FailureReason.EVIDENCE_RELATION_PROOF_MISMATCH,
    )

    @Test
    fun evidenceAbsentFromPositiveExampleFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(evidenceBindings = listOf(EvidenceBinding(Fixture.evidenceId, Fixture.otherEvidence)))),
        FailureReason.EVIDENCE_RELATION_PROOF_MISMATCH,
    )

    @Test
    fun missingEvidenceIdFailsClosedWithoutPositionalMatching() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(evidenceBindings = listOf(EvidenceBinding("different-id", Fixture.evidence)))),
        FailureReason.EVIDENCE_RELATION_PROOF_MISMATCH,
    )

    @Test
    fun equalRejectedAndPositiveTargetsFailClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(rejectedTarget = Fixture.positive.target)),
        FailureReason.TARGET_OR_BOUNDARY_BINDING_MISMATCH,
    )

    @Test
    fun rejectedTargetBoundToCurrentCanonicalFailsClosed() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(rejectedTarget = HimTrainingTargetV1.Identity(Fixture.canonicalId))),
        FailureReason.TARGET_OR_BOUNDARY_BINDING_MISMATCH,
    )

    @Test
    fun inadmissibleBoundaryFailsClosedAndIsNotInferred() = assertReason(
        Fixture.request(proof = Fixture.proof().copy(boundaryType = HimNegativeBoundaryTypeV1.WRONG_SCOPE)),
        FailureReason.TARGET_OR_BOUNDARY_BINDING_MISMATCH,
    )

    @Test
    fun everyDeclaredFailureReasonHasAReachableFixture() {
        val reasons = setOf(
            FailureReason.INVALID_NEGATIVE_SUPERVISION_RECORD,
            FailureReason.INVALID_CONTEXT_PROOF,
            FailureReason.CONTEXT_PROOF_RECORD_MISMATCH,
            FailureReason.POSITIVE_TRAINING_EXAMPLE_REFERENCE_MISMATCH,
            FailureReason.INPUT_IDENTITY_PROOF_MISMATCH,
            FailureReason.EVIDENCE_RELATION_PROOF_MISMATCH,
            FailureReason.TARGET_OR_BOUNDARY_BINDING_MISMATCH,
            FailureReason.V1_STRUCTURAL_PRECONDITION_FAILURE,
        )
        assertEquals(reasons, FailureReason.entries.toSet())
    }

    @Test
    fun outputContainsAllFieldsRequiredByDownstreamBindingContext() {
        val decision = completed(Fixture.request()).bindingDecision
        assertEquals(Fixture.record, decision.trainingBinding.record)
        assertEquals(Fixture.positive, decision.trainingBinding.positiveTrainingExample)
        assertEquals(Fixture.rejectedTarget, decision.trainingBinding.rejectedTarget)
        assertEquals(Fixture.boundaryType, decision.trainingBinding.boundaryType)
        assertEquals(Fixture.positive.provenance, decision.trainingBinding.trainingProvenance)
        assertEquals<List<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding>>(decision.evidenceBindings, decision.trainingBinding.evidenceBindings)
    }

    @Test
    fun noNotYetResolvedStateExistsInProofAwareSuccessModel() {
        assertEquals(setOf(ResolutionState.RESOLVED), ResolutionState.entries.toSet())
        assertEquals(setOf(InputIdentity.PROVABLE), InputIdentity.entries.toSet())
        assertEquals(setOf(EvidenceRelationship.COMPATIBLE), EvidenceRelationship.entries.toSet())
    }

    @Test
    fun contractIsSingletonInMemoryOnly() {
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.CONTRACT_ID.endsWith("_V1"))
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.VERSION)
        assertEquals("PROOF_AWARE_POSITIVE_TRAINING_EXAMPLE_BINDING_RESOLUTION_CONTEXT_ONLY", HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.STATE)
    }

    private fun completed(request: HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.Request): HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.Resolved =
        assertIs<Result.Resolved>(HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.evaluate(request)).value

    private fun assertReason(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.Request,
        expected: FailureReason,
    ) {
        val result = assertIs<Result.Failed>(HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.evaluate(request))
        assertEquals(expected, result.reason)
    }

    private object Fixture {
        val canonicalId = HimEntityId("AbCd12")
        val stableEntryId = "a".repeat(64)
        val reviewUnit = HimZeroCandidateRecoveryHumanReviewReviewUnitV1(stableEntryId, canonicalId.value)
        val occurrence = "occurrence:v1:${"b".repeat(64)}"
        val otherOccurrence = "occurrence:v1:${"c".repeat(64)}"
        val evidenceId = "negative-evidence-1"
        val evidence = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("d".repeat(64)), "off:product:row:1")
        val otherEvidence = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("e".repeat(64)), "off:product:row:2")
        val positive = HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = HimTrainingInputV1(
                observedTerm = "unknown food",
                normalizedObservedTerm = "unknown food",
                canonicalContext = listOf(HimCandidateCanonicalContext(1, canonicalId, "Canonical", null)),
                evidence = listOf(HimTrainingEvidenceInputV1(evidence, "FOOD", 1)),
            ),
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(canonicalId)),
            provenance = HimTrainingProvenanceV1(sourceEvidenceReferences = listOf(evidence)),
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
            reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED),
            evidenceReferenceIds = listOf(evidenceId),
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
            evidenceReferenceIds = listOf(evidenceId),
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
            evidenceReferenceIds = listOf(evidenceId),
            downstreamRoute = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE,
            candidateState = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE,
        )
        val materialization = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationDecision(
            decisionId = "1".repeat(64),
            preMaterializationIdentity = HimCandidateOccurrenceReference(occurrence),
            occurrenceReference = HimCandidateOccurrenceReference(occurrence),
            projectionInputBindingDigest = HimSha256("2".repeat(64)),
            state = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationState.PROJECTED,
            reasons = emptyList(),
            projectedTrainingExample = positive,
            trainingExampleReference = positive.exampleReference,
        )
        val referenceBinding = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1.BindingRecord(
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
        val projectionRequest = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.Request(
            negativeSupervisionRecord = record,
            validationRecord = validationRecord,
            reviewUnit = reviewUnit,
            positiveTrainingExample = positive,
            materializationDecision = materialization,
            referenceBinding = referenceBinding,
            rejectedTarget = rejectedTarget,
            boundaryType = boundaryType,
            evidenceBindings = listOf(EvidenceBinding(evidenceId, evidence)),
        )

        fun proof() = when (val result = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.evaluate(projectionRequest)) {
            is HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.Result.Completed -> result.value
            is HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.Result.Failed -> error(result.reason)
        }

        fun request(
            record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record = this.record,
            positive: HimTrainingExampleV1 = this.positive,
            proof: ProofRecord = proof(),
        ) = HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.Request(record, positive, proof)

        fun positiveWithCanonicalContext(id: String) = HimTrainingExampleV1.create(
            taskType = positive.taskType,
            input = positive.input.copy(canonicalContext = listOf(HimCandidateCanonicalContext(1, HimEntityId(id), "Other", null))),
            target = positive.target,
            provenance = positive.provenance,
        )
    }

}
