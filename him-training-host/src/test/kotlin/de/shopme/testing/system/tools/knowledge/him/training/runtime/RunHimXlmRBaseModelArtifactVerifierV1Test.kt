package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.runtime.HimXlmRBaseModelArtifactManifestV1
import de.shopme.tools.knowledge.him.training.runtime.HimXlmRBaseModelArtifactVerifierV1
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimXlmRBaseModelArtifactVerifierV1Test {
    @Test
    fun frozenSelectionAndArtifactIdentityAreExact() {
        assertEquals("HUGGING_FACE", HimXlmRBaseModelArtifactManifestV1.PROVIDER)
        assertEquals("FacebookAI/xlm-roberta-base", HimXlmRBaseModelArtifactManifestV1.REPOSITORY_ID)
        assertEquals("XLM-R", HimXlmRBaseModelArtifactManifestV1.MODEL_FAMILY)
        assertEquals("base", HimXlmRBaseModelArtifactManifestV1.MODEL_VARIANT)
        assertEquals(
            "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089",
            HimXlmRBaseModelArtifactManifestV1.UPSTREAM_REVISION,
        )
        assertEquals(5, HimXlmRBaseModelArtifactManifestV1.EXPECTED_ARTIFACT_COUNT)
        assertEquals("SAFETENSORS", HimXlmRBaseModelArtifactManifestV1.MODEL_WEIGHT_FORMAT)
        assertEquals(
            setOf("MODEL_CONFIG", "MODEL_WEIGHTS", "TOKENIZER_MODEL", "TOKENIZER_DEFINITION", "TOKENIZER_CONFIG"),
            HimXlmRBaseModelArtifactManifestV1.FROZEN_MANIFEST.artifacts.map { it.role.name }.toSet(),
        )
    }

    @Test
    fun frozenArtifactBytesAndRolesAreBound() {
        val artifacts = HimXlmRBaseModelArtifactManifestV1.FROZEN_MANIFEST.artifacts
        assertEquals(
            setOf("config.json", "model.safetensors", "sentencepiece.bpe.model", "tokenizer.json", "tokenizer_config.json"),
            artifacts.map { it.filename }.toSet(),
        )
        assertEquals(
            1_115_567_652L,
            artifacts.single {
                it.role == HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_WEIGHTS
            }.byteSize,
        )
        assertTrue(artifacts.all { it.byteSize > 0L })
        assertTrue(artifacts.all { it.repositoryRelativePath.startsWith("training/him/") })
        assertTrue(artifacts.none { it.repositoryRelativePath.startsWith("/") || it.repositoryRelativePath.contains("..") })
        assertTrue(artifacts.none { it.repositoryRelativePath.contains("/Users/") })
        assertFalse(artifacts.any { it.filename == "pytorch_model.bin" })
    }

    @Test
    fun manifestIsDeterministicAndOrderIndependent() {
        val manifest = HimXlmRBaseModelArtifactManifestV1.FROZEN_MANIFEST
        val rebuilt = HimXlmRBaseModelArtifactManifestV1.create(manifest.artifacts.reversed())

        assertEquals(manifest, rebuilt)
        assertEquals(manifest.logicalDigest, rebuilt.logicalDigest)
        assertEquals(manifest.manifestReference, rebuilt.manifestReference)
        assertEquals(manifest.canonicalIdentity(), rebuilt.canonicalIdentity())
    }

    @Test
    fun noMachinePathOrRemoteLookupIsPartOfManifestAuthority() {
        val manifest = HimXlmRBaseModelArtifactManifestV1.FROZEN_MANIFEST
        assertFalse(manifest.canonicalIdentity().contains("/Users/"))
        assertFalse(manifest.canonicalIdentity().contains("\\"))
        assertEquals("NO", HimXlmRBaseModelArtifactManifestV1.REMOTE_MODEL_REFERENCE_IS_RUNTIME_AUTHORITY)
        assertEquals("YES", HimXlmRBaseModelArtifactManifestV1.LOCAL_DIGEST_BOUND_ARTIFACT_SET_IS_RUNTIME_AUTHORITY)
        assertEquals("0", HimXlmRBaseModelArtifactManifestV1.AUTOMATIC_MODEL_DOWNLOAD)
        assertEquals("0", HimXlmRBaseModelArtifactManifestV1.AUTOMATIC_MODEL_REFRESH)
    }

    @Test
    fun hermeticFixturePassesWithExactLocalIdentity() = withFixture { root, manifest ->
        val result = HimXlmRBaseModelArtifactVerifierV1.verify(
            HimXlmRBaseModelArtifactVerifierV1.Request(root, manifest),
        )

        assertEquals(manifest, completed(result))
        assertEquals(HimXlmRBaseModelArtifactVerifierV1.CONTRACT_ID, completed(result).contractId)
        assertEquals(HimXlmRBaseModelArtifactVerifierV1.VERSION, completed(result).version)
        assertEquals(HimXlmRBaseModelArtifactVerifierV1.STATE, completed(result).state)
    }

    @Test
    fun missingArtifactFailsClosed() = withFixture { root, manifest ->
        Files.delete(
            root.resolve(
                manifest.artifacts.first {
                    it.role == HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_WEIGHTS
                }.filenameAtRoot(),
            ),
        )
        assertFailure(root, manifest, HimXlmRBaseModelArtifactVerifierV1.FailureReason.ARTIFACT_NOT_FOUND)
    }

    @Test
    fun unexpectedArtifactFailsClosed() = withFixture { root, manifest ->
        Files.write(
            root.resolve(HimXlmRBaseModelArtifactManifestV1.LOCAL_ARTIFACT_REVISION_ROOT).resolve("pytorch_model.bin"),
            byteArrayOf(1),
        )
        assertFailure(root, manifest, HimXlmRBaseModelArtifactVerifierV1.FailureReason.UNEXPECTED_ARTIFACT)
    }

    @Test
    fun wrongDigestAndWrongSizeFailClosed() = withFixture { root, manifest ->
        val wrongDigest = manifestWithReplacement(manifest, HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_WEIGHTS) {
            it.copy(sha256 = HimSha256("0".repeat(64)))
        }
        assertFailure(root, wrongDigest, HimXlmRBaseModelArtifactVerifierV1.FailureReason.ARTIFACT_DIGEST_MISMATCH)

        val wrongSize = manifestWithReplacement(manifest, HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_WEIGHTS) {
            it.copy(byteSize = it.byteSize + 1L)
        }
        assertFailure(root, wrongSize, HimXlmRBaseModelArtifactVerifierV1.FailureReason.ARTIFACT_SIZE_MISMATCH)
    }

    @Test
    fun symlinkArtifactFailsClosed() = withFixture { root, manifest ->
        val artifactRoot = root.resolve(HimXlmRBaseModelArtifactManifestV1.LOCAL_ARTIFACT_REVISION_ROOT)
        val target = Files.createTempFile("him-xlm-r-target-", ".bin")
        try {
            Files.delete(artifactRoot.resolve("config.json"))
            Files.createSymbolicLink(artifactRoot.resolve("config.json"), target)
            assertFailure(root, manifest, HimXlmRBaseModelArtifactVerifierV1.FailureReason.ARTIFACT_SYMLINK_REJECTED)
        } finally {
            Files.deleteIfExists(target)
        }
    }

    @Test
    fun repositoryRootSymlinkFailsClosed() {
        val realRoot = Files.createTempDirectory("him-xlm-r-real-root-")
        val link = Files.createTempDirectory("him-xlm-r-link-parent-").resolve("repo")
        try {
            Files.createSymbolicLink(link, realRoot)
            val result = HimXlmRBaseModelArtifactVerifierV1.verify(link)
            assertEquals(
                HimXlmRBaseModelArtifactVerifierV1.FailureReason.INVALID_REPOSITORY_ROOT,
                failed(result).reason,
            )
        } finally {
            Files.deleteIfExists(link)
            Files.deleteIfExists(realRoot)
            Files.deleteIfExists(link.parent)
        }
    }

    @Test
    fun absoluteAndTraversalArtifactRootPathsFailClosed() = withFixture { root, manifest ->
        for (path in listOf("../escape", "/tmp/self-authorized")) {
            val result = HimXlmRBaseModelArtifactVerifierV1.verify(
                HimXlmRBaseModelArtifactVerifierV1.Request(root, manifest, path),
            )
            assertEquals(
                HimXlmRBaseModelArtifactVerifierV1.FailureReason.INVALID_ARTIFACT_ROOT_PATH,
                failed(result).reason,
            )
        }
    }

    @Test
    fun nonSafetensorsPointerFailsClosed() = withFixture { root, manifest ->
        val artifactRoot = root.resolve(HimXlmRBaseModelArtifactManifestV1.LOCAL_ARTIFACT_REVISION_ROOT)
        val pointer = "version https://git-lfs.github.com/spec/v1\noid sha256:${"0".repeat(64)}\nsize 12\n".toByteArray()
        Files.write(artifactRoot.resolve("model.safetensors"), pointer)
        val pointerManifest = manifestWithReplacement(manifest, HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_WEIGHTS) {
            it.copy(byteSize = pointer.size.toLong(), sha256 = digest(pointer))
        }
        assertFailure(root, pointerManifest, HimXlmRBaseModelArtifactVerifierV1.FailureReason.SAFETENSORS_HEADER_INVALID)
    }

    @Test
    fun malformedManifestInputsAreRejectedBeforeVerification() {
        assertThrows(IllegalArgumentException::class.java) {
            HimXlmRBaseModelArtifactManifestV1.Artifact(
                role = HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_CONFIG,
                repositoryRelativePath = "../escape/config.json",
                filename = "config.json",
                byteSize = 1L,
                sha256 = HimSha256("0".repeat(64)),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            HimXlmRBaseModelArtifactManifestV1.Artifact(
                role = HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_CONFIG,
                repositoryRelativePath = "training/him/config.json",
                filename = "config.json",
                byteSize = 0L,
                sha256 = HimSha256("0".repeat(64)),
            )
        }
    }

    @Test
    fun materializedPinnedSetPassesOfflineAndRepeatsIdentically() {
        val root = findRepositoryRoot()
        val first = HimXlmRBaseModelArtifactVerifierV1.verify(root)
        val second = HimXlmRBaseModelArtifactVerifierV1.verify(root)

        assertEquals(HimXlmRBaseModelArtifactManifestV1.FROZEN_MANIFEST, completed(first))
        assertEquals(completed(first), completed(second))
        assertEquals(completed(first).logicalDigest, completed(second).logicalDigest)
        assertEquals(completed(first).manifestReference, completed(second).manifestReference)
    }

    @Test
    fun materializedConfigAndTokenizerFactsAreBoundWithoutExecution() {
        val root = findRepositoryRoot().resolve(HimXlmRBaseModelArtifactManifestV1.LOCAL_ARTIFACT_REVISION_ROOT)
        val config = Files.readString(root.resolve("config.json"))
        val tokenizerConfig = Files.readString(root.resolve("tokenizer_config.json"))
        val tokenizer = Files.readString(root.resolve("tokenizer.json"))

        assertTrue(config.contains("\"model_type\": \"xlm-roberta\""))
        assertTrue(config.contains("\"hidden_size\": 768"))
        assertTrue(config.contains("\"num_hidden_layers\": 12"))
        assertTrue(config.contains("\"num_attention_heads\": 12"))
        assertTrue(config.contains("\"vocab_size\": 250002"))
        assertTrue(config.contains("\"max_position_embeddings\": 514"))
        assertTrue(config.contains("\"type_vocab_size\": 1"))
        assertTrue(config.contains("\"pad_token_id\": 1"))
        assertTrue(config.contains("\"bos_token_id\": 0"))
        assertTrue(config.contains("\"eos_token_id\": 2"))
        assertEquals("{\"model_max_length\": 512}", tokenizerConfig)
        assertTrue(tokenizer.startsWith("{\"version\":\"1.0\""))
        for (token in listOf("<s>", "<pad>", "</s>", "<unk>", "<mask>")) {
            assertTrue(tokenizer.contains(token))
        }
    }

    private fun assertFailure(
        root: Path,
        manifest: HimXlmRBaseModelArtifactManifestV1.Manifest,
        expectedReason: HimXlmRBaseModelArtifactVerifierV1.FailureReason,
    ) {
        val result = HimXlmRBaseModelArtifactVerifierV1.verify(
            HimXlmRBaseModelArtifactVerifierV1.Request(root, manifest),
        )
        assertEquals(expectedReason, failed(result).reason)
    }

    private fun manifestWithReplacement(
        manifest: HimXlmRBaseModelArtifactManifestV1.Manifest,
        role: HimXlmRBaseModelArtifactManifestV1.ArtifactRole,
        replacement: (HimXlmRBaseModelArtifactManifestV1.Artifact) -> HimXlmRBaseModelArtifactManifestV1.Artifact,
    ): HimXlmRBaseModelArtifactManifestV1.Manifest =
        HimXlmRBaseModelArtifactManifestV1.create(
            manifest.artifacts.map { artifact ->
                if (artifact.role == role) replacement(artifact) else artifact
            },
        )

    private fun completed(
        result: HimXlmRBaseModelArtifactVerifierV1.Result,
    ): HimXlmRBaseModelArtifactManifestV1.Manifest =
        (result as HimXlmRBaseModelArtifactVerifierV1.Result.Completed).value

    private fun failed(
        result: HimXlmRBaseModelArtifactVerifierV1.Result,
    ): HimXlmRBaseModelArtifactVerifierV1.Result.Failed =
        (result as HimXlmRBaseModelArtifactVerifierV1.Result.Failed)

    private fun withFixture(block: (Path, HimXlmRBaseModelArtifactManifestV1.Manifest) -> Unit) {
        val root = Files.createTempDirectory("him-xlm-r-artifacts-")
        try {
            val bytesByRole = mapOf(
                HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_CONFIG to "config".toByteArray(),
                HimXlmRBaseModelArtifactManifestV1.ArtifactRole.MODEL_WEIGHTS to safetensorsBytes(),
                HimXlmRBaseModelArtifactManifestV1.ArtifactRole.TOKENIZER_MODEL to "sentencepiece".toByteArray(),
                HimXlmRBaseModelArtifactManifestV1.ArtifactRole.TOKENIZER_DEFINITION to "tokenizer".toByteArray(),
                HimXlmRBaseModelArtifactManifestV1.ArtifactRole.TOKENIZER_CONFIG to "tokenizer-config".toByteArray(),
            )
            val manifest = HimXlmRBaseModelArtifactManifestV1.create(
                HimXlmRBaseModelArtifactManifestV1.EXPECTED_ARTIFACTS.map { artifact ->
                    val bytes = bytesByRole.getValue(artifact.role)
                    artifact.copy(byteSize = bytes.size.toLong(), sha256 = digest(bytes))
                },
            )
            val artifactRoot = root.resolve(HimXlmRBaseModelArtifactManifestV1.LOCAL_ARTIFACT_REVISION_ROOT)
            Files.createDirectories(artifactRoot)
            manifest.artifacts.forEach { artifact ->
                val bytes = bytesByRole.getValue(artifact.role)
                Files.write(artifactRoot.resolve(artifact.filename), bytes)
            }
            block(root, manifest)
        } finally {
            deleteTree(root)
        }
    }

    private fun safetensorsBytes(): ByteArray = ByteArray(32).also { bytes ->
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putLong(1L)
        bytes[8] = '{'.code.toByte()
        bytes[9] = '}'.code.toByte()
    }

    private fun digest(bytes: ByteArray): HimSha256 = HimSha256(
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    private fun HimXlmRBaseModelArtifactManifestV1.Artifact.filenameAtRoot(): Path =
        Path.of(HimXlmRBaseModelArtifactManifestV1.LOCAL_ARTIFACT_REVISION_ROOT, filename)

    private fun findRepositoryRoot(): Path {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
        while (true) {
            if (Files.isRegularFile(current.resolve("settings.gradle.kts")) &&
                Files.isRegularFile(current.resolve("gradlew"))
            ) {
                return current
            }
            current = current.parent ?: error("Repository root not found")
        }
    }

    private fun deleteTree(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
