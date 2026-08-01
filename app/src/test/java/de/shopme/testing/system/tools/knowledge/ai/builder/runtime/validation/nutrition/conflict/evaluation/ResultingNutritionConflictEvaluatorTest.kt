package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictAnalysis
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictExample
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictMatchType
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictNutrient
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionNutrientConflict
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictClassification
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictEvaluator
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultingNutritionConflictEvaluatorTest {

    @Test
    fun evaluate_classifiesAllPrimaryConflictPatterns() {

        val directory =
            Files.createTempDirectory(
                "nutrition-conflict-evaluation"
            )
                .toFile()

        try {
            val analysis =
                analysis(
                    aggregateFile =
                        directory.resolve(
                            "aggregates.json"
                        ),
                    runtimeFile =
                        directory.resolve(
                            "nutrition.json"
                        ),
                    examples =
                        listOf(
                            uniformScaleExample(),
                            energyUnitExample(),
                            singleNutrientExample(),
                            multiNutrientExample()
                        )
                )

            val result =
                ResultingNutritionConflictEvaluator()
                    .evaluate(
                        analysis =
                            analysis
                    )

            assertEquals(
                4L,
                result.evaluatedConflictEntryCount
            )

            assertEquals(
                1L,
                result.uniformScaleMismatchCount
            )

            assertEquals(
                1L,
                result.energyUnitConversionMismatchCount
            )

            assertEquals(
                1L,
                result.singleNutrientConflictCount
            )

            assertEquals(
                1L,
                result.multiNutrientProfileConflictCount
            )

            assertEquals(
                1L,
                result.countsByClassification[
                    ResultingNutritionConflictClassification
                        .LIKELY_UNIFORM_SCALE_MISMATCH
                ]
            )

            assertEquals(
                1L,
                result.countsByClassification[
                    ResultingNutritionConflictClassification
                        .LIKELY_ENERGY_UNIT_CONVERSION_MISMATCH
                ]
            )

            assertEquals(
                1L,
                result.countsByClassification[
                    ResultingNutritionConflictClassification
                        .SINGLE_NUTRIENT_CONFLICT
                ]
            )

            assertEquals(
                1L,
                result.countsByClassification[
                    ResultingNutritionConflictClassification
                        .MULTI_NUTRIENT_PROFILE_CONFLICT
                ]
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun uniformScaleExample():
            ResultingNutritionConflictExample =
        ResultingNutritionConflictExample(
            aggregateCanonicalId =
                "uniform-scale",
            runtimeCanonicalId =
                "uniform-scale",
            matchType =
                ResultingNutritionConflictMatchType.EXACT,
            conflictCount =
                3,
            maximumAbsoluteDifference =
                200.0,
            conflicts =
                listOf(
                    conflict(
                        nutrient =
                            ResultingNutritionConflictNutrient.ENERGY,
                        aggregateValue =
                            400.0,
                        runtimeValue =
                            200.0
                    ),
                    conflict(
                        nutrient =
                            ResultingNutritionConflictNutrient.FAT,
                        aggregateValue =
                            20.0,
                        runtimeValue =
                            10.0
                    ),
                    conflict(
                        nutrient =
                            ResultingNutritionConflictNutrient.PROTEIN,
                        aggregateValue =
                            10.0,
                        runtimeValue =
                            5.0
                    )
                )
        )

    private fun energyUnitExample():
            ResultingNutritionConflictExample =
        ResultingNutritionConflictExample(
            aggregateCanonicalId =
                "energy-unit",
            runtimeCanonicalId =
                "energy-unit",
            matchType =
                ResultingNutritionConflictMatchType.EXACT,
            conflictCount =
                1,
            maximumAbsoluteDifference =
                318.4,
            conflicts =
                listOf(
                    conflict(
                        nutrient =
                            ResultingNutritionConflictNutrient.ENERGY,
                        aggregateValue =
                            100.0,
                        runtimeValue =
                            418.4
                    )
                )
        )

    private fun singleNutrientExample():
            ResultingNutritionConflictExample =
        ResultingNutritionConflictExample(
            aggregateCanonicalId =
                "single",
            runtimeCanonicalId =
                "single",
            matchType =
                ResultingNutritionConflictMatchType.EXACT,
            conflictCount =
                1,
            maximumAbsoluteDifference =
                5.0,
            conflicts =
                listOf(
                    conflict(
                        nutrient =
                            ResultingNutritionConflictNutrient.SUGARS,
                        aggregateValue =
                            10.0,
                        runtimeValue =
                            15.0
                    )
                )
        )

    private fun multiNutrientExample():
            ResultingNutritionConflictExample =
        ResultingNutritionConflictExample(
            aggregateCanonicalId =
                "multi",
            runtimeCanonicalId =
                "multi",
            matchType =
                ResultingNutritionConflictMatchType
                    .NORMALIZATION_EQUIVALENT,
            conflictCount =
                3,
            maximumAbsoluteDifference =
                300.0,
            conflicts =
                listOf(
                    conflict(
                        nutrient =
                            ResultingNutritionConflictNutrient.ENERGY,
                        aggregateValue =
                            400.0,
                        runtimeValue =
                            100.0
                    ),
                    conflict(
                        nutrient =
                            ResultingNutritionConflictNutrient.FAT,
                        aggregateValue =
                            20.0,
                        runtimeValue =
                            15.0
                    ),
                    conflict(
                        nutrient =
                            ResultingNutritionConflictNutrient.PROTEIN,
                        aggregateValue =
                            10.0,
                        runtimeValue =
                            2.0
                    )
                )
        )

    private fun conflict(
        nutrient: ResultingNutritionConflictNutrient,
        aggregateValue: Double,
        runtimeValue: Double
    ): ResultingNutritionNutrientConflict {

        val difference =
            kotlin.math.abs(
                aggregateValue -
                        runtimeValue
            )

        return ResultingNutritionNutrientConflict(
            nutrient =
                nutrient,
            aggregateValue =
                aggregateValue,
            runtimeValue =
                runtimeValue,
            absoluteDifference =
                difference,
            tolerance =
                nutrient.absoluteTolerance
        )
    }

    private fun analysis(
        aggregateFile: java.io.File,
        runtimeFile: java.io.File,
        examples: List<ResultingNutritionConflictExample>
    ): ResultingNutritionConflictAnalysis {

        aggregateFile.writeText("[]")
        runtimeFile.writeText("{}")

        val conflictCountsByNutrient =
            examples
                .flatMap { example ->
                    example.conflicts
                }
                .groupingBy { conflict ->
                    conflict.nutrient
                }
                .eachCount()
                .mapValues { (_, count) ->
                    count.toLong()
                }

        val nutrientConflictCount =
            examples.sumOf { example ->
                example.conflictCount
            }
                .toLong()

        return ResultingNutritionConflictAnalysis(
            aggregateFile =
                aggregateFile,
            runtimeFile =
                runtimeFile,
            aggregateEntryCount =
                examples.size.toLong(),
            runtimeEntryCount =
                examples.size.toLong(),
            exactMatchedEntryCount =
                examples.count { example ->
                    example.matchType ==
                            ResultingNutritionConflictMatchType.EXACT
                }
                    .toLong(),
            normalizationEquivalentMatchedEntryCount =
                examples.count { example ->
                    example.matchType ==
                            ResultingNutritionConflictMatchType
                                .NORMALIZATION_EQUIVALENT
                }
                    .toLong(),
            matchedEntryCount =
                examples.size.toLong(),
            comparableEntryCount =
                examples.size.toLong(),
            nonComparableEntryCount =
                0L,
            conflictEntryCount =
                examples.size.toLong(),
            conflictFreeEntryCount =
                0L,
            nutrientComparisonCount =
                nutrientConflictCount,
            nutrientConflictCount =
                nutrientConflictCount,
            entryConflictRate =
                1.0,
            nutrientConflictRate =
                1.0,
            comparisonCountsByNutrient =
                conflictCountsByNutrient,
            conflictCountsByNutrient =
                conflictCountsByNutrient,
            maximumAbsoluteDifferenceByNutrient =
                examples
                    .flatMap { example ->
                        example.conflicts
                    }
                    .groupBy { conflict ->
                        conflict.nutrient
                    }
                    .mapValues { (_, conflicts) ->
                        conflicts.maxOf { conflict ->
                            conflict.absoluteDifference
                        }
                    },
            conflictExamples =
                examples,
            omittedConflictExampleCount =
                0L,
            durationMillis =
                1L
        )
    }
}