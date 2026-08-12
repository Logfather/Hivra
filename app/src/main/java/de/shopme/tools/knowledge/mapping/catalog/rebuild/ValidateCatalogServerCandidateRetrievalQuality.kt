package de.shopme.tools.knowledge.mapping.catalog.rebuild

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import kotlin.math.abs

data class CatalogServerCandidateQualityBucket(
    val minimumInclusive: Double?,
    val maximumExclusive: Double?,
    val count: Int
)

data class CatalogServerCandidateQualitySample(
    val catalogKey: String,
    val topCandidateServerKey: String?,
    val topScore: Double?,
    val secondScore: Double?,
    val scoreMargin: Double?,
    val sharedTokens: List<String>
)

data class CatalogServerCandidateArtifactQuality(
    val artifactName: String,
    val unmatchedCount: Int,
    val withCandidatesCount: Int,
    val withoutCandidatesCount: Int,

    val top1ScoreMinimum: Double?,
    val top1ScoreMaximum: Double?,
    val top1ScoreAverage: Double?,

    val top1AtLeast090Count: Int,
    val top1AtLeast080Count: Int,
    val top1AtLeast070Count: Int,
    val top1AtLeast060Count: Int,
    val top1AtLeast050Count: Int,
    val top1Below050Count: Int,
    val top1Below030Count: Int,

    val zeroSharedTokenTop1Count: Int,
    val positiveSharedTokenTop1Count: Int,

    val top1Top2MarginAverage: Double?,
    val marginAtLeast020Count: Int,
    val marginAtLeast010Count: Int,
    val marginBelow005Count: Int,

    val uniqueTop1ServerKeyCount: Int,
    val repeatedTop1ServerKeyCount: Int,
    val maximumTop1ServerKeyReuseCount: Int,

    val qualityClassification: String,

    val lowestConfidenceSamples:
    List<CatalogServerCandidateQualitySample>,

    val ambiguousSamples:
    List<CatalogServerCandidateQualitySample>,

    val highestConfidenceSamples:
    List<CatalogServerCandidateQualitySample>
)

