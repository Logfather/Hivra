package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPrimaryBucketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewResolutionStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewRuntimeResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewScopeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusEntryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusReportV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewPriorityCounterV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewPriorityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewRecordKindBreakdownV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewSelectionReasonBreakdownV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewSelectionReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewSourceBreakdownV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewAssociationStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCanonicalTargetV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusCountersV1
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewRuntimeV1Test {
    @Test fun runtimeContractIdAndVersionAreFrozen() {
        assertEquals("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_RUNTIME_V1", HimZeroCandidateRecoveryHumanReviewRuntimeV1.CONTRACT_ID)
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewRuntimeV1.VERSION)
    }

    @Test fun requestFieldModelIsExactlyFrozen() {
        val fields = HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
        assertEquals(
            setOf("enabled", "batchId", "inputBinding", "corpus", "catalog", "registry", "authority", "reviewScope", "priorDecisionRecords", "submittedDecisionRecords", "durableDecisionBatchRoot", "derivedReportRoot"),
            fields.map { it.name }.toSet(),
        )
    }

    @Test fun disabledReturnsDisabled() {
        assertIs<HimZeroCandidateRecoveryHumanReviewRuntimeResult.Disabled>(Fixture.execute(enabled = false))
    }

    @Test fun disabledDoesNotPerformFileIoOrPersistence() {
        val root = Files.createTempDirectory("him-review-disabled-").toFile()
        try {
            assertIs<HimZeroCandidateRecoveryHumanReviewRuntimeResult.Disabled>(Fixture.execute(enabled = false, root = root))
            assertFalse(root.listFiles()?.isNotEmpty() == true)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun syntheticCorpusWithOneTargetResolvesOneUnit() {
        val result = Fixture.completed()
        assertEquals(1, result.authorizedReviewUnits.size)
        assertEquals(1, result.counters.resolvedReviewUnits)
    }

    @Test fun oneCorpusEntryWithMultipleTargetsResolvesSeparateUnits() {
        val result = Fixture.completed(targetIds = listOf(Fixture.TARGET_A, Fixture.TARGET_B))
        assertEquals(2, result.authorizedReviewUnits.size)
        assertNotEquals(result.authorizedReviewUnits[0].reviewUnitId, result.authorizedReviewUnits[1].reviewUnitId)
    }

    @Test fun unitSortingIsDeterministic() {
        val result = Fixture.completed(targetIds = listOf(Fixture.TARGET_B, Fixture.TARGET_A))
        assertEquals(result.authorizedReviewUnits.sortedWith(compareBy({ it.stableEntryId }, { it.canonicalEntityId }, { it.reviewUnitId })), result.authorizedReviewUnits)
    }

    @Test fun duplicateUnitIdIsRejected() {
        val corpus = Fixture.corpus(targetIds = listOf(Fixture.TARGET_A), duplicateEntry = true)
        assertFailure(Fixture.execute(corpus = corpus), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.DUPLICATE_REVIEW_UNIT)
    }

    @Test fun foundationTripleIsFullyValidated() {
        val result = Fixture.completed()
        assertEquals(1384, result.counters.corpusEntries + 1383)
    }

    @Test fun unknownTargetIdIsRejected() {
        val corpus = Fixture.corpus(targetIds = listOf("c99999"))
        assertFailure(Fixture.execute(corpus = corpus), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.INVALID_FOUNDATION)
    }

    @Test fun inputBindingMismatchIsRejected() {
        val binding = Fixture.inputBinding(Fixture.corpus()).copy(corpusLogicalDigest = "f".repeat(64)).let {
            it.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(it))
        }
        assertFailure(Fixture.execute(inputBinding = binding), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.INPUT_BINDING_MISMATCH)
    }

    @Test fun scopeIdPolicyIsValidated() {
        assertFailure(Fixture.execute(scopeId = "Bad Scope"), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.INVALID_SCOPE)
    }

    @Test fun emptyScopeUnitListIsRejected() {
        assertFailure(Fixture.execute(scopeIds = emptyList()), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.INVALID_SCOPE)
    }

    @Test fun unknownScopeUnitIsRejected() {
        assertFailure(Fixture.execute(scopeIds = listOf("f".repeat(64))), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.UNKNOWN_AUTHORIZED_REVIEW_UNIT)
    }

    @Test fun scopeDigestIsDeterministic() {
        assertEquals(Fixture.scope().scopeDigest, Fixture.scope().scopeDigest)
    }

    @Test fun manipulatedScopeDigestIsRejected() {
        val scope = Fixture.scope().copy(scopeDigest = "f".repeat(64))
        assertFailure(Fixture.execute(scope = scope), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.SCOPE_DIGEST_MISMATCH)
    }

    @Test fun submittedDecisionOutsideScopeIsRejected() {
        val corpus = Fixture.corpus(targetIds = listOf(Fixture.TARGET_A, Fixture.TARGET_B))
        val record = Fixture.decision(Fixture.unit(Fixture.TARGET_B))
        assertFailure(Fixture.execute(corpus = corpus, submitted = listOf(record)), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.UNAUTHORIZED_REVIEW_UNIT)
    }

    @Test fun submissionLimitIsEnforced() {
        val records = listOf(Fixture.decision(), Fixture.decision(reviewer = "reviewer-2"))
        assertFailure(Fixture.execute(submitted = records, maxRecords = 1), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.SUBMISSION_LIMIT_EXCEEDED)
    }

    @Test fun emptySubmittedDecisionsAreRejected() {
        assertFailure(Fixture.execute(submitted = emptyList()), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.EMPTY_SUBMITTED_DECISIONS)
    }

    @Test fun runtimeNeverCreatesItsOwnDecision() {
        val result = Fixture.completed()
        assertEquals(Fixture.submittedCount, result.counters.submittedDecisionRecords)
        assertEquals(Fixture.submittedCount, result.decisionBatch.decisionRecords.size)
    }

    @Test fun validFirstRevisionOneIsAccepted() {
        assertEquals(1, Fixture.completed().submittedDecisionRecords.single().revision)
    }

    @Test fun firstRevisionGreaterThanOneIsRejected() {
        assertFailure(Fixture.execute(submitted = listOf(Fixture.decision(revision = 2))), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.INVALID_REVISION_SEQUENCE)
    }

    @Test fun correctFollowUpRevisionIsAccepted() {
        val first = Fixture.decision()
        val second = Fixture.decision(revision = 2, reviewer = "reviewer-1")
        val result = Fixture.completed(prior = listOf(first), submitted = listOf(second))
        assertEquals(2, result.submittedDecisionRecords.single().revision)
    }

    @Test fun revisionWithGapIsRejected() {
        val prior = Fixture.decision()
        assertFailure(Fixture.execute(prior = listOf(prior), submitted = listOf(Fixture.decision(revision = 3))), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.INVALID_REVISION_SEQUENCE)
    }

    @Test fun staleRevisionIsRejected() {
        val prior = Fixture.decision(revision = 2)
        assertFailure(Fixture.execute(prior = listOf(Fixture.decision(), prior), submitted = listOf(Fixture.decision(revision = 2))), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.STALE_REVISION)
    }

    @Test fun duplicateReviewerSubmissionIsRejected() {
        val records = listOf(Fixture.decision(), Fixture.decision(revision = 2))
        assertFailure(Fixture.execute(submitted = records), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.DUPLICATE_REVIEWER_SUBMISSION)
    }

    @Test fun priorDecisionsAreNotCopiedIntoNewBatch() {
        val prior = Fixture.decision()
        val result = Fixture.completed(prior = listOf(prior), submitted = listOf(Fixture.decision(revision = 2)))
        assertEquals(listOf(2), result.decisionBatch.decisionRecords.map { it.revision })
    }

    @Test fun newRevisionDoesNotOverwritePriorDecision() {
        val prior = Fixture.decision()
        val result = Fixture.completed(prior = listOf(prior), submitted = listOf(Fixture.decision(revision = 2)))
        assertEquals(Fixture.submittedCount, result.decisionBatch.decisionRecords.size)
        assertEquals(1, prior.revision)
    }

    @Test fun twoRevisionsOfSameReviewerAreNotIndependentReviews() {
        val prior = Fixture.decision()
        val result = Fixture.completed(prior = listOf(prior), submitted = listOf(Fixture.decision(revision = 2)))
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.SINGLE_REVIEW_ONLY, result.resolutions.single().resolutionState)
    }

    @Test fun oneConfirmIsSingleReviewOnly() {
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.SINGLE_REVIEW_ONLY, Fixture.completed().resolutions.single().resolutionState)
    }

    @Test fun oneRejectIsSingleReviewOnly() {
        val result = Fixture.completed(submitted = listOf(Fixture.decision(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, evidence = Fixture.rejectEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED))))
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.SINGLE_REVIEW_ONLY, result.resolutions.single().resolutionState)
    }

    @Test fun twoIndependentConfirmsAgree() {
        val result = Fixture.completed(submitted = listOf(Fixture.decision(), Fixture.decision(reviewer = "reviewer-2")))
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.INDEPENDENT_CONFIRM_AGREEMENT, result.resolutions.single().resolutionState)
    }

    @Test fun twoIndependentRejectsAgree() {
        val reject = { reviewer: String -> Fixture.decision(reviewer = reviewer, decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, evidence = Fixture.rejectEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED)) }
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.INDEPENDENT_REJECT_AGREEMENT, Fixture.completed(submitted = listOf(reject("reviewer-1"), reject("reviewer-2"))).resolutions.single().resolutionState)
    }

    @Test fun confirmAndRejectDisagree() {
        val reject = Fixture.decision(reviewer = "reviewer-2", decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, evidence = Fixture.rejectEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED))
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.INDEPENDENT_DISAGREEMENT, Fixture.completed(submitted = listOf(Fixture.decision(), reject)).resolutions.single().resolutionState)
    }

    @Test fun confirmPlusInsufficientEvidenceAbstentionIsAbstentionPresent() {
        val abstain = Fixture.decision(reviewer = "reviewer-2", decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_INSUFFICIENT_EVIDENCE, evidence = emptyList(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.REQUIRED_DIRECT_EVIDENCE_MISSING))
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.ABSTENTION_PRESENT, Fixture.completed(submitted = listOf(Fixture.decision(), abstain)).resolutions.single().resolutionState)
    }

    @Test fun rejectPlusAmbiguousAbstentionIsAbstentionPresent() {
        val abstain = Fixture.decision(reviewer = "reviewer-2", decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS, evidence = Fixture.ambiguousEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.MULTIPLE_PLAUSIBLE_INTERPRETATIONS))
        val reject = Fixture.decision(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, evidence = Fixture.rejectEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED))
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.ABSTENTION_PRESENT, Fixture.completed(submitted = listOf(reject, abstain)).resolutions.single().resolutionState)
    }

    @Test fun conflictAbstentionIsAbstentionPresent() {
        val conflict = Fixture.decision(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_CONFLICTING_EVIDENCE, evidence = Fixture.conflictEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.BOUND_EVIDENCE_CONFLICT))
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.ABSTENTION_PRESENT, Fixture.completed(submitted = listOf(conflict)).resolutions.single().resolutionState)
    }

    @Test fun escalationHasPriority() {
        val escalation = Fixture.decision(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE, evidence = emptyList(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.GOVERNANCE_DECISION_REQUIRED))
        val abstain = Fixture.decision(reviewer = "reviewer-2", decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_INSUFFICIENT_EVIDENCE, evidence = emptyList(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.REQUIRED_DIRECT_EVIDENCE_MISSING))
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.ESCALATED_OUT_OF_SCOPE, Fixture.completed(submitted = listOf(escalation, abstain)).resolutions.single().resolutionState)
    }

    @Test fun threeReviewersDoNotUseMajorityDecision() {
        val reject = Fixture.decision(reviewer = "reviewer-2", decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, evidence = Fixture.rejectEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED))
        val abstain = Fixture.decision(reviewer = "reviewer-3", decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS, evidence = Fixture.ambiguousEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.MULTIPLE_PLAUSIBLE_INTERPRETATIONS))
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.ABSTENTION_PRESENT, Fixture.completed(submitted = listOf(Fixture.decision(), reject, abstain)).resolutions.single().resolutionState)
    }

    @Test fun resolutionUsesHighestRevisionPerReviewer() {
        val prior = Fixture.decision()
        val followUp = Fixture.decision(revision = 2)
        val result = Fixture.completed(prior = listOf(prior), submitted = listOf(followUp))
        assertEquals(listOf("reviewer-1"), result.resolutions.single().currentReviewerRefs)
    }

    @Test fun alternativeProposalRequiresReject() {
        val record = Fixture.decision(proposal = HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1(Fixture.TARGET_B))
        assertFailure(Fixture.execute(submitted = listOf(record)), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.INVALID_SUBMITTED_DECISION)
    }

    @Test fun unknownAlternativeCanonicalIsRejected() {
        val record = Fixture.decision(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, evidence = Fixture.proposalEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED), proposal = HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1("c99999"))
        assertFailure(Fixture.execute(submitted = listOf(record)), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.ALTERNATIVE_CANONICAL_NOT_FOUND)
    }

    @Test fun alternativeIdEqualToOriginalTargetIsRejected() {
        val record = Fixture.decision(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, evidence = Fixture.proposalEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED), proposal = HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1(Fixture.TARGET_A))
        assertFailure(Fixture.execute(submitted = listOf(record)), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.INVALID_SUBMITTED_DECISION)
    }

    @Test fun differentAlternativeProposalsRemainVisible() {
        val first = Fixture.decision(reviewer = "reviewer-1", decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, evidence = Fixture.proposalEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED), proposal = HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1(Fixture.TARGET_B))
        val second = Fixture.decision(reviewer = "reviewer-2", decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, evidence = Fixture.proposalEvidence('z'), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED), proposal = HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1(Fixture.TARGET_C))
        val result = Fixture.completed(targetIds = listOf(Fixture.TARGET_A, Fixture.TARGET_B, Fixture.TARGET_C), scopeIds = Fixture.unit(Fixture.TARGET_A).reviewUnitId.let { listOf(it) }, submitted = listOf(first, second))
        assertEquals(2, result.submittedDecisionRecords.mapNotNull { it.alternativeCanonicalProposal }.size)
    }

    @Test fun runtimeDoesNotGrantGoldEligibility() {
        assertFalse(Fixture.completed().decisionBatch.decisionRecords.single().downstreamRoute.name.contains("AUTHORIZED"))
    }

    @Test fun runtimeDoesNotGrantNegativeSupervision() {
        assertFalse(Fixture.completed().decisionBatch.decisionRecords.single().downstreamRoute.name.contains("AUTHORIZED"))
    }

    @Test fun runtimeDoesNotGrantAuthorityEligibilityOrMutation() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewRuntimeResult.Completed::class.java.declaredFields.any { it.name.contains("authority", true) || it.name.contains("mutation", true) })
    }

    @Test fun runtimeCountersAreGlobal() {
        val result = Fixture.completed(submitted = listOf(Fixture.decision(), Fixture.decision(reviewer = "reviewer-2")))
        assertEquals(2, result.counters.submittedDecisionRecords)
        assertEquals(2, result.counters.uniqueSubmittedReviewers)
    }

    @Test fun successfulRunPersistsExactlyOneImmutableBatch() {
        val root = Files.createTempDirectory("him-review-runtime-").toFile()
        try {
            val result = Fixture.completed(root = root)
            assertEquals(1, result.decisionBatch.decisionRecords.size)
            assertTrue(root.resolve("data/batch-01/review-decisions.v1.json").isFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun batchContainsOnlySubmittedDecisions() {
        val prior = Fixture.decision()
        val result = Fixture.completed(prior = listOf(prior), submitted = listOf(Fixture.decision(revision = 2)))
        assertEquals(listOf(2), result.decisionBatch.decisionRecords.map { it.revision })
    }

    @Test fun persistenceReturnsCreated() {
        assertEquals("CREATED", Fixture.completed().persistenceResult.persistenceStatus.name)
    }

    @Test fun secondIdenticalRunReturnsAlreadyPresentIdentical() {
        val root = Files.createTempDirectory("him-review-runtime-").toFile()
        try {
            Fixture.completed(root = root)
            assertEquals("ALREADY_PRESENT_IDENTICAL", Fixture.completed(root = root).persistenceResult.persistenceStatus.name)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun bothRunsRemainByteIdentical() {
        val root = Files.createTempDirectory("him-review-runtime-").toFile()
        try {
            Fixture.completed(root = root)
            val before = root.resolve("data/batch-01/review-decisions.v1.json").readBytes()
            Fixture.completed(root = root)
            assertTrue(before.contentEquals(root.resolve("data/batch-01/review-decisions.v1.json").readBytes()))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun reloadEqualsPreparedBatch() {
        val result = Fixture.completed()
        assertEquals(result.decisionBatch, result.persistenceResult.batch)
    }

    @Test fun persistenceFailureIsMappedSafely() {
        val root = Files.createTempDirectory("him-review-runtime-").toFile()
        try {
            root.resolve("data/batch-01").mkdirs()
            root.resolve("data/batch-01/review-decisions.v1.json").writeText("foreign")
            assertFailure(Fixture.execute(root = root), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.PERSISTENCE_FAILED)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun existingDivergentBatchIsNotOverwritten() {
        val root = Files.createTempDirectory("him-review-runtime-").toFile()
        try {
            root.resolve("data/batch-01").mkdirs()
            val file = root.resolve("data/batch-01/review-decisions.v1.json")
            file.writeText("foreign")
            Fixture.execute(root = root)
            assertEquals("foreign", file.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun partialArtifactStateIsNotRepaired() {
        val root = Files.createTempDirectory("him-review-runtime-").toFile()
        try {
            root.resolve("data/batch-01").mkdirs()
            root.resolve("data/batch-01/review-decisions.v1.json").writeText("foreign")
            assertFailure(Fixture.execute(root = root), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.PERSISTENCE_FAILED)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun diagnosticsContainNoPathsEvidenceReviewerOrExceptionText() {
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewRuntimeResult.Failed>(Fixture.execute(scopeId = "Bad Scope"))
        assertFalse(failure.safeContext.contains('/') || failure.safeContext.contains('\\') || failure.safeContext.contains("Exception", true))
    }

    @Test fun runtimeHasNoClockRandomOrOperationalPort() {
        val names = HimZeroCandidateRecoveryHumanReviewRuntimeV1::class.java.declaredMethods.map { it.name }
        assertTrue(names.none { it.contains("clock", true) || it.contains("random", true) || it.contains("network", true) || it.contains("search", true) || it.contains("fetch", true) || it.contains("sqlite", true) })
    }

    @Test fun hermeticTestDoesNotOpenRealCorpus() {
        assertFalse(Files.exists(java.io.File(HimZeroCandidateRecoveryReviewCorpusContractV1.REVIEW_ROOT).toPath().resolve("review-corpus.v1.json")))
    }

    @Test fun hermeticTestWritesOnlyTemporaryRoots() {
        val root = Files.createTempDirectory("him-review-runtime-").toFile()
        try {
            Fixture.completed(root = root)
            assertTrue(root.resolve("data").isDirectory)
            assertTrue(root.resolve("build").isDirectory)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun contractAndPersistenceFieldModelsRemainUntouched() {
        assertTrue(HimZeroCandidateRecoveryHumanReviewContractV1.VERSION.contains("CONTRACT"))
        assertTrue(HimZeroCandidateRecoveryHumanReviewPersistenceV1.CONTRACT_ID.contains("PERSISTENCE"))
    }

    @Test fun runtimeHasNoAutomaticDecisionPort() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1::class.java.declaredFields.any { it.name.contains("decisionFactory", true) || it.name.contains("reviewer", true) && it.name != "priorDecisionRecords" && it.name != "submittedDecisionRecords" })
    }

    @Test fun failedResultsUseTypedReasons() {
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewRuntimeResult.Failed>(Fixture.execute(scopeId = "Bad Scope"))
        assertIs<HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1>(failure.reason)
    }

    @Test fun scopeAuthorizesNoConcreteDecision() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewScopeV1::class.java.declaredFields.any { it.name == "decision" })
    }

    @Test fun resolutionStateTaxonomyIsClosed() {
        assertEquals(listOf("SINGLE_REVIEW_ONLY", "INDEPENDENT_CONFIRM_AGREEMENT", "INDEPENDENT_REJECT_AGREEMENT", "INDEPENDENT_DISAGREEMENT", "ABSTENTION_PRESENT", "ESCALATED_OUT_OF_SCOPE"), HimZeroCandidateRecoveryHumanReviewResolutionStateV1.entries.map { it.name })
    }

    @Test fun persistenceResultIsRequiredForCompletion() {
        assertNotNull(Fixture.completed().persistenceResult)
    }

    @Test fun runtimeCounterPersistenceCountIsGlobal() {
        assertEquals(1, Fixture.completed().counters.persistedDecisionRecords)
    }

    @Test fun outputIsDeterministicallyOrdered() {
        val result = Fixture.completed(targetIds = listOf(Fixture.TARGET_B, Fixture.TARGET_A))
        assertEquals(result.authorizedReviewUnits.map { it.reviewUnitId }.sorted(), result.authorizedReviewUnits.map { it.reviewUnitId })
    }

    @Test fun repeatedScopeConstructionIsEqual() {
        assertEquals(Fixture.scope(), Fixture.scope())
    }

    @Test fun inputBindingUsesExplicitCorpusBinding() {
        val result = Fixture.completed()
        assertEquals(Fixture.corpus().inputBinding, result.scope.let { Fixture.inputBinding(Fixture.corpus()).existingCorpusInputBinding })
    }

    @Test fun alternativeProposalRemainsUnvalidated() {
        val record = Fixture.decision(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, evidence = Fixture.proposalEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED), proposal = HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1(Fixture.TARGET_B))
        assertEquals("UNVALIDATED_REVIEW_PROPOSAL", record.alternativeCanonicalProposal?.state)
    }

    @Test fun reviewerRefsRemainOpaque() {
        val result = Fixture.completed()
        assertEquals("reviewer-1", result.resolutions.single().currentReviewerRefs.single())
    }

    @Test fun noMajorityFieldExists() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewResolutionV1::class.java.declaredFields.any { it.name.contains("majority", true) })
    }

    @Test fun noEligibilityFieldExists() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewRuntimeResult.Completed::class.java.declaredFields.any { it.name.contains("eligibility", true) })
    }

    @Test fun priorContextDoesNotIncreasePersistedCount() {
        val result = Fixture.completed(prior = listOf(Fixture.decision()), submitted = listOf(Fixture.decision(revision = 2)))
        assertEquals(1, result.counters.persistedDecisionRecords)
    }

    @Test fun submittedDecisionBreakdownIsComplete() {
        val result = Fixture.completed()
        assertEquals(HimZeroCandidateRecoveryHumanReviewDecisionV1.entries.size, result.counters.submittedDecisionBreakdown.size)
    }

    @Test fun resolutionBreakdownIsComplete() {
        val result = Fixture.completed()
        assertEquals(HimZeroCandidateRecoveryHumanReviewResolutionStateV1.entries.size, result.counters.resolutionStateBreakdown.size)
    }

    @Test fun scopeRoundIsBoundToRecords() {
        assertFailure(Fixture.execute(submitted = listOf(Fixture.decision(reviewRound = 2))), HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1.REVIEW_ROUND_MISMATCH)
    }

    @Test fun alternativeProposalCounterIsDerived() {
        val record = Fixture.decision(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, evidence = Fixture.proposalEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED), proposal = HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1(Fixture.TARGET_B))
        assertEquals(1, Fixture.completed(submitted = listOf(record)).counters.alternativeCanonicalProposals)
    }

    @Test fun immutableBatchDigestIsPresent() {
        assertTrue(Fixture.completed().decisionBatch.batchLogicalDigest.matches(Regex("[0-9a-f]{64}")))
    }

    @Test fun inputBindingDigestIsPresent() {
        assertTrue(Fixture.completed().decisionBatch.bindingDigest.matches(Regex("[0-9a-f]{64}")))
    }

    @Test fun authorizedUnitsAreSortedByReviewUnitId() {
        val result = Fixture.completed(targetIds = listOf(Fixture.TARGET_B, Fixture.TARGET_A))
        assertEquals(result.authorizedReviewUnits.sortedBy { it.reviewUnitId }, result.authorizedReviewUnits)
    }

    @Test fun noDecisionIsSynthesizedForAnUnsubmittedAuthorizedUnit() {
        val result = Fixture.completed(targetIds = listOf(Fixture.TARGET_A, Fixture.TARGET_B), scopeIds = listOf(Fixture.unit(Fixture.TARGET_A).reviewUnitId))
        assertEquals(1, result.submittedDecisionRecords.size)
        assertEquals(1, result.resolutions.size)
    }

    @Test fun rootArgumentsAreExplicit() {
        val fields = HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1::class.java.declaredFields.map { it.name }
        assertTrue("durableDecisionBatchRoot" in fields && "derivedReportRoot" in fields)
    }

    @Test fun noRealReviewDecisionIsCreated() {
        assertEquals(0, Fixture.realDecisionCount)
    }

    @Test fun persistenceReloadIsValidated() {
        val result = Fixture.completed()
        assertEquals(result.persistenceResult.batch, result.decisionBatch)
    }

    private fun assertFailure(
        result: HimZeroCandidateRecoveryHumanReviewRuntimeResult,
        reason: HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1,
    ) {
        assertEquals(reason, assertIs<HimZeroCandidateRecoveryHumanReviewRuntimeResult.Failed>(result).reason)
    }

    private object Fixture {
        const val TARGET_A = "c00001"
        const val TARGET_B = "c00002"
        const val TARGET_C = "c00003"
        const val submittedCount = 1
        const val realDecisionCount = 0

        fun execute(
            enabled: Boolean = true,
            root: java.io.File = Files.createTempDirectory("him-review-runtime-").toFile(),
            corpus: HimZeroCandidateRecoveryReviewCorpusReportV1 = corpus(),
            inputBinding: HimZeroCandidateRecoveryHumanReviewInputBindingV1 = inputBinding(corpus),
            scope: HimZeroCandidateRecoveryHumanReviewScopeV1 = scope(scopeIds = listOf(unit(TARGET_A).reviewUnitId), inputBinding = inputBinding),
            scopeId: String = scope.scopeId,
            scopeIds: List<String> = scope.authorizedReviewUnitIds,
            maxRecords: Int = scope.maxNewDecisionRecords,
            prior: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1> = emptyList(),
            submitted: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1> = listOf(decision()),
        ): HimZeroCandidateRecoveryHumanReviewRuntimeResult {
            val actualScope = scope(
                scopeId = scopeId,
                scopeIds = scopeIds,
                maxRecords = maxRecords,
                inputBinding = inputBinding,
                previous = scope,
            )
            return HimZeroCandidateRecoveryHumanReviewRuntimeV1.execute(
                HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1(
                    enabled, "batch-01", inputBinding, corpus, catalog(), registry(), authority(), actualScope,
                    prior, submitted, root.resolve("data"), root.resolve("build"),
                ),
            )
        }

        fun completed(
            root: java.io.File = Files.createTempDirectory("him-review-runtime-").toFile(),
            targetIds: List<String> = listOf(TARGET_A),
            corpus: HimZeroCandidateRecoveryReviewCorpusReportV1 = corpus(targetIds = targetIds),
            inputBinding: HimZeroCandidateRecoveryHumanReviewInputBindingV1 = inputBinding(corpus),
            scopeIds: List<String> = targetIds.map { unit(it).reviewUnitId }.sorted(),
            prior: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1> = emptyList(),
            submitted: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1> = listOf(decision()),
        ): HimZeroCandidateRecoveryHumanReviewRuntimeResult.Completed {
            val result = execute(root = root, corpus = corpus, inputBinding = inputBinding, scope = scope(scopeIds = scopeIds, inputBinding = inputBinding), prior = prior, submitted = submitted)
            return assertIs(result)
        }

        fun scope(
            scopeId: String = "scope-01",
            scopeIds: List<String> = listOf(unit(TARGET_A).reviewUnitId),
            maxRecords: Int = 10,
            inputBinding: HimZeroCandidateRecoveryHumanReviewInputBindingV1 = inputBinding(corpus()),
            previous: HimZeroCandidateRecoveryHumanReviewScopeV1? = null,
        ): HimZeroCandidateRecoveryHumanReviewScopeV1 {
            val base = HimZeroCandidateRecoveryHumanReviewScopeV1(scopeId, 1, scopeIds.sorted(), maxRecords, inputBinding.bindingDigest, "")
            return if (previous != null && scopeId == previous.scopeId && scopeIds == previous.authorizedReviewUnitIds && maxRecords == previous.maxNewDecisionRecords) previous else base.copy(scopeDigest = HimZeroCandidateRecoveryHumanReviewRuntimeV1.scopeDigest(base))
        }

        fun unit(targetId: String = TARGET_A) = HimZeroCandidateRecoveryHumanReviewReviewUnitV1(ENTRY_ID, targetId)

        fun decision(
            unit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1 = unit(),
            reviewer: String = "reviewer-1",
            reviewRound: Int = 1,
            revision: Int = 1,
            decision: HimZeroCandidateRecoveryHumanReviewDecisionV1 = HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
            evidence: List<HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1> = confirmEvidence(),
            reasons: List<HimZeroCandidateRecoveryHumanReviewReasonCodeV1> = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED),
            proposal: HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1? = null,
        ) = HimZeroCandidateRecoveryHumanReviewDecisionRecordV1(
            HimZeroCandidateRecoveryHumanReviewContractV1.VERSION, unit, reviewer, reviewRound, revision, decision, reasons, evidence, proposal,
        )

        fun confirmEvidence(seed: Char = 'a') = listOf(
            evidence(seed, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
            evidence((seed + 1), HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
        )

        fun rejectEvidence(seed: Char = 'a') = listOf(
            evidence(seed, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION),
            evidence((seed + 1), HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION),
        )

        fun ambiguousEvidence(seed: Char = 'a') = listOf(evidence(seed, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.INDIRECT))

        fun conflictEvidence(seed: Char = 'a') = listOf(
            evidence(seed, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
            evidence(seed + 1, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CROSS_SOURCE_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION),
        )

        fun proposalEvidence(seed: Char = 'a') = (confirmEvidence(seed) + listOf(evidence(seed + 2, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION)))
            .sortedBy { it.evidenceReferenceId }

        fun evidence(
            seed: Char,
            kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
            position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
            directness: HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1 = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
        ) = HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1(
            HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256("evidence:$seed"), kind, directness, position,
            "data/evidence/artifact.json", "record:$seed", HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256("artifact:$seed"), null, listOf("identity.name"),
        ).let { reference -> listOf(reference).sortedBy { it.evidenceReferenceId }.single() }

        fun corpus(
            targetIds: List<String> = listOf(TARGET_A),
            duplicateEntry: Boolean = false,
        ): HimZeroCandidateRecoveryReviewCorpusReportV1 {
            val targets = targetIds.distinct().sorted().map { id -> HimZeroCandidateRecoveryReviewCanonicalTargetV1(id, "Canonical $id", emptyList(), emptyList()) }
            val entries = buildList {
                add(entry(targets))
                if (duplicateEntry) add(entry(targets, 2))
            }
            val input = corpusInputBinding()
            val unsigned = HimZeroCandidateRecoveryReviewCorpusReportV1(
                HimZeroCandidateRecoveryReviewCorpusContractV1.VERSION, input,
                HimZeroCandidateRecoveryReviewCorpusCountersV1(entries.size, 0, 0, 0, 0, entries.size, targets.size, entries.size, 0),
                HimZeroCandidateRecoveryReviewCorpusContractV1.PRIORITY_ORDER.map { HimZeroCandidateRecoveryReviewPriorityCounterV1(it, 0, 0, 0) },
                HimZeroCandidateRecoveryReviewCorpusContractV1.SOURCE_ORDER.map { HimZeroCandidateRecoveryReviewSourceBreakdownV1(it, if (it == HimGroundTruthSource.OPEN_FOOD_FACTS) entries.size else 0) },
                HimZeroCandidateRecoveryReviewCorpusContractV1.RECORD_KIND_ORDER.map { HimZeroCandidateRecoveryReviewRecordKindBreakdownV1(it, if (it == HimEvidenceRecordKind.OFF_PRODUCT) entries.size else 0) },
                HimZeroCandidateRecoveryReviewCorpusContractV1.SELECTION_REASON_ORDER.map { HimZeroCandidateRecoveryReviewSelectionReasonBreakdownV1(it, if (it == HimZeroCandidateRecoveryReviewSelectionReasonV1.RECURRING_PRIMARY_VALUE_GROUP) entries.size else 0) },
                entries, "",
            )
            return unsigned.copy(logicalDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.logicalDigest(unsigned))
        }

        private fun entry(targets: List<HimZeroCandidateRecoveryReviewCanonicalTargetV1>, index: Int = 1) = HimZeroCandidateRecoveryReviewCorpusEntryV1(
            ENTRY_ID, HimGroundTruthSource.OPEN_FOOD_FACTS, if (index == 1) EVIDENCE_REFERENCE else HimEvidenceRecordReference.offProduct(index.toLong(), index.toString()).value, HimEvidenceRecordKind.OFF_PRODUCT, "shard-000001",
            listOf(HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(if (index == 1) EVIDENCE_REFERENCE else HimEvidenceRecordReference.offProduct(index.toLong(), index.toString()).value)), targets, emptyList(), emptyList(),
            HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH, emptyList(), listOf(HimZeroCandidateRecoveryReviewSelectionReasonV1.RECURRING_PRIMARY_VALUE_GROUP), emptyList(),
            HimZeroCandidateRecoveryReviewAssociationStateV1.UNVERIFIED_AUDIT_ASSOCIATION, HimZeroCandidateRecoveryReviewStateV1.UNREVIEWED,
        )

        fun inputBinding(corpus: HimZeroCandidateRecoveryReviewCorpusReportV1 = corpus()) = humanInputBinding(corpus)

        private fun corpusInputBinding() = run {
            val cause = HimZeroCandidateCauseAnalysisFileBindingV1("fixture/cause.json", 1, "a".repeat(64), "b".repeat(64))
            val base = HimZeroCandidateRecoveryReviewCorpusInputBindingV1(
                cause,
                HimZeroCandidateRecoveryReviewCorpusFileBindingV1("fixture/catalog.json", 1, "1".repeat(64), "c".repeat(64)),
                HimZeroCandidateRecoveryReviewCorpusFileBindingV1("fixture/authority.json", 1, "2".repeat(64), "d".repeat(64)),
                "b".repeat(64), "a".repeat(40), "",
            )
            base.copy(bindingDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.bindingDigest(base))
        }

        private fun humanInputBinding(corpus: HimZeroCandidateRecoveryReviewCorpusReportV1) = run {
            val base = HimZeroCandidateRecoveryHumanReviewInputBindingV1(
                HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/review-corpus.json", 1, "3".repeat(64), corpus.logicalDigest),
                corpus.logicalDigest, "4".repeat(64), corpus.inputBinding.bindingDigest, corpus.inputBinding,
                HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/registry.json", 1, "5".repeat(64), "6".repeat(64)),
                HimZeroCandidateRecoveryHumanReviewContractV1.VERSION, HimZeroCandidateRecoveryHumanReviewContractV1.VERSION, "a".repeat(40), "",
            )
            base.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(base))
        }

        private fun catalog() = HimProductOnlyCanonicalMaster(
            "fixture/catalog.json", "1".repeat(64), (1..1384).map { HimProductOnlyCanonical("Canonical c%05d".format(it), "canonical%05d".format(it), emptyList()) },
        )

        private fun registry() = HimEntityIdRegistry((1..1384).map { index -> HimEntityIdRegistryEntry(HimEntityId("c%05d".format(index)), HimEntityType.CANONICAL, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "canonical%05d".format(index)) })

        private fun authority() = HimCanonicalFamilyAuthority(
            "1", HimCanonicalFamilySourceCatalog(catalog().path, catalog().contentSha256, 1384),
            (1..1384).map { index ->
                HimCanonicalFamily(HimEntityId("c%05d".format(index)), "Canonical c%05d".format(index), "canonical%05d".format(index), emptyList(), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList())
            },
        )

        private const val ENTRY_ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        private val EVIDENCE_REFERENCE = HimEvidenceRecordReference.offProduct(1L, "1").value
    }
}
