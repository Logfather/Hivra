package de.shopme.tools.knowledge.rebuild.nutrition.coverage

enum class NutritionCoverageGapType {

    /**
     * Der Catalog Key besitzt laut Catalog→Server-Match-Report einen
     * exakten Nutrition-Server-Match, ist aber nicht in der erzeugten
     * Runtime-Abdeckung enthalten.
     *
     * Für einen exakten Match ist keine KI-Match-Anfrage erforderlich.
     */
    EXACT_MATCH_NOT_IN_RUNTIME,

    /**
     * Für den fehlenden Catalog Key existiert keine persistierte
     * Match-Anfrage.
     */
    NO_REQUEST,

    /**
     * Es existiert eine Match-Anfrage, aber keine persistierte
     * Entscheidung.
     */
    NO_DECISION,

    /**
     * Eine vorhandene Anfrage enthält keine Kandidaten.
     *
     * Der aktuelle Match-Request-Contract verbietet leere
     * Kandidatenlisten. Die Klassifikation bleibt trotzdem bestehen,
     * damit beschädigte oder ältere persistierte Daten diagnostiziert
     * werden können.
     */
    NO_CANDIDATES,

    /**
     * Im produktiven OFF-Slim-Export existieren Produkte, die über
     * Namen oder Aliase zum Catalog Key gefunden wurden. Keines dieser
     * Produkte enthält jedoch Nutrition-Daten, aus denen der
     * produktive OFF-Extractor einen Nutrition-Kandidaten erzeugen
     * könnte.
     */
    SOURCE_DATA_NO_NUTRITION,

    /**
     * Kein Kandidat besitzt gemeinsame Tokens mit dem Catalog Key.
     */
    NO_SHARED_TOKENS,

    /**
     * Der beste Kandidat besitzt einen sehr niedrigen diagnostischen
     * Retrieval-Score.
     */
    VERY_LOW_SCORE,

    /**
     * Der beste Kandidat besitzt nur eine schwache Token-Überlappung
     * mit dem Catalog Key.
     */
    WEAK_TOKEN_OVERLAP,

    /**
     * Die führenden Kandidaten liegen so nah beieinander, dass das
     * Retrieval keine eindeutige Rangfolge erzeugt.
     */
    SCORE_CLUSTER,

    /**
     * Catalog Key und bester Kandidat enthalten widersprüchliche
     * relevante Modifier.
     */
    MODIFIER_MISMATCH,

    /**
     * Der beste Kandidat ist wesentlich allgemeiner als der Catalog
     * Key.
     */
    TOO_GENERIC,

    /**
     * Der beste Kandidat ist wesentlich spezifischer als der Catalog
     * Key.
     */
    TOO_SPECIFIC,

    /**
     * Die persistierte Entscheidung lautet NO_MATCH, ohne dass eine
     * spezifischere deterministische Retrieval-Ursache erkannt wurde.
     */
    NO_MATCH,

    /**
     * Die persistierte Entscheidung lautet MATCH, aber das Mapping
     * wurde nicht in das zentrale Catalog→Server-Mapping-Artefakt
     * übernommen.
     */
    MATCH_NOT_PERSISTED,

    /**
     * Der fehlende Eintrag konnte keiner bekannten Klasse zugeordnet
     * werden.
     */
    UNKNOWN
}