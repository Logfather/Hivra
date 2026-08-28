package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPrimaryBucketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContextLimitationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewGroupMembershipV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewPriorityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewSelectionReasonV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1Test {
    @Test fun contractIdentityIsFrozen() {
        assertEquals("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_PACKET_CONTRACT_V1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.CONTRACT_ID)
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VERSION)
        assertEquals("INDEPENDENT_VALIDATION_CONTEXT_ONLY", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.STATE)
        assertEquals("p1-artischocken-brie-decision-validation-v1", HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.PACKET_ID)
    }

    @Test fun completeFixtureIsValidAndContainsFourFrozenUnits() {
        val packet = packet()
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Valid>(validate(packet))
        assertEquals(4, packet.items.size)
        assertEquals(
            listOf("36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e", "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7", "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70", "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50"),
            packet.items.map { it.reviewUnitId },
        )
    }

    @Test fun originalDecisionsAndRejectReasonsAreBound() {
        val packet = packet()
        assertEquals(listOf(HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION, HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION, HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION), packet.items.map { it.originalDecision })
        assertEquals(2, packet.items.count { it.originalReasonCodes.contains(de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_SEMANTIC_MISMATCH) })
        assertTrue(packet.items.all { it.alternativeCanonicalProposal == null })
        assertTrue(packet.items.all { it.originalDecisionIdentity.length == 64 })
    }

    @Test fun evidenceFirstStructureHasThreeDirectRecordsPerUnit() {
        val packet = packet()
        assertTrue(packet.items.all { item -> item.directEvidence.size == 3 })
        assertTrue(packet.items.all { item -> item.directEvidence.all { it.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT } })
        assertTrue(packet.items.all { item -> item.directEvidence.map { it.kind }.toSet() == setOf(HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD) })
        assertEquals(12, packet.items.flatMap { it.directEvidence }.size)
        assertEquals(12, packet.items.flatMap { it.directEvidence }.map { it.evidenceReferenceId }.distinct().size)
    }

    @Test fun evidencePositionCountersAreFourTwoSix() {
        val counter = packet().counters
        assertEquals(4, counter.supportsAssociationEvidence)
        assertEquals(2, counter.contradictsAssociationEvidence)
        assertEquals(6, counter.contextOnlyEvidence)
        assertEquals(4, counter.sourceEvidenceRecords)
        assertEquals(4, counter.catalogEvidenceRecords)
        assertEquals(4, counter.authorityEvidenceRecords)
    }

    @Test fun countersAreDerivedAndNoValidationOrRouteIsSelected() {
        val packet = packet()
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.deriveCounters(packet.items), packet.counters)
        assertEquals(4, packet.counters.packetItems)
        assertEquals(4, packet.counters.distinctReviewUnits)
        assertEquals(2, packet.counters.originalConfirmDecisions)
        assertEquals(2, packet.counters.originalRejectDecisions)
        assertEquals(0, packet.counters.originalAbstainDecisions)
        assertEquals(0, packet.counters.originalEscalateDecisions)
        assertEquals(12, packet.counters.directEvidenceReferences)
        assertEquals(12, packet.counters.distinctEvidenceReferences)
        assertEquals(0, packet.counters.itemsWithAlternativeCanonicalProposal)
        assertEquals(0, packet.counters.itemsWithValidationAssessment)
        assertEquals(0, packet.counters.itemsWithDownstreamRoute)
    }

    @Test fun completeValidationTaxonomyAndLimitationsAreNeutral() {
        val packet = packet()
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_ASSESSMENTS, packet.allowedValidationAssessments)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_REASON_CODES, packet.allowedValidationReasonCodes)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.REQUIRED_CONTEXT_LIMITATIONS, packet.contextLimitations)
        assertTrue(packet.items.all { it.contextLimitations == packet.contextLimitations })
        assertTrue(packet.items.all { it.allowedValidationAssessments == packet.allowedValidationAssessments })
    }

    @Test fun inputBindingsAndFuturePathsAreFrozen() {
        val binding = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.frozenInputBinding()
        assertEquals("p1-artischocken-brie-logfather-r1-v1", binding.decisionBatchId)
        assertEquals("d2719de5cfa96e7616e7cd3bd535c9b2f7079a45", binding.contractBaselineHead)
        assertEquals("2a892ea6591967fdcbb439f63a3c1d3ef04d041ac10af4863c7c22d4ff140342", binding.decisionBatch.sha256)
        assertEquals("4c06a60e7e9f8548088f7d006667d55f36d1d92fb93818d6d95b2f54dd6c8a43", binding.reviewPacketJson.sha256)
        assertEquals("5a386e8adba953ac0bbdd8324b92de53cde867dfc577f87ba4a53b2b647062fb", binding.supplementJson.sha256)
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.JSON_OUTPUT_PATH.startsWith("build/"))
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.MARKDOWN_OUTPUT_PATH.endsWith("validation-packet.v1.md"))
    }

    @Test fun changingSemanticInputChangesDigests() {
        val original = packet()
        val changedItem = original.items[0].copy(canonicalLabel = "changed")
        val changed = packet(original.items.toMutableList().also { it[0] = changedItem })
        assertNotEquals(original.packetBindingDigest, changed.packetBindingDigest)
        assertNotEquals(original.packetLogicalDigest, changed.packetLogicalDigest)
    }

    @Test fun wrongBindingsFailClosedWithTypedReasons() {
        assertInvalid(packet().copy(inputBinding = packet().inputBinding.copy(decisionBatchId = "other")), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_DECISION_BATCH_BINDING)
        assertInvalid(packet().copy(inputBinding = packet().inputBinding.copy(reviewPacketBindingDigest = "f".repeat(64))), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_REVIEW_PACKET_BINDING)
        assertInvalid(packet().copy(inputBinding = packet().inputBinding.copy(supplementBindingDigest = "f".repeat(64))), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_SUPPLEMENT_BINDING)
        assertInvalid(packet().copy(inputBinding = packet().inputBinding.copy(corpusBindingDigest = "f".repeat(64))), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_CORPUS_BINDING)
        assertInvalid(packet().copy(inputBinding = packet().inputBinding.copy(contractBaselineHead = "f".repeat(40))), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_CONTRACT_BASELINE)
    }

    @Test fun wrongStructureAndDecisionFailClosed() {
        val original = packet()
        assertInvalid(original.copy(items = original.items.drop(1)), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_ITEM_COUNT)
        assertInvalid(original.copy(items = original.items.reversed()), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_ITEM_ORDER)
        assertInvalid(original.copy(items = original.items.toMutableList().also { it[0] = it[0].copy(reviewUnitId = it[1].reviewUnitId) }), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_ITEM_ORDER)
        val changed = original.items.toMutableList().also { it[1] = it[1].copy(originalDecision = HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION) }
        assertInvalid(packet(changed), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.ORIGINAL_DECISION_IDENTITY_MISMATCH)
    }

    @Test fun evidenceMutationIsRejected() {
        val original = packet()
        val item = original.items[0]
        assertInvalid(packet(original.items.toMutableList().also { it[0] = item.copy(directEvidence = item.directEvidence.drop(1)) }), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_EVIDENCE_COUNT)
        assertInvalid(packet(original.items.toMutableList().also { it[0] = item.copy(directEvidence = item.directEvidence.map { card -> card.copy(directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.INDIRECT) }) }), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_EVIDENCE_DIRECTNESS)
        assertInvalid(packet(original.items.toMutableList().also { it[0] = item.copy(directEvidence = item.directEvidence.map { card -> card.copy(position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY) }) }), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.INVALID_EVIDENCE_POSITION)
        val duplicate = item.directEvidence.toMutableList().also { it[1] = it[0] }
        assertInvalid(packet(original.items.toMutableList().also { it[0] = item.copy(directEvidence = duplicate) }), HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1.DUPLICATE_EVIDENCE_REFERENCE)
    }

    @Test fun noForbiddenIndependentValidatorOrPromotionFieldsExist() {
        val forbidden = setOf("selectedvalidation", "validatorreviewer", "validationtimestamp", "validationrecord", "validationbatch", "downstreamroute", "goldapproval", "trainingapproval", "authoritymutation", "adjudication", "promotion")
        val fields = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1::class.java.declaredFields.map { it.name.lowercase() } + HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1::class.java.declaredFields.map { it.name.lowercase() }
        assertTrue(fields.none { field -> forbidden.any { word -> word in field } })
        assertFalse(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1::class.java.declaredMethods.any { method -> method.name.lowercase().contains("read") || method.name.lowercase().contains("write") || method.name.lowercase().contains("open") || method.name.lowercase().contains("search") || method.name.lowercase().contains("fetch") || method.name.lowercase().contains("sqlite") })
    }

    @Test fun packetDoesNotDependOnRealFilesOrStores() {
        val methodNames = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1::class.java.declaredMethods.map { it.name.lowercase() }
        assertTrue(methodNames.none { it.contains("read") || it.contains("write") || it.contains("open") || it.contains("store") || it.contains("network") || it.contains("inference") })
        assertTrue(HimZeroCandidateRecoveryHumanReviewContractV1::class.java.declaredMethods.none { it.name.contains("File", ignoreCase = true) })
    }

    private fun validate(packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.validate(packet)

    private fun assertInvalid(packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1, reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketFailureReasonV1) {
        assertEquals(reason, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Invalid>(validate(packet)).reason)
    }

    private fun packet(items: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1> = baseItems()): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1 =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.create(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.frozenInputBinding(),
            items,
        )

    private fun baseItems() = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION.entries.mapIndexed { index, entry ->
        val selection = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.FROZEN_SELECTIONS[index]
        val source = listOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.AGRIBALYSE, HimGroundTruthSource.CIQUAL, HimGroundTruthSource.GLYCEMIC_INDEX)[index]
        val recordKind = listOf(HimEvidenceRecordKind.OFF_PRODUCT, HimEvidenceRecordKind.AGRIBALYSE_RECORD, HimEvidenceRecordKind.CIQUAL_FOOD, HimEvidenceRecordKind.GI_MEASUREMENT)[index]
        val evidence = selection.evidenceReferenceIds.sorted().mapIndexed { evidenceIndex, referenceId ->
            val kind = when (evidenceIndex) {
                0 -> HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION
                1 -> HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD
                else -> HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD
            }
            val position = when {
                evidenceIndex == 0 && (index == 0 || index == 2) -> HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION
                evidenceIndex == 1 && (index == 0 || index == 2) -> HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION
                evidenceIndex == 0 -> HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
                else -> HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY
            }
            val artifact = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1("fixture/$index/$evidenceIndex.json", 10, "${index}${evidenceIndex}".padEnd(64, 'a').take(64), "${index}${evidenceIndex}".padEnd(64, 'b').take(64))
            val field = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1("field", "value-$index-$evidenceIndex", sha("value-$index-$evidenceIndex"))
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(
                entry.reviewUnitId,
                referenceId,
                kind,
                HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                position,
                artifact,
                "record:$index:$evidenceIndex",
                listOf(field),
                if (kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION) HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1(source, recordKind, artifact, "record:$index:$evidenceIndex", listOf(field), artifact) else null,
            )
        }
        val context = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1(
            source,
            recordKind,
            "fixture:$index",
            listOf(sha("finding-$index")),
            listOf(HimZeroCandidateRecoveryReviewSelectionReasonV1.RECURRING_PRIMARY_VALUE_GROUP),
            listOf("primary-$index"),
            HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH,
            listOf(HimZeroCandidateRecoveryReviewGroupMembershipV1(entry.groupDisplayValue, HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET, 2, 2, listOf(entry.canonicalEntityId))),
        )
        val originalField = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1("source", "source-$index", sha("source-$index"), "source-$index", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL, "source-$index".length)
        val targetBase = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1(entry.canonicalEntityId, entry.expectedDisplayLabel, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED, "catalog:${entry.canonicalEntityId}", "authority:${entry.canonicalEntityId}", emptyList(), emptyList(), "")
        val rationale = "Bound original rationale for review unit ${index + 1}."
        val itemBase = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1(
            entry.reviewUnitId,
            entry.stableEntryId,
            entry.canonicalEntityId,
            entry.expectedDisplayLabel,
            context,
            listOf(originalField),
            "",
            selection.decision,
            selection.reasonCodes,
            selection.evidenceReferenceIds.sorted(),
            rationale,
            sha(rationale),
            null,
            targetBase.copy(targetContextDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(targetBase)),
            evidence,
            emptyList(),
            emptyList(),
            emptyList(),
            "",
            "",
        )
        itemBase.copy(originalDecisionIdentity = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.originalDecisionIdentity(itemBase))
    }

    private fun sha(value: String) = java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
