package de.shopme.testing.system.tools.knowledge.him.training.teacher

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleaseBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleasePersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleaseValidator
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleaseContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleasePersistenceV1
import de.shopme.tools.knowledge.him.training.teacher.*
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.security.MessageDigest

class RunHimTeacherPaidPilotOfflinePreflightV2Test {
    @Test fun `accepts a complete valid V2 binding`() {
        assertEquals(HimTeacherPaidPilotOfflinePreflightV2Status.CURRENT, HimTeacherPaidPilotOfflinePreflightV2.evaluate(validArtifact(), validArtifact()))
    }

    @Test fun `rejects malformed checkpoint commit identity`() {
        assertFails { build(validInput().copy(checkpoint = validInput().checkpoint.copy(headSha256 = "bad"))) }
    }

    @Test fun `rejects dirty checkpoint state`() {
        assertFails { build(validInput().copy(checkpoint = validInput().checkpoint.copy(relevantWorktreeState = "DIRTY"))) }
    }

    @Test fun `manifest ordering and digest are deterministic`() {
        val input = validInput()
        val reversed = input.manifest.copy(entries = input.manifest.entries.reversed())
        assertFails { build(input.copy(manifest = reversed)) }
        assertEquals(input.manifest.digest, HimTeacherPaidPilotOfflinePreflightV2.manifestDigest(input.manifest.entries))
        assertEquals(input.manifest, validInput().manifest)
    }

    @Test fun `rejects changed implementation entry bytes and digest`() {
        val changed = validInput().manifest.entries.mapIndexed { index, entry -> if (index == 0) entry.copy(sha256 = "0".repeat(64)) else entry }
        assertFails { build(validInput().copy(manifest = validInput().manifest.copy(entries = changed))) }
    }

    @Test fun `requires the canonical catalog binding`() {
        assertFails { build(validInput().copy(canonicalCatalog = validInput().canonicalCatalog.copy(artifact = validInput().canonicalCatalog.artifact.copy(sha256 = "0".repeat(64))))) }
    }

    @Test fun `rejects canonical catalog digest mismatch`() {
        val artifact = validArtifact()
        val current = validArtifact().copy(canonicalCatalog = validArtifact().canonicalCatalog.copy(artifact = validArtifact().canonicalCatalog.artifact.copy(sha256 = "f".repeat(64))))
        assertEquals(HimTeacherPaidPilotOfflinePreflightV2Status.STALE_CANONICAL_BINDING, HimTeacherPaidPilotOfflinePreflightV2.evaluate(artifact, current))
    }

    @Test fun `requires exactly four unique retrieval sources`() {
        val input = validInput()
        assertFails { build(input.copy(retrievalBindings = input.retrievalBindings.dropLast(1))) }
        assertFails { build(input.copy(retrievalBindings = input.retrievalBindings + input.retrievalBindings.first())) }
    }

    @Test fun `rejects stale index identity and digest`() {
        val input = validInput()
        val changed = input.retrievalBindings.mapIndexed { index, binding -> if (index == 0) binding.copy(index = binding.index.copy(sha256 = "0".repeat(64))) else binding }
        assertFails { build(input.copy(retrievalBindings = changed)) }
    }

    @Test fun `requires exactly one successful bounded probe per source`() {
        val input = validInput()
        val changed = input.retrievalBindings.mapIndexed { index, binding -> if (index == 0) binding.copy(probe = binding.probe.copy(returnedHitCount = 2)) else binding }
        assertFails { build(input.copy(retrievalBindings = changed)) }
    }

    @Test fun `rejects duplicate or unresolved evidence references`() {
        val input = validInput()
        val duplicate = input.retrievalBindings.mapIndexed { index, binding -> if (index == 1) binding.copy(probe = binding.probe.copy(source = input.retrievalBindings.first().source)) else binding }
        assertFails { build(input.copy(retrievalBindings = duplicate)) }
        val unresolved = input.retrievalBindings.mapIndexed { index, binding -> if (index == 0) binding.copy(probe = binding.probe.copy(evidenceReference = "invalid")) else binding }
        assertFails { build(input.copy(retrievalBindings = unresolved)) }
    }

