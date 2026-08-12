package de.shopme.tools.knowledge.catalog.canonical.rebuild.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.rebuild.RebuildCanonicalFoodCatalog

object RunRebuildCanonicalFoodCatalog {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        )

        val paths =
            KnowledgeBuildPaths.default()

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/rebuild"
            )

        RebuildCanonicalFoodCatalog()
            .rebuild(
                inputFile =
                    paths.canonicalFoodCatalog,

                outputFile =
                    outputDirectory.resolve(
                        "canonical-food-catalog.vnext.json"
                    ),

                decisionsFile =
                    outputDirectory.resolve(
                        "canonical-food-catalog.rebuild-decisions.json"
                    ),

                reportFile =
                    paths.reportsRoot.resolve(
                        "canonical-food-catalog-rebuild-report.json"
                    )
            )
    }
}