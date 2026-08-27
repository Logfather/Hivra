package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object HimZeroCandidateRecoveryHumanReviewPersistenceV1 {
    const val CONTRACT_ID = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_PERSISTENCE_V1"
    const val PERSISTENCE_CONTRACT_ID = CONTRACT_ID
    const val VERSION = "1"
    const val PERSISTENCE_VERSION = VERSION
    const val BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_PERSISTENCE_BINDING_V1"
    const val BATCH_LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_DECISION_BATCH_LOGICAL_V1"

    private const val BATCH_FILE_NAME = "review-decisions.v1.json"
    private const val REPORT_DIRECTORY = "batches"
    private const val REPORT_JSON_FILE_NAME = "persistence-report.v1.json"
    private const val REPORT_TEXT_FILE_NAME = "persistence-report.v1.txt"
    private val BATCH_ID = Regex("[a-z0-9][a-z0-9-]{0,63}")
    private val SHA256 = Regex("[0-9a-f]{64}")

    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .create()

    fun bindingDigest(binding: HimZeroCandidateRecoveryHumanReviewInputBindingV1): String =
        sha256(
            BINDING_DIGEST_DOMAIN + "\n" +
                gson.toJson(binding.copy(bindingDigest = "")),
        )

    fun batchLogicalDigest(batch: HimZeroCandidateRecoveryHumanReviewDecisionBatchV1): String =
        sha256(
            BATCH_LOGICAL_DIGEST_DOMAIN + "\n" +
                gson.toJson(batch.copy(batchLogicalDigest = "")),
        )

    fun serializeBatch(batch: HimZeroCandidateRecoveryHumanReviewDecisionBatchV1): ByteArray {
        val canonical = canonicalize(batch)
        canonical.validate()
        return (gson.toJson(canonical) + "\n").toByteArray(StandardCharsets.UTF_8)
    }

    fun deserializeBatch(bytes: ByteArray): HimZeroCandidateRecoveryHumanReviewDecisionBatchV1 =
        try {
            val root = JsonParser.parseString(bytes.toString(StandardCharsets.UTF_8)).asJsonObject
            requireAllowedKeys(root, BATCH_KEYS)
            val batch = requireNotNull(gson.fromJson(root, HimZeroCandidateRecoveryHumanReviewDecisionBatchV1::class.java)) {
                "DESERIALIZATION_FAILED"
            }
            batch.validate()
            batch
        } catch (_: PersistenceFailure) {
            throw IllegalArgumentException("DESERIALIZATION_FAILED")
        } catch (_: Throwable) {
            throw IllegalArgumentException("DESERIALIZATION_FAILED")
        }

    fun readBatch(file: File): HimZeroCandidateRecoveryHumanReviewDecisionBatchV1 {
        if (!file.isFile) throw IllegalArgumentException("READ_FAILED")
        return deserializeBatch(file.readBytes())
    }

    fun deriveCounters(
        records: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
    ): HimZeroCandidateRecoveryHumanReviewPersistenceCountersV1 {
        val decisionBreakdown = HimZeroCandidateRecoveryHumanReviewDecisionV1.entries.map { decision ->
            HimZeroCandidateRecoveryHumanReviewDecisionCounterV1(
                decision = decision,
                records = records.count { it.decision == decision },
            )
        }
        val routeBreakdown = HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.entries.map { route ->
            HimZeroCandidateRecoveryHumanReviewRouteCounterV1(
                route = route,
                records = records.count { it.downstreamRoute == route },
            )
        }
        return HimZeroCandidateRecoveryHumanReviewPersistenceCountersV1(
            decisionRecords = records.size,
            uniqueReviewUnits = records.map { it.reviewUnit.reviewUnitId }.distinct().size,
            uniqueReviewers = records.map { it.reviewerRef }.distinct().size,
            decisionBreakdown = decisionBreakdown,
            downstreamRouteBreakdown = routeBreakdown,
            alternativeCanonicalProposals = records.count { it.alternativeCanonicalProposal != null },
            abstainedRecords = records.count {
                it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_INSUFFICIENT_EVIDENCE ||
                    it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS ||
                    it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_CONFLICTING_EVIDENCE
            },
            escalatedRecords = records.count {
                it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE
            },
        )
    }

    fun execute(
        request: HimZeroCandidateRecoveryHumanReviewPersistenceRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewPersistenceResultV1 = try {
        val prepared = prepare(request)
        val durableRoot = safeRoot(request.durableDecisionBatchRoot)
        val derivedRoot = safeRoot(request.derivedReportRoot)
        val durableFile = resolveInside(durableRoot, "${prepared.batch.batchId}/$BATCH_FILE_NAME")
        val reportDirectory = resolveInside(derivedRoot, "$REPORT_DIRECTORY/${prepared.batch.batchId}")
        val reportJson = reportDirectory.resolve(REPORT_JSON_FILE_NAME)
        val reportText = reportDirectory.resolve(REPORT_TEXT_FILE_NAME)
        val durableExists = durableFile.exists()
        val jsonExists = reportJson.exists()
        val textExists = reportText.exists()

        when {
            !durableExists && !jsonExists && !textExists -> {
                val reports = reportsFor(prepared, HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1.CREATED)
                publishNew(
                    listOf(
                        durableFile to prepared.batchBytes,
                        reportJson to reports.jsonBytes,
                        reportText to reports.textBytes,
                    ),
                )
                completed(
                    HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1.CREATED,
                    prepared.batch,
                    reports.report,
                )
            }
            durableExists && !jsonExists && !textExists -> {
                requireExistingBatchIsIdentical(durableFile, prepared.batchBytes)
                val reports = reportsFor(prepared, HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL)
                publishNew(listOf(reportJson to reports.jsonBytes, reportText to reports.textBytes))
                completed(
                    HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
                    prepared.batch,
                    reports.report,
                )
            }
            durableExists && jsonExists && textExists -> {
                requireExistingBatchIsIdentical(durableFile, prepared.batchBytes)
                val existingJson = reportJson.readBytes()
                val existingText = reportText.readBytes()
                val existingReport = readReport(existingJson)
                val expectedJson = reportsFor(
                    prepared,
                    existingReport.persistenceStatus,
                )
                require(existingJson.contentEquals(expectedJson.jsonBytes)) {
                    fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.EXISTING_REPORT_CONFLICT, "reportJson")
                }
                require(existingText.contentEquals(expectedJson.textBytes)) {
                    fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.EXISTING_REPORT_CONFLICT, "reportText")
                }
                completed(
                    HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
                    prepared.batch,
                    existingReport,
                )
            }
            else -> fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.PARTIAL_ARTIFACT_STATE, "artifactPair")
        }
    } catch (failure: PersistenceFailure) {
        HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Failed(failure.reason, failure.safeContext)
    } catch (_: Throwable) {
        HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Failed(
            HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.WRITE_FAILED,
            "persistence",
        )
    }

    private fun prepare(
        request: HimZeroCandidateRecoveryHumanReviewPersistenceRequestV1,
    ): PreparedBatch {
        validateBatchId(request.batch.batchId)
        validateInputBinding(request.batch.inputBinding)
        val normalizedRecords = request.batch.decisionRecords.map(::normalizeRecord)
        normalizedRecords.forEach { record ->
            when (val validation = record.validate()) {
                HimZeroCandidateRecoveryHumanReviewValidationResultV1.Valid -> Unit
                is HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid ->
                    fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_DECISION_RECORD, validation.error.name)
            }
        }
        if (normalizedRecords.isEmpty()) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.EMPTY_DECISION_BATCH, "records")
        }
        val identities = normalizedRecords.map { identityOf(it) }
        if (identities.distinct().size != identities.size) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.DUPLICATE_DECISION_IDENTITY, "records")
        }
        if (request.batch.counters != deriveCounters(normalizedRecords)) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_COUNTERS, "counters")
        }
        val sortedRecords = normalizedRecords.sortedWith(compareBy({ it.reviewUnit.reviewUnitId }, { it.reviewerRef }, { it.reviewRound }, { it.revision }))
        val counters = deriveCounters(sortedRecords)
        val unsigned = request.batch.copy(
            persistenceContractId = CONTRACT_ID,
            persistenceVersion = VERSION,
            decisionRecords = sortedRecords,
            counters = counters,
            bindingDigest = bindingDigest(request.batch.inputBinding),
            batchLogicalDigest = "",
        )
        if (request.batch.persistenceContractId != CONTRACT_ID || request.batch.persistenceVersion != VERSION) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_INPUT_BINDING, "contract")
        }
        if (request.batch.bindingDigest != unsigned.bindingDigest) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INPUT_BINDING_DIGEST_MISMATCH, "batchBinding")
        }
        val batch = unsigned.copy(batchLogicalDigest = batchLogicalDigest(unsigned))
        if (request.batch.batchLogicalDigest != batch.batchLogicalDigest) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.BATCH_LOGICAL_DIGEST_MISMATCH, "batchLogical")
        }
        batch.validate()
        return PreparedBatch(batch, serializeBatch(batch))
    }

    private fun validateBatch(
        batch: HimZeroCandidateRecoveryHumanReviewDecisionBatchV1,
    ) {
        validateBatchId(batch.batchId)
        if (batch.persistenceContractId != CONTRACT_ID || batch.persistenceVersion != VERSION) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_INPUT_BINDING, "contract")
        }
        validateInputBinding(batch.inputBinding)
        if (batch.decisionRecords.isEmpty()) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.EMPTY_DECISION_BATCH, "records")
        }
        val normalized = batch.decisionRecords.map(::normalizeRecord)
        if (normalized != batch.decisionRecords) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_DECISION_RECORD, "ordering")
        }
        normalized.forEach { record ->
            if (record.validate() !is HimZeroCandidateRecoveryHumanReviewValidationResultV1.Valid) {
                fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_DECISION_RECORD, "record")
            }
        }
        val identities = normalized.map { identityOf(it) }
        if (identities.distinct().size != identities.size) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.DUPLICATE_DECISION_IDENTITY, "records")
        }
        if (batch.counters != deriveCounters(normalized)) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_COUNTERS, "counters")
        }
        if (batch.bindingDigest != bindingDigest(batch.inputBinding)) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INPUT_BINDING_DIGEST_MISMATCH, "batchBinding")
        }
        if (!SHA256.matches(batch.batchLogicalDigest) || batchLogicalDigest(batch) != batch.batchLogicalDigest) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.BATCH_LOGICAL_DIGEST_MISMATCH, "batchLogical")
        }
    }

    private fun validateInputBinding(
        binding: HimZeroCandidateRecoveryHumanReviewInputBindingV1,
    ) {
        if (!binding.validate().valid) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_INPUT_BINDING, "inputBinding")
        }
        try {
            binding.existingCorpusInputBinding.validate()
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_INPUT_BINDING, "corpusBinding")
        }
        if (binding.corpusFileBinding.logicalDigest != binding.corpusLogicalDigest) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_INPUT_BINDING, "corpusDigest")
        }
        if (binding.bindingDigest != bindingDigest(binding)) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INPUT_BINDING_DIGEST_MISMATCH, "inputBinding")
        }
    }

    private fun normalizeRecord(
        record: HimZeroCandidateRecoveryHumanReviewDecisionRecordV1,
    ): HimZeroCandidateRecoveryHumanReviewDecisionRecordV1 = record.copy(
        reasonCodes = record.reasonCodes.sortedBy { it.ordinal },
        evidenceReferences = record.evidenceReferences
            .map { it.copy(fieldReferences = it.fieldReferences.sorted()) }
            .sortedBy { it.evidenceReferenceId },
    )

    private fun canonicalize(
        batch: HimZeroCandidateRecoveryHumanReviewDecisionBatchV1,
    ): HimZeroCandidateRecoveryHumanReviewDecisionBatchV1 = batch.copy(
        decisionRecords = batch.decisionRecords.map(::normalizeRecord)
            .sortedWith(compareBy({ it.reviewUnit.reviewUnitId }, { it.reviewerRef }, { it.reviewRound }, { it.revision })),
    )

    private fun identityOf(
        record: HimZeroCandidateRecoveryHumanReviewDecisionRecordV1,
    ): String = listOf(
        record.reviewUnit.reviewUnitId,
        record.reviewerRef,
        record.reviewRound.toString(),
        record.revision.toString(),
    ).joinToString("\u0000")

    private fun validateBatchId(batchId: String) {
        if (!BATCH_ID.matches(batchId) || batchId.contains("..") || batchId.any { it.isWhitespace() }) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.INVALID_BATCH_ID, "batchId")
        }
    }

    private fun safeRoot(root: File): Path = root.toPath().toAbsolutePath().normalize()

    private fun resolveInside(root: Path, relative: String): File {
        val resolved = root.resolve(relative).normalize()
        if (!resolved.startsWith(root)) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.UNSAFE_TARGET_PATH, "target")
        }
        return resolved.toFile()
    }

    private fun requireExistingBatchIsIdentical(file: File, expected: ByteArray) {
        val actual = try {
            file.readBytes()
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.READ_FAILED, "batch")
        }
        if (!actual.contentEquals(expected)) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.EXISTING_BATCH_CONFLICT, "batch")
        }
        try {
            deserializeBatch(actual)
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.DESERIALIZATION_FAILED, "batch")
        }
    }

    private fun readReport(bytes: ByteArray): HimZeroCandidateRecoveryHumanReviewPersistenceReportV1 = try {
        val root = JsonParser.parseString(bytes.toString(StandardCharsets.UTF_8)).asJsonObject
        requireAllowedKeys(root, REPORT_KEYS)
        val report = requireNotNull(gson.fromJson(root, HimZeroCandidateRecoveryHumanReviewPersistenceReportV1::class.java)) {
            "DESERIALIZATION_FAILED"
        }
        report.validate()
        report
    } catch (_: Throwable) {
        fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.DESERIALIZATION_FAILED, "report")
    }

    private fun reportsFor(
        prepared: PreparedBatch,
        status: HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1,
    ): PreparedReports {
        val batchRelativePath = "${prepared.batch.batchId}/$BATCH_FILE_NAME"
        val unsigned = HimZeroCandidateRecoveryHumanReviewPersistenceReportV1(
            persistenceContractId = CONTRACT_ID,
            persistenceVersion = VERSION,
            batchId = prepared.batch.batchId,
            decisionBatchRelativePath = batchRelativePath,
            batchSha256 = sha256(prepared.batchBytes),
            batchByteSize = prepared.batchBytes.size.toLong(),
            batchLogicalDigest = prepared.batch.batchLogicalDigest,
            bindingDigest = prepared.batch.bindingDigest,
            counters = prepared.batch.counters,
            persistenceStatus = status,
            reportLogicalDigest = "",
        )
        val report = unsigned.copy(reportLogicalDigest = reportLogicalDigest(unsigned))
        val jsonBytes = (gson.toJson(report) + "\n").toByteArray(StandardCharsets.UTF_8)
        val textBytes = reportText(report).toByteArray(StandardCharsets.UTF_8)
        return PreparedReports(report, jsonBytes, textBytes)
    }

    private fun reportLogicalDigest(
        report: HimZeroCandidateRecoveryHumanReviewPersistenceReportV1,
    ): String = sha256(
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_PERSISTENCE_REPORT_LOGICAL_V1\n" +
            gson.toJson(report.copy(reportLogicalDigest = "")),
    )

    private fun reportText(
        report: HimZeroCandidateRecoveryHumanReviewPersistenceReportV1,
    ): String = buildString {
        appendLine("persistenceContractId=${report.persistenceContractId}")
        appendLine("persistenceVersion=${report.persistenceVersion}")
        appendLine("batchId=${report.batchId}")
        appendLine("decisionBatchRelativePath=${report.decisionBatchRelativePath}")
        appendLine("batchSha256=${report.batchSha256}")
        appendLine("batchByteSize=${report.batchByteSize}")
        appendLine("batchLogicalDigest=${report.batchLogicalDigest}")
        appendLine("bindingDigest=${report.bindingDigest}")
        appendLine("decisionRecords=${report.counters.decisionRecords}")
        appendLine("uniqueReviewUnits=${report.counters.uniqueReviewUnits}")
        appendLine("uniqueReviewers=${report.counters.uniqueReviewers}")
        report.counters.decisionBreakdown.forEach { appendLine("decision.${it.decision.name}=${it.records}") }
        report.counters.downstreamRouteBreakdown.forEach { appendLine("route.${it.route.name}=${it.records}") }
        appendLine("alternativeCanonicalProposals=${report.counters.alternativeCanonicalProposals}")
        appendLine("abstainedRecords=${report.counters.abstainedRecords}")
        appendLine("escalatedRecords=${report.counters.escalatedRecords}")
        appendLine("persistenceStatus=${report.persistenceStatus.name}")
        appendLine("reportLogicalDigest=${report.reportLogicalDigest}")
    }

    private fun publishNew(files: List<Pair<File, ByteArray>>) {
        val published = mutableListOf<File>()
        val temporaries = mutableListOf<Path>()
        try {
            val prepared = files.map { (file, bytes) ->
                if (file.exists()) fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.EXISTING_REPORT_CONFLICT, "target")
                val parent = requireNotNull(file.parentFile) {
                    "WRITE_FAILED"
                }
                if (!parent.exists() && !parent.mkdirs()) {
                    fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.WRITE_FAILED, "parent")
                }
                val temporary = Files.createTempFile(parent.toPath(), ".${file.name}.", ".tmp")
                temporaries.add(temporary)
                try {
                    FileOutputStream(temporary.toFile()).use { output ->
                        output.write(bytes)
                        output.fd.sync()
                    }
                } catch (_: Throwable) {
                    Files.deleteIfExists(temporary)
                    fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.WRITE_FAILED, "temporary")
                }
                file to temporary
            }
            prepared.forEach { (file, temporary) ->
                try {
                    Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporary, file.toPath())
                } catch (_: Throwable) {
                    fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.ATOMIC_PUBLICATION_FAILED, "publish")
                }
                published += file
            }
        } catch (failure: PersistenceFailure) {
            published.asReversed().forEach { Files.deleteIfExists(it.toPath()) }
            temporaries.forEach { Files.deleteIfExists(it) }
            throw failure
        } catch (_: Throwable) {
            published.asReversed().forEach { Files.deleteIfExists(it.toPath()) }
            temporaries.forEach { Files.deleteIfExists(it) }
            fail(HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1.WRITE_FAILED, "publish")
        }
    }

    private fun completed(
        status: HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1,
        batch: HimZeroCandidateRecoveryHumanReviewDecisionBatchV1,
        report: HimZeroCandidateRecoveryHumanReviewPersistenceReportV1,
    ) = HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Completed(status, batch, report)

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .toHex()

    private fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun requireAllowedKeys(root: com.google.gson.JsonObject, allowed: Set<String>) {
        if (root.keySet() != allowed) throw IllegalArgumentException("UNKNOWN_FIELD")
    }

    private class PersistenceFailure(
        val reason: HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1,
        val safeContext: String,
    ) : IllegalArgumentException()

    private fun fail(
        reason: HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1,
        safeContext: String,
    ): Nothing = throw PersistenceFailure(reason, safeContext)

    private data class PreparedBatch(
        val batch: HimZeroCandidateRecoveryHumanReviewDecisionBatchV1,
        val batchBytes: ByteArray,
    )

    private data class PreparedReports(
        val report: HimZeroCandidateRecoveryHumanReviewPersistenceReportV1,
        val jsonBytes: ByteArray,
        val textBytes: ByteArray,
    )

    private val BATCH_KEYS = setOf(
        "persistenceContractId", "persistenceVersion", "batchId", "inputBinding", "decisionRecords",
        "counters", "bindingDigest", "batchLogicalDigest",
    )
    private val REPORT_KEYS = setOf(
        "persistenceContractId", "persistenceVersion", "batchId", "decisionBatchRelativePath",
        "batchSha256", "batchByteSize", "batchLogicalDigest", "bindingDigest", "counters",
        "persistenceStatus", "reportLogicalDigest",
    )
}

