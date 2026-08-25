package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File

data class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeRequestV1(
    val enabled: Boolean,
    val mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
    val auditPlan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
    val auditAggregate: HimEvidenceAlignmentCatalogAuditAggregateV1,
    val auditShard: HimEvidenceAlignmentCatalogAuditShardResultV1,
    val catalog: HimProductOnlyCanonicalMaster,
    val authority: HimCanonicalFamilyAuthority,
    val fetchPorts: List<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentExactFetchPortV1>,
    val outputFile: File? = null,
)

interface HimEvidenceAlignmentCatalogAuditFindingsEnrichmentExactFetchPortV1 {
    val source: HimGroundTruthSource

    fun fetch(reference: HimEvidenceRecordReference): HimEvidenceRetrievalIndexRecord?
}

enum class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1 {
    MISSION_BINDING_MISMATCH,
    AGGREGATE_BINDING_MISMATCH,
    AUDIT_SHARD_BINDING_MISMATCH,
    SOURCE_BINDING_MISMATCH,
    EXACT_FETCH_NOT_FOUND,
    EXACT_FETCH_REFERENCE_MISMATCH,
    RECORD_KIND_MISMATCH,
    EXTRACTOR_FAILURE,
    ALIGNMENT_FAILURE,
    RECONSTRUCTED_CLASSIFICATION_MISMATCH,
    COUNTER_INVARIANT_FAILED,
    PERSISTENCE_FAILED,
    RELOAD_VALIDATION_FAILED,
    RESULT_ALREADY_EXISTS,
}

sealed interface HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult<out T> {
    data class Completed<T>(val value: T) : HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult<T>
    data class Skipped(val reason: String) : HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult<Nothing>
    data class Failed(val reason: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1, val safeContext: String) : HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult<Nothing>
}

object HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeV1 {
    val MISSION_BINDING_MISMATCH = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.MISSION_BINDING_MISMATCH
    val AGGREGATE_BINDING_MISMATCH = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.AGGREGATE_BINDING_MISMATCH
    val AUDIT_SHARD_BINDING_MISMATCH = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.AUDIT_SHARD_BINDING_MISMATCH
    val SOURCE_BINDING_MISMATCH = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.SOURCE_BINDING_MISMATCH
    val EXACT_FETCH_NOT_FOUND = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.EXACT_FETCH_NOT_FOUND
    val EXACT_FETCH_REFERENCE_MISMATCH = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.EXACT_FETCH_REFERENCE_MISMATCH
    val RECORD_KIND_MISMATCH = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.RECORD_KIND_MISMATCH
    val EXTRACTOR_FAILURE = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.EXTRACTOR_FAILURE
    val ALIGNMENT_FAILURE = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.ALIGNMENT_FAILURE
    val RECONSTRUCTED_CLASSIFICATION_MISMATCH = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.RECONSTRUCTED_CLASSIFICATION_MISMATCH
    val COUNTER_INVARIANT_FAILED = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.COUNTER_INVARIANT_FAILED
    val PERSISTENCE_FAILED = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.PERSISTENCE_FAILED
    val RELOAD_VALIDATION_FAILED = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.RELOAD_VALIDATION_FAILED
    val RESULT_ALREADY_EXISTS = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.RESULT_ALREADY_EXISTS

