package de.shopme.testing.system.tools.knowledge.catalog.validation

import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationResult
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogQualityIssue
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogQualityIssueType
import java.util.Locale

class CatalogQualityValidator {

    fun validate(
        entries: List<IndexedCatalogFoodItem>,
        normalizations: List<CatalogNormalizationResult>
    ): CatalogQualityValidationResult {
        validateInputs(
            entries = entries,
            normalizations = normalizations
        )

        val normalizationsBySourceIndex = normalizations.associateBy {
            it.sourceIndex
        }

        val issues = mutableListOf<CatalogQualityIssue>()

        entries
            .sortedBy { it.sourceIndex }
            .forEach { entry ->
                val normalization = normalizationsBySourceIndex
                    .getValue(entry.sourceIndex)

                validateEntry(
                    entry = entry,
                    normalization = normalization,
                    issues = issues
                )
            }

        addDuplicateItemNameIssues(
            entries = entries,
            issues = issues
        )

        addDuplicateNormalizedKeyIssues(
            entries = entries,
            normalizations = normalizations,
            issues = issues
        )

        val normalizedIssues = issues
            .distinct()
            .sortedWith(ISSUE_COMPARATOR)

        val affectedSourceIndices = normalizedIssues
            .mapNotNull { it.sourceIndex }
            .distinct()
            .sorted()

        val issueCountsByType =
            normalizedIssues
                .groupingBy { it.type }
                .eachCount()
                .entries
                .sortedBy { it.key.name }
                .associateTo(linkedMapOf()) {
                    it.key to it.value
                }

        val issueCountsBySeverity =
            normalizedIssues
                .groupingBy { it.severity }
                .eachCount()
                .entries
                .sortedBy { it.key.name }
                .associateTo(linkedMapOf()) {
                    it.key to it.value
                }

        val issueCountsByField =
            normalizedIssues
                .groupingBy {
                    it.field ?: GLOBAL_FIELD
                }
                .eachCount()
                .toSortedMap()

        val errorCount = normalizedIssues.count {
            it.severity == CatalogIssueSeverity.ERROR
        }

        val warningCount = normalizedIssues.count {
            it.severity == CatalogIssueSeverity.WARNING
        }

        val infoCount = normalizedIssues.count {
            it.severity == CatalogIssueSeverity.INFO
        }

        val uniqueItemNameCount = entries
            .map { entry ->
                normalizeComparisonText(entry.item.itemname)
            }
            .filter(String::isNotBlank)
            .distinct()
            .size

        val uniqueNormalizedKeyCount = normalizations
            .map { it.computedNormalizedKey }
            .filter(String::isNotBlank)
            .distinct()
            .size

        val missingItemNameCount = entries.count {
            it.item.itemname.isBlank()
        }

        val missingCategoryCount = entries.count {
            it.item.category.isNullOrBlank()
        }

        val missingNormalizedKeyCount = entries.count {
            it.item.normalized.isNullOrBlank()
        }

        val duplicateItemNameCount =
            countEntriesInDuplicateGroups(
                values = entries.map {
                    normalizeComparisonText(
                        it.item.itemname
                    )
                }
            )

        val duplicateNormalizedKeyCount =
            countEntriesInDuplicateGroups(
                values = normalizations.map {
                    it.computedNormalizedKey
                }
            )

        val invalidPluralCount = normalizedIssues.count {
            it.field == FIELD_PLURAL
        }

        return CatalogQualityValidationResult(
            inputEntryCount = entries.size,
            validEntryCount =
                entries.size - affectedSourceIndices.size,
            affectedEntryCount =
                affectedSourceIndices.size,
            issueCount = normalizedIssues.size,
            errorCount = errorCount,
            warningCount = warningCount,
            infoCount = infoCount,
            uniqueItemNameCount = uniqueItemNameCount,
            uniqueNormalizedKeyCount =
                uniqueNormalizedKeyCount,
            missingItemNameCount =
                missingItemNameCount,
            missingCategoryCount =
                missingCategoryCount,
            missingNormalizedKeyCount =
                missingNormalizedKeyCount,
            duplicateItemNameCount =
                duplicateItemNameCount,
            duplicateNormalizedKeyCount =
                duplicateNormalizedKeyCount,
            invalidPluralCount =
                invalidPluralCount,
            issueCountsByType =
                issueCountsByType,
            issueCountsBySeverity =
                issueCountsBySeverity,
            issueCountsByField =
                issueCountsByField,
            affectedSourceIndices =
                affectedSourceIndices,
            issues = normalizedIssues,
            valid = errorCount == 0
        )
    }