enum class HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1 {
    CREATED,
    ALREADY_PRESENT_IDENTICAL,
}

enum class HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1 {
    INVALID_BATCH_ID,
    UNSAFE_TARGET_PATH,
    EMPTY_DECISION_BATCH,
    INVALID_INPUT_BINDING,
    INPUT_BINDING_DIGEST_MISMATCH,
    INVALID_DECISION_RECORD,
    DUPLICATE_DECISION_IDENTITY,
    INVALID_COUNTERS,
    BATCH_LOGICAL_DIGEST_MISMATCH,
    PARTIAL_ARTIFACT_STATE,
    EXISTING_BATCH_CONFLICT,
    EXISTING_REPORT_CONFLICT,
    SERIALIZATION_FAILED,
    DESERIALIZATION_FAILED,
    READ_FAILED,
    WRITE_FAILED,
    ATOMIC_PUBLICATION_FAILED,
    RELOAD_MISMATCH,
}

sealed interface HimZeroCandidateRecoveryHumanReviewPersistenceResultV1 {
    data class Completed(
        val persistenceStatus: HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1,
        val batch: HimZeroCandidateRecoveryHumanReviewDecisionBatchV1,
        val report: HimZeroCandidateRecoveryHumanReviewPersistenceReportV1,
    ) : HimZeroCandidateRecoveryHumanReviewPersistenceResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewPersistenceFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateRecoveryHumanReviewPersistenceResultV1
}

