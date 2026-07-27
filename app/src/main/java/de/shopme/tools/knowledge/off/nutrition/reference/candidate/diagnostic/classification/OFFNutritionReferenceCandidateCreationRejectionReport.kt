package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.classification

data class OFFNutritionReferenceCandidateCreationRejectionReport(
    val version: Int,
    val sourceGapAnalysisVersion: Int,
    val sourceTraceVersion: Int?,
    val candidateNotCreatedFindingCount: Int,
    val classifiedFindingCount: Int,
    val unmatchedFindingCount: Int,
    val matchedTraceCount: Int,
    val rejectedTraceCount: Int,
    val createdTraceCount: Int,
    val countsByFirstRejectionStage:
    Map<OFFNutritionReferenceCandidateCreationRejectionStage, Int>,
    val countsByRejectionStage:
    Map<OFFNutritionReferenceCandidateCreationRejectionStage, Int>,
    val countsByRejectionReason: Map<String, Int>,
    val findings:
    List<OFFNutritionReferenceCandidateCreationRejectionFinding>
) {

    init {
        require(version > 0) {
            "version must be greater than zero."
        }

        require(sourceGapAnalysisVersion > 0) {
            "sourceGapAnalysisVersion must be greater than zero."
        }

        require(candidateNotCreatedFindingCount >= 0) {
            "candidateNotCreatedFindingCount must not be negative."
        }

        require(classifiedFindingCount >= 0) {
            "classifiedFindingCount must not be negative."
        }

        require(unmatchedFindingCount >= 0) {
            "unmatchedFindingCount must not be negative."
        }

        require(
            classifiedFindingCount + unmatchedFindingCount ==
                    candidateNotCreatedFindingCount
        ) {
            "Classified and unmatched findings must cover all source findings."
        }

        require(
            candidateNotCreatedFindingCount ==
                    findings.size
        ) {
            "candidateNotCreatedFindingCount must equal findings.size."
        }

        require(
            matchedTraceCount ==
                    findings.sumOf { finding ->
                        finding.matchedTraceCount
                    }
        ) {
            "matchedTraceCount does not match findings."
        }

        require(
            rejectedTraceCount ==
                    findings.sumOf { finding ->
                        finding.rejectedTraceCount
                    }
        ) {
            "rejectedTraceCount does not match findings."
        }

        require(
            createdTraceCount ==
                    findings.sumOf { finding ->
                        finding.createdTraceCount
                    }
        ) {
            "createdTraceCount does not match findings."
        }

        require(
            countsByFirstRejectionStage.values.sum() ==
                    candidateNotCreatedFindingCount
        ) {
            "First-stage counts must cover all findings."
        }

        require(
            findings ==
                    findings.sortedWith(FINDING_COMPARATOR)
        ) {
            "findings must be deterministically sorted."
        }
    }

    companion object {

        val FINDING_COMPARATOR:
                Comparator<OFFNutritionReferenceCandidateCreationRejectionFinding> =
            compareBy(
                { finding ->
                    finding.firstRejectionStage.sortOrder
                },
                { finding ->
                    finding.catalogIndex
                },
                { finding ->
                    finding.catalogKey
                }
            )

        private val
                OFFNutritionReferenceCandidateCreationRejectionStage.sortOrder:
                Int
            get() =
                when (this) {
                    OFFNutritionReferenceCandidateCreationRejectionStage.IDENTITY ->
                        0

                    OFFNutritionReferenceCandidateCreationRejectionStage.NUTRITION ->
                        1

                    OFFNutritionReferenceCandidateCreationRejectionStage
                        .REFERENCE_ELIGIBILITY ->
                        2

                    OFFNutritionReferenceCandidateCreationRejectionStage
                        .CANDIDATE_CREATION ->
                        3

                    OFFNutritionReferenceCandidateCreationRejectionStage
                        .TRACE_NOT_FOUND ->
                        4

                    OFFNutritionReferenceCandidateCreationRejectionStage.UNKNOWN ->
                        5
                }
    }
}