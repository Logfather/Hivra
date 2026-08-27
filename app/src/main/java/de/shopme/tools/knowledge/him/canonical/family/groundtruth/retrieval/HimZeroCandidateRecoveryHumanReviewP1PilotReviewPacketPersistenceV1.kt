package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/** Deterministic JSON/Markdown persistence for an already validated review packet. */
object HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1 {
    const val CONTRACT_ID = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_REVIEW_PACKET_PERSISTENCE_V1"
    const val VERSION = "1"
    const val OUTPUT_ROOT =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-review-packet/v1"
    const val JSON_FILE_NAME = "review-packet.v1.json"
    const val MARKDOWN_FILE_NAME = "review-packet.v1.md"
    const val MARKDOWN_HEADER = "# HIM Zero-Candidate Recovery Human Review – P1 Pilot Review Packet V1"
    const val MATERIALIZED_CORPUS_EXPLANATION =
        "Only the source fields materialized in the immutable recovery-review corpus are included.\n" +
            "No complete source record or separate source projection is asserted.\n" +
            "No evidence-sufficiency decision is made by this packet."

    private const val TEMP_JSON_FILE_NAME = ".review-packet.v1.json.tmp"
    private const val TEMP_MARKDOWN_FILE_NAME = ".review-packet.v1.md.tmp"
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()
    private val SAFE_MISSION_ID = Regex("[a-z0-9][a-z0-9-]{0,127}")
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun serializePacket(packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1): ByteArray {
        requireValid(packet)
        return try {
            (gson.toJson(packet) + "\n").toByteArray(StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.SERIALIZATION_FAILED)
        }
    }

