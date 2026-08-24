package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalIdentity
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalVariant
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import java.io.File

class RunHimEvidenceAlignmentCatalogAuditContractV1Test {
    @Test
    fun fourSourceBindingsAreExactAndComplete() {
        val bindings = bindings()
        bindings.validate()
        assertEquals(
            listOf("OPEN_FOOD_FACTS", "AGRIBALYSE", "CIQUAL", "GLYCEMIC_INDEX"),
            bindings.sourceBindings.map { it.source.name },
        )
        assertEquals(4, bindings.sourceBindings.map { it.source }.distinct().size)
    }

    @Test
    fun missingDuplicateAndAdditionalSourceBindingsFailClosed() {
        val bindings = bindings()
        assertFailsWith<IllegalArgumentException> {
            bindings.copy(sourceBindings = bindings.sourceBindings.drop(1)).validate()
        }
        assertFailsWith<IllegalArgumentException> {
            bindings.copy(sourceBindings = bindings.sourceBindings.dropLast(1) + bindings.sourceBindings.first()).validate()
        }
        assertFailsWith<IllegalArgumentException> {
            bindings.copy(sourceBindings = bindings.sourceBindings + bindings.sourceBindings.first()).validate()
        }
    }

    @Test
    fun canonicalOrderIsIndependentOfInputOrder() {
        val first = plan(authority().families)
        val reversed = plan(authority().families.reversed())
        assertEquals(first.canonicalOrder, reversed.canonicalOrder)
        assertEquals(first.canonicalOrderDigest, reversed.canonicalOrderDigest)
        assertEquals(first.missionDigest, reversed.missionDigest)
    }

    @Test
    fun duplicateEntityIdIsRejected() {
        val families = authority().families + authority().families.first().copy(canonicalName = "Duplikat", normalizedName = "duplikat")
        assertFailsWith<IllegalArgumentException> { plan(families) }
    }

    @Test
    fun queryTermsUseExistingNormalizationAndStableDeduplication() {
        val family = authority().families.single { it.canonicalName == "Vanille" }.copy(
            aliases = listOf(
                HimCanonicalAlias(HimEntityId("c00001"), " VANILLE ", "vanille", HimLifecycleStatus.ACTIVE),
                HimCanonicalAlias(HimEntityId("c00002"), "Vanille de Bourbon", "vanille de bourbon", HimLifecycleStatus.ACTIVE),
            ),
        )
        val canonical = HimEvidenceAlignmentCatalogAuditContractV1.canonicalDescriptor(family)
        assertEquals("vanille", canonical.queryTerms.first().normalizedTerm)
        assertEquals(canonical.queryTerms.size, canonical.queryTerms.map { it.normalizedTerm }.distinct().size)
        assertEquals("Vanille", canonical.queryTerms.first().term)
        assertTrue(canonical.queryTerms.map { it.term }.containsAll(listOf("Vanille", "Bourbon", "Vanille de Bourbon")))
    }

    @Test
    fun boundCanonicalKeyIsUsedForSataysosseWithoutSpecialHandling() {
        val canonical = HimEvidenceAlignmentCatalogAuditContractV1.canonicalDescriptor(
            family("a00006", "Sataysoße", "sataysosse", "Scharf", "c00006"),
        )

        assertEquals("sataysosse", canonical.normalizedName)
        assertEquals("Sataysoße", canonical.queryTerms.first().term)
        assertEquals("sataysosse", canonical.queryTerms.first().normalizedTerm)
        assertNotEquals(
            HimEvidenceAlignmentCatalogAuditContractV1.normalizeQuery("Sataysoße"),
            canonical.queryTerms.first().normalizedTerm,
        )
    }

    @Test
    fun generalBoundCanonicalKeysDoNotDependOnWordSpecificRules() {
        val families = listOf(
            family("a00006", "Straße", "strasse", "Süß", "c00006"),
            family("a00007", "Crème", "creme", "Frisch", "c00007"),
        )

        val plan = plan(families)
        assertEquals(
            listOf("strasse", "creme"),
            plan.canonicalOrder.map { it.normalizedName },
        )
    }