    private fun validateEntry(
        entry: IndexedCatalogFoodItem,
        normalization: CatalogNormalizationResult,
        issues: MutableList<CatalogQualityIssue>
    ) {
        val item = entry.item

        if (item.itemname.isBlank()) {
            addIssueIfSupported(
                issues = issues,
                sourceIndex = entry.sourceIndex,
                itemName = null,
                typeNames = listOf(
                    "MISSING_ITEM_NAME",
                    "EMPTY_ITEM_NAME"
                ),
                severity = CatalogIssueSeverity.ERROR,
                field = FIELD_ITEM_NAME,
                message = "Catalog item name is missing or blank."
            )
        }

        if (
            item.itemname.isNotBlank() &&
            item.itemname != item.itemname.trim()
        ) {
            addIssueIfSupported(
                issues = issues,
                sourceIndex = entry.sourceIndex,
                itemName = item.itemname,
                typeNames = listOf(
                    "SURROUNDING_WHITESPACE",
                    "ITEM_NAME_WHITESPACE",
                    "NON_CANONICAL_ITEM_NAME"
                ),
                severity = CatalogIssueSeverity.WARNING,
                field = FIELD_ITEM_NAME,
                message =
                    "Catalog item name contains surrounding whitespace."
            )
        }

        if (
            MULTIPLE_WHITESPACE_REGEX.containsMatchIn(
                item.itemname
            )
        ) {
            addIssueIfSupported(
                issues = issues,
                sourceIndex = entry.sourceIndex,
                itemName = item.itemname,
                typeNames = listOf(
                    "MULTIPLE_WHITESPACE",
                    "ITEM_NAME_WHITESPACE",
                    "NON_CANONICAL_ITEM_NAME"
                ),
                severity = CatalogIssueSeverity.WARNING,
                field = FIELD_ITEM_NAME,
                message =
                    "Catalog item name contains repeated whitespace."
            )
        }

        if (item.category.isNullOrBlank()) {
            addIssueIfSupported(
                issues = issues,
                sourceIndex = entry.sourceIndex,
                itemName = item.itemname
                    .takeIf(String::isNotBlank),
                typeNames = listOf(
                    "MISSING_CATEGORY",
                    "EMPTY_CATEGORY"
                ),
                severity = CatalogIssueSeverity.ERROR,
                field = FIELD_CATEGORY,
                message = "Catalog category is missing or blank."
            )
        }

        if (item.normalized.isNullOrBlank()) {
            addIssueIfSupported(
                issues = issues,
                sourceIndex = entry.sourceIndex,
                itemName = item.itemname
                    .takeIf(String::isNotBlank),
                typeNames = listOf(
                    "MISSING_NORMALIZED_KEY",
                    "EMPTY_NORMALIZED_KEY"
                ),
                severity = CatalogIssueSeverity.ERROR,
                field = FIELD_NORMALIZED,
                message =
                    "Catalog normalized key is missing or blank."
            )
        } else if (
            item.normalized !=
            normalization.computedNormalizedKey
        ) {
            addIssueIfSupported(
                issues = issues,
                sourceIndex = entry.sourceIndex,
                itemName = item.itemname
                    .takeIf(String::isNotBlank),
                typeNames = listOf(
                    "NON_CANONICAL_NORMALIZED_KEY",
                    "INVALID_NORMALIZED_KEY",
                    "NORMALIZED_KEY_MISMATCH"
                ),
                severity = CatalogIssueSeverity.WARNING,
                field = FIELD_NORMALIZED,
                message =
                    "Stored normalized key differs from the computed " +
                            "canonical key '${normalization.computedNormalizedKey}'."
            )
        }

        validateList(
            sourceIndex = entry.sourceIndex,
            itemName = item.itemname,
            field = FIELD_COLLOQUIAL,
            values = item.colloquial,
            issues = issues
        )

        validateList(
            sourceIndex = entry.sourceIndex,
            itemName = item.itemname,
            field = FIELD_PHONETIC_TOKENS,
            values = item.phoneticTokens,
            issues = issues
        )

        validateList(
            sourceIndex = entry.sourceIndex,
            itemName = item.itemname,
            field = FIELD_AUTOCOMPLETE_TOKENS,
            values = item.autocompleteTokens,
            issues = issues
        )

        val originalPlural = item.plural

        if (
            originalPlural != null &&
            originalPlural.isBlank()
        ) {
            addIssueIfSupported(
                issues = issues,
                sourceIndex = entry.sourceIndex,
                itemName = item.itemname
                    .takeIf(String::isNotBlank),
                typeNames = listOf(
                    "INVALID_PLURAL",
                    "EMPTY_PLURAL"
                ),
                severity = CatalogIssueSeverity.WARNING,
                field = FIELD_PLURAL,
                message = "Catalog plural is blank."
            )
        } else if (
            originalPlural != null &&
            originalPlural !=
            normalization.computedPlural
        ) {
            addIssueIfSupported(
                issues = issues,
                sourceIndex = entry.sourceIndex,
                itemName = item.itemname
                    .takeIf(String::isNotBlank),
                typeNames = listOf(
                    "INVALID_PLURAL",
                    "NON_CANONICAL_PLURAL"
                ),
                severity = CatalogIssueSeverity.WARNING,
                field = FIELD_PLURAL,
                message =
                    "Stored plural differs from the normalized plural."
            )
        }
    }

