package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketPlausibilityValidatorTest {

    private val validator =
        MarketPlausibilityValidator()

    @Test
    fun rejectsPureKnowledgeAttributeIdentity() {

        assertDecision(
            itemName = "Müsli – Proteinreich",
            category = "cereals",
            normalized = "muesli-high-protein",
            expected =
                MarketPlausibilityDecision.REJECT,
            expectedReason =
                MarketPlausibilityReason
                    .KNOWLEDGE_ONLY_IDENTITY
        )
    }

    @Test
    fun rejectsGenericPlaceholderIdentity() {

        assertDecision(
            itemName = "Brot – Fischbasiert",
            category = "bakery",
            normalized = "bread-fish-based",
            expected =
                MarketPlausibilityDecision.REJECT,
            expectedReason =
                MarketPlausibilityReason
                    .GENERIC_PLACEHOLDER
        )
    }

    @Test
    fun rejectsKnownInvalidButterIdentity() {

        assertDecision(
            itemName = "Butter – Höhlengereift",
            category = "dairy",
            normalized = "butter-cave-aged",
            expected =
                MarketPlausibilityDecision.REJECT,
            expectedReason =
                MarketPlausibilityReason
                    .EXPLICIT_FAMILY_VARIANT_REJECT
        )
    }

    @Test
    fun rejectsInherentBreadProcessingIdentity() {

        assertDecision(
            itemName = "Brot – Gebacken",
            category = "bakery",
            normalized = "bread-baked",
            expected =
                MarketPlausibilityDecision.REJECT,
            expectedReason =
                MarketPlausibilityReason
                    .INHERENT_PROCESSING_STATE
        )
    }

    @Test
    fun acceptsExplicitlyKnownBreadIdentity() {

        assertDecision(
            itemName = "Brot – Buchweizen",
            category = "bakery",
            normalized = "bread-buckwheat",
            expected =
                MarketPlausibilityDecision.ACCEPT,
            expectedReason =
                MarketPlausibilityReason
                    .EXPLICIT_FAMILY_VARIANT_ACCEPT
        )
    }

    @Test
    fun rejectsButterProcessingDisallowedByFamilyProfile() {

        assertDecision(
            itemName = "Butter – Fermentiert",
            category = "dairy",
            normalized = "butter-fermented",
            expected =
                MarketPlausibilityDecision.REJECT,
            expectedReason =
                MarketPlausibilityReason
                    .FAMILY_VARIANT_INCOMPATIBLE
        )
    }

    @Test
    fun rejectsConcreteFishFormThatIsTypeCompatibleButValueImplausible() {

        assertDecision(
            itemName = "Frischfisch – Flocken",
            category = "fish",
            normalized = "fresh-fish-flakes",
            expected =
                MarketPlausibilityDecision.REJECT,
            expectedReason =
                MarketPlausibilityReason
                    .FAMILY_VARIANT_VALUE_INCOMPATIBLE
        )
    }

    @Test
    fun rejectsPreparedWildMeatAsCanonicalMarketIdentity() {

        assertDecision(
            itemName = "Wildfleisch – Frittiert",
            category = "meat",
            normalized = "wild-meat-deep-fried",
            expected =
                MarketPlausibilityDecision.REJECT,
            expectedReason =
                MarketPlausibilityReason
                    .FAMILY_VARIANT_VALUE_INCOMPATIBLE
        )
    }

    @Test
    fun allowsKnownConcreteChickenCut() {

        assertDecision(
            itemName = "Hähnchenfleisch – Filet",
            category = "meat",
            normalized = "chicken-fillet",
            expected =
                MarketPlausibilityDecision.ACCEPT,
            expectedReason =
                MarketPlausibilityReason
                    .EXPLICIT_FAMILY_VARIANT_ACCEPT
        )
    }

    @Test
    fun unresolvedValuePolicyDoesNotOverrideSupportedFamilyIdentity() {

        assertDecision(
            itemName = "Müsli – Apfel",
            category = "breakfast",
            normalized = "muesli-apple",
            expected =
                MarketPlausibilityDecision.ACCEPT,
            expectedReason =
                MarketPlausibilityReason
                    .PROFILE_SUPPORTED_IDENTITY
        )
    }

    private fun assertDecision(
        itemName: String,
        category: String,
        normalized: String,
        expected: MarketPlausibilityDecision,
        expectedReason: MarketPlausibilityReason
    ) {

        val json =
            JsonParser
                .parseString(
                    """
                    {
                      "itemname": "$itemName",
                      "category": "$category",
                      "normalized": "$normalized"
                    }
                    """.trimIndent()
                )
                .asJsonObject

        val result =
            requireNotNull(
                validator.validate(json)
            )

        assertEquals(
            expected = expected,
            actual = result.decision
        )

        assertEquals(
            expected = expectedReason,
            actual = result.reason
        )
    }
}