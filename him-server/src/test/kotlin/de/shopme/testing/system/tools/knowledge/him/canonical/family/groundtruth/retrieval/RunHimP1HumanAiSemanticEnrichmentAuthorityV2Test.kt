package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1AiSemanticProposalV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1HumanAiSemanticEnrichmentAuthorityV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1HumanAuthorizedSemanticEnrichmentV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1HumanConfirmationStatusV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1MaterializationEligibilityV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1SemanticProvenanceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimP1HumanReviewSemanticContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.RecordRelationV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.SemanticDecisionV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.SemanticRelationKindV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.SemanticRelationV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.SemanticResolutionV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HumanRationaleEvidenceV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimP1HumanAiSemanticEnrichmentAuthorityV2Test {
    @Test
    fun aiProposalIsNeverDirectTrainingAuthority() {
        val proposal = proposal(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION)

        assertEquals(HimP1SemanticProvenanceV2.AI_SEMANTIC_ENRICHMENT, proposal.provenance)
        assertFalse(proposal.canDirectlyEnterTraining)
    }

    @Test
    fun provenanceNamesAiAndNotHumanValidation() {
        assertEquals("AI_SEMANTIC_ENRICHMENT", HimP1HumanReviewSemanticContractV2.SECONDARY_ANALYSIS_TYPE)
        assertEquals("ChatGPT / OpenAI model", HimP1HumanReviewSemanticContractV2.SECONDARY_ANALYSIS_PROVIDER)
        assertFalse(HimP1HumanReviewSemanticContractV2.SECONDARY_ANALYSIS_IS_HUMAN)
        assertFalse(HimP1HumanReviewSemanticContractV2.SECONDARY_ANALYSIS_IS_INDEPENDENT_HUMAN_VALIDATION)
        assertFalse(HimP1HumanReviewSemanticContractV2.SECONDARY_ANALYSIS_IS_SAME_REVIEWER_SECOND_PASS)
        assertFalse(HimP1HumanReviewSemanticContractV2.SECONDARY_ANALYSIS_IS_TEMPORAL_HUMAN_CONSISTENCY_CHECK)
    }

    @Test
    fun humanRejectCannotBeOverriddenByAiConfirm() {
        val human = decision(HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION)
        val ai = proposal(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION)

        assertEquals(
            HimP1MaterializationEligibilityV2.HUMAN_AI_CONFLICT_REQUIRES_REVIEW,
            HimP1HumanAiSemanticEnrichmentAuthorityV2.classify(human, ai),
        )
    }

    @Test
    fun humanAbstainCannotBecomePositiveFromAiAlone() {
        val human = decision(HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS)
        val ai = proposal(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION)

        assertEquals(
            HimP1MaterializationEligibilityV2.EXCLUDED_HUMAN_ABSTAIN,
            HimP1HumanAiSemanticEnrichmentAuthorityV2.classify(human, ai),
        )
    }

    @Test
    fun escalationCannotBeAutoResolved() {
        val human = decision(HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE)
        val ai = proposal(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION)

        assertEquals(
            HimP1MaterializationEligibilityV2.EXCLUDED_ESCALATION,
            HimP1HumanAiSemanticEnrichmentAuthorityV2.classify(human, ai),
        )
    }

    @Test
    fun componentRelationRemainsAuxiliaryAndHasNoModelTarget() {
        val component = SemanticRelationV2(
            kind = SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
            resolution = SemanticResolutionV2.RESOLVED,
            targetReference = canonicalReference(),
            semanticLabel = "component or derived product",
        )
        val proposal = proposal(
            HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
            RecordRelationV2("evidence", listOf("evidence"), component),
        )

        assertTrue(HimP1HumanAiSemanticEnrichmentAuthorityV2.isAuxiliaryOnly(proposal))
        assertEquals(null, HimP1HumanReviewSemanticContractV2.targetForTraining(component))
    }

    @Test
    fun wrongProviderCannotMasqueradeAsAiAuthority() {
        assertThrows(IllegalArgumentException::class.java) {
            proposal(
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                provider = "human-reviewer:christian-glatschke:v1",
            )
        }
    }

    @Test
    fun missingHumanConfirmationFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            authorized(HimP1HumanConfirmationStatusV2.REQUIRED)
        }
    }

    @Test
    fun explicitHumanConfirmationCreatesDistinctAuthority() {
        val enrichment = authorized(HimP1HumanConfirmationStatusV2.CONFIRMED)

        assertEquals(HimP1SemanticProvenanceV2.HUMAN_CONFIRMED_AI_ENRICHMENT, enrichment.provenance)
        assertTrue(HimP1HumanAiSemanticEnrichmentAuthorityV2.canEnterTraining(enrichment))
        assertEquals(enrichment.aiProposal.semanticDecision.recordRelations, enrichment.recordRelations)
    }

    @Test
    fun modifiedHumanConfirmationPreservesAiProposalHistory() {
        val original = proposal(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION)
        val modified = authorized(HimP1HumanConfirmationStatusV2.CONFIRMED).copy(aiProposal = original)

        assertEquals(original, modified.aiProposal)
        assertEquals(HimP1SemanticProvenanceV2.HUMAN_CONFIRMED_AI_ENRICHMENT, modified.provenance)
    }

    @Test
    fun historicalHumanRecordRemainsSingleHumanReview() {
        val human = decision(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION)

        assertEquals("SINGLE_REVIEW_CANDIDATE_ONLY", human.reviewState)
        assertEquals("human-reviewer:christian-glatschke:v1", human.reviewerRef)
    }

    private fun authorized(status: HimP1HumanConfirmationStatusV2): HimP1HumanAuthorizedSemanticEnrichmentV2 =
        HimP1HumanAuthorizedSemanticEnrichmentV2(
            humanDecision = decision(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION),
            humanReviewerRef = "human-reviewer:christian-glatschke:v1",
            humanRationale = "The human explicitly confirms the structured semantic relation.",
            sourceEvidence = decision(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION).evidenceReferences,
            aiProposal = proposal(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION),
            confirmationStatus = status,
        )

    private fun proposal(
        decision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
        recordRelation: RecordRelationV2? = null,
        provider: String = HimP1HumanReviewSemanticContractV2.SECONDARY_ANALYSIS_PROVIDER,
    ): HimP1AiSemanticProposalV2 {
        val semantic = SemanticDecisionV2(
            contractId = HimP1HumanReviewSemanticContractV2.CONTRACT_ID,
            version = HimP1HumanReviewSemanticContractV2.VERSION,
            packetId = "human-review-expansion-v2:ABC123",
            candidateId = "ABC123",
            candidateCanonicalId = "AbCd12",
            stableEntryId = "a".repeat(64),
            reviewUnitId = decision(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION).reviewUnit.reviewUnitId,
            v1BatchId = "human-batch-v1",
            v1BatchLogicalDigest = HimSha256("b".repeat(64)),
            v1DecisionRecordIdentity = "human-decision-identity",
            originalReviewerRef = "human-reviewer:christian-glatschke:v1",
            originalReviewRound = 1,
            originalRevision = 1,
            originalDecision = decision,
            aggregateRelation = resolvedRelation(),
            recordRelations = listOfNotNull(recordRelation),
            evidenceReferenceIds = listOf("evidence"),
            rationaleEvidence = HumanRationaleEvidenceV2(
                reviewerRef = "human-reviewer:christian-glatschke:v1",
                sourceDecisionIdentity = "human-decision-identity",
                text = "AI structured proposal retained as non-human enrichment.",
            ),
        )
        return HimP1AiSemanticProposalV2(
            sourceBatchId = "ai-batch-v2",
            sourceBatchLogicalDigest = HimSha256("c".repeat(64)),
            reviewUnitId = semantic.reviewUnitId,
            semanticDecision = semantic,
            provider = provider,
        )
    }

    private fun decision(decision: HimZeroCandidateRecoveryHumanReviewDecisionV1): HimZeroCandidateRecoveryHumanReviewDecisionRecordV1 =
        HimZeroCandidateRecoveryHumanReviewDecisionRecordV1(
            contractId = HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
            reviewUnit = HimZeroCandidateRecoveryHumanReviewReviewUnitV1("a".repeat(64), "AbCd12"),
            reviewerRef = "human-reviewer:christian-glatschke:v1",
            reviewRound = 1,
            revision = 1,
            decision = decision,
            reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED),
            evidenceReferences = listOf(evidence()),
            reviewerNote = "Human review authority.",
        )

    private fun evidence(): HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1 =
        HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1(
            evidenceReferenceId = "evidence".padEnd(64, 'e'),
            kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD,
            directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
            position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION,
            artifactReference = "review-corpus.v1.json",
            recordReference = "off:product:row:1",
            artifactSha256 = "d".repeat(64),
            artifactLogicalDigest = "e".repeat(64),
            fieldReferences = listOf("productName"),
        )

    private fun resolvedRelation(): SemanticRelationV2 = SemanticRelationV2(
        kind = SemanticRelationKindV2.IDENTITY_OF,
        resolution = SemanticResolutionV2.RESOLVED,
        targetReference = canonicalReference(),
        semanticLabel = "identity",
    )

    private fun canonicalReference() = HimFamilyEntityReference.Canonical(HimEntityId("AbCd12"))
}
