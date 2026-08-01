package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

import java.io.File

data class StreamingOFFNutritionReferenceQualityPipelineRequest(
    val inputFile: File,
    val acceptedCandidatesFile: File,
    val rejectedCandidatesFile: File,
    val qualityReportFile: File,
    val qualityDistributionReportFile: File? = null,
    val batchSize: Int = DEFAULT_BATCH_SIZE,
    val maxCandidates: Int? = null,
    val progressInterval: Int = DEFAULT_PROGRESS_INTERVAL
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

        require(batchSize > 0) {
            "batchSize must be greater than zero."
        }

        require(maxCandidates == null || maxCandidates > 0) {
            "maxCandidates must be null or greater than zero."
        }

        require(progressInterval > 0) {
            "progressInterval must be greater than zero."
        }

        val input =
            inputFile.canonicalFile

        val outputs =
            listOfNotNull(
                acceptedCandidatesFile,
                rejectedCandidatesFile,
                qualityReportFile,
                qualityDistributionReportFile
            )
                .map(File::getCanonicalFile)

        require(outputs.toSet().size == outputs.size) {
            "Streaming pipeline output files must be distinct."
        }

        require(input !in outputs) {
            "OFF input file must not also be used as output."
        }

        require(
            acceptedCandidatesFile.extension.equals(
                other = "jsonl",
                ignoreCase = true
            )
        ) {
            "Accepted candidate output must use JSONL: " +
                    acceptedCandidatesFile.absolutePath
        }

        require(
            rejectedCandidatesFile.extension.equals(
                other = "jsonl",
                ignoreCase = true
            )
        ) {
            "Rejected candidate output must use JSONL: " +
                    rejectedCandidatesFile.absolutePath
        }
    }

    companion object {

        const val DEFAULT_BATCH_SIZE =
            1_000

        const val DEFAULT_PROGRESS_INTERVAL =
            100_000
    }
}