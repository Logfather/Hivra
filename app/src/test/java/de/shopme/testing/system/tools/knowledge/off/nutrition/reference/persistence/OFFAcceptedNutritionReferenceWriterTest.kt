package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.persistence

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.persistence.OFFAcceptedNutritionReferenceReader
import de.shopme.tools.knowledge.off.nutrition.reference.persistence.OFFAcceptedNutritionReferenceWriter
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFAcceptedNutritionReferenceWriterTest {

    @Test
    fun write_thenRead_preservesReferencesInOrder() {

        val references =
            createOFFNutritionReferenceCandidates()

        val outputDirectory =
            Files.createTempDirectory(
                "off-accepted-references"
            ).toFile()

        val outputFile =
            outputDirectory.resolve(
                "references.jsonl.gz"
            )

        val writeResult =
            OFFAcceptedNutritionReferenceWriter()
                .write(
                    outputFile =
                        outputFile
                ) { append ->

                    references.forEach { reference ->
                        append(reference)
                    }
                }

        val restoredReferences =
            mutableListOf<CanonicalOFFNutritionReferenceCandidate>()

        val readCount =
            OFFAcceptedNutritionReferenceReader()
                .forEachReference(
                    inputFile =
                        outputFile,
                    consumer =
                        restoredReferences::add
                )

        assertTrue(
            outputFile.isFile,
            "The compressed reference artifact must exist."
        )

        assertEquals(
            references.size.toLong(),
            writeResult.writtenReferenceCount
        )

        assertEquals(
            references.size.toLong(),
            readCount
        )

        assertEquals(
            references,
            restoredReferences,
            "Writer and reader must preserve all references and their order."
        )

        assertEquals(
            64,
            writeResult.contentSha256.length
        )

        assertTrue(
            writeResult.contentSha256.matches(
                Regex("[0-9a-f]{64}")
            ),
            "The content hash must be a lowercase SHA-256 value."
        )

        assertTrue(
            writeResult.uncompressedContentBytes > 0L
        )
    }

    private fun createOFFNutritionReferenceCandidates():
            List<CanonicalOFFNutritionReferenceCandidate> {

        return listOf(
            CanonicalOFFNutritionReferenceCandidate(
                sourceId =
                    "fixture-1",
                canonicalId =
                    "whole-milk",
                aliases =
                    setOf(
                        "whole milk",
                        "vollmilch"
                    ),
                matchAliases =
                    setOf(
                        "milk",
                        "whole milk",
                        "vollmilch"
                    ),
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 64.0,
                        "fatPer100g" to 3.5,
                        "saturatedFatPer100g" to 2.3,
                        "carbohydratesPer100g" to 4.8,
                        "sugarsPer100g" to 4.8,
                        "proteinsPer100g" to 3.3,
                        "saltPer100g" to 0.1
                    ),
                productName =
                    "Whole Milk",
                brand =
                    "Fixture Brand",
                categories =
                    "Dairies, Milks, Whole milks",
                singleIngredientNutritionAliases =
                    setOf(
                        "milk",
                        "whole milk"
                    ),
                source =
                    "open_food_facts",
                sourceVersion =
                    "1",
                sourceConfidence =
                    1.0
            ),
            CanonicalOFFNutritionReferenceCandidate(
                sourceId =
                    "fixture-2",
                canonicalId =
                    "plain-yogurt",
                aliases =
                    setOf(
                        "plain yogurt",
                        "naturjoghurt"
                    ),
                matchAliases =
                    setOf(
                        "yogurt",
                        "plain yogurt",
                        "naturjoghurt"
                    ),
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 61.0,
                        "fatPer100g" to 3.3,
                        "saturatedFatPer100g" to 2.1,
                        "carbohydratesPer100g" to 4.7,
                        "sugarsPer100g" to 4.7,
                        "proteinsPer100g" to 3.5,
                        "saltPer100g" to 0.12
                    ),
                productName =
                    "Plain Yogurt",
                brand =
                    null,
                categories =
                    "Dairies, Fermented foods, Yogurts",
                singleIngredientNutritionAliases =
                    setOf(
                        "yogurt",
                        "plain yogurt"
                    ),
                source =
                    "open_food_facts",
                sourceVersion =
                    "1",
                sourceConfidence =
                    1.0
            )
        )
    }
}