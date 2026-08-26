package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource

object HimUnresolvedPrimaryIdentityDiagnosticContractV1 {
    const val VERSION = "HIM_UNRESOLVED_PRIMARY_IDENTITY_DIAGNOSTIC_V1"
    const val DIAGNOSTIC_ROOT = "build/knowledge/reports/him/evidence-alignment/catalog-audit/unresolved-identity-diagnostic/v1"
    const val SHARDING_POLICY_VERSION = "HIM_UNRESOLVED_PRIMARY_IDENTITY_DIAGNOSTIC_SHARDING_V1"
    const val NOT_AVAILABLE = "NOT_AVAILABLE"

    val SOURCE_ORDER = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER
    val SHARD_IDS = (1..11).map { "shard-${it.toString().padStart(6, '0')}" }

    fun referenceKey(source: HimGroundTruthSource, evidenceReference: String): String =
        "${source.name}|$evidenceReference"

    fun expectedOwner(occurrences: List<HimUnresolvedPrimaryIdentityDiagnosticOccurrenceV1>): String =
        occurrences.sortedWith(
            compareBy<HimUnresolvedPrimaryIdentityDiagnosticOccurrenceV1> { it.auditShardId }
                .thenBy { it.entityId }
                .thenBy { it.findingOccurrenceId },
        ).first().auditShardId
}

enum class HimUnresolvedPrimaryIdentityDiagnosticLanguageV1 {
    DE,
    EN,
    FR,
    ES_ES,
    SCIENTIFIC,
    UNSPECIFIED,
}

enum class HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1 {
    PRIMARY_IDENTITY_CANDIDATE,
    GENERIC_IDENTITY_CANDIDATE,
    CATEGORY_CONTEXT,
    BRAND_CONTEXT,
    SCIENTIFIC_CONTEXT,
    OTHER_IDENTITY_CONTEXT,
}

enum class HimUnresolvedPrimaryIdentityDiagnosticStatusV1 {
    IDENTITY_FIELDS_PRESENT_NO_CANDIDATE,
    SINGLE_CANDIDATE_UNRESOLVED,
    MULTIPLE_CANDIDATES_UNRESOLVED,
    IDENTITY_FIELDS_MISSING,
    IDENTITY_FIELDS_EMPTY,
    FETCH_OR_BINDING_FAILURE,
}

enum class HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1 {
    INPUT_BINDING_MISMATCH,
    MISSION_VALIDATION_FAILED,
    ENRICHMENT_MISSION_VALIDATION_FAILED,
    ENRICHMENT_AGGREGATE_VALIDATION_FAILED,
    REFERENCE_PLAN_MISMATCH,
    DUPLICATE_REFERENCE_PLAN,
    DUPLICATE_OCCURRENCE,
    SHARD_BINDING_MISMATCH,
    SOURCE_BINDING_MISMATCH,
    RECORD_KIND_MISMATCH,
    EXACT_FETCH_FAILED,
    PROJECTION_INVALID,
    EXTRACTOR_FAILURE,
    COUNTER_INVARIANT_FAILED,
    PERSISTENCE_FAILED,
    RELOAD_VALIDATION_FAILED,
}

