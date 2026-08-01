package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

import de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming.StreamingOFFNutritionReferenceQualityPipeline
import de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming.StreamingOFFNutritionReferenceQualityPipelineRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class StreamingOFFNutritionReferenceQualityPipelineTest {

    private companion object {

        const val FULL_DUMP_ENVIRONMENT_VARIABLE =
            "OFF_FULL_DUMP_FILE"
    }

    @Test
    fun run_streamsCandidatesAndPersistsConsistentArtifacts() {

        val inputFile =
            resolveFullOFFDump()

        val outputDirectory =
            Files.createTempDirectory(
                "off-nutrition-streaming-test"
            )
                .toFile()



        val result =
            StreamingOFFNutritionReferenceQualityPipeline()
                .run(
                    StreamingOFFNutritionReferenceQualityPipelineRequest(
                        inputFile =
                            inputFile,
                        acceptedCandidatesFile =
                            outputDirectory.resolve(
                                "accepted.jsonl"
                            ),
                        rejectedCandidatesFile =
                            outputDirectory.resolve(
                                "rejected.jsonl"
                            ),
                        qualityReportFile =
                            outputDirectory.resolve(
                                "report.json"
                            ),
                        qualityDistributionReportFile =
                            outputDirectory.resolve(
                                "distribution-report.json"
                            ),
                        batchSize =
                            250,
                        maxCandidates =
                            5_000,
                        progressInterval =
                            1_000
                    )
                )

        val statistics =
            result.statistics

        assertEquals(
            5_000L,
            statistics.extractedCandidateCount
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

        assertTrue(
            result.acceptedCandidatesWriteResult.outputFile.isFile
        )

        assertTrue(
            result.rejectedCandidatesWriteResult.outputFile.isFile
        )

        assertTrue(
            result.reportWriteResult.outputFile.isFile
        )

        assertEquals(
            statistics.acceptedCandidateCount,
            result.acceptedCandidatesWriteResult
                .outputFile
                .useLines { lines ->
                    lines.count().toLong()
                }
        )

        assertEquals(
            statistics.rejectedCandidateCount,
            result.rejectedCandidatesWriteResult
                .outputFile
                .useLines { lines ->
                    lines.count().toLong()
                }
        )

        val distributionReport =
            requireNotNull(
                result.qualityDistributionReport
            )

        assertEquals(
            result.statistics.generatedCandidateCount,
            distributionReport.analyzedCandidateCount
        )

        assertEquals(
            result.statistics.acceptedCandidateCount,
            distributionReport.acceptedCandidateCount
        )

        assertEquals(
            result.statistics.rejectedCandidateCount,
            distributionReport.rejectedCandidateCount
        )

        assertTrue(
            requireNotNull(
                result.qualityDistributionReportFile
            ).isFile
        )
    }

    private fun resolveProjectRoot(): File {
        val currentDirectory =
            File(".").canonicalFile

        return when {
            currentDirectory.name == "app" ->
                requireNotNull(
                    currentDirectory.parentFile
                ) {
                    "Could not resolve project root from: " +
                            currentDirectory.absolutePath
                }
                    .canonicalFile

            currentDirectory.resolve("app").isDirectory ->
                currentDirectory

            else ->
                error(
                    "Could not resolve ShopMe project root from: " +
                            currentDirectory.absolutePath
                )
        }
    }

    private fun resolveFullOFFDump(): File {

        val configured =
            System.getenv(
                FULL_DUMP_ENVIRONMENT_VARIABLE
            )
                ?.takeIf { value ->
                    value.isNotBlank()
                }
                ?.let(::File)
                ?.canonicalFile

        if (configured != null) {
            require(configured.isFile) {
                "Configured OFF dump does not exist: " +
                        configured.absolutePath
            }

            return configured
        }

        val projectRoot =
            resolveProjectRoot()

        val candidates =
            listOf(
                projectRoot.resolve(
                    "data/raw/openfoodfacts/" +
                            "openfoodfacts-products.jsonl.gz"
                ),
                projectRoot.resolve(
                    "data/raw/openfoodfacts/" +
                            "openfoodfacts-products.jsonl"
                )
            )
                .map(File::getCanonicalFile)

        return candidates
            .firstOrNull(File::isFile)
            ?: error(
                buildString {
                    appendLine(
                        "Full OFF dump not found."
                    )
                    appendLine("Checked:")

                    candidates.forEach { candidate ->
                        appendLine(
                            "- ${candidate.absolutePath}"
                        )
                    }

                    append(
                        "Alternatively set " +
                                "$FULL_DUMP_ENVIRONMENT_VARIABLE."
                    )
                }
            )
    }
}