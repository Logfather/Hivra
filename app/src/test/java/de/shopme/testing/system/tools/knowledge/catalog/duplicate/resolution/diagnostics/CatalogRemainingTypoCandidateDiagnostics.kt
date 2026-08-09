package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlan
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassificationResult
import java.util.Locale
import kotlin.math.abs

class CatalogRemainingTypoCandidateDiagnostics {

    fun diagnose(
        classificationResult:
        CatalogReviewBacklogClassificationResult,

        sourceEntries:
        List<IndexedCatalogFoodItem>,

        plan:
        CatalogCanonicalizationPlan
    ): CatalogRemainingTypoDiagnosticsResult {
        require(classificationResult.valid)
        require(plan.valid)

        val sourceItemsByIndex =
            sourceEntries.associate {
                it.sourceIndex to it.item
            }

        require(
            sourceItemsByIndex.size ==
                    sourceEntries.size
        ) {
            "sourceEntries contain duplicate sourceIndex values."
        }

        val planEntriesByIndex =
            plan.entries.associateBy {
                it.sourceIndex
            }

        val remainingTypoEntries =
            classificationResult.entries
                .filter {
                    it.primaryClassification ==
                            CatalogReviewBacklogClassification
                                .TYPO_VARIANT
                }
                .sortedBy { it.sourceIndex }

        val diagnostics =
            remainingTypoEntries.map { classifiedEntry ->
                val planEntry =
                    planEntriesByIndex[
                        classifiedEntry.sourceIndex
                    ]

                val sourceItem =
                    sourceItemsByIndex[
                        classifiedEntry.sourceIndex
                    ]

                diagnoseEntry(
                    sourceIndex =
                        classifiedEntry.sourceIndex,

                    fallbackSourceName =
                        classifiedEntry.itemName,

                    fallbackCategory =
                        classifiedEntry.category,

                    fallbackNormalizedKey =
                        classifiedEntry.normalizedKey,

                    sourceItem =
                        sourceItem,

                    targetSourceIndex =
                        classifiedEntry
                            .mergeTargetSourceIndex,

                    targetItem =
                        classifiedEntry
                            .mergeTargetSourceIndex
                            ?.let(sourceItemsByIndex::get),

                    action =
                        planEntry?.action
                            ?: classifiedEntry
                                .originalAction,

                    originalReasons =
                        planEntry?.reasons
                            ?: classifiedEntry
                                .originalReasons
                )
            }

        val countsBySubtype =
            diagnostics
                .groupingBy { it.subtype }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByRecommendation =
            diagnostics
                .groupingBy {
                    it.recommendation
                }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByDiagnosticReason =
            diagnostics
                .flatMap {
                    it.diagnosticReasons
                }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByCategory =
            diagnostics
                .groupingBy {
                    it.sourceCategory
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?: UNCATEGORIZED
                }
                .eachCount()
                .toSortedMap()

        return CatalogRemainingTypoDiagnosticsResult(
            version =
                CatalogRemainingTypoDiagnosticsResult
                    .CURRENT_VERSION,

            remainingTypoCandidateCount =
                remainingTypoEntries.size,

            diagnosedEntryCount =
                diagnostics.size,

            existingResolverShouldHaveAcceptedCount =
                diagnostics.count {
                    it.existingResolverWouldAccept
                },

            countsBySubtype =
                countsBySubtype,

            countsByRecommendation =
                countsByRecommendation,

            countsByDiagnosticReason =
                countsByDiagnosticReason,

            countsByCategory =
                countsByCategory,

            entries =
                diagnostics,

            valid = true
        )
    }

