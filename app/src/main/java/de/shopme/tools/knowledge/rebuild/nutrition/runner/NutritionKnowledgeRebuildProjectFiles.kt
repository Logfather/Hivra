package de.shopme.tools.knowledge.rebuild.nutrition.runner

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

            val generatedKnowledgeDirectory =
                File(
                    projectRoot,
                    "data/generated/knowledge"
                )

            val serverArtifactDirectory =
                File(
                    generatedKnowledgeDirectory,
                    "server"
                )

            val runtimeArtifactDirectory =
                File(
                    generatedKnowledgeDirectory,
                    "runtime"
                )

            return NutritionKnowledgeRebuildProjectFiles(
                projectRoot =
                    projectRoot,
                catalogFile =
                    File(
                        projectRoot,
                        "data/raw/catalog/" +
                                "supermarket_dataset.translated.json"
                    ),
                serverArtifactDirectory =
                    serverArtifactDirectory,
                serverNutritionFile =
                    File(
                        serverArtifactDirectory,
                        "nutrition.json"
                    ),
                runtimeArtifactDirectory =
                    runtimeArtifactDirectory,
                runtimeNutritionFile =
                    File(
                        runtimeArtifactDirectory,
                        "nutrition.json"
                    ),
                requestFile =
                    File(
                        generatedKnowledgeDirectory,
                        "match-requests/" +
                                "nutrition.match-requests.json"
                    ),
                decisionFile =
                    File(
                        generatedKnowledgeDirectory,
                        "match-decisions/" +
                                "nutrition.match-decisions.json"
                    ),
                diagnosticsFile =
                    File(
                        generatedKnowledgeDirectory,
                        "reports/" +
                                "nutrition.match-diagnostics.json"
                    ),
                errorFile =
                    File(
                        generatedKnowledgeDirectory,
                        "match-decisions/" +
                                "nutrition.match-errors.json"
                    ),
                exactMappingFile =
                    File(
                        generatedKnowledgeDirectory,
                        "mappings/" +
                                "nutrition.mappings.json"
                    ),
                outputMappingFile =
                    File(
                        generatedKnowledgeDirectory,
                        "mappings/" +
                                "catalog-server.mappings.json"
                    ),
                validationReportFile =
                    File(
                        generatedKnowledgeDirectory,
                        "reports/" +
                                "nutrition.mapping-validation-report.json"
                    ),
                localModelFile =
                    File(
                        generatedKnowledgeDirectory,
                        "models/" +
                                "nutrition.local-matcher-model.json"
                    ),
                rebuildResultFile =
                    File(
                        generatedKnowledgeDirectory,
                        "reports/" +
                                "nutrition.rebuild-result.json"
                    ),
                representativeValidationFile =
                    File(
                        generatedKnowledgeDirectory,
                        "reports/" +
                                "nutrition.low-confidence-validation.json"
                    ),
                matchReportFile =
                    File(
                        projectRoot,
                        "data/generated/reports/catalog-server-matches/" +
                                "nutrition.matches.json"
                    ),
                exactMatchReportFile =
                    projectRoot.resolve(
                        "data/generated/reports/catalog-server-matches/" +
                                "nutrition.matches.json"
                    ),
            )
        }
    }
}