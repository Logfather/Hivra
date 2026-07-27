package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.missing

enum class MissingOFFNutritionRetrievalCandidateType {

    SOURCE_TERM_MISSING,

    SINGULAR_PLURAL_MISMATCH,

    COMPOUND_TERM_MISMATCH,

    NORMALIZATION_MISMATCH,

    ALIAS_NOT_INDEXED,

    BRAND_OR_REGIONAL_TERM,

    NON_FOOD_OR_CATALOG_ANOMALY,

    UNKNOWN
}