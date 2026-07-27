package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage

data class OFFNutritionCoverageAliasEntry(
    val identity: String,
    val aliases: Set<String>
) {

    init {
        require(identity.isNotBlank())

        require(
            aliases ==
                    aliases
                        .filter(String::isNotBlank)
                        .toSortedSet()
        )
    }
}