package de.shopme.tools.knowledge.catalog.nutrition.group

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import kotlin.math.abs
import kotlin.math.max

enum class UnresolvedNutritionConsensusClassification {

    STRONG_CONSENSUS,

    MODERATE_CONSENSUS,

    WEAK_CONSENSUS,

    INSUFFICIENT_NUTRITION
}

enum class NutritionCandidateConsistency {

    CONSISTENT,

    BORDERLINE,

    OUTLIER,

    INSUFFICIENT_FIELDS
}

data class NutritionSignalProfile(
    val energyKcalPer100g: Double?,
    val fatPer100g: Double?,
    val saturatedFatPer100g: Double?,
    val carbohydratesPer100g: Double?,
    val sugarsPer100g: Double?,
    val fiberPer100g: Double?,
    val proteinsPer100g: Double?,
    val saltPer100g: Double?
) {

    fun values(): Map<String, Double?> =
        linkedMapOf(
            "energyKcalPer100g" to
                    energyKcalPer100g,

            "fatPer100g" to
                    fatPer100g,

            "saturatedFatPer100g" to
                    saturatedFatPer100g,

            "carbohydratesPer100g" to
                    carbohydratesPer100g,

            "sugarsPer100g" to
                    sugarsPer100g,

            "fiberPer100g" to
                    fiberPer100g,

            "proteinsPer100g" to
                    proteinsPer100g,

            "saltPer100g" to
                    saltPer100g
        )

    fun presentFieldCount(): Int =
        values()
            .values
            .count {
                it != null
            }
}

data class UnresolvedNutritionCandidateAnalysis(
    val serverKey: String,
    val membershipReason: String,
    val retrievalScore: Double,
    val membershipConfidence: Double,
    val nutritionAvailable: Boolean,
    val nutrition: NutritionSignalProfile?,
    val comparableFieldCount: Int,
    val medianRelativeDistance: Double?,
    val consistency: NutritionCandidateConsistency
)

data class UnresolvedCanonicalNutritionProductAnalysis(
    val catalogKey: String,
    val canonicalName: String,
    val reviewCandidateCount: Int,
    val candidatesWithNutritionCount: Int,
    val candidatesWithoutNutritionCount: Int,
    val medianNutrition: NutritionSignalProfile?,
    val consensusClassification:
    UnresolvedNutritionConsensusClassification,
    val consistentCandidateCount: Int,
    val borderlineCandidateCount: Int,
    val outlierCandidateCount: Int,
    val consensusRatio: Double,
    val candidates:
    List<UnresolvedNutritionCandidateAnalysis>
)