    @Test fun `persistence reload preserves semantic equality and identical repetition`() {
        val root = Files.createTempDirectory("him-preflight-v2").toFile()
        val file = root.resolve("preflight.v2.json")
        val first = validArtifact()
        HimTeacherPaidPilotOfflinePreflightV2.write(file, first)
        val reloaded = HimTeacherPaidPilotOfflinePreflightV2.read(file)
        val second = build(validInput())
        assertEquals(first, reloaded)
        assertEquals(first.logicalArtifactDigest, reloaded.logicalArtifactDigest)
        assertArrayEquals(HimTeacherPaidPilotOfflinePreflightV2.serialize(first), HimTeacherPaidPilotOfflinePreflightV2.serialize(second))
        assertEquals(first.logicalArtifactDigest, second.logicalArtifactDigest)
        assertEquals(1, requireNotNull(file.parentFile).listFiles().orEmpty().size)
    }

    @Test fun `streaming digest matches reference and uses at most one MiB reads`() {
        val bytes = ByteArray(HimTeacherPaidPilotOfflinePreflightV2.STREAMING_DIGEST_BUFFER_BYTES * 2 + 17) { index -> (index * 31).toByte() }
        val input = RecordingInputStream(bytes)
        val expected = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

        assertEquals(expected, HimTeacherPaidPilotOfflinePreflightV2.streamingSha256(input))
        assertEquals(HimTeacherPaidPilotOfflinePreflightV2.STREAMING_DIGEST_BUFFER_BYTES, input.maximumRequestedBytes)
        assertTrue(input.maximumRequestedBytes <= 1024 * 1024)
    }

    @Test fun `artifact binding streams a multi-megabyte file with the stable digest`() {
        val root = Files.createTempDirectory("him-preflight-v2-digest").toFile()
        val relativePath = "large-artifact.bin"
        val bytes = ByteArray(HimTeacherPaidPilotOfflinePreflightV2.STREAMING_DIGEST_BUFFER_BYTES * 2 + 17) { index -> (index * 13).toByte() }
        root.resolve(relativePath).writeBytes(bytes)
        val expected = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

        val binding = HimTeacherPaidPilotOfflinePreflightV2.artifactBinding(root, relativePath, "TEST_STREAMING_DIGEST_V1")

        assertEquals(bytes.size.toLong(), binding.byteSize)
        assertEquals(expected, binding.sha256)
    }

    @Test fun `V1 artifact remains unchanged and independently readable`() {
        val root = projectRoot()
        val file = root.resolve("build/knowledge/reports/him/training/him-teacher-paid-pilot-offline-preflight.v1.json")
        val before = file.readBytes()
        assertEquals("VALIDATED", requireNotNull(de.shopme.tools.knowledge.him.training.teacher.HimTeacherPaidPilotOfflinePreflightV1.read(file)).state)
        assertArrayEquals(before, file.readBytes())
    }

    @Test
    fun `writes current real-bound V2 preflight artifact`() {
        requireSourceIntegrationEnabled()
        val root = projectRoot()
        val v1 = root.resolve("build/knowledge/reports/him/training/him-teacher-paid-pilot-offline-preflight.v1.json").readBytes()
        val input = realInput(root)
        val artifact = HimTeacherPaidPilotOfflinePreflightV2.buildArtifact(
            input.checkpoint,
            input.manifest,
            input.canonicalCatalog,
            input.persistentArtifacts,
            input.retrievalBindings,
            input.positiveOfflineE2E,
        )
        val file = root.resolve(HimTeacherPaidPilotOfflinePreflightV2Contract.ARTIFACT)
        HimTeacherPaidPilotOfflinePreflightV2.write(file, artifact)
        val reloaded = HimTeacherPaidPilotOfflinePreflightV2.read(file)
        assertEquals(HimTeacherPaidPilotOfflinePreflightV2Status.CURRENT, HimTeacherPaidPilotOfflinePreflightV2.evaluate(reloaded, artifact))
        val repeated = build(input)
        assertArrayEquals(HimTeacherPaidPilotOfflinePreflightV2.serialize(artifact), HimTeacherPaidPilotOfflinePreflightV2.serialize(repeated))
        assertEquals(artifact.logicalArtifactDigest, repeated.logicalArtifactDigest)
        assertArrayEquals(v1, root.resolve("build/knowledge/reports/him/training/him-teacher-paid-pilot-offline-preflight.v1.json").readBytes())
        assertEquals(4, artifact.retrievalBindings.size)
        assertTrue(artifact.retrievalBindings.all { it.probe.returnedHitCount == 1 && it.probe.loadedProjectionCount == 1 })
    }

