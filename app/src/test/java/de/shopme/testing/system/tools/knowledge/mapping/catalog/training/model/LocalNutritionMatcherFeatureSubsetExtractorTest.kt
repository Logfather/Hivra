package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherCandidate
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureProvider
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureSubsetExtractor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalNutritionMatcherFeatureSubsetExtractorTest {

    @Test
    fun selectFeaturesInRequestedOrder() {

        val delegate =
            FixtureFeatureProvider()

        val extractor =
            LocalNutritionMatcherFeatureSubsetExtractor(
                delegate =
                    delegate,
                selectedFeatureNames =
                    listOf(
                        "first",
                        "third",
                    ),
            )

        val features =
            extractor.extract(
                candidate =
                    fixtureCandidate(),
                diagnosticScoreImputationValue =
                    0.5,
            )

        assertEquals(
            expected =
                listOf(
                    "first",
                    "third",
                ),
            actual =
                extractor.featureNames,
        )

        assertContentEquals(
            expected =
                doubleArrayOf(
                    1.0,
                    3.0,
                ),
            actual =
                features,
        )
    }

    @Test
    fun rejectUnknownFeature() {

        assertFailsWith<IllegalArgumentException> {

            LocalNutritionMatcherFeatureSubsetExtractor(
                delegate =
                    FixtureFeatureProvider(),
                selectedFeatureNames =
                    listOf(
                        "unknown",
                    ),
            )
        }
    }

    @Test
    fun rejectDuplicateFeature() {

        assertFailsWith<IllegalArgumentException> {

            LocalNutritionMatcherFeatureSubsetExtractor(
                delegate =
                    FixtureFeatureProvider(),
                selectedFeatureNames =
                    listOf(
                        "first",
                        "first",
                    ),
            )
        }
    }

    private fun fixtureCandidate():
            LocalNutritionMatcherCandidate {

        return LocalNutritionMatcherCandidate(
            catalogKey =
                "apple",
            serverKey =
                "fresh apple",
            candidateRank =
                1,
            candidateCount =
                5,
            diagnosticScore =
                0.8,
            diagnosticScoreAvailable =
                true,
            sharedTokens =
                listOf("apple"),
            domainMismatchFeatures =
                null,
        )
    }

    private class FixtureFeatureProvider :
        LocalNutritionMatcherFeatureProvider {

        override val featureNames =
            listOf(
                "first",
                "second",
                "third",
            )

        override fun extract(
            example: NutritionMatcherTrainingExample,
            diagnosticScoreImputationValue: Double,
        ): DoubleArray {

            return vector()
        }

        override fun extract(
            candidate: LocalNutritionMatcherCandidate,
            diagnosticScoreImputationValue: Double,
        ): DoubleArray {

            return vector()
        }

        private fun vector() =
            doubleArrayOf(
                1.0,
                2.0,
                3.0,
            )
    }
}