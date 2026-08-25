package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource

object HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1 {
    const val VERSION = "HIM_EVIDENCE_ALIGNMENT_CATALOG_AUDIT_FINDINGS_ANALYSIS_V1"
    const val NOT_AVAILABLE = "NOT_AVAILABLE"

    val SOURCE_ORDER = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER

    fun primaryOutcome(
        value: HimEvidenceAlignmentAuditClassification,
    ): HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1 = when (value) {
        HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED ->
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.DIRECT_SUPPORTED
        HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED ->
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.DIRECT_REJECTED
        HimEvidenceAlignmentAuditClassification.MODIFIER_ONLY ->
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.MODIFIER_ONLY
        HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY ->
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.UNRESOLVED_PRIMARY_IDENTITY
        HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT ->
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.NO_RETRIEVAL_HIT
        HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND ->
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.UNSUPPORTED_RECORD_KIND
        HimEvidenceAlignmentAuditClassification.INVALID_PROJECTION ->
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.INVALID_PROJECTION
        HimEvidenceAlignmentAuditClassification.SOURCE_OR_BINDING_FAILURE ->
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.SOURCE_OR_BINDING_FAILURE
    }
}

enum class HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1 {
    DIRECT_SUPPORTED,
    DIRECT_REJECTED,
    MODIFIER_ONLY,
    UNRESOLVED_PRIMARY_IDENTITY,
    NO_RETRIEVAL_HIT,
    UNSUPPORTED_RECORD_KIND,
    INVALID_PROJECTION,
    SOURCE_OR_BINDING_FAILURE,
}

enum class HimEvidenceAlignmentCatalogAuditFindingsAnalysisCountBucketV1 {
    ZERO,
    ONE,
    TWO_OR_MORE,
}

enum class HimEvidenceAlignmentCatalogAuditFindingsAnalysisPresenceV1 {
    PRESENT,
    NOT_AVAILABLE,
}

enum class HimEvidenceAlignmentCatalogAuditFindingsAnalysisExtractorResolutionV1 {
    RESOLVED,
    MISSING,
    UNRESOLVED,
    NOT_AVAILABLE,
}

enum class HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1 {
    SOURCE,
    RECORD_KIND,
    ORIGINAL_AUDIT_CLASSIFICATION,
    RECONSTRUCTION_STATUS,
    CLASSIFICATION_COMPARISON,
    RECONSTRUCTED_ALIGNMENT_CLASSIFICATION,
    EXTRACTOR_RESOLUTION,
    EXTRACTION_PATH,
    IDENTITY_FIELD_USED,
    ORIGINAL_PRIMARY_IDENTITY,
    RECONSTRUCTED_PRIMARY_IDENTITY,
    ORIGINAL_EVIDENCE_REFERENCE,
    PROJECTION,
    RECONSTRUCTED_REASON_CODE,
    CANDIDATE_COUNT,
    MODIFIER_COUNT,
    AUDIT_SHARD,
    SOURCE_X_ORIGINAL_AUDIT_CLASSIFICATION,
    SOURCE_X_RECORD_KIND,
    SOURCE_X_RECONSTRUCTION_STATUS,
    SOURCE_X_RECONSTRUCTED_ALIGNMENT_CLASSIFICATION,
}

enum class HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1 {
    HAS_ORIGINAL_PRIMARY_IDENTITY,
    HAS_RECONSTRUCTED_PRIMARY_IDENTITY,
    HAS_MODIFIERS,
    HAS_CANDIDATE_IDENTITIES,
    HAS_IDENTITY_FIELDS_USED,
    HAS_EXACT_EVIDENCE_REFERENCE,
    HAS_PROJECTION,
    HAS_RECONSTRUCTED_ALIGNMENT,
    HAS_RECONSTRUCTED_REASON_CODE,
    ORIGINAL_PRIMARY_IDENTITY_MISSING,
    EXTRACTOR_RESOLVED,
    EXTRACTOR_UNRESOLVED,
    RECONSTRUCTABLE,
    NOT_RECONSTRUCTABLE,
    ALIGNMENT_COMPARABLE,
    ALIGNMENT_NOT_COMPARABLE,
    CLASSIFICATION_MATCH,
    CLASSIFICATION_MISMATCH,
    NO_RETRIEVAL_HIT,
    UNSUPPORTED_RECORD_KIND,
}

