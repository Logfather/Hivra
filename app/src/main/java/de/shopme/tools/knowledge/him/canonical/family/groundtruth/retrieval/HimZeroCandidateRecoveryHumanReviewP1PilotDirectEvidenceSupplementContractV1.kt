package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure contract for a future direct-evidence supplement to the frozen P1 review packet.
 * This contract deliberately contains no persistence, source access, decision, or training behavior.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "DIRECT_EVIDENCE_CONTEXT_ONLY"
    const val EVIDENCE_REFERENCE_ID_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_REFERENCE_V1"
    const val UNIT_BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_UNIT_BINDING_V1"
    const val UNIT_LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_UNIT_LOGICAL_V1"
    const val SUPPLEMENT_BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_BINDING_V1"
    const val SUPPLEMENT_LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_LOGICAL_V1"
    const val MAX_FIELD_BYTES = 16 * 1024
    const val MAX_PROJECTION_BYTES = 64 * 1024

    const val PACKET_INPUT_BINDING_DIGEST =
        "4426f0b5be456ab92e32f7cd9b27378d89b0b3fdc7c2ed46b21415830afc475d"
    const val PACKET_BINDING_DIGEST =
        "beb465178ecde447e3995672ea77fd16b5a020b6eabfc4e310ee0ae8f8771793"
    const val PACKET_LOGICAL_DIGEST =
        "311f4925716045d47a02af3652dea66393f7cb713dbfe7239705ce4684acce6e"
    const val CORPUS_SHA256 =
        "4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016"
    const val CORPUS_LOGICAL_DIGEST =
        "3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02"
    const val CORPUS_BINDING_DIGEST =
        "b1691dbf87eed143d23ea44275ec5d96bf9483e4675469af5d282d3858288e81"
    const val PACKET_IMPLEMENTATION_HEAD = "a1f21b960cf287939a6bd1b45e8bbf1a1be47e8b"

    val FROZEN_REVIEW_UNITS = listOf(
        HimZeroCandidateRecoveryHumanReviewReviewUnitV1(
            "a98f67c7aa8af729e27d402585df4c78d2a1a9c3f85e636dd7d257cba1810ebd",
            "ZuhV5V",
        ),
        HimZeroCandidateRecoveryHumanReviewReviewUnitV1(
            "ec4d7ccf39c1b9dbe184a0af90190cbc5d6a9e96ce3e3af121f5f59258eb5e13",
            "rVnyq7",
        ),
        HimZeroCandidateRecoveryHumanReviewReviewUnitV1(
            "6db7a77b0a3ff2471b8001b1644bc7a957efa52369df722899298c87d286ed71",
            "ZuhV5V",
        ),
        HimZeroCandidateRecoveryHumanReviewReviewUnitV1(
            "f4957e490b1714a1e48ca5d43ae762564603ac712842b3c2714b9f36bf2c41df",
            "rVnyq7",
        ),
    )

    private val SHA256 = Regex("[0-9a-f]{64}")
    private val HEAD = Regex("[0-9a-f]{40}")
    private val ENTITY = Regex("[0-9A-Za-z]{6}")

    fun create(
        binding: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1,
        bundles: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1>,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1 {
        val normalizedBundles = bundles.sortedBy { it.reviewUnit.reviewUnitId }.map { bundle ->
            val normalizedEvidence = bundle.evidence.map { evidence ->
                val normalized = evidence.copy(fields = evidence.fields.sortedBy { it.fieldReference })
                normalized.copy(evidenceReferenceId = evidenceReferenceId(normalized))
            }.sortedBy { it.evidenceReferenceId }
            val unsigned = bundle.copy(
                evidence = normalizedEvidence,
                unitBindingDigest = "",
                unitLogicalDigest = "",
            )
            val bound = unsigned.copy(unitBindingDigest = unitBindingDigest(unsigned))
            bound.copy(unitLogicalDigest = unitLogicalDigest(bound))
        }
        val unsigned = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            binding = binding,
            bundles = normalizedBundles,
            counters = deriveCounters(normalizedBundles),
            supplementBindingDigest = "",
            supplementLogicalDigest = "",
        )
        val bound = unsigned.copy(supplementBindingDigest = supplementBindingDigest(unsigned))
        return bound.copy(supplementLogicalDigest = supplementLogicalDigest(bound))
    }

    fun deriveCounters(
        bundles: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1>,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCountersV1(
        reviewUnits = bundles.size,
        sourceEvidenceProjections = bundles.sumOf { it.evidence.count { evidence -> evidence.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION } },
        catalogTargetEvidenceRecords = bundles.sumOf { it.evidence.count { evidence -> evidence.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD } },
        authorityTargetEvidenceRecords = bundles.sumOf { it.evidence.count { evidence -> evidence.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD } },
        directEvidenceReferences = bundles.sumOf { it.evidence.size },
        supportingEvidenceReferences = bundles.sumOf { bundle -> bundle.evidence.count { it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION } },
        contradictingEvidenceReferences = bundles.sumOf { bundle -> bundle.evidence.count { it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION } },
        contextOnlyEvidenceReferences = bundles.sumOf { bundle -> bundle.evidence.count { it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY } },
        distinctSourceRecords = bundles.flatMap { bundle -> bundle.evidence.filter { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }.map { it.recordReference } }.distinct().size,
        distinctCanonicalTargets = bundles.map { it.reviewUnit.canonicalEntityId }.distinct().size,
    )

    fun validate(
        supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1 {
        if (supplement.contractId != CONTRACT_ID) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_CONTRACT_ID)
        if (supplement.version != VERSION) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_CONTRACT_VERSION)
        if (supplement.state != STATE) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_STATE)
        bindingFailure(supplement.binding)?.let { return invalid(it) }
        if (supplement.bundles.size != FROZEN_REVIEW_UNITS.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_REVIEW_UNIT)
        if (supplement.bundles.map { it.reviewUnit.reviewUnitId } != supplement.bundles.map { it.reviewUnit.reviewUnitId }.sorted()) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_REVIEW_UNIT)
        if (supplement.bundles.map { it.reviewUnit.reviewUnitId } != FROZEN_REVIEW_UNITS.map { it.reviewUnitId }) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.UNEXPECTED_REVIEW_UNIT)
        supplement.bundles.forEachIndexed { index, bundle ->
            val expected = FROZEN_REVIEW_UNITS[index]
            if (bundle.reviewUnit != expected) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_REVIEW_UNIT)
            if (bundle.packetItemBindingDigest != PACKET_ITEM_BINDING_DIGESTS[index] || bundle.packetItemLogicalDigest != PACKET_ITEM_LOGICAL_DIGESTS[index]) {
                return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_PACKET_ITEM_BINDING)
            }
            if (bundle.evidence.map { it.evidenceReferenceId }.distinct().size != bundle.evidence.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.DUPLICATE_EVIDENCE_REFERENCE)
            if (bundle.evidence != bundle.evidence.sortedBy { it.evidenceReferenceId }) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_REFERENCE_ID)
            bundle.evidence.forEach { evidence ->
                evidenceFailure(evidence, bundle.reviewUnit)?.let { return invalid(it) }
            }
            val source = bundle.evidence.filter { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }
            val catalog = bundle.evidence.filter { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD }
            val authority = bundle.evidence.filter { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD }
            if (source.isEmpty()) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.MISSING_SOURCE_EVIDENCE)
            if (source.size > 1) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.DUPLICATE_SOURCE_EVIDENCE)
            if (catalog.isEmpty()) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.MISSING_CATALOG_EVIDENCE)
            if (catalog.size > 1) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.DUPLICATE_CATALOG_EVIDENCE)
            if (authority.size > 1) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.TOO_MANY_AUTHORITY_EVIDENCE)
            val expectedSourcePosition = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
            val expectedCatalogPosition = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY
            if (source.single().position != expectedSourcePosition || catalog.single().position != expectedCatalogPosition) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_POSITION)
            if ((index == 1 || index == 3) && bundle.evidence.count { it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION } < 1) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_POSITION)
            if (bundle.unitBindingDigest != unitBindingDigest(bundle)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.UNIT_BINDING_DIGEST_MISMATCH)
            if (bundle.unitLogicalDigest != unitLogicalDigest(bundle)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.UNIT_LOGICAL_DIGEST_MISMATCH)
        }
        if (supplement.counters != deriveCounters(supplement.bundles)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_COUNTERS)
        if (supplement.supplementBindingDigest != supplementBindingDigest(supplement)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.SUPPLEMENT_BINDING_DIGEST_MISMATCH)
        if (supplement.supplementLogicalDigest != supplementLogicalDigest(supplement)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.SUPPLEMENT_LOGICAL_DIGEST_MISMATCH)
        return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Valid
    }

    fun evidenceReferenceId(evidence: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1): String =
        sha256(buildString {
            appendLine(EVIDENCE_REFERENCE_ID_DOMAIN)
            appendLine("contractId=$CONTRACT_ID")
            appendLine("version=$VERSION")
            appendLine("reviewUnitId=${evidence.reviewUnitId}")
            appendLine("kind=${evidence.kind.name}")
            appendLine("directness=${evidence.directness.name}")
            appendLine("position=${evidence.position.name}")
            appendArtifact(evidence.artifact)
            appendLine("recordReference=${evidence.recordReference}")
            evidence.fields.sortedBy { it.fieldReference }.forEach { field ->
                appendLine("fieldReference=${field.fieldReference}")
                appendLine("fieldValueSha256=${field.fullValueSha256}")
            }
            evidence.sourceProjection?.let { projection ->
                appendLine("projectionSource=${projection.source.name}")
                appendLine("projectionRecordKind=${projection.recordKind.name}")
                appendLine("projectionRecordReference=${projection.recordReference}")
                appendArtifact(projection.sourceOriginArtifact)
            }
        })

    fun unitBindingDigest(bundle: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1): String =
        sha256(buildString {
            appendLine(UNIT_BINDING_DIGEST_DOMAIN)
            appendLine("reviewUnit=${bundle.reviewUnit}")
            appendLine("packetItemBindingDigest=${bundle.packetItemBindingDigest}")
            appendLine("packetItemLogicalDigest=${bundle.packetItemLogicalDigest}")
            bundle.evidence.forEach { appendLine("evidenceReferenceId=${it.evidenceReferenceId}") }
        })

    fun unitLogicalDigest(bundle: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1): String =
        sha256(buildString {
            appendLine(UNIT_LOGICAL_DIGEST_DOMAIN)
            appendLine("unitBindingDigest=${bundle.unitBindingDigest}")
            bundle.evidence.forEach { evidence ->
                appendLine("evidenceReferenceId=${evidence.evidenceReferenceId}")
                appendLine("kind=${evidence.kind.name}")
                appendLine("directness=${evidence.directness.name}")
                appendLine("position=${evidence.position.name}")
                appendArtifact(evidence.artifact)
                appendLine("recordReference=${evidence.recordReference}")
                evidence.fields.forEach { appendLine("field=$it") }
            }
        })

    fun supplementBindingDigest(supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1): String =
        sha256(buildString {
            appendLine(SUPPLEMENT_BINDING_DIGEST_DOMAIN)
            appendLine("contractId=${supplement.contractId}")
            appendLine("version=${supplement.version}")
            appendLine("state=${supplement.state}")
            appendLine("binding=${supplement.binding}")
            supplement.bundles.forEach { appendLine("unitBindingDigest=${it.unitBindingDigest}") }
        })

    fun supplementLogicalDigest(supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1): String =
        sha256(buildString {
            appendLine(SUPPLEMENT_LOGICAL_DIGEST_DOMAIN)
            appendLine("supplementBindingDigest=${supplement.supplementBindingDigest}")
            supplement.bundles.forEach { appendLine("unitLogicalDigest=${it.unitLogicalDigest}") }
            appendLine("counters=${supplement.counters}")
        })

    private fun bindingFailure(binding: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1? {
        if (binding.missionContractId != HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.CONTRACT_ID || binding.missionVersion != HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.VERSION || binding.missionId != HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.MISSION_ID || binding.scopeId != HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.SCOPE_ID) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_MISSION_BINDING
        if (binding.missionSelectionDigest != HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_SELECTION_DIGEST) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_MISSION_BINDING
        if (binding.packetContractId != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.CONTRACT_ID || binding.packetVersion != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.VERSION || binding.packetInputBindingDigest != PACKET_INPUT_BINDING_DIGEST || binding.packetBindingDigest != PACKET_BINDING_DIGEST || binding.packetLogicalDigest != PACKET_LOGICAL_DIGEST) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_PACKET_BINDING
        if (binding.corpusArtifact.sha256 != CORPUS_SHA256 || binding.corpusArtifact.logicalDigest != CORPUS_LOGICAL_DIGEST || binding.corpusBindingDigest != CORPUS_BINDING_DIGEST) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_CORPUS_BINDING
        if (!HEAD.matches(binding.packetImplementationHead)) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_IMPLEMENTATION_HEAD
        if (binding.corpusArtifact.validate() != null) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_ARTIFACT_BINDING
        return null
    }

    private fun evidenceFailure(
        evidence: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1,
        reviewUnit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1? {
        if (evidence.reviewUnitId != reviewUnit.reviewUnitId) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_REFERENCE_ID
        if (evidence.directness != HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INDIRECT_EVIDENCE_FORBIDDEN
        if (evidence.kind !in setOf(HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD)) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_KIND
        if (evidence.artifact.validate() != null) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_ARTIFACT_BINDING
        if (isForbiddenArtifact(evidence.artifact.relativePath)) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.FORBIDDEN_SOURCE_ARTIFACT
        if (evidence.fields.isEmpty() || evidence.fields != evidence.fields.distinctBy { it.fieldReference }.sortedBy { it.fieldReference }) return if (evidence.fields.map { it.fieldReference }.distinct().size != evidence.fields.size) HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.DUPLICATE_FIELD_REFERENCE else HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_FIELD_REFERENCE
        evidence.fields.forEach { field -> field.validate()?.let { return it } }
        if (evidence.evidenceReferenceId != evidenceReferenceId(evidence)) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_REFERENCE_ID
        val expectedRecord = when (evidence.kind) {
            HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD -> "catalog:${reviewUnit.canonicalEntityId}"
            HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD -> "authority:${reviewUnit.canonicalEntityId}"
            HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION -> evidence.recordReference
            else -> return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_KIND
        }
        if (evidence.recordReference != expectedRecord || evidence.recordReference.isBlank()) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_TARGET_RECORD_REFERENCE
        when (evidence.kind) {
            HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION -> {
                val projection = evidence.sourceProjection ?: return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_ARTIFACT_BINDING
                if (projection.recordKind.source != projection.source || projection.recordReference != evidence.recordReference || projection.artifact != evidence.artifact || projection.fields != evidence.fields) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_ARTIFACT_BINDING
                if (isForbiddenArtifact(projection.sourceOriginArtifact.relativePath)) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.FORBIDDEN_SOURCE_ARTIFACT
                if (projection.sourceOriginArtifact.validate() != null) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_ARTIFACT_BINDING
                if (!isSourceRecordReferenceValid(projection.source, projection.recordKind, projection.recordReference)) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_SOURCE_RECORD_REFERENCE
                if (projectionSize(projection) > MAX_PROJECTION_BYTES) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.PROJECTION_TOO_LARGE
                if (projection.fields.none { isSemanticIdentityField(it.fieldReference) }) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.MISSING_SEMANTIC_IDENTITY_FIELD
            }
            else -> if (evidence.sourceProjection != null) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_ARTIFACT_BINDING
        }
        val humanReference = HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1(
            evidence.evidenceReferenceId,
            evidence.kind,
            evidence.directness,
            evidence.position,
            evidence.artifact.relativePath,
            evidence.recordReference,
            evidence.artifact.sha256,
            evidence.artifact.logicalDigest,
            evidence.fields.map { it.fieldReference },
        )
        if (!humanReference.validate().valid) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_EVIDENCE_REFERENCE_ID
        return null
    }

    private fun isSemanticIdentityField(reference: String): Boolean =
        listOf("name", "identity", "product", "food", "label", "canonical").any { it in reference.lowercase() }

    private fun isSourceRecordReferenceValid(
        source: HimGroundTruthSource,
        recordKind: HimEvidenceRecordKind,
        reference: String,
    ): Boolean = when (source) {
        HimGroundTruthSource.OPEN_FOOD_FACTS -> recordKind == HimEvidenceRecordKind.OFF_PRODUCT && reference.matches(Regex("off:product:row:[0-9]+:code:[0-9]+"))
        HimGroundTruthSource.AGRIBALYSE -> recordKind == HimEvidenceRecordKind.AGRIBALYSE_RECORD && reference.matches(Regex("agribalyse:row:[0-9]+:agb:[0-9]+"))
        HimGroundTruthSource.CIQUAL -> recordKind == HimEvidenceRecordKind.CIQUAL_FOOD && reference.matches(Regex("ciqual:food:[0-9]+"))
        HimGroundTruthSource.GLYCEMIC_INDEX -> recordKind == HimEvidenceRecordKind.GI_MEASUREMENT && reference.matches(Regex("gi:measurement:[0-9]+"))
    }

    private fun projectionSize(projection: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1): Int =
        projection.recordReference.toByteArray(StandardCharsets.UTF_8).size +
            projection.fields.sumOf { field ->
                field.fieldReference.toByteArray(StandardCharsets.UTF_8).size +
                    field.fullValue.toByteArray(StandardCharsets.UTF_8).size +
                    field.fullValueSha256.length
            }

    private fun isForbiddenArtifact(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".md") || listOf("review-corpus", "review-packet", "decision-batch", "decision_batch").any { it in lower }
    }

    private fun StringBuilder.appendArtifact(artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1) {
        appendLine("artifactPath=${artifact.relativePath}")
            .appendLine("artifactByteSize=${artifact.byteSize}")
            .appendLine("artifactSha256=${artifact.sha256}")
            .appendLine("artifactLogicalDigest=${artifact.logicalDigest ?: ""}")
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun invalid(reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Invalid(reason)

    private val PACKET_ITEM_BINDING_DIGESTS = listOf(
        "a61bd239809b266d8a8ca36ef98b8b635966b4d0e6f6716468b77ef284829f29",
        "0fb192a227076e28204d841ad8573cc33e486fcfd8991678fa24f7727937fb35",
        "2fef9dcff87ceeebfe0ae5ca2f9996ab5e8115c13c9949bb6bb0a1b1ed3e5287",
        "1528b8f9dfd01272762f10e1f22a7f7f4ecad05cb1f3ee59493b6c2eb6d91722",
    )
    private val PACKET_ITEM_LOGICAL_DIGESTS = listOf(
        "50af5a0589bc19f9154d840091cb5ccbc7d7250b2fca5a3a9a1d05967905c418",
        "7b26d34c7335cd0f03ffaf49d72bd5edff36a2e9df465ce3251acb9e8839a57c",
        "ef5008317a70136fa95b4f025cd060544af249db579551cc996694ec9fa35ab0",
        "dd78dbf8d17943f1d6f7645903f305e33f571f3efb70d4dff8a46e77dfb92d89",
    )
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1(
    val relativePath: String,
    val byteSize: Long,
    val sha256: String,
    val logicalDigest: String?,
) {
    fun validate(): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1? {
        if (relativePath.isBlank() || relativePath.startsWith('/') || relativePath.contains('\\') || relativePath.split('/').any { it.isBlank() || it == "." || it == ".." }) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_ARTIFACT_BINDING
        if (byteSize < 0 || !sha256.matches(Regex("[0-9a-f]{64}")) || (logicalDigest != null && !logicalDigest.matches(Regex("[0-9a-f]{64}")))) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_ARTIFACT_BINDING
        return null
    }
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1(
    val fieldReference: String,
    val fullValue: String,
    val fullValueSha256: String,
) {
    fun validate(): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1? {
        if (fieldReference.isBlank() || fieldReference.contains('\u0000')) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_FIELD_REFERENCE
        if (fullValue.contains('\u0000') || hasUnpairedSurrogate(fullValue)) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.INVALID_FIELD_REFERENCE
        if (fullValue.toByteArray(StandardCharsets.UTF_8).size > HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.MAX_FIELD_BYTES) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.FIELD_VALUE_TOO_LARGE
        if (!fullValueSha256.matches(Regex("[0-9a-f]{64}")) || fullValueSha256 != sha256(fullValue)) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.FIELD_VALUE_DIGEST_MISMATCH
        return null
    }

    companion object {
        fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1(
    val source: HimGroundTruthSource,
    val recordKind: HimEvidenceRecordKind,
    val artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
    val recordReference: String,
    val fields: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1>,
    val sourceOriginArtifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(
    val reviewUnitId: String,
    val evidenceReferenceId: String,
    val kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
    val directness: HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1,
    val position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
    val artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
    val recordReference: String,
    val fields: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1>,
    val sourceProjection: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1?,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1(
    val reviewUnit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1,
    val packetItemBindingDigest: String,
    val packetItemLogicalDigest: String,
    val evidence: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1>,
    val unitBindingDigest: String,
    val unitLogicalDigest: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1(
    val missionContractId: String,
    val missionVersion: String,
    val missionId: String,
    val scopeId: String,
    val missionSelectionDigest: String,
    val packetContractId: String,
    val packetVersion: String,
    val packetInputBindingDigest: String,
    val packetBindingDigest: String,
    val packetLogicalDigest: String,
    val corpusArtifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
    val corpusBindingDigest: String,
    val packetImplementationHead: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCountersV1(
    val reviewUnits: Int,
    val sourceEvidenceProjections: Int,
    val catalogTargetEvidenceRecords: Int,
    val authorityTargetEvidenceRecords: Int,
    val directEvidenceReferences: Int,
    val supportingEvidenceReferences: Int,
    val contradictingEvidenceReferences: Int,
    val contextOnlyEvidenceReferences: Int,
    val distinctSourceRecords: Int,
    val distinctCanonicalTargets: Int,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1(
    val contractId: String,
    val version: String,
    val state: String,
    val binding: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1,
    val bundles: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1>,
    val counters: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCountersV1,
    val supplementBindingDigest: String,
    val supplementLogicalDigest: String,
)

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1 {
    INVALID_CONTRACT_ID,
    INVALID_CONTRACT_VERSION,
    INVALID_STATE,
    INVALID_MISSION_BINDING,
    INVALID_PACKET_BINDING,
    INVALID_CORPUS_BINDING,
    INVALID_IMPLEMENTATION_HEAD,
    INVALID_REVIEW_UNIT,
    UNEXPECTED_REVIEW_UNIT,
    INVALID_PACKET_ITEM_BINDING,
    INVALID_SOURCE_RECORD_REFERENCE,
    INVALID_ARTIFACT_BINDING,
    FORBIDDEN_SOURCE_ARTIFACT,
    INVALID_FIELD_REFERENCE,
    DUPLICATE_FIELD_REFERENCE,
    FIELD_VALUE_DIGEST_MISMATCH,
    FIELD_VALUE_TOO_LARGE,
    PROJECTION_TOO_LARGE,
    MISSING_SEMANTIC_IDENTITY_FIELD,
    INVALID_EVIDENCE_REFERENCE_ID,
    INVALID_EVIDENCE_KIND,
    INDIRECT_EVIDENCE_FORBIDDEN,
    MISSING_SOURCE_EVIDENCE,
    DUPLICATE_SOURCE_EVIDENCE,
    MISSING_CATALOG_EVIDENCE,
    DUPLICATE_CATALOG_EVIDENCE,
    TOO_MANY_AUTHORITY_EVIDENCE,
    INVALID_TARGET_RECORD_REFERENCE,
    INVALID_EVIDENCE_POSITION,
    DUPLICATE_EVIDENCE_REFERENCE,
    INVALID_COUNTERS,
    UNIT_BINDING_DIGEST_MISMATCH,
    UNIT_LOGICAL_DIGEST_MISMATCH,
    SUPPLEMENT_BINDING_DIGEST_MISMATCH,
    SUPPLEMENT_LOGICAL_DIGEST_MISMATCH,
    FORBIDDEN_DECISION_SEMANTICS,
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1 {
    val valid: Boolean
    data object Valid : HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1 { override val valid = true }
    data class Invalid(val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1) : HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1 { override val valid = false }
}

private fun hasUnpairedSurrogate(value: String): Boolean {
    var index = 0
    while (index < value.length) {
        val character = value[index]
        if (Character.isHighSurrogate(character)) {
            if (index + 1 >= value.length || !Character.isLowSurrogate(value[index + 1])) return true
            index += 2
        } else {
            if (Character.isLowSurrogate(character)) return true
            index += 1
        }
    }
    return false
}
