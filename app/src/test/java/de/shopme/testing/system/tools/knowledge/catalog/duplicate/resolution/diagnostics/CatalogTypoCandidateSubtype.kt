package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics

enum class CatalogTypoCandidateSubtype {

    /**
     * Genau ein Token unterscheidet sich durch eine Einfügung, Löschung,
     * Ersetzung oder Transposition mit Distanz eins.
     */
    SINGLE_EDIT_TYPO,

    /**
     * Unterschiede sind ausschließlich auf ä/ae, ö/oe, ü/ue oder ß/ss
     * zurückzuführen.
     */
    GERMAN_ORTHOGRAPHY_VARIANT,

    /**
     * Zusammenschreibung und Getrenntschreibung:
     *
     * Basmati Reis ↔ Basmatireis
     */
    COMPOUND_SPACING_VARIANT,

    /**
     * Bindestrich und Leerzeichen oder Zusammenschreibung:
     *
     * Curry-Sauce ↔ Curry Sauce
     */
    HYPHENATION_VARIANT,

    /**
     * Typ und Type.
     */
    TYP_TYPE_VARIANT,

    /**
     * Sauce, Soße und Sosse.
     */
    SAUCE_SOSSE_VARIANT,

    /**
     * Mehr als eine Zeichenoperation innerhalb eines einzelnen Tokens.
     */
    MULTI_EDIT_TYPO,

    /**
     * Mehrere lexikalische Tokens unterscheiden sich.
     */
    MULTIPLE_TOKEN_DIFFERENCE,

    /**
     * Zahlen, Mengen, Prozentangaben oder Produktstufen unterscheiden sich.
     */
    NUMERIC_VARIANT,

    /**
     * Der Fall ist wahrscheinlich kein Typo und wurde im ursprünglichen
     * Duplicate-Plan zu grob klassifiziert.
     */
    PROBABLY_MISCLASSIFIED,

    /**
     * Quelle, Ziel oder Plan-Evidenz reichen für keine genauere Einordnung.
     */
    UNCLASSIFIED
}