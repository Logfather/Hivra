package de.shopme.testing.system.tools.knowledge.multisource

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class RunFullMultiSourceServerKnowledgeBuildTest {

    @Test
    fun runFullMultiSourceServerKnowledgeBuild() {

        RunFullMultiSourceServerKnowledgeBuild.main(
            emptyArray()
        )

        val nutritionArtifactFile =
            File(
                "data/generated/knowledge/server/nutrition.json"
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