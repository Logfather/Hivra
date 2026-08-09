package de.shopme.testing.system.tools.knowledge.catalog.baseline

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.runner.CatalogNormalizationPipelineResult
import java.io.File
import java.security.MessageDigest

class CanonicalFoodCatalogBaselineFreezer(
    private val gson: Gson =
        createDefaultGson()
) {

    fun freeze(
        projectDirectory: File,
        normalizedCatalogFile: File,
        pipelineResult:
        CatalogNormalizationPipelineResult
    ): CanonicalFoodCatalogBaseline {
        require(projectDirectory.isDirectory) {
            "Project directory does not exist: " +
                    projectDirectory.absolutePath
        }

        require(normalizedCatalogFile.isFile) {
            "Normalized catalog file does not exist: " +
                    normalizedCatalogFile.absolutePath
        }

        require(normalizedCatalogFile.length() > 0L)

        require(pipelineResult.valid) {
            "Catalog normalization pipeline must be valid."
        }

        require(
            pipelineResult.validationResult.valid
        ) {
            "Normalized catalog validation must be valid."
        }

        val catalogItems =
            readCatalog(normalizedCatalogFile)

        val finalOutputEntryCount =
            pipelineResult.semanticTypoRemovalResult
                .outputEntryCount

        require(
            catalogItems.size ==
                    finalOutputEntryCount
        ) {
            "Persisted normalized catalog contains " +
                    "${catalogItems.size} entries, but semantic typo removal " +
                    "produced $finalOutputEntryCount."
        }

        val categoryCounts =
            catalogItems
                .groupingBy { item ->
                    requireNotNull(
                        item.category
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                    ) {
                        "Baseline catalog contains an item without category: " +
                                item.itemname
                    }
                }
                .eachCount()
                .toSortedMap()

        val catalogArtifact =
            artifactReference(
                projectDirectory =
                    projectDirectory,
                file =
                    normalizedCatalogFile
            )

        val supportingArtifacts =
            linkedMapOf(
                "reviewBacklogEvaluation" to
                        pipelineResult.reviewBacklogReportFile,

                "reviewBacklogClassification" to
                        pipelineResult
                            .reviewBacklogClassificationReportFile,

                "reviewBacklogAnalysis" to
                        pipelineResult
                            .reviewBacklogAnalysisReportFile,

                "updatedReviewAnalysis" to
                        pipelineResult
                            .updatedReviewAnalysisReportFile,

                "remainingTypoDiagnostics" to
                        pipelineResult
                            .remainingTypoDiagnosticsReportFile,

                "misclassifiedTypoReclassification" to
                        pipelineResult
                            .misclassifiedTypoReclassificationReportFile,

                "semanticTypoRemovals" to
                        pipelineResult
                            .semanticTypoRemovalReportFile
            )
                .mapValues { (_, file) ->
                    artifactReference(
                        projectDirectory =
                            projectDirectory,
                        file =
                            file
                    )
                }
                .toSortedMap()

        val baselineId =
            buildString {
                append("canonical-food-catalog-v")
                append(
                    CanonicalFoodCatalogBaseline
                        .CURRENT_VERSION
                )
                append('-')
                append(
                    catalogArtifact.sha256
                        .take(BASELINE_HASH_PREFIX_LENGTH)
                )
            }

        return CanonicalFoodCatalogBaseline(
            version =
                CanonicalFoodCatalogBaseline
                    .CURRENT_VERSION,

            baselineId =
                baselineId,

            catalogArtifact =
                catalogArtifact,

            normalizedCatalogEntryCount =
                catalogItems.size,

            canonicalCategoryCount =
                categoryCounts.size,

            categoryCounts =
                categoryCounts,

            applicationInputEntryCount =
                pipelineResult.applicationResult
                    .inputEntryCount,

            applicationOutputEntryCount =
                pipelineResult.applicationResult
                    .outputEntryCount,

            duplicateMergedEntryCount =
                pipelineResult.applicationResult
                    .mergedEntryCount,

            semanticTypoRemovedEntryCount =
                pipelineResult.semanticTypoRemovalResult
                    .removedEntryCount,

            finalOutputEntryCount =
                finalOutputEntryCount,

            validationIssueCount =
                pipelineResult.validationResult
                    .issueCount,

            normalizedCatalogValid =
                pipelineResult.validationResult
                    .valid,

            pipelineValid =
                pipelineResult.valid,

            supportingArtifacts =
                supportingArtifacts,

            valid = true
        )
    }

    private fun readCatalog(
        catalogFile: File
    ): List<CatalogFoodItem> {
        val parsed =
            gson.fromJson(
                catalogFile.reader(Charsets.UTF_8),
                Array<CatalogFoodItem>::class.java
            )

        requireNotNull(parsed) {
            "Could not parse normalized catalog: " +
                    catalogFile.absolutePath
        }

        return parsed.toList()
    }

    private fun artifactReference(
        projectDirectory: File,
        file: File
    ): CatalogBaselineArtifactReference {
        require(file.isFile) {
            "Baseline artifact does not exist: " +
                    file.absolutePath
        }

        require(file.length() > 0L) {
            "Baseline artifact is empty: " +
                    file.absolutePath
        }

        val relativePath =
            projectDirectory
                .canonicalFile
                .toPath()
                .relativize(
                    file.canonicalFile.toPath()
                )
                .toString()
                .replace(
                    File.separatorChar,
                    '/'
                )

        require(!relativePath.startsWith("../")) {
            "Baseline artifact must be located inside the project: " +
                    file.absolutePath
        }

        return CatalogBaselineArtifactReference(
            relativePath =
                relativePath,

            byteCount =
                file.length(),

            sha256 =
                sha256(file)
        )
    }

    private fun sha256(
        file: File
    ): String {
        val digest =
            MessageDigest.getInstance("SHA-256")

        file.inputStream()
            .buffered()
            .use { input ->
                val buffer =
                    ByteArray(BUFFER_SIZE)

                while (true) {
                    val read =
                        input.read(buffer)

                    if (read < 0) {
                        break
                    }

                    if (read > 0) {
                        digest.update(
                            buffer,
                            0,
                            read
                        )
                    }
                }
            }

        return digest.digest()
            .joinToString(separator = "") { byte ->
                "%02x".format(
                    byte.toInt() and 0xff
                )
            }
    }

    private companion object {
        const val BASELINE_HASH_PREFIX_LENGTH = 16
        const val BUFFER_SIZE = 16 * 1024

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create()
    }
}