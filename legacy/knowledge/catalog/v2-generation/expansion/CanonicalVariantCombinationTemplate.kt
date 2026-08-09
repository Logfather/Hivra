package de.shopme.testing.system.tools.knowledge.catalog.expansion.combination

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis

data class CanonicalVariantCombinationTemplate(
    val key: String,

    val mode:
    CanonicalVariantCombinationMode,

    val axes:
    List<CanonicalProductFamilyVariantAxis>,

    /**
     * Maximale Zahl später materialisierter Kombinationen.
     *
     * Dadurch wird ausdrücklich verhindert, dass alle verfügbaren
     * Achsenwerte kartesisch multipliziert werden.
     */
    val maximumMaterializedCombinationCount: Int,

    val required: Boolean,

    val rationale: String
) {

    init {
        require(CANONICAL_KEY_REGEX.matches(key)) {
            "Invalid combination-template key: '$key'."
        }

        require(axes.isNotEmpty())

        require(
            axes ==
                    axes
                        .distinct()
                        .sortedBy { it.name }
        ) {
            "Template axes must be unique and sorted for '$key'."
        }

        require(
            axes.size ==
                    when (mode) {
                        CanonicalVariantCombinationMode.SINGLE_AXIS ->
                            1

                        CanonicalVariantCombinationMode.ANCHORED_PAIR ->
                            2

                        CanonicalVariantCombinationMode.CURATED_TRIPLE ->
                            3
                    }
        ) {
            "Axis count is inconsistent with mode '$mode' for '$key'."
        }

        require(maximumMaterializedCombinationCount > 0)

        require(rationale.isNotBlank())
        require(rationale == rationale.trim())
    }

    private companion object {
        val CANONICAL_KEY_REGEX =
            Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
    }
}