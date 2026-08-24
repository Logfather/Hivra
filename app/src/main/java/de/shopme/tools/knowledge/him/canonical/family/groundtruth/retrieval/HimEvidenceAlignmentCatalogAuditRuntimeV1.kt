package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object HimEvidenceAlignmentCatalogAuditPathsV1 {
    const val REPORT_ROOT = "build/knowledge/reports/him/evidence-alignment/catalog-audit/v1"
    const val MISSION_FILE_NAME = "mission.v1.json"
    const val SHARDS_DIRECTORY_NAME = "shards"
    const val AGGREGATE_FILE_NAME = "aggregate.v1.json"
    const val AGGREGATE_TEXT_FILE_NAME = "aggregate.v1.txt"

    fun mission(root: File): File = root.resolve(REPORT_ROOT).resolve(MISSION_FILE_NAME)

    fun shard(root: File, shardId: String): File {
        require(shardId.matches(Regex("shard-[0-9]{6}")))
        return root.resolve(REPORT_ROOT).resolve(SHARDS_DIRECTORY_NAME)
            .resolve("$shardId.result.v1.json")
    }

    fun aggregate(root: File): File = root.resolve(REPORT_ROOT).resolve(AGGREGATE_FILE_NAME)

    fun aggregateText(root: File): File = root.resolve(REPORT_ROOT).resolve(AGGREGATE_TEXT_FILE_NAME)
}

data class HimEvidenceAlignmentCatalogAuditRuntimeGateV1(
    val sourceIntegrationEnabled: Boolean,
    val catalogAuditEnabled: Boolean,
    val confirmation: String?,
) {
    val enabled: Boolean
        get() = sourceIntegrationEnabled &&
            catalogAuditEnabled &&
            confirmation == CONFIRMATION

    companion object {
        const val SOURCE_INTEGRATION_PROPERTY = "him.sourceIntegration.enabled"
        const val CATALOG_AUDIT_PROPERTY = "him.evidenceAlignmentCatalogAudit.enabled"
        const val CONFIRMATION_PROPERTY = "him.evidenceAlignmentCatalogAudit.confirmation"
        const val SHARD_ID_PROPERTY = "him.evidenceAlignmentCatalogAudit.shardId"
        const val CONFIRMATION = "AUTHORIZED_FULL_CATALOG_OFFLINE_AUDIT"

        fun fromProperties(get: (String) -> String? = System::getProperty) =
            HimEvidenceAlignmentCatalogAuditRuntimeGateV1(
                sourceIntegrationEnabled = get(SOURCE_INTEGRATION_PROPERTY) == "true",
                catalogAuditEnabled = get(CATALOG_AUDIT_PROPERTY) == "true",
                confirmation = get(CONFIRMATION_PROPERTY),
            )
    }
}

sealed interface HimEvidenceAlignmentCatalogAuditRuntimeResult<out T> {
    data class Skipped(val reason: String) : HimEvidenceAlignmentCatalogAuditRuntimeResult<Nothing>
    data class Failed(val reason: String) : HimEvidenceAlignmentCatalogAuditRuntimeResult<Nothing>
    data class Completed<T>(val value: T) : HimEvidenceAlignmentCatalogAuditRuntimeResult<T>
}

data class HimEvidenceAlignmentCatalogAuditMissionFreezeRequestV1(
    val root: File,
    val gitHead: String,
    val catalogRelativePath: String,
    val authorityRelativePath: String,
    val groundTruthReleaseReference: String,
    val implementationManifestRelativePaths: List<String>,
    val sourceBindings: List<HimEvidenceAlignmentCatalogAuditSourceBindingV1>,
    val authority: HimCanonicalFamilyAuthority,
    val maxItemsPerShard: Int,
)

