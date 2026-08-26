package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource

object HimZeroCandidateRecoveryReviewCorpusContractV1 {
    const val VERSION = "HIM_ZERO_CANDIDATE_RECOVERY_REVIEW_CORPUS_V1"
    const val REVIEW_ROOT =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-review-corpus/v1"

    val SOURCE_ORDER = HimZeroCandidateCauseAnalysisContractV1.SOURCE_ORDER
    val RECORD_KIND_ORDER = HimZeroCandidateCauseAnalysisContractV1.RECORD_KIND_ORDER
    val PRIORITY_ORDER = HimZeroCandidateRecoveryReviewPriorityV1.values().toList()
    val SELECTION_REASON_ORDER = HimZeroCandidateRecoveryReviewSelectionReasonV1.values().toList()

    fun sourceIndex(source: HimGroundTruthSource): Int = SOURCE_ORDER.indexOf(source)

    fun referenceKey(source: HimGroundTruthSource, evidenceReference: String): String =
        HimUnresolvedPrimaryIdentityDiagnosticContractV1.referenceKey(source, evidenceReference)
}

enum class HimZeroCandidateRecoveryReviewPriorityV1 {
    REPEATED_TEXT_SINGLE_AUDIT_TARGET,
    REPEATED_LOCALIZED_SINGLE_AUDIT_TARGET,
    REPEATED_STRUCTURED_OR_MIXED_SINGLE_AUDIT_TARGET,
    REPEATED_AMBIGUOUS_AUDIT_TARGET,
}

enum class HimZeroCandidateRecoveryReviewSelectionReasonV1 {
    RECURRING_PRIMARY_VALUE_GROUP,
    CIQUAL_LOCALIZED_PRIMARY_SUPPLEMENT,
}

enum class HimZeroCandidateRecoveryReviewAssociationStateV1 {
    UNVERIFIED_AUDIT_ASSOCIATION,
}

enum class HimZeroCandidateRecoveryReviewStateV1 {
    UNREVIEWED,
}

enum class HimZeroCandidateRecoveryReviewFailureReasonV1 {
    INPUT_BINDING_MISMATCH,
    CAUSE_ANALYSIS_VALIDATION_FAILED,
    CAUSE_ANALYSIS_BINDING_MISMATCH,
    CATALOG_VALIDATION_FAILED,
    AUTHORITY_VALIDATION_FAILED,
    CANONICAL_TARGET_NOT_FOUND,
    CANONICAL_TARGET_AMBIGUOUS,
    UNKNOWN_GROUP_REFERENCE,
    DUPLICATE_EVIDENCE_REFERENCE,
    INVALID_PRIORITY_CLASSIFICATION,
    SELECTION_INVARIANT_FAILED,
    COUNTER_INVARIANT_FAILED,
    REPORT_VALIDATION_FAILED,
    PERSISTENCE_FAILED,
}

