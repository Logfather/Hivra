package de.shopme.tools.knowledge.off.nutrition.reference.persistence

import java.io.File

data class OFFAcceptedNutritionReferenceWriteResult(
    val outputFile: File,
    val writtenReferenceCount: Long,
    val contentSha256: String,
    val uncompressedContentBytes: Long
) {

    init {
        require(writtenReferenceCount >= 0L)
        require(uncompressedContentBytes >= 0L)

        require(
            contentSha256.matches(
                Regex("[0-9a-f]{64}")
            )
        )
    }
}