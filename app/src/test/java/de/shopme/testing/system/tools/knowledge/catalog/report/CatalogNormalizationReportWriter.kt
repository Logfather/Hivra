package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationChange
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationResult
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

class CatalogNormalizationReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        normalizations: List<CatalogNormalizationResult>,
        outputFile: File
    ) {
        require(outputFile.name.isNotBlank()) {
            "Normalization report output file must have a name."
        }

        require(!outputFile.exists() || outputFile.isFile) {
            "Normalization report output path is not a file: " +
                    outputFile.path
        }

        validateNormalizations(normalizations)

        val normalizedEntries = normalizations
            .map(::normalizeResult)
            .sortedWith(RESULT_COMPARATOR)

        val changedEntries = normalizedEntries.filter {
            it.changes.isNotEmpty()
        }

        val unchangedEntries = normalizedEntries.filter {
            it.changes.isEmpty()
        }

        val normalizedKeyGroups = normalizedEntries
            .filter {
                it.computedNormalizedKey.isNotBlank()
            }
            .groupBy {
                it.computedNormalizedKey
            }
            .toSortedMap()

        val normalizedKeyCollisionGroups = normalizedKeyGroups
            .filterValues {
                it.size > 1
            }
            .map { (normalizedKey, entries) ->
                CatalogNormalizationKeyCollision(
                    normalizedKey = normalizedKey,
                    sourceIndices = entries
                        .map { it.sourceIndex }
                        .distinct()
                        .sorted(),
                    itemNames = entries
                        .map { it.originalItemName }
                        .distinct()
                        .sortedWith(String.CASE_INSENSITIVE_ORDER),
                    entryCount = entries.size
                )
            }
            .sortedWith(KEY_COLLISION_COMPARATOR)

        val changeCountsByType = normalizedEntries
            .flatMap { it.changes }
            .groupingBy { it.type.name }
            .eachCount()
            .toSortedMap()

        val changeCountsByField = normalizedEntries
            .flatMap { it.changes }
            .groupingBy { it.field }
            .eachCount()
            .toSortedMap()

        val totalChangeCount = normalizedEntries
            .sumOf { it.changes.size }

        val canonicalNameChangedCount = normalizedEntries.count {
            it.originalItemName != it.computedCanonicalName
        }

        val normalizedKeyChangedCount = normalizedEntries.count {
            it.originalNormalized != it.computedNormalizedKey
        }

        val pluralChangedCount = normalizedEntries.count {
            it.computedPlural != null &&
                    it.computedPlural != it.originalPlural
        }

        val aliasChangedCount = normalizedEntries.count {
            it.normalizedColloquial != it.originalColloquial
        }

        val phoneticTokenChangedCount = normalizedEntries.count {
            it.normalizedPhoneticTokens != it.originalPhoneticTokens
        }

        val autocompleteTokenChangedCount = normalizedEntries.count {
            it.normalizedAutocompleteTokens !=
                    it.originalAutocompleteTokens
        }

        val report = CatalogNormalizationReport(
            version = CURRENT_VERSION,
            inputEntryCount = normalizedEntries.size,
            changedEntryCount = changedEntries.size,
            unchangedEntryCount = unchangedEntries.size,
            totalChangeCount = totalChangeCount,
            canonicalNameChangedCount = canonicalNameChangedCount,
            normalizedKeyChangedCount = normalizedKeyChangedCount,
            pluralChangedCount = pluralChangedCount,
            aliasChangedCount = aliasChangedCount,
            phoneticTokenChangedCount = phoneticTokenChangedCount,
            autocompleteTokenChangedCount =
                autocompleteTokenChangedCount,
            normalizedKeyCollisionGroupCount =
                normalizedKeyCollisionGroups.size,
            normalizedKeyCollisionEntryCount =
                normalizedKeyCollisionGroups.sumOf {
                    it.entryCount
                },
            uniqueComputedNormalizedKeyCount =
                normalizedKeyGroups.size,
            emptyComputedNormalizedKeyCount =
                normalizedEntries.count {
                    it.computedNormalizedKey.isBlank()
                },
            changeCountsByType = changeCountsByType,
            changeCountsByField = changeCountsByField,
            affectedSourceIndices = changedEntries
                .map { it.sourceIndex }
                .distinct()
                .sorted(),
            normalizedKeyCollisionGroups =
                normalizedKeyCollisionGroups,
            entries = normalizedEntries,
            valid = normalizedEntries
                .map { it.sourceIndex }
                .distinct()
                .size == normalizedEntries.size
        )

        validateReport(report)

        val outputDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        ) {
            "Normalization report output file has no parent directory: " +
                    outputFile.path
        }

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Failed to create normalization report directory: " +
                        outputDirectory.path
            }
        }

        require(outputDirectory.isDirectory) {
            "Normalization report parent is not a directory: " +
                    outputDirectory.path
        }

        val json = gson.toJson(report).trimEnd() +
                System.lineSeparator()

        writeAtomically(
            outputFile = outputFile,
            content = json
        )
    }

    private fun normalizeResult(
        result: CatalogNormalizationResult
    ): CatalogNormalizationReportEntry =
        CatalogNormalizationReportEntry(
            sourceIndex = result.sourceIndex,
            originalItemName = normalizeRequiredText(
                result.originalItemName
            ),
            originalNormalized = result.originalNormalized
                ?.trim()
                ?.takeIf(String::isNotBlank),
            originalPlural = result.originalPlural
                ?.let(::normalizeOptionalText),
            originalColloquial = result.originalColloquial
                .map(::normalizeRequiredText)
                .filter(String::isNotBlank)
                .distinct()
                .sortedWith(String.CASE_INSENSITIVE_ORDER),
            originalPhoneticTokens =
                result.originalPhoneticTokens
                    .map(::normalizeRequiredText)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),
            originalAutocompleteTokens =
                result.originalAutocompleteTokens
                    .map(::normalizeRequiredText)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),
            computedCanonicalName = normalizeRequiredText(
                result.computedCanonicalName
            ),
            computedNormalizedKey =
                result.computedNormalizedKey.trim(),
            computedPlural = result.computedPlural
                ?.let(::normalizeOptionalText),
            normalizedColloquial =
                result.normalizedColloquial
                    .map(::normalizeRequiredText)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sortedWith(String.CASE_INSENSITIVE_ORDER),
            normalizedPhoneticTokens =
                result.normalizedPhoneticTokens
                    .map(::normalizeRequiredText)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),
            normalizedAutocompleteTokens =
                result.normalizedAutocompleteTokens
                    .map(::normalizeRequiredText)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),
            changes = result.changes
                .map(::normalizeChange)
                .distinct()
                .sortedWith(CHANGE_COMPARATOR)
        )

    private fun normalizeChange(
        change: CatalogNormalizationChange
    ): CatalogNormalizationChange =
        change.copy(
            field = change.field.trim(),
            before = change.before
                ?.trim()
                ?.takeIf(String::isNotBlank),
            after = change.after
                ?.trim()
                ?.takeIf(String::isNotBlank),
            reason = normalizeRequiredText(change.reason)
        )

    private fun validateNormalizations(
        normalizations: List<CatalogNormalizationResult>
    ) {
        val duplicateSourceIndices = normalizations
            .groupBy { it.sourceIndex }
            .filterValues { it.size > 1 }
            .keys
            .sorted()

        require(duplicateSourceIndices.isEmpty()) {
            "Catalog normalizations contain duplicate sourceIndex values: " +
                    duplicateSourceIndices.joinToString(", ")
        }

        normalizations.forEach(::validateNormalization)
    }

    private fun validateNormalization(
        normalization: CatalogNormalizationResult
    ) {
        require(normalization.sourceIndex >= 0) {
            "Normalization sourceIndex must not be negative."
        }

        require(normalization.originalItemName.isNotBlank()) {
            "Normalization originalItemName must not be blank at " +
                    "sourceIndex ${normalization.sourceIndex}."
        }

        require(normalization.computedCanonicalName.isNotBlank()) {
            "Normalization computedCanonicalName must not be blank at " +
                    "sourceIndex ${normalization.sourceIndex}."
        }

        require(normalization.computedNormalizedKey.isNotBlank()) {
            "Normalization computedNormalizedKey must not be blank at " +
                    "sourceIndex ${normalization.sourceIndex}."
        }

        require(
            NORMALIZED_KEY_REGEX.matches(
                normalization.computedNormalizedKey
            )
        ) {
            "Normalization computedNormalizedKey contains invalid " +
                    "characters at sourceIndex ${normalization.sourceIndex}: " +
                    "'${normalization.computedNormalizedKey}'."
        }

        require(
            normalization.computedNormalizedKey ==
                    normalization.computedNormalizedKey
                        .lowercase(Locale.ROOT)
        ) {
            "Normalization computedNormalizedKey must be lowercase at " +
                    "sourceIndex ${normalization.sourceIndex}."
        }

        require(
            normalization.normalizedColloquial
                .none(String::isBlank)
        ) {
            "normalizedColloquial must not contain blank values at " +
                    "sourceIndex ${normalization.sourceIndex}."
        }

        require(
            normalization.normalizedPhoneticTokens
                .none(String::isBlank)
        ) {
            "normalizedPhoneticTokens must not contain blank values at " +
                    "sourceIndex ${normalization.sourceIndex}."
        }

        require(
            normalization.normalizedAutocompleteTokens
                .none(String::isBlank)
        ) {
            "normalizedAutocompleteTokens must not contain blank values at " +
                    "sourceIndex ${normalization.sourceIndex}."
        }

        require(
            normalization.normalizedColloquial.distinct().size ==
                    normalization.normalizedColloquial.size
        ) {
            "normalizedColloquial must not contain duplicates at " +
                    "sourceIndex ${normalization.sourceIndex}."
        }

        require(
            normalization.normalizedPhoneticTokens.distinct().size ==
                    normalization.normalizedPhoneticTokens.size
        ) {
            "normalizedPhoneticTokens must not contain duplicates at " +
                    "sourceIndex ${normalization.sourceIndex}."
        }

        require(
            normalization.normalizedAutocompleteTokens
                .distinct()
                .size ==
                    normalization.normalizedAutocompleteTokens.size
        ) {
            "normalizedAutocompleteTokens must not contain duplicates at " +
                    "sourceIndex ${normalization.sourceIndex}."
        }

        normalization.changes.forEach { change ->
            validateChange(
                sourceIndex = normalization.sourceIndex,
                change = change
            )
        }
    }

    private fun validateChange(
        sourceIndex: Int,
        change: CatalogNormalizationChange
    ) {
        require(change.field.isNotBlank()) {
            "Normalization change field must not be blank at sourceIndex " +
                    "$sourceIndex."
        }

        require(change.reason.isNotBlank()) {
            "Normalization change reason must not be blank at sourceIndex " +
                    "$sourceIndex."
        }

        require(
            change.before != null ||
                    change.after != null
        ) {
            "Normalization change must contain before or after at " +
                    "sourceIndex $sourceIndex."
        }

        require(change.before != change.after) {
            "Normalization change before and after must differ at " +
                    "sourceIndex $sourceIndex, field '${change.field}'."
        }
    }

    private fun validateReport(
        report: CatalogNormalizationReport
    ) {
        require(report.version > 0) {
            "Normalization report version must be greater than zero."
        }

        require(report.inputEntryCount >= 0) {
            "inputEntryCount must not be negative."
        }

        require(report.changedEntryCount >= 0) {
            "changedEntryCount must not be negative."
        }

        require(report.unchangedEntryCount >= 0) {
            "unchangedEntryCount must not be negative."
        }

        require(report.totalChangeCount >= 0) {
            "totalChangeCount must not be negative."
        }

        require(
            report.changedEntryCount +
                    report.unchangedEntryCount ==
                    report.inputEntryCount
        ) {
            "changedEntryCount plus unchangedEntryCount must equal " +
                    "inputEntryCount."
        }

        require(
            report.inputEntryCount ==
                    report.entries.size
        ) {
            "inputEntryCount must equal entries size."
        }

        require(
            report.totalChangeCount ==
                    report.entries.sumOf { it.changes.size }
        ) {
            "totalChangeCount is inconsistent with entries."
        }

        require(
            report.changedEntryCount ==
                    report.entries.count {
                        it.changes.isNotEmpty()
                    }
        ) {
            "changedEntryCount is inconsistent with entries."
        }

        require(
            report.unchangedEntryCount ==
                    report.entries.count {
                        it.changes.isEmpty()
                    }
        ) {
            "unchangedEntryCount is inconsistent with entries."
        }

        require(
            report.canonicalNameChangedCount ==
                    report.entries.count {
                        it.originalItemName !=
                                it.computedCanonicalName
                    }
        ) {
            "canonicalNameChangedCount is inconsistent."
        }

        require(
            report.normalizedKeyChangedCount ==
                    report.entries.count {
                        it.originalNormalized !=
                                it.computedNormalizedKey
                    }
        ) {
            "normalizedKeyChangedCount is inconsistent."
        }

        require(
            report.pluralChangedCount ==
                    report.entries.count {
                        it.computedPlural != null &&
                                it.computedPlural != it.originalPlural
                    }
        ) {
            "pluralChangedCount is inconsistent."
        }

        require(
            report.aliasChangedCount ==
                    report.entries.count {
                        it.normalizedColloquial !=
                                it.originalColloquial
                    }
        ) {
            "aliasChangedCount is inconsistent."
        }

        require(
            report.phoneticTokenChangedCount ==
                    report.entries.count {
                        it.normalizedPhoneticTokens !=
                                it.originalPhoneticTokens
                    }
        ) {
            "phoneticTokenChangedCount is inconsistent."
        }

        require(
            report.autocompleteTokenChangedCount ==
                    report.entries.count {
                        it.normalizedAutocompleteTokens !=
                                it.originalAutocompleteTokens
                    }
        ) {
            "autocompleteTokenChangedCount is inconsistent."
        }

        require(
            report.changeCountsByType.values.sum() ==
                    report.totalChangeCount
        ) {
            "changeCountsByType must sum to totalChangeCount."
        }

        require(
            report.changeCountsByField.values.sum() ==
                    report.totalChangeCount
        ) {
            "changeCountsByField must sum to totalChangeCount."
        }

        require(
            report.changeCountsByType.keys.toList() ==
                    report.changeCountsByType.keys.sorted()
        ) {
            "changeCountsByType must be sorted by key."
        }

        require(
            report.changeCountsByField.keys.toList() ==
                    report.changeCountsByField.keys.sorted()
        ) {
            "changeCountsByField must be sorted by key."
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
            report.affectedSourceIndices ==
                    report.entries
                        .filter { it.changes.isNotEmpty() }
                        .map { it.sourceIndex }
                        .sorted()
        ) {
            "affectedSourceIndices is inconsistent with changed entries."
        }

        require(
            report.normalizedKeyCollisionGroupCount ==
                    report.normalizedKeyCollisionGroups.size
        ) {
            "normalizedKeyCollisionGroupCount is inconsistent."
        }

        require(
            report.normalizedKeyCollisionEntryCount ==
                    report.normalizedKeyCollisionGroups.sumOf {
                        it.entryCount
                    }
        ) {
            "normalizedKeyCollisionEntryCount is inconsistent."
        }

        require(
            report.normalizedKeyCollisionGroups ==
                    report.normalizedKeyCollisionGroups.sortedWith(
                        KEY_COLLISION_COMPARATOR
                    )
        ) {
            "normalizedKeyCollisionGroups must be deterministically sorted."
        }

        require(
            report.entries ==
                    report.entries.sortedWith(RESULT_COMPARATOR)
        ) {
            "Normalization entries must be deterministically sorted."
        }

        require(
            report.entries.map { it.sourceIndex }.distinct().size ==
                    report.inputEntryCount
        ) {
            "Normalization entries must have unique sourceIndex values."
        }

        require(
            report.valid ==
                    (
                            report.entries
                                .map { it.sourceIndex }
                                .distinct()
                                .size ==
                                    report.inputEntryCount
                            )
        ) {
            "Normalization report valid flag is inconsistent."
        }
    }

    private fun normalizeRequiredText(
        value: String
    ): String =
        value
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")

    private fun normalizeOptionalText(
        value: String
    ): String? =
        normalizeRequiredText(value)
            .takeIf(String::isNotBlank)

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

    private data class CatalogNormalizationReport(
        val version: Int,
        val inputEntryCount: Int,
        val changedEntryCount: Int,
        val unchangedEntryCount: Int,
        val totalChangeCount: Int,
        val canonicalNameChangedCount: Int,
        val normalizedKeyChangedCount: Int,
        val pluralChangedCount: Int,
        val aliasChangedCount: Int,
        val phoneticTokenChangedCount: Int,
        val autocompleteTokenChangedCount: Int,
        val normalizedKeyCollisionGroupCount: Int,
        val normalizedKeyCollisionEntryCount: Int,
        val uniqueComputedNormalizedKeyCount: Int,
        val emptyComputedNormalizedKeyCount: Int,
        val changeCountsByType: Map<String, Int>,
        val changeCountsByField: Map<String, Int>,
        val affectedSourceIndices: List<Int>,
        val normalizedKeyCollisionGroups:
        List<CatalogNormalizationKeyCollision>,
        val entries: List<CatalogNormalizationReportEntry>,
        val valid: Boolean
    )

    private data class CatalogNormalizationReportEntry(
        val sourceIndex: Int,
        val originalItemName: String,
        val originalNormalized: String?,
        val originalPlural: String?,
        val originalColloquial: List<String>,
        val originalPhoneticTokens: List<String>,
        val originalAutocompleteTokens: List<String>,
        val computedCanonicalName: String,
        val computedNormalizedKey: String,
        val computedPlural: String?,
        val normalizedColloquial: List<String>,
        val normalizedPhoneticTokens: List<String>,
        val normalizedAutocompleteTokens: List<String>,
        val changes: List<CatalogNormalizationChange>
    )

    private data class CatalogNormalizationKeyCollision(
        val normalizedKey: String,
        val sourceIndices: List<Int>,
        val itemNames: List<String>,
        val entryCount: Int
    )

    private companion object {

        const val CURRENT_VERSION = 1

        val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")
        val NORMALIZED_KEY_REGEX =
            Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")

        val CHANGE_COMPARATOR =
            compareBy<CatalogNormalizationChange>(
                { it.type.name },
                { it.field },
                { it.before ?: "" },
                { it.after ?: "" },
                { it.reason }
            )

        val RESULT_COMPARATOR =
            compareBy<CatalogNormalizationReportEntry>(
                { it.sourceIndex },
                {
                    it.originalItemName.lowercase(
                        Locale.ROOT
                    )
                },
                { it.computedNormalizedKey }
            )

        val KEY_COLLISION_COMPARATOR =
            compareByDescending<CatalogNormalizationKeyCollision> {
                it.entryCount
            }
                .thenBy {
                    it.normalizedKey
                }
                .thenBy {
                    it.sourceIndices.firstOrNull()
                        ?: Int.MAX_VALUE
                }

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
    }
}