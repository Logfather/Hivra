package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1
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
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1Test {
    @Test
    fun validSingletonPipelineCompletes() {
        val completed = completed(Fixture.request())
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.ResolutionState.RESOLVED,
            completed.proofAwareResolution.bindingDecision.state,
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.BindingState.READY_FOR_NEGATIVE_TRAINING_EXAMPLE_ADAPTER,
            completed.downstreamBindingDecision.state,
        )
    }

    @Test
    fun successRetainsExactProofAwareResolutionAndContext() {
        val completed = completed(Fixture.request())
        assertEquals(Fixture.record, completed.proofAwareResolution.negativeSupervisionRecord)
        assertEquals(Fixture.positive, completed.proofAwareResolution.positiveTrainingExample)
        assertEquals(Fixture.proof(), completed.proofAwareResolution.contextProof)
        assertEquals(Fixture.record.recordId, completed.downstreamBindingDecision.recordId)
        assertEquals(Fixture.positive.exampleReference, completed.downstreamBindingDecision.positiveExampleReference)
    }

    @Test
    fun downstreamContextPreservesTargetBoundaryEvidenceAndProvenance() {
        val completed = completed(Fixture.request())
        val decision = completed.downstreamBindingDecision
        assertEquals(Fixture.rejectedTarget, decision.rejectedTarget)
        assertEquals(Fixture.boundaryType, decision.boundaryType)
        assertEquals(Fixture.positive.provenance, decision.trainingProvenance)
        assertEquals(Fixture.record.evidenceReferenceIds.single(), decision.evidenceBindings.single().persistedEvidenceReferenceId)
        assertEquals(Fixture.evidence, decision.evidenceBindings.single().trainingEvidence.reference)
        assertEquals("FOOD", decision.evidenceBindings.single().trainingEvidence.recordKind)
        assertEquals(1, decision.evidenceBindings.single().trainingEvidence.retrievalRank)
    }

    @Test
    fun contextBatchIsSingletonAndValid() {
        val context = completed(Fixture.request()).downstreamBindingContext
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.CONTRACT_ID, context.contractId)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.VERSION, context.version)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.STATE, context.state)
        assertEquals(1, context.decisions.size)
        assertEquals(1, context.counters.totalRecords)
        assertEquals(1, context.counters.readyForNegativeTrainingExampleAdapter)
        assertEquals(0, context.counters.notYetBindable)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.batchLogicalDigest(context),
            context.logicalDigest,
        )
    }

    @Test
    fun repeatedExecutionIsDeterministic() {
        val first = completed(Fixture.request())
        val second = completed(Fixture.request())
        assertEquals(first, second)
        assertEquals(first.downstreamBindingContext, second.downstreamBindingContext)
        assertEquals(first.downstreamBindingDecision, second.downstreamBindingDecision)
    }

    @Test
    fun proofAwareFailurePropagatesWithoutDownstreamSuccess() {
        val failed = failed(Fixture.request(proof = Fixture.proof().copy(state = "INVALID")))
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.FailureReason.PROOF_AWARE_RESOLUTION_FAILED,
            failed.reason,
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.FailureReason.INVALID_CONTEXT_PROOF,
            assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.FailureCause.ProofAware>(failed.cause).reason,
        )
    }

    @Test
    fun recordMismatchRemainsOwnedByProofAwareResolution() {
        val failed = failed(Fixture.request(proof = Fixture.proof().copy(reviewUnitId = "other-review")))
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.FailureReason.CONTEXT_PROOF_RECORD_MISMATCH,
            assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.FailureCause.ProofAware>(failed.cause).reason,
        )
    }

    @Test
    fun positiveReferenceMismatchRemainsOwnedByProofAwareResolution() {
        val failed = failed(
            Fixture.request(
                proof = Fixture.proof().copy(
                    positiveTrainingExampleReference = HimTrainingExampleReference("example:v1:${"f".repeat(64)}"),
                ),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.FailureReason.POSITIVE_TRAINING_EXAMPLE_REFERENCE_MISMATCH,
            assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.FailureCause.ProofAware>(failed.cause).reason,
        )
    }

    @Test
    fun identityProofMismatchRemainsOwnedByProofAwareResolution() {
        val failed = failed(
            Fixture.request(
                proof = Fixture.proof().copy(
                    identityProof = Fixture.proof().identityProof.copy(canonicalEntityId = "ZzYyXx"),
                ),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.FailureReason.INPUT_IDENTITY_PROOF_MISMATCH,
            assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.FailureCause.ProofAware>(failed.cause).reason,
        )
    }

    @Test
    fun evidenceProofMismatchRemainsOwnedByProofAwareResolution() {
        val failed = failed(
            Fixture.request(
                proof = Fixture.proof().copy(
                    evidenceBindings = emptyList(),
                ),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.FailureReason.EVIDENCE_RELATION_PROOF_MISMATCH,
            assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.FailureCause.ProofAware>(failed.cause).reason,
        )
    }

    @Test
    fun rejectedTargetAndBoundaryComeOnlyFromProofAwareResolution() {
        val completed = completed(
            Fixture.request(
                proof = Fixture.proof().copy(
                    rejectedTarget = HimTrainingTargetV1.NewCanonical("different target"),
                    boundaryType = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
                ),
            ),
        )
        assertEquals(HimTrainingTargetV1.NewCanonical("different target"), completed.downstreamBindingDecision.rejectedTarget)
        assertEquals(HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY, completed.downstreamBindingDecision.boundaryType)
    }

    @Test
    fun manyToOneKeepsIndependentBindings() {
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
        assertEquals(first.proofAwareResolution.positiveTrainingExample.exampleReference, second.proofAwareResolution.positiveTrainingExample.exampleReference)
        assertNotEquals(first.downstreamBindingDecision.recordId, second.downstreamBindingDecision.recordId)
    }

    @Test
    fun contractIsSingletonInMemoryOnly() {
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.VERSION)
        assertEquals(
            "POSITIVE_TRAINING_EXAMPLE_DOWNSTREAM_ORCHESTRATION_CONTEXT_ONLY",
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.STATE,
        )
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.CONTRACT_ID.endsWith("_V1"))
    }

    @Test
    fun noNegativeTrainingExampleIsProduced() {
        val completed = completed(Fixture.request())
        assertTrue(completed.downstreamBindingDecision.rejectedTarget is HimTrainingTargetV1.NewCanonical)
        assertEquals(Fixture.positive, completed.proofAwareResolution.positiveTrainingExample)
    }

    private fun completed(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Request,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Completed =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Result.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.evaluate(request),
        ).value

    private fun failed(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Request,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Result.Failed =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Result.Failed>(
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.evaluate(request),
        )

    private object Fixture {
        val canonicalId = HimEntityId("AbCd12")
        val stableEntryId = "a".repeat(64)
        val reviewUnit = HimZeroCandidateRecoveryHumanReviewReviewUnitV1(stableEntryId, canonicalId.value)
        val occurrence = "occurrence:v1:${"b".repeat(64)}"
        val evidenceId = "negative-evidence-1"
        val evidence = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("d".repeat(64)), "off:product:row:1")
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
            evidenceBindings = listOf(
                HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.EvidenceBinding(evidenceId, evidence),
            ),
        )

        fun proof() = when (
            val result = HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.evaluate(projectionRequest)
        ) {
            is HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.Result.Completed -> result.value
            is HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.Result.Failed -> error(result.reason)
        }

        fun request(
            record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record = this.record,
            positive: HimTrainingExampleV1 = this.positive,
            proof: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord = proof(),
        ) = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Request(record, positive, proof)
    }
}
