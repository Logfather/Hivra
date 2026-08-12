package de.shopme.testing.system.tools.knowledge.mapping.catalog.rebuild

import de.shopme.tools.knowledge.mapping.catalog.rebuild.runner.RunGenerateCatalogServerMatchReports
import org.junit.Test

class RunGenerateCatalogServerMatchReportsTest {

    @Test
    fun generateMatchReportsForUnresolvedCanonicalCatalogMappings() {

        RunGenerateCatalogServerMatchReports.main(
            emptyArray()
        )
    }
}