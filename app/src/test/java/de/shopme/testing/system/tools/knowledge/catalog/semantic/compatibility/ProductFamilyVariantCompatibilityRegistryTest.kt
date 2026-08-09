package de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductFamilyVariantCompatibilityRegistryTest {

    @Test
    fun rejectsKnownInvalidButterVariants() {

        assertDecision(
            family = "Butter",
            category = "dairy",
            variant = "Höhlengereift",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )

        assertDecision(
            family = "Butter",
            category = "dairy",
            variant = "Salzlakegereift",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )

        assertDecision(
            family = "Butter",
            category = "dairy",
            variant = "Frittiert",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )

        assertDecision(
            family = "Butter",
            category = "dairy",
            variant = "Gebacken",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )
    }

    @Test
    fun rejectsKnownInvalidFreshCheesePreparationVariants() {

        assertDecision(
            family = "Frischkäse",
            category = "dairy",
            variant = "Frittiert",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )

        assertDecision(
            family = "Frischkäse",
            category = "dairy",
            variant = "Gebacken",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )

        assertDecision(
            family = "Frischkäse",
            category = "dairy",
            variant = "Gekocht",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )
    }

    @Test
    fun allowsMaturationForHardCheese() {

        assertDecision(
            family = "Hartkäse",
            category = "dairy",
            variant = "Höhlengereift",
            expected =
                ProductFamilyVariantCompatibilityDecision.ALLOW
        )

        assertDecision(
            family = "Hartkäse",
            category = "dairy",
            variant = "Lang gereift",
            expected =
                ProductFamilyVariantCompatibilityDecision.ALLOW
        )
    }

    @Test
    fun allowsIngredientVariantsForBread() {

        assertDecision(
            family = "Brot",
            category = "bakery",
            variant = "Gerste",
            expected =
                ProductFamilyVariantCompatibilityDecision.ALLOW
        )

        assertDecision(
            family = "Brot",
            category = "bakery",
            variant = "Buchweizen",
            expected =
                ProductFamilyVariantCompatibilityDecision.ALLOW
        )
    }

    @Test
    fun rejectsInherentBakedVariantForBread() {

        assertDecision(
            family = "Brot",
            category = "bakery",
            variant = "Gebacken",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )
    }

    @Test
    fun rejectsGenericPlaceholdersGlobally() {

        assertDecision(
            family = "Brot",
            category = "bakery",
            variant = "Fischbasiert",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )

        assertDecision(
            family = "Backschokolade",
            category = "baking-ingredients",
            variant = "Überzogenes Lebensmittel",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )
    }

    @Test
    fun semanticProfileRejectsUnsupportedButterProcessing() {

        assertDecision(
            family = "Butter",
            category = "dairy",
            variant = "Fermentiert",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )
    }

    @Test
    fun semanticProfilesConstrainLargeFamilies() {

        assertDecision(
            family = "Mineralwasser",
            category = "beverages",
            variant = "Rind",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )

        assertDecision(
            family = "Fruchtsaft",
            category = "beverages",
            variant = "Apfel",
            expected =
                ProductFamilyVariantCompatibilityDecision.ALLOW
        )

        assertDecision(
            family = "Hähnchenfleisch",
            category = "meat",
            variant = "Filet",
            expected =
                ProductFamilyVariantCompatibilityDecision.ALLOW
        )

        assertDecision(
            family = "Einzelgewürze",
            category = "spices",
            variant = "Frittiert",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )
    }

    @Test
    fun semanticProfilesDoNotPromoteKnowledgeClaimsToCatalogIdentity() {

        assertDecision(
            family = "Müsli",
            category = "cereals",
            variant = "Proteinreich",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )

        assertDecision(
            family = "Sojadrinks",
            category = "beverages",
            variant = "Eifrei",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )

        assertDecision(
            family = "Fruchtaufstriche",
            category = "spreads",
            variant = "Zuckerreduziert",
            expected =
                ProductFamilyVariantCompatibilityDecision.REJECT
        )
    }

    private fun assertDecision(
        family: String,
        category: String,
        variant: String,
        expected: ProductFamilyVariantCompatibilityDecision
    ) {

        val definition =
            requireNotNull(
                SemanticVariantTaxonomyRegistry
                    .definitionFor(variant)
            ) {
                "Missing semantic taxonomy definition for '$variant'."
            }

        val result =
            ProductFamilyVariantCompatibilityRegistry
                .evaluate(
                    family = family,
                    category = category,
                    variant = definition
                )

        assertEquals(
            expected = expected,
            actual = result.decision,
            message =
                "$family × $variant produced ${result.decision} " +
                        "because ${result.reason}."
        )
    }
}