    @Test
    fun boundIdentityVariantAndAliasKeysRemainStableAndDeduplicated() {
        val family = family("a00006", "Frucht", "frucht", "Süß", "c00006").copy(
            identities = listOf(
                HimCanonicalIdentity(
                    identityId = HimEntityId("b00006"),
                    identityName = "Äpfel",
                    normalizedName = "aepfel",
                    lifecycleStatus = HimLifecycleStatus.ACTIVE,
                    variants = listOf(
                        HimCanonicalVariant(HimEntityId("d00006"), "Süß", "suess", HimLifecycleStatus.ACTIVE),
                    ),
                    aliases = listOf(
                        HimCanonicalAlias(HimEntityId("e00006"), "Straße", "strasse", HimLifecycleStatus.ACTIVE),
                    ),
                ),
            ),
            variants = listOf(
                HimCanonicalVariant(HimEntityId("d00007"), "Süß", "suess", HimLifecycleStatus.ACTIVE),
            ),
            aliases = listOf(
                HimCanonicalAlias(HimEntityId("e00007"), "Straße", "strasse", HimLifecycleStatus.ACTIVE),
            ),
        )

        val first = HimEvidenceAlignmentCatalogAuditContractV1.canonicalDescriptor(family)
        val second = HimEvidenceAlignmentCatalogAuditContractV1.canonicalDescriptor(family)
        assertEquals(first, second)
        assertEquals(
            listOf("frucht", "aepfel", "suess", "strasse"),
            first.queryTerms.map { it.normalizedTerm },
        )
        assertEquals(first.queryTerms.size, first.queryTerms.map { it.normalizedTerm }.distinct().size)
    }

    @Test
    fun queryAndFetchLimitsAreBounded() {
        assertFailsWith<IllegalArgumentException> { HimEvidenceSearchLimit(11) }
        val terms = (1..33).map {
            HimEvidenceAlignmentCatalogAuditQueryTermV1("term$it", "term$it", HimEvidenceAlignmentCatalogAuditQueryTermKind.ALIAS)
        }
        assertFailsWith<IllegalArgumentException> {
            HimEvidenceAlignmentCatalogAuditQueryPlanV1("a00001", HimGroundTruthSource.CIQUAL, terms, 10, 330)
        }
        val queryPlan = plan().queryPlans.first()
        assertEquals(10, queryPlan.maxResultsPerQuery)
        assertEquals(queryPlan.terms.size * 10, queryPlan.maxFetches)
    }

    @Test
    fun shardsAreContiguousNonOverlappingAndDeterministic() {
        val one = plan(maxItemsPerShard = 2)
        val two = plan(maxItemsPerShard = 2)
        assertEquals(one.shards, two.shards)
        assertEquals(one.canonicalOrder.map { it.entityId }, one.shards.flatMap { it.canonicalEntityIds })
        assertTrue(one.shards.zipWithNext().all { it.first.endExclusive == it.second.startInclusive })
        assertEquals(one.canonicalOrder.size, one.shards.sumOf { it.canonicalCount })
    }

    @Test
    fun shardSizeOrBindingChangesMissionDigest() {
        assertNotEquals(plan(maxItemsPerShard = 1).missionDigest, plan(maxItemsPerShard = 2).missionDigest)
        assertNotEquals(plan().missionDigest, plan(gitHead = "b".repeat(40)).missionDigest)
    }

    @Test
    fun resumeAcceptsOnlyBindingIdenticalValidatedShards() {
        val plan = plan(maxItemsPerShard = 2)
        val result = completeShard(plan, plan.shards.first())
        assertEquals(listOf(result.shardId), HimEvidenceAlignmentCatalogAuditContractV1.validateResume(plan, listOf(result)))
        assertFailsWith<IllegalArgumentException> {
            HimEvidenceAlignmentCatalogAuditContractV1.validateResume(plan, listOf(result.copy(missionDigest = "f".repeat(64))))
        }
        assertFailsWith<IllegalArgumentException> {
            HimEvidenceAlignmentCatalogAuditContractV1.validateResume(plan, listOf(result.copy(state = HimEvidenceAlignmentCatalogAuditState.PARTIAL)))
        }
    }

