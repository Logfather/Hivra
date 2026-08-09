package de.shopme.testing.system.tools.knowledge.catalog.evaluation

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CanonicalFoodKeyNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.validation.NormalizedCatalogValidator
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale

class NormalizedCatalogEvaluator(
    private val keyNormalizer: CanonicalFoodKeyNormalizer,
    private val catalogValidator: NormalizedCatalogValidator
) {

    fun evaluate(
        normalizedCatalogFile: File,
        items: List<CatalogFoodItem>,
        canonicalizationPlanFile: File,
        categoryRegistry: CanonicalFoodCategoryRegistry
    ): NormalizedCatalogEvaluationResult {
        validateInputs(
            normalizedCatalogFile = normalizedCatalogFile,
            canonicalizationPlanFile = canonicalizationPlanFile
        )

        val validationResult = catalogValidator.validate(
            items = items,
            categoryRegistry = categoryRegistry
        )

        val canonicalizationPlan =
            readCanonicalizationPlan(canonicalizationPlanFile)

        val duplicateNormalizedKeyGroups =
            duplicateNormalizedKeyGroups(items)

        val duplicateItemNameGroups =
            duplicateItemNameGroups(items)

        val categoryCounts = items
            .groupingBy { item ->
                item.category
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: MISSING_CATEGORY_KEY
            }
            .eachCount()
            .toSortedMap()

        val missingNormalizedKeyCount = items.count {
            it.normalized.isNullOrBlank()
        }

        val invalidNormalizedKeyCount = items.count { item ->
            val key = item.normalized

            !key.isNullOrBlank() &&
                    !keyNormalizer.isCanonicalKey(key)
        }

        val missingCategoryCount = items.count {
            it.category.isNullOrBlank()
        }

        val unknownCategoryCount = items.count { item ->
            val category = item.category

            !category.isNullOrBlank() &&
                    !categoryRegistry.contains(category)
        }

        val missingPluralCount = items.count {
            it.plural.isNullOrBlank()
        }

        val lowercasePluralCount = items.count { item ->
            val plural = item.plural
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: return@count false

            plural.firstOrNull(Char::isLetter)
                ?.isLowerCase() == true
        }

        val manualReviewActionCount =
            canonicalizationPlan.requiredInt(
                "manualReviewActionCount"
            )

        val explicitReviewActionCount =
            canonicalizationPlan.requiredInt(
                "explicitReviewActionCount"
            )

        val mergeActionCount =
            canonicalizationPlan.requiredInt(
                "mergeActionCount"
            )

        val removalActionCount =
            canonicalizationPlan.requiredInt(
                "removalActionCount"
            )

        val splitActionCount =
            canonicalizationPlan.requiredInt(
                "splitActionCount"
            )

        val actionCounts =
            canonicalizationPlan.requiredIntMap(
                "actionCounts"
            )

        val blockingReasons = buildBlockingReasons(
            validationIssueCount =
                validationResult.issueCount,
            missingNormalizedKeyCount =
                missingNormalizedKeyCount,
            invalidNormalizedKeyCount =
                invalidNormalizedKeyCount,
            duplicateNormalizedKeyGroupCount =
                duplicateNormalizedKeyGroups.size,
            duplicateItemNameGroupCount =
                duplicateItemNameGroups.size,
            missingCategoryCount =
                missingCategoryCount,
            unknownCategoryCount =
                unknownCategoryCount
        )

        val reviewReasons = buildReviewReasons(
            manualReviewActionCount =
                manualReviewActionCount,
            explicitReviewActionCount =
                explicitReviewActionCount,
            splitActionCount =
                splitActionCount
        )

        val readinessStatus = when {
            validationResult.issueCount > 0 ->
                NormalizedCatalogReadinessStatus.BLOCKED

            manualReviewActionCount > 0 ->
                NormalizedCatalogReadinessStatus.REVIEW_REQUIRED

            else ->
                NormalizedCatalogReadinessStatus.READY
        }

        val examples = buildExamples(
            items = items,
            categoryRegistry = categoryRegistry,
            duplicateNormalizedKeyGroups =
                duplicateNormalizedKeyGroups,
            duplicateItemNameGroups =
                duplicateItemNameGroups
        )

        return NormalizedCatalogEvaluationResult(
            version =
                NormalizedCatalogEvaluationResult.CURRENT_VERSION,
            normalizedCatalogFile =
                normalizedCatalogFile.canonicalPath,
            canonicalizationPlanFile =
                canonicalizationPlanFile.canonicalPath,

            entryCount = items.size,
            categoryCount = categoryCounts.size,

            validationIssueCount =
                validationResult.issueCount,
            validationIssueCountsByType =
                validationResult.issueCountsByType
                    .toList()
                    .sortedBy { it.first.name }
                    .associate { (type, count) ->
                        type.name to count
                    },

            duplicateNormalizedKeyGroupCount =
                duplicateNormalizedKeyGroups.size,
            duplicateNormalizedKeyEntryCount =
                duplicateNormalizedKeyGroups.values
                    .sumOf(List<Int>::size),

            duplicateItemNameGroupCount =
                duplicateItemNameGroups.size,
            duplicateItemNameEntryCount =
                duplicateItemNameGroups.values
                    .sumOf(List<Int>::size),

            missingNormalizedKeyCount =
                missingNormalizedKeyCount,
            invalidNormalizedKeyCount =
                invalidNormalizedKeyCount,

            missingCategoryCount =
                missingCategoryCount,
            unknownCategoryCount =
                unknownCategoryCount,

            missingPluralCount =
                missingPluralCount,
            lowercasePluralCount =
                lowercasePluralCount,

            emptyColloquialCount = items.count {
                it.colloquial.isEmpty()
            },
            emptyPhoneticTokensCount = items.count {
                it.phoneticTokens.isEmpty()
            },
            emptyAutocompleteTokensCount = items.count {
                it.autocompleteTokens.isEmpty()
            },

            manualReviewActionCount =
                manualReviewActionCount,
            explicitReviewActionCount =
                explicitReviewActionCount,
            mergeActionCount =
                mergeActionCount,
            removalActionCount =
                removalActionCount,
            splitActionCount =
                splitActionCount,

            categoryCounts =
                categoryCounts,
            canonicalizationActionCounts =
                actionCounts,

            blockingReasons =
                blockingReasons,
            reviewReasons =
                reviewReasons,

            examples =
                examples,

            readinessStatus =
                readinessStatus
        )
    }

    private fun validateInputs(
        normalizedCatalogFile: File,
        canonicalizationPlanFile: File
    ) {
        require(normalizedCatalogFile.isFile) {
            "Normalized catalog does not exist: " +
                    normalizedCatalogFile.absolutePath
        }

        require(normalizedCatalogFile.canRead()) {
            "Normalized catalog is not readable: " +
                    normalizedCatalogFile.absolutePath
        }

        require(canonicalizationPlanFile.isFile) {
            "Canonicalization-plan report does not exist: " +
                    canonicalizationPlanFile.absolutePath
        }

        require(canonicalizationPlanFile.canRead()) {
            "Canonicalization-plan report is not readable: " +
                    canonicalizationPlanFile.absolutePath
        }
    }

    private fun readCanonicalizationPlan(
        file: File
    ): JsonObject {
        val root = JsonParser.parseString(
            file.readText(StandardCharsets.UTF_8)
        )

        require(root.isJsonObject) {
            "Canonicalization-plan report root must be a JSON object."
        }

        return root.asJsonObject
    }

    private fun duplicateNormalizedKeyGroups(
        items: List<CatalogFoodItem>
    ): Map<String, List<Int>> =
        items
            .mapIndexedNotNull { sourceIndex, item ->
                item.normalized
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?.let { normalizedKey ->
                        normalizedKey to sourceIndex
                    }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .filterValues { it.size > 1 }
            .toSortedMap()

    private fun duplicateItemNameGroups(
        items: List<CatalogFoodItem>
    ): Map<String, List<Int>> =
        items
            .mapIndexed { sourceIndex, item ->
                normalizeNameForComparison(item.itemname) to
                        sourceIndex
            }
            .filter { it.first.isNotBlank() }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .filterValues { it.size > 1 }
            .toSortedMap()

    private fun normalizeNameForComparison(
        value: String
    ): String =
        value
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .lowercase(Locale.GERMAN)

    private fun buildBlockingReasons(
        validationIssueCount: Int,
        missingNormalizedKeyCount: Int,
        invalidNormalizedKeyCount: Int,
        duplicateNormalizedKeyGroupCount: Int,
        duplicateItemNameGroupCount: Int,
        missingCategoryCount: Int,
        unknownCategoryCount: Int
    ): List<String> =
        buildList {
            if (validationIssueCount > 0) {
                add(
                    "$validationIssueCount strict normalized-catalog " +
                            "validation issues remain."
                )
            }

            if (missingNormalizedKeyCount > 0) {
                add(
                    "$missingNormalizedKeyCount entries have no " +
                            "normalized key."
                )
            }

            if (invalidNormalizedKeyCount > 0) {
                add(
                    "$invalidNormalizedKeyCount entries use a " +
                            "non-canonical normalized key."
                )
            }

            if (duplicateNormalizedKeyGroupCount > 0) {
                add(
                    "$duplicateNormalizedKeyGroupCount duplicate " +
                            "normalized-key groups remain."
                )
            }

            if (duplicateItemNameGroupCount > 0) {
                add(
                    "$duplicateItemNameGroupCount duplicate canonical-name " +
                            "groups remain."
                )
            }

            if (missingCategoryCount > 0) {
                add(
                    "$missingCategoryCount entries have no category."
                )
            }

            if (unknownCategoryCount > 0) {
                add(
                    "$unknownCategoryCount entries reference an unknown " +
                            "category."
                )
            }
        }
            .distinct()
            .sorted()

    private fun buildReviewReasons(
        manualReviewActionCount: Int,
        explicitReviewActionCount: Int,
        splitActionCount: Int
    ): List<String> =
        buildList {
            if (manualReviewActionCount > 0) {
                add(
                    "$manualReviewActionCount canonicalization actions " +
                            "still require manual review."
                )
            }

            if (explicitReviewActionCount > 0) {
                add(
                    "$explicitReviewActionCount entries use the explicit " +
                            "REVIEW action."
                )
            }

            if (splitActionCount > 0) {
                add(
                    "$splitActionCount entries require a catalog split."
                )
            }
        }
            .distinct()
            .sorted()

    private fun buildExamples(
        items: List<CatalogFoodItem>,
        categoryRegistry: CanonicalFoodCategoryRegistry,
        duplicateNormalizedKeyGroups: Map<String, List<Int>>,
        duplicateItemNameGroups: Map<String, List<Int>>
    ): List<NormalizedCatalogEvaluationExample> {
        val examples =
            mutableListOf<NormalizedCatalogEvaluationExample>()

        items.forEachIndexed { sourceIndex, item ->
            val normalizedKey = item.normalized

            when {
                normalizedKey.isNullOrBlank() -> {
                    examples += example(
                        sourceIndex = sourceIndex,
                        item = item,
                        reason = "MISSING_NORMALIZED_KEY"
                    )
                }

                !keyNormalizer.isCanonicalKey(normalizedKey) -> {
                    examples += example(
                        sourceIndex = sourceIndex,
                        item = item,
                        reason = "INVALID_NORMALIZED_KEY"
                    )
                }
            }

            val category = item.category

            when {
                category.isNullOrBlank() -> {
                    examples += example(
                        sourceIndex = sourceIndex,
                        item = item,
                        reason = "MISSING_CATEGORY"
                    )
                }

                !categoryRegistry.contains(category) -> {
                    examples += example(
                        sourceIndex = sourceIndex,
                        item = item,
                        reason = "UNKNOWN_CATEGORY"
                    )
                }
            }

            val plural = item.plural
                ?.trim()
                ?.takeIf(String::isNotBlank)

            if (
                plural != null &&
                plural.firstOrNull(Char::isLetter)
                    ?.isLowerCase() == true
            ) {
                examples += example(
                    sourceIndex = sourceIndex,
                    item = item,
                    reason = "LOWERCASE_PLURAL"
                )
            }
        }

        duplicateNormalizedKeyGroups
            .values
            .flatten()
            .distinct()
            .sorted()
            .forEach { sourceIndex ->
                examples += example(
                    sourceIndex = sourceIndex,
                    item = items[sourceIndex],
                    reason = "DUPLICATE_NORMALIZED_KEY"
                )
            }

        duplicateItemNameGroups
            .values
            .flatten()
            .distinct()
            .sorted()
            .forEach { sourceIndex ->
                examples += example(
                    sourceIndex = sourceIndex,
                    item = items[sourceIndex],
                    reason = "DUPLICATE_ITEM_NAME"
                )
            }

        return examples
            .distinct()
            .sortedWith(
                compareBy<NormalizedCatalogEvaluationExample>(
                    { it.reason },
                    { it.sourceIndex },
                    {
                        it.itemName.lowercase(
                            Locale.GERMAN
                        )
                    }
                )
            )
            .take(MAXIMUM_EXAMPLES)
    }

    private fun example(
        sourceIndex: Int,
        item: CatalogFoodItem,
        reason: String
    ): NormalizedCatalogEvaluationExample =
        NormalizedCatalogEvaluationExample(
            sourceIndex = sourceIndex,
            itemName = item.itemname,
            normalizedKey = item.normalized,
            category = item.category,
            plural = item.plural,
            reason = reason
        )

    private fun JsonObject.requiredInt(
        fieldName: String
    ): Int {
        val element = get(fieldName)

        require(
            element != null &&
                    element.isJsonPrimitive &&
                    element.asJsonPrimitive.isNumber
        ) {
            "Canonicalization-plan field '$fieldName' must be numeric."
        }

        return element.asInt
    }

    private fun JsonObject.requiredIntMap(
        fieldName: String
    ): Map<String, Int> {
        val element = get(fieldName)

        require(element != null && element.isJsonObject) {
            "Canonicalization-plan field '$fieldName' must be an object."
        }

        return element.asJsonObject
            .entrySet()
            .associate { (key, value) ->
                require(
                    value.isJsonPrimitive &&
                            value.asJsonPrimitive.isNumber
                ) {
                    "Canonicalization-plan map '$fieldName' contains " +
                            "a non-numeric value for '$key'."
                }

                key to value.asInt
            }
            .toSortedMap()
    }

    private companion object {

        const val MISSING_CATEGORY_KEY =
            "<missing>"

        const val MAXIMUM_EXAMPLES =
            250

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")
    }
}