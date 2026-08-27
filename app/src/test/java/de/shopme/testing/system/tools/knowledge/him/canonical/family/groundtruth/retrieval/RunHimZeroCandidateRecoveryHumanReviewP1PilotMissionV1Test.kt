package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionEntryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewPriorityV1
import java.lang.reflect.Modifier
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotMissionV1Test {
    @Test
    fun contractIdAndVersionAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_MISSION_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.VERSION)
    }

    @Test
    fun missionAndScopeIdsAreFrozen() {
        val mission = mission()
        assertEquals("p1-artischocken-herzen-brie-double-creme-v1", mission.missionId)
        assertEquals(mission.missionId, mission.scopeId)
        assertEquals(mission.missionId, HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.MISSION_ID)
    }

    @Test
    fun priorityRoundAndDecisionLimitAreFrozen() {
        val mission = mission()
        assertEquals(HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET, mission.priority)
        assertEquals(1, mission.reviewRound)
        assertEquals(4, mission.maxNewDecisionRecords)
    }

    @Test
    fun corpusBindingsAndExpectedCountersAreFrozen() {
        val mission = mission()
        assertEquals("4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016", mission.corpusFileSha256)
        assertEquals("3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02", mission.corpusLogicalDigest)
        assertEquals("b1691dbf87eed143d23ea44275ec5d96bf9483e4675469af5d282d3858288e81", mission.corpusBindingDigest)
        assertEquals(2, mission.expectedSelectedGroups)
        assertEquals(4, mission.expectedSelectedCorpusEntries)
        assertEquals(4, mission.expectedSelectedReviewUnits)
        assertEquals(2, mission.expectedDistinctCanonicalTargets)
        assertEquals(0, mission.expectedMultiTargetEntries)
    }

    @Test
    fun frozenMissionIsValid() {
        assertValid(mission())
    }

    @Test
    fun exactGroupsEntriesAndUnitsArePresent() {
        val mission = mission()
        assertEquals(2, mission.entries.map { it.groupOrdinal }.distinct().size)
        assertEquals(4, mission.entries.size)
        assertEquals(4, mission.entries.map { it.stableEntryId }.distinct().size)
        assertEquals(4, mission.entries.map { it.reviewUnitId }.distinct().size)
    }

    @Test
    fun exactCanonicalTargetsArePresent() {
        val mission = mission()
        assertEquals(2, mission.entries.map { it.canonicalEntityId }.distinct().size)
        assertEquals(setOf("ZuhV5V", "rVnyq7"), mission.entries.map { it.canonicalEntityId }.toSet())
        assertEquals(0, mission.entries.count { it.canonicalEntityId !in setOf("ZuhV5V", "rVnyq7") })
        assertEquals(0, mission.entries.count { it.canonicalEntityId == "" })
    }

    @Test
    fun artichokeEntriesBindTheirFrozenTarget() {
        val entries = mission().entries.filter { it.groupDisplayValue == "Artischocken Herzen" }
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.canonicalEntityId == "ZuhV5V" && it.expectedDisplayLabel == "Artischocken" })
        assertTrue(entries.all { it.groupOrdinal == 1 })
    }

    @Test
    fun brieEntriesBindTheirFrozenTarget() {
        val entries = mission().entries.filter { it.groupDisplayValue == "Brie double crème" }
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.canonicalEntityId == "rVnyq7" && it.expectedDisplayLabel == "Crème double" })
        assertTrue(entries.all { it.groupOrdinal == 4 })
    }

    @Test
    fun noAdditionalCanonicalTargetIsPresent() {
        assertEquals(setOf("ZuhV5V", "rVnyq7"), mission().entries.map { it.canonicalEntityId }.toSet())
        assertEquals(0, mission().entries.count { it.canonicalEntityId !in setOf("ZuhV5V", "rVnyq7") })
    }

    @Test
    fun reviewUnitIdsAreRecomputedThroughCommittedContract() {
        assertTrue(mission().entries.all {
            it.reviewUnitId == HimZeroCandidateRecoveryHumanReviewContractV1.reviewUnitId(
                it.stableEntryId,
                it.canonicalEntityId,
            )
        })
    }

    @Test
    fun reviewUnitIdsHaveTheFrozenCanonicalOrder() {
        assertEquals(
            listOf(
                "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e",
                "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7",
                "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70",
                "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50",
            ),
            mission().entries.map { it.reviewUnitId },
        )
        assertEquals(
            mission().entries,
            mission().entries.sortedWith(compareBy({ it.reviewUnitId }, { it.stableEntryId }, { it.canonicalEntityId })),
        )
    }

    @Test
    fun selectionDigestIsFrozenAndValid() {
        val mission = mission()
        assertEquals(
            "e5b639be1e04b004a8de2594bed4002c854727d15eb66420a42c4293f0f3dbcb",
            mission.selectionDigest,
        )
        assertEquals(mission.selectionDigest, HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.selectionDigest(mission))
    }

    @Test
    fun independentSelectionDigestRecalculationMatches() {
        assertEquals(
            mission().selectionDigest,
            independentSelectionDigest(mission()),
        )
    }

    @Test
    fun everyBoundMissionPropertyChangesTheSelectionDigest() {
        val mission = mission()
        val changed = listOf(
            mission.copy(contractId = "OTHER"),
            mission.copy(version = "2"),
            mission.copy(missionId = "other-mission"),
            mission.copy(scopeId = "other-scope"),
            mission.copy(corpusFileSha256 = "a".repeat(64)),
            mission.copy(corpusLogicalDigest = "b".repeat(64)),
            mission.copy(corpusBindingDigest = "c".repeat(64)),
            mission.copy(reviewRound = 2),
            mission.copy(maxNewDecisionRecords = 3),
            mission.copy(expectedSelectedGroups = 3),
            mission.copy(expectedSelectedCorpusEntries = 3),
            mission.copy(expectedSelectedReviewUnits = 3),
            mission.copy(expectedDistinctCanonicalTargets = 3),
            mission.copy(expectedMultiTargetEntries = 1),
        )
        assertTrue(changed.all { HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.selectionDigest(it) != mission.selectionDigest })
    }

    @Test
    fun manipulatedSelectionDigestFailsClosed() {
        val result = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.validate(
            mission().copy(selectionDigest = "a".repeat(64)),
        )
        assertInvalid(result, HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.SELECTION_DIGEST_MISMATCH)
    }

    @Test
    fun manipulatedStableEntryIdFailsClosed() {
        val entry = mission().entries.first().copy(stableEntryId = "a".repeat(64))
        assertInvalid(
            validateWithEntries(listOf(entry) + mission().entries.drop(1)),
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_REVIEW_UNIT_ID,
        )
    }

    @Test
    fun manipulatedCanonicalIdFailsClosed() {
        val entry = mission().entries.first().copy(canonicalEntityId = "A1b2C3")
        assertInvalid(
            validateWithEntries(listOf(entry) + mission().entries.drop(1)),
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_REVIEW_UNIT_ID,
        )
    }

    @Test
    fun manipulatedReviewUnitIdFailsClosed() {
        val entry = mission().entries.first().copy(reviewUnitId = "a".repeat(64))
        assertInvalid(
            validateWithEntries(listOf(entry) + mission().entries.drop(1)),
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_REVIEW_UNIT_ID,
        )
    }

    @Test
    fun duplicateStableEntryIdFailsClosed() {
        val entries = mission().entries.toMutableList()
        entries[1] = entries[1].copy(stableEntryId = entries[0].stableEntryId)
        assertInvalid(validateWithEntries(entries), HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.DUPLICATE_STABLE_ENTRY_ID)
    }

    @Test
    fun duplicateReviewUnitIdFailsClosed() {
        val entries = mission().entries.toMutableList()
        entries[1] = entries[1].copy(reviewUnitId = entries[0].reviewUnitId)
        assertInvalid(validateWithEntries(entries), HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.DUPLICATE_REVIEW_UNIT_ID)
    }

    @Test
    fun fifthEntryFailsClosed() {
        assertInvalid(
            validateWithEntries(mission().entries + mission().entries.first()),
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.UNEXPECTED_SELECTION,
        )
    }

    @Test
    fun missingEntryFailsClosed() {
        assertInvalid(
            validateWithEntries(mission().entries.dropLast(1)),
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.UNEXPECTED_SELECTION,
        )
    }

    @Test
    fun exchangedAuthorizedUnitFailsClosed() {
        val entries = mission().entries.toMutableList()
        entries[0] = entries[0].copy(reviewUnitId = entries[1].reviewUnitId)
        assertInvalid(validateWithEntries(entries), HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.DUPLICATE_REVIEW_UNIT_ID)
    }

    @Test
    fun changedDisplayContextFailsClosed() {
        val entry = mission().entries.first().copy(groupDisplayValue = "Changed")
        assertInvalid(
            validateWithEntries(listOf(entry) + mission().entries.drop(1)),
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.UNEXPECTED_SELECTION,
        )
    }

    @Test
    fun missionFieldModelContainsNoDecisionOrReviewerState() {
        val fields = HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()
        assertFalse(fields.any { it in setOf("decision", "decisionId", "decisionRecords") })
        assertFalse(fields.any { it in setOf("reviewer", "reviewerRef") })
        assertFalse(fields.any { it == "timestamp" })
        assertFalse(fields.any { it == "approval" })
        assertFalse(fields.any { it == "publication" })
        assertFalse(fields.any { it == "eligibility" })
    }

    @Test
    fun missionFieldModelIsExactlyFrozen() {
        assertEquals(
            setOf(
                "contractId", "version", "missionId", "scopeId", "priority", "corpusFileSha256",
                "corpusLogicalDigest", "corpusBindingDigest", "reviewRound", "maxNewDecisionRecords",
                "expectedSelectedGroups", "expectedSelectedCorpusEntries", "expectedSelectedReviewUnits",
                "expectedDistinctCanonicalTargets", "expectedMultiTargetEntries", "entries", "selectionDigest",
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1::class.java.declaredFields
                .filterNot { Modifier.isStatic(it.modifiers) }
                .map { it.name }
                .toSet(),
        )
    }

    @Test
    fun validInputBindingDigestCreatesTheCommittedRuntimeScope() {
        val scope = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.runtimeScope("a".repeat(64))
        assertEquals("p1-artischocken-herzen-brie-double-creme-v1", scope.scopeId)
        assertEquals(1, scope.reviewRound)
        assertEquals(mission().entries.map { it.reviewUnitId }.sorted(), scope.authorizedReviewUnitIds)
        assertEquals(4, scope.authorizedReviewUnitIds.size)
        assertEquals(4, scope.maxNewDecisionRecords)
    }

    @Test
    fun runtimeScopeDigestUsesCommittedRuntimeFunction() {
        val scope = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.runtimeScope("a".repeat(64))
        assertEquals(HimZeroCandidateRecoveryHumanReviewRuntimeV1.scopeDigest(scope), scope.scopeDigest)
        assertValidScope(scope, "a".repeat(64))
    }

    @Test
    fun blankInputBindingDigestFailsClosed() {
        val result = runCatching { HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.runtimeScope("") }.exceptionOrNull()
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_RUNTIME_INPUT_BINDING_DIGEST.name,
            result?.message,
        )
    }

    @Test
    fun malformedInputBindingDigestFailsClosed() {
        val result = runCatching { HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.runtimeScope("not-a-digest") }.exceptionOrNull()
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.INVALID_RUNTIME_INPUT_BINDING_DIGEST.name,
            result?.message,
        )
    }

    @Test
    fun manipulatedRuntimeScopeFailsClosed() {
        val binding = "a".repeat(64)
        val broken = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.runtimeScope(binding)
            .copy(maxNewDecisionRecords = 3)
        assertInvalidScope(
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.validateRuntimeScope(broken, binding),
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1.RUNTIME_SCOPE_MISMATCH,
        )
    }

    @Test
    fun missionHasNoRuntimeOrPersistencePathFields() {
        val fields = HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1::class.java.declaredFields
        assertFalse(fields.any { it.type.name == "java.io.File" })
        assertFalse(fields.any { it.name.contains("path", ignoreCase = true) })
        assertFalse(fields.any { it.name.contains("batch", ignoreCase = true) })
    }

    private fun mission() = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION

    private fun validateWithEntries(entries: List<HimZeroCandidateRecoveryHumanReviewP1PilotMissionEntryV1>) =
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.validate(mission().copy(entries = entries))

    private fun assertValid(mission: HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1) {
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1.Valid>(
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.validate(mission),
        )
    }

    private fun assertInvalid(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1,
        expected: HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1,
    ) {
        val invalid = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1.Invalid>(result)
        assertEquals(expected, invalid.reason)
    }

    private fun assertValidScope(
        scope: de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewScopeV1,
        inputBindingDigest: String,
    ) {
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1.Valid>(
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.validateRuntimeScope(scope, inputBindingDigest),
        )
    }

    private fun assertInvalidScope(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotMissionValidationResultV1,
        expected: HimZeroCandidateRecoveryHumanReviewP1PilotMissionFailureReasonV1,
    ) = assertInvalid(result, expected)

    private fun independentSelectionDigest(mission: HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1): String =
        sha256(
            buildString {
                appendLine("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_MISSION_SELECTION_V1")
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

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
