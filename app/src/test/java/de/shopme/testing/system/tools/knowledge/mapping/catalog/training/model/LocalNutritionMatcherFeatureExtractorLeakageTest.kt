package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.NutritionDomainMismatchFeatures
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExampleRole
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingLabel
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingProvenance
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherCandidate
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureExtractor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalNutritionMatcherFeatureExtractorLeakageTest {

    @Test
    fun doNotExposeDiagnosticScoreAvailabilityAsFeature() {

        val extractor =
            LocalNutritionMatcherFeatureExtractor()

        assertFalse(
            "diagnostic_score_available" in
                    extractor.featureNames,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract.ALL_FEATURE_COUNT,
            actual =
                extractor.featureNames.size,
        )
    }

    @Test
    fun imputeUnavailableDiagnosticScoresNeutrally() {

        val extractor =
            LocalNutritionMatcherFeatureExtractor()

        val imputationValue =
            0.6842

        val unavailableWithZeroPlaceholder =
            example(
                diagnosticScore =
                    0.0,
                diagnosticScoreAvailable =
                    false,
            )

        val unavailableWithDifferentPlaceholder =
            example(
                diagnosticScore =
                    999.0,
                diagnosticScoreAvailable =
                    false,
            )

        val availableAtImputationValue =
            example(
                diagnosticScore =
                    imputationValue,
                diagnosticScoreAvailable =
                    true,
            )

        val zeroPlaceholderFeatures =
            extractor.extract(
                example =
                    unavailableWithZeroPlaceholder,
                diagnosticScoreImputationValue =
                    imputationValue,
            )

        val differentPlaceholderFeatures =
            extractor.extract(
                example =
                    unavailableWithDifferentPlaceholder,
                diagnosticScoreImputationValue =
                    imputationValue,
            )

        val availableFeatures =
            extractor.extract(
                example =
                    availableAtImputationValue,
                diagnosticScoreImputationValue =
                    imputationValue,
            )

        assertContentEquals(
            expected =
                zeroPlaceholderFeatures,
            actual =
                differentPlaceholderFeatures,
        )

        assertContentEquals(
            expected =
                availableFeatures,
            actual =
                zeroPlaceholderFeatures,
        )
    }

    @Test
    fun includeDomainMismatchCountsWithoutAvailabilityLeakage() {

        val extractor =
            LocalNutritionMatcherFeatureExtractor()

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract.ALL_FEATURE_COUNT,
            actual =
                extractor.featureNames.size,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_NAMES,
            actual =
                extractor.featureNames.take(
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_COUNT,
                ),
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ALL_DOMAIN_FEATURE_NAMES,
            actual =
                extractor.featureNames.drop(
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_COUNT,
                ),
        )

        assertFalse(
            "diagnostic_score_available" in
                    extractor.featureNames,
        )

        assertFalse(
            "domain_feature_version" in
                    extractor.featureNames,
        )

        assertFalse(
            "domain_report_relationship_present" in
                    extractor.featureNames,
        )
    }

    @Test
    fun appendDomainMismatchFeaturesInDeterministicOrder() {

        val extractor =
            LocalNutritionMatcherFeatureExtractor()

        val candidate =
            LocalNutritionMatcherCandidate(
                catalogKey =
                    "vegetarian wrap",
                serverKey =
                    "vegetarian lettuce wrap",
                candidateRank =
                    1,
                candidateCount =
                    5,
                diagnosticScore =
                    0.82,
                diagnosticScoreAvailable =
                    true,
                sharedTokens =
                    listOf(
                        "vegetarian",
                        "wrap",
                    ),
                domainMismatchFeatures =
                    NutritionDomainMismatchFeatures(
                        version = 1,
                        reportRelationshipPresent = true,
                        observationCount = 14,
                        dietOrSubstituteDifferenceCount = 1,
                        crossDomainMismatchCount = 2,
                        sameDomainDifferentEntityCount = 3,
                        formOrProcessingDifferenceCount = 4,
                        regionOrStyleDifferenceCount = 5,
                        compatibleDomainRelationshipCount = 6,
                        unknownTokenInvolvedCount = 7,
                        nonSemanticTokenDifferenceCount = 8,
                        unknownMismatchCount = 9,
                        identityConflictCount = 10,
                        modifierDifferenceCount = 11,
                        knownSemanticObservationCount = 12,
                        unknownSemanticObservationCount = 2,
                    ),
            )

        val features =
            extractor.extract(
                candidate =
                    candidate,
                diagnosticScoreImputationValue =
                    0.5,
            )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract.ALL_FEATURE_COUNT,
                features.size,
        )

        assertContentEquals(
            expected =
                doubleArrayOf(
                    1.0,   // dietOrSubstituteDifferenceCount
                    2.0,   // crossDomainMismatchCount
                    3.0,   // sameDomainDifferentEntityCount
                    4.0,   // formOrProcessingDifferenceCount
                    5.0,   // regionOrStyleDifferenceCount
                    6.0,   // compatibleDomainRelationshipCount
                    7.0,   // unknownTokenInvolvedCount
                    8.0,   // nonSemanticTokenDifferenceCount
                    9.0,   // unknownMismatchCount
                    10.0,  // identityConflictCount
                    11.0,  // modifierDifferenceCount
                ),
            actual =
                features.copyOfRange(
                    fromIndex =
                        LocalNutritionMatcherFeatureContract
                            .BASE_FEATURE_COUNT,
                    toIndex =
                        LocalNutritionMatcherFeatureContract.ALL_FEATURE_COUNT,
                ),
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ALL_DOMAIN_FEATURE_NAMES,
            actual =
                extractor.featureNames.drop(
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_COUNT,
                ),
        )
    }

    @Test
    fun useNeutralDomainMismatchVectorWhenFeaturesAreUnavailable() {

        val extractor =
            LocalNutritionMatcherFeatureExtractor()

        val candidate =
            LocalNutritionMatcherCandidate(
                catalogKey =
                    "apple",
                serverKey =
                    "fresh apple",
                candidateRank =
                    1,
                candidateCount =
                    5,
                diagnosticScore =
                    0.8,
                diagnosticScoreAvailable =
                    true,
                sharedTokens =
                    listOf("apple"),
                domainMismatchFeatures =
                    null,
            )

        val features =
            extractor.extract(
                candidate =
                    candidate,
                diagnosticScoreImputationValue =
                    0.5,
            )

        assertContentEquals(
            expected =
                DoubleArray(
                    LocalNutritionMatcherFeatureContract
                        .ALL_DOMAIN_FEATURE_COUNT,
                ),
            actual =
                features.copyOfRange(
                    fromIndex =
                        LocalNutritionMatcherFeatureContract
                            .BASE_FEATURE_COUNT,
                    toIndex =
                        LocalNutritionMatcherFeatureContract.ALL_FEATURE_COUNT,
                ),
        )
    }

    @Test
    fun reportRelationshipPresenceDoesNotChangeFeatureVector() {

        val extractor =
            LocalNutritionMatcherFeatureExtractor()

        val withoutReport =
            NutritionDomainMismatchFeatures(
                version = 1,
                reportRelationshipPresent = false,
                observationCount = 2,
                modifierDifferenceCount = 1,
                knownSemanticObservationCount = 2,
            )

        val withReport =
            withoutReport.copy(
                reportRelationshipPresent = true,
            )

        fun candidate(
            features: NutritionDomainMismatchFeatures,
        ): LocalNutritionMatcherCandidate {

            return LocalNutritionMatcherCandidate(
                catalogKey =
                    "apple juice",
                serverKey =
                    "organic apple juice",
                candidateRank =
                    1,
                candidateCount =
                    5,
                diagnosticScore =
                    0.8,
                diagnosticScoreAvailable =
                    true,
                sharedTokens =
                    listOf(
                        "apple",
                        "juice",
                    ),
                domainMismatchFeatures =
                    features,
            )
        }

        val first =
            extractor.extract(
                candidate =
                    candidate(withoutReport),
                diagnosticScoreImputationValue =
                    0.5,
            )

        val second =
            extractor.extract(
                candidate =
                    candidate(withReport),
                diagnosticScoreImputationValue =
                    0.5,
            )

        assertContentEquals(
            expected =
                first,
            actual =
                second,
        )
    }

    private fun example(
        diagnosticScore: Double,
        diagnosticScoreAvailable: Boolean,
    ): NutritionMatcherTrainingExample {

        return NutritionMatcherTrainingExample(
            id =
                "fixture",
            catalogKey =
                "fruit yogurt",
            serverArtifact =
                "nutrition.json",
            serverKey =
                "cherry fruit yogurt",
            label =
                NutritionMatcherTrainingLabel.POSITIVE,
            role =
                NutritionMatcherTrainingExampleRole
                    .ACCEPTED_ORIGINAL_MATCH,
            selected =
                true,
            candidateRank =
                1,
            candidateCount =
                5,
            diagnosticScore =
                diagnosticScore,
            diagnosticScoreAvailable =
                diagnosticScoreAvailable,
            sharedTokens =
                listOf(
                    "fruit",
                    "yogurt",
                ),
            domainMismatchFeatures =
                NutritionDomainMismatchFeatures(
                    version = 1,
                    reportRelationshipPresent = false,
                ),
            matcherConfidence =
                0.96,
            originalDecisionType =
                "MATCH",
            originalDecisionReason =
                "Accepted match.",
            originalValidationStatus =
                "ACCEPTED",
            originalValidationReason =
                "Accepted.",
            representativeDecisionType =
                null,
            representativeReasons =
                emptyList(),
            trainingWeight =
                1.0,
            provenance =
                NutritionMatcherTrainingProvenance(
                    sourceType =
                        "TEST",
                    candidateQualityFile =
                        "candidate-quality.json",
                    diagnosticsFile =
                        "diagnostics.json",
                    representativeValidationFile =
                        "validation.json",
                    sourceVersion =
                        1,
                    matcher =
                        "test matcher",
                    validator =
                        "test validator",
                ),
        )
    }

    @Test
    fun excludeHarmfulDomainMismatchFeaturesFromActiveModelContract() {
        assertEquals(
            expected =
                4,
            actual =
                LocalNutritionMatcherFeatureContract.HARMFUL_DOMAIN_FEATURE_NAMES
                    .size,
        )

        assertEquals(
            expected =
                setOf(
                    "domain_cross_domain_mismatch_count",
                    "domain_compatible_relationship_count",
                    "domain_unknown_token_involved_count",
                    "domain_identity_conflict_count",
                ),
            actual =
                LocalNutritionMatcherFeatureContract.HARMFUL_DOMAIN_FEATURE_NAMES
                    .toSet(),
        )

        assertEquals(
            expected =
                7,
            actual =
                LocalNutritionMatcherFeatureContract.ACTIVE_DOMAIN_FEATURE_NAMES
                    .size,
        )

        assertEquals(
            expected =
                listOf(
                    "domain_diet_or_substitute_difference_count",
                    "domain_same_domain_different_entity_count",
                    "domain_form_or_processing_difference_count",
                    "domain_region_or_style_difference_count",
                    "domain_non_semantic_token_difference_count",
                    "domain_unknown_mismatch_count",
                    "domain_modifier_difference_count",
                ),
            actual =
                LocalNutritionMatcherFeatureContract.ACTIVE_DOMAIN_FEATURE_NAMES,
        )

        assertEquals(
            expected =
                18,
            actual =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES
                    .size,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_NAMES,
            actual =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES
                    .take(
                        LocalNutritionMatcherFeatureContract
                            .BASE_FEATURE_COUNT,
                    ),
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract.ACTIVE_DOMAIN_FEATURE_NAMES,
            actual =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES
                    .drop(
                        LocalNutritionMatcherFeatureContract
                            .BASE_FEATURE_COUNT,
                    ),
        )

        assertTrue(
            actual =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES
                    .none { featureName ->
                        featureName in
                                LocalNutritionMatcherFeatureContract.HARMFUL_DOMAIN_FEATURE_NAMES
                    },
        )
    }
}