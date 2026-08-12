package de.shopme.tools.knowledge.catalog.canonical.rebuild

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.catalog.canonical.rebuild.io.LegacyCanonicalFoodCatalogReader
import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.CanonicalFoodCatalogRebuildAction
import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.CanonicalFoodCatalogRebuildDecision
import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.CanonicalFoodIdentity
import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.LegacyCanonicalFoodItem
import java.io.File

data class CanonicalFoodCatalogRebuildReport(
    val version: Int,
    val inputEntryCount: Int,
    val syntheticRejectedCount: Int,
    val brandRejectedCount: Int,
    val semanticGroupCount: Int,
    val outputEntryCount: Int,
    val collapsedEntryCount: Int,
    val sourceVariantCount: Int,
    val variantCount: Int,
    val outputFile: String,
    val decisionsFile: String
)

class RebuildCanonicalFoodCatalog(
    private val reader:
    LegacyCanonicalFoodCatalogReader =
        LegacyCanonicalFoodCatalogReader(),

    private val nameSelector:
    CanonicalGermanFoodNameSelector =
        CanonicalGermanFoodNameSelector(),

    private val variantExtractor:
    CanonicalFoodVariantExtractor =
        CanonicalFoodVariantExtractor(),

    private val brandDetector:
    CanonicalFoodBrandLeakDetector =
        CanonicalFoodBrandLeakDetector()
) {

    fun rebuild(
        inputFile: File,
        outputFile: File,
        decisionsFile: File,
        reportFile: File
    ): CanonicalFoodCatalogRebuildReport {

        val input =
            reader.read(
                inputFile
            )

        require(
            input.isNotEmpty()
        )

        val decisions =
            mutableListOf<
                    CanonicalFoodCatalogRebuildDecision
                    >()

        /*
         * Der bekannte 1.270er Generatorblock wird nicht
         * als Source-Evidenz recycelt.
         *
         * Diese Einträge haben ihr Vertrauen verloren.
         */
        val nonSynthetic =
            input.filter { item ->

                if (
                    isSyntheticExpansion(
                        item
                    )
                ) {

                    decisions +=
                        decision(
                            item =
                                item,
                            action =
                                CanonicalFoodCatalogRebuildAction
                                    .REJECT_SYNTHETIC,
                            reason =
                                "Generated Family–Variant expansion entry."
                        )

                    false

                } else {

                    true
                }
            }

        val withoutBrands =
            nonSynthetic.filter { item ->

                if (
                    brandDetector
                        .isBrandIdentity(
                            item.itemname
                        )
                ) {

                    decisions +=
                        decision(
                            item =
                                item,
                            action =
                                CanonicalFoodCatalogRebuildAction
                                    .REJECT_BRAND,
                            reason =
                                "Brand/product-brand leaked into canonical identity."
                        )

                    false

                } else {

                    true
                }
            }

        /*
         * normalizedEnglish wird nur noch als
         * Legacy-Semantic-Evidence benutzt.
         *
         * Fehlt es, fällt die Gruppe auf den deutschen
         * normalized key zurück.
         */
        val semanticGroups =
            withoutBrands
                .groupBy { item ->

                    item
                        .normalizedEnglish
                        ?.trim()
                        ?.lowercase()
                        ?.takeIf(
                            String::isNotBlank
                        )
                        ?.let {
                            "en:$it"
                        }
                        ?: "de:${item.normalized}"
                }
                .toSortedMap()

        val rebuilt =
            semanticGroups
                .values
                .map { group ->

                    rebuildGroup(
                        group =
                            group,
                        decisions =
                            decisions
                    )
                }
                .sortedBy {
                    it.normalized
                }

        validateOutput(
            entries =
                rebuilt
        )

        writeJson(
            value =
                rebuilt,
            file =
                outputFile
        )

        writeJson(
            value =
                decisions.sortedWith(
                    compareBy<
                            CanonicalFoodCatalogRebuildDecision
                            > {
                        it.sourceNormalized
                    }
                        .thenBy {
                            it.action.name
                        }
                ),
            file =
                decisionsFile
        )

        val syntheticRejectedCount =
            decisions.count {
                it.action ==
                        CanonicalFoodCatalogRebuildAction
                            .REJECT_SYNTHETIC
            }

        val brandRejectedCount =
            decisions.count {
                it.action ==
                        CanonicalFoodCatalogRebuildAction
                            .REJECT_BRAND
            }

        val report =
            CanonicalFoodCatalogRebuildReport(
                version =
                    1,

                inputEntryCount =
                    input.size,

                syntheticRejectedCount =
                    syntheticRejectedCount,

                brandRejectedCount =
                    brandRejectedCount,

                semanticGroupCount =
                    semanticGroups.size,

                outputEntryCount =
                    rebuilt.size,

                collapsedEntryCount =
                    input.size -
                            rebuilt.size,

                sourceVariantCount =
                    rebuilt.sumOf {
                        it.sourceVariants.size
                    },

                variantCount =
                    rebuilt.sumOf {
                        it.variants.size
                    },

                outputFile =
                    outputFile.path,

                decisionsFile =
                    decisionsFile.path
            )

        writeJson(
            value =
                report,
            file =
                reportFile
        )

        printReport(
            report =
                report,
            reportFile =
                reportFile
        )

        return report
    }

    private fun rebuildGroup(
        group: List<LegacyCanonicalFoodItem>,
        decisions:
        MutableList<
                CanonicalFoodCatalogRebuildDecision
                >
    ): CanonicalFoodIdentity {

        require(
            group.isNotEmpty()
        )

        val canonical =
            nameSelector.select(
                group
            )

        val sourceVariants =
            group
                .asSequence()
                .map {
                    it.itemname.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .filter {
                    it !=
                            canonical.itemname
                }
                .distinct()
                .sorted()
                .toList()

        val variants =
            variantExtractor.extract(
                sourceNames =
                    group.map {
                        it.itemname
                    }
            )

        group.forEach { item ->

            decisions +=
                CanonicalFoodCatalogRebuildDecision(
                    sourceItemname =
                        item.itemname,

                    sourceNormalized =
                        item.normalized,

                    sourceCategory =
                        item.category,

                    action =
                        if (
                            item ==
                            canonical
                        ) {
                            CanonicalFoodCatalogRebuildAction
                                .KEEP_AS_CANONICAL
                        } else {
                            CanonicalFoodCatalogRebuildAction
                                .MERGE_AS_SOURCE_VARIANT
                        },

                    targetNormalized =
                        canonical.normalized,

                    reason =
                        if (
                            item ==
                            canonical
                        ) {
                            "Selected canonical representative of semantic group."
                        } else {
                            "Same legacy semantic identity; retained as sourceVariant."
                        }
                )
        }

        val category =
            selectCategory(
                group =
                    group,
                canonical =
                    canonical
            )

        return CanonicalFoodIdentity(
            itemname =
                canonical.itemname,

            normalized =
                canonical.normalized,

            category =
                category,

            variants =
                variants,

            sourceVariants =
                sourceVariants
        )
    }

    private fun selectCategory(
        group: List<LegacyCanonicalFoodItem>,
        canonical: LegacyCanonicalFoodItem
    ): String {

        val counts =
            group
                .groupingBy {
                    it.category
                }
                .eachCount()

        return counts
            .entries
            .sortedWith(
                compareByDescending<
                        Map.Entry<String, Int>
                        > {
                    it.value
                }
                    .thenByDescending {
                        if (
                            it.key ==
                            canonical.category
                        ) {
                            1
                        } else {
                            0
                        }
                    }
                    .thenBy {
                        it.key
                    }
            )
            .first()
            .key
    }

    private fun isSyntheticExpansion(
        item: LegacyCanonicalFoodItem
    ): Boolean {

        /*
         * Der aktuelle Generatorblock besitzt genau
         * dieses charakteristische Muster:
         *
         *   Family – Variant
         *   normalizedEnglish fehlt
         *
         * Beide Bedingungen müssen erfüllt sein.
         */
        return item.itemname.contains(
            SYNTHETIC_SEPARATOR
        ) &&
                item.normalizedEnglish ==
                null
    }

    private fun validateOutput(
        entries: List<CanonicalFoodIdentity>
    ) {

        require(
            entries.isNotEmpty()
        )

        require(
            entries
                .map {
                    it.normalized
                }
                .distinct()
                .size ==
                    entries.size
        ) {
            "Duplicate normalized keys in rebuilt catalog."
        }

        entries.forEach { entry ->

            require(
                entry.itemname.isNotBlank()
            )

            require(
                entry.normalized.isNotBlank()
            )

            require(
                entry.category.isNotBlank()
            )

            require(
                entry.sourceVariants
                    .none {
                        it ==
                                entry.itemname
                    }
            ) {
                "Canonical itemname must not repeat in sourceVariants: " +
                        entry.normalized
            }

            require(
                entry.sourceVariants.size ==
                        entry.sourceVariants
                            .distinct()
                            .size
            )

            require(
                entry.variants.size ==
                        entry.variants
                            .distinct()
                            .size
            )
        }
    }

    private fun decision(
        item: LegacyCanonicalFoodItem,
        action: CanonicalFoodCatalogRebuildAction,
        reason: String
    ): CanonicalFoodCatalogRebuildDecision =
        CanonicalFoodCatalogRebuildDecision(
            sourceItemname =
                item.itemname,

            sourceNormalized =
                item.normalized,

            sourceCategory =
                item.category,

            action =
                action,

            targetNormalized =
                null,

            reason =
                reason
        )

    private fun writeJson(
        value: Any,
        file: File
    ) {

        val parent =
            requireNotNull(
                file.parentFile
            )

        require(
            parent.exists() ||
                    parent.mkdirs()
        )

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
    }

    private fun printReport(
        report: CanonicalFoodCatalogRebuildReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CANONICAL FOOD CATALOG REBUILD")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Input entries          : " +
                    report.inputEntryCount
        )
        println(
            "Synthetic rejected     : " +
                    report.syntheticRejectedCount
        )
        println(
            "Brands rejected        : " +
                    report.brandRejectedCount
        )
        println(
            "Semantic groups        : " +
                    report.semanticGroupCount
        )
        println(
            "Output identities      : " +
                    report.outputEntryCount
        )
        println(
            "Collapsed entries      : " +
                    report.collapsedEntryCount
        )
        println(
            "Variants               : " +
                    report.variantCount
        )
        println(
            "Source variants        : " +
                    report.sourceVariantCount
        )
        println()
        println(
            "Catalog                : " +
                    report.outputFile
        )
        println(
            "Decisions              : " +
                    report.decisionsFile
        )
        println(
            "Report                 : " +
                    reportFile.path
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    companion object {

        private const val SYNTHETIC_SEPARATOR =
            " – "

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}