package de.shopme.testing.system.tools.knowledge.catalog.normalization

data class GermanPluralEvaluation(
    val normalizedPlural: String?,
    val reason: String
) {

    init {
        require(
            normalizedPlural == null ||
                    normalizedPlural.isNotBlank()
        ) {
            "normalizedPlural must not be blank."
        }

        require(reason.isNotBlank()) {
            "reason must not be blank."
        }
    }
}