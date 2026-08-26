package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Deterministic UTF-8/LF persistence for the hermetic zero-candidate report. */
object HimZeroCandidateCauseAnalysisPersistenceV1 {
    private val gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()

    fun writeReport(
        jsonFile: File,
        textFile: File,
        report: HimZeroCandidateCauseAnalysisReportV1,
    ) {
        report.validate()
        val json = gson.toJson(report) + "\n"
        val text = summary(report)
        writeNoOverwrite(jsonFile, json, "ANALYSIS_JSON_ALREADY_EXISTS")
        writeNoOverwrite(textFile, text, "ANALYSIS_TEXT_ALREADY_EXISTS")
    }

    fun readReport(file: File): HimZeroCandidateCauseAnalysisReportV1 =
        gson.fromJson(file.readText(Charsets.UTF_8), HimZeroCandidateCauseAnalysisReportV1::class.java).also {
            it.validate()
        }

    fun logicalDigest(report: HimZeroCandidateCauseAnalysisReportV1): String =
        sha256(gson.toJson(report.copy(logicalDigest = "")))

    fun bindingDigest(binding: HimZeroCandidateCauseAnalysisInputBindingV1): String =
        sha256(gson.toJson(binding.copy(bindingDigest = "")))

    fun sha256(value: String): String = sha256(value.toByteArray(Charsets.UTF_8))

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

    private fun writeNoOverwrite(file: File, content: String, conflict: String) {
        if (file.exists()) {
            require(file.readText(Charsets.UTF_8) == content) { conflict }
            return
        }
        val temporary = file.parentFile?.let { parent ->
            parent.mkdirs()
            Files.createTempFile(parent.toPath(), file.name, ".tmp").toFile()
        } ?: error("PARENT_DIRECTORY_REQUIRED")
        try {
            temporary.writeText(content, Charsets.UTF_8)
            file.parentFile?.mkdirs()
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } finally {
            temporary.delete()
        }
    }

    private fun summary(report: HimZeroCandidateCauseAnalysisReportV1): String = buildString {
        appendLine("contract=${report.contractId}")
        appendLine("diagnosticMissionLogicalDigest=${report.inputBinding.diagnosticMissionLogicalDigest}")
        appendLine("diagnosticAggregateLogicalDigest=${report.inputBinding.diagnosticAggregateLogicalDigest}")
        appendLine("analysisImplementationHead=${report.inputBinding.analysisImplementationHead}")
        appendLine("records=${report.counters.records}")
        appendLine("zeroCandidateRecords=${report.counters.zeroCandidateRecords}")
        appendLine("uniqueEvidenceReferences=${report.counters.uniqueEvidenceReferences}")
        appendLine("canonicalTargets=${report.counters.canonicalTargets}")
        report.bucketCounters.forEach { appendLine("bucket.${it.bucket.name}=${it.count}") }
        report.sourceBucketBreakdown.forEach {
            appendLine("sourceBucket.${it.source.name}.${it.bucket.name}=${it.count}")
        }
        report.flagCounters.forEach { appendLine("flag.${it.flag.name}=${it.count}") }
        appendLine("recurringPrimaryValueGroups=${report.recurringPrimaryValueGroups.size}")
        appendLine("recurringTokenGroups=${report.recurringTokenGroups.size}")
        appendLine("logicalDigest=${report.logicalDigest}")
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
