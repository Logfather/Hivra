package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline

import java.io.File

data class OFFNutritionReferenceQualityPipelineRequest(
    val inputFile: File,
    val outputFiles: OFFNutritionReferenceQualityPipelineOutputFiles,
    val maxCandidates: Int? = null
) {

    init {
        require(inputFile.isFile) {
            "OFF dump not found: ${inputFile.absolutePath}"
        }

        require(
            inputFile.name.endsWith(
                suffix = ".jsonl.gz",
                ignoreCase = true
            )
        ) {
            "OFF input must be a gzipped JSONL file: " +
                    inputFile.absolutePath
        }

        require(maxCandidates == null || maxCandidates > 0) {
            "maxCandidates must be null or greater than zero."
        }

        requireInputIsNotAnOutput()
    }

    val isUnlimited: Boolean
        get() =
            maxCandidates == null

    private fun requireInputIsNotAnOutput() {
        val canonicalInput =
            inputFile.canonicalFile

        val canonicalOutputs =
            listOf(
                outputFiles.acceptedCandidatesFile,
                outputFiles.rejectedCandidatesFile,
                outputFiles.qualityReportFile
            )
                .map(File::getCanonicalFile)

        require(canonicalInput !in canonicalOutputs) {
            "OFF input file must not also be used as an output file."
        }
    }
}