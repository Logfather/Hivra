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

/**
 * Purely local persistence for an already validated independent decision-validation batch.
 * It has no validation runtime, source access, reviewer assignment, or downstream effect.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PERSISTENCE_V1"
    const val PERSISTENCE_CONTRACT_ID = CONTRACT_ID
    const val VERSION = "1"
    const val PERSISTENCE_VERSION = VERSION
    const val DURABLE_ROOT =
        "data/knowledge/him/canonical-family/human-review/zero-candidate-recovery/v1/decision-validation-batches"
    const val REPORT_ROOT =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/v1/decision-validation-batches"
    const val DURABLE_FILE_NAME = "decision-validations.v1.json"
    const val REPORT_JSON_FILE_NAME = "persistence-report.v1.json"
    const val REPORT_TEXT_FILE_NAME = "persistence-report.v1.txt"
    const val VALIDATION_STATE = "INDEPENDENT_DECISION_VALIDATION_CONTEXT_ONLY"
    const val REPORT_LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PERSISTENCE_REPORT_LOGICAL_V1"

    private const val TEMP_FILE_PREFIX = ".him-validation-persistence-"
    private val BATCH_ID = Regex("[a-z0-9][a-z0-9-]{0,63}")
    private val SHA256 = Regex("[0-9a-f]{64}")
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()

    fun serializeValidationBatch(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
    ): ByteArray {
        requireValidBatch(batch)
        return try {
            (gson.toJson(batch) + "\n").toByteArray(StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            throw PersistenceFailure(PersistenceFailureReason.SERIALIZATION_FAILED, "batch")
        }
    }

    fun serializeBatch(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
    ): ByteArray = serializeValidationBatch(batch)

    fun deserializeValidationBatch(
        bytes: ByteArray,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1 = try {
        val root = parseStrictJson(bytes).asJsonObject
        requireKeys(root, BATCH_KEYS)
        requireNestedKeys(root)
        requireNotNull(gson.fromJson(root, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1::class.java))
            .also(::requireValidBatch)
    } catch (failure: PersistenceFailure) {
        throw failure
    } catch (_: Throwable) {
        throw PersistenceFailure(PersistenceFailureReason.DESERIALIZATION_FAILED, "batch")
    }

    fun deserializeBatch(
        bytes: ByteArray,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1 = deserializeValidationBatch(bytes)

    fun readValidationBatch(file: File): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1 {
        if (!file.isFile || Files.isSymbolicLink(file.toPath())) {
            throw PersistenceFailure(PersistenceFailureReason.READ_FAILED, "batch")
        }
        return try {
            deserializeValidationBatch(file.readBytes())
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(PersistenceFailureReason.READ_FAILED, "batch")
        }
    }

    fun serializeReport(report: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1): ByteArray {
        requireValidReport(report)
        return try {
            (gson.toJson(report) + "\n").toByteArray(StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            throw PersistenceFailure(PersistenceFailureReason.SERIALIZATION_FAILED, "reportJson")
        }
    }

    fun deserializeReport(
        bytes: ByteArray,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1 = try {
        val root = parseStrictJson(bytes).asJsonObject
        requireKeys(root, REPORT_KEYS)
        requireReportNestedKeys(root)
        requireNotNull(gson.fromJson(root, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1::class.java))
            .also(::requireValidReport)
    } catch (failure: PersistenceFailure) {
        throw failure
    } catch (_: Throwable) {
        throw PersistenceFailure(PersistenceFailureReason.DESERIALIZATION_FAILED, "reportJson")
    }

    fun renderReportText(
        report: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1,
    ): String {
        requireValidReport(report)
        return buildString {
            appendLine("persistenceContractId=${report.persistenceContractId}")
            appendLine("persistenceVersion=${report.persistenceVersion}")
            appendLine("validationContractId=${report.validationContractId}")
            appendLine("validationContractVersion=${report.validationContractVersion}")
            appendLine("validationBatchId=${report.validationBatchId}")
            appendLine("state=${report.state}")
            appendLine("validatorReviewerRef=${report.validatorReviewerRef}")
            appendLine("validationRound=${report.validationRound}")
            appendLine("validationRevision=${report.validationRevision}")
            appendLine("durableRelativePath=${report.durableRelativePath}")
            appendLine("reportJsonRelativePath=${report.reportJsonRelativePath}")
            appendLine("reportTextRelativePath=${report.reportTextRelativePath}")
            appendLine("inputBindingDigest=${report.inputBindingDigest}")
            appendLine("validationBatchBindingDigest=${report.validationBatchBindingDigest}")
            appendLine("validationBatchLogicalDigest=${report.validationBatchLogicalDigest}")
            appendLine("durableJsonSha256=${report.durableJsonSha256}")
            appendLine("durableJsonByteSize=${report.durableJsonByteSize}")
            appendCounters(report.counters)
            appendLine("reportLogicalDigest=${report.reportLogicalDigest}")
            appendLine()
            appendLine("VALIDATION CONTEXT ONLY — NO GOLD, NEGATIVE SUPERVISION, TRAINING, PUBLICATION, OR AUTHORITY EFFECT")
        }
    }

    fun execute(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1 {
        if (!request.enabled) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Disabled
        return try {
            validateBatchId(request.validationBatchId)
            if (request.validationBatchId.isBlank()) fail(PersistenceFailureReason.INVALID_VALIDATION_BATCH_ID, "batchId")
            requireValidBatch(request.validationBatch)
            val durableRoot = safeRoot(request.durableRoot, PersistenceFailureReason.UNSAFE_DURABLE_ROOT)
            val reportRoot = safeRoot(request.reportRoot, PersistenceFailureReason.UNSAFE_REPORT_ROOT)
            val paths = resolvePaths(durableRoot, reportRoot, request.validationBatchId)
            val durableBytes = serializeValidationBatch(request.validationBatch)
            val report = buildReport(request.validationBatch, request.validationBatchId, paths.durableRelativePath, paths.reportJsonRelativePath, paths.reportTextRelativePath, durableBytes)
            val reportJsonBytes = serializeReport(report)
            val reportTextBytes = renderReportText(report).toByteArray(StandardCharsets.UTF_8)
            publishOrReuse(paths, request.validationBatch, durableBytes, reportJsonBytes, reportTextBytes, report)
        } catch (failure: PersistenceFailure) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Failed(
                failure.reason,
                failure.safeContext,
            )
        } catch (_: Throwable) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Failed(
                PersistenceFailureReason.WRITE_FAILED,
                "persistence",
            )
        }
    }

    private fun publishOrReuse(
        paths: ResolvedPaths,
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
        durableBytes: ByteArray,
        reportJsonBytes: ByteArray,
        reportTextBytes: ByteArray,
        report: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Completed {
        val exists = listOf(paths.durable, paths.reportJson, paths.reportText).map { path ->
            if (Files.isSymbolicLink(path)) fail(PersistenceFailureReason.UNSAFE_TARGET_PATH, "target")
            Files.exists(path, LinkOption.NOFOLLOW_LINKS)
        }
        if (exists.any { it } && !exists.all { it }) fail(PersistenceFailureReason.PARTIAL_ARTIFACT_STATE, "artifacts")
        if (exists.all { it }) {
            val existingDurable = readBytes(paths.durable, "durable")
            if (!existingDurable.contentEquals(durableBytes)) fail(PersistenceFailureReason.EXISTING_DURABLE_ARTIFACT_CONFLICT, "durable")
            val existingReportJson = readBytes(paths.reportJson, "reportJson")
            if (!existingReportJson.contentEquals(reportJsonBytes)) fail(PersistenceFailureReason.EXISTING_REPORT_ARTIFACT_CONFLICT, "reportJson")
            val existingReportText = readBytes(paths.reportText, "reportText")
            if (!existingReportText.contentEquals(reportTextBytes)) fail(PersistenceFailureReason.EXISTING_REPORT_ARTIFACT_CONFLICT, "reportText")
            val reloadedBatch = try { deserializeValidationBatch(existingDurable) } catch (_: Throwable) {
                fail(PersistenceFailureReason.RELOAD_MISMATCH, "durable")
            }
            if (reloadedBatch != batch) fail(PersistenceFailureReason.RELOAD_MISMATCH, "durable")
            try { deserializeReport(existingReportJson) } catch (_: Throwable) {
                fail(PersistenceFailureReason.RELOAD_MISMATCH, "reportJson")
            }
            return completed(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
                batch,
                report,
                durableBytes,
                reportJsonBytes,
                reportTextBytes,
                paths,
            )
        }
        publishNew(
            listOf(
                paths.durable to durableBytes,
                paths.reportJson to reportJsonBytes,
                paths.reportText to reportTextBytes,
            ),
        )
        val reloadedDurable = readBytes(paths.durable, "durable")
        val reloadedReportJson = readBytes(paths.reportJson, "reportJson")
        val reloadedReportText = readBytes(paths.reportText, "reportText")
        if (!reloadedDurable.contentEquals(durableBytes) || !reloadedReportJson.contentEquals(reportJsonBytes) || !reloadedReportText.contentEquals(reportTextBytes)) {
            fail(PersistenceFailureReason.BYTE_IDENTITY_MISMATCH, "reload")
        }
        try {
            if (deserializeValidationBatch(reloadedDurable) != batch || deserializeReport(reloadedReportJson) != report) {
                fail(PersistenceFailureReason.RELOAD_MISMATCH, "reload")
            }
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            fail(PersistenceFailureReason.RELOAD_MISMATCH, "reload")
        }
        return completed(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1.CREATED,
            batch,
            report,
            durableBytes,
            reportJsonBytes,
            reportTextBytes,
            paths,
        )
    }

    private fun completed(
        status: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1,
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
        report: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1,
        durableBytes: ByteArray,
        reportJsonBytes: ByteArray,
        reportTextBytes: ByteArray,
        paths: ResolvedPaths,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Completed(
        status = status,
        validationBatchId = report.validationBatchId,
        durableRelativePath = paths.durableRelativePath,
        reportJsonRelativePath = paths.reportJsonRelativePath,
        reportTextRelativePath = paths.reportTextRelativePath,
        validationBatchBindingDigest = batch.bindingDigest,
        validationBatchLogicalDigest = batch.logicalDigest,
        counters = batch.counters,
        durableJsonSha256 = sha256(durableBytes),
        durableJsonByteSize = durableBytes.size.toLong(),
        reportJsonSha256 = sha256(reportJsonBytes),
        reportJsonByteSize = reportJsonBytes.size.toLong(),
        reportTextSha256 = sha256(reportTextBytes),
        reportTextByteSize = reportTextBytes.size.toLong(),
        validationBatch = batch,
    )

    private fun buildReport(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
        validationBatchId: String,
        durableRelativePath: String,
        reportJsonRelativePath: String,
        reportTextRelativePath: String,
        durableBytes: ByteArray,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1 {
        val unsigned = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1(
            persistenceContractId = CONTRACT_ID,
            persistenceVersion = VERSION,
            validationContractId = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.CONTRACT_ID,
            validationContractVersion = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.VERSION,
            validationBatchId = validationBatchId,
            state = VALIDATION_STATE,
            validatorReviewerRef = batch.validatorReviewerRef,
            validationRound = batch.validationRound,
            validationRevision = batch.validationRevision,
            durableRelativePath = durableRelativePath,
            reportJsonRelativePath = reportJsonRelativePath,
            reportTextRelativePath = reportTextRelativePath,
            inputBindingDigest = batch.inputBinding.bindingDigest,
            validationBatchBindingDigest = batch.bindingDigest,
            validationBatchLogicalDigest = batch.logicalDigest,
            durableJsonSha256 = sha256(durableBytes),
            durableJsonByteSize = durableBytes.size.toLong(),
            counters = batch.counters,
            reportLogicalDigest = "",
        )
        return unsigned.copy(reportLogicalDigest = reportLogicalDigest(unsigned))
    }

    private fun reportLogicalDigest(
        report: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1,
    ): String = sha256(REPORT_LOGICAL_DIGEST_DOMAIN + "\n" + gson.toJson(report.copy(reportLogicalDigest = "")))

    private fun requireValidBatch(batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1) {
        when (val validation = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validate(batch)) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Invalid ->
                fail(mapFailure(validation.reason), "validationBatch")
        }
    }

    private fun mapFailure(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1,
    ): PersistenceFailureReason = when (reason) {
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_INPUT_BINDING -> PersistenceFailureReason.INVALID_INPUT_BINDING
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INPUT_BINDING_DIGEST_MISMATCH -> PersistenceFailureReason.INPUT_BINDING_DIGEST_MISMATCH
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATOR_NOT_INDEPENDENT -> PersistenceFailureReason.VALIDATOR_NOT_INDEPENDENT
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATOR_REVIEWER_REF -> PersistenceFailureReason.INVALID_VALIDATOR
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.DUPLICATE_VALIDATION_IDENTITY -> PersistenceFailureReason.DUPLICATE_VALIDATION_IDENTITY
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_COUNTERS -> PersistenceFailureReason.INVALID_COUNTERS
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATION_BINDING_DIGEST_MISMATCH -> PersistenceFailureReason.VALIDATION_BINDING_DIGEST_MISMATCH
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATION_LOGICAL_DIGEST_MISMATCH -> PersistenceFailureReason.VALIDATION_LOGICAL_DIGEST_MISMATCH
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_RECORD_ID,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_ROUND,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_REVISION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RATIONALE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_EVIDENCE_REFERENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.CROSS_UNIT_EVIDENCE_REFERENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INSUFFICIENT_DIRECT_EVIDENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.UNADDRESSED_CONTRADICTING_EVIDENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_DOWNSTREAM_ROUTE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_ASSESSMENT,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.UNKNOWN_REVIEW_UNIT,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.ORIGINAL_DECISION_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.ORIGINAL_DECISION_IDENTITY_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RECORD_COUNT,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RECORD_ORDER -> PersistenceFailureReason.INVALID_VALIDATION_RECORD
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_CONTRACT_ID,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_CONTRACT_VERSION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_STATE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_BATCH_BINDING -> PersistenceFailureReason.INVALID_VALIDATION_BATCH
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATION_RECORD_DIGEST_MISMATCH -> PersistenceFailureReason.VALIDATION_RECORD_DIGEST_MISMATCH
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.FORBIDDEN_GOLD_SEMANTICS -> PersistenceFailureReason.FORBIDDEN_GOLD_SEMANTICS
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.FORBIDDEN_NEGATIVE_SUPERVISION_SEMANTICS -> PersistenceFailureReason.FORBIDDEN_NEGATIVE_SUPERVISION_SEMANTICS
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.FORBIDDEN_AUTHORITY_MUTATION_SEMANTICS -> PersistenceFailureReason.FORBIDDEN_AUTHORITY_MUTATION_SEMANTICS
    }

    private fun requireValidReport(report: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1) {
        if (report.persistenceContractId != CONTRACT_ID || report.persistenceVersion != VERSION ||
            report.validationContractId != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.CONTRACT_ID ||
            report.validationContractVersion != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.VERSION ||
            report.state != VALIDATION_STATE || !BATCH_ID.matches(report.validationBatchId) ||
            report.durableRelativePath != "${report.validationBatchId}/$DURABLE_FILE_NAME" ||
            report.reportJsonRelativePath != "${report.validationBatchId}/$REPORT_JSON_FILE_NAME" ||
            report.reportTextRelativePath != "${report.validationBatchId}/$REPORT_TEXT_FILE_NAME" ||
            !SHA256.matches(report.inputBindingDigest) || !SHA256.matches(report.validationBatchBindingDigest) ||
            !SHA256.matches(report.validationBatchLogicalDigest) || !SHA256.matches(report.durableJsonSha256) ||
            report.durableJsonByteSize <= 0 || !SHA256.matches(report.reportLogicalDigest) ||
            !Regex("[A-Za-z0-9._:-]{1,128}").matches(report.validatorReviewerRef) ||
            report.validationRound != 1 || report.validationRevision != 1
        ) fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "report")
        if (report.validatorReviewerRef == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.ORIGINAL_REVIEWER_REF) {
            fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "report")
        }
        val counters = report.counters
        if (listOf(
                counters.totalValidationRecords,
                counters.independentlyValidatedRecords,
                counters.challengedRecords,
                counters.abstainedRecords,
                counters.escalatedRecords,
                counters.potentialPositiveGoldCandidates,
                counters.potentialNegativeSupervisionCandidates,
                counters.requiresAdjudicationRecords,
                counters.notEligibleRecords,
                counters.distinctReviewUnits,
                counters.distinctOriginalDecisionIdentities,
                counters.referencedEvidenceCount,
                counters.distinctReferencedEvidenceCount,
            ).any { it < 0 } ||
            counters.independentlyValidatedRecords + counters.challengedRecords + counters.abstainedRecords + counters.escalatedRecords != counters.totalValidationRecords ||
            counters.potentialPositiveGoldCandidates + counters.potentialNegativeSupervisionCandidates + counters.requiresAdjudicationRecords + counters.notEligibleRecords != counters.totalValidationRecords ||
            counters.distinctReviewUnits > counters.totalValidationRecords ||
            counters.distinctOriginalDecisionIdentities > counters.totalValidationRecords ||
            counters.distinctReferencedEvidenceCount > counters.referencedEvidenceCount
        ) fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "report")
        if (report.reportLogicalDigest != reportLogicalDigest(report.copy(reportLogicalDigest = ""))) {
            fail(PersistenceFailureReason.DESERIALIZATION_FAILED, "report")
        }
    }

    private fun validateBatchId(batchId: String) {
        if (!BATCH_ID.matches(batchId) || batchId.endsWith('-') || batchId.contains("..") || batchId.any { it.isWhitespace() }) {
            fail(PersistenceFailureReason.INVALID_VALIDATION_BATCH_ID, "batchId")
        }
    }

    private fun safeRoot(root: File, unsafeReason: PersistenceFailureReason): Path {
        val normalized = try { root.toPath().toAbsolutePath().normalize() } catch (_: Throwable) {
            fail(unsafeReason, "root")
        }
        if (Files.isSymbolicLink(normalized)) fail(unsafeReason, "root")
        return normalized
    }

    private fun resolvePaths(durableRoot: Path, reportRoot: Path, batchId: String): ResolvedPaths {
        val durableDirectory = resolveInside(durableRoot, batchId, PersistenceFailureReason.UNSAFE_TARGET_PATH)
        val reportDirectory = resolveInside(reportRoot, batchId, PersistenceFailureReason.UNSAFE_TARGET_PATH)
        return ResolvedPaths(
            durable = durableDirectory.resolve(DURABLE_FILE_NAME),
            reportJson = reportDirectory.resolve(REPORT_JSON_FILE_NAME),
            reportText = reportDirectory.resolve(REPORT_TEXT_FILE_NAME),
            durableRelativePath = "$batchId/$DURABLE_FILE_NAME",
            reportJsonRelativePath = "$batchId/$REPORT_JSON_FILE_NAME",
            reportTextRelativePath = "$batchId/$REPORT_TEXT_FILE_NAME",
        )
    }

    private fun resolveInside(root: Path, child: String, reason: PersistenceFailureReason): Path {
        val resolved = root.resolve(child).normalize()
        if (!resolved.startsWith(root) || hasSymlinkComponentWithin(resolved, root)) fail(reason, "target")
        return resolved
    }

    private fun hasSymlinkComponentWithin(path: Path, boundary: Path): Boolean {
        val relative = try { boundary.relativize(path) } catch (_: Throwable) { return true }
        var probe = boundary
        for (part in relative) {
            probe = probe.resolve(part)
            if (Files.isSymbolicLink(probe)) return true
        }
        return false
    }

    private fun publishNew(files: List<Pair<Path, ByteArray>>) {
        val published = mutableListOf<Path>()
        val temporary = mutableListOf<Path>()
        try {
            val prepared = files.map { (target, bytes) ->
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) fail(PersistenceFailureReason.EXISTING_REPORT_ARTIFACT_CONFLICT, "target")
                val parent = target.parent ?: fail(PersistenceFailureReason.WRITE_FAILED, "parent")
                try { Files.createDirectories(parent) } catch (_: Throwable) { fail(PersistenceFailureReason.WRITE_FAILED, "parent") }
                if (Files.isSymbolicLink(parent)) fail(PersistenceFailureReason.UNSAFE_TARGET_PATH, "target")
                val temp = try {
                    Files.createTempFile(parent, TEMP_FILE_PREFIX, ".tmp")
                } catch (_: Throwable) {
                    fail(PersistenceFailureReason.WRITE_FAILED, "temporary")
                }
                temporary.add(temp)
                try {
                    FileOutputStream(temp.toFile()).use { output ->
                        output.write(bytes)
                        output.fd.sync()
                    }
                    if (!Files.readAllBytes(temp).contentEquals(bytes)) fail(PersistenceFailureReason.BYTE_IDENTITY_MISMATCH, "temporary")
                } catch (failure: PersistenceFailure) {
                    throw failure
                } catch (_: Throwable) {
                    fail(PersistenceFailureReason.WRITE_FAILED, "temporary")
                }
                target to temp
            }
            prepared.forEach { (target, temp) ->
                try {
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: AtomicMoveNotSupportedException) {
                    try { Files.move(temp, target) } catch (_: Throwable) {
                        fail(PersistenceFailureReason.ATOMIC_PUBLICATION_FAILED, "publish")
                    }
                } catch (_: Throwable) {
                    fail(PersistenceFailureReason.ATOMIC_PUBLICATION_FAILED, "publish")
                }
                published.add(target)
            }
        } catch (failure: PersistenceFailure) {
            published.asReversed().forEach { path -> try { Files.deleteIfExists(path) } catch (_: Throwable) { } }
            temporary.forEach { path -> try { Files.deleteIfExists(path) } catch (_: Throwable) { } }
            throw failure
        } catch (_: Throwable) {
            published.asReversed().forEach { path -> try { Files.deleteIfExists(path) } catch (_: Throwable) { } }
            temporary.forEach { path -> try { Files.deleteIfExists(path) } catch (_: Throwable) { } }
            fail(PersistenceFailureReason.ATOMIC_PUBLICATION_FAILED, "publish")
        }
    }

    private fun readBytes(path: Path, context: String): ByteArray = try {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) fail(PersistenceFailureReason.READ_FAILED, context)
        Files.readAllBytes(path)
    } catch (failure: PersistenceFailure) {
        throw failure
    } catch (_: Throwable) {
        fail(PersistenceFailureReason.READ_FAILED, context)
    }

    private fun StringBuilder.appendCounters(
        counters: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationCountersV1,
    ) {
        appendLine("counter.totalValidationRecords=${counters.totalValidationRecords}")
        appendLine("counter.independentlyValidatedRecords=${counters.independentlyValidatedRecords}")
        appendLine("counter.challengedRecords=${counters.challengedRecords}")
        appendLine("counter.abstainedRecords=${counters.abstainedRecords}")
        appendLine("counter.escalatedRecords=${counters.escalatedRecords}")
        appendLine("counter.potentialPositiveGoldCandidates=${counters.potentialPositiveGoldCandidates}")
        appendLine("counter.potentialNegativeSupervisionCandidates=${counters.potentialNegativeSupervisionCandidates}")
        appendLine("counter.requiresAdjudicationRecords=${counters.requiresAdjudicationRecords}")
        appendLine("counter.notEligibleRecords=${counters.notEligibleRecords}")
        appendLine("counter.distinctReviewUnits=${counters.distinctReviewUnits}")
        appendLine("counter.distinctOriginalDecisionIdentities=${counters.distinctOriginalDecisionIdentities}")
        appendLine("counter.referencedEvidenceCount=${counters.referencedEvidenceCount}")
        appendLine("counter.distinctReferencedEvidenceCount=${counters.distinctReferencedEvidenceCount}")
    }

    private fun parseStrictJson(bytes: ByteArray): JsonElement {
        val reader = JsonReader(StringReader(bytes.toString(StandardCharsets.UTF_8)))
        reader.isLenient = false
        val result = readJson(reader)
        if (reader.peek() != JsonToken.END_DOCUMENT) throw IllegalArgumentException("trailing")
        return result
    }

    private fun readJson(reader: JsonReader): JsonElement = when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> {
            reader.beginObject()
            val result = JsonObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (result.has(name)) throw IllegalArgumentException("duplicate")
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
        else -> throw IllegalArgumentException("json")
    }

    private fun requireKeys(root: JsonObject, allowed: Set<String>) {
        if (root.keySet() != allowed) throw IllegalArgumentException("fields")
    }

    private fun requireNestedKeys(root: JsonObject) {
        val binding = root.getAsJsonObject("inputBinding")
        requireKeys(binding, INPUT_BINDING_KEYS)
        requireKeys(binding.getAsJsonObject("batchFileBinding"), FILE_BINDING_KEYS)
        binding.getAsJsonArray("originalSelections").forEach { selection -> requireKeys(selection.asJsonObject, SELECTION_KEYS) }
        binding.getAsJsonArray("evidenceBindings").forEach { evidence -> requireKeys(evidence.asJsonObject, EVIDENCE_BINDING_KEYS) }
        root.getAsJsonArray("records").forEach { record ->
            val item = record.asJsonObject
            requireKeys(item, RECORD_KEYS)
            requireKeys(item.getAsJsonObject("originalDecision"), SELECTION_KEYS)
        }
        requireKeys(root.getAsJsonObject("counters"), COUNTER_KEYS)
    }

    private fun requireReportNestedKeys(root: JsonObject) {
        requireKeys(root.getAsJsonObject("counters"), COUNTER_KEYS)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    private fun fail(reason: PersistenceFailureReason, context: String): Nothing = throw PersistenceFailure(reason, context)

    private data class ResolvedPaths(
        val durable: Path,
        val reportJson: Path,
        val reportText: Path,
        val durableRelativePath: String,
        val reportJsonRelativePath: String,
        val reportTextRelativePath: String,
    )

    private data class PersistenceFailure(
        val reason: PersistenceFailureReason,
        val safeContext: String,
    ) : RuntimeException()

    private val BATCH_KEYS = setOf(
        "contractId", "version", "state", "inputBinding", "validatorReviewerRef", "validationRound",
        "validationRevision", "records", "counters", "bindingDigest", "logicalDigest",
    )
    private val INPUT_BINDING_KEYS = setOf(
        "batchId", "submissionId", "batchFileBinding", "originalInputBindingDigest", "originalBatchLogicalDigest",
        "originalReviewerRef", "originalReviewRound", "originalRevision", "originalSelections", "evidenceBindings", "bindingDigest",
    )
    private val FILE_BINDING_KEYS = setOf("relativePath", "byteSize", "sha256", "logicalDigest")
    private val SELECTION_KEYS = setOf(
        "reviewUnitId", "stableEntryId", "canonicalEntityId", "decision", "reasonCodes", "evidenceReferenceIds",
        "revision", "alternativeCanonicalProposal", "reviewerNote",
    )
    private val EVIDENCE_BINDING_KEYS = setOf("reviewUnitId", "evidenceReferenceId", "kind", "directness", "position")
    private val RECORD_KEYS = setOf(
        "validationRecordId", "originalDecision", "validatorReviewerRef", "validationRound", "validationRevision",
        "assessment", "reasonCodes", "evidenceReferenceIds", "rationale",
    )
    private val COUNTER_KEYS = setOf(
        "totalValidationRecords", "independentlyValidatedRecords", "challengedRecords", "abstainedRecords", "escalatedRecords",
        "potentialPositiveGoldCandidates", "potentialNegativeSupervisionCandidates", "requiresAdjudicationRecords", "notEligibleRecords",
        "distinctReviewUnits", "distinctOriginalDecisionIdentities", "referencedEvidenceCount", "distinctReferencedEvidenceCount",
    )
    private val REPORT_KEYS = setOf(
        "persistenceContractId", "persistenceVersion", "validationContractId", "validationContractVersion", "validationBatchId",
        "state", "validatorReviewerRef", "validationRound", "validationRevision", "durableRelativePath", "reportJsonRelativePath",
        "reportTextRelativePath", "inputBindingDigest", "validationBatchBindingDigest", "validationBatchLogicalDigest",
        "durableJsonSha256", "durableJsonByteSize", "counters", "reportLogicalDigest",
    )
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1 {
    CREATED,
    ALREADY_PRESENT_IDENTICAL,
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1 {
    INVALID_VALIDATION_BATCH_ID,
    UNSAFE_DURABLE_ROOT,
    UNSAFE_REPORT_ROOT,
    UNSAFE_TARGET_PATH,
    INVALID_VALIDATION_BATCH,
    INVALID_INPUT_BINDING,
    INPUT_BINDING_DIGEST_MISMATCH,
    INVALID_VALIDATOR,
    VALIDATOR_NOT_INDEPENDENT,
    INVALID_VALIDATION_RECORD,
    DUPLICATE_VALIDATION_IDENTITY,
    INVALID_COUNTERS,
    VALIDATION_RECORD_DIGEST_MISMATCH,
    VALIDATION_BINDING_DIGEST_MISMATCH,
    VALIDATION_LOGICAL_DIGEST_MISMATCH,
    PARTIAL_ARTIFACT_STATE,
    EXISTING_DURABLE_ARTIFACT_CONFLICT,
    EXISTING_REPORT_ARTIFACT_CONFLICT,
    SERIALIZATION_FAILED,
    DESERIALIZATION_FAILED,
    READ_FAILED,
    WRITE_FAILED,
    ATOMIC_PUBLICATION_FAILED,
    ROLLBACK_FAILED,
    RELOAD_MISMATCH,
    BYTE_IDENTITY_MISMATCH,
    FORBIDDEN_GOLD_SEMANTICS,
    FORBIDDEN_NEGATIVE_SUPERVISION_SEMANTICS,
    FORBIDDEN_TRAINING_SEMANTICS,
    FORBIDDEN_AUTHORITY_MUTATION_SEMANTICS,
}

private typealias PersistenceFailureReason = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1 {
    data object Disabled : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1

    data class Completed(
        val status: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1,
        val validationBatchId: String,
        val durableRelativePath: String,
        val reportJsonRelativePath: String,
        val reportTextRelativePath: String,
        val validationBatchBindingDigest: String,
        val validationBatchLogicalDigest: String,
        val counters: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationCountersV1,
        val durableJsonSha256: String,
        val durableJsonByteSize: Long,
        val reportJsonSha256: String,
        val reportJsonByteSize: Long,
        val reportTextSha256: String,
        val reportTextByteSize: Long,
        val validationBatch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceRequestV1(
    val enabled: Boolean,
    val durableRoot: File,
    val reportRoot: File,
    val validationBatchId: String,
    val validationBatch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceReportV1(
    val persistenceContractId: String,
    val persistenceVersion: String,
    val validationContractId: String,
    val validationContractVersion: String,
    val validationBatchId: String,
    val state: String,
    val validatorReviewerRef: String,
    val validationRound: Int,
    val validationRevision: Int,
    val durableRelativePath: String,
    val reportJsonRelativePath: String,
    val reportTextRelativePath: String,
    val inputBindingDigest: String,
    val validationBatchBindingDigest: String,
    val validationBatchLogicalDigest: String,
    val durableJsonSha256: String,
    val durableJsonByteSize: Long,
    val counters: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationCountersV1,
    val reportLogicalDigest: String,
)
