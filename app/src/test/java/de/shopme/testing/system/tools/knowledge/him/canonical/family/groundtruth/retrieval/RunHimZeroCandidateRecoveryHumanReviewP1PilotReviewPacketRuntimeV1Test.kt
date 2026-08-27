package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticLanguageV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticLexicalFeaturesV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPrimaryBucketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionEntryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusEntryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusReportV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewGroupMembershipV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewPriorityCounterV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewPriorityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewSelectionReasonBreakdownV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewSelectionReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewRecordKindBreakdownV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewSourceBreakdownV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewAssociationStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCanonicalTargetV1
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1Test {
    @Test
    fun contractAndVersionAreFrozen() {
        assertEquals("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_REVIEW_PACKET_RUNTIME_V1", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1.CONTRACT_ID)
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1.VERSION)
    }

    @Test
    fun requestAndResultModelsHaveOnlyTheRequiredFields() {
        assertEquals(
            listOf("enabled", "corpus", "inputBinding", "catalog", "registry", "authority", "packetImplementationHead", "packetOutputRoot"),
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1::class.java.declaredFields.filter { !it.isSynthetic && !it.name.startsWith("$") }.map { it.name },
        )
        assertEquals(
            listOf("runtimeContractId", "runtimeVersion", "missionId", "packet", "persistenceStatus", "jsonPath", "markdownPath", "jsonByteSize", "markdownByteSize", "jsonSha256", "markdownSha256", "packetBindingDigest", "packetLogicalDigest", "counters"),
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed::class.java.declaredFields.filter { !it.isSynthetic && !it.name.startsWith("$") }.map { it.name },
        )
    }

    @Test
    fun disabledReturnsBeforeAnyValidationOrOutputAccess() {
        val result = execute(enabled = false, packetOutputRoot = Files.createTempDirectory("missing-parent").resolve("does-not-exist").toFile())
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Disabled>(result)
    }

    @Test
    fun syntheticMissionIsResolvedAndPersistedWithoutDecisionSemantics() {
        val root = Files.createTempDirectory("p1-packet-runtime").toFile()
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed>(execute(packetOutputRoot = root))
        assertEquals(4, result.packet.items.size)
        assertEquals(4, result.counters.missionEntriesResolved)
        assertEquals(4, result.counters.packetItemsCreated)
        assertEquals(2, result.counters.distinctCanonicalTargets)
        assertEquals(4, result.counters.materializedCorpusOnlyItems)
        assertEquals(0, result.packet.counters.sourceProjectionIncludedItems)
        assertTrue(result.jsonPath.startsWith("p1-artischocken-herzen-brie-double-creme-v1/"))
        assertFalse(result.jsonPath.startsWith("/"))
        assertFalse(result.markdownPath.startsWith("/"))
        assertTrue(root.resolve(result.jsonPath).isFile)
        assertTrue(root.resolve(result.markdownPath).isFile)
        assertTrue(result.packet.items.all { it.evidenceScope.coverage == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY })
        assertTrue(result.packet.items.all { HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FULL_SOURCE_RECORD_NOT_INCLUDED in it.contextLimitations })
        assertTrue(result.packet.items.all { HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.SOURCE_PROJECTION_NOT_INCLUDED in it.contextLimitations })
    }

    @Test
    fun secondRunIsIdenticalAndUsesCommittedPersistenceIdempotently() {
        val root = Files.createTempDirectory("p1-packet-idempotence").toFile()
        val first = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed>(execute(packetOutputRoot = root))
        val second = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed>(execute(packetOutputRoot = root))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1.CREATED, first.persistenceStatus)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, second.persistenceStatus)
        assertEquals(first.packet, second.packet)
        assertEquals(first.jsonSha256, second.jsonSha256)
        assertEquals(first.markdownSha256, second.markdownSha256)
        assertEquals(first.packetLogicalDigest, second.packetLogicalDigest)
        assertEquals(first.packetBindingDigest, second.packetBindingDigest)
    }

    @Test
    fun invalidHeadAndFoundationFailClosedWithTypedReasons() {
        val invalidHead = execute(packetImplementationHead = "not-a-head")
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_IMPLEMENTATION_HEAD, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(invalidHead).reason)
        val broken = fixture().copy(catalog = fixture().catalog.copy(records = fixture().catalog.records.drop(1)))
        val result = execute(broken)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_FOUNDATION, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(result).reason)
    }

    @Test
    fun inputBindingMismatchFailsClosed() {
        val value = fixture()
        val broken = value.copy(inputBinding = value.inputBinding.copy(corpusBindingDigest = "not-the-corpus-binding"))
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(broken))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_INPUT_BINDING, result.reason)
    }

    @Test
    fun corpusOrderDoesNotChangePacket() {
        val first = fixture()
        val reversedCorpus = first.corpus.copy(entries = first.corpus.entries.reversed())
        val rootA = Files.createTempDirectory("p1-order-a").toFile()
        val rootB = Files.createTempDirectory("p1-order-b").toFile()
        val a = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed>(execute(first.copy(packetOutputRoot = rootA)))
        val b = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed>(execute(first.copy(corpus = reversedCorpus, packetOutputRoot = rootB)))
        assertEquals(a.packet, b.packet)
        assertEquals(a.packetLogicalDigest, b.packetLogicalDigest)
    }

    @Test
    fun missingTargetAndWrongTargetFailClosed() {
        val missing = fixture().copy(registry = fixture().registry.copy(entries = fixture().registry.entries.drop(1)))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_FOUNDATION, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(missing)).reason)
        val base = fixture()
        val changedUnsigned = base.corpus.copy(
            entries = base.corpus.entries.mapIndexed { index, entry ->
                if (index == 0) entry.copy(auditLinkedCanonicalTargets = listOf(HimZeroCandidateRecoveryReviewCanonicalTargetV1("rVnyq7", "Crème double", emptyList(), emptyList()))) else entry
            },
            logicalDigest = "",
        )
        val changedCorpus = changedUnsigned.copy(logicalDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.logicalDigest(changedUnsigned))
        val changedInputBase = base.inputBinding.copy(
            corpusFileBinding = base.inputBinding.corpusFileBinding.copy(logicalDigest = changedCorpus.logicalDigest),
            corpusLogicalDigest = changedCorpus.logicalDigest,
        )
        val wrongTarget = base.copy(
            corpus = changedCorpus,
            inputBinding = changedInputBase.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(changedInputBase)),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.UNEXPECTED_MISSION_TARGET, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(wrongTarget)).reason)
    }

    @Test
    fun persistenceConflictIsNotOverwritten() {
        val root = Files.createTempDirectory("p1-packet-conflict").toFile()
        val first = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed>(execute(packetOutputRoot = root))
        root.resolve(first.jsonPath).writeText("conflict")
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(packetOutputRoot = root))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.PERSISTENCE_FAILED, result.reason)
    }

    @Test fun missionEntriesProduceExactlyFourItems() = assertEquals(4, completed().packet.items.size)
    @Test fun nonSelectedCorpusEntriesCannotIncreasePacketItemCount() = assertEquals(4, completed().counters.packetItemsCreated)
    @Test fun corpusOrderIsCanonicalizedBeforeMissionResolution() {
        val value = fixture()
        val result = completed(rebound(value, value.corpus.entries.shuffled(java.util.Random(7))))
        assertEquals(listOf("a98f67c7aa8af729e27d402585df4c78d2a1a9c3f85e636dd7d257cba1810ebd", "ec4d7ccf39c1b9dbe184a0af90190cbc5d6a9e96ce3e3af121f5f59258eb5e13", "6db7a77b0a3ff2471b8001b1644bc7a957efa52369df722899298c87d286ed71", "f4957e490b1714a1e48ca5d43ae762564603ac712842b3c2714b9f36bf2c41df"), result.packet.items.map { it.stableEntryId })
    }
    @Test fun missingMissionEntryFailsClosed() {
        val value = fixture()
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(rebound(value, value.corpus.entries.drop(1))))
    }
    @Test fun duplicateMissionEntryFailsClosed() {
        val value = fixture()
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(rebound(value, value.corpus.entries + value.corpus.entries.first())))
    }
    @Test fun multipleTargetsFailClosed() {
        val value = fixture()
        val target = HimZeroCandidateRecoveryReviewCanonicalTargetV1("rVnyq7", "Crème double", emptyList(), emptyList())
        val entries = value.corpus.entries.mapIndexed { index, entry -> if (index == 0) entry.copy(auditLinkedCanonicalTargets = entry.auditLinkedCanonicalTargets + target) else entry }
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.MULTI_TARGET_MISSION_ENTRY, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(rebound(value, entries))).reason)
    }
    @Test fun wrongPriorityFailsClosed() {
        val value = fixture()
        val entries = value.corpus.entries.mapIndexed { index, entry -> if (index == 0) entry.copy(recurringGroupMemberships = entry.recurringGroupMemberships.map { it.copy(priorityClass = HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_LOCALIZED_SINGLE_AUDIT_TARGET) }) else entry }
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(rebound(value, entries)))
    }
    @Test fun reviewUnitBindingIsDerivedFromStableEntryAndTarget() {
        val result = completed()
        assertTrue(result.packet.items.all { it.reviewUnitId == HimZeroCandidateRecoveryHumanReviewContractV1.reviewUnitId(it.stableEntryId, it.canonicalEntityId) })
    }
    @Test fun sourceContextEvidenceReferencesRemainUnchanged() {
        val value = fixture()
        val result = completed(value)
        assertEquals(value.corpus.entries.map { it.evidenceReference }.toSet(), result.packet.items.map { it.sourceContext.evidenceReference }.toSet())
    }
    @Test fun sourceContextPrimaryValuesRemainUnchanged() {
        val value = fixture()
        val result = completed(value)
        assertEquals(value.corpus.entries.flatMap { it.primaryValues }.sorted(), result.packet.items.flatMap { it.sourceContext.primaryValues }.sorted())
    }
    @Test fun originalFieldsAreSortedByFieldName() {
        val result = completed()
        assertTrue(result.packet.items.all { it.originalFields == it.originalFields.sortedBy { field -> field.fieldName } })
    }
    @Test fun originalValueDigestUsesCompleteUtf8Value() {
        val result = completed()
        assertTrue(result.packet.items.flatMap { it.originalFields }.all { it.originalValueSha256 == HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(it.originalValue) })
    }
    @Test fun shortOriginalValueUsesFullDisplay() {
        val result = completed()
        assertTrue(result.packet.items.flatMap { it.originalFields }.all { it.displayState == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL })
    }
    @Test fun longOriginalValueUsesTruncatedDisplay() {
        val value = fixture()
        val long = "x".repeat(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.MAX_HUMAN_DISPLAY_LENGTH + 1)
        val entries = value.corpus.entries.mapIndexed { index, entry -> if (index == 0) entry.replaceField(long) else entry }
        val result = completed(rebound(value, entries))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.TRUNCATED_FOR_HUMAN_PROJECTION, result.packet.items.single { it.stableEntryId == value.corpus.entries.first().stableEntryId }.originalFields.single().displayState)
    }
    @Test fun completeLongOriginalValueRemainsInPacketModel() {
        val value = fixture()
        val long = "y".repeat(300)
        val result = completed(rebound(value, value.corpus.entries.mapIndexed { index, entry -> if (index == 0) entry.replaceField(long) else entry }))
        assertEquals(long, result.packet.items.single { it.stableEntryId == value.corpus.entries.first().stableEntryId }.originalFields.single().originalValue)
    }
    @Test fun unicodeSurrogateBoundaryIsNotSplit() {
        val value = fixture()
        val long = "🙂".repeat(200)
        val result = completed(rebound(value, value.corpus.entries.mapIndexed { index, entry -> if (index == 0) entry.replaceField(long) else entry }))
        val display = result.packet.items.single { it.stableEntryId == value.corpus.entries.first().stableEntryId }.originalFields.single().humanDisplayValue!!
        assertTrue(display.endsWith("…"))
        val body = display.dropLast(1)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.MAX_HUMAN_DISPLAY_LENGTH - 2, body.length)
        assertEquals(body.length / 2, body.codePointCount(0, body.length))
        assertTrue(Character.isHighSurrogate(body.first()))
        assertTrue(Character.isLowSurrogate(body.last()))
    }
    @Test fun emptyOriginalValueUsesEmptyDisplayState() {
        val value = fixture()
        val result = completed(rebound(value, value.corpus.entries.mapIndexed { index, entry -> if (index == 0) entry.replaceField("") else entry }))
        val field = result.packet.items.single { it.stableEntryId == value.corpus.entries.first().stableEntryId }.originalFields.single()
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.EMPTY_SOURCE_VALUE, field.displayState)
    }
    @Test fun duplicateOriginalFieldNamesAreRejectedByRuntime() {
        val value = fixture()
        val field = value.corpus.entries.first().originalFields.first()
        val duplicate = value.corpus.entries.mapIndexed { index, entry -> if (index == 0) entry.copy(originalFields = listOf(field, field)) else entry }
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.DUPLICATE_ORIGINAL_FIELD, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(rebound(value, duplicate))).reason)
    }
    @Test fun targetResolvesThroughRegistryAndAuthority() {
        val result = completed()
        assertTrue(result.packet.items.all { it.targetContext.registryResolution == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED && it.targetContext.authorityResolution == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED })
    }
    @Test fun unknownTargetFailsClosed() {
        val value = fixture()
        val brokenRegistry = value.registry.copy(entries = value.registry.entries.mapIndexed { index, entry -> if (index == 0) entry.copy(entityId = HimEntityId("XxXxXx")) else entry })
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_FOUNDATION, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(value.copy(registry = brokenRegistry))).reason)
    }
    @Test fun targetLabelMismatchFailsClosed() {
        val value = fixture()
        val entries = value.corpus.entries.mapIndexed { index, entry -> if (index == 0) entry.copy(auditLinkedCanonicalTargets = listOf(HimZeroCandidateRecoveryReviewCanonicalTargetV1("ZuhV5V", "Wrong", emptyList(), emptyList()))) else entry }
        val broken = rebound(value, entries)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.TARGET_LABEL_MISMATCH, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(broken)).reason)
    }
    @Test fun authorityIdentityTermsAreMaterializedWithoutSourceTerms() {
        val result = completed()
        assertTrue(result.packet.items.all { it.targetContext.identityTerms.isEmpty() })
    }
    @Test fun authorityAliasTermsAreMaterializedWithoutSourceTerms() {
        val result = completed()
        assertTrue(result.packet.items.all { it.targetContext.aliasTerms.isEmpty() })
    }
    @Test fun emptyIdentityTermsProduceLimitation() {
        assertTrue(completed().packet.items.all { HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.IDENTITY_TERMS_ABSENT in it.contextLimitations })
    }
    @Test fun emptyAliasTermsProduceLimitation() {
        assertTrue(completed().packet.items.all { HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.ALIAS_TERMS_ABSENT in it.contextLimitations })
    }
    @Test fun evidenceCoverageIsMaterializedCorpusOnly() {
        assertTrue(completed().packet.items.all { it.evidenceScope.coverage == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY })
    }
    @Test fun sourceProjectionCoverageIsNeverProduced() {
        assertTrue(completed().packet.items.none { it.evidenceScope.coverage == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.BOUND_SOURCE_PROJECTION_INCLUDED })
    }
    @Test fun fullSourceRecordLimitationIsPresent() {
        assertTrue(completed().packet.items.all { HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FULL_SOURCE_RECORD_NOT_INCLUDED in it.contextLimitations })
    }
    @Test fun sourceProjectionLimitationIsPresent() {
        assertTrue(completed().packet.items.all { HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.SOURCE_PROJECTION_NOT_INCLUDED in it.contextLimitations })
    }
    @Test fun evidenceDirectnessDoesNotClaimSufficiency() {
        assertTrue(completed().packet.items.all { it.evidenceScope.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.INDIRECT })
    }
    @Test fun evidenceKindIsOriginCorpusRecord() {
        assertTrue(completed().packet.items.all { it.evidenceScope.evidenceKind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD })
    }
    @Test fun targetContextDigestIsDeterministic() {
        val result = completed()
        assertTrue(result.packet.items.all { it.targetContext.targetContextDigest == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(it.targetContext) })
    }
    @Test fun itemBindingDigestsAreDeterministic() {
        val result = completed()
        assertTrue(result.packet.items.all { it.itemBindingDigest == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.itemBindingDigest(it) })
    }
    @Test fun itemLogicalDigestsAreDeterministic() {
        val result = completed()
        assertTrue(result.packet.items.all { it.itemLogicalDigest == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.itemLogicalDigest(it) })
    }
    @Test fun packetBindingDigestIsDeterministic() {
        val result = completed()
        assertEquals(result.packet.packetBindingDigest, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.packetBindingDigest(result.packet))
    }
    @Test fun packetLogicalDigestIsDeterministic() {
        val result = completed()
        assertEquals(result.packet.packetLogicalDigest, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.packetLogicalDigest(result.packet))
    }
    @Test fun packetHasExactlyFourItems() = assertEquals(4, completed().packet.counters.packetItems)
    @Test fun packetItemsFollowCommittedMissionReviewUnitOrder() {
        val result = completed()
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.missionBinding(HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION).reviewUnitIds.sorted(), result.packet.items.map { it.reviewUnitId })
    }
    @Test fun packetCountersAreDerivedFromItems() {
        val result = completed()
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.deriveCounters(result.packet.items), result.packet.counters)
    }
    @Test fun runtimeCountersDescribeFourResolvedUnits() = assertEquals(4, completed().counters.uniqueReviewUnits)
    @Test fun runtimeCountersDescribeTwoCanonicalTargets() = assertEquals(2, completed().counters.distinctCanonicalTargets)
    @Test fun runtimeCountersDescribeMaterializedFields() = assertEquals(4, completed().counters.originalFieldsMaterialized)
    @Test fun packetHasNoDecisionFields() {
        val result = completed()
        assertTrue(result.packet::class.java.declaredFields.none { it.name.contains("decision", true) || it.name.contains("reviewer", true) || it.name.contains("recommend", true) })
    }
    @Test fun packetHasNoReasonCodeFields() = assertTrue(completed().packet::class.java.declaredFields.none { it.name.contains("reason", true) })
    @Test fun packetHasNoReviewerFields() = assertTrue(completed().packet::class.java.declaredFields.none { it.name.contains("reviewer", true) })
    @Test fun packetHasNoRecommendationFields() = assertTrue(completed().packet::class.java.declaredFields.none { it.name.contains("recommend", true) })
    @Test fun packetStateHasNoGoldOrSupervisionSemantics() = assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1.REVIEW_CONTEXT_ONLY, completed().packet.packetState)
    @Test fun syntheticPersistenceCreatesExactlyJsonAndMarkdown() {
        val root = Files.createTempDirectory("p1-files").toFile()
        val result = completed(packetOutputRoot = root)
        assertEquals(setOf("review-packet.v1.json", "review-packet.v1.md"), root.resolve("p1-artischocken-herzen-brie-double-creme-v1").list()!!.toSet())
        assertTrue(result.jsonByteSize > 0 && result.markdownByteSize > 0)
    }
    @Test fun firstPersistenceStatusIsCreated() {
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1.CREATED, completed().persistenceStatus)
    }
    @Test fun persistenceReloadMatchesPacket() {
        val root = Files.createTempDirectory("p1-reload").toFile()
        val result = completed(packetOutputRoot = root)
        assertEquals(result.packet, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.readPacket(root.resolve(result.jsonPath)))
    }
    @Test fun secondPersistenceStatusIsAlreadyPresentIdentical() {
        val root = Files.createTempDirectory("p1-existing").toFile()
        completed(packetOutputRoot = root)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, completed(packetOutputRoot = root).persistenceStatus)
    }
    @Test fun repeatedPersistenceJsonShaIsIdentical() {
        val root = Files.createTempDirectory("p1-json-digest").toFile()
        val first = completed(packetOutputRoot = root)
        val second = completed(packetOutputRoot = root)
        assertEquals(first.jsonSha256, second.jsonSha256)
    }
    @Test fun repeatedPersistenceMarkdownShaIsIdentical() {
        val root = Files.createTempDirectory("p1-md-digest").toFile()
        val first = completed(packetOutputRoot = root)
        val second = completed(packetOutputRoot = root)
        assertEquals(first.markdownSha256, second.markdownSha256)
    }
    @Test fun repeatedPersistencePacketDigestsAreIdentical() {
        val root = Files.createTempDirectory("p1-packet-digest").toFile()
        val first = completed(packetOutputRoot = root)
        val second = completed(packetOutputRoot = root)
        assertEquals(first.packetLogicalDigest, second.packetLogicalDigest)
    }
    @Test fun partialPacketStateFailsClosed() {
        val root = Files.createTempDirectory("p1-partial").toFile()
        val dir = root.resolve("p1-artischocken-herzen-brie-double-creme-v1")
        assertTrue(dir.mkdirs())
        dir.resolve("review-packet.v1.json").writeText("partial")
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.PERSISTENCE_FAILED, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(packetOutputRoot = root)).reason)
    }
    @Test fun persistenceFailureIsMappedToSafeRuntimeFailure() {
        val root = Files.createTempDirectory("p1-error").toFile()
        root.resolve("p1-artischocken-herzen-brie-double-creme-v1").mkdirs()
        root.resolve("p1-artischocken-herzen-brie-double-creme-v1/review-packet.v1.json").writeText("bad")
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(packetOutputRoot = root))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.PERSISTENCE_FAILED, failure.reason)
        assertEquals("persistence", failure.safeContext)
    }
    @Test fun completedPathsAreRelative() {
        val result = completed()
        assertTrue(!result.jsonPath.startsWith("/") && !result.markdownPath.startsWith("/"))
    }
    @Test fun failureDiagnosticsContainNoAbsolutePathOrThrowableText() {
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed>(execute(packetImplementationHead = "bad"))
        assertFalse(failure.safeContext.contains("/"))
        assertFalse(failure.safeContext.contains("Exception", true))
    }
    @Test fun runtimeUsesNoExternalPorts() {
        val names = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1::class.java.declaredFields.map { it.name }.filterNot { it.startsWith("$") }
        assertTrue(names.none { it.contains("store", true) || it.contains("search", true) || it.contains("fetch", true) || it.contains("port", true) })
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed>(execute())
    }
    @Test fun runtimeUsesSyntheticCorpusPathOnly() = assertTrue(fixture().inputBinding.corpusFileBinding.relativePath.startsWith("fixture/"))
    @Test fun runtimeUsesTemporaryPacketRootOnly() {
        val root = Files.createTempDirectory("p1-safe-root").toFile()
        val result = completed(packetOutputRoot = root)
        assertTrue(root.resolve(result.jsonPath).isFile)
        assertFalse(java.io.File(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.OUTPUT_ROOT).resolve(result.jsonPath).exists())
    }
    @Test fun runtimeProducesNoDecisionBatchThroughPacketModel() {
        val result = completed()
        assertTrue(result.packet.packetState == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1.REVIEW_CONTEXT_ONLY)
        assertTrue(result.packet.items.none { it.contextLimitations.any { limitation -> limitation.name.contains("DECISION") } })
    }
    @Test fun committedDependenciesRemainAtExpectedVersions() {
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.VERSION)
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.VERSION)
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.VERSION)
    }
    @Test fun truncationAddsExplicitLimitation() {
        val value = fixture()
        val long = "z".repeat(300)
        val result = completed(rebound(value, value.corpus.entries.mapIndexed { index, entry -> if (index == 0) entry.replaceField(long) else entry }))
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FIELD_TRUNCATED_IN_HUMAN_PROJECTION in result.packet.items.first().contextLimitations)
    }
    @Test fun allFourSourcesRemainDistinctInPacketContext() {
        assertEquals(4, completed().packet.items.map { it.sourceContext.source }.distinct().size)
    }
    @Test fun runtimeResultContainsNoClockOrRandomFields() {
        assertTrue(completed()::class.java.declaredFields.none { it.name in setOf("clock", "timestamp", "random", "uuid") })
    }
    @Test fun realProtectedRootsRemainUnusedBySyntheticExecution() {
        val result = completed(packetOutputRoot = Files.createTempDirectory("p1-protected-check").toFile())
        assertFalse(result.jsonPath.contains("build/"))
        assertFalse(result.markdownPath.contains("build/"))
    }

    private fun completed(
        value: Fixture = fixture(),
        packetOutputRoot: java.io.File = Files.createTempDirectory("p1-packet").toFile(),
    ) = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed>(execute(value, packetOutputRoot = packetOutputRoot))

    private fun rebound(value: Fixture, entries: List<HimZeroCandidateRecoveryReviewCorpusEntryV1>): Fixture {
        val unsigned = value.corpus.copy(entries = entries, logicalDigest = "")
        val corpus = unsigned.copy(logicalDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.logicalDigest(unsigned))
        val inputBase = value.inputBinding.copy(
            corpusFileBinding = value.inputBinding.corpusFileBinding.copy(logicalDigest = corpus.logicalDigest),
            corpusLogicalDigest = corpus.logicalDigest,
        )
        return value.copy(corpus = corpus, inputBinding = inputBase.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(inputBase)))
    }

    private fun HimZeroCandidateRecoveryReviewCorpusEntryV1.replaceField(value: String) = copy(
        originalFields = originalFields.mapIndexed { index, field ->
            if (index != 0) field else field.copy(
                originalLexicalValue = value,
                trimmedValue = value.trim(),
                lexicalFeatures = field.lexicalFeatures.copy(empty = value.isEmpty(), characterLength = value.length),
            )
        },
    )

    private fun execute(
        value: Fixture = fixture(),
        enabled: Boolean = true,
        packetImplementationHead: String = "a".repeat(40),
        packetOutputRoot: java.io.File = Files.createTempDirectory("p1-packet").toFile(),
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1.execute(
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1(enabled, value.corpus, value.inputBinding, value.catalog, value.registry, value.authority, packetImplementationHead, packetOutputRoot),
    )

    private data class Fixture(
        val corpus: HimZeroCandidateRecoveryReviewCorpusReportV1,
        val inputBinding: HimZeroCandidateRecoveryHumanReviewInputBindingV1,
        val catalog: HimProductOnlyCanonicalMaster,
        val registry: HimEntityIdRegistry,
        val authority: HimCanonicalFamilyAuthority,
        val packetOutputRoot: java.io.File = Files.createTempDirectory("p1-packet").toFile(),
    )

    companion object {
        private fun fixture(): Fixture {
            val mission = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION
            val catalog = HimProductOnlyCanonicalMaster("fixture/catalog.json", "1".repeat(64), buildList {
                add(HimProductOnlyCanonical("Artischocken", "artischocken", emptyList()))
                add(HimProductOnlyCanonical("Crème double", "creme-double", emptyList()))
                addAll((3..1384).map { HimProductOnlyCanonical("Canonical $it", "canonical-$it", emptyList()) })
            })
            val registry = HimEntityIdRegistry(buildList {
                add(HimEntityIdRegistryEntry(HimEntityId("ZuhV5V"), HimEntityType.CANONICAL, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "artischocken"))
                add(HimEntityIdRegistryEntry(HimEntityId("rVnyq7"), HimEntityType.CANONICAL, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "creme-double"))
                addAll((3..1384).map { HimEntityIdRegistryEntry(HimEntityId("c%05d".format(it)), HimEntityType.CANONICAL, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "canonical-$it") })
            })
            val authority = HimCanonicalFamilyAuthority("1", HimCanonicalFamilySourceCatalog(catalog.path, catalog.contentSha256, catalog.records.size), buildList {
                add(HimCanonicalFamily(HimEntityId("ZuhV5V"), "Artischocken", "artischocken", emptyList(), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList()))
                add(HimCanonicalFamily(HimEntityId("rVnyq7"), "Crème double", "creme-double", emptyList(), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList()))
                addAll((3..1384).map { HimCanonicalFamily(HimEntityId("c%05d".format(it)), "Canonical $it", "canonical-$it", emptyList(), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList()) })
            })
            val entries = mission.entries.mapIndexed { index, missionEntry -> corpusEntry(missionEntry, index) }
            val corpusInputBase = HimZeroCandidateRecoveryReviewCorpusInputBindingV1(
                HimZeroCandidateCauseAnalysisFileBindingV1("fixture/cause.json", 1, "2".repeat(64), "3".repeat(64)),
                HimZeroCandidateRecoveryReviewCorpusFileBindingV1(catalog.path, 1, catalog.contentSha256, "4".repeat(64)),
                HimZeroCandidateRecoveryReviewCorpusFileBindingV1("fixture/authority.json", 1, "5".repeat(64), "6".repeat(64)),
                "3".repeat(64), "b".repeat(40), "",
            )
            val corpusInput = corpusInputBase.copy(bindingDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.bindingDigest(corpusInputBase))
            val unsigned = HimZeroCandidateRecoveryReviewCorpusReportV1(
                HimZeroCandidateRecoveryReviewCorpusContractV1.VERSION,
                corpusInput,
                HimZeroCandidateRecoveryReviewCorpusCountersV1(4, 2, 4, 0, 0, 4, 2, 4, 0),
                HimZeroCandidateRecoveryReviewCorpusContractV1.PRIORITY_ORDER.map { priority -> HimZeroCandidateRecoveryReviewPriorityCounterV1(priority, if (priority == HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET) 2 else 0, if (priority == HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET) 4 else 0, if (priority == HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET) 4 else 0) },
                HimZeroCandidateRecoveryReviewCorpusContractV1.SOURCE_ORDER.map { source -> HimZeroCandidateRecoveryReviewSourceBreakdownV1(source, entries.count { it.source == source }) },
                HimZeroCandidateRecoveryReviewCorpusContractV1.RECORD_KIND_ORDER.map { kind -> HimZeroCandidateRecoveryReviewRecordKindBreakdownV1(kind, entries.count { it.recordKind == kind }) },
                HimZeroCandidateRecoveryReviewCorpusContractV1.SELECTION_REASON_ORDER.map { reason -> HimZeroCandidateRecoveryReviewSelectionReasonBreakdownV1(reason, entries.count { reason in it.selectionReasons }) },
                entries,
                "",
            )
            val corpus = unsigned.copy(logicalDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.logicalDigest(unsigned))
            val inputBase = HimZeroCandidateRecoveryHumanReviewInputBindingV1(
                HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/review-corpus.json", 1, mission.corpusFileSha256, corpus.logicalDigest),
                corpus.logicalDigest,
                "7".repeat(64),
                corpusInput.bindingDigest,
                corpusInput,
                HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/registry.json", 1, "8".repeat(64), "9".repeat(64)),
                HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
                HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
                "c".repeat(40),
                "",
            )
            return Fixture(corpus, inputBase.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(inputBase)), catalog, registry, authority)
        }

        private fun corpusEntry(missionEntry: HimZeroCandidateRecoveryHumanReviewP1PilotMissionEntryV1, index: Int): HimZeroCandidateRecoveryReviewCorpusEntryV1 {
            val source = listOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.AGRIBALYSE, HimGroundTruthSource.CIQUAL, HimGroundTruthSource.GLYCEMIC_INDEX)[index]
            val kind = listOf(HimEvidenceRecordKind.OFF_PRODUCT, HimEvidenceRecordKind.AGRIBALYSE_RECORD, HimEvidenceRecordKind.CIQUAL_FOOD, HimEvidenceRecordKind.GI_MEASUREMENT)[index]
            val reference = when (source) {
                HimGroundTruthSource.OPEN_FOOD_FACTS -> HimEvidenceRecordReference.offProduct(1, "1001").value
                HimGroundTruthSource.AGRIBALYSE -> HimEvidenceRecordReference.agribalyse(1, "1002").value
                HimGroundTruthSource.CIQUAL -> HimEvidenceRecordReference.ciqualFood("1003").value
                HimGroundTruthSource.GLYCEMIC_INDEX -> HimEvidenceRecordReference.gi("measurement", 1).value
            }
            val groupValue = "group-${missionEntry.groupOrdinal}"
            val target = HimZeroCandidateRecoveryReviewCanonicalTargetV1(missionEntry.canonicalEntityId, missionEntry.expectedDisplayLabel, emptyList(), emptyList())
            val value = "materialized-$index"
            val lexical = HimUnresolvedPrimaryIdentityDiagnosticLexicalFeaturesV1(false, value.length, 1, 1, false, false, false, false, false, false)
            val field = HimUnresolvedPrimaryIdentityDiagnosticFieldV1("name", value, value, HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.EN, HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE, source, kind, lexical)
            return HimZeroCandidateRecoveryReviewCorpusEntryV1(
                missionEntry.stableEntryId,
                source,
                reference,
                kind,
                "shard-000001",
                listOf(HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256("finding-$index")),
                listOf(target),
                listOf(field),
                listOf("primary-$index"),
                HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH,
                emptyList(),
                listOf(HimZeroCandidateRecoveryReviewSelectionReasonV1.RECURRING_PRIMARY_VALUE_GROUP),
                listOf(HimZeroCandidateRecoveryReviewGroupMembershipV1(groupValue, HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET, 2, 2, listOf(missionEntry.canonicalEntityId).sorted())),
                HimZeroCandidateRecoveryReviewAssociationStateV1.UNVERIFIED_AUDIT_ASSOCIATION,
                HimZeroCandidateRecoveryReviewStateV1.UNREVIEWED,
            )
        }
    }
}
