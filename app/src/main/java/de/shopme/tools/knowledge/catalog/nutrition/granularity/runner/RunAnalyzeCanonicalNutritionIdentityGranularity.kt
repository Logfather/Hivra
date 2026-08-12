package de.shopme.tools.knowledge.catalog.nutrition.granularity.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.nutrition.granularity.AnalyzeCanonicalNutritionIdentityGranularity

object RunAnalyzeCanonicalNutritionIdentityGranularity {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        ) {
            "RunAnalyzeCanonicalNutritionIdentityGranularity " +
                    "does not accept arguments."
        }

        AnalyzeCanonicalNutritionIdentityGranularity()
            .analyze(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}