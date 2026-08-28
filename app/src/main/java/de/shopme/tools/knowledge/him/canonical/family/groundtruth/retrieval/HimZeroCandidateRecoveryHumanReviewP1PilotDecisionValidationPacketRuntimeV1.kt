package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.io.File

/**
 * Materializes the independent decision-validation context from already bound,
 * typed pilot inputs. This runtime creates no validation decision and has no
 * external ports.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PACKET_RUNTIME_V1"
    const val RUNTIME_CONTRACT_ID = CONTRACT_ID
    const val VERSION = "1"
    const val RUNTIME_VERSION = VERSION
    const val STATE = "INDEPENDENT_VALIDATION_CONTEXT_ONLY"

    fun execute(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1 {
        if (!request.enabled) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Disabled
        return try {
            validateRequest(request)
            val packet = materialize(request)
            when (
                val persisted = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceRequestV1(
                        enabled = true,
                        outputRoot = request.outputRoot,
                        packet = packet,
                    ),
                )
            ) {
                is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Completed -> {
                    val json = request.outputRoot.toPath().resolve(persisted.jsonPath).toFile()
                    val reloaded = try {
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.readPacket(json)
                    } catch (_: Throwable) {
                        fail(PacketRuntimeFailureReason.RELOAD_MISMATCH, "reload")
                    }
                    if (reloaded != packet) fail(PacketRuntimeFailureReason.RELOAD_MISMATCH, "reload")
                    when (HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.validate(reloaded)) {
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Valid -> Unit
                        is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Invalid ->
                            fail(PacketRuntimeFailureReason.PACKET_CONTRACT_VALIDATION_FAILED, "packet")
                    }
                    if (persisted.jsonPath != "${packet.packetId}/${HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.JSON_FILE_NAME}" ||
                        persisted.markdownPath != "${packet.packetId}/${HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.MARKDOWN_FILE_NAME}" ||
                        persisted.jsonPath.startsWith('/') || persisted.markdownPath.startsWith('/')
                    ) fail(PacketRuntimeFailureReason.UNEXPECTED_OUTPUT_STATE, "paths")
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Completed(
                        packet = reloaded,
                        persistenceStatus = persisted.status,
                        jsonPath = persisted.jsonPath,
                        markdownPath = persisted.markdownPath,
                        counters = runtimeCounters(reloaded),
                        packetBindingDigest = persisted.packetBindingDigest,
                        packetLogicalDigest = persisted.packetLogicalDigest,
                        jsonSha256 = persisted.jsonSha256,
                        markdownSha256 = persisted.markdownSha256,
                    )
                }
                is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Failed ->
                    fail(PacketRuntimeFailureReason.PERSISTENCE_FAILED, "persistence")
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Disabled ->
                    fail(PacketRuntimeFailureReason.PERSISTENCE_FAILED, "persistence")
            }
        } catch (failure: RuntimeFailure) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Failed(
                failure.reason,
                failure.safeContext,
            )
        } catch (_: Throwable) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Failed(
                PacketRuntimeFailureReason.INTERNAL_INVARIANT_VIOLATION,
                "runtime",
            )
        }
    }

    private fun validateRequest(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1,
    ) {
        if (request.runtimeContractId != CONTRACT_ID || request.runtimeVersion != VERSION) {
            fail(PacketRuntimeFailureReason.INVALID_REQUEST, "runtime-contract")
        }
        if (!HEAD.matches(request.implementationHead)) {
            fail(PacketRuntimeFailureReason.INVALID_IMPLEMENTATION_HEAD, "implementation-head")
        }
        if (request.outputRoot.path.isBlank()) fail(PacketRuntimeFailureReason.INVALID_REQUEST, "output-root")
        when (HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.validate(request.decisionBatch)) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1.Invalid ->
                fail(PacketRuntimeFailureReason.INVALID_DECISION_BATCH, "decision-batch")
        }
        val mission = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION
        when (
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.validate(
                request.reviewPacket,
                mission,
                request.reviewPacket.humanReviewInputBinding,
            )
        ) {
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Invalid ->
                fail(PacketRuntimeFailureReason.INVALID_REVIEW_PACKET, "review-packet")
        }
        when (HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.validate(request.directEvidenceSupplement)) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Invalid ->
                fail(PacketRuntimeFailureReason.INVALID_DIRECT_EVIDENCE_SUPPLEMENT, "supplement")
        }
        val expected = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.frozenInputBinding()
        if (request.decisionBatch.packetBinding.inputBindingDigest != expected.reviewPacketInputBindingDigest ||
            request.decisionBatch.packetBinding.bindingDigest != expected.reviewPacketBindingDigest ||
            request.decisionBatch.packetBinding.logicalDigest != expected.reviewPacketLogicalDigest ||
            request.decisionBatch.supplementBinding.bindingDigest != expected.supplementBindingDigest ||
            request.decisionBatch.supplementBinding.logicalDigest != expected.supplementLogicalDigest ||
            request.decisionBatch.corpusBindingDigest != expected.corpusBindingDigest
        ) fail(PacketRuntimeFailureReason.INPUT_BINDING_MISMATCH, "decision-bindings")
        val supplementBinding = request.directEvidenceSupplement.binding
        if (supplementBinding.missionContractId != mission.contractId ||
            supplementBinding.missionVersion != mission.version ||
            supplementBinding.missionId != mission.missionId ||
            supplementBinding.scopeId != mission.scopeId ||
            supplementBinding.missionSelectionDigest != mission.selectionDigest ||
            supplementBinding.packetInputBindingDigest != expected.reviewPacketInputBindingDigest ||
            supplementBinding.packetBindingDigest != expected.reviewPacketBindingDigest ||
            supplementBinding.packetLogicalDigest != expected.reviewPacketLogicalDigest ||
            supplementBinding.corpusBindingDigest != expected.corpusBindingDigest
        ) fail(PacketRuntimeFailureReason.INPUT_BINDING_MISMATCH, "supplement-bindings")
        if (request.reviewPacket.missionBinding != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.missionBinding(mission)) {
            fail(PacketRuntimeFailureReason.MISSION_SCOPE_SUBMISSION_MISMATCH, "mission")
        }
        val expectedUnitIds = mission.entries.map { it.reviewUnitId }
        if (request.decisionBatch.selections.map { it.reviewUnitId } != expectedUnitIds ||
            request.reviewPacket.items.map { it.reviewUnitId } != expectedUnitIds ||
            request.directEvidenceSupplement.bundles.map { it.reviewUnit.reviewUnitId } != expectedUnitIds
        ) fail(PacketRuntimeFailureReason.REVIEW_UNIT_SET_MISMATCH, "units")
        request.decisionBatch.selections.forEach { selection ->
            val item = request.reviewPacket.items.singleOrNull { it.reviewUnitId == selection.reviewUnitId }
                ?: fail(PacketRuntimeFailureReason.MISSING_REVIEW_UNIT, "unit")
            val bundle = request.directEvidenceSupplement.bundles.singleOrNull { it.reviewUnit.reviewUnitId == selection.reviewUnitId }
                ?: fail(PacketRuntimeFailureReason.MISSING_REVIEW_UNIT, "unit")
            if (item.stableEntryId != selection.stableEntryId || item.canonicalEntityId != selection.canonicalEntityId) {
                fail(PacketRuntimeFailureReason.REVIEW_UNIT_BINDING_MISMATCH, "unit")
            }
            if (bundle.reviewUnit.stableEntryId != item.stableEntryId ||
                bundle.reviewUnit.canonicalEntityId != item.canonicalEntityId ||
                bundle.packetItemBindingDigest != item.itemBindingDigest ||
                bundle.evidence.map { it.evidenceReferenceId }.sorted() != selection.evidenceReferenceIds.sorted()
            ) fail(PacketRuntimeFailureReason.EVIDENCE_BINDING_MISMATCH, "evidence")
        }
    }

    private fun materialize(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1 {
        val expectedBinding = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.frozenInputBinding()
        val items = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.FROZEN_REVIEW_UNITS.map { missionEntry ->
            val selection = request.decisionBatch.selections.single { it.reviewUnitId == missionEntry.reviewUnitId }
            val reviewItem = request.reviewPacket.items.single { it.reviewUnitId == missionEntry.reviewUnitId }
            val bundle = request.directEvidenceSupplement.bundles.single { it.reviewUnit.reviewUnitId == missionEntry.reviewUnitId }
            val rationale = "Original human-review decision bound for ${missionEntry.reviewUnitId}."
            val base = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1(
                reviewUnitId = missionEntry.reviewUnitId,
                stableEntryId = selection.stableEntryId,
                canonicalEntityId = selection.canonicalEntityId,
                canonicalLabel = missionEntry.expectedDisplayLabel,
                sourceContext = reviewItem.sourceContext,
                originalFields = reviewItem.originalFields,
                originalDecisionIdentity = "",
                originalDecision = selection.decision,
                originalReasonCodes = selection.reasonCodes,
                originalEvidenceReferenceIds = selection.evidenceReferenceIds.sorted(),
                originalRationale = rationale,
                originalRationaleSha256 = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.run {
                    sha256ForRuntime(rationale)
                },
                alternativeCanonicalProposal = null,
                targetContext = reviewItem.targetContext,
                directEvidence = bundle.evidence.sortedBy { it.evidenceReferenceId },
                contextLimitations = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.REQUIRED_CONTEXT_LIMITATIONS,
                allowedValidationAssessments = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_ASSESSMENTS,
                allowedValidationReasonCodes = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_REASON_CODES,
                itemBindingDigest = "",
                itemLogicalDigest = "",
            )
            base.copy(
                originalDecisionIdentity = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.originalDecisionIdentity(base),
            )
        }
        val packet = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.create(expectedBinding, items)
        when (HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.validate(packet)) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Valid -> return packet
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Invalid ->
                fail(PacketRuntimeFailureReason.PACKET_CONTRACT_VALIDATION_FAILED, "packet")
        }
    }

    private fun runtimeCounters(
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeCountersV1(
        packetItems = packet.counters.packetItems,
        distinctReviewUnits = packet.counters.distinctReviewUnits,
        confirmDecisions = packet.counters.originalConfirmDecisions,
        rejectDecisions = packet.counters.originalRejectDecisions,
        directEvidenceReferences = packet.counters.directEvidenceReferences,
        distinctEvidenceReferences = packet.counters.distinctEvidenceReferences,
        supportsAssociationEvidence = packet.counters.supportsAssociationEvidence,
        contradictsAssociationEvidence = packet.counters.contradictsAssociationEvidence,
        contextOnlyEvidence = packet.counters.contextOnlyEvidence,
    )

    private fun fail(reason: PacketRuntimeFailureReason, safeContext: String): Nothing = throw RuntimeFailure(reason, safeContext)

    private data class RuntimeFailure(val reason: PacketRuntimeFailureReason, val safeContext: String) : IllegalArgumentException()

    private val HEAD = Regex("[0-9a-f]{40}")
}

private fun HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.sha256ForRuntime(value: String): String =
    java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1 {
    INVALID_REQUEST,
    INVALID_IMPLEMENTATION_HEAD,
    INVALID_DECISION_BATCH,
    INVALID_REVIEW_PACKET,
    INVALID_DIRECT_EVIDENCE_SUPPLEMENT,
    INPUT_BINDING_MISMATCH,
    CORPUS_BINDING_MISMATCH,
    MISSION_SCOPE_SUBMISSION_MISMATCH,
    REVIEW_UNIT_SET_MISMATCH,
    MISSING_REVIEW_UNIT,
    DUPLICATE_REVIEW_UNIT,
    UNEXPECTED_REVIEW_UNIT,
    REVIEW_UNIT_BINDING_MISMATCH,
    EVIDENCE_BINDING_MISMATCH,
    EVIDENCE_CARDINALITY_MISMATCH,
    DECISION_MISMATCH,
    REASON_CODE_MISMATCH,
    PACKET_CONTRACT_VALIDATION_FAILED,
    ITEM_DIGEST_MISMATCH,
    PACKET_BINDING_DIGEST_MISMATCH,
    PACKET_LOGICAL_DIGEST_MISMATCH,
    PERSISTENCE_FAILED,
    RELOAD_MISMATCH,
    UNEXPECTED_OUTPUT_STATE,
    INTERNAL_INVARIANT_VIOLATION,
}

private typealias PacketRuntimeFailureReason = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1(
    val enabled: Boolean,
    val outputRoot: File,
    val implementationHead: String,
    val decisionBatch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1,
    val reviewPacket: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1,
    val directEvidenceSupplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
    val runtimeContractId: String = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1.CONTRACT_ID,
    val runtimeVersion: String = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1.VERSION,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeCountersV1(
    val packetItems: Int,
    val distinctReviewUnits: Int,
    val confirmDecisions: Int,
    val rejectDecisions: Int,
    val directEvidenceReferences: Int,
    val distinctEvidenceReferences: Int,
    val supportsAssociationEvidence: Int,
    val contradictsAssociationEvidence: Int,
    val contextOnlyEvidence: Int,
)

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1 {
    data object Disabled : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1

    data class Completed(
        val packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
        val persistenceStatus: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1,
        val jsonPath: String,
        val markdownPath: String,
        val counters: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeCountersV1,
        val packetBindingDigest: String,
        val packetLogicalDigest: String,
        val jsonSha256: String,
        val markdownSha256: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1
}
