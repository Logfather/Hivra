package de.shopme.tools.knowledge.ciqual.parser

import de.shopme.tools.knowledge.ciqual.model.CiqualNutritionValues
import java.text.Normalizer

internal class CiqualNutrientNormalizer {

    fun normalize(
        constituentValues: Map<String, Double>
    ): CiqualNutritionValues {
        val normalized =
            constituentValues
                .entries
                .associate { entry ->
                    normalizeName(entry.key) to entry.value
                }

        return CiqualNutritionValues(
            energyKcalPer100g =
                normalized.findValue(
                    "energie reglement ue n 1169 2011 kcal",
                    "energie kcal",
                    "energie",
                    requiredToken = "kcal"
                ),
            fatPer100g =
                normalized.findValue(
                    "lipides",
                    "matieres grasses",
                    "matiere grasse"
                ),
            saturatedFatPer100g =
                normalized.findValue(
                    "ag satures",
                    "acides gras satures",
                    "acide gras sature",
                    "dont acides gras satures"
                ),
            carbohydratesPer100g =
                normalized.findValue(
                    "glucides",
                    "glucides assimilables",
                    "glucides disponibles"
                ),
            sugarsPer100g =
                normalized.findValue(
                    "sucres",
                    "dont sucres"
                ),
            fiberPer100g =
                normalized.findValue(
                    "fibres alimentaires",
                    "fibres"
                ),
            proteinsPer100g =
                normalized.findValue(
                    "proteines",
                    "proteines n x 6 25"
                ),
            saltPer100g =
                normalized.findValue(
                    "sel chlorure de sodium",
                    "sel"
                )
        )
    }

    private fun Map<String, Double>.findValue(
        vararg acceptedNames: String,
        requiredToken: String? = null
    ): Double? {
        acceptedNames.forEach { acceptedName ->
            this[acceptedName]?.let {
                return it
            }
        }

        return entries
            .asSequence()
            .filter { entry ->
                requiredToken == null ||
                        entry.key.contains(requiredToken)
            }
            .filter { entry ->
                acceptedNames.any { acceptedName ->
                    entry.key == acceptedName ||
                            entry.key.startsWith("$acceptedName ") ||
                            entry.key.contains(" $acceptedName ")
                }
            }
            .map { it.value }
            .firstOrNull()
    }

    private fun normalizeName(
        value: String
    ): String =
        Normalizer
            .normalize(
                value,
                Normalizer.Form.NFD
            )
            .replace(
                Regex("\\p{M}+"),
                ""
            )
            .lowercase()
            .replace(
                Regex("[^a-z0-9]+"),
                " "
            )
            .trim()
}