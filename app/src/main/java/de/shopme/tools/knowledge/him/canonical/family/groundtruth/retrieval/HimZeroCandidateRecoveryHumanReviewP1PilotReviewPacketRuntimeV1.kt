package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File

/** Builds the neutral, context-only packet for the committed P1 pilot mission. */
object HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1 {
    const val CONTRACT_ID = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_REVIEW_PACKET_RUNTIME_V1"
    const val VERSION = "1"

    fun execute(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1 {
        if (!request.enabled) return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Disabled
        return try {
            val packet = buildPacket(request)
            val persistence = when (
                val result = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.execute(
                    HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceRequestV1(
                        enabled = true,
                        outputRoot = request.packetOutputRoot,
                        packet = packet,
                    ),
                )
            ) {
                is HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Completed -> result
                is HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Disabled ->
                    fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.PERSISTENCE_FAILED, "persistence")
                is HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceResultV1.Failed -> {
                    val reason = when (result.reason) {
                        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.RELOAD_MISMATCH -> HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.PERSISTENCE_RELOAD_MISMATCH
                        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceFailureReasonV1.BYTE_IDENTITY_MISMATCH -> HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.BYTE_IDENTITY_MISMATCH
                        else -> HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.PERSISTENCE_FAILED
                    }
                    fail(reason, "persistence")
                }
            }
            val persistedJson = request.packetOutputRoot.resolve(persistence.jsonPath)
            val reloaded = try {
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.readPacket(persistedJson)
            } catch (_: Throwable) {
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.PERSISTENCE_RELOAD_MISMATCH, "packet-json")
            }
            if (reloaded != packet) {
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.PERSISTENCE_RELOAD_MISMATCH, "packet-json")
            }
            val counters = packet.counters
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed(
                runtimeContractId = CONTRACT_ID,
                runtimeVersion = VERSION,
                missionId = packet.missionBinding.missionId,
                packet = reloaded,
                persistenceStatus = persistence.status,
                jsonPath = persistence.jsonPath,
                markdownPath = persistence.markdownPath,
                jsonByteSize = persistence.jsonByteSize,
                markdownByteSize = persistence.markdownByteSize,
                jsonSha256 = persistence.jsonSha256,
                markdownSha256 = persistence.markdownSha256,
                packetBindingDigest = persistence.packetBindingDigest,
                packetLogicalDigest = persistence.packetLogicalDigest,
                counters = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeCountersV1(
                    corpusEntries = request.corpus.entries.size,
                    missionEntriesRequested = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION.entries.size,
                    missionEntriesResolved = packet.items.size,
                    packetItemsCreated = packet.items.size,
                    uniqueReviewUnits = counters.uniqueReviewUnits,
                    distinctCanonicalTargets = counters.distinctCanonicalTargets,
                    originalFieldsMaterialized = packet.items.sumOf { it.originalFields.size },
                    fullDisplayFields = counters.fullDisplayFields,
                    truncatedDisplayFields = counters.truncatedDisplayFields,
                    emptySourceFields = counters.emptySourceFields,
                    itemsWithIdentityTerms = counters.itemsWithIdentityTerms,
                    itemsWithoutIdentityTerms = counters.itemsWithoutIdentityTerms,
                    itemsWithAliasTerms = counters.itemsWithAliasTerms,
                    itemsWithoutAliasTerms = counters.itemsWithoutAliasTerms,
                    materializedCorpusOnlyItems = counters.materializedCorpusOnlyItems,
                    persistedPacketItems = packet.items.size,
                ),
            )
        } catch (failure: RuntimeFailure) {
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed(failure.reason, failure.safeContext)
        } catch (_: Throwable) {
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed(
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INTERNAL_INVARIANT_VIOLATION,
                "runtime",
            )
        }
    }

    private fun buildPacket(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1 {
        val mission = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION
        if (!HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.validate(mission).valid) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_MISSION, "mission")
        }
        if (HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.VERSION != VERSION ||
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.VERSION != VERSION
        ) fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_REQUEST, "contract-version")
        if (!request.packetImplementationHead.matches(Regex("[0-9a-f]{40}"))) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_IMPLEMENTATION_HEAD, "implementation-head")
        }
        if (!request.inputBinding.validate().valid) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_INPUT_BINDING, "input-binding")
        }
        val corpus = validateCorpus(request)
        validateFoundation(request)
        val canonicalInputBinding = request.inputBinding.copy(
            corpusFileBinding = request.inputBinding.corpusFileBinding.copy(logicalDigest = corpus.logicalDigest),
            corpusLogicalDigest = corpus.logicalDigest,
        ).let { it.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(it)) }

        val items = mission.entries.map { missionEntry ->
            val matches = corpus.entries.filter { it.stableEntryId == missionEntry.stableEntryId }
            if (matches.isEmpty()) fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.MISSING_MISSION_ENTRY, "mission-entry")
            if (matches.size != 1) fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.DUPLICATE_MISSION_ENTRY, "mission-entry")
            val entry = matches.single()
            buildItem(request.copy(inputBinding = canonicalInputBinding), missionEntry, entry)
        }
        val packet = try {
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.create(
                missionBinding = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.missionBinding(mission),
                humanReviewInputBinding = canonicalInputBinding,
                corpusFileBinding = canonicalInputBinding.corpusFileBinding,
                corpusLogicalDigest = canonicalInputBinding.corpusLogicalDigest,
                corpusBindingDigest = request.inputBinding.corpusBindingDigest,
                packetImplementationHead = request.packetImplementationHead,
                items = items,
            )
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.PACKET_BUILD_FAILED, "packet")
        }
        when (val validation = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.validate(packet, mission, canonicalInputBinding)) {
            is HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Invalid ->
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.PACKET_VALIDATION_FAILED, "packet")
        }
        return packet
    }

    private fun validateCorpus(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1,
    ): HimZeroCandidateRecoveryReviewCorpusReportV1 {
        val input = request.inputBinding
        if (input.corpusFileBinding.logicalDigest.isBlank() ||
            input.corpusBindingDigest != request.corpus.inputBinding.bindingDigest ||
            request.corpus.inputBinding != input.existingCorpusInputBinding
        ) fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.CORPUS_BINDING_MISMATCH, "corpus-binding")
        val canonicalOrder = request.corpus.entries.sortedWith(
            compareBy({ HimZeroCandidateRecoveryReviewCorpusContractV1.sourceIndex(it.source) }, { it.evidenceReference }),
        )
        val unsigned = request.corpus.copy(entries = canonicalOrder, logicalDigest = "")
        val normalized = unsigned.copy(logicalDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.logicalDigest(unsigned))
        try {
            normalized.validate()
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_CORPUS, "corpus")
        }
        return normalized
    }

    private fun validateFoundation(request: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1) {
        try {
            HimCanonicalFamilyValidator().validate(request.catalog, request.registry, request.authority)
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_FOUNDATION, "foundation")
        }
        val source = request.authority.sourceCatalog
        val inputCatalog = request.inputBinding.existingCorpusInputBinding.canonicalCatalog
        val inputAuthority = request.inputBinding.existingCorpusInputBinding.canonicalAuthority
        if (source.path != request.catalog.path || source.contentSha256 != request.catalog.contentSha256 || source.recordCount != request.catalog.records.size ||
            inputCatalog.relativePath != request.catalog.path || inputCatalog.sha256 != request.catalog.contentSha256 ||
            inputAuthority.relativePath.isBlank() || inputAuthority.sha256.isBlank()
        ) fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.FOUNDATION_BINDING_MISMATCH, "foundation-binding")
    }

    private fun buildItem(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1,
        missionEntry: HimZeroCandidateRecoveryHumanReviewP1PilotMissionEntryV1,
        entry: HimZeroCandidateRecoveryReviewCorpusEntryV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1 {
        if (entry.auditLinkedCanonicalTargets.size != 1) fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.MULTI_TARGET_MISSION_ENTRY, "mission-target")
        val target = entry.auditLinkedCanonicalTargets.single()
        if (target.canonicalEntityId != missionEntry.canonicalEntityId) fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.UNEXPECTED_MISSION_TARGET, "mission-target")
        if (entry.recurringGroupMemberships.none { it.priorityClass == HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET }) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_PRIORITY, "priority")
        }
        if (entry.reviewUnitId() != missionEntry.reviewUnitId) fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.REVIEW_UNIT_ID_MISMATCH, "review-unit")
        if (entry.source.name.isBlank() || entry.recordKind.source != entry.source || entry.findingOccurrenceIds.isEmpty() || entry.primaryValues.isEmpty() || entry.originalFields.isEmpty()) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_SOURCE_CONTEXT, "source-context")
        }
        if (entry.evidenceReference.isBlank()) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_EVIDENCE_CONTEXT, "evidence")
        }
        if (entry.originalFields.map { it.fieldPath }.distinct().size != entry.originalFields.size) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.DUPLICATE_ORIGINAL_FIELD, "original-fields")
        }
        if (entry.originalFields.any { it.fieldPath.isBlank() || it.source != entry.source || it.recordKind != entry.recordKind }) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.INVALID_ORIGINAL_FIELD, "original-fields")
        }
        val registryEntry = request.registry.entries.singleOrNull {
            it.entityType == HimEntityType.CANONICAL && it.entityId.value == target.canonicalEntityId
        } ?: fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.TARGET_NOT_FOUND, "target")
        val catalogRecord = request.catalog.records.singleOrNull { it.normalized == registryEntry.sourceReference }
            ?: fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.TARGET_NOT_FOUND, "target")
        val family = request.authority.families.singleOrNull { it.canonicalId.value == target.canonicalEntityId }
            ?: fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.TARGET_NOT_FOUND, "target")
        if (family.canonicalName != target.canonicalName || family.canonicalName != missionEntry.expectedDisplayLabel || family.normalizedName != catalogRecord.normalized) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1.TARGET_LABEL_MISMATCH, "target")
        }
        val identityTerms = family.identities.map { it.identityName }.distinct().sorted()
        val aliasTerms = (family.aliases.map { it.aliasName } + family.identities.flatMap { identity -> identity.aliases.map { it.aliasName } }).distinct().sorted()
        val targetContext = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1(
            target.canonicalEntityId,
            missionEntry.expectedDisplayLabel,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED,
            "catalog:${target.canonicalEntityId}",
            "authority:${target.canonicalEntityId}",
            identityTerms,
            aliasTerms,
            "",
        ).let { it.copy(targetContextDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(it)) }
        val originals = entry.originalFields.map { field -> originalField(field) }
        val limitations = buildList {
            add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FULL_SOURCE_RECORD_NOT_INCLUDED)
            add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.SOURCE_PROJECTION_NOT_INCLUDED)
            if (identityTerms.isEmpty()) add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.IDENTITY_TERMS_ABSENT)
            if (aliasTerms.isEmpty()) add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.ALIAS_TERMS_ABSENT)
            if (originals.any { it.displayState == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.TRUNCATED_FOR_HUMAN_PROJECTION }) add(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FIELD_TRUNCATED_IN_HUMAN_PROJECTION)
        }.distinct().sortedBy { it.ordinal }
        return HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1(
            entry.stableEntryId,
            missionEntry.reviewUnitId,
            target.canonicalEntityId,
            missionEntry.groupOrdinal,
            missionEntry.groupDisplayValue,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1(entry.source, entry.recordKind, entry.evidenceReference, entry.findingOccurrenceIds, entry.selectionReasons, entry.primaryValues, entry.primaryBucket, entry.recurringGroupMemberships),
            originals.sortedBy { it.fieldName },
            targetContext,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceScopeV1(
                HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD,
                HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.INDIRECT,
                request.inputBinding.corpusFileBinding.relativePath,
                request.inputBinding.corpusFileBinding.sha256,
                request.inputBinding.corpusFileBinding.logicalDigest,
                entry.evidenceReference,
                originals.map { it.fieldName }.sorted(),
                HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY,
            ),
            limitations,
            "",
            "",
        )
    }

    private fun originalField(field: HimUnresolvedPrimaryIdentityDiagnosticFieldV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1(
            fieldName = field.fieldPath,
            originalValue = field.originalLexicalValue,
            originalValueSha256 = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(field.originalLexicalValue),
            humanDisplayValue = when {
                field.originalLexicalValue.isEmpty() -> ""
                field.originalLexicalValue.length <= HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.MAX_HUMAN_DISPLAY_LENGTH -> field.originalLexicalValue
                else -> {
                    var end = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.MAX_HUMAN_DISPLAY_LENGTH - 1
                    if (Character.isLowSurrogate(field.originalLexicalValue[end])) end -= 1
                    field.originalLexicalValue.substring(0, end) + "…"
                }
            },
            displayState = when {
                field.originalLexicalValue.isEmpty() -> HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.EMPTY_SOURCE_VALUE
                field.originalLexicalValue.length <= HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.MAX_HUMAN_DISPLAY_LENGTH -> HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL
                else -> HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.TRUNCATED_FOR_HUMAN_PROJECTION
            },
            originalCharacterCount = field.originalLexicalValue.length,
        )

    private fun HimZeroCandidateRecoveryReviewCorpusEntryV1.reviewUnitId(): String =
        HimZeroCandidateRecoveryHumanReviewContractV1.reviewUnitId(
            stableEntryId,
            auditLinkedCanonicalTargets.single().canonicalEntityId,
        )

    private fun fail(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1,
        safeContext: String,
    ): Nothing = throw RuntimeFailure(reason, safeContext)
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1(
    val enabled: Boolean,
    val corpus: HimZeroCandidateRecoveryReviewCorpusReportV1,
    val inputBinding: HimZeroCandidateRecoveryHumanReviewInputBindingV1,
    val catalog: HimProductOnlyCanonicalMaster,
    val registry: HimEntityIdRegistry,
    val authority: HimCanonicalFamilyAuthority,
    val packetImplementationHead: String,
    val packetOutputRoot: File,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeCountersV1(
    val corpusEntries: Int,
    val missionEntriesRequested: Int,
    val missionEntriesResolved: Int,
    val packetItemsCreated: Int,
    val uniqueReviewUnits: Int,
    val distinctCanonicalTargets: Int,
    val originalFieldsMaterialized: Int,
    val fullDisplayFields: Int,
    val truncatedDisplayFields: Int,
    val emptySourceFields: Int,
    val itemsWithIdentityTerms: Int,
    val itemsWithoutIdentityTerms: Int,
    val itemsWithAliasTerms: Int,
    val itemsWithoutAliasTerms: Int,
    val materializedCorpusOnlyItems: Int,
    val persistedPacketItems: Int,
)

enum class HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1 {
    INVALID_REQUEST,
    INVALID_IMPLEMENTATION_HEAD,
    INVALID_MISSION,
    INVALID_INPUT_BINDING,
    INVALID_CORPUS,
    CORPUS_BINDING_MISMATCH,
    INVALID_FOUNDATION,
    FOUNDATION_BINDING_MISMATCH,
    MISSING_MISSION_ENTRY,
    DUPLICATE_MISSION_ENTRY,
    UNEXPECTED_MISSION_TARGET,
    MULTI_TARGET_MISSION_ENTRY,
    INVALID_PRIORITY,
    REVIEW_UNIT_ID_MISMATCH,
    INVALID_SOURCE_CONTEXT,
    DUPLICATE_ORIGINAL_FIELD,
    INVALID_ORIGINAL_FIELD,
    TARGET_NOT_FOUND,
    TARGET_LABEL_MISMATCH,
    INVALID_TARGET_CONTEXT,
    INVALID_EVIDENCE_CONTEXT,
    PACKET_BUILD_FAILED,
    PACKET_VALIDATION_FAILED,
    PERSISTENCE_FAILED,
    PERSISTENCE_RELOAD_MISMATCH,
    BYTE_IDENTITY_MISMATCH,
    INTERNAL_INVARIANT_VIOLATION,
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1 {
    data object Disabled : HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1

    data class Completed(
        val runtimeContractId: String,
        val runtimeVersion: String,
        val missionId: String,
        val packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1,
        val persistenceStatus: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1,
        val jsonPath: String,
        val markdownPath: String,
        val jsonByteSize: Long,
        val markdownByteSize: Long,
        val jsonSha256: String,
        val markdownSha256: String,
        val packetBindingDigest: String,
        val packetLogicalDigest: String,
        val counters: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeCountersV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1
}

private class RuntimeFailure(
    val reason: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeFailureReasonV1,
    val safeContext: String,
) : IllegalArgumentException()
