package de.shopme.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.NutritionDomainMismatchFeatures
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample
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
     * domainMismatchFeatures.observationCount
     * domainMismatchFeatures.knownSemanticObservationCount
     * domainMismatchFeatures.unknownSemanticObservationCount
     *     Beschreiben primär Umfang beziehungsweise Verfügbarkeit der
     *     Domain-Analyse und nicht unmittelbar die semantische Qualität
     *     des konkreten Catalog-Server-Matches.
     */
    override val featureNames: List<String> =
        BASE_FEATURE_NAMES +
                DOMAIN_MISMATCH_FEATURE_NAMES

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

        require(candidate.catalogKey.isNotBlank()) {
            "Local matcher catalogKey must not be blank."
        }

        require(candidate.serverKey.isNotBlank()) {
            "Local matcher serverKey must not be blank."
        }

        require(candidate.candidateCount > 0) {
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

        require(diagnosticScore.isFinite()) {
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
            catalogTokens intersect serverTokens

        val unionTokens =
            catalogTokens union serverTokens

        val catalogTokenCount =
            catalogTokens.size

        val serverTokenCount =
            serverTokens.size

        val maxTokenCount =
            max(
                catalogTokenCount,
                serverTokenCount,
            )

        val minTokenCount =
            min(
                catalogTokenCount,
                serverTokenCount,
            )

        val maxCharacterLength =
            max(
                candidate.catalogKey.length,
                candidate.serverKey.length,
            )

        val minCharacterLength =
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
                calculatedSharedTokens.size.toDouble(),
                safeDivide(
                    numerator =
                        candidate.sharedTokens
                            .distinct()
                            .size
                            .toDouble(),
                    denominator =
                        maxTokenCount.toDouble(),
                ),
                safeDivide(
                    numerator =
                        calculatedSharedTokens.size.toDouble(),
                    denominator =
                        unionTokens.size.toDouble(),
                ),
                safeDivide(
                    numerator =
                        calculatedSharedTokens.size.toDouble(),
                    denominator =
                        catalogTokenCount.toDouble(),
                ),
                safeDivide(
                    numerator =
                        calculatedSharedTokens.size.toDouble(),
                    denominator =
                        serverTokenCount.toDouble(),
                ),
                safeDivide(
                    numerator =
                        minTokenCount.toDouble(),
                    denominator =
                        maxTokenCount.toDouble(),
                ),
                safeDivide(
                    numerator =
                        minCharacterLength.toDouble(),
                    denominator =
                        maxCharacterLength.toDouble(),
                ),
                if (
                    normalize(candidate.catalogKey) ==
                    normalize(candidate.serverKey)
                ) {
                    1.0
                } else {
                    0.0
                },
            )

        val domainMismatchFeatures =
            extractDomainMismatchFeatures(
                features =
                    candidate.domainMismatchFeatures,
            )

        val result =
            baseFeatures +
                    domainMismatchFeatures

        check(
            result.size ==
                    featureNames.size,
        ) {
            "Local nutrition matcher feature vector has " +
                    "${result.size} values, but the feature contract " +
                    "contains ${featureNames.size} names."
        }

        require(
            result.all {
                it.isFinite()
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

        return doubleArrayOf(
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
        )
    }

    private fun validateDomainMismatchFeatures(
        features: NutritionDomainMismatchFeatures,
    ) {

        val allPersistedCounts =
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
            allPersistedCounts.all {
                it >= 0
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

        return normalize(value)
            .split(" ")
            .asSequence()
            .map {
                it.trim()
            }
            .filter {
                it.length >=
                        MIN_TOKEN_LENGTH
            }
            .filterNot {
                it in STOP_TOKENS
            }
            .toSortedSet()
    }

    private fun normalize(
        value: String,
    ): String {

        return value
            .lowercase()
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
            numerator / denominator

        return if (result.isFinite()) {
            result
        } else {
            0.0
        }
    }

    companion object {

        const val BASE_FEATURE_COUNT =
            11

        const val DOMAIN_MISMATCH_FEATURE_COUNT =
            11

        const val FEATURE_COUNT =
            BASE_FEATURE_COUNT +
                    DOMAIN_MISMATCH_FEATURE_COUNT

        const val DOMAIN_MISMATCH_FEATURE_VERSION =
            1

        val BASE_FEATURE_NAMES: List<String> =
            listOf(
                "diagnostic_score",
                "reciprocal_candidate_rank",
                "reciprocal_candidate_count",
                "shared_token_count",
                "shared_token_ratio",
                "token_jaccard",
                "catalog_token_coverage",
                "server_token_coverage",
                "token_count_similarity",
                "character_length_similarity",
                "exact_normalized_match",
            )

        val DOMAIN_MISMATCH_FEATURE_NAMES: List<String> =
            listOf(
                "domain_diet_or_substitute_difference_count",
                "domain_cross_domain_mismatch_count",
                "domain_same_domain_different_entity_count",
                "domain_form_or_processing_difference_count",
                "domain_region_or_style_difference_count",
                "domain_compatible_relationship_count",
                "domain_unknown_token_involved_count",
                "domain_non_semantic_token_difference_count",
                "domain_unknown_mismatch_count",
                "domain_identity_conflict_count",
                "domain_modifier_difference_count",
            )

        private const val MIN_TOKEN_LENGTH =
            2

        private val NON_ALPHANUMERIC_REGEX =
            Regex("[^a-z0-9]+")

        private val WHITESPACE_REGEX =
            Regex("\\s+")

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