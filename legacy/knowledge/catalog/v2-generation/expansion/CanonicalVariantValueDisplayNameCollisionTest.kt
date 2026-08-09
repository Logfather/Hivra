package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CanonicalVariantValueDisplayNameCollisionTest {

    @Test
    fun distinguishBoiledProcessingFromCookedPreparationState() {

        val boiled =
            CanonicalVariantValuePolicy
                .valuesFor(
                    CanonicalProductFamilyVariantAxis
                        .PROCESSING_METHOD
                )
                .single { value ->
                    value.key == "boiled"
                }

        val cooked =
            CanonicalVariantValuePolicy
                .valuesFor(
                    CanonicalProductFamilyVariantAxis
                        .PREPARATION_STATE
                )
                .single { value ->
                    value.key == "cooked"
                }

        assertEquals(
            "Gekocht",
            boiled.displayName
        )

        assertEquals(
            "Gegart",
            cooked.displayName
        )

        assertNotEquals(
            boiled.displayName.lowercase(),
            cooked.displayName.lowercase()
        )
    }
}