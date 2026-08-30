package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure, in-memory binding boundary from persisted P1 negative supervision to
 * the existing training domain. It never creates a negative training example,
 * corpus entry, partition, or training effect.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_SUPERVISION_TO_TRAINING_BINDING_CONTEXT_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "NEGATIVE_SUPERVISION_TRAINING_BINDING_CONTEXT_ONLY"

    private const val BATCH_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_SUPERVISION_TRAINING_BINDING_CONTEXT_BATCH_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun evaluate(request: Request): Result {
        if (request.bindings.isEmpty()) return Result.Failed(FailureReason.INVALID_BINDING_CONTEXT)
        if (request.bindings.map { it.record.recordId }.distinct().size != request.bindings.size) {
            return Result.Failed(FailureReason.DUPLICATE_RECORD_BINDING)
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
        return Result.Completed(unsigned.copy(logicalDigest = batchLogicalDigest(unsigned)))
    }

    fun batchLogicalDigest(batch: Batch): String = sha256(
        buildString {
            appendLine(BATCH_DIGEST_DOMAIN)
            appendLine("contractId=${batch.contractId}")
            appendLine("version=${batch.version}")
            appendLine("state=${batch.state}")
            batch.decisions.forEach { decision ->
                appendLine("recordId=${decision.recordId}")
                appendLine("state=${decision.state.name}")
                decision.reasons.forEach { appendLine("reason=${it.name}") }
                appendLine("positiveExampleReference=${decision.positiveExampleReference?.value.orEmpty()}")
                appendLine("rejectedTarget=${decision.rejectedTarget?.let(::targetKey).orEmpty()}")
                appendLine("boundaryType=${decision.boundaryType?.name.orEmpty()}")
                decision.evidenceBindings
                    .sortedWith(compareBy({ it.persistedEvidenceReferenceId }, { evidenceKey(it.trainingEvidence.reference) }))
                    .forEach { binding ->
                        appendLine("persistedEvidenceReferenceId=${binding.persistedEvidenceReferenceId}")
                        appendLine("trainingEvidence=${evidenceKey(binding.trainingEvidence.reference)}")
                        appendLine("recordKind=${binding.trainingEvidence.recordKind}")
                        appendLine("retrievalRank=${binding.trainingEvidence.retrievalRank}")
                    }
                appendLine("trainingProvenance=${provenanceKey(decision.trainingProvenance)}")
            }
            appendLine("counters=${batch.counters}")
        },
    )

    private fun evaluateBinding(binding: RecordBinding): BindingDecision {
        val reasons = linkedSetOf<FailureReason>()
        val record = binding.record
        if (!validPersistedRecord(record)) reasons += FailureReason.INVALID_PERSISTED_RECORD

        val positive = binding.positiveTrainingExample
        if (positive == null) {
            reasons += FailureReason.MISSING_POSITIVE_TRAINING_EXAMPLE_BINDING
        } else {
            if (!validTrainingExample(positive)) reasons += FailureReason.POSITIVE_EXAMPLE_MISMATCH
            if (canonicalId(positive.target)?.value != record.canonicalEntityId ||
                positive.input.canonicalContext.none { it.canonicalId.value == record.canonicalEntityId }
            ) reasons += FailureReason.POSITIVE_EXAMPLE_MISMATCH
        }

        val rejectedTarget = binding.rejectedTarget
        if (rejectedTarget == null) {
            reasons += FailureReason.MISSING_REJECTED_TARGET_BINDING
        } else {
            val rejectedCanonicalId = canonicalId(rejectedTarget)?.value
            if (rejectedCanonicalId != null && rejectedCanonicalId != record.canonicalEntityId) {
                reasons += FailureReason.CANONICAL_TARGET_MISMATCH
            }
        }

        val boundaryType = binding.boundaryType
        if (boundaryType == null) reasons += FailureReason.MISSING_NEGATIVE_BOUNDARY_TYPE_BINDING
        val provenance = binding.trainingProvenance
        if (provenance == null) {
            reasons += FailureReason.MISSING_TRAINING_PROVENANCE_BINDING
        } else if (positive != null && provenance != positive.provenance) {
            reasons += FailureReason.CONTRADICTORY_TRAINING_PROVENANCE
        }

        if (positive != null) {
            validateEvidenceBinding(binding, positive, reasons)
            if (rejectedTarget != null && boundaryType != null && validTrainingExample(positive)) {
                val admissibility = HimNegativeTrainingExamplePolicyV1.evaluate(
                    positiveExample = positive,
                    rejectedTarget = rejectedTarget,
                    boundaryType = boundaryType,
                )
                if (!admissibility.admissible) reasons += FailureReason.UNSUPPORTED_BOUNDARY_SEMANTICS
            }
        }

        val state = if (reasons.isEmpty()) {
            BindingState.READY_FOR_NEGATIVE_TRAINING_EXAMPLE_ADAPTER
        } else {
            BindingState.NOT_YET_BINDABLE
        }
        return BindingDecision(
            recordId = record.recordId,
            state = state,
            reasons = reasons.toList(),
            positiveExampleReference = positive?.exampleReference,
            rejectedTarget = rejectedTarget,
            boundaryType = boundaryType,
            evidenceBindings = binding.evidenceBindings
                .sortedWith(compareBy({ it.persistedEvidenceReferenceId }, { evidenceKey(it.trainingEvidence.reference) })),
            trainingProvenance = provenance,
        )
    }

    private fun validateEvidenceBinding(
        binding: RecordBinding,
        positive: HimTrainingExampleV1,
        reasons: MutableSet<FailureReason>,
    ) {
        val persistedIds = binding.record.evidenceReferenceIds.toSet()
        val suppliedIds = binding.evidenceBindings.map { it.persistedEvidenceReferenceId }
        if (binding.evidenceBindings.isEmpty()) {
            reasons += FailureReason.MISSING_REQUIRED_EVIDENCE_BINDING
            return
        }
        if (suppliedIds.any(String::isBlank) || suppliedIds.distinct().size != suppliedIds.size ||
            suppliedIds.toSet() != persistedIds
        ) reasons += FailureReason.EVIDENCE_BINDING_MISMATCH

        val inputEvidence = positive.input.evidence.map { it.reference }.toSet()
        val provenanceEvidence = positive.provenance.sourceEvidenceReferences.toSet()
        if (binding.evidenceBindings.any {
                it.trainingEvidence.reference !in inputEvidence ||
                    it.trainingEvidence.reference !in provenanceEvidence
            }
        ) reasons += FailureReason.EVIDENCE_BINDING_MISMATCH
    }

    private fun validPersistedRecord(
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
    ): Boolean =
        record.recordId.isNotBlank() && SHA256.matches(record.recordId) &&
            record.negativeCandidateId.isNotBlank() && SHA256.matches(record.negativeCandidateId) &&
            record.readinessDecisionId.isNotBlank() && SHA256.matches(record.readinessDecisionId) &&
            record.validationRecordId.isNotBlank() && SHA256.matches(record.validationRecordId) &&
            record.canonicalEntityId.isNotBlank() &&
            record.candidateState == HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE &&
            record.originalDecision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION &&
            record.downstreamRoute == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE &&
            record.evidenceReferenceIds.isNotEmpty() &&
            record.evidenceReferenceIds.distinct().size == record.evidenceReferenceIds.size

    private fun validTrainingExample(example: HimTrainingExampleV1): Boolean = try {
        de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleValidatorV1.validate(example)
        true
    } catch (_: Throwable) {
        false
    }

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
        is HimFamilyEntityReference.Identity ->
            "IDENTITY|${reference.canonicalId.value}|${reference.identityId.value}"
    }

    private fun evidenceKey(reference: HimEvidenceReference): String =
        "${reference.source}|${reference.sourceArtifactSha256.value}|${reference.sourceRecordIdentity}"

    private fun provenanceKey(provenance: HimTrainingProvenanceV1?): String = provenance?.let {
        buildString {
            append("candidate=${it.candidateReference?.value.orEmpty()};")
            append("generation=${it.generationRunReference?.value.orEmpty()};")
            append("input=${it.inputRunReference?.value.orEmpty()};")
            append("validation=${it.validationReference?.value.orEmpty()};")
            append("promotion=${it.promotionReference?.value.orEmpty()};")
            append("mutation=${it.mutationReference?.value.orEmpty()};")
            append("release=${it.groundTruthReleaseReference?.value.orEmpty()};")
            append("promotedId=${it.promotedEntityId?.value.orEmpty()};")
            append("promotedType=${it.promotedEntityType?.name.orEmpty()};")
            it.sourceEvidenceReferences.sortedWith(compareBy({ it.source }, { it.sourceArtifactSha256.value }, { it.sourceRecordIdentity }))
                .forEach { reference -> append("evidence=${evidenceKey(reference)};") }
            it.sourceArtifactDigests.sortedBy { it.value }.forEach { digest -> append("artifact=${digest.value};") }
            append("foundationRelease=${it.retrievalFoundationRelease.orEmpty()};")
            append("foundationSha=${it.retrievalFoundationReleaseSha256?.value.orEmpty()};")
            append("foundationDigest=${it.retrievalFoundationDigest?.value.orEmpty()};")
            append("teacher=${it.teacher?.provider.orEmpty()}/${it.teacher?.model.orEmpty()};")
        }
    }.orEmpty()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    enum class BindingState {
        READY_FOR_NEGATIVE_TRAINING_EXAMPLE_ADAPTER,
        NOT_YET_BINDABLE,
    }

    enum class FailureReason {
        INVALID_BINDING_CONTEXT,
        INVALID_PERSISTED_RECORD,
        DUPLICATE_RECORD_BINDING,
        MISSING_POSITIVE_TRAINING_EXAMPLE_BINDING,
        MISSING_REJECTED_TARGET_BINDING,
        MISSING_NEGATIVE_BOUNDARY_TYPE_BINDING,
        MISSING_REQUIRED_EVIDENCE_BINDING,
        MISSING_TRAINING_PROVENANCE_BINDING,
        CANONICAL_TARGET_MISMATCH,
        EVIDENCE_BINDING_MISMATCH,
        POSITIVE_EXAMPLE_MISMATCH,
        CONTRADICTORY_TRAINING_PROVENANCE,
        UNSUPPORTED_BOUNDARY_SEMANTICS,
    }

    data class EvidenceBinding(
        val persistedEvidenceReferenceId: String,
        val trainingEvidence: HimTrainingEvidenceInputV1,
    )

    data class RecordBinding(
        val record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        val positiveTrainingExample: HimTrainingExampleV1?,
        val rejectedTarget: HimTrainingTargetV1?,
        val boundaryType: HimNegativeBoundaryTypeV1?,
        val evidenceBindings: List<EvidenceBinding>,
        val trainingProvenance: HimTrainingProvenanceV1?,
    )

    data class Request(val bindings: List<RecordBinding>)

    data class BindingDecision(
        val recordId: String,
        val state: BindingState,
        val reasons: List<FailureReason>,
        val positiveExampleReference: HimTrainingExampleReference?,
        val rejectedTarget: HimTrainingTargetV1?,
        val boundaryType: HimNegativeBoundaryTypeV1?,
        val evidenceBindings: List<EvidenceBinding>,
        val trainingProvenance: HimTrainingProvenanceV1?,
    )

    data class Counters(
        val totalRecords: Int,
        val readyForNegativeTrainingExampleAdapter: Int,
        val notYetBindable: Int,
    ) {
        companion object {
            fun from(decisions: List<BindingDecision>) = Counters(
                totalRecords = decisions.size,
                readyForNegativeTrainingExampleAdapter = decisions.count {
                    it.state == BindingState.READY_FOR_NEGATIVE_TRAINING_EXAMPLE_ADAPTER
                },
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
        data class Completed(val value: Batch) : Result

        data class Failed(val reason: FailureReason) : Result
    }
}
