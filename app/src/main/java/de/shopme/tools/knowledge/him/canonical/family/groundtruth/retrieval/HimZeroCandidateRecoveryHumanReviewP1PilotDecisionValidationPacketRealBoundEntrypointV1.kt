package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Opt-in-only binding layer for the independent P1 decision-validation packet.
 * It loads already persisted, immutable inputs and delegates packet semantics to
 * [HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1].
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PACKET_REAL_BOUND_ENTRYPOINT_V1"
    const val VERSION = "1"
    const val STATE = "INDEPENDENT_VALIDATION_CONTEXT_ONLY"

    const val ENABLED_PROPERTY =
        "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacket.enabled"
    const val CONFIRMATION_PROPERTY =
        "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacket.confirmation"
    const val AUTHORIZED_EXECUTION_HEAD_PROPERTY =
        "him.zeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacket.authorizedExecutionHead"
    const val SOURCE_INTEGRATION_PROPERTY = "him.sourceIntegration.enabled"
    const val CONFIRMATION =
        "AUTHORIZED_BOUNDED_P1_PILOT_DECISION_VALIDATION_PACKET_OFFLINE"
    const val CORE_BASELINE_HEAD = "ae12a546bcc87b5c28bf2ec45b72b1d705ab6360"

    private val HEAD = Regex("[0-9a-f]{40}")

    enum class FailureReasonV1 {
        CONFIRMATION_REQUIRED,
        EXECUTION_HEAD_REQUIRED,
        EXECUTION_HEAD_MISMATCH,
        SOURCE_INTEGRATION_REQUIRED,
        REPOSITORY_ROOT_INVALID,
        CORE_BASELINE_NOT_ANCESTOR,
        INPUT_MISSING,
        INPUT_BINDING_MISMATCH,
        INVALID_DECISION_BATCH,
        INVALID_REVIEW_PACKET,
        INVALID_DIRECT_EVIDENCE_SUPPLEMENT,
        RUNTIME_FAILED,
        RUNTIME_DISABLED,
        OUTPUT_CONFLICT,
        INPUT_LOAD_FAILED,
        DECISION_BATCH_FILE_BINDING_INVALID,
        DECISION_BATCH_READ_FAILED,
        DECISION_BATCH_CONTRACT_INVALID,
        DECISION_BATCH_ID_MISMATCH,
        DECISION_BATCH_SUBMISSION_ID_MISMATCH,
        DECISION_BATCH_INPUT_BINDING_DIGEST_MISMATCH,
        DECISION_BATCH_LOGICAL_DIGEST_MISMATCH,
        DECISION_BATCH_COUNTER_MISMATCH,
        DECISION_BATCH_PILOT_CONTENT_MISMATCH,
        DECISION_BATCH_PACKET_BINDING_MISMATCH,
    }

    data class GateV1(
        val enabled: Boolean,
        val confirmation: String?,
        val authorizedExecutionHead: String?,
        val sourceIntegrationEnabled: Boolean,
    ) {
        fun isComplete(): Boolean =
            enabled &&
                confirmation == CONFIRMATION &&
                authorizedExecutionHead?.matches(HEAD) == true &&
                sourceIntegrationEnabled

        companion object {
            fun fromSystemProperties(): GateV1 = GateV1(
                enabled = System.getProperty(ENABLED_PROPERTY) == "true",
                confirmation = System.getProperty(CONFIRMATION_PROPERTY),
                authorizedExecutionHead = System.getProperty(AUTHORIZED_EXECUTION_HEAD_PROPERTY),
                sourceIntegrationEnabled = System.getProperty(SOURCE_INTEGRATION_PROPERTY) == "true",
            )
        }
    }

    interface RepositoryPortV1 {
        fun currentHead(root: File): String

        fun isAncestor(root: File, ancestor: String, descendant: String): Boolean
    }

    fun interface InputLoaderV1 {
        fun load(root: File): BoundInputsV1
    }

    fun interface RuntimeInvokerV1 {
        fun execute(
            request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1,
        ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1
    }

    data class BoundInputsV1(
        val decisionBatch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1,
        val reviewPacket: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1,
        val directEvidenceSupplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
    )

    data class RequestV1(
        val gate: GateV1,
        val repositoryRoot: File,
        val repository: RepositoryPortV1,
        val inputLoader: InputLoaderV1,
        val runtimeInvoker: RuntimeInvokerV1 = RuntimeInvokerV1 { request ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1.execute(request)
        },
        val outputRoot: File = repositoryRoot.resolve(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1.OUTPUT_ROOT,
        ),
    )

    sealed interface ResultV1 {
        data object Disabled : ResultV1

        data class Completed(
            val runtime: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Completed,
            val executionHead: String,
        ) : ResultV1

        data class Failed(
            val reason: FailureReasonV1,
            val safeContext: String,
            val runtimeReason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1? = null,
        ) : ResultV1
    }

    fun executeFromSystemProperties(
        repositoryRoot: File,
        repository: RepositoryPortV1 = GitRepositoryPortV1,
        inputLoader: InputLoaderV1 = RealInputLoaderV1,
        runtimeInvoker: RuntimeInvokerV1 = RuntimeInvokerV1 { request ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1.execute(request)
        },
    ): ResultV1 = execute(
        RequestV1(
            GateV1.fromSystemProperties(),
            repositoryRoot,
            repository,
            inputLoader,
            runtimeInvoker,
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
        if (!request.gate.sourceIntegrationEnabled) {
            return ResultV1.Failed(FailureReasonV1.SOURCE_INTEGRATION_REQUIRED, "source-integration")
        }
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
        val inputs = try {
            request.inputLoader.load(request.repositoryRoot)
        } catch (failure: BoundInputFailureV1) {
            return ResultV1.Failed(failure.reason, failure.safeContext)
        }
        try {
            validateInputs(inputs)
        } catch (failure: BoundInputFailureV1) {
            return ResultV1.Failed(failure.reason, failure.safeContext)
        }
        val runtimeRequest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1(
            enabled = true,
            outputRoot = request.outputRoot,
            implementationHead = executionHead,
            decisionBatch = inputs.decisionBatch,
            reviewPacket = inputs.reviewPacket,
            directEvidenceSupplement = inputs.directEvidenceSupplement,
        )
        return when (val result = request.runtimeInvoker.execute(runtimeRequest)) {
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Completed ->
                ResultV1.Completed(result, executionHead)
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Failed ->
                ResultV1.Failed(FailureReasonV1.RUNTIME_FAILED, result.reason.name, result.reason)
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Disabled ->
                ResultV1.Failed(FailureReasonV1.RUNTIME_DISABLED, "runtime")
        }
    }

    private fun validateInputs(inputs: BoundInputsV1) {
        when (val validation = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.validate(inputs.decisionBatch)) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1.Invalid -> {
                val failure = decisionBatchContractFailure(validation.reason)
                fail(failure.reason, failure.safeContext)
            }
        }
        val mission = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION
        when (
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.validate(
                inputs.reviewPacket,
                mission,
                inputs.reviewPacket.humanReviewInputBinding,
            )
        ) {
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Invalid ->
                fail(FailureReasonV1.INVALID_REVIEW_PACKET, "review-packet")
        }
        when (HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.validate(inputs.directEvidenceSupplement)) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Invalid ->
                fail(FailureReasonV1.INVALID_DIRECT_EVIDENCE_SUPPLEMENT, "supplement")
        }
        val expected = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.frozenInputBinding()
        if (inputs.decisionBatch.packetBinding.inputBindingDigest != expected.reviewPacketInputBindingDigest ||
            inputs.decisionBatch.packetBinding.bindingDigest != expected.reviewPacketBindingDigest ||
            inputs.decisionBatch.packetBinding.logicalDigest != expected.reviewPacketLogicalDigest ||
            inputs.decisionBatch.supplementBinding.bindingDigest != expected.supplementBindingDigest ||
            inputs.decisionBatch.supplementBinding.logicalDigest != expected.supplementLogicalDigest ||
            inputs.decisionBatch.corpusBindingDigest != expected.corpusBindingDigest
        ) fail(FailureReasonV1.DECISION_BATCH_PACKET_BINDING_MISMATCH, "decision-batch:packet-binding")
        val supplementBinding = inputs.directEvidenceSupplement.binding
        if (supplementBinding.packetInputBindingDigest != expected.reviewPacketInputBindingDigest ||
            supplementBinding.packetBindingDigest != expected.reviewPacketBindingDigest ||
            supplementBinding.packetLogicalDigest != expected.reviewPacketLogicalDigest ||
            supplementBinding.corpusBindingDigest != expected.corpusBindingDigest
        ) fail(FailureReasonV1.DECISION_BATCH_PACKET_BINDING_MISMATCH, "decision-batch:packet-binding")
    }

    private data class FailureClassificationV1(
        val reason: FailureReasonV1,
        val safeContext: String,
    )

    private fun decisionBatchContractFailure(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1,
    ): FailureClassificationV1 = when (reason) {
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_SUBMISSION_ID ->
            FailureClassificationV1(FailureReasonV1.DECISION_BATCH_SUBMISSION_ID_MISMATCH, "decision-batch:submission-id")
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_PACKET_BINDING,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_SUPPLEMENT_BINDING,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_CORPUS_BINDING ->
            FailureClassificationV1(FailureReasonV1.DECISION_BATCH_PACKET_BINDING_MISMATCH, "decision-batch:packet-binding")
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_COUNTERS ->
            FailureClassificationV1(FailureReasonV1.DECISION_BATCH_COUNTER_MISMATCH, "decision-batch:counters")
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.SUBMISSION_BINDING_DIGEST_MISMATCH ->
            FailureClassificationV1(FailureReasonV1.DECISION_BATCH_INPUT_BINDING_DIGEST_MISMATCH, "decision-batch:input-binding")
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.SUBMISSION_LOGICAL_DIGEST_MISMATCH ->
            FailureClassificationV1(FailureReasonV1.DECISION_BATCH_LOGICAL_DIGEST_MISMATCH, "decision-batch:logical-digest")
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_REVIEWER_REF,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_REVIEW_ROUND,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_REVISION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_DECISION_COUNT,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.DECISION_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.REASON_CODES_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.EVIDENCE_REFERENCE_IDS_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.ALTERNATIVE_CANONICAL_PROPOSAL_FORBIDDEN,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.REVIEWER_NOTE_FORBIDDEN ->
            FailureClassificationV1(FailureReasonV1.DECISION_BATCH_PILOT_CONTENT_MISMATCH, "decision-batch:pilot-content")
        else -> FailureClassificationV1(FailureReasonV1.DECISION_BATCH_CONTRACT_INVALID, "decision-batch:contract")
    }

    private fun fail(reason: FailureReasonV1, safeContext: String): Nothing =
        throw BoundInputFailureV1(reason, safeContext)

    private data class BoundInputFailureV1(
        val reason: FailureReasonV1,
        val safeContext: String,
    ) : IllegalArgumentException()

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

    private object RealInputLoaderV1 : InputLoaderV1 {
        override fun load(root: File): BoundInputsV1 {
            val binding = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.frozenInputBinding()
            val decisionBatchFile = boundFile(
                root,
                binding.decisionBatch,
                FailureReasonV1.DECISION_BATCH_FILE_BINDING_INVALID,
                "decision-batch:file-binding",
            )
            val reviewPacketFile = boundFile(root, binding.reviewPacketJson)
            val supplementFile = boundFile(root, binding.supplementJson)
            val oldBatch = readDecisionBatch(decisionBatchFile)
            if (oldBatch.batchId != binding.decisionBatchId) {
                fail(FailureReasonV1.DECISION_BATCH_ID_MISMATCH, "decision-batch:id")
            }
            if (oldBatch.inputBinding.bindingDigest != binding.originalInputBindingDigest) {
                fail(FailureReasonV1.DECISION_BATCH_INPUT_BINDING_DIGEST_MISMATCH, "decision-batch:input-binding")
            }
            if (oldBatch.batchLogicalDigest != binding.originalBatchLogicalDigest) {
                fail(FailureReasonV1.DECISION_BATCH_LOGICAL_DIGEST_MISMATCH, "decision-batch:logical-digest")
            }
            val selections = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.FROZEN_SELECTIONS.map { expected ->
                val record = oldBatch.decisionRecords.singleOrNull { it.reviewUnit.reviewUnitId == expected.reviewUnitId }
                    ?: fail(FailureReasonV1.DECISION_BATCH_PILOT_CONTENT_MISMATCH, "decision-batch:pilot-content")
                if (record.reviewUnit.stableEntryId != expected.stableEntryId ||
                    record.reviewUnit.canonicalEntityId != expected.canonicalEntityId ||
                    record.reviewerRef != binding.originalReviewerRef ||
                    record.reviewRound != binding.originalReviewRound ||
                    record.revision != binding.originalRevision ||
                    record.decision != expected.decision ||
                    record.reasonCodes != expected.reasonCodes ||
                    record.evidenceReferences.map { it.evidenceReferenceId } != expected.evidenceReferenceIds ||
                    record.alternativeCanonicalProposal != null ||
                    record.reviewerNote != null
                ) fail(FailureReasonV1.DECISION_BATCH_PILOT_CONTENT_MISMATCH, "decision-batch:pilot-content")
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1(
                    record.reviewUnit.reviewUnitId,
                    record.reviewUnit.stableEntryId,
                    record.reviewUnit.canonicalEntityId,
                    record.decision,
                    record.reasonCodes,
                    record.evidenceReferences.map { it.evidenceReferenceId },
                    record.revision,
                    null,
                    null,
                )
            }
            val submission = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.create(selections)
            val reviewPacket = try {
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.readPacket(reviewPacketFile)
            } catch (_: Throwable) {
                fail(FailureReasonV1.INVALID_REVIEW_PACKET, "review-packet")
            }
            val supplement = try {
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.readSupplement(supplementFile)
            } catch (_: Throwable) {
                fail(FailureReasonV1.INVALID_DIRECT_EVIDENCE_SUPPLEMENT, "supplement")
            }
            return BoundInputsV1(submission, reviewPacket, supplement)
        }

        private fun boundFile(
            root: File,
            binding: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
            failureReason: FailureReasonV1 = FailureReasonV1.INPUT_MISSING,
            failureContext: String = "input-file",
        ): File {
            val rootPath = root.canonicalFile.toPath()
            val file = root.resolve(binding.relativePath).canonicalFile
            if (!file.toPath().startsWith(rootPath) ||
                !file.isFile ||
                Files.isSymbolicLink(file.toPath()) ||
                file.length() != binding.byteSize ||
                sha256(file) != binding.sha256
            ) fail(failureReason, failureContext)
            return file
        }

        private fun readDecisionBatch(file: File): HimZeroCandidateRecoveryHumanReviewDecisionBatchV1 = try {
            HimZeroCandidateRecoveryHumanReviewPersistenceV1.readBatch(file)
        } catch (failure: IllegalArgumentException) {
            when (failure.message) {
                "DESERIALIZATION_FAILED" ->
                    fail(FailureReasonV1.DECISION_BATCH_READ_FAILED, "decision-batch:deserialization")
                "READ_FAILED" ->
                    fail(FailureReasonV1.DECISION_BATCH_READ_FAILED, "decision-batch:read")
                else ->
                    fail(FailureReasonV1.DECISION_BATCH_READ_FAILED, "decision-batch:read")
            }
        } catch (_: Throwable) {
            fail(FailureReasonV1.DECISION_BATCH_READ_FAILED, "decision-batch:read")
        }

        private fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
    }
}