data class HimEvidenceAlignmentCatalogAuditShardRequestV1(
    val root: File,
    val currentGitHead: String,
    val currentBindings: HimEvidenceAlignmentCatalogAuditBindingsV1,
    val catalog: HimProductOnlyCanonicalMaster,
    val authority: HimCanonicalFamilyAuthority,
    val stores: List<HimEvidenceAlignmentCatalogAuditStoreV1>,
    val shardId: String,
)

interface HimEvidenceAlignmentCatalogAuditStoreV1 {
    val source: HimGroundTruthSource
    val binding: HimEvidenceAlignmentCatalogAuditSourceBindingV1

    fun search(
        query: String,
        limit: HimEvidenceSearchLimit,
    ): List<HimEvidenceSearchResult>

    fun fetch(reference: HimEvidenceRecordReference): HimEvidenceRetrievalIndexRecord?
}

object HimEvidenceAlignmentCatalogAuditRuntimeV1 {
    const val STREAMING_DIGEST_BUFFER_BYTES = 1024 * 1024

    fun freezeMission(
        request: HimEvidenceAlignmentCatalogAuditMissionFreezeRequestV1,
        gate: HimEvidenceAlignmentCatalogAuditRuntimeGateV1,
    ): HimEvidenceAlignmentCatalogAuditRuntimeResult<HimEvidenceAlignmentCatalogAuditMissionPlanV1> {
        if (!gate.enabled) return HimEvidenceAlignmentCatalogAuditRuntimeResult.Skipped("AUDIT_OPT_IN_REQUIRED")
        return runCatching {
            require(request.gitHead.matches(Regex("[0-9a-f]{40}")))
            require(request.implementationManifestRelativePaths.isNotEmpty())
            val catalogBinding = fileBinding(request.root, request.catalogRelativePath)
            val authorityBinding = fileBinding(request.root, request.authorityRelativePath)
            val implementationBinding = implementationBinding(request.root, request.implementationManifestRelativePaths)
            val bindings = HimEvidenceAlignmentCatalogAuditBindingsV1(
                gitHead = request.gitHead,
                implementationBindingSha256 = implementationBinding,
                canonicalCatalog = catalogBinding,
                authority = authorityBinding,
                groundTruthReleaseReference = request.groundTruthReleaseReference,
                sourceBindings = request.sourceBindings,
            )
            val plan = HimEvidenceAlignmentCatalogAuditContractV1.plan(
                bindings = bindings,
                authority = request.authority,
                maxItemsPerShard = request.maxItemsPerShard,
            )
            val missionFile = HimEvidenceAlignmentCatalogAuditPathsV1.mission(request.root)
            HimEvidenceAlignmentCatalogAuditPersistenceV1.writeMission(missionFile, plan)
            HimEvidenceAlignmentCatalogAuditPersistenceV1.readMission(missionFile)
        }.fold(
            onSuccess = { HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed(it) },
            onFailure = { HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed("MISSION_FREEZE_FAILED") },
        )
    }

