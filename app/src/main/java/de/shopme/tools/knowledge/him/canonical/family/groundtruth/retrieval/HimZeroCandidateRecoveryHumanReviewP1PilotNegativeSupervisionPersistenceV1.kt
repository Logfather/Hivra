package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/** Durable, training-neutral persistence for validated negative-supervision context. */
object HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_SUPERVISION_PERSISTENCE_V1"
    const val VERSION = "1"
    const val STATE = "NEGATIVE_SUPERVISION_PERSISTED_CONTEXT_ONLY"
    const val DURABLE_ROOT =
        "data/knowledge/him/canonical-family/human-review/zero-candidate-recovery/v1/negative-supervision-batches"
    const val DURABLE_FILE_NAME = "negative-supervision.v1.json"

    private const val DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_SUPERVISION_PERSISTENCE_BATCH_V1"
    private const val RECORD_ID_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_SUPERVISION_RECORD_V1"
    private const val TEMP_FILE_NAME = ".negative-supervision.v1.json.tmp"
    private val SAFE_BATCH_ID = Regex("[a-z0-9][a-z0-9-]{0,63}")
    private val SHA256 = Regex("[0-9a-f]{64}")
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()

    fun serializeBatch(batch: Batch): ByteArray {
        requireValidBatch(batch)
        return try {
            (gson.toJson(batch) + "\n").toByteArray(StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReason.SERIALIZATION_FAILED, "batch")
        }
    }

    fun deserializeBatch(bytes: ByteArray): Batch {
        return try {
            requireSingleTrailingLf(bytes)
            val root = JsonParser.parseString(bytes.toString(StandardCharsets.UTF_8)).asJsonObject
            requireKeys(root, ROOT_KEYS)
            requireKeys(root.getAsJsonObject("inputBinding"), INPUT_BINDING_KEYS)
            requireKeys(root.getAsJsonObject("counters"), COUNTER_KEYS)
            root.getAsJsonArray("records").forEach { requireKeys(it.asJsonObject, RECORD_KEYS) }
            requireNotNull(gson.fromJson(root, Batch::class.java)).also(::requireValidBatch)
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReason.DESERIALIZATION_FAILED, "batch")
        }
    }

    fun readBatch(file: File): Batch {
        val path = file.toPath()
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw PersistenceFailure(FailureReason.READ_FAILED, "batch")
        }
        return try {
            deserializeBatch(Files.readAllBytes(path))
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReason.READ_FAILED, "batch")
        }
    }

    fun execute(request: Request): Result {
        if (!request.enabled) return Result.Disabled
        return try {
            validateBatchId(request.batchId)
            validateCandidate(request.candidateBatch)
            val negativeDecisions = request.candidateBatch.decisions.filter {
                it.state == HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE
            }
            if (negativeDecisions.isEmpty()) {
                return Result.Skipped(SkipReason.NO_NEGATIVE_SUPERVISION_CANDIDATES)
            }
            val batch = buildBatch(request.batchId, request.candidateBatch, negativeDecisions)
            val paths = resolvePaths(request.durableRoot, request.batchId)
            val bytes = serializeBatch(batch)
            publishOrReuse(paths, batch, bytes)
        } catch (failure: PersistenceFailure) {
            Result.Failed(failure.reason, failure.safeContext)
        } catch (_: Throwable) {
            Result.Failed(FailureReason.WRITE_FAILED, "persistence")
        }
    }

    fun recordId(decision: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Decision): String =
        sha256(
            buildString {
                appendLine(RECORD_ID_DOMAIN)
                appendLine("contractId=$CONTRACT_ID")
                appendLine("version=$VERSION")
                appendLine("negativeCandidateId=${decision.negativeCandidateId}")
                appendLine("validationRecordId=${decision.validationRecordId}")
                appendLine("reviewUnitId=${decision.reviewUnitId}")
                appendLine("canonicalEntityId=${decision.canonicalEntityId}")
                appendLine("readinessDecisionId=${decision.readinessDecisionId}")
                appendLine("eligibilityDecisionId=${decision.eligibilityDecisionId}")
                appendLine("originalDecision=${decision.originalDecision.name}")
                appendLine("assessment=${decision.assessment.name}")
                decision.validationReasonCodes.forEach { appendLine("reasonCode=${it.name}") }
                decision.evidenceReferenceIds.forEach { appendLine("evidenceReferenceId=$it") }
                appendLine("downstreamRoute=${decision.downstreamRoute.name}")
            },
        )

    fun batchLogicalDigest(batch: Batch): String = sha256(
        buildString {
            appendLine(DIGEST_DOMAIN)
            appendLine("contractId=${batch.contractId}")
            appendLine("version=${batch.version}")
            appendLine("state=${batch.state}")
            appendLine("sourceCandidateContractId=${batch.sourceCandidateContractId}")
            appendLine("sourceCandidateVersion=${batch.sourceCandidateVersion}")
            appendLine("sourceReadinessBatchId=${batch.sourceReadinessBatchId}")
            appendLine("sourceCandidateBatchLogicalDigest=${batch.sourceCandidateBatchLogicalDigest}")
            appendLine("durableBatchId=${batch.durableBatchId}")
            appendInputBinding(this, batch.inputBinding)
            batch.records.forEach { record ->
                appendLine("recordId=${record.recordId}")
                appendLine("negativeCandidateId=${record.negativeCandidateId}")
                appendLine("readinessDecisionId=${record.readinessDecisionId}")
                appendLine("readinessState=${record.readinessState.name}")
                appendLine("eligibilityBatchId=${record.eligibilityBatchId}")
                appendLine("eligibilityInputBindingDigest=${record.eligibilityInputBindingDigest}")
                appendLine("eligibilityDecisionId=${record.eligibilityDecisionId}")
                appendLine("eligibilityState=${record.eligibilityState.name}")
                appendLine("validationBatchId=${record.validationBatchId}")
                appendLine("validationBatchBindingDigest=${record.validationBatchBindingDigest}")
                appendLine("validationBatchLogicalDigest=${record.validationBatchLogicalDigest}")
                appendLine("validationRecordId=${record.validationRecordId}")
                appendLine("reviewUnitId=${record.reviewUnitId}")
                appendLine("stableEntryId=${record.stableEntryId}")
                appendLine("canonicalEntityId=${record.canonicalEntityId}")
                appendLine("originalReviewerRef=${record.originalReviewerRef}")
                appendLine("validatorReviewerRef=${record.validatorReviewerRef}")
                appendLine("validationRound=${record.validationRound}")
                appendLine("validationRevision=${record.validationRevision}")
                appendLine("originalDecision=${record.originalDecision.name}")
                appendLine("assessment=${record.assessment.name}")
                record.validationReasonCodes.forEach { appendLine("reasonCode=${it.name}") }
                record.evidenceReferenceIds.forEach { appendLine("evidenceReferenceId=$it") }
                appendLine("downstreamRoute=${record.downstreamRoute.name}")
                appendLine("candidateState=${record.candidateState.name}")
            }
            appendLine("totalNegativeSupervisionRecords=${batch.counters.totalNegativeSupervisionRecords}")
        },
    )

    private fun buildBatch(
        batchId: String,
        candidateBatch: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Batch,
        negativeDecisions: List<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Decision>,
    ): Batch {
        val records = negativeDecisions.map { decision ->
            Record(
                recordId = recordId(decision),
                negativeCandidateId = requireNotNull(decision.negativeCandidateId),
                readinessDecisionId = decision.readinessDecisionId,
                readinessState = decision.readinessState,
                eligibilityBatchId = decision.eligibilityBatchId,
                eligibilityInputBindingDigest = decision.eligibilityInputBindingDigest,
                eligibilityDecisionId = decision.eligibilityDecisionId,
                eligibilityState = decision.eligibilityState,
                validationBatchId = decision.validationBatchId,
                validationBatchBindingDigest = decision.validationBatchBindingDigest,
                validationBatchLogicalDigest = decision.validationBatchLogicalDigest,
                validationRecordId = decision.validationRecordId,
                reviewUnitId = decision.reviewUnitId,
                stableEntryId = decision.stableEntryId,
                canonicalEntityId = decision.canonicalEntityId,
                originalReviewerRef = decision.originalReviewerRef,
                validatorReviewerRef = decision.validatorReviewerRef,
                validationRound = decision.validationRound,
                validationRevision = decision.validationRevision,
                originalDecision = decision.originalDecision,
                assessment = decision.assessment,
                validationReasonCodes = decision.validationReasonCodes,
                evidenceReferenceIds = decision.evidenceReferenceIds,
                downstreamRoute = decision.downstreamRoute,
                candidateState = decision.state,
            )
        }
        val inputBinding = InputBinding(
            candidateBatchLogicalDigest = candidateBatch.logicalDigest,
            persistedCandidateDecisionIds = records.map { it.readinessDecisionId },
            persistedNegativeCandidateIds = records.map { it.negativeCandidateId },
            validationBatchLogicalDigests = records.map { it.validationBatchLogicalDigest }.distinct(),
            eligibilityInputBindingDigests = records.map { it.eligibilityInputBindingDigest }.distinct(),
            readinessBatchLogicalDigest = candidateBatch.readinessBatchLogicalDigest,
        )
        val unsigned = Batch(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            sourceCandidateContractId = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CONTRACT_ID,
            sourceCandidateVersion = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.VERSION,
            sourceReadinessBatchId = candidateBatch.readinessBatchId,
            sourceCandidateBatchLogicalDigest = candidateBatch.logicalDigest,
            durableBatchId = batchId,
            inputBinding = inputBinding,
            records = records,
            counters = Counters.from(records),
            logicalDigest = "",
        )
        return unsigned.copy(logicalDigest = batchLogicalDigest(unsigned))
    }

    private fun publishOrReuse(paths: ResolvedPaths, batch: Batch, bytes: ByteArray): Result.Completed {
        if (Files.isSymbolicLink(paths.json)) fail(FailureReason.UNSAFE_TARGET_PATH, "target")
        if (Files.exists(paths.json, LinkOption.NOFOLLOW_LINKS)) {
            val existing = try { Files.readAllBytes(paths.json) } catch (_: Throwable) {
                fail(FailureReason.READ_FAILED, "target")
            }
            if (!existing.contentEquals(bytes)) fail(FailureReason.EXISTING_ARTIFACT_CONFLICT, "target")
            val reloaded = try { deserializeBatch(existing) } catch (_: Throwable) {
                fail(FailureReason.RELOAD_MISMATCH, "target")
            }
            if (reloaded != batch) fail(FailureReason.RELOAD_MISMATCH, "target")
            return completed(PersistenceStatus.ALREADY_PRESENT_IDENTICAL, batch, paths, bytes)
        }
        if (Files.exists(paths.temp, LinkOption.NOFOLLOW_LINKS)) fail(FailureReason.PARTIAL_ARTIFACT_STATE, "temporary")
        try {
            Files.createDirectories(paths.directory)
            Files.write(paths.temp, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            FileOutputStream(paths.temp.toFile(), true).use { it.fd.sync() }
            try {
                Files.move(paths.temp, paths.json, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(paths.temp, paths.json)
            }
        } catch (_: Throwable) {
            try { Files.deleteIfExists(paths.temp) } catch (_: Throwable) { }
            fail(FailureReason.ATOMIC_PUBLICATION_FAILED, "write")
        }
        val persisted = try { Files.readAllBytes(paths.json) } catch (_: Throwable) {
            fail(FailureReason.RELOAD_MISMATCH, "reload")
        }
        if (!persisted.contentEquals(bytes)) fail(FailureReason.BYTE_IDENTITY_MISMATCH, "reload")
        try {
            if (deserializeBatch(persisted) != batch) fail(FailureReason.RELOAD_MISMATCH, "reload")
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            fail(FailureReason.RELOAD_MISMATCH, "reload")
        }
        return completed(PersistenceStatus.CREATED, batch, paths, bytes)
    }

    private fun completed(
        status: PersistenceStatus,
        batch: Batch,
        paths: ResolvedPaths,
        bytes: ByteArray,
    ) = Result.Completed(
        status = status,
        batch = batch,
        relativePath = paths.relativePath,
        byteSize = bytes.size.toLong(),
        sha256 = sha256(bytes),
    )

    private fun validateCandidate(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Batch,
    ) {
        if (batch.contractId != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CONTRACT_ID ||
            batch.version != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.VERSION ||
            batch.state != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.STATE
        ) fail(FailureReason.INVALID_CANDIDATE_BATCH, "candidate")
        if (!SHA256.matches(batch.logicalDigest) ||
            batch.logicalDigest != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.batchLogicalDigest(
                batch.copy(logicalDigest = ""),
            )
        ) fail(FailureReason.INVALID_CANDIDATE_BATCH, "candidate")
        val expectedCounters = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Counters.from(batch.decisions)
        if (batch.counters != expectedCounters) fail(FailureReason.INVALID_CANDIDATE_BATCH, "candidate")
        if (batch.decisions.size != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING.originalSelections.size) {
            fail(FailureReason.INVALID_CANDIDATE_BATCH, "candidate")
        }
        if (batch.decisions.map { it.readinessDecisionId }.distinct().size != batch.decisions.size ||
            batch.decisions.map { it.reviewUnitId }.distinct().size != batch.decisions.size
        ) fail(FailureReason.INVALID_CANDIDATE_BATCH, "candidate")
        batch.decisions.forEach { decision ->
            if (!SHA256.matches(decision.readinessDecisionId) ||
                !SHA256.matches(decision.validationRecordId) ||
                !SHA256.matches(decision.eligibilityDecisionId) ||
                decision.reviewUnitId.isBlank() ||
                decision.stableEntryId.isBlank() ||
                decision.canonicalEntityId.isBlank() ||
                decision.originalReviewerRef.isBlank() ||
                decision.validatorReviewerRef.isBlank() ||
                decision.validationRound < 1 ||
                decision.validationRevision < 1 ||
                decision.validationReasonCodes.isEmpty() ||
                decision.evidenceReferenceIds.any { it.isBlank() }
            ) {
                fail(FailureReason.INVALID_CANDIDATE_BATCH, "candidate")
            }
            when (decision.state) {
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE -> {
                    if (decision.negativeCandidateId == null || !SHA256.matches(decision.negativeCandidateId) ||
                        decision.originalDecision != HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION ||
                        decision.downstreamRoute != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE ||
                        decision.evidenceReferenceIds.isEmpty() ||
                        decision.evidenceReferenceIds.distinct().size != decision.evidenceReferenceIds.size
                    ) fail(FailureReason.INVALID_CANDIDATE_BATCH, "candidate")
                }
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NOT_APPLICABLE_NON_NEGATIVE -> {
                    if (decision.negativeCandidateId != null) fail(FailureReason.INVALID_CANDIDATE_BATCH, "candidate")
                }
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.REQUIRES_ADJUDICATION,
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NOT_ELIGIBLE_FOR_NEGATIVE_SUPERVISION,
                    -> if (decision.negativeCandidateId != null) fail(FailureReason.INVALID_CANDIDATE_BATCH, "candidate")
            }
        }
    }

    private fun requireValidBatch(batch: Batch) {
        validateBatchId(batch.durableBatchId)
        if (batch.contractId != CONTRACT_ID || batch.version != VERSION || batch.state != STATE) fail(FailureReason.INVALID_BATCH, "batch")
        if (batch.sourceCandidateContractId != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CONTRACT_ID ||
            batch.sourceCandidateVersion != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.VERSION ||
            batch.sourceReadinessBatchId.isBlank() ||
            !SHA256.matches(batch.sourceCandidateBatchLogicalDigest)
        ) fail(FailureReason.INVALID_BATCH, "binding")
        if (batch.records.isEmpty() || batch.records.size != batch.counters.totalNegativeSupervisionRecords) fail(FailureReason.INVALID_COUNTERS, "counters")
        if (batch.records.map { it.recordId }.distinct().size != batch.records.size ||
            batch.records.map { it.negativeCandidateId }.distinct().size != batch.records.size ||
            batch.records.map { it.reviewUnitId }.distinct().size != batch.records.size
        ) fail(FailureReason.INVALID_RECORDS, "records")
        batch.records.forEach { record ->
            if (record.candidateState != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE ||
                !SHA256.matches(record.recordId) || !SHA256.matches(record.negativeCandidateId) ||
                record.evidenceReferenceIds.isEmpty() || record.evidenceReferenceIds.distinct().size != record.evidenceReferenceIds.size ||
                record.validationReasonCodes.isEmpty() || record.originalDecision != HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION ||
                record.downstreamRoute != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE ||
                record.recordId != recordId(record.toCandidateDecision())
            ) fail(FailureReason.INVALID_RECORDS, "records")
        }
        val expectedInput = InputBinding(
            candidateBatchLogicalDigest = batch.sourceCandidateBatchLogicalDigest,
            persistedCandidateDecisionIds = batch.records.map { it.readinessDecisionId },
            persistedNegativeCandidateIds = batch.records.map { it.negativeCandidateId },
            validationBatchLogicalDigests = batch.records.map { it.validationBatchLogicalDigest }.distinct(),
            eligibilityInputBindingDigests = batch.records.map { it.eligibilityInputBindingDigest }.distinct(),
            readinessBatchLogicalDigest = batch.inputBinding.readinessBatchLogicalDigest,
        )
        if (batch.inputBinding.candidateBatchLogicalDigest != batch.sourceCandidateBatchLogicalDigest || batch.inputBinding != expectedInput) {
            fail(FailureReason.BROKEN_INPUT_BINDING, "binding")
        }
        if (batch.counters != Counters.from(batch.records) || batch.logicalDigest != batchLogicalDigest(batch.copy(logicalDigest = ""))) {
            fail(FailureReason.INVALID_BATCH, "digest")
        }
    }

    private fun Record.toCandidateDecision() = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Decision(
        negativeCandidateId = negativeCandidateId,
        readinessDecisionId = readinessDecisionId,
        readinessState = readinessState,
        eligibilityBatchId = eligibilityBatchId,
        eligibilityInputBindingDigest = eligibilityInputBindingDigest,
        eligibilityDecisionId = eligibilityDecisionId,
        eligibilityState = eligibilityState,
        validationBatchId = validationBatchId,
        validationBatchBindingDigest = validationBatchBindingDigest,
        validationBatchLogicalDigest = validationBatchLogicalDigest,
        validationRecordId = validationRecordId,
        reviewUnitId = reviewUnitId,
        stableEntryId = stableEntryId,
        canonicalEntityId = canonicalEntityId,
        originalReviewerRef = originalReviewerRef,
        validatorReviewerRef = validatorReviewerRef,
        validationRound = validationRound,
        validationRevision = validationRevision,
        originalDecision = originalDecision,
        assessment = assessment,
        validationReasonCodes = validationReasonCodes,
        evidenceReferenceIds = evidenceReferenceIds,
        downstreamRoute = downstreamRoute,
        state = candidateState,
    )

    private fun validateBatchId(value: String) {
        if (!SAFE_BATCH_ID.matches(value)) fail(FailureReason.INVALID_BATCH_ID, "batchId")
    }

    private fun resolvePaths(rootFile: File, batchId: String): ResolvedPaths {
        val root = rootFile.toPath().toAbsolutePath().normalize()
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(root)) fail(FailureReason.SYMLINK_ESCAPE, "root")
        val directory = root.resolve(batchId).normalize()
        if (!directory.startsWith(root) || directory == root || hasSymlinkComponentWithin(directory, root)) fail(FailureReason.UNSAFE_DURABLE_ROOT, "root")
        val json = directory.resolve(DURABLE_FILE_NAME).normalize()
        val temp = directory.resolve(TEMP_FILE_NAME).normalize()
        if (!json.startsWith(directory) || !temp.startsWith(directory)) fail(FailureReason.UNSAFE_DURABLE_ROOT, "target")
        return ResolvedPaths(root, directory, json, temp, "$batchId/$DURABLE_FILE_NAME")
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

    private fun requireSingleTrailingLf(bytes: ByteArray) {
        if (bytes.isEmpty() || bytes.last() != '\n'.code.toByte() || (bytes.size > 1 && bytes[bytes.size - 2] == '\n'.code.toByte())) {
            throw PersistenceFailure(FailureReason.DESERIALIZATION_FAILED, "batch")
        }
    }

    private fun requireKeys(value: JsonObject, expected: Set<String>) {
        if (value.keySet() != expected) throw PersistenceFailure(FailureReason.DESERIALIZATION_FAILED, "schema")
    }

    private fun appendInputBinding(builder: StringBuilder, binding: InputBinding) {
        builder.appendLine("candidateBatchLogicalDigest=${binding.candidateBatchLogicalDigest}")
        binding.persistedCandidateDecisionIds.forEach { builder.appendLine("persistedCandidateDecisionId=$it") }
        binding.persistedNegativeCandidateIds.forEach { builder.appendLine("persistedNegativeCandidateId=$it") }
        binding.validationBatchLogicalDigests.forEach { builder.appendLine("validationBatchLogicalDigest=$it") }
        binding.eligibilityInputBindingDigests.forEach { builder.appendLine("eligibilityInputBindingDigest=$it") }
        builder.appendLine("readinessBatchLogicalDigest=${binding.readinessBatchLogicalDigest}")
    }

    private fun fail(reason: FailureReason, context: String): Nothing = throw PersistenceFailure(reason, context)

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private data class ResolvedPaths(
        val root: Path,
        val directory: Path,
        val json: Path,
        val temp: Path,
        val relativePath: String,
    )

    private val ROOT_KEYS = setOf(
        "contractId", "version", "state", "sourceCandidateContractId", "sourceCandidateVersion",
        "sourceReadinessBatchId", "sourceCandidateBatchLogicalDigest", "durableBatchId", "inputBinding",
        "records", "counters", "logicalDigest",
    )
    private val INPUT_BINDING_KEYS = setOf(
        "candidateBatchLogicalDigest", "persistedCandidateDecisionIds", "persistedNegativeCandidateIds",
        "validationBatchLogicalDigests", "eligibilityInputBindingDigests", "readinessBatchLogicalDigest",
    )
    private val COUNTER_KEYS = setOf("totalNegativeSupervisionRecords")
    private val RECORD_KEYS = setOf(
        "recordId", "negativeCandidateId", "readinessDecisionId", "readinessState", "eligibilityBatchId",
        "eligibilityInputBindingDigest", "eligibilityDecisionId", "eligibilityState", "validationBatchId",
        "validationBatchBindingDigest", "validationBatchLogicalDigest", "validationRecordId", "reviewUnitId",
        "stableEntryId", "canonicalEntityId", "originalReviewerRef", "validatorReviewerRef", "validationRound",
        "validationRevision", "originalDecision", "assessment", "validationReasonCodes", "evidenceReferenceIds",
        "downstreamRoute", "candidateState",
    )

    data class InputBinding(
        val candidateBatchLogicalDigest: String,
        val persistedCandidateDecisionIds: List<String>,
        val persistedNegativeCandidateIds: List<String>,
        val validationBatchLogicalDigests: List<String>,
        val eligibilityInputBindingDigests: List<String>,
        val readinessBatchLogicalDigest: String,
    )

    data class Record(
        val recordId: String,
        val negativeCandidateId: String,
        val readinessDecisionId: String,
        val readinessState: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState,
        val eligibilityBatchId: String,
        val eligibilityInputBindingDigest: String,
        val eligibilityDecisionId: String,
        val eligibilityState: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State,
        val validationBatchId: String,
        val validationBatchBindingDigest: String,
        val validationBatchLogicalDigest: String,
        val validationRecordId: String,
        val reviewUnitId: String,
        val stableEntryId: String,
        val canonicalEntityId: String,
        val originalReviewerRef: String,
        val validatorReviewerRef: String,
        val validationRound: Int,
        val validationRevision: Int,
        val originalDecision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
        val assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1,
        val validationReasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1>,
        val evidenceReferenceIds: List<String>,
        val downstreamRoute: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1,
        val candidateState: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState,
    )

    data class Counters(val totalNegativeSupervisionRecords: Int) {
        companion object {
            fun from(records: List<Record>) = Counters(records.size)
        }
    }

    data class Batch(
        val contractId: String,
        val version: String,
        val state: String,
        val sourceCandidateContractId: String,
        val sourceCandidateVersion: String,
        val sourceReadinessBatchId: String,
        val sourceCandidateBatchLogicalDigest: String,
        val durableBatchId: String,
        val inputBinding: InputBinding,
        val records: List<Record>,
        val counters: Counters,
        val logicalDigest: String,
    )

    data class Request(
        val batchId: String,
        val candidateBatch: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Batch,
        val durableRoot: File,
        val enabled: Boolean = true,
    )

    enum class PersistenceStatus { CREATED, ALREADY_PRESENT_IDENTICAL }

    enum class SkipReason { NO_NEGATIVE_SUPERVISION_CANDIDATES }

    enum class FailureReason {
        INVALID_REQUEST,
        INVALID_BATCH_ID,
        INVALID_CANDIDATE_BATCH,
        INVALID_BATCH,
        INVALID_RECORDS,
        INVALID_COUNTERS,
        BROKEN_INPUT_BINDING,
        SERIALIZATION_FAILED,
        DESERIALIZATION_FAILED,
        READ_FAILED,
        UNSAFE_DURABLE_ROOT,
        UNSAFE_TARGET_PATH,
        SYMLINK_ESCAPE,
        PARTIAL_ARTIFACT_STATE,
        EXISTING_ARTIFACT_CONFLICT,
        ATOMIC_PUBLICATION_FAILED,
        BYTE_IDENTITY_MISMATCH,
        RELOAD_MISMATCH,
        WRITE_FAILED,
    }

    sealed interface Result {
        data object Disabled : Result
        data class Skipped(val reason: SkipReason) : Result
        data class Completed(
            val status: PersistenceStatus,
            val batch: Batch,
            val relativePath: String,
            val byteSize: Long,
            val sha256: String,
        ) : Result
        data class Failed(val reason: FailureReason, val safeContext: String) : Result
    }

    private class PersistenceFailure(
        val reason: FailureReason,
        val safeContext: String,
    ) : IllegalArgumentException()
}
