package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace

data class OFFNutritionMissingGeneratorTraceReport(
    val version: Int,
    val sourceRejectionReportVersion: Int,
    val missingTraceFindingCount: Int,
    val diagnosedFindingCount: Int,
    val countsByFirstMissingStage:
    Map<OFFNutritionMissingGeneratorTraceStage, Int>,
    val countsByCause:
    Map<OFFNutritionMissingGeneratorTraceCause, Int>,
    val sourceCandidateMatchedFindingCount: Int,
    val qualityFilteredCandidateMatchedFindingCount: Int,
    val deduplicatedCandidateMatchedFindingCount: Int,
    val generatorTraceMatchedFindingCount: Int,
    val findings: List<OFFNutritionMissingGeneratorTraceFinding>
) {

    init {
        require(version > 0) {
            "version must be greater than zero."
        }

        require(sourceRejectionReportVersion > 0) {
            "sourceRejectionReportVersion must be greater than zero."
        }

        require(missingTraceFindingCount >= 0) {
            "missingTraceFindingCount must not be negative."
        }

        require(diagnosedFindingCount >= 0) {
            "diagnosedFindingCount must not be negative."
        }

        require(missingTraceFindingCount == findings.size) {
            "missingTraceFindingCount must equal findings.size."
        }

        require(diagnosedFindingCount == findings.size) {
            "Every missing-trace finding must receive a diagnosis."
        }

        require(
            countsByFirstMissingStage.values.sum() ==
                    missingTraceFindingCount
        ) {
            "Missing-stage counts must cover all findings."
        }

        require(
            countsByCause.values.sum() ==
                    missingTraceFindingCount
        ) {
            "Cause counts must cover all findings."
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
                Comparator<OFFNutritionMissingGeneratorTraceFinding> =
            compareBy(
                { finding ->
                    finding.firstMissingStage.sortOrder
                },
                { finding ->
                    finding.catalogIndex
                },
                { finding ->
                    finding.catalogKey
                }
            )

        private val OFFNutritionMissingGeneratorTraceStage.sortOrder: Int
            get() =
                when (this) {
                    OFFNutritionMissingGeneratorTraceStage
                        .SOURCE_REFERENCE_CANDIDATE ->
                        0

                    OFFNutritionMissingGeneratorTraceStage
                        .QUALITY_FILTERED_REFERENCE_CANDIDATE ->
                        1

                    OFFNutritionMissingGeneratorTraceStage
                        .DEDUPLICATED_REFERENCE_CANDIDATE ->
                        2

                    OFFNutritionMissingGeneratorTraceStage
                        .GENERATOR_TRACE ->
                        3

                    OFFNutritionMissingGeneratorTraceStage.UNKNOWN ->
                        4
                }
    }
}