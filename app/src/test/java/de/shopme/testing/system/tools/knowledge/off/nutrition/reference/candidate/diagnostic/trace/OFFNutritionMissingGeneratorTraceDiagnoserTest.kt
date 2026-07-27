package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace

import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace.OFFNutritionMissingGeneratorTraceCause
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace.OFFNutritionMissingGeneratorTraceDiagnoser
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace.OFFNutritionMissingGeneratorTraceStage
import kotlin.test.Test
import kotlin.test.assertEquals

class OFFNutritionMissingGeneratorTraceDiagnoserTest {

    @Test
    fun diagnose_identifiesFirstMissingPersistedPipelineStage() {

        val directory =
            createTempDir(
                prefix =
                    "off-missing-generator-trace-"
            )

        try {
            val rejectionReportFile =
                directory.resolve("rejections.json")

            rejectionReportFile.writeText(
                """
                {
                  "version": 1,
                  "findings": [
                    {
                      "catalogIndex": 1,
                      "catalogKey": "trout",
                      "normalizedEnglish": "trout",
                      "rawOFFProductMatchCount": 20,
                      "rawOFFProductWithUsableNutritionCount": 5,
                      "matchedRawProductNames": ["Rainbow Trout"],
                      "firstRejectionStage": "TRACE_NOT_FOUND"
                    },
                    {
                      "catalogIndex": 2,
                      "catalogKey": "skyr",
                      "normalizedEnglish": "skyr",
                      "rawOFFProductMatchCount": 10,
                      "rawOFFProductWithUsableNutritionCount": 3,
                      "matchedRawProductNames": ["Organic Skyr"],
                      "firstRejectionStage": "TRACE_NOT_FOUND"
                    },
                    {
                      "catalogIndex": 3,
                      "catalogKey": "goulash",
                      "normalizedEnglish": "goulash",
                      "rawOFFProductMatchCount": 12,
                      "rawOFFProductWithUsableNutritionCount": 4,
                      "matchedRawProductNames": ["Beef Goulash"],
                      "firstRejectionStage": "TRACE_NOT_FOUND"
                    }
                  ]
                }
                """.trimIndent()
            )

            val sourceFile =
                directory.resolve("source.json")

            sourceFile.writeText(
                """
                [
                  {
                    "canonicalId": "rainbow trout",
                    "aliases": ["Rainbow Trout"],
                    "metadata": {
                      "sourceId": "off-1",
                      "attributes": {
                        "productName": "Rainbow Trout"
                      }
                    }
                  },
                  {
                    "canonicalId": "beef goulash",
                    "aliases": ["Beef Goulash"],
                    "metadata": {
                      "sourceId": "off-2",
                      "attributes": {
                        "productName": "Beef Goulash"
                      }
                    }
                  }
                ]
                """.trimIndent()
            )

            val qualityFilteredFile =
                directory.resolve("quality-filtered.json")

            qualityFilteredFile.writeText(
                """
                [
                  {
                    "canonicalId": "rainbow trout",
                    "aliases": ["Rainbow Trout"],
                    "metadata": {
                      "sourceId": "off-1",
                      "attributes": {
                        "productName": "Rainbow Trout"
                      }
                    }
                  },
                  {
                    "canonicalId": "beef goulash",
                    "aliases": ["Beef Goulash"],
                    "metadata": {
                      "sourceId": "off-2",
                      "attributes": {
                        "productName": "Beef Goulash"
                      }
                    }
                  }
                ]
                """.trimIndent()
            )

            val deduplicatedFile =
                directory.resolve("deduplicated.json")

            deduplicatedFile.writeText(
                """
                [
                  {
                    "canonicalId": "rainbow trout",
                    "aliases": ["Rainbow Trout"],
                    "metadata": {
                      "sourceId": "off-1",
                      "attributes": {
                        "productName": "Rainbow Trout"
                      }
                    }
                  }
                ]
                """.trimIndent()
            )

            val traceFile =
                directory.resolve("traces.json")

            traceFile.writeText(
                """
                {
                  "version": 1,
                  "traces": []
                }
                """.trimIndent()
            )

            val report =
                OFFNutritionMissingGeneratorTraceDiagnoser()
                    .diagnose(
                        rejectionReportFile =
                            rejectionReportFile,
                        sourceCandidateFile =
                            sourceFile,
                        qualityFilteredCandidateFile =
                            qualityFilteredFile,
                        deduplicatedCandidateFile =
                            deduplicatedFile,
                        traceFile =
                            traceFile
                    )

            assertEquals(
                3,
                report.missingTraceFindingCount
            )

            assertEquals(
                1,
                report.countsByCause[
                    OFFNutritionMissingGeneratorTraceCause
                        .SOURCE_CANDIDATE_NOT_FOUND
                ]
            )

            assertEquals(
                1,
                report.countsByCause[
                    OFFNutritionMissingGeneratorTraceCause
                        .REMOVED_BY_DEDUPLICATION
                ]
            )

            assertEquals(
                1,
                report.countsByCause[
                    OFFNutritionMissingGeneratorTraceCause
                        .TRACE_NOT_EMITTED
                ]
            )

            val trout =
                report.findings.single { finding ->
                    finding.catalogKey == "trout"
                }

            assertEquals(
                OFFNutritionMissingGeneratorTraceStage
                    .GENERATOR_TRACE,
                trout.firstMissingStage
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}