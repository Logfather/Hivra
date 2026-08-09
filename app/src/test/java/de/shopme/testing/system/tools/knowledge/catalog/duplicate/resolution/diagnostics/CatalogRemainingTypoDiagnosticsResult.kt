package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics

data class CatalogRemainingTypoDiagnosticsResult(
    val version: Int,

    val remainingTypoCandidateCount: Int,
    val diagnosedEntryCount: Int,

    val existingResolverShouldHaveAcceptedCount: Int,

    val countsBySubtype:
    Map<CatalogTypoCandidateSubtype, Int>,

    val countsByRecommendation:
    Map<CatalogTypoCandidateRecommendation, Int>,

    val countsByDiagnosticReason:
    Map<CatalogTypoCandidateDiagnosticReason, Int>,

    val countsByCategory:
    Map<String, Int>,

    val entries:
    List<CatalogTypoCandidateDiagnosticEntry>,

    val valid: Boolean
) {

    init {
        require(version > 0)

        require(remainingTypoCandidateCount >= 0)
        require(diagnosedEntryCount >= 0)

        require(
            remainingTypoCandidateCount ==
                    diagnosedEntryCount
        ) {
            "Every remaining typo candidate must be diagnosed."
        }

        require(
            diagnosedEntryCount ==
                    entries.size
        )

        require(
            existingResolverShouldHaveAcceptedCount ==
                    entries.count {
                        it.existingResolverWouldAccept
                    }
        )

        require(
            countsBySubtype.values.sum() ==
                    diagnosedEntryCount
        )

        require(
            countsByRecommendation.values.sum() ==
                    diagnosedEntryCount
        )

        /*
         * Ein Eintrag kann mehrere Diagnosegründe besitzen. Deshalb muss
         * countsByDiagnosticReason nicht diagnosedEntryCount entsprechen.
         */
        require(
            countsByDiagnosticReason.values.sum() >=
                    diagnosedEntryCount
        )

        require(
            countsByCategory.values.sum() ==
                    diagnosedEntryCount
        )

        require(
            entries ==
                    entries.sortedBy { it.sourceIndex }
        )

        require(
            entries.map { it.sourceIndex }
                .distinct()
                .size ==
                    entries.size
        )

        require(valid)
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}