package de.shopme.testing.system.tools.knowledge.catalog.nonfood

enum class NonFoodRecommendation {

    /**
     * Der Eintrag ist mit sehr hoher Sicherheit kein Lebensmittel und kann
     * im nachfolgenden Kanonisierungs-Commit automatisch entfernt werden.
     */
    REMOVE_AUTOMATICALLY,

    /**
     * Der Eintrag ist wahrscheinlich Non-Food, enthält aber mindestens ein
     * Signal, das vor der Entfernung manuell geprüft werden sollte.
     */
    REMOVE_AFTER_REVIEW,

    /**
     * Der Eintrag ist uneindeutig und darf nicht automatisch entfernt werden.
     */
    REVIEW,

    /**
     * Die vorhandenen Signale reichen nicht für eine Non-Food-Einstufung aus.
     * Der Eintrag soll im Lebensmittelkatalog verbleiben.
     */
    KEEP
}