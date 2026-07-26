package de.shopme.tools.knowledge.off.nutrition.reference

/**
 * Ein einzelner kanonischer Nutrition-Referenzkandidat aus Open Food Facts.
 *
 * Der Kandidat repräsentiert weiterhin genau ein OFF-Produkt und darf in dieser
 * Phase noch nicht anhand seines kanonischen Namens mit anderen Produkten
 * zusammengeführt werden.
 */
data class CanonicalOFFNutritionReferenceCandidate(
    val sourceId: String,
    val canonicalId: String,
    val aliases: Set<String>,
    val matchAliases: Set<String>,
    val nutrition: Map<String, Double>,
    val productName: String?,
    val brand: String?,
    val categories: String?,
    val singleIngredientNutritionAliases: Set<String>,
    val source: String,
    val sourceVersion: String,
    val sourceConfidence: Double
)