package de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.GenerateHierarchicalCanonicalGermanFoodTaxonomy

object RunGenerateHierarchicalCanonicalGermanFoodTaxonomy {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        )

        GenerateHierarchicalCanonicalGermanFoodTaxonomy()
            .generate(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}