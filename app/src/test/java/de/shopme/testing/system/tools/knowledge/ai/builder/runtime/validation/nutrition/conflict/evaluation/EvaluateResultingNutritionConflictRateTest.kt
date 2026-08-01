package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictAnalyzer
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictEvaluationReportWriter
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictEvaluator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvaluateResultingNutritionConflictRateTest {

    @Test
    fun evaluateResultingNutritionConflictRate() {

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
                        "resulting-nutrition-conflict-evaluation.json"
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

        /*
         * Für die Evaluation werden alle Konflikteinträge benötigt.
         *
         * Der aktuelle produktive Datensatz enthält 408 Konflikte.
         * Die Grenze von 100.000 hält ausreichend Reserve, ohne eine
         * unbegrenzte Speicherbelegung zu erlauben.
         */
        val conflictAnalysis =
            ResultingNutritionConflictAnalyzer(
                maximumReportedConflictExamples =
                    MAXIMUM_EVALUATED_CONFLICTS
            )
                .analyze(
                    aggregateFile =
                        aggregateFile,
                    runtimeFile =
                        runtimeFile
                )

        assertEquals(
            0L,
            conflictAnalysis.omittedConflictExampleCount,
            "Conflict evaluation did not receive every conflict entry."
        )

        val evaluation =
            ResultingNutritionConflictEvaluator(
                maximumReportedExamples =
                    250
            )
                .evaluate(
                    analysis =
                        conflictAnalysis
                )

        ResultingNutritionConflictEvaluationReportWriter()
            .write(
                evaluation =
                    evaluation,
                outputFile =
                    reportFile
            )

        assertTrue(
            reportFile.isFile,
            "Nutrition conflict evaluation report was not generated: " +
                    reportFile.absolutePath
        )

        assertEquals(
            conflictAnalysis.conflictEntryCount,
            evaluation.evaluatedConflictEntryCount
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("NUTRITION CONFLICT EVALUATION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
        println(
            "Conflict entries=" +
                    evaluation.conflictEntryCount
        )
        println(
            "Uniform scale mismatches=" +
                    evaluation.uniformScaleMismatchCount
        )
        println(
            "Energy unit-conversion mismatches=" +
                    evaluation.energyUnitConversionMismatchCount
        )
        println(
            "Single-nutrient conflicts=" +
                    evaluation.singleNutrientConflictCount
        )
        println(
            "Multi-nutrient profile conflicts=" +
                    evaluation.multiNutrientProfileConflictCount
        )
        println(
            "Counts by severity=" +
                    evaluation.countsBySeverity
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

    private companion object {

        const val MAXIMUM_EVALUATED_CONFLICTS =
            100_000
    }
}