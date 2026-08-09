package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.combination.SemanticCombinationConstraintAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.semantic.combination.SemanticCombinationConstraintAuditReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunSemanticCombinationConstraintAuditTest {

    @Test
    fun auditSemanticCombinationConstraints() {

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
            SemanticCombinationConstraintAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile =
                        inputFile
                            .relativeTo(projectRoot)
                            .path
                )

        assertTrue(
            report.combinationEntryCount > 0,
            "Expected multi-variant entries in frozen v1 catalog."
        )

        assertTrue(
            report.rejectEntryCount > 0,
            "Expected semantically invalid variant combinations."
        )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/" +
                        "semantic-audit/" +
                        "canonical-food-catalog-" +
                        "semantic-combination-constraints.json"
            )

        SemanticCombinationConstraintAuditReportWriter()
            .write(
                report = report,
                outputFile = outputFile
            )

        println()
        println("Semantic Combination Constraint Audit")
        println("-------------------------------------")
        println(
            "Catalog entries                 : " +
                    report.catalogEntryCount
        )
        println(
            "Combination-bearing entries     : " +
                    report.combinationEntryCount
        )
        println(
            "Combination variant occurrences : " +
                    report.combinationVariantOccurrenceCount
        )

        println()
        println("Combination decisions:")
        println(
            "  ALLOW                         : " +
                    report.allowEntryCount
        )
        println(
            "  REJECT                        : " +
                    report.rejectEntryCount
        )
        println(
            "  REVIEW                        : " +
                    report.reviewEntryCount
        )

        println()
        println("By reason:")

        report
            .countsByReason
            .forEach { (reason, count) ->
                println(
                    "  ${reason.padEnd(42)} $count"
                )
            }

        println()
        println("Top rejected type combinations:")

        report
            .rejectedCombinationSignatures
            .entries
            .take(25)
            .forEach { (signature, count) ->
                println(
                    "  ${signature.padEnd(45)} $count"
                )
            }

        println()
        println("Top combination gaps:")

        report
            .reviewGaps
            .take(30)
            .forEach { gap ->
                println(
                    "  ${gap.profile.name.padEnd(28)} " +
                            "${gap.typeSignature.padEnd(40)} " +
                            gap.occurrenceCount
                )

                gap.exampleItems
                    .take(3)
                    .forEach { example ->
                        println(
                            "      $example"
                        )
                    }
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