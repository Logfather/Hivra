package de.shopme.testing.system.tools.knowledge.multisource

import de.shopme.tools.knowledge.ai.builder.runtime.MultiSourceRuntimeKnowledgeBuild
import de.shopme.tools.knowledge.ai.builder.runtime.MultiSourceRuntimeKnowledgeBuildResult
import de.shopme.tools.knowledge.ciqual.model.CiqualSourceFiles
import java.io.File

object RunFullMultiSourceServerKnowledgeBuild {

    @JvmStatic
    fun main(args: Array<String>) {

        val projectRoot =
            resolveProjectRoot()

        val offFile =
            args.getOrNull(0)
                ?.let(::File)
                ?.canonicalFile
                ?: projectRoot.resolve(
                    "data/generated/openfoodfacts/" +
                            "openfoodfacts-products.slim.jsonl.gz"
                )
                    .canonicalFile

        val agribalyseFile =
            args.getOrNull(1)
                ?.let(::File)
                ?.canonicalFile
                ?: projectRoot.resolve(
                    "data/generated/agribalyse/" +
                            "agribalyse-foods.slim.tsv"
                )
                    .canonicalFile

        val ciqualDirectory =
            args.getOrNull(2)
                ?.let(::File)
                ?.canonicalFile
                ?: resolveCiqualDirectory(
                    projectRoot =
                        projectRoot
                )

        val outputDirectory =
            args.getOrNull(3)
                ?.let(::File)
                ?.canonicalFile
                ?: projectRoot.resolve(
                    "data/generated/knowledge/server"
                )
                    .canonicalFile

        val offNutritionAggregateFile =
            args.getOrNull(4)
                ?.let(::File)
                ?.canonicalFile
                ?: projectRoot.resolve(
                    "data/generated/knowledge/references/off/" +
                            "off-nutrition-reference-aggregates.json"
                )
                    .canonicalFile

        require(offFile.isFile) {
            "OFF source file does not exist: " +
                    offFile.absolutePath
        }

        require(offNutritionAggregateFile.isFile) {
            "OFF nutrition aggregate file does not exist: " +
                    offNutritionAggregateFile.absolutePath
        }

        require(agribalyseFile.isFile) {
            "Agribalyse source file does not exist: " +
                    agribalyseFile.absolutePath
        }

        require(ciqualDirectory.isDirectory) {
            "CIQUAL source directory does not exist: " +
                    ciqualDirectory.absolutePath
        }

        ensureOutputDirectoryExists(
            outputDirectory =
                outputDirectory
        )

        val result =
            MultiSourceRuntimeKnowledgeBuild()
                .build(
                    offFile =
                        offFile,
                    offNutritionAggregateFile =
                        offNutritionAggregateFile,
                    agribalyseFile =
                        agribalyseFile,
                    ciqualDirectory =
                        ciqualDirectory,
                    outputDir =
                        outputDirectory,
                    maxOffCandidates =
                        null,
                    maxOffNutritionAggregates =
                        null
                )

        verifyResult(
            result =
                result,
            outputDirectory =
                outputDirectory
        )

        printResult(
            result =
                result,
            offFile =
                offFile,
            offNutritionAggregateFile =
                offNutritionAggregateFile,
            agribalyseFile =
                agribalyseFile,
            ciqualDirectory =
                ciqualDirectory,
            outputDirectory =
                outputDirectory
        )
    }

    fun resolveDefaultOutputDirectory(): File =
        resolveProjectRoot()
            .resolve(
                "data/generated/knowledge/server"
            )
            .canonicalFile

    private fun resolveProjectRoot(): File {
        val currentDirectory =
            File(".").canonicalFile

        return when {
            currentDirectory.name == "app" ->
                requireNotNull(
                    currentDirectory.parentFile
                ) {
                    "Could not resolve project root from: " +
                            currentDirectory.absolutePath
                }
                    .canonicalFile

            currentDirectory.resolve("app").isDirectory ->
                currentDirectory

            else ->
                error(
                    "Could not resolve ShopMe project root from: " +
                            currentDirectory.absolutePath
                )
        }
    }

