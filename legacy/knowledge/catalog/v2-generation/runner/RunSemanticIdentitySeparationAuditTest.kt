package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.SemanticIdentitySeparationAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.SemanticIdentitySeparationAuditReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunSemanticIdentitySeparationAuditTest {

    @Test
    fun auditIdentitySeparation() {

        val projectRoot =
            resolveProjectRoot()

        val inputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/final/" +
                        "canonical-food-catalog-final-v1-" +
                        "f6bbdd7fbd37f3cb.json"
            )

        require(inputFile.isFile) {
            "Final canonical catalog release not found: " +
                    inputFile.absolutePath
        }

        val catalog =
            JsonParser
                .parseString(
                    inputFile.readText()
                )
                .asJsonArray

        assertEquals(
            expected = 10354,
            actual = catalog.size()
        )

        val report =
            SemanticIdentitySeparationAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile =
                        inputFile
                            .relativeTo(projectRoot)
                            .path
                )

        assertEquals(
            expected = 6935,
            actual =
                report.variantBearingEntryCount,
            message =
                "Identity audit must cover every variant-bearing " +
                        "entry from the frozen v1 catalog."
        )

        assertTrue(
            report.knowledgeOnlyOccurrenceCount > 0
        )

        assertTrue(
            report.entriesWithKnowledgeOnlyVariants > 0
        )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/" +
                        "semantic-audit/" +
                        "canonical-food-catalog-" +
                        "identity-separation.json"
            )

        SemanticIdentitySeparationAuditReportWriter()
            .write(
                report = report,
                outputFile = outputFile
            )

        println()
        println("Semantic Identity Separation Audit")
        println("----------------------------------")
        println(
            "Catalog entries                    : " +
                    report.catalogEntryCount
        )
        println(
            "Variant-bearing entries            : " +
                    report.variantBearingEntryCount
        )

        println()
        println("Variant occurrences:")
        println(
            "  KNOWLEDGE_ONLY                   : " +
                    report.knowledgeOnlyOccurrenceCount
        )
        println(
            "  IDENTITY_ALLOWED                : " +
                    report.identityAllowedOccurrenceCount
        )
        println(
            "  REVIEW                          : " +
                    report.reviewOccurrenceCount
        )

        println()
        println("Affected catalog entries:")
        println(
            "  Entries with knowledge attrs    : " +
                    report.entriesWithKnowledgeOnlyVariants
        )
        println(
            "  Pure knowledge-attribute items  : " +
                    report.pureKnowledgeAttributeEntryCount
        )
        println(
            "  Mixed identity + knowledge      : " +
                    report.mixedIdentityAndKnowledgeEntryCount
        )

        println()
        println("Knowledge-only occurrences by type:")

        report
            .countsByType
            .forEach { (type, count) ->
                println(
                    "  ${type.padEnd(28)} $count"
                )
            }

        println()
        println("Projected knowledge dimensions:")

        report
            .countsByKnowledgeDimension
            .forEach { (dimension, count) ->
                println(
                    "  ${dimension.padEnd(28)} $count"
                )
            }

        println()
        println("Top projected identity collisions:")

        report
            .projectedBaseIdentityCollisions
            .entries
            .take(30)
            .forEach { (identity, count) ->
                println(
                    "  ${identity.padEnd(45)} $count"
                )
            }

        println()
        println(
            "Report: " +
                    outputFile
                        .relativeTo(projectRoot)
                        .path
        )
    }

    private fun resolveProjectRoot(): File {

        val current =
            File(".")
                .canonicalFile

        return listOfNotNull(
            current,
            current.parentFile
        )
            .firstOrNull { candidate ->
                File(
                    candidate,
                    "data/generated/knowledge/catalog/final"
                ).isDirectory
            }
            ?: error(
                "Could not resolve ShopMe project root from: " +
                        current.absolutePath
            )
    }
}