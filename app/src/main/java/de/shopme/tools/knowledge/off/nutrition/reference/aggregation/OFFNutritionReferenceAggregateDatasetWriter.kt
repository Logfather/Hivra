package de.shopme.tools.knowledge.off.nutrition.reference.aggregation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class OFFNutritionReferenceAggregateDatasetWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
) {

    fun write(
        aggregates: List<CanonicalOFFNutritionReferenceAggregate>,
        outputFile: File
    ): OFFNutritionReferenceAggregateDatasetWriteResult {

        val sortedAggregates =
            aggregates.sortedBy { aggregate ->
                aggregate.canonicalId
            }

        require(
            sortedAggregates
                .map { aggregate ->
                    aggregate.canonicalId
                }
                .distinct()
                .size ==
                    sortedAggregates.size
        ) {
            "Aggregate dataset contains duplicate canonical IDs."
        }

        outputFile.parentFile?.mkdirs()

        require(
            outputFile.parentFile?.isDirectory == true
        ) {
            "Could not create aggregate output directory: " +
                    outputFile.parentFile?.absolutePath
        }

        val temporaryFile =
            File(
                outputFile.parentFile,
                "${outputFile.name}.tmp"
            )

        temporaryFile.writeText(
            gson.toJson(sortedAggregates) + "\n"
        )

        if (outputFile.exists()) {
            require(outputFile.delete()) {
                "Could not replace aggregate dataset: " +
                        outputFile.absolutePath
            }
        }

        require(temporaryFile.renameTo(outputFile)) {
            "Could not move aggregate dataset into place: " +
                    outputFile.absolutePath
        }

        return OFFNutritionReferenceAggregateDatasetWriteResult(
            aggregateCount =
                sortedAggregates.size,
            fileSizeBytes =
                outputFile.length(),
            outputFile =
                outputFile
        )
    }
}

data class OFFNutritionReferenceAggregateDatasetWriteResult(
    val aggregateCount: Int,
    val fileSizeBytes: Long,
    val outputFile: File
) {

    init {
        require(aggregateCount >= 0) {
            "aggregateCount must not be negative."
        }

        require(fileSizeBytes >= 0L) {
            "fileSizeBytes must not be negative."
        }
    }
}