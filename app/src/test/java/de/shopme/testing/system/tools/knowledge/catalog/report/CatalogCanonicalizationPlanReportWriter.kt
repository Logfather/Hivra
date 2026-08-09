package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlan
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlanEntry
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

class CatalogCanonicalizationPlanReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        plan: CatalogCanonicalizationPlan,
        outputFile: File
    ) {
        require(outputFile.name.isNotBlank()) {
            "Canonicalization-plan report output file must have a name."
        }

        require(!outputFile.exists() || outputFile.isFile) {
            "Canonicalization-plan report output path is not a file: " +
                    outputFile.path
        }

        validatePlan(plan)

        val normalizedEntries = plan.entries
            .map(::normalizeEntry)
            .sortedWith(ENTRY_COMPARATOR)

        val actionCounts = CatalogCanonicalizationAction.entries
            .associateWith { action ->
                normalizedEntries.count { it.action == action }
            }
            .filterValues { it > 0 }
            .toList()
            .sortedBy { (action, _) -> action.name }
            .associate { (action, count) ->
                action.name to count
            }

        val automaticActionCounts = normalizedEntries
            .filter { it.automatic }
            .groupingBy { it.action.name }
            .eachCount()
            .toSortedMap()

        val reviewRequiredActionCounts = normalizedEntries
            .filterNot { it.automatic }
            .groupingBy { it.action.name }
            .eachCount()
            .toSortedMap()

        val confidenceBuckets = buildConfidenceBuckets(
            normalizedEntries
        )

        val affectedSourceIndices = normalizedEntries
            .asSequence()
            .filter {
                it.action != CatalogCanonicalizationAction.KEEP
            }
            .map { it.sourceIndex }
            .sorted()
            .toList()

        val mergeEntries = normalizedEntries
            .filter {
                it.action == CatalogCanonicalizationAction.MERGE
            }

        val mergeTargetCounts = mergeEntries
            .map { entry ->
                requireNotNull(entry.mergeTargetSourceIndex) {
                    "MERGE entry must contain mergeTargetSourceIndex at " +
                            "sourceIndex ${entry.sourceIndex}."
                }
            }
            .groupingBy(Int::toString)
            .eachCount()
            .toSortedMap(compareBy(String::toInt))

        val proposedCategoryCounts = normalizedEntries
            .mapNotNull { entry ->
                entry.proposedCategory
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
            }
            .groupingBy { it }
            .eachCount()
            .toSortedMap()

        val report = CatalogCanonicalizationPlanReport(
            version = plan.version,
            inputEntryCount = plan.inputEntryCount,
            planEntryCount = normalizedEntries.size,
            affectedEntryCount = affectedSourceIndices.size,
            unchangedEntryCount = normalizedEntries.count {
                it.action == CatalogCanonicalizationAction.KEEP
            },
            automaticActionCount = normalizedEntries.count {
                it.automatic
            },
            manualReviewActionCount = normalizedEntries.count {
                !it.automatic
            },
            explicitReviewActionCount = normalizedEntries.count {
                it.action == CatalogCanonicalizationAction.REVIEW
            },
            mergeActionCount = mergeEntries.size,
            removalActionCount = normalizedEntries.count {
                it.action ==
                        CatalogCanonicalizationAction.REMOVE_NON_FOOD
            },
            splitActionCount = normalizedEntries.count {
                it.action == CatalogCanonicalizationAction.SPLIT
            },
            renameActionCount = normalizedEntries.count {
                it.action == CatalogCanonicalizationAction.RENAME
            },
            normalizeActionCount = normalizedEntries.count {
                it.action == CatalogCanonicalizationAction.NORMALIZE
            },
            moveCategoryActionCount = normalizedEntries.count {
                it.action ==
                        CatalogCanonicalizationAction.MOVE_CATEGORY
            },
            minimumConfidence = normalizedEntries
                .minOfOrNull { it.confidence },
            maximumConfidence = normalizedEntries
                .maxOfOrNull { it.confidence },
            averageConfidence = normalizedEntries
                .takeIf(List<CatalogCanonicalizationPlanEntry>::isNotEmpty)
                ?.map { it.confidence }
                ?.average()
                ?.let(::normalizeConfidence),
            actionCounts = actionCounts,
            automaticActionCounts = automaticActionCounts,
            reviewRequiredActionCounts = reviewRequiredActionCounts,
            confidenceBuckets = confidenceBuckets,
            mergeTargetCounts = mergeTargetCounts,
            proposedCategoryCounts = proposedCategoryCounts,
            affectedSourceIndices = affectedSourceIndices,
            entries = normalizedEntries,
            valid = plan.valid
        )

        validateReport(report)

        val outputDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        ) {
            "Canonicalization-plan report output file has no parent " +
                    "directory: ${outputFile.path}"
        }

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Failed to create canonicalization-plan report directory: " +
                        outputDirectory.path
            }
        }

        require(outputDirectory.isDirectory) {
            "Canonicalization-plan report parent is not a directory: " +
                    outputDirectory.path
        }

        val json = gson.toJson(report).trimEnd() +
                System.lineSeparator()

        writeAtomically(
            outputFile = outputFile,
            content = json
        )
    }

    private fun normalizeEntry(
        entry: CatalogCanonicalizationPlanEntry
    ): CatalogCanonicalizationPlanEntry =
        entry.copy(
            originalItemName = normalizeText(entry.originalItemName),
            proposedCanonicalName = entry.proposedCanonicalName
                ?.let(::normalizeText)
                ?.takeIf(String::isNotBlank),
            proposedNormalizedKey = entry.proposedNormalizedKey
                ?.trim()
                ?.takeIf(String::isNotBlank),
            proposedCategory = entry.proposedCategory
                ?.trim()
                ?.takeIf(String::isNotBlank),
            reasons = entry.reasons
                .map(::normalizeText)
                .filter(String::isNotBlank)
                .distinct()
                .sorted(),
            confidence = normalizeConfidence(entry.confidence)
        )

    private fun validatePlan(
        plan: CatalogCanonicalizationPlan
    ) {
        require(plan.version > 0) {
            "Canonicalization plan version must be greater than zero."
        }

        require(plan.inputEntryCount >= 0) {
            "inputEntryCount must not be negative."
        }

        require(plan.planEntryCount >= 0) {
            "planEntryCount must not be negative."
        }

        require(plan.automaticActionCount >= 0) {
            "automaticActionCount must not be negative."
        }

        require(plan.reviewActionCount >= 0) {
            "reviewActionCount must not be negative."
        }

        require(plan.unchangedEntryCount >= 0) {
            "unchangedEntryCount must not be negative."
        }

        require(plan.planEntryCount == plan.entries.size) {
            "planEntryCount must equal entries size."
        }

        require(plan.planEntryCount == plan.inputEntryCount) {
            "Every input catalog entry must have exactly one plan entry."
        }

        require(
            plan.entries.map { it.sourceIndex }.distinct().size ==
                    plan.entries.size
        ) {
            "Canonicalization plan contains duplicate sourceIndex values."
        }

        require(
            plan.automaticActionCount ==
                    plan.entries.count { it.automatic }
        ) {
            "automaticActionCount is inconsistent with entries."
        }

        require(
            plan.reviewActionCount ==
                    plan.entries.count {
                        it.action ==
                                CatalogCanonicalizationAction.REVIEW
                    }
        ) {
            "reviewActionCount is inconsistent with REVIEW entries."
        }

        require(
            plan.unchangedEntryCount ==
                    plan.entries.count {
                        it.action ==
                                CatalogCanonicalizationAction.KEEP
                    }
        ) {
            "unchangedEntryCount is inconsistent with KEEP entries."
        }

        require(
            plan.actionCounts.values.sum() ==
                    plan.planEntryCount
        ) {
            "Sum of plan actionCounts must equal planEntryCount."
        }

        require(
            plan.affectedSourceIndices ==
                    plan.entries
                        .filter {
                            it.action !=
                                    CatalogCanonicalizationAction.KEEP
                        }
                        .map { it.sourceIndex }
                        .sorted()
        ) {
            "affectedSourceIndices is inconsistent with non-KEEP entries."
        }

        plan.entries.forEach(::validateEntry)
    }

    private fun validateEntry(
        entry: CatalogCanonicalizationPlanEntry
    ) {
        require(entry.sourceIndex >= 0) {
            "Canonicalization plan entry sourceIndex must not be negative."
        }

        require(entry.originalItemName.isNotBlank()) {
            "Canonicalization plan entry originalItemName must not be blank " +
                    "at sourceIndex ${entry.sourceIndex}."
        }

        require(entry.confidence.isFinite()) {
            "Canonicalization plan entry confidence must be finite at " +
                    "sourceIndex ${entry.sourceIndex}."
        }

        require(
            entry.confidence in
                    MINIMUM_CONFIDENCE..MAXIMUM_CONFIDENCE
        ) {
            "Canonicalization plan entry confidence must be between " +
                    "$MINIMUM_CONFIDENCE and $MAXIMUM_CONFIDENCE at " +
                    "sourceIndex ${entry.sourceIndex}, but was " +
                    "${entry.confidence}."
        }

        require(entry.reasons.isNotEmpty()) {
            "Canonicalization plan entry must contain at least one reason " +
                    "at sourceIndex ${entry.sourceIndex}."
        }

        require(entry.reasons.none(String::isBlank)) {
            "Canonicalization plan entry reasons must not contain blank " +
                    "values at sourceIndex ${entry.sourceIndex}."
        }

        require(
            entry.reasons.distinct().size ==
                    entry.reasons.size
        ) {
            "Canonicalization plan entry reasons must not contain " +
                    "duplicates at sourceIndex ${entry.sourceIndex}."
        }

        when (entry.action) {
            CatalogCanonicalizationAction.KEEP -> {
                require(entry.mergeTargetSourceIndex == null) {
                    "KEEP entry must not contain mergeTargetSourceIndex at " +
                            "sourceIndex ${entry.sourceIndex}."
                }
            }

            CatalogCanonicalizationAction.MERGE -> {
                requireNotNull(entry.mergeTargetSourceIndex) {
                    "MERGE entry must contain mergeTargetSourceIndex at " +
                            "sourceIndex ${entry.sourceIndex}."
                }

                require(
                    entry.mergeTargetSourceIndex != entry.sourceIndex
                ) {
                    "MERGE entry must not target itself at sourceIndex " +
                            "${entry.sourceIndex}."
                }
            }

            CatalogCanonicalizationAction.REMOVE_NON_FOOD -> {
                require(entry.mergeTargetSourceIndex == null) {
                    "REMOVE_NON_FOOD entry must not contain a merge target " +
                            "at sourceIndex ${entry.sourceIndex}."
                }
            }

            CatalogCanonicalizationAction.NORMALIZE,
            CatalogCanonicalizationAction.RENAME -> {
                require(
                    !entry.proposedCanonicalName.isNullOrBlank()
                ) {
                    "${entry.action.name} entry must contain " +
                            "proposedCanonicalName at sourceIndex " +
                            "${entry.sourceIndex}."
                }

                require(
                    !entry.proposedNormalizedKey.isNullOrBlank()
                ) {
                    "${entry.action.name} entry must contain " +
                            "proposedNormalizedKey at sourceIndex " +
                            "${entry.sourceIndex}."
                }
            }

            CatalogCanonicalizationAction.MOVE_CATEGORY -> {
                require(
                    !entry.proposedCategory.isNullOrBlank()
                ) {
                    "MOVE_CATEGORY entry must contain proposedCategory at " +
                            "sourceIndex ${entry.sourceIndex}."
                }
            }

            CatalogCanonicalizationAction.SPLIT,
            CatalogCanonicalizationAction.REVIEW -> Unit
        }
    }

    private fun validateReport(
        report: CatalogCanonicalizationPlanReport
    ) {
        require(report.version > 0) {
            "Canonicalization-plan report version must be greater than zero."
        }

        require(
            report.planEntryCount ==
                    report.entries.size
        ) {
            "planEntryCount must equal entries size."
        }

        require(
            report.planEntryCount ==
                    report.inputEntryCount
        ) {
            "planEntryCount must equal inputEntryCount."
        }

        require(
            report.affectedEntryCount ==
                    report.affectedSourceIndices.size
        ) {
            "affectedEntryCount must equal affectedSourceIndices size."
        }

        require(
            report.unchangedEntryCount +
                    report.affectedEntryCount ==
                    report.planEntryCount
        ) {
            "unchangedEntryCount plus affectedEntryCount must equal " +
                    "planEntryCount."
        }

        require(
            report.automaticActionCount +
                    report.manualReviewActionCount ==
                    report.planEntryCount
        ) {
            "Automatic and manual-review counts must sum to planEntryCount."
        }

        require(
            report.explicitReviewActionCount ==
                    report.entries.count {
                        it.action ==
                                CatalogCanonicalizationAction.REVIEW
                    }
        ) {
            "explicitReviewActionCount is inconsistent."
        }

        require(
            report.actionCounts.values.sum() ==
                    report.planEntryCount
        ) {
            "actionCounts must sum to planEntryCount."
        }

        require(
            report.automaticActionCounts.values.sum() ==
                    report.automaticActionCount
        ) {
            "automaticActionCounts must sum to automaticActionCount."
        }

        require(
            report.reviewRequiredActionCounts.values.sum() ==
                    report.manualReviewActionCount
        ) {
            "reviewRequiredActionCounts must sum to " +
                    "manualReviewActionCount."
        }

        require(
            report.confidenceBuckets.values.sum() ==
                    report.planEntryCount
        ) {
            "confidenceBuckets must sum to planEntryCount."
        }

        require(
            report.mergeTargetCounts.values.sum() ==
                    report.mergeActionCount
        ) {
            "mergeTargetCounts must sum to mergeActionCount."
        }

        val expectedMergeTargetCounts = report.entries
            .filter {
                it.action == CatalogCanonicalizationAction.MERGE
            }
            .map { entry ->
                requireNotNull(entry.mergeTargetSourceIndex) {
                    "MERGE report entry must contain mergeTargetSourceIndex " +
                            "at sourceIndex ${entry.sourceIndex}."
                }
            }
            .groupingBy(Int::toString)
            .eachCount()
            .toSortedMap(compareBy(String::toInt))

        require(
            report.mergeTargetCounts == expectedMergeTargetCounts
        ) {
            "mergeTargetCounts is inconsistent with MERGE entries."
        }

        require(
            report.mergeTargetCounts.keys.all { key ->
                key.toIntOrNull() != null
            }
        ) {
            "mergeTargetCounts keys must contain integer source indices."
        }

        require(
            report.proposedCategoryCounts.values.sum() ==
                    report.entries.count {
                        !it.proposedCategory.isNullOrBlank()
                    }
        ) {
            "proposedCategoryCounts is inconsistent with entries."
        }

        require(
            report.actionCounts.keys.toList() ==
                    report.actionCounts.keys.sorted()
        ) {
            "actionCounts must be sorted by key."
        }

        require(
            report.automaticActionCounts.keys.toList() ==
                    report.automaticActionCounts.keys.sorted()
        ) {
            "automaticActionCounts must be sorted by key."
        }

        require(
            report.reviewRequiredActionCounts.keys.toList() ==
                    report.reviewRequiredActionCounts.keys.sorted()
        ) {
            "reviewRequiredActionCounts must be sorted by key."
        }

        require(
            report.proposedCategoryCounts.keys.toList() ==
                    report.proposedCategoryCounts.keys.sorted()
        ) {
            "proposedCategoryCounts must be sorted by key."
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
            "confidenceBuckets must use the canonical order."
        }

        require(
            report.affectedSourceIndices ==
                    report.affectedSourceIndices.sorted()
        ) {
            "affectedSourceIndices must be sorted."
        }

        require(
            report.affectedSourceIndices.distinct().size ==
                    report.affectedSourceIndices.size
        ) {
            "affectedSourceIndices must not contain duplicates."
        }

        require(
            report.entries ==
                    report.entries.sortedWith(ENTRY_COMPARATOR)
        ) {
            "Canonicalization plan entries must be deterministically sorted."
        }

        require(
            report.entries.map { it.sourceIndex }.distinct().size ==
                    report.planEntryCount
        ) {
            "Canonicalization plan entries must have unique sourceIndex " +
                    "values."
        }

        if (report.entries.isEmpty()) {
            require(report.minimumConfidence == null)
            require(report.maximumConfidence == null)
            require(report.averageConfidence == null)
        } else {
            requireNotNull(report.minimumConfidence)
            requireNotNull(report.maximumConfidence)
            requireNotNull(report.averageConfidence)

            require(
                report.minimumConfidence ==
                        report.entries.minOf { it.confidence }
            ) {
                "minimumConfidence is inconsistent."
            }

            require(
                report.maximumConfidence ==
                        report.entries.maxOf { it.confidence }
            ) {
                "maximumConfidence is inconsistent."
            }

            require(
                report.averageConfidence ==
                        normalizeConfidence(
                            report.entries
                                .map { it.confidence }
                                .average()
                        )
            ) {
                "averageConfidence is inconsistent."
            }
        }

        require(
            report.valid ==
                    (
                            report.planEntryCount ==
                                    report.inputEntryCount &&
                                    report.entries
                                        .map { it.sourceIndex }
                                        .distinct()
                                        .size ==
                                    report.inputEntryCount
                            )
        ) {
            "Canonicalization-plan report valid flag is inconsistent."
        }
    }

    private fun buildConfidenceBuckets(
        entries: List<CatalogCanonicalizationPlanEntry>
    ): Map<String, Int> {
        val counts = linkedMapOf(
            CONFIDENCE_BUCKET_0_49 to 0,
            CONFIDENCE_BUCKET_50_79 to 0,
            CONFIDENCE_BUCKET_80_94 to 0,
            CONFIDENCE_BUCKET_95_100 to 0
        )

        entries.forEach { entry ->
            val bucket = when {
                entry.confidence >= 0.95 ->
                    CONFIDENCE_BUCKET_95_100

                entry.confidence >= 0.80 ->
                    CONFIDENCE_BUCKET_80_94

                entry.confidence >= 0.50 ->
                    CONFIDENCE_BUCKET_50_79

                else ->
                    CONFIDENCE_BUCKET_0_49
            }

            counts[bucket] = counts.getValue(bucket) + 1
        }

        return counts
    }

    private fun normalizeText(
        value: String
    ): String =
        value
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")

    private fun normalizeConfidence(
        value: Double
    ): Double =
        String.format(
            Locale.ROOT,
            CONFIDENCE_FORMAT,
            value
        ).toDouble()

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

    private data class CatalogCanonicalizationPlanReport(
        val version: Int,
        val inputEntryCount: Int,
        val planEntryCount: Int,
        val affectedEntryCount: Int,
        val unchangedEntryCount: Int,
        val automaticActionCount: Int,
        val manualReviewActionCount: Int,
        val explicitReviewActionCount: Int,
        val mergeActionCount: Int,
        val removalActionCount: Int,
        val splitActionCount: Int,
        val renameActionCount: Int,
        val normalizeActionCount: Int,
        val moveCategoryActionCount: Int,
        val minimumConfidence: Double?,
        val maximumConfidence: Double?,
        val averageConfidence: Double?,
        val actionCounts: Map<String, Int>,
        val automaticActionCounts: Map<String, Int>,
        val reviewRequiredActionCounts: Map<String, Int>,
        val confidenceBuckets: Map<String, Int>,
        val mergeTargetCounts: Map<String, Int>,
        val proposedCategoryCounts: Map<String, Int>,
        val affectedSourceIndices: List<Int>,
        val entries: List<CatalogCanonicalizationPlanEntry>,
        val valid: Boolean
    )

    private companion object {

        const val MINIMUM_CONFIDENCE = 0.0
        const val MAXIMUM_CONFIDENCE = 1.0
        const val CONFIDENCE_FORMAT = "%.6f"

        const val CONFIDENCE_BUCKET_0_49 = "0.00-0.49"
        const val CONFIDENCE_BUCKET_50_79 = "0.50-0.79"
        const val CONFIDENCE_BUCKET_80_94 = "0.80-0.94"
        const val CONFIDENCE_BUCKET_95_100 = "0.95-1.00"

        val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")

        val ENTRY_COMPARATOR =
            compareBy<CatalogCanonicalizationPlanEntry>(
                { it.sourceIndex },
                { it.action.name },
                {
                    it.originalItemName.lowercase(
                        Locale.ROOT
                    )
                }
            )

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
    }
}