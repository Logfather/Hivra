package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleValidatorV1
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure in-memory resolution of one explicitly supplied positive training example.
 * It never discovers, creates, persists, partitions, or trains on examples.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleBindingResolutionContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_BINDING_RESOLUTION_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "POSITIVE_TRAINING_EXAMPLE_BINDING_RESOLUTION_CONTEXT_ONLY"

    private const val BATCH_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_BINDING_RESOLUTION_BATCH_V1"
    private const val DECISION_ID_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_BINDING_RESOLUTION_DECISION_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun evaluate(request: Request): Result {
        if (request.records.isEmpty()) return Result.Failed(FailureReason.INVALID_BINDING_CONTEXT)
        if (request.records.map { it.recordId }.distinct().size != request.records.size) {
            return Result.Failed(FailureReason.DUPLICATE_RECORD_BINDING)
        }
        if (request.bindings.map { it.recordId }.distinct().size != request.bindings.size) {
            return Result.Failed(FailureReason.DUPLICATE_RECORD_BINDING)
        }

        val recordIds = request.records.map { it.recordId }.toSet()
        if (request.bindings.any { it.recordId !in recordIds }) {
            return Result.Failed(FailureReason.EXTRA_BINDING)
        }
        val positiveReferences = request.bindings.mapNotNull { it.positiveTrainingExample?.exampleReference }
        if (positiveReferences.distinct().size != positiveReferences.size) {
            return Result.Failed(FailureReason.DUPLICATE_POSITIVE_EXAMPLE_BINDING)
        }
        if (request.records.any { !validPersistedRecord(it) }) {
            return Result.Failed(FailureReason.INVALID_PERSISTED_RECORD)
        }

        val bindingByRecordId = request.bindings.associateBy { it.recordId }
        val decisions = request.records.map { record ->
            evaluateRecord(record, bindingByRecordId[record.recordId]?.positiveTrainingExample)
        }
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
        require(batch.decisions.map { it.recordId }.distinct().size == batch.decisions.size)
        require(batch.decisions.map { it.decisionId }.distinct().size == batch.decisions.size)
        require(batch.decisions.all { decision ->
            decision.reasons.distinct().size == decision.reasons.size &&
                (decision.reasons.isEmpty() == (decision.state == ResolutionState.RESOLVED_POSITIVE_TRAINING_EXAMPLE_BINDING))
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
                appendLine("decisionId=${decision.decisionId}")
                appendLine("recordId=${decision.recordId}")
                appendLine("state=${decision.state.name}")
                appendLine("positiveExampleReference=${decision.positiveExampleReference?.value.orEmpty()}")
                appendLine("positiveTarget=${decision.positiveTarget?.let(::targetKey).orEmpty()}")
                appendLine("inputIdentity=${decision.inputIdentity.name}")
                appendLine("evidenceRelationship=${decision.evidenceRelationship.name}")
                decision.reasons.forEach { appendLine("reason=${it.name}") }
            }
            appendLine("totalRecords=${batch.counters.totalRecords}")
            appendLine("resolved=${batch.counters.resolvedPositiveTrainingExampleBindings}")
            appendLine("notYetResolved=${batch.counters.notYetResolved}")
        },
    )

    private fun evaluateRecord(
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        positive: HimTrainingExampleV1?,
    ): BindingDecision {
        val reasons = mutableListOf<FailureReason>()
        var positiveReference: HimTrainingExampleReference? = null
        var positiveTarget: HimTrainingTargetV1? = null
        var inputIdentity = InputIdentity.NOT_PROVABLE
        var evidenceRelationship = EvidenceRelationship.NOT_PROVABLE

        if (positive == null) {
            reasons += FailureReason.MISSING_POSITIVE_TRAINING_EXAMPLE
        } else {
            positiveReference = positive.exampleReference
            positiveTarget = positive.target
            val validationReason = validatePositive(positive)
            if (validationReason != null) {
                reasons += validationReason
            } else {
                when (val targetCanonicalId = canonicalId(positive.target)) {
                    null -> reasons += FailureReason.POSITIVE_TARGET_NOT_COMPARABLE
                    else -> if (targetCanonicalId.value != record.canonicalEntityId) {
                        reasons += FailureReason.POSITIVE_EXAMPLE_RECORD_MISMATCH
                    }
                }
                if (reasons.isEmpty()) {
                    reasons += FailureReason.INPUT_IDENTITY_NOT_PROVABLE
                    reasons += FailureReason.EVIDENCE_RELATION_NOT_PROVABLE
                }
            }
        }

        val state = if (reasons.isEmpty()) {
            ResolutionState.RESOLVED_POSITIVE_TRAINING_EXAMPLE_BINDING
        } else {
            ResolutionState.NOT_YET_RESOLVED
        }
        val decisionId = decisionId(
            record = record,
            positiveReference = positiveReference,
            state = state,
            reasons = reasons,
        )
        return BindingDecision(
            decisionId = decisionId,
            recordId = record.recordId,
            state = state,
            reasons = reasons,
            positiveExampleReference = positiveReference,
            positiveTarget = positiveTarget,
            inputIdentity = inputIdentity,
            evidenceRelationship = evidenceRelationship,
        )
    }

    private fun validatePositive(example: HimTrainingExampleV1): FailureReason? = try {
        val expectedReference = HimTrainingExampleIdentityV1.example(
            taskType = example.taskType,
            input = example.input,
            target = example.target,
            provenance = example.provenance,
        )
        if (example.exampleReference != expectedReference) {
            FailureReason.INVALID_TRAINING_EXAMPLE_REFERENCE
        } else {
            HimTrainingExampleValidatorV1.validate(example)
            null
        }
    } catch (_: IllegalArgumentException) {
        FailureReason.INVALID_POSITIVE_TRAINING_EXAMPLE
    }

    private fun validPersistedRecord(
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

    private fun decisionId(
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        positiveReference: HimTrainingExampleReference?,
        state: ResolutionState,
        reasons: List<FailureReason>,
    ): String = sha256(
        buildString {
            appendLine(DECISION_ID_DOMAIN)
            appendLine("recordId=${record.recordId}")
            appendLine("positiveExampleReference=${positiveReference?.value.orEmpty()}")
            appendLine("state=${state.name}")
            reasons.forEach { appendLine("reason=${it.name}") }
        },
    )

    private fun canonicalId(target: HimTrainingTargetV1): HimEntityId? = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> target.canonicalId
        is HimTrainingTargetV1.Identity -> target.parentCanonicalId
        is HimTrainingTargetV1.Variant -> target.scope.canonicalId
        is HimTrainingTargetV1.Alias -> target.equivalentEntity.canonicalId
        is HimTrainingTargetV1.NewCanonical -> null
    }

    private fun targetKey(target: HimTrainingTargetV1): String = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> "EXISTING_CANONICAL|${target.canonicalId.value}"
        is HimTrainingTargetV1.Identity -> "IDENTITY|${target.parentCanonicalId.value}"
        is HimTrainingTargetV1.Variant -> "VARIANT|${familyKey(target.scope)}"
        is HimTrainingTargetV1.Alias -> "ALIAS|${familyKey(target.equivalentEntity)}"
        is HimTrainingTargetV1.NewCanonical -> "NEW_CANONICAL|${target.proposedCanonicalName.orEmpty()}"
    }

    private fun familyKey(reference: HimFamilyEntityReference): String = when (reference) {
        is HimFamilyEntityReference.Canonical -> "CANONICAL|${reference.canonicalId.value}"
        is HimFamilyEntityReference.Identity -> "IDENTITY|${reference.canonicalId.value}|${reference.identityId.value}"
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    enum class ResolutionState {
        RESOLVED_POSITIVE_TRAINING_EXAMPLE_BINDING,
        NOT_YET_RESOLVED,
    }

    enum class InputIdentity {
        PROVABLE,
        NOT_PROVABLE,
    }

    enum class EvidenceRelationship {
        COMPATIBLE,
        NOT_PROVABLE,
    }

    enum class FailureReason {
        INVALID_BINDING_CONTEXT,
        INVALID_PERSISTED_RECORD,
        DUPLICATE_RECORD_BINDING,
        DUPLICATE_POSITIVE_EXAMPLE_BINDING,
        EXTRA_BINDING,
        MISSING_POSITIVE_TRAINING_EXAMPLE,
        INVALID_POSITIVE_TRAINING_EXAMPLE,
        INVALID_TRAINING_EXAMPLE_REFERENCE,
        POSITIVE_EXAMPLE_RECORD_MISMATCH,
        POSITIVE_TARGET_NOT_COMPARABLE,
        INPUT_IDENTITY_NOT_PROVABLE,
        EVIDENCE_RELATION_NOT_PROVABLE,
    }

    data class Binding(
        val recordId: String,
        val positiveTrainingExample: HimTrainingExampleV1?,
    )

    data class Request(
        val records: List<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record>,
        val bindings: List<Binding>,
    )

    data class BindingDecision(
        val decisionId: String,
        val recordId: String,
        val state: ResolutionState,
        val reasons: List<FailureReason>,
        val positiveExampleReference: HimTrainingExampleReference?,
        val positiveTarget: HimTrainingTargetV1?,
        val inputIdentity: InputIdentity,
        val evidenceRelationship: EvidenceRelationship,
    )

    data class Counters(
        val totalRecords: Int,
        val resolvedPositiveTrainingExampleBindings: Int,
        val notYetResolved: Int,
    ) {
        companion object {
            fun from(decisions: List<BindingDecision>) = Counters(
                totalRecords = decisions.size,
                resolvedPositiveTrainingExampleBindings = decisions.count {
                    it.state == ResolutionState.RESOLVED_POSITIVE_TRAINING_EXAMPLE_BINDING
                },
                notYetResolved = decisions.count { it.state == ResolutionState.NOT_YET_RESOLVED },
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
        data class Completed(val value: Batch) : Result

        data class Failed(val reason: FailureReason) : Result
    }
}
