package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.value

enum class FamilyVariantValuePlausibilityDecision {
    ALLOW,
    REJECT,
    REVIEW
}

enum class FamilyVariantValuePlausibilityReason {

    /**
     * Konkreter Variant-Wert ist für diese Family explizit plausibel.
     */
    EXPLICIT_FAMILY_VALUE_ALLOW,

    /**
     * Konkreter Variant-Wert ist für diese Family explizit unplausibel.
     */
    EXPLICIT_FAMILY_VALUE_REJECT,

    /**
     * Konkreter Variant-Wert ist für ein semantisches Family-Profil
     * generell plausibel.
     */
    PROFILE_VALUE_ALLOW,

    /**
     * Konkreter Variant-Wert ist für ein semantisches Family-Profil
     * generell unplausibel.
     */
    PROFILE_VALUE_REJECT,

    /**
     * Keine belastbare Value-Policy vorhanden.
     */
    NO_EXPLICIT_VALUE_POLICY
}

data class FamilyVariantValuePlausibilityResult(
    val family: String,
    val familyKey: String,
    val category: String,

    val variantDisplayName: String,
    val variantCanonicalKey: String,

    val decision: FamilyVariantValuePlausibilityDecision,
    val reason: FamilyVariantValuePlausibilityReason
)