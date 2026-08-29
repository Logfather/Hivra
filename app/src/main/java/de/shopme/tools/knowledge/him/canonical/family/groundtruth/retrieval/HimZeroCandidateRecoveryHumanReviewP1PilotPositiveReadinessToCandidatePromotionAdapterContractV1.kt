package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1 as DecisionValidationContract
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1 as DecisionValidationRoute
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1 as ReadinessContract
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure, in-memory binding boundary from positive P1 readiness to the existing
 * candidate-promotion domain. It never authorizes, promotes, persists, or
 * publishes a candidate.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_READINESS_TO_CANDIDATE_PROMOTION_ADAPTER_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "POSITIVE_CANDIDATE_PROMOTION_BINDING_CONTEXT_ONLY"

    private const val DECISION_ID_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_READINESS_TO_CANDIDATE_PROMOTION_BINDING_DECISION_V1"
    private const val BATCH_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_READINESS_TO_CANDIDATE_PROMOTION_BINDING_BATCH_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun evaluate(request: Request): Result {
        validateReadiness(request.readinessBatch)?.let { return Result.Failed(it) }

        val duplicateBindings = request.candidateBindings
            .groupBy { it.readinessDecisionId }
            .values
            .any { it.size > 1 }
        if (duplicateBindings ||
            request.candidateBindings.map { it.candidateReference }.distinct().size !=
            request.candidateBindings.size
        ) return Result.Failed(FailureReason.DUPLICATE_CANDIDATE_BINDING)

        val readinessById = request.readinessBatch.decisions.associateBy { it.readinessDecisionId }
        request.candidateBindings.forEach { binding ->
            val readinessDecision = readinessById[binding.readinessDecisionId]
                ?: return Result.Failed(FailureReason.CANDIDATE_BINDING_MISMATCH)
            if (readinessDecision.state != ReadinessContract.ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE) {
                return Result.Failed(FailureReason.NON_POSITIVE_CANDIDATE_BINDING)
            }
            validateCandidateBinding(readinessDecision, binding)?.let { return Result.Failed(it) }
        }

        val decisions = request.readinessBatch.decisions.map { readinessDecision ->
            val candidateBinding = request.candidateBindings.singleOrNull {
                it.readinessDecisionId == readinessDecision.readinessDecisionId
            }
            val bindingState = when (readinessDecision.state) {
                ReadinessContract.ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE ->
                    if (candidateBinding == null) {
                        BindingState.BLOCKED_MISSING_CANDIDATE_BINDING
                    } else {
                        BindingState.READY_FOR_LATER_PROMOTION_DOMAIN_STEP
                    }

                ReadinessContract.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
                ReadinessContract.ReadinessState.REQUIRES_ADJUDICATION,
                ReadinessContract.ReadinessState.NOT_PUBLICATION_READY,
                    -> BindingState.NOT_APPLICABLE_NON_POSITIVE
            }
            BindingDecision(
                bindingDecisionId = bindingDecisionId(
                    request.readinessBatch,
                    readinessDecision,
                    bindingState,
                    candidateBinding,
                ),
                readinessDecision = readinessDecision,
                state = bindingState,
                candidatePromotionBinding = candidateBinding,
            )
        }
        val output = Batch(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            readinessBatchId = request.readinessBatch.eligibilityBatchId,
            readinessInputBindingDigest = request.readinessBatch.eligibilityInputBindingDigest,
            readinessBatchLogicalDigest = request.readinessBatch.logicalDigest,
            decisions = decisions,
            counters = Counters.from(decisions),
            logicalDigest = "",
        )
        return Result.Completed(output.copy(logicalDigest = batchLogicalDigest(output)))
    }

    fun bindingDecisionId(
        readinessBatch: ReadinessContract.Batch,
        readinessDecision: ReadinessContract.Decision,
        state: BindingState,
        candidateBinding: CandidatePromotionBinding?,
    ): String = sha256(
        buildString {
            appendLine(DECISION_ID_DOMAIN)
            appendLine("contractId=$CONTRACT_ID")
            appendLine("version=$VERSION")
            appendLine("readinessBatchLogicalDigest=${readinessBatch.logicalDigest}")
            appendLine("readinessDecisionId=${readinessDecision.readinessDecisionId}")
            appendLine("readinessState=${readinessDecision.state.name}")
            appendLine("bindingState=${state.name}")
            appendLine("inputBindingDigest=${readinessBatch.eligibilityInputBindingDigest}")
            candidateBinding?.let { appendCandidateBinding(this, it) }
        },
    )

    fun batchLogicalDigest(batch: Batch): String = sha256(
        buildString {
            appendLine(BATCH_DIGEST_DOMAIN)
            appendLine("contractId=${batch.contractId}")
            appendLine("version=${batch.version}")
            appendLine("state=${batch.state}")
            appendLine("readinessBatchId=${batch.readinessBatchId}")
            appendLine("readinessInputBindingDigest=${batch.readinessInputBindingDigest}")
            appendLine("readinessBatchLogicalDigest=${batch.readinessBatchLogicalDigest}")
            batch.decisions.forEach { decision ->
                appendLine("bindingDecisionId=${decision.bindingDecisionId}")
                appendLine("readinessDecisionId=${decision.readinessDecision.readinessDecisionId}")
                appendLine("readinessState=${decision.readinessDecision.state.name}")
                appendLine("bindingState=${decision.state.name}")
                decision.candidatePromotionBinding?.let { appendCandidateBinding(this, it) }
            }
            appendLine("counters=${batch.counters}")
        },
    )

    private fun validateReadiness(batch: ReadinessContract.Batch): FailureReason? {
        if (batch.contractId != ReadinessContract.CONTRACT_ID ||
            batch.version != ReadinessContract.VERSION ||
            batch.state != ReadinessContract.STATE
        ) return FailureReason.INVALID_READINESS_CONTRACT
        if (!SHA256.matches(batch.eligibilityInputBindingDigest) ||
            !SHA256.matches(batch.eligibilityBatchLogicalDigest) ||
            !SHA256.matches(batch.logicalDigest)
        ) return FailureReason.INVALID_READINESS_DIGEST
        if (batch.logicalDigest != ReadinessContract.batchLogicalDigest(batch.copy(logicalDigest = ""))) {
            return FailureReason.INVALID_READINESS_DIGEST
        }
        if (batch.decisions.size != batch.counters.totalDecisions ||
            batch.counters != ReadinessContract.Counters.from(batch.decisions)
        ) return FailureReason.INVALID_READINESS_COUNTERS
        if (batch.decisions.map { it.readinessDecisionId }.distinct().size != batch.decisions.size) {
            return FailureReason.DUPLICATE_READINESS_DECISION
        }
        if (batch.decisions.map { it.reviewUnitId } !=
            batch.decisions.map { it.reviewUnitId }.distinct()
        ) return FailureReason.DUPLICATE_REVIEW_UNIT
        if (batch.decisions.any { !SHA256.matches(it.readinessDecisionId) }) {
            return FailureReason.INVALID_READINESS_DECISION_ID
        }
        val expectedOrder = DecisionValidationContract.FROZEN_INPUT_BINDING.originalSelections.map { it.reviewUnitId }
        if (batch.decisions.map { it.reviewUnitId } != expectedOrder) {
            return FailureReason.INVALID_READINESS_ORDER
        }
        batch.decisions.forEach { decision ->
            if (decision.eligibilityBatchId != batch.eligibilityBatchId ||
                decision.eligibilityInputBindingDigest != batch.eligibilityInputBindingDigest
            ) return FailureReason.BROKEN_READINESS_BINDING
            if (decision.reviewUnitId.isBlank() || decision.stableEntryId.isBlank() ||
                decision.canonicalEntityId.isBlank() || decision.originalReviewerRef.isBlank() ||
                decision.validatorReviewerRef.isBlank() || decision.validationRound < 1 ||
                decision.validationRevision < 1 || decision.validationBatchId.isBlank() ||
                !SHA256.matches(decision.validationRecordId) ||
                !SHA256.matches(decision.validationBatchBindingDigest) ||
                !SHA256.matches(decision.validationBatchLogicalDigest)
            ) return FailureReason.BROKEN_READINESS_PROVENANCE
            if (decision.eligibilityState != expectedEligibilityState(decision.state)) {
                return FailureReason.BROKEN_READINESS_BINDING
            }
            val expectedRoute = when (decision.state) {
                ReadinessContract.ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE ->
                    DecisionValidationRoute.POTENTIAL_POSITIVE_GOLD_CANDIDATE
                ReadinessContract.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE ->
                    DecisionValidationRoute.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE
                ReadinessContract.ReadinessState.REQUIRES_ADJUDICATION ->
                    DecisionValidationRoute.REQUIRES_ADJUDICATION
                ReadinessContract.ReadinessState.NOT_PUBLICATION_READY ->
                    DecisionValidationRoute.NOT_ELIGIBLE
            }
            if (decision.downstreamRoute != expectedRoute) return FailureReason.READINESS_ROUTE_MISMATCH
        }
        return null
    }

    private fun expectedEligibilityState(
        state: ReadinessContract.ReadinessState,
    ) = when (state) {
        ReadinessContract.ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE ->
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_POSITIVE_GOLD_CANDIDATE
        ReadinessContract.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE ->
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE
        ReadinessContract.ReadinessState.REQUIRES_ADJUDICATION ->
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.REQUIRES_ADJUDICATION
        ReadinessContract.ReadinessState.NOT_PUBLICATION_READY ->
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.NOT_ELIGIBLE
    }

    private fun validateCandidateBinding(
        readinessDecision: ReadinessContract.Decision,
        candidateBinding: CandidatePromotionBinding,
    ): FailureReason? {
        if (candidateBinding.readinessDecisionId != readinessDecision.readinessDecisionId) {
            return FailureReason.CANDIDATE_BINDING_MISMATCH
        }
        if (candidateBinding.candidateTerm.isBlank()) return FailureReason.MISSING_CANDIDATE_BINDING
        if (candidateBinding.candidateRelation == HimCandidateRelation.CreateNewCanonical) {
            return FailureReason.UNSUPPORTED_CANDIDATE_RELATION
        }
        if (candidateCanonicalId(candidateBinding.candidateRelation) != readinessDecision.canonicalEntityId) {
            return FailureReason.CANDIDATE_CANONICAL_MISMATCH
        }
        return null
    }

    private fun candidateCanonicalId(relation: HimCandidateRelation): String = when (relation) {
        is HimCandidateRelation.Identity -> relation.parentCanonicalId.value
        is HimCandidateRelation.Variant -> relation.scope.canonicalId.value
        is HimCandidateRelation.Alias -> relation.equivalentEntity.canonicalId.value
        HimCandidateRelation.CreateNewCanonical -> ""
    }

    private fun appendCandidateBinding(builder: StringBuilder, binding: CandidatePromotionBinding) {
        builder.appendLine("candidateReference=${binding.candidateReference.value}")
        builder.appendLine("candidateTerm=${binding.candidateTerm}")
        builder.appendLine("candidateRelation=${relationKey(binding.candidateRelation)}")
        builder.appendLine("candidateDatasetDigest=${binding.candidateDatasetDigest.value}")
        builder.appendLine("validationReference=${binding.validationReference.value}")
        builder.appendLine("candidateBindingReadinessDecisionId=${binding.readinessDecisionId}")
    }

    private fun relationKey(relation: HimCandidateRelation): String = when (relation) {
        is HimCandidateRelation.Identity -> "IDENTITY|parent=${relation.parentCanonicalId.value}"
        is HimCandidateRelation.Variant -> "VARIANT|canonical=${relation.scope.canonicalId.value}"
        is HimCandidateRelation.Alias -> "ALIAS|canonical=${relation.equivalentEntity.canonicalId.value}"
        HimCandidateRelation.CreateNewCanonical -> "CREATE_NEW_CANONICAL"
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    enum class BindingState {
        READY_FOR_LATER_PROMOTION_DOMAIN_STEP,
        BLOCKED_MISSING_CANDIDATE_BINDING,
        NOT_APPLICABLE_NON_POSITIVE,
    }

    enum class FailureReason {
        INVALID_READINESS_CONTRACT,
        INVALID_READINESS_DIGEST,
        INVALID_READINESS_COUNTERS,
        DUPLICATE_READINESS_DECISION,
        DUPLICATE_REVIEW_UNIT,
        INVALID_READINESS_DECISION_ID,
        INVALID_READINESS_ORDER,
        BROKEN_READINESS_BINDING,
        BROKEN_READINESS_PROVENANCE,
        READINESS_ROUTE_MISMATCH,
        DUPLICATE_CANDIDATE_BINDING,
        CANDIDATE_BINDING_MISMATCH,
        NON_POSITIVE_CANDIDATE_BINDING,
        MISSING_CANDIDATE_BINDING,
        CANDIDATE_CANONICAL_MISMATCH,
        UNSUPPORTED_CANDIDATE_RELATION,
    }

    data class CandidatePromotionBinding(
        val readinessDecisionId: String,
        val candidateReference: HimCandidateReference,
        val candidateTerm: String,
        val candidateRelation: HimCandidateRelation,
        val candidateDatasetDigest: HimSha256,
        val validationReference: HimCandidateValidationDecisionReference,
    ) {
        init {
            require(candidateTerm.isNotBlank())
        }
    }

    data class Request(
        val readinessBatch: ReadinessContract.Batch,
        val candidateBindings: List<CandidatePromotionBinding> = emptyList(),
    )

    data class BindingDecision(
        val bindingDecisionId: String,
        val readinessDecision: ReadinessContract.Decision,
        val state: BindingState,
        val candidatePromotionBinding: CandidatePromotionBinding?,
    )

    data class Counters(
        val totalDecisions: Int,
        val readyForLaterPromotionDomainStep: Int,
        val blockedMissingCandidateBinding: Int,
        val nonApplicableNonPositive: Int,
    ) {
        companion object {
            fun from(decisions: List<BindingDecision>) = Counters(
                totalDecisions = decisions.size,
                readyForLaterPromotionDomainStep = decisions.count {
                    it.state == BindingState.READY_FOR_LATER_PROMOTION_DOMAIN_STEP
                },
                blockedMissingCandidateBinding = decisions.count {
                    it.state == BindingState.BLOCKED_MISSING_CANDIDATE_BINDING
                },
                nonApplicableNonPositive = decisions.count {
                    it.state == BindingState.NOT_APPLICABLE_NON_POSITIVE
                },
            )
        }
    }

    data class Batch(
        val contractId: String,
        val version: String,
        val state: String,
        val readinessBatchId: String,
        val readinessInputBindingDigest: String,
        val readinessBatchLogicalDigest: String,
        val decisions: List<BindingDecision>,
        val counters: Counters,
        val logicalDigest: String,
    )

    sealed interface Result {
        data class Completed(val value: Batch) : Result

        data class Failed(val reason: FailureReason) : Result
    }
}
