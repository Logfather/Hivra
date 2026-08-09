package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.report

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report.PackagedNutritionKnowledgeQualityReportAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackagedNutritionKnowledgeQualityReportAssetsTest {

    @Test
    fun all_containsEveryRequiredNutritionQualityReportAsset() {

        assertEquals(
            6,
            PackagedNutritionKnowledgeQualityReportAssets
                .all
                .size
        )

        assertEquals(
            PackagedNutritionKnowledgeQualityReportAssets
                .all
                .size,
            PackagedNutritionKnowledgeQualityReportAssets
                .all
                .distinct()
                .size
        )

        PackagedNutritionKnowledgeQualityReportAssets
            .all
            .forEach { assetPath ->
                assertTrue(
                    assetPath.startsWith(
                        PackagedNutritionKnowledgeQualityReportAssets
                            .DIRECTORY + "/"
                    )
                )

                assertTrue(
                    assetPath.endsWith(
                        ".json"
                    )
                )
            }
    }
}