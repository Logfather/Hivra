package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.security.MessageDigest

/** Immutable, pure selection contract for the first P1 human-review mission. */
object HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1 {
    const val CONTRACT_ID = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_MISSION_V1"
    const val VERSION = "1"
    const val MISSION_ID = "p1-artischocken-herzen-brie-double-creme-v1"
    const val SCOPE_ID = MISSION_ID
    const val SELECTION_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_MISSION_SELECTION_V1"
    const val CORPUS_FILE_SHA256 =
        "4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016"
    const val CORPUS_LOGICAL_DIGEST =
        "3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02"
    const val CORPUS_BINDING_DIGEST =
        "b1691dbf87eed143d23ea44275ec5d96bf9483e4675469af5d282d3858288e81"
    const val REVIEW_ROUND = 1
    const val MAX_NEW_DECISION_RECORDS = 4
    const val EXPECTED_SELECTED_GROUPS = 2
    const val EXPECTED_SELECTED_CORPUS_ENTRIES = 4
    const val EXPECTED_SELECTED_REVIEW_UNITS = 4
    const val EXPECTED_DISTINCT_CANONICAL_TARGETS = 2
    const val EXPECTED_MULTI_TARGET_ENTRIES = 0
    const val FROZEN_SELECTION_DIGEST =
        "e5b639be1e04b004a8de2594bed4002c854727d15eb66420a42c4293f0f3dbcb"

    val FROZEN_MISSION: HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1 =
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1(
            contractId = CONTRACT_ID,
            version = VERSION,
            missionId = MISSION_ID,
            scopeId = SCOPE_ID,
            priority = HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET,
            corpusFileSha256 = CORPUS_FILE_SHA256,
            corpusLogicalDigest = CORPUS_LOGICAL_DIGEST,
            corpusBindingDigest = CORPUS_BINDING_DIGEST,
            reviewRound = REVIEW_ROUND,
            maxNewDecisionRecords = MAX_NEW_DECISION_RECORDS,
            expectedSelectedGroups = EXPECTED_SELECTED_GROUPS,
            expectedSelectedCorpusEntries = EXPECTED_SELECTED_CORPUS_ENTRIES,
            expectedSelectedReviewUnits = EXPECTED_SELECTED_REVIEW_UNITS,
            expectedDistinctCanonicalTargets = EXPECTED_DISTINCT_CANONICAL_TARGETS,
            expectedMultiTargetEntries = EXPECTED_MULTI_TARGET_ENTRIES,
            entries = listOf(
                entry(
                    groupOrdinal = 1,
                    groupDisplayValue = "Artischocken Herzen",
                    stableEntryId = "a98f67c7aa8af729e27d402585df4c78d2a1a9c3f85e636dd7d257cba1810ebd",
                    canonicalEntityId = "ZuhV5V",
                    expectedDisplayLabel = "Artischocken",
                    reviewUnitId = "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e",
                ),
                entry(
                    groupOrdinal = 4,
                    groupDisplayValue = "Brie double crème",
                    stableEntryId = "ec4d7ccf39c1b9dbe184a0af90190cbc5d6a9e96ce3e3af121f5f59258eb5e13",
                    canonicalEntityId = "rVnyq7",
                    expectedDisplayLabel = "Crème double",
                    reviewUnitId = "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7",
                ),
                entry(
                    groupOrdinal = 1,
                    groupDisplayValue = "Artischocken Herzen",
                    stableEntryId = "6db7a77b0a3ff2471b8001b1644bc7a957efa52369df722899298c87d286ed71",
                    canonicalEntityId = "ZuhV5V",
                    expectedDisplayLabel = "Artischocken",
                    reviewUnitId = "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70",
                ),
                entry(
                    groupOrdinal = 4,
                    groupDisplayValue = "Brie double crème",
                    stableEntryId = "f4957e490b1714a1e48ca5d43ae762564603ac712842b3c2714b9f36bf2c41df",
                    canonicalEntityId = "rVnyq7",
                    expectedDisplayLabel = "Crème double",
                    reviewUnitId = "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50",
                ),
            ),
            selectionDigest = FROZEN_SELECTION_DIGEST,
        )

