package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.candidate.diagnostic

import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.MissingOFFNutritionReferenceCandidateDiagnoser
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.MissingOFFNutritionReferenceCandidateStage
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.OFFNutritionReferenceCandidateTrace
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionSourceCoverageFinding
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionSourceCoverageStage
import kotlin.test.Test
import kotlin.test.assertEquals

class MissingOFFNutritionReferenceCandidateDiagnoserTest {

    @Test
    fun diagnose_identifiesReferenceEligibilityRejection() {

        val coverageFinding =
            OFFNutritionSourceCoverageFinding(
                catalogIndex =
                    16,
                catalogKey =
                    "stachelbeere",
                normalizedEnglish =
                    "gooseberry",
                retrievalTerms =
                    listOf(
                        "gooseberry",
                        "stachelbeere"
                    ),
                rawOFFProductMatchCount =
                    10,
                rawOFFProductWithAnyNutritionCount =
                    5,
                rawOFFProductWithUsableNutritionCount =
                    4,
                matchedRawProductIds =
                    emptyList(),
                matchedRawProductWithUsableNutritionIds =
                    emptyList(),
                referenceCandidateMatchCount =
                    0,
                referenceAggregateMatchCount =
                    0,
                matcherCandidateMatchCount =
                    0,
                firstMissingStage =
                    OFFNutritionSourceCoverageStage.REFERENCE_CANDIDATE,
                matchedRawProductNames =
                    listOf(
                        "Gooseberry"
                    ),
                matchedReferenceCandidateAliases =
                    emptyList(),
                matchedReferenceAggregateAliases =
                    emptyList(),
                matchedMatcherCandidateAliases =
                    emptyList(),
                reasons =
                    listOf(
                        "Usable OFF products exist, but no canonical nutrition reference candidate matches."
                    )
            )

        val trace =
            OFFNutritionReferenceCandidateTrace(
                sourceProductId =
                    "123",
                productName =
                    "Gooseberry",
                normalizedProductIdentities =
                    listOf(
                        "gooseberry"
                    ),
                identityAccepted =
                    true,
                identityRejectionReasons =
                    emptyList(),
                nutritionAccepted =
                    true,
                nutritionRejectionReasons =
                    emptyList(),
                referenceEligible =
                    false,
                referenceEligibilityRejectionReasons =
                    listOf(
                        "PRODUCT_IS_NOT_SINGLE_INGREDIENT"
                    ),
                candidateCreated =
                    false,
                createdCandidateId =
                    null
            )

        val report =
            MissingOFFNutritionReferenceCandidateDiagnoser()
                .diagnose(
                    requestCount =
                        3_649,
                    sourceCoverageFindings =
                        listOf(
                            coverageFinding
                        ),
                    traces =
                        listOf(
                            trace
                        ),
                    persistedCandidates =
                        emptyList()
                )

        assertEquals(
            3_649,
            report.requestCount
        )

        assertEquals(
            1,
            report.referenceCandidateGapCount
        )

        assertEquals(
            1,
            report.traceCount
        )

        assertEquals(
            mapOf(
                MissingOFFNutritionReferenceCandidateStage
                    .REFERENCE_ELIGIBILITY_REJECTED to 1
            ),
            report.countsByFirstMissingStage
        )

        assertEquals(
            mapOf(
                "PRODUCT_IS_NOT_SINGLE_INGREDIENT" to 1
            ),
            report.countsByReferenceEligibilityRejectionReason
        )

        val finding =
            report.findings.single()

        assertEquals(
            16,
            finding.catalogIndex
        )

        assertEquals(
            "stachelbeere",
            finding.catalogKey
        )

        assertEquals(
            listOf(
                "gooseberry",
                "stachelbeere"
            ),
            finding.retrievalTerms
        )

        assertEquals(
            1,
            finding.matchingTraceCount
        )

        assertEquals(
            1,
            finding.identityAcceptedCount
        )

        assertEquals(
            1,
            finding.nutritionAcceptedCount
        )

        assertEquals(
            0,
            finding.referenceEligibleCount
        )

        assertEquals(
            0,
            finding.candidateCreatedCount
        )

        assertEquals(
            0,
            finding.candidatePersistedCount
        )

        assertEquals(
            MissingOFFNutritionReferenceCandidateStage
                .REFERENCE_ELIGIBILITY_REJECTED,
            finding.firstMissingStage
        )

        assertEquals(
            mapOf(
                "PRODUCT_IS_NOT_SINGLE_INGREDIENT" to 1
            ),
            finding
                .countsByReferenceEligibilityRejectionReason
        )

        assertEquals(
            listOf(
                "Gooseberry"
            ),
            finding.matchedProductNames
        )

        assertEquals(
            emptyList(),
            finding.createdCandidateIds
        )

        assertEquals(
            emptyList(),
            finding.persistedCandidateIds
        )
    }
}