data class HimZeroCandidateRecoveryReviewCorpusFileBindingV1(
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

data class HimZeroCandidateRecoveryReviewCorpusInputBindingV1(
    val causeAnalysis: HimZeroCandidateCauseAnalysisFileBindingV1,
    val canonicalCatalog: HimZeroCandidateRecoveryReviewCorpusFileBindingV1,
    val canonicalAuthority: HimZeroCandidateRecoveryReviewCorpusFileBindingV1,
    val causeAnalysisLogicalDigest: String,
    val recoveryReviewImplementationHead: String,
    val bindingDigest: String,
) {
    fun validate() {
        require(causeAnalysis.logicalDigest == causeAnalysisLogicalDigest)
        require(recoveryReviewImplementationHead.matches(HEAD))
        require(bindingDigest.matches(SHA256))
        require(bindingDigest == HimZeroCandidateRecoveryReviewCorpusPersistenceV1.bindingDigest(copy(bindingDigest = "")))
    }
}

data class HimZeroCandidateRecoveryReviewCanonicalTargetV1(
    val canonicalEntityId: String,
    val canonicalName: String,
    val identityTerms: List<String>,
    val aliasTerms: List<String>,
) {
    init {
        require(canonicalEntityId.matches(ENTITY))
        require(canonicalName.isNotBlank())
        require(identityTerms == identityTerms.distinct().sorted())
        require(aliasTerms == aliasTerms.distinct().sorted())
    }
}

data class HimZeroCandidateRecoveryReviewGroupMembershipV1(
    val primaryValue: String,
    val priorityClass: HimZeroCandidateRecoveryReviewPriorityV1,
    val referenceCount: Int,
    val findingOccurrenceCount: Int,
    val auditLinkedCanonicalEntityIds: List<String>,
) {
    init {
        require(primaryValue.isNotBlank())
        require(referenceCount >= 2)
        require(findingOccurrenceCount >= referenceCount)
        require(auditLinkedCanonicalEntityIds == auditLinkedCanonicalEntityIds.distinct().sorted())
        require(auditLinkedCanonicalEntityIds.all { it.matches(ENTITY) })
    }
}

data class HimZeroCandidateRecoveryReviewCorpusEntryV1(
    val stableEntryId: String,
    val source: HimGroundTruthSource,
    val evidenceReference: String,
    val recordKind: HimEvidenceRecordKind,
    val ownerShardId: String,
    val findingOccurrenceIds: List<String>,
    val auditLinkedCanonicalTargets: List<HimZeroCandidateRecoveryReviewCanonicalTargetV1>,
    val originalFields: List<HimUnresolvedPrimaryIdentityDiagnosticFieldV1>,
    val primaryValues: List<String>,
    val primaryBucket: HimZeroCandidateCauseAnalysisPrimaryBucketV1,
    val diagnosticFlags: List<HimZeroCandidateCauseAnalysisFlagV1>,
    val selectionReasons: List<HimZeroCandidateRecoveryReviewSelectionReasonV1>,
    val recurringGroupMemberships: List<HimZeroCandidateRecoveryReviewGroupMembershipV1>,
    val associationState: HimZeroCandidateRecoveryReviewAssociationStateV1,
    val reviewState: HimZeroCandidateRecoveryReviewStateV1,
) {
    init {
        require(recordKind.source == source)
        HimEvidenceRecordReference.parse(source, evidenceReference)
        require(ownerShardId in HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS)
        require(stableEntryId.matches(SHA256))
        require(findingOccurrenceIds.isNotEmpty() && findingOccurrenceIds == findingOccurrenceIds.distinct().sorted())
        require(findingOccurrenceIds.all { it.matches(SHA256) })
        require(auditLinkedCanonicalTargets.map { it.canonicalEntityId } ==
            auditLinkedCanonicalTargets.map { it.canonicalEntityId }.distinct().sorted())
        require(originalFields == originalFields.sortedWith(compareBy({ it.fieldPath }, { it.originalLexicalValue })))
        require(originalFields.all { it.source == source && it.recordKind == recordKind })
        require(primaryValues == primaryValues.distinct().sorted())
        require(diagnosticFlags == diagnosticFlags.distinct().sortedBy { it.ordinal })
        require(selectionReasons == selectionReasons.distinct().sortedBy { it.ordinal })
        require(recurringGroupMemberships == recurringGroupMemberships.sortedWith(
            compareBy<HimZeroCandidateRecoveryReviewGroupMembershipV1> {
                HimZeroCandidateRecoveryReviewContract.ordinal(it.priorityClass)
            }.thenBy { it.primaryValue },
        ))
        require(selectionReasons.isNotEmpty())
        require(associationState == HimZeroCandidateRecoveryReviewAssociationStateV1.UNVERIFIED_AUDIT_ASSOCIATION)
        require(reviewState == HimZeroCandidateRecoveryReviewStateV1.UNREVIEWED)
    }
}

data class HimZeroCandidateRecoveryReviewCorpusCountersV1(
    val causeAnalysisRecords: Int,
    val recurringPrimaryValueGroups: Int,
    val recurringReferenceMemberships: Int,
    val ciqualLocalizedSupplementRecords: Int,
    val selectionOverlapRecords: Int,
    val selectedUniqueRecords: Int,
    val auditLinkedCanonicalTargets: Int,
    val unreviewedRecords: Int,
    val confirmedRecords: Int,
)

data class HimZeroCandidateRecoveryReviewPriorityCounterV1(
    val priorityClass: HimZeroCandidateRecoveryReviewPriorityV1,
    val groups: Int,
    val referenceMemberships: Int,
    val findingOccurrences: Int,
)

data class HimZeroCandidateRecoveryReviewSourceBreakdownV1(
    val source: HimGroundTruthSource,
    val entries: Int,
)

data class HimZeroCandidateRecoveryReviewRecordKindBreakdownV1(
    val recordKind: HimEvidenceRecordKind,
    val entries: Int,
)

data class HimZeroCandidateRecoveryReviewSelectionReasonBreakdownV1(
    val reason: HimZeroCandidateRecoveryReviewSelectionReasonV1,
    val entries: Int,
)

data class HimZeroCandidateRecoveryReviewCorpusReportV1(
    val contractId: String,
    val inputBinding: HimZeroCandidateRecoveryReviewCorpusInputBindingV1,
    val counters: HimZeroCandidateRecoveryReviewCorpusCountersV1,
    val priorityCounters: List<HimZeroCandidateRecoveryReviewPriorityCounterV1>,
    val sourceBreakdown: List<HimZeroCandidateRecoveryReviewSourceBreakdownV1>,
    val recordKindBreakdown: List<HimZeroCandidateRecoveryReviewRecordKindBreakdownV1>,
    val selectionReasonBreakdown: List<HimZeroCandidateRecoveryReviewSelectionReasonBreakdownV1>,
    val entries: List<HimZeroCandidateRecoveryReviewCorpusEntryV1>,
    val logicalDigest: String,
) {
    fun validate() {
        require(contractId == HimZeroCandidateRecoveryReviewCorpusContractV1.VERSION)
        inputBinding.validate()
        require(entries == entries.sortedWith(compareBy({ HimZeroCandidateRecoveryReviewContract.sourceIndex(it.source) }, { it.evidenceReference })))
        require(entries.map { HimZeroCandidateRecoveryReviewContract.referenceKey(it.source, it.evidenceReference) }.distinct().size == entries.size)
        require(entries.flatMap { it.findingOccurrenceIds }.distinct().size == entries.flatMap { it.findingOccurrenceIds }.size)
        require(entries.all { it.reviewState == HimZeroCandidateRecoveryReviewStateV1.UNREVIEWED })
        require(counters.confirmedRecords == 0)
        require(counters.unreviewedRecords == entries.size)
        require(counters.selectedUniqueRecords == entries.size)
        require(counters.causeAnalysisRecords >= counters.selectedUniqueRecords)
        val uniqueMemberships = entries.flatMap { it.recurringGroupMemberships }
            .distinctBy { it.primaryValue }
        require(counters.recurringPrimaryValueGroups == uniqueMemberships.size)
        require(counters.recurringReferenceMemberships == uniqueMemberships.sumOf { it.referenceCount })
        require(counters.ciqualLocalizedSupplementRecords == entries.count {
            it.source == HimGroundTruthSource.CIQUAL &&
                it.primaryBucket == HimZeroCandidateCauseAnalysisPrimaryBucketV1.LOCALIZED_PRIMARY_ONLY
        })
        require(counters.auditLinkedCanonicalTargets == entries.flatMap { it.auditLinkedCanonicalTargets.map { target -> target.canonicalEntityId } }.distinct().size)
        require(priorityCounters.map { it.priorityClass } == HimZeroCandidateRecoveryReviewContract.PRIORITY_ORDER)
        val memberships = entries.flatMap { it.recurringGroupMemberships }
        require(priorityCounters == HimZeroCandidateRecoveryReviewContract.PRIORITY_ORDER.map { priority ->
            val selected = memberships.filter { it.priorityClass == priority }
                .distinctBy { it.primaryValue }
            HimZeroCandidateRecoveryReviewPriorityCounterV1(
                priority,
                selected.map { it.primaryValue }.distinct().size,
                selected.sumOf { it.referenceCount },
                selected.sumOf { it.findingOccurrenceCount },
            )
        })
        require(sourceBreakdown == HimZeroCandidateRecoveryReviewContract.SOURCE_ORDER.map { source ->
            HimZeroCandidateRecoveryReviewSourceBreakdownV1(source, entries.count { it.source == source })
        })
        require(recordKindBreakdown == HimZeroCandidateRecoveryReviewContract.RECORD_KIND_ORDER.map { kind ->
            HimZeroCandidateRecoveryReviewRecordKindBreakdownV1(kind, entries.count { it.recordKind == kind })
        })
        require(selectionReasonBreakdown == HimZeroCandidateRecoveryReviewContract.SELECTION_REASON_ORDER.map { reason ->
            HimZeroCandidateRecoveryReviewSelectionReasonBreakdownV1(reason, entries.count { reason in it.selectionReasons })
        })
        require(counters.selectionOverlapRecords == entries.count { it.selectionReasons.size > 1 })
        require(logicalDigest.matches(SHA256))
        require(logicalDigest == HimZeroCandidateRecoveryReviewCorpusPersistenceV1.logicalDigest(copy(logicalDigest = "")))
    }
}

private object HimZeroCandidateRecoveryReviewContract {
    val SOURCE_ORDER = HimZeroCandidateRecoveryReviewCorpusContractV1.SOURCE_ORDER
    val RECORD_KIND_ORDER = HimZeroCandidateRecoveryReviewCorpusContractV1.RECORD_KIND_ORDER
    val PRIORITY_ORDER = HimZeroCandidateRecoveryReviewCorpusContractV1.PRIORITY_ORDER
    val SELECTION_REASON_ORDER = HimZeroCandidateRecoveryReviewCorpusContractV1.SELECTION_REASON_ORDER
    fun sourceIndex(source: HimGroundTruthSource) = HimZeroCandidateRecoveryReviewCorpusContractV1.sourceIndex(source)
    fun referenceKey(source: HimGroundTruthSource, reference: String) =
        HimZeroCandidateRecoveryReviewCorpusContractV1.referenceKey(source, reference)
    fun ordinal(priority: HimZeroCandidateRecoveryReviewPriorityV1) = priority.ordinal
}

private val SHA256 = Regex("[0-9a-f]{64}")
private val HEAD = Regex("[0-9a-f]{40}")
private val ENTITY = Regex("[0-9A-Za-z]{6}")
