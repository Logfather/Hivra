package de.shopme.testing.system.tools.knowledge.catalog.category

import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogIssueSeverity
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

class CatalogCategoryValidator {

    fun validate(
        entries: List<IndexedCatalogFoodItem>,
        registry: CanonicalFoodCategoryRegistry
    ): CatalogCategoryValidationResult {
        require(entries.map { it.sourceIndex }.distinct().size == entries.size) {
            "Catalog entries contain duplicate sourceIndex values."
        }

        val definitions = registry.definitions()

        require(definitions.isNotEmpty()) {
            "Canonical food category registry must not be empty."
        }

        val definitionsByKey = definitions.associateBy { it.key }

        val definitionsByNormalizedDisplayName = definitions
            .groupBy { normalizeLookupValue(it.displayName) }

        val categoryAliasIndex = buildCategoryAliasIndex(definitions)

        val issues = mutableListOf<CatalogCategoryIssue>()
        val validCategoryCounts = sortedMapOf<String, Int>()
        val unknownCategoryCounts = sortedMapOf<String, Int>()
        val suggestedCategoryMappings = sortedMapOf<String, String>()

        var categorizedEntryCount = 0
        var missingCategoryCount = 0
        var unknownCategoryCount = 0
        var validCategoryCount = 0
        var rootCategoryAssignmentCount = 0

        entries
            .sortedBy { it.sourceIndex }
            .forEach { entry ->
                val originalCategory = entry.item.category

                if (originalCategory == null) {
                    missingCategoryCount++

                    issues += CatalogCategoryIssue(
                        type = CatalogCategoryIssueType.MISSING_CATEGORY,
                        severity = CatalogIssueSeverity.ERROR,
                        sourceIndex = entry.sourceIndex,
                        itemName = entry.item.itemname,
                        originalCategory = null,
                        normalizedCategory = null,
                        suggestedCategoryKey = null,
                        message = "Catalog item '${entry.item.itemname}' has no category."
                    )

                    return@forEach
                }

                if (originalCategory.isBlank()) {
                    missingCategoryCount++

                    issues += CatalogCategoryIssue(
                        type = CatalogCategoryIssueType.EMPTY_CATEGORY,
                        severity = CatalogIssueSeverity.ERROR,
                        sourceIndex = entry.sourceIndex,
                        itemName = entry.item.itemname,
                        originalCategory = originalCategory,
                        normalizedCategory = null,
                        suggestedCategoryKey = null,
                        message = "Catalog item '${entry.item.itemname}' has an empty category."
                    )

                    return@forEach
                }

                categorizedEntryCount++

                val normalizedCategoryKey = normalizeCategoryKey(originalCategory)
                val directDefinition = definitionsByKey[originalCategory]
                val normalizedDefinition = definitionsByKey[normalizedCategoryKey]

                when {
                    directDefinition != null -> {
                        validCategoryCount++
                        incrementCount(validCategoryCounts, directDefinition.key)

                        addRootCategoryIssueIfRequired(
                            entry = entry,
                            definition = directDefinition,
                            registry = registry,
                            issues = issues
                        )

                        if (directDefinition.parentKey == null) {
                            rootCategoryAssignmentCount++
                        }

                        addPossibleMisclassificationIssue(
                            entry = entry,
                            assignedDefinition = directDefinition,
                            registry = registry,
                            issues = issues
                        )
                    }

                    normalizedDefinition != null -> {
                        validCategoryCount++
                        incrementCount(validCategoryCounts, normalizedDefinition.key)

                        suggestedCategoryMappings.putIfAbsent(
                            originalCategory,
                            normalizedDefinition.key
                        )

                        addFormattingIssues(
                            entry = entry,
                            originalCategory = originalCategory,
                            normalizedCategory = normalizedCategoryKey,
                            suggestedCategoryKey = normalizedDefinition.key,
                            issues = issues
                        )

                        if (normalizedDefinition.parentKey == null) {
                            rootCategoryAssignmentCount++
                        }

                        addRootCategoryIssueIfRequired(
                            entry = entry,
                            definition = normalizedDefinition,
                            registry = registry,
                            issues = issues
                        )

                        addPossibleMisclassificationIssue(
                            entry = entry,
                            assignedDefinition = normalizedDefinition,
                            registry = registry,
                            issues = issues
                        )
                    }

                    else -> {
                        val resolution = resolveUnknownCategory(
                            originalCategory = originalCategory,
                            definitionsByNormalizedDisplayName =
                                definitionsByNormalizedDisplayName,
                            categoryAliasIndex = categoryAliasIndex
                        )

                        if (resolution.definition != null) {
                            validCategoryCount++
                            incrementCount(
                                validCategoryCounts,
                                resolution.definition.key
                            )

                            suggestedCategoryMappings.putIfAbsent(
                                originalCategory,
                                resolution.definition.key
                            )

                            issues += CatalogCategoryIssue(
                                type = resolution.issueType,
                                severity = resolution.severity,
                                sourceIndex = entry.sourceIndex,
                                itemName = entry.item.itemname,
                                originalCategory = originalCategory,
                                normalizedCategory = normalizedCategoryKey,
                                suggestedCategoryKey = resolution.definition.key,
                                message = resolution.message
                            )

                            if (resolution.definition.parentKey == null) {
                                rootCategoryAssignmentCount++
                            }

                            addRootCategoryIssueIfRequired(
                                entry = entry,
                                definition = resolution.definition,
                                registry = registry,
                                issues = issues
                            )

                            addPossibleMisclassificationIssue(
                                entry = entry,
                                assignedDefinition = resolution.definition,
                                registry = registry,
                                issues = issues
                            )
                        } else {
                            unknownCategoryCount++
                            incrementCount(
                                unknownCategoryCounts,
                                originalCategory.trim()
                            )

                            issues += CatalogCategoryIssue(
                                type = if (resolution.ambiguous) {
                                    CatalogCategoryIssueType.AMBIGUOUS_CATEGORY_REFERENCE
                                } else {
                                    CatalogCategoryIssueType.UNKNOWN_CATEGORY
                                },
                                severity = CatalogIssueSeverity.ERROR,
                                sourceIndex = entry.sourceIndex,
                                itemName = entry.item.itemname,
                                originalCategory = originalCategory,
                                normalizedCategory = normalizedCategoryKey,
                                suggestedCategoryKey = null,
                                message = resolution.message
                            )

                            addFormattingIssues(
                                entry = entry,
                                originalCategory = originalCategory,
                                normalizedCategory = normalizedCategoryKey,
                                suggestedCategoryKey = null,
                                issues = issues
                            )
                        }
                    }
                }
            }

        val assignedCategoryKeys = validCategoryCounts.keys

        val unusedCategoryKeys = definitions
            .asSequence()
            .map { it.key }
            .filterNot { it in assignedCategoryKeys }
            .sorted()
            .toList()

        addUnusedCategoryIssues(
            unusedCategoryKeys = unusedCategoryKeys,
            registry = registry,
            issues = issues
        )

        addDistributionIssues(
            categoryCounts = validCategoryCounts,
            registry = registry,
            issues = issues
        )

        val sortedIssues = issues
            .distinct()
            .sortedWith(
                compareBy<CatalogCategoryIssue>(
                    { severityRank(it.severity) },
                    { it.type.name },
                    { it.sourceIndex ?: Int.MAX_VALUE },
                    { it.itemName?.lowercase(Locale.ROOT) ?: "" },
                    { it.originalCategory?.lowercase(Locale.ROOT) ?: "" },
                    { it.suggestedCategoryKey ?: "" },
                    { it.message }
                )
            )

        val valid = missingCategoryCount == 0 &&
                unknownCategoryCount == 0 &&
                sortedIssues.none { it.severity == CatalogIssueSeverity.ERROR }

        return CatalogCategoryValidationResult(
            inputEntryCount = entries.size,
            categorizedEntryCount = categorizedEntryCount,
            missingCategoryCount = missingCategoryCount,
            unknownCategoryCount = unknownCategoryCount,
            validCategoryCount = validCategoryCount,
            uniqueAssignedCategoryCount = validCategoryCounts.size,
            rootCategoryAssignmentCount = rootCategoryAssignmentCount,
            categoryCounts = validCategoryCounts.toMap(),
            unknownCategoryCounts = unknownCategoryCounts.toMap(),
            suggestedCategoryMappings = suggestedCategoryMappings.toMap(),
            unusedCategoryKeys = unusedCategoryKeys,
            issues = sortedIssues,
            valid = valid
        )
    }

    private fun resolveUnknownCategory(
        originalCategory: String,
        definitionsByNormalizedDisplayName:
        Map<String, List<CatalogCategoryDefinition>>,
        categoryAliasIndex: Map<String, List<CatalogCategoryDefinition>>
    ): CategoryResolution {
        val normalizedLookupValue = normalizeLookupValue(originalCategory)

        val matchingDisplayNameDefinitions =
            definitionsByNormalizedDisplayName[normalizedLookupValue]
                .orEmpty()
                .distinctBy { it.key }
                .sortedBy { it.key }

        if (matchingDisplayNameDefinitions.size == 1) {
            val definition = matchingDisplayNameDefinitions.single()

            return CategoryResolution(
                definition = definition,
                issueType =
                    CatalogCategoryIssueType.CATEGORY_DISPLAY_NAME_USED_AS_KEY,
                severity = CatalogIssueSeverity.WARNING,
                ambiguous = false,
                message =
                    "Category '$originalCategory' uses the display name " +
                            "'${definition.displayName}' instead of canonical key " +
                            "'${definition.key}'."
            )
        }

        if (matchingDisplayNameDefinitions.size > 1) {
            return CategoryResolution(
                definition = null,
                issueType =
                    CatalogCategoryIssueType.AMBIGUOUS_CATEGORY_REFERENCE,
                severity = CatalogIssueSeverity.ERROR,
                ambiguous = true,
                message =
                    "Category '$originalCategory' matches multiple category " +
                            "display names: " +
                            matchingDisplayNameDefinitions
                                .joinToString(", ") { it.key } +
                            "."
            )
        }

        val aliasMatches = categoryAliasIndex[normalizedLookupValue]
            .orEmpty()
            .distinctBy { it.key }
            .sortedBy { it.key }

        if (aliasMatches.size == 1) {
            val definition = aliasMatches.single()

            return CategoryResolution(
                definition = definition,
                issueType = CatalogCategoryIssueType.POSSIBLE_CATEGORY_ALIAS,
                severity = CatalogIssueSeverity.WARNING,
                ambiguous = false,
                message =
                    "Category '$originalCategory' appears to be an alias of " +
                            "canonical category '${definition.key}' " +
                            "('${definition.displayName}')."
            )
        }

        if (aliasMatches.size > 1) {
            return CategoryResolution(
                definition = null,
                issueType =
                    CatalogCategoryIssueType.AMBIGUOUS_CATEGORY_REFERENCE,
                severity = CatalogIssueSeverity.ERROR,
                ambiguous = true,
                message =
                    "Category '$originalCategory' ambiguously matches " +
                            "canonical categories: " +
                            aliasMatches.joinToString(", ") { it.key } +
                            "."
            )
        }

        val similarityMatches = findSimilarityMatches(
            value = originalCategory,
            candidates = categoryAliasIndex
        )

        return when {
            similarityMatches.isEmpty() ->
                CategoryResolution(
                    definition = null,
                    issueType = CatalogCategoryIssueType.UNKNOWN_CATEGORY,
                    severity = CatalogIssueSeverity.ERROR,
                    ambiguous = false,
                    message =
                        "Category '$originalCategory' is not part of the " +
                                "canonical food category registry."
                )

            similarityMatches.size == 1 -> {
                val match = similarityMatches.single()

                CategoryResolution(
                    definition = match.definition,
                    issueType = CatalogCategoryIssueType.POSSIBLE_CATEGORY_ALIAS,
                    severity = CatalogIssueSeverity.WARNING,
                    ambiguous = false,
                    message =
                        "Category '$originalCategory' likely maps to " +
                                "'${match.definition.key}' " +
                                "('${match.definition.displayName}', " +
                                "similarity=${formatScore(match.score)})."
                )
            }

            else ->
                CategoryResolution(
                    definition = null,
                    issueType =
                        CatalogCategoryIssueType.AMBIGUOUS_CATEGORY_REFERENCE,
                    severity = CatalogIssueSeverity.ERROR,
                    ambiguous = true,
                    message =
                        "Category '$originalCategory' has multiple plausible " +
                                "canonical matches: " +
                                similarityMatches.joinToString(", ") {
                                    "${it.definition.key}=" +
                                            formatScore(it.score)
                                } +
                                "."
                )
        }
    }

    private fun addFormattingIssues(
        entry: IndexedCatalogFoodItem,
        originalCategory: String,
        normalizedCategory: String,
        suggestedCategoryKey: String?,
        issues: MutableList<CatalogCategoryIssue>
    ) {
        if (originalCategory != originalCategory.trim()) {
            issues += CatalogCategoryIssue(
                type = CatalogCategoryIssueType.CATEGORY_KEY_WHITESPACE,
                severity = CatalogIssueSeverity.WARNING,
                sourceIndex = entry.sourceIndex,
                itemName = entry.item.itemname,
                originalCategory = originalCategory,
                normalizedCategory = normalizedCategory,
                suggestedCategoryKey = suggestedCategoryKey,
                message =
                    "Category '$originalCategory' contains surrounding " +
                            "whitespace."
            )
        }

        if (MULTIPLE_WHITESPACE_REGEX.containsMatchIn(originalCategory.trim())) {
            issues += CatalogCategoryIssue(
                type = CatalogCategoryIssueType.CATEGORY_KEY_WHITESPACE,
                severity = CatalogIssueSeverity.WARNING,
                sourceIndex = entry.sourceIndex,
                itemName = entry.item.itemname,
                originalCategory = originalCategory,
                normalizedCategory = normalizedCategory,
                suggestedCategoryKey = suggestedCategoryKey,
                message =
                    "Category '$originalCategory' contains repeated " +
                            "whitespace."
            )
        }

        if ('_' in originalCategory) {
            issues += CatalogCategoryIssue(
                type = CatalogCategoryIssueType.CATEGORY_KEY_UNDERSCORE,
                severity = CatalogIssueSeverity.WARNING,
                sourceIndex = entry.sourceIndex,
                itemName = entry.item.itemname,
                originalCategory = originalCategory,
                normalizedCategory = normalizedCategory,
                suggestedCategoryKey = suggestedCategoryKey,
                message =
                    "Category '$originalCategory' uses underscores instead " +
                            "of canonical hyphens."
            )
        }

        if (
            originalCategory.any { it.isUpperCase() } &&
            originalCategory.lowercase(Locale.ROOT) == normalizedCategory
        ) {
            issues += CatalogCategoryIssue(
                type = CatalogCategoryIssueType.CATEGORY_KEY_CASE_MISMATCH,
                severity = CatalogIssueSeverity.WARNING,
                sourceIndex = entry.sourceIndex,
                itemName = entry.item.itemname,
                originalCategory = originalCategory,
                normalizedCategory = normalizedCategory,
                suggestedCategoryKey = suggestedCategoryKey,
                message =
                    "Category '$originalCategory' does not use canonical " +
                            "lowercase key formatting."
            )
        }

        if (!RAW_CATEGORY_KEY_REGEX.matches(originalCategory.trim())) {
            issues += CatalogCategoryIssue(
                type =
                    CatalogCategoryIssueType.CATEGORY_KEY_INVALID_CHARACTERS,
                severity = CatalogIssueSeverity.WARNING,
                sourceIndex = entry.sourceIndex,
                itemName = entry.item.itemname,
                originalCategory = originalCategory,
                normalizedCategory = normalizedCategory,
                suggestedCategoryKey = suggestedCategoryKey,
                message =
                    "Category '$originalCategory' contains characters that " +
                            "are not valid in a canonical category key."
            )
        }

        if (originalCategory != normalizedCategory) {
            issues += CatalogCategoryIssue(
                type =
                    CatalogCategoryIssueType.NON_NORMALIZED_CATEGORY_KEY,
                severity = CatalogIssueSeverity.WARNING,
                sourceIndex = entry.sourceIndex,
                itemName = entry.item.itemname,
                originalCategory = originalCategory,
                normalizedCategory = normalizedCategory,
                suggestedCategoryKey = suggestedCategoryKey,
                message =
                    "Category '$originalCategory' normalizes to " +
                            "'$normalizedCategory'."
            )
        }
    }

    private fun addRootCategoryIssueIfRequired(
        entry: IndexedCatalogFoodItem,
        definition: CatalogCategoryDefinition,
        registry: CanonicalFoodCategoryRegistry,
        issues: MutableList<CatalogCategoryIssue>
    ) {
        if (definition.parentKey != null) {
            return
        }

        val childDefinitions = registry.children(definition.key)

        if (childDefinitions.isEmpty()) {
            return
        }

        issues += CatalogCategoryIssue(
            type = CatalogCategoryIssueType.ROOT_CATEGORY_ASSIGNED_TO_ITEM,
            severity = CatalogIssueSeverity.INFO,
            sourceIndex = entry.sourceIndex,
            itemName = entry.item.itemname,
            originalCategory = entry.item.category,
            normalizedCategory = definition.key,
            suggestedCategoryKey = null,
            message =
                "Catalog item '${entry.item.itemname}' is assigned directly " +
                        "to root category '${definition.key}'. A more specific " +
                        "child category may be available."
        )
    }

    private fun addPossibleMisclassificationIssue(
        entry: IndexedCatalogFoodItem,
        assignedDefinition: CatalogCategoryDefinition,
        registry: CanonicalFoodCategoryRegistry,
        issues: MutableList<CatalogCategoryIssue>
    ) {
        val itemTokens = tokenize(entry.item.itemname)

        if (itemTokens.isEmpty()) {
            return
        }

        val assignedRootKey =
            registry.topLevelCategory(assignedDefinition.key).key

        val rootScores = registry.rootCategories()
            .map { rootDefinition ->
                val score = calculateRootCategoryEvidenceScore(
                    itemTokens = itemTokens,
                    rootCategoryKey = rootDefinition.key
                )

                RootCategoryEvidence(
                    rootCategoryKey = rootDefinition.key,
                    score = score
                )
            }
            .filter { it.score > 0 }
            .sortedWith(
                compareByDescending<RootCategoryEvidence> { it.score }
                    .thenBy { it.rootCategoryKey }
            )

        val strongestEvidence = rootScores.firstOrNull()
            ?: return

        if (strongestEvidence.rootCategoryKey == assignedRootKey) {
            return
        }

        val assignedEvidenceScore = rootScores
            .firstOrNull { it.rootCategoryKey == assignedRootKey }
            ?.score
            ?: 0

        if (
            strongestEvidence.score < MINIMUM_MISCLASSIFICATION_SCORE ||
            strongestEvidence.score - assignedEvidenceScore <
            MINIMUM_MISCLASSIFICATION_MARGIN
        ) {
            return
        }

        issues += CatalogCategoryIssue(
            type = CatalogCategoryIssueType.POSSIBLE_MISCLASSIFICATION,
            severity = CatalogIssueSeverity.WARNING,
            sourceIndex = entry.sourceIndex,
            itemName = entry.item.itemname,
            originalCategory = entry.item.category,
            normalizedCategory = assignedDefinition.key,
            suggestedCategoryKey = strongestEvidence.rootCategoryKey,
            message =
                "Catalog item '${entry.item.itemname}' is assigned to " +
                        "'${assignedDefinition.key}', but its name contains " +
                        "stronger lexical evidence for root category " +
                        "'${strongestEvidence.rootCategoryKey}'."
        )
    }

    private fun addUnusedCategoryIssues(
        unusedCategoryKeys: List<String>,
        registry: CanonicalFoodCategoryRegistry,
        issues: MutableList<CatalogCategoryIssue>
    ) {
        unusedCategoryKeys.forEach { categoryKey ->
            val definition = registry.requireDefinition(categoryKey)

            issues += CatalogCategoryIssue(
                type = CatalogCategoryIssueType.CATEGORY_WITHOUT_CATALOG_ITEMS,
                severity = CatalogIssueSeverity.INFO,
                sourceIndex = null,
                itemName = null,
                originalCategory = null,
                normalizedCategory = categoryKey,
                suggestedCategoryKey = null,
                message =
                    "Canonical category '$categoryKey' " +
                            "('${definition.displayName}') currently contains " +
                            "no catalog items."
            )
        }
    }

    private fun addDistributionIssues(
        categoryCounts: Map<String, Int>,
        registry: CanonicalFoodCategoryRegistry,
        issues: MutableList<CatalogCategoryIssue>
    ) {
        if (categoryCounts.isEmpty()) {
            return
        }

        val positiveCounts = categoryCounts.values
            .filter { it > 0 }
            .sorted()

        if (positiveCounts.isEmpty()) {
            return
        }

        val median = calculateMedian(positiveCounts)
        val largeCategoryThreshold = max(
            MINIMUM_LARGE_CATEGORY_ABSOLUTE_COUNT,
            (median * LARGE_CATEGORY_MEDIAN_MULTIPLIER).toInt()
        )

        categoryCounts
            .toSortedMap()
            .forEach { (categoryKey, count) ->
                val definition = registry.requireDefinition(categoryKey)

                if (
                    count in 1..VERY_SMALL_CATEGORY_MAXIMUM_COUNT &&
                    definition.parentKey != null
                ) {
                    issues += CatalogCategoryIssue(
                        type =
                            CatalogCategoryIssueType.CATEGORY_WITH_VERY_FEW_ITEMS,
                        severity = CatalogIssueSeverity.INFO,
                        sourceIndex = null,
                        itemName = null,
                        originalCategory = null,
                        normalizedCategory = categoryKey,
                        suggestedCategoryKey = null,
                        message =
                            "Canonical category '$categoryKey' " +
                                    "('${definition.displayName}') contains only " +
                                    "$count catalog item(s)."
                    )
                }

                if (count >= largeCategoryThreshold) {
                    issues += CatalogCategoryIssue(
                        type =
                            CatalogCategoryIssueType
                                .CATEGORY_WITH_UNUSUALLY_MANY_ITEMS,
                        severity = CatalogIssueSeverity.WARNING,
                        sourceIndex = null,
                        itemName = null,
                        originalCategory = null,
                        normalizedCategory = categoryKey,
                        suggestedCategoryKey = null,
                        message =
                            "Canonical category '$categoryKey' " +
                                    "('${definition.displayName}') contains " +
                                    "$count catalog items, exceeding the " +
                                    "distribution threshold of " +
                                    "$largeCategoryThreshold."
                    )
                }
            }
    }

    private fun buildCategoryAliasIndex(
        definitions: List<CatalogCategoryDefinition>
    ): Map<String, List<CatalogCategoryDefinition>> {
        val aliases = linkedMapOf<String, MutableList<CatalogCategoryDefinition>>()

        fun register(
            alias: String,
            definition: CatalogCategoryDefinition
        ) {
            val normalizedAlias = normalizeLookupValue(alias)

            if (normalizedAlias.isBlank()) {
                return
            }

            aliases
                .getOrPut(normalizedAlias) { mutableListOf() }
                .add(definition)
        }

        definitions
            .sortedBy { it.key }
            .forEach { definition ->
                register(definition.key, definition)
                register(definition.displayName, definition)

                generateDerivedAliases(definition)
                    .sorted()
                    .forEach { alias ->
                        register(alias, definition)
                    }
            }

        EXPLICIT_CATEGORY_ALIASES
            .toSortedMap()
            .forEach { (alias, targetKey) ->
                val targetDefinition = definitions
                    .firstOrNull { it.key == targetKey }
                    ?: return@forEach

                register(alias, targetDefinition)
            }

        return aliases
            .toSortedMap()
            .mapValues { (_, values) ->
                values
                    .distinctBy { it.key }
                    .sortedBy { it.key }
            }
    }

    private fun generateDerivedAliases(
        definition: CatalogCategoryDefinition
    ): Set<String> {
        val aliases = linkedSetOf<String>()

        val keyWithSpaces = definition.key.replace('-', ' ')
        aliases += keyWithSpaces
        aliases += singularizeLastToken(keyWithSpaces)
        aliases += pluralizeLastTokenHeuristically(keyWithSpaces)

        val displayName = definition.displayName
        aliases += singularizeLastToken(displayName)
        aliases += pluralizeLastTokenHeuristically(displayName)

        val normalizedDisplayName = normalizeLookupValue(displayName)

        if (normalizedDisplayName.endsWith("produkte")) {
            aliases += normalizedDisplayName.removeSuffix("produkte").trim()
        }

        if (normalizedDisplayName.endsWith("erzeugnisse")) {
            aliases += normalizedDisplayName.removeSuffix("erzeugnisse").trim()
        }

        if (normalizedDisplayName.endsWith("gerichte")) {
            aliases += normalizedDisplayName.removeSuffix("gerichte").trim()
        }

        return aliases
            .filter { it.isNotBlank() }
            .toSortedSet()
    }

    private fun findSimilarityMatches(
        value: String,
        candidates: Map<String, List<CatalogCategoryDefinition>>
    ): List<CategorySimilarityMatch> {
        val normalizedValue = normalizeLookupValue(value)

        if (normalizedValue.isBlank()) {
            return emptyList()
        }

        val matchesByDefinition = linkedMapOf<String, CategorySimilarityMatch>()

        candidates.forEach { (candidateAlias, definitions) ->
            val score = categorySimilarity(
                first = normalizedValue,
                second = candidateAlias
            )

            if (score < MINIMUM_CATEGORY_SIMILARITY) {
                return@forEach
            }

            definitions.forEach { definition ->
                val existing = matchesByDefinition[definition.key]

                if (existing == null || score > existing.score) {
                    matchesByDefinition[definition.key] =
                        CategorySimilarityMatch(
                            definition = definition,
                            score = score
                        )
                }
            }
        }

        val sortedMatches = matchesByDefinition.values
            .sortedWith(
                compareByDescending<CategorySimilarityMatch> { it.score }
                    .thenBy { it.definition.key }
            )

        val bestScore = sortedMatches.firstOrNull()?.score
            ?: return emptyList()

        return sortedMatches
            .filter {
                bestScore - it.score <=
                        CATEGORY_SIMILARITY_AMBIGUITY_MARGIN
            }
            .take(MAXIMUM_SIMILARITY_MATCH_COUNT)
    }

    private fun categorySimilarity(
        first: String,
        second: String
    ): Double {
        if (first == second) {
            return 1.0
        }

        val firstTokens = tokenize(first)
        val secondTokens = tokenize(second)

        if (firstTokens.isEmpty() || secondTokens.isEmpty()) {
            return 0.0
        }

        val tokenJaccard = jaccard(firstTokens, secondTokens)
        val tokenContainment = containment(firstTokens, secondTokens)
        val editSimilarity = normalizedEditSimilarity(first, second)

        return (
                tokenJaccard * CATEGORY_JACCARD_WEIGHT +
                        tokenContainment * CATEGORY_CONTAINMENT_WEIGHT +
                        editSimilarity * CATEGORY_EDIT_WEIGHT
                ).coerceIn(0.0, 1.0)
    }

    private fun calculateRootCategoryEvidenceScore(
        itemTokens: Set<String>,
        rootCategoryKey: String
    ): Int {
        val evidenceTokens =
            ROOT_CATEGORY_EVIDENCE_TOKENS[rootCategoryKey]
                ?: return 0

        return itemTokens.sumOf { token ->
            when {
                token in evidenceTokens.strong -> STRONG_EVIDENCE_WEIGHT
                token in evidenceTokens.medium -> MEDIUM_EVIDENCE_WEIGHT
                token in evidenceTokens.weak -> WEAK_EVIDENCE_WEIGHT
                else -> 0
            }
        }
    }

    private fun normalizeCategoryKey(value: String): String =
        transliterateGermanCharacters(
            Normalizer.normalize(value, Normalizer.Form.NFKD)
        )
            .lowercase(Locale.ROOT)
            .replace(AMPERSAND_REGEX, " und ")
            .replace(UNDERSCORE_OR_WHITESPACE_REGEX, "-")
            .replace(NON_KEY_CHARACTER_REGEX, "-")
            .replace(MULTIPLE_HYPHEN_REGEX, "-")
            .trim('-')

    private fun normalizeLookupValue(value: String): String =
        transliterateGermanCharacters(
            Normalizer.normalize(value, Normalizer.Form.NFKD)
        )
            .lowercase(Locale.ROOT)
            .replace(AMPERSAND_REGEX, " und ")
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

    private fun transliterateGermanCharacters(value: String): String =
        value
            .replace("Ä", "Ae")
            .replace("Ö", "Oe")
            .replace("Ü", "Ue")
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")
            .replace(COMBINING_MARKS_REGEX, "")

    private fun tokenize(value: String): Set<String> =
        normalizeLookupValue(value)
            .split(' ')
            .asSequence()
            .map(String::trim)
            .filter { it.length >= MINIMUM_TOKEN_LENGTH }
            .filterNot { it in STOP_TOKENS }
            .toSortedSet()

    private fun singularizeLastToken(value: String): String {
        val tokens = normalizeLookupValue(value)
            .split(' ')
            .filter { it.isNotBlank() }
            .toMutableList()

        if (tokens.isEmpty()) {
            return ""
        }

        val lastToken = tokens.last()

        tokens[tokens.lastIndex] = when {
            lastToken.length > 5 && lastToken.endsWith("nen") ->
                lastToken.dropLast(2)

            lastToken.length > 5 && lastToken.endsWith("en") ->
                lastToken.dropLast(2)

            lastToken.length > 4 && lastToken.endsWith("ern") ->
                lastToken.dropLast(1)

            lastToken.length > 4 && lastToken.endsWith("er") ->
                lastToken

            lastToken.length > 4 && lastToken.endsWith("e") ->
                lastToken.dropLast(1)

            lastToken.length > 4 && lastToken.endsWith("n") ->
                lastToken.dropLast(1)

            lastToken.length > 4 && lastToken.endsWith("s") ->
                lastToken.dropLast(1)

            else -> lastToken
        }

        return tokens.joinToString(" ")
    }

    private fun pluralizeLastTokenHeuristically(value: String): String {
        val tokens = normalizeLookupValue(value)
            .split(' ')
            .filter { it.isNotBlank() }
            .toMutableList()

        if (tokens.isEmpty()) {
            return ""
        }

        val lastToken = tokens.last()

        tokens[tokens.lastIndex] = when {
            lastToken.endsWith("e") -> "${lastToken}n"
            lastToken.endsWith("er") -> lastToken
            lastToken.endsWith("en") -> lastToken
            lastToken.endsWith("s") -> lastToken
            else -> "${lastToken}e"
        }

        return tokens.joinToString(" ")
    }

    private fun jaccard(
        first: Set<String>,
        second: Set<String>
    ): Double {
        val unionSize = first.union(second).size

        if (unionSize == 0) {
            return 0.0
        }

        return first.intersect(second).size.toDouble() / unionSize
    }

    private fun containment(
        first: Set<String>,
        second: Set<String>
    ): Double {
        val minimumSize = minOf(first.size, second.size)

        if (minimumSize == 0) {
            return 0.0
        }

        return first.intersect(second).size.toDouble() / minimumSize
    }

    private fun normalizedEditSimilarity(
        first: String,
        second: String
    ): Double {
        val maximumLength = max(first.length, second.length)

        if (maximumLength == 0) {
            return 1.0
        }

        val distance = levenshteinDistance(first, second)

        return (
                1.0 -
                        distance.toDouble() / maximumLength.toDouble()
                ).coerceIn(0.0, 1.0)
    }

    private fun levenshteinDistance(
        first: String,
        second: String
    ): Int {
        if (first == second) {
            return 0
        }

        if (first.isEmpty()) {
            return second.length
        }

        if (second.isEmpty()) {
            return first.length
        }

        var previous = IntArray(second.length + 1) { it }
        var current = IntArray(second.length + 1)

        for (firstIndex in first.indices) {
            current[0] = firstIndex + 1

            for (secondIndex in second.indices) {
                val substitutionCost =
                    if (first[firstIndex] == second[secondIndex]) {
                        0
                    } else {
                        1
                    }

                current[secondIndex + 1] = minOf(
                    current[secondIndex] + 1,
                    previous[secondIndex + 1] + 1,
                    previous[secondIndex] + substitutionCost
                )
            }

            val temporary = previous
            previous = current
            current = temporary
        }

        return previous[second.length]
    }

    private fun calculateMedian(values: List<Int>): Double {
        require(values.isNotEmpty()) {
            "Cannot calculate median of an empty list."
        }

        val middleIndex = values.size / 2

        return if (values.size % 2 == 0) {
            (
                    values[middleIndex - 1].toDouble() +
                            values[middleIndex].toDouble()
                    ) / 2.0
        } else {
            values[middleIndex].toDouble()
        }
    }

    private fun incrementCount(
        counts: MutableMap<String, Int>,
        key: String
    ) {
        counts[key] = counts.getOrDefault(key, 0) + 1
    }

    private fun severityRank(
        severity: CatalogIssueSeverity
    ): Int =
        when (severity) {
            CatalogIssueSeverity.ERROR -> 0
            CatalogIssueSeverity.WARNING -> 1
            CatalogIssueSeverity.INFO -> 2
        }

    private fun formatScore(score: Double): String =
        String.format(Locale.ROOT, "%.4f", score)

    private data class CategoryResolution(
        val definition: CatalogCategoryDefinition?,
        val issueType: CatalogCategoryIssueType,
        val severity: CatalogIssueSeverity,
        val ambiguous: Boolean,
        val message: String
    )

    private data class CategorySimilarityMatch(
        val definition: CatalogCategoryDefinition,
        val score: Double
    )

    private data class RootCategoryEvidence(
        val rootCategoryKey: String,
        val score: Int
    )

    private data class CategoryEvidenceTokens(
        val strong: Set<String>,
        val medium: Set<String> = emptySet(),
        val weak: Set<String> = emptySet()
    )

    private companion object {

        const val MINIMUM_CATEGORY_SIMILARITY = 0.86
        const val CATEGORY_SIMILARITY_AMBIGUITY_MARGIN = 0.025
        const val MAXIMUM_SIMILARITY_MATCH_COUNT = 3

        const val CATEGORY_JACCARD_WEIGHT = 0.45
        const val CATEGORY_CONTAINMENT_WEIGHT = 0.35
        const val CATEGORY_EDIT_WEIGHT = 0.20

        const val VERY_SMALL_CATEGORY_MAXIMUM_COUNT = 2
        const val MINIMUM_LARGE_CATEGORY_ABSOLUTE_COUNT = 100
        const val LARGE_CATEGORY_MEDIAN_MULTIPLIER = 8.0

        const val MINIMUM_TOKEN_LENGTH = 2
        const val STRONG_EVIDENCE_WEIGHT = 3
        const val MEDIUM_EVIDENCE_WEIGHT = 2
        const val WEAK_EVIDENCE_WEIGHT = 1
        const val MINIMUM_MISCLASSIFICATION_SCORE = 3
        const val MINIMUM_MISCLASSIFICATION_MARGIN = 2

        val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")
        val COMBINING_MARKS_REGEX = Regex("\\p{M}+")
        val AMPERSAND_REGEX = Regex("&")
        val UNDERSCORE_OR_WHITESPACE_REGEX = Regex("[_\\s]+")
        val NON_KEY_CHARACTER_REGEX = Regex("[^a-z0-9-]+")
        val MULTIPLE_HYPHEN_REGEX = Regex("-+")
        val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9]+")
        val RAW_CATEGORY_KEY_REGEX =
            Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")

        val STOP_TOKENS = setOf(
            "art",
            "aus",
            "der",
            "die",
            "das",
            "ein",
            "eine",
            "einer",
            "eines",
            "fuer",
            "für",
            "im",
            "in",
            "mit",
            "nach",
            "oder",
            "und",
            "von",
            "zum",
            "zur"
        )

        val EXPLICIT_CATEGORY_ALIASES = mapOf(
            "alkoholfreie getraenke" to "beverages",
            "babyartikel" to "baby-food",
            "babykost" to "baby-food",
            "backware" to "bakery",
            "backwaren" to "bakery",
            "brotwaren" to "bread",
            "cerealien" to "breakfast-cereals",
            "essige" to "vinegar",
            "fertigessen" to "ready-meals",
            "fisch und meeresfruechte" to "fish",
            "fleisch und wurst" to "meat",
            "fruehstueckscerealien" to "breakfast-cereals",
            "gemuese und salat" to "vegetables",
            "gewuerze und kraeuter" to "spices",
            "getraenke" to "beverages",
            "huelsenfruechte" to "legumes",
            "internationale spezialitaeten" to "international-food",
            "kaeseprodukte" to "cheese",
            "konserven und glaeser" to "canned-food",
            "milch und milchprodukte" to "dairy",
            "milchersatz" to "plant-based-alternatives",
            "molkereiprodukte" to "dairy",
            "nudelwaren" to "pasta",
            "obst und gemuese" to "fruit",
            "oele und fette" to "oils",
            "pflanzliche produkte" to "plant-based-alternatives",
            "reis und getreide" to "grains",
            "saucen und dips" to "sauces",
            "snackartikel" to "snacks",
            "suessigkeiten" to "confectionery",
            "süßigkeiten" to "confectionery",
            "teigwaren" to "pasta",
            "tiefkuehl" to "frozen-food",
            "tiefkuehlkost" to "frozen-food",
            "tiefkühl" to "frozen-food",
            "trockenfruechte" to "dried-fruit",
            "wurstwaren" to "sausage"
        )

        val ROOT_CATEGORY_EVIDENCE_TOKENS = mapOf(
            "fruit" to CategoryEvidenceTokens(
                strong = setOf(
                    "apfel",
                    "birne",
                    "banane",
                    "erdbeere",
                    "himbeere",
                    "kirsche",
                    "mango",
                    "orange",
                    "pfirsich",
                    "traube",
                    "zitrone"
                ),
                medium = setOf(
                    "beere",
                    "frucht",
                    "obst"
                )
            ),
            "vegetables" to CategoryEvidenceTokens(
                strong = setOf(
                    "aubergine",
                    "blumenkohl",
                    "brokkoli",
                    "gurke",
                    "karotte",
                    "kohl",
                    "kuerbis",
                    "kürbis",
                    "paprika",
                    "sellerie",
                    "spargel",
                    "tomate",
                    "zucchini"
                ),
                medium = setOf(
                    "gemuese",
                    "gemüse",
                    "wurzel"
                )
            ),
            "herbs" to CategoryEvidenceTokens(
                strong = setOf(
                    "basilikum",
                    "dill",
                    "koriander",
                    "majoran",
                    "minze",
                    "oregano",
                    "petersilie",
                    "rosmarin",
                    "salbei",
                    "schnittlauch",
                    "thymian"
                ),
                medium = setOf(
                    "kraut",
                    "kraeuter",
                    "kräuter"
                )
            ),
            "mushrooms" to CategoryEvidenceTokens(
                strong = setOf(
                    "austernpilz",
                    "champignon",
                    "morchel",
                    "pfifferling",
                    "pilz",
                    "shiitake",
                    "steinpilz"
                )
            ),
            "meat" to CategoryEvidenceTokens(
                strong = setOf(
                    "fleisch",
                    "kalb",
                    "lamm",
                    "rind",
                    "schwein",
                    "steak",
                    "wildschwein"
                ),
                medium = setOf(
                    "braten",
                    "filet",
                    "hackfleisch",
                    "schnitzel"
                )
            ),
            "poultry" to CategoryEvidenceTokens(
                strong = setOf(
                    "ente",
                    "gans",
                    "gefluegel",
                    "geflügel",
                    "haehnchen",
                    "hähnchen",
                    "pute",
                    "truthahn",
                    "wachtel"
                )
            ),
            "sausage" to CategoryEvidenceTokens(
                strong = setOf(
                    "bratwurst",
                    "leberwurst",
                    "salami",
                    "schinken",
                    "speck",
                    "wurst"
                )
            ),
            "fish" to CategoryEvidenceTokens(
                strong = setOf(
                    "forelle",
                    "hering",
                    "kabeljau",
                    "lachs",
                    "makrele",
                    "sardine",
                    "thunfisch",
                    "zander"
                ),
                medium = setOf(
                    "fisch",
                    "filet"
                )
            ),
            "seafood" to CategoryEvidenceTokens(
                strong = setOf(
                    "garnele",
                    "hummer",
                    "krabbe",
                    "krebs",
                    "muschel",
                    "oktopus",
                    "scampi",
                    "tintenfisch"
                )
            ),
            "dairy" to CategoryEvidenceTokens(
                strong = setOf(
                    "butter",
                    "buttermilch",
                    "joghurt",
                    "kefir",
                    "milch",
                    "quark",
                    "sahne"
                )
            ),
            "cheese" to CategoryEvidenceTokens(
                strong = setOf(
                    "brie",
                    "camembert",
                    "emmentaler",
                    "feta",
                    "gouda",
                    "kaese",
                    "käse",
                    "mozzarella",
                    "parmesan"
                )
            ),
            "eggs" to CategoryEvidenceTokens(
                strong = setOf(
                    "ei",
                    "eier",
                    "eigelb",
                    "eiklar",
                    "wachtelei"
                )
            ),
            "bread" to CategoryEvidenceTokens(
                strong = setOf(
                    "brot",
                    "knäckebrot",
                    "knaeckebrot",
                    "pumpernickel",
                    "toast"
                )
            ),
            "bakery" to CategoryEvidenceTokens(
                strong = setOf(
                    "broetchen",
                    "brötchen",
                    "croissant",
                    "gebaeck",
                    "gebäck",
                    "keks",
                    "kuchen",
                    "torte",
                    "waffel"
                )
            ),
            "grains" to CategoryEvidenceTokens(
                strong = setOf(
                    "dinkel",
                    "gerste",
                    "hafer",
                    "hirse",
                    "mais",
                    "roggen",
                    "weizen"
                ),
                medium = setOf(
                    "getreide",
                    "korn"
                )
            ),
            "rice" to CategoryEvidenceTokens(
                strong = setOf(
                    "basmati",
                    "jasminreis",
                    "reis",
                    "risottoreis",
                    "sushireis"
                )
            ),
            "pasta" to CategoryEvidenceTokens(
                strong = setOf(
                    "farfalle",
                    "fusilli",
                    "lasagne",
                    "linguine",
                    "makaroni",
                    "nudel",
                    "nudeln",
                    "penne",
                    "spaghetti",
                    "tagliatelle"
                )
            ),
            "legumes" to CategoryEvidenceTokens(
                strong = setOf(
                    "bohne",
                    "erbse",
                    "huelsenfrucht",
                    "hülsenfrucht",
                    "kichererbse",
                    "linse",
                    "lupine",
                    "sojabohne"
                )
            ),
            "frozen-food" to CategoryEvidenceTokens(
                strong = setOf(
                    "tiefgekuehlt",
                    "tiefgekühlt",
                    "tiefkuehl",
                    "tiefkühl",
                    "tk"
                )
            ),
            "ready-meals" to CategoryEvidenceTokens(
                strong = setOf(
                    "fertiggericht",
                    "lasagne",
                    "mahlzeit",
                    "menue",
                    "menü",
                    "pizza"
                )
            ),
            "spices" to CategoryEvidenceTokens(
                strong = setOf(
                    "chili",
                    "curry",
                    "gewuerz",
                    "gewürz",
                    "paprikapulver",
                    "pfeffer",
                    "salz",
                    "zimt"
                )
            ),
            "oils" to CategoryEvidenceTokens(
                strong = setOf(
                    "oel",
                    "öl",
                    "olivenoel",
                    "olivenöl",
                    "rapsoel",
                    "rapsöl",
                    "sonnenblumenoel",
                    "sonnenblumenöl"
                )
            ),
            "vinegar" to CategoryEvidenceTokens(
                strong = setOf(
                    "apfelessig",
                    "balsamico",
                    "essig",
                    "reisessig",
                    "weinessig"
                )
            ),
            "sauces" to CategoryEvidenceTokens(
                strong = setOf(
                    "dip",
                    "dressing",
                    "ketchup",
                    "mayonnaise",
                    "pesto",
                    "sauce",
                    "senf"
                )
            ),
            "confectionery" to CategoryEvidenceTokens(
                strong = setOf(
                    "bonbon",
                    "fruchtgummi",
                    "kaugummi",
                    "lakritz",
                    "marzipan",
                    "praline",
                    "schokolade"
                )
            ),
            "snacks" to CategoryEvidenceTokens(
                strong = setOf(
                    "chips",
                    "cracker",
                    "flips",
                    "popcorn",
                    "riegel",
                    "snack"
                )
            ),
            "nuts" to CategoryEvidenceTokens(
                strong = setOf(
                    "cashew",
                    "haselnuss",
                    "mandel",
                    "nuss",
                    "nüsse",
                    "pistazie",
                    "walnuss"
                )
            ),
            "seeds" to CategoryEvidenceTokens(
                strong = setOf(
                    "chia",
                    "hanfsamen",
                    "kern",
                    "kuerbiskern",
                    "kürbiskern",
                    "leinsamen",
                    "mohn",
                    "saat",
                    "samen",
                    "sesam"
                )
            ),
            "beverages" to CategoryEvidenceTokens(
                strong = setOf(
                    "getraenk",
                    "getränk",
                    "kaffee",
                    "limonade",
                    "saft",
                    "tee",
                    "wasser"
                )
            )
        )
    }
}