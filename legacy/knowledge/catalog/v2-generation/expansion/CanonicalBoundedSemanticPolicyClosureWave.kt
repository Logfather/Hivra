package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

data class CanonicalBoundedSemanticPolicyClosureWave(
    val waveNumber: Int,
    val waveKey: String,
    val assignedBatchCount: Int,
    val assignedGapCount: Int,
    val affectedCandidateReferenceCount: Int,
    val assignments:
    List<CanonicalBoundedSemanticPolicyClosureAssignment>,
    val complete: Boolean,
    val valid: Boolean
) {

    init {
        require(waveNumber > 0)
        require(waveKey.isNotBlank())
        require(assignedBatchCount >= 0)
        require(assignedGapCount >= 0)
        require(affectedCandidateReferenceCount >= 0)
        require(assignedGapCount == assignments.size)
    }
}