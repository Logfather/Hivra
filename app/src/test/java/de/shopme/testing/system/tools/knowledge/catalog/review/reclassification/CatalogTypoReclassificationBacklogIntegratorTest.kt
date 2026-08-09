package de.shopme.testing.system.tools.knowledge.catalog.review.reclassification

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogStatus
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewAutomationAssessment
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassificationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.ClassifiedCatalogReviewBacklogEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogTypoReclassificationBacklogIntegratorTest {

    private val integrator =
        CatalogTypoReclassificationBacklogIntegrator()

    @Test
    fun replaceFrozenTypoClassification() {
        val result =
            integrator.integrate(
                classificationResult =
                    classificationResult(
                        classifiedTypoEntry(
                            sourceIndex = 10,
                            itemName =
                                "TK-Blattspinat",
                            category =
                                "Tiefkühlprodukte",
                            targetSourceIndex = 20
                        )
                    ),

                reclassificationResult =
                    reclassificationResult(
                        reclassifiedEntry(
                            sourceIndex = 10,
                            sourceName =
                                "TK-Blattspinat",
                            sourceCategory =
                                "Tiefkühlprodukte",
                            targetSourceIndex = 20,
                            targetName =
                                "Blattspinat",
                            targetCategory =
                                "Gemüse",
                            classification =
                                CatalogMisclassifiedTypoClass
                                    .FROZEN_FORM_VARIANT
                        )
                    )
            )

        val entry =
            result.entries.single()

        assertEquals(
            CatalogReviewBacklogClassification
                .FROZEN_FORM_VARIANT,
            entry.primaryClassification
        )

        assertFalse(
            CatalogReviewBacklogClassification
                .TYPO_VARIANT in
                    entry.detectedClassifications
        )

        assertTrue(
            CatalogReviewBacklogClassification
                .FROZEN_FORM_VARIANT in
                    entry.detectedClassifications
        )

        assertEquals(
            CatalogReviewAutomationAssessment
                .MANUAL_REVIEW_REQUIRED,
            entry.automationAssessment
        )
    }

    @Test
    fun replaceAllSupportedReclassificationTypes() {
        val result =
            integrator.integrate(
                classificationResult =
                    classificationResult(
                        classifiedTypoEntry(
                            sourceIndex = 10,
                            itemName = "TK-Blattspinat",
                            category = "Tiefkühlprodukte",
                            targetSourceIndex = 20
                        ),
                        classifiedTypoEntry(
                            sourceIndex = 30,
                            itemName = "Ananas in Dose",
                            category = "Konserven",
                            targetSourceIndex = 40
                        ),
                        classifiedTypoEntry(
                            sourceIndex = 50,
                            itemName =
                                "Vegetarische Pizza Bio",
                            category = "Vegetarisch",
                            targetSourceIndex = 60
                        )
                    ),

                reclassificationResult =
                    reclassificationResult(
                        reclassifiedEntry(
                            sourceIndex = 10,
                            sourceName = "TK-Blattspinat",
                            sourceCategory =
                                "Tiefkühlprodukte",
                            targetSourceIndex = 20,
                            targetName = "Blattspinat",
                            targetCategory = "Gemüse",
                            classification =
                                CatalogMisclassifiedTypoClass
                                    .FROZEN_FORM_VARIANT
                        ),
                        reclassifiedEntry(
                            sourceIndex = 30,
                            sourceName = "Ananas in Dose",
                            sourceCategory = "Konserven",
                            targetSourceIndex = 40,
                            targetName = "Ananas",
                            targetCategory = "Obst",
                            classification =
                                CatalogMisclassifiedTypoClass
                                    .CANNED_FORM_VARIANT
                        ),
                        reclassifiedEntry(
                            sourceIndex = 50,
                            sourceName =
                                "Vegetarische Pizza Bio",
                            sourceCategory =
                                "Vegetarisch",
                            targetSourceIndex = 60,
                            targetName =
                                "Vegetarische Pizza",
                            targetCategory =
                                "Vegetarisch",
                            classification =
                                CatalogMisclassifiedTypoClass
                                    .BIO_ATTRIBUTE_VARIANT
                        )
                    )
            )

        assertEquals(
            1,
            result.countsByClassification[
                CatalogReviewBacklogClassification
                    .FROZEN_FORM_VARIANT
            ]
        )

        assertEquals(
            1,
            result.countsByClassification[
                CatalogReviewBacklogClassification
                    .CANNED_FORM_VARIANT
            ]
        )

        assertEquals(
            1,
            result.countsByClassification[
                CatalogReviewBacklogClassification
                    .BIO_ATTRIBUTE_VARIANT
            ]
        )

        assertEquals(
            null,
            result.countsByClassification[
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT
            ]
        )
    }

    @Test
    fun preserveUnrelatedBacklogEntries() {
        val typoEntry =
            classifiedTypoEntry(
                sourceIndex = 10,
                itemName = "TK-Blattspinat",
                category = "Tiefkühlprodukte",
                targetSourceIndex = 20
            )

        val semanticEntry =
            classifiedTypoEntry(
                sourceIndex = 30,
                itemName = "Chili sin Carne",
                category = "Vegetarisch",
                targetSourceIndex = 40
            ).copy(
                primaryClassification =
                    CatalogReviewBacklogClassification
                        .POSSIBLE_SEMANTIC_DUPLICATE,

                detectedClassifications =
                    listOf(
                        CatalogReviewBacklogClassification
                            .POSSIBLE_SEMANTIC_DUPLICATE
                    ),

                automationAssessment =
                    CatalogReviewAutomationAssessment
                        .MANUAL_REVIEW_REQUIRED
            )

        val result =
            integrator.integrate(
                classificationResult =
                    classificationResult(
                        typoEntry,
                        semanticEntry
                    ),

                reclassificationResult =
                    reclassificationResult(
                        reclassifiedEntry(
                            sourceIndex = 10,
                            sourceName = "TK-Blattspinat",
                            sourceCategory =
                                "Tiefkühlprodukte",
                            targetSourceIndex = 20,
                            targetName = "Blattspinat",
                            targetCategory = "Gemüse",
                            classification =
                                CatalogMisclassifiedTypoClass
                                    .FROZEN_FORM_VARIANT
                        )
                    )
            )

        assertEquals(
            semanticEntry,
            result.entries.single {
                it.sourceIndex == 30
            }
        )
    }

    @Test
    fun integrateDeterministically() {
        val classificationResult =
            classificationResult(
                classifiedTypoEntry(
                    sourceIndex = 20,
                    itemName = "Ananas in Dose",
                    category = "Konserven",
                    targetSourceIndex = 30
                ),
                classifiedTypoEntry(
                    sourceIndex = 10,
                    itemName = "TK-Blattspinat",
                    category = "Tiefkühlprodukte",
                    targetSourceIndex = 40
                )
            )

        val reclassificationResult =
            reclassificationResult(
                reclassifiedEntry(
                    sourceIndex = 20,
                    sourceName = "Ananas in Dose",
                    sourceCategory = "Konserven",
                    targetSourceIndex = 30,
                    targetName = "Ananas",
                    targetCategory = "Obst",
                    classification =
                        CatalogMisclassifiedTypoClass
                            .CANNED_FORM_VARIANT
                ),
                reclassifiedEntry(
                    sourceIndex = 10,
                    sourceName = "TK-Blattspinat",
                    sourceCategory = "Tiefkühlprodukte",
                    targetSourceIndex = 40,
                    targetName = "Blattspinat",
                    targetCategory = "Gemüse",
                    classification =
                        CatalogMisclassifiedTypoClass
                            .FROZEN_FORM_VARIANT
                )
            )

        val first =
            integrator.integrate(
                classificationResult,
                reclassificationResult
            )

        val second =
            integrator.integrate(
                classificationResult,
                reclassificationResult
            )

        assertEquals(first, second)

        assertEquals(
            listOf(10, 20),
            first.entries.map {
                it.sourceIndex
            }
        )
    }

    private fun classifiedTypoEntry(
        sourceIndex: Int,
        itemName: String,
        category: String,
        targetSourceIndex: Int
    ): ClassifiedCatalogReviewBacklogEntry =
        ClassifiedCatalogReviewBacklogEntry(
            sourceIndex = sourceIndex,
            itemName = itemName,
            category = category,
            normalizedKey =
                itemName.lowercase(),
            originalAction =
                CatalogCanonicalizationAction.REVIEW,
            backlogStatus =
                CatalogReviewBacklogStatus
                    .STILL_REVIEW_REQUIRED,
            mergeTargetSourceIndex =
                targetSourceIndex,
            primaryClassification =
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT,
            detectedClassifications =
                listOf(
                    CatalogReviewBacklogClassification
                        .TYPO_VARIANT
                ),
            automationAssessment =
                CatalogReviewAutomationAssessment
                    .POTENTIALLY_DETERMINISTIC,
            hasUniqueMergeTarget = true,
            hasMultipleConflictReasons = false,
            originalReasons =
                listOf(
                    "Duplicate reasons: TYPO_VARIANT."
                ),
            classificationReasons =
                listOf(
                    "Primary review classification is TYPO_VARIANT."
                )
        )

    private fun classificationResult(
        vararg entries:
        ClassifiedCatalogReviewBacklogEntry
    ): CatalogReviewBacklogClassificationResult {
        val sorted =
            entries.sortedBy {
                it.sourceIndex
            }

        return createClassificationResult(sorted)
    }

    private fun createClassificationResult(
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

        val countsByAssessment =
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

        return CatalogReviewBacklogClassificationResult(
            version =
                CatalogReviewBacklogClassificationResult
                    .CURRENT_VERSION,
            sourceBacklogEntryCount =
                entries.size,
            classifiedEntryCount =
                entries.size,
            potentiallyDeterministicCount =
                entries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .POTENTIALLY_DETERMINISTIC
                },
            manualReviewRequiredCount =
                entries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .MANUAL_REVIEW_REQUIRED
                },
            conflictingEvidenceCount = 0,
            splitRequiredCount = 0,
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
                countsByAssessment,
            countsByCategory =
                entries
                    .groupingBy {
                        it.category ?: "<uncategorized>"
                    }
                    .eachCount()
                    .toSortedMap(),
            entries =
                entries,
            valid = true
        )
    }

    private fun reclassifiedEntry(
        sourceIndex: Int,
        sourceName: String,
        sourceCategory: String,
        targetSourceIndex: Int,
        targetName: String,
        targetCategory: String,
        classification:
        CatalogMisclassifiedTypoClass
    ): CatalogMisclassifiedTypoEntry =
        CatalogMisclassifiedTypoEntry(
            sourceIndex = sourceIndex,
            sourceName = sourceName,
            sourceCategory = sourceCategory,
            targetSourceIndex =
                targetSourceIndex,
            targetName = targetName,
            targetCategory =
                targetCategory,
            originalSubtype =
                "PROBABLY_MISCLASSIFIED",
            reclassifiedAs =
                classification,
            recommendation =
                CatalogMisclassifiedTypoRecommendation
                    .PRESERVE_AS_SEPARATE_FOODS,
            matchedMarkers =
                when (classification) {
                    CatalogMisclassifiedTypoClass
                        .FROZEN_FORM_VARIANT ->
                        listOf("tk")

                    CatalogMisclassifiedTypoClass
                        .CANNED_FORM_VARIANT ->
                        listOf("dose")

                    CatalogMisclassifiedTypoClass
                        .BIO_ATTRIBUTE_VARIANT ->
                        listOf("bio")

                    else ->
                        emptyList()
                },
            reasons =
                listOf(
                    "Original TYPO_VARIANT classification was replaced."
                )
        )

    private fun reclassificationResult(
        vararg entries:
        CatalogMisclassifiedTypoEntry
    ): CatalogMisclassifiedTypoReclassificationResult {
        val sorted =
            entries.sortedBy {
                it.sourceIndex
            }

        return CatalogMisclassifiedTypoReclassificationResult(
            version =
                CatalogMisclassifiedTypoReclassificationResult
                    .CURRENT_VERSION,
            inputCandidateCount =
                sorted.size,
            reclassifiedEntryCount =
                sorted.size,
            preserveSeparateFoodCount =
                sorted.count {
                    it.recommendation ==
                            CatalogMisclassifiedTypoRecommendation
                                .PRESERVE_AS_SEPARATE_FOODS
                },
            manualReviewRequiredCount =
                sorted.count {
                    it.recommendation ==
                            CatalogMisclassifiedTypoRecommendation
                                .MANUAL_REVIEW_REQUIRED
                },
            countsByClass =
                sorted
                    .groupingBy {
                        it.reclassifiedAs
                    }
                    .eachCount()
                    .toList()
                    .sortedBy {
                        it.first.name
                    }
                    .associate {
                        it
                    },
            countsByRecommendation =
                sorted
                    .groupingBy {
                        it.recommendation
                    }
                    .eachCount()
                    .toList()
                    .sortedBy {
                        it.first.name
                    }
                    .associate {
                        it
                    },
            countsBySourceCategory =
                sorted
                    .groupingBy {
                        it.sourceCategory
                            ?: "<uncategorized>"
                    }
                    .eachCount()
                    .toSortedMap(),
            entries = sorted,
            valid = true
        )
    }
}