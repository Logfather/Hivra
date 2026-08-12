package de.shopme.tools.knowledge.catalog.canonical.rebuild.base

class CanonicalFoodVariantCompatibilityRegistry {

    fun isCompatible(
        baseName: String,
        category: String,
        variant: String
    ): Boolean {

        val normalizedBase =
            normalize(
                baseName
            )

        return when (variant) {

            VARIANT_FROZEN ->
                frozenCompatible(
                    baseName =
                        normalizedBase,
                    category =
                        category
                )

            VARIANT_FRESH ->
                freshCompatible(
                    baseName =
                        normalizedBase,
                    category =
                        category
                )

            VARIANT_DRIED ->
                driedCompatible(
                    baseName =
                        normalizedBase,
                    category =
                        category
                )

            VARIANT_SMOKED ->
                smokedCompatible(
                    baseName =
                        normalizedBase,
                    category =
                        category
                )

            VARIANT_CANNED ->
                cannedCompatible(
                    baseName =
                        normalizedBase,
                    category =
                        category
                )

            VARIANT_SLICED,
            VARIANT_DICED,
            VARIANT_GRATED,
            VARIANT_GROUND -> {
                true
            }

            /*
             * Claims wie Bio oder Standard werden überhaupt
             * nicht als variants[] erzeugt und landen daher
             * hier normalerweise nicht.
             */
            else ->
                false
        }
    }

    private fun frozenCompatible(
        baseName: String,
        category: String
    ): Boolean {

        /*
         * Explizite Negativregeln haben Vorrang.
         *
         * Diese Kombinationen sind im deutschen
         * Supermarkt keine sinnvollen regulären
         * Produktidentitäten.
         */
        if (
            FROZEN_EXPLICITLY_INVALID_BASES.any {
                baseName.contains(
                    it
                )
            }
        ) {
            return false
        }

        if (
            category in
            FROZEN_ALLOWED_CATEGORIES
        ) {
            return true
        }

        return FROZEN_ALLOWED_BASE_HINTS.any {
            baseName.contains(
                it
            )
        }
    }

    private fun freshCompatible(
        baseName: String,
        category: String
    ): Boolean {

        if (
            category in
            FRESH_ALLOWED_CATEGORIES
        ) {
            return true
        }

        return FRESH_ALLOWED_BASE_HINTS.any {
            baseName.contains(
                it
            )
        }
    }

    private fun driedCompatible(
        baseName: String,
        category: String
    ): Boolean {

        if (
            category in
            DRIED_ALLOWED_CATEGORIES
        ) {
            return true
        }

        return DRIED_ALLOWED_BASE_HINTS.any {
            baseName.contains(
                it
            )
        }
    }

    private fun smokedCompatible(
        baseName: String,
        category: String
    ): Boolean {

        if (
            category in
            SMOKED_ALLOWED_CATEGORIES
        ) {
            return true
        }

        return SMOKED_ALLOWED_BASE_HINTS.any {
            baseName.contains(
                it
            )
        }
    }

    private fun cannedCompatible(
        baseName: String,
        category: String
    ): Boolean {

        if (
            category in
            CANNED_ALLOWED_CATEGORIES
        ) {
            return true
        }

        return CANNED_ALLOWED_BASE_HINTS.any {
            baseName.contains(
                it
            )
        }
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
            .trim()

    companion object {

        const val VARIANT_FROZEN =
            "tiefgekühlt"

        const val VARIANT_FRESH =
            "frisch"

        const val VARIANT_DRIED =
            "getrocknet"

        const val VARIANT_SMOKED =
            "geräuchert"

        const val VARIANT_CANNED =
            "konserviert"

        const val VARIANT_SLICED =
            "geschnitten"

        const val VARIANT_DICED =
            "gewürfelt"

        const val VARIANT_GRATED =
            "geraspelt"

        const val VARIANT_GROUND =
            "gemahlen"

        private val FROZEN_ALLOWED_CATEGORIES =
            setOf(
                "fruit",
                "vegetables",
                "fish",
                "meat",
                "bakery",
                "ready-meals"
            )

        /*
         * Kategorie allein reicht nicht.
         * Cocktailtomaten sind z.B. Gemüse, aber
         * trotzdem kein regulärer TK-Supermarktartikel.
         */
        private val FROZEN_EXPLICITLY_INVALID_BASES =
            setOf(
                "cocktailtomate",
                "cherrytomate",
                "kirschtomate",
                "salattomate"
            )

        private val FROZEN_ALLOWED_BASE_HINTS =
            setOf(
                "erdbeer",
                "himbeer",
                "blaubeer",
                "heidelbeer",
                "beeren",
                "waldbeer",
                "erbsen",
                "bohnen",
                "spinat",
                "brokkoli",
                "blumenkohl",
                "karotten",
                "moehren",
                "gemuesemischung",
                "gemuesemix",
                "erbsensuppe",
                "croissant",
                "pizza",
                "fischfilet"
            )

        private val FRESH_ALLOWED_CATEGORIES =
            setOf(
                "fruit",
                "vegetables",
                "meat",
                "fish",
                "bakery",
                "dairy"
            )

        private val FRESH_ALLOWED_BASE_HINTS =
            setOf(
                "saft",
                "brot",
                "milch",
                "fleisch",
                "fisch"
            )

        private val DRIED_ALLOWED_CATEGORIES =
            setOf(
                "fruit",
                "vegetables",
                "spices",
                "legumes",
                "mushrooms",
                "pasta"
            )

        private val DRIED_ALLOWED_BASE_HINTS =
            setOf(
                "frucht",
                "obst",
                "pilz",
                "kraeuter",
                "kraut",
                "tomate",
                "bohne",
                "linse"
            )

        private val SMOKED_ALLOWED_CATEGORIES =
            setOf(
                "fish",
                "meat",
                "sausage"
            )

        private val SMOKED_ALLOWED_BASE_HINTS =
            setOf(
                "lachs",
                "forelle",
                "schinken",
                "speck",
                "fleisch",
                "wurst"
            )

        private val CANNED_ALLOWED_CATEGORIES =
            setOf(
                "canned-food",
                "vegetables",
                "fruit",
                "legumes",
                "fish"
            )

        private val CANNED_ALLOWED_BASE_HINTS =
            setOf(
                "bohne",
                "erbsen",
                "mais",
                "thunfisch",
                "tomate",
                "champignon",
                "ananas",
                "pfirsich"
            )
    }
}