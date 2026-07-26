package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.validation

import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregateDatasetWriter
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregator
import de.shopme.tools.knowledge.off.nutrition.reference.deduplication.OFFNutritionReferenceCandidateDeduplicator
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilter
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidationReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidateCanonicalOFFNutritionReferenceAggregatesTest {

    @Test
    fun validateCanonicalOFFNutritionReferenceAggregates() {

        val inputFile =
            File(
                "../data/generated/openfoodfacts/" +
                        "openfoodfacts-products.slim.jsonl.gz"
            )
                .canonicalFile

        require(inputFile.isFile) {
            "OFF slim dump not found: ${inputFile.absolutePath}"
        }

        val extractedCandidates =
            OFFCandidateExtractor()
                .extract(
                    file =
                        inputFile,
                    maxCandidates =
                        MAX_CANDIDATES
                )

        val generatedCandidates =
            OFFNutritionReferenceCandidateGenerator()
                .generate(
                    candidates =
                        extractedCandidates
                )

        val qualityResult =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        generatedCandidates.candidates
                )

        val deduplicationResult =
            OFFNutritionReferenceCandidateDeduplicator()
                .deduplicate(
                    candidates =
                        qualityResult.acceptedCandidates
                )

        val aggregationResult =
            OFFNutritionReferenceAggregator()
                .aggregate(
                    candidates =
                        deduplicationResult.candidates
                )

        val validationResult =
            OFFNutritionReferenceAggregateValidator()
                .validate(
                    aggregates =
                        aggregationResult.aggregates
                )

        val outputDirectory =
            File(
                "../data/generated/knowledge/off/nutrition"
            )
                .canonicalFile

        val validatedDatasetFile =
            outputDirectory.resolve(
                "nutrition-reference-aggregates.validated.json"
            )

        val validationReportFile =
            outputDirectory.resolve(
                "nutrition-reference-aggregate-validation.json"
            )

        OFFNutritionReferenceAggregateDatasetWriter()
            .write(
                aggregates =
                    validationResult.validatedAggregates,
                outputFile =
                    validatedDatasetFile
            )

        OFFNutritionReferenceAggregateValidationReportWriter()
            .write(
                result =
                    validationResult,
                outputFile =
                    validationReportFile
            )

        val issueCounts =
            validationResult.entries
                .asSequence()
                .flatMap { entry ->
                    entry.issues.asSequence()
                }
                .groupingBy { issue ->
                    issue.type
                }
                .eachCount()
                .toSortedMap(
                    compareBy { type ->
                        type.name
                    }
                )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CANONICAL OFF NUTRITION AGGREGATE VALIDATION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "inputAggregates=" +
                    validationResult.inputAggregateCount
        )
        println(
            "acceptedAggregates=" +
                    validationResult.acceptedAggregateCount
        )
        println(
            "warningAggregates=" +
                    validationResult.warningAggregateCount
        )
        println(
            "rejectedAggregates=" +
                    validationResult.rejectedAggregateCount
        )
        println(
            "validatedAggregates=" +
                    validationResult.validatedAggregates.size
        )
        println(
            "totalWarnings=" +
                    validationResult.totalWarningCount
        )
        println(
            "totalErrors=" +
                    validationResult.totalErrorCount
        )
        println(
            "issueCountsByType=" +
                    issueCounts
        )
        println(
            "validatedDatasetFile=" +
                    validatedDatasetFile.absolutePath
        )
        println(
            "validationReportFile=" +
                    validationReportFile.absolutePath
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            aggregationResult.aggregateCount,
            validationResult.inputAggregateCount
        )

        assertEquals(
            validationResult.inputAggregateCount,
            validationResult.acceptedAggregateCount +
                    validationResult.warningAggregateCount +
                    validationResult.rejectedAggregateCount
        )

        assertEquals(
            validationResult.inputAggregateCount,
            validationResult.validatedAggregates.size +
                    validationResult.rejectedAggregates.size
        )

        assertTrue(
            validationResult.validatedAggregates.isNotEmpty()
        )

        assertTrue(validatedDatasetFile.isFile)
        assertTrue(validationReportFile.isFile)
    }

    private companion object {

        const val MAX_CANDIDATES =
            50_000
    }
}