package de.shopme.testing.system.tools.knowledge.catalog.nutrition.group

import de.shopme.tools.knowledge.mapping.catalog.nutrition.group.runner.RunValidateCanonicalNutritionSourceVariantMembership
import org.junit.Test

class RunValidateCanonicalNutritionSourceVariantMembershipTest {

    @Test
    fun validateNutritionSourceVariantGroupMembership() {

        RunValidateCanonicalNutritionSourceVariantMembership.main(
            emptyArray()
        )
    }
}