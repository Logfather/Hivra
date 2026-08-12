package de.shopme.tools.knowledge.catalog.nutrition.granularity

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.retrieval.CanonicalCatalogRetrievalIdentityReader
import java.io.File
import java.text.Normalizer
import java.util.Locale

enum class CanonicalNutritionIdentityGranularityDecision {

    CORRECT_IDENTITY,

    CANONICAL_IDENTITY_TOO_SPECIFIC,

    CANONICAL_IDENTITY_INVALID_VARIANT,

    UNRESOLVED
}

enum class CanonicalNutritionIdentityGranularityReason {

    CURRENT_IDENTITY_SUPPORTED_BY_SOURCE_POPULATION,

    BROADER_EXISTING_CANONICAL_IDENTITY_DOMINATES,

    VARIANT_ATTRIBUTE_DOMINATES_OVER_BASE_IDENTITY,

    NO_DOMINANT_IDENTITY_SIGNAL,

    INSUFFICIENT_NUTRITION_EVIDENCE
}

data class CanonicalNutritionIdentityCandidateSupport(
    val catalogKey: String,
    val canonicalName: String,
    val supportCount: Int,
    val populationSize: Int,
    val supportRatio: Double,
    val bestRetrievalTerm: String,
    val retrievalTokens: List<String>
)

data class CanonicalNutritionIdentityGranularityAnalysis(
    val catalogKey: String,
    val canonicalName: String,
    val nutritionConsensus: String,
    val nutritionConsensusRatio: Double,
    val nutritionallyUsableCandidateCount: Int,
    val sourceIdentityTokens: List<String>,
    val currentIdentitySupportRatio: Double,
    val decision:
    CanonicalNutritionIdentityGranularityDecision,
    val reason:
    CanonicalNutritionIdentityGranularityReason,
    val proposedCatalogKey: String?,
    val proposedCanonicalName: String?,
    val proposedIdentitySupportRatio: Double?,
    val projectedResolvable: Boolean,
    val competingIdentities:
    List<CanonicalNutritionIdentityCandidateSupport>
)

