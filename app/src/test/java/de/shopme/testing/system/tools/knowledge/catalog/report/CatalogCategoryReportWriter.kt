package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryIssue
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryValidationResult
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

class CatalogCategoryReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        result: CatalogCategoryValidationResult,
        outputFile: File
    ) {
        require(outputFile.name.isNotBlank()) {
            "Category report output file must have a name."
        }

        require(!outputFile.exists() || outputFile.isFile) {
            "Category report output path is not a file: ${outputFile.path}"
        }

        validateResult(result)

        val normalizedIssues = result.issues
            .distinct()
            .sortedWith(ISSUE_COMPARATOR)

        val normalizedCategoryCounts = result.categoryCounts
            .toSortedMap()

        val normalizedUnknownCategoryCounts =
            result.unknownCategoryCounts.toSortedMap()

        val normalizedSuggestedMappings =
            result.suggestedCategoryMappings.toSortedMap()

        val normalizedUnusedCategoryKeys = result.unusedCategoryKeys
            .distinct()
            .sorted()

        val report = CatalogCategoryReport(
            version = CURRENT_VERSION,
            inputEntryCount = result.inputEntryCount,
            categorizedEntryCount = result.categorizedEntryCount,
            missingCategoryCount = result.missingCategoryCount,
            unknownCategoryCount = result.unknownCategoryCount,
            validCategoryCount = result.validCategoryCount,
            uniqueAssignedCategoryCount =
                result.uniqueAssignedCategoryCount,
            rootCategoryAssignmentCount =
                result.rootCategoryAssignmentCount,
            categoryCount = normalizedCategoryCounts.size,
            unknownCategoryValueCount =
                normalizedUnknownCategoryCounts.size,
            suggestedMappingCount =
                normalizedSuggestedMappings.size,
            unusedCategoryCount =
                normalizedUnusedCategoryKeys.size,
            issueCount = normalizedIssues.size,
            issueCountsByType = normalizedIssues
                .groupingBy { it.type.name }
                .eachCount()
                .toSortedMap(),
            issueCountsBySeverity = normalizedIssues
                .groupingBy { it.severity.name }
                .eachCount()
                .toSortedMap(),
            categoryCounts = normalizedCategoryCounts,
            unknownCategoryCounts =
                normalizedUnknownCategoryCounts,
            suggestedCategoryMappings =
                normalizedSuggestedMappings,
            unusedCategoryKeys =
                normalizedUnusedCategoryKeys,
            issues = normalizedIssues,
            valid = result.valid
        )

        validateReport(report)

        val outputDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        ) {
            "Category report output file has no parent directory: " +
                    outputFile.path
        }

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Failed to create category report directory: " +
                        outputDirectory.path
            }
        }

        require(outputDirectory.isDirectory) {
            "Category report parent is not a directory: " +
                    outputDirectory.path
        }

        val json = gson.toJson(report).trimEnd() +
                System.lineSeparator()

        writeAtomically(
            outputFile = outputFile,
            content = json
        )
    }

    private fun validateResult(
        result: CatalogCategoryValidationResult
    ) {
        require(result.inputEntryCount >= 0) {
            "inputEntryCount must not be negative."
        }

        require(result.categorizedEntryCount >= 0) {
            "categorizedEntryCount must not be negative."
        }

        require(result.missingCategoryCount >= 0) {
            "missingCategoryCount must not be negative."
        }

        require(result.unknownCategoryCount >= 0) {
            "unknownCategoryCount must not be negative."
        }

        require(result.validCategoryCount >= 0) {
            "validCategoryCount must not be negative."
        }

        require(result.uniqueAssignedCategoryCount >= 0) {
            "uniqueAssignedCategoryCount must not be negative."
        }

        require(result.rootCategoryAssignmentCount >= 0) {
            "rootCategoryAssignmentCount must not be negative."
        }

        require(
            result.categorizedEntryCount +
                    result.missingCategoryCount ==
                    result.inputEntryCount
        ) {
            "categorizedEntryCount plus missingCategoryCount must " +
                    "equal inputEntryCount."
        }

        require(
            result.validCategoryCount +
                    result.unknownCategoryCount ==
                    result.categorizedEntryCount
        ) {
            "validCategoryCount plus unknownCategoryCount must " +
                    "equal categorizedEntryCount."
        }

        require(
            result.categoryCounts.values.all { it >= 0 }
        ) {
            "categoryCounts must not contain negative values."
        }

        require(
            result.unknownCategoryCounts.values.all { it >= 0 }
        ) {
            "unknownCategoryCounts must not contain negative values."
        }

        require(
            result.categoryCounts.values.sum() ==
                    result.validCategoryCount
        ) {
            "Sum of categoryCounts must equal validCategoryCount."
        }

        require(
            result.unknownCategoryCounts.values.sum() ==
                    result.unknownCategoryCount
        ) {
            "Sum of unknownCategoryCounts must equal " +
                    "unknownCategoryCount."
        }

        require(
            result.uniqueAssignedCategoryCount ==
                    result.categoryCounts.size
        ) {
            "uniqueAssignedCategoryCount must equal categoryCounts size."
        }

        require(
            result.unusedCategoryKeys.distinct().size ==
                    result.unusedCategoryKeys.size
        ) {
            "unusedCategoryKeys must not contain duplicates."
        }

        val duplicateSuggestedMappings =
            result.suggestedCategoryMappings
                .entries
                .groupBy { it.key }
                .filterValues { it.size > 1 }
                .keys
                .sorted()

        require(duplicateSuggestedMappings.isEmpty()) {
            "suggestedCategoryMappings contains duplicate source " +
                    "category values: " +
                    duplicateSuggestedMappings.joinToString(", ")
        }

        result.issues.forEach(::validateIssue)
    }

    private fun validateIssue(
        issue: CatalogCategoryIssue
    ) {
        require(
            issue.sourceIndex == null ||
                    issue.sourceIndex >= 0
        ) {
            "Category issue sourceIndex must not be negative."
        }

        require(
            issue.itemName == null ||
                    issue.itemName.isNotBlank()
        ) {
            "Category issue itemName must not be blank."
        }

        require(
            issue.originalCategory == null ||
                    issue.originalCategory.isNotEmpty()
        ) {
            "Category issue originalCategory must not be empty."
        }

        require(
            issue.normalizedCategory == null ||
                    issue.normalizedCategory.isNotBlank()
        ) {
            "Category issue normalizedCategory must not be blank."
        }

        require(
            issue.suggestedCategoryKey == null ||
                    issue.suggestedCategoryKey.isNotBlank()
        ) {
            "Category issue suggestedCategoryKey must not be blank."
        }

        require(issue.message.isNotBlank()) {
            "Category issue message must not be blank."
        }

        require(
            issue.sourceIndex != null ||
                    issue.itemName == null
        ) {
            "A global category issue without sourceIndex must not " +
                    "contain an itemName."
        }
    }

    private fun validateReport(
        report: CatalogCategoryReport
    ) {
        require(report.version > 0) {
            "Category report version must be greater than zero."
        }

        require(
            report.categoryCount ==
                    report.categoryCounts.size
        ) {
            "categoryCount must equal categoryCounts size."
        }

        require(
            report.unknownCategoryValueCount ==
                    report.unknownCategoryCounts.size
        ) {
            "unknownCategoryValueCount must equal " +
                    "unknownCategoryCounts size."
        }

        require(
            report.suggestedMappingCount ==
                    report.suggestedCategoryMappings.size
        ) {
            "suggestedMappingCount must equal " +
                    "suggestedCategoryMappings size."
        }

        require(
            report.unusedCategoryCount ==
                    report.unusedCategoryKeys.size
        ) {
            "unusedCategoryCount must equal unusedCategoryKeys size."
        }

        require(report.issueCount == report.issues.size) {
            "issueCount must equal issues size."
        }

        require(
            report.issueCountsByType.values.sum() ==
                    report.issueCount
        ) {
            "Sum of issueCountsByType must equal issueCount."
        }

        require(
            report.issueCountsBySeverity.values.sum() ==
                    report.issueCount
        ) {
            "Sum of issueCountsBySeverity must equal issueCount."
        }

        require(
            report.categoryCounts.keys.toList() ==
                    report.categoryCounts.keys.sorted()
        ) {
            "categoryCounts must be sorted by key."
        }

        require(
            report.unknownCategoryCounts.keys.toList() ==
                    report.unknownCategoryCounts.keys.sorted()
        ) {
            "unknownCategoryCounts must be sorted by key."
        }

        require(
            report.suggestedCategoryMappings.keys.toList() ==
                    report.suggestedCategoryMappings.keys.sorted()
        ) {
            "suggestedCategoryMappings must be sorted by key."
        }

        require(
            report.issueCountsByType.keys.toList() ==
                    report.issueCountsByType.keys.sorted()
        ) {
            "issueCountsByType must be sorted by key."
        }

        require(
            report.issueCountsBySeverity.keys.toList() ==
                    report.issueCountsBySeverity.keys.sorted()
        ) {
            "issueCountsBySeverity must be sorted by key."
        }

        require(
            report.unusedCategoryKeys ==
                    report.unusedCategoryKeys.sorted()
        ) {
            "unusedCategoryKeys must be sorted."
        }

        require(
            report.unusedCategoryKeys.distinct().size ==
                    report.unusedCategoryKeys.size
        ) {
            "unusedCategoryKeys must not contain duplicates."
        }

        require(
            report.issues ==
                    report.issues.sortedWith(ISSUE_COMPARATOR)
        ) {
            "Category issues must be deterministically sorted."
        }

        require(
            report.issues.distinct().size ==
                    report.issues.size
        ) {
            "Category issues must not contain duplicates."
        }

        require(
            report.valid ==
                    (
                            report.missingCategoryCount == 0 &&
                                    report.unknownCategoryCount == 0 &&
                                    report.issues.none {
                                        it.severity.name == ERROR_SEVERITY
                                    }
                            )
        ) {
            "Category report valid flag is inconsistent."
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

    private data class CatalogCategoryReport(
        val version: Int,
        val inputEntryCount: Int,
        val categorizedEntryCount: Int,
        val missingCategoryCount: Int,
        val unknownCategoryCount: Int,
        val validCategoryCount: Int,
        val uniqueAssignedCategoryCount: Int,
        val rootCategoryAssignmentCount: Int,
        val categoryCount: Int,
        val unknownCategoryValueCount: Int,
        val suggestedMappingCount: Int,
        val unusedCategoryCount: Int,
        val issueCount: Int,
        val issueCountsByType: Map<String, Int>,
        val issueCountsBySeverity: Map<String, Int>,
        val categoryCounts: Map<String, Int>,
        val unknownCategoryCounts: Map<String, Int>,
        val suggestedCategoryMappings: Map<String, String>,
        val unusedCategoryKeys: List<String>,
        val issues: List<CatalogCategoryIssue>,
        val valid: Boolean
    )

    private companion object {

        const val CURRENT_VERSION = 1
        const val ERROR_SEVERITY = "ERROR"

        val ISSUE_COMPARATOR =
            compareBy<CatalogCategoryIssue>(
                { severityRank(it.severity.name) },
                { it.type.name },
                { it.sourceIndex ?: Int.MAX_VALUE },
                {
                    it.itemName
                        ?.lowercase(Locale.ROOT)
                        ?: ""
                },
                {
                    it.originalCategory
                        ?.lowercase(Locale.ROOT)
                        ?: ""
                },
                { it.normalizedCategory ?: "" },
                { it.suggestedCategoryKey ?: "" },
                { it.message }
            )

        fun severityRank(
            severityName: String
        ): Int =
            when (severityName) {
                "ERROR" -> 0
                "WARNING" -> 1
                "INFO" -> 2
                else -> 3
            }

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
    }
}