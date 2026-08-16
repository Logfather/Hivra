package de.shopme.tools.knowledge.ai.builder.runtime.partition

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.Closeable
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.PriorityQueue

/**
 * Führt partitionsweise erzeugte Runtime-Knowledge-Artefakte zu den
 * finalen Runtime-Artefakten zusammen.
 *
 * Erwartete Shard-Struktur:
 *
 * shardRootDirectory/
 *   partition-0000/
 *     nutrition.json
 *     ingredients.json
 *   partition-0001/
 *     nutrition.json
 *     ...
 *
 * Erwartetes Artefaktformat:
 *
 * {
 *   "version": 1,
 *   "entries": {
 *     "canonical-key": {
 *       ...
 *     }
 *   }
 * }
 *
 * Der Merge ist memory-bounded:
 *
 * - Pro Shard wird nur der aktuelle Eintrag gehalten.
 * - Alle Shards eines Artefakttyps werden über einen k-way merge
 *   zusammengeführt.
 * - Es wird keine globale entries-Map aufgebaut.
 */
class PartitionedRuntimeKnowledgeArtifactShardMerger(
    private val shardRootDirectory: File,
    private val outputDirectory: File,
    private val artifactFileNames: List<String> =
        DEFAULT_ARTIFACT_FILE_NAMES
) {

    fun merge():
            PartitionedRuntimeKnowledgeArtifactShardMergeResult {

        require(shardRootDirectory.isDirectory) {
            "Runtime artifact shard directory does not exist: " +
                    shardRootDirectory.path
        }

        require(
            outputDirectory.mkdirs() ||
                    outputDirectory.isDirectory
        ) {
            "Could not create runtime artifact output directory: " +
                    outputDirectory.path
        }

        val mergedArtifacts =
            linkedMapOf<String, PartitionedRuntimeKnowledgeArtifactMergeEntry>()

        artifactFileNames
            .distinct()
            .sorted()
            .forEach { artifactFileName ->

                val shardFiles =
                    findShardFiles(
                        artifactFileName =
                            artifactFileName
                    )

                if (shardFiles.isEmpty()) {
                    return@forEach
                }

                val outputFile =
                    outputDirectory.resolve(
                        artifactFileName
                    )

                val mergeEntry =
                    mergeArtifact(
                        artifactFileName =
                            artifactFileName,
                        shardFiles =
                            shardFiles,
                        outputFile =
                            outputFile
                    )

                mergedArtifacts[artifactFileName] =
                    mergeEntry
            }

        return PartitionedRuntimeKnowledgeArtifactShardMergeResult(
            artifacts =
                mergedArtifacts.toMap()
        )
    }

    private fun findShardFiles(
        artifactFileName: String
    ): List<File> {

        return shardRootDirectory
            .walkTopDown()
            .filter { file ->
                file.isFile &&
                        file.name ==
                        artifactFileName
            }
            .sortedBy { file ->
                file.relativeTo(
                    shardRootDirectory
                ).invariantSeparatorsPath
            }
            .toList()
    }

    private fun mergeArtifact(
        artifactFileName: String,
        shardFiles: List<File>,
        outputFile: File
    ): PartitionedRuntimeKnowledgeArtifactMergeEntry {

        require(shardFiles.isNotEmpty()) {
            "At least one shard file is required for artifact: " +
                    artifactFileName
        }

        outputFile.parentFile?.let { parentDirectory ->
            require(
                parentDirectory.mkdirs() ||
                        parentDirectory.isDirectory
            ) {
                "Could not create output directory for artifact: " +
                        parentDirectory.path
            }
        }

        val temporaryFile =
            outputFile.resolveSibling(
                outputFile.name +
                        TEMPORARY_FILE_SUFFIX
            )

        if (temporaryFile.exists()) {
            require(
                temporaryFile.delete()
            ) {
                "Could not delete previous temporary artifact file: " +
                        temporaryFile.path
            }
        }

        val cursors =
            mutableListOf<RuntimeKnowledgeArtifactShardCursor>()

        try {
            shardFiles.forEach { shardFile ->
                val cursor =
                    RuntimeKnowledgeArtifactShardCursor(
                        file =
                            shardFile
                    )

                cursor.open()

                cursors +=
                    cursor
            }

            val priorityQueue =
                PriorityQueue(
                    compareBy<RuntimeKnowledgeArtifactShardCursor>(
                        { cursor ->
                            cursor.currentKey
                        },
                        { cursor ->
                            cursor.relativePath(
                                shardRootDirectory
                            )
                        }
                    )
                )

            cursors
                .filter { cursor ->
                    cursor.hasCurrentEntry
                }
                .forEach { cursor ->
                    priorityQueue +=
                        cursor
                }

            var entryCount =
                0L

            var previousKey: String? =
                null

            JsonWriter(
                temporaryFile
                    .outputStream()
                    .buffered()
                    .writer(
                        StandardCharsets.UTF_8
                    )
            )
                .use { writer ->

                    writer.setIndent(
                        JSON_INDENT
                    )

                    writer.beginObject()

                    writer.name(
                        ENTRIES_PROPERTY_NAME
                    )
                    writer.beginObject()

                    while (priorityQueue.isNotEmpty()) {
                        val cursor =
                            priorityQueue.remove()

                        val key =
                            cursor.currentKey

                        require(
                            key != previousKey
                        ) {
                            "Duplicate runtime artifact key '$key' " +
                                    "while merging $artifactFileName. " +
                                    "Current shard=${cursor.file.path}"
                        }

                        writer.name(
                            key
                        )

                        GSON.toJson(
                            cursor.currentValue,
                            writer
                        )

                        previousKey =
                            key

                        entryCount++

                        cursor.advance()

                        if (cursor.hasCurrentEntry) {
                            priorityQueue +=
                                cursor
                        }
                    }

                    writer.endObject()

                    /*
                     * Nach vollständigem Entry-Merge wurden alle Root-Metadaten
                     * sämtlicher Shards gelesen.
                     */
                    val expectedMetadata =
                        cursors
                            .first()
                            .metadata
                            .toSortedMap()

                    cursors
                        .drop(1)
                        .forEach { cursor ->
                            require(
                                cursor.metadata.toSortedMap() ==
                                        expectedMetadata
                            ) {
                                "Runtime artifact shard metadata differs for " +
                                        "$artifactFileName. " +
                                        "Reference=${cursors.first().file.path}, " +
                                        "current=${cursor.file.path}, " +
                                        "referenceMetadata=$expectedMetadata, " +
                                        "currentMetadata=${cursor.metadata.toSortedMap()}"
                            }
                        }

                    expectedMetadata.forEach {
                            (propertyName, propertyValue) ->

                        writer.name(
                            propertyName
                        )

                        GSON.toJson(
                            propertyValue,
                            writer
                        )
                    }

                    writer.endObject()
                }

            require(entryCount > 0L) {
                "Merged runtime knowledge artifact must not be empty: " +
                        artifactFileName
            }

            replaceAtomically(
                temporaryFile =
                    temporaryFile,
                outputFile =
                    outputFile
            )

            println(
                "Runtime artifact shards merged " +
                        "artifact=" +
                        artifactFileName +
                        " shards=" +
                        shardFiles.size +
                        " entries=" +
                        entryCount +
                        " output=" +
                        outputFile.path
            )

            val mergedMetadata =
                cursors
                    .first()
                    .metadata
                    .toSortedMap()

            return PartitionedRuntimeKnowledgeArtifactMergeEntry(
                artifactFileName =
                    artifactFileName,
                outputFile =
                    outputFile,
                shardCount =
                    shardFiles.size,
                entryCount =
                    entryCount,
                metadata =
                    mergedMetadata
            )

        } catch (throwable: Throwable) {
            temporaryFile.delete()

            throw throwable
        } finally {
            cursors.forEach { cursor ->
                runCatching {
                    cursor.close()
                }
            }
        }
    }

    private fun replaceAtomically(
        temporaryFile: File,
        outputFile: File
    ) {
        require(temporaryFile.isFile) {
            "Temporary runtime artifact was not created: " +
                    temporaryFile.path
        }

        if (outputFile.exists()) {
            require(
                outputFile.delete()
            ) {
                "Could not delete previous runtime artifact: " +
                        outputFile.path
            }
        }

        require(
            temporaryFile.renameTo(
                outputFile
            )
        ) {
            "Could not move temporary runtime artifact " +
                    "${temporaryFile.path} to ${outputFile.path}"
        }
    }

    private class RuntimeKnowledgeArtifactShardCursor(
        val file: File
    ) : Closeable {

        private var reader: JsonReader? =
            null

        private var insideEntries =
            false

        private var rootFinished =
            false

        val metadata =
            linkedMapOf<String, JsonElement>()

        var currentKey: String =
            ""
            private set

        var currentValue: JsonElement =
            GSON.toJsonTree(
                emptyMap<String, Any>()
            )
            private set

        var hasCurrentEntry: Boolean =
            false
            private set

        fun open() {
            check(reader == null) {
                "Runtime artifact shard cursor is already open: " +
                        file.path
            }

            require(file.isFile) {
                "Runtime artifact shard does not exist: " +
                        file.path
            }

            val jsonReader =
                JsonReader(
                    file
                        .inputStream()
                        .buffered()
                        .reader(
                            StandardCharsets.UTF_8
                        )
                )

            reader =
                jsonReader

            jsonReader.beginObject()

            var entriesFound =
                false

            while (jsonReader.hasNext()) {
                val propertyName =
                    jsonReader.nextName()

                if (propertyName == ENTRIES_PROPERTY_NAME) {
                    jsonReader.beginObject()

                    insideEntries =
                        true

                    entriesFound =
                        true

                    advance()

                    break
                }

                metadata[propertyName] =
                    GSON.fromJson(
                        jsonReader,
                        JsonElement::class.java
                    )
            }

            require(entriesFound) {
                "Runtime artifact shard has no entries object: " +
                        file.path
            }
        }

        fun advance() {
            val jsonReader =
                checkNotNull(reader) {
                    "Runtime artifact shard cursor is not open: " +
                            file.path
                }

            check(insideEntries) {
                "Runtime artifact shard cursor is not inside entries: " +
                        file.path
            }

            if (jsonReader.hasNext()) {
                currentKey =
                    jsonReader.nextName()

                require(currentKey.isNotBlank()) {
                    "Runtime artifact shard contains a blank entry key: " +
                            file.path
                }

                currentValue =
                    GSON.fromJson(
                        jsonReader,
                        JsonElement::class.java
                    )

                hasCurrentEntry =
                    true

                return
            }

            jsonReader.endObject()

            insideEntries =
                false

            hasCurrentEntry =
                false

            currentKey =
                ""

            readRemainingRootProperties(
                jsonReader =
                    jsonReader
            )
        }

        private fun readRemainingRootProperties(
            jsonReader: JsonReader
        ) {
            while (jsonReader.hasNext()) {
                val propertyName =
                    jsonReader.nextName()

                require(
                    propertyName != ENTRIES_PROPERTY_NAME
                ) {
                    "Runtime artifact shard contains multiple entries objects: " +
                            file.path
                }

                require(
                    propertyName !in metadata
                ) {
                    "Runtime artifact shard contains duplicate root property " +
                            "'$propertyName': ${file.path}"
                }

                metadata[propertyName] =
                    GSON.fromJson(
                        jsonReader,
                        JsonElement::class.java
                    )
            }

            jsonReader.endObject()

            rootFinished =
                true
        }

        fun relativePath(
            rootDirectory: File
        ): String =
            file.relativeTo(
                rootDirectory
            ).invariantSeparatorsPath

        override fun close() {
            val jsonReader =
                reader
                    ?: return

            try {
                if (!rootFinished) {
                    runCatching {
                        if (insideEntries) {
                            while (jsonReader.hasNext()) {
                                jsonReader.nextName()
                                jsonReader.skipValue()
                            }

                            jsonReader.endObject()

                            insideEntries =
                                false
                        }

                        while (jsonReader.hasNext()) {
                            jsonReader.nextName()
                            jsonReader.skipValue()
                        }

                        jsonReader.endObject()
                    }
                }
            } finally {
                jsonReader.close()

                reader =
                    null

                insideEntries =
                    false

                hasCurrentEntry =
                    false
            }
        }
    }

    private companion object {

        const val ENTRIES_PROPERTY_NAME =
            "entries"

        const val TEMPORARY_FILE_SUFFIX =
            ".tmp"

        const val JSON_INDENT =
            "  "

        val GSON =
            Gson()

        val DEFAULT_ARTIFACT_FILE_NAMES =
            listOf(
                "nutrition.json",
                "environmental_impact.json",
                "allergens.json",
                "food_taxonomy.json",
                "processing.json",
                "water_footprint.json",
                "water_stress.json",
                "pesticides.json",
                "food_miles.json",
                "nutri_score.json",
                "diet_classification.json",
                "animal_welfare.json"
            )
    }
}

data class PartitionedRuntimeKnowledgeArtifactShardMergeResult(
    val artifacts:
    Map<String, PartitionedRuntimeKnowledgeArtifactMergeEntry>
) {

    val artifactCount: Int
        get() =
            artifacts.size

    val totalShardCount: Int
        get() =
            artifacts.values.sumOf { artifact ->
                artifact.shardCount
            }

    val totalEntryCount: Long
        get() =
            artifacts.values.sumOf { artifact ->
                artifact.entryCount
            }

    fun artifact(
        fileName: String
    ): PartitionedRuntimeKnowledgeArtifactMergeEntry? =
        artifacts[fileName]

    fun outputFileOrDefault(
        fileName: String,
        outputDirectory: File
    ): File =
        artifacts[fileName]
            ?.outputFile
            ?: outputDirectory.resolve(
                fileName
            )
}

data class PartitionedRuntimeKnowledgeArtifactMergeEntry(
    val artifactFileName: String,
    val outputFile: File,
    val shardCount: Int,
    val entryCount: Long,
    val metadata: Map<String, JsonElement>
)