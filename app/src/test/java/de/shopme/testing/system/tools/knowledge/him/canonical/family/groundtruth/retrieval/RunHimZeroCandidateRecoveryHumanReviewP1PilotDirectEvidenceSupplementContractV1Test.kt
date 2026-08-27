package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewValidationErrorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewValidationResultV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1Test {
    @Test
    fun contractIdentityAndStateAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_CONTRACT_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.VERSION)
        assertEquals("DIRECT_EVIDENCE_CONTEXT_ONLY", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.STATE)
    }

    @Test
    fun publicModelFieldsContainNoDecisionSemantics() {
        val forbidden = setOf(
            "decision", "reasonCodes", "reviewerRef", "reviewRound", "revision", "reviewerNote",
            "alternativeCanonicalProposal", "downstreamRoute", "validationState", "conflictState",
            "gold", "negativeSupervision", "approval", "publication", "training", "mutation",
        )
        val fields = listOf(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1::class.java,
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1::class.java,
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1::class.java,
        ).flatMap { it.declaredFields.map { field -> field.name } }
        assertTrue(fields.none { field -> forbidden.any { token -> field.equals(token, ignoreCase = true) } })
    }

    @Test
    fun frozenReviewUnitsAndIdsAreExact() {
        assertEquals(4, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.FROZEN_REVIEW_UNITS.size)
        assertEquals(
            listOf(
                "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e",
                "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7",
                "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70",
                "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50",
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.FROZEN_REVIEW_UNITS.map { it.reviewUnitId },
        )
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.FROZEN_REVIEW_UNITS.forEach { unit ->
            assertEquals(
                HimZeroCandidateRecoveryHumanReviewContractV1.reviewUnitId(unit.stableEntryId, unit.canonicalEntityId),
                unit.reviewUnitId,
            )
        }
    }

    @Test
    fun validSupplementHasExactlyTheRequiredEvidenceCardinality() {
        val supplement = fixture()
        assertValid(supplement)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCountersV1(
                reviewUnits = 4,
                sourceEvidenceProjections = 4,
                catalogTargetEvidenceRecords = 4,
                authorityTargetEvidenceRecords = 4,
                directEvidenceReferences = 12,
                supportingEvidenceReferences = 4,
                contradictingEvidenceReferences = 2,
                contextOnlyEvidenceReferences = 6,
                distinctSourceRecords = 4,
                distinctCanonicalTargets = 2,
            ),
            supplement.counters,
        )
    }

    @Test
    fun sourceAndTargetEvidenceAreAcceptedByHumanReviewContract() {
        val supplement = fixture()
        supplement.bundles.forEach { bundle ->
            val references = bundle.evidence.map { it.toHumanReference() }.sortedBy { it.evidenceReferenceId }
            val decision = HimZeroCandidateRecoveryHumanReviewDecisionRecordV1(
                HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
                bundle.reviewUnit,
                "reviewer-1",
                1,
                1,
                if (bundle.reviewUnit.canonicalEntityId == "ZuhV5V") HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
                listOf(
                    if (bundle.reviewUnit.canonicalEntityId == "ZuhV5V") HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED
                    else HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED,
                ),
                references,
            )
            assertTrue(decision.validate().valid)
        }
    }

    @Test
    fun artichokePositionsSupportAndBriePositionsContradict() {
        val supplement = fixture()
        assertTrue(supplement.bundles[0].evidence.first { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION)
        assertTrue(supplement.bundles[2].evidence.first { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD }.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION)
        assertTrue(supplement.bundles[1].evidence.first { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION)
        assertTrue(supplement.bundles[3].evidence.first { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD }.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY)
    }

    @Test
    fun reorderedInputIsCanonicalizedDeterministically() {
        val original = fixture()
        val reordered = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(
            binding(),
            original.bundles.reversed().map { it.copy(evidence = it.evidence.reversed()) },
        )
        assertEquals(original, reordered)
        assertEquals(original.supplementBindingDigest, reordered.supplementBindingDigest)
        assertEquals(original.supplementLogicalDigest, reordered.supplementLogicalDigest)
    }

    @Test
    fun evidenceAndUnitDigestsChangeWhenSemanticsChange() {
        val original = fixture()
        val source = original.bundles[0].evidence.first { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }
        val changed = source.copy(position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY)
        assertNotEquals(source.evidenceReferenceId, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.evidenceReferenceId(changed))
        val broken = original.copy(bundles = original.bundles.toMutableList().apply {
            this[0] = this[0].copy(evidence = this[0].evidence.map { if (it == source) changed else it })
        })
        assertReason(broken, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_REFERENCE_ID)
    }

    @Test
    fun allRequiredBindingLayersAreValidated() {
        assertReason(fixture().copy(contractId = "wrong"), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_CONTRACT_ID)
        assertReason(fixture().copy(version = "2"), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_CONTRACT_VERSION)
        assertReason(fixture().copy(state = "REVIEW_CONTEXT_ONLY"), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_STATE)
        assertReason(fixture().copy(binding = binding().copy(missionSelectionDigest = "0")), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_MISSION_BINDING)
        assertReason(fixture().copy(binding = binding().copy(packetBindingDigest = "0")), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_PACKET_BINDING)
        assertReason(fixture().copy(binding = binding().copy(corpusBindingDigest = "0")), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_CORPUS_BINDING)
        assertReason(fixture().copy(binding = binding().copy(packetImplementationHead = "A".repeat(40))), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_IMPLEMENTATION_HEAD)
        assertReason(fixture().copy(binding = binding().copy(corpusArtifact = corpusArtifact().copy(relativePath = "/absolute"))), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_ARTIFACT_BINDING)
    }

    @Test
    fun packetAndUnitBindingsAreFailClosed() {
        val original = fixture()
        assertReason(original.copy(bundles = original.bundles.toMutableList().apply { this[0] = this[0].copy(packetItemBindingDigest = "0") }), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_PACKET_ITEM_BINDING)
        assertReason(original.copy(bundles = original.bundles.toMutableList().apply { this[0] = this[0].copy(unitBindingDigest = "0") }), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.UNIT_BINDING_DIGEST_MISMATCH)
        assertReason(original.copy(bundles = original.bundles.toMutableList().apply { this[0] = this[0].copy(unitLogicalDigest = "0") }), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.UNIT_LOGICAL_DIGEST_MISMATCH)
        assertReason(original.copy(supplementBindingDigest = "0"), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.SUPPLEMENT_BINDING_DIGEST_MISMATCH)
        assertReason(original.copy(supplementLogicalDigest = "0"), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.SUPPLEMENT_LOGICAL_DIGEST_MISMATCH)
    }

    @Test
    fun fieldOrderingDuplicatesDigestsUnicodeAndLimitsAreBound() {
        val original = fixture()
        val source = original.bundles[0].evidence.first { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }
        val reversed = source.copy(fields = source.fields.reversed(), sourceProjection = source.sourceProjection?.copy(fields = source.fields.reversed()))
        assertReason(replaceFirstSource(original, reversed), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_FIELD_REFERENCE)
        val duplicateField = field("identity.productName", "duplicate")
        assertReason(replaceFirstSource(original, source.copy(fields = source.fields + duplicateField, sourceProjection = source.sourceProjection?.copy(fields = source.fields + duplicateField))), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.DUPLICATE_FIELD_REFERENCE)
        val badDigest = source.copy(fields = source.fields.map { it.copy(fullValueSha256 = "0".repeat(64)) }, sourceProjection = source.sourceProjection?.copy(fields = source.fields.map { it.copy(fullValueSha256 = "0".repeat(64)) }))
        assertReason(replaceFirstSource(original, badDigest), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.FIELD_VALUE_DIGEST_MISMATCH)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1.sha256("😀"), field("identity.productName", "😀").fullValueSha256)
        val oversized = field("identity.productName", "x".repeat(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.MAX_FIELD_BYTES + 1))
        assertReason(replaceFirstSource(original, source.copy(fields = listOf(oversized), sourceProjection = source.sourceProjection?.copy(fields = listOf(oversized)))), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.FIELD_VALUE_TOO_LARGE)
        val largeFields = (0..4).map { field("identity.name.$it", "x".repeat(16 * 1024)) }
        val largeSource = source.copy(fields = largeFields, sourceProjection = source.sourceProjection?.copy(fields = largeFields))
        val largeSupplement = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(
            binding(),
            original.bundles.mapIndexed { index, bundle ->
                if (index == 0) bundle.copy(evidence = bundle.evidence.map { if (it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION) largeSource else it }) else bundle
            },
        )
        assertReason(largeSupplement, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.PROJECTION_TOO_LARGE)
    }

    @Test
    fun evidenceKindDirectnessOriginAndReferenceRulesAreClosed() {
        val original = fixture()
        val source = original.bundles[0].evidence.first { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }
        assertReason(replaceFirstSource(original, source.copy(evidenceReferenceId = "0")), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_REFERENCE_ID)
        assertReason(replaceFirstSource(original, source.copy(kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD)), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_KIND)
        assertReason(replaceFirstSource(original, source.copy(directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.INDIRECT)), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INDIRECT_EVIDENCE_FORBIDDEN)
        assertReason(replaceFirstSource(original, source.copy(artifact = source.artifact.copy(relativePath = "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-review-corpus/v1/review-corpus.v1.json"))), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.FORBIDDEN_SOURCE_ARTIFACT)
        assertReason(replaceFirstSource(original, source.copy(sourceProjection = null)), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_REFERENCE_ID)
        val noIdentity = field("taxonomy.category", "food")
        val noIdentitySupplement = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(
            binding(),
            original.bundles.mapIndexed { index, bundle ->
                if (index == 0) bundle.copy(evidence = bundle.evidence.map { if (it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION) source.copy(fields = listOf(noIdentity), sourceProjection = source.sourceProjection?.copy(fields = listOf(noIdentity))) else it }) else bundle
            },
        )
        assertReason(noIdentitySupplement, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.MISSING_SEMANTIC_IDENTITY_FIELD)
        assertReason(replaceFirstSource(original, source.copy(recordReference = "not-a-source-reference")), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_REFERENCE_ID)
    }

    @Test
    fun cardinalityTargetsAndDuplicateEvidenceAreFailClosed() {
        val original = fixture()
        val source = original.bundles[0].evidence.first { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }
        assertReason(original.copy(bundles = original.bundles.drop(1)), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_REVIEW_UNIT)
        assertReason(original.copy(bundles = original.bundles.toMutableList().apply { this[0] = this[0].copy(reviewUnit = HimZeroCandidateRecoveryHumanReviewReviewUnitV1("A".repeat(64), "AbCd12")) }), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.UNEXPECTED_REVIEW_UNIT)
        val badPosition = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(
            binding(),
            original.bundles.mapIndexed { index, bundle ->
                if (index == 0) bundle.copy(evidence = bundle.evidence.map { if (it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION) source.copy(position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY) else it }) else bundle
            },
        )
        assertReason(badPosition, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_POSITION)
        val noSource = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(binding(), original.bundles.map { it.copy(evidence = it.evidence.filterNot { evidence -> evidence.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }) })
        assertReason(noSource, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.MISSING_SOURCE_EVIDENCE)
        val noCatalog = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(binding(), original.bundles.map { it.copy(evidence = it.evidence.filterNot { evidence -> evidence.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD }) })
        assertReason(noCatalog, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.MISSING_CATALOG_EVIDENCE)
        val duplicateEvidence = original.copy(bundles = original.bundles.toMutableList().apply { this[0] = this[0].copy(evidence = this[0].evidence + this[0].evidence.first().copy(evidenceReferenceId = this[0].evidence.first().evidenceReferenceId)) })
        assertReason(duplicateEvidence, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.DUPLICATE_EVIDENCE_REFERENCE)
        val tooManyAuthority = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(binding(), original.bundles.map { bundle ->
            bundle.copy(evidence = bundle.evidence + bundle.evidence.first { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD }.copy(fields = listOf(field("canonical.extra", "extra"))))
        })
        assertReason(tooManyAuthority, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.TOO_MANY_AUTHORITY_EVIDENCE)
    }

    @Test
    fun countersAndDecisionCompatibilityRemainTyped() {
        val original = fixture()
        assertReason(original.copy(counters = original.counters.copy(reviewUnits = 0)), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_COUNTERS)
        val indirectCorpus = HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1(
            "a".repeat(64),
            HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD,
            HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.INDIRECT,
            HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY,
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-review-corpus/v1/review-corpus.v1.json",
            "off:product:row:1:code:1",
            "b".repeat(64),
            null,
            listOf("identity.productName"),
        )
        val decision = HimZeroCandidateRecoveryHumanReviewDecisionRecordV1(
            HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
            HimZeroCandidateRecoveryHumanReviewContractV1.reviewUnitId("a".repeat(64), "ZuhV5V").let { HimZeroCandidateRecoveryHumanReviewReviewUnitV1("a".repeat(64), "ZuhV5V") },
            "reviewer-1",
            1,
            1,
            HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED),
            listOf(indirectCorpus),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.MISSING_REQUIRED_DIRECT_EVIDENCE, assertIs<HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid>(decision.validate()).error)
        assertFalse(original.bundles.any { it.evidence.any { evidence -> evidence.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD } })
    }

    private fun fixture(): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1 {
        val units = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.FROZEN_REVIEW_UNITS
        val records = listOf(
            "off:product:row:431650:code:4002239680509",
            "off:product:row:3272579:code:0061483010917",
            "off:product:row:1551407:code:4013200552046",
            "off:product:row:3322623:code:2026088009283",
        )
        val bundles = units.mapIndexed { index, unit ->
            val position = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
            val catalogPosition = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1(
                unit,
                packetBinding(index),
                packetLogical(index),
                listOf(
                    sourceCard(unit, records[index], position),
                    targetCard(unit, catalogPosition),
                    authorityCard(unit),
                ),
                "",
                "",
            )
        }
        return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(binding(), bundles)
    }

    private fun binding() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1(
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
        corpusArtifact(),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CORPUS_BINDING_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_IMPLEMENTATION_HEAD,
    )

    private fun corpusArtifact() = artifact(
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-review-corpus/v1/review-corpus.v1.json",
        2359985,
        "4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016",
        "3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02",
    )

    private fun sourceArtifact() = artifact("data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", 1, "1".repeat(64), "2".repeat(64))
    private fun catalogArtifact() = artifact("data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json", 1, "3".repeat(64), "4".repeat(64))
    private fun authorityArtifact() = artifact("data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json", 1, "5".repeat(64), "6".repeat(64))
    private fun artifact(path: String, size: Long, sha256: String, logicalDigest: String?) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1(path, size, sha256, logicalDigest)

    private fun sourceCard(unit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1, record: String, position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1) = card(
        unit.reviewUnitId,
        HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION,
        position,
        sourceArtifact(),
        record,
        listOf(field("identity.productName", if (unit.canonicalEntityId == "ZuhV5V") "Artischocken Herzen" else "Brie double crème"), field("identity.productNameEnglish", if (unit.canonicalEntityId == "ZuhV5V") "Artichoke hearts" else "Double cream brie")),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1(HimGroundTruthSource.OPEN_FOOD_FACTS, HimEvidenceRecordKind.OFF_PRODUCT, sourceArtifact(), record, listOf(field("identity.productName", if (unit.canonicalEntityId == "ZuhV5V") "Artischocken Herzen" else "Brie double crème"), field("identity.productNameEnglish", if (unit.canonicalEntityId == "ZuhV5V") "Artichoke hearts" else "Double cream brie")), sourceArtifact()),
    )

    private fun targetCard(unit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1, position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1) = card(
        unit.reviewUnitId,
        HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD,
        position,
        catalogArtifact(),
        "catalog:${unit.canonicalEntityId}",
        listOf(field("canonical.name", if (unit.canonicalEntityId == "ZuhV5V") "Artischocken" else "Crème double")),
        null,
    )

    private fun authorityCard(unit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1) = card(
        unit.reviewUnitId,
        HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD,
        HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY,
        authorityArtifact(),
        "authority:${unit.canonicalEntityId}",
        listOf(field("canonical.authorityName", if (unit.canonicalEntityId == "ZuhV5V") "Artischocken" else "Crème double")),
        null,
    )

    private fun card(unitId: String, kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1, position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1, artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1, record: String, fields: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1>, projection: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1?) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(unitId, "", kind, HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT, position, artifact, record, fields, projection)

    private fun field(reference: String, value: String) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1(reference, value, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1.sha256(value))

    private fun packetBinding(index: Int) = listOf(
        "a61bd239809b266d8a8ca36ef98b8b635966b4d0e6f6716468b77ef284829f29",
        "0fb192a227076e28204d841ad8573cc33e486fcfd8991678fa24f7727937fb35",
        "2fef9dcff87ceeebfe0ae5ca2f9996ab5e8115c13c9949bb6bb0a1b1ed3e5287",
        "1528b8f9dfd01272762f10e1f22a7f7f4ecad05cb1f3ee59493b6c2eb6d91722",
    )[index]

    private fun packetLogical(index: Int) = listOf(
        "50af5a0589bc19f9154d840091cb5ccbc7d7250b2fca5a3a9a1d05967905c418",
        "7b26d34c7335cd0f03ffaf49d72bd5edff36a2e9df465ce3251acb9e8839a57c",
        "ef5008317a70136fa95b4f025cd060544af249db579551cc996694ec9fa35ab0",
        "dd78dbf8d17943f1d6f7645903f305e33f571f3efb70d4dff8a46e77dfb92d89",
    )[index]

    private fun replaceFirstSource(original: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1, replacement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1) = original.copy(bundles = original.bundles.toMutableList().apply {
        this[0] = this[0].copy(evidence = this[0].evidence.map { if (it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION) replacement else it })
    })

    private fun HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1.toHumanReference() = HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1(
        evidenceReferenceId,
        kind,
        directness,
        position,
        artifact.relativePath,
        recordReference,
        artifact.sha256,
        artifact.logicalDigest,
        fields.map { it.fieldReference },
    )

    private fun assertValid(supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1) {
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Valid>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.validate(supplement),
        )
    }

    private fun assertReason(
        supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
        expected: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1,
    ) {
        val result = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Invalid>(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.validate(supplement),
        )
        assertEquals(expected, result.reason)
    }
}