data class HimZeroCandidateRecoveryHumanReviewPersistenceRequestV1(
    val durableDecisionBatchRoot: File,
    val derivedReportRoot: File,
    val batch: HimZeroCandidateRecoveryHumanReviewDecisionBatchV1,
)

data class HimZeroCandidateRecoveryHumanReviewDecisionBatchV1(
    val persistenceContractId: String,
    val persistenceVersion: String,
    val batchId: String,
    val inputBinding: HimZeroCandidateRecoveryHumanReviewInputBindingV1,
    val decisionRecords: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
    val counters: HimZeroCandidateRecoveryHumanReviewPersistenceCountersV1,
    val bindingDigest: String,
    val batchLogicalDigest: String,
) {
    fun validate() {
        require(persistenceContractId == HimZeroCandidateRecoveryHumanReviewPersistenceV1.CONTRACT_ID) {
            "INVALID_BATCH_CONTRACT"
        }
        require(persistenceVersion == HimZeroCandidateRecoveryHumanReviewPersistenceV1.VERSION) {
            "INVALID_BATCH_VERSION"
        }
        require(batchId.matches(Regex("[a-z0-9][a-z0-9-]{0,63}"))) { "INVALID_BATCH_ID" }
        require(inputBinding.validate().valid) { "INVALID_INPUT_BINDING" }
        require(decisionRecords.isNotEmpty()) { "EMPTY_DECISION_BATCH" }
        require(decisionRecords == decisionRecords.sortedWith(compareBy({ it.reviewUnit.reviewUnitId }, { it.reviewerRef }, { it.reviewRound }, { it.revision }))) {
            "INVALID_DECISION_ORDER"
        }
        decisionRecords.forEach { record ->
            require(record.validate().valid) { "INVALID_DECISION_RECORD" }
        }
        val identities = decisionRecords.map {
            listOf(it.reviewUnit.reviewUnitId, it.reviewerRef, it.reviewRound.toString(), it.revision.toString())
                .joinToString("\u0000")
        }
        require(identities.distinct().size == identities.size) { "DUPLICATE_DECISION_IDENTITY" }
        require(counters == HimZeroCandidateRecoveryHumanReviewPersistenceV1.deriveCounters(decisionRecords)) {
            "INVALID_COUNTERS"
        }
        require(bindingDigest == HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(inputBinding)) {
            "INPUT_BINDING_DIGEST_MISMATCH"
        }
        require(batchLogicalDigest.matches(Regex("[0-9a-f]{64}"))) { "BATCH_LOGICAL_DIGEST_MISMATCH" }
        require(batchLogicalDigest == HimZeroCandidateRecoveryHumanReviewPersistenceV1.batchLogicalDigest(copy(batchLogicalDigest = ""))) {
            "BATCH_LOGICAL_DIGEST_MISMATCH"
        }
    }
}

