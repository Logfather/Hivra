package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.text.Normalizer
import java.util.Locale

object HimEvidenceAlignmentCatalogAuditContractV1 {
    const val VERSION = "HIM_EVIDENCE_ALIGNMENT_CATALOG_AUDIT_CONTRACT_V1"
    const val QUERY_POLICY_VERSION = "HIM_EVIDENCE_ALIGNMENT_CATALOG_AUDIT_QUERY_POLICY_V1"
    const val SHARDING_POLICY_VERSION = "HIM_EVIDENCE_ALIGNMENT_CATALOG_AUDIT_SHARDING_POLICY_V1"
    const val MAX_RESULTS_PER_QUERY = HimEvidenceSearchLimit.MAX_RESULTS
    const val MAX_QUERY_TERMS_PER_SOURCE = 32
    const val MAX_FETCHES_PER_CANONICAL_SOURCE =
        MAX_QUERY_TERMS_PER_SOURCE * MAX_RESULTS_PER_QUERY
    val SOURCE_ORDER = listOf(
        HimGroundTruthSource.OPEN_FOOD_FACTS,
        HimGroundTruthSource.AGRIBALYSE,
        HimGroundTruthSource.CIQUAL,
        HimGroundTruthSource.GLYCEMIC_INDEX,
    )

