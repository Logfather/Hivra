package de.shopme.testing.system.tools.knowledge.catalog.expansion.variant

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis

data class CanonicalVariantAxisCoverageRequirement(
    val axis:
    CanonicalProductFamilyVariantAxis,

    val criticality:
    CanonicalVariantAxisCriticality,

    val selectionMode:
    CanonicalVariantAxisSelectionMode,

    /**
     * Mindestanzahl fachlich unterschiedlicher Ausprägungen, die bei der
     * späteren Kandidatenerzeugung für diese Achse geprüft werden müssen.
     *
     * Dies ist keine SKU-Anzahl und noch keine finale Artikelanzahl.
     */
    val minimumRelevantValueCount: Int,

    /**
     * Empfohlene Zahl fachlich relevanter Ausprägungen.
     *
     * Der Wert beschreibt den Analyseumfang. Er darf später durch eine
     * konkretere Produktfamilien-Taxonomie ersetzt werden.
     */
    val recommendedRelevantValueCount: Int,

    /**
     * Schutzgrenze gegen eine unkontrollierte Variantenexplosion.
     */
    val maximumRelevantValueCount: Int,

    val allowCrossAxisCombination: Boolean,

    val rationale: String
) {

    init {
        require(minimumRelevantValueCount > 0)

        require(
            recommendedRelevantValueCount >=
                    minimumRelevantValueCount
        )

        require(
            maximumRelevantValueCount >=
                    recommendedRelevantValueCount
        )

        require(rationale.isNotBlank())
        require(rationale == rationale.trim())

        if (
            selectionMode ==
            CanonicalVariantAxisSelectionMode.SINGLE_VALUE_ALLOWED
        ) {
            require(minimumRelevantValueCount == 1)
        }

        if (
            criticality ==
            CanonicalVariantAxisCriticality.REQUIRED
        ) {
            require(
                selectionMode !=
                        CanonicalVariantAxisSelectionMode
                            .SINGLE_VALUE_ALLOWED ||
                        recommendedRelevantValueCount == 1
            )
        }
    }
}