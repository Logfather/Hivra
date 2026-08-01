package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.ResultingNutritionKnowledgeValidationReportWriter
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.ResultingNutritionKnowledgeValidator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ValidateResultingNutritionKnowledgeTest {

    @Test
    fun validateResultingNutritionKnowledge() {

        val projectRoot =
            resolveProjectRoot()

        val nutritionFile =
            projectRoot.resolve(
                "data/generated/knowledge/server/nutrition.json"
            )
                .canonicalFile

        val reportFile =
            projectRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "resulting-nutrition-knowledge-validation.json"
            )
                .canonicalFile

        val result =
            ResultingNutritionKnowledgeValidator(
                maximumReportedWarnings =
                    250,
                maximumReportedErrors =
                    250
            )
                .validate(
                    inputFile =
                        nutritionFile
                )

        ResultingNutritionKnowledgeValidationReportWriter()
            .write(
                result =
                    result,
                outputFile =
                    reportFile
            )

        assertTrue(
            reportFile.isFile,
            "Nutrition validation report was not generated: " +
                    reportFile.absolutePath
        )

        assertTrue(
            result.entryCount > 0L,
            "Resulting nutrition knowledge contains no entries."
        )

        assertTrue(
            result.isValid,
            buildString {
                appendLine(
                    "Resulting nutrition knowledge is invalid."
                )
                appendLine(
                    "Entries=${result.entryCount}"
                )
                appendLine(
                    "Warnings=${result.warningCount}"
                )
                appendLine(
                    "Errors=${result.errorCount}"
                )
                appendLine(
                    "Rejected=${result.rejectedEntryCount}"
                )
                append(
                    "Report=${reportFile.absolutePath}"
                )
            }
        )
    }

    private fun resolveProjectRoot(): File {

        val currentDirectory =
            File(".").canonicalFile

        return when {
            currentDirectory.name == "app" ->
                requireNotNull(
                    currentDirectory.parentFile
                )
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
}