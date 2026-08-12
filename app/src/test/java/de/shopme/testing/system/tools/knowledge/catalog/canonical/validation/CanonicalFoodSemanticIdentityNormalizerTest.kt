package de.shopme.testing.system.tools.knowledge.catalog.canonical.validation

import de.shopme.tools.knowledge.catalog.canonical.validation.CanonicalFoodSemanticIdentityNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalFoodSemanticIdentityNormalizerTest {

    private val normalizer =
        CanonicalFoodSemanticIdentityNormalizer()

    @Test
    fun basmatiRiceSpacingIsEquivalent() {

        assertEquals(
            normalizer.normalize(
                "Basmatireis"
            ),
            normalizer.normalize(
                "Basmati Reis"
            )
        )
    }

    @Test
    fun appleSchorleUsesCompoundIdentity() {

        assertEquals(
            normalizer.normalize(
                "Apfelschorle"
            ),
            normalizer.normalize(
                "Apfelsaft Schorle"
            )
        )
    }

    @Test
    fun vegetableLasagnaSpacingIsEquivalent() {

        assertEquals(
            normalizer.normalize(
                "Gemüselasagne"
            ),
            normalizer.normalize(
                "Gemüse Lasagne"
            )
        )
    }

    @Test
    fun croissantPluralIsEquivalent() {

        assertEquals(
            normalizer.normalize(
                "Croissant"
            ),
            normalizer.normalize(
                "Croissants"
            )
        )
    }

    @Test
    fun strawberrySingularPluralIsEquivalent() {

        assertEquals(
            normalizer.normalize(
                "Erdbeere"
            ),
            normalizer.normalize(
                "Erdbeeren"
            )
        )
    }
}