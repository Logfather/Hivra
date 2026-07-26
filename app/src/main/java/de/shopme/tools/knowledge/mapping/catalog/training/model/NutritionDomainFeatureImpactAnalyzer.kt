package de.shopme.tools.knowledge.mapping.catalog.training.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class NutritionDomainFeatureImpactAnalyzer {

    fun analyze(
        comparisonReportFile: File,
    ): NutritionDomainFeatureImpactReport {

        require(comparisonReportFile.exists()) {
            "Nutrition matcher comparison report does not exist: " +
                    comparisonReportFile.absolutePath
        }

        require(comparisonReportFile.isFile) {
            "Nutrition matcher comparison report is not a file: " +
                    comparisonReportFile.absolutePath
        }

        val root =
            comparisonReportFile
                .reader()
                .use { reader ->
                    JsonParser
                        .parseReader(reader)
                        .asJsonObject
                }

        val sourceVersion =
            root.requiredInt(
                name =
                    "version",
            )

        require(sourceVersion > 0) {
            "Nutrition matcher comparison report version must be positive."
        }

        val singleFeatureComparisons =
            root.requiredArray(
                name =
                    "singleFeatureComparisons",
            )
                .map { element ->
                    require(element.isJsonObject) {
                        "Every single-feature comparison must be a JSON object."
                    }

                    element.asJsonObject
                }

        val expectedFeatureNames =
            LocalNutritionMatcherFeatureContract
                .ALL_DOMAIN_FEATURE_NAMES

        require(
            expectedFeatureNames.isNotEmpty(),
        ) {
            "Nutrition Domain-Mismatch feature contract must not be empty."
        }

        require(
            expectedFeatureNames.size ==
                    expectedFeatureNames.distinct().size,
        ) {
            "Nutrition Domain-Mismatch feature contract contains duplicates."
        }

        val parsedFeatureNames =
            singleFeatureComparisons
                .map { comparison ->
                    comparison.requiredString(
                        name =
                            "featureName",
                    )
                }

        require(
            parsedFeatureNames.size ==
                    parsedFeatureNames.distinct().size,
        ) {
            val duplicates =
                parsedFeatureNames
                    .groupingBy { it }
                    .eachCount()
                    .filterValues {
                        it > 1
                    }
                    .keys
                    .sorted()

            "Nutrition single-feature comparisons contain duplicate features: " +
                    duplicates.joinToString()
        }

        val expectedFeatureNameSet =
            expectedFeatureNames.toSet()

        val parsedFeatureNameSet =
            parsedFeatureNames.toSet()

        val unknownFeatureNames =
            parsedFeatureNameSet
                .filterNot {
                    it in expectedFeatureNameSet
                }
                .sorted()

        require(unknownFeatureNames.isEmpty()) {
            "Nutrition single-feature comparisons contain unknown features: " +
                    unknownFeatureNames.joinToString()
        }

        val missingFeatureNames =
            expectedFeatureNames
                .filterNot {
                    it in parsedFeatureNameSet
                }

        require(missingFeatureNames.isEmpty()) {
            "Nutrition single-feature comparisons are missing features: " +
                    missingFeatureNames.joinToString()
        }

        require(
            singleFeatureComparisons.size ==
                    expectedFeatureNames.size,
        ) {
            "Nutrition single-feature comparison count differs from the " +
                    "Domain-Mismatch feature contract. Expected " +
                    "${expectedFeatureNames.size}, but found " +
                    "${singleFeatureComparisons.size}."
        }

        val comparisonsByFeatureName =
            singleFeatureComparisons
                .associateBy { comparison ->
                    comparison.requiredString(
                        name =
                            "featureName",
                    )
                }

        /*
         * Die Reihenfolge stammt bewusst aus dem produktiven Featurevertrag.
         * Sie wird nicht alphabetisch verändert, damit der Report dieselbe
         * deterministische Reihenfolge wie FeatureExtractor und Modell behält.
         */
        val impacts =
            expectedFeatureNames
                .map { featureName ->
                    val comparison =
                        requireNotNull(
                            comparisonsByFeatureName[
                                featureName
                            ],
                        ) {
                            "Missing single-feature comparison for: " +
                                    featureName
                        }

                    parseImpact(
                        featureName =
                            featureName,
                        comparison =
                            comparison,
                    )
                }

        validateImpacts(
            impacts =
                impacts,
            expectedFeatureNames =
                expectedFeatureNames,
        )

        val harmfulFeatureNames =
            impacts
                .filter {
                    it.classification ==
                            NutritionDomainFeatureImpactClassification.HARMFUL
                }
                .map {
                    it.featureName
                }

        val neutralFeatureNames =
            impacts
                .filter {
                    it.classification ==
                            NutritionDomainFeatureImpactClassification.NEUTRAL
                }
                .map {
                    it.featureName
                }

        val beneficialFeatureNames =
            impacts
                .filter {
                    it.classification ==
                            NutritionDomainFeatureImpactClassification.BENEFICIAL
                }
                .map {
                    it.featureName
                }

        val report =
            NutritionDomainFeatureImpactReport(
                version =
                    REPORT_VERSION,
                sourceComparisonReportVersion =
                    sourceVersion,
                domainFeatureCount =
                    impacts.size,
                harmfulCount =
                    harmfulFeatureNames.size,
                neutralCount =
                    neutralFeatureNames.size,
                beneficialCount =
                    beneficialFeatureNames.size,
                harmfulFeatureNames =
                    harmfulFeatureNames,
                neutralFeatureNames =
                    neutralFeatureNames,
                beneficialFeatureNames =
                    beneficialFeatureNames,
                impacts =
                    impacts,
            )

        validateReport(
            report =
                report,
            expectedFeatureNames =
                expectedFeatureNames,
        )

        return report
    }

    private fun parseImpact(
        featureName: String,
        comparison: JsonObject,
    ): NutritionDomainFeatureImpact {

        val delta =
            comparison.requiredObject(
                name =
                    "delta",
            )

        val deltaPrecision =
            delta.requiredFiniteDouble(
                name =
                    "precision",
            )

        val deltaRecall =
            delta.requiredFiniteDouble(
                name =
                    "recall",
            )

        val deltaF1 =
            delta.requiredFiniteDouble(
                name =
                    "f1",
            )

        val deltaBalancedAccuracy =
            delta.requiredFiniteDouble(
                name =
                    "balancedAccuracy",
            )

        val deltaFalsePositiveCount =
            delta.requiredInt(
                name =
                    "falsePositiveCount",
            )

        val deltaFalseNegativeCount =
            delta.requiredInt(
                name =
                    "falseNegativeCount",
            )

        return NutritionDomainFeatureImpact(
            featureName =
                featureName,
            deltaPrecision =
                deltaPrecision,
            deltaRecall =
                deltaRecall,
            deltaF1 =
                deltaF1,
            deltaBalancedAccuracy =
                deltaBalancedAccuracy,
            deltaFalsePositiveCount =
                deltaFalsePositiveCount,
            deltaFalseNegativeCount =
                deltaFalseNegativeCount,
            classification =
                classify(
                    deltaF1 =
                        deltaF1,
                    deltaBalancedAccuracy =
                        deltaBalancedAccuracy,
                    deltaFalsePositiveCount =
                        deltaFalsePositiveCount,
                ),
        )
    }

    private fun classify(
        deltaF1: Double,
        deltaBalancedAccuracy: Double,
        deltaFalsePositiveCount: Int,
    ): NutritionDomainFeatureImpactClassification {

        return when {
            deltaF1 < 0.0 ||
                    deltaBalancedAccuracy < 0.0 ||
                    deltaFalsePositiveCount > 0 ->
                NutritionDomainFeatureImpactClassification.HARMFUL

            deltaF1 > 0.0 &&
                    deltaBalancedAccuracy > 0.0 &&
                    deltaFalsePositiveCount <= 0 ->
                NutritionDomainFeatureImpactClassification.BENEFICIAL

            else ->
                NutritionDomainFeatureImpactClassification.NEUTRAL
        }
    }

    private fun validateImpacts(
        impacts: List<NutritionDomainFeatureImpact>,
        expectedFeatureNames: List<String>,
    ) {
        require(
            impacts.size ==
                    expectedFeatureNames.size,
        ) {
            "Nutrition Domain-Mismatch impact count differs from the " +
                    "feature contract."
        }

        require(
            impacts.map {
                it.featureName
            } ==
                    expectedFeatureNames,
        ) {
            "Nutrition Domain-Mismatch impacts differ from the expected " +
                    "deterministic feature order."
        }

        require(
            impacts.map {
                it.featureName
            }.distinct().size ==
                    impacts.size,
        ) {
            "Nutrition Domain-Mismatch impacts contain duplicate features."
        }

        require(
            impacts.all { impact ->
                impact.featureName.isNotBlank() &&
                        impact.deltaPrecision.isFinite() &&
                        impact.deltaRecall.isFinite() &&
                        impact.deltaF1.isFinite() &&
                        impact.deltaBalancedAccuracy.isFinite()
            },
        ) {
            "Nutrition Domain-Mismatch impact contains invalid values."
        }

        impacts.forEach { impact ->
            val expectedClassification =
                classify(
                    deltaF1 =
                        impact.deltaF1,
                    deltaBalancedAccuracy =
                        impact.deltaBalancedAccuracy,
                    deltaFalsePositiveCount =
                        impact.deltaFalsePositiveCount,
                )

            require(
                impact.classification ==
                        expectedClassification,
            ) {
                "Nutrition Domain-Mismatch feature " +
                        "${impact.featureName} has inconsistent impact " +
                        "classification."
            }
        }
    }

    private fun validateReport(
        report: NutritionDomainFeatureImpactReport,
        expectedFeatureNames: List<String>,
    ) {
        require(
            report.version ==
                    REPORT_VERSION,
        ) {
            "Unexpected Nutrition Domain-Mismatch impact report version."
        }

        require(
            report.sourceComparisonReportVersion > 0,
        ) {
            "Source comparison report version must be positive."
        }

        require(
            report.domainFeatureCount ==
                    expectedFeatureNames.size,
        ) {
            "Nutrition Domain-Mismatch impact report feature count differs " +
                    "from the feature contract."
        }

        require(
            report.harmfulCount +
                    report.neutralCount +
                    report.beneficialCount ==
                    report.domainFeatureCount,
        ) {
            "Nutrition Domain-Mismatch impact classification counts do not " +
                    "cover all features."
        }

        require(
            report.harmfulCount ==
                    report.harmfulFeatureNames.size,
        ) {
            "Harmful feature count differs from harmful feature names."
        }

        require(
            report.neutralCount ==
                    report.neutralFeatureNames.size,
        ) {
            "Neutral feature count differs from neutral feature names."
        }

        require(
            report.beneficialCount ==
                    report.beneficialFeatureNames.size,
        ) {
            "Beneficial feature count differs from beneficial feature names."
        }

        val classifiedFeatureNames =
            report.harmfulFeatureNames +
                    report.neutralFeatureNames +
                    report.beneficialFeatureNames

        require(
            classifiedFeatureNames.size ==
                    classifiedFeatureNames.distinct().size,
        ) {
            "A Nutrition Domain-Mismatch feature appears in more than one " +
                    "classification."
        }

        require(
            classifiedFeatureNames.toSet() ==
                    expectedFeatureNames.toSet(),
        ) {
            "Nutrition Domain-Mismatch classifications do not cover exactly " +
                    "the expected features."
        }

        require(
            report.impacts.map {
                it.featureName
            } ==
                    expectedFeatureNames,
        ) {
            "Nutrition Domain-Mismatch report impacts differ from the " +
                    "deterministic feature contract."
        }
    }

    private fun JsonObject.requiredArray(
        name: String,
    ) =
        requireNotNull(
            get(name),
        ) {
            "Missing required JSON array: $name"
        }
            .also { element ->
                require(element.isJsonArray) {
                    "Required JSON value is not an array: $name"
                }
            }
            .asJsonArray

    private fun JsonObject.requiredObject(
        name: String,
    ): JsonObject =
        requireNotNull(
            get(name),
        ) {
            "Missing required JSON object: $name"
        }
            .also { element ->
                require(element.isJsonObject) {
                    "Required JSON value is not an object: $name"
                }
            }
            .asJsonObject

    private fun JsonObject.requiredString(
        name: String,
    ): String {
        val value =
            requiredPrimitive(
                name =
                    name,
            )
                .asString

        require(value.isNotBlank()) {
            "Required JSON string must not be blank: $name"
        }

        return value
    }

    private fun JsonObject.requiredInt(
        name: String,
    ): Int =
        requiredPrimitive(
            name =
                name,
        )
            .asInt

    private fun JsonObject.requiredFiniteDouble(
        name: String,
    ): Double {
        val value =
            requiredPrimitive(
                name =
                    name,
            )
                .asDouble

        require(value.isFinite()) {
            "Required JSON number must be finite: $name"
        }

        return value
    }

    private fun JsonObject.requiredPrimitive(
        name: String,
    ): JsonElement {
        val element =
            requireNotNull(
                get(name),
            ) {
                "Missing required JSON value: $name"
            }

        require(element.isJsonPrimitive) {
            "Required JSON value is not primitive: $name"
        }

        return element
    }

    companion object {
        const val REPORT_VERSION: Int =
            1
    }
}