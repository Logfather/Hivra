package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage

data class OFFNutritionSourceCoverageReport(
    val version: Int,
    val requestCount: Int,
    val missingRequestCount: Int,
    val rawOFFScannedProductCount: Long,
    val countsByFirstMissingStage:
    Map<OFFNutritionSourceCoverageStage, Int>,
    val findings: List<OFFNutritionSourceCoverageFinding>
) {

    init {
        require(version == CURRENT_VERSION)
        require(requestCount >= 0)
        require(missingRequestCount >= 0)
        require(rawOFFScannedProductCount >= 0)

        require(missingRequestCount == findings.size)

        require(
            countsByFirstMissingStage.values.sum() ==
                    missingRequestCount
        )

        require(
            findings ==
                    findings.sortedWith(
                        compareBy<OFFNutritionSourceCoverageFinding>(
                            { it.catalogIndex },
                            { it.catalogKey }
                        )
                    )
        ) {
            "Coverage findings must be deterministically sorted."
        }
    }

    companion object {

        const val CURRENT_VERSION =
            1
    }
}