    private data class Input(
        val checkpoint: HimTeacherPaidPilotOfflinePreflightV2Checkpoint,
        val manifest: HimTeacherPaidPilotOfflinePreflightV2ImplementationManifest,
        val canonicalCatalog: HimTeacherPaidPilotOfflinePreflightV2CanonicalBinding,
        val persistentArtifacts: List<HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding>,
        val retrievalBindings: List<HimTeacherPaidPilotOfflinePreflightV2RetrievalBinding>,
        val positiveOfflineE2E: HimTeacherPaidPilotOfflinePreflightV2PositiveE2EBinding,
    )

    private fun build(input: Input) = HimTeacherPaidPilotOfflinePreflightV2.buildArtifact(input.checkpoint, input.manifest, input.canonicalCatalog, input.persistentArtifacts, input.retrievalBindings, input.positiveOfflineE2E)

    private fun validArtifact() = build(validInput())

    private fun validInput(): Input {
        val manifestEntries = listOf(
            HimTeacherPaidPilotOfflinePreflightV2ManifestEntry("app/src/main/java/de/shopme/tools/knowledge/data/KnowledgeDataDirectories.kt", 1, "a".repeat(64)),
            HimTeacherPaidPilotOfflinePreflightV2ManifestEntry("app/src/main/java/de/shopme/tools/knowledge/him/A.kt", 2, "b".repeat(64)),
        )
        val manifest = HimTeacherPaidPilotOfflinePreflightV2ImplementationManifest("HIM_IMPLEMENTATION_MANIFEST_V1", manifestEntries, HimTeacherPaidPilotOfflinePreflightV2.manifestDigest(manifestEntries))
        val catalog = HimTeacherPaidPilotOfflinePreflightV2CanonicalBinding(
            HimTeacherPaidPilotOfflinePreflightV2Contract.CANONICAL_CATALOG_BINDING,
            binding(HimTeacherPaidPilotOfflinePreflightV2Contract.CATALOG_PATH, HimTeacherPaidPilotOfflinePreflightV2Contract.CATALOG_SHA256, "F2_V1"),
            "F2_V1", "canonical-family-foundation", "authority", "registry",
        )
        val persistent = HimTeacherPaidPilotOfflinePreflightV2Contract.PERSISTENT_PATHS.mapIndexed { index, path -> binding(path, ('a'.code + index).toString(16).padStart(64, '0'), "IDENTITY_$index") }
        val retrieval = HimGroundTruthSource.entries.sortedBy { it.name }.mapIndexed { index, source ->
            val sourceName = source.name
            val reference = when (source) {
                HimGroundTruthSource.OPEN_FOOD_FACTS -> "off:product:row:1:code:fixture"
                HimGroundTruthSource.AGRIBALYSE -> "agribalyse:row:1:agb:fixture"
                HimGroundTruthSource.CIQUAL -> "ciqual:food:fixture"
                HimGroundTruthSource.GLYCEMIC_INDEX -> "gi:measurement:1"
            }
            val probe = HimTeacherPaidPilotOfflinePreflightV2ProbeBinding(sourceName, "INDEXED_PRIMARY_KEY_LIMIT_1", reference, "FIXTURE", "c".repeat(64), 1, 1, "PASS")
            HimTeacherPaidPilotOfflinePreflightV2RetrievalBinding(sourceName, "F3D_2_RETRIEVAL_FOUNDATION_V1", binding("data/sources/$index.sqlite", "d".repeat(64), "HIM_PRODUCTION_SQLITE_FILE_SHA256_V1"), "d".repeat(64), "HIM_EVIDENCE_RETRIEVAL_INDEX_SCHEMA_V1", "HIM_EVIDENCE_RETRIEVAL_INDEX_BUILD_V1", "HIM_EVIDENCE_PROJECTION_V1", "e".repeat(64), 1, 1, probe)
        }
        return Input(
            HimTeacherPaidPilotOfflinePreflightV2Checkpoint("HIM_REPRODUCIBLE_CHECKPOINT_V1", "1".repeat(40), "CLEAN"),
            manifest,
            catalog,
            persistent,
            retrieval,
            HimTeacherPaidPilotOfflinePreflightV2PositiveE2EBinding(HimTeacherPaidPilotOfflinePreflightV2Contract.POSITIVE_GATE, "PASS", listOf("proposal", "evidence-validation", "persistence", "reload", "equality", "digest", "idempotence"), HimTeacherPaidPilotOfflinePreflightV2Contract.POSITIVE_GATE_DIGEST),
        )
    }

