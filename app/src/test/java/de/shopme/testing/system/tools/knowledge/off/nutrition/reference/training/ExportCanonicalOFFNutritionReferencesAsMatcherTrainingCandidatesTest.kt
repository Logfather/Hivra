package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.training

import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregator
import de.shopme.tools.knowledge.off.nutrition.reference.deduplication.OFFNutritionReferenceCandidateDeduplicator
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilter
import de.shopme.tools.knowledge.off.nutrition.reference.training.OFFNutritionMatcherTrainingCandidateDatasetWriter
import de.shopme.tools.knowledge.off.nutrition.reference.training.OFFNutritionMatcherTrainingCandidateExportReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.training.OFFNutritionMatcherTrainingCandidateExporter
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportCanonicalOFFNutritionReferencesAsMatcherTrainingCandidatesTest {

    @Test
    fun exportCanonicalOFFNutritionReferencesAsMatcherTrainingCandidates() {

        val inputFile =
            File(
                "../data/generated/openfoodfacts/" +
                        "openfoodfacts-products.slim.jsonl.gz"
            )
                .canonicalFile

        require(inputFile.isFile) {
            "OFF slim dump not found: ${inputFile.absolutePath}"
        }

        val extracted =
            OFFCandidateExtractor()
                .extract(
                    file =
                        inputFile,
                    maxCandidates =
                        MAX_CANDIDATES
                )

        val generated =
            OFFNutritionReferenceCandidateGenerator()
                .generate(
                    candidates =
                        extracted
                )

        val qualityResult =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        generated.candidates
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

        val exportResult =
            OFFNutritionMatcherTrainingCandidateExporter()
                .export(
                    validationResult =
                        validationResult
                )

        val outputDirectory =
            File(
                "../data/generated/knowledge/off/nutrition"
            )
                .canonicalFile

        val datasetFile =
            outputDirectory.resolve(
                "nutrition-reference-matcher-candidates.json"
            )

        val reportFile =
            outputDirectory.resolve(
                "nutrition-reference-matcher-candidate-export-report.json"
            )

        OFFNutritionMatcherTrainingCandidateDatasetWriter()
            .write(
                candidates =
                    exportResult.candidates,
                outputFile =
                    datasetFile
            )

        OFFNutritionMatcherTrainingCandidateExportReportWriter()
            .write(
                result =
                    exportResult,
                outputFile =
                    reportFile
            )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("OFF NUTRITION MATCHER TRAINING CANDIDATE EXPORT")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "inputAggregates=" +
                    exportResult.inputAggregateCount
        )
        println(
            "exportedCandidates=" +
                    exportResult.exportedCandidateCount
        )
        println(
            "acceptedCandidates=" +
                    exportResult.acceptedCandidateCount
        )
        println(
            "warningCandidates=" +
                    exportResult.warningCandidateCount
        )
        println(
            "uniqueRetrievalAliases=" +
                    exportResult.uniqueRetrievalAliasCount
        )
        println(
            "datasetFile=" +
                    datasetFile.absolutePath
        )
        println(
            "reportFile=" +
                    reportFile.absolutePath
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            validationResult.validatedAggregates.size,
            exportResult.exportedCandidateCount
        )

        assertEquals(
            20_590,
            exportResult.exportedCandidateCount
        )

        assertEquals(
            19_876,
            exportResult.acceptedCandidateCount
        )

        assertEquals(
            714,
            exportResult.warningCandidateCount
        )

        assertTrue(
            exportResult.uniqueRetrievalAliasCount >=
                    exportResult.exportedCandidateCount
        )

        assertTrue(
            exportResult.candidates.all { candidate ->
                candidate.serverArtifact ==
                        "nutrition.json"
            }
        )

        assertTrue(
            exportResult.candidates.all { candidate ->
                candidate.serverKey in
                        candidate.retrievalAliases
            }
        )

        assertTrue(datasetFile.isFile)
        assertTrue(reportFile.isFile)
    }

    private companion object {

        const val MAX_CANDIDATES =
            50_000
    }
}