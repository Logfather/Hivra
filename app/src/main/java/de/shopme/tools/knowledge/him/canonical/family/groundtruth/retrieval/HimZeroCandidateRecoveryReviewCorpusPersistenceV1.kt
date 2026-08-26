package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object HimZeroCandidateRecoveryReviewCorpusPersistenceV1 {
    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    fun logicalDigest(report: HimZeroCandidateRecoveryReviewCorpusReportV1): String =
        sha256(gson.toJson(report.copy(logicalDigest = "")))

    fun bindingDigest(binding: HimZeroCandidateRecoveryReviewCorpusInputBindingV1): String =
        sha256(gson.toJson(binding.copy(bindingDigest = "")))

    fun serialize(report: HimZeroCandidateRecoveryReviewCorpusReportV1): ByteArray =
        (gson.toJson(report) + "\n").toByteArray(Charsets.UTF_8)

    fun readReport(file: File): HimZeroCandidateRecoveryReviewCorpusReportV1 {
        require(file.isFile) { "RECOVERY_REVIEW_REPORT_MISSING" }
        val report = requireNotNull(gson.fromJson(file.readText(Charsets.UTF_8), HimZeroCandidateRecoveryReviewCorpusReportV1::class.java)) {
            "RECOVERY_REVIEW_REPORT_INVALID"
        }
        report.validate()
        return report
    }

    fun writeReport(
        jsonFile: File,
        textFile: File,
        report: HimZeroCandidateRecoveryReviewCorpusReportV1,
    ) {
        report.validate()
        require(jsonFile.parentFile != null && textFile.parentFile != null) { "PERSISTENCE_PARENT_REQUIRED" }
        val json = serialize(report)
        val text = summary(report).toByteArray(Charsets.UTF_8)
        val jsonExists = jsonFile.exists()
        val textExists = textFile.exists()
        require(jsonExists == textExists) { "PERSISTENCE_PARTIAL_OUTPUT" }
        if (jsonExists) {
            require(jsonFile.readBytes().contentEquals(json)) { "PERSISTENCE_CONFLICT" }
            require(textFile.readBytes().contentEquals(text)) { "PERSISTENCE_CONFLICT" }
            return
        }
        writeNoOverwrite(jsonFile, json)
        try {
            writeNoOverwrite(textFile, text)
        } catch (failure: Throwable) {
            Files.deleteIfExists(jsonFile.toPath())
            throw failure
        }
    }

    fun sha256(file: File): String {
        FileInputStream(file).use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
            return digest.digest().toHex()
        }
    }

    fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).toHex()

    private fun writeNoOverwrite(file: File, bytes: ByteArray) {
        require(!file.exists()) { "PERSISTENCE_CONFLICT" }
        val parent = requireNotNull(file.parentFile) { "PERSISTENCE_PARENT_REQUIRED" }
        require(parent.exists() || parent.mkdirs()) { "PERSISTENCE_PARENT_REQUIRED" }
        val temporary = Files.createTempFile(parent.toPath(), ".${file.name}.", ".tmp")
        try {
            Files.write(temporary, bytes)
            try {
                Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, file.toPath())
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun summary(report: HimZeroCandidateRecoveryReviewCorpusReportV1): String = buildString {
        appendLine("contract=${report.contractId}")
        appendLine("causeAnalysisLogicalDigest=${report.inputBinding.causeAnalysisLogicalDigest}")
        appendLine("recoveryReviewImplementationHead=${report.inputBinding.recoveryReviewImplementationHead}")
        appendLine("selectedUniqueRecords=${report.counters.selectedUniqueRecords}")
        appendLine("auditLinkedCanonicalTargets=${report.counters.auditLinkedCanonicalTargets}")
        report.priorityCounters.forEach {
            appendLine("priority.${it.priorityClass.name}.groups=${it.groups}")
            appendLine("priority.${it.priorityClass.name}.referenceMemberships=${it.referenceMemberships}")
            appendLine("priority.${it.priorityClass.name}.findingOccurrences=${it.findingOccurrences}")
        }
        report.sourceBreakdown.forEach { appendLine("source.${it.source.name}=${it.entries}") }
        report.recordKindBreakdown.forEach { appendLine("recordKind.${it.recordKind.name}=${it.entries}") }
        report.selectionReasonBreakdown.forEach { appendLine("reason.${it.reason.name}=${it.entries}") }
        appendLine("logicalDigest=${report.logicalDigest}")
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
