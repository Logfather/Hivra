package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File

/**
 * Contract for deterministic, observable analysis of existing zero-candidate
 * diagnostic records. Buckets and flags describe materialized fields only; they
 * do not prove translation gaps, missing canonicals, extractor defects, or any
 * other causal claim. The compound/token flag is a review signal, not a proven
 * compound cause. No token groups are inferred when the input has no materialized
 * token list.
 */
object HimZeroCandidateCauseAnalysisContractV1 {
    const val VERSION = "HIM_ZERO_CANDIDATE_CAUSE_ANALYSIS_V1"

    val SOURCE_ORDER = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER
    val BUCKET_ORDER = HimZeroCandidateCauseAnalysisPrimaryBucketV1.values().toList()
    val FLAG_ORDER = HimZeroCandidateCauseAnalysisFlagV1.values().toList()
    val RECORD_KIND_ORDER = listOf(
        HimEvidenceRecordKind.OFF_PRODUCT,
        HimEvidenceRecordKind.AGRIBALYSE_RECORD,
        HimEvidenceRecordKind.CIQUAL_FOOD,
        HimEvidenceRecordKind.GI_MEASUREMENT,
    )

    fun sourceIndex(source: HimGroundTruthSource): Int = SOURCE_ORDER.indexOf(source)
}

enum class HimZeroCandidateCauseAnalysisPrimaryBucketV1 {
    NO_USABLE_DIAGNOSTIC_FIELDS,
    CONTEXT_ONLY,
    BRAND_CONTEXT_DOMINATED,
    LOCALIZED_PRIMARY_ONLY,
    STRUCTURED_ALPHANUMERIC_PRIMARY,
    PRIMARY_TEXT_PRESENT_NO_MATCH,
    UNCLASSIFIED,
}

enum class HimZeroCandidateCauseAnalysisFlagV1 {
    HAS_PRIMARY_IDENTITY_FIELD,
    HAS_MULTIPLE_PRIMARY_IDENTITY_FIELDS,
    HAS_BRAND_CONTEXT,
    HAS_CATEGORY_CONTEXT,
    HAS_ONLY_CONTEXT_FIELDS,
    HAS_EXPLICIT_LOCALIZED_FIELD,
    HAS_SOURCE_NEUTRAL_PRIMARY_FIELD,
    HAS_DIGITS_OR_CODE_SIGNAL,
    HAS_NON_ASCII_TEXT,
    HAS_MULTIPLE_NORMALIZED_TOKENS,
    HAS_TOKEN_SEPARATOR_SIGNAL,
    POSSIBLE_COMPOUND_OR_TOKEN_BOUNDARY_REVIEW,
    HAS_MULTIPLE_OCCURRENCES,
    HAS_MULTIPLE_CANONICAL_TARGETS,
}

enum class HimZeroCandidateCauseAnalysisFailureReasonV1 {
    INPUT_BINDING_MISMATCH,
    MISSION_VALIDATION_FAILED,
    AGGREGATE_VALIDATION_FAILED,
    MISSION_BINDING_MISMATCH,
    AGGREGATE_BINDING_MISMATCH,
    ZERO_CANDIDATE_COUNT_MISMATCH,
    DUPLICATE_EVIDENCE_REFERENCE,
    UNSUPPORTED_FIELD_ROLE,
    UNSUPPORTED_FIELD_SHAPE,
    BUCKET_ASSIGNMENT_FAILED,
    COUNTER_INVARIANT_FAILED,
    REPORT_VALIDATION_FAILED,
    PERSISTENCE_FAILED,
}

data class HimZeroCandidateCauseAnalysisFileBindingV1(
    val relativePath: String,
    val byteSize: Long,
    val sha256: String,
    val logicalDigest: String,
) {
    init {
        require(relativePath.isNotBlank())
        require(!relativePath.startsWith('/') && !relativePath.contains('\\'))
        require(relativePath.split('/').none { it.isBlank() || it == "." || it == ".." })
        require(byteSize >= 0)
        require(sha256.matches(SHA256))
        require(logicalDigest.matches(SHA256))
    }
}

data class HimZeroCandidateCauseAnalysisInputBindingV1(
    val diagnosticMission: HimZeroCandidateCauseAnalysisFileBindingV1,
    val diagnosticAggregate: HimZeroCandidateCauseAnalysisFileBindingV1,
    val diagnosticMissionLogicalDigest: String,
    val diagnosticAggregateLogicalDigest: String,
    val analysisImplementationHead: String,
    val bindingDigest: String,
) {
    fun validate() {
        require(diagnosticMission.logicalDigest == diagnosticMissionLogicalDigest)
        require(diagnosticAggregate.logicalDigest == diagnosticAggregateLogicalDigest)
        require(analysisImplementationHead.matches(HEAD))
        require(bindingDigest.matches(SHA256))
        require(bindingDigest == HimZeroCandidateCauseAnalysisPersistenceV1.bindingDigest(copy(bindingDigest = "")))
    }
}

