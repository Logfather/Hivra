package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherConservativeThresholdPolicyContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutritionMatcherConservativeThresholdPolicyContractTest {

    @Test
    fun activePolicyUsesConservative97Candidate() {

        val candidate =
            NutritionMatcherConservativeThresholdPolicyContract
                .CANDIDATES
                .single { candidate ->
                    candidate.name ==
                            NutritionMatcherConservativeThresholdPolicyContract
                                .ACTIVE_CANDIDATE_NAME
                }

        assertEquals(
            expected =
                NutritionMatcherConservativeThresholdPolicyContract
                    .CONSERVATIVE_POLICY,
            actual =
                NutritionMatcherConservativeThresholdPolicyContract
                    .ACTIVE_POLICY,
        )

        assertEquals(
            expected =
                NutritionMatcherConservativeThresholdPolicyContract
                    .ACTIVE_POLICY,
            actual =
                candidate.policy,
        )

        assertEquals(
            expected =
                0.97,
            actual =
                candidate.policy.minimumPrecision,
        )

        assertEquals(
            expected =
                0.01,
            actual =
                candidate.policy.maximumFalsePositiveRate,
        )

        assertEquals(
            expected =
                5,
            actual =
                candidate.policy.minimumPredictedPositiveCount,
        )

        assertTrue(
            actual =
                candidate.conservatismRank > 0,
        )
    }
}