package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalSecondSemanticPolicyBatchFactoryTest {

    @Test
    fun createCompleteSecondSemanticPolicyBatch() {
        val entries =
            CanonicalSecondSemanticPolicyBatchFactory()
                .createEntries()

        assertEquals(
            EXPECTED_POLICY_COUNT,
            entries.size
        )

        assertEquals(
            EXPECTED_POLICY_COUNT,
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
                            .FLAVOR_PROFILE
            }
        )

        assertTrue(
            entries.all {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES
            }
        )

        assertTrue(
            entries.all {
                it.active &&
                        it.complete &&
                        it.allowedValues.isNotEmpty()
            }
        )

        assertEquals(
            EXPECTED_FAMILY_KEYS,
            entries.map {
                it.familyKey
            }
        )
    }

    @Test
    fun referenceOnlyCanonicalFlavorProfileValues() {
        val entries =
            CanonicalSecondSemanticPolicyBatchFactory()
                .createEntries()

        val result =
            CanonicalSemanticPolicyBatchVocabularyValidator()
                .validate(
                    entries =
                        entries
                )

        assertTrue(
            result.valid,
            result.issues.joinToString(
                separator =
                    System.lineSeparator()
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
            CanonicalSecondSemanticPolicyBatchFactory()

        assertEquals(
            factory.createEntries(),
            factory.createEntries()
        )
    }

    private companion object {
        const val EXPECTED_POLICY_COUNT =
            15

        val EXPECTED_FAMILY_KEYS =
            listOf(
                "cocoa-drinks",
                "coffee",
                "cola",
                "energy-drinks",
                "fruit-drinks",
                "fruit-juice",
                "fruit-nectar",
                "iced-tea",
                "lemonade",
                "malt-drinks",
                "mineral-water",
                "sports-drinks",
                "table-water",
                "tea",
                "vegetable-juice"
            )
    }
}