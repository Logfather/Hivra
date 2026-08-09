package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFirstSemanticPolicyBatchFactoryTest {

    @Test
    fun createCompleteFirstSemanticPolicyBatch() {
        val entries =
            CanonicalFirstSemanticPolicyBatchFactory()
                .createEntries()

        assertEquals(
            12,
            entries.size
        )

        assertEquals(
            12,
            entries
                .map {
                    it.identityKey
                }
                .distinct()
                .size
        )

        assertTrue(
            entries.all {
                it.axis ==
                        CanonicalProductFamilyVariantAxis
                            .PLANT_SPECIES
            }
        )

        assertEquals(
            10,
            entries.count {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES
            }
        )

        assertEquals(
            2,
            entries.count {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE
            }
        )

        assertTrue(
            entries.all {
                it.complete &&
                        it.active
            }
        )

        assertEquals(
            listOf(
                "cabbage",
                "flower-vegetables",
                "fruit-vegetables",
                "leafy-vegetables",
                "mushrooms",
                "onion-vegetables",
                "root-vegetables",
                "sea-vegetables",
                "sprouts",
                "stem-vegetables",
                "tuber-vegetables",
                "vegetable-mixtures"
            ),
            entries.map {
                it.familyKey
            }
        )
    }

    @Test
    fun referenceOnlyCanonicalVariantValues() {
        val entries =
            CanonicalFirstSemanticPolicyBatchFactory()
                .createEntries()

        val result =
            CanonicalSemanticPolicyBatchVocabularyValidator()
                .validate(entries)

        assertTrue(
            result.valid,
            result.issues.joinToString(
                separator = System.lineSeparator()
            )
        )

        assertEquals(
            0,
            result.issueCount
        )
    }

    @Test
    fun createDeterministically() {
        val factory =
            CanonicalFirstSemanticPolicyBatchFactory()

        assertEquals(
            factory.createEntries(),
            factory.createEntries()
        )
    }
}