package de.shopme.tools.knowledge.him.sources.off

/**
 * Clean, source-native representation of one Open Food Facts product
 * prepared as deterministic evidence for HIM.
 *
 * This contract intentionally does not contain:
 *
 * - canonical ShopMe identities
 * - catalog mappings
 * - PRE-HIM proposal / merge structures
 * - embedded Agribalyse knowledge
 * - historical OFF calculation trees
 * - OFF operational metadata
 *
 * The raw OFF source remains authoritative and immutable.
 * This record is a deterministic projection of relevant source evidence.
 */
data class OFFHimSourceRecord(
    val source: OFFHimSourceIdentity,
    val identity: OFFHimProductIdentity,
    val taxonomy: OFFHimTaxonomyEvidence?,
    val ingredients: OFFHimIngredientsEvidence?,
    val nutrition: OFFHimNutritionEvidence?,
    val classification: OFFHimClassificationEvidence?,
    val allergens: OFFHimAllergenEvidence?,
    val geography: OFFHimGeographyEvidence?,
    val environmentalEvidence: OFFHimEnvironmentalEvidence?,
    val packagingEvidence: OFFHimPackagingEvidence?,
    val quality: OFFHimQualityEvidence,
    val provenance: OFFHimProvenance
)

data class OFFHimSourceIdentity(
    val code: String
)

data class OFFHimProductIdentity(
    val productName: String?,
    val productNameGerman: String?,
    val productNameEnglish: String?,
    val genericName: String?,
    val genericNameGerman: String?,
    val genericNameEnglish: String?,
    val brands: List<String>,
    val productType: String?,
    val quantity: String?,
    val servingSize: String?
)

data class OFFHimTaxonomyEvidence(
    val categories: List<String>,
    val categoryHierarchy: List<String>,
    val foodGroups: List<String>,
    val pnnsGroups: List<String>,
    val mainCategory: String?,
    val ciqualReferences: List<String>
)

data class OFFHimIngredientsEvidence(
    val text: String?,
    val tags: List<String>,
    val hierarchy: List<String>,
    val items: List<OFFHimIngredient>,
    val knownIngredientCount: Int?,
    val unknownIngredientCount: Int?
)

data class OFFHimIngredient(
    val id: String?,
    val text: String?,
    val percent: Double?,
    val percentMin: Double?,
    val percentMax: Double?,
    val percentEstimate: Double?,
    val vegetarian: String?,
    val vegan: String?,
    val ciqualFoodCode: String?,
    val ecobalyseCode: String?,
    val ingredients: List<OFFHimIngredient>
)

data class OFFHimNutritionEvidence(
    val declared: Map<String, OFFHimNutrientValue>,
    val structured: OFFHimStructuredNutrition?,
    val nutrientLevels: Map<String, String>,
    val vitamins: List<String>,
    val minerals: List<String>,
    val aminoAcids: List<String>,
    val nucleotides: List<String>,
    val otherNutritionalSubstances: List<String>,
    val estimated: OFFHimEstimatedNutritionEvidence?
)

data class OFFHimNutrientValue(
    val value: Double?,
    val unit: String?,
    val per100g: Double?,
    val perServing: Double?
)

data class OFFHimStructuredNutrition(
    val inputSets: List<OFFHimNutritionInputSet>,
    val aggregated: Map<String, OFFHimStructuredNutrient>
)

data class OFFHimNutritionInputSet(
    val source: String?,
    val sourceDescription: String?,
    val per: String?,
    val preparation: String?,
    val nutrients: Map<String, OFFHimStructuredNutrient>
)

data class OFFHimStructuredNutrient(
    val value: Double?,
    val unit: String?,
    val source: String?,
    val sourceIndex: Int?,
    val sourcePer: String?
)

data class OFFHimEstimatedNutritionEvidence(
    val fruitsVegetablesNutsPercentEstimate: Double?,
    val recipeEstimatorAvailable: Boolean
)

data class OFFHimClassificationEvidence(
    val nutriScore: OFFHimNutriScoreEvidence?,
    val nova: OFFHimNovaEvidence?,
    val additives: List<String>,
    val labels: List<String>
)

data class OFFHimNutriScoreEvidence(
    val grade: String?,
    val score: Int?,
    val version: String?
)

