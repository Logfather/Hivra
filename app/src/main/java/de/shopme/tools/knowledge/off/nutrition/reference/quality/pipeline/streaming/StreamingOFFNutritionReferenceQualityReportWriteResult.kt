package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

import java.io.File

data class StreamingOFFNutritionReferenceQualityReportWriteResult(
    val outputFile: File,
    val writtenByteCount: Long
) {

    init {
        require(writtenByteCount >= 0L)
    }
}