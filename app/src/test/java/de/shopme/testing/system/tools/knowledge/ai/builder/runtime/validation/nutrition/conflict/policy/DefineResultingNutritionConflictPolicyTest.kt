package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictAnalyzer
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictEvaluator
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy.ResultingNutritionConflictPolicy
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy.ResultingNutritionConflictPolicyEvaluator
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy.ResultingNutritionConflictPolicyReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DefineResultingNutritionConflictPolicyTest {

    @Test
    fun defineResultingNutritionConflictPolicy() {

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
                "data/generated/knowledge/policies/" +
                        "resulting-nutrition-conflict-policy.json"
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
                    MAXIMUM_EVALUATED_CONFLICTS
            )
                .analyze(
                    aggregateFile =
                        aggregateFile,
                    runtimeFile =
                        runtimeFile
                )

        val evaluation =
            ResultingNutritionConflictEvaluator(
                maximumReportedExamples =
                    MAXIMUM_EVALUATED_CONFLICTS
            )
                .evaluate(
                    analysis =
                        analysis
                )

        val policy =
            ResultingNutritionConflictPolicy()

        val decision =
            ResultingNutritionConflictPolicyEvaluator(
                policy =
                    policy
            )
                .evaluate(
                    analysis =
                        analysis,
                    evaluation =
                        evaluation
                )

        ResultingNutritionConflictPolicyReportWriter()
            .write(
                policy =
                    policy,
                decision =
                    decision,
                outputFile =
                    reportFile
            )

        assertTrue(
            reportFile.isFile,
            "Nutrition conflict policy report was not generated: " +
                    reportFile.absolutePath
        )

        assertTrue(
            decision.approved,
            buildString {
                appendLine(
                    "Resulting Nutrition conflict policy rejected the " +
                            "current dataset."
                )
                appendLine(
                    "Entry conflict rate=" +
                            decision.entryConflictRate
                )
                appendLine(
                    "Maximum entry conflict rate=" +
                            decision.maximumEntryConflictRate
                )
                appendLine(
                    "Nutrient conflict rate=" +
                            decision.nutrientConflictRate
                )
                appendLine(
                    "Maximum nutrient conflict rate=" +
                            decision.maximumNutrientConflictRate
                )
                appendLine(
                    "Extreme conflicts=" +
                            decision.extremeConflictCount
                )
                appendLine(
                    "Maximum extreme conflicts=" +
                            decision.maximumExtremeConflictCount
                )
                appendLine(
                    "Violations=" +
                            decision.violations
                )
            }
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("NUTRITION CONFLICT POLICY")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
        println(
            "Approved=" +
                    decision.approved
        )
        println(
            "Entry conflict rate=" +
                    decision.entryConflictRate
        )
        println(
            "Maximum entry conflict rate=" +
                    decision.maximumEntryConflictRate
        )
        println(
            "Nutrient conflict rate=" +
                    decision.nutrientConflictRate
        )
        println(
            "Maximum nutrient conflict rate=" +
                    decision.maximumNutrientConflictRate
        )
        println(
            "Extreme conflicts=" +
                    decision.extremeConflictCount
        )
        println(
            "Maximum extreme conflicts=" +
                    decision.maximumExtremeConflictCount
        )
        println(
            "Violations=" +
                    decision.violations.size
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