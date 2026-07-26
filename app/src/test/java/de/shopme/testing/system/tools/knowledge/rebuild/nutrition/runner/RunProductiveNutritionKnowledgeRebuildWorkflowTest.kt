package de.shopme.testing.system.tools.knowledge.rebuild.nutrition.runner

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildMode
import de.shopme.tools.knowledge.rebuild.nutrition.runner.NutritionKnowledgeRebuildEnvironment
import de.shopme.tools.knowledge.rebuild.nutrition.runner.NutritionKnowledgeRebuildProjectFiles
import de.shopme.tools.knowledge.rebuild.nutrition.runner.NutritionKnowledgeRebuildWorkflowFactory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunProductiveNutritionKnowledgeRebuildWorkflowTest {

    @Test
    fun runProductiveNutritionKnowledgeRebuild() {

        val environment =
            System.getenv()

        NutritionKnowledgeRebuildEnvironment
            .requireProductiveOpenAIEnabled(
                mode =
                    NutritionKnowledgeRebuildMode.PRODUCTIVE,
                environment =
                    environment
            )

        require(
            environment["OPENAI_API_KEY"]
                ?.isNotBlank() ==
                    true
        ) {
            "OPENAI_API_KEY is required for the productive " +
                    "nutrition knowledge rebuild."
        }

        val projectRoot =
            File("..")

        val files =
            NutritionKnowledgeRebuildProjectFiles
                .fromProjectRoot(
                    projectRoot =
                        projectRoot
                )

        val beforeMappingFileContent =
            files.outputMappingFile
                .takeIf {
                    it.isFile
                }
                ?.readText()

        try {
            val result =
                NutritionKnowledgeRebuildWorkflowFactory()
                    .create(
                        mode =
                            NutritionKnowledgeRebuildMode.PRODUCTIVE,
                        files =
                            files
                    )
                    .run(
                        mode =
                            NutritionKnowledgeRebuildMode.PRODUCTIVE
                    )

            assertEquals(
                expected =
                    NutritionKnowledgeRebuildMode.PRODUCTIVE,
                actual =
                    result.mode
            )

            assertTrue(
                actual =
                    files.rebuildResultFile.isFile,
                message =
                    "Nutrition rebuild report was not created: " +
                            files.rebuildResultFile.absolutePath
            )

            assertTrue(
                actual =
                    files.runtimeNutritionFile.isFile,
                message =
                    "Runtime nutrition artifact was not created: " +
                            files.runtimeNutritionFile.absolutePath
            )

            assertEquals(
                expected =
                    0,
                actual =
                    result.matching.gptFallbackRequiredCount
            )

            assertEquals(
                expected =
                    0,
                actual =
                    result.matching.errorCount
            )

            assertEquals(
                expected =
                    result.matching.processedCount,
                actual =
                    result.matching.localModelDecisionCount +
                            result.matching.chatGptDecisionCount +
                            result.matching.errorCount
            )

            assertEquals(
                expected =
                    result.matching.requestCount,
                actual =
                    result.matching.previouslyCompletedCount +
                            result.matching.processedCount
            )

            assertEquals(
                expected =
                    result.matching.processedCount,
                actual =
                    result.matching.localModelDecisionCount +
                            result.matching.chatGptDecisionCount
            )

            /*
             * Der produktive Rebuild bildet den aktuellen validierten
             * Request-, Decision- und Mapping-Bestand ab.
             *
             * Veraltete Mappings dürfen dabei entfernt werden.
             * Deshalb wird keine monotone Zunahme des Mapping-Bestands
             * oder der Coverage vorausgesetzt.
             */
            assertEquals(
                expected =
                    result.persistence.existingMappingCount +
                            result.persistence.addedMappingCount -
                            result.persistence.removedMappingCount,
                actual =
                    result.persistence.finalMappingCount
            )

            assertEquals(
                expected =
                    result.persistence.addedMappingCount -
                            result.persistence.removedMappingCount,
                actual =
                    result.delta.mappingCount
            )

            assertEquals(
                expected =
                    result.before.mappingCount +
                            result.delta.mappingCount,
                actual =
                    result.after.mappingCount
            )

            assertEquals(
                expected =
                    result.persistence.finalMappingCount,
                actual =
                    result.after.mappingCount
            )

            assertEquals(
                expected =
                    result.after.runtimeEntryCount,
                actual =
                    result.after.coveredCatalogItemCount
            )

            assertEquals(
                expected =
                    result.after.exactMatchCount +
                            result.after.mappedMatchCount,
                actual =
                    result.after.coveredCatalogItemCount
            )

            assertEquals(
                expected =
                    result.after.catalogItemCount -
                            result.after.coveredCatalogItemCount,
                actual =
                    result.after.missingCatalogItemCount
            )

            assertEquals(
                expected =
                    result.before.catalogItemCount,
                actual =
                    result.after.catalogItemCount
            )

            assertEquals(
                expected =
                    result.before.coveredCatalogItemCount +
                            result.delta.coveredCatalogItemCount,
                actual =
                    result.after.coveredCatalogItemCount
            )

            assertEquals(
                expected =
                    result.before.missingCatalogItemCount +
                            result.delta.missingCatalogItemCount,
                actual =
                    result.after.missingCatalogItemCount
            )

            assertEquals(
                expected =
                    0,
                actual =
                    result.delta.coveredCatalogItemCount +
                            result.delta.missingCatalogItemCount
            )

            val persisted =
                JsonParser.parseString(
                    files.rebuildResultFile.readText()
                )
                    .asJsonObject

            assertEquals(
                expected =
                    "PRODUCTIVE",
                actual =
                    persisted["mode"]
                        .asString
            )

            println()
            println(
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            )
            println(
                "PRODUCTIVE NUTRITION REBUILD RESULT"
            )
            println(
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            )
            println(
                "Requests                  : " +
                        result.matching.requestCount
            )
            println(
                "Previously completed      : " +
                        result.matching.previouslyCompletedCount
            )
            println(
                "Processed                 : " +
                        result.matching.processedCount
            )
            println(
                "LOCAL_MODEL decisions     : " +
                        result.matching.localModelDecisionCount
            )
            println(
                "CHAT_GPT decisions        : " +
                        result.matching.chatGptDecisionCount
            )
            println(
                "GPT fallback required     : " +
                        result.matching.gptFallbackRequiredCount
            )
            println(
                "Errors                    : " +
                        result.matching.errorCount
            )
            println()
            println(
                "Mappings before           : " +
                        result.before.mappingCount
            )
            println(
                "Mappings unchanged        : " +
                        result.persistence.unchangedMappingCount
            )
            println(
                "Mappings added            : " +
                        result.persistence.addedMappingCount
            )
            println(
                "Mappings removed          : " +
                        result.persistence.removedMappingCount
            )
            println(
                "Mappings after            : " +
                        result.after.mappingCount
            )
            println(
                "Mapping delta             : " +
                        formatSignedCount(
                            result.delta.mappingCount
                        )
            )
            println()
            println(
                "Covered before            : " +
                        result.before.coveredCatalogItemCount
            )
            println(
                "Covered after             : " +
                        result.after.coveredCatalogItemCount
            )
            println(
                "Covered delta             : " +
                        formatSignedCount(
                            result.delta.coveredCatalogItemCount
                        )
            )
            println()
            println(
                "Missing before            : " +
                        result.before.missingCatalogItemCount
            )
            println(
                "Missing after             : " +
                        result.after.missingCatalogItemCount
            )
            println(
                "Missing delta             : " +
                        formatSignedCount(
                            result.delta.missingCatalogItemCount
                        )
            )
            println()
            println(
                "Coverage before           : " +
                        result.before.coverage
            )
            println(
                "Coverage after            : " +
                        result.after.coverage
            )
            println(
                "Coverage delta            : " +
                        result.delta.coverage
            )
            println()
            println(
                "Report                    : " +
                        files.rebuildResultFile.path
            )
            println(
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            )

        } catch (
            throwable: Throwable
        ) {

            /*
             * Ein produktiver Lauf kann nach einem Teil der
             * OpenAI-Aufrufe abbrechen. Die Decision-Datei wird durch
             * den Runner fortlaufend persistiert und erlaubt daher
             * grundsätzlich eine Wiederaufnahme.
             *
             * Die Mapping-Datei wird bei einem Fehler auf ihren
             * vorherigen Stand zurückgesetzt.
             */
            restoreFile(
                file =
                    files.outputMappingFile,
                previousContent =
                    beforeMappingFileContent
            )

            throw throwable
        }
    }

    private fun restoreFile(
        file: File,
        previousContent: String?
    ) {
        if (previousContent == null) {

            if (file.exists()) {
                check(
                    file.delete()
                ) {
                    "Could not delete newly created file after " +
                            "failed productive rebuild: " +
                            file.absolutePath
                }
            }

            return
        }

        file.parentFile
            ?.let { directory ->

                if (!directory.exists()) {
                    check(
                        directory.mkdirs()
                    ) {
                        "Could not create restore directory: " +
                                directory.absolutePath
                    }
                }
            }

        file.writeText(
            previousContent
        )
    }

    private fun formatSignedCount(
        value: Int
    ): String {

        return String.format(
            java.util.Locale.ROOT,
            "%+d",
            value
        )
    }
}