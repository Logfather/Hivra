package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleaseBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleasePersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleaseValidator
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndex
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexValidator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleaseContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleasePersistenceV1
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

private fun releaseArtifact(path: String, bytes: ByteArray, count: Int) =
    HimGroundTruthReleaseArtifactReference(path, HimSha256(
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    ), count)

class RunHimFoundationToTransactionalGroundTruthBootstrapV1Test {

    @Test
    fun `real Foundation bootstraps to deterministic transactional Release N in two roots`() {
        val firstRoot = fixtureRoot()
        val secondRoot = fixtureRoot()
        try {
            materializeFoundation(projectRoot(), firstRoot)
            materializeFoundation(projectRoot(), secondRoot)

            val first = bootstrap(firstRoot, productionOptIn = true)
            val second = bootstrap(secondRoot, productionOptIn = true)

            assertEquals(BootstrapStatus.PUBLISHED, first.status)
            assertEquals(BootstrapStatus.PUBLISHED, second.status)
            assertEquals(first.releaseReference, second.releaseReference)
            assertArrayEquals(first.material.authorityBytes, second.material.authorityBytes)
            assertArrayEquals(first.material.registryBytes, second.material.registryBytes)
            assertArrayEquals(first.material.retiredBytes, second.material.retiredBytes)
            assertArrayEquals(first.material.ledgerBytes, second.material.ledgerBytes)
            assertArrayEquals(first.material.fingerprintBytes, second.material.fingerprintBytes)
            assertArrayEquals(first.material.releaseBytes, second.material.releaseBytes)
            assertArrayEquals(first.currentPointerBytes, second.currentPointerBytes)
            assertBootstrapSemantics(first)
            assertBootstrapSemantics(second)
        } finally {
            firstRoot.deleteRecursively()
            secondRoot.deleteRecursively()
        }
    }

