package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalIdentity
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalVariant
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import java.io.File

class RunHimEvidenceAlignmentCatalogAuditFindingsEnrichmentV1Test {
    @Test
    fun contractSeparatesHeadsBindsAuditAndPlansExactlyElevenMatchingShards() {
        val fixture = fixture()

        assertEquals(
            HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.VERSION,
            fixture.mission.contractId,
        )
        assertEquals(fixture.plan.bindings.gitHead, fixture.mission.provenance.auditHead)
        assertEquals("b".repeat(40), fixture.mission.provenance.enrichmentImplementationHead)
        assertEquals(11, fixture.mission.shards.size)
        assertEquals(fixture.plan.shards.map { it.shardId }, fixture.mission.shards.map { it.shardId })
        assertEquals(fixture.plan.shards.map { it.startInclusive }, fixture.mission.shards.map { it.startInclusive })
        assertEquals(fixture.plan.shards.map { it.endExclusive }, fixture.mission.shards.map { it.endExclusive })
        assertEquals(fixture.plan.shards.flatMap { it.canonicalEntityIds }, fixture.mission.shards.flatMap { it.canonicalEntityIds })
        assertEquals(11, fixture.mission.provenance.auditShardBindings.size)
    }

    @Test
    fun runtimeKeepsNoHitSeparateRoutesFourExtractorsAndCachesBySourceAndReference() {
        val fixture = fixture()
        val result = completed(fixture.request)
        val ports = fixture.ports.associateBy { it.source }

        assertEquals(6, result.counters.reconstructedFindings)
        assertEquals(
            fixture.request.auditShard.cells.sumOf { cell ->
                cell.findings.count { it.classification == HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT }
            },
            result.counters.noRetrievalHits,
        )
        assertEquals(1, result.counters.unsupportedRecordKinds)
        assertEquals(5, result.counters.uniqueEvidenceReferences)
        assertEquals(5, result.counters.exactFetches)
        assertEquals(0, result.counters.classificationMismatches)
        assertEquals(1, ports.getValue(HimGroundTruthSource.OPEN_FOOD_FACTS).fetchCount)
        assertEquals(1, ports.getValue(HimGroundTruthSource.AGRIBALYSE).fetchCount)
        assertEquals(2, ports.getValue(HimGroundTruthSource.CIQUAL).fetchCount)
        assertEquals(1, ports.getValue(HimGroundTruthSource.GLYCEMIC_INDEX).fetchCount)

        val noHits = result.findings.filter {
            it.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.NO_RETRIEVAL_HIT
        }
        assertEquals(2, noHits.size)
        assertTrue(noHits.all { it.originalEvidenceReference == null && it.recordKind == null })
        assertTrue(noHits.all {
            it.queryTermFindingState == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.NOT_PERSISTED_IN_AUDIT_V1
        })

        val repeated = result.findings.filter { it.originalEvidenceReference == OFF_REFERENCE }
        assertEquals(2, repeated.size)
        assertEquals(2, repeated.map { it.findingOccurrenceId }.distinct().size)
        assertTrue(result.findings.zipWithNext().all { (left, right) ->
            left.entityId < right.entityId || left.source.ordinal <= right.source.ordinal
        })

        val ciqual = result.findings.single { it.recordKind == HimEvidenceRecordKind.CIQUAL_FOOD }
        assertEquals(HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH, ciqual.reconstructedAlignmentClassification)
        assertEquals(listOf("gousse", "pod"), ciqual.modifiers)
        assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH, ciqual.classificationComparison)

