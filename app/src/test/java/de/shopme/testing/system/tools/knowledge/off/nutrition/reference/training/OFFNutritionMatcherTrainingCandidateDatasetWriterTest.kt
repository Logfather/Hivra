package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.training

import de.shopme.tools.knowledge.off.nutrition.reference.training
.OFFNutritionMatcherTrainingCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.training
.OFFNutritionMatcherTrainingCandidateDatasetWriter
import de.shopme.tools.knowledge.off.nutrition.reference.validation
.OFFNutritionReferenceAggregateValidationStatus
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionMatcherTrainingCandidateDatasetWriterTest {

    @Test
    fun write_persistsDatasetDeterministically() {

        val candidate =
            OFFNutritionMatcherTrainingCandidate(
                serverArtifact =
                    "nutrition.json",
                serverKey =
                    "apple",
                canonicalId =
                    "apple",
                retrievalAliases =
                    listOf(
                        "apple",
                        "fresh apple"
                    ),
                canonicalAliases =
                    listOf("fresh apple"),
                matchAliases =
                    emptyList(),
                singleIngredientNutritionAliases =
                    emptyList(),
                nutrition =
                    sortedMapOf(
                        "energyKcalPer100g" to 52.0
                    ),
                profileCount =
                    1,
                nutrientCount =
                    1,
                validationStatus =
                    OFFNutritionReferenceAggregateValidationStatus
                        .ACCEPTED,
                warningCount =
                    0,
                validationIssueTypes =
                    emptyList(),
                representativeSourceId =
                    "001",
                sourceIds =
                    listOf("001"),
                source =
                    "open_food_facts_aggregate",
                sourceVersion =
                    "1",
                sourceConfidence =
                    1.0
            )

        val outputFile =
            Files
                .createTempDirectory(
                    "off-matcher-candidate-writer"
                )
                .resolve(
                    "candidates.json"
                )
                .toFile()

        val writer =
            OFFNutritionMatcherTrainingCandidateDatasetWriter()

        writer.write(
            candidates =
                listOf(candidate),
            outputFile =
                outputFile
        )

        val first =
            outputFile.readText()

        writer.write(
            candidates =
                listOf(candidate),
            outputFile =
                outputFile
        )

        val second =
            outputFile.readText()

        assertTrue(outputFile.isFile)

        assertEquals(
            first,
            second
        )

        assertTrue(
            first.contains(
                "\"serverKey\": \"apple\""
            )
        )
    }
}