    fun normalizeQuery(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFC)
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.ROOT)

    fun plan(
        bindings: HimEvidenceAlignmentCatalogAuditBindingsV1,
        authority: HimCanonicalFamilyAuthority,
        maxItemsPerShard: Int,
    ): HimEvidenceAlignmentCatalogAuditMissionPlanV1 {
        bindings.validate()
        require(maxItemsPerShard > 0)
        val canonicalOrder = authority.families
            .map(::canonicalDescriptor)
            .sortedWith(compareBy<HimEvidenceAlignmentCatalogAuditCanonicalV1> { it.entityId }
                .thenBy { it.normalizedName })
        require(canonicalOrder.map { it.entityId }.distinct().size == canonicalOrder.size) {
            "Canonical universe contains duplicate Entity IDs."
        }
        val queryPlans = canonicalOrder.flatMap { canonical ->
            SOURCE_ORDER.map { source -> queryPlan(canonical, source) }
        }
        val canonicalOrderDigest = HimEvidenceAlignmentCatalogAuditPersistenceV1.sha256(
            canonicalOrder.joinToString("\n") {
                "${it.entityId}|${it.canonicalName}|${it.normalizedName}"
            },
        )
        val shards = canonicalOrder.chunked(maxItemsPerShard).mapIndexed { index, chunk ->
            val start = index * maxItemsPerShard
            HimEvidenceAlignmentCatalogAuditShardV1(
                shardId = "shard-${(index + 1).toString().padStart(6, '0')}",
                startInclusive = start,
                endExclusive = start + chunk.size,
                canonicalCount = chunk.size,
                canonicalOrderDigest = HimEvidenceAlignmentCatalogAuditPersistenceV1.sha256(
                    chunk.joinToString("\n") { it.entityId },
                ),
                canonicalEntityIds = chunk.map { it.entityId },
            )
        }
        val unsigned = HimEvidenceAlignmentCatalogAuditMissionPlanV1(
            bindings = bindings,
            canonicalOrder = canonicalOrder,
            canonicalOrderDigest = canonicalOrderDigest,
            expectedCanonicalCount = canonicalOrder.size,
            queryPlans = queryPlans,
            maxItemsPerShard = maxItemsPerShard,
            shards = shards,
            missionDigest = "",
        )
        return unsigned.copy(
            missionDigest = HimEvidenceAlignmentCatalogAuditPersistenceV1.logicalDigest(unsigned),
        ).also { it.validate() }
    }

    fun canonicalDescriptor(family: HimCanonicalFamily): HimEvidenceAlignmentCatalogAuditCanonicalV1 {
        val normalizedName = normalizeQuery(family.canonicalName)
        require(normalizedName.isNotEmpty())
        require(normalizedName == normalizeQuery(family.normalizedName))
        return HimEvidenceAlignmentCatalogAuditCanonicalV1(
            entityId = family.canonicalId.value,
            canonicalName = family.canonicalName,
            normalizedName = normalizedName,
            queryTerms = queryTerms(family),
        )
    }

    fun validateResume(
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        existing: List<HimEvidenceAlignmentCatalogAuditShardResultV1>,
    ): List<String> {
        plan.validate()
        val seen = mutableSetOf<String>()
        existing.forEach { result ->
            require(seen.add(result.shardId)) { "Duplicate shard result: ${result.shardId}" }
            result.validateAgainst(plan)
            require(result.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE) {
                "Only validated complete shards may be resumed."
            }
        }
        return existing.map { it.shardId }.sorted()
    }

    fun shardResult(
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        shardId: String,
        cells: List<HimEvidenceAlignmentCatalogAuditCellV1>,
    ): HimEvidenceAlignmentCatalogAuditShardResultV1 {
        val shard = plan.shards.singleOrNull { it.shardId == shardId }
            ?: error("Unknown shard: $shardId")
        val ordered = orderCells(plan, shard.canonicalEntityIds, cells)
        val complete = ordered.size == shard.canonicalCount * SOURCE_ORDER.size &&
            ordered.all { it.completed } && ordered.none { it.technicalErrors > 0 }
        val unsigned = HimEvidenceAlignmentCatalogAuditShardResultV1(
            missionDigest = plan.missionDigest,
            shardId = shardId,
            shardBindingDigest = HimEvidenceAlignmentCatalogAuditPersistenceV1.shardBindingDigest(plan, shard),
            state = if (complete) HimEvidenceAlignmentCatalogAuditState.COMPLETE
            else HimEvidenceAlignmentCatalogAuditState.PARTIAL,
            cells = ordered,
            counters = counters(plan, ordered, 1, if (complete) 1 else 0, shard.canonicalCount),
            logicalDigest = "",
        )
        return unsigned.copy(
            logicalDigest = HimEvidenceAlignmentCatalogAuditPersistenceV1.logicalDigest(unsigned),
        ).also { it.validateAgainst(plan) }
    }

    fun aggregate(
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        shardResults: List<HimEvidenceAlignmentCatalogAuditShardResultV1>,
    ): HimEvidenceAlignmentCatalogAuditAggregateV1 {
        plan.validate()
        val byId = shardResults.associateBy { it.shardId }
        require(byId.size == shardResults.size) { "Duplicate shard result." }
        shardResults.forEach { it.validateAgainst(plan) }
        val cells = shardResults.flatMap { it.cells }
        val complete = byId.keys == plan.shards.map { it.shardId }.toSet() &&
            shardResults.all { it.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE } &&
            cells.size == plan.expectedCanonicalCount * SOURCE_ORDER.size &&
            cells.all { it.completed } && cells.none { it.technicalErrors > 0 }
        val unsigned = HimEvidenceAlignmentCatalogAuditAggregateV1(
            missionDigest = plan.missionDigest,
            state = if (complete) HimEvidenceAlignmentCatalogAuditState.COMPLETE
            else HimEvidenceAlignmentCatalogAuditState.PARTIAL,
            shardResults = shardResults.sortedBy { it.shardId },
            counters = counters(plan, cells, plan.shards.size, shardResults.count {
                it.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE
            }, plan.expectedCanonicalCount),
            logicalDigest = "",
        )
        return unsigned.copy(
            logicalDigest = HimEvidenceAlignmentCatalogAuditPersistenceV1.logicalDigest(unsigned),
        ).also { it.validateAgainst(plan) }
    }

    private fun queryPlan(
        canonical: HimEvidenceAlignmentCatalogAuditCanonicalV1,
        source: HimGroundTruthSource,
    ) = HimEvidenceAlignmentCatalogAuditQueryPlanV1(
        entityId = canonical.entityId,
        source = source,
        terms = canonical.queryTerms,
        maxResultsPerQuery = MAX_RESULTS_PER_QUERY,
        maxFetches = canonical.queryTerms.size * MAX_RESULTS_PER_QUERY,
    )

    private fun queryTerms(family: HimCanonicalFamily): List<HimEvidenceAlignmentCatalogAuditQueryTermV1> {
        val candidates = buildList {
            add(HimEvidenceAlignmentCatalogAuditQueryTermV1(
                family.canonicalName, normalizeQuery(family.canonicalName),
                HimEvidenceAlignmentCatalogAuditQueryTermKind.CANONICAL_NAME,
            ))
            family.identities.forEach { identity ->
                add(HimEvidenceAlignmentCatalogAuditQueryTermV1(identity.identityName, normalizeQuery(identity.identityName), HimEvidenceAlignmentCatalogAuditQueryTermKind.IDENTITY))
                identity.variants.forEach { add(HimEvidenceAlignmentCatalogAuditQueryTermV1(it.variantName, normalizeQuery(it.variantName), HimEvidenceAlignmentCatalogAuditQueryTermKind.VARIANT)) }
                identity.aliases.forEach { add(HimEvidenceAlignmentCatalogAuditQueryTermV1(it.aliasName, normalizeQuery(it.aliasName), HimEvidenceAlignmentCatalogAuditQueryTermKind.ALIAS)) }
            }
            family.variants.forEach { add(HimEvidenceAlignmentCatalogAuditQueryTermV1(it.variantName, normalizeQuery(it.variantName), HimEvidenceAlignmentCatalogAuditQueryTermKind.VARIANT)) }
            family.aliases.forEach { add(HimEvidenceAlignmentCatalogAuditQueryTermV1(it.aliasName, normalizeQuery(it.aliasName), HimEvidenceAlignmentCatalogAuditQueryTermKind.ALIAS)) }
        }
        val result = candidates.filter { it.normalizedTerm.isNotEmpty() }
            .distinctBy { it.normalizedTerm }
        require(result.size <= MAX_QUERY_TERMS_PER_SOURCE) {
            "Unbounded query plan rejected for ${family.canonicalId.value}."
        }
        return result
    }

    private fun canonicalDescriptorSortKey(value: String) = value

    private fun orderCells(
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        canonicalIds: List<String>,
        cells: List<HimEvidenceAlignmentCatalogAuditCellV1>,
    ): List<HimEvidenceAlignmentCatalogAuditCellV1> {
        val expected = canonicalIds.flatMap { id -> SOURCE_ORDER.map { id to it } }.toSet()
        val actual = cells.map { it.entityId to it.source }
        require(actual.size == actual.toSet().size) { "Duplicate canonical/source evaluation." }
        require(actual.all { it in expected }) { "Cell is outside the shard." }
        val index = plan.canonicalOrder.mapIndexed { i, c -> c.entityId to i }.toMap()
        return cells.sortedWith(compareBy({ index.getValue(it.entityId) }, { SOURCE_ORDER.indexOf(it.source) }))
    }

    private fun counters(
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        cells: List<HimEvidenceAlignmentCatalogAuditCellV1>,
        shardsExpected: Int,
        shardsProcessed: Int,
        canonicalTotal: Int,
    ): HimEvidenceAlignmentCatalogAuditCountersV1 {
        val completeCanonicalCount = cells.map { it.entityId }.distinct().count { id ->
            SOURCE_ORDER.all { source -> cells.any { it.entityId == id && it.source == source && it.completed } }
        }
        val expectedCells = canonicalTotal * SOURCE_ORDER.size
        return HimEvidenceAlignmentCatalogAuditCountersV1(
            canonicalsTotal = plan.expectedCanonicalCount,
            canonicalsProcessed = completeCanonicalCount,
            shardsExpected = shardsExpected,
            shardsProcessed = shardsProcessed,
            sourcesExpected = SOURCE_ORDER.size,
            sourcesProcessed = SOURCE_ORDER.count { source -> cells.any { it.source == source && it.completed } },
            queries = cells.sumOf { it.queries },
            retrievalHits = cells.sumOf { it.retrievalHits },
            deduplicatedProjections = cells.sumOf { it.deduplicatedProjections },
            fetches = cells.sumOf { it.fetches },
            directSupported = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED } },
            directRejected = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED } },
            modifierOnly = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.MODIFIER_ONLY } },
            unresolvedPrimaryIdentity = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY } },
            noRetrievalHit = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT } },
            invalidOrUnsupported = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.INVALID_PROJECTION || f.classification == HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND } },
            coverageGaps = (expectedCells - cells.map { it.entityId to it.source }.toSet().size).coerceAtLeast(0),
            technicalErrors = cells.sumOf { it.technicalErrors },
        )
    }
}

