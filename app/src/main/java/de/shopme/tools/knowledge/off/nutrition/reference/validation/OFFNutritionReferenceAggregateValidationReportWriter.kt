package de.shopme.tools.knowledge.off.nutrition.reference.validation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class OFFNutritionReferenceAggregateValidationReportWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
) {

    fun write(
        result: OFFNutritionReferenceAggregateValidationResult,
        outputFile: File
    ): File {

        outputFile.parentFile?.mkdirs()

        require(
            outputFile.parentFile?.isDirectory == true
        ) {
            "Could not create validation report directory: " +
                    outputFile.parentFile?.absolutePath
        }

        val issueCountsByType =
            result.entries
                .asSequence()
                .flatMap { entry ->
                    entry.issues.asSequence()
                }
                .groupingBy { issue ->
                    issue.type
                }
                .eachCount()
                .toSortedMap(
                    compareBy { type ->
                        type.name
                    }
                )

        val warningEntries =
            result.entries
                .filter { entry ->
                    entry.status ==
                            OFFNutritionReferenceAggregateValidationStatus.WARNING
                }

        val rejectedEntries =
            result.entries
                .filter { entry ->
                    entry.status ==
                            OFFNutritionReferenceAggregateValidationStatus.REJECTED
                }

        val report =
            OFFNutritionReferenceAggregateValidationReport(
                version =
                    REPORT_VERSION,
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
                validatedAggregateCount =
                    result.validatedAggregates.size,
                totalWarningCount =
                    result.totalWarningCount,
                totalErrorCount =
                    result.totalErrorCount,
                issueCountsByType =
                    issueCountsByType,
                warningEntries =
                    warningEntries,
                rejectedEntries =
                    rejectedEntries,
                entries =
                    result.entries
            )

        val temporaryFile =
            File(
                outputFile.parentFile,
                "${outputFile.name}.tmp"
            )

        temporaryFile.writeText(
            gson.toJson(report) + "\n"
        )

        if (outputFile.exists()) {
            require(outputFile.delete()) {
                "Could not replace validation report: " +
                        outputFile.absolutePath
            }
        }

        require(temporaryFile.renameTo(outputFile)) {
            "Could not move validation report into place: " +
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

data class OFFNutritionReferenceAggregateValidationReport(
    val version: Int,
    val thresholds:
    OFFNutritionReferenceAggregateValidationThresholds,
    val inputAggregateCount: Int,
    val acceptedAggregateCount: Int,
    val warningAggregateCount: Int,
    val rejectedAggregateCount: Int,
    val validatedAggregateCount: Int,
    val totalWarningCount: Int,
    val totalErrorCount: Int,
    val issueCountsByType:
    Map<OFFNutritionReferenceAggregateIssueType, Int>,
    val warningEntries:
    List<OFFNutritionReferenceAggregateValidationEntry>,
    val rejectedEntries:
    List<OFFNutritionReferenceAggregateValidationEntry>,
    val entries:
    List<OFFNutritionReferenceAggregateValidationEntry>
)

data class OFFNutritionReferenceAggregateValidationThresholds(
    val maximumEnergyKcalPer100g: Double,
    val maximumNutrientGramsPer100g: Double,
    val minimumProfilesForCoverageCheck: Int,
    val minimumCoreNutrientObservationCoverage: Double,
    val minimumObservationsForVariationCheck: Int,
    val maximumRelativeNutrientRange: Double,
    val maximumEnergyRelativeDifference: Double,
    val highProfileCountThreshold: Int
)