package de.shopme.testing.system.tools.knowledge.runner

import de.shopme.testing.system.tools.knowledge.build.KnowledgeBuildContextFactory
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidateCanonicalKnowledgeBuildCatalogIntegrationTest {

    @Test
    fun validateCanonicalKnowledgeBuildCatalogIntegration() {

        val projectRoot =
            resolveProjectRoot()

        val context =
            KnowledgeBuildContextFactory(
                projectRoot
            )
                .create()

        println()
        println("Canonical Knowledge Build Catalog Integration")
        println("---------------------------------------------")
        println(
            "Catalog source  : " +
                    context
                        .canonicalCatalog
                        .sourceFile
                        .relativeTo(projectRoot)
                        .path
        )
        println(
            "Catalog entries : " +
                    context.catalogEntryCount
        )
        println(
            "Catalog SHA-256 : " +
                    context.catalogSha256
        )

        val paths =
            KnowledgeBuildPaths.fromProjectRoot(
                projectRoot
            )

        assertEquals(
            expected =
                paths.canonicalFoodCatalog
                    .relativeTo(projectRoot)
                    .path,
            actual =
                context
                    .canonicalCatalog
                    .sourceFile
                    .relativeTo(projectRoot)
                    .path
        )

        assertEquals(
            expected = 4596,
            actual =
                context.catalogEntryCount
        )

        assertEquals(
            expected =
                "72d837193a6083df7def41b5084b37673d4a8e126a100e9b968d082a03708dcf",
            actual =
                context.catalogSha256
        )
    }

    private fun resolveProjectRoot(): File =
        KnowledgeBuildPaths
            .default()
            .projectRoot
}