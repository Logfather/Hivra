package de.shopme.tools.knowledge.catalog.canonical.resolution.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.validation.ValidateBaseResolvedCanonicalFoodCatalog

object RunValidateSemanticResolvedCanonicalFoodCatalog {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        ) {
            "RunValidateSemanticResolvedCanonicalFoodCatalog does not accept arguments."
        }

        val paths =
            KnowledgeBuildPaths.default()

        val inputFile =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/semantic-resolved/" +
                        "canonical-food-catalog.semantic-resolved.json"
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "semantic-resolved-canonical-food-validation.json"
            )

        require(
            inputFile.isFile
        ) {
            "Semantic-resolved canonical food catalog not found: " +
                    inputFile.absolutePath
        }

        ValidateBaseResolvedCanonicalFoodCatalog()
            .validate(
                paths =
                    paths,
                inputFile =
                    inputFile,
                reportFile =
                    reportFile
            )
    }
}