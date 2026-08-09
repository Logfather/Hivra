package de.shopme.testing.system.tools.knowledge.catalog.canonicalization

import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryIssue
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryIssueType
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateGroup
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateRecommendation
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageIssue
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageIssueType
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.CatalogNonFoodCandidate
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.NonFoodRecommendation
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationResult
import java.util.Locale

class CatalogCanonicalizationPlanner {

    fun createPlan(
        entries: List<IndexedCatalogFoodItem>,
        normalizations: List<CatalogNormalizationResult>,
        duplicateGroups: List<CatalogDuplicateGroup>,
        categoryResult: CatalogCategoryValidationResult,
        languageResult: CatalogLanguageValidationResult,
        nonFoodCandidates: List<CatalogNonFoodCandidate>
    ): CatalogCanonicalizationPlan {
        validateInputs(
            entries = entries,
            normalizations = normalizations,
            duplicateGroups = duplicateGroups,
            categoryResult = categoryResult,
            languageResult = languageResult,
            nonFoodCandidates = nonFoodCandidates
        )

        val normalizationBySourceIndex =
            normalizations.associateBy { it.sourceIndex }

        val duplicateMembershipBySourceIndex =
            buildDuplicateMembershipIndex(duplicateGroups)

        val categoryIssuesBySourceIndex =
            categoryResult.issues
                .filter { it.sourceIndex != null }
                .groupBy { requireNotNull(it.sourceIndex) }

        val languageIssuesBySourceIndex =
            languageResult.issues.groupBy { it.sourceIndex }

        val nonFoodBySourceIndex =
            nonFoodCandidates.associateBy { it.sourceIndex }

        val planEntries = entries
            .sortedBy { it.sourceIndex }
            .map { entry ->
                createPlanEntry(
                    entry = entry,
                    normalization =
                        normalizationBySourceIndex.getValue(entry.sourceIndex),
                    duplicateMembership =
                        duplicateMembershipBySourceIndex[entry.sourceIndex],
                    categoryIssues =
                        categoryIssuesBySourceIndex[entry.sourceIndex]
                            .orEmpty(),
                    languageIssues =
                        languageIssuesBySourceIndex[entry.sourceIndex]
                            .orEmpty(),
                    nonFoodCandidate =
                        nonFoodBySourceIndex[entry.sourceIndex]
                )
            }

        val actionCounts = planEntries
            .groupingBy { it.action }
            .eachCount()
            .toList()
            .sortedBy { (action, _) -> action.name }
            .associate { it }

        val affectedSourceIndices = planEntries
            .asSequence()
            .filter {
                it.action != CatalogCanonicalizationAction.KEEP
            }
            .map { it.sourceIndex }
            .sorted()
            .toList()

        val structurallyValid =
            planEntries.size == entries.size &&
                    planEntries.map { it.sourceIndex }.distinct().size ==
                    entries.size

        return CatalogCanonicalizationPlan(
            version = CatalogCanonicalizationPlan.CURRENT_VERSION,
            inputEntryCount = entries.size,
            planEntryCount = planEntries.size,
            automaticActionCount =
                planEntries.count { it.automatic },
            reviewActionCount =
                planEntries.count {
                    it.action ==
                            CatalogCanonicalizationAction.REVIEW
                },
            unchangedEntryCount =
                planEntries.count {
                    it.action ==
                            CatalogCanonicalizationAction.KEEP
                },
            actionCounts = actionCounts,
            affectedSourceIndices = affectedSourceIndices,
            entries = planEntries,
            valid = structurallyValid
        )
    }

