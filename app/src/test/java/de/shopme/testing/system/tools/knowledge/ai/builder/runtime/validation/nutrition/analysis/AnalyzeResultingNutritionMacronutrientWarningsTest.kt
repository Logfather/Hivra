package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.analysis

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.analysis.ResultingNutritionMacronutrientWarningAnalysisReportWriter
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.analysis.ResultingNutritionMacronutrientWarningAnalyzer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyzeResultingNutritionMacronutrientWarningsTest {

    @Test
    fun analyzeResultingNutritionMacronutrientWarnings() {

        val projectRoot =
            resolveProjectRoot()

        val nutritionFile =
            projectRoot.resolve(
                "data/generated/knowledge/server/nutrition.json"
            )
                .canonicalFile

        val validationReportFile =
            projectRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "resulting-nutrition-knowledge-validation.json"
            )
                .canonicalFile

        val analysisReportFile =
            projectRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "resulting-nutrition-macronutrient-warning-analysis.json"
            )
                .canonicalFile

        require(nutritionFile.isFile) {
            "Resulting nutrition artifact does not exist: " +
                    nutritionFile.absolutePath
        }

        require(validationReportFile.isFile) {
            "Resulting nutrition validation report does not exist: " +
                    validationReportFile.absolutePath
        }

        val expectedWarningCount =
            readExpectedWarningCount(
                validationReportFile =
                    validationReportFile
            )

        val analysis =
            ResultingNutritionMacronutrientWarningAnalyzer(
                maximumReportedExamples =
                    250
            )
                .analyze(
                    inputFile =
                        nutritionFile
                )

        ResultingNutritionMacronutrientWarningAnalysisReportWriter()
            .write(
                analysis =
                    analysis,
                outputFile =
                    analysisReportFile
            )

        assertTrue(
            analysisReportFile.isFile,
            "Macronutrient warning analysis report was not generated: " +
                    analysisReportFile.absolutePath
        )

        assertEquals(
            expectedWarningCount,
            analysis.warningEntryCount,
            "Macronutrient warning analysis must cover every warning " +
                    "reported by the resulting Nutrition validator."
        )

        assertEquals(
            analysis.warningEntryCount,
            analysis.fiberDoubleCountingCandidateCount +
                    analysis.coreMacronutrientExcessCount
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("MACRONUTRIENT WARNING ANALYSIS COMPLETE")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
        println("Entries=${analysis.entryCount}")
        println("Warnings=${analysis.warningEntryCount}")
        println(
            "Fiber double-counting candidates=" +
                    analysis.fiberDoubleCountingCandidateCount
        )
        println(
            "Core macronutrient excess=" +
                    analysis.coreMacronutrientExcessCount
        )
        println(
            "Maximum macronutrient sum=" +
                    analysis.maximumMacronutrientSum
        )
        println(
            "Maximum excess grams=" +
                    analysis.maximumExcessGrams
        )
        println(
            "Report=" +
                    analysisReportFile.absolutePath
        )
        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun readExpectedWarningCount(
        validationReportFile: File
    ): Long {

        val report =
            JsonParser
                .parseString(
                    validationReportFile.readText()
                )
                .asJsonObject

        val countsByReason =
            report
                .getAsJsonObject(
                    COUNTS_BY_REASON_PROPERTY_NAME
                )

        return countsByReason
            ?.get(
                MACRONUTRIENT_WARNING_REASON
            )
            ?.asLong
            ?: 0L
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

    private companion object {

        const val COUNTS_BY_REASON_PROPERTY_NAME =
            "countsByReason"

        const val MACRONUTRIENT_WARNING_REASON =
            "MACRONUTRIENT_SUM_EXCEEDS_MAXIMUM"
    }
}