data class HimEvidenceAlignmentCatalogAuditFileBindingV1(
    val relativePath: String,
    val byteSize: Long,
    val sha256: String,
) {
    init {
        require(relativeRepositoryPath(relativePath))
        require(byteSize >= 0)
        require(sha256.matches(SHA256))
    }
}

data class HimEvidenceAlignmentCatalogAuditBindingsV1(
    val contractId: String = HimEvidenceAlignmentCatalogAuditContractV1.VERSION,
    val contractVersion: String = HimEvidenceAlignmentCatalogAuditContractV1.VERSION,
    val gitHead: String,
    val implementationBindingSha256: String,
    val canonicalCatalog: HimEvidenceAlignmentCatalogAuditFileBindingV1,
    val authority: HimEvidenceAlignmentCatalogAuditFileBindingV1,
    val groundTruthReleaseReference: String,
    val sourceBindings: List<HimEvidenceAlignmentCatalogAuditSourceBindingV1>,
    val evidenceAlignmentContractVersion: String = HimEvidenceAlignmentContractV1.VERSION,
    val evaluatorVersion: String = HimDeterministicEvidenceAlignmentEvaluatorV1.VERSION,
    val queryPolicyVersion: String = HimEvidenceAlignmentCatalogAuditContractV1.QUERY_POLICY_VERSION,
    val shardingPolicyVersion: String = HimEvidenceAlignmentCatalogAuditContractV1.SHARDING_POLICY_VERSION,
) {
    fun validate() {
        require(contractId == HimEvidenceAlignmentCatalogAuditContractV1.VERSION)
        require(contractVersion == HimEvidenceAlignmentCatalogAuditContractV1.VERSION)
        require(gitHead.matches(Regex("[0-9a-f]{40}")))
        require(implementationBindingSha256.matches(SHA256))
        require(groundTruthReleaseReference.matches(Regex("release:v1:[0-9a-f]{64}")))
        require(evidenceAlignmentContractVersion == HimEvidenceAlignmentContractV1.VERSION)
        require(evaluatorVersion == HimDeterministicEvidenceAlignmentEvaluatorV1.VERSION)
        require(queryPolicyVersion == HimEvidenceAlignmentCatalogAuditContractV1.QUERY_POLICY_VERSION)
        require(shardingPolicyVersion == HimEvidenceAlignmentCatalogAuditContractV1.SHARDING_POLICY_VERSION)
        require(sourceBindings.size == HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.size)
        require(sourceBindings.map { it.source } == HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER)
        sourceBindings.forEach { it.validate() }
    }
}

