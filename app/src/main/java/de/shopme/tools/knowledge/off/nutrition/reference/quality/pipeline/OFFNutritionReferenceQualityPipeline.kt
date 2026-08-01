package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline

import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceDatasetWriter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.rejection.OFFNutritionReferenceQualityRejectionWriter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.report.OFFNutritionReferenceQualityReport
import de.shopme.tools.knowledge.off.nutrition.reference.quality.report.OFFNutritionReferenceQualityReportWriter

class OFFNutritionReferenceQualityPipeline(
    private val candidateExtractor: OFFCandidateExtractor =
        OFFCandidateExtractor(),
    private val candidateGenerator: OFFNutritionReferenceCandidateGenerator =
        OFFNutritionReferenceCandidateGenerator(),
    private val qualityFilter: OFFNutritionReferenceQualityFilter =
        OFFNutritionReferenceQualityFilter(),
    private val acceptedCandidatesWriter: OFFNutritionReferenceDatasetWriter =
        OFFNutritionReferenceDatasetWriter(),
    private val rejectedCandidatesWriter:
    OFFNutritionReferenceQualityRejectionWriter =
        OFFNutritionReferenceQualityRejectionWriter(),
    private val qualityReportWriter:
    OFFNutritionReferenceQualityReportWriter =
        OFFNutritionReferenceQualityReportWriter()
) {

    fun run(
        request: OFFNutritionReferenceQualityPipelineRequest
    ): OFFNutritionReferenceQualityPipelineResult {

        val extractedCandidates =
            candidateExtractor.extract(
                file = request.inputFile,
                maxCandidates = request.maxCandidates
            )

        val generationResult =
            candidateGenerator.generate(
                candidates = extractedCandidates
            )

        val qualityFilterResult =
            qualityFilter.filter(
                candidates = generationResult.candidates
            )

        val qualityReport =
            OFFNutritionReferenceQualityReport.from(
                filterResult = qualityFilterResult
            )

        val acceptedCandidatesWriteResult =
            acceptedCandidatesWriter.write(
                candidates =
                    qualityFilterResult.acceptedCandidates,
                outputFile =
                    request.outputFiles.acceptedCandidatesFile
            )

        val rejectedCandidatesWriteResult =
            rejectedCandidatesWriter.write(
                rejections =
                    qualityFilterResult.rejections,
                outputFile =
                    request.outputFiles.rejectedCandidatesFile
            )

        val qualityReportWriteResult =
            qualityReportWriter.write(
                report = qualityReport,
                outputFile =
                    request.outputFiles.qualityReportFile
            )

        return OFFNutritionReferenceQualityPipelineResult(
            inputFile =
                request.inputFile.canonicalFile,
            maxCandidates =
                request.maxCandidates,
            extractedCandidateCount =
                extractedCandidates.size,
            generationResult =
                generationResult,
            qualityFilterResult =
                qualityFilterResult,
            qualityReport =
                qualityReport,
            acceptedCandidatesWriteResult =
                acceptedCandidatesWriteResult,
            rejectedCandidatesWriteResult =
                rejectedCandidatesWriteResult,
            qualityReportWriteResult =
                qualityReportWriteResult
        )
    }
}