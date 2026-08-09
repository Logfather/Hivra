package de.shopme.testing.system.tools.knowledge.catalog.validation

import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CanonicalFoodKeyNormalizer
import java.util.Locale

class NormalizedCatalogValidator(
    private val keyNormalizer: CanonicalFoodKeyNormalizer
) {

    fun validate(
        items: List<CatalogFoodItem>,
        categoryRegistry: CanonicalFoodCategoryRegistry
    ): NormalizedCatalogValidationResult {
        val issues =
            mutableListOf<NormalizedCatalogValidationIssue>()

        items.forEachIndexed { index, item ->
            validateItem(
                sourceIndex = index,
                item = item,
                categoryRegistry = categoryRegistry,
                issues = issues
            )
        }

        addDuplicateNormalizedKeyIssues(
            items = items,
            issues = issues
        )

        addDuplicateNameIssues(
            items = items,
            issues = issues
        )

        val expectedOrder = items.sortedWith(
            compareBy<CatalogFoodItem>(
                {
                    it.category
                        ?.lowercase(Locale.ROOT)
                        ?: ""
                },
                {
                    it.itemname.lowercase(
                        Locale.GERMAN
                    )
                },
                {
                    it.normalized
                        ?.lowercase(Locale.ROOT)
                        ?: ""
                }
            )
        )

        if (items != expectedOrder) {
            issues += NormalizedCatalogValidationIssue(
                type =
                    NormalizedCatalogValidationIssueType
                        .NON_DETERMINISTIC_ORDER,
                sourceIndex = null,
                itemName = null,
                value = null,
                message =
                    "Normalized catalog items are not deterministically sorted."
            )
        }

        val normalizedIssues = issues
            .distinct()
            .sortedWith(
                compareBy<NormalizedCatalogValidationIssue>(
                    { it.type.name },
                    { it.sourceIndex ?: Int.MAX_VALUE },
                    {
                        it.itemName
                            ?.lowercase(Locale.GERMAN)
                            ?: ""
                    },
                    { it.value ?: "" },
                    { it.message }
                )
            )

        val countsByType = normalizedIssues
            .groupingBy { it.type }
            .eachCount()
            .toList()
            .sortedBy { it.first.name }
            .associate { it }

        return NormalizedCatalogValidationResult(
            version =
                NormalizedCatalogValidationResult.CURRENT_VERSION,
            inputEntryCount = items.size,
            issueCount = normalizedIssues.size,
            issueCountsByType = countsByType,
            issues = normalizedIssues,
            valid = normalizedIssues.isEmpty()
        )
    }

    private fun validateItem(
        sourceIndex: Int,
        item: CatalogFoodItem,
        categoryRegistry: CanonicalFoodCategoryRegistry,
        issues: MutableList<NormalizedCatalogValidationIssue>
    ) {
        if (item.itemname.isBlank()) {
            issues += issue(
                type =
                    NormalizedCatalogValidationIssueType.EMPTY_ITEM_NAME,
                sourceIndex = sourceIndex,
                item = item,
                value = item.itemname,
                message = "itemname must not be blank."
            )
        }

        val normalizedKey = item.normalized

        if (normalizedKey.isNullOrBlank()) {
            issues += issue(
                type =
                    NormalizedCatalogValidationIssueType
                        .MISSING_NORMALIZED_KEY,
                sourceIndex = sourceIndex,
                item = item,
                value = normalizedKey,
                message = "normalized key is missing."
            )
        } else if (!keyNormalizer.isCanonicalKey(normalizedKey)) {
            issues += issue(
                type =
                    NormalizedCatalogValidationIssueType
                        .INVALID_NORMALIZED_KEY,
                sourceIndex = sourceIndex,
                item = item,
                value = normalizedKey,
                message =
                    "normalized key '$normalizedKey' is not canonical."
            )
        }

        val category = item.category

        if (category.isNullOrBlank()) {
            issues += issue(
                type =
                    NormalizedCatalogValidationIssueType.MISSING_CATEGORY,
                sourceIndex = sourceIndex,
                item = item,
                value = category,
                message = "category is missing."
            )
        } else if (!categoryRegistry.contains(category)) {
            issues += issue(
                type =
                    NormalizedCatalogValidationIssueType.UNKNOWN_CATEGORY,
                sourceIndex = sourceIndex,
                item = item,
                value = category,
                message =
                    "Category '$category' is not registered."
            )
        }

        validateList(
            sourceIndex = sourceIndex,
            item = item,
            fieldName = "colloquial",
            values = item.colloquial,
            issues = issues
        )

        validateList(
            sourceIndex = sourceIndex,
            item = item,
            fieldName = "phoneticTokens",
            values = item.phoneticTokens,
            issues = issues
        )

        validateList(
            sourceIndex = sourceIndex,
            item = item,
            fieldName = "autocompleteTokens",
            values = item.autocompleteTokens,
            issues = issues
        )
    }

    private fun validateList(
        sourceIndex: Int,
        item: CatalogFoodItem,
        fieldName: String,
        values: List<String>,
        issues: MutableList<NormalizedCatalogValidationIssue>
    ) {
        if (values.any(String::isBlank)) {
            issues += issue(
                type =
                    NormalizedCatalogValidationIssueType.BLANK_LIST_VALUE,
                sourceIndex = sourceIndex,
                item = item,
                value = fieldName,
                message =
                    "$fieldName contains blank values."
            )
        }

        if (values.distinct().size != values.size) {
            issues += issue(
                type =
                    NormalizedCatalogValidationIssueType
                        .DUPLICATE_LIST_VALUE,
                sourceIndex = sourceIndex,
                item = item,
                value = fieldName,
                message =
                    "$fieldName contains duplicate values."
            )
        }
    }

    private fun addDuplicateNormalizedKeyIssues(
        items: List<CatalogFoodItem>,
        issues: MutableList<NormalizedCatalogValidationIssue>
    ) {
        items
            .mapIndexedNotNull { index, item ->
                item.normalized
                    ?.takeIf(String::isNotBlank)
                    ?.let { key ->
                        key to (index to item)
                    }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .filterValues { it.size > 1 }
            .toSortedMap()
            .forEach { (key, entries) ->
                entries.forEach { (index, item) ->
                    issues += issue(
                        type =
                            NormalizedCatalogValidationIssueType
                                .DUPLICATE_NORMALIZED_KEY,
                        sourceIndex = index,
                        item = item,
                        value = key,
                        message =
                            "normalized key '$key' is duplicated."
                    )
                }
            }
    }

    private fun addDuplicateNameIssues(
        items: List<CatalogFoodItem>,
        issues: MutableList<NormalizedCatalogValidationIssue>
    ) {
        items
            .mapIndexed { index, item ->
                item.itemname
                    .trim()
                    .lowercase(Locale.GERMAN) to
                        (index to item)
            }
            .filter { it.first.isNotBlank() }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .filterValues { it.size > 1 }
            .toSortedMap()
            .forEach { (name, entries) ->
                entries.forEach { (index, item) ->
                    issues += issue(
                        type =
                            NormalizedCatalogValidationIssueType
                                .DUPLICATE_ITEM_NAME,
                        sourceIndex = index,
                        item = item,
                        value = name,
                        message =
                            "Canonical item name '$name' is duplicated."
                    )
                }
            }
    }

    private fun issue(
        type: NormalizedCatalogValidationIssueType,
        sourceIndex: Int,
        item: CatalogFoodItem,
        value: String?,
        message: String
    ): NormalizedCatalogValidationIssue =
        NormalizedCatalogValidationIssue(
            type = type,
            sourceIndex = sourceIndex,
            itemName = item.itemname,
            value = value,
            message = message
        )
}