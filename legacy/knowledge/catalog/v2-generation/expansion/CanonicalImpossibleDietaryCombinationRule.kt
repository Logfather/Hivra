package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.rule

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticContext
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticDecision
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticFinding
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticRule
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticRuleType

class CanonicalImpossibleDietaryCombinationRule :
    CanonicalExpansionSemanticRule {

    override fun evaluate(
        context: CanonicalExpansionSemanticContext
    ): List<CanonicalExpansionSemanticFinding> {
        val dietaryForm =
            context.valueFor(
                CanonicalProductFamilyVariantAxis.DIETARY_FORM
            ) ?: return emptyList()

        val animalSpecies =
            context.valueFor(
                CanonicalProductFamilyVariantAxis.ANIMAL_SPECIES
            )

        val proteinSource =
            context.valueFor(
                CanonicalProductFamilyVariantAxis.PROTEIN_SOURCE
            )

        if (
            dietaryForm == "vegan" &&
            (
                    animalSpecies != null ||
                            proteinSource in
                            ANIMAL_PROTEIN_VALUES
                    )
        ) {
            return listOf(
                finding(
                    ruleKey =
                        "vegan-animal-source-conflict",

                    message =
                        "Vegan dietary form cannot be combined with an " +
                                "animal species or animal protein source."
                )
            )
        }

        if (
            dietaryForm == "vegetarian" &&
            (
                    animalSpecies != null ||
                            proteinSource in
                            MEAT_AND_FISH_PROTEIN_VALUES
                    )
        ) {
            return listOf(
                finding(
                    ruleKey =
                        "vegetarian-meat-source-conflict",

                    message =
                        "Vegetarian dietary form cannot be combined with " +
                                "meat, poultry or fish protein."
                )
            )
        }

        return emptyList()
    }

    private fun finding(
        ruleKey: String,
        message: String
    ): CanonicalExpansionSemanticFinding =
        CanonicalExpansionSemanticFinding(
            ruleType =
                CanonicalExpansionSemanticRuleType
                    .IMPOSSIBLE_DIETARY_FORM,

            decision =
                CanonicalExpansionSemanticDecision
                    .REJECT_IMPOSSIBLE_COMBINATION,

            ruleKey =
                ruleKey,

            message =
                message
        )

    private companion object {
        val ANIMAL_PROTEIN_VALUES =
            setOf(
                "milk-protein",
                "egg-protein",
                "beef-protein",
                "pork-protein",
                "poultry-protein",
                "fish-protein",
                "mixed-animal-protein"
            )

        val MEAT_AND_FISH_PROTEIN_VALUES =
            setOf(
                "beef-protein",
                "pork-protein",
                "poultry-protein",
                "fish-protein",
                "mixed-animal-protein"
            )
    }
}