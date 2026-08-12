package de.shopme.testing.system.tools.knowledge.rebuild.dimensions

import de.shopme.tools.knowledge.rebuild.dimensions.runner.RunRebuildKnowledgeDimensionsAgainstCanonicalFoodCatalog
import org.junit.Test

class RunRebuildKnowledgeDimensionsAgainstCanonicalFoodCatalogTest {

    @Test
    fun rebuildKnowledgeDimensionsAgainstCanonicalFoodCatalog() {

        RunRebuildKnowledgeDimensionsAgainstCanonicalFoodCatalog.main(
            emptyArray()
        )
    }
}