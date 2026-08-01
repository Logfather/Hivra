package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

import de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution.OFFNutritionReferenceQualityDistributionReport
import java.io.File

data class StreamingOFFNutritionReferenceQualityPipelineResult(
    val statistics:
    StreamingOFFNutritionReferenceQualityStatistics,
    val acceptedCandidatesWriteResult:
    JsonLinesWriteResult,
    val rejectedCandidatesWriteResult:
    JsonLinesWriteResult,
    val report:
    StreamingOFFNutritionReferenceQualityReport,
    val reportWriteResult:
    StreamingOFFNutritionReferenceQualityReportWriteResult,
    val qualityDistributionReport:
    OFFNutritionReferenceQualityDistributionReport?,
    val qualityDistributionReportFile:
    File?
) {

    init {
        require(
            acceptedCandidatesWriteResult.itemCount ==
                    statistics.acceptedCandidateCount
        ) {
            "Accepted JSONL count does not match accumulated statistics."
        }

        require(
            rejectedCandidatesWriteResult.itemCount ==
                    statistics.rejectedCandidateCount
        ) {
            "Rejected JSONL count does not match accumulated statistics."
        }

        require(
            report.generatedCandidateCount ==
                    statistics.generatedCandidateCount
        )

        require(
            report.acceptedCandidateCount ==
                    statistics.acceptedCandidateCount
        )

        require(
            report.rejectedCandidateCount ==
                    statistics.rejectedCandidateCount
        )

        require(
            (qualityDistributionReport == null) ==
                    (qualityDistributionReportFile == null)
        ) {
            "Distribution report and output file must both be null or both be present."
        }

        qualityDistributionReport?.let { distributionReport ->
            require(
                distributionReport.analyzedCandidateCount ==
                        statistics.generatedCandidateCount
            )

            require(
                distributionReport.acceptedCandidateCount ==
                        statistics.acceptedCandidateCount
            )

            require(
                distributionReport.rejectedCandidateCount ==
                        statistics.rejectedCandidateCount
            )
        }
    }
}