        val unsupported = result.findings.single {
            it.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.UNSUPPORTED_RECORD_KIND
        }
        assertEquals(HimEvidenceRecordKind.CIQUAL_TAXONOMY, unsupported.recordKind)
        assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.NOT_RECONSTRUCTABLE, unsupported.classificationComparison)
        assertEquals(HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND, unsupported.originalAuditClassification)
    }

    @Test
    fun classificationComparisonIsExhaustiveAndDoesNotMapNonComparableAuditValues() {
        val direct = listOf(
            HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH,
            HimEvidenceAlignmentClassificationV1.PRIMARY_AUTHORITY_BOUND_MATCH,
        )
        val allAlignment = HimEvidenceAlignmentClassificationV1.values().toList()

        direct.forEach { alignment ->
            assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH, compare(HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED, alignment))
        }
        allAlignment.filterNot { it in direct }.forEach { alignment ->
            assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MISMATCH, compare(HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED, alignment))
        }
        assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH, compare(HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED, HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY))
        assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH, compare(HimEvidenceAlignmentAuditClassification.MODIFIER_ONLY, HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER))
        listOf(
            HimEvidenceAlignmentClassificationV1.MISSING_PRIMARY_IDENTITY,
            HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY,
        ).forEach { alignment ->
            assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH, compare(HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY, alignment))
        }
        listOf(
            HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT,
            HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND,
            HimEvidenceAlignmentAuditClassification.INVALID_PROJECTION,
            HimEvidenceAlignmentAuditClassification.SOURCE_OR_BINDING_FAILURE,
        ).forEach { frozen ->
            allAlignment.forEach { alignment ->
                assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.NOT_RECONSTRUCTABLE, compare(frozen, alignment))
            }
        }
    }

    @Test
    fun persistenceReloadIsIdempotentAndRefusesDifferentBytes() {
        val fixture = fixture()
        val result = completed(fixture.request)
        val file = createTempDirectory("him-enrichment").toFile().resolve("shard.json")

        HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.writeShard(file, fixture.mission, result)
        val firstBytes = file.readBytes()
        val reloaded = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readShard(file, fixture.mission)
        assertEquals(result, reloaded)
        HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.writeShard(file, fixture.mission, result)
        assertContentEquals(firstBytes, file.readBytes())

        val different = result.copy(
            findings = result.findings.mapIndexed { index, finding ->
                if (index == 0) finding.copy(reconstructedReasonCode = "DIFFERENT_REASON") else finding
            },
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }
        assertFailsWith<IllegalArgumentException> {
            HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.writeShard(file, fixture.mission, different)
        }
    }

    @Test
    fun failuresAreTypedSafeAndDoNotExposePathsOrThrowableText() {
        val fixture = fixture()
        val notFound = fixture(failReference = OFF_REFERENCE)
        val failedNotFound = failed(notFound.request)
        assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.EXACT_FETCH_NOT_FOUND, failedNotFound.reason)

        val wrongReference = fixture(wrongReference = OFF_REFERENCE)
        val failedReference = failed(wrongReference.request)
        assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.EXACT_FETCH_REFERENCE_MISMATCH, failedReference.reason)

        val sourceMismatch = fixture().request.copy(fetchPorts = fixture().ports.reversed())
        val failedSource = failed(sourceMismatch)
        assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.SOURCE_BINDING_MISMATCH, failedSource.reason)

        listOf(failedNotFound, failedReference, failedSource).forEach { result ->
            assertFalse(result.safeContext.contains("/"))
            assertFalse(result.safeContext.contains("Exception"))
            assertFalse(result.safeContext.contains("Throwable"))
        }
    }

    @Test
    fun classificationMismatchFailsClosedAndBindingsCannotBeManipulated() {
        val fixture = fixture()
        val wrongClassification = auditAggregateFor(
            fixture.plan,
            override = { finding ->
                if (finding.evidenceReference == CIQUAL_REFERENCE) {
                    finding.copy(classification = HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED)
                } else finding
            },
        )
        val wrongMission = enrichmentMission(fixture.plan, wrongClassification)
        val mismatch = failed(
            fixture.request.copy(
                mission = wrongMission,
                auditAggregate = wrongClassification,
                auditShard = wrongClassification.shardResults.first(),
            ),
        )
        assertEquals(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.RECONSTRUCTED_CLASSIFICATION_MISMATCH, mismatch.reason)

        val aggregateTampered = fixture.request.copy(
            auditAggregate = fixture.aggregate.copy(logicalDigest = "f".repeat(64)),
        )
        assertTrue(failed(aggregateTampered).reason != HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFailureReasonV1.RECONSTRUCTED_CLASSIFICATION_MISMATCH)

        val shardTampered = fixture.request.copy(
            auditShard = fixture.request.auditShard.copy(missionDigest = "f".repeat(64)),
        )
        assertTrue(failed(shardTampered).safeContext.isNotBlank())
    }

    @Test
    fun emptyAggregateShardSetIsAValidSubsetButForeignShardIsRejected() {
        val fixture = fixture()
        val counters = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(0, 0, 0, 0, 0, 0, 0, 0, 0)
        val empty = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1(
            missionDigest = fixture.mission.logicalDigest,
            state = HimEvidenceAlignmentCatalogAuditState.PARTIAL,
            shardResults = emptyList(),
            counters = counters,
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }
        empty.validateAgainst(fixture.mission)

        val foreign = fixture.mission.copy(auditMissionDigest = "e".repeat(64)).let {
            it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it))
        }
        assertFailsWith<IllegalArgumentException> { empty.validateAgainst(foreign) }
    }

    @Test
    fun exactFetchPortHasNoSearchOperation() {
        assertTrue(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentExactFetchPortV1::class.java.declaredMethods.none { it.name == "search" })
    }

    private fun compare(
        frozen: HimEvidenceAlignmentAuditClassification,
        alignment: HimEvidenceAlignmentClassificationV1,
    ) = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeV1.compareClassifications(frozen, alignment)

    private fun completed(
        request: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeRequestV1,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1 = when (
        val result = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeV1.executeShard(request)
    ) {
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Completed -> result.value
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Failed -> fail("${result.reason} ${result.safeContext}")
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Skipped -> fail(result.reason)
    }

    private fun failed(
        request: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeRequestV1,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Failed = when (
        val result = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeV1.executeShard(request)
    ) {
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Failed -> result
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Completed -> fail("EXPECTED_FAILED_RESULT")
        is HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult.Skipped -> fail(result.reason)
    }

    private fun fixture(
        failReference: String? = null,
        wrongReference: String? = null,
    ): Fixture {
        val plan = auditPlan()
        val aggregate = auditAggregateFor(plan)
        val mission = enrichmentMission(plan, aggregate)
        val ports = ports(failReference, wrongReference)
        return Fixture(
            plan = plan,
            aggregate = aggregate,
            mission = mission,
            ports = ports,
            request = request(plan, aggregate, mission, ports),
        )
    }

    private fun request(
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        aggregate: HimEvidenceAlignmentCatalogAuditAggregateV1,
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
        ports: List<FakePort>,
    ) = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeRequestV1(
        enabled = true,
        mission = mission,
        auditPlan = plan,
        auditAggregate = aggregate,
        auditShard = aggregate.shardResults.first(),
        catalog = catalog(),
        authority = authority(),
        fetchPorts = ports,
    )

    private fun enrichmentMission(
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        aggregate: HimEvidenceAlignmentCatalogAuditAggregateV1,
    ) = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.plan(
        plan,
        HimEvidenceAlignmentCatalogAuditFindingsEnrichmentProvenanceV1(
            auditHead = plan.bindings.gitHead,
            enrichmentImplementationHead = "b".repeat(40),
            auditMission = enrichmentFile("build/knowledge/reports/him/mission.json", plan.missionDigest),
            auditAggregate = enrichmentFile("build/knowledge/reports/him/aggregate.json", aggregate.logicalDigest),
            auditMissionLogicalDigest = plan.missionDigest,
            auditAggregateLogicalDigest = aggregate.logicalDigest,
            auditShardBindings = aggregate.shardResults.map {
                HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardFileBindingV1(
                    it.shardId,
                    enrichmentFile("build/knowledge/reports/him/${it.shardId}.json", it.logicalDigest),
                )
            },
            auditBindings = plan.bindings,
            extractorEvaluatorImplementationSha256 = "f".repeat(64),
        ),
    )

    private fun enrichmentFile(path: String, logicalDigest: String) =
        HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFileBindingV1(path, 1, "1".repeat(64), logicalDigest)

    private fun auditPlan() = HimEvidenceAlignmentCatalogAuditContractV1.plan(bindings(), authority(), 2)

    private fun auditAggregateFor(
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        override: ((HimEvidenceAlignmentCatalogAuditFindingV1) -> HimEvidenceAlignmentCatalogAuditFindingV1)? = null,
    ) = HimEvidenceAlignmentCatalogAuditContractV1.aggregate(
        plan,
        plan.shards.map { shard ->
            HimEvidenceAlignmentCatalogAuditContractV1.shardResult(
                plan,
                shard.shardId,
                shard.canonicalEntityIds.flatMap { entityId ->
                    HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.map { source ->
                        val original = originalFinding(entityId, source)
                        val finding = (override?.invoke(original) ?: original)
                        val found = finding.evidenceReference != null
                        HimEvidenceAlignmentCatalogAuditCellV1(
                            entityId = entityId,
                            source = source,
                            completed = true,
                            queries = 1,
                            retrievalHits = if (found) 1 else 0,
                            deduplicatedProjections = if (found) 1 else 0,
                            fetches = if (found) 1 else 0,
                            findings = listOf(finding),
                            modifierCoverage = emptyList(),
                            technicalValid = true,
                            technicalErrors = 0,
                        )
                    }
                },
            )
        },
    )

    private fun originalFinding(entityId: String, source: HimGroundTruthSource) = when {
        entityId == "c00001" && source == HimGroundTruthSource.OPEN_FOOD_FACTS -> auditFinding(entityId, source, HimEvidenceAlignmentAuditClassification.MODIFIER_ONLY, OFF_REFERENCE)
        entityId == "c00001" && source == HimGroundTruthSource.AGRIBALYSE -> auditFinding(entityId, source, HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED, AGRIBALYSE_REFERENCE)
        entityId == "c00001" && source == HimGroundTruthSource.CIQUAL -> auditFinding(entityId, source, HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED, CIQUAL_REFERENCE)
        entityId == "c00001" && source == HimGroundTruthSource.GLYCEMIC_INDEX -> auditFinding(entityId, source, HimEvidenceAlignmentAuditClassification.MODIFIER_ONLY, GI_REFERENCE)
        entityId == "c00002" && source == HimGroundTruthSource.OPEN_FOOD_FACTS -> auditFinding(entityId, source, HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED, OFF_REFERENCE)
        entityId == "c00002" && source == HimGroundTruthSource.CIQUAL -> auditFinding(entityId, source, HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND, TAXONOMY_REFERENCE)
        else -> auditFinding(entityId, source, HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT, null)
    }

    private fun auditFinding(entityId: String, source: HimGroundTruthSource, classification: HimEvidenceAlignmentAuditClassification, reference: String?) =
        HimEvidenceAlignmentCatalogAuditFindingV1(entityId, source, classification, reference, null)

    private fun ports(failReference: String?, wrongReference: String?) = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.map { source ->
        FakePort(source, records(), failReference, wrongReference)
    }

    private class FakePort(
        override val source: HimGroundTruthSource,
        private val records: Map<String, HimEvidenceRetrievalIndexRecord>,
        private val failReference: String?,
        private val wrongReference: String?,
    ) : HimEvidenceAlignmentCatalogAuditFindingsEnrichmentExactFetchPortV1 {
        var fetchCount: Int = 0
            private set

        override fun fetch(reference: HimEvidenceRecordReference): HimEvidenceRetrievalIndexRecord? {
            fetchCount++
            if (reference.value == failReference) return null
            val record = records[reference.value] ?: return null
            return if (reference.value == wrongReference) {
                record.copy(sourceRecordReference = HimEvidenceRecordReference.offProduct(999999, "wrong"))
            } else record
        }
    }

    private data class Fixture(
        val plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        val aggregate: HimEvidenceAlignmentCatalogAuditAggregateV1,
        val mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
        val ports: List<FakePort>,
        val request: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeRequestV1,
    )

    private fun records() = listOf(offRecord(), agribalyseRecord(), ciqualRecord(), taxonomyRecord(), giRecord()).associateBy {
        it.sourceRecordReference.value
    }

    private fun offRecord() = HimOffEvidenceProjectionV1.fromProjectionJson(
        """
        {"rowOrdinal":1,"source":{"code":"4260694945322"},"identity":{"productName":"Vanille Pudding mit Bourbon-Vanille","productNameGerman":null,"productNameEnglish":null,"genericName":null,"genericNameGerman":null,"genericNameEnglish":null,"brands":[],"productType":null},"taxonomy":{"categories":["Desserts"]},"ingredients":{"text":"Vanille"},"quality":{}}
        """.trimIndent(),
    ).copy(sourceRecordReference = HimEvidenceRecordReference.offProduct(4474818, "4260694945322"), internalRecordKey = 4474818)

    private fun agribalyseRecord() = HimAgribalyseEvidenceProjectionV1.fromProjectionJson(
        """
        {"rowOrdinal":1,"agbCode":"31044","ciqualCode":"fixture-ciqual","seasonCode":"fixture-season","airTransportCode":"no","productNameFr":"Sucre vanillé","lciName":"Sugar, vanilla flavoured","foodGroup":"Sugar and sweet products","foodSubgroup":"Sugar","preparation":"fixture","delivery":"fixture","packagingApproach":"fixture"}
        """.trimIndent(),
    ).copy(sourceRecordReference = HimEvidenceRecordReference.agribalyse(1804, "31044"), internalRecordKey = 1804)

    private fun ciqualRecord() = HimCiqualEvidenceProjectionV1.fromProjectionJson(
        """
        {"recordKind":"FOOD","alimCode":"11057","nameFr":"Vanille, gousse","nameEn":"Vanilla, pod","scientificName":{"lexicalValue":"Vanilla planifolia","missingAttributeValue":null},"groupCode":"01","groupNameFr":"Spices","groupNameEn":"Spices","subgroupCode":"01","subgroupNameFr":"Spices","subgroupNameEn":"Spices","subSubgroupCode":"01","subSubgroupNameFr":"Spices","subSubgroupNameEn":"Spices"}
        """.trimIndent(),
        3,
    ).copy(sourceRecordReference = HimEvidenceRecordReference.ciqualFood("11057"))

    private fun taxonomyRecord() = HimCiqualEvidenceProjectionV1.fromProjectionJson(
        """
        {"recordKind":"TAXONOMY","groupCode":"01","groupNameFr":"Vanille","groupNameEn":"Vanilla","subgroupCode":"01","subgroupNameFr":"Spices","subgroupNameEn":"Spices","subSubgroupCode":"01","subSubgroupNameFr":"Pod","subSubgroupNameEn":"Pod"}
        """.trimIndent(),
        9,
    ).copy(sourceRecordReference = HimEvidenceRecordReference.ciqualTaxonomy("01", "01", "01"))

    private fun giRecord() = HimGlycemicIndexEvidenceProjectionV1.fromProjectionJson(
        """
        {"recordKind":"MEASUREMENT","arrayOrdinal":823,"foodNumber":823,"pageNumber":1,"foodItem":{"lexicalValue":"Prince Petit Déjeuner Vanille (LU, France and Spain)","status":"PRESENT"},"country":{"lexicalValue":"France","status":"PRESENT"},"year":{"lexicalValue":"2010","status":"PRESENT"},"gi":{"lexicalValue":"73","status":"PRESENT"},"sem":{"lexicalValue":"6","status":"PRESENT"},"gl":{"lexicalValue":"11","status":"PRESENT"},"subjects":{"lexicalValue":"10","status":"PRESENT"},"availableCarbohydrate":{"lexicalValue":"50","status":"PRESENT"},"testPortion":{"lexicalValue":"119","status":"PRESENT"},"referenceFoodTime":{"lexicalValue":"Bread, 2h","status":"PRESENT"},"timepoints":{"lexicalValue":"Standard","status":"PRESENT"},"sampleCollection":{"lexicalValue":"Capillary","status":"PRESENT"},"analysisMethod":{"lexicalValue":"YSI","status":"PRESENT"},"referenceCode":{"lexicalValue":"UO7","status":"PRESENT"},"sourceContext":{"majorCategory":"COOKIES","subcategory":null,"deeperHeading":null}}
        """.trimIndent(),
        4,
    ).copy(sourceRecordReference = HimEvidenceRecordReference.gi("measurement", 823))

    private fun catalog() = HimProductOnlyCanonicalMaster(
        path = "fixture/catalog.json",
        contentSha256 = "a".repeat(64),
        records = fixtureNames().map { name ->
            HimProductOnlyCanonical(name, normalize(name), emptyList())
        },
    )

    private fun authority() = HimCanonicalFamilyAuthority(
        schemaVersion = "AUTHORITY_V1",
        sourceCatalog = HimCanonicalFamilySourceCatalog("fixture/catalog.json", "a".repeat(64), 22),
        families = fixtureNames().mapIndexed { index, name ->
            family("c%05d".format(index + 1), name, normalize(name), index + 1)
        },
    )

    private fun fixtureNames() = listOf("Vanille", "Apfel", "Pudding", "Alpha", "Zucker", "Keks", "Beta") +
        (8..22).map { "Item%02d".format(it) }

    private fun family(id: String, name: String, normalized: String, index: Int) = HimCanonicalFamily(
        canonicalId = HimEntityId(id),
        canonicalName = name,
        normalizedName = normalized,
        taxonomyPaths = emptyList(),
        lifecycleStatus = HimLifecycleStatus.ACTIVE,
        identities = listOf(HimCanonicalIdentity(HimEntityId("b${id.takeLast(5)}"), name, normalized, HimLifecycleStatus.ACTIVE, emptyList(), emptyList())),
        variants = listOf(HimCanonicalVariant(HimEntityId("d${id.takeLast(5)}"), "Sorte$index", "sorte$index", HimLifecycleStatus.ACTIVE)),
        aliases = when (name) {
            "Vanille" -> listOf(HimCanonicalAlias(HimEntityId("e${id.takeLast(5)}"), "Vanilla", "vanilla", HimLifecycleStatus.ACTIVE))
            "Zucker" -> listOf(
                HimCanonicalAlias(HimEntityId("e${id.takeLast(4)}a"), "Sucre", "sucre", HimLifecycleStatus.ACTIVE),
                HimCanonicalAlias(HimEntityId("e${id.takeLast(4)}b"), "Sugar", "sugar", HimLifecycleStatus.ACTIVE),
            )
            "Keks" -> listOf(
                HimCanonicalAlias(HimEntityId("e${id.takeLast(4)}a"), "Cookies", "cookies", HimLifecycleStatus.ACTIVE),
                HimCanonicalAlias(HimEntityId("e${id.takeLast(4)}b"), "Biscuit", "biscuit", HimLifecycleStatus.ACTIVE),
            )
            else -> emptyList()
        },
    )

    private fun bindings() = HimEvidenceAlignmentCatalogAuditBindingsV1(
        gitHead = "a".repeat(40),
        implementationBindingSha256 = "b".repeat(64),
        canonicalCatalog = HimEvidenceAlignmentCatalogAuditFileBindingV1("data/catalog.json", 1, "c".repeat(64)),
        authority = HimEvidenceAlignmentCatalogAuditFileBindingV1("data/authority.json", 1, "d".repeat(64)),
        groundTruthReleaseReference = "release:v1:${"e".repeat(64)}",
        sourceBindings = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.mapIndexed { index, source ->
            HimEvidenceAlignmentCatalogAuditSourceBindingV1(
                source = source,
                indexRelativePath = "data/index/${source.name.lowercase()}.sqlite",
                indexByteSize = (index + 1).toLong(),
                sqliteFileSha256 = "f".repeat(64),
                sourceArtifactPath = source.artifactPath,
                sourceArtifactSha256 = "1".repeat(64),
                schemaVersion = "INDEX_SCHEMA_V1",
                indexBuildPolicyVersion = "INDEX_BUILD_V1",
                evidenceProjectionPolicyVersion = "PROJECTION_V1",
                logicalContentSha256 = "2".repeat(64),
                buildState = HimEvidenceRetrievalIndexBuildState.VALIDATED,
            )
        },
    )

    private fun normalize(value: String) = value.lowercase()

    companion object {
        private const val OFF_REFERENCE = "off:product:row:4474818:code:4260694945322"
        private const val AGRIBALYSE_REFERENCE = "agribalyse:row:1804:agb:31044"
        private const val CIQUAL_REFERENCE = "ciqual:food:11057"
        private const val TAXONOMY_REFERENCE = "ciqual:taxonomy:01/01/01"
        private const val GI_REFERENCE = "gi:measurement:823"
    }
}
