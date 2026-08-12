package de.shopme.tools.knowledge.mapping.catalog.nutrition.group

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.retrieval.CanonicalCatalogRetrievalIdentityReader
import de.shopme.tools.knowledge.mapping.catalog.retrieval.SourceKnowledgeIdentityIndexReader
import de.shopme.tools.knowledge.mapping.catalog.retrieval.SourceKnowledgeIdentityIndexWriter
import java.io.File
import java.text.Normalizer
import java.util.Locale
import java.util.PriorityQueue
import kotlin.math.max

data class DiscoveredCanonicalNutritionSourceVariant(
    val serverKey: String,
    val exact: Boolean,
    val retrievalScore: Double,
    val sharedTokens: List<String>,
    val matchedSourceTerm: String,
    val aliases: List<String>,
    val matchAliases: List<String>
)

data class CanonicalNutritionSourceVariantDiscoveryGroup(
    val catalogKey: String,
    val canonicalName: String,
    val exactSourceVariantCount: Int,
    val discoveredSourceVariantCount: Int,
    val sourceVariants:
    List<DiscoveredCanonicalNutritionSourceVariant>
)

data class CanonicalNutritionSourceVariantDiscovery(
    val version: Int,
    val catalogEntryCount: Int,
    val maximumVariantsPerProduct: Int,
    val groups:
    List<CanonicalNutritionSourceVariantDiscoveryGroup>
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

data class CanonicalNutritionSourceVariantDiscoveryReport(
    val version: Int,
    val catalogEntryCount: Int,
    val groupCount: Int,
    val groupsWithExactVariants: Int,
    val groupsWithDiscoveredVariants: Int,
    val groupsWithoutVariants: Int,
    val exactVariantCount: Int,
    val discoveredVariantCount: Int,
    val maximumVariantsPerProduct: Int,
    val discoveryFile: String
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

class DiscoverCanonicalNutritionSourceVariants(
    private val maximumVariantsPerProduct: Int =
        DEFAULT_MAXIMUM_VARIANTS_PER_PRODUCT
) {

    init {

        require(
            maximumVariantsPerProduct > 0
        ) {
            "maximumVariantsPerProduct must be greater than zero."
        }
    }

    fun discover(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): CanonicalNutritionSourceVariantDiscoveryReport {

        val catalogFile =
            paths.canonicalFoodCatalog

        val nutritionFile =
            paths.serverRoot.resolve(
                NUTRITION_ARTIFACT
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
            nutritionFile.isFile
        ) {
            "Nutrition server artifact does not exist: " +
                    nutritionFile.absolutePath
        }

        require(
            sourceIdentityIndexFile.isFile
        ) {
            "Source identity index does not exist: " +
                    sourceIdentityIndexFile.absolutePath
        }

        require(
            paths.catalogServerMappings.isFile
        ) {
            "Catalog-server mappings do not exist: " +
                    paths.catalogServerMappings.absolutePath
        }

        val canonicalIdentities =
            CanonicalCatalogRetrievalIdentityReader()
                .read(
                    file =
                        catalogFile
                )

        require(
            canonicalIdentities.size ==
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT
        ) {
            "Canonical catalog entry count changed. " +
                    "Expected " +
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT +
                    ", found ${canonicalIdentities.size}."
        }

        val canonicalNames =
            readCanonicalNames(
                file =
                    catalogFile
            )

        val exactMappings =
            readExactNutritionMappings(
                file =
                    paths.catalogServerMappings
            )

        val retrievalTermsByCatalogKey =
            canonicalIdentities
                .associate { identity ->
                    identity.catalogKey to
                            identity.retrievalTerms
                }

        val retrievalIndex =
            CatalogRetrievalIndex(
                retrievalTermsByCatalogKey =
                    retrievalTermsByCatalogKey
            )

        val candidateQueues =
            canonicalIdentities
                .associate { identity ->

                    identity.catalogKey to
                            PriorityQueue(
                                maximumVariantsPerProduct + 1,
                                WORST_CANDIDATE_FIRST
                            )
                }
                .toMutableMap()

        /*
         * Exakte Nutrition-Mappings werden immer in die Discovery
         * aufgenommen und später nicht vom Top-N-Ranking verdrängt.
         */
        exactMappings
            .forEach { (catalogKey, serverKeys) ->

                serverKeys.forEach { serverKey ->

                    offerCandidate(
                        queue =
                            requireNotNull(
                                candidateQueues[
                                    catalogKey
                                ]
                            ),
                        candidate =
                            MutableDiscoveredVariant(
                                serverKey =
                                    serverKey,
                                exact =
                                    true,
                                retrievalScore =
                                    1.0,
                                sharedTokens =
                                    emptyList(),
                                matchedSourceTerm =
                                    serverKey
                            )
                    )
                }
            }

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("DISCOVER NUTRITION SOURCE VARIANTS")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries       : " +
                    canonicalIdentities.size
        )
        println(
            "Max variants / product: " +
                    maximumVariantsPerProduct
        )
        println()

        /*
         * 1. Source-Aliase / Match-Aliase.
         *
         * Hier steckt der größte Recall-Gewinn gegenüber dem früheren
         * reinen serverKey-Retrieval.
         */
        SourceKnowledgeIdentityIndexReader()
            .forEach(
                file =
                    sourceIdentityIndexFile
            ) { identity ->

                if (
                    NUTRITION_ARTIFACT !in
                    identity.artifacts
                ) {
                    return@forEach
                }

                val sourceTerms =
                    buildList {

                        add(
                            identity.serverKey
                        )

                        addAll(
                            identity.aliases
                        )

                        addAll(
                            identity.matchAliases
                        )
                    }
                        .asSequence()
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .toList()

                sourceTerms.forEach { sourceTerm ->

                    processSourceIdentity(
                        sourceTerm =
                            sourceTerm,
                        serverKey =
                            identity.serverKey,
                        retrievalIndex =
                            retrievalIndex,
                        candidateQueues =
                            candidateQueues
                    )
                }
            }

        /*
         * 2. Alle 512k echten Server-Keys.
         *
         * Einige Nutrition-Keys besitzen keinen zusätzlichen
         * SourceIdentityRecord und dürfen deshalb nicht verloren gehen.
         */
        val serverEntryCount =
            streamServerKeys(
                file =
                    nutritionFile
            ) { serverKey ->

                processSourceIdentity(
                    sourceTerm =
                        serverKey,
                    serverKey =
                        serverKey,
                    retrievalIndex =
                        retrievalIndex,
                    candidateQueues =
                        candidateQueues
                )
            }

        /*
         * Erst nachdem Top-N feststeht, laden wir Aliase nur für die
         * tatsächlich persistierten Server-Keys.
         *
         * Dadurch materialisieren wir nicht den kompletten
         * 881k-Identity-Index.
         */
        val selectedServerKeys =
            candidateQueues
                .values
                .asSequence()
                .flatMap {
                    it.asSequence()
                }
                .map {
                    it.serverKey
                }
                .toSet()

        val sourceMetadata =
            readSelectedSourceMetadata(
                file =
                    sourceIdentityIndexFile,
                selectedServerKeys =
                    selectedServerKeys
            )

        val groups =
            canonicalIdentities
                .map { identity ->

                    val variants =
                        candidateQueues[
                            identity.catalogKey
                        ]
                            .orEmpty()
                            .sortedWith(
                                BEST_CANDIDATE_FIRST
                            )
                            .map { candidate ->

                                val metadata =
                                    sourceMetadata[
                                        candidate.serverKey
                                    ]

                                DiscoveredCanonicalNutritionSourceVariant(
                                    serverKey =
                                        candidate.serverKey,
                                    exact =
                                        candidate.exact,
                                    retrievalScore =
                                        candidate.retrievalScore,
                                    sharedTokens =
                                        candidate.sharedTokens,
                                    matchedSourceTerm =
                                        candidate.matchedSourceTerm,
                                    aliases =
                                        metadata
                                            ?.aliases
                                            .orEmpty(),
                                    matchAliases =
                                        metadata
                                            ?.matchAliases
                                            .orEmpty()
                                )
                            }

                    CanonicalNutritionSourceVariantDiscoveryGroup(
                        catalogKey =
                            identity.catalogKey,
                        canonicalName =
                            canonicalNames[
                                identity.catalogKey
                            ]
                                ?: identity.catalogKey,
                        exactSourceVariantCount =
                            variants.count {
                                it.exact
                            },
                        discoveredSourceVariantCount =
                            variants.count {
                                !it.exact
                            },
                        sourceVariants =
                            variants
                    )
                }
                .sortedBy {
                    it.catalogKey
                }

        val discovery =
            CanonicalNutritionSourceVariantDiscovery(
                version =
                    CanonicalNutritionSourceVariantDiscovery
                        .CURRENT_VERSION,
                catalogEntryCount =
                    groups.size,
                maximumVariantsPerProduct =
                    maximumVariantsPerProduct,
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
            "Could not create source-variant directory: " +
                    outputDirectory.absolutePath
        }

        val discoveryFile =
            outputDirectory.resolve(
                "nutrition.source-variant-discovery.json"
            )

        writeJson(
            value =
                discovery,
            file =
                discoveryFile
        )

        val report =
            CanonicalNutritionSourceVariantDiscoveryReport(
                version =
                    CanonicalNutritionSourceVariantDiscoveryReport
                        .CURRENT_VERSION,

                catalogEntryCount =
                    groups.size,

                groupCount =
                    groups.size,

                groupsWithExactVariants =
                    groups.count {
                        it.exactSourceVariantCount > 0
                    },

                groupsWithDiscoveredVariants =
                    groups.count {
                        it.discoveredSourceVariantCount > 0
                    },

                groupsWithoutVariants =
                    groups.count {
                        it.sourceVariants.isEmpty()
                    },

                exactVariantCount =
                    groups.sumOf {
                        it.exactSourceVariantCount
                    },

                discoveredVariantCount =
                    groups.sumOf {
                        it.discoveredSourceVariantCount
                    },

                maximumVariantsPerProduct =
                    maximumVariantsPerProduct,

                discoveryFile =
                    discoveryFile.path
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "nutrition-source-variant-discovery.json"
            )

        writeJson(
            value =
                report,
            file =
                reportFile
        )

        println(
            "Nutrition server entries: " +
                    serverEntryCount
        )

        printReport(
            report =
                report,
            reportFile =
                reportFile
        )

        return report
    }

    private fun processSourceIdentity(
        sourceTerm: String,
        serverKey: String,
        retrievalIndex: CatalogRetrievalIndex,
        candidateQueues:
        MutableMap<
                String,
                PriorityQueue<MutableDiscoveredVariant>
                >
    ) {

        val normalizedSourceTerm =
            normalizeKey(
                value =
                    sourceTerm
            )

        if (
            normalizedSourceTerm.isBlank()
        ) {
            return
        }

        val sourceTokens =
            tokenizeNormalized(
                value =
                    normalizedSourceTerm
            )

        if (
            sourceTokens.isEmpty()
        ) {
            return
        }

        val matchingCatalogKeys =
            retrievalIndex.retrieve(
                sourceTokens =
                    sourceTokens
            )

        matchingCatalogKeys
            .forEach { catalogKey ->

                val scored =
                    scoreAgainstCatalogIdentity(
                        catalogKey =
                            catalogKey,
                        normalizedSourceTerm =
                            normalizedSourceTerm,
                        sourceTokens =
                            sourceTokens,
                        serverKey =
                            serverKey,
                        matchedSourceTerm =
                            sourceTerm,
                        retrievalIndex =
                            retrievalIndex
                    )
                        ?: return@forEach

                offerCandidate(
                    queue =
                        requireNotNull(
                            candidateQueues[
                                catalogKey
                            ]
                        ),
                    candidate =
                        scored
                )
            }
    }

    private fun scoreAgainstCatalogIdentity(
        catalogKey: String,
        normalizedSourceTerm: String,
        sourceTokens: Set<String>,
        serverKey: String,
        matchedSourceTerm: String,
        retrievalIndex: CatalogRetrievalIndex
    ): MutableDiscoveredVariant? {

        val catalogTerms =
            retrievalIndex
                .normalizedRetrievalTerms[
                catalogKey
            ]
                .orEmpty()

        var bestScore =
            Double.NEGATIVE_INFINITY

        var bestSharedTokens =
            emptyList<String>()

        catalogTerms.forEach { catalogTerm ->

            val catalogTokens =
                tokenizeNormalized(
                    value =
                        catalogTerm
                )

            val sharedTokens =
                catalogTokens
                    .intersect(
                        sourceTokens
                    )
                    .sorted()

            /*
             * Keine semantische/tokenbasierte Verbindung:
             * kein Discovery-Candidate.
             */
            if (
                sharedTokens.isEmpty()
            ) {
                return@forEach
            }

            val score =
                calculateDiagnosticScore(
                    catalogKey =
                        catalogTerm,
                    serverKey =
                        normalizedSourceTerm,
                    catalogTokens =
                        catalogTokens,
                    serverTokens =
                        sourceTokens,
                    sharedTokens =
                        sharedTokens.toSet()
                )

            if (
                score >
                bestScore
            ) {
                bestScore =
                    score

                bestSharedTokens =
                    sharedTokens
            }
        }

        if (
            bestScore ==
            Double.NEGATIVE_INFINITY
        ) {
            return null
        }

        return MutableDiscoveredVariant(
            serverKey =
                serverKey,
            exact =
                false,
            retrievalScore =
                bestScore,
            sharedTokens =
                bestSharedTokens,
            matchedSourceTerm =
                matchedSourceTerm
        )
    }

    private fun offerCandidate(
        queue:
        PriorityQueue<MutableDiscoveredVariant>,
        candidate:
        MutableDiscoveredVariant
    ) {

        val existing =
            queue.firstOrNull {
                it.serverKey ==
                        candidate.serverKey
            }

        if (
            existing != null
        ) {

            /*
             * Exact darf niemals durch einen Retrieval-Treffer
             * verdrängt werden.
             */
            if (
                existing.exact &&
                !candidate.exact
            ) {
                return
            }

            if (
                !existing.exact &&
                candidate.exact
            ) {

                queue.remove(
                    existing
                )

                queue +=
                    candidate

                return
            }

            if (
                existing.retrievalScore >=
                candidate.retrievalScore
            ) {
                return
            }

            queue.remove(
                existing
            )
        }

        queue +=
            candidate

        /*
         * Exact-Mappings zählen nicht gegen das Discovery-Limit.
         *
         * Ein Produkt darf also z.B.
         *
         * 1 Exact + 20 Discovery
         *
         * besitzen.
         */
        while (
            queue.count {
                !it.exact
            } >
            maximumVariantsPerProduct
        ) {

            val worstRetrievalCandidate =
                queue
                    .filter {
                        !it.exact
                    }
                    .minWithOrNull(
                        WORST_CANDIDATE_FIRST
                    )
                    ?: break

            queue.remove(
                worstRetrievalCandidate
            )
        }
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
            root["mappings"]
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?: error(
                    "Mapping file does not contain 'mappings': " +
                            file.absolutePath
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

                mapping.requiredString(
                    key =
                        "catalogKey"
                ) to
                        mapping.requiredString(
                            key =
                                "serverKey"
                        )
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

    private fun readCanonicalNames(
        file: File
    ): Map<String, String> {

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
            .associate { element ->

                val json =
                    element.asJsonObject

                json.requiredString(
                    key =
                        "normalized"
                ) to
                        json.requiredString(
                            key =
                                "itemname"
                        )
            }
    }

    private fun readSelectedSourceMetadata(
        file: File,
        selectedServerKeys: Set<String>
    ): Map<String, SourceMetadata> {

        val mutable =
            mutableMapOf<
                    String,
                    MutableSourceMetadata
                    >()

        SourceKnowledgeIdentityIndexReader()
            .forEach(
                file =
                    file
            ) { identity ->

                if (
                    NUTRITION_ARTIFACT !in
                    identity.artifacts
                ) {
                    return@forEach
                }

                if (
                    identity.serverKey !in
                    selectedServerKeys
                ) {
                    return@forEach
                }

                val metadata =
                    mutable.getOrPut(
                        identity.serverKey
                    ) {
                        MutableSourceMetadata()
                    }

                metadata.aliases +=
                    identity.aliases

                metadata.matchAliases +=
                    identity.matchAliases
            }

        return mutable
            .mapValues { (_, metadata) ->

                SourceMetadata(
                    aliases =
                        metadata.aliases
                            .asSequence()
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .distinct()
                            .sorted()
                            .toList(),

                    matchAliases =
                        metadata.matchAliases
                            .asSequence()
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .distinct()
                            .sorted()
                            .toList()
                )
            }
    }

    private fun streamServerKeys(
        file: File,
        consume: (String) -> Unit
    ): Int {

        var entryCount =
            0

        file.bufferedReader()
            .use { bufferedReader ->

                JsonReader(
                    bufferedReader
                ).use { reader ->

                    reader.beginObject()

                    var entriesFound =
                        false

                    while (
                        reader.hasNext()
                    ) {

                        when (
                            reader.nextName()
                        ) {

                            "entries" -> {

                                entriesFound =
                                    true

                                reader.beginObject()

                                while (
                                    reader.hasNext()
                                ) {

                                    val serverKey =
                                        reader.nextName()

                                    entryCount++

                                    consume(
                                        serverKey
                                    )

                                    reader.skipValue()
                                }

                                reader.endObject()
                            }

                            else -> {
                                reader.skipValue()
                            }
                        }
                    }

                    reader.endObject()

                    require(
                        entriesFound
                    ) {
                        "Nutrition artifact does not contain entries: " +
                                file.absolutePath
                    }
                }
            }

        return entryCount
    }

    private fun calculateDiagnosticScore(
        catalogKey: String,
        serverKey: String,
        catalogTokens: Set<String>,
        serverTokens: Set<String>,
        sharedTokens: Set<String>
    ): Double {

        if (
            catalogKey ==
            serverKey
        ) {
            return 1.0
        }

        val union =
            catalogTokens +
                    serverTokens

        val jaccard =
            if (
                union.isEmpty()
            ) {
                0.0
            } else {
                sharedTokens.size.toDouble() /
                        union.size.toDouble()
            }

        val containment =
            if (
                catalogTokens.isEmpty()
            ) {
                0.0
            } else {
                sharedTokens.size.toDouble() /
                        catalogTokens.size.toDouble()
            }

        val characterSimilarity =
            normalizedCharacterSimilarity(
                left =
                    catalogKey,
                right =
                    serverKey
            )

        val prefixBonus =
            when {

                serverKey.startsWith(
                    "$catalogKey "
                ) ->
                    PREFIX_BONUS

                catalogKey.startsWith(
                    "$serverKey "
                ) ->
                    PREFIX_BONUS

                else ->
                    0.0
            }

        return (
                TOKEN_JACCARD_WEIGHT *
                        jaccard +
                        TOKEN_CONTAINMENT_WEIGHT *
                        containment +
                        CHARACTER_SIMILARITY_WEIGHT *
                        characterSimilarity +
                        prefixBonus
                )
            .coerceIn(
                minimumValue =
                    0.0,
                maximumValue =
                    1.0
            )
    }

    private fun normalizedCharacterSimilarity(
        left: String,
        right: String
    ): Double {

        val maximumLength =
            max(
                left.length,
                right.length
            )

        if (
            maximumLength == 0
        ) {
            return 1.0
        }

        val distance =
            levenshteinDistance(
                left =
                    left,
                right =
                    right
            )

        return (
                1.0 -
                        distance.toDouble() /
                        maximumLength.toDouble()
                )
            .coerceIn(
                minimumValue =
                    0.0,
                maximumValue =
                    1.0
            )
    }

    private fun levenshteinDistance(
        left: String,
        right: String
    ): Int {

        if (
            left ==
            right
        ) {
            return 0
        }

        if (
            left.isEmpty()
        ) {
            return right.length
        }

        if (
            right.isEmpty()
        ) {
            return left.length
        }

        var previous =
            IntArray(
                right.length + 1
            ) {
                it
            }

        left.forEachIndexed { leftIndex, leftCharacter ->

            val current =
                IntArray(
                    right.length + 1
                )

            current[0] =
                leftIndex + 1

            right.forEachIndexed { rightIndex, rightCharacter ->

                current[
                    rightIndex + 1
                ] =
                    minOf(
                        current[
                            rightIndex
                        ] + 1,

                        previous[
                            rightIndex + 1
                        ] + 1,

                        previous[
                            rightIndex
                        ] +
                                if (
                                    leftCharacter ==
                                    rightCharacter
                                ) {
                                    0
                                } else {
                                    1
                                }
                    )
            }

            previous =
                current
        }

        return previous[
            right.length
        ]
    }

    private fun normalizeKey(
        value: String
    ): String {

        val decomposed =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFKD
            )

        return decomposed
            .replace(
                DIACRITIC_REGEX,
                ""
            )
            .lowercase(
                Locale.ROOT
            )
            .replace(
                "-",
                " "
            )
            .replace(
                "_",
                " "
            )
            .replace(
                NON_KEY_CHARACTER_REGEX,
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()
    }

    private fun tokenizeNormalized(
        value: String
    ): Set<String> =
        value
            .split(
                " "
            )
            .asSequence()
            .filter(
                String::isNotBlank
            )
            .map(
                ::canonicalToken
            )
            .filter(
                String::isNotBlank
            )
            .toSortedSet()

    private fun canonicalToken(
        value: String
    ): String =
        when (
            value
        ) {

            "yoghurt" ->
                "yogurt"

            "sausages" ->
                "sausage"

            "vegetables" ->
                "vegetable"

            "fruits" ->
                "fruit"

            else ->
                if (
                    value.endsWith(
                        "s"
                    ) &&
                    value.length >
                    MINIMUM_SINGULARIZATION_LENGTH
                ) {
                    value.dropLast(
                        1
                    )
                } else {
                    value
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
        CanonicalNutritionSourceVariantDiscoveryReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("NUTRITION SOURCE-VARIANT DISCOVERY RESULT")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries       : " +
                    report.catalogEntryCount
        )
        println(
            "Groups                : " +
                    report.groupCount
        )
        println(
            "Groups with exact     : " +
                    report.groupsWithExactVariants
        )
        println(
            "Groups with discovery : " +
                    report.groupsWithDiscoveredVariants
        )
        println(
            "Groups without variant: " +
                    report.groupsWithoutVariants
        )
        println(
            "Exact variants        : " +
                    report.exactVariantCount
        )
        println(
            "Discovered variants   : " +
                    report.discoveredVariantCount
        )
        println(
            "Max discovery / group : " +
                    report.maximumVariantsPerProduct
        )
        println(
            "Discovery file        : " +
                    report.discoveryFile
        )
        println(
            "Report                : " +
                    reportFile.path
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    private fun JsonObject.requiredString(
        key: String
    ): String =
        optionalString(
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

    private data class MutableDiscoveredVariant(
        val serverKey: String,
        val exact: Boolean,
        val retrievalScore: Double,
        val sharedTokens: List<String>,
        val matchedSourceTerm: String
    )

    private data class MutableSourceMetadata(
        val aliases: MutableList<String> =
            mutableListOf(),
        val matchAliases: MutableList<String> =
            mutableListOf()
    )

    private data class SourceMetadata(
        val aliases: List<String>,
        val matchAliases: List<String>
    )

    private class CatalogRetrievalIndex(
        retrievalTermsByCatalogKey:
        Map<String, List<String>>
    ) {

        val normalizedRetrievalTerms:
                Map<String, List<String>>

        private val tokenIndex:
                Map<String, Set<String>>

        init {

            normalizedRetrievalTerms =
                retrievalTermsByCatalogKey
                    .mapValues { (catalogKey, terms) ->

                        terms
                            .ifEmpty {
                                listOf(
                                    catalogKey
                                )
                            }
                            .asSequence()
                            .map(
                                ::normalizeStatic
                            )
                            .filter(
                                String::isNotBlank
                            )
                            .distinct()
                            .toList()
                    }

            tokenIndex =
                buildMap {

                    normalizedRetrievalTerms
                        .forEach { (catalogKey, terms) ->

                            terms
                                .asSequence()
                                .flatMap { term ->

                                    tokenizeStatic(
                                        term
                                    )
                                        .asSequence()
                                }
                                .distinct()
                                .forEach { token ->

                                    put(
                                        token,
                                        get(token)
                                            .orEmpty() +
                                                catalogKey
                                    )
                                }
                        }
                }
        }

        fun retrieve(
            sourceTokens: Set<String>
        ): Set<String> =
            sourceTokens
                .asSequence()
                .flatMap { token ->

                    tokenIndex[
                        token
                    ]
                        .orEmpty()
                        .asSequence()
                }
                .toSet()

        companion object {

            private fun normalizeStatic(
                value: String
            ): String {

                val decomposed =
                    Normalizer.normalize(
                        value,
                        Normalizer.Form.NFKD
                    )

                return decomposed
                    .replace(
                        DIACRITIC_REGEX,
                        ""
                    )
                    .lowercase(
                        Locale.ROOT
                    )
                    .replace(
                        "-",
                        " "
                    )
                    .replace(
                        "_",
                        " "
                    )
                    .replace(
                        NON_KEY_CHARACTER_REGEX,
                        " "
                    )
                    .replace(
                        WHITESPACE_REGEX,
                        " "
                    )
                    .trim()
            }

            private fun tokenizeStatic(
                value: String
            ): Set<String> =
                value
                    .split(
                        " "
                    )
                    .asSequence()
                    .filter(
                        String::isNotBlank
                    )
                    .map { token ->

                        when (
                            token
                        ) {

                            "yoghurt" ->
                                "yogurt"

                            "sausages" ->
                                "sausage"

                            "vegetables" ->
                                "vegetable"

                            "fruits" ->
                                "fruit"

                            else ->
                                if (
                                    token.endsWith(
                                        "s"
                                    ) &&
                                    token.length >
                                    MINIMUM_SINGULARIZATION_LENGTH
                                ) {
                                    token.dropLast(
                                        1
                                    )
                                } else {
                                    token
                                }
                        }
                    }
                    .filter(
                        String::isNotBlank
                    )
                    .toSortedSet()
        }
    }

    companion object {

        private const val NUTRITION_ARTIFACT =
            "nutrition.json"

        /*
         * Discovery darf breiter sein als die früheren Top-5
         * Match-Requests.
         *
         * Membership Validation reduziert anschließend wieder.
         */
        private const val DEFAULT_MAXIMUM_VARIANTS_PER_PRODUCT =
            20

        private const val TOKEN_JACCARD_WEIGHT =
            0.40

        private const val TOKEN_CONTAINMENT_WEIGHT =
            0.30

        private const val CHARACTER_SIMILARITY_WEIGHT =
            0.25

        private const val PREFIX_BONUS =
            0.05

        private const val MINIMUM_SINGULARIZATION_LENGTH =
            4

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        private val DIACRITIC_REGEX =
            Regex("\\p{M}+")

        private val NON_KEY_CHARACTER_REGEX =
            Regex("[^\\p{L}\\p{N} ]+")

        private val WHITESPACE_REGEX =
            Regex("\\s+")

        private val BEST_CANDIDATE_FIRST =
            compareByDescending<
                    MutableDiscoveredVariant
                    > {
                it.exact
            }
                .thenByDescending {
                    it.retrievalScore
                }
                .thenBy {
                    it.serverKey
                }

        private val WORST_CANDIDATE_FIRST =
            compareBy<
                    MutableDiscoveredVariant
                    > {
                it.retrievalScore
            }
                .thenByDescending {
                    it.serverKey
                }
    }
}