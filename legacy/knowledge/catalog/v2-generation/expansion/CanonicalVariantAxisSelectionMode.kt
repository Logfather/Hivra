package de.shopme.testing.system.tools.knowledge.catalog.expansion.variant

enum class CanonicalVariantAxisSelectionMode {

    /**
     * Alle für die Produktfamilie fachlich relevanten Ausprägungen sollen
     * im späteren Katalog berücksichtigt werden.
     */
    EXHAUSTIVE_RELEVANT_VALUES,

    /**
     * Nur markt- und wissensrelevante Ausprägungen werden kuratiert.
     * Nicht jede theoretisch denkbare Ausprägung erzeugt einen Eintrag.
     */
    CURATED_RELEVANT_VALUES,

    /**
     * Die Achse darf für diese Familie auf genau eine Ausprägung begrenzt
     * bleiben, sofern die Familie die Identität bereits vollständig trägt.
     */
    SINGLE_VALUE_ALLOWED
}