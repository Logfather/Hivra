package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1Test {
    @Test
    fun contractAndRequestSurfaceAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_RUNTIME_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1.VERSION)
        val fields = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeRequestV1::class.java
            .declaredFields.filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }.map { it.name }
        assertEquals(
            listOf("enabled", "outputRoot", "supplementImplementationHead", "mission", "reviewPacket", "reviewPacketBinding", "sourceProjectionInputs", "foundationInput"),
            fields,
        )
    }

    @Test
    fun disabledReturnsBeforeValidationOrOutputAccess() {
        val root = Files.createTempDirectory("him-direct-runtime-disabled-").resolve("missing").toFile()
        val result = execute(root, enabled = false, head = "bad")
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Disabled>(result)
        assertFalse(root.exists())
    }

    @Test
    fun validRequestCreatesFourUnitTwelveCardSupplement() {
        val result = completed()
        assertEquals(4, result.supplement.bundles.size)
        assertEquals(4, result.counters.reviewUnits)
        assertEquals(4, result.counters.sourceEvidenceProjections)
        assertEquals(4, result.counters.catalogTargetEvidenceRecords)
        assertEquals(4, result.counters.authorityTargetEvidenceRecords)
        assertEquals(12, result.counters.directEvidenceReferences)
        assertEquals(4, result.counters.supportingEvidenceReferences)
        assertEquals(2, result.counters.contradictingEvidenceReferences)
        assertEquals(6, result.counters.contextOnlyEvidenceReferences)
        assertEquals(4, result.counters.distinctSourceRecords)
        assertEquals(2, result.counters.distinctCanonicalTargets)
        assertTrue(result.supplement.bundles.all { it.evidence.size == 3 })
        assertTrue(result.supplement.bundles.all { it.evidence.all { evidence -> evidence.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT } })
        assertTrue(result.supplement.bundles.all { it.evidence.none { evidence -> evidence.recordReference.contains("decision") } })
    }

    @Test
    fun exactSourceReferencesPositionsAndTargetsAreDerived() {
        val result = completed()
        val refs = result.supplement.bundles.map { bundle ->
            bundle.evidence.single { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }.recordReference
        }
        assertEquals(EXPECTED_SOURCE_REFERENCES, refs)
        result.supplement.bundles.forEachIndexed { index, bundle ->
            val source = bundle.evidence.single { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }
            val catalog = bundle.evidence.single { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD }
            val authority = bundle.evidence.single { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD }
            val expectedPosition = if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
            assertEquals(expectedPosition, source.position)
            assertEquals(if (index == 0 || index == 2) HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION else HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY, catalog.position)
            assertEquals(HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY, authority.position)
            assertEquals("catalog:${bundle.reviewUnit.canonicalEntityId}", catalog.recordReference)
            assertEquals("authority:${bundle.reviewUnit.canonicalEntityId}", authority.recordReference)
            assertEquals(source.recordReference, source.sourceProjection!!.recordReference)
        }
    }

    @Test
    fun sourceInputOrderIsCanonicalizedAndRepeatedExecutionIsIdempotent() {
        val firstRoot = Files.createTempDirectory("him-direct-runtime-a-").toFile()
        val secondRoot = Files.createTempDirectory("him-direct-runtime-b-").toFile()
        val first = completed(execute(firstRoot, sourceInputs = sourceInputs()))
        val second = completed(execute(secondRoot, sourceInputs = sourceInputs().reversed()))
        assertEquals(first.supplement, second.supplement)
        assertEquals(first.supplementBindingDigest, second.supplementBindingDigest)
        assertEquals(first.supplementLogicalDigest, second.supplementLogicalDigest)
        assertEquals(first.jsonSha256, second.jsonSha256)
        assertEquals(first.markdownSha256, second.markdownSha256)
        val repeat = completed(execute(firstRoot, sourceInputs = sourceInputs()))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, repeat.persistenceStatus)
        assertEquals(first.jsonSha256, repeat.jsonSha256)
        assertEquals(first.markdownSha256, repeat.markdownSha256)
        assertTrue(first.jsonPath.startsWith("p1-artischocken-herzen-brie-double-creme-v1/"))
        assertFalse(first.jsonPath.startsWith('/'))
        assertFalse(first.markdownPath.startsWith('/'))
    }

    @Test
    fun persistedPairReloadsAndHasStableBytesAndDigests() {
        val root = Files.createTempDirectory("him-direct-runtime-persist-").toFile()
        val result = completed(execute(root))
        val json = root.resolve(result.jsonPath)
        val markdown = root.resolve(result.markdownPath)
        assertTrue(json.isFile)
        assertTrue(markdown.isFile)
        assertEquals(result.jsonByteSize, json.length())
        assertEquals(result.markdownByteSize, markdown.length())
        assertEquals(result.supplement, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.readSupplement(json))
        assertEquals(result.jsonSha256, sha256(json.readBytes()))
        assertEquals(result.markdownSha256, sha256(markdown.readBytes()))
    }

    @Test
    fun missionPacketAndImplementationBindingsFailClosed() {
        val mission = execute(Files.createTempDirectory("him-direct-runtime-mission-").toFile(), mission = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION.copy(scopeId = "wrong"))
        assertFailed(mission, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_MISSION)
        assertFailed(execute(Files.createTempDirectory("him-direct-runtime-head-").toFile(), head = "b".repeat(39)), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_IMPLEMENTATION_HEAD)
        val packetBinding = packetBinding().copy(packetLogicalDigest = "f".repeat(64))
        assertFailed(execute(Files.createTempDirectory("him-direct-runtime-packet-").toFile(), reviewPacketBinding = packetBinding), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_PACKET_BINDING)
    }

    @Test
    fun foundationAndTargetBindingsFailClosed() {
        val base = foundation()
        val brokenCatalog = base.copy(catalog = base.catalog.copy(records = base.catalog.records.drop(1)))
        assertFailed(execute(Files.createTempDirectory("him-direct-runtime-foundation-").toFile(), foundationInput = brokenCatalog), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_FOUNDATION)
        val brokenAuthority = base.copy(authority = base.authority.copy(families = base.authority.families.drop(1)))
        assertFailed(execute(Files.createTempDirectory("him-direct-runtime-target-").toFile(), foundationInput = brokenAuthority), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_FOUNDATION)
    }

    @Test
    fun sourceCountDuplicateMissingAndUnexpectedBindingsFailClosed() {
        val root = Files.createTempDirectory("him-direct-runtime-inputs-").toFile()
        assertFailed(execute(root, sourceInputs = sourceInputs().drop(1)), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_SOURCE_PROJECTION_COUNT)
        val duplicate = sourceInputs().toMutableList().also { it[1] = it[0] }
        assertFailed(execute(Files.createTempDirectory("him-direct-runtime-duplicate-").toFile(), sourceInputs = duplicate), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.DUPLICATE_SOURCE_PROJECTION)
        val wrong = sourceInputs().toMutableList().also { it[0] = it[0].copy(canonicalEntityId = "rVnyq7") }
        assertFailed(execute(Files.createTempDirectory("him-direct-runtime-wrong-").toFile(), sourceInputs = wrong), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_SOURCE_PROJECTION_BINDING)
    }

    @Test
    fun sourceArtifactsFieldsAndProjectionDigestAreBound() {
        val base = sourceInputs()
        val forbidden = base.toMutableList().also { it[0] = it[0].copy(artifact = it[0].artifact.copy(relativePath = "review-packet/source.json")) }
        assertFailed(execute(Files.createTempDirectory("him-direct-runtime-artifact-").toFile(), sourceInputs = forbidden), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_SOURCE_FIELD)
        val empty = base.toMutableList().also { it[0] = it[0].copy(fields = emptyList()) }
        assertFailed(execute(Files.createTempDirectory("him-direct-runtime-fields-").toFile(), sourceInputs = empty), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.INVALID_SOURCE_FIELD)
        val changed = base.toMutableList().also { it[0] = it[0].copy(projectionLogicalDigest = "0".repeat(64)) }
        assertFailed(execute(Files.createTempDirectory("him-direct-runtime-digest-").toFile(), sourceInputs = changed), HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1.SOURCE_PROJECTION_DIGEST_MISMATCH)
    }

    @Test
    fun unicodeAndCompleteFieldValuesRemainUntouched() {
        val unicode = sourceInputs().toMutableList()
        val fields = listOf(field("identity.productName", "Artischocken Herzen 😀\nfull source value"), field("identity.brand", "Bränd"))
        unicode[0] = unicode[0].copy(fields = fields, projectionLogicalDigest = "")
        unicode[0] = unicode[0].copy(projectionLogicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1.projectionLogicalDigest(unicode[0]))
        val result = completed(execute(Files.createTempDirectory("him-direct-runtime-unicode-").toFile(), sourceInputs = unicode))
        val source = result.supplement.bundles.first().evidence.single { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }
        assertEquals("Artischocken Herzen 😀\nfull source value", source.fields.first { it.fieldReference == "identity.productName" }.fullValue)
    }

    @Test
    fun onlyTemporaryOutputAndNoDecisionSurfaceAreUsed() {
        val root = Files.createTempDirectory("him-direct-runtime-safe-").toFile()
        val result = completed(execute(root))
        assertTrue(result.jsonPath.startsWith("p1-artischocken-herzen-brie-double-creme-v1/"))
        assertFalse(result.jsonPath.contains("build/"))
        val names = listOf(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1::class.java.declaredFields.map { it.name },
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeRequestV1::class.java.declaredFields.map { it.name },
        ).flatten()
        assertTrue(names.none { it.contains("decision", ignoreCase = true) || it.contains("provider", ignoreCase = true) || it.contains("sqlite", ignoreCase = true) })
        assertNotEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.OUTPUT_ROOT, root.path)
    }

    private fun completed(result: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1) =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Completed>(result)

    private fun completed(
        outputRoot: File = Files.createTempDirectory("him-direct-runtime-").toFile(),
        sourceInputs: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1> = sourceInputs(),
    ) = completed(execute(outputRoot, sourceInputs = sourceInputs))

    private fun assertFailed(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1,
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeFailureReasonV1,
    ) {
        assertEquals(reason, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Failed>(result).reason)
    }

    private fun execute(
        outputRoot: File,
        enabled: Boolean = true,
        head: String = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_IMPLEMENTATION_HEAD,
        mission: HimZeroCandidateRecoveryHumanReviewP1PilotMissionV1 = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION,
        reviewPacketBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementReviewPacketBindingV1 = packetBinding(),
        sourceInputs: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1> = sourceInputs(),
        foundationInput: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFoundationInputV1 = foundation(),
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1.execute(
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeRequestV1(
            enabled,
            outputRoot,
            head,
            mission,
            packet(),
            reviewPacketBinding,
            sourceInputs,
            foundationInput,
        ),
    )

    @Test
    fun packetFixtureIsFullyInMemoryAndValidatesWithoutRepositoryAccess() {
        val value = packet()
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketValidationResultV1.Valid>(
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.validate(
                value,
                HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION,
                value.humanReviewInputBinding,
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_LOGICAL_DIGEST,
            value.packetLogicalDigest,
        )
    }

    private fun packet() = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.create(
        missionBinding = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.missionBinding(HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION),
        humanReviewInputBinding = inputBinding(),
        corpusFileBinding = inputBinding().corpusFileBinding,
        corpusLogicalDigest = inputBinding().corpusLogicalDigest,
        corpusBindingDigest = inputBinding().corpusBindingDigest,
        packetImplementationHead = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_IMPLEMENTATION_HEAD,
        items = listOf(
            packetItem(
                stableEntryId = "a98f67c7aa8af729e27d402585df4c78d2a1a9c3f85e636dd7d257cba1810ebd",
                reviewUnitId = "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e",
                canonicalEntityId = "ZuhV5V",
                groupOrdinal = 1,
                groupDisplayValue = "Artischocken Herzen",
                evidenceReference = "off:product:row:431650:code:4002239680509",
                findingOccurrenceId = "f815ccdc47d36e85fb66e440ab842f123e7a94864828e300f9e78d1561f14f95",
                fields = listOf(
                    "identity.brands" to "xx:feinkost-dittmann",
                    "identity.productName" to "Artischocken Herzen",
                    "identity.productNameEnglish" to "Artischocken Herzen",
                    "identity.productNameGerman" to "Artischocken Herzen",
                    "identity.productType" to "food",
                    "taxonomy.categories" to "de:artischocken-herzen",
                    "taxonomy.categoryHierarchy" to "de:artischocken herzen",
                    "taxonomy.pnnsGroups" to "unknown",
                ),
            ),
            packetItem(
                stableEntryId = "ec4d7ccf39c1b9dbe184a0af90190cbc5d6a9e96ce3e3af121f5f59258eb5e13",
                reviewUnitId = "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7",
                canonicalEntityId = "rVnyq7",
                groupOrdinal = 4,
                groupDisplayValue = "Brie double crème",
                evidenceReference = "off:product:row:3272579:code:0061483010917",
                findingOccurrenceId = "591e856a64e912802e2b49f15b1d4dbb34a75a4c7f10f88e1a1d995a585c0447",
                fields = listOf(
                    "identity.productName" to "Brie double crème",
                    "identity.productNameEnglish" to "Brie double crème",
                    "identity.productType" to "food",
                    "taxonomy.pnnsGroups" to "unknown",
                ),
            ),
            packetItem(
                stableEntryId = "6db7a77b0a3ff2471b8001b1644bc7a957efa52369df722899298c87d286ed71",
                reviewUnitId = "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70",
                canonicalEntityId = "ZuhV5V",
                groupOrdinal = 1,
                groupDisplayValue = "Artischocken Herzen",
                evidenceReference = "off:product:row:1551407:code:4013200552046",
                findingOccurrenceId = "ee0912b2605140cca8ae9735a8a5e9414e9a16c87855ce756cf0b9285618e0b7",
                fields = listOf(
                    "identity.productName" to "Artischocken Herzen",
                    "identity.productNameGerman" to "Artischocken Herzen",
                    "identity.productType" to "food",
                    "taxonomy.pnnsGroups" to "unknown",
                ),
            ),
            packetItem(
                stableEntryId = "f4957e490b1714a1e48ca5d43ae762564603ac712842b3c2714b9f36bf2c41df",
                reviewUnitId = "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50",
                canonicalEntityId = "rVnyq7",
                groupOrdinal = 4,
                groupDisplayValue = "Brie double crème",
                evidenceReference = "off:product:row:3322623:code:2026088009283",
                findingOccurrenceId = "0cdac56b8596846af722542e7402c5b1c0e413ae0f74ae4ff54d1fb6b8d99a5f",
                fields = listOf(
                    "identity.productName" to "Brie double crème",
                    "identity.productNameEnglish" to "Brie double crème",
                    "identity.productType" to "food",
                    "taxonomy.pnnsGroups" to "unknown",
                ),
            ),
        ),
    )

    private fun packetItem(
        stableEntryId: String,
        reviewUnitId: String,
        canonicalEntityId: String,
        groupOrdinal: Int,
        groupDisplayValue: String,
        evidenceReference: String,
        findingOccurrenceId: String,
        fields: List<Pair<String, String>>,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketItemV1(
        stableEntryId = stableEntryId,
        reviewUnitId = reviewUnitId,
        canonicalEntityId = canonicalEntityId,
        groupOrdinal = groupOrdinal,
        groupDisplayValue = groupDisplayValue,
        sourceContext = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketSourceContextV1(
            source = HimGroundTruthSource.OPEN_FOOD_FACTS,
            recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
            evidenceReference = evidenceReference,
            findingOccurrenceIds = listOf(findingOccurrenceId),
            selectionReasons = listOf(HimZeroCandidateRecoveryReviewSelectionReasonV1.RECURRING_PRIMARY_VALUE_GROUP),
            primaryValues = listOf(groupDisplayValue),
            primaryValueBucket = HimZeroCandidateCauseAnalysisPrimaryBucketV1.PRIMARY_TEXT_PRESENT_NO_MATCH,
            recurringGroupMemberships = listOf(
                HimZeroCandidateRecoveryReviewGroupMembershipV1(
                    groupDisplayValue,
                    HimZeroCandidateRecoveryReviewPriorityV1.REPEATED_TEXT_SINGLE_AUDIT_TARGET,
                    2,
                    2,
                    listOf(canonicalEntityId),
                ),
            ),
        ),
        originalFields = fields.map { (name, value) -> packetField(name, value) },
        targetContext = target(canonicalEntityId, if (canonicalEntityId == "ZuhV5V") "Artischocken" else "Crème double"),
        evidenceScope = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceScopeV1(
            evidenceKind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD,
            directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.INDIRECT,
            artifactReference = "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-review-corpus/v1/review-corpus.v1.json",
            artifactSha256 = "4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016",
            artifactLogicalDigest = "3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02",
            recordReference = evidenceReference,
            fieldReferences = fields.map { it.first },
            coverage = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY,
        ),
        contextLimitations = listOf(
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FULL_SOURCE_RECORD_NOT_INCLUDED,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.IDENTITY_TERMS_ABSENT,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.ALIAS_TERMS_ABSENT,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.SOURCE_PROJECTION_NOT_INCLUDED,
        ),
        itemBindingDigest = "",
        itemLogicalDigest = "",
    )

    private fun packetField(name: String, value: String) = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketOriginalFieldV1(
        fieldName = name,
        originalValue = value,
        originalValueSha256 = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(value),
        humanDisplayValue = value,
        displayState = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketDisplayStateV1.FULL,
        originalCharacterCount = value.length,
    )

    private fun target(id: String, label: String): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1 {
        val base = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketTargetContextV1(
            canonicalEntityId = id,
            expectedDisplayLabel = label,
            registryResolution = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED,
            authorityResolution = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketResolutionV1.RESOLVED,
            catalogRecordReference = "catalog:$id",
            authorityRecordReference = "authority:$id",
            identityTerms = emptyList(),
            aliasTerms = emptyList(),
            targetContextDigest = "",
        )
        return base.copy(targetContextDigest = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.targetContextDigest(base))
    }

    private fun inputBinding(): HimZeroCandidateRecoveryHumanReviewInputBindingV1 {
        val corpusInput = HimZeroCandidateRecoveryReviewCorpusInputBindingV1(
            causeAnalysis = HimZeroCandidateCauseAnalysisFileBindingV1(
                "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-cause-analysis/v1/analysis.v1.json",
                12686016,
                "bd725f67889eb39cfbc8016ec176834bc90774020a9eff4cc5498141df171314",
                "692c1e6f09e05598066cd1cf841dfdbed59c3fbd1250476bea2922eb17e0aedc",
            ),
            canonicalCatalog = HimZeroCandidateRecoveryReviewCorpusFileBindingV1(
                "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json",
                266953,
                "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
                "6af4bcdcf8a0cb442533ca42dee5ef3cc44d5e3911aed470641ab4b76645aabc",
            ),
            canonicalAuthority = HimZeroCandidateRecoveryReviewCorpusFileBindingV1(
                "data/knowledge/him/canonical-family/groundtruth/releases/eee305ee12b323cde27b4abd4fc074e74b04194f95110b52784c8629d62e17b0/canonical-family-authority.v1.json",
                532624,
                "729b2ca9d8fdb22d9a61097cb4cf853c536d3b1a18ed9965f1f3e1dd17727425",
                "7f6dfccfda1666116879abbd6243c6eb11e63c5d55fe44c8a378c8becb57d638",
            ),
            causeAnalysisLogicalDigest = "692c1e6f09e05598066cd1cf841dfdbed59c3fbd1250476bea2922eb17e0aedc",
            recoveryReviewImplementationHead = "8ccb4fb3ac2fd612bbbdb0d39ee36848b0e95dd4",
            bindingDigest = "b1691dbf87eed143d23ea44275ec5d96bf9483e4675469af5d282d3858288e81",
        )
        return HimZeroCandidateRecoveryHumanReviewInputBindingV1(
            corpusFileBinding = HimZeroCandidateRecoveryHumanReviewFileBindingV1(
                "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-review-corpus/v1/review-corpus.v1.json",
                2359985,
                "4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016",
                "3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02",
            ),
            corpusLogicalDigest = "3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02",
            corpusReportDigest = "4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016",
            corpusBindingDigest = "b1691dbf87eed143d23ea44275ec5d96bf9483e4675469af5d282d3858288e81",
            existingCorpusInputBinding = corpusInput,
            registryBinding = HimZeroCandidateRecoveryHumanReviewFileBindingV1(
                "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json",
                285805,
                "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
                "032188945aa7e165fe48827ee888f4a611f76e23c3eeebffb02b1429a82ed5a1",
            ),
            contractId = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_CONTRACT_V1",
            contractVersion = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_CONTRACT_V1",
            implementationHead = "a1f21b960cf287939a6bd1b45e8bbf1a1be47e8b",
            bindingDigest = "4426f0b5be456ab92e32f7cd9b27378d89b0b3fdc7c2ed46b21415830afc475d",
        )
    }

    private fun packetBinding() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementReviewPacketBindingV1(
        HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/review-packet.json", 1, "1".repeat(64), "2".repeat(64)),
        HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/review-packet.md", 1, "3".repeat(64), "4".repeat(64)),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_INPUT_BINDING_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_BINDING_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_LOGICAL_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_IMPLEMENTATION_HEAD,
    )

    private fun foundation(): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFoundationInputV1 {
        val catalog = HimProductOnlyCanonicalMaster("fixture/catalog.json", "1".repeat(64), buildList {
            add(HimProductOnlyCanonical("Artischocken", "artischocken", emptyList()))
            add(HimProductOnlyCanonical("Crème double", "creme-double", emptyList()))
            addAll((3..1384).map { HimProductOnlyCanonical("Canonical $it", "canonical-$it", emptyList()) })
        })
        val registry = HimEntityIdRegistry(buildList {
            add(HimEntityIdRegistryEntry(HimEntityId("ZuhV5V"), HimEntityType.CANONICAL, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "artischocken"))
            add(HimEntityIdRegistryEntry(HimEntityId("rVnyq7"), HimEntityType.CANONICAL, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "creme-double"))
            addAll((3..1384).map { HimEntityIdRegistryEntry(HimEntityId("c%05d".format(it)), HimEntityType.CANONICAL, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "canonical-$it") })
        })
        val authority = HimCanonicalFamilyAuthority("1", HimCanonicalFamilySourceCatalog(catalog.path, catalog.contentSha256, catalog.records.size), buildList {
            add(HimCanonicalFamily(HimEntityId("ZuhV5V"), "Artischocken", "artischocken", emptyList(), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList()))
            add(HimCanonicalFamily(HimEntityId("rVnyq7"), "Crème double", "creme-double", emptyList(), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList()))
            addAll((3..1384).map { HimCanonicalFamily(HimEntityId("c%05d".format(it)), "Canonical $it", "canonical-$it", emptyList(), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList()) })
        })
        return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFoundationInputV1(
            catalog,
            HimZeroCandidateRecoveryHumanReviewFileBindingV1(catalog.path, 1, catalog.contentSha256, "2".repeat(64)),
            registry,
            HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/registry.json", 1, "3".repeat(64), "4".repeat(64)),
            authority,
            HimZeroCandidateRecoveryHumanReviewFileBindingV1("fixture/authority.json", 1, "5".repeat(64), "6".repeat(64)),
        )
    }

    private fun sourceInputs(): List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1> =
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.FROZEN_REVIEW_UNITS.mapIndexed { index, unit ->
            val references = EXPECTED_SOURCE_REFERENCES
            val fields = listOf(field("identity.productName", if (index == 0 || index == 2) "Artischocken Herzen" else "Brie double crème"), field("identity.brand", if (index == 0 || index == 2) "Maison" else "Fromagerie"))
            val artifact = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1("data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", 1, "a".repeat(64), "b".repeat(64))
            val origin = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1("data/sources/openfoodfacts/raw/source.jsonl.gz", 1, "c".repeat(64), "d".repeat(64))
            val input = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1(
                unit.stableEntryId,
                unit.reviewUnitId,
                unit.canonicalEntityId,
                HimGroundTruthSource.OPEN_FOOD_FACTS,
                HimEvidenceRecordKind.OFF_PRODUCT,
                artifact,
                origin,
                references[index],
                fields,
                "",
            )
            input.copy(projectionLogicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1.projectionLogicalDigest(input))
        }

    private fun field(reference: String, value: String) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1(reference, value, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1.sha256(value))

    private fun sha256(bytes: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        private val EXPECTED_SOURCE_REFERENCES = listOf(
            "off:product:row:431650:code:4002239680509",
            "off:product:row:3272579:code:0061483010917",
            "off:product:row:1551407:code:4013200552046",
            "off:product:row:3322623:code:2026088009283",
        )
    }
}