    private fun createPlanEntry(
        entry: IndexedCatalogFoodItem,
        normalization: CatalogNormalizationResult,
        duplicateMembership: DuplicateMembership?,
        categoryIssues: List<CatalogCategoryIssue>,
        languageIssues: List<CatalogLanguageIssue>,
        nonFoodCandidate: CatalogNonFoodCandidate?
    ): CatalogCanonicalizationPlanEntry {
        val normalizedName = normalization.computedCanonicalName

        val normalizedCategorySuggestion =
            resolveCategorySuggestion(categoryIssues)

        val languageNameSuggestion =
            resolveLanguageNameSuggestion(languageIssues)

        val effectiveProposedName =
            languageNameSuggestion
                ?.takeIf(String::isNotBlank)
                ?: normalizedName.takeIf(String::isNotBlank)

        val nonFoodDecision = resolveNonFoodDecision(
            entry = entry,
            normalization = normalization,
            candidate = nonFoodCandidate,
            categorySuggestion = normalizedCategorySuggestion
        )

        if (nonFoodDecision != null) {
            return nonFoodDecision
        }

        val duplicateDecision = resolveDuplicateDecision(
            entry = entry,
            normalization = normalization,
            membership = duplicateMembership,
            categorySuggestion = normalizedCategorySuggestion
        )

        if (duplicateDecision != null) {
            return duplicateDecision
        }

        val splitDecision = resolveSplitDecision(
            entry = entry,
            normalization = normalization,
            categorySuggestion = normalizedCategorySuggestion
        )

        if (splitDecision != null) {
            return splitDecision
        }

        val categoryDecision = resolveCategoryDecision(
            entry = entry,
            normalization = normalization,
            categoryIssues = categoryIssues,
            suggestedCategory = normalizedCategorySuggestion,
            proposedName = effectiveProposedName
        )

        if (categoryDecision != null) {
            return categoryDecision
        }

        val languageDecision = resolveLanguageDecision(
            entry = entry,
            normalization = normalization,
            languageIssues = languageIssues,
            proposedName = effectiveProposedName,
            proposedCategory = normalizedCategorySuggestion
        )

        if (languageDecision != null) {
            return languageDecision
        }

        val normalizationDecision = resolveNormalizationDecision(
            entry = entry,
            normalization = normalization,
            proposedCategory = normalizedCategorySuggestion
        )

        if (normalizationDecision != null) {
            return normalizationDecision
        }

        return CatalogCanonicalizationPlanEntry(
            sourceIndex = entry.sourceIndex,
            originalItemName = entry.item.itemname,
            action = CatalogCanonicalizationAction.KEEP,
            proposedCanonicalName = null,
            proposedNormalizedKey = null,
            proposedCategory = null,
            mergeTargetSourceIndex = null,
            reasons = listOf(
                "No canonicalization issue was detected."
            ),
            confidence = 1.0,
            automatic = true
        )
    }

    private fun resolveNonFoodDecision(
        entry: IndexedCatalogFoodItem,
        normalization: CatalogNormalizationResult,
        candidate: CatalogNonFoodCandidate?,
        categorySuggestion: String?
    ): CatalogCanonicalizationPlanEntry? {
        candidate ?: return null

        val reasons = buildList {
            add(
                "Non-food detector classified the entry as " +
                        candidate.recommendation.name +
                        " with confidence " +
                        formatConfidence(candidate.confidence) +
                        "."
            )

            if (candidate.reasons.isNotEmpty()) {
                add(
                    "Non-food reasons: " +
                            candidate.reasons
                                .map { it.name }
                                .sorted()
                                .joinToString(", ") +
                            "."
                )
            }

            if (candidate.matchedTerms.isNotEmpty()) {
                add(
                    "Matched non-food terms: " +
                            candidate.matchedTerms
                                .sorted()
                                .joinToString(", ") +
                            "."
                )
            }
        }

        return when (candidate.recommendation) {
            NonFoodRecommendation.REMOVE_AUTOMATICALLY ->
                CatalogCanonicalizationPlanEntry(
                    sourceIndex = entry.sourceIndex,
                    originalItemName = entry.item.itemname,
                    action =
                        CatalogCanonicalizationAction.REMOVE_NON_FOOD,
                    proposedCanonicalName = null,
                    proposedNormalizedKey = null,
                    proposedCategory = null,
                    mergeTargetSourceIndex = null,
                    reasons = reasons,
                    confidence = candidate.confidence,
                    automatic = true
                )

            NonFoodRecommendation.REMOVE_AFTER_REVIEW ->
                CatalogCanonicalizationPlanEntry(
                    sourceIndex = entry.sourceIndex,
                    originalItemName = entry.item.itemname,
                    action =
                        CatalogCanonicalizationAction.REMOVE_NON_FOOD,
                    proposedCanonicalName = null,
                    proposedNormalizedKey = null,
                    proposedCategory = null,
                    mergeTargetSourceIndex = null,
                    reasons = reasons,
                    confidence = candidate.confidence,
                    automatic = false
                )

            NonFoodRecommendation.REVIEW ->
                CatalogCanonicalizationPlanEntry(
                    sourceIndex = entry.sourceIndex,
                    originalItemName = entry.item.itemname,
                    action = CatalogCanonicalizationAction.REVIEW,
                    proposedCanonicalName =
                        normalization.computedCanonicalName
                            .takeIf(String::isNotBlank),
                    proposedNormalizedKey =
                        normalization.computedNormalizedKey
                            .takeIf(String::isNotBlank),
                    proposedCategory = categorySuggestion,
                    mergeTargetSourceIndex = null,
                    reasons = reasons,
                    confidence = candidate.confidence,
                    automatic = false
                )

            NonFoodRecommendation.KEEP ->
                null
        }
    }

