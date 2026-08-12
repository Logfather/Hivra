package de.shopme.testing.system.tools.knowledge.catalog.canonical.validation

import de.shopme.tools.knowledge.catalog.canonical.validation.runner.RunValidateBaseResolvedCanonicalFoodCatalog
import org.junit.Test

class RunValidateBaseResolvedCanonicalFoodCatalogTest {

    @Test
    fun validateBaseResolvedCanonicalFoodCatalogSemantics() {

        RunValidateBaseResolvedCanonicalFoodCatalog.main(
            emptyArray()
        )
    }
}