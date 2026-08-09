package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogQualityIssue
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogIssueSeverity
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogQualityValidationResult
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

class CatalogQualityReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        result: CatalogQualityValidationResult,
        outputFile: File
    ) {
        require(outputFile.name.isNotBlank()) {
            "Catalog-quality report output file must have a name."
        }

        require(!outputFile.exists() || outputFile.isFile) {
            "Catalog-quality report output path is not a file: " +
                    outputFile.path
        }

        validateResult(result)

        val normalizedIssues = result.issues
            .map(::normalizeIssue)
            .distinct()
            .sortedWith(ISSUE_COMPARATOR)

        val affectedSourceIndices = normalizedIssues
            .mapNotNull { it.sourceIndex }
            .distinct()
            .sorted()

        val issueCountsByType = normalizedIssues
            .groupingBy { it.type.name }
            .eachCount()
            .toSortedMap()

        val issueCountsBySeverity: Map<String, Int> =
            normalizedIssues
                .fold(sortedMapOf<String, Int>()) { counts, issue ->
                    val severityName = issue.severity.name

                    counts[severityName] =
                        counts.getOrDefault(severityName, 0) + 1

                    counts
                }
                .toMap()

        val issueCountsByField = normalizedIssues
            .groupingBy {
                it.field
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: GLOBAL_FIELD_KEY
            }
            .eachCount()
            .toSortedMap()

        val report = CatalogQualityReport(
            version = CURRENT_VERSION,
            inputEntryCount = result.inputEntryCount,
            validEntryCount = result.validEntryCount,
            affectedEntryCount = result.affectedEntryCount,
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
            uniqueItemNameCount = result.uniqueItemNameCount,
            uniqueNormalizedKeyCount =
                result.uniqueNormalizedKeyCount,
            missingItemNameCount = result.missingItemNameCount,
            missingCategoryCount = result.missingCategoryCount,
            missingNormalizedKeyCount =
                result.missingNormalizedKeyCount,
            duplicateItemNameCount =
                result.duplicateItemNameCount,
            duplicateNormalizedKeyCount =
                result.duplicateNormalizedKeyCount,
            invalidPluralCount = result.invalidPluralCount,
            issueCountsByType = issueCountsByType,
            issueCountsBySeverity = issueCountsBySeverity,
            issueCountsByField = issueCountsByField,
            affectedSourceIndices = affectedSourceIndices,
            issues = normalizedIssues,
            valid = result.valid
        )

        validateReport(report)

        val outputDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        ) {
            "Catalog-quality report output file has no parent directory: " +
                    outputFile.path
        }

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Failed to create catalog-quality report directory: " +
                        outputDirectory.path
            }
        }

        require(outputDirectory.isDirectory) {
            "Catalog-quality report parent is not a directory: " +
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
        issue: CatalogQualityIssue
    ): CatalogQualityIssue =
        issue.copy(
            itemName = issue.itemName
                ?.trim()
                ?.replace(MULTIPLE_WHITESPACE_REGEX, " ")
                ?.takeIf(String::isNotBlank),
            field = issue.field
                ?.trim()
                ?.takeIf(String::isNotBlank),
            message = issue.message
                .trim()
                .replace(MULTIPLE_WHITESPACE_REGEX, " ")
        )

    private fun validateResult(
        result: CatalogQualityValidationResult
    ) {
        require(result.inputEntryCount >= 0) {
            "inputEntryCount must not be negative."
        }

        require(result.validEntryCount >= 0) {
            "validEntryCount must not be negative."
        }

        require(result.affectedEntryCount >= 0) {
            "affectedEntryCount must not be negative."
        }

        require(result.validEntryCount + result.affectedEntryCount ==
                result.inputEntryCount
        ) {
            "validEntryCount plus affectedEntryCount must equal inputEntryCount."
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
            result.issueCountsByType.values.sum() ==
                    result.issueCount
        ) {
            "issueCountsByType must sum to issueCount."
        }

        require(
            result.issueCountsBySeverity.values.sum() ==
                    result.issueCount
        ) {
            "issueCountsBySeverity must sum to issueCount."
        }

        require(
            result.issueCountsByField.values.sum() ==
                    result.issueCount
        ) {
            "issueCountsByField must sum to issueCount."
        }

        require(
            result.affectedEntryCount ==
                    result.affectedSourceIndices.size
        ) {
            "affectedEntryCount must equal affectedSourceIndices size."
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
            result.valid ==
                    result.issues.none {
                        it.severity == CatalogIssueSeverity.ERROR
                    }
        ) {
            "valid is inconsistent with issue severities."
        }

        result.issues.forEach(::validateIssue)
    }

    private fun validateIssue(
        issue: CatalogQualityIssue
    ) {
        require(
            issue.sourceIndex == null ||
                    issue.sourceIndex >= 0
        ) {
            "Catalog quality issue sourceIndex must not be negative."
        }

        require(
            issue.itemName == null ||
                    issue.itemName.isNotBlank()
        ) {
            "Catalog quality issue itemName must not be blank."
        }

        require(
            issue.field == null ||
                    issue.field.isNotBlank()
        ) {
            "Catalog quality issue field must not be blank."
        }

        require(issue.message.isNotBlank()) {
            "Catalog quality issue message must not be blank."
        }

        require(
            issue.sourceIndex != null ||
                    issue.itemName == null
        ) {
            "A global catalog quality issue without sourceIndex must not " +
                    "contain an itemName."
        }
    }

    private fun validateReport(
        report: CatalogQualityReport
    ) {
        require(report.version > 0) {
            "Catalog-quality report version must be greater than zero."
        }

        require(report.inputEntryCount >= 0)
        require(report.validEntryCount >= 0)
        require(report.affectedEntryCount >= 0)
        require(report.issueCount >= 0)
        require(report.errorCount >= 0)
        require(report.warningCount >= 0)
        require(report.infoCount >= 0)
        require(report.uniqueItemNameCount >= 0)
        require(report.uniqueNormalizedKeyCount >= 0)
        require(report.missingItemNameCount >= 0)
        require(report.missingCategoryCount >= 0)
        require(report.missingNormalizedKeyCount >= 0)
        require(report.duplicateItemNameCount >= 0)
        require(report.duplicateNormalizedKeyCount >= 0)
        require(report.invalidPluralCount >= 0)

        require(
            report.validEntryCount +
                    report.affectedEntryCount ==
                    report.inputEntryCount
        ) {
            "validEntryCount plus affectedEntryCount must equal inputEntryCount."
        }

        require(report.issueCount == report.issues.size) {
            "issueCount must equal issues size."
        }

        require(
            report.errorCount +
                    report.warningCount +
                    report.infoCount ==
                    report.issueCount
        ) {
            "Severity counts must sum to issueCount."
        }

        require(
            report.issueCountsByType.values.sum() ==
                    report.issueCount
        ) {
            "issueCountsByType must sum to issueCount."
        }

        require(
            report.issueCountsBySeverity.values.sum() ==
                    report.issueCount
        ) {
            "issueCountsBySeverity must sum to issueCount."
        }

        require(
            report.issueCountsByField.values.sum() ==
                    report.issueCount
        ) {
            "issueCountsByField must sum to issueCount."
        }

        require(
            report.issueCountsByType.keys.toList() ==
                    report.issueCountsByType.keys.sorted()
        ) {
            "issueCountsByType must be sorted."
        }

        require(
            report.issueCountsBySeverity.keys.toList() ==
                    report.issueCountsBySeverity.keys.sorted()
        ) {
            "issueCountsBySeverity must be sorted."
        }

        require(
            report.issueCountsByField.keys.toList() ==
                    report.issueCountsByField.keys.sorted()
        ) {
            "issueCountsByField must be sorted."
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
            report.affectedEntryCount ==
                    report.affectedSourceIndices.size
        ) {
            "affectedEntryCount must equal affectedSourceIndices size."
        }

        require(
            report.issues ==
                    report.issues.sortedWith(ISSUE_COMPARATOR)
        ) {
            "Catalog quality issues must be deterministically sorted."
        }

        require(
            report.issues.distinct().size ==
                    report.issues.size
        ) {
            "Catalog quality issues must not contain duplicates."
        }

        require(
            report.valid ==
                    (report.errorCount == 0)
        ) {
            "Catalog-quality report valid flag is inconsistent."
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

    private data class CatalogQualityReport(
        val version: Int,
        val inputEntryCount: Int,
        val validEntryCount: Int,
        val affectedEntryCount: Int,
        val issueCount: Int,
        val errorCount: Int,
        val warningCount: Int,
        val infoCount: Int,
        val uniqueItemNameCount: Int,
        val uniqueNormalizedKeyCount: Int,
        val missingItemNameCount: Int,
        val missingCategoryCount: Int,
        val missingNormalizedKeyCount: Int,
        val duplicateItemNameCount: Int,
        val duplicateNormalizedKeyCount: Int,
        val invalidPluralCount: Int,
        val issueCountsByType: Map<String, Int>,
        val issueCountsBySeverity: Map<String, Int>,
        val issueCountsByField: Map<String, Int>,
        val affectedSourceIndices: List<Int>,
        val issues: List<CatalogQualityIssue>,
        val valid: Boolean
    )

    private companion object {

        const val CURRENT_VERSION = 1
        const val GLOBAL_FIELD_KEY = "<global>"

        val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")

        val ISSUE_COMPARATOR =
            compareBy<CatalogQualityIssue>(
                { severityRank(it.severity) },
                { it.type.name },
                { it.sourceIndex ?: Int.MAX_VALUE },
                {
                    it.itemName
                        ?.lowercase(Locale.ROOT)
                        ?: ""
                },
                { it.field ?: "" },
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