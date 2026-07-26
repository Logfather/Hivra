package de.shopme.tools.knowledge.mapping.catalog

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

class DefaultCatalogKnowledgeMatchRequestGenerator(
    private val serverArtifactFile: File? =
        null,
    private val fallbackCandidateCount: Int =
        DEFAULT_FALLBACK_CANDIDATE_COUNT
) : CatalogKnowledgeMatchRequestGenerator {

    init {
        require(fallbackCandidateCount > 0) {
            "fallbackCandidateCount must be greater than zero."
        }
    }

    override fun generate(
        matchReportFile: File
    ): CatalogKnowledgeMatchRequests {

        require(matchReportFile.isFile) {
            "Catalog-server match report does not exist: " +
                    matchReportFile.absolutePath
        }

        val root =
            JsonParser.parseString(
                matchReportFile.readText()
            )

        require(root.isJsonObject) {
            "Catalog-server match report must contain a JSON object: " +
                    matchReportFile.absolutePath
        }

        val rootObject =
            root.asJsonObject

        val artifactName =
            rootObject.requiredString(
                key =
                    "artifactName"
            )

        val fallbackServerKeys =
            readFallbackServerKeys(
                artifactName =
                    artifactName
            )

        val requests =
            rootObject
                .requiredArray(
                    key =
                        "unmatched"
                )
                .asSequence()
                .map { element ->

                    require(element.isJsonObject) {
                        "Unmatched catalog entry must be a JSON object."
                    }

                    element.asJsonObject
                }
                .map { unmatched ->

                    unmatched.toRequest(
                        artifactName =
                            artifactName,
                        fallbackServerKeys =
                            fallbackServerKeys
                    )
                }
                .sortedWith(
                    CatalogKnowledgeMatchRequests.REQUEST_ORDER
                )
                .toList()

        return CatalogKnowledgeMatchRequests(
            version =
                CatalogKnowledgeMatchRequestContract.CURRENT_VERSION,
            requests =
                requests
        )
    }

    private fun JsonObject.toRequest(
        artifactName: String,
        fallbackServerKeys: List<String>
    ): CatalogKnowledgeMatchRequest {

        val catalogKey =
            requiredString(
                key =
                    "catalogKey"
            )

        val persistedCandidates =
            requiredArray(
                key =
                    "nearestCandidates"
            )
                .asSequence()
                .map { element ->

                    require(element.isJsonObject) {
                        "Nearest candidate must be a JSON object."
                    }

                    element.asJsonObject
                }
                .map { candidate ->

                    candidate.toMatchCandidate()
                }
                .sortedWith(
                    CatalogKnowledgeMatchRequest.CANDIDATE_ORDER
                )
                .toList()

        val candidates =
            if (persistedCandidates.isNotEmpty()) {

                persistedCandidates

            } else {

                require(fallbackServerKeys.isNotEmpty()) {
                    "No nearest candidates exist for '$catalogKey' and " +
                            "no server artifact keys are available for " +
                            "deterministic fallback retrieval."
                }

                retrieveFallbackCandidates(
                    catalogKey =
                        catalogKey,
                    serverKeys =
                        fallbackServerKeys
                )
            }

        require(candidates.isNotEmpty()) {
            "No deterministic match candidates could be generated for " +
                    "'$catalogKey' in '$artifactName'."
        }

        return CatalogKnowledgeMatchRequest(
            catalogKey =
                catalogKey,
            serverArtifact =
                artifactName,
            candidates =
                candidates
        )
    }

    private fun retrieveFallbackCandidates(
        catalogKey: String,
        serverKeys: List<String>
    ): List<CatalogKnowledgeMatchCandidate> {

        val normalizedCatalogKey =
            normalizeKey(
                value =
                    catalogKey
            )

        val catalogTokens =
            tokenize(
                value =
                    normalizedCatalogKey
            )

        return serverKeys
            .asSequence()
            .map { serverKey ->

                val normalizedServerKey =
                    normalizeKey(
                        value =
                            serverKey
                    )

                val serverTokens =
                    tokenize(
                        value =
                            normalizedServerKey
                    )

                val sharedTokens =
                    catalogTokens
                        .intersect(
                            serverTokens
                        )
                        .sorted()

                val diagnosticScore =
                    calculateDiagnosticScore(
                        catalogKey =
                            normalizedCatalogKey,
                        serverKey =
                            normalizedServerKey,
                        catalogTokens =
                            catalogTokens,
                        serverTokens =
                            serverTokens,
                        sharedTokens =
                            sharedTokens.toSet()
                    )

                CatalogKnowledgeMatchCandidate(
                    serverKey =
                        serverKey,
                    diagnosticScore =
                        diagnosticScore,
                    sharedTokens =
                        sharedTokens
                )
            }
            .sortedWith(
                CatalogKnowledgeMatchRequest.CANDIDATE_ORDER
            )
            .take(
                fallbackCandidateCount
            )
            .toList()
    }

    private fun calculateDiagnosticScore(
        catalogKey: String,
        serverKey: String,
        catalogTokens: Set<String>,
        serverTokens: Set<String>,
        sharedTokens: Set<String>
    ): Double {

        if (catalogKey == serverKey) {
            return 1.0
        }

        val unionTokens =
            catalogTokens +
                    serverTokens

        val tokenJaccard =
            if (unionTokens.isEmpty()) {
                0.0
            } else {
                sharedTokens.size.toDouble() /
                        unionTokens.size.toDouble()
            }

        val containment =
            if (catalogTokens.isEmpty()) {
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

        if (maximumLength == 0) {
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

        if (left == right) {
            return 0
        }

        if (left.isEmpty()) {
            return right.length
        }

        if (right.isEmpty()) {
            return left.length
        }

        var previousRow =
            IntArray(
                size =
                    right.length + 1
            ) { index ->
                index
            }

        left.forEachIndexed { leftIndex, leftCharacter ->

            val currentRow =
                IntArray(
                    size =
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

    private fun readFallbackServerKeys(
        artifactName: String
    ): List<String> {

        val file =
            serverArtifactFile
                ?: return emptyList()

        require(file.isFile) {
            "Server artifact file does not exist: " +
                    file.absolutePath
        }

        val root =
            JsonParser.parseString(
                file.readText()
            )

        val keys =
            when {

                root.isJsonObject -> {

                    val objectValue =
                        root.asJsonObject

                    when {

                        objectValue[
                            "entries"
                        ]?.isJsonObject ==
                                true ->
                            objectValue[
                                "entries"
                            ]
                                .asJsonObject
                                .keySet()

                        objectValue[
                            "entries"
                        ]?.isJsonArray ==
                                true ->
                            objectValue[
                                "entries"
                            ]
                                .asJsonArray
                                .mapNotNull(
                                    ::readEntryKey
                                )
                                .toSet()

                        else ->
                            objectValue.keySet()
                    }
                }

                root.isJsonArray ->
                    root.asJsonArray
                        .mapNotNull(
                            ::readEntryKey
                        )
                        .toSet()

                else ->
                    error(
                        "Unsupported server artifact structure: " +
                                file.absolutePath
                    )
            }

        return keys
            .asSequence()
            .map(
                String::trim
            )
            .filter(
                String::isNotBlank
            )
            .distinct()
            .sorted()
            .toList()
            .also {
                require(it.isNotEmpty()) {
                    "Server artifact contains no readable keys: " +
                            file.absolutePath
                }
            }
    }

    private fun readEntryKey(
        element: JsonElement
    ): String? {

        if (!element.isJsonObject) {
            return null
        }

        val objectValue =
            element.asJsonObject

        return objectValue.optionalString(
            key =
                "key"
        )
            ?: objectValue.optionalString(
                key =
                    "canonicalKey"
            )
            ?: objectValue.optionalString(
                key =
                    "id"
            )
            ?: objectValue.optionalString(
                key =
                    "name"
            )
    }

    private fun JsonObject.toMatchCandidate():
            CatalogKnowledgeMatchCandidate {

        val sharedTokens =
            requiredArray(
                key =
                    "sharedTokens"
            )
                .asSequence()
                .map { element ->

                    require(
                        element.isJsonPrimitive &&
                                element
                                    .asJsonPrimitive
                                    .isString
                    ) {
                        "sharedTokens must contain strings."
                    }

                    normalizeKey(
                        value =
                            element.asString
                    )
                }
                .filter(
                    String::isNotBlank
                )
                .distinct()
                .sorted()
                .toList()

        return CatalogKnowledgeMatchCandidate(
            serverKey =
                requiredString(
                    key =
                        "serverKey"
                ),
            diagnosticScore =
                requiredDouble(
                    key =
                        "score"
                ),
            sharedTokens =
                sharedTokens
        )
    }

    private fun tokenize(
        value: String
    ): Set<String> {

        return normalizeKey(
            value =
                value
        )
            .split(
                " "
            )
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
    }

    private fun canonicalToken(
        value: String
    ): String {

        return when (value) {
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

    private fun JsonObject.requiredString(
        key: String
    ): String {

        return optionalString(
            key =
                key
        )
            ?: error(
                "Missing or blank '$key'."
            )
    }

    private fun JsonObject.optionalString(
        key: String
    ): String? {

        return get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive &&
                        it.asJsonPrimitive.isString
            }
            ?.asString
            ?.trim()
            ?.takeIf(
                String::isNotBlank
            )
    }

    private fun JsonObject.requiredDouble(
        key: String
    ): Double {

        return get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive &&
                        it.asJsonPrimitive.isNumber
            }
            ?.asDouble
            ?: error(
                "Missing numeric '$key'."
            )
    }

    private fun JsonObject.requiredArray(
        key: String
    ): JsonArray {

        return get(key)
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray
            ?: error(
                "Missing JSON array '$key'."
            )
    }

    private companion object {

        const val DEFAULT_FALLBACK_CANDIDATE_COUNT =
            5

        const val TOKEN_JACCARD_WEIGHT =
            0.40

        const val TOKEN_CONTAINMENT_WEIGHT =
            0.30

        const val CHARACTER_SIMILARITY_WEIGHT =
            0.25

        const val PREFIX_BONUS =
            0.05

        const val MINIMUM_SINGULARIZATION_LENGTH =
            4

        val DIACRITIC_REGEX =
            Regex("\\p{M}+")

        val NON_KEY_CHARACTER_REGEX =
            Regex("[^\\p{L}\\p{N} ]+")

        val WHITESPACE_REGEX =
            Regex("\\s+")
    }
}