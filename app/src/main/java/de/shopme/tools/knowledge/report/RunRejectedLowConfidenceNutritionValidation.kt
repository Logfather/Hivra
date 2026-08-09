package de.shopme.tools.knowledge.report

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths

object RunRejectedLowConfidenceNutritionValidation {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {
        require(args.isEmpty()) {
            "RunRejectedLowConfidenceNutritionValidation " +
                    "does not accept arguments."
        }

        val paths =
            KnowledgeBuildPaths.default()

        RejectedLowConfidenceNutritionMappingValidator()
            .run(
                candidateQualityFile =
                    paths.reportArtifact(
                        "nutrition.rejected-candidate-quality.json"
                    ),
                diagnosticsFile =
                    paths.diagnosticArtifact(
                        "nutrition.match-diagnostics.json"
                    ),
                outputFile =
                    paths.reportArtifact(
                        "nutrition.low-confidence-validation.json"
                    )
            )
    }
}