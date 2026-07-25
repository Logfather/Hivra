package de.shopme.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.NutritionDomainMismatchFeatures
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class LocalNutritionMatcherFeatureExtractor :
    LocalNutritionMatcherFeatureProvider {

    /**
     * diagnostic_score_available ist bewusst kein Feature.
     *
     * Es würde die Herkunft des Beispiels verraten:
     *
     * false -> ursprünglich akzeptierter Match
     * true  -> Candidate-Quality-/Rejected-Pipeline
     *
     * Ebenfalls bewusst ausgeschlossen:
     *
     * domainMismatchFeatures.version
     *     Reines Schemafeld.
     *
     * domainMismatchFeatures.reportRelationshipPresent
     *     Beschreibt die Verfügbarkeit beziehungsweise Herkunft der
     *     Reportbeziehung und nicht die semantische Qualität des Matches.
     *
     * Die aggregierten Beobachtungszähler bleiben dagegen Bestandteil
     * des vollständigen historischen Featurevektors. Der produktive
     * Featurevertrag kann daraus über einen Subset-Extractor ausschließlich
     * die aktuell aktiven Features auswählen.
     */
    override val featureNames: List<String> =
        LocalNutritionMatcherFeatureContract
            .ALL_FEATURE_NAMES

    override fun extract(
        example: NutritionMatcherTrainingExample,
        diagnosticScoreImputationValue: Double,
    ): DoubleArray {

        return extract(
            candidate =
                LocalNutritionMatcherCandidate(
                    catalogKey =
                        example.catalogKey,
                    serverKey =
                        example.serverKey,
                    candidateRank =
                        example.candidateRank,
                    candidateCount =
                        example.candidateCount,
                    diagnosticScore =
                        example.diagnosticScore,
                    diagnosticScoreAvailable =
                        example.diagnosticScoreAvailable,
                    sharedTokens =
                        example.sharedTokens,
                    domainMismatchFeatures =
                        example.domainMismatchFeatures,
                ),
            diagnosticScoreImputationValue =
                diagnosticScoreImputationValue,
        )
    }

    override fun extract(
        candidate: LocalNutritionMatcherCandidate,
        diagnosticScoreImputationValue: Double,
    ): DoubleArray {

        require(
            diagnosticScoreImputationValue.isFinite(),
        ) {
            "Diagnostic score imputation value must be finite."
        }

        require(
            candidate.catalogKey.isNotBlank(),
        ) {
            "Local matcher catalogKey must not be blank."
        }

        require(
            candidate.serverKey.isNotBlank(),
        ) {
            "Local matcher serverKey must not be blank."
        }

        require(
            candidate.candidateCount > 0,
        ) {
            "Local matcher candidateCount must be greater than zero."
        }

        require(
            candidate.candidateRank in
                    1..candidate.candidateCount,
        ) {
            "Local matcher candidateRank must be within " +
                    "1..candidateCount."
        }

        val diagnosticScore =
            if (candidate.diagnosticScoreAvailable) {
                candidate.diagnosticScore
            } else {
                diagnosticScoreImputationValue
            }

        require(
            diagnosticScore.isFinite(),
        ) {
            "Effective diagnostic score must be finite."
        }

        val catalogTokens =
            tokenize(
                value =
                    candidate.catalogKey,
            )

        val serverTokens =
            tokenize(
                value =
                    candidate.serverKey,
            )

        val calculatedSharedTokens =
            catalogTokens intersect
                    serverTokens

        val unionTokens =
            catalogTokens union
                    serverTokens

        val catalogTokenCount =
            catalogTokens.size

        val serverTokenCount =
            serverTokens.size

        val maximumTokenCount =
            max(
                catalogTokenCount,
                serverTokenCount,
            )

        val minimumTokenCount =
            min(
                catalogTokenCount,
                serverTokenCount,
            )

        val maximumCharacterLength =
            max(
                candidate.catalogKey.length,
                candidate.serverKey.length,
            )

        val minimumCharacterLength =
            min(
                candidate.catalogKey.length,
                candidate.serverKey.length,
            )

        val baseFeatures =
            doubleArrayOf(
                diagnosticScore,
                safeDivide(
                    numerator =
                        1.0,
                    denominator =
                        candidate.candidateRank.toDouble(),
                ),
                safeDivide(
                    numerator =
                        1.0,
                    denominator =
                        candidate.candidateCount.toDouble(),
                ),
                calculatedSharedTokens
                    .size
                    .toDouble(),
                safeDivide(
                    numerator =
                        candidate.sharedTokens
                            .distinct()
                            .size
                            .toDouble(),
                    denominator =
                        maximumTokenCount.toDouble(),
                ),
                safeDivide(
                    numerator =
                        calculatedSharedTokens
                            .size
                            .toDouble(),
                    denominator =
                        unionTokens
                            .size
                            .toDouble(),
                ),
                safeDivide(
                    numerator =
                        calculatedSharedTokens
                            .size
                            .toDouble(),
                    denominator =
                        catalogTokenCount.toDouble(),
                ),
                safeDivide(
                    numerator =
                        calculatedSharedTokens
                            .size
                            .toDouble(),
                    denominator =
                        serverTokenCount.toDouble(),
                ),
                safeDivide(
                    numerator =
                        minimumTokenCount.toDouble(),
                    denominator =
                        maximumTokenCount.toDouble(),
                ),
                safeDivide(
                    numerator =
                        minimumCharacterLength.toDouble(),
                    denominator =
                        maximumCharacterLength.toDouble(),
                ),
                if (
                    normalize(
                        value =
                            candidate.catalogKey,
                    ) ==
                    normalize(
                        value =
                            candidate.serverKey,
                    )
                ) {
                    1.0
                } else {
                    0.0
                },
            )

        check(
            baseFeatures.size ==
                    BASE_FEATURE_COUNT,
        ) {
            "Local nutrition matcher base feature vector has " +
                    "${baseFeatures.size} values, but the base feature " +
                    "contract contains $BASE_FEATURE_COUNT names."
        }

        val domainFeatures =
            extractDomainMismatchFeatures(
                features =
                    candidate.domainMismatchFeatures,
            )

        check(
            domainFeatures.size ==
                    DOMAIN_MISMATCH_FEATURE_COUNT,
        ) {
            "Local nutrition matcher Domain-Mismatch feature vector has " +
                    "${domainFeatures.size} values, but the complete domain " +
                    "feature contract contains " +
                    "$DOMAIN_MISMATCH_FEATURE_COUNT names."
        }

        val result =
            baseFeatures +
                    domainFeatures

        check(
            result.size ==
                    ALL_FEATURE_COUNT,
        ) {
            "Local nutrition matcher feature vector has " +
                    "${result.size} values, but the complete feature " +
                    "contract contains $ALL_FEATURE_COUNT names."
        }

        check(
            result.size ==
                    featureNames.size,
        ) {
            "Local nutrition matcher feature vector has " +
                    "${result.size} values, but the extractor exposes " +
                    "${featureNames.size} feature names."
        }

        require(
            result.all { value ->
                value.isFinite()
            },
        ) {
            "Local nutrition matcher feature vector contains " +
                    "a non-finite value."
        }

        return result
    }

    private fun extractDomainMismatchFeatures(
        features: NutritionDomainMismatchFeatures?,
    ): DoubleArray {

        if (features == null) {
            return DoubleArray(
                size =
                    DOMAIN_MISMATCH_FEATURE_COUNT,
            )
        }

        require(
            features.version ==
                    DOMAIN_MISMATCH_FEATURE_VERSION,
        ) {
            "Unsupported nutrition Domain-Mismatch feature version: " +
                    features.version
        }

        validateDomainMismatchFeatures(
            features =
                features,
        )

        /*
         * Die Reihenfolge muss exakt
         * LocalNutritionMatcherFeatureContract.ALL_DOMAIN_FEATURE_NAMES
         * entsprechen.
         */
        return doubleArrayOf(
            features.observationCount.toDouble(),
            features.dietOrSubstituteDifferenceCount.toDouble(),
            features.crossDomainMismatchCount.toDouble(),
            features.sameDomainDifferentEntityCount.toDouble(),
            features.formOrProcessingDifferenceCount.toDouble(),
            features.regionOrStyleDifferenceCount.toDouble(),
            features.compatibleDomainRelationshipCount.toDouble(),
            features.unknownTokenInvolvedCount.toDouble(),
            features.nonSemanticTokenDifferenceCount.toDouble(),
            features.unknownMismatchCount.toDouble(),
            features.identityConflictCount.toDouble(),
            features.modifierDifferenceCount.toDouble(),
            features.knownSemanticObservationCount.toDouble(),
            features.unknownSemanticObservationCount.toDouble(),
        )
    }

    private fun validateDomainMismatchFeatures(
        features: NutritionDomainMismatchFeatures,
    ) {

        val persistedCounts =
            intArrayOf(
                features.observationCount,
                features.dietOrSubstituteDifferenceCount,
                features.crossDomainMismatchCount,
                features.sameDomainDifferentEntityCount,
                features.formOrProcessingDifferenceCount,
                features.regionOrStyleDifferenceCount,
                features.compatibleDomainRelationshipCount,
                features.unknownTokenInvolvedCount,
                features.nonSemanticTokenDifferenceCount,
                features.unknownMismatchCount,
                features.identityConflictCount,
                features.modifierDifferenceCount,
                features.knownSemanticObservationCount,
                features.unknownSemanticObservationCount,
            )

        require(
            persistedCounts.all { count ->
                count >= 0
            },
        ) {
            "Nutrition Domain-Mismatch feature counts must not " +
                    "be negative."
        }

        require(
            features.knownSemanticObservationCount +
                    features.unknownSemanticObservationCount <=
                    features.observationCount,
        ) {
            "Known and unknown semantic observations exceed " +
                    "the total Domain-Mismatch observation count."
        }
    }

    private fun tokenize(
        value: String,
    ): Set<String> {

        return normalize(
            value =
                value,
        )
            .split(" ")
            .asSequence()
            .map { token ->
                token.trim()
            }
            .filter { token ->
                token.length >=
                        MIN_TOKEN_LENGTH
            }
            .filterNot { token ->
                token in
                        STOP_TOKENS
            }
            .toSortedSet()
    }

    private fun normalize(
        value: String,
    ): String {

        return value
            .lowercase(
                Locale.ROOT,
            )
            .replace(
                NON_ALPHANUMERIC_REGEX,
                " ",
            )
            .replace(
                WHITESPACE_REGEX,
                " ",
            )
            .trim()
    }

    private fun safeDivide(
        numerator: Double,
        denominator: Double,
    ): Double {

        if (
            denominator == 0.0 ||
            !denominator.isFinite()
        ) {
            return 0.0
        }

        val result =
            numerator /
                    denominator

        return if (result.isFinite()) {
            result
        } else {
            0.0
        }
    }

    companion object {

        /**
         * Compatibility-Zugriff auf die zentral definierten Basisfeatures.
         *
         * Neue Implementierungen sollen direkt
         * LocalNutritionMatcherFeatureContract verwenden.
         */
        val BASE_FEATURE_NAMES: List<String>
            get() =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_NAMES

        /**
         * Compatibility-Name für sämtliche historisch extrahierbaren
         * Domain-Mismatch-Features.
         */
        val DOMAIN_MISMATCH_FEATURE_NAMES: List<String>
            get() =
                LocalNutritionMatcherFeatureContract
                    .ALL_DOMAIN_FEATURE_NAMES

        val ALL_DOMAIN_FEATURE_NAMES: List<String>
            get() =
                LocalNutritionMatcherFeatureContract
                    .ALL_DOMAIN_FEATURE_NAMES

        val ACTIVE_DOMAIN_MISMATCH_FEATURE_NAMES: List<String>
            get() =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_NAMES

        val ACTIVE_DOMAIN_FEATURE_NAMES: List<String>
            get() =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_NAMES

        val HARMFUL_DOMAIN_MISMATCH_FEATURE_NAMES: List<String>
            get() =
                LocalNutritionMatcherFeatureContract
                    .HARMFUL_DOMAIN_FEATURE_NAMES

        val HARMFUL_DOMAIN_FEATURE_NAMES: List<String>
            get() =
                LocalNutritionMatcherFeatureContract
                    .HARMFUL_DOMAIN_FEATURE_NAMES

        val ACTIVE_FEATURE_NAMES: List<String>
            get() =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES

        val ALL_FEATURE_NAMES: List<String>
            get() =
                LocalNutritionMatcherFeatureContract
                    .ALL_FEATURE_NAMES

        val BASE_FEATURE_COUNT: Int
            get() =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_COUNT

        val DOMAIN_MISMATCH_FEATURE_COUNT: Int
            get() =
                LocalNutritionMatcherFeatureContract
                    .ALL_DOMAIN_FEATURE_COUNT

        val ALL_DOMAIN_FEATURE_COUNT: Int
            get() =
                LocalNutritionMatcherFeatureContract
                    .ALL_DOMAIN_FEATURE_COUNT

        val ACTIVE_DOMAIN_MISMATCH_FEATURE_COUNT: Int
            get() =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_COUNT

        val ACTIVE_DOMAIN_FEATURE_COUNT: Int
            get() =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_COUNT

        val ACTIVE_FEATURE_COUNT: Int
            get() =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_COUNT

        val ALL_FEATURE_COUNT: Int
            get() =
                LocalNutritionMatcherFeatureContract
                    .ALL_FEATURE_COUNT

        private const val DOMAIN_MISMATCH_FEATURE_VERSION =
            1

        private const val MIN_TOKEN_LENGTH =
            2

        private val NON_ALPHANUMERIC_REGEX =
            Regex(
                pattern =
                    "[^a-z0-9]+",
            )

        private val WHITESPACE_REGEX =
            Regex(
                pattern =
                    "\\s+",
            )

        private val STOP_TOKENS =
            setOf(
                "and",
                "or",
                "the",
                "with",
                "of",
                "in",
                "on",
                "for",
                "to",
                "a",
                "an",
            )
    }
}