package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.value

import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilySemanticProfile
import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilySemanticProfileRegistry
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantDefinition
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry

object FamilyVariantValuePlausibilityRegistry {

    /*
     * Harte familienbezogene Positive.
     *
     * Diese Liste bleibt bewusst klein:
     * Sie beschreibt reale kanonische Identitäten, keine
     * beliebigen technisch möglichen Kombinationen.
     */
    private val explicitlyAllowedByFamily =
        mapOf(

            familyKey("Hähnchenfleisch") to
                    setOf(
                        "fillet",
                        "schnitzel-cut"
                    ),

            familyKey("Kalbfleisch") to
                    setOf(
                        "fillet",
                        "chop",
                        "schnitzel-cut"
                    ),

            familyKey("Fruchtsaft") to
                    setOf(
                        "apple"
                    ),

            familyKey("Brot") to
                    setOf(
                        "barley",
                        "buckwheat"
                    ),

            familyKey("Hartkäse") to
                    setOf(
                        "aged",
                        "extra-aged"
                    )
        )

    /*
     * Harte familienbezogene Negative.
     *
     * Hier landen bekannte konkrete Fehlkombinationen.
     */
    private val explicitlyRejectedByFamily =
        mapOf(

            familyKey("Frischfisch") to
                    setOf(
                        "flakes",
                        "baked",
                        "deep-fried"
                    ),

            familyKey("Fischfilets") to
                    setOf(
                        "flakes"
                    ),

            familyKey("Krustentiere") to
                    setOf(
                        "flakes"
                    ),

            familyKey("Wildfleisch") to
                    setOf(
                        "baked",
                        "deep-fried"
                    ),

            familyKey("Hähnchenfleisch") to
                    setOf(
                        "baked",
                        "deep-fried"
                    ),

            familyKey("Fleischalternativen") to
                    setOf(
                        "apple",
                        "bean"
                    )
        )

    /*
     * Profilweite Value-Rejects.
     *
     * Diese Regeln greifen nur dort, wo der Wert semantisch
     * unabhängig von der konkreten Family offensichtlich nicht
     * zu einer marktüblichen Produktidentität passt.
     */
    private val rejectedByProfile =
        mapOf(

            ProductFamilySemanticProfile.FRESH_FISH to
                    setOf(
                        "flakes"
                    ),

            ProductFamilySemanticProfile.CRUSTACEAN to
                    setOf(
                        "flakes"
                    ),

            ProductFamilySemanticProfile.RAW_MEAT to
                    setOf(
                        "baked",
                        "deep-fried"
                    )
        )

    /*
     * Profilweite Positive bleiben ebenfalls konservativ.
     */
    private val allowedByProfile =
        mapOf(

            ProductFamilySemanticProfile.RAW_MEAT to
                    setOf(
                        "fillet",
                        "chop",
                        "schnitzel-cut"
                    ),

            ProductFamilySemanticProfile.FRESH_FISH to
                    setOf(
                        "fillet",
                        "fresh",
                        "frozen"
                    ),

            ProductFamilySemanticProfile.CRUSTACEAN to
                    setOf(
                        "fresh",
                        "frozen"
                    )
        )

    fun evaluate(
        family: String,
        category: String,
        variant: SemanticVariantDefinition
    ): FamilyVariantValuePlausibilityResult {

        val normalizedFamily =
            familyKey(
                family
            )

        val variantKey =
            variant.canonicalKey

        if (
            variantKey in (
                    explicitlyRejectedByFamily[
                        normalizedFamily
                    ]
                        ?: emptySet()
                    )
        ) {
            return result(
                family = family,
                familyKey = normalizedFamily,
                category = category,
                variant = variant,
                decision =
                    FamilyVariantValuePlausibilityDecision.REJECT,
                reason =
                    FamilyVariantValuePlausibilityReason
                        .EXPLICIT_FAMILY_VALUE_REJECT
            )
        }

        if (
            variantKey in (
                    explicitlyAllowedByFamily[
                        normalizedFamily
                    ]
                        ?: emptySet()
                    )
        ) {
            return result(
                family = family,
                familyKey = normalizedFamily,
                category = category,
                variant = variant,
                decision =
                    FamilyVariantValuePlausibilityDecision.ALLOW,
                reason =
                    FamilyVariantValuePlausibilityReason
                        .EXPLICIT_FAMILY_VALUE_ALLOW
            )
        }

        val profile =
            ProductFamilySemanticProfileRegistry
                .profileFor(
                    family
                )

        if (
            variantKey in (
                    rejectedByProfile[
                        profile
                    ]
                        ?: emptySet()
                    )
        ) {
            return result(
                family = family,
                familyKey = normalizedFamily,
                category = category,
                variant = variant,
                decision =
                    FamilyVariantValuePlausibilityDecision.REJECT,
                reason =
                    FamilyVariantValuePlausibilityReason
                        .PROFILE_VALUE_REJECT
            )
        }

        if (
            variantKey in (
                    allowedByProfile[
                        profile
                    ]
                        ?: emptySet()
                    )
        ) {
            return result(
                family = family,
                familyKey = normalizedFamily,
                category = category,
                variant = variant,
                decision =
                    FamilyVariantValuePlausibilityDecision.ALLOW,
                reason =
                    FamilyVariantValuePlausibilityReason
                        .PROFILE_VALUE_ALLOW
            )
        }

        return result(
            family = family,
            familyKey = normalizedFamily,
            category = category,
            variant = variant,
            decision =
                FamilyVariantValuePlausibilityDecision.REVIEW,
            reason =
                FamilyVariantValuePlausibilityReason
                    .NO_EXPLICIT_VALUE_POLICY
        )
    }

    private fun result(
        family: String,
        familyKey: String,
        category: String,
        variant: SemanticVariantDefinition,
        decision: FamilyVariantValuePlausibilityDecision,
        reason: FamilyVariantValuePlausibilityReason
    ) =
        FamilyVariantValuePlausibilityResult(
            family = family,
            familyKey = familyKey,
            category = category,
            variantDisplayName =
                variant.displayName,
            variantCanonicalKey =
                variant.canonicalKey,
            decision = decision,
            reason = reason
        )

    private fun familyKey(
        value: String
    ): String =
        SemanticVariantTaxonomyRegistry
            .normalizeLookupKey(
                value
            )
}