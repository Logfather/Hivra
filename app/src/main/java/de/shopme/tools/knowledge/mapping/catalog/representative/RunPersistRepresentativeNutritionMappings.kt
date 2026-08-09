package de.shopme.tools.knowledge.mapping.catalog.representative

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths

object RunPersistRepresentativeNutritionMappings {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {
        require(args.isEmpty()) {
            "RunPersistRepresentativeNutritionMappings " +
                    "does not accept arguments."
        }

        val paths =
            KnowledgeBuildPaths.default()

        PersistRepresentativeNutritionMappings()
            .run(
                validationFile =
                    paths.nutritionLowConfidenceValidationReport,
                mappingFile =
                    paths.catalogServerMappings
            )
    }
}