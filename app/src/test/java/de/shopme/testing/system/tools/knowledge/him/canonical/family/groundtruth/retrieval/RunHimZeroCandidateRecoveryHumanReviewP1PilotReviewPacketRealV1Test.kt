package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusReportV1
import java.io.File
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Assume.assumeTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRealV1Test {

    @Test
    fun `gate is disabled by default`() {
        val gate = gate(
            System.getProperty(ENABLED_PROPERTY),
            System.getProperty(CONFIRMATION_PROPERTY),
        )
        assertFalse(gate.enabled)
        assertFalse(gate.confirmation == CONFIRMATION)
    }

    @Test
    fun `wrong confirmation is rejected before input access`() {
        val gate = gate("true", "WRONG_CONFIRMATION")
        assertTrue(gate.enabled)
        assertFalse(gate.confirmation == CONFIRMATION)
    }

    @Test
    fun `enabled without source integration is not operational`() {
        val gate = gate("true", CONFIRMATION)
        assertTrue(gate.enabled && gate.confirmation == CONFIRMATION)
        assertTrue(SOURCE_INTEGRATION_PROPERTY.isNotBlank())
        assertFalse(gateIsCompleteWithoutSourceIntegration(gate))
    }

    @Test
    fun `property names and confirmation are frozen`() {
        assertEquals(
            "him.zeroCandidateRecoveryHumanReviewP1PilotReviewPacket.enabled",
            ENABLED_PROPERTY,
        )
        assertEquals(
            "him.zeroCandidateRecoveryHumanReviewP1PilotReviewPacket.confirmation",
            CONFIRMATION_PROPERTY,
        )
        assertEquals(
            "AUTHORIZED_P1_PILOT_REVIEW_PACKET_CONTEXT_ONLY_OFFLINE",
            CONFIRMATION,
        )
    }

    @Test
    fun `expected implementation head is frozen`() {
        assertEquals("b44d28e4750e6d5d47210d91eb08c03ec033d3d2", EXPECTED_IMPLEMENTATION_HEAD)
        assertTrue(EXPECTED_IMPLEMENTATION_HEAD.matches(HEAD))
    }

    @Test
    fun `output root and packet file names are frozen`() {
        assertEquals(
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/p1-pilot-review-packet/v1",
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.OUTPUT_ROOT,
        )
        assertEquals("review-packet.v1.json", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.JSON_FILE_NAME)
        assertEquals("review-packet.v1.md", HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.MARKDOWN_FILE_NAME)
    }

    @Test
    fun `mission id and selection digest are frozen`() {
        assertEquals(
            "p1-artischocken-herzen-brie-double-creme-v1",
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.MISSION_ID,
        )
        assertEquals(
            "e5b639be1e04b004a8de2594bed4002c854727d15eb66420a42c4293f0f3dbcb",
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_SELECTION_DIGEST,
        )
    }

    @Test
    fun `review unit selection contains exactly four frozen ids`() {
        assertEquals(
            EXPECTED_REVIEW_UNIT_IDS,
            HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION.entries.map { it.reviewUnitId },
        )
        assertEquals(4, EXPECTED_REVIEW_UNIT_IDS.distinct().size)
    }

    @Test
    fun `entrypoint has no decision reviewer or gold component`() {
        val forbidden = listOf("Decision", "Reviewer", "Gold", "Recommendation", "Supervision")
        val types = listOf(
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1::class.java,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1::class.java,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketV1::class.java,
        )
        val names = types.flatMap { type ->
            type.declaredFields.map { it.name } +
                type.declaredMethods.map { it.name } +
                type.declaredFields.map { it.type.simpleName }
        }
        assertTrue(names.none { name -> forbidden.any { token -> name.contains(token, ignoreCase = true) } })
    }

    @Test
    fun `entrypoint has no store search fetch scan sqlite network openai or inference dependency`() {
        val forbidden = listOf("Store", "Search", "Fetch", "Scan", "SQLite", "Network", "OpenAI", "Inference", "Provider")
        val types = listOf(
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1::class.java,
            HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1::class.java,
        )
        val names = types.flatMap { type ->
            type.declaredFields.map { it.type.simpleName } +
                type.declaredMethods.flatMap { method ->
                    listOf(method.name, method.returnType.simpleName) + method.parameterTypes.map { it.simpleName }
                } +
                type.declaredConstructors.flatMap { constructor -> constructor.parameterTypes.map { it.simpleName } }
        }
        assertTrue(names.none { name -> forbidden.any { token -> name.contains(token, ignoreCase = true) } })
    }

    @Test
    fun `runtime request remains the committed field model`() {
        val fields = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1::class.java
            .declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
        assertEquals(
            setOf("enabled", "corpus", "inputBinding", "catalog", "registry", "authority", "packetImplementationHead", "packetOutputRoot"),
            fields.map { it.name }.toSet(),
        )
        assertTrue(fields.none { it.name.contains("port", ignoreCase = true) })
    }

    @Test
    fun `safe diagnostics expose only typed reason and safe context`() {
        val message = safeDiagnostic("INVALID_INPUT_BINDING", "input-binding")
        assertEquals("INVALID_INPUT_BINDING input-binding", message)
        assertFalse(message.contains("/"))
        assertFalse(message.contains("Exception"))
        assertFalse(message.contains("Throwable"))
    }

    @Test
    fun `operational test is skipped without both opt ins`() {
        val gate = gate(
            System.getProperty(ENABLED_PROPERTY),
            System.getProperty(CONFIRMATION_PROPERTY),
        )
        assumeTrue(!gate.enabled || gate.confirmation != CONFIRMATION)
    }

    @Test
    fun `normal offline execution does not create packet artifacts`() {
        val gate = gate(null, null)
        assertFalse(gateIsCompleteWithoutSourceIntegration(gate))
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.OUTPUT_ROOT.startsWith("build/"))
    }

    @Test
    fun `writes current real bound P1 pilot review packet twice without review decisions`() {
        requireRealGate()
        val root = projectRoot()
        val currentHead = git(root, "rev-parse", "HEAD")
        require(currentHead == EXPECTED_IMPLEMENTATION_HEAD) { "PILOT_PACKET_HEAD_MISMATCH" }

        val corpusFile = root.resolve(CORPUS_JSON_PATH)
        require(corpusFile.isFile) { "RECOVERY_REVIEW_CORPUS_MISSING" }
        require(corpusFile.length() == CORPUS_BYTES) { "RECOVERY_REVIEW_CORPUS_SIZE_MISMATCH" }
        require(HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(corpusFile) == CORPUS_SHA256) {
            "RECOVERY_REVIEW_CORPUS_SHA256_MISMATCH"
        }
        val corpus = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.readReport(corpusFile)
        corpus.validate()
        require(corpus.logicalDigest == CORPUS_LOGICAL_DIGEST) { "RECOVERY_REVIEW_CORPUS_LOGICAL_DIGEST_MISMATCH" }
        require(corpus.inputBinding.bindingDigest == CORPUS_BINDING_DIGEST) { "RECOVERY_REVIEW_CORPUS_BINDING_DIGEST_MISMATCH" }

        val mission = HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_MISSION
        require(HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.validate(mission).valid) { "PILOT_MISSION_INVALID" }
        mission.entries.forEach { missionEntry ->
            require(corpus.entries.count { it.stableEntryId == missionEntry.stableEntryId } == 1) {
                "PILOT_MISSION_ENTRY_MISSING"
            }
            require(corpus.entries.single { it.stableEntryId == missionEntry.stableEntryId }.auditLinkedCanonicalTargets.single().canonicalEntityId == missionEntry.canonicalEntityId) {
                "PILOT_MISSION_TARGET_MISMATCH"
            }
        }

        val paths = HimCanonicalFamilyPaths(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
        val persistence = HimCanonicalFamilyPersistence()
        val registry = persistence.readRegistry(paths.entityIdRegistry)
        val masterAuthority = persistence.readAuthority(paths.familyAuthority)
        HimCanonicalFamilyValidator().validate(catalog, registry, masterAuthority)
        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val authority = persistence.readAuthority(active.authorityFile)
        require(authority.sourceCatalog.path == catalog.path) { "AUTHORITY_SOURCE_CATALOG_MISMATCH" }
        require(authority.sourceCatalog.contentSha256 == catalog.contentSha256) { "AUTHORITY_SOURCE_CATALOG_MISMATCH" }
        require(authority.sourceCatalog.recordCount == catalog.records.size) { "AUTHORITY_SOURCE_CATALOG_MISMATCH" }

        val protectedInputs = listOf(
            corpusFile,
            paths.productOnlyMaster,
            paths.entityIdRegistry,
            paths.familyAuthority,
            active.authorityFile,
        ).distinctBy { it.canonicalFile }
            .associateWith { it.readBytes() }
        val inputBinding = inputBinding(root, corpusFile, corpus, catalog, authority, active.authorityFile, currentHead, persistence)
        require(inputBinding.validate().valid) { "PILOT_PACKET_INPUT_BINDING_INVALID" }

        val outputRoot = root.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.OUTPUT_ROOT)
        val outputDirectory = outputRoot.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.MISSION_ID)
        val jsonFile = outputDirectory.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.JSON_FILE_NAME)
        val markdownFile = outputDirectory.resolve(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.MARKDOWN_FILE_NAME)
        require(jsonFile.exists() == markdownFile.exists()) { "PILOT_PACKET_PARTIAL_OUTPUT" }
        val previousJson = if (jsonFile.isFile) jsonFile.readBytes() else null
        val previousMarkdown = if (markdownFile.isFile) markdownFile.readBytes() else null

        val request = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeRequestV1(
            enabled = true,
            corpus = corpus,
            inputBinding = inputBinding,
            catalog = catalog,
            registry = registry,
            authority = authority,
            packetImplementationHead = EXPECTED_IMPLEMENTATION_HEAD,
            packetOutputRoot = outputRoot,
        )
        val first = completed(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1.execute(request))
        validateRealResult(first, jsonFile, markdownFile)
        val firstJson = jsonFile.readBytes()
        val firstMarkdown = markdownFile.readBytes()
        require(previousJson == null || previousJson.contentEquals(firstJson)) { "PILOT_PACKET_EXISTING_JSON_CHANGED" }
        require(previousMarkdown == null || previousMarkdown.contentEquals(firstMarkdown)) { "PILOT_PACKET_EXISTING_MARKDOWN_CHANGED" }
        val firstReload = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.readPacket(jsonFile)
        require(first.packet == firstReload) { "PILOT_PACKET_RELOAD_MISMATCH" }

        val second = completed(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1.execute(request))
        require(second.persistenceStatus == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL) {
            "PILOT_PACKET_SECOND_STATUS_MISMATCH"
        }
        validateRealResult(second, jsonFile, markdownFile)
        val secondReload = HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.readPacket(jsonFile)
        require(first.packet == second.packet && first.packet == secondReload) { "PILOT_PACKET_SECOND_RELOAD_MISMATCH" }
        require(firstJson.contentEquals(jsonFile.readBytes())) { "PILOT_PACKET_JSON_NOT_IDEMPOTENT" }
        require(firstMarkdown.contentEquals(markdownFile.readBytes())) { "PILOT_PACKET_MARKDOWN_NOT_IDEMPOTENT" }
        require(first.jsonSha256 == second.jsonSha256 && first.markdownSha256 == second.markdownSha256) { "PILOT_PACKET_DIGEST_NOT_IDEMPOTENT" }
        require(first.counters == second.counters) { "PILOT_PACKET_COUNTERS_NOT_IDEMPOTENT" }
        protectedInputs.forEach { (file, bytes) -> require(file.readBytes().contentEquals(bytes)) { "PILOT_PACKET_INPUT_MUTATED" } }
    }

    private fun validateRealResult(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed,
        jsonFile: File,
        markdownFile: File,
    ) {
        require(result.runtimeContractId == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1.CONTRACT_ID)
        require(result.runtimeVersion == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeV1.VERSION)
        require(result.missionId == HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.MISSION_ID)
        require(result.persistenceStatus == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1.CREATED ||
            result.persistenceStatus == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceStatusV1.ALREADY_PRESENT_IDENTICAL)
        require(result.packet.packetState == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketStateV1.REVIEW_CONTEXT_ONLY)
        require(result.packet.items.size == 4)
        require(result.packet.counters.packetItems == 4)
        require(result.packet.counters.uniqueStableEntries == 4)
        require(result.packet.counters.uniqueReviewUnits == 4)
        require(result.packet.counters.distinctCanonicalTargets == 2)
        require(result.packet.counters.materializedCorpusOnlyItems == 4)
        require(result.packet.counters.sourceProjectionIncludedItems == 0)
        require(result.packet.counters.uniqueReviewUnits == result.packet.items.map { it.reviewUnitId }.distinct().size)
        require(result.packet.items.map { it.reviewUnitId } == EXPECTED_REVIEW_UNIT_IDS)
        require(result.packet.items.map { it.canonicalEntityId }.toSet() == setOf("ZuhV5V", "rVnyq7"))
        require(result.packet.items.all { it.evidenceScope.coverage == HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketEvidenceCoverageV1.MATERIALIZED_CORPUS_FIELDS_ONLY })
        require(result.packet.items.all { HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.FULL_SOURCE_RECORD_NOT_INCLUDED in it.contextLimitations })
        require(result.packet.items.all { HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContextLimitationV1.SOURCE_PROJECTION_NOT_INCLUDED in it.contextLimitations })
        require(result.packet.items.none { item -> item.contextLimitations.any { it.name.contains("DECISION") || it.name.contains("GOLD") } })
        require(result.packet.corpusLogicalDigest == CORPUS_LOGICAL_DIGEST)
        require(result.packet.corpusBindingDigest == CORPUS_BINDING_DIGEST)
        require(result.packet.packetImplementationHead == EXPECTED_IMPLEMENTATION_HEAD)
        require(jsonFile.isFile && markdownFile.isFile)
        require(jsonFile.length() == result.jsonByteSize)
        require(markdownFile.length() == result.markdownByteSize)
        require(HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketPersistenceV1.readPacket(jsonFile) == result.packet)
        val markdown = markdownFile.readText()
        require(markdown.startsWith(
            "# HIM Zero-Candidate Recovery Human Review – P1 Pilot Review Packet V1\n\n" +
                "> REVIEW CONTEXT ONLY\n> NO DECISION RECORDED\n> NO GOLD, TRAINING OR AUTHORITY EFFECT",
        ))
        require(!markdown.contains("Reviewer:") && !markdown.contains("Decision:") && !markdown.contains("Confidence:"))
    }

    private fun inputBinding(
        root: File,
        corpusFile: File,
        corpus: HimZeroCandidateRecoveryReviewCorpusReportV1,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
        authorityFile: File,
        currentHead: String,
        persistence: HimCanonicalFamilyPersistence,
    ): HimZeroCandidateRecoveryHumanReviewInputBindingV1 {
        val unsigned = HimZeroCandidateRecoveryHumanReviewInputBindingV1(
            corpusFileBinding = fileBinding(root, corpusFile, corpus.logicalDigest),
            corpusLogicalDigest = corpus.logicalDigest,
            corpusReportDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(corpusFile),
            corpusBindingDigest = corpus.inputBinding.bindingDigest,
            existingCorpusInputBinding = corpus.inputBinding,
            registryBinding = fileBinding(
                root,
                HimCanonicalFamilyPaths(root).entityIdRegistry,
                logicalDigest(persistence.serialize(persistence.readRegistry(HimCanonicalFamilyPaths(root).entityIdRegistry))),
            ),
            contractId = HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
            contractVersion = HimZeroCandidateRecoveryHumanReviewContractV1.VERSION,
            implementationHead = currentHead,
            bindingDigest = "",
        )
        require(unsigned.existingCorpusInputBinding.canonicalCatalog.relativePath == catalog.path) { "CORPUS_CATALOG_BINDING_MISMATCH" }
        require(unsigned.existingCorpusInputBinding.canonicalAuthority.relativePath == relativePath(root, authorityFile)) { "CORPUS_AUTHORITY_BINDING_MISMATCH" }
        require(unsigned.existingCorpusInputBinding.canonicalAuthority.sha256 == HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(authorityFile)) { "CORPUS_AUTHORITY_BINDING_MISMATCH" }
        require(authority.sourceCatalog.path == catalog.path) { "AUTHORITY_SOURCE_CATALOG_MISMATCH" }
        return unsigned.copy(bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(unsigned))
    }

    private fun fileBinding(root: File, file: File, logicalDigest: String) =
        HimZeroCandidateRecoveryHumanReviewFileBindingV1(
            relativePath = relativePath(root, file),
            byteSize = file.length(),
            sha256 = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(file),
            logicalDigest = logicalDigest,
        )

    private fun logicalDigest(serialized: ByteArray): String {
        require(serialized.isNotEmpty() && serialized.last() == '\n'.code.toByte())
        return HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(
            serialized.copyOf(serialized.size - 1).toString(Charsets.UTF_8),
        )
    }

    private fun completed(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed = when (result) {
        is HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Completed -> result
        is HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Failed -> fail(safeDiagnostic(result.reason.name, result.safeContext))
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketRuntimeResultV1.Disabled -> fail("RUNTIME_DISABLED")
    }

    private fun safeDiagnostic(reason: String, safeContext: String): String = "$reason $safeContext"

    private fun requireRealGate() {
        assumeTrue(System.getProperty(ENABLED_PROPERTY) == "true")
        assumeTrue(System.getProperty(CONFIRMATION_PROPERTY) == CONFIRMATION)
        HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
    }

    private fun gate(enabled: String?, confirmation: String?) = Gate(
        enabled = enabled == "true",
        confirmation = confirmation,
    )

    private fun gateIsCompleteWithoutSourceIntegration(gate: Gate): Boolean =
        gate.enabled && gate.confirmation == CONFIRMATION && false

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
        require(process.waitFor() == 0 && output.matches(HEAD)) { "GIT_BINDING_FAILED" }
        return output
    }

    private data class Gate(val enabled: Boolean, val confirmation: String?)

    companion object {
        const val ENABLED_PROPERTY = "him.zeroCandidateRecoveryHumanReviewP1PilotReviewPacket.enabled"
        const val CONFIRMATION_PROPERTY = "him.zeroCandidateRecoveryHumanReviewP1PilotReviewPacket.confirmation"
        const val CONFIRMATION = "AUTHORIZED_P1_PILOT_REVIEW_PACKET_CONTEXT_ONLY_OFFLINE"
        const val SOURCE_INTEGRATION_PROPERTY = "him.sourceIntegration.enabled"
        const val EXPECTED_IMPLEMENTATION_HEAD = "b44d28e4750e6d5d47210d91eb08c03ec033d3d2"
        const val CORPUS_JSON_PATH =
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-review-corpus/v1/review-corpus.v1.json"
        const val CORPUS_BYTES = 2359985L
        const val CORPUS_SHA256 = "4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016"
        const val CORPUS_LOGICAL_DIGEST = "3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02"
        const val CORPUS_BINDING_DIGEST = "b1691dbf87eed143d23ea44275ec5d96bf9483e4675469af5d282d3858288e81"
        private val HEAD = Regex("[0-9a-f]{40}")
        private val EXPECTED_REVIEW_UNIT_IDS = listOf(
            "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e",
            "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7",
            "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70",
            "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50",
        )
    }
}
