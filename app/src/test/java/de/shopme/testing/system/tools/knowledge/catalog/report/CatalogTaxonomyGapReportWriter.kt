package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryDefinition
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryValidationResult
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

class CatalogTaxonomyGapReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        registry: CanonicalFoodCategoryRegistry,
        categoryResult: CatalogCategoryValidationResult,
        outputFile: File
    ) {
        require(outputFile.name.isNotBlank()) {
            "Taxonomy-gap report output file must have a name."
        }

        require(!outputFile.exists() || outputFile.isFile) {
            "Taxonomy-gap report output path is not a file: " +
                    outputFile.path
        }

        val definitions = registry.definitions()

        validateInputs(
            registry = registry,
            definitions = definitions,
            categoryResult = categoryResult
        )

        val categoryCounts = categoryResult.categoryCounts.toSortedMap()

        val assignedCategoryKeys = categoryCounts
            .filterValues { it > 0 }
            .keys

        val rootDefinitions = registry.rootCategories()
            .sortedWith(DEFINITION_COMPARATOR)

        val taxonomyEntries = definitions
            .map { definition ->
                createTaxonomyEntry(
                    definition = definition,
                    registry = registry,
                    categoryCounts = categoryCounts
                )
            }
            .sortedWith(TAXONOMY_ENTRY_COMPARATOR)

        val rootSummaries = rootDefinitions
            .map { rootDefinition ->
                createRootSummary(
                    rootDefinition = rootDefinition,
                    registry = registry,
                    categoryCounts = categoryCounts
                )
            }
            .sortedWith(ROOT_SUMMARY_COMPARATOR)

        val unusedCategoryKeys = definitions
            .asSequence()
            .map { it.key }
            .filterNot { it in assignedCategoryKeys }
            .sorted()
            .toList()

        val unusedLeafCategoryKeys = registry.leafCategories()
            .asSequence()
            .map { it.key }
            .filterNot { it in assignedCategoryKeys }
            .sorted()
            .toList()

        val unusedRootCategoryKeys = rootDefinitions
            .asSequence()
            .map { it.key }
            .filterNot { rootKey ->
                containsAnyAssignedEntry(
                    categoryKey = rootKey,
                    registry = registry,
                    categoryCounts = categoryCounts
                )
            }
            .sorted()
            .toList()

        val underrepresentedCategoryKeys = taxonomyEntries
            .asSequence()
            .filter {
                it.directCatalogItemCount in
                        1..UNDERREPRESENTED_CATEGORY_MAXIMUM_COUNT
            }
            .map { it.key }
            .sorted()
            .toList()

        val emptyInternalCategoryKeys = taxonomyEntries
            .asSequence()
            .filter {
                !it.leaf &&
                        it.directCatalogItemCount == 0 &&
                        it.descendantCatalogItemCount == 0
            }
            .map { it.key }
            .sorted()
            .toList()

        val rootOnlyAssignedCategoryKeys = taxonomyEntries
            .asSequence()
            .filter {
                it.root &&
                        it.directCatalogItemCount > 0 &&
                        it.childCount > 0
            }
            .map { it.key }
            .sorted()
            .toList()

        val unknownCategoryEntries = categoryResult
            .unknownCategoryCounts
            .map { (category, count) ->
                UnknownTaxonomyCategory(
                    category = category,
                    entryCount = count,
                    suggestedCategoryKey =
                        categoryResult.suggestedCategoryMappings[category]
                )
            }
            .sortedWith(UNKNOWN_CATEGORY_COMPARATOR)

        val totalAssignedEntryCount = categoryCounts.values.sum()

        val leafAssignedEntryCount = taxonomyEntries
            .filter { it.leaf }
            .sumOf { it.directCatalogItemCount }

        val rootAssignedEntryCount = taxonomyEntries
            .filter { it.root }
            .sumOf { it.directCatalogItemCount }

        val internalAssignedEntryCount =
            totalAssignedEntryCount -
                    leafAssignedEntryCount -
                    rootAssignedEntryCount

        val report = CatalogTaxonomyGapReport(
            version = CURRENT_VERSION,
            registryCategoryCount = definitions.size,
            rootCategoryCount = rootDefinitions.size,
            internalCategoryCount = taxonomyEntries.count {
                !it.root && !it.leaf
            },
            leafCategoryCount = taxonomyEntries.count { it.leaf },
            assignedCategoryCount = assignedCategoryKeys.size,
            unusedCategoryCount = unusedCategoryKeys.size,
            unusedLeafCategoryCount = unusedLeafCategoryKeys.size,
            unusedRootCategoryCount = unusedRootCategoryKeys.size,
            emptyInternalCategoryCount = emptyInternalCategoryKeys.size,
            underrepresentedCategoryCount =
                underrepresentedCategoryKeys.size,
            rootOnlyAssignedCategoryCount =
                rootOnlyAssignedCategoryKeys.size,
            unknownCategoryCount =
                categoryResult.unknownCategoryCount,
            unknownCategoryValueCount =
                unknownCategoryEntries.size,
            totalAssignedEntryCount = totalAssignedEntryCount,
            leafAssignedEntryCount = leafAssignedEntryCount,
            internalAssignedEntryCount = internalAssignedEntryCount,
            rootAssignedEntryCount = rootAssignedEntryCount,
            taxonomyCoverage = calculateRatio(
                numerator = assignedCategoryKeys.size,
                denominator = definitions.size
            ),
            leafTaxonomyCoverage = calculateRatio(
                numerator = registry.leafCategories()
                    .count { it.key in assignedCategoryKeys },
                denominator = registry.leafCategories().size
            ),
            entryLeafSpecificity = calculateRatio(
                numerator = leafAssignedEntryCount,
                denominator = totalAssignedEntryCount
            ),
            unusedCategoryKeys = unusedCategoryKeys,
            unusedLeafCategoryKeys = unusedLeafCategoryKeys,
            unusedRootCategoryKeys = unusedRootCategoryKeys,
            emptyInternalCategoryKeys = emptyInternalCategoryKeys,
            underrepresentedCategoryKeys =
                underrepresentedCategoryKeys,
            rootOnlyAssignedCategoryKeys =
                rootOnlyAssignedCategoryKeys,
            unknownCategories = unknownCategoryEntries,
            rootSummaries = rootSummaries,
            categories = taxonomyEntries,
            valid = categoryResult.unknownCategoryCount == 0
        )

        validateReport(report)

        val outputDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        ) {
            "Taxonomy-gap report output file has no parent directory: " +
                    outputFile.path
        }

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Failed to create taxonomy-gap report directory: " +
                        outputDirectory.path
            }
        }

        require(outputDirectory.isDirectory) {
            "Taxonomy-gap report parent is not a directory: " +
                    outputDirectory.path
        }

        val json = gson.toJson(report).trimEnd() +
                System.lineSeparator()

        writeAtomically(
            outputFile = outputFile,
            content = json
        )
    }

    private fun createTaxonomyEntry(
        definition: CatalogCategoryDefinition,
        registry: CanonicalFoodCategoryRegistry,
        categoryCounts: Map<String, Int>
    ): TaxonomyCategoryEntry {
        val descendants = registry.descendants(definition.key)
        val children = registry.children(definition.key)
        val directCatalogItemCount =
            categoryCounts[definition.key] ?: 0

        val descendantCatalogItemCount = descendants.sumOf {
            categoryCounts[it.key] ?: 0
        }

        val assignedDescendantCategoryCount = descendants.count {
            (categoryCounts[it.key] ?: 0) > 0
        }

        val unusedDescendantCategoryCount =
            descendants.size - assignedDescendantCategoryCount

        return TaxonomyCategoryEntry(
            key = definition.key,
            displayName = definition.displayName,
            parentKey = definition.parentKey,
            path = registry.categoryPath(definition.key),
            depth = registry.depth(definition.key),
            root = definition.parentKey == null,
            leaf = children.isEmpty(),
            childCount = children.size,
            descendantCount = descendants.size,
            directCatalogItemCount = directCatalogItemCount,
            descendantCatalogItemCount =
                descendantCatalogItemCount,
            totalSubtreeCatalogItemCount =
                directCatalogItemCount +
                        descendantCatalogItemCount,
            assignedDescendantCategoryCount =
                assignedDescendantCategoryCount,
            unusedDescendantCategoryCount =
                unusedDescendantCategoryCount,
            used = directCatalogItemCount > 0,
            subtreeUsed =
                directCatalogItemCount > 0 ||
                        descendantCatalogItemCount > 0,
            underrepresented =
                directCatalogItemCount in
                        1..UNDERREPRESENTED_CATEGORY_MAXIMUM_COUNT,
            directlyAssignedDespiteChildren =
                directCatalogItemCount > 0 &&
                        children.isNotEmpty(),
            childKeys = children
                .map { it.key }
                .sorted()
        )
    }

    private fun createRootSummary(
        rootDefinition: CatalogCategoryDefinition,
        registry: CanonicalFoodCategoryRegistry,
        categoryCounts: Map<String, Int>
    ): RootTaxonomySummary {
        val descendants = registry.descendants(rootDefinition.key)

        val subtreeDefinitions =
            listOf(rootDefinition) + descendants

        val directCatalogItemCount =
            categoryCounts[rootDefinition.key] ?: 0

        val descendantCatalogItemCount = descendants.sumOf {
            categoryCounts[it.key] ?: 0
        }

        val usedCategoryCount = subtreeDefinitions.count {
            (categoryCounts[it.key] ?: 0) > 0
        }

        val leafDefinitions = subtreeDefinitions.filter {
            registry.children(it.key).isEmpty()
        }

        val usedLeafCategoryCount = leafDefinitions.count {
            (categoryCounts[it.key] ?: 0) > 0
        }

        return RootTaxonomySummary(
            key = rootDefinition.key,
            displayName = rootDefinition.displayName,
            categoryCount = subtreeDefinitions.size,
            descendantCategoryCount = descendants.size,
            leafCategoryCount = leafDefinitions.size,
            usedCategoryCount = usedCategoryCount,
            unusedCategoryCount =
                subtreeDefinitions.size - usedCategoryCount,
            usedLeafCategoryCount = usedLeafCategoryCount,
            unusedLeafCategoryCount =
                leafDefinitions.size - usedLeafCategoryCount,
            directCatalogItemCount = directCatalogItemCount,
            descendantCatalogItemCount =
                descendantCatalogItemCount,
            totalCatalogItemCount =
                directCatalogItemCount +
                        descendantCatalogItemCount,
            categoryCoverage = calculateRatio(
                numerator = usedCategoryCount,
                denominator = subtreeDefinitions.size
            ),
            leafCoverage = calculateRatio(
                numerator = usedLeafCategoryCount,
                denominator = leafDefinitions.size
            )
        )
    }

    private fun containsAnyAssignedEntry(
        categoryKey: String,
        registry: CanonicalFoodCategoryRegistry,
        categoryCounts: Map<String, Int>
    ): Boolean {
        if ((categoryCounts[categoryKey] ?: 0) > 0) {
            return true
        }

        return registry.descendants(categoryKey).any {
            (categoryCounts[it.key] ?: 0) > 0
        }
    }

    private fun validateInputs(
        registry: CanonicalFoodCategoryRegistry,
        definitions: List<CatalogCategoryDefinition>,
        categoryResult: CatalogCategoryValidationResult
    ) {
        require(definitions.isNotEmpty()) {
            "Canonical food category registry must not be empty."
        }

        require(
            definitions.map { it.key }.distinct().size ==
                    definitions.size
        ) {
            "Canonical food category registry contains duplicate keys."
        }

        require(categoryResult.inputEntryCount >= 0) {
            "Category validation inputEntryCount must not be negative."
        }

        require(
            categoryResult.categoryCounts.values.all { it >= 0 }
        ) {
            "Category validation categoryCounts must not contain " +
                    "negative values."
        }

        require(
            categoryResult.unknownCategoryCounts.values.all { it >= 0 }
        ) {
            "Category validation unknownCategoryCounts must not contain " +
                    "negative values."
        }

        require(
            categoryResult.categoryCounts.values.sum() ==
                    categoryResult.validCategoryCount
        ) {
            "Category validation categoryCounts are inconsistent with " +
                    "validCategoryCount."
        }

        require(
            categoryResult.unknownCategoryCounts.values.sum() ==
                    categoryResult.unknownCategoryCount
        ) {
            "Category validation unknownCategoryCounts are inconsistent " +
                    "with unknownCategoryCount."
        }

        val unknownRegistryKeys =
            categoryResult.categoryCounts.keys -
                    registry.keys()

        require(unknownRegistryKeys.isEmpty()) {
            "Category validation references unknown canonical category " +
                    "keys: ${unknownRegistryKeys.sorted().joinToString(", ")}"
        }

        val unknownSuggestedTargetKeys =
            categoryResult.suggestedCategoryMappings.values
                .filterNot(registry::contains)
                .distinct()
                .sorted()

        require(unknownSuggestedTargetKeys.isEmpty()) {
            "Category validation contains suggestions to unknown category " +
                    "keys: ${unknownSuggestedTargetKeys.joinToString(", ")}"
        }
    }

    private fun validateReport(
        report: CatalogTaxonomyGapReport
    ) {
        require(report.version > 0) {
            "Taxonomy-gap report version must be greater than zero."
        }

        require(report.registryCategoryCount >= 0)
        require(report.rootCategoryCount >= 0)
        require(report.internalCategoryCount >= 0)
        require(report.leafCategoryCount >= 0)
        require(report.assignedCategoryCount >= 0)
        require(report.unusedCategoryCount >= 0)
        require(report.unusedLeafCategoryCount >= 0)
        require(report.unusedRootCategoryCount >= 0)
        require(report.emptyInternalCategoryCount >= 0)
        require(report.underrepresentedCategoryCount >= 0)
        require(report.rootOnlyAssignedCategoryCount >= 0)
        require(report.unknownCategoryCount >= 0)
        require(report.unknownCategoryValueCount >= 0)

        require(
            report.rootCategoryCount +
                    report.internalCategoryCount +
                    report.leafCategoryCount ==
                    report.registryCategoryCount
        ) {
            "Root, internal and leaf category counts must sum to " +
                    "registryCategoryCount."
        }

        require(
            report.assignedCategoryCount +
                    report.unusedCategoryCount ==
                    report.registryCategoryCount
        ) {
            "assignedCategoryCount plus unusedCategoryCount must equal " +
                    "registryCategoryCount."
        }

        require(
            report.unusedCategoryCount ==
                    report.unusedCategoryKeys.size
        ) {
            "unusedCategoryCount is inconsistent."
        }

        require(
            report.unusedLeafCategoryCount ==
                    report.unusedLeafCategoryKeys.size
        ) {
            "unusedLeafCategoryCount is inconsistent."
        }

        require(
            report.unusedRootCategoryCount ==
                    report.unusedRootCategoryKeys.size
        ) {
            "unusedRootCategoryCount is inconsistent."
        }

        require(
            report.emptyInternalCategoryCount ==
                    report.emptyInternalCategoryKeys.size
        ) {
            "emptyInternalCategoryCount is inconsistent."
        }

        require(
            report.underrepresentedCategoryCount ==
                    report.underrepresentedCategoryKeys.size
        ) {
            "underrepresentedCategoryCount is inconsistent."
        }

        require(
            report.rootOnlyAssignedCategoryCount ==
                    report.rootOnlyAssignedCategoryKeys.size
        ) {
            "rootOnlyAssignedCategoryCount is inconsistent."
        }

        require(
            report.unknownCategoryValueCount ==
                    report.unknownCategories.size
        ) {
            "unknownCategoryValueCount is inconsistent."
        }

        require(
            report.unknownCategoryCount ==
                    report.unknownCategories.sumOf { it.entryCount }
        ) {
            "unknownCategoryCount is inconsistent."
        }

        require(
            report.totalAssignedEntryCount ==
                    report.leafAssignedEntryCount +
                    report.internalAssignedEntryCount +
                    report.rootAssignedEntryCount
        ) {
            "Assigned-entry specificity counts must sum to " +
                    "totalAssignedEntryCount."
        }

        require(
            report.categories.size ==
                    report.registryCategoryCount
        ) {
            "Category entry count must equal registryCategoryCount."
        }

        require(
            report.rootSummaries.size ==
                    report.rootCategoryCount
        ) {
            "Root-summary count must equal rootCategoryCount."
        }

        requireRatio(report.taxonomyCoverage, "taxonomyCoverage")
        requireRatio(
            report.leafTaxonomyCoverage,
            "leafTaxonomyCoverage"
        )
        requireRatio(
            report.entryLeafSpecificity,
            "entryLeafSpecificity"
        )

        requireSortedUnique(
            report.unusedCategoryKeys,
            "unusedCategoryKeys"
        )

        requireSortedUnique(
            report.unusedLeafCategoryKeys,
            "unusedLeafCategoryKeys"
        )

        requireSortedUnique(
            report.unusedRootCategoryKeys,
            "unusedRootCategoryKeys"
        )

        requireSortedUnique(
            report.emptyInternalCategoryKeys,
            "emptyInternalCategoryKeys"
        )

        requireSortedUnique(
            report.underrepresentedCategoryKeys,
            "underrepresentedCategoryKeys"
        )

        requireSortedUnique(
            report.rootOnlyAssignedCategoryKeys,
            "rootOnlyAssignedCategoryKeys"
        )

        require(
            report.categories ==
                    report.categories.sortedWith(
                        TAXONOMY_ENTRY_COMPARATOR
                    )
        ) {
            "Taxonomy category entries must be deterministically sorted."
        }

        require(
            report.rootSummaries ==
                    report.rootSummaries.sortedWith(
                        ROOT_SUMMARY_COMPARATOR
                    )
        ) {
            "Root summaries must be deterministically sorted."
        }

        require(
            report.unknownCategories ==
                    report.unknownCategories.sortedWith(
                        UNKNOWN_CATEGORY_COMPARATOR
                    )
        ) {
            "Unknown categories must be deterministically sorted."
        }

        report.categories.forEach(::validateTaxonomyEntry)
        report.rootSummaries.forEach(::validateRootSummary)
        report.unknownCategories.forEach(::validateUnknownCategory)
    }

    private fun validateTaxonomyEntry(
        entry: TaxonomyCategoryEntry
    ) {
        require(entry.key.isNotBlank())
        require(entry.displayName.isNotBlank())
        require(entry.path.isNotBlank())
        require(entry.depth >= 0)
        require(entry.childCount >= 0)
        require(entry.descendantCount >= 0)
        require(entry.directCatalogItemCount >= 0)
        require(entry.descendantCatalogItemCount >= 0)
        require(entry.totalSubtreeCatalogItemCount >= 0)
        require(entry.assignedDescendantCategoryCount >= 0)
        require(entry.unusedDescendantCategoryCount >= 0)

        require(
            entry.totalSubtreeCatalogItemCount ==
                    entry.directCatalogItemCount +
                    entry.descendantCatalogItemCount
        )

        require(
            entry.assignedDescendantCategoryCount +
                    entry.unusedDescendantCategoryCount ==
                    entry.descendantCount
        )

        require(entry.childCount == entry.childKeys.size)

        requireSortedUnique(
            entry.childKeys,
            "childKeys for ${entry.key}"
        )

        require(entry.root == (entry.parentKey == null))
        require(entry.leaf == (entry.childCount == 0))
        require(entry.used == (entry.directCatalogItemCount > 0))

        require(
            entry.subtreeUsed ==
                    (entry.totalSubtreeCatalogItemCount > 0)
        )

        require(
            entry.underrepresented ==
                    (
                            entry.directCatalogItemCount in
                                    1..UNDERREPRESENTED_CATEGORY_MAXIMUM_COUNT
                            )
        )

        require(
            entry.directlyAssignedDespiteChildren ==
                    (
                            entry.directCatalogItemCount > 0 &&
                                    entry.childCount > 0
                            )
        )
    }

    private fun validateRootSummary(
        summary: RootTaxonomySummary
    ) {
        require(summary.key.isNotBlank())
        require(summary.displayName.isNotBlank())
        require(summary.categoryCount > 0)
        require(summary.descendantCategoryCount >= 0)
        require(summary.leafCategoryCount >= 0)
        require(summary.usedCategoryCount >= 0)
        require(summary.unusedCategoryCount >= 0)
        require(summary.usedLeafCategoryCount >= 0)
        require(summary.unusedLeafCategoryCount >= 0)
        require(summary.directCatalogItemCount >= 0)
        require(summary.descendantCatalogItemCount >= 0)
        require(summary.totalCatalogItemCount >= 0)

        require(
            summary.usedCategoryCount +
                    summary.unusedCategoryCount ==
                    summary.categoryCount
        )

        require(
            summary.usedLeafCategoryCount +
                    summary.unusedLeafCategoryCount ==
                    summary.leafCategoryCount
        )

        require(
            summary.totalCatalogItemCount ==
                    summary.directCatalogItemCount +
                    summary.descendantCatalogItemCount
        )

        requireRatio(
            summary.categoryCoverage,
            "categoryCoverage for ${summary.key}"
        )

        requireRatio(
            summary.leafCoverage,
            "leafCoverage for ${summary.key}"
        )
    }

    private fun validateUnknownCategory(
        category: UnknownTaxonomyCategory
    ) {
        require(category.category.isNotBlank())
        require(category.entryCount > 0)
        require(
            category.suggestedCategoryKey == null ||
                    category.suggestedCategoryKey.isNotBlank()
        )
    }

    private fun requireSortedUnique(
        values: List<String>,
        fieldName: String
    ) {
        require(values == values.sorted()) {
            "$fieldName must be sorted."
        }

        require(values.distinct().size == values.size) {
            "$fieldName must not contain duplicates."
        }
    }

    private fun requireRatio(
        value: Double,
        fieldName: String
    ) {
        require(value.isFinite()) {
            "$fieldName must be finite."
        }

        require(value in MINIMUM_RATIO..MAXIMUM_RATIO) {
            "$fieldName must be between $MINIMUM_RATIO and " +
                    "$MAXIMUM_RATIO, but was $value."
        }
    }

    private fun calculateRatio(
        numerator: Int,
        denominator: Int
    ): Double {
        require(numerator >= 0)
        require(denominator >= 0)
        require(numerator <= denominator || denominator == 0)

        if (denominator == 0) {
            return 1.0
        }

        return normalizeRatio(
            numerator.toDouble() /
                    denominator.toDouble()
        )
    }

    private fun normalizeRatio(
        value: Double
    ): Double =
        String.format(
            Locale.ROOT,
            RATIO_FORMAT,
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

    private data class CatalogTaxonomyGapReport(
        val version: Int,
        val registryCategoryCount: Int,
        val rootCategoryCount: Int,
        val internalCategoryCount: Int,
        val leafCategoryCount: Int,
        val assignedCategoryCount: Int,
        val unusedCategoryCount: Int,
        val unusedLeafCategoryCount: Int,
        val unusedRootCategoryCount: Int,
        val emptyInternalCategoryCount: Int,
        val underrepresentedCategoryCount: Int,
        val rootOnlyAssignedCategoryCount: Int,
        val unknownCategoryCount: Int,
        val unknownCategoryValueCount: Int,
        val totalAssignedEntryCount: Int,
        val leafAssignedEntryCount: Int,
        val internalAssignedEntryCount: Int,
        val rootAssignedEntryCount: Int,
        val taxonomyCoverage: Double,
        val leafTaxonomyCoverage: Double,
        val entryLeafSpecificity: Double,
        val unusedCategoryKeys: List<String>,
        val unusedLeafCategoryKeys: List<String>,
        val unusedRootCategoryKeys: List<String>,
        val emptyInternalCategoryKeys: List<String>,
        val underrepresentedCategoryKeys: List<String>,
        val rootOnlyAssignedCategoryKeys: List<String>,
        val unknownCategories: List<UnknownTaxonomyCategory>,
        val rootSummaries: List<RootTaxonomySummary>,
        val categories: List<TaxonomyCategoryEntry>,
        val valid: Boolean
    )

    private data class TaxonomyCategoryEntry(
        val key: String,
        val displayName: String,
        val parentKey: String?,
        val path: String,
        val depth: Int,
        val root: Boolean,
        val leaf: Boolean,
        val childCount: Int,
        val descendantCount: Int,
        val directCatalogItemCount: Int,
        val descendantCatalogItemCount: Int,
        val totalSubtreeCatalogItemCount: Int,
        val assignedDescendantCategoryCount: Int,
        val unusedDescendantCategoryCount: Int,
        val used: Boolean,
        val subtreeUsed: Boolean,
        val underrepresented: Boolean,
        val directlyAssignedDespiteChildren: Boolean,
        val childKeys: List<String>
    )

    private data class RootTaxonomySummary(
        val key: String,
        val displayName: String,
        val categoryCount: Int,
        val descendantCategoryCount: Int,
        val leafCategoryCount: Int,
        val usedCategoryCount: Int,
        val unusedCategoryCount: Int,
        val usedLeafCategoryCount: Int,
        val unusedLeafCategoryCount: Int,
        val directCatalogItemCount: Int,
        val descendantCatalogItemCount: Int,
        val totalCatalogItemCount: Int,
        val categoryCoverage: Double,
        val leafCoverage: Double
    )

    private data class UnknownTaxonomyCategory(
        val category: String,
        val entryCount: Int,
        val suggestedCategoryKey: String?
    )

    private companion object {

        const val CURRENT_VERSION = 1
        const val UNDERREPRESENTED_CATEGORY_MAXIMUM_COUNT = 2

        const val MINIMUM_RATIO = 0.0
        const val MAXIMUM_RATIO = 1.0
        const val RATIO_FORMAT = "%.6f"

        val DEFINITION_COMPARATOR =
            compareBy<CatalogCategoryDefinition>(
                { it.parentKey ?: "" },
                {
                    it.displayName.lowercase(
                        Locale.ROOT
                    )
                },
                { it.key }
            )

        val TAXONOMY_ENTRY_COMPARATOR =
            compareBy<TaxonomyCategoryEntry>(
                { it.depth },
                { it.path.lowercase(Locale.ROOT) },
                { it.key }
            )

        val ROOT_SUMMARY_COMPARATOR =
            compareBy<RootTaxonomySummary>(
                {
                    it.displayName.lowercase(
                        Locale.ROOT
                    )
                },
                { it.key }
            )

        val UNKNOWN_CATEGORY_COMPARATOR =
            compareByDescending<UnknownTaxonomyCategory> {
                it.entryCount
            }
                .thenBy {
                    it.category.lowercase(Locale.ROOT)
                }

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
    }
}