    fun selectionDigest(mission: HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1): String =
        sha256(
            buildString {
                appendLine(SELECTION_DIGEST_DOMAIN)
                appendLine("contractId=${mission.contractId}")
                appendLine("version=${mission.version}")
                appendLine("missionId=${mission.missionId}")
                appendLine("scopeId=${mission.scopeId}")
                appendLine("priority=${mission.priority.name}")
                appendLine("corpusFileSha256=${mission.corpusFileSha256}")
                appendLine("corpusLogicalDigest=${mission.corpusLogicalDigest}")
                appendLine("corpusBindingDigest=${mission.corpusBindingDigest}")
                appendLine("reviewRound=${mission.reviewRound}")
                appendLine("maxNewDecisionRecords=${mission.maxNewDecisionRecords}")
                appendLine("expectedSelectedGroups=${mission.expectedSelectedGroups}")
                appendLine("expectedSelectedCorpusEntries=${mission.expectedSelectedCorpusEntries}")
                appendLine("expectedSelectedReviewUnits=${mission.expectedSelectedReviewUnits}")
                appendLine("expectedDistinctCanonicalTargets=${mission.expectedDistinctCanonicalTargets}")
                appendLine("expectedMultiTargetEntries=${mission.expectedMultiTargetEntries}")
                mission.entries.forEachIndexed { index, entry ->
                    appendLine("entry[$index].groupOrdinal=${entry.groupOrdinal}")
                    appendLine("entry[$index].groupDisplayValue=${entry.groupDisplayValue}")
                    appendLine("entry[$index].stableEntryId=${entry.stableEntryId}")
                    appendLine("entry[$index].canonicalEntityId=${entry.canonicalEntityId}")
                    appendLine("entry[$index].expectedDisplayLabel=${entry.expectedDisplayLabel}")
                    appendLine("entry[$index].reviewUnitId=${entry.reviewUnitId}")
                }
            },
        )

    fun validate(): HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1 =
        validate(FROZEN_MISSION)