    fun executeShard(
        request: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeRequestV1,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1> {
        if (!request.enabled) return HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Skipped(
            "ENRICHMENT_OPT_IN_REQUIRED",
        )
        return try {
            validateRequest(request)
            val ports = request.fetchPorts.associateBy { it.source }
            val cache = linkedMapOf<String, HimEvidenceRetrievalIndexRecord>()
            var physicalFetches = 0
            val findings = request.auditShard.cells
                .sortedWith(compareBy({ request.auditPlan.canonicalOrder.indexOfFirst { c -> c.entityId == it.entityId } }, { SOURCE_ORDER.indexOf(it.source) }))
                .flatMap { cell ->
                    val canonical = request.auditPlan.canonicalOrder.singleOrNull { it.entityId == cell.entityId }
                        ?: fail(MISSION_BINDING_MISMATCH, request.auditShard.shardId, cell.source, cell.entityId, null)
                    val queryPlan = request.auditPlan.queryPlans.singleOrNull { it.entityId == cell.entityId && it.source == cell.source }
                        ?: fail(MISSION_BINDING_MISMATCH, request.auditShard.shardId, cell.source, cell.entityId, null)
                    cell.findings.map { finding ->
                        val occurrenceId = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.findingOccurrenceId(
                            request.auditShard.shardId,
                            finding.entityId,
                            finding.source,
                            finding.evidenceReference,
                        )
                        if (finding.evidenceReference == null) {
                            return@map HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1(
                                findingOccurrenceId = occurrenceId,
                                auditShardId = request.auditShard.shardId,
                                entityId = finding.entityId,
                                canonicalName = canonical.canonicalName,
                                normalizedName = canonical.normalizedName,
                                source = finding.source,
                                originalAuditClassification = finding.classification,
                                originalEvidenceReference = null,
                                originalPrimaryIdentity = finding.primaryIdentity,
                                queryPlanTerms = queryPlan.terms,
                                recordKind = null,
                                projectionSha256 = null,
                                candidateIdentities = emptyList(),
                                reconstructedPrimaryIdentity = null,
                                modifiers = emptyList(),
                                extractorResolution = null,
                                identityFieldsUsed = emptyList(),
                                extractionPath = null,
                                reconstructedAlignmentClassification = null,
                                reconstructedReasonCode = "NO_RETRIEVAL_HIT",
                                classificationComparison = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.NOT_RECONSTRUCTABLE,
                                reconstructionStatus = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.NO_RETRIEVAL_HIT,
                            )
                        }
                        val reference = try {
                            HimEvidenceRecordReference.parse(finding.source, finding.evidenceReference)
                        } catch (_: Throwable) {
                            fail(RECORD_KIND_MISMATCH, request.auditShard.shardId, finding.source, finding.entityId, finding.evidenceReference)
                        }
                        val cacheKey = "${finding.source.name}|${reference.value}"
                        val record = cache[cacheKey] ?: run {
                            val port = ports[finding.source]
                                ?: fail(SOURCE_BINDING_MISMATCH, request.auditShard.shardId, finding.source, finding.entityId, finding.evidenceReference)
                            physicalFetches++
                            val fetched = try { port.fetch(reference) } catch (_: Throwable) { null }
                                ?: fail(EXACT_FETCH_NOT_FOUND, request.auditShard.shardId, finding.source, finding.entityId, finding.evidenceReference)
                            cache[cacheKey] = fetched
                            fetched
                        }
                        if (record.sourceRecordReference != reference) {
                            fail(EXACT_FETCH_REFERENCE_MISMATCH, request.auditShard.shardId, finding.source, finding.entityId, finding.evidenceReference)
                        }
                        if (record.recordKind.source != finding.source) {
                            fail(RECORD_KIND_MISMATCH, request.auditShard.shardId, finding.source, finding.entityId, finding.evidenceReference)
                        }
                        val projectionSha = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(
                            record.evidenceProjection.deterministicJson,
                        )
                        if (record.recordKind !in SUPPORTED_KINDS) {
                            if (finding.classification != HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND) {
                                fail(RECONSTRUCTED_CLASSIFICATION_MISMATCH, request.auditShard.shardId, finding.source, finding.entityId, finding.evidenceReference)
                            }
                            return@map HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1(
                                findingOccurrenceId = occurrenceId,
                                auditShardId = request.auditShard.shardId,
                                entityId = finding.entityId,
                                canonicalName = canonical.canonicalName,
                                normalizedName = canonical.normalizedName,
                                source = finding.source,
                                originalAuditClassification = finding.classification,
                                originalEvidenceReference = finding.evidenceReference,
                                originalPrimaryIdentity = finding.primaryIdentity,
                                queryPlanTerms = queryPlan.terms,
                                recordKind = record.recordKind,
                                projectionSha256 = projectionSha,
                                candidateIdentities = emptyList(),
                                reconstructedPrimaryIdentity = null,
                                modifiers = emptyList(),
                                extractorResolution = null,
                                identityFieldsUsed = emptyList(),
                                extractionPath = "UNSUPPORTED_RECORD_KIND",
                                reconstructedAlignmentClassification = null,
                                reconstructedReasonCode = "UNSUPPORTED_RECORD_KIND",
                                classificationComparison = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.NOT_RECONSTRUCTABLE,
                                reconstructionStatus = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.UNSUPPORTED_RECORD_KIND,
                            )
                        }
                        val family = request.authority.families.singleOrNull { it.canonicalId.value == finding.entityId }
                            ?: fail(MISSION_BINDING_MISMATCH, request.auditShard.shardId, finding.source, finding.entityId, finding.evidenceReference)
                        val extracted = try { extract(record, request.catalog, request.authority) }
                            catch (_: Throwable) {
                                fail(EXTRACTOR_FAILURE, request.auditShard.shardId, finding.source, finding.entityId, finding.evidenceReference)
                            }
                        val evaluation = try {
                            HimDeterministicEvidenceAlignmentEvaluatorV1.align(extracted.alignment, family)
                        } catch (_: Throwable) {
                            fail(ALIGNMENT_FAILURE, request.auditShard.shardId, finding.source, finding.entityId, finding.evidenceReference)
                        }
                        val reconstructedClassification = evaluation.alignment.classification
                        val comparison = compareClassifications(finding.classification, reconstructedClassification)
                        if (comparison == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MISMATCH) {
                            fail(RECONSTRUCTED_CLASSIFICATION_MISMATCH, request.auditShard.shardId, finding.source, finding.entityId, finding.evidenceReference)
                        }
                        HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1(
                            findingOccurrenceId = occurrenceId,
                            auditShardId = request.auditShard.shardId,
                            entityId = finding.entityId,
                            canonicalName = canonical.canonicalName,
                            normalizedName = canonical.normalizedName,
                            source = finding.source,
                            originalAuditClassification = finding.classification,
                            originalEvidenceReference = finding.evidenceReference,
                            originalPrimaryIdentity = finding.primaryIdentity,
                            queryPlanTerms = queryPlan.terms,
                            recordKind = record.recordKind,
                            projectionSha256 = projectionSha,
                            candidateIdentities = extracted.candidateIdentities,
                            reconstructedPrimaryIdentity = extracted.primaryIdentity,
                            modifiers = extracted.modifiers,
                            extractorResolution = extracted.resolution,
                            identityFieldsUsed = extracted.identityFieldsUsed,
                            extractionPath = extracted.extractionPath,
                            reconstructedAlignmentClassification = reconstructedClassification,
                            reconstructedReasonCode = evaluation.rationale,
                            classificationComparison = comparison,
                            reconstructionStatus = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.RECONSTRUCTED,
                        )
                    }
                }
            val result = buildResult(request, findings, cache.size, physicalFetches)
            request.outputFile?.let { output ->
                try {
                    HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.writeShard(output, request.mission, result)
                } catch (failure: Throwable) {
                    val reason = if (failure.message == RESULT_ALREADY_EXISTS.name) RESULT_ALREADY_EXISTS else PERSISTENCE_FAILED
                    fail(reason, request.auditShard.shardId, null, null, null)
                }
                try {
                    HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Completed(
                        HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readShard(output, request.mission),
                    )
                } catch (_: Throwable) {
                    fail(RELOAD_VALIDATION_FAILED, request.auditShard.shardId, null, null, null)
                }
            } ?: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Completed(result)
        } catch (failure: EnrichmentFailure) {
            HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Failed(failure.reason, failure.context)
        } catch (_: Throwable) {
            HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Failed(
                COUNTER_INVARIANT_FAILED,
                safeContext(null, null, null, null),
            )
        }
    }

    private fun validateRequest(request: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeRequestV1) {
        request.mission.validate()
        request.auditPlan.validate()
        request.auditAggregate.validateAgainst(request.auditPlan)
        request.auditShard.validateAgainst(request.auditPlan)
        if (request.auditShard.missionDigest != request.mission.auditMissionDigest) {
            fail(AUDIT_SHARD_BINDING_MISMATCH, request.auditShard.shardId, null, null, null)
        }
        if (request.auditAggregate.logicalDigest != request.mission.provenance.auditAggregateLogicalDigest) {
            fail(AGGREGATE_BINDING_MISMATCH, request.auditShard.shardId, null, null, null)
        }
        if (request.fetchPorts.map { it.source } != SOURCE_ORDER || request.fetchPorts.map { it.source }.distinct().size != SOURCE_ORDER.size) {
            fail(SOURCE_BINDING_MISMATCH, request.auditShard.shardId, null, null, null)
        }
        if (request.auditAggregate.shardResults.singleOrNull { it.shardId == request.auditShard.shardId } != request.auditShard) {
            fail(AUDIT_SHARD_BINDING_MISMATCH, request.auditShard.shardId, null, null, null)
        }
    }

    private fun buildResult(
        request: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeRequestV1,
        findings: List<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1>,
        uniqueReferences: Int,
        exactFetches: Int,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1 {
        val shard = request.mission.shards.single { it.shardId == request.auditShard.shardId }
        val counters = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(
            canonicalsProcessed = shard.canonicalCount,
            sourcesProcessed = SOURCE_ORDER.size,
            findingOccurrences = findings.size,
            uniqueEvidenceReferences = uniqueReferences,
            exactFetches = exactFetches,
            noRetrievalHits = findings.count { it.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.NO_RETRIEVAL_HIT },
            unsupportedRecordKinds = findings.count { it.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.UNSUPPORTED_RECORD_KIND },
            reconstructedFindings = findings.count { it.reconstructionStatus != HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.NO_RETRIEVAL_HIT },
            classificationMismatches = findings.count {
                it.classificationComparison == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MISMATCH
            },
        )
        if (counters.classificationMismatches != 0) {
            fail(COUNTER_INVARIANT_FAILED, request.auditShard.shardId, null, null, null)
        }
        val unsigned = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1(
            missionDigest = request.mission.logicalDigest,
            shardId = request.auditShard.shardId,
            shardBindingDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.shardBindingDigest(request.mission, shard),
            state = HimEvidenceAlignmentCatalogAuditState.COMPLETE,
            findings = findings,
            counters = counters,
            logicalDigest = "",
        )
        return unsigned.copy(
            logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(unsigned),
        ).also { it.validateAgainst(request.mission) }
    }

    private data class Extracted(
        val primaryIdentity: String?,
        val modifiers: List<String>,
        val candidateIdentities: List<String>,
        val resolution: String,
        val identityFieldsUsed: List<String>,
        val extractionPath: String,
        val alignment: HimDeterministicEvidenceExtractionV1,
    )

    private fun extract(
        record: HimEvidenceRetrievalIndexRecord,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ): Extracted = when (record.recordKind) {
        HimEvidenceRecordKind.OFF_PRODUCT -> HimOffDeterministicPrimaryIdentityExtractorV1.extract(record, catalog, authority).let {
            Extracted(it.primaryIdentity, it.modifiers, it.candidateIdentities, it.resolution.name, listOfNotNull(it.identityFieldUsed), it.extractionPath, extraction(record, it.toAlignmentInput(), it.primaryIdentity, it.modifiers, it.candidateIdentities, it.extractionPath))
        }
        HimEvidenceRecordKind.AGRIBALYSE_RECORD -> HimAgribalyseDeterministicPrimaryIdentityExtractorV1.extract(record, catalog, authority).let {
            Extracted(it.primaryIdentity, it.modifiers, it.candidateIdentities, it.resolution.name, it.identityFieldsUsed, it.extractionPath, extraction(record, it.toAlignmentInput(), it.primaryIdentity, it.modifiers, it.candidateIdentities, it.extractionPath))
        }
        HimEvidenceRecordKind.CIQUAL_FOOD -> HimCiqualDeterministicPrimaryIdentityExtractorV1.extract(record, catalog, authority).let {
            Extracted(it.primaryIdentity, it.modifiers, it.candidateIdentities, it.resolution.name, it.identityFieldsUsed, it.extractionPath, extraction(record, it.toAlignmentInput(), it.primaryIdentity, it.modifiers, it.candidateIdentities, it.extractionPath))
        }
        HimEvidenceRecordKind.GI_MEASUREMENT -> HimGlycemicIndexDeterministicPrimaryIdentityExtractorV1.extract(record, catalog, authority).let {
            Extracted(it.primaryIdentity, it.modifiers, it.candidateIdentities, it.resolution.name, it.identityFieldsUsed, it.extractionPath, extraction(record, it.toAlignmentInput(), it.primaryIdentity, it.modifiers, it.candidateIdentities, it.extractionPath))
        }
        else -> throw IllegalArgumentException("Unsupported record kind")
    }

    private fun extraction(
        record: HimEvidenceRetrievalIndexRecord,
        input: HimEvidenceAlignmentInputV1,
        primaryIdentity: String?,
        modifiers: List<String>,
        candidates: List<String>,
        path: String,
    ) = HimDeterministicEvidenceExtractionV1(
        source = record.sourceRecordReference.source,
        recordKind = record.recordKind,
        sourceRecordIdentity = record.sourceRecordReference.value,
        primaryIdentity = primaryIdentity,
        modifiers = modifiers,
        candidateIdentities = candidates,
        extractionPath = path,
        alignmentInput = input,
        failureReason = null,
    )

    internal fun compareClassifications(
        frozen: HimEvidenceAlignmentAuditClassification,
        reconstructed: HimEvidenceAlignmentClassificationV1,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1 = when (frozen) {
        HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED ->
            if (alignmentSupportsDirect(reconstructed)) {
                HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH
            } else {
                HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MISMATCH
            }
        HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED ->
            if (reconstructed == HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY) {
                HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH
            } else {
                HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MISMATCH
            }
        HimEvidenceAlignmentAuditClassification.MODIFIER_ONLY ->
            if (reconstructed == HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER) {
                HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH
            } else {
                HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MISMATCH
            }
        HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY ->
            if (reconstructed == HimEvidenceAlignmentClassificationV1.MISSING_PRIMARY_IDENTITY ||
                reconstructed == HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY
            ) {
                HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH
            } else {
                HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MISMATCH
            }
        HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT,
        HimEvidenceAlignmentAuditClassification.INVALID_PROJECTION,
        HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND,
        HimEvidenceAlignmentAuditClassification.SOURCE_OR_BINDING_FAILURE,
        -> HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.NOT_RECONSTRUCTABLE
    }

    private fun alignmentSupportsDirect(value: HimEvidenceAlignmentClassificationV1): Boolean = when (value) {
        HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH,
        HimEvidenceAlignmentClassificationV1.PRIMARY_AUTHORITY_BOUND_MATCH,
        -> true
        HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER,
        HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY,
        HimEvidenceAlignmentClassificationV1.MISSING_PRIMARY_IDENTITY,
        HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY,
        -> false
    }

    private fun safeContext(
        shardId: String?,
        source: HimGroundTruthSource?,
        entityId: String?,
        evidenceReference: String?,
    ) = listOfNotNull(
        shardId?.let { "shard=$it" },
        source?.let { "source=${it.name}" },
        entityId?.let { "entity=$it" },
        evidenceReference?.let { "reference=$it" },
    ).joinToString(" ").ifBlank { "enrichment" }

    private fun safeFailure(
        reason: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1,
        shardId: String?,
        source: HimGroundTruthSource?,
        entityId: String?,
        reference: String?,
    ): IllegalArgumentException = IllegalArgumentException("${reason.name} ${safeContext(shardId, source, entityId, reference)}")

    private fun fail(
        reason: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1,
        shardId: String?,
        source: HimGroundTruthSource?,
        entityId: String?,
        reference: String?,
    ): Nothing = throw EnrichmentFailure(reason, safeContext(shardId, source, entityId, reference))

    private class EnrichmentFailure(
        val reason: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1,
        val context: String,
    ) : RuntimeException()

    private val SOURCE_ORDER = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER
    private val SUPPORTED_KINDS = setOf(
        HimEvidenceRecordKind.OFF_PRODUCT,
        HimEvidenceRecordKind.AGRIBALYSE_RECORD,
        HimEvidenceRecordKind.CIQUAL_FOOD,
        HimEvidenceRecordKind.GI_MEASUREMENT,
    )
}
