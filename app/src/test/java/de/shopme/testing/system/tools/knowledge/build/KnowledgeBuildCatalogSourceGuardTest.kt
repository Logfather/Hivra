package de.shopme.testing.system.tools.knowledge.build

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import kotlin.test.Test
import kotlin.test.assertTrue

class KnowledgeBuildCatalogSourceGuardTest {

    @Test
    fun productiveKnowledgeBuildDoesNotReadCatalogSnapshotsDirectly() {

        val paths =
            KnowledgeBuildPaths.default()

        val knowledgeRoot =
            paths.projectRoot.resolve(
                "app/src/main/java/de/shopme/tools/knowledge"
            )

        require(knowledgeRoot.isDirectory) {
            "Productive Knowledge source directory does not exist: " +
                    knowledgeRoot.absolutePath
        }

        val forbiddenPatterns =
            listOf(
                "/catalog/final/canonical-food-catalog",
                "/catalog/regeneration/canonical-food-catalog",
                "data/generated/knowledge/catalog",
                "data/raw/catalog",
                "app/src/main/assets/catalog",
                "supermarket_dataset"
            )

        val violations =
            knowledgeRoot
                .walkTopDown()
                .filter {
                    it.isFile &&
                            it.extension == "kt"
                }
                .flatMap { file ->

                    val text =
                        file.readText()

                    forbiddenPatterns
                        .filter { pattern ->
                            text.contains(pattern)
                        }
                        .map { pattern ->
                            "${file.relativeTo(paths.projectRoot).path}: " +
                                    pattern
                        }
                        .asSequence()
                }
                .toList()

        assertTrue(
            violations.isEmpty(),
            buildString {
                appendLine(
                    "Productive Knowledge Build code must not read " +
                            "legacy catalog sources or intermediate " +
                            "catalog snapshots directly."
                )

                violations.forEach {
                    appendLine(it)
                }
            }
        )
    }
}