    @Test
    fun partialShardCannotClaimComplete() {
        val plan = plan(maxItemsPerShard = 2)
        val result = HimEvidenceAlignmentCatalogAuditContractV1.shardResult(plan, plan.shards.first().shardId, emptyList())
        assertEquals(HimEvidenceAlignmentCatalogAuditState.PARTIAL, result.state)
        val aggregate = HimEvidenceAlignmentCatalogAuditContractV1.aggregate(plan, listOf(result))
        assertEquals(HimEvidenceAlignmentCatalogAuditState.PARTIAL, aggregate.state)
        assertTrue(aggregate.counters.coverageGaps > 0)
    }

    @Test
    fun missingSourcePreventsComplete() {
        val plan = plan(maxItemsPerShard = 2)
        val shard = plan.shards.first()
        val cells = cells(shard).dropLast(1)
        val result = HimEvidenceAlignmentCatalogAuditContractV1.shardResult(plan, shard.shardId, cells)
        assertEquals(HimEvidenceAlignmentCatalogAuditState.PARTIAL, result.state)
        assertEquals(HimEvidenceAlignmentCatalogAuditState.PARTIAL, HimEvidenceAlignmentCatalogAuditContractV1.aggregate(plan, listOf(result)).state)
    }

    @Test
    fun duplicateCanonicalSourceEvaluationIsRejected() {
        val plan = plan(maxItemsPerShard = 2)
        val shard = plan.shards.first()
        val cell = cells(shard).first()
        assertFailsWith<IllegalArgumentException> {
            HimEvidenceAlignmentCatalogAuditContractV1.shardResult(plan, shard.shardId, listOf(cell, cell))
        }
    }

    @Test
    fun counterInconsistencyIsRejected() {
        val plan = plan(maxItemsPerShard = 2)
        val result = completeShard(plan, plan.shards.first())
        assertFailsWith<IllegalArgumentException> {
            result.copy(counters = result.counters.copy(queries = result.counters.queries + 1)).validateAgainst(plan)
        }
    }

    @Test
    fun modifierCoverageIsSeparateFromPrimaryFinding() {
        val plan = plan(maxItemsPerShard = 2)
        val source = HimGroundTruthSource.CIQUAL
        val cell = cells(plan.shards.first()).first { it.source == source }.copy(
            findings = listOf(finding(plan.shards.first().canonicalEntityIds.first(), source, HimEvidenceAlignmentAuditClassification.MODIFIER_ONLY)),
            modifierCoverage = listOf(HimEvidenceAlignmentCatalogAuditModifierCoverageV1("gousse", false)),
        )
        val result = HimEvidenceAlignmentCatalogAuditContractV1.shardResult(plan, plan.shards.first().shardId, listOf(cell))
        assertEquals(1, result.counters.modifierOnly)
        assertEquals(0, result.counters.directSupported)
        assertEquals(1, cell.modifierCoverage.count { !it.coveredByAuthority })
    }

    @Test
    fun findingsDoNotBecomeTechnicalErrors() {
        val plan = plan(maxItemsPerShard = 2)
        val shard = plan.shards.first()
        val cells = cells(shard).map { cell ->
            if (cell.source == HimGroundTruthSource.OPEN_FOOD_FACTS) cell.copy(
                findings = listOf(finding(cell.entityId, cell.source, HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND)),
            ) else cell
        }
        val result = HimEvidenceAlignmentCatalogAuditContractV1.shardResult(plan, shard.shardId, cells)
        assertEquals(2, result.counters.invalidOrUnsupported)
        assertEquals(0, result.counters.technicalErrors)
        assertEquals(HimEvidenceAlignmentCatalogAuditState.COMPLETE, result.state)
    }

    @Test
    fun missionPersistenceReloadsAndIsIdempotent() {
        val directory = createTempDirectory("him-audit-mission").toFile()
        val file = File(directory, "mission.json")
        val plan = plan()
        HimEvidenceAlignmentCatalogAuditPersistenceV1.writeMission(file, plan)
        val first = file.readBytes()
        HimEvidenceAlignmentCatalogAuditPersistenceV1.writeMission(file, plan)
        assertContentEquals(first, file.readBytes())
        assertEquals(plan, HimEvidenceAlignmentCatalogAuditPersistenceV1.readMission(file))
    }

