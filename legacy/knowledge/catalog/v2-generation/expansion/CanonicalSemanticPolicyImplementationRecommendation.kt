package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

enum class CanonicalSemanticPolicyImplementationRecommendation {

    /**
     * Für die Family-Axis-Kombination wurde nur ein einziger konkreter
     * Variantenwert beobachtet.
     *
     * Das ist ein Kandidat für CLOSED_IDENTITY, muss aber fachlich
     * verifiziert werden.
     */
    REVIEW_CLOSED_IDENTITY,

    /**
     * Mehrere konkrete Variantenwerte wurden beobachtet.
     *
     * Die zulässige Menge muss kuratiert werden.
     */
    CURATE_ALLOWED_VALUES
}