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

class CanonicalAnimalPlantConflictRule :
    CanonicalExpansionSemanticRule {

    override fun evaluate(
        context: CanonicalExpansionSemanticContext
    ): List<CanonicalExpansionSemanticFinding> {
        if (
            !context.hasAxis(
                CanonicalProductFamilyVariantAxis.ANIMAL_SPECIES
            ) ||
            !context.hasAxis(
                CanonicalProductFamilyVariantAxis.PLANT_SPECIES
            )
        ) {
            return emptyList()
        }

        return listOf(
            CanonicalExpansionSemanticFinding(
                ruleType =
                    CanonicalExpansionSemanticRuleType
                        .IMPOSSIBLE_ANIMAL_PLANT_COMBINATION,

                decision =
                    CanonicalExpansionSemanticDecision
                        .REJECT_IMPOSSIBLE_COMBINATION,

                ruleKey =
                    "animal-and-plant-species-conflict",

                message =
                    "A candidate cannot use animal species and plant " +
                            "species as parallel primary identity axes."
            )
        )
    }
}