package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanonicalBoundedSemanticPolicyOverrideRegistryTest {

    @Test
    fun rejectPreparationStateForCanonicalChocolateFamilies() {
        val registry =
            CanonicalBoundedSemanticPolicyOverrideRegistry()

        EXPECTED_FAMILIES.forEach { familyKey ->
            val policy =
                registry.findOverride(
                    familyKey =
                        familyKey,

                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PREPARATION_STATE
                )

            requireNotNull(policy)

            assertEquals(
                familyKey,
                policy.familyKey
            )

            assertEquals(
                CanonicalProductFamilyVariantAxis
                    .PREPARATION_STATE,
                policy.axis
            )

            assertEquals(
                CanonicalFamilyAxisSemanticPolicyType
                    .NOT_APPLICABLE,
                policy.policyType
            )

            assertTrue(
                policy.allowedValues.isEmpty()
            )

            assertTrue(policy.active)

            assertEquals(
                CanonicalBoundedSemanticPolicyOverrideRegistry
                    .OVERRIDE_SOURCE,
                policy.source
            )
        }
    }

    @Test
    fun exposeExactlyFiveCurrentOverrides() {
        val overrides =
            CanonicalBoundedSemanticPolicyOverrideRegistry()
                .allOverrides()

        assertEquals(
            EXPECTED_FAMILIES,
            overrides
                .map {
                    it.familyKey
                }
                .toSet()
        )

        assertEquals(
            5,
            overrides.size
        )
    }

    @Test
    fun doNotOverrideUnrelatedFamilyAxisIdentity() {
        val result =
            CanonicalBoundedSemanticPolicyOverrideRegistry()
                .findOverride(
                    familyKey =
                        "fruit-juice",

                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PREPARATION_STATE
                )

        assertNull(result)
    }

    private companion object {

        val EXPECTED_FAMILIES =
            setOf(
                "chocolate-bars",
                "dark-chocolate",
                "filled-chocolate",
                "milk-chocolate",
                "pralines"
            )
    }
}