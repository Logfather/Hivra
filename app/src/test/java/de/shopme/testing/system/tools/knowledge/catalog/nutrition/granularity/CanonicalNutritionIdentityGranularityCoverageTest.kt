package de.shopme.testing.system.tools.knowledge.catalog.nutrition.granularity

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.nutrition.granularity.AnalyzeCanonicalNutritionIdentityGranularity
import kotlin.test.Test
import kotlin.test.assertTrue

class CanonicalNutritionIdentityGranularityCoverageTest {

    @Test
    fun projectedResolutionCoverageMustReachNinetyPercent() {

        val report =
            AnalyzeCanonicalNutritionIdentityGranularity()
                .analyze(
                    paths =
                        KnowledgeBuildPaths.default()
                )

        assertTrue(
            actual =
                report.projectedResolvableCoverage >=
                        0.90,
            message =
                "Projected unresolved Nutrition coverage must be >= 90%. " +
                        "Current: %.2f%% (%d/%d)"
                            .format(
                                report.projectedResolvableCoverage *
                                        100.0,
                                report.projectedResolvableCount,
                                report.unresolvedProductCount
                            )
        )
    }
}