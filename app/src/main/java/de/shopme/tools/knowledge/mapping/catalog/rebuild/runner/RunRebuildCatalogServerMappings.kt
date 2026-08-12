package de.shopme.tools.knowledge.mapping.catalog.rebuild.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.rebuild.RebuildCatalogServerMappings

object RunRebuildCatalogServerMappings {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        ) {
            "RunRebuildCatalogServerMappings " +
                    "does not accept arguments."
        }

        RebuildCatalogServerMappings()
            .rebuild(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}