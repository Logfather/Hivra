package de.shopme.testing.system.tools.knowledge.catalog.taxonomy

import de.shopme.tools.knowledge.catalog.taxonomy.runner.RunGeneratePrimaryCanonicalGermanFoodTaxonomy
import org.junit.Test

class RunGeneratePrimaryCanonicalGermanFoodTaxonomyTest {

    @Test
    fun generatePrimaryCanonicalGermanFoodTaxonomy() {

        RunGeneratePrimaryCanonicalGermanFoodTaxonomy.main(
            emptyArray()
        )
    }
}