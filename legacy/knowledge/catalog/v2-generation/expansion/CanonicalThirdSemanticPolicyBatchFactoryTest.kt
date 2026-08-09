package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalThirdSemanticPolicyBatchFactoryTest {

    @Test
    fun createCompleteThirdSemanticPolicyBatch() {
        val entries =
            CanonicalThirdSemanticPolicyBatchFactory()
                .createEntries()

        assertEquals(
            EXPECTED_POLICY_COUNT,
            entries.size
        )

        assertEquals(
            EXPECTED_POLICY_COUNT,
            entries
                .map { it.identityKey }
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
            entries.map { it.familyKey }
        )
    }

    @Test
    fun referenceOnlyCanonicalPlantSpeciesValues() {
        val entries =
            CanonicalThirdSemanticPolicyBatchFactory()
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
            CanonicalThirdSemanticPolicyBatchFactory()

        assertEquals(
            factory.createEntries(),
            factory.createEntries()
        )
    }

    private companion object {
        const val EXPECTED_POLICY_COUNT =
            8

        val EXPECTED_FAMILY_KEYS =
            listOf(
                "citrus-fruit",
                "dried-fruit",
                "grapes",
                "melons",
                "pome-fruit",
                "prepared-fruit",
                "stone-fruit",
                "tropical-fruit"
            )
    }
}