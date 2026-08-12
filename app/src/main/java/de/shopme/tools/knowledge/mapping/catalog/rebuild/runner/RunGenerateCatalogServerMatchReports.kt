package de.shopme.tools.knowledge.mapping.catalog.rebuild.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.rebuild.GenerateCatalogServerMatchReports

object RunGenerateCatalogServerMatchReports {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        ) {
            "RunGenerateCatalogServerMatchReports " +
                    "does not accept arguments."
        }

        GenerateCatalogServerMatchReports()
            .generate(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}