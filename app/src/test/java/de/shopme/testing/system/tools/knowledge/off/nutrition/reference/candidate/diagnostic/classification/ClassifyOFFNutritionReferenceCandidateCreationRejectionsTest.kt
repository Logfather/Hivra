package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.candidate.diagnostic.classification

import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.classification.OFFNutritionReferenceCandidateCreationRejectionClassifier
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.classification.OFFNutritionReferenceCandidateCreationRejectionReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassifyOFFNutritionReferenceCandidateCreationRejectionsTest {

    @Test
    fun classifyOFFNutritionReferenceCandidateCreationRejections() {

        val projectRoot =
            resolveProjectRoot()

        val outputDirectory =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition"
            )

        val gapAnalysisFile =
            outputDirectory.resolve(
                "off-nutrition-reference-candidate-gap-analysis.json"
            )

        require(gapAnalysisFile.isFile) {
            "OFF reference candidate gap analysis not found: " +
                    gapAnalysisFile.absolutePath
        }

        val traceFile =
            resolveTraceFile(
                outputDirectory =
                    outputDirectory
            )

        val report =
            OFFNutritionReferenceCandidateCreationRejectionClassifier()
                .classify(
                    gapAnalysisFile =
                        gapAnalysisFile,
                    traceFile =
                        traceFile
                )

        val reportFile =
            outputDirectory.resolve(
                "off-nutrition-reference-candidate-" +
                        "creation-rejections.json"
            )

        OFFNutritionReferenceCandidateCreationRejectionReportWriter()
            .write(
                report =
                    report,
                outputFile =
                    reportFile
            )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("OFF REFERENCE CANDIDATE CREATION REJECTIONS")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "candidateNotCreatedFindings=" +
                    report.candidateNotCreatedFindingCount
        )
        println(
            "classifiedFindings=" +
                    report.classifiedFindingCount
        )
        println(
            "unmatchedFindings=" +
                    report.unmatchedFindingCount
        )
        println(
            "matchedTraces=" +
                    report.matchedTraceCount
        )
        println(
            "rejectedTraces=" +
                    report.rejectedTraceCount
        )
        println(
            "createdTraces=" +
                    report.createdTraceCount
        )
        println(
            "countsByFirstRejectionStage=" +
                    report.countsByFirstRejectionStage
        )
        println(
            "countsByRejectionStage=" +
                    report.countsByRejectionStage
        )
        println(
            "countsByRejectionReason=" +
                    report.countsByRejectionReason
        )
        println(
            "reportFile=" +
                    reportFile.absolutePath
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            EXPECTED_CANDIDATE_NOT_CREATED_FINDING_COUNT,
            report.candidateNotCreatedFindingCount
        )

        assertTrue(
            report.classifiedFindingCount > 0,
            "At least one candidate creation gap must match a generator trace."
        )

        assertTrue(
            report.matchedTraceCount > 0,
            "At least one generator trace must be matched."
        )

        assertTrue(reportFile.isFile)
        assertTrue(reportFile.length() > 0L)
    }

    private fun resolveTraceFile(
        outputDirectory: File
    ): File {

        val candidates =
            listOf(
                "off-nutrition-reference-candidate-traces.json",
                "nutrition-reference-candidate-traces.json",
                "off-nutrition-reference-candidate-trace.json"
            )
                .map(outputDirectory::resolve)

        return candidates
            .firstOrNull(File::isFile)
            ?: error(
                buildString {
                    appendLine(
                        "OFF nutrition reference candidate trace file " +
                                "not found. Checked:"
                    )

                    candidates.forEach { file ->
                        appendLine(
                            "  - ${file.absolutePath}"
                        )
                    }
                }
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

    private companion object {

        const val EXPECTED_CANDIDATE_NOT_CREATED_FINDING_COUNT =
            68
    }
}