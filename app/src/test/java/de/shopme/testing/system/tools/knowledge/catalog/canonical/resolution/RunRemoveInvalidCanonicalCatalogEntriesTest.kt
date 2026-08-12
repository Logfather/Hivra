package de.shopme.testing.system.tools.knowledge.catalog.canonical.resolution

import de.shopme.tools.knowledge.catalog.canonical.resolution.runner.RunRemoveInvalidCanonicalCatalogEntries
import org.junit.Test

class RunRemoveInvalidCanonicalCatalogEntriesTest {

    @Test
    fun removeAllRemainingInvalidCanonicalCatalogEntries() {

        RunRemoveInvalidCanonicalCatalogEntries.main(
            emptyArray()
        )
    }
}