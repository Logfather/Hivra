package de.shopme.testing.system.tools.knowledge.rebuild.nutrition

import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeMappingPersistenceStep
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeMatchingStep
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildFiles
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildMatchingResult
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildMode
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildPersistenceResult
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildSnapshot
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildWorkflow
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRequestRebuildResult
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRequestRebuilder
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRuntimeRebuildStep
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeSnapshotReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutritionKnowledgeRebuildWorkflowTest {

    @Test
    fun rebuildNutritionKnowledgeOfflineDeterministically() {

        val directory =
            createTempDirectory(
                prefix =
                    "nutrition-rebuild-workflow-"
            )
                .toFile()

        try {
            var snapshotReadCount =
                0

            val snapshotReader =
                object : NutritionKnowledgeSnapshotReader {

                    override fun read(
                    ): NutritionKnowledgeRebuildSnapshot {

                        snapshotReadCount++

                        return if (snapshotReadCount == 1) {
                            NutritionKnowledgeRebuildSnapshot(
                                mappingCount =
                                    100,
                                catalogItemCount =
                                    200,
                                exactMatchCount =
                                    80,
                                mappedMatchCount =
                                    20,
                                runtimeEntryCount =
                                    100,
                                coveredCatalogItemCount =
                                    100,
                                missingCatalogItemCount =
                                    100,
                                coverage =
                                    0.5
                            )
                        } else {
                            NutritionKnowledgeRebuildSnapshot(
                                mappingCount =
                                    120,
                                catalogItemCount =
                                    200,
                                exactMatchCount =
                                    80,
                                mappedMatchCount =
                                    40,
                                runtimeEntryCount =
                                    120,
                                coveredCatalogItemCount =
                                    120,
                                missingCatalogItemCount =
                                    80,
                                coverage =
                                    0.6
                            )
                        }
                    }
                }

            var runtimeRebuilt =
                false

            val resultFile =
                File(
                    directory,
                    "nutrition.rebuild-result.json"
                )

            val output =
                ByteArrayOutputStream()

            val result =
                NutritionKnowledgeRebuildWorkflow(
                    snapshotReader =
                        snapshotReader,
                    requestRebuilder =
                        object :
                            NutritionKnowledgeRequestRebuilder {

                            override fun rebuild():
                                    NutritionKnowledgeRequestRebuildResult =
                                NutritionKnowledgeRequestRebuildResult(
                                    requestCount = 100,
                                    requestFile = "requests.json"
                                )
                        },
                    matchingStep =
                        object :
                            NutritionKnowledgeMatchingStep {

                            override fun run(
                                mode:
                                NutritionKnowledgeRebuildMode
                            ): NutritionKnowledgeRebuildMatchingResult {

                                assertEquals(
                                    expected =
                                        NutritionKnowledgeRebuildMode.OFFLINE,
                                    actual =
                                        mode
                                )

                                return NutritionKnowledgeRebuildMatchingResult(
                                    requestCount = 100,
                                    previouslyCompletedCount = 0,
                                    processedCount = 100,
                                    localModelDecisionCount = 20,
                                    chatGptDecisionCount = 0,
                                    gptFallbackRequiredCount = 80,
                                    matchCount = 20,
                                    noMatchCount = 0,
                                    errorCount = 0
                                )
                            }
                        },
                    mappingPersistenceStep =
                        object :
                            NutritionKnowledgeMappingPersistenceStep {

                            override fun run():
                                    NutritionKnowledgeRebuildPersistenceResult =
                                NutritionKnowledgeRebuildPersistenceResult(
                                    existingMappingCount = 100,
                                    addedMappingCount = 20,
                                    unchangedMappingCount = 0,
                                    conflictCount = 0,
                                    finalMappingCount = 120,
                                    removedMappingCount =
                                        0,
                                )
                        },
                    runtimeRebuildStep =
                        object :
                            NutritionKnowledgeRuntimeRebuildStep {

                            override fun run() {
                                runtimeRebuilt = true
                            }
                        },
                    files =
                        NutritionKnowledgeRebuildFiles(
                            catalogFile = "catalog.json",
                            nutritionArtifactFile = "nutrition.json",
                            requestFile = "requests.json",
                            decisionFile = "decisions.json",
                            validationFile = "validations.json",
                            mappingFile = "mappings.json",
                            resultFile = resultFile.path
                        ),
                    resultFile =
                        resultFile,
                    output =
                        PrintStream(output)
                )
                    .run(
                        mode =
                            NutritionKnowledgeRebuildMode.OFFLINE
                    )

            assertTrue(runtimeRebuilt)
            assertTrue(resultFile.isFile)

            assertEquals(
                expected = 20,
                actual =
                    result.delta.mappingCount
            )

            assertEquals(
                expected = 20,
                actual =
                    result.matching.localModelDecisionCount
            )

            assertEquals(
                expected = 80,
                actual =
                    result.matching.gptFallbackRequiredCount
            )

            assertEquals(
                expected = 0,
                actual =
                    result.matching.chatGptDecisionCount
            )

            assertEquals(
                expected = 20,
                actual =
                    result.delta.coveredCatalogItemCount
            )

            assertEquals(
                expected = -20,
                actual =
                    result.delta.missingCatalogItemCount
            )

        } finally {
            directory.deleteRecursively()
        }
    }
}