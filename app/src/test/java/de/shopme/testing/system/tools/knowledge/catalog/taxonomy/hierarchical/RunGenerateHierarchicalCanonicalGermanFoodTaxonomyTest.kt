package de.shopme.testing.system.tools.knowledge.catalog.taxonomy.hierarchical

import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.runner.RunGenerateHierarchicalCanonicalGermanFoodTaxonomy
import org.junit.Test

class RunGenerateHierarchicalCanonicalGermanFoodTaxonomyTest {

    @Test
    fun generateHierarchicalCanonicalGermanFoodTaxonomy() {

        RunGenerateHierarchicalCanonicalGermanFoodTaxonomy.main(
            emptyArray()
        )
    }
}