package de.shopme.testing.system.tools.knowledge.catalog.review.analysis.updated

import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewBacklogAnalysisResult
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewResolutionRecommendation
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification

class CatalogUpdatedReviewAnalyzer {

    fun analyze(
        previousAnalysis:
        CatalogReviewBacklogAnalysisResult?,

        currentAnalysis:
        CatalogReviewBacklogAnalysisResult,

        implementedRecommendation:
        CatalogReviewResolutionRecommendation
    ): CatalogUpdatedReviewAnalysisResult {
        previousAnalysis?.let {
            require(it.valid) {
                "Previous analysis must be valid."
            }
        }

        require(currentAnalysis.valid) {
            "Current analysis must be valid."
        }

        val affectedClassification =
            classificationFor(
                implementedRecommendation
            )

        val previousAffectedAnalysis =
            previousAnalysis
                ?.classificationAnalyses
                ?.firstOrNull {
                    it.classification ==
                            affectedClassification
                }

        val currentAffectedAnalysis =
            currentAnalysis.classificationAnalyses
                .firstOrNull {
                    it.classification ==
                            affectedClassification
                }

        val previousEntryCount =
            previousAffectedAnalysis?.entryCount

        val remainingEntryCount =
            currentAffectedAnalysis?.entryCount ?: 0

        val previousPotentiallyDeterministicCount =
            previousAffectedAnalysis
                ?.potentiallyDeterministicCount

        val remainingPotentiallyDeterministicCount =
            currentAffectedAnalysis
                ?.potentiallyDeterministicCount
                ?: 0

        val resolverEffectAssessment =
            assessResolverEffect(
                implementedRecommendation =
                    implementedRecommendation,
                affectedClassification =
                    affectedClassification,
                previousEntryCount =
                    previousEntryCount,
                remainingEntryCount =
                    remainingEntryCount,
                previousPotentiallyDeterministicCount =
                    previousPotentiallyDeterministicCount,
                remainingPotentiallyDeterministicCount =
                    remainingPotentiallyDeterministicCount
            )

        val deterministicClassifications =
            currentAnalysis.classificationAnalyses
                .filter {
                    it.potentiallyDeterministicCount > 0
                }
                .associate {
                    it.classification to
                            it.potentiallyDeterministicCount
                }
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val manualClassifications =
            currentAnalysis.classificationAnalyses
                .mapNotNull { analysis ->
                    val manualCount =
                        analysis.manualReviewRequiredCount +
                                analysis.conflictingEvidenceCount +
                                analysis.splitRequiredCount

                    if (manualCount == 0) {
                        null
                    } else {
                        analysis.classification to
                                manualCount
                    }
                }
                .sortedBy { it.first.name }
                .associate { it }

        val manualOnlyCount =
            currentAnalysis.manualReviewRequiredCount +
                    currentAnalysis.conflictingEvidenceCount +
                    currentAnalysis.splitRequiredCount

        val nextRecommendation =
            currentAnalysis.nextRecommendedImplementation
                ?.takeUnless {
                    it ==
                            CatalogReviewResolutionRecommendation
                                .PRESERVE_DISTINCT_PRODUCT_FORMS
                }

        val blockers = buildImplementationBlockers(
            currentAnalysis = currentAnalysis,
            nextRecommendation = nextRecommendation
        )

        val implementationReady =
            nextRecommendation != null &&
                    blockers.isEmpty()

        val highestPriorityClassification =
            nextRecommendation?.let(
                ::classificationFor
            )

        return CatalogUpdatedReviewAnalysisResult(
            version =
                CatalogUpdatedReviewAnalysisResult
                    .CURRENT_VERSION,

            classifiedEntryCount =
                currentAnalysis.classifiedEntryCount,

            potentiallyDeterministicCount =
                currentAnalysis
                    .potentiallyDeterministicCount,

            manualOnlyCount =
                manualOnlyCount,

            deterministicShare =
                share(
                    currentAnalysis
                        .potentiallyDeterministicCount,
                    currentAnalysis
                        .classifiedEntryCount
                ),

            manualOnlyShare =
                share(
                    manualOnlyCount,
                    currentAnalysis
                        .classifiedEntryCount
                ),

            highestPriorityClassification =
                highestPriorityClassification,

            nextRecommendedImplementation =
                nextRecommendation,

            resolverEffectAssessment =
                resolverEffectAssessment,

            remainingDeterministicClassifications =
                deterministicClassifications,

            remainingManualClassifications =
                manualClassifications,

            implementationReady =
                implementationReady,

            implementationBlockers =
                blockers,

            observations =
                buildObservations(
                    currentAnalysis =
                        currentAnalysis,
                    resolverEffectAssessment =
                        resolverEffectAssessment,
                    nextRecommendation =
                        nextRecommendation
                ),

            valid = true
        )
    }

