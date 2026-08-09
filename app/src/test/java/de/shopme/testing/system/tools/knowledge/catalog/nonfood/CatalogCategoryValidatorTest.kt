package de.shopme.testing.system.tools.knowledge.catalog.category

import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogIssueSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatalogCategoryValidatorTest {

    private val registry = CanonicalFoodCategoryRegistry()
    private val validator = CatalogCategoryValidator()

    @Test
    fun validateKnownCanonicalCategories() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Apfel",
                category = requireExistingCategory(
                    preferredKeys = listOf(
                        "fruit",
                        "obst"
                    )
                )
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Karotte",
                category = requireExistingCategory(
                    preferredKeys = listOf(
                        "vegetables",
                        "gemuese"
                    )
                )
            )
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(
            entries.size,
            result.inputEntryCount
        )

        assertEquals(
            entries.size,
            result.categorizedEntryCount
        )

        assertEquals(
            0,
            result.missingCategoryCount
        )

        assertEquals(
            0,
            result.unknownCategoryCount
        )

        assertEquals(
            entries.size,
            result.validCategoryCount
        )

        assertTrue(
            result.categoryCounts.values.sum() ==
                    entries.size
        )

        assertTrue(
            result.issues.none {
                it.type ==
                        CatalogCategoryIssueType.UNKNOWN_CATEGORY
            }
        )

        assertTrue(result.valid)
    }

    @Test
    fun detectMissingCategory() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Apfel",
                category = null
            )
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(1, result.inputEntryCount)
        assertEquals(0, result.categorizedEntryCount)
        assertEquals(1, result.missingCategoryCount)
        assertEquals(0, result.validCategoryCount)
        assertEquals(0, result.unknownCategoryCount)

        val issue = result.issues.single {
            it.type ==
                    CatalogCategoryIssueType.MISSING_CATEGORY
        }

        assertEquals(
            CatalogIssueSeverity.ERROR,
            issue.severity
        )

        assertEquals(0, issue.sourceIndex)
        assertEquals("Apfel", issue.itemName)
        assertEquals(null, issue.originalCategory)
        assertFalse(result.valid)
    }

    @Test
    fun detectEmptyCategory() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 2,
                itemName = "Birne",
                category = "   "
            )
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(1, result.missingCategoryCount)
        assertEquals(0, result.categorizedEntryCount)

        val issue = result.issues.single {
            it.type ==
                    CatalogCategoryIssueType.EMPTY_CATEGORY
        }

        assertEquals(
            CatalogIssueSeverity.ERROR,
            issue.severity
        )

        assertEquals(2, issue.sourceIndex)
        assertEquals("   ", issue.originalCategory)
        assertFalse(result.valid)
    }

    @Test
    fun detectUnknownCategory() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 3,
                itemName = "Apfel",
                category = "unknown-category-value"
            )
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(1, result.categorizedEntryCount)
        assertEquals(1, result.unknownCategoryCount)
        assertEquals(0, result.validCategoryCount)

        assertEquals(
            mapOf("unknown-category-value" to 1),
            result.unknownCategoryCounts
        )

        val issue = result.issues.single {
            it.type ==
                    CatalogCategoryIssueType.UNKNOWN_CATEGORY
        }

        assertEquals(
            CatalogIssueSeverity.ERROR,
            issue.severity
        )

        assertEquals(
            "unknown-category-value",
            issue.originalCategory
        )

        assertEquals(
            "unknown-category-value",
            issue.normalizedCategory
        )

        assertEquals(null, issue.suggestedCategoryKey)
        assertFalse(result.valid)
    }

    @Test
    fun countRepeatedUnknownCategories() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Produkt A",
                category = "unbekannt"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Produkt B",
                category = "unbekannt"
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "Produkt C",
                category = "andere-unbekannte-kategorie"
            )
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(3, result.unknownCategoryCount)

        assertEquals(
            mapOf(
                "andere-unbekannte-kategorie" to 1,
                "unbekannt" to 2
            ),
            result.unknownCategoryCounts
        )

        assertFalse(result.valid)
    }

    @Test
    fun normalizeCategoryKeyCase() {
        val canonicalKey = requireExistingCategory(
            preferredKeys = listOf(
                "fruit",
                "vegetables",
                "dairy",
                "cheese"
            )
        )

        val nonCanonicalKey = canonicalKey.uppercase()

        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Testlebensmittel",
                category = nonCanonicalKey
            )
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(1, result.validCategoryCount)
        assertEquals(0, result.unknownCategoryCount)

        assertEquals(
            canonicalKey,
            result.suggestedCategoryMappings[nonCanonicalKey]
        )

        assertTrue(
            result.issues.any {
                it.type ==
                        CatalogCategoryIssueType
                            .CATEGORY_KEY_CASE_MISMATCH &&
                        it.suggestedCategoryKey == canonicalKey
            }
        )
    }

    @Test
    fun normalizeCategoryKeyWhitespace() {
        val canonicalKey = requireExistingCategory()

        val inputCategory = "  $canonicalKey  "

        val result = validator.validate(
            entries = listOf(
                indexedItem(
                    sourceIndex = 0,
                    itemName = "Testlebensmittel",
                    category = inputCategory
                )
            ),
            registry = registry
        )

        assertEquals(1, result.validCategoryCount)

        assertEquals(
            canonicalKey,
            result.suggestedCategoryMappings[inputCategory]
        )

        assertTrue(
            result.issues.any {
                it.type ==
                        CatalogCategoryIssueType
                            .CATEGORY_KEY_WHITESPACE
            }
        )

        assertTrue(
            result.issues.any {
                it.type ==
                        CatalogCategoryIssueType
                            .NON_NORMALIZED_CATEGORY_KEY
            }
        )
    }

    @Test
    fun normalizeCategoryKeyWithUnderscores() {
        val definition = registry.definitions()
            .firstOrNull {
                '-' in it.key
            }
            ?: return

        val categoryWithUnderscores =
            definition.key.replace('-', '_')

        val result = validator.validate(
            entries = listOf(
                indexedItem(
                    sourceIndex = 0,
                    itemName = "Testlebensmittel",
                    category = categoryWithUnderscores
                )
            ),
            registry = registry
        )

        assertEquals(1, result.validCategoryCount)

        assertEquals(
            definition.key,
            result.suggestedCategoryMappings[
                categoryWithUnderscores
            ]
        )

        assertTrue(
            result.issues.any {
                it.type ==
                        CatalogCategoryIssueType
                            .CATEGORY_KEY_UNDERSCORE
            }
        )
    }

    @Test
    fun resolveDisplayNameUsedAsCategoryKey() {
        val definition = registry.definitions()
            .firstOrNull {
                it.displayName.isNotBlank() &&
                        it.displayName != it.key
            }
            ?: error(
                "Registry must contain at least one definition with a " +
                        "displayName different from its key."
            )

        val result = validator.validate(
            entries = listOf(
                indexedItem(
                    sourceIndex = 0,
                    itemName = "Testlebensmittel",
                    category = definition.displayName
                )
            ),
            registry = registry
        )

        assertEquals(1, result.validCategoryCount)
        assertEquals(0, result.unknownCategoryCount)

        assertEquals(
            definition.key,
            result.suggestedCategoryMappings[
                definition.displayName
            ]
        )

        val issue = result.issues.firstOrNull {
            it.type ==
                    CatalogCategoryIssueType
                        .CATEGORY_DISPLAY_NAME_USED_AS_KEY
        }

        assertNotNull(issue)
        assertEquals(definition.key, issue.suggestedCategoryKey)
    }

    @Test
    fun countAssignedCanonicalCategories() {
        val categoryKey = requireExistingCategory()

        val entries = listOf(
            indexedItem(0, "Lebensmittel A", categoryKey),
            indexedItem(1, "Lebensmittel B", categoryKey),
            indexedItem(2, "Lebensmittel C", categoryKey)
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(
            3,
            result.categoryCounts[categoryKey]
        )

        assertEquals(
            1,
            result.uniqueAssignedCategoryCount
        )

        assertEquals(
            3,
            result.validCategoryCount
        )
    }

    @Test
    fun countMultipleAssignedCategories() {
        val categoryKeys = registry.definitions()
            .map { it.key }
            .distinct()
            .take(3)

        require(categoryKeys.size == 3) {
            "Registry must contain at least three categories."
        }

        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Lebensmittel A",
                category = categoryKeys[0]
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Lebensmittel B",
                category = categoryKeys[1]
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "Lebensmittel C",
                category = categoryKeys[2]
            ),
            indexedItem(
                sourceIndex = 3,
                itemName = "Lebensmittel D",
                category = categoryKeys[0]
            )
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(4, result.validCategoryCount)
        assertEquals(3, result.uniqueAssignedCategoryCount)
        assertEquals(2, result.categoryCounts[categoryKeys[0]])
        assertEquals(1, result.categoryCounts[categoryKeys[1]])
        assertEquals(1, result.categoryCounts[categoryKeys[2]])
    }

    @Test
    fun reportUnusedRegistryCategories() {
        val assignedCategory = requireExistingCategory()

        val result = validator.validate(
            entries = listOf(
                indexedItem(
                    sourceIndex = 0,
                    itemName = "Testlebensmittel",
                    category = assignedCategory
                )
            ),
            registry = registry
        )

        val expectedUnusedKeys = registry.definitions()
            .map { it.key }
            .filterNot { it == assignedCategory }
            .sorted()

        assertEquals(
            expectedUnusedKeys,
            result.unusedCategoryKeys
        )

        assertEquals(
            result.unusedCategoryKeys.sorted(),
            result.unusedCategoryKeys
        )

        assertEquals(
            result.unusedCategoryKeys.distinct().size,
            result.unusedCategoryKeys.size
        )
    }

    @Test
    fun emitIssueForUnusedCategories() {
        val assignedCategory = requireExistingCategory()

        val result = validator.validate(
            entries = listOf(
                indexedItem(
                    sourceIndex = 0,
                    itemName = "Testlebensmittel",
                    category = assignedCategory
                )
            ),
            registry = registry
        )

        val issueKeys = result.issues
            .filter {
                it.type ==
                        CatalogCategoryIssueType
                            .CATEGORY_WITHOUT_CATALOG_ITEMS
            }
            .mapNotNull { it.normalizedCategory }
            .sorted()

        assertEquals(
            result.unusedCategoryKeys,
            issueKeys
        )

        assertTrue(
            result.issues
                .filter {
                    it.type ==
                            CatalogCategoryIssueType
                                .CATEGORY_WITHOUT_CATALOG_ITEMS
                }
                .all {
                    it.severity == CatalogIssueSeverity.INFO &&
                            it.sourceIndex == null
                }
        )
    }

    @Test
    fun identifyDirectRootCategoryAssignment() {
        val rootDefinition = registry.definitions()
            .firstOrNull { definition ->
                definition.parentKey == null &&
                        registry.children(definition.key).isNotEmpty()
            }
            ?: error(
                "Registry must contain at least one root category with " +
                        "child categories."
            )

        val result = validator.validate(
            entries = listOf(
                indexedItem(
                    sourceIndex = 0,
                    itemName = "Testlebensmittel",
                    category = rootDefinition.key
                )
            ),
            registry = registry
        )

        assertEquals(
            1,
            result.rootCategoryAssignmentCount
        )

        val issue = result.issues.firstOrNull {
            it.type ==
                    CatalogCategoryIssueType
                        .ROOT_CATEGORY_ASSIGNED_TO_ITEM &&
                    it.sourceIndex == 0
        }

        assertNotNull(issue)
        assertEquals(CatalogIssueSeverity.INFO, issue.severity)
    }

    @Test
    fun leafCategoryAssignmentIsNotRootAssignment() {
        val leafDefinition = registry.definitions()
            .firstOrNull { definition ->
                definition.parentKey != null &&
                        registry.children(definition.key).isEmpty()
            }
            ?: return

        val result = validator.validate(
            entries = listOf(
                indexedItem(
                    sourceIndex = 0,
                    itemName = "Testlebensmittel",
                    category = leafDefinition.key
                )
            ),
            registry = registry
        )

        assertEquals(
            0,
            result.rootCategoryAssignmentCount
        )

        assertTrue(
            result.issues.none {
                it.type ==
                        CatalogCategoryIssueType
                            .ROOT_CATEGORY_ASSIGNED_TO_ITEM &&
                        it.sourceIndex == 0
            }
        )
    }

    @Test
    fun resultCountsRemainConsistent() {
        val categoryKey = requireExistingCategory()

        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Lebensmittel A",
                category = categoryKey
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Lebensmittel B",
                category = null
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "Lebensmittel C",
                category = "unknown-category-value"
            )
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(
            result.inputEntryCount,
            result.categorizedEntryCount +
                    result.missingCategoryCount
        )

        assertEquals(
            result.categorizedEntryCount,
            result.validCategoryCount +
                    result.unknownCategoryCount
        )

        assertEquals(
            result.validCategoryCount,
            result.categoryCounts.values.sum()
        )

        assertEquals(
            result.unknownCategoryCount,
            result.unknownCategoryCounts.values.sum()
        )

        assertEquals(
            result.uniqueAssignedCategoryCount,
            result.categoryCounts.size
        )
    }

    @Test
    fun issueOrderingIsDeterministic() {
        val categoryKey = requireExistingCategory()

        val entries = listOf(
            indexedItem(
                sourceIndex = 3,
                itemName = "Lebensmittel D",
                category = "unknown-category-value"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Lebensmittel B",
                category = null
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "Lebensmittel C",
                category = "  $categoryKey  "
            )
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(
            result.issues.sortedWith(issueComparator()),
            result.issues
        )
    }

    @Test
    fun validationIsDeterministic() {
        val categoryKey = requireExistingCategory()

        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Lebensmittel A",
                category = categoryKey
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Lebensmittel B",
                category = null
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "Lebensmittel C",
                category = "unknown-category-value"
            )
        )

        val first = validator.validate(entries, registry)
        val second = validator.validate(entries, registry)
        val third = validator.validate(entries, registry)

        assertEquals(first, second)
        assertEquals(first, third)
    }

    @Test
    fun validationIsIndependentOfInputOrder() {
        val categoryKeys = registry.definitions()
            .map { it.key }
            .distinct()
            .take(2)

        require(categoryKeys.size == 2)

        val entries = listOf(
            indexedItem(
                sourceIndex = 4,
                itemName = "Lebensmittel A",
                category = categoryKeys[0]
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Lebensmittel B",
                category = categoryKeys[1]
            ),
            indexedItem(
                sourceIndex = 7,
                itemName = "Lebensmittel C",
                category = null
            )
        )

        val forward = validator.validate(
            entries = entries,
            registry = registry
        )

        val reversed = validator.validate(
            entries = entries.reversed(),
            registry = registry
        )

        assertEquals(forward, reversed)
    }

    @Test
    fun rejectDuplicateSourceIndices() {
        val categoryKey = requireExistingCategory()

        val entries = listOf(
            indexedItem(
                sourceIndex = 4,
                itemName = "Lebensmittel A",
                category = categoryKey
            ),
            indexedItem(
                sourceIndex = 4,
                itemName = "Lebensmittel B",
                category = categoryKey
            )
        )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                validator.validate(
                    entries = entries,
                    registry = registry
                )
            }

        assertTrue(
            exception.message
                ?.contains("duplicate sourceIndex") == true
        )
    }

    @Test
    fun emptyInputProducesValidEmptyAssignmentResult() {
        val result = validator.validate(
            entries = emptyList(),
            registry = registry
        )

        assertEquals(0, result.inputEntryCount)
        assertEquals(0, result.categorizedEntryCount)
        assertEquals(0, result.missingCategoryCount)
        assertEquals(0, result.unknownCategoryCount)
        assertEquals(0, result.validCategoryCount)
        assertEquals(0, result.uniqueAssignedCategoryCount)
        assertEquals(0, result.rootCategoryAssignmentCount)
        assertTrue(result.categoryCounts.isEmpty())
        assertTrue(result.unknownCategoryCounts.isEmpty())
        assertTrue(result.suggestedCategoryMappings.isEmpty())

        assertEquals(
            registry.definitions()
                .map { it.key }
                .sorted(),
            result.unusedCategoryKeys
        )

        assertTrue(result.valid)
    }

    @Test
    fun suggestedMappingsAreSorted() {
        val definitions = registry.definitions()
            .filter {
                it.key.any(Char::isLetter)
            }
            .take(3)

        require(definitions.size == 3)

        val entries = definitions
            .mapIndexed { index, definition ->
                indexedItem(
                    sourceIndex = index,
                    itemName = "Lebensmittel $index",
                    category = definition.key.uppercase()
                )
            }
            .reversed()

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(
            result.suggestedCategoryMappings.keys.sorted(),
            result.suggestedCategoryMappings.keys.toList()
        )
    }

    @Test
    fun categoryCountsAreSorted() {
        val categoryKeys = registry.definitions()
            .map { it.key }
            .distinct()
            .take(4)
            .reversed()

        require(categoryKeys.size == 4)

        val entries = categoryKeys.mapIndexed {
                sourceIndex,
                categoryKey ->
            indexedItem(
                sourceIndex = sourceIndex,
                itemName = "Lebensmittel $sourceIndex",
                category = categoryKey
            )
        }

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(
            result.categoryCounts.keys.sorted(),
            result.categoryCounts.keys.toList()
        )
    }

    @Test
    fun unknownCategoryCountsAreSorted() {
        val entries = listOf(
            indexedItem(0, "Lebensmittel A", "z-category"),
            indexedItem(1, "Lebensmittel B", "a-category"),
            indexedItem(2, "Lebensmittel C", "m-category")
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        assertEquals(
            listOf(
                "a-category",
                "m-category",
                "z-category"
            ),
            result.unknownCategoryCounts.keys.toList()
        )
    }

    @Test
    fun everyEntrySpecificIssueReferencesItsEntry() {
        val categoryKey = requireExistingCategory()

        val entries = listOf(
            indexedItem(
                sourceIndex = 5,
                itemName = "Lebensmittel A",
                category = null
            ),
            indexedItem(
                sourceIndex = 8,
                itemName = "Lebensmittel B",
                category = "unknown-category-value"
            ),
            indexedItem(
                sourceIndex = 11,
                itemName = "Lebensmittel C",
                category = "  $categoryKey  "
            )
        )

        val result = validator.validate(
            entries = entries,
            registry = registry
        )

        val entriesByIndex = entries.associateBy {
            it.sourceIndex
        }

        result.issues
            .filter { it.sourceIndex != null }
            .forEach { issue ->
                val sourceIndex =
                    requireNotNull(issue.sourceIndex)

                val entry = entriesByIndex[sourceIndex]

                assertNotNull(
                    entry,
                    "Issue references unknown sourceIndex $sourceIndex."
                )

                assertEquals(
                    entry.item.itemname,
                    issue.itemName
                )
            }
    }

    private fun indexedItem(
        sourceIndex: Int,
        itemName: String,
        category: String?
    ): IndexedCatalogFoodItem =
        IndexedCatalogFoodItem(
            sourceIndex = sourceIndex,
            item = CatalogFoodItem(
                itemname = itemName,
                category = category,
                production = null,
                normalized = null,
                plural = null,
                colloquial = emptyList(),
                phoneticTokens = emptyList(),
                autocompleteTokens = emptyList(),
                normalizedEnglish = null
            )
        )

    private fun requireExistingCategory(
        preferredKeys: List<String> = emptyList()
    ): String {
        val definitions = registry.definitions()

        preferredKeys.firstOrNull { preferredKey ->
            definitions.any { it.key == preferredKey }
        }?.let {
            return it
        }

        return definitions
            .firstOrNull()
            ?.key
            ?: error(
                "CanonicalFoodCategoryRegistry must contain at least " +
                        "one category definition."
            )
    }

    private fun issueComparator():
            Comparator<CatalogCategoryIssue> =
        compareBy<CatalogCategoryIssue>(
            { severityRank(it.severity) },
            { it.type.name },
            { it.sourceIndex ?: Int.MAX_VALUE },
            { it.itemName?.lowercase() ?: "" },
            {
                it.originalCategory
                    ?.lowercase()
                    ?: ""
            },
            { it.normalizedCategory ?: "" },
            { it.suggestedCategoryKey ?: "" },
            { it.message }
        )

    private fun severityRank(
        severity: CatalogIssueSeverity
    ): Int =
        when (severity) {
            CatalogIssueSeverity.ERROR -> 0
            CatalogIssueSeverity.WARNING -> 1
            CatalogIssueSeverity.INFO -> 2
        }
}