    @Test
    fun shardPersistenceReloadsAndIsIdempotent() {
        val directory = createTempDirectory("him-audit-shard").toFile()
        val plan = plan(maxItemsPerShard = 2)
        val result = completeShard(plan, plan.shards.first())
        val file = File(directory, "shard.json")
        HimEvidenceAlignmentCatalogAuditPersistenceV1.writeShard(file, plan, result)
        val bytes = file.readBytes()
        HimEvidenceAlignmentCatalogAuditPersistenceV1.writeShard(file, plan, result)
        assertContentEquals(bytes, file.readBytes())
        assertEquals(result, HimEvidenceAlignmentCatalogAuditPersistenceV1.readShard(file, plan))
    }

    @Test
    fun aggregatePersistenceReloadsAndIsIdempotent() {
        val directory = createTempDirectory("him-audit-aggregate").toFile()
        val plan = plan(maxItemsPerShard = 2)
        val aggregate = HimEvidenceAlignmentCatalogAuditContractV1.aggregate(
            plan,
            plan.shards.map { completeShard(plan, it) },
        )
        assertEquals(HimEvidenceAlignmentCatalogAuditState.COMPLETE, aggregate.state)
        val file = File(directory, "aggregate.json")
        HimEvidenceAlignmentCatalogAuditPersistenceV1.writeAggregate(file, plan, aggregate)
        val bytes = file.readBytes()
        HimEvidenceAlignmentCatalogAuditPersistenceV1.writeAggregate(file, plan, aggregate)
        assertContentEquals(bytes, file.readBytes())
        assertEquals(aggregate, HimEvidenceAlignmentCatalogAuditPersistenceV1.readAggregate(file, plan))
    }

    @Test
    fun bindingMismatchCannotOverwriteExistingArtifact() {
        val directory = createTempDirectory("him-audit-freeze").toFile()
        val file = File(directory, "mission.json")
        val first = plan()
        HimEvidenceAlignmentCatalogAuditPersistenceV1.writeMission(file, first)
        assertFailsWith<IllegalArgumentException> {
            HimEvidenceAlignmentCatalogAuditPersistenceV1.writeMission(file, plan(gitHead = "c".repeat(40)))
        }
    }

    @Test
    fun persistedBindingsContainNoAbsolutePaths() {
        assertFailsWith<IllegalArgumentException> {
            bindings().copy(
                canonicalCatalog = HimEvidenceAlignmentCatalogAuditFileBindingV1("/tmp/catalog.json", 1, "a".repeat(64)),
            ).validate()
        }
        assertFailsWith<IllegalArgumentException> {
            bindings().copy(
                sourceBindings = bindings().sourceBindings.mapIndexed { index, binding ->
                    if (index == 0) binding.copy(indexRelativePath = "../index.sqlite") else binding
                },
            ).validate()
        }
    }

    @Test
    fun smallFourSourceUniverseAggregatesCompletely() {
        val plan = plan(maxItemsPerShard = 2)
        val aggregate = HimEvidenceAlignmentCatalogAuditContractV1.aggregate(
            plan,
            plan.shards.map { completeShard(plan, it) },
        )
        assertEquals(3, aggregate.counters.canonicalsTotal)
        assertEquals(3, aggregate.counters.canonicalsProcessed)
        assertEquals(4, aggregate.counters.sourcesExpected)
        assertEquals(4, aggregate.counters.sourcesProcessed)
        assertEquals(0, aggregate.counters.coverageGaps)
    }

    @Test
    fun vanillaMatrixHasOneSupportedAndThreeRejectedDirectFindings() {
        val plan = plan(maxItemsPerShard = 1)
        val classifications = listOf(
            HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED,
            HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED,
            HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED,
            HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED,
        )
        val shard = plan.shards.first()
        val cells = cells(shard).mapIndexed { index, cell ->
            cell.copy(findings = listOf(finding(cell.entityId, cell.source, classifications[index])))
        }
        val result = HimEvidenceAlignmentCatalogAuditContractV1.shardResult(plan, shard.shardId, cells)
        assertEquals(1, result.counters.directSupported)
        assertEquals(3, result.counters.directRejected)
    }

    private fun plan(
        families: List<HimCanonicalFamily> = authority().families,
        maxItemsPerShard: Int = 2,
        gitHead: String = "a".repeat(40),
    ) = HimEvidenceAlignmentCatalogAuditContractV1.plan(
        bindings().copy(gitHead = gitHead),
        authority().copy(families = families),
        maxItemsPerShard,
    )

