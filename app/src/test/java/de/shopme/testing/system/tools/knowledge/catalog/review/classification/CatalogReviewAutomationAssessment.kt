package de.shopme.testing.system.tools.knowledge.catalog.review.classification

enum class CatalogReviewAutomationAssessment {

    /**
     * Der vorhandene Backlog-Eintrag enthält noch nicht genug Evidenz für
     * eine automatische Änderung.
     */
    MANUAL_REVIEW_REQUIRED,

    /**
     * Der Fall besitzt ein eindeutiges Ziel und genau einen grundsätzlich
     * deterministisch prüfbaren Variantengrund.
     *
     * Dieser Status führt noch keine Änderung aus. Er markiert lediglich
     * einen Kandidaten für einen späteren, spezialisierten Resolver.
     */
    POTENTIALLY_DETERMINISTIC,

    /**
     * Der Eintrag enthält mehrere oder widersprüchliche fachliche Gründe.
     */
    CONFLICTING_EVIDENCE,

    /**
     * Der Eintrag muss in mehrere kanonische Identitäten zerlegt werden.
     */
    SPLIT_REQUIRED
}