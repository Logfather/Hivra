package de.shopme.tools.knowledge.catalog.canonical.resolution.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.resolution.ResolveRemainingCanonicalCatalogSemanticFailures

object RunResolveRemainingCanonicalCatalogSemanticFailures {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        )

        ResolveRemainingCanonicalCatalogSemanticFailures()
            .resolve(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}