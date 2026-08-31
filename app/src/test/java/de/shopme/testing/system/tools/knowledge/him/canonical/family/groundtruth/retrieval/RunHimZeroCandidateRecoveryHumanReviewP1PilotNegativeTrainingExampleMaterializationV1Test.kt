package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
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

class RunHimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1Test {
    @Test
    fun validCompletedMaterializesOneNegativeExampleAndP1Lineage() {
        val materialized = completed(Fixture.orchestration())

        assertEquals(
            HimNegativeTrainingExampleV1.create(Fixture.positive, Fixture.rejectedTarget, Fixture.boundaryType),
            materialized.negativeTrainingExample,
        )
        assertEquals(Fixture.positive.exampleReference, materialized.negativeTrainingExample.provenance.positiveExampleReference)
        assertEquals(Fixture.positive, materialized.negativeTrainingExample.positiveExample)
        assertEquals(Fixture.rejectedTarget, materialized.negativeTrainingExample.rejectedTarget)
        assertEquals(Fixture.boundaryType, materialized.negativeTrainingExample.boundaryType)
        assertTrue(materialized.materializationId.value != materialized.negativeTrainingExample.reference.value)
        assertEquals(Fixture.record.recordId, materialized.p1Lineage.negativeSupervisionRecordId)
        assertEquals(Fixture.record.negativeCandidateId, materialized.p1Lineage.negativeCandidateId)
        assertEquals(Fixture.record.readinessDecisionId, materialized.p1Lineage.readinessDecisionId)
        assertEquals(Fixture.record.readinessState, materialized.p1Lineage.readinessState)
        assertEquals(Fixture.record.eligibilityBatchId, materialized.p1Lineage.eligibilityBatchId)
        assertEquals(Fixture.record.eligibilityDecisionId, materialized.p1Lineage.eligibilityDecisionId)
        assertEquals(Fixture.record.eligibilityState, materialized.p1Lineage.eligibilityState)
        assertEquals(Fixture.record.validationRecordId, materialized.p1Lineage.validationRecordId)
        assertEquals(Fixture.record.canonicalEntityId, materialized.p1Lineage.canonicalEntityId)
        assertEquals(Fixture.record.originalReviewerRef, materialized.p1Lineage.originalReviewerRef)
        assertEquals(Fixture.record.validatorReviewerRef, materialized.p1Lineage.validatorReviewerRef)
        assertEquals(Fixture.record.validationRound, materialized.p1Lineage.validationRound)
        assertEquals(Fixture.record.validationRevision, materialized.p1Lineage.validationRevision)
        assertEquals(Fixture.record.validationReasonCodes, materialized.p1Lineage.validationReasonCodes)
        assertEquals(Fixture.record.evidenceReferenceIds, materialized.p1Lineage.evidenceReferenceIds)
        assertEquals(Fixture.rejectedTarget, materialized.p1Lineage.rejectedTarget)
        assertEquals(Fixture.boundaryType, materialized.p1Lineage.boundaryType)
        assertEquals(Fixture.proof().occurrenceReference, materialized.p1Lineage.occurrenceReference)
        assertEquals(Fixture.positive.exampleReference, materialized.p1Lineage.positiveTrainingExampleReference)
        assertEquals(Fixture.materialization.decisionId, materialized.p1Lineage.materializationDecisionId)
        assertEquals(Fixture.proof().projectionInputBindingDigest, materialized.p1Lineage.projectionInputBindingDigest)
        assertEquals(Fixture.proof().bindingDigest, materialized.p1Lineage.proofBindingDigest)
        assertEquals(Fixture.proof().logicalDigest, materialized.p1Lineage.proofLogicalDigest)
        assertEquals(Fixture.record.evidenceReferenceIds.single(), materialized.evidenceBindings.single().persistedEvidenceReferenceId)
    }

