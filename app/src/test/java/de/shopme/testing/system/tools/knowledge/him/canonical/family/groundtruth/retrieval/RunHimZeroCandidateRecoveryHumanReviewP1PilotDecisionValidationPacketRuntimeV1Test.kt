package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPrimaryBucketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceScopeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewGroupMembershipV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewPriorityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewSelectionReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import java.nio.file.Files
import java.nio.file.Path

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1Test {
    @Test
    fun contractAndRuntimeIdentityAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PACKET_RUNTIME_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1.VERSION)
        assertEquals("INDEPENDENT_VALIDATION_CONTEXT_ONLY", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1.STATE)
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1::class.java.declaredFields.map { it.name }.containsAll(listOf("enabled", "outputRoot", "implementationHead", "decisionBatch", "reviewPacket", "directEvidenceSupplement")))
    }

    @Test
    fun disabledReturnsBeforeValidationOrIo() = withRoot { root ->
        val result = execute(root, enabled = false, implementationHead = "not-a-head", decisionBatch = invalidBatch())
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Disabled>(result)
        assertTrue(root.toFile().listFiles().isNullOrEmpty())
    }

    @Test
    fun validInputsMaterializeExactlyFourNeutralItemsAndCounters() = withRoot { root ->
        val completed = completed(execute(root))
        assertEquals(4, completed.packet.items.size)
        assertEquals(4, completed.packet.counters.packetItems)
        assertEquals(4, completed.packet.counters.distinctReviewUnits)
        assertEquals(2, completed.packet.counters.originalConfirmDecisions)
        assertEquals(2, completed.packet.counters.originalRejectDecisions)
        assertEquals(12, completed.packet.counters.directEvidenceReferences)
        assertEquals(12, completed.packet.counters.distinctEvidenceReferences)
        assertEquals(4, completed.packet.counters.supportsAssociationEvidence)
        assertEquals(2, completed.packet.counters.contradictsAssociationEvidence)
        assertEquals(6, completed.packet.counters.contextOnlyEvidence)
        assertEquals("INDEPENDENT_VALIDATION_CONTEXT_ONLY", completed.packet.state)
        assertEquals(4, completed.counters.packetItems)
        assertEquals(2, completed.counters.confirmDecisions)
        assertEquals(2, completed.counters.rejectDecisions)
        assertTrue(completed.jsonPath.startsWith("${completed.packet.packetId}/"))
        assertTrue(completed.markdownPath.startsWith("${completed.packet.packetId}/"))
        assertFalse(completed.jsonPath.startsWith('/'))
        assertFalse(completed.markdownPath.startsWith('/'))
    }

    @Test
    fun decisionsEvidenceAndNeutralFutureStateRemainBound() = withRoot { root ->
        val packet = completed(execute(root)).packet
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            ),
            packet.items.map { it.originalDecision },
        )
        assertTrue(packet.items.all { it.alternativeCanonicalProposal == null })
        assertTrue(packet.items.all { it.allowedValidationAssessments.isNotEmpty() })
        assertTrue(packet.items.all { it.allowedValidationReasonCodes.isNotEmpty() })
        assertTrue(packet.items.all { it.directEvidence.size == 3 })
        assertTrue(packet.items.all { it.directEvidence.all { evidence -> evidence.directness.name == "DIRECT" } })
        assertTrue(packet.items.all { it.itemBindingDigest.length == 64 && it.itemLogicalDigest.length == 64 })
    }

    @Test
    fun persistedPacketReloadsAndSecondExecutionIsByteIdentical() = withRoot { root ->
        val request = request(root)
        val first = completed(execute(request))
        val directory = root.resolve(first.packet.packetId)
        val jsonBefore = Files.readAllBytes(directory.resolve("validation-packet.v1.json"))
        val markdownBefore = Files.readAllBytes(directory.resolve("validation-packet.v1.md"))
        val second = completed(execute(request))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1.CREATED, first.persistenceStatus)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, second.persistenceStatus)
        assertTrue(jsonBefore.contentEquals(Files.readAllBytes(directory.resolve("validation-packet.v1.json"))))
        assertTrue(markdownBefore.contentEquals(Files.readAllBytes(directory.resolve("validation-packet.v1.md"))))
        assertEquals(first.packet, second.packet)
        assertEquals(first.packetBindingDigest, second.packetBindingDigest)
        assertEquals(first.packetLogicalDigest, second.packetLogicalDigest)
        assertEquals(first.jsonSha256, second.jsonSha256)
        assertEquals(first.markdownSha256, second.markdownSha256)
    }

    @Test
    fun invalidHeadAndBindingsFailClosedWithTypedReasons() = withRoot { root ->
        assertFailure(execute(request(root, implementationHead = "z".repeat(40))), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.INVALID_IMPLEMENTATION_HEAD)
        assertFailure(execute(request(root, decisionBatch = invalidBatch())), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.INVALID_DECISION_BATCH)
        assertFailure(execute(request(root, reviewPacket = reviewPacket().copy(packetImplementationHead = "b".repeat(40)))), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.INVALID_REVIEW_PACKET)
        assertFailure(execute(request(root, directEvidenceSupplement = supplement().copy(binding = supplement().binding.copy(corpusBindingDigest = "f".repeat(64))))), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.INVALID_DIRECT_EVIDENCE_SUPPLEMENT)
    }

    @Test
    fun missingDuplicateAndUnexpectedUnitsFailClosed() = withRoot { root ->
        val base = decisionBatch()
        assertFailure(execute(request(root, decisionBatch = base.copy(selections = base.selections.drop(1)))), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.INVALID_DECISION_BATCH)
        assertFailure(execute(request(root, decisionBatch = base.copy(selections = base.selections + base.selections.first()))), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.INVALID_DECISION_BATCH)
        assertFailure(execute(request(root, reviewPacket = reviewPacket().copy(items = reviewPacket().items.drop(1)))), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.INVALID_REVIEW_PACKET)
    }

    @Test
    fun evidenceMutationAndDecisionMutationFailBeforePublication() = withRoot { root ->
        val selection = decisionBatch().selections.first().copy(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION)
        val batch = decisionBatch().copy(selections = decisionBatch().selections.toMutableList().also { it[0] = selection })
        assertFailure(execute(request(root, decisionBatch = batch)), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.INVALID_DECISION_BATCH)
        val changed = supplement().copy(bundles = supplement().bundles.toMutableList().also { bundleList ->
            val bundle = bundleList[0]
            bundleList[0] = bundle.copy(evidence = bundle.evidence.map { evidence ->
                if (evidence.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION) evidence.copy(position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY) else evidence
            })
        })
        assertFailure(execute(request(root, directEvidenceSupplement = changed)), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.INVALID_DIRECT_EVIDENCE_SUPPLEMENT)
        assertTrue(root.toFile().listFiles().isNullOrEmpty())
    }

    @Test
    fun partialAndConflictingOutputsFailClosed() = withRoot { root ->
        val request = request(root)
        val directory = root.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.PACKET_ID)
        Files.createDirectories(directory)
        Files.write(directory.resolve("validation-packet.v1.json"), byteArrayOf(1))
        assertFailure(execute(request), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.PERSISTENCE_FAILED)
        Files.delete(directory.resolve("validation-packet.v1.json"))
        val first = completed(execute(request))
        val json = directory.resolve("validation-packet.v1.json")
        val original = Files.readAllBytes(json)
        Files.write(json, original + byteArrayOf(1))
        assertFailure(execute(request), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1.PERSISTENCE_FAILED)
        assertTrue(Files.readAllBytes(json).contentEquals(original + byteArrayOf(1)))
        assertNotNull(first)
    }

    @Test
    fun runtimeHasNoExternalPortsOrPromotionFields() {
        val methods = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1::class.java.declaredMethods.map { it.name.lowercase() }
        assertTrue(methods.none { name -> listOf("network", "openai", "search", "fetch", "sqlite", "inference", "source").any(name::contains) })
        val fields = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1::class.java.declaredFields.map { it.name.lowercase() }
        assertTrue(fields.none { name -> listOf("validator", "assessment", "gold", "training", "route", "approval", "timestamp", "random", "uuid").any(name::contains) })
    }

    private fun execute(
        root: Path,
        enabled: Boolean = true,
        implementationHead: String = "b".repeat(40),
        decisionBatch: de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1 = decisionBatch(),
        reviewPacket: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1 = reviewPacket(),
        directEvidenceSupplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1 = supplement(),
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1.execute(
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1(
            enabled,
            root.toFile(),
            implementationHead,
            decisionBatch,
            reviewPacket,
            directEvidenceSupplement,
        ),
    )

    private fun execute(request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeV1.execute(request)

    private fun request(root: Path, implementationHead: String = "b".repeat(40), decisionBatch: de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1 = decisionBatch(), reviewPacket: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1 = reviewPacket(), directEvidenceSupplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1 = supplement()) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeRequestV1(true, root.toFile(), implementationHead, decisionBatch, reviewPacket, directEvidenceSupplement)

    private fun completed(result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Completed = when (result) {
        is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Completed -> result
        is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Failed -> fail("${result.reason} ${result.safeContext}")
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Disabled -> fail("disabled")
    }

    private fun assertFailure(result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1, expected: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeFailureReasonV1) {
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketRuntimeResultV1.Failed>(result)
        assertEquals(expected, failure.reason)
        assertFalse(failure.safeContext.contains('/'))
    }

    private fun withRoot(block: (Path) -> Unit) {
        val root = Files.createTempDirectory("him-decision-validation-packet-runtime")
        try { block(root) } finally { root.toFile().deleteRecursively() }
    }

    private fun decisionBatch() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.create()

    private fun invalidBatch() = decisionBatch().copy(contractId = "invalid")

    private fun reviewPacket(): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1 {
        val mission = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION
        val records = listOf(
            "off:product:row:431650:code:4002239680509",
            "off:product:row:3272579:code:0061483010917",
            "off:product:row:1551407:code:4013200552046",
            "off:product:row:3322623:code:2026088009283",
        )
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
                    records[index],
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
                    "build/knowledge/him/review-corpus.json",
                    "b".repeat(64),
                    null,
                    records[index],
                    listOf("name"),
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY,
                ),
                listOf(
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.IDENTITY_TERMS_ABSENT,
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.ALIAS_TERMS_ABSENT,
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FULL_SOURCE_RECORD_NOT_INCLUDED,
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.SOURCE_PROJECTION_NOT_INCLUDED,
                ).sortedBy { it.ordinal },
                "",
                "",
            )
        }
        val input = reviewInputBinding()
        return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.create(
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.missionBinding(mission),
            input,
            input.corpusFileBinding,
            input.corpusLogicalDigest,
            input.corpusBindingDigest,
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_IMPLEMENTATION_HEAD,
            items,
        )
    }

    private fun supplement(): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1 {
        val units = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.FROZEN_REVIEW_UNITS
        val records = listOf(
            "off:product:row:431650:code:4002239680509",
            "off:product:row:3272579:code:0061483010917",
            "off:product:row:1551407:code:4013200552046",
            "off:product:row:3322623:code:2026088009283",
        )
        val bundles = units.mapIndexed { index, unit ->
            val sourcePosition = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
            val catalogPosition = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY
            val value = if (unit.canonicalEntityId == "ZuhV5V") "Artischocken Herzen" else "Brie double crème"
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1(
                unit,
                packetBinding(index),
                packetLogical(index),
                listOf(
                    sourceCard(unit, records[index], sourcePosition, index),
                    targetCard(unit, catalogPosition),
                    authorityCard(unit),
                ),
                "",
                "",
            )
        }
        return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(supplementBinding(), bundles)
    }

    private fun supplementBinding() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1(
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
        artifact("build/knowledge/him/review-corpus.json", 2359985, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CORPUS_SHA256, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CORPUS_LOGICAL_DIGEST),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CORPUS_BINDING_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_IMPLEMENTATION_HEAD,
    )

    private fun sourceFields(index: Int): List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1> = when (index) {
        0 -> listOf(
            field("projection.classification.labels[0]", "en:vegetarian"),
            field("projection.classification.labels[1]", "en:vegan"),
            field("projection.classification.labels[2]", "en:no-lactose"),
            field("projection.classification.nova.tags[0]", "unknown"),
            field("projection.classification.nutriScore.grade", "unknown"),
            field("projection.classification.nutriScore.version", "2023"),
            field("projection.environmentalEvidence.diagnostics.missingAgribalyseCategory", "true"),
            field("projection.environmentalEvidence.diagnostics.missingAgribalyseMatch", "true"),
            field("projection.environmentalEvidence.diagnostics.missingIngredients", "true"),
            field("projection.environmentalEvidence.diagnostics.missingLabels", "true"),
            field("projection.environmentalEvidence.diagnostics.missingOrigins", "true"),
            field("projection.environmentalEvidence.diagnostics.missingPackaging", "true"),
            field("projection.environmentalEvidence.diagnostics.status", "unknown"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].origin", "en:unknown"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].percent", "100.0"),
            field("projection.environmentalEvidence.origin.epiScore", "0.0"),
            field("projection.environmentalEvidence.origin.epiValue", "-5.0"),
            field("projection.environmentalEvidence.origin.originsFromCategories[0]", "en:unknown"),
            field("projection.environmentalEvidence.origin.originsFromSourceField[0]", "en:unknown"),
            field("projection.environmentalEvidence.origin.warning", "origins_are_100_percent_unknown"),
            field("projection.environmentalEvidence.packaging.score", "81.0"),
            field("projection.environmentalEvidence.packaging.value", "-2.0"),
            field("projection.environmentalEvidence.packaging.warning", "unspecified_shape"),
            field("projection.environmentalEvidence.productionSystem.value", "0.0"),
            field("projection.environmentalEvidence.productionSystem.warning", "no_label"),
            field("projection.environmentalEvidence.score.grade", "unknown"),
            field("projection.environmentalEvidence.score.tags[0]", "unknown"),
            field("projection.environmentalEvidence.threatenedSpecies.warning", "ingredients_missing"),
            field("projection.geography.countries[0]", "en:germany"),
            field("projection.identity.brands[0]", "xx:feinkost-dittmann"),
            field("projection.identity.productName", "Artischocken Herzen"),
            field("projection.identity.productNameEnglish", "Artischocken Herzen"),
            field("projection.identity.productNameGerman", "Artischocken Herzen"),
            field("projection.identity.productType", "food"),
            field("projection.identity.quantity", "330g"),
            field("projection.packagingEvidence.hierarchy[0]", "en:glass"),
            field("projection.packagingEvidence.items[0].foodContact", "true"),
            field("projection.packagingEvidence.items[0].material", "en:glass"),
            field("projection.packagingEvidence.materials[0]", "en:glass"),
            field("projection.packagingEvidence.taxonomy[0]", "en:glass"),
            field("projection.quality.complete", "false"),
            field("projection.quality.completeness", "0.575"),
            field("projection.quality.info[0]", "en:packaging-data-incomplete"),
            field("projection.quality.info[1]", "en:food-groups-1-unknown"),
            field("projection.quality.info[2]", "en:food-groups-2-unknown"),
            field("projection.quality.info[3]", "en:food-groups-3-unknown"),
            field("projection.quality.scans", "2"),
            field("projection.quality.uniqueScans", "2"),
            field("projection.quality.warnings[0]", "en:environmental-score-origins-of-ingredients-origins-are-100-percent-unknown"),
            field("projection.quality.warnings[1]", "en:environmental-score-packaging-unspecified-shape"),
            field("projection.quality.warnings[2]", "en:environmental-score-production-system-no-label"),
            field("projection.quality.warnings[3]", "en:environmental-score-threatened-species-ingredients-missing"),
            field("projection.rowOrdinal", "431650"),
            field("projection.source.code", "4002239680509"),
            field("projection.taxonomy.categories[0]", "de:artischocken-herzen"),
            field("projection.taxonomy.categoryHierarchy[0]", "de:artischocken herzen"),
            field("projection.taxonomy.ciqualReferences[0]", "unknown"),
            field("projection.taxonomy.pnnsGroups[0]", "unknown"),
        )
        1 -> listOf(
            field("projection.classification.nova.tags[0]", "unknown"),
            field("projection.classification.nutriScore.grade", "unknown"),
            field("projection.classification.nutriScore.version", "2021"),
            field("projection.environmentalEvidence.score.legacyEcoScoreGrade", "unknown"),
            field("projection.geography.countries[0]", "en:united-states"),
            field("projection.identity.productName", "Brie double crème"),
            field("projection.identity.productNameEnglish", "Brie double crème"),
            field("projection.identity.productType", "food"),
            field("projection.identity.servingSize", "30.0g"),
            field("projection.nutrition.declared.energy-kcal.per100g", "366.6666666666667"),
            field("projection.nutrition.declared.energy-kcal.perServing", "110.0"),
            field("projection.nutrition.declared.energy-kcal.unit", "kcal"),
            field("projection.nutrition.declared.energy-kcal.value", "366.6666666666667"),
            field("projection.nutrition.declared.energy.per100g", "1534.0"),
            field("projection.nutrition.declared.energy.perServing", "460.0"),
            field("projection.nutrition.declared.energy.unit", "kcal"),
            field("projection.nutrition.declared.energy.value", "1534.0"),
            field("projection.nutrition.declared.fat.per100g", "33.333333333333336"),
            field("projection.nutrition.declared.fat.perServing", "10.0"),
            field("projection.nutrition.declared.fat.unit", "g"),
            field("projection.nutrition.declared.fat.value", "33.333333333333336"),
            field("projection.nutrition.declared.proteins.per100g", "20.0"),
            field("projection.nutrition.declared.proteins.perServing", "6.0"),
            field("projection.nutrition.declared.proteins.unit", "g"),
            field("projection.nutrition.declared.proteins.value", "20.0"),
            field("projection.nutrition.declared.salt.per100g", "1.75"),
            field("projection.nutrition.declared.salt.perServing", "0.525"),
            field("projection.nutrition.declared.salt.unit", "g"),
            field("projection.nutrition.declared.salt.value", "1.75"),
            field("projection.nutrition.declared.saturated-fat.per100g", "20.0"),
            field("projection.nutrition.declared.saturated-fat.perServing", "6.0"),
            field("projection.nutrition.declared.saturated-fat.unit", "g"),
            field("projection.nutrition.declared.saturated-fat.value", "20.0"),
            field("projection.nutrition.declared.sodium.per100g", "0.7"),
            field("projection.nutrition.declared.sodium.perServing", "0.21"),
            field("projection.nutrition.declared.sodium.unit", "g"),
            field("projection.nutrition.declared.sodium.value", "0.7"),
            field("projection.quality.complete", "false"),
            field("projection.quality.completeness", "0.2"),
            field("projection.quality.info[0]", "en:no-packaging-data"),
            field("projection.quality.info[1]", "en:ecoscore-extended-data-not-computed"),
            field("projection.quality.info[2]", "en:food-groups-1-unknown"),
            field("projection.quality.info[3]", "en:food-groups-2-unknown"),
            field("projection.quality.info[4]", "en:food-groups-3-unknown"),
            field("projection.quality.scans", "1"),
            field("projection.quality.uniqueScans", "1"),
            field("projection.quality.warnings[0]", "en:serving-quantity-defined-but-quantity-undefined"),
            field("projection.quality.warnings[1]", "en:ecoscore-origins-of-ingredients-origins-are-100-percent-unknown"),
            field("projection.quality.warnings[2]", "en:ecoscore-packaging-packaging-data-missing"),
            field("projection.quality.warnings[3]", "en:ecoscore-production-system-no-label"),
            field("projection.quality.warnings[4]", "en:ecoscore-threatened-species-ingredients-missing"),
            field("projection.rowOrdinal", "3272579"),
            field("projection.source.code", "0061483010917"),
            field("projection.taxonomy.pnnsGroups[0]", "unknown"),
        )
        2 -> listOf(
            field("projection.classification.nova.tags[0]", "unknown"),
            field("projection.classification.nutriScore.grade", "unknown"),
            field("projection.classification.nutriScore.version", "2023"),
            field("projection.environmentalEvidence.diagnostics.missingAgribalyseMatch", "true"),
            field("projection.environmentalEvidence.diagnostics.missingCategories", "true"),
            field("projection.environmentalEvidence.diagnostics.missingIngredients", "true"),
            field("projection.environmentalEvidence.diagnostics.missingKeyData", "true"),
            field("projection.environmentalEvidence.diagnostics.missingLabels", "true"),
            field("projection.environmentalEvidence.diagnostics.missingOrigins", "true"),
            field("projection.environmentalEvidence.diagnostics.missingPackaging", "true"),
            field("projection.environmentalEvidence.diagnostics.status", "unknown"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].origin", "en:unknown"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].percent", "100.0"),
            field("projection.environmentalEvidence.origin.epiScore", "0.0"),
            field("projection.environmentalEvidence.origin.epiValue", "-5.0"),
            field("projection.environmentalEvidence.origin.originsFromCategories[0]", "en:unknown"),
            field("projection.environmentalEvidence.origin.originsFromSourceField[0]", "en:unknown"),
            field("projection.environmentalEvidence.origin.warning", "origins_are_100_percent_unknown"),
            field("projection.environmentalEvidence.packaging.value", "-15.0"),
            field("projection.environmentalEvidence.packaging.warning", "packaging_data_missing"),
            field("projection.environmentalEvidence.productionSystem.value", "0.0"),
            field("projection.environmentalEvidence.productionSystem.warning", "no_label"),
            field("projection.environmentalEvidence.score.grade", "unknown"),
            field("projection.environmentalEvidence.score.tags[0]", "unknown"),
            field("projection.environmentalEvidence.threatenedSpecies.warning", "ingredients_missing"),
            field("projection.geography.countries[0]", "en:germany"),
            field("projection.identity.productName", "Artischocken Herzen"),
            field("projection.identity.productNameGerman", "Artischocken Herzen"),
            field("projection.identity.productType", "food"),
            field("projection.nutrition.structured.aggregated.carbohydrates.source", "packaging"),
            field("projection.nutrition.structured.aggregated.carbohydrates.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.carbohydrates.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.carbohydrates.unit", "g"),
            field("projection.nutrition.structured.aggregated.carbohydrates.value", "6.5"),
            field("projection.nutrition.structured.aggregated.energy-kcal.source", "packaging"),
            field("projection.nutrition.structured.aggregated.energy-kcal.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.energy-kcal.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.energy-kcal.unit", "kcal"),
            field("projection.nutrition.structured.aggregated.energy-kcal.value", "38.0"),
            field("projection.nutrition.structured.aggregated.energy-kj.source", "packaging"),
            field("projection.nutrition.structured.aggregated.energy-kj.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.energy-kj.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.energy-kj.unit", "kj"),
            field("projection.nutrition.structured.aggregated.energy-kj.value", "134.9"),
            field("projection.nutrition.structured.aggregated.energy.source", "packaging"),
            field("projection.nutrition.structured.aggregated.energy.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.energy.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.energy.unit", "kj"),
            field("projection.nutrition.structured.aggregated.energy.value", "134.9"),
            field("projection.nutrition.structured.aggregated.fat.source", "packaging"),
            field("projection.nutrition.structured.aggregated.fat.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.fat.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.fat.unit", "g"),
            field("projection.nutrition.structured.aggregated.fat.value", "0.2"),
            field("projection.nutrition.structured.aggregated.proteins.source", "packaging"),
            field("projection.nutrition.structured.aggregated.proteins.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.proteins.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.proteins.unit", "g"),
            field("projection.nutrition.structured.aggregated.proteins.value", "1.0"),
            field("projection.nutrition.structured.aggregated.salt.source", "packaging"),
            field("projection.nutrition.structured.aggregated.salt.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.salt.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.salt.unit", "g"),
            field("projection.nutrition.structured.aggregated.salt.value", "0.8"),
            field("projection.nutrition.structured.aggregated.saturated-fat.source", "packaging"),
            field("projection.nutrition.structured.aggregated.saturated-fat.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.saturated-fat.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.saturated-fat.unit", "g"),
            field("projection.nutrition.structured.aggregated.saturated-fat.value", "0.1"),
            field("projection.nutrition.structured.aggregated.sodium.source", "packaging"),
            field("projection.nutrition.structured.aggregated.sodium.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.sodium.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.sodium.unit", "g"),
            field("projection.nutrition.structured.aggregated.sodium.value", "0.32"),
            field("projection.nutrition.structured.aggregated.sugars.source", "packaging"),
            field("projection.nutrition.structured.aggregated.sugars.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.sugars.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.sugars.unit", "g"),
            field("projection.nutrition.structured.aggregated.sugars.value", "1.2"),
            field("projection.nutrition.structured.inputSets[0].nutrients.carbohydrates.unit", "g"),
            field("projection.nutrition.structured.inputSets[0].nutrients.carbohydrates.value", "6.5"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kcal.unit", "kcal"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kcal.value", "38.0"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kj.unit", "kj"),
            field("projection.nutrition.structured.inputSets[0].nutrients.fat.unit", "g"),
            field("projection.nutrition.structured.inputSets[0].nutrients.fat.value", "0.2"),
            field("projection.nutrition.structured.inputSets[0].nutrients.proteins.unit", "g"),
            field("projection.nutrition.structured.inputSets[0].nutrients.proteins.value", "1.0"),
            field("projection.nutrition.structured.inputSets[0].nutrients.salt.unit", "g"),
            field("projection.nutrition.structured.inputSets[0].nutrients.salt.value", "0.8"),
            field("projection.nutrition.structured.inputSets[0].nutrients.saturated-fat.unit", "g"),
            field("projection.nutrition.structured.inputSets[0].nutrients.saturated-fat.value", "0.1"),
            field("projection.nutrition.structured.inputSets[0].nutrients.sodium.unit", "g"),
            field("projection.nutrition.structured.inputSets[0].nutrients.sodium.value", "0.32"),
            field("projection.nutrition.structured.inputSets[0].nutrients.sugars.unit", "g"),
            field("projection.nutrition.structured.inputSets[0].nutrients.sugars.value", "1.2"),
            field("projection.nutrition.structured.inputSets[0].per", "100g"),
            field("projection.nutrition.structured.inputSets[0].preparation", "as_sold"),
            field("projection.nutrition.structured.inputSets[0].source", "packaging"),
            field("projection.quality.complete", "false"),
            field("projection.quality.completeness", "0.275"),
            field("projection.quality.info[0]", "en:no-packaging-data"),
            field("projection.quality.info[1]", "en:food-groups-1-unknown"),
            field("projection.quality.info[2]", "en:food-groups-2-unknown"),
            field("projection.quality.info[3]", "en:food-groups-3-unknown"),
            field("projection.quality.warnings[0]", "en:nutrition-energy-value-in-kcal-does-not-match-value-in-kj"),
            field("projection.quality.warnings[1]", "en:environmental-score-origins-of-ingredients-origins-are-100-percent-unknown"),
            field("projection.quality.warnings[2]", "en:environmental-score-packaging-packaging-data-missing"),
            field("projection.quality.warnings[3]", "en:environmental-score-production-system-no-label"),
            field("projection.quality.warnings[4]", "en:environmental-score-threatened-species-ingredients-missing"),
            field("projection.rowOrdinal", "1551407"),
            field("projection.source.code", "4013200552046"),
            field("projection.taxonomy.pnnsGroups[0]", "unknown"),
        )
        3 -> listOf(
            field("projection.classification.nova.tags[0]", "unknown"),
            field("projection.classification.nutriScore.grade", "unknown"),
            field("projection.classification.nutriScore.version", "2023"),
            field("projection.environmentalEvidence.diagnostics.missingAgribalyseMatch", "true"),
            field("projection.environmentalEvidence.diagnostics.missingCategories", "true"),
            field("projection.environmentalEvidence.diagnostics.missingIngredients", "true"),
            field("projection.environmentalEvidence.diagnostics.missingKeyData", "true"),
            field("projection.environmentalEvidence.diagnostics.missingLabels", "true"),
            field("projection.environmentalEvidence.diagnostics.missingOrigins", "true"),
            field("projection.environmentalEvidence.diagnostics.missingPackaging", "true"),
            field("projection.environmentalEvidence.diagnostics.status", "unknown"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].origin", "en:unknown"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].percent", "100.0"),
            field("projection.environmentalEvidence.origin.epiScore", "0.0"),
            field("projection.environmentalEvidence.origin.epiValue", "-5.0"),
            field("projection.environmentalEvidence.origin.originsFromCategories[0]", "en:unknown"),
            field("projection.environmentalEvidence.origin.originsFromSourceField[0]", "en:unknown"),
            field("projection.environmentalEvidence.origin.warning", "origins_are_100_percent_unknown"),
            field("projection.environmentalEvidence.packaging.value", "-15.0"),
            field("projection.environmentalEvidence.packaging.warning", "packaging_data_missing"),
            field("projection.environmentalEvidence.productionSystem.value", "0.0"),
            field("projection.environmentalEvidence.productionSystem.warning", "no_label"),
            field("projection.environmentalEvidence.score.grade", "unknown"),
            field("projection.environmentalEvidence.score.tags[0]", "unknown"),
            field("projection.environmentalEvidence.threatenedSpecies.warning", "ingredients_missing"),
            field("projection.geography.countries[0]", "en:ireland"),
            field("projection.identity.productName", "Brie double crème"),
            field("projection.identity.productNameEnglish", "Brie double crème"),
            field("projection.identity.productType", "food"),
            field("projection.identity.servingSize", "30.0g"),
            field("projection.nutrition.structured.aggregated.carbohydrates.source", "packaging"),
            field("projection.nutrition.structured.aggregated.carbohydrates.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.carbohydrates.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.carbohydrates.unit", "g"),
            field("projection.nutrition.structured.aggregated.carbohydrates.value", "3.3333333333333335"),
            field("projection.nutrition.structured.aggregated.energy-kcal.source", "packaging"),
            field("projection.nutrition.structured.aggregated.energy-kcal.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.energy-kcal.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.energy-kcal.unit", "kcal"),
            field("projection.nutrition.structured.aggregated.energy-kcal.value", "333.33333333333337"),
            field("projection.nutrition.structured.aggregated.energy-kj.source", "packaging"),
            field("projection.nutrition.structured.aggregated.energy-kj.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.energy-kj.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.energy-kj.unit", "kj"),
            field("projection.nutrition.structured.aggregated.energy-kj.value", "1506.6666666666667"),
            field("projection.nutrition.structured.aggregated.energy.source", "packaging"),
            field("projection.nutrition.structured.aggregated.energy.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.energy.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.energy.unit", "kj"),
            field("projection.nutrition.structured.aggregated.energy.value", "1506.6666666666667"),
            field("projection.nutrition.structured.aggregated.fat.source", "packaging"),
            field("projection.nutrition.structured.aggregated.fat.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.fat.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.fat.unit", "g"),
            field("projection.nutrition.structured.aggregated.fat.value", "30.0"),
            field("projection.nutrition.structured.aggregated.proteins.source", "packaging"),
            field("projection.nutrition.structured.aggregated.proteins.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.proteins.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.proteins.unit", "g"),
            field("projection.nutrition.structured.aggregated.proteins.value", "20.0"),
            field("projection.nutrition.structured.aggregated.saturated-fat.source", "packaging"),
            field("projection.nutrition.structured.aggregated.saturated-fat.sourceIndex", "0"),
            field("projection.nutrition.structured.aggregated.saturated-fat.sourcePer", "100g"),
            field("projection.nutrition.structured.aggregated.saturated-fat.unit", "g"),
            field("projection.nutrition.structured.aggregated.saturated-fat.value", "20.0"),
            field("projection.nutrition.structured.inputSets[0].nutrients.carbohydrates.unit", "g"),
            field("projection.nutrition.structured.inputSets[0].nutrients.carbohydrates.value", "3.3333333333333335"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kcal.unit", "kcal"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kcal.value", "333.33333333333337"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kj.unit", "kj"),
            field("projection.nutrition.structured.inputSets[0].nutrients.fat.unit", "g"),
            field("projection.nutrition.structured.inputSets[0].nutrients.fat.value", "30.0"),
            field("projection.nutrition.structured.inputSets[0].nutrients.proteins.unit", "g"),
            field("projection.nutrition.structured.inputSets[0].nutrients.proteins.value", "20.0"),
            field("projection.nutrition.structured.inputSets[0].nutrients.saturated-fat.unit", "g"),
            field("projection.nutrition.structured.inputSets[0].nutrients.saturated-fat.value", "20.0"),
            field("projection.nutrition.structured.inputSets[0].per", "100g"),
            field("projection.nutrition.structured.inputSets[0].preparation", "as_sold"),
            field("projection.nutrition.structured.inputSets[0].source", "packaging"),
            field("projection.quality.complete", "false"),
            field("projection.quality.completeness", "0.2"),
            field("projection.quality.info[0]", "en:no-packaging-data"),
            field("projection.quality.info[1]", "en:food-groups-1-unknown"),
            field("projection.quality.info[2]", "en:food-groups-2-unknown"),
            field("projection.quality.info[3]", "en:food-groups-3-unknown"),
            field("projection.quality.warnings[0]", "en:serving-quantity-defined-but-quantity-undefined"),
            field("projection.quality.warnings[1]", "en:environmental-score-origins-of-ingredients-origins-are-100-percent-unknown"),
            field("projection.quality.warnings[2]", "en:environmental-score-packaging-packaging-data-missing"),
            field("projection.quality.warnings[3]", "en:environmental-score-production-system-no-label"),
            field("projection.quality.warnings[4]", "en:environmental-score-threatened-species-ingredients-missing"),
            field("projection.rowOrdinal", "3322623"),
            field("projection.source.code", "2026088009283"),
            field("projection.taxonomy.pnnsGroups[0]", "unknown"),
        )
        else -> error("unknown source fixture")
    }

    private fun sourceCard(unit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1, record: String, position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1, index: Int) = card(
        unit.reviewUnitId,
        HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION,
        position,
        sourceIndexArtifact(),
        record,
        sourceFields(index),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1(
            HimGroundTruthSource.OPEN_FOOD_FACTS,
            HimEvidenceRecordKind.OFF_PRODUCT,
            sourceIndexArtifact(),
            record,
            sourceFields(index),
            sourceArtifact(),
        ),
    )

    private fun sourceIndexArtifact() = artifact(
        "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite",
        25551749120,
        "80c7da8c2b0a94ee0b12fb03e095f50429d2c0b300ce70dc22d0b5095aabf5df",
        "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743",
    )

    private fun sourceArtifact() = artifact(
        "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz",
        0,
        "63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236",
        null,
    )
    private fun targetCard(unit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1, position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1) = card(
        unit.reviewUnitId,
        HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD,
        position,
        catalogArtifact(),
        "catalog:${unit.canonicalEntityId}",
        listOf(
            field("canonical.entityId", unit.canonicalEntityId),
            field("canonical.name", if (unit.canonicalEntityId == "ZuhV5V") "Artischocken" else "Crème double"),
            field("canonical.normalizedName", if (unit.canonicalEntityId == "ZuhV5V") "artischocken" else "creme-double"),
            field("canonical.taxonomyPaths", if (unit.canonicalEntityId == "ZuhV5V") "[[vegetables, mediterranean-vegetables, artichokes]]" else "[[dairy-and-eggs, dairy-products, cream, double-cream]]"),
        ),
        null,
    )

    private fun authorityCard(unit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1) = card(
        unit.reviewUnitId,
        HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD,
        HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY,
        authorityArtifact(),
        "authority:${unit.canonicalEntityId}",
        listOf(
            field("canonical.aliases", "[]"),
            field("canonical.entityId", unit.canonicalEntityId),
            field("canonical.identities", "[]"),
            field("canonical.lifecycleStatus", "ACTIVE"),
            field("canonical.name", if (unit.canonicalEntityId == "ZuhV5V") "Artischocken" else "Crème double"),
            field("canonical.normalizedName", if (unit.canonicalEntityId == "ZuhV5V") "artischocken" else "creme-double"),
            field("canonical.variants", "[]"),
        ),
        null,
    )

    private fun catalogArtifact() = artifact(
        "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json",
        266953,
        "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
        "6ffd1f6c08d3f2f9d02aba427540d99a94d8461517bf6ea24e08f342fff5406e",
    )

    private fun authorityArtifact() = artifact(
        "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json",
        532398,
        "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
        "d4a26bf27de8768498bea8277cfb903f572900af00fe499bff9f63dcbc44ca7c",
    )

    private fun card(unitId: String, kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1, position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1, artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1, record: String, fields: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1>, projection: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1?) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(unitId, "", kind, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, position, artifact, record, fields, projection)

    private fun target(id: String, label: String) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1(id, label, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, "catalog:$id", "authority:$id", emptyList(), emptyList(), "").let { it.copy(targetContextDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(it)) }

    private fun reviewInputBinding(): HimZeroCandidateRecoveryHumanReviewInputBindingV1 {
        val cause = HimZeroCandidateCauseAnalysisFileBindingV1("fixture/cause.json", 1, "c".repeat(64), "d".repeat(64))
        val corpusBase = HimZeroCandidateRecoveryReviewCorpusInputBindingV1(cause, HimZeroCandidateRecoveryReviewCorpusFileBindingV1("fixture/catalog.json", 1, "1".repeat(64), "2".repeat(64)), HimZeroCandidateRecoveryReviewCorpusFileBindingV1("fixture/authority.json", 1, "3".repeat(64), "4".repeat(64)), "d".repeat(64), "a".repeat(40), "")
        val corpus = corpusBase.copy(bindingDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.bindingDigest(corpusBase))
        val base = HimZeroCandidateRecoveryHumanReviewInputBindingV1(HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/review-corpus.json", 1, "5".repeat(64), "6".repeat(64)), "6".repeat(64), "7".repeat(64), "8".repeat(64), corpus, HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/registry.json", 1, "9".repeat(64), "a".repeat(64)), HimZeroCandidateRecoveryHumanReviewContractV1.VERSION, HimZeroCandidateRecoveryHumanReviewContractV1.VERSION, "a".repeat(40), "")
        return base.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(base))
    }

    private fun artifact(path: String, size: Long, sha256: String, logicalDigest: String?) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1(path, size, sha256, logicalDigest)
    private fun field(reference: String, value: String) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1(reference, value, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1.sha256(value))
    private fun packetBinding(index: Int) = listOf("a61bd239809b266d8a8ca36ef98b8b635966b4d0e6f6716468b77ef284829f29", "0fb192a227076e28204d841ad8573cc33e486fcfd8991678fa24f7727937fb35", "2fef9dcff87ceeebfe0ae5ca2f9996ab5e8115c13c9949bb6bb0a1b1ed3e5287", "1528b8f9dfd01272762f10e1f22a7f7f4ecad05cb1f3ee59493b6c2eb6d91722")[index]
    private fun packetLogical(index: Int) = listOf("50af5a0589bc19f9154d840091cb5ccbc7d7250b2fca5a3a9a1d05967905c418", "7b26d34c7335cd0f03ffaf49d72bd5edff36a2e9df465ce3251acb9e8839a57c", "ef5008317a70136fa95b4f025cd060544af249db579551cc996694ec9fa35ab0", "dd78dbf8d17943f1d6f7645903f305e33f571f3efb70d4dff8a46e77dfb92d89")[index]
    private fun sha256(value: String) = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(value)
}
