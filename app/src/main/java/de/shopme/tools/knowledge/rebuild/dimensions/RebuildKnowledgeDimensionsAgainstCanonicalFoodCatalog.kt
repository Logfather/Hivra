package de.shopme.tools.knowledge.rebuild.dimensions

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.agribalyse.parser.AgribalyseRawSourceReducer
import de.shopme.tools.knowledge.ai.builder.runtime.MultiSourceRuntimeKnowledgeBuild
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import java.security.MessageDigest

data class KnowledgeDimensionRebuildArtifact(
    val artifact: String,
    val entryCount: Int,
    val fileSizeBytes: Long,
    val sha256: String
)

data class KnowledgeDimensionRebuildReport(
    val version: Int,
    val catalogFile: String,
    val catalogEntryCount: Int,
    val catalogSha256: String,
    val offSourceFile: String,
    val offNutritionAggregateFile: String,
    val agribalyseSourceFile: String,
    val ciqualSourceDirectory: String?,
    val serverDirectory: String,
    val inputCandidateCount: Int,
    val normalizedCandidateCount: Int,
    val mergedCandidateCount: Int,
    val conflictCount: Int,
    val artifactCount: Int,
    val artifacts: List<KnowledgeDimensionRebuildArtifact>
) {

    companion object {
        const val CURRENT_VERSION =
            1
    }
}

