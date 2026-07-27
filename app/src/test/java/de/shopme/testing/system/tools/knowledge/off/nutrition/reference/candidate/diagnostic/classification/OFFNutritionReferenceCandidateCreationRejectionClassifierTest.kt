package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.candidate.diagnostic.classification

import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.classification.OFFNutritionReferenceCandidateCreationRejectionClassifier
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.classification.OFFNutritionReferenceCandidateCreationRejectionStage
import kotlin.test.Test
import kotlin.test.assertEquals

class OFFNutritionReferenceCandidateCreationRejectionClassifierTest {

    @Test
    fun classify_groupsCreationFailuresByFirstRejectedGeneratorStage() {

        val directory =
            createTempDir(
                prefix =
                    "off-candidate-rejection-classifier-"
            )

        try {
            val gapFile =
                directory.resolve("gap-analysis.json")

            gapFile.writeText(
                """
                {
                  "version": 1,
                  "findings": [
                    {
                      "catalogIndex": 16,
                      "catalogKey": "stachelbeere",
                      "normalizedEnglish": "gooseberry",
                      "cause": "CANDIDATE_NOT_CREATED",
                      "rawOFFProductMatchCount": 150,
                      "rawOFFProductWithUsableNutritionCount": 22,
                      "matchedRawProductNames": [
                        "Gooseberry Jam",
                        "Stachelbeeren"
                      ]
                    },
                    {
                      "catalogIndex": 20,
                      "catalogKey": "clementine",
                      "normalizedEnglish": "clementine",
                      "cause": "CANDIDATE_NOT_CREATED",
                      "rawOFFProductMatchCount": 569,
                      "rawOFFProductWithUsableNutritionCount": 70,
                      "matchedRawProductNames": [
                        "Clementine"
                      ]
                    }
                  ]
                }
                """.trimIndent()
            )

            val traceFile =
                directory.resolve("traces.json")

            traceFile.writeText(
                """
                {
                  "version": 1,
                  "traces": [
                    {
                      "sourceProductId": "off-1",
                      "productName": "Gooseberry Jam",
                      "normalizedProductIdentities": [
                        "gooseberry jam",
                        "gooseberry"
                      ],
                      "identityAccepted": true,
                      "identityRejectionReasons": [],
                      "nutritionAccepted": true,
                      "nutritionRejectionReasons": [],
                      "referenceEligible": false,
                      "referenceEligibilityRejectionReasons": [
                        "PRODUCT_IDENTITY_TOO_SPECIFIC"
                      ],
                      "candidateCreated": false,
                      "createdCandidateId": null
                    },
                    {
                      "sourceProductId": "off-2",
                      "productName": "Stachelbeeren",
                      "normalizedProductIdentities": [
                        "stachelbeeren",
                        "gooseberry"
                      ],
                      "identityAccepted": true,
                      "identityRejectionReasons": [],
                      "nutritionAccepted": false,
                      "nutritionRejectionReasons": [
                        "MISSING_CORE_NUTRITION"
                      ],
                      "referenceEligible": false,
                      "referenceEligibilityRejectionReasons": [],
                      "candidateCreated": false,
                      "createdCandidateId": null
                    }
                  ]
                }
                """.trimIndent()
            )

            val report =
                OFFNutritionReferenceCandidateCreationRejectionClassifier()
                    .classify(
                        gapAnalysisFile =
                            gapFile,
                        traceFile =
                            traceFile
                    )

            assertEquals(
                2,
                report.candidateNotCreatedFindingCount
            )

            assertEquals(
                1,
                report.classifiedFindingCount
            )

            assertEquals(
                1,
                report.unmatchedFindingCount
            )

            assertEquals(
                2,
                report.matchedTraceCount
            )

            assertEquals(
                2,
                report.rejectedTraceCount
            )

            assertEquals(
                1,
                report.countsByFirstRejectionStage[
                    OFFNutritionReferenceCandidateCreationRejectionStage
                        .NUTRITION
                ]
            )

            assertEquals(
                1,
                report.countsByFirstRejectionStage[
                    OFFNutritionReferenceCandidateCreationRejectionStage
                        .TRACE_NOT_FOUND
                ]
            )

            assertEquals(
                1,
                report.countsByRejectionReason[
                    "MISSING_CORE_NUTRITION"
                ]
            )

            assertEquals(
                1,
                report.countsByRejectionReason[
                    "PRODUCT_IDENTITY_TOO_SPECIFIC"
                ]
            )

            assertEquals(
                listOf(
                    "stachelbeere",
                    "clementine"
                ),
                report.findings.map { finding ->
                    finding.catalogKey
                }
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}