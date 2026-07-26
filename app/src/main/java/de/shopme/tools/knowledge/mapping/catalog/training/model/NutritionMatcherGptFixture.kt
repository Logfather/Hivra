package de.shopme.tools.knowledge.mapping.catalog.training.model

data class NutritionMatcherGptFixture(
    val catalogKey: String,
    val gptDecision: NutritionMatcherGptFixtureDecision,
    val gptSelectedServerKey: String?,
    val candidates: List<LocalNutritionMatcherCandidate>,
)

enum class NutritionMatcherGptFixtureDecision {

    MATCH,

    NO_MATCH,
}