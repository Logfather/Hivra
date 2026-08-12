package de.shopme.testing.system.tools.knowledge.catalog.canonical.rebuild

import de.shopme.tools.knowledge.catalog.canonical.rebuild.runner.RunRebuildCanonicalFoodCatalog
import org.junit.Test

class RunRebuildCanonicalFoodCatalogTest {

    @Test
    fun rebuildCanonicalFoodCatalogIntoIdentityModel() {

        RunRebuildCanonicalFoodCatalog.main(
            emptyArray()
        )
    }
}