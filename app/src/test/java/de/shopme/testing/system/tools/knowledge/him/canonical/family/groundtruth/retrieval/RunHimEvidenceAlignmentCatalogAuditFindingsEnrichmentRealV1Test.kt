package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Assume.assumeTrue
import java.io.File

/**
 * Explicit, real-bound entrypoints for the findings-enrichment lifecycle.
 *
 * The tests are deliberately opt-in. Without all own properties and the source
 * integration gate they are skipped before any real catalog, authority, index,
 * or enrichment artifact is opened or written.
 */
class RunHimEvidenceAlignmentCatalogAuditFindingsEnrichmentRealV1Test {
    @Test
    fun `enrichment gate skips without explicit opt in`() {
        val gate = gate(null, null, null)
        assertFalse(gate.enabled)
        assertFalse(gate.confirmation == ENRICHMENT_CONFIRMATION)
        assertTrue(gate.shardId == null)
    }

    @Test
    fun `writes current real-bound findings enrichment mission without stores or fetches`() {
        requireSourceIntegration()
        requireEnrichmentGate(requireShard = false)

        val root = projectRoot()
        val audit = loadAuditArtifacts(root)
        val mission = buildEnrichmentMission(root, audit)
        require(mission.provenance.auditHead == audit.plan.bindings.gitHead) {
            "AUDIT_HEAD_MISMATCH"
        }
        require(mission.provenance.enrichmentImplementationHead == audit.currentHead) {
            "IMPLEMENTATION_HEAD_MISMATCH"
        }
        val output = enrichmentMission(root)
        val before = output.takeIf { it.isFile }?.readBytes()

        HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.writeMission(output, mission)
        val reloaded = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readMission(output)
        require(reloaded == mission) { "RELOAD_VALIDATION_FAILED" }
        val after = output.readBytes()
        if (before != null) require(before.contentEquals(after)) { "MISSION_RESULT_ALREADY_EXISTS" }
        require(reloaded.provenance.auditShardBindings.size == AUDIT_SHARD_COUNT) { "AUDIT_SHARD_BINDING_MISMATCH" }
        require(reloaded.shards.map { it.startInclusive } == audit.plan.shards.map { it.startInclusive }) {
            "SHARD_BOUNDARY_MISMATCH"
        }
        require(reloaded.shards.map { it.endExclusive } == audit.plan.shards.map { it.endExclusive }) {
            "SHARD_BOUNDARY_MISMATCH"
        }
    }

    @Test
    fun `executes exactly one current real-bound findings enrichment shard`() {
        requireSourceIntegration()
        requireEnrichmentGate(requireShard = true)
        val shardId = System.getProperty(ENRICHMENT_SHARD_ID_PROPERTY)
            ?: fail("SHARD_ID_REQUIRED")

        val root = projectRoot()
        val audit = loadAuditArtifacts(root)
        val missionFile = enrichmentMission(root)
        require(missionFile.isFile) { "ENRICHMENT_MISSION_MISSING" }
        val mission = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readMission(missionFile)
        require(mission.provenance.enrichmentImplementationHead == audit.currentHead) { "IMPLEMENTATION_HEAD_MISMATCH" }
        require(mission.auditMissionDigest == audit.plan.missionDigest) { "MISSION_BINDING_MISMATCH" }
        require(mission.provenance.auditAggregateLogicalDigest == audit.aggregate.logicalDigest) {
            "AGGREGATE_BINDING_MISMATCH"
        }
        val auditShard = audit.shards.singleOrNull { it.shardId == shardId }
            ?: fail("UNKNOWN_SHARD shard=$shardId")
        val auditShardPlan = audit.plan.shards.singleOrNull { it.shardId == shardId }
            ?: fail("UNKNOWN_SHARD_PLAN shard=$shardId")
        val enrichmentShard = mission.shards.singleOrNull { it.shardId == shardId }
            ?: fail("UNKNOWN_SHARD shard=$shardId")
        require(enrichmentShard.startInclusive == auditShardPlan.startInclusive) {
            "SHARD_BOUNDARY_MISMATCH shard=$shardId"
        }
        require(enrichmentShard.endExclusive == auditShardPlan.endExclusive) {
            "SHARD_BOUNDARY_MISMATCH shard=$shardId"
        }

        val (catalog, authority) = loadCatalogAndAuthority(root, mission)
        val output = enrichmentShard(root, shardId)
        val result = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeV1.executeShard(
            HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeRequestV1(
                enabled = true,
                mission = mission,
                auditPlan = audit.plan,
                auditAggregate = audit.aggregate,
                auditShard = auditShard,
                catalog = catalog,
                authority = authority,
                fetchPorts = openRealExactFetchPorts(root, mission),
                outputFile = output,
            ),
        )
        val shardResult = enrichmentValue(result)
        require(shardResult.shardId == shardId) { "SHARD_BINDING_MISMATCH shard=$shardId" }
        require(shardResult.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE) { "SHARD_INCOMPLETE shard=$shardId" }
        require(shardResult.missionDigest == mission.logicalDigest) { "MISSION_BINDING_MISMATCH shard=$shardId" }
        require(shardResult.findings.map { it.findingOccurrenceId }.distinct().size == shardResult.findings.size) {
            "FINDING_OCCURRENCE_DUPLICATE shard=$shardId"
        }
        require(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readShard(output, mission) == shardResult) {
            "RELOAD_VALIDATION_FAILED shard=$shardId"
        }
    }

    @Test
    fun `aggregates exactly eleven current real-bound findings enrichment shards without stores`() {
        requireSourceIntegration()
        requireEnrichmentGate(requireShard = false)

        val root = projectRoot()
        val audit = loadAuditArtifacts(root)
        val missionFile = enrichmentMission(root)
        require(missionFile.isFile) { "ENRICHMENT_MISSION_MISSING" }
        val mission = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readMission(missionFile)
        require(mission.provenance.enrichmentImplementationHead == audit.currentHead) { "IMPLEMENTATION_HEAD_MISMATCH" }
        val shardDirectory = enrichmentShardDirectory(root)
        val expectedNames = mission.shards.map { "${it.shardId}.result.v1.json" }.toSet()
        val actualNames = shardDirectory.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".result.v1.json") }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
        require(actualNames == expectedNames) { "ENRICHMENT_SHARD_SET_MISMATCH" }
        val shards = mission.shards.map { shard ->
            HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readShard(
                enrichmentShard(root, shard.shardId),
                mission,
            )
        }
        require(shards.size == AUDIT_SHARD_COUNT) { "ENRICHMENT_SHARD_COUNT_MISMATCH" }
        require(shards.all { it.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE }) {
            "ENRICHMENT_SHARD_INCOMPLETE"
        }

        val aggregate = aggregateFrom(mission, shards)
        val output = enrichmentAggregate(root)
        val before = output.takeIf { it.isFile }?.readBytes()
        HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.writeAggregate(output, mission, aggregate)
        val reloaded = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readAggregate(output, mission)
        require(reloaded == aggregate) { "RELOAD_VALIDATION_FAILED" }
        if (before != null) require(before.contentEquals(output.readBytes())) { "AGGREGATE_RESULT_ALREADY_EXISTS" }
    }

    @Test
    fun `typed runtime diagnostics do not expose exception text or paths`() {
        val failed = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Failed(
            HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.SOURCE_BINDING_MISMATCH,
            "shard=shard-000001 source=CIQUAL entity=abc123 reference=ciqual:food:1",
        )
        val message = diagnostic(failed)
        assertEquals(
            "SOURCE_BINDING_MISMATCH shard=shard-000001 source=CIQUAL entity=abc123 reference=ciqual:food:1",
            message,
        )
        assertFalse(message.contains("/"))
        assertFalse(message.contains("Exception"))
        assertFalse(message.contains("Throwable"))
    }

    @Test
    fun `exact fetch port has no search operation`() {
        assertTrue(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentExactFetchPortV1::class.java.declaredMethods.none { it.name == "search" })
    }

    private fun requireSourceIntegration() = HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()

    private fun requireEnrichmentGate(requireShard: Boolean): String? {
        val current = gate(
            System.getProperty(ENRICHMENT_ENABLED_PROPERTY),
            System.getProperty(ENRICHMENT_CONFIRMATION_PROPERTY),
            System.getProperty(ENRICHMENT_SHARD_ID_PROPERTY),
        )
        assumeTrue(current.enabled)
        assumeTrue(current.confirmation == ENRICHMENT_CONFIRMATION)
        if (!requireShard) return null
        val shardId = current.shardId
        require(shardId != null && shardId.matches(SHARD_ID)) { "SHARD_ID_REQUIRED" }
        return shardId
    }

    private fun gate(enabled: String?, confirmation: String?, shardId: String?) = EnrichmentGate(
        enabled = enabled == "true",
        confirmation = confirmation,
        shardId = shardId,
    )

    private fun diagnostic(
        result: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult<*>,
    ): String = when (result) {
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Completed -> "COMPLETED"
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Failed ->
            "${result.reason.name} ${result.safeContext}"
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Skipped -> result.reason
    }

    private fun enrichmentValue(
        result: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1>,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1 = when (result) {
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Completed -> result.value
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Failed ->
            fail("${result.reason.name} ${result.safeContext}")
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Skipped -> fail(result.reason)
    }

    private fun loadAuditArtifacts(root: File): AuditArtifacts {
        val currentHead = git(root, "rev-parse", "HEAD")
        val missionFile = HimEvidenceAlignmentCatalogAuditPathsV1.mission(root)
        val aggregateFile = HimEvidenceAlignmentCatalogAuditPathsV1.aggregate(root)
        val aggregateTextFile = HimEvidenceAlignmentCatalogAuditPathsV1.aggregateText(root)
        require(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(missionFile) == FROZEN_MISSION_SHA256) {
            "MISSION_BINDING_MISMATCH"
        }
        require(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(aggregateFile) == FROZEN_AGGREGATE_SHA256) {
            "AGGREGATE_BINDING_MISMATCH"
        }
        require(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(aggregateTextFile) == FROZEN_AGGREGATE_TEXT_SHA256) {
            "AGGREGATE_TEXT_BINDING_MISMATCH"
        }
        val plan = HimEvidenceAlignmentCatalogAuditPersistenceV1.readMission(missionFile)
        require(plan.missionDigest == FROZEN_MISSION_LOGICAL_DIGEST) { "MISSION_BINDING_MISMATCH" }
        val aggregate = HimEvidenceAlignmentCatalogAuditPersistenceV1.readAggregate(aggregateFile, plan)
        require(aggregate.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE) { "AGGREGATE_INCOMPLETE" }
        val shardDirectory = root.resolve(HimEvidenceAlignmentCatalogAuditPathsV1.REPORT_ROOT)
            .resolve(HimEvidenceAlignmentCatalogAuditPathsV1.SHARDS_DIRECTORY_NAME)
        val names = shardDirectory.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".result.v1.json") }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
        val expectedNames = plan.shards.map { "${it.shardId}.result.v1.json" }.toSet()
        require(names == expectedNames && names.size == AUDIT_SHARD_COUNT) { "AUDIT_SHARD_SET_MISMATCH" }
        val shards = plan.shards.map { shard ->
            HimEvidenceAlignmentCatalogAuditPersistenceV1.readShard(
                HimEvidenceAlignmentCatalogAuditPathsV1.shard(root, shard.shardId),
                plan,
            )
        }
        require(shards.all { it.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE }) { "AUDIT_SHARD_INCOMPLETE" }
        require(aggregate.shardResults == shards.sortedBy { it.shardId }) { "AGGREGATE_BINDING_MISMATCH" }
        require(plan.expectedCanonicalCount == EXPECTED_CANONICAL_COUNT) { "CANONICAL_COUNT_MISMATCH" }
        require(plan.shards.size == AUDIT_SHARD_COUNT) { "AUDIT_SHARD_COUNT_MISMATCH" }
        return AuditArtifacts(root, currentHead, plan, aggregate, shards)
    }

    private fun buildEnrichmentMission(root: File, audit: AuditArtifacts) =
        HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.plan(
            auditPlan = audit.plan,
            provenance = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentProvenanceV1(
                auditHead = audit.plan.bindings.gitHead,
                enrichmentImplementationHead = audit.currentHead,
                auditMission = auditFileBinding(root, HimEvidenceAlignmentCatalogAuditPathsV1.mission(root), audit.plan.missionDigest),
                auditAggregate = auditFileBinding(root, HimEvidenceAlignmentCatalogAuditPathsV1.aggregate(root), audit.aggregate.logicalDigest),
                auditMissionLogicalDigest = audit.plan.missionDigest,
                auditAggregateLogicalDigest = audit.aggregate.logicalDigest,
                auditShardBindings = audit.plan.shards.map { shard ->
                    val result = audit.shards.single { it.shardId == shard.shardId }
                    HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardFileBindingV1(
                        shardId = shard.shardId,
                        file = auditFileBinding(root, HimEvidenceAlignmentCatalogAuditPathsV1.shard(root, shard.shardId), result.logicalDigest),
                    )
                },
                auditBindings = audit.plan.bindings,
                extractorEvaluatorImplementationSha256 = implementationBindingSha256(root),
            ),
        )

    private fun auditFileBinding(root: File, file: File, logicalDigest: String) =
        HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFileBindingV1(
            relativePath = relativePath(root, file),
            byteSize = file.length(),
            sha256 = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(file),
            logicalDigest = logicalDigest,
        )

    private fun loadCatalogAndAuthority(
        root: File,
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
    ): Pair<HimProductOnlyCanonicalMaster, HimCanonicalFamilyAuthority> {
        val paths = HimCanonicalFamilyPaths(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
        val persistence = HimCanonicalFamilyPersistence()
        val registry = persistence.readRegistry(paths.entityIdRegistry)
        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val authority = persistence.readAuthority(active.authorityFile)
        HimCanonicalFamilyValidator().validate(catalog, registry, authority)
        val catalogBinding = mission.provenance.auditBindings.canonicalCatalog
        val authorityBinding = mission.provenance.auditBindings.authority
        require(catalogBinding.relativePath == relativePath(root, paths.productOnlyMaster)) { "CATALOG_BINDING_MISMATCH" }
        require(authorityBinding.relativePath == relativePath(root, active.authorityFile)) { "AUTHORITY_BINDING_MISMATCH" }
        require(catalogBinding.byteSize == paths.productOnlyMaster.length()) { "CATALOG_BINDING_MISMATCH" }
        require(authorityBinding.byteSize == active.authorityFile.length()) { "AUTHORITY_BINDING_MISMATCH" }
        require(catalogBinding.sha256 == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(paths.productOnlyMaster)) {
            "CATALOG_BINDING_MISMATCH"
        }
        require(authorityBinding.sha256 == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(active.authorityFile)) {
            "AUTHORITY_BINDING_MISMATCH"
        }
        return catalog to authority
    }

    private fun openRealExactFetchPorts(
        root: File,
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
    ): List<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentExactFetchPortV1> {
        val bindings = mission.provenance.auditBindings.sourceBindings
        val offFile = root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX)
        val offValidation = HimOffEvidenceIndexValidator.validateReadOnly(offFile)
        validateRealIndexBinding(root, offFile, offValidation.metadata, bindings.single { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS })
        val off = HimOffSqliteEvidenceRetrievalStore.openAfterValidation(offFile, offValidation)
        val agribalyseFile = root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX)
        val agribalyseValidation = HimAgribalyseEvidenceIndexValidator.validateReadOnly(agribalyseFile)
        validateRealIndexBinding(root, agribalyseFile, agribalyseValidation.metadata, bindings.single { it.source == HimGroundTruthSource.AGRIBALYSE })
        val agribalyse = HimAgribalyseSqliteEvidenceRetrievalStore.openAfterValidation(agribalyseFile, agribalyseValidation)
        val ciqualFile = root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX)
        val ciqualValidation = HimCiqualEvidenceIndexValidator.validateReadOnly(ciqualFile)
        validateRealIndexBinding(root, ciqualFile, ciqualValidation.metadata, bindings.single { it.source == HimGroundTruthSource.CIQUAL })
        val ciqual = HimCiqualSqliteEvidenceRetrievalStore.openAfterValidation(ciqualFile, ciqualValidation)
        val giFile = root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX)
        val giValidation = HimGlycemicIndexEvidenceIndexValidator.validateReadOnly(giFile)
        validateRealIndexBinding(root, giFile, giValidation.metadata, bindings.single { it.source == HimGroundTruthSource.GLYCEMIC_INDEX })
        val gi = HimGlycemicIndexSqliteEvidenceRetrievalStore.openAfterValidation(giFile, giValidation)
        require(relativePath(root, offFile) == bindings.single { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS }.indexRelativePath) {
            "SOURCE_BINDING_MISMATCH"
        }
        require(relativePath(root, agribalyseFile) == bindings.single { it.source == HimGroundTruthSource.AGRIBALYSE }.indexRelativePath) {
            "SOURCE_BINDING_MISMATCH"
        }
        require(relativePath(root, ciqualFile) == bindings.single { it.source == HimGroundTruthSource.CIQUAL }.indexRelativePath) {
            "SOURCE_BINDING_MISMATCH"
        }
        require(relativePath(root, giFile) == bindings.single { it.source == HimGroundTruthSource.GLYCEMIC_INDEX }.indexRelativePath) {
            "SOURCE_BINDING_MISMATCH"
        }
        return listOf(
            exactPort(HimGroundTruthSource.OPEN_FOOD_FACTS) { reference -> off.fetch(reference)?.let(::toIndexRecord) },
            exactPort(HimGroundTruthSource.AGRIBALYSE) { reference -> agribalyse.fetch(reference)?.let(::toIndexRecord) },
            exactPort(HimGroundTruthSource.CIQUAL) { reference -> ciqual.fetch(reference)?.let(::toIndexRecord) },
            exactPort(HimGroundTruthSource.GLYCEMIC_INDEX) { reference -> gi.fetch(reference)?.let(::toIndexRecord) },
        )
    }

    private fun validateRealIndexBinding(
        root: File,
        file: File,
        metadata: HimEvidenceRetrievalIndexMetadata,
        binding: HimEvidenceAlignmentCatalogAuditSourceBindingV1,
    ) {
        require(metadata.source == binding.source) { "SOURCE_BINDING_MISMATCH" }
        require(relativePath(root, file) == binding.indexRelativePath) { "SOURCE_BINDING_MISMATCH" }
        require(file.length() == binding.indexByteSize) { "SOURCE_BINDING_MISMATCH" }
        require(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(file) == binding.sqliteFileSha256) {
            "SOURCE_BINDING_MISMATCH"
        }
        require(metadata.sourceArtifactPath == binding.sourceArtifactPath) { "SOURCE_BINDING_MISMATCH" }
        require(metadata.sourceArtifactSha256.value == binding.sourceArtifactSha256) { "SOURCE_BINDING_MISMATCH" }
        require(metadata.schemaVersion == binding.schemaVersion) { "SOURCE_BINDING_MISMATCH" }
        require(metadata.indexBuildPolicyVersion == binding.indexBuildPolicyVersion) { "SOURCE_BINDING_MISMATCH" }
        require(metadata.evidenceProjectionPolicyVersion == binding.evidenceProjectionPolicyVersion) { "SOURCE_BINDING_MISMATCH" }
        require(metadata.logicalContentSha256.value == binding.logicalContentSha256) { "SOURCE_BINDING_MISMATCH" }
        require(metadata.buildState == binding.buildState) { "SOURCE_BINDING_MISMATCH" }
    }

    private fun exactPort(
        source: HimGroundTruthSource,
        fetcher: (HimEvidenceRecordReference) -> HimEvidenceRetrievalIndexRecord?,
    ) = object : HimEvidenceAlignmentCatalogAuditFindingsEnrichmentExactFetchPortV1 {
        override val source = source
        override fun fetch(reference: HimEvidenceRecordReference) = fetcher(reference)
    }

    private fun toIndexRecord(result: HimEvidenceSearchResult): HimEvidenceRetrievalIndexRecord = when (result.source) {
        HimGroundTruthSource.OPEN_FOOD_FACTS -> HimOffEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson)
        HimGroundTruthSource.AGRIBALYSE -> HimAgribalyseEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson)
        HimGroundTruthSource.CIQUAL -> HimCiqualEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson, 1)
        HimGroundTruthSource.GLYCEMIC_INDEX -> HimGlycemicIndexEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson, 1)
    }

    private fun aggregateFrom(
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
        shards: List<HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1>,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1 {
        require(shards.map { it.shardId }.toSet() == mission.shards.map { it.shardId }.toSet()) {
            "ENRICHMENT_SHARD_SET_MISMATCH"
        }
        val ordered = shards.sortedBy { it.shardId }
        val counters = ordered.map { it.counters }.fold(
            HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(0, 0, 0, 0, 0, 0, 0, 0, 0),
        ) { left, right ->
            HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(
                canonicalsProcessed = left.canonicalsProcessed + right.canonicalsProcessed,
                sourcesProcessed = left.sourcesProcessed + right.sourcesProcessed,
                findingOccurrences = left.findingOccurrences + right.findingOccurrences,
                uniqueEvidenceReferences = left.uniqueEvidenceReferences + right.uniqueEvidenceReferences,
                exactFetches = left.exactFetches + right.exactFetches,
                noRetrievalHits = left.noRetrievalHits + right.noRetrievalHits,
                unsupportedRecordKinds = left.unsupportedRecordKinds + right.unsupportedRecordKinds,
                reconstructedFindings = left.reconstructedFindings + right.reconstructedFindings,
                classificationMismatches = left.classificationMismatches + right.classificationMismatches,
            )
        }
        val unsigned = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1(
            missionDigest = mission.logicalDigest,
            state = HimEvidenceAlignmentCatalogAuditState.COMPLETE,
            shardResults = ordered,
            counters = counters,
            logicalDigest = "",
        )
        return unsigned.copy(
            logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(unsigned),
        ).also { it.validateAgainst(mission) }
    }

    private fun implementationBindingSha256(root: File): String {
        val entries = ENRICHMENT_IMPLEMENTATION_PATHS.sorted().map { path ->
            val file = root.resolve(path)
            require(file.isFile && file.canRead()) { "IMPLEMENTATION_BINDING_MISMATCH" }
            "$path|${file.length()}|${HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(file)}"
        }
        return HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(entries.joinToString("\n") + "\n")
    }

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("USER_DIR_UNAVAILABLE")).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) return current
            current = current.parentFile ?: error("REPOSITORY_ROOT_NOT_FOUND")
        }
    }

    private fun relativePath(root: File, file: File): String = root.canonicalFile.toPath()
        .relativize(file.canonicalFile.toPath())
        .toString()
        .replace(File.separatorChar, '/')

    private fun git(root: File, vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git") + arguments.toList())
            .directory(root)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        require(process.waitFor() == 0) { "GIT_BINDING_FAILED" }
        require(output.matches(Regex("[0-9a-f]{40}"))) { "GIT_BINDING_FAILED" }
        return output
    }

    private fun enrichmentRoot(root: File) = root.resolve(ENRICHMENT_REPORT_ROOT)
    private fun enrichmentMission(root: File) = enrichmentRoot(root).resolve("mission.v1.json")
    private fun enrichmentShardDirectory(root: File) = enrichmentRoot(root).resolve("shards")
    private fun enrichmentShard(root: File, shardId: String) = enrichmentShardDirectory(root).resolve("$shardId.result.v1.json")
    private fun enrichmentAggregate(root: File) = enrichmentRoot(root).resolve("aggregate.v1.json")

    private data class EnrichmentGate(val enabled: Boolean, val confirmation: String?, val shardId: String?)

    private data class AuditArtifacts(
        val root: File,
        val currentHead: String,
        val plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        val aggregate: HimEvidenceAlignmentCatalogAuditAggregateV1,
        val shards: List<HimEvidenceAlignmentCatalogAuditShardResultV1>,
    )

    companion object {
        const val ENRICHMENT_ENABLED_PROPERTY = "him.evidenceAlignmentCatalogAuditFindingsEnrichment.enabled"
        const val ENRICHMENT_CONFIRMATION_PROPERTY = "him.evidenceAlignmentCatalogAuditFindingsEnrichment.confirmation"
        const val ENRICHMENT_SHARD_ID_PROPERTY = "him.evidenceAlignmentCatalogAuditFindingsEnrichment.shardId"
        const val ENRICHMENT_CONFIRMATION = "AUTHORIZED_FULL_CATALOG_FINDINGS_ENRICHMENT_OFFLINE"
        const val ENRICHMENT_REPORT_ROOT = "build/knowledge/reports/him/evidence-alignment/catalog-audit/findings-enrichment/v1"
        private const val AUDIT_SHARD_COUNT = 11
        private const val EXPECTED_CANONICAL_COUNT = 1384
        private const val FROZEN_MISSION_SHA256 = "2da7f0f7cb6205267ac070b96034aecfab0cda22f5a7fec8062df792d84baf96"
        private const val FROZEN_MISSION_LOGICAL_DIGEST = "8b68c1ab830bf0bdffdb950d8c0cc1172a2b83ee322887359baca37185701956"
        private const val FROZEN_AGGREGATE_SHA256 = "3823ef86a5b17aedc5ade88e593977b16d9fca382276e12903a374127f916c27"
        private const val FROZEN_AGGREGATE_TEXT_SHA256 = "50de3996fdce61dea9663c50345104138d2f609139e8e9ceba79f8584f320441"
        private val SHARD_ID = Regex("shard-[0-9]{6}")
        private val ENRICHMENT_IMPLEMENTATION_PATHS = listOf(
            "app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/retrieval/HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.kt",
            "app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/retrieval/HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.kt",
            "app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/retrieval/HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeV1.kt",
        )
    }
}
