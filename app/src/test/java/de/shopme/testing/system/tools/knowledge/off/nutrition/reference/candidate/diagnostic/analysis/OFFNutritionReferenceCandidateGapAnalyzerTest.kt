package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis

import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis.OFFNutritionReferenceCandidateGapAnalyzer
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis.OFFNutritionReferenceCandidateGapCause
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis.OFFNutritionReferenceCandidateGapPriority
import kotlin.test.Test
import kotlin.test.assertEquals

class OFFNutritionReferenceCandidateGapAnalyzerTest {

    @Test
    fun analyze_classifiesGapStagesDeterministically() {

        val directory =
            createTempDir(
                prefix =
                    "off-reference-gap-analysis-"
            )

        try {
            val sourceFile =
                directory.resolve(
                    "source-diagnostic.json"
                )

            sourceFile.writeText(
                """
                {
                  "version": 1,
                  "requestCount": 4,
                  "missingRequestCount": 4,
                  "rawOFFScannedProductCount": 4591866,
                  "findings": [
                    {
                      "catalogIndex": 3,
                      "catalogKey": "millet",
                      "normalizedEnglish": "millet",
                      "rawOFFProductMatchCount": 100,
                      "rawOFFProductWithAnyNutritionCount": 20,
                      "rawOFFProductWithUsableNutritionCount": 18,
                      "referenceCandidateMatchCount": 0,
                      "referenceAggregateMatchCount": 0,
                      "matcherCandidateMatchCount": 0,
                      "firstMissingStage": "REFERENCE_CANDIDATE",
                      "matchedRawProductNames": ["Whole millet"],
                      "reasons": [
                        "Usable OFF products exist, but no candidate matches."
                      ]
                    },
                    {
                      "catalogIndex": 2,
                      "catalogKey": "teewurst",
                      "normalizedEnglish": "teewurst",
                      "rawOFFProductMatchCount": 12,
                      "rawOFFProductWithAnyNutritionCount": 0,
                      "rawOFFProductWithUsableNutritionCount": 0,
                      "referenceCandidateMatchCount": 0,
                      "referenceAggregateMatchCount": 0,
                      "matcherCandidateMatchCount": 0,
                      "firstMissingStage": "RAW_OFF_USABLE_NUTRITION",
                      "matchedRawProductNames": ["Teewurst"],
                      "reasons": [
                        "No usable core nutrition."
                      ]
                    },
                    {
                      "catalogIndex": 1,
                      "catalogKey": "tulip bulbs",
                      "normalizedEnglish": "tulip bulbs",
                      "rawOFFProductMatchCount": 0,
                      "rawOFFProductWithAnyNutritionCount": 0,
                      "rawOFFProductWithUsableNutritionCount": 0,
                      "referenceCandidateMatchCount": 0,
                      "referenceAggregateMatchCount": 0,
                      "matcherCandidateMatchCount": 0,
                      "firstMissingStage": "RAW_OFF_PRODUCT",
                      "matchedRawProductNames": [],
                      "reasons": [
                        "No matching raw product."
                      ]
                    },
                    {
                      "catalogIndex": 0,
                      "catalogKey": "nectarine",
                      "normalizedEnglish": "nectarine",
                      "rawOFFProductMatchCount": 50,
                      "rawOFFProductWithAnyNutritionCount": 20,
                      "rawOFFProductWithUsableNutritionCount": 20,
                      "referenceCandidateMatchCount": 1,
                      "referenceAggregateMatchCount": 0,
                      "matcherCandidateMatchCount": 0,
                      "firstMissingStage": "REFERENCE_AGGREGATE",
                      "matchedRawProductNames": ["Nectarine"],
                      "reasons": [
                        "Candidate exists but no aggregate matches."
                      ]
                    }
                  ]
                }
                """.trimIndent()
            )

            val report =
                OFFNutritionReferenceCandidateGapAnalyzer()
                    .analyze(
                        sourceDiagnosticFile =
                            sourceFile
                    )

            assertEquals(
                4,
                report.analyzedFindingCount
            )

            assertEquals(
                1,
                report.referenceCandidateGapCount
            )

            assertEquals(
                1,
                report.countsByCause[
                    OFFNutritionReferenceCandidateGapCause
                        .NO_RAW_OFF_PRODUCT
                ]
            )

            assertEquals(
                1,
                report.countsByCause[
                    OFFNutritionReferenceCandidateGapCause
                        .NO_USABLE_RAW_NUTRITION
                ]
            )

            assertEquals(
                1,
                report.countsByCause[
                    OFFNutritionReferenceCandidateGapCause
                        .CANDIDATE_NOT_CREATED
                ]
            )

            assertEquals(
                1,
                report.countsByCause[
                    OFFNutritionReferenceCandidateGapCause
                        .REFERENCE_AGGREGATION_GAP
                ]
            )

            assertEquals(
                2,
                report.countsByPriority[
                    OFFNutritionReferenceCandidateGapPriority.HIGH
                ]
            )

            assertEquals(
                listOf(
                    3,
                    0,
                    1,
                    2
                ),
                report.findings.map { finding ->
                    finding.catalogIndex
                }
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}