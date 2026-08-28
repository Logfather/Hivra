package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1Test {
    @Test
    fun contractAndGateIdentityAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_REAL_BOUND_ENTRYPOINT_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.VERSION)
        assertEquals(
            "INDEPENDENT_VALIDATION_CONTEXT_ONLY",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.STATE,
        )
        assertEquals(
            "AUTHORIZED_BOUNDED_P1_PILOT_DECISION_VALIDATION_REAL_BOUND_OFFLINE",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.CONFIRMATION,
        )
    }

    @Test
    fun disabledGateReturnsBeforeBridgeOrRuntimeAccess() = withRoot { root ->
        var bridgeInvoked = false
        var runtimeInvoked = false
        val result = execute(
            root,
            gate = gate(enabled = false, confirmation = null, authorizedExecutionHead = null),
            bridgeInvoker = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.BridgeInvokerV1 {
                bridgeInvoked = true
                bridgeDisabled()
            },
            runtimeInvoker = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.RuntimeInvokerV1 {
                runtimeInvoked = true
                runtimeDisabled()
            },
        )
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.ResultV1.Disabled>(result)
        assertFalse(bridgeInvoked)
        assertFalse(runtimeInvoked)
        assertTrue(root.toFile().listFiles().isNullOrEmpty())
    }

    @Test
    fun incompleteGateFailsClosedBeforeBridgeAccess() = withRoot { root ->
        val cases = listOf(
            gate(confirmation = null) to HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.FailureReasonV1.CONFIRMATION_REQUIRED,
            gate(confirmation = "wrong") to HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.FailureReasonV1.CONFIRMATION_REQUIRED,
            gate(authorizedExecutionHead = null) to HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.FailureReasonV1.EXECUTION_HEAD_REQUIRED,
        )
        cases.forEach { (invalidGate, expected) ->
            var bridgeInvoked = false
            val result = execute(
                root,
                gate = invalidGate,
                bridgeInvoker = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.BridgeInvokerV1 {
                    bridgeInvoked = true
                    bridgeDisabled()
                },
            )
            assertEquals(expected, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.ResultV1.Failed>(result).reason)
            assertFalse(bridgeInvoked)
        }
    }

    @Test
    fun executionHeadAndAncestorGatesFailClosed() = withRoot { root ->
        val mismatch = execute(
            root,
            repository = fakeRepository(current = "b".repeat(40), ancestorResult = true),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.FailureReasonV1.EXECUTION_HEAD_MISMATCH,
            assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.ResultV1.Failed>(mismatch).reason,
        )
        val nonAncestor = execute(
            root,
            repository = fakeRepository(current = AUTHORIZED_HEAD, ancestorResult = false),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.FailureReasonV1.CORE_BASELINE_NOT_ANCESTOR,
            assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.ResultV1.Failed>(nonAncestor).reason,
        )
    }

    @Test
    fun invalidIndependentHumanInputStopsBeforeRuntimeOrPersistence() = withRoot { root ->
        var runtimeInvoked = false
        val result = execute(
            root,
            bridgeRequest = bridgeRequest(root).copy(validatorReviewerRef = null),
            runtimeInvoker = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.RuntimeInvokerV1 {
                runtimeInvoked = true
                runtimeDisabled()
            },
        )
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.ResultV1.Failed>(result)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.FailureReasonV1.BRIDGE_FAILED,
            failure.reason,
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeFailureReasonV1.INVALID_VALIDATOR_REVIEWER,
            failure.bridgeReason,
        )
        assertEquals("validator", failure.safeContext)
        assertFalse(runtimeInvoked)
        assertTrue(root.toFile().listFiles().isNullOrEmpty())
    }

    @Test
    fun successfulSyntheticExecutionUsesBridgeRuntimePersistenceAndReload() = withRoot { root ->
        val requests = mutableListOf<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1>()
        val result = execute(
            root,
            runtimeInvoker = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.RuntimeInvokerV1 { request ->
                requests += request
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1.execute(request)
            },
        )
        val completed = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.ResultV1.Completed>(result)
        assertEquals(AUTHORIZED_HEAD, completed.executionHead)
        assertEquals(4, completed.runtime.validationBatch.records.size)
        assertEquals(1, requests.size)
        assertTrue(root.resolve("durable").toFile().isDirectory)
        assertTrue(root.resolve("reports").toFile().isDirectory)
        assertTrue(root.resolve("durable").toFile().walkTopDown().any { it.isFile })
        assertTrue(root.resolve("reports").toFile().walkTopDown().any { it.isFile })
        assertTrue(completed.runtime.durableRelativePath.startsWith("p1-independent-validation-v1/"))
        assertFalse(completed.runtime.durableRelativePath.startsWith('/'))
    }

    @Test
    fun identicalAuthorizedExecutionIsIdempotentAndConflictsFailClosed() = withRoot { root ->
        val request = bridgeRequest(root)
        val first = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.ResultV1.Completed>(
            execute(root, bridgeRequest = request),
        )
        val second = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.ResultV1.Completed>(
            execute(root, bridgeRequest = request),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
            second.runtime.persistenceStatus,
        )
        assertEquals(first.runtime.validationBatch, second.runtime.validationBatch)
        assertEquals(first.runtime.durableJsonSha256, second.runtime.durableJsonSha256)

        val durable = root.resolve("durable").resolve(first.runtime.durableRelativePath).toFile()
        val original = durable.readBytes()
        durable.writeBytes(byteArrayOf(1, 2, 3))
        val conflict = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.ResultV1.Failed>(
            execute(root, bridgeRequest = request),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.FailureReasonV1.RUNTIME_FAILED,
            conflict.reason,
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1.PERSISTENCE_FAILED,
            conflict.runtimeReason,
        )
        assertFalse(original.contentEquals(durable.readBytes()))
    }

    @Test
    fun entrypointContainsNoDownstreamMutationSurface() {
        val names = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.ResultV1.Completed::class.java.declaredFields
            .map { it.name.lowercase() }
        assertTrue(names.none { it.contains("gold") || it.contains("training") || it.contains("authority") || it.contains("catalog") })
    }

    private fun execute(
        root: Path,
        gate: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.GateV1 = gate(),
        repository: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.RepositoryPortV1 = fakeRepository(),
        bridgeRequest: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeRequestV1 = bridgeRequest(root),
        bridgeInvoker: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.BridgeInvokerV1 = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.BridgeInvokerV1 { request ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeV1.prepare(request)
        },
        runtimeInvoker: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.RuntimeInvokerV1 = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.RuntimeInvokerV1 { request ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1.execute(request)
        },
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.execute(
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.RequestV1(
            gate = gate,
            repositoryRoot = root.toFile(),
            repository = repository,
            bridgeRequest = bridgeRequest,
            bridgeInvoker = bridgeInvoker,
            runtimeInvoker = runtimeInvoker,
        ),
    )

    private fun gate(
        enabled: Boolean = true,
        confirmation: String? = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.CONFIRMATION,
        authorizedExecutionHead: String? = AUTHORIZED_HEAD,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.GateV1(
        enabled,
        confirmation,
        authorizedExecutionHead,
    )

    private fun fakeRepository(
        current: String = AUTHORIZED_HEAD,
        ancestorResult: Boolean = true,
    ) = object : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundEntrypointV1.RepositoryPortV1 {
        override fun currentHead(root: File): String = current

        override fun isAncestor(root: File, ancestor: String, descendant: String): Boolean = ancestorResult
    }

    private fun bridgeRequest(root: Path): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeRequestV1 {
        val packet = packet()
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeRequestV1(
            enabled = true,
            validatorReviewerRef = "validator:test:v1",
            assessments = packet.items.mapIndexed { index, item ->
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1(
                    reviewUnitId = item.reviewUnitId,
                    stableEntryId = item.stableEntryId,
                    canonicalEntityId = item.canonicalEntityId,
                    originalDecision = item.originalDecision,
                    assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
                    reasonCodes = listOf(
                        if (index == 0 || index == 2) {
                            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION
                        } else {
                            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION
                        },
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
                    ).sortedBy { it.ordinal },
                    evidenceReferenceIds = item.directEvidence.map { it.evidenceReferenceId }.sorted(),
                    rationale = "Independent validation rationale for unit ${index + 1}.",
                )
            },
            packet = packet,
            originalDecisionBatch = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.create(),
            validationBatchId = "p1-independent-validation-v1",
            validationRound = 1,
            validationRevision = 1,
            durableRoot = root.resolve("durable").toFile(),
            reportRoot = root.resolve("reports").toFile(),
        )
    }

    private fun packet(): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1 {
        val type = Class.forName(
            "de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval." +
                "RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeV1Test",
        )
        val method = type.getDeclaredMethod("packet")
        method.isAccessible = true
        return method.invoke(type.getDeclaredConstructor().newInstance()) as HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1
    }

    private fun bridgeDisabled() =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1.Disabled

    private fun runtimeDisabled() =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Disabled

    private fun withRoot(block: (Path) -> Unit) {
        val root = Files.createTempDirectory("him-p1-validation-entrypoint-test")
        try {
            block(root)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private companion object {
        val AUTHORIZED_HEAD = "a".repeat(40)
    }
}
