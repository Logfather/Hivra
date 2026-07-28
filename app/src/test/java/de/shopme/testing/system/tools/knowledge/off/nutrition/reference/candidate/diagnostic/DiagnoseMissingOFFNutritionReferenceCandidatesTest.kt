package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.candidate.diagnostic

import com.google.gson.Gson
import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.CollectingOFFNutritionReferenceCandidateTraceSink
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.MissingOFFNutritionReferenceCandidateDiagnoser
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.MissingOFFNutritionReferenceCandidateReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.OFFNutritionReferenceCandidateTraceSink
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.OFFNutritionReferenceCandidateTraceWriter
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionCoverageAliasDatasetReader
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionSourceCoverageReport
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiagnoseMissingOFFNutritionReferenceCandidatesTest {

    @Test
    fun diagnoseMissingOFFNutritionReferenceCandidates() {

        val projectRoot =
            resolveProjectRoot()

        val sourceCoverageReportFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "catalog-off-nutrition-source-coverage-diagnostic.json"
            )

        val persistedCandidateFile =
            resolveExistingFile(
                projectRoot = projectRoot,
                paths =
                    listOf(
                        "data/generated/knowledge/off/nutrition/" +
                                "nutrition-reference-candidates.json",
                        "data/generated/knowledge/off/nutrition/" +
                                "nutrition-reference-candidates.filtered.json",
                        "data/generated/knowledge/off/nutrition/" +
                                "nutrition-reference-candidates.deduplicated.json"
                    ),
                description =
                    "Persisted OFF nutrition reference candidates"
            )

        val traceFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "nutrition-reference-candidate-traces.json"
            )

        val reportFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "catalog-off-nutrition-missing-reference-candidate-report.json"
            )

        val sourceCoverageReport =
            readSourceCoverageReport(
                inputFile =
                    sourceCoverageReportFile
            )

        val collectingTraceSink =
            CollectingOFFNutritionReferenceCandidateTraceSink()

        runProductiveReferenceCandidateGeneration(
            projectRoot =
                projectRoot,
            traceSink =
                collectingTraceSink
        )

        val traces =
            collectingTraceSink.traces()

        OFFNutritionReferenceCandidateTraceWriter()
            .write(
                traces =
                    traces,
                outputFile =
                    traceFile
            )

        val persistedCandidates =
            OFFNutritionCoverageAliasDatasetReader()
                .read(
                    inputFile =
                        persistedCandidateFile
                )

        val report =
            MissingOFFNutritionReferenceCandidateDiagnoser()
                .diagnose(
                    requestCount =
                        sourceCoverageReport.requestCount,
                    sourceCoverageFindings =
                        sourceCoverageReport.findings,
                    traces =
                        traces,
                    persistedCandidates =
                        persistedCandidates
                )

        MissingOFFNutritionReferenceCandidateReportWriter()
            .write(
                report =
                    report,
                outputFile =
                    reportFile
            )

        assertEquals(
            expected =
                68,
            actual =
                report.referenceCandidateGapCount
        )

        assertEquals(
            expected =
                report.referenceCandidateGapCount,
            actual =
                report.countsByFirstMissingStage.values.sum()
        )

        assertEquals(
            report.referenceCandidateGapCount,
            report.findings.size
        )

        assertEquals(
            traces.size,
            report.traceCount
        )

        assertTrue(
            report.findings ==
                    report.findings.sortedBy { it.catalogIndex }
        )

        assertTrue(
            actual =
                traces.isNotEmpty(),
            message =
                "The productive reference candidate generation produced no diagnostic traces."
        )

        assertTrue(
            actual =
                traceFile.isFile,
            message =
                "Trace file was not written: ${traceFile.absolutePath}"
        )

        assertTrue(
            actual =
                reportFile.isFile,
            message =
                "Diagnostic report was not written: ${reportFile.absolutePath}"
        )

        printReportSummary(
            report =
                report,
            traceFile =
                traceFile,
            reportFile =
                reportFile
        )
    }

    private fun readSourceCoverageReport(
        inputFile: File
    ): OFFNutritionSourceCoverageReport {

        require(inputFile.isFile) {
            "Source coverage report not found: ${inputFile.absolutePath}"
        }

        return inputFile
            .reader(Charsets.UTF_8)
            .use { reader ->
                Gson().fromJson(
                    reader,
                    OFFNutritionSourceCoverageReport::class.java
                )
            }
    }

    private fun runProductiveReferenceCandidateGeneration(
        projectRoot: File,
        traceSink: OFFNutritionReferenceCandidateTraceSink
    ) {

        require(projectRoot.isDirectory) {
            "Project root does not exist: ${projectRoot.absolutePath}"
        }

        val inputFile =
            projectRoot.resolve(
                "data/generated/openfoodfacts/" +
                        "openfoodfacts-products.slim.jsonl.gz"
            )

        require(inputFile.isFile) {
            "OFF slim dump not found: ${inputFile.absolutePath}"
        }

        val extractedCandidates =
            OFFCandidateExtractor()
                .extract(
                    file =
                        inputFile,
                    maxCandidates =
                        MAX_CANDIDATES
                )

        require(extractedCandidates.isNotEmpty()) {
            "OFF candidate extraction returned no candidates."
        }

        val generationResult =
            OFFNutritionReferenceCandidateGenerator(
                traceSink =
                    traceSink
            )
                .generate(
                    candidates =
                        extractedCandidates
                )

        require(
            generationResult.inputCandidateCount ==
                    extractedCandidates.size
        ) {
            "Generator input count does not match extracted candidate count: " +
                    "extracted=${extractedCandidates.size}, " +
                    "generatorInput=${generationResult.inputCandidateCount}."
        }

        require(generationResult.generatedCandidateCount > 0) {
            "Productive OFF nutrition reference candidate generation " +
                    "produced no candidates."
        }

        println()
        println("OFF reference candidate diagnostic generation:")
        println(
            "  extractedCandidates=" +
                    extractedCandidates.size
        )
        println(
            "  generatedCandidates=" +
                    generationResult.generatedCandidateCount
        )
        println(
            "  skippedWithoutNutrition=" +
                    generationResult.skippedWithoutNutritionCount
        )
        println(
            "  skippedInvalidIdentity=" +
                    generationResult.skippedInvalidIdentityCount
        )
        println(
            "  skippedInvalidNutritionPayload=" +
                    generationResult.skippedInvalidNutritionPayloadCount
        )
    }

    private fun resolveExistingFile(
        projectRoot: File,
        paths: List<String>,
        description: String
    ): File {

        return paths
            .asSequence()
            .map(projectRoot::resolve)
            .firstOrNull(File::isFile)
            ?: error(
                buildString {
                    append(description)
                    appendLine(" not found. Checked:")

                    paths.forEach { path ->
                        append("  - ")
                        appendLine(
                            projectRoot
                                .resolve(path)
                                .absolutePath
                        )
                    }
                }.trimEnd()
            )
    }

    private fun resolveProjectRoot(): File {

        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                ) {
                    "System property user.dir is not available."
                }
            ).absoluteFile

        return generateSequence(
            seed =
                workingDirectory,
            nextFunction = { directory ->
                directory.parentFile
            }
        )
            .take(8)
            .firstOrNull { directory ->
                directory
                    .resolve("settings.gradle.kts")
                    .isFile ||
                        directory
                            .resolve("settings.gradle")
                            .isFile
            }
            ?: error(
                "Could not resolve project root from " +
                        workingDirectory.absolutePath
            )
    }

    private fun printReportSummary(
        report:
        de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.MissingOFFNutritionReferenceCandidateReport,
        traceFile: File,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("MISSING OFF NUTRITION REFERENCE CANDIDATES")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Requests: ${report.requestCount}")
        println(
            "Reference candidate gaps: " +
                    report.referenceCandidateGapCount
        )
        println("Traces: ${report.traceCount}")
        println(
            "Counts by first missing stage: " +
                    report.countsByFirstMissingStage
        )
        println(
            "Identity rejection reasons: " +
                    report.countsByIdentityRejectionReason
        )
        println(
            "Nutrition rejection reasons: " +
                    report.countsByNutritionRejectionReason
        )
        println(
            "Eligibility rejection reasons: " +
                    report.countsByReferenceEligibilityRejectionReason
        )
        println("Trace file: ${traceFile.absolutePath}")
        println("Report: ${reportFile.absolutePath}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private companion object {

        const val MAX_CANDIDATES =
            50_000
    }
}