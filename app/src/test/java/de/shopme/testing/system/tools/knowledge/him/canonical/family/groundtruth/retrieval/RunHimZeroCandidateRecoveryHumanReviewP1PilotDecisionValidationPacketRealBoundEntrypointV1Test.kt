package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
        assertFailure(result, "INVALID_DECISION_BATCH")
        assertFalse(invoked)
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

    private fun assertFailure(result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ResultV1, expected: String) {
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRealBoundEntrypointV1.ResultV1.Failed>(result)
        assertEquals(expected, failure.reason.name)
    }

    private fun withRoot(block: (Path) -> Unit) {
        val root = Files.createTempDirectory("him-decision-validation-packet-real-bound")
        try {
            block(root)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private companion object {
        val AUTHORIZED_HEAD = "b".repeat(40)
    }
}