data class HimEvidenceAlignmentCatalogAuditFindingsAnalysisFileBindingV1(
    val relativePath: String,
    val byteSize: Long,
    val sha256: String,
    val logicalDigest: String,
) {
    init {
        require(relativePath.isNotBlank() && !relativePath.startsWith('/') && !relativePath.contains('\\'))
        require(relativePath.split('/').none { it.isBlank() || it == "." || it == ".." })
        require(byteSize >= 0)
        require(sha256.matches(SHA256))
        require(logicalDigest.matches(SHA256))
    }
}

data class HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1(
    val contractId: String,
    val auditHead: String,
    val enrichmentImplementationHead: String,
    val analysisImplementationHead: String,
    val enrichmentMission: HimEvidenceAlignmentCatalogAuditFindingsAnalysisFileBindingV1,
    val enrichmentAggregate: HimEvidenceAlignmentCatalogAuditFindingsAnalysisFileBindingV1,
    val enrichmentMissionLogicalDigest: String,
    val enrichmentAggregateLogicalDigest: String,
    val canonicalOrderDigest: String,
    val expectedCanonicalCount: Int,
    val expectedShardIds: List<String>,
    val bindingDigest: String,
) {
    fun validate() {
        require(contractId == HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.VERSION)
        require(auditHead.matches(HEAD))
        require(enrichmentImplementationHead.matches(HEAD))
        require(analysisImplementationHead.matches(HEAD))
        require(enrichmentMissionLogicalDigest.matches(SHA256))
        require(enrichmentAggregateLogicalDigest.matches(SHA256))
        require(enrichmentMission.logicalDigest == enrichmentMissionLogicalDigest)
        require(enrichmentAggregate.logicalDigest == enrichmentAggregateLogicalDigest)
        require(canonicalOrderDigest.matches(SHA256))
        require(expectedCanonicalCount > 0)
        require(expectedShardIds == expectedShardIds.distinct())
        require(expectedShardIds.all { it.matches(SHARD) })
        require(bindingDigest.matches(SHA256))
        require(bindingDigest == HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.bindingDigest(copy(bindingDigest = "")))
    }
}

data class HimEvidenceAlignmentCatalogAuditFindingsAnalysisFlagsV1(
    val hasOriginalPrimaryIdentity: Boolean,
    val hasReconstructedPrimaryIdentity: Boolean,
    val hasModifiers: Boolean,
    val hasCandidateIdentities: Boolean,
    val hasIdentityFieldsUsed: Boolean,
    val hasExactEvidenceReference: Boolean,
    val hasProjection: Boolean,
    val hasReconstructedAlignment: Boolean,
    val hasReconstructedReasonCode: Boolean,
    val extractorResolved: Boolean,
    val extractorUnresolved: Boolean,
    val reconstructable: Boolean,
    val notReconstructable: Boolean,
    val alignmentComparable: Boolean,
    val alignmentNotComparable: Boolean,
    val classificationMatch: Boolean,
    val classificationMismatch: Boolean,
    val noRetrievalHit: Boolean,
    val unsupportedRecordKind: Boolean,
)

