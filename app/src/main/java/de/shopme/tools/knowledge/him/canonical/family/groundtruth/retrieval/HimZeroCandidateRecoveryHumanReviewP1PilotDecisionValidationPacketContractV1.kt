package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure, evidence-first context contract for an independent validation packet.
 * It deliberately contains no persistence, reviewer assignment, validation result,
 * downstream route, promotion, or mutation semantics.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PACKET_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "INDEPENDENT_VALIDATION_CONTEXT_ONLY"
    const val PACKET_ID = "p1-artischocken-brie-decision-validation-v1"
    const val CONTRACT_BASELINE_HEAD = "d2719de5cfa96e7616e7cd3bd535c9b2f7079a45"

    const val ITEM_BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PACKET_ITEM_V1"
    const val PACKET_BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PACKET_BINDING_V1"
    const val PACKET_LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PACKET_LOGICAL_V1"

    const val DECISION_BATCH_ID = "p1-artischocken-brie-logfather-r1-v1"
    const val DECISION_BATCH_PATH =
        "data/knowledge/him/canonical-family/human-review/zero-candidate-recovery/v1/decision-batches/p1-artischocken-brie-logfather-r1-v1/review-decisions.v1.json"
    const val DECISION_BATCH_SIZE = 29094L
    const val DECISION_BATCH_SHA256 =
        "2a892ea6591967fdcbb439f63a3c1d3ef04d041ac10af4863c7c22d4ff140342"
    const val DECISION_BATCH_INPUT_BINDING_DIGEST =
        "12021b21c18e9394cbb9d27a645ddeaf922675cd1dd3cfb1f5e3a63a83bedc23"
    const val DECISION_BATCH_LOGICAL_DIGEST =
        "53052a8bcceafdade7c7c597750e3497d8ca377369915502257c1427f8106c02"
    const val ORIGINAL_REVIEWER_REF = "reviewer:logfather:v1"
    const val REVIEW_ROUND = 1
    const val REVISION = 1

    const val REVIEW_PACKET_JSON_SHA256 =
        "4c06a60e7e9f8548088f7d006667d55f36d1d92fb93818d6d95b2f54dd6c8a43"
    const val REVIEW_PACKET_MARKDOWN_SHA256 =
        "7560a7f84474676a80398e014bf73cb4527d7e956071994281ef1f05c84d4ad6"
    const val REVIEW_PACKET_BINDING_DIGEST =
        "beb465178ecde447e3995672ea77fd16b5a020b6eabfc4e310ee0ae8f8771793"
    const val REVIEW_PACKET_LOGICAL_DIGEST =
        "311f4925716045d47a02af3652dea66393f7cb713dbfe7239705ce4684acce6e"
    const val DECISION_VALIDATION_PACKET_BINDING_DIGEST =
        "9e127ca7afab1721b60039f611ed475f8c49f08035c88073e7a2ce0abfe010fa"
    const val DECISION_VALIDATION_PACKET_LOGICAL_DIGEST =
        "83272934442c39495d4a33cfef0a0403fece79d6db4b63e6d5454d7c6867dccd"
    const val REVIEW_PACKET_INPUT_BINDING_DIGEST =
        "4426f0b5be456ab92e32f7cd9b27378d89b0b3fdc7c2ed46b21415830afc475d"
    const val REVIEW_PACKET_JSON_SIZE = 18626L
    const val REVIEW_PACKET_MARKDOWN_SIZE = 14760L

    const val SUPPLEMENT_JSON_SHA256 =
        "5a386e8adba953ac0bbdd8324b92de53cde867dfc577f87ba4a53b2b647062fb"
    const val SUPPLEMENT_MARKDOWN_SHA256 =
        "6ead006b0deb97ec759e898984289d4578a9ae99a61f90b069445d9d25ff5f55"
    const val SUPPLEMENT_BINDING_DIGEST =
        "6674ba8683e349a9d399ffa72c2fe3a2f52b17ea041901cb74ab15d2d99be291"
    const val SUPPLEMENT_LOGICAL_DIGEST =
        "ebc15972f77d34265810e77a71c729f4ddf5aba79e2518f3a4759ca71302c579"
    const val SUPPLEMENT_JSON_SIZE = 136096L
    const val SUPPLEMENT_MARKDOWN_SIZE = 160984L

    const val CORPUS_SHA256 =
        "4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016"
    const val CORPUS_LOGICAL_DIGEST =
        "3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02"
    const val CORPUS_BINDING_DIGEST =
        "b1691dbf87eed143d23ea44275ec5d96bf9483e4675469af5d282d3858288e81"
    const val CORPUS_SIZE = 2359985L

    const val JSON_OUTPUT_PATH =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-decision-validation-packet/v1/p1-artischocken-brie-decision-validation-v1/validation-packet.v1.json"
    const val MARKDOWN_OUTPUT_PATH =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-decision-validation-packet/v1/p1-artischocken-brie-decision-validation-v1/validation-packet.v1.md"

    val VALIDATION_ASSESSMENTS = listOf(
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ESCALATE_VALIDATION_CONFLICT,
    )
    val VALIDATION_REASON_CODES = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.values().toList()
    val REQUIRED_CONTEXT_LIMITATIONS = listOf(
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1.BOUND_ARTIFACTS_ONLY,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1.DIRECT_EVIDENCE_NOT_AUTOMATIC_DECISION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1.ORIGINAL_DECISION_NOT_INDEPENDENTLY_VALIDATED,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1.VALIDATOR_MUST_BE_INDEPENDENT,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1.CHALLENGE_DOES_NOT_CHANGE_ORIGINAL,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1.GOLD_EXCLUDED,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1.NEGATIVE_SUPERVISION_EXCLUDED,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1.TRAINING_EXCLUDED,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1.AUTHORITY_MUTATION_EXCLUDED,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1.CONFLICT_REQUIRES_ADJUDICATION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1.NO_EXTERNAL_SOURCE_RESEARCH,
    ).sortedBy { it.ordinal }

    val FROZEN_REVIEW_UNITS = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION.entries

    fun frozenInputBinding() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketInputBindingV1(
        decisionBatchId = DECISION_BATCH_ID,
        decisionBatch = fileBinding(DECISION_BATCH_PATH, DECISION_BATCH_SIZE, DECISION_BATCH_SHA256, DECISION_BATCH_LOGICAL_DIGEST),
        originalInputBindingDigest = DECISION_BATCH_INPUT_BINDING_DIGEST,
        originalBatchLogicalDigest = DECISION_BATCH_LOGICAL_DIGEST,
        originalReviewerRef = ORIGINAL_REVIEWER_REF,
        originalReviewRound = REVIEW_ROUND,
        originalRevision = REVISION,
        reviewPacketJson = fileBinding(
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-review-packet/v1/p1-artischocken-herzen-brie-double-creme-v1/review-packet.v1.json",
            REVIEW_PACKET_JSON_SIZE,
            REVIEW_PACKET_JSON_SHA256,
            REVIEW_PACKET_LOGICAL_DIGEST,
        ),
        reviewPacketMarkdown = fileBinding(
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-review-packet/v1/p1-artischocken-herzen-brie-double-creme-v1/review-packet.v1.md",
            REVIEW_PACKET_MARKDOWN_SIZE,
            REVIEW_PACKET_MARKDOWN_SHA256,
            REVIEW_PACKET_LOGICAL_DIGEST,
        ),
        reviewPacketInputBindingDigest = REVIEW_PACKET_INPUT_BINDING_DIGEST,
        reviewPacketBindingDigest = REVIEW_PACKET_BINDING_DIGEST,
        reviewPacketLogicalDigest = REVIEW_PACKET_LOGICAL_DIGEST,
        supplementJson = fileBinding(
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-direct-evidence-supplement/v1/p1-artischocken-herzen-brie-double-creme-v1/direct-evidence-supplement.v1.json",
            SUPPLEMENT_JSON_SIZE,
            SUPPLEMENT_JSON_SHA256,
            SUPPLEMENT_LOGICAL_DIGEST,
        ),
        supplementMarkdown = fileBinding(
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-direct-evidence-supplement/v1/p1-artischocken-herzen-brie-double-creme-v1/direct-evidence-supplement.v1.md",
            SUPPLEMENT_MARKDOWN_SIZE,
            SUPPLEMENT_MARKDOWN_SHA256,
            SUPPLEMENT_LOGICAL_DIGEST,
        ),
        supplementBindingDigest = SUPPLEMENT_BINDING_DIGEST,
        supplementLogicalDigest = SUPPLEMENT_LOGICAL_DIGEST,
        corpus = fileBinding(
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-review-corpus/v1/review-corpus.v1.json",
            CORPUS_SIZE,
            CORPUS_SHA256,
            CORPUS_LOGICAL_DIGEST,
        ),
        corpusLogicalDigest = CORPUS_LOGICAL_DIGEST,
        corpusBindingDigest = CORPUS_BINDING_DIGEST,
        contractBaselineHead = CONTRACT_BASELINE_HEAD,
    )

    fun create(
        inputBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketInputBindingV1,
        items: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1>,
        contextLimitations: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1> = REQUIRED_CONTEXT_LIMITATIONS,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1 {
        val order = FROZEN_REVIEW_UNITS.mapIndexed { index, entry -> entry.reviewUnitId to index }.toMap()
        val normalized = items.sortedWith(compareBy({ order[it.reviewUnitId] ?: Int.MAX_VALUE }, { it.reviewUnitId }))
            .map { item ->
                val bound = item.copy(
                    contextLimitations = contextLimitations.distinct().sortedBy { it.ordinal },
                    allowedValidationAssessments = VALIDATION_ASSESSMENTS,
                    allowedValidationReasonCodes = VALIDATION_REASON_CODES,
                    itemBindingDigest = "",
                    itemLogicalDigest = "",
                )
                val itemBound = bound.copy(itemBindingDigest = itemBindingDigest(bound))
                itemBound.copy(itemLogicalDigest = itemLogicalDigest(itemBound))
            }
        val unsigned = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1(
            CONTRACT_ID,
            VERSION,
            STATE,
            PACKET_ID,
            inputBinding,
            normalized,
            contextLimitations.distinct().sortedBy { it.ordinal },
            VALIDATION_ASSESSMENTS,
            VALIDATION_REASON_CODES,
            deriveCounters(normalized),
            "",
            "",
        )
        val bound = unsigned.copy(packetBindingDigest = packetBindingDigest(unsigned))
        return bound.copy(packetLogicalDigest = packetLogicalDigest(bound))
    }

    fun deriveCounters(
        items: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1>,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketCountersV1(
        packetItems = items.size,
        distinctReviewUnits = items.map { it.reviewUnitId }.distinct().size,
        originalConfirmDecisions = items.count { it.originalDecision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION },
        originalRejectDecisions = items.count { it.originalDecision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION },
        originalAbstainDecisions = items.count { it.originalDecision.name.startsWith("ABSTAIN") },
        originalEscalateDecisions = items.count { it.originalDecision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE },
        sourceEvidenceRecords = items.sumOf { it.directEvidence.count { evidence -> evidence.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION } },
        catalogEvidenceRecords = items.sumOf { it.directEvidence.count { evidence -> evidence.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD } },
        authorityEvidenceRecords = items.sumOf { it.directEvidence.count { evidence -> evidence.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD } },
        directEvidenceReferences = items.sumOf { it.directEvidence.size },
        distinctEvidenceReferences = items.flatMap { it.directEvidence }.map { it.evidenceReferenceId }.distinct().size,
        supportsAssociationEvidence = items.sumOf { it.directEvidence.count { evidence -> evidence.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION } },
        contradictsAssociationEvidence = items.sumOf { it.directEvidence.count { evidence -> evidence.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION } },
        contextOnlyEvidence = items.sumOf { it.directEvidence.count { evidence -> evidence.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY } },
        itemsWithAlternativeCanonicalProposal = 0,
        itemsWithValidationAssessment = 0,
        itemsWithDownstreamRoute = 0,
    )

    fun originalDecisionIdentity(item: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1): String = sha256(
        buildString {
            appendLine("$CONTRACT_ID:ORIGINAL_DECISION_IDENTITY_V1")
            appendLine("batchLogicalDigest=$DECISION_BATCH_LOGICAL_DIGEST")
            appendLine("reviewUnitId=${item.reviewUnitId}")
            appendLine("stableEntryId=${item.stableEntryId}")
            appendLine("canonicalEntityId=${item.canonicalEntityId}")
            appendLine("reviewer=$ORIGINAL_REVIEWER_REF")
            appendLine("round=$REVIEW_ROUND")
            appendLine("revision=$REVISION")
            appendLine("decision=${item.originalDecision.name}")
            item.originalReasonCodes.forEach { appendLine("reason=${it.name}") }
            item.originalFieldsForIdentity.forEach { appendLine("evidence=$it") }
            appendLine("rationaleSha256=${item.originalRationaleSha256}")
        },
    )

    fun itemBindingDigest(item: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1): String = sha256(
        buildString {
            appendLine(ITEM_BINDING_DIGEST_DOMAIN)
            appendLine("packetId=$PACKET_ID")
            appendLine("reviewUnitId=${item.reviewUnitId}")
            appendLine("stableEntryId=${item.stableEntryId}")
            appendLine("canonicalEntityId=${item.canonicalEntityId}")
            appendLine("canonicalLabel=${item.canonicalLabel}")
            appendLine("originalDecisionIdentity=${item.originalDecisionIdentity}")
            item.directEvidence.forEach { appendLine("evidence=${it.evidenceReferenceId}") }
        },
    )

    fun itemLogicalDigest(item: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1): String = sha256(
        buildString {
            appendLine("$ITEM_BINDING_DIGEST_DOMAIN:LOGICAL")
            appendLine(item.copy(itemLogicalDigest = ""))
            appendLine("itemBindingDigest=${item.itemBindingDigest}")
        },
    )

    fun packetBindingDigest(packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1): String = sha256(
        buildString {
            appendLine(PACKET_BINDING_DIGEST_DOMAIN)
            appendLine("contractId=${packet.contractId}")
            appendLine("version=${packet.version}")
            appendLine("state=${packet.state}")
            appendLine("packetId=${packet.packetId}")
            appendLine("contractBaselineHead=${packet.inputBinding.contractBaselineHead}")
            appendLine("decisionBatch=${packet.inputBinding.decisionBatch.sha256}")
            appendLine("reviewPacket=${packet.inputBinding.reviewPacketBindingDigest}")
            appendLine("supplement=${packet.inputBinding.supplementBindingDigest}")
            appendLine("corpus=${packet.inputBinding.corpusBindingDigest}")
            packet.items.forEach { appendLine("item=${it.itemBindingDigest}") }
        },
    )

    fun packetLogicalDigest(packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1): String = sha256(
        buildString {
            appendLine(PACKET_LOGICAL_DIGEST_DOMAIN)
            appendLine("packetBindingDigest=${packet.packetBindingDigest}")
            appendLine("contextLimitations=${packet.contextLimitations}")
            appendLine("allowedAssessments=${packet.allowedValidationAssessments}")
            appendLine("allowedReasons=${packet.allowedValidationReasonCodes}")
            packet.items.forEach { appendLine("item=${it.itemLogicalDigest}") }
            appendLine("counters=${packet.counters}")
        },
    )

    fun validate(packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1 {
        if (packet.contractId != CONTRACT_ID) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_CONTRACT_ID)
        if (packet.version != VERSION) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_CONTRACT_VERSION)
        if (packet.state != STATE) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_STATE)
        if (packet.packetId != PACKET_ID) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_PACKET_ID)
        inputBindingFailure(packet.inputBinding)?.let { return invalid(it) }
        if (packet.items.size != FROZEN_REVIEW_UNITS.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_ITEM_COUNT)
        if (packet.contextLimitations != REQUIRED_CONTEXT_LIMITATIONS) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_CONTEXT_LIMITATIONS)
        if (packet.allowedValidationAssessments != VALIDATION_ASSESSMENTS || packet.allowedValidationReasonCodes != VALIDATION_REASON_CODES) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_VALIDATION_OPTIONS)
        if (packet.items.map { it.reviewUnitId } != FROZEN_REVIEW_UNITS.map { it.reviewUnitId }) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_ITEM_ORDER)
        if (packet.items.map { it.reviewUnitId }.distinct().size != packet.items.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.DUPLICATE_REVIEW_UNIT)
        val evidenceOccurrences = packet.items.flatMap { item -> item.directEvidence.map { it.evidenceReferenceId to item.reviewUnitId } }
        if (evidenceOccurrences.map { it.first }.distinct().size != evidenceOccurrences.size) {
            val duplicate = evidenceOccurrences.groupBy { it.first }.values.first { it.size > 1 }
            return invalid(
                if (duplicate.map { it.second }.distinct().size > 1) {
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.CROSS_UNIT_EVIDENCE_REFERENCE
                } else {
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.DUPLICATE_EVIDENCE_REFERENCE
                },
            )
        }
        packet.items.forEachIndexed { index, item ->
            val expected = FROZEN_REVIEW_UNITS[index]
            if (item.reviewUnitId != expected.reviewUnitId || item.stableEntryId != expected.stableEntryId || item.canonicalEntityId != expected.canonicalEntityId || item.canonicalLabel != expected.expectedDisplayLabel) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.REVIEW_UNIT_BINDING_MISMATCH)
            itemFailure(index, item)?.let { return invalid(it) }
        }
        if (packet.counters != deriveCounters(packet.items)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_COUNTERS)
        if (packet.packetBindingDigest != packetBindingDigest(packet)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.PACKET_BINDING_DIGEST_MISMATCH)
        if (packet.packetLogicalDigest != packetLogicalDigest(packet)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.PACKET_LOGICAL_DIGEST_MISMATCH)
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Valid
    }

    private fun inputBindingFailure(
        binding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketInputBindingV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1? {
        val expected = frozenInputBinding()
        if (binding.decisionBatchId != expected.decisionBatchId || binding.decisionBatch != expected.decisionBatch ||
            binding.originalInputBindingDigest != expected.originalInputBindingDigest || binding.originalBatchLogicalDigest != expected.originalBatchLogicalDigest ||
            binding.originalReviewerRef != expected.originalReviewerRef || binding.originalReviewRound != expected.originalReviewRound || binding.originalRevision != expected.originalRevision
        ) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_DECISION_BATCH_BINDING
        if (binding.reviewPacketJson != expected.reviewPacketJson || binding.reviewPacketMarkdown != expected.reviewPacketMarkdown || binding.reviewPacketInputBindingDigest != expected.reviewPacketInputBindingDigest ||
            binding.reviewPacketBindingDigest != expected.reviewPacketBindingDigest || binding.reviewPacketLogicalDigest != expected.reviewPacketLogicalDigest
        ) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_REVIEW_PACKET_BINDING
        if (binding.supplementJson != expected.supplementJson || binding.supplementMarkdown != expected.supplementMarkdown || binding.supplementBindingDigest != expected.supplementBindingDigest || binding.supplementLogicalDigest != expected.supplementLogicalDigest) {
            return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_SUPPLEMENT_BINDING
        }
        if (binding.corpus != expected.corpus || binding.corpusLogicalDigest != expected.corpusLogicalDigest || binding.corpusBindingDigest != expected.corpusBindingDigest) {
            return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_CORPUS_BINDING
        }
        if (binding.contractBaselineHead != CONTRACT_BASELINE_HEAD) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_CONTRACT_BASELINE
        return null
    }

    private fun itemFailure(index: Int, item: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1? {
        if (item.originalDecisionIdentity != originalDecisionIdentity(item) || !SHA256.matches(item.originalDecisionIdentity)) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.ORIGINAL_DECISION_IDENTITY_MISMATCH
        if (item.originalRationale.isBlank() || item.originalRationaleSha256 != sha256(item.originalRationale)) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_ORIGINAL_RATIONALE
        val expectedSelection = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.FROZEN_SELECTIONS[index]
        if (item.originalDecision != expectedSelection.decision || item.originalReasonCodes != expectedSelection.reasonCodes || item.alternativeCanonicalProposal != null) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.ORIGINAL_DECISION_MISMATCH
        if (item.originalEvidenceReferenceIds != expectedSelection.evidenceReferenceIds.sorted()) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.ORIGINAL_EVIDENCE_BINDING_MISMATCH
        val sourceContext = item.sourceContext
        if (sourceContext.recordKind.source != sourceContext.source || sourceContext.evidenceReference.isBlank() ||
            sourceContext.findingOccurrenceIds.isEmpty() || sourceContext.findingOccurrenceIds != sourceContext.findingOccurrenceIds.distinct().sorted() ||
            sourceContext.findingOccurrenceIds.any { !SHA256.matches(it) } || sourceContext.primaryValues != sourceContext.primaryValues.distinct().sorted() ||
            sourceContext.selectionReasons != sourceContext.selectionReasons.distinct().sortedBy { it.ordinal } ||
            sourceContext.recurringGroupMemberships != sourceContext.recurringGroupMemberships.sortedWith(compareBy({ it.priorityClass.ordinal }, { it.primaryValue }))
        ) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_SOURCE_CONTEXT
        if (item.originalFields != item.originalFields.distinctBy { it.fieldName }.sortedBy { it.fieldName }) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_SOURCE_CONTEXT
        if (item.originalFields.any { it.fieldName.isBlank() || it.originalCharacterCount != it.originalValue.length || it.originalValueSha256 != sha256(it.originalValue) }) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.FIELD_VALUE_DIGEST_MISMATCH
        val target = item.targetContext
        if (target.canonicalEntityId != item.canonicalEntityId || target.expectedDisplayLabel.isBlank() ||
            target.registryResolution != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED || target.authorityResolution != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED ||
            target.identityTerms != target.identityTerms.distinct().sorted() || target.aliasTerms != target.aliasTerms.distinct().sorted() ||
            target.targetContextDigest != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(target)
        ) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_SOURCE_CONTEXT
        val evidence = item.directEvidence
        if (evidence.size != 3) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_EVIDENCE_COUNT
        if (evidence.map { it.evidenceReferenceId }.distinct().size != evidence.size) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.DUPLICATE_EVIDENCE_REFERENCE
        if (evidence.map { it.evidenceReferenceId }.sorted() != item.originalEvidenceReferenceIds.sorted()) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.UNEXPECTED_EVIDENCE
        if (evidence.any { it.reviewUnitId != item.reviewUnitId || it.directness != HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT }) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_EVIDENCE_DIRECTNESS
        val kinds = evidence.map { it.kind }.toSet()
        if (kinds != setOf(HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD)) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_EVIDENCE_KIND
        evidence.forEach { card ->
            if (!SHA256.matches(card.evidenceReferenceId)) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.EVIDENCE_REFERENCE_MISMATCH
            if (card.artifact.validate() != null || card.recordReference.isBlank() || card.fields.isEmpty() || card.fields != card.fields.distinctBy { it.fieldReference }.sortedBy { it.fieldReference } || card.fields.any { it.validate() != null }) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_EVIDENCE_BINDING
            if (card.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION) {
                val projection = card.sourceProjection ?: return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_SOURCE_PROJECTION
                if (projection.source != item.sourceContext.source || projection.recordKind != item.sourceContext.recordKind || projection.recordReference != card.recordReference) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.RECORD_BINDING_MISMATCH
            } else if (card.sourceProjection != null) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_SOURCE_PROJECTION
        }
        val source = evidence.single { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }
        val catalog = evidence.single { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD }
        val authority = evidence.single { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD }
        val expectedSourcePosition = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
        val expectedCatalogPosition = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY
        if (source.position != expectedSourcePosition || catalog.position != expectedCatalogPosition || authority.position != HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_EVIDENCE_POSITION
        if (item.contextLimitations != REQUIRED_CONTEXT_LIMITATIONS || item.allowedValidationAssessments != VALIDATION_ASSESSMENTS || item.allowedValidationReasonCodes != VALIDATION_REASON_CODES) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_CONTEXT_LIMITATIONS
        if (item.itemBindingDigest != itemBindingDigest(item)) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.ITEM_BINDING_DIGEST_MISMATCH
        if (item.itemLogicalDigest != itemLogicalDigest(item)) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.ITEM_LOGICAL_DIGEST_MISMATCH
        return null
    }

    private fun fileBinding(path: String, size: Long, sha256: String, logicalDigest: String) =
        HimZeroCandidateRecoveryHumanReviewFileBindingV1(path, size, sha256, logicalDigest)

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun invalid(reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Invalid(reason)

    private val SHA256 = Regex("[0-9a-f]{64}")
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1 {
    BOUND_ARTIFACTS_ONLY,
    DIRECT_EVIDENCE_NOT_AUTOMATIC_DECISION,
    ORIGINAL_DECISION_NOT_INDEPENDENTLY_VALIDATED,
    VALIDATOR_MUST_BE_INDEPENDENT,
    CHALLENGE_DOES_NOT_CHANGE_ORIGINAL,
    GOLD_EXCLUDED,
    NEGATIVE_SUPERVISION_EXCLUDED,
    TRAINING_EXCLUDED,
    AUTHORITY_MUTATION_EXCLUDED,
    CONFLICT_REQUIRES_ADJUDICATION,
    NO_EXTERNAL_SOURCE_RESEARCH,
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1 {
    INVALID_CONTRACT_ID,
    INVALID_CONTRACT_VERSION,
    INVALID_STATE,
    INVALID_PACKET_ID,
    INVALID_INPUT_BINDING,
    INPUT_BINDING_DIGEST_MISMATCH,
    INVALID_DECISION_BATCH_BINDING,
    INVALID_REVIEW_PACKET_BINDING,
    INVALID_SUPPLEMENT_BINDING,
    INVALID_CORPUS_BINDING,
    INVALID_CONTRACT_BASELINE,
    INVALID_ITEM_COUNT,
    UNKNOWN_REVIEW_UNIT,
    MISSING_REVIEW_UNIT,
    DUPLICATE_REVIEW_UNIT,
    INVALID_ITEM_ORDER,
    REVIEW_UNIT_BINDING_MISMATCH,
    ORIGINAL_DECISION_IDENTITY_MISMATCH,
    ORIGINAL_DECISION_MISMATCH,
    ORIGINAL_EVIDENCE_BINDING_MISMATCH,
    INVALID_ORIGINAL_RATIONALE,
    INVALID_SOURCE_CONTEXT,
    MISSING_DIRECT_EVIDENCE,
    INVALID_EVIDENCE_COUNT,
    UNEXPECTED_EVIDENCE,
    DUPLICATE_EVIDENCE_REFERENCE,
    CROSS_UNIT_EVIDENCE_REFERENCE,
    INVALID_EVIDENCE_DIRECTNESS,
    INVALID_EVIDENCE_POSITION,
    INVALID_EVIDENCE_KIND,
    EVIDENCE_REFERENCE_MISMATCH,
    INVALID_EVIDENCE_BINDING,
    INVALID_SOURCE_PROJECTION,
    ARTIFACT_BINDING_MISMATCH,
    RECORD_BINDING_MISMATCH,
    FIELD_VALUE_DIGEST_MISMATCH,
    INVALID_CONTEXT_LIMITATIONS,
    INVALID_VALIDATION_OPTIONS,
    FORBIDDEN_PRESELECTED_ASSESSMENT,
    FORBIDDEN_DOWNSTREAM_ROUTE,
    FORBIDDEN_VALIDATOR_ASSIGNMENT,
    INVALID_COUNTERS,
    ITEM_BINDING_DIGEST_MISMATCH,
    ITEM_LOGICAL_DIGEST_MISMATCH,
    PACKET_BINDING_DIGEST_MISMATCH,
    PACKET_LOGICAL_DIGEST_MISMATCH,
    FORBIDDEN_GOLD_SEMANTICS,
    FORBIDDEN_NEGATIVE_SUPERVISION_SEMANTICS,
    FORBIDDEN_TRAINING_SEMANTICS,
    FORBIDDEN_AUTHORITY_MUTATION_SEMANTICS,
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1 {
    val valid: Boolean
    data object Valid : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1 { override val valid = true }
    data class Invalid(val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1 { override val valid = false }
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketInputBindingV1(
    val decisionBatchId: String,
    val decisionBatch: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val originalInputBindingDigest: String,
    val originalBatchLogicalDigest: String,
    val originalReviewerRef: String,
    val originalReviewRound: Int,
    val originalRevision: Int,
    val reviewPacketJson: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val reviewPacketMarkdown: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val reviewPacketInputBindingDigest: String,
    val reviewPacketBindingDigest: String,
    val reviewPacketLogicalDigest: String,
    val supplementJson: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val supplementMarkdown: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val supplementBindingDigest: String,
    val supplementLogicalDigest: String,
    val corpus: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val corpusLogicalDigest: String,
    val corpusBindingDigest: String,
    val contractBaselineHead: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1(
    val reviewUnitId: String,
    val stableEntryId: String,
    val canonicalEntityId: String,
    val canonicalLabel: String,
    val sourceContext: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1,
    val originalFields: List<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1>,
    val originalDecisionIdentity: String,
    val originalDecision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
    val originalReasonCodes: List<HimZeroCandidateRecoveryHumanReviewReasonCodeV1>,
    val originalEvidenceReferenceIds: List<String>,
    val originalRationale: String,
    val originalRationaleSha256: String,
    val alternativeCanonicalProposal: String?,
    val targetContext: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1,
    val directEvidence: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1>,
    val contextLimitations: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1>,
    val allowedValidationAssessments: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1>,
    val allowedValidationReasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1>,
    val itemBindingDigest: String,
    val itemLogicalDigest: String,
) {
    val originalFieldsForIdentity: List<String>
        get() = originalEvidenceReferenceIds.sorted()
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketCountersV1(
    val packetItems: Int,
    val distinctReviewUnits: Int,
    val originalConfirmDecisions: Int,
    val originalRejectDecisions: Int,
    val originalAbstainDecisions: Int,
    val originalEscalateDecisions: Int,
    val sourceEvidenceRecords: Int,
    val catalogEvidenceRecords: Int,
    val authorityEvidenceRecords: Int,
    val directEvidenceReferences: Int,
    val distinctEvidenceReferences: Int,
    val supportsAssociationEvidence: Int,
    val contradictsAssociationEvidence: Int,
    val contextOnlyEvidence: Int,
    val itemsWithAlternativeCanonicalProposal: Int,
    val itemsWithValidationAssessment: Int,
    val itemsWithDownstreamRoute: Int,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1(
    val contractId: String,
    val version: String,
    val state: String,
    val packetId: String,
    val inputBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketInputBindingV1,
    val items: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1>,
    val contextLimitations: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1>,
    val allowedValidationAssessments: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1>,
    val allowedValidationReasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1>,
    val counters: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketCountersV1,
    val packetBindingDigest: String,
    val packetLogicalDigest: String,
)