    private fun assessResolverEffect(
        implementedRecommendation:
        CatalogReviewResolutionRecommendation,

        affectedClassification:
        CatalogReviewBacklogClassification,

        previousEntryCount: Int?,
        remainingEntryCount: Int,

        previousPotentiallyDeterministicCount: Int?,
        remainingPotentiallyDeterministicCount: Int
    ): CatalogResolverEffectAssessment {
        if (
            previousEntryCount == null ||
            previousPotentiallyDeterministicCount == null
        ) {
            return CatalogResolverEffectAssessment(
                implementedRecommendation =
                    implementedRecommendation,
                affectedClassification =
                    affectedClassification,
                baselineAvailable = false,
                previousEntryCount = null,
                remainingEntryCount =
                    remainingEntryCount,
                resolvedEntryCount = null,
                resolutionRate = null,
                previousPotentiallyDeterministicCount = null,
                remainingPotentiallyDeterministicCount =
                    remainingPotentiallyDeterministicCount,
                resolvedPotentiallyDeterministicCount = null,
                status =
                    CatalogUpdatedReportStatus
                        .EFFECT_BASELINE_MISSING,
                rationale =
                    "Resolver effect cannot be measured because no " +
                            "previous analysis baseline was supplied."
            )
        }

        require(remainingEntryCount <= previousEntryCount) {
            "Current classification count exceeds previous count."
        }

        require(
            remainingPotentiallyDeterministicCount <=
                    previousPotentiallyDeterministicCount
        ) {
            "Current deterministic count exceeds previous count."
        }

        val resolvedEntryCount =
            previousEntryCount - remainingEntryCount

        val resolvedPotentiallyDeterministicCount =
            previousPotentiallyDeterministicCount -
                    remainingPotentiallyDeterministicCount

        val resolutionRate =
            if (previousEntryCount == 0) {
                0.0
            } else {
                resolvedEntryCount.toDouble() /
                        previousEntryCount.toDouble()
            }

        val status = when {
            previousEntryCount == 0 ->
                CatalogUpdatedReportStatus
                    .RESOLVER_EFFECT_NOT_VISIBLE

            remainingEntryCount == 0 ->
                CatalogUpdatedReportStatus
                    .RESOLVER_EFFECT_CONFIRMED

            resolvedEntryCount > 0 ->
                CatalogUpdatedReportStatus
                    .RESOLVER_PARTIALLY_EFFECTIVE

            else ->
                CatalogUpdatedReportStatus
                    .RESOLVER_EFFECT_NOT_VISIBLE
        }

        val rationale = when (status) {
            CatalogUpdatedReportStatus
                .RESOLVER_EFFECT_CONFIRMED ->
                "Resolver removed all $previousEntryCount " +
                        "${affectedClassification.name} entries."

            CatalogUpdatedReportStatus
                .RESOLVER_PARTIALLY_EFFECTIVE ->
                "Resolver removed $resolvedEntryCount of " +
                        "$previousEntryCount ${affectedClassification.name} " +
                        "entries; $remainingEntryCount remain."

            CatalogUpdatedReportStatus
                .RESOLVER_EFFECT_NOT_VISIBLE ->
                if (previousEntryCount == 0) {
                    "No baseline ${affectedClassification.name} entries " +
                            "were available for effect measurement."
                } else {
                    "Resolver removed none of the $previousEntryCount " +
                            "${affectedClassification.name} entries."
                }

            CatalogUpdatedReportStatus
                .EFFECT_BASELINE_MISSING ->
                error(
                    "EFFECT_BASELINE_MISSING is handled before effect calculation."
                )

            CatalogUpdatedReportStatus
                .NO_DETERMINISTIC_WORK_REMAINING ->
                "No deterministic review work remains."
        }

        return CatalogResolverEffectAssessment(
            implementedRecommendation =
                implementedRecommendation,
            affectedClassification =
                affectedClassification,
            baselineAvailable = true,
            previousEntryCount =
                previousEntryCount,
            remainingEntryCount =
                remainingEntryCount,
            resolvedEntryCount =
                resolvedEntryCount,
            resolutionRate =
                resolutionRate,
            previousPotentiallyDeterministicCount =
                previousPotentiallyDeterministicCount,
            remainingPotentiallyDeterministicCount =
                remainingPotentiallyDeterministicCount,
            resolvedPotentiallyDeterministicCount =
                resolvedPotentiallyDeterministicCount,
            status = status,
            rationale = rationale
        )
    }

