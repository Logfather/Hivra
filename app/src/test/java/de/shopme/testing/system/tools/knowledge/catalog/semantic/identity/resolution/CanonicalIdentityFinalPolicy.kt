package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution

object CanonicalIdentityFinalPolicy {

    data class PreferredIdentity(
        val winnerNormalized: String,
        val reason: CanonicalIdentityResolutionReason
    )

    private val preferredByFingerprint =
        mapOf(

            /*
             * ---------------------------------------------------------
             * ORTHOGRAPHY
             * ---------------------------------------------------------
             */

            fingerprint(
                reason = "IDENTICAL_NORMALIZED_NAME",
                value = "vollkorncracker"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "vollkorncracker",
                        reason =
                            CanonicalIdentityResolutionReason
                                .STANDARD_ORTHOGRAPHY_PREFERRED
                    ),

            fingerprint(
                reason = "PUNCTUATION_VARIANT",
                value = "weisskohl"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "weisskohl",
                        reason =
                            CanonicalIdentityResolutionReason
                                .STANDARD_ORTHOGRAPHY_PREFERRED
                    ),

            /*
             * ---------------------------------------------------------
             * NATURAL GERMAN WORD ORDER
             * ---------------------------------------------------------
             */

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value = "beverages::gruner|tee"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "gruener-tee",
                        reason =
                            CanonicalIdentityResolutionReason
                                .NATURAL_GERMAN_WORD_ORDER_PREFERRED
                    ),

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value = "dairy::bio|eier"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "bio-eier",
                        reason =
                            CanonicalIdentityResolutionReason
                                .NATURAL_GERMAN_WORD_ORDER_PREFERRED
                    ),

            /*
             * ---------------------------------------------------------
             * PRODUCT BEFORE STORAGE STATE
             * ---------------------------------------------------------
             */

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value = "fruit::beerenmix|tk"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "beerenmix-tk",
                        reason =
                            CanonicalIdentityResolutionReason
                                .PRODUCT_BEFORE_STATE_PREFERRED
                    ),

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value = "ready-meals::margherita|pizza|tk"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "pizza-margherita-tk",
                        reason =
                            CanonicalIdentityResolutionReason
                                .PRODUCT_BEFORE_STATE_PREFERRED
                    ),

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value = "snacks::frites|pommes|tk"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "pommes-frites-tk",
                        reason =
                            CanonicalIdentityResolutionReason
                                .PRODUCT_BEFORE_STATE_PREFERRED
                    ),

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value = "vegetables::gemusepfanne|tk"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "gemuesepfanne-tk",
                        reason =
                            CanonicalIdentityResolutionReason
                                .PRODUCT_BEFORE_STATE_PREFERRED
                    ),

            /*
             * ---------------------------------------------------------
             * SPECIFIC DISH BEFORE GENERIC CLASS
             * ---------------------------------------------------------
             */

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value =
                    "ready-meals::carne|chili|con|fertiggericht"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "chili-con-carne-fertiggericht",
                        reason =
                            CanonicalIdentityResolutionReason
                                .SPECIFIC_DISH_BEFORE_GENERIC_CLASS_PREFERRED
                    ),

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value =
                    "ready-meals::fertiggericht|gemusecurry"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "gemuesecurry-fertiggericht",
                        reason =
                            CanonicalIdentityResolutionReason
                                .SPECIFIC_DISH_BEFORE_GENERIC_CLASS_PREFERRED
                    ),

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value =
                    "ready-meals::fertiggericht|gulasch"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "gulasch-fertiggericht",
                        reason =
                            CanonicalIdentityResolutionReason
                                .SPECIFIC_DISH_BEFORE_GENERIC_CLASS_PREFERRED
                    ),

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value = "rice::basmati|reis"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "basmati-reis",
                        reason =
                            CanonicalIdentityResolutionReason
                                .NATURAL_GERMAN_WORD_ORDER_PREFERRED
                    ),

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value = "rice::reis|sushi"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "sushi-reis",
                        reason =
                            CanonicalIdentityResolutionReason
                                .NATURAL_GERMAN_WORD_ORDER_PREFERRED
                    ),

            fingerprint(
                reason = "WORD_ORDER_VARIANT",
                value = "vegetables::erbsen|tiefkuhl"
            ) to
                    PreferredIdentity(
                        winnerNormalized =
                            "erbsen-tiefkuehl",
                        reason =
                            CanonicalIdentityResolutionReason
                                .PRODUCT_BEFORE_STATE_PREFERRED
                    ),
        )

    fun preferredIdentity(
        duplicateReason: String,
        fingerprint: String
    ): PreferredIdentity? =
        preferredByFingerprint[
            fingerprint(
                reason = duplicateReason,
                value = fingerprint
            )
        ]

    private fun fingerprint(
        reason: String,
        value: String
    ): String =
        "$reason::$value"
}