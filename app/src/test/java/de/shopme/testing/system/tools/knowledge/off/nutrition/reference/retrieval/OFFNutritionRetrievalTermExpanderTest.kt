package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.retrieval

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.OFFNutritionRetrievalTermExpander
import kotlin.test.Test
import kotlin.test.assertTrue

class OFFNutritionRetrievalTermExpanderTest {

    private val expander =
        OFFNutritionRetrievalTermExpander()

    @Test
    fun expand_addsSingularForm() {

        val result =
            expander.expand(
                terms =
                    listOf(
                        "nectarines"
                    )
            )

        assertTrue(
            "nectarine" in result
        )
    }

    @Test
    fun expand_addsPluralForm() {

        val result =
            expander.expand(
                terms =
                    listOf(
                        "nectarine"
                    )
            )

        assertTrue(
            "nectarines" in result
        )
    }

    @Test
    fun expand_inflectsLastTokenOfCompoundTerm() {

        val result =
            expander.expand(
                terms =
                    listOf(
                        "veal schnitzels"
                    )
            )

        assertTrue(
            "veal schnitzel" in result
        )
    }
}