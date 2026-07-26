package de.shopme.tools.knowledge.off.nutrition.reference.aggregation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class OFFNutritionReferenceAggregationReportWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
) {

    fun write(
        result: OFFNutritionReferenceAggregationResult,
        outputFile: File
    ): File {

        outputFile.parentFile?.mkdirs()

        require(
            outputFile.parentFile?.isDirectory == true
        ) {
            "Could not create aggregation report directory: " +
                    outputFile.parentFile?.absolutePath
        }

        val profileCountDistribution =
            result.aggregates
                .groupingBy { aggregate ->
                    aggregate.profileCount
                }
                .eachCount()
                .toSortedMap()

        val largestAggregates =
            result.aggregates
                .sortedWith(
                    compareByDescending<
                            CanonicalOFFNutritionReferenceAggregate
                            > {
                        it.profileCount
                    }
                        .thenBy {
                            it.canonicalId
                        }
                )
                .take(LARGEST_AGGREGATE_LIMIT)
                .map { aggregate ->
                    OFFNutritionReferenceAggregationReportEntry(
                        canonicalId =
                            aggregate.canonicalId,
                        profileCount =
                            aggregate.profileCount,
                        representativeSourceId =
                            aggregate.representativeSourceId,
                        nutrientCount =
                            aggregate.nutrition.size
                    )
                }

        val report =
            OFFNutritionReferenceAggregationReport(
                version =
                    1,
                inputCandidateCount =
                    result.inputCandidateCount,
                aggregateCount =
                    result.aggregateCount,
                singleProfileAggregateCount =
                    result.singleProfileAggregateCount,
                multiProfileAggregateCount =
                    result.multiProfileAggregateCount,
                maximumProfileCount =
                    result.maximumProfileCount,
                profileCountDistribution =
                    profileCountDistribution,
                largestAggregates =
                    largestAggregates
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
                "Could not replace aggregation report: " +
                        outputFile.absolutePath
            }
        }

        require(temporaryFile.renameTo(outputFile)) {
            "Could not move aggregation report into place: " +
                    outputFile.absolutePath
        }

        return outputFile
    }

    private companion object {

        const val LARGEST_AGGREGATE_LIMIT =
            100
    }
}

data class OFFNutritionReferenceAggregationReport(
    val version: Int,
    val inputCandidateCount: Int,
    val aggregateCount: Int,
    val singleProfileAggregateCount: Int,
    val multiProfileAggregateCount: Int,
    val maximumProfileCount: Int,
    val profileCountDistribution: Map<Int, Int>,
    val largestAggregates:
    List<OFFNutritionReferenceAggregationReportEntry>
)

data class OFFNutritionReferenceAggregationReportEntry(
    val canonicalId: String,
    val profileCount: Int,
    val representativeSourceId: String,
    val nutrientCount: Int
)