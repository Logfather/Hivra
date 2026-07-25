package de.shopme.tools.knowledge.mapping.catalog.training.model

import com.google.gson.Gson
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample
import java.io.File
import kotlin.math.exp

class LocalNutritionMatcherPredictor(
    private val model:
    LocalNutritionMatcherModel,
    private val featureExtractor:
    LocalNutritionMatcherFeatureProvider =
        LocalNutritionMatcherFeatureSubsetExtractor(
            delegate =
                LocalNutritionMatcherFeatureExtractor(),
            selectedFeatureNames =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES,
        ),
) {

    init {

        val featureCount =
            featureExtractor.featureNames.size

        require(
            model.version ==
                    LocalNutritionMatcherModelContract.CURRENT_VERSION,
        ) {
            "Unsupported local nutrition matcher model version: " +
                    "expected=" +
                    LocalNutritionMatcherModelContract.CURRENT_VERSION +
                    ", actual=" +
                    model.version +
                    "."
        }

        require(
            featureExtractor.featureNames ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES,
        ) {
            "Local nutrition matcher predictor does not use the active " +
                    "feature contract."
        }

        require(
            model.featureNames ==
                    featureExtractor.featureNames,
        ) {
            "Local nutrition matcher model and predictor feature contracts differ. " +
                    "Model: ${model.featureNames.joinToString()}; " +
                    "Predictor: ${featureExtractor.featureNames.joinToString()}"
        }

        require(
            model.featureMeans.size ==
                    featureCount
        )

        require(
            model.featureStandardDeviations.size ==
                    featureCount
        )

        require(
            model.coefficients.size ==
                    featureCount
        )

        require(
            "diagnostic_score_available" !in
                    model.featureNames
        ) {
            "Local matcher model contains forbidden source-leakage feature."
        }

        require(
            model.diagnosticScoreImputationValue
                .isFinite()
        ) {
            "Local matcher model contains invalid diagnostic score imputation value."
        }

        require(
            model.featureNames.none { featureName ->
                featureName in
                        LocalNutritionMatcherFeatureContract
                            .HARMFUL_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Local nutrition matcher model contains disabled harmful features: " +
                    model.featureNames
                        .filter { featureName ->
                            featureName in
                                    LocalNutritionMatcherFeatureContract
                                        .HARMFUL_DOMAIN_FEATURE_NAMES
                        }
                        .joinToString()
        }
    }

    fun predictProbability(
        example: NutritionMatcherTrainingExample
    ): Double {

        return predictProbability(
            candidate =
                LocalNutritionMatcherCandidate(
                    catalogKey =
                        example.catalogKey,
                    serverKey =
                        example.serverKey,
                    candidateRank =
                        example.candidateRank,
                    candidateCount =
                        example.candidateCount,
                    diagnosticScore =
                        example.diagnosticScore,
                    diagnosticScoreAvailable =
                        example.diagnosticScoreAvailable,
                    sharedTokens =
                        example.sharedTokens,
                    domainMismatchFeatures =
                        example.domainMismatchFeatures,
                )
        )
    }

    fun predictProbability(
        candidate: LocalNutritionMatcherCandidate
    ): Double {

        val rawFeatures =
            featureExtractor.extract(
                candidate =
                    candidate,
                diagnosticScoreImputationValue =
                    model.diagnosticScoreImputationValue
            )

        var score =
            model.intercept

        rawFeatures.indices.forEach { index ->

            val scaled =
                (
                        rawFeatures[index] -
                                model.featureMeans[index]
                        ) /
                        model.featureStandardDeviations[index]

            score +=
                model.coefficients[index] *
                        scaled
        }

        return sigmoid(
            value = score
        )
    }

    fun predictsMatch(
        example: NutritionMatcherTrainingExample
    ): Boolean {

        return predictProbability(
            example = example
        ) >= model.decisionThreshold
    }

    fun predictsMatch(
        candidate: LocalNutritionMatcherCandidate
    ): Boolean {

        return predictProbability(
            candidate = candidate
        ) >= model.decisionThreshold
    }

    companion object {

        fun fromFile(
            modelFile: File
        ): LocalNutritionMatcherPredictor {

            require(modelFile.isFile) {
                "Local nutrition matcher model does not exist: " +
                        modelFile.absolutePath
            }

            val model =
                Gson().fromJson(
                    modelFile.readText(),
                    LocalNutritionMatcherModel::class.java
                )

            return LocalNutritionMatcherPredictor(
                model = model
            )
        }

        private fun sigmoid(
            value: Double
        ): Double {

            return if (value >= 0.0) {

                1.0 /
                        (
                                1.0 +
                                        exp(-value)
                                )

            } else {

                val exponential =
                    exp(value)

                exponential /
                        (1.0 + exponential)
            }
        }
    }
}