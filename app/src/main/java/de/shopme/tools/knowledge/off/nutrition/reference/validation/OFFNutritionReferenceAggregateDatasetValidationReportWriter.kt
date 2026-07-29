package de.shopme.tools.knowledge.off.nutrition.reference.validation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class OFFNutritionReferenceAggregateDatasetValidationReportWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
) {

    fun write(
        result:
        OFFNutritionReferenceAggregateDatasetValidationResult,
        outputFile: File
    ): File {

        val parentDirectory =
            requireNotNull(
                outputFile.parentFile
            ) {
                "Validation report must have a parent directory."
            }

        require(
            parentDirectory.isDirectory ||
                    parentDirectory.mkdirs()
        ) {
            "Could not create validation report directory: " +
                    parentDirectory.absolutePath
        }

        val report =
            OFFNutritionReferenceAggregateDatasetValidationReport(
                version =
                    REPORT_VERSION,
                valid =
                    result.valid,
                thresholds =
                    createThresholds(),
                inputAggregateCount =
                    result.inputAggregateCount,
                acceptedAggregateCount =
                    result.acceptedAggregateCount,
                warningAggregateCount =
                    result.warningAggregateCount,
                rejectedAggregateCount =
                    result.rejectedAggregateCount,
                totalProfileCount =
                    result.totalProfileCount,
                totalWarningCount =
                    result.totalWarningCount,
                totalErrorCount =
                    result.totalErrorCount,
                issueCountsByType =
                    result.issueCountsByType,
                warningEntries =
                    result.warningEntries,
                rejectedEntries =
                    result.rejectedEntries
            )

        val temporaryFile =
            File(
                parentDirectory,
                "${outputFile.name}.tmp"
            )

        temporaryFile.writeText(
            gson.toJson(report) + "\n",
            StandardCharsets.UTF_8
        )

        try {
            Files.move(
                temporaryFile.toPath(),
                outputFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (
            _: AtomicMoveNotSupportedException
        ) {
            Files.move(
                temporaryFile.toPath(),
                outputFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        require(outputFile.isFile) {
            "Validation report was not written: " +
                    outputFile.absolutePath
        }

        require(outputFile.length() > 0L) {
            "Validation report is empty: " +
                    outputFile.absolutePath
        }

        return outputFile
    }

    private fun createThresholds():
            OFFNutritionReferenceAggregateValidationThresholds =
        OFFNutritionReferenceAggregateValidationThresholds(
            maximumEnergyKcalPer100g =
                OFFNutritionReferenceAggregateValidator
                    .MAXIMUM_ENERGY_KCAL_PER_100G,
            maximumNutrientGramsPer100g =
                OFFNutritionReferenceAggregateValidator
                    .MAXIMUM_NUTRIENT_GRAMS_PER_100G,
            minimumProfilesForCoverageCheck =
                OFFNutritionReferenceAggregateValidator
                    .MINIMUM_PROFILES_FOR_COVERAGE_CHECK,
            minimumCoreNutrientObservationCoverage =
                OFFNutritionReferenceAggregateValidator
                    .MINIMUM_CORE_NUTRIENT_OBSERVATION_COVERAGE,
            minimumObservationsForVariationCheck =
                OFFNutritionReferenceAggregateValidator
                    .MINIMUM_OBSERVATIONS_FOR_VARIATION_CHECK,
            maximumRelativeNutrientRange =
                OFFNutritionReferenceAggregateValidator
                    .MAXIMUM_RELATIVE_NUTRIENT_RANGE,
            maximumEnergyRelativeDifference =
                OFFNutritionReferenceAggregateValidator
                    .MAXIMUM_ENERGY_RELATIVE_DIFFERENCE,
            highProfileCountThreshold =
                OFFNutritionReferenceAggregateValidator
                    .HIGH_PROFILE_COUNT_THRESHOLD
        )

    private companion object {

        const val REPORT_VERSION =
            1
    }
}

data class OFFNutritionReferenceAggregateDatasetValidationReport(
    val version: Int,
    val valid: Boolean,
    val thresholds:
    OFFNutritionReferenceAggregateValidationThresholds,
    val inputAggregateCount: Int,
    val acceptedAggregateCount: Int,
    val warningAggregateCount: Int,
    val rejectedAggregateCount: Int,
    val totalProfileCount: Long,
    val totalWarningCount: Int,
    val totalErrorCount: Int,
    val issueCountsByType:
    Map<OFFNutritionReferenceAggregateIssueType, Int>,
    val warningEntries:
    List<OFFNutritionReferenceAggregateValidationEntry>,
    val rejectedEntries:
    List<OFFNutritionReferenceAggregateValidationEntry>
)