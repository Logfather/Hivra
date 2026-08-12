package de.shopme.testing.system.tools.knowledge.build

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeBuildLegacyPathGuardTest {

    @Test
    fun productiveKnowledgeCodeContainsNoLegacyBuildPaths() {

        val projectRoot =
            KnowledgeBuildPaths
                .default()
                .projectRoot

        val productionRoot =
            projectRoot.resolve(
                "app/src/main/java/de/shopme/tools/knowledge"
            )

        require(productionRoot.isDirectory) {
            "Production Knowledge source directory missing: " +
                    productionRoot.absolutePath
        }

        val forbiddenPatterns =
            listOf(
                "supermarket_dataset",
                "data/raw/catalog",
                "../data/raw/catalog",
                "data/generated/knowledge",
                "../data/generated/knowledge",
                "app/data/",
                "app/data\\",
                "data/generated/foods.json"
            )

        val violations =
            productionRoot
                .walkTopDown()
                .filter {
                    it.isFile &&
                            it.extension == "kt"
                }
                .flatMap { file ->

                    file
                        .readLines(Charsets.UTF_8)
                        .asSequence()
                        .mapIndexedNotNull {
                                index,
                                line ->

                            val forbidden =
                                forbiddenPatterns
                                    .firstOrNull {
                                            pattern ->
                                        line.contains(pattern)
                                    }

                            if (forbidden == null) {
                                null
                            } else {
                                LegacyPathViolation(
                                    file =
                                        file.relativeTo(
                                            projectRoot
                                        ).path,
                                    line =
                                        index + 1,
                                    forbidden =
                                        forbidden,
                                    source =
                                        line.trim()
                                )
                            }
                        }
                }
                .toList()

        if (violations.isNotEmpty()) {

            println()
            println("========== LEGACY KNOWLEDGE PATHS ==========")

            violations.forEach {

                println(
                    "${it.file}:${it.line} -> ${it.source}"
                )
            }

            println("============================================")
        }

        assertTrue(
            "Legacy Knowledge Build paths detected.",
            violations.isEmpty()
        )
    }

    private data class LegacyPathViolation(
        val file: String,
        val line: Int,
        val forbidden: String,
        val source: String
    )
}