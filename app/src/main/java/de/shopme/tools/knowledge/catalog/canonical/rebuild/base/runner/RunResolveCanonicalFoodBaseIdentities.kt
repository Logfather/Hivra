package de.shopme.tools.knowledge.catalog.canonical.rebuild.base.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.rebuild.base.ResolveCanonicalFoodBaseIdentities

object RunResolveCanonicalFoodBaseIdentities {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        ) {
            "RunResolveCanonicalFoodBaseIdentities does not accept arguments."
        }

        ResolveCanonicalFoodBaseIdentities()
            .resolve(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}