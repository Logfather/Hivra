package de.shopme.tools.knowledge.mapping.catalog.nutrition.group

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.retrieval.SourceKnowledgeIdentityIndexReader
import de.shopme.tools.knowledge.mapping.catalog.retrieval.SourceKnowledgeIdentityIndexWriter
import java.io.File

enum class CanonicalNutritionSourceVariantStatus {

    EXACT_ACCEPTED,

    RETRIEVAL_CANDIDATE
}

data class CanonicalNutritionSourceVariant(
    val serverKey: String,
    val aliases: List<String>,
    val matchAliases: List<String>,
    val status: CanonicalNutritionSourceVariantStatus,
    val retrievalScore: Double?,
    val sharedTokens: List<String>
)

data class CanonicalNutritionSourceVariantGroup(
    val catalogKey: String,
    val canonicalName: String,
    val acceptedSourceVariants:
    List<CanonicalNutritionSourceVariant>,
    val candidateSourceVariants:
    List<CanonicalNutritionSourceVariant>
)

data class CanonicalNutritionSourceVariantGroups(
    val version: Int,
    val catalogEntryCount: Int,
    val groups:
    List<CanonicalNutritionSourceVariantGroup>
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

data class CanonicalNutritionSourceVariantGroupBuildReport(
    val version: Int,
    val catalogEntryCount: Int,
    val groupCount: Int,
    val exactAcceptedVariantCount: Int,
    val retrievalCandidateVariantCount: Int,
    val groupsWithAcceptedVariants: Int,
    val groupsWithRetrievalCandidates: Int,
    val groupsWithoutAnyEvidence: Int,
    val sourceIdentityRecordCount: Int,
    val groupsFile: String
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

class BuildCanonicalNutritionSourceVariantGroups {

    fun build(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): CanonicalNutritionSourceVariantGroupBuildReport {

        val catalogFile =
            paths.canonicalFoodCatalog

        val mappingFile =
            paths.catalogServerMappings

        val nutritionMatchReportFile =
            paths.projectRoot.resolve(
                "build/knowledge/match-reports/" +
                        "nutrition.matches.json"
            )

        val sourceIdentityIndexFile =
            paths.serverRoot
                .resolve(
                    SourceKnowledgeIdentityIndexWriter
                        .DIRECTORY_NAME
                )
                .resolve(
                    SourceKnowledgeIdentityIndexWriter
                        .FILE_NAME
                )

        require(
            catalogFile.isFile
        ) {
            "Canonical food catalog does not exist: " +
                    catalogFile.absolutePath
        }

        require(
            mappingFile.isFile
        ) {
            "Catalog-server mapping file does not exist: " +
                    mappingFile.absolutePath
        }

        require(
            nutritionMatchReportFile.isFile
        ) {
            "Nutrition match report does not exist: " +
                    nutritionMatchReportFile.absolutePath
        }

        require(
            sourceIdentityIndexFile.isFile
        ) {
            "Source identity index does not exist: " +
                    sourceIdentityIndexFile.absolutePath
        }

        val catalogEntries =
            readCatalogEntries(
                file =
                    catalogFile
            )

        require(
            catalogEntries.size ==
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT
        ) {
            "Canonical catalog entry count changed. " +
                    "Expected " +
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT +
                    ", found ${catalogEntries.size}."
        }

        val exactMappings =
            readExactNutritionMappings(
                file =
                    mappingFile
            )

        val retrievalCandidates =
            readNutritionRetrievalCandidates(
                file =
                    nutritionMatchReportFile
            )

        val requiredServerKeys =
            buildSet {

                exactMappings
                    .values
                    .flatten()
                    .forEach(
                        ::add
                    )

                retrievalCandidates
                    .values
                    .flatten()
                    .map {
                        it.serverKey
                    }
                    .forEach(
                        ::add
                    )
            }

        val sourceIdentities =
            readRelevantSourceIdentities(
                file =
                    sourceIdentityIndexFile,
                requiredServerKeys =
                    requiredServerKeys
            )

        val groups =
            catalogEntries
                .map { catalogEntry ->

                    buildGroup(
                        catalogEntry =
                            catalogEntry,
                        exactServerKeys =
                            exactMappings[
                                catalogEntry.catalogKey
                            ].orEmpty(),
                        retrievalCandidates =
                            retrievalCandidates[
                                catalogEntry.catalogKey
                            ].orEmpty(),
                        sourceIdentities =
                            sourceIdentities
                    )
                }
                .sortedBy {
                    it.catalogKey
                }

        require(
            groups
                .map {
                    it.catalogKey
                }
                .distinct()
                .size ==
                    groups.size
        ) {
            "Duplicate canonical Nutrition source-variant groups."
        }

        val result =
            CanonicalNutritionSourceVariantGroups(
                version =
                    CanonicalNutritionSourceVariantGroups
                        .CURRENT_VERSION,
                catalogEntryCount =
                    catalogEntries.size,
                groups =
                    groups
            )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/source-variant-groups"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        ) {
            "Could not create source-variant group directory: " +
                    outputDirectory.absolutePath
        }

        val groupsFile =
            outputDirectory.resolve(
                "nutrition.source-variant-groups.json"
            )

        writeJson(
            value =
                result,
            file =
                groupsFile
        )

        val report =
            CanonicalNutritionSourceVariantGroupBuildReport(
                version =
                    CanonicalNutritionSourceVariantGroupBuildReport
                        .CURRENT_VERSION,

                catalogEntryCount =
                    catalogEntries.size,

                groupCount =
                    groups.size,

                exactAcceptedVariantCount =
                    groups.sumOf {
                        it.acceptedSourceVariants.size
                    },

                retrievalCandidateVariantCount =
                    groups.sumOf {
                        it.candidateSourceVariants.size
                    },

                groupsWithAcceptedVariants =
                    groups.count {
                        it.acceptedSourceVariants
                            .isNotEmpty()
                    },

                groupsWithRetrievalCandidates =
                    groups.count {
                        it.candidateSourceVariants
                            .isNotEmpty()
                    },

                groupsWithoutAnyEvidence =
                    groups.count {
                        it.acceptedSourceVariants.isEmpty() &&
                                it.candidateSourceVariants.isEmpty()
                    },

                sourceIdentityRecordCount =
                    sourceIdentities.size,

                groupsFile =
                    groupsFile.path
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "nutrition-source-variant-group-build.json"
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

    private fun buildGroup(
        catalogEntry: CatalogEntry,
        exactServerKeys: List<String>,
        retrievalCandidates: List<RetrievalCandidate>,
        sourceIdentities:
        Map<String, AggregatedSourceIdentity>
    ): CanonicalNutritionSourceVariantGroup {

        val accepted =
            exactServerKeys
                .distinct()
                .sorted()
                .map { serverKey ->

                    val sourceIdentity =
                        sourceIdentities[
                            serverKey
                        ]

                    CanonicalNutritionSourceVariant(
                        serverKey =
                            serverKey,

                        aliases =
                            sourceIdentity
                                ?.aliases
                                .orEmpty(),

                        matchAliases =
                            sourceIdentity
                                ?.matchAliases
                                .orEmpty(),

                        status =
                            CanonicalNutritionSourceVariantStatus
                                .EXACT_ACCEPTED,

                        retrievalScore =
                            1.0,

                        sharedTokens =
                            emptyList()
                    )
                }

        val acceptedServerKeys =
            accepted
                .map {
                    it.serverKey
                }
                .toSet()

        val candidates =
            retrievalCandidates
                .filterNot {
                    it.serverKey in
                            acceptedServerKeys
                }
                .groupBy {
                    it.serverKey
                }
                .map { (serverKey, occurrences) ->

                    val best =
                        occurrences.maxWithOrNull(
                            compareBy<RetrievalCandidate> {
                                it.score
                            }
                                .thenBy {
                                    it.serverKey
                                }
                        )
                            ?: error(
                                "Missing retrieval candidate."
                            )

                    val sourceIdentity =
                        sourceIdentities[
                            serverKey
                        ]

                    CanonicalNutritionSourceVariant(
                        serverKey =
                            serverKey,

                        aliases =
                            sourceIdentity
                                ?.aliases
                                .orEmpty(),

                        matchAliases =
                            sourceIdentity
                                ?.matchAliases
                                .orEmpty(),

                        status =
                            CanonicalNutritionSourceVariantStatus
                                .RETRIEVAL_CANDIDATE,

                        retrievalScore =
                            best.score,

                        sharedTokens =
                            best.sharedTokens
                    )
                }
                .sortedWith(
                    compareByDescending<
                            CanonicalNutritionSourceVariant
                            > {
                        it.retrievalScore
                            ?: 0.0
                    }
                        .thenBy {
                            it.serverKey
                        }
                )

        return CanonicalNutritionSourceVariantGroup(
            catalogKey =
                catalogEntry.catalogKey,

            canonicalName =
                catalogEntry.canonicalName,

            acceptedSourceVariants =
                accepted,

            candidateSourceVariants =
                candidates
        )
    }

    private fun readCatalogEntries(
        file: File
    ): List<CatalogEntry> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )

        require(
            root.isJsonArray
        ) {
            "Canonical catalog root must be an array."
        }

        val entries =
            root
                .asJsonArray
                .map { element ->

                    require(
                        element.isJsonObject
                    ) {
                        "Canonical catalog entry must be an object."
                    }

                    val json =
                        element.asJsonObject

                    CatalogEntry(
                        catalogKey =
                            json.requiredString(
                                key =
                                    "normalized"
                            ),

                        canonicalName =
                            json.requiredString(
                                key =
                                    "itemname"
                            )
                    )
                }

        require(
            entries
                .map {
                    it.catalogKey
                }
                .distinct()
                .size ==
                    entries.size
        ) {
            "Canonical catalog contains duplicate normalized keys."
        }

        return entries
    }

    private fun readExactNutritionMappings(
        file: File
    ): Map<String, List<String>> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonObject

        val mappings =
            root
                .requiredArray(
                    key =
                        "mappings"
                )

        return mappings
            .mapNotNull { element ->

                val mapping =
                    element.asJsonObject

                val artifact =
                    mapping.optionalString(
                        key =
                            "serverArtifact"
                    )
                        ?: mapping.optionalString(
                            key =
                                "sourceArtifact"
                        )
                        ?: return@mapNotNull null

                if (
                    artifact !=
                    NUTRITION_ARTIFACT
                ) {
                    return@mapNotNull null
                }

                val catalogKey =
                    mapping.requiredString(
                        key =
                            "catalogKey"
                    )

                val serverKey =
                    mapping.requiredString(
                        key =
                            "serverKey"
                    )

                catalogKey to
                        serverKey
            }
            .groupBy(
                keySelector = {
                    it.first
                },
                valueTransform = {
                    it.second
                }
            )
            .mapValues { (_, serverKeys) ->
                serverKeys
                    .distinct()
                    .sorted()
            }
    }

    private fun readNutritionRetrievalCandidates(
        file: File
    ): Map<String, List<RetrievalCandidate>> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonObject

        require(
            root.requiredString(
                key =
                    "artifactName"
            ) ==
                    NUTRITION_ARTIFACT
        ) {
            "Expected nutrition match report."
        }

        return root
            .requiredArray(
                key =
                    "unmatched"
            )
            .associate { element ->

                val unmatched =
                    element.asJsonObject

                val catalogKey =
                    unmatched.requiredString(
                        key =
                            "catalogKey"
                    )

                val candidates =
                    unmatched
                        .requiredArray(
                            key =
                                "nearestCandidates"
                        )
                        .map { candidateElement ->

                            val candidate =
                                candidateElement
                                    .asJsonObject

                            RetrievalCandidate(
                                serverKey =
                                    candidate.requiredString(
                                        key =
                                            "serverKey"
                                    ),

                                score =
                                    candidate.requiredDouble(
                                        key =
                                            "score"
                                    ),

                                sharedTokens =
                                    candidate
                                        .requiredArray(
                                            key =
                                                "sharedTokens"
                                        )
                                        .mapNotNull {
                                            it
                                                .takeIf {
                                                        value ->
                                                    value.isJsonPrimitive
                                                }
                                                ?.asString
                                                ?.trim()
                                                ?.takeIf(
                                                    String::isNotBlank
                                                )
                                        }
                                        .distinct()
                                        .sorted()
                            )
                        }
                        .sortedWith(
                            compareByDescending<
                                    RetrievalCandidate
                                    > {
                                it.score
                            }
                                .thenBy {
                                    it.serverKey
                                }
                        )

                catalogKey to
                        candidates
            }
    }

    private fun readRelevantSourceIdentities(
        file: File,
        requiredServerKeys: Set<String>
    ): Map<String, AggregatedSourceIdentity> {

        if (
            requiredServerKeys.isEmpty()
        ) {
            return emptyMap()
        }

        val identities =
            mutableMapOf<
                    String,
                    MutableSourceIdentity
                    >()

        SourceKnowledgeIdentityIndexReader()
            .forEach(
                file =
                    file
            ) { record ->

                if (
                    NUTRITION_ARTIFACT !in
                    record.artifacts
                ) {
                    return@forEach
                }

                if (
                    record.serverKey !in
                    requiredServerKeys
                ) {
                    return@forEach
                }

                val accumulator =
                    identities.getOrPut(
                        record.serverKey
                    ) {
                        MutableSourceIdentity()
                    }

                accumulator.aliases +=
                    record.aliases

                accumulator.matchAliases +=
                    record.matchAliases
            }

        return identities
            .mapValues { (_, accumulator) ->

                AggregatedSourceIdentity(
                    aliases =
                        accumulator
                            .aliases
                            .asSequence()
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .distinct()
                            .sorted()
                            .toList(),

                    matchAliases =
                        accumulator
                            .matchAliases
                            .asSequence()
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .distinct()
                            .sorted()
                            .toList()
                )
            }
            .toSortedMap()
    }

    private fun writeJson(
        value: Any,
        file: File
    ) {

        val parent =
            requireNotNull(
                file.parentFile
            ) {
                "Output file has no parent directory: " +
                        file.absolutePath
            }

        require(
            parent.exists() ||
                    parent.mkdirs()
        ) {
            "Could not create output directory: " +
                    parent.absolutePath
        }

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
    }

    private fun printReport(
        report:
        CanonicalNutritionSourceVariantGroupBuildReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CANONICAL NUTRITION SOURCE-VARIANT GROUPS")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries          : " +
                    report.catalogEntryCount
        )
        println(
            "Groups                   : " +
                    report.groupCount
        )
        println(
            "Exact accepted variants  : " +
                    report.exactAcceptedVariantCount
        )
        println(
            "Retrieval candidates     : " +
                    report.retrievalCandidateVariantCount
        )
        println(
            "Groups with accepted     : " +
                    report.groupsWithAcceptedVariants
        )
        println(
            "Groups with candidates   : " +
                    report.groupsWithRetrievalCandidates
        )
        println(
            "Groups without evidence  : " +
                    report.groupsWithoutAnyEvidence
        )
        println(
            "Source identities loaded : " +
                    report.sourceIdentityRecordCount
        )
        println(
            "Groups file              : " +
                    report.groupsFile
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
        optionalString(
            key =
                key
        )
            ?: error(
                "Missing or blank '$key'."
            )

    private fun JsonObject.optionalString(
        key: String
    ): String? =
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

    private fun JsonObject.requiredDouble(
        key: String
    ): Double {

        val value =
            get(key)

        require(
            value != null &&
                    !value.isJsonNull &&
                    value.isJsonPrimitive &&
                    value
                        .asJsonPrimitive
                        .isNumber
        ) {
            "Missing numeric '$key'."
        }

        return value.asDouble
    }

    private fun JsonObject.requiredArray(
        key: String
    ): JsonArray =
        get(key)
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray
            ?: error(
                "Missing JSON array '$key'."
            )

    private data class CatalogEntry(
        val catalogKey: String,
        val canonicalName: String
    )

    private data class RetrievalCandidate(
        val serverKey: String,
        val score: Double,
        val sharedTokens: List<String>
    )

    private data class MutableSourceIdentity(
        val aliases: MutableList<String> =
            mutableListOf(),
        val matchAliases: MutableList<String> =
            mutableListOf()
    )

    private data class AggregatedSourceIdentity(
        val aliases: List<String>,
        val matchAliases: List<String>
    )

    companion object {

        private const val NUTRITION_ARTIFACT =
            "nutrition.json"

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}