package de.shopme.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample

class LocalNutritionMatcherFeatureSubsetExtractor(
    private val delegate: LocalNutritionMatcherFeatureProvider =
        LocalNutritionMatcherFeatureExtractor(),
    selectedFeatureNames: List<String>,
) : LocalNutritionMatcherFeatureProvider {

    override val featureNames: List<String> =
        selectedFeatureNames.toList()

    private val selectedIndices: IntArray

    init {
        require(featureNames.isNotEmpty()) {
            "Local nutrition matcher feature subset must not be empty."
        }

        require(
            featureNames.size ==
                    featureNames.distinct().size,
        ) {
            "Local nutrition matcher feature subset contains duplicate names."
        }

        require(
            delegate.featureNames.isNotEmpty(),
        ) {
            "Delegate feature contract must not be empty."
        }

        require(
            delegate.featureNames.size ==
                    delegate.featureNames.distinct().size,
        ) {
            "Delegate feature contract contains duplicate names."
        }

        val delegateIndices =
            delegate.featureNames
                .withIndex()
                .associate {
                    it.value to it.index
                }

        val unknownFeatureNames =
            featureNames.filterNot {
                it in delegateIndices
            }

        require(unknownFeatureNames.isEmpty()) {
            "Unknown local nutrition matcher features: " +
                    unknownFeatureNames
                        .sorted()
                        .joinToString()
        }

        selectedIndices =
            featureNames
                .map { featureName ->
                    requireNotNull(
                        delegateIndices[featureName],
                    ) {
                        "Missing delegate feature index for: $featureName"
                    }
                }
                .toIntArray()
    }

    override fun extract(
        example: NutritionMatcherTrainingExample,
        diagnosticScoreImputationValue: Double,
    ): DoubleArray {

        val completeFeatureVector =
            delegate.extract(
                example =
                    example,
                diagnosticScoreImputationValue =
                    diagnosticScoreImputationValue,
            )

        return selectFeatures(
            completeFeatureVector =
                completeFeatureVector,
        )
    }

    override fun extract(
        candidate: LocalNutritionMatcherCandidate,
        diagnosticScoreImputationValue: Double,
    ): DoubleArray {

        val completeFeatureVector =
            delegate.extract(
                candidate =
                    candidate,
                diagnosticScoreImputationValue =
                    diagnosticScoreImputationValue,
            )

        return selectFeatures(
            completeFeatureVector =
                completeFeatureVector,
        )
    }

    private fun selectFeatures(
        completeFeatureVector: DoubleArray,
    ): DoubleArray {

        require(
            completeFeatureVector.size ==
                    delegate.featureNames.size,
        ) {
            "Delegate feature vector contains " +
                    "${completeFeatureVector.size} values, but its contract " +
                    "contains ${delegate.featureNames.size} names."
        }

        val selectedFeatureVector =
            DoubleArray(
                size =
                    selectedIndices.size,
            ) { targetIndex ->

                completeFeatureVector[
                    selectedIndices[targetIndex]
                ]
            }

        check(
            selectedFeatureVector.size ==
                    featureNames.size,
        ) {
            "Selected feature vector contains " +
                    "${selectedFeatureVector.size} values, but its contract " +
                    "contains ${featureNames.size} names."
        }

        require(
            selectedFeatureVector.all {
                it.isFinite()
            },
        ) {
            "Selected local nutrition matcher feature vector contains " +
                    "a non-finite value."
        }

        return selectedFeatureVector
    }
}