package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis

import java.io.File

data class OFFNutritionReferenceCandidateGapAnalysisWriteResult(
    val findingCount: Int,
    val outputFile: File,
    val fileSizeBytes: Long
) {

    init {
        require(findingCount >= 0) {
            "findingCount must not be negative."
        }

        require(fileSizeBytes > 0L) {
            "fileSizeBytes must be greater than zero."
        }

        require(outputFile.isFile) {
            "outputFile must exist."
        }

        require(outputFile.length() == fileSizeBytes) {
            "fileSizeBytes must equal outputFile.length()."
        }
    }
}