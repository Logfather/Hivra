package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ResultingNutritionKnowledgeValidationReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        result: ResultingNutritionKnowledgeValidationResult,
        outputFile: File
    ): File {

        outputFile.parentFile?.let { parent ->
            require(
                parent.mkdirs() ||
                        parent.isDirectory
            ) {
                "Could not create validation report directory: " +
                        parent.absolutePath
            }
        }

        val report =
            ResultingNutritionKnowledgeValidationReport(
                version =
                    ResultingNutritionKnowledgeValidationReport
                        .CURRENT_VERSION,
                inputFile =
                    result.inputFile.path,
                inputFileSizeBytes =
                    result.inputFileSizeBytes,
                entryCount =
                    result.entryCount,
                validEntryCount =
                    result.validEntryCount,
                warningEntryCount =
                    result.warningEntryCount,
                rejectedEntryCount =
                    result.rejectedEntryCount,
                warningCount =
                    result.warningCount,
                errorCount =
                    result.errorCount,
                issueCount =
                    result.issueCount,
                valid =
                    result.isValid,
                thresholds =
                    ResultingNutritionKnowledgeValidationThresholds(),
                countsByReason =
                    result.countsByReason
                        .mapKeys {
                            it.key.name
                        }
                        .toSortedMap(),
                warningIssues =
                    result.warningIssues,
                errorIssues =
                    result.errorIssues,
                durationMillis =
                    result.durationMillis,
                reportedWarningCount =
                    result.reportedWarningCount,
                reportedErrorCount =
                    result.reportedErrorCount,
                omittedWarningCount =
                    result.omittedWarningCount,
                omittedErrorCount =
                    result.omittedErrorCount,
            )

        val temporaryFile =
            outputFile.resolveSibling(
                outputFile.name +
                        ".tmp"
            )

        temporaryFile.writeText(
            gson.toJson(
                report
            ) + "\n",
            StandardCharsets.UTF_8
        )

        Files.move(
            temporaryFile.toPath(),
            outputFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )

        return outputFile
    }
}

data class ResultingNutritionKnowledgeValidationReport(
    val version: Int,
    val inputFile: String,
    val inputFileSizeBytes: Long,
    val entryCount: Long,
    val validEntryCount: Long,
    val warningEntryCount: Long,
    val rejectedEntryCount: Long,
    val warningCount: Long,
    val errorCount: Long,
    val issueCount: Long,
    val valid: Boolean,
    val thresholds:
    ResultingNutritionKnowledgeValidationThresholds,
    val countsByReason: Map<String, Long>,
    val warningIssues:
    List<ResultingNutritionKnowledgeValidationIssue>,
    val errorIssues:
    List<ResultingNutritionKnowledgeValidationIssue>,
    val durationMillis: Long,
    val reportedWarningCount: Int,
    val reportedErrorCount: Int,
    val omittedWarningCount: Long,
    val omittedErrorCount: Long,
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

data class ResultingNutritionKnowledgeValidationThresholds(
    val maximumEnergyKcalPer100g: Double =
        950.0,
    val maximumComponentGramsPer100g: Double =
        100.0,
    val relationshipToleranceGrams: Double =
        0.5,
    val maximumMacronutrientSumGrams: Double =
        105.0,
    val minimumMacronutrientsForSumCheck: Int =
        3
)