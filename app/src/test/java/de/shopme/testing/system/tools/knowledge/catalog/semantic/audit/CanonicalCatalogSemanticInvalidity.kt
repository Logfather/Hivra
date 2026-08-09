package de.shopme.testing.system.tools.knowledge.catalog.semantic.audit

enum class CanonicalCatalogSemanticInvalidityDomain {
    PRODUCT_IDENTITY,
    METADATA
}

enum class CanonicalCatalogSemanticInvaliditySeverity {
    ERROR,
    REVIEW
}

enum class CanonicalCatalogSemanticInvalidityReason {

    /**
     * Technische/ontologische Platzhalter wurden als konkrete
     * Lebensmittelvariante materialisiert.
     *
     * Beispiele:
     * - Fischbasiert
     * - Käsebasiert
     * - Fruchtbasiert
     * - Kultiviertes Lebensmittel
     * - Überzogenes Lebensmittel
     */
    GENERIC_SEMANTIC_PLACEHOLDER,

    /**
     * Ein Knowledge-Attribut oder Claim wurde zur eigenständigen
     * Catalog-Identität gemacht.
     */
    KNOWLEDGE_ATTRIBUTE_AS_PRODUCT,

    /**
     * Eine Variante ist für die Category bzw. Product Family
     * offensichtlich fachfremd.
     */
    CROSS_DOMAIN_VARIANT,

    /**
     * Die Variante beschreibt lediglich einen Vorgang, der für
     * die Produktfamilie inhärent oder nicht identitätsbildend ist.
     *
     * Beispiel:
     * Brot – Gebacken
     */
    NON_IDENTITY_PROCESSING_VARIANT,

    /**
     * Mehrere generische Varianten wurden kombinatorisch
     * materialisiert.
     */
    SYNTHETIC_VARIANT_COMBINATION,

    /**
     * Die Variant-Bezeichnung wurde fälschlich als colloquial
     * Synonym übernommen.
     */
    VARIANT_LEAKED_INTO_COLLOQUIAL,

    /**
     * Auffälliger Eintrag, der fachliche Prüfung benötigt,
     * aber durch diesen Audit noch nicht sicher verworfen wird.
     */
    MARKET_PLAUSIBILITY_REVIEW
}

data class CanonicalCatalogSemanticInvalidity(
    val itemName: String,
    val normalized: String,
    val category: String,
    val domain: CanonicalCatalogSemanticInvalidityDomain,
    val severity: CanonicalCatalogSemanticInvaliditySeverity,
    val reason: CanonicalCatalogSemanticInvalidityReason,
    val family: String,
    val variants: List<String>,
    val explanation: String,

)

data class CanonicalCatalogSemanticInvalidityReport(
    val schemaVersion: Int,
    val inputFile: String,
    val catalogEntryCount: Int,

    val semanticInvalidEntryCount: Int,
    val semanticReviewEntryCount: Int,
    val metadataInvalidEntryCount: Int,
    val metadataReviewEntryCount: Int,

    val semanticIssueCount: Int,
    val metadataIssueCount: Int,
    val issueCount: Int,

    val countsByReason: Map<String, Int>,
    val countsByCategory: Map<String, Int>,
    val semanticCountsByCategory: Map<String, Int>,

    val issues: List<CanonicalCatalogSemanticInvalidity>
)