package de.shopme.testing.system.tools.knowledge.catalog.canonicalization

import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryIssue
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryIssueType
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateCandidate
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateGroup
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateReason
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateRecommendation
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageIssue
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageIssueType
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.CatalogNonFoodCandidate
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.CatalogNonFoodReason
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.NonFoodRecommendation
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationChange
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationChangeType
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationResult
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogIssueSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogCanonicalizationPlannerTest {

    private val planner = CatalogCanonicalizationPlanner()

    @Test
    fun createKeepActionForUnchangedEntry() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Apfel",
                normalized = "apfel",
                category = "fruit"
            )
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 0,
                    originalItemName = "Apfel",
                    originalNormalized = "apfel",
                    canonicalName = "Apfel",
                    normalizedKey = "apfel"
                )
            ),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(
                entries = entries
            ),
            languageResult = languageResult(
                entries = entries
            ),
            nonFoodCandidates = emptyList()
        )

        assertEquals(1, plan.inputEntryCount)
        assertEquals(1, plan.planEntryCount)
        assertEquals(1, plan.unchangedEntryCount)
        assertEquals(1, plan.automaticActionCount)
        assertEquals(0, plan.reviewActionCount)

        val planEntry = plan.entries.single()

        assertEquals(
            CatalogCanonicalizationAction.KEEP,
            planEntry.action
        )

        assertTrue(planEntry.automatic)
        assertEquals(1.0, planEntry.confidence)
        assertNull(planEntry.proposedCanonicalName)
        assertNull(planEntry.proposedNormalizedKey)
        assertNull(planEntry.proposedCategory)
        assertNull(planEntry.mergeTargetSourceIndex)
        assertTrue(plan.valid)
    }

    @Test
    fun createNormalizeActionForChangedNormalizedKey() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 3,
                itemName = "Paprika rot",
                normalized = "Paprika_rot",
                category = "vegetables"
            )
        )

        val normalization = normalization(
            sourceIndex = 3,
            originalItemName = "Paprika rot",
            originalNormalized = "Paprika_rot",
            canonicalName = "Paprika rot",
            normalizedKey = "paprika-rot",
            changes = listOf(
                normalizationChange(
                    type = CatalogNormalizationChangeType
                        .RECOMPUTED_NORMALIZED_KEY,
                    field = "normalized",
                    before = "Paprika_rot",
                    after = "paprika-rot",
                    reason = "Recomputed normalized key."
                )
            )
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(normalization),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = emptyList()
        )

        val planEntry = plan.entries.single()

        assertEquals(
            CatalogCanonicalizationAction.NORMALIZE,
            planEntry.action
        )

        assertEquals(
            "Paprika rot",
            planEntry.proposedCanonicalName
        )

        assertEquals(
            "paprika-rot",
            planEntry.proposedNormalizedKey
        )

        assertTrue(planEntry.automatic)
        assertEquals(1.0, planEntry.confidence)
        assertTrue(planEntry.reasons.isNotEmpty())
    }

    @Test
    fun createRenameActionForSafeLanguageIssue() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 5,
                itemName = "  Apfel  ",
                normalized = "apfel",
                category = "fruit"
            )
        )

        val languageIssue = languageIssue(
            sourceIndex = 5,
            itemName = "  Apfel  ",
            type = CatalogLanguageIssueType.LEADING_WHITESPACE,
            severity = CatalogIssueSeverity.WARNING,
            field = "itemname",
            originalValue = "  Apfel  ",
            suggestedValue = "Apfel",
            message = "Primary name contains surrounding whitespace."
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 5,
                    originalItemName = "  Apfel  ",
                    originalNormalized = "apfel",
                    canonicalName = "Apfel",
                    normalizedKey = "apfel",
                    changes = listOf(
                        normalizationChange(
                            type = CatalogNormalizationChangeType
                                .TRIMMED_WHITESPACE,
                            field = "itemname",
                            before = "  Apfel  ",
                            after = "Apfel"
                        )
                    )
                )
            ),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(
                entries = entries,
                issues = listOf(languageIssue)
            ),
            nonFoodCandidates = emptyList()
        )

        val planEntry = plan.entries.single()

        assertEquals(
            CatalogCanonicalizationAction.RENAME,
            planEntry.action
        )

        assertEquals("Apfel", planEntry.proposedCanonicalName)
        assertEquals("apfel", planEntry.proposedNormalizedKey)
        assertTrue(planEntry.automatic)
        assertTrue(planEntry.confidence >= 0.95)
    }

    @Test
    fun createReviewActionForBrandInPrimaryName() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 7,
                itemName = "Barilla Penne",
                normalized = "barilla-penne",
                category = "pasta"
            )
        )

        val issue = languageIssue(
            sourceIndex = 7,
            itemName = "Barilla Penne",
            type = CatalogLanguageIssueType.BRAND_IN_PRIMARY_NAME,
            severity = CatalogIssueSeverity.WARNING,
            field = "itemname",
            originalValue = "Barilla Penne",
            suggestedValue = "Penne",
            matchedTerms = listOf("barilla"),
            message = "Primary item name contains a brand."
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 7,
                    originalItemName = "Barilla Penne",
                    originalNormalized = "barilla-penne",
                    canonicalName = "Barilla Penne",
                    normalizedKey = "barilla-penne"
                )
            ),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(
                entries = entries,
                issues = listOf(issue)
            ),
            nonFoodCandidates = emptyList()
        )

        val planEntry = plan.entries.single()

        assertEquals(
            CatalogCanonicalizationAction.REVIEW,
            planEntry.action
        )

        assertEquals("Penne", planEntry.proposedCanonicalName)
        assertFalse(planEntry.automatic)
        assertNull(planEntry.mergeTargetSourceIndex)
    }

    @Test
    fun createMoveCategoryActionForUnambiguousSuggestion() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 9,
                itemName = "Apfel",
                normalized = "apfel",
                category = "produce"
            )
        )

        val issue = categoryIssue(
            sourceIndex = 9,
            itemName = "Apfel",
            type = CatalogCategoryIssueType.NON_NORMALIZED_CATEGORY_KEY,
            severity = CatalogIssueSeverity.WARNING,
            originalCategory = "produce",
            normalizedCategory = "produce",
            suggestedCategoryKey = "fruit",
            message = "Category should be mapped to fruit."
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 9,
                    originalItemName = "Apfel",
                    originalNormalized = "apfel",
                    canonicalName = "Apfel",
                    normalizedKey = "apfel"
                )
            ),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(
                entries = entries,
                issues = listOf(issue),
                categoryCounts = mapOf("fruit" to 1),
                suggestedMappings = mapOf("produce" to "fruit")
            ),
            languageResult = languageResult(entries),
            nonFoodCandidates = emptyList()
        )

        val planEntry = plan.entries.single()

        assertEquals(
            CatalogCanonicalizationAction.MOVE_CATEGORY,
            planEntry.action
        )

        assertEquals("fruit", planEntry.proposedCategory)
        assertTrue(planEntry.automatic)
        assertTrue(planEntry.confidence >= 0.95)
    }

    @Test
    fun createReviewActionForUnknownCategoryWithoutSuggestion() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 12,
                itemName = "Testlebensmittel",
                normalized = "testlebensmittel",
                category = "unknown"
            )
        )

        val issue = categoryIssue(
            sourceIndex = 12,
            itemName = "Testlebensmittel",
            type = CatalogCategoryIssueType.UNKNOWN_CATEGORY,
            severity = CatalogIssueSeverity.ERROR,
            originalCategory = "unknown",
            normalizedCategory = "unknown",
            suggestedCategoryKey = null,
            message = "Unknown category."
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 12,
                    originalItemName = "Testlebensmittel",
                    originalNormalized = "testlebensmittel",
                    canonicalName = "Testlebensmittel",
                    normalizedKey = "testlebensmittel"
                )
            ),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(
                entries = entries,
                issues = listOf(issue),
                unknownCategoryCounts = mapOf("unknown" to 1)
            ),
            languageResult = languageResult(entries),
            nonFoodCandidates = emptyList()
        )

        val planEntry = plan.entries.single()

        assertEquals(
            CatalogCanonicalizationAction.REVIEW,
            planEntry.action
        )

        assertFalse(planEntry.automatic)
        assertNull(planEntry.proposedCategory)
    }

    @Test
    fun createAutomaticMergeActionForDuplicate() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Apfel",
                normalized = "apfel",
                category = "fruit"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "APFEL",
                normalized = "apfel",
                category = "fruit"
            )
        )

        val duplicateGroup = duplicateGroup(
            canonicalSourceIndex = 0,
            canonicalName = "Apfel",
            recommendation =
                CatalogDuplicateRecommendation.MERGE_AUTOMATICALLY,
            members = listOf(
                duplicateCandidate(
                    sourceIndex = 0,
                    itemName = "Apfel",
                    normalizedKey = "apfel",
                    score = 1.0
                ),
                duplicateCandidate(
                    sourceIndex = 1,
                    itemName = "APFEL",
                    normalizedKey = "apfel",
                    score = 0.99
                )
            )
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 0,
                    originalItemName = "Apfel",
                    originalNormalized = "apfel",
                    canonicalName = "Apfel",
                    normalizedKey = "apfel"
                ),
                normalization(
                    sourceIndex = 1,
                    originalItemName = "APFEL",
                    originalNormalized = "apfel",
                    canonicalName = "Apfel",
                    normalizedKey = "apfel"
                )
            ),
            duplicateGroups = listOf(duplicateGroup),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = emptyList()
        )

        assertEquals(
            CatalogCanonicalizationAction.KEEP,
            plan.entries.single { it.sourceIndex == 0 }.action
        )

        val duplicateEntry = plan.entries.single {
            it.sourceIndex == 1
        }

        assertEquals(
            CatalogCanonicalizationAction.MERGE,
            duplicateEntry.action
        )

        assertEquals(0, duplicateEntry.mergeTargetSourceIndex)
        assertEquals("Apfel", duplicateEntry.proposedCanonicalName)
        assertTrue(duplicateEntry.automatic)
        assertEquals(0.99, duplicateEntry.confidence)
    }

    @Test
    fun createNonAutomaticMergeForReviewRecommendation() {
        val entries = listOf(
            indexedItem(0, "Crème fraîche", "creme-fraiche", "dairy"),
            indexedItem(1, "Creme fraiche", "creme-fraiche", "dairy")
        )

        val group = duplicateGroup(
            canonicalSourceIndex = 0,
            canonicalName = "Crème fraîche",
            recommendation =
                CatalogDuplicateRecommendation.MERGE_AFTER_REVIEW,
            members = listOf(
                duplicateCandidate(
                    sourceIndex = 0,
                    itemName = "Crème fraîche",
                    normalizedKey = "creme-fraiche",
                    score = 1.0
                ),
                duplicateCandidate(
                    sourceIndex = 1,
                    itemName = "Creme fraiche",
                    normalizedKey = "creme-fraiche",
                    score = 0.88
                )
            )
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = entries.map {
                normalization(
                    sourceIndex = it.sourceIndex,
                    originalItemName = it.item.itemname,
                    originalNormalized = it.item.normalized,
                    canonicalName = "Crème fraîche",
                    normalizedKey = "creme-fraiche"
                )
            },
            duplicateGroups = listOf(group),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = emptyList()
        )

        val mergeEntry = plan.entries.single {
            it.sourceIndex == 1
        }

        assertEquals(
            CatalogCanonicalizationAction.MERGE,
            mergeEntry.action
        )

        assertFalse(mergeEntry.automatic)
        assertEquals(0, mergeEntry.mergeTargetSourceIndex)
    }

    @Test
    fun createRemoveNonFoodAction() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 20,
                itemName = "Spülmittel",
                normalized = "spuelmittel",
                category = "cleaning"
            )
        )

        val candidate = nonFoodCandidate(
            sourceIndex = 20,
            itemName = "Spülmittel",
            category = "cleaning",
            confidence = 0.99,
            recommendation =
                NonFoodRecommendation.REMOVE_AUTOMATICALLY
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 20,
                    originalItemName = "Spülmittel",
                    originalNormalized = "spuelmittel",
                    canonicalName = "Spülmittel",
                    normalizedKey = "spuelmittel"
                )
            ),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = listOf(candidate)
        )

        val planEntry = plan.entries.single()

        assertEquals(
            CatalogCanonicalizationAction.REMOVE_NON_FOOD,
            planEntry.action
        )

        assertTrue(planEntry.automatic)
        assertEquals(0.99, planEntry.confidence)
        assertNull(planEntry.proposedCanonicalName)
        assertNull(planEntry.proposedNormalizedKey)
        assertNull(planEntry.proposedCategory)
    }

    @Test
    fun createManualRemovalForNonFoodReviewCandidate() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 21,
                itemName = "Vitaminpräparat",
                normalized = "vitaminpraeparat",
                category = "supplements"
            )
        )

        val candidate = nonFoodCandidate(
            sourceIndex = 21,
            itemName = "Vitaminpräparat",
            category = "supplements",
            confidence = 0.83,
            recommendation =
                NonFoodRecommendation.REMOVE_AFTER_REVIEW
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 21,
                    originalItemName = "Vitaminpräparat",
                    originalNormalized = "vitaminpraeparat",
                    canonicalName = "Vitaminpräparat",
                    normalizedKey = "vitaminpraeparat"
                )
            ),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = listOf(candidate)
        )

        val planEntry = plan.entries.single()

        assertEquals(
            CatalogCanonicalizationAction.REMOVE_NON_FOOD,
            planEntry.action
        )

        assertFalse(planEntry.automatic)
    }

    @Test
    fun createReviewForAmbiguousNonFoodCandidate() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 22,
                itemName = "Katzengras",
                normalized = "katzengras",
                category = "herbs"
            )
        )

        val candidate = nonFoodCandidate(
            sourceIndex = 22,
            itemName = "Katzengras",
            category = "herbs",
            confidence = 0.62,
            recommendation = NonFoodRecommendation.REVIEW
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 22,
                    originalItemName = "Katzengras",
                    originalNormalized = "katzengras",
                    canonicalName = "Katzengras",
                    normalizedKey = "katzengras"
                )
            ),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = listOf(candidate)
        )

        val planEntry = plan.entries.single()

        assertEquals(
            CatalogCanonicalizationAction.REVIEW,
            planEntry.action
        )

        assertFalse(planEntry.automatic)
    }

    @Test
    fun nonFoodDecisionHasPriorityOverDuplicateDecision() {
        val entries = listOf(
            indexedItem(0, "Spülmittel", "spuelmittel", "cleaning"),
            indexedItem(1, "Spuelmittel", "spuelmittel", "cleaning")
        )

        val duplicateGroup = duplicateGroup(
            canonicalSourceIndex = 0,
            canonicalName = "Spülmittel",
            recommendation =
                CatalogDuplicateRecommendation.MERGE_AUTOMATICALLY,
            members = listOf(
                duplicateCandidate(
                    sourceIndex = 0,
                    itemName = "Spülmittel",
                    normalizedKey = "spuelmittel",
                    score = 1.0
                ),
                duplicateCandidate(
                    sourceIndex = 1,
                    itemName = "Spuelmittel",
                    normalizedKey = "spuelmittel",
                    score = 0.98
                )
            )
        )

        val nonFood = nonFoodCandidate(
            sourceIndex = 1,
            itemName = "Spuelmittel",
            category = "cleaning",
            confidence = 0.99,
            recommendation =
                NonFoodRecommendation.REMOVE_AUTOMATICALLY
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = entries.map {
                normalization(
                    sourceIndex = it.sourceIndex,
                    originalItemName = it.item.itemname,
                    originalNormalized = it.item.normalized,
                    canonicalName = "Spülmittel",
                    normalizedKey = "spuelmittel"
                )
            },
            duplicateGroups = listOf(duplicateGroup),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = listOf(nonFood)
        )

        assertEquals(
            CatalogCanonicalizationAction.REMOVE_NON_FOOD,
            plan.entries.single { it.sourceIndex == 1 }.action
        )
    }

    @Test
    fun duplicateDecisionHasPriorityOverNormalization() {
        val entries = listOf(
            indexedItem(0, "Apfel", "apfel", "fruit"),
            indexedItem(1, "APFEL", "APFEL", "fruit")
        )

        val group = duplicateGroup(
            canonicalSourceIndex = 0,
            canonicalName = "Apfel",
            recommendation =
                CatalogDuplicateRecommendation.MERGE_AUTOMATICALLY,
            members = listOf(
                duplicateCandidate(0, "Apfel", "apfel", 1.0),
                duplicateCandidate(1, "APFEL", "apfel", 0.99)
            )
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 0,
                    originalItemName = "Apfel",
                    originalNormalized = "apfel",
                    canonicalName = "Apfel",
                    normalizedKey = "apfel"
                ),
                normalization(
                    sourceIndex = 1,
                    originalItemName = "APFEL",
                    originalNormalized = "APFEL",
                    canonicalName = "Apfel",
                    normalizedKey = "apfel",
                    changes = listOf(
                        normalizationChange(
                            type = CatalogNormalizationChangeType
                                .RECOMPUTED_NORMALIZED_KEY,
                            field = "normalized",
                            before = "APFEL",
                            after = "apfel"
                        )
                    )
                )
            ),
            duplicateGroups = listOf(group),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = emptyList()
        )

        assertEquals(
            CatalogCanonicalizationAction.MERGE,
            plan.entries.single { it.sourceIndex == 1 }.action
        )
    }

    @Test
    fun createSplitActionForCombinedFoodName() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 30,
                itemName = "Apfel und Birne",
                normalized = "apfel-und-birne",
                category = "fruit"
            )
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 30,
                    originalItemName = "Apfel und Birne",
                    originalNormalized = "apfel-und-birne",
                    canonicalName = "Apfel und Birne",
                    normalizedKey = "apfel-und-birne"
                )
            ),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = emptyList()
        )

        val planEntry = plan.entries.single()

        assertEquals(
            CatalogCanonicalizationAction.SPLIT,
            planEntry.action
        )

        assertFalse(planEntry.automatic)
        assertTrue(planEntry.reasons.any {
            it.contains("Apfel") &&
                    it.contains("Birne")
        })
    }

    @Test
    fun protectedCombinedExpressionIsNotSplit() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 31,
                itemName = "Salz und Pfeffer",
                normalized = "salz-und-pfeffer",
                category = "spices"
            )
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 31,
                    originalItemName = "Salz und Pfeffer",
                    originalNormalized = "salz-und-pfeffer",
                    canonicalName = "Salz und Pfeffer",
                    normalizedKey = "salz-und-pfeffer"
                )
            ),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = emptyList()
        )

        assertEquals(
            CatalogCanonicalizationAction.KEEP,
            plan.entries.single().action
        )
    }

    @Test
    fun produceExactlyOnePlanEntryPerInputEntry() {
        val entries = listOf(
            indexedItem(4, "Apfel", "apfel", "fruit"),
            indexedItem(1, "Birne", "birne", "fruit"),
            indexedItem(9, "Tomate", "tomate", "vegetables")
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = entries.map {
                normalization(
                    sourceIndex = it.sourceIndex,
                    originalItemName = it.item.itemname,
                    originalNormalized = it.item.normalized,
                    canonicalName = it.item.itemname,
                    normalizedKey = requireNotNull(
                        it.item.normalized
                    )
                )
            },
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = emptyList()
        )

        assertEquals(entries.size, plan.entries.size)
        assertEquals(entries.size, plan.planEntryCount)

        assertEquals(
            entries.map { it.sourceIndex }.sorted(),
            plan.entries.map { it.sourceIndex }
        )

        assertEquals(
            entries.size,
            plan.entries.map { it.sourceIndex }.distinct().size
        )
    }

    @Test
    fun calculateActionCountsConsistently() {
        val entries = listOf(
            indexedItem(0, "Apfel", "apfel", "fruit"),
            indexedItem(1, "Birne", "BIRNE", "fruit"),
            indexedItem(2, "Spülmittel", "spuelmittel", "cleaning")
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Apfel",
                originalNormalized = "apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "Birne",
                originalNormalized = "BIRNE",
                canonicalName = "Birne",
                normalizedKey = "birne",
                changes = listOf(
                    normalizationChange(
                        type = CatalogNormalizationChangeType
                            .RECOMPUTED_NORMALIZED_KEY,
                        field = "normalized",
                        before = "BIRNE",
                        after = "birne"
                    )
                )
            ),
            normalization(
                sourceIndex = 2,
                originalItemName = "Spülmittel",
                originalNormalized = "spuelmittel",
                canonicalName = "Spülmittel",
                normalizedKey = "spuelmittel"
            )
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = normalizations,
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = listOf(
                nonFoodCandidate(
                    sourceIndex = 2,
                    itemName = "Spülmittel",
                    category = "cleaning",
                    confidence = 0.99,
                    recommendation =
                        NonFoodRecommendation.REMOVE_AUTOMATICALLY
                )
            )
        )

        assertEquals(
            plan.entries.size,
            plan.actionCounts.values.sum()
        )

        assertEquals(
            plan.entries.count { it.automatic },
            plan.automaticActionCount
        )

        assertEquals(
            plan.entries.count {
                it.action == CatalogCanonicalizationAction.REVIEW
            },
            plan.reviewActionCount
        )

        assertEquals(
            plan.entries.count {
                it.action == CatalogCanonicalizationAction.KEEP
            },
            plan.unchangedEntryCount
        )
    }

    @Test
    fun affectedSourceIndicesContainOnlyNonKeepEntries() {
        val entries = listOf(
            indexedItem(0, "Apfel", "apfel", "fruit"),
            indexedItem(1, "Birne", "BIRNE", "fruit")
        )

        val plan = planner.createPlan(
            entries = entries,
            normalizations = listOf(
                normalization(
                    sourceIndex = 0,
                    originalItemName = "Apfel",
                    originalNormalized = "apfel",
                    canonicalName = "Apfel",
                    normalizedKey = "apfel"
                ),
                normalization(
                    sourceIndex = 1,
                    originalItemName = "Birne",
                    originalNormalized = "BIRNE",
                    canonicalName = "Birne",
                    normalizedKey = "birne",
                    changes = listOf(
                        normalizationChange(
                            type = CatalogNormalizationChangeType
                                .RECOMPUTED_NORMALIZED_KEY,
                            field = "normalized",
                            before = "BIRNE",
                            after = "birne"
                        )
                    )
                )
            ),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(entries),
            languageResult = languageResult(entries),
            nonFoodCandidates = emptyList()
        )

        assertEquals(
            listOf(1),
            plan.affectedSourceIndices
        )
    }

    @Test
    fun planIsDeterministic() {
        val entries = listOf(
            indexedItem(5, "Paprika rot", "Paprika_rot", "vegetables"),
            indexedItem(1, "Apfel", "apfel", "fruit")
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 5,
                originalItemName = "Paprika rot",
                originalNormalized = "Paprika_rot",
                canonicalName = "Paprika rot",
                normalizedKey = "paprika-rot",
                changes = listOf(
                    normalizationChange(
                        type = CatalogNormalizationChangeType
                            .RECOMPUTED_NORMALIZED_KEY,
                        field = "normalized",
                        before = "Paprika_rot",
                        after = "paprika-rot"
                    )
                )
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "Apfel",
                originalNormalized = "apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            )
        )

        val first = planner.createPlan(
            entries,
            normalizations,
            emptyList(),
            categoryResult(entries),
            languageResult(entries),
            emptyList()
        )

        val second = planner.createPlan(
            entries,
            normalizations,
            emptyList(),
            categoryResult(entries),
            languageResult(entries),
            emptyList()
        )

        assertEquals(first, second)
    }

    @Test
    fun planIsIndependentOfInputOrder() {
        val entries = listOf(
            indexedItem(5, "Paprika rot", "Paprika_rot", "vegetables"),
            indexedItem(1, "Apfel", "apfel", "fruit")
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 5,
                originalItemName = "Paprika rot",
                originalNormalized = "Paprika_rot",
                canonicalName = "Paprika rot",
                normalizedKey = "paprika-rot",
                changes = listOf(
                    normalizationChange(
                        type = CatalogNormalizationChangeType
                            .RECOMPUTED_NORMALIZED_KEY,
                        field = "normalized",
                        before = "Paprika_rot",
                        after = "paprika-rot"
                    )
                )
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "Apfel",
                originalNormalized = "apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            )
        )

        val forward = planner.createPlan(
            entries,
            normalizations,
            emptyList(),
            categoryResult(entries),
            languageResult(entries),
            emptyList()
        )

        val reversed = planner.createPlan(
            entries.reversed(),
            normalizations.reversed(),
            emptyList(),
            categoryResult(entries.reversed()),
            languageResult(entries.reversed()),
            emptyList()
        )

        assertEquals(forward, reversed)
    }

    @Test
    fun rejectDuplicateEntrySourceIndices() {
        val entries = listOf(
            indexedItem(2, "Apfel", "apfel", "fruit"),
            indexedItem(2, "Birne", "birne", "fruit")
        )

        assertFailsWith<IllegalArgumentException> {
            planner.createPlan(
                entries = entries,
                normalizations = listOf(
                    normalization(
                        sourceIndex = 2,
                        originalItemName = "Apfel",
                        originalNormalized = "apfel",
                        canonicalName = "Apfel",
                        normalizedKey = "apfel"
                    )
                ),
                duplicateGroups = emptyList(),
                categoryResult = categoryResult(entries),
                languageResult = languageResult(entries),
                nonFoodCandidates = emptyList()
            )
        }
    }

    @Test
    fun rejectMissingNormalization() {
        val entries = listOf(
            indexedItem(0, "Apfel", "apfel", "fruit"),
            indexedItem(1, "Birne", "birne", "fruit")
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            planner.createPlan(
                entries = entries,
                normalizations = listOf(
                    normalization(
                        sourceIndex = 0,
                        originalItemName = "Apfel",
                        originalNormalized = "apfel",
                        canonicalName = "Apfel",
                        normalizedKey = "apfel"
                    )
                ),
                duplicateGroups = emptyList(),
                categoryResult = categoryResult(entries),
                languageResult = languageResult(entries),
                nonFoodCandidates = emptyList()
            )
        }

        assertTrue(exception.message?.contains("Missing") == true)
        assertTrue(exception.message?.contains("1") == true)
    }

    @Test
    fun rejectUnexpectedNormalization() {
        val entries = listOf(
            indexedItem(0, "Apfel", "apfel", "fruit")
        )

        assertFailsWith<IllegalArgumentException> {
            planner.createPlan(
                entries = entries,
                normalizations = listOf(
                    normalization(
                        sourceIndex = 0,
                        originalItemName = "Apfel",
                        originalNormalized = "apfel",
                        canonicalName = "Apfel",
                        normalizedKey = "apfel"
                    ),
                    normalization(
                        sourceIndex = 7,
                        originalItemName = "Birne",
                        originalNormalized = "birne",
                        canonicalName = "Birne",
                        normalizedKey = "birne"
                    )
                ),
                duplicateGroups = emptyList(),
                categoryResult = categoryResult(entries),
                languageResult = languageResult(entries),
                nonFoodCandidates = emptyList()
            )
        }
    }

    @Test
    fun rejectNonFoodCandidateForUnknownEntry() {
        val entries = listOf(
            indexedItem(0, "Apfel", "apfel", "fruit")
        )

        assertFailsWith<IllegalArgumentException> {
            planner.createPlan(
                entries = entries,
                normalizations = listOf(
                    normalization(
                        sourceIndex = 0,
                        originalItemName = "Apfel",
                        originalNormalized = "apfel",
                        canonicalName = "Apfel",
                        normalizedKey = "apfel"
                    )
                ),
                duplicateGroups = emptyList(),
                categoryResult = categoryResult(entries),
                languageResult = languageResult(entries),
                nonFoodCandidates = listOf(
                    nonFoodCandidate(
                        sourceIndex = 9,
                        itemName = "Spülmittel",
                        category = "cleaning",
                        confidence = 0.99,
                        recommendation =
                            NonFoodRecommendation.REMOVE_AUTOMATICALLY
                    )
                )
            )
        }
    }

    @Test
    fun rejectDuplicateMembershipAcrossDuplicateGroups() {
        val entries = listOf(
            indexedItem(0, "Apfel", "apfel", "fruit"),
            indexedItem(1, "APFEL", "apfel", "fruit"),
            indexedItem(2, "Apfel alt", "apfel", "fruit")
        )

        val firstGroup = duplicateGroup(
            groupId = "group-a",
            canonicalSourceIndex = 0,
            canonicalName = "Apfel",
            recommendation =
                CatalogDuplicateRecommendation.MERGE_AUTOMATICALLY,
            members = listOf(
                duplicateCandidate(0, "Apfel", "apfel", 1.0),
                duplicateCandidate(1, "APFEL", "apfel", 0.99)
            )
        )

        val secondGroup = duplicateGroup(
            groupId = "group-b",
            canonicalSourceIndex = 1,
            canonicalName = "APFEL",
            recommendation =
                CatalogDuplicateRecommendation.MERGE_AFTER_REVIEW,
            members = listOf(
                duplicateCandidate(1, "APFEL", "apfel", 1.0),
                duplicateCandidate(2, "Apfel alt", "apfel", 0.80)
            )
        )

        assertFailsWith<IllegalArgumentException> {
            planner.createPlan(
                entries = entries,
                normalizations = entries.map {
                    normalization(
                        sourceIndex = it.sourceIndex,
                        originalItemName = it.item.itemname,
                        originalNormalized = it.item.normalized,
                        canonicalName = "Apfel",
                        normalizedKey = "apfel"
                    )
                },
                duplicateGroups = listOf(firstGroup, secondGroup),
                categoryResult = categoryResult(entries),
                languageResult = languageResult(entries),
                nonFoodCandidates = emptyList()
            )
        }
    }

    @Test
    fun rejectDuplicateGroupWithoutCanonicalMember() {
        val entries = listOf(
            indexedItem(0, "Apfel", "apfel", "fruit"),
            indexedItem(1, "APFEL", "apfel", "fruit")
        )

        val invalidGroup = duplicateGroup(
            canonicalSourceIndex = 7,
            canonicalName = "Apfel",
            recommendation =
                CatalogDuplicateRecommendation.MERGE_AUTOMATICALLY,
            members = listOf(
                duplicateCandidate(0, "Apfel", "apfel", 1.0),
                duplicateCandidate(1, "APFEL", "apfel", 0.99)
            )
        )

        assertFailsWith<IllegalArgumentException> {
            planner.createPlan(
                entries = entries,
                normalizations = entries.map {
                    normalization(
                        sourceIndex = it.sourceIndex,
                        originalItemName = it.item.itemname,
                        originalNormalized = it.item.normalized,
                        canonicalName = "Apfel",
                        normalizedKey = "apfel"
                    )
                },
                duplicateGroups = listOf(invalidGroup),
                categoryResult = categoryResult(entries),
                languageResult = languageResult(entries),
                nonFoodCandidates = emptyList()
            )
        }
    }

    @Test
    fun emptyInputProducesValidEmptyPlan() {
        val plan = planner.createPlan(
            entries = emptyList(),
            normalizations = emptyList(),
            duplicateGroups = emptyList(),
            categoryResult = categoryResult(emptyList()),
            languageResult = languageResult(emptyList()),
            nonFoodCandidates = emptyList()
        )

        assertEquals(0, plan.inputEntryCount)
        assertEquals(0, plan.planEntryCount)
        assertEquals(0, plan.automaticActionCount)
        assertEquals(0, plan.reviewActionCount)
        assertEquals(0, plan.unchangedEntryCount)
        assertTrue(plan.actionCounts.isEmpty())
        assertTrue(plan.affectedSourceIndices.isEmpty())
        assertTrue(plan.entries.isEmpty())
        assertTrue(plan.valid)
    }

    private fun indexedItem(
        sourceIndex: Int,
        itemName: String,
        normalized: String?,
        category: String?
    ): IndexedCatalogFoodItem =
        IndexedCatalogFoodItem(
            sourceIndex = sourceIndex,
            item = CatalogFoodItem(
                itemname = itemName,
                category = category,
                production = null,
                normalized = normalized,
                plural = null,
                colloquial = emptyList(),
                phoneticTokens = emptyList(),
                autocompleteTokens = emptyList(),
                normalizedEnglish = null
            )
        )

    private fun normalization(
        sourceIndex: Int,
        originalItemName: String,
        originalNormalized: String?,
        canonicalName: String,
        normalizedKey: String,
        changes: List<CatalogNormalizationChange> = emptyList()
    ): CatalogNormalizationResult =
        CatalogNormalizationResult(
            sourceIndex = sourceIndex,
            originalItemName = originalItemName,
            originalNormalized = originalNormalized,
            originalPlural = null,
            originalColloquial = emptyList(),
            originalPhoneticTokens = emptyList(),
            originalAutocompleteTokens = emptyList(),
            computedCanonicalName = canonicalName,
            computedNormalizedKey = normalizedKey,
            computedPlural = null,
            normalizedColloquial = emptyList(),
            normalizedPhoneticTokens = emptyList(),
            normalizedAutocompleteTokens = emptyList(),
            changes = changes
        )

    private fun normalizationChange(
        type: CatalogNormalizationChangeType,
        field: String,
        before: String?,
        after: String?,
        reason: String = "Test normalization change."
    ): CatalogNormalizationChange =
        CatalogNormalizationChange(
            type = type,
            field = field,
            before = before,
            after = after,
            reason = reason
        )

    private fun duplicateGroup(
        groupId: String = "duplicate-group",
        canonicalSourceIndex: Int,
        canonicalName: String,
        recommendation: CatalogDuplicateRecommendation,
        members: List<CatalogDuplicateCandidate>
    ): CatalogDuplicateGroup =
        CatalogDuplicateGroup(
            groupId = groupId,
            canonicalCandidateSourceIndex =
                canonicalSourceIndex,
            canonicalCandidateName = canonicalName,
            members = members,
            confidence = members
                .map { it.matchScore }
                .average(),
            recommendation = recommendation
        )

    private fun duplicateCandidate(
        sourceIndex: Int,
        itemName: String,
        normalizedKey: String,
        score: Double
    ): CatalogDuplicateCandidate =
        CatalogDuplicateCandidate(
            sourceIndex = sourceIndex,
            itemName = itemName,
            normalizedKey = normalizedKey,
            matchScore = score,
            reasons = setOf(
                CatalogDuplicateReason.IDENTICAL_NORMALIZED_KEY
            )
        )

    private fun categoryIssue(
        sourceIndex: Int?,
        itemName: String?,
        type: CatalogCategoryIssueType,
        severity: CatalogIssueSeverity,
        originalCategory: String?,
        normalizedCategory: String?,
        suggestedCategoryKey: String?,
        message: String
    ): CatalogCategoryIssue =
        CatalogCategoryIssue(
            type = type,
            severity = severity,
            sourceIndex = sourceIndex,
            itemName = itemName,
            originalCategory = originalCategory,
            normalizedCategory = normalizedCategory,
            suggestedCategoryKey = suggestedCategoryKey,
            message = message
        )

    private fun languageIssue(
        sourceIndex: Int,
        itemName: String,
        type: CatalogLanguageIssueType,
        severity: CatalogIssueSeverity,
        field: String,
        originalValue: String?,
        suggestedValue: String?,
        matchedTerms: List<String> = emptyList(),
        message: String
    ): CatalogLanguageIssue =
        CatalogLanguageIssue(
            type = type,
            severity = severity,
            sourceIndex = sourceIndex,
            itemName = itemName,
            field = field,
            originalValue = originalValue,
            suggestedValue = suggestedValue,
            matchedTerms = matchedTerms.sorted(),
            message = message
        )

    private fun nonFoodCandidate(
        sourceIndex: Int,
        itemName: String,
        category: String?,
        confidence: Double,
        recommendation: NonFoodRecommendation
    ): CatalogNonFoodCandidate =
        CatalogNonFoodCandidate(
            sourceIndex = sourceIndex,
            itemName = itemName,
            category = category,
            reasons = setOf(
                CatalogNonFoodReason.CLEANING
            ),
            matchedTerms = setOf(
                itemName.lowercase()
            ),
            confidence = confidence,
            recommendation = recommendation
        )

    private fun categoryResult(
        entries: List<IndexedCatalogFoodItem>,
        issues: List<CatalogCategoryIssue> = emptyList(),
        categoryCounts: Map<String, Int>? = null,
        unknownCategoryCounts: Map<String, Int> = emptyMap(),
        suggestedMappings: Map<String, String> = emptyMap()
    ): CatalogCategoryValidationResult {
        val computedCategoryCounts = categoryCounts
            ?: entries
                .mapNotNull { it.item.category }
                .filter(String::isNotBlank)
                .filterNot {
                    it in unknownCategoryCounts.keys
                }
                .groupingBy { it }
                .eachCount()
                .toSortedMap()

        val missingCategoryCount = entries.count {
            it.item.category.isNullOrBlank()
        }

        val unknownCategoryCount =
            unknownCategoryCounts.values.sum()

        val categorizedEntryCount =
            entries.size - missingCategoryCount

        val validCategoryCount =
            categorizedEntryCount - unknownCategoryCount

        return CatalogCategoryValidationResult(
            inputEntryCount = entries.size,
            categorizedEntryCount = categorizedEntryCount,
            missingCategoryCount = missingCategoryCount,
            unknownCategoryCount = unknownCategoryCount,
            validCategoryCount = validCategoryCount,
            uniqueAssignedCategoryCount =
                computedCategoryCounts.size,
            rootCategoryAssignmentCount = 0,
            categoryCounts =
                computedCategoryCounts.toSortedMap(),
            unknownCategoryCounts =
                unknownCategoryCounts.toSortedMap(),
            suggestedCategoryMappings =
                suggestedMappings.toSortedMap(),
            unusedCategoryKeys = emptyList(),
            issues = issues.sortedWith(
                compareBy<CatalogCategoryIssue>(
                    { it.severity.name },
                    { it.type.name },
                    { it.sourceIndex ?: Int.MAX_VALUE },
                    { it.message }
                )
            ),
            valid = issues.none {
                it.severity == CatalogIssueSeverity.ERROR
            }
        )
    }

    private fun languageResult(
        entries: List<IndexedCatalogFoodItem>,
        issues: List<CatalogLanguageIssue> = emptyList()
    ): CatalogLanguageValidationResult {
        val sortedIssues = issues.sortedWith(
            compareBy<CatalogLanguageIssue>(
                { severityRank(it.severity) },
                { it.type.name },
                { it.sourceIndex },
                { it.field },
                { it.message }
            )
        )

        val affectedSourceIndices = sortedIssues
            .map { it.sourceIndex }
            .distinct()
            .sorted()

        return CatalogLanguageValidationResult(
            inputEntryCount = entries.size,
            affectedEntryCount =
                affectedSourceIndices.size,
            issueCount = sortedIssues.size,
            errorCount = sortedIssues.count {
                it.severity == CatalogIssueSeverity.ERROR
            },
            warningCount = sortedIssues.count {
                it.severity == CatalogIssueSeverity.WARNING
            },
            infoCount = sortedIssues.count {
                it.severity == CatalogIssueSeverity.INFO
            },
            issueCountsByType = sortedIssues
                .groupingBy { it.type }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it },
            issueCountsByField = sortedIssues
                .groupingBy { it.field }
                .eachCount()
                .toSortedMap(),
            affectedSourceIndices =
                affectedSourceIndices,
            issues = sortedIssues,
            valid = sortedIssues.none {
                it.severity == CatalogIssueSeverity.ERROR
            }
        )
    }

    private fun severityRank(
        severity: CatalogIssueSeverity
    ): Int =
        when (severity) {
            CatalogIssueSeverity.ERROR -> 0
            CatalogIssueSeverity.WARNING -> 1
            CatalogIssueSeverity.INFO -> 2
        }
}