data class HimEvidenceAlignmentCatalogAuditSourceBindingV1(
    val source: HimGroundTruthSource,
    val indexRelativePath: String,
    val indexByteSize: Long,
    val sqliteFileSha256: String,
    val sourceArtifactPath: String,
    val sourceArtifactSha256: String,
    val schemaVersion: String,
    val indexBuildPolicyVersion: String,
    val evidenceProjectionPolicyVersion: String,
    val logicalContentSha256: String,
    val buildState: HimEvidenceRetrievalIndexBuildState,
) {
    fun validate() {
        require(relativeRepositoryPath(indexRelativePath))
        require(indexByteSize >= 0)
        require(sqliteFileSha256.matches(SHA256))
        require(relativeRepositoryPath(sourceArtifactPath))
        require(sourceArtifactPath == source.artifactPath)
        require(sourceArtifactSha256.matches(SHA256))
        require(schemaVersion.isNotBlank())
        require(indexBuildPolicyVersion.isNotBlank())
        require(evidenceProjectionPolicyVersion.isNotBlank())
        require(logicalContentSha256.matches(SHA256))
        require(buildState == HimEvidenceRetrievalIndexBuildState.VALIDATED)
    }
}

data class HimEvidenceAlignmentCatalogAuditCanonicalV1(
    val entityId: String,
    val canonicalName: String,
    val normalizedName: String,
    val queryTerms: List<HimEvidenceAlignmentCatalogAuditQueryTermV1>,
) {
    init {
        require(entityId.matches(Regex("[0-9A-Za-z]{6}")))
        require(canonicalName.isNotBlank())
        require(normalizedName.isNotBlank())
        require(queryTerms.isNotEmpty())
        require(queryTerms.first().kind == HimEvidenceAlignmentCatalogAuditQueryTermKind.CANONICAL_NAME)
        require(queryTerms.map { it.normalizedTerm }.distinct().size == queryTerms.size)
    }
}

