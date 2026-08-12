package de.shopme.tools.knowledge.catalog.canonical.rebuild

class CanonicalFoodBrandLeakDetector {

    fun isBrandIdentity(
        itemname: String
    ): Boolean {

        val normalized =
            itemname
                .lowercase()
                .trim()

        return BRAND_IDENTITIES.any {
            normalized ==
                    it
        }
    }

    companion object {

        /*
         * Nur eindeutig bekannte Leaks.
         *
         * Nicht fuzzy erweitern.
         */
        private val BRAND_IDENTITIES =
            setOf(
                "coca cola",
                "coca-cola",
                "nutella",
                "toffifee",
                "orangina",
                "maizena maisstärke",
                "maizena maisstaerke",
                "knorr gemüsebrühe",
                "knorr gemuesebruehe"
            )
    }
}