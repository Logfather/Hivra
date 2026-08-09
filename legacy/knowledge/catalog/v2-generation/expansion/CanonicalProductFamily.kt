package de.shopme.testing.system.tools.knowledge.catalog.expansion.family

data class CanonicalProductFamily(
    val key: String,
    val category: String,
    val displayName: String,

    /**
     * Relative Gewichtung innerhalb der Kategorie.
     *
     * Die konkrete Zielanzahl wird deterministisch aus dem Zielbestand
     * der Kategorie und den Gewichten sämtlicher Familien abgeleitet.
     */
    val allocationWeight: Int,

    val allowedVariantAxes:
    List<CanonicalProductFamilyVariantAxis>,

    val rationale: String
) {

    init {
        require(CANONICAL_KEY_REGEX.matches(key)) {
            "Invalid canonical product-family key: '$key'."
        }

        require(CANONICAL_KEY_REGEX.matches(category)) {
            "Invalid canonical category key: '$category'."
        }

        require(displayName.isNotBlank())
        require(displayName == displayName.trim())

        require(allocationWeight > 0)

        require(allowedVariantAxes.isNotEmpty())

        require(
            allowedVariantAxes ==
                    allowedVariantAxes
                        .distinct()
                        .sortedBy { it.name }
        ) {
            "Variant axes must be unique and sorted for '$key'."
        }

        require(rationale.isNotBlank())
        require(rationale == rationale.trim())
    }

    private companion object {
        val CANONICAL_KEY_REGEX =
            Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
    }
}