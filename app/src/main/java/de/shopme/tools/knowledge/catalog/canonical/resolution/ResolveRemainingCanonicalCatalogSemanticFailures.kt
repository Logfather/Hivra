package de.shopme.tools.knowledge.catalog.canonical.resolution

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.CanonicalFoodIdentity
import de.shopme.tools.knowledge.catalog.canonical.resolution.model.CanonicalFoodSemanticResolutionAction
import de.shopme.tools.knowledge.catalog.canonical.resolution.model.CanonicalFoodSemanticResolutionDecision
import de.shopme.tools.knowledge.catalog.canonical.resolution.model.CanonicalFoodSemanticResolutionReport
import de.shopme.tools.knowledge.catalog.canonical.validation.CanonicalFoodSemanticIdentityNormalizer
import java.io.File

class ResolveRemainingCanonicalCatalogSemanticFailures(
    private val identityNormalizer:
    CanonicalFoodSemanticIdentityNormalizer =
        CanonicalFoodSemanticIdentityNormalizer()
) {

    fun resolve(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): CanonicalFoodSemanticResolutionReport {

        val inputFile =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/base-resolved/" +
                        "canonical-food-catalog.base-resolved.json"
            )

        require(
            inputFile.isFile
        ) {
            "Base-resolved canonical food catalog not found: " +
                    inputFile.absolutePath
        }

        val input =
            readCatalog(
                inputFile
            )

        val duplicateGroups =
            input
                .groupBy { entry ->

                    identityNormalizer
                        .normalize(
                            entry.itemname
                        )
                }
                .filterValues {
                    it.size >
                            1
                }

        val duplicateMemberKeys =
            duplicateGroups
                .values
                .flatten()
                .map {
                    it.normalized
                }
                .toSet()

        val output =
            mutableListOf<CanonicalFoodIdentity>()

        val decisions =
            mutableListOf<
                    CanonicalFoodSemanticResolutionDecision
                    >()

        /*
         * Alle Nicht-Duplicate-Einträge zunächst übernehmen.
         */
        input
            .filter {
                it.normalized !in
                        duplicateMemberKeys
            }
            .forEach { entry ->

                val correctedCategory =
                    CanonicalFoodCategoryCorrectionRegistry
                        .correctedCategory(
                            normalized =
                                entry.normalized,
                            currentCategory =
                                entry.category
                        )

                output +=
                    entry.copy(
                        category =
                            correctedCategory
                    )

                decisions +=
                    CanonicalFoodSemanticResolutionDecision(
                        sourceItemname =
                            entry.itemname,

                        sourceNormalized =
                            entry.normalized,

                        action =
                            if (
                                correctedCategory !=
                                entry.category
                            ) {
                                CanonicalFoodSemanticResolutionAction
                                    .CORRECT_CATEGORY
                            } else {
                                CanonicalFoodSemanticResolutionAction
                                    .KEEP
                            },

                        targetItemname =
                            entry.itemname,

                        targetNormalized =
                            entry.normalized,

                        reason =
                            if (
                                correctedCategory !=
                                entry.category
                            ) {
                                "Explicit canonical category correction: " +
                                        "${entry.category} -> $correctedCategory"
                            } else {
                                "No semantic resolution required."
                            }
                    )
            }

        duplicateGroups
            .toSortedMap()
            .forEach { (_, group) ->

                resolveDuplicateGroup(
                    group =
                        group,
                    output =
                        output,
                    decisions =
                        decisions
                )
            }

        val finalOutput =
            output
                .sortedBy {
                    it.normalized
                }

        validateOutput(
            finalOutput
        )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/semantic-resolved"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        ) {
            "Unable to create semantic-resolved catalog directory: " +
                    outputDirectory.absolutePath
        }

        val outputFile =
            outputDirectory.resolve(
                "canonical-food-catalog.semantic-resolved.json"
            )

        val decisionsFile =
            outputDirectory.resolve(
                "canonical-food-catalog.semantic-resolution-decisions.json"
            )

        writeJson(
            value =
                finalOutput,
            file =
                outputFile
        )

        writeJson(
            value =
                decisions.sortedWith(
                    compareBy<
                            CanonicalFoodSemanticResolutionDecision
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
            CanonicalFoodSemanticResolutionReport(
                version =
                    1,

                inputEntryCount =
                    input.size,

                outputEntryCount =
                    finalOutput.size,

                mergedEntryCount =
                    decisions.count {
                        it.action ==
                                CanonicalFoodSemanticResolutionAction
                                    .MERGE
                    },

                rejectedEntryCount =
                    decisions.count {
                        it.action ==
                                CanonicalFoodSemanticResolutionAction
                                    .REJECT_INVALID_IDENTITY
                    },

                categoryCorrectionCount =
                    decisions.count {
                        it.action ==
                                CanonicalFoodSemanticResolutionAction
                                    .CORRECT_CATEGORY
                    },

                outputFile =
                    outputFile.path,

                decisionsFile =
                    decisionsFile.path
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "canonical-food-semantic-resolution-report.json"
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

    private fun resolveDuplicateGroup(
        group: List<CanonicalFoodIdentity>,
        output: MutableList<CanonicalFoodIdentity>,
        decisions:
        MutableList<
                CanonicalFoodSemanticResolutionDecision
                >
    ) {

        require(
            group.size >
                    1
        )

        val resolution =
            CanonicalFoodDuplicateResolutionRegistry
                .find(
                    group.map {
                        it.itemname
                    }
                )
                ?: error(
                    "No explicit duplicate resolution registered for: " +
                            group
                                .map {
                                    it.itemname
                                }
                                .sorted()
                )

        when (
            resolution.type
        ) {

            DuplicateResolutionType.REJECT_GROUP -> {

                group.forEach { entry ->

                    decisions +=
                        CanonicalFoodSemanticResolutionDecision(
                            sourceItemname =
                                entry.itemname,

                            sourceNormalized =
                                entry.normalized,

                            action =
                                CanonicalFoodSemanticResolutionAction
                                    .REJECT_INVALID_IDENTITY,

                            targetItemname =
                                null,

                            targetNormalized =
                                null,

                            reason =
                                "Duplicate group does not represent a valid " +
                                        "canonical product identity."
                        )
                }
            }

            DuplicateResolutionType.MERGE -> {

                val canonicalItemname =
                    requireNotNull(
                        resolution.canonicalItemname
                    )

                val canonicalNormalized =
                    requireNotNull(
                        resolution.canonicalNormalized
                    )

                val canonicalCategory =
                    requireNotNull(
                        resolution.canonicalCategory
                    )

                val variants =
                    group
                        .asSequence()
                        .flatMap {
                            it.variants.asSequence()
                        }
                        .map(
                            String::trim
                        )
                        .filter(
                            String::isNotBlank
                        )
                        .distinct()
                        .sorted()
                        .toList()

                val sourceVariants =
                    group
                        .asSequence()
                        .flatMap { entry ->

                            sequence {
                                yield(
                                    entry.itemname
                                )

                                yieldAll(
                                    entry.sourceVariants
                                )
                            }
                        }
                        .map(
                            String::trim
                        )
                        .filter(
                            String::isNotBlank
                        )
                        .filter {
                            !it.equals(
                                canonicalItemname,
                                ignoreCase =
                                    true
                            )
                        }
                        .distinctBy {
                            it.lowercase()
                        }
                        .sorted()
                        .toList()

                output +=
                    CanonicalFoodIdentity(
                        itemname =
                            canonicalItemname,

                        normalized =
                            canonicalNormalized,

                        category =
                            canonicalCategory,

                        variants =
                            variants,

                        sourceVariants =
                            sourceVariants
                    )

                group.forEach { entry ->

                    decisions +=
                        CanonicalFoodSemanticResolutionDecision(
                            sourceItemname =
                                entry.itemname,

                            sourceNormalized =
                                entry.normalized,

                            action =
                                if (
                                    entry.itemname.equals(
                                        canonicalItemname,
                                        ignoreCase =
                                            true
                                    )
                                ) {
                                    CanonicalFoodSemanticResolutionAction
                                        .KEEP
                                } else {
                                    CanonicalFoodSemanticResolutionAction
                                        .MERGE
                                },

                            targetItemname =
                                canonicalItemname,

                            targetNormalized =
                                canonicalNormalized,

                            reason =
                                if (
                                    entry.itemname.equals(
                                        canonicalItemname,
                                        ignoreCase =
                                            true
                                    )
                                ) {
                                    "Selected as canonical representative."
                                } else {
                                    "Merged into canonical identity according to " +
                                            "German compound/plural naming policy."
                                }
                        )
                }
            }
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
            "Duplicate normalized keys remain after semantic resolution."
        }

        val remainingSemanticDuplicates =
            entries
                .groupBy {
                    identityNormalizer
                        .normalize(
                            it.itemname
                        )
                }
                .filterValues {
                    it.size >
                            1
                }

        require(
            remainingSemanticDuplicates.isEmpty()
        ) {
            "Semantic duplicate identities remain: " +
                    remainingSemanticDuplicates
                        .values
                        .map {
                                group ->
                            group.map {
                                it.itemname
                            }
                        }
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
        }
    }

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

    private fun JsonObject.requiredString(
        key: String
    ): String =
        get(key)
            ?.takeUnless {
                it.isJsonNull
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
            ?.mapNotNull { value ->

                value
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

    private fun writeJson(
        value: Any,
        file: File
    ) {

        val parent =
            requireNotNull(
                file.parentFile
            ) {
                "Output file has no parent: ${file.path}"
            }

        require(
            parent.exists() ||
                    parent.mkdirs()
        ) {
            "Unable to create output directory: ${parent.path}"
        }

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
    }

    private fun printReport(
        report: CanonicalFoodSemanticResolutionReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CANONICAL FOOD SEMANTIC FAILURE RESOLUTION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Input identities       : ${report.inputEntryCount}")
        println("Output identities      : ${report.outputEntryCount}")
        println("Merged entries         : ${report.mergedEntryCount}")
        println("Rejected entries       : ${report.rejectedEntryCount}")
        println("Category corrections   : ${report.categoryCorrectionCount}")
        println()
        println("Catalog                : ${report.outputFile}")
        println("Decisions              : ${report.decisionsFile}")
        println("Report                 : ${reportFile.path}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    companion object {

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}