data class CanonicalNutritionIdentityGranularityResult(
    val version: Int,
    val unresolvedProductCount: Int,
    val analyses:
    List<CanonicalNutritionIdentityGranularityAnalysis>
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

data class CanonicalNutritionIdentityGranularityReport(
    val version: Int,
    val unresolvedProductCount: Int,
    val correctIdentityCount: Int,
    val tooSpecificIdentityCount: Int,
    val invalidVariantIdentityCount: Int,
    val unresolvedIdentityCount: Int,
    val projectedResolvableCount: Int,
    val projectedResolvableCoverage: Double,
    val targetCoverage: Double,
    val targetReached: Boolean,
    val resultFile: String
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}

class AnalyzeCanonicalNutritionIdentityGranularity {

    fun analyze(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): CanonicalNutritionIdentityGranularityReport {

        val unresolvedAnalysisFile =
            paths.projectRoot.resolve(
                "build/knowledge/analysis/" +
                        "unresolved-canonical-nutrition-products.json"
            )

        require(
            unresolvedAnalysisFile.isFile
        ) {
            "Unresolved canonical Nutrition analysis not found: " +
                    unresolvedAnalysisFile.absolutePath
        }

        val catalogIdentities =
            CanonicalCatalogRetrievalIdentityReader()
                .read(
                    file =
                        paths.canonicalFoodCatalog
                )

        require(
            catalogIdentities.size ==
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT
        )

        val catalogNames =
            readCatalogNames(
                file =
                    paths.canonicalFoodCatalog
            )

        val catalogProfiles =
            catalogIdentities
                .associate { identity ->

                    identity.catalogKey to
                            CanonicalIdentityProfile(
                                catalogKey =
                                    identity.catalogKey,

                                canonicalName =
                                    catalogNames[
                                        identity.catalogKey
                                    ]
                                        ?: identity.catalogKey,

                                terms =
                                    identity.retrievalTerms
                                        .asSequence()
                                        .map(
                                            ::normalize
                                        )
                                        .filter(
                                            String::isNotBlank
                                        )
                                        .distinct()
                                        .map { term ->

                                            CanonicalIdentityTerm(
                                                value =
                                                    term,

                                                tokens =
                                                    tokenize(
                                                        term
                                                    )
                                            )
                                        }
                                        .filter {
                                            it.tokens.isNotEmpty()
                                        }
                                        .toList()
                            )
                }

        val unresolvedProducts =
            readUnresolvedProducts(
                file =
                    unresolvedAnalysisFile
            )

        val analyses =
            unresolvedProducts
                .map { product ->

                    analyzeProduct(
                        product =
                            product,
                        catalogProfiles =
                            catalogProfiles
                    )
                }
                .sortedBy {
                    it.catalogKey
                }

        val result =
            CanonicalNutritionIdentityGranularityResult(
                version =
                    CanonicalNutritionIdentityGranularityResult
                        .CURRENT_VERSION,

                unresolvedProductCount =
                    analyses.size,

                analyses =
                    analyses
            )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/analysis"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        )

        val resultFile =
            outputDirectory.resolve(
                "canonical-nutrition-identity-granularity.json"
            )

        writeJson(
            value =
                result,
            file =
                resultFile
        )

        val projectedResolvableCount =
            analyses.count {
                it.projectedResolvable
            }

        val projectedCoverage =
            if (
                analyses.isEmpty()
            ) {
                1.0
            } else {
                projectedResolvableCount.toDouble() /
                        analyses.size.toDouble()
            }

        val report =
            CanonicalNutritionIdentityGranularityReport(
                version =
                    CanonicalNutritionIdentityGranularityReport
                        .CURRENT_VERSION,

                unresolvedProductCount =
                    analyses.size,

                correctIdentityCount =
                    analyses.count {
                        it.decision ==
                                CanonicalNutritionIdentityGranularityDecision
                                    .CORRECT_IDENTITY
                    },

                tooSpecificIdentityCount =
                    analyses.count {
                        it.decision ==
                                CanonicalNutritionIdentityGranularityDecision
                                    .CANONICAL_IDENTITY_TOO_SPECIFIC
                    },

                invalidVariantIdentityCount =
                    analyses.count {
                        it.decision ==
                                CanonicalNutritionIdentityGranularityDecision
                                    .CANONICAL_IDENTITY_INVALID_VARIANT
                    },

                unresolvedIdentityCount =
                    analyses.count {
                        it.decision ==
                                CanonicalNutritionIdentityGranularityDecision
                                    .UNRESOLVED
                    },

                projectedResolvableCount =
                    projectedResolvableCount,

                projectedResolvableCoverage =
                    projectedCoverage,

                targetCoverage =
                    TARGET_RESOLVABLE_COVERAGE,

                targetReached =
                    projectedCoverage >=
                            TARGET_RESOLVABLE_COVERAGE,

                resultFile =
                    resultFile.path
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "canonical-nutrition-identity-granularity-report.json"
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

    private fun analyzeProduct(
        product: UnresolvedNutritionProduct,
        catalogProfiles:
        Map<String, CanonicalIdentityProfile>
    ): CanonicalNutritionIdentityGranularityAnalysis {

        val nutritionallyUsableCandidates =
            product.candidates
                .filter {
                    it.consistency ==
                            "CONSISTENT" ||
                            it.consistency ==
                            "BORDERLINE"
                }

        if (
            nutritionallyUsableCandidates.isEmpty()
        ) {

            return unresolved(
                product =
                    product,
                reason =
                    CanonicalNutritionIdentityGranularityReason
                        .INSUFFICIENT_NUTRITION_EVIDENCE
            )
        }

        val populationTokens =
            nutritionallyUsableCandidates
                .map { candidate ->

                    tokenize(
                        candidate.serverKey
                    )
                }

        val sourceIdentityTokens =
            dominantTokens(
                populations =
                    populationTokens
            )

        val currentProfile =
            catalogProfiles[
                product.catalogKey
            ]
                ?: return unresolved(
                    product =
                        product,
                    reason =
                        CanonicalNutritionIdentityGranularityReason
                            .NO_DOMINANT_IDENTITY_SIGNAL
                )

        val currentSupport =
            calculateIdentitySupport(
                profile =
                    currentProfile,
                candidateTokenSets =
                    populationTokens
            )

        val competing =
            catalogProfiles
                .values
                .asSequence()
                .filter {
                    it.catalogKey !=
                            product.catalogKey
                }
                .mapNotNull { profile ->

                    val support =
                        calculateIdentitySupport(
                            profile =
                                profile,
                            candidateTokenSets =
                                populationTokens
                        )

                    if (
                        support.supportCount <
                        MINIMUM_IDENTITY_SUPPORT_COUNT ||
                        support.supportRatio <
                        MINIMUM_COMPETING_IDENTITY_SUPPORT_RATIO
                    ) {
                        null

                    } else {

                        CanonicalNutritionIdentityCandidateSupport(
                            catalogKey =
                                profile.catalogKey,

                            canonicalName =
                                profile.canonicalName,

                            supportCount =
                                support.supportCount,

                            populationSize =
                                populationTokens.size,

                            supportRatio =
                                support.supportRatio,

                            bestRetrievalTerm =
                                support.bestTerm
                                    ?: "",

                            retrievalTokens =
                                support.bestTokens
                                    .sorted()
                        )
                    }
                }
                .sortedWith(
                    compareByDescending<
                            CanonicalNutritionIdentityCandidateSupport
                            > {
                        it.supportRatio
                    }
                        .thenByDescending {
                            it.retrievalTokens.size
                        }
                        .thenBy {
                            it.catalogKey
                        }
                )
                .take(
                    MAXIMUM_COMPETING_IDENTITIES
                )
                .toList()

        val proposed =
            competing
                .firstOrNull {
                    isValidBroaderIdentity(
                        currentProfile =
                            currentProfile,
                        candidate =
                            it
                    )
                }

        val currentIdentityStrong =
            currentSupport.supportRatio >=
                    CURRENT_IDENTITY_STRONG_SUPPORT_RATIO

        if (
            currentIdentityStrong
        ) {

            return CanonicalNutritionIdentityGranularityAnalysis(
                catalogKey =
                    product.catalogKey,

                canonicalName =
                    product.canonicalName,

                nutritionConsensus =
                    product.consensusClassification,

                nutritionConsensusRatio =
                    product.consensusRatio,

                nutritionallyUsableCandidateCount =
                    populationTokens.size,

                sourceIdentityTokens =
                    sourceIdentityTokens,

                currentIdentitySupportRatio =
                    currentSupport.supportRatio,

                decision =
                    CanonicalNutritionIdentityGranularityDecision
                        .CORRECT_IDENTITY,

                reason =
                    CanonicalNutritionIdentityGranularityReason
                        .CURRENT_IDENTITY_SUPPORTED_BY_SOURCE_POPULATION,

                proposedCatalogKey =
                    null,

                proposedCanonicalName =
                    null,

                proposedIdentitySupportRatio =
                    null,

                projectedResolvable =
                    hasSufficientNutritionConsensus(
                        product
                    ),

                competingIdentities =
                    competing
            )
        }

        if (
            proposed !=
            null
        ) {

            val invalidVariant =
                currentProfile.terms
                    .flatMap {
                        it.tokens
                    }
                    .any {
                        it in
                                NON_IDENTITY_VARIANT_TOKENS
                    }

            return CanonicalNutritionIdentityGranularityAnalysis(
                catalogKey =
                    product.catalogKey,

                canonicalName =
                    product.canonicalName,

                nutritionConsensus =
                    product.consensusClassification,

                nutritionConsensusRatio =
                    product.consensusRatio,

                nutritionallyUsableCandidateCount =
                    populationTokens.size,

                sourceIdentityTokens =
                    sourceIdentityTokens,

                currentIdentitySupportRatio =
                    currentSupport.supportRatio,

                decision =
                    if (
                        invalidVariant
                    ) {
                        CanonicalNutritionIdentityGranularityDecision
                            .CANONICAL_IDENTITY_INVALID_VARIANT
                    } else {
                        CanonicalNutritionIdentityGranularityDecision
                            .CANONICAL_IDENTITY_TOO_SPECIFIC
                    },

                reason =
                    if (
                        invalidVariant
                    ) {
                        CanonicalNutritionIdentityGranularityReason
                            .VARIANT_ATTRIBUTE_DOMINATES_OVER_BASE_IDENTITY
                    } else {
                        CanonicalNutritionIdentityGranularityReason
                            .BROADER_EXISTING_CANONICAL_IDENTITY_DOMINATES
                    },

                proposedCatalogKey =
                    proposed.catalogKey,

                proposedCanonicalName =
                    proposed.canonicalName,

                proposedIdentitySupportRatio =
                    proposed.supportRatio,

                projectedResolvable =
                    hasSufficientNutritionConsensus(
                        product
                    ) &&
                            proposed.supportRatio >=
                            AUTO_RESOLUTION_IDENTITY_SUPPORT_RATIO,

                competingIdentities =
                    competing
            )
        }

        return unresolved(
            product =
                product,
            reason =
                CanonicalNutritionIdentityGranularityReason
                    .NO_DOMINANT_IDENTITY_SIGNAL,
            sourceIdentityTokens =
                sourceIdentityTokens,
            currentIdentitySupportRatio =
                currentSupport.supportRatio,
            competingIdentities =
                competing
        )
    }

    private fun isValidBroaderIdentity(
        currentProfile: CanonicalIdentityProfile,
        candidate:
        CanonicalNutritionIdentityCandidateSupport
    ): Boolean {

        val currentTokenCounts =
            currentProfile.terms
                .map {
                    it.tokens.size
                }
                .filter {
                    it > 0
                }

        if (
            currentTokenCounts.isEmpty()
        ) {
            return false
        }

        val currentMinimumTokenCount =
            currentTokenCounts.min()

        /*
         * Der vorgeschlagene Begriff soll tatsächlich
         * semantisch breiter / mindestens nicht
         * spezifischer sein.
         */
        return candidate.retrievalTokens.size <=
                currentMinimumTokenCount &&
                candidate.supportRatio >=
                AUTO_RESOLUTION_IDENTITY_SUPPORT_RATIO
    }

    private fun calculateIdentitySupport(
        profile: CanonicalIdentityProfile,
        candidateTokenSets: List<Set<String>>
    ): IdentitySupport {

        if (
            candidateTokenSets.isEmpty() ||
            profile.terms.isEmpty()
        ) {

            return IdentitySupport(
                supportCount =
                    0,

                supportRatio =
                    0.0,

                bestTerm =
                    null,

                bestTokens =
                    emptySet()
            )
        }

        var bestSupportCount =
            0

        var bestTerm:
                String? =
            null

        var bestTokens:
                Set<String> =
            emptySet()

        profile.terms
            .forEach { term ->

                val supported =
                    candidateTokenSets.count { candidateTokens ->

                        identityTermMatches(
                            identityTokens =
                                term.tokens,
                            candidateTokens =
                                candidateTokens
                        )
                    }

                if (
                    supported >
                    bestSupportCount ||
                    (
                            supported ==
                                    bestSupportCount &&
                                    term.tokens.size >
                                    bestTokens.size
                            )
                ) {

                    bestSupportCount =
                        supported

                    bestTerm =
                        term.value

                    bestTokens =
                        term.tokens
                }
            }

        return IdentitySupport(
            supportCount =
                bestSupportCount,

            supportRatio =
                bestSupportCount.toDouble() /
                        candidateTokenSets.size.toDouble(),

            bestTerm =
                bestTerm,

            bestTokens =
                bestTokens
        )
    }

    private fun identityTermMatches(
        identityTokens: Set<String>,
        candidateTokens: Set<String>
    ): Boolean {

        if (
            identityTokens.isEmpty() ||
            candidateTokens.isEmpty()
        ) {
            return false
        }

        return candidateTokens.containsAll(
            identityTokens
        )
    }

    private fun dominantTokens(
        populations: List<Set<String>>
    ): List<String> {

        if (
            populations.isEmpty()
        ) {
            return emptyList()
        }

        val minimumOccurrences =
            kotlin.math.ceil(
                populations.size *
                        DOMINANT_SOURCE_TOKEN_RATIO
            )
                .toInt()

        return populations
            .flatten()
            .groupingBy {
                it
            }
            .eachCount()
            .filterValues {
                it >=
                        minimumOccurrences
            }
            .keys
            .filter {
                it !in
                        SOURCE_NOISE_TOKENS
            }
            .sorted()
    }

    private fun hasSufficientNutritionConsensus(
        product: UnresolvedNutritionProduct
    ): Boolean =
        product.consensusClassification ==
                "STRONG_CONSENSUS" ||
                product.consensusClassification ==
                "MODERATE_CONSENSUS"

    private fun unresolved(
        product: UnresolvedNutritionProduct,
        reason:
        CanonicalNutritionIdentityGranularityReason,
        sourceIdentityTokens: List<String> =
            emptyList(),
        currentIdentitySupportRatio: Double =
            0.0,
        competingIdentities:
        List<CanonicalNutritionIdentityCandidateSupport> =
            emptyList()
    ): CanonicalNutritionIdentityGranularityAnalysis {

        return CanonicalNutritionIdentityGranularityAnalysis(
            catalogKey =
                product.catalogKey,

            canonicalName =
                product.canonicalName,

            nutritionConsensus =
                product.consensusClassification,

            nutritionConsensusRatio =
                product.consensusRatio,

            nutritionallyUsableCandidateCount =
                product.candidates.count {
                    it.consistency ==
                            "CONSISTENT" ||
                            it.consistency ==
                            "BORDERLINE"
                },

            sourceIdentityTokens =
                sourceIdentityTokens,

            currentIdentitySupportRatio =
                currentIdentitySupportRatio,

            decision =
                CanonicalNutritionIdentityGranularityDecision
                    .UNRESOLVED,

            reason =
                reason,

            proposedCatalogKey =
                null,

            proposedCanonicalName =
                null,

            proposedIdentitySupportRatio =
                null,

            projectedResolvable =
                false,

            competingIdentities =
                competingIdentities
        )
    }

    private fun readUnresolvedProducts(
        file: File
    ): List<UnresolvedNutritionProduct> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonObject

        return root
            .requiredArray(
                key =
                    "products"
            )
            .map { element ->

                val product =
                    element.asJsonObject

                UnresolvedNutritionProduct(
                    catalogKey =
                        product.requiredString(
                            "catalogKey"
                        ),

                    canonicalName =
                        product.requiredString(
                            "canonicalName"
                        ),

                    consensusClassification =
                        product.requiredString(
                            "consensusClassification"
                        ),

                    consensusRatio =
                        product.requiredDouble(
                            "consensusRatio"
                        ),

                    candidates =
                        product
                            .requiredArray(
                                "candidates"
                            )
                            .map { candidateElement ->

                                val candidate =
                                    candidateElement
                                        .asJsonObject

                                UnresolvedCandidate(
                                    serverKey =
                                        candidate.requiredString(
                                            "serverKey"
                                        ),

                                    consistency =
                                        candidate.requiredString(
                                            "consistency"
                                        )
                                )
                            }
                )
            }
    }

    private fun readCatalogNames(
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
                    "normalized"
                ) to
                        json.requiredString(
                            "itemname"
                        )
            }
    }

    private fun normalize(
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

    private fun tokenize(
        value: String
    ): Set<String> =
        normalize(
            value
        )
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
            .filter {
                it !in
                        SOURCE_NOISE_TOKENS
            }
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
        CanonicalNutritionIdentityGranularityReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CANONICAL NUTRITION IDENTITY GRANULARITY")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Unresolved products       : " +
                    report.unresolvedProductCount
        )
        println(
            "Correct identity          : " +
                    report.correctIdentityCount
        )
        println(
            "Identity too specific     : " +
                    report.tooSpecificIdentityCount
        )
        println(
            "Invalid identity variant  : " +
                    report.invalidVariantIdentityCount
        )
        println(
            "Still unresolved          : " +
                    report.unresolvedIdentityCount
        )
        println()
        println(
            "Projected resolvable      : " +
                    report.projectedResolvableCount
        )
        println(
            "Projected coverage        : " +
                    "%.2f %%".format(
                        Locale.ROOT,
                        report.projectedResolvableCoverage *
                                100.0
                    )
        )
        println(
            "Required coverage         : " +
                    "%.2f %%".format(
                        Locale.ROOT,
                        report.targetCoverage *
                                100.0
                    )
        )
        println(
            "Coverage gate             : " +
                    if (
                        report.targetReached
                    ) {
                        "PASS"
                    } else {
                        "FAIL"
                    }
        )
        println()
        println(
            "Analysis                  : " +
                    report.resultFile
        )
        println(
            "Report                    : " +
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
                "Missing array '$key'."
            )

    private data class CanonicalIdentityProfile(
        val catalogKey: String,
        val canonicalName: String,
        val terms: List<CanonicalIdentityTerm>
    )

    private data class CanonicalIdentityTerm(
        val value: String,
        val tokens: Set<String>
    )

    private data class IdentitySupport(
        val supportCount: Int,
        val supportRatio: Double,
        val bestTerm: String?,
        val bestTokens: Set<String>
    )

    private data class UnresolvedNutritionProduct(
        val catalogKey: String,
        val canonicalName: String,
        val consensusClassification: String,
        val consensusRatio: Double,
        val candidates: List<UnresolvedCandidate>
    )

    private data class UnresolvedCandidate(
        val serverKey: String,
        val consistency: String
    )

    companion object {

        /*
         * Commit target:
         * mindestens 90 % der 1.105 offenen Produkte
         * müssen nach dieser Analyse deterministisch
         * als auflösbar klassifizierbar sein.
         */
        private const val TARGET_RESOLVABLE_COVERAGE =
            0.90

        private const val CURRENT_IDENTITY_STRONG_SUPPORT_RATIO =
            0.70

        private const val MINIMUM_COMPETING_IDENTITY_SUPPORT_RATIO =
            0.50

        private const val AUTO_RESOLUTION_IDENTITY_SUPPORT_RATIO =
            0.70

        private const val MINIMUM_IDENTITY_SUPPORT_COUNT =
            2

        private const val MAXIMUM_COMPETING_IDENTITIES =
            5

        private const val DOMINANT_SOURCE_TOKEN_RATIO =
            0.60

        private const val MINIMUM_SINGULARIZATION_LENGTH =
            4

        /*
         * Diese Tokens beschreiben häufig Claim,
         * Verarbeitung, Form oder Verkaufsvariante,
         * nicht die eigentliche Basisidentität.
         */
        private val NON_IDENTITY_VARIANT_TOKENS =
            setOf(
                "organic",
                "bio",
                "pure",
                "fresh",
                "raw",
                "dried",
                "dry",
                "powder",
                "powdered",
                "frozen",
                "smoked",
                "grilled",
                "roasted",
                "cooked",
                "natural",
                "classic",
                "original",
                "premium",
                "light",
                "dark",
                "mild",
                "spicy",
                "sweet",
                "salted",
                "unsalted"
            )

        private val SOURCE_NOISE_TOKENS =
            setOf(
                "100",
                "percent",
                "brand",
                "product",
                "food",
                "foods",
                "style",
                "type",
                "original",
                "classic",
                "premium"
            )

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
    }
}