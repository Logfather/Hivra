package de.shopme.presentation.developer.foodintelligence.nutrition

import android.content.Context
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report.NutritionKnowledgeQualityReport
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report.NutritionKnowledgeQualityReportFiles
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report.NutritionKnowledgeQualityReportReader
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report.PackagedNutritionKnowledgeQualityReportAssets
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class PackagedNutritionKnowledgeQualityReportLoader(
    context: Context,
    private val reportReader:
    NutritionKnowledgeQualityReportReader =
        NutritionKnowledgeQualityReportReader()
) {

    private val applicationContext =
        context.applicationContext

    fun load(): NutritionKnowledgeQualityReport {

        val materializedFiles =
            materializePackagedReports()

        return reportReader.read(
            materializedFiles
        )
    }

    private fun materializePackagedReports():
            NutritionKnowledgeQualityReportFiles {

        val outputDirectory =
            applicationContext.cacheDir.resolve(
                CACHE_DIRECTORY
            )

        require(
            outputDirectory.mkdirs() ||
                    outputDirectory.isDirectory
        ) {
            "Could not create packaged Nutrition quality report cache " +
                    "directory: ${outputDirectory.absolutePath}"
        }

        return NutritionKnowledgeQualityReportFiles(
            validationReportFile =
                materializeAsset(
                    assetPath =
                        PackagedNutritionKnowledgeQualityReportAssets
                            .VALIDATION_REPORT,
                    outputDirectory =
                        outputDirectory
                ),
            coverageReportFile =
                materializeAsset(
                    assetPath =
                        PackagedNutritionKnowledgeQualityReportAssets
                            .COVERAGE_REPORT,
                    outputDirectory =
                        outputDirectory
                ),
            coverageGapClassificationReportFile =
                materializeAsset(
                    assetPath =
                        PackagedNutritionKnowledgeQualityReportAssets
                            .COVERAGE_GAP_CLASSIFICATION_REPORT,
                    outputDirectory =
                        outputDirectory
                ),
            conflictReportFile =
                materializeAsset(
                    assetPath =
                        PackagedNutritionKnowledgeQualityReportAssets
                            .CONFLICT_REPORT,
                    outputDirectory =
                        outputDirectory
                ),
            conflictPolicyReportFile =
                materializeAsset(
                    assetPath =
                        PackagedNutritionKnowledgeQualityReportAssets
                            .CONFLICT_POLICY_REPORT,
                    outputDirectory =
                        outputDirectory
                ),
            offNutritionSourceSnapshotFile =
                materializeAsset(
                    assetPath =
                        PackagedNutritionKnowledgeQualityReportAssets
                            .OFF_SOURCE_SNAPSHOT,
                    outputDirectory =
                        outputDirectory
                )
        )
    }

    private fun materializeAsset(
        assetPath: String,
        outputDirectory: File
    ): File {

        val outputFile =
            outputDirectory.resolve(
                assetPath.substringAfterLast(
                    '/'
                )
            )

        val temporaryFile =
            outputFile.resolveSibling(
                outputFile.name + ".tmp"
            )

        runCatching {
            applicationContext.assets
                .open(
                    assetPath
                )
                .use { input ->
                    temporaryFile
                        .outputStream()
                        .buffered()
                        .use { output ->
                            input.copyTo(
                                output
                            )
                        }
                }
        }
            .getOrElse { throwable ->
                temporaryFile.delete()

                if (
                    throwable is
                            FileNotFoundException
                ) {
                    throw IllegalStateException(
                        "Packaged Nutrition quality report asset does not " +
                                "exist: $assetPath",
                        throwable
                    )
                }

                throw IllegalStateException(
                    "Could not read packaged Nutrition quality report " +
                            "asset: $assetPath",
                    throwable
                )
            }

        require(
            temporaryFile.isFile &&
                    temporaryFile.length() > 0L
        ) {
            temporaryFile.delete()

            "Packaged Nutrition quality report asset is empty: $assetPath"
        }

        runCatching {
            Files.move(
                temporaryFile.toPath(),
                outputFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }
            .getOrElse {
                Files.move(
                    temporaryFile.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }

        return outputFile
    }

    companion object {

        private const val CACHE_DIRECTORY =
            "knowledge-quality/nutrition"
    }
}