package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPrimaryBucketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewGroupMembershipV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewPriorityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewSelectionReasonV1
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1Test {
    @Test
    fun persistenceIdentityAndFrozenOutputPolicyAreBound() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PACKET_PERSISTENCE_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.VERSION)
        assertEquals(
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-decision-validation-packet/v1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.OUTPUT_ROOT,
        )
        assertEquals("validation-packet.v1.json", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.JSON_FILE_NAME)
        assertEquals("validation-packet.v1.md", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.MARKDOWN_FILE_NAME)
        assertEquals("INDEPENDENT_VALIDATION_CONTEXT_ONLY", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.VALIDATION_STATE)
    }

    @Test
    fun requestAndResultModelsAreClosedAndMinimal() {
        assertEquals(
            setOf("enabled", "outputRoot", "packet"),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceRequestV1::class.java.declaredFields
                .filterNot { it.isSynthetic || it.name.startsWith("$") }.map { it.name }.toSet(),
        )
        assertEquals(
            setOf("Disabled", "Completed", "Failed"),
            setOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Disabled::class.simpleName,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Completed::class.simpleName,
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Failed::class.simpleName,
            ),
        )
        assertEquals(
            listOf("CREATED", "ALREADY_PRESENT_IDENTICAL"),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1.entries.map { it.name },
        )
    }

    @Test
    fun disabledReturnsBeforeValidationOrIo() {
        val result = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceRequestV1(
                enabled = false,
                outputRoot = Path.of("/path/not-used").toFile(),
                packet = packet().copy(state = "INVALID"),
            ),
        )
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Disabled>(result)
    }

    @Test
    fun jsonIsUtf8DeterministicStrictAndHasOneFinalLf() {
        val bytes = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.serializePacket(packet(unicode = true))
        assertEquals('\n'.code.toByte(), bytes.last())
        assertEquals(1, bytes.count { it == '\n'.code.toByte() })
        assertTrue(bytes.toString(Charsets.UTF_8).startsWith("{\"contractId\""))
        assertTrue(bytes.toString(Charsets.UTF_8).contains("日本語"))
        assertFalse(bytes.toString(Charsets.UTF_8).contains("\\u"))
        assertEquals(
            packet(unicode = true),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.deserializePacket(bytes),
        )
    }

    @Test
    fun jsonRejectsUnknownMissingDuplicateAndInvalidEnumFields() {
        val json = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1
            .serializePacket(packet()).toString(Charsets.UTF_8)
        assertDeserializationFailure(json.replaceFirst("{\"contractId\"", "{\"unknown\":1,\"contractId\""))
        assertDeserializationFailure(json.replaceFirst("\"inputBinding\":{", "\"inputBinding\":{\"unknown\":1,"))
        assertDeserializationFailure(json.replaceFirst("\"items\":[{", "\"items\":[{\"unknown\":1,"))
        assertDeserializationFailure(json.replaceFirst("\"sourceContext\":{", "\"sourceContext\":{\"unknown\":1,"))
        assertDeserializationFailure(json.replaceFirst("\"directEvidence\":[{", "\"directEvidence\":[{\"unknown\":1,"))
        assertDeserializationFailure(json.replaceFirst("\"artifact\":{", "\"artifact\":{\"unknown\":1,"))
        assertDeserializationFailure(json.replaceFirst("\"fields\":[{", "\"fields\":[{\"unknown\":1,"))
        assertDeserializationFailure(json.replaceFirst("\"targetContext\":{", "\"targetContext\":{\"unknown\":1,"))
        assertDeserializationFailure(json.replaceFirst("\"counters\":{", "\"counters\":{\"unknown\":1,"))
        assertDeserializationFailure(json.replaceFirst(",\"packetId\":", ",\"missingPacketId\":"))
        assertDeserializationFailure(json.replaceFirst("CONFIRM_ASSOCIATION", "NOT_AN_ENUM"))
        val duplicate = json.replaceFirst(",\"packetId\":", ",\"packetId\":\"p1-artischocken-brie-decision-validation-v1\",\"packetId\":")
        assertDeserializationFailure(duplicate)
    }

    @Test
    fun fourItemsEvidenceBindingsDecisionsAndCountersSurviveRoundTrip() {
        val original = packet()
        val bytes = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.serializePacket(original)
        val reloaded = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.deserializePacket(bytes)
        assertEquals(original.items.map { it.reviewUnitId }, reloaded.items.map { it.reviewUnitId })
        assertEquals(original.items.flatMap { it.directEvidence }.map { it.evidenceReferenceId }, reloaded.items.flatMap { it.directEvidence }.map { it.evidenceReferenceId })
        assertEquals(original.items.map { it.originalDecision }, reloaded.items.map { it.originalDecision })
        assertEquals(original.items.map { it.originalReasonCodes }, reloaded.items.map { it.originalReasonCodes })
        assertEquals(original.counters, reloaded.counters)
        assertEquals(original.packetBindingDigest, reloaded.packetBindingDigest)
        assertEquals(original.packetLogicalDigest, reloaded.packetLogicalDigest)
        assertEquals(12, reloaded.items.flatMap { it.directEvidence }.size)
        assertEquals(12, reloaded.items.flatMap { it.directEvidence }.map { it.evidenceReferenceId }.distinct().size)
    }

    @Test
    fun markdownIsDeterministicNeutralAndContextOnly() {
        val original = packet(unicode = true)
        val first = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.renderMarkdown(original)
        val second = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.renderMarkdown(original)
        assertEquals(first, second)
        assertTrue(first.endsWith("\n"))
        assertFalse(first.endsWith("\n\n"))
        assertTrue(first.contains("INDEPENDENT VALIDATION CONTEXT ONLY"))
        assertTrue(first.contains("NO GOLD, NEGATIVE SUPERVISION, TRAINING, PUBLICATION, OR AUTHORITY EFFECT"))
        assertTrue(first.contains("SOURCE EVIDENCE"))
        assertTrue(first.contains("CANONICAL CATALOG EVIDENCE"))
        assertTrue(first.contains("CANONICAL FAMILY AUTHORITY EVIDENCE"))
        assertTrue(first.contains("CONFIRM_ASSOCIATION"))
        assertFalse(first.contains("VALIDATE_ORIGINAL_DECISION"))
        assertFalse(first.contains("POTENTIAL_POSITIVE_GOLD_CANDIDATE"))
        assertFalse(first.contains("/Users/"))
    }

    @Test
    fun manipulatedPacketDigestsFailClosedBeforePublication() = withRoot { root ->
        val original = packet()
        val request = request(root, original)
        val result = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(
            request.copy(packet = original.copy(packetBindingDigest = "0".repeat(64))),
        )
        assertFailure(result, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1.BINDING_DIGEST_MISMATCH)
        assertTrue(root.toFile().listFiles().isNullOrEmpty())
    }

    @Test
    fun firstExecutionPublishesExactlyTwoFilesReloadsAndReturnsRelativePaths() = withRoot { root ->
        val original = packet()
        val completed = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(request(root, original)),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1.CREATED, completed.status)
        assertEquals("${original.packetId}/validation-packet.v1.json", completed.jsonPath)
        assertEquals("${original.packetId}/validation-packet.v1.md", completed.markdownPath)
        assertFalse(completed.jsonPath.startsWith('/'))
        val directory = root.resolve(original.packetId)
        assertEquals(setOf("validation-packet.v1.json", "validation-packet.v1.md"), directory.toFile().list()!!.toSet())
        assertEquals(original, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.readPacket(directory.resolve("validation-packet.v1.json").toFile()))
        assertEquals(completed.jsonByteSize, Files.size(directory.resolve("validation-packet.v1.json")))
        assertEquals(completed.markdownByteSize, Files.size(directory.resolve("validation-packet.v1.md")))
        assertEquals(completed.jsonSha256, sha(directory.resolve("validation-packet.v1.json")))
        assertEquals(completed.markdownSha256, sha(directory.resolve("validation-packet.v1.md")))
    }

    @Test
    fun secondExecutionIsAlreadyPresentIdenticalAndByteStable() = withRoot { root ->
        val original = packet()
        val request = request(root, original)
        val first = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(request),
        )
        val snapshot = snapshot(root)
        val second = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(request),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1.CREATED, first.status)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, second.status)
        assertEquals(snapshot, snapshot(root))
        assertEquals(first.jsonSha256, second.jsonSha256)
        assertEquals(first.markdownSha256, second.markdownSha256)
    }

    @Test
    fun partialAndExistingConflictsAreNeverOverwritten() = withRoot { root ->
        val original = packet()
        val request = request(root, original)
        val directory = root.resolve(original.packetId)
        Files.createDirectories(directory)
        Files.write(directory.resolve("validation-packet.v1.json"), byteArrayOf(1))
        assertFailure(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(request),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1.PARTIAL_PACKET_ARTIFACT_STATE,
        )
        Files.delete(directory.resolve("validation-packet.v1.json"))
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(request),
        )
        val jsonPath = directory.resolve("validation-packet.v1.json")
        val originalJson = Files.readAllBytes(jsonPath)
        Files.write(jsonPath, originalJson + byteArrayOf(1))
        assertFailure(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(request),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1.EXISTING_JSON_CONFLICT,
        )
        assertTrue(Files.readAllBytes(jsonPath).contentEquals(originalJson + byteArrayOf(1)))
    }

    @Test
    fun invalidPacketIdTraversalAndUnexpectedFilesFailClosed() = withRoot { root ->
        val original = packet()
        val invalid = original.copy(packetId = "../escape")
        assertFailure(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(request(root, invalid)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1.INVALID_PACKET_ID,
        )
        val directory = root.resolve(original.packetId)
        Files.createDirectories(directory)
        Files.write(directory.resolve("unexpected.txt"), byteArrayOf(1))
        assertFailure(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(request(root, original)),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1.UNEXPECTED_OUTPUT_FILE,
        )
    }

    @Test
    fun symlinkOutputRootIsRejectedWithoutFollowingIt() = withRoot { root ->
        val outside = Files.createTempDirectory("him-validation-outside")
        val link = root.resolve("link")
        try {
            try {
                Files.createSymbolicLink(link, outside)
            } catch (_: UnsupportedOperationException) {
                return@withRoot
            } catch (_: java.nio.file.FileSystemException) {
                return@withRoot
            }
            val result = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.execute(
                request(link, packet()),
            )
            assertFailure(result, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1.SYMLINK_ESCAPE)
            assertTrue(outside.toFile().listFiles().isNullOrEmpty())
        } finally {
            outside.toFile().deleteRecursively()
        }
    }

    @Test
    fun persistenceHasNoRuntimeOrExternalStoreDependency() {
        val methods = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1::class.java.declaredMethods
            .map { it.name.lowercase() }
        assertTrue(methods.none { name -> listOf("network", "openai", "inference", "retrieval", "search", "fetch", "sqlite").any(name::contains) })
    }

    private fun assertDeserializationFailure(json: String) {
        assertFailsWith<IllegalArgumentException> {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.deserializePacket(json.toByteArray(Charsets.UTF_8))
        }
    }

    private fun assertFailure(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1,
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1,
    ) {
        assertEquals(reason, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Failed>(result).reason)
    }

    private fun request(root: Path, packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceRequestV1(true, root.toFile(), packet)

    private fun withRoot(block: (Path) -> Unit) {
        val root = Files.createTempDirectory("him-validation-packet-persistence")
        try {
            block(root)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun snapshot(root: Path): Map<String, String> {
        val directory = root.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.PACKET_ID)
        return directory.toFile().listFiles().orEmpty().associate { file ->
            file.name to file.readBytes().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        }
    }

    private fun sha(path: Path): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path)).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun packet(unicode: Boolean = false): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1 {
        val items = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION.entries.mapIndexed { index, entry ->
            val selection = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.FROZEN_SELECTIONS[index]
            val source = listOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.AGRIBALYSE, HimGroundTruthSource.CIQUAL, HimGroundTruthSource.GLYCEMIC_INDEX)[index]
            val recordKind = listOf(HimEvidenceRecordKind.OFF_PRODUCT, HimEvidenceRecordKind.AGRIBALYSE_RECORD, HimEvidenceRecordKind.CIQUAL_FOOD, HimEvidenceRecordKind.GI_MEASUREMENT)[index]
            val evidence = selection.evidenceReferenceIds.sorted().mapIndexed { evidenceIndex, referenceId ->
                val kind = when (evidenceIndex) {
                    0 -> HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION
                    1 -> HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD
                    else -> HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD
                }
                val position = when {
                    evidenceIndex == 0 && (index == 0 || index == 2) -> HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION
                    evidenceIndex == 1 && (index == 0 || index == 2) -> HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION
                    evidenceIndex == 0 -> HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
                    else -> HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY
                }
                val artifact = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1(
                    "fixture/$index/$evidenceIndex.json", 10,
                    "${index}${evidenceIndex}".padEnd(64, 'a').take(64),
                    "${index}${evidenceIndex}".padEnd(64, 'b').take(64),
                )
                val value = "value-$index-$evidenceIndex"
                val field = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1("field", value, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1.sha256(value))
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(
                    entry.reviewUnitId, referenceId, kind, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position, artifact, "record:$index:$evidenceIndex", listOf(field),
                    if (kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION) {
                        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1(source, recordKind, artifact, "record:$index:$evidenceIndex", listOf(field), artifact)
                    } else null,
                )
            }
            val context = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1(
                source, recordKind, "fixture:$index", listOf(shaString("finding-$index")),
                listOf(HimZeroCandidateRecoveryReviewSelectionReasonV1.RECURRING_PRIMARY_VALUE_GROUP), listOf("primary-$index"),
                HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH,
                listOf(HimZeroCandidateRecoveryReviewGroupMembershipV1(entry.groupDisplayValue, HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET, 2, 2, listOf(entry.canonicalEntityId))),
            )
            val originalValue = if (unicode) "Quelle $index — 日本語\ncrème brûlée" else "source-$index"
            val originalField = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1(
                "source", originalValue, shaString(originalValue), originalValue,
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL, originalValue.length,
            )
            val target = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1(
                entry.canonicalEntityId, entry.expectedDisplayLabel,
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED,
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED,
                "catalog:${entry.canonicalEntityId}", "authority:${entry.canonicalEntityId}", emptyList(), emptyList(), "",
            )
            val targetBound = target.copy(targetContextDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(target))
            val rationale = "Bound original rationale for review unit ${index + 1}."
            val item = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1(
                entry.reviewUnitId, entry.stableEntryId, entry.canonicalEntityId, entry.expectedDisplayLabel, context,
                listOf(originalField), "", selection.decision, selection.reasonCodes, selection.evidenceReferenceIds.sorted(),
                rationale, shaString(rationale), null, targetBound, evidence, emptyList(), emptyList(), emptyList(), "", "",
            )
            item.copy(originalDecisionIdentity = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.originalDecisionIdentity(item))
        }
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.create(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.frozenInputBinding(), items,
        )
    }

    private fun shaString(value: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
