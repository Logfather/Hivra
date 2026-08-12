package de.shopme.testing.system.tools.knowledge.catalog.canonical.validation

import de.shopme.tools.knowledge.catalog.canonical.validation.CanonicalFoodSemanticIdentityNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanonicalFoodSemanticDuplicateDetectionTest {

    private val normalizer =
        CanonicalFoodSemanticIdentityNormalizer()

    @Test
    fun onlyActuallyDuplicatedSemanticKeysAreDetected() {

        val names =
            listOf(
                "Aal",
                "Apfelsaft",
                "Basmatireis",
                "Basmati Reis",
                "Cola",
                "Erdbeeren"
            )

        val duplicateKeys =
            names
                .groupBy { name ->

                    normalizer
                        .normalize(
                            name
                        )
                }
                .filterValues { group ->

                    group.size >
                            1
                }
                .keys

        assertEquals(
            1,
            duplicateKeys.size
        )

        assertTrue(
            normalizer.normalize(
                "Basmatireis"
            ) in
                    duplicateKeys
        )

        assertTrue(
            normalizer.normalize(
                "Basmati Reis"
            ) in
                    duplicateKeys
        )

        assertFalse(
            normalizer.normalize(
                "Aal"
            ) in
                    duplicateKeys
        )

        assertFalse(
            normalizer.normalize(
                "Apfelsaft"
            ) in
                    duplicateKeys
        )

        assertFalse(
            normalizer.normalize(
                "Cola"
            ) in
                    duplicateKeys
        )
    }
}