package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis

import com.google.gson.GsonBuilder
import java.io.File

class OFFNutritionReferenceCandidateGapAnalysisReportWriter {

    fun write(
        report: OFFNutritionReferenceCandidateGapAnalysisReport,
        outputFile: File
    ): OFFNutritionReferenceCandidateGapAnalysisWriteResult {

        val canonicalOutputFile =
            outputFile.canonicalFile

        canonicalOutputFile.parentFile?.mkdirs()

        require(
            canonicalOutputFile.parentFile?.isDirectory == true
        ) {
            "Output directory could not be created: " +
                    canonicalOutputFile.parentFile?.absolutePath
        }

        val json =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
                .toJson(report)
                .trimEnd() +
                    System.lineSeparator()

        canonicalOutputFile.writeText(json)

        require(canonicalOutputFile.isFile) {
            "Analysis report was not written: " +
                    canonicalOutputFile.absolutePath
        }

        require(canonicalOutputFile.length() > 0L) {
            "Analysis report is empty: " +
                    canonicalOutputFile.absolutePath
        }

        return OFFNutritionReferenceCandidateGapAnalysisWriteResult(
            findingCount =
                report.analyzedFindingCount,
            outputFile =
                canonicalOutputFile,
            fileSizeBytes =
                canonicalOutputFile.length()
        )
    }
}