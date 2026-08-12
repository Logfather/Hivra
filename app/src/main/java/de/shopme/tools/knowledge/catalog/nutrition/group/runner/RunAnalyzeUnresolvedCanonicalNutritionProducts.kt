package de.shopme.tools.knowledge.catalog.nutrition.group.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.nutrition.group.AnalyzeUnresolvedCanonicalNutritionProducts

object RunAnalyzeUnresolvedCanonicalNutritionProducts {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        ) {
            "RunAnalyzeUnresolvedCanonicalNutritionProducts " +
                    "does not accept arguments."
        }

        AnalyzeUnresolvedCanonicalNutritionProducts()
            .analyze(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}