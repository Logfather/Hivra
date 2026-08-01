package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ResultingNutritionConflictReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        analysis: ResultingNutritionConflictAnalysis,
        outputFile: File
    ): File {

        outputFile.parentFile?.let { parent ->
            require(
                parent.mkdirs() ||
                        parent.isDirectory
            ) {
                "Could not create Nutrition conflict report directory: " +
                        parent.absolutePath
            }
        }

        val report =
            ResultingNutritionConflictReport(
                version =
                    ResultingNutritionConflictReport.CURRENT_VERSION,
                aggregateFile =
                    analysis.aggregateFile.path,
                runtimeFile =
                    analysis.runtimeFile.path,
                aggregateEntryCount =
                    analysis.aggregateEntryCount,
                runtimeEntryCount =
                    analysis.runtimeEntryCount,
                exactMatchedEntryCount =
                    analysis.exactMatchedEntryCount,
                normalizationEquivalentMatchedEntryCount =
                    analysis
                        .normalizationEquivalentMatchedEntryCount,
                matchedEntryCount =
                    analysis.matchedEntryCount,
                comparableEntryCount =
                    analysis.comparableEntryCount,
                nonComparableEntryCount =
                    analysis.nonComparableEntryCount,
                conflictEntryCount =
                    analysis.conflictEntryCount,
                conflictFreeEntryCount =
                    analysis.conflictFreeEntryCount,
                nutrientComparisonCount =
                    analysis.nutrientComparisonCount,
                nutrientConflictCount =
                    analysis.nutrientConflictCount,
                entryConflictRate =
                    analysis.entryConflictRate,
                entryConflictPercentage =
                    analysis.entryConflictRate * 100.0,
                nutrientConflictRate =
                    analysis.nutrientConflictRate,
                nutrientConflictPercentage =
                    analysis.nutrientConflictRate * 100.0,
                comparisonCountsByNutrient =
                    analysis.comparisonCountsByNutrient
                        .mapKeys { entry ->
                            entry.key.name
                        }
                        .toSortedMap(),
                conflictCountsByNutrient =
                    analysis.conflictCountsByNutrient
                        .mapKeys { entry ->
                            entry.key.name
                        }
                        .toSortedMap(),
                maximumAbsoluteDifferenceByNutrient =
                    analysis.maximumAbsoluteDifferenceByNutrient
                        .mapKeys { entry ->
                            entry.key.name
                        }
                        .toSortedMap(),
                tolerances =
                    ResultingNutritionConflictNutrient
                        .entries
                        .associate { nutrient ->
                            nutrient.name to
                                    nutrient.absoluteTolerance
                        }
                        .toSortedMap(),
                conflictExamples =
                    analysis.conflictExamples,
                omittedConflictExampleCount =
                    analysis.omittedConflictExampleCount,
                durationMillis =
                    analysis.durationMillis
            )

        val temporaryFile =
            outputFile.resolveSibling(
                outputFile.name + ".tmp"
            )

        temporaryFile.writeText(
            gson.toJson(report) + "\n",
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

data class ResultingNutritionConflictReport(
    val version: Int,
    val aggregateFile: String,
    val runtimeFile: String,
    val aggregateEntryCount: Long,
    val runtimeEntryCount: Long,
    val exactMatchedEntryCount: Long,
    val normalizationEquivalentMatchedEntryCount: Long,
    val matchedEntryCount: Long,
    val comparableEntryCount: Long,
    val nonComparableEntryCount: Long,
    val conflictEntryCount: Long,
    val conflictFreeEntryCount: Long,
    val nutrientComparisonCount: Long,
    val nutrientConflictCount: Long,
    val entryConflictRate: Double,
    val entryConflictPercentage: Double,
    val nutrientConflictRate: Double,
    val nutrientConflictPercentage: Double,
    val comparisonCountsByNutrient: Map<String, Long>,
    val conflictCountsByNutrient: Map<String, Long>,
    val maximumAbsoluteDifferenceByNutrient: Map<String, Double>,
    val tolerances: Map<String, Double>,
    val conflictExamples: List<ResultingNutritionConflictExample>,
    val omittedConflictExampleCount: Long,
    val durationMillis: Long
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}