package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

data class CanonicalVariantValue(
    val key: String,
    val displayName: String,

    /**
     * Alternative deutsche Such- und Schreibformen.
     *
     * Keine Marken, Händler, EANs oder Packungsgrößen.
     */
    val aliases: List<String>,

    /**
     * true, wenn der Wert typischerweise eine eigenständige kanonische
     * Lebensmittelidentität begründen kann.
     */
    val identityRelevant: Boolean,

    /**
     * true, wenn der Wert Nutrition, Allergene, Verarbeitung oder andere
     * Knowledge-Dimensionen materiell verändern kann.
     */
    val knowledgeRelevant: Boolean
) {

    init {
        require(CANONICAL_KEY_REGEX.matches(key)) {
            "Invalid canonical variant value key: '$key'."
        }

        require(displayName.isNotBlank())
        require(displayName == displayName.trim())

        require(
            aliases ==
                    aliases
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        ) {
            "Aliases must be normalized, unique and sorted for '$key'."
        }

        require(identityRelevant || knowledgeRelevant) {
            "Variant value '$key' must be identity- or knowledge-relevant."
        }
    }

    private companion object {
        val CANONICAL_KEY_REGEX =
            Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
    }
}