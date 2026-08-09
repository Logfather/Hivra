package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageIssue
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogIssueSeverity
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

class CatalogLanguageIssuesReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        result: CatalogLanguageValidationResult,
        outputFile: File
    ) {
        require(outputFile.name.isNotBlank()) {
            "Language-issues report output file must have a name."
        }

        require(!outputFile.exists() || outputFile.isFile) {
            "Language-issues report output path is not a file: " +
                    outputFile.path
        }

        validateResult(result)

        val normalizedIssues = result.issues
            .map(::normalizeIssue)
            .distinct()
            .sortedWith(ISSUE_COMPARATOR)

        val issueCountsByType = normalizedIssues
            .groupingBy { it.type.name }
            .eachCount()
            .toSortedMap()

        val issueCountsByField = normalizedIssues
            .groupingBy { normalizeFieldName(it.field) }
            .eachCount()
            .toSortedMap()

        val issueCountsBySeverity = normalizedIssues
            .groupingBy { it.severity.name }
            .eachCount()
            .toSortedMap()

        val affectedSourceIndices = normalizedIssues
            .mapTo(sortedSetOf()) { it.sourceIndex }
            .toList()

        val affectedFieldCount = issueCountsByField.size

        val affectedItemCount = normalizedIssues
            .map { it.sourceIndex }
            .distinct()
            .size

        val suggestedRepairCount = normalizedIssues.count {
            !it.suggestedValue.isNullOrBlank() &&
                    it.suggestedValue != it.originalValue
        }

        val report = CatalogLanguageIssuesReport(
            version = CURRENT_VERSION,
            inputEntryCount = result.inputEntryCount,
            affectedEntryCount = affectedItemCount,
            unaffectedEntryCount =
                result.inputEntryCount - affectedItemCount,
            issueCount = normalizedIssues.size,
            errorCount = normalizedIssues.count {
                it.severity == CatalogIssueSeverity.ERROR
            },
            warningCount = normalizedIssues.count {
                it.severity == CatalogIssueSeverity.WARNING
            },
            infoCount = normalizedIssues.count {
                it.severity == CatalogIssueSeverity.INFO
            },
            affectedFieldCount = affectedFieldCount,
            suggestedRepairCount = suggestedRepairCount,
            issueCountsByType = issueCountsByType,
            issueCountsByField = issueCountsByField,
            issueCountsBySeverity = issueCountsBySeverity,
            affectedSourceIndices = affectedSourceIndices,
            issues = normalizedIssues,
            valid = result.valid
        )

        validateReport(report)

        val outputDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        ) {
            "Language-issues report output file has no parent directory: " +
                    outputFile.path
        }

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Failed to create language-issues report directory: " +
                        outputDirectory.path
            }
        }

        require(outputDirectory.isDirectory) {
            "Language-issues report parent is not a directory: " +
                    outputDirectory.path
        }

        val json = gson.toJson(report).trimEnd() +
                System.lineSeparator()

        writeAtomically(
            outputFile = outputFile,
            content = json
        )
    }

    private fun normalizeIssue(
        issue: CatalogLanguageIssue
    ): CatalogLanguageIssue =
        issue.copy(
            field = normalizeFieldName(issue.field),
            originalValue = issue.originalValue,
            suggestedValue = issue.suggestedValue
                ?.trim()
                ?.replace(MULTIPLE_WHITESPACE_REGEX, " ")
                ?.takeIf(String::isNotBlank),
            matchedTerms = issue.matchedTerms
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted(),
            message = issue.message
                .trim()
                .replace(MULTIPLE_WHITESPACE_REGEX, " ")
        )

    private fun normalizeFieldName(
        field: String
    ): String =
        field
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")

    private fun validateResult(
        result: CatalogLanguageValidationResult
    ) {
        require(result.inputEntryCount >= 0) {
            "inputEntryCount must not be negative."
        }

        require(result.affectedEntryCount >= 0) {
            "affectedEntryCount must not be negative."
        }

        require(result.issueCount >= 0) {
            "issueCount must not be negative."
        }

        require(result.errorCount >= 0) {
            "errorCount must not be negative."
        }

        require(result.warningCount >= 0) {
            "warningCount must not be negative."
        }

        require(result.infoCount >= 0) {
            "infoCount must not be negative."
        }

        require(result.affectedEntryCount <= result.inputEntryCount) {
            "affectedEntryCount must not exceed inputEntryCount."
        }

        require(result.issueCount == result.issues.size) {
            "issueCount must equal issues size."
        }

        require(
            result.errorCount +
                    result.warningCount +
                    result.infoCount ==
                    result.issueCount
        ) {
            "Severity counts must sum to issueCount."
        }

        require(
            result.issueCountsByType.values.all { it >= 0 }
        ) {
            "issueCountsByType must not contain negative values."
        }

        require(
            result.issueCountsByField.values.all { it >= 0 }
        ) {
            "issueCountsByField must not contain negative values."
        }

        require(
            result.issueCountsByType.values.sum() ==
                    result.issueCount
        ) {
            "Sum of issueCountsByType must equal issueCount."
        }

        require(
            result.issueCountsByField.values.sum() ==
                    result.issueCount
        ) {
            "Sum of issueCountsByField must equal issueCount."
        }

        require(
            result.affectedSourceIndices ==
                    result.affectedSourceIndices.sorted()
        ) {
            "affectedSourceIndices must be sorted."
        }

        require(
            result.affectedSourceIndices.distinct().size ==
                    result.affectedSourceIndices.size
        ) {
            "affectedSourceIndices must not contain duplicates."
        }

        require(
            result.affectedEntryCount ==
                    result.affectedSourceIndices.size
        ) {
            "affectedEntryCount must equal affectedSourceIndices size."
        }

        require(
            result.affectedSourceIndices.all { sourceIndex ->
                result.issues.any {
                    it.sourceIndex == sourceIndex
                }
            }
        ) {
            "Every affected source index must be represented by an issue."
        }

        require(
            result.valid ==
                    result.issues.none {
                        it.severity == CatalogIssueSeverity.ERROR
                    }
        ) {
            "valid must be true exactly when no ERROR issue exists."
        }

        result.issues.forEach(::validateIssue)
    }

    private fun validateIssue(
        issue: CatalogLanguageIssue
    ) {
        require(issue.sourceIndex >= 0) {
            "Language issue sourceIndex must not be negative."
        }

        require(issue.itemName.isNotBlank()) {
            "Language issue itemName must not be blank."
        }

        require(issue.field.isNotBlank()) {
            "Language issue field must not be blank."
        }

        require(issue.message.isNotBlank()) {
            "Language issue message must not be blank."
        }

        require(
            issue.matchedTerms.none(String::isBlank)
        ) {
            "Language issue matchedTerms must not contain blank values."
        }

        require(
            issue.matchedTerms.distinct().size ==
                    issue.matchedTerms.size
        ) {
            "Language issue matchedTerms must not contain duplicates."
        }

        require(
            issue.matchedTerms ==
                    issue.matchedTerms.sorted()
        ) {
            "Language issue matchedTerms must be sorted."
        }

        require(
            issue.suggestedValue == null ||
                    issue.suggestedValue.isNotBlank()
        ) {
            "Language issue suggestedValue must not be blank."
        }

        require(
            issue.originalValue != null ||
                    issue.suggestedValue == null
        ) {
            "Language issue must not provide a suggestedValue when " +
                    "originalValue is null."
        }
    }

    private fun validateReport(
        report: CatalogLanguageIssuesReport
    ) {
        require(report.version > 0) {
            "Language-issues report version must be greater than zero."
        }

        require(report.inputEntryCount >= 0) {
            "Language-issues report inputEntryCount must not be negative."
        }

        require(report.affectedEntryCount >= 0) {
            "Language-issues report affectedEntryCount must not be negative."
        }

        require(report.unaffectedEntryCount >= 0) {
            "Language-issues report unaffectedEntryCount must not be negative."
        }

        require(
            report.affectedEntryCount +
                    report.unaffectedEntryCount ==
                    report.inputEntryCount
        ) {
            "affectedEntryCount plus unaffectedEntryCount must equal " +
                    "inputEntryCount."
        }

        require(report.issueCount == report.issues.size) {
            "Language-issues report issueCount must equal issues size."
        }

        require(
            report.errorCount +
                    report.warningCount +
                    report.infoCount ==
                    report.issueCount
        ) {
            "Language-issues severity counts must sum to issueCount."
        }

        require(
            report.issueCountsByType.values.sum() ==
                    report.issueCount
        ) {
            "Sum of issueCountsByType must equal issueCount."
        }

        require(
            report.issueCountsByField.values.sum() ==
                    report.issueCount
        ) {
            "Sum of issueCountsByField must equal issueCount."
        }

        require(
            report.issueCountsBySeverity.values.sum() ==
                    report.issueCount
        ) {
            "Sum of issueCountsBySeverity must equal issueCount."
        }

        require(
            report.affectedFieldCount ==
                    report.issueCountsByField.size
        ) {
            "affectedFieldCount must equal issueCountsByField size."
        }

        require(
            report.affectedEntryCount ==
                    report.affectedSourceIndices.size
        ) {
            "affectedEntryCount must equal affectedSourceIndices size."
        }

        require(
            report.suggestedRepairCount ==
                    report.issues.count {
                        !it.suggestedValue.isNullOrBlank() &&
                                it.suggestedValue != it.originalValue
                    }
        ) {
            "suggestedRepairCount is inconsistent with issues."
        }

        require(
            report.issueCountsByType.keys.toList() ==
                    report.issueCountsByType.keys.sorted()
        ) {
            "issueCountsByType must be sorted by key."
        }

        require(
            report.issueCountsByField.keys.toList() ==
                    report.issueCountsByField.keys.sorted()
        ) {
            "issueCountsByField must be sorted by key."
        }

        require(
            report.issueCountsBySeverity.keys.toList() ==
                    report.issueCountsBySeverity.keys.sorted()
        ) {
            "issueCountsBySeverity must be sorted by key."
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
            report.issues ==
                    report.issues.sortedWith(ISSUE_COMPARATOR)
        ) {
            "Language issues must be deterministically sorted."
        }

        require(
            report.issues.distinct().size ==
                    report.issues.size
        ) {
            "Language issues must not contain duplicates."
        }

        require(
            report.valid ==
                    (report.errorCount == 0)
        ) {
            "Language-issues report valid flag is inconsistent."
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

    private data class CatalogLanguageIssuesReport(
        val version: Int,
        val inputEntryCount: Int,
        val affectedEntryCount: Int,
        val unaffectedEntryCount: Int,
        val issueCount: Int,
        val errorCount: Int,
        val warningCount: Int,
        val infoCount: Int,
        val affectedFieldCount: Int,
        val suggestedRepairCount: Int,
        val issueCountsByType: Map<String, Int>,
        val issueCountsByField: Map<String, Int>,
        val issueCountsBySeverity: Map<String, Int>,
        val affectedSourceIndices: List<Int>,
        val issues: List<CatalogLanguageIssue>,
        val valid: Boolean
    )

    private companion object {

        const val CURRENT_VERSION = 1

        val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")

        val ISSUE_COMPARATOR =
            compareBy<CatalogLanguageIssue>(
                { severityRank(it.severity) },
                { it.type.name },
                { it.sourceIndex },
                {
                    it.itemName.lowercase(
                        Locale.ROOT
                    )
                },
                { it.field },
                { it.originalValue ?: "" },
                { it.suggestedValue ?: "" },
                {
                    it.matchedTerms.joinToString(
                        separator = "|"
                    )
                },
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

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
    }
}