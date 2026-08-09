package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit

enum class CanonicalIdentityDuplicateSeverity {
    ERROR,
    REVIEW
}

enum class CanonicalIdentityDuplicateReason {

    /**
     * Mehrere Einträge besitzen denselben normalized key.
     *
     * Das ist ein harter kanonischer Identitätskonflikt.
     */
    IDENTICAL_NORMALIZED_KEY,

    /**
     * Mehrere Einträge besitzen nach sprachneutraler
     * Namensnormalisierung denselben itemname.
     */
    IDENTICAL_NORMALIZED_NAME,

    /**
     * Die Namen enthalten dieselben normalisierten Tokens,
     * jedoch in unterschiedlicher Reihenfolge.
     *
     * Beispiel:
     * Butter Croissant
     * Croissant Butter
     */
    WORD_ORDER_VARIANT,

    /**
     * Einträge unterscheiden sich ausschließlich durch
     * Interpunktion bzw. Separatoren.
     */
    PUNCTUATION_VARIANT
}

enum class CanonicalIdentitySourceLayer {
    BASELINE,
    REGENERATED_EXPANSION
}

data class CanonicalIdentityAuditItem(
    val itemName: String,
    val normalized: String,
    val category: String,
    val sourceLayer: CanonicalIdentitySourceLayer
)

data class CanonicalIdentityDuplicateGroup(
    val severity: CanonicalIdentityDuplicateSeverity,
    val reason: CanonicalIdentityDuplicateReason,
    val fingerprint: String,
    val entryCount: Int,
    val categories: List<String>,
    val entries: List<CanonicalIdentityAuditItem>
)

data class CanonicalIdentityDuplicateAuditReport(
    val schemaVersion: Int,

    val baselineEntryCount: Int,
    val regeneratedExpansionEntryCount: Int,
    val projectedCatalogEntryCount: Int,

    val duplicateGroupCount: Int,
    val errorGroupCount: Int,
    val reviewGroupCount: Int,

    val affectedEntryCount: Int,
    val errorAffectedEntryCount: Int,
    val reviewAffectedEntryCount: Int,

    val countsByReason: Map<String, Int>,

    val groups: List<CanonicalIdentityDuplicateGroup>
)