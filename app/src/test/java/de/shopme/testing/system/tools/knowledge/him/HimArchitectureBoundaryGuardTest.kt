package de.shopme.testing.system.tools.knowledge.him

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class HimArchitectureBoundaryGuardTest {

    @Test
    fun himMustNotDependOnPreHimArchitecture() {

        val paths =
            KnowledgeBuildPaths.default()

        val himSourceRoot =
            paths.projectRoot.resolve(
                "app/src/main/java/de/shopme/tools/knowledge/him"
            )

        /*
         * HIM does not exist yet.
         *
         * The guard is intentionally installed before the first HIM
         * production class is created. As soon as the package exists,
         * every Kotlin source below it is validated automatically.
         */
        if (!himSourceRoot.exists()) {
            return
        }

        assertTrue(
            himSourceRoot.isDirectory,
            "HIM source root must be a directory: " +
                    himSourceRoot.absolutePath
        )

        val kotlinFiles =
            himSourceRoot
                .walkTopDown()
                .filter {
                    it.isFile &&
                            it.extension.equals(
                                other = "kt",
                                ignoreCase = true
                            )
                }
                .sortedBy {
                    it.absolutePath
                }
                .toList()

        val violations =
            kotlinFiles
                .flatMap { file ->
                    findViolations(
                        sourceRoot =
                            himSourceRoot,
                        file =
                            file
                    )
                }

        assertTrue(
            violations.isEmpty(),
            buildString {
                appendLine(
                    "HIM architecture boundary violated."
                )
                appendLine()
                appendLine(
                    "HIM must not depend on PRE-HIM Candidate, Mapping, " +
                            "Patch, Compiler or legacy Knowledge Builder architecture."
                )
                appendLine()
                appendLine(
                    "Violations:"
                )

                violations.forEach { violation ->
                    appendLine(
                        "  - $violation"
                    )
                }

                appendLine()
                appendLine(
                    "Use source-neutral HIM contracts instead of reusing " +
                            "PRE-HIM pipeline types."
                )
            }
        )
    }

    private fun findViolations(
        sourceRoot: File,
        file: File
    ): List<String> {

        val source =
            file.readText()

        val relativePath =
            file.relativeTo(
                sourceRoot
            ).path

        return FORBIDDEN_DEPENDENCIES
            .filter { forbiddenDependency ->
                source.contains(
                    forbiddenDependency
                )
            }
            .map { forbiddenDependency ->
                "$relativePath -> $forbiddenDependency"
            }
    }

    private companion object {

        val FORBIDDEN_DEPENDENCIES =
            listOf(
                /*
                 * PRE-HIM AI/Knowledge Builder pipeline.
                 */
                "de.shopme.tools.knowledge.ai.builder",

                /*
                 * PRE-HIM canonical candidate / merge / partition model.
                 */
                "de.shopme.tools.knowledge.ki_candidates",

                /*
                 * Old compiler/import/patch architecture.
                 */
                "de.shopme.tools.knowledge.compiler",

                /*
                 * Old foods patch architecture.
                 */
                "de.shopme.tools.knowledge.patch",

                /*
                 * Historical catalog/server mapping architecture.
                 * The productive package has already been removed, but this
                 * guard prevents accidental reintroduction.
                 */
                "de.shopme.tools.knowledge.mapping.catalog",

                /*
                 * Old AI-side catalog mapping layer.
                 */
                "de.shopme.tools.knowledge.ai.catalog"
            )
    }
}