data class HimZeroCandidateCauseAnalysisRecordV1(
    val source: HimGroundTruthSource,
    val evidenceReference: String,
    val recordKind: HimEvidenceRecordKind,
    val ownerShardId: String,
    val findingOccurrenceIds: List<String>,
    val canonicalEntityIds: List<String>,
    val originalStatus: HimUnresolvedPrimaryIdentityDiagnosticStatusV1,
    val candidateIdentities: List<String>,
    val fields: List<HimUnresolvedPrimaryIdentityDiagnosticFieldV1>,
    val primaryValues: List<String>,
    val primaryBucket: HimZeroCandidateCauseAnalysisPrimaryBucketV1,
    val flags: List<HimZeroCandidateCauseAnalysisFlagV1>,
) {
    init {
        require(recordKind.source == source)
        require(ownerShardId in HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS)
        require(evidenceReference.isNotBlank())
        HimEvidenceRecordReference.parse(source, evidenceReference)
        require(findingOccurrenceIds.isNotEmpty() && findingOccurrenceIds == findingOccurrenceIds.distinct().sorted())
        require(findingOccurrenceIds.all { it.matches(SHA256) })
        require(canonicalEntityIds == canonicalEntityIds.distinct().sorted())
        require(canonicalEntityIds.all { it.matches(ENTITY) })
        require(originalStatus == HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_PRESENT_NO_CANDIDATE)
        require(candidateIdentities.isEmpty())
        require(fields == fields.sortedWith(compareBy({ it.fieldPath }, { it.originalLexicalValue })))
        require(fields.all { it.source == source && it.recordKind == recordKind })
        require(primaryValues == primaryValues.distinct().sorted())
        require(flags == flags.distinct().sortedBy { it.ordinal })
    }
}

data class HimZeroCandidateCauseAnalysisTotalCountersV1(
    val records: Int,
    val zeroCandidateRecords: Int,
    val uniqueEvidenceReferences: Int,
    val canonicalTargets: Int,
) {
    init { require(listOf(records, zeroCandidateRecords, uniqueEvidenceReferences, canonicalTargets).all { it >= 0 }) }
}

data class HimZeroCandidateCauseAnalysisBucketCounterV1(
    val bucket: HimZeroCandidateCauseAnalysisPrimaryBucketV1,
    val count: Int,
) {
    init { require(count >= 0) }
}

data class HimZeroCandidateCauseAnalysisSourceBucketBreakdownV1(
    val source: HimGroundTruthSource,
    val bucket: HimZeroCandidateCauseAnalysisPrimaryBucketV1,
    val count: Int,
) {
    init { require(count >= 0) }
}

data class HimZeroCandidateCauseAnalysisSourceFlagBreakdownV1(
    val source: HimGroundTruthSource,
    val flag: HimZeroCandidateCauseAnalysisFlagV1,
    val count: Int,
) {
    init { require(count >= 0) }
}

data class HimZeroCandidateCauseAnalysisRecordKindBucketBreakdownV1(
    val recordKind: HimEvidenceRecordKind,
    val bucket: HimZeroCandidateCauseAnalysisPrimaryBucketV1,
    val count: Int,
) {
    init { require(count >= 0) }
}

data class HimZeroCandidateCauseAnalysisFlagCounterV1(
    val flag: HimZeroCandidateCauseAnalysisFlagV1,
    val count: Int,
) {
    init { require(count >= 0) }
}

data class HimZeroCandidateCauseAnalysisReferenceMemberV1(
    val source: HimGroundTruthSource,
    val evidenceReference: String,
) {
    init {
        HimEvidenceRecordReference.parse(source, evidenceReference)
    }
}

data class HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1(
    val key: String,
    val references: List<HimZeroCandidateCauseAnalysisReferenceMemberV1>,
) {
    init {
        require(key.isNotBlank())
        require(references.size >= 2)
        require(references == references.distinct().sortedWith(compareBy({ HimZeroCandidateCauseAnalysisContractV1.sourceIndex(it.source) }, { it.evidenceReference })))
    }
}

