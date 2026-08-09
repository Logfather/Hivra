package de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate

import de.shopme.testing.system.tools.knowledge.catalog.expansion.combination
.CanonicalVariantCombinationMode

data class CanonicalCatalogExpansionCandidate(
    val candidateIndex: Int,

    val candidateKey: String,

    val category: String,
    val familyKey: String,
    val familyDisplayName: String,

    val familyCandidateIndex: Int,

    val templateKey: String,
    val combinationMode: CanonicalVariantCombinationMode,

    val variantValues:
    List<CanonicalExpansionCandidateVariantValue>,

    /**
     * Technischer, deterministisch erzeugter Arbeitsname.
     *
     * Dieser Name ist noch nicht als finaler deutscher Katalogname
     * freigegeben.
     */
    val proposedCanonicalName: String,

    val proposedNormalizedKey: String,

    val status: CanonicalExpansionCandidateStatus,

    val requiresSemanticValidation: Boolean
) {

    init {
        require(candidateIndex > 0)

        require(CANDIDATE_KEY_REGEX.matches(candidateKey)) {
            "Invalid candidate key: '$candidateKey'."
        }

        require(category.isNotBlank())
        require(familyKey.isNotBlank())
        require(familyDisplayName.isNotBlank())

        require(familyCandidateIndex > 0)

        require(templateKey.isNotBlank())

        require(variantValues.isNotEmpty())

        require(
            variantValues ==
                    variantValues.sortedBy {
                        it.axis.name
                    }
        )

        require(
            variantValues.map { it.axis }
                .distinct()
                .size ==
                    variantValues.size
        )

        require(proposedCanonicalName.isNotBlank())
        require(proposedCanonicalName == proposedCanonicalName.trim())

        require(NORMALIZED_KEY_REGEX.matches(proposedNormalizedKey)) {
            "Invalid proposed normalized key: '$proposedNormalizedKey'."
        }

        require(
            requiresSemanticValidation ==
                    (
                            status ==
                                    CanonicalExpansionCandidateStatus
                                        .REQUIRES_SEMANTIC_VALIDATION
                            )
        )
    }

    private companion object {
        val CANDIDATE_KEY_REGEX =
            Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

        val NORMALIZED_KEY_REGEX =
            Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
    }
}