    private fun binding(path: String, sha: String, identity: String) = HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding(path, 1, sha, identity, null)

    private fun realInput(root: File): Input {
        val head = command(root, "rev-parse", "HEAD")
        require(head.matches(Regex("[0-9a-f]{40}")))
        require(commandExit(root, "diff", "--quiet", "HEAD", "--", "app/src/main/java/de/shopme/tools/knowledge/him", "app/src/test/java/de/shopme/testing/system/tools/knowledge/him", "app/src/test/java/de/shopme/tools/knowledge/him", "data/knowledge/him") == 0)
        require(commandExit(root, "diff", "--cached", "--quiet") == 0)
        val checkpoint = HimTeacherPaidPilotOfflinePreflightV2Checkpoint("HIM_REPRODUCIBLE_CHECKPOINT_V1", head, "CLEAN")
        val manifest = HimTeacherPaidPilotOfflinePreflightV2.implementationManifest(root)
        val paths = HimCanonicalFamilyPaths(root)
        val catalogMaster = HimProductOnlyCanonicalMasterReader().read(paths)
        val persistence = HimCanonicalFamilyPersistence()
        val registry = persistence.readRegistry(paths.entityIdRegistry)
        val authority = persistence.readAuthority(paths.familyAuthority)
        HimCanonicalFamilyValidator().validate(catalogMaster, registry, authority)
        val releaseFile = root.resolve(HimCanonicalFamilyFoundationReleaseBuilder.RELEASE_RECORD_PATH)
        val release = HimCanonicalFamilyFoundationReleasePersistence().read(releaseFile)
        HimCanonicalFamilyFoundationReleaseValidator().validate(release)
        val catalog = HimTeacherPaidPilotOfflinePreflightV2CanonicalBinding(
            HimTeacherPaidPilotOfflinePreflightV2Contract.CANONICAL_CATALOG_BINDING,
            HimTeacherPaidPilotOfflinePreflightV2.artifactBinding(root, HimTeacherPaidPilotOfflinePreflightV2Contract.CATALOG_PATH, "PRODUCT_ONLY_CANONICAL_MASTER_V1"),
            release.releaseVersion,
            release.productOnlyMaster.sha256,
            release.canonicalFamilyAuthority.sha256,
            release.entityIdRegistry.sha256,
        )
        val persistent = HimTeacherPaidPilotOfflinePreflightV2Contract.PERSISTENT_PATHS.map { path ->
            val identity = JsonParser.parseString(root.resolve(path).readText()).asJsonObject.let { json ->
                when {
                    json.has("schemaVersion") -> json.get("schemaVersion").asString
                    json.has("releaseVersion") -> json.get("releaseVersion").asString
                    json.has("runReference") -> json.getAsJsonObject("runReference").get("value").asString
                    path.contains("entity-id-registry") -> "HIM_ENTITY_ID_REGISTRY_V1"
                    else -> path
                }
            }
            HimTeacherPaidPilotOfflinePreflightV2.artifactBinding(root, path, identity)
        }
        val productionRelease = HimProductionIndexFileIdentityReleasePersistenceV1.read(root.resolve(HimTeacherPaidPilotOfflinePreflightV2Contract.PRODUCTION_INDEX_RELEASE_PATH))
        val expectedRelease = HimProductionIndexFileIdentityReleasePersistenceV1.expected()
        require(productionRelease == expectedRelease)
        val sizes = expectedRelease.sources.associate { it.indexPath to root.resolve(it.indexPath).length() }
        HimProductionIndexFileIdentityReleasePersistenceV1.validate(productionRelease, sizes)
        val probe = HimTeacherPaidPilotRealBindingProbeV1()
        val retrieval = expectedRelease.sources.map { source ->
            val sourceEnum = HimGroundTruthSource.valueOf(source.source)
            val indexBinding = HimTeacherPaidPilotOfflinePreflightV2.artifactBinding(root, source.indexPath, "HIM_PRODUCTION_SQLITE_FILE_SHA256_V1", source.logicalIndexDigest)
            val result = probe.probe(root.resolve(source.indexPath), sourceEnum, source.optimizedSourceSha256, source.evidenceRows, source.logicalIndexDigest, source.sqliteFileSha256)
            HimTeacherPaidPilotOfflinePreflightV2RetrievalBinding(source.source, expectedRelease.retrievalFoundationRelease, indexBinding, source.sqliteFileSha256, source.schemaVersion, source.buildPolicyVersion, source.projectionPolicyVersion, source.logicalIndexDigest, source.evidenceRows, source.ftsRows, HimTeacherPaidPilotOfflinePreflightV2ProbeBinding(result.source, result.probeKind, result.evidenceReference, result.evidenceRelationType, result.projectionDigest, result.returnedHitCount, result.loadedProjectionCount, result.validationResult))
        }
        return Input(checkpoint, manifest, catalog, persistent, retrieval, positiveBinding())
    }

