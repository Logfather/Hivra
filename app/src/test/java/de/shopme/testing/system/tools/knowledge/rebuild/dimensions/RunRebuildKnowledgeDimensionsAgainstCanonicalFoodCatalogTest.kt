package de.shopme.testing.system.tools.knowledge.rebuild.dimensions

import de.shopme.tools.knowledge.build.ActiveFoodKnowledgeScope
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.rebuild.dimensions.runner.RunRebuildKnowledgeDimensionsAgainstCanonicalFoodCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class RunRebuildKnowledgeDimensionsAgainstCanonicalFoodCatalogTest {

    @Test
    fun rebuildKnowledgeDimensionsAgainstCanonicalFoodCatalog() {

        RunRebuildKnowledgeDimensionsAgainstCanonicalFoodCatalog.main(
            emptyArray()
        )

        val paths =
            KnowledgeBuildPaths.default()

        val activeServerArtifacts =
            paths.serverRoot
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
                .map {
                    it.name
                }
                .toSortedSet()

        val legacyServerArtifacts =
            paths.legacyServerRoot
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
                .map {
                    it.name
                }
                .toSortedSet()

        assertEquals(
            "Productive Server Knowledge artifact set differs from active Food Knowledge scope.",
            ActiveFoodKnowledgeScope.activeArtifacts,
            activeServerArtifacts
        )

        assertEquals(
            "Legacy Server Knowledge artifact set differs from legacy Food Knowledge scope.",
            ActiveFoodKnowledgeScope.legacyArtifacts,
            legacyServerArtifacts
        )

        assertEquals(
            "Active Food Knowledge artifact count differs.",
            12,
            activeServerArtifacts.size
        )

        assertEquals(
            "Legacy Food Knowledge artifact count differs.",
            11,
            legacyServerArtifacts.size
        )
    }
}