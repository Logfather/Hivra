package de.shopme.tools.knowledge.catalog.truecanonical

import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.CanonicalFoodIdentity

class TrueCanonicalGermanFoodCatalogValidator(
    private val nameNormalizer:
    TrueCanonicalFoodNameNormalizer =
        TrueCanonicalFoodNameNormalizer()
) {

    fun validate(
        entries: List<CanonicalFoodIdentity>
    ) {

        require(
            entries.isNotEmpty()
        )

        validateStructure(
            entries
        )

        validateNoVariants(
            entries
        )

        validateNoSourceVariants(
            entries
        )

        validateNormalizedKeys(
            entries
        )

        validateNoDuplicateIdentities(
            entries
        )

        validateNoForbiddenIdentityAttributes(
            entries
        )

        validateNoBrands(
            entries
        )

        validateNoKnownSingularPluralConflicts(
            entries
        )

        validateCategories(
            entries
        )
    }

    private fun validateStructure(
        entries: List<CanonicalFoodIdentity>
    ) {

        entries.forEach { entry ->

            require(
                entry.itemname.isNotBlank()
            )

            require(
                entry.normalized.isNotBlank()
            )

            require(
                entry.category.isNotBlank()
            )
        }
    }

    private fun validateNoVariants(
        entries: List<CanonicalFoodIdentity>
    ) {

        val offenders =
            entries
                .filter {
                    it.variants.isNotEmpty()
                }

        require(
            offenders.isEmpty()
        ) {
            "True canonical catalog must contain zero variants: " +
                    offenders.map {
                        it.itemname
                    }
        }
    }

    private fun validateNoSourceVariants(
        entries: List<CanonicalFoodIdentity>
    ) {

        val offenders =
            entries
                .filter {
                    it.sourceVariants.isNotEmpty()
                }

        require(
            offenders.isEmpty()
        ) {
            "True canonical catalog must contain zero sourceVariants: " +
                    offenders.map {
                        it.itemname
                    }
        }
    }

    private fun validateNormalizedKeys(
        entries: List<CanonicalFoodIdentity>
    ) {

        val invalid =
            entries
                .filter { entry ->

                    entry.normalized !=
                            nameNormalizer.normalize(
                                entry.itemname
                            )
                }

        require(
            invalid.isEmpty()
        ) {
            "Invalid normalized canonical keys: " +
                    invalid.map {
                        "${it.itemname} -> ${it.normalized}"
                    }
        }
    }

    private fun validateNoDuplicateIdentities(
        entries: List<CanonicalFoodIdentity>
    ) {

        val duplicates =
            entries
                .groupBy {
                    it.normalized
                }
                .filterValues {
                    it.size >
                            1
                }

        require(
            duplicates.isEmpty()
        ) {
            "Duplicate true canonical identities: " +
                    duplicates.values.map { group ->
                        group.map {
                            it.itemname
                        }
                    }
        }
    }

    private fun validateNoForbiddenIdentityAttributes(
        entries: List<CanonicalFoodIdentity>
    ) {

        val invalid =
            entries
                .filter { entry ->

                    FORBIDDEN_PATTERNS.any { pattern ->

                        pattern.containsMatchIn(
                            entry.itemname
                        )
                    }
                }

        require(
            invalid.isEmpty()
        ) {
            "Non-identity attributes found in true canonical catalog: " +
                    invalid.map {
                        it.itemname
                    }
        }
    }

    private fun validateNoBrands(
        entries: List<CanonicalFoodIdentity>
    ) {

        val invalid =
            entries
                .filter { entry ->

                    KNOWN_BRANDS.any { brand ->

                        Regex(
                            """(?i)(^|\s)$brand(\s|$)"""
                        )
                            .containsMatchIn(
                                entry.itemname
                            )
                    }
                }

        require(
            invalid.isEmpty()
        ) {
            "Brands found in true canonical catalog: " +
                    invalid.map {
                        it.itemname
                    }
        }
    }

    private fun validateNoKnownSingularPluralConflicts(
        entries: List<CanonicalFoodIdentity>
    ) {

        val keys =
            entries
                .map {
                    it.normalized
                }
                .toSet()

        val conflicts =
            SINGULAR_TO_CANONICAL_PLURAL
                .filter { (singular, _) ->

                    singular in
                            keys
                }

        require(
            conflicts.isEmpty()
        ) {
            "Singular identities found where canonical plural is required: " +
                    conflicts
        }
    }

    private fun validateCategories(
        entries: List<CanonicalFoodIdentity>
    ) {

        val invalid =
            entries
                .filter {
                    it.category !in
                            ALLOWED_CATEGORIES
                }

        require(
            invalid.isEmpty()
        ) {
            "Unknown categories: " +
                    invalid.map {
                        "${it.itemname} -> ${it.category}"
                    }
        }
    }

    companion object {

        private val FORBIDDEN_PATTERNS =
            listOf(

                /*
                 * Claims
                 */
                Regex(
                    """(?i)(^|\s)(bio|organic|vegan|vegetarisch|glutenfrei|zuckerfrei)(\s|$)"""
                ),

                Regex(
                    """(?i)(^|\s)ohne\s+zucker(\s|$)"""
                ),

                /*
                 * Storage
                 */
                Regex(
                    """(?i)(^|\s)(tk|tiefkühl|tiefgekühlt|gefroren)(\s|$)"""
                ),

                /*
                 * Processing
                 */
                Regex(
                    """(?i)(^|\s)(gekocht|gebraten|gebacken|geröstet|mariniert|geschält|gehackt|raffiniert)(\s|$)"""
                ),

                /*
                 * Packaging / preserving
                 */
                Regex(
                    """(?i)(^|\s)(dose|dosen|konserve|glas|beutel)(\s|$)"""
                ),

                /*
                 * Synthetic generic modifiers
                 */
                Regex(
                    """(?i)(^|\s)(standard|klassisch|premium)(\s|$)"""
                )
            )

        private val KNOWN_BRANDS =
            setOf(
                "coca-cola",
                "pepsi",
                "nutella",
                "toffifee",
                "maggi",
                "knorr",
                "haribo"
            )

        /*
         * Nur sichere zählbare Lebensmittelpaare.
         *
         * Registry enthält nur die rechte Seite.
         */
        private val SINGULAR_TO_CANONICAL_PLURAL =
            mapOf(
                "apfel" to
                        "aepfel",

                "aprikose" to
                        "aprikosen",

                "banane" to
                        "bananen",

                "birne" to
                        "birnen",

                "blaubeere" to
                        "blaubeeren",

                "brombeere" to
                        "brombeeren",

                "clementine" to
                        "clementinen",

                "cranberry" to
                        "cranberries",

                "dattel" to
                        "datteln",

                "erdbeere" to
                        "erdbeeren",

                "feige" to
                        "feigen",

                "himbeere" to
                        "himbeeren",

                "kirsche" to
                        "kirschen",

                "karotte" to
                        "karotten",

                "kartoffel" to
                        "kartoffeln",

                "tomate" to
                        "tomaten",

                "croissant" to
                        "croissants"
            )

        private val ALLOWED_CATEGORIES =
            setOf(
                "fruit",
                "vegetables",
                "legumes",
                "meat",
                "sausage",
                "fish",
                "dairy",
                "bakery",
                "grains",
                "rice",
                "pasta",
                "flour",
                "oils",
                "beverages",
                "plant-based-drinks",
                "plant-based-alternatives",
                "sauces",
                "spreads",
                "ready-meals",
                "snacks",
                "confectionery",
                "spices",
                "baking-ingredients"
            )
    }
}