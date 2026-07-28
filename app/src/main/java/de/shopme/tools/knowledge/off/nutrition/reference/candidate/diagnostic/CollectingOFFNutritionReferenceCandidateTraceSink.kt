package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic

class CollectingOFFNutritionReferenceCandidateTraceSink :
    OFFNutritionReferenceCandidateTraceSink {

    private val mutableTraces =
        mutableListOf<OFFNutritionReferenceCandidateTrace>()

    override fun record(
        trace: OFFNutritionReferenceCandidateTrace
    ) {
        mutableTraces += trace
    }

    fun traces(): List<OFFNutritionReferenceCandidateTrace> {

        return mutableTraces
            .sortedWith(
                compareBy<OFFNutritionReferenceCandidateTrace>(
                    { it.sourceProductId },
                    { it.productName },
                    {
                        it.normalizedProductIdentities
                            .joinToString("\u0000")
                    }
                )
            )
    }
}