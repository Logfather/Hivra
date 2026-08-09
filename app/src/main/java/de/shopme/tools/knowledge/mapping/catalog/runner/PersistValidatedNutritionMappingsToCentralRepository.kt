package de.shopme.tools.knowledge.mapping.catalog.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File

class PersistValidatedNutritionMappingsToCentralRepository(
    private val validatedNutritionMappingFile: File,
    private val centralValidatedMappingFile: File,
    private val existingCentralMappingFile: File,
    private val validatedMappingDirectory: File,
    private val conflictReportFile: File,
    private val mergeReportFile: File,
    private val printLine: (String) -> Unit = ::println
) {

    fun run():
            PersistValidatedNutritionMappingsToCentralRepositoryResult {

        require(validatedNutritionMappingFile.isFile) {
            "Validated nutrition mapping file does not exist: " +
                    validatedNutritionMappingFile.absolutePath
        }

        ensureDirectoryExists(
            directory = validatedMappingDirectory
        )

        require(
            centralValidatedMappingFile.parentFile
                ?.canonicalFile ==
                    validatedMappingDirectory.canonicalFile
        ) {
            "Central validated nutrition mapping file must be located in " +
                    validatedMappingDirectory.absolutePath
        }

        copyValidatedNutritionMappings()

        val mergeReport =
            MergeValidatedCatalogServerKnowledgeMappings(
                existingMappingFile =
                    existingCentralMappingFile,
                validatedMappingDirectory =
                    validatedMappingDirectory,
                outputMappingFile =
                    existingCentralMappingFile,
                conflictReportFile =
                    conflictReportFile,
                mergeReportFile =
                    mergeReportFile,
                printLine =
                    printLine
            ).run()

        val result =
            PersistValidatedNutritionMappingsToCentralRepositoryResult(
                validatedNutritionMappingFile =
                    validatedNutritionMappingFile.path,
                centralValidatedMappingFile =
                    centralValidatedMappingFile.path,
                centralMappingFile =
                    existingCentralMappingFile.path,
                addedMappingCount =
                    mergeReport.addedMappingCount,
                unchangedMappingCount =
                    mergeReport.unchangedMappingCount,
                conflictCount =
                    mergeReport.conflictCount,
                totalMappingCount =
                    mergeReport.totalMappingCount
            )

        printSummary(
            result = result
        )

        return result
    }


    private fun copyValidatedNutritionMappings() {

        val sourceContent =
            validatedNutritionMappingFile
                .readText()
                .trim()

        require(sourceContent.isNotBlank()) {
            "Validated nutrition mapping file must not be blank"
        }

        val targetParent =
            requireNotNull(
                centralValidatedMappingFile.parentFile
            ) {
                "Central validated mapping file has no parent directory"
            }

        ensureDirectoryExists(
            directory = targetParent
        )

        val temporaryFile =
            File(
                targetParent,
                "${centralValidatedMappingFile.name}.tmp"
            )

        temporaryFile.writeText(
            sourceContent + System.lineSeparator()
        )

        if (
            centralValidatedMappingFile.exists() &&
            !centralValidatedMappingFile.delete()
        ) {
            error(
                "Could not replace existing validated nutrition mapping file: " +
                        centralValidatedMappingFile.absolutePath
            )
        }

        check(
            temporaryFile.renameTo(
                centralValidatedMappingFile
            )
        ) {
            "Could not move validated nutrition mapping file to: " +
                    centralValidatedMappingFile.absolutePath
        }
    }


    private fun ensureDirectoryExists(
        directory: File
    ) {

        if (!directory.exists()) {
            check(directory.mkdirs()) {
                "Could not create directory: " +
                        directory.absolutePath
            }
        }

        require(directory.isDirectory) {
            "Path is not a directory: " +
                    directory.absolutePath
        }
    }


    private fun printSummary(
        result:
        PersistValidatedNutritionMappingsToCentralRepositoryResult
    ) {

        printLine("")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("CENTRAL NUTRITION MAPPING REPOSITORY")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine(
            "Validated source : " +
                    result.validatedNutritionMappingFile
        )
        printLine(
            "Validated target : " +
                    result.centralValidatedMappingFile
        )
        printLine(
            "Central mapping  : " +
                    result.centralMappingFile
        )
        printLine(
            "Added            : " +
                    result.addedMappingCount
        )
        printLine(
            "Unchanged        : " +
                    result.unchangedMappingCount
        )
        printLine(
            "Conflicts        : " +
                    result.conflictCount
        )
        printLine(
            "Total mappings   : " +
                    result.totalMappingCount
        )
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }


    companion object {

        @JvmStatic
        fun main(
            args: Array<String>
        ) {

            require(args.isEmpty()) {
                "PersistValidatedNutritionMappingsToCentralRepository " +
                        "does not accept arguments."
            }

            val paths =
                KnowledgeBuildPaths.default()

            paths.ensureBuildDirectories()

            PersistValidatedNutritionMappingsToCentralRepository(
                validatedNutritionMappingFile =
                    paths.catalogServerMappings,

                centralValidatedMappingFile =
                    paths.validatedMappingsDirectory.resolve(
                        "nutrition.validated-mappings.json"
                    ),

                existingCentralMappingFile =
                    paths.catalogServerMappings,

                validatedMappingDirectory =
                    paths.validatedMappingsDirectory,

                conflictReportFile =
                    paths.catalogServerMappingConflictReport,

                mergeReportFile =
                    paths.catalogServerMappingMergeReport
            ).run()
        }
    }
}


data class PersistValidatedNutritionMappingsToCentralRepositoryResult(
    val validatedNutritionMappingFile: String,
    val centralValidatedMappingFile: String,
    val centralMappingFile: String,
    val addedMappingCount: Int,
    val unchangedMappingCount: Int,
    val conflictCount: Int,
    val totalMappingCount: Int
)