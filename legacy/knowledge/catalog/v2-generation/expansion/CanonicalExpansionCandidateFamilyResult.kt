package de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate

data class CanonicalExpansionCandidateFamilyResult(
    val familyKey: String,
    val category: String,
    val familyDisplayName: String,

    val sourceTargetEntryCount: Int,
    val derivedTargetEntryCount: Int,

    /**
     * Deterministisch geschätzter Baselinebestand der Familie.
     *
     * Die bestehende Baseline ist aktuell nur auf Kategorieebene gezählt.
     */
    val estimatedBaselineFamilyEntryCount: Int,

    val requiredExpansionEntryCount: Int,

    val generatedCandidateCount: Int,

    val candidates:
    List<CanonicalCatalogExpansionCandidate>,

    val complete: Boolean
) {

    init {
        require(familyKey.isNotBlank())
        require(category.isNotBlank())
        require(familyDisplayName.isNotBlank())

        require(sourceTargetEntryCount > 0)

        require(
            derivedTargetEntryCount >=
                    sourceTargetEntryCount
        )

        require(estimatedBaselineFamilyEntryCount >= 0)

        require(
            estimatedBaselineFamilyEntryCount <=
                    sourceTargetEntryCount
        )

        require(
            requiredExpansionEntryCount ==
                    derivedTargetEntryCount -
                    estimatedBaselineFamilyEntryCount
        )

        require(generatedCandidateCount == candidates.size)

        require(
            generatedCandidateCount ==
                    requiredExpansionEntryCount
        )

        require(
            candidates ==
                    candidates.sortedBy {
                        it.familyCandidateIndex
                    }
        )

        require(
            candidates.map {
                it.familyCandidateIndex
            } ==
                    (1..candidates.size).toList()
        )

        require(
            candidates.all {
                it.familyKey == familyKey &&
                        it.category == category
            }
        )

        require(
            complete ==
                    (
                            generatedCandidateCount ==
                                    requiredExpansionEntryCount
                            )
        )
    }
}