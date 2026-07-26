package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CatalogOFFNutritionRetrievalReportWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
) {

    fun write(
        result: CatalogOFFNutritionRetrievalResult,
        outputFile: File
    ): File {

        outputFile.parentFile?.mkdirs()

        require(outputFile.parentFile?.isDirectory == true) {
            "Could not create retrieval report directory: " +
                    outputFile.parentFile?.absolutePath
        }

        val candidateCountDistribution =
            sortedMapOf(
                "0" to result.requests.count {
                    it.candidates.isEmpty()
                },
                "1" to result.requests.count {
                    it.candidates.size == 1
                },
                "2-4" to result.requests.count {
                    it.candidates.size in 2..4
                },
                "5-9" to result.requests.count {
                    it.candidates.size in 5..9
                },
                "10" to result.requests.count {
                    it.candidates.size == 10
                }
            )

        val topScoreDistribution =
            sortedMapOf(
                "EXACT" to result.requests.count {
                    it.candidates.firstOrNull()?.exactMatch == true
                },
                "HIGH" to result.requests.count {
                    val candidate =
                        it.candidates.firstOrNull()

                    candidate != null &&
                            !candidate.exactMatch &&
                            candidate.score >= 0.80
                },
                "MEDIUM" to result.requests.count {
                    val score =
                        it.candidates.firstOrNull()?.score

                    score != null &&
                            score >= 0.50 &&
                            score < 0.80
                },
                "LOW" to result.requests.count {
                    val score =
                        it.candidates.firstOrNull()?.score

                    score != null &&
                            score < 0.50
                },
                "NONE" to result.requests.count {
                    it.candidates.isEmpty()
                }
            )

        val report =
            CatalogOFFNutritionRetrievalReport(
                version =
                    1,
                catalogItemCount =
                    result.catalogItemCount,
                sourceCandidateCount =
                    result.sourceCandidateCount,
                requestCount =
                    result.requestCount,
                requestWithCandidatesCount =
                    result.requestWithCandidatesCount,
                requestWithoutCandidatesCount =
                    result.requestWithoutCandidatesCount,
                exactTopCandidateCount =
                    result.exactTopCandidateCount,
                totalRetrievedCandidateCount =
                    result.totalRetrievedCandidateCount,
                averageCandidateCount =
                    if (result.requestCount == 0) {
                        0.0
                    } else {
                        result.totalRetrievedCandidateCount.toDouble() /
                                result.requestCount.toDouble()
                    },
                maximumCandidateCount =
                    result.maximumCandidateCount,
                candidateCountDistribution =
                    candidateCountDistribution,
                topScoreDistribution =
                    topScoreDistribution,
                requestsWithoutCandidates =
                    result.requests
                        .filter { request ->
                            request.candidates.isEmpty()
                        }
                        .map { request ->
                            CatalogOFFNutritionRetrievalMissingRequest(
                                catalogIndex =
                                    request.catalogIndex,
                                catalogKey =
                                    request.catalogKey,
                                normalizedEnglish =
                                    request.normalizedEnglish,
                                itemName =
                                    request.itemName,
                                category =
                                    request.category,
                                production =
                                    request.production
                            )
                        }
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
                "Could not replace retrieval report: " +
                        outputFile.absolutePath
            }
        }

        require(temporaryFile.renameTo(outputFile)) {
            "Could not move retrieval report into place: " +
                    outputFile.absolutePath
        }

        return outputFile
    }
}

data class CatalogOFFNutritionRetrievalReport(
    val version: Int,
    val catalogItemCount: Int,
    val sourceCandidateCount: Int,
    val requestCount: Int,
    val requestWithCandidatesCount: Int,
    val requestWithoutCandidatesCount: Int,
    val exactTopCandidateCount: Int,
    val totalRetrievedCandidateCount: Int,
    val averageCandidateCount: Double,
    val maximumCandidateCount: Int,
    val candidateCountDistribution: Map<String, Int>,
    val topScoreDistribution: Map<String, Int>,
    val requestsWithoutCandidates:
    List<CatalogOFFNutritionRetrievalMissingRequest>
)

data class CatalogOFFNutritionRetrievalMissingRequest(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,
    val itemName: String,
    val category: String?,
    val production: String?
)