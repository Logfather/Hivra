package de.shopme.testing.system.tools.knowledge.catalog.semantic.variant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SemanticVariantTaxonomyRegistryTest {

    @Test
    fun classifiesKnownVariantTypes() {

        assertType(
            rawValue = "Gerste",
            expected =
                SemanticVariantType.INGREDIENT
        )

        assertType(
            rawValue = "Fermentiert",
            expected =
                SemanticVariantType.PROCESSING
        )

        assertType(
            rawValue = "Frittiert",
            expected =
                SemanticVariantType.PREPARATION
        )

        assertType(
            rawValue = "Höhlengereift",
            expected =
                SemanticVariantType.MATURATION
        )

        assertType(
            rawValue = "Gewürfelt",
            expected =
                SemanticVariantType.FORM
        )

        assertType(
            rawValue = "Cremig",
            expected =
                SemanticVariantType.TEXTURE
        )

        assertType(
            rawValue = "Proteinreich",
            expected =
                SemanticVariantType.NUTRITION_CLAIM
        )

        assertType(
            rawValue = "Glutenfrei",
            expected =
                SemanticVariantType.ALLERGEN_CLAIM
        )

        assertType(
            rawValue = "Fischbasiert",
            expected =
                SemanticVariantType.GENERIC_PLACEHOLDER
        )

        assertType(
            rawValue = "Eifrei",
            expected =
                SemanticVariantType.ALLERGEN_CLAIM
        )

        assertType(
            rawValue = "Ballaststoffreich",
            expected =
                SemanticVariantType.NUTRITION_CLAIM
        )

        assertType(
            rawValue = "Bitter",
            expected =
                SemanticVariantType.FLAVOR
        )

        assertType(
            rawValue = "Angereichert",
            expected =
                SemanticVariantType.PROCESSING
        )

        assertType(
            rawValue = "Gegart",
            expected =
                SemanticVariantType.PREPARATION
        )

        assertType(
            rawValue = "Gekühlt",
            expected =
                SemanticVariantType.STORAGE_STATE
        )

        assertType(
            rawValue = "Doppelrahmstufe",
            expected =
                SemanticVariantType.COMPOSITION
        )

        assertType(
            rawValue = "Filet",
            expected =
                SemanticVariantType.CUT
        )

        assertType(
            rawValue = "Kalb",
            expected =
                SemanticVariantType.INGREDIENT
        )
    }

    @Test
    fun lookupIsNormalizationStable() {

        val first =
            SemanticVariantTaxonomyRegistry
                .definitionFor(
                    "Höhlengereift"
                )

        val second =
            SemanticVariantTaxonomyRegistry
                .definitionFor(
                    "hohlengereift"
                )

        assertNotNull(first)
        assertNotNull(second)

        assertEquals(
            first.canonicalKey,
            second.canonicalKey
        )
    }

    @Test
    fun aliasesResolveToSameDefinition() {

        val proteinreich =
            SemanticVariantTaxonomyRegistry
                .definitionFor(
                    "Proteinreich"
                )

        val eiweissreich =
            SemanticVariantTaxonomyRegistry
                .definitionFor(
                    "Eiweißreich"
                )

        assertNotNull(proteinreich)
        assertNotNull(eiweissreich)

        assertEquals(
            proteinreich.canonicalKey,
            eiweissreich.canonicalKey
        )
    }

    @Test
    fun unknownValueIsNotSilentlyClassified() {

        assertNull(
            SemanticVariantTaxonomyRegistry
                .definitionFor(
                    "Fantasiemerkmal"
                )
        )
    }

    @Test
    fun canonicalKeysAreUnique() {

        val definitions =
            SemanticVariantTaxonomyRegistry
                .all()

        assertEquals(
            definitions.size,
            definitions
                .map { it.canonicalKey }
                .distinct()
                .size
        )

        assertTrue(
            definitions.isNotEmpty()
        )
    }

    private fun assertType(
        rawValue: String,
        expected: SemanticVariantType
    ) {

        val definition =
            SemanticVariantTaxonomyRegistry
                .definitionFor(rawValue)

        assertNotNull(
            definition,
            "Expected taxonomy definition for '$rawValue'."
        )

        assertEquals(
            expected = expected,
            actual = definition.type
        )
    }
}