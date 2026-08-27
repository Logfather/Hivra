package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1Test {
    @Test
    fun persistenceIdentityVersionRootAndNamesAreFrozen() {
        assertEquals("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_PERSISTENCE_V1", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.CONTRACT_ID)
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.VERSION)
        assertEquals("build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-direct-evidence-supplement/v1", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.OUTPUT_ROOT)
        assertEquals("direct-evidence-supplement.v1.json", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.JSON_FILE_NAME)
        assertEquals("direct-evidence-supplement.v1.md", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MARKDOWN_FILE_NAME)
    }

    @Test
    fun requestAndResultModelsExposeOnlyPersistenceFields() {
        val requestFields = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceRequestV1::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
        assertEquals(setOf("enabled", "outputRoot", "supplement"), requestFields.map { it.name }.toSet())
        val completedFields = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Completed::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
        assertTrue(completedFields.map { it.name }.toSet().containsAll(setOf("status", "supplement", "jsonPath", "markdownPath", "jsonSha256", "markdownSha256", "supplementBindingDigest", "supplementLogicalDigest")))
        assertEquals(setOf("CREATED", "ALREADY_PRESENT_IDENTICAL"), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1.entries.map { it.name }.toSet())
    }

    @Test
    fun disabledReturnsBeforeValidationOrFileIo() {
        val root = Files.createTempDirectory("him-direct-disabled-").resolve("not-created").toFile()
        val broken = fixture().copy(contractId = "broken")
        val result = execute(root, broken, enabled = false)
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Disabled>(result)
        assertFalse(root.exists())
    }

    @Test
    fun jsonIsDeterministicUtf8AndHasExactlyOneTrailingLf() {
        val first = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.serializeSupplement(fixture())
        val second = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.serializeSupplement(fixture())
        assertTrue(first.contentEquals(second))
        assertEquals(1, first.takeLastWhile { it == '\n'.code.toByte() }.size)
        assertFalse(first.dropLast(1).contains('\n'.code.toByte()))
        assertTrue(first.toString(Charsets.UTF_8).contains("fullValue"))
    }

    @Test
    fun typedJsonRoundTripIsComplete() {
        val original = fixture()
        val reloaded = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.deserializeSupplement(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.serializeSupplement(original),
        )
        assertEquals(original, reloaded)
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Valid>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.validate(reloaded),
        )
    }

    @Test
    fun unknownFieldsAreRejectedAtRootAndNestedLevels() {
        val json = jsonText()
        assertFailure(json.replaceFirst("\"contractId\"", "\"unknown\":true,\"contractId\""))
        assertFailure(json.replaceFirst("\"relativePath\"", "\"unknown\":true,\"relativePath\""))
    }

    @Test
    fun missingDuplicateAndWrongTypedFieldsAreRejected() {
        val json = jsonText()
        assertFailure(json.replaceFirst(",\"state\":\"DIRECT_EVIDENCE_CONTEXT_ONLY\"", ""))
        assertFailure(json.replaceFirst("\"state\":\"DIRECT_EVIDENCE_CONTEXT_ONLY\"", "\"state\":\"DIRECT_EVIDENCE_CONTEXT_ONLY\",\"state\":\"DIRECT_EVIDENCE_CONTEXT_ONLY\""))
        assertFailure(json.replaceFirst("\"version\":\"1\"", "\"version\":1"))
    }

    @Test
    fun unicodeMarkdownCharactersAndMultilineValuesRemainComplete() {
        val value = "😀\n# heading\n[link](https://example.test)\n`literal`\n<not-html>"
        val original = fixture(value)
        val markdown = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.renderMarkdown(original)
        assertTrue(value.lines().all { "    $it" in markdown })
        assertTrue(markdown.contains("fullValue"))
        assertFalse(markdown.contains("\n# heading"))
        assertFalse(Regex("(^|[ :])/(Users|private|tmp)/").containsMatchIn(markdown))
    }

    @Test
    fun markdownTitleWarningAndFourUnitsAreFixedAndNeutral() {
        val markdown = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.renderMarkdown(fixture())
        assertTrue(markdown.startsWith(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MARKDOWN_HEADER + "\n\n" + HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MARKDOWN_WARNING_BLOCK))
        assertEquals(4, Regex("^## REVIEW UNIT [1-4]$", RegexOption.MULTILINE).findAll(markdown).count())
        listOf("confirmed", "rejected", "approved", "reviewer").forEach { forbidden ->
            assertFalse(markdown.contains(forbidden, ignoreCase = true))
        }
        assertTrue(markdown.contains("SUPPORTS_ASSOCIATION"))
        assertTrue(markdown.contains("CONTRADICTS_ASSOCIATION"))
        assertTrue(markdown.contains("CONTEXT_ONLY"))
    }

    @Test
    fun markdownHasCompleteBindingsEvidenceFieldsAndDigestsInContractOrder() {
        val markdown = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.renderMarkdown(fixture())
        assertTrue(markdown.contains("Mission selection digest:"))
        assertTrue(markdown.contains("Packet input binding digest:"))
        assertTrue(markdown.contains("Corpus artifact SHA-256:"))
        assertTrue(markdown.contains("Source-origin artifact SHA-256:"))
        assertTrue(markdown.contains("Supplement binding digest:"))
        assertTrue(markdown.contains("Supplement logical digest:"))
        assertTrue(markdown.indexOf("## REVIEW UNIT 1") < markdown.indexOf("## REVIEW UNIT 2"))
        assertTrue(markdown.indexOf("## REVIEW UNIT 2") < markdown.indexOf("## REVIEW UNIT 3"))
        assertTrue(markdown.indexOf("## REVIEW UNIT 3") < markdown.indexOf("## REVIEW UNIT 4"))
    }

    @Test
    fun markdownContainsNoDecisionOrMutationModelFields() {
        val forbiddenFields = setOf("decision", "reviewer", "reviewRound", "revision", "approval", "publication", "training", "mutation", "alternativeCanonicalProposal")
        val text = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.renderMarkdown(fixture())
        forbiddenFields.forEach { field -> assertFalse(Regex("(?i)\\b$field\\s*:").containsMatchIn(text)) }
    }

    @Test
    fun firstCallCreatesAtomicSiblingPairAndReloads() = withRoot { root ->
        val result = completed(execute(root, fixture()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1.CREATED, result.status)
        val directory = root.toPath().resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MISSION_DIRECTORY)
        assertEquals(setOf("direct-evidence-supplement.v1.json", "direct-evidence-supplement.v1.md"), Files.list(directory).use { stream -> stream.map { it.fileName.toString() }.toList().toSet() })
        assertEquals(fixture(), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.readSupplement(directory.resolve("direct-evidence-supplement.v1.json").toFile()))
        assertEquals("${HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MISSION_DIRECTORY}/direct-evidence-supplement.v1.json", result.jsonPath)
        assertEquals("${HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MISSION_DIRECTORY}/direct-evidence-supplement.v1.md", result.markdownPath)
    }

    @Test
    fun secondIdenticalCallIsAlreadyPresentAndByteIdentical() = withRoot { root ->
        val first = completed(execute(root, fixture()))
        val directory = root.toPath().resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MISSION_DIRECTORY)
        val jsonBefore = Files.readAllBytes(directory.resolve("direct-evidence-supplement.v1.json"))
        val markdownBefore = Files.readAllBytes(directory.resolve("direct-evidence-supplement.v1.md"))
        val second = completed(execute(root, fixture()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, second.status)
        assertTrue(jsonBefore.contentEquals(Files.readAllBytes(directory.resolve("direct-evidence-supplement.v1.json"))))
        assertTrue(markdownBefore.contentEquals(Files.readAllBytes(directory.resolve("direct-evidence-supplement.v1.md"))))
        assertEquals(first.jsonSha256, second.jsonSha256)
        assertEquals(first.markdownSha256, second.markdownSha256)
    }

    @Test
    fun conflictsAndPartialPairsAreNeverOverwritten() = withRoot { root ->
        execute(root, fixture())
        val directory = root.toPath().resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MISSION_DIRECTORY)
        val json = directory.resolve("direct-evidence-supplement.v1.json")
        val markdown = directory.resolve("direct-evidence-supplement.v1.md")
        val jsonBefore = Files.readAllBytes(json)
        Files.write(json, jsonBefore + byteArrayOf(1))
        assertReason(execute(root, fixture()), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.EXISTING_JSON_CONFLICT)
        assertTrue(Files.readAllBytes(markdown).isNotEmpty())
        Files.delete(json)
        assertReason(execute(root, fixture()), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.PARTIAL_ARTIFACT_STATE)
        Files.delete(markdown)
        Files.createFile(json)
        assertReason(execute(root, fixture()), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.PARTIAL_ARTIFACT_STATE)
    }

    @Test
    fun unsafeMissionAndOutputRootsFailClosedWithoutRealOutput() {
        val brokenMission = fixture().copy(binding = fixture().binding.copy(missionId = "../escape"))
        assertReason(execute(Files.createTempDirectory("him-direct-path-").toFile(), brokenMission), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.INVALID_MISSION_ID)
        val protectedRoot = Files.createTempDirectory("him-direct-packet-").resolve("p1-pilot-review-packet").toFile()
        assertReason(execute(protectedRoot, fixture()), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.FORBIDDEN_REAL_OUTPUT_ACCESS)
    }

    @Test
    fun missionDirectorySymlinkIsRejected() = withRoot { root ->
        val outside = Files.createTempDirectory("him-direct-outside-")
        Files.createSymbolicLink(root.toPath().resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MISSION_DIRECTORY), outside)
        assertReason(execute(root, fixture()), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SYMLINK_REJECTED)
        assertTrue(Files.list(outside).use { !it.findAny().isPresent })
        outside.toFile().deleteRecursively()
    }

    @Test
    fun legitimateParentSymlinkIsNotRejected() {
        val outer = Files.createTempDirectory("him-direct-parent-")
        val realParent = Files.createDirectory(outer.resolve("real"))
        val parentLink = Files.createSymbolicLink(outer.resolve("parent-link"), realParent)
        val root = parentLink.resolve("output").toFile()
        try {
            assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Completed>(execute(root, fixture()))
        } finally {
            outer.toFile().deleteRecursively()
        }
    }

    @Test
    fun completedHasOnlyRelativePathsAndStableHashes() = withRoot { root ->
        val result = completed(execute(root, fixture()))
        assertFalse(result.jsonPath.startsWith('/'))
        assertFalse(result.markdownPath.startsWith('/'))
        assertEquals(64, result.jsonSha256.length)
        assertEquals(64, result.markdownSha256.length)
        assertEquals(fixture().supplementBindingDigest, result.supplementBindingDigest)
        assertEquals(fixture().supplementLogicalDigest, result.supplementLogicalDigest)
        assertNotNull(result.supplement)
    }

    @Test
    fun malformedPersistedJsonFailsClosed() = withRoot { root ->
        val result = completed(execute(root, fixture()))
        val json = root.toPath().resolve(result.jsonPath)
        Files.write(json, "not-json\n".toByteArray())
        assertFailsWith<IllegalArgumentException> { HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.readSupplement(json.toFile()) }
    }

    @Test
    fun countersRemainExactlyContractDerived() {
        val supplement = fixture()
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.deriveCounters(supplement.bundles),
            supplement.counters,
        )
    }

    @Test
    fun unknownEnumAndNestedTypeValuesAreRejected() {
        val json = jsonText()
        assertFailure(json.replaceFirst("\"kind\":\"SOURCE_EVIDENCE_PROJECTION\"", "\"kind\":\"UNKNOWN_KIND\""))
        assertFailure(json.replaceFirst("\"byteSize\":1", "\"byteSize\":\"1\""))
        assertFailure(
            Regex("\"fullValueSha256\"\\s*:\\s*\"[^\"]+\"[,]?", RegexOption.MULTILINE)
                .replaceFirst(json, ""),
        )
    }

    @Test
    fun nestedUnknownAndDuplicateFieldsAreRejected() {
        val json = jsonText()
        assertFailure(json.replaceFirst("\"fullValue\":", "\"unknown\":true,\"fullValue\":"))
        assertFailure(json.replaceFirst("\"fullValue\":\"Artischocken Herzen\"", "\"fullValue\":\"Artischocken Herzen\",\"fullValue\":\"Artischocken Herzen\""))
    }

    @Test
    fun invalidSupplementCannotBeSerializedOrExecuted() {
        val broken = fixture().copy(state = "REVIEW_CONTEXT_ONLY")
        assertFailsWith<IllegalArgumentException> { HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.serializeSupplement(broken) }
        assertReason(
            execute(Files.createTempDirectory("him-direct-invalid-").toFile(), broken),
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.INVALID_SUPPLEMENT,
        )
    }

    @Test
    fun markdownRetainsAllRecordReferencesAndFieldDigests() {
        val supplement = fixture("line one\nline two")
        val markdown = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.renderMarkdown(supplement)
        supplement.bundles.flatMap { it.evidence }.forEach { evidence ->
            assertTrue(evidence.recordReference in markdown)
            assertTrue(evidence.evidenceReferenceId in markdown)
            evidence.fields.forEach { field ->
                assertTrue(field.fullValueSha256 in markdown)
                assertTrue(field.fullValue.lines().all { "    $it" in markdown })
            }
        }
    }

    @Test
    fun successfulPublicationLeavesNoTemporaryFiles() = withRoot { root ->
        execute(root, fixture())
        val directory = root.toPath().resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MISSION_DIRECTORY)
        assertTrue(Files.list(directory).use { stream -> stream.noneMatch { it.fileName.toString().endsWith(".tmp") } })
    }

    @Test
    fun existingMarkdownConflictPreservesJsonBytes() = withRoot { root ->
        execute(root, fixture())
        val directory = root.toPath().resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MISSION_DIRECTORY)
        val json = directory.resolve("direct-evidence-supplement.v1.json")
        val markdown = directory.resolve("direct-evidence-supplement.v1.md")
        val jsonBefore = Files.readAllBytes(json)
        Files.write(markdown, Files.readAllBytes(markdown) + byteArrayOf(1))
        assertReason(execute(root, fixture()), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.EXISTING_MARKDOWN_CONFLICT)
        assertTrue(jsonBefore.contentEquals(Files.readAllBytes(json)))
    }

    @Test
    fun resultSurfaceContainsNoDecisionOrIdentityMutationFields() {
        val forbidden = setOf("decision", "reviewer", "approval", "publication", "training", "mutation", "gold")
        val names = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Completed::class.java.declaredFields.map { it.name }
        assertTrue(names.none { name -> forbidden.any { token -> name.contains(token, ignoreCase = true) } })
    }

    @Test
    fun failureDiagnosticsAreTypedAndDoNotContainSecretsOrPaths() {
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Failed>(
            execute(Files.createTempDirectory("him-direct-failure-").toFile(), fixture().copy(supplementLogicalDigest = "0")),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SUPPLEMENT_LOGICAL_DIGEST_MISMATCH, failure.reason)
        assertFalse(failure.safeContext.contains('/'))
        assertFalse(failure.safeContext.contains("secret", ignoreCase = true))
    }

    @Test
    fun onlySyntheticRootsAreUsedAndRealOutputContractIsNotOpened() {
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.OUTPUT_ROOT.startsWith("build/"))
        assertFalse(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.OUTPUT_ROOT.startsWith('/'))
    }

    private fun fixture(fieldValue: String = "Artischocken Herzen") =
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(
            binding(),
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.FROZEN_REVIEW_UNITS.mapIndexed { index, unit ->
                val position = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
                val catalogPosition = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1(
                    unit,
                    packetBinding(index),
                    packetLogical(index),
                    listOf(
                        sourceCard(unit, sourceRecords[index], position, fieldValue),
                        targetCard(unit, catalogPosition),
                        authorityCard(unit),
                    ),
                    "",
                    "",
                )
            },
        )

    private val sourceRecords = listOf(
        "off:product:row:431650:code:4002239680509",
        "off:product:row:3272579:code:0061483010917",
        "off:product:row:1551407:code:4013200552046",
        "off:product:row:3322623:code:2026088009283",
    )

    private fun binding() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1(
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.CONTRACT_ID,
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.VERSION,
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.MISSION_ID,
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.SCOPE_ID,
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_SELECTION_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.CONTRACT_ID,
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.VERSION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_INPUT_BINDING_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_BINDING_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_LOGICAL_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1(
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-review-corpus/v1/review-corpus.v1.json",
            2359985,
            "4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016",
            "3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02",
        ),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CORPUS_BINDING_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_IMPLEMENTATION_HEAD,
    )

    private fun sourceArtifact() = artifact("data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", "1")
    private fun catalogArtifact() = artifact("data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json", "3")
    private fun authorityArtifact() = artifact("data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json", "5")

    private fun sourceCard(unit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1, record: String, position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1, value: String) = card(
        unit.reviewUnitId,
        HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION,
        position,
        sourceArtifact(),
        record,
        listOf(field("identity.productName", value), field("identity.productNameEnglish", if (value == "Artischocken Herzen") "Artichoke hearts" else value)),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1(HimGroundTruthSource.OPEN_FOOD_FACTS, HimEvidenceRecordKind.OFF_PRODUCT, sourceArtifact(), record, listOf(field("identity.productName", value), field("identity.productNameEnglish", if (value == "Artischocken Herzen") "Artichoke hearts" else value)), sourceArtifact()),
    )

    private fun targetCard(unit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1, position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1) = card(
        unit.reviewUnitId,
        HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD,
        position,
        catalogArtifact(),
        "catalog:${unit.canonicalEntityId}",
        listOf(field("canonical.name", if (unit.canonicalEntityId == "ZuhV5V") "Artischocken" else "Crème double")),
        null,
    )

    private fun authorityCard(unit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1) = card(
        unit.reviewUnitId,
        HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD,
        HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY,
        authorityArtifact(),
        "authority:${unit.canonicalEntityId}",
        listOf(field("canonical.authorityName", if (unit.canonicalEntityId == "ZuhV5V") "Artischocken" else "Crème double")),
        null,
    )

    private fun card(unitId: String, kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1, position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1, artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1, record: String, fields: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1>, projection: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1?) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(unitId, "", kind, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, position, artifact, record, fields, projection)
    private fun field(reference: String, value: String) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1(reference, value, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1.sha256(value))
    private fun artifact(path: String, digit: String) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1(path, 1, digit.repeat(64), (digit.toInt() + 1).toString().repeat(64))
    private fun packetBinding(index: Int) = listOf("a61bd239809b266d8a8ca36ef98b8b635966b4d0e6f6716468b77ef284829f29", "0fb192a227076e28204d841ad8573cc33e486fcfd8991678fa24f7727937fb35", "2fef9dcff87ceeebfe0ae5ca2f9996ab5e8115c13c9949bb6bb0a1b1ed3e5287", "1528b8f9dfd01272762f10e1f22a7f7f4ecad05cb1f3ee59493b6c2eb6d91722")[index]
    private fun packetLogical(index: Int) = listOf("50af5a0589bc19f9154d840091cb5ccbc7d7250b2fca5a3a9a1d05967905c418", "7b26d34c7335cd0f03ffaf49d72bd5edff36a2e9df465ce3251acb9e8839a57c", "ef5008317a70136fa95b4f025cd060544af249db579551cc996694ec9fa35ab0", "dd78dbf8d17943f1d6f7645903f305e33f571f3efb70d4dff8a46e77dfb92d89")[index]

    private fun execute(root: File, supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1, enabled: Boolean = true) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.execute(
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceRequestV1(enabled, root, supplement),
    )

    private fun completed(result: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1) = when (result) {
        is HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Completed -> result
        is HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Failed -> error("PERSISTENCE_RESULT=${result.reason}:${result.safeContext}")
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Disabled -> error("PERSISTENCE_RESULT=DISABLED")
    }
    private fun assertReason(result: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1, expected: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1) = assertEquals(expected, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Failed>(result).reason)
    private fun jsonText() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.serializeSupplement(fixture()).toString(Charsets.UTF_8)
    private fun assertFailure(json: String) = assertFailsWith<IllegalArgumentException> { HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.deserializeSupplement(json.toByteArray()) }

    private fun withRoot(block: (File) -> Unit) {
        val root = Files.createTempDirectory("him-direct-persistence-").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }

}
