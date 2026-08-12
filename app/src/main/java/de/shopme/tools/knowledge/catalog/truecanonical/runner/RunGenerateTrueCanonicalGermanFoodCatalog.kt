package de.shopme.tools.knowledge.catalog.truecanonical.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.truecanonical.GenerateTrueCanonicalGermanFoodCatalog

object RunGenerateTrueCanonicalGermanFoodCatalog {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        )

        GenerateTrueCanonicalGermanFoodCatalog()
            .generate(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}