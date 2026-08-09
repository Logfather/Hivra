package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

import java.text.Normalizer

object CanonicalProductFamilyPluralPolicy {

    enum class PluralKind {

        /**
         * Family-Bezeichnung steht bereits im Plural.
         *
         * Beispiel:
         * Bohnen → Bohnen
         */
        ALREADY_PLURAL,

        /**
         * Massennomen oder im Catalog invariantes Nomen.
         *
         * Beispiel:
         * Hafer → Hafer
         */
        INVARIANT,

        /**
         * Expliziter deutscher Plural.
         *
         * Beispiel:
         * Rohwurst → Rohwürste
         */
        EXPLICIT_PLURAL
    }

    data class Definition(
        val family: String,
        val plural: String,
        val kind: PluralKind
    )

    /*
     * Regex vor DEFINITIONS initialisieren:
     * definitionsByKey ruft während object initialization key(...) auf.
     */
    private val COMBINING_MARKS_REGEX =
        Regex("\\p{M}+")

    private val NON_ALPHANUMERIC_REGEX =
        Regex("[^a-z0-9]+")

    private val WHITESPACE_REGEX =
        Regex("\\s+")

    private val DEFINITIONS =
        listOf(

            /*
             * -----------------------------------------------------
             * ALREADY PLURAL
             * -----------------------------------------------------
             */

            alreadyPlural("Fleischalternativen"),
            alreadyPlural("Kräuter"),
            alreadyPlural("Süße Frühstücksprodukte"),
            alreadyPlural("Bohnen"),
            alreadyPlural("Hülsenfruchtkonserven"),
            alreadyPlural("Fischerzeugnisse"),
            alreadyPlural("Fischgerichte"),
            alreadyPlural("Gemüsekonserven"),
            alreadyPlural("Herzhafte Frühstücksprodukte"),
            alreadyPlural("Obstkonserven"),
            alreadyPlural("Wurstalternativen"),
            alreadyPlural("Eintöpfe"),
            alreadyPlural("Fruchtaufstriche"),
            alreadyPlural("Nussaufstriche"),
            alreadyPlural("Suppen"),
            alreadyPlural("Tomatensaucen"),
            alreadyPlural("Eingelegte Lebensmittel"),
            alreadyPlural("Fleischgerichte"),
            alreadyPlural("Gemüsegerichte"),
            alreadyPlural("Haferdrinks"),
            alreadyPlural("Internationale Gerichte"),
            alreadyPlural("Kartoffelgerichte"),
            alreadyPlural("Laugengebäck-Snacks"),
            alreadyPlural("Nudelgerichte"),
            alreadyPlural("Reisgerichte"),
            alreadyPlural("Salatdressings"),
            alreadyPlural("Sojadrinks"),
            alreadyPlural("Vegane Gerichte"),
            alreadyPlural("Vegetarische Gerichte"),
            alreadyPlural("Asiatische Saucen"),
            alreadyPlural("Gemüseaufstriche"),
            alreadyPlural("Kerne und Saaten"),
            alreadyPlural("Maissnacks"),
            alreadyPlural("Snackmischungen"),
            alreadyPlural("Tortillachips"),
            alreadyPlural("Einzelgewürze"),
            alreadyPlural("Fischfilets"),
            alreadyPlural("Gemüsechips"),
            alreadyPlural("Gewürzmischungen"),
            alreadyPlural("Fertiggerichtkonserven"),
            alreadyPlural("Mandeldrinks"),
            alreadyPlural("Krustentiere"),
            alreadyPlural("Schokoladenaufstriche"),

            /*
             * -----------------------------------------------------
             * INVARIANT / MASS NOUNS
             * -----------------------------------------------------
             */

            invariant("Porridge"),
            invariant("Hafer"),
            invariant("Frischfisch"),
            invariant("Wildfleisch"),
            invariant("Hähnchenfleisch"),
            invariant("Joghurt"),

            /*
             * -----------------------------------------------------
             * EXPLICIT PLURALS
             * -----------------------------------------------------
             */

            explicit(
                family = "Gefüllte Schokolade",
                plural = "Gefüllte Schokoladen"
            ),

            explicit(
                family = "Rohwurst",
                plural = "Rohwürste"
            ),

            explicit(
                family = "Streichwurst",
                plural = "Streichwürste"
            ),

            explicit(
                family = "Fruchtnektar",
                plural = "Fruchtnektare"
            )
        )

    private val definitionsByKey =
        DEFINITIONS.associateBy {
            key(it.family)
        }

    init {

        require(
            definitionsByKey.size ==
                    DEFINITIONS.size
        ) {
            "Canonical product-family plural keys must be unique."
        }
    }

    fun definitionFor(
        family: String
    ): Definition? =
        definitionsByKey[
            key(family)
        ]

    fun pluralFor(
        family: String
    ): String? =
        definitionFor(family)
            ?.plural

    fun contains(
        family: String
    ): Boolean =
        definitionFor(family) != null

    fun definitions(): List<Definition> =
        DEFINITIONS.toList()

    private fun alreadyPlural(
        family: String
    ) =
        Definition(
            family = family,
            plural = family,
            kind =
                PluralKind.ALREADY_PLURAL
        )

    private fun invariant(
        family: String
    ) =
        Definition(
            family = family,
            plural = family,
            kind =
                PluralKind.INVARIANT
        )

    private fun explicit(
        family: String,
        plural: String
    ) =
        Definition(
            family = family,
            plural = plural,
            kind =
                PluralKind.EXPLICIT_PLURAL
        )

    private fun key(
        value: String
    ): String {

        val decomposed =
            Normalizer.normalize(
                value
                    .trim()
                    .lowercase(),
                Normalizer.Form.NFD
            )

        return decomposed
            .replace(
                COMBINING_MARKS_REGEX,
                ""
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
    }
}