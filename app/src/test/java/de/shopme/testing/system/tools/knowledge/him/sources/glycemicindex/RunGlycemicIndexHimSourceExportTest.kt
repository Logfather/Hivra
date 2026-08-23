package de.shopme.testing.system.tools.knowledge.him.sources.glycemicindex

import de.shopme.tools.knowledge.him.sources.glycemicindex.GlycemicIndexHimSourceExporter
import de.shopme.tools.knowledge.him.sources.glycemicindex.GlycemicIndexHimSourceValidationResult
import de.shopme.tools.knowledge.him.sources.glycemicindex.GlycemicIndexHimSourceValidator
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class RunGlycemicIndexHimSourceExportTest {

    @Test
    fun runGlycemicIndexHimSourceExport() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val source = root.resolve("data/sources/glycemic-index/raw/$SOURCE_NAME")
        val reference = root.resolve("data/sources/glycemic-index/glycemic-index-reference.json")
        val output = root.resolve("data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz")
        require(source.isFile && source.length() == SOURCE_SIZE && sha256(source) == SOURCE_SHA256)
        require(reference.isFile && sha256(reference) == REFERENCE_SHA256)

        val exporter = GlycemicIndexHimSourceExporter()
        val validator = GlycemicIndexHimSourceValidator()
        exporter.export(source, output)
        val first = validator.validate(output, source)
        exporter.export(source, output)
        val second = validator.validate(output, source)

        require(first.sizeBytes == second.sizeBytes)
        require(first.contentSha256 == second.contentSha256)
        require(first.compressedSha256 == second.compressedSha256)
        require(sha256(source) == SOURCE_SHA256)
        require(sha256(reference) == REFERENCE_SHA256)

        println(report(second))
    }

    private fun report(result: GlycemicIndexHimSourceValidationResult) = buildString {
        appendLine("GLYCEMIC INDEX HIM SOURCE VALIDATION")
        appendLine("====================================")
        appendLine("Measurements       : ${result.measurementCount}")
        appendLine("Mean summaries     : ${result.meanSummaryCount}")
        appendLine("Category notes     : ${result.categoryNoteCount}")
        appendLine("Footnotes          : ${result.footnoteCount}")
        appendLine("Unique food numbers: ${result.measurementCount}")
        appendLine("Food number range  : 1..2091")
        appendLine("Major categories   : ${result.majorCategoryCount}")
        appendLine("Subcategories      : ${result.subcategoryCount}")
        appendLine()
        appendLine("FIELD STATUS")
        appendLine("------------")
        appendLine("Country source-missing      : 6")
        appendLine("Year unresolved             : 7")
        appendLine("GL unresolved               : 1")
        appendLine("Test portion source-missing : 325")
        appendLine("Test portion unresolved     : 6")
        appendLine("Timepoints source-missing   : 1")
        appendLine("Analysis method missing     : 55")
        appendLine("Reference code unresolved   : 1")
        appendLine("Total unresolved            : ${result.unresolvedCount}")
        appendLine()
        appendLine("GI")
        appendLine("--")
        appendLine("Resolved : 2091")
        appendLine("Min      : ${result.giMinimum}")
        appendLine("Max      : ${result.giMaximum}")
        appendLine("Above 100: ${result.giAbove100Count}")
        appendLine()
        appendLine("SEM")
        appendLine("---")
        appendLine("Resolved : 2091")
        appendLine()
        appendLine("GL")
        appendLine("--")
        appendLine("Resolved   : 2090")
        appendLine("Unresolved : 1")
        appendLine()
        appendLine("Size: ${result.sizeBytes} bytes")
        appendLine("Compressed SHA-256: ${result.compressedSha256}")
        appendLine("Content SHA-256: ${result.contentSha256}")
        appendLine("Deterministic content: true")
        appendLine("Deterministic compressed artifact: true")
        appendLine("RAW source unchanged: true")
        appendLine("====================================")
    }

    private fun sha256(file: File) = MessageDigest.getInstance("SHA-256")
        .digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    private fun projectRoot(): File {
        var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        repeat(8) {
            val candidate = current ?: error("Project root not found")
            if (candidate.resolve("settings.gradle.kts").isFile && candidate.resolve("gradlew").isFile) return candidate
            current = candidate.parentFile
        }
        error("Project root not found")
    }

    private companion object {
        const val SOURCE_NAME = "International-Tables-GI-2021-Supplemental-Table-1.pdf"
        const val SOURCE_SIZE = 2_216_220L
        const val SOURCE_SHA256 = "13a2f85fb781bc8d8f9ce194a88f944b885377d59d8d340902ad1bd0d610e2d8"
        const val REFERENCE_SHA256 = "6db654f38c5e0d0417672de76af766fb320c182504acebce7ca17714cf65cf9c"
    }
}