data class HimZeroCandidateCauseAnalysisReportV1(
    val contractId: String,
    val inputBinding: HimZeroCandidateCauseAnalysisInputBindingV1,
    val counters: HimZeroCandidateCauseAnalysisTotalCountersV1,
    val bucketCounters: List<HimZeroCandidateCauseAnalysisBucketCounterV1>,
    val sourceBucketBreakdown: List<HimZeroCandidateCauseAnalysisSourceBucketBreakdownV1>,
    val sourceFlagBreakdown: List<HimZeroCandidateCauseAnalysisSourceFlagBreakdownV1>,
    val recordKindBucketBreakdown: List<HimZeroCandidateCauseAnalysisRecordKindBucketBreakdownV1>,
    val flagCounters: List<HimZeroCandidateCauseAnalysisFlagCounterV1>,
    val recurringPrimaryValueGroups: List<HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1>,
    val recurringTokenGroups: List<String>,
    val records: List<HimZeroCandidateCauseAnalysisRecordV1>,
    val logicalDigest: String,
) {
    fun validate() {
        require(contractId == HimZeroCandidateCauseAnalysisContractV1.VERSION)
        inputBinding.validate()
        require(records == records.sortedWith(compareBy({ HimZeroCandidateCauseAnalysisContractV1.sourceIndex(it.source) }, { it.evidenceReference })))
        require(records.map { it.source to it.evidenceReference }.distinct().size == records.size)
        require(records.flatMap { it.findingOccurrenceIds }.distinct().size == records.flatMap { it.findingOccurrenceIds }.size)
        require(counters.records == records.size)
        require(counters.zeroCandidateRecords == records.size)
        require(counters.uniqueEvidenceReferences == records.size)
        require(counters.canonicalTargets == records.flatMap { it.canonicalEntityIds }.distinct().size)
        require(bucketCounters.map { it.bucket } == HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER)
        require(bucketCounters.sumOf { it.count } == records.size)
        require(bucketCounters == HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER.map { bucket ->
            HimZeroCandidateCauseAnalysisBucketCounterV1(bucket, records.count { it.primaryBucket == bucket })
        })
        require(sourceBucketBreakdown == HimZeroCandidateCauseAnalysisContractV1.SOURCE_ORDER.flatMap { source ->
            HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER.map { bucket ->
                HimZeroCandidateCauseAnalysisSourceBucketBreakdownV1(source, bucket, records.count { it.source == source && it.primaryBucket == bucket })
            }
        })
        require(recordKindBucketBreakdown == HimZeroCandidateCauseAnalysisContractV1.RECORD_KIND_ORDER.flatMap { kind ->
            HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER.map { bucket ->
                HimZeroCandidateCauseAnalysisRecordKindBucketBreakdownV1(kind, bucket, records.count { it.recordKind == kind && it.primaryBucket == bucket })
            }
        })
        require(sourceFlagBreakdown == HimZeroCandidateCauseAnalysisContractV1.SOURCE_ORDER.flatMap { source ->
            HimZeroCandidateCauseAnalysisContractV1.FLAG_ORDER.map { flag ->
                HimZeroCandidateCauseAnalysisSourceFlagBreakdownV1(source, flag, records.count { it.source == source && flag in it.flags })
            }
        })
        require(flagCounters == HimZeroCandidateCauseAnalysisContractV1.FLAG_ORDER.map { flag ->
            HimZeroCandidateCauseAnalysisFlagCounterV1(flag, records.count { flag in it.flags })
        })
        require(recurringPrimaryValueGroups == recurringPrimaryValueGroups.sortedWith(compareByDescending<HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1> { it.references.size }.thenBy { it.key }))
        require(recurringTokenGroups == recurringTokenGroups.distinct().sorted())
        require(logicalDigest.matches(SHA256))
        require(logicalDigest == HimZeroCandidateCauseAnalysisPersistenceV1.logicalDigest(copy(logicalDigest = "")))
    }
}

data class HimZeroCandidateCauseAnalysisRuntimeRequestV1(
    val enabled: Boolean,
    val mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1,
    val aggregate: HimUnresolvedPrimaryIdentityDiagnosticAggregateV1,
    val inputBinding: HimZeroCandidateCauseAnalysisInputBindingV1,
    val jsonOutputFile: File? = null,
    val textOutputFile: File? = null,
)

sealed interface HimZeroCandidateCauseAnalysisRuntimeResult<out T> {
    data class Completed<T>(val value: T) : HimZeroCandidateCauseAnalysisRuntimeResult<T>
    data class Skipped(val reason: String) : HimZeroCandidateCauseAnalysisRuntimeResult<Nothing>
    data class Failed(
        val reason: HimZeroCandidateCauseAnalysisFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateCauseAnalysisRuntimeResult<Nothing>
}

private val SHA256 = Regex("[0-9a-f]{64}")
private val HEAD = Regex("[0-9a-f]{40}")
private val ENTITY = Regex("[0-9A-Za-z]{6}")
