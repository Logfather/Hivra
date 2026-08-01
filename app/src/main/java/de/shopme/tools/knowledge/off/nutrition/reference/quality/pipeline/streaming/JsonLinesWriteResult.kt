package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

import java.io.File

data class JsonLinesWriteResult(
    val outputFile: File,
    val itemCount: Long,
    val writtenByteCount: Long
) {

    init {
        require(itemCount >= 0L) {
            "itemCount must not be negative."
        }

        require(writtenByteCount >= 0L) {
            "writtenByteCount must not be negative."
        }
    }
}