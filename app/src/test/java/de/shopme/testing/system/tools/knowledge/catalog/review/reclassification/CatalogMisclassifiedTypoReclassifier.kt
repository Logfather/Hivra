package de.shopme.testing.system.tools.knowledge.catalog.review.reclassification

import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogRemainingTypoDiagnosticsResult
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogTypoCandidateDiagnosticEntry
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogTypoCandidateSubtype
import java.util.Locale

class CatalogMisclassifiedTypoReclassifier {

    fun reclassify(
        diagnosticsResult:
        CatalogRemainingTypoDiagnosticsResult
    ): CatalogMisclassifiedTypoReclassificationResult {
        require(diagnosticsResult.valid) {
            "Typo diagnostics must be valid."
        }

        val candidates =
            diagnosticsResult.entries
                .filter {
                    it.subtype ==
                            CatalogTypoCandidateSubtype
                                .PROBABLY_MISCLASSIFIED
                }
                .sortedBy { it.sourceIndex }

        val entries =
            candidates.map(::reclassifyEntry)

        val countsByClass =
            entries
                .groupingBy {
                    it.reclassifiedAs
                }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByRecommendation =
            entries
                .groupingBy {
                    it.recommendation
                }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsBySourceCategory =
            entries
                .groupingBy {
                    it.sourceCategory
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?: UNCATEGORIZED
                }
                .eachCount()
                .toSortedMap()

        return CatalogMisclassifiedTypoReclassificationResult(
            version =
                CatalogMisclassifiedTypoReclassificationResult
                    .CURRENT_VERSION,

            inputCandidateCount =
                candidates.size,

            reclassifiedEntryCount =
                entries.size,

            preserveSeparateFoodCount =
                entries.count {
                    it.recommendation ==
                            CatalogMisclassifiedTypoRecommendation
                                .PRESERVE_AS_SEPARATE_FOODS
                },

            manualReviewRequiredCount =
                entries.count {
                    it.recommendation ==
                            CatalogMisclassifiedTypoRecommendation
                                .MANUAL_REVIEW_REQUIRED
                },

            countsByClass =
                countsByClass,

            countsByRecommendation =
                countsByRecommendation,

            countsBySourceCategory =
                countsBySourceCategory,

            entries =
                entries,

            valid = true
        )
    }

    private fun reclassifyEntry(
        entry: CatalogTypoCandidateDiagnosticEntry
    ): CatalogMisclassifiedTypoEntry {
        val sourceText =
            normalize(entry.sourceName)

        val targetText =
            normalize(entry.targetName.orEmpty())

        val sourceTokens =
            tokenize(sourceText)

        val targetTokens =
            tokenize(targetText)

        val matchedMarkers =
            detectMarkers(
                sourceTokens = sourceTokens,
                targetTokens = targetTokens
            )

        val classification =
            determineClassification(
                sourceText = sourceText,
                targetText = targetText,
                sourceTokens = sourceTokens,
                targetTokens = targetTokens,
                matchedMarkers = matchedMarkers
            )

        val recommendation =
            determineRecommendation(
                classification = classification,
                sourceCategory = entry.sourceCategory,
                targetCategory = entry.targetCategory
            )

        return CatalogMisclassifiedTypoEntry(
            sourceIndex = entry.sourceIndex,
            sourceName = entry.sourceName,
            sourceCategory =
                entry.sourceCategory,

            targetSourceIndex =
                requireNotNull(
                    entry.targetSourceIndex
                ) {
                    "Misclassified typo entry " +
                            "${entry.sourceIndex} has no target."
                },

            targetName =
                requireNotNull(
                    entry.targetName
                ) {
                    "Misclassified typo entry " +
                            "${entry.sourceIndex} has no target name."
                },

            targetCategory =
                entry.targetCategory,

            originalSubtype =
                entry.subtype.name,

            reclassifiedAs =
                classification,

            recommendation =
                recommendation,

            matchedMarkers =
                matchedMarkers,

            reasons =
                buildReasons(
                    classification = classification,
                    sourceCategory =
                        entry.sourceCategory,
                    targetCategory =
                        entry.targetCategory,
                    matchedMarkers =
                        matchedMarkers
                )
        )
    }

