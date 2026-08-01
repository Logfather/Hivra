package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityRejection
import de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution.OFFNutritionReferenceQualityDistributionAnalyzer
import de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution.OFFNutritionReferenceQualityDistributionReportWriter

class StreamingOFFNutritionReferenceQualityPipeline(
    private val candidateExtractor: OFFCandidateExtractor =
        OFFCandidateExtractor(),
    private val candidateGenerator:
    OFFNutritionReferenceCandidateGenerator =
        OFFNutritionReferenceCandidateGenerator(),
    private val qualityFilter:
    OFFNutritionReferenceQualityFilter =
        OFFNutritionReferenceQualityFilter(),
    private val reportWriter:
    StreamingOFFNutritionReferenceQualityReportWriter =
        StreamingOFFNutritionReferenceQualityReportWriter(),
    private val distributionReportWriter:
    OFFNutritionReferenceQualityDistributionReportWriter =
        OFFNutritionReferenceQualityDistributionReportWriter()
) {

    fun run(
        request: StreamingOFFNutritionReferenceQualityPipelineRequest,
        progressConsumer:
            (StreamingOFFNutritionReferenceQualityStatistics) -> Unit =
            {}
    ): StreamingOFFNutritionReferenceQualityPipelineResult {

        val startedAtNanos =
            System.nanoTime()

        val accumulator =
            OFFNutritionReferenceQualityStatisticsAccumulator()

        val distributionAnalyzer =
            request.qualityDistributionReportFile
                ?.let {
                    OFFNutritionReferenceQualityDistributionAnalyzer()
                }

        val batch =
            ArrayList<CanonicalKnowledgeCandidate>(
                request.batchSize
            )

        AtomicJsonLinesWriter<
                CanonicalOFFNutritionReferenceCandidate
                >(
            request.acceptedCandidatesFile
        ).use { acceptedWriter ->

            AtomicJsonLinesWriter<
                    OFFNutritionReferenceQualityRejection
                    >(
                request.rejectedCandidatesFile
            ).use { rejectedWriter ->

                candidateExtractor.forEachCandidate(
                    file =
                        request.inputFile,
                    maxCandidates =
                        request.maxCandidates
                ) { candidate ->

                    accumulator.recordExtractedCandidate()

                    batch +=
                        candidate

                    if (batch.size >= request.batchSize) {
                        processBatch(
                            batch = batch,
                            accumulator = accumulator,
                            acceptedWriter = acceptedWriter,
                            rejectedWriter = rejectedWriter,
                            distributionAnalyzer =
                                distributionAnalyzer
                        )

                        batch.clear()
                    }

                    val currentStatistics =
                        accumulator.snapshot()

                    if (
                        currentStatistics.extractedCandidateCount %
                        request.progressInterval == 0L
                    ) {
                        progressConsumer(
                            currentStatistics
                        )
                    }
                }

                if (batch.isNotEmpty()) {
                    processBatch(
                        batch = batch,
                        accumulator = accumulator,
                        acceptedWriter = acceptedWriter,
                        rejectedWriter = rejectedWriter,
                        distributionAnalyzer =
                            distributionAnalyzer
                    )

                    batch.clear()
                }

                val statistics =
                    accumulator.snapshot()

                val acceptedWriteResult =
                    acceptedWriter.complete()

                val rejectedWriteResult =
                    rejectedWriter.complete()

                val durationMillis =
                    (System.nanoTime() - startedAtNanos) /
                            NANOS_PER_MILLISECOND

                val report =
                    StreamingOFFNutritionReferenceQualityReport.create(
                        request =
                            request,
                        statistics =
                            statistics,
                        durationMillis =
                            durationMillis
                    )

                val reportWriteResult =
                    reportWriter.write(
                        report =
                            report,
                        outputFile =
                            request.qualityReportFile
                    )

                val qualityDistributionReport =
                    distributionAnalyzer?.createReport()

                val qualityDistributionReportFile =
                    qualityDistributionReport?.let { distributionReport ->
                        distributionReportWriter.write(
                            report =
                                distributionReport,
                            outputFile =
                                requireNotNull(
                                    request.qualityDistributionReportFile
                                )
                        )
                    }

                return StreamingOFFNutritionReferenceQualityPipelineResult(
                    statistics =
                        statistics,
                    acceptedCandidatesWriteResult =
                        acceptedWriteResult,
                    rejectedCandidatesWriteResult =
                        rejectedWriteResult,
                    report =
                        report,
                    reportWriteResult =
                        reportWriteResult,
                    qualityDistributionReport =
                        qualityDistributionReport,
                    qualityDistributionReportFile =
                        qualityDistributionReportFile
                )
            }
        }
    }

    private fun processBatch(
        batch: List<CanonicalKnowledgeCandidate>,
        accumulator:
        OFFNutritionReferenceQualityStatisticsAccumulator,
        acceptedWriter:
        AtomicJsonLinesWriter<
                CanonicalOFFNutritionReferenceCandidate
                >,
        rejectedWriter:
        AtomicJsonLinesWriter<
                OFFNutritionReferenceQualityRejection
                >,
        distributionAnalyzer:
        OFFNutritionReferenceQualityDistributionAnalyzer?
    ) {
        val generationResult =
            candidateGenerator.generate(
                candidates = batch
            )

        accumulator.addGenerationResult(
            generationResult
        )

        if (generationResult.candidates.isEmpty()) {
            return
        }

        val qualityResult =
            qualityFilter.filter(
                candidates =
                    generationResult.candidates
            )

        accumulator.addQualityResult(
            qualityResult
        )

        distributionAnalyzer?.observe(
            candidates =
                generationResult.candidates,
            qualityResult =
                qualityResult
        )

        acceptedWriter.writeAll(
            qualityResult.acceptedCandidates
        )

        rejectedWriter.writeAll(
            qualityResult.rejections
        )
    }

    private companion object {

        const val NANOS_PER_MILLISECOND =
            1_000_000L
    }
}