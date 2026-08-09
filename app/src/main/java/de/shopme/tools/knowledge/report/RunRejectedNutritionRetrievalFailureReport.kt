package de.shopme.tools.knowledge.report

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths

object RunRejectedNutritionRetrievalFailureReport {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {
        require(args.isEmpty()) {
            "RunRejectedNutritionRetrievalFailureReport " +
                    "does not accept arguments."
        }

        val paths =
            KnowledgeBuildPaths.default()

        RejectedNutritionRetrievalFailureClassifier()
            .run(
                candidateQualityFile =
                    paths.reportArtifact(
                        "nutrition.rejected-candidate-quality.json"
                    ),
                outputFile =
                    paths.reportArtifact(
                        "nutrition.retrieval-failures.json"
                    )
            )
    }
}