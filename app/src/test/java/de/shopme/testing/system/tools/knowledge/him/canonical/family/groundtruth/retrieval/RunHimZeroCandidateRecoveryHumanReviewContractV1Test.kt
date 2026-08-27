package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewValidationErrorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewValidationResultV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewContractV1Test {
    @Test fun contractIdAndVersionAreFrozen() {
        assertEquals("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_CONTRACT_V1", HimZeroCandidateRecoveryHumanReviewContractV1.VERSION)
    }

    @Test fun primaryDecisionEnumIsExactlyFrozen() {
        assertEquals(
            listOf(
                "CONFIRM_ASSOCIATION", "REJECT_ASSOCIATION", "ABSTAIN_INSUFFICIENT_EVIDENCE",
                "ABSTAIN_AMBIGUOUS", "ABSTAIN_CONFLICTING_EVIDENCE", "ESCALATE_OUT_OF_SCOPE",
            ),
            HimZeroCandidateRecoveryHumanReviewDecisionV1.entries.map { it.name },
        )
    }

    @Test fun invalidReviewUnitIsNotADecision() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewDecisionV1.entries.any { it.name == "INVALID_REVIEW_UNIT" })
    }

    @Test fun correctionIsNotADecision() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewDecisionV1.entries.any { it.name == "CORRECT_TO_EXISTING_CANONICAL" })
    }

    @Test fun reviewUnitContainsOneEntryAndOneCanonical() {
        val unit = unit()
        assertEquals(ENTRY_ID, unit.stableEntryId)
        assertEquals(TARGET_ID, unit.canonicalEntityId)
    }

    @Test fun differentCanonicalTargetsHaveDifferentUnitIds() {
        assertNotEquals(unit("A1b2C3").reviewUnitId, unit("D4e5F6").reviewUnitId)
    }

    @Test fun reviewUnitIdIsDeterministicLowercaseSha256() {
        val first = unit().reviewUnitId
        assertEquals(first, unit().reviewUnitId)
        assertEquals(64, first.length)
        assertTrue(first.matches(Regex("[0-9a-f]{64}")))
    }

    @Test fun blankEntryIdIsTypedInvalid() {
        assertInvalid(unit(stableEntryId = "").validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.BLANK_STABLE_ENTRY_ID)
    }

    @Test fun blankCanonicalIdIsTypedInvalid() {
        assertInvalid(unit(canonicalEntityId = "").validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.BLANK_CANONICAL_ENTITY_ID)
    }

    @Test fun confirmWithDirectSourceAndTargetSupportIsValid() {
        assertValid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, confirmEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)).validate())
    }

    @Test fun confirmWithoutDirectSupportingEvidenceIsInvalid() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, emptyList(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.MISSING_REQUIRED_DIRECT_EVIDENCE)
    }

    @Test fun confirmWithDirectContradictionIsInvalid() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, contradictionEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.MISSING_REQUIRED_DIRECT_EVIDENCE)
    }

    @Test fun rejectWithDirectContradictingSourceAndTargetEvidenceIsValid() {
        assertValid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, rejectEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED)).validate())
    }

    @Test fun rejectAloneBecauseEvidenceIsMissingIsInvalid() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, emptyList(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.MISSING_REQUIRED_DIRECT_EVIDENCE)
    }

    @Test fun rejectWithOnlyIndirectEvidenceIsInvalid() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, listOf(evidence('a', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.INDIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION)), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_SEMANTIC_MISMATCH)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.MISSING_REQUIRED_DIRECT_EVIDENCE)
    }

    @Test fun rejectWithoutDirectContradictionIsInvalid() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, confirmEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_SEMANTIC_MISMATCH)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.REJECT_WITHOUT_DIRECT_CONTRADICTION)
    }

    @Test fun evidenceShortageIsOnlyAnInsufficientEvidenceAbstention() {
        assertValid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_INSUFFICIENT_EVIDENCE, emptyList(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.REQUIRED_DIRECT_EVIDENCE_MISSING)).validate())
    }

    @Test fun ambiguousRequiresEvidence() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS, emptyList(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.MULTIPLE_PLAUSIBLE_INTERPRETATIONS)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.MISSING_REQUIRED_DIRECT_EVIDENCE)
    }

    @Test fun conflictRequiresOpposingEvidence() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_CONFLICTING_EVIDENCE, confirmEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.BOUND_EVIDENCE_CONFLICT)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.CONFLICT_WITHOUT_OPPOSING_EVIDENCE)
    }

    @Test fun conflictWithOpposingEvidenceIsValid() {
        val refs = listOf(
            evidence('a', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
            evidence('b', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CROSS_SOURCE_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION),
        )
        assertValid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_CONFLICTING_EVIDENCE, refs, listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.BOUND_EVIDENCE_CONFLICT)).validate())
    }

    @Test fun outOfScopeRequiresStructuredReason() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE, emptyList(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INCOMPATIBLE_REASON_CODE)
    }

    @Test fun everyDecisionRejectsIncompatibleReasons() {
        HimZeroCandidateRecoveryHumanReviewDecisionV1.entries.forEach { decision ->
            val result = record(decision, if (decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_INSUFFICIENT_EVIDENCE || decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE) emptyList() else confirmEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)).validate()
            if (decision != HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION) assertFalse(result.valid)
        }
    }

    @Test fun emptyReasonsAreInvalid() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, confirmEvidence(), emptyList()).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.EMPTY_REASON_CODES)
    }

    @Test fun optionalNoteCannotReplaceStructuredSemantics() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, emptyList(), emptyList(), reviewerNote = "free text").validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.EMPTY_REASON_CODES)
    }

    @Test fun alternativeProposalIsOnlyAllowedWithReject() {
        val result = record(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, confirmEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED), proposal = proposal()).validate()
        assertInvalid(result, HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_ALTERNATIVE_CANONICAL_PROPOSAL)
    }

    @Test fun alternativeTargetCannotEqualRejectedTarget() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, rejectEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED), proposal = proposal(TARGET_ID)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_ALTERNATIVE_CANONICAL_PROPOSAL)
    }

    @Test fun alternativeProposalNeedsDirectSupportingSourceAndTargetEvidence() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, rejectEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED), proposal = proposal(), useProposalSupport = false).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_ALTERNATIVE_CANONICAL_PROPOSAL)
    }

    @Test fun alternativeProposalHasUnvalidatedState() {
        assertEquals("UNVALIDATED_REVIEW_PROPOSAL", proposal().state)
    }

    @Test fun oneReviewerRecordIsSingleCandidateOnly() {
        assertEquals("SINGLE_REVIEW_CANDIDATE_ONLY", record(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, confirmEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)).reviewState)
    }

    @Test fun oneReviewerDoesNotRequireSecondReviewer() {
        assertValid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, confirmEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)).validate())
    }

    @Test fun confirmRoutesOnlyToPositivePotential() {
        assertEquals(HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.POSITIVE_GOLD_CANDIDATE_AFTER_INDEPENDENT_VALIDATION, record(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, confirmEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)).downstreamRoute)
    }

    @Test fun rejectRoutesOnlyToNegativePotential() {
        assertEquals(HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.NEGATIVE_SUPERVISION_CANDIDATE_AFTER_INDEPENDENT_VALIDATION, record(HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, rejectEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED)).downstreamRoute)
    }

    @Test fun abstentionsAndEscalationRouteToNone() {
        HimZeroCandidateRecoveryHumanReviewDecisionV1.entries.filterNot { it == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION || it == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION }.forEach { decision ->
            assertEquals(HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.NONE, record(decision, if (decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS) listOf(evidence('a', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.INDIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY)) else emptyList(), listOf(reasonFor(decision))).downstreamRoute)
        }
    }

    @Test fun noDecisionProvidesAuthorityPotentialOrMutation() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.entries.any { it.name.contains("AUTHORITY") })
        assertFalse(HimZeroCandidateRecoveryHumanReviewDecisionV1.entries.any { it.name.contains("MUTATION") })
    }

    @Test fun reviewerEmailWhitespaceAndPathFormsAreInvalid() {
        listOf("person@example.test", "two words", "../local").forEach { reviewer ->
            assertInvalid(record(reviewerRef = reviewer, decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, evidence = confirmEvidence(), reasons = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_REVIEWER_REF)
        }
    }

    @Test fun contractFieldModelHasNoTimestamp() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewDecisionRecordV1::class.java.declaredFields.any { it.name.contains("timestamp", true) })
    }

    @Test fun contractFieldModelHasNoApprovalPublicationOrMutationState() {
        val fields = HimZeroCandidateRecoveryHumanReviewDecisionRecordV1::class.java.declaredFields.map { it.name.lowercase() }
        assertTrue(fields.none { it.contains("approval") || it.contains("publication") || it.contains("mutation") })
    }

    @Test fun durableRootIsUnderData() {
        assertTrue(HimZeroCandidateRecoveryHumanReviewContractV1.DURABLE_DECISION_BATCH_ROOT.startsWith("data/knowledge/"))
    }

    @Test fun derivedRootIsUnderBuildReports() {
        assertTrue(HimZeroCandidateRecoveryHumanReviewContractV1.DERIVED_REPORT_ROOT.startsWith("build/knowledge/reports/"))
    }

    @Test fun durableAndDerivedRootsDiffer() {
        assertNotEquals(HimZeroCandidateRecoveryHumanReviewContractV1.DURABLE_DECISION_BATCH_ROOT, HimZeroCandidateRecoveryHumanReviewContractV1.DERIVED_REPORT_ROOT)
    }

    @Test fun contractHasNoForbiddenOperationalDependencyNames() {
        val names = listOf(
            HimZeroCandidateRecoveryHumanReviewContractV1::class.java,
            HimZeroCandidateRecoveryHumanReviewDecisionRecordV1::class.java,
        ).flatMap { type -> type.declaredFields.map { it.type.simpleName } + type.declaredMethods.map { it.name } }
        assertTrue(names.none { name -> listOf("network", "openai", "sqlite", "store", "search", "fetch", "scan").any { token -> name.contains(token, true) } })
    }

    @Test fun validationDiagnosticsAreTypedAndSafe() {
        val invalid = assertIs<HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid>(unit(stableEntryId = "").validate())
        assertEquals(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.BLANK_STABLE_ENTRY_ID, invalid.error)
        assertFalse(invalid.error.name.contains("Exception") || invalid.error.name.contains("/"))
    }

    @Test fun publicDecisionRecordFieldModelIsFrozenToReviewInputs() {
        assertEquals(
            setOf("contractId", "reviewUnit", "reviewerRef", "reviewRound", "revision", "decision", "reasonCodes", "evidenceReferences", "alternativeCanonicalProposal", "reviewerNote"),
            HimZeroCandidateRecoveryHumanReviewDecisionRecordV1::class.java.declaredFields.filterNot { it.name.startsWith("$") }.map { it.name }.toSet(),
        )
    }

    @Test fun evidenceKindsDirectnessAndPositionsAreClosed() {
        assertEquals(5, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.entries.size)
        assertEquals(listOf("DIRECT", "INDIRECT"), HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.entries.map { it.name })
        assertEquals(listOf("SUPPORTS_ASSOCIATION", "CONTRADICTS_ASSOCIATION", "CONTEXT_ONLY"), HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.entries.map { it.name })
    }

    @Test fun duplicateEvidenceReferencesFailClosed() {
        val one = evidence('a', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION)
        val duplicate = one.copy(kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CROSS_SOURCE_RECORD)
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, listOf(one, duplicate), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.DUPLICATE_EVIDENCE_REFERENCE)
    }

    @Test fun duplicateReasonCodesFailClosed() {
        assertInvalid(record(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, confirmEvidence(), listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED, HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)).validate(), HimZeroCandidateRecoveryHumanReviewValidationErrorV1.DUPLICATE_REASON_CODE)
    }

    private fun assertValid(result: HimZeroCandidateRecoveryHumanReviewValidationResultV1) {
        assertTrue(result.valid)
        assertIs<HimZeroCandidateRecoveryHumanReviewValidationResultV1.Valid>(result)
    }

    private fun assertInvalid(result: HimZeroCandidateRecoveryHumanReviewValidationResultV1, expected: HimZeroCandidateRecoveryHumanReviewValidationErrorV1) {
        assertFalse(result.valid)
        assertEquals(expected, assertIs<HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid>(result).error)
    }

    private fun unit(canonicalEntityId: String = TARGET_ID, stableEntryId: String = ENTRY_ID) =
        HimZeroCandidateRecoveryHumanReviewReviewUnitV1(stableEntryId, canonicalEntityId)

    private fun record(
        decision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
        evidence: List<HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1>,
        reasons: List<HimZeroCandidateRecoveryHumanReviewReasonCodeV1>,
        reviewerRef: String = "reviewer-1",
        reviewerNote: String? = null,
        proposal: HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1? = null,
        useProposalSupport: Boolean = true,
    ) = HimZeroCandidateRecoveryHumanReviewDecisionRecordV1(
        HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
        unit(), reviewerRef, 1, 1, decision, reasons,
        if (proposal != null && useProposalSupport) evidence.toList() + proposalSupport() else evidence,
        proposal, reviewerNote,
    )

    private fun confirmEvidence() = listOf(
        evidence('a', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
        evidence('b', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
    )

    private fun rejectEvidence() = listOf(
        evidence('a', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION),
        evidence('b', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION),
    )

    private fun contradictionEvidence() = listOf(
        evidence('a', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION),
        evidence('b', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
    )

    private fun proposal(id: String = "D4e5F6") = HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1(id)

    private fun proposalSupport() = listOf(
        evidence('c', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
        evidence('d', HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
    )

    private fun evidence(
        seed: Char,
        kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
        directness: HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1,
        position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
    ) = HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1(
        seed.toString().repeat(64), kind, directness, position,
        "data/source/artifact.json", "record:$seed", seed.toString().repeat(64), null, listOf("identity.name"),
    )

    private fun reasonFor(decision: HimZeroCandidateRecoveryHumanReviewDecisionV1) = when (decision) {
        HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_INSUFFICIENT_EVIDENCE -> HimZeroCandidateRecoveryHumanReviewReasonCodeV1.REQUIRED_DIRECT_EVIDENCE_MISSING
        HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS -> HimZeroCandidateRecoveryHumanReviewReasonCodeV1.MULTIPLE_PLAUSIBLE_INTERPRETATIONS
        HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_CONFLICTING_EVIDENCE -> HimZeroCandidateRecoveryHumanReviewReasonCodeV1.BOUND_EVIDENCE_CONFLICT
        HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE -> HimZeroCandidateRecoveryHumanReviewReasonCodeV1.GOVERNANCE_DECISION_REQUIRED
        else -> HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED
    }

    private companion object {
        val ENTRY_ID = "a".repeat(64)
        const val TARGET_ID = "A1b2C3"
    }
}
