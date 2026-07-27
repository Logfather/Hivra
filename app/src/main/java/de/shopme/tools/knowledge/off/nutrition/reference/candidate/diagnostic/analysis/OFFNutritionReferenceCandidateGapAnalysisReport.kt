package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis

data class OFFNutritionReferenceCandidateGapAnalysisReport(
    val version: Int,
    val sourceDiagnosticVersion: Int,
    val sourceRequestCount: Int,
    val sourceMissingRequestCount: Int,
    val sourceRawOFFScannedProductCount: Long,
    val analyzedFindingCount: Int,
    val referenceCandidateGapCount: Int,
    val countsByFirstMissingStage: Map<String, Int>,
    val countsByCause: Map<OFFNutritionReferenceCandidateGapCause, Int>,
    val countsByPriority: Map<OFFNutritionReferenceCandidateGapPriority, Int>,
    val findings: List<OFFNutritionReferenceCandidateGapAnalysisFinding>
) {

    init {
        require(version > 0) {
            "version must be greater than zero."
        }

        require(sourceDiagnosticVersion > 0) {
            "sourceDiagnosticVersion must be greater than zero."
        }

        require(sourceRequestCount >= 0) {
            "sourceRequestCount must not be negative."
        }

        require(sourceMissingRequestCount >= 0) {
            "sourceMissingRequestCount must not be negative."
        }

        require(sourceRawOFFScannedProductCount >= 0L) {
            "sourceRawOFFScannedProductCount must not be negative."
        }

        require(analyzedFindingCount == findings.size) {
            "analyzedFindingCount must equal findings.size."
        }

        require(referenceCandidateGapCount >= 0) {
            "referenceCandidateGapCount must not be negative."
        }

        require(
            countsByFirstMissingStage.values.sum() ==
                    analyzedFindingCount
        ) {
            "Stage counts must cover all analyzed findings."
        }

        require(
            countsByCause.values.sum() ==
                    analyzedFindingCount
        ) {
            "Cause counts must cover all analyzed findings."
        }

        require(
            countsByPriority.values.sum() ==
                    analyzedFindingCount
        ) {
            "Priority counts must cover all analyzed findings."
        }

        require(
            referenceCandidateGapCount ==
                    findings.count { finding ->
                        finding.firstMissingStage ==
                                FIRST_MISSING_STAGE_REFERENCE_CANDIDATE
                    }
        ) {
            "referenceCandidateGapCount does not match findings."
        }

        require(
            findings ==
                    findings.sortedWith(FINDING_COMPARATOR)
        ) {
            "findings must be deterministically sorted."
        }
    }

    companion object {

        const val FIRST_MISSING_STAGE_REFERENCE_CANDIDATE =
            "REFERENCE_CANDIDATE"

        val FINDING_COMPARATOR:
                Comparator<OFFNutritionReferenceCandidateGapAnalysisFinding> =
            compareBy<OFFNutritionReferenceCandidateGapAnalysisFinding>(
                { finding ->
                    finding.priority.sortOrder
                },
                { finding ->
                    finding.cause.name
                },
                { finding ->
                    finding.catalogIndex
                },
                { finding ->
                    finding.catalogKey
                }
            )

        private val OFFNutritionReferenceCandidateGapPriority.sortOrder: Int
            get() =
                when (this) {
                    OFFNutritionReferenceCandidateGapPriority.HIGH ->
                        0

                    OFFNutritionReferenceCandidateGapPriority.MEDIUM ->
                        1

                    OFFNutritionReferenceCandidateGapPriority.LOW ->
                        2
                }
    }
}