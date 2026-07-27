package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.validation

enum class OFFNutritionRetrievalQualityType {

    NO_CANDIDATES,

    EXACT_ALIAS_MATCH,

    STRONG_LEXICAL_MATCH,

    LOW_SCORE,

    VERY_LOW_SCORE,

    WEAK_TOKEN_OVERLAP,

    GENERIC_MODIFIER_MATCH,

    PROCESSING_FORM_MISMATCH,

    PREPARATION_FORM_MISMATCH,

    ANIMAL_SPECIES_MISMATCH,

    PLANT_PRODUCT_MISMATCH,

    DIETARY_FORM_MISMATCH,

    UNKNOWN_RISK
}