package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineFreezer
import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineWriter
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalFoodCatalogBaselineFreezeTest {

    private val projectDirectory =
        resolveProjectDirectory()

    private val inputCatalogFile =
        KnowledgeBuildPaths
            .default()
            .canonicalFoodCatalog

    private val auditOutputDirectory =
        File(
            projectDirectory,
            "data/generated/knowledge/catalog/audit"
        )

    private val normalizedOutputDirectory =
        File(
            projectDirectory,
            "data/generated/knowledge/catalog/normalized"
        )

    private val baselineOutputFile =
        File(
            projectDirectory,
            "data/generated/knowledge/catalog/baseline/" +
                    "canonical-food-catalog-baseline.json"
        )

    @Test
    fun freezeCanonicalFoodCatalogBaseline() {
        val pipeline =
            CatalogNormalizationPipelineFactory.create()

        val pipelineResult =
            pipeline.run(
                catalogFile =
                    inputCatalogFile,

                auditOutputDirectory =
                    auditOutputDirectory,

                normalizedOutputDirectory =
                    normalizedOutputDirectory
            )

        assertTrue(
            pipelineResult.valid,
            "Canonical catalog normalization pipeline is invalid."
        )

        val baseline =
            CanonicalFoodCatalogBaselineFreezer()
                .freeze(
                    projectDirectory =
                        projectDirectory,

                    normalizedCatalogFile =
                        pipelineResult.normalizedCatalogFile,

                    pipelineResult =
                        pipelineResult
                )

        CanonicalFoodCatalogBaselineWriter()
            .write(
                baseline =
                    baseline,
                outputFile =
                    baselineOutputFile
            )

        val persistedBaseline =
            CanonicalFoodCatalogBaselineReader()
                .read(
                    inputFile =
                        baselineOutputFile
                )

        assertEquals(
            baseline,
            persistedBaseline,
            "Persisted baseline differs from generated baseline."
        )

        assertTrue(persistedBaseline.valid)

        assertEquals(
            expected = 3649,
            actual =
                persistedBaseline
                    .applicationInputEntryCount
        )

        assertEquals(
            expected = 3431,
            actual =
                persistedBaseline
                    .applicationOutputEntryCount
        )

        assertEquals(
            expected = 218,
            actual =
                persistedBaseline
                    .duplicateMergedEntryCount
        )

        assertEquals(
            expected = 12,
            actual =
                persistedBaseline
                    .semanticTypoRemovedEntryCount
        )

        assertEquals(
            expected = 3419,
            actual =
                persistedBaseline
                    .finalOutputEntryCount
        )

        assertEquals(
            expected = 3419,
            actual =
                persistedBaseline
                    .normalizedCatalogEntryCount
        )

        assertEquals(
            expected = 0,
            actual =
                persistedBaseline
                    .validationIssueCount
        )

        assertTrue(
            persistedBaseline.baselineId.startsWith(
                "canonical-food-catalog-v1-"
            )
        )

        println(
            buildString {
                appendLine(
                    "Canonical food catalog baseline"
                )
                appendLine(
                    "-------------------------------"
                )
                appendLine(
                    "Baseline ID: " +
                            persistedBaseline.baselineId
                )
                appendLine(
                    "Catalog: " +
                            persistedBaseline
                                .catalogArtifact
                                .relativePath
                )
                appendLine(
                    "Catalog SHA-256: " +
                            persistedBaseline
                                .catalogArtifact
                                .sha256
                )
                appendLine(
                    "Final entries: " +
                            persistedBaseline
                                .finalOutputEntryCount
                )
                appendLine(
                    "Canonical categories: " +
                            persistedBaseline
                                .canonicalCategoryCount
                )
                appendLine(
                    "Merged duplicates: " +
                            persistedBaseline
                                .duplicateMergedEntryCount
                )
                appendLine(
                    "Removed semantic typo entries: " +
                            persistedBaseline
                                .semanticTypoRemovedEntryCount
                )
                append(
                    "Baseline valid: " +
                            persistedBaseline.valid
                )
            }
        )
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(System.getProperty("user.dir"))
                .canonicalFile

        val paths =
            KnowledgeBuildPaths.fromProjectRoot(
                workingDirectory
            )

        return when {

            paths.canonicalFoodCatalog.isFile ->
                workingDirectory

            workingDirectory.name == "app" -> {

                val projectRoot =
                    workingDirectory.parentFile

                val parentPaths =
                    KnowledgeBuildPaths.fromProjectRoot(
                        projectRoot
                    )

                if (
                    parentPaths.canonicalFoodCatalog.isFile
                ) {
                    projectRoot
                } else {
                    error(
                        "Could not resolve ShopMe project directory from: " +
                                workingDirectory.absolutePath
                    )
                }
            }

            else ->
                error(
                    "Could not resolve ShopMe project directory from: " +
                            workingDirectory.absolutePath
                )
        }
    }
}