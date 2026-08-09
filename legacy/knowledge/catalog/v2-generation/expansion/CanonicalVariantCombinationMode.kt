package de.shopme.testing.system.tools.knowledge.catalog.expansion.combination

enum class CanonicalVariantCombinationMode {

    /**
     * Ein einzelner Achsenwert erzeugt eine eigenständige Variante.
     */
    SINGLE_AXIS,

    /**
     * Eine identitätsbestimmende Achse wird mit genau einer
     * qualifizierenden Achse kombiniert.
     */
    ANCHORED_PAIR,

    /**
     * Eine identitätsbestimmende Achse wird mit zwei fachlich
     * kompatiblen Qualifikatoren kombiniert.
     *
     * Die Materialisierung bleibt explizit begrenzt.
     */
    CURATED_TRIPLE
}