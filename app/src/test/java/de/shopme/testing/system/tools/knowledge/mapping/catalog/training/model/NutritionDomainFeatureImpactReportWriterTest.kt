package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionDomainFeatureImpact
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionDomainFeatureImpactClassification
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionDomainFeatureImpactReport
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionDomainFeatureImpactReportWriter
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutritionDomainFeatureImpactReportWriterTest {

    @Test
    fun writeReportDeterministically() {
        val directory =
            createTempDirectory(
                prefix =
                    "nutrition-domain-feature-impact-writer-",
            )
                .toFile()

        try {
            val outputFile =
                File(
                    directory,
                    "nutrition.domain-feature-impact.json",
                )

            val report =
                NutritionDomainFeatureImpactReport(
                    version =
                        1,
                    sourceComparisonReportVersion =
                        1,
                    domainFeatureCount =
                        1,
                    harmfulCount =
                        1,
                    neutralCount =
                        0,
                    beneficialCount =
                        0,
                    harmfulFeatureNames =
                        listOf(
                            "domain_identity_conflict_count",
                        ),
                    neutralFeatureNames =
                        emptyList(),
                    beneficialFeatureNames =
                        emptyList(),
                    impacts =
                        listOf(
                            NutritionDomainFeatureImpact(
                                featureName =
                                    "domain_identity_conflict_count",
                                deltaPrecision =
                                    -0.013386727688787181,
                                deltaRecall =
                                    0.0,
                                deltaF1 =
                                    -0.012288786482335001,
                                deltaBalancedAccuracy =
                                    -0.004132231404958553,
                                deltaFalsePositiveCount =
                                    6,
                                deltaFalseNegativeCount =
                                    0,
                                classification =
                                    NutritionDomainFeatureImpactClassification.HARMFUL,
                            ),
                        ),
                )

            val writer =
                NutritionDomainFeatureImpactReportWriter()

            writer.write(
                report =
                    report,
                outputFile =
                    outputFile,
            )

            val firstContent =
                outputFile.readText(
                    charset =
                        Charsets.UTF_8,
                )

            writer.write(
                report =
                    report,
                outputFile =
                    outputFile,
            )

            val secondContent =
                outputFile.readText(
                    charset =
                        Charsets.UTF_8,
                )

            assertEquals(
                expected =
                    firstContent,
                actual =
                    secondContent,
            )

            assertTrue(
                firstContent.endsWith(
                    suffix =
                        "\n",
                ),
            )

            assertTrue(
                firstContent.contains(
                    other =
                        "\"classification\": \"HARMFUL\"",
                ),
            )

            assertTrue(
                firstContent.contains(
                    other =
                        "\"deltaFalsePositiveCount\": 6",
                ),
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}