package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictAnalyzer
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MeasureResultingNutritionConflictRateTest {

    @Test
    fun measureResultingNutritionConflictRate() {

        val projectRoot =
            resolveProjectRoot()

        val aggregateFile =
            projectRoot.resolve(
                "data/generated/knowledge/references/off/" +
                        "off-nutrition-reference-aggregates.json"
            )
                .canonicalFile

        val runtimeFile =
            projectRoot.resolve(
                "data/generated/knowledge/server/nutrition.json"
            )
                .canonicalFile

        val reportFile =
            projectRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "resulting-nutrition-conflict-rate.json"
            )
                .canonicalFile

        require(aggregateFile.isFile) {
            "OFF Nutrition aggregate dataset does not exist: " +
                    aggregateFile.absolutePath
        }

        require(runtimeFile.isFile) {
            "Resulting Nutrition runtime artifact does not exist: " +
                    runtimeFile.absolutePath
        }

        val analysis =
            ResultingNutritionConflictAnalyzer(
                maximumReportedConflictExamples =
                    250
            )
                .analyze(
                    aggregateFile =
                        aggregateFile,
                    runtimeFile =
                        runtimeFile
                )

        ResultingNutritionConflictReportWriter()
            .write(
                analysis =
                    analysis,
                outputFile =
                    reportFile
            )

        assertTrue(
            reportFile.isFile,
            "Nutrition conflict report was not generated: " +
                    reportFile.absolutePath
        )

        assertTrue(
            analysis.matchedEntryCount > 0L
        )

        assertTrue(
            analysis.comparableEntryCount > 0L
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("RESULTING NUTRITION CONFLICT RATE")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
        println(
            "Exact matches=" +
                    analysis.exactMatchedEntryCount
        )
        println(
            "Normalization-equivalent matches=" +
                    analysis.normalizationEquivalentMatchedEntryCount
        )
        println(
            "Comparable entries=" +
                    analysis.comparableEntryCount
        )
        println(
            "Conflict entries=" +
                    analysis.conflictEntryCount
        )
        println(
            "Entry conflict rate=" +
                    analysis.entryConflictRate
        )
        println(
            "Nutrient comparisons=" +
                    analysis.nutrientComparisonCount
        )
        println(
            "Nutrient conflicts=" +
                    analysis.nutrientConflictCount
        )
        println(
            "Nutrient conflict rate=" +
                    analysis.nutrientConflictRate
        )
        println(
            "Report=" +
                    reportFile.absolutePath
        )
        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
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