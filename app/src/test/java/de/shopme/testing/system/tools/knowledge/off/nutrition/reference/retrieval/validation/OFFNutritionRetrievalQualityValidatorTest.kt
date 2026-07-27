package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.retrieval.validation

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalRequest
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievedCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.validation.OFFNutritionRetrievalQualityType
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.validation.OFFNutritionRetrievalQualityValidator
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionRetrievalQualityValidatorTest {

    @Test
    fun validate_classifiesNoCandidates() {

        val request =
            request(
                catalogIndex = 0,
                catalogKey = "nektarine",
                normalizedEnglish = "nectarine",
                candidates = emptyList()
            )

        val report =
            OFFNutritionRetrievalQualityValidator()
                .validate(
                    requests = listOf(request)
                )

        val finding =
            report.findings.single()

        assertEquals(
            OFFNutritionRetrievalQualityType.NO_CANDIDATES,
            finding.primaryType
        )

        assertEquals(
            1,
            report.requestWithoutCandidatesCount
        )
    }

    @Test
    fun validate_classifiesExactAliasMatch() {

        val request =
            request(
                catalogIndex = 0,
                catalogKey = "apfel",
                normalizedEnglish = "apple",
                candidates =
                    listOf(
                        candidate(
                            serverKey = "apple country, apples",
                            score = 1.0,
                            exactMatch = true,
                            matchedCatalogTerm = "apple",
                            matchedCandidateAlias = "apple",
                            tokenIntersectionCount = 1,
                            tokenUnionCount = 1,
                            tokenJaccard = 1.0,
                            containmentScore = 1.0
                        )
                    )
            )

        val report =
            OFFNutritionRetrievalQualityValidator()
                .validate(
                    requests = listOf(request)
                )

        assertEquals(
            OFFNutritionRetrievalQualityType.EXACT_ALIAS_MATCH,
            report.findings.single().primaryType
        )
    }

    @Test
    fun validate_detectsProcessingFormMismatch() {

        val request =
            request(
                catalogIndex = 0,
                catalogKey = "banane",
                normalizedEnglish = "banana",
                candidates =
                    listOf(
                        candidate(
                            serverKey = "banana cake",
                            score = 0.91,
                            exactMatch = false,
                            matchedCatalogTerm = "banana",
                            matchedCandidateAlias = "banana cake",
                            tokenIntersectionCount = 1,
                            tokenUnionCount = 2,
                            tokenJaccard = 0.5,
                            containmentScore = 1.0
                        )
                    )
            )

        val report =
            OFFNutritionRetrievalQualityValidator()
                .validate(
                    requests = listOf(request)
                )

        val finding =
            report.findings.single()

        assertEquals(
            OFFNutritionRetrievalQualityType.PROCESSING_FORM_MISMATCH,
            finding.primaryType
        )

        assertTrue(
            OFFNutritionRetrievalQualityType.PROCESSING_FORM_MISMATCH in
                    finding.qualityTypes
        )
    }

    @Test
    fun validate_detectsAnimalSpeciesMismatch() {

        val request =
            request(
                catalogIndex = 0,
                catalogKey = "entenbrust",
                normalizedEnglish = "duck breast",
                candidates =
                    listOf(
                        candidate(
                            serverKey = "turkey breast",
                            score = 0.44,
                            exactMatch = false,
                            matchedCatalogTerm = "duck breast",
                            matchedCandidateAlias = "turkey breast",
                            tokenIntersectionCount = 1,
                            tokenUnionCount = 3,
                            tokenJaccard = 1.0 / 3.0,
                            containmentScore = 0.5
                        )
                    )
            )

        val report =
            OFFNutritionRetrievalQualityValidator()
                .validate(
                    requests = listOf(request)
                )

        assertEquals(
            OFFNutritionRetrievalQualityType.ANIMAL_SPECIES_MISMATCH,
            report.findings.single().primaryType
        )
    }

    @Test
    fun validate_detectsVeryLowScore() {

        val request =
            request(
                catalogIndex = 0,
                catalogKey = "fenchelknolle",
                normalizedEnglish = "fennel bulb",
                candidates =
                    listOf(
                        candidate(
                            serverKey = "wild fennel pasta sauce",
                            score = 0.24,
                            exactMatch = false,
                            matchedCatalogTerm = "fennel bulb",
                            matchedCandidateAlias =
                                "wild fennel pasta sauce",
                            tokenIntersectionCount = 1,
                            tokenUnionCount = 5,
                            tokenJaccard = 0.2,
                            containmentScore = 0.5
                        )
                    )
            )

        val report =
            OFFNutritionRetrievalQualityValidator()
                .validate(
                    requests = listOf(request)
                )

        assertTrue(
            OFFNutritionRetrievalQualityType.VERY_LOW_SCORE in
                    report.findings.single().qualityTypes
        )
    }

    private fun request(
        catalogIndex: Int,
        catalogKey: String,
        normalizedEnglish: String,
        candidates: List<CatalogOFFNutritionRetrievedCandidate>
    ): CatalogOFFNutritionRetrievalRequest {

        return CatalogOFFNutritionRetrievalRequest(
            catalogIndex =
                catalogIndex,
            catalogKey =
                catalogKey,
            normalizedEnglish =
                normalizedEnglish,
            itemName =
                catalogKey,
            category =
                null,
            production =
                null,
            catalogTerms =
                listOf(
                    catalogKey,
                    normalizedEnglish
                )
                    .distinct()
                    .sorted(),
            candidates =
                candidates
        )
    }

    private fun candidate(
        serverKey: String,
        score: Double,
        exactMatch: Boolean,
        matchedCatalogTerm: String,
        matchedCandidateAlias: String,
        tokenIntersectionCount: Int,
        tokenUnionCount: Int,
        tokenJaccard: Double,
        containmentScore: Double
    ): CatalogOFFNutritionRetrievedCandidate {

        return CatalogOFFNutritionRetrievedCandidate(
            rank =
                1,
            serverArtifact =
                "nutrition.json",
            serverKey =
                serverKey,
            score =
                score,
            exactMatch =
                exactMatch,
            matchedCatalogTerm =
                matchedCatalogTerm,
            matchedCandidateAlias =
                matchedCandidateAlias,
            tokenIntersectionCount =
                tokenIntersectionCount,
            tokenUnionCount =
                tokenUnionCount,
            tokenJaccard =
                tokenJaccard,
            containmentScore =
                containmentScore,
            profileCount =
                1,
            validationStatus =
                OFFNutritionReferenceAggregateValidationStatus.ACCEPTED,
            warningCount =
                0
        )
    }
}