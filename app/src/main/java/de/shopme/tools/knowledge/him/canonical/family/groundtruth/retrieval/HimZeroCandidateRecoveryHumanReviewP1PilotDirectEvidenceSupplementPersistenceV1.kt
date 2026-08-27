package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/** Deterministic persistence for the context-only direct-evidence supplement. */
object HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_PERSISTENCE_V1"
    const val VERSION = "1"
    const val OUTPUT_ROOT =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-direct-evidence-supplement/v1"
    const val MISSION_DIRECTORY = "p1-artischocken-herzen-brie-double-creme-v1"
    const val JSON_FILE_NAME = "direct-evidence-supplement.v1.json"
    const val MARKDOWN_FILE_NAME = "direct-evidence-supplement.v1.md"
    const val MARKDOWN_HEADER =
        "# HIM Zero-Candidate Recovery Human Review – P1 Pilot Direct Evidence Supplement V1"
    const val MARKDOWN_WARNING_BLOCK =
        "> DIRECT EVIDENCE CONTEXT ONLY\n" +
            "> NO HUMAN REVIEW DECISION RECORDED\n" +
            "> NO GOLD, TRAINING OR AUTHORITY EFFECT"

    private const val TEMP_PREFIX = ".direct-evidence-supplement.v1-"
    private const val TEMP_SUFFIX = ".tmp"
    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .create()

    fun serializeSupplement(
        supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
    ): ByteArray {
        requireValid(supplement)
        return try {
            (gson.toJson(supplement) + "\n").toByteArray(StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SERIALIZATION_FAILED)
        }
    }

    fun deserializeSupplement(
        bytes: ByteArray,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1 {
        return try {
            requireSingleTrailingLf(bytes)
            val text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes))
                .toString()
            rejectDuplicateKeys(bytes)
            val root = JsonParser.parseString(text)
            val objectValue = requireObject(root)
            validateJsonShape(objectValue)
            val supplement = requireNotNull(gson.fromJson(objectValue, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1::class.java))
            requireValid(supplement)
            supplement
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.DESERIALIZATION_FAILED, "json")
        }
    }

    fun readSupplement(file: File): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1 {
        val path = file.toPath()
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.READ_FAILED)
        }
        return try {
            deserializeSupplement(Files.readAllBytes(path))
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.READ_FAILED)
        }
    }

    fun renderMarkdown(
        supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
    ): String {
        requireValid(supplement)
        return try {
            buildString {
                appendLine(MARKDOWN_HEADER)
                appendLine()
                appendLine(MARKDOWN_WARNING_BLOCK)
                appendLine()
                appendCode("Contract ID", supplement.contractId)
                appendCode("Version", supplement.version)
                appendCode("State", supplement.state)
                appendLine()
                appendLine("## BINDINGS")
                appendLine()
                appendCode("Mission contract ID", supplement.binding.missionContractId)
                appendCode("Mission version", supplement.binding.missionVersion)
                appendCode("Mission ID", supplement.binding.missionId)
                appendCode("Scope ID", supplement.binding.scopeId)
                appendCode("Mission selection digest", supplement.binding.missionSelectionDigest)
                appendCode("Packet contract ID", supplement.binding.packetContractId)
                appendCode("Packet version", supplement.binding.packetVersion)
                appendCode("Packet input binding digest", supplement.binding.packetInputBindingDigest)
                appendCode("Packet binding digest", supplement.binding.packetBindingDigest)
                appendCode("Packet logical digest", supplement.binding.packetLogicalDigest)
                appendArtifact("Corpus artifact", supplement.binding.corpusArtifact)
                appendCode("Corpus binding digest", supplement.binding.corpusBindingDigest)
                appendCode("Packet implementation HEAD", supplement.binding.packetImplementationHead)
                appendLine()
                appendLine("## COUNTERS")
                appendLine()
                appendCounters(supplement.counters)
                supplement.bundles.forEachIndexed { index, bundle ->
                    appendLine()
                    appendLine("## REVIEW UNIT ${index + 1}")
                    appendLine()
                    appendCode("Stable entry ID", bundle.reviewUnit.stableEntryId)
                    appendCode("Review unit ID", bundle.reviewUnit.reviewUnitId)
                    appendCode("Canonical entity ID", bundle.reviewUnit.canonicalEntityId)
                    appendCode("Packet item binding digest", bundle.packetItemBindingDigest)
                    appendCode("Packet item logical digest", bundle.packetItemLogicalDigest)
                    appendCode("Unit binding digest", bundle.unitBindingDigest)
                    appendCode("Unit logical digest", bundle.unitLogicalDigest)
                    appendLine()
                    appendLine("### EVIDENCE")
                    bundle.evidence.forEachIndexed { evidenceIndex, evidence ->
                        appendLine()
                        appendLine("#### Evidence ${evidenceIndex + 1}")
                        appendCode("Evidence reference ID", evidence.evidenceReferenceId)
                        appendCode("Review unit ID", evidence.reviewUnitId)
                        appendCode("Evidence kind", evidence.kind.name)
                        appendCode("Directness", evidence.directness.name)
                        appendCode("Position", evidence.position.name)
                        appendArtifact("Artifact", evidence.artifact)
                        appendCode("Record reference", evidence.recordReference)
                        appendLine("Field references:")
                        evidence.fields.forEach { field ->
                            appendCode("  ${field.fieldReference} SHA-256", field.fullValueSha256)
                            appendCode("  ${field.fieldReference} fullValue", field.fullValue)
                        }
                        evidence.sourceProjection?.let { projection ->
                            appendLine("Source projection:")
                            appendCode("  Source", projection.source.name)
                            appendCode("  Record kind", projection.recordKind.name)
                            appendArtifact("  Source-origin artifact", projection.sourceOriginArtifact)
                            appendCode("  Projection record reference", projection.recordReference)
                            projection.fields.forEach { field ->
                                appendCode("  Projection ${field.fieldReference} SHA-256", field.fullValueSha256)
                                appendCode("  Projection ${field.fieldReference} fullValue", field.fullValue)
                            }
                        }
                    }
                }
                appendLine()
                appendLine("## SUPPLEMENT DIGESTS")
                appendLine()
                appendCode("Supplement binding digest", supplement.supplementBindingDigest)
                appendCode("Supplement logical digest", supplement.supplementLogicalDigest)
            }
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.MARKDOWN_RENDER_FAILED)
        }
    }

    fun execute(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1 {
        if (!request.enabled) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Disabled
        return try {
            if (request.supplement.binding.missionId != MISSION_DIRECTORY) {
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.INVALID_MISSION_ID, "mission")
            }
            requireValid(request.supplement)
            val paths = resolvePaths(request.outputRoot.toPath(), request.supplement.binding.missionId)
            val jsonBytes = serializeSupplement(request.supplement)
            val markdownBytes = renderMarkdown(request.supplement).toByteArray(StandardCharsets.UTF_8)
            publishOrReuse(paths, request.supplement, jsonBytes, markdownBytes)
        } catch (failure: PersistenceFailure) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Failed(failure.reason, failure.safeContext)
        } catch (_: Throwable) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Failed(
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.WRITE_FAILED,
                "persistence",
            )
        }
    }

    private fun publishOrReuse(
        paths: ResolvedPaths,
        supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
        jsonBytes: ByteArray,
        markdownBytes: ByteArray,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Completed {
        rejectTargetSymlinks(paths)
        val jsonExists = Files.exists(paths.json, LinkOption.NOFOLLOW_LINKS)
        val markdownExists = Files.exists(paths.markdown, LinkOption.NOFOLLOW_LINKS)
        if (jsonExists != markdownExists) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.PARTIAL_ARTIFACT_STATE, "pair")
        }
        if (jsonExists) {
            val existingJson = readFile(paths.json)
            if (!existingJson.contentEquals(jsonBytes)) {
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.EXISTING_JSON_CONFLICT, "json")
            }
            val existingMarkdown = readFile(paths.markdown)
            if (!existingMarkdown.contentEquals(markdownBytes)) {
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.EXISTING_MARKDOWN_CONFLICT, "markdown")
            }
            val reloaded = deserializeSupplement(existingJson)
            if (reloaded != supplement) {
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.RELOAD_MISMATCH, "supplement")
            }
            return completed(
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
                reloaded,
                paths,
                existingJson,
                existingMarkdown,
            )
        }
        try {
            Files.createDirectories(paths.directory)
            if (hasSymlinkComponentWithin(paths.directory, paths.root) || Files.isSymbolicLink(paths.directory)) {
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SYMLINK_REJECTED, "directory")
            }
            rejectTargetSymlinks(paths)
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.WRITE_FAILED, "directory")
        }
        val temporaryJson = try {
            Files.createTempFile(paths.directory, TEMP_PREFIX, TEMP_SUFFIX)
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.TEMPORARY_PUBLICATION_FAILED, "json")
        }
        val temporaryMarkdown = try {
            Files.createTempFile(paths.directory, TEMP_PREFIX, TEMP_SUFFIX)
        } catch (_: Throwable) {
            deleteQuietly(temporaryJson)
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.TEMPORARY_PUBLICATION_FAILED, "markdown")
        }
        var jsonPublished = false
        try {
            writeAndSync(temporaryJson, jsonBytes)
            writeAndSync(temporaryMarkdown, markdownBytes)
            moveNoReplace(temporaryJson, paths.json)
            jsonPublished = true
            moveNoReplace(temporaryMarkdown, paths.markdown)
        } catch (_: Throwable) {
            deleteQuietly(temporaryJson)
            deleteQuietly(temporaryMarkdown)
            if (jsonPublished) {
                try {
                    if (Files.isRegularFile(paths.json, LinkOption.NOFOLLOW_LINKS) && readFile(paths.json).contentEquals(jsonBytes)) {
                        Files.deleteIfExists(paths.json)
                    }
                } catch (_: Throwable) {
                    throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.ROLLBACK_FAILED, "json")
                }
            }
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.ATOMIC_PUBLICATION_FAILED, "pair")
        }
        val persistedJson = readFile(paths.json)
        val persistedMarkdown = readFile(paths.markdown)
        if (!persistedJson.contentEquals(jsonBytes)) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.JSON_BYTE_MISMATCH, "json")
        }
        if (!persistedMarkdown.contentEquals(markdownBytes)) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.MARKDOWN_BYTE_MISMATCH, "markdown")
        }
        if (sha256(persistedJson) != sha256(jsonBytes) || sha256(persistedMarkdown) != sha256(markdownBytes)) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SHA256_MISMATCH, "bytes")
        }
        val reloaded = try {
            deserializeSupplement(persistedJson)
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.RELOAD_MISMATCH, "supplement")
        }
        if (reloaded != supplement) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.RELOAD_MISMATCH, "supplement")
        }
        return completed(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1.CREATED,
            reloaded,
            paths,
            persistedJson,
            persistedMarkdown,
        )
    }

    private fun completed(
        status: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1,
        supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
        paths: ResolvedPaths,
        json: ByteArray,
        markdown: ByteArray,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Completed(
        status = status,
        supplement = supplement,
        jsonPath = paths.jsonRelative,
        markdownPath = paths.markdownRelative,
        jsonByteSize = json.size.toLong(),
        markdownByteSize = markdown.size.toLong(),
        jsonSha256 = sha256(json),
        markdownSha256 = sha256(markdown),
        supplementBindingDigest = supplement.supplementBindingDigest,
        supplementLogicalDigest = supplement.supplementLogicalDigest,
    )

    private fun resolvePaths(outputRoot: Path, missionId: String): ResolvedPaths {
        if (missionId != MISSION_DIRECTORY) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.INVALID_MISSION_ID, "mission")
        }
        if (outputRoot.toString().isBlank()) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.INVALID_OUTPUT_ROOT, "root")
        }
        val root = outputRoot.toAbsolutePath().normalize()
        val normalizedText = root.toString().replace(File.separatorChar, '/')
        val protectedSegments = setOf(
            "p1-pilot-review-packet",
            "review-corpus",
            "decision-batches",
            "canonical-food-catalog",
            "canonical-family-authority",
        )
        if (normalizedText.split('/').any { it in protectedSegments }) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.FORBIDDEN_REAL_OUTPUT_ACCESS, "root")
        }
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.INVALID_OUTPUT_ROOT, "root")
        }
        if (Files.isSymbolicLink(root)) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SYMLINK_REJECTED, "root")
        }
        val directory = root.resolve(MISSION_DIRECTORY).normalize()
        if (!directory.startsWith(root) || directory == root) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.UNSAFE_TARGET_PATH, "mission")
        }
        val json = directory.resolve(JSON_FILE_NAME).normalize()
        val markdown = directory.resolve(MARKDOWN_FILE_NAME).normalize()
        if (!json.startsWith(directory) || !markdown.startsWith(directory)) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.UNSAFE_TARGET_PATH, "target")
        }
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(directory)) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SYMLINK_REJECTED, "directory")
        }
        if (hasSymlinkComponentWithin(directory, root)) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SYMLINK_REJECTED, "directory")
        }
        return ResolvedPaths(root, directory, json, markdown, "$MISSION_DIRECTORY/$JSON_FILE_NAME", "$MISSION_DIRECTORY/$MARKDOWN_FILE_NAME")
    }

    private fun hasSymlinkComponentWithin(path: Path, boundary: Path): Boolean {
        var current: Path? = path
        while (current != null) {
            if (Files.isSymbolicLink(current)) return true
            if (current == boundary) return false
            current = current.parent
        }
        return false
    }

    private fun rejectTargetSymlinks(paths: ResolvedPaths) {
        if (Files.isSymbolicLink(paths.root) || Files.isSymbolicLink(paths.directory) ||
            Files.isSymbolicLink(paths.json) || Files.isSymbolicLink(paths.markdown)
        ) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SYMLINK_REJECTED, "target")
        }
    }

    private fun moveNoReplace(source: Path, target: Path) {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.ATOMIC_PUBLICATION_FAILED, "target")
        }
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target)
        }
    }

    private fun writeAndSync(path: Path, bytes: ByteArray) {
        try {
            FileOutputStream(path.toFile(), false).use { output ->
                output.write(bytes)
                output.flush()
                output.fd.sync()
            }
        } catch (_: Throwable) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.WRITE_FAILED, "temporary")
        }
    }

    private fun readFile(path: Path): ByteArray = try {
        Files.readAllBytes(path)
    } catch (_: Throwable) {
        throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.READ_FAILED, "file")
    }

    private fun deleteQuietly(path: Path) {
        try {
            Files.deleteIfExists(path)
        } catch (_: Throwable) {
            // The publication path reports the primary failure; no pre-existing file is touched here.
        }
    }

    private fun requireValid(supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1) {
        when (val result = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.validate(supplement)) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Invalid -> {
                val reason = when (result.reason) {
                    HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.SUPPLEMENT_BINDING_DIGEST_MISMATCH -> HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SUPPLEMENT_BINDING_DIGEST_MISMATCH
                    HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.SUPPLEMENT_LOGICAL_DIGEST_MISMATCH -> HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SUPPLEMENT_LOGICAL_DIGEST_MISMATCH
                    HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_COUNTERS -> HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.INVALID_COUNTERS
                    HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_MISSION_BINDING,
                    HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_PACKET_BINDING,
                    HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_CORPUS_BINDING,
                    HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_IMPLEMENTATION_HEAD,
                    -> HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.INVALID_CONTRACT_BINDING
                    else -> HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.INVALID_SUPPLEMENT
                }
                fail(reason, "supplement")
            }
        }
    }

    private fun fail(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1,
        safeContext: String,
    ): Nothing = throw PersistenceFailure(reason, safeContext)

    private fun requireSingleTrailingLf(bytes: ByteArray) {
        if (bytes.isEmpty() || bytes.last() != '\n'.code.toByte() || bytes.dropLast(1).lastOrNull() == '\n'.code.toByte()) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.DESERIALIZATION_FAILED, "line-ending")
        }
    }

    private fun rejectDuplicateKeys(bytes: ByteArray) {
        try {
            val reader = JsonReader(InputStreamReader(ByteArrayInputStream(bytes), StandardCharsets.UTF_8))
            reader.isLenient = false
            consume(reader)
            if (reader.peek() != JsonToken.END_DOCUMENT) throw IllegalArgumentException()
        } catch (_: PersistenceFailure) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.DESERIALIZATION_FAILED, "json")
        } catch (_: Throwable) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.DESERIALIZATION_FAILED, "json")
        }
    }

    private fun consume(reader: JsonReader) {
        when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                val names = mutableSetOf<String>()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (!names.add(name)) throw IllegalArgumentException()
                    consume(reader)
                }
                reader.endObject()
            }
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.hasNext()) consume(reader)
                reader.endArray()
            }
            JsonToken.STRING, JsonToken.NUMBER -> reader.nextString()
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NULL -> reader.nextNull()
            else -> throw IllegalArgumentException()
        }
    }

    private fun validateJsonShape(root: JsonObject) {
        requireKeys(root, ROOT_KEYS)
        requireString(root, "contractId")
        requireString(root, "version")
        requireString(root, "state")
        validateBinding(requireObject(root["binding"]))
        requireArray(root["bundles"]).forEach { validateBundle(requireObject(it)) }
        validateCounters(requireObject(root["counters"]))
        requireString(root, "supplementBindingDigest")
        requireString(root, "supplementLogicalDigest")
    }

    private fun validateBinding(value: JsonObject) {
        requireKeys(value, BINDING_KEYS)
        BINDING_KEYS.filterNot { it == "corpusArtifact" }.forEach { requireString(value, it) }
        validateArtifact(requireObject(value["corpusArtifact"]))
    }

    private fun validateBundle(value: JsonObject) {
        requireKeys(value, BUNDLE_KEYS)
        validateReviewUnit(requireObject(value["reviewUnit"]))
        requireString(value, "packetItemBindingDigest")
        requireString(value, "packetItemLogicalDigest")
        requireArray(value["evidence"]).forEach { validateCard(requireObject(it)) }
        requireString(value, "unitBindingDigest")
        requireString(value, "unitLogicalDigest")
    }

    private fun validateReviewUnit(value: JsonObject) {
        requireKeys(value, REVIEW_UNIT_KEYS)
        requireString(value, "stableEntryId")
        requireString(value, "canonicalEntityId")
    }

    private fun validateCard(value: JsonObject) {
        requireKeys(value, CARD_KEYS)
        requireString(value, "reviewUnitId")
        requireString(value, "evidenceReferenceId")
        requireEnum(value, "kind", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.entries.map { it.name }.toSet())
        requireEnum(value, "directness", HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.entries.map { it.name }.toSet())
        requireEnum(value, "position", HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.entries.map { it.name }.toSet())
        validateArtifact(requireObject(value["artifact"]))
        requireString(value, "recordReference")
        requireArray(value["fields"]).forEach { validateField(requireObject(it)) }
        val projection = value["sourceProjection"]
        if (!projection.isJsonNull) validateProjection(requireObject(projection))
    }

    private fun validateProjection(value: JsonObject) {
        requireKeys(value, PROJECTION_KEYS)
        requireEnum(value, "source", HimGroundTruthSource.values().map { it.name }.toSet())
        requireEnum(value, "recordKind", HimEvidenceRecordKind.entries.map { it.name }.toSet())
        validateArtifact(requireObject(value["artifact"]))
        requireString(value, "recordReference")
        requireArray(value["fields"]).forEach { validateField(requireObject(it)) }
        validateArtifact(requireObject(value["sourceOriginArtifact"]))
    }

    private fun validateArtifact(value: JsonObject) {
        requireKeys(value, ARTIFACT_KEYS)
        requireString(value, "relativePath")
        requireInteger(value, "byteSize")
        requireString(value, "sha256")
        requireNullableString(value, "logicalDigest")
    }

    private fun validateField(value: JsonObject) {
        requireKeys(value, FIELD_KEYS)
        requireString(value, "fieldReference")
        requireString(value, "fullValue")
        requireString(value, "fullValueSha256")
    }

    private fun validateCounters(value: JsonObject) {
        requireKeys(value, COUNTER_KEYS)
        COUNTER_SCALAR_KEYS.forEach { requireInteger(value, it) }
    }

    private fun requireObject(value: JsonElement?): JsonObject =
        if (value != null && value.isJsonObject) value.asJsonObject else throw IllegalArgumentException()

    private fun requireArray(value: JsonElement?): List<JsonElement> =
        if (value != null && value.isJsonArray) value.asJsonArray.toList() else throw IllegalArgumentException()

    private fun requireKeys(value: JsonObject, allowed: Set<String>) {
        if (value.keySet() != allowed) {
            if ((value.keySet() - allowed).isNotEmpty()) {
                throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.UNKNOWN_JSON_FIELD, "json")
            }
            throw PersistenceFailure(
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.DESERIALIZATION_FAILED,
                "schema:${value.keySet().sorted().joinToString(",")}",
            )
        }
    }

    private fun requireString(value: JsonObject, key: String) {
        val element = value[key]
        if (element == null || !element.isJsonPrimitive || !element.asJsonPrimitive.isString) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.DESERIALIZATION_FAILED, "type:$key")
        }
    }

    private fun requireNullableString(value: JsonObject, key: String) {
        val element = value[key]
        if (element == null || (!element.isJsonNull && (!element.isJsonPrimitive || !element.asJsonPrimitive.isString))) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.DESERIALIZATION_FAILED, "type:$key")
        }
    }

    private fun requireInteger(value: JsonObject, key: String) {
        val element = value[key]
        if (element == null || !element.isJsonPrimitive || !element.asJsonPrimitive.isNumber || element.asString.toLongOrNull() == null) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.DESERIALIZATION_FAILED, "type:$key")
        }
    }

    private fun requireEnum(value: JsonObject, key: String, allowed: Set<String>) {
        requireString(value, key)
        if (value[key].asString !in allowed) throw IllegalArgumentException()
    }

    private fun StringBuilder.appendCounters(
        counters: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCountersV1,
    ) {
        appendCode("Review units", counters.reviewUnits.toString())
        appendCode("Source evidence projections", counters.sourceEvidenceProjections.toString())
        appendCode("Catalog target evidence records", counters.catalogTargetEvidenceRecords.toString())
        appendCode("Authority target evidence records", counters.authorityTargetEvidenceRecords.toString())
        appendCode("Direct evidence references", counters.directEvidenceReferences.toString())
        appendCode("Supporting evidence references", counters.supportingEvidenceReferences.toString())
        appendCode("Contradicting evidence references", counters.contradictingEvidenceReferences.toString())
        appendCode("Context-only evidence references", counters.contextOnlyEvidenceReferences.toString())
        appendCode("Distinct source records", counters.distinctSourceRecords.toString())
        appendCode("Distinct canonical targets", counters.distinctCanonicalTargets.toString())
    }

    private fun StringBuilder.appendArtifact(
        label: String,
        artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
    ) {
        appendCode("$label relative path", artifact.relativePath)
        appendCode("$label byte size", artifact.byteSize.toString())
        appendCode("$label SHA-256", artifact.sha256)
        appendCode("$label logical digest", artifact.logicalDigest ?: "(none)")
    }

    private fun StringBuilder.appendCode(label: String, value: String) {
        appendLine("$label:")
        value.replace("\r\n", "\n").replace('\r', '\n').split('\n').forEach { appendLine("    $it") }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private data class ResolvedPaths(
        val root: Path,
        val directory: Path,
        val json: Path,
        val markdown: Path,
        val jsonRelative: String,
        val markdownRelative: String,
    )

    private class PersistenceFailure(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1,
        val safeContext: String = "persistence",
    ) : IllegalArgumentException("$reason:$safeContext")

    private val ROOT_KEYS = setOf("contractId", "version", "state", "binding", "bundles", "counters", "supplementBindingDigest", "supplementLogicalDigest")
    private val BINDING_KEYS = setOf("missionContractId", "missionVersion", "missionId", "scopeId", "missionSelectionDigest", "packetContractId", "packetVersion", "packetInputBindingDigest", "packetBindingDigest", "packetLogicalDigest", "corpusArtifact", "corpusBindingDigest", "packetImplementationHead")
    private val ARTIFACT_KEYS = setOf("relativePath", "byteSize", "sha256", "logicalDigest")
    private val REVIEW_UNIT_KEYS = setOf("stableEntryId", "canonicalEntityId")
    private val BUNDLE_KEYS = setOf("reviewUnit", "packetItemBindingDigest", "packetItemLogicalDigest", "evidence", "unitBindingDigest", "unitLogicalDigest")
    private val CARD_KEYS = setOf("reviewUnitId", "evidenceReferenceId", "kind", "directness", "position", "artifact", "recordReference", "fields", "sourceProjection")
    private val PROJECTION_KEYS = setOf("source", "recordKind", "artifact", "recordReference", "fields", "sourceOriginArtifact")
    private val FIELD_KEYS = setOf("fieldReference", "fullValue", "fullValueSha256")
    private val COUNTER_KEYS = setOf("reviewUnits", "sourceEvidenceProjections", "catalogTargetEvidenceRecords", "authorityTargetEvidenceRecords", "directEvidenceReferences", "supportingEvidenceReferences", "contradictingEvidenceReferences", "contextOnlyEvidenceReferences", "distinctSourceRecords", "distinctCanonicalTargets")
    private val COUNTER_SCALAR_KEYS = COUNTER_KEYS
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1 {
    CREATED,
    ALREADY_PRESENT_IDENTICAL,
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1 {
    INVALID_REQUEST,
    INVALID_OUTPUT_ROOT,
    INVALID_MISSION_ID,
    UNSAFE_TARGET_PATH,
    INVALID_SUPPLEMENT,
    INVALID_CONTRACT_BINDING,
    INVALID_COUNTERS,
    SUPPLEMENT_BINDING_DIGEST_MISMATCH,
    SUPPLEMENT_LOGICAL_DIGEST_MISMATCH,
    SERIALIZATION_FAILED,
    DESERIALIZATION_FAILED,
    UNKNOWN_JSON_FIELD,
    READ_FAILED,
    WRITE_FAILED,
    PARTIAL_ARTIFACT_STATE,
    EXISTING_JSON_CONFLICT,
    EXISTING_MARKDOWN_CONFLICT,
    TEMPORARY_PUBLICATION_FAILED,
    ATOMIC_PUBLICATION_FAILED,
    ROLLBACK_FAILED,
    RELOAD_MISMATCH,
    JSON_BYTE_MISMATCH,
    MARKDOWN_BYTE_MISMATCH,
    SHA256_MISMATCH,
    SYMLINK_REJECTED,
    FORBIDDEN_REAL_OUTPUT_ACCESS,
    MARKDOWN_RENDER_FAILED,
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1 {
    data object Disabled : HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1

    data class Completed(
        val status: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1,
        val supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
        val jsonPath: String,
        val markdownPath: String,
        val jsonByteSize: Long,
        val markdownByteSize: Long,
        val jsonSha256: String,
        val markdownSha256: String,
        val supplementBindingDigest: String,
        val supplementLogicalDigest: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceRequestV1(
    val enabled: Boolean,
    val outputRoot: File,
    val supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
)
