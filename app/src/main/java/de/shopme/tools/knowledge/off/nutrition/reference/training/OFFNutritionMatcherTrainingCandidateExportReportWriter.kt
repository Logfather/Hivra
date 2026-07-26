package de.shopme.tools.knowledge.off.nutrition.reference.training

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.off.nutrition.reference.validation
.OFFNutritionReferenceAggregateValidationStatus
import java.io.File

class OFFNutritionMatcherTrainingCandidateExportReportWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
) {

    fun write(
        result: OFFNutritionMatcherTrainingCandidateExportResult,
        outputFile: File
    ): File {

        outputFile.parentFile?.mkdirs()

        require(outputFile.parentFile?.isDirectory == true) {
            "Could not create matcher candidate report directory: " +
                    outputFile.parentFile?.absolutePath
        }

        val warningIssueCounts =
            result.candidates
                .asSequence()
                .filter { candidate ->
                    candidate.validationStatus ==
                            OFFNutritionReferenceAggregateValidationStatus
                                .WARNING
                }
                .flatMap { candidate ->
                    candidate.validationIssueTypes.asSequence()
                }
                .groupingBy { issueType ->
                    issueType
                }
                .eachCount()
                .toSortedMap()

        val profileCountDistribution =
            sortedMapOf(
                "1" to
                        result.candidates.count { candidate ->
                            candidate.profileCount == 1
                        },
                "2-4" to
                        result.candidates.count { candidate ->
                            candidate.profileCount in 2..4
                        },
                "5-9" to
                        result.candidates.count { candidate ->
                            candidate.profileCount in 5..9
                        },
                "10-49" to
                        result.candidates.count { candidate ->
                            candidate.profileCount in 10..49
                        },
                "50+" to
                        result.candidates.count { candidate ->
                            candidate.profileCount >= 50
                        }
            )

        val report =
            OFFNutritionMatcherTrainingCandidateExportReport(
                version =
                    REPORT_VERSION,
                datasetType =
                    DATASET_TYPE,
                serverArtifact =
                    OFFNutritionMatcherTrainingCandidate.SERVER_ARTIFACT,
                inputAggregateCount =
                    result.inputAggregateCount,
                exportedCandidateCount =
                    result.exportedCandidateCount,
                acceptedCandidateCount =
                    result.acceptedCandidateCount,
                warningCandidateCount =
                    result.warningCandidateCount,
                uniqueRetrievalAliasCount =
                    result.uniqueRetrievalAliasCount,
                averageRetrievalAliasCount =
                    average(
                        values =
                            result.candidates.map { candidate ->
                                candidate.retrievalAliases.size
                            }
                    ),
                averageProfileCount =
                    average(
                        values =
                            result.candidates.map { candidate ->
                                candidate.profileCount
                            }
                    ),
                maximumProfileCount =
                    result.candidates
                        .maxOfOrNull { candidate ->
                            candidate.profileCount
                        }
                        ?: 0,
                profileCountDistribution =
                    profileCountDistribution,
                warningIssueCounts =
                    warningIssueCounts
            )

        val temporaryFile =
            File(
                outputFile.parentFile,
                "${outputFile.name}.tmp"
            )

        temporaryFile.writeText(
            gson.toJson(report) +
                    System.lineSeparator()
        )

        if (outputFile.exists()) {
            require(outputFile.delete()) {
                "Could not replace matcher candidate export report: " +
                        outputFile.absolutePath
            }
        }

        require(temporaryFile.renameTo(outputFile)) {
            "Could not move matcher candidate report into place: " +
                    outputFile.absolutePath
        }

        return outputFile
    }

    private fun average(
        values: List<Int>
    ): Double {

        if (values.isEmpty()) {
            return 0.0
        }

        return values.sum().toDouble() /
                values.size.toDouble()
    }

    private companion object {

        const val REPORT_VERSION =
            1

        const val DATASET_TYPE =
            "OFF_NUTRITION_MATCHER_TRAINING_CANDIDATES"
    }
}

data class OFFNutritionMatcherTrainingCandidateExportReport(
    val version: Int,
    val datasetType: String,
    val serverArtifact: String,
    val inputAggregateCount: Int,
    val exportedCandidateCount: Int,
    val acceptedCandidateCount: Int,
    val warningCandidateCount: Int,
    val uniqueRetrievalAliasCount: Int,
    val averageRetrievalAliasCount: Double,
    val averageProfileCount: Double,
    val maximumProfileCount: Int,
    val profileCountDistribution: Map<String, Int>,
    val warningIssueCounts: Map<String, Int>
)