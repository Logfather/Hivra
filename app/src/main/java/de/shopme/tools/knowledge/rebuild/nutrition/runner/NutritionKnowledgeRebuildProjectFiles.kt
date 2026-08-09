package de.shopme.tools.knowledge.rebuild.nutrition.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildFiles
import java.io.File

data class NutritionKnowledgeRebuildProjectFiles(
    val projectRoot: File,
    val catalogFile: File,
    val serverArtifactDirectory: File,
    val serverNutritionFile: File,
    val runtimeArtifactDirectory: File,
    val runtimeNutritionFile: File,
    val matchReportFile: File,
    val exactMatchReportFile: File,
    val requestFile: File,
    val decisionFile: File,
    val diagnosticsFile: File,
    val errorFile: File,
    val exactMappingFile: File,
    val outputMappingFile: File,
    val validationReportFile: File,
    val localModelFile: File,
    val rebuildResultFile: File,
    val representativeValidationFile: File,
) {

    fun toResultFiles():
            NutritionKnowledgeRebuildFiles {

        return NutritionKnowledgeRebuildFiles(
            catalogFile =
                catalogFile.path,
            nutritionArtifactFile =
                runtimeNutritionFile.path,
            requestFile =
                requestFile.path,
            decisionFile =
                decisionFile.path,
            validationFile =
                validationReportFile.path,
            mappingFile =
                outputMappingFile.path,
            resultFile =
                rebuildResultFile.path
        )
    }

    companion object {

        fun fromProjectRoot(
            projectRoot: File
        ): NutritionKnowledgeRebuildProjectFiles {

            val paths =
                KnowledgeBuildPaths.fromProjectRoot(
                    projectRoot
                )

            return NutritionKnowledgeRebuildProjectFiles(
                projectRoot =
                    projectRoot,

                catalogFile =
                    paths.canonicalFoodCatalog,

                serverArtifactDirectory =
                    paths.serverRoot,

                serverNutritionFile =
                    paths.serverArtifact(
                        "nutrition.json"
                    ),

                runtimeArtifactDirectory =
                    paths.runtimeRoot,

                runtimeNutritionFile =
                    paths.runtimeArtifact(
                        "nutrition.json"
                    ),

                requestFile =
                    paths.intermediateArtifact(
                        "match-requests/nutrition.match-requests.json"
                    ),

                decisionFile =
                    paths.intermediateArtifact(
                        "match-decisions/nutrition.match-decisions.json"
                    ),

                diagnosticsFile =
                    paths.reportArtifact(
                        "nutrition.match-diagnostics.json"
                    ),

                errorFile =
                    paths.intermediateArtifact(
                        "match-decisions/nutrition.match-errors.json"
                    ),

                exactMappingFile =
                    paths.mappingArtifact(
                        "nutrition.mappings.json"
                    ),

                outputMappingFile =
                    paths.mappingArtifact(
                        "catalog-server.mappings.json"
                    ),

                validationReportFile =
                    paths.reportArtifact(
                        "nutrition.mapping-validation-report.json"
                    ),

                localModelFile =
                    paths.nutritionLocalMatcherModel,

                rebuildResultFile =
                    paths.reportArtifact(
                        "nutrition.rebuild-result.json"
                    ),

                representativeValidationFile =
                    paths.reportArtifact(
                        "nutrition.low-confidence-validation.json"
                    ),

                matchReportFile =
                    paths.reportArtifact(
                        "catalog-server-matches/nutrition.matches.json"
                    ),

                exactMatchReportFile =
                    paths.reportArtifact(
                        "catalog-server-matches/nutrition.matches.json"
                    ),
            )
        }
    }
}