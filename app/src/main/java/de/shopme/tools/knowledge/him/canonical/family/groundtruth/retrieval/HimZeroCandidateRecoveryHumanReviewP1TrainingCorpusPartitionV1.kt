package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolutionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolverV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionAssignmentV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionManifestV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionPolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1

/** Creates an in-memory P1 partition manifest from an authoritative source binding. */
object HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_PARTITION_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "P1_TRAINING_CORPUS_PARTITIONED_IN_MEMORY"

    data class Request(
        val sourceBinding:
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed,
    )

    data class Counters(
        val total: Int,
        val train: Int,
        val validation: Int,
        val holdout: Int,
    ) {
        init {
            require(listOf(total, train, validation, holdout).all { it >= 0 })
            require(total == train + validation + holdout)
        }

        companion object {
            fun from(assignments: List<HimTrainingPartitionAssignmentV1>) = Counters(
                total = assignments.size,
                train = assignments.count { it.partition == HimTrainingPartitionV1.TRAIN },
                validation = assignments.count { it.partition == HimTrainingPartitionV1.VALIDATION },
                holdout = assignments.count { it.partition == HimTrainingPartitionV1.HOLDOUT },
            )
        }
    }

    sealed interface Result {
        data class Completed(
            val snapshotBinding:
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1,
            val partitionManifest: HimTrainingPartitionManifestV1,
            val counters: Counters,
        ) : Result {
            fun validateAgainst(
                sourceBinding:
                    HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed,
            ) {
                val expected = sourceBinding.snapshotBinding
                require(snapshotBinding == expected) { "SNAPSHOT_BINDING_MISMATCH" }
                val expectedRecords = sourceBinding.sourceBindings.bindings.map { binding ->
                    when (binding) {
                        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Positive ->
                            binding.positiveExample.exampleReference.value
                        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Negative ->
                            binding.negativeExample.reference.value
                    }
                }
                val actualRecords = partitionManifest.assignments.map { it.record.recordReference }
                require(actualRecords.size == expectedRecords.size) { "PARTITION_COVERAGE_MISMATCH" }
                require(actualRecords.toSet() == expectedRecords.toSet()) { "PARTITION_COVERAGE_MISMATCH" }
            }
        }

        data class Failed(
            val reason: FailureReasonV1,
            val safeContext: String,
        ) : Result
    }

    enum class FailureReasonV1 {
        INVALID_SOURCE_BINDING,
        PARTITION_RECORD_MAPPING_FAILED,
        INCOMPLETE_PARTITION_COVERAGE,
    }

    fun execute(request: Request): Result {
        val sourceBinding = request.sourceBinding
        if (sourceBinding.snapshotBinding != sourceBinding.sourceBindings.snapshotBinding) {
            return Result.Failed(FailureReasonV1.INVALID_SOURCE_BINDING, "snapshot")
        }

        val records = sourceBinding.sourceBindings.bindings.map { it.toPartitionRecord() }
        if (records.map { it.recordReference }.distinct().size != records.size) {
            return Result.Failed(FailureReasonV1.INCOMPLETE_PARTITION_COVERAGE, "records")
        }

        val assignments = records.map { record ->
            val resolution = when (record) {
                is HimTrainingPartitionRecordV1.Positive -> HimTrainingFamilyGroupResolverV1.resolve(record.positiveExample)
                is HimTrainingPartitionRecordV1.Negative -> HimTrainingFamilyGroupResolverV1.resolve(record.negativeExample)
            }
            when (resolution) {
                is HimTrainingFamilyGroupResolutionV1.Resolved ->
                    HimTrainingPartitionAssignmentV1(
                        record = record,
                        groupReference = resolution.groupReference,
                        partition = HimTrainingPartitionPolicyV1.partitionForGroup(resolution.groupReference),
                    )
                is HimTrainingFamilyGroupResolutionV1.NotYetGroupable ->
                    return Result.Failed(FailureReasonV1.PARTITION_RECORD_MAPPING_FAILED, "record")
            }
        }
        val manifest = HimTrainingPartitionManifestV1.create(assignments)
        val result = Result.Completed(sourceBinding.snapshotBinding, manifest, Counters.from(assignments))
        result.validateAgainst(sourceBinding)
        return result
    }

    private fun HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.toPartitionRecord():
        HimTrainingPartitionRecordV1 = when (this) {
        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Positive ->
            HimTrainingPartitionRecordV1.Positive(positiveExample)
        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Negative ->
            HimTrainingPartitionRecordV1.Negative(negativeExample)
    }
}
