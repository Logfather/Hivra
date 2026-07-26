package de.shopme.testing.system.tools.knowledge.mapping.catalog

import de.shopme.tools.knowledge.mapping.catalog.DefaultCatalogKnowledgeMatchRequestGenerator
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultCatalogKnowledgeMatchRequestGeneratorFallbackTest {

    @Test
    fun generateFallbackCandidatesForUnmatchedEntryWithoutCandidates() {

        val directory =
            createTempDirectory(
                prefix =
                    "nutrition-request-fallback-"
            )
                .toFile()

        try {
            val matchReportFile =
                File(
                    directory,
                    "nutrition.matches.json"
                )
                    .apply {
                        writeText(
                            """
                            {
                              "artifactName": "nutrition.json",
                              "unmatched": [
                                {
                                  "catalogKey": "mace",
                                  "nearestCandidates": []
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                    }

            val serverArtifactFile =
                File(
                    directory,
                    "nutrition.json"
                )
                    .apply {
                        writeText(
                            """
                            {
                              "entries": {
                                "apple raw": {},
                                "mace ground": {},
                                "nutmeg ground": {},
                                "cinnamon ground": {},
                                "pepper black ground": {},
                                "clove ground": {}
                              }
                            }
                            """.trimIndent()
                        )
                    }

            val requests =
                DefaultCatalogKnowledgeMatchRequestGenerator(
                    serverArtifactFile =
                        serverArtifactFile
                )
                    .generate(
                        matchReportFile =
                            matchReportFile
                    )

            assertEquals(
                expected =
                    1,
                actual =
                    requests.requests.size
            )

            val request =
                requests.requests.single()

            assertEquals(
                expected =
                    "mace",
                actual =
                    request.catalogKey
            )

            assertEquals(
                expected =
                    "nutrition.json",
                actual =
                    request.serverArtifact
            )

            assertTrue(
                actual =
                    request.candidates.isNotEmpty()
            )

            assertEquals(
                expected =
                    "mace ground",
                actual =
                    request.candidates.first().serverKey
            )

            assertTrue(
                actual =
                    request.candidates.first().diagnosticScore >
                            0.0
            )

        } finally {
            directory.deleteRecursively()
        }
    }
}