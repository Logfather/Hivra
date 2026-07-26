package de.shopme.tools.knowledge.off.nutrition.reference.training

import de.shopme.tools.knowledge.off.nutrition.reference.validation
.OFFNutritionReferenceAggregateValidationStatus

/**
 * Kanonische OFF-Nutrition-Referenz für Candidate Retrieval und die spätere
 * Erzeugung gelabelter Catalog→Server-Trainingsbeispiele.
 *
 * Diese Struktur enthält bewusst kein positives oder negatives Trainingslabel.
 */
data class OFFNutritionMatcherTrainingCandidate(
    val serverArtifact: String,
    val serverKey: String,
    val canonicalId: String,
    val retrievalAliases: List<String>,
    val canonicalAliases: List<String>,
    val matchAliases: List<String>,
    val singleIngredientNutritionAliases: List<String>,
    val nutrition: Map<String, Double>,
    val profileCount: Int,
    val nutrientCount: Int,
    val validationStatus:
    OFFNutritionReferenceAggregateValidationStatus,
    val warningCount: Int,
    val validationIssueTypes: List<String>,
    val representativeSourceId: String,
    val sourceIds: List<String>,
    val source: String,
    val sourceVersion: String,
    val sourceConfidence: Double
) {

    init {
        require(serverArtifact == SERVER_ARTIFACT) {
            "OFF matcher training candidate must target $SERVER_ARTIFACT."
        }

        require(serverKey.isNotBlank()) {
            "Matcher training candidate serverKey must not be blank."
        }

        require(canonicalId.isNotBlank()) {
            "Matcher training candidate canonicalId must not be blank."
        }

        require(serverKey == canonicalId) {
            "serverKey must equal canonicalId for canonical OFF references."
        }

        require(retrievalAliases.isNotEmpty()) {
            "Matcher training candidate must contain retrieval aliases."
        }

        require(
            retrievalAliases ==
                    retrievalAliases
                        .distinct()
                        .sorted()
        ) {
            "retrievalAliases must be unique and sorted."
        }

        require(
            canonicalAliases ==
                    canonicalAliases
                        .distinct()
                        .sorted()
        ) {
            "canonicalAliases must be unique and sorted."
        }

        require(
            matchAliases ==
                    matchAliases
                        .distinct()
                        .sorted()
        ) {
            "matchAliases must be unique and sorted."
        }

        require(
            singleIngredientNutritionAliases ==
                    singleIngredientNutritionAliases
                        .distinct()
                        .sorted()
        ) {
            "singleIngredientNutritionAliases must be unique and sorted."
        }

        require(nutrition.isNotEmpty()) {
            "Matcher training candidate nutrition must not be empty."
        }

        require(
            nutrition ==
                    nutrition.toSortedMap()
        ) {
            "Matcher training candidate nutrition must be sorted."
        }

        require(
            nutrition.values.all { value ->
                value.isFinite()
            }
        ) {
            "Matcher training candidate nutrition values must be finite."
        }

        require(profileCount > 0) {
            "Matcher training candidate profileCount must be positive."
        }

        require(nutrientCount == nutrition.size) {
            "nutrientCount must equal nutrition.size."
        }

        require(warningCount >= 0) {
            "warningCount must not be negative."
        }

        require(
            validationIssueTypes ==
                    validationIssueTypes
                        .distinct()
                        .sorted()
        ) {
            "validationIssueTypes must be unique and sorted."
        }

        require(representativeSourceId.isNotBlank()) {
            "representativeSourceId must not be blank."
        }

        require(sourceIds.isNotEmpty()) {
            "sourceIds must not be empty."
        }

        require(
            sourceIds ==
                    sourceIds
                        .distinct()
                        .sorted()
        ) {
            "sourceIds must be unique and sorted."
        }

        require(representativeSourceId in sourceIds) {
            "representativeSourceId must be contained in sourceIds."
        }

        require(source.isNotBlank()) {
            "source must not be blank."
        }

        require(sourceVersion.isNotBlank()) {
            "sourceVersion must not be blank."
        }

        require(
            sourceConfidence.isFinite() &&
                    sourceConfidence in 0.0..1.0
        ) {
            "sourceConfidence must be finite and within 0.0..1.0."
        }
    }

    companion object {

        const val SERVER_ARTIFACT =
            "nutrition.json"
    }
}