    private fun resolveDuplicateDecision(
        entry: IndexedCatalogFoodItem,
        normalization: CatalogNormalizationResult,
        membership: DuplicateMembership?,
        categorySuggestion: String?
    ): CatalogCanonicalizationPlanEntry? {
        membership ?: return null

        if (entry.sourceIndex == membership.canonicalSourceIndex) {
            return null
        }

        val reasons = buildList {
            add(
                "Entry belongs to duplicate group " +
                        "'${membership.group.groupId}'."
            )

            add(
                "Canonical duplicate target is sourceIndex " +
                        "${membership.canonicalSourceIndex} " +
                        "('${membership.group.canonicalCandidateName}')."
            )

            if (membership.memberReasons.isNotEmpty()) {
                add(
                    "Duplicate reasons: " +
                            membership.memberReasons
                                .map { it.name }
                                .sorted()
                                .joinToString(", ") +
                            "."
                )
            }

            add(
                "Duplicate recommendation: " +
                        membership.group.recommendation.name +
                        "."
            )
        }

        return when (membership.group.recommendation) {
            CatalogDuplicateRecommendation.MERGE_AUTOMATICALLY ->
                CatalogCanonicalizationPlanEntry(
                    sourceIndex = entry.sourceIndex,
                    originalItemName = entry.item.itemname,
                    action = CatalogCanonicalizationAction.MERGE,
                    proposedCanonicalName =
                        membership.group.canonicalCandidateName,
                    proposedNormalizedKey = null,
                    proposedCategory = categorySuggestion,
                    mergeTargetSourceIndex =
                        membership.canonicalSourceIndex,
                    reasons = reasons,
                    confidence = membership.memberScore,
                    automatic = true
                )

            CatalogDuplicateRecommendation.MERGE_AFTER_REVIEW ->
                CatalogCanonicalizationPlanEntry(
                    sourceIndex = entry.sourceIndex,
                    originalItemName = entry.item.itemname,
                    action = CatalogCanonicalizationAction.MERGE,
                    proposedCanonicalName =
                        membership.group.canonicalCandidateName,
                    proposedNormalizedKey = null,
                    proposedCategory = categorySuggestion,
                    mergeTargetSourceIndex =
                        membership.canonicalSourceIndex,
                    reasons = reasons,
                    confidence = membership.memberScore,
                    automatic = false
                )

            CatalogDuplicateRecommendation.REVIEW ->
                CatalogCanonicalizationPlanEntry(
                    sourceIndex = entry.sourceIndex,
                    originalItemName = entry.item.itemname,
                    action = CatalogCanonicalizationAction.REVIEW,
                    proposedCanonicalName =
                        normalization.computedCanonicalName
                            .takeIf(String::isNotBlank),
                    proposedNormalizedKey =
                        normalization.computedNormalizedKey
                            .takeIf(String::isNotBlank),
                    proposedCategory = categorySuggestion,
                    mergeTargetSourceIndex =
                        membership.canonicalSourceIndex,
                    reasons = reasons,
                    confidence = membership.memberScore,
                    automatic = false
                )

            CatalogDuplicateRecommendation.KEEP_SEPARATE ->
                null
        }
    }

