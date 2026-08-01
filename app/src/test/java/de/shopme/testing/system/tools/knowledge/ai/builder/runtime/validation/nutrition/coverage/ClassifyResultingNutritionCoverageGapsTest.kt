package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage.ResultingNutritionCoverageGapClassificationReportWriter
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage.ResultingNutritionCoverageGapClassifier
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ClassifyResultingNutritionCoverageGapsTest {

    @Test
    fun classifyResultingNutritionCoverageGaps() {

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
                        "resulting-nutrition-coverage-gap-classification.json"
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

        val classification =
            ResultingNutritionCoverageGapClassifier(
                maximumReportedNormalizationEquivalentExamples =
                    250,
                maximumReportedTrueMissingRuntimeCanonicalIds =
                    250,
                maximumReportedTrueAdditionalRuntimeCanonicalIds =
                    250
            )
                .classify(
                    aggregateFile =
                        aggregateFile,
                    runtimeFile =
                        runtimeFile
                )

        ResultingNutritionCoverageGapClassificationReportWriter()
            .write(
                classification =
                    classification,
                outputFile =
                    reportFile
            )

        assertTrue(
            reportFile.isFile,
            "Nutrition coverage gap classification report was not " +
                    "generated: ${reportFile.absolutePath}"
        )

        assertTrue(
            classification.aggregateEntryCount > 0L
        )

        assertTrue(
            classification.runtimeEntryCount > 0L
        )

        assertTrue(
            classification.effectiveCoverageRate >=
                    classification.exactCoverageRate
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("NUTRITION COVERAGE GAP CLASSIFICATION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
        println(
            "Aggregate entries=" +
                    classification.aggregateEntryCount
        )
        println(
            "Runtime entries=" +
                    classification.runtimeEntryCount
        )
        println(
            "Exact matches=" +
                    classification.exactMatchCount
        )
        println(
            "Normalization-equivalent matches=" +
                    classification.normalizationEquivalentMatchCount
        )
        println(
            "True missing runtime entries=" +
                    classification.trueMissingRuntimeEntryCount
        )
        println(
            "True additional runtime entries=" +
                    classification.trueAdditionalRuntimeEntryCount
        )
        println(
            "Exact coverage=" +
                    classification.exactCoverageRate
        )
        println(
            "Effective coverage=" +
                    classification.effectiveCoverageRate
        )
        println(
            "Normalization collision groups=" +
                    classification.normalizationCollisionGroupCount
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