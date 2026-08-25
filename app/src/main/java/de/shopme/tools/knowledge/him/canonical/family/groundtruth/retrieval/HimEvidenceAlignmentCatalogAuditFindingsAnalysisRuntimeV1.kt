package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

data class HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1(
    val mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
    val aggregate: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1,
    val inputBinding: HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1,
)

enum class HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1 {
    INPUT_BINDING_MISMATCH,
    MISSION_VALIDATION_FAILED,
    AGGREGATE_VALIDATION_FAILED,
    MISSION_BINDING_MISMATCH,
    AGGREGATE_BINDING_MISMATCH,
    SHARD_SET_INCOMPLETE,
    DUPLICATE_FINDING_OCCURRENCE,
    COUNTER_INVARIANT_FAILED,
    BREAKDOWN_INVARIANT_FAILED,
    CLASSIFICATION_MISMATCH_PRESENT,
    UNSUPPORTED_ANALYSIS_VALUE,
    PERSISTENCE_WRITE_FAILED,
    PERSISTENCE_RELOAD_FAILED,
    EXISTING_ARTIFACT_MISMATCH,
}

sealed interface HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult<out T> {
    data class Completed<T>(val value: T) : HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult<T>
    data class Failed(val reason: HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1, val safeContext: String) :
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult<Nothing>
}

object HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeV1 {
    fun analyze(
        request: HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1,
    ): HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult<HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1> = try {
        validateInput(request)
        val findings = orderedFindings(request.mission, request.aggregate)
        val report = buildReport(request, findings)
        report.validate()
        validateDerivedReport(report)
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Completed(report)
    } catch (failure: AnalysisFailure) {
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Failed(failure.reason, failure.safeContext)
    } catch (_: IllegalArgumentException) {
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Failed(
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.COUNTER_INVARIANT_FAILED,
            "analysis",
        )
    }

