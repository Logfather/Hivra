package de.shopme.testing.system.tools.knowledge.catalog.application

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlan
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlanEntry
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.category.migration.CatalogCategoryMigrationStatus
import de.shopme.testing.system.tools.knowledge.catalog.category.migration.CatalogLegacyCategoryMigrator
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.CatalogDeterministicDuplicateResolver
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationResult
import java.util.Locale

class CatalogCanonicalizationPlanApplier(
    private val categoryMigrator: CatalogLegacyCategoryMigrator,
    private val categoryRegistry: CanonicalFoodCategoryRegistry,
    private val duplicateResolver:
    CatalogDeterministicDuplicateResolver
) {

    fun apply(
        entries: List<IndexedCatalogFoodItem>,
        normalizations: List<CatalogNormalizationResult>,
        plan: CatalogCanonicalizationPlan
    ): CatalogCanonicalizationApplicationResult {
        validateInputs(
            entries = entries,
            normalizations = normalizations,
            plan = plan
        )

        val entriesBySourceIndex =
            entries.associateBy { it.sourceIndex }

        val normalizationsBySourceIndex =
            normalizations.associateBy { it.sourceIndex }

        val planEntriesBySourceIndex =
            plan.entries.associateBy { it.sourceIndex }

        val outputBySourceIndex =
            linkedMapOf<Int, CatalogFoodItem>()

        val applicationEntries =
            mutableListOf<CatalogCanonicalizationApplicationEntry>()

        entries
            .sortedBy { it.sourceIndex }
            .forEach { indexedEntry ->
                val sourceIndex = indexedEntry.sourceIndex

                val planEntry =
                    planEntriesBySourceIndex.getValue(sourceIndex)

                val normalization =
                    normalizationsBySourceIndex.getValue(sourceIndex)

                val applicationEntry = applyEntry(
                    indexedEntry = indexedEntry,
                    normalization = normalization,
                    planEntry = planEntry,
                    outputBySourceIndex = outputBySourceIndex,
                    entriesBySourceIndex = entriesBySourceIndex
                )

                applicationEntries += applicationEntry
            }

        val duplicateResolution = duplicateResolver.resolve(
            itemsBySourceIndex = outputBySourceIndex,
            plan = plan
        )

        val duplicateDecisionsBySourceIndex =
            duplicateResolution.decisions.associateBy {
                it.sourceIndex
            }

        val resolvedApplicationEntries =
            applicationEntries.map { applicationEntry ->
                val duplicateDecision =
                    duplicateDecisionsBySourceIndex[
                        applicationEntry.sourceIndex
                    ]

                if (duplicateDecision == null) {
                    applicationEntry
                } else {
                    applicationEntry.copy(
                        status =
                            CatalogCanonicalizationApplicationStatus
                                .MERGED_INTO_TARGET,
                        resultingNormalizedKey = null,
                        resultingCategory = null,
                        mergeTargetSourceIndex =
                            duplicateDecision.targetSourceIndex,
                        reasons = (
                                applicationEntry.reasons +
                                        buildString {
                                            append(
                                                "Deterministic duplicate resolution: "
                                            )
                                            append(
                                                duplicateDecision.reason.name
                                            )
                                            append(" -> sourceIndex ")
                                            append(
                                                duplicateDecision.targetSourceIndex
                                            )
                                        }
                                )
                            .distinct()
                            .sorted()
                    )
                }
            }

        /*
         * Source-Index-Zuordnung nach der deterministischen Duplicate-Auflösung.
         *
         * Die Werte werden hier erneut normalisiert, damit die Map exakt denselben
         * finalen Datenstand wie outputItems repräsentiert.
         *
         * toSortedMap() garantiert eine stabile Reihenfolge nach sourceIndex.
         */
        val outputItemsBySourceIndex =
            duplicateResolution
                .itemsBySourceIndex
                .toSortedMap()
                .mapValues { (_, item) ->
                    normalizeOutputItem(item)
                }

        /*
         * Die persistierte Katalogliste besitzt weiterhin ihre fachliche Sortierung.
         * Sie wird aus derselben finalen Map abgeleitet, damit Liste und Map niemals
         * auseinanderlaufen können.
         */
        val outputItems =
            outputItemsBySourceIndex
                .values
                .sortedWith(
                    OUTPUT_ITEM_COMPARATOR
                )

        val sortedApplicationEntries =
            resolvedApplicationEntries.sortedBy {
                it.sourceIndex
            }

        val failedEntryCount = sortedApplicationEntries.count {
            it.status ==
                    CatalogCanonicalizationApplicationStatus.FAILED
        }

        return CatalogCanonicalizationApplicationResult(
            version =
                CatalogCanonicalizationApplicationResult
                    .CURRENT_VERSION,

            inputEntryCount =
                entries.size,

            outputEntryCount =
                outputItems.size,

            appliedEntryCount =
                sortedApplicationEntries.count {
                    it.status ==
                            CatalogCanonicalizationApplicationStatus
                                .APPLIED
                },

            skippedReviewEntryCount =
                sortedApplicationEntries.count {
                    it.status ==
                            CatalogCanonicalizationApplicationStatus
                                .SKIPPED_REVIEW_REQUIRED
                },

            removedEntryCount =
                sortedApplicationEntries.count {
                    it.status ==
                            CatalogCanonicalizationApplicationStatus
                                .REMOVED
                },

            mergedEntryCount =
                sortedApplicationEntries.count {
                    it.status ==
                            CatalogCanonicalizationApplicationStatus
                                .MERGED_INTO_TARGET
                },

            failedEntryCount =
                failedEntryCount,

            outputItems =
                outputItems,

            outputItemsBySourceIndex =
                outputItemsBySourceIndex,

            entries =
                sortedApplicationEntries,

            valid =
                failedEntryCount == 0
        )
    }

    private fun applyEntry(
        indexedEntry: IndexedCatalogFoodItem,
        normalization: CatalogNormalizationResult,
        planEntry: CatalogCanonicalizationPlanEntry,
        outputBySourceIndex: MutableMap<Int, CatalogFoodItem>,
        entriesBySourceIndex: Map<Int, IndexedCatalogFoodItem>
    ): CatalogCanonicalizationApplicationEntry {
        val sourceIndex = indexedEntry.sourceIndex
        val originalItem = indexedEntry.item

        /*
         * Diese Baseline ist deterministisch und nicht destruktiv.
         *
         * Sie darf deshalb unabhängig von der späteren semantischen
         * Entscheidung angewendet werden. Ein REVIEW blockiert nur Aktionen
         * wie Merge, Remove, Split oder eine unsichere Kategoriezuordnung.
         */
        val baselineNormalizedItem = createBaselineNormalizedItem(
            originalItem = originalItem,
            normalization = normalization
        )

        return when (planEntry.action) {
            CatalogCanonicalizationAction.KEEP -> {
                putOutputItem(
                    sourceIndex = sourceIndex,
                    item = baselineNormalizedItem,
                    outputBySourceIndex = outputBySourceIndex
                )

                applicationEntry(
                    indexedEntry = indexedEntry,
                    planEntry = planEntry,
                    status =
                        CatalogCanonicalizationApplicationStatus.APPLIED,
                    resultingItem =
                        outputBySourceIndex.getValue(sourceIndex)
                )
            }

            CatalogCanonicalizationAction.NORMALIZE,
            CatalogCanonicalizationAction.RENAME -> {
                val resultingItem = baselineNormalizedItem.copy(
                    itemname =
                        planEntry.proposedCanonicalName
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                            ?: normalization.computedCanonicalName,
                    normalized =
                        planEntry.proposedNormalizedKey
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                            ?: normalization.computedNormalizedKey
                )

                putOutputItem(
                    sourceIndex = sourceIndex,
                    item = resultingItem,
                    outputBySourceIndex = outputBySourceIndex
                )

                applicationEntry(
                    indexedEntry = indexedEntry,
                    planEntry = planEntry,
                    status =
                        CatalogCanonicalizationApplicationStatus.APPLIED,
                    resultingItem =
                        outputBySourceIndex.getValue(sourceIndex)
                )
            }

            CatalogCanonicalizationAction.MOVE_CATEGORY -> {
                val proposedCategory = planEntry.proposedCategory
                    ?.trim()
                    ?.takeIf(String::isNotBlank)

                if (planEntry.automatic && proposedCategory != null) {
                    val resultingItem = baselineNormalizedItem.copy(
                        category = proposedCategory
                    )

                    putOutputItem(
                        sourceIndex = sourceIndex,
                        item = resultingItem,
                        outputBySourceIndex = outputBySourceIndex
                    )

                    applicationEntry(
                        indexedEntry = indexedEntry,
                        planEntry = planEntry,
                        status =
                            CatalogCanonicalizationApplicationStatus.APPLIED,
                        resultingItem =
                            outputBySourceIndex.getValue(sourceIndex)
                    )
                } else {
                    /*
                     * Die unsichere Kategorieverschiebung wird nicht
                     * angewendet. Die sichere technische Baseline bleibt
                     * jedoch erhalten.
                     */
                    putOutputItem(
                        sourceIndex = sourceIndex,
                        item = baselineNormalizedItem,
                        outputBySourceIndex = outputBySourceIndex
                    )

                    applicationEntry(
                        indexedEntry = indexedEntry,
                        planEntry = planEntry,
                        status =
                            CatalogCanonicalizationApplicationStatus
                                .SKIPPED_REVIEW_REQUIRED,
                        resultingItem =
                            outputBySourceIndex.getValue(sourceIndex)
                    )
                }
            }

            CatalogCanonicalizationAction.REMOVE_NON_FOOD -> {
                if (planEntry.automatic) {
                    CatalogCanonicalizationApplicationEntry(
                        sourceIndex = sourceIndex,
                        originalItemName = originalItem.itemname,
                        action = planEntry.action,
                        automatic = true,
                        status =
                            CatalogCanonicalizationApplicationStatus.REMOVED,
                        resultingNormalizedKey = null,
                        resultingCategory = null,
                        mergeTargetSourceIndex = null,
                        reasons = normalizedReasons(planEntry)
                    )
                } else {
                    /*
                     * Der Eintrag bleibt erhalten, wird aber technisch
                     * vollständig normalisiert.
                     */
                    putOutputItem(
                        sourceIndex = sourceIndex,
                        item = baselineNormalizedItem,
                        outputBySourceIndex = outputBySourceIndex
                    )

                    applicationEntry(
                        indexedEntry = indexedEntry,
                        planEntry = planEntry,
                        status =
                            CatalogCanonicalizationApplicationStatus
                                .SKIPPED_REVIEW_REQUIRED,
                        resultingItem =
                            outputBySourceIndex.getValue(sourceIndex)
                    )
                }
            }

            CatalogCanonicalizationAction.MERGE -> {
                val targetSourceIndex =
                    requireNotNull(
                        planEntry.mergeTargetSourceIndex
                    ) {
                        "MERGE entry at sourceIndex $sourceIndex " +
                                "does not contain mergeTargetSourceIndex."
                    }

                require(targetSourceIndex in entriesBySourceIndex) {
                    "MERGE entry at sourceIndex $sourceIndex references " +
                            "unknown target $targetSourceIndex."
                }

                require(targetSourceIndex != sourceIndex) {
                    "MERGE entry at sourceIndex $sourceIndex must not " +
                            "target itself."
                }

                if (planEntry.automatic) {
                    CatalogCanonicalizationApplicationEntry(
                        sourceIndex = sourceIndex,
                        originalItemName = originalItem.itemname,
                        action = planEntry.action,
                        automatic = true,
                        status =
                            CatalogCanonicalizationApplicationStatus
                                .MERGED_INTO_TARGET,
                        resultingNormalizedKey = null,
                        resultingCategory = null,
                        mergeTargetSourceIndex = targetSourceIndex,
                        reasons = normalizedReasons(planEntry)
                    )
                } else {
                    /*
                     * Der unsichere Merge wird nicht ausgeführt. Der
                     * Quellkatalogeintrag bleibt als eigener Datensatz
                     * bestehen, erhält aber die sichere Baseline.
                     */
                    putOutputItem(
                        sourceIndex = sourceIndex,
                        item = baselineNormalizedItem,
                        outputBySourceIndex = outputBySourceIndex
                    )

                    applicationEntry(
                        indexedEntry = indexedEntry,
                        planEntry = planEntry,
                        status =
                            CatalogCanonicalizationApplicationStatus
                                .SKIPPED_REVIEW_REQUIRED,
                        resultingItem =
                            outputBySourceIndex.getValue(sourceIndex)
                    )
                }
            }

            CatalogCanonicalizationAction.SPLIT -> {
                /*
                 * Splitten erzeugt neue semantische Identitäten und darf
                 * deshalb nicht automatisch erfolgen. Der bestehende
                 * Datensatz wird aber technisch normalisiert ausgegeben.
                 */
                putOutputItem(
                    sourceIndex = sourceIndex,
                    item = baselineNormalizedItem,
                    outputBySourceIndex = outputBySourceIndex
                )

                applicationEntry(
                    indexedEntry = indexedEntry,
                    planEntry = planEntry,
                    status =
                        CatalogCanonicalizationApplicationStatus
                            .SKIPPED_REVIEW_REQUIRED,
                    resultingItem =
                        outputBySourceIndex.getValue(sourceIndex)
                )
            }

            CatalogCanonicalizationAction.REVIEW -> {
                /*
                 * REVIEW bedeutet ausschließlich:
                 *
                 * Die semantische Entscheidung ist noch offen.
                 *
                 * Es bedeutet ausdrücklich nicht:
                 *
                 * Der technische Altzustand muss unverändert bleiben.
                 */
                putOutputItem(
                    sourceIndex = sourceIndex,
                    item = baselineNormalizedItem,
                    outputBySourceIndex = outputBySourceIndex
                )

                applicationEntry(
                    indexedEntry = indexedEntry,
                    planEntry = planEntry,
                    status =
                        CatalogCanonicalizationApplicationStatus
                            .SKIPPED_REVIEW_REQUIRED,
                    resultingItem =
                        outputBySourceIndex.getValue(sourceIndex)
                )
            }
        }
    }

    private fun createBaselineNormalizedItem(
        originalItem: CatalogFoodItem,
        normalization: CatalogNormalizationResult
    ): CatalogFoodItem {
        val technicallyNormalizedItem = originalItem.copy(
            itemname = normalization.computedCanonicalName,
            normalized = normalization.computedNormalizedKey,
            plural = normalization.computedPlural,
            colloquial = normalization.normalizedColloquial,
            phoneticTokens =
                normalization.normalizedPhoneticTokens,
            autocompleteTokens =
                normalization.normalizedAutocompleteTokens
        )

        val categoryMigration = categoryMigrator.migrate(
            item = technicallyNormalizedItem,
            registry = categoryRegistry
        )

        val resultingCategory = when (
            categoryMigration.status
        ) {
            CatalogCategoryMigrationStatus.ALREADY_CANONICAL,
            CatalogCategoryMigrationStatus.MIGRATED_DIRECTLY,
            CatalogCategoryMigrationStatus.MIGRATED_BY_PRODUCT_RULE ->
                categoryMigration.resultingCategory

            CatalogCategoryMigrationStatus.UNRESOLVED ->
                technicallyNormalizedItem.category
        }

        return technicallyNormalizedItem.copy(
            category = resultingCategory
        )
    }

    private fun putOutputItem(
        sourceIndex: Int,
        item: CatalogFoodItem,
        outputBySourceIndex: MutableMap<Int, CatalogFoodItem>
    ) {
        require(sourceIndex !in outputBySourceIndex) {
            "Output already contains sourceIndex $sourceIndex."
        }

        outputBySourceIndex[sourceIndex] =
            normalizeOutputItem(item)
    }

    private fun applicationEntry(
        indexedEntry: IndexedCatalogFoodItem,
        planEntry: CatalogCanonicalizationPlanEntry,
        status: CatalogCanonicalizationApplicationStatus,
        resultingItem: CatalogFoodItem
    ): CatalogCanonicalizationApplicationEntry =
        CatalogCanonicalizationApplicationEntry(
            sourceIndex = indexedEntry.sourceIndex,
            originalItemName = indexedEntry.item.itemname,
            action = planEntry.action,
            automatic = planEntry.automatic,
            status = status,
            resultingNormalizedKey =
                resultingItem.normalized,
            resultingCategory =
                resultingItem.category,
            mergeTargetSourceIndex =
                planEntry.mergeTargetSourceIndex,
            reasons = normalizedReasons(planEntry)
        )

    private fun normalizedReasons(
        planEntry: CatalogCanonicalizationPlanEntry
    ): List<String> =
        planEntry.reasons
            .map(::normalizeText)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    private fun normalizeOutputItem(
        item: CatalogFoodItem
    ): CatalogFoodItem =
        item.copy(
            itemname = normalizeText(item.itemname),
            category = item.category
                ?.let(::normalizeText)
                ?.takeIf(String::isNotBlank),
            production = item.production
                ?.let(::normalizeText)
                ?.takeIf(String::isNotBlank),
            normalized = item.normalized
                ?.trim()
                ?.takeIf(String::isNotBlank),
            plural = item.plural
                ?.let(::normalizeText)
                ?.takeIf(String::isNotBlank),
            colloquial = normalizeDisplayValues(
                item.colloquial
            ),
            phoneticTokens = normalizeTechnicalValues(
                item.phoneticTokens
            ),
            autocompleteTokens = normalizeTechnicalValues(
                item.autocompleteTokens
            ),
            normalizedEnglish = item.normalizedEnglish
                ?.let(::normalizeText)
                ?.takeIf(String::isNotBlank)
        )

    private fun normalizeText(
        value: String
    ): String =
        value
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")

    private fun normalizeDisplayValues(
        values: List<String>
    ): List<String> =
        values
            .map(::normalizeText)
            .filter(String::isNotBlank)
            .distinctBy {
                it.lowercase(Locale.GERMAN)
            }
            .sortedWith(
                compareBy<String>(
                    { it.lowercase(Locale.GERMAN) },
                    { it }
                )
            )

    private fun normalizeTechnicalValues(
        values: List<String>
    ): List<String> =
        values
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    private fun validateInputs(
        entries: List<IndexedCatalogFoodItem>,
        normalizations: List<CatalogNormalizationResult>,
        plan: CatalogCanonicalizationPlan
    ) {
        require(
            entries.map { it.sourceIndex }.distinct().size ==
                    entries.size
        ) {
            "Catalog entries contain duplicate sourceIndex values."
        }

        require(
            normalizations.map { it.sourceIndex }.distinct().size ==
                    normalizations.size
        ) {
            "Catalog normalizations contain duplicate sourceIndex values."
        }

        require(
            plan.entries.map { it.sourceIndex }.distinct().size ==
                    plan.entries.size
        ) {
            "Canonicalization plan contains duplicate sourceIndex values."
        }

        val entryIndices =
            entries.mapTo(sortedSetOf()) { it.sourceIndex }

        val normalizationIndices =
            normalizations.mapTo(sortedSetOf()) { it.sourceIndex }

        val planIndices =
            plan.entries.mapTo(sortedSetOf()) { it.sourceIndex }

        require(entryIndices == normalizationIndices) {
            "Catalog entries and normalizations must cover identical " +
                    "source indices."
        }

        require(entryIndices == planIndices) {
            "Catalog entries and canonicalization plan must cover " +
                    "identical source indices."
        }

        require(plan.inputEntryCount == entries.size) {
            "Canonicalization plan inputEntryCount is inconsistent."
        }

        require(plan.planEntryCount == plan.entries.size) {
            "Canonicalization plan planEntryCount is inconsistent."
        }

        require(plan.valid) {
            "Canonicalization plan must be structurally valid."
        }

        plan.entries
            .filter {
                it.action ==
                        CatalogCanonicalizationAction.MERGE
            }
            .forEach { mergeEntry ->
                val targetSourceIndex = requireNotNull(
                    mergeEntry.mergeTargetSourceIndex
                ) {
                    "MERGE entry at sourceIndex " +
                            "${mergeEntry.sourceIndex} has no merge target."
                }

                require(targetSourceIndex in entryIndices) {
                    "MERGE entry at sourceIndex " +
                            "${mergeEntry.sourceIndex} references unknown " +
                            "target $targetSourceIndex."
                }

                require(
                    targetSourceIndex != mergeEntry.sourceIndex
                ) {
                    "MERGE entry at sourceIndex " +
                            "${mergeEntry.sourceIndex} targets itself."
                }
            }
    }

    private companion object {

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        val OUTPUT_ITEM_COMPARATOR =
            compareBy<CatalogFoodItem>(
                {
                    it.category
                        ?.lowercase(Locale.ROOT)
                        ?: ""
                },
                {
                    it.itemname.lowercase(
                        Locale.GERMAN
                    )
                },
                {
                    it.normalized
                        ?.lowercase(Locale.ROOT)
                        ?: ""
                }
            )
    }
}