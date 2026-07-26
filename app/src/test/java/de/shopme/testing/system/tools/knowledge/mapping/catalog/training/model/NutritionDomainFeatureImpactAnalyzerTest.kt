package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureExtractor
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionDomainFeatureImpactAnalyzer
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionDomainFeatureImpactClassification
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract

class NutritionDomainFeatureImpactAnalyzerTest {

    @Test
    fun classifyEveryDomainFeatureExactlyOnce() {
        val directory =
            createTempDirectory(
                prefix =
                    "nutrition-domain-feature-impact-",
            )
                .toFile()

        try {
            val comparisonReportFile =
                File(
                    directory,
                    "comparison.json",
                )

            comparisonReportFile.writeText(
                text =
                    comparisonReportJson(),
                charset =
                    Charsets.UTF_8,
            )

            val report =
                NutritionDomainFeatureImpactAnalyzer()
                    .analyze(
                        comparisonReportFile =
                            comparisonReportFile,
                    )

            assertEquals(
                expected =
                    1,
                actual =
                    report.version,
            )

            assertEquals(
                expected =
                    1,
                actual =
                    report.sourceComparisonReportVersion,
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureContract
                        .ALL_DOMAIN_FEATURE_NAMES
                        .size,
                actual =
                    report.domainFeatureCount,
            )

            assertEquals(
                expected =
                    report.domainFeatureCount,
                actual =
                    report.harmfulCount +
                            report.neutralCount +
                            report.beneficialCount,
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureContract
                        .ALL_DOMAIN_FEATURE_NAMES,
                actual =
                    report.impacts.map {
                        it.featureName
                    },
            )

            assertEquals(
                expected =
                    report.domainFeatureCount,
                actual =
                    report.impacts
                        .map {
                            it.featureName
                        }
                        .distinct()
                        .size,
            )

            assertEquals(
                expected =
                    NutritionDomainFeatureImpactClassification.HARMFUL,
                actual =
                    report.impacts[0].classification,
            )

            assertEquals(
                expected =
                    NutritionDomainFeatureImpactClassification.BENEFICIAL,
                actual =
                    report.impacts[1].classification,
            )

            assertTrue(
                report.impacts
                    .drop(2)
                    .all { impact ->
                        impact.classification ==
                                NutritionDomainFeatureImpactClassification.NEUTRAL
                    },
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectMissingDomainFeatureComparison() {
        val directory =
            createTempDirectory(
                prefix =
                    "nutrition-domain-feature-impact-missing-",
            )
                .toFile()

        try {
            val comparisonReportFile =
                File(
                    directory,
                    "comparison.json",
                )

            val featureNames =
                LocalNutritionMatcherFeatureContract
                    .ALL_DOMAIN_FEATURE_NAMES
                    .dropLast(1)

            comparisonReportFile.writeText(
                text =
                    comparisonReportJson(
                        featureNames =
                            featureNames,
                    ),
                charset =
                    Charsets.UTF_8,
            )

            val result =
                runCatching {
                    NutritionDomainFeatureImpactAnalyzer()
                        .analyze(
                            comparisonReportFile =
                                comparisonReportFile,
                        )
                }

            assertTrue(
                result.isFailure,
            )

            assertTrue(
                result.exceptionOrNull()
                    ?.message
                    ?.contains(
                        other =
                            "missing features",
                        ignoreCase =
                            true,
                    ) ==
                        true,
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun comparisonReportJson(
        featureNames: List<String> =
            LocalNutritionMatcherFeatureContract
                .ALL_DOMAIN_FEATURE_NAMES,
    ): String {
        val comparisons =
            featureNames
                .mapIndexed { index, featureName ->
                    when (index) {
                        0 ->
                            comparisonJson(
                                featureName =
                                    featureName,
                                deltaPrecision =
                                    -0.01,
                                deltaRecall =
                                    0.0,
                                deltaF1 =
                                    -0.01,
                                deltaBalancedAccuracy =
                                    -0.005,
                                deltaFalsePositiveCount =
                                    3,
                                deltaFalseNegativeCount =
                                    0,
                            )

                        1 ->
                            comparisonJson(
                                featureName =
                                    featureName,
                                deltaPrecision =
                                    0.01,
                                deltaRecall =
                                    0.01,
                                deltaF1 =
                                    0.01,
                                deltaBalancedAccuracy =
                                    0.005,
                                deltaFalsePositiveCount =
                                    -1,
                                deltaFalseNegativeCount =
                                    -1,
                            )

                        else ->
                            comparisonJson(
                                featureName =
                                    featureName,
                                deltaPrecision =
                                    0.0,
                                deltaRecall =
                                    0.0,
                                deltaF1 =
                                    0.0,
                                deltaBalancedAccuracy =
                                    0.0,
                                deltaFalsePositiveCount =
                                    0,
                                deltaFalseNegativeCount =
                                    0,
                            )
                    }
                }
                .joinToString(
                    separator =
                        ",\n",
                )

        return """
            {
              "version": 1,
              "singleFeatureComparisons": [
                $comparisons
              ]
            }
        """
            .trimIndent()
    }

    private fun comparisonJson(
        featureName: String,
        deltaPrecision: Double,
        deltaRecall: Double,
        deltaF1: Double,
        deltaBalancedAccuracy: Double,
        deltaFalsePositiveCount: Int,
        deltaFalseNegativeCount: Int,
    ): String =
        """
            {
              "featureName": "$featureName",
              "delta": {
                "precision": $deltaPrecision,
                "recall": $deltaRecall,
                "f1": $deltaF1,
                "balancedAccuracy": $deltaBalancedAccuracy,
                "falsePositiveCount": $deltaFalsePositiveCount,
                "falseNegativeCount": $deltaFalseNegativeCount
              }
            }
        """
            .trimIndent()
}