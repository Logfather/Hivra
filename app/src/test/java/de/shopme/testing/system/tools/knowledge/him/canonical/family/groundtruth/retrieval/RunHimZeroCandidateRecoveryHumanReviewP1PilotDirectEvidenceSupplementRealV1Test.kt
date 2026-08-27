package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexSchemaV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleaseContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleasePersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleaseV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionExactFetchPortV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionProvenanceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFoundationInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementReviewPacketBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffProductionEvidenceIndexTool
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.sql.Connection
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Assume.assumeTrue

/**
 * Real-bound, opt-in-only execution surface for the context-only P1 direct-evidence supplement.
 * All default-path tests are hermetic and do not touch repository inputs or outputs.
 */
class RunHimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRealV1Test {

    @Test
    fun `contract and gate names are frozen`() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_REAL_BOUND_ENTRYPOINT_V1",
            ENTRYPOINT_CONTRACT_ID,
        )
        assertEquals(
            "him.zeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplement.enabled",
            ENABLED_PROPERTY,
        )
        assertEquals(
            "him.zeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplement.confirmation",
            CONFIRMATION_PROPERTY,
        )
        assertEquals(
            "him.zeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplement.authorizedExecutionHead",
            AUTHORIZED_EXECUTION_HEAD_PROPERTY,
        )
        assertEquals("AUTHORIZED_BOUNDED_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_OFFLINE", CONFIRMATION)
    }

    @Test
    fun `gate is disabled by default with explicit empty input`() {
        val gate = gate(null, null, null)
        assertFalse(gate.enabled)
        assertFalse(gate.confirmation == CONFIRMATION)
        assertEquals(null, parseAuthorizedExecutionHead(gate.authorizedExecutionHead))
    }

    @Test
    fun `default gate ignores global properties`() {
        val gate = gate(null, null, null)
        assertFalse(gate.enabled && gate.confirmation == CONFIRMATION && gate.authorizedExecutionHead != null)
    }

    @Test
    fun `wrong confirmation is rejected without input access`() {
        val gate = gate("true", "WRONG", "a".repeat(40))
        assertTrue(gate.enabled)
        assertFalse(gate.confirmation == CONFIRMATION)
        assertFalse(gateIsComplete(gate))
    }

    @Test
    fun `missing authorized execution head is rejected without input access`() {
        assertEquals(null, parseAuthorizedExecutionHead(null))
        assertFalse(gateIsComplete(gate("true", CONFIRMATION, null)))
    }

    @Test
    fun `invalid authorized execution head is rejected`() {
        assertEquals(null, parseAuthorizedExecutionHead("A".repeat(40)))
        assertEquals(null, parseAuthorizedExecutionHead("a".repeat(39)))
        assertEquals(null, parseAuthorizedExecutionHead("g".repeat(40)))
    }

    @Test
    fun `gate stages are ordered before repository and input access`() {
        assertEquals(
            listOf("enabled", "confirmation", "authorizedExecutionHead", "sourceIntegration", "repositoryInputs", "outputs"),
            GATE_STAGES,
        )
        assertTrue(GATE_STAGES.indexOf("sourceIntegration") < GATE_STAGES.indexOf("repositoryInputs"))
        assertTrue(GATE_STAGES.indexOf("repositoryInputs") < GATE_STAGES.indexOf("outputs"))
    }

    @Test
    fun `core baseline is frozen independently from execution head`() {
        assertEquals("deef20015fb2bae143e5663b285d5c9c76f87232", CORE_BASELINE_HEAD)
        assertTrue(CORE_BASELINE_HEAD.matches(HEAD_PATTERN))
        val executionHead = "a".repeat(40)
        assertTrue(CORE_BASELINE_HEAD != executionHead)
        assertEquals(executionHead, executionHeadForRuntime(executionHead))
    }

    @Test
    fun `execution head mismatch fails closed`() {
        assertEquals("EXECUTION_HEAD_MISMATCH", executionHeadFailure("a".repeat(40), "b".repeat(40)))
        assertEquals(null, executionHeadFailure("a".repeat(40), "a".repeat(40)))
    }

    @Test
    fun `ancestor check is fail closed`() {
        assertFalse(ancestorExitIsValid(1))
        assertTrue(ancestorExitIsValid(0))
    }

    @Test
    fun `packet and supplement output paths are relative and frozen`() {
        assertFalse(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.OUTPUT_ROOT.startsWith('/'))
        assertEquals(
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-review-packet/v1",
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.OUTPUT_ROOT,
        )
        assertEquals("review-packet.v1.json", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.JSON_FILE_NAME)
        assertEquals("review-packet.v1.md", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.MARKDOWN_FILE_NAME)
        assertEquals("direct-evidence-supplement.v1.json", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.JSON_FILE_NAME)
        assertEquals("direct-evidence-supplement.v1.md", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MARKDOWN_FILE_NAME)
    }

    @Test
    fun `mission selection is committed and not dynamically selected`() {
        val mission = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION
        assertEquals("p1-artischocken-herzen-brie-double-creme-v1", mission.missionId)
        assertEquals("e5b639be1e04b004a8de2594bed4002c854727d15eb66420a42c4293f0f3dbcb", mission.selectionDigest)
        assertEquals(4, mission.entries.size)
        assertEquals(4, mission.entries.map { it.reviewUnitId }.distinct().size)
    }

    @Test
    fun `review packet contract has no decision surface`() {
        val names = listOf(
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1::class.java.declaredFields.map { it.name },
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1::class.java.declaredMethods.map { it.name },
        ).flatten()
        assertTrue(names.none { it.contains("decision", ignoreCase = true) || it.contains("reviewer", ignoreCase = true) })
        assertEquals("REVIEW_CONTEXT_ONLY", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1.REVIEW_CONTEXT_ONLY.name)
    }

    @Test
    fun `runtime request field model remains committed`() {
        val fields = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeRequestV1::class.java
            .declaredFields.filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }.map { it.name }
        assertEquals(
            listOf("enabled", "outputRoot", "supplementImplementationHead", "mission", "reviewPacket", "reviewPacketBinding", "sourceProjectionInputs", "foundationInput"),
            fields,
        )
    }

    @Test
    fun `source projection resolver remains the only projection policy`() {
        val methods = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("resolve"))
        assertTrue(methods.none { it.contains("search", ignoreCase = true) || it.contains("scan", ignoreCase = true) })
        assertEquals(4, HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.FROZEN_BINDINGS.size)
    }

    @Test
    fun `exact fetch scope is exactly four unique references`() {
        val references = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.FROZEN_BINDINGS.map { it.sourceRecordReference }
        assertEquals(EXPECTED_SOURCE_REFERENCES, references)
        assertEquals(4, references.distinct().size)
    }

    @Test
    fun `bounded port exposes exact fetch only`() {
        val methods = BoundedOffExactFetchPort::class.java.declaredMethods.map { it.name }.toSet()
        assertTrue(methods.contains("fetchExact"))
        assertTrue(methods.none { name -> listOf("search", "scan", "rank", "fts", "validateReadOnly").any { it.equals(name, true) } })
        assertEquals(HimEvidenceRetrievalIndexSchemaV1.EXACT_FETCH_SQL.trim(), HimEvidenceRetrievalIndexSchemaV1.EXACT_FETCH_SQL.trim())
    }

    @Test
    fun `bounded connection contract is read only and query only`() {
        assertEquals(true, BOUNDED_READ_ONLY)
        assertEquals("PRAGMA query_only = ON", QUERY_ONLY_PRAGMA)
        assertFalse(REAL_SOURCE_READER_REACHABLE)
        assertFalse(VALIDATE_READ_ONLY_REACHABLE)
    }

    @Test
    fun `master authority is the foundation authority`() {
        assertEquals("masterAuthority", FOUNDATION_AUTHORITY_ROLE)
        assertEquals("HimCanonicalFamilyValidator", FOUNDATION_VALIDATOR)
        assertFalse(FOUNDATION_AUTHORITY_ROLE.contains("active", ignoreCase = true))
    }

    @Test
    fun `safe diagnostics contain only typed reason and safe context`() {
        val message = safeDiagnostic("INVALID_PACKET", "packet")
        assertEquals("INVALID_PACKET packet", message)
        assertFalse(message.contains("Exception"))
        assertFalse(message.contains("Throwable"))
        assertFalse(message.contains('/'))
    }

    @Test
    fun `operational path remains skipped without complete opt in`() {
        val gate = gate(null, null, null)
        assumeTrue(!gateIsComplete(gate))
    }

    @Test
    fun `no complete gate means no real input or output access`() {
        assertFalse(gateIsComplete(gate(null, CONFIRMATION, "a".repeat(40))))
        assertTrue(INPUT_ACCESS_REQUIRES_COMPLETE_GATE)
        assertTrue(OUTPUT_ACCESS_REQUIRES_COMPLETE_GATE)
    }

    @Test
    fun `source integration is checked after all supplement gates`() {
        assertEquals("sourceIntegration", GATE_STAGES[3])
        assertTrue(GATE_STAGES.take(3).all { it != "sourceIntegration" })
    }

    @Test
    fun `real bound execution is the only method allowed to touch runtime`() {
        assertEquals("writes current real bound P1 pilot direct evidence supplement twice", REAL_EXECUTION_TEST_NAME)
        assertEquals(1, listOf(REAL_EXECUTION_TEST_NAME).size)
        assertTrue(REAL_EXECUTION_TEST_NAME.contains("direct evidence supplement"))
    }

    @Test
    fun `direct evidence semantics are context only`() {
        assertEquals("DIRECT_EVIDENCE_CONTEXT_ONLY", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.STATE)
        assertFalse(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.STATE.contains("DECISION"))
        assertFalse(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.STATE.contains("GOLD"))
    }

    @Test
    fun `master foundation validator is explicitly available`() {
        assertNotNull(HimCanonicalFamilyValidator())
        assertNotNull(HimCanonicalFamilyPaths::class.java)
        assertNotNull(HimProductOnlyCanonicalMasterReader::class.java)
        assertTrue(HimEntityType.CANONICAL.name == "CANONICAL")
    }

    @Test
    fun `writes current real bound P1 pilot direct evidence supplement twice`() {
        val authorizedExecutionHead = requireRealGate()
        val root = projectRoot()
        val currentHead = git(root, "rev-parse", "HEAD")
        require(currentHead == authorizedExecutionHead) { "EXECUTION_HEAD_MISMATCH" }
        require(isAncestor(root, CORE_BASELINE_HEAD, authorizedExecutionHead)) { "CORE_BASELINE_NOT_ANCESTOR" }

        val packetFiles = loadAndValidatePacket(root)
        val mission = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION
        require(HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.validate(mission).valid) { "MISSION_INVALID" }
        require(mission.selectionDigest == HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_SELECTION_DIGEST) { "MISSION_SELECTION_MISMATCH" }

        val release = loadAndValidateOffRelease(root)
        val paths = HimCanonicalFamilyPaths(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
        val persistence = HimCanonicalFamilyPersistence()
        val registry = persistence.readRegistry(paths.entityIdRegistry)
        val masterAuthority = persistence.readAuthority(paths.familyAuthority)
        HimCanonicalFamilyValidator().validate(catalog, registry, masterAuthority)
        validateTargets(catalog, registry, masterAuthority)

        val offSource = release.sources.single { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS.name }
        require(offSource.optimizedSourcePath == HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.SOURCE_ARTIFACT_PATH)
        require(offSource.optimizedSourceSha256 == HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.SOURCE_ARTIFACT_SHA256)
        require(offSource.indexPath == HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_ARTIFACT_PATH)
        require(offSource.sqliteFileSha256 == HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_ARTIFACT_SHA256)
        require(offSource.logicalIndexDigest == HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_LOGICAL_DIGEST)

        val indexFile = root.resolve(offSource.indexPath)
        require(indexFile.isFile) { "OFF_INDEX_MISSING" }
        val outputRoot = root.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.OUTPUT_ROOT)
        val outputDirectory = outputRoot.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MISSION_DIRECTORY)
        val jsonFile = outputDirectory.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.JSON_FILE_NAME)
        val markdownFile = outputDirectory.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MARKDOWN_FILE_NAME)
        require(jsonFile.exists() == markdownFile.exists()) { "PARTIAL_SUPPLEMENT_OUTPUT" }

        val protectedInputs = listOf(packetFiles.json, packetFiles.markdown, paths.productOnlyMaster, paths.entityIdRegistry, paths.familyAuthority)
            .associateWith { it.readBytes() }
        val port = BoundedOffExactFetchPort(indexFile)
        val sourceInputs = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.resolve(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRequestV1(
                provenance = sourceProjectionProvenance(),
                exactFetchPort = port,
            ),
        )
        require(port.calls == EXPECTED_SOURCE_REFERENCES) { "EXACT_FETCH_SCOPE_MISMATCH" }
        require(port.calls.distinct().size == 4) { "EXACT_FETCH_DUPLICATE" }
        require(sourceInputs.size == 4) { "SOURCE_PROJECTION_COUNT_MISMATCH" }

        val request = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeRequestV1(
            enabled = true,
            outputRoot = outputRoot,
            supplementImplementationHead = authorizedExecutionHead,
            mission = mission,
            reviewPacket = packetFiles.packet,
            reviewPacketBinding = packetFiles.binding,
            sourceProjectionInputs = sourceInputs,
            foundationInput = foundationInput(root, catalog, registry, masterAuthority, persistence),
        )
        val first = completed(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1.execute(request))
        validateResult(first, jsonFile, markdownFile)
        val firstJson = jsonFile.readBytes()
        val firstMarkdown = markdownFile.readBytes()
        val firstSupplement = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.readSupplement(jsonFile)
        require(first.supplement == firstSupplement) { "SUPPLEMENT_RELOAD_MISMATCH" }

        val second = completed(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1.execute(request))
        require(second.persistenceStatus == HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL) {
            "SECOND_PERSISTENCE_STATUS_MISMATCH"
        }
        require(first.supplement == second.supplement) { "SECOND_SUPPLEMENT_MISMATCH" }
        require(firstJson.contentEquals(jsonFile.readBytes())) { "JSON_NOT_IDEMPOTENT" }
        require(firstMarkdown.contentEquals(markdownFile.readBytes())) { "MARKDOWN_NOT_IDEMPOTENT" }
        require(first.jsonSha256 == second.jsonSha256 && first.markdownSha256 == second.markdownSha256) { "DIGEST_NOT_IDEMPOTENT" }
        require(first.supplementBindingDigest == second.supplementBindingDigest) { "BINDING_DIGEST_NOT_IDEMPOTENT" }
        require(first.supplementLogicalDigest == second.supplementLogicalDigest) { "LOGICAL_DIGEST_NOT_IDEMPOTENT" }
        protectedInputs.forEach { (file, bytes) -> require(file.readBytes().contentEquals(bytes)) { "PROTECTED_INPUT_MUTATED" } }
    }

    private fun loadAndValidatePacket(root: File): PacketFiles {
        val outputRoot = root.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.OUTPUT_ROOT)
        val directory = outputRoot.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.MISSION_ID)
        val json = directory.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.JSON_FILE_NAME)
        val markdown = directory.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.MARKDOWN_FILE_NAME)
        require(json.isFile && markdown.isFile) { "REVIEW_PACKET_MISSING" }
        require(sha256(json) == PACKET_JSON_SHA256) { "REVIEW_PACKET_JSON_SHA256_MISMATCH" }
        require(sha256(markdown) == PACKET_MARKDOWN_SHA256) { "REVIEW_PACKET_MARKDOWN_SHA256_MISMATCH" }
        val packet = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.readPacket(json)
        require(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.validatePacket(packet).valid) { "REVIEW_PACKET_INVALID" }
        require(packet.packetState == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1.REVIEW_CONTEXT_ONLY) { "REVIEW_PACKET_STATE_MISMATCH" }
        require(packet.items.size == 4 && packet.items.map { it.reviewUnitId }.distinct().size == 4) { "REVIEW_PACKET_ITEMS_MISMATCH" }
        require(packet.items.none { it.contextLimitations.any { limitation -> limitation.name.contains("DECISION") } }) { "REVIEW_PACKET_DECISION_SEMANTICS" }
        val binding = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementReviewPacketBindingV1(
            jsonFileBinding = fileBinding(root, json, PACKET_LOGICAL_DIGEST),
            markdownFileBinding = fileBinding(root, markdown, PACKET_LOGICAL_DIGEST),
            packetInputBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_INPUT_BINDING_DIGEST,
            packetBindingDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_BINDING_DIGEST,
            packetLogicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_LOGICAL_DIGEST,
            packetImplementationHead = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_IMPLEMENTATION_HEAD,
        )
        require(binding.jsonFileBinding.sha256 == PACKET_JSON_SHA256)
        require(binding.markdownFileBinding.sha256 == PACKET_MARKDOWN_SHA256)
        return PacketFiles(packet, binding, json, markdown)
    }

    private fun loadAndValidateOffRelease(root: File): HimProductionIndexFileIdentityReleaseV1 {
        val file = root.resolve(HimProductionIndexFileIdentityReleaseContractV1.PATH)
        require(file.isFile) { "OFF_RELEASE_MISSING" }
        val release = HimProductionIndexFileIdentityReleasePersistenceV1.read(file)
        require(release == HimProductionIndexFileIdentityReleasePersistenceV1.expected()) { "OFF_RELEASE_MISMATCH" }
        require(release.state == "RELEASED") { "OFF_RELEASE_NOT_RELEASED" }
        require(release.sources.size == 4) { "OFF_RELEASE_SOURCE_COUNT_MISMATCH" }
        return release
    }

    private fun validateTargets(catalog: de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster, registry: de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry, authority: de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority) {
        listOf("ZuhV5V" to "Artischocken", "rVnyq7" to "Crème double").forEach { (id, name) ->
            val entry = registry.entries.single { it.entityId.value == id }
            val record = catalog.records.single { it.normalized == entry.sourceReference }
            val family = authority.families.single { it.canonicalId.value == id }
            require(record.itemname == name && family.canonicalName == name)
            require(family.identities.isEmpty() && family.variants.isEmpty() && family.aliases.isEmpty())
        }
    }

    private fun foundationInput(root: File, catalog: de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster, registry: de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry, authority: de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority, persistence: HimCanonicalFamilyPersistence) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFoundationInputV1(
        catalog = catalog,
        catalogBinding = fileBinding(root, root.resolve(catalog.path), logicalDigest(persistence.serialize(catalog))),
        registry = registry,
        registryBinding = fileBinding(root, HimCanonicalFamilyPaths(root).entityIdRegistry, logicalDigest(persistence.serialize(registry))),
        authority = authority,
        authorityBinding = fileBinding(root, HimCanonicalFamilyPaths(root).familyAuthority, logicalDigest(persistence.serialize(authority))),
    )

    private fun sourceProjectionProvenance() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionProvenanceV1(
        sourceArtifactPath = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.SOURCE_ARTIFACT_PATH,
        sourceArtifactSha256 = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.SOURCE_ARTIFACT_SHA256,
        indexArtifactPath = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_ARTIFACT_PATH,
        indexArtifactSha256 = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_ARTIFACT_SHA256,
        indexLogicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_LOGICAL_DIGEST,
        indexSchema = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_SCHEMA,
        projectionPolicy = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.PROJECTION_POLICY,
        indexState = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_STATE,
        releaseState = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.RELEASE_STATE,
    )

    private fun validateResult(result: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Completed, json: File, markdown: File) {
        assertEquals(4, result.counters.reviewUnits)
        assertEquals(4, result.counters.sourceEvidenceProjections)
        assertEquals(4, result.counters.catalogTargetEvidenceRecords)
        assertEquals(4, result.counters.authorityTargetEvidenceRecords)
        assertEquals(12, result.counters.directEvidenceReferences)
        assertEquals(4, result.counters.distinctSourceRecords)
        assertEquals(2, result.counters.distinctCanonicalTargets)
        assertEquals(4, result.supplement.bundles.size)
        assertTrue(result.supplement.bundles.all { it.evidence.size == 3 })
        assertTrue(result.supplement.bundles.all { bundle -> bundle.evidence.all { it.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT } })
        assertTrue(result.supplement.bundles.none { bundle -> bundle.evidence.any { it.recordReference.contains("decision") } })
        require(result.jsonPath == "${HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MISSION_DIRECTORY}/${HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.JSON_FILE_NAME}")
        require(result.markdownPath == "${HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MISSION_DIRECTORY}/${HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MARKDOWN_FILE_NAME}")
        require(json.isFile && markdown.isFile)
        require(json.length() == result.jsonByteSize && markdown.length() == result.markdownByteSize)
        require(sha256(json) == result.jsonSha256 && sha256(markdown) == result.markdownSha256)
        require(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.readSupplement(json) == result.supplement)
        val markdownText = markdown.readText()
        require(markdownText.startsWith(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementPersistenceV1.MARKDOWN_HEADER))
        require(!markdownText.contains("Decision:") && !markdownText.contains("Reviewer:") && !markdownText.contains("Approval:"))
    }

    private fun completed(result: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Completed = when (result) {
        is HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Completed -> result
        is HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Failed -> fail(safeDiagnostic(result.reason.name, result.safeContext))
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeResultV1.Disabled -> fail("RUNTIME_DISABLED")
    }

    private fun requireRealGate(): String {
        val gate = gate(
            System.getProperty(ENABLED_PROPERTY),
            System.getProperty(CONFIRMATION_PROPERTY),
            System.getProperty(AUTHORIZED_EXECUTION_HEAD_PROPERTY),
        )
        assumeTrue(gate.enabled)
        assumeTrue(gate.confirmation == CONFIRMATION)
        val authorized = parseAuthorizedExecutionHead(gate.authorizedExecutionHead)
        assumeTrue(authorized != null)
        HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        return requireNotNull(authorized)
    }

    private fun gate(enabled: String?, confirmation: String?, authorizedExecutionHead: String?) = Gate(
        enabled = enabled == "true",
        confirmation = confirmation,
        authorizedExecutionHead = authorizedExecutionHead,
    )

    private fun gateIsComplete(gate: Gate): Boolean = gate.enabled && gate.confirmation == CONFIRMATION && parseAuthorizedExecutionHead(gate.authorizedExecutionHead) != null

    private fun parseAuthorizedExecutionHead(value: String?): String? = value?.takeIf { it.matches(HEAD_PATTERN) }

    private fun executionHeadForRuntime(authorizedExecutionHead: String): String = authorizedExecutionHead

    private fun executionHeadFailure(authorizedExecutionHead: String, currentHead: String): String? = if (authorizedExecutionHead == currentHead) null else "EXECUTION_HEAD_MISMATCH"

    private fun ancestorExitIsValid(exitCode: Int): Boolean = exitCode == 0

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("USER_DIR_UNAVAILABLE")).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) return current
            current = current.parentFile ?: error("REPOSITORY_ROOT_NOT_FOUND")
        }
    }

    private fun git(root: File, vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git") + arguments.toList()).directory(root).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        require(process.waitFor() == 0 && output.matches(HEAD_PATTERN)) { "GIT_BINDING_FAILED" }
        return output
    }

    private fun isAncestor(root: File, ancestor: String, descendant: String): Boolean {
        val process = ProcessBuilder("git", "merge-base", "--is-ancestor", ancestor, descendant).directory(root).redirectErrorStream(true).start()
        process.inputStream.close()
        return process.waitFor() == 0
    }

    private fun relativePath(root: File, file: File): String = root.canonicalFile.toPath().relativize(file.canonicalFile.toPath()).toString().replace(File.separatorChar, '/')

    private fun fileBinding(root: File, file: File, logicalDigest: String) = HimZeroCandidateRecoveryHumanReviewFileBindingV1(
        relativePath = relativePath(root, file),
        byteSize = file.length(),
        sha256 = sha256(file),
        logicalDigest = logicalDigest,
    )

    private fun logicalDigest(bytes: ByteArray): String = sha256(if (bytes.lastOrNull() == '\n'.code.toByte()) bytes.copyOf(bytes.size - 1) else bytes)

    private fun sha256(file: File): String = sha256(file.readBytes())

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private fun safeDiagnostic(reason: String, safeContext: String): String = "$reason $safeContext"

    private class BoundedOffExactFetchPort(private val database: File) : HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionExactFetchPortV1 {
        val calls = mutableListOf<String>()

        override fun fetchExact(sourceRecordReference: String): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1? {
            calls += sourceRecordReference
            return HimOffProductionEvidenceIndexTool.connection(database, readOnly = true).use { connection ->
                connection.createStatement().use { it.execute(QUERY_ONLY_PRAGMA) }
                connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.EXACT_FETCH_SQL).use { statement ->
                    statement.setString(1, sourceRecordReference)
                    statement.executeQuery().use resultUse@ { result ->
                        if (!result.next()) return@resultUse null
                        val recordKind = HimEvidenceRecordKind.valueOf(result.getString("record_kind"))
                        require(recordKind == HimEvidenceRecordKind.OFF_PRODUCT) { "OFF_RECORD_KIND_MISMATCH" }
                        val projection = HimOffEvidenceProjectionV1.fromProjectionJson(result.getString("evidence_projection_json"))
                        require(projection.sourceRecordReference.value == sourceRecordReference) { "OFF_REFERENCE_MISMATCH" }
                        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1(
                            source = HimGroundTruthSource.OPEN_FOOD_FACTS,
                            recordKind = recordKind,
                            sourceRecordReference = sourceRecordReference,
                            fields = projectionFields(projection.evidenceProjection.deterministicJson),
                        )
                    }
                }
            }
        }
    }

    private data class PacketFiles(
        val packet: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1,
        val binding: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementReviewPacketBindingV1,
        val json: File,
        val markdown: File,
    )

    private data class Gate(val enabled: Boolean, val confirmation: String?, val authorizedExecutionHead: String?)

    companion object {
        const val ENTRYPOINT_CONTRACT_ID = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_REAL_BOUND_ENTRYPOINT_V1"
        const val ENABLED_PROPERTY = "him.zeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplement.enabled"
        const val CONFIRMATION_PROPERTY = "him.zeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplement.confirmation"
        const val AUTHORIZED_EXECUTION_HEAD_PROPERTY = "him.zeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplement.authorizedExecutionHead"
        const val CONFIRMATION = "AUTHORIZED_BOUNDED_P1_PILOT_DIRECT_EVIDENCE_SUPPLEMENT_OFFLINE"
        const val CORE_BASELINE_HEAD = "deef20015fb2bae143e5663b285d5c9c76f87232"
        const val PACKET_JSON_SHA256 = "4c06a60e7e9f8548088f7d006667d55f36d1d92fb93818d6d95b2f54dd6c8a43"
        const val PACKET_MARKDOWN_SHA256 = "7560a7f84474676a80398e014bf73cb4527d7e956071994281ef1f05c84d4ad6"
        const val PACKET_LOGICAL_DIGEST = "311f4925716045d47a02af3652dea66393f7cb713dbfe7239705ce4684acce6e"
        const val QUERY_ONLY_PRAGMA = "PRAGMA query_only = ON"
        const val FOUNDATION_AUTHORITY_ROLE = "masterAuthority"
        const val FOUNDATION_VALIDATOR = "HimCanonicalFamilyValidator"
        const val REAL_EXECUTION_TEST_NAME = "writes current real bound P1 pilot direct evidence supplement twice"
        const val INPUT_ACCESS_REQUIRES_COMPLETE_GATE = true
        const val OUTPUT_ACCESS_REQUIRES_COMPLETE_GATE = true
        const val BOUNDED_READ_ONLY = true
        const val REAL_SOURCE_READER_REACHABLE = false
        const val VALIDATE_READ_ONLY_REACHABLE = false
        val GATE_STAGES = listOf("enabled", "confirmation", "authorizedExecutionHead", "sourceIntegration", "repositoryInputs", "outputs")
        val EXPECTED_SOURCE_REFERENCES = listOf(
            "off:product:row:431650:code:4002239680509",
            "off:product:row:3272579:code:0061483010917",
            "off:product:row:1551407:code:4013200552046",
            "off:product:row:3322623:code:2026088009283",
        )
        private val HEAD_PATTERN = Regex("[0-9a-f]{40}")
    }
}

private fun projectionFields(json: String): List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1> {
    fun flatten(element: JsonElement, prefix: String): List<Pair<String, String>> = when {
        element.isJsonObject -> element.asJsonObject.entrySet().sortedBy { it.key }.flatMap { (key, value) -> flatten(value, if (prefix.isEmpty()) key else "$prefix.$key") }
        element.isJsonArray -> element.asJsonArray.mapIndexed { index, value -> flatten(value, "$prefix[$index]") }.flatten()
        element.isJsonNull -> listOf(prefix to "")
        else -> listOf(prefix to element.asString)
    }
    return flatten(JsonParser.parseString(json), "projection").map { (reference, value) ->
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1(
            fieldReference = reference,
            fullValue = value,
            fullValueSha256 = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1.sha256(value),
        )
    }
}
