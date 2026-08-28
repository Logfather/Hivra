package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.io.File

/**
 * Opt-in orchestration boundary for the independent P1 decision-validation flow.
 * Human input remains explicit; this layer only gates and delegates to the
 * committed bridge and validation runtime.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_REAL_BOUND_ENTRYPOINT_V1"
    const val VERSION = "1"
    const val STATE = "INDEPENDENT_VALIDATION_CONTEXT_ONLY"

    const val ENABLED_PROPERTY =
        "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionValidation.enabled"
    const val CONFIRMATION_PROPERTY =
        "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionValidation.confirmation"
    const val AUTHORIZED_EXECUTION_HEAD_PROPERTY =
        "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionValidation.authorizedExecutionHead"
    const val CONFIRMATION =
        "AUTHORIZED_BOUNDED_P1_PILOT_DECISION_VALIDATION_REAL_BOUND_OFFLINE"
    const val CORE_BASELINE_HEAD = "ae12a546bcc87b5c28bf2ec45b72b1d705ab6360"

    private val HEAD = Regex("[0-9a-f]{40}")

    enum class FailureReasonV1 {
        CONFIRMATION_REQUIRED,
        EXECUTION_HEAD_REQUIRED,
        EXECUTION_HEAD_MISMATCH,
        CORE_BASELINE_NOT_ANCESTOR,
        REPOSITORY_ROOT_INVALID,
        PREPARED_RUNTIME_REQUEST_INVALID,
        BRIDGE_FAILED,
        RUNTIME_FAILED,
        RUNTIME_DISABLED,
    }

    data class GateV1(
        val enabled: Boolean,
        val confirmation: String?,
        val authorizedExecutionHead: String?,
    ) {
        fun isComplete(): Boolean =
            enabled &&
                confirmation == CONFIRMATION &&
                authorizedExecutionHead?.matches(HEAD) == true

        companion object {
            fun fromSystemProperties(): GateV1 = GateV1(
                enabled = System.getProperty(ENABLED_PROPERTY) == "true",
                confirmation = System.getProperty(CONFIRMATION_PROPERTY),
                authorizedExecutionHead = System.getProperty(AUTHORIZED_EXECUTION_HEAD_PROPERTY),
            )
        }
    }

    interface RepositoryPortV1 {
        fun currentHead(root: File): String

        fun isAncestor(root: File, ancestor: String, descendant: String): Boolean
    }

    fun interface BridgeInvokerV1 {
        fun prepare(
            request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeRequestV1,
        ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1
    }

    fun interface RuntimeInvokerV1 {
        fun execute(
            request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1,
        ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1
    }

    data class RequestV1(
        val gate: GateV1,
        val repositoryRoot: File,
        val repository: RepositoryPortV1,
        val bridgeRequest: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeRequestV1,
        val bridgeInvoker: BridgeInvokerV1 = BridgeInvokerV1 { request ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeV1.prepare(request)
        },
        val runtimeInvoker: RuntimeInvokerV1 = RuntimeInvokerV1 { request ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1.execute(request)
        },
    )

    sealed interface ResultV1 {
        data object Disabled : ResultV1

        data class Completed(
            val runtime: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Completed,
            val executionHead: String,
        ) : ResultV1

        data class Failed(
            val reason: FailureReasonV1,
            val safeContext: String,
            val bridgeReason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeFailureReasonV1? = null,
            val runtimeReason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1? = null,
        ) : ResultV1
    }

    fun executeFromSystemProperties(
        repositoryRoot: File,
        bridgeRequest: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeRequestV1,
        repository: RepositoryPortV1 = GitRepositoryPortV1,
        bridgeInvoker: BridgeInvokerV1 = BridgeInvokerV1 { request ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeV1.prepare(request)
        },
        runtimeInvoker: RuntimeInvokerV1 = RuntimeInvokerV1 { request ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1.execute(request)
        },
    ): ResultV1 = execute(
        RequestV1(
            gate = GateV1.fromSystemProperties(),
            repositoryRoot = repositoryRoot,
            repository = repository,
            bridgeRequest = bridgeRequest,
            bridgeInvoker = bridgeInvoker,
            runtimeInvoker = runtimeInvoker,
        ),
    )

    fun execute(request: RequestV1): ResultV1 {
        if (!request.gate.enabled) return ResultV1.Disabled
        if (request.gate.confirmation != CONFIRMATION) {
            return ResultV1.Failed(FailureReasonV1.CONFIRMATION_REQUIRED, "confirmation")
        }
        val executionHead = request.gate.authorizedExecutionHead
            ?.takeIf { it.matches(HEAD) }
            ?: return ResultV1.Failed(FailureReasonV1.EXECUTION_HEAD_REQUIRED, "execution-head")
        if (!request.repositoryRoot.isDirectory) {
            return ResultV1.Failed(FailureReasonV1.REPOSITORY_ROOT_INVALID, "repository-root")
        }

        val currentHead = request.repository.currentHead(request.repositoryRoot)
        if (currentHead != executionHead) {
            return ResultV1.Failed(FailureReasonV1.EXECUTION_HEAD_MISMATCH, "execution-head")
        }
        if (!request.repository.isAncestor(request.repositoryRoot, CORE_BASELINE_HEAD, executionHead)) {
            return ResultV1.Failed(FailureReasonV1.CORE_BASELINE_NOT_ANCESTOR, "core-baseline")
        }

        return when (val result = request.bridgeInvoker.prepare(request.bridgeRequest)) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1.Disabled ->
                ResultV1.Disabled
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1.Failed ->
                ResultV1.Failed(FailureReasonV1.BRIDGE_FAILED, result.safeContext, bridgeReason = result.reason)
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1.Prepared -> {
                if (!result.runtimeRequest.enabled) {
                    ResultV1.Failed(FailureReasonV1.PREPARED_RUNTIME_REQUEST_INVALID, "runtime-request")
                } else {
                    when (val runtime = request.runtimeInvoker.execute(result.runtimeRequest)) {
                        is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Completed ->
                            ResultV1.Completed(runtime, executionHead)
                        is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Failed ->
                            ResultV1.Failed(FailureReasonV1.RUNTIME_FAILED, runtime.safeContext, runtimeReason = runtime.reason)
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Disabled ->
                            ResultV1.Failed(FailureReasonV1.RUNTIME_DISABLED, "runtime")
                    }
                }
            }
        }
    }

    private object GitRepositoryPortV1 : RepositoryPortV1 {
        override fun currentHead(root: File): String = git(root, "rev-parse", "HEAD")

        override fun isAncestor(root: File, ancestor: String, descendant: String): Boolean {
            val process = ProcessBuilder("git", "merge-base", "--is-ancestor", ancestor, descendant)
                .directory(root)
                .redirectErrorStream(true)
                .start()
            process.inputStream.close()
            return process.waitFor() == 0
        }

        private fun git(root: File, vararg arguments: String): String {
            val process = ProcessBuilder(listOf("git") + arguments.toList())
                .directory(root)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            if (process.waitFor() != 0 || !output.matches(HEAD)) {
                throw IllegalArgumentException()
            }
            return output
        }
    }
}
