package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionDiagnosticsV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionLeakageValidatorV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionManifestIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionValidationResultV1

/** Validates P1 partition isolation without persistence, stores, or partition mutation. */
object HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_LEAKAGE_VALIDATION_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "P1_TRAINING_CORPUS_LEAKAGE_VALIDATED_IN_MEMORY"

    data class Request(
        val partitionResult:
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed,
        val sourceBinding:
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed,
    )

    sealed interface Result {
        data class Completed(
            val snapshotBinding:
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1,
            val partitionManifestLogicalDigest: HimSha256,
            val partitionCounters: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters,
            val validationResult: HimTrainingPartitionValidationResultV1,
        ) : Result

        data class Failed(
            val reason: FailureReasonV1,
            val validationResult: HimTrainingPartitionValidationResultV1? = null,
            val safeContext: String,
        ) : Result
    }

    enum class FailureReasonV1 {
        INVALID_PARTITION_RESULT,
        LEAKAGE_DETECTED,
    }

    fun execute(request: Request): Result {
        val partition = request.partitionResult
        try {
            partition.validateAgainst(request.sourceBinding)
            val manifest = partition.partitionManifest
            require(
                manifest.logicalDigest ==
                    HimTrainingPartitionManifestIdentityV1.digest(manifest.policyVersion, manifest.assignments),
            ) { "PARTITION_MANIFEST_DIGEST_MISMATCH" }
            require(
                partition.counters ==
                    HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters.from(
                        manifest.assignments,
                    ),
            ) { "PARTITION_COUNTER_MISMATCH" }
            require(
                manifest.diagnostics == HimTrainingPartitionDiagnosticsV1.from(manifest.assignments),
            ) { "PARTITION_DIAGNOSTICS_MISMATCH" }
        } catch (_: IllegalArgumentException) {
            return Result.Failed(FailureReasonV1.INVALID_PARTITION_RESULT, safeContext = "partition")
        }

        val validation = HimTrainingPartitionLeakageValidatorV1.validate(partition.partitionManifest)
        return if (validation.valid) {
            Result.Completed(
                snapshotBinding = partition.snapshotBinding,
                partitionManifestLogicalDigest = partition.partitionManifest.logicalDigest,
                partitionCounters = partition.counters,
                validationResult = validation,
            )
        } else {
            Result.Failed(
                reason = FailureReasonV1.LEAKAGE_DETECTED,
                validationResult = validation,
                safeContext = "partition-manifest",
            )
        }
    }
}
