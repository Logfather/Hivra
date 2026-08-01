package de.shopme.tools.knowledge.off.nutrition.reference.quality.report

import java.io.File

data class OFFNutritionReferenceQualityReportWriteResult(
    val outputFile: File,
    val writtenByteCount: Long
) {

    init {
        require(outputFile.isAbsolute) {
            "Output file must be absolute."
        }

        require(writtenByteCount >= 0L) {
            "Written byte count must not be negative."
        }
    }
}