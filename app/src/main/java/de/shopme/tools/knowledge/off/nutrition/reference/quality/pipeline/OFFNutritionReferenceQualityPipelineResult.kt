package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline

import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerationResult
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceDatasetWriteResult
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilterResult
import de.shopme.tools.knowledge.off.nutrition.reference.quality.rejection.OFFNutritionReferenceQualityRejectionWriteResult
import de.shopme.tools.knowledge.off.nutrition.reference.quality.report.OFFNutritionReferenceQualityReport
import de.shopme.tools.knowledge.off.nutrition.reference.quality.report.OFFNutritionReferenceQualityReportWriteResult
import java.io.File

data class OFFNutritionReferenceQualityPipelineResult(
    val inputFile: File,
    val maxCandidates: Int?,
    val extractedCandidateCount: Int,
    val generationResult: OFFNutritionReferenceCandidateGenerationResult,
    val qualityFilterResult: OFFNutritionReferenceQualityFilterResult,
    val qualityReport: OFFNutritionReferenceQualityReport,
    val acceptedCandidatesWriteResult:
    OFFNutritionReferenceDatasetWriteResult,
    val rejectedCandidatesWriteResult:
    OFFNutritionReferenceQualityRejectionWriteResult,
    val qualityReportWriteResult:
    OFFNutritionReferenceQualityReportWriteResult
) {

    val generatedCandidateCount: Int
        get() =
            generationResult.generatedCandidateCount

    val acceptedCandidateCount: Int
        get() =
            qualityFilterResult.acceptedCandidateCount

    val rejectedCandidateCount: Int
        get() =
            qualityFilterResult.rejectedCandidateCount

    val processedUnlimitedInput: Boolean
        get() =
            maxCandidates == null

    init {
        require(extractedCandidateCount >= 0) {
            "extractedCandidateCount must not be negative."
        }

        require(
            generatedCandidateCount ==
                    acceptedCandidateCount + rejectedCandidateCount
        ) {
            "Generated candidate count must equal accepted plus rejected: " +
                    "generated=$generatedCandidateCount, " +
                    "accepted=$acceptedCandidateCount, " +
                    "rejected=$rejectedCandidateCount."
        }

        require(
            acceptedCandidatesWriteResult.candidateCount ==
                    acceptedCandidateCount
        ) {
            "Persisted accepted candidate count does not match filter result."
        }

        require(
            rejectedCandidatesWriteResult.rejectionCount ==
                    rejectedCandidateCount
        ) {
            "Persisted rejection count does not match filter result."
        }

        require(
            qualityReport.inputCandidateCount ==
                    generatedCandidateCount
        ) {
            "Quality report input count does not match generated candidates."
        }

        require(
            qualityReport.acceptedCandidateCount ==
                    acceptedCandidateCount
        ) {
            "Quality report accepted count does not match filter result."
        }

        require(
            qualityReport.rejectedCandidateCount ==
                    rejectedCandidateCount
        ) {
            "Quality report rejected count does not match filter result."
        }

        require(inputFile.isFile) {
            "Processed OFF input file no longer exists: " +
                    inputFile.absolutePath
        }

        require(maxCandidates == null || maxCandidates > 0) {
            "maxCandidates must be null or greater than zero."
        }
    }
}