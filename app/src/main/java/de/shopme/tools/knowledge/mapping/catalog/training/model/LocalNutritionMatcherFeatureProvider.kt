package de.shopme.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample

interface LocalNutritionMatcherFeatureProvider {

    val featureNames: List<String>

    fun extract(
        example: NutritionMatcherTrainingExample,
        diagnosticScoreImputationValue: Double,
    ): DoubleArray

    fun extract(
        candidate: LocalNutritionMatcherCandidate,
        diagnosticScoreImputationValue: Double,
    ): DoubleArray
}