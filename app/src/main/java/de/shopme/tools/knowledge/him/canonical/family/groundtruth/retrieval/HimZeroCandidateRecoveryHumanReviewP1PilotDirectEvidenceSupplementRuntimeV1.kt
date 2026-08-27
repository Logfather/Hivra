package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Offline runtime that materializes a direct-evidence context from already resolved inputs.
 * It deliberately has no source, retrieval, SQLite, network, or decision-generation boundary.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_RUNTIME_V1"
    const val VERSION = "1"
    const val PROJECTION_LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_PROJECTION_LOGICAL_V1"

    private val HEAD = Regex("[0-9a-f]{40}")
    private val EXPECTED_SOURCE_REFERENCES = listOf(
        "off:product:row:431650:code:4002239680509",
        "off:product:row:3272579:code:0061483010917",
        "off:product:row:1551407:code:4013200552046",
        "off:product:row:3322623:code:2026088009283",
    )
    private val FORBIDDEN_SOURCE_MARKERS = listOf(
        "review-corpus",
        "review-packet",
        "decision-batch",
        "decision_batches",
        "direct-evidence-supplement",
    )

    fun execute(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1 {
        if (!request.enabled) return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Disabled
        return try {
            validateRequest(request)
            val targets = validateFoundation(request.foundationInput)
            val inputs = normalizeSourceInputs(request.sourceProjectionInputs, request.mission)
            val supplement = buildSupplement(request, targets, inputs)
            requireSupplementValid(supplement)
            val persisted = persist(request.outputRoot, supplement)
            val jsonFile = request.outputRoot.toPath().resolve(persisted.jsonPath).normalize()
            val markdownFile = request.outputRoot.toPath().resolve(persisted.markdownPath).normalize()
            if (!jsonFile.startsWith(request.outputRoot.toPath().toAbsolutePath().normalize()) ||
                !markdownFile.startsWith(request.outputRoot.toPath().toAbsolutePath().normalize())
            ) fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.RELOAD_FAILED, "paths")
            val jsonBytes = Files.readAllBytes(jsonFile)
            val markdownBytes = Files.readAllBytes(markdownFile)
            if (jsonBytes.size.toLong() != persisted.jsonByteSize || markdownBytes.size.toLong() != persisted.markdownByteSize) {
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.BYTE_IDENTITY_MISMATCH, "sizes")
            }
            if (sha256(jsonBytes) != persisted.jsonSha256 || sha256(markdownBytes) != persisted.markdownSha256) {
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.BYTE_IDENTITY_MISMATCH, "hashes")
            }
            val reloaded = try {
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.readSupplement(jsonFile.toFile())
            } catch (_: Throwable) {
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.RELOAD_FAILED, "json")
            }
            requireSupplementValid(reloaded)
            if (reloaded != supplement ||
                !HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.serializeSupplement(reloaded).contentEquals(jsonBytes) ||
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.renderMarkdown(reloaded).toByteArray(StandardCharsets.UTF_8).contentEquals(markdownBytes).not()
            ) fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.RELOAD_MISMATCH, "supplement")
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Completed(
                persistenceStatus = persisted.status,
                supplement = reloaded,
                counters = reloaded.counters,
                supplementBindingDigest = reloaded.supplementBindingDigest,
                supplementLogicalDigest = reloaded.supplementLogicalDigest,
                jsonPath = persisted.jsonPath,
                markdownPath = persisted.markdownPath,
                jsonByteSize = persisted.jsonByteSize,
                markdownByteSize = persisted.markdownByteSize,
                jsonSha256 = persisted.jsonSha256,
                markdownSha256 = persisted.markdownSha256,
            )
        } catch (failure: RuntimeFailure) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Failed(failure.reason, failure.safeContext)
        } catch (_: Throwable) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Failed(
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_REQUEST,
                "runtime",
            )
        }
    }

    fun projectionLogicalDigest(input: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1): String =
        sha256(buildString {
            appendLine(PROJECTION_LOGICAL_DIGEST_DOMAIN)
            appendLine("stableEntryId=${input.stableEntryId}")
            appendLine("reviewUnitId=${input.reviewUnitId}")
            appendLine("canonicalEntityId=${input.canonicalEntityId}")
            appendLine("source=${input.source.name}")
            appendLine("recordKind=${input.recordKind.name}")
            appendLine("recordReference=${input.recordReference}")
            appendArtifact(input.artifact)
            appendArtifact(input.sourceOriginArtifact)
            input.fields.sortedBy { it.fieldReference }.forEach {
                appendLine("fieldReference=${it.fieldReference}")
                appendLine("fullValue=${it.fullValue}")
                appendLine("fullValueSha256=${it.fullValueSha256}")
            }
        })

    private fun validateRequest(request: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeRequestV1) {
        if (!HEAD.matches(request.supplementImplementationHead)) fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_IMPLEMENTATION_HEAD, "implementation-head")
        if (request.mission != HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION ||
            !HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.validate(request.mission).valid
        ) fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_MISSION, "mission")
        val packet = request.reviewPacket
        if (packet.contractId != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.CONTRACT_ID ||
            packet.version != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.VERSION ||
            packet.packetState != HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1.REVIEW_CONTEXT_ONLY
        ) fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_PACKET, "packet")
        if (!HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.validate(packet, request.mission, packet.humanReviewInputBinding).valid) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_PACKET, "packet")
        }
        val binding = request.reviewPacketBinding
        if (binding.jsonFileBinding.validate() is HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid ||
            binding.markdownFileBinding.validate() is HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid ||
            binding.packetInputBindingDigest != HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_INPUT_BINDING_DIGEST ||
            binding.packetBindingDigest != HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_BINDING_DIGEST ||
            binding.packetLogicalDigest != HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_LOGICAL_DIGEST ||
            binding.packetImplementationHead != HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_IMPLEMENTATION_HEAD ||
            packet.packetImplementationHead != binding.packetImplementationHead ||
            packet.packetBindingDigest != binding.packetBindingDigest ||
            packet.packetLogicalDigest != binding.packetLogicalDigest ||
            packet.humanReviewInputBinding.bindingDigest != binding.packetInputBindingDigest
        ) fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_PACKET_BINDING, "packet-binding")
        if (binding.jsonFileBinding.relativePath.endsWith(".md") || binding.markdownFileBinding.relativePath.endsWith(".json")) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_PACKET_BINDING, "packet-files")
        }
    }

    private fun validateFoundation(
        input: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFoundationInputV1,
    ): Map<String, FoundationTarget> {
        if (input.catalogBinding.validate() is HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid ||
            input.registryBinding.validate() is HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid ||
            input.authorityBinding.validate() is HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid ||
            input.catalogBinding.relativePath != input.catalog.path ||
            input.catalogBinding.sha256 != input.catalog.contentSha256
        ) fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_FOUNDATION, "foundation-binding")
        try {
            HimCanonicalFamilyValidator().validate(input.catalog, input.registry, input.authority)
        } catch (_: Throwable) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_FOUNDATION, "foundation")
        }
        val targets = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.FROZEN_REVIEW_UNITS
            .map { it.canonicalEntityId }
            .distinct()
            .associateWith { id ->
                val registry = input.registry.entries.singleOrNull {
                    it.entityType == HimEntityType.CANONICAL && it.entityId.value == id
                } ?: fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.MISSING_CANONICAL_TARGET, "target")
                val record = input.catalog.records.singleOrNull { it.normalized == registry.sourceReference }
                    ?: fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_CANONICAL_TARGET, "target")
                val family = input.authority.families.singleOrNull { it.canonicalId.value == id }
                    ?: fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.MISSING_CANONICAL_TARGET, "target")
                if (family.canonicalName != record.itemname || family.normalizedName != record.normalized) {
                    fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_CANONICAL_TARGET, "target")
                }
                FoundationTarget(registry, record, family)
            }
        return targets
    }

    private fun normalizeSourceInputs(
        inputs: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1>,
        mission: HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1,
    ): List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1> {
        if (inputs.size != mission.entries.size) fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_SOURCE_PROJECTION_COUNT, "source-count")
        if (inputs.map { it.recordReference }.distinct().size != inputs.size || inputs.map { it.reviewUnitId }.distinct().size != inputs.size) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.DUPLICATE_SOURCE_PROJECTION, "source-duplicates")
        }
        val normalized = mission.entries.mapIndexed { index, entry ->
            val input = inputs.singleOrNull { it.stableEntryId == entry.stableEntryId }
                ?: fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.MISSING_SOURCE_PROJECTION, "source")
            if (input.reviewUnitId != entry.reviewUnitId || input.canonicalEntityId != entry.canonicalEntityId ||
                input.recordReference != EXPECTED_SOURCE_REFERENCES[index] ||
                input.source != HimGroundTruthSource.OPEN_FOOD_FACTS || input.recordKind != HimEvidenceRecordKind.OFF_PRODUCT
            ) fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_SOURCE_PROJECTION_BINDING, "source-binding")
            val artifactFailure = input.artifact.validate() ?: input.sourceOriginArtifact.validate()
            if (artifactFailure != null || input.fields.isEmpty() || input.fields.any { it.validate() != null } ||
                input.artifact.relativePath.lowercase().containsAny(FORBIDDEN_SOURCE_MARKERS) ||
                input.sourceOriginArtifact.relativePath.lowercase().containsAny(FORBIDDEN_SOURCE_MARKERS) ||
                input.artifact.relativePath.endsWith(".md") || input.sourceOriginArtifact.relativePath.endsWith(".md") ||
                input.fields.none { it.fullValue.isNotBlank() }
            ) fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_SOURCE_FIELD, "source-fields")
            if (input.projectionLogicalDigest != projectionLogicalDigest(input)) {
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.SOURCE_PROJECTION_DIGEST_MISMATCH, "source-digest")
            }
            input
        }
        return normalized
    }

    private fun buildSupplement(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeRequestV1,
        targets: Map<String, FoundationTarget>,
        inputs: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1>,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1 {
        val bundles = inputs.mapIndexed { index, input ->
            val target = targets.getValue(input.canonicalEntityId)
            val reviewUnit = HimZeroCandidateRecoveryHumanReviewReviewUnitV1(input.stableEntryId, input.canonicalEntityId)
            val sourcePosition = if (index == 0 || index == 2) {
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION
            } else {
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
            }
            val targetPosition = if (index == 0 || index == 2) {
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION
            } else {
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY
            }
            val packetItem = request.reviewPacket.items.singleOrNull { it.reviewUnitId == input.reviewUnitId }
                ?: fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_PACKET, "packet-item")
            val sourceProjection = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1(
                source = input.source,
                recordKind = input.recordKind,
                artifact = input.artifact,
                recordReference = input.recordReference,
                fields = input.fields.sortedBy { it.fieldReference },
                sourceOriginArtifact = input.sourceOriginArtifact,
            )
            val sourceCard = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(
                reviewUnitId = reviewUnit.reviewUnitId,
                evidenceReferenceId = "",
                kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION,
                directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                position = sourcePosition,
                artifact = input.artifact,
                recordReference = input.recordReference,
                fields = input.fields.sortedBy { it.fieldReference },
                sourceProjection = sourceProjection,
            )
            val catalogArtifact = request.foundationInput.catalogBinding.toArtifact()
            val authorityArtifact = request.foundationInput.authorityBinding.toArtifact()
            val catalogCard = catalogCard(
                reviewUnit.reviewUnitId,
                target,
                catalogArtifact,
                targetPosition,
            )
            val authorityCard = authorityCard(
                reviewUnit.reviewUnitId,
                target,
                authorityArtifact,
            )
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1(
                reviewUnit = reviewUnit,
                packetItemBindingDigest = packetItem.itemBindingDigest,
                packetItemLogicalDigest = packetItem.itemLogicalDigest,
                evidence = listOf(sourceCard, catalogCard, authorityCard),
                unitBindingDigest = "",
                unitLogicalDigest = "",
            )
        }
        val binding = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1(
            missionContractId = request.mission.contractId,
            missionVersion = request.mission.version,
            missionId = request.mission.missionId,
            scopeId = request.mission.scopeId,
            missionSelectionDigest = request.mission.selectionDigest,
            packetContractId = request.reviewPacket.contractId,
            packetVersion = request.reviewPacket.version,
            packetInputBindingDigest = request.reviewPacketBinding.packetInputBindingDigest,
            packetBindingDigest = request.reviewPacketBinding.packetBindingDigest,
            packetLogicalDigest = request.reviewPacketBinding.packetLogicalDigest,
            corpusArtifact = request.reviewPacket.humanReviewInputBinding.corpusFileBinding.toArtifact(),
            corpusBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CORPUS_BINDING_DIGEST,
            packetImplementationHead = request.reviewPacketBinding.packetImplementationHead,
        )
        return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(binding, bundles)
    }

    private fun catalogCard(
        reviewUnitId: String,
        target: FoundationTarget,
        artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
        position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
    ) = card(
        reviewUnitId = reviewUnitId,
        kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD,
        position = position,
        artifact = artifact,
        recordReference = "catalog:${target.authority.canonicalId.value}",
        fields = listOf(
            field("canonical.entityId", target.authority.canonicalId.value),
            field("canonical.name", target.catalog.itemname),
            field("canonical.normalizedName", target.catalog.normalized),
            field("canonical.taxonomyPaths", target.catalog.taxonomyPaths.toString()),
        ),
    )

    private fun authorityCard(
        reviewUnitId: String,
        target: FoundationTarget,
        artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
    ) = card(
        reviewUnitId = reviewUnitId,
        kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD,
        position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY,
        artifact = artifact,
        recordReference = "authority:${target.authority.canonicalId.value}",
        fields = listOf(
            field("canonical.entityId", target.authority.canonicalId.value),
            field("canonical.name", target.authority.canonicalName),
            field("canonical.normalizedName", target.authority.normalizedName),
            field("canonical.lifecycleStatus", target.authority.lifecycleStatus.name),
            field("canonical.identities", target.authority.identities.toString()),
            field("canonical.variants", target.authority.variants.toString()),
            field("canonical.aliases", target.authority.aliases.toString()),
        ),
    )

    private fun card(
        reviewUnitId: String,
        kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
        position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
        artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
        recordReference: String,
        fields: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1>,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(
        reviewUnitId = reviewUnitId,
        evidenceReferenceId = "",
        kind = kind,
        directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
        position = position,
        artifact = artifact,
        recordReference = recordReference,
        fields = fields.sortedBy { it.fieldReference },
        sourceProjection = null,
    )

    private fun field(
        reference: String,
        value: String,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1(
        fieldReference = reference,
        fullValue = value,
        fullValueSha256 = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1.sha256(value),
    )

    private fun requireSupplementValid(supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1) {
        when (val result = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.validate(supplement)) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementValidationResultV1.Invalid ->
                fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.SUPPLEMENT_VALIDATION_FAILED, "supplement")
        }
    }

    private fun persist(
        outputRoot: File,
        supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Completed = when (
        val result = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceRequestV1(true, outputRoot, supplement),
        )
    ) {
        is HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Completed -> result
        is HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Failed -> {
            val reason = when (result.reason) {
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.RELOAD_MISMATCH -> HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.RELOAD_MISMATCH
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.JSON_BYTE_MISMATCH,
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.MARKDOWN_BYTE_MISMATCH,
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceFailureReasonV1.SHA256_MISMATCH ->
                    HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.BYTE_IDENTITY_MISMATCH
                else -> HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.PERSISTENCE_FAILED
            }
            fail(reason, "persistence")
        }
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceResultV1.Disabled ->
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.PERSISTENCE_FAILED, "persistence")
    }

    private fun HimZeroCandidateRecoveryHumanReviewFileBindingV1.toArtifact() =
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1(relativePath, byteSize, sha256, logicalDigest)

    private fun String.containsAny(values: List<String>): Boolean = values.any { it in this }

    private fun StringBuilder.appendArtifact(artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1) {
        appendLine("artifactPath=${artifact.relativePath}")
        appendLine("artifactByteSize=${artifact.byteSize}")
        appendLine("artifactSha256=${artifact.sha256}")
        appendLine("artifactLogicalDigest=${artifact.logicalDigest ?: ""}")
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    private fun fail(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1,
        safeContext: String,
    ): Nothing = throw RuntimeFailure(reason, safeContext)

    private data class FoundationTarget(
        val registry: HimEntityIdRegistryEntry,
        val catalog: HimProductOnlyCanonical,
        val authority: HimCanonicalFamily,
    )

    private class RuntimeFailure(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1,
        val safeContext: String,
    ) : IllegalArgumentException()
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementReviewPacketBindingV1(
    val jsonFileBinding: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val markdownFileBinding: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val packetInputBindingDigest: String,
    val packetBindingDigest: String,
    val packetLogicalDigest: String,
    val packetImplementationHead: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1(
    val stableEntryId: String,
    val reviewUnitId: String,
    val canonicalEntityId: String,
    val source: HimGroundTruthSource,
    val recordKind: HimEvidenceRecordKind,
    val artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
    val sourceOriginArtifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
    val recordReference: String,
    val fields: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1>,
    val projectionLogicalDigest: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFoundationInputV1(
    val catalog: HimProductOnlyCanonicalMaster,
    val catalogBinding: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val registry: HimEntityIdRegistry,
    val registryBinding: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val authority: HimCanonicalFamilyAuthority,
    val authorityBinding: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeRequestV1(
    val enabled: Boolean,
    val outputRoot: File,
    val supplementImplementationHead: String,
    val mission: HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1,
    val reviewPacket: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1,
    val reviewPacketBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementReviewPacketBindingV1,
    val sourceProjectionInputs: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1>,
    val foundationInput: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFoundationInputV1,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeCountersV1(
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

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1 {
    INVALID_REQUEST,
    INVALID_IMPLEMENTATION_HEAD,
    INVALID_MISSION,
    INVALID_PACKET,
    INVALID_PACKET_BINDING,
    INVALID_FOUNDATION,
    MISSING_CANONICAL_TARGET,
    INVALID_CANONICAL_TARGET,
    INVALID_SOURCE_PROJECTION_COUNT,
    MISSING_SOURCE_PROJECTION,
    DUPLICATE_SOURCE_PROJECTION,
    UNEXPECTED_SOURCE_PROJECTION,
    INVALID_SOURCE_PROJECTION_BINDING,
    FORBIDDEN_SOURCE_ARTIFACT,
    INVALID_SOURCE_FIELD,
    SOURCE_FIELD_DIGEST_MISMATCH,
    SOURCE_PROJECTION_DIGEST_MISMATCH,
    INVALID_SOURCE_ORIGIN_BINDING,
    EVIDENCE_REFERENCE_BUILD_FAILED,
    EVIDENCE_REFERENCE_INVALID,
    INVALID_EVIDENCE_POSITION,
    INVALID_EVIDENCE_CARDINALITY,
    INVALID_COUNTERS,
    UNIT_BINDING_DIGEST_MISMATCH,
    SUPPLEMENT_BINDING_DIGEST_MISMATCH,
    SUPPLEMENT_LOGICAL_DIGEST_MISMATCH,
    SUPPLEMENT_VALIDATION_FAILED,
    PERSISTENCE_FAILED,
    RELOAD_FAILED,
    RELOAD_MISMATCH,
    BYTE_IDENTITY_MISMATCH,
    FORBIDDEN_DECISION_SEMANTICS,
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1 {
    data object Disabled : HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1

    data class Completed(
        val persistenceStatus: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1,
        val supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1,
        val counters: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCountersV1,
        val supplementBindingDigest: String,
        val supplementLogicalDigest: String,
        val jsonPath: String,
        val markdownPath: String,
        val jsonByteSize: Long,
        val markdownByteSize: Long,
        val jsonSha256: String,
        val markdownSha256: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1
}
