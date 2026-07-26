package de.shopme.tools.knowledge.mapping.catalog.training.model

data class NutritionMatcherGptFixtureComparisonReport(
    val version: Int,
    val modelVersion: Int,
    val modelType: String,
    val featureNames: List<String>,
    val featureCount: Int,
    val decisionThreshold: Double,
    val thresholdMinimumPrecision: Double,
    val thresholdMaximumFalsePositiveRate: Double,
    val thresholdMinimumPredictedPositiveCount: Int,
    val fixtureCatalogKeyCount: Int,
    val comparableFixtureCount: Int,
    val nonComparableFixtureCount: Int,
    val gptPositiveFixtureCount: Int,
    val gptNoMatchFixtureCount: Int,
    val topOneAgreementCount: Int,
    val topOneAccuracy: Double,
    val meanGptPositiveRank: Double?,
    val medianGptPositiveRank: Double?,
    val maximumGptPositiveRank: Int?,
    val thresholdAcceptedAgreementCount: Int,
    val thresholdAcceptedAgreementRate: Double,
    val thresholdRejectedAgreementCount: Int,
    val thresholdRejectedAgreementRate: Double,
    val localFalseAcceptanceCount: Int,
    val localMissedAcceptanceCount: Int,
    val entries: List<NutritionMatcherGptFixtureComparisonEntry>,
)

data class NutritionMatcherGptFixtureComparisonEntry(
    val catalogKey: String,
    val gptDecision: String,
    val gptSelectedServerKey: String?,
    val localTopServerKey: String?,
    val localTopProbability: Double?,
    val localTopRankAgreesWithGpt: Boolean,
    val gptSelectedCandidateRank: Int?,
    val gptSelectedCandidateProbability: Double?,
    val localDecision: String,
    val localAcceptedServerKey: String?,
    val productiveDecisionAgreesWithGpt: Boolean,
    val disagreementType: NutritionMatcherGptFixtureDisagreementType?,
)

enum class NutritionMatcherGptFixtureDisagreementType {

    GPT_MATCH_LOCAL_NO_MATCH,

    GPT_NO_MATCH_LOCAL_MATCH,

    GPT_MATCH_LOCAL_DIFFERENT_MATCH,

    GPT_SELECTED_CANDIDATE_NOT_AVAILABLE,
}