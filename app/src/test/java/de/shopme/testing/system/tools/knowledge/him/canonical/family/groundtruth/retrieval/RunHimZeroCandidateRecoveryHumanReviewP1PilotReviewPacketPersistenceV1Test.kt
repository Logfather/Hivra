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
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionEntryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceScopeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewGroupMembershipV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewPriorityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewSelectionReasonV1
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class RunHimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1Test {
    @Test fun persistenceContractIdVersionRootAndNamesAreFrozen() {
        assertEquals("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_REVIEW_PACKET_PERSISTENCE_V1", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.CONTRACT_ID)
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.VERSION)
        assertEquals("build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-review-packet/v1", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.OUTPUT_ROOT)
        assertEquals("review-packet.v1.json", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.JSON_FILE_NAME)
        assertEquals("review-packet.v1.md", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.MARKDOWN_FILE_NAME)
    }

    @Test fun requestFieldModelIsExactlyFrozen() {
        val fields = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceRequestV1::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
        assertEquals(setOf("enabled", "outputRoot", "packet"), fields.map { it.name }.toSet())
    }

    @Test fun disabledDoesNoFileIo() {
        val root = Files.createTempDirectory("him-packet-disabled-").resolve("not-created").toFile()
        val result = execute(root, enabled = false)
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Disabled>(result)
        assertFalse(root.exists())
    }

    @Test fun validPacketSerializesDeterministicallyAndEndsWithOneLf() {
        val first = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.serializePacket(packet())
        val second = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.serializePacket(packet())
        assertTrue(first.contentEquals(second))
        assertEquals(1, first.takeLastWhile { it == '\n'.code.toByte() }.size)
        assertFalse(first.dropLast(1).contains('\n'.code.toByte()))
    }

    @Test fun jsonRoundTripIsComplete() {
        val packet = packet()
        val reloaded = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.deserializePacket(
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.serializePacket(packet),
        )
        assertEquals(packet, reloaded)
    }

    @Test fun unknownJsonFieldFailsClosed() {
        val json = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.serializePacket(packet()).toString(Charsets.UTF_8)
        val unknown = json.dropLast(2) + ",\"unknown\":true}\n"
        assertFailsWith<IllegalArgumentException> {
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.deserializePacket(unknown.toByteArray())
        }
    }

    @Test fun packetAndItemDigestsAreValidatedBeforeSerialization() {
        val broken = packet().copy(packetBindingDigest = "0".repeat(64))
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed>(execute(Files.createTempDirectory("him-packet-invalid-").toFile(), broken))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.PACKET_BINDING_DIGEST_MISMATCH, failure.reason)
    }

    @Test fun originalValueDigestAndItemDigestAreValidatedOnReload() {
        val broken = packet().items[0].copy(originalFields = listOf(field("name", "changed", originalDigest = sha256("source value 1"))))
        val rebuilt = packet(packet().items.toMutableList().also { it[0] = broken })
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.PACKET_VALIDATION_FAILED, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed>(execute(Files.createTempDirectory("him-packet-item-invalid-").toFile(), rebuilt)).reason)
        val tampered = packet().copy(items = packet().items.toMutableList().also { it[0] = it[0].copy(originalFields = listOf(field("name", "tampered"))) })
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.PACKET_VALIDATION_FAILED, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed>(execute(Files.createTempDirectory("him-packet-item-invalid-2-").toFile(), tampered)).reason)
    }

    @Test fun markdownStartsWithExactWarningBlockAndHasFourUnits() = withRoot { root ->
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Completed>(execute(root, packet()))
        val markdown = root.resolve("${missionId()}/review-packet.v1.md").readText()
        assertEquals("# HIM Zero-Candidate Recovery Human Review – P1 Pilot Review Packet V1\n\n> REVIEW CONTEXT ONLY\n> NO DECISION RECORDED\n> NO GOLD, TRAINING OR AUTHORITY EFFECT", markdown.substringBefore("\n\nPacket contract ID:"))
        assertEquals(4, Regex("^## Review Unit \\d+$", RegexOption.MULTILINE).findAll(markdown).count())
        assertEquals(result.markdownSha256, sha256(markdown.toByteArray()))
    }

    @Test fun markdownHasFixedSectionsSeparatedSourceAndTargetAndNoDecisionList() = withRoot { root ->
        execute(root, packet())
        val markdown = root.resolve("${missionId()}/review-packet.v1.md").readText()
        val sections = listOf("REVIEW UNIT", "SOURCE CONTEXT", "MATERIALIZED ORIGINAL FIELDS", "AUDIT TARGET UNDER REVIEW", "TARGET CONTEXT", "EVIDENCE SCOPE", "CONTEXT LIMITATIONS", "BINDINGS")
        sections.forEach { assertEquals(4, Regex("^### $it$", RegexOption.MULTILINE).findAll(markdown).count()) }
        assertFalse(markdown.contains("→"))
        assertFalse(markdown.contains("Decision"))
        listOf("correct", "incorrect", "likely", "recommended", "confirmed", "rejected", "wahrscheinlich", "empfohlen", "bestätigt", "abgelehnt").forEach { assertFalse(markdown.contains(it, ignoreCase = true)) }
    }

    @Test fun markdownEscapesStructureAndIndentsMultilineLinkAndHtmlValues() {
        val unsafe = "# heading\n[link](https://example.test)\n<b>html</b>"
        val base = packet().items[0]
        val item = base.copy(originalFields = listOf(field("field`name", unsafe)))
        val markdown = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.renderMarkdown(packet(packet().items.toMutableList().also { it[0] = item }))
        assertTrue(markdown.contains("    # heading\n    [link](https://example.test)\n    <b>html</b>"))
        assertFalse(markdown.contains("\n# heading"))
    }

    @Test fun markdownRendersTruncatedAndEmptyDisplayStates() {
        val original = "x".repeat(300)
        val truncated = original.take(239) + "…"
        val base = packet().items[0]
        val truncatedItem = base.copy(originalFields = listOf(field("long", original, display = truncated, state = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.TRUNCATED_FOR_HUMAN_PROJECTION)), contextLimitations = limitations(true))
        val emptyItem = packet().items[1].copy(originalFields = listOf(field("empty", "", display = "", state = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.EMPTY_SOURCE_VALUE)))
        val markdown = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.renderMarkdown(packet(packet().items.toMutableList().also { it[0] = truncatedItem; it[1] = emptyItem }))
        assertTrue("Display note: TRUNCATED_FOR_HUMAN_PROJECTION" in markdown)
        assertTrue("Display note: EMPTY_SOURCE_VALUE" in markdown)
        assertTrue("Characters: 300" in markdown)
    }

    @Test fun materializedCoverageUsesExactNeutralExplanationAndNoAbsolutePaths() {
        val markdown = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.renderMarkdown(packet())
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.MATERIALIZED_CORPUS_EXPLANATION.lines().all { it in markdown })
        assertFalse(Regex("(^|[ :])/(Users|private|tmp)/").containsMatchIn(markdown))
    }

    @Test fun firstPersistenceCreatesExactlyJsonAndMarkdownAndReloads() = withRoot { root ->
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Completed>(execute(root, packet()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1.CREATED, result.status)
        val directory = root.resolve(missionId())
        assertEquals(setOf("review-packet.v1.json", "review-packet.v1.md"), directory.list()?.toSet())
        assertEquals(packet(), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.readPacket(directory.resolve("review-packet.v1.json")))
        assertEquals("${missionId()}/review-packet.v1.json", result.jsonPath)
        assertEquals("${missionId()}/review-packet.v1.md", result.markdownPath)
    }

    @Test fun secondIdenticalPersistenceIsByteIdenticalAndNoOverwrite() = withRoot { root ->
        val first = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Completed>(execute(root, packet()))
        val jsonBefore = root.resolve("${missionId()}/review-packet.v1.json").readBytes()
        val markdownBefore = root.resolve("${missionId()}/review-packet.v1.md").readBytes()
        val second = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Completed>(execute(root, packet()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, second.status)
        assertTrue(jsonBefore.contentEquals(root.resolve("${missionId()}/review-packet.v1.json").readBytes()))
        assertTrue(markdownBefore.contentEquals(root.resolve("${missionId()}/review-packet.v1.md").readBytes()))
        assertEquals(first.jsonSha256, second.jsonSha256)
        assertEquals(first.markdownSha256, second.markdownSha256)
    }

    @Test fun onlyJsonIsPartialStateAndUntouched() = withRoot { root ->
        val directory = root.toPath().resolve(missionId()).also { Files.createDirectories(it) }.toFile()
        directory.resolve("review-packet.v1.json").writeText("partial")
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed>(execute(root, packet()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.PARTIAL_PACKET_ARTIFACT_STATE, result.reason)
        assertEquals("partial", directory.resolve("review-packet.v1.json").readText())
    }

    @Test fun onlyMarkdownIsPartialStateAndUntouched() = withRoot { root ->
        val directory = root.toPath().resolve(missionId()).also { Files.createDirectories(it) }.toFile()
        directory.resolve("review-packet.v1.md").writeText("partial")
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed>(execute(root, packet()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.PARTIAL_PACKET_ARTIFACT_STATE, result.reason)
        assertEquals("partial", directory.resolve("review-packet.v1.md").readText())
    }

    @Test fun conflictingJsonAndMarkdownAreNeverOverwritten() = withRoot { root ->
        execute(root, packet())
        val json = root.resolve("${missionId()}/review-packet.v1.json")
        val markdown = root.resolve("${missionId()}/review-packet.v1.md")
        val jsonBefore = json.readBytes(); val markdownBefore = markdown.readBytes()
        json.writeBytes(jsonBefore + "x".toByteArray())
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed>(execute(root, packet()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.EXISTING_JSON_CONFLICT, result.reason)
        assertTrue(markdownBefore.contentEquals(markdown.readBytes()))
    }

    @Test fun conflictingMarkdownIsNeverOverwritten() = withRoot { root ->
        execute(root, packet())
        val json = root.resolve("${missionId()}/review-packet.v1.json")
        val markdown = root.resolve("${missionId()}/review-packet.v1.md")
        val jsonBefore = json.readBytes()
        val markdownBefore = markdown.readBytes()
        markdown.writeBytes(markdownBefore + "\nchanged".toByteArray())
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed>(execute(root, packet()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.EXISTING_MARKDOWN_CONFLICT, result.reason)
        assertTrue(jsonBefore.contentEquals(json.readBytes()))
    }

    @Test fun unsafeMissionIdFailsBeforeOutputPathResolution() {
        val brokenBinding = packet().missionBinding.copy(missionId = "../escape")
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed>(execute(Files.createTempDirectory("him-packet-path-").toFile(), packet().copy(missionBinding = brokenBinding)))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.INVALID_MISSION_ID, result.reason)
    }

    @Test fun symlinkMissionDirectoryFailsClosed() = withRoot { root ->
        val outside = Files.createTempDirectory("him-packet-outside-")
        Files.createSymbolicLink(root.toPath().resolve(missionId()), outside)
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed>(execute(root, packet()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.SYMLINK_ESCAPE, result.reason)
        assertTrue(Files.list(outside).use { !it.findAny().isPresent })
        outside.toFile().deleteRecursively()
    }

    @Test fun completedContainsOnlyRelativePathsAndStableDigests() = withRoot { root ->
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Completed>(execute(root, packet()))
        assertFalse(result.jsonPath.startsWith("/")); assertFalse(result.markdownPath.startsWith("/"))
        assertEquals(64, result.jsonSha256.length); assertEquals(64, result.markdownSha256.length)
        assertEquals(packet().packetBindingDigest, result.packetBindingDigest)
        assertEquals(packet().packetLogicalDigest, result.packetLogicalDigest)
    }

    @Test fun persistenceHasNoRetrievalOrDecisionSurface() {
        val names = (HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1::class.java.declaredMethods.map { it.name } + HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceRequestV1::class.java.declaredFields.map { it.name }).joinToString(" ").lowercase()
        assertFalse(names.contains("search") || names.contains("fetch") || names.contains("retrieve") || names.contains("decision") || names.contains("corpusreader"))
    }

    private fun execute(root: java.io.File, packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1 = packet(), enabled: Boolean = true) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.execute(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceRequestV1(enabled, root, packet))
    private fun withRoot(block: (java.io.File) -> Unit) { val root = Files.createTempDirectory("him-packet-persistence-").toFile(); try { block(root) } finally { root.deleteRecursively() } }
    private fun missionId() = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.MISSION_ID

    private fun packet(items: List<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1> = baseItems()) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.create(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.missionBinding(mission()), inputBinding(), inputBinding().corpusFileBinding, inputBinding().corpusLogicalDigest, inputBinding().corpusBindingDigest, "a".repeat(40), items)
    private fun mission() = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION
    private fun baseItems() = mission().entries.mapIndexed { index, entry -> fixtureItem(index + 1, entry) }

    private fun fixtureItem(index: Int, entry: HimZeroCandidateRecoveryHumanReviewP1PilotMissionEntryV1) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1(entry.stableEntryId, entry.reviewUnitId, entry.canonicalEntityId, entry.groupOrdinal, entry.groupDisplayValue, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1(HimGroundTruthSource.OPEN_FOOD_FACTS, HimEvidenceRecordKind.OFF_PRODUCT, HimEvidenceRecordReference.offProduct(index.toLong(), "code$index").value, listOf(sha256("finding$index")), listOf(HimZeroCandidateRecoveryReviewSelectionReasonV1.RECURRING_PRIMARY_VALUE_GROUP), listOf("primary $index"), HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH, listOf(HimZeroCandidateRecoveryReviewGroupMembershipV1(entry.groupDisplayValue, HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET, 2, 2, listOf(entry.canonicalEntityId)))), listOf(field("name", "source value $index")), target(entry.canonicalEntityId, entry.expectedDisplayLabel), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceScopeV1(HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, "data/evidence/corpus.json", "b".repeat(64), null, "record:$index", listOf("name"), HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY), limitations(), "", "")
    private fun field(name: String, value: String, originalDigest: String = sha256(value), display: String? = value, state: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1 = if (value.isEmpty()) HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.EMPTY_SOURCE_VALUE else HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1(name, value, originalDigest, display, state, value.length)
    private fun target(id: String, label: String): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1 { val base = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1(id, label, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, "catalog:$id", "authority:$id", emptyList(), emptyList(), ""); return base.copy(targetContextDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(base)) }
    private fun limitations(includeTruncation: Boolean = false) = buildList { add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.IDENTITY_TERMS_ABSENT); add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.ALIAS_TERMS_ABSENT); add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FULL_SOURCE_RECORD_NOT_INCLUDED); add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.SOURCE_PROJECTION_NOT_INCLUDED); if (includeTruncation) add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FIELD_TRUNCATED_IN_HUMAN_PROJECTION) }.sortedBy { it.ordinal }

    private fun inputBinding(): HimZeroCandidateRecoveryHumanReviewInputBindingV1 {
        val cause = HimZeroCandidateCauseAnalysisFileBindingV1("fixture/cause.json", 1, "c".repeat(64), "d".repeat(64))
        val corpusBase = HimZeroCandidateRecoveryReviewCorpusInputBindingV1(cause, HimZeroCandidateRecoveryReviewCorpusFileBindingV1("fixture/catalog.json", 1, "1".repeat(64), "2".repeat(64)), HimZeroCandidateRecoveryReviewCorpusFileBindingV1("fixture/authority.json", 1, "3".repeat(64), "4".repeat(64)), "d".repeat(64), "a".repeat(40), "")
        val corpus = corpusBase.copy(bindingDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.bindingDigest(corpusBase))
        val base = HimZeroCandidateRecoveryHumanReviewInputBindingV1(HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/review-corpus.json", 1, "5".repeat(64), "6".repeat(64)), "6".repeat(64), "7".repeat(64), "8".repeat(64), corpus, HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/registry.json", 1, "9".repeat(64), "a".repeat(64)), HimZeroCandidateRecoveryHumanReviewContractV1.VERSION, HimZeroCandidateRecoveryHumanReviewContractV1.VERSION, "a".repeat(40), "")
        return base.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(base))
    }
    private fun sha256(value: String) = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(value)
    private fun sha256(bytes: ByteArray) = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(bytes.toString(Charsets.UTF_8))
}
