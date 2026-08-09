package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlan
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlanEntry
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogDeterministicDuplicateResolverTest {

    private val resolver =
        CatalogDeterministicDuplicateResolver()

    @Test
    fun resolveIdenticalNormalizedKeys() {
        val items = mapOf(
            10 to item(
                name = "Erbsen (Dose)",
                key = "erbsen-dose",
                category = "canned-food"
            ),
            11 to item(
                name = "Erbsen Dose",
                key = "erbsen-dose",
                category = "canned-food"
            ),
            12 to item(
                name = "Erbsen, Dose",
                key = "erbsen-dose",
                category = "canned-food"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(items.keys)
        )

        assertEquals(3, result.inputEntryCount)
        assertEquals(1, result.outputEntryCount)
        assertEquals(2, result.mergedEntryCount)

        assertEquals(
            listOf(11, 12),
            result.decisions.map { it.sourceIndex }
        )

        assertTrue(10 in result.itemsBySourceIndex)
        assertFalse(11 in result.itemsBySourceIndex)
        assertFalse(12 in result.itemsBySourceIndex)
    }

    @Test
    fun preserveAliasesFromRemovedDuplicates() {
        val items = mapOf(
            1 to item(
                name = "Bohnen (Dose)",
                key = "bohnen-dose",
                category = "canned-food"
            ),
            2 to item(
                name = "Bohnen, Dose",
                key = "bohnen-dose",
                category = "canned-food"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(items.keys)
        )

        val target = result.itemsBySourceIndex
            .values
            .single()

        assertTrue(
            "Bohnen, Dose" in target.colloquial ||
                    "Bohnen (Dose)" in target.colloquial
        )
    }

    @Test
    fun preserveDifferentCanonicalKeys() {
        val items = mapOf(
            1 to item(
                name = "Erbsen Dose",
                key = "erbsen-dose",
                category = "canned-food"
            ),
            2 to item(
                name = "Erbsen tiefgekühlt",
                key = "erbsen-tiefgekuehlt",
                category = "vegetables"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(items.keys)
        )

        assertEquals(2, result.outputEntryCount)
        assertTrue(result.decisions.isEmpty())
    }

    @Test
    fun resolveDeterministically() {
        val items = mapOf(
            20 to item(
                name = "Bio Eier",
                key = "bio-eier",
                category = "dairy"
            ),
            3 to item(
                name = "Bio-Eier",
                key = "bio-eier",
                category = "dairy"
            )
        )

        val first = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(items.keys)
        )

        val second = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(items.keys)
        )

        assertEquals(first, second)
        assertEquals(3, first.itemsBySourceIndex.keys.single())
    }

    @Test
    fun resolveExplicitSingularPluralVariant() {
        val items = mapOf(
            10 to item(
                name = "Erdbeere",
                key = "erdbeere",
                category = "fruit",
                plural = "Erdbeeren"
            ),
            41 to item(
                name = "Erdbeeren",
                key = "erdbeeren",
                category = "fruit",
                plural = "Erdbeeren"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 10,
                    itemName = "Erdbeere",
                    action = CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 41,
                    itemName = "Erdbeeren",
                    action = CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 10,
                    reasons = listOf(
                        "Canonical duplicate target is sourceIndex 10 ('Erdbeere').",
                        "Duplicate reasons: SINGULAR_PLURAL_VARIANT.",
                        "Duplicate recommendation: MERGE_AFTER_REVIEW.",
                        "Entry belongs to duplicate group 'test-group'."
                    )
                )
            )
        )

        assertEquals(2, result.inputEntryCount)
        assertEquals(1, result.outputEntryCount)
        assertEquals(1, result.mergedEntryCount)

        assertTrue(10 in result.itemsBySourceIndex)
        assertFalse(41 in result.itemsBySourceIndex)

        val decision = result.decisions.single()

        assertEquals(41, decision.sourceIndex)
        assertEquals(10, decision.targetSourceIndex)

        assertEquals(
            CatalogDuplicateResolutionReason
                .DETERMINISTIC_SINGULAR_PLURAL_VARIANT,
            decision.reason
        )

        val canonicalItem =
            result.itemsBySourceIndex.getValue(10)

        assertEquals("Erdbeere", canonicalItem.itemname)
        assertEquals("Erdbeeren", canonicalItem.plural)
        assertEquals("erdbeere", canonicalItem.normalized)

        assertTrue(
            canonicalItem.colloquial.any {
                it.equals(
                    "Erdbeeren",
                    ignoreCase = true
                )
            },
            "Removed plural form must remain available as an alias."
        )
    }

    @Test
    fun preserveSingularPluralVariantAcrossDifferentCategories() {
        val items = mapOf(
            10 to item(
                name = "Kirsche",
                key = "kirsche",
                category = "fruit",
                plural = "Kirschen"
            ),
            42 to item(
                name = "Kirschen",
                key = "kirschen",
                category = "canned-food",
                plural = "Kirschen"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 10,
                    itemName = "Kirsche",
                    action = CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 42,
                    itemName = "Kirschen",
                    action = CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 10,
                    reasons = singularPluralReasons(
                        targetSourceIndex = 10,
                        targetName = "Kirsche"
                    )
                )
            )
        )

        assertEquals(2, result.outputEntryCount)
        assertTrue(result.decisions.isEmpty())
    }

    @Test
    fun preserveEntriesWhenPluralDoesNotMatchSourceName() {
        val items = mapOf(
            10 to item(
                name = "Kartoffel",
                key = "kartoffel",
                category = "vegetables",
                plural = "Kartoffeln"
            ),
            45 to item(
                name = "Kartoffelprodukt",
                key = "kartoffelprodukt",
                category = "vegetables",
                plural = "Kartoffelprodukte"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 10,
                    itemName = "Kartoffel",
                    action = CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 45,
                    itemName = "Kartoffelprodukt",
                    action = CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 10,
                    reasons = singularPluralReasons(
                        targetSourceIndex = 10,
                        targetName = "Kartoffel"
                    )
                )
            )
        )

        assertEquals(2, result.outputEntryCount)
        assertTrue(result.decisions.isEmpty())
    }

    @Test
    fun preserveSingularPluralVariantWithSemanticConflict() {
        val items = mapOf(
            10 to item(
                name = "Karotte",
                key = "karotte",
                category = "vegetables",
                plural = "Karotten"
            ),
            44 to item(
                name = "Karotten",
                key = "karotten",
                category = "vegetables",
                plural = "Karotten"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 10,
                    itemName = "Karotte",
                    action = CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 44,
                    itemName = "Karotten",
                    action = CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 10,
                    reasons = listOf(
                        "Canonical duplicate target is sourceIndex 10 ('Karotte').",
                        "Duplicate reasons: SINGULAR_PLURAL_VARIANT, " +
                                "POSSIBLE_SEMANTIC_DUPLICATE.",
                        "Duplicate recommendation: MERGE_AFTER_REVIEW.",
                        "Entry belongs to duplicate group 'test-group'."
                    )
                )
            )
        )

        assertEquals(2, result.outputEntryCount)
        assertTrue(result.decisions.isEmpty())
    }

    @Test
    fun resolveReviewSingularPluralVariantWithUniqueTarget() {
        val items = mapOf(
            503 to item(
                name = "Karotte",
                key = "karotte",
                category = "vegetables",
                plural = "Karotten"
            ),
            44 to item(
                name = "Karotten",
                key = "karotten",
                category = "vegetables",
                plural = "Karotten"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 503,
                    itemName = "Karotte",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 44,
                    itemName = "Karotten",
                    action =
                        CatalogCanonicalizationAction.REVIEW,
                    mergeTargetSourceIndex = 503,
                    reasons = listOf(
                        "Canonical duplicate target is sourceIndex 503 " +
                                "('Karotte').",
                        "Duplicate reasons: SINGULAR_PLURAL_VARIANT.",
                        "Duplicate recommendation: REVIEW.",
                        "Entry belongs to duplicate group 'karotte-test'."
                    )
                )
            )
        )

        assertEquals(1, result.outputEntryCount)
        assertEquals(1, result.mergedEntryCount)

        assertTrue(503 in result.itemsBySourceIndex)
        assertFalse(44 in result.itemsBySourceIndex)

        val decision = result.decisions.single()

        assertEquals(44, decision.sourceIndex)
        assertEquals(503, decision.targetSourceIndex)

        assertEquals(
            CatalogDuplicateResolutionReason
                .DETERMINISTIC_SINGULAR_PLURAL_VARIANT,
            decision.reason
        )
    }

    @Test
    fun resolveSingularPluralVariantUsingDeterministicNameRelation() {
        val items = mapOf(
            684 to item(
                name = "Kartoffel",
                key = "kartoffel",
                category = "vegetables",
                plural = "Kartoffele"
            ),
            45 to item(
                name = "Kartoffeln",
                key = "kartoffeln",
                category = "vegetables",
                plural = "Kartoffeln"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 684,
                    itemName = "Kartoffel",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 45,
                    itemName = "Kartoffeln",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 684,
                    reasons = singularPluralReasons(
                        targetSourceIndex = 684,
                        targetName = "Kartoffel"
                    )
                )
            )
        )

        assertEquals(1, result.outputEntryCount)
        assertEquals(1, result.mergedEntryCount)
        assertTrue(684 in result.itemsBySourceIndex)
        assertFalse(45 in result.itemsBySourceIndex)
    }

    @Test
    fun preserveFalsePositiveSingularPluralClassification() {
        val items = mapOf(
            1 to item(
                name = "Süßsaure Sauce",
                key = "suesssaure-sauce",
                category = "sauces",
                plural = "Süßsaure Saucen"
            ),
            2 to item(
                name = "Süß-Saure Sauce",
                key = "suess-saure-sauce",
                category = "sauces",
                plural = "Süß-Saure Saucen"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 1,
                    itemName = "Süßsaure Sauce",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 2,
                    itemName = "Süß-Saure Sauce",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 1,
                    reasons = singularPluralReasons(
                        targetSourceIndex = 1,
                        targetName = "Süßsaure Sauce"
                    )
                )
            )
        )

        assertEquals(2, result.outputEntryCount)
        assertTrue(result.decisions.isEmpty())
    }

    @Test
    fun preserveMergedPluralAsAutocompleteToken() {
        val items = mapOf(
            10 to item(
                name = "Tomate",
                key = "tomate",
                category = "vegetables",
                plural = "Tomaten"
            ),
            20 to item(
                name = "Tomaten",
                key = "tomaten",
                category = "vegetables",
                plural = "Tomaten"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 10,
                    itemName = "Tomate",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 20,
                    itemName = "Tomaten",
                    action =
                        CatalogCanonicalizationAction.REVIEW,
                    mergeTargetSourceIndex = 10,
                    reasons = listOf(
                        "Canonical duplicate target is sourceIndex 10 " +
                                "('Tomate').",
                        "Duplicate reasons: SINGULAR_PLURAL_VARIANT.",
                        "Duplicate recommendation: REVIEW.",
                        "Entry belongs to duplicate group 'tomate-test'."
                    )
                )
            )
        )

        val target =
            result.itemsBySourceIndex.getValue(10)

        assertTrue(
            "tomaten" in target.autocompleteTokens,
            "Merged plural form must remain searchable."
        )
    }

    @Test
    fun resolveDeterministicTypoVariant() {
        val items = mapOf(
            100 to item(
                name = "Basmati Reis",
                key = "basmati-reis",
                category = "rice",
                plural = "Basmati-Reise"
            ),
            200 to item(
                name = "Basamti Reis",
                key = "basamti-reis",
                category = "rice",
                plural = "Basamti-Reise"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 100,
                    itemName = "Basmati Reis",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 200,
                    itemName = "Basamti Reis",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 100,
                    reasons = typoReasons(
                        targetSourceIndex = 100,
                        targetName = "Basmati Reis"
                    )
                )
            )
        )

        assertEquals(1, result.outputEntryCount)
        assertEquals(1, result.mergedEntryCount)

        assertTrue(100 in result.itemsBySourceIndex)
        assertFalse(200 in result.itemsBySourceIndex)

        val decision = result.decisions.single()

        assertEquals(200, decision.sourceIndex)
        assertEquals(100, decision.targetSourceIndex)

        assertEquals(
            CatalogDuplicateResolutionReason
                .DETERMINISTIC_TYPO_VARIANT,
            decision.reason
        )
    }

    @Test
    fun resolveSharpSOrthographyVariant() {
        val items = mapOf(
            10 to item(
                name = "Weißkohl",
                key = "weisskohl",
                category = "vegetables",
                plural = "Weißkohle"
            ),
            20 to item(
                name = "Weis(s)kohl",
                key = "weisskohl-alt",
                category = "vegetables",
                plural = "Weis(s)kohle"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 10,
                    itemName = "Weißkohl",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 20,
                    itemName = "Weis(s)kohl",
                    action =
                        CatalogCanonicalizationAction.REVIEW,
                    mergeTargetSourceIndex = 10,
                    reasons = typoReasons(
                        targetSourceIndex = 10,
                        targetName = "Weißkohl",
                        recommendation = "REVIEW"
                    )
                )
            )
        )

        assertEquals(1, result.outputEntryCount)
        assertTrue(10 in result.itemsBySourceIndex)
        assertFalse(20 in result.itemsBySourceIndex)
    }

    @Test
    fun preserveTypoCandidateWithDifferentProductNumber() {
        val items = mapOf(
            10 to item(
                name = "Weizenmehl Typ 405",
                key = "weizenmehl-typ-405",
                category = "flour",
                plural = "Weizenmehle Typ 405"
            ),
            20 to item(
                name = "Weizenmehl Typ 550",
                key = "weizenmehl-typ-550",
                category = "flour",
                plural = "Weizenmehle Typ 550"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 10,
                    itemName = "Weizenmehl Typ 405",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 20,
                    itemName = "Weizenmehl Typ 550",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 10,
                    reasons = typoReasons(
                        targetSourceIndex = 10,
                        targetName = "Weizenmehl Typ 405"
                    )
                )
            )
        )

        assertEquals(2, result.outputEntryCount)
        assertTrue(result.decisions.isEmpty())
    }

    @Test
    fun preserveTypoCandidateWithMultipleDifferentTokens() {
        val items = mapOf(
            10 to item(
                name = "Tomatensauce mild",
                key = "tomatensauce-mild",
                category = "sauces",
                plural = "Tomatensaucen mild"
            ),
            20 to item(
                name = "Tomatensoße scharf",
                key = "tomatensosse-scharf",
                category = "sauces",
                plural = "Tomatensoßen scharf"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 10,
                    itemName = "Tomatensauce mild",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 20,
                    itemName = "Tomatensoße scharf",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 10,
                    reasons = typoReasons(
                        targetSourceIndex = 10,
                        targetName = "Tomatensauce mild"
                    )
                )
            )
        )

        assertEquals(2, result.outputEntryCount)
        assertTrue(result.decisions.isEmpty())
    }

    @Test
    fun preserveTypoVariantWithSemanticConflict() {
        val items = mapOf(
            10 to item(
                name = "Pizza Margherita",
                key = "pizza-margherita",
                category = "ready-meals",
                plural = "Pizzen Margherita"
            ),
            20 to item(
                name = "Pizza Margerita",
                key = "pizza-margerita",
                category = "ready-meals",
                plural = "Pizzen Margerita"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 10,
                    itemName = "Pizza Margherita",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 20,
                    itemName = "Pizza Margerita",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 10,
                    reasons = listOf(
                        "Canonical duplicate target is sourceIndex 10 " +
                                "('Pizza Margherita').",
                        "Duplicate reasons: TYPO_VARIANT, " +
                                "POSSIBLE_SEMANTIC_DUPLICATE.",
                        "Duplicate recommendation: MERGE_AFTER_REVIEW.",
                        "Entry belongs to duplicate group 'pizza-test'."
                    )
                )
            )
        )

        assertEquals(2, result.outputEntryCount)
        assertTrue(result.decisions.isEmpty())
    }

    @Test
    fun preserveMergedTypoAsAutocompleteToken() {
        val items = mapOf(
            10 to item(
                name = "Weißkohl",
                key = "weisskohl",
                category = "vegetables",
                plural = "Weißkohle"
            ),
            20 to item(
                name = "Weis(s)kohl",
                key = "weisskohl-alt",
                category = "vegetables",
                plural = "Weis(s)kohle"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 10,
                    itemName = "Weißkohl",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 20,
                    itemName = "Weis(s)kohl",
                    action =
                        CatalogCanonicalizationAction.REVIEW,
                    mergeTargetSourceIndex = 10,
                    reasons = typoReasons(
                        targetSourceIndex = 10,
                        targetName = "Weißkohl",
                        recommendation = "REVIEW"
                    )
                )
            )
        )

        val canonicalItem =
            result.itemsBySourceIndex.getValue(10)

        assertTrue(
            canonicalItem.autocompleteTokens.any {
                it.contains(
                    "weis",
                    ignoreCase = true
                )
            },
            "Removed typo form must remain searchable."
        )
    }

    @Test
    fun resolveDeterministicTypTypeVariant() {
        val items = mapOf(
            100 to item(
                name = "Weizenmehl Typ 405",
                key = "weizenmehl-typ-405",
                category = "grains",
                plural = "Weizenmehle Typ 405"
            ),
            200 to item(
                name = "Weizenmehl Type 405",
                key = "weizenmehl-type-405",
                category = "grains",
                plural = "Weizenmehle Type 405"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 100,
                    itemName = "Weizenmehl Typ 405",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 200,
                    itemName = "Weizenmehl Type 405",
                    action =
                        CatalogCanonicalizationAction.REVIEW,
                    mergeTargetSourceIndex = 100,
                    reasons = typoReasons(
                        targetSourceIndex = 100,
                        targetName = "Weizenmehl Typ 405",
                        recommendation = "REVIEW"
                    )
                )
            )
        )

        assertEquals(1, result.outputEntryCount)
        assertEquals(1, result.mergedEntryCount)

        assertTrue(100 in result.itemsBySourceIndex)
        assertFalse(200 in result.itemsBySourceIndex)

        val decision = result.decisions.single()

        assertEquals(200, decision.sourceIndex)
        assertEquals(100, decision.targetSourceIndex)

        assertEquals(
            CatalogDuplicateResolutionReason
                .DETERMINISTIC_TYPO_VARIANT,
            decision.reason
        )
    }

    @Test
    fun preserveTypTypeVariantWithDifferentTypeNumber() {
        val items = mapOf(
            100 to item(
                name = "Weizenmehl Typ 405",
                key = "weizenmehl-typ-405",
                category = "grains",
                plural = "Weizenmehle Typ 405"
            ),
            200 to item(
                name = "Weizenmehl Type 550",
                key = "weizenmehl-type-550",
                category = "grains",
                plural = "Weizenmehle Type 550"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 100,
                    itemName = "Weizenmehl Typ 405",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 200,
                    itemName = "Weizenmehl Type 550",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 100,
                    reasons = typoReasons(
                        targetSourceIndex = 100,
                        targetName = "Weizenmehl Typ 405"
                    )
                )
            )
        )

        assertEquals(2, result.outputEntryCount)
        assertTrue(result.decisions.isEmpty())
    }

    @Test
    fun resolveDeterministicSauceSosseVariant() {
        val items = mapOf(
            100 to item(
                name = "Bohnen in Tomatensoße",
                key = "bohnen-in-tomatensosse",
                category = "canned-food",
                plural = "Bohnen in Tomatensoße"
            ),
            200 to item(
                name = "Bohnen in Tomatensauce",
                key = "bohnen-in-tomatensauce",
                category = "canned-food",
                plural = "Bohnen in Tomatensauce"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 100,
                    itemName = "Bohnen in Tomatensoße",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 200,
                    itemName = "Bohnen in Tomatensauce",
                    action =
                        CatalogCanonicalizationAction.REVIEW,
                    mergeTargetSourceIndex = 100,
                    reasons = typoReasons(
                        targetSourceIndex = 100,
                        targetName =
                            "Bohnen in Tomatensoße",
                        recommendation = "REVIEW"
                    )
                )
            )
        )

        assertEquals(1, result.outputEntryCount)
        assertEquals(1, result.mergedEntryCount)

        assertTrue(100 in result.itemsBySourceIndex)
        assertFalse(200 in result.itemsBySourceIndex)

        val decision =
            result.decisions.single()

        assertEquals(200, decision.sourceIndex)
        assertEquals(100, decision.targetSourceIndex)

        assertEquals(
            CatalogDuplicateResolutionReason
                .DETERMINISTIC_TYPO_VARIANT,
            decision.reason
        )
    }

    @Test
    fun resolveSardinesTomatoSauceOrthographyVariant() {
        val items = mapOf(
            100 to item(
                name = "Sardinen in Tomatensoße",
                key = "sardinen-in-tomatensosse",
                category = "canned-food",
                plural = "Sardinen in Tomatensoße"
            ),
            200 to item(
                name = "Sardinen in Tomatensauce",
                key = "sardinen-in-tomatensauce",
                category = "canned-food",
                plural = "Sardinen in Tomatensauce"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 100,
                    itemName =
                        "Sardinen in Tomatensoße",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 200,
                    itemName =
                        "Sardinen in Tomatensauce",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 100,
                    reasons = typoReasons(
                        targetSourceIndex = 100,
                        targetName =
                            "Sardinen in Tomatensoße"
                    )
                )
            )
        )

        assertEquals(1, result.outputEntryCount)
        assertTrue(100 in result.itemsBySourceIndex)
        assertFalse(200 in result.itemsBySourceIndex)
    }

    @Test
    fun preserveDifferentSauceFlavors() {
        val items = mapOf(
            100 to item(
                name = "Hering in Senfsauce",
                key = "hering-in-senfsauce",
                category = "fish",
                plural = "Heringe in Senfsauce"
            ),
            200 to item(
                name = "Hering in Sahnesauce",
                key = "hering-in-sahnesauce",
                category = "fish",
                plural = "Heringe in Sahnesauce"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 100,
                    itemName = "Hering in Senfsauce",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 200,
                    itemName = "Hering in Sahnesauce",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 100,
                    reasons = typoReasons(
                        targetSourceIndex = 100,
                        targetName =
                            "Hering in Senfsauce"
                    )
                )
            )
        )

        assertEquals(2, result.outputEntryCount)

        assertTrue(
            result.decisions.isEmpty(),
            "Different sauce flavors must remain separate foods."
        )
    }

    @Test
    fun preserveSauceVariantWithAdditionalProductDifference() {
        val items = mapOf(
            100 to item(
                name = "Tomatensoße mild",
                key = "tomatensosse-mild",
                category = "sauces",
                plural = "Tomatensoßen mild"
            ),
            200 to item(
                name = "Tomatensauce scharf",
                key = "tomatensauce-scharf",
                category = "sauces",
                plural = "Tomatensaucen scharf"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 100,
                    itemName = "Tomatensoße mild",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 200,
                    itemName = "Tomatensauce scharf",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    mergeTargetSourceIndex = 100,
                    reasons = typoReasons(
                        targetSourceIndex = 100,
                        targetName =
                            "Tomatensoße mild"
                    )
                )
            )
        )

        assertEquals(2, result.outputEntryCount)

        assertTrue(
            result.decisions.isEmpty(),
            "Additional flavor differences must block the merge."
        )
    }

    @Test
    fun preserveMergedSauceSpellingAsAutocompleteToken() {
        val items = mapOf(
            100 to item(
                name = "Bohnen in Tomatensoße",
                key = "bohnen-in-tomatensosse",
                category = "canned-food",
                plural = "Bohnen in Tomatensoße"
            ),
            200 to item(
                name = "Bohnen in Tomatensauce",
                key = "bohnen-in-tomatensauce",
                category = "canned-food",
                plural = "Bohnen in Tomatensauce"
            )
        )

        val result = resolver.resolve(
            itemsBySourceIndex = items,
            plan = plan(
                planEntry(
                    sourceIndex = 100,
                    itemName =
                        "Bohnen in Tomatensoße",
                    action =
                        CatalogCanonicalizationAction.REVIEW
                ),
                planEntry(
                    sourceIndex = 200,
                    itemName =
                        "Bohnen in Tomatensauce",
                    action =
                        CatalogCanonicalizationAction.REVIEW,
                    mergeTargetSourceIndex = 100,
                    reasons = typoReasons(
                        targetSourceIndex = 100,
                        targetName =
                            "Bohnen in Tomatensoße",
                        recommendation = "REVIEW"
                    )
                )
            )
        )

        val canonicalItem =
            result.itemsBySourceIndex
                .getValue(100)

        assertTrue(
            canonicalItem.autocompleteTokens.any {
                it.contains(
                    "tomatensauce",
                    ignoreCase = true
                )
            },
            "Removed Sauce spelling must remain searchable."
        )
    }






    //##################################################################################

    private fun typoReasons(
        targetSourceIndex: Int,
        targetName: String,
        recommendation: String =
            "MERGE_AFTER_REVIEW"
    ): List<String> =
        listOf(
            "Canonical duplicate target is sourceIndex " +
                    "$targetSourceIndex ('$targetName').",
            "Duplicate reasons: TYPO_VARIANT.",
            "Duplicate recommendation: $recommendation.",
            "Entry belongs to duplicate group 'typo-test-group'."
        )

    private fun item(
        name: String,
        key: String,
        category: String,
        plural: String = name
    ): CatalogFoodItem =
        CatalogFoodItem(
            itemname = name,
            category = category,
            production = "Standard",
            normalized = key,
            plural = plural,
            colloquial = emptyList(),
            phoneticTokens = emptyList(),
            autocompleteTokens = listOf(key),
            normalizedEnglish = null
        )

    private fun singularPluralReasons(
        targetSourceIndex: Int,
        targetName: String
    ): List<String> =
        listOf(
            "Canonical duplicate target is sourceIndex " +
                    "$targetSourceIndex ('$targetName').",
            "Duplicate reasons: SINGULAR_PLURAL_VARIANT.",
            "Duplicate recommendation: MERGE_AFTER_REVIEW.",
            "Entry belongs to duplicate group 'test-group'."
        )

    private fun planEntry(
        sourceIndex: Int,
        itemName: String,
        action: CatalogCanonicalizationAction,
        mergeTargetSourceIndex: Int? = null,
        reasons: List<String> = listOf(
            "Test review entry."
        )
    ): CatalogCanonicalizationPlanEntry =
        CatalogCanonicalizationPlanEntry(
            sourceIndex = sourceIndex,
            originalItemName = itemName,
            action = action,
            proposedCanonicalName = null,
            proposedNormalizedKey = null,
            proposedCategory = null,
            mergeTargetSourceIndex =
                mergeTargetSourceIndex,
            automatic = false,
            reasons = reasons
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted(),
            confidence = 0.50
        )

    private fun plan(
        vararg planEntries: CatalogCanonicalizationPlanEntry
    ): CatalogCanonicalizationPlan {
        val entries = planEntries
            .sortedBy { it.sourceIndex }

        require(
            entries.map { it.sourceIndex }
                .distinct()
                .size == entries.size
        ) {
            "Plan fixture must not contain duplicate sourceIndex values."
        }

        val actionCounts = entries
            .groupingBy { it.action }
            .eachCount()
            .toList()
            .sortedBy { it.first.name }
            .associate { it }

        val automaticActionCount = entries.count {
            it.automatic
        }

        /*
         * CatalogCanonicalizationPlan.reviewActionCount bezeichnet exakt
         * die Anzahl der REVIEW-Aktionen, nicht die Anzahl aller manuellen
         * Entscheidungen.
         */
        val reviewActionCount = entries.count {
            it.action ==
                    CatalogCanonicalizationAction.REVIEW
        }

        val unchangedEntryCount = entries.count {
            it.action ==
                    CatalogCanonicalizationAction.KEEP
        }

        return CatalogCanonicalizationPlan(
            version = 1,
            inputEntryCount = entries.size,
            planEntryCount = entries.size,
            automaticActionCount = automaticActionCount,
            reviewActionCount = reviewActionCount,
            unchangedEntryCount = unchangedEntryCount,
            actionCounts = actionCounts,
            affectedSourceIndices = entries
                .map { it.sourceIndex },
            entries = entries,
            valid = true
        )
    }

    private fun plan(
        sourceIndices: Collection<Int>
    ): CatalogCanonicalizationPlan {
        val entries = sourceIndices
            .sorted()
            .map { sourceIndex ->
                CatalogCanonicalizationPlanEntry(
                    sourceIndex = sourceIndex,
                    originalItemName = "Item $sourceIndex",
                    action =
                        CatalogCanonicalizationAction.REVIEW,
                    proposedCanonicalName = null,
                    proposedNormalizedKey = null,
                    proposedCategory = null,
                    mergeTargetSourceIndex = null,
                    automatic = false,
                    reasons = listOf("Test review entry."),
                    confidence = 0.50
                )
            }

        return CatalogCanonicalizationPlan(
            version = 1,
            inputEntryCount = entries.size,
            planEntryCount = entries.size,
            automaticActionCount = 0,
            reviewActionCount = entries.size,
            unchangedEntryCount = 0,
            actionCounts = mapOf(
                CatalogCanonicalizationAction.REVIEW to
                        entries.size
            ),
            affectedSourceIndices =
                entries.map { it.sourceIndex },
            entries = entries,
            valid = true
        )
    }
}