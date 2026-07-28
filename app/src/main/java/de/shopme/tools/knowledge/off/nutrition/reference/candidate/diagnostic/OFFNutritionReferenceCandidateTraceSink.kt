package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic

fun interface OFFNutritionReferenceCandidateTraceSink {

    fun record(
        trace: OFFNutritionReferenceCandidateTrace
    )

    companion object {

        val NONE =
            OFFNutritionReferenceCandidateTraceSink {
                // Intentionally empty in productive runs without diagnostics.
            }
    }
}