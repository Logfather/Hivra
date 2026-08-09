package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

data class CanonicalBoundedSemanticPolicyClosurePlan(
    val version: Int,
    val planId: String,
    val firstWaveNumber: Int,
    val finalWaveNumber: Int,
    val sourceReviewRequiredCandidateCount: Int,
    val sourceMissingPolicyGapCount: Int,
    val sourceImplementationBatchCount: Int,
    val waveCount: Int,
    val waves: List<CanonicalBoundedSemanticPolicyClosureWave>,
    val assignedGapCount: Int,
    val uniqueGapIdentityCount: Int,
    val completeGapCoverage: Boolean,
    val deterministicOrderValid: Boolean,
    val boundedByFinalWave: Boolean,
    val valid: Boolean
) {

    init {
        require(version > 0)
        require(planId.isNotBlank())
        require(firstWaveNumber > 0)
        require(finalWaveNumber >= firstWaveNumber)
        require(sourceReviewRequiredCandidateCount >= 0)
        require(sourceMissingPolicyGapCount >= 0)
        require(sourceImplementationBatchCount >= 0)
        require(waveCount == waves.size)
        require(assignedGapCount >= 0)
        require(uniqueGapIdentityCount >= 0)
        require(
            waveCount ==
                    finalWaveNumber -
                    firstWaveNumber +
                    1
        )
    }

    companion object {
        const val CURRENT_VERSION =
            1
    }
}