    private fun buildImplementationBlockers(
        currentAnalysis:
        CatalogReviewBacklogAnalysisResult,
        nextRecommendation:
        CatalogReviewResolutionRecommendation?
    ): List<String> {
        if (nextRecommendation == null) {
            return emptyList()
        }

        val classification =
            classificationFor(nextRecommendation)

        val analysis =
            currentAnalysis.classificationAnalyses
                .firstOrNull {
                    it.classification == classification
                }
                ?: return listOf(
                    "Recommended classification has no analysis entry."
                )

        val blockers = mutableListOf<String>()

        if (
            analysis.potentiallyDeterministicCount == 0 &&
            nextRecommendation in
            IMPLEMENTATION_RECOMMENDATIONS
        ) {
            blockers +=
                "Recommended resolver has no potentially deterministic entries."
        }

        if (
            analysis.conflictingEvidenceCount > 0
        ) {
            blockers +=
                "Recommended classification contains conflicting evidence."
        }

        if (
            analysis.entriesWithUniqueMergeTargetCount <
            analysis.potentiallyDeterministicCount
        ) {
            blockers +=
                "Not every deterministic candidate has a unique merge target."
        }

        return blockers
            .distinct()
            .sorted()
    }

    private fun buildObservations(
        currentAnalysis:
        CatalogReviewBacklogAnalysisResult,
        resolverEffectAssessment:
        CatalogResolverEffectAssessment,
        nextRecommendation:
        CatalogReviewResolutionRecommendation?
    ): List<String> =
        buildList {
            add(resolverEffectAssessment.rationale)

            add(
                "${currentAnalysis.potentiallyDeterministicCount} of " +
                        "${currentAnalysis.classifiedEntryCount} entries " +
                        "remain potentially deterministic."
            )

            add(
                "${currentAnalysis.manualReviewRequiredCount} entries " +
                        "require direct manual review."
            )

            add(
                "${currentAnalysis.conflictingEvidenceCount} entries " +
                        "contain conflicting evidence."
            )

            add(
                "${currentAnalysis.splitRequiredCount} entries require " +
                        "a catalog split."
            )

            if (nextRecommendation != null) {
                add(
                    "The next recommended implementation is " +
                            "${nextRecommendation.name}."
                )
            } else {
                add(
                    "No further deterministic resolver is recommended."
                )
            }
        }
            .distinct()
            .sorted()