    private fun determineClassification(
        sourceText: String,
        targetText: String,
        sourceTokens: Set<String>,
        targetTokens: Set<String>,
        matchedMarkers: List<String>
    ): CatalogMisclassifiedTypoClass {
        val sourceOnlyTokens =
            sourceTokens - targetTokens

        val targetOnlyTokens =
            targetTokens - sourceTokens

        val allDifferentTokens =
            sourceOnlyTokens + targetOnlyTokens

        if (
            allDifferentTokens.any {
                it in FROZEN_MARKERS
            }
        ) {
            return CatalogMisclassifiedTypoClass
                .FROZEN_FORM_VARIANT
        }

        if (
            allDifferentTokens.any {
                it in BIO_MARKERS
            }
        ) {
            return CatalogMisclassifiedTypoClass
                .BIO_ATTRIBUTE_VARIANT
        }

        if (
            containsCannedMarker(sourceText) ||
            containsCannedMarker(targetText)
        ) {
            return if (
                sourceTokens.containsAll(targetTokens) ||
                targetTokens.containsAll(sourceTokens)
            ) {
                CatalogMisclassifiedTypoClass
                    .CANNED_FORM_VARIANT
            } else {
                CatalogMisclassifiedTypoClass
                    .SALES_FORM_VARIANT
            }
        }

        if (
            matchedMarkers.any {
                it in PRESERVATION_MARKERS
            }
        ) {
            return CatalogMisclassifiedTypoClass
                .PRESERVATION_FORM_VARIANT
        }

        if (
            sourceText != targetText &&
            sourceTokens != targetTokens
        ) {
            return CatalogMisclassifiedTypoClass
                .SEMANTIC_PRODUCT_VARIANT
        }

        return CatalogMisclassifiedTypoClass
            .UNCLASSIFIED
    }

    private fun determineRecommendation(
        classification:
        CatalogMisclassifiedTypoClass,

        sourceCategory: String?,
        targetCategory: String?
    ): CatalogMisclassifiedTypoRecommendation {
        val categoriesDiffer =
            !sourceCategory
                .orEmpty()
                .trim()
                .equals(
                    targetCategory
                        .orEmpty()
                        .trim(),
                    ignoreCase = true
                )

        return when (classification) {
            CatalogMisclassifiedTypoClass
                .FROZEN_FORM_VARIANT,

            CatalogMisclassifiedTypoClass
                .CANNED_FORM_VARIANT,

            CatalogMisclassifiedTypoClass
                .BIO_ATTRIBUTE_VARIANT,

            CatalogMisclassifiedTypoClass
                .SALES_FORM_VARIANT,

            CatalogMisclassifiedTypoClass
                .PRESERVATION_FORM_VARIANT ->
                CatalogMisclassifiedTypoRecommendation
                    .PRESERVE_AS_SEPARATE_FOODS

            CatalogMisclassifiedTypoClass
                .SEMANTIC_PRODUCT_VARIANT ->
                if (categoriesDiffer) {
                    CatalogMisclassifiedTypoRecommendation
                        .PRESERVE_AS_SEPARATE_FOODS
                } else {
                    CatalogMisclassifiedTypoRecommendation
                        .MANUAL_REVIEW_REQUIRED
                }

            CatalogMisclassifiedTypoClass
                .UNCLASSIFIED ->
                CatalogMisclassifiedTypoRecommendation
                    .MANUAL_REVIEW_REQUIRED
        }
    }

    private fun detectMarkers(
        sourceTokens: Set<String>,
        targetTokens: Set<String>
    ): List<String> =
        (sourceTokens + targetTokens)
            .filter {
                it in ALL_MARKERS
            }
            .distinct()
            .sorted()