    fun validate(
        mission: HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1 {
        if (mission.contractId != CONTRACT_ID) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_CONTRACT_ID)
        if (mission.version != VERSION) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_CONTRACT_VERSION)
        if (mission.missionId != MISSION_ID) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_MISSION_ID)
        if (mission.scopeId != SCOPE_ID) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_SCOPE_ID)
        if (mission.priority != HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_PRIORITY)
        if (mission.corpusFileSha256 != CORPUS_FILE_SHA256 ||
            mission.corpusLogicalDigest != CORPUS_LOGICAL_DIGEST ||
            mission.corpusBindingDigest != CORPUS_BINDING_DIGEST
        ) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_CORPUS_BINDING)
        if (mission.reviewRound != REVIEW_ROUND || mission.maxNewDecisionRecords != MAX_NEW_DECISION_RECORDS ||
            mission.expectedSelectedGroups != EXPECTED_SELECTED_GROUPS ||
            mission.expectedSelectedCorpusEntries != EXPECTED_SELECTED_CORPUS_ENTRIES ||
            mission.expectedSelectedReviewUnits != EXPECTED_SELECTED_REVIEW_UNITS ||
            mission.expectedDistinctCanonicalTargets != EXPECTED_DISTINCT_CANONICAL_TARGETS ||
            mission.expectedMultiTargetEntries != EXPECTED_MULTI_TARGET_ENTRIES
        ) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_EXPECTED_COUNTERS)
        if (mission.entries.size != EXPECTED_SELECTED_CORPUS_ENTRIES) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.UNEXPECTED_SELECTION)
        if (mission.entries.map { it.groupOrdinal }.distinct().size != EXPECTED_SELECTED_GROUPS) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.UNEXPECTED_SELECTION)
        if (mission.entries.map { it.stableEntryId }.distinct().size != mission.entries.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.DUPLICATE_STABLE_ENTRY_ID)
        if (mission.entries.map { it.reviewUnitId }.distinct().size != mission.entries.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.DUPLICATE_REVIEW_UNIT_ID)
        if (mission.entries.any { !SHA256.matches(it.stableEntryId) || !ENTITY.matches(it.canonicalEntityId) || !SHA256.matches(it.reviewUnitId) }) {
            return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_REVIEW_UNIT_ID)
        }
        if (mission.entries.any { it.reviewUnitId != HimZeroCandidateRecoveryHumanReviewContractV1.reviewUnitId(it.stableEntryId, it.canonicalEntityId) }) {
            return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_REVIEW_UNIT_ID)
        }
        if (mission.entries != mission.entries.sortedWith(compareBy({ it.reviewUnitId }, { it.stableEntryId }, { it.canonicalEntityId }))) {
            return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_SELECTION_ORDER)
        }
        if (mission.entries.map { it.canonicalEntityId }.distinct().size != EXPECTED_DISTINCT_CANONICAL_TARGETS ||
            mission.entries.count { it.canonicalEntityId == "ZuhV5V" } != 2 ||
            mission.entries.count { it.canonicalEntityId == "rVnyq7" } != 2
        ) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.UNEXPECTED_SELECTION)
        if (mission.entries != FROZEN_MISSION.entries) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.UNEXPECTED_SELECTION)
        if (mission.selectionDigest != FROZEN_SELECTION_DIGEST || selectionDigest(mission) != FROZEN_SELECTION_DIGEST) {
            return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.SELECTION_DIGEST_MISMATCH)
        }
        return valid()
    }

    fun runtimeScope(inputBindingDigest: String): HimZeroCandidateRecoveryHumanReviewScopeV1 {
        if (!SHA256.matches(inputBindingDigest)) {
            throw IllegalArgumentException(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_RUNTIME_INPUT_BINDING_DIGEST.name)
        }
        val base = HimZeroCandidateRecoveryHumanReviewScopeV1(
            scopeId = SCOPE_ID,
            reviewRound = REVIEW_ROUND,
            authorizedReviewUnitIds = FROZEN_MISSION.entries.map { it.reviewUnitId }.sorted(),
            maxNewDecisionRecords = MAX_NEW_DECISION_RECORDS,
            inputBindingDigest = inputBindingDigest,
            scopeDigest = "",
        )
        return base.copy(scopeDigest = HimZeroCandidateRecoveryHumanReviewRuntimeV1.scopeDigest(base))
    }

    fun validateRuntimeScope(
        scope: HimZeroCandidateRecoveryHumanReviewScopeV1,
        inputBindingDigest: String,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1 {
        if (!SHA256.matches(inputBindingDigest)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_RUNTIME_INPUT_BINDING_DIGEST)
        return if (scope == runtimeScope(inputBindingDigest)) valid()
        else invalid(HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.RUNTIME_SCOPE_MISMATCH)
    }

    private fun entry(
        groupOrdinal: Int,
        groupDisplayValue: String,
        stableEntryId: String,
        canonicalEntityId: String,
        expectedDisplayLabel: String,
        reviewUnitId: String,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotMissionEntryV1(
        groupOrdinal,
        groupDisplayValue,
        stableEntryId,
        canonicalEntityId,
        expectedDisplayLabel,
        reviewUnitId,
    )

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun valid() = HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1.Valid

    private fun invalid(reason: HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1.Invalid(reason)

    private val SHA256 = Regex("[0-9a-f]{64}")
    private val ENTITY = Regex("[A-Za-z0-9]{6}")
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1(
    val contractId: String,
    val version: String,
    val missionId: String,
    val scopeId: String,
    val priority: HimZeroCandidateRecoveryReviewPriorityV1,
    val corpusFileSha256: String,
    val corpusLogicalDigest: String,
    val corpusBindingDigest: String,
    val reviewRound: Int,
    val maxNewDecisionRecords: Int,
    val expectedSelectedGroups: Int,
    val expectedSelectedCorpusEntries: Int,
    val expectedSelectedReviewUnits: Int,
    val expectedDistinctCanonicalTargets: Int,
    val expectedMultiTargetEntries: Int,
    val entries: List<HimZeroCandidateRecoveryHumanReviewP1PilotMissionEntryV1>,
    val selectionDigest: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotMissionEntryV1(
    val groupOrdinal: Int,
    val groupDisplayValue: String,
    val stableEntryId: String,
    val canonicalEntityId: String,
    val expectedDisplayLabel: String,
    val reviewUnitId: String,
)

enum class HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1 {
    INVALID_CONTRACT_ID,
    INVALID_CONTRACT_VERSION,
    INVALID_MISSION_ID,
    INVALID_SCOPE_ID,
    INVALID_PRIORITY,
    INVALID_CORPUS_BINDING,
    INVALID_EXPECTED_COUNTERS,
    DUPLICATE_STABLE_ENTRY_ID,
    DUPLICATE_REVIEW_UNIT_ID,
    INVALID_REVIEW_UNIT_ID,
    UNEXPECTED_SELECTION,
    INVALID_SELECTION_ORDER,
    SELECTION_DIGEST_MISMATCH,
    INVALID_RUNTIME_INPUT_BINDING_DIGEST,
    RUNTIME_SCOPE_MISMATCH,
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1 {
    val valid: Boolean

    data object Valid : HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1 {
        override val valid: Boolean = true
    }

    data class Invalid(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1 {
        override val valid: Boolean = false
    }
}
