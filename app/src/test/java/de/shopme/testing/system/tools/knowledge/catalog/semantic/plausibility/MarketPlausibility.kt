package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility

enum class MarketPlausibilityDecision {
    ACCEPT,
    REJECT,
    REVIEW
}

enum class MarketPlausibilityReason {

    FAMILY_VARIANT_VALUE_INCOMPATIBLE,

    FAMILY_VARIANT_VALUE_UNRESOLVED,

    /**
     * Der Eintrag enthält ausschließlich Knowledge-Attribute
     * und erzeugt keine eigenständige Produktidentität.
     */
    KNOWLEDGE_ONLY_IDENTITY,

    /**
     * Der Eintrag enthält einen generischen semantischen
     * Platzhalter.
     */
    GENERIC_PLACEHOLDER,

    /**
     * Mindestens eine Family×Variant-Beziehung ist explizit
     * inkompatibel.
     */
    FAMILY_VARIANT_INCOMPATIBLE,

    /**
     * Eine Multi-Variant-Kombination ist semantisch unzulässig.
     */
    COMBINATION_INCOMPATIBLE,

    /**
     * Ein Variant beschreibt lediglich einen inhärenten
     * Prozess des Produkts und erzeugt keine Marktidentität.
     */
    INHERENT_PROCESSING_STATE,

    /**
     * Ein konkreter Variant-Key wurde für diese Product Family
     * explizit als marktunplausibel klassifiziert.
     */
    EXPLICIT_FAMILY_VARIANT_REJECT,

    /**
     * Der Eintrag entspricht einer explizit bekannten,
     * plausiblen Family×Variant-Marktidentität.
     */
    EXPLICIT_FAMILY_VARIANT_ACCEPT,

    /**
     * Die Family besitzt ein Semantic Profile und sämtliche
     * Identity-Varianten sind kompatibel.
     */
    PROFILE_SUPPORTED_IDENTITY,

    /**
     * Einzelne semantische Aspekte sind noch nicht eindeutig
     * genug für eine automatische Marktentscheidung.
     */
    UNRESOLVED_MARKET_PLAUSIBILITY
}

data class MarketPlausibilityResult(
    val itemName: String,
    val normalizedItem: String,
    val category: String,
    val family: String,

    val decision: MarketPlausibilityDecision,
    val reason: MarketPlausibilityReason,

    val identityVariantKeys: List<String>,
    val knowledgeOnlyVariantKeys: List<String>,

    val explanation: String
)