package de.shopme.tools.knowledge.mapping.catalog.nutrition.group.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.nutrition.group.DiscoverCanonicalNutritionSourceVariants

object RunDiscoverCanonicalNutritionSourceVariants {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        ) {
            "RunDiscoverCanonicalNutritionSourceVariants " +
                    "does not accept arguments."
        }

        DiscoverCanonicalNutritionSourceVariants()
            .discover(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}