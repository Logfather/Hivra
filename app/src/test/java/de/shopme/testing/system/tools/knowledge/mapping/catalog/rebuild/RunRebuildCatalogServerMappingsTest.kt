package de.shopme.testing.system.tools.knowledge.mapping.catalog.rebuild

import de.shopme.tools.knowledge.mapping.catalog.rebuild.runner.RunRebuildCatalogServerMappings
import org.junit.Test

class RunRebuildCatalogServerMappingsTest {

    @Test
    fun rebuildCatalogServerMappingsAgainstCanonicalFoodCatalog() {

        RunRebuildCatalogServerMappings.main(
            emptyArray()
        )
    }
}