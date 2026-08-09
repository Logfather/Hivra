package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File

data class NutritionKnowledgeQualityReportFiles(
    val validationReportFile: File,
    val coverageReportFile: File,
    val coverageGapClassificationReportFile: File,
    val conflictReportFile: File,
    val conflictPolicyReportFile: File,
    val offNutritionSourceSnapshotFile: File
) {

    init {
        requireDistinctFiles()
    }

    private fun requireDistinctFiles() {
        val files =
            listOf(
                validationReportFile,
                coverageReportFile,
                coverageGapClassificationReportFile,
                conflictReportFile,
                conflictPolicyReportFile,
                offNutritionSourceSnapshotFile
            )

        val canonicalPaths =
            files.map { file ->
                file.canonicalFile.path
            }

        require(
            canonicalPaths.size ==
                    canonicalPaths.distinct().size
        ) {
            "Nutrition Knowledge quality report files must be distinct."
        }
    }

    companion object {

        fun productive(
            projectDirectory: File
        ): NutritionKnowledgeQualityReportFiles {

            require(projectDirectory.isDirectory) {
                "Project directory does not exist: " +
                        projectDirectory.absolutePath
            }

            val paths =
                KnowledgeBuildPaths.fromProjectRoot(
                    projectDirectory
                )

            paths.ensureBuildDirectories()

            return NutritionKnowledgeQualityReportFiles(
                validationReportFile =
                    paths.reportArtifact(
                        "resulting-nutrition-knowledge-validation.json"
                    ),
                coverageReportFile =
                    paths.reportArtifact(
                        "resulting-nutrition-coverage.json"
                    ),
                coverageGapClassificationReportFile =
                    paths.reportArtifact(
                        "resulting-nutrition-coverage-gap-classification.json"
                    ),
                conflictReportFile =
                    paths.reportArtifact(
                        "resulting-nutrition-conflict-rate.json"
                    ),
                conflictPolicyReportFile =
                    paths.resultingNutritionConflictPolicy,
                offNutritionSourceSnapshotFile =
                    paths.frozenOffNutritionSourceSnapshot
            )
        }
    }
}