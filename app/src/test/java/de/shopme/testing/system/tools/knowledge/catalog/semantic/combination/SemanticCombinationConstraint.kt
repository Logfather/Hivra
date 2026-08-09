package de.shopme.testing.system.tools.knowledge.catalog.semantic.combination

import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilySemanticProfile
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

enum class SemanticCombinationDecision {
    ALLOW,
    REJECT,
    REVIEW
}

enum class SemanticCombinationConstraintReason {

    /**
     * Mindestens eine einzelne Variante ist bereits für die
     * Product Family unzulässig.
     */
    CONTAINS_INCOMPATIBLE_MEMBER_VARIANT,

    /**
     * Mindestens eine einzelne Variante besitzt noch keine
     * abschließende Family-Compatibility-Entscheidung.
     */
    CONTAINS_UNRESOLVED_MEMBER_VARIANT,

    /**
     * Ein generischer semantischer Platzhalter wurde mit weiteren
     * Variant-Values zu einer synthetischen Produktidentität kombiniert.
     */
    GENERIC_PLACEHOLDER_IN_COMBINATION,

    /**
     * Derselbe Variant-Key wurde mehrfach materialisiert.
     */
    DUPLICATE_VARIANT_KEY,

    /**
     * Mehrere Werte eines semantisch single-valued Variantentyps
     * wurden gleichzeitig kombiniert.
     */
    MUTUALLY_EXCLUSIVE_TYPE_VALUES,

    /**
     * Die Kombination der Variantentypen ist für das Family Profile
     * ausdrücklich zulässig.
     */
    PROFILE_TYPE_PAIR_ALLOW,

    /**
     * Alle Variant-Paare des Eintrags sind für das Family Profile
     * explizit zulässig.
     */
    PROFILE_COMBINATION_ALLOW,

    /**
     * Die Einzelvarianten sind zulässig, die Kombination selbst
     * besitzt aber noch keine explizite Freigabe.
     */
    NO_EXPLICIT_COMBINATION_RULE
}

data class SemanticCombinationMember(
    val rawValue: String,
    val canonicalKey: String,
    val type: SemanticVariantType
)

data class SemanticCombinationConstraintResult(
    val family: String,
    val normalizedFamily: String,
    val category: String,
    val profile: ProductFamilySemanticProfile,
    val members: List<SemanticCombinationMember>,
    val decision: SemanticCombinationDecision,
    val reason: SemanticCombinationConstraintReason
)