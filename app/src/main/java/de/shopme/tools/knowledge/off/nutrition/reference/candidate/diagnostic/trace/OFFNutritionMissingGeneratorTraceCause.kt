package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace

enum class OFFNutritionMissingGeneratorTraceCause {

    SOURCE_CANDIDATE_NOT_FOUND,

    REMOVED_BY_QUALITY_FILTER,

    REMOVED_BY_DEDUPLICATION,

    TRACE_NOT_EMITTED,

    ARTIFACT_IDENTITY_MISMATCH
}