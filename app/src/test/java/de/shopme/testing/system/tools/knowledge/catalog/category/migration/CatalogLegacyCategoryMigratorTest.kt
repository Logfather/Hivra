package de.shopme.testing.system.tools.knowledge.catalog.category.migration

import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatalogLegacyCategoryMigratorTest {

    private val registry =
        CanonicalFoodCategoryRegistry()

    private val migrator =
        CatalogLegacyCategoryMigrator()

    @Test
    fun migrateDirectLegacyCategories() {
        val testCases = mapOf(
            "Backwaren" to "bakery",
            "Getränke" to "beverages",
            "Konserven" to "canned-food",
            "Süßwaren" to "confectionery",
            "Milchprodukte" to "dairy",
            "Fisch" to "fish",
            "Obst" to "fruit",
            "Fleisch" to "meat",
            "Öle" to "oils",
            "Nudeln" to "pasta",
            "Fertiggerichte" to "ready-meals",
            "Reis" to "rice",
            "Saucen" to "sauces",
            "Gewürze" to "spices",
            "Gemüse" to "vegetables",
            "Snacks" to "snacks",
        )

        testCases.forEach { (legacyCategory, expectedTarget) ->
            if (!registry.contains(expectedTarget)) {
                return@forEach
            }

            val result = migrator.migrate(
                item = item(
                    name = "Testprodukt",
                    category = legacyCategory
                ),
                registry = registry
            )

            assertEquals(
                CatalogCategoryMigrationStatus.MIGRATED_DIRECTLY,
                result.status,
                "Unexpected status for '$legacyCategory'."
            )

            assertEquals(
                expectedTarget,
                result.resultingCategory
            )

            assertEquals(1.0, result.confidence)
            assertNotNull(result.ruleId)
        }
    }

    @Test
    fun preserveCanonicalCategory() {
        val canonicalCategory =
            registry.definitions().first().key

        val result = migrator.migrate(
            item = item(
                name = "Testprodukt",
                category = canonicalCategory
            ),
            registry = registry
        )

        assertEquals(
            CatalogCategoryMigrationStatus.ALREADY_CANONICAL,
            result.status
        )

        assertEquals(
            canonicalCategory,
            result.resultingCategory
        )
    }

    @Test
    fun migrateFrozenVegetablesByProductRule() {
        assertTrue(registry.contains("vegetables"))

        val result = migrator.migrate(
            item = item(
                name = "Tiefkühl-Gemüsemischung",
                category = "Tiefkühlprodukte"
            ),
            registry = registry
        )

        assertEquals(
            CatalogCategoryMigrationStatus.MIGRATED_BY_PRODUCT_RULE,
            result.status
        )

        assertEquals(
            "vegetables",
            result.resultingCategory
        )
    }

    @Test
    fun migrateFrozenFruitByProductRule() {
        assertTrue(registry.contains("fruit"))

        val result = migrator.migrate(
            item = item(
                name = "Tiefkühl-Beerenmischung",
                category = "Tiefkühlprodukte"
            ),
            registry = registry
        )

        assertEquals(
            "fruit",
            result.resultingCategory
        )
    }

    @Test
    fun migrateVeganSauceByProductRule() {
        assertTrue(registry.contains("sauces"))

        val result = migrator.migrate(
            item = item(
                name = "Vegane Mayonnaise",
                category = "Vegan"
            ),
            registry = registry
        )

        assertEquals(
            "sauces",
            result.resultingCategory
        )
    }

    @Test
    fun migrateGrainProductPastaByProductRule() {
        assertTrue(registry.contains("pasta"))

        val result = migrator.migrate(
            item = item(
                name = "Dinkel-Penne",
                category = "Getreideprodukte"
            ),
            registry = registry
        )

        assertEquals(
            "pasta",
            result.resultingCategory
        )
    }

    @Test
    fun preserveUnknownCategoryAsUnresolved() {
        val result = migrator.migrate(
            item = item(
                name = "Nicht klassifizierbares Produkt",
                category = "Unbekannte Alt-Kategorie"
            ),
            registry = registry
        )

        assertEquals(
            CatalogCategoryMigrationStatus.UNRESOLVED,
            result.status
        )

        assertEquals(null, result.resultingCategory)
        assertEquals(0.0, result.confidence)
    }

    @Test
    fun migrateDeterministically() {
        val item = item(
            name = "Tiefkühl-Spinat",
            category = "Tiefkühlprodukte"
        )

        val first = migrator.migrate(
            item = item,
            registry = registry
        )

        val second = migrator.migrate(
            item = item,
            registry = registry
        )

        assertEquals(first, second)
    }

    @Test
    fun migrateLegacyGrainCategoryCompletely() {
        val legacyItems = listOf(
            "Amaranth",
            "Braune Linsen",
            "Erbsen getrocknet",
            "Frühstückscerealien",
            "Glutenfreie Mehlmischung",
            "Glutenfreies Mehl",
            "Graupen",
            "Grünkern",
            "Grünkernmehl",
            "Hefe frisch",
            "Hefeflocken",
            "Hefewürfel",
            "Kamut",
            "Kamutmehl",
            "Kartoffelmehl",
            "Kichererbsen getrocknet",
            "Kichererbsenmehl",
            "Kidneybohnen getrocknet",
            "Kuszkus",
            "Linsenmehl",
            "Lupinenmehl",
            "Maisflocken",
            "Maismehl",
            "Maisschrot",
            "Maisstärke",
            "Mandelmehl",
            "Milchreis",
            "Polenta",
            "Reisflocken",
            "Reismehl",
            "Reisvollkorn",
            "Rote Linsen",
            "Semmelbrösel",
            "Sorghum",
            "Tapioka",
            "Teff",
            "Teffmehl",
            "Vollkornmehl"
        )

        legacyItems.forEach { itemName ->
            val result = migrator.migrate(
                item = item(
                    name = itemName,
                    category = "Getreideprodukte"
                ),
                registry = registry
            )

            assertTrue(
                result.status ==
                        CatalogCategoryMigrationStatus
                            .MIGRATED_BY_PRODUCT_RULE,
                "Legacy grain item '$itemName' was not migrated: $result"
            )

            val resultingCategory = assertNotNull(
                result.resultingCategory,
                "Legacy grain item '$itemName' has no resulting category."
            )

            assertTrue(
                registry.contains(resultingCategory),
                "Legacy grain item '$itemName' was migrated to unknown " +
                        "category '$resultingCategory'."
            )

            assertTrue(
                result.confidence >= 0.85,
                "Legacy grain migration confidence is too low for " +
                        "'$itemName': ${result.confidence}"
            )

            assertNotNull(
                result.ruleId,
                "Legacy grain item '$itemName' has no migration rule."
            )
        }
    }

    @Test
    fun migrateLegacyLegumesToExistingCanonicalCategory() {
        val legumeItems = listOf(
            "Braune Linsen",
            "Grüne Linsen",
            "Rote Linsen",
            "Schwarze Linsen",
            "Erbsen getrocknet",
            "Kichererbsen getrocknet",
            "Kidneybohnen getrocknet"
        )

        legumeItems.forEach { itemName ->
            val result = migrator.migrate(
                item = item(
                    name = itemName,
                    category = "Getreideprodukte"
                ),
                registry = registry
            )

            val target = assertNotNull(
                result.resultingCategory
            )

            assertTrue(registry.contains(target))

            assertEquals(
                "grain-product-legume",
                result.ruleId,
                "Unexpected migration rule for '$itemName'."
            )
        }
    }

    @Test
    fun migrateLegacyFloursBeforeGeneralGrainRules() {
        val flourItems = listOf(
            "Grünkernmehl",
            "Kamutmehl",
            "Kartoffelmehl",
            "Kichererbsenmehl",
            "Linsenmehl",
            "Lupinenmehl",
            "Maismehl",
            "Mandelmehl",
            "Reismehl",
            "Teffmehl",
            "Vollkornmehl"
        )

        flourItems.forEach { itemName ->
            val result = migrator.migrate(
                item = item(
                    name = itemName,
                    category = "Getreideprodukte"
                ),
                registry = registry
            )

            assertEquals(
                "grain-product-flour",
                result.ruleId,
                "Flour item '$itemName' matched the wrong migration rule."
            )

            assertNotNull(result.resultingCategory)
        }
    }

    @Test
    fun migrateLegacyYeastAsBakingIngredient() {
        val yeastItems = listOf(
            "Hefe frisch",
            "Hefeflocken",
            "Hefewürfel"
        )

        yeastItems.forEach { itemName ->
            val result = migrator.migrate(
                item = item(
                    name = itemName,
                    category = "Getreideprodukte"
                ),
                registry = registry
            )

            assertEquals(
                "grain-product-baking-yeast",
                result.ruleId
            )

            assertNotNull(result.resultingCategory)
        }
    }

    @Test
    fun migrateLegacyBreakfastCereals() {
        val breakfastItems = listOf(
            "Frühstückscerealien",
            "Maisflocken",
            "Reisflocken"
        )

        breakfastItems.forEach { itemName ->
            val result = migrator.migrate(
                item = item(
                    name = itemName,
                    category = "Getreideprodukte"
                ),
                registry = registry
            )

            assertEquals(
                "grain-product-breakfast-cereal",
                result.ruleId
            )

            assertNotNull(result.resultingCategory)
        }
    }

    @Test
    fun migrateUnclassifiedLegacyGrainItemWithCanonicalFallback() {
        val result = migrator.migrate(
            item = item(
                name = "Historisches trockenes Grundprodukt",
                category = "Getreideprodukte"
            ),
            registry = registry
        )

        assertEquals(
            CatalogCategoryMigrationStatus.MIGRATED_BY_PRODUCT_RULE,
            result.status
        )

        assertEquals(
            "grain-product-generic-fallback",
            result.ruleId
        )

        val target = assertNotNull(result.resultingCategory)

        assertTrue(
            registry.contains(target),
            "Fallback target '$target' must exist in the registry."
        )

        assertEquals(0.85, result.confidence)
    }

    @Test
    fun migrateAllCurrentlyKnownLegacyGrainEntries() {
        val knownLegacyEntries = listOf(
            "Amaranth",
            "Braune Linsen",
            "Erbsen getrocknet",
            "Frühstückscerealien",
            "Getrocknete Erbsen",
            "Glutenfreie Mehlmischung",
            "Glutenfreies Mehl",
            "Graupen",
            "Grüne Erbsen getrocknet",
            "Grüne Linsen",
            "Grünkern",
            "Grünkern Standard",
            "Grünkernmehl",
            "Hefe frisch",
            "Hefeflocken",
            "Hefewürfel",
            "Kamut",
            "Kamut Bio",
            "Kamutmehl",
            "Kartoffelmehl",
            "Kichererbsen getrocknet",
            "Kichererbsenmehl",
            "Kidneybohnen getrocknet",
            "Kuszkus",
            "Linsen Braun",
            "Linsen Trocken",
            "Linsenmehl",
            "Lupinenmehl",
            "Maisflocken",
            "Maismehl",
            "Maismehl Bio",
            "Maismehl Gelb",
            "Maismehl Standard",
            "Maisschrot",
            "Maisstärke",
            "Maizena Maisstärke",
            "Mandelmehl",
            "Milchreis",
            "Polenta",
            "Reisflocken",
            "Reismehl",
            "Reisvollkorn",
            "Rote Linse",
            "Rote Linsen",
            "Rote Linsen Bio",
            "Rote Linsen getrocknet",
            "Schwarze Linsen",
            "Semmelbrösel",
            "Sorghum",
            "Tapioka",
            "Teff",
            "Teffmehl",
            "Vollkornmehl"
        )

        assertEquals(
            53,
            knownLegacyEntries.size,
            "Known legacy grain fixture must cover all evaluation findings."
        )

        knownLegacyEntries.forEach { itemName ->
            val result = migrator.migrate(
                item = item(
                    name = itemName,
                    category = "Getreideprodukte"
                ),
                registry = registry
            )

            assertTrue(
                result.status !=
                        CatalogCategoryMigrationStatus.UNRESOLVED,
                "Known legacy grain item remains unresolved: '$itemName'."
            )

            val target = assertNotNull(
                result.resultingCategory,
                "Known legacy grain item has no target: '$itemName'."
            )

            assertTrue(
                registry.contains(target),
                "Known legacy grain item '$itemName' targets unknown " +
                        "category '$target'."
            )
        }
    }

    @Test
    fun migrateLegacyCapitalizedSnacksCategory() {
        assertTrue(
            registry.contains("snacks"),
            "Canonical category 'snacks' must exist."
        )

        val result =
            migrator.migrate(
                item =
                    item(
                        name = "Gemüsechips",
                        category = "Snacks"
                    ),
                registry =
                    registry
            )

        assertEquals(
            expected =
                CatalogCategoryMigrationStatus
                    .MIGRATED_DIRECTLY,
            actual =
                result.status,
            message =
                "Legacy category 'Snacks' must be migrated directly."
        )

        assertEquals(
            expected = "snacks",
            actual = result.resultingCategory,
            message =
                "Legacy category 'Snacks' must be migrated to canonical " +
                        "category 'snacks'."
        )

        assertEquals(
            expected = 1.0,
            actual = result.confidence,
            message =
                "Direct legacy category migration must have full confidence."
        )

        assertNotNull(
            actual = result.ruleId,
            message =
                "Direct legacy category migration must expose its rule ID."
        )
    }

    private fun item(
        name: String,
        category: String
    ): CatalogFoodItem =
        CatalogFoodItem(
            itemname = name,
            category = category,
            production = "Standard",
            normalized = null,
            plural = null,
            colloquial = emptyList(),
            phoneticTokens = emptyList(),
            autocompleteTokens = emptyList(),
            normalizedEnglish = null
        )
}