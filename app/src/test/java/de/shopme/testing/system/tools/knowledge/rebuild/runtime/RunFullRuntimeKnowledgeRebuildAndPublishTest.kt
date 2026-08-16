package de.shopme.testing.system.tools.knowledge.rebuild.runtime

import de.shopme.tools.knowledge.build.ActiveFoodKnowledgeScope
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.rebuild.runtime.runner.RunFullRuntimeKnowledgeRebuild
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RunFullRuntimeKnowledgeRebuildAndPublishTest {

    @Test
    fun runFullRuntimeKnowledgeRebuildAndPublish() {

        RunFullRuntimeKnowledgeRebuild.main(
            emptyArray()
        )

        val paths =
            KnowledgeBuildPaths.default()

        val runtimeArtifacts =
            jsonArtifacts(
                directory =
                    paths.runtimeRoot
            )

        val publishedArtifacts =
            jsonArtifacts(
                directory =
                    paths.publishedRuntimeRoot
            )

        assertEquals(
            "Build Runtime Knowledge artifact set differs from active Food Knowledge scope.",
            ActiveFoodKnowledgeScope.activeArtifacts,
            runtimeArtifacts.keys
        )

        assertEquals(
            "Published Runtime Knowledge artifact set differs from active Food Knowledge scope.",
            ActiveFoodKnowledgeScope.activeArtifacts,
            publishedArtifacts.keys
        )

        assertEquals(
            "Published Runtime Knowledge artifact set differs from generated Runtime Knowledge.",
            runtimeArtifacts.keys,
            publishedArtifacts.keys
        )

        runtimeArtifacts
            .forEach { (artifactName, runtimeFile) ->

                val publishedFile =
                    requireNotNull(
                        publishedArtifacts[
                            artifactName
                        ]
                    ) {
                        "Published Runtime Knowledge artifact missing: " +
                                artifactName
                    }

                assertTrue(
                    "Generated Runtime Knowledge artifact is empty: " +
                            runtimeFile.absolutePath,
                    runtimeFile.length() > 0L
                )

                assertTrue(
                    "Published Runtime Knowledge artifact is empty: " +
                            publishedFile.absolutePath,
                    publishedFile.length() > 0L
                )

                assertEquals(
                    "Published Runtime Knowledge artifact differs from generated Runtime artifact: " +
                            artifactName,
                    runtimeFile.readBytes().toList(),
                    publishedFile.readBytes().toList()
                )
            }
    }

    private fun jsonArtifacts(
        directory: File
    ): Map<String, File> {

        assertTrue(
            "Runtime Knowledge directory does not exist: " +
                    directory.absolutePath,
            directory.isDirectory
        )

        return directory
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
            .associateBy {
                it.name
            }
            .toSortedMap()
    }
}