    private fun completeShard(
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        shard: HimEvidenceAlignmentCatalogAuditShardV1,
    ) = HimEvidenceAlignmentCatalogAuditContractV1.shardResult(plan, shard.shardId, cells(shard))

    private fun cells(
        shard: HimEvidenceAlignmentCatalogAuditShardV1,
    ) = shard.canonicalEntityIds.flatMap { entityId ->
        HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.map { source ->
            HimEvidenceAlignmentCatalogAuditCellV1(
                entityId = entityId,
                source = source,
                completed = true,
                queries = 1,
                retrievalHits = 1,
                deduplicatedProjections = 1,
                fetches = 1,
                findings = listOf(finding(entityId, source, HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT)),
                modifierCoverage = emptyList(),
                technicalValid = true,
                technicalErrors = 0,
            )
        }
    }

    private fun finding(
        entityId: String,
        source: HimGroundTruthSource,
        classification: HimEvidenceAlignmentAuditClassification,
    ) = HimEvidenceAlignmentCatalogAuditFindingV1(entityId, source, classification, null, null)

    private fun bindings() = HimEvidenceAlignmentCatalogAuditBindingsV1(
        gitHead = "a".repeat(40),
        implementationBindingSha256 = "b".repeat(64),
        canonicalCatalog = HimEvidenceAlignmentCatalogAuditFileBindingV1("data/catalog.json", 10, "c".repeat(64)),
        authority = HimEvidenceAlignmentCatalogAuditFileBindingV1("data/authority.json", 11, "d".repeat(64)),
        groundTruthReleaseReference = "release:v1:${"e".repeat(64)}",
        sourceBindings = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.mapIndexed { index, source ->
            HimEvidenceAlignmentCatalogAuditSourceBindingV1(
                source = source,
                indexRelativePath = "data/index/${source.name.lowercase()}.sqlite",
                indexByteSize = (index + 1).toLong(),
                sqliteFileSha256 = "f".repeat(64),
                sourceArtifactPath = source.artifactPath,
                sourceArtifactSha256 = "1".repeat(64),
                schemaVersion = "HIM_EVIDENCE_RETRIEVAL_INDEX_SCHEMA_V1",
                indexBuildPolicyVersion = "INDEX_BUILD_V1",
                evidenceProjectionPolicyVersion = "EVIDENCE_PROJECTION_V1",
                logicalContentSha256 = "2".repeat(64),
                buildState = HimEvidenceRetrievalIndexBuildState.VALIDATED,
            )
        },
    )

    private fun authority() = HimCanonicalFamilyAuthority(
        schemaVersion = "AUTHORITY_V1",
        sourceCatalog = HimCanonicalFamilySourceCatalog("data/catalog.json", "c".repeat(64), 3),
        families = listOf(
            family("a00003", "Apfel", "apfel", "Sorte", "c00003"),
            family("a00001", "Vanille", "vanille", "Bourbon", "c00001"),
            family("a00002", "Keks", "keks", "Cookie", "c00002"),
        ),
    )

    private fun family(
        id: String,
        name: String,
        normalized: String,
        variantName: String,
        aliasId: String,
    ) = HimCanonicalFamily(
        canonicalId = HimEntityId(id),
        canonicalName = name,
        normalizedName = normalized,
        taxonomyPaths = emptyList(),
        lifecycleStatus = HimLifecycleStatus.ACTIVE,
        identities = listOf(
            HimCanonicalIdentity(
                identityId = HimEntityId("b${id.takeLast(5)}"),
                identityName = name,
                normalizedName = normalized,
                lifecycleStatus = HimLifecycleStatus.ACTIVE,
                variants = emptyList(),
                aliases = emptyList(),
            ),
        ),
        variants = listOf(HimCanonicalVariant(HimEntityId("d${id.takeLast(5)}"), variantName, variantName.lowercase(), HimLifecycleStatus.ACTIVE)),
        aliases = listOf(HimCanonicalAlias(HimEntityId(aliasId), "$name alias", "$normalized alias", HimLifecycleStatus.ACTIVE)),
    )
}
