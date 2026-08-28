package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Assume.assumeTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1Test {
    @Test
    fun contractAndGateIdentityAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PACKET_REAL_BOUND_ENTRYPOINT_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.VERSION)
        assertEquals(
            "INDEPENDENT_VALIDATION_CONTEXT_ONLY",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.STATE,
        )
        assertEquals(
            "AUTHORIZED_BOUNDED_P1_PILOT_DECISION_VALIDATION_PACKET_OFFLINE",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.CONFIRMATION,
        )
    }

    @Test
    fun disabledGateReturnsBeforeInputOrRuntimeAccess() = withRoot { root ->
        var loaded = false
        var invoked = false
        val result = execute(
            root,
            gate = gate(
                enabled = false,
                confirmation = null,
                authorizedExecutionHead = null,
                sourceIntegrationEnabled = false,
            ),
            loader = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.InputLoaderV1 {
                loaded = true
                inputs()
            },
            invoker = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.RuntimeInvokerV1 {
                invoked = true
                runtimeFailure()
            },
        )
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ResultV1.Disabled>(result)
        assertFalse(loaded)
        assertFalse(invoked)
        assertTrue(root.toFile().listFiles().isNullOrEmpty())
    }

    @Test
    fun incompleteConfirmationHeadAndSourceGatesFailClosed() = withRoot { root ->
        assertFailure(execute(root, gate = gate(confirmation = null)), "CONFIRMATION_REQUIRED")
        assertFailure(execute(root, gate = gate(confirmation = "wrong")), "CONFIRMATION_REQUIRED")
        assertFailure(execute(root, gate = gate(authorizedExecutionHead = null)), "EXECUTION_HEAD_REQUIRED")
        assertFailure(execute(root, gate = gate(sourceIntegrationEnabled = false)), "SOURCE_INTEGRATION_REQUIRED")
    }

    @Test
    fun executionHeadAndAncestorGatesFailClosed() = withRoot { root ->
        val repository = fakeRepository(current = "a".repeat(40), ancestorResult = true)
        assertFailure(execute(root, repository = repository), "EXECUTION_HEAD_MISMATCH")
        assertFailure(
            execute(
                root,
                repository = fakeRepository(current = AUTHORIZED_HEAD, ancestorResult = false),
            ),
            "CORE_BASELINE_NOT_ANCESTOR",
        )
    }

    @Test
    fun invalidInputsFailClosedBeforeRuntime() = withRoot { root ->
        val invalid = inputs().copy(decisionBatch = inputs().decisionBatch.copy(contractId = "invalid"))
        var invoked = false
        val result = execute(
            root,
            loader = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.InputLoaderV1 { invalid },
            invoker = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.RuntimeInvokerV1 {
                invoked = true
                runtimeFailure()
            },
        )
        assertFailure(result, "DECISION_BATCH_CONTRACT_INVALID", "decision-batch:contract")
        assertFalse(invoked)
    }

    @Test
    fun decisionBatchFailureDiagnosticsUseOnlyClosedSafeStages() {
        val reasons = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.FailureReasonV1.entries
            .map { it.name }
            .toSet()
        assertTrue(
            setOf(
                "DECISION_BATCH_FILE_BINDING_INVALID",
                "DECISION_BATCH_READ_FAILED",
                "DECISION_BATCH_CONTRACT_INVALID",
                "DECISION_BATCH_ID_MISMATCH",
                "DECISION_BATCH_SUBMISSION_ID_MISMATCH",
                "DECISION_BATCH_INPUT_BINDING_DIGEST_MISMATCH",
                "DECISION_BATCH_LOGICAL_DIGEST_MISMATCH",
                "DECISION_BATCH_COUNTER_MISMATCH",
                "DECISION_BATCH_PILOT_CONTENT_MISMATCH",
                "DECISION_BATCH_PACKET_BINDING_MISMATCH",
            ).all { it in reasons },
        )
        assertTrue(reasons.none { it.contains("THROWABLE") || it.contains("EXCEPTION") || it.contains("PATH") })
    }

    @Test
    fun runtimeFailureIsReturnedAsTypedResult() = withRoot { root ->
        val result = execute(root, invoker = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.RuntimeInvokerV1 { runtimeFailure() })
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ResultV1.Failed>(result)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.FailureReasonV1.RUNTIME_FAILED,
            failure.reason,
        )
        assertEquals("PERSISTENCE_FAILED", failure.safeContext)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.PERSISTENCE_FAILED,
            failure.runtimeReason,
        )
    }

    @Test
    fun happyPathUsesSyntheticInputsAndTemporaryOutputOnly() = withRoot { root ->
        val requests = mutableListOf<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1>()
        val invoker = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.RuntimeInvokerV1 { request ->
            requests += request
            runtimeCompleted(request)
        }
        val first = execute(root, invoker = invoker)
        val second = execute(root, invoker = invoker)
        val firstCompleted = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ResultV1.Completed>(first)
        val secondCompleted = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ResultV1.Completed>(second)
        assertEquals(AUTHORIZED_HEAD, firstCompleted.executionHead)
        assertEquals(firstCompleted.executionHead, secondCompleted.executionHead)
        assertEquals(2, requests.size)
        assertTrue(requests.all { it.outputRoot.toPath().startsWith(root) })
        assertTrue(requests.all { it.implementationHead == AUTHORIZED_HEAD })
        assertEquals(requests[0], requests[1])
        assertTrue(root.toFile().listFiles().isNullOrEmpty())
    }

    @Test
    fun `writes real-bound P1 pilot decision validation packet only when fully authorized`() {
        val enabled = System.getProperty(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ENABLED_PROPERTY,
        )
        val confirmation = System.getProperty(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.CONFIRMATION_PROPERTY,
        )
        val authorizedExecutionHead = System.getProperty(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.AUTHORIZED_EXECUTION_HEAD_PROPERTY,
        )
        val gate = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.GateV1(
            enabled = enabled == "true",
            confirmation = confirmation,
            authorizedExecutionHead = authorizedExecutionHead,
            sourceIntegrationEnabled = System.getProperty(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.SOURCE_INTEGRATION_PROPERTY,
            ) == "true",
        )
        assumeTrue(gate.isComplete())
        HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()

        val root = projectRoot()
        val result = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1
            .executeFromSystemProperties(root)
        val completed = when (result) {
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ResultV1.Completed -> result
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ResultV1.Failed ->
                fail("${result.reason} ${result.safeContext}")
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ResultV1.Disabled ->
                fail("UNEXPECTED_DISABLED")
        }
        val runtime = completed.runtime
        val packet = runtime.packet
        val contract = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1
        val persistence = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1
        val counters = packet.counters

        assertEquals(contract.CONTRACT_ID, packet.contractId)
        assertEquals(contract.VERSION, packet.version)
        assertEquals(contract.STATE, packet.state)
        assertEquals(contract.PACKET_ID, packet.packetId)
        assertEquals(contract.frozenInputBinding(), packet.inputBinding)
        assertTrue(
            completed.runtime.persistenceStatus == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1.CREATED ||
                completed.runtime.persistenceStatus == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
        )

        assertEquals(4, counters.packetItems)
        assertEquals(4, counters.distinctReviewUnits)
        assertEquals(2, counters.originalConfirmDecisions)
        assertEquals(2, counters.originalRejectDecisions)
        assertEquals(0, counters.originalAbstainDecisions)
        assertEquals(0, counters.originalEscalateDecisions)
        assertEquals(4, counters.sourceEvidenceRecords)
        assertEquals(4, counters.catalogEvidenceRecords)
        assertEquals(4, counters.authorityEvidenceRecords)
        assertEquals(12, counters.directEvidenceReferences)
        assertEquals(12, counters.distinctEvidenceReferences)
        assertEquals(4, counters.supportsAssociationEvidence)
        assertEquals(2, counters.contradictsAssociationEvidence)
        assertEquals(6, counters.contextOnlyEvidence)
        assertEquals(0, counters.itemsWithAlternativeCanonicalProposal)
        assertEquals(0, counters.itemsWithValidationAssessment)
        assertEquals(0, counters.itemsWithDownstreamRoute)

        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            ),
            packet.items.map { it.originalDecision },
        )
        val rejectReasonCodes = packet.items
            .filter { it.originalDecision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION }
            .flatMap { it.originalReasonCodes }
            .toSet()
        assertEquals(
            setOf(
                HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_SEMANTIC_MISMATCH,
                HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED,
            ),
            rejectReasonCodes,
        )
        assertTrue(packet.items.all { it.alternativeCanonicalProposal == null })

        val evidence = packet.items.flatMap { item ->
            assertEquals(1, item.directEvidence.count { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION })
            assertEquals(1, item.directEvidence.count { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD })
            assertEquals(1, item.directEvidence.count { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD })
            item.directEvidence
        }
        assertEquals(12, evidence.size)
        assertEquals(12, evidence.map { it.evidenceReferenceId }.distinct().size)
        assertTrue(evidence.all { it.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT })
        assertEquals(4, evidence.count { it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION })
        assertEquals(2, evidence.count { it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION })
        assertEquals(6, evidence.count { it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY })

        assertFalse(runtime.jsonPath.startsWith('/'))
        assertFalse(runtime.markdownPath.startsWith('/'))
        assertEquals("${packet.packetId}/${persistence.JSON_FILE_NAME}", runtime.jsonPath)
        assertEquals("${packet.packetId}/${persistence.MARKDOWN_FILE_NAME}", runtime.markdownPath)
        val outputRoot = root.resolve(persistence.OUTPUT_ROOT).canonicalFile.toPath()
        val outputDirectory = outputRoot.resolve(packet.packetId).normalize()
        val json = outputRoot.resolve(runtime.jsonPath).normalize().toFile()
        val markdown = outputRoot.resolve(runtime.markdownPath).normalize().toFile()
        assertTrue(json.toPath().startsWith(outputRoot))
        assertTrue(markdown.toPath().startsWith(outputRoot))
        assertTrue(json.toPath().startsWith(outputDirectory))
        assertTrue(markdown.toPath().startsWith(outputDirectory))
        assertTrue(json.isFile)
        assertTrue(markdown.isFile)
        assertFalse(Files.isSymbolicLink(json.toPath()))
        assertFalse(Files.isSymbolicLink(markdown.toPath()))
        assertEquals(
            setOf(persistence.JSON_FILE_NAME, persistence.MARKDOWN_FILE_NAME),
            outputDirectory.toFile().list()?.toSet(),
        )
        assertFalse(root.resolve(persistence.JSON_FILE_NAME).isFile)
        assertFalse(root.resolve(persistence.MARKDOWN_FILE_NAME).isFile)

        val jsonBytes = json.readBytes()
        val markdownBytes = markdown.readBytes()
        assertTrue(jsonBytes.isNotEmpty() && jsonBytes.last() == '\n'.code.toByte())
        assertTrue(markdownBytes.isNotEmpty() && markdownBytes.last() == '\n'.code.toByte())
        assertEquals(json.length(), jsonBytes.size.toLong())
        assertEquals(markdown.length(), markdownBytes.size.toLong())
        assertEquals(runtime.jsonSha256, sha256(jsonBytes))
        assertEquals(runtime.markdownSha256, sha256(markdownBytes))
        assertEquals(packet.packetBindingDigest, runtime.packetBindingDigest)
        assertEquals(packet.packetLogicalDigest, runtime.packetLogicalDigest)
        assertEquals(packet.packetBindingDigest, contract.packetBindingDigest(packet))
        assertEquals(packet.packetLogicalDigest, contract.packetLogicalDigest(packet))

        val reloaded = persistence.readPacket(json)
        assertEquals(packet, reloaded)
        assertTrue(persistence.validatePacket(reloaded).valid)
        assertEquals(runtime.packetBindingDigest, reloaded.packetBindingDigest)
        assertEquals(runtime.packetLogicalDigest, reloaded.packetLogicalDigest)
        println(
            "HIM_P1_DECISION_VALIDATION_PACKET " +
                "status=${runtime.persistenceStatus} " +
                "packetId=${packet.packetId} " +
                "state=${packet.state} " +
                "json=${runtime.jsonPath} " +
                "markdown=${runtime.markdownPath} " +
                "items=${counters.packetItems} " +
                "reviewUnits=${counters.distinctReviewUnits} " +
                "directEvidence=${counters.directEvidenceReferences} " +
                "uniqueEvidence=${counters.distinctEvidenceReferences} " +
                "packetBindingDigest=${runtime.packetBindingDigest} " +
                "packetLogicalDigest=${runtime.packetLogicalDigest} " +
                "jsonBytes=${jsonBytes.size} " +
                "jsonSha256=${runtime.jsonSha256} " +
                "markdownBytes=${markdownBytes.size} " +
                "markdownSha256=${runtime.markdownSha256}",
        )
    }

    private fun execute(
        root: Path,
        gate: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.GateV1 = gate(),
        repository: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.RepositoryPortV1 = fakeRepository(),
        loader: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.InputLoaderV1 = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.InputLoaderV1 { inputs() },
        invoker: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.RuntimeInvokerV1 = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.RuntimeInvokerV1 { runtimeFailure() },
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.execute(
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.RequestV1(
            gate = gate,
            repositoryRoot = root.toFile(),
            repository = repository,
            inputLoader = loader,
            runtimeInvoker = invoker,
            outputRoot = root.resolve("temporary-output").toFile(),
        ),
    )

    private fun gate(
        enabled: Boolean = true,
        confirmation: String? = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.CONFIRMATION,
        authorizedExecutionHead: String? = AUTHORIZED_HEAD,
        sourceIntegrationEnabled: Boolean = true,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.GateV1(
        enabled,
        confirmation,
        authorizedExecutionHead,
        sourceIntegrationEnabled,
    )

    private fun fakeRepository(current: String = AUTHORIZED_HEAD, ancestorResult: Boolean = true) = object : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.RepositoryPortV1 {
        override fun currentHead(root: java.io.File) = current

        override fun isAncestor(root: java.io.File, ancestor: String, descendant: String) = ancestorResult
    }

    private fun runtimeFailure() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Failed(
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.PERSISTENCE_FAILED,
        "persistence",
    )

    private fun runtimeCompleted(request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Completed {
        val packet = validationPacket(request)
        val counters = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeCountersV1(
            packetItems = packet.counters.packetItems,
            distinctReviewUnits = packet.counters.distinctReviewUnits,
            confirmDecisions = packet.counters.originalConfirmDecisions,
            rejectDecisions = packet.counters.originalRejectDecisions,
            directEvidenceReferences = packet.counters.directEvidenceReferences,
            distinctEvidenceReferences = packet.counters.distinctEvidenceReferences,
            supportsAssociationEvidence = packet.counters.supportsAssociationEvidence,
            contradictsAssociationEvidence = packet.counters.contradictsAssociationEvidence,
            contextOnlyEvidence = packet.counters.contextOnlyEvidence,
        )
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Completed(
            packet,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1.CREATED,
            "temporary-output/validation-packet.v1.json",
            "temporary-output/validation-packet.v1.md",
            counters,
            "a".repeat(64),
            "b".repeat(64),
            "c".repeat(64),
            "d".repeat(64),
        )
    }

    private fun validationPacket(request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.create(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.frozenInputBinding(),
            request.reviewPacket.items.map { item ->
                val selection = request.decisionBatch.selections.single { it.reviewUnitId == item.reviewUnitId }
                val bundle = request.directEvidenceSupplement.bundles.single { it.reviewUnit.reviewUnitId == item.reviewUnitId }
                val rationale = "Synthetic test validation context."
                val base = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1(
                    item.reviewUnitId,
                    item.stableEntryId,
                    item.canonicalEntityId,
                    item.targetContext.expectedDisplayLabel,
                    item.sourceContext,
                    item.originalFields,
                    "",
                    selection.decision,
                    selection.reasonCodes,
                    bundle.evidence.map { it.evidenceReferenceId },
                    rationale,
                    sha256(rationale),
                    null,
                    item.targetContext,
                    bundle.evidence,
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.REQUIRED_CONTEXT_LIMITATIONS,
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_ASSESSMENTS,
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_REASON_CODES,
                    "",
                    "",
                )
                base.copy(
                    originalDecisionIdentity = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.originalDecisionIdentity(base),
                )
            },
        )

    private fun inputs(): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.BoundInputsV1 {
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.BoundInputsV1(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.create(),
            reviewPacket(),
            supplement(),
        )
    }

    private fun reviewPacket(): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1 {
        val mission = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION
        val input = reviewInputBinding()
        val items = mission.entries.mapIndexed { index, entry ->
            val value = if (entry.canonicalEntityId == "ZuhV5V") "Artischocken Herzen" else "Brie double crème"
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1(
                entry.stableEntryId,
                entry.reviewUnitId,
                entry.canonicalEntityId,
                entry.groupOrdinal,
                entry.groupDisplayValue,
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1(
                    HimGroundTruthSource.OPEN_FOOD_FACTS,
                    HimEvidenceRecordKind.OFF_PRODUCT,
                    "off:product:row:${index + 1}:code:test",
                    listOf(sha256("finding$index")),
                    listOf(HimZeroCandidateRecoveryReviewSelectionReasonV1.RECURRING_PRIMARY_VALUE_GROUP),
                    listOf(value),
                    HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH,
                    listOf(HimZeroCandidateRecoveryReviewGroupMembershipV1(entry.groupDisplayValue, HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET, 2, 2, listOf(entry.canonicalEntityId))),
                ),
                listOf(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1("name", value, sha256(value), value, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL, value.length)),
                target(entry.canonicalEntityId, entry.expectedDisplayLabel),
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceScopeV1(
                    HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD,
                    HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    "fixture/review-corpus.json",
                    "b".repeat(64),
                    null,
                    "off:product:row:${index + 1}:code:test",
                    listOf("name"),
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY,
                ),
                listOf(
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FULL_SOURCE_RECORD_NOT_INCLUDED,
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.IDENTITY_TERMS_ABSENT,
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.ALIAS_TERMS_ABSENT,
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.SOURCE_PROJECTION_NOT_INCLUDED,
                ).sortedBy { it.ordinal },
                "",
                "",
            )
        }
        return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.create(
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.missionBinding(mission),
            input,
            input.corpusFileBinding,
            input.corpusLogicalDigest,
            input.corpusBindingDigest,
            "a".repeat(40),
            items,
        )
    }

    private fun supplement() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1(
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.CONTRACT_ID,
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.VERSION,
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.MISSION_ID,
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.SCOPE_ID,
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_SELECTION_DIGEST,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.CONTRACT_ID,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.VERSION,
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_INPUT_BINDING_DIGEST,
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_BINDING_DIGEST,
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_LOGICAL_DIGEST,
            artifact(
                "fixture/review-corpus.json",
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.CORPUS_SIZE,
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CORPUS_SHA256,
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CORPUS_LOGICAL_DIGEST,
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CORPUS_BINDING_DIGEST,
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_IMPLEMENTATION_HEAD,
        ),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.FROZEN_REVIEW_UNITS.mapIndexed { index, unit ->
            val sourcePosition = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
            val catalogPosition = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1(
                unit,
                listOf("a61bd239809b266d8a8ca36ef98b8b635966b4d0e6f6716468b77ef284829f29", "0fb192a227076e28204d841ad8573cc33e486fcfd8991678fa24f7727937fb35", "2fef9dcff87ceeebfe0ae5ca2f9996ab5e8115c13c9949bb6bb0a1b1ed3e5287", "1528b8f9dfd01272762f10e1f22a7f7f4ecad05cb1f3ee59493b6c2eb6d91722")[index],
                listOf("50af5a0589bc19f9154d840091cb5ccbc7d7250b2fca5a3a9a1d05967905c418", "7b26d34c7335cd0f03ffaf49d72bd5edff36a2e9df465ce3251acb9e8839a57c", "ef5008317a70136fa95b4f025cd060544af249db579551cc996694ec9fa35ab0", "dd78dbf8d17943f1d6f7645903f305e33f571f3efb70d4dff8a46e77dfb92d89")[index],
                listOf(
                    card(unit, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, sourcePosition, "source", "off:product:row:${index + 1}:code:${index + 1}", HimGroundTruthSource.OPEN_FOOD_FACTS, HimEvidenceRecordKind.OFF_PRODUCT),
                    card(unit, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, catalogPosition, "catalog", "catalog:${unit.canonicalEntityId}", null, null),
                    card(unit, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY, "authority", "authority:${unit.canonicalEntityId}", null, null),
                ),
                "",
                "",
            )
        },
    )

    private fun card(
        unit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1,
        kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
        position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
        artifactName: String,
        record: String,
        source: HimGroundTruthSource?,
        recordKind: HimEvidenceRecordKind?,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(
        unit.reviewUnitId,
        "",
        kind,
        HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
        position,
        artifact("fixture/$artifactName", 1, "3".repeat(64), "4".repeat(64)),
        record,
        listOf(field("projection.identity.productName", record)),
        source?.let { HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1(it, recordKind!!, artifact("fixture/$artifactName", 1, "3".repeat(64), "4".repeat(64)), record, listOf(field("projection.identity.productName", record)), artifact("fixture/source.json", 1, "5".repeat(64), "6".repeat(64))) },
    )

    private fun reviewInputBinding(): HimZeroCandidateRecoveryHumanReviewInputBindingV1 {
        val cause = HimZeroCandidateCauseAnalysisFileBindingV1("fixture/cause.json", 1, "c".repeat(64), "d".repeat(64))
        val corpusBase = HimZeroCandidateRecoveryReviewCorpusInputBindingV1(cause, HimZeroCandidateRecoveryReviewCorpusFileBindingV1("fixture/catalog.json", 1, "1".repeat(64), "2".repeat(64)), HimZeroCandidateRecoveryReviewCorpusFileBindingV1("fixture/authority.json", 1, "3".repeat(64), "4".repeat(64)), "d".repeat(64), AUTHORIZED_HEAD, "")
        val corpus = corpusBase.copy(bindingDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.bindingDigest(corpusBase))
        val base = HimZeroCandidateRecoveryHumanReviewInputBindingV1(HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/review-corpus.json", 1, "5".repeat(64), "6".repeat(64)), "6".repeat(64), "7".repeat(64), "8".repeat(64), corpus, HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/registry.json", 1, "9".repeat(64), "a".repeat(64)), HimZeroCandidateRecoveryHumanReviewContractV1.VERSION, HimZeroCandidateRecoveryHumanReviewContractV1.VERSION, AUTHORIZED_HEAD, "")
        return base.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(base))
    }

    private fun target(id: String, label: String) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1(id, label, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, "catalog:$id", "authority:$id", emptyList(), emptyList(), "").let { it.copy(targetContextDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(it)) }
    private fun artifact(path: String, size: Long, sha256: String, logicalDigest: String?) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1(path, size, sha256, logicalDigest)
    private fun field(reference: String, value: String) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1(reference, value, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1.sha256(value))
    private fun sha256(value: String) = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(value)

    private fun assertFailure(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ResultV1,
        expected: String,
        expectedContext: String? = null,
    ) {
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ResultV1.Failed>(result)
        assertEquals(expected, failure.reason.name)
        expectedContext?.let { assertEquals(it, failure.safeContext) }
    }

    private fun withRoot(block: (Path) -> Unit) {
        val root = Files.createTempDirectory("him-decision-validation-packet-real-bound")
        try {
            block(root)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("USER_DIR_UNAVAILABLE")).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) return current
            current = current.parentFile ?: error("REPOSITORY_ROOT_NOT_FOUND")
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private companion object {
        val AUTHORIZED_HEAD = "b".repeat(40)
    }
}
