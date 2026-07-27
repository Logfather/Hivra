package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace

import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace.OFFNutritionMissingGeneratorTraceDiagnoser
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace.OFFNutritionMissingGeneratorTraceReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiagnoseMissingOFFNutritionGeneratorTracesTest {

    @Test
    fun diagnoseMissingOFFNutritionGeneratorTraces() {

        val projectRoot =
            resolveProjectRoot()

        val outputDirectory =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition"
            )

        val rejectionReportFile =
            outputDirectory.resolve(
                "off-nutrition-reference-candidate-" +
                        "creation-rejections.json"
            )

        val sourceCandidateFile =
            outputDirectory.resolve(
                "nutrition-reference-candidates.json"
            )

        val qualityFilteredCandidateFile =
            outputDirectory.resolve(
                "nutrition-reference-candidates.quality-filtered.json"
            )

        val deduplicatedCandidateFile =
            outputDirectory.resolve(
                "nutrition-reference-candidates.deduplicated.json"
            )

        val traceFile =
            outputDirectory.resolve(
                "nutrition-reference-candidate-traces.json"
            )

        listOf(
            rejectionReportFile,
            sourceCandidateFile,
            qualityFilteredCandidateFile,
            deduplicatedCandidateFile,
            traceFile
        ).forEach { file ->
            require(file.isFile) {
                "Required OFF nutrition diagnostic input not found: " +
                        file.absolutePath
            }
        }

        val report =
            OFFNutritionMissingGeneratorTraceDiagnoser()
                .diagnose(
                    rejectionReportFile =
                        rejectionReportFile,
                    sourceCandidateFile =
                        sourceCandidateFile,
                    qualityFilteredCandidateFile =
                        qualityFilteredCandidateFile,
                    deduplicatedCandidateFile =
                        deduplicatedCandidateFile,
                    traceFile =
                        traceFile
                )

        val reportFile =
            outputDirectory.resolve(
                "off-nutrition-missing-generator-" +
                        "trace-diagnostic.json"
            )

        OFFNutritionMissingGeneratorTraceReportWriter()
            .write(
                report =
                    report,
                outputFile =
                    reportFile
            )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("OFF MISSING GENERATOR TRACE DIAGNOSTIC")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "missingTraceFindings=" +
                    report.missingTraceFindingCount
        )
        println(
            "countsByFirstMissingStage=" +
                    report.countsByFirstMissingStage
        )
        println(
            "countsByCause=" +
                    report.countsByCause
        )
        println(
            "sourceCandidateMatched=" +
                    report.sourceCandidateMatchedFindingCount
        )
        println(
            "qualityFilteredMatched=" +
                    report.qualityFilteredCandidateMatchedFindingCount
        )
        println(
            "deduplicatedMatched=" +
                    report.deduplicatedCandidateMatchedFindingCount
        )
        println(
            "generatorTraceMatched=" +
                    report.generatorTraceMatchedFindingCount
        )
        println(
            "reportFile=" +
                    reportFile.absolutePath
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            EXPECTED_MISSING_TRACE_FINDING_COUNT,
            report.missingTraceFindingCount
        )

        assertEquals(
            EXPECTED_MISSING_TRACE_FINDING_COUNT,
            report.diagnosedFindingCount
        )

        assertTrue(report.countsByCause.isNotEmpty())
        assertTrue(reportFile.isFile)
        assertTrue(reportFile.length() > 0L)
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

        const val EXPECTED_MISSING_TRACE_FINDING_COUNT =
            38
    }
}