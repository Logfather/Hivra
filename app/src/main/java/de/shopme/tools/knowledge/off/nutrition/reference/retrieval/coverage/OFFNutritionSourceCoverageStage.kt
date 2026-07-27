package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage

enum class OFFNutritionSourceCoverageStage {

    /**
     * Im OFF-Slim-Dump wurde kein Produkt mit passender Identität gefunden.
     */
    RAW_OFF_PRODUCT,

    /**
     * Passende OFF-Produkte existieren, aber keines besitzt verwertbare
     * Nutrition-Daten.
     */
    RAW_OFF_USABLE_NUTRITION,

    /**
     * Verwertbare OFF-Produkte existieren, erscheinen aber nicht im
     * kanonischen Nutrition-Referenzdatensatz.
     */
    REFERENCE_CANDIDATE,

    /**
     * Ein Referenzkandidat existiert, wurde aber nicht in ein validiertes
     * Referenzaggregat übernommen.
     */
    REFERENCE_AGGREGATE,

    /**
     * Ein validiertes Aggregat existiert, wurde aber nicht als
     * Matcher-Kandidat exportiert.
     */
    MATCHER_CANDIDATE,

    /**
     * Ein Matcher-Kandidat mit passendem Alias existiert, wurde jedoch nicht
     * vom Catalog-Retrieval gefunden.
     */
    RETRIEVAL_INDEX,

    /**
     * Alle untersuchten Stufen enthalten passende Daten.
     *
     * In diesem Fall ist das Problem innerhalb der Retrieval-Logik oder ihrer
     * Filterung zu suchen.
     */
    NONE
}