    @Test
    fun `matching current pointer is idempotent and does not create a second state`() {
        val root = fixtureRoot()
        try {
            materializeFoundation(projectRoot(), root)
            val first = bootstrap(root, productionOptIn = true)
            val pointerBefore = currentPointerFile(root).readBytes()
            val releaseDirectoriesBefore = releaseDirectories(root)

            val second = bootstrap(root, productionOptIn = true)

            assertEquals(BootstrapStatus.IDEMPOTENT_ALREADY_BOOTSTRAPPED, second.status)
            assertEquals(first.releaseReference, second.releaseReference)
            assertArrayEquals(pointerBefore, currentPointerFile(root).readBytes())
            assertEquals(releaseDirectoriesBefore, releaseDirectories(root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `conflicting current pointer hard fails`() {
        val root = fixtureRoot()
        try {
            materializeFoundation(projectRoot(), root)
            val material = buildMaterial(root)
            val conflictingInput = HimTransactionalGroundTruthPublicationInputV1(
                authorityBytes = material.authorityBytes,
                activeRegistryBytes = material.registryBytes,
                retiredRegistryBytes = material.retiredBytes,
                mutationLedgerBytes = material.ledgerBytes,
                fingerprintIndexBytes = material.fingerprintBytes,
                releaseBytes = material.releaseBytes + byteArrayOf('x'.code.toByte()),
            )
            HimTransactionalGroundTruthPublicationV1().publish(root, conflictingInput) { }

            assertFails {
                bootstrap(root, productionOptIn = false)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `Foundation tamper or artifact mismatch hard fails before publication`() {
        val root = fixtureRoot()
        try {
            materializeFoundation(projectRoot(), root)
            val authorityFile = HimCanonicalFamilyPaths(root).familyAuthority
            authorityFile.writeBytes(authorityFile.readBytes() + byteArrayOf('\n'.code.toByte()))

            assertFails {
                bootstrap(root, productionOptIn = false)
            }
            assertFalse(currentPointerFile(root).exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `opt in is exact and no opt in performs no production publication`() {
        assertFalse(productionOptedIn(emptyMap()))
        assertFalse(productionOptedIn(mapOf(BOOTSTRAP_ENV to "TRUE")))
        assertTrue(productionOptedIn(mapOf(BOOTSTRAP_ENV to "true")))

        val root = fixtureRoot()
        try {
            materializeFoundation(projectRoot(), root)
            val before = guardSnapshot(root)
            val result = bootstrap(root, productionOptIn = false)
            val after = guardSnapshot(root)

            assertEquals(BootstrapStatus.SKIPPED_PRODUCTION_OPT_IN, result.status)
            assertEquals(before, after)
            assertFalse(currentPointerFile(root).exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `real production bootstrap is opt in only and preserves all non transactional state`() {
        val root = projectRoot()
        val before = guardSnapshot(root)
        val result = bootstrap(root, productionOptIn = productionOptedIn(System.getenv()))
        val after = guardSnapshot(root)

        assertEquals(before, after)
        assertNotNull(result.material)

        if (productionOptedIn(System.getenv())) {
            assertTrue(result.status == BootstrapStatus.PUBLISHED || result.status == BootstrapStatus.IDEMPOTENT_ALREADY_BOOTSTRAPPED)
            assertBootstrapSemantics(result)
        } else if (currentPointerFile(root).isFile) {
            assertEquals(BootstrapStatus.IDEMPOTENT_ALREADY_BOOTSTRAPPED, result.status)
            assertBootstrapSemantics(result)
        } else {
            assertEquals(BootstrapStatus.SKIPPED_PRODUCTION_OPT_IN, result.status)
        }
    }

    private fun bootstrap(
        root: File,
        productionOptIn: Boolean,
    ): BootstrapResult {
        val material = buildMaterial(root)
        val current = HimTransactionalGroundTruthPublicationV1().readCurrent(root)
        val expectedReference = HimGroundTruthReleaseIdentityV1Factory.compute(material.input)

        if (current != null) {
            val active = HimActiveGroundTruthResolutionV1().resolve(root)
            require(active.releaseReference == expectedReference) {
                "GROUND_TRUTH_ALREADY_ACTIVE: ${active.releaseReference.value}"
            }
            assertActiveState(root, material, active)
            return BootstrapResult(
                status = BootstrapStatus.IDEMPOTENT_ALREADY_BOOTSTRAPPED,
                releaseReference = expectedReference,
                material = material,
                currentPointerBytes = currentPointerFile(root).readBytes(),
                root = root,
            )
        }

        val dryRunFirst = fixtureRoot()
        val dryRunSecond = fixtureRoot()
        try {
            materializeFoundation(root, dryRunFirst)
            materializeFoundation(root, dryRunSecond)
            val first = publish(dryRunFirst, buildMaterial(dryRunFirst))
            val second = publish(dryRunSecond, buildMaterial(dryRunSecond))
            assertEquals(first.releaseReference, second.releaseReference)
            assertArrayEquals(first.currentPointerBytes, second.currentPointerBytes)
            assertArrayEquals(first.material.authorityBytes, second.material.authorityBytes)
            assertArrayEquals(first.material.registryBytes, second.material.registryBytes)
            assertArrayEquals(first.material.retiredBytes, second.material.retiredBytes)
            assertArrayEquals(first.material.ledgerBytes, second.material.ledgerBytes)
            assertArrayEquals(first.material.fingerprintBytes, second.material.fingerprintBytes)
            assertArrayEquals(first.material.releaseBytes, second.material.releaseBytes)
        } finally {
            dryRunFirst.deleteRecursively()
            dryRunSecond.deleteRecursively()
        }

        if (!productionOptIn) {
            return BootstrapResult(
                status = BootstrapStatus.SKIPPED_PRODUCTION_OPT_IN,
                releaseReference = expectedReference,
                material = material,
                currentPointerBytes = ByteArray(0),
                root = root,
            )
        }

        val published = publish(root, material)
        assertBootstrapSemantics(published)
        return published
    }

    private fun publish(
        root: File,
        material: BootstrapMaterial,
    ): BootstrapResult {
        val publication = HimTransactionalGroundTruthPublicationV1().publish(
            projectRoot = root,
            input = material.input,
        ) { releaseDirectory ->
            validatePublishedRelease(releaseDirectory, material)
        }
        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        assertEquals(publication.releaseReference, active.releaseReference)
        assertActiveState(root, material, active)
        return BootstrapResult(
            status = if (publication.created) BootstrapStatus.PUBLISHED else BootstrapStatus.IDEMPOTENT_ALREADY_BOOTSTRAPPED,
            releaseReference = publication.releaseReference,
            material = material,
            currentPointerBytes = currentPointerFile(root).readBytes(),
            root = root,
        )
    }

    private fun buildMaterial(root: File): BootstrapMaterial {
        val paths = HimCanonicalFamilyPaths(root)
        val foundationReleaseFile = root.resolve(HimCanonicalFamilyFoundationReleaseBuilder.RELEASE_RECORD_PATH)
        val foundationRelease = HimCanonicalFamilyFoundationReleasePersistence().read(foundationReleaseFile)
        HimCanonicalFamilyFoundationReleaseValidator().validate(foundationRelease)

        val authorityFile = paths.familyAuthority
        val registryFile = paths.entityIdRegistry
        val fingerprintFile = root.resolve(HimCanonicalFamilyFoundationReleaseBuilder.FINGERPRINT_INDEX_PATH)
        require(sha256(authorityFile.readBytes()) == HimCanonicalFamilyFoundationReleaseBuilder.CANONICAL_FAMILY_AUTHORITY_SHA256)
        require(sha256(registryFile.readBytes()) == HimCanonicalFamilyFoundationReleaseBuilder.ENTITY_ID_REGISTRY_SHA256)
        require(sha256(fingerprintFile.readBytes()) == HimCanonicalFamilyFoundationReleaseBuilder.FINGERPRINT_INDEX_SHA256)

        val familyPersistence = HimCanonicalFamilyPersistence()
        val authorityBytes = authorityFile.readBytes()
        val registryBytes = registryFile.readBytes()
        val authority = familyPersistence.readAuthority(authorityFile)
        val registry = familyPersistence.readRegistry(registryFile)
        val foundationIndex = HimEntityFingerprintIndexPersistence().read(fingerprintFile)
        HimEntityFingerprintIndexValidator().validate(
            registry = registry,
            authority = authority,
            index = foundationIndex,
            registrySha256 = sha256(registryBytes),
            authoritySha256 = sha256(authorityBytes),
        )

        val retired = HimRetiredEntityIdRegistry(
            schemaVersion = RETIRED_REGISTRY_SCHEMA,
            entries = emptyList(),
        )
        val retiredBytes = serialize(retired)
        val ledgerPersistence = HimCanonicalFamilyMutationLedgerPersistenceV1()
        val ledger = ledgerPersistence.emptyLedger()
        val ledgerBytes = ledgerPersistence.serialize(ledger)
        val fingerprint = HimGroundTruthEntityFingerprintIndexBuilderV1().build(
            registry = registry,
            authority = authority,
            registrySha256 = sha256(registryBytes),
            authoritySha256 = sha256(authorityBytes),
        )
        HimGroundTruthEntityFingerprintIndexValidatorV1().validate(
            registry = registry,
            authority = authority,
            index = fingerprint,
            registrySha256 = sha256(registryBytes),
            authoritySha256 = sha256(authorityBytes),
        )
        val fingerprintBytes = HimEntityFingerprintIndexPersistence().serialize(fingerprint)
        val releaseInput = HimGroundTruthReleaseBuildInputV1(
            authority = authority,
            activeEntityIdRegistry = registry,
            retiredEntityIdRegistry = retired,
            mutationLedger = ledger,
            entityFingerprintIndex = fingerprint,
            authorityArtifact = releaseArtifact(HimTransactionalGroundTruthPublicationContractV1.AUTHORITY_FILE_NAME, authorityBytes, authority.families.size),
            activeEntityIdRegistryArtifact = releaseArtifact(HimTransactionalGroundTruthPublicationContractV1.ACTIVE_REGISTRY_FILE_NAME, registryBytes, registry.entries.size),
            retiredEntityIdRegistryArtifact = releaseArtifact(HimTransactionalGroundTruthPublicationContractV1.RETIRED_REGISTRY_FILE_NAME, retiredBytes, retired.entries.size),
            mutationLedgerArtifact = releaseArtifact(HimTransactionalGroundTruthPublicationContractV1.MUTATION_LEDGER_FILE_NAME, ledgerBytes, ledger.entries.size),
            entityFingerprintIndexArtifact = releaseArtifact(HimTransactionalGroundTruthPublicationContractV1.FINGERPRINT_INDEX_FILE_NAME, fingerprintBytes, fingerprint.entryCount),
        )
        val release = HimGroundTruthReleaseBuilderV1().build(releaseInput)
        val releaseBytes = HimGroundTruthReleasePersistenceV1().serialize(release)
        return BootstrapMaterial(
            authority = authority,
            registry = registry,
            retired = retired,
            ledger = ledger,
            fingerprint = fingerprint,
            authorityBytes = authorityBytes,
            registryBytes = registryBytes,
            retiredBytes = retiredBytes,
            ledgerBytes = ledgerBytes,
            fingerprintBytes = fingerprintBytes,
            releaseBytes = releaseBytes,
            input = HimTransactionalGroundTruthPublicationInputV1(
                authorityBytes,
                registryBytes,
                retiredBytes,
                ledgerBytes,
                fingerprintBytes,
                releaseBytes,
            ),
        )
    }

    private fun validatePublishedRelease(
        releaseDirectory: File,
        material: BootstrapMaterial,
    ) {
        val files = listOf(
            HimTransactionalGroundTruthPublicationContractV1.AUTHORITY_FILE_NAME to material.authorityBytes,
            HimTransactionalGroundTruthPublicationContractV1.ACTIVE_REGISTRY_FILE_NAME to material.registryBytes,
            HimTransactionalGroundTruthPublicationContractV1.RETIRED_REGISTRY_FILE_NAME to material.retiredBytes,
            HimTransactionalGroundTruthPublicationContractV1.MUTATION_LEDGER_FILE_NAME to material.ledgerBytes,
            HimTransactionalGroundTruthPublicationContractV1.FINGERPRINT_INDEX_FILE_NAME to material.fingerprintBytes,
            HimTransactionalGroundTruthPublicationContractV1.RELEASE_FILE_NAME to material.releaseBytes,
        )
        files.forEach { (name, bytes) ->
            assertArrayEquals(bytes, releaseDirectory.resolve(name).readBytes())
        }
        val release = HimGroundTruthReleasePersistenceV1().read(
            releaseDirectory.resolve(HimTransactionalGroundTruthPublicationContractV1.RELEASE_FILE_NAME),
        )
        val input = material.releaseInput()
        HimGroundTruthReleaseValidatorV1().validate(release, input)
        HimGroundTruthEntityFingerprintIndexValidatorV1().validate(
            registry = material.registry,
            authority = material.authority,
            index = material.fingerprint,
            registrySha256 = sha256(material.registryBytes),
            authoritySha256 = sha256(material.authorityBytes),
        )
    }

    private fun assertBootstrapSemantics(result: BootstrapResult) {
        val active = HimActiveGroundTruthResolutionV1().resolve(result.root)
        assertActiveState(result.root, result.material, active)
    }

    private fun assertActiveState(
        root: File,
        material: BootstrapMaterial,
        active: HimActiveGroundTruthArtifactsV1,
    ) {
        assertArrayEquals(material.authorityBytes, active.authorityBytes)
        assertArrayEquals(material.registryBytes, active.activeRegistryBytes)
        assertArrayEquals(material.retiredBytes, active.retiredRegistryBytes)
        assertArrayEquals(material.ledgerBytes, active.mutationLedgerBytes)
        assertArrayEquals(material.fingerprintBytes, active.fingerprintIndexBytes)
        assertArrayEquals(material.releaseBytes, active.releaseBytes)
        assertEquals(material.authority, HimCanonicalFamilyPersistence().readAuthority(active.authorityFile))
        assertEquals(material.registry, HimCanonicalFamilyPersistence().readRegistry(active.activeRegistryFile))
        assertEquals(emptyList<Any>(), material.retired.entries)
        assertEquals(emptyList<Any>(), material.ledger.entries)
        assertTrue(active.releaseDirectory.isDirectory)
        assertEquals(
            HimGroundTruthReleaseIdentityV1Factory.compute(material.input),
            active.releaseReference,
        )
        assertEquals(root.resolve("data/knowledge/him/canonical-family/groundtruth/current.v1.json"), currentPointerFile(root))
    }

    private fun materializeFoundation(sourceRoot: File, targetRoot: File) {
        val sourcePaths = HimCanonicalFamilyPaths(sourceRoot)
        val targetPaths = HimCanonicalFamilyPaths(targetRoot)
        val files = listOf(
            sourcePaths.familyAuthority to targetPaths.familyAuthority,
            sourcePaths.entityIdRegistry to targetPaths.entityIdRegistry,
            sourceRoot.resolve(HimCanonicalFamilyFoundationReleaseBuilder.FINGERPRINT_INDEX_PATH) to
                    targetRoot.resolve(HimCanonicalFamilyFoundationReleaseBuilder.FINGERPRINT_INDEX_PATH),
            sourceRoot.resolve(HimCanonicalFamilyFoundationReleaseBuilder.RELEASE_RECORD_PATH) to
                    targetRoot.resolve(HimCanonicalFamilyFoundationReleaseBuilder.RELEASE_RECORD_PATH),
        )
        files.forEach { (source, target) ->
            require(source.isFile) { "Missing Foundation artifact: ${source.absolutePath}" }
            val parent = requireNotNull(target.parentFile)
            require(parent.mkdirs() || parent.isDirectory)
            source.copyTo(target, overwrite = true)
        }
    }

    private fun guardSnapshot(root: File): Map<String, FileState> {
        val fixed = linkedSetOf(
            HimCanonicalFamilyFoundationReleaseBuilder.PRODUCT_MASTER_PATH,
            HimCanonicalFamilyFoundationReleaseBuilder.RELEASE_RECORD_PATH,
            HimCanonicalFamilyFoundationReleaseBuilder.CANONICAL_FAMILY_AUTHORITY_PATH,
            HimCanonicalFamilyFoundationReleaseBuilder.ENTITY_ID_REGISTRY_PATH,
            HimCanonicalFamilyFoundationReleaseBuilder.FINGERPRINT_INDEX_PATH,
            "data/knowledge/him/retrieval/master/him-production-index-file-identity-release.v1.json",
            "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json",
            "data/knowledge/dimensions",
        )
        HimProductionIndexFileIdentityReleaseContractV1.sources.forEach { source ->
            fixed += source.optimizedSourcePath
            fixed += source.indexPath
        }
        return fixed.flatMap { relative ->
            if (relative.endsWith("/dimensions")) {
                snapshotTree(root, relative).map { it.key to it.value }
            } else {
                listOf(relative to fileState(root.resolve(relative)))
            }
        }.toMap() + snapshotTree(root, "data/knowledge/him/candidates") + snapshotTree(root, "data/knowledge/him/validation")
    }

    private fun snapshotTree(root: File, relative: String): Map<String, FileState> {
        val directory = root.resolve(relative)
        if (!directory.exists()) return mapOf(relative to FileState(false, 0L, 0L))
        return Files.walk(directory.toPath()).use { stream ->
            stream.filter { Files.isRegularFile(it) }.map { path ->
                val file = path.toFile()
                root.toPath().relativize(path).toString() to fileState(file)
            }.toList().toMap()
        }
    }

    private fun fileState(file: File) = FileState(file.isFile, if (file.isFile) file.length() else 0L, if (file.exists()) file.lastModified() else 0L)

    private fun releaseDirectories(root: File): List<String> =
        root.resolve("data/knowledge/him/canonical-family/groundtruth/releases")
            .listFiles()
            .orEmpty()
            .map { it.name }
            .sorted()

    private fun currentPointerFile(root: File) = root.resolve(
        "data/knowledge/him/canonical-family/groundtruth/current.v1.json",
    )

    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile }

    private fun fixtureRoot() = Files.createTempDirectory("him-foundation-bootstrap-v1-").toFile()

    private fun serialize(value: Any) =
        (GSON.toJson(value) + "\n").toByteArray(Charsets.UTF_8)

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun assertFails(block: () -> Unit) {
        assertTrue(runCatching(block).isFailure)
    }

    private fun productionOptedIn(environment: Map<String, String>): Boolean =
        environment[BOOTSTRAP_ENV] == "true"

    private data class BootstrapMaterial(
        val authority: HimCanonicalFamilyAuthority,
        val registry: HimEntityIdRegistry,
        val retired: HimRetiredEntityIdRegistry,
        val ledger: HimCanonicalFamilyMutationLedger,
        val fingerprint: HimEntityFingerprintIndex,
        val authorityBytes: ByteArray,
        val registryBytes: ByteArray,
        val retiredBytes: ByteArray,
        val ledgerBytes: ByteArray,
        val fingerprintBytes: ByteArray,
        val releaseBytes: ByteArray,
        val input: HimTransactionalGroundTruthPublicationInputV1,
    ) {
        fun releaseInput() = HimGroundTruthReleaseBuildInputV1(
            authority = authority,
            activeEntityIdRegistry = registry,
            retiredEntityIdRegistry = retired,
            mutationLedger = ledger,
            entityFingerprintIndex = fingerprint,
            authorityArtifact = releaseArtifact(HimTransactionalGroundTruthPublicationContractV1.AUTHORITY_FILE_NAME, authorityBytes, authority.families.size),
            activeEntityIdRegistryArtifact = releaseArtifact(HimTransactionalGroundTruthPublicationContractV1.ACTIVE_REGISTRY_FILE_NAME, registryBytes, registry.entries.size),
            retiredEntityIdRegistryArtifact = releaseArtifact(HimTransactionalGroundTruthPublicationContractV1.RETIRED_REGISTRY_FILE_NAME, retiredBytes, retired.entries.size),
            mutationLedgerArtifact = releaseArtifact(HimTransactionalGroundTruthPublicationContractV1.MUTATION_LEDGER_FILE_NAME, ledgerBytes, ledger.entries.size),
            entityFingerprintIndexArtifact = releaseArtifact(HimTransactionalGroundTruthPublicationContractV1.FINGERPRINT_INDEX_FILE_NAME, fingerprintBytes, fingerprint.entryCount),
        )
    }

    private data class BootstrapResult(
        val status: BootstrapStatus,
        val releaseReference: HimGroundTruthReleaseIdentityV1,
        val material: BootstrapMaterial,
        val currentPointerBytes: ByteArray,
        val root: File,
    )

    private data class FileState(val exists: Boolean, val length: Long, val lastModified: Long)

    private enum class BootstrapStatus {
        PUBLISHED,
        IDEMPOTENT_ALREADY_BOOTSTRAPPED,
        SKIPPED_PRODUCTION_OPT_IN,
    }

    companion object {
        private const val BOOTSTRAP_ENV = "HIM_GROUND_TRUTH_BOOTSTRAP"
        private const val RETIRED_REGISTRY_SCHEMA = "HIM_RETIRED_ENTITY_ID_REGISTRY_V1"
        private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    }
}
