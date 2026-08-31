package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionValidationResultV1

/** Admits one exact leakage-validated training partition to a later training run. */
object HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_READINESS_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "P1_TRAINING_READINESS_VALIDATED"

    data class Request(
        val leakageValidation:
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Result,
        val partitionResult:
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed,
    )

    sealed interface Result {
        data class Ready(
            val snapshotBinding:
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1,
            val partitionManifestLogicalDigest: HimSha256,
            val partitionCounters: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters,
            val partitionPolicyVersion: String,
            val leakageValidationResult: HimTrainingPartitionValidationResultV1,
        ) : Result

        data class NotReady(
            val reason: Reason,
            val safeContext: String,
        ) : Result
    }

    enum class Reason {
        INVALID_LEAKAGE_VALIDATION_RESULT,
        TRAINING_BINDING_MISMATCH,
        TRAINING_CORPUS_EMPTY,
        TRAIN_PARTITION_EMPTY,
    }

    fun execute(request: Request): Result {
        val partition = request.partitionResult
        val leakage = request.leakageValidation as?
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Result.Completed
            ?: return Result.NotReady(Reason.INVALID_LEAKAGE_VALIDATION_RESULT, "leakage")
        val validation = leakage.validationResult
        if (!validation.valid || validation.diagnostics.any { it.fatal }) {
            return Result.NotReady(Reason.INVALID_LEAKAGE_VALIDATION_RESULT, "leakage")
        }
        if (
            leakage.snapshotBinding != partition.snapshotBinding ||
            leakage.partitionManifestLogicalDigest != partition.partitionManifest.logicalDigest ||
            leakage.partitionCounters != partition.counters ||
            partition.partitionManifest.policyVersion != HimTrainingPartitionContractV1.POLICY_VERSION ||
            partition.counters !=
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters.from(
                    partition.partitionManifest.assignments,
                )
        ) {
            return Result.NotReady(Reason.TRAINING_BINDING_MISMATCH, "partition")
        }
        if (partition.counters.total == 0) {
            return Result.NotReady(Reason.TRAINING_CORPUS_EMPTY, "counters")
        }
        if (partition.counters.train == 0) {
            return Result.NotReady(Reason.TRAIN_PARTITION_EMPTY, "counters")
        }
        return Result.Ready(
            snapshotBinding = partition.snapshotBinding,
            partitionManifestLogicalDigest = partition.partitionManifest.logicalDigest,
            partitionCounters = partition.counters,
            partitionPolicyVersion = partition.partitionManifest.policyVersion,
            leakageValidationResult = validation,
        )
    }
}
