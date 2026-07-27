package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.missing

data class MissingOFFNutritionRetrievalCandidateFinding(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,
    val itemName: String,
    val category: String?,
    val production: String?,
    val primaryType: MissingOFFNutritionRetrievalCandidateType,
    val relatedSourceAliases: List<String>,
    val reasons: List<String>
) {

    init {
        require(catalogIndex >= 0)
        require(catalogKey.isNotBlank())
        require(normalizedEnglish.isNotBlank())
        require(itemName.isNotBlank())

        require(
            relatedSourceAliases ==
                    relatedSourceAliases
                        .distinct()
                        .sorted()
        )

        require(
            reasons.isNotEmpty()
        )

        require(
            reasons ==
                    reasons
                        .distinct()
                        .sorted()
        )
    }
}