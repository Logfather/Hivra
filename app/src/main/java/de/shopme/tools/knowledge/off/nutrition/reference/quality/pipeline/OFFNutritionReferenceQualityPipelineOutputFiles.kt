package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline

import java.io.File

data class OFFNutritionReferenceQualityPipelineOutputFiles(
    val acceptedCandidatesFile: File,
    val rejectedCandidatesFile: File,
    val qualityReportFile: File
) {

    init {
        requireDistinctFiles()
    }

    private fun requireDistinctFiles() {
        val canonicalFiles =
            listOf(
                acceptedCandidatesFile,
                rejectedCandidatesFile,
                qualityReportFile
            )
                .map(File::getCanonicalFile)

        require(canonicalFiles.toSet().size == canonicalFiles.size) {
            "OFF nutrition quality pipeline output files must be distinct."
        }
    }

    companion object {

        fun inDirectory(
            outputDirectory: File
        ): OFFNutritionReferenceQualityPipelineOutputFiles {

            val canonicalDirectory =
                outputDirectory.canonicalFile

            return OFFNutritionReferenceQualityPipelineOutputFiles(
                acceptedCandidatesFile =
                    canonicalDirectory.resolve(
                        "nutrition-reference-candidates.quality-filtered.json"
                    ),
                rejectedCandidatesFile =
                    canonicalDirectory.resolve(
                        "nutrition-reference-candidates.rejected.json"
                    ),
                qualityReportFile =
                    canonicalDirectory.resolve(
                        "off-nutrition-reference-quality.json"
                    )
            )
        }
    }
}