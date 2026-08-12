package de.shopme.tools.knowledge.rebuild.runtime

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.runtime.CatalogRuntimeKnowledgeGenerator
import java.io.File

data class FullRuntimeKnowledgeRebuildResult(
    val catalogFile: String,
    val serverArtifactDirectory: String,
    val mappingFile: String,
    val runtimeArtifactDirectory: String,
    val publishedRuntimeDirectory: String,
    val serverArtifactCount: Int,
    val runtimeArtifactCount: Int,
    val publishedArtifactCount: Int,
    val runtimeArtifacts: List<String>,
    val publishedArtifacts: List<String>
)

class FullRuntimeKnowledgeRebuild(
    private val generator:
    CatalogRuntimeKnowledgeGenerator =
        CatalogRuntimeKnowledgeGenerator()
) {

    fun rebuild(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default(),
        publish: Boolean = true
    ): FullRuntimeKnowledgeRebuildResult {

        paths.ensureBuildDirectories()

        validateInputs(
            paths = paths
        )

        val serverArtifacts =
            jsonFiles(
                directory =
                    paths.serverRoot
            )

        cleanRuntimeArtifacts(
            directory =
                paths.runtimeRoot
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("FULL RUNTIME KNOWLEDGE REBUILD")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog  : ${paths.canonicalFoodCatalog.path}"
        )
        println(
            "Server   : ${paths.serverRoot.path}"
        )
        println(
            "Mappings : ${paths.catalogServerMappings.path}"
        )
        println(
            "Runtime  : ${paths.runtimeRoot.path}"
        )
        println(
            "Publish  : ${paths.publishedRuntimeRoot.path}"
        )
        println(
            "Artifacts: ${serverArtifacts.size}"
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()

        generator.generate(
            catalogFile =
                paths.canonicalFoodCatalog,
            serverArtifactDirectory =
                paths.serverRoot,
            runtimeArtifactDirectory =
                paths.runtimeRoot,
            catalogServerMappingFile =
                paths.catalogServerMappings
        )

        val runtimeArtifacts =
            jsonFiles(
                directory =
                    paths.runtimeRoot
            )

        require(
            runtimeArtifacts.isNotEmpty()
        ) {
            "Full Runtime Rebuild produced no runtime artifacts: " +
                    paths.runtimeRoot.absolutePath
        }

        validateRuntimeArtifacts(
            runtimeArtifacts =
                runtimeArtifacts
        )

        val publishedArtifacts =
            if (publish) {
                publishRuntimeArtifacts(
                    runtimeArtifacts =
                        runtimeArtifacts,
                    publishedDirectory =
                        paths.publishedRuntimeRoot
                )
            } else {
                emptyList()
            }

        if (publish) {
            validatePublishedArtifacts(
                runtimeArtifacts =
                    runtimeArtifacts,
                publishedArtifacts =
                    publishedArtifacts
            )
        }

        val result =
            FullRuntimeKnowledgeRebuildResult(
                catalogFile =
                    paths.canonicalFoodCatalog.path,
                serverArtifactDirectory =
                    paths.serverRoot.path,
                mappingFile =
                    paths.catalogServerMappings.path,
                runtimeArtifactDirectory =
                    paths.runtimeRoot.path,
                publishedRuntimeDirectory =
                    paths.publishedRuntimeRoot.path,
                serverArtifactCount =
                    serverArtifacts.size,
                runtimeArtifactCount =
                    runtimeArtifacts.size,
                publishedArtifactCount =
                    publishedArtifacts.size,
                runtimeArtifacts =
                    runtimeArtifacts.map {
                        it.name
                    },
                publishedArtifacts =
                    publishedArtifacts.map {
                        it.name
                    }
            )

        printResult(
            result = result,
            publish = publish
        )

        return result
    }

    private fun validateInputs(
        paths: KnowledgeBuildPaths
    ) {

        require(
            paths.canonicalFoodCatalog.isFile
        ) {
            "Canonical food catalog does not exist: " +
                    paths.canonicalFoodCatalog.absolutePath
        }

        require(
            paths.serverRoot.isDirectory
        ) {
            "Server Knowledge artifact directory does not exist: " +
                    paths.serverRoot.absolutePath
        }

        require(
            paths.catalogServerMappings.isFile
        ) {
            "Central catalog-server mapping file does not exist: " +
                    paths.catalogServerMappings.absolutePath
        }

        require(
            jsonFiles(
                directory =
                    paths.serverRoot
            ).isNotEmpty()
        ) {
            "No server Knowledge artifacts found in: " +
                    paths.serverRoot.absolutePath
        }
    }

    private fun cleanRuntimeArtifacts(
        directory: File
    ) {

        if (!directory.exists()) {
            require(
                directory.mkdirs()
            ) {
                "Could not create Runtime Knowledge directory: " +
                        directory.absolutePath
            }

            return
        }

        require(
            directory.isDirectory
        ) {
            "Runtime Knowledge path is not a directory: " +
                    directory.absolutePath
        }

        jsonFiles(
            directory = directory
        ).forEach { file ->

            require(
                file.delete()
            ) {
                "Could not remove stale Runtime Knowledge artifact: " +
                        file.absolutePath
            }
        }
    }

    private fun validateRuntimeArtifacts(
        runtimeArtifacts: List<File>
    ) {

        runtimeArtifacts.forEach { file ->

            require(
                file.isFile
            ) {
                "Runtime artifact does not exist: " +
                        file.absolutePath
            }

            require(
                file.length() > 0L
            ) {
                "Runtime artifact is empty: " +
                        file.absolutePath
            }
        }
    }

    private fun publishRuntimeArtifacts(
        runtimeArtifacts: List<File>,
        publishedDirectory: File
    ): List<File> {

        if (!publishedDirectory.exists()) {
            require(
                publishedDirectory.mkdirs()
            ) {
                "Could not create published Runtime directory: " +
                        publishedDirectory.absolutePath
            }
        }

        require(
            publishedDirectory.isDirectory
        ) {
            "Published Runtime path is not a directory: " +
                    publishedDirectory.absolutePath
        }

        /*
         * Publication is an exact JSON mirror of build/knowledge/runtime.
         *
         * Remove stale JSON artifacts first so files that disappeared from
         * the Runtime build cannot silently survive in the application.
         */
        jsonFiles(
            directory =
                publishedDirectory
        ).forEach { file ->

            require(
                file.delete()
            ) {
                "Could not remove stale published Runtime artifact: " +
                        file.absolutePath
            }
        }

        runtimeArtifacts.forEach { source ->

            val target =
                publishedDirectory.resolve(
                    source.name
                )

            source.copyTo(
                target =
                    target,
                overwrite =
                    true
            )
        }

        return jsonFiles(
            directory =
                publishedDirectory
        )
    }

    private fun validatePublishedArtifacts(
        runtimeArtifacts: List<File>,
        publishedArtifacts: List<File>
    ) {

        val runtimeByName =
            runtimeArtifacts.associateBy {
                it.name
            }

        val publishedByName =
            publishedArtifacts.associateBy {
                it.name
            }

        require(
            runtimeByName.keys ==
                    publishedByName.keys
        ) {
            buildString {
                appendLine(
                    "Published Runtime artifact set differs from build output."
                )

                appendLine(
                    "Runtime   : " +
                            runtimeByName.keys
                                .sorted()
                                .joinToString()
                )

                appendLine(
                    "Published : " +
                            publishedByName.keys
                                .sorted()
                                .joinToString()
                )
            }
        }

        runtimeByName
            .toSortedMap()
            .forEach { (name, runtimeFile) ->

                val publishedFile =
                    requireNotNull(
                        publishedByName[name]
                    )

                require(
                    runtimeFile.readBytes()
                        .contentEquals(
                            publishedFile.readBytes()
                        )
                ) {
                    "Published Runtime artifact is not byte-identical: " +
                            name
                }
            }
    }

    private fun jsonFiles(
        directory: File
    ): List<File> =
        directory
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

    private fun printResult(
        result: FullRuntimeKnowledgeRebuildResult,
        publish: Boolean
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("FULL RUNTIME REBUILD RESULT")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Server artifacts    : " +
                    result.serverArtifactCount
        )
        println(
            "Runtime artifacts   : " +
                    result.runtimeArtifactCount
        )

        if (publish) {
            println(
                "Published artifacts : " +
                        result.publishedArtifactCount
            )
        } else {
            println(
                "Published artifacts : disabled"
            )
        }

        println()
        println("Runtime:")

        result.runtimeArtifacts.forEach {
            println("  ✓ $it")
        }

        if (publish) {
            println()
            println("Published:")

            result.publishedArtifacts.forEach {
                println("  ✓ $it")
            }
        }

        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("FINISHED")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }
}