    fun deserializePacket(bytes: ByteArray): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1 {
        return try {
            val root = JsonParser.parseString(bytes.toString(StandardCharsets.UTF_8)).asJsonObject
            requireKeys(root, ROOT_KEYS)
            val packet = requireNotNull(gson.fromJson(root, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1::class.java))
            validateNestedKeys(root)
            requireValid(packet)
            packet
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.DESERIALIZATION_FAILED)
        }
    }

    fun readPacket(file: File): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1 {
        if (!file.isFile) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.READ_FAILED)
        return try {
            deserializePacket(file.readBytes())
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.READ_FAILED)
        }
    }

    fun validatePacket(packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1 =
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.validate(
            packet,
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION,
            packet.humanReviewInputBinding,
        )

    fun renderMarkdown(packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1): String {
        requireValid(packet)
        return try {
            buildString {
                appendLine(MARKDOWN_HEADER)
                appendLine()
                appendLine("> REVIEW CONTEXT ONLY")
                appendLine("> NO DECISION RECORDED")
                appendLine("> NO GOLD, TRAINING OR AUTHORITY EFFECT")
                appendLine()
                appendLine("Packet contract ID: ${packet.contractId}")
                appendLine("Packet version: ${packet.version}")
                appendLine("Mission ID: ${packet.missionBinding.missionId}")
                appendLine("Priority: ${packet.missionBinding.priority.name}")
                appendLine("Review round: ${packet.missionBinding.reviewRound}")
                appendLine("Selection digest: ${packet.missionBinding.selectionDigest}")
                appendLine("Packet binding digest: ${packet.packetBindingDigest}")
                appendLine("Packet logical digest: ${packet.packetLogicalDigest}")
                appendLine("Groups: ${packet.missionBinding.expectedSelectedGroups}")
                appendLine("Entries: ${packet.missionBinding.expectedSelectedCorpusEntries}")
                appendLine("Review units: ${packet.missionBinding.expectedSelectedReviewUnits}")
                packet.items.forEachIndexed { index, item ->
                    appendLine()
                    appendLine("## Review Unit ${index + 1}")
                    appendLine()
                    appendLine("### REVIEW UNIT")
                    appendLine("Stable entry ID: ${item.stableEntryId}")
                    appendLine("Review unit ID: ${item.reviewUnitId}")
                    appendLine("Group ordinal: ${item.groupOrdinal}")
                    appendLine("Group display value: ${item.groupDisplayValue}")
                    appendLine()
                    appendLine("### SOURCE CONTEXT")
                    appendLine("Source: ${item.sourceContext.source.name}")
                    appendLine("Record kind: ${item.sourceContext.recordKind.name}")
                    appendLine("Evidence reference: ${safeInline(item.sourceContext.evidenceReference)}")
                    appendLine("Finding occurrence IDs: ${item.sourceContext.findingOccurrenceIds.joinToString(", ")}")
                    appendLine("Selection reasons: ${item.sourceContext.selectionReasons.joinToString(", ") { it.name }}")
                    appendLine("Primary values: ${item.sourceContext.primaryValues.joinToString(", ") { safeInline(it) }}")
                    appendLine("Primary-value bucket: ${item.sourceContext.primaryValueBucket.name}")
                    appendLine("Recurring group memberships: ${item.sourceContext.recurringGroupMemberships.size}")
                    appendLine()
                    appendLine("### MATERIALIZED ORIGINAL FIELDS")
                    item.originalFields.forEach { field ->
                        appendLine("Field: ${safeInline(field.fieldName)}")
                        appendLine("Characters: ${field.originalCharacterCount}")
                        appendLine("Full-value SHA-256: ${field.originalValueSha256}")
                        appendLine("Display state: ${field.displayState.name}")
                        if (field.displayState == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.TRUNCATED_FOR_HUMAN_PROJECTION) appendLine("Display note: TRUNCATED_FOR_HUMAN_PROJECTION")
                        if (field.displayState == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.EMPTY_SOURCE_VALUE) appendLine("Display note: EMPTY_SOURCE_VALUE")
                        appendLine()
                        displayLines(field.humanDisplayValue ?: "").forEach { appendLine("    $it") }
                        appendLine()
                    }
                    appendLine("### AUDIT TARGET UNDER REVIEW")
                    appendLine("Canonical entity ID: ${item.targetContext.canonicalEntityId}")
                    appendLine("Bound display label: ${safeInline(item.targetContext.expectedDisplayLabel)}")
                    appendLine()
                    appendLine("### TARGET CONTEXT")
                    appendLine("Registry resolution: ${item.targetContext.registryResolution.name}")
                    appendLine("Authority resolution: ${item.targetContext.authorityResolution.name}")
                    appendLine("Catalog record reference: ${safeInline(item.targetContext.catalogRecordReference)}")
                    appendLine("Authority record reference: ${safeInline(item.targetContext.authorityRecordReference)}")
                    appendLine("Identity terms: ${renderList(item.targetContext.identityTerms)}")
                    appendLine("Alias terms: ${renderList(item.targetContext.aliasTerms)}")
                    appendLine("Target-context digest: ${item.targetContext.targetContextDigest}")
                    appendLine()
                    appendLine("### EVIDENCE SCOPE")
                    appendLine("Evidence kind: ${item.evidenceScope.evidenceKind.name}")
                    appendLine("Directness: ${item.evidenceScope.directness.name}")
                    appendLine("Coverage: ${item.evidenceScope.coverage.name}")
                    appendLine("Artifact reference: ${safeInline(item.evidenceScope.artifactReference)}")
                    appendLine("Artifact SHA-256: ${item.evidenceScope.artifactSha256}")
                    appendLine("Artifact logical digest: ${item.evidenceScope.artifactLogicalDigest ?: "(none)"}")
                    appendLine("Record reference: ${safeInline(item.evidenceScope.recordReference)}")
                    appendLine("Field references: ${renderList(item.evidenceScope.fieldReferences)}")
                    if (item.evidenceScope.coverage == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY) {
                        appendLine()
                        MATERIALIZED_CORPUS_EXPLANATION.lines().forEach { appendLine(it) }
                    }
                    appendLine()
                    appendLine("### CONTEXT LIMITATIONS")
                    item.contextLimitations.forEach { appendLine("- ${it.name}") }
                    appendLine()
                    appendLine("### BINDINGS")
                    appendLine("Stable entry ID: ${item.stableEntryId}")
                    appendLine("Review unit ID: ${item.reviewUnitId}")
                    appendLine("Canonical entity ID: ${item.canonicalEntityId}")
                    appendLine("Evidence reference: ${safeInline(item.sourceContext.evidenceReference)}")
                    appendLine("Item binding digest: ${item.itemBindingDigest}")
                    appendLine("Item logical digest: ${item.itemLogicalDigest}")
                }
                appendLine()
                appendLine("## BINDINGS")
                appendLine("Corpus file SHA-256: ${packet.corpusFileBinding.sha256}")
                appendLine("Corpus logical digest: ${packet.corpusLogicalDigest}")
                appendLine("Corpus binding digest: ${packet.corpusBindingDigest}")
                appendLine("Mission selection digest: ${packet.missionBinding.selectionDigest}")
                appendLine("Input binding digest: ${packet.humanReviewInputBinding.bindingDigest}")
                appendLine("Packet implementation HEAD: ${packet.packetImplementationHead}")
                appendLine("Packet binding digest: ${packet.packetBindingDigest}")
                appendLine("Packet logical digest: ${packet.packetLogicalDigest}")
            }
        } catch (_: Throwable) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.MARKDOWN_RENDER_FAILED)
        }
    }

    fun execute(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1 {
        if (!request.enabled) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Disabled
        if (!SAFE_MISSION_ID.matches(request.packet.missionBinding.missionId)) {
            return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.INVALID_MISSION_ID)
        }
        return try {
            requireValid(request.packet)
            val paths = resolvePaths(request.outputRoot.toPath(), request.packet.missionBinding.missionId)
            val jsonBytes = serializePacket(request.packet)
            val markdownBytes = renderMarkdown(request.packet).toByteArray(StandardCharsets.UTF_8)
            publishOrReuse(paths, request.packet, jsonBytes, markdownBytes)
        } catch (failure: PersistenceFailure) {
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed(failure.reason)
        } catch (_: Throwable) {
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.WRITE_FAILED)
        }
    }

    private fun publishOrReuse(
        paths: ResolvedPaths,
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1,
        jsonBytes: ByteArray,
        markdownBytes: ByteArray,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Completed {
        if (Files.isSymbolicLink(paths.json) || Files.isSymbolicLink(paths.markdown)) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.SYMLINK_ESCAPE)
        val jsonExists = Files.exists(paths.json, LinkOption.NOFOLLOW_LINKS)
        val markdownExists = Files.exists(paths.markdown, LinkOption.NOFOLLOW_LINKS)
        if (jsonExists != markdownExists) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.PARTIAL_PACKET_ARTIFACT_STATE)
        if (jsonExists) {
            val existingJson = readBytes(paths.json)
            if (!existingJson.contentEquals(jsonBytes)) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.EXISTING_JSON_CONFLICT)
            val existingMarkdown = readBytes(paths.markdown)
            if (!existingMarkdown.contentEquals(markdownBytes)) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.EXISTING_MARKDOWN_CONFLICT)
            val reloaded = deserializePacket(existingJson)
            if (reloaded != packet || renderMarkdown(reloaded).toByteArray(StandardCharsets.UTF_8).contentEquals(existingMarkdown).not()) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.RELOAD_MISMATCH)
            return completed(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, reloaded, paths, existingJson, existingMarkdown)
        }
        try {
            Files.createDirectories(paths.directory)
        } catch (_: Throwable) {
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.DIRECTORY_CREATE_FAILED)
        }
        if (Files.exists(paths.tempJson) || Files.exists(paths.tempMarkdown)) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.ATOMIC_PUBLICATION_FAILED)
        var jsonPublished = false
        try {
            writeNew(paths.tempJson, jsonBytes)
            writeNew(paths.tempMarkdown, markdownBytes)
            Files.move(paths.tempJson, paths.json, StandardCopyOption.ATOMIC_MOVE)
            jsonPublished = true
            Files.move(paths.tempMarkdown, paths.markdown, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Throwable) {
            deleteTemp(paths.tempJson)
            deleteTemp(paths.tempMarkdown)
            if (jsonPublished) {
                try {
                    if (Files.exists(paths.json) && readBytes(paths.json).contentEquals(jsonBytes)) Files.deleteIfExists(paths.json)
                } catch (_: Throwable) {
                    throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.ROLLBACK_FAILED)
                }
            }
            throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.ATOMIC_PUBLICATION_FAILED)
        }
        val persistedJson = readBytes(paths.json)
        val persistedMarkdown = readBytes(paths.markdown)
        if (!persistedJson.contentEquals(jsonBytes) || !persistedMarkdown.contentEquals(markdownBytes)) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.BYTE_IDENTITY_MISMATCH)
        val reloaded = try { deserializePacket(persistedJson) } catch (_: Throwable) { throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.RELOAD_MISMATCH) }
        if (reloaded != packet || renderMarkdown(reloaded).toByteArray(StandardCharsets.UTF_8).contentEquals(persistedMarkdown).not()) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.RELOAD_MISMATCH)
        return completed(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1.CREATED, reloaded, paths, persistedJson, persistedMarkdown)
    }

    private fun completed(
        status: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1,
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1,
        paths: ResolvedPaths,
        json: ByteArray,
        markdown: ByteArray,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Completed(
        status,
        packet,
        paths.jsonRelative,
        paths.markdownRelative,
        json.size.toLong(),
        markdown.size.toLong(),
        sha256(json),
        sha256(markdown),
        packet.packetBindingDigest,
        packet.packetLogicalDigest,
    )

    private fun resolvePaths(outputRoot: Path, missionId: String): ResolvedPaths {
        if (!SAFE_MISSION_ID.matches(missionId)) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.INVALID_MISSION_ID)
        if (outputRoot.toString().isBlank()) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.UNSAFE_OUTPUT_PATH)
        val root = outputRoot.toAbsolutePath().normalize()
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(root)) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.SYMLINK_ESCAPE)
        val directory = root.resolve(missionId).normalize()
        if (!directory.startsWith(root) || directory == root) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.UNSAFE_OUTPUT_PATH)
        if (hasSymlinkComponentWithin(directory, root)) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.SYMLINK_ESCAPE)
        val json = directory.resolve(JSON_FILE_NAME).normalize()
        val markdown = directory.resolve(MARKDOWN_FILE_NAME).normalize()
        if (!json.startsWith(directory) || !markdown.startsWith(directory)) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.UNSAFE_OUTPUT_PATH)
        return ResolvedPaths(root, directory, json, markdown, directory.resolve(TEMP_JSON_FILE_NAME), directory.resolve(TEMP_MARKDOWN_FILE_NAME), "$missionId/$JSON_FILE_NAME", "$missionId/$MARKDOWN_FILE_NAME")
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

    private fun requireValid(packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1) {
        when (val result = validatePacket(packet)) {
            is HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Invalid -> {
                val reason = when (result.reason) {
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.PACKET_BINDING_DIGEST_MISMATCH -> HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.PACKET_BINDING_DIGEST_MISMATCH
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.PACKET_LOGICAL_DIGEST_MISMATCH -> HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.PACKET_LOGICAL_DIGEST_MISMATCH
                    else -> HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.PACKET_VALIDATION_FAILED
                }
                throw PersistenceFailure(reason)
            }
        }
    }

    private fun requireKeys(objectValue: JsonObject, allowed: Set<String>) {
        if (objectValue.keySet() != allowed) throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.DESERIALIZATION_FAILED)
    }

    private fun validateNestedKeys(root: JsonObject) {
        val missionBinding = root.getAsJsonObject("missionBinding")
        requireKeys(missionBinding, MISSION_BINDING_KEYS)
        val humanInputBinding = root.getAsJsonObject("humanReviewInputBinding")
        requireKeys(humanInputBinding, HUMAN_INPUT_BINDING_KEYS)
        requireKeys(humanInputBinding.getAsJsonObject("corpusFileBinding"), FILE_BINDING_KEYS)
        humanInputBinding.get("registryBinding")?.takeUnless { it.isJsonNull }?.let { requireKeys(it.asJsonObject, FILE_BINDING_KEYS) }
        val existingCorpusInputBinding = humanInputBinding.getAsJsonObject("existingCorpusInputBinding")
        requireKeys(existingCorpusInputBinding, EXISTING_CORPUS_INPUT_BINDING_KEYS)
        requireKeys(existingCorpusInputBinding.getAsJsonObject("causeAnalysis"), FILE_BINDING_KEYS)
        requireKeys(existingCorpusInputBinding.getAsJsonObject("canonicalCatalog"), FILE_BINDING_KEYS)
        requireKeys(existingCorpusInputBinding.getAsJsonObject("canonicalAuthority"), FILE_BINDING_KEYS)
        requireKeys(root.getAsJsonObject("corpusFileBinding"), FILE_BINDING_KEYS)
        requireKeys(root.getAsJsonObject("counters"), COUNTER_KEYS)
        root.getAsJsonObject("counters").getAsJsonArray("sourceCounts").forEach { requireKeys(it.asJsonObject, SOURCE_COUNT_KEYS) }
        root.getAsJsonObject("counters").getAsJsonArray("recordKindCounts").forEach { requireKeys(it.asJsonObject, RECORD_KIND_COUNT_KEYS) }
        root.getAsJsonArray("items").forEach { itemElement ->
            val item = itemElement.asJsonObject
            requireKeys(item, ITEM_KEYS)
            requireKeys(item.getAsJsonObject("sourceContext"), SOURCE_CONTEXT_KEYS)
            item.getAsJsonObject("sourceContext").getAsJsonArray("recurringGroupMemberships").forEach { requireKeys(it.asJsonObject, GROUP_MEMBERSHIP_KEYS) }
            requireKeys(item.getAsJsonObject("targetContext"), TARGET_CONTEXT_KEYS)
            requireKeys(item.getAsJsonObject("evidenceScope"), EVIDENCE_SCOPE_KEYS)
            item.getAsJsonArray("originalFields").forEach { requireKeys(it.asJsonObject, ORIGINAL_FIELD_KEYS) }
        }
    }

    private fun displayLines(value: String): List<String> = value.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    private fun renderList(values: List<String>): String = if (values.isEmpty()) "(empty)" else values.joinToString(", ") { safeInline(it) }
    private fun safeInline(value: String): String = value.replace("\r", " ").replace("\n", " ").replace("`", "'")
    private fun writeNew(path: Path, bytes: ByteArray) {
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        FileOutputStream(path.toFile(), true).use { it.fd.sync() }
    }
    private fun deleteTemp(path: Path) { try { Files.deleteIfExists(path) } catch (_: Throwable) { /* handled by caller where required */ } }
    private fun readBytes(path: Path): ByteArray = try { Files.readAllBytes(path) } catch (_: Throwable) { throw PersistenceFailure(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.READ_FAILED) }
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private data class ResolvedPaths(
        val root: Path,
        val directory: Path,
        val json: Path,
        val markdown: Path,
        val tempJson: Path,
        val tempMarkdown: Path,
        val jsonRelative: String,
        val markdownRelative: String,
    )

    private val ROOT_KEYS = setOf("contractId", "version", "packetState", "missionBinding", "humanReviewInputBinding", "corpusFileBinding", "corpusLogicalDigest", "corpusBindingDigest", "packetImplementationHead", "items", "counters", "packetBindingDigest", "packetLogicalDigest")
    private val MISSION_BINDING_KEYS = setOf("missionContractId", "missionVersion", "missionId", "scopeId", "priority", "reviewRound", "maxNewDecisionRecords", "selectionDigest", "expectedSelectedGroups", "expectedSelectedCorpusEntries", "expectedSelectedReviewUnits", "expectedDistinctCanonicalTargets", "expectedMultiTargetEntries", "reviewUnitIds")
    private val HUMAN_INPUT_BINDING_KEYS = setOf("corpusFileBinding", "corpusLogicalDigest", "corpusReportDigest", "corpusBindingDigest", "existingCorpusInputBinding", "registryBinding", "contractId", "contractVersion", "implementationHead", "bindingDigest")
    private val EXISTING_CORPUS_INPUT_BINDING_KEYS = setOf("causeAnalysis", "canonicalCatalog", "canonicalAuthority", "causeAnalysisLogicalDigest", "recoveryReviewImplementationHead", "bindingDigest")
    private val FILE_BINDING_KEYS = setOf("relativePath", "byteSize", "sha256", "logicalDigest")
    private val COUNTER_KEYS = setOf("packetItems", "uniqueStableEntries", "uniqueReviewUnits", "distinctCanonicalTargets", "sourceCounts", "recordKindCounts", "materializedCorpusOnlyItems", "sourceProjectionIncludedItems", "itemsWithIdentityTerms", "itemsWithoutIdentityTerms", "itemsWithAliasTerms", "itemsWithoutAliasTerms", "fullDisplayFields", "truncatedDisplayFields", "emptySourceFields")
    private val SOURCE_COUNT_KEYS = setOf("source", "items")
    private val RECORD_KIND_COUNT_KEYS = setOf("recordKind", "items")
    private val ITEM_KEYS = setOf("stableEntryId", "reviewUnitId", "canonicalEntityId", "groupOrdinal", "groupDisplayValue", "sourceContext", "originalFields", "targetContext", "evidenceScope", "contextLimitations", "itemBindingDigest", "itemLogicalDigest")
    private val SOURCE_CONTEXT_KEYS = setOf("source", "recordKind", "evidenceReference", "findingOccurrenceIds", "selectionReasons", "primaryValues", "primaryValueBucket", "recurringGroupMemberships")
    private val TARGET_CONTEXT_KEYS = setOf("canonicalEntityId", "expectedDisplayLabel", "registryResolution", "authorityResolution", "catalogRecordReference", "authorityRecordReference", "identityTerms", "aliasTerms", "targetContextDigest")
    private val EVIDENCE_SCOPE_KEYS = setOf("evidenceKind", "directness", "artifactReference", "artifactSha256", "artifactLogicalDigest", "recordReference", "fieldReferences", "coverage")
    private val ORIGINAL_FIELD_KEYS = setOf("fieldName", "originalValue", "originalValueSha256", "humanDisplayValue", "displayState", "originalCharacterCount")
    private val GROUP_MEMBERSHIP_KEYS = setOf("primaryValue", "priorityClass", "referenceCount", "findingOccurrenceCount", "auditLinkedCanonicalEntityIds")
}

