package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

enum class SemanticVariantIdentityDecision {
    IDENTITY_ALLOWED,
    KNOWLEDGE_ONLY,
    REVIEW
}

enum class SemanticVariantIdentityReason {
    IDENTITY_FORMING_TYPE,
    KNOWLEDGE_ONLY_TYPE,
    CONDITIONAL_IDENTITY_TYPE
}

data class SemanticVariantIdentityPolicyResult(
    val type: SemanticVariantType,
    val decision: SemanticVariantIdentityDecision,
    val reason: SemanticVariantIdentityReason
)

object SemanticVariantIdentityPolicy {

    private val knowledgeOnlyTypes =
        setOf(
            SemanticVariantType.NUTRITION_CLAIM,
            SemanticVariantType.ALLERGEN_CLAIM,
            SemanticVariantType.DIET_CLAIM,
            SemanticVariantType.GENERIC_PLACEHOLDER
        )

    private val identityFormingTypes =
        setOf(
            SemanticVariantType.INGREDIENT,
            SemanticVariantType.MATURATION,
            SemanticVariantType.CUT
        )

    private val conditionalIdentityTypes =
        setOf(
            SemanticVariantType.PROCESSING,
            SemanticVariantType.PREPARATION,
            SemanticVariantType.FORM,
            SemanticVariantType.TEXTURE,
            SemanticVariantType.FLAVOR,
            SemanticVariantType.COMPOSITION,
            SemanticVariantType.PRODUCTION_METHOD,
            SemanticVariantType.STORAGE_STATE,
            SemanticVariantType.STYLE
        )

    fun evaluate(
        type: SemanticVariantType
    ): SemanticVariantIdentityPolicyResult {

        return when {

            type in knowledgeOnlyTypes ->
                SemanticVariantIdentityPolicyResult(
                    type = type,
                    decision =
                        SemanticVariantIdentityDecision.KNOWLEDGE_ONLY,
                    reason =
                        SemanticVariantIdentityReason.KNOWLEDGE_ONLY_TYPE
                )

            type in identityFormingTypes ->
                SemanticVariantIdentityPolicyResult(
                    type = type,
                    decision =
                        SemanticVariantIdentityDecision.IDENTITY_ALLOWED,
                    reason =
                        SemanticVariantIdentityReason.IDENTITY_FORMING_TYPE
                )

            type in conditionalIdentityTypes ->
                SemanticVariantIdentityPolicyResult(
                    type = type,
                    decision =
                        SemanticVariantIdentityDecision.REVIEW,
                    reason =
                        SemanticVariantIdentityReason.CONDITIONAL_IDENTITY_TYPE
                )

            else ->
                error(
                    "Unclassified semantic variant type: $type"
                )
        }
    }
}