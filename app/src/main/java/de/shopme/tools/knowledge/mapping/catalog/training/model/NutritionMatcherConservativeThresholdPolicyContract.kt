package de.shopme.tools.knowledge.mapping.catalog.training.model

object NutritionMatcherConservativeThresholdPolicyContract {

    val CURRENT_POLICY =
        NutritionMatcherThresholdOptimizationPolicy(
            minimumPrecision =
                0.95,
            maximumFalsePositiveRate =
                0.02,
            minimumPredictedPositiveCount =
                5,
        )

    val CONSERVATIVE_POLICY =
        NutritionMatcherThresholdOptimizationPolicy(
            minimumPrecision =
                0.97,
            maximumFalsePositiveRate =
                0.01,
            minimumPredictedPositiveCount =
                5,
        )

    val STRICT_POLICY =
        NutritionMatcherThresholdOptimizationPolicy(
            minimumPrecision =
                0.98,
            maximumFalsePositiveRate =
                0.005,
            minimumPredictedPositiveCount =
                5,
        )

    val VERY_STRICT_POLICY =
        NutritionMatcherThresholdOptimizationPolicy(
            minimumPrecision =
                0.99,
            maximumFalsePositiveRate =
                0.0025,
            minimumPredictedPositiveCount =
                5,
        )

    val ACTIVE_POLICY =
        CONSERVATIVE_POLICY

    const val ACTIVE_CANDIDATE_NAME =
        "CONSERVATIVE_97"

    val CANDIDATES: List<NutritionMatcherThresholdPolicyCandidate> =
        listOf(
            NutritionMatcherThresholdPolicyCandidate(
                name =
                    "CURRENT_95",
                policy =
                    CURRENT_POLICY,
                conservatismRank =
                    0,
            ),
            NutritionMatcherThresholdPolicyCandidate(
                name =
                    "CONSERVATIVE_97",
                policy =
                    CONSERVATIVE_POLICY,
                conservatismRank =
                    1,
            ),
            NutritionMatcherThresholdPolicyCandidate(
                name =
                    "STRICT_98",
                policy =
                    STRICT_POLICY,
                conservatismRank =
                    2,
            ),
            NutritionMatcherThresholdPolicyCandidate(
                name =
                    "VERY_STRICT_99",
                policy =
                    VERY_STRICT_POLICY,
                conservatismRank =
                    3,
            ),
        )

    init {
        require(
            CANDIDATES.isNotEmpty(),
        ) {
            "Nutrition threshold policy candidates must not be empty."
        }

        require(
            CANDIDATES.map { candidate ->
                candidate.name
            }
                .distinct()
                .size ==
                    CANDIDATES.size,
        ) {
            "Nutrition threshold policy candidate names must be unique."
        }

        require(
            CANDIDATES.map { candidate ->
                candidate.conservatismRank
            }
                .distinct()
                .size ==
                    CANDIDATES.size,
        ) {
            "Nutrition threshold policy conservatism ranks must be unique."
        }

        require(
            CANDIDATES.zipWithNext()
                .all { (left, right) ->
                    right.policy.minimumPrecision >=
                            left.policy.minimumPrecision &&
                            right.policy.maximumFalsePositiveRate <=
                            left.policy.maximumFalsePositiveRate &&
                            right.conservatismRank >
                            left.conservatismRank
                },
        ) {
            "Nutrition threshold policies must become monotonically " +
                    "more conservative."
        }

        require(
            ACTIVE_POLICY ==
                    CONSERVATIVE_POLICY,
        ) {
            "Active nutrition threshold policy must use the deterministically " +
                    "selected conservative policy."
        }

        require(
            CANDIDATES.single { candidate ->
                candidate.name ==
                        ACTIVE_CANDIDATE_NAME
            }
                .policy ==
                    ACTIVE_POLICY,
        ) {
            "Active nutrition threshold policy candidate is inconsistent."
        }
    }
}

data class NutritionMatcherThresholdPolicyCandidate(
    val name: String,
    val policy: NutritionMatcherThresholdOptimizationPolicy,
    val conservatismRank: Int,
)