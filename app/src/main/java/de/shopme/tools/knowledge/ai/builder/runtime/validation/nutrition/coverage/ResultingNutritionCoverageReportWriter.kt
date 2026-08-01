package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ResultingNutritionCoverageReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        analysis: ResultingNutritionCoverageAnalysis,
        outputFile: File
    ): File {

        outputFile.parentFile?.let { parent ->
            require(
                parent.mkdirs() ||
                        parent.isDirectory
            ) {
                "Could not create Nutrition coverage report " +
                        "directory: ${parent.absolutePath}"
            }
        }

        val report =
            ResultingNutritionCoverageReport(
                version =
                    ResultingNutritionCoverageReport
                        .CURRENT_VERSION,
                aggregateFile =
                    analysis.aggregateFile.path,
                aggregateFileSizeBytes =
                    analysis.aggregateFileSizeBytes,
                runtimeFile =
                    analysis.runtimeFile.path,
                runtimeFileSizeBytes =
                    analysis.runtimeFileSizeBytes,
                aggregateEntryCount =
                    analysis.aggregateEntryCount,
                runtimeEntryCount =
                    analysis.runtimeEntryCount,
                coveredAggregateEntryCount =
                    analysis.coveredAggregateEntryCount,
                missingRuntimeEntryCount =
                    analysis.missingRuntimeEntryCount,
                additionalRuntimeEntryCount =
                    analysis.additionalRuntimeEntryCount,
                aggregateCoverageRate =
                    analysis.aggregateCoverageRate,
                aggregateCoveragePercentage =
                    analysis.aggregateCoverageRate *
                            100.0,
                complete =
                    analysis.isComplete,
                missingRuntimeCanonicalIds =
                    analysis.missingRuntimeCanonicalIds,
                additionalRuntimeCanonicalIds =
                    analysis.additionalRuntimeCanonicalIds,
                omittedMissingRuntimeCanonicalIdCount =
                    analysis
                        .omittedMissingRuntimeCanonicalIdCount,
                omittedAdditionalRuntimeCanonicalIdCount =
                    analysis
                        .omittedAdditionalRuntimeCanonicalIdCount,
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

data class ResultingNutritionCoverageReport(
    val version: Int,
    val aggregateFile: String,
    val aggregateFileSizeBytes: Long,
    val runtimeFile: String,
    val runtimeFileSizeBytes: Long,
    val aggregateEntryCount: Long,
    val runtimeEntryCount: Long,
    val coveredAggregateEntryCount: Long,
    val missingRuntimeEntryCount: Long,
    val additionalRuntimeEntryCount: Long,
    val aggregateCoverageRate: Double,
    val aggregateCoveragePercentage: Double,
    val complete: Boolean,
    val missingRuntimeCanonicalIds: List<String>,
    val additionalRuntimeCanonicalIds: List<String>,
    val omittedMissingRuntimeCanonicalIdCount: Long,
    val omittedAdditionalRuntimeCanonicalIdCount: Long,
    val durationMillis: Long
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}