package de.shopme.testing.system.tools.knowledge.catalog.review

enum class CatalogReviewBacklogStatus {

    /**
     * Der ursprüngliche Review-Grund wurde durch die sichere technische
     * Normalisierung vollständig beseitigt.
     */
    RESOLVED_BY_NORMALIZATION,

    /**
     * Die offene Kategorieentscheidung wurde durch die deterministische
     * Migration auf einen registrierten Taxonomie-Key beseitigt.
     */
    RESOLVED_BY_CATEGORY_MIGRATION,

    /**
     * Der Eintrag wurde deterministisch in ein kanonisches Ziel
     * zusammengeführt.
     */
    RESOLVED_BY_DUPLICATE_MERGE,

    /**
     * Der Eintrag wurde nach einer sicheren Non-Food-Entscheidung entfernt.
     */
    RESOLVED_BY_REMOVAL,

    /**
     * Der Eintrag benötigt weiterhin eine fachliche Entscheidung.
     */
    STILL_REVIEW_REQUIRED,

    /**
     * Der Eintrag beschreibt weiterhin mehr als eine semantische Identität
     * und muss aufgeteilt werden.
     */
    STILL_SPLIT_REQUIRED
}