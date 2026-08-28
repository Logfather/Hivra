package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPrimaryBucketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewFileBindingV1
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationV1Test {
    @Test
    fun contractIdentityAndOutputBoundaryAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_RECORD_MATERIALIZATION_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationV1.VERSION)
        assertEquals("RECORD_MATERIALIZATION_ONLY", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationV1.STATE)
        val declaredMethods = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationV1::class.java.declaredMethods.map { it.name }.toSet()
        assertTrue("materialize" in declaredMethods)
        assertFalse("createBatch" in declaredMethods)
    }

    @Test
    fun validSubmissionProducesExactlyFourExistingRecordsInSubmissionOrder() {
        val records = completed(materialize())
        val selections = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING.originalSelections
        assertEquals(4, records.size)
        assertEquals(selections.map { it.reviewUnitId }, records.map { it.originalDecision.reviewUnitId })
        assertEquals("reviewer:synthetic-independent:v1", records.map { it.validatorReviewerRef }.distinct().single())
        assertEquals(listOf(1, 1, 1, 1), records.map { it.validationRound })
        assertEquals(listOf(1, 1, 1, 1), records.map { it.validationRevision })
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
            ),
            records.map { it.assessment },
        )
        assertTrue(records.all { it.validationRecordId.matches(Regex("[0-9a-f]{64}")) })
        assertTrue(records.all { it.downstreamRoute.name.isNotBlank() })
    }

    @Test
    fun everyAssessmentReasonEvidenceAndRationaleIsCopiedWithoutReevaluation() {
        val submission = completedSubmission()
        val records = completed(materialize(submission))
        assertEquals(submission.assessments.map { it.assessment }, records.map { it.assessment })
        assertEquals(submission.assessments.map { it.reasonCodes }, records.map { it.reasonCodes })
        assertEquals(submission.assessments.map { it.evidenceReferenceIds }, records.map { it.evidenceReferenceIds })
        assertEquals(submission.assessments.map { it.rationale }, records.map { it.rationale })
        assertEquals(
            records.map {
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validationRecordId(
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING,
                    it.originalDecision,
                    it.validatorReviewerRef,
                    it.validationRound,
                    it.validationRevision,
                )
            },
            records.map { it.validationRecordId },
        )
    }

    @Test
    fun repeatedIdenticalSubmissionProducesIdenticalRecordsAndNoBatchOrPersistence() {
        val first = completed(materialize())
        val second = completed(materialize())
        assertEquals(first, second)
        assertFalse(first.toString().contains("Batch"))
        assertFalse(first.toString().contains("Persistence"))
    }

    @Test
    fun invalidSubmissionDigestMissingExtraReorderedAndForeignBindingFailClosed() {
        val original = completedSubmission()
        assertFailure(
            materialize(original.copy(inputBindingDigest = "0".repeat(64))),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INPUT_BINDING_DIGEST_MISMATCH,
        )
        assertFailure(
            materialize(original.copy(assessments = original.assessments.dropLast(1))),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_ASSESSMENT_COUNT,
        )
        assertFailure(
            materialize(original.copy(assessments = original.assessments + original.assessments.first())),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_ASSESSMENT_COUNT,
        )
        assertFailure(
            materialize(original.copy(assessments = original.assessments.reversed())),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_ASSESSMENT_ORDER,
        )
        val foreignBinding = original.packetBinding.copy(
            unitBindings = original.packetBinding.unitBindings.toMutableList().also {
                it[0] = it[0].copy(canonicalEntityId = "foreign")
            },
        )
        assertFailure(
            materialize(original.copy(packetBinding = foreignBinding)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.UNIT_BINDING_MISMATCH,
        )
    }

    @Test
    fun foreignPacketBindingCannotBeMaterializedForP1() {
        val original = completedSubmission()
        val foreignPacketBinding = original.packetBinding.copy(packetId = "foreign-packet")
        val unsigned = original.copy(
            submissionId = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.submissionId(
                foreignPacketBinding.packetId,
                original.validatorReviewerRef,
                original.validationRound,
                original.validationRevision,
            ),
            packetBinding = foreignPacketBinding,
            inputBindingDigest = "",
            logicalDigest = "",
        )
        val withBinding = unsigned.copy(
            inputBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.inputBindingDigest(unsigned),
        )
        val foreignSubmission = withBinding.copy(
            logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.logicalDigest(withBinding),
        )
        assertFailure(
            materialize(foreignSubmission),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_PACKET_BINDING,
        )
    }

    @Test
    fun upstreamReviewPacketBindingDigestCannotBeMaterializedForP1() {
        val original = completedSubmission()
        val upstreamBinding = original.packetBinding.copy(
            packetBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.REVIEW_PACKET_BINDING_DIGEST,
        )
        val unsigned = original.copy(packetBinding = upstreamBinding, inputBindingDigest = "", logicalDigest = "")
        val withBinding = unsigned.copy(
            inputBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.inputBindingDigest(unsigned),
        )
        val foreignSubmission = withBinding.copy(
            logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.logicalDigest(withBinding),
        )
        assertFailure(
            materialize(foreignSubmission),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_PACKET_BINDING,
        )
    }

    @Test
    fun upstreamReviewPacketLogicalDigestCannotBeMaterializedForP1() {
        val original = completedSubmission()
        val upstreamBinding = original.packetBinding.copy(
            packetLogicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.REVIEW_PACKET_LOGICAL_DIGEST,
        )
        val unsigned = original.copy(packetBinding = upstreamBinding, inputBindingDigest = "", logicalDigest = "")
        val withBinding = unsigned.copy(
            inputBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.inputBindingDigest(unsigned),
        )
        val foreignSubmission = withBinding.copy(
            logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.logicalDigest(withBinding),
        )
        assertFailure(
            materialize(foreignSubmission),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_PACKET_BINDING,
        )
    }

    private fun materialize(
        submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1 = completedSubmission(),
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationV1.materialize(submission)

    private fun completedSubmission() = completed(
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.create(
            packet(),
            "reviewer:synthetic-independent:v1",
            1,
            1,
            assessments(),
        ),
    )

    private fun completed(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1 =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1.Completed>(result).submission

    private fun completed(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1,
    ) = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1.Completed>(result).records

    private fun assertFailure(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1,
        expected: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1,
    ) {
        assertEquals(
            expected,
            assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1.Failed>(result).reason,
        )
    }

    private fun assessments() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING.originalSelections.map { selection ->
        val confirm = selection.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1(
            reviewUnitId = selection.reviewUnitId,
            stableEntryId = selection.stableEntryId,
            canonicalEntityId = selection.canonicalEntityId,
            originalDecision = selection.decision,
            assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
            reasonCodes = if (confirm) {
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
            evidenceReferenceIds = listOf(selection.evidenceReferenceIds.first()),
            rationale = "Synthetic independent assessment for ${selection.reviewUnitId}",
        )
    }

    private fun packet() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1(
        contractId = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.CONTRACT_ID,
        version = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VERSION,
        state = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.STATE,
        packetId = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.PACKET_ID,
        inputBinding = packetInputBinding(),
        items = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING.originalSelections.mapIndexed { index, selection -> item(index, selection) },
        contextLimitations = emptyList(),
        allowedValidationAssessments = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_ASSESSMENTS,
        allowedValidationReasonCodes = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_REASON_CODES,
        counters = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketCountersV1(4, 4, 2, 2, 0, 0, 4, 0, 0, 4, 4, 2, 2, 0, 0, 0, 0),
        packetBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.DECISION_VALIDATION_PACKET_BINDING_DIGEST,
        packetLogicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.DECISION_VALIDATION_PACKET_LOGICAL_DIGEST,
    )

    private fun packetInputBinding() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketInputBindingV1(
        decisionBatchId = "fixture-batch",
        decisionBatch = fileBinding("fixture/batch.json"),
        originalInputBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.ORIGINAL_INPUT_BINDING_DIGEST,
        originalBatchLogicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.ORIGINAL_BATCH_LOGICAL_DIGEST,
        originalReviewerRef = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.ORIGINAL_REVIEWER_REF,
        originalReviewRound = 1,
        originalRevision = 1,
        reviewPacketJson = fileBinding("fixture/packet.json"),
        reviewPacketMarkdown = fileBinding("fixture/packet.md"),
        reviewPacketInputBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.REVIEW_PACKET_INPUT_BINDING_DIGEST,
        reviewPacketBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.REVIEW_PACKET_BINDING_DIGEST,
        reviewPacketLogicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.REVIEW_PACKET_LOGICAL_DIGEST,
        supplementJson = fileBinding("fixture/supplement.json"),
        supplementMarkdown = fileBinding("fixture/supplement.md"),
        supplementBindingDigest = "e".repeat(64),
        supplementLogicalDigest = "f".repeat(64),
        corpus = fileBinding("fixture/corpus.json"),
        corpusLogicalDigest = "1".repeat(64),
        corpusBindingDigest = "2".repeat(64),
        contractBaselineHead = "3".repeat(40),
    )

    private fun item(
        index: Int,
        selection: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1 {
        val source = listOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.AGRIBALYSE, HimGroundTruthSource.CIQUAL, HimGroundTruthSource.GLYCEMIC_INDEX)[index]
        val kind = listOf(HimEvidenceRecordKind.OFF_PRODUCT, HimEvidenceRecordKind.AGRIBALYSE_RECORD, HimEvidenceRecordKind.CIQUAL_FOOD, HimEvidenceRecordKind.GI_MEASUREMENT)[index]
        val support = selection.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION
        val artifact = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1("fixture/$index.json", 1, "a".repeat(64), "b".repeat(64))
        val evidenceId = selection.evidenceReferenceIds.first()
        val target = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1(
            selection.canonicalEntityId,
            "Canonical $index",
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED,
            "catalog:${selection.canonicalEntityId}",
            "authority:${selection.canonicalEntityId}",
            emptyList(),
            emptyList(),
            "",
        )
        val card = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(
            selection.reviewUnitId,
            evidenceId,
            HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION,
            HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
            if (support) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION,
            artifact,
            "record:$index",
            listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1("field", "value", sha("value"))),
            null,
        )
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1(
            selection.reviewUnitId,
            selection.stableEntryId,
            selection.canonicalEntityId,
            "Canonical $index",
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1(source, kind, "fixture:$index", listOf(sha("finding-$index")), emptyList(), listOf("value"), HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH, emptyList()),
            listOf(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1("field", "value", sha("value"), "value", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL, 5)),
            "identity-$index",
            selection.decision,
            selection.reasonCodes,
            selection.evidenceReferenceIds,
            "Original rationale $index",
            sha("Original rationale $index"),
            null,
            target.copy(targetContextDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(target)),
            listOf(card),
            emptyList(),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_ASSESSMENTS,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_REASON_CODES,
            "c".repeat(64),
            "d".repeat(64),
        )
    }

    private fun fileBinding(path: String) = HimZeroCandidateRecoveryHumanReviewFileBindingV1(path, 1, "4".repeat(64), "5".repeat(64))

    private fun sha(value: String) = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
