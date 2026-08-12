package de.shopme.testing.system.tools.knowledge.catalog.truecanonical

import de.shopme.tools.knowledge.catalog.truecanonical.TrueCanonicalFoodNameNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals

class TrueCanonicalFoodNameNormalizerTest {

    private val normalizer =
        TrueCanonicalFoodNameNormalizer()

    @Test
    fun normalizeGermanUmlauts() {

        assertEquals(
            "aepfel",
            normalizer.normalize(
                "Äpfel"
            )
        )

        assertEquals(
            "gemueselasagne",
            normalizer.normalize(
                "Gemüselasagne"
            )
        )
    }

    @Test
    fun normalizeOtherDiacritics() {

        assertEquals(
            "bearnaise-sauce",
            normalizer.normalize(
                "Béarnaise Sauce"
            )
        )

        assertEquals(
            "jalapenos",
            normalizer.normalize(
                "Jalapeños"
            )
        )
    }
}