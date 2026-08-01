package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictClassification
import java.util.Collections

data class ResultingNutritionConflictPolicy(
    val version: Int =
        CURRENT_VERSION,
    val status:
    ResultingNutritionConflictPolicyStatus =
        ResultingNutritionConflictPolicyStatus.APPROVED,
    val maximumEntryConflictRate: Double =
        DEFAULT_MAXIMUM_ENTRY_CONFLICT_RATE,
    val maximumNutrientConflictRate: Double =
        DEFAULT_MAXIMUM_NUTRIENT_CONFLICT_RATE,
    val maximumExtremeConflictCount: Long =
        DEFAULT_MAXIMUM_EXTREME_CONFLICT_COUNT,
    val requireCompleteEvaluation: Boolean =
        true,
    val requireCompleteConflictEvidence: Boolean =
        true,
    val automaticCorrectionAllowed: Boolean =
        false,
    val automaticEntryRejectionAllowed: Boolean =
        false,
    val classificationPolicies:
    Map<
            ResultingNutritionConflictClassification,
            ResultingNutritionConflictClassificationPolicy
            > =
        defaultClassificationPolicies(),
    val rationale:
    ResultingNutritionConflictPolicyRationale =
        ResultingNutritionConflictPolicyRationale()
) {

    init {
        require(version > 0) {
            "Nutrition conflict policy version must be positive."
        }

        require(status == ResultingNutritionConflictPolicyStatus.APPROVED) {
            "Productive Nutrition conflict policy must be approved."
        }

        require(maximumEntryConflictRate in 0.0..1.0) {
            "Maximum entry conflict rate must be between 0 and 1."
        }

        require(maximumNutrientConflictRate in 0.0..1.0) {
            "Maximum nutrient conflict rate must be between 0 and 1."
        }

        require(maximumExtremeConflictCount >= 0L) {
            "Maximum extreme conflict count must not be negative."
        }

        require(requireCompleteEvaluation) {
            "Approved policy must require complete conflict evaluation."
        }

        require(requireCompleteConflictEvidence) {
            "Approved policy must require complete conflict evidence."
        }

        require(!automaticCorrectionAllowed) {
            "Approved policy must not allow automatic correction."
        }

        require(!automaticEntryRejectionAllowed) {
            "Approved policy must not allow automatic entry rejection."
        }

        require(
            classificationPolicies.keys ==
                    ResultingNutritionConflictClassification
                        .entries
                        .toSet()
        ) {
            "Classification policies must cover every Nutrition " +
                    "conflict classification."
        }

        require(
            classificationPolicies.all { (classification, rule) ->
                classification ==
                        rule.classification
            }
        ) {
            "Classification policy keys must match their rules."
        }

        require(
            classificationPolicies ==
                    classificationPolicies.toSortedMap(
                        compareBy { classification ->
                            classification.name
                        }
                    )
        ) {
            "Classification policies must be sorted by classification."
        }
    }

    companion object {

        const val CURRENT_VERSION =
            1

        /**
         * 0,10 Prozent.
         *
         * Der produktiv gemessene Wert beträgt rund 0,080217 Prozent.
         */
        const val DEFAULT_MAXIMUM_ENTRY_CONFLICT_RATE =
            0.001

        /**
         * 0,05 Prozent.
         *
         * Der produktiv gemessene Wert beträgt rund 0,047333 Prozent.
         */
        const val DEFAULT_MAXIMUM_NUTRIENT_CONFLICT_RATE =
            0.0005

        /**
         * Der produktive Datensatz enthält 49 EXTREME-Konflikte.
         */
        const val DEFAULT_MAXIMUM_EXTREME_CONFLICT_COUNT =
            50L

        fun defaultClassificationPolicies():
                Map<
                        ResultingNutritionConflictClassification,
                        ResultingNutritionConflictClassificationPolicy
                        > {

            val policies =
                listOf(
                    ResultingNutritionConflictClassificationPolicy(
                        classification =
                            ResultingNutritionConflictClassification
                                .LIKELY_ENERGY_UNIT_CONVERSION_MISMATCH,
                        action =
                            ResultingNutritionConflictPolicyAction
                                .RETAIN_AND_REVIEW,
                        automaticCorrectionAllowed =
                            false,
                        automaticEntryRejectionAllowed =
                            false,
                        requiresPersistedEvidence =
                            true,
                        rationale =
                            "A kcal/kJ-like ratio is diagnostic evidence " +
                                    "only. The pipeline must not infer or " +
                                    "apply a unit conversion automatically."
                    ),
                    ResultingNutritionConflictClassificationPolicy(
                        classification =
                            ResultingNutritionConflictClassification
                                .LIKELY_UNIFORM_SCALE_MISMATCH,
                        action =
                            ResultingNutritionConflictPolicyAction
                                .RETAIN_AND_REVIEW,
                        automaticCorrectionAllowed =
                            false,
                        automaticEntryRejectionAllowed =
                            false,
                        requiresPersistedEvidence =
                            true,
                        rationale =
                            "A uniform scale factor can indicate a " +
                                    "serving-size or basis mismatch, but " +
                                    "does not prove which side is correct."
                    ),
                    ResultingNutritionConflictClassificationPolicy(
                        classification =
                            ResultingNutritionConflictClassification
                                .MULTI_NUTRIENT_PROFILE_CONFLICT,
                        action =
                            ResultingNutritionConflictPolicyAction
                                .RETAIN_AND_REPORT,
                        automaticCorrectionAllowed =
                            false,
                        automaticEntryRejectionAllowed =
                            false,
                        requiresPersistedEvidence =
                            true,
                        rationale =
                            "Multiple divergent nutrients indicate " +
                                    "competing profiles. The deterministic " +
                                    "runtime value is retained while the " +
                                    "conflict remains observable."
                    ),
                    ResultingNutritionConflictClassificationPolicy(
                        classification =
                            ResultingNutritionConflictClassification
                                .SINGLE_NUTRIENT_CONFLICT,
                        action =
                            ResultingNutritionConflictPolicyAction
                                .RETAIN_AND_REPORT,
                        automaticCorrectionAllowed =
                            false,
                        automaticEntryRejectionAllowed =
                            false,
                        requiresPersistedEvidence =
                            true,
                        rationale =
                            "A single divergent nutrient is insufficient " +
                                    "evidence for automatic correction or " +
                                    "entry rejection."
                    )
                )
                    .associateBy { rule ->
                        rule.classification
                    }
                    .toSortedMap(
                        compareBy { classification ->
                            classification.name
                        }
                    )

            return Collections.unmodifiableMap(
                policies
            )
        }
    }
}

