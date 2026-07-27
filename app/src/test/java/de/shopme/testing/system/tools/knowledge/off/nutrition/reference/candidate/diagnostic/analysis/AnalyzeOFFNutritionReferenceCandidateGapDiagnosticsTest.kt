package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis

import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis.OFFNutritionReferenceCandidateGapAnalysisReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis.OFFNutritionReferenceCandidateGapAnalyzer
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis.OFFNutritionReferenceCandidateGapPriority
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyzeOFFNutritionReferenceCandidateGapDiagnosticsTest {

    @Test
    fun analyzeOFFNutritionReferenceCandidateGapDiagnostics() {

        val projectRoot =
            resolveProjectRoot()

        val sourceDiagnosticFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "catalog-off-nutrition-source-coverage-diagnostic.json"
            )

        require(sourceDiagnosticFile.isFile) {
            "OFF nutrition source coverage diagnostic not found: " +
                    sourceDiagnosticFile.absolutePath
        }

        val report =
            OFFNutritionReferenceCandidateGapAnalyzer()
                .analyze(
                    sourceDiagnosticFile =
                        sourceDiagnosticFile
                )

        val outputFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "off-nutrition-reference-candidate-gap-analysis.json"
            )

        val writeResult =
            OFFNutritionReferenceCandidateGapAnalysisReportWriter()
                .write(
                    report =
                        report,
                    outputFile =
                        outputFile
                )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("OFF NUTRITION REFERENCE CANDIDATE GAP ANALYSIS")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "sourceRequests=" +
                    report.sourceRequestCount
        )
        println(
            "sourceMissingRequests=" +
                    report.sourceMissingRequestCount
        )
        println(
            "rawOFFScannedProducts=" +
                    report.sourceRawOFFScannedProductCount
        )
        println(
            "analyzedFindings=" +
                    report.analyzedFindingCount
        )
        println(
            "referenceCandidateGaps=" +
                    report.referenceCandidateGapCount
        )
        println(
            "countsByStage=" +
                    report.countsByFirstMissingStage
        )
        println(
            "countsByCause=" +
                    report.countsByCause
        )
        println(
            "countsByPriority=" +
                    report.countsByPriority
        )
        println(
            "highPriorityExamples=" +
                    report.findings
                        .filter { finding ->
                            finding.priority ==
                                    OFFNutritionReferenceCandidateGapPriority.HIGH
                        }
                        .take(20)
                        .map { finding ->
                            "${finding.catalogKey}:${finding.cause}"
                        }
        )
        println(
            "outputFile=" +
                    writeResult.outputFile.absolutePath
        )
        println(
            "fileSizeBytes=" +
                    writeResult.fileSizeBytes
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            report.sourceMissingRequestCount,
            report.analyzedFindingCount
        )

        assertEquals(
            report.analyzedFindingCount,
            writeResult.findingCount
        )

        assertTrue(
            report.referenceCandidateGapCount > 0
        )

        assertTrue(
            outputFile.isFile
        )

        assertTrue(
            outputFile.length() > 0L
        )

        assertEquals(
            outputFile.length(),
            writeResult.fileSizeBytes
        )
    }

    private fun resolveProjectRoot(): File {

        val workingDirectory =
            File(
                System.getProperty("user.dir")
            ).absoluteFile

        return generateSequence(
            seed =
                workingDirectory
        ) { directory ->
            directory.parentFile
        }
            .firstOrNull { directory ->
                directory.resolve("settings.gradle.kts").isFile ||
                        directory.resolve("settings.gradle").isFile
            }
            ?.canonicalFile
            ?: error(
                "Could not resolve project root from: " +
                        workingDirectory.absolutePath
            )
    }
}