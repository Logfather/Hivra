package de.shopme.testing.system.tools.knowledge.catalog.normalization

import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import java.util.Locale

class CatalogFoodItemNormalizer(
    private val foodNameNormalizer: CanonicalFoodNameNormalizer,
    private val keyNormalizer: CanonicalFoodKeyNormalizer,
    private val tokenNormalizer: CatalogTokenNormalizer,
    private val pluralNormalizer: GermanFoodPluralNormalizer
) {

    fun normalize(
        item: IndexedCatalogFoodItem
    ): CatalogNormalizationResult {
        require(item.sourceIndex >= 0) {
            "Catalog item sourceIndex must not be negative."
        }

        require(item.item.itemname.isNotBlank()) {
            "Catalog item itemname must not be blank at sourceIndex " +
                    "${item.sourceIndex}."
        }

        validateOriginalLists(item)

        val originalItemName = item.item.itemname
        val originalNormalized = item.item.normalized
        val originalPlural = item.item.plural
        val originalColloquial = item.item.colloquial.toList()
        val originalPhoneticTokens = item.item.phoneticTokens.toList()
        val originalAutocompleteTokens =
            item.item.autocompleteTokens.toList()

        val computedCanonicalName =
            foodNameNormalizer.normalize(originalItemName)

        require(computedCanonicalName.isNotBlank()) {
            "Canonical food name must not be blank at sourceIndex " +
                    "${item.sourceIndex}."
        }

        val computedNormalizedKey =
            keyNormalizer.normalize(computedCanonicalName)

        require(
            keyNormalizer.isCanonicalKey(computedNormalizedKey)
        ) {
            "Generated normalized key '$computedNormalizedKey' is invalid " +
                    "at sourceIndex ${item.sourceIndex}."
        }

        val pluralEvaluation = pluralNormalizer.evaluate(
            singular = computedCanonicalName,
            existingPlural = originalPlural
        )

        val computedPlural = pluralEvaluation.normalizedPlural
            ?.let(::normalizeOptionalText)

        val normalizedColloquial =
            tokenNormalizer.normalizeAliases(
                values = originalColloquial,
                canonicalName = computedCanonicalName
            )
                .let(::normalizeAliasList)

        val normalizedPhoneticTokens =
            tokenNormalizer.normalizePhoneticTokens(
                originalPhoneticTokens
            )
                .let(::normalizeTechnicalTokenList)

        val normalizedAutocompleteTokens =
            tokenNormalizer.normalizeAutocompleteTokens(
                values = originalAutocompleteTokens,
                canonicalName = computedCanonicalName
            )
                .let(::normalizeTechnicalTokenList)

        val changes = buildChanges(
            originalItemName = originalItemName,
            originalNormalized = originalNormalized,
            originalPlural = originalPlural,
            originalColloquial = originalColloquial,
            originalPhoneticTokens = originalPhoneticTokens,
            originalAutocompleteTokens = originalAutocompleteTokens,
            computedCanonicalName = computedCanonicalName,
            computedNormalizedKey = computedNormalizedKey,
            computedPlural = computedPlural,
            normalizedColloquial = normalizedColloquial,
            normalizedPhoneticTokens = normalizedPhoneticTokens,
            normalizedAutocompleteTokens =
                normalizedAutocompleteTokens,
            pluralEvaluation = pluralEvaluation
        )

        return CatalogNormalizationResult(
            sourceIndex = item.sourceIndex,

            originalItemName = originalItemName,
            originalNormalized = originalNormalized,
            originalPlural = originalPlural,
            originalColloquial = originalColloquial,
            originalPhoneticTokens = originalPhoneticTokens,
            originalAutocompleteTokens =
                originalAutocompleteTokens,

            computedCanonicalName = computedCanonicalName,
            computedNormalizedKey = computedNormalizedKey,
            computedPlural = computedPlural,
            normalizedColloquial = normalizedColloquial,
            normalizedPhoneticTokens =
                normalizedPhoneticTokens,
            normalizedAutocompleteTokens =
                normalizedAutocompleteTokens,

            changes = changes
        )
    }

    fun normalizeAll(
        items: List<IndexedCatalogFoodItem>
    ): List<CatalogNormalizationResult> {
        val duplicateSourceIndices = items
            .groupBy { it.sourceIndex }
            .filterValues { it.size > 1 }
            .keys
            .sorted()

        require(duplicateSourceIndices.isEmpty()) {
            "Catalog items contain duplicate sourceIndex values: " +
                    duplicateSourceIndices.joinToString(", ")
        }

        return items
            .sortedBy { it.sourceIndex }
            .map(::normalize)
    }

    private fun validateOriginalLists(
        item: IndexedCatalogFoodItem
    ) {
        require(
            item.item.colloquial.none(String::isBlank)
        ) {
            "Catalog item colloquial aliases contain blank values at " +
                    "sourceIndex ${item.sourceIndex}."
        }

        require(
            item.item.phoneticTokens.none(String::isBlank)
        ) {
            "Catalog item phonetic tokens contain blank values at " +
                    "sourceIndex ${item.sourceIndex}."
        }

        require(
            item.item.autocompleteTokens.none(String::isBlank)
        ) {
            "Catalog item autocomplete tokens contain blank values at " +
                    "sourceIndex ${item.sourceIndex}."
        }
    }

    private fun buildChanges(
        originalItemName: String,
        originalNormalized: String?,
        originalPlural: String?,
        originalColloquial: List<String>,
        originalPhoneticTokens: List<String>,
        originalAutocompleteTokens: List<String>,
        computedCanonicalName: String,
        computedNormalizedKey: String,
        computedPlural: String?,
        normalizedColloquial: List<String>,
        normalizedPhoneticTokens: List<String>,
        normalizedAutocompleteTokens: List<String>,
        pluralEvaluation: GermanPluralEvaluation
    ): List<CatalogNormalizationChange> {
        val changes = mutableListOf<CatalogNormalizationChange>()

        addItemNameChanges(
            originalValue = originalItemName,
            computedValue = computedCanonicalName,
            changes = changes
        )

        addNormalizedKeyChanges(
            originalValue = originalNormalized,
            computedValue = computedNormalizedKey,
            changes = changes
        )

        addPluralChanges(
            originalValue = originalPlural,
            computedValue = computedPlural,
            pluralEvaluation = pluralEvaluation,
            changes = changes
        )

        addListChanges(
            field = FIELD_COLLOQUIAL,
            originalValues = originalColloquial,
            normalizedValues = normalizedColloquial,
            changes = changes
        )

        addListChanges(
            field = FIELD_PHONETIC_TOKENS,
            originalValues = originalPhoneticTokens,
            normalizedValues = normalizedPhoneticTokens,
            changes = changes
        )

        addListChanges(
            field = FIELD_AUTOCOMPLETE_TOKENS,
            originalValues = originalAutocompleteTokens,
            normalizedValues = normalizedAutocompleteTokens,
            changes = changes
        )

        return changes
            .distinct()
            .sortedWith(CHANGE_COMPARATOR)
    }

    private fun addItemNameChanges(
        originalValue: String,
        computedValue: String,
        changes: MutableList<CatalogNormalizationChange>
    ) {
        if (originalValue == computedValue) {
            return
        }

        val originalTrimmed = originalValue.trim()
        val whitespaceCollapsed = originalTrimmed
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")

        if (originalValue != originalTrimmed) {
            changes += change(
                type =
                    CatalogNormalizationChangeType.TRIMMED_WHITESPACE,
                field = FIELD_ITEM_NAME,
                before = originalValue,
                after = originalTrimmed,
                reason =
                    "Removed leading or trailing whitespace from item name."
            )
        }

        if (originalTrimmed != whitespaceCollapsed) {
            changes += change(
                type =
                    CatalogNormalizationChangeType.COLLAPSED_WHITESPACE,
                field = FIELD_ITEM_NAME,
                before = originalTrimmed,
                after = whitespaceCollapsed,
                reason =
                    "Collapsed repeated whitespace in item name."
            )
        }

        if (
            normalizeCaseComparison(whitespaceCollapsed) ==
            normalizeCaseComparison(computedValue) &&
            whitespaceCollapsed != computedValue
        ) {
            changes += change(
                type =
                    CatalogNormalizationChangeType.NORMALIZED_CASE,
                field = FIELD_ITEM_NAME,
                before = whitespaceCollapsed,
                after = computedValue,
                reason =
                    "Normalized capitalization of the canonical food name."
            )
        }

        if (
            normalizeHyphenComparison(whitespaceCollapsed) ==
            normalizeHyphenComparison(computedValue) &&
            whitespaceCollapsed != computedValue
        ) {
            changes += change(
                type =
                    CatalogNormalizationChangeType.NORMALIZED_HYPHEN,
                field = FIELD_ITEM_NAME,
                before = whitespaceCollapsed,
                after = computedValue,
                reason =
                    "Normalized hyphen characters or hyphen spacing."
            )
        }

        if (
            normalizeApostropheComparison(whitespaceCollapsed) ==
            normalizeApostropheComparison(computedValue) &&
            whitespaceCollapsed != computedValue
        ) {
            changes += change(
                type =
                    CatalogNormalizationChangeType.NORMALIZED_APOSTROPHE,
                field = FIELD_ITEM_NAME,
                before = whitespaceCollapsed,
                after = computedValue,
                reason =
                    "Normalized apostrophe characters or apostrophe spacing."
            )
        }

        if (
            normalizeUmlautComparison(whitespaceCollapsed) ==
            normalizeUmlautComparison(computedValue) &&
            whitespaceCollapsed != computedValue
        ) {
            changes += change(
                type =
                    CatalogNormalizationChangeType.NORMALIZED_UMLAUT,
                field = FIELD_ITEM_NAME,
                before = whitespaceCollapsed,
                after = computedValue,
                reason =
                    "Normalized German umlaut spelling."
            )
        }

        if (
            changes.none {
                it.field == FIELD_ITEM_NAME &&
                        it.after == computedValue
            }
        ) {
            changes += change(
                type =
                    CatalogNormalizationChangeType.NORMALIZED_CASE,
                field = FIELD_ITEM_NAME,
                before = originalValue,
                after = computedValue,
                reason =
                    "Normalized item name to its deterministic canonical form."
            )
        }
    }

    private fun addNormalizedKeyChanges(
        originalValue: String?,
        computedValue: String,
        changes: MutableList<CatalogNormalizationChange>
    ) {
        val normalizedOriginal = originalValue
            ?.trim()
            ?.takeIf(String::isNotBlank)

        if (normalizedOriginal == computedValue) {
            return
        }

        changes += change(
            type =
                CatalogNormalizationChangeType
                    .RECOMPUTED_NORMALIZED_KEY,
            field = FIELD_NORMALIZED,
            before = originalValue,
            after = computedValue,
            reason =
                if (normalizedOriginal == null) {
                    "Generated missing canonical normalized key."
                } else {
                    "Recomputed normalized key from canonical item name."
                }
        )
    }

    private fun addPluralChanges(
        originalValue: String?,
        computedValue: String?,
        pluralEvaluation: GermanPluralEvaluation,
        changes: MutableList<CatalogNormalizationChange>
    ) {
        /*
         * Der Rohwert muss mit dem Ergebnis verglichen werden. Würden wir den
         * Originalwert zuerst trimmen, gingen reine Whitespace-Reparaturen im
         * Änderungsprotokoll verloren.
         */
        if (originalValue == computedValue) {
            return
        }

        val normalizedOriginal = originalValue
            ?.let(::normalizeOptionalText)

        val reason = pluralEvaluation.reason
            .takeIf(String::isNotBlank)
            ?: when {
                normalizedOriginal == null &&
                        computedValue != null ->
                    "Generated deterministic German plural form."

                normalizedOriginal != null &&
                        computedValue == null ->
                    "Removed unusable or invalid plural form."

                normalizedOriginal == computedValue &&
                        originalValue != computedValue ->
                    "Normalized whitespace in German plural form."

                else ->
                    "Normalized German plural form."
            }

        changes += change(
            type = CatalogNormalizationChangeType.REPAIRED_PLURAL,
            field = FIELD_PLURAL,
            before = originalValue,
            after = computedValue,
            reason = reason
        )
    }

    private fun addListChanges(
        field: String,
        originalValues: List<String>,
        normalizedValues: List<String>,
        changes: MutableList<CatalogNormalizationChange>
    ) {
        val trimmedOriginalValues = originalValues
            .map(String::trim)

        val normalizedOriginalForComparison =
            normalizeListForComparison(originalValues)

        val normalizedTargetForComparison =
            normalizeListForComparison(normalizedValues)

        originalValues
            .filter(String::isBlank)
            .forEach { blankValue ->
                changes += change(
                    type =
                        CatalogNormalizationChangeType
                            .REMOVED_EMPTY_ALIAS,
                    field = field,
                    before = blankValue,
                    after = null,
                    reason =
                        "Removed blank value from '$field'."
                )
            }

        val duplicateValues = trimmedOriginalValues
            .filter(String::isNotBlank)
            .groupBy {
                it.lowercase(Locale.ROOT)
            }
            .filterValues { it.size > 1 }
            .values
            .flatten()
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

        duplicateValues.forEach { duplicateValue ->
            changes += change(
                type =
                    CatalogNormalizationChangeType
                        .REMOVED_DUPLICATE_TOKEN,
                field = field,
                before = duplicateValue,
                after = null,
                reason =
                    "Removed duplicate value from '$field'."
            )
        }

        if (
            normalizedOriginalForComparison ==
            normalizedTargetForComparison
        ) {
            if (
                trimmedOriginalValues
                    .filter(String::isNotBlank) !=
                normalizedValues
            ) {
                changes += change(
                    type =
                        CatalogNormalizationChangeType.SORTED_TOKEN_LIST,
                    field = field,
                    before = serializeValues(originalValues),
                    after = serializeValues(normalizedValues),
                    reason =
                        "Sorted '$field' deterministically."
                )
            }

            return
        }

        val removedValues =
            normalizedOriginalForComparison -
                    normalizedTargetForComparison

        val addedValues =
            normalizedTargetForComparison -
                    normalizedOriginalForComparison

        removedValues
            .sorted()
            .forEach { removedValue ->
                changes += change(
                    type =
                        CatalogNormalizationChangeType
                            .REMOVED_DUPLICATE_TOKEN,
                    field = field,
                    before = removedValue,
                    after = null,
                    reason =
                        "Removed non-canonical value from '$field'."
                )
            }

        if (addedValues.isNotEmpty()) {
            changes += change(
                type =
                    CatalogNormalizationChangeType.SORTED_TOKEN_LIST,
                field = field,
                before = serializeValues(originalValues),
                after = serializeValues(normalizedValues),
                reason =
                    "Normalized and deterministically ordered values in " +
                            "'$field'."
            )
        } else if (
            serializeValues(originalValues) !=
            serializeValues(normalizedValues)
        ) {
            changes += change(
                type =
                    CatalogNormalizationChangeType.SORTED_TOKEN_LIST,
                field = field,
                before = serializeValues(originalValues),
                after = serializeValues(normalizedValues),
                reason =
                    "Normalized and deterministically ordered values in " +
                            "'$field'."
            )
        }
    }

    private fun normalizeAliasList(
        values: List<String>
    ): List<String> =
        values
            .asSequence()
            .map(::normalizeRequiredText)
            .filter(String::isNotBlank)
            .distinctBy {
                it.lowercase(Locale.ROOT)
            }
            .sortedWith(
                compareBy<String>(
                    { it.lowercase(Locale.ROOT) },
                    { it }
                )
            )
            .toList()

    private fun normalizeTechnicalTokenList(
        values: List<String>
    ): List<String> =
        values
            .asSequence()
            .map(::normalizeRequiredText)
            .map {
                it.lowercase(Locale.ROOT)
            }
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
            .toList()

    private fun normalizeListForComparison(
        values: List<String>
    ): Set<String> =
        values
            .asSequence()
            .map(::normalizeRequiredText)
            .filter(String::isNotBlank)
            .map {
                it.lowercase(Locale.ROOT)
            }
            .toSortedSet()

    private fun serializeValues(
        values: List<String>
    ): String? =
        values
            .map(::normalizeRequiredText)
            .filter(String::isNotBlank)
            .takeIf(List<String>::isNotEmpty)
            ?.joinToString(LIST_VALUE_SEPARATOR)

    private fun change(
        type: CatalogNormalizationChangeType,
        field: String,
        before: String?,
        after: String?,
        reason: String
    ): CatalogNormalizationChange {
        val normalizedBefore = before
            ?.takeIf { it.isNotEmpty() }

        val normalizedAfter = after
            ?.takeIf { it.isNotEmpty() }

        require(
            normalizedBefore != normalizedAfter
        ) {
            "Normalization change before and after must differ for " +
                    "field '$field'."
        }

        return CatalogNormalizationChange(
            type = type,
            field = field,
            before = normalizedBefore,
            after = normalizedAfter,
            reason = normalizeRequiredText(reason)
        )
    }

    private fun normalizeRequiredText(
        value: String
    ): String =
        value
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")

    private fun normalizeOptionalText(
        value: String
    ): String? =
        normalizeRequiredText(value)
            .takeIf(String::isNotBlank)

    private fun normalizeCaseComparison(
        value: String
    ): String =
        normalizeRequiredText(value)
            .lowercase(Locale.GERMAN)

    private fun normalizeHyphenComparison(
        value: String
    ): String =
        normalizeRequiredText(value)
            .replace(HYPHEN_VARIANT_REGEX, "-")
            .replace(SPACE_AROUND_HYPHEN_REGEX, "-")
            .lowercase(Locale.GERMAN)

    private fun normalizeApostropheComparison(
        value: String
    ): String =
        normalizeRequiredText(value)
            .replace(APOSTROPHE_VARIANT_REGEX, "'")
            .replace(SPACE_AROUND_APOSTROPHE_REGEX, "'")
            .lowercase(Locale.GERMAN)

    private fun normalizeUmlautComparison(
        value: String
    ): String =
        normalizeRequiredText(value)
            .lowercase(Locale.GERMAN)
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")

    private companion object {

        const val FIELD_ITEM_NAME = "itemname"
        const val FIELD_NORMALIZED = "normalized"
        const val FIELD_PLURAL = "plural"
        const val FIELD_COLLOQUIAL = "colloquial"
        const val FIELD_PHONETIC_TOKENS = "phonetic_tokens"
        const val FIELD_AUTOCOMPLETE_TOKENS =
            "autocomplete_tokens"

        const val LIST_VALUE_SEPARATOR = " | "

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        val HYPHEN_VARIANT_REGEX =
            Regex("[-‐-‒–—−﹘﹣－]+")

        val SPACE_AROUND_HYPHEN_REGEX =
            Regex("\\s*-\\s*")

        val APOSTROPHE_VARIANT_REGEX =
            Regex("['’‘`´ʼ＇]")

        val SPACE_AROUND_APOSTROPHE_REGEX =
            Regex("\\s*'\\s*")

        val CHANGE_COMPARATOR =
            compareBy<CatalogNormalizationChange>(
                { it.type.name },
                { it.field },
                { it.before ?: "" },
                { it.after ?: "" },
                { it.reason }
            )
    }
}