    fun executeShard(
        request: HimEvidenceAlignmentCatalogAuditShardRequestV1,
        gate: HimEvidenceAlignmentCatalogAuditRuntimeGateV1,
    ): HimEvidenceAlignmentCatalogAuditRuntimeResult<HimEvidenceAlignmentCatalogAuditShardResultV1> {
        if (!gate.enabled) return HimEvidenceAlignmentCatalogAuditRuntimeResult.Skipped("AUDIT_OPT_IN_REQUIRED")
        return try {
            val plan = HimEvidenceAlignmentCatalogAuditPersistenceV1.readMission(
                HimEvidenceAlignmentCatalogAuditPathsV1.mission(request.root),
            )
            require(plan.bindings.gitHead == request.currentGitHead) { "MISSION_HEAD_MISMATCH" }
            require(plan.bindings == request.currentBindings) { "MISSION_BINDING_MISMATCH" }
            require(plan.shards.any { it.shardId == request.shardId }) { "UNKNOWN_SHARD" }
            require(request.stores.map { it.source } == HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER) {
                "SOURCE_ORDER_MISMATCH"
            }
            request.stores.forEach { store ->
                require(store.binding == plan.bindings.sourceBindings.single { it.source == store.source }) {
                    "SOURCE_BINDING_MISMATCH"
                }
            }

            val output = HimEvidenceAlignmentCatalogAuditPathsV1.shard(request.root, request.shardId)
            if (output.isFile) {
                return HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed(
                    HimEvidenceAlignmentCatalogAuditPersistenceV1.readShard(output, plan),
                )
            }

            val shard = plan.shards.single { it.shardId == request.shardId }
            val stores = request.stores.associateBy { it.source }
            val families = request.authority.families.groupBy { it.canonicalId.value }
            val cells = buildList {
                shard.canonicalEntityIds.forEach { entityId ->
                    val family = families[entityId].orEmpty().singleOrNull()
                        ?: failed("SOURCE_OR_BINDING_FAILURE", request.shardId, entityId, null, null)
                    HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.forEach { source ->
                        val store = stores.getValue(source)
                        val queryPlan = plan.queryPlans.single { it.entityId == entityId && it.source == source }
                        val hitsByReference = linkedMapOf<String, HimEvidenceSearchResult>()
                        var retrievalHits = 0
                        queryPlan.terms.forEach { term ->
                            val hits = try {
                                store.search(term.term, HimEvidenceSearchLimit(queryPlan.maxResultsPerQuery))
                            } catch (_: Throwable) {
                                failed("SEARCH_FAILURE", request.shardId, entityId, source, term.normalizedTerm)
                            }
                            require(hits.size <= queryPlan.maxResultsPerQuery) { "SEARCH_LIMIT_EXCEEDED" }
                            retrievalHits += hits.size
                            hits.forEach { hit ->
                                require(hit.source == source)
                                require(hit.sourceRecordReference.source == source)
                                require(hit.recordKind.source == source)
                                hitsByReference.putIfAbsent(hit.sourceRecordReference.value, hit)
                            }
                        }

                        val evaluations = hitsByReference.keys.sorted().map { referenceValue ->
                            val reference = HimEvidenceRecordReference.parse(source, referenceValue)
                            val record = try {
                                store.fetch(reference)
                            } catch (_: Throwable) {
                                failed("FETCH_FAILURE", request.shardId, entityId, source, referenceValue)
                            } ?: failed("MISSING_PROJECTION", request.shardId, entityId, source, referenceValue)
                            require(record.sourceRecordReference == reference) { "FETCH_REFERENCE_MISMATCH" }
                            try {
                                HimDeterministicEvidenceAlignmentEvaluatorV1.evaluate(
                                    record = record,
                                    catalog = request.catalog,
                                    authority = request.authority,
                                    canonicalFamily = family,
                                )
                            } catch (_: Throwable) {
                                failed("EVALUATOR_FAILURE", request.shardId, entityId, source, referenceValue)
                            }
                        }

                        val findings = evaluations.map { evaluation ->
                            HimEvidenceAlignmentCatalogAuditFindingV1(
                                entityId = entityId,
                                source = source,
                                classification = classification(evaluation),
                                evidenceReference = evaluation.sourceRecordIdentity,
                                primaryIdentity = evaluation.primaryIdentity,
                            )
                        }.ifEmpty {
                            listOf(
                                HimEvidenceAlignmentCatalogAuditFindingV1(
                                    entityId = entityId,
                                    source = source,
                                    classification = HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT,
                                    evidenceReference = null,
                                    primaryIdentity = null,
                                ),
                            )
                        }
                        val modifierCoverage = evaluations.flatMap { evaluation ->
                            evaluation.alignment.modifierCoverage.map {
                                HimEvidenceAlignmentCatalogAuditModifierCoverageV1(
                                    modifier = it.modifier,
                                    coveredByAuthority = it.coveredByAuthority,
                                )
                            }
                        }
                        add(
                            HimEvidenceAlignmentCatalogAuditCellV1(
                                entityId = entityId,
                                source = source,
                                completed = true,
                                queries = queryPlan.terms.size,
                                retrievalHits = retrievalHits,
                                deduplicatedProjections = hitsByReference.size,
                                fetches = evaluations.size,
                                findings = findings,
                                modifierCoverage = modifierCoverage,
                                technicalValid = true,
                                technicalErrors = 0,
                            ),
                        )
                    }
                }
            }
            val result = HimEvidenceAlignmentCatalogAuditContractV1.shardResult(plan, request.shardId, cells)
            require(result.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE)
            HimEvidenceAlignmentCatalogAuditPersistenceV1.writeShard(output, plan, result)
            HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed(
                HimEvidenceAlignmentCatalogAuditPersistenceV1.readShard(output, plan),
            )
        } catch (failure: Throwable) {
            val diagnostic = failure.message?.takeIf { it.startsWith("AUDIT_FAILURE ") }
            HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed(
                diagnostic ?: failure.message?.takeIf { it in setOf("MISSION_HEAD_MISMATCH", "MISSION_BINDING_MISMATCH", "UNKNOWN_SHARD", "SOURCE_ORDER_MISMATCH", "SOURCE_BINDING_MISMATCH") }
                    ?: "SHARD_EXECUTION_FAILED",
            )
        }
    }