data class OFFHimNovaEvidence(
    val group: Int?,
    val tags: List<String>,
    val markers: List<String>
)

data class OFFHimAllergenEvidence(
    val allergens: List<String>,
    val traces: List<String>,
    val allergensFromIngredients: List<String>,
    val fieldSources: Map<String, List<String>>
)

data class OFFHimGeographyEvidence(
    val origins: List<String>,
    val manufacturingPlaces: List<String>,
    val countries: List<String>,
    val purchasePlaces: List<String>,
    val regulatoryCodes: List<String>
)

data class OFFHimEnvironmentalEvidence(
    val score: OFFHimEnvironmentalScoreEvidence?,
    val origin: OFFHimEnvironmentalOriginEvidence?,
    val packaging: OFFHimEnvironmentalPackagingEvidence?,
    val productionSystem: OFFHimProductionSystemEvidence?,
    val threatenedSpecies: OFFHimThreatenedSpeciesEvidence?,
    val forestFootprint: OFFHimForestFootprintEvidence?,
    val diagnostics: OFFHimEnvironmentalDiagnostics?,
    val legacyCarbonKnownIngredientPercent: Double?
)

data class OFFHimEnvironmentalScoreEvidence(
    val grade: String?,
    val score: Double?,
    val tags: List<String>,
    val legacyEcoScoreGrade: String?,
    val legacyEcoScoreScore: Double?
)

data class OFFHimEnvironmentalOriginEvidence(
    val aggregatedOrigins: List<OFFHimOriginShare>,
    val originsFromCategories: List<String>,
    val originsFromSourceField: List<String>,
    val epiScore: Double?,
    val epiValue: Double?,
    val warning: String?
)

data class OFFHimOriginShare(
    val origin: String,
    val percent: Double?
)

data class OFFHimEnvironmentalPackagingEvidence(
    val value: Double?,
    val score: Double?,
    val nonRecyclableAndNonBiodegradableMaterials: Boolean?,
    val warning: String?
)

data class OFFHimProductionSystemEvidence(
    val labels: List<String>,
    val value: Double?,
    val warning: String?
)

data class OFFHimThreatenedSpeciesEvidence(
    val ingredient: String?,
    val value: Double?,
    val warning: String?
)

data class OFFHimForestFootprintEvidence(
    val footprintPerKg: Double?,
    val grade: String?
)

data class OFFHimEnvironmentalDiagnostics(
    val status: String?,
    val missingKeyData: Boolean?,
    val missingAgribalyseMatch: Boolean?,
    val missingDataWarning: String?,
    val missingCategories: Boolean?,
    val missingIngredients: Boolean?,
    val missingLabels: Boolean?,
    val missingOrigins: Boolean?,
    val missingPackaging: Boolean?,
    val missingAgribalyseCategory: Boolean?,
    val notApplicableForCategory: Boolean?
)

data class OFFHimPackagingEvidence(
    val taxonomy: List<String>,
    val hierarchy: List<String>,
    val materials: List<String>,
    val shapes: List<String>,
    val recycling: List<String>,
    val items: List<OFFHimPackagingItem>,
    val complete: Boolean?
)

data class OFFHimPackagingItem(
    val material: String?,
    val shape: String?,
    val numberOfUnits: Double?,
    val quantityPerUnit: OFFHimQuantity?,
    val measuredWeight: Double?,
    val specifiedWeight: Double?,
    val recycling: String?,
    val foodContact: Boolean?
)

data class OFFHimQuantity(
    val value: Double?,
    val unit: String?,
    val raw: String?
)

data class OFFHimQualityEvidence(
    val completeness: Double?,
    val complete: Boolean?,
    val obsolete: Boolean?,
    val warnings: List<String>,
    val errors: List<String>,
    val info: List<String>,
    val unknownNutrients: List<String>,
    val scans: Long?,
    val uniqueScans: Long?
)

data class OFFHimProvenance(
    val language: String?,
    val languages: List<String>,
    val sources: List<OFFHimSourceReference>,
    val fieldSources: Map<String, List<String>>,
    val createdAtEpochSeconds: Long?,
    val lastModifiedAtEpochSeconds: Long?,
    val lastUpdatedAtEpochSeconds: Long?
)

data class OFFHimSourceReference(
    val id: String?,
    val url: String?,
    val fields: List<String>,
    val importedAtEpochSeconds: Long?
)