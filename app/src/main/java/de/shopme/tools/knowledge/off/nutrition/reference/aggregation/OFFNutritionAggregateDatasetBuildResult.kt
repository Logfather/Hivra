package de.shopme.tools.knowledge.off.nutrition.reference.aggregation

import java.io.File

data class OFFNutritionAggregateDatasetBuildResult(
    val inputReferenceCount: Long,
    val deduplicatedReferenceCount: Long,
    val removedDuplicateCount: Long,
    val duplicateGroupCount: Long,
    val aggregateCount: Int,
    val singleProfileAggregateCount: Int,
    val multiProfileAggregateCount: Int,
    val maximumProfileCount: Int,
    val aggregateDatasetFile: File,
    val aggregationReportFile: File,
    val aggregateDatasetFileSizeBytes: Long
) {

    init {
        require(inputReferenceCount >= 0L) {
            "inputReferenceCount must not be negative."
        }

        require(deduplicatedReferenceCount >= 0L) {
            "deduplicatedReferenceCount must not be negative."
        }

        require(removedDuplicateCount >= 0L) {
            "removedDuplicateCount must not be negative."
        }

        require(duplicateGroupCount >= 0L) {
            "duplicateGroupCount must not be negative."
        }

        require(aggregateCount >= 0) {
            "aggregateCount must not be negative."
        }

        require(singleProfileAggregateCount >= 0) {
            "singleProfileAggregateCount must not be negative."
        }

        require(multiProfileAggregateCount >= 0) {
            "multiProfileAggregateCount must not be negative."
        }

        require(maximumProfileCount >= 0) {
            "maximumProfileCount must not be negative."
        }

        require(
            inputReferenceCount ==
                    deduplicatedReferenceCount +
                    removedDuplicateCount
        ) {
            "Input references must equal deduplicated references plus " +
                    "removed duplicates."
        }

        require(
            aggregateCount ==
                    singleProfileAggregateCount +
                    multiProfileAggregateCount
        ) {
            "Single- and multi-profile counts must cover all aggregates."
        }

        require(aggregateDatasetFile.isFile) {
            "Aggregate dataset file does not exist: " +
                    aggregateDatasetFile.absolutePath
        }

        require(aggregationReportFile.isFile) {
            "Aggregation report file does not exist: " +
                    aggregationReportFile.absolutePath
        }

        require(aggregateDatasetFileSizeBytes > 0L) {
            "Aggregate dataset file must not be empty."
        }

        require(
            aggregateDatasetFile.length() ==
                    aggregateDatasetFileSizeBytes
        ) {
            "Aggregate dataset file size does not match result."
        }
    }
}