package de.shopme.tools.knowledge.mapping.catalog.training

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths

object RunRepresentativeNutritionMappingTrainingExampleExport {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {
        require(args.isEmpty()) {
            "RunRepresentativeNutritionMappingTrainingExampleExport " +
                    "does not accept arguments."
        }

        val paths =
            KnowledgeBuildPaths.default()

        RepresentativeNutritionMappingTrainingExampleExporter()
            .run(
                validationFile =
                    paths.nutritionLowConfidenceValidationReport,
                outputFile =
                    paths.nutritionRepresentativeTrainingExamples
            )
    }
}