package de.shopme.testing.system.tools.knowledge.catalog.nutrition.group

import de.shopme.tools.knowledge.mapping.catalog.nutrition.group.runner.RunDiscoverCanonicalNutritionSourceVariants
import org.junit.Test

class RunDiscoverCanonicalNutritionSourceVariantsTest {

    @Test
    fun discoverNutritionSourceVariantsForAllCanonicalProducts() {

        RunDiscoverCanonicalNutritionSourceVariants.main(
            emptyArray()
        )
    }
}
