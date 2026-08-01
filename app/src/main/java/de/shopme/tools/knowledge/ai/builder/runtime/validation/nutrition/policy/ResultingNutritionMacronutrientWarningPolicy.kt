package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.policy

data class ResultingNutritionMacronutrientWarningPolicy(
    val version: Int =
        CURRENT_VERSION,
    val status:
    ResultingNutritionMacronutrientWarningPolicyStatus =
        ResultingNutritionMacronutrientWarningPolicyStatus.APPROVED,
    val maximumCoreMacronutrientSumGrams: Double =
        DEFAULT_MAXIMUM_CORE_MACRONUTRIENT_SUM_GRAMS,
    val minimumPresentCoreMacronutrients: Int =
        DEFAULT_MINIMUM_PRESENT_CORE_MACRONUTRIENTS,
    val includedNutrients:
    Set<String> =
        DEFAULT_INCLUDED_NUTRIENTS,
    val excludedNutrients:
    Set<String> =
        DEFAULT_EXCLUDED_NUTRIENTS,
    val reportCoreMacronutrientExcessAsWarning: Boolean =
        false,
    val rationale:
    ResultingNutritionMacronutrientWarningPolicyRationale =
        ResultingNutritionMacronutrientWarningPolicyRationale()
) {

    init {
        require(version > 0) {
            "Macronutrient warning policy version must be positive."
        }

        require(maximumCoreMacronutrientSumGrams > 0.0) {
            "Maximum core macronutrient sum must be positive."
        }

        require(minimumPresentCoreMacronutrients > 0) {
            "Minimum present core macronutrients must be positive."
        }

        require(includedNutrients.isNotEmpty()) {
            "Included macronutrients must not be empty."
        }

        require(
            includedNutrients ==
                    includedNutrients.toSortedSet()
        ) {
            "Included macronutrients must be sorted."
        }

        require(
            excludedNutrients ==
                    excludedNutrients.toSortedSet()
        ) {
            "Excluded nutrients must be sorted."
        }

        require(
            includedNutrients
                .intersect(
                    excludedNutrients
                )
                .isEmpty()
        ) {
            "Included and excluded nutrients must be disjoint."
        }

        require(
            FIBER_KEY in
                    excludedNutrients
        ) {
            "Fiber must be explicitly excluded from the core " +
                    "macronutrient sum."
        }
    }

    fun evaluate(
        fat: Double?,
        carbohydrates: Double?,
        protein: Double?
    ): ResultingNutritionMacronutrientWarningPolicyDecision {

        val presentValues =
            listOfNotNull(
                fat,
                carbohydrates,
                protein
            )

        if (
            presentValues.size <
            minimumPresentCoreMacronutrients
        ) {
            return ResultingNutritionMacronutrientWarningPolicyDecision(
                evaluated =
                    false,
                coreMacronutrientSumGrams =
                    presentValues.sum(),
                exceedsMaximum =
                    false,
                reportWarning =
                    false
            )
        }

        val coreMacronutrientSum =
            presentValues.sum()

        val exceedsMaximum =
            coreMacronutrientSum >
                    maximumCoreMacronutrientSumGrams

        return ResultingNutritionMacronutrientWarningPolicyDecision(
            evaluated =
                true,
            coreMacronutrientSumGrams =
                coreMacronutrientSum,
            exceedsMaximum =
                exceedsMaximum,
            reportWarning =
                exceedsMaximum &&
                        reportCoreMacronutrientExcessAsWarning
        )
    }

    companion object {

        const val CURRENT_VERSION =
            1

        const val DEFAULT_MAXIMUM_CORE_MACRONUTRIENT_SUM_GRAMS =
            105.0

        const val DEFAULT_MINIMUM_PRESENT_CORE_MACRONUTRIENTS =
            3

        const val FAT_KEY =
            "fat"

        const val CARBOHYDRATES_KEY =
            "carbohydrates"

        const val PROTEIN_KEY =
            "protein"

        const val FIBER_KEY =
            "fiber"

        val DEFAULT_INCLUDED_NUTRIENTS:
                Set<String> =
            sortedSetOf(
                FAT_KEY,
                CARBOHYDRATES_KEY,
                PROTEIN_KEY
            )

        val DEFAULT_EXCLUDED_NUTRIENTS:
                Set<String> =
            sortedSetOf(
                FIBER_KEY
            )
    }
}

data class ResultingNutritionMacronutrientWarningPolicyDecision(
    val evaluated: Boolean,
    val coreMacronutrientSumGrams: Double,
    val exceedsMaximum: Boolean,
    val reportWarning: Boolean
) {

    init {
        require(coreMacronutrientSumGrams >= 0.0)

        require(
            !reportWarning ||
                    exceedsMaximum
        ) {
            "A warning may only be reported for an exceeded maximum."
        }

        require(
            !exceedsMaximum ||
                    evaluated
        ) {
            "An unevaluated entry must not exceed the maximum."
        }
    }
}

data class ResultingNutritionMacronutrientWarningPolicyRationale(
    val analyzedEntryCount: Long =
        512_102L,
    val previousWarningCount: Long =
        11_658L,
    val fiberDoubleCountingCandidateCount: Long =
        11_636L,
    val coreMacronutrientExcessCount: Long =
        22L,
    val decision:
    String =
        "Exclude fiber from the core macronutrient sum and do not " +
                "report the remaining core-sum excess cases as " +
                "runtime warnings."
) {

    init {
        require(analyzedEntryCount > 0L)
        require(previousWarningCount >= 0L)
        require(fiberDoubleCountingCandidateCount >= 0L)
        require(coreMacronutrientExcessCount >= 0L)

        require(
            previousWarningCount ==
                    fiberDoubleCountingCandidateCount +
                    coreMacronutrientExcessCount
        ) {
            "Analyzed warning counts are inconsistent."
        }

        require(decision.isNotBlank())
    }
}

enum class ResultingNutritionMacronutrientWarningPolicyStatus {
    DRAFT,
    APPROVED
}