    fun aggregate(
        root: File,
    ): HimEvidenceAlignmentCatalogAuditRuntimeResult<HimEvidenceAlignmentCatalogAuditAggregateV1> = try {
        val plan = HimEvidenceAlignmentCatalogAuditPersistenceV1.readMission(
            HimEvidenceAlignmentCatalogAuditPathsV1.mission(root),
        )
        val results = plan.shards.map { shard ->
            val file = HimEvidenceAlignmentCatalogAuditPathsV1.shard(root, shard.shardId)
            require(file.isFile) { "MISSING_SHARD" }
            HimEvidenceAlignmentCatalogAuditPersistenceV1.readShard(file, plan)
        }
        val aggregate = HimEvidenceAlignmentCatalogAuditContractV1.aggregate(plan, results)
        require(aggregate.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE)
        val aggregateFile = HimEvidenceAlignmentCatalogAuditPathsV1.aggregate(root)
        HimEvidenceAlignmentCatalogAuditPersistenceV1.writeAggregate(aggregateFile, plan, aggregate)
        val reloaded = HimEvidenceAlignmentCatalogAuditPersistenceV1.readAggregate(aggregateFile, plan)
        writeNoOverwrite(
            HimEvidenceAlignmentCatalogAuditPathsV1.aggregateText(root),
            aggregateText(reloaded).toByteArray(StandardCharsets.UTF_8),
        )
        HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed(reloaded)
    } catch (_: Throwable) {
        HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed("AGGREGATE_FAILED")
    }

    fun streamingSha256(file: File): String = file.inputStream().use(::streamingSha256)

    fun streamingSha256(open: () -> InputStream): String = open().use(::streamingSha256)