enum class HimEvidenceAlignmentCatalogAuditQueryTermKind { CANONICAL_NAME, IDENTITY, VARIANT, ALIAS }

data class HimEvidenceAlignmentCatalogAuditQueryTermV1(
    val term: String,
    val normalizedTerm: String,
    val kind: HimEvidenceAlignmentCatalogAuditQueryTermKind,
) {
    init {
        require(term.isNotBlank())
        require(normalizedTerm == HimEvidenceAlignmentCatalogAuditContractV1.normalizeQuery(term))
        require(normalizedTerm.isNotBlank())
    }
}

data class HimEvidenceAlignmentCatalogAuditQueryPlanV1(
    val entityId: String,
    val source: HimGroundTruthSource,
    val terms: List<HimEvidenceAlignmentCatalogAuditQueryTermV1>,
    val maxResultsPerQuery: Int,
    val maxFetches: Int,
) {
    init {
        require(entityId.matches(Regex("[0-9A-Za-z]{6}")))
        require(terms.isNotEmpty() && terms.size <= HimEvidenceAlignmentCatalogAuditContractV1.MAX_QUERY_TERMS_PER_SOURCE)
        require(maxResultsPerQuery == HimEvidenceAlignmentCatalogAuditContractV1.MAX_RESULTS_PER_QUERY)
        require(maxFetches == terms.size * maxResultsPerQuery)
    }
}

data class HimEvidenceAlignmentCatalogAuditShardV1(
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
        require(canonicalCount == endExclusive - startInclusive)
        require(canonicalCount == canonicalEntityIds.size)
        require(canonicalEntityIds.distinct().size == canonicalEntityIds.size)
        require(canonicalOrderDigest.matches(SHA256))
    }
}

data class HimEvidenceAlignmentCatalogAuditMissionPlanV1(
    val bindings: HimEvidenceAlignmentCatalogAuditBindingsV1,
    val canonicalOrder: List<HimEvidenceAlignmentCatalogAuditCanonicalV1>,
    val canonicalOrderDigest: String,
    val expectedCanonicalCount: Int,
    val queryPlans: List<HimEvidenceAlignmentCatalogAuditQueryPlanV1>,
    val maxItemsPerShard: Int,
    val shards: List<HimEvidenceAlignmentCatalogAuditShardV1>,
    val missionDigest: String,
) {
    fun validate() {
        bindings.validate()
        require(expectedCanonicalCount == canonicalOrder.size)
        require(canonicalOrderDigest.matches(SHA256))
        require(canonicalOrder.map { it.entityId }.distinct().size == canonicalOrder.size)
        require(queryPlans.size == canonicalOrder.size * HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.size)
        require(queryPlans.map { it.entityId to it.source }.distinct().size == queryPlans.size)
        require(maxItemsPerShard > 0)
        require(shards.flatMap { it.canonicalEntityIds } == canonicalOrder.map { it.entityId })
        require(shards.zipWithNext().all { it.first.endExclusive == it.second.startInclusive })
        require(missionDigest.isEmpty() || missionDigest.matches(SHA256))
        require(
            missionDigest.isEmpty() ||
                missionDigest == HimEvidenceAlignmentCatalogAuditPersistenceV1.logicalDigest(copy(missionDigest = "")),
        )
    }
}

enum class HimEvidenceAlignmentCatalogAuditState { PARTIAL, COMPLETE }

enum class HimEvidenceAlignmentAuditClassification {
    DIRECT_SUPPORTED,
    DIRECT_REJECTED,
    MODIFIER_ONLY,
    UNRESOLVED_PRIMARY_IDENTITY,
    NO_RETRIEVAL_HIT,
    UNSUPPORTED_RECORD_KIND,
    INVALID_PROJECTION,
    SOURCE_OR_BINDING_FAILURE,
}

