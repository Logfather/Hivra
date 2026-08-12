package de.shopme.tools.knowledge.mapping.catalog.rebuild

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

data class RebuiltCatalogServerNearestCandidate(
    val serverKey: String,
    val score: Double,
    val sharedTokens: List<String>
)

data class RebuiltCatalogServerUnmatchedEntry(
    val catalogKey: String,
    val nearestCandidates:
    List<RebuiltCatalogServerNearestCandidate>
)

data class RebuiltCatalogServerMatchReport(
    val version: Int,
    val artifactName: String,
    val catalogEntryCount: Int,
    val exactMappingCount: Int,
    val unmatchedCount: Int,
    val withCandidatesCount: Int,
    val withoutCandidatesCount: Int,
    val nearestCandidateCount: Int,
    val unmatched:
    List<RebuiltCatalogServerUnmatchedEntry>
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

data class CatalogServerMatchReportGenerationArtifact(
    val artifactName: String,
    val serverEntryCount: Int,
    val exactMappingCount: Int,
    val unmatchedCount: Int,
    val withCandidatesCount: Int,
    val withoutCandidatesCount: Int,
    val outputFile: String
)

data class CatalogServerMatchReportGenerationReport(
    val version: Int,
    val catalogEntryCount: Int,
    val serverArtifactCount: Int,
    val nearestCandidateCount: Int,
    val artifacts:
    List<CatalogServerMatchReportGenerationArtifact>
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

class GenerateCatalogServerMatchReports(
    private val nearestCandidateCount: Int =
        DEFAULT_NEAREST_CANDIDATE_COUNT
) {

    init {

        require(
            nearestCandidateCount > 0
        ) {
            "nearestCandidateCount must be greater than zero."
        }
    }

    fun generate(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): CatalogServerMatchReportGenerationReport {

        require(
            paths.canonicalFoodCatalog.isFile
        ) {
            "Canonical food catalog does not exist: " +
                    paths.canonicalFoodCatalog.absolutePath
        }

        require(
            paths.serverRoot.isDirectory
        ) {
            "Server Knowledge directory does not exist: " +
                    paths.serverRoot.absolutePath
        }

        require(
            paths.catalogServerMappings.isFile
        ) {
            "Catalog-server mapping file does not exist: " +
                    paths.catalogServerMappings.absolutePath
        }

        val catalogIdentities =
            CanonicalCatalogRetrievalIdentityReader()
                .read(
                    file =
                        paths.canonicalFoodCatalog
                )

        val catalogKeys =
            catalogIdentities
                .map {
                    it.catalogKey
                }
                .toSortedSet()

        val retrievalTermsByCatalogKey =
            catalogIdentities
                .associate { identity ->
                    identity.catalogKey to
                            identity.retrievalTerms
                }

        require(
            catalogKeys.size ==
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT
        ) {
            "Canonical catalog entry count changed. " +
                    "Expected " +
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT +
                    ", found ${catalogKeys.size}."
        }

        val exactMappingsByArtifact =
            readExactMappings(
                file =
                    paths.catalogServerMappings
            )

        val serverArtifacts =
            paths.serverRoot
                .listFiles()
                .orEmpty()
                .asSequence()
                .filter {
                    it.isFile
                }
                .filter {
                    it.extension.equals(
                        other = "json",
                        ignoreCase = true
                    )
                }
                .sortedBy {
                    it.name
                }
                .toList()

        require(
            serverArtifacts.isNotEmpty()
        ) {
            "No server Knowledge artifacts found in: " +
                    paths.serverRoot.absolutePath
        }

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

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/match-reports"
            )

        resetDirectory(
            directory =
                outputDirectory
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("GENERATE CATALOG → SERVER MATCH REPORTS")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries    : " +
                    catalogKeys.size
        )
        println(
            "Server artifacts   : " +
                    serverArtifacts.size
        )
        println(
            "Candidates per key : " +
                    nearestCandidateCount
        )
        println(
            "Source identities  : " +
                    if (sourceIdentityIndexFile.isFile) {
                        sourceIdentityIndexFile.path
                    } else {
                        "not available"
                    }
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()

        val artifactReports =
            serverArtifacts.map { artifactFile ->

                generateArtifactReport(
                    artifactFile =
                        artifactFile,
                    catalogKeys =
                        catalogKeys,
                    retrievalTermsByCatalogKey =
                        retrievalTermsByCatalogKey,
                    exactCatalogKeys =
                        exactMappingsByArtifact[
                            artifactFile.name
                        ].orEmpty(),
                    outputDirectory =
                        outputDirectory,
                    sourceIdentityIndexFile =
                        sourceIdentityIndexFile
                )
            }

        val generationReport =
            CatalogServerMatchReportGenerationReport(
                version =
                    CatalogServerMatchReportGenerationReport
                        .CURRENT_VERSION,
                catalogEntryCount =
                    catalogKeys.size,
                serverArtifactCount =
                    artifactReports.size,
                nearestCandidateCount =
                    nearestCandidateCount,
                artifacts =
                    artifactReports
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "catalog-server-match-report-generation.json"
            )

        writeJson(
            value =
                generationReport,
            file =
                reportFile
        )

        printGenerationReport(
            report =
                generationReport,
            reportFile =
                reportFile
        )

        return generationReport
    }

    private fun generateArtifactReport(
        artifactFile: File,
        catalogKeys: Set<String>,
        retrievalTermsByCatalogKey:
        Map<String, List<String>>,
        exactCatalogKeys: Set<String>,
        outputDirectory: File,
        sourceIdentityIndexFile: File
    ): CatalogServerMatchReportGenerationArtifact {

        val unresolvedCatalogKeys =
            catalogKeys
                .minus(
                    exactCatalogKeys
                )
                .sorted()

        val retrievalIndex =
            CatalogRetrievalIndex(
                catalogKeys =
                    unresolvedCatalogKeys,
                retrievalTermsByCatalogKey =
                    retrievalTermsByCatalogKey
            )

        val candidateQueues =
            unresolvedCatalogKeys
                .associateWith {
                    PriorityQueue(
                        nearestCandidateCount + 1,
                        WORST_CANDIDATE_FIRST
                    )
                }
                .toMutableMap()

        if (
            sourceIdentityIndexFile.isFile
        ) {

            SourceKnowledgeIdentityIndexReader()
                .forEach(
                    file =
                        sourceIdentityIndexFile
                ) { identity ->

                    if (
                        artifactFile.name !in
                        identity.artifacts
                    ) {
                        return@forEach
                    }

                    val sourceTerms =
                        (
                                identity.aliases +
                                        identity.matchAliases
                                )
                            .asSequence()
                            .map(
                                String::trim
                            )
                            .filter(
                                String::isNotBlank
                            )
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
        }

        val serverEntryCount =
            streamServerKeys(
                file =
                    artifactFile
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

        val unmatched =
            unresolvedCatalogKeys
                .map { catalogKey ->

                    val candidates =
                        candidateQueues[
                            catalogKey
                        ]
                            .orEmpty()
                            .sortedWith(
                                BEST_CANDIDATE_FIRST
                            )

                    RebuiltCatalogServerUnmatchedEntry(
                        catalogKey =
                            catalogKey,
                        nearestCandidates =
                            candidates
                    )
                }

        val withCandidatesCount =
            unmatched.count {
                it.nearestCandidates.isNotEmpty()
            }

        val withoutCandidatesCount =
            unmatched.size -
                    withCandidatesCount

        val matchReport =
            RebuiltCatalogServerMatchReport(
                version =
                    RebuiltCatalogServerMatchReport
                        .CURRENT_VERSION,
                artifactName =
                    artifactFile.name,
                catalogEntryCount =
                    catalogKeys.size,
                exactMappingCount =
                    exactCatalogKeys.size,
                unmatchedCount =
                    unmatched.size,
                withCandidatesCount =
                    withCandidatesCount,
                withoutCandidatesCount =
                    withoutCandidatesCount,
                nearestCandidateCount =
                    nearestCandidateCount,
                unmatched =
                    unmatched
            )

        val outputFile =
            outputDirectory.resolve(
                artifactFile.name
                    .removeSuffix(
                        suffix = ".json"
                    ) +
                        ".matches.json"
            )

        writeJson(
            value =
                matchReport,
            file =
                outputFile
        )

        println(
            artifactFile.name
                .padEnd(30) +
                    "server=" +
                    serverEntryCount
                        .toString()
                        .padStart(8) +
                    " exact=" +
                    exactCatalogKeys.size
                        .toString()
                        .padStart(5) +
                    " unresolved=" +
                    unresolvedCatalogKeys.size
                        .toString()
                        .padStart(5) +
                    " candidates=" +
                    withCandidatesCount
                        .toString()
                        .padStart(5)
        )

        return CatalogServerMatchReportGenerationArtifact(
            artifactName =
                artifactFile.name,
            serverEntryCount =
                serverEntryCount,
            exactMappingCount =
                exactCatalogKeys.size,
            unmatchedCount =
                unresolvedCatalogKeys.size,
            withCandidatesCount =
                withCandidatesCount,
            withoutCandidatesCount =
                withoutCandidatesCount,
            outputFile =
                outputFile.path
        )
    }

    private fun processSourceIdentity(
        sourceTerm: String,
        serverKey: String,
        retrievalIndex: CatalogRetrievalIndex,
        candidateQueues:
        MutableMap<
                String,
                PriorityQueue<
                        RebuiltCatalogServerNearestCandidate
                        >
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

        val candidateCatalogKeys =
            retrievalIndex.retrieve(
                sourceTokens =
                    sourceTokens
            )

        candidateCatalogKeys
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
        retrievalIndex: CatalogRetrievalIndex
    ): RebuiltCatalogServerNearestCandidate? {

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
             * Kein gemeinsames Token =
             * kein plausibler Retrieval-Kandidat.
             *
             * Damit eliminieren wir den bisherigen
             * rein lexikalischen Zufalls-Fallback.
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

        return RebuiltCatalogServerNearestCandidate(
            serverKey =
                serverKey,
            score =
                bestScore,
            sharedTokens =
                bestSharedTokens
        )
    }

    private fun offerCandidate(
        queue:
        PriorityQueue<RebuiltCatalogServerNearestCandidate>,
        candidate:
        RebuiltCatalogServerNearestCandidate
    ) {

        val existing =
            queue.firstOrNull {
                it.serverKey ==
                        candidate.serverKey
            }

        if (
            existing != null
        ) {

            if (
                existing.score >=
                candidate.score
            ) {
                return
            }

            queue.remove(
                existing
            )
        }

        queue +=
            candidate

        if (
            queue.size >
            nearestCandidateCount
        ) {
            queue.poll()
        }
    }

    private fun readExactMappings(
        file: File
    ): Map<String, Set<String>> {

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
            .map { element ->

                val mapping =
                    element.asJsonObject

                val artifact =
                    mapping.optionalString(
                        key =
                            "serverArtifact"
                    )
                        ?: mapping.requiredString(
                            key =
                                "sourceArtifact"
                        )

                artifact to
                        mapping.requiredString(
                            key =
                                "catalogKey"
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
            .mapValues { (_, catalogKeys) ->
                catalogKeys.toSet()
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
                        "Server artifact does not contain 'entries': " +
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

        val unionTokens =
            catalogTokens +
                    serverTokens

        val tokenJaccard =
            if (
                unionTokens.isEmpty()
            ) {
                0.0
            } else {
                sharedTokens.size.toDouble() /
                        unionTokens.size.toDouble()
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
                    prefix =
                        "$catalogKey "
                ) ->
                    PREFIX_BONUS

                catalogKey.startsWith(
                    prefix =
                        "$serverKey "
                ) ->
                    PREFIX_BONUS

                else ->
                    0.0
            }

        return (
                TOKEN_JACCARD_WEIGHT *
                        tokenJaccard +
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

        var previousRow =
            IntArray(
                right.length + 1
            ) { index ->
                index
            }

        left.forEachIndexed { leftIndex, leftCharacter ->

            val currentRow =
                IntArray(
                    right.length + 1
                )

            currentRow[0] =
                leftIndex + 1

            right.forEachIndexed { rightIndex, rightCharacter ->

                val insertionCost =
                    currentRow[
                        rightIndex
                    ] + 1

                val deletionCost =
                    previousRow[
                        rightIndex + 1
                    ] + 1

                val substitutionCost =
                    previousRow[
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

                currentRow[
                    rightIndex + 1
                ] =
                    minOf(
                        insertionCost,
                        deletionCost,
                        substitutionCost
                    )
            }

            previousRow =
                currentRow
        }

        return previousRow[
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
                        suffix =
                            "s"
                    ) &&
                    value.length >
                    MINIMUM_SINGULARIZATION_LENGTH
                ) {
                    value.dropLast(
                        n =
                            1
                    )
                } else {
                    value
                }
        }

    private fun resetDirectory(
        directory: File
    ) {

        if (
            directory.exists()
        ) {

            require(
                directory.deleteRecursively()
            ) {
                "Could not reset match-report directory: " +
                        directory.absolutePath
            }
        }

        require(
            directory.mkdirs()
        ) {
            "Could not create match-report directory: " +
                    directory.absolutePath
        }
    }

    private fun writeJson(
        value: Any,
        file: File
    ) {

        val parentDirectory =
            requireNotNull(
                file.parentFile
            ) {
                "Output file has no parent directory: " +
                        file.absolutePath
            }

        require(
            parentDirectory.exists() ||
                    parentDirectory.mkdirs()
        ) {
            "Could not create output directory: " +
                    parentDirectory.absolutePath
        }

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
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

    private fun printGenerationReport(
        report:
        CatalogServerMatchReportGenerationReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CATALOG → SERVER MATCH REPORT RESULT")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries  : " +
                    report.catalogEntryCount
        )
        println(
            "Artifacts        : " +
                    report.serverArtifactCount
        )
        println(
            "Top candidates   : " +
                    report.nearestCandidateCount
        )

        println()

        report.artifacts
            .forEach { artifact ->

                println(
                    artifact.artifactName
                        .padEnd(30) +
                            " unresolved=" +
                            artifact.unmatchedCount
                                .toString()
                                .padStart(5) +
                            " withCandidates=" +
                            artifact.withCandidatesCount
                                .toString()
                                .padStart(5) +
                            " zero=" +
                            artifact.withoutCandidatesCount
                                .toString()
                                .padStart(5)
                )
            }

        println()
        println(
            "Report           : " +
                    reportFile.path
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    private class CatalogRetrievalIndex(
        catalogKeys: List<String>,
        retrievalTermsByCatalogKey:
        Map<String, List<String>>
    ) {

        val normalizedRetrievalTerms:
                Map<String, List<String>>

        private val tokenIndex:
                Map<String, Set<String>>

        init {

            normalizedRetrievalTerms =
                catalogKeys.associateWith { catalogKey ->

                    retrievalTermsByCatalogKey[
                        catalogKey
                    ]
                        .orEmpty()
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
                                        value =
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
                                        suffix =
                                            "s"
                                    ) &&
                                    token.length >
                                    MINIMUM_SINGULARIZATION_LENGTH
                                ) {
                                    token.dropLast(
                                        n =
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

        private const val DEFAULT_NEAREST_CANDIDATE_COUNT =
            5

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
                    RebuiltCatalogServerNearestCandidate
                    > {
                it.score
            }
                .thenBy {
                    it.serverKey
                }

        private val WORST_CANDIDATE_FIRST =
            compareBy<
                    RebuiltCatalogServerNearestCandidate
                    > {
                it.score
            }
                .thenByDescending {
                    it.serverKey
                }
    }
}