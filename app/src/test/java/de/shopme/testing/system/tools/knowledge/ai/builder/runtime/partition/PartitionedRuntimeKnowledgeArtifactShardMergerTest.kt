package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.partition

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.ai.builder.runtime.partition.PartitionedRuntimeKnowledgeArtifactShardMerger
import org.junit.Assert
import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartitionedRuntimeKnowledgeArtifactShardMergerTest {

    @Test
    fun merge_combinesShardEntriesInDeterministicKeyOrder() {
        val rootDirectory =
            Files.createTempDirectory(
                "runtime-artifact-shard-merger-test"
            ).toFile()

        try {
            val shardDirectory =
                rootDirectory.resolve(
                    "shards"
                )

            val outputDirectory =
                rootDirectory.resolve(
                    "output"
                )

            val partition0 =
                shardDirectory.resolve(
                    "partition-0000"
                )

            val partition1 =
                shardDirectory.resolve(
                    "partition-0001"
                )

            require(partition0.mkdirs())
            require(partition1.mkdirs())

            partition0
                .resolve(
                    "nutrition.json"
                )
                .writeText(
                    """
                    {
                      "version": 1,
                      "entries": {
                        "apple": {
                          "value": 1
                        },
                        "pear": {
                          "value": 3
                        }
                      }
                    }
                    """.trimIndent()
                )

            partition1
                .resolve(
                    "nutrition.json"
                )
                .writeText(
                    """
                    {
                      "version": 1,
                      "entries": {
                        "banana": {
                          "value": 2
                        },
                        "plum": {
                          "value": 4
                        }
                      }
                    }
                    """.trimIndent()
                )

            val result =
                PartitionedRuntimeKnowledgeArtifactShardMerger(
                    shardRootDirectory =
                        shardDirectory,
                    outputDirectory =
                        outputDirectory,
                    artifactFileNames =
                        listOf(
                            "nutrition.json"
                        )
                )
                    .merge()

            Assert.assertEquals(
                1,
                result.artifactCount
            )

            val artifact =
                result.artifact(
                    "nutrition.json"
                )

            requireNotNull(
                artifact
            )

            Assert.assertEquals(
                2,
                artifact.shardCount
            )

            Assert.assertEquals(
                4L,
                artifact.entryCount
            )

            Assert.assertTrue(
                artifact.outputFile.isFile
            )

            val json =
                JsonParser
                    .parseString(
                        artifact.outputFile.readText()
                    )
                    .asJsonObject

            Assert.assertEquals(
                1,
                json["version"].asInt
            )

            val keys =
                json["entries"]
                    .asJsonObject
                    .keySet()
                    .toList()

            Assert.assertEquals(
                listOf(
                    "apple",
                    "banana",
                    "pear",
                    "plum"
                ),
                keys
            )
        } finally {
            rootDirectory.deleteRecursively()
        }
    }

    @Test
    fun merge_ignoresMissingArtifactShards() {
        val rootDirectory =
            Files.createTempDirectory(
                "runtime-artifact-empty-shard-test"
            ).toFile()

        try {
            val shardDirectory =
                rootDirectory.resolve(
                    "shards"
                )

            val outputDirectory =
                rootDirectory.resolve(
                    "output"
                )

            require(shardDirectory.mkdirs())

            val result =
                PartitionedRuntimeKnowledgeArtifactShardMerger(
                    shardRootDirectory =
                        shardDirectory,
                    outputDirectory =
                        outputDirectory,
                    artifactFileNames =
                        listOf(
                            "fairtrade.json"
                        )
                )
                    .merge()

            Assert.assertEquals(
                0,
                result.artifactCount
            )

            Assert.assertFalse(
                outputDirectory
                    .resolve(
                        "fairtrade.json"
                    )
                    .exists()
            )
        } finally {
            rootDirectory.deleteRecursively()
        }
    }

    @Test(
        expected =
            IllegalArgumentException::class
    )
    fun merge_rejectsDuplicateKeysAcrossShards() {
        val rootDirectory =
            Files.createTempDirectory(
                "runtime-artifact-duplicate-test"
            ).toFile()

        try {
            val shardDirectory =
                rootDirectory.resolve(
                    "shards"
                )

            val outputDirectory =
                rootDirectory.resolve(
                    "output"
                )

            val partition0 =
                shardDirectory.resolve(
                    "partition-0000"
                )

            val partition1 =
                shardDirectory.resolve(
                    "partition-0001"
                )

            require(partition0.mkdirs())
            require(partition1.mkdirs())

            val artifact =
                """
                {
                  "version": 1,
                  "entries": {
                    "apple": {
                      "value": 1
                    }
                  }
                }
                """.trimIndent()

            partition0
                .resolve(
                    "nutrition.json"
                )
                .writeText(
                    artifact
                )

            partition1
                .resolve(
                    "nutrition.json"
                )
                .writeText(
                    artifact
                )

            PartitionedRuntimeKnowledgeArtifactShardMerger(
                shardRootDirectory =
                    shardDirectory,
                outputDirectory =
                    outputDirectory,
                artifactFileNames =
                    listOf(
                        "nutrition.json"
                    )
            )
                .merge()
        } finally {
            rootDirectory.deleteRecursively()
        }
    }

    @Test
    fun merge_acceptsEntriesBeforeVersion() {
        val rootDirectory =
            Files.createTempDirectory(
                "runtime-artifact-field-order-test"
            ).toFile()

        try {
            val shardDirectory =
                rootDirectory.resolve(
                    "shards"
                )

            val outputDirectory =
                rootDirectory.resolve(
                    "output"
                )

            val partitionDirectory =
                shardDirectory.resolve(
                    "partition-0000"
                )

            require(
                partitionDirectory.mkdirs()
            )

            partitionDirectory
                .resolve(
                    "allergens.json"
                )
                .writeText(
                    """
                {
                  "entries": {
                    "apple": {
                      "value": 1
                    }
                  },
                  "version": 1
                }
                """.trimIndent()
                )

            val result =
                PartitionedRuntimeKnowledgeArtifactShardMerger(
                    shardRootDirectory =
                        shardDirectory,
                    outputDirectory =
                        outputDirectory,
                    artifactFileNames =
                        listOf(
                            "allergens.json"
                        )
                )
                    .merge()

            val artifact =
                requireNotNull(
                    result.artifact(
                        "allergens.json"
                    )
                )

            assertEquals(
                1L,
                artifact.entryCount
            )

            assertTrue(
                artifact.outputFile.isFile
            )
        } finally {
            rootDirectory.deleteRecursively()
        }
    }

    @Test
    fun merge_acceptsArtifactWithoutVersionMetadata() {
        val rootDirectory =
            Files.createTempDirectory(
                "runtime-artifact-without-version-test"
            ).toFile()

        try {
            val shardDirectory =
                rootDirectory.resolve("shards")

            val outputDirectory =
                rootDirectory.resolve("output")

            val partitionDirectory =
                shardDirectory.resolve("partition-0000")

            require(partitionDirectory.mkdirs())

            partitionDirectory
                .resolve("allergens.json")
                .writeText(
                    """
                {
                  "entries": {
                    "apple": {
                      "value": 1
                    }
                  }
                }
                """.trimIndent()
                )

            val result =
                PartitionedRuntimeKnowledgeArtifactShardMerger(
                    shardRootDirectory =
                        shardDirectory,
                    outputDirectory =
                        outputDirectory,
                    artifactFileNames =
                        listOf("allergens.json")
                )
                    .merge()

            val artifact =
                requireNotNull(
                    result.artifact("allergens.json")
                )

            assertEquals(
                1L,
                artifact.entryCount
            )

            assertTrue(
                artifact.metadata.isEmpty()
            )

            assertTrue(
                artifact.outputFile.isFile
            )
        } finally {
            rootDirectory.deleteRecursively()
        }
    }
}