data class HimEvidenceAlignmentCatalogAuditFindingsAnalysisFindingIndexEntryV1(
    val findingOccurrenceId: String,
    val auditShardId: String,
    val entityId: String,
    val canonicalName: String,
    val normalizedName: String,
    val source: HimGroundTruthSource,
    val originalAuditClassification: HimEvidenceAlignmentAuditClassification,
    val originalEvidenceReference: String?,
    val originalPrimaryIdentity: String?,
    val originalClaimedEvidenceRelation: String?,
    val originalReasonPersistenceState: String,
    val queryPlanTerms: List<HimEvidenceAlignmentCatalogAuditQueryTermV1>,
    val queryTermFindingState: String,
    val recordKind: HimEvidenceRecordKind?,
    val projectionSha256: String?,
    val candidateIdentities: List<String>,
    val reconstructedPrimaryIdentity: String?,
    val modifiers: List<String>,
    val extractorResolution: String?,
    val identityFieldsUsed: List<String>,
    val extractionPath: String?,
    val reconstructedAlignmentClassification: HimEvidenceAlignmentClassificationV1?,
    val reconstructedReasonCode: String?,
    val classificationComparison: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1,
    val reconstructionStatus: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus,
    val flags: HimEvidenceAlignmentCatalogAuditFindingsAnalysisFlagsV1,
) {
    init {
        require(findingOccurrenceId.matches(SHA256))
        require(auditShardId.matches(SHARD))
        require(entityId.matches(ENTITY))
        require(canonicalName.isNotBlank() && normalizedName.isNotBlank())
        require(originalEvidenceReference == null || originalEvidenceReference.isNotBlank())
        require(originalReasonPersistenceState == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.NOT_PERSISTED_IN_AUDIT_V1)
        require(queryPlanTerms.isNotEmpty())
        require(queryTermFindingState == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.NOT_PERSISTED_IN_AUDIT_V1)
        require(projectionSha256 == null || projectionSha256.matches(SHA256))
        require(candidateIdentities.all(String::isNotBlank))
        require(modifiers.all(String::isNotBlank))
        require(identityFieldsUsed.all(String::isNotBlank))
    }
}

data class HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeCountV1(
    val outcome: HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1,
    val count: Int,
) {
    init { require(count > 0) }
}

data class HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownRowV1(
    val axis: HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownAxisV1,
    val key: String,
    val count: Int,
) {
    init {
        require(key.isNotBlank())
        require(count > 0)
    }
}

data class HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagCountV1(
    val flag: HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1,
    val count: Int,
) {
    init { require(count >= 0) }
}

data class HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1(
    val contractId: String,
    val inputBinding: HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1,
    val counters: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1,
    val totalFindingOccurrences: Int,
    val primaryOutcomeBuckets: List<HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeCountV1>,
    val breakdowns: List<HimEvidenceAlignmentCatalogAuditFindingsAnalysisBreakdownRowV1>,
    val diagnosticFlags: List<HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagCountV1>,
    val findingIndex: List<HimEvidenceAlignmentCatalogAuditFindingsAnalysisFindingIndexEntryV1>,
    val logicalDigest: String,
) {
    fun validate() {
        require(contractId == HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.VERSION)
        inputBinding.validate()
        require(totalFindingOccurrences == findingIndex.size)
        require(totalFindingOccurrences >= 0)
        require(findingIndex.map { it.findingOccurrenceId }.distinct().size == findingIndex.size)
        require(primaryOutcomeBuckets.map { it.outcome } == primaryOutcomeBuckets.map { it.outcome }.distinct())
        require(breakdowns.zipWithNext().all { (previous, next) ->
            previous.axis.ordinal < next.axis.ordinal || previous.axis == next.axis && previous.key <= next.key
        })
        require(diagnosticFlags.map { it.flag } == HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.values().toList())
        require(logicalDigest.matches(SHA256))
        require(logicalDigest == HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.logicalDigest(copy(logicalDigest = "")))
    }
}

private val SHA256 = Regex("[0-9a-f]{64}")
private val HEAD = Regex("[0-9a-f]{40}")
private val SHARD = Regex("shard-[0-9]{6}")
private val ENTITY = Regex("[0-9A-Za-z]{6}")
