package de.shopme.testing.system.tools.knowledge.catalog.semantic.combination

import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilySemanticProfile
import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilySemanticProfileRegistry
import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilyVariantCompatibilityDecision
import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilyVariantCompatibilityRegistry
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantDefinition
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

object SemanticCombinationConstraintRegistry {

    /*
     * Mehrere Werte dieser Typen sind innerhalb derselben
     * Catalog-Identität grundsätzlich widersprüchlich.
     */
    private val singleValuedTypes =
        setOf(
            SemanticVariantType.MATURATION,
            SemanticVariantType.STORAGE_STATE,
            SemanticVariantType.STYLE,
            SemanticVariantType.CUT
        )

    /*
     * Nur diese Type-Pairs sind auf Profile-Ebene explizit freigegeben.
     *
     * Fehlt ein Pair, wird NICHT automatisch rejected,
     * sondern REVIEW.
     */
    private val allowedPairsByProfile =
        mapOf(

            ProductFamilySemanticProfile.BAKED_GOOD to
                    pairs(
                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.PROCESSING,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.STORAGE_STATE,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.STYLE,

                        SemanticVariantType.PROCESSING to
                                SemanticVariantType.STORAGE_STATE,

                        SemanticVariantType.PROCESSING to
                                SemanticVariantType.STYLE
                    ),

            ProductFamilySemanticProfile.CHEESE to
                    pairs(
                        SemanticVariantType.MATURATION to
                                SemanticVariantType.COMPOSITION,

                        SemanticVariantType.MATURATION to
                                SemanticVariantType.FLAVOR,

                        SemanticVariantType.MATURATION to
                                SemanticVariantType.INGREDIENT,

                        SemanticVariantType.MATURATION to
                                SemanticVariantType.FORM,

                        SemanticVariantType.COMPOSITION to
                                SemanticVariantType.FLAVOR,

                        SemanticVariantType.COMPOSITION to
                                SemanticVariantType.INGREDIENT,

                        SemanticVariantType.COMPOSITION to
                                SemanticVariantType.FORM,

                        SemanticVariantType.FLAVOR to
                                SemanticVariantType.INGREDIENT
                    ),

            ProductFamilySemanticProfile.RAW_MEAT to
                    pairs(
                        SemanticVariantType.CUT to
                                SemanticVariantType.PREPARATION,

                        SemanticVariantType.CUT to
                                SemanticVariantType.STORAGE_STATE,

                        SemanticVariantType.FORM to
                                SemanticVariantType.PREPARATION,

                        SemanticVariantType.FORM to
                                SemanticVariantType.STORAGE_STATE,

                        SemanticVariantType.PREPARATION to
                                SemanticVariantType.STORAGE_STATE
                    ),

            ProductFamilySemanticProfile.FRESH_FISH to
                    pairs(
                        SemanticVariantType.CUT to
                                SemanticVariantType.PREPARATION,

                        SemanticVariantType.CUT to
                                SemanticVariantType.STORAGE_STATE,

                        SemanticVariantType.FORM to
                                SemanticVariantType.PREPARATION,

                        SemanticVariantType.FORM to
                                SemanticVariantType.STORAGE_STATE,

                        SemanticVariantType.PREPARATION to
                                SemanticVariantType.STORAGE_STATE
                    ),

            ProductFamilySemanticProfile.CEREAL to
                    pairs(
                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.FORM,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.FLAVOR,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.PROCESSING,

                        SemanticVariantType.FORM to
                                SemanticVariantType.FLAVOR
                    ),

            ProductFamilySemanticProfile.PORRIDGE to
                    pairs(
                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.FORM,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.FLAVOR,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.PROCESSING,

                        SemanticVariantType.FORM to
                                SemanticVariantType.FLAVOR
                    ),

            ProductFamilySemanticProfile.READY_MEAL to
                    pairs(
                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.PREPARATION,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.STORAGE_STATE,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.STYLE,

                        SemanticVariantType.PREPARATION to
                                SemanticVariantType.STORAGE_STATE,

                        SemanticVariantType.PREPARATION to
                                SemanticVariantType.STYLE
                    ),

            ProductFamilySemanticProfile.SOUP_STEW to
                    pairs(
                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.PREPARATION,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.TEXTURE,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.STORAGE_STATE,

                        SemanticVariantType.PREPARATION to
                                SemanticVariantType.TEXTURE,

                        SemanticVariantType.TEXTURE to
                                SemanticVariantType.STORAGE_STATE
                    ),

            ProductFamilySemanticProfile.SAUCE to
                    pairs(
                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.FLAVOR,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.TEXTURE,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.PROCESSING,

                        SemanticVariantType.FLAVOR to
                                SemanticVariantType.TEXTURE,

                        SemanticVariantType.FLAVOR to
                                SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.FRUIT_SPREAD to
                    pairs(
                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.FLAVOR,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.TEXTURE,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.PROCESSING,

                        SemanticVariantType.FLAVOR to
                                SemanticVariantType.TEXTURE
                    ),

            ProductFamilySemanticProfile.SNACK to
                    pairs(
                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.FLAVOR,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.PROCESSING,

                        SemanticVariantType.FLAVOR to
                                SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.CHOCOLATE to
                    pairs(
                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.FLAVOR,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.COMPOSITION,

                        SemanticVariantType.FLAVOR to
                                SemanticVariantType.COMPOSITION
                    ),

            ProductFamilySemanticProfile.CONFECTIONERY to
                    pairs(
                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.FLAVOR,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.TEXTURE,

                        SemanticVariantType.INGREDIENT to
                                SemanticVariantType.FORM,

                        SemanticVariantType.FLAVOR to
                                SemanticVariantType.TEXTURE
                    )
        )

    fun evaluate(
        family: String,
        category: String,
        variants: List<SemanticVariantDefinition>
    ): SemanticCombinationConstraintResult {

        require(variants.size >= 2) {
            "Combination constraint evaluation requires at least two variants."
        }

        val normalizedFamily =
            ProductFamilyVariantCompatibilityRegistry
                .normalizeFamily(family)

        val profile =
            ProductFamilySemanticProfileRegistry
                .profileFor(family)

        val members =
            variants.map { variant ->
                SemanticCombinationMember(
                    rawValue = variant.displayName,
                    canonicalKey = variant.canonicalKey,
                    type = variant.type
                )
            }

        /*
         * 1. Doppelte Variant-Keys.
         */
        if (
            variants
                .map { it.canonicalKey }
                .distinct()
                .size != variants.size
        ) {
            return result(
                family = family,
                normalizedFamily = normalizedFamily,
                category = category,
                profile = profile,
                members = members,
                decision = SemanticCombinationDecision.REJECT,
                reason =
                    SemanticCombinationConstraintReason
                        .DUPLICATE_VARIANT_KEY
            )
        }

        /*
         * 2. Generic Placeholder darf nie Bestandteil einer
         *    zusammengesetzten Catalog-Identität sein.
         */
        if (
            variants.any {
                it.type ==
                        SemanticVariantType.GENERIC_PLACEHOLDER
            }
        ) {
            return result(
                family = family,
                normalizedFamily = normalizedFamily,
                category = category,
                profile = profile,
                members = members,
                decision = SemanticCombinationDecision.REJECT,
                reason =
                    SemanticCombinationConstraintReason
                        .GENERIC_PLACEHOLDER_IN_COMBINATION
            )
        }

        /*
         * 3. Prüfen, ob einzelne Family×Variant-Beziehungen
         *    bereits rejected sind.
         */
        val memberCompatibility =
            variants.map { variant ->
                ProductFamilyVariantCompatibilityRegistry
                    .evaluate(
                        family = family,
                        category = category,
                        variant = variant
                    )
            }

        if (
            memberCompatibility.any {
                it.decision ==
                        ProductFamilyVariantCompatibilityDecision.REJECT
            }
        ) {
            return result(
                family = family,
                normalizedFamily = normalizedFamily,
                category = category,
                profile = profile,
                members = members,
                decision = SemanticCombinationDecision.REJECT,
                reason =
                    SemanticCombinationConstraintReason
                        .CONTAINS_INCOMPATIBLE_MEMBER_VARIANT
            )
        }

        /*
         * 4. Semantisch single-valued Types dürfen nicht mehrfach
         *    unterschiedliche Values enthalten.
         */
        val mutuallyExclusive =
            variants
                .groupBy { it.type }
                .any { (type, values) ->
                    type in singleValuedTypes &&
                            values
                                .map { it.canonicalKey }
                                .distinct()
                                .size > 1
                }

        if (mutuallyExclusive) {
            return result(
                family = family,
                normalizedFamily = normalizedFamily,
                category = category,
                profile = profile,
                members = members,
                decision = SemanticCombinationDecision.REJECT,
                reason =
                    SemanticCombinationConstraintReason
                        .MUTUALLY_EXCLUSIVE_TYPE_VALUES
            )
        }

        /*
         * 5. Wenn einzelne Varianten noch REVIEW sind, darf auch
         *    die Kombination nicht automatisch ALLOW werden.
         */
        if (
            memberCompatibility.any {
                it.decision ==
                        ProductFamilyVariantCompatibilityDecision.REVIEW
            }
        ) {
            return result(
                family = family,
                normalizedFamily = normalizedFamily,
                category = category,
                profile = profile,
                members = members,
                decision = SemanticCombinationDecision.REVIEW,
                reason =
                    SemanticCombinationConstraintReason
                        .CONTAINS_UNRESOLVED_MEMBER_VARIANT
            )
        }

        /*
         * 6. Alle Member sind einzeln ALLOW.
         *
         *    Jetzt müssen ALLE Type-Pairs explizit für das
         *    Semantic Profile freigegeben sein.
         */
        val requiredPairs =
            typePairsFor(
                variants = variants
            )

        val allowedPairs =
            allowedPairsByProfile[profile]
                ?: emptySet()

        if (
            requiredPairs.isNotEmpty() &&
            requiredPairs.all {
                it in allowedPairs
            }
        ) {
            return result(
                family = family,
                normalizedFamily = normalizedFamily,
                category = category,
                profile = profile,
                members = members,
                decision = SemanticCombinationDecision.ALLOW,
                reason =
                    SemanticCombinationConstraintReason
                        .PROFILE_COMBINATION_ALLOW
            )
        }

        /*
         * Keine implizite Cartesian-Product-Freigabe.
         */
        return result(
            family = family,
            normalizedFamily = normalizedFamily,
            category = category,
            profile = profile,
            members = members,
            decision = SemanticCombinationDecision.REVIEW,
            reason =
                SemanticCombinationConstraintReason
                    .NO_EXPLICIT_COMBINATION_RULE
        )
    }

    private fun typePairsFor(
        variants: List<SemanticVariantDefinition>
    ): Set<SemanticVariantTypePair> {

        val result =
            linkedSetOf<SemanticVariantTypePair>()

        for (leftIndex in variants.indices) {
            for (
            rightIndex in
            (leftIndex + 1) until variants.size
            ) {

                val left =
                    variants[leftIndex]

                val right =
                    variants[rightIndex]

                /*
                 * Gleicher Typ wird bereits über die Single-Value-
                 * Invarianten behandelt oder bleibt für Multi-Value-
                 * Typen ohne Pair-Constraint.
                 */
                if (left.type == right.type) {
                    continue
                }

                result +=
                    SemanticVariantTypePair.of(
                        first = left.type,
                        second = right.type
                    )
            }
        }

        return result
    }

    private fun pairs(
        vararg pairs:
        Pair<SemanticVariantType, SemanticVariantType>
    ): Set<SemanticVariantTypePair> =
        pairs
            .map { pair ->
                SemanticVariantTypePair.of(
                    first = pair.first,
                    second = pair.second
                )
            }
            .toSet()

    private fun result(
        family: String,
        normalizedFamily: String,
        category: String,
        profile: ProductFamilySemanticProfile,
        members: List<SemanticCombinationMember>,
        decision: SemanticCombinationDecision,
        reason: SemanticCombinationConstraintReason
    ) =
        SemanticCombinationConstraintResult(
            family = family,
            normalizedFamily = normalizedFamily,
            category = category,
            profile = profile,
            members = members,
            decision = decision,
            reason = reason
        )
}