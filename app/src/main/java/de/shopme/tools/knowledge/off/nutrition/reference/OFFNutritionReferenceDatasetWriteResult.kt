package de.shopme.tools.knowledge.off.nutrition.reference

import java.io.File

/**
 * Ergebnis der Persistierung eines kanonischen OFF-Nutrition-Referenzdatasets.
 */
data class OFFNutritionReferenceDatasetWriteResult(
    val outputFile: File,
    val candidateCount: Int,
    val fileSizeBytes: Long
) {

    init {
        require(candidateCount >= 0) {
            "candidateCount must not be negative."
        }

        require(fileSizeBytes > 0L) {
            "fileSizeBytes must be positive."
        }

        require(outputFile.isFile) {
            "Persisted OFF nutrition reference dataset does not exist: " +
                    outputFile.absolutePath
        }

        require(outputFile.length() == fileSizeBytes) {
            "Persisted OFF nutrition reference dataset size mismatch: " +
                    "reported=$fileSizeBytes, actual=${outputFile.length()}."
        }
    }
}