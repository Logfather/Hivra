package de.shopme.testing.system.tools.knowledge.mapping.catalog.rebuild

import de.shopme.tools.knowledge.mapping.catalog.rebuild.GenerateCanonicalIdentityKnowledgeEvidenceClassificationReport
import org.junit.Test
import kotlin.test.assertEquals

class RunGenerateCanonicalIdentityKnowledgeEvidenceClassificationReportTest {

    @Test
    fun generateClassificationReport() {

        val report =
            GenerateCanonicalIdentityKnowledgeEvidenceClassificationReport()
                .generate()

        assertEquals(
            25,
            report.inputRelationshipCount
        )

        assertEquals(
            25,
            report.keepIdentityCount +
                    report.reviewCount +
                    report.insufficientEvidenceCount
        )
    }
}