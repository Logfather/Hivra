package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale

object HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1 {
    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .create()

    fun writeReport(jsonFile: File, textFile: File, report: HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1) {
        report.validate()
        writeNoOverwrite(jsonFile, (gson.toJson(report) + "\n").toByteArray(StandardCharsets.UTF_8))
        writeNoOverwrite(textFile, text(report).toByteArray(StandardCharsets.UTF_8))
    }

    fun readReport(file: File): HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1 =
        gson.fromJson(file.readText(StandardCharsets.UTF_8), HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1::class.java)
            .also { it.validate() }

    fun logicalDigest(report: HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1): String =
        sha256(gson.toJson(report.copy(logicalDigest = "")))

    fun bindingDigest(binding: HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1): String =
        sha256(gson.toJson(binding.copy(bindingDigest = "")))

    fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it) }
    }

    private fun sha256(value: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(value)
        .joinToString("") { "%02x".format(Locale.ROOT, it) }

    private fun text(report: HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1): String = buildString {
        appendLine("HIM Evidence-Alignment Catalog Audit Findings Analysis V1")
        appendLine("contractId=${report.contractId}")
        appendLine("auditHead=${report.inputBinding.auditHead}")
        appendLine("enrichmentImplementationHead=${report.inputBinding.enrichmentImplementationHead}")
        appendLine("analysisImplementationHead=${report.inputBinding.analysisImplementationHead}")
        appendLine("enrichmentMission=${report.inputBinding.enrichmentMission}")
        appendLine("enrichmentAggregate=${report.inputBinding.enrichmentAggregate}")
        appendLine("enrichmentMissionLogicalDigest=${report.inputBinding.enrichmentMissionLogicalDigest}")
        appendLine("enrichmentAggregateLogicalDigest=${report.inputBinding.enrichmentAggregateLogicalDigest}")
        appendLine("canonicalOrderDigest=${report.inputBinding.canonicalOrderDigest}")
        appendLine("bindingDigest=${report.inputBinding.bindingDigest}")
        appendLine("logicalDigest=${report.logicalDigest}")
        appendLine("expectedCanonicalCount=${report.inputBinding.expectedCanonicalCount}")
        appendLine("totalFindingOccurrences=${report.totalFindingOccurrences}")
        appendLine("counters=${report.counters}")
        appendLine("primaryOutcomeBuckets")
        report.primaryOutcomeBuckets.forEach { appendLine("${it.outcome.name}=${it.count}") }
        appendLine("diagnosticFlags")
        report.diagnosticFlags.forEach { appendLine("${it.flag.name}=${it.count}") }
        appendLine("breakdowns")
        report.breakdowns.forEach { appendLine("${it.axis.name}|${it.key}|${it.count}") }
        appendLine("findingIndexSample")
        report.findingIndex.take(50).forEach { finding ->
            appendLine(listOf(
                finding.findingOccurrenceId,
                finding.auditShardId,
                finding.entityId,
                finding.source.name,
                finding.originalAuditClassification.name,
                finding.reconstructionStatus.name,
                finding.classificationComparison.name,
                finding.originalEvidenceReference ?: HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.NOT_AVAILABLE,
            ).joinToString("|"))
        }
    }

    private fun writeNoOverwrite(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        if (file.exists()) {
            require(file.readBytes().contentEquals(bytes)) { "RESULT_ALREADY_EXISTS" }
            return
        }
        val parent = requireNotNull(file.parentFile)
        val temporary = Files.createTempFile(parent.toPath(), file.name, ".pending")
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
}