    private fun buildReasons(
        classification:
        CatalogMisclassifiedTypoClass,

        sourceCategory: String?,
        targetCategory: String?,

        matchedMarkers: List<String>
    ): List<String> =
        buildList {
            add(
                "Original TYPO_VARIANT classification was replaced by " +
                        "${classification.name}."
            )

            if (matchedMarkers.isNotEmpty()) {
                add(
                    "Detected identity-relevant markers: " +
                            matchedMarkers.joinToString(", ") +
                            "."
                )
            }

            if (
                !sourceCategory
                    .orEmpty()
                    .trim()
                    .equals(
                        targetCategory
                            .orEmpty()
                            .trim(),
                        ignoreCase = true
                    )
            ) {
                add(
                    "Source and target categories differ: " +
                            "'${sourceCategory.orEmpty()}' versus " +
                            "'${targetCategory.orEmpty()}'."
                )
            }

            when (classification) {
                CatalogMisclassifiedTypoClass
                    .FROZEN_FORM_VARIANT ->
                    add(
                        "Frozen-state markers are identity-relevant and " +
                                "must not be treated as typographical noise."
                    )

                CatalogMisclassifiedTypoClass
                    .CANNED_FORM_VARIANT ->
                    add(
                        "Canned or preserved form is an identity-relevant " +
                                "product attribute."
                    )

                CatalogMisclassifiedTypoClass
                    .BIO_ATTRIBUTE_VARIANT ->
                    add(
                        "Bio is a production or quality attribute and not " +
                                "a typographical variant."
                    )

                CatalogMisclassifiedTypoClass
                    .SALES_FORM_VARIANT ->
                    add(
                        "The entries represent different sales or packaging " +
                                "forms."
                    )

                CatalogMisclassifiedTypoClass
                    .PRESERVATION_FORM_VARIANT ->
                    add(
                        "The entries differ by preservation or preparation " +
                                "form."
                    )

                CatalogMisclassifiedTypoClass
                    .SEMANTIC_PRODUCT_VARIANT ->
                    add(
                        "The differing tokens describe a semantic product " +
                                "difference."
                    )

                CatalogMisclassifiedTypoClass
                    .UNCLASSIFIED ->
                    add(
                        "The available evidence is insufficient for an " +
                                "automatic domain classification."
                    )
            }
        }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    private fun containsCannedMarker(
        text: String
    ): Boolean =
        CANNED_PHRASES.any {
            it in text
        } ||
                tokenize(text).any {
                    it in CANNED_MARKERS
                }

    private fun normalize(
        value: String
    ): String =
        value
            .trim()
            .lowercase(Locale.GERMAN)
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")
            .replace(NON_LEXICAL_REGEX, " ")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

    private fun tokenize(
        value: String
    ): Set<String> =
        normalize(value)
            .split(' ')
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSortedSet()

    private companion object {

        const val UNCATEGORIZED =
            "<uncategorized>"

        val NON_LEXICAL_REGEX =
            Regex("[^\\p{L}\\p{M}\\p{N}]+")

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        val FROZEN_MARKERS = setOf(
            "tk",
            "tiefkuehl",
            "tiefgekuehlt",
            "gefroren"
        )

        val BIO_MARKERS = setOf(
            "bio",
            "oekologisch",
            "organic"
        )

        val CANNED_MARKERS = setOf(
            "dose",
            "dosen",
            "konserve",
            "konserviert"
        )

        val CANNED_PHRASES = setOf(
            "in dose",
            "in dosen",
            "aus der dose"
        )

        val PRESERVATION_MARKERS = setOf(
            "konserviert",
            "eingelegt",
            "getrocknet",
            "geraeuchert",
            "pasteurisiert"
        )

        val ALL_MARKERS =
            FROZEN_MARKERS +
                    BIO_MARKERS +
                    CANNED_MARKERS +
                    PRESERVATION_MARKERS
    }
}