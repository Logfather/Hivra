package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource

object HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1 {
    const val VERSION = "HIM_EVIDENCE_ALIGNMENT_CATALOG_AUDIT_FINDINGS_ENRICHMENT_V1"
    const val NOT_PERSISTED_IN_AUDIT_V1 = "NOT_PERSISTED_IN_AUDIT_V1"

    val SOURCE_ORDER = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER

    fun plan(
        auditPlan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        provenance: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentProvenanceV1,
        maxItemsPerShard: Int = auditPlan.maxItemsPerShard,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1 {
        auditPlan.validate()
        provenance.validate()
        require(provenance.auditMissionLogicalDigest == auditPlan.missionDigest)
        require(maxItemsPerShard == auditPlan.maxItemsPerShard)
        val shards = auditPlan.shards.map {
            HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardPlanV1(
                shardId = it.shardId,
                startInclusive = it.startInclusive,
                endExclusive = it.endExclusive,
                canonicalCount = it.canonicalCount,
                canonicalOrderDigest = it.canonicalOrderDigest,
                canonicalEntityIds = it.canonicalEntityIds,
            )
        }
        val unsigned = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1(
            contractId = VERSION,
            auditMissionDigest = auditPlan.missionDigest,
            auditCanonicalOrderDigest = auditPlan.canonicalOrderDigest,
            expectedCanonicalCount = auditPlan.expectedCanonicalCount,
            maxItemsPerShard = maxItemsPerShard,
            shards = shards,
            provenance = provenance,
            logicalDigest = "",
        )
        return unsigned.copy(
            logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(unsigned),
        ).also { it.validate() }
    }

    fun findingOccurrenceId(
        shardId: String,
        entityId: String,
        source: HimGroundTruthSource,
        evidenceReference: String?,
    ): String = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(
        listOf(
            VERSION,
            shardId,
            entityId,
            source.name,
            evidenceReference ?: "NO_RETRIEVAL_HIT",
        ).joinToString("|"),
    )
}

enum class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus {
    RECONSTRUCTED,
    NO_RETRIEVAL_HIT,
    UNSUPPORTED_RECORD_KIND,
}

enum class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1 {
    MATCH,
    MISMATCH,
    NOT_RECONSTRUCTABLE,
}

data class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFileBindingV1(
    val relativePath: String,
    val byteSize: Long,
    val sha256: String,
    val logicalDigest: String,
) {
    init {
        require(enrichmentRelativeRepositoryPath(relativePath))
        require(byteSize >= 0)
        require(sha256.matches(SHA256))
        require(logicalDigest.matches(SHA256))
    }
}

data class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardFileBindingV1(
    val shardId: String,
    val file: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFileBindingV1,
) {
    init { require(shardId.matches(Regex("shard-[0-9]{6}"))) }
}

data class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentProvenanceV1(
    val auditHead: String,
    val enrichmentImplementationHead: String,
    val auditMission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFileBindingV1,
    val auditAggregate: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFileBindingV1,
    val auditMissionLogicalDigest: String,
    val auditAggregateLogicalDigest: String,
    val auditShardBindings: List<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardFileBindingV1>,
    val auditBindings: HimEvidenceAlignmentCatalogAuditBindingsV1,
    val extractorEvaluatorImplementationSha256: String,
) {
    fun validate() {
        require(auditHead.matches(Regex("[0-9a-f]{40}")))
        require(enrichmentImplementationHead.matches(Regex("[0-9a-f]{40}")))
        require(auditMissionLogicalDigest.matches(SHA256))
        require(auditAggregateLogicalDigest.matches(SHA256))
        require(extractorEvaluatorImplementationSha256.matches(SHA256))
        auditBindings.validate()
        require(auditBindings.gitHead == auditHead)
        require(auditShardBindings.size == 11)
        require(auditShardBindings.map { it.shardId } ==
            (1..11).map { "shard-${it.toString().padStart(6, '0')}" })
    }
}

data class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardPlanV1(
    val shardId: String,
    val startInclusive: Int,
    val endExclusive: Int,
    val canonicalCount: Int,
    val canonicalOrderDigest: String,
    val canonicalEntityIds: List<String>,
) {
    init {
        require(shardId.matches(Regex("shard-[0-9]{6}")))
        require(startInclusive >= 0 && endExclusive > startInclusive)
        require(endExclusive - startInclusive == canonicalCount)
        require(canonicalEntityIds.size == canonicalCount)
        require(canonicalOrderDigest.matches(SHA256))
    }
}

data class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1(
    val contractId: String,
    val auditMissionDigest: String,
    val auditCanonicalOrderDigest: String,
    val expectedCanonicalCount: Int,
    val maxItemsPerShard: Int,
    val shards: List<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardPlanV1>,
    val provenance: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentProvenanceV1,
    val logicalDigest: String,
) {
    fun validate() {
        require(contractId == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.VERSION)
        require(auditMissionDigest.matches(SHA256))
        require(auditCanonicalOrderDigest.matches(SHA256))
        require(expectedCanonicalCount > 0 && maxItemsPerShard > 0)
        require(shards.size == 11)
        require(shards.flatMap { it.canonicalEntityIds }.size == expectedCanonicalCount)
        require(shards.zipWithNext().all { it.first.endExclusive == it.second.startInclusive })
        require(provenance.auditMissionLogicalDigest == auditMissionDigest)
        provenance.validate()
        require(logicalDigest.matches(SHA256))
        require(logicalDigest == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(copy(logicalDigest = "")))
    }
}

data class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1(
    val findingOccurrenceId: String,
    val auditShardId: String,
    val entityId: String,
    val canonicalName: String,
    val normalizedName: String,
    val source: HimGroundTruthSource,
    val originalAuditClassification: HimEvidenceAlignmentAuditClassification,
    val originalEvidenceReference: String?,
    val originalPrimaryIdentity: String?,
    val originalClaimedEvidenceRelation: String? = null,
    val originalReasonPersistenceState: String = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.NOT_PERSISTED_IN_AUDIT_V1,
    val queryPlanTerms: List<HimEvidenceAlignmentCatalogAuditQueryTermV1>,
    val queryTermFindingState: String = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.NOT_PERSISTED_IN_AUDIT_V1,
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
) {
    init {
        require(findingOccurrenceId.matches(SHA256))
        require(auditShardId.matches(Regex("shard-[0-9]{6}")))
        require(entityId.matches(Regex("[0-9A-Za-z]{6}")))
        require(canonicalName.isNotBlank() && normalizedName.isNotBlank())
        require(originalEvidenceReference == null || originalEvidenceReference.isNotBlank())
        require(originalReasonPersistenceState == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.NOT_PERSISTED_IN_AUDIT_V1)
        require(queryTermFindingState == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.NOT_PERSISTED_IN_AUDIT_V1)
        require(queryPlanTerms.isNotEmpty())
        if (reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.NO_RETRIEVAL_HIT) {
            require(originalEvidenceReference == null)
            require(recordKind == null && projectionSha256 == null)
            require(reconstructedAlignmentClassification == null)
            require(classificationComparison == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.NOT_RECONSTRUCTABLE)
        } else {
            require(originalEvidenceReference != null)
            require(recordKind != null)
        }
    }
}

data class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(
    val canonicalsProcessed: Int,
    val sourcesProcessed: Int,
    val findingOccurrences: Int,
    val uniqueEvidenceReferences: Int,
    val exactFetches: Int,
    val noRetrievalHits: Int,
    val unsupportedRecordKinds: Int,
    val reconstructedFindings: Int,
    val classificationMismatches: Int,
) {
    init {
        require(listOf(canonicalsProcessed, sourcesProcessed, findingOccurrences, uniqueEvidenceReferences,
            exactFetches, noRetrievalHits, unsupportedRecordKinds, reconstructedFindings,
            classificationMismatches).all { it >= 0 })
        require(exactFetches <= uniqueEvidenceReferences)
        require(reconstructedFindings + noRetrievalHits == findingOccurrences)
    }
}

data class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1(
    val missionDigest: String,
    val shardId: String,
    val shardBindingDigest: String,
    val state: HimEvidenceAlignmentCatalogAuditState,
    val findings: List<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1>,
    val counters: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1,
    val logicalDigest: String,
) {
    fun validateAgainst(mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1) {
        mission.validate()
        val shard = mission.shards.singleOrNull { it.shardId == shardId } ?: error("Unknown enrichment shard")
        require(missionDigest == mission.logicalDigest)
        require(shardBindingDigest == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.shardBindingDigest(mission, shard))
        require(findings.map { it.findingOccurrenceId }.distinct().size == findings.size)
        require(findings.all { it.auditShardId == shardId && it.entityId in shard.canonicalEntityIds })
        require(logicalDigest.matches(SHA256))
        require(logicalDigest == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(copy(logicalDigest = "")))
    }
}

data class HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1(
    val missionDigest: String,
    val state: HimEvidenceAlignmentCatalogAuditState,
    val shardResults: List<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1>,
    val counters: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1,
    val logicalDigest: String,
) {
    fun validateAgainst(mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1) {
        mission.validate()
        require(missionDigest == mission.logicalDigest)
        require(shardResults.map { it.shardId }.distinct().size == shardResults.size)
        shardResults.forEach { it.validateAgainst(mission) }
        val expectedIds = mission.shards.map { it.shardId }.toSet()
        require(shardResults.map { it.shardId }.toSet().let { candidateIds ->
            candidateIds == expectedIds || candidateIds.all { it in expectedIds }
        })
        require(logicalDigest.matches(SHA256))
        require(logicalDigest == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(copy(logicalDigest = "")))
        val expectedState = if (shardResults.size == mission.shards.size &&
            shardResults.all { it.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE }) {
            HimEvidenceAlignmentCatalogAuditState.COMPLETE
        } else {
            HimEvidenceAlignmentCatalogAuditState.PARTIAL
        }
        require(state == expectedState)
    }
}

private fun enrichmentRelativeRepositoryPath(value: String): Boolean =
    value.isNotBlank() && !value.startsWith("/") && !value.contains('\\') &&
        value.split('/').none { it.isBlank() || it == "." || it == ".." }

private val SHA256 = Regex("[0-9a-f]{64}")
