package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.policy

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.policy.ResultingNutritionMacronutrientWarningPolicy
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.policy.ResultingNutritionMacronutrientWarningPolicyStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultingNutritionMacronutrientWarningPolicyTest {

    private val policy =
        ResultingNutritionMacronutrientWarningPolicy()

    @Test
    fun policy_excludesFiberFromCoreMacronutrientSum() {

        val decision =
            policy.evaluate(
                fat =
                    10.0,
                carbohydrates =
                    70.0,
                protein =
                    15.0
            )

        assertTrue(
            decision.evaluated
        )

        assertEquals(
            95.0,
            decision.coreMacronutrientSumGrams
        )

        assertFalse(
            decision.exceedsMaximum
        )

        assertFalse(
            decision.reportWarning
        )

        assertEquals(
            sortedSetOf(
                "carbohydrates",
                "fat",
                "protein"
            ),
            policy.includedNutrients
        )

        assertEquals(
            sortedSetOf(
                "fiber"
            ),
            policy.excludedNutrients
        )
    }

    @Test
    fun policy_ignoresRemainingCoreMacronutrientExcess() {

        val decision =
            policy.evaluate(
                fat =
                    40.0,
                carbohydrates =
                    60.0,
                protein =
                    20.0
            )

        assertTrue(
            decision.evaluated
        )

        assertEquals(
            120.0,
            decision.coreMacronutrientSumGrams
        )

        assertTrue(
            decision.exceedsMaximum
        )

        assertFalse(
            decision.reportWarning
        )

        assertFalse(
            policy.reportCoreMacronutrientExcessAsWarning
        )

        assertEquals(
            ResultingNutritionMacronutrientWarningPolicyStatus.APPROVED,
            policy.status
        )
    }

    @Test
    fun policy_doesNotEvaluateIncompleteCoreMacronutrients() {

        val decision =
            policy.evaluate(
                fat =
                    40.0,
                carbohydrates =
                    70.0,
                protein =
                    null
            )

        assertFalse(
            decision.evaluated
        )

        assertFalse(
            decision.exceedsMaximum
        )

        assertFalse(
            decision.reportWarning
        )
    }

    @Test
    fun policy_persistsAnalyzedRationale() {

        assertEquals(
            512_102L,
            policy.rationale.analyzedEntryCount
        )

        assertEquals(
            11_658L,
            policy.rationale.previousWarningCount
        )

        assertEquals(
            11_636L,
            policy.rationale
                .fiberDoubleCountingCandidateCount
        )

        assertEquals(
            22L,
            policy.rationale
                .coreMacronutrientExcessCount
        )
    }
}