private class PersistenceFailure(
    val reason: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1,
) : IllegalArgumentException()

enum class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1 { CREATED, ALREADY_PRESENT_IDENTICAL }

enum class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1 {
    INVALID_REQUEST,
    INVALID_PACKET,
    INVALID_MISSION_ID,
    UNSAFE_OUTPUT_PATH,
    SYMLINK_ESCAPE,
    PACKET_VALIDATION_FAILED,
    PACKET_BINDING_DIGEST_MISMATCH,
    PACKET_LOGICAL_DIGEST_MISMATCH,
    SERIALIZATION_FAILED,
    DESERIALIZATION_FAILED,
    MARKDOWN_RENDER_FAILED,
    PARTIAL_PACKET_ARTIFACT_STATE,
    EXISTING_JSON_CONFLICT,
    EXISTING_MARKDOWN_CONFLICT,
    DIRECTORY_CREATE_FAILED,
    WRITE_FAILED,
    ATOMIC_PUBLICATION_FAILED,
    ROLLBACK_FAILED,
    READ_FAILED,
    RELOAD_MISMATCH,
    BYTE_IDENTITY_MISMATCH,
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1 {
    data object Disabled : HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1

    data class Completed(
        val status: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1,
        val packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1,
        val jsonPath: String,
        val markdownPath: String,
        val jsonByteSize: Long,
        val markdownByteSize: Long,
        val jsonSha256: String,
        val markdownSha256: String,
        val packetBindingDigest: String,
        val packetLogicalDigest: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceRequestV1(
    val enabled: Boolean,
    val outputRoot: File,
    val packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1,
)
