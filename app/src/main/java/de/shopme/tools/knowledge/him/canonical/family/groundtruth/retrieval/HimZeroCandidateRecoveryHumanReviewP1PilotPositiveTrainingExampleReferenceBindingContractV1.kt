package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure in-memory binding from one persisted P1 subject to one content-addressed
 * positive training-example reference. It does not read or write a store.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_REFERENCE_BINDING_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "POSITIVE_TRAINING_EXAMPLE_REFERENCE_BINDING_CONTEXT_ONLY"

    private const val BINDING_DECISION_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_REFERENCE_BINDING_DECISION_V1"
    private const val BATCH_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_REFERENCE_BINDING_BATCH_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun evaluate(request: Request): Result {
        if (request.bindings.isEmpty()) return Result.Failed(FailureReason.INVALID_BINDING_CONTEXT)
        if (request.bindings.any { !validNegativeSupervisionRecord(it.negativeSupervisionRecord) }) {
            return Result.Failed(FailureReason.INVALID_NEGATIVE_SUPERVISION_RECORD)
        }

        val grouped = request.bindings.groupBy { it.bindingKey }
        if (grouped.values.any { bindings -> bindings.size > 1 }) {
            val reason = if (grouped.values.any { bindings ->
                bindings.map { it.materializationDecision?.trainingExampleReference?.value.orEmpty() }
                    .distinct()
                    .size > 1
            }) {
                FailureReason.AMBIGUOUS_BINDING
            } else {
                FailureReason.DUPLICATE_BINDING
            }
            return Result.Failed(reason)
        }

        val decisions = request.bindings.map(::evaluateBinding)
        val unsigned = Batch(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            decisions = decisions,
            counters = Counters.from(decisions),
            logicalDigest = "",
        )
        val batch = unsigned.copy(logicalDigest = batchLogicalDigest(unsigned))
        validate(batch)
        return Result.Completed(batch)
    }

    fun validate(batch: Batch) {
        require(batch.contractId == CONTRACT_ID)
        require(batch.version == VERSION)
        require(batch.state == STATE)
        require(batch.decisions.map { it.bindingDecisionId }.distinct().size == batch.decisions.size)
        require(batch.decisions.map { it.bindingKey }.distinct().size == batch.decisions.size)
        require(batch.decisions.all { decision ->
            SHA256.matches(decision.bindingDecisionId) &&
                SHA256.matches(decision.bindingKey) &&
                decision.negativeSupervisionRecordId == decision.bindingKey &&
                decision.reasons.distinct().size == decision.reasons.size &&
                (decision.reasons.isEmpty() == (decision.state == BindingState.BOUND)) &&
                (decision.state == BindingState.BOUND) ==
                    (decision.trainingExampleReference != null && decision.materializationDecisionId != null)
        })
        require(batch.counters == Counters.from(batch.decisions))
        require(SHA256.matches(batch.logicalDigest))
        require(batch.logicalDigest == batchLogicalDigest(batch.copy(logicalDigest = "")))
    }

    fun batchLogicalDigest(batch: Batch): String = sha256(
        buildString {
            appendLine(BATCH_DIGEST_DOMAIN)
            appendLine("contractId=${batch.contractId}")
            appendLine("version=${batch.version}")
            appendLine("state=${batch.state}")
            batch.decisions.forEach { decision ->
                appendLine("bindingDecisionId=${decision.bindingDecisionId}")
                appendLine("bindingKey=${decision.bindingKey}")
                appendLine("negativeSupervisionRecordId=${decision.negativeSupervisionRecordId}")
                appendLine("materializationDecisionId=${decision.materializationDecisionId.orEmpty()}")
                appendLine("trainingExampleReference=${decision.trainingExampleReference?.value.orEmpty()}")
                appendLine("state=${decision.state.name}")
                decision.reasons.forEach { appendLine("reason=${it.name}") }
            }
            appendLine("totalBindings=${batch.counters.totalBindings}")
            appendLine("bound=${batch.counters.bound}")
            appendLine("notYetBindable=${batch.counters.notYetBindable}")
        },
    )

    private fun evaluateBinding(request: BindingRequest): BindingDecision {
        val reasons = mutableListOf<FailureReason>()
        val record = request.negativeSupervisionRecord
        val materialization = request.materializationDecision

        if (request.bindingKey != record.recordId) {
            reasons += FailureReason.P1_BINDING_IDENTITY_MISMATCH
        }
        if (!validNegativeSupervisionRecord(record)) {
            reasons += FailureReason.INVALID_NEGATIVE_SUPERVISION_RECORD
        }

        if (materialization == null || materialization.state !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationState.PROJECTED
        ) {
            reasons += FailureReason.MATERIALIZATION_NOT_PROJECTED
        }

        val projected = materialization?.projectedTrainingExample
        val reference = materialization?.trainingExampleReference
        if (materialization?.state ==
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationState.PROJECTED
        ) {
            if (projected == null || reference == null) {
                reasons += FailureReason.MATERIALIZATION_NOT_PROJECTED
            } else {
                if (materialization.preMaterializationIdentity != materialization.occurrenceReference) {
                    reasons += FailureReason.MATERIALIZATION_LINEAGE_MISMATCH
                }
                when (val canonicalId = canonicalId(projected.target)) {
                    null -> reasons += FailureReason.UNSUPPORTED_LINEAGE_RELATION
                    else -> if (canonicalId.value != record.canonicalEntityId) {
                        reasons += FailureReason.CANONICAL_LINEAGE_MISMATCH
                    }
                }
            }
        }

        val normalizedReasons = reasons.distinct()
        val state = if (normalizedReasons.isEmpty()) BindingState.BOUND else BindingState.NOT_YET_BINDABLE
        val boundReference = if (state == BindingState.BOUND) reference else null
        val decisionId = bindingDecisionId(
            bindingKey = record.recordId,
            materializationDecisionId = materialization?.decisionId,
            projectionInputBindingDigest = materialization?.projectionInputBindingDigest?.value,
            reference = boundReference,
            state = state,
            reasons = normalizedReasons,
        )
        return BindingDecision(
            bindingDecisionId = decisionId,
            bindingKey = record.recordId,
            negativeSupervisionRecordId = record.recordId,
            materializationDecisionId = materialization?.decisionId,
            trainingExampleReference = boundReference,
            state = state,
            reasons = normalizedReasons,
        )
    }

    private fun validNegativeSupervisionRecord(
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
    ): Boolean =
        record.recordId.isNotBlank() && SHA256.matches(record.recordId) &&
            record.negativeCandidateId.isNotBlank() && SHA256.matches(record.negativeCandidateId) &&
            record.readinessDecisionId.isNotBlank() && SHA256.matches(record.readinessDecisionId) &&
            record.validationRecordId.isNotBlank() && SHA256.matches(record.validationRecordId) &&
            record.reviewUnitId.isNotBlank() &&
            record.stableEntryId.isNotBlank() &&
            record.canonicalEntityId.isNotBlank() &&
            record.originalReviewerRef.isNotBlank() &&
            record.validatorReviewerRef.isNotBlank() &&
            record.validationRound >= 1 &&
            record.validationRevision >= 1 &&
            record.validationReasonCodes.isNotEmpty() &&
            record.candidateState == HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE &&
            record.originalDecision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION &&
            record.downstreamRoute == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE &&
            record.evidenceReferenceIds.isNotEmpty() &&
            record.evidenceReferenceIds.distinct().size == record.evidenceReferenceIds.size &&
            record.evidenceReferenceIds.all(String::isNotBlank)

    private fun canonicalId(target: HimTrainingTargetV1): HimEntityId? = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> target.canonicalId
        is HimTrainingTargetV1.Identity -> target.parentCanonicalId
        is HimTrainingTargetV1.Variant -> target.scope.canonicalId
        is HimTrainingTargetV1.Alias -> target.equivalentEntity.canonicalId
        is HimTrainingTargetV1.NewCanonical -> null
    }

    private fun bindingDecisionId(
        bindingKey: String,
        materializationDecisionId: String?,
        projectionInputBindingDigest: String?,
        reference: HimTrainingExampleReference?,
        state: BindingState,
        reasons: List<FailureReason>,
    ): String = sha256(
        buildString {
            appendLine(BINDING_DECISION_DOMAIN)
            appendLine("contractId=$CONTRACT_ID")
            appendLine("version=$VERSION")
            appendLine("bindingKey=$bindingKey")
            appendLine("materializationDecisionId=${materializationDecisionId.orEmpty()}")
            appendLine("projectionInputBindingDigest=${projectionInputBindingDigest.orEmpty()}")
            appendLine("trainingExampleReference=${reference?.value.orEmpty()}")
            appendLine("state=${state.name}")
            reasons.forEach { appendLine("reason=${it.name}") }
        },
    )

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    enum class BindingState {
        BOUND,
        NOT_YET_BINDABLE,
    }

    enum class FailureReason {
        INVALID_BINDING_CONTEXT,
        INVALID_NEGATIVE_SUPERVISION_RECORD,
        MATERIALIZATION_NOT_PROJECTED,
        P1_BINDING_IDENTITY_MISMATCH,
        MATERIALIZATION_LINEAGE_MISMATCH,
        CANONICAL_LINEAGE_MISMATCH,
        UNSUPPORTED_LINEAGE_RELATION,
        DUPLICATE_BINDING,
        AMBIGUOUS_BINDING,
    }

    data class Request(val bindings: List<BindingRequest>)

    data class BindingRequest(
        val negativeSupervisionRecord: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        val materializationDecision: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationDecision?,
        val bindingKey: String = negativeSupervisionRecord.recordId,
    )

    data class BindingDecision(
        val bindingDecisionId: String,
        val bindingKey: String,
        val negativeSupervisionRecordId: String,
        val materializationDecisionId: String?,
        val trainingExampleReference: HimTrainingExampleReference?,
        val state: BindingState,
        val reasons: List<FailureReason>,
    )

    data class Counters(
        val totalBindings: Int,
        val bound: Int,
        val notYetBindable: Int,
    ) {
        init {
            require(totalBindings >= 0)
            require(bound >= 0)
            require(notYetBindable >= 0)
            require(bound + notYetBindable == totalBindings)
        }

        companion object {
            fun from(decisions: List<BindingDecision>) = Counters(
                totalBindings = decisions.size,
                bound = decisions.count { it.state == BindingState.BOUND },
                notYetBindable = decisions.count { it.state == BindingState.NOT_YET_BINDABLE },
            )
        }
    }

    data class Batch(
        val contractId: String,
        val version: String,
        val state: String,
        val decisions: List<BindingDecision>,
        val counters: Counters,
        val logicalDigest: String,
    )

    sealed interface Result {
        data class Completed(val batch: Batch) : Result

        data class Failed(val reason: FailureReason) : Result
    }
}
