package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

import de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming.StreamingOFFNutritionReferenceQualityPipeline
import de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming.StreamingOFFNutritionReferenceQualityPipelineRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RunStreamingFullDumpOFFNutritionReferenceQualityPipelineTest {

    @Test
    fun runStreamingPipelineOnFullOFFDump() {

        val workingDirectory =
            File(".").canonicalFile

        val projectRoot =
            if (workingDirectory.name == "app") {
                requireNotNull(
                    workingDirectory.parentFile
                ).canonicalFile
            } else {
                workingDirectory
            }

        val inputFile =
            System.getenv("OFF_FULL_DUMP_FILE")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let(::File)
                ?.canonicalFile
                ?: projectRoot.resolve(
                    "data/raw/openfoodfacts/" +
                            "openfoodfacts-products.jsonl.gz"
                )
                    .canonicalFile

        val distributionReportFile =
            projectRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "off-nutrition-reference-quality-" +
                        "distribution-full.json"
            )

        require(inputFile.isFile) {
            "Full OFF dump not found: ${inputFile.absolutePath}"
        }

        require(
            inputFile.length() >=
                    MINIMUM_FULL_DUMP_SIZE_BYTES
        ) {
            "Input is too small to be the full OFF dump: " +
                    "${inputFile.length()} bytes."
        }

        val acceptedFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/full/" +
                        "nutrition-reference-candidates." +
                        "quality-filtered.jsonl"
            )

        val rejectedFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/full/" +
                        "nutrition-reference-candidates.rejected.jsonl"
            )

        val reportFile =
            projectRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "off-nutrition-reference-quality-full.json"
            )

        val result =
            StreamingOFFNutritionReferenceQualityPipeline()
                .run(
                    request =
                        StreamingOFFNutritionReferenceQualityPipelineRequest(
                            inputFile =
                                inputFile,
                            acceptedCandidatesFile =
                                acceptedFile,
                            rejectedCandidatesFile =
                                rejectedFile,
                            qualityReportFile =
                                reportFile,
                            qualityDistributionReportFile =
                                distributionReportFile,
                            batchSize =
                                1_000,
                            maxCandidates =
                                null,
                            progressInterval =
                                100_000
                        ),
                    progressConsumer = { statistics ->
                        println(
                            "OFF streaming progress: " +
                                    "extracted=" +
                                    statistics.extractedCandidateCount +
                                    ", generated=" +
                                    statistics.generatedCandidateCount +
                                    ", accepted=" +
                                    statistics.acceptedCandidateCount +
                                    ", rejected=" +
                                    statistics.rejectedCandidateCount
                        )
                    }
                )

        val statistics =
            result.statistics

        assertTrue(
            statistics.extractedCandidateCount >
                    MINIMUM_EXPECTED_EXTRACTED_CANDIDATES
        )

        assertEquals(
            statistics.generatedCandidateCount,
            statistics.acceptedCandidateCount +
                    statistics.rejectedCandidateCount
        )

        assertEquals(
            statistics.acceptedCandidateCount,
            result.acceptedCandidatesWriteResult.itemCount
        )

        assertEquals(
            statistics.rejectedCandidateCount,
            result.rejectedCandidatesWriteResult.itemCount
        )

        assertTrue(acceptedFile.isFile)
        assertTrue(rejectedFile.isFile)
        assertTrue(reportFile.isFile)

        val distributionReport =
            requireNotNull(
                result.qualityDistributionReport
            )

        assertEquals(
            statistics.generatedCandidateCount,
            distributionReport.analyzedCandidateCount
        )

        assertEquals(
            statistics.rejectedCandidateCount,
            distributionReport.rejectedCandidateCount
        )

        assertEquals(
            statistics.rejectionReasonOccurrenceCount,
            distributionReport.rejectionReasonOccurrenceCount
        )

        assertTrue(
            distributionReportFile.isFile
        )

        println()
        println(SEPARATOR)
        println(
            "STREAMING OFF NUTRITION QUALITY – FULL DUMP"
        )
        println(SEPARATOR)
        println(
            "inputFile=${inputFile.absolutePath}"
        )
        println(
            "inputFileSizeBytes=${inputFile.length()}"
        )
        println(
            "extractedCandidates=" +
                    statistics.extractedCandidateCount
        )
        println(
            "generatedCandidates=" +
                    statistics.generatedCandidateCount
        )
        println(
            "acceptedCandidates=" +
                    statistics.acceptedCandidateCount
        )
        println(
            "rejectedCandidates=" +
                    statistics.rejectedCandidateCount
        )
        println(
            "acceptanceRate=" +
                    statistics.acceptanceRate
        )
        println(
            "rejectionReasonOccurrences=" +
                    statistics.rejectionReasonOccurrenceCount
        )
        println(
            "countsByReason=" +
                    statistics.countsByReason
        )
        println(
            "durationMillis=" +
                    result.report.durationMillis
        )
        println(
            "acceptedFile=${acceptedFile.absolutePath}"
        )
        println(
            "rejectedFile=${rejectedFile.absolutePath}"
        )
        println(
            "reportFile=${reportFile.absolutePath}"
        )
        println(
            "distributionReportFile=" +
                    distributionReportFile.absolutePath
        )

        println(
            "multipleReasonRejections=" +
                    distributionReport.multipleReasonRejectionCount
        )

        println(
            "reasonCombinations=" +
                    distributionReport.countsByReasonCombination
        )

        println(
            "aboveMaximumByKey=" +
                    distributionReport.aboveMaximumCountsByNutritionKey
        )

        println(
            "macronutrientSumBuckets=" +
                    distributionReport.macronutrientSumBuckets
        )
        println(SEPARATOR)
    }

    private companion object {

        const val MINIMUM_FULL_DUMP_SIZE_BYTES =
            100L * 1024L * 1024L

        const val MINIMUM_EXPECTED_EXTRACTED_CANDIDATES =
            100_000L

        const val SEPARATOR =
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    }
}