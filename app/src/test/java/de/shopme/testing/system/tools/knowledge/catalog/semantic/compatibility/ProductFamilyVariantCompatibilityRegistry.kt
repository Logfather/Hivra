package de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantDefinition
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

object ProductFamilyVariantCompatibilityRegistry {

    /*
     * Generische semantische Platzhalter bilden niemals eine
     * belastbare Product-Family-Variante.
     */
    private val globallyRejectedTypes =
        setOf(
            SemanticVariantType.GENERIC_PLACEHOLDER
        )

    /*
     * Category-Regeln sind absichtlich konservativ.
     *
     * Sie dienen nur als positiver Fallback.
     * Nicht aufgeführte Typen werden NICHT automatisch rejected,
     * sondern REVIEW.
     */
    private val categoryAllowedTypes =
        mapOf(
            "bakery" to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STORAGE_STATE,
                        SemanticVariantType.STYLE
                    ),

            "baking-ingredients" to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING
                    ),

            "dairy" to
                    setOf(
                        SemanticVariantType.COMPOSITION,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.STORAGE_STATE
                    )
        )

    private val familyRules =
        listOf(

            /*
             * ---------------------------------------------------------
             * DAIRY
             * ---------------------------------------------------------
             */

            rule(
                family = "Butter",
                allowedTypes =
                    setOf(
                        SemanticVariantType.COMPOSITION
                    ),
                rejectedVariantKeys =
                    setOf(
                        "aged",
                        "extra-aged",
                        "cave-aged",
                        "brine-ripened",
                        "baked",
                        "boiled",
                        "deep-fried",
                        "sliced"
                    )
            ),

            rule(
                family = "Frischkäse",
                allowedTypes =
                    setOf(
                        SemanticVariantType.COMPOSITION,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.INGREDIENT
                    ),
                rejectedVariantKeys =
                    setOf(
                        "aged",
                        "extra-aged",
                        "cave-aged",
                        "brine-ripened",
                        "baked",
                        "boiled",
                        "deep-fried"
                    )
            ),

            rule(
                family = "Hartkäse",
                allowedTypes =
                    setOf(
                        SemanticVariantType.MATURATION,
                        SemanticVariantType.COMPOSITION,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.INGREDIENT
                    ),
                rejectedTypes =
                    setOf(
                        SemanticVariantType.CUT
                    )
            ),

            /*
             * ---------------------------------------------------------
             * BAKERY
             * ---------------------------------------------------------
             */

            rule(
                family = "Brot",
                allowedTypes =
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STORAGE_STATE,
                        SemanticVariantType.STYLE
                    ),
                rejectedVariantKeys =
                    setOf(
                        "baked"
                    )
            ),

            rule(
                family = "Baguette",
                allowedTypes =
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STORAGE_STATE,
                        SemanticVariantType.STYLE
                    ),
                rejectedVariantKeys =
                    setOf(
                        "baked"
                    )
            ),

            rule(
                family = "Brötchen",
                allowedTypes =
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STORAGE_STATE,
                        SemanticVariantType.STYLE
                    ),
                rejectedVariantKeys =
                    setOf(
                        "baked"
                    )
            ),

            rule(
                family = "Toastbrot",
                allowedTypes =
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STORAGE_STATE,
                        SemanticVariantType.STYLE
                    ),
                rejectedVariantKeys =
                    setOf(
                        "baked"
                    )
            ),

            rule(
                family = "Knäckebrot",
                allowedTypes =
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STYLE
                    ),
                rejectedVariantKeys =
                    setOf(
                        "baked"
                    )
            ),

            rule(
                family = "Fladenbrot",
                allowedTypes =
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STYLE
                    ),
                rejectedVariantKeys =
                    setOf(
                        "baked"
                    )
            ),

            rule(
                family = "Kuchen",
                allowedTypes =
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.TEXTURE,
                        SemanticVariantType.STYLE
                    ),
                rejectedVariantKeys =
                    setOf(
                        "baked"
                    )
            )
        )
            .associateBy {
                it.familyKey
            }

    fun evaluate(
        family: String,
        category: String,
        variant: SemanticVariantDefinition
    ): ProductFamilyVariantCompatibilityResult {

        val familyKey =
            normalizeFamily(family)

        /*
         * Globaler semantischer Hard-Reject.
         */
        if (
            variant.type in globallyRejectedTypes
        ) {
            return result(
                family = family,
                familyKey = familyKey,
                category = category,
                variant = variant,
                decision =
                    ProductFamilyVariantCompatibilityDecision.REJECT,
                reason =
                    ProductFamilyVariantCompatibilityReason
                        .GLOBAL_VARIANT_TYPE_REJECT
            )
        }

        val familyRule =
            familyRules[familyKey]

        if (familyRule != null) {

            if (
                variant.canonicalKey in
                familyRule.rejectedVariantKeys
            ) {
                return result(
                    family = family,
                    familyKey = familyKey,
                    category = category,
                    variant = variant,
                    decision =
                        ProductFamilyVariantCompatibilityDecision.REJECT,
                    reason =
                        ProductFamilyVariantCompatibilityReason
                            .EXPLICIT_FAMILY_VARIANT_REJECT
                )
            }

            if (
                variant.type in
                familyRule.rejectedTypes
            ) {
                return result(
                    family = family,
                    familyKey = familyKey,
                    category = category,
                    variant = variant,
                    decision =
                        ProductFamilyVariantCompatibilityDecision.REJECT,
                    reason =
                        ProductFamilyVariantCompatibilityReason
                            .EXPLICIT_FAMILY_TYPE_REJECT
                )
            }

            if (
                variant.canonicalKey in
                familyRule.allowedVariantKeys
            ) {
                return result(
                    family = family,
                    familyKey = familyKey,
                    category = category,
                    variant = variant,
                    decision =
                        ProductFamilyVariantCompatibilityDecision.ALLOW,
                    reason =
                        ProductFamilyVariantCompatibilityReason
                            .EXPLICIT_FAMILY_VARIANT_ALLOW
                )
            }

            if (
                variant.type in
                familyRule.allowedTypes
            ) {
                return result(
                    family = family,
                    familyKey = familyKey,
                    category = category,
                    variant = variant,
                    decision =
                        ProductFamilyVariantCompatibilityDecision.ALLOW,
                    reason =
                        ProductFamilyVariantCompatibilityReason
                            .EXPLICIT_FAMILY_TYPE_ALLOW
                )
            }
        }

        val profile =
            ProductFamilySemanticProfileRegistry
                .profileFor(family)

        if (
            profile !=
            ProductFamilySemanticProfile.GENERIC
        ) {

            if (
                ProductFamilySemanticProfileCompatibility
                    .isAllowed(
                        profile = profile,
                        type = variant.type
                    )
            ) {
                return result(
                    family = family,
                    familyKey = familyKey,
                    category = category,
                    variant = variant,
                    decision =
                        ProductFamilyVariantCompatibilityDecision.ALLOW,
                    reason =
                        ProductFamilyVariantCompatibilityReason
                            .SEMANTIC_PROFILE_TYPE_ALLOW
                )
            }

            return result(
                family = family,
                familyKey = familyKey,
                category = category,
                variant = variant,
                decision =
                    ProductFamilyVariantCompatibilityDecision.REJECT,
                reason =
                    ProductFamilyVariantCompatibilityReason
                        .SEMANTIC_PROFILE_TYPE_REJECT
            )
        }

        val categoryTypes =
            categoryAllowedTypes[category]

        if (
            categoryTypes != null &&
            variant.type in categoryTypes
        ) {
            return result(
                family = family,
                familyKey = familyKey,
                category = category,
                variant = variant,
                decision =
                    ProductFamilyVariantCompatibilityDecision.ALLOW,
                reason =
                    ProductFamilyVariantCompatibilityReason
                        .CATEGORY_TYPE_ALLOW
            )
        }

        return result(
            family = family,
            familyKey = familyKey,
            category = category,
            variant = variant,
            decision =
                ProductFamilyVariantCompatibilityDecision.REVIEW,
            reason =
                ProductFamilyVariantCompatibilityReason
                    .NO_EXPLICIT_COMPATIBILITY_RULE
        )
    }

    fun hasFamilyRule(
        family: String
    ): Boolean =
        normalizeFamily(family) in familyRules

    fun familyRuleCount(): Int =
        familyRules.size

    private fun result(
        family: String,
        familyKey: String,
        category: String,
        variant: SemanticVariantDefinition,
        decision: ProductFamilyVariantCompatibilityDecision,
        reason: ProductFamilyVariantCompatibilityReason
    ) =
        ProductFamilyVariantCompatibilityResult(
            family = family,
            normalizedFamily = familyKey,
            category = category,
            variantRawValue =
                variant.displayName,
            variantCanonicalKey =
                variant.canonicalKey,
            variantType =
                variant.type,
            decision =
                decision,
            reason =
                reason
        )

    private fun rule(
        family: String,
        allowedTypes: Set<SemanticVariantType> =
            emptySet(),
        rejectedTypes: Set<SemanticVariantType> =
            emptySet(),
        allowedVariantKeys: Set<String> =
            emptySet(),
        rejectedVariantKeys: Set<String> =
            emptySet()
    ) =
        ProductFamilyVariantCompatibilityRule(
            familyKey =
                normalizeFamily(family),
            allowedTypes =
                allowedTypes,
            rejectedTypes =
                rejectedTypes,
            allowedVariantKeys =
                allowedVariantKeys,
            rejectedVariantKeys =
                rejectedVariantKeys
        )

    fun normalizeFamily(
        value: String
    ): String =
        SemanticVariantTaxonomyRegistry
            .normalizeLookupKey(value)
}