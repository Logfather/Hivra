package de.shopme.tools.knowledge.mapping.catalog.nutrition.group.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.nutrition.group.BuildCanonicalNutritionSourceVariantGroups

object RunBuildCanonicalNutritionSourceVariantGroups {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        ) {
            "RunBuildCanonicalNutritionSourceVariantGroups " +
                    "does not accept arguments."
        }

        BuildCanonicalNutritionSourceVariantGroups()
            .build(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}