    private fun resolveCiqualDirectory(
        projectRoot: File
    ): File {

        val candidates =
            listOf(
                projectRoot.resolve(
                    "data/raw/ciqual/Ciqual"
                ),
                projectRoot.resolve(
                    "data/generated/ciqual"
                )
            )

        return candidates
            .firstOrNull { candidate ->
                candidate.isDirectory &&
                        runCatching {
                            CiqualSourceFiles
                                .fromDirectory(
                                    candidate
                                )
                                .validate()

                            true
                        }
                            .getOrDefault(false)
            }
            ?.canonicalFile
            ?: error(
                buildString {
                    appendLine(
                        "No complete CIQUAL source directory was found."
                    )
                    appendLine("Checked:")

                    candidates.forEach { candidate ->
                        appendLine(
                            "- ${candidate.absolutePath}"
                        )
                    }

                    append(
                        "Pass the complete CIQUAL source directory " +
                                "as the third runner argument."
                    )
                }
            )
    }

    private fun ensureOutputDirectoryExists(
        outputDirectory: File
    ) {
        if (!outputDirectory.exists()) {
            check(
                outputDirectory.mkdirs()
            ) {
                "Could not create server knowledge output directory: " +
                        outputDirectory.absolutePath
            }
        }

        require(outputDirectory.isDirectory) {
            "Server knowledge output path is not a directory: " +
                    outputDirectory.absolutePath
        }
    }

    private fun verifyResult(
        result: MultiSourceRuntimeKnowledgeBuildResult,
        outputDirectory: File
    ) {
        check(result.offCandidateCount > 0) {
            "Full server build produced no OFF candidates."
        }

        check(result.offNutritionAggregateCount > 0) {
            "Full server build produced no OFF nutrition aggregates."
        }

        check(result.agribalyseCandidateCount > 0) {
            "Full server build produced no Agribalyse candidates."
        }

        check(result.ciqualCandidateCount > 0) {
            "Full server build produced no CIQUAL candidates."
        }

        check(result.nutritionCandidateCount > 0) {
            "Full server build produced no nutrition candidates."
        }

        check(result.nutritionArtifactEntryCount > 0) {
            "Full server build produced an empty nutrition artifact."
        }

        check(result.nutritionArtifactFile.isFile) {
            "Nutrition artifact was not written: " +
                    result.nutritionArtifactFile.absolutePath
        }

        val expectedNutritionFile =
            outputDirectory
                .resolve(
                    "nutrition.json"
                )
                .canonicalFile

        check(
            result.nutritionArtifactFile.canonicalFile ==
                    expectedNutritionFile
        ) {
            "Nutrition artifact was written to the wrong location. " +
                    "Expected=${expectedNutritionFile.absolutePath}, " +
                    "actual=${result.nutritionArtifactFile.absolutePath}"
        }
    }

    private fun printResult(
        result: MultiSourceRuntimeKnowledgeBuildResult,
        offFile: File,
        offNutritionAggregateFile: File,
        agribalyseFile: File,
        ciqualDirectory: File,
        outputDirectory: File
    ) {
        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("FULL MULTI-SOURCE SERVER BUILD DONE")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
        println("OFF file=${offFile.path}")
        println(
            "OFF nutrition aggregate file=" +
                    offNutritionAggregateFile.path
        )
        println("Agribalyse file=${agribalyseFile.path}")
        println("CIQUAL directory=${ciqualDirectory.path}")
        println("Output directory=${outputDirectory.path}")
        println()
        println("OFF candidates=${result.offCandidateCount}")
        println(
            "OFF nutrition aggregates=" +
                    result.offNutritionAggregateCount
        )
        println(
            "Agribalyse candidates=" +
                    result.agribalyseCandidateCount
        )
        println(
            "CIQUAL candidates=" +
                    result.ciqualCandidateCount
        )
        println(
            "Input candidates=" +
                    result.inputCandidateCount
        )
        println(
            "Normalized candidates=" +
                    result.normalizedCandidateCount
        )
        println(
            "Merged candidates=" +
                    result.mergedCandidateCount
        )
        println("Conflicts=${result.conflictCount}")
        println()
        println(
            "Nutrition candidates=" +
                    result.nutritionCandidateCount
        )
        println(
            "Nutrition artifact entries=" +
                    result.nutritionArtifactEntryCount
        )
        println(
            "Nutrition artifact=" +
                    result.nutritionArtifactFile.path
        )
        println()
        println("SERVER KNOWLEDGE BUILD SUCCESSFUL")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}