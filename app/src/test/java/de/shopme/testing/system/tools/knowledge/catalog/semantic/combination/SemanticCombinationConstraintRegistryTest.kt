package de.shopme.testing.system.tools.knowledge.catalog.semantic.combination

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class SemanticCombinationConstraintRegistryTest {

    @Test
    fun rejectsCombinationContainingGenericPlaceholder() {

        assertDecision(
            family = "Brot",
            category = "bakery",
            variants =
                listOf(
                    "Fermentiert",
                    "Fischbasiert"
                ),
            expected =
                SemanticCombinationDecision.REJECT,
            expectedReason =
                SemanticCombinationConstraintReason
                    .GENERIC_PLACEHOLDER_IN_COMBINATION
        )
    }

    @Test
    fun allowsExplicitBreadIngredientProcessingPair() {

        assertDecision(
            family = "Brot",
            category = "bakery",
            variants =
                listOf(
                    "Buchweizen",
                    "Fermentiert"
                ),
            expected =
                SemanticCombinationDecision.ALLOW,
            expectedReason =
                SemanticCombinationConstraintReason
                    .PROFILE_COMBINATION_ALLOW
        )
    }

    @Test
    fun rejectsCombinationContainingIndividuallyRejectedVariant() {

        assertDecision(
            family = "Butter",
            category = "dairy",
            variants =
                listOf(
                    "Fettreich",
                    "Höhlengereift"
                ),
            expected =
                SemanticCombinationDecision.REJECT,
            expectedReason =
                SemanticCombinationConstraintReason
                    .CONTAINS_INCOMPATIBLE_MEMBER_VARIANT
        )
    }

    @Test
    fun rejectsMutuallyExclusiveMaturationValues() {

        assertDecision(
            family = "Hartkäse",
            category = "dairy",
            variants =
                listOf(
                    "Lang gereift",
                    "Höhlengereift"
                ),
            expected =
                SemanticCombinationDecision.REJECT,
            expectedReason =
                SemanticCombinationConstraintReason
                    .MUTUALLY_EXCLUSIVE_TYPE_VALUES
        )
    }

    @Test
    fun doesNotImplicitlyAllowUnknownPair() {

        assertDecision(
            family = "Hartkäse",
            category = "dairy",
            variants =
                listOf(
                    "Mild",
                    "Geschnitten"
                ),
            expected =
                SemanticCombinationDecision.REVIEW,
            expectedReason =
                SemanticCombinationConstraintReason
                    .NO_EXPLICIT_COMBINATION_RULE
        )
    }

    private fun assertDecision(
        family: String,
        category: String,
        variants: List<String>,
        expected: SemanticCombinationDecision,
        expectedReason: SemanticCombinationConstraintReason
    ) {

        val definitions =
            variants.map { rawValue ->
                requireNotNull(
                    SemanticVariantTaxonomyRegistry
                        .definitionFor(rawValue)
                ) {
                    "Missing taxonomy definition for '$rawValue'."
                }
            }

        val result =
            SemanticCombinationConstraintRegistry
                .evaluate(
                    family = family,
                    category = category,
                    variants = definitions
                )

        assertEquals(
            expected = expected,
            actual = result.decision
        )

        assertEquals(
            expected = expectedReason,
            actual = result.reason
        )
    }
}