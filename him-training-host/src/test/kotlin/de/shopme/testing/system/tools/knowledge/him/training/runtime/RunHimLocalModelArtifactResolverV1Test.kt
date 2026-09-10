package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.runtime.HimLocalModelArtifactResolverV1
import de.shopme.tools.knowledge.him.training.runtime.HimModelArtifactResolutionV1
import de.shopme.tools.knowledge.him.training.runtime.HimModelBindingV1
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Comparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimLocalModelArtifactResolverV1Test {
    @Test
    fun validExplicitArtifactsResolve() = withRoot { root ->
        val result = completed(validRequest(root))

        assertEquals(HimLocalModelArtifactResolverV1.CONTRACT_ID, result.contractId)
        assertEquals(HimLocalModelArtifactResolverV1.VERSION, result.version)
        assertEquals(HimLocalModelArtifactResolverV1.STATE, result.state)
        assertEquals(root.toRealPath(), Path.of(result.baseModel.operationalPath).parent)
        assertEquals(Path.of("base-model.artifact"), Path.of(result.baseModel.relativePath))
        assertEquals(Path.of("tokenizer.artifact"), Path.of(result.tokenizer.relativePath))
        assertEquals(Path.of("configuration.artifact"), Path.of(result.modelConfiguration.relativePath))
    }

    @Test
    fun requestRequiresTheActualModelBindingType() {
        assertTrue(
            HimLocalModelArtifactResolverV1.Request::class.java.declaredConstructors.any { constructor ->
                constructor.parameterTypes.contains(HimModelBindingV1::class.java)
            },
        )
        assertEquals(
            HimModelBindingV1::class.java,
            HimLocalModelArtifactResolverV1.Request::class.java.declaredFields
                .single { it.name == "modelBinding" }
                .type,
        )
    }

    @Test
    fun exactlyThreeArtifactRolesAreRequired() {
        assertEquals(
            setOf("BASE_MODEL", "TOKENIZER", "MODEL_CONFIGURATION"),
            HimModelArtifactResolutionV1.ArtifactRole.entries.map { it.name }.toSet(),
        )
        assertEquals(3, HimLocalModelArtifactResolverV1.REQUIRED_ARTIFACT_ROLE_COUNT)
    }

    @Test
    fun baseModelDigestIsVerified() = withRoot { root ->
        val result = completed(validRequest(root))
        assertEquals(result.baseModel.expectedDigest, result.baseModel.actualDigest)
        assertEquals(digest(BASE_BYTES), result.baseModel.actualDigest)
    }

    @Test
    fun tokenizerDigestIsVerified() = withRoot { root ->
        val result = completed(validRequest(root))
        assertEquals(result.tokenizer.expectedDigest, result.tokenizer.actualDigest)
        assertEquals(digest(TOKENIZER_BYTES), result.tokenizer.actualDigest)
    }

    @Test
    fun modelConfigurationDigestIsVerified() = withRoot { root ->
        val result = completed(validRequest(root))
        assertEquals(result.modelConfiguration.expectedDigest, result.modelConfiguration.actualDigest)
        assertEquals(digest(CONFIGURATION_BYTES), result.modelConfiguration.actualDigest)
    }

    @Test
    fun baseModelDigestMismatchFailsClosed() = withRoot { root ->
        val request = validRequest(root)
        Files.write(root.resolve(request.baseModelPath), "different-base".toByteArray())
        assertEquals(
            HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_DIGEST_MISMATCH,
            failed(request).reason,
        )
    }

    @Test
    fun oneBytePlaceholderBaseModelFailsClosed() = withRoot { root ->
        val request = validRequest(root)
        Files.write(root.resolve(request.baseModelPath), byteArrayOf(0))
        assertEquals(
            HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_DIGEST_MISMATCH,
            failed(request).reason,
        )
    }

    @Test
    fun staleOldManifestBindingFailsClosed() = withRoot { root ->
        val baseBytes = BASE_BYTES
        val tokenizerBytes = TOKENIZER_BYTES
        val configurationBytes = CONFIGURATION_BYTES
        write(root, Path.of("base-model.artifact"), baseBytes)
        write(root, Path.of("tokenizer.artifact"), tokenizerBytes)
        write(root, Path.of("configuration.artifact"), configurationBytes)
        val staleBinding = HimModelBindingV1.create(
            modelFamilyId = "model-family:fixture:v1",
            baseModelId = "base-model:fixture:v1",
            baseModelArtifactDigest = HimSha256("812207b3c475a52c392af100879cfd0b2815d48c90b1f75d27929fc06ca30ea1"),
            tokenizerId = "tokenizer:fixture:v1",
            tokenizerArtifactDigest = digest(tokenizerBytes),
            modelConfigurationArtifactDigest = digest(configurationBytes),
        )
        val request = HimLocalModelArtifactResolverV1.Request(
            modelBinding = staleBinding,
            artifactRoot = root,
            baseModelPath = Path.of("base-model.artifact"),
            tokenizerPath = Path.of("tokenizer.artifact"),
            modelConfigurationPath = Path.of("configuration.artifact"),
        )
        assertEquals(
            HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_DIGEST_MISMATCH,
            failed(request).reason,
        )
    }

    @Test
    fun tokenizerDigestMismatchFailsClosed() = withRoot { root ->
        val request = validRequest(root)
        Files.write(root.resolve(request.tokenizerPath), "different-tokenizer".toByteArray())
        assertEquals(
            HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_DIGEST_MISMATCH,
            failed(request).reason,
        )
    }

    @Test
    fun modelConfigurationDigestMismatchFailsClosed() = withRoot { root ->
        val request = validRequest(root)
        Files.write(root.resolve(request.modelConfigurationPath), "different-configuration".toByteArray())
        assertEquals(
            HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_DIGEST_MISMATCH,
            failed(request).reason,
        )
    }

    @Test
    fun missingRootFailsClosed() {
        val root = Files.createTempDirectory("him-resolver-missing-root-")
        deleteTree(root)
        val result = HimLocalModelArtifactResolverV1.resolve(
            request(root),
        )

        assertEquals(
            HimLocalModelArtifactResolverV1.FailureReason.INVALID_ARTIFACT_ROOT,
            failed(result).reason,
        )
    }

    @Test
    fun rootFileIsNotAcceptedAsArtifactRoot() {
        val rootFile = Files.createTempFile("him-resolver-root-file-", ".tmp")
        try {
            val result = HimLocalModelArtifactResolverV1.resolve(request(rootFile))
            assertEquals(
                HimLocalModelArtifactResolverV1.FailureReason.INVALID_ARTIFACT_ROOT,
                failed(result).reason,
            )
        } finally {
            Files.deleteIfExists(rootFile)
        }
    }

    @Test
    fun missingArtifactFailsClosed() = withRoot { root ->
        val request = validRequest(root)
        Files.delete(root.resolve(request.baseModelPath))
        assertEquals(
            HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_NOT_FOUND,
            failed(request).reason,
        )
    }

    @Test
    fun directoryArtifactIsNotAccepted() = withRoot { root ->
        val request = validRequest(root)
        Files.delete(root.resolve(request.baseModelPath))
        Files.createDirectory(root.resolve(request.baseModelPath))
        assertEquals(
            HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_NOT_REGULAR_FILE,
            failed(request).reason,
        )
    }

    @Test
    fun parentTraversalIsRejected() = withRoot { root ->
        val request = requestWithOnlyNonBaseFiles(root, Path.of("..", "outside.artifact"))
        assertEquals(
            HimLocalModelArtifactResolverV1.FailureReason.INVALID_ARTIFACT_PATH,
            failed(request).reason,
        )
    }

    @Test
    fun absoluteArtifactPathIsRejected() = withRoot { root ->
        val request = requestWithOnlyNonBaseFiles(root, root.resolve("outside.artifact"))
        assertEquals(
            HimLocalModelArtifactResolverV1.FailureReason.INVALID_ARTIFACT_PATH,
            failed(request).reason,
        )
    }

    @Test
    fun artifactPathEscapingThroughSymlinkedParentIsRejected() = withRoot { root ->
        val external = Files.createTempDirectory("him-resolver-external-parent-")
        try {
            Files.write(external.resolve("model.artifact"), BASE_BYTES)
            Files.createSymbolicLink(root.resolve("external"), external)
            val request = requestWithOnlyNonBaseFiles(root, Path.of("external", "model.artifact"))
            assertEquals(
                HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_ROOT_ESCAPE,
                failed(request).reason,
            )
        } finally {
            deleteTree(external)
        }
    }

    @Test
    fun internalSymlinkedArtifactIsRejected() = withRoot { root ->
        val target = root.resolve("model-target.artifact")
        Files.write(target, BASE_BYTES)
        Files.createSymbolicLink(root.resolve("model-link.artifact"), target)
        val request = requestWithOnlyNonBaseFiles(root, Path.of("model-link.artifact"))
        assertEquals(
            HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_SYMLINK_REJECTED,
            failed(request).reason,
        )
    }

    @Test
    fun externalSymlinkedArtifactIsRejectedAsRootEscape() = withRoot { root ->
        val external = Files.createTempDirectory("him-resolver-external-artifact-")
        try {
            Files.write(external.resolve("model.artifact"), BASE_BYTES)
            Files.createSymbolicLink(root.resolve("model-link.artifact"), external.resolve("model.artifact"))
            val request = requestWithOnlyNonBaseFiles(root, Path.of("model-link.artifact"))
            assertEquals(
                HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_ROOT_ESCAPE,
                failed(request).reason,
            )
        } finally {
            deleteTree(external)
        }
    }

    @Test
    fun nestedRegularFileResolves() = withRoot { root ->
        val request = validRequest(
            root = root,
            basePath = Path.of("nested", "base.artifact"),
            tokenizerPath = Path.of("nested", "tokenizer.artifact"),
            configurationPath = Path.of("nested", "configuration.artifact"),
        )
        val result = completed(request)
        assertEquals(Path.of("nested", "base.artifact"), Path.of(result.baseModel.relativePath))
        assertTrue(Path.of(result.baseModel.operationalPath).startsWith(root.toRealPath()))
    }

    @Test
    fun resolverHasNoDirectoryScanOrSearchSurface() {
        val forbidden = setOf("scan", "search", "walk", "list", "find")
        val names = HimLocalModelArtifactResolverV1::class.java.declaredMethods.map { it.name.lowercase() }
        assertTrue(names.none { name -> forbidden.any { token -> token in name } })
    }

    @Test
    fun verifiedActualDigestEqualsExpectedDigest() = withRoot { root ->
        val result = completed(validRequest(root))
        assertEquals(result.baseModel.expectedDigest, result.baseModel.actualDigest)
        assertEquals(result.tokenizer.expectedDigest, result.tokenizer.actualDigest)
        assertEquals(result.modelConfiguration.expectedDigest, result.modelConfiguration.actualDigest)
    }

    @Test
    fun modelBindingIdentityIsPreserved() = withRoot { root ->
        val request = validRequest(root)
        val result = completed(request)
        assertEquals(request.modelBinding, result.modelBinding)
        assertEquals(request.modelBinding.logicalDigest, result.modelBinding.logicalDigest)
        assertEquals(request.modelBinding.modelBindingReference, result.modelBinding.modelBindingReference)
    }

    @Test
    fun resolvedRolesRemainDistinct() = withRoot { root ->
        val result = completed(validRequest(root))
        assertEquals(
            setOf(
                HimModelArtifactResolutionV1.ArtifactRole.BASE_MODEL,
                HimModelArtifactResolutionV1.ArtifactRole.TOKENIZER,
                HimModelArtifactResolutionV1.ArtifactRole.MODEL_CONFIGURATION,
            ),
            setOf(result.baseModel.role, result.tokenizer.role, result.modelConfiguration.role),
        )
    }

    @Test
    fun identicalContentsRemainExplicitlyRoleBound() = withRoot { root ->
        val bytes = "same-content".toByteArray()
        val request = validRequest(
            root = root,
            baseBytes = bytes,
            tokenizerBytes = bytes,
            configurationBytes = bytes,
        )
        val result = completed(request)
        assertEquals(result.baseModel.actualDigest, result.tokenizer.actualDigest)
        assertEquals(result.tokenizer.actualDigest, result.modelConfiguration.actualDigest)
        assertEquals(HimModelArtifactResolutionV1.ArtifactRole.BASE_MODEL, result.baseModel.role)
        assertEquals(HimModelArtifactResolutionV1.ArtifactRole.TOKENIZER, result.tokenizer.role)
        assertEquals(HimModelArtifactResolutionV1.ArtifactRole.MODEL_CONFIGURATION, result.modelConfiguration.role)
    }

    @Test
    fun sameExplicitLayoutProducesSameResolutionIdentity() = withRoot { firstRoot ->
        withRoot { secondRoot ->
            val first = completed(validRequest(firstRoot))
            val second = completed(validRequest(secondRoot))
            assertEquals(first.logicalDigest, second.logicalDigest)
            assertEquals(first.resolutionReference, second.resolutionReference)
        }
    }

    @Test
    fun absoluteRootPathIsExcludedFromLogicalDigest() = withRoot { firstRoot ->
        withRoot { secondRoot ->
            val first = completed(validRequest(firstRoot))
            val second = completed(validRequest(secondRoot))
            assertEquals(first.logicalDigest, second.logicalDigest)
            assertNotEquals(first.baseModel.operationalPath, second.baseModel.operationalPath)
        }
    }

    @Test
    fun changingBaseArtifactPathChangesResolutionIdentity() = withRoot { root ->
        val first = completed(validRequest(root))
        val second = completed(
            validRequest(
                root = root,
                basePath = Path.of("other-base.artifact"),
            ),
        )
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun changingTokenizerArtifactPathChangesResolutionIdentity() = withRoot { root ->
        val first = completed(validRequest(root))
        val second = completed(
            validRequest(
                root = root,
                tokenizerPath = Path.of("other-tokenizer.artifact"),
            ),
        )
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun changingConfigurationArtifactPathChangesResolutionIdentity() = withRoot { root ->
        val first = completed(validRequest(root))
        val second = completed(
            validRequest(
                root = root,
                configurationPath = Path.of("other-configuration.artifact"),
            ),
        )
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun changingBaseArtifactDigestChangesResolutionIdentity() = withRoot { root ->
        val first = completed(validRequest(root))
        val second = completed(validRequest(root, baseBytes = "different-base".toByteArray()))
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun changingTokenizerIdentityChangesResolutionIdentity() = withRoot { root ->
        val first = completed(validRequest(root))
        val second = completed(validRequest(root, tokenizerBytes = "different-tokenizer".toByteArray()))
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun changingConfigurationIdentityChangesResolutionIdentity() = withRoot { root ->
        val first = completed(validRequest(root))
        val second = completed(validRequest(root, configurationBytes = "different-configuration".toByteArray()))
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun noRemoteUrlApiIsExposed() {
        val names = publicSurfaceNames()
        assertTrue(names.none { it.contains("url") || it.contains("http") || it.contains("remote") })
    }

    @Test
    fun noDownloadApiIsExposed() {
        assertTrue(publicSurfaceNames().none { it.contains("download") })
        assertEquals("0", HimLocalModelArtifactResolverV1.UNBOUNDED_FILESYSTEM_SEARCH)
    }

    @Test
    fun noModelParsingOrLoadingApiIsExposed() {
        val forbidden = setOf("parse", "load", "deserialize", "safetensor", "pytorch", "tokenizer")
        assertTrue(publicSurfaceNames().none { name -> forbidden.any { token -> token in name } })
        assertEquals("0", HimLocalModelArtifactResolverV1.MODEL_CONTENT_INTERPRETATION)
    }

    @Test
    fun noPythonOrProcessApiIsExposed() {
        val forbidden = setOf("python", "process", "exec", "command", "runtime")
        assertTrue(publicSurfaceNames().none { name -> forbidden.any { token -> token in name } })
    }

    @Test
    fun noCopyOrMutationApiIsExposed() {
        val forbidden = setOf("copy", "write", "move", "delete", "persist", "save", "store")
        assertTrue(publicSurfaceNames().none { name -> forbidden.any { token -> token in name } })
        assertEquals("0", HimLocalModelArtifactResolverV1.ARTIFACT_CONTENT_MUTATIONS)
    }

    @Test
    fun noPersistenceSurfaceIsExposed() {
        val forbidden = setOf("persist", "json", "database", "sqlite", "report")
        assertTrue(publicSurfaceNames().none { name -> forbidden.any { token -> token in name } })
    }

    @Test
    fun everyPublicFailureReasonIsDefinedAndReachableByTheFixture() {
        assertEquals(
            setOf(
                HimLocalModelArtifactResolverV1.FailureReason.INVALID_ARTIFACT_ROOT,
                HimLocalModelArtifactResolverV1.FailureReason.INVALID_ARTIFACT_PATH,
                HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_NOT_FOUND,
                HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_NOT_REGULAR_FILE,
                HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_SYMLINK_REJECTED,
                HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_ROOT_ESCAPE,
                HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_DIGEST_MISMATCH,
            ),
            HimLocalModelArtifactResolverV1.FailureReason.entries.toSet(),
        )
        withRoot { root ->
            val request = validRequest(root)
            Files.write(root.resolve(request.baseModelPath), "wrong".toByteArray())
            assertEquals(
                HimLocalModelArtifactResolverV1.FailureReason.ARTIFACT_DIGEST_MISMATCH,
                failed(request).reason,
            )
        }
    }

    @Test
    fun unexpectedLocalFilesystemStateFailsClosed() {
        val rootFile = Files.createTempFile("him-resolver-unexpected-state-", ".tmp")
        try {
            val result = HimLocalModelArtifactResolverV1.resolve(request(rootFile))
            assertIs<HimLocalModelArtifactResolverV1.Result.Failed>(result)
        } finally {
            Files.deleteIfExists(rootFile)
        }
    }

    @Test
    fun futureRuntimeCompatibilityIsTypedAndReadOnly() = withRoot { root ->
        val result = completed(validRequest(root))
        val fields = HimModelArtifactResolutionV1::class.java.declaredFields.map { it.name }.toSet()
        assertTrue(fields.containsAll(setOf("modelBinding", "baseModel", "tokenizer", "modelConfiguration")))
        assertTrue(Path.of(result.baseModel.operationalPath).isAbsolute)
        assertTrue(Path.of(result.tokenizer.operationalPath).isAbsolute)
        assertTrue(Path.of(result.modelConfiguration.operationalPath).isAbsolute)
    }

    @Test
    fun resolutionReferenceBindsResolutionDigest() = withRoot { root ->
        val result = completed(validRequest(root))
        assertEquals(
            "local-model-artifact-resolution:v1:${result.logicalDigest.value}",
            result.resolutionReference,
        )
    }

    @Test
    fun resolvedPathsAreRealAndRootBound() = withRoot { root ->
        val realRoot = root.toRealPath()
        val result = completed(validRequest(root))
        listOf(result.baseModel, result.tokenizer, result.modelConfiguration).forEach { artifact ->
            val operationalPath = Path.of(artifact.operationalPath)
            assertEquals(operationalPath, operationalPath.toRealPath())
            assertTrue(operationalPath.startsWith(realRoot))
            assertTrue(!Path.of(artifact.relativePath).isAbsolute)
        }
    }

    @Test
    fun resolverUsesExactSha256VerificationAndStrictSymlinkPolicy() {
        assertEquals("SHA256_EXACT", HimLocalModelArtifactResolverV1.ARTIFACT_DIGEST_VERIFICATION)
        assertEquals("REJECT_ALL_SYMLINKED_ARTIFACT_PATHS", HimLocalModelArtifactResolverV1.SYMLINK_POLICY)
        assertEquals("NO", HimLocalModelArtifactResolverV1.ABSOLUTE_PATH_IN_LOGICAL_DIGEST)
    }

    private fun completed(
        request: HimLocalModelArtifactResolverV1.Request,
    ): HimModelArtifactResolutionV1 =
        assertIs<HimLocalModelArtifactResolverV1.Result.Completed>(
            HimLocalModelArtifactResolverV1.resolve(request),
        ).value

    private fun failed(
        request: HimLocalModelArtifactResolverV1.Request,
    ): HimLocalModelArtifactResolverV1.Result.Failed =
        failed(HimLocalModelArtifactResolverV1.resolve(request))

    private fun failed(
        result: HimLocalModelArtifactResolverV1.Result,
    ): HimLocalModelArtifactResolverV1.Result.Failed =
        assertIs<HimLocalModelArtifactResolverV1.Result.Failed>(result)

    private fun validRequest(
        root: Path,
        baseBytes: ByteArray = BASE_BYTES,
        tokenizerBytes: ByteArray = TOKENIZER_BYTES,
        configurationBytes: ByteArray = CONFIGURATION_BYTES,
        basePath: Path = Path.of("base-model.artifact"),
        tokenizerPath: Path = Path.of("tokenizer.artifact"),
        configurationPath: Path = Path.of("configuration.artifact"),
    ): HimLocalModelArtifactResolverV1.Request {
        write(root, basePath, baseBytes)
        write(root, tokenizerPath, tokenizerBytes)
        write(root, configurationPath, configurationBytes)
        return HimLocalModelArtifactResolverV1.Request(
            modelBinding = binding(baseBytes, tokenizerBytes, configurationBytes),
            artifactRoot = root,
            baseModelPath = basePath,
            tokenizerPath = tokenizerPath,
            modelConfigurationPath = configurationPath,
        )
    }

    private fun requestWithOnlyNonBaseFiles(
        root: Path,
        basePath: Path,
    ): HimLocalModelArtifactResolverV1.Request {
        write(root, Path.of("tokenizer.artifact"), TOKENIZER_BYTES)
        write(root, Path.of("configuration.artifact"), CONFIGURATION_BYTES)
        return HimLocalModelArtifactResolverV1.Request(
            modelBinding = binding(),
            artifactRoot = root,
            baseModelPath = basePath,
            tokenizerPath = Path.of("tokenizer.artifact"),
            modelConfigurationPath = Path.of("configuration.artifact"),
        )
    }

    private fun request(root: Path): HimLocalModelArtifactResolverV1.Request =
        HimLocalModelArtifactResolverV1.Request(
            modelBinding = binding(),
            artifactRoot = root,
            baseModelPath = Path.of("base-model.artifact"),
            tokenizerPath = Path.of("tokenizer.artifact"),
            modelConfigurationPath = Path.of("configuration.artifact"),
        )

    private fun binding(
        baseBytes: ByteArray = BASE_BYTES,
        tokenizerBytes: ByteArray = TOKENIZER_BYTES,
        configurationBytes: ByteArray = CONFIGURATION_BYTES,
    ): HimModelBindingV1 =
        HimModelBindingV1.create(
            modelFamilyId = "model-family:fixture:v1",
            baseModelId = "base-model:fixture:v1",
            baseModelArtifactDigest = digest(baseBytes),
            tokenizerId = "tokenizer:fixture:v1",
            tokenizerArtifactDigest = digest(tokenizerBytes),
            modelConfigurationArtifactDigest = digest(configurationBytes),
        )

    private fun write(root: Path, relativePath: Path, bytes: ByteArray) {
        val target = root.resolve(relativePath)
        target.parent?.let { Files.createDirectories(it) }
        Files.write(target, bytes)
    }

    private fun digest(bytes: ByteArray): HimSha256 = HimSha256(
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    private fun publicSurfaceNames(): Set<String> = buildSet {
        addAll(HimLocalModelArtifactResolverV1::class.java.declaredMethods.map { it.name.lowercase() })
        addAll(HimLocalModelArtifactResolverV1::class.java.declaredFields.map { it.name.lowercase() })
    }

    private fun <T> withRoot(block: (Path) -> T): T {
        val root = Files.createTempDirectory("him-local-model-artifacts-")
        return try {
            block(root)
        } finally {
            deleteTree(root)
        }
    }

    private fun deleteTree(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private companion object {
        val BASE_BYTES = "base-model-fixture".toByteArray()
        val TOKENIZER_BYTES = "tokenizer-fixture".toByteArray()
        val CONFIGURATION_BYTES = "configuration-fixture".toByteArray()
    }
}

private inline fun <reified T> assertIs(value: Any?): T {
    assertTrue(value is T)
    @Suppress("UNCHECKED_CAST")
    return value as T
}