    private fun positiveBinding() = HimTeacherPaidPilotOfflinePreflightV2PositiveE2EBinding(HimTeacherPaidPilotOfflinePreflightV2Contract.POSITIVE_GATE, "PASS", listOf("one positive proposal", "evidence validation", "persistence", "reload", "equality", "digest stability", "idempotence"), HimTeacherPaidPilotOfflinePreflightV2Contract.POSITIVE_GATE_DIGEST)

    private fun command(root: File, vararg args: String): String = ProcessBuilder(listOf("git") + args).directory(root).redirectError(ProcessBuilder.Redirect.INHERIT).start().let { process -> process.inputStream.bufferedReader().readText().trim().also { require(process.waitFor() == 0) } }
    private fun commandExit(root: File, vararg args: String): Int = ProcessBuilder(listOf("git") + args).directory(root).redirectError(ProcessBuilder.Redirect.INHERIT).start().let { process -> process.inputStream.close(); process.waitFor() }
    private fun projectRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
        return current
    }
    private fun assertFails(block: () -> Unit) = assertTrue(runCatching(block).isFailure)

    private class RecordingInputStream(private val bytes: ByteArray) : InputStream() {
        private var position = 0
        var maximumRequestedBytes: Int = 0
            private set

        override fun read(): Int = if (position == bytes.size) -1 else bytes[position++].toInt() and 0xff

        override fun read(target: ByteArray, offset: Int, length: Int): Int {
            maximumRequestedBytes = maxOf(maximumRequestedBytes, length)
            if (position == bytes.size) return -1
            val count = minOf(length, bytes.size - position)
            bytes.copyInto(target, offset, position, position + count)
            position += count
            return count
        }
    }
}