data class CatalogServerCandidateRetrievalQualityReport(
    val version: Int,
    val catalogEntryCount: Int,
    val artifactCount: Int,
    val artifacts:
    List<CatalogServerCandidateArtifactQuality>
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

class ValidateCatalogServerCandidateRetrievalQuality(
    private val sampleCount: Int =
        DEFAULT_SAMPLE_COUNT
) {

    init {

        require(
            sampleCount > 0
        ) {
            "sampleCount must be greater than zero."
        }
    }

    fun validate(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): CatalogServerCandidateRetrievalQualityReport {

        val matchReportDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/match-reports"
            )

        require(
            matchReportDirectory.isDirectory
        ) {
            "Catalog-server match report directory does not exist: " +
                    matchReportDirectory.absolutePath
        }

        val matchReportFiles =
            matchReportDirectory
                .listFiles()
                .orEmpty()
                .asSequence()
                .filter {
                    it.isFile
                }
                .filter {
                    it.name.endsWith(
                        suffix =
                            ".matches.json",
                        ignoreCase =
                            true
                    )
                }
                .sortedBy {
                    it.name
                }
                .toList()

        require(
            matchReportFiles.isNotEmpty()
        ) {
            "No catalog-server match reports found in: " +
                    matchReportDirectory.absolutePath
        }

        val artifacts =
            matchReportFiles.map(
                ::analyzeArtifact
            )

        val catalogEntryCounts =
            matchReportFiles
                .map { file ->
                    parseObject(
                        file =
                            file
                    )
                        .requiredInt(
                            key =
                                "catalogEntryCount"
                        )
                }
                .distinct()

        require(
            catalogEntryCounts.size ==
                    1
        ) {
            "Match reports use different catalog entry counts: " +
                    catalogEntryCounts.sorted()
        }

        val report =
            CatalogServerCandidateRetrievalQualityReport(
                version =
                    CatalogServerCandidateRetrievalQualityReport
                        .CURRENT_VERSION,
                catalogEntryCount =
                    catalogEntryCounts.single(),
                artifactCount =
                    artifacts.size,
                artifacts =
                    artifacts
            )

        val outputFile =
            paths.reportsRoot.resolve(
                "catalog-server-candidate-retrieval-quality.json"
            )

        writeJson(
            value =
                report,
            file =
                outputFile
        )

        printReport(
            report =
                report,
            outputFile =
                outputFile
        )

        return report
    }

    private fun analyzeArtifact(
        file: File
    ): CatalogServerCandidateArtifactQuality {

        val root =
            parseObject(
                file =
                    file
            )

        val artifactName =
            root.requiredString(
                key =
                    "artifactName"
            )

        val unmatched =
            root.requiredArray(
                key =
                    "unmatched"
            )
                .map { element ->
                    readCandidateSet(
                        json =
                            element.asJsonObject
                    )
                }

        val withCandidates =
            unmatched.filter {
                it.candidates.isNotEmpty()
            }

        val withoutCandidatesCount =
            unmatched.size -
                    withCandidates.size

        val top1Scores =
            withCandidates.map {
                it.candidates.first().score
            }

        val margins =
            withCandidates
                .mapNotNull { candidateSet ->

                    val first =
                        candidateSet.candidates
                            .getOrNull(0)
                            ?.score
                            ?: return@mapNotNull null

                    val second =
                        candidateSet.candidates
                            .getOrNull(1)
                            ?.score
                            ?: return@mapNotNull null

                    first -
                            second
                }

        val top1ServerKeyCounts =
            withCandidates
                .groupingBy {
                    it.candidates
                        .first()
                        .serverKey
                }
                .eachCount()

        val qualityClassification =
            classifyQuality(
                total =
                    unmatched.size,
                withCandidates =
                    withCandidates.size,
                top1Scores =
                    top1Scores,
                candidateSets =
                    withCandidates,
                margins =
                    margins
            )

        val samples =
            withCandidates.map(
                ::toSample
            )

        val lowestConfidence =
            samples
                .sortedWith(
                    compareBy<CatalogServerCandidateQualitySample> {
                        it.topScore
                    }
                        .thenBy {
                            it.catalogKey
                        }
                )
                .take(
                    sampleCount
                )

        val ambiguous =
            samples
                .filter {
                    it.secondScore != null
                }
                .sortedWith(
                    compareBy<CatalogServerCandidateQualitySample> {
                        abs(
                            it.scoreMargin
                                ?: Double.MAX_VALUE
                        )
                    }
                        .thenByDescending {
                            it.topScore
                                ?: 0.0
                        }
                        .thenBy {
                            it.catalogKey
                        }
                )
                .take(
                    sampleCount
                )

        val highestConfidence =
            samples
                .sortedWith(
                    compareByDescending<CatalogServerCandidateQualitySample> {
                        it.topScore
                            ?: 0.0
                    }
                        .thenByDescending {
                            it.scoreMargin
                                ?: 0.0
                        }
                        .thenBy {
                            it.catalogKey
                        }
                )
                .take(
                    sampleCount
                )

        return CatalogServerCandidateArtifactQuality(
            artifactName =
                artifactName,
            unmatchedCount =
                unmatched.size,
            withCandidatesCount =
                withCandidates.size,
            withoutCandidatesCount =
                withoutCandidatesCount,

            top1ScoreMinimum =
                top1Scores.minOrNull(),
            top1ScoreMaximum =
                top1Scores.maxOrNull(),
            top1ScoreAverage =
                top1Scores.averageOrNull(),

            top1AtLeast090Count =
                top1Scores.count {
                    it >= 0.90
                },
            top1AtLeast080Count =
                top1Scores.count {
                    it >= 0.80
                },
            top1AtLeast070Count =
                top1Scores.count {
                    it >= 0.70
                },
            top1AtLeast060Count =
                top1Scores.count {
                    it >= 0.60
                },
            top1AtLeast050Count =
                top1Scores.count {
                    it >= 0.50
                },
            top1Below050Count =
                top1Scores.count {
                    it < 0.50
                },
            top1Below030Count =
                top1Scores.count {
                    it < 0.30
                },

            zeroSharedTokenTop1Count =
                withCandidates.count {
                    it.candidates
                        .first()
                        .sharedTokens
                        .isEmpty()
                },
            positiveSharedTokenTop1Count =
                withCandidates.count {
                    it.candidates
                        .first()
                        .sharedTokens
                        .isNotEmpty()
                },

            top1Top2MarginAverage =
                margins.averageOrNull(),
            marginAtLeast020Count =
                margins.count {
                    it >= 0.20
                },
            marginAtLeast010Count =
                margins.count {
                    it >= 0.10
                },
            marginBelow005Count =
                margins.count {
                    it < 0.05
                },

            uniqueTop1ServerKeyCount =
                top1ServerKeyCounts.size,
            repeatedTop1ServerKeyCount =
                top1ServerKeyCounts
                    .count {
                        it.value > 1
                    },
            maximumTop1ServerKeyReuseCount =
                top1ServerKeyCounts
                    .values
                    .maxOrNull()
                    ?: 0,

            qualityClassification =
                qualityClassification,

            lowestConfidenceSamples =
                lowestConfidence,

            ambiguousSamples =
                ambiguous,

            highestConfidenceSamples =
                highestConfidence
        )
    }

    private fun readCandidateSet(
        json: JsonObject
    ): CandidateSet {

        val catalogKey =
            json.requiredString(
                key =
                    "catalogKey"
            )

        val candidates =
            json
                .requiredArray(
                    key =
                        "nearestCandidates"
                )
                .map { element ->

                    val candidate =
                        element.asJsonObject

                    Candidate(
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
                                .map {
                                    it.asString
                                        .trim()
                                }
                                .filter(
                                    String::isNotBlank
                                )
                                .distinct()
                                .sorted()
                    )
                }
                .sortedWith(
                    compareByDescending<Candidate> {
                        it.score
                    }
                        .thenBy {
                            it.serverKey
                        }
                )

        return CandidateSet(
            catalogKey =
                catalogKey,
            candidates =
                candidates
        )
    }

    private fun toSample(
        candidateSet: CandidateSet
    ): CatalogServerCandidateQualitySample {

        val top1 =
            candidateSet
                .candidates
                .getOrNull(0)

        val top2 =
            candidateSet
                .candidates
                .getOrNull(1)

        return CatalogServerCandidateQualitySample(
            catalogKey =
                candidateSet.catalogKey,
            topCandidateServerKey =
                top1?.serverKey,
            topScore =
                top1?.score,
            secondScore =
                top2?.score,
            scoreMargin =
                if (
                    top1 != null &&
                    top2 != null
                ) {
                    top1.score -
                            top2.score
                } else {
                    null
                },
            sharedTokens =
                top1
                    ?.sharedTokens
                    .orEmpty()
        )
    }

    private fun classifyQuality(
        total: Int,
        withCandidates: Int,
        top1Scores: List<Double>,
        candidateSets: List<CandidateSet>,
        margins: List<Double>
    ): String {

        if (
            total == 0
        ) {
            return "NO_UNRESOLVED_ITEMS"
        }

        val candidateCoverage =
            withCandidates.toDouble() /
                    total.toDouble()

        if (
            candidateCoverage <
            MINIMUM_CANDIDATE_COVERAGE_FOR_USABLE
        ) {
            return "INSUFFICIENT_RECALL"
        }

        if (
            top1Scores.isEmpty()
        ) {
            return "INSUFFICIENT_RECALL"
        }

        val averageTop1 =
            top1Scores.average()

        val zeroSharedTokenRatio =
            candidateSets
                .count {
                    it.candidates
                        .first()
                        .sharedTokens
                        .isEmpty()
                }
                .toDouble() /
                    candidateSets.size.toDouble()

        val averageMargin =
            margins.averageOrNull()
                ?: 0.0

        return when {

            averageTop1 >=
                    STRONG_AVERAGE_TOP1 &&
                    zeroSharedTokenRatio <=
                    STRONG_MAX_ZERO_TOKEN_RATIO &&
                    averageMargin >=
                    STRONG_MINIMUM_AVERAGE_MARGIN ->
                "STRONG"

            averageTop1 >=
                    USABLE_AVERAGE_TOP1 &&
                    zeroSharedTokenRatio <=
                    USABLE_MAX_ZERO_TOKEN_RATIO ->
                "USABLE_WITH_VALIDATION"

            else ->
                "WEAK_RETRIEVAL"
        }
    }

    private fun parseObject(
        file: File
    ): JsonObject {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )

        require(
            root.isJsonObject
        ) {
            "Expected JSON object: " +
                    file.absolutePath
        }

        return root.asJsonObject
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
                "Missing or blank '$key'."
            )

    private fun JsonObject.requiredInt(
        key: String
    ): Int {

        val value =
            get(key)

        require(
            value != null &&
                    value.isJsonPrimitive &&
                    value
                        .asJsonPrimitive
                        .isNumber
        ) {
            "Missing numeric '$key'."
        }

        return value.asInt
    }

    private fun JsonObject.requiredDouble(
        key: String
    ): Double {

        val value =
            get(key)

        require(
            value != null &&
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

    private fun List<Double>.averageOrNull():
            Double? =
        if (
            isEmpty()
        ) {
            null
        } else {
            average()
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
        CatalogServerCandidateRetrievalQualityReport,
        outputFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CATALOG → SERVER CANDIDATE QUALITY")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries : " +
                    report.catalogEntryCount
        )
        println(
            "Artifacts       : " +
                    report.artifactCount
        )
        println()

        report.artifacts
            .forEach { artifact ->

                println(
                    artifact.artifactName
                        .padEnd(30) +
                            " avg=" +
                            (
                                    artifact.top1ScoreAverage
                                        ?.let {
                                            "%.4f".format(
                                                it
                                            )
                                        }
                                        ?: "-"
                                    )
                                .padStart(7) +
                            " zeroTokens=" +
                            artifact.zeroSharedTokenTop1Count
                                .toString()
                                .padStart(5) +
                            " margin<.05=" +
                            artifact.marginBelow005Count
                                .toString()
                                .padStart(5) +
                            " " +
                            artifact.qualityClassification
                )
            }

        println()
        println(
            "Report          : " +
                    outputFile.path
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    private data class CandidateSet(
        val catalogKey: String,
        val candidates: List<Candidate>
    )

    private data class Candidate(
        val serverKey: String,
        val score: Double,
        val sharedTokens: List<String>
    )

    companion object {

        private const val DEFAULT_SAMPLE_COUNT =
            20

        /*
         * Diese Schwellen klassifizieren ausschließlich
         * Retrieval-Qualität.
         *
         * Sie sind KEINE Auto-Match-Grenzen.
         */
        private const val MINIMUM_CANDIDATE_COVERAGE_FOR_USABLE =
            0.95

        private const val STRONG_AVERAGE_TOP1 =
            0.75

        private const val STRONG_MAX_ZERO_TOKEN_RATIO =
            0.10

        private const val STRONG_MINIMUM_AVERAGE_MARGIN =
            0.10

        private const val USABLE_AVERAGE_TOP1 =
            0.50

        private const val USABLE_MAX_ZERO_TOKEN_RATIO =
            0.35

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}