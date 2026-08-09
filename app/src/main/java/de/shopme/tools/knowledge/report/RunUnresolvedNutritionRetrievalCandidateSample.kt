package de.shopme.tools.knowledge.report

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths

object RunUnresolvedNutritionRetrievalCandidateSample {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {
        require(args.isEmpty()) {
            "RunUnresolvedNutritionRetrievalCandidateSample " +
                    "does not accept arguments."
        }

        val paths =
            KnowledgeBuildPaths.default()

        UnresolvedNutritionRetrievalCandidateSampler()
            .run(
                unresolvedFailureFile =
                    paths.reportArtifact(
                        "nutrition.unresolved-retrieval-failures.json"
                    ),
                outputFile =
                    paths.reportArtifact(
                        "nutrition.unresolved-retrieval-candidate-sample.json"
                    )
            )
    }
}