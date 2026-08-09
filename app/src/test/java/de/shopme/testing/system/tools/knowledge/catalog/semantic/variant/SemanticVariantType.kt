package de.shopme.testing.system.tools.knowledge.catalog.semantic.variant

enum class SemanticVariantType {

    /**
     * Eine konkrete Zutat oder Rohstoffbasis.
     *
     * Beispiele:
     * Gerste
     * Amaranth
     * Mandel
     * Rind
     */
    INGREDIENT,

    /**
     * Industrieller oder handwerklicher Verarbeitungsprozess.
     *
     * Beispiele:
     * fermentiert
     * geräuchert
     * getrocknet
     */
    PROCESSING,

    /**
     * Kulinarische Zubereitungsart.
     *
     * Beispiele:
     * gebacken
     * gekocht
     * frittiert
     * gebraten
     */
    PREPARATION,

    /**
     * Reifung bzw. Reifegrad.
     *
     * Beispiele:
     * gereift
     * lang gereift
     * höhlengereift
     */
    MATURATION,

    /**
     * Physische Darreichungs- oder Schnittform.
     *
     * Beispiele:
     * geschnitten
     * gewürfelt
     * geraspelt
     * Flocken
     * Granulat
     */
    FORM,

    /**
     * Fleisch- bzw. Zerlegeform eines tierischen Lebensmittels.
     *
     * Beispiele:
     * Filet
     * Kotelett
     * Schnitzel
     */
    CUT,

    /**
     * Textur oder Konsistenz.
     *
     * Beispiele:
     * cremig
     * knusprig
     */
    TEXTURE,

    /**
     * Sensorisches Geschmacks- oder Aromaprofil.
     *
     * Beispiele:
     * zitrusartig
     * nussig
     * würzig
     */
    FLAVOR,

    /**
     * Sachliche Zusammensetzung bzw. Produktbeschaffenheit,
     * ohne bereits zu entscheiden, ob sie identitätsbildend ist.
     *
     * Beispiele:
     * vollfett
     * fettreduziert
     */
    COMPOSITION,

    /**
     * Nährwertbezogene Aussage.
     *
     * Beispiele:
     * proteinreich
     * zuckerfrei
     * fettarm
     */
    NUTRITION_CLAIM,

    /**
     * Allergenbezogene Aussage.
     *
     * Beispiele:
     * glutenfrei
     * laktosefrei
     * selleriefrei
     */
    ALLERGEN_CLAIM,

    /**
     * Ernährungsbezogene Aussage.
     *
     * Beispiele:
     * vegan
     * vegetarisch
     */
    DIET_CLAIM,

    /**
     * Produktions- oder Anbaumethode.
     *
     * Beispiele:
     * Bio
     * konventionell
     */
    PRODUCTION_METHOD,

    /**
     * Lager- bzw. Vertriebszustand.
     *
     * Beispiele:
     * tiefgekühlt
     * frisch
     */
    STORAGE_STATE,

    /**
     * Unspezifischer Stil- oder Sortimentsdescriptor.
     *
     * Beispiele:
     * klassisch
     * traditionell
     */
    STYLE,

    /**
     * Abstrakter semantischer Platzhalter, der keine konkrete
     * Lebensmittelvariante bezeichnet.
     *
     * Beispiele:
     * fischbasiert
     * käsebasiert
     * kultiviertes Lebensmittel
     */
    GENERIC_PLACEHOLDER
}