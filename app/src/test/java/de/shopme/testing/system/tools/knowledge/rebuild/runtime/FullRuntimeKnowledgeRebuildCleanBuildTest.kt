package de.shopme.testing.system.tools.knowledge.rebuild.runtime

import de.shopme.tools.knowledge.build.ActiveFoodKnowledgeScope
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.rebuild.runtime.FullRuntimeKnowledgeRebuild
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FullRuntimeKnowledgeRebuildCleanBuildTest {

    @Test
    fun rebuildDoesNotRequirePreviousRuntimeArtifacts() {

        val sourcePaths =
            KnowledgeBuildPaths.default()

        val temporaryProjectRoot =
            Files.createTempDirectory(
                "shopme-runtime-rebuild-"
            ).toFile()

        try {
            val paths =
                createIsolatedBuildFixture(
                    sourcePaths =
                        sourcePaths,
                    projectRoot =
                        temporaryProjectRoot
                )

            assertFalse(
                "Runtime directory must not exist before the first rebuild.",
                paths.runtimeRoot.exists()
            )

            val firstResult =
                FullRuntimeKnowledgeRebuild()
                    .rebuild(
                        paths =
                            paths,
                        publish =
                            false
                    )

            assertTrue(
                "Runtime directory was not created by the rebuild.",
                paths.runtimeRoot.isDirectory
            )

            assertTrue(
                "Clean rebuild produced no Runtime Knowledge artifacts.",
                firstResult.runtimeArtifactCount > 0
            )

            assertTrue(
                "Unexpected active Server Knowledge artifact count: " +
                        firstResult.serverArtifactCount,
                firstResult.serverArtifactCount ==
                        ActiveFoodKnowledgeScope.activeArtifacts.size
            )

            assertTrue(
                "Unexpected Runtime Knowledge artifact count: " +
                        firstResult.runtimeArtifactCount,
                firstResult.runtimeArtifactCount ==
                        ActiveFoodKnowledgeScope.activeArtifacts.size
            )

            assertTrue(
                "Runtime Knowledge artifact set differs from active Food Knowledge scope.",
                firstResult.runtimeArtifacts.toSet() ==
                        ActiveFoodKnowledgeScope.activeArtifacts
            )

            val staleRuntimeArtifact =
                paths.runtimeArtifact(
                    "__stale-runtime-input__.json"
                )

            staleRuntimeArtifact.writeText(
                """{"stale":true}"""
            )

            assertTrue(
                "Could not create stale Runtime Knowledge regression fixture.",
                staleRuntimeArtifact.isFile
            )

            val secondResult =
                FullRuntimeKnowledgeRebuild()
                    .rebuild(
                        paths =
                            paths,
                        publish =
                            false
                    )

            assertFalse(
                "Stale Runtime Knowledge artifact survived the rebuild.",
                staleRuntimeArtifact.exists()
            )

            assertFalse(
                "Stale Runtime Knowledge artifact leaked into rebuild result.",
                secondResult.runtimeArtifacts.contains(
                    staleRuntimeArtifact.name
                )
            )

            assertTrue(
                "Runtime artifact set changed after rebuilding from stale Runtime state.",
                firstResult.runtimeArtifacts ==
                        secondResult.runtimeArtifacts
            )
        } finally {
            temporaryProjectRoot.deleteRecursively()
        }
    }

    private fun createIsolatedBuildFixture(
        sourcePaths: KnowledgeBuildPaths,
        projectRoot: File
    ): KnowledgeBuildPaths {

        projectRoot
            .resolve("app")
            .mkdirs()

        projectRoot
            .resolve("data")
            .mkdirs()

        projectRoot
            .resolve("gradlew")
            .writeText("")

        val paths =
            KnowledgeBuildPaths.fromProjectRoot(
                projectRoot
            )

        copyFile(
            source =
                sourcePaths.canonicalFoodCatalog,
            target =
                paths.canonicalFoodCatalog
        )

        copyFile(
            source =
                sourcePaths.catalogServerMappings,
            target =
                paths.catalogServerMappings
        )

        val sourceServerArtifacts =
            sourcePaths.serverRoot
                .listFiles()
                .orEmpty()
                .asSequence()
                .filter {
                    it.isFile
                }
                .filter {
                    it.extension.equals(
                        other = "json",
                        ignoreCase = true
                    )
                }
                .sortedBy {
                    it.name
                }
                .toList()

        require(
            sourceServerArtifacts.isNotEmpty()
        ) {
            "No Server Knowledge artifacts available for regression fixture: " +
                    sourcePaths.serverRoot.absolutePath
        }

        require(
            paths.serverRoot.mkdirs() ||
                    paths.serverRoot.isDirectory
        ) {
            "Could not create isolated Server Knowledge directory: " +
                    paths.serverRoot.absolutePath
        }

        sourceServerArtifacts.forEach { source ->

            source.copyTo(
                target =
                    paths.serverRoot.resolve(
                        source.name
                    ),
                overwrite =
                    true
            )
        }

        return paths
    }

    private fun copyFile(
        source: File,
        target: File
    ) {

        require(
            source.isFile
        ) {
            "Regression fixture source file does not exist: " +
                    source.absolutePath
        }

        val parent =
            requireNotNull(
                target.parentFile
            )

        require(
            parent.mkdirs() ||
                    parent.isDirectory
        ) {
            "Could not create regression fixture directory: " +
                    parent.absolutePath
        }

        source.copyTo(
            target =
                target,
            overwrite =
                true
        )
    }
}
