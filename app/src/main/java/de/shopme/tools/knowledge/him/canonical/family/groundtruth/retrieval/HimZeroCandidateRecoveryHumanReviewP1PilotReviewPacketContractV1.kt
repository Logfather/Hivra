package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.security.MessageDigest

/** Pure, context-only packet contract for the committed P1 review mission. */
object HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1 {
    const val CONTRACT_ID = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_REVIEW_PACKET_CONTRACT_V1"
    const val VERSION = "1"
    const val PACKET_BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_REVIEW_PACKET_BINDING_V1"
    const val ITEM_BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_REVIEW_PACKET_ITEM_BINDING_V1"
    const val ITEM_LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_REVIEW_PACKET_ITEM_LOGICAL_V1"
    const val PACKET_LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_REVIEW_PACKET_LOGICAL_V1"
    const val TARGET_CONTEXT_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_REVIEW_PACKET_TARGET_CONTEXT_V1"
    const val MAX_HUMAN_DISPLAY_LENGTH = 240

    const val HUMAN_PROJECTION_HEADER =
        "REVIEW CONTEXT ONLY\nNO DECISION RECORDED\nNO GOLD, TRAINING OR AUTHORITY EFFECT"
    val HUMAN_PROJECTION_SECTIONS = listOf(
        "REVIEW UNIT",
        "SOURCE CONTEXT",
        "MATERIALIZED ORIGINAL FIELDS",
        "AUDIT TARGET UNDER REVIEW",
        "TARGET CONTEXT",
        "EVIDENCE SCOPE",
        "CONTEXT LIMITATIONS",
        "BINDINGS",
    )

    val SOURCE_ORDER = HimZeroCandidateRecoveryReviewCorpusContractV1.SOURCE_ORDER
    val RECORD_KIND_ORDER = HimZeroCandidateRecoveryReviewCorpusContractV1.RECORD_KIND_ORDER