data class UnresolvedCanonicalNutritionProductsAnalysis(
    val version: Int,
    val catalogEntryCount: Int,
    val unresolvedProductCount: Int,
    val products:
    List<UnresolvedCanonicalNutritionProductAnalysis>
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

data class UnresolvedCanonicalNutritionProductsAnalysisReport(
    val version: Int,
    val catalogEntryCount: Int,
    val unresolvedProductCount: Int,
    val reviewCandidateCount: Int,
    val requiredNutritionServerKeyCount: Int,
    val nutritionServerKeysFound: Int,
    val productsWithNutritionEvidence: Int,
    val productsWithoutNutritionEvidence: Int,
    val strongConsensusProductCount: Int,
    val moderateConsensusProductCount: Int,
    val weakConsensusProductCount: Int,
    val insufficientNutritionProductCount: Int,
    val analysisFile: String
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

class AnalyzeUnresolvedCanonicalNutritionProducts {

    fun analyze(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): UnresolvedCanonicalNutritionProductsAnalysisReport {

        val membershipFile =
            paths.projectRoot.resolve(
                "build/knowledge/source-variant-groups/" +
                        "nutrition.source-variant-membership.resolved.json"
            )

        val nutritionFile =
            paths.serverRoot.resolve(
                "nutrition.json"
            )

        require(
            membershipFile.isFile
        ) {
            "Resolved Nutrition membership file does not exist: " +
                    membershipFile.absolutePath
        }

        require(
            nutritionFile.isFile
        ) {
            "Server nutrition artifact does not exist: " +
                    nutritionFile.absolutePath
        }

        val unresolvedGroups =
            readUnresolvedGroups(
                file =
                    membershipFile
            )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("ANALYZE UNRESOLVED CANONICAL NUTRITION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Unresolved products   : " +
                    unresolvedGroups.size
        )

        val requiredServerKeys =
            unresolvedGroups
                .asSequence()
                .flatMap { group ->
                    group.reviewCandidates
                        .asSequence()
                }
                .map {
                    it.serverKey
                }
                .toSet()

        val nutritionProfiles =
            readSelectedNutritionProfiles(
                file =
                    nutritionFile,
                requiredServerKeys =
                    requiredServerKeys
            )

        println(
            "Required server keys  : " +
                    requiredServerKeys.size
        )

        println(
            "Nutrition keys found  : " +
                    nutritionProfiles.size
        )

        val analyses =
            unresolvedGroups
                .map { group ->

                    analyzeGroup(
                        group =
                            group,
                        nutritionProfiles =
                            nutritionProfiles
                    )
                }
                .sortedBy {
                    it.catalogKey
                }

        val result =
            UnresolvedCanonicalNutritionProductsAnalysis(
                version =
                    UnresolvedCanonicalNutritionProductsAnalysis
                        .CURRENT_VERSION,

                catalogEntryCount =
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT,

                unresolvedProductCount =
                    analyses.size,

                products =
                    analyses
            )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/analysis"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        ) {
            "Could not create analysis directory: " +
                    outputDirectory.absolutePath
        }

        val analysisFile =
            outputDirectory.resolve(
                "unresolved-canonical-nutrition-products.json"
            )

        writeJson(
            value =
                result,
            file =
                analysisFile
        )

        val report =
            UnresolvedCanonicalNutritionProductsAnalysisReport(
                version =
                    UnresolvedCanonicalNutritionProductsAnalysisReport
                        .CURRENT_VERSION,

                catalogEntryCount =
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT,

                unresolvedProductCount =
                    analyses.size,

                reviewCandidateCount =
                    unresolvedGroups.sumOf {
                        it.reviewCandidates.size
                    },

                requiredNutritionServerKeyCount =
                    requiredServerKeys.size,

                nutritionServerKeysFound =
                    nutritionProfiles.size,

                productsWithNutritionEvidence =
                    analyses.count {
                        it.candidatesWithNutritionCount >
                                0
                    },

                productsWithoutNutritionEvidence =
                    analyses.count {
                        it.candidatesWithNutritionCount ==
                                0
                    },

                strongConsensusProductCount =
                    analyses.count {
                        it.consensusClassification ==
                                UnresolvedNutritionConsensusClassification
                                    .STRONG_CONSENSUS
                    },

                moderateConsensusProductCount =
                    analyses.count {
                        it.consensusClassification ==
                                UnresolvedNutritionConsensusClassification
                                    .MODERATE_CONSENSUS
                    },

                weakConsensusProductCount =
                    analyses.count {
                        it.consensusClassification ==
                                UnresolvedNutritionConsensusClassification
                                    .WEAK_CONSENSUS
                    },

                insufficientNutritionProductCount =
                    analyses.count {
                        it.consensusClassification ==
                                UnresolvedNutritionConsensusClassification
                                    .INSUFFICIENT_NUTRITION
                    },

                analysisFile =
                    analysisFile.path
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "unresolved-canonical-nutrition-products-analysis.json"
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

    private fun analyzeGroup(
        group: UnresolvedMembershipGroup,
        nutritionProfiles:
        Map<String, NutritionSignalProfile>
    ): UnresolvedCanonicalNutritionProductAnalysis {

        val candidateProfiles =
            group.reviewCandidates
                .mapNotNull { candidate ->

                    nutritionProfiles[
                        candidate.serverKey
                    ]
                        ?.let { profile ->

                            candidate to
                                    profile
                        }
                }

        val median =
            medianProfile(
                profiles =
                    candidateProfiles.map {
                        it.second
                    }
            )

        val candidateAnalyses =
            group.reviewCandidates
                .map { candidate ->

                    val profile =
                        nutritionProfiles[
                            candidate.serverKey
                        ]

                    if (
                        profile == null ||
                        median == null
                    ) {

                        UnresolvedNutritionCandidateAnalysis(
                            serverKey =
                                candidate.serverKey,

                            membershipReason =
                                candidate.reason,

                            retrievalScore =
                                candidate.retrievalScore,

                            membershipConfidence =
                                candidate.confidence,

                            nutritionAvailable =
                                profile != null,

                            nutrition =
                                profile,

                            comparableFieldCount =
                                0,

                            medianRelativeDistance =
                                null,

                            consistency =
                                NutritionCandidateConsistency
                                    .INSUFFICIENT_FIELDS
                        )

                    } else {

                        val distance =
                            calculateNutritionDistance(
                                candidate =
                                    profile,
                                median =
                                    median
                            )

                        val consistency =
                            classifyCandidateConsistency(
                                comparableFieldCount =
                                    distance.comparableFieldCount,
                                distance =
                                    distance.averageRelativeDistance
                            )

                        UnresolvedNutritionCandidateAnalysis(
                            serverKey =
                                candidate.serverKey,

                            membershipReason =
                                candidate.reason,

                            retrievalScore =
                                candidate.retrievalScore,

                            membershipConfidence =
                                candidate.confidence,

                            nutritionAvailable =
                                true,

                            nutrition =
                                profile,

                            comparableFieldCount =
                                distance.comparableFieldCount,

                            medianRelativeDistance =
                                distance.averageRelativeDistance,

                            consistency =
                                consistency
                        )
                    }
                }
                .sortedWith(
                    compareBy<
                            UnresolvedNutritionCandidateAnalysis
                            > {
                        consistencyOrder(
                            it.consistency
                        )
                    }
                        .thenBy {
                            it.medianRelativeDistance
                                ?: Double.MAX_VALUE
                        }
                        .thenByDescending {
                            it.retrievalScore
                        }
                        .thenBy {
                            it.serverKey
                        }
                )

        val consistentCount =
            candidateAnalyses.count {
                it.consistency ==
                        NutritionCandidateConsistency
                            .CONSISTENT
            }

        val borderlineCount =
            candidateAnalyses.count {
                it.consistency ==
                        NutritionCandidateConsistency
                            .BORDERLINE
            }

        val outlierCount =
            candidateAnalyses.count {
                it.consistency ==
                        NutritionCandidateConsistency
                            .OUTLIER
            }

        val nutritionallyComparable =
            consistentCount +
                    borderlineCount +
                    outlierCount

        val consensusRatio =
            if (
                nutritionallyComparable ==
                0
            ) {
                0.0
            } else {
                consistentCount.toDouble() /
                        nutritionallyComparable.toDouble()
            }

        val consensusClassification =
            classifyGroupConsensus(
                comparableCandidateCount =
                    nutritionallyComparable,
                consistentCandidateCount =
                    consistentCount,
                borderlineCandidateCount =
                    borderlineCount,
                outlierCandidateCount =
                    outlierCount
            )

        return UnresolvedCanonicalNutritionProductAnalysis(
            catalogKey =
                group.catalogKey,

            canonicalName =
                group.canonicalName,

            reviewCandidateCount =
                group.reviewCandidates.size,

            candidatesWithNutritionCount =
                candidateProfiles.size,

            candidatesWithoutNutritionCount =
                group.reviewCandidates.size -
                        candidateProfiles.size,

            medianNutrition =
                median,

            consensusClassification =
                consensusClassification,

            consistentCandidateCount =
                consistentCount,

            borderlineCandidateCount =
                borderlineCount,

            outlierCandidateCount =
                outlierCount,

            consensusRatio =
                consensusRatio,

            candidates =
                candidateAnalyses
        )
    }

    private fun classifyGroupConsensus(
        comparableCandidateCount: Int,
        consistentCandidateCount: Int,
        borderlineCandidateCount: Int,
        outlierCandidateCount: Int
    ): UnresolvedNutritionConsensusClassification {

        if (
            comparableCandidateCount <
            MINIMUM_CANDIDATES_FOR_CONSENSUS
        ) {
            return UnresolvedNutritionConsensusClassification
                .INSUFFICIENT_NUTRITION
        }

        val consistentRatio =
            consistentCandidateCount.toDouble() /
                    comparableCandidateCount.toDouble()

        val acceptableRatio =
            (
                    consistentCandidateCount +
                            borderlineCandidateCount
                    )
                .toDouble() /
                    comparableCandidateCount.toDouble()

        val outlierRatio =
            outlierCandidateCount.toDouble() /
                    comparableCandidateCount.toDouble()

        if (
            comparableCandidateCount >=
            MINIMUM_CANDIDATES_FOR_STRONG_CONSENSUS &&
            consistentRatio >=
            STRONG_CONSENSUS_MINIMUM_CONSISTENT_RATIO &&
            outlierRatio <=
            STRONG_CONSENSUS_MAXIMUM_OUTLIER_RATIO
        ) {
            return UnresolvedNutritionConsensusClassification
                .STRONG_CONSENSUS
        }

        if (
            acceptableRatio >=
            MODERATE_CONSENSUS_MINIMUM_ACCEPTABLE_RATIO
        ) {
            return UnresolvedNutritionConsensusClassification
                .MODERATE_CONSENSUS
        }

        return UnresolvedNutritionConsensusClassification
            .WEAK_CONSENSUS
    }

    private fun classifyCandidateConsistency(
        comparableFieldCount: Int,
        distance: Double?
    ): NutritionCandidateConsistency {

        if (
            comparableFieldCount <
            MINIMUM_COMPARABLE_NUTRITION_FIELDS ||
            distance == null
        ) {
            return NutritionCandidateConsistency
                .INSUFFICIENT_FIELDS
        }

        return when {

            distance <=
                    CONSISTENT_DISTANCE_THRESHOLD ->
                NutritionCandidateConsistency
                    .CONSISTENT

            distance <=
                    BORDERLINE_DISTANCE_THRESHOLD ->
                NutritionCandidateConsistency
                    .BORDERLINE

            else ->
                NutritionCandidateConsistency
                    .OUTLIER
        }
    }

    private fun calculateNutritionDistance(
        candidate: NutritionSignalProfile,
        median: NutritionSignalProfile
    ): NutritionDistance {

        val candidateValues =
            candidate.values()

        val medianValues =
            median.values()

        val distances =
            candidateValues
                .mapNotNull { (field, candidateValue) ->

                    val medianValue =
                        medianValues[
                            field
                        ]

                    if (
                        candidateValue == null ||
                        medianValue == null
                    ) {
                        return@mapNotNull null
                    }

                    val scale =
                        max(
                            abs(
                                medianValue
                            ),
                            minimumScale(
                                field =
                                    field
                            )
                        )

                    abs(
                        candidateValue -
                                medianValue
                    ) /
                            scale
                }

        return NutritionDistance(
            comparableFieldCount =
                distances.size,

            averageRelativeDistance =
                if (
                    distances.isEmpty()
                ) {
                    null
                } else {
                    distances.average()
                }
        )
    }

    private fun minimumScale(
        field: String
    ): Double =
        when (
            field
        ) {

            "energyKcalPer100g" ->
                25.0

            "saltPer100g" ->
                0.25

            else ->
                2.0
        }

    private fun medianProfile(
        profiles: List<NutritionSignalProfile>
    ): NutritionSignalProfile? {

        if (
            profiles.isEmpty()
        ) {
            return null
        }

        return NutritionSignalProfile(
            energyKcalPer100g =
                median(
                    profiles.mapNotNull {
                        it.energyKcalPer100g
                    }
                ),

            fatPer100g =
                median(
                    profiles.mapNotNull {
                        it.fatPer100g
                    }
                ),

            saturatedFatPer100g =
                median(
                    profiles.mapNotNull {
                        it.saturatedFatPer100g
                    }
                ),

            carbohydratesPer100g =
                median(
                    profiles.mapNotNull {
                        it.carbohydratesPer100g
                    }
                ),

            sugarsPer100g =
                median(
                    profiles.mapNotNull {
                        it.sugarsPer100g
                    }
                ),

            fiberPer100g =
                median(
                    profiles.mapNotNull {
                        it.fiberPer100g
                    }
                ),

            proteinsPer100g =
                median(
                    profiles.mapNotNull {
                        it.proteinsPer100g
                    }
                ),

            saltPer100g =
                median(
                    profiles.mapNotNull {
                        it.saltPer100g
                    }
                )
        )
    }

    private fun median(
        values: List<Double>
    ): Double? {

        if (
            values.isEmpty()
        ) {
            return null
        }

        val sorted =
            values.sorted()

        val middle =
            sorted.size /
                    2

        return if (
            sorted.size %
            2 ==
            1
        ) {
            sorted[
                middle
            ]
        } else {
            (
                    sorted[
                        middle - 1
                    ] +
                            sorted[
                                middle
                            ]
                    ) /
                    2.0
        }
    }

    private fun readUnresolvedGroups(
        file: File
    ): List<UnresolvedMembershipGroup> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonObject

        return root
            .requiredArray(
                key =
                    "groups"
            )
            .mapNotNull { element ->

                val group =
                    element.asJsonObject

                val acceptedCount =
                    group
                        .requiredArray(
                            key =
                                "acceptedSourceVariants"
                        )
                        .size()

                if (
                    acceptedCount >
                    0
                ) {
                    return@mapNotNull null
                }

                val reviewCandidates =
                    group
                        .requiredArray(
                            key =
                                "reviewSourceVariants"
                        )
                        .map { candidateElement ->

                            val candidate =
                                candidateElement
                                    .asJsonObject

                            ReviewCandidate(
                                serverKey =
                                    candidate.requiredString(
                                        key =
                                            "serverKey"
                                    ),

                                reason =
                                    candidate.requiredString(
                                        key =
                                            "reason"
                                    ),

                                retrievalScore =
                                    candidate.requiredDouble(
                                        key =
                                            "retrievalScore"
                                    ),

                                confidence =
                                    candidate.requiredDouble(
                                        key =
                                            "confidence"
                                    )
                            )
                        }

                UnresolvedMembershipGroup(
                    catalogKey =
                        group.requiredString(
                            key =
                                "catalogKey"
                        ),

                    canonicalName =
                        group.requiredString(
                            key =
                                "canonicalName"
                        ),

                    reviewCandidates =
                        reviewCandidates
                )
            }
            .sortedBy {
                it.catalogKey
            }
    }

    private fun readSelectedNutritionProfiles(
        file: File,
        requiredServerKeys: Set<String>
    ): Map<String, NutritionSignalProfile> {

        if (
            requiredServerKeys.isEmpty()
        ) {
            return emptyMap()
        }

        val result =
            mutableMapOf<
                    String,
                    NutritionSignalProfile
                    >()

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

                                    if (
                                        serverKey !in
                                        requiredServerKeys
                                    ) {

                                        reader.skipValue()

                                        continue
                                    }

                                    /*
                                     * Nur ausgewählte REVIEW-Keys
                                     * materialisieren.
                                     *
                                     * Dadurch bleibt der komplette
                                     * 512k-Nutrition-Pool streaming.
                                     */
                                    val element =
                                        Streams.parse(
                                            reader
                                        )

                                    extractNutritionProfile(
                                        element =
                                            element
                                    )
                                        ?.let { profile ->

                                            result[
                                                serverKey
                                            ] =
                                                profile
                                        }
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
                        "Nutrition artifact does not contain 'entries': " +
                                file.absolutePath
                    }
                }
            }

        return result
            .toSortedMap()
    }

    private fun extractNutritionProfile(
        element: JsonElement
    ): NutritionSignalProfile? {

        if (
            !element.isJsonObject
        ) {
            return null
        }

        val objectValue =
            element.asJsonObject

        val profile =
            NutritionSignalProfile(
                energyKcalPer100g =
                    findNutritionNumber(
                        json =
                            objectValue,
                        acceptedKeys =
                            ENERGY_KEYS
                    ),

                fatPer100g =
                    findNutritionNumber(
                        json =
                            objectValue,
                        acceptedKeys =
                            FAT_KEYS
                    ),

                saturatedFatPer100g =
                    findNutritionNumber(
                        json =
                            objectValue,
                        acceptedKeys =
                            SATURATED_FAT_KEYS
                    ),

                carbohydratesPer100g =
                    findNutritionNumber(
                        json =
                            objectValue,
                        acceptedKeys =
                            CARBOHYDRATE_KEYS
                    ),

                sugarsPer100g =
                    findNutritionNumber(
                        json =
                            objectValue,
                        acceptedKeys =
                            SUGAR_KEYS
                    ),

                fiberPer100g =
                    findNutritionNumber(
                        json =
                            objectValue,
                        acceptedKeys =
                            FIBER_KEYS
                    ),

                proteinsPer100g =
                    findNutritionNumber(
                        json =
                            objectValue,
                        acceptedKeys =
                            PROTEIN_KEYS
                    ),

                saltPer100g =
                    findNutritionNumber(
                        json =
                            objectValue,
                        acceptedKeys =
                            SALT_KEYS
                    )
            )

        return profile
            .takeIf {
                it.presentFieldCount() >
                        0
            }
    }

    private fun findNutritionNumber(
        json: JsonObject,
        acceptedKeys: Set<String>
    ): Double? {

        /*
         * Primär erwartetes Serverformat:
         *
         * entries[key] = {
         *   energyKcalPer100g: ...,
         *   ...
         * }
         */
        json.entrySet()
            .firstOrNull { (key, value) ->

                normalizeFieldName(
                    key
                ) in
                        acceptedKeys &&
                        value.isJsonPrimitive &&
                        value
                            .asJsonPrimitive
                            .isNumber
            }
            ?.value
            ?.let {
                return it.asDouble
            }

        /*
         * Robust gegenüber einem zusätzlichen
         * payload/value/nutrition-Wrapper.
         */
        NUTRITION_CONTAINER_KEYS
            .forEach { containerKey ->

                json[
                    containerKey
                ]
                    ?.takeIf {
                        it.isJsonObject
                    }
                    ?.asJsonObject
                    ?.let { nested ->

                        nested.entrySet()
                            .firstOrNull { (key, value) ->

                                normalizeFieldName(
                                    key
                                ) in
                                        acceptedKeys &&
                                        value.isJsonPrimitive &&
                                        value
                                            .asJsonPrimitive
                                            .isNumber
                            }
                            ?.value
                            ?.let {
                                return it.asDouble
                            }
                    }
            }

        return null
    }

    private fun normalizeFieldName(
        value: String
    ): String =
        value
            .lowercase()
            .replace(
                NON_ALPHANUMERIC_FIELD,
                ""
            )

    private fun consistencyOrder(
        consistency:
        NutritionCandidateConsistency
    ): Int =
        when (
            consistency
        ) {

            NutritionCandidateConsistency.CONSISTENT ->
                0

            NutritionCandidateConsistency.BORDERLINE ->
                1

            NutritionCandidateConsistency.OUTLIER ->
                2

            NutritionCandidateConsistency.INSUFFICIENT_FIELDS ->
                3
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
        UnresolvedCanonicalNutritionProductsAnalysisReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("UNRESOLVED CANONICAL NUTRITION ANALYSIS")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries             : " +
                    report.catalogEntryCount
        )
        println(
            "Unresolved products         : " +
                    report.unresolvedProductCount
        )
        println(
            "REVIEW candidates           : " +
                    report.reviewCandidateCount
        )
        println(
            "Required server keys        : " +
                    report.requiredNutritionServerKeyCount
        )
        println(
            "Nutrition server keys found : " +
                    report.nutritionServerKeysFound
        )
        println(
            "Products with Nutrition     : " +
                    report.productsWithNutritionEvidence
        )
        println(
            "Products without Nutrition  : " +
                    report.productsWithoutNutritionEvidence
        )
        println()
        println(
            "Strong consensus            : " +
                    report.strongConsensusProductCount
        )
        println(
            "Moderate consensus          : " +
                    report.moderateConsensusProductCount
        )
        println(
            "Weak consensus              : " +
                    report.weakConsensusProductCount
        )
        println(
            "Insufficient Nutrition      : " +
                    report.insufficientNutritionProductCount
        )
        println()
        println(
            "Analysis                    : " +
                    report.analysisFile
        )
        println(
            "Report                      : " +
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
                "Missing or blank '$key'."
            )

    private fun JsonObject.requiredDouble(
        key: String
    ): Double =
        get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive &&
                        it
                            .asJsonPrimitive
                            .isNumber
            }
            ?.asDouble
            ?: error(
                "Missing numeric '$key'."
            )

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

    private data class ReviewCandidate(
        val serverKey: String,
        val reason: String,
        val retrievalScore: Double,
        val confidence: Double
    )

    private data class UnresolvedMembershipGroup(
        val catalogKey: String,
        val canonicalName: String,
        val reviewCandidates: List<ReviewCandidate>
    )

    private data class NutritionDistance(
        val comparableFieldCount: Int,
        val averageRelativeDistance: Double?
    )

    companion object {

        /*
         * Zwei Kandidaten reichen für eine erste
         * Konsistenzbewertung.
         */
        private const val MINIMUM_CANDIDATES_FOR_CONSENSUS =
            2

        /*
         * Für STRONG verlangen wir wenigstens drei
         * unabhängige Nutrition-Profile.
         */
        private const val MINIMUM_CANDIDATES_FOR_STRONG_CONSENSUS =
            3

        /*
         * Mindestens vier der acht Nutrition-Felder
         * müssen Candidate und Median gemeinsam haben.
         */
        private const val MINIMUM_COMPARABLE_NUTRITION_FIELDS =
            4

        private const val CONSISTENT_DISTANCE_THRESHOLD =
            0.30

        private const val BORDERLINE_DISTANCE_THRESHOLD =
            0.65

        private const val STRONG_CONSENSUS_MINIMUM_CONSISTENT_RATIO =
            0.70

        private const val STRONG_CONSENSUS_MAXIMUM_OUTLIER_RATIO =
            0.15

        private const val MODERATE_CONSENSUS_MINIMUM_ACCEPTABLE_RATIO =
            0.70

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        private val NON_ALPHANUMERIC_FIELD =
            Regex(
                "[^a-z0-9]"
            )

        private val NUTRITION_CONTAINER_KEYS =
            setOf(
                "nutrition",
                "payload",
                "value",
                "values"
            )

        private val ENERGY_KEYS =
            setOf(
                "energykcalper100g",
                "energykcal100g",
                "energykcal",
                "calories100g",
                "calories"
            )

        private val FAT_KEYS =
            setOf(
                "fatper100g",
                "fat100g",
                "fat",
                "totalfat100g",
                "totalfat"
            )

        private val SATURATED_FAT_KEYS =
            setOf(
                "saturatedfatper100g",
                "saturatedfat100g",
                "saturatedfat",
                "saturates100g",
                "saturates"
            )

        private val CARBOHYDRATE_KEYS =
            setOf(
                "carbohydratesper100g",
                "carbohydrates100g",
                "carbohydrates",
                "carbohydrate100g",
                "carbohydrate",
                "carbs100g",
                "carbs"
            )

        private val SUGAR_KEYS =
            setOf(
                "sugarsper100g",
                "sugars100g",
                "sugars",
                "sugar100g",
                "sugar"
            )

        private val FIBER_KEYS =
            setOf(
                "fiberper100g",
                "fiber100g",
                "fiber",
                "fibre100g",
                "fibre"
            )

        private val PROTEIN_KEYS =
            setOf(
                "proteinsper100g",
                "proteins100g",
                "proteins",
                "protein100g",
                "protein"
            )

        private val SALT_KEYS =
            setOf(
                "saltper100g",
                "salt100g",
                "salt"
            )
    }
}