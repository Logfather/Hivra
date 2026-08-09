package de.shopme.testing.system.tools.knowledge.catalog.expansion.combination

data class CanonicalProductFamilyCombinationCoverage(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val baselineEntryCount: Int,

    val derivedTargetEntryCount: Int,
    val requiredExpansionEntryCount: Int,

    val categoryCount: Int,
    val productFamilyCount: Int,

    val combinationTemplateCount: Int,
    val curatedCombinationCapacity: Int,

    val singleAxisTemplateCount: Int,
    val anchoredPairTemplateCount: Int,
    val curatedTripleTemplateCount: Int,

    val categories:
    List<CanonicalCategoryCombinationCoverage>,

    val familiesWithCompleteCombinationCoverageCount: Int,
    val familiesWithIncompleteCombinationCoverageCount: Int,

    val cartesianExpansionForbidden: Boolean,
    val readyForCandidateGeneration: Boolean,

    val blockers: List<String>,

    val valid: Boolean
) {

    init {
        require(version > 0)
        require(sourceBaselineId.isNotBlank())

        require(
            SHA_256_REGEX.matches(
                sourceBaselineCatalogSha256
            )
        )

        require(baselineEntryCount > 0)
        require(derivedTargetEntryCount > baselineEntryCount)

        require(
            requiredExpansionEntryCount ==
                    derivedTargetEntryCount -
                    baselineEntryCount
        )

        require(categoryCount == categories.size)

        require(
            productFamilyCount ==
                    categories.sumOf {
                        it.productFamilyCount
                    }
        )

        require(
            combinationTemplateCount ==
                    categories.sumOf {
                        it.templateCount
                    }
        )

        require(
            curatedCombinationCapacity ==
                    categories.sumOf {
                        it.curatedCombinationCapacity
                    }
        )

        require(
            combinationTemplateCount ==
                    singleAxisTemplateCount +
                    anchoredPairTemplateCount +
                    curatedTripleTemplateCount
        )

        require(
            categories ==
                    categories.sortedBy {
                        it.category
                    }
        )

        val allFamilies =
            categories.flatMap {
                it.families
            }

        val allTemplates =
            allFamilies.flatMap {
                it.templates
            }

        require(
            singleAxisTemplateCount ==
                    allTemplates.count {
                        it.mode ==
                                CanonicalVariantCombinationMode.SINGLE_AXIS
                    }
        )

        require(
            anchoredPairTemplateCount ==
                    allTemplates.count {
                        it.mode ==
                                CanonicalVariantCombinationMode.ANCHORED_PAIR
                    }
        )

        require(
            curatedTripleTemplateCount ==
                    allTemplates.count {
                        it.mode ==
                                CanonicalVariantCombinationMode.CURATED_TRIPLE
                    }
        )

        require(
            familiesWithCompleteCombinationCoverageCount ==
                    allFamilies.count {
                        it.valid
                    }
        )

        require(
            familiesWithIncompleteCombinationCoverageCount ==
                    allFamilies.count {
                        !it.valid
                    }
        )

        require(
            productFamilyCount ==
                    familiesWithCompleteCombinationCoverageCount +
                    familiesWithIncompleteCombinationCoverageCount
        )

        require(cartesianExpansionForbidden)

        require(
            blockers ==
                    blockers
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        require(
            readyForCandidateGeneration ==
                    (
                            blockers.isEmpty() &&
                                    cartesianExpansionForbidden &&
                                    familiesWithIncompleteCombinationCoverageCount == 0
                            )
        )

        require(valid == readyForCandidateGeneration)
    }

    companion object {
        const val CURRENT_VERSION = 1

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}