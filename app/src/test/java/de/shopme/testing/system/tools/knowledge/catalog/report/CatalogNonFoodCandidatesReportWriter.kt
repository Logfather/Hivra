package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.CatalogNonFoodCandidate
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.CatalogNonFoodReason
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.NonFoodRecommendation
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

class CatalogNonFoodCandidatesReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        candidates: List<CatalogNonFoodCandidate>,
        outputFile: File
    ) {
        require(outputFile.name.isNotBlank()) {
            "Non-food candidates report output file must have a name."
        }

        require(!outputFile.exists() || outputFile.isFile) {
            "Non-food candidates report output path is not a file: " +
                    outputFile.path
        }

        validateCandidates(candidates)

        val normalizedCandidates = candidates
            .map(::normalizeCandidate)
            .distinct()
            .sortedWith(CANDIDATE_COMPARATOR)

        val recommendationCounts = NonFoodRecommendation.entries
            .associateWith { recommendation ->
                normalizedCandidates.count {
                    it.recommendation == recommendation
                }
            }
            .toList()
            .sortedBy { (recommendation, _) ->
                recommendation.name
            }
            .associate { (recommendation, count) ->
                recommendation.name to count
            }

        val reasonCounts = CatalogNonFoodReason.entries
            .associateWith { reason ->
                normalizedCandidates.count { candidate ->
                    reason in candidate.reasons
                }
            }
            .filterValues { it > 0 }
            .toList()
            .sortedBy { (reason, _) ->
                reason.name
            }
            .associate { (reason, count) ->
                reason.name to count
            }

        val matchedTermCounts = normalizedCandidates
            .flatMap { candidate ->
                candidate.matchedTerms
            }
            .groupingBy { it }
            .eachCount()
            .toSortedMap()

        val categoryCounts = normalizedCandidates
            .groupingBy { candidate ->
                candidate.category
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: MISSING_CATEGORY_KEY
            }
            .eachCount()
            .toSortedMap()

        val confidenceBuckets = buildConfidenceBuckets(
            normalizedCandidates
        )

        val report = CatalogNonFoodCandidatesReport(
            version = CURRENT_VERSION,
            candidateCount = normalizedCandidates.size,
            automaticRemovalCount = normalizedCandidates.count {
                it.recommendation ==
                        NonFoodRecommendation.REMOVE_AUTOMATICALLY
            },
            removalAfterReviewCount = normalizedCandidates.count {
                it.recommendation ==
                        NonFoodRecommendation.REMOVE_AFTER_REVIEW
            },
            reviewCount = normalizedCandidates.count {
                it.recommendation == NonFoodRecommendation.REVIEW
            },
            keepCount = normalizedCandidates.count {
                it.recommendation == NonFoodRecommendation.KEEP
            },
            uniqueReasonCount = reasonCounts.size,
            uniqueMatchedTermCount = matchedTermCounts.size,
            affectedCategoryCount = categoryCounts.size,
            minimumConfidence = normalizedCandidates
                .minOfOrNull { it.confidence },
            maximumConfidence = normalizedCandidates
                .maxOfOrNull { it.confidence },
            averageConfidence = normalizedCandidates
                .takeIf(List<CatalogNonFoodCandidate>::isNotEmpty)
                ?.map { it.confidence }
                ?.average()
                ?.let(::normalizeConfidence),
            recommendationCounts = recommendationCounts,
            reasonCounts = reasonCounts,
            matchedTermCounts = matchedTermCounts,
            categoryCounts = categoryCounts,
            confidenceBuckets = confidenceBuckets,
            candidates = normalizedCandidates
        )

        validateReport(report)

        val outputDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        ) {
            "Non-food candidates report output file has no parent " +
                    "directory: ${outputFile.path}"
        }

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Failed to create non-food candidates report directory: " +
                        outputDirectory.path
            }
        }

        require(outputDirectory.isDirectory) {
            "Non-food candidates report parent is not a directory: " +
                    outputDirectory.path
        }

        val json = gson.toJson(report).trimEnd() +
                System.lineSeparator()

        writeAtomically(
            outputFile = outputFile,
            content = json
        )
    }

    private fun normalizeCandidate(
        candidate: CatalogNonFoodCandidate
    ): CatalogNonFoodCandidate =
        candidate.copy(
            itemName = candidate.itemName
                .trim()
                .replace(MULTIPLE_WHITESPACE_REGEX, " "),
            category = candidate.category
                ?.trim()
                ?.replace(MULTIPLE_WHITESPACE_REGEX, " ")
                ?.takeIf(String::isNotBlank),
            reasons = candidate.reasons
                .toSortedSet(compareBy { it.name }),
            matchedTerms = candidate.matchedTerms
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()
                .toCollection(linkedSetOf()),
            confidence = normalizeConfidence(
                candidate.confidence
            )
        )

    private fun validateCandidates(
        candidates: List<CatalogNonFoodCandidate>
    ) {
        val duplicateSourceIndices = candidates
            .groupBy { it.sourceIndex }
            .filterValues { it.size > 1 }
            .keys
            .sorted()

        require(duplicateSourceIndices.isEmpty()) {
            "Non-food candidates contain duplicate source indices: " +
                    duplicateSourceIndices.joinToString(", ")
        }

        candidates.forEach(::validateCandidate)
    }

    private fun validateCandidate(
        candidate: CatalogNonFoodCandidate
    ) {
        require(candidate.sourceIndex >= 0) {
            "Non-food candidate sourceIndex must not be negative."
        }

        require(candidate.itemName.isNotBlank()) {
            "Non-food candidate itemName must not be blank at sourceIndex " +
                    "${candidate.sourceIndex}."
        }

        require(
            candidate.category == null ||
                    candidate.category.isNotBlank()
        ) {
            "Non-food candidate category must not be blank at sourceIndex " +
                    "${candidate.sourceIndex}."
        }

        require(candidate.reasons.isNotEmpty()) {
            "Non-food candidate must contain at least one reason at " +
                    "sourceIndex ${candidate.sourceIndex}."
        }

        require(candidate.matchedTerms.isNotEmpty()) {
            "Non-food candidate must contain at least one matched term at " +
                    "sourceIndex ${candidate.sourceIndex}."
        }

        require(
            candidate.matchedTerms.none(String::isBlank)
        ) {
            "Non-food candidate matchedTerms must not contain blank values " +
                    "at sourceIndex ${candidate.sourceIndex}."
        }

        require(
            candidate.matchedTerms.size ==
                    candidate.matchedTerms.distinct().size
        ) {
            "Non-food candidate matchedTerms must not contain duplicates at " +
                    "sourceIndex ${candidate.sourceIndex}."
        }

        require(candidate.confidence.isFinite()) {
            "Non-food candidate confidence must be finite at sourceIndex " +
                    "${candidate.sourceIndex}."
        }

        require(
            candidate.confidence in
                    MINIMUM_CONFIDENCE..MAXIMUM_CONFIDENCE
        ) {
            "Non-food candidate confidence must be between " +
                    "$MINIMUM_CONFIDENCE and $MAXIMUM_CONFIDENCE at " +
                    "sourceIndex ${candidate.sourceIndex}, but was " +
                    "${candidate.confidence}."
        }

        validateRecommendationConsistency(candidate)
    }

    private fun validateRecommendationConsistency(
        candidate: CatalogNonFoodCandidate
    ) {
        when (candidate.recommendation) {
            NonFoodRecommendation.REMOVE_AUTOMATICALLY -> {
                require(
                    candidate.confidence >=
                            AUTOMATIC_REMOVAL_MINIMUM_CONFIDENCE
                ) {
                    "REMOVE_AUTOMATICALLY candidate at sourceIndex " +
                            "${candidate.sourceIndex} has confidence " +
                            "${candidate.confidence}, below required minimum " +
                            "$AUTOMATIC_REMOVAL_MINIMUM_CONFIDENCE."
                }
            }

            NonFoodRecommendation.REMOVE_AFTER_REVIEW -> {
                require(
                    candidate.confidence >=
                            REMOVE_AFTER_REVIEW_MINIMUM_CONFIDENCE
                ) {
                    "REMOVE_AFTER_REVIEW candidate at sourceIndex " +
                            "${candidate.sourceIndex} has confidence " +
                            "${candidate.confidence}, below required minimum " +
                            "$REMOVE_AFTER_REVIEW_MINIMUM_CONFIDENCE."
                }
            }

            NonFoodRecommendation.REVIEW -> {
                require(
                    candidate.confidence >=
                            REVIEW_MINIMUM_CONFIDENCE
                ) {
                    "REVIEW candidate at sourceIndex " +
                            "${candidate.sourceIndex} has confidence " +
                            "${candidate.confidence}, below required minimum " +
                            "$REVIEW_MINIMUM_CONFIDENCE."
                }
            }

            NonFoodRecommendation.KEEP -> Unit
        }
    }

    private fun buildConfidenceBuckets(
        candidates: List<CatalogNonFoodCandidate>
    ): Map<String, Int> {
        val counts = linkedMapOf(
            CONFIDENCE_BUCKET_0_49 to 0,
            CONFIDENCE_BUCKET_50_79 to 0,
            CONFIDENCE_BUCKET_80_94 to 0,
            CONFIDENCE_BUCKET_95_100 to 0
        )

        candidates.forEach { candidate ->
            val bucket = when {
                candidate.confidence >= 0.95 ->
                    CONFIDENCE_BUCKET_95_100

                candidate.confidence >= 0.80 ->
                    CONFIDENCE_BUCKET_80_94

                candidate.confidence >= 0.50 ->
                    CONFIDENCE_BUCKET_50_79

                else ->
                    CONFIDENCE_BUCKET_0_49
            }

            counts[bucket] = counts.getValue(bucket) + 1
        }

        return counts
    }

    private fun validateReport(
        report: CatalogNonFoodCandidatesReport
    ) {
        require(report.version > 0) {
            "Non-food candidates report version must be greater than zero."
        }

        require(report.candidateCount == report.candidates.size) {
            "candidateCount must equal candidates size."
        }

        require(
            report.automaticRemovalCount +
                    report.removalAfterReviewCount +
                    report.reviewCount +
                    report.keepCount ==
                    report.candidateCount
        ) {
            "Recommendation summary counts must sum to candidateCount."
        }

        require(
            report.recommendationCounts.values.sum() ==
                    report.candidateCount
        ) {
            "recommendationCounts must sum to candidateCount."
        }

        require(
            report.reasonCounts.values.sum() ==
                    report.candidates.sumOf { it.reasons.size }
        ) {
            "reasonCounts must equal the total number of candidate reasons."
        }

        require(
            report.matchedTermCounts.values.sum() ==
                    report.candidates.sumOf { it.matchedTerms.size }
        ) {
            "matchedTermCounts must equal the total number of matched terms."
        }

        require(
            report.categoryCounts.values.sum() ==
                    report.candidateCount
        ) {
            "categoryCounts must sum to candidateCount."
        }

        require(
            report.confidenceBuckets.values.sum() ==
                    report.candidateCount
        ) {
            "confidenceBuckets must sum to candidateCount."
        }

        require(
            report.uniqueReasonCount ==
                    report.reasonCounts.size
        ) {
            "uniqueReasonCount must equal reasonCounts size."
        }

        require(
            report.uniqueMatchedTermCount ==
                    report.matchedTermCounts.size
        ) {
            "uniqueMatchedTermCount must equal matchedTermCounts size."
        }

        require(
            report.affectedCategoryCount ==
                    report.categoryCounts.size
        ) {
            "affectedCategoryCount must equal categoryCounts size."
        }

        require(
            report.recommendationCounts.keys.toList() ==
                    report.recommendationCounts.keys.sorted()
        ) {
            "recommendationCounts must be sorted by key."
        }

        require(
            report.reasonCounts.keys.toList() ==
                    report.reasonCounts.keys.sorted()
        ) {
            "reasonCounts must be sorted by key."
        }

        require(
            report.matchedTermCounts.keys.toList() ==
                    report.matchedTermCounts.keys.sorted()
        ) {
            "matchedTermCounts must be sorted by key."
        }

        require(
            report.categoryCounts.keys.toList() ==
                    report.categoryCounts.keys.sorted()
        ) {
            "categoryCounts must be sorted by key."
        }

        require(
            report.confidenceBuckets.keys.toList() ==
                    listOf(
                        CONFIDENCE_BUCKET_0_49,
                        CONFIDENCE_BUCKET_50_79,
                        CONFIDENCE_BUCKET_80_94,
                        CONFIDENCE_BUCKET_95_100
                    )
        ) {
            "confidenceBuckets must use the canonical bucket order."
        }

        require(
            report.candidates ==
                    report.candidates.sortedWith(CANDIDATE_COMPARATOR)
        ) {
            "Non-food candidates must be deterministically sorted."
        }

        require(
            report.candidates.map { it.sourceIndex }.distinct().size ==
                    report.candidateCount
        ) {
            "Non-food candidates must have unique sourceIndex values."
        }

        if (report.candidateCount == 0) {
            require(report.minimumConfidence == null) {
                "minimumConfidence must be null for an empty report."
            }

            require(report.maximumConfidence == null) {
                "maximumConfidence must be null for an empty report."
            }

            require(report.averageConfidence == null) {
                "averageConfidence must be null for an empty report."
            }
        } else {
            requireNotNull(report.minimumConfidence) {
                "minimumConfidence must be present for a non-empty report."
            }

            requireNotNull(report.maximumConfidence) {
                "maximumConfidence must be present for a non-empty report."
            }

            requireNotNull(report.averageConfidence) {
                "averageConfidence must be present for a non-empty report."
            }

            require(
                report.minimumConfidence ==
                        report.candidates.minOf { it.confidence }
            ) {
                "minimumConfidence is inconsistent with candidates."
            }

            require(
                report.maximumConfidence ==
                        report.candidates.maxOf { it.confidence }
            ) {
                "maximumConfidence is inconsistent with candidates."
            }

            require(
                report.averageConfidence ==
                        normalizeConfidence(
                            report.candidates
                                .map { it.confidence }
                                .average()
                        )
            ) {
                "averageConfidence is inconsistent with candidates."
            }
        }
    }

    private fun writeAtomically(
        outputFile: File,
        content: String
    ) {
        val parentDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        )

        val temporaryFile = File(
            parentDirectory,
            ".${outputFile.name}.tmp"
        )

        try {
            temporaryFile.outputStream()
                .buffered()
                .use { output ->
                    output.write(
                        content.toByteArray(
                            StandardCharsets.UTF_8
                        )
                    )
                    output.flush()
                }

            try {
                Files.move(
                    temporaryFile.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporaryFile.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } finally {
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }
        }
    }

    private fun normalizeConfidence(
        value: Double
    ): Double =
        String.format(
            Locale.ROOT,
            CONFIDENCE_FORMAT,
            value
        ).toDouble()

    private data class CatalogNonFoodCandidatesReport(
        val version: Int,
        val candidateCount: Int,
        val automaticRemovalCount: Int,
        val removalAfterReviewCount: Int,
        val reviewCount: Int,
        val keepCount: Int,
        val uniqueReasonCount: Int,
        val uniqueMatchedTermCount: Int,
        val affectedCategoryCount: Int,
        val minimumConfidence: Double?,
        val maximumConfidence: Double?,
        val averageConfidence: Double?,
        val recommendationCounts: Map<String, Int>,
        val reasonCounts: Map<String, Int>,
        val matchedTermCounts: Map<String, Int>,
        val categoryCounts: Map<String, Int>,
        val confidenceBuckets: Map<String, Int>,
        val candidates: List<CatalogNonFoodCandidate>
    )

    private companion object {

        const val CURRENT_VERSION = 1

        const val MINIMUM_CONFIDENCE = 0.0
        const val MAXIMUM_CONFIDENCE = 1.0

        const val AUTOMATIC_REMOVAL_MINIMUM_CONFIDENCE = 0.95
        const val REMOVE_AFTER_REVIEW_MINIMUM_CONFIDENCE = 0.80
        const val REVIEW_MINIMUM_CONFIDENCE = 0.50

        const val CONFIDENCE_FORMAT = "%.6f"

        const val MISSING_CATEGORY_KEY = "<missing>"

        const val CONFIDENCE_BUCKET_0_49 = "0.00-0.49"
        const val CONFIDENCE_BUCKET_50_79 = "0.50-0.79"
        const val CONFIDENCE_BUCKET_80_94 = "0.80-0.94"
        const val CONFIDENCE_BUCKET_95_100 = "0.95-1.00"

        val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")

        val CANDIDATE_COMPARATOR =
            compareBy<CatalogNonFoodCandidate>(
                {
                    recommendationRank(
                        it.recommendation
                    )
                },
                {
                    -it.confidence
                },
                {
                    it.itemName.lowercase(
                        Locale.ROOT
                    )
                },
                {
                    it.category
                        ?.lowercase(Locale.ROOT)
                        ?: ""
                },
                {
                    it.sourceIndex
                }
            )

        fun recommendationRank(
            recommendation: NonFoodRecommendation
        ): Int =
            when (recommendation) {
                NonFoodRecommendation.REMOVE_AUTOMATICALLY -> 0
                NonFoodRecommendation.REMOVE_AFTER_REVIEW -> 1
                NonFoodRecommendation.REVIEW -> 2
                NonFoodRecommendation.KEEP -> 3
            }

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
    }
}