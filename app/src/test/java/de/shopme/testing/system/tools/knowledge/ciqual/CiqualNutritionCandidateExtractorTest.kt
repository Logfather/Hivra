package de.shopme.testing.system.tools.knowledge.ciqual

import de.shopme.tools.knowledge.ciqual.extractor.CiqualNutritionCandidateExtractor
import de.shopme.tools.knowledge.ciqual.model.CiqualNutritionRecord
import de.shopme.tools.knowledge.ciqual.model.CiqualNutritionValues
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CiqualNutritionCandidateExtractorTest {

    private val extractor =
        CiqualNutritionCandidateExtractor()

    @Test
    fun extractsCanonicalNutritionCandidate() {
        val record =
            CiqualNutritionRecord(
                foodCode = "20001",
                frenchName = "Cerfeuil, frais",
                englishName = "Chervil, fresh",
                groupCode = "02",
                nutrition =
                    CiqualNutritionValues(
                        energyKcalPer100g = 48.0,
                        fatPer100g = 0.6,
                        saturatedFatPer100g = 0.1,
                        carbohydratesPer100g = 3.1,
                        sugarsPer100g = 0.0,
                        fiberPer100g = 5.2,
                        proteinsPer100g = 3.3,
                        saltPer100g = 0.1
                    ),
                constituentValues =
                    mapOf(
                        "Energie" to 48.0,
                        "Lipides" to 0.6
                    )
            )

        val candidate =
            extractor.toCandidate(record)

        assertNotNull(candidate)

        assertEquals(
            "chervil fresh",
            candidate.canonicalId
        )

        assertEquals(
            setOf(
                "cerfeuil frais",
                "chervil fresh"
            ),
            candidate.aliases
        )

        assertEquals(
            setOf(
                "cerfeuil frais",
                "chervil fresh"
            ),
            candidate.matchAliases
        )

        assertEquals(
            1,
            candidate.dimensions.size
        )

        val nutrition =
            candidate.dimensions.single()

        @Suppress("UNCHECKED_CAST")
        val payload =
            nutrition.payload as Map<String, Any>

        assertEquals(
            KnowledgeDimensionCandidateType.NUTRITION,
            nutrition.dimension
        )

        assertEquals(
            48.0,
            payload["energyKcalPer100g"]
        )
        assertEquals(
            0.6,
            payload["fatPer100g"]
        )
        assertEquals(
            0.1,
            payload["saturatedFatPer100g"]
        )
        assertEquals(
            3.1,
            payload["carbohydratesPer100g"]
        )
        assertEquals(
            0.0,
            payload["sugarsPer100g"]
        )
        assertEquals(
            5.2,
            payload["fiberPer100g"]
        )
        assertEquals(
            3.3,
            payload["proteinsPer100g"]
        )
        assertEquals(
            0.1,
            payload["saltPer100g"]
        )

        assertEquals(
            "ciqual",
            candidate.metadata.source
        )
        assertEquals(
            "20001",
            candidate.metadata.sourceId
        )
        assertEquals(
            1.0,
            candidate.metadata.confidence
        )
        assertEquals(
            "2025-11-03",
            candidate.metadata.version
        )
        assertEquals(
            "Cerfeuil, frais",
            candidate.metadata.attributes["frenchName"]
        )
        assertEquals(
            "Chervil, fresh",
            candidate.metadata.attributes["englishName"]
        )
        assertEquals(
            "02",
            candidate.metadata.attributes["groupCode"]
        )
    }

    @Test
    fun usesFrenchNameWhenEnglishNameIsMissing() {
        val record =
            CiqualNutritionRecord(
                foodCode = "20002",
                frenchName = "Salsifis, cuit",
                englishName = null,
                groupCode = "02",
                nutrition =
                    CiqualNutritionValues(
                        energyKcalPer100g = 42.0,
                        fatPer100g = null,
                        saturatedFatPer100g = null,
                        carbohydratesPer100g = 5.0,
                        sugarsPer100g = null,
                        fiberPer100g = 3.0,
                        proteinsPer100g = 1.5,
                        saltPer100g = null
                    ),
                constituentValues =
                    emptyMap()
            )

        val candidate =
            extractor.toCandidate(record)

        assertNotNull(candidate)

        assertEquals(
            "salsifis cuit",
            candidate.canonicalId
        )

        assertEquals(
            setOf("salsifis cuit"),
            candidate.aliases
        )

        assertEquals(
            setOf("salsifis cuit"),
            candidate.matchAliases
        )
    }

    @Test
    fun omitsInvalidNutritionValues() {
        val record =
            CiqualNutritionRecord(
                foodCode = "20003",
                frenchName = "Aliment de test",
                englishName = "Test food",
                groupCode = null,
                nutrition =
                    CiqualNutritionValues(
                        energyKcalPer100g = 50.0,
                        fatPer100g = 150.0,
                        saturatedFatPer100g = null,
                        carbohydratesPer100g = -1.0,
                        sugarsPer100g = null,
                        fiberPer100g = null,
                        proteinsPer100g = 2.0,
                        saltPer100g = Double.NaN
                    ),
                constituentValues =
                    emptyMap()
            )

        val candidate =
            extractor.toCandidate(record)

        assertNotNull(candidate)

        @Suppress("UNCHECKED_CAST")
        val payload =
            candidate
                .dimensions
                .single()
                .payload as Map<String, Any>

        assertEquals(
            50.0,
            payload["energyKcalPer100g"]
        )
        assertEquals(
            2.0,
            payload["proteinsPer100g"]
        )

        assertTrue(
            "fatPer100g" !in payload
        )
        assertTrue(
            "carbohydratesPer100g" !in payload
        )
        assertTrue(
            "saltPer100g" !in payload
        )
    }

    @Test
    fun rejectsRecordWithoutUsableNutritionValues() {
        val record =
            CiqualNutritionRecord(
                foodCode = "20004",
                frenchName = "Aliment invalide",
                englishName = "Invalid food",
                groupCode = null,
                nutrition =
                    CiqualNutritionValues(
                        energyKcalPer100g = null,
                        fatPer100g = 150.0,
                        saturatedFatPer100g = null,
                        carbohydratesPer100g = -1.0,
                        sugarsPer100g = null,
                        fiberPer100g = null,
                        proteinsPer100g = null,
                        saltPer100g = Double.NaN
                    ),
                constituentValues =
                    emptyMap()
            )

        assertEquals(
            null,
            extractor.toCandidate(record)
        )
    }
}