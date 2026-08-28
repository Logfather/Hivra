package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/** Deterministic, context-only persistence for the independent validation packet. */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PACKET_PERSISTENCE_V1"
    const val VERSION = "1"
    const val OUTPUT_ROOT =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-decision-validation-packet/v1"
    const val JSON_FILE_NAME = "validation-packet.v1.json"
    const val MARKDOWN_FILE_NAME = "validation-packet.v1.md"
    const val MARKDOWN_HEADER =
        "# HIM Zero-Candidate Recovery Human Review – P1 Pilot Decision Validation Packet V1"
    const val VALIDATION_STATE = "INDEPENDENT_VALIDATION_CONTEXT_ONLY"

    private const val TEMP_JSON_FILE_NAME = ".validation-packet.v1.json.tmp"
    private const val TEMP_MARKDOWN_FILE_NAME = ".validation-packet.v1.md.tmp"
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()
    private val SAFE_PACKET_ID = Regex("[a-z0-9][a-z0-9-]{0,127}")
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun serializePacket(
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
    ): ByteArray {
        requireValid(packet)
        return try {
            canonicalJson(packet).toByteArray(StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            fail(PersistenceFailureReason.SERIALIZATION_FAILED, "packet")
        }
    }

    fun deserializePacket(bytes: ByteArray): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1 {
        return try {
            requireSingleFinalLf(bytes)
            val root = parseStrictJson(bytes).takeIf { it.isJsonObject }
                ?: fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "root")
            val packet = gson.fromJson(root, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1::class.java)
                ?: fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "packet")
            val canonical = JsonParser.parseString(canonicalJson(packet))
            if (canonical != root) fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "fields")
            requireValid(packet)
            packet
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "packet")
        }
    }

    fun readPacket(file: File): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1 {
        if (!file.isFile || Files.isSymbolicLink(file.toPath())) {
            fail(PersistenceFailureReason.READ_FAILED, "packet")
        }
        return try {
            deserializePacket(file.readBytes())
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            fail(PersistenceFailureReason.READ_FAILED, "packet")
        }
    }

    fun validatePacket(
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1 =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.validate(packet)

    fun renderMarkdown(
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
    ): String {
        requireValid(packet)
        return try {
            buildString {
                appendLine(MARKDOWN_HEADER)
                appendLine()
                appendLine("> INDEPENDENT VALIDATION CONTEXT ONLY")
                appendLine("> NO INDEPENDENT VALIDATION HAS BEEN PERFORMED")
                appendLine("> NO GOLD, NEGATIVE SUPERVISION, TRAINING, PUBLICATION, OR AUTHORITY EFFECT")
                appendLine("> NO VALIDATION ASSESSMENT OR DOWNSTREAM ROUTE IS SELECTED")
                appendLine()
                appendLine("Persistence contract ID: ${packetPersistenceContractId()}")
                appendLine("Persistence version: $VERSION")
                appendLine("Packet contract ID: ${packet.contractId}")
                appendLine("Packet version: ${packet.version}")
                appendLine("Packet state: ${packet.state}")
                appendLine("Packet ID: ${safeInline(packet.packetId)}")
                appendLine()
                appendLine("## INPUT BINDINGS")
                appendLine()
                appendLine("Decision batch ID: ${safeInline(packet.inputBinding.decisionBatchId)}")
                appendFileBinding("Decision batch", packet.inputBinding.decisionBatch)
                appendLine("Original input binding digest: ${packet.inputBinding.originalInputBindingDigest}")
                appendLine("Original batch logical digest: ${packet.inputBinding.originalBatchLogicalDigest}")
                appendLine("Original reviewer reference: ${safeInline(packet.inputBinding.originalReviewerRef)}")
                appendLine("Original review round: ${packet.inputBinding.originalReviewRound}")
                appendLine("Original revision: ${packet.inputBinding.originalRevision}")
                appendFileBinding("Review packet JSON", packet.inputBinding.reviewPacketJson)
                appendFileBinding("Review packet Markdown", packet.inputBinding.reviewPacketMarkdown)
                appendLine("Review packet input binding digest: ${packet.inputBinding.reviewPacketInputBindingDigest}")
                appendLine("Review packet binding digest: ${packet.inputBinding.reviewPacketBindingDigest}")
                appendLine("Review packet logical digest: ${packet.inputBinding.reviewPacketLogicalDigest}")
                appendFileBinding("Direct-evidence supplement JSON", packet.inputBinding.supplementJson)
                appendFileBinding("Direct-evidence supplement Markdown", packet.inputBinding.supplementMarkdown)
                appendLine("Supplement binding digest: ${packet.inputBinding.supplementBindingDigest}")
                appendLine("Supplement logical digest: ${packet.inputBinding.supplementLogicalDigest}")
                appendFileBinding("Review corpus", packet.inputBinding.corpus)
                appendLine("Corpus logical digest: ${packet.inputBinding.corpusLogicalDigest}")
                appendLine("Corpus binding digest: ${packet.inputBinding.corpusBindingDigest}")
                appendLine("Contract baseline HEAD: ${packet.inputBinding.contractBaselineHead}")
                appendLine()
                appendLine("## CONTEXT LIMITATIONS")
                packet.contextLimitations.forEach { appendLine("- ${it.name}") }
                appendLine()
                appendLine("## FOUR FROZEN UNITS")
                packet.items.forEachIndexed { index, item ->
                    appendLine()
                    appendLine("### Unit ${index + 1}")
                    appendLine("Review unit ID: ${safeInline(item.reviewUnitId)}")
                    appendLine("Stable entry ID: ${safeInline(item.stableEntryId)}")
                    appendLine("Canonical entity ID: ${safeInline(item.canonicalEntityId)}")
                    appendLine("Canonical label: ${safeInline(item.canonicalLabel)}")
                    appendLine("Original decision: ${item.originalDecision.name}")
                    appendLine("Original reason codes: ${item.originalReasonCodes.joinToString(", ") { it.name }}")
                    appendLine("Original evidence references: ${item.originalEvidenceReferenceIds.joinToString(", ")}")
                    appendLine("Original rationale SHA-256: ${item.originalRationaleSha256}")
                    appendLine("Alternative canonical proposal: (none)")
                    appendLine()
                    appendLine("#### SOURCE EVIDENCE")
                    appendEvidenceCards(this, item, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION)
                    appendLine()
                    appendLine("#### CANONICAL CATALOG EVIDENCE")
                    appendEvidenceCards(this, item, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD)
                    appendLine()
                    appendLine("#### CANONICAL FAMILY AUTHORITY EVIDENCE")
                    appendEvidenceCards(this, item, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD)
                    appendLine()
                    appendLine("#### ORIGINAL FIELDS")
                    item.originalFields.forEach { field ->
                        appendLine("Field: ${safeInline(field.fieldName)}")
                        appendLine("Value: ${safeInline(field.originalValue)}")
                        appendLine("Value SHA-256: ${field.originalValueSha256}")
                        appendLine("Display state: ${field.displayState.name}")
                        appendLine("Character count: ${field.originalCharacterCount}")
                    }
                    appendLine()
                    appendLine("#### TARGET CONTEXT")
                    appendLine("Registry resolution: ${item.targetContext.registryResolution.name}")
                    appendLine("Authority resolution: ${item.targetContext.authorityResolution.name}")
                    appendLine("Catalog record reference: ${safeInline(item.targetContext.catalogRecordReference)}")
                    appendLine("Authority record reference: ${safeInline(item.targetContext.authorityRecordReference)}")
                    appendLine("Identity terms: ${renderList(item.targetContext.identityTerms)}")
                    appendLine("Alias terms: ${renderList(item.targetContext.aliasTerms)}")
                    appendLine("Target-context digest: ${item.targetContext.targetContextDigest}")
                    appendLine()
                    appendLine("Item binding digest: ${item.itemBindingDigest}")
                    appendLine("Item logical digest: ${item.itemLogicalDigest}")
                }
                appendLine()
                appendLine("## COUNTERS")
                appendCounters(packet.counters)
                appendLine()
                appendLine("## PACKET DIGESTS")
                appendLine("Packet binding digest: ${packet.packetBindingDigest}")
                appendLine("Packet logical digest: ${packet.packetLogicalDigest}")
            }
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            fail(PersistenceFailureReason.MARKDOWN_RENDER_FAILED, "markdown")
        }
    }

    fun execute(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1 {
        if (!request.enabled) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Disabled
        return try {
            if (!SAFE_PACKET_ID.matches(request.packet.packetId)) fail(PersistenceFailureReason.INVALID_PACKET_ID, "packetId")
            requireValid(request.packet)
            val paths = resolvePaths(request.outputRoot.toPath(), request.packet.packetId)
            val jsonBytes = serializePacket(request.packet)
            val markdownBytes = renderMarkdown(request.packet).toByteArray(StandardCharsets.UTF_8)
            publishOrReuse(paths, request.packet, jsonBytes, markdownBytes)
        } catch (failure: PersistenceFailure) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Failed(
                failure.reason,
                failure.safeContext,
            )
        } catch (_: Throwable) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Failed(
                PersistenceFailureReason.WRITE_FAILED,
                "persistence",
            )
        }
    }

    private fun publishOrReuse(
        paths: ResolvedPaths,
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
        jsonBytes: ByteArray,
        markdownBytes: ByteArray,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Completed {
        inspectDirectory(paths.directory, paths)
        val jsonExists = Files.exists(paths.json, LinkOption.NOFOLLOW_LINKS)
        val markdownExists = Files.exists(paths.markdown, LinkOption.NOFOLLOW_LINKS)
        if (jsonExists != markdownExists) fail(PersistenceFailureReason.PARTIAL_PACKET_ARTIFACT_STATE, "pair")
        if (jsonExists) {
            val existingJson = readBytes(paths.json, "json")
            if (!existingJson.contentEquals(jsonBytes)) fail(PersistenceFailureReason.EXISTING_JSON_CONFLICT, "json")
            val existingMarkdown = readBytes(paths.markdown, "markdown")
            if (!existingMarkdown.contentEquals(markdownBytes)) fail(PersistenceFailureReason.EXISTING_MARKDOWN_CONFLICT, "markdown")
            val reloaded = try {
                deserializePacket(existingJson)
            } catch (_: Throwable) {
                fail(PersistenceFailureReason.RELOAD_MISMATCH, "reload")
            }
            if (reloaded != packet || !renderMarkdown(reloaded).toByteArray(StandardCharsets.UTF_8).contentEquals(existingMarkdown)) {
                fail(PersistenceFailureReason.RELOAD_MISMATCH, "reload")
            }
            return completed(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
                reloaded,
                paths,
                existingJson,
                existingMarkdown,
            )
        }
        var tempJsonCreated = false
        var tempMarkdownCreated = false
        var jsonPublished = false
        var markdownPublished = false
        try {
            Files.createDirectories(paths.directory)
            if (Files.isSymbolicLink(paths.directory)) fail(PersistenceFailureReason.SYMLINK_ESCAPE, "directory")
            inspectDirectory(paths.directory, paths)
            if (Files.exists(paths.tempJson, LinkOption.NOFOLLOW_LINKS) ||
                Files.exists(paths.tempMarkdown, LinkOption.NOFOLLOW_LINKS)
            ) {
                fail(PersistenceFailureReason.ATOMIC_PUBLICATION_FAILED, "temporary")
            }
            writeNew(paths.tempJson, jsonBytes)
            tempJsonCreated = true
            writeNew(paths.tempMarkdown, markdownBytes)
            tempMarkdownCreated = true
            moveNoReplace(paths.tempJson, paths.json)
            jsonPublished = true
            moveNoReplace(paths.tempMarkdown, paths.markdown)
            markdownPublished = true
        } catch (failure: PersistenceFailure) {
            cleanupPublished(paths, jsonBytes, markdownBytes, tempJsonCreated, tempMarkdownCreated, jsonPublished, markdownPublished)
            throw failure
        } catch (_: Throwable) {
            cleanupPublished(paths, jsonBytes, markdownBytes, tempJsonCreated, tempMarkdownCreated, jsonPublished, markdownPublished)
            fail(PersistenceFailureReason.ATOMIC_PUBLICATION_FAILED, "publish")
        }
        try {
            val persistedJson = readBytes(paths.json, "json")
            if (!persistedJson.contentEquals(jsonBytes)) fail(PersistenceFailureReason.JSON_BYTE_MISMATCH, "json")
            val persistedMarkdown = readBytes(paths.markdown, "markdown")
            if (!persistedMarkdown.contentEquals(markdownBytes)) fail(PersistenceFailureReason.MARKDOWN_BYTE_MISMATCH, "markdown")
            val reloaded = try {
                deserializePacket(persistedJson)
            } catch (_: Throwable) {
                fail(PersistenceFailureReason.RELOAD_MISMATCH, "reload")
            }
            if (reloaded != packet) fail(PersistenceFailureReason.RELOAD_MISMATCH, "reload")
            if (reloaded.packetBindingDigest != packet.packetBindingDigest) fail(PersistenceFailureReason.BINDING_DIGEST_MISMATCH, "binding")
            if (reloaded.packetLogicalDigest != packet.packetLogicalDigest) fail(PersistenceFailureReason.LOGICAL_DIGEST_MISMATCH, "logical")
            if (!renderMarkdown(reloaded).toByteArray(StandardCharsets.UTF_8).contentEquals(persistedMarkdown)) {
                fail(PersistenceFailureReason.RELOAD_MISMATCH, "markdown")
            }
            return completed(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1.CREATED,
                reloaded,
                paths,
                persistedJson,
                persistedMarkdown,
            )
        } catch (failure: PersistenceFailure) {
            cleanupPublished(paths, jsonBytes, markdownBytes, false, false, jsonPublished, markdownPublished)
            throw failure
        } catch (_: Throwable) {
            cleanupPublished(paths, jsonBytes, markdownBytes, false, false, jsonPublished, markdownPublished)
            fail(PersistenceFailureReason.RELOAD_MISMATCH, "reload")
        }
    }

    private fun completed(
        status: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1,
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
        paths: ResolvedPaths,
        json: ByteArray,
        markdown: ByteArray,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1.Completed(
        status = status,
        packet = packet,
        jsonPath = paths.jsonRelative,
        markdownPath = paths.markdownRelative,
        jsonByteSize = json.size.toLong(),
        markdownByteSize = markdown.size.toLong(),
        jsonSha256 = sha256(json),
        markdownSha256 = sha256(markdown),
        packetBindingDigest = packet.packetBindingDigest,
        packetLogicalDigest = packet.packetLogicalDigest,
    )

    private fun resolvePaths(outputRoot: Path, packetId: String): ResolvedPaths {
        if (!SAFE_PACKET_ID.matches(packetId)) fail(PersistenceFailureReason.INVALID_PACKET_ID, "packetId")
        if (outputRoot.toString().isBlank()) fail(PersistenceFailureReason.INVALID_OUTPUT_ROOT, "root")
        val root = try { outputRoot.toAbsolutePath().normalize() } catch (_: Throwable) {
            fail(PersistenceFailureReason.INVALID_OUTPUT_ROOT, "root")
        }
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(root)) {
            fail(PersistenceFailureReason.SYMLINK_ESCAPE, "root")
        }
        val directory = root.resolve(packetId).normalize()
        if (!directory.startsWith(root) || directory == root || hasSymlinkComponentWithin(directory, root)) {
            fail(PersistenceFailureReason.UNSAFE_TARGET_PATH, "target")
        }
        val json = directory.resolve(JSON_FILE_NAME).normalize()
        val markdown = directory.resolve(MARKDOWN_FILE_NAME).normalize()
        if (!json.startsWith(directory) || !markdown.startsWith(directory)) {
            fail(PersistenceFailureReason.UNSAFE_TARGET_PATH, "target")
        }
        return ResolvedPaths(
            directory = directory,
            json = json,
            markdown = markdown,
            tempJson = directory.resolve(TEMP_JSON_FILE_NAME),
            tempMarkdown = directory.resolve(TEMP_MARKDOWN_FILE_NAME),
            jsonRelative = "$packetId/$JSON_FILE_NAME",
            markdownRelative = "$packetId/$MARKDOWN_FILE_NAME",
        )
    }

    private fun inspectDirectory(directory: Path, paths: ResolvedPaths) {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return
        if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            fail(PersistenceFailureReason.UNSAFE_TARGET_PATH, "directory")
        }
        try {
            Files.newDirectoryStream(directory).use { entries ->
                for (entry in entries) {
                    if (entry != paths.json && entry != paths.markdown && entry != paths.tempJson && entry != paths.tempMarkdown) {
                        fail(PersistenceFailureReason.UNEXPECTED_OUTPUT_FILE, "directory")
                    }
                }
            }
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            fail(PersistenceFailureReason.READ_FAILED, "directory")
        }
    }

    private fun hasSymlinkComponentWithin(path: Path, boundary: Path): Boolean {
        val relative = try { boundary.relativize(path) } catch (_: Throwable) { return true }
        var current = boundary
        for (part in relative) {
            current = current.resolve(part)
            if (Files.isSymbolicLink(current)) return true
        }
        return false
    }

    private fun writeNew(path: Path, bytes: ByteArray) {
        try {
            Files.write(path, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            FileOutputStream(path.toFile(), true).use { it.fd.sync() }
        } catch (_: Throwable) {
            fail(PersistenceFailureReason.WRITE_FAILED, "temporary")
        }
    }

    private fun moveNoReplace(source: Path, target: Path) {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) fail(PersistenceFailureReason.ATOMIC_PUBLICATION_FAILED, "publish")
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            try {
                Files.move(source, target)
            } catch (_: Throwable) {
                fail(PersistenceFailureReason.ATOMIC_PUBLICATION_FAILED, "publish")
            }
        } catch (_: Throwable) {
            fail(PersistenceFailureReason.ATOMIC_PUBLICATION_FAILED, "publish")
        }
    }

    private fun cleanupPublished(
        paths: ResolvedPaths,
        json: ByteArray,
        markdown: ByteArray,
        tempJsonCreated: Boolean,
        tempMarkdownCreated: Boolean,
        jsonPublished: Boolean,
        markdownPublished: Boolean,
    ) {
        if (tempJsonCreated) deleteIfMatching(paths.tempJson, json)
        if (tempMarkdownCreated) deleteIfMatching(paths.tempMarkdown, markdown)
        if (jsonPublished) deleteIfMatching(paths.json, json)
        if (markdownPublished) deleteIfMatching(paths.markdown, markdown)
    }

    private fun deleteIfMatching(path: Path, expected: ByteArray) {
        try {
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) return
            if (Files.readAllBytes(path).contentEquals(expected)) Files.deleteIfExists(path)
        } catch (_: Throwable) {
            // A failed cleanup is reported by the original safe publication reason.
        }
    }

    private fun readBytes(path: Path, context: String): ByteArray {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            fail(PersistenceFailureReason.READ_FAILED, context)
        }
        return try {
            Files.readAllBytes(path)
        } catch (_: Throwable) {
            fail(PersistenceFailureReason.READ_FAILED, context)
        }
    }

    private fun requireValid(packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1) {
        when (val result = validatePacket(packet)) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Invalid ->
                fail(mapContractReason(result.reason), "packet")
        }
    }

    private fun mapContractReason(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1,
    ): PersistenceFailureReason = when (reason) {
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.PACKET_BINDING_DIGEST_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.ITEM_BINDING_DIGEST_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.RECORD_BINDING_MISMATCH,
        -> PersistenceFailureReason.BINDING_DIGEST_MISMATCH
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.PACKET_LOGICAL_DIGEST_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.ITEM_LOGICAL_DIGEST_MISMATCH,
        -> PersistenceFailureReason.LOGICAL_DIGEST_MISMATCH
        else -> PersistenceFailureReason.INVALID_PACKET
    }

    private fun canonicalJson(packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1): String =
        gson.toJson(packet) + "\n"

    private fun requireSingleFinalLf(bytes: ByteArray) {
        if (bytes.isEmpty() || bytes.last() != '\n'.code.toByte() || bytes.dropLast(1).contains('\n'.code.toByte())) {
            fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "json")
        }
    }

    private fun parseStrictJson(bytes: ByteArray): JsonElement {
        val reader = JsonReader(StringReader(bytes.toString(StandardCharsets.UTF_8).dropLast(1)))
        reader.isLenient = false
        val result = readJson(reader)
        if (reader.peek() != JsonToken.END_DOCUMENT) fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "json")
        return result
    }

    private fun readJson(reader: JsonReader): JsonElement = when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> {
            reader.beginObject()
            val result = JsonObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (result.has(name)) fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "duplicate")
                result.add(name, readJson(reader))
            }
            reader.endObject()
            result
        }
        JsonToken.BEGIN_ARRAY -> {
            reader.beginArray()
            val result = JsonArray()
            while (reader.hasNext()) result.add(readJson(reader))
            reader.endArray()
            result
        }
        JsonToken.STRING -> JsonPrimitive(reader.nextString())
        JsonToken.NUMBER -> JsonPrimitive(BigDecimal(reader.nextString()))
        JsonToken.BOOLEAN -> JsonPrimitive(reader.nextBoolean())
        JsonToken.NULL -> { reader.nextNull(); JsonNull.INSTANCE }
        else -> fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "json")
    }

    private fun appendEvidenceCards(
        builder: StringBuilder,
        item: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1,
        kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
    ) {
        item.directEvidence.filter { it.kind == kind }.forEach { card ->
            builder.appendLine("Evidence reference: ${card.evidenceReferenceId}")
            builder.appendLine("Evidence kind: ${card.kind.name}")
            builder.appendLine("Directness: ${card.directness.name}")
            builder.appendLine("Position: ${card.position.name}")
            builder.appendLine("Artifact: ${safeInline(card.artifact.relativePath)}")
            builder.appendLine("Artifact byte size: ${card.artifact.byteSize}")
            builder.appendLine("Artifact SHA-256: ${card.artifact.sha256}")
            builder.appendLine("Artifact logical digest: ${card.artifact.logicalDigest ?: "(none)"}")
            builder.appendLine("Record reference: ${safeInline(card.recordReference)}")
            card.fields.forEach { field ->
                builder.appendLine("Field ${safeInline(field.fieldReference)}: ${safeInline(field.fullValue)}")
                builder.appendLine("Field SHA-256: ${field.fullValueSha256}")
            }
            card.sourceProjection?.let { projection ->
                builder.appendLine("Projection source: ${projection.source.name}")
                builder.appendLine("Projection record kind: ${projection.recordKind.name}")
                builder.appendLine("Projection record reference: ${safeInline(projection.recordReference)}")
                builder.appendLine("Projection artifact: ${safeInline(projection.artifact.relativePath)}")
                builder.appendLine("Source-origin artifact: ${safeInline(projection.sourceOriginArtifact.relativePath)}")
            }
        }
    }

    private fun StringBuilder.appendFileBinding(label: String, binding: HimZeroCandidateRecoveryHumanReviewFileBindingV1) {
        appendLine("$label path: ${safeInline(binding.relativePath)}")
        appendLine("$label byte size: ${binding.byteSize}")
        appendLine("$label SHA-256: ${binding.sha256}")
        appendLine("$label logical digest: ${binding.logicalDigest}")
    }

    private fun StringBuilder.appendCounters(counters: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketCountersV1) {
        appendLine("packetItems=${counters.packetItems}")
        appendLine("distinctReviewUnits=${counters.distinctReviewUnits}")
        appendLine("originalConfirmDecisions=${counters.originalConfirmDecisions}")
        appendLine("originalRejectDecisions=${counters.originalRejectDecisions}")
        appendLine("originalAbstainDecisions=${counters.originalAbstainDecisions}")
        appendLine("originalEscalateDecisions=${counters.originalEscalateDecisions}")
        appendLine("sourceEvidenceRecords=${counters.sourceEvidenceRecords}")
        appendLine("catalogEvidenceRecords=${counters.catalogEvidenceRecords}")
        appendLine("authorityEvidenceRecords=${counters.authorityEvidenceRecords}")
        appendLine("directEvidenceReferences=${counters.directEvidenceReferences}")
        appendLine("distinctEvidenceReferences=${counters.distinctEvidenceReferences}")
        appendLine("supportsAssociationEvidence=${counters.supportsAssociationEvidence}")
        appendLine("contradictsAssociationEvidence=${counters.contradictsAssociationEvidence}")
        appendLine("contextOnlyEvidence=${counters.contextOnlyEvidence}")
        appendLine("itemsWithAlternativeCanonicalProposal=${counters.itemsWithAlternativeCanonicalProposal}")
        appendLine("itemsWithValidationAssessment=${counters.itemsWithValidationAssessment}")
        appendLine("itemsWithDownstreamRoute=${counters.itemsWithDownstreamRoute}")
    }

    private fun renderList(values: List<String>): String =
        if (values.isEmpty()) "(empty)" else values.joinToString(", ") { safeInline(it) }

    private fun safeInline(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\r", " ")
        .replace("\n", " ")
        .replace("`", "'")
        .replace("|", "\\|")

    private fun packetPersistenceContractId(): String = CONTRACT_ID

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun fail(reason: PersistenceFailureReason, safeContext: String): Nothing =
        throw PersistenceFailure(reason, safeContext)

    private data class ResolvedPaths(
        val directory: Path,
        val json: Path,
        val markdown: Path,
        val tempJson: Path,
        val tempMarkdown: Path,
        val jsonRelative: String,
        val markdownRelative: String,
    )

    private data class PersistenceFailure(
        val reason: PersistenceFailureReason,
        val safeContext: String,
    ) : IllegalArgumentException()
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1 {
    CREATED,
    ALREADY_PRESENT_IDENTICAL,
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1 {
    INVALID_REQUEST,
    INVALID_PACKET,
    INVALID_PACKET_ID,
    INVALID_OUTPUT_ROOT,
    UNSAFE_TARGET_PATH,
    SYMLINK_ESCAPE,
    SERIALIZATION_FAILED,
    DESERIALIZATION_FAILED,
    READ_FAILED,
    WRITE_FAILED,
    PARTIAL_PACKET_ARTIFACT_STATE,
    UNEXPECTED_OUTPUT_FILE,
    EXISTING_JSON_CONFLICT,
    EXISTING_MARKDOWN_CONFLICT,
    ATOMIC_PUBLICATION_FAILED,
    ROLLBACK_FAILED,
    RELOAD_MISMATCH,
    JSON_BYTE_MISMATCH,
    MARKDOWN_BYTE_MISMATCH,
    BINDING_DIGEST_MISMATCH,
    LOGICAL_DIGEST_MISMATCH,
    MARKDOWN_RENDER_FAILED,
}

private typealias PersistenceFailureReason = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1 {
    data object Disabled : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1

    data class Completed(
        val status: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1,
        val packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
        val jsonPath: String,
        val markdownPath: String,
        val jsonByteSize: Long,
        val markdownByteSize: Long,
        val jsonSha256: String,
        val markdownSha256: String,
        val packetBindingDigest: String,
        val packetLogicalDigest: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceRequestV1(
    val enabled: Boolean,
    val outputRoot: File,
    val packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
)
