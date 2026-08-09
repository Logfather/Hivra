package de.shopme.testing.system.tools.knowledge.catalog.review.analysis.updated

import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewBacklogAnalysisResult
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewClassificationAnalysis
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewResolutionPriority
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewResolutionRecommendation
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewAutomationAssessment
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogUpdatedReviewAnalyzerTest {

    private val analyzer =
        CatalogUpdatedReviewAnalyzer()

    @Test
    fun confirmCompletedSingularPluralResolver() {
        val previousAnalysis = analysisResult(
            classificationAnalysis(
                classification =
                    CatalogReviewBacklogClassification
                        .SINGULAR_PLURAL_VARIANT,
                entryCount = 47,
                deterministicCount = 47,
                priority =
                    CatalogReviewResolutionPriority.HIGH,
                recommendation =
                    CatalogReviewResolutionRecommendation
                        .IMPLEMENT_SINGULAR_PLURAL_RESOLVER
            )
        )

        val currentAnalysis = analysisResult(
            classificationAnalysis(
                classification =
                    CatalogReviewBacklogClassification
                        .TYPO_VARIANT,
                entryCount = 42,
                deterministicCount = 42,
                priority =
                    CatalogReviewResolutionPriority.HIGH,
                recommendation =
                    CatalogReviewResolutionRecommendation
                        .IMPLEMENT_TYPO_VARIANT_RESOLVER
            )
        )

        val result = analyzer.analyze(
            previousAnalysis = previousAnalysis,
            currentAnalysis = currentAnalysis,
            implementedRecommendation =
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_SINGULAR_PLURAL_RESOLVER
        )

        assertEquals(
            CatalogUpdatedReportStatus
                .RESOLVER_EFFECT_CONFIRMED,
            result.resolverEffectAssessment.status
        )

        assertEquals(
            0,
            result.resolverEffectAssessment
                .remainingEntryCount
        )

        assertEquals(
            CatalogReviewResolutionRecommendation
                .IMPLEMENT_TYPO_VARIANT_RESOLVER,
            result.nextRecommendedImplementation
        )

        assertTrue(result.implementationReady)
        assertTrue(result.implementationBlockers.isEmpty())
    }

    @Test
    fun detectPartiallyEffectiveResolver() {
        val previousAnalysis = analysisResult(
            classificationAnalysis(
                classification =
                    CatalogReviewBacklogClassification
                        .SINGULAR_PLURAL_VARIANT,
                entryCount = 47,
                deterministicCount = 47,
                priority =
                    CatalogReviewResolutionPriority.HIGH,
                recommendation =
                    CatalogReviewResolutionRecommendation
                        .IMPLEMENT_SINGULAR_PLURAL_RESOLVER
            )
        )

        val currentAnalysis = analysisResult(
            classificationAnalysis(
                classification =
                    CatalogReviewBacklogClassification
                        .SINGULAR_PLURAL_VARIANT,
                entryCount = 25,
                deterministicCount = 25,
                priority =
                    CatalogReviewResolutionPriority.HIGH,
                recommendation =
                    CatalogReviewResolutionRecommendation
                        .IMPLEMENT_SINGULAR_PLURAL_RESOLVER
            )
        )

        val result = analyzer.analyze(
            previousAnalysis = previousAnalysis,
            currentAnalysis = currentAnalysis,
            implementedRecommendation =
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_SINGULAR_PLURAL_RESOLVER
        )

        assertEquals(
            CatalogUpdatedReportStatus
                .RESOLVER_PARTIALLY_EFFECTIVE,
            result.resolverEffectAssessment.status
        )

        assertEquals(
            22,
            result.resolverEffectAssessment.resolvedEntryCount
        )

        assertEquals(
            22,
            result.resolverEffectAssessment
                .resolvedPotentiallyDeterministicCount
        )

        assertEquals(
            CatalogUpdatedReportStatus
                .RESOLVER_PARTIALLY_EFFECTIVE,
            result.resolverEffectAssessment.status
        )

        assertEquals(
            25,
            result.resolverEffectAssessment.remainingEntryCount
        )
        assertEquals(
            25,
            result.resolverEffectAssessment
                .remainingPotentiallyDeterministicCount
        )
    }

    @Test
    fun blockResolverWithoutDeterministicCandidates() {
        val currentAnalysis = analysisResult(
            classificationAnalysis(
                classification =
                    CatalogReviewBacklogClassification
                        .SALES_FORM_VARIANT,
                entryCount = 10,
                deterministicCount = 0,
                manualCount = 10,
                priority =
                    CatalogReviewResolutionPriority.LOW,
                recommendation =
                    CatalogReviewResolutionRecommendation
                        .IMPLEMENT_SALES_FORM_VARIANT_CLASSIFIER
            )
        )

        val result = analyzer.analyze(
            previousAnalysis = currentAnalysis,
            currentAnalysis = currentAnalysis,
            implementedRecommendation =
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_SINGULAR_PLURAL_RESOLVER
        )

        assertFalse(result.implementationReady)

        assertTrue(
            result.implementationBlockers.any {
                "no potentially deterministic entries" in
                        it.lowercase()
            }
        )
    }

    @Test
    fun analyzeDeterministically() {
        val currentAnalysis = analysisResult(
            classificationAnalysis(
                classification =
                    CatalogReviewBacklogClassification
                        .TYPO_VARIANT,
                entryCount = 4,
                deterministicCount = 4,
                priority =
                    CatalogReviewResolutionPriority.MEDIUM,
                recommendation =
                    CatalogReviewResolutionRecommendation
                        .IMPLEMENT_TYPO_VARIANT_RESOLVER
            )
        )

        val first = analyzer.analyze(
            previousAnalysis = currentAnalysis,
            currentAnalysis = currentAnalysis,
            implementedRecommendation =
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_SINGULAR_PLURAL_RESOLVER
        )

        val second = analyzer.analyze(
            previousAnalysis = currentAnalysis,
            currentAnalysis = currentAnalysis,
            implementedRecommendation =
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_SINGULAR_PLURAL_RESOLVER
        )

        assertEquals(first, second)
    }

    @Test
    fun reportMissingEffectBaseline() {
        val currentAnalysis = analysisResult(
            classificationAnalysis(
                classification =
                    CatalogReviewBacklogClassification
                        .TYPO_VARIANT,
                entryCount = 42,
                deterministicCount = 42,
                priority =
                    CatalogReviewResolutionPriority.HIGH,
                recommendation =
                    CatalogReviewResolutionRecommendation
                        .IMPLEMENT_TYPO_VARIANT_RESOLVER
            )
        )

        val result = analyzer.analyze(
            previousAnalysis = null,
            currentAnalysis = currentAnalysis,
            implementedRecommendation =
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_SINGULAR_PLURAL_RESOLVER
        )

        assertEquals(
            CatalogUpdatedReportStatus
                .EFFECT_BASELINE_MISSING,
            result.resolverEffectAssessment.status
        )

        assertEquals(
            false,
            result.resolverEffectAssessment.baselineAvailable
        )

        assertEquals(
            null,
            result.resolverEffectAssessment.previousEntryCount
        )

        assertEquals(
            null,
            result.resolverEffectAssessment.resolvedEntryCount
        )
    }

    private fun classificationAnalysis(
        classification:
        CatalogReviewBacklogClassification,
        entryCount: Int,
        deterministicCount: Int,
        manualCount: Int = 0,
        priority:
        CatalogReviewResolutionPriority,
        recommendation:
        CatalogReviewResolutionRecommendation
    ): CatalogReviewClassificationAnalysis =
        CatalogReviewClassificationAnalysis(
            classification = classification,
            entryCount = entryCount,
            shareOfBacklog = 1.0,
            potentiallyDeterministicCount =
                deterministicCount,
            manualReviewRequiredCount =
                manualCount,
            conflictingEvidenceCount = 0,
            splitRequiredCount = 0,
            entriesWithUniqueMergeTargetCount =
                deterministicCount,
            priority = priority,
            recommendation = recommendation,
            rationale = "Test analysis.",
            exampleSourceIndices =
                (1..minOf(entryCount, 10)).toList()
        )

    private fun analysisResult(
        vararg analyses:
        CatalogReviewClassificationAnalysis
    ): CatalogReviewBacklogAnalysisResult {
        val sorted = analyses
            .sortedWith(
                compareByDescending<CatalogReviewClassificationAnalysis> {
                    it.entryCount
                }.thenBy {
                    it.classification.name
                }
            )

        val classifiedCount =
            sorted.sumOf { it.entryCount }

        val deterministicCount =
            sorted.sumOf {
                it.potentiallyDeterministicCount
            }

        val manualCount =
            sorted.sumOf {
                it.manualReviewRequiredCount
            }

        val countsByClassification =
            sorted.associate {
                it.classification to it.entryCount
            }
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByAssessment =
            buildMap {
                if (deterministicCount > 0) {
                    put(
                        CatalogReviewAutomationAssessment
                            .POTENTIALLY_DETERMINISTIC,
                        deterministicCount
                    )
                }

                if (manualCount > 0) {
                    put(
                        CatalogReviewAutomationAssessment
                            .MANUAL_REVIEW_REQUIRED,
                        manualCount
                    )
                }
            }

        val recommendations =
            sorted.map { it.recommendation }
                .distinct()

        return CatalogReviewBacklogAnalysisResult(
            version =
                CatalogReviewBacklogAnalysisResult
                    .CURRENT_VERSION,
            classifiedEntryCount =
                classifiedCount,
            potentiallyDeterministicCount =
                deterministicCount,
            manualReviewRequiredCount =
                manualCount,
            conflictingEvidenceCount = 0,
            splitRequiredCount = 0,
            potentiallyDeterministicShare =
                if (classifiedCount == 0) {
                    0.0
                } else {
                    deterministicCount.toDouble() /
                            classifiedCount
                },
            manualOnlyShare =
                if (classifiedCount == 0) {
                    0.0
                } else {
                    manualCount.toDouble() /
                            classifiedCount
                },
            classificationCount =
                sorted.size,
            affectedCategoryCount = 0,
            countsByClassification =
                countsByClassification,
            countsByAutomationAssessment =
                countsByAssessment,
            classificationAnalyses =
                sorted,
            categoryAnalyses =
                emptyList(),
            prioritizedRecommendations =
                recommendations,
            nextRecommendedImplementation =
                recommendations.firstOrNull(),
            valid = true
        )
    }
}