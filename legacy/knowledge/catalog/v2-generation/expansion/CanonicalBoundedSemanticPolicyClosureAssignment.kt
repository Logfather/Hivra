package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

data class CanonicalBoundedSemanticPolicyClosureAssignment(
    val assignmentIndex: Int,
    val waveNumber: Int,
    val sourceBatchKey: String,
    val category: String,
    val familyKey: String,
    val axis: String,
    val gapKey: String,
    val affectedCandidateCount: Int,
    val observedValues: List<String>,
    val implementationKey: String
) {

    init {
        require(assignmentIndex > 0)
        require(waveNumber > 0)
        require(sourceBatchKey.isNotBlank())
        require(category.isNotBlank())
        require(familyKey.isNotBlank())
        require(axis.isNotBlank())
        require(gapKey.isNotBlank())
        require(affectedCandidateCount >= 0)
        require(implementationKey.isNotBlank())
        require(
            observedValues ==
                    observedValues
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )
    }

    val identityKey: String
        get() =
            gapKey
}