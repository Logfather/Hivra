package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.rule

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticContext
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticDecision
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticFinding
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticRule
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticRuleType

class CanonicalRedundantFamilyValueRule :
    CanonicalExpansionSemanticRule {

    override fun evaluate(
        context: CanonicalExpansionSemanticContext
    ): List<CanonicalExpansionSemanticFinding> {
        val redundantValues =
            REDUNDANT_VALUES_BY_FAMILY[
                context.candidate.familyKey
            ] ?: return emptyList()

        val findings =
            context.candidate.variantValues
                .filter { value ->
                    value.valueKey in
                            redundantValues
                }
                .map { value ->
                    CanonicalExpansionSemanticFinding(
                        ruleType =
                            CanonicalExpansionSemanticRuleType
                                .REDUNDANT_FAMILY_VALUE,

                        decision =
                            CanonicalExpansionSemanticDecision
                                .REJECT_REDUNDANT_VARIANT,

                        ruleKey =
                            "redundant-${context.candidate.familyKey}-" +
                                    value.valueKey,

                        message =
                            "Variant value '${value.valueKey}' is already " +
                                    "fully implied by family " +
                                    "'${context.candidate.familyKey}'."
                    )
                }

        /*
         * Nur ein reiner Einzelachsen-Kandidat wird wegen dieser
         * Redundanz verworfen.
         *
         * Bei Mehrfachkombinationen kann der redundante Wert später
         * entfernt werden, während andere Werte noch relevant bleiben.
         * Solche Fälle werden konservativ überprüft.
         */
        return when {
            findings.isEmpty() ->
                emptyList()

            context.candidate.variantValues.size == 1 ->
                findings

            else ->
                listOf(
                    CanonicalExpansionSemanticFinding(
                        ruleType =
                            CanonicalExpansionSemanticRuleType
                                .REDUNDANT_FAMILY_VALUE,

                        decision =
                            CanonicalExpansionSemanticDecision
                                .REVIEW_REQUIRED,

                        ruleKey =
                            "partially-redundant-family-value",

                        message =
                            "At least one value is implied by the family, " +
                                    "but additional variant values may still " +
                                    "justify a canonical entry."
                    )
                )
        }
    }

    private companion object {
        val REDUNDANT_VALUES_BY_FAMILY:
                Map<String, Set<String>> =
            sortedMapOf(
                "milk" to
                        setOf(
                            "milk",
                            "milk-protein"
                        ),

                "beef" to
                        setOf(
                            "beef",
                            "cattle",
                            "beef-protein"
                        ),

                "pork" to
                        setOf(
                            "pork",
                            "pig",
                            "pork-protein"
                        ),

                "chicken" to
                        setOf(
                            "chicken",
                            "chicken-protein",
                            "poultry-protein"
                        ),

                "turkey" to
                        setOf(
                            "turkey",
                            "poultry-protein"
                        ),

                "wheat-pasta" to
                        setOf(
                            "wheat"
                        ),

                "rye-flour" to
                        setOf(
                            "rye"
                        ),

                "wheat-flour" to
                        setOf(
                            "wheat"
                        ),

                "spelt-flour" to
                        setOf(
                            "spelt"
                        ),

                "oat-drinks" to
                        setOf(
                            "oats"
                        ),

                "soy-drinks" to
                        setOf(
                            "soy"
                        ),

                "almond-drinks" to
                        setOf(
                            "almond"
                        ),

                "rice-drinks" to
                        setOf(
                            "rice"
                        ),

                "coconut-drinks" to
                        setOf(
                            "coconut"
                        )
            )
    }
}