    @Test
    fun materializationIdentityIsDeterministicAndIncludesP1RecordBinding() {
        val first = completed(Fixture.orchestration())
        val second = completed(Fixture.orchestration())
        assertEquals(first, second)
        assertEquals(first.materializationId, second.materializationId)

        val secondRecord = Fixture.record.copy(
            recordId = "9".repeat(64),
            negativeCandidateId = "8".repeat(64),
            readinessDecisionId = "7".repeat(64),
            validationRecordId = "6".repeat(64),
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
        val other = completed(Fixture.orchestration(secondRecord, secondProof))
        assertEquals(first.negativeTrainingExample.reference, other.negativeTrainingExample.reference)
        assertNotEquals(first.materializationId, other.materializationId)
    }

    @Test
    fun evidenceBindingsAreExactSortedAndOwnedByInputAndProvenance() {
        val materialized = completed(Fixture.orchestration())
        val decision = Fixture.orchestration().downstreamBindingDecision
        assertEquals(decision.evidenceBindings, materialized.evidenceBindings)
        assertEquals(
            materialized.negativeTrainingExample.positiveExample.input.evidence.map { it.reference }.toSet(),
            materialized.evidenceBindings.map { it.trainingEvidence.reference }.toSet(),
        )
        assertEquals(
            materialized.negativeTrainingExample.positiveExample.provenance.sourceEvidenceReferences.toSet(),
            materialized.evidenceBindings.map { it.trainingEvidence.reference }.toSet(),
        )
    }

    @Test
    fun wrongBoundaryFailsClosed() {
        val baseline = Fixture.orchestration()
        val proofAware = baseline.proofAwareResolution
        val altered = baseline.copy(
            proofAwareResolution = proofAware.copy(
                bindingDecision = proofAware.bindingDecision.copy(
                    boundaryType = HimNegativeBoundaryTypeV1.WRONG_SCOPE,
                ),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.FailureReason.P1_LINEAGE_BINDING_MISMATCH,
            failed(altered).reason,
        )
    }

    @Test
    fun provenanceMismatchFailsClosed() {
        val baseline = Fixture.orchestration()
        val altered = baseline.copy(
            proofAwareResolution = baseline.proofAwareResolution.copy(
                bindingDecision = baseline.proofAwareResolution.bindingDecision.copy(
                    trainingProvenance = HimTrainingProvenanceV1(),
                    trainingBinding = baseline.proofAwareResolution.bindingDecision.trainingBinding.copy(
                        trainingProvenance = HimTrainingProvenanceV1(),
                    ),
                ),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.FailureReason.P1_LINEAGE_BINDING_MISMATCH,
            failed(altered).reason,
        )
    }

    @Test
    fun wrongRejectedTargetFailsClosed() {
        val baseline = Fixture.orchestration()
        val altered = baseline.copy(
            proofAwareResolution = baseline.proofAwareResolution.copy(
                bindingDecision = baseline.proofAwareResolution.bindingDecision.copy(
                    rejectedTarget = HimTrainingTargetV1.NewCanonical("another canonical"),
                ),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.FailureReason.P1_LINEAGE_BINDING_MISMATCH,
            failed(altered).reason,
        )
    }

    @Test
    fun missingExtraAndDuplicateProofEvidenceFailClosed() {
        val baseline = Fixture.orchestration()
        val proof = Fixture.proof()
        val missing = baseline.copy(
            proofAwareResolution = baseline.proofAwareResolution.copy(
                contextProof = proof.copy(evidenceBindings = emptyList()),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.FailureReason.EVIDENCE_BINDING_MISMATCH,
            failed(missing).reason,
        )

        val extraEvidence = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("e".repeat(64)), "off:product:row:2")
        val extra = baseline.copy(
            proofAwareResolution = baseline.proofAwareResolution.copy(
                contextProof = proof.copy(
                    evidenceBindings = proof.evidenceBindings +
                        HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.EvidenceBinding(
                            "extra-evidence",
                            extraEvidence,
                        ),
                ),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.FailureReason.EVIDENCE_BINDING_MISMATCH,
            failed(extra).reason,
        )

        val duplicate = baseline.copy(
            proofAwareResolution = baseline.proofAwareResolution.copy(
                contextProof = proof.copy(evidenceBindings = proof.evidenceBindings + proof.evidenceBindings),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.FailureReason.EVIDENCE_BINDING_MISMATCH,
            failed(duplicate).reason,
        )
    }

    @Test
    fun invalidStructureAndGenericPolicyFailureAreTyped() {
        val baseline = Fixture.orchestration()
        val invalidStructure = baseline.copy(
            downstreamBindingContext = baseline.downstreamBindingContext.copy(state = "INVALID"),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.FailureReason.INVALID_ORCHESTRATION_RESULT,
            failed(invalidStructure).reason,
        )

        val sameAsPositive = Fixture.positive.target
        val changedDecision = baseline.downstreamBindingDecision.copy(rejectedTarget = sameAsPositive)
        val changedContext = baseline.downstreamBindingContext.copy(
            decisions = listOf(changedDecision),
            counters = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.Counters.from(
                listOf(changedDecision),
            ),
            logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.batchLogicalDigest(
                baseline.downstreamBindingContext.copy(
                    decisions = listOf(changedDecision),
                    counters = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.Counters.from(
                        listOf(changedDecision),
                    ),
                    logicalDigest = "",
                ),
            ),
        )
        val genericFailure = baseline.copy(
            proofAwareResolution = baseline.proofAwareResolution.copy(
                contextProof = baseline.proofAwareResolution.contextProof.copy(rejectedTarget = sameAsPositive),
                bindingDecision = baseline.proofAwareResolution.bindingDecision.copy(
                    rejectedTarget = sameAsPositive,
                    trainingBinding = baseline.proofAwareResolution.bindingDecision.trainingBinding.copy(
                        rejectedTarget = sameAsPositive,
                    ),
                ),
            ),
            downstreamBindingContext = changedContext,
            downstreamBindingDecision = changedDecision,
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.FailureReason.GENERIC_NEGATIVE_EXAMPLE_REJECTED,
            failed(genericFailure).reason,
        )
    }

    @Test
    fun contractIsPureInMemoryAndHasNoPersistenceSurface() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_TRAINING_EXAMPLE_MATERIALIZATION_CONTRACT_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.VERSION)
        assertEquals(
            "P1_NEGATIVE_TRAINING_EXAMPLE_MATERIALIZATION_CONTEXT_ONLY",
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.STATE,
        )
        val methodNames =
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1::class.java
                .declaredMethods
                .map { it.name }
                .toSet()
        assertTrue(methodNames.contains("materialize"))
        assertTrue(methodNames.none { it.contains("persist", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("write", ignoreCase = true) })
        assertTrue(methodNames.none { it.contains("read", ignoreCase = true) })
    }

    private fun completed(
        orchestration: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Completed,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.Materialized =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.Result.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.materialize(orchestration),
        ).value

    private fun failed(
        orchestration: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Completed,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.Result.Failed =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.Result.Failed>(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.materialize(orchestration),
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

        fun orchestration(
            record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record = this.record,
            proof: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord = proof(),
        ): HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Completed = when (
            val result = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.evaluate(
                HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Request(
                    negativeSupervisionRecord = record,
                    positiveTrainingExample = positive,
                    contextProof = proof,
                ),
            )
        ) {
            is HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Result.Completed -> result.value
            is HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Result.Failed -> error(result.reason)
        }
    }
}
