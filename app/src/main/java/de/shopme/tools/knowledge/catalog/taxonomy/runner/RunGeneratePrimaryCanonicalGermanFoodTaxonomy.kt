package de.shopme.tools.knowledge.catalog.taxonomy.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.taxonomy.GeneratePrimaryCanonicalGermanFoodTaxonomy

object RunGeneratePrimaryCanonicalGermanFoodTaxonomy {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        )

        GeneratePrimaryCanonicalGermanFoodTaxonomy()
            .generate(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}