data class HimZeroCandidateRecoveryHumanReviewDecisionCounterV1(
    val decision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
    val records: Int,
)

data class HimZeroCandidateRecoveryHumanReviewRouteCounterV1(
    val route: HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1,
    val records: Int,
)

data class HimZeroCandidateRecoveryHumanReviewPersistenceCountersV1(
    val decisionRecords: Int,
    val uniqueReviewUnits: Int,
    val uniqueReviewers: Int,
    val decisionBreakdown: List<HimZeroCandidateRecoveryHumanReviewDecisionCounterV1>,
    val downstreamRouteBreakdown: List<HimZeroCandidateRecoveryHumanReviewRouteCounterV1>,
    val alternativeCanonicalProposals: Int,
    val abstainedRecords: Int,
    val escalatedRecords: Int,
)

data class HimZeroCandidateRecoveryHumanReviewPersistenceReportV1(
    val persistenceContractId: String,
    val persistenceVersion: String,
    val batchId: String,
    val decisionBatchRelativePath: String,
    val batchSha256: String,
    val batchByteSize: Long,
    val batchLogicalDigest: String,
    val bindingDigest: String,
    val counters: HimZeroCandidateRecoveryHumanReviewPersistenceCountersV1,
    val persistenceStatus: HimZeroCandidateRecoveryHumanReviewPersistenceStatusV1,
    val reportLogicalDigest: String,
) {
    fun validate() {
        require(persistenceContractId == HimZeroCandidateRecoveryHumanReviewPersistenceV1.CONTRACT_ID)
        require(persistenceVersion == HimZeroCandidateRecoveryHumanReviewPersistenceV1.VERSION)
        require(batchId.matches(Regex("[a-z0-9][a-z0-9-]{0,63}")))
        require(decisionBatchRelativePath == "$batchId/review-decisions.v1.json")
        require(!decisionBatchRelativePath.startsWith('/') && !decisionBatchRelativePath.contains('\\'))
        require(batchSha256.matches(Regex("[0-9a-f]{64}")))
        require(batchByteSize >= 0L)
        require(batchLogicalDigest.matches(Regex("[0-9a-f]{64}")))
        require(bindingDigest.matches(Regex("[0-9a-f]{64}")))
        require(reportLogicalDigest.matches(Regex("[0-9a-f]{64}")))
        require(reportLogicalDigest == reportDigest(this))
    }
}

private fun reportDigest(
    report: HimZeroCandidateRecoveryHumanReviewPersistenceReportV1,
): String = MessageDigest.getInstance("SHA-256")
    .digest(
        (
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_PERSISTENCE_REPORT_LOGICAL_V1\n" +
                GsonBuilder().disableHtmlEscaping().serializeNulls().create()
                    .toJson(report.copy(reportLogicalDigest = ""))
            ).toByteArray(StandardCharsets.UTF_8),
    ).joinToString("") { "%02x".format(it.toInt() and 0xff) }