    fun missionBinding(mission: HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketMissionBindingV1(
            mission.contractId,
            mission.version,
            mission.missionId,
            mission.scopeId,
            mission.priority,
            mission.reviewRound,
            mission.maxNewDecisionRecords,
            mission.selectionDigest,
            mission.expectedSelectedGroups,
            mission.expectedSelectedCorpusEntries,
            mission.expectedSelectedReviewUnits,
            mission.expectedDistinctCanonicalTargets,
            mission.expectedMultiTargetEntries,
            mission.entries.map { it.reviewUnitId },
        )

    fun create(
        missionBinding: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketMissionBindingV1,
        humanReviewInputBinding: HimZeroCandidateRecoveryHumanReviewInputBindingV1,
        corpusFileBinding: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
        corpusLogicalDigest: String,
        corpusBindingDigest: String,
        packetImplementationHead: String,
        items: List<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1>,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1 {
        val normalizedItems = items
            .sortedWith(compareBy({ it.reviewUnitId }, { it.stableEntryId }, { it.canonicalEntityId }))
            .map { item ->
                val bound = item.copy(itemBindingDigest = itemBindingDigest(item))
                bound.copy(itemLogicalDigest = itemLogicalDigest(bound))
            }
        val counters = deriveCounters(normalizedItems)
        val packet = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1(
            CONTRACT_ID,
            VERSION,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1.REVIEW_CONTEXT_ONLY,
            missionBinding,
            humanReviewInputBinding,
            corpusFileBinding,
            corpusLogicalDigest,
            corpusBindingDigest,
            packetImplementationHead,
            normalizedItems,
            counters,
            "",
            "",
        )
        val bound = packet.copy(packetBindingDigest = packetBindingDigest(packet))
        return bound.copy(packetLogicalDigest = packetLogicalDigest(bound))
    }

    fun deriveCounters(
        items: List<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1>,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketCountersV1(
        packetItems = items.size,
        uniqueStableEntries = items.map { it.stableEntryId }.distinct().size,
        uniqueReviewUnits = items.map { it.reviewUnitId }.distinct().size,
        distinctCanonicalTargets = items.map { it.canonicalEntityId }.distinct().size,
        sourceCounts = SOURCE_ORDER.map { source ->
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceCountV1(source, items.count { it.sourceContext.source == source })
        },
        recordKindCounts = RECORD_KIND_ORDER.map { kind ->
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRecordKindCountV1(kind, items.count { it.sourceContext.recordKind == kind })
        },
        materializedCorpusOnlyItems = items.count { it.evidenceScope.coverage == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY },
        sourceProjectionIncludedItems = items.count { it.evidenceScope.coverage == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.BOUND_SOURCE_PROJECTION_INCLUDED },
        itemsWithIdentityTerms = items.count { it.targetContext.identityTerms.isNotEmpty() },
        itemsWithoutIdentityTerms = items.count { it.targetContext.identityTerms.isEmpty() },
        itemsWithAliasTerms = items.count { it.targetContext.aliasTerms.isNotEmpty() },
        itemsWithoutAliasTerms = items.count { it.targetContext.aliasTerms.isEmpty() },
        fullDisplayFields = items.sumOf { item -> item.originalFields.count { it.displayState == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL } },
        truncatedDisplayFields = items.sumOf { item -> item.originalFields.count { it.displayState == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.TRUNCATED_FOR_HUMAN_PROJECTION } },
        emptySourceFields = items.sumOf { item -> item.originalFields.count { it.displayState == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.EMPTY_SOURCE_VALUE } },
    )

    fun targetContextDigest(target: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1): String =
        sha256(buildString {
            appendLine(TARGET_CONTEXT_DIGEST_DOMAIN)
            appendLine("canonicalEntityId=${target.canonicalEntityId}")
            appendLine("expectedDisplayLabel=${target.expectedDisplayLabel}")
            appendLine("registryResolution=${target.registryResolution.name}")
            appendLine("authorityResolution=${target.authorityResolution.name}")
            appendLine("catalogRecordReference=${target.catalogRecordReference}")
            appendLine("authorityRecordReference=${target.authorityRecordReference}")
            target.identityTerms.forEach { appendLine("identityTerm=$it") }
            target.aliasTerms.forEach { appendLine("aliasTerm=$it") }
        })

    fun itemBindingDigest(item: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1): String =
        sha256(buildString {
            appendLine(ITEM_BINDING_DIGEST_DOMAIN)
            appendLine("contractId=$CONTRACT_ID")
            appendLine("version=$VERSION")
            appendLine("stableEntryId=${item.stableEntryId}")
            appendLine("reviewUnitId=${item.reviewUnitId}")
            appendLine("canonicalEntityId=${item.canonicalEntityId}")
            appendLine("groupOrdinal=${item.groupOrdinal}")
            appendLine("source=${item.sourceContext.source.name}")
            appendLine("recordKind=${item.sourceContext.recordKind.name}")
            appendLine("evidenceReference=${item.sourceContext.evidenceReference}")
        })

    fun itemLogicalDigest(item: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1): String =
        sha256(serializeItem(item, ITEM_LOGICAL_DIGEST_DOMAIN, includeItemLogicalDigest = false))

    fun packetBindingDigest(packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1): String =
        sha256(buildString {
            appendLine(PACKET_BINDING_DIGEST_DOMAIN)
            appendLine("contractId=${packet.contractId}")
            appendLine("version=${packet.version}")
            appendLine("missionContractId=${packet.missionBinding.missionContractId}")
            appendLine("missionVersion=${packet.missionBinding.missionVersion}")
            appendLine("missionId=${packet.missionBinding.missionId}")
            appendLine("selectionDigest=${packet.missionBinding.selectionDigest}")
            appendLine("humanReviewInputBindingDigest=${packet.humanReviewInputBinding.bindingDigest}")
            appendLine("corpusFileSha256=${packet.corpusFileBinding.sha256}")
            appendLine("corpusLogicalDigest=${packet.corpusLogicalDigest}")
            appendLine("corpusBindingDigest=${packet.corpusBindingDigest}")
            appendLine("packetImplementationHead=${packet.packetImplementationHead}")
            packet.missionBinding.reviewUnitIds.forEach { appendLine("reviewUnitId=$it") }
        })

    fun packetLogicalDigest(packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1): String =
        sha256(buildString {
            appendLine(PACKET_LOGICAL_DIGEST_DOMAIN)
            appendLine("contractId=${packet.contractId}")
            appendLine("version=${packet.version}")
            appendLine("packetState=${packet.packetState.name}")
            appendLine("packetBindingDigest=${packet.packetBindingDigest}")
            appendLine("mission=${packet.missionBinding}")
            appendLine("humanReviewInputBinding=${packet.humanReviewInputBinding}")
            appendLine("corpusFileBinding=${packet.corpusFileBinding}")
            appendLine("corpusLogicalDigest=${packet.corpusLogicalDigest}")
            appendLine("corpusBindingDigest=${packet.corpusBindingDigest}")
            appendLine("packetImplementationHead=${packet.packetImplementationHead}")
            packet.items.forEach { appendLine("itemLogicalDigest=${it.itemLogicalDigest}") }
            appendLine("counters=${packet.counters}")
        })

    fun validate(
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1,
        mission: HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1,
        expectedInputBinding: HimZeroCandidateRecoveryHumanReviewInputBindingV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1 {
        if (packet.contractId != CONTRACT_ID) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_CONTRACT_ID)
        if (packet.version != VERSION) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_CONTRACT_VERSION)
        if (packet.packetState != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1.REVIEW_CONTEXT_ONLY) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_PACKET_STATE)
        if (!HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.validate(mission).valid) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_MISSION_BINDING)
        val expectedMission = missionBinding(mission)
        if (packet.missionBinding != expectedMission) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_MISSION_BINDING)
        if (packet.missionBinding.selectionDigest != mission.selectionDigest) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.MISSION_SELECTION_DIGEST_MISMATCH)
        if (!expectedInputBinding.validate().valid || packet.humanReviewInputBinding != expectedInputBinding) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_INPUT_BINDING)
        if (packet.corpusFileBinding != expectedInputBinding.corpusFileBinding ||
            packet.corpusLogicalDigest != expectedInputBinding.corpusLogicalDigest ||
            packet.corpusBindingDigest != expectedInputBinding.corpusBindingDigest
        ) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_CORPUS_BINDING)
        if (!HEAD.matches(packet.packetImplementationHead)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_IMPLEMENTATION_HEAD)
        if (packet.items.size != mission.expectedSelectedReviewUnits) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_PACKET_ITEM_COUNT)
        val expectedIds = mission.entries.map { it.reviewUnitId }.toSet()
        if (packet.items.any { it.reviewUnitId !in expectedIds }) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.UNEXPECTED_REVIEW_UNIT)
        if (packet.items.map { it.stableEntryId }.distinct().size != packet.items.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.DUPLICATE_STABLE_ENTRY_ID)
        if (packet.items.map { it.reviewUnitId }.distinct().size != packet.items.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.DUPLICATE_REVIEW_UNIT_ID)
        if (packet.items != packet.items.sortedWith(compareBy({ it.reviewUnitId }, { it.stableEntryId }, { it.canonicalEntityId }))) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.UNEXPECTED_REVIEW_UNIT)
        packet.items.forEach { item ->
            validateItem(item, mission)?.let { return invalid(it) }
        }
        if (packet.counters != deriveCounters(packet.items)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_COUNTERS)
        if (packet.packetBindingDigest != packetBindingDigest(packet)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.PACKET_BINDING_DIGEST_MISMATCH)
        if (packet.packetLogicalDigest != packetLogicalDigest(packet)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.PACKET_LOGICAL_DIGEST_MISMATCH)
        return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Valid
    }

    private fun validateItem(
        item: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1,
        mission: HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1? {
        val entry = mission.entries.singleOrNull { it.reviewUnitId == item.reviewUnitId }
        if (entry == null || entry.stableEntryId != item.stableEntryId || entry.canonicalEntityId != item.canonicalEntityId ||
            entry.groupOrdinal != item.groupOrdinal || entry.groupDisplayValue != item.groupDisplayValue ||
            item.reviewUnitId != HimZeroCandidateRecoveryHumanReviewContractV1.reviewUnitId(item.stableEntryId, item.canonicalEntityId)
        ) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.REVIEW_UNIT_BINDING_MISMATCH
        val context = item.sourceContext
        if (context.evidenceReference.isBlank() || context.recordKind.source != context.source ||
            context.findingOccurrenceIds.isEmpty() || context.findingOccurrenceIds != context.findingOccurrenceIds.distinct().sorted() ||
            context.findingOccurrenceIds.any { !SHA256.matches(it) } || context.primaryValues != context.primaryValues.distinct().sorted() ||
            context.selectionReasons != context.selectionReasons.distinct().sortedBy { it.ordinal } ||
            context.recurringGroupMemberships != context.recurringGroupMemberships.sortedWith(compareBy({ it.priorityClass.ordinal }, { it.primaryValue }))
        ) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_SOURCE_CONTEXT
        if (item.originalFields != item.originalFields.sortedBy { it.fieldName } || item.originalFields.map { it.fieldName }.distinct().size != item.originalFields.size) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_ORIGINAL_FIELD
        item.originalFields.forEach { field ->
            if (field.fieldName.isBlank() || field.originalCharacterCount != field.originalValue.length) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_ORIGINAL_FIELD
            if (field.originalValueSha256 != sha256(field.originalValue)) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.ORIGINAL_VALUE_DIGEST_MISMATCH
            when (field.displayState) {
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL -> if (field.humanDisplayValue != field.originalValue) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_DISPLAY_STATE
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.EMPTY_SOURCE_VALUE -> if (field.originalValue.isNotEmpty() || field.humanDisplayValue != "") return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_DISPLAY_STATE
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.TRUNCATED_FOR_HUMAN_PROJECTION -> {
                    if (field.originalValue.isEmpty() || field.humanDisplayValue == null || !field.humanDisplayValue.contains('…') || field.humanDisplayValue == field.originalValue || field.humanDisplayValue.length > MAX_HUMAN_DISPLAY_LENGTH) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.SILENT_TRUNCATION
                    if (HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FIELD_TRUNCATED_IN_HUMAN_PROJECTION !in item.contextLimitations) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.MISSING_TRUNCATION_LIMITATION
                }
            }
        }
        val target = item.targetContext
        if (target.canonicalEntityId != item.canonicalEntityId || target.expectedDisplayLabel.isBlank() || target.registryResolution != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED || target.authorityResolution != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED ||
            target.catalogRecordReference.isBlank() || target.authorityRecordReference.isBlank() || target.catalogRecordReference.startsWith('/') || target.authorityRecordReference.startsWith('/') ||
            target.identityTerms != target.identityTerms.distinct().sorted() || target.aliasTerms != target.aliasTerms.distinct().sorted()
        ) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_TARGET_CONTEXT
        if (target.targetContextDigest != targetContextDigest(target)) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.TARGET_CONTEXT_DIGEST_MISMATCH
        val evidence = item.evidenceScope
        if (evidence.artifactReference.isBlank() || evidence.artifactReference.startsWith('/') || evidence.artifactReference.contains('\\') || !SHA256.matches(evidence.artifactSha256) || evidence.fieldReferences != evidence.fieldReferences.distinct().sorted() || evidence.fieldReferences.any { it.isBlank() }) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_EVIDENCE_SCOPE
        when (evidence.coverage) {
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY -> {
                if (evidence.evidenceKind != HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD || HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FULL_SOURCE_RECORD_NOT_INCLUDED !in item.contextLimitations || HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.SOURCE_PROJECTION_NOT_INCLUDED !in item.contextLimitations) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_EVIDENCE_SCOPE
            }
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.BOUND_SOURCE_PROJECTION_INCLUDED -> {
                if (evidence.evidenceKind != HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION || evidence.artifactLogicalDigest == null || !SHA256.matches(evidence.artifactLogicalDigest)) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.MISSING_SOURCE_PROJECTION_BINDING
            }
        }
        val expectedLimitations = buildList {
            if (target.identityTerms.isEmpty()) add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.IDENTITY_TERMS_ABSENT)
            if (target.aliasTerms.isEmpty()) add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.ALIAS_TERMS_ABSENT)
            if (evidence.coverage == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY) {
                add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FULL_SOURCE_RECORD_NOT_INCLUDED)
                add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.SOURCE_PROJECTION_NOT_INCLUDED)
            }
            if (item.originalFields.any { it.displayState == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.TRUNCATED_FOR_HUMAN_PROJECTION }) add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FIELD_TRUNCATED_IN_HUMAN_PROJECTION)
        }.distinct().sortedBy { it.ordinal }
        if (item.contextLimitations != expectedLimitations) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.INVALID_CONTEXT_LIMITATIONS
        if (item.itemBindingDigest != itemBindingDigest(item)) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.REVIEW_UNIT_BINDING_MISMATCH
        if (item.itemLogicalDigest != itemLogicalDigest(item)) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1.ITEM_LOGICAL_DIGEST_MISMATCH
        return null
    }

    private fun serializeItem(item: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1, domain: String, includeItemLogicalDigest: Boolean): String = buildString {
        appendLine(domain)
        appendLine("stableEntryId=${item.stableEntryId}")
        appendLine("reviewUnitId=${item.reviewUnitId}")
        appendLine("canonicalEntityId=${item.canonicalEntityId}")
        appendLine("groupOrdinal=${item.groupOrdinal}")
        appendLine("groupDisplayValue=${item.groupDisplayValue}")
        appendLine("sourceContext=${item.sourceContext}")
        item.originalFields.forEach { appendLine("originalField=$it") }
        appendLine("targetContext=${item.targetContext}")
        appendLine("evidenceScope=${item.evidenceScope}")
        item.contextLimitations.forEach { appendLine("limitation=${it.name}") }
        appendLine("itemBindingDigest=${item.itemBindingDigest}")
        if (includeItemLogicalDigest) appendLine("itemLogicalDigest=${item.itemLogicalDigest}")
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun invalid(reason: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Invalid(reason)
    private val SHA256 = Regex("[0-9a-f]{64}")
    private val HEAD = Regex("[0-9a-f]{40}")
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1 { REVIEW_CONTEXT_ONLY }

enum class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1 {
    MATERIALIZED_CORPUS_FIELDS_ONLY,
    BOUND_SOURCE_PROJECTION_INCLUDED,
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1 {
    FULL_SOURCE_RECORD_NOT_INCLUDED,
    IDENTITY_TERMS_ABSENT,
    ALIAS_TERMS_ABSENT,
    SOURCE_PROJECTION_NOT_INCLUDED,
    FIELD_TRUNCATED_IN_HUMAN_PROJECTION,
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1 {
    FULL,
    TRUNCATED_FOR_HUMAN_PROJECTION,
    EMPTY_SOURCE_VALUE,
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1 { RESOLVED, UNRESOLVED }

enum class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1 {
    INVALID_CONTRACT_ID,
    INVALID_CONTRACT_VERSION,
    INVALID_PACKET_STATE,
    INVALID_MISSION_BINDING,
    MISSION_SELECTION_DIGEST_MISMATCH,
    INVALID_INPUT_BINDING,
    INVALID_CORPUS_BINDING,
    INVALID_IMPLEMENTATION_HEAD,
    INVALID_PACKET_ITEM_COUNT,
    UNEXPECTED_REVIEW_UNIT,
    DUPLICATE_STABLE_ENTRY_ID,
    DUPLICATE_REVIEW_UNIT_ID,
    REVIEW_UNIT_BINDING_MISMATCH,
    INVALID_SOURCE_CONTEXT,
    INVALID_ORIGINAL_FIELD,
    ORIGINAL_VALUE_DIGEST_MISMATCH,
    INVALID_DISPLAY_STATE,
    SILENT_TRUNCATION,
    MISSING_TRUNCATION_LIMITATION,
    INVALID_TARGET_CONTEXT,
    TARGET_CONTEXT_DIGEST_MISMATCH,
    INVALID_EVIDENCE_SCOPE,
    MISSING_SOURCE_PROJECTION_BINDING,
    INVALID_CONTEXT_LIMITATIONS,
    INVALID_COUNTERS,
    PACKET_BINDING_DIGEST_MISMATCH,
    ITEM_LOGICAL_DIGEST_MISMATCH,
    PACKET_LOGICAL_DIGEST_MISMATCH,
    FORBIDDEN_DECISION_SEMANTICS,
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1 {
    val valid: Boolean
    data object Valid : HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1 { override val valid = true }
    data class Invalid(val reason: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketFailureReasonV1) : HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1 { override val valid = false }
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketMissionBindingV1(
    val missionContractId: String,
    val missionVersion: String,
    val missionId: String,
    val scopeId: String,
    val priority: HimZeroCandidateRecoveryReviewPriorityV1,
    val reviewRound: Int,
    val maxNewDecisionRecords: Int,
    val selectionDigest: String,
    val expectedSelectedGroups: Int,
    val expectedSelectedCorpusEntries: Int,
    val expectedSelectedReviewUnits: Int,
    val expectedDistinctCanonicalTargets: Int,
    val expectedMultiTargetEntries: Int,
    val reviewUnitIds: List<String>,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1(
    val source: HimGroundTruthSource,
    val recordKind: HimEvidenceRecordKind,
    val evidenceReference: String,
    val findingOccurrenceIds: List<String>,
    val selectionReasons: List<HimZeroCandidateRecoveryReviewSelectionReasonV1>,
    val primaryValues: List<String>,
    val primaryValueBucket: HimZeroCandidateCauseAnalysisPrimaryBucketV1,
    val recurringGroupMemberships: List<HimZeroCandidateRecoveryReviewGroupMembershipV1>,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1(
    val fieldName: String,
    val originalValue: String,
    val originalValueSha256: String,
    val humanDisplayValue: String?,
    val displayState: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1,
    val originalCharacterCount: Int,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1(
    val canonicalEntityId: String,
    val expectedDisplayLabel: String,
    val registryResolution: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1,
    val authorityResolution: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1,
    val catalogRecordReference: String,
    val authorityRecordReference: String,
    val identityTerms: List<String>,
    val aliasTerms: List<String>,
    val targetContextDigest: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceScopeV1(
    val evidenceKind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
    val directness: HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1,
    val artifactReference: String,
    val artifactSha256: String,
    val artifactLogicalDigest: String?,
    val recordReference: String,
    val fieldReferences: List<String>,
    val coverage: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1(
    val stableEntryId: String,
    val reviewUnitId: String,
    val canonicalEntityId: String,
    val groupOrdinal: Int,
    val groupDisplayValue: String,
    val sourceContext: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1,
    val originalFields: List<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1>,
    val targetContext: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1,
    val evidenceScope: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceScopeV1,
    val contextLimitations: List<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1>,
    val itemBindingDigest: String,
    val itemLogicalDigest: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceCountV1(val source: HimGroundTruthSource, val items: Int)
data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRecordKindCountV1(val recordKind: HimEvidenceRecordKind, val items: Int)

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketCountersV1(
    val packetItems: Int,
    val uniqueStableEntries: Int,
    val uniqueReviewUnits: Int,
    val distinctCanonicalTargets: Int,
    val sourceCounts: List<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceCountV1>,
    val recordKindCounts: List<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRecordKindCountV1>,
    val materializedCorpusOnlyItems: Int,
    val sourceProjectionIncludedItems: Int,
    val itemsWithIdentityTerms: Int,
    val itemsWithoutIdentityTerms: Int,
    val itemsWithAliasTerms: Int,
    val itemsWithoutAliasTerms: Int,
    val fullDisplayFields: Int,
    val truncatedDisplayFields: Int,
    val emptySourceFields: Int,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1(
    val contractId: String,
    val version: String,
    val packetState: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1,
    val missionBinding: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketMissionBindingV1,
    val humanReviewInputBinding: HimZeroCandidateRecoveryHumanReviewInputBindingV1,
    val corpusFileBinding: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val corpusLogicalDigest: String,
    val corpusBindingDigest: String,
    val packetImplementationHead: String,
    val items: List<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1>,
    val counters: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketCountersV1,
    val packetBindingDigest: String,
    val packetLogicalDigest: String,
)
