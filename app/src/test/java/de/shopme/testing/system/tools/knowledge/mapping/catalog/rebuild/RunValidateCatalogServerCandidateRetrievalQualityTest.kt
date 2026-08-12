package de.shopme.testing.system.tools.knowledge.mapping.catalog.rebuild

import de.shopme.tools.knowledge.mapping.catalog.rebuild.runner.RunValidateCatalogServerCandidateRetrievalQuality
import org.junit.Test

class RunValidateCatalogServerCandidateRetrievalQualityTest {

    @Test
    fun validateCandidateRecallAndQuality() {

        RunValidateCatalogServerCandidateRetrievalQuality.main(
            emptyArray()
        )
    }
}