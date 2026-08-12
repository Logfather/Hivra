package de.shopme.tools.knowledge.rebuild.dimensions.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.rebuild.dimensions.RebuildKnowledgeDimensionsAgainstCanonicalFoodCatalog

object RunRebuildKnowledgeDimensionsAgainstCanonicalFoodCatalog {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(args.isEmpty()) {
            "RunRebuildKnowledgeDimensionsAgainstCanonicalFoodCatalog " +
                    "does not accept arguments."
        }

        val paths =
            KnowledgeBuildPaths.default()

        val projectRoot =
            paths.projectRoot

        val offFile =
            projectRoot.resolve(
                "data/sources/openfoodfacts/" +
                        "openfoodfacts-products.jsonl.gz"
            )

        val offNutritionAggregateFile =
            projectRoot.resolve(
                "data/references/off/" +
                        "off-nutrition-reference-aggregates.json"
            )

        val agribalyseSourceFile =
            projectRoot.resolve(
                "data/sources/agribalyse/" +
                        "AGRIBALYSE3.2_Tableur produits alimentaires_" +
                        "PublieAOUT25.xlsx"
            )

        val ciqualDirectory =
            projectRoot.resolve(
                "data/sources/ciqual/Ciqual"
            )

        RebuildKnowledgeDimensionsAgainstCanonicalFoodCatalog()
            .rebuild(
                paths =
                    paths,
                offFile =
                    offFile,
                offNutritionAggregateFile =
                    offNutritionAggregateFile,
                agribalyseSourceFile =
                    agribalyseSourceFile,
                ciqualDirectory =
                    ciqualDirectory,
                maxOffCandidates =
                    null,
                maxOffNutritionAggregates =
                    null
            )
    }
}