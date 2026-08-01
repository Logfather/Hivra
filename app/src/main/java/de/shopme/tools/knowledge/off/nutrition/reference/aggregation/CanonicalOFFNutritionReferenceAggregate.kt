package de.shopme.tools.knowledge.off.nutrition.reference.aggregation

data class CanonicalOFFNutritionReferenceAggregate(
    val canonicalId: String,
    val aliases: Set<String>,
    val matchAliases: Set<String>,
    val singleIngredientNutritionAliases: Set<String>,
    val nutrition: Map<String, Double>,
    val nutrientStatistics:
    Map<String, OFFNutritionReferenceNutrientStatistics>,
    val profileCount: Int,
    val sourceIds: List<String>,
    val representativeSourceId: String,
    val source: String,
    val sourceVersion: String,
    val sourceConfidence: Double
) {

    init {
        require(canonicalId.isNotBlank()) {
            "Aggregate canonicalId must not be blank."
        }

        require(profileCount > 0) {
            "Aggregate profileCount must be positive."
        }

        require(sourceIds.size == profileCount) {
            "Aggregate sourceIds.size must equal profileCount."
        }

        require(sourceIds == sourceIds.distinct().sorted()) {
            "Aggregate sourceIds must be unique and sorted."
        }

        require(representativeSourceId in sourceIds) {
            "Aggregate representativeSourceId must occur in sourceIds."
        }

        require(nutrition.isNotEmpty()) {
            "Aggregate nutrition must not be empty."
        }

        require(
            nutrition.keys ==
                    nutrientStatistics.keys
        ) {
            "Aggregate nutrition and nutrientStatistics must have " +
                    "identical keys."
        }

        require(
            nutrition ==
                    nutrition.toSortedMap()
        ) {
            "Aggregate nutrition must be sorted by key."
        }

        require(
            nutrientStatistics ==
                    nutrientStatistics.toSortedMap()
        ) {
            "Aggregate nutrientStatistics must be sorted by key."
        }

        require(
            nutrition.values.all(
                Double::isFinite
            )
        ) {
            "Aggregate nutrition values must be finite."
        }

        require(
            nutrition.all { (nutrientKey, value) ->

                val statistics =
                    nutrientStatistics[
                        nutrientKey
                    ]
                        ?: return@all false

                value >=
                        statistics.minimum &&
                        value <=
                        statistics.maximum
            }
        ) {
            "Aggregate nutrition values must stay within persisted " +
                    "observation ranges."
        }

        require(aliases == aliases.toSortedSet()) {
            "Aggregate aliases must be sorted."
        }

        require(matchAliases == matchAliases.toSortedSet()) {
            "Aggregate matchAliases must be sorted."
        }

        require(
            singleIngredientNutritionAliases ==
                    singleIngredientNutritionAliases.toSortedSet()
        ) {
            "Aggregate single-ingredient aliases must be sorted."
        }

        require(source.isNotBlank()) {
            "Aggregate source must not be blank."
        }

        require(sourceVersion.isNotBlank()) {
            "Aggregate sourceVersion must not be blank."
        }

        require(sourceConfidence in 0.0..1.0) {
            "Aggregate sourceConfidence must be between 0 and 1."
        }
    }
}