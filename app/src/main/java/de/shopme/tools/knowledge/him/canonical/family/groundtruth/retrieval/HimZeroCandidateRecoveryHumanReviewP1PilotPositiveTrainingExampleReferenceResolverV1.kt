package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.training.corpus.HimPositiveTrainingExamplePersistenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/** Exact, read-only resolution of one persisted P1 binding to one training example. */
object HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceResolverV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_REFERENCE_RESOLVER_V1"
    const val VERSION = "1"
    const val STATE = "POSITIVE_TRAINING_EXAMPLE_REFERENCE_RESOLVER_CONTEXT_ONLY"

    fun resolveByNegativeSupervisionRecordId(
        recordId: String,
        bindingRoot: File = File(
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1.DURABLE_ROOT,
        ),
        positiveExampleRoot: File = File(HimPositiveTrainingExamplePersistenceV1.DURABLE_ROOT),
    ): Result {
        val binding = try {
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1
                .readByNegativeSupervisionRecordId(recordId, bindingRoot)
        } catch (_: IllegalArgumentException) {
            return when (bindingArtifactExists(recordId, bindingRoot)) {
                true, null -> Result.Failed(FailureReason.INVALID_PERSISTED_BINDING)
                false -> Result.Failed(FailureReason.BINDING_NOT_FOUND)
            }
        }

        val trainingExample = try {
            HimPositiveTrainingExamplePersistenceV1.read(binding.trainingExampleReference, positiveExampleRoot)
        } catch (failure: HimPositiveTrainingExamplePersistenceV1.PersistenceFailure) {
            return when (failure.reason) {
                HimPositiveTrainingExamplePersistenceV1.FailureReason.ARTIFACT_MISSING ->
                    Result.Failed(FailureReason.MISSING_POSITIVE_TRAINING_EXAMPLE)
                HimPositiveTrainingExamplePersistenceV1.FailureReason.REFERENCE_CONTENT_MISMATCH ->
                    Result.Failed(FailureReason.REFERENCE_CONTENT_MISMATCH)
                else -> Result.Failed(FailureReason.INVALID_POSITIVE_TRAINING_EXAMPLE)
            }
        } catch (_: IllegalArgumentException) {
            return Result.Failed(FailureReason.INVALID_POSITIVE_TRAINING_EXAMPLE)
        }

        return Result.Completed(
            Resolved(
                bindingRecord = binding,
                trainingExample = trainingExample,
            ),
        )
    }

    private fun bindingArtifactExists(recordId: String, bindingRoot: File): Boolean? = try {
        val path = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1
            .pathFor(recordId, bindingRoot)
            .toPath()
        Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path)
    } catch (_: IllegalArgumentException) {
        null
    }

    data class Resolved(
        val bindingRecord: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1.BindingRecord,
        val trainingExample: HimTrainingExampleV1,
    )

    enum class FailureReason {
        BINDING_NOT_FOUND,
        INVALID_PERSISTED_BINDING,
        MISSING_POSITIVE_TRAINING_EXAMPLE,
        INVALID_POSITIVE_TRAINING_EXAMPLE,
        REFERENCE_CONTENT_MISMATCH,
    }

    sealed interface Result {
        data class Completed(val value: Resolved) : Result

        data class Failed(val reason: FailureReason) : Result
    }
}
