package de.shopme.testing.system.tools.knowledge.mapping.catalog

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import de.shopme.testing.system.tools.knowledge.report.CatalogJsonFileReader
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMapping
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMappingContract
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMappingReport
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMappingStrategy
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMappingWriter
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMappings
import de.shopme.tools.knowledge.mapping.catalog.DefaultCatalogKnowledgeMappingBuilder
import de.shopme.tools.report.CatalogKnowledgeKeyExtractor
import java.io.File
import java.io.FileReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistExactCatalogKnowledgeMappingsTest {

    @Test
    fun persistExactCatalogKnowledgeMappings() {

        val paths =
            KnowledgeBuildPaths.default()

        paths.ensureBuildDirectories()

        val catalogFile =
            paths.canonicalFoodCatalog

        val serverDirectory =
            paths.serverRoot

        require(catalogFile.isFile) {
            "Canonical catalog does not exist: " +
                    catalogFile.absolutePath
        }

        require(serverDirectory.isDirectory) {
            "Server knowledge directory does not exist: " +
                    serverDirectory.absolutePath
        }

        val catalogKeys =
            CatalogKnowledgeKeyExtractor()
                .extract(
                    CatalogJsonFileReader()
                        .read(catalogFile)
                )

        assertTrue(
            catalogKeys.isNotEmpty(),
            "No catalog knowledge keys found"
        )

        val builder =
            DefaultCatalogKnowledgeMappingBuilder()

        val writer =
            CatalogKnowledgeMappingWriter()

        val serverArtifacts =
            serverDirectory
                .listFiles()
                .orEmpty()
                .asSequence()
                .filter(File::isFile)
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

        assertTrue(
            serverArtifacts.isNotEmpty(),
            "No server knowledge artifacts found in " +
                    serverDirectory.absolutePath
        )

        serverArtifacts.forEach { artifactFile ->

            val serverKeys =
                readServerKeys(
                    artifactFile = artifactFile
                )

            val mappings =
                builder.build(
                    catalogKeys =
                        catalogKeys,
                    artifactName =
                        artifactFile.name,
                    serverKeys =
                        serverKeys.asSequence()
                )

            val artifact =
                CatalogKnowledgeMappings(
                    version =
                        CatalogKnowledgeMappingContract
                            .CURRENT_VERSION,
                    mappings =
                        mappings
                )

            val outputFile =
                paths.mappingArtifact(
                    "${artifactFile.nameWithoutExtension}.mappings.json"
                )

            writer.write(
                mappings =
                    artifact,
                file =
                    outputFile
            )

            verifyWrittenMappings(
                outputFile =
                    outputFile,
                expectedArtifactName =
                    artifactFile.name,
                expectedMappings =
                    mappings
            )

            CatalogKnowledgeMappingReport(
                artifactName =
                    artifactFile.name,
                catalogKeyCount =
                    catalogKeys.size,
                serverKeyCount =
                    serverKeys.size.toLong(),
                exactMappingCount =
                    mappings.size,
                unmatchedCatalogKeyCount =
                    catalogKeys.size -
                            mappings.size,
                outputFile =
                    outputFile.path
            ).printTo()
        }
    }

    private fun readServerKeys(
        artifactFile: File
    ): Set<String> {

        val keys =
            linkedSetOf<String>()

        JsonReader(
            FileReader(
                artifactFile
            )
        ).use { reader ->

            reader.beginObject()

            while (reader.hasNext()) {

                when (reader.nextName()) {

                    "entries" ->
                        readEntryKeys(
                            reader = reader,
                            destination = keys
                        )

                    else ->
                        reader.skipValue()
                }
            }

            reader.endObject()
        }

        return keys
    }

    private fun readEntryKeys(
        reader: JsonReader,
        destination: MutableSet<String>
    ) {

        require(
            reader.peek() ==
                    JsonToken.BEGIN_OBJECT
        ) {
            "Expected 'entries' to be a JSON object"
        }

        reader.beginObject()

        while (reader.hasNext()) {

            val key =
                reader
                    .nextName()
                    .trim()

            if (key.isNotBlank()) {
                destination += key
            }

            reader.skipValue()
        }

        reader.endObject()
    }

    private fun verifyWrittenMappings(
        outputFile: File,
        expectedArtifactName: String,
        expectedMappings:
        List<CatalogKnowledgeMapping>
    ) {

        assertTrue(
            outputFile.isFile,
            "Mapping file was not written: " +
                    outputFile.absolutePath
        )

        val root =
            JsonParser
                .parseString(
                    outputFile.readText()
                )
                .asJsonObject

        assertEquals(
            CatalogKnowledgeMappingContract
                .CURRENT_VERSION,
            root["version"].asInt
        )

        val writtenMappings =
            root["mappings"]
                .asJsonArray
                .map { element ->
                    Gson().fromJson(
                        element,
                        CatalogKnowledgeMapping::class.java
                    )
                }

        assertEquals(
            expectedMappings.size,
            writtenMappings.size
        )

        assertEquals(
            expectedMappings,
            writtenMappings
        )

        writtenMappings.forEach { mapping ->

            assertEquals(
                expectedArtifactName,
                mapping.serverArtifact
            )

            assertEquals(
                CatalogKnowledgeMappingStrategy.EXACT,
                mapping.strategy
            )

            assertEquals(
                1.0,
                mapping.confidence
            )

            assertEquals(
                mapping.catalogKey,
                mapping.serverKey
            )
        }

        assertEquals(
            writtenMappings.sortedWith(
                MAPPING_ORDER
            ),
            writtenMappings,
            "Mappings must be written deterministically"
        )
    }

    companion object {

        private val MAPPING_ORDER =
            compareBy<CatalogKnowledgeMapping>(
                CatalogKnowledgeMapping::catalogKey
            ).thenBy(
                CatalogKnowledgeMapping::serverArtifact
            ).thenBy(
                CatalogKnowledgeMapping::serverKey
            )
    }
}