package de.shopme.tools.knowledge.mapping.catalog.nutrition.group.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.nutrition.group.ResolveHighConfidenceNutritionSourceVariantReviews

object RunResolveHighConfidenceNutritionSourceVariantReviews {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        ) {
            "RunResolveHighConfidenceNutritionSourceVariantReviews " +
                    "does not accept arguments."
        }

        ResolveHighConfidenceNutritionSourceVariantReviews()
            .resolve(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}