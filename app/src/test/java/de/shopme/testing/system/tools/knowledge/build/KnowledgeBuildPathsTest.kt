package de.shopme.testing.system.tools.knowledge.build

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnowledgeBuildPathsTest {

    private val paths =
        KnowledgeBuildPaths.default()

    private val projectRoot =
        paths.projectRoot

    @Test
    fun canonicalCatalogUsesSingleProductiveAuthority() {

        assertEquals(
            projectRoot
                .resolve(
                    "data/knowledge/catalog/master/product-only/" +
                            "canonical-food-catalog.product-only.master.json"
                )
                .canonicalFile,
            paths.canonicalFoodCatalog.canonicalFile
        )

        assertTrue(
            paths.canonicalFoodCatalog.isFile
        )

        assertEquals(
            "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            KnowledgeBuildPaths.CANONICAL_CATALOG_SHA256
        )

        assertEquals(
            1384,
            KnowledgeBuildPaths.CANONICAL_CATALOG_ENTRY_COUNT
        )
    }

    @Test
    fun persistentInputsLiveBelowData() {

        val dataRoot =
            projectRoot
                .resolve("data")
                .canonicalFile

        listOf(
            paths.catalogRoot,
            paths.knowledgeRoot,
            paths.canonicalCatalogMasterRoot,
            paths.sourcesRoot,
            paths.referencesRoot,
            paths.frozenRoot,
            paths.policiesRoot,
            paths.modelsRoot,
            paths.trainingRoot
        ).forEach { path ->

            assertTrue(
                "$path must live below data/",
                path.canonicalPath.startsWith(
                    dataRoot.canonicalPath +
                            File.separator
                )
            )
        }
    }

    @Test
    fun reproducibleOutputsLiveBelowBuildKnowledge() {

        val buildRoot =
            projectRoot
                .resolve("build/knowledge")
                .canonicalFile

        assertEquals(
            buildRoot,
            paths.knowledgeBuildRoot.canonicalFile
        )

        listOf(
            paths.diagnosticsRoot,
            paths.intermediateRoot,
            paths.mappingsRoot,
            paths.reportsRoot,
            paths.runtimeRoot,
            paths.serverRoot
        ).forEach { path ->

            assertTrue(
                "$path must live below build/knowledge/",
                path.canonicalPath.startsWith(
                    buildRoot.canonicalPath +
                            File.separator
                )
            )
        }
    }

    @Test
    fun sourceDirectoriesUseNewPersistentArchitecture() {

        assertEquals(
            projectRoot
                .resolve("data/sources/agribalyse")
                .canonicalFile,
            paths.agribalyseSourceRoot.canonicalFile
        )

        assertEquals(
            projectRoot
                .resolve("data/sources/ciqual/Ciqual")
                .canonicalFile,
            paths.ciqualSourceRoot.canonicalFile
        )

        assertEquals(
            projectRoot
                .resolve("data/sources/fdc")
                .canonicalFile,
            paths.foodDataCentralSourceRoot.canonicalFile
        )

        assertEquals(
            projectRoot
                .resolve("data/sources/openfoodfacts")
                .canonicalFile,
            paths.openFoodFactsSourceRoot.canonicalFile
        )
    }

    @Test
    fun buildOutputDirectoriesCanBeCreatedDeterministically() {

        paths.ensureBuildDirectories()

        listOf(
            paths.diagnosticsRoot,
            paths.intermediateRoot,
            paths.mappingsRoot,
            paths.reportsRoot,
            paths.runtimeRoot,
            paths.serverRoot
        ).forEach { directory ->
            assertTrue(
                "${directory.absolutePath} must exist",
                directory.isDirectory
            )
        }
    }
}