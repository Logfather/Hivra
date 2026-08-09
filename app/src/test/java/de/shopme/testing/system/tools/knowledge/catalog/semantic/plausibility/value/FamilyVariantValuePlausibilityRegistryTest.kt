package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.value

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class FamilyVariantValuePlausibilityRegistryTest {

    @Test
    fun rejectsKnownSyntheticFishForms() {

        assertDecision(
            family = "Frischfisch",
            category = "fish",
            variant = "Flocken",
            expected =
                FamilyVariantValuePlausibilityDecision.REJECT
        )

        assertDecision(
            family = "Fischfilets",
            category = "fish",
            variant = "Flocken",
            expected =
                FamilyVariantValuePlausibilityDecision.REJECT
        )

        assertDecision(
            family = "Krustentiere",
            category = "fish",
            variant = "Flocken",
            expected =
                FamilyVariantValuePlausibilityDecision.REJECT
        )
    }

    @Test
    fun rejectsPreparedRawMeatAsCanonicalIdentity() {

        assertDecision(
            family = "Wildfleisch",
            category = "meat",
            variant = "Frittiert",
            expected =
                FamilyVariantValuePlausibilityDecision.REJECT
        )

        assertDecision(
            family = "Wildfleisch",
            category = "meat",
            variant = "Gebacken",
            expected =
                FamilyVariantValuePlausibilityDecision.REJECT
        )

        assertDecision(
            family = "Hähnchenfleisch",
            category = "meat",
            variant = "Frittiert",
            expected =
                FamilyVariantValuePlausibilityDecision.REJECT
        )
    }

    @Test
    fun allowsKnownMeatCuts() {

        assertDecision(
            family = "Hähnchenfleisch",
            category = "meat",
            variant = "Filet",
            expected =
                FamilyVariantValuePlausibilityDecision.ALLOW
        )

        assertDecision(
            family = "Kalbfleisch",
            category = "meat",
            variant = "Kotelett",
            expected =
                FamilyVariantValuePlausibilityDecision.ALLOW
        )
    }

    @Test
    fun rejectsKnownCrossSemanticAlternativeValues() {

        assertDecision(
            family = "Fleischalternativen",
            category = "plant-based-alternatives",
            variant = "Apfel",
            expected =
                FamilyVariantValuePlausibilityDecision.REJECT
        )

        assertDecision(
            family = "Fleischalternativen",
            category = "plant-based-alternatives",
            variant = "Bohne",
            expected =
                FamilyVariantValuePlausibilityDecision.REJECT
        )
    }

    @Test
    fun unknownConcreteValueRemainsReview() {

        assertDecision(
            family = "Fleischalternativen",
            category = "plant-based-alternatives",
            variant = "Bitter",
            expected =
                FamilyVariantValuePlausibilityDecision.REVIEW
        )
    }

    private fun assertDecision(
        family: String,
        category: String,
        variant: String,
        expected: FamilyVariantValuePlausibilityDecision
    ) {

        val definition =
            requireNotNull(
                SemanticVariantTaxonomyRegistry
                    .definitionFor(
                        variant
                    )
            ) {
                "Missing semantic variant definition for '$variant'."
            }

        val result =
            FamilyVariantValuePlausibilityRegistry
                .evaluate(
                    family = family,
                    category = category,
                    variant = definition
                )

        assertEquals(
            expected = expected,
            actual = result.decision
        )
    }
}