    private fun streamingSha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(STREAMING_DIGEST_BUFFER_BYTES)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count > 0) digest.update(buffer, 0, count)
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun fileBinding(root: File, relativePath: String): HimEvidenceAlignmentCatalogAuditFileBindingV1 {
        require(isRelative(relativePath))
        val file = root.resolve(relativePath)
        require(file.isFile && file.canRead())
        return HimEvidenceAlignmentCatalogAuditFileBindingV1(relativePath, file.length(), streamingSha256(file))
    }

    private fun implementationBinding(root: File, relativePaths: List<String>): String {
        val paths = relativePaths.distinct().sorted()
        require(paths.size == relativePaths.size)
        val entries = paths.map { path ->
            val binding = fileBinding(root, path)
            "$path|${binding.byteSize}|${binding.sha256}"
        }
        return HimEvidenceAlignmentCatalogAuditPersistenceV1.sha256(entries.joinToString("\n") + "\n")
    }

    private fun classification(
        evaluation: HimDeterministicEvidenceAlignmentEvaluationV1,
    ): HimEvidenceAlignmentAuditClassification = when {
        evaluation.failureReason == "UNSUPPORTED_RECORD_KIND" -> HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND
        evaluation.failureReason == "INVALID_OR_INCOMPLETE_PROJECTION" -> HimEvidenceAlignmentAuditClassification.INVALID_PROJECTION
        evaluation.directEvidenceSupported -> HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED
        evaluation.alignment.classification == HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER -> HimEvidenceAlignmentAuditClassification.MODIFIER_ONLY
        evaluation.alignment.classification == HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY ||
            evaluation.alignment.classification == HimEvidenceAlignmentClassificationV1.MISSING_PRIMARY_IDENTITY -> HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY
        else -> HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED
    }

    private fun failed(
        reason: String,
        shardId: String,
        entityId: String,
        source: HimGroundTruthSource?,
        query: String?,
    ): Nothing = throw IllegalStateException(
        "AUDIT_FAILURE reason=$reason shard=$shardId canonical=$entityId source=${source?.name ?: "UNKNOWN"} query=${query ?: "NONE"}",
    )

    private fun aggregateText(
        aggregate: HimEvidenceAlignmentCatalogAuditAggregateV1,
    ): String = buildString {
        appendLine("HIM_EVIDENCE_ALIGNMENT_CATALOG_AUDIT_V1")
        appendLine("state=${aggregate.state.name}")
        appendLine("missionDigest=${aggregate.missionDigest}")
        appendLine("canonicalsTotal=${aggregate.counters.canonicalsTotal}")
        appendLine("canonicalsProcessed=${aggregate.counters.canonicalsProcessed}")
        appendLine("shardsExpected=${aggregate.counters.shardsExpected}")
        appendLine("shardsProcessed=${aggregate.counters.shardsProcessed}")
        appendLine("sourcesExpected=${aggregate.counters.sourcesExpected}")
        appendLine("sourcesProcessed=${aggregate.counters.sourcesProcessed}")
        appendLine("queries=${aggregate.counters.queries}")
        appendLine("retrievalHits=${aggregate.counters.retrievalHits}")
        appendLine("deduplicatedProjections=${aggregate.counters.deduplicatedProjections}")
        appendLine("fetches=${aggregate.counters.fetches}")
        appendLine("directSupported=${aggregate.counters.directSupported}")
        appendLine("directRejected=${aggregate.counters.directRejected}")
        appendLine("modifierOnly=${aggregate.counters.modifierOnly}")
        appendLine("unresolvedPrimaryIdentity=${aggregate.counters.unresolvedPrimaryIdentity}")
        appendLine("noRetrievalHit=${aggregate.counters.noRetrievalHit}")
        appendLine("invalidOrUnsupported=${aggregate.counters.invalidOrUnsupported}")
        appendLine("coverageGaps=${aggregate.counters.coverageGaps}")
        appendLine("technicalErrors=${aggregate.counters.technicalErrors}")
        appendLine("logicalDigest=${aggregate.logicalDigest}")
    }

    private fun writeNoOverwrite(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        if (file.exists()) {
            require(file.readBytes().contentEquals(bytes))
            return
        }
        val parent = requireNotNull(file.parentFile)
        val temporary = Files.createTempFile(parent.toPath(), ".${file.name}.", ".pending")
        try {
            Files.write(temporary, bytes)
            try {
                Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, file.toPath())
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun isRelative(path: String): Boolean =
        path.isNotBlank() && !path.startsWith('/') && !path.contains('\\') &&
            path.split('/').none { it.isBlank() || it == "." || it == ".." }
}
