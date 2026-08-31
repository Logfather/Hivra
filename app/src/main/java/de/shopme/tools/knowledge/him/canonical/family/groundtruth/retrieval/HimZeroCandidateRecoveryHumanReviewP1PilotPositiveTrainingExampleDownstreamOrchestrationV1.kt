package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1

/**
 * Pure singleton orchestration from one proven P1 binding to the existing
 * downstream binding-context contract. No records are discovered, persisted,
 * deduplicated, or converted into a negative training example here.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_DOWNSTREAM_ORCHESTRATION_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "POSITIVE_TRAINING_EXAMPLE_DOWNSTREAM_ORCHESTRATION_CONTEXT_ONLY"

    fun evaluate(request: Request): Result {
        val proofAwareResult =
            HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.evaluate(
                HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.Request(
                    negativeSupervisionRecord = request.negativeSupervisionRecord,
                    positiveTrainingExample = request.positiveTrainingExample,
                    contextProof = request.contextProof,
                ),
            )

        return when (proofAwareResult) {
            is HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.Result.Failed ->
                Result.Failed(
                    reason = FailureReason.PROOF_AWARE_RESOLUTION_FAILED,
                    cause = FailureCause.ProofAware(proofAwareResult.reason),
                )

            is HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.Result.Resolved -> {
                val resolved = proofAwareResult.value
                val contextResult =
                    HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.evaluate(
                        HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.Request(
                            bindings = listOf(
                                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.RecordBinding(
                                    record = resolved.negativeSupervisionRecord,
                                    positiveTrainingExample = resolved.positiveTrainingExample,
                                    rejectedTarget = resolved.bindingDecision.rejectedTarget,
                                    boundaryType = resolved.bindingDecision.boundaryType,
                                    evidenceBindings = resolved.bindingDecision.evidenceBindings,
                                    trainingProvenance = resolved.bindingDecision.trainingProvenance,
                                ),
                            ),
                        ),
                    )

                val downstreamContext = when (contextResult) {
                    is HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.Result.Failed ->
                        error("DOWNSTREAM_CONTEXT_BINDING_INVARIANT_FAILED")
                    is HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.Result.Completed ->
                        contextResult.value
                }
                val downstreamDecision = downstreamContext.decisions.singleOrNull()
                    ?: error("DOWNSTREAM_CONTEXT_BINDING_INVARIANT_FAILED")
                check(
                    downstreamDecision.state ==
                        HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.BindingState.READY_FOR_NEGATIVE_TRAINING_EXAMPLE_ADAPTER,
                ) { "DOWNSTREAM_CONTEXT_BINDING_INVARIANT_FAILED" }
                Result.Completed(
                    Completed(
                        proofAwareResolution = resolved,
                        downstreamBindingContext = downstreamContext,
                        downstreamBindingDecision = downstreamDecision,
                    ),
                )
            }
        }
    }

    enum class FailureReason {
        PROOF_AWARE_RESOLUTION_FAILED,
    }

    sealed interface FailureCause {
        data class ProofAware(
            val reason: HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.FailureReason,
        ) : FailureCause
    }

    data class Request(
        val negativeSupervisionRecord: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        val positiveTrainingExample: HimTrainingExampleV1,
        val contextProof: HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1.ProofRecord,
    )

    data class Completed(
        val proofAwareResolution: HimZeroCandidateRecoveryHumanReviewP1PilotProofAwarePositiveTrainingExampleBindingResolutionContractV1.Resolved,
        val downstreamBindingContext: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.Batch,
        val downstreamBindingDecision: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.BindingDecision,
    )

    sealed interface Result {
        data class Completed(val value: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleDownstreamOrchestrationV1.Completed) : Result

        data class Failed(val reason: FailureReason, val cause: FailureCause) : Result
    }
}
