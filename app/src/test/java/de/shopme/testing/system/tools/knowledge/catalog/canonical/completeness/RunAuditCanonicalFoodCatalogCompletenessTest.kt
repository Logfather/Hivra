package de.shopme.testing.system.tools.knowledge.catalog.canonical.completeness

import de.shopme.tools.knowledge.catalog.canonical.completeness.runner.RunAuditCanonicalFoodCatalogCompleteness
import org.junit.Test

class RunAuditCanonicalFoodCatalogCompletenessTest {

    @Test
    fun auditCanonicalFoodCatalogCompletenessAgainstTaxonomyAndSources() {

        RunAuditCanonicalFoodCatalogCompleteness.main(
            emptyArray()
        )
    }
}