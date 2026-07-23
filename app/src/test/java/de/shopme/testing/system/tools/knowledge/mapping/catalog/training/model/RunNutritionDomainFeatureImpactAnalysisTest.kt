package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureExtractor
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionDomainFeatureImpactAnalyzer
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionDomainFeatureImpactClassification
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionDomainFeatureImpactReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunNutritionDomainFeatureImpactAnalysisTest {

    @Test
    fun identifyHarmfulDomainMismatchFeatures() {
        val projectDirectory =
            resolveProjectDirectory()

        val comparisonReportFile =
            File(
                projectDirectory,
                "data/generated/knowledge/reports/" +
                        "nutrition.local-matcher-feature-comparison.json",
            )

        val outputFile =
            File(
                projectDirectory,
                "data/generated/knowledge/reports/" +
                        "nutrition.domain-feature-impact.json",
            )

        require(comparisonReportFile.exists()) {
            "Nutrition matcher feature comparison report does not exist: " +
                    comparisonReportFile.absolutePath
        }

        val report =
            NutritionDomainFeatureImpactAnalyzer()
                .analyze(
                    comparisonReportFile =
                        comparisonReportFile,
                )

        NutritionDomainFeatureImpactReportWriter()
            .write(
                report =
                    report,
                outputFile =
                    outputFile,
            )

        val expectedFeatureNames =
            LocalNutritionMatcherFeatureExtractor
                .DOMAIN_MISMATCH_FEATURE_NAMES

        assertEquals(
            expected =
                1,
            actual =
                report.version,
        )

        assertEquals(
            expected =
                expectedFeatureNames.size,
            actual =
                report.domainFeatureCount,
        )

        assertEquals(
            expected =
                expectedFeatureNames,
            actual =
                report.impacts.map {
                    it.featureName
                },
        )

        assertEquals(
            expected =
                expectedFeatureNames.size,
            actual =
                report.harmfulCount +
                        report.neutralCount +
                        report.beneficialCount,
        )

        assertEquals(
            expected =
                expectedFeatureNames.size,
            actual =
                (
                        report.harmfulFeatureNames +
                                report.neutralFeatureNames +
                                report.beneficialFeatureNames
                        )
                    .distinct()
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
                report.harmfulFeatureNames.toSet(),
        )

        assertEquals(
            expected =
                setOf(
                    "domain_diet_or_substitute_difference_count",
                    "domain_same_domain_different_entity_count",
                    "domain_form_or_processing_difference_count",
                    "domain_region_or_style_difference_count",
                    "domain_non_semantic_token_difference_count",
                    "domain_unknown_mismatch_count",
                    "domain_modifier_difference_count",
                ),
            actual =
                report.neutralFeatureNames.toSet(),
        )

        assertTrue(
            report.beneficialFeatureNames.isEmpty(),
        )

        assertTrue(
            report.impacts
                .filter {
                    it.classification ==
                            NutritionDomainFeatureImpactClassification.HARMFUL
                }
                .all { impact ->
                    impact.deltaF1 < 0.0 ||
                            impact.deltaBalancedAccuracy < 0.0 ||
                            impact.deltaFalsePositiveCount > 0
                },
        )

        assertTrue(
            outputFile.exists(),
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("NUTRITION DOMAIN FEATURE IMPACT")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Domain features : ${report.domainFeatureCount}")
        println("Harmful         : ${report.harmfulCount}")
        println("Neutral         : ${report.neutralCount}")
        println("Beneficial      : ${report.beneficialCount}")
        println()
        println("Harmful features:")

        report.impacts
            .filter {
                it.classification ==
                        NutritionDomainFeatureImpactClassification.HARMFUL
            }
            .forEach { impact ->
                println(
                    "  ${impact.featureName}: " +
                            "ΔF1=${impact.deltaF1}, " +
                            "ΔBalAcc=${impact.deltaBalancedAccuracy}, " +
                            "ΔFP=${impact.deltaFalsePositiveCount}, " +
                            "ΔFN=${impact.deltaFalseNegativeCount}",
                )
            }

        println()
        println("Output          : ${outputFile.absolutePath}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty("user.dir"),
                ) {
                    "System property user.dir is missing."
                },
            )
                .absoluteFile

        return generateSequence(
            seed =
                workingDirectory,
        ) { directory ->
            directory.parentFile
        }
            .take(6)
            .firstOrNull { candidate ->
                File(
                    candidate,
                    "data/generated/knowledge/reports/" +
                            "nutrition.local-matcher-feature-comparison.json",
                )
                    .exists()
            }
            ?: error(
                "Could not resolve ShopMe project directory from: " +
                        workingDirectory.absolutePath,
            )
    }
}