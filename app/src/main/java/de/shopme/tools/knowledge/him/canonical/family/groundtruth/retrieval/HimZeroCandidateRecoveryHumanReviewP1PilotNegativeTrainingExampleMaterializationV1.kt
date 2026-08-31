package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure, in-memory P1 boundary from a completed downstream binding to one
 * source-bound negative training example materialization.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_TRAINING_EXAMPLE_MATERIALIZATION_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "P1_NEGATIVE_TRAINING_EXAMPLE_MATERIALIZATION_CONTEXT_ONLY"

    private const val MATERIALIZATION_ID_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_TRAINING_EXAMPLE_MATERIALIZATION_ID_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun materialize(
        orchestration: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Completed,
    ): Result {
        val structuralFailure = validateStructure(orchestration)
        if (structuralFailure != null) return Result.Failed(structuralFailure)

        val resolved = orchestration.proofAwareResolution
        val decision = orchestration.downstreamBindingDecision
        val evidenceBindings = decision.evidenceBindings
            .sortedWith(compareBy({ it.persistedEvidenceReferenceId }, { evidenceKey(it.trainingEvidence.reference) }))

        val lineageFailure = validateLineage(orchestration)
        if (lineageFailure != null) return Result.Failed(lineageFailure)
        val evidenceFailure = validateEvidence(orchestration, evidenceBindings)
        if (evidenceFailure != null) return Result.Failed(evidenceFailure)

        val negativeExample = try {
            HimNegativeTrainingExampleV1.create(
                positiveExample = resolved.positiveTrainingExample,
                rejectedTarget = requireNotNull(decision.rejectedTarget),
                boundaryType = requireNotNull(decision.boundaryType),
            )
        } catch (_: IllegalArgumentException) {
            return Result.Failed(FailureReason.GENERIC_NEGATIVE_EXAMPLE_REJECTED)
        }

        val lineage = P1Lineage(
            negativeSupervisionRecordId = resolved.negativeSupervisionRecord.recordId,
            negativeCandidateId = resolved.negativeSupervisionRecord.negativeCandidateId,
            readinessDecisionId = resolved.negativeSupervisionRecord.readinessDecisionId,
            readinessState = resolved.negativeSupervisionRecord.readinessState,
            eligibilityBatchId = resolved.negativeSupervisionRecord.eligibilityBatchId,
            eligibilityInputBindingDigest = resolved.negativeSupervisionRecord.eligibilityInputBindingDigest,
            eligibilityDecisionId = resolved.negativeSupervisionRecord.eligibilityDecisionId,
            eligibilityState = resolved.negativeSupervisionRecord.eligibilityState,
            validationBatchId = resolved.negativeSupervisionRecord.validationBatchId,
            validationBatchBindingDigest = resolved.negativeSupervisionRecord.validationBatchBindingDigest,
            validationBatchLogicalDigest = resolved.negativeSupervisionRecord.validationBatchLogicalDigest,
            validationRecordId = resolved.negativeSupervisionRecord.validationRecordId,
            reviewUnitId = resolved.negativeSupervisionRecord.reviewUnitId,
            stableEntryId = resolved.negativeSupervisionRecord.stableEntryId,
            canonicalEntityId = resolved.negativeSupervisionRecord.canonicalEntityId,
            originalReviewerRef = resolved.negativeSupervisionRecord.originalReviewerRef,
            validatorReviewerRef = resolved.negativeSupervisionRecord.validatorReviewerRef,
            validationRound = resolved.negativeSupervisionRecord.validationRound,
            validationRevision = resolved.negativeSupervisionRecord.validationRevision,
            originalDecision = resolved.negativeSupervisionRecord.originalDecision,
            assessment = resolved.negativeSupervisionRecord.assessment,
            validationReasonCodes = resolved.negativeSupervisionRecord.validationReasonCodes,
            evidenceReferenceIds = resolved.negativeSupervisionRecord.evidenceReferenceIds,
            downstreamRoute = resolved.negativeSupervisionRecord.downstreamRoute,
            candidateState = resolved.negativeSupervisionRecord.candidateState,
            rejectedTarget = requireNotNull(decision.rejectedTarget),
            boundaryType = requireNotNull(decision.boundaryType),
            occurrenceReference = resolved.contextProof.occurrenceReference,
            positiveTrainingExampleReference = resolved.positiveTrainingExample.exampleReference,
            materializationDecisionId = resolved.contextProof.materializationDecisionId,
            projectionInputBindingDigest = resolved.contextProof.projectionInputBindingDigest,
            proofBindingDigest = resolved.contextProof.bindingDigest,
            proofLogicalDigest = resolved.contextProof.logicalDigest,
        )
        val materializationId = materializationId(
            negativeSupervisionRecordId = lineage.negativeSupervisionRecordId,
            negativeExampleReference = negativeExample.reference.value,
        )
        return Result.Completed(
            Materialized(
                materializationId = materializationId,
                negativeTrainingExample = negativeExample,
                p1Lineage = lineage,
                evidenceBindings = evidenceBindings,
            ),
        )
    }

    private fun validateStructure(
        orchestration: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Completed,
    ): FailureReason? {
        val resolved = orchestration.proofAwareResolution
        val context = orchestration.downstreamBindingContext
        val decision = orchestration.downstreamBindingDecision
        if (
            resolved.bindingDecision.state !=
            HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.ResolutionState.RESOLVED ||
            resolved.bindingDecision.inputIdentity !=
            HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.InputIdentity.PROVABLE ||
            resolved.bindingDecision.evidenceRelationship !=
            HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.EvidenceRelationship.COMPATIBLE
        ) return FailureReason.INVALID_ORCHESTRATION_RESULT
        if (
            context.contractId != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.CONTRACT_ID ||
            context.version != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.VERSION ||
            context.state != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.STATE ||
            context.decisions.singleOrNull() != decision ||
            context.counters !=
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.Counters.from(context.decisions) ||
            context.logicalDigest !=
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.batchLogicalDigest(
                context.copy(logicalDigest = ""),
            )
        ) return FailureReason.INVALID_ORCHESTRATION_RESULT
        if (
            decision.recordId != resolved.negativeSupervisionRecord.recordId ||
            decision.state !=
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.BindingState.READY_FOR_NEGATIVE_TRAINING_EXAMPLE_ADAPTER ||
            decision.reasons.isNotEmpty() ||
            decision.positiveExampleReference != resolved.positiveTrainingExample.exampleReference ||
            decision.rejectedTarget == null ||
            decision.boundaryType == null ||
            decision.trainingProvenance != resolved.positiveTrainingExample.provenance
        ) return FailureReason.INVALID_ORCHESTRATION_RESULT
        return null
    }

    private fun validateLineage(
        orchestration: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Completed,
    ): FailureReason? {
        val resolved = orchestration.proofAwareResolution
        val proof = resolved.contextProof
        val record = resolved.negativeSupervisionRecord
        val proofDecision = resolved.bindingDecision
        val decision = orchestration.downstreamBindingDecision
        if (
            !validSha(record.recordId) ||
            !validSha(record.negativeCandidateId) ||
            !validSha(record.readinessDecisionId) ||
            !validSha(record.eligibilityInputBindingDigest) ||
            !validSha(record.eligibilityDecisionId) ||
            !validSha(record.validationBatchBindingDigest) ||
            !validSha(record.validationBatchLogicalDigest) ||
            !validSha(record.validationRecordId) ||
            record.canonicalEntityId.isBlank() ||
            record.reviewUnitId.isBlank() ||
            record.stableEntryId.isBlank() ||
            record.originalReviewerRef.isBlank() ||
            record.validatorReviewerRef.isBlank() ||
            record.validationRound < 1 ||
            record.validationRevision < 1 ||
            record.validationReasonCodes.isEmpty() ||
            record.evidenceReferenceIds.isEmpty() ||
            record.evidenceReferenceIds.distinct().size != record.evidenceReferenceIds.size ||
            record.candidateState != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE ||
            record.originalDecision != HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION ||
            record.downstreamRoute != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE
        ) return FailureReason.P1_LINEAGE_BINDING_MISMATCH
        if (
            proof.contractId != HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.CONTRACT_ID ||
            proof.version != HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.VERSION ||
            proof.state != HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.STATE ||
            proof.negativeSupervisionRecordId != record.recordId ||
            proof.negativeCandidateId != record.negativeCandidateId ||
            proof.readinessDecisionId != record.readinessDecisionId ||
            proof.eligibilityDecisionId != record.eligibilityDecisionId ||
            proof.validationBatchId != record.validationBatchId ||
            proof.validationBatchBindingDigest != record.validationBatchBindingDigest ||
            proof.validationBatchLogicalDigest != record.validationBatchLogicalDigest ||
            proof.eligibilityBatchId != record.eligibilityBatchId ||
            proof.eligibilityInputBindingDigest != record.eligibilityInputBindingDigest ||
            proof.validationRecordId != record.validationRecordId ||
            proof.reviewUnitId != record.reviewUnitId ||
            proof.stableEntryId != record.stableEntryId ||
            proof.canonicalEntityId != record.canonicalEntityId ||
            proof.originalReviewerRef != record.originalReviewerRef ||
            proof.validatorReviewerRef != record.validatorReviewerRef ||
            proof.validationRound != record.validationRound ||
            proof.validationRevision != record.validationRevision ||
            proof.positiveTrainingExampleReference != resolved.positiveTrainingExample.exampleReference ||
            proof.materializationDecisionId.isBlank() ||
            proof.occurrenceReference.isBlank() ||
            proof.rejectedTarget != proofDecision.rejectedTarget ||
            proof.boundaryType != proofDecision.boundaryType ||
            !validSha(proof.projectionInputBindingDigest) ||
            !validSha(proof.bindingDigest) ||
            !validSha(proof.logicalDigest) ||
            proof.identityProof.negativeSupervisionRecordId != record.recordId ||
            proof.identityProof.validationRecordId != record.validationRecordId ||
            proof.identityProof.reviewUnitId != record.reviewUnitId ||
            proof.identityProof.stableEntryId != record.stableEntryId ||
            proof.identityProof.canonicalEntityId != record.canonicalEntityId ||
            proof.identityProof.occurrenceReference != proof.occurrenceReference ||
            proof.identityProof.materializationDecisionId != proof.materializationDecisionId ||
            proof.identityProof.positiveTrainingExampleReference != resolved.positiveTrainingExample.exampleReference.value
        ) return FailureReason.P1_LINEAGE_BINDING_MISMATCH
        if (
            proofDecision.rejectedTarget != decision.rejectedTarget ||
            proofDecision.boundaryType != decision.boundaryType ||
            proofDecision.evidenceBindings != decision.evidenceBindings ||
            proofDecision.trainingProvenance != decision.trainingProvenance
        ) return FailureReason.P1_LINEAGE_BINDING_MISMATCH
        return null
    }

    private fun validateEvidence(
        orchestration: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Completed,
        evidenceBindings: List<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding>,
    ): FailureReason? {
        val resolved = orchestration.proofAwareResolution
        val decision = orchestration.downstreamBindingDecision
        val recordIds = resolved.negativeSupervisionRecord.evidenceReferenceIds
        val suppliedIds = evidenceBindings.map { it.persistedEvidenceReferenceId }
        val proofById = resolved.contextProof.evidenceBindings.associateBy { it.negativeEvidenceReferenceId }
        if (
            evidenceBindings.isEmpty() ||
            suppliedIds.any(String::isBlank) ||
            suppliedIds.distinct().size != suppliedIds.size ||
            suppliedIds.toSet() != recordIds.toSet() ||
            proofById.size != resolved.contextProof.evidenceBindings.size ||
            proofById.keys != recordIds.toSet()
        ) return FailureReason.EVIDENCE_BINDING_MISMATCH
        val inputEvidence = resolved.positiveTrainingExample.input.evidence.map { it.reference }.toSet()
        val provenanceEvidence = resolved.positiveTrainingExample.provenance.sourceEvidenceReferences.toSet()
        if (evidenceBindings.any { binding ->
                val proofBinding = proofById[binding.persistedEvidenceReferenceId]
                proofBinding == null ||
                    proofBinding.evidence != binding.trainingEvidence.reference ||
                    binding.trainingEvidence.reference !in inputEvidence ||
                    binding.trainingEvidence.reference !in provenanceEvidence
            }
        ) return FailureReason.EVIDENCE_BINDING_MISMATCH
        if (decision.evidenceBindings.map { it.persistedEvidenceReferenceId }.toSet() != recordIds.toSet()) {
            return FailureReason.EVIDENCE_BINDING_MISMATCH
        }
        return null
    }

    private fun materializationId(
        negativeSupervisionRecordId: String,
        negativeExampleReference: String,
    ): MaterializationReferenceV1 {
        val canonical = buildString {
            appendLine(MATERIALIZATION_ID_DOMAIN)
            appendLine("negativeSupervisionRecordId=$negativeSupervisionRecordId")
            appendLine("negativeExampleReference=$negativeExampleReference")
        }
        return MaterializationReferenceV1("negative-materialization:v1:${sha256(canonical)}")
    }

    private fun validSha(value: String): Boolean = SHA256.matches(value)

    private fun evidenceKey(reference: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference): String =
        "${reference.source}|${reference.sourceArtifactSha256.value}|${reference.sourceRecordIdentity}"

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    enum class FailureReason {
        INVALID_ORCHESTRATION_RESULT,
        P1_LINEAGE_BINDING_MISMATCH,
        EVIDENCE_BINDING_MISMATCH,
        GENERIC_NEGATIVE_EXAMPLE_REJECTED,
    }

    data class MaterializationReferenceV1(val value: String) {
        init {
            require(value.matches(Regex("negative-materialization:v1:[0-9a-f]{64}")))
        }
    }

    data class P1Lineage(
        val negativeSupervisionRecordId: String,
        val negativeCandidateId: String,
        val readinessDecisionId: String,
        val readinessState: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState,
        val eligibilityBatchId: String,
        val eligibilityInputBindingDigest: String,
        val eligibilityDecisionId: String,
        val eligibilityState: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State,
        val validationBatchId: String,
        val validationBatchBindingDigest: String,
        val validationBatchLogicalDigest: String,
        val validationRecordId: String,
        val reviewUnitId: String,
        val stableEntryId: String,
        val canonicalEntityId: String,
        val originalReviewerRef: String,
        val validatorReviewerRef: String,
        val validationRound: Int,
        val validationRevision: Int,
        val originalDecision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
        val assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1,
        val validationReasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1>,
        val evidenceReferenceIds: List<String>,
        val downstreamRoute: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1,
        val candidateState: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState,
        val rejectedTarget: HimTrainingTargetV1,
        val boundaryType: HimNegativeBoundaryTypeV1,
        val occurrenceReference: String,
        val positiveTrainingExampleReference: HimTrainingExampleReference,
        val materializationDecisionId: String,
        val projectionInputBindingDigest: String,
        val proofBindingDigest: String,
        val proofLogicalDigest: String,
    )

    data class Materialized(
        val materializationId: MaterializationReferenceV1,
        val negativeTrainingExample: HimNegativeTrainingExampleV1,
        val p1Lineage: P1Lineage,
        val evidenceBindings: List<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding>,
    )

    sealed interface Result {
        data class Completed(val value: Materialized) : Result

        data class Failed(val reason: FailureReason) : Result
    }
}