    private fun resolveSplitDecision(
        entry: IndexedCatalogFoodItem,
        normalization: CatalogNormalizationResult,
        categorySuggestion: String?
    ): CatalogCanonicalizationPlanEntry? {
        val itemName = entry.item.itemname
        val normalizedItemName =
            itemName.lowercase(Locale.GERMAN)

        val matchedSeparators = MULTI_ITEM_SEPARATORS
            .filter { separator ->
                separator.containsMatchIn(normalizedItemName)
            }

        if (matchedSeparators.isEmpty()) {
            return null
        }

        val itemParts = splitPotentialCombinedItem(itemName)

        if (itemParts.size < 2) {
            return null
        }

        val hasProtectedExpression =
            PROTECTED_COMBINED_FOOD_EXPRESSIONS.any {
                normalizedItemName.contains(it)
            }

        if (hasProtectedExpression) {
            return null
        }

        return CatalogCanonicalizationPlanEntry(
            sourceIndex = entry.sourceIndex,
            originalItemName = itemName,
            action = CatalogCanonicalizationAction.SPLIT,
            proposedCanonicalName =
                normalization.computedCanonicalName
                    .takeIf(String::isNotBlank),
            proposedNormalizedKey =
                normalization.computedNormalizedKey
                    .takeIf(String::isNotBlank),
            proposedCategory = categorySuggestion,
            mergeTargetSourceIndex = null,
            reasons = listOf(
                "Primary name may represent multiple canonical foods.",
                "Detected item components: " +
                        itemParts.joinToString(" | ") +
                        "."
            ),
            confidence = SPLIT_REVIEW_CONFIDENCE,
            automatic = false
        )
    }

    private fun resolveCategoryDecision(
        entry: IndexedCatalogFoodItem,
        normalization: CatalogNormalizationResult,
        categoryIssues: List<CatalogCategoryIssue>,
        suggestedCategory: String?,
        proposedName: String?
    ): CatalogCanonicalizationPlanEntry? {
        if (categoryIssues.isEmpty()) {
            return null
        }

        val unknownOrAmbiguous = categoryIssues.any {
            it.type == CatalogCategoryIssueType.UNKNOWN_CATEGORY ||
                    it.type ==
                    CatalogCategoryIssueType.AMBIGUOUS_CATEGORY_REFERENCE
        }

        val possibleMisclassification = categoryIssues.any {
            it.type ==
                    CatalogCategoryIssueType.POSSIBLE_MISCLASSIFICATION
        }

        if (
            unknownOrAmbiguous ||
            (
                    possibleMisclassification &&
                            suggestedCategory == null
                    )
        ) {
            return CatalogCanonicalizationPlanEntry(
                sourceIndex = entry.sourceIndex,
                originalItemName = entry.item.itemname,
                action = CatalogCanonicalizationAction.REVIEW,
                proposedCanonicalName = proposedName,
                proposedNormalizedKey =
                    normalization.computedNormalizedKey
                        .takeIf(String::isNotBlank),
                proposedCategory = suggestedCategory,
                mergeTargetSourceIndex = null,
                reasons = categoryIssues
                    .map { it.message }
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),
                confidence = CATEGORY_REVIEW_CONFIDENCE,
                automatic = false
            )
        }

