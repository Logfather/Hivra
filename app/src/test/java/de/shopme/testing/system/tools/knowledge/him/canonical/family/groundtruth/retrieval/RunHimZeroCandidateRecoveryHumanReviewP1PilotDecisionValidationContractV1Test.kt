package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1Test {
    @Test
    fun contractIdentityAndStateAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_CONTRACT_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.VERSION)
        assertEquals(
            "INDEPENDENT_DECISION_VALIDATION_CONTEXT_ONLY",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.STATE,
        )
    }

    @Test
    fun committedInputBindingIsFrozen() {
        val input = inputBinding()
        assertEquals("p1-artischocken-brie-logfather-r1-v1", input.batchId)
        assertEquals(29094L, input.batchFileBinding.byteSize)
        assertEquals(
            "2a892ea6591967fdcbb439f63a3c1d3ef04d041ac10af4863c7c22d4ff140342",
            input.batchFileBinding.sha256,
        )
        assertEquals("12021b21c18e9394cbb9d27a645ddeaf922675cd1dd3cfb1f5e3a63a83bedc23", input.originalInputBindingDigest)
        assertEquals("53052a8bcceafdade7c7c597750e3497d8ca377369915502257c1427f8106c02", input.originalBatchLogicalDigest)
        assertEquals(4, input.originalSelections.size)
        assertEquals(12, input.evidenceBindings.size)
        assertEquals(input.bindingDigest, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.inputBindingDigest(input.copy(bindingDigest = "")))
    }

    @Test
    fun originalDecisionsAndReviewUnitsAreBoundExactly() {
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            ),
            inputBinding().originalSelections.map { it.decision },
        )
        assertEquals(4, inputBinding().originalSelections.map { it.reviewUnitId }.distinct().size)
        assertTrue(inputBinding().originalSelections.all { it.alternativeCanonicalProposal == null })
    }

    @Test
    fun originalRejectReasonsRemainBound() {
        val rejects = inputBinding().originalSelections.filter { it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION }
        assertEquals(2, rejects.size)
        assertTrue(rejects.all { it.reasonCodes.map(Enum<*>::name) == listOf("DIRECT_SEMANTIC_MISMATCH", "DIRECT_ASSOCIATION_CONTRADICTED") })
    }

    @Test
    fun validationAssessmentsAreClosedAndTyped() {
        assertEquals(
            listOf(
                "VALIDATE_ORIGINAL_DECISION",
                "CHALLENGE_ORIGINAL_DECISION",
                "ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE",
                "ESCALATE_VALIDATION_CONFLICT",
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.entries.map { it.name },
        )
        assertEquals(10, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.entries.size)
        assertEquals(4, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.entries.size)
    }

    @Test
    fun independentValidatorIsAcceptedAndSelfValidationFailsClosed() {
        assertValid(validBatch())
        assertFailure(
            batchWithValidator("reviewer:logfather:v1"),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATOR_NOT_INDEPENDENT,
        )
    }

    @Test
    fun reviewerReferenceRoundAndRevisionAreExact() {
        assertFailure(batchWithValidator(""), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATOR_REVIEWER_REF)
        assertFailure(batchWithRound(2), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_ROUND)
        assertFailure(batchWithRevision(2), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_REVISION)
    }

    @Test
    fun validConfirmAndRejectPathsProduceOnlyPotentialRoutes() {
        val batch = validBatch()
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_POSITIVE_GOLD_CANDIDATE,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_POSITIVE_GOLD_CANDIDATE,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE,
            ),
            batch.records.map { it.downstreamRoute },
        )
        assertTrue(batch.records.all { it.downstreamRoute.name.contains("POTENTIAL") })
        assertValid(batch)
    }

    @Test
    fun routeDerivationIsDeterministicForEveryAssessment() {
        val confirm = HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION
        val reject = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION
        val validate = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_POSITIVE_GOLD_CANDIDATE, derive(confirm, validate))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE, derive(reject, validate))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.REQUIRES_ADJUDICATION, derive(confirm, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.NOT_ELIGIBLE, derive(reject, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.REQUIRES_ADJUDICATION, derive(confirm, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ESCALATE_VALIDATION_CONFLICT))
    }

    @Test
    fun confirmRequiresDirectSupportingEvidence() {
        val unit = inputBinding().originalSelections[0]
        val contextOnly = evidenceIds(unit).filter { id -> inputBinding().evidenceBindings.single { it.evidenceReferenceId == id }.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY }
        assertFailure(
            batchReplacing(0, record(0, evidenceReferenceIds = contextOnly)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INSUFFICIENT_DIRECT_EVIDENCE,
        )
    }

    @Test
    fun rejectRequiresDirectContradictingEvidence() {
        val unit = inputBinding().originalSelections[1]
        val supportOnly = evidenceIds(unit).filter { id -> inputBinding().evidenceBindings.single { it.evidenceReferenceId == id }.position != HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION }
        assertFailure(
            batchReplacing(1, record(1, evidenceReferenceIds = supportOnly)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INSUFFICIENT_DIRECT_EVIDENCE,
        )
    }

    @Test
    fun confirmRejectAndReasonCodesMustMatchEvidence() {
        val confirm = record(0, reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED))
        assertFailure(batchReplacing(0, confirm), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES)
        val reject = record(1, reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION))
        assertFailure(batchReplacing(1, reject), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES)
    }

    @Test
    fun challengeRequiresAdjudicationReasonAndEvidence() {
        assertFailure(
            batchReplacing(
                0,
                record(
                    0,
                    assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION,
                    reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION),
                ),
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES,
        )
        assertFailure(
            batchReplacing(
                0,
                record(
                    0,
                    assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION,
                    reasonCodes = listOf(
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.CONFLICTING_DIRECT_EVIDENCE,
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REQUIRES_ADJUDICATION,
                    ),
                    evidenceReferenceIds = emptyList(),
                ),
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_EVIDENCE_REFERENCE,
        )
    }

    @Test
    fun abstentionAndEscalationNeedTypedReasonsAndNoCandidateRoute() {
        val abstain = record(
            0,
            assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE,
            reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.INSUFFICIENT_DIRECT_EVIDENCE),
            evidenceReferenceIds = emptyList(),
        )
        val abstainBatch = batchReplacing(0, abstain)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.NOT_ELIGIBLE, abstain.downstreamRoute)
        assertValid(abstainBatch)

        val escalation = record(
            0,
            assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ESCALATE_VALIDATION_CONFLICT,
            reasonCodes = listOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.CONFLICTING_DIRECT_EVIDENCE,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REQUIRES_ADJUDICATION,
            ),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.REQUIRES_ADJUDICATION, escalation.downstreamRoute)
        assertValid(batchReplacing(0, escalation))

        assertFailure(
            batchReplacing(
                0,
                record(
                    0,
                    assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE,
                    reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.CONFLICTING_DIRECT_EVIDENCE),
                    evidenceReferenceIds = emptyList(),
                ),
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES,
        )
    }

    @Test
    fun evidenceMustBeKnownUniqueAndUnitLocal() {
        val unit = inputBinding().originalSelections[0]
        val refs = evidenceIds(unit)
        assertFailure(
            batchReplacing(0, record(0, evidenceReferenceIds = refs + refs.first())),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_EVIDENCE_REFERENCE,
        )
        assertFailure(
            batchReplacing(0, record(0, evidenceReferenceIds = listOf(evidenceIds(inputBinding().originalSelections[1]).first()))),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.CROSS_UNIT_EVIDENCE_REFERENCE,
        )
        assertFailure(
            batchReplacing(0, record(0, evidenceReferenceIds = listOf("f".repeat(64)))),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_EVIDENCE_REFERENCE,
        )
    }

    @Test
    fun validationRecordIdentityIsDeterministicAndSemanticInputsChangeIt() {
        val selection = inputBinding().originalSelections[0]
        val first = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validationRecordId(
            inputBinding(), selection, VALIDATOR, 1, 1,
        )
        val second = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validationRecordId(
            inputBinding(), selection, VALIDATOR, 1, 1,
        )
        assertEquals(first, second)
        assertTrue(first.matches(Regex("[0-9a-f]{64}")))
        assertNotEquals(
            first,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validationRecordId(
                inputBinding(), selection, "reviewer:independent-v2", 1, 1,
            ),
        )
        assertNotEquals(
            first,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validationRecordId(
                inputBinding(), selection, VALIDATOR, 1, 2,
            ),
        )
    }

    @Test
    fun recordsAndBatchAreDeterministicallySortedAndDigested() {
        val records = validRecords()
        val first = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
            inputBinding(), VALIDATOR, 1, 1, records,
        )
        val second = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
            inputBinding(), VALIDATOR, 1, 1, records.reversed(),
        )
        assertEquals(first, second)
        assertEquals(first.bindingDigest, second.bindingDigest)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertValid(first)
    }

    @Test
    fun countersAreDerivedFromRecordsOnly() {
        val batch = validBatch()
        assertEquals(4, batch.counters.totalValidationRecords)
        assertEquals(4, batch.counters.independentlyValidatedRecords)
        assertEquals(0, batch.counters.challengedRecords)
        assertEquals(0, batch.counters.abstainedRecords)
        assertEquals(0, batch.counters.escalatedRecords)
        assertEquals(2, batch.counters.potentialPositiveGoldCandidates)
        assertEquals(2, batch.counters.potentialNegativeSupervisionCandidates)
        assertEquals(0, batch.counters.requiresAdjudicationRecords)
        assertEquals(0, batch.counters.notEligibleRecords)
        assertEquals(4, batch.counters.distinctReviewUnits)
        assertEquals(4, batch.counters.distinctOriginalDecisionIdentities)
        assertEquals(12, batch.counters.referencedEvidenceCount)
        assertEquals(12, batch.counters.distinctReferencedEvidenceCount)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.deriveCounters(batch.records),
            batch.counters,
        )
    }

    @Test
    fun changedCountersAndDigestsFailClosed() {
        val batch = validBatch()
        assertFailure(
            batch.copy(counters = batch.counters.copy(totalValidationRecords = 0)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_COUNTERS,
        )
        assertFailure(
            batch.copy(bindingDigest = "0".repeat(64)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATION_BINDING_DIGEST_MISMATCH,
        )
        assertFailure(
            batch.copy(logicalDigest = "0".repeat(64)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATION_LOGICAL_DIGEST_MISMATCH,
        )
    }

    @Test
    fun inputAndBatchIdentityFieldsFailClosed() {
        assertFailure(validBatch().copy(contractId = "wrong"), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_CONTRACT_ID)
        assertFailure(validBatch().copy(version = "2"), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_CONTRACT_VERSION)
        assertFailure(validBatch().copy(state = "PROMOTED"), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_STATE)
        assertFailure(
            validBatch().copy(inputBinding = inputBinding().copy(batchId = "other")),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_INPUT_BINDING,
        )
        assertFailure(
            validBatch().copy(inputBinding = inputBinding().copy(bindingDigest = "0".repeat(64))),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INPUT_BINDING_DIGEST_MISMATCH,
        )
    }

    @Test
    fun recordCountOrderAndIdentityAreFailClosed() {
        val batch = validBatch()
        assertFailure(
            batch.copy(records = batch.records.dropLast(1)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RECORD_COUNT,
        )
        assertFailure(
            batch.copy(records = batch.records.reversed()),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RECORD_ORDER,
        )
        assertFailure(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
                inputBinding(), VALIDATOR, 1, 1, listOf(batch.records[0], batch.records[0], batch.records[2], batch.records[3]),
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.DUPLICATE_VALIDATION_IDENTITY,
        )
    }

    @Test
    fun originalDecisionAndRationaleAreValidated() {
        val original = inputBinding().originalSelections[0]
        val foreign = original.copy(canonicalEntityId = "foreign")
        val foreignRecord = record(0, originalDecision = foreign)
        assertFailure(batchReplacing(0, foreignRecord), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.ORIGINAL_DECISION_MISMATCH)

        val badRationale = record(0, rationale = "  repeated  whitespace")
        assertFailure(batchReplacing(0, badRationale), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RATIONALE)
        assertFailure(batchReplacing(0, record(0, rationale = "/private/tmp/secret")), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RATIONALE)
        assertFailure(batchReplacing(0, record(0, rationale = "exception: local stacktrace")), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RATIONALE)
        assertFailure(batchReplacing(0, record(0, rationale = "VALIDATE_ORIGINAL_DECISION")), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RATIONALE)
    }

    @Test
    fun duplicateReasonsAndRecordIdsAreRejected() {
        val record = validRecords()[0]
        assertFailure(
            batchReplacing(
                0,
                record.copy(reasonCodes = listOf(
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION,
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION,
                )),
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES,
        )
        assertFailure(
            batchReplacing(0, record.copy(validationRecordId = "0".repeat(64))),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_RECORD_ID,
        )
    }

    @Test
    fun forbiddenDownstreamSemanticsAreNotModelFields() {
        val forbidden = setOf(
            "goldLabel", "goldApproved", "negativeSupervisionRecord", "trainingApproved", "trainingSplit", "modelTarget",
            "authorityMutation", "catalogMutation", "registryMutation", "publicationApproved", "automaticPromotion",
            "automaticAdjudication", "validatorAssignment",
        )
        val modelClasses = listOf(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1::class.java,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1::class.java,
        )
        val fieldNames = modelClasses.flatMap { it.declaredFields.map { field -> field.name } }.toSet()
        assertTrue(forbidden.intersect(fieldNames).isEmpty())
        assertFalse(fieldNames.any { it.contains("gold", ignoreCase = true) && it != "POTENTIAL_POSITIVE_GOLD_CANDIDATE" })
    }

    @Test
    fun validationResultIsTypedAndSafe() {
        val valid = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validate(validBatch())
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Valid>(valid)
        val invalid = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validate(validBatch().copy(state = "wrong"))
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Invalid>(invalid)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_STATE, failure.reason)
    }

    @Test
    fun digestDomainsAndNoRealValidatorAreExplicit() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_BINDING_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.INPUT_BINDING_DIGEST_DOMAIN,
        )
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_RECORD_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.RECORD_ID_DOMAIN,
        )
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_LOGICAL_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.BATCH_LOGICAL_DIGEST_DOMAIN,
        )
        assertNotEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.ORIGINAL_REVIEWER_REF,
            VALIDATOR,
        )
        assertTrue(inputBinding().originalSelections.all { it.evidenceReferenceIds.isNotEmpty() })
    }

    private fun inputBinding() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING

    private fun validBatch() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
        inputBinding(), VALIDATOR, 1, 1, validRecords(),
    )

    private fun validRecords() = inputBinding().originalSelections.mapIndexed { index, selection -> record(index, selection) }

    private fun record(
        index: Int,
        originalDecision: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1 = inputBinding().originalSelections[index],
        assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1 = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
        reasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1> = if (originalDecision.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION) {
            listOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
            )
        } else {
            listOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
            )
        },
        evidenceReferenceIds: List<String> = evidenceIds(originalDecision),
        rationale: String = "Independent evidence review for unit $index",
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createValidationRecord(
        inputBinding(), originalDecision, VALIDATOR, 1, 1, assessment, reasonCodes, evidenceReferenceIds, rationale,
    )

    private fun evidenceIds(selection: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1) = selection.evidenceReferenceIds.sorted()

    private fun batchReplacing(index: Int, replacement: de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
            inputBinding(), VALIDATOR, 1, 1, validRecords().toMutableList().also { it[index] = replacement },
        )

    private fun batchWithValidator(validator: String) =
        validBatch().copy(validatorReviewerRef = validator)

    private fun batchWithRound(round: Int) =
        validBatch().copy(validationRound = round)

    private fun batchWithRevision(revision: Int) =
        validBatch().copy(validationRevision = revision)

    private fun derive(
        decision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
        assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.deriveRoute(decision, assessment)

    private fun assertValid(batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1) {
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Valid>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validate(batch),
        )
    }

    private fun assertFailure(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
        expected: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1,
    ) {
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Invalid>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validate(batch),
        )
        assertEquals(expected, failure.reason)
    }

    private companion object {
        const val VALIDATOR = "reviewer:independent-v1"
    }
}
