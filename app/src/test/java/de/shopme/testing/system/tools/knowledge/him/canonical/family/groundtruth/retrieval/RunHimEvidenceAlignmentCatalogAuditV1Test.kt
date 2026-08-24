package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleasePersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleaseValidator
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleaseBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthArtifactsV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthRelease
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseBuildInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleasePersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseValidatorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetiredEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherPaidPilotOfflinePreflightV2
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.fail
import java.io.File
import java.io.InputStream
import java.sql.DriverManager
import java.util.TreeMap
import org.junit.Assume.assumeTrue

class RunHimEvidenceAlignmentCatalogAuditV1Test {
    private companion object {
        const val REAL_MAX_ITEMS_PER_SHARD = 128
        val HIM_SCOPE = arrayOf(
            "app/src/main/java/de/shopme/tools/knowledge/him",
            "app/src/test/java/de/shopme/testing/system/tools/knowledge/him",
        )
    }

    @Test
    fun missionFreezePerformsNoSearchOrFetch() {
        val root = fixtureRoot()
        val result = HimEvidenceAlignmentCatalogAuditRuntimeV1.freezeMission(freezeRequest(root), enabledGate())
        val completed = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(result)
        assertIs<HimEvidenceAlignmentCatalogAuditMissionPlanV1>(completed.value)
        assertTrue(HimEvidenceAlignmentCatalogAuditPathsV1.mission(root).isFile)
    }

    @Test
    fun invalidCatalogBindingHasSafeStageDiagnostic() {
        val result = HimEvidenceAlignmentCatalogAuditRuntimeV1.freezeMission(
            freezeRequest(fixtureRoot()).copy(catalogRelativePath = "missing/catalog.json"),
            enabledGate(),
        )

        assertEquals(
            HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed(
                HimEvidenceAlignmentCatalogAuditRuntimeV1.MISSION_FREEZE_CATALOG_BINDING_FAILED,
            ),
            result,
        )
    }

    @Test
    fun missingImplementationBindingHasSafeStageDiagnostic() {
        val result = HimEvidenceAlignmentCatalogAuditRuntimeV1.freezeMission(
            freezeRequest(fixtureRoot()).copy(implementationManifestRelativePaths = listOf("missing/implementation.kt")),
            enabledGate(),
        )

        assertEquals(
            HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed(
                HimEvidenceAlignmentCatalogAuditRuntimeV1.MISSION_FREEZE_IMPLEMENTATION_BINDING_FAILED,
            ),
            result,
        )
    }

    @Test
    fun invalidPlanInvariantHasSafeStageDiagnostic() {
        val result = HimEvidenceAlignmentCatalogAuditRuntimeV1.freezeMission(
            freezeRequest(fixtureRoot(), maxItemsPerShard = 0),
            enabledGate(),
        )

        assertEquals(
            HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed(
                HimEvidenceAlignmentCatalogAuditRuntimeV1.MISSION_FREEZE_PLAN_FAILED,
            ),
            result,
        )
    }

    @Test
    fun `writes current real-bound catalog alignment audit mission without executing shards`() {
        HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()

        val root = projectRoot()
        val head = git(root, "rev-parse", "HEAD")
        require(head.matches(Regex("[0-9a-f]{40}")))
        require(gitExit(root, "diff", "--quiet", "--", *HIM_SCOPE) == 0) {
            "Relevant HIM scope has uncommitted work."
        }
        require(gitExit(root, "diff", "--cached", "--quiet", "--", *HIM_SCOPE) == 0) {
            "Relevant HIM scope has staged work."
        }

        val missionFile = HimEvidenceAlignmentCatalogAuditPathsV1.mission(root)
        val shardDirectory = root.resolve(HimEvidenceAlignmentCatalogAuditPathsV1.REPORT_ROOT)
            .resolve(HimEvidenceAlignmentCatalogAuditPathsV1.SHARDS_DIRECTORY_NAME)
        val aggregateFile = HimEvidenceAlignmentCatalogAuditPathsV1.aggregate(root)
        val aggregateTextFile = HimEvidenceAlignmentCatalogAuditPathsV1.aggregateText(root)
        require(!shardDirectory.exists()) { "Real mission freeze found an existing shard directory." }
        require(!aggregateFile.exists() && !aggregateTextFile.exists()) {
            "Real mission freeze found an existing aggregate artifact."
        }
        val missionBefore = missionFile.takeIf { it.isFile }?.readBytes()

        val context = loadRealAuditContext(root, requireMission = false, openStores = false)
        val request = missionFreezeRequest(context)

        // The committed runtime API receives this explicit freeze gate. No audit property is read here;
        // the only external opt-in is HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled().
        val result = HimEvidenceAlignmentCatalogAuditRuntimeV1.freezeMission(
            request,
            HimEvidenceAlignmentCatalogAuditRuntimeGateV1(
                sourceIntegrationEnabled = true,
                catalogAuditEnabled = true,
                confirmation = HimEvidenceAlignmentCatalogAuditRuntimeGateV1.CONFIRMATION,
            ),
        )
        val plan = runtimeValue<HimEvidenceAlignmentCatalogAuditMissionPlanV1>(result)
        val missionAfter = missionFile.readBytes()
        if (missionBefore != null) assertTrue(missionBefore.contentEquals(missionAfter))
        assertEquals(plan, HimEvidenceAlignmentCatalogAuditPersistenceV1.readMission(missionFile))
        assertEquals(plan.missionDigest, HimEvidenceAlignmentCatalogAuditPersistenceV1.readMission(missionFile).missionDigest)
        assertTrue(!shardDirectory.exists())
        assertFalse(aggregateFile.exists())
        assertFalse(aggregateTextFile.exists())
    }

    @Test
    fun `executes exactly one current real-bound catalog alignment audit shard`() {
        HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val gate = realShardGate()
        require(gate.enabled)
        val shardId = System.getProperty(HimEvidenceAlignmentCatalogAuditRuntimeGateV1.SHARD_ID_PROPERTY)
            ?.takeIf { it.isNotBlank() }
            ?: error("SHARD_ID_REQUIRED")
        require(shardId.matches(Regex("shard-[0-9]{6}"))) { "INVALID_SHARD_ID" }

        val context = loadRealAuditContext(projectRoot(), requireMission = true, openStores = true)
        val mission = requireNotNull(context.mission)
        require(mission.bindings.gitHead == context.currentGitHead) { "MISSION_HEAD_MISMATCH" }
        require(mission.bindings == context.bindings) { "MISSION_BINDING_MISMATCH" }
        val shard = mission.shards.singleOrNull { it.shardId == shardId }
            ?: error("UNKNOWN_SHARD")
        val output = HimEvidenceAlignmentCatalogAuditPathsV1.shard(context.root, shardId)
        require(!output.exists()) { "SHARD_RESULT_ALREADY_EXISTS" }
        require(!HimEvidenceAlignmentCatalogAuditPathsV1.aggregate(context.root).exists()) { "AGGREGATE_ALREADY_EXISTS" }
        require(!HimEvidenceAlignmentCatalogAuditPathsV1.aggregateText(context.root).exists()) { "AGGREGATE_TEXT_ALREADY_EXISTS" }
        require(context.stores.map { it.source } == HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER) {
            "SOURCE_ORDER_MISMATCH"
        }
        require(context.stores.all { it.binding == mission.bindings.sourceBindings.single { binding -> binding.source == it.source } }) {
            "SOURCE_BINDING_MISMATCH"
        }

        val result = HimEvidenceAlignmentCatalogAuditRuntimeV1.executeShard(
            HimEvidenceAlignmentCatalogAuditShardRequestV1(
                root = context.root,
                currentGitHead = context.currentGitHead,
                currentBindings = context.bindings,
                catalog = context.catalog,
                authority = context.authority,
                stores = context.stores,
                shardId = shardId,
            ),
            gate,
        )
        val shardResult = runtimeValue<HimEvidenceAlignmentCatalogAuditShardResultV1>(result)
        require(shardResult.shardId == shardId)
        require(shardResult.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE)
        require(shardResult.missionDigest == mission.missionDigest)
        require(shardResult.shardBindingDigest == HimEvidenceAlignmentCatalogAuditPersistenceV1.shardBindingDigest(mission, shard))
        require(shardResult.cells.size == shard.canonicalCount * HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.size)
        require(shardResult.cells.map { it.entityId to it.source } == shard.canonicalEntityIds.flatMap { entityId ->
            HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.map { source -> entityId to source }
        })
        require(shardResult.cells.all { it.completed && it.technicalValid && it.technicalErrors == 0 })
        require(shardResult.counters.coverageGaps == 0)
        require(shardResult.counters.technicalErrors == 0)
        val reloaded = HimEvidenceAlignmentCatalogAuditPersistenceV1.readShard(output, mission)
        require(reloaded == shardResult)
        require(!HimEvidenceAlignmentCatalogAuditPathsV1.aggregate(context.root).exists())
        require(!HimEvidenceAlignmentCatalogAuditPathsV1.aggregateText(context.root).exists())
    }

    @Test
    fun `aggregates current complete real-bound catalog alignment audit without opening stores`() {
        assumeTrue(System.getProperty(HimEvidenceAlignmentCatalogAuditRuntimeGateV1.CATALOG_AUDIT_PROPERTY) == "true")
        assumeTrue(System.getProperty(HimEvidenceAlignmentCatalogAuditRuntimeGateV1.CONFIRMATION_PROPERTY) == HimEvidenceAlignmentCatalogAuditRuntimeGateV1.CONFIRMATION)
        val context = loadRealAuditContext(projectRoot(), requireMission = true, openStores = false)
        val mission = requireNotNull(context.mission)
        require(mission.bindings.gitHead == context.currentGitHead) { "MISSION_HEAD_MISMATCH" }
        require(mission.bindings == context.bindings) { "MISSION_BINDING_MISMATCH" }
        require(mission.shards.all { HimEvidenceAlignmentCatalogAuditPathsV1.shard(context.root, it.shardId).isFile }) {
            "MISSING_SHARD"
        }
        val aggregateFile = HimEvidenceAlignmentCatalogAuditPathsV1.aggregate(context.root)
        val aggregateTextFile = HimEvidenceAlignmentCatalogAuditPathsV1.aggregateText(context.root)
        val aggregateBefore = aggregateFile.takeIf { it.isFile }?.readBytes()
        val aggregateTextBefore = aggregateTextFile.takeIf { it.isFile }?.readBytes()

        val result = HimEvidenceAlignmentCatalogAuditRuntimeV1.aggregate(context.root)
        val aggregate = runtimeValue<HimEvidenceAlignmentCatalogAuditAggregateV1>(result)
        require(aggregate.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE)
        val reloaded = HimEvidenceAlignmentCatalogAuditPersistenceV1.readAggregate(aggregateFile, mission)
        require(reloaded == aggregate)
        require(aggregateBefore == null || aggregateBefore.contentEquals(aggregateFile.readBytes()))
        require(aggregateTextBefore == null || aggregateTextBefore.contentEquals(aggregateTextFile.readBytes()))
        require(aggregateTextFile.readText().startsWith("HIM_EVIDENCE_ALIGNMENT_CATALOG_AUDIT_V1\n"))
        require(aggregateTextFile.readText().contains("logicalDigest=${aggregate.logicalDigest}"))
    }

    @Test
    fun identicalMissionFreezeReloadsWithoutChangingBytes() {
        val root = fixtureRoot()
        val first = freeze(root)
        val bytes = HimEvidenceAlignmentCatalogAuditPathsV1.mission(root).readBytes()
        val second = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(
            HimEvidenceAlignmentCatalogAuditRuntimeV1.freezeMission(freezeRequest(root), enabledGate()),
        ).value as HimEvidenceAlignmentCatalogAuditMissionPlanV1
        assertEquals(first, second)
        assertTrue(bytes.contentEquals(HimEvidenceAlignmentCatalogAuditPathsV1.mission(root).readBytes()))
    }

    @Test
    fun largeFileDigestUsesBoundedStreamingReads() {
        val bytes = ByteArray(HimEvidenceAlignmentCatalogAuditRuntimeV1.STREAMING_DIGEST_BUFFER_BYTES * 2 + 17) { (it % 251).toByte() }
        val stream = TrackingInputStream(bytes)
        val digest = HimEvidenceAlignmentCatalogAuditRuntimeV1.streamingSha256 { stream }
        assertEquals(64, digest.length)
        assertTrue(stream.maxRequestedBytes <= HimEvidenceAlignmentCatalogAuditRuntimeV1.STREAMING_DIGEST_BUFFER_BYTES)
        assertTrue(stream.readCalls > 1)
    }

    @Test
    fun exactlyOneFrozenShardIsExecuted() {
        val root = fixtureRoot()
        val plan = freeze(root, maxItemsPerShard = 1)
        val stores = stores()
        val result = execute(root, plan, plan.shards.first().shardId, stores)
        val shard = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(result).value
        assertIs<HimEvidenceAlignmentCatalogAuditShardResultV1>(shard)
        assertEquals(1, shard.counters.canonicalsProcessed)
        assertEquals(1, shard.counters.shardsProcessed)
        assertTrue(stores.all { it.searchCalls.isNotEmpty() })
    }

    @Test
    fun onlyCanonicalsInSelectedShardAreProcessed() {
        val root = fixtureRoot()
        val plan = freeze(root, maxItemsPerShard = 1)
        val stores = stores()
        execute(root, plan, plan.shards.first().shardId, stores)
        assertEquals(1, plan.shards.first().canonicalCount)
        assertEquals(1, plan.shards.first().canonicalEntityIds.size)
        assertTrue(stores.all { it.searchCalls.size == 1 })
    }

    @Test
    fun everyCanonicalCompletesAgainstExactlyFourSources() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val stores = stores()
        val result = execute(root, plan, plan.shards.first().shardId, stores)
        val shard = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(result).value as HimEvidenceAlignmentCatalogAuditShardResultV1
        assertEquals(4, shard.cells.size)
        assertEquals(4, shard.cells.map { it.source }.distinct().size)
        assertTrue(shard.cells.all { it.completed && it.technicalValid })
    }

    @Test
    fun queryAndFetchLimitsArePassedUnchanged() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val stores = stores()
        execute(root, plan, plan.shards.first().shardId, stores)
        assertTrue(stores.all { it.limits == listOf(HimEvidenceSearchLimit.MAX_RESULTS) })
        assertTrue(stores.all { it.searchCalls.size <= HimEvidenceAlignmentCatalogAuditContractV1.MAX_QUERY_TERMS_PER_SOURCE })
    }

    @Test
    fun duplicateHitsAreDeduplicatedBeforeFetch() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val stores = stores(duplicateHits = true)
        val result = execute(root, plan, plan.shards.first().shardId, stores)
        val shard = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(result).value as HimEvidenceAlignmentCatalogAuditShardResultV1
        assertTrue(shard.cells.all { it.retrievalHits == 2 && it.deduplicatedProjections == 1 && it.fetches == 1 })
    }

    @Test
    fun eachUniqueEvidenceReferenceIsFetchedAtMostOnce() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val stores = stores(duplicateHits = true)
        execute(root, plan, plan.shards.first().shardId, stores)
        assertTrue(stores.all { it.fetchCalls.size == it.fetchCalls.distinct().size })
        assertTrue(stores.all { it.fetchCalls.size == 1 })
    }

    @Test
    fun extractorRoutingFollowsRecordKindAndKeepsCrossSourceMatrix() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val stores = stores()
        val result = execute(root, plan, plan.shards.first().shardId, stores)
        val shard = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(result).value as HimEvidenceAlignmentCatalogAuditShardResultV1
        assertEquals(1, shard.counters.directSupported)
        assertEquals(1, shard.counters.directRejected)
        assertEquals(2, shard.counters.modifierOnly)
        assertEquals(3, shard.counters.directRejected + shard.counters.modifierOnly)
    }

    @Test
    fun unsupportedRecordKindIsFailClosedWithoutDirectSupport() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val records = records().toMutableMap()
        records[HimGroundTruthSource.CIQUAL] = unsupportedCiqual()
        val stores = stores(records = records)
        val result = execute(root, plan, plan.shards.first().shardId, stores)
        val shard = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(result).value as HimEvidenceAlignmentCatalogAuditShardResultV1
        assertEquals(1, shard.counters.invalidOrUnsupported)
        assertEquals(0, shard.counters.directSupported)
    }

    @Test
    fun searchFailurePreventsCompleteAndPersistence() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val stores = stores(failSearch = HimGroundTruthSource.OPEN_FOOD_FACTS)
        val result = execute(root, plan, plan.shards.first().shardId, stores)
        assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed>(result)
        assertFalse(HimEvidenceAlignmentCatalogAuditPathsV1.shard(root, plan.shards.first().shardId).exists())
    }

    @Test
    fun fetchFailurePreventsCompleteAndPersistence() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val stores = stores(failFetch = HimGroundTruthSource.AGRIBALYSE)
        val result = execute(root, plan, plan.shards.first().shardId, stores)
        assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed>(result)
        assertFalse(HimEvidenceAlignmentCatalogAuditPathsV1.shard(root, plan.shards.first().shardId).exists())
    }

    @Test
    fun technicalFailureDiagnosticIdentifiesShardCanonicalSourceAndQuery() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val result = execute(root, plan, plan.shards.first().shardId, stores(failSearch = HimGroundTruthSource.CIQUAL))
        val failure = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed>(result)
        assertTrue(failure.reason.contains("shard=${plan.shards.first().shardId}"))
        assertTrue(failure.reason.contains("canonical=${plan.shards.first().canonicalEntityIds.first()}"))
        assertTrue(failure.reason.contains("source=CIQUAL"))
        assertTrue(failure.reason.contains("query=vanille"), failure.reason)
        assertFalse(failure.reason.contains(root.absolutePath))
    }

    @Test
    fun identicalShardRepeatReloadsWithoutProviderOrStoreCallsAndKeepsBytes() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val firstStores = stores()
        execute(root, plan, plan.shards.first().shardId, firstStores)
        val file = HimEvidenceAlignmentCatalogAuditPathsV1.shard(root, plan.shards.first().shardId)
        val bytes = file.readBytes()
        val secondStores = stores()
        val result = execute(root, plan, plan.shards.first().shardId, secondStores)
        assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(result)
        assertTrue(bytes.contentEquals(file.readBytes()))
        assertTrue(secondStores.all { it.searchCalls.isEmpty() && it.fetchCalls.isEmpty() })
    }

    @Test
    fun differingFreezeCannotOverwriteMission() {
        val root = fixtureRoot()
        val first = freeze(root)
        val original = HimEvidenceAlignmentCatalogAuditPathsV1.mission(root).readBytes()
        val changed = HimEvidenceAlignmentCatalogAuditRuntimeV1.freezeMission(
            freezeRequest(root).copy(gitHead = "b".repeat(40)),
            enabledGate(),
        )
        assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed>(changed)
        assertTrue(original.contentEquals(HimEvidenceAlignmentCatalogAuditPathsV1.mission(root).readBytes()))
        assertEquals(first.missionDigest, HimEvidenceAlignmentCatalogAuditPersistenceV1.readMission(HimEvidenceAlignmentCatalogAuditPathsV1.mission(root)).missionDigest)
    }

    @Test
    fun resumeAcceptsOnlyBindingIdenticalShard() {
        val root = fixtureRoot()
        val plan = freeze(root)
        execute(root, plan, plan.shards.first().shardId, stores())
        val valid = execute(root, plan, plan.shards.first().shardId, stores())
        assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(valid)
        val stale = execute(root, plan.copy(bindings = plan.bindings.copy(gitHead = "c".repeat(40))), plan.shards.first().shardId, stores())
        assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed>(stale)
    }

    @Test
    fun aggregateWithoutAllShardsIsRejected() {
        val root = fixtureRoot()
        val plan = freeze(root, maxItemsPerShard = 1)
        execute(root, plan, plan.shards.first().shardId, stores())
        val result = HimEvidenceAlignmentCatalogAuditRuntimeV1.aggregate(root)
        assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed>(result)
        assertFalse(HimEvidenceAlignmentCatalogAuditPathsV1.aggregate(root).exists())
    }

    @Test
    fun aggregateRequiresCompleteBindingIdenticalShards() {
        val root = fixtureRoot()
        val plan = freeze(root, maxItemsPerShard = 1)
        plan.shards.forEach { shard ->
            val result = execute(root, plan, shard.shardId, stores())
            assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(result)
        }
        val aggregate = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(
            HimEvidenceAlignmentCatalogAuditRuntimeV1.aggregate(root),
        ).value as HimEvidenceAlignmentCatalogAuditAggregateV1
        assertEquals(HimEvidenceAlignmentCatalogAuditState.COMPLETE, aggregate.state)
        assertEquals(plan.shards.size, aggregate.counters.shardsProcessed)
        assertEquals(0, aggregate.counters.coverageGaps)
    }

    @Test
    fun aggregateDoesNotOpenStoresAndSecondRunIsByteIdentical() {
        val root = fixtureRoot()
        val plan = freeze(root, maxItemsPerShard = 1)
        plan.shards.forEach { execute(root, plan, it.shardId, stores()) }
        val first = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(
            HimEvidenceAlignmentCatalogAuditRuntimeV1.aggregate(root),
        ).value as HimEvidenceAlignmentCatalogAuditAggregateV1
        val json = HimEvidenceAlignmentCatalogAuditPathsV1.aggregate(root).readBytes()
        val text = HimEvidenceAlignmentCatalogAuditPathsV1.aggregateText(root).readBytes()
        val second = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(
            HimEvidenceAlignmentCatalogAuditRuntimeV1.aggregate(root),
        ).value as HimEvidenceAlignmentCatalogAuditAggregateV1
        assertEquals(first, second)
        assertTrue(json.contentEquals(HimEvidenceAlignmentCatalogAuditPathsV1.aggregate(root).readBytes()))
        assertTrue(text.contentEquals(HimEvidenceAlignmentCatalogAuditPathsV1.aggregateText(root).readBytes()))
    }

    @Test
    fun canonicalInputOrderDoesNotChangeMissionBytes() {
        val root = fixtureRoot()
        val first = freeze(root, authority = authority())
        val firstBytes = HimEvidenceAlignmentCatalogAuditPathsV1.mission(root).readBytes()
        val secondRoot = fixtureRoot()
        val second = freeze(secondRoot, authority = authority().copy(families = authority().families.reversed()))
        val secondBytes = HimEvidenceAlignmentCatalogAuditPathsV1.mission(secondRoot).readBytes()
        assertEquals(first.missionDigest, second.missionDigest)
        assertTrue(firstBytes.contentEquals(secondBytes))
    }

    @Test
    fun sourceIntegrationEntrypointsSkipWithoutOptIn() {
        val root = fixtureRoot()
        val result = HimEvidenceAlignmentCatalogAuditRuntimeV1.freezeMission(
            freezeRequest(root),
            HimEvidenceAlignmentCatalogAuditRuntimeGateV1(false, false, null),
        )
        assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Skipped>(result)
        assertFalse(HimEvidenceAlignmentCatalogAuditPathsV1.mission(root).exists())
    }

    @Test
    fun runtimeResultCompletedReturnsTypedValue() {
        val value = runtimeValue<String>(HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed("completed-value"))
        assertEquals("completed-value", value)
    }

    @Test
    fun runtimeResultFailureAndSkipReasonsRemainExact() {
        val reasons = listOf(
            "AUDIT_FAILURE reason=EVALUATOR_FAILURE shard=shard-000001 canonical=abc source=CIQUAL query=Vanille",
            "MISSION_HEAD_MISMATCH",
            "SOURCE_BINDING_MISMATCH",
        )
        reasons.forEach { reason ->
            val failure = assertFailsWith<AssertionError> {
                runtimeValue<String>(HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed(reason))
            }
            assertEquals(reason, failure.message)
            assertFalse(failure.message.orEmpty().contains("/"))
        }

        val skipped = assertFailsWith<AssertionError> {
            runtimeValue<String>(HimEvidenceAlignmentCatalogAuditRuntimeResult.Skipped("AUDIT_OPT_IN_REQUIRED"))
        }
        assertEquals("AUDIT_OPT_IN_REQUIRED", skipped.message)
    }

    @Test
    fun shardExecutionRequiresExactConfirmationPhrase() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val result = execute(
            root,
            plan,
            plan.shards.first().shardId,
            stores(),
            HimEvidenceAlignmentCatalogAuditRuntimeGateV1(true, true, "AUTHORIZED_FULL_CATALOG_OFFLINE_AUDIT_WRONG"),
        )
        assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Skipped>(result)
        assertFalse(HimEvidenceAlignmentCatalogAuditPathsV1.shard(root, plan.shards.first().shardId).exists())
    }

    @Test
    fun runtimeDoesNotReadPaidNetworkProperties() {
        val names = mutableListOf<String>()
        val gate = HimEvidenceAlignmentCatalogAuditRuntimeGateV1.fromProperties { name ->
            names += name
            when (name) {
                HimEvidenceAlignmentCatalogAuditRuntimeGateV1.SOURCE_INTEGRATION_PROPERTY -> "true"
                HimEvidenceAlignmentCatalogAuditRuntimeGateV1.CATALOG_AUDIT_PROPERTY -> "true"
                HimEvidenceAlignmentCatalogAuditRuntimeGateV1.CONFIRMATION_PROPERTY -> HimEvidenceAlignmentCatalogAuditRuntimeGateV1.CONFIRMATION
                else -> error("unexpected property: $name")
            }
        }
        assertTrue(gate.enabled)
        assertTrue(names.all { !it.contains("paid", ignoreCase = true) && !it.contains("openai", ignoreCase = true) })
    }

    @Test
    fun noRetrievalHitIsACompleteTechnicalCellWithExplicitFinding() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val result = execute(root, plan, plan.shards.first().shardId, stores(noHits = true))
        val shard = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(result).value as HimEvidenceAlignmentCatalogAuditShardResultV1
        assertEquals(4, shard.counters.noRetrievalHit)
        assertEquals(0, shard.counters.technicalErrors)
        assertTrue(shard.cells.all { it.completed })
    }

    @Test
    fun sourceBindingMismatchFailsBeforeSearch() {
        val root = fixtureRoot()
        val plan = freeze(root)
        val stores = stores().toMutableList()
        stores[0] = stores[0].copy(binding = stores[0].binding.copy(indexRelativePath = "other.sqlite"))
        val result = execute(root, plan, plan.shards.first().shardId, stores)
        assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed>(result)
        assertTrue(stores.all { it.searchCalls.isEmpty() })
    }

    private fun execute(
        root: File,
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        shardId: String,
        stores: List<FakeStore>,
        gate: HimEvidenceAlignmentCatalogAuditRuntimeGateV1 = enabledGate(),
    ) = HimEvidenceAlignmentCatalogAuditRuntimeV1.executeShard(
        HimEvidenceAlignmentCatalogAuditShardRequestV1(
            root = root,
            currentGitHead = plan.bindings.gitHead,
            currentBindings = plan.bindings,
            catalog = catalog(),
            authority = authority(),
            stores = stores,
            shardId = shardId,
        ),
        gate,
    )

    private fun freeze(
        root: File,
        maxItemsPerShard: Int = 1,
        authority: HimCanonicalFamilyAuthority = authority(),
    ): HimEvidenceAlignmentCatalogAuditMissionPlanV1 = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(
        HimEvidenceAlignmentCatalogAuditRuntimeV1.freezeMission(
            freezeRequest(root, maxItemsPerShard, authority),
            enabledGate(),
        ),
    ).value as HimEvidenceAlignmentCatalogAuditMissionPlanV1

    private fun freezeRequest(
        root: File,
        maxItemsPerShard: Int = 1,
        authority: HimCanonicalFamilyAuthority = authority(),
    ) = HimEvidenceAlignmentCatalogAuditMissionFreezeRequestV1(
        root = root,
        gitHead = "a".repeat(40),
        catalogRelativePath = "catalog.json",
        authorityRelativePath = "authority.json",
        groundTruthReleaseReference = "release:v1:${"b".repeat(64)}",
        implementationManifestRelativePaths = listOf("implementation.kt"),
        sourceBindings = sourceBindings(),
        authority = authority,
        maxItemsPerShard = maxItemsPerShard,
    )

    private fun fixtureRoot(): File {
        val root = createTempDirectory("him-catalog-audit").toFile()
        root.resolve("catalog.json").writeText("catalog")
        root.resolve("authority.json").writeText("authority")
        root.resolve("implementation.kt").writeText("implementation")
        return root
    }

    private fun enabledGate() = HimEvidenceAlignmentCatalogAuditRuntimeGateV1(true, true, HimEvidenceAlignmentCatalogAuditRuntimeGateV1.CONFIRMATION)

    private fun catalog() = HimProductOnlyCanonicalMaster(
        path = "catalog.json",
        contentSha256 = "c".repeat(64),
        records = listOf(
            HimProductOnlyCanonical("Vanille", "vanille", emptyList()),
            HimProductOnlyCanonical("Pudding", "pudding", emptyList()),
            HimProductOnlyCanonical("Zucker", "zucker", emptyList()),
            HimProductOnlyCanonical("Keks", "keks", emptyList()),
            HimProductOnlyCanonical("Apfel", "apfel", emptyList()),
        ),
    )

    private fun authority() = HimCanonicalFamilyAuthority(
        schemaVersion = "fixture",
        sourceCatalog = HimCanonicalFamilySourceCatalog("catalog.json", "c".repeat(64), 5),
        families = listOf(
            family("a00001", "Vanille", "vanille"),
            family("b00002", "Pudding", "pudding"),
            family("c00003", "Zucker", "zucker", aliases = listOf(alias("Sucre", "c10001"), alias("Sugar", "c10002"))),
            family("d00004", "Keks", "keks", aliases = listOf(alias("Cookies", "d10001"), alias("Biscuit", "d10002"))),
            family("e00005", "Apfel", "apfel"),
        ),
    )

    private fun family(
        id: String,
        name: String,
        normalized: String,
        aliases: List<HimCanonicalAlias> = emptyList(),
    ) = HimCanonicalFamily(
        canonicalId = HimEntityId(id),
        canonicalName = name,
        normalizedName = normalized,
        taxonomyPaths = emptyList(),
        lifecycleStatus = HimLifecycleStatus.ACTIVE,
        identities = emptyList(),
        variants = emptyList(),
        aliases = aliases,
    )

    private fun alias(name: String, id: String) = HimCanonicalAlias(
        aliasId = HimEntityId(id),
        aliasName = name,
        normalizedName = name.lowercase(),
        lifecycleStatus = HimLifecycleStatus.ACTIVE,
    )

    private fun sourceBindings() = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.mapIndexed { index, source ->
        HimEvidenceAlignmentCatalogAuditSourceBindingV1(
            source = source,
            indexRelativePath = "indexes/${source.name.lowercase()}.sqlite",
            indexByteSize = (index + 1).toLong(),
            sqliteFileSha256 = "d".repeat(64),
            sourceArtifactPath = source.artifactPath,
            sourceArtifactSha256 = "e".repeat(64),
            schemaVersion = "INDEX_SCHEMA_V1",
            indexBuildPolicyVersion = "INDEX_BUILD_V1",
            evidenceProjectionPolicyVersion = "PROJECTION_V1",
            logicalContentSha256 = "f".repeat(64),
            buildState = HimEvidenceRetrievalIndexBuildState.VALIDATED,
        )
    }

    private fun validateActiveGroundTruthRelease(
        root: File,
        active: HimActiveGroundTruthArtifactsV1,
        authority: HimCanonicalFamilyAuthority,
        release: HimGroundTruthRelease,
    ) {
        val persistence = HimCanonicalFamilyPersistence()
        val registry = persistence.readRegistry(active.activeRegistryFile)
        val retired = GsonBuilder().create().fromJson(
            active.retiredRegistryBytes.toString(Charsets.UTF_8),
            HimRetiredEntityIdRegistry::class.java,
        )
        val ledger = HimCanonicalFamilyMutationLedgerPersistenceV1().read(active.mutationLedgerFile)
        val fingerprint = HimEntityFingerprintIndexPersistence().read(active.fingerprintIndexFile)
        HimGroundTruthReleaseValidatorV1().validate(
            release = release,
            input = HimGroundTruthReleaseBuildInputV1(
                authority = authority,
                activeEntityIdRegistry = registry,
                retiredEntityIdRegistry = retired,
                mutationLedger = ledger,
                entityFingerprintIndex = fingerprint,
                authorityArtifact = release.sources.canonicalFamilyAuthority,
                activeEntityIdRegistryArtifact = release.sources.activeEntityIdRegistry,
                retiredEntityIdRegistryArtifact = release.sources.retiredEntityIdRegistry,
                mutationLedgerArtifact = release.sources.mutationLedger,
                entityFingerprintIndexArtifact = release.sources.entityFingerprintIndex,
            ),
        )
        require(active.releaseDirectory.canonicalFile.path.startsWith(root.canonicalFile.path))
    }

    private data class RealAuditContext(
        val root: File,
        val currentGitHead: String,
        val catalog: HimProductOnlyCanonicalMaster,
        val authority: HimCanonicalFamilyAuthority,
        val groundTruthRelease: HimGroundTruthRelease,
        val bindings: HimEvidenceAlignmentCatalogAuditBindingsV1,
        val mission: HimEvidenceAlignmentCatalogAuditMissionPlanV1?,
        val stores: List<HimEvidenceAlignmentCatalogAuditStoreV1>,
    )

    private fun missionFreezeRequest(context: RealAuditContext) =
        HimEvidenceAlignmentCatalogAuditMissionFreezeRequestV1(
            root = context.root,
            gitHead = context.currentGitHead,
            catalogRelativePath = context.bindings.canonicalCatalog.relativePath,
            authorityRelativePath = context.bindings.authority.relativePath,
            groundTruthReleaseReference = context.bindings.groundTruthReleaseReference,
            implementationManifestRelativePaths = HimTeacherPaidPilotOfflinePreflightV2
                .implementationManifest(context.root).entries.map { it.path },
            sourceBindings = context.bindings.sourceBindings,
            authority = context.authority,
            maxItemsPerShard = REAL_MAX_ITEMS_PER_SHARD,
        )

    private fun loadRealAuditContext(
        root: File,
        requireMission: Boolean,
        openStores: Boolean,
    ): RealAuditContext {
        val currentGitHead = git(root, "rev-parse", "HEAD")
        require(currentGitHead.matches(Regex("[0-9a-f]{40}")))
        val paths = HimCanonicalFamilyPaths(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
        val persistence = HimCanonicalFamilyPersistence()
        val masterRegistry = persistence.readRegistry(paths.entityIdRegistry)
        val masterAuthority = persistence.readAuthority(paths.familyAuthority)
        HimCanonicalFamilyValidator().validate(catalog, masterRegistry, masterAuthority)

        val foundationRelease = HimCanonicalFamilyFoundationReleasePersistence().read(
            root.resolve(HimCanonicalFamilyFoundationReleaseBuilder.RELEASE_RECORD_PATH),
        )
        HimCanonicalFamilyFoundationReleaseValidator().validate(foundationRelease)

        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val authority = persistence.readAuthority(active.authorityFile)
        require(authority.sourceCatalog.path == catalog.path)
        require(authority.sourceCatalog.contentSha256 == catalog.contentSha256)
        require(authority.sourceCatalog.recordCount == catalog.records.size)
        val release = HimGroundTruthReleasePersistenceV1().read(active.releaseFile)
        require(release.state == HimGroundTruthReleaseState.RELEASED)
        validateActiveGroundTruthRelease(root, active, authority, release)

        val productionReleaseFile = root.resolve(HimProductionIndexFileIdentityReleaseContractV1.PATH)
        val productionRelease = HimProductionIndexFileIdentityReleasePersistenceV1.read(productionReleaseFile)
        val indexSizes = productionRelease.sources.associate { source ->
            source.indexPath to root.resolve(source.indexPath).length()
        }
        HimProductionIndexFileIdentityReleasePersistenceV1.validate(productionRelease, indexSizes)
        val sourceBindings = productionRelease.sources.map { source ->
            val index = root.resolve(source.indexPath)
            val metadata = readIndexMetadata(index)
            require(metadata.source == HimGroundTruthSource.valueOf(source.source))
            require(metadata.sourceArtifactPath == source.optimizedSourcePath)
            require(metadata.sourceArtifactSha256.value == source.optimizedSourceSha256)
            require(metadata.schemaVersion == source.schemaVersion)
            require(metadata.indexBuildPolicyVersion == source.buildPolicyVersion)
            require(metadata.evidenceProjectionPolicyVersion == source.projectionPolicyVersion)
            require(metadata.evidenceRecordCount == source.evidenceRows)
            require(metadata.ftsRowCount == source.ftsRows)
            require(metadata.indexedRecordCount == source.evidenceRows)
            require(metadata.logicalContentSha256.value == source.logicalIndexDigest)
            require(metadata.buildState == HimEvidenceRetrievalIndexBuildState.VALIDATED)
            require(
                HimEvidenceRetrievalIndexValidator.runtimeEligibility(
                    metadata,
                    HimExpectedEvidenceRetrievalIndex(metadata.source, metadata.sourceArtifactSha256),
                ) == HimEvidenceRetrievalIndexEligibility.Ready,
            )
            val sqliteDigest = HimEvidenceAlignmentCatalogAuditRuntimeV1.streamingSha256(index)
            require(sqliteDigest == source.sqliteFileSha256)
            HimEvidenceAlignmentCatalogAuditSourceBindingV1(
                source = metadata.source,
                indexRelativePath = source.indexPath,
                indexByteSize = index.length(),
                sqliteFileSha256 = sqliteDigest,
                sourceArtifactPath = metadata.sourceArtifactPath,
                sourceArtifactSha256 = metadata.sourceArtifactSha256.value,
                schemaVersion = metadata.schemaVersion,
                indexBuildPolicyVersion = metadata.indexBuildPolicyVersion,
                evidenceProjectionPolicyVersion = metadata.evidenceProjectionPolicyVersion,
                logicalContentSha256 = metadata.logicalContentSha256.value,
                buildState = metadata.buildState,
            )
        }
        require(sourceBindings.map { it.source } == HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER)

        val manifest = HimTeacherPaidPilotOfflinePreflightV2.implementationManifest(root)
        val implementationPaths = manifest.entries.map { it.path }
        val implementationBindingSha256 = implementationBindingSha256(root, implementationPaths)
        val bindings = HimEvidenceAlignmentCatalogAuditBindingsV1(
            gitHead = currentGitHead,
            implementationBindingSha256 = implementationBindingSha256,
            canonicalCatalog = HimEvidenceAlignmentCatalogAuditFileBindingV1(
                HimCanonicalFamilyPaths.PRODUCT_ONLY_MASTER_PATH,
                root.resolve(HimCanonicalFamilyPaths.PRODUCT_ONLY_MASTER_PATH).length(),
                HimEvidenceAlignmentCatalogAuditRuntimeV1.streamingSha256(
                    root.resolve(HimCanonicalFamilyPaths.PRODUCT_ONLY_MASTER_PATH),
                ),
            ),
            authority = HimEvidenceAlignmentCatalogAuditFileBindingV1(
                relativePath(root, active.authorityFile),
                active.authorityFile.length(),
                HimEvidenceAlignmentCatalogAuditRuntimeV1.streamingSha256(active.authorityFile),
            ),
            groundTruthReleaseReference = active.releaseReference.value,
            sourceBindings = sourceBindings,
        )
        bindings.validate()
        val missionFile = HimEvidenceAlignmentCatalogAuditPathsV1.mission(root)
        val mission = missionFile.takeIf { it.isFile }?.let(HimEvidenceAlignmentCatalogAuditPersistenceV1::readMission)
        if (requireMission) requireNotNull(mission) { "MISSION_MISSING" }

        val stores = if (openStores) realStores(root, sourceBindings) else emptyList()
        return RealAuditContext(root, currentGitHead, catalog, authority, release, bindings, mission, stores)
    }

    private fun implementationBindingSha256(root: File, relativePaths: List<String>): String {
        val paths = relativePaths.distinct().sorted()
        require(paths.size == relativePaths.size)
        val entries = paths.map { path ->
            val file = root.resolve(path)
            require(file.isFile && file.canRead())
            "$path|${file.length()}|${HimEvidenceAlignmentCatalogAuditRuntimeV1.streamingSha256(file)}"
        }
        return HimEvidenceAlignmentCatalogAuditPersistenceV1.sha256(entries.joinToString("\n") + "\n")
    }

    private fun realStores(
        root: File,
        bindings: List<HimEvidenceAlignmentCatalogAuditSourceBindingV1>,
    ): List<HimEvidenceAlignmentCatalogAuditStoreV1> {
        val offFile = root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX)
        val offValidation = HimOffEvidenceIndexValidator.validateReadOnly(offFile)
        val off = HimOffSqliteEvidenceRetrievalStore.openAfterValidation(offFile, offValidation)
        val agribalyseFile = root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX)
        val agribalyseValidation = HimAgribalyseEvidenceIndexValidator.validateReadOnly(agribalyseFile)
        val agribalyse = HimAgribalyseSqliteEvidenceRetrievalStore.openAfterValidation(agribalyseFile, agribalyseValidation)
        val ciqualFile = root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX)
        val ciqualValidation = HimCiqualEvidenceIndexValidator.validateReadOnly(ciqualFile)
        val ciqual = HimCiqualSqliteEvidenceRetrievalStore.openAfterValidation(ciqualFile, ciqualValidation)
        val glycemicIndexFile = root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX)
        val glycemicIndexValidation = HimGlycemicIndexEvidenceIndexValidator.validateReadOnly(glycemicIndexFile)
        val glycemicIndex = HimGlycemicIndexSqliteEvidenceRetrievalStore.openAfterValidation(glycemicIndexFile, glycemicIndexValidation)
        require(relativePath(root, offFile) == bindings.single { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS }.indexRelativePath)
        require(relativePath(root, agribalyseFile) == bindings.single { it.source == HimGroundTruthSource.AGRIBALYSE }.indexRelativePath)
        require(relativePath(root, ciqualFile) == bindings.single { it.source == HimGroundTruthSource.CIQUAL }.indexRelativePath)
        require(relativePath(root, glycemicIndexFile) == bindings.single { it.source == HimGroundTruthSource.GLYCEMIC_INDEX }.indexRelativePath)
        return listOf(
            RealStore(HimGroundTruthSource.OPEN_FOOD_FACTS, bindings.single { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS }, off::search) { reference ->
                off.fetch(reference)?.let(::toIndexRecord)
            },
            RealStore(HimGroundTruthSource.AGRIBALYSE, bindings.single { it.source == HimGroundTruthSource.AGRIBALYSE }, agribalyse::search) { reference ->
                agribalyse.fetch(reference)?.let(::toIndexRecord)
            },
            RealStore(HimGroundTruthSource.CIQUAL, bindings.single { it.source == HimGroundTruthSource.CIQUAL }, ciqual::search) { reference ->
                ciqual.fetch(reference)?.let(::toIndexRecord)
            },
            RealStore(HimGroundTruthSource.GLYCEMIC_INDEX, bindings.single { it.source == HimGroundTruthSource.GLYCEMIC_INDEX }, glycemicIndex::search) { reference ->
                glycemicIndex.fetch(reference)?.let(::toIndexRecord)
            },
        )
    }

    private fun toIndexRecord(result: HimEvidenceSearchResult): HimEvidenceRetrievalIndexRecord = when (result.source) {
        HimGroundTruthSource.OPEN_FOOD_FACTS -> HimOffEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson)
        HimGroundTruthSource.AGRIBALYSE -> HimAgribalyseEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson)
        HimGroundTruthSource.CIQUAL -> HimCiqualEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson, 1)
        HimGroundTruthSource.GLYCEMIC_INDEX -> HimGlycemicIndexEvidenceProjectionV1.fromProjectionJson(result.evidenceProjection.deterministicJson, 1)
    }

    private fun realShardGate() = HimEvidenceAlignmentCatalogAuditRuntimeGateV1.fromProperties().also {
        require(System.getProperty(HimEvidenceAlignmentCatalogAuditRuntimeGateV1.SOURCE_INTEGRATION_PROPERTY) == "true") {
            "SOURCE_INTEGRATION_REQUIRED"
        }
        require(System.getProperty(HimEvidenceAlignmentCatalogAuditRuntimeGateV1.CATALOG_AUDIT_PROPERTY) == "true") {
            "AUDIT_OPT_IN_REQUIRED"
        }
        require(System.getProperty(HimEvidenceAlignmentCatalogAuditRuntimeGateV1.CONFIRMATION_PROPERTY) == HimEvidenceAlignmentCatalogAuditRuntimeGateV1.CONFIRMATION) {
            "AUDIT_CONFIRMATION_REQUIRED"
        }
    }

    private inline fun <reified T> runtimeValue(
        result: HimEvidenceAlignmentCatalogAuditRuntimeResult<T>,
    ): T = when (result) {
        is HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed -> assertIs<T>(result.value)
        is HimEvidenceAlignmentCatalogAuditRuntimeResult.Failed -> fail(result.reason)
        is HimEvidenceAlignmentCatalogAuditRuntimeResult.Skipped -> fail(result.reason)
    }

    private fun readIndexMetadata(index: File): HimEvidenceRetrievalIndexMetadata {
        require(index.isFile && index.canRead())
        Class.forName("org.sqlite.JDBC")
        val url = "jdbc:sqlite:file:${index.canonicalFile.path}?mode=ro"
        return DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA query_only=ON")
                statement.executeQuery(
                    """
                    SELECT schema_version, source, source_artifact_path, source_artifact_sha256,
                           source_record_count, logical_record_counts_json,
                           index_build_policy_version, evidence_projection_policy_version,
                           indexed_record_count, evidence_record_count, fts_row_count,
                           logical_content_sha256, build_state, sqlite_runtime_version,
                           sqlite_file_sha256
                    FROM index_metadata WHERE singleton_id=1
                    """.trimIndent(),
                ).use { result ->
                    require(result.next())
                    val logicalCounts = JsonParser.parseString(
                        result.getString("logical_record_counts_json"),
                    ).asJsonObject.entrySet().associate { it.key to it.value.asLong }.toSortedMap()
                    HimEvidenceRetrievalIndexMetadata(
                        schemaVersion = result.getString("schema_version"),
                        source = HimGroundTruthSource.valueOf(result.getString("source")),
                        sourceArtifactPath = result.getString("source_artifact_path"),
                        sourceArtifactSha256 = HimSha256(result.getString("source_artifact_sha256")),
                        sourceRecordCount = result.getLong("source_record_count"),
                        logicalRecordCounts = TreeMap(logicalCounts),
                        indexBuildPolicyVersion = result.getString("index_build_policy_version"),
                        evidenceProjectionPolicyVersion = result.getString("evidence_projection_policy_version"),
                        indexedRecordCount = result.getLong("indexed_record_count"),
                        evidenceRecordCount = result.getLong("evidence_record_count"),
                        ftsRowCount = result.getLong("fts_row_count"),
                        logicalContentSha256 = HimSha256(result.getString("logical_content_sha256")),
                        buildState = HimEvidenceRetrievalIndexBuildState.valueOf(result.getString("build_state")),
                        sqliteRuntimeVersion = result.getString("sqlite_runtime_version"),
                        sqliteFileSha256 = result.getString("sqlite_file_sha256")?.let(::HimSha256),
                    )
                }
            }
        }
    }

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("user.dir unavailable")).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) return current
            current = current.parentFile ?: error("Repository root not found")
        }
    }

    private fun relativePath(root: File, file: File): String {
        val canonicalRoot = root.canonicalFile.toPath()
        val canonicalFile = file.canonicalFile.toPath()
        require(canonicalFile.startsWith(canonicalRoot))
        return canonicalRoot.relativize(canonicalFile).toString().replace(File.separatorChar, '/')
    }

    private fun git(root: File, vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git") + arguments.toList())
            .directory(root)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        require(process.waitFor() == 0) { "Git command failed: ${arguments.joinToString(" ")}: $output" }
        return output
    }

    private fun gitExit(root: File, vararg arguments: String): Int {
        val process = ProcessBuilder(listOf("git") + arguments.toList())
            .directory(root)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().use { it.readText() }
        return process.waitFor()
    }

    private fun stores(
        duplicateHits: Boolean = false,
        noHits: Boolean = false,
        failSearch: HimGroundTruthSource? = null,
        failFetch: HimGroundTruthSource? = null,
        records: Map<HimGroundTruthSource, HimEvidenceRetrievalIndexRecord> = records(),
    ) = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.map { source ->
        FakeStore(
            source = source,
            binding = sourceBindings().single { it.source == source },
            record = records.getValue(source),
            duplicateHits = duplicateHits,
            noHits = noHits,
            failSearch = failSearch == source,
            failFetch = failFetch == source,
        )
    }

    private fun records() = mapOf(
        HimGroundTruthSource.OPEN_FOOD_FACTS to off(),
        HimGroundTruthSource.AGRIBALYSE to agribalyse(),
        HimGroundTruthSource.CIQUAL to ciqual(),
        HimGroundTruthSource.GLYCEMIC_INDEX to gi(),
    )

    private fun off() = HimOffEvidenceProjectionV1.fromProjectionJson(
        """{"rowOrdinal":1,"source":{"code":"off"},"identity":{"productName":"Vanille Pudding mit Bourbon-Vanille","productNameGerman":null,"productNameEnglish":null,"genericName":null,"genericNameGerman":null,"genericNameEnglish":null,"brands":[],"productType":null},"taxonomy":{"categories":["Desserts"]},"ingredients":{"text":"Vanille"},"quality":{}}""",
    )

    private fun agribalyse() = HimAgribalyseEvidenceProjectionV1.fromProjectionJson(
        """{"rowOrdinal":2,"agbCode":"31044","ciqualCode":"fixture","seasonCode":"fixture","airTransportCode":"no","productNameFr":"Sucre vanillé","lciName":"Sugar, vanilla flavoured","foodGroup":"Sugar","foodSubgroup":"Sugar","preparation":"fixture","delivery":"fixture","packagingApproach":"fixture"}""",
    )

    private fun ciqual() = HimCiqualEvidenceProjectionV1.fromProjectionJson(
        """{"recordKind":"FOOD","alimCode":"11057","nameFr":"Vanille, gousse","nameEn":"Vanilla, pod","scientificName":{"lexicalValue":"Vanilla planifolia","missingAttributeValue":null},"groupCode":"01","groupNameFr":"Spices","groupNameEn":"Spices","subgroupCode":"01","subgroupNameFr":"Spices","subgroupNameEn":"Spices","subSubgroupCode":"01","subSubgroupNameFr":"Spices","subSubgroupNameEn":"Spices"}""",
        3,
    )

    private fun unsupportedCiqual() = HimCiqualEvidenceProjectionV1.fromProjectionJson(
        """{"recordKind":"TAXONOMY","groupCode":"01","groupNameFr":"Vanille","groupNameEn":"Vanilla","subgroupCode":"01","subgroupNameFr":"Spices","subgroupNameEn":"Spices","subSubgroupCode":"01","subSubgroupNameFr":"Spices","subSubgroupNameEn":"Spices"}""",
        3,
    )

    private fun gi() = HimGlycemicIndexEvidenceProjectionV1.fromProjectionJson(
        """{"recordKind":"MEASUREMENT","arrayOrdinal":823,"foodNumber":823,"pageNumber":1,"foodItem":{"lexicalValue":"Prince Petit Déjeuner Vanille (LU, France and Spain)","status":"PRESENT"},"country":{"lexicalValue":"France","status":"PRESENT"},"year":{"lexicalValue":"2010","status":"PRESENT"},"gi":{"lexicalValue":"73","status":"PRESENT"},"sem":{"lexicalValue":"6","status":"PRESENT"},"gl":{"lexicalValue":"11","status":"PRESENT"},"subjects":{"lexicalValue":"10","status":"PRESENT"},"availableCarbohydrate":{"lexicalValue":"50","status":"PRESENT"},"testPortion":{"lexicalValue":"119","status":"PRESENT"},"referenceFoodTime":{"lexicalValue":"Bread, 2h","status":"PRESENT"},"timepoints":{"lexicalValue":"Standard","status":"PRESENT"},"sampleCollection":{"lexicalValue":"Capillary","status":"PRESENT"},"analysisMethod":{"lexicalValue":"YSI","status":"PRESENT"},"referenceCode":{"lexicalValue":"UO7","status":"PRESENT"},"sourceContext":{"majorCategory":"COOKIES","subcategory":null,"deeperHeading":null}}""",
        4,
    )

    private class RealStore(
        override val source: HimGroundTruthSource,
        override val binding: HimEvidenceAlignmentCatalogAuditSourceBindingV1,
        private val searcher: (String, HimEvidenceSearchLimit) -> List<HimEvidenceSearchResult>,
        private val fetcher: (HimEvidenceRecordReference) -> HimEvidenceRetrievalIndexRecord?,
    ) : HimEvidenceAlignmentCatalogAuditStoreV1 {
        override fun search(query: String, limit: HimEvidenceSearchLimit) = searcher(query, limit)

        override fun fetch(reference: HimEvidenceRecordReference) = fetcher(reference)
    }

    private class FakeStore(
        override val source: HimGroundTruthSource,
        override val binding: HimEvidenceAlignmentCatalogAuditSourceBindingV1,
        private val record: HimEvidenceRetrievalIndexRecord,
        private val duplicateHits: Boolean,
        private val noHits: Boolean,
        private val failSearch: Boolean,
        private val failFetch: Boolean,
        val searchCalls: MutableList<String> = mutableListOf(),
        val fetchCalls: MutableList<String> = mutableListOf(),
        val limits: MutableList<Int> = mutableListOf(),
    ) : HimEvidenceAlignmentCatalogAuditStoreV1 {
        override fun search(query: String, limit: HimEvidenceSearchLimit): List<HimEvidenceSearchResult> {
            if (failSearch) error("fake search failure")
            searchCalls += query
            limits += limit.value
            if (noHits) return emptyList()
            val hit = HimEvidenceSearchResult(source, record.sourceRecordReference, record.recordKind, 1, record.evidenceProjection)
            return if (duplicateHits) listOf(hit, hit) else listOf(hit)
        }

        override fun fetch(reference: HimEvidenceRecordReference): HimEvidenceRetrievalIndexRecord? {
            if (failFetch) error("fake fetch failure")
            fetchCalls += reference.value
            return if (reference == record.sourceRecordReference) record else null
        }

        fun copy(binding: HimEvidenceAlignmentCatalogAuditSourceBindingV1) = FakeStore(
            source, binding, record, duplicateHits, noHits, failSearch, failFetch,
            searchCalls, fetchCalls, limits,
        )
    }

    private class TrackingInputStream(private val bytes: ByteArray) : InputStream() {
        private var position = 0
        var maxRequestedBytes = 0
        var readCalls = 0

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            maxRequestedBytes = maxOf(maxRequestedBytes, length)
            readCalls++
            if (position >= bytes.size) return -1
            val count = minOf(length, bytes.size - position)
            bytes.copyInto(buffer, offset, position, position + count)
            position += count
            return count
        }

        override fun read(): Int = if (position < bytes.size) bytes[position++].toInt() else -1
    }
}