class RebuildKnowledgeDimensionsAgainstCanonicalFoodCatalog(
    private val builder:
    MultiSourceRuntimeKnowledgeBuild =
        MultiSourceRuntimeKnowledgeBuild()
) {

    fun rebuild(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default(),
        offFile: File,
        offNutritionAggregateFile: File,
        agribalyseSourceFile: File,
        ciqualDirectory: File? = null,
        maxOffCandidates: Int? = null,
        maxOffNutritionAggregates: Int? = null
    ): KnowledgeDimensionRebuildReport {

        validateCatalog(
            paths =
                paths
        )

        validateSources(
            offFile =
                offFile,
            offNutritionAggregateFile =
                offNutritionAggregateFile,
            agribalyseSourceFile =
                agribalyseSourceFile,
            ciqualDirectory =
                ciqualDirectory
        )

        val agribalyseReferenceDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/references/agribalyse"
            )

        require(
            agribalyseReferenceDirectory.exists() ||
                    agribalyseReferenceDirectory.mkdirs()
        ) {
            "Could not create Agribalyse reference directory: " +
                    agribalyseReferenceDirectory.absolutePath
        }

        val agribalyseReferenceFile =
            agribalyseReferenceDirectory.resolve(
                "agribalyse-foods.slim.tsv"
            )

        AgribalyseRawSourceReducer()
            .reduce(
                input =
                    agribalyseSourceFile,
                output =
                    agribalyseReferenceFile,
                sheetName =
                    "Synthese"
            )

        require(
            agribalyseReferenceFile.isFile
        ) {
            "Agribalyse reference file was not generated: " +
                    agribalyseReferenceFile.absolutePath
        }

        require(
            agribalyseReferenceFile.length() > 0L
        ) {
            "Agribalyse reference file is empty: " +
                    agribalyseReferenceFile.absolutePath
        }

        val serverRebuildDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/server.rebuild"
            )

        resetRebuildDirectory(
            directory =
                serverRebuildDirectory
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("REBUILD KNOWLEDGE DIMENSIONS")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog     : " +
                    paths.canonicalFoodCatalog.path
        )
        println(
            "OFF         : " +
                    offFile.path
        )
        println(
            "Agribalyse  : " +
                    agribalyseSourceFile.path
        )
        println(
            "CIQUAL      : " +
                    (
                            ciqualDirectory
                                ?.path
                                ?: "disabled"
                            )
        )
        println(
            "Server      : " +
                    paths.serverRoot.path
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()

        val result =
            builder.build(
                offFile =
                    offFile,
                offNutritionAggregateFile =
                    offNutritionAggregateFile,
                agribalyseFile =
                    agribalyseReferenceFile,
                outputDir =
                    serverRebuildDirectory,
                maxOffCandidates =
                    maxOffCandidates,
                maxOffNutritionAggregates =
                    maxOffNutritionAggregates,
                ciqualDirectory =
                    ciqualDirectory
            )

        val artifacts =
            readGeneratedArtifacts(
                directory =
                    serverRebuildDirectory
            )

        require(
            artifacts.size ==
                    EXPECTED_SERVER_ARTIFACTS.size
        ) {
            buildString {
                append(
                    "Expected "
                )
                append(
                    EXPECTED_SERVER_ARTIFACTS.size
                )
                append(
                    " server Knowledge artifacts, found "
                )
                append(
                    artifacts.size
                )
                append(
                    "."
                )
            }
        }

        publishServerRebuild(
            rebuildDirectory =
                serverRebuildDirectory,
            serverDirectory =
                paths.serverRoot
        )

        val generatedNames =
            artifacts
                .map {
                    it.artifact
                }
                .toSet()

        require(
            generatedNames ==
                    EXPECTED_SERVER_ARTIFACTS
        ) {
            buildString {
                appendLine(
                    "Generated server Knowledge artifact set differs."
                )

                appendLine(
                    "Missing: " +
                            (
                                    EXPECTED_SERVER_ARTIFACTS -
                                            generatedNames
                                    )
                                .sorted()
                                .joinToString()
                )

                appendLine(
                    "Unexpected: " +
                            (
                                    generatedNames -
                                            EXPECTED_SERVER_ARTIFACTS
                                    )
                                .sorted()
                                .joinToString()
                )
            }
        }

        val catalogBytes =
            paths.canonicalFoodCatalog
                .readBytes()

        val publishedArtifacts =
            readGeneratedArtifacts(
                directory =
                    paths.serverRoot
            )

        val report =
            KnowledgeDimensionRebuildReport(
                version =
                    KnowledgeDimensionRebuildReport.CURRENT_VERSION,
                catalogFile =
                    paths.canonicalFoodCatalog.path,
                catalogEntryCount =
                    readCatalogEntryCount(
                        file = paths.canonicalFoodCatalog
                    ),
                catalogSha256 =
                    sha256(
                        bytes = catalogBytes
                    ),
                offSourceFile =
                    offFile.path,
                offNutritionAggregateFile =
                    offNutritionAggregateFile.path,
                agribalyseSourceFile =
                    agribalyseSourceFile.path,
                ciqualSourceDirectory =
                    ciqualDirectory?.path,
                serverDirectory =
                    paths.serverRoot.path,
                inputCandidateCount =
                    result.inputCandidateCount,
                normalizedCandidateCount =
                    result.normalizedCandidateCount,
                mergedCandidateCount =
                    result.mergedCandidateCount,
                conflictCount =
                    result.conflictCount,
                artifactCount =
                    publishedArtifacts.size,
                artifacts =
                    publishedArtifacts
            )

        require(
            report.catalogEntryCount ==
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT
        ) {
            "Canonical catalog entry count changed. " +
                    "Expected " +
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_ENTRY_COUNT +
                    ", found " +
                    report.catalogEntryCount +
                    "."
        }

        require(
            report.catalogSha256 ==
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_SHA256
        ) {
            "Canonical catalog SHA-256 changed. " +
                    "Expected " +
                    KnowledgeBuildPaths
                        .CANONICAL_CATALOG_SHA256 +
                    ", found " +
                    report.catalogSha256 +
                    "."
        }

        val reportFile =
            paths.reportsRoot.resolve(
                "knowledge-dimensions-rebuild-report.json"
            )

        writeReport(
            report =
                report,
            file =
                reportFile
        )

        printReport(
            report =
                report,
            reportFile =
                reportFile
        )

        return report
    }

    private fun validateCatalog(
        paths: KnowledgeBuildPaths
    ) {

        require(
            paths.canonicalFoodCatalog.isFile
        ) {
            "Canonical food catalog does not exist: " +
                    paths.canonicalFoodCatalog.absolutePath
        }
    }

    private fun validateSources(
        offFile: File,
        offNutritionAggregateFile: File,
        agribalyseSourceFile: File,
        ciqualDirectory: File?
    ) {

        require(offFile.isFile) {
            "OFF source file does not exist: " +
                    offFile.absolutePath
        }

        require(offNutritionAggregateFile.isFile) {
            "OFF nutrition aggregate file does not exist: " +
                    offNutritionAggregateFile.absolutePath
        }

        require(agribalyseSourceFile.isFile) {
            "Agribalyse source file does not exist: " +
                    agribalyseSourceFile.absolutePath
        }

        require(
            agribalyseSourceFile.extension.equals(
                other = "xlsx",
                ignoreCase = true
            )
        ) {
            "Agribalyse source must be an XLSX file: " +
                    agribalyseSourceFile.absolutePath
        }

        if (ciqualDirectory != null) {
            require(ciqualDirectory.isDirectory) {
                "CIQUAL source directory does not exist: " +
                        ciqualDirectory.absolutePath
            }
        }
    }

    private fun resetRebuildDirectory(
        directory: File
    ) {

        if (directory.exists()) {

            require(
                directory.deleteRecursively()
            ) {
                "Could not reset server Knowledge rebuild directory: " +
                        directory.absolutePath
            }
        }

        require(
            directory.mkdirs()
        ) {
            "Could not create server Knowledge rebuild directory: " +
                    directory.absolutePath
        }
    }

    private fun publishServerRebuild(
        rebuildDirectory: File,
        serverDirectory: File
    ) {

        val rebuiltArtifacts =
            rebuildDirectory
                .listFiles()
                .orEmpty()
                .filter {
                    it.isFile &&
                            it.extension.equals(
                                "json",
                                ignoreCase = true
                            )
                }

        require(
            rebuiltArtifacts.size ==
                    EXPECTED_SERVER_ARTIFACTS.size
        ) {
            "Cannot publish incomplete server Knowledge rebuild."
        }

        val previousDirectory =
            serverDirectory.resolveSibling(
                "server.previous"
            )

        if (previousDirectory.exists()) {
            previousDirectory.deleteRecursively()
        }

        if (serverDirectory.exists()) {

            require(
                serverDirectory.renameTo(
                    previousDirectory
                )
            ) {
                "Could not backup current server directory."
            }
        }

        try {

            require(
                rebuildDirectory.renameTo(
                    serverDirectory
                )
            ) {
                "Could not publish rebuilt server directory."
            }

            previousDirectory.deleteRecursively()

        } catch (t: Throwable) {

            if (
                !serverDirectory.exists() &&
                previousDirectory.exists()
            ) {
                previousDirectory.renameTo(
                    serverDirectory
                )
            }

            throw t
        }
    }

    private fun readGeneratedArtifacts(
        directory: File
    ): List<KnowledgeDimensionRebuildArtifact> =
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
            .map { file ->

                KnowledgeDimensionRebuildArtifact(
                    artifact =
                        file.name,
                    entryCount =
                        readArtifactEntryCount(
                            file =
                                file
                        ),
                    fileSizeBytes =
                        file.length(),
                    sha256 =
                        sha256(
                            bytes =
                                file.readBytes()
                        )
                )
            }
            .toList()

    private fun readCatalogEntryCount(
        file: File
    ): Int {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )

        require(
            root.isJsonArray
        ) {
            "Canonical catalog root must be an array: " +
                    file.absolutePath
        }

        return root
            .asJsonArray
            .size()
    }

    private fun readArtifactEntryCount(
        file: File
    ): Int {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )

        require(
            root.isJsonObject
        ) {
            "Server Knowledge artifact root must be an object: " +
                    file.absolutePath
        }

        val entries =
            root
                .asJsonObject
                .get("entries")

        require(
            entries != null
        ) {
            "Server Knowledge artifact does not contain 'entries': " +
                    file.absolutePath
        }

        return when {

            entries.isJsonObject ->
                entries
                    .asJsonObject
                    .size()

            entries.isJsonArray ->
                entries
                    .asJsonArray
                    .size()

            else ->
                error(
                    "Unsupported 'entries' structure in: " +
                            file.absolutePath
                )
        }
    }

    private fun writeReport(
        report: KnowledgeDimensionRebuildReport,
        file: File
    ) {

        val parentDirectory =
            requireNotNull(
                file.parentFile
            ) {
                "Report has no parent directory: " +
                        file.absolutePath
            }

        require(
            parentDirectory.exists() ||
                    parentDirectory.mkdirs()
        ) {
            "Could not create report directory: " +
                    parentDirectory.absolutePath
        }

        file.writeText(
            gson.toJson(
                report
            ) + "\n"
        )
    }

    private fun printReport(
        report: KnowledgeDimensionRebuildReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("KNOWLEDGE DIMENSIONS REBUILD RESULT")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries       : " +
                    report.catalogEntryCount
        )
        println(
            "Input candidates       : " +
                    report.inputCandidateCount
        )
        println(
            "Normalized candidates  : " +
                    report.normalizedCandidateCount
        )
        println(
            "Merged candidates      : " +
                    report.mergedCandidateCount
        )
        println(
            "Conflicts              : " +
                    report.conflictCount
        )
        println(
            "Server artifacts       : " +
                    report.artifactCount
        )
        println(
            "OFF Nutrition Aggregates : " +
                    report.offNutritionAggregateFile
        )
        println()

        report.artifacts
            .forEach { artifact ->

                println(
                    artifact.artifact
                        .padEnd(30) +
                            artifact.entryCount
                                .toString()
                                .padStart(8)
                )
            }

        println()
        println(
            "Report                 : " +
                    reportFile.path
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    private fun sha256(
        bytes: ByteArray
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256"
            )
            .digest(
                bytes
            )
            .joinToString(
                separator = ""
            ) { byte ->
                "%02x".format(
                    byte.toInt() and 0xff
                )
            }

    companion object {

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        private val EXPECTED_SERVER_ARTIFACTS =
            setOf(
                "allergens.json",
                "animal_welfare.json",
                "biodiversity.json",
                "diet_classification.json",
                "environmental_impact.json",
                "fairtrade.json",
                "food_miles.json",
                "food_taxonomy.json",
                "ingredient_graph.json",
                "ingredients.json",
                "locality.json",
                "nutri_score.json",
                "nutrition.json",
                "packaging.json",
                "pesticides.json",
                "pollinator.json",
                "processing.json",
                "production.json",
                "recipe_graph.json",
                "recipes.json",
                "seasonality.json",
                "water_footprint.json",
                "water_stress.json"
            )
    }
}