package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy

enum class CanonicalFamilyAxisSemanticPolicyType {

    /**
     * Nur explizit aufgeführte Werte sind für diese
     * Produktfamilie und Achse zulässig.
     */
    CURATED_ALLOWED_VALUES,

    /**
     * Die Produktfamilie impliziert exakt einen Identitätswert.
     */
    CLOSED_IDENTITY,

    /**
     * Die Variantenachse ist für diese Produktfamilie fachlich
     * grundsätzlich nicht anwendbar.
     *
     * Jeder Kandidat mit dieser Family-Axis-Kombination wird verworfen.
     */
    NOT_APPLICABLE,

    /**
     * Die Policy ist fachlich noch nicht abgeschlossen.
     */
    REVIEW_REQUIRED
}