data class HimEvidenceAlignmentCatalogAuditFindingV1(
    val entityId: String,
    val source: HimGroundTruthSource,
    val classification: HimEvidenceAlignmentAuditClassification,
    val evidenceReference: String?,
    val primaryIdentity: String?,
) {
    init {
        require(entityId.matches(Regex("[0-9A-Za-z]{6}")))
        require(evidenceReference == null || evidenceReference.isNotBlank())
    }
}

data class HimEvidenceAlignmentCatalogAuditModifierCoverageV1(
    val modifier: String,
    val coveredByAuthority: Boolean,
) {
    init { require(modifier.isNotBlank()) }
}

data class HimEvidenceAlignmentCatalogAuditCellV1(
    val entityId: String,
    val source: HimGroundTruthSource,
    val completed: Boolean,
    val queries: Int,
    val retrievalHits: Int,
    val deduplicatedProjections: Int,
    val fetches: Int,
    val findings: List<HimEvidenceAlignmentCatalogAuditFindingV1>,
    val modifierCoverage: List<HimEvidenceAlignmentCatalogAuditModifierCoverageV1>,
    val technicalValid: Boolean,
    val technicalErrors: Int,
) {
    init {
        require(entityId.matches(Regex("[0-9A-Za-z]{6}")))
        require(queries >= 0 && retrievalHits >= 0 && deduplicatedProjections >= 0 && fetches >= 0)
        require(retrievalHits <= queries * HimEvidenceAlignmentCatalogAuditContractV1.MAX_RESULTS_PER_QUERY)
        require(deduplicatedProjections <= retrievalHits)
        require(fetches <= deduplicatedProjections)
        require(technicalErrors >= 0)
        require(technicalValid == (technicalErrors == 0))
        require(findings.all { it.entityId == entityId && it.source == source })
        require(findings.mapNotNull { it.evidenceReference }.distinct().size == findings.count { it.evidenceReference != null })
    }
}

data class HimEvidenceAlignmentCatalogAuditCountersV1(
    val canonicalsTotal: Int,
    val canonicalsProcessed: Int,
    val shardsExpected: Int,
    val shardsProcessed: Int,
    val sourcesExpected: Int,
    val sourcesProcessed: Int,
    val queries: Int,
    val retrievalHits: Int,
    val deduplicatedProjections: Int,
    val fetches: Int,
    val directSupported: Int,
    val directRejected: Int,
    val modifierOnly: Int,
    val unresolvedPrimaryIdentity: Int,
    val noRetrievalHit: Int,
    val invalidOrUnsupported: Int,
    val coverageGaps: Int,
    val technicalErrors: Int,
) {
    init {
        val values = listOf(canonicalsTotal, canonicalsProcessed, shardsExpected, shardsProcessed,
            sourcesExpected, sourcesProcessed, queries, retrievalHits, deduplicatedProjections,
            fetches, directSupported, directRejected, modifierOnly, unresolvedPrimaryIdentity,
            noRetrievalHit, invalidOrUnsupported, coverageGaps, technicalErrors)
        require(values.all { it >= 0 })
        require(canonicalsProcessed <= canonicalsTotal)
        require(shardsProcessed <= shardsExpected)
        require(sourcesProcessed <= sourcesExpected)
    }
}

data class HimEvidenceAlignmentCatalogAuditShardResultV1(
    val missionDigest: String,
    val shardId: String,
    val shardBindingDigest: String,
    val state: HimEvidenceAlignmentCatalogAuditState,
    val cells: List<HimEvidenceAlignmentCatalogAuditCellV1>,
    val counters: HimEvidenceAlignmentCatalogAuditCountersV1,
    val logicalDigest: String,
) {
    fun validateAgainst(plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1) {
        plan.validate()
        val shard = plan.shards.singleOrNull { it.shardId == shardId } ?: error("Unknown shard: $shardId")
        require(missionDigest == plan.missionDigest)
        require(shardBindingDigest == HimEvidenceAlignmentCatalogAuditPersistenceV1.shardBindingDigest(plan, shard))
        require(cells.map { it.entityId to it.source }.distinct().size == cells.size)
        require(counters == expectedCounters(plan, cells, 1, if (state == HimEvidenceAlignmentCatalogAuditState.COMPLETE) 1 else 0, shard.canonicalCount))
        require(logicalDigest.matches(SHA256))
        require(logicalDigest == HimEvidenceAlignmentCatalogAuditPersistenceV1.logicalDigest(copy(logicalDigest = "")))
        if (state == HimEvidenceAlignmentCatalogAuditState.COMPLETE) {
            require(cells.size == shard.canonicalCount * HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.size)
            require(cells.all { it.completed })
            require(counters.coverageGaps == 0 && counters.technicalErrors == 0)
        }
    }
}