data class HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1(
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

data class HimUnresolvedPrimaryIdentityDiagnosticSourceReferenceCountV1(
    val source: HimGroundTruthSource,
    val count: Int,
) {
    init {
        require(count >= 0)
    }
}

data class HimUnresolvedPrimaryIdentityDiagnosticInputBindingV1(
    val analysis: HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1,
    val enrichmentMission: HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1,
    val enrichmentAggregate: HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1,
    val auditHead: String,
    val enrichmentImplementationHead: String,
    val analysisImplementationHead: String,
    val expectedOccurrenceCount: Int,
    val expectedReferenceCount: Int,
    val expectedCanonicalCount: Int,
    val sourceReferenceCounts: List<HimUnresolvedPrimaryIdentityDiagnosticSourceReferenceCountV1>,
    val bindingDigest: String,
) {
    fun validate() {
        require(auditHead.matches(HEAD))
        require(enrichmentImplementationHead.matches(HEAD))
        require(analysisImplementationHead.matches(HEAD))
        require(expectedOccurrenceCount > 0)
        require(expectedReferenceCount > 0)
        require(expectedCanonicalCount > 0)
        require(sourceReferenceCounts.map { it.source } == HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER)
        require(sourceReferenceCounts.sumOf { it.count } == expectedReferenceCount)
        require(bindingDigest.matches(SHA256))
        require(bindingDigest == HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.bindingDigest(copy(bindingDigest = "")))
    }
}

data class HimUnresolvedPrimaryIdentityDiagnosticOccurrenceV1(
    val findingOccurrenceId: String,
    val auditShardId: String,
    val entityId: String,
    val canonicalName: String,
    val normalizedName: String,
    val source: HimGroundTruthSource,
    val evidenceReference: String,
    val originalAuditClassification: HimEvidenceAlignmentAuditClassification,
    val originalRecordKind: HimEvidenceRecordKind,
    val candidateIdentities: List<String>,
    val originalExtractorResolution: String?,
    val originalIdentityFieldUsed: List<String>,
    val originalExtractionPath: String?,
    val originalReasonCode: String?,
) {
    init {
        require(findingOccurrenceId.matches(SHA256))
        require(auditShardId in HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS)
        require(entityId.matches(ENTITY))
        require(canonicalName.isNotBlank() && normalizedName.isNotBlank())
        require(evidenceReference.isNotBlank())
        require(originalAuditClassification == HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY)
        require(originalRecordKind.source == source)
        HimEvidenceRecordReference.parse(source, evidenceReference)
        require(candidateIdentities.distinct().size == candidateIdentities.size)
        require(candidateIdentities.all(String::isNotBlank))
        require(originalIdentityFieldUsed.distinct().size == originalIdentityFieldUsed.size)
    }
}

data class HimUnresolvedPrimaryIdentityDiagnosticReferencePlanV1(
    val source: HimGroundTruthSource,
    val evidenceReference: String,
    val ownerShardId: String,
    val occurrences: List<HimUnresolvedPrimaryIdentityDiagnosticOccurrenceV1>,
) {
    init {
        require(ownerShardId in HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS)
        require(occurrences.isNotEmpty())
        require(occurrences.all { it.source == source && it.evidenceReference == evidenceReference })
        require(ownerShardId == HimUnresolvedPrimaryIdentityDiagnosticContractV1.expectedOwner(occurrences))
        require(occurrences.map { it.findingOccurrenceId }.distinct().size == occurrences.size)
        HimEvidenceRecordReference.parse(source, evidenceReference)
    }

    val key: String
        get() = HimUnresolvedPrimaryIdentityDiagnosticContractV1.referenceKey(source, evidenceReference)
}

data class HimUnresolvedPrimaryIdentityDiagnosticShardPlanV1(
    val shardId: String,
    val referencePlanKeys: List<String>,
) {
    init {
        require(shardId in HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS)
        require(referencePlanKeys == referencePlanKeys.distinct())
        require(referencePlanKeys == referencePlanKeys.sorted())
    }
}

data class HimUnresolvedPrimaryIdentityDiagnosticMissionV1(
    val contractId: String,
    val inputBinding: HimUnresolvedPrimaryIdentityDiagnosticInputBindingV1,
    val auditHead: String,
    val enrichmentImplementationHead: String,
    val analysisImplementationHead: String,
    val diagnosticImplementationHead: String,
    val analysisBinding: HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1,
    val enrichmentMissionBinding: HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1,
    val enrichmentAggregateBinding: HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1,
    val sourceBindings: List<HimEvidenceAlignmentCatalogAuditSourceBindingV1>,
    val expectedOccurrenceCount: Int,
    val expectedReferenceCount: Int,
    val expectedCanonicalCount: Int,
    val reusedReferenceCount: Int,
    val reusedOccurrenceCount: Int,
    val missingIdentityCount: Int,
    val zeroCandidateCount: Int,
    val singleCandidateCount: Int,
    val multipleCandidateCount: Int,
    val referencePlans: List<HimUnresolvedPrimaryIdentityDiagnosticReferencePlanV1>,
    val shards: List<HimUnresolvedPrimaryIdentityDiagnosticShardPlanV1>,
    val logicalDigest: String,
) {
    fun validate() {
        require(contractId == HimUnresolvedPrimaryIdentityDiagnosticContractV1.VERSION)
        require(auditHead.matches(HEAD))
        require(enrichmentImplementationHead.matches(HEAD))
        require(analysisImplementationHead.matches(HEAD))
        require(diagnosticImplementationHead.matches(HEAD))
        inputBinding.validate()
        require(inputBinding.auditHead == auditHead)
        require(inputBinding.enrichmentImplementationHead == enrichmentImplementationHead)
        require(inputBinding.analysisImplementationHead == analysisImplementationHead)
        require(inputBinding.analysis == analysisBinding)
        require(inputBinding.enrichmentMission == enrichmentMissionBinding)
        require(inputBinding.enrichmentAggregate == enrichmentAggregateBinding)
        require(sourceBindings.map { it.source } == HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER)
        sourceBindings.forEach { it.validate() }
        require(expectedOccurrenceCount == inputBinding.expectedOccurrenceCount)
        require(expectedReferenceCount == inputBinding.expectedReferenceCount)
        require(expectedCanonicalCount == inputBinding.expectedCanonicalCount)
        require(referencePlans.map { it.key }.distinct().size == referencePlans.size)
        require(referencePlans == referencePlans.sortedWith(compareBy({ HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER.indexOf(it.source) }, { it.evidenceReference })))
        val occurrences = referencePlans.flatMap { it.occurrences }
        require(occurrences.map { it.findingOccurrenceId }.distinct().size == occurrences.size)
        require(occurrences.size == expectedOccurrenceCount)
        require(referencePlans.size == expectedReferenceCount)
        require(occurrences.map { it.entityId }.distinct().size == expectedCanonicalCount)
        require(referencePlans.count { it.occurrences.size > 1 } == reusedReferenceCount)
        require(referencePlans.filter { it.occurrences.size > 1 }.sumOf { it.occurrences.size } == reusedOccurrenceCount)
        require(sourceReferenceCountsFrom(referencePlans) == inputBinding.sourceReferenceCounts)
        require(occurrences.count { it.originalExtractorResolution == "MISSING" } == missingIdentityCount)
        require(occurrences.count { it.candidateIdentities.isEmpty() } == zeroCandidateCount)
        require(occurrences.count { it.candidateIdentities.size == 1 } == singleCandidateCount)
        require(occurrences.count { it.candidateIdentities.size >= 2 } == multipleCandidateCount)
        require(shards.map { it.shardId } == HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS)
        val assigned = shards.flatMap { shard -> shard.referencePlanKeys.map { it to shard.shardId } }
        require(assigned.map { it.first }.distinct().size == assigned.size)
        require(assigned.toMap() == referencePlans.associate { it.key to it.ownerShardId })
        require(logicalDigest.matches(SHA256))
        require(logicalDigest == HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(copy(logicalDigest = "")))
    }

    private fun sourceReferenceCountsFrom(
        plans: List<HimUnresolvedPrimaryIdentityDiagnosticReferencePlanV1>,
    ) = HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER.map {
        HimUnresolvedPrimaryIdentityDiagnosticSourceReferenceCountV1(it, plans.count { plan -> plan.source == it })
    }
}

data class HimUnresolvedPrimaryIdentityDiagnosticLexicalFeaturesV1(
    val empty: Boolean,
    val characterLength: Int,
    val tokenCount: Int,
    val distinctTokenCount: Int,
    val containsDigits: Boolean,
    val containsParentheses: Boolean,
    val containsHyphen: Boolean,
    val containsComma: Boolean,
    val containsSlash: Boolean,
    val containsBrandContext: Boolean,
) {
    init {
        require(characterLength >= 0 && tokenCount >= 0 && distinctTokenCount >= 0)
        require(distinctTokenCount <= tokenCount)
    }
}

data class HimUnresolvedPrimaryIdentityDiagnosticFieldV1(
    val fieldPath: String,
    val originalLexicalValue: String,
    val trimmedValue: String,
    val language: HimUnresolvedPrimaryIdentityDiagnosticLanguageV1,
    val role: HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1,
    val source: HimGroundTruthSource,
    val recordKind: HimEvidenceRecordKind,
    val lexicalFeatures: HimUnresolvedPrimaryIdentityDiagnosticLexicalFeaturesV1,
) {
    init {
        require(fieldPath.isNotBlank())
        require(trimmedValue == originalLexicalValue.trim())
        require(recordKind.source == source)
    }
}

data class HimUnresolvedPrimaryIdentityDiagnosticRecordV1(
    val source: HimGroundTruthSource,
    val evidenceReference: String,
    val recordKind: HimEvidenceRecordKind,
    val ownerShardId: String,
    val occurrences: List<HimUnresolvedPrimaryIdentityDiagnosticOccurrenceV1>,
    val canonicalEntityIds: List<String>,
    val fields: List<HimUnresolvedPrimaryIdentityDiagnosticFieldV1>,
    val candidateIdentities: List<String>,
    val originalExtractorResolutions: List<String>,
    val originalIdentityFieldsUsed: List<String>,
    val originalExtractionPaths: List<String>,
    val originalReasonCodes: List<String>,
    val status: HimUnresolvedPrimaryIdentityDiagnosticStatusV1,
) {
    init {
        require(occurrences.isNotEmpty())
        require(occurrences.all { it.source == source && it.evidenceReference == evidenceReference })
        require(canonicalEntityIds == canonicalEntityIds.distinct().sorted())
        require(fields == fields.sortedWith(compareBy({ it.fieldPath }, { it.originalLexicalValue })))
        require(candidateIdentities == candidateIdentities.distinct().sorted())
        require(fields.all { it.source == source && it.recordKind == recordKind })
    }
}

data class HimUnresolvedPrimaryIdentityDiagnosticSourceCounterV1(
    val source: HimGroundTruthSource,
    val references: Int,
) {
    init { require(references >= 0) }
}

data class HimUnresolvedPrimaryIdentityDiagnosticCountersV1(
    val unresolvedOccurrences: Int,
    val canonicalTargets: Int,
    val uniqueEvidenceReferences: Int,
    val exactFetches: Int,
    val recordsLoaded: Int,
    val identityFieldsPresent: Int,
    val recordsWithNoIdentityFields: Int,
    val recordsWithOnlyEmptyIdentityFields: Int,
    val zeroCandidateRecords: Int,
    val singleCandidateRecords: Int,
    val multipleCandidateRecords: Int,
    val perSourceReferences: List<HimUnresolvedPrimaryIdentityDiagnosticSourceCounterV1>,
    val technicalErrors: Int,
) {
    fun validate() {
        require(listOf(unresolvedOccurrences, canonicalTargets, uniqueEvidenceReferences, exactFetches,
            recordsLoaded, identityFieldsPresent, recordsWithNoIdentityFields,
            recordsWithOnlyEmptyIdentityFields, zeroCandidateRecords, singleCandidateRecords,
            multipleCandidateRecords, technicalErrors).all { it >= 0 })
        require(exactFetches == uniqueEvidenceReferences)
        require(recordsLoaded == uniqueEvidenceReferences)
        require(zeroCandidateRecords + singleCandidateRecords + multipleCandidateRecords == recordsLoaded)
        require(identityFieldsPresent + recordsWithNoIdentityFields + recordsWithOnlyEmptyIdentityFields == recordsLoaded)
        require(perSourceReferences.map { it.source } == HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER)
        require(perSourceReferences.sumOf { it.references } == uniqueEvidenceReferences)
        require(technicalErrors == 0)
    }
}

enum class HimUnresolvedPrimaryIdentityDiagnosticResultStateV1 {
    PARTIAL,
    COMPLETE,
}

data class HimUnresolvedPrimaryIdentityDiagnosticShardResultV1(
    val missionDigest: String,
    val shardId: String,
    val state: HimUnresolvedPrimaryIdentityDiagnosticResultStateV1,
    val records: List<HimUnresolvedPrimaryIdentityDiagnosticRecordV1>,
    val counters: HimUnresolvedPrimaryIdentityDiagnosticCountersV1,
    val logicalDigest: String,
) {
    fun validateAgainst(mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1) {
        mission.validate()
        require(shardId in HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS)
        require(missionDigest == mission.logicalDigest)
        val plan = mission.shards.single { it.shardId == shardId }
        require(records.map { HimUnresolvedPrimaryIdentityDiagnosticContractV1.referenceKey(it.source, it.evidenceReference) } == plan.referencePlanKeys)
        require(records.map { it.ownerShardId }.all { it == shardId })
        require(records.map { it.evidenceReference }.distinct().size == records.size)
        counters.validate()
        val derived = HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.deriveCounters(records)
        require(counters == derived)
        require(logicalDigest.matches(SHA256))
        require(logicalDigest == HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(copy(logicalDigest = "")))
        require(state == HimUnresolvedPrimaryIdentityDiagnosticResultStateV1.COMPLETE)
    }
}

data class HimUnresolvedPrimaryIdentityDiagnosticAggregateV1(
    val missionDigest: String,
    val state: HimUnresolvedPrimaryIdentityDiagnosticResultStateV1,
    val shardResults: List<HimUnresolvedPrimaryIdentityDiagnosticShardResultV1>,
    val counters: HimUnresolvedPrimaryIdentityDiagnosticCountersV1,
    val logicalDigest: String,
) {
    fun validateAgainst(mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1) {
        mission.validate()
        require(missionDigest == mission.logicalDigest)
        require(shardResults.map { it.shardId } == HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS)
        shardResults.forEach { it.validateAgainst(mission) }
        val records = shardResults.flatMap { it.records }
        require(records.map { HimUnresolvedPrimaryIdentityDiagnosticContractV1.referenceKey(it.source, it.evidenceReference) }.distinct().size == records.size)
        require(records.size == mission.expectedReferenceCount)
        counters.validate()
        require(counters == HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.deriveCounters(records))
        require(counters.unresolvedOccurrences == mission.expectedOccurrenceCount)
        require(counters.canonicalTargets == mission.expectedCanonicalCount)
        require(state == HimUnresolvedPrimaryIdentityDiagnosticResultStateV1.COMPLETE)
        require(logicalDigest.matches(SHA256))
        require(logicalDigest == HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(copy(logicalDigest = "")))
    }
}

private val SHA256 = Regex("[0-9a-f]{64}")
private val HEAD = Regex("[0-9a-f]{40}")
private val ENTITY = Regex("[0-9A-Za-z]{6}")
