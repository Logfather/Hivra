package de.shopme.testing.system.tools.knowledge.multisource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunFullMultiSourceServerKnowledgeBuildTest {

    @Test
    fun runFullMultiSourceServerKnowledgeBuild() {

        RunFullMultiSourceServerKnowledgeBuild.main(
            emptyArray()
        )

        val outputDirectory =
            RunFullMultiSourceServerKnowledgeBuild
                .resolveDefaultOutputDirectory()

        val nutritionArtifactFile =
            outputDirectory.resolve(
                "nutrition.json"
            )

        assertEquals(
            "server",
            outputDirectory.name,
            "Resolved output directory must be the server knowledge directory."
        )

        assertTrue(
            nutritionArtifactFile.isFile,
            "Server nutrition artifact was not generated: " +
                    nutritionArtifactFile.absolutePath
        )

        assertTrue(
            nutritionArtifactFile.length() > 0L,
            "Generated server nutrition artifact is empty: " +
                    nutritionArtifactFile.absolutePath
        )
    }
}