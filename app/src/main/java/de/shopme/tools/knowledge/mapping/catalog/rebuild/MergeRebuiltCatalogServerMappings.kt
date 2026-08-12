package de.shopme.tools.knowledge.mapping.catalog.rebuild

import de.shopme.tools.knowledge.mapping.catalog.CatalogServerKnowledgeMappingMergeReport
import de.shopme.tools.knowledge.mapping.catalog.runner.MergeValidatedCatalogServerKnowledgeMappings
import java.io.File

class MergeRebuiltCatalogServerMappings {

    fun merge(
        exactMappingFile: File,
        validatedMappingDirectory: File,
        outputMappingFile: File,
        conflictReportFile: File,
        mergeReportFile: File
    ): CatalogServerKnowledgeMappingMergeReport {

        require(
            exactMappingFile.isFile
        ) {
            "Exact mapping file does not exist: " +
                    exactMappingFile.absolutePath
        }

        require(
            validatedMappingDirectory.isDirectory
        ) {
            "Validated mapping directory does not exist: " +
                    validatedMappingDirectory.absolutePath
        }

        return MergeValidatedCatalogServerKnowledgeMappings(
            existingMappingFile =
                exactMappingFile,
            validatedMappingDirectory =
                validatedMappingDirectory,
            outputMappingFile =
                outputMappingFile,
            conflictReportFile =
                conflictReportFile,
            mergeReportFile =
                mergeReportFile
        )
            .run()
    }
}