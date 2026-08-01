package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.analysis

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ResultingNutritionMacronutrientWarningAnalysisReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        analysis:
        ResultingNutritionMacronutrientWarningAnalysis,
        outputFile: File
    ): File {

        outputFile.parentFile?.let { parent ->
            require(
                parent.mkdirs() ||
                        parent.isDirectory
            ) {
                "Could not create macronutrient warning analysis " +
                        "report directory: " +
                        parent.absolutePath
            }
        }

        val report =
            ResultingNutritionMacronutrientWarningAnalysisReport(
                version =
                    ResultingNutritionMacronutrientWarningAnalysisReport
                        .CURRENT_VERSION,
                inputFile =
                    analysis.inputFile.path,
                inputFileSizeBytes =
                    analysis.inputFileSizeBytes,
                entryCount =
                    analysis.entryCount,
                warningEntryCount =
                    analysis.warningEntryCount,
                warningRate =
                    if (analysis.entryCount == 0L) {
                        0.0
                    } else {
                        analysis.warningEntryCount.toDouble() /
                                analysis.entryCount.toDouble()
                    },
                fiberDoubleCountingCandidateCount =
                    analysis.fiberDoubleCountingCandidateCount,
                coreMacronutrientExcessCount =
                    analysis.coreMacronutrientExcessCount,
                countsByClassification =
                    analysis.countsByClassification
                        .mapKeys { entry ->
                            entry.key.name
                        }
                        .toSortedMap(),
                countsByExcessBucket =
                    analysis.countsByExcessBucket
                        .mapKeys { entry ->
                            entry.key.name
                        }
                        .toSortedMap(),
                maximumMacronutrientSum =
                    analysis.maximumMacronutrientSum,
                maximumExcessGrams =
                    analysis.maximumExcessGrams,
                thresholds =
                    ResultingNutritionMacronutrientWarningAnalysisThresholds(),
                examples =
                    analysis.examples,
                durationMillis =
                    analysis.durationMillis
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

        runCatching {
            Files.move(
                temporaryFile.toPath(),
                outputFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }
            .getOrElse {
                Files.move(
                    temporaryFile.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }

        return outputFile
    }
}

data class ResultingNutritionMacronutrientWarningAnalysisReport(
    val version: Int,
    val inputFile: String,
    val inputFileSizeBytes: Long,
    val entryCount: Long,
    val warningEntryCount: Long,
    val warningRate: Double,
    val fiberDoubleCountingCandidateCount: Long,
    val coreMacronutrientExcessCount: Long,
    val countsByClassification: Map<String, Long>,
    val countsByExcessBucket: Map<String, Long>,
    val maximumMacronutrientSum: Double,
    val maximumExcessGrams: Double,
    val thresholds:
    ResultingNutritionMacronutrientWarningAnalysisThresholds,
    val examples:
    List<ResultingNutritionMacronutrientWarningExample>,
    val durationMillis: Long
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

data class ResultingNutritionMacronutrientWarningAnalysisThresholds(
    val maximumMacronutrientSumGrams: Double =
        105.0,
    val minimumMacronutrientsForSumCheck: Int =
        3
)