package de.shopme.tools.knowledge.catalog.canonical.rebuild

import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.LegacyCanonicalFoodItem

class CanonicalGermanFoodNameSelector {

    fun select(
        items: List<LegacyCanonicalFoodItem>
    ): LegacyCanonicalFoodItem {

        require(items.isNotEmpty())

        return items
            .sortedWith(
                compareByDescending<LegacyCanonicalFoodItem> {
                    canonicalScore(it)
                }
                    .thenBy {
                        it.itemname.length
                    }
                    .thenBy {
                        it.itemname
                    }
            )
            .first()
    }

    private fun canonicalScore(
        item: LegacyCanonicalFoodItem
    ): Int {

        var score =
            0

        val name =
            item.itemname

        /*
         * Generierte Family–Variant-Formen können nie
         * kanonischer Gewinner sein.
         */
        if (
            SYNTHETIC_SEPARATOR in name
        ) {
            score -=
                10_000
        }

        /*
         * Offensichtliche Zustands-/Meta-Suffixe
         * verlieren gegen die Basisidentity.
         */
        if (
            META_SUFFIX_REGEX.containsMatchIn(
                name
            )
        ) {
            score -=
                500
        }

        /*
         * Kürzere natürliche deutsche Produktnamen
         * gewinnen gegenüber verbose Varianten.
         */
        score -=
            name.count {
                it.isWhitespace()
            } * 10

        score -=
            name.count {
                it == '-'
            } * 5

        /*
         * Natürliche Komposita bevorzugen.
         */
        if (
            !name.contains(' ') &&
            !name.contains(',')
        ) {
            score +=
                50
        }

        return score
    }

    companion object {

        private const val SYNTHETIC_SEPARATOR =
            " – "

        private val META_SUFFIX_REGEX =
            Regex(
                """(?i)\b(""" +
                        listOf(
                            "bio",
                            "standard",
                            "frisch",
                            "gefrohren",
                            "gefroren",
                            "tiefgekühlt",
                            "tiefkuehlt",
                            "tiefkühl",
                            "tk",
                            "geschnitten",
                            "geraspelt",
                            "gewürfelt",
                            "getrocknet",
                            "konserviert"
                        )
                            .joinToString("|") +
                        """)\b"""
            )
    }
}