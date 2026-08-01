package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ResultingNutritionCoverageGapClassificationReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        classification:
        ResultingNutritionCoverageGapClassification,
        outputFile: File
    ): File {

        outputFile.parentFile?.let { parent ->
            require(
                parent.mkdirs() ||
                        parent.isDirectory
            ) {
                "Could not create Nutrition coverage gap report " +
                        "directory: ${parent.absolutePath}"
            }
        }

        val report =
            ResultingNutritionCoverageGapClassificationReport(
                version =
                    ResultingNutritionCoverageGapClassificationReport
                        .CURRENT_VERSION,
                aggregateFile =
                    classification.aggregateFile.path,
                runtimeFile =
                    classification.runtimeFile.path,
                aggregateEntryCount =
                    classification.aggregateEntryCount,
                runtimeEntryCount =
                    classification.runtimeEntryCount,
                exactMatchCount =
                    classification.exactMatchCount,
                normalizationEquivalentMatchCount =
                    classification
                        .normalizationEquivalentMatchCount,
                effectiveCoveredAggregateEntryCount =
                    classification
                        .effectiveCoveredAggregateEntryCount,
                trueMissingRuntimeEntryCount =
                    classification.trueMissingRuntimeEntryCount,
                trueAdditionalRuntimeEntryCount =
                    classification.trueAdditionalRuntimeEntryCount,
                exactCoverageRate =
                    classification.exactCoverageRate,
                exactCoveragePercentage =
                    classification.exactCoverageRate *
                            100.0,
                effectiveCoverageRate =
                    classification.effectiveCoverageRate,
                effectiveCoveragePercentage =
                    classification.effectiveCoverageRate *
                            100.0,
                complete =
                    classification.complete,
                normalizationCollisionGroupCount =
                    classification.normalizationCollisionGroupCount,
                normalizationEquivalentExamples =
                    classification.normalizationEquivalentExamples,
                trueMissingRuntimeCanonicalIds =
                    classification.trueMissingRuntimeCanonicalIds,
                trueAdditionalRuntimeCanonicalIds =
                    classification.trueAdditionalRuntimeCanonicalIds,
                omittedNormalizationEquivalentExampleCount =
                    classification
                        .omittedNormalizationEquivalentExampleCount,
                omittedTrueMissingRuntimeCanonicalIdCount =
                    classification
                        .omittedTrueMissingRuntimeCanonicalIdCount,
                omittedTrueAdditionalRuntimeCanonicalIdCount =
                    classification
                        .omittedTrueAdditionalRuntimeCanonicalIdCount,
                durationMillis =
                    classification.durationMillis
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

data class ResultingNutritionCoverageGapClassificationReport(
    val version: Int,
    val aggregateFile: String,
    val runtimeFile: String,
    val aggregateEntryCount: Long,
    val runtimeEntryCount: Long,
    val exactMatchCount: Long,
    val normalizationEquivalentMatchCount: Long,
    val effectiveCoveredAggregateEntryCount: Long,
    val trueMissingRuntimeEntryCount: Long,
    val trueAdditionalRuntimeEntryCount: Long,
    val exactCoverageRate: Double,
    val exactCoveragePercentage: Double,
    val effectiveCoverageRate: Double,
    val effectiveCoveragePercentage: Double,
    val complete: Boolean,
    val normalizationCollisionGroupCount: Long,
    val normalizationEquivalentExamples:
    List<ResultingNutritionCoverageNormalizationEquivalentPair>,
    val trueMissingRuntimeCanonicalIds: List<String>,
    val trueAdditionalRuntimeCanonicalIds: List<String>,
    val omittedNormalizationEquivalentExampleCount: Long,
    val omittedTrueMissingRuntimeCanonicalIdCount: Long,
    val omittedTrueAdditionalRuntimeCanonicalIdCount: Long,
    val durationMillis: Long
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}