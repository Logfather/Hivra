package de.shopme.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample

class LocalNutritionMatcherFeatureSubsetExtractor(
    private val delegate: LocalNutritionMatcherFeatureProvider,
    selectedFeatureNames: List<String>,
) : LocalNutritionMatcherFeatureProvider {

    override val featureNames: List<String> =
        selectedFeatureNames.toList()

    private val selectedFeatureIndices: List<Int> =
        featureNames.map { featureName ->
            val featureIndex =
                delegate.featureNames.indexOf(
                    featureName,
                )

            require(featureIndex >= 0) {
                "Selected nutrition matcher feature is not provided " +
                        "by the delegate extractor: $featureName"
            }

            featureIndex
        }

    init {
        require(
            featureNames.isNotEmpty(),
        ) {
            "Selected nutrition matcher feature names must not be empty."
        }

        require(
            featureNames.distinct().size ==
                    featureNames.size,
        ) {
            "Selected nutrition matcher feature names must be unique."
        }

        require(
            delegate.featureNames.distinct().size ==
                    delegate.featureNames.size,
        ) {
            "Delegate nutrition matcher feature names must be unique."
        }

        require(
            selectedFeatureIndices.distinct().size ==
                    selectedFeatureIndices.size,
        ) {
            "Selected nutrition matcher feature indices must be unique."
        }

        require(
            selectedFeatureIndices ==
                    selectedFeatureIndices.sorted(),
        ) {
            "Selected nutrition matcher features must preserve the " +
                    "delegate feature order."
        }
    }

    override fun extract(
        candidate: LocalNutritionMatcherCandidate,
        diagnosticScoreImputationValue: Double,
    ): DoubleArray {
        val completeFeatures =
            delegate.extract(
                candidate = candidate,
                diagnosticScoreImputationValue =
                    diagnosticScoreImputationValue,
            )

        return selectFeatures(
            completeFeatures = completeFeatures,
        )
    }

    override fun extract(
        example: NutritionMatcherTrainingExample,
        diagnosticScoreImputationValue: Double,
    ): DoubleArray {
        val completeFeatures =
            delegate.extract(
                example = example,
                diagnosticScoreImputationValue =
                    diagnosticScoreImputationValue,
            )

        return selectFeatures(
            completeFeatures = completeFeatures,
        )
    }

    private fun selectFeatures(
        completeFeatures: DoubleArray,
    ): DoubleArray {
        require(
            completeFeatures.size ==
                    delegate.featureNames.size,
        ) {
            "Delegate nutrition matcher feature vector size differs " +
                    "from its feature-name contract. Expected " +
                    delegate.featureNames.size +
                    ", actual " +
                    completeFeatures.size +
                    "."
        }

        return DoubleArray(
            size = selectedFeatureIndices.size,
        ) { selectedIndex ->
            completeFeatures[
                selectedFeatureIndices[selectedIndex]
            ]
        }
    }
}