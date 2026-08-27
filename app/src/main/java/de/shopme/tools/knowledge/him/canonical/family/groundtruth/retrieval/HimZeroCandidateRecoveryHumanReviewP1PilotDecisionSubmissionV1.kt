package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

/**
 * Pure, unpersisted submission of the four explicitly authorized P1 review decisions.
 * This contract deliberately has no runtime, persistence, source, or downstream-promotion behavior.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_SUBMISSION_V1"
    const val VERSION = "1"
    const val STATE = "HUMAN_REVIEWER_AUTHORIZED_UNPERSISTED"
    const val SUBMISSION_ID =
        "p1-artischocken-herzen-brie-double-creme-reviewer-logfather-r1-v1"
    const val MISSION_ID = "p1-artischocken-herzen-brie-double-creme-v1"
    const val REVIEWER_REF = "reviewer:logfather:v1"
    const val REVIEW_ROUND = 1
    const val REVISION = 1
    const val BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_SUBMISSION_BINDING_V1"
    const val LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_SUBMISSION_LOGICAL_V1"

    private const val PACKET_JSON_PATH =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-review-packet/v1/p1-artischocken-herzen-brie-double-creme-v1/review-packet.v1.json"
    private const val PACKET_MARKDOWN_PATH =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-review-packet/v1/p1-artischocken-herzen-brie-double-creme-v1/review-packet.v1.md"
    private const val SUPPLEMENT_JSON_PATH =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-direct-evidence-supplement/v1/p1-artischocken-herzen-brie-double-creme-v1/direct-evidence-supplement.v1.json"
    private const val SUPPLEMENT_MARKDOWN_PATH =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-direct-evidence-supplement/v1/p1-artischocken-herzen-brie-double-creme-v1/direct-evidence-supplement.v1.md"

    private const val PACKET_JSON_SHA256 =
        "4c06a60e7e9f8548088f7d006667d55f36d1d92fb93818d6d95b2f54dd6c8a43"
    private const val PACKET_MARKDOWN_SHA256 =
        "7560a7f84474676a80398e014bf73cb4527d7e956071994281ef1f05c84d4ad6"
    private const val SUPPLEMENT_JSON_SHA256 =
        "5a386e8adba953ac0bbdd8324b92de53cde867dfc577f87ba4a53b2b647062fb"
    private const val SUPPLEMENT_MARKDOWN_SHA256 =
        "6ead006b0deb97ec759e898984289d4578a9ae99a61f90b069445d9d25ff5f55"
    private const val PACKET_INPUT_BINDING_DIGEST =
        "4426f0b5be456ab92e32f7cd9b27378d89b0b3fdc7c2ed46b21415830afc475d"
    private const val PACKET_BINDING_DIGEST =
        "beb465178ecde447e3995672ea77fd16b5a020b6eabfc4e310ee0ae8f8771793"
    private const val PACKET_LOGICAL_DIGEST =
        "311f4925716045d47a02af3652dea66393f7cb713dbfe7239705ce4684acce6e"
    private const val SUPPLEMENT_BINDING_DIGEST =
        "6674ba8683e349a9d399ffa72c2fe3a2f52b17ea041901cb74ab15d2d99be291"
    private const val SUPPLEMENT_LOGICAL_DIGEST =
        "ebc15972f77d34265810e77a71c729f4ddf5aba79e2518f3a4759ca71302c579"
    private const val CORPUS_BINDING_DIGEST =
        "b1691dbf87eed143d23ea44275ec5d96bf9483e4675469af5d282d3858288e81"
    private const val SELECTION_DIGEST =
        "e5b639be1e04b004a8de2594bed4002c854727d15eb66420a42c4293f0f3dbcb"

    val FROZEN_BINDINGS = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionBindingsV1(
        mission = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionMissionBindingV1(
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.CONTRACT_ID,
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.VERSION,
            MISSION_ID,
            MISSION_ID,
            SELECTION_DIGEST,
        ),
        packet = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionPacketBindingV1(
            HimZeroCandidateRecoveryHumanReviewFileBindingV1(PACKET_JSON_PATH, 18626, PACKET_JSON_SHA256, PACKET_LOGICAL_DIGEST),
            HimZeroCandidateRecoveryHumanReviewFileBindingV1(PACKET_MARKDOWN_PATH, 14760, PACKET_MARKDOWN_SHA256, PACKET_LOGICAL_DIGEST),
            PACKET_INPUT_BINDING_DIGEST,
            PACKET_BINDING_DIGEST,
            PACKET_LOGICAL_DIGEST,
        ),
        supplement = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionSupplementBindingV1(
            HimZeroCandidateRecoveryHumanReviewFileBindingV1(SUPPLEMENT_JSON_PATH, 136096, SUPPLEMENT_JSON_SHA256, SUPPLEMENT_LOGICAL_DIGEST),
            HimZeroCandidateRecoveryHumanReviewFileBindingV1(SUPPLEMENT_MARKDOWN_PATH, 160984, SUPPLEMENT_MARKDOWN_SHA256, SUPPLEMENT_LOGICAL_DIGEST),
            SUPPLEMENT_BINDING_DIGEST,
            SUPPLEMENT_LOGICAL_DIGEST,
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CONTRACT_ID,
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.VERSION,
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.STATE,
        ),
        corpusBindingDigest = CORPUS_BINDING_DIGEST,
    )

    val FROZEN_SELECTIONS = listOf(
        selection(
            "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e",
            "a98f67c7aa8af729e27d402585df4c78d2a1a9c3f85e636dd7d257cba1810ebd",
            "ZuhV5V",
            HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
            listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED),
            listOf(
                "49c134d1fd2b99ae2c164cffdc5e553a6364f2cabfea7f057bb58e613aaef9f4",
                "5912c6bde368803377441d627a04c74d1aecf08b43131b19d793196b32bf94f9",
                "9ecfd9e8e601ffdb1f60620ff204882a32bbbfc355cd6b9b79f403866a40d9bd",
            ),
        ),
        selection(
            "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7",
            "ec4d7ccf39c1b9dbe184a0af90190cbc5d6a9e96ce3e3af121f5f59258eb5e13",
            "rVnyq7",
            HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            listOf(
                HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_SEMANTIC_MISMATCH,
                HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED,
            ),
            listOf(
                "280b91e4c4e10f95288308d50a0df31e3be773385c11afc104e8525127cc5b1e",
                "8a9e75f184ab7e1e4dd81c0b6d805ddf252dce54d8d39244b892e53ad59e7bcd",
                "b4a88e7492228b36d6919cbc75959520b86d8505cf1f85b93764f21538cdfe09",
            ),
        ),
        selection(
            "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70",
            "6db7a77b0a3ff2471b8001b1644bc7a957efa52369df722899298c87d286ed71",
            "ZuhV5V",
            HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
            listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED),
            listOf(
                "4712bc9b78ecfc168ea1902518ff625f92eff4ee14a9d1938a5c66fc4dd8b39e",
                "896f30747f7fcfcb29f64e9170e9ca6798ad796a996e79d7b5432b146fde2020",
                "d6f7e596663b3547c30bb4afcf386cdc97bc32d170860ed39486f23ec719dabf",
            ),
        ),
        selection(
            "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50",
            "f4957e490b1714a1e48ca5d43ae762564603ac712842b3c2714b9f36bf2c41df",
            "rVnyq7",
            HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            listOf(
                HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_SEMANTIC_MISMATCH,
                HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED,
            ),
            listOf(
                "045ee67398a6f7355b0faef3760ceb0d24bf22515b9e67d84c7931e96d2c9fba",
                "2b225f611ce2dc5eed79f4238b0a24d328318638549fb63f3a9611f1fc29e027",
                "dbcacd77e3944b77661262caa8845e24b1a1cb2eb6b78726cb7595dc7ecf5d93",
            ),
        ),
    )

    fun create(
        selections: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1> = FROZEN_SELECTIONS,
        bindings: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionBindingsV1 = FROZEN_BINDINGS,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1 {
        val unsigned = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1(
            CONTRACT_ID,
            VERSION,
            STATE,
            SUBMISSION_ID,
            bindings.mission,
            bindings.packet,
            bindings.supplement,
            bindings.corpusBindingDigest,
            REVIEWER_REF,
            REVIEW_ROUND,
            selections,
            deriveCounters(selections),
            "",
            "",
        )
        val bound = unsigned.copy(submissionBindingDigest = submissionBindingDigest(unsigned))
        return bound.copy(submissionLogicalDigest = submissionLogicalDigest(bound))
    }

    fun deriveCounters(
        selections: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1>,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionCountersV1(
        authorizedDecisionSelections = selections.size,
        confirmAssociations = selections.count { it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION },
        rejectAssociations = selections.count { it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION },
        abstentions = selections.count { it.decision in setOf(HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_INSUFFICIENT_EVIDENCE, HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS, HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_CONFLICTING_EVIDENCE) },
        escalations = selections.count { it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE },
        selectedEvidenceReferences = selections.sumOf { it.evidenceReferenceIds.size },
        uniqueEvidenceReferences = selections.flatMap { it.evidenceReferenceIds }.distinct().size,
        alternativeCanonicalProposals = selections.count { it.alternativeCanonicalProposal != null },
        reviewers = if (selections.isEmpty()) 0 else 1,
        reviewRound = selections.map { REVIEW_ROUND }.minOrNull() ?: 0,
        minimumRevision = selections.map { it.revision }.minOrNull() ?: 0,
        maximumRevision = selections.map { it.revision }.maxOrNull() ?: 0,
    )

    fun validate(
        submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1 {
        if (submission.contractId != CONTRACT_ID) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_CONTRACT_ID)
        if (submission.version != VERSION) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_CONTRACT_VERSION)
        if (submission.state != STATE) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_SUBMISSION_STATE)
        if (submission.submissionId != SUBMISSION_ID) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_SUBMISSION_ID)
        if (submission.missionBinding != FROZEN_BINDINGS.mission) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_MISSION_BINDING)
        if (submission.packetBinding != FROZEN_BINDINGS.packet) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_PACKET_BINDING)
        if (submission.supplementBinding != FROZEN_BINDINGS.supplement) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_SUPPLEMENT_BINDING)
        if (submission.corpusBindingDigest != FROZEN_BINDINGS.corpusBindingDigest) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_CORPUS_BINDING)
        if (submission.reviewerRef != REVIEWER_REF) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_REVIEWER_REF)
        if (submission.reviewRound != REVIEW_ROUND) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_REVIEW_ROUND)
        if (submission.selections.any { it.revision != REVISION }) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_REVISION)
        if (submission.selections.size != FROZEN_SELECTIONS.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_DECISION_COUNT)
        if (submission.selections.map { it.reviewUnitId }.distinct().size != submission.selections.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.DUPLICATE_REVIEW_UNIT)
        if (submission.selections.any { it.reviewUnitId !in FROZEN_SELECTIONS.map { expected -> expected.reviewUnitId } }) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.UNEXPECTED_REVIEW_UNIT)
        submission.selections.forEach { selection ->
            val expected = FROZEN_SELECTIONS.singleOrNull { it.reviewUnitId == selection.reviewUnitId }
                ?: return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.UNEXPECTED_REVIEW_UNIT)
            if (selection.stableEntryId != expected.stableEntryId || selection.canonicalEntityId != expected.canonicalEntityId) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_REVIEW_UNIT)
            if (selection.decision != expected.decision) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.DECISION_MISMATCH)
            if (selection.reasonCodes != expected.reasonCodes) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.REASON_CODES_MISMATCH)
            if (selection.evidenceReferenceIds.distinct().size != selection.evidenceReferenceIds.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.DUPLICATE_EVIDENCE_REFERENCE)
            if (selection.evidenceReferenceIds.toSet() != expected.evidenceReferenceIds.toSet()) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.EVIDENCE_REFERENCE_IDS_MISMATCH)
            if (selection.alternativeCanonicalProposal != null) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.ALTERNATIVE_CANONICAL_PROPOSAL_FORBIDDEN)
            if (selection.reviewerNote != null) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.REVIEWER_NOTE_FORBIDDEN)
        }
        if (submission.selections.map { it.reviewUnitId } != FROZEN_SELECTIONS.map { it.reviewUnitId }) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_REVIEW_UNIT)
        if (submission.counters != deriveCounters(submission.selections)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_COUNTERS)
        if (submission.submissionBindingDigest != submissionBindingDigest(submission)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.SUBMISSION_BINDING_DIGEST_MISMATCH)
        if (submission.submissionLogicalDigest != submissionLogicalDigest(submission)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.SUBMISSION_LOGICAL_DIGEST_MISMATCH)
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1.Valid
    }

    fun materialize(
        submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1,
        supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1 {
        val submissionResult = validate(submission)
        if (submissionResult is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1.Invalid) return failed(submissionResult.reason)
        when (val supplementResult = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.validate(supplement)) {
            is HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Invalid ->
                return failed(supplementFailure(supplementResult.reason))
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Valid -> Unit
        }

        val cards = supplement.bundles.flatMap { bundle -> bundle.evidence.map { evidence -> evidence.evidenceReferenceId to evidence } }
        if (cards.map { it.first }.distinct().size != cards.size) return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.DUPLICATE_EVIDENCE_REFERENCE)
        val cardById = cards.toMap()
        val records = submission.selections.map { selection ->
            val bundle = supplement.bundles.singleOrNull { it.reviewUnit.reviewUnitId == selection.reviewUnitId }
                ?: return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_REVIEW_UNIT)
            val references = selection.evidenceReferenceIds.map { evidenceId ->
                val evidence = cardById[evidenceId]
                    ?: return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.MISSING_EVIDENCE_REFERENCE)
                if (evidence.reviewUnitId != selection.reviewUnitId) return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.CROSS_UNIT_EVIDENCE_REFERENCE)
                expectedEvidence(evidenceId)?.let { expected ->
                    if (evidence.kind != expected.kind) return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.EVIDENCE_KIND_MISMATCH)
                    if (evidence.directness != expected.directness) return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.EVIDENCE_DIRECTNESS_MISMATCH)
                    if (evidence.position != expected.position) return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.EVIDENCE_POSITION_MISMATCH)
                } ?: return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.UNEXPECTED_EVIDENCE_REFERENCE)
                HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1(
                    evidence.evidenceReferenceId,
                    evidence.kind,
                    evidence.directness,
                    evidence.position,
                    evidence.artifact.relativePath,
                    evidence.recordReference,
                    evidence.artifact.sha256,
                    evidence.artifact.logicalDigest,
                    evidence.fields.map { it.fieldReference },
                )
            }.sortedBy { it.evidenceReferenceId }
            val record = HimZeroCandidateRecoveryHumanReviewDecisionRecordV1(
                HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
                bundle.reviewUnit,
                submission.reviewerRef,
                submission.reviewRound,
                selection.revision,
                selection.decision,
                selection.reasonCodes,
                references,
                null,
                null,
            )
            if (!record.validate().valid) return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.DECISION_RECORD_VALIDATION_FAILED)
            record
        }
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Completed(records)
    }

    fun submissionBindingDigest(submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1): String =
        HimZeroCandidateRecoveryHumanReviewContractV1.sha256(buildString {
            appendLine(BINDING_DIGEST_DOMAIN)
            appendLine("contractId=${submission.contractId}")
            appendLine("version=${submission.version}")
            appendLine("state=${submission.state}")
            appendLine("submissionId=${submission.submissionId}")
            appendLine("mission=${submission.missionBinding}")
            appendFileBinding("packetJson", submission.packetBinding.json)
            appendFileBinding("packetMarkdown", submission.packetBinding.markdown)
            appendLine("packetInputBindingDigest=${submission.packetBinding.inputBindingDigest}")
            appendLine("packetBindingDigest=${submission.packetBinding.bindingDigest}")
            appendLine("packetLogicalDigest=${submission.packetBinding.logicalDigest}")
            appendFileBinding("supplementJson", submission.supplementBinding.json)
            appendFileBinding("supplementMarkdown", submission.supplementBinding.markdown)
            appendLine("supplementBindingDigest=${submission.supplementBinding.bindingDigest}")
            appendLine("supplementLogicalDigest=${submission.supplementBinding.logicalDigest}")
            appendLine("supplementContractId=${submission.supplementBinding.contractId}")
            appendLine("supplementVersion=${submission.supplementBinding.version}")
            appendLine("supplementState=${submission.supplementBinding.state}")
            appendLine("corpusBindingDigest=${submission.corpusBindingDigest}")
            appendLine("reviewerRef=${submission.reviewerRef}")
            appendLine("reviewRound=${submission.reviewRound}")
            submission.selections.forEachIndexed { index, selection ->
                appendSelection(index, selection)
            }
        })

    fun submissionLogicalDigest(submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1): String =
        HimZeroCandidateRecoveryHumanReviewContractV1.sha256(buildString {
            appendLine(LOGICAL_DIGEST_DOMAIN)
            appendLine("contractId=${submission.contractId}")
            appendLine("version=${submission.version}")
            appendLine("state=${submission.state}")
            appendLine("submissionBindingDigest=${submission.submissionBindingDigest}")
            appendLine("mission=${submission.missionBinding}")
            submission.selections.forEachIndexed { index, selection -> appendSelection(index, selection) }
            appendLine("counters=${submission.counters}")
        })

    private fun StringBuilder.appendFileBinding(label: String, binding: HimZeroCandidateRecoveryHumanReviewFileBindingV1) {
        appendLine("$label.relativePath=${binding.relativePath}")
        appendLine("$label.byteSize=${binding.byteSize}")
        appendLine("$label.sha256=${binding.sha256}")
        appendLine("$label.logicalDigest=${binding.logicalDigest}")
    }

    private fun StringBuilder.appendSelection(index: Int, selection: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1) {
        appendLine("selection[$index].reviewUnitId=${selection.reviewUnitId}")
        appendLine("selection[$index].stableEntryId=${selection.stableEntryId}")
        appendLine("selection[$index].canonicalEntityId=${selection.canonicalEntityId}")
        appendLine("selection[$index].decision=${selection.decision.name}")
        selection.reasonCodes.forEach { appendLine("selection[$index].reasonCode=${it.name}") }
        selection.evidenceReferenceIds.forEach { appendLine("selection[$index].evidenceReferenceId=$it") }
        appendLine("selection[$index].revision=${selection.revision}")
        appendLine("selection[$index].alternativeCanonicalProposal=${selection.alternativeCanonicalProposal ?: ""}")
        appendLine("selection[$index].reviewerNote=${selection.reviewerNote ?: ""}")
    }

    private fun expectedEvidence(id: String): EvidenceExpectation? = when (id) {
        "49c134d1fd2b99ae2c164cffdc5e553a6364f2cabfea7f057bb58e613aaef9f4",
        "b4a88e7492228b36d6919cbc75959520b86d8505cf1f85b93764f21538cdfe09",
        "d6f7e596663b3547c30bb4afcf386cdc97bc32d170860ed39486f23ec719dabf",
        "045ee67398a6f7355b0faef3760ceb0d24bf22515b9e67d84c7931e96d2c9fba",
        -> EvidenceExpectation(HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY)
        "5912c6bde368803377441d627a04c74d1aecf08b43131b19d793196b32bf94f9",
        "4712bc9b78ecfc168ea1902518ff625f92eff4ee14a9d1938a5c66fc4dd8b39e",
        -> EvidenceExpectation(HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION)
        "9ecfd9e8e601ffdb1f60620ff204882a32bbbfc355cd6b9b79f403866a40d9bd",
        "896f30747f7fcfcb29f64e9170e9ca6798ad796a996e79d7b5432b146fde2020",
        -> EvidenceExpectation(HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION)
        "280b91e4c4e10f95288308d50a0df31e3be773385c11afc104e8525127cc5b1e",
        "dbcacd77e3944b77661262caa8845e24b1a1cb2eb6b78726cb7595dc7ecf5d93",
        -> EvidenceExpectation(HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY)
        "8a9e75f184ab7e1e4dd81c0b6d805ddf252dce54d8d39244b892e53ad59e7bcd",
        "2b225f611ce2dc5eed79f4238b0a24d328318638549fb63f3a9611f1fc29e027",
        -> EvidenceExpectation(HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION)
        else -> null
    }

    private fun supplementFailure(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1,
    ) = when (reason) {
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.MISSING_SOURCE_EVIDENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.MISSING_CATALOG_EVIDENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_REFERENCE_ID,
        -> HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.MISSING_EVIDENCE_REFERENCE
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.DUPLICATE_EVIDENCE_REFERENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.DUPLICATE_SOURCE_EVIDENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.DUPLICATE_CATALOG_EVIDENCE,
        -> HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.DUPLICATE_EVIDENCE_REFERENCE
        else -> HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_SUPPLEMENT_BINDING
    }

    private fun selection(
        reviewUnitId: String,
        stableEntryId: String,
        canonicalEntityId: String,
        decision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
        reasonCodes: List<HimZeroCandidateRecoveryHumanReviewReasonCodeV1>,
        evidenceReferenceIds: List<String>,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1(
        reviewUnitId,
        stableEntryId,
        canonicalEntityId,
        decision,
        reasonCodes,
        evidenceReferenceIds,
        REVISION,
        null,
        null,
    )

    private fun invalid(reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1.Invalid(reason)

    private fun failed(reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Failed(reason)

    private data class EvidenceExpectation(
        val kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
        val directness: HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1,
        val position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
    )
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionMissionBindingV1(
    val contractId: String,
    val version: String,
    val missionId: String,
    val scopeId: String,
    val selectionDigest: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionPacketBindingV1(
    val json: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val markdown: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val inputBindingDigest: String,
    val bindingDigest: String,
    val logicalDigest: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionSupplementBindingV1(
    val json: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val markdown: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val bindingDigest: String,
    val logicalDigest: String,
    val contractId: String,
    val version: String,
    val state: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionBindingsV1(
    val mission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionMissionBindingV1,
    val packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionPacketBindingV1,
    val supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionSupplementBindingV1,
    val corpusBindingDigest: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1(
    val reviewUnitId: String,
    val stableEntryId: String,
    val canonicalEntityId: String,
    val decision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
    val reasonCodes: List<HimZeroCandidateRecoveryHumanReviewReasonCodeV1>,
    val evidenceReferenceIds: List<String>,
    val revision: Int,
    val alternativeCanonicalProposal: String?,
    val reviewerNote: String?,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionCountersV1(
    val authorizedDecisionSelections: Int,
    val confirmAssociations: Int,
    val rejectAssociations: Int,
    val abstentions: Int,
    val escalations: Int,
    val selectedEvidenceReferences: Int,
    val uniqueEvidenceReferences: Int,
    val alternativeCanonicalProposals: Int,
    val reviewers: Int,
    val reviewRound: Int,
    val minimumRevision: Int,
    val maximumRevision: Int,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1(
    val contractId: String,
    val version: String,
    val state: String,
    val submissionId: String,
    val missionBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionMissionBindingV1,
    val packetBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionPacketBindingV1,
    val supplementBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionSupplementBindingV1,
    val corpusBindingDigest: String,
    val reviewerRef: String,
    val reviewRound: Int,
    val selections: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1>,
    val counters: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionCountersV1,
    val submissionBindingDigest: String,
    val submissionLogicalDigest: String,
)

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1 {
    INVALID_CONTRACT_ID,
    INVALID_CONTRACT_VERSION,
    INVALID_SUBMISSION_STATE,
    INVALID_SUBMISSION_ID,
    INVALID_MISSION_BINDING,
    INVALID_PACKET_BINDING,
    INVALID_SUPPLEMENT_BINDING,
    INVALID_CORPUS_BINDING,
    INVALID_REVIEWER_REF,
    INVALID_REVIEW_ROUND,
    INVALID_REVISION,
    INVALID_DECISION_COUNT,
    DUPLICATE_REVIEW_UNIT,
    UNEXPECTED_REVIEW_UNIT,
    INVALID_REVIEW_UNIT,
    DECISION_MISMATCH,
    REASON_CODES_MISMATCH,
    EVIDENCE_REFERENCE_IDS_MISMATCH,
    MISSING_EVIDENCE_REFERENCE,
    UNEXPECTED_EVIDENCE_REFERENCE,
    DUPLICATE_EVIDENCE_REFERENCE,
    CROSS_UNIT_EVIDENCE_REFERENCE,
    EVIDENCE_KIND_MISMATCH,
    EVIDENCE_DIRECTNESS_MISMATCH,
    EVIDENCE_POSITION_MISMATCH,
    DECISION_RECORD_VALIDATION_FAILED,
    ALTERNATIVE_CANONICAL_PROPOSAL_FORBIDDEN,
    REVIEWER_NOTE_FORBIDDEN,
    INVALID_COUNTERS,
    SUBMISSION_BINDING_DIGEST_MISMATCH,
    SUBMISSION_LOGICAL_DIGEST_MISMATCH,
    FORBIDDEN_PERSISTENCE_STATE,
    FORBIDDEN_DOWNSTREAM_AUTHORIZATION,
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1 {
    val valid: Boolean

    data object Valid : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1 {
        override val valid: Boolean = true
    }

    data class Invalid(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1 {
        override val valid: Boolean = false
    }
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1 {
    data class Completed(
        val records: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1
}
