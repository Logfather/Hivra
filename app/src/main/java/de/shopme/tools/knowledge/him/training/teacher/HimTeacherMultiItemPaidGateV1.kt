package de.shopme.tools.knowledge.him.training.teacher

/** Offline decision produced from one completed real Teacher pilot. */
object HimTeacherMultiItemPaidGateContractV1 {
    const val VERSION = "HIM_TEACHER_MULTI_ITEM_PAID_GATE_V1"
    const val MAX_RECOMMENDED_NEXT_PILOT_ITEMS = 5
    const val MIN_RECOMMENDED_NEXT_PILOT_ITEMS = 3
}

enum class HimTeacherPilotSemanticAuditResultV1 {
    PLAUSIBLE_CONSERVATIVE,
    QUESTIONABLE,
    LOCAL_DATA_GAP,
    CONTRACT_GAP,
}

enum class HimTeacherMultiItemPaidGateStateV1 {
    READY_FOR_SMALL_MULTI_ITEM_PILOT,
    NOT_READY,
    LOCAL_DATA_GAP,
    CONTRACT_GAP,
}

data class HimTeacherMultiItemPaidGateInputV1(
    val preflightCurrent: Boolean,
    val f3g2Complete: Boolean,
    val exactlyOneRealWorkItem: Boolean,
    val strictDecodePass: Boolean,
    val teacherValidationPass: Boolean,
    val persistencePass: Boolean,
    val reloadPass: Boolean,
    val partitionPreserved: Boolean,
    val providerAttemptsWithinContract: Boolean,
    val retriesWithinContract: Boolean,
    val groundTruthUnchanged: Boolean,
    val authorityUnchanged: Boolean,
    val entityIdsUnchanged: Boolean,
    val usageAvailable: Boolean,
    val semanticAudit: HimTeacherPilotSemanticAuditResultV1,
    val unexplainedProviderFailure: Boolean,
)

data class HimTeacherMultiItemPaidGateDecisionV1(
    val contractVersion: String,
    val state: HimTeacherMultiItemPaidGateStateV1,
    val reasons: List<String>,
    val maximumRecommendedNextPilotItems: Int,
    val automaticBatchAuthorization: Boolean,
) {
    init {
        require(contractVersion == HimTeacherMultiItemPaidGateContractV1.VERSION)
        require(reasons == reasons.distinct())
        require(maximumRecommendedNextPilotItems == HimTeacherMultiItemPaidGateContractV1.MAX_RECOMMENDED_NEXT_PILOT_ITEMS)
        require(!automaticBatchAuthorization)
    }
}

object HimTeacherMultiItemPaidGateV1 {
    fun evaluate(input: HimTeacherMultiItemPaidGateInputV1): HimTeacherMultiItemPaidGateDecisionV1 {
        val contractReasons = buildList {
            if (input.semanticAudit == HimTeacherPilotSemanticAuditResultV1.CONTRACT_GAP) add("semantic audit found a contract gap")
        }
        if (contractReasons.isNotEmpty()) return decision(HimTeacherMultiItemPaidGateStateV1.CONTRACT_GAP, contractReasons)

        val localDataReasons = buildList {
            if (input.semanticAudit == HimTeacherPilotSemanticAuditResultV1.LOCAL_DATA_GAP) add("semantic audit found a local data gap")
        }
        if (localDataReasons.isNotEmpty()) return decision(HimTeacherMultiItemPaidGateStateV1.LOCAL_DATA_GAP, localDataReasons)

        val failedTechnicalChecks = buildList {
            if (!input.preflightCurrent) add("F3.8g.1 preflight is not current")
            if (!input.f3g2Complete) add("F3.8g.2 is not complete")
            if (!input.exactlyOneRealWorkItem) add("pilot did not execute exactly one real work item")
            if (!input.strictDecodePass) add("strict decode did not pass")
            if (!input.teacherValidationPass) add("Teacher validation did not pass")
            if (!input.persistencePass) add("persistence did not pass")
            if (!input.reloadPass) add("reload did not pass")
            if (!input.partitionPreserved) add("partition was not preserved")
            if (!input.providerAttemptsWithinContract) add("provider attempts exceeded the frozen contract")
            if (!input.retriesWithinContract) add("technical retries exceeded the frozen contract")
            if (!input.groundTruthUnchanged) add("Ground Truth changed")
            if (!input.authorityUnchanged) add("Authority changed")
            if (!input.entityIdsUnchanged) add("Entity IDs changed")
            if (!input.usageAvailable) add("safe provider usage is unavailable")
            if (input.semanticAudit == HimTeacherPilotSemanticAuditResultV1.QUESTIONABLE) add("semantic plausibility is questionable")
            if (input.unexplainedProviderFailure) add("provider/runtime failure is unexplained")
        }
        return if (failedTechnicalChecks.isEmpty()) {
            decision(HimTeacherMultiItemPaidGateStateV1.READY_FOR_SMALL_MULTI_ITEM_PILOT, listOf("all V1 offline gate checks passed"))
        } else {
            decision(HimTeacherMultiItemPaidGateStateV1.NOT_READY, failedTechnicalChecks)
        }
    }

    private fun decision(state: HimTeacherMultiItemPaidGateStateV1, reasons: List<String>) =
        HimTeacherMultiItemPaidGateDecisionV1(
            HimTeacherMultiItemPaidGateContractV1.VERSION,
            state,
            reasons,
            HimTeacherMultiItemPaidGateContractV1.MAX_RECOMMENDED_NEXT_PILOT_ITEMS,
            automaticBatchAuthorization = false,
        )
}
