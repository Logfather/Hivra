package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPrimaryBucketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceScopeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewGroupMembershipV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewPriorityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewSelectionReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class RunHimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1Test {
    @Test fun contractIdAndVersionAreFrozen() {
        assertEquals("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_REVIEW_PACKET_CONTRACT_V1", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.CONTRACT_ID)
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.VERSION)
    }

    @Test fun packetStateIsContextOnly() {
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1.REVIEW_CONTEXT_ONLY, packet().packetState)
    }

    @Test fun missionContractMissionAndSelectionAreBound() {
        val packet = packet()
        val mission = mission()
        assertEquals(mission.contractId, packet.missionBinding.missionContractId)
        assertEquals(mission.missionId, packet.missionBinding.missionId)
        assertEquals(mission.selectionDigest, packet.missionBinding.selectionDigest)
        assertValid(packet)
    }

    @Test fun exactlyFourMissionItemsAreAccepted() { assertValid(packet()) }

    @Test fun additionalItemIsRejected() {
        val extra = fixtureItem(5, mission().entries[0])
        assertInvalid(packet(packet().items + extra), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_PACKET_ITEM_COUNT)
    }

    @Test fun missingItemIsRejected() { assertInvalid(packet(packet().items.drop(1)), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_PACKET_ITEM_COUNT) }

    @Test fun exchangedUnitIsRejected() {
        val items = packet().items.toMutableList().also { list -> list[0] = list[0].copy(reviewUnitId = list[1].reviewUnitId) }
        assertInvalid(packet(items), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.DUPLICATE_REVIEW_UNIT_ID)
    }

    @Test fun itemOrderIsFrozenByReviewUnitStableEntryAndCanonical() {
        val reversed = packet(packet().items.reversed())
        assertEquals(packet().items, reversed.items)
    }

    @Test fun duplicateStableEntryIdIsRejected() {
        val items = packet().items.toMutableList().also { list -> list[1] = list[1].copy(stableEntryId = list[0].stableEntryId) }
        assertInvalid(packet(items), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.DUPLICATE_STABLE_ENTRY_ID)
    }

    @Test fun duplicateReviewUnitIdIsRejected() {
        val items = packet().items.toMutableList().also { list -> list[1] = list[1].copy(reviewUnitId = list[0].reviewUnitId) }
        assertInvalid(packet(items), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.DUPLICATE_REVIEW_UNIT_ID)
    }

    @Test fun reviewUnitIdIsRecomputedFromStableAndCanonicalIds() {
        val item = packet().items[0].copy(canonicalEntityId = "rVnyq7")
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.REVIEW_UNIT_BINDING_MISMATCH)
    }

    @Test fun sourceAndRecordKindMustAgree() {
        val item = packet().items[0].copy(sourceContext = packet().items[0].sourceContext.copy(recordKind = HimEvidenceRecordKind.AGRIBALYSE_RECORD))
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_SOURCE_CONTEXT)
    }

    @Test fun evidenceReferenceIsRequired() {
        val item = packet().items[0].copy(sourceContext = packet().items[0].sourceContext.copy(evidenceReference = ""))
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_SOURCE_CONTEXT)
    }

    @Test fun primaryValuesAreSortedAndDistinct() {
        val item = packet().items[0].copy(sourceContext = packet().items[0].sourceContext.copy(primaryValues = listOf("z", "a")))
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_SOURCE_CONTEXT)
    }

    @Test fun findingOccurrenceIdsAreSortedAndDistinct() {
        val context = packet().items[0].sourceContext.copy(findingOccurrenceIds = listOf("f".repeat(64), "a".repeat(64)))
        val item = packet().items[0].copy(sourceContext = context)
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_SOURCE_CONTEXT)
    }

    @Test fun originalFieldsAreSortedByFieldName() {
        val fields = listOf(field("z", "z"), field("a", "a"))
        val item = packet().items[0].copy(originalFields = fields)
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_ORIGINAL_FIELD)
    }

    @Test fun originalValueDigestIsRecomputed() {
        val item = packet().items[0].copy(originalFields = listOf(field("name", "changed", originalDigest = sha256("original"))))
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.ORIGINAL_VALUE_DIGEST_MISMATCH)
    }

    @Test fun fullDisplayRequiresExactOriginalValue() {
        val item = packet().items[0].copy(originalFields = listOf(field("name", "original", display = "different")))
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_DISPLAY_STATE)
    }

    @Test fun truncatedDisplayRetainsFullMachineValueAndVisibleMarker() {
        val original = "x".repeat(300)
        val truncated = original.take(239) + "…"
        val item = packet().items[0].copy(
            originalFields = listOf(field("name", original, display = truncated, state = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.TRUNCATED_FOR_HUMAN_PROJECTION)),
            contextLimitations = limitations(includeTruncation = true),
        )
        assertValid(packet(packet().items.toMutableList().also { it[0] = item }))
        assertEquals(original, item.originalFields.single().originalValue)
    }

    @Test fun truncationWithoutLimitationIsRejected() {
        val original = "x".repeat(300)
        val item = packet().items[0].copy(originalFields = listOf(field("name", original, display = "short…", state = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.TRUNCATED_FOR_HUMAN_PROJECTION)))
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.MISSING_TRUNCATION_LIMITATION)
    }

    @Test fun silentTruncationIsRejected() {
        val original = "x".repeat(300)
        val item = packet().items[0].copy(originalFields = listOf(field("name", original, display = "short", state = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.TRUNCATED_FOR_HUMAN_PROJECTION)), contextLimitations = limitations(true))
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.SILENT_TRUNCATION)
    }

    @Test fun emptySourceValueIsExplicit() {
        val item = packet().items[0].copy(originalFields = listOf(field("optional", "", display = "", state = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.EMPTY_SOURCE_VALUE)))
        assertValid(packet(packet().items.toMutableList().also { it[0] = item }))
    }

    @Test fun emptyStateWithNonEmptyValueIsRejected() {
        val item = packet().items[0].copy(originalFields = listOf(field("optional", "value", display = "", state = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.EMPTY_SOURCE_VALUE)))
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_DISPLAY_STATE)
    }

    @Test fun targetIdMustMatchMissionItem() {
        val item = packet().items[0].copy(canonicalEntityId = "ABC123")
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.REVIEW_UNIT_BINDING_MISMATCH)
    }

    @Test fun targetDisplayLabelIsContextOnly() {
        val item = packet().items[0].copy(targetContext = target("ZuhV5V", "A different technical label"))
        assertValid(packet(packet().items.toMutableList().also { it[0] = item }))
    }

    @Test fun identityTermsAreSorted() {
        val target = target("ZuhV5V", "Artischocken", identity = listOf("z", "a"))
        val item = packet().items[0].copy(targetContext = target, contextLimitations = targetOnlyLimitations())
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_TARGET_CONTEXT)
    }

    @Test fun aliasTermsAreSorted() {
        val target = target("ZuhV5V", "Artischocken", aliases = listOf("z", "a"))
        val item = packet().items[0].copy(targetContext = target, contextLimitations = targetOnlyLimitations())
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_TARGET_CONTEXT)
    }

    @Test fun missingIdentityAndAliasTermsProduceLimitations() {
        val item = packet().items[0]
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.IDENTITY_TERMS_ABSENT in item.contextLimitations)
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.ALIAS_TERMS_ABSENT in item.contextLimitations)
        assertValid(packet())
    }

    @Test fun targetContextDigestIsDeterministic() {
        val target = packet().items[0].targetContext
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(target), target.targetContextDigest)
        assertEquals(target.targetContextDigest, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(target))
    }

    @Test fun materializedCoverageRequiresSourceLimitations() { assertValid(packet()) }

    @Test fun boundProjectionRequiresLogicalProjectionBinding() {
        val item = packet().items[0].copy(evidenceScope = packet().items[0].evidenceScope.copy(coverage = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.BOUND_SOURCE_PROJECTION_INCLUDED, evidenceKind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, artifactLogicalDigest = "a".repeat(64)), contextLimitations = targetOnlyLimitations())
        assertValid(packet(packet().items.toMutableList().also { it[0] = item }))
    }

    @Test fun boundProjectionWithoutLogicalBindingIsRejected() {
        val item = packet().items[0].copy(evidenceScope = packet().items[0].evidenceScope.copy(coverage = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.BOUND_SOURCE_PROJECTION_INCLUDED, evidenceKind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION), contextLimitations = targetOnlyLimitations())
        assertInvalid(packet(packet().items.toMutableList().also { it[0] = item }), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.MISSING_SOURCE_PROJECTION_BINDING)
    }

    @Test fun itemLogicalDigestIsDeterministic() { assertEquals(packet().items, packet().items) }

    @Test fun changingOriginalFieldChangesItemDigest() {
        val original = packet().items[0]
        val changed = original.copy(originalFields = listOf(field("name", "changed")))
        assertNotEquals(original.itemLogicalDigest, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.itemLogicalDigest(changed.copy(itemBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.itemBindingDigest(changed))))
    }

    @Test fun changingTargetContextChangesItemDigest() {
        val original = packet().items[0]
        val changedTarget = target("ZuhV5V", "Changed")
        val changed = original.copy(targetContext = changedTarget)
        assertNotEquals(original.itemLogicalDigest, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.itemLogicalDigest(changed.copy(itemBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.itemBindingDigest(changed))))
    }

    @Test fun packetBindingAndLogicalDigestsAreDeterministic() {
        assertEquals(packet().packetBindingDigest, packet().packetBindingDigest)
        assertEquals(packet().packetLogicalDigest, packet().packetLogicalDigest)
        assertEquals(packet().packetBindingDigest, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.packetBindingDigest(packet()))
        assertEquals(packet().packetLogicalDigest, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.packetLogicalDigest(packet()))
    }

    @Test fun changingItemChangesPacketDigest() {
        val changed = packet().items.toMutableList().also { it[0] = it[0].copy(groupDisplayValue = "changed") }
        val rebuilt = packet(changed)
        assertNotEquals(packet().packetLogicalDigest, rebuilt.packetLogicalDigest)
    }

    @Test fun countersAreDerivedFromItems() {
        val result = packet()
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.deriveCounters(result.items), result.counters)
        assertEquals(4, result.counters.packetItems)
        assertEquals(4, result.counters.uniqueReviewUnits)
        assertEquals(2, result.counters.distinctCanonicalTargets)
        assertEquals(4, result.counters.materializedCorpusOnlyItems)
        assertEquals(4, result.counters.fullDisplayFields)
    }

    @Test fun manipulatedCountersAreRejected() {
        val broken = packet().copy(counters = packet().counters.copy(packetItems = 0), packetLogicalDigest = "")
        assertInvalid(broken, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_COUNTERS)
    }

    @Test fun noDecisionReviewerReasonConfidenceTimestampOrEffectFieldsExist() {
        val forbidden = setOf("reviewer", "decision", "reason", "confidence", "timestamp", "gold", "training", "approval", "publication", "mutation")
        val fields = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1::class.java.declaredFields.map { it.name.lowercase() }
        assertTrue(fields.none { field -> forbidden.any { word -> word in field } })
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.HUMAN_PROJECTION_HEADER.contains("NO DECISION RECORDED"))
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.HUMAN_PROJECTION_SECTIONS.none { section -> forbidden.any { it in section.lowercase() } })
    }

    @Test fun humanProjectionSectionsAreExactlyFrozen() {
        assertEquals(listOf("REVIEW UNIT", "SOURCE CONTEXT", "MATERIALIZED ORIGINAL FIELDS", "AUDIT TARGET UNDER REVIEW", "TARGET CONTEXT", "EVIDENCE SCOPE", "CONTEXT LIMITATIONS", "BINDINGS"), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.HUMAN_PROJECTION_SECTIONS)
    }

    @Test fun packetHasNoFileIoOrRetrievalSurface() {
        val names = (HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1::class.java.declaredMethods.map { it.name } + HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1::class.java.declaredFields.map { it.name }).joinToString(" ").lowercase()
        assertFalse("sqlite" in names || "search" in names || "fetch" in names || "scan" in names || "socket" in names)
    }

    @Test fun validationResultIsTyped() { assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Valid>(validate(packet())) }

    @Test fun invalidContractIdFailsClosed() { assertInvalid(packet().copy(contractId = "other"), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_CONTRACT_ID) }

    @Test fun invalidImplementationHeadFailsClosed() { assertInvalid(packet().copy(packetImplementationHead = "z".repeat(40)), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_IMPLEMENTATION_HEAD) }

    private fun validate(packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.validate(packet, mission(), inputBinding())
    private fun assertValid(packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1) { assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Valid>(validate(packet)) }
    private fun assertInvalid(packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1, reason: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1) { assertEquals(reason, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Invalid>(validate(packet)).reason) }

    private fun packet(items: List<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1> = baseItems()) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.create(
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.missionBinding(mission()), inputBinding(), inputBinding().corpusFileBinding, inputBinding().corpusLogicalDigest, inputBinding().corpusBindingDigest, "a".repeat(40), items,
    )

    private fun mission() = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION

    private fun baseItems() = mission().entries.mapIndexed { index, entry -> fixtureItem(index + 1, entry) }

    private fun fixtureItem(index: Int, entry: de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionEntryV1) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1(
        entry.stableEntryId, entry.reviewUnitId, entry.canonicalEntityId, entry.groupOrdinal, entry.groupDisplayValue,
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1(
            HimGroundTruthSource.OPEN_FOOD_FACTS, HimEvidenceRecordKind.OFF_PRODUCT, HimEvidenceRecordReference.offProduct(index.toLong(), "code$index").value, listOf(sha256("finding$index")), listOf(HimZeroCandidateRecoveryReviewSelectionReasonV1.RECURRING_PRIMARY_VALUE_GROUP), listOf("primary $index"), HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH,
            listOf(HimZeroCandidateRecoveryReviewGroupMembershipV1(entry.groupDisplayValue, HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET, 2, 2, listOf(entry.canonicalEntityId))),
        ),
        listOf(field("name", "source value $index")),
        target(entry.canonicalEntityId, entry.expectedDisplayLabel),
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceScopeV1(HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, "data/evidence/corpus.json", "b".repeat(64), null, "record:$index", listOf("name"), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY),
        limitations(), "", "",
    )

    private fun field(name: String, value: String, originalDigest: String = sha256(value), display: String? = value, state: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1 = if (value.isEmpty()) HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.EMPTY_SOURCE_VALUE else HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1(name, value, originalDigest, display, state, value.length)

    private fun target(id: String, label: String, identity: List<String> = emptyList(), aliases: List<String> = emptyList()): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1 {
        val base = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1(id, label, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, "catalog:$id", "authority:$id", identity, aliases, "")
        return base.copy(targetContextDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(base))
    }

    private fun limitations(includeTruncation: Boolean = false) = buildList {
        add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.IDENTITY_TERMS_ABSENT)
        add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.ALIAS_TERMS_ABSENT)
        add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FULL_SOURCE_RECORD_NOT_INCLUDED)
        add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.SOURCE_PROJECTION_NOT_INCLUDED)
        if (includeTruncation) add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FIELD_TRUNCATED_IN_HUMAN_PROJECTION)
    }.sortedBy { it.ordinal }

    private fun targetOnlyLimitations() = listOf(
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.IDENTITY_TERMS_ABSENT,
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.ALIAS_TERMS_ABSENT,
    )

    private fun inputBinding(): HimZeroCandidateRecoveryHumanReviewInputBindingV1 {
        val cause = HimZeroCandidateCauseAnalysisFileBindingV1("fixture/cause.json", 1, "c".repeat(64), "d".repeat(64))
        val corpusInputBase = HimZeroCandidateRecoveryReviewCorpusInputBindingV1(cause, HimZeroCandidateRecoveryReviewCorpusFileBindingV1("fixture/catalog.json", 1, "1".repeat(64), "2".repeat(64)), HimZeroCandidateRecoveryReviewCorpusFileBindingV1("fixture/authority.json", 1, "3".repeat(64), "4".repeat(64)), "d".repeat(64), "a".repeat(40), "")
        val corpusInput = corpusInputBase.copy(bindingDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.bindingDigest(corpusInputBase))
        val base = HimZeroCandidateRecoveryHumanReviewInputBindingV1(HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/review-corpus.json", 1, "5".repeat(64), "6".repeat(64)), "6".repeat(64), "7".repeat(64), "8".repeat(64), corpusInput, HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/registry.json", 1, "9".repeat(64), "a".repeat(64)), HimZeroCandidateRecoveryHumanReviewContractV1.VERSION, HimZeroCandidateRecoveryHumanReviewContractV1.VERSION, "a".repeat(40), "")
        return base.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(base))
    }

    private fun sha256(value: String) = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(value)
}