    private fun validateInput(request: HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1) {
        try {
            request.mission.validate()
        } catch (_: IllegalArgumentException) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.MISSION_VALIDATION_FAILED, "mission")
        }
        try {
            request.aggregate.validateAgainst(request.mission)
        } catch (_: IllegalArgumentException) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.AGGREGATE_VALIDATION_FAILED, "aggregate")
        }
        request.inputBinding.validate()
        if (request.inputBinding.enrichmentMissionLogicalDigest != request.mission.logicalDigest ||
            request.inputBinding.enrichmentAggregateLogicalDigest != request.aggregate.logicalDigest
        ) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.INPUT_BINDING_MISMATCH, "input")
        }
        if (request.inputBinding.auditHead != request.mission.provenance.auditHead ||
            request.inputBinding.enrichmentImplementationHead != request.mission.provenance.enrichmentImplementationHead ||
            request.inputBinding.canonicalOrderDigest != request.mission.auditCanonicalOrderDigest ||
            request.inputBinding.expectedCanonicalCount != request.mission.expectedCanonicalCount ||
            request.inputBinding.expectedShardIds != request.mission.shards.map { it.shardId }
        ) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.MISSION_BINDING_MISMATCH, "mission")
        }
        if (request.aggregate.missionDigest != request.mission.logicalDigest) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.AGGREGATE_BINDING_MISMATCH, "aggregate")
        }
        val aggregateShardIds = request.aggregate.shardResults.map { it.shardId }
        if (request.aggregate.state != HimEvidenceAlignmentCatalogAuditState.COMPLETE ||
            aggregateShardIds.distinct().size != aggregateShardIds.size ||
            aggregateShardIds.toSet() != request.mission.shards.map { it.shardId }.toSet()
        ) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.SHARD_SET_INCOMPLETE, "shards")
        }
        val shardCounters = request.aggregate.shardResults.map { it.counters }
        val expectedCounters = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(
            canonicalsProcessed = shardCounters.sumOf { it.canonicalsProcessed },
            sourcesProcessed = shardCounters.sumOf { it.sourcesProcessed },
            findingOccurrences = shardCounters.sumOf { it.findingOccurrences },
            uniqueEvidenceReferences = shardCounters.sumOf { it.uniqueEvidenceReferences },
            exactFetches = shardCounters.sumOf { it.exactFetches },
            noRetrievalHits = shardCounters.sumOf { it.noRetrievalHits },
            unsupportedRecordKinds = shardCounters.sumOf { it.unsupportedRecordKinds },
            reconstructedFindings = shardCounters.sumOf { it.reconstructedFindings },
            classificationMismatches = shardCounters.sumOf { it.classificationMismatches },
        )
        if (request.aggregate.counters != expectedCounters) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.COUNTER_INVARIANT_FAILED, "aggregateCounters")
        }
        if (request.aggregate.counters.classificationMismatches != 0 ||
            request.aggregate.shardResults.flatMap { it.findings }.any {
                it.classificationComparison == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MISMATCH
            }
        ) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.CLASSIFICATION_MISMATCH_PRESENT, "aggregate")
        }
    }

    private fun orderedFindings(
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
        aggregate: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1,
    ): List<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1> {
        val shardOrder = mission.shards.mapIndexed { index, shard -> shard.shardId to index }.toMap()
        val canonicalOrder = mission.shards.flatMap { it.canonicalEntityIds }
            .mapIndexed { index, entityId -> entityId to index }.toMap()
        val findings = aggregate.shardResults.flatMap { it.findings }
        val unique = findings.map { it.findingOccurrenceId }.distinct()
        if (unique.size != findings.size) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.DUPLICATE_FINDING_OCCURRENCE, "findings")
        }
        return findings.sortedWith(compareBy(
            { shardOrder[it.auditShardId] ?: Int.MAX_VALUE },
            { canonicalOrder[it.entityId] ?: Int.MAX_VALUE },
            { HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.SOURCE_ORDER.indexOf(it.source) },
            { it.originalEvidenceReference ?: HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.NOT_AVAILABLE },
            { it.findingOccurrenceId },
        ))
    }

    private fun buildReport(
        request: HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1,
        findings: List<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1>,
    ): HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1 {
        val index = findings.map(::indexEntry)
        val primary = index.groupingBy { HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.primaryOutcome(it.originalAuditClassification) }
            .eachCount()
            .toSortedMap()
            .map { (outcome, count) -> HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeCountV1(outcome, count) }
        val breakdowns = buildBreakdowns(index)
        val flags = HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.values().map { flag ->
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagCountV1(flag, index.count { hasFlag(it.flags, flag) })
        }
        val unsigned = HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1(
            contractId = HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.VERSION,
            inputBinding = request.inputBinding,
            counters = request.aggregate.counters,
            totalFindingOccurrences = index.size,
            primaryOutcomeBuckets = primary,
            breakdowns = breakdowns,
            diagnosticFlags = flags,
            findingIndex = index,
            logicalDigest = "",
        )
        return unsigned.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.logicalDigest(unsigned))
    }

    private fun indexEntry(
        finding: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1,
    ) = HimEvidenceAlignmentCatalogAuditFindingsAnalysisFindingIndexEntryV1(
        findingOccurrenceId = finding.findingOccurrenceId,
        auditShardId = finding.auditShardId,
        entityId = finding.entityId,
        canonicalName = finding.canonicalName,
        normalizedName = finding.normalizedName,
        source = finding.source,
        originalAuditClassification = finding.originalAuditClassification,
        originalEvidenceReference = finding.originalEvidenceReference,
        originalPrimaryIdentity = finding.originalPrimaryIdentity,
        originalClaimedEvidenceRelation = finding.originalClaimedEvidenceRelation,
        originalReasonPersistenceState = finding.originalReasonPersistenceState,
        queryPlanTerms = finding.queryPlanTerms,
        queryTermFindingState = finding.queryTermFindingState,
        recordKind = finding.recordKind,
        projectionSha256 = finding.projectionSha256,
        candidateIdentities = finding.candidateIdentities,
        reconstructedPrimaryIdentity = finding.reconstructedPrimaryIdentity,
        modifiers = finding.modifiers,
        extractorResolution = finding.extractorResolution,
        identityFieldsUsed = finding.identityFieldsUsed,
        extractionPath = finding.extractionPath,
        reconstructedAlignmentClassification = finding.reconstructedAlignmentClassification,
        reconstructedReasonCode = finding.reconstructedReasonCode,
        classificationComparison = finding.classificationComparison,
        reconstructionStatus = finding.reconstructionStatus,
        flags = HimEvidenceAlignmentCatalogAuditFindingsAnalysisFlagsV1(
            hasOriginalPrimaryIdentity = finding.originalPrimaryIdentity != null,
            hasReconstructedPrimaryIdentity = finding.reconstructedPrimaryIdentity != null,
            hasModifiers = finding.modifiers.isNotEmpty(),
            hasCandidateIdentities = finding.candidateIdentities.isNotEmpty(),
            hasIdentityFieldsUsed = finding.identityFieldsUsed.isNotEmpty(),
            hasExactEvidenceReference = finding.originalEvidenceReference != null,
            hasProjection = finding.projectionSha256 != null,
            hasReconstructedAlignment = finding.reconstructedAlignmentClassification != null,
            hasReconstructedReasonCode = finding.reconstructedReasonCode != null,
            extractorResolved = extractorResolution(finding.extractorResolution) == HimEvidenceAlignmentCatalogAuditFindingsAnalysisExtractorResolutionV1.RESOLVED,
            extractorUnresolved = extractorResolution(finding.extractorResolution).let {
                it == HimEvidenceAlignmentCatalogAuditFindingsAnalysisExtractorResolutionV1.MISSING ||
                    it == HimEvidenceAlignmentCatalogAuditFindingsAnalysisExtractorResolutionV1.UNRESOLVED
            },
            reconstructable = finding.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.RECONSTRUCTED,
            notReconstructable = finding.reconstructionStatus != HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.RECONSTRUCTED,
            alignmentComparable = finding.classificationComparison != HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.NOT_RECONSTRUCTABLE,
            alignmentNotComparable = finding.classificationComparison == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.NOT_RECONSTRUCTABLE,
            classificationMatch = finding.classificationComparison == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH,
            classificationMismatch = finding.classificationComparison == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MISMATCH,
            noRetrievalHit = finding.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.NO_RETRIEVAL_HIT,
            unsupportedRecordKind = finding.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.UNSUPPORTED_RECORD_KIND,
        ),
    )

    private fun buildBreakdowns(
        entries: List<HimEvidenceAlignmentCatalogAuditFindingsAnalysisFindingIndexEntryV1>,
    ): List<HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownRowV1> {
        val counts = mutableMapOf<Pair<HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1, String>, Int>()
        fun add(axis: HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1, key: String) {
            counts[axis to key] = (counts[axis to key] ?: 0) + 1
        }
        entries.forEach { finding ->
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.SOURCE, finding.source.name)
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.RECORD_KIND, finding.recordKind?.name ?: NOT_AVAILABLE)
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.ORIGINAL_AUDIT_CLASSIFICATION, finding.originalAuditClassification.name)
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.RECONSTRUCTION_STATUS, finding.reconstructionStatus.name)
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.CLASSIFICATION_COMPARISON, finding.classificationComparison.name)
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.RECONSTRUCTED_ALIGNMENT_CLASSIFICATION, finding.reconstructedAlignmentClassification?.name ?: NOT_AVAILABLE)
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.EXTRACTOR_RESOLUTION, extractorResolution(finding.extractorResolution).name)
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.EXTRACTION_PATH, finding.extractionPath ?: NOT_AVAILABLE)
            if (finding.identityFieldsUsed.isEmpty()) add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.IDENTITY_FIELD_USED, NOT_AVAILABLE)
            else finding.identityFieldsUsed.forEach { add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.IDENTITY_FIELD_USED, it) }
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.ORIGINAL_PRIMARY_IDENTITY, presence(finding.originalPrimaryIdentity))
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.RECONSTRUCTED_PRIMARY_IDENTITY, presence(finding.reconstructedPrimaryIdentity))
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.ORIGINAL_EVIDENCE_REFERENCE, presence(finding.originalEvidenceReference))
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.PROJECTION, presence(finding.projectionSha256))
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.RECONSTRUCTED_REASON_CODE, presence(finding.reconstructedReasonCode))
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.CANDIDATE_COUNT, countBucket(finding.candidateIdentities.size))
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.MODIFIER_COUNT, countBucket(finding.modifiers.size))
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.AUDIT_SHARD, finding.auditShardId)
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.SOURCE_X_ORIGINAL_AUDIT_CLASSIFICATION, crossKey(finding.source, finding.originalAuditClassification.name))
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.SOURCE_X_RECORD_KIND, crossKey(finding.source, finding.recordKind?.name ?: NOT_AVAILABLE))
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.SOURCE_X_RECONSTRUCTION_STATUS, crossKey(finding.source, finding.reconstructionStatus.name))
            add(HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.SOURCE_X_RECONSTRUCTED_ALIGNMENT_CLASSIFICATION, crossKey(finding.source, finding.reconstructedAlignmentClassification?.name ?: NOT_AVAILABLE))
        }
        return counts.entries.sortedWith(compareBy({ it.key.first.ordinal }, { it.key.second }))
            .map { HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownRowV1(it.key.first, it.key.second, it.value) }
    }

    private fun validateDerivedReport(report: HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1) {
        val expectedFindings = report.findingIndex.size
        val shardLocalUniqueEvidenceReferences = report.findingIndex
            .groupBy { it.auditShardId }
            .values
            .sumOf { shardFindings ->
                shardFindings
                    .mapNotNull { it.originalEvidenceReference }
                    .distinct()
                    .size
            }
        if (report.totalFindingOccurrences != expectedFindings ||
            report.counters.findingOccurrences != expectedFindings ||
            report.counters.uniqueEvidenceReferences != shardLocalUniqueEvidenceReferences ||
            report.counters.noRetrievalHits != report.findingIndex.count {
                it.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.NO_RETRIEVAL_HIT
            } ||
            report.counters.unsupportedRecordKinds != report.findingIndex.count {
                it.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.UNSUPPORTED_RECORD_KIND
            } ||
            report.counters.reconstructedFindings != report.findingIndex.count {
                it.reconstructionStatus != HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.NO_RETRIEVAL_HIT
            }
        ) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.COUNTER_INVARIANT_FAILED, "counters")
        }
        if (report.primaryOutcomeBuckets.sumOf { it.count } != expectedFindings ||
            report.primaryOutcomeBuckets.any { bucket ->
                bucket.count != report.findingIndex.count {
                    HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.primaryOutcome(it.originalAuditClassification) == bucket.outcome
                }
            }
        ) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.BREAKDOWN_INVARIANT_FAILED, "primaryOutcomes")
        }
        val recomputed = buildBreakdowns(report.findingIndex)
        if (recomputed != report.breakdowns) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.BREAKDOWN_INVARIANT_FAILED, "breakdowns")
        }
        val baseAxes = HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.values()
            .filterNot { it == HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.IDENTITY_FIELD_USED }
        if (baseAxes.any { axis -> report.breakdowns.filter { it.axis == axis }.sumOf { it.count } != expectedFindings }) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.BREAKDOWN_INVARIANT_FAILED, "breakdownSums")
        }
        val identityFieldBase = report.findingIndex.sumOf { it.identityFieldsUsed.ifEmpty { listOf(NOT_AVAILABLE) }.size }
        if (report.breakdowns.filter { it.axis == HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1.IDENTITY_FIELD_USED }
                .sumOf { it.count } != identityFieldBase
        ) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.BREAKDOWN_INVARIANT_FAILED, "identityFieldSums")
        }
        if (report.diagnosticFlags != HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.values().map { flag ->
                HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagCountV1(flag, report.findingIndex.count { hasFlag(it.flags, flag) })
            }) {
            fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.BREAKDOWN_INVARIANT_FAILED, "flags")
        }
    }

    private fun hasFlag(flags: HimEvidenceAlignmentCatalogAuditFindingsAnalysisFlagsV1, flag: HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1): Boolean = when (flag) {
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.HAS_ORIGINAL_PRIMARY_IDENTITY -> flags.hasOriginalPrimaryIdentity
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.HAS_RECONSTRUCTED_PRIMARY_IDENTITY -> flags.hasReconstructedPrimaryIdentity
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.HAS_MODIFIERS -> flags.hasModifiers
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.HAS_CANDIDATE_IDENTITIES -> flags.hasCandidateIdentities
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.HAS_IDENTITY_FIELDS_USED -> flags.hasIdentityFieldsUsed
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.HAS_EXACT_EVIDENCE_REFERENCE -> flags.hasExactEvidenceReference
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.HAS_PROJECTION -> flags.hasProjection
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.HAS_RECONSTRUCTED_ALIGNMENT -> flags.hasReconstructedAlignment
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.HAS_RECONSTRUCTED_REASON_CODE -> flags.hasReconstructedReasonCode
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.ORIGINAL_PRIMARY_IDENTITY_MISSING -> !flags.hasOriginalPrimaryIdentity
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.EXTRACTOR_RESOLVED -> flags.extractorResolved
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.EXTRACTOR_UNRESOLVED -> flags.extractorUnresolved
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.RECONSTRUCTABLE -> flags.reconstructable
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.NOT_RECONSTRUCTABLE -> flags.notReconstructable
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.ALIGNMENT_COMPARABLE -> flags.alignmentComparable
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.ALIGNMENT_NOT_COMPARABLE -> flags.alignmentNotComparable
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.CLASSIFICATION_MATCH -> flags.classificationMatch
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.CLASSIFICATION_MISMATCH -> flags.classificationMismatch
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.NO_RETRIEVAL_HIT -> flags.noRetrievalHit
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.UNSUPPORTED_RECORD_KIND -> flags.unsupportedRecordKind
    }

    private fun presence(value: String?): String = if (value == null) {
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisPresenceV1.NOT_AVAILABLE.name
    } else {
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisPresenceV1.PRESENT.name
    }

    private fun crossKey(source: de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource, value: String): String =
        "${source.name}|$value"

    private fun extractorResolution(value: String?): HimEvidenceAlignmentCatalogAuditFindingsAnalysisExtractorResolutionV1 = when (value) {
        null -> HimEvidenceAlignmentCatalogAuditFindingsAnalysisExtractorResolutionV1.NOT_AVAILABLE
        "RESOLVED" -> HimEvidenceAlignmentCatalogAuditFindingsAnalysisExtractorResolutionV1.RESOLVED
        "MISSING" -> HimEvidenceAlignmentCatalogAuditFindingsAnalysisExtractorResolutionV1.MISSING
        "UNRESOLVED" -> HimEvidenceAlignmentCatalogAuditFindingsAnalysisExtractorResolutionV1.UNRESOLVED
        else -> fail(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.UNSUPPORTED_ANALYSIS_VALUE, "extractorResolution")
    }

    private fun countBucket(value: Int): String = when (value) {
        0 -> HimEvidenceAlignmentCatalogAuditFindingsAnalysisCountBucketV1.ZERO.name
        1 -> HimEvidenceAlignmentCatalogAuditFindingsAnalysisCountBucketV1.ONE.name
        else -> HimEvidenceAlignmentCatalogAuditFindingsAnalysisCountBucketV1.TWO_OR_MORE.name
    }

    private fun fail(
        reason: HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1,
        context: String,
    ): Nothing = throw AnalysisFailure(reason, context)

    private class AnalysisFailure(
        val reason: HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1,
        val safeContext: String,
    ) : RuntimeException()

    private const val NOT_AVAILABLE = HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.NOT_AVAILABLE
}
