package de.shopme.tools.knowledge.catalog.canonical.resolution.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.validation.ValidateBaseResolvedCanonicalFoodCatalog

object RunValidateFinalCandidateCanonicalFoodCatalog {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        )

        val paths =
            KnowledgeBuildPaths.default()

        ValidateBaseResolvedCanonicalFoodCatalog()
            .validate(
                paths =
                    paths,

                inputFile =
                    paths.projectRoot.resolve(
                        "build/knowledge/catalog/final-candidate/" +
                                "canonical-food-catalog.final-candidate.json"
                    ),

                reportFile =
                    paths.reportsRoot.resolve(
                        "final-candidate-canonical-food-validation.json"
                    )
            )
    }
}