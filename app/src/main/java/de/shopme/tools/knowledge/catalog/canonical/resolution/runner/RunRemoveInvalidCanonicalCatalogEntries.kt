package de.shopme.tools.knowledge.catalog.canonical.resolution.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.resolution.RemoveInvalidCanonicalCatalogEntries

object RunRemoveInvalidCanonicalCatalogEntries {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        )

        RemoveInvalidCanonicalCatalogEntries()
            .remove(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}