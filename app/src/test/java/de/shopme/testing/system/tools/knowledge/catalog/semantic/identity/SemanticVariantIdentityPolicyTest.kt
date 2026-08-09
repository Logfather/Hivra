package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType
import kotlin.test.Test
import kotlin.test.assertEquals

class SemanticVariantIdentityPolicyTest {

    @Test
    fun knowledgeClaimsAreNeverCatalogIdentity() {

        assertDecision(
            type =
                SemanticVariantType.NUTRITION_CLAIM,
            expected =
                SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
        )

        assertDecision(
            type =
                SemanticVariantType.ALLERGEN_CLAIM,
            expected =
                SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
        )

        assertDecision(
            type =
                SemanticVariantType.DIET_CLAIM,
            expected =
                SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
        )

        assertDecision(
            type =
                SemanticVariantType.GENERIC_PLACEHOLDER,
            expected =
                SemanticVariantIdentityDecision.KNOWLEDGE_ONLY
        )
    }

    @Test
    fun coreIdentityTypesRemainIdentityForming() {

        assertDecision(
            type =
                SemanticVariantType.INGREDIENT,
            expected =
                SemanticVariantIdentityDecision.IDENTITY_ALLOWED
        )

        assertDecision(
            type =
                SemanticVariantType.MATURATION,
            expected =
                SemanticVariantIdentityDecision.IDENTITY_ALLOWED
        )

        assertDecision(
            type =
                SemanticVariantType.CUT,
            expected =
                SemanticVariantIdentityDecision.IDENTITY_ALLOWED
        )
    }

    @Test
    fun conditionalTypesRemainReview() {

        assertDecision(
            type =
                SemanticVariantType.PROCESSING,
            expected =
                SemanticVariantIdentityDecision.REVIEW
        )

        assertDecision(
            type =
                SemanticVariantType.FORM,
            expected =
                SemanticVariantIdentityDecision.REVIEW
        )

        assertDecision(
            type =
                SemanticVariantType.FLAVOR,
            expected =
                SemanticVariantIdentityDecision.REVIEW
        )
    }

    private fun assertDecision(
        type: SemanticVariantType,
        expected: SemanticVariantIdentityDecision
    ) {

        val result =
            SemanticVariantIdentityPolicy
                .evaluate(type)

        assertEquals(
            expected = expected,
            actual = result.decision
        )
    }
}