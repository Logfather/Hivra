package de.shopme.tools.knowledge.off.nutrition.reference.quality.rejection

import java.io.File

data class OFFNutritionReferenceQualityRejectionWriteResult(
    val outputFile: File,
    val rejectionCount: Int,
    val writtenByteCount: Long
) {

    init {
        require(rejectionCount >= 0) {
            "rejectionCount must not be negative."
        }

        require(writtenByteCount >= 0L) {
            "writtenByteCount must not be negative."
        }
    }
}