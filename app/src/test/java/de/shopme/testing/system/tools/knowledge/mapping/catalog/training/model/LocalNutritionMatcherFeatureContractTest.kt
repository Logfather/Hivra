package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureExtractor
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureSubsetExtractor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalNutritionMatcherFeatureContractTest {

    @Test
    fun exposeOptimizedProductiveFeatureSubset() {

        val harmfulFeatureNames =
            listOf(
                "domain_form_or_processing_difference_count",
                "domain_modifier_difference_count",
                "domain_unknown_mismatch_count",
            )

        assertEquals(
            expected =
                harmfulFeatureNames,
            actual =
                LocalNutritionMatcherFeatureContract
                    .HARMFUL_DOMAIN_FEATURE_NAMES,
        )

        assertEquals(
            expected =
                11,
            actual =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_COUNT,
        )

        assertEquals(
            expected =
                4,
            actual =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_COUNT,
        )

        assertEquals(
            expected =
                15,
            actual =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_COUNT,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_NAMES +
                        LocalNutritionMatcherFeatureContract
                            .ACTIVE_DOMAIN_FEATURE_NAMES,
            actual =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_NAMES,
            actual =
                LocalNutritionMatcherFeatureContract
                    .OPTIMIZABLE_FEATURE_NAMES,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_NAMES,
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
                                LocalNutritionMatcherFeatureContract
                                    .HARMFUL_DOMAIN_FEATURE_NAMES
                    },
        )

        assertTrue(
            actual =
                LocalNutritionMatcherFeatureContract
                    .HARMFUL_DOMAIN_FEATURE_NAMES
                    .all { featureName ->
                        featureName in
                                LocalNutritionMatcherFeatureContract
                                    .ALL_DOMAIN_FEATURE_NAMES
                    },
        )

        assertTrue(
            actual =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_NAMES
                    .all { featureName ->
                        featureName in
                                LocalNutritionMatcherFeatureContract
                                    .ALL_DOMAIN_FEATURE_NAMES
                    },
        )
    }

    @Test
    fun exposeCompleteHistoricalFeatureContract() {

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_NAMES +
                        LocalNutritionMatcherFeatureContract
                            .ALL_DOMAIN_FEATURE_NAMES,
            actual =
                LocalNutritionMatcherFeatureContract
                    .ALL_FEATURE_NAMES,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_NAMES
                    .size,
            actual =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_COUNT,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ALL_DOMAIN_FEATURE_NAMES
                    .size,
            actual =
                LocalNutritionMatcherFeatureContract
                    .ALL_DOMAIN_FEATURE_COUNT,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_NAMES
                    .size,
            actual =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_COUNT,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES
                    .size,
            actual =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_COUNT,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ALL_FEATURE_NAMES
                    .size,
            actual =
                LocalNutritionMatcherFeatureContract
                    .ALL_FEATURE_COUNT,
        )
    }

    @Test
    fun fullExtractorMatchesCompleteFeatureContract() {

        val extractor =
            LocalNutritionMatcherFeatureExtractor()

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ALL_FEATURE_NAMES,
            actual =
                extractor.featureNames,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ALL_FEATURE_COUNT,
            actual =
                extractor.featureNames
                    .size,
        )
    }

    @Test
    fun productiveSubsetExtractorMatchesActiveFeatureContract() {

        val extractor =
            LocalNutritionMatcherFeatureSubsetExtractor(
                delegate =
                    LocalNutritionMatcherFeatureExtractor(),
                selectedFeatureNames =
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES,
            )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES,
            actual =
                extractor.featureNames,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_COUNT,
            actual =
                extractor.featureNames
                    .size,
        )
    }

    @Test
    fun activeAndHarmfulDomainFeaturesAreDisjoint() {

        val overlappingFeatureNames =
            LocalNutritionMatcherFeatureContract
                .ACTIVE_DOMAIN_FEATURE_NAMES
                .intersect(
                    LocalNutritionMatcherFeatureContract
                        .HARMFUL_DOMAIN_FEATURE_NAMES
                        .toSet(),
                )

        assertTrue(
            actual =
                overlappingFeatureNames.isEmpty(),
            message =
                "Active and harmful Domain-Mismatch features overlap: " +
                        overlappingFeatureNames.joinToString(),
        )
    }

    @Test
    fun featureNamesAreUniqueWithinEveryContract() {

        assertContainsNoDuplicates(
            contractName =
                "base feature contract",
            featureNames =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_NAMES,
        )

        assertContainsNoDuplicates(
            contractName =
                "complete domain feature contract",
            featureNames =
                LocalNutritionMatcherFeatureContract
                    .ALL_DOMAIN_FEATURE_NAMES,
        )

        assertContainsNoDuplicates(
            contractName =
                "active domain feature contract",
            featureNames =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_NAMES,
        )

        assertContainsNoDuplicates(
            contractName =
                "complete feature contract",
            featureNames =
                LocalNutritionMatcherFeatureContract
                    .ALL_FEATURE_NAMES,
        )

        assertContainsNoDuplicates(
            contractName =
                "active feature contract",
            featureNames =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES,
        )
    }

    private fun assertContainsNoDuplicates(
        contractName: String,
        featureNames: List<String>,
    ) {

        val duplicateFeatureNames =
            featureNames
                .groupingBy { featureName ->
                    featureName
                }
                .eachCount()
                .filterValues { count ->
                    count > 1
                }
                .keys
                .sorted()

        assertTrue(
            actual =
                duplicateFeatureNames.isEmpty(),
            message =
                "The $contractName contains duplicate feature names: " +
                        duplicateFeatureNames.joinToString(),
        )
    }
}