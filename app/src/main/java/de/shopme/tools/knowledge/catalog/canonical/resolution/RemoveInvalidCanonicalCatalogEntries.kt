package de.shopme.tools.knowledge.catalog.canonical.resolution

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.CanonicalFoodIdentity
import java.io.File

data class RemoveInvalidCanonicalCatalogEntriesReport(
    val version: Int,
    val inputEntryCount: Int,
    val invalidEntryCount: Int,
    val outputEntryCount: Int,
    val outputFile: String,
    val removedEntriesFile: String
)

data class RemovedCanonicalFoodEntry(
    val itemname: String,
    val normalized: String,
    val category: String,
    val reasons: List<String>
)

class RemoveInvalidCanonicalCatalogEntries {

    fun remove(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): RemoveInvalidCanonicalCatalogEntriesReport {

        val inputFile =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/semantic-resolved/" +
                        "canonical-food-catalog.semantic-resolved.json"
            )

        val validationFile =
            paths.reportsRoot.resolve(
                "semantic-resolved-canonical-food-validation.json"
            )

        require(
            inputFile.isFile
        ) {
            "Semantic-resolved catalog not found: " +
                    inputFile.absolutePath
        }

        require(
            validationFile.isFile
        ) {
            "Semantic validation report not found: " +
                    validationFile.absolutePath
        }

        val input =
            readCatalog(
                inputFile
            )

        val invalidEntries =
            readInvalidEntries(
                validationFile
            )

        val invalidKeys =
            invalidEntries
                .map {
                    it.normalized
                }
                .toSet()

        require(
            invalidKeys.size ==
                    invalidEntries.size
        ) {
            "Duplicate invalid catalog keys in validation report."
        }

        val output =
            input
                .filter {
                    it.normalized !in
                            invalidKeys
                }
                .sortedBy {
                    it.normalized
                }

        val removed =
            input
                .filter {
                    it.normalized in
                            invalidKeys
                }
                .map { entry ->

                    val invalid =
                        requireNotNull(
                            invalidEntries
                                .firstOrNull {
                                    it.normalized ==
                                            entry.normalized
                                }
                        )

                    RemovedCanonicalFoodEntry(
                        itemname =
                            entry.itemname,

                        normalized =
                            entry.normalized,

                        category =
                            entry.category,

                        reasons =
                            invalid.reasons
                    )
                }
                .sortedBy {
                    it.normalized
                }

        require(
            removed.size ==
                    invalidEntries.size
        ) {
            "Not all invalid validation entries were found in catalog. " +
                    "Expected=${invalidEntries.size}, actual=${removed.size}"
        }

        require(
            output.size +
                    removed.size ==
                    input.size
        )

        validateOutput(
            output
        )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/final-candidate"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        ) {
            "Unable to create final candidate directory: " +
                    outputDirectory.absolutePath
        }

        val outputFile =
            outputDirectory.resolve(
                "canonical-food-catalog.final-candidate.json"
            )

        val removedEntriesFile =
            outputDirectory.resolve(
                "canonical-food-catalog.removed-invalid-entries.json"
            )

        writeJson(
            value =
                output,
            file =
                outputFile
        )

        writeJson(
            value =
                removed,
            file =
                removedEntriesFile
        )

        val report =
            RemoveInvalidCanonicalCatalogEntriesReport(
                version =
                    1,

                inputEntryCount =
                    input.size,

                invalidEntryCount =
                    removed.size,

                outputEntryCount =
                    output.size,

                outputFile =
                    outputFile.path,

                removedEntriesFile =
                    removedEntriesFile.path
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "canonical-food-invalid-entry-removal-report.json"
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

    private fun readInvalidEntries(
        file: File
    ): List<InvalidEntry> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonObject

        return root[
            "entries"
        ]
            .asJsonArray
            .mapNotNull { element ->

                val json =
                    element.asJsonObject

                val issues =
                    json[
                        "issues"
                    ]
                        .asJsonArray
                        .map {
                            it.asJsonObject
                        }

                val errors =
                    issues
                        .filter {
                            it[
                                "severity"
                            ].asString ==
                                    "ERROR"
                        }

                if (
                    errors.isEmpty()
                ) {
                    return@mapNotNull null
                }

                InvalidEntry(
                    normalized =
                        json[
                            "catalogKey"
                        ].asString,

                    reasons =
                        errors.map { issue ->

                            issue[
                                "type"
                            ].asString +
                                    ": " +
                                    issue[
                                        "message"
                                    ].asString
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
            "Duplicate normalized keys remain."
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
        report: RemoveInvalidCanonicalCatalogEntriesReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("REMOVE INVALID CANONICAL CATALOG ENTRIES")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Input entries          : " +
                    report.inputEntryCount
        )
        println(
            "Removed invalid        : " +
                    report.invalidEntryCount
        )
        println(
            "Output entries         : " +
                    report.outputEntryCount
        )
        println()
        println(
            "Catalog                : " +
                    report.outputFile
        )
        println(
            "Removed entries        : " +
                    report.removedEntriesFile
        )
        println(
            "Report                 : " +
                    reportFile.path
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    private data class InvalidEntry(
        val normalized: String,
        val reasons: List<String>
    )

    companion object {

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}