    private fun classificationFor(
        recommendation:
        CatalogReviewResolutionRecommendation
    ): CatalogReviewBacklogClassification =
        when (recommendation) {
            CatalogReviewResolutionRecommendation
                .IMPLEMENT_SINGULAR_PLURAL_RESOLVER ->
                CatalogReviewBacklogClassification
                    .SINGULAR_PLURAL_VARIANT

            CatalogReviewResolutionRecommendation
                .IMPLEMENT_TYPO_VARIANT_RESOLVER ->
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT

            CatalogReviewResolutionRecommendation
                .IMPLEMENT_PUNCTUATION_VARIANT_RESOLVER ->
                CatalogReviewBacklogClassification
                    .PUNCTUATION_VARIANT

            CatalogReviewResolutionRecommendation
                .IMPLEMENT_WORD_ORDER_VARIANT_RESOLVER ->
                CatalogReviewBacklogClassification
                    .WORD_ORDER_VARIANT

            CatalogReviewResolutionRecommendation
                .IMPLEMENT_SALES_FORM_VARIANT_CLASSIFIER ->
                CatalogReviewBacklogClassification
                    .SALES_FORM_VARIANT

            CatalogReviewResolutionRecommendation
                .IMPLEMENT_PREPARATION_VARIANT_CLASSIFIER ->
                CatalogReviewBacklogClassification
                    .PREPARATION_VARIANT

            CatalogReviewResolutionRecommendation
                .IMPLEMENT_BRAND_VARIANT_POLICY ->
                CatalogReviewBacklogClassification
                    .BRAND_VARIANT

            CatalogReviewResolutionRecommendation
                .REVIEW_POSSIBLE_SEMANTIC_DUPLICATES ->
                CatalogReviewBacklogClassification
                    .POSSIBLE_SEMANTIC_DUPLICATE

            CatalogReviewResolutionRecommendation
                .REVIEW_SEMANTIC_CATEGORY_CONFLICTS ->
                CatalogReviewBacklogClassification
                    .SEMANTIC_CATEGORY_CONFLICT

            CatalogReviewResolutionRecommendation
                .RESOLVE_SPLIT_ACTIONS ->
                CatalogReviewBacklogClassification
                    .SPLIT_REQUIRED

            CatalogReviewResolutionRecommendation
                .REVIEW_UNCLASSIFIED_ENTRIES ->
                CatalogReviewBacklogClassification
                    .UNCLASSIFIED_REVIEW

            CatalogReviewResolutionRecommendation
                .REVIEW_CONFLICTING_EVIDENCE ->
                CatalogReviewBacklogClassification
                    .MULTIPLE_DUPLICATE_REASONS

            CatalogReviewResolutionRecommendation
                .PRESERVE_DISTINCT_PRODUCT_FORMS ->
                error(
                    "PRESERVE_DISTINCT_PRODUCT_FORMS does not identify a single " +
                            "review-backlog classification and must not be used as an " +
                            "implementation recommendation."
                )
        }

    private fun share(
        count: Int,
        total: Int
    ): Double =
        if (total == 0) {
            0.0
        } else {
            count.toDouble() / total.toDouble()
        }

    private companion object {

        val IMPLEMENTATION_RECOMMENDATIONS = setOf(
            CatalogReviewResolutionRecommendation
                .IMPLEMENT_SINGULAR_PLURAL_RESOLVER,
            CatalogReviewResolutionRecommendation
                .IMPLEMENT_TYPO_VARIANT_RESOLVER,
            CatalogReviewResolutionRecommendation
                .IMPLEMENT_PUNCTUATION_VARIANT_RESOLVER,
            CatalogReviewResolutionRecommendation
                .IMPLEMENT_WORD_ORDER_VARIANT_RESOLVER,
            CatalogReviewResolutionRecommendation
                .IMPLEMENT_SALES_FORM_VARIANT_CLASSIFIER,
            CatalogReviewResolutionRecommendation
                .IMPLEMENT_PREPARATION_VARIANT_CLASSIFIER,
            CatalogReviewResolutionRecommendation
                .IMPLEMENT_BRAND_VARIANT_POLICY
        )
    }
}