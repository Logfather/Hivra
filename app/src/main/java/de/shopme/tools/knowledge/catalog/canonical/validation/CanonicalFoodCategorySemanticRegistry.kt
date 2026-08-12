package de.shopme.tools.knowledge.catalog.canonical.validation

class CanonicalFoodCategorySemanticRegistry {

    fun expectedCategories(
        itemname: String
    ): Set<String> {

        val normalized =
            normalize(
                itemname
            )

        exactOverrides[
            normalized
        ]
            ?.let {
                return it
            }

        /*
         * Wichtig:
         * Regeln arbeiten ausschließlich auf vollständigen
         * Produktfamilien / Heads.
         *
         * Kein beliebiges substring matching mehr.
         */
        familyRules
            .firstOrNull { rule ->

                rule.pattern
                    .containsMatchIn(
                        normalized
                    )
            }
            ?.let {
                return it.categories
            }

        return emptySet()
    }

    private fun normalize(
        value: String
    ): String =
        value
            .lowercase()
            .replace(
                "ä",
                "ae"
            )
            .replace(
                "ö",
                "oe"
            )
            .replace(
                "ü",
                "ue"
            )
            .replace(
                "ß",
                "ss"
            )
            .replace(
                NON_ALPHANUMERIC_REGEX,
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()

    private data class FamilyRule(
        val pattern: Regex,
        val categories: Set<String>
    )

    companion object {

        /*
         * Explizite semantische Sonderfälle.
         *
         * Diese Liste ist bewusst klein und eindeutig.
         */
        private val exactOverrides =
            mapOf(
                "agavendicksaft" to
                        setOf(
                            "confectionery"
                        ),

                "ahornsirup" to
                        setOf(
                            "confectionery"
                        ),

                "apfelessig" to
                        setOf(
                            "sauces"
                        ),

                "himbeeressig" to
                        setOf(
                            "sauces"
                        ),

                "flachsoel" to
                        setOf(
                            "oils"
                        ),

                "cashewjoghurt" to
                        setOf(
                            "plant-based-alternatives"
                        ),

                "haferjoghurt" to
                        setOf(
                            "plant-based-alternatives"
                        ),

                "karottensaft" to
                        setOf(
                            "beverages"
                        )
            )

        /*
         * Reihenfolge ist semantische Priorität.
         *
         * Spezifische Heads müssen vor generischen
         * Bestandteilen geprüft werden.
         */
        private val familyRules =
            listOf(

                /*
                 * Ready meals
                 */
                FamilyRule(
                    pattern =
                        Regex(
                            """\b(fertiggericht|fertigmahlzeit|fertigsuppe|fertigsalat)\b"""
                        ),
                    categories =
                        setOf(
                            "ready-meals"
                        )
                ),

                FamilyRule(
                    pattern =
                        Regex(
                            """\b(currywurst mit pommes|currywurst mit sosse)\b"""
                        ),
                    categories =
                        setOf(
                            "ready-meals"
                        )
                ),

                /*
                 * Sauces / spreads
                 */
                FamilyRule(
                    pattern =
                        Regex(
                            """\b(brotaufstrich|aufstrich)\b"""
                        ),
                    categories =
                        setOf(
                            "spreads",
                            "confectionery"
                        )
                ),

                FamilyRule(
                    pattern =
                        Regex(
                            """\b(sauce|sosse|dressing|dip|ketchup|senf|essig)\b"""
                        ),
                    categories =
                        setOf(
                            "sauces"
                        )
                ),

                /*
                 * Snacks
                 */
                FamilyRule(
                    pattern =
                        Regex(
                            """\b(chips|cracker|snack|sticks|flips)\b"""
                        ),
                    categories =
                        setOf(
                            "snacks"
                        )
                ),

                /*
                 * Plant-based alternatives.
                 *
                 * Vor dairy, weil "Haferjoghurt"
                 * semantisch kein Milchprodukt ist.
                 */
                FamilyRule(
                    pattern =
                        Regex(
                            """\b(hafer|cashew|soja|mandel|kokos|erbse)[a-z]*\s*(joghurt|drink|milch|creme)\b"""
                        ),
                    categories =
                        setOf(
                            "plant-based-alternatives",
                            "plant-based-drinks"
                        )
                ),

                /*
                 * Beverages
                 */
                FamilyRule(
                    pattern =
                        Regex(
                            """\b[a-z]+saft\b"""
                        ),
                    categories =
                        setOf(
                            "beverages"
                        )
                ),

                FamilyRule(
                    pattern =
                        Regex(
                            """\b(cola|limonade|eistee|mineralwasser|smoothie|tee|kaffee)\b"""
                        ),
                    categories =
                        setOf(
                            "beverages"
                        )
                ),

                /*
                 * Dairy
                 */
                FamilyRule(
                    pattern =
                        Regex(
                            """\b(joghurt|quark|skyr|buttermilch|frischkaese|kaese|milch)\b"""
                        ),
                    categories =
                        setOf(
                            "dairy"
                        )
                ),

                /*
                 * Fish
                 */
                FamilyRule(
                    pattern =
                        Regex(
                            """\b(lachs|forelle|thunfisch|hering|makrele|kabeljau|heilbutt|karpfen|aal)\b"""
                        ),
                    categories =
                        setOf(
                            "fish",
                            "canned-food"
                        )
                ),

                /*
                 * Meat / sausage
                 */
                FamilyRule(
                    pattern =
                        Regex(
                            """\b(wurst|salami|bratwurst|leberwurst|mettwurst)\b"""
                        ),
                    categories =
                        setOf(
                            "sausage",
                            "meat"
                        )
                ),

                FamilyRule(
                    pattern =
                        Regex(
                            """\b(rindfleisch|schweinefleisch|kalbfleisch|lammfleisch|hirschfleisch)\b"""
                        ),
                    categories =
                        setOf(
                            "meat"
                        )
                ),

                /*
                 * Bakery
                 */
                FamilyRule(
                    pattern =
                        Regex(
                            """\b(brot|broetchen|baguette|croissant|toast)\b"""
                        ),
                    categories =
                        setOf(
                            "bakery"
                        )
                ),

                /*
                 * Fruit / vegetables nur als echte
                 * vollständige Produktfamilien.
                 *
                 * Kein "contains(apfel)" mehr.
                 */
                FamilyRule(
                    pattern =
                        Regex(
                            """^(apfel|aepfel|birne|birnen|banane|bananen|erdbeere|erdbeeren|himbeere|himbeeren|blaubeere|blaubeeren|heidelbeere|heidelbeeren|orange|orangen)$"""
                        ),
                    categories =
                        setOf(
                            "fruit"
                        )
                ),

                FamilyRule(
                    pattern =
                        Regex(
                            """^(tomate|tomaten|cocktailtomaten|cherrytomaten|gurke|gurken|paprika|brokkoli|blumenkohl|spinat|karotte|karotten|moehre|moehren)$"""
                        ),
                    categories =
                        setOf(
                            "vegetables"
                        )
                )
            )

        private val NON_ALPHANUMERIC_REGEX =
            Regex(
                """[^a-z0-9]+"""
            )

        private val WHITESPACE_REGEX =
            Regex(
                """\s+"""
            )
    }
}