package de.shopme.testing.system.tools.knowledge.catalog.review.reclassification

enum class CatalogMisclassifiedTypoClass {

    /**
     * Tiefgekühlte Form gegenüber einer nicht explizit tiefgekühlten Form.
     *
     * Beispiele:
     * TK-Blattspinat ↔ Blattspinat
     * Lachsfilet TK ↔ Lachsfilet
     */
    FROZEN_FORM_VARIANT,

    /**
     * Konservierte oder in Dosen angebotene Form gegenüber einer
     * unverarbeiteten oder anders abgefüllten Form.
     *
     * Beispiele:
     * Ananas in Dose ↔ Ananas
     * Bohnen in Dose ↔ Bohnen
     */
    CANNED_FORM_VARIANT,

    /**
     * Qualitäts- oder Produktionsattribut Bio.
     *
     * Beispiel:
     * Vegetarische Pizza Bio ↔ Vegetarische Pizza
     */
    BIO_ATTRIBUTE_VARIANT,

    /**
     * Unterschiedliche Verkaufs- oder Darreichungsform.
     *
     * Beispiel:
     * Bohnen in Dose ↔ Bohnen, Dose
     */
    SALES_FORM_VARIANT,

    /**
     * Unterschiedliche Konservierungs- oder Zubereitungsform, die nicht
     * allein über das Token "Dose" beschrieben wird.
     */
    PRESERVATION_FORM_VARIANT,

    /**
     * Identitätsrelevanter Produktunterschied, der nicht automatisch
     * zusammengeführt werden darf.
     */
    SEMANTIC_PRODUCT_VARIANT,

    /**
     * Die vorhandenen Daten reichen für keine belastbare Unterklasse.
     */
    UNCLASSIFIED
}