    private fun diagnoseEntry(
        sourceIndex: Int,

        fallbackSourceName: String,
        fallbackCategory: String?,
        fallbackNormalizedKey: String?,

        sourceItem: CatalogFoodItem?,

        targetSourceIndex: Int?,
        targetItem: CatalogFoodItem?,

        action: CatalogCanonicalizationAction,
        originalReasons: List<String>
    ): CatalogTypoCandidateDiagnosticEntry {
        val reasons =
            linkedSetOf<
                    CatalogTypoCandidateDiagnosticReason
                    >()

        val sourceName =
            sourceItem
                ?.itemname
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: fallbackSourceName.trim()

        val sourceCategory =
            sourceItem
                ?.category
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: fallbackCategory

        val sourceNormalizedKey =
            sourceItem
                ?.normalized
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: fallbackNormalizedKey

        if (sourceItem == null) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .SOURCE_ENTRY_NOT_FOUND
        }

        if (targetSourceIndex == null) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .MISSING_MERGE_TARGET
        }

        if (targetSourceIndex == sourceIndex) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .SELF_REFERENCING_MERGE_TARGET
        }

        if (
            targetSourceIndex != null &&
            targetItem == null
        ) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .TARGET_ENTRY_NOT_FOUND
        }

        if (
            action != CatalogCanonicalizationAction.MERGE &&
            action != CatalogCanonicalizationAction.REVIEW
        ) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .UNSUPPORTED_PLAN_ACTION
        }

        val normalizedReasons =
            originalReasons
                .map(::normalizeReason)
                .filter(String::isNotBlank)

        if (
            normalizedReasons.none {
                TYPO_MARKER in it
            }
        ) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .MISSING_TYPO_REASON
        }

        if (
            normalizedReasons.any { reason ->
                CONFLICTING_REASON_MARKERS.any {
                    it in reason
                }
            }
        ) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .CONFLICTING_DUPLICATE_REASON
        }

        if (
            normalizedReasons.any { reason ->
                TYPO_MARKER !in reason &&
                        ALLOWED_REASON_MARKERS.none {
                            it in reason
                        }
            }
        ) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .UNKNOWN_PLAN_REASON
        }

        val targetName =
            targetItem
                ?.itemname
                ?.trim()
                ?.takeIf(String::isNotBlank)

        val targetCategory =
            targetItem
                ?.category
                ?.trim()
                ?.takeIf(String::isNotBlank)

        val targetNormalizedKey =
            targetItem
                ?.normalized
                ?.trim()
                ?.takeIf(String::isNotBlank)

        if (
            sourceCategory != null &&
            targetCategory != null &&
            !sourceCategory.equals(
                targetCategory,
                ignoreCase = true
            )
        ) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .CATEGORY_MISMATCH
        }

        val normalizedSourceText =
            normalizeComparisonText(sourceName)

        val normalizedTargetText =
            targetName
                ?.let(::normalizeComparisonText)
                .orEmpty()

        if (normalizedSourceText.isBlank()) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .EMPTY_SOURCE_NAME
        }

        if (
            targetName != null &&
            normalizedTargetText.isBlank()
        ) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .EMPTY_TARGET_NAME
        }

        if (
            normalizedTargetText.isNotBlank() &&
            normalizedSourceText ==
            normalizedTargetText
        ) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .IDENTICAL_COMPARISON_TEXT
        }

        val sourceTokens =
            tokenize(normalizedSourceText)

        val targetTokens =
            tokenize(normalizedTargetText)

        val sourceNumericTokens =
            extractNumericTokens(
                normalizedSourceText
            )

        val targetNumericTokens =
            extractNumericTokens(
                normalizedTargetText
            )

        if (
            sourceNumericTokens !=
            targetNumericTokens
        ) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .NUMERIC_TOKEN_MISMATCH
        }

        if (
            sourceTokens.size !=
            targetTokens.size
        ) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .TOKEN_COUNT_MISMATCH
        }

        val tokenDifferences =
            if (
                sourceTokens.size ==
                targetTokens.size
            ) {
                sourceTokens
                    .zip(targetTokens)
                    .mapIndexedNotNull {
                            index,
                            (sourceToken, targetToken) ->

                        if (sourceToken == targetToken) {
                            null
                        } else {
                            createTokenDifference(
                                tokenIndex = index,
                                sourceToken =
                                    sourceToken,
                                targetToken =
                                    targetToken
                            )
                        }
                    }
            } else {
                emptyList()
            }

        when {
            sourceTokens.size ==
                    targetTokens.size &&
                    tokenDifferences.isEmpty() ->
                reasons +=
                    CatalogTypoCandidateDiagnosticReason
                        .NO_DIFFERING_TOKEN

            tokenDifferences.size > 1 ->
                reasons +=
                    CatalogTypoCandidateDiagnosticReason
                        .MULTIPLE_DIFFERING_TOKENS
        }

        tokenDifferences.forEach { difference ->
            if (
                difference.sourceLength <
                MINIMUM_TOKEN_LENGTH ||
                difference.targetLength <
                MINIMUM_TOKEN_LENGTH
            ) {
                reasons +=
                    CatalogTypoCandidateDiagnosticReason
                        .TOKEN_BELOW_MINIMUM_LENGTH
            }

            if (
                difference
                    .germanOrthographyEquivalent
            ) {
                reasons +=
                    CatalogTypoCandidateDiagnosticReason
                        .GERMAN_ORTHOGRAPHY_EQUIVALENT
            }

            if (difference.typTypeEquivalent) {
                reasons +=
                    CatalogTypoCandidateDiagnosticReason
                        .TYP_TYPE_EQUIVALENT
            }

            if (difference.sauceSosseEquivalent) {
                reasons +=
                    CatalogTypoCandidateDiagnosticReason
                        .SAUCE_SOSSE_EQUIVALENT
            }

            if (difference.editDistance == 1) {
                reasons +=
                    CatalogTypoCandidateDiagnosticReason
                        .EDIT_DISTANCE_ONE
            }

            if (
                difference.editDistance >
                MAXIMUM_EXISTING_RESOLVER_DISTANCE
            ) {
                reasons +=
                    CatalogTypoCandidateDiagnosticReason
                        .EDIT_DISTANCE_EXCEEDS_LIMIT
            }

            if (
                difference.lengthDifference >
                MAXIMUM_EXISTING_LENGTH_DIFFERENCE
            ) {
                reasons +=
                    CatalogTypoCandidateDiagnosticReason
                        .LENGTH_DIFFERENCE_EXCEEDS_LIMIT
            }
        }

        val compactSource =
            compactComparisonText(sourceName)

        val compactTarget =
            targetName
                ?.let(::compactComparisonText)
                .orEmpty()

        val spacingEquivalent =
            normalizedTargetText.isNotBlank() &&
                    compactSource == compactTarget &&
                    sourceTokens.size !=
                    targetTokens.size

        val sourceContainsHyphen =
            HYPHEN_REGEX.containsMatchIn(
                sourceName
            )

        val targetContainsHyphen =
            targetName?.let(
                HYPHEN_REGEX::containsMatchIn
            ) ?: false

        /*
         * Eine Hyphenation-Variante liegt nur vor, wenn mindestens eine der beiden
         * Schreibweisen tatsächlich einen Bindestrich enthält.
         *
         * Reine Zusammen-/Getrenntschreibung wie:
         *
         * Basmati Reis ↔ Basmatireis
         *
         * bleibt dadurch eine COMPOUND_SPACING_VARIANT.
         */
        val hyphenationEquivalent =
            targetName != null &&
                    (sourceContainsHyphen ||
                            targetContainsHyphen) &&
                    normalizeHyphenationText(sourceName) ==
                    normalizeHyphenationText(targetName) &&
                    sourceName != targetName

        if (spacingEquivalent) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .COMPOUND_SPACING_EQUIVALENT
        }

        if (hyphenationEquivalent) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .HYPHENATION_EQUIVALENT
        }

        val existingResolverWouldAccept =
            existingResolverWouldAccept(
                action = action,
                targetSourceIndex =
                    targetSourceIndex,
                sourceIndex = sourceIndex,
                normalizedReasons =
                    normalizedReasons,
                sourceCategory =
                    sourceCategory,
                targetCategory =
                    targetCategory,
                sourceNumericTokens =
                    sourceNumericTokens,
                targetNumericTokens =
                    targetNumericTokens,
                sourceTokens =
                    sourceTokens,
                targetTokens =
                    targetTokens,
                tokenDifferences =
                    tokenDifferences
            )

        if (existingResolverWouldAccept) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .EXISTING_RESOLVER_SHOULD_HAVE_ACCEPTED
        }

        val subtype =
            determineSubtype(
                tokenDifferences =
                    tokenDifferences,
                spacingEquivalent =
                    spacingEquivalent,
                hyphenationEquivalent =
                    hyphenationEquivalent,
                numericTokensDiffer =
                    sourceNumericTokens !=
                            targetNumericTokens,
                tokenCountsDiffer =
                    sourceTokens.size !=
                            targetTokens.size
            )

        val recommendation =
            recommendationFor(
                subtype = subtype,
                existingResolverWouldAccept =
                    existingResolverWouldAccept,
                sourceAvailable =
                    sourceItem != null,
                targetAvailable =
                    targetItem != null
            )

        if (
            recommendation ==
            CatalogTypoCandidateRecommendation
                .MANUAL_REVIEW_REQUIRED
        ) {
            reasons +=
                CatalogTypoCandidateDiagnosticReason
                    .MANUAL_SEMANTIC_REVIEW_REQUIRED
        }

        return CatalogTypoCandidateDiagnosticEntry(
            sourceIndex = sourceIndex,
            sourceName = sourceName,
            sourceCategory = sourceCategory,
            sourceNormalizedKey =
                sourceNormalizedKey,

            targetSourceIndex =
                targetSourceIndex,
            targetName = targetName,
            targetCategory = targetCategory,
            targetNormalizedKey =
                targetNormalizedKey,

            originalAction = action,

            originalReasons =
                originalReasons
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),

            normalizedSourceText =
                normalizedSourceText,
            normalizedTargetText =
                targetName
                    ?.let(::normalizeComparisonText),

            sourceTokens = sourceTokens,
            targetTokens = targetTokens,

            sourceNumericTokens =
                sourceNumericTokens,
            targetNumericTokens =
                targetNumericTokens,

            tokenDifferences =
                tokenDifferences,

            subtype = subtype,
            recommendation =
                recommendation,

            existingResolverWouldAccept =
                existingResolverWouldAccept,

            diagnosticReasons =
                reasons
                    .distinct()
                    .sortedBy { it.name }
        )
    }

    private fun determineSubtype(
        tokenDifferences:
        List<CatalogTypoTokenDifference>,

        spacingEquivalent: Boolean,
        hyphenationEquivalent: Boolean,
        numericTokensDiffer: Boolean,
        tokenCountsDiffer: Boolean
    ): CatalogTypoCandidateSubtype {
        if (numericTokensDiffer) {
            return CatalogTypoCandidateSubtype
                .NUMERIC_VARIANT
        }

        if (hyphenationEquivalent) {
            return CatalogTypoCandidateSubtype
                .HYPHENATION_VARIANT
        }

        if (spacingEquivalent) {
            return CatalogTypoCandidateSubtype
                .COMPOUND_SPACING_VARIANT
        }

        if (tokenCountsDiffer) {
            return CatalogTypoCandidateSubtype
                .PROBABLY_MISCLASSIFIED
        }

        if (tokenDifferences.size > 1) {
            return CatalogTypoCandidateSubtype
                .MULTIPLE_TOKEN_DIFFERENCE
        }

        val difference =
            tokenDifferences.singleOrNull()
                ?: return CatalogTypoCandidateSubtype
                    .UNCLASSIFIED

        return when {
            difference.typTypeEquivalent ->
                CatalogTypoCandidateSubtype
                    .TYP_TYPE_VARIANT

            difference.sauceSosseEquivalent ->
                CatalogTypoCandidateSubtype
                    .SAUCE_SOSSE_VARIANT

            difference
                .germanOrthographyEquivalent ->
                CatalogTypoCandidateSubtype
                    .GERMAN_ORTHOGRAPHY_VARIANT

            difference.editDistance == 1 ->
                CatalogTypoCandidateSubtype
                    .SINGLE_EDIT_TYPO

            difference.editDistance > 1 ->
                CatalogTypoCandidateSubtype
                    .MULTI_EDIT_TYPO

            else ->
                CatalogTypoCandidateSubtype
                    .UNCLASSIFIED
        }
    }

    private fun recommendationFor(
        subtype: CatalogTypoCandidateSubtype,
        existingResolverWouldAccept: Boolean,
        sourceAvailable: Boolean,
        targetAvailable: Boolean
    ): CatalogTypoCandidateRecommendation {
        if (!sourceAvailable || !targetAvailable) {
            return CatalogTypoCandidateRecommendation
                .INVESTIGATE_DATA_INCONSISTENCY
        }

        if (existingResolverWouldAccept) {
            return CatalogTypoCandidateRecommendation
                .INVESTIGATE_EXISTING_TYPO_RESOLVER
        }

        return when (subtype) {
            CatalogTypoCandidateSubtype
                .GERMAN_ORTHOGRAPHY_VARIANT ->
                CatalogTypoCandidateRecommendation
                    .IMPLEMENT_ORTHOGRAPHIC_VARIANT_RESOLVER

            CatalogTypoCandidateSubtype
                .COMPOUND_SPACING_VARIANT ->
                CatalogTypoCandidateRecommendation
                    .IMPLEMENT_COMPOUND_SPACING_RESOLVER

            CatalogTypoCandidateSubtype
                .HYPHENATION_VARIANT ->
                CatalogTypoCandidateRecommendation
                    .IMPLEMENT_HYPHENATION_RESOLVER

            CatalogTypoCandidateSubtype
                .TYP_TYPE_VARIANT ->
                CatalogTypoCandidateRecommendation
                    .IMPLEMENT_TYP_TYPE_RULE

            CatalogTypoCandidateSubtype
                .SAUCE_SOSSE_VARIANT ->
                CatalogTypoCandidateRecommendation
                    .IMPLEMENT_SAUCE_SOSSE_RULE

            CatalogTypoCandidateSubtype
                .PROBABLY_MISCLASSIFIED ->
                CatalogTypoCandidateRecommendation
                    .RECLASSIFY_DUPLICATE_REASON

            CatalogTypoCandidateSubtype
                .SINGLE_EDIT_TYPO,
            CatalogTypoCandidateSubtype
                .MULTI_EDIT_TYPO,
            CatalogTypoCandidateSubtype
                .MULTIPLE_TOKEN_DIFFERENCE,
            CatalogTypoCandidateSubtype
                .NUMERIC_VARIANT,
            CatalogTypoCandidateSubtype
                .UNCLASSIFIED ->
                CatalogTypoCandidateRecommendation
                    .MANUAL_REVIEW_REQUIRED
        }
    }

    private fun existingResolverWouldAccept(
        action: CatalogCanonicalizationAction,
        targetSourceIndex: Int?,
        sourceIndex: Int,
        normalizedReasons: List<String>,
        sourceCategory: String?,
        targetCategory: String?,
        sourceNumericTokens: List<String>,
        targetNumericTokens: List<String>,
        sourceTokens: List<String>,
        targetTokens: List<String>,
        tokenDifferences:
        List<CatalogTypoTokenDifference>
    ): Boolean {
        if (
            action != CatalogCanonicalizationAction.MERGE &&
            action != CatalogCanonicalizationAction.REVIEW
        ) {
            return false
        }

        if (
            targetSourceIndex == null ||
            targetSourceIndex == sourceIndex
        ) {
            return false
        }

        if (
            normalizedReasons.none {
                TYPO_MARKER in it
            }
        ) {
            return false
        }

        if (
            normalizedReasons.any { reason ->
                CONFLICTING_REASON_MARKERS.any {
                    it in reason
                }
            }
        ) {
            return false
        }

        if (
            normalizedReasons.any { reason ->
                TYPO_MARKER !in reason &&
                        ALLOWED_REASON_MARKERS.none {
                            it in reason
                        }
            }
        ) {
            return false
        }

        if (
            sourceCategory == null ||
            targetCategory == null ||
            !sourceCategory.equals(
                targetCategory,
                ignoreCase = true
            )
        ) {
            return false
        }

        if (
            sourceNumericTokens !=
            targetNumericTokens
        ) {
            return false
        }

        if (
            sourceTokens.size !=
            targetTokens.size
        ) {
            return false
        }

        val difference =
            tokenDifferences.singleOrNull()
                ?: return false

        if (
            difference.sourceLength <
            MINIMUM_TOKEN_LENGTH ||
            difference.targetLength <
            MINIMUM_TOKEN_LENGTH
        ) {
            return false
        }

        return difference
            .germanOrthographyEquivalent ||
                (
                        difference.editDistance == 1 &&
                                difference.lengthDifference <=
                                MAXIMUM_EXISTING_LENGTH_DIFFERENCE
                        )
    }

    private fun createTokenDifference(
        tokenIndex: Int,
        sourceToken: String,
        targetToken: String
    ): CatalogTypoTokenDifference {
        val normalizedSource =
            normalizeGermanOrthography(
                sourceToken
            )

        val normalizedTarget =
            normalizeGermanOrthography(
                targetToken
            )

        return CatalogTypoTokenDifference(
            tokenIndex = tokenIndex,
            sourceToken = sourceToken,
            targetToken = targetToken,

            normalizedSourceToken =
                normalizedSource,
            normalizedTargetToken =
                normalizedTarget,

            sourceLength =
                normalizedSource.length,
            targetLength =
                normalizedTarget.length,

            lengthDifference =
                abs(
                    normalizedSource.length -
                            normalizedTarget.length
                ),

            editDistance =
                damerauLevenshteinDistance(
                    normalizedSource,
                    normalizedTarget
                ),

            germanOrthographyEquivalent =
                normalizedSource ==
                        normalizedTarget,

            typTypeEquivalent =
                setOf(
                    sourceToken,
                    targetToken
                ).map {
                    it.lowercase(Locale.GERMAN)
                }.toSet() ==
                        setOf("typ", "type"),

            sauceSosseEquivalent =
                normalizeSauceSosse(sourceToken) ==
                        normalizeSauceSosse(targetToken) &&
                        !sourceToken.equals(
                            targetToken,
                            ignoreCase = true
                        )
        )
    }

    private fun normalizeComparisonText(
        value: String
    ): String =
        value
            .trim()
            .lowercase(Locale.GERMAN)
            .replace(
                OPTIONAL_CHARACTER_BRACKET_REGEX,
                "$1"
            )
            .replace(
                NON_LEXICAL_CHARACTER_REGEX,
                " "
            )
            .replace(
                MULTIPLE_WHITESPACE_REGEX,
                " "
            )
            .trim()

    private fun compactComparisonText(
        value: String
    ): String =
        normalizeGermanOrthography(value)
            .replace(
                NON_ALPHANUMERIC_REGEX,
                ""
            )

    private fun normalizeHyphenationText(
        value: String
    ): String =
        normalizeGermanOrthography(value)
            .replace(
                HYPHEN_OR_WHITESPACE_REGEX,
                ""
            )

    private fun normalizeGermanOrthography(
        value: String
    ): String =
        value
            .trim()
            .lowercase(Locale.GERMAN)
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")

    private fun normalizeSauceSosse(
        value: String
    ): String =
        normalizeGermanOrthography(value)
            .replace("sauce", "sosse")
            .replace("soße", "sosse")

    private fun tokenize(
        value: String
    ): List<String> =
        value
            .split(' ')
            .map(String::trim)
            .filter(String::isNotBlank)

    private fun extractNumericTokens(
        value: String
    ): List<String> =
        NUMBER_REGEX
            .findAll(value)
            .map { it.value }
            .toList()

    private fun normalizeReason(
        value: String
    ): String =
        value
            .trim()
            .uppercase(Locale.ROOT)
            .replace(
                MULTIPLE_WHITESPACE_REGEX,
                " "
            )

    private fun damerauLevenshteinDistance(
        left: String,
        right: String
    ): Int {
        if (left == right) {
            return 0
        }

        if (left.isEmpty()) {
            return right.length
        }

        if (right.isEmpty()) {
            return left.length
        }

        val rows =
            Array(left.length + 1) {
                IntArray(right.length + 1)
            }

        for (leftIndex in 0..left.length) {
            rows[leftIndex][0] =
                leftIndex
        }

        for (rightIndex in 0..right.length) {
            rows[0][rightIndex] =
                rightIndex
        }

        for (leftIndex in 1..left.length) {
            for (rightIndex in 1..right.length) {
                val substitutionCost =
                    if (
                        left[leftIndex - 1] ==
                        right[rightIndex - 1]
                    ) {
                        0
                    } else {
                        1
                    }

                rows[leftIndex][rightIndex] =
                    minOf(
                        rows[leftIndex - 1][
                            rightIndex
                        ] + 1,

                        rows[leftIndex][
                            rightIndex - 1
                        ] + 1,

                        rows[leftIndex - 1][
                            rightIndex - 1
                        ] + substitutionCost
                    )

                if (
                    leftIndex > 1 &&
                    rightIndex > 1 &&
                    left[leftIndex - 1] ==
                    right[rightIndex - 2] &&
                    left[leftIndex - 2] ==
                    right[rightIndex - 1]
                ) {
                    rows[leftIndex][rightIndex] =
                        minOf(
                            rows[leftIndex][
                                rightIndex
                            ],
                            rows[leftIndex - 2][
                                rightIndex - 2
                            ] + 1
                        )
                }
            }
        }

        return rows[left.length][right.length]
    }

    private companion object {

        val HYPHEN_REGEX =
            Regex("[-‐-‒–—−﹘﹣－]")

        const val UNCATEGORIZED =
            "<uncategorized>"

        const val TYPO_MARKER =
            "TYPO_VARIANT"

        const val MINIMUM_TOKEN_LENGTH =
            4

        const val MAXIMUM_EXISTING_RESOLVER_DISTANCE =
            1

        const val MAXIMUM_EXISTING_LENGTH_DIFFERENCE =
            1

        val OPTIONAL_CHARACTER_BRACKET_REGEX =
            Regex("\\(([\\p{L}\\p{M}])\\)")

        val NON_LEXICAL_CHARACTER_REGEX =
            Regex("[^\\p{L}\\p{M}\\p{N}]+")

        val NON_ALPHANUMERIC_REGEX =
            Regex("[^\\p{L}\\p{M}\\p{N}]+")

        val HYPHEN_OR_WHITESPACE_REGEX =
            Regex("[-‐-‒–—−﹘﹣－\\s]+")

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        val NUMBER_REGEX =
            Regex("\\d+(?:[.,]\\d+)?")

        val ALLOWED_REASON_MARKERS = setOf(
            "CANONICAL DUPLICATE TARGET",
            "DUPLICATE RECOMMENDATION: MERGE_AFTER_REVIEW",
            "DUPLICATE RECOMMENDATION: REVIEW",
            "ENTRY BELONGS TO DUPLICATE GROUP"
        )

        val CONFLICTING_REASON_MARKERS = setOf(
            "IDENTICAL_ITEM_NAME",
            "IDENTICAL_NORMALIZED_KEY",
            "NORMALIZED_NAME_MATCH",
            "SINGULAR_PLURAL_VARIANT",
            "WORD_ORDER_VARIANT",
            "PUNCTUATION_VARIANT",
            "COLOR_ORDER_VARIANT",
            "PREPARATION_VARIANT",
            "SALES_FORM_VARIANT",
            "BRAND_VARIANT",
            "POSSIBLE_SEMANTIC_DUPLICATE"
        )
    }
}