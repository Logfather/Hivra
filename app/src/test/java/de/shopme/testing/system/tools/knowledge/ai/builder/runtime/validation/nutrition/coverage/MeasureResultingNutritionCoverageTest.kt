package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage.ResultingNutritionCoverageAnalyzer
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage.ResultingNutritionCoverageReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MeasureResultingNutritionCoverageTest {

    @Test
    fun measureResultingNutritionCoverage() {

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
                        "resulting-nutrition-coverage.json"
            )
                .canonicalFile

        require(aggregateFile.isFile) {
            "OFF Nutrition aggregate dataset does not exist: " +
                    aggregateFile.absolutePath
        }

        require(runtimeFile.isFile) {
            "Resulting Nutrition artifact does not exist: " +
                    runtimeFile.absolutePath
        }

        val analysis =
            ResultingNutritionCoverageAnalyzer(
                maximumReportedMissingRuntimeCanonicalIds =
                    250,
                maximumReportedAdditionalRuntimeCanonicalIds =
                    250
            )
                .analyze(
                    aggregateFile =
                        aggregateFile,
                    runtimeFile =
                        runtimeFile
                )

        ResultingNutritionCoverageReportWriter()
            .write(
                analysis =
                    analysis,
                outputFile =
                    reportFile
            )

        assertTrue(
            reportFile.isFile,
            "Nutrition coverage report was not generated: " +
                    reportFile.absolutePath
        )

        assertTrue(
            analysis.aggregateEntryCount > 0L,
            "Nutrition coverage analysis found no OFF aggregates."
        )

        assertTrue(
            analysis.runtimeEntryCount > 0L,
            "Nutrition coverage analysis found no runtime entries."
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("RESULTING NUTRITION COVERAGE")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
        println(
            "OFF aggregate entries=" +
                    analysis.aggregateEntryCount
        )
        println(
            "Runtime entries=" +
                    analysis.runtimeEntryCount
        )
        println(
            "Covered OFF aggregates=" +
                    analysis.coveredAggregateEntryCount
        )
        println(
            "Missing runtime entries=" +
                    analysis.missingRuntimeEntryCount
        )
        println(
            "Additional runtime entries=" +
                    analysis.additionalRuntimeEntryCount
        )
        println(
            "OFF aggregate coverage=" +
                    analysis.aggregateCoverageRate
        )
        println(
            "Complete=" +
                    analysis.isComplete
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