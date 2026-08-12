package de.shopme.tools.knowledge.catalog.canonical.validation.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.validation.ValidateBaseResolvedCanonicalFoodCatalog

object RunValidateBaseResolvedCanonicalFoodCatalog {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(args.isEmpty())

        ValidateBaseResolvedCanonicalFoodCatalog()
            .validate(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}