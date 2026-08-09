package de.shopme.testing.system.tools.knowledge.catalog.review

import de.shopme.testing.system.tools.knowledge.catalog.application.CatalogCanonicalizationApplicationEntry
import de.shopme.testing.system.tools.knowledge.catalog.application.CatalogCanonicalizationApplicationResult
import de.shopme.testing.system.tools.knowledge.catalog.application.CatalogCanonicalizationApplicationStatus
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlan
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlanEntry
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CanonicalFoodKeyNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogReviewBacklogEvaluatorTest {

    private val keyNormalizer =
        CanonicalFoodKeyNormalizer()

    private val categoryRegistry =
        CanonicalFoodCategoryRegistry()

    private val evaluator =
        CatalogReviewBacklogEvaluator(
            keyNormalizer = keyNormalizer
        )

    @Test
    fun classifyResolvedDuplicateMerge() {
        val sourceIndex = 10
        val mergeTargetSourceIndex = 4

        val sourceEntry =
            indexedItem(
                sourceIndex = sourceIndex,
                itemName = "Erbsen, Dose",
                category = "Konserven",
                normalizedKey = "erbsen dose"
            )

        val normalization =
            normalization(
                sourceIndex = sourceIndex,
                originalItemName = "Erbsen, Dose",
                originalNormalizedKey = "erbsen dose",
                canonicalName = "Erbsen, Dose",
                canonicalKey = "erbsen-dose",
                plural = "Erbsen"
            )

        val planEntry =
            planEntry(
                sourceIndex = sourceIndex,
                originalItemName = "Erbsen, Dose",
                action =
                    CatalogCanonicalizationAction.MERGE,
                mergeTargetSourceIndex =
                    mergeTargetSourceIndex,
                reasons =
                    listOf(
                        "Possible semantic duplicate requires manual review."
                    )
            )

        val applicationEntry =
            applicationEntry(
                sourceIndex = sourceIndex,
                originalItemName = "Erbsen, Dose",
                action =
                    CatalogCanonicalizationAction.MERGE,
                status =
                    CatalogCanonicalizationApplicationStatus
                        .MERGED_INTO_TARGET,
                resultingNormalizedKey = null,
                resultingCategory = null,
                mergeTargetSourceIndex =
                    mergeTargetSourceIndex,
                reasons =
                    listOf(
                        "Deterministic duplicate resolution.",
                        "Possible semantic duplicate requires manual review."
                    )
            )

        val result =
            evaluator.evaluate(
                sourceEntries = listOf(sourceEntry),
                normalizations = listOf(normalization),
                plan = plan(planEntry),
                applicationResult =
                    applicationResult(
                        applicationEntry =
                            applicationEntry,
                        outputItems =
                            emptyList()
                    ),
                categoryRegistry =
                    categoryRegistry
            )

        assertEquals(
            1,
            result.evaluatedBacklogEntryCount
        )

        assertEquals(
            1,
            result.resolvedEntryCount
        )

        assertEquals(
            0,
            result.unresolvedEntryCount
        )

        assertEquals(
            1,
            result.resolvedByDuplicateMergeCount
        )

        assertEquals(
            CatalogReviewBacklogStatus
                .RESOLVED_BY_DUPLICATE_MERGE,
            result.entries.single().status
        )

        assertEquals(
            mergeTargetSourceIndex,
            result.entries.single()
                .mergeTargetSourceIndex
        )

        assertTrue(
            result.entries.single()
                .resolutionReason
                .contains(
                    mergeTargetSourceIndex.toString()
                )
        )
    }

    @Test
    fun classifyResolvedCategoryMigration() {
        val sourceIndex = 20

        val sourceEntry =
            indexedItem(
                sourceIndex = sourceIndex,
                itemName = "Vollkornbrot",
                category = "Backwaren",
                normalizedKey = "vollkornbrot"
            )

        val normalization =
            normalization(
                sourceIndex = sourceIndex,
                originalItemName = "Vollkornbrot",
                originalNormalizedKey = "vollkornbrot",
                canonicalName = "Vollkornbrot",
                canonicalKey = "vollkornbrot",
                plural = "Vollkornbrote"
            )

        val planEntry =
            planEntry(
                sourceIndex = sourceIndex,
                originalItemName = "Vollkornbrot",
                action =
                    CatalogCanonicalizationAction
                        .MOVE_CATEGORY,
                proposedCategory = "bakery",
                reasons =
                    listOf(
                        "Legacy category requires category mapping."
                    )
            )

        val applicationEntry =
            applicationEntry(
                sourceIndex = sourceIndex,
                originalItemName = "Vollkornbrot",
                action =
                    CatalogCanonicalizationAction
                        .MOVE_CATEGORY,
                status =
                    CatalogCanonicalizationApplicationStatus
                        .SKIPPED_REVIEW_REQUIRED,
                resultingNormalizedKey =
                    "vollkornbrot",
                resultingCategory =
                    "bakery",
                mergeTargetSourceIndex =
                    null,
                reasons =
                    listOf(
                        "Legacy category requires category mapping."
                    )
            )

        val resultingItem =
            catalogItem(
                itemName = "Vollkornbrot",
                category = "bakery",
                normalizedKey = "vollkornbrot",
                plural = "Vollkornbrote"
            )

        val result =
            evaluator.evaluate(
                sourceEntries = listOf(sourceEntry),
                normalizations = listOf(normalization),
                plan = plan(planEntry),
                applicationResult =
                    applicationResult(
                        applicationEntry =
                            applicationEntry,
                        outputItems =
                            listOf(resultingItem)
                    ),
                categoryRegistry =
                    categoryRegistry
            )

        assertEquals(
            CatalogReviewBacklogStatus
                .RESOLVED_BY_CATEGORY_MIGRATION,
            result.entries.single().status
        )

        assertEquals(
            1,
            result.resolvedByCategoryMigrationCount
        )

        assertEquals(
            "Backwaren",
            result.entries.single().originalCategory
        )

        assertEquals(
            "bakery",
            result.entries.single().resultingCategory
        )

        assertTrue(
            result.entries.single()
                .resolutionReason
                .contains("Backwaren")
        )

        assertTrue(
            result.entries.single()
                .resolutionReason
                .contains("bakery")
        )
    }

    @Test
    fun classifyResolvedTechnicalNormalization() {
        val sourceIndex = 30

        val sourceEntry =
            indexedItem(
                sourceIndex = sourceIndex,
                itemName = "  Pommes   Frites  ",
                category = "snacks",
                normalizedKey = "pommes frites"
            )

        val normalization =
            normalization(
                sourceIndex = sourceIndex,
                originalItemName =
                    "  Pommes   Frites  ",
                originalNormalizedKey =
                    "pommes frites",
                canonicalName =
                    "Pommes Frites",
                canonicalKey =
                    "pommes-frites",
                plural =
                    "Pommes Frites"
            )

        val planEntry =
            planEntry(
                sourceIndex = sourceIndex,
                originalItemName =
                    "  Pommes   Frites  ",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                reasons =
                    listOf(
                        "Normalized key and whitespace require normalization."
                    )
            )

        val applicationEntry =
            applicationEntry(
                sourceIndex = sourceIndex,
                originalItemName =
                    "  Pommes   Frites  ",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                status =
                    CatalogCanonicalizationApplicationStatus
                        .SKIPPED_REVIEW_REQUIRED,
                resultingNormalizedKey =
                    "pommes-frites",
                resultingCategory =
                    "snacks",
                mergeTargetSourceIndex =
                    null,
                reasons =
                    listOf(
                        "Normalized key and whitespace require normalization."
                    )
            )

        val resultingItem =
            catalogItem(
                itemName = "Pommes Frites",
                category = "snacks",
                normalizedKey = "pommes-frites",
                plural = "Pommes Frites"
            )

        val result =
            evaluator.evaluate(
                sourceEntries = listOf(sourceEntry),
                normalizations = listOf(normalization),
                plan = plan(planEntry),
                applicationResult =
                    applicationResult(
                        applicationEntry =
                            applicationEntry,
                        outputItems =
                            listOf(resultingItem)
                    ),
                categoryRegistry =
                    categoryRegistry
            )

        assertEquals(
            CatalogReviewBacklogStatus
                .RESOLVED_BY_NORMALIZATION,
            result.entries.single().status
        )

        assertEquals(
            1,
            result.resolvedByNormalizationCount
        )

        assertEquals(
            "pommes-frites",
            result.entries.single()
                .resultingNormalizedKey
        )

        assertEquals(
            0,
            result.stillReviewRequiredCount
        )
    }

    @Test
    fun preserveSemanticReview() {
        val sourceIndex = 40

        val sourceEntry =
            indexedItem(
                sourceIndex = sourceIndex,
                itemName = "Vegane Butter",
                category = "Vegan",
                normalizedKey = "vegane butter"
            )

        val normalization =
            normalization(
                sourceIndex = sourceIndex,
                originalItemName = "Vegane Butter",
                originalNormalizedKey =
                    "vegane butter",
                canonicalName = "Vegane Butter",
                canonicalKey = "vegane-butter",
                plural = "Vegane Butter"
            )

        val planEntry =
            planEntry(
                sourceIndex = sourceIndex,
                originalItemName = "Vegane Butter",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                reasons =
                    listOf(
                        "Manual review required for ambiguous semantic classification."
                    )
            )

        val applicationEntry =
            applicationEntry(
                sourceIndex = sourceIndex,
                originalItemName = "Vegane Butter",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                status =
                    CatalogCanonicalizationApplicationStatus
                        .SKIPPED_REVIEW_REQUIRED,
                resultingNormalizedKey =
                    "vegane-butter",
                resultingCategory =
                    "plant-based-alternatives",
                mergeTargetSourceIndex =
                    null,
                reasons =
                    listOf(
                        "Manual review required for ambiguous semantic classification."
                    )
            )

        val resultingItem =
            catalogItem(
                itemName = "Vegane Butter",
                category =
                    "plant-based-alternatives",
                normalizedKey =
                    "vegane-butter",
                plural =
                    "Vegane Butter"
            )

        val result =
            evaluator.evaluate(
                sourceEntries = listOf(sourceEntry),
                normalizations = listOf(normalization),
                plan = plan(planEntry),
                applicationResult =
                    applicationResult(
                        applicationEntry =
                            applicationEntry,
                        outputItems =
                            listOf(resultingItem)
                    ),
                categoryRegistry =
                    categoryRegistry
            )

        assertEquals(
            CatalogReviewBacklogStatus
                .STILL_REVIEW_REQUIRED,
            result.entries.single().status
        )

        assertEquals(
            0,
            result.resolvedEntryCount
        )

        assertEquals(
            1,
            result.unresolvedEntryCount
        )

        assertEquals(
            1,
            result.stillReviewRequiredCount
        )

        assertTrue(
            result.entries.single()
                .resolutionReason
                .contains("REVIEW")
        )

        assertTrue(
            result.entries.single()
                .resolutionReason
                .contains("ambiguous semantic")
        )
    }

    @Test
    fun preserveSplitAction() {
        val sourceIndex = 50

        val sourceEntry =
            indexedItem(
                sourceIndex = sourceIndex,
                itemName = "Obst und Gemüse Mix",
                category = "vegetables",
                normalizedKey =
                    "obst-und-gemuese-mix"
            )

        val normalization =
            normalization(
                sourceIndex = sourceIndex,
                originalItemName =
                    "Obst und Gemüse Mix",
                originalNormalizedKey =
                    "obst-und-gemuese-mix",
                canonicalName =
                    "Obst und Gemüse Mix",
                canonicalKey =
                    "obst-und-gemuese-mix",
                plural =
                    "Obst- und Gemüsemischungen"
            )

        val planEntry =
            planEntry(
                sourceIndex = sourceIndex,
                originalItemName =
                    "Obst und Gemüse Mix",
                action =
                    CatalogCanonicalizationAction.SPLIT,
                reasons =
                    listOf(
                        "Entry contains multiple foods and requires split."
                    )
            )

        val applicationEntry =
            applicationEntry(
                sourceIndex = sourceIndex,
                originalItemName =
                    "Obst und Gemüse Mix",
                action =
                    CatalogCanonicalizationAction.SPLIT,
                status =
                    CatalogCanonicalizationApplicationStatus
                        .SKIPPED_REVIEW_REQUIRED,
                resultingNormalizedKey =
                    "obst-und-gemuese-mix",
                resultingCategory =
                    "vegetables",
                mergeTargetSourceIndex =
                    null,
                reasons =
                    listOf(
                        "Entry contains multiple foods and requires split."
                    )
            )

        val resultingItem =
            catalogItem(
                itemName = "Obst und Gemüse Mix",
                category = "vegetables",
                normalizedKey =
                    "obst-und-gemuese-mix",
                plural =
                    "Obst- und Gemüsemischungen"
            )

        val result =
            evaluator.evaluate(
                sourceEntries = listOf(sourceEntry),
                normalizations = listOf(normalization),
                plan = plan(planEntry),
                applicationResult =
                    applicationResult(
                        applicationEntry =
                            applicationEntry,
                        outputItems =
                            listOf(resultingItem)
                    ),
                categoryRegistry =
                    categoryRegistry
            )

        assertEquals(
            CatalogReviewBacklogStatus
                .STILL_SPLIT_REQUIRED,
            result.entries.single().status
        )

        assertEquals(
            1,
            result.stillSplitRequiredCount
        )

        assertEquals(
            1,
            result.unresolvedEntryCount
        )

        assertEquals(
            0,
            result.resolvedEntryCount
        )
    }

    @Test
    fun evaluateDeterministically() {
        val fixtures =
            createMixedFixtures()

        val first =
            evaluator.evaluate(
                sourceEntries =
                    fixtures.sourceEntries,
                normalizations =
                    fixtures.normalizations,
                plan =
                    fixtures.plan,
                applicationResult =
                    fixtures.applicationResult,
                categoryRegistry =
                    categoryRegistry
            )

        val second =
            evaluator.evaluate(
                sourceEntries =
                    fixtures.sourceEntries,
                normalizations =
                    fixtures.normalizations,
                plan =
                    fixtures.plan,
                applicationResult =
                    fixtures.applicationResult,
                categoryRegistry =
                    categoryRegistry
            )

        assertEquals(
            first,
            second,
            "Review backlog evaluation must be deterministic."
        )

        assertEquals(
            first.entries.sortedBy {
                it.sourceIndex
            },
            first.entries,
            "Backlog entries must be sorted by sourceIndex."
        )

        assertEquals(
            first.evaluatedBacklogEntryCount,
            first.statusCounts.values.sum()
        )

        assertEquals(
            first.evaluatedBacklogEntryCount,
            first.resolvedEntryCount +
                    first.unresolvedEntryCount
        )

        assertTrue(first.valid)
    }

    @Test
    fun resolvePureTechnicalLegacyCategoryMigration() {
        val sourceIndex = 60

        val sourceEntry =
            indexedItem(
                sourceIndex = sourceIndex,
                itemName = "Haferdrink",
                category = "Milchalternativen",
                normalizedKey = "haferdrink"
            )

        val normalization =
            normalization(
                sourceIndex = sourceIndex,
                originalItemName = "Haferdrink",
                originalNormalizedKey = "haferdrink",
                canonicalName = "Haferdrink",
                canonicalKey = "haferdrink",
                plural = "Haferdrinks"
            )

        val reasons =
            listOf(
                "Category 'Milchalternativen' contains characters that are not valid in a canonical category key.",
                "Category 'Milchalternativen' does not use canonical lowercase key formatting.",
                "Category 'Milchalternativen' is not part of the canonical food category registry.",
                "Category 'Milchalternativen' normalizes to 'milchalternativen'."
            )

        val planEntry =
            planEntry(
                sourceIndex = sourceIndex,
                originalItemName = "Haferdrink",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                proposedCategory =
                    "plant-based-drinks",
                reasons =
                    reasons
            )

        val applicationEntry =
            applicationEntry(
                sourceIndex = sourceIndex,
                originalItemName = "Haferdrink",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                status =
                    CatalogCanonicalizationApplicationStatus
                        .SKIPPED_REVIEW_REQUIRED,
                resultingNormalizedKey =
                    "haferdrink",
                resultingCategory =
                    "plant-based-drinks",
                mergeTargetSourceIndex =
                    null,
                reasons =
                    reasons
            )

        val resultingItem =
            catalogItem(
                itemName = "Haferdrink",
                category =
                    "plant-based-drinks",
                normalizedKey =
                    "haferdrink",
                plural =
                    "Haferdrinks"
            )

        val result =
            evaluator.evaluate(
                sourceEntries = listOf(sourceEntry),
                normalizations = listOf(normalization),
                plan = plan(planEntry),
                applicationResult =
                    applicationResult(
                        applicationEntry =
                            applicationEntry,
                        outputItems =
                            listOf(resultingItem)
                    ),
                categoryRegistry =
                    categoryRegistry
            )

        assertEquals(
            CatalogReviewBacklogStatus
                .RESOLVED_BY_CATEGORY_MIGRATION,
            result.entries.single().status
        )

        assertEquals(
            1,
            result.resolvedByCategoryMigrationCount
        )

        assertEquals(
            0,
            result.stillReviewRequiredCount
        )
    }

    @Test
    fun preserveSemanticCategoryConflictAfterLegacyMigration() {
        val sourceIndex = 61

        val sourceEntry =
            indexedItem(
                sourceIndex = sourceIndex,
                itemName = "Mozzarella",
                category = "Milchprodukte",
                normalizedKey = "mozzarella"
            )

        val normalization =
            normalization(
                sourceIndex = sourceIndex,
                originalItemName = "Mozzarella",
                originalNormalizedKey = "mozzarella",
                canonicalName = "Mozzarella",
                canonicalKey = "mozzarella",
                plural = "Mozzarella"
            )

        val reasons =
            listOf(
                "Catalog item 'Mozzarella' is assigned directly to root category 'dairy'. A more specific child category may be available.",
                "Catalog item 'Mozzarella' is assigned to 'dairy', but its name contains stronger lexical evidence for root category 'cheese'.",
                "Category 'Milchprodukte' uses the display name 'Milchprodukte' instead of canonical key 'dairy'."
            )

        val planEntry =
            planEntry(
                sourceIndex = sourceIndex,
                originalItemName = "Mozzarella",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                proposedCategory =
                    "dairy",
                reasons =
                    reasons
            )

        val applicationEntry =
            applicationEntry(
                sourceIndex = sourceIndex,
                originalItemName = "Mozzarella",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                status =
                    CatalogCanonicalizationApplicationStatus
                        .SKIPPED_REVIEW_REQUIRED,
                resultingNormalizedKey =
                    "mozzarella",
                resultingCategory =
                    "dairy",
                mergeTargetSourceIndex =
                    null,
                reasons =
                    reasons
            )

        val resultingItem =
            catalogItem(
                itemName = "Mozzarella",
                category = "dairy",
                normalizedKey = "mozzarella",
                plural = "Mozzarella"
            )

        val result =
            evaluator.evaluate(
                sourceEntries = listOf(sourceEntry),
                normalizations = listOf(normalization),
                plan = plan(planEntry),
                applicationResult =
                    applicationResult(
                        applicationEntry =
                            applicationEntry,
                        outputItems =
                            listOf(resultingItem)
                    ),
                categoryRegistry =
                    categoryRegistry
            )

        val entry =
            result.entries.single()

        assertEquals(
            CatalogReviewBacklogStatus
                .STILL_REVIEW_REQUIRED,
            entry.status
        )

        assertEquals(
            0,
            result.resolvedByCategoryMigrationCount
        )

        assertEquals(
            1,
            result.stillReviewRequiredCount
        )

        assertEquals(
            1,
            result.unresolvedEntryCount
        )

        assertTrue(
            entry.resolutionReason.contains(
                "stronger lexical evidence",
                ignoreCase = true
            ),
            "Semantic category conflict must remain visible in the backlog."
        )

        assertTrue(
            entry.resolutionReason.contains(
                "cheese",
                ignoreCase = true
            ),
            "Alternative category evidence must remain visible."
        )
    }

    private fun createMixedFixtures(): MixedFixtures {
        val technicalSource =
            indexedItem(
                sourceIndex = 2,
                itemName = "  Curry   Sauce ",
                category = "sauces",
                normalizedKey = "curry sauce"
            )

        val semanticSource =
            indexedItem(
                sourceIndex = 5,
                itemName = "Veganes Produkt",
                category = "Vegan",
                normalizedKey = "veganes produkt"
            )

        val technicalNormalization =
            normalization(
                sourceIndex = 2,
                originalItemName =
                    "  Curry   Sauce ",
                originalNormalizedKey =
                    "curry sauce",
                canonicalName =
                    "Curry Sauce",
                canonicalKey =
                    "curry-sauce",
                plural =
                    "Curry-Saucen"
            )

        val semanticNormalization =
            normalization(
                sourceIndex = 5,
                originalItemName =
                    "Veganes Produkt",
                originalNormalizedKey =
                    "veganes produkt",
                canonicalName =
                    "Veganes Produkt",
                canonicalKey =
                    "veganes-produkt",
                plural =
                    "Vegane Produkte"
            )

        val technicalPlanEntry =
            planEntry(
                sourceIndex = 2,
                originalItemName =
                    "  Curry   Sauce ",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                reasons =
                    listOf(
                        "Normalized key requires normalization."
                    )
            )

        val semanticPlanEntry =
            planEntry(
                sourceIndex = 5,
                originalItemName =
                    "Veganes Produkt",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                reasons =
                    listOf(
                        "Manual review required for ambiguous semantic classification."
                    )
            )

        val technicalApplicationEntry =
            applicationEntry(
                sourceIndex = 2,
                originalItemName =
                    "  Curry   Sauce ",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                status =
                    CatalogCanonicalizationApplicationStatus
                        .SKIPPED_REVIEW_REQUIRED,
                resultingNormalizedKey =
                    "curry-sauce",
                resultingCategory =
                    "sauces",
                mergeTargetSourceIndex =
                    null,
                reasons =
                    listOf(
                        "Normalized key requires normalization."
                    )
            )

        val semanticApplicationEntry =
            applicationEntry(
                sourceIndex = 5,
                originalItemName =
                    "Veganes Produkt",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                status =
                    CatalogCanonicalizationApplicationStatus
                        .SKIPPED_REVIEW_REQUIRED,
                resultingNormalizedKey =
                    "veganes-produkt",
                resultingCategory =
                    "plant-based-alternatives",
                mergeTargetSourceIndex =
                    null,
                reasons =
                    listOf(
                        "Manual review required for ambiguous semantic classification."
                    )
            )

        val technicalOutputItem =
            catalogItem(
                itemName = "Curry Sauce",
                category = "sauces",
                normalizedKey = "curry-sauce",
                plural = "Curry-Saucen"
            )

        val semanticOutputItem =
            catalogItem(
                itemName = "Veganes Produkt",
                category =
                    "plant-based-alternatives",
                normalizedKey =
                    "veganes-produkt",
                plural =
                    "Vegane Produkte"
            )

        return MixedFixtures(
            sourceEntries =
                listOf(
                    technicalSource,
                    semanticSource
                ),

            normalizations =
                listOf(
                    technicalNormalization,
                    semanticNormalization
                ),

            plan =
                plan(
                    technicalPlanEntry,
                    semanticPlanEntry
                ),

            applicationResult =
                applicationResult(
                    applicationEntries =
                        listOf(
                            technicalApplicationEntry,
                            semanticApplicationEntry
                        ),

                    outputItemsBySourceIndex =
                        linkedMapOf(
                            2 to technicalOutputItem,
                            5 to semanticOutputItem
                        )
                )
        )
    }

    private fun indexedItem(
        sourceIndex: Int,
        itemName: String,
        category: String,
        normalizedKey: String?
    ): IndexedCatalogFoodItem =
        IndexedCatalogFoodItem(
            sourceIndex = sourceIndex,
            item =
                catalogItem(
                    itemName = itemName,
                    category = category,
                    normalizedKey = normalizedKey,
                    plural = itemName.trim()
                )
        )

    private fun catalogItem(
        itemName: String,
        category: String,
        normalizedKey: String?,
        plural: String?
    ): CatalogFoodItem =
        CatalogFoodItem(
            itemname = itemName,
            category = category,
            production = "Standard",
            normalized = normalizedKey,
            plural = plural,
            colloquial = emptyList(),
            phoneticTokens = emptyList(),
            autocompleteTokens =
                normalizedKey
                    ?.let(::listOf)
                    ?: emptyList(),
            normalizedEnglish = null
        )

    private fun normalization(
        sourceIndex: Int,
        originalItemName: String,
        originalNormalizedKey: String?,
        canonicalName: String,
        canonicalKey: String,
        plural: String?
    ): CatalogNormalizationResult =
        CatalogNormalizationResult(
            sourceIndex = sourceIndex,
            originalItemName =
                originalItemName,
            originalNormalized =
                originalNormalizedKey,
            originalPlural = null,
            originalColloquial =
                emptyList(),
            originalPhoneticTokens =
                emptyList(),
            originalAutocompleteTokens =
                emptyList(),
            computedCanonicalName =
                canonicalName,
            computedNormalizedKey =
                canonicalKey,
            computedPlural =
                plural,
            normalizedColloquial =
                emptyList(),
            normalizedPhoneticTokens =
                emptyList(),
            normalizedAutocompleteTokens =
                listOf(canonicalKey),
            changes =
                emptyList()
        )

    private fun planEntry(
        sourceIndex: Int,
        originalItemName: String,
        action: CatalogCanonicalizationAction,
        proposedCategory: String? = null,
        mergeTargetSourceIndex: Int? = null,
        reasons: List<String>
    ): CatalogCanonicalizationPlanEntry =
        CatalogCanonicalizationPlanEntry(
            sourceIndex = sourceIndex,
            originalItemName =
                originalItemName,
            action = action,
            proposedCanonicalName = null,
            proposedNormalizedKey = null,
            proposedCategory =
                proposedCategory,
            mergeTargetSourceIndex =
                mergeTargetSourceIndex,
            automatic = false,
            reasons =
                reasons
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),
            confidence = 0.50
        )

    private fun plan(
        vararg entries:
        CatalogCanonicalizationPlanEntry
    ): CatalogCanonicalizationPlan {
        val sortedEntries =
            entries.sortedBy {
                it.sourceIndex
            }

        val actionCounts =
            sortedEntries
                .groupingBy {
                    it.action
                }
                .eachCount()
                .toList()
                .sortedBy {
                    it.first.name
                }
                .associate {
                    it
                }

        return CatalogCanonicalizationPlan(
            version = 1,

            inputEntryCount =
                sortedEntries.size,

            planEntryCount =
                sortedEntries.size,

            automaticActionCount = 0,

            reviewActionCount =
                sortedEntries.count {
                    it.action ==
                            CatalogCanonicalizationAction
                                .REVIEW
                },

            unchangedEntryCount =
                sortedEntries.count {
                    it.action ==
                            CatalogCanonicalizationAction
                                .KEEP
                },

            actionCounts =
                actionCounts,

            affectedSourceIndices =
                sortedEntries.map {
                    it.sourceIndex
                },

            entries =
                sortedEntries,

            valid = true
        )
    }

    private fun applicationEntry(
        sourceIndex: Int,
        originalItemName: String,
        action: CatalogCanonicalizationAction,
        status:
        CatalogCanonicalizationApplicationStatus,
        resultingNormalizedKey: String?,
        resultingCategory: String?,
        mergeTargetSourceIndex: Int?,
        reasons: List<String>
    ): CatalogCanonicalizationApplicationEntry =
        CatalogCanonicalizationApplicationEntry(
            sourceIndex = sourceIndex,
            originalItemName =
                originalItemName,
            action = action,
            automatic = false,
            status = status,
            resultingNormalizedKey =
                resultingNormalizedKey,
            resultingCategory =
                resultingCategory,
            mergeTargetSourceIndex =
                mergeTargetSourceIndex,
            reasons =
                reasons
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted()
        )

    /**
     * Convenience-Fixture für Tests mit exakt einem Application Entry.
     *
     * Ist der Artikel weiterhin im Ergebnis vorhanden, wird er unter dem
     * echten sourceIndex des Application Entry abgelegt.
     *
     * Bei einem vollständig gemergten oder entfernten Eintrag ist die
     * outputItems-Liste leer und damit auch die Source-Index-Map leer.
     */
    private fun applicationResult(
        applicationEntry:
        CatalogCanonicalizationApplicationEntry,

        outputItems:
        List<CatalogFoodItem>
    ): CatalogCanonicalizationApplicationResult {
        require(outputItems.size <= 1) {
            "Single-entry applicationResult fixture supports at most " +
                    "one output item."
        }

        val outputItemsBySourceIndex =
            outputItems
                .singleOrNull()
                ?.let { outputItem ->
                    linkedMapOf(
                        applicationEntry.sourceIndex to
                                outputItem
                    )
                }
                ?: emptyMap()

        return applicationResult(
            applicationEntries =
                listOf(applicationEntry),
            outputItemsBySourceIndex =
                outputItemsBySourceIndex
        )
    }

    /**
     * Allgemeines Fixture für mehrere Application Entries.
     *
     * Die Source-Index-Map muss explizit übergeben werden, damit Merges,
     * Removals und nicht fortlaufende Source-Indizes korrekt modelliert
     * bleiben.
     */
    private fun applicationResult(
        applicationEntries:
        List<CatalogCanonicalizationApplicationEntry>,

        outputItemsBySourceIndex:
        Map<Int, CatalogFoodItem>
    ): CatalogCanonicalizationApplicationResult {
        val sortedEntries =
            applicationEntries
                .sortedBy {
                    it.sourceIndex
                }

        val sortedOutputItemsBySourceIndex =
            outputItemsBySourceIndex
                .toSortedMap()

        val outputItems =
            sortedOutputItemsBySourceIndex
                .values
                .toList()

        val failedEntryCount =
            sortedEntries.count {
                it.status ==
                        CatalogCanonicalizationApplicationStatus
                            .FAILED
            }

        return CatalogCanonicalizationApplicationResult(
            version =
                CatalogCanonicalizationApplicationResult
                    .CURRENT_VERSION,

            inputEntryCount =
                sortedEntries.size,

            outputEntryCount =
                outputItems.size,

            appliedEntryCount =
                sortedEntries.count {
                    it.status ==
                            CatalogCanonicalizationApplicationStatus
                                .APPLIED
                },

            skippedReviewEntryCount =
                sortedEntries.count {
                    it.status ==
                            CatalogCanonicalizationApplicationStatus
                                .SKIPPED_REVIEW_REQUIRED
                },

            removedEntryCount =
                sortedEntries.count {
                    it.status ==
                            CatalogCanonicalizationApplicationStatus
                                .REMOVED
                },

            mergedEntryCount =
                sortedEntries.count {
                    it.status ==
                            CatalogCanonicalizationApplicationStatus
                                .MERGED_INTO_TARGET
                },

            failedEntryCount =
                failedEntryCount,

            outputItems =
                outputItems,

            outputItemsBySourceIndex =
                sortedOutputItemsBySourceIndex,

            entries =
                sortedEntries,

            valid =
                failedEntryCount == 0
        )
    }

    private data class MixedFixtures(
        val sourceEntries:
        List<IndexedCatalogFoodItem>,

        val normalizations:
        List<CatalogNormalizationResult>,

        val plan:
        CatalogCanonicalizationPlan,

        val applicationResult:
        CatalogCanonicalizationApplicationResult
    )
}