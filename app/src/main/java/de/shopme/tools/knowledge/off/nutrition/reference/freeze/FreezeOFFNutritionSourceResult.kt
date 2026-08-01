package de.shopme.tools.knowledge.off.nutrition.reference.freeze

import java.io.File

data class FreezeOFFNutritionSourceResult(
    val sourceAggregateFile: File,
    val frozenAggregateFile: File,
    val snapshotFile: File,
    val sourceFileSizeBytes: Long,
    val frozenFileSizeBytes: Long,
    val sourceSha256: String,
    val frozenSha256: String,
    val aggregateEntryCount: Long,
    val snapshot: OFFNutritionSourceSnapshot,
    val frozenAggregateChanged: Boolean,
    val snapshotChanged: Boolean
) {

    init {
        require(sourceAggregateFile.isFile) {
            "OFF Nutrition source aggregate file does not exist."
        }

        require(frozenAggregateFile.isFile) {
            "Frozen OFF Nutrition aggregate file does not exist."
        }

        require(snapshotFile.isFile) {
            "OFF Nutrition source snapshot file does not exist."
        }

        require(sourceFileSizeBytes > 0L)
        require(frozenFileSizeBytes > 0L)

        require(
            sourceFileSizeBytes ==
                    frozenFileSizeBytes
        )

        require(
            sourceSha256 ==
                    frozenSha256
        )

        require(aggregateEntryCount > 0L)

        require(
            snapshot.sourceSha256 ==
                    sourceSha256
        )

        require(
            snapshot.aggregateEntryCount ==
                    aggregateEntryCount
        )
    }
}