    private fun validateList(
        sourceIndex: Int,
        itemName: String,
        field: String,
        values: List<String>,
        issues: MutableList<CatalogQualityIssue>
    ) {
        if (values.any(String::isBlank)) {
            addIssueIfSupported(
                issues = issues,
                sourceIndex = sourceIndex,
                itemName = itemName
                    .takeIf(String::isNotBlank),
                typeNames = listOf(
                    "EMPTY_LIST_VALUE",
                    "BLANK_TOKEN",
                    "INVALID_LIST_VALUE"
                ),
                severity = CatalogIssueSeverity.WARNING,
                field = field,
                message =
                    "Catalog field '$field' contains blank values."
            )
        }

        if (values.distinct().size != values.size) {
            addIssueIfSupported(
                issues = issues,
                sourceIndex = sourceIndex,
                itemName = itemName
                    .takeIf(String::isNotBlank),
                typeNames = listOf(
                    "DUPLICATE_LIST_VALUE",
                    "DUPLICATE_TOKEN"
                ),
                severity = CatalogIssueSeverity.WARNING,
                field = field,
                message =
                    "Catalog field '$field' contains duplicate values."
            )
        }

        val caseInsensitiveValues = values
            .filter(String::isNotBlank)
            .map {
                normalizeComparisonText(it)
            }

        if (
            caseInsensitiveValues.distinct().size !=
            caseInsensitiveValues.size
        ) {
            addIssueIfSupported(
                issues = issues,
                sourceIndex = sourceIndex,
                itemName = itemName
                    .takeIf(String::isNotBlank),
                typeNames = listOf(
                    "DUPLICATE_LIST_VALUE",
                    "DUPLICATE_TOKEN"
                ),
                severity = CatalogIssueSeverity.WARNING,
                field = field,
                message =
                    "Catalog field '$field' contains " +
                            "case-insensitive duplicate values."
            )
        }
    }

    private fun addDuplicateItemNameIssues(
        entries: List<IndexedCatalogFoodItem>,
        issues: MutableList<CatalogQualityIssue>
    ) {
        entries
            .groupBy {
                normalizeComparisonText(
                    it.item.itemname
                )
            }
            .filterKeys(String::isNotBlank)
            .filterValues { it.size > 1 }
            .toSortedMap()
            .forEach { (normalizedName, matchingEntries) ->
                matchingEntries
                    .sortedBy { it.sourceIndex }
                    .forEach { entry ->
                        addIssueIfSupported(
                            issues = issues,
                            sourceIndex = entry.sourceIndex,
                            itemName = entry.item.itemname,
                            typeNames = listOf(
                                "DUPLICATE_ITEM_NAME",
                                "DUPLICATE_NAME"
                            ),
                            severity =
                                CatalogIssueSeverity.WARNING,
                            field = FIELD_ITEM_NAME,
                            message =
                                "Catalog item name collides with another " +
                                        "entry after normalization: " +
                                        "'$normalizedName'."
                        )
                    }
            }
    }

    private fun addDuplicateNormalizedKeyIssues(
        entries: List<IndexedCatalogFoodItem>,
        normalizations: List<CatalogNormalizationResult>,
        issues: MutableList<CatalogQualityIssue>
    ) {
        val entriesBySourceIndex = entries.associateBy {
            it.sourceIndex
        }

        normalizations
            .groupBy { it.computedNormalizedKey }
            .filterKeys(String::isNotBlank)
            .filterValues { it.size > 1 }
            .toSortedMap()
            .forEach { (normalizedKey, matchingNormalizations) ->
                matchingNormalizations
                    .sortedBy { it.sourceIndex }
                    .forEach { normalization ->
                        val entry = entriesBySourceIndex
                            .getValue(normalization.sourceIndex)

                        addIssueIfSupported(
                            issues = issues,
                            sourceIndex =
                                normalization.sourceIndex,
                            itemName =
                                entry.item.itemname,
                            typeNames = listOf(
                                "DUPLICATE_NORMALIZED_KEY",
                                "NORMALIZED_KEY_COLLISION"
                            ),
                            severity =
                                CatalogIssueSeverity.ERROR,
                            field = FIELD_NORMALIZED,
                            message =
                                "Computed normalized key '$normalizedKey' " +
                                        "is used by multiple catalog entries."
                        )
                    }
            }
    }

    private fun addIssueIfSupported(
        issues: MutableList<CatalogQualityIssue>,
        sourceIndex: Int?,
        itemName: String?,
        typeNames: List<String>,
        severity: CatalogIssueSeverity,
        field: String?,
        message: String
    ) {
        val issueType = resolveIssueType(typeNames)
            ?: return

        issues += CatalogQualityIssue(
            sourceIndex = sourceIndex,
            itemName = itemName,
            type = issueType,
            severity = severity,
            field = field,
            message = message
        )
    }

    private fun resolveIssueType(
        preferredNames: List<String>
    ): CatalogQualityIssueType? {
        val valuesByName =
            CatalogQualityIssueType.entries.associateBy {
                it.name
            }

        return preferredNames.firstNotNullOfOrNull {
            valuesByName[it]
        }
    }

    private fun validateInputs(
        entries: List<IndexedCatalogFoodItem>,
        normalizations: List<CatalogNormalizationResult>
    ) {
        val duplicateEntryIndices = entries
            .groupBy { it.sourceIndex }
            .filterValues { it.size > 1 }
            .keys
            .sorted()

        require(duplicateEntryIndices.isEmpty()) {
            "Catalog entries contain duplicate sourceIndex values: " +
                    duplicateEntryIndices.joinToString(", ")
        }

        val duplicateNormalizationIndices = normalizations
            .groupBy { it.sourceIndex }
            .filterValues { it.size > 1 }
            .keys
            .sorted()

        require(duplicateNormalizationIndices.isEmpty()) {
            "Catalog normalizations contain duplicate sourceIndex values: " +
                    duplicateNormalizationIndices.joinToString(", ")
        }

        val entryIndices = entries
            .mapTo(sortedSetOf()) { it.sourceIndex }

        val normalizationIndices = normalizations
            .mapTo(sortedSetOf()) { it.sourceIndex }

        val missingNormalizationIndices =
            entryIndices - normalizationIndices

        require(missingNormalizationIndices.isEmpty()) {
            "Missing catalog normalizations for source indices: " +
                    missingNormalizationIndices.joinToString(", ")
        }

        val unexpectedNormalizationIndices =
            normalizationIndices - entryIndices

        require(unexpectedNormalizationIndices.isEmpty()) {
            "Catalog normalizations reference unknown source indices: " +
                    unexpectedNormalizationIndices.joinToString(", ")
        }
    }

    private fun countEntriesInDuplicateGroups(
        values: List<String>
    ): Int =
        values
            .filter(String::isNotBlank)
            .groupingBy { it }
            .eachCount()
            .values
            .filter { it > 1 }
            .sum()

    private fun normalizeComparisonText(
        value: String
    ): String =
        value
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .lowercase(Locale.ROOT)

    private companion object {

        const val FIELD_ITEM_NAME = "itemname"
        const val FIELD_CATEGORY = "category"
        const val FIELD_NORMALIZED = "normalized"
        const val FIELD_PLURAL = "plural"
        const val FIELD_COLLOQUIAL = "colloquial"
        const val FIELD_PHONETIC_TOKENS = "phonetic_tokens"
        const val FIELD_AUTOCOMPLETE_TOKENS =
            "autocomplete_tokens"

        const val GLOBAL_FIELD = "<global>"

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        val ISSUE_COMPARATOR =
            compareBy<CatalogQualityIssue>(
                { severityRank(it.severity) },
                { it.type.name },
                { it.sourceIndex ?: Int.MAX_VALUE },
                {
                    it.itemName
                        ?.lowercase(Locale.ROOT)
                        ?: ""
                },
                { it.field ?: "" },
                { it.message }
            )

        fun severityRank(
            severity: CatalogIssueSeverity
        ): Int =
            when (severity) {
                CatalogIssueSeverity.ERROR -> 0
                CatalogIssueSeverity.WARNING -> 1
                CatalogIssueSeverity.INFO -> 2
            }
    }
}