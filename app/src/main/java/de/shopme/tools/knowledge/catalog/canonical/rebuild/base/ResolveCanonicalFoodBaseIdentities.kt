package de.shopme.tools.knowledge.catalog.canonical.rebuild.base

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.rebuild.base.model.CanonicalFoodBaseIdentityResolutionAction
import de.shopme.tools.knowledge.catalog.canonical.rebuild.base.model.CanonicalFoodBaseIdentityResolutionDecision
import de.shopme.tools.knowledge.catalog.canonical.rebuild.base.model.CanonicalFoodBaseIdentityResolutionReport
import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.CanonicalFoodIdentity
import java.io.File

class ResolveCanonicalFoodBaseIdentities(
    private val resolver:
    CanonicalFoodBaseIdentityResolver =
        CanonicalFoodBaseIdentityResolver()
) {

    fun resolve(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): CanonicalFoodBaseIdentityResolutionReport {

        val inputFile =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/rebuild/" +
                        "canonical-food-catalog.vnext.json"
            )

        require(
            inputFile.isFile
        ) {
            "Rebuilt canonical food catalog not found: " +
                    inputFile.absolutePath
        }

        val input =
            readCatalog(
                file =
                    inputFile
            )

        val decisions =
            mutableListOf<
                    CanonicalFoodBaseIdentityResolutionDecision
                    >()

        val accepted =
            input.mapNotNull { entry ->

                val resolution =
                    resolver.resolve(
                        itemname =
                            entry.itemname,
                        category =
                            entry.category
                    )

                if (
                    resolution.rejected
                ) {

                    decisions +=
                        CanonicalFoodBaseIdentityResolutionDecision(
                            sourceItemname =
                                entry.itemname,

                            sourceNormalized =
                                entry.normalized,

                            action =
                                CanonicalFoodBaseIdentityResolutionAction
                                    .REJECT_SEMANTICALLY_IMPLAUSIBLE,

                            resolvedItemname =
                                null,

                            resolvedNormalized =
                                null,

                            extractedVariants =
                                resolution.extractedVariants,

                            reason =
                                resolution.reason
                        )

                    null

                } else {

                    val action =
                        if (
                            resolution.normalized ==
                            entry.normalized
                        ) {
                            CanonicalFoodBaseIdentityResolutionAction
                                .KEEP
                        } else {
                            CanonicalFoodBaseIdentityResolutionAction
                                .CONSOLIDATE
                        }

                    decisions +=
                        CanonicalFoodBaseIdentityResolutionDecision(
                            sourceItemname =
                                entry.itemname,

                            sourceNormalized =
                                entry.normalized,

                            action =
                                action,

                            resolvedItemname =
                                resolution.itemname,

                            resolvedNormalized =
                                resolution.normalized,

                            extractedVariants =
                                resolution.extractedVariants,

                            reason =
                                resolution.reason
                        )

                    ResolvedInput(
                        original =
                            entry,

                        resolvedItemname =
                            resolution.itemname,

                        resolvedNormalized =
                            resolution.normalized,

                        extractedVariants =
                            resolution.extractedVariants
                    )
                }
            }

        val output =
            accepted
                .groupBy {
                    it.resolvedNormalized
                }
                .map { (_, group) ->

                    consolidateGroup(
                        group =
                            group
                    )
                }
                .sortedBy {
                    it.normalized
                }

        validateOutput(
            entries =
                output
        )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/base-resolved"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        )

        val outputFile =
            outputDirectory.resolve(
                "canonical-food-catalog.base-resolved.json"
            )

        val decisionsFile =
            outputDirectory.resolve(
                "canonical-food-catalog.base-resolution-decisions.json"
            )

        writeJson(
            value =
                output,
            file =
                outputFile
        )

        writeJson(
            value =
                decisions.sortedWith(
                    compareBy<
                            CanonicalFoodBaseIdentityResolutionDecision
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

        val report =
            CanonicalFoodBaseIdentityResolutionReport(
                version =
                    1,

                inputEntryCount =
                    input.size,

                outputEntryCount =
                    output.size,

                consolidatedEntryCount =
                    input.size -
                            decisions.count {
                                it.action ==
                                        CanonicalFoodBaseIdentityResolutionAction
                                            .REJECT_SEMANTICALLY_IMPLAUSIBLE
                            } -
                            output.size,

                rejectedSemanticallyImplausibleCount =
                    decisions.count {
                        it.action ==
                                CanonicalFoodBaseIdentityResolutionAction
                                    .REJECT_SEMANTICALLY_IMPLAUSIBLE
                    },

                outputVariantCount =
                    output.sumOf {
                        it.variants.size
                    },

                outputSourceVariantCount =
                    output.sumOf {
                        it.sourceVariants.size
                    },

                outputFile =
                    outputFile.path,

                decisionsFile =
                    decisionsFile.path
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "canonical-food-base-identity-resolution-report.json"
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

    private fun consolidateGroup(
        group: List<ResolvedInput>
    ): CanonicalFoodIdentity {

        require(
            group.isNotEmpty()
        )

        /*
         * Bevorzuge eine Identity, die bereits exakt
         * dem abgeleiteten Basis-Key entsprach.
         */
        val representative =
            group
                .sortedWith(
                    compareByDescending<ResolvedInput> {
                        if (
                            it.original.normalized ==
                            it.resolvedNormalized
                        ) {
                            1
                        } else {
                            0
                        }
                    }
                        .thenBy {
                            it.resolvedItemname.length
                        }
                        .thenBy {
                            it.resolvedItemname
                        }
                )
                .first()

        val variants =
            group
                .asSequence()
                .flatMap { resolved ->

                    sequence {
                        yieldAll(
                            resolved.original.variants
                        )

                        yieldAll(
                            resolved.extractedVariants
                        )
                    }
                }
                .map {
                    it.trim()
                }
                .filter(
                    String::isNotBlank
                )
                .distinct()
                .sorted()
                .toList()

        val canonicalName =
            representative.resolvedItemname

        val sourceVariants =
            group
                .asSequence()
                .flatMap { resolved ->

                    sequence {
                        yield(
                            resolved.original.itemname
                        )

                        yieldAll(
                            resolved.original.sourceVariants
                        )
                    }
                }
                .map {
                    it.trim()
                }
                .filter(
                    String::isNotBlank
                )
                .filter {
                    !it.equals(
                        canonicalName,
                        ignoreCase =
                            true
                    )
                }
                .distinctBy {
                    it.lowercase()
                }
                .sorted()
                .toList()

        val category =
            selectCategory(
                group
            )

        return CanonicalFoodIdentity(
            itemname =
                canonicalName,

            normalized =
                representative.resolvedNormalized,

            category =
                category,

            variants =
                variants,

            sourceVariants =
                sourceVariants
        )
    }

    private fun selectCategory(
        group: List<ResolvedInput>
    ): String =
        group
            .groupingBy {
                it.original.category
            }
            .eachCount()
            .entries
            .sortedWith(
                compareByDescending<
                        Map.Entry<String, Int>
                        > {
                    it.value
                }
                    .thenBy {
                        it.key
                    }
            )
            .first()
            .key

    private fun readCatalog(
        file: File
    ): List<CanonicalFoodIdentity> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )

        require(
            root.isJsonArray
        )

        return root
            .asJsonArray
            .map { element ->

                val json =
                    element.asJsonObject

                CanonicalFoodIdentity(
                    itemname =
                        json.requiredString(
                            "itemname"
                        ),

                    normalized =
                        json.requiredString(
                            "normalized"
                        ),

                    category =
                        json.requiredString(
                            "category"
                        ),

                    variants =
                        json.stringList(
                            "variants"
                        ),

                    sourceVariants =
                        json.stringList(
                            "sourceVariants"
                        )
                )
            }
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
            "Duplicate normalized canonical base identities."
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
                entry.sourceVariants.none {
                    it.equals(
                        entry.itemname,
                        ignoreCase =
                            true
                    )
                }
            )

            require(
                entry.variants ==
                        entry.variants
                            .distinct()
                            .sorted()
            )

            require(
                entry.sourceVariants
                    .map {
                        it.lowercase()
                    }
                    .distinct()
                    .size ==
                        entry.sourceVariants.size
            )
        }
    }

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
        report:
        CanonicalFoodBaseIdentityResolutionReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CANONICAL FOOD BASE IDENTITY RESOLUTION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Input identities         : " +
                    report.inputEntryCount
        )
        println(
            "Output identities        : " +
                    report.outputEntryCount
        )
        println(
            "Consolidated             : " +
                    report.consolidatedEntryCount
        )
        println(
            "Semantic rejects         : " +
                    report.rejectedSemanticallyImplausibleCount
        )
        println(
            "Variants                 : " +
                    report.outputVariantCount
        )
        println(
            "Source variants          : " +
                    report.outputSourceVariantCount
        )
        println()
        println(
            "Catalog                  : " +
                    report.outputFile
        )
        println(
            "Decisions                : " +
                    report.decisionsFile
        )
        println(
            "Report                   : " +
                    reportFile.path
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    private fun JsonObject.requiredString(
        key: String
    ): String =
        get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive
            }
            ?.asString
            ?.trim()
            ?.takeIf(
                String::isNotBlank
            )
            ?: error(
                "Missing '$key'."
            )

    private fun JsonObject.stringList(
        key: String
    ): List<String> =
        get(key)
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray
            ?.mapNotNull { element ->

                element
                    .takeIf {
                        it.isJsonPrimitive
                    }
                    ?.asString
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    )
            }
            .orEmpty()

    private data class ResolvedInput(
        val original: CanonicalFoodIdentity,
        val resolvedItemname: String,
        val resolvedNormalized: String,
        val extractedVariants: List<String>
    )

    companion object {

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}