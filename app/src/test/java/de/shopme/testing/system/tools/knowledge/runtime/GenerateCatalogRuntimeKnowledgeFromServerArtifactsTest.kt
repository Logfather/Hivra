package de.shopme.testing.system.tools.knowledge.runtime

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.runtime.CatalogRuntimeKnowledgeGenerator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateCatalogRuntimeKnowledgeFromServerArtifactsTest {

    @Test
    fun generateCatalogRuntimeKnowledgeFromServerArtifacts() {

        val paths =
            KnowledgeBuildPaths.default()

        paths.ensureBuildDirectories()

        val catalogFile =
            paths.canonicalFoodCatalog

        val serverDirectory =
            paths.serverRoot

        val runtimeDirectory =
            paths.runtimeRoot

        val mappingFile =
            paths.catalogServerMappings

        assertTrue(
            catalogFile.isFile,
            "Catalog file missing: ${catalogFile.path}"
        )

        assertTrue(
            serverDirectory.isDirectory,
            "Server knowledge directory missing: " +
                    serverDirectory.path
        )

        assertTrue(
            mappingFile.isFile,
            "Central catalog-server mapping file missing: " +
                    mappingFile.path
        )

        val report =
            CatalogRuntimeKnowledgeGenerator()
                .generate(
                    catalogFile =
                        catalogFile,
                    serverArtifactDirectory =
                        serverDirectory,
                    runtimeArtifactDirectory =
                        runtimeDirectory,
                    catalogServerMappingFile =
                        mappingFile
                )

        assertEquals(
            KnowledgeBuildPaths.CANONICAL_CATALOG_ENTRY_COUNT,
            report.catalogKeyCount,
            "Unexpected number of unique catalog knowledge keys"
        )

        assertTrue(
            report.mappingCount > 0,
            "Expected central catalog-server mappings"
        )

        assertTrue(
            report.artifacts.isNotEmpty(),
            "Expected generated runtime artifacts"
        )

        assertTrue(
            report.artifacts.all { artifact ->
                File(
                    artifact.runtimeFile
                ).isFile
            },
            "Every runtime artifact must be written"
        )

        assertTrue(
            report.artifacts.all { artifact ->
                artifact.runtimeEntryCount ==
                        artifact.exactMatchCount +
                        artifact.mappedMatchCount
            },
            "Runtime entries must equal exact plus mapped matches"
        )

        val nutritionReport =
            report.artifacts
                .single {
                    it.artifact ==
                            "nutrition.json"
                }

        assertTrue(
            nutritionReport.exactMatchCount > 0,
            "Expected exact nutrition matches"
        )

        assertTrue(
            nutritionReport.mappedMatchCount > 0,
            "Expected validated AI mappings to add nutrition entries"
        )

        assertTrue(
            nutritionReport.runtimeEntryCount >
                    nutritionReport.exactMatchCount,
            "Nutrition runtime coverage must exceed exact-only coverage"
        )

        assertEquals(
            nutritionReport.exactMatchCount +
                    nutritionReport.mappedMatchCount,
            nutritionReport.runtimeEntryCount
        )

        assertEquals(
            report.catalogKeyCount -
                    nutritionReport.runtimeEntryCount,
            nutritionReport.missingCount
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CENTRAL MAPPING RUNTIME BUILD SUMMARY")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Catalog keys : ${report.catalogKeyCount}")
        println("Mappings     : ${report.mappingCount}")
        println("Artifacts    : ${report.artifacts.size}")
        println()
        println("Nutrition")
        println(
            "Exact        : " +
                    nutritionReport.exactMatchCount
        )
        println(
            "Mapped       : " +
                    nutritionReport.mappedMatchCount
        )
        println(
            "Runtime      : " +
                    nutritionReport.runtimeEntryCount
        )
        println(
            "Missing      : " +
                    nutritionReport.missingCount
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}