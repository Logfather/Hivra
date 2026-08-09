package de.shopme.tools.knowledge.report

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths

object RunRejectedNutritionCandidateQualityReport {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {
        require(args.isEmpty()) {
            "RunRejectedNutritionCandidateQualityReport " +
                    "does not accept arguments."
        }

        val paths =
            KnowledgeBuildPaths.default()

        RejectedNutritionCandidateQualityReporter()
            .run(
                requestFile =
                    paths.intermediateArtifact(
                        "match-requests/nutrition.match-requests.json"
                    ),
                diagnosticsFile =
                    paths.diagnosticArtifact(
                        "nutrition.match-diagnostics.json"
                    ),
                outputFile =
                    paths.reportArtifact(
                        "nutrition.rejected-candidate-quality.json"
                    )
            )
    }
}