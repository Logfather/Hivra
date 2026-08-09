package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.evaluation.NormalizedCatalogEvaluator
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CanonicalFoodKeyNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.reader.CatalogFoodItemReader
import de.shopme.testing.system.tools.knowledge.catalog.report.NormalizedCatalogEvaluationReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.validation.NormalizedCatalogValidator
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunNormalizedCanonicalFoodCatalogEvaluationTest {

    @Test
    fun evaluateNormalizedCanonicalFoodCatalog() {
        val projectRoot = resolveProjectRoot()

        val normalizedCatalogFile = requireInputFile(
            File(
                projectRoot,
                NORMALIZED_CATALOG_PATH
            ),
            "Normalized catalog"
        )

        val canonicalizationPlanFile = requireInputFile(
            File(
                projectRoot,
                CANONICALIZATION_PLAN_PATH
            ),
            "Canonicalization-plan report"
        )

        val outputFile = File(
            projectRoot,
            EVALUATION_REPORT_PATH
        )

        val reader = CatalogFoodItemReader()

        val indexedItems = reader.read(
            normalizedCatalogFile
        )

        val items = indexedItems.map { it.item }

        val keyNormalizer =
            CanonicalFoodKeyNormalizer()

        val evaluator = NormalizedCatalogEvaluator(
            keyNormalizer = keyNormalizer,
            catalogValidator =
                NormalizedCatalogValidator(
                    keyNormalizer = keyNormalizer
                )
        )

        val categoryRegistry =
            CanonicalFoodCategoryRegistry()

        val firstResult = evaluator.evaluate(
            normalizedCatalogFile =
                normalizedCatalogFile,
            items = items,
            canonicalizationPlanFile =
                canonicalizationPlanFile,
            categoryRegistry =
                categoryRegistry
        )

        val secondResult = evaluator.evaluate(
            normalizedCatalogFile =
                normalizedCatalogFile,
            items = items,
            canonicalizationPlanFile =
                canonicalizationPlanFile,
            categoryRegistry =
                categoryRegistry
        )

        assertEquals(
            firstResult,
            secondResult,
            "Normalized catalog evaluation must be deterministic."
        )

        val writer =
            NormalizedCatalogEvaluationReportWriter()

        writer.write(
            result = firstResult,
            outputFile = outputFile
        )

        assertTrue(
            outputFile.isFile,
            "Evaluation report was not generated: " +
                    outputFile.absolutePath
        )

        assertTrue(
            outputFile.length() > 0L,
            "Evaluation report must not be empty."
        )

        val firstContent = outputFile.readText(
            StandardCharsets.UTF_8
        )

        writer.write(
            result = secondResult,
            outputFile = outputFile
        )

        val secondContent = outputFile.readText(
            StandardCharsets.UTF_8
        )

        assertEquals(
            firstContent,
            secondContent,
            "Repeated evaluation writes must be byte-identical."
        )

        assertTrue(
            firstContent.endsWith(
                System.lineSeparator()
            )
        )

        assertEquals(
            items.size,
            firstResult.entryCount
        )

        assertEquals(
            firstResult.validationIssueCount,
            firstResult.validationIssueCountsByType
                .values
                .sum()
        )

        assertEquals(
            items.size,
            firstResult.categoryCounts.values.sum()
        )

        printResult(
            result = firstResult,
            outputFile = outputFile
        )
    }

    private fun requireInputFile(
        file: File,
        description: String
    ): File {
        require(file.exists()) {
            "$description does not exist: ${file.absolutePath}"
        }

        require(file.isFile) {
            "$description path is not a file: ${file.absolutePath}"
        }

        require(file.canRead()) {
            "$description is not readable: ${file.absolutePath}"
        }

        require(file.length() > 0L) {
            "$description is empty: ${file.absolutePath}"
        }

        return file
    }

    private fun resolveProjectRoot(): File {
        val userDirectory = requireNotNull(
            System.getProperty("user.dir")
        ) {
            "System property 'user.dir' is not available."
        }

        val startDirectory = File(
            userDirectory
        ).canonicalFile

        return generateSequence(startDirectory) {
            it.parentFile
        }
            .flatMap { directory ->
                sequenceOf(
                    directory,
                    File(directory, "ShopMe")
                )
            }
            .distinctBy(File::getCanonicalPath)
            .firstOrNull { candidate ->
                File(
                    candidate,
                    NORMALIZED_CATALOG_PATH
                ).isFile &&
                        File(candidate, "gradlew").isFile &&
                        File(candidate, "app").isDirectory
            }
            ?: throw IllegalArgumentException(
                "Could not locate ShopMe project root from " +
                        startDirectory.absolutePath
            )
    }

    private fun printResult(
        result:
        de.shopme.testing.system.tools.knowledge.catalog.evaluation
        .NormalizedCatalogEvaluationResult,
        outputFile: File
    ) {
        println()
        println("Normalized canonical food catalog evaluation")
        println("--------------------------------------------")
        println("Entries: ${result.entryCount}")
        println("Categories: ${result.categoryCount}")
        println(
            "Validation issues: " +
                    result.validationIssueCount
        )
        println(
            "Invalid normalized keys: " +
                    result.invalidNormalizedKeyCount
        )
        println(
            "Duplicate key groups: " +
                    result.duplicateNormalizedKeyGroupCount
        )
        println(
            "Duplicate name groups: " +
                    result.duplicateItemNameGroupCount
        )
        println(
            "Unknown categories: " +
                    result.unknownCategoryCount
        )
        println(
            "Missing plurals: " +
                    result.missingPluralCount
        )
        println(
            "Lowercase plurals: " +
                    result.lowercasePluralCount
        )
        println(
            "Manual review actions: " +
                    result.manualReviewActionCount
        )
        println(
            "Readiness: ${result.readinessStatus.name}"
        )
        println(
            "Report: ${outputFile.absolutePath}"
        )

        if (result.blockingReasons.isNotEmpty()) {
            println()
            println("Blocking reasons:")

            result.blockingReasons.forEach {
                println("- $it")
            }
        }

        if (result.reviewReasons.isNotEmpty()) {
            println()
            println("Review reasons:")

            result.reviewReasons.forEach {
                println("- $it")
            }
        }

        println()
    }

    private companion object {

        const val NORMALIZED_CATALOG_PATH =
            "data/generated/knowledge/catalog/normalized/" +
                    "catalog.normalized.json"

        const val CANONICALIZATION_PLAN_PATH =
            "data/generated/knowledge/catalog/audit/" +
                    "catalog-canonicalization-plan.json"

        const val EVALUATION_REPORT_PATH =
            "data/generated/knowledge/catalog/evaluation/" +
                    "catalog-normalized-evaluation.json"
    }
}