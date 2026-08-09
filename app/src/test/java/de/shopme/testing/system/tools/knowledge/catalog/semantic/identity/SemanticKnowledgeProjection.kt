package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

enum class SemanticKnowledgeDimension {
    NUTRITION,
    ALLERGENS,
    DIET_CLASSIFICATION,
    PROCESSING,
    COMPOSITION,
    STORAGE,
    PRODUCTION,
    FLAVOR,
    FORM,
    OTHER
}

object SemanticKnowledgeProjection {

    fun dimensionFor(
        type: SemanticVariantType
    ): SemanticKnowledgeDimension {

        return when (type) {

            SemanticVariantType.NUTRITION_CLAIM ->
                SemanticKnowledgeDimension.NUTRITION

            SemanticVariantType.ALLERGEN_CLAIM ->
                SemanticKnowledgeDimension.ALLERGENS

            SemanticVariantType.DIET_CLAIM ->
                SemanticKnowledgeDimension.DIET_CLASSIFICATION

            SemanticVariantType.PROCESSING,
            SemanticVariantType.PREPARATION ->
                SemanticKnowledgeDimension.PROCESSING

            SemanticVariantType.COMPOSITION ->
                SemanticKnowledgeDimension.COMPOSITION

            SemanticVariantType.STORAGE_STATE ->
                SemanticKnowledgeDimension.STORAGE

            SemanticVariantType.PRODUCTION_METHOD ->
                SemanticKnowledgeDimension.PRODUCTION

            SemanticVariantType.FLAVOR,
            SemanticVariantType.TEXTURE ->
                SemanticKnowledgeDimension.FLAVOR

            SemanticVariantType.FORM,
            SemanticVariantType.CUT ->
                SemanticKnowledgeDimension.FORM

            else ->
                SemanticKnowledgeDimension.OTHER
        }
    }
}