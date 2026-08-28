package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPrimaryBucketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionValidationResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewFileBindingV1
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1Test {
    @Test
    fun contractIdentityAndHumanInputStateAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_SUBMISSION_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.VERSION)
        assertEquals("HUMAN_INPUT_ONLY", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.STATE)
    }

    @Test
    fun fourTypedAssessmentsAreBoundInPacketOrderAndDeterministic() {
        val first = completed(submission())
        val second = completed(submission())
        assertEquals(listOf("unit-1", "unit-2", "unit-3", "unit-4"), first.assessments.map { it.reviewUnitId })
        assertEquals(first, second)
        assertEquals(first, assertValid(first))
        assertTrue(first.assessments.all { it.assessment in HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_ASSESSMENTS })
        assertTrue(first.assessments.all { it.reasonCodes.all { code -> code in HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_REASON_CODES } })
        assertTrue(first.submissionId.matches(Regex("[0-9a-f]{64}")))
        assertTrue(first.inputBindingDigest.matches(Regex("[0-9a-f]{64}")))
        assertTrue(first.logicalDigest.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun validatorIsExplicitIndependentAndNoRouteOrRecordIsMaterialized() {
        val result = createSubmission("reviewer:synthetic-independent:v1")
        val submission = completed(result)
        assertEquals("reviewer:synthetic-independent:v1", submission.validatorReviewerRef)
        assertFalse(submission.assessments.any { it::class.simpleName?.contains("Record") == true })
        assertFalse(submission.toString().contains("downstreamRoute"))
    }

    @Test
    fun deterministicDigestsBindIdentityAssessmentsReasonsEvidenceAndRationale() {
        val original = completed(submission())
        val changed = completed(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.create(
                packet(),
                "reviewer:synthetic-independent:v1",
                1,
                1,
                inputs().mapIndexed { index, input -> if (index == 0) input.copy(rationale = "Changed rationale") else input },
            ),
        )
        assertNotEquals(original.logicalDigest, changed.logicalDigest)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.inputBindingDigest(original),
            original.inputBindingDigest,
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.logicalDigest(original),
            original.logicalDigest,
        )
    }

    @Test
    fun missingBlankMalformedAndNonIndependentValidatorFailClosed() {
        assertFailure(createSubmission(null), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_VALIDATOR_REVIEWER)
        assertFailure(createSubmission(""), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_VALIDATOR_REVIEWER)
        assertFailure(createSubmission("reviewer:logfather:v1"), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.VALIDATOR_NOT_INDEPENDENT)
        assertFailure(createSubmission("reviewer/invalid"), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_VALIDATOR_REVIEWER)
    }

    @Test
    fun unitCardinalityIdentityOrderReasonCodesAndRoundsFailClosed() {
        assertFailure(createSubmissionWithInputs(inputs().dropLast(1)), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_ASSESSMENT_COUNT)
        assertFailure(createSubmissionWithInputs(inputs().dropLast(1) + inputs().first()), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.DUPLICATE_REVIEW_UNIT)
        assertFailure(createSubmissionWithInputs(inputs().mapIndexed { index, input -> if (index == 0) input.copy(reviewUnitId = "unknown") else input }), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.UNKNOWN_REVIEW_UNIT)
        assertFailure(createSubmissionWithInputs(inputs().mapIndexed { index, input -> if (index == 0) input.copy(canonicalEntityId = "foreign") else input }), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.UNIT_BINDING_MISMATCH)
        assertFailure(createSubmissionWithInputs(inputs().reversed()), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_ASSESSMENT_ORDER)
        assertFailure(createSubmissionWithInputs(inputs().mapIndexed { index, input -> if (index == 0) input.copy(reasonCodes = emptyList()) else input }), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_REASON_CODES)
        assertFailure(createSubmissionWithInputs(inputs().mapIndexed { index, input -> if (index == 0) input.copy(assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE) else input }), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_REASON_CODES)
        assertFailure(createSubmissionWithRounds(2, 1), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_VALIDATION_ROUND)
        assertFailure(createSubmissionWithRounds(1, 2), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_VALIDATION_REVISION)
    }

    @Test
    fun manipulatedSubmissionBindingAndLogicalDigestsFailClosed() {
        val original = completed(submission())
        val bindingBroken = original.copy(inputBindingDigest = "0".repeat(64))
        val logicalBroken = original.copy(logicalDigest = "0".repeat(64))
        assertValidationFailure(bindingBroken, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INPUT_BINDING_DIGEST_MISMATCH)
        assertValidationFailure(logicalBroken, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.LOGICAL_DIGEST_MISMATCH)
    }

    private fun submission() = createSubmission("reviewer:synthetic-independent:v1")

    private fun createSubmission(validator: String?) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.create(packet(), validator, 1, 1, inputs())

    private fun createSubmissionWithInputs(inputs: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1>) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.create(packet(), "reviewer:synthetic-independent:v1", 1, 1, inputs)

    private fun createSubmissionWithRounds(round: Int, revision: Int) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.create(packet(), "reviewer:synthetic-independent:v1", round, revision, inputs())

    private fun completed(result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1) =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1.Completed>(result).submission

    private fun assertFailure(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1,
        expected: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1,
    ) {
        assertEquals(expected, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1.Invalid>(result).reason)
    }

    private fun assertValid(submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1) =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionValidationResultV1.Valid>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.validate(submission),
        ).let { submission }

    private fun assertValidationFailure(
        submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1,
        expected: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1,
    ) {
        assertEquals(expected, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionValidationResultV1.Invalid>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.validate(submission),
        ).reason)
    }

    private fun inputs() = (1..4).map { index ->
        val decision = if (index % 2 == 1) HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION
        val reasons = if (decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION) {
            listOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
            )
        } else {
            listOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
            )
        }
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1(
            "unit-$index",
            "stable-$index",
            "canonical-$index",
            decision,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
            reasons,
            listOf("evidence-$index"),
            "Synthetic independent assessment $index",
        )
    }

    private fun packet(): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1 {
        val items = (1..4).map { index ->
            val decision = if (index % 2 == 1) HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION
            val support = decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION
            val source = listOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.AGRIBALYSE, HimGroundTruthSource.CIQUAL, HimGroundTruthSource.GLYCEMIC_INDEX)[index - 1]
            val kind = listOf(HimEvidenceRecordKind.OFF_PRODUCT, HimEvidenceRecordKind.AGRIBALYSE_RECORD, HimEvidenceRecordKind.CIQUAL_FOOD, HimEvidenceRecordKind.GI_MEASUREMENT)[index - 1]
            val evidence = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(
                "unit-$index",
                "evidence-$index",
                HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION,
                HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                if (support) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1("fixture/$index.json", 1, "a".repeat(64), "b".repeat(64)),
                "record:$index",
                listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1("field", "value", sha("value"))),
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1(source, kind, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1("fixture/$index.json", 1, "a".repeat(64), "b".repeat(64)), "record:$index", emptyList(), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1("fixture/$index.json", 1, "a".repeat(64), "b".repeat(64))),
            )
            val target = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1("canonical-$index", "Canonical $index", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, "catalog:canonical-$index", "authority:canonical-$index", emptyList(), emptyList(), "")
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1(
                "unit-$index", "stable-$index", "canonical-$index", "Canonical $index",
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1(source, kind, "fixture:$index", listOf(sha("finding-$index")), emptyList(), listOf("value"), HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH, emptyList()),
                listOf(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1("field", "value", sha("value"), "value", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL, 5)),
                "", decision, emptyList(), listOf("evidence-$index"), "Rationale $index", sha("Rationale $index"), null,
                target.copy(targetContextDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(target)),
                listOf(evidence), emptyList(), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_ASSESSMENTS, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_REASON_CODES, "", "",
            )
        }
        val binding = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketInputBindingV1(
            "fixture-batch", fileBinding("fixture/batch.json"), "input", "batch", "reviewer:logfather:v1", 1, 1,
            fileBinding("fixture/packet.json"), fileBinding("fixture/packet.md"), "packet-input", "packet-binding", "packet-logical",
            fileBinding("fixture/supplement.json"), fileBinding("fixture/supplement.md"), "supplement-binding", "supplement-logical",
            fileBinding("fixture/corpus.json"), "corpus-logical", "corpus-binding", "fixture-baseline",
        )
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.CONTRACT_ID,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VERSION,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.STATE,
            "fixture-packet", binding, items, emptyList(), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_ASSESSMENTS, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_REASON_CODES,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketCountersV1(4, 4, 2, 2, 0, 0, 4, 0, 0, 4, 4, 2, 2, 0, 0, 0, 0), "packet-binding", "packet-logical",
        )
    }

    private fun fileBinding(path: String) = HimZeroCandidateRecoveryHumanReviewFileBindingV1(path, 1, "c".repeat(64), "d".repeat(64))
    private fun sha(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
