package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CanonicalProductFamilyPluralPolicyTest {

    @Test
    fun resolvesAlreadyPluralFamilies() {

        assertPlural(
            family = "Bohnen",
            expected = "Bohnen",
            expectedKind =
                CanonicalProductFamilyPluralPolicy
                    .PluralKind.ALREADY_PLURAL
        )

        assertPlural(
            family = "Fischfilets",
            expected = "Fischfilets",
            expectedKind =
                CanonicalProductFamilyPluralPolicy
                    .PluralKind.ALREADY_PLURAL
        )

        assertPlural(
            family = "Fleischalternativen",
            expected = "Fleischalternativen",
            expectedKind =
                CanonicalProductFamilyPluralPolicy
                    .PluralKind.ALREADY_PLURAL
        )

        assertPlural(
            family = "Joghurt",
            expected = "Joghurt",
            expectedKind =
                CanonicalProductFamilyPluralPolicy
                    .PluralKind.INVARIANT
        )
    }

    @Test
    fun resolvesInvariantFamilies() {

        assertPlural(
            family = "Hafer",
            expected = "Hafer",
            expectedKind =
                CanonicalProductFamilyPluralPolicy
                    .PluralKind.INVARIANT
        )

        assertPlural(
            family = "Frischfisch",
            expected = "Frischfisch",
            expectedKind =
                CanonicalProductFamilyPluralPolicy
                    .PluralKind.INVARIANT
        )

        assertPlural(
            family = "Porridge",
            expected = "Porridge",
            expectedKind =
                CanonicalProductFamilyPluralPolicy
                    .PluralKind.INVARIANT
        )
    }

    @Test
    fun resolvesExplicitGermanPlural() {

        assertPlural(
            family = "Rohwurst",
            expected = "Rohwürste",
            expectedKind =
                CanonicalProductFamilyPluralPolicy
                    .PluralKind.EXPLICIT_PLURAL
        )

        assertPlural(
            family = "Streichwurst",
            expected = "Streichwürste",
            expectedKind =
                CanonicalProductFamilyPluralPolicy
                    .PluralKind.EXPLICIT_PLURAL
        )

        assertPlural(
            family = "Fruchtnektar",
            expected = "Fruchtnektare",
            expectedKind =
                CanonicalProductFamilyPluralPolicy
                    .PluralKind.EXPLICIT_PLURAL
        )
    }

    @Test
    fun lookupIsNormalizationStable() {

        assertEquals(
            expected =
                "Rohwürste",
            actual =
                CanonicalProductFamilyPluralPolicy
                    .pluralFor(
                        "  Rohwurst "
                    )
        )
    }

    private fun assertPlural(
        family: String,
        expected: String,
        expectedKind:
        CanonicalProductFamilyPluralPolicy.PluralKind
    ) {

        val definition =
            assertNotNull(
                CanonicalProductFamilyPluralPolicy
                    .definitionFor(family)
            )

        assertEquals(
            expected = expected,
            actual = definition.plural
        )

        assertEquals(
            expected = expectedKind,
            actual = definition.kind
        )
    }
}