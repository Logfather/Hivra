package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ResultingNutritionConflictEvaluationReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        evaluation: ResultingNutritionConflictEvaluation,
        outputFile: File
    ): File {

        outputFile.parentFile?.let { parent ->
            require(
                parent.mkdirs() ||
                        parent.isDirectory
            ) {
                "Could not create Nutrition conflict evaluation " +
                        "report directory: ${parent.absolutePath}"
            }
        }

        val report =
            ResultingNutritionConflictEvaluationReport(
                version =
                    ResultingNutritionConflictEvaluationReport
                        .CURRENT_VERSION,
                aggregateFile =
                    evaluation.aggregateFile.path,
                runtimeFile =
                    evaluation.runtimeFile.path,
                conflictEntryCount =
                    evaluation.conflictEntryCount,
                evaluatedConflictEntryCount =
                    evaluation.evaluatedConflictEntryCount,
                countsByClassification =
                    evaluation.countsByClassification
                        .mapKeys { entry ->
                            entry.key.name
                        }
                        .toSortedMap(),
                percentagesByClassification =
                    evaluation.countsByClassification
                        .mapKeys { entry ->
                            entry.key.name
                        }
                        .mapValues { (_, count) ->
                            percentage(
                                count =
                                    count,
                                total =
                                    evaluation.evaluatedConflictEntryCount
                            )
                        }
                        .toSortedMap(),
                countsBySeverity =
                    evaluation.countsBySeverity
                        .mapKeys { entry ->
                            entry.key.name
                        }
                        .toSortedMap(),
                countsByMatchType =
                    evaluation.countsByMatchType
                        .mapKeys { entry ->
                            entry.key.name
                        }
                        .toSortedMap(),
                countsByConflictingNutrientCount =
                    evaluation.countsByConflictingNutrientCount,
                classificationsByNutrient =
                    evaluation.classificationsByNutrient
                        .mapKeys { entry ->
                            entry.key.name
                        }
                        .mapValues { (_, classifications) ->
                            classifications
                                .mapKeys { entry ->
                                    entry.key.name
                                }
                                .toSortedMap()
                        }
                        .toSortedMap(),
                uniformScaleMismatchCount =
                    evaluation.uniformScaleMismatchCount,
                energyUnitConversionMismatchCount =
                    evaluation.energyUnitConversionMismatchCount,
                singleNutrientConflictCount =
                    evaluation.singleNutrientConflictCount,
                multiNutrientProfileConflictCount =
                    evaluation.multiNutrientProfileConflictCount,
                examples =
                    evaluation.examples,
                omittedExampleCount =
                    evaluation.omittedExampleCount,
                thresholds =
                    ResultingNutritionConflictEvaluationThresholds(),
                durationMillis =
                    evaluation.durationMillis
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

    private fun percentage(
        count: Long,
        total: Long
    ): Double =
        if (total == 0L) {
            0.0
        } else {
            count.toDouble() /
                    total.toDouble() *
                    100.0
        }
}

data class ResultingNutritionConflictEvaluationReport(
    val version: Int,
    val aggregateFile: String,
    val runtimeFile: String,
    val conflictEntryCount: Long,
    val evaluatedConflictEntryCount: Long,
    val countsByClassification: Map<String, Long>,
    val percentagesByClassification: Map<String, Double>,
    val countsBySeverity: Map<String, Long>,
    val countsByMatchType: Map<String, Long>,
    val countsByConflictingNutrientCount: Map<Int, Long>,
    val classificationsByNutrient:
    Map<String, Map<String, Long>>,
    val uniformScaleMismatchCount: Long,
    val energyUnitConversionMismatchCount: Long,
    val singleNutrientConflictCount: Long,
    val multiNutrientProfileConflictCount: Long,
    val examples:
    List<ResultingNutritionConflictEvaluationExample>,
    val omittedExampleCount: Long,
    val thresholds:
    ResultingNutritionConflictEvaluationThresholds,
    val durationMillis: Long
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

data class ResultingNutritionConflictEvaluationThresholds(
    val minimumScaleNutrientCount: Int =
        3,
    val minimumNearIdentityScale: Double =
        0.8,
    val maximumNearIdentityScale: Double =
        1.25,
    val maximumScaleFactorRelativeDeviation: Double =
        0.25,
    val kilojoulePerKilocalorie: Double =
        4.184,
    val energyUnitRelativeTolerance: Double =
        0.10,
    val lowMaximumToleranceMultiple: Double =
        2.0,
    val mediumMaximumToleranceMultiple: Double =
        10.0,
    val highMaximumToleranceMultiple: Double =
        50.0
)