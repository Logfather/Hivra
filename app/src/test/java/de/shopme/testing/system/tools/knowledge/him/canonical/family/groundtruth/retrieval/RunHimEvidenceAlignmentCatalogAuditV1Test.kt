package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import java.io.File
import java.io.InputStream

class RunHimEvidenceAlignmentCatalogAuditV1Test {
    @Test
    fun missionFreezePerformsNoSearchOrFetch() {
        val root = fixtureRoot()
        val result = HimEvidenceAlignmentCatalogAuditRuntimeV1.freezeMission(freezeRequest(root), enabledGate())
        val completed = assertIs<HimEvidenceAlignmentCatalogAuditRuntimeResult.Completed<*>>(result)
        assertIs<HimEvidenceAlignmentCatalogAuditMissionPlanV1>(completed.value)
        assertTrue(HimEvidenceAlignmentCatalogAuditPathsV1.mission(root).isFile)
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