data class HimEvidenceAlignmentCatalogAuditAggregateV1(
    val missionDigest: String,
    val state: HimEvidenceAlignmentCatalogAuditState,
    val shardResults: List<HimEvidenceAlignmentCatalogAuditShardResultV1>,
    val counters: HimEvidenceAlignmentCatalogAuditCountersV1,
    val logicalDigest: String,
) {
    fun validateAgainst(plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1) {
        plan.validate()
        require(missionDigest == plan.missionDigest)
        require(shardResults.map { it.shardId }.distinct().size == shardResults.size)
        shardResults.forEach { it.validateAgainst(plan) }
        require(
            counters == expectedCounters(
                plan,
                shardResults.flatMap { it.cells },
                plan.shards.size,
                shardResults.count { it.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE },
                plan.expectedCanonicalCount,
            ),
        )
        require(logicalDigest.matches(SHA256))
        require(logicalDigest == HimEvidenceAlignmentCatalogAuditPersistenceV1.logicalDigest(copy(logicalDigest = "")))
        val expectedState = if (
            shardResults.size == plan.shards.size &&
            shardResults.all { it.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE } &&
            counters.coverageGaps == 0 && counters.technicalErrors == 0
        ) HimEvidenceAlignmentCatalogAuditState.COMPLETE else HimEvidenceAlignmentCatalogAuditState.PARTIAL
        require(state == expectedState)
        if (state == HimEvidenceAlignmentCatalogAuditState.COMPLETE) {
            require(counters.canonicalsProcessed == plan.expectedCanonicalCount)
            require(counters.sourcesProcessed == HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.size)
        }
    }
}

private fun expectedCounters(
    plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
    cells: List<HimEvidenceAlignmentCatalogAuditCellV1>,
    shardsExpected: Int,
    shardsProcessed: Int,
    canonicalTotal: Int,
) = run {
    val expectedCells = canonicalTotal * HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.size
    val completeIds = cells.map { it.entityId }.distinct().count { id ->
        HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.all { source -> cells.any { it.entityId == id && it.source == source && it.completed } }
    }
    HimEvidenceAlignmentCatalogAuditCountersV1(
        canonicalsTotal = plan.expectedCanonicalCount,
        canonicalsProcessed = completeIds,
        shardsExpected = shardsExpected,
        shardsProcessed = shardsProcessed,
        sourcesExpected = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.size,
        sourcesProcessed = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.count { source -> cells.any { it.source == source && it.completed } },
        queries = cells.sumOf { it.queries },
        retrievalHits = cells.sumOf { it.retrievalHits },
        deduplicatedProjections = cells.sumOf { it.deduplicatedProjections },
        fetches = cells.sumOf { it.fetches },
        directSupported = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED } },
        directRejected = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED } },
        modifierOnly = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.MODIFIER_ONLY } },
        unresolvedPrimaryIdentity = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY } },
        noRetrievalHit = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT } },
        invalidOrUnsupported = cells.sumOf { it.findings.count { f -> f.classification == HimEvidenceAlignmentAuditClassification.INVALID_PROJECTION || f.classification == HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND } },
        coverageGaps = (expectedCells - cells.map { it.entityId to it.source }.toSet().size).coerceAtLeast(0),
        technicalErrors = cells.sumOf { it.technicalErrors },
    )
}

private fun relativeRepositoryPath(value: String): Boolean =
    value.isNotBlank() && !value.startsWith("/") && !value.contains('\\') &&
        value.split('/').none { it.isBlank() || it == "." || it == ".." }

private val SHA256 = Regex("[0-9a-f]{64}")
