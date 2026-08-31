package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleValidatorV1

/**
 * Pure, in-memory successor to the structural P1 binding boundary. It applies
 * an already validated Context Proof to one exact negative record and one
 * exact positive training example; it never discovers or persists anything.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_PROOF_AWARE_POSITIVE_TRAINING_EXAMPLE_BINDING_RESOLUTION_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "PROOF_AWARE_POSITIVE_TRAINING_EXAMPLE_BINDING_RESOLUTION_CONTEXT_ONLY"

    private val SHA256 = Regex("[0-9a-f]{64}")
    private val OCCURRENCE = Regex("occurrence:v1:[0-9a-f]{64}")
    private val TRAINING_REFERENCE = Regex("example:v1:[0-9a-f]{64}")

    fun evaluate(request: Request): Result {
        val record = request.negativeSupervisionRecord
        val proof = request.contextProof
        val positive = request.positiveTrainingExample

        if (!validNegativeRecord(record)) return Result.Failed(FailureReason.INVALID_NEGATIVE_SUPERVISION_RECORD)
        if (!validProofShape(proof)) return Result.Failed(FailureReason.INVALID_CONTEXT_PROOF)
        if (!validV1PositivePreconditions(record, positive)) {
            return Result.Failed(FailureReason.V1_STRUCTURAL_PRECONDITION_FAILURE)
        }
        if (!proofMatchesRecord(proof, record)) return Result.Failed(FailureReason.CONTEXT_PROOF_RECORD_MISMATCH)
        if (proof.positiveTrainingExampleReference != positive.exampleReference) {
            return Result.Failed(FailureReason.POSITIVE_TRAINING_EXAMPLE_REFERENCE_MISMATCH)
        }
        if (!identityProofMatches(proof, record, positive)) {
            return Result.Failed(FailureReason.INPUT_IDENTITY_PROOF_MISMATCH)
        }
        if (!targetAndBoundaryApply(proof, record, positive)) {
            return Result.Failed(FailureReason.TARGET_OR_BOUNDARY_BINDING_MISMATCH)
        }

        val evidenceBindings = mapEvidence(proof, record, positive)
            ?: return Result.Failed(FailureReason.EVIDENCE_RELATION_PROOF_MISMATCH)
        val trainingBinding = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.RecordBinding(
            record = record,
            positiveTrainingExample = positive,
            rejectedTarget = proof.rejectedTarget,
            boundaryType = proof.boundaryType,
            evidenceBindings = evidenceBindings,
            trainingProvenance = positive.provenance,
        )
        val decision = BindingDecision(
            recordId = record.recordId,
            state = ResolutionState.RESOLVED,
            inputIdentity = InputIdentity.PROVABLE,
            evidenceRelationship = EvidenceRelationship.COMPATIBLE,
            rejectedTarget = proof.rejectedTarget,
            boundaryType = proof.boundaryType,
            evidenceBindings = evidenceBindings,
            trainingProvenance = positive.provenance,
            trainingBinding = trainingBinding,
        )
        return Result.Resolved(
            Resolved(
                negativeSupervisionRecord = record,
                positiveTrainingExample = positive,
                contextProof = proof,
                bindingDecision = decision,
            ),
        )
    }

    private fun validNegativeRecord(
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
    ): Boolean =
        record.recordId.matches(SHA256) &&
            record.negativeCandidateId.matches(SHA256) &&
            record.readinessDecisionId.matches(SHA256) &&
            record.validationRecordId.matches(SHA256) &&
            record.canonicalEntityId.isNotBlank() &&
            record.originalReviewerRef.isNotBlank() &&
            record.validatorReviewerRef.isNotBlank() &&
            record.reviewUnitId.isNotBlank() &&
            record.stableEntryId.isNotBlank() &&
            record.validationRound > 0 &&
            record.validationRevision > 0 &&
            record.validationReasonCodes.isNotEmpty() &&
            record.candidateState == HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE &&
            record.originalDecision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION &&
            record.downstreamRoute == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE &&
            record.evidenceReferenceIds.isNotEmpty() &&
            record.evidenceReferenceIds.distinct().size == record.evidenceReferenceIds.size &&
            record.evidenceReferenceIds.all(String::isNotBlank)

    private fun validProofShape(
        proof: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord,
    ): Boolean =
        proof.contractId == HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.CONTRACT_ID &&
            proof.version == HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.VERSION &&
            proof.state == HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.STATE &&
            proof.negativeSupervisionRecordId.matches(SHA256) &&
            proof.negativeCandidateId.matches(SHA256) &&
            proof.readinessDecisionId.matches(SHA256) &&
            proof.eligibilityDecisionId.matches(SHA256) &&
            proof.validationRecordId.matches(SHA256) &&
            proof.validationBatchBindingDigest.matches(SHA256) &&
            proof.validationBatchLogicalDigest.matches(SHA256) &&
            proof.eligibilityInputBindingDigest.matches(SHA256) &&
            proof.projectionInputBindingDigest.matches(SHA256) &&
            proof.materializationDecisionId.matches(SHA256) &&
            proof.bindingDigest.matches(SHA256) &&
            proof.logicalDigest.matches(SHA256) &&
            proof.positiveTrainingExampleReference.value.matches(TRAINING_REFERENCE) &&
            proof.occurrenceReference.matches(OCCURRENCE) &&
            proof.identityProof.positiveTrainingExampleReference.matches(TRAINING_REFERENCE) &&
            proof.identityProof.occurrenceReference.matches(OCCURRENCE) &&
            proof.evidenceBindings.all { it.negativeEvidenceReferenceId.isNotBlank() && validEvidence(it.evidence) }

    private fun validEvidence(evidence: HimEvidenceReference): Boolean =
        evidence.source.isNotBlank() && evidence.sourceRecordIdentity.isNotBlank() &&
            evidence.sourceArtifactSha256.value.matches(SHA256)

    private fun validV1PositivePreconditions(
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        positive: HimTrainingExampleV1,
    ): Boolean = try {
        HimTrainingExampleValidatorV1.validate(positive)
        val targetCanonicalId = canonicalId(positive.target)
        targetCanonicalId?.value == record.canonicalEntityId &&
            positive.input.canonicalContext.any { it.canonicalId.value == record.canonicalEntityId }
    } catch (_: Throwable) {
        false
    }

    private fun proofMatchesRecord(
        proof: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord,
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
    ): Boolean =
        proof.negativeSupervisionRecordId == record.recordId &&
            proof.negativeCandidateId == record.negativeCandidateId &&
            proof.readinessDecisionId == record.readinessDecisionId &&
            proof.eligibilityDecisionId == record.eligibilityDecisionId &&
            proof.validationBatchId == record.validationBatchId &&
            proof.validationBatchBindingDigest == record.validationBatchBindingDigest &&
            proof.validationBatchLogicalDigest == record.validationBatchLogicalDigest &&
            proof.eligibilityBatchId == record.eligibilityBatchId &&
            proof.eligibilityInputBindingDigest == record.eligibilityInputBindingDigest &&
            proof.validationRecordId == record.validationRecordId &&
            proof.reviewUnitId == record.reviewUnitId &&
            proof.stableEntryId == record.stableEntryId &&
            proof.canonicalEntityId == record.canonicalEntityId &&
            proof.originalReviewerRef == record.originalReviewerRef &&
            proof.validatorReviewerRef == record.validatorReviewerRef &&
            proof.validationRound == record.validationRound &&
            proof.validationRevision == record.validationRevision

    private fun identityProofMatches(
        proof: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord,
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        positive: HimTrainingExampleV1,
    ): Boolean {
        val identity = proof.identityProof
        return identity.negativeSupervisionRecordId == record.recordId &&
            identity.validationRecordId == record.validationRecordId &&
            identity.reviewUnitId == record.reviewUnitId &&
            identity.stableEntryId == record.stableEntryId &&
            identity.canonicalEntityId == record.canonicalEntityId &&
            identity.occurrenceReference == proof.occurrenceReference &&
            identity.materializationDecisionId == proof.materializationDecisionId &&
            identity.positiveTrainingExampleReference == positive.exampleReference.value
    }

    private fun targetAndBoundaryApply(
        proof: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord,
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        positive: HimTrainingExampleV1,
    ): Boolean {
        return try {
            if (proof.rejectedTarget == positive.target) return false
            if (canonicalId(proof.rejectedTarget)?.value == record.canonicalEntityId) return false
            HimNegativeTrainingExamplePolicyV1.evaluate(
                positiveExample = positive,
                rejectedTarget = proof.rejectedTarget,
                boundaryType = proof.boundaryType,
            ).admissible
        } catch (_: Throwable) {
            false
        }
    }

    private fun mapEvidence(
        proof: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord,
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        positive: HimTrainingExampleV1,
    ): List<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding>? {
        val proofIds = proof.evidenceBindings.map { it.negativeEvidenceReferenceId }
        if (proofIds.distinct().size != proofIds.size) return null
        if (proofIds.toSet() != record.evidenceReferenceIds.toSet()) return null
        val inputByReference = positive.input.evidence.associateBy { it.reference }
        val provenanceEvidence = positive.provenance.sourceEvidenceReferences.toSet()
        val mapped = proof.evidenceBindings.mapNotNull { binding ->
            val input = inputByReference[binding.evidence] ?: return null
            if (binding.evidence !in provenanceEvidence) return null
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding(
                persistedEvidenceReferenceId = binding.negativeEvidenceReferenceId,
                trainingEvidence = input,
            )
        }
        return mapped.takeIf { it.size == proof.evidenceBindings.size }
            ?.sortedWith(compareBy({ it.persistedEvidenceReferenceId }, { evidenceKey(it.trainingEvidence.reference) }))
    }

    private fun canonicalId(target: HimTrainingTargetV1): HimEntityId? = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> target.canonicalId
        is HimTrainingTargetV1.Identity -> target.parentCanonicalId
        is HimTrainingTargetV1.Variant -> target.scope.canonicalId
        is HimTrainingTargetV1.Alias -> target.equivalentEntity.canonicalId
        is HimTrainingTargetV1.NewCanonical -> null
    }

    private fun evidenceKey(reference: HimEvidenceReference): String =
        "${reference.source}|${reference.sourceArtifactSha256.value}|${reference.sourceRecordIdentity}"

    enum class ResolutionState { RESOLVED }

    enum class InputIdentity { PROVABLE }

    enum class EvidenceRelationship { COMPATIBLE }

    enum class FailureReason {
        INVALID_NEGATIVE_SUPERVISION_RECORD,
        INVALID_CONTEXT_PROOF,
        CONTEXT_PROOF_RECORD_MISMATCH,
        POSITIVE_TRAINING_EXAMPLE_REFERENCE_MISMATCH,
        INPUT_IDENTITY_PROOF_MISMATCH,
        EVIDENCE_RELATION_PROOF_MISMATCH,
        TARGET_OR_BOUNDARY_BINDING_MISMATCH,
        V1_STRUCTURAL_PRECONDITION_FAILURE,
    }

    data class BindingDecision(
        val recordId: String,
        val state: ResolutionState,
        val inputIdentity: InputIdentity,
        val evidenceRelationship: EvidenceRelationship,
        val rejectedTarget: HimTrainingTargetV1,
        val boundaryType: HimNegativeBoundaryTypeV1,
        val evidenceBindings: List<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding>,
        val trainingProvenance: HimTrainingProvenanceV1,
        val trainingBinding: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.RecordBinding,
    )

    data class Request(
        val negativeSupervisionRecord: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        val positiveTrainingExample: HimTrainingExampleV1,
        val contextProof: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord,
    )

    data class Resolved(
        val negativeSupervisionRecord: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        val positiveTrainingExample: HimTrainingExampleV1,
        val contextProof: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord,
        val bindingDecision: BindingDecision,
    )

    sealed interface Result {
        data class Resolved(val value: HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.Resolved) : Result
        data class Failed(val reason: FailureReason) : Result
    }
}