data class ResultingNutritionConflictPolicyRationale(
    val analyzedRuntimeEntryCount: Long =
        512_102L,
    val matchedEntryCount: Long =
        508_619L,
    val conflictEntryCount: Long =
        408L,
    val nutrientComparisonCount: Long =
        3_722_600L,
    val nutrientConflictCount: Long =
        1_762L,
    val measuredEntryConflictRate: Double =
        0.0008021721563685195,
    val measuredNutrientConflictRate: Double =
        0.00047332509536345566,
    val measuredExtremeConflictCount: Long =
        49L,
    val decision: String =
        "Approve the resulting Nutrition dataset while retaining and " +
                "reporting every conflict. Do not automatically correct " +
                "or reject individual entries."
) {

    init {
        require(analyzedRuntimeEntryCount > 0L)
        require(matchedEntryCount > 0L)
        require(conflictEntryCount >= 0L)
        require(nutrientComparisonCount > 0L)
        require(nutrientConflictCount >= 0L)
        require(measuredEntryConflictRate in 0.0..1.0)
        require(measuredNutrientConflictRate in 0.0..1.0)
        require(measuredExtremeConflictCount >= 0L)
        require(decision.isNotBlank())

        require(
            conflictEntryCount <=
                    matchedEntryCount
        )

        require(
            nutrientConflictCount <=
                    nutrientComparisonCount
        )
    }
}