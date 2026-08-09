package de.shopme.tools.knowledge.report

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths

object RunUnresolvedNutritionRetrievalFailureReport {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {
        require(args.isEmpty()) {
            "RunUnresolvedNutritionRetrievalFailureReport " +
                    "does not accept arguments."
        }

        val paths =
            KnowledgeBuildPaths.default()

        UnresolvedNutritionRetrievalFailureReporter()
            .run(
                retrievalFailureFile =
                    paths.reportArtifact(
                        "nutrition.retrieval-failures.json"
                    ),
                outputFile =
                    paths.reportArtifact(
                        "nutrition.unresolved-retrieval-failures.json"
                    )
            )
    }
}