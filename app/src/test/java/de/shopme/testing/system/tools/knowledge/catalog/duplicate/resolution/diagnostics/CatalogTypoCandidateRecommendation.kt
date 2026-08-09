package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics

enum class CatalogTypoCandidateRecommendation {

    /**
     * Sollte bereits vom bestehenden konservativen Typo-Resolver gelöst
     * werden können. Ein solcher Fall weist auf eine Implementierungs- oder
     * Datenflusslücke hin.
     */
    INVESTIGATE_EXISTING_TYPO_RESOLVER,

    /**
     * Kann durch einen spezialisierten Orthografie-Resolver geprüft werden.
     */
    IMPLEMENT_ORTHOGRAPHIC_VARIANT_RESOLVER,

    /**
     * Kann durch einen spezialisierten Compound-Spacing-Resolver geprüft
     * werden.
     */
    IMPLEMENT_COMPOUND_SPACING_RESOLVER,

    /**
     * Kann durch einen spezialisierten Hyphenation-Resolver geprüft werden.
     */
    IMPLEMENT_HYPHENATION_RESOLVER,

    /**
     * Kann durch eine kleine deterministische Typ/Type-Regel geprüft werden.
     */
    IMPLEMENT_TYP_TYPE_RULE,

    /**
     * Kann durch eine fachlich kontrollierte Sauce/Soße-Regel geprüft
     * werden.
     */
    IMPLEMENT_SAUCE_SOSSE_RULE,

    /**
     * Benötigt weiterhin manuelle Prüfung.
     */
    MANUAL_REVIEW_REQUIRED,

    /**
     * Sollte im Duplicate Detector neu klassifiziert werden.
     */
    RECLASSIFY_DUPLICATE_REASON,

    /**
     * Fehlende oder inkonsistente Quelldaten müssen untersucht werden.
     */
    INVESTIGATE_DATA_INCONSISTENCY
}