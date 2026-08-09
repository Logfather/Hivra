package de.shopme.testing.system.tools.knowledge.catalog.evaluation

enum class NormalizedCatalogReadinessStatus {

    /**
     * Der erzeugte Katalog verletzt noch mindestens eine strikte
     * Zielinvariante.
     */
    BLOCKED,

    /**
     * Der Katalog ist strukturell valide, enthält aber noch manuell zu
     * entscheidende Canonicalization-Aktionen.
     */
    REVIEW_REQUIRED,

    /**
     * Der Katalog erfüllt alle Zielinvarianten und besitzt keinen offenen
     * Review-Backlog.
     */
    READY
}