        if (
            suggestedCategory != null &&
            suggestedCategory != entry.item.category
        ) {
            val automatic =
                categoryIssues.none {
                    it.type ==
                            CatalogCategoryIssueType
                                .POSSIBLE_MISCLASSIFICATION
                }

            return CatalogCanonicalizationPlanEntry(
                sourceIndex = entry.sourceIndex,
                originalItemName = entry.item.itemname,
                action =
                    CatalogCanonicalizationAction.MOVE_CATEGORY,
                proposedCanonicalName = proposedName,
                proposedNormalizedKey =
                    normalization.computedNormalizedKey
                        .takeIf(String::isNotBlank),
                proposedCategory = suggestedCategory,
                mergeTargetSourceIndex = null,
                reasons = categoryIssues
                    .map { it.message }
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),
                confidence = if (automatic) {
                    AUTOMATIC_CATEGORY_CONFIDENCE
                } else {
                    CATEGORY_REVIEW_CONFIDENCE
                },
                automatic = automatic
            )
        }

        return null
    }

    private fun resolveLanguageDecision(
        entry: IndexedCatalogFoodItem,
        normalization: CatalogNormalizationResult,
        languageIssues: List<CatalogLanguageIssue>,
        proposedName: String?,
        proposedCategory: String?
    ): CatalogCanonicalizationPlanEntry? {
        if (languageIssues.isEmpty()) {
            return null
        }

        val primaryNameIssues = languageIssues.filter {
            it.field == FIELD_ITEM_NAME
        }

        if (primaryNameIssues.isEmpty()) {
            return null
        }

        val requiresReview = primaryNameIssues.any {
            it.type in REVIEW_LANGUAGE_ISSUE_TYPES
        }

        val safeRenameIssues = primaryNameIssues.filter {
            it.type in SAFE_RENAME_ISSUE_TYPES
        }

        val suggestedName = chooseBestLanguageSuggestion(
            originalName = entry.item.itemname,
            issues = primaryNameIssues,
            normalization = normalization
        )

        if (requiresReview) {
            return CatalogCanonicalizationPlanEntry(
                sourceIndex = entry.sourceIndex,
                originalItemName = entry.item.itemname,
                action = CatalogCanonicalizationAction.REVIEW,
                proposedCanonicalName =
                    suggestedName ?: proposedName,
                proposedNormalizedKey =
                    normalization.computedNormalizedKey
                        .takeIf(String::isNotBlank),
                proposedCategory = proposedCategory,
                mergeTargetSourceIndex = null,
                reasons = primaryNameIssues
                    .map { it.message }
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),
                confidence = LANGUAGE_REVIEW_CONFIDENCE,
                automatic = false
            )
        }

        if (
            safeRenameIssues.isNotEmpty() &&
            !suggestedName.isNullOrBlank() &&
            suggestedName != entry.item.itemname
        ) {
            return CatalogCanonicalizationPlanEntry(
                sourceIndex = entry.sourceIndex,
                originalItemName = entry.item.itemname,
                action = CatalogCanonicalizationAction.RENAME,
                proposedCanonicalName = suggestedName,
                proposedNormalizedKey =
                    normalization.computedNormalizedKey
                        .takeIf(String::isNotBlank),
                proposedCategory = proposedCategory,
                mergeTargetSourceIndex = null,
                reasons = safeRenameIssues
                    .map { it.message }
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),
                confidence = AUTOMATIC_LANGUAGE_CONFIDENCE,
                automatic = true
            )
        }

        return null
    }

    private fun resolveNormalizationDecision(
        entry: IndexedCatalogFoodItem,
        normalization: CatalogNormalizationResult,
        proposedCategory: String?
    ): CatalogCanonicalizationPlanEntry? {
        val canonicalNameChanged =
            normalization.computedCanonicalName !=
                    entry.item.itemname

        val normalizedKeyChanged =
            normalization.computedNormalizedKey !=
                    entry.item.normalized

        val normalizationChanged =
            canonicalNameChanged ||
                    normalizedKeyChanged ||
                    normalization.changes.isNotEmpty()

        if (!normalizationChanged) {
            return null
        }

        val reasons = buildList {
            if (canonicalNameChanged) {
                add(
                    "Canonical name changes from " +
                            "'${entry.item.itemname}' to " +
                            "'${normalization.computedCanonicalName}'."
                )
            }

            if (normalizedKeyChanged) {
                add(
                    "Normalized key changes from " +
                            "'${entry.item.normalized}' to " +
                            "'${normalization.computedNormalizedKey}'."
                )
            }

            normalization.changes
                .map { change ->
                    buildString {
                        append("Normalization change ")
                        append(change.type.name)
                        append(" in field '")
                        append(change.field)
                        append("'")

                        if (change.reason.isNotBlank()) {
                            append(": ")
                            append(change.reason)
                        }

                        append(".")
                    }
                }
                .distinct()
                .sorted()
                .forEach(::add)
        }

        return CatalogCanonicalizationPlanEntry(
            sourceIndex = entry.sourceIndex,
            originalItemName = entry.item.itemname,
            action = CatalogCanonicalizationAction.NORMALIZE,
            proposedCanonicalName =
                normalization.computedCanonicalName,
            proposedNormalizedKey =
                normalization.computedNormalizedKey,
            proposedCategory = proposedCategory,
            mergeTargetSourceIndex = null,
            reasons = reasons
                .filter(String::isNotBlank)
                .distinct()
                .sorted(),
            confidence = AUTOMATIC_NORMALIZATION_CONFIDENCE,
            automatic = true
        )
    }

    private fun resolveCategorySuggestion(
        issues: List<CatalogCategoryIssue>
    ): String? {
        val suggestions = issues
            .mapNotNull { it.suggestedCategoryKey }
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

        return suggestions.singleOrNull()
    }

    private fun resolveLanguageNameSuggestion(
        issues: List<CatalogLanguageIssue>
    ): String? {
        val suggestions = issues
            .asSequence()
            .filter { it.field == FIELD_ITEM_NAME }
            .mapNotNull { it.suggestedValue }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sortedWith(
                compareByDescending<String> { it.length }
                    .thenBy {
                        it.lowercase(Locale.GERMAN)
                    }
            )
            .toList()

        return suggestions.firstOrNull()
    }

    private fun chooseBestLanguageSuggestion(
        originalName: String,
        issues: List<CatalogLanguageIssue>,
        normalization: CatalogNormalizationResult
    ): String? {
        val explicitSuggestions = issues
            .mapNotNull { it.suggestedValue }
            .map(String::trim)
            .filter(String::isNotBlank)
            .filter { it != originalName }
            .distinct()
            .sortedWith(
                compareBy<String>(
                    { suggestionDistance(originalName, it) },
                    { it.length },
                    { it.lowercase(Locale.GERMAN) }
                )
            )

        return explicitSuggestions.firstOrNull()
            ?: normalization.computedCanonicalName
                .takeIf {
                    it.isNotBlank() &&
                            it != originalName
                }
    }

    private fun buildDuplicateMembershipIndex(
        duplicateGroups: List<CatalogDuplicateGroup>
    ): Map<Int, DuplicateMembership> {
        val memberships = linkedMapOf<Int, DuplicateMembership>()

        duplicateGroups
            .sortedBy { it.groupId }
            .forEach { group ->
                group.members
                    .sortedBy { it.sourceIndex }
                    .forEach { member ->
                        val membership = DuplicateMembership(
                            group = group,
                            canonicalSourceIndex =
                                group.canonicalCandidateSourceIndex,
                            memberScore = member.matchScore,
                            memberReasons = member.reasons
                        )

                        val existing =
                            memberships.put(
                                member.sourceIndex,
                                membership
                            )

                        require(existing == null) {
                            "sourceIndex ${member.sourceIndex} belongs to " +
                                    "multiple duplicate groups: " +
                                    "'${existing?.group?.groupId}' and " +
                                    "'${group.groupId}'."
                        }
                    }
            }

        return memberships
    }

    private fun validateInputs(
        entries: List<IndexedCatalogFoodItem>,
        normalizations: List<CatalogNormalizationResult>,
        duplicateGroups: List<CatalogDuplicateGroup>,
        categoryResult: CatalogCategoryValidationResult,
        languageResult: CatalogLanguageValidationResult,
        nonFoodCandidates: List<CatalogNonFoodCandidate>
    ) {
        val entryIndices =
            entries.map { it.sourceIndex }

        require(entryIndices.distinct().size == entries.size) {
            "Catalog entries contain duplicate sourceIndex values."
        }

        val normalizationIndices =
            normalizations.map { it.sourceIndex }

        require(
            normalizationIndices.distinct().size ==
                    normalizations.size
        ) {
            "Catalog normalizations contain duplicate sourceIndex values."
        }

        require(
            normalizationIndices.toSet() ==
                    entryIndices.toSet()
        ) {
            val missing =
                entryIndices.toSet() -
                        normalizationIndices.toSet()

            val unexpected =
                normalizationIndices.toSet() -
                        entryIndices.toSet()

            buildString {
                append(
                    "Catalog normalization source indices do not match " +
                            "catalog entry source indices."
                )

                if (missing.isNotEmpty()) {
                    append(" Missing: ")
                    append(missing.sorted().joinToString(", "))
                    append(".")
                }

                if (unexpected.isNotEmpty()) {
                    append(" Unexpected: ")
                    append(unexpected.sorted().joinToString(", "))
                    append(".")
                }
            }
        }

        require(
            categoryResult.inputEntryCount ==
                    entries.size
        ) {
            "Category validation inputEntryCount " +
                    "${categoryResult.inputEntryCount} does not match " +
                    "catalog entry count ${entries.size}."
        }

        require(
            languageResult.inputEntryCount ==
                    entries.size
        ) {
            "Language validation inputEntryCount " +
                    "${languageResult.inputEntryCount} does not match " +
                    "catalog entry count ${entries.size}."
        }

        require(
            nonFoodCandidates
                .map { it.sourceIndex }
                .distinct()
                .size ==
                    nonFoodCandidates.size
        ) {
            "Non-food candidates contain duplicate sourceIndex values."
        }

        val unknownNonFoodIndices =
            nonFoodCandidates
                .map { it.sourceIndex }
                .toSet() -
                    entryIndices.toSet()

        require(unknownNonFoodIndices.isEmpty()) {
            "Non-food candidates reference unknown source indices: " +
                    unknownNonFoodIndices
                        .sorted()
                        .joinToString(", ") +
                    "."
        }

        val duplicateMemberIndices =
            duplicateGroups
                .flatMap { group ->
                    group.members.map { it.sourceIndex }
                }

        val unknownDuplicateIndices =
            duplicateMemberIndices.toSet() -
                    entryIndices.toSet()

        require(unknownDuplicateIndices.isEmpty()) {
            "Duplicate groups reference unknown source indices: " +
                    unknownDuplicateIndices
                        .sorted()
                        .joinToString(", ") +
                    "."
        }

        duplicateGroups.forEach { group ->
            require(
                group.members.any {
                    it.sourceIndex ==
                            group.canonicalCandidateSourceIndex
                }
            ) {
                "Duplicate group '${group.groupId}' does not contain " +
                        "its canonical source index " +
                        "${group.canonicalCandidateSourceIndex}."
            }
        }
    }

    private fun splitPotentialCombinedItem(
        itemName: String
    ): List<String> {
        var parts = listOf(itemName)

        MULTI_ITEM_SEPARATORS.forEach { separator ->
            parts = parts.flatMap { value ->
                separator
                    .split(value)
                    .map(String::trim)
                    .filter(String::isNotBlank)
            }
        }

        return parts
            .map(String::trim)
            .filter { it.length >= MINIMUM_SPLIT_COMPONENT_LENGTH }
            .distinctBy {
                it.lowercase(Locale.GERMAN)
            }
    }

    private fun suggestionDistance(
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

        first.indices.forEach { firstIndex ->
            current[0] = firstIndex + 1

            second.indices.forEach { secondIndex ->
                val substitutionCost =
                    if (
                        first[firstIndex]
                            .lowercaseChar() ==
                        second[secondIndex]
                            .lowercaseChar()
                    ) {
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

    private fun formatConfidence(
        confidence: Double
    ): String =
        String.format(
            Locale.ROOT,
            "%.4f",
            confidence
        )

    private data class DuplicateMembership(
        val group: CatalogDuplicateGroup,
        val canonicalSourceIndex: Int,
        val memberScore: Double,
        val memberReasons:
        Set<de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateReason>
    )

    private companion object {

        const val FIELD_ITEM_NAME = "itemname"

        const val AUTOMATIC_NORMALIZATION_CONFIDENCE = 1.0
        const val AUTOMATIC_LANGUAGE_CONFIDENCE = 0.97
        const val AUTOMATIC_CATEGORY_CONFIDENCE = 0.96
        const val LANGUAGE_REVIEW_CONFIDENCE = 0.70
        const val CATEGORY_REVIEW_CONFIDENCE = 0.65
        const val SPLIT_REVIEW_CONFIDENCE = 0.60

        const val MINIMUM_SPLIT_COMPONENT_LENGTH = 2

        val SAFE_RENAME_ISSUE_TYPES = setOf(
            CatalogLanguageIssueType.LEADING_WHITESPACE,
            CatalogLanguageIssueType.TRAILING_WHITESPACE,
            CatalogLanguageIssueType.MULTIPLE_WHITESPACE,
            CatalogLanguageIssueType.INVALID_CAPITALIZATION,
            CatalogLanguageIssueType.INVALID_UMLAUT_FORM,
            CatalogLanguageIssueType.INVALID_APOSTROPHE,
            CatalogLanguageIssueType.INVALID_HYPHENATION,
            CatalogLanguageIssueType.TYPO_SUSPECTED,
            CatalogLanguageIssueType.PACKAGE_SIZE_IN_PRIMARY_NAME,
            CatalogLanguageIssueType.PRODUCT_NUMBER_IN_PRIMARY_NAME
        )

        val REVIEW_LANGUAGE_ISSUE_TYPES = setOf(
            CatalogLanguageIssueType.ENGLISH_PRIMARY_NAME,
            CatalogLanguageIssueType.MIXED_LANGUAGE_NAME,
            CatalogLanguageIssueType.ABBREVIATION_SUSPECTED,
            CatalogLanguageIssueType.BRAND_IN_PRIMARY_NAME,
            CatalogLanguageIssueType.RETAILER_IN_PRIMARY_NAME
        )

        val MULTI_ITEM_SEPARATORS = listOf(
            Regex(
                pattern = "\\s+/\\s+",
                option = RegexOption.IGNORE_CASE
            ),
            Regex(
                pattern = "\\s+&\\s+",
                option = RegexOption.IGNORE_CASE
            ),
            Regex(
                pattern = "\\s+und\\s+",
                option = RegexOption.IGNORE_CASE
            ),
            Regex(
                pattern = "\\s+oder\\s+",
                option = RegexOption.IGNORE_CASE
            ),
            Regex(
                pattern = "\\s*;\\s*",
                option = RegexOption.IGNORE_CASE
            )
        )

        val PROTECTED_COMBINED_FOOD_EXPRESSIONS = setOf(
            "brot und butter",
            "essig und öl",
            "essig und oel",
            "fisch und meeresfrüchte",
            "fisch und meeresfruechte",
            "kräuter und gewürze",
            "kraeuter und gewuerze",
            "milch und milchprodukte",
            "nudeln und reis",
            "obst und gemüse",
            "obst und gemuese",
            "salz und pfeffer",
            "saucen und dips",
            "zucker und süßungsmittel",
            "zucker und suessungsmittel"
        )
    }
}