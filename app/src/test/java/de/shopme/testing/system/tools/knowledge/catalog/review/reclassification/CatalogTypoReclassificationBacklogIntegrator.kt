package de.shopme.testing.system.tools.knowledge.catalog.review.reclassification

import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewAutomationAssessment
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassificationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.ClassifiedCatalogReviewBacklogEntry

class CatalogTypoReclassificationBacklogIntegrator {

    fun integrate(
        classificationResult:
        CatalogReviewBacklogClassificationResult,

        reclassificationResult:
        CatalogMisclassifiedTypoReclassificationResult
    ): CatalogReviewBacklogClassificationResult {
        require(classificationResult.valid) {
            "Review backlog classification must be valid."
        }

        require(reclassificationResult.valid) {
            "Typo reclassification must be valid."
        }

        val reclassificationsBySourceIndex =
            reclassificationResult.entries
                .associateBy {
                    it.sourceIndex
                }

        require(
            reclassificationsBySourceIndex.size ==
                    reclassificationResult.entries.size
        ) {
            "Typo reclassification contains duplicate sourceIndex values."
        }

        val classificationEntriesBySourceIndex =
            classificationResult.entries
                .associateBy {
                    it.sourceIndex
                }

        val missingSourceIndices =
            reclassificationsBySourceIndex.keys -
                    classificationEntriesBySourceIndex.keys

        require(missingSourceIndices.isEmpty()) {
            "Typo reclassification references unknown backlog entries: " +
                    missingSourceIndices.sorted().joinToString()
        }

        val integratedEntries =
            classificationResult.entries
                .map { entry ->
                    val reclassification =
                        reclassificationsBySourceIndex[
                            entry.sourceIndex
                        ]

                    if (reclassification == null) {
                        entry
                    } else {
                        integrateEntry(
                            entry = entry,
                            reclassification =
                                reclassification
                        )
                    }
                }
                .sortedBy {
                    it.sourceIndex
                }

        validateCoverage(
            integratedEntries = integratedEntries,
            reclassificationResult =
                reclassificationResult
        )

        return createResult(
            sourceResult =
                classificationResult,
            entries =
                integratedEntries
        )
    }

    private fun integrateEntry(
        entry: ClassifiedCatalogReviewBacklogEntry,
        reclassification:
        CatalogMisclassifiedTypoEntry
    ): ClassifiedCatalogReviewBacklogEntry {
        require(
            entry.primaryClassification ==
                    CatalogReviewBacklogClassification
                        .TYPO_VARIANT
        ) {
            "Entry ${entry.sourceIndex} is not classified as TYPO_VARIANT."
        }

        require(
            reclassification.recommendation ==
                    CatalogMisclassifiedTypoRecommendation
                        .PRESERVE_AS_SEPARATE_FOODS
        ) {
            "Only preserved separate-food variants may be integrated " +
                    "automatically."
        }

        require(
            entry.sourceIndex ==
                    reclassification.sourceIndex
        )

        require(
            entry.mergeTargetSourceIndex ==
                    reclassification.targetSourceIndex
        ) {
            "Reclassification target differs from the backlog target for " +
                    "sourceIndex ${entry.sourceIndex}."
        }

        val effectiveClassification =
            mapClassification(
                reclassification.reclassifiedAs
            )

        val effectiveDetectedClassifications =
            (
                    entry.detectedClassifications
                        .filter {
                            it !=
                                    CatalogReviewBacklogClassification
                                        .TYPO_VARIANT
                        } +
                            effectiveClassification
                    )
                .distinct()
                .sortedBy {
                    it.name
                }

        val effectiveClassificationReasons =
            (
                    entry.classificationReasons +
                            reclassification.reasons +
                            listOf(
                                "Original TYPO_VARIANT classification was replaced " +
                                        "by ${effectiveClassification.name}.",
                                "Source and target must remain separate canonical foods."
                            )
                    )
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        return entry.copy(
            primaryClassification =
                effectiveClassification,

            detectedClassifications =
                effectiveDetectedClassifications,

            /*
             * Diese Fälle sind nicht automatisierbare Dubletten.
             * Sie bleiben als fachlich getrennte Lebensmittel erhalten.
             */
            automationAssessment =
                CatalogReviewAutomationAssessment
                    .MANUAL_REVIEW_REQUIRED,

            classificationReasons =
                effectiveClassificationReasons
        )
    }

    private fun mapClassification(
        classification:
        CatalogMisclassifiedTypoClass
    ): CatalogReviewBacklogClassification =
        when (classification) {
            CatalogMisclassifiedTypoClass
                .FROZEN_FORM_VARIANT ->
                CatalogReviewBacklogClassification
                    .FROZEN_FORM_VARIANT

            CatalogMisclassifiedTypoClass
                .CANNED_FORM_VARIANT ->
                CatalogReviewBacklogClassification
                    .CANNED_FORM_VARIANT

            CatalogMisclassifiedTypoClass
                .BIO_ATTRIBUTE_VARIANT ->
                CatalogReviewBacklogClassification
                    .BIO_ATTRIBUTE_VARIANT

            /*
             * Diese Klassen sind in der aktuellen produktiven
             * Reclassification nicht enthalten. Sie bleiben konservativ
             * unter den bereits vorhandenen fachlichen Review-Klassen.
             */
            CatalogMisclassifiedTypoClass
                .SALES_FORM_VARIANT ->
                CatalogReviewBacklogClassification
                    .SALES_FORM_VARIANT

            CatalogMisclassifiedTypoClass
                .PRESERVATION_FORM_VARIANT ->
                CatalogReviewBacklogClassification
                    .PREPARATION_VARIANT

            CatalogMisclassifiedTypoClass
                .SEMANTIC_PRODUCT_VARIANT ->
                CatalogReviewBacklogClassification
                    .POSSIBLE_SEMANTIC_DUPLICATE

            CatalogMisclassifiedTypoClass
                .UNCLASSIFIED ->
                CatalogReviewBacklogClassification
                    .UNCLASSIFIED_REVIEW
        }

    private fun validateCoverage(
        integratedEntries:
        List<ClassifiedCatalogReviewBacklogEntry>,

        reclassificationResult:
        CatalogMisclassifiedTypoReclassificationResult
    ) {
        val expectedByClassification =
            reclassificationResult.entries
                .groupingBy {
                    mapClassification(
                        it.reclassifiedAs
                    )
                }
                .eachCount()

        expectedByClassification.forEach {
                (classification, expectedCount) ->

            val actualCount =
                integratedEntries.count {
                        entry ->
                    entry.primaryClassification ==
                            classification &&
                            entry.sourceIndex in
                            reclassificationResult.entries
                                .asSequence()
                                .filter {
                                        reclassification ->
                                    mapClassification(
                                        reclassification
                                            .reclassifiedAs
                                    ) == classification
                                }
                                .map {
                                    it.sourceIndex
                                }
                                .toSet()
                }

            require(actualCount == expectedCount) {
                "Integrated count for ${classification.name} is " +
                        "$actualCount, expected $expectedCount."
            }
        }

        val remainingReclassifiedTypoEntries =
            integratedEntries.count { entry ->
                entry.sourceIndex in
                        reclassificationResult.entries
                            .map {
                                it.sourceIndex
                            }
                            .toSet() &&
                        entry.primaryClassification ==
                        CatalogReviewBacklogClassification
                            .TYPO_VARIANT
            }

        require(remainingReclassifiedTypoEntries == 0) {
            "$remainingReclassifiedTypoEntries reclassified entries still " +
                    "use TYPO_VARIANT."
        }
    }

    private fun createResult(
        sourceResult:
        CatalogReviewBacklogClassificationResult,

        entries:
        List<ClassifiedCatalogReviewBacklogEntry>
    ): CatalogReviewBacklogClassificationResult {
        val countsByClassification =
            entries
                .groupingBy {
                    it.primaryClassification
                }
                .eachCount()
                .toList()
                .sortedBy {
                    it.first.name
                }
                .associate {
                    it
                }

        val countsByAutomationAssessment =
            entries
                .groupingBy {
                    it.automationAssessment
                }
                .eachCount()
                .toList()
                .sortedBy {
                    it.first.name
                }
                .associate {
                    it
                }

        val countsByCategory =
            entries
                .groupingBy {
                    it.category
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?: UNCATEGORIZED
                }
                .eachCount()
                .toSortedMap()

        val potentiallyDeterministicCount =
            entries.count {
                it.automationAssessment ==
                        CatalogReviewAutomationAssessment
                            .POTENTIALLY_DETERMINISTIC
            }

        val manualReviewRequiredCount =
            entries.count {
                it.automationAssessment ==
                        CatalogReviewAutomationAssessment
                            .MANUAL_REVIEW_REQUIRED
            }

        val conflictingEvidenceCount =
            entries.count {
                it.automationAssessment ==
                        CatalogReviewAutomationAssessment
                            .CONFLICTING_EVIDENCE
            }

        val splitRequiredCount =
            entries.count {
                it.automationAssessment ==
                        CatalogReviewAutomationAssessment
                            .SPLIT_REQUIRED
            }

        return CatalogReviewBacklogClassificationResult(
            version =
                sourceResult.version,

            sourceBacklogEntryCount =
                sourceResult.sourceBacklogEntryCount,

            classifiedEntryCount =
                entries.size,

            potentiallyDeterministicCount =
                potentiallyDeterministicCount,

            manualReviewRequiredCount =
                manualReviewRequiredCount,

            conflictingEvidenceCount =
                conflictingEvidenceCount,

            splitRequiredCount =
                splitRequiredCount,

            entriesWithUniqueMergeTargetCount =
                entries.count {
                    it.hasUniqueMergeTarget
                },

            entriesWithMultipleConflictReasonsCount =
                entries.count {
                    it.hasMultipleConflictReasons
                },

            countsByClassification =
                countsByClassification,

            countsByAutomationAssessment =
                countsByAutomationAssessment,

            countsByCategory =
                countsByCategory,

            entries =
                entries,

            valid = true
        )
    }

    private companion object {
        const val UNCATEGORIZED =
            "<uncategorized>"
    }
}