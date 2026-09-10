package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256

/**
 * Additive provenance and authorization boundary for the Batch-1 review data.
 *
 * The V1 record is human authority. The V2 semantic decision is an AI proposal
 * and becomes training-eligible only after an explicit human confirmation.
 */
object HimP1HumanAiSemanticEnrichmentAuthorityV2 {
    const val CONTRACT_ID = "HIM_P1_HUMAN_AI_SEMANTIC_ENRICHMENT_AUTHORITY_V2"
    const val VERSION = "2"
    const val AI_PROVIDER = "ChatGPT / OpenAI model"

    fun canEnterTraining(enrichment: HimP1HumanAuthorizedSemanticEnrichmentV2): Boolean =
        enrichment.confirmationStatus == HimP1HumanConfirmationStatusV2.CONFIRMED &&
            enrichment.humanDecision.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION &&
            enrichment.aiProposal.semanticDecision.originalDecision ==
            HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION &&
            !enrichment.isAuxiliaryOnly

    fun classify(
        humanDecision: HimZeroCandidateRecoveryHumanReviewDecisionRecordV1,
        aiProposal: HimP1AiSemanticProposalV2,
    ): HimP1MaterializationEligibilityV2 = when {
        humanDecision.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE ->
            HimP1MaterializationEligibilityV2.EXCLUDED_ESCALATION
        humanDecision.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION &&
            aiProposal.semanticDecision.originalDecision ==
            HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION ->
            HimP1MaterializationEligibilityV2.HUMAN_AI_CONFLICT_REQUIRES_REVIEW
        humanDecision.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION ->
            HimP1MaterializationEligibilityV2.EXCLUDED_HUMAN_REJECT
        humanDecision.decision != HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION ->
            HimP1MaterializationEligibilityV2.EXCLUDED_HUMAN_ABSTAIN
        else -> HimP1MaterializationEligibilityV2.AI_PROPOSAL_REQUIRES_HUMAN_CONFIRMATION
    }

    fun isAuxiliaryOnly(aiProposal: HimP1AiSemanticProposalV2): Boolean =
        aiProposal.semanticDecision.recordRelations.isNotEmpty() &&
            aiProposal.semanticDecision.recordRelations.all {
                it.relation.kind == SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF
            }
}

enum class HimP1SemanticProvenanceV2 {
    HUMAN_REVIEW,
    AI_SEMANTIC_ENRICHMENT,
    HUMAN_CONFIRMED_AI_ENRICHMENT,
    AUXILIARY_SEMANTIC_ONLY,
}

enum class HimP1HumanConfirmationStatusV2 {
    NOT_REQUIRED,
    REQUIRED,
    CONFIRMED,
    REJECTED,
    KEEP_UNRESOLVED,
}

enum class HimP1MaterializationEligibilityV2 {
    DIRECTLY_HUMAN_AUTHORIZED_STRUCTURED_SEMANTICS,
    AI_PROPOSAL_REQUIRES_HUMAN_CONFIRMATION,
    HUMAN_AI_CONFLICT_REQUIRES_REVIEW,
    EXCLUDED_HUMAN_REJECT,
    EXCLUDED_HUMAN_ABSTAIN,
    EXCLUDED_ESCALATION,
    AUXILIARY_ONLY_NOT_MODEL_TARGET,
}

data class HimP1AiSemanticProposalV2(
    val sourceBatchId: String,
    val sourceBatchLogicalDigest: HimSha256,
    val reviewUnitId: String,
    val semanticDecision: SemanticDecisionV2,
    val provider: String = HimP1HumanAiSemanticEnrichmentAuthorityV2.AI_PROVIDER,
) {
    init {
        require(sourceBatchId.isNotBlank())
        require(sourceBatchLogicalDigest.value.matches(Regex("[0-9a-f]{64}")))
        require(provider == HimP1HumanAiSemanticEnrichmentAuthorityV2.AI_PROVIDER)
        require(semanticDecision.reviewUnitId == reviewUnitId)
        semanticDecision.validate()
    }

    val provenance: HimP1SemanticProvenanceV2
        get() = HimP1SemanticProvenanceV2.AI_SEMANTIC_ENRICHMENT

    val canDirectlyEnterTraining: Boolean
        get() = false
}

data class HimP1HumanAuthorizedSemanticEnrichmentV2(
    val humanDecision: HimZeroCandidateRecoveryHumanReviewDecisionRecordV1,
    val humanReviewerRef: String,
    val humanRationale: String,
    val sourceEvidence: List<HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1>,
    val aiProposal: HimP1AiSemanticProposalV2,
    val confirmationStatus: HimP1HumanConfirmationStatusV2,
    val isAuxiliaryOnly: Boolean = HimP1HumanAiSemanticEnrichmentAuthorityV2.isAuxiliaryOnly(aiProposal),
) {
    init {
        require(humanReviewerRef == humanDecision.reviewerRef)
        require(humanRationale.isNotBlank())
        require(sourceEvidence == humanDecision.evidenceReferences)
        require(aiProposal.reviewUnitId == humanDecision.reviewUnit.reviewUnitId)
        require(confirmationStatus == HimP1HumanConfirmationStatusV2.CONFIRMED)
    }

    val provenance: HimP1SemanticProvenanceV2
        get() = if (isAuxiliaryOnly) {
            HimP1SemanticProvenanceV2.AUXILIARY_SEMANTIC_ONLY
        } else {
            HimP1SemanticProvenanceV2.HUMAN_CONFIRMED_AI_ENRICHMENT
        }

    val recordRelations: List<RecordRelationV2>
        get() = aiProposal.semanticDecision.recordRelations
}
