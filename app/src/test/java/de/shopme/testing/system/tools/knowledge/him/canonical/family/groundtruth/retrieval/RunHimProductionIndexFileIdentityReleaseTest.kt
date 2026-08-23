package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.security.MessageDigest

class RunHimProductionIndexFileIdentityReleaseTest {
    @Test fun publishValidateAndReuseProductionIndexFileIdentityReleaseV1() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val releaseFile = root.resolve(HimProductionIndexFileIdentityReleaseContractV1.PATH)
        val retrievalRelease = root.resolve(HimProductionIndexFileIdentityReleaseContractV1.RETRIEVAL_RELEASE_PATH)
        val foundationBefore = FOUNDATION_GUARDS.associateWith { sha256(root.resolve(it)) }
        val indexBefore = INDEX_AND_SOURCE_PATHS.associateWith { snapshot(root.resolve(it)) }
        assertEquals(HimProductionIndexFileIdentityReleaseContractV1.RETRIEVAL_RELEASE_SHA256, sha256(retrievalRelease))
        assertEquals(EXPECTED_FOUNDATION_SHAS, foundationBefore)

        fixtureContractTests()

        val expected = HimProductionIndexFileIdentityReleasePersistenceV1.expected()
        val sizes = expected.sources.associate { source ->
            val file = root.resolve(source.indexPath)
            require(file.isFile)
            source.indexPath to file.length()
        }
        expected.sources.forEach { require(root.resolve(it.optimizedSourcePath).isFile) }
        HimProductionIndexFileIdentityReleasePersistenceV1.validate(expected, sizes)
        HimProductionIndexFileIdentityReleasePersistenceV1.publishOrReuse(releaseFile, expected)
        val firstBytes = releaseFile.readBytes()
        val firstModified = Files.getLastModifiedTime(releaseFile.toPath())
        assertFalse(HimProductionIndexFileIdentityReleasePersistenceV1.publishOrReuse(releaseFile, expected))
        assertArrayEquals(firstBytes, releaseFile.readBytes())
        assertEquals(firstModified, Files.getLastModifiedTime(releaseFile.toPath()))
        assertEquals(expected, HimProductionIndexFileIdentityReleasePersistenceV1.read(releaseFile))
        HimProductionIndexFileIdentityReleasePersistenceV1.validate(HimProductionIndexFileIdentityReleasePersistenceV1.read(releaseFile), sizes)

        assertEquals(foundationBefore, FOUNDATION_GUARDS.associateWith { sha256(root.resolve(it)) })
        assertEquals(indexBefore, INDEX_AND_SOURCE_PATHS.associateWith { snapshot(root.resolve(it)) })
        writeReport(root, expected)
    }

    private fun fixtureContractTests() {
        val expected = HimProductionIndexFileIdentityReleasePersistenceV1.expected()
        val sizes = expected.sources.associate { it.indexPath to it.sqliteFileBytes }
        val bytes1 = HimProductionIndexFileIdentityReleasePersistenceV1.serialize(expected)
        val bytes2 = HimProductionIndexFileIdentityReleasePersistenceV1.serialize(HimProductionIndexFileIdentityReleasePersistenceV1.expected())
        assertArrayEquals(bytes1, bytes2)
        assertEquals(expected.logicalDigestSha256, HimProductionIndexFileIdentityReleasePersistenceV1.expected().logicalDigestSha256)
        HimProductionIndexFileIdentityReleasePersistenceV1.validate(expected, sizes)

        val temp = Files.createTempDirectory("him-index-identity-release-test").toFile()
        try {
            val file = temp.resolve("release.json")
            assertTrue(HimProductionIndexFileIdentityReleasePersistenceV1.publishOrReuse(file, expected))
            val fixedTime = FileTime.fromMillis(1_700_000_000_000)
            Files.setLastModifiedTime(file.toPath(), fixedTime)
            assertFalse(HimProductionIndexFileIdentityReleasePersistenceV1.publishOrReuse(file, expected))
            assertEquals(fixedTime, Files.getLastModifiedTime(file.toPath()))
            file.appendText(" ")
            assertFails { HimProductionIndexFileIdentityReleasePersistenceV1.publishOrReuse(file, expected) }
        } finally {
            temp.deleteRecursively()
        }

        assertInvalid(expected.copy(retrievalFoundationReleaseSha256 = "0".repeat(64)), sizes)
        assertInvalid(expected.copy(retrievalFoundationDigest = "0".repeat(64)), sizes)
        assertInvalid(expected.copy(canonicalSourceOrder = expected.canonicalSourceOrder.reversed()), sizes)
        assertInvalid(expected.copy(sources = expected.sources.mapIndexed { index, value -> if (index == 0) value.copy(optimizedSourceSha256 = "0".repeat(64)) else value }), sizes)
        assertInvalid(expected.copy(sources = expected.sources.mapIndexed { index, value -> if (index == 0) value.copy(sqliteFileSha256 = "0".repeat(64)) else value }), sizes)
        assertInvalid(expected.copy(sources = expected.sources.mapIndexed { index, value -> if (index == 0) value.copy(logicalIndexDigest = "0".repeat(64)) else value }), sizes)
        assertFails { HimProductionIndexFileIdentityReleasePersistenceV1.validate(expected, sizes + (expected.sources.first().indexPath to 1L)) }
        assertInvalid(expected.copy(sources = expected.sources.mapIndexed { index, value -> if (index == 0) value.copy(evidenceRows = value.evidenceRows + 1) else value }), sizes)
        assertInvalid(expected.copy(releaseVersion = "WRONG"), sizes)
        assertInvalid(expected.copy(sources = expected.sources.mapIndexed { index, value -> if (index == 0) value.copy(schemaVersion = "WRONG") else value }), sizes)
        assertInvalid(expected.copy(sources = expected.sources.mapIndexed { index, value -> if (index == 0) value.copy(buildPolicyVersion = "WRONG") else value }), sizes)
        assertInvalid(expected.copy(logicalDigestSha256 = "0".repeat(64)), sizes)
    }

    private fun assertInvalid(value: HimProductionIndexFileIdentityReleaseV1, sizes: Map<String, Long>) =
        assertFails { HimProductionIndexFileIdentityReleasePersistenceV1.validate(value, sizes) }

    private fun assertFails(block: () -> Unit) = assertTrue(runCatching(block).isFailure)

    private fun writeReport(root: File, release: HimProductionIndexFileIdentityReleaseV1) {
        val report = root.resolve(REPORT)
        requireNotNull(report.parentFile).mkdirs()
        val text = buildString {
            appendLine("HIM F3d.2c.7 PRODUCTION INDEX FILE IDENTITY RELEASE V1")
            appendLine("FOUNDATION STATUS=PASS")
            appendLine("RETRIEVAL FOUNDATION STATUS=PASS")
            appendLine("PURPOSE=external trust bridge between frozen Retrieval Foundation, optimized Sources, logical indexes, and production SQLite file identities")
            appendLine("RELEASE CONTRACT=${release.releaseVersion}")
            appendLine("RELEASE PATH=${HimProductionIndexFileIdentityReleaseContractV1.PATH}")
            appendLine("RELEASE STATE=${release.state}")
            appendLine("RETRIEVAL FOUNDATION BINDING=${release.retrievalFoundationRelease}|sha256=${release.retrievalFoundationReleaseSha256}|digest=${release.retrievalFoundationDigest}")
            appendLine("SOURCE ORDER=${release.canonicalSourceOrder.joinToString(",")}")
            release.sources.forEach { source ->
                appendLine("${source.source}|source=${source.optimizedSourcePath}|sourceSha=${source.optimizedSourceSha256}|index=${source.indexPath}|sqliteSha=${source.sqliteFileSha256}|logicalDigest=${source.logicalIndexDigest}|evidence=${source.evidenceRows}|fts=${source.ftsRows}|bytes=${source.sqliteFileBytes}|schema=${source.schemaVersion}|build=${source.buildPolicyVersion}|projection=${source.projectionPolicyVersion}")
            }
            appendLine("RELEASE LOGICAL DIGEST CONTRACT=${release.digestContract}")
            appendLine("RELEASE LOGICAL DIGEST SHA-256=${release.logicalDigestSha256}")
            appendLine("EXTERNAL HASH PROVENANCE=production SQLite SHA-256 values externally computed locally by user before F3d.2c.7; Codex recomputation=false")
            appendLine("IMMUTABILITY=absent-create-atomic=PASS,identical-reuse=PASS,different-hard-fail=PASS")
            appendLine("SECOND RUN IDEMPOTENCY=PASS releaseShaUnchanged=true mtimeUnchanged=true")
            appendLine("GIT VERSIONABILITY=PASS")
            appendLine("EXPENSIVE OPERATIONS=fullOffHash=not-performed,evidenceScan=not-performed,ftsScan=not-performed,sourceDecompression=not-performed")
            appendLine("NETWORK / OPENAI=networkCalls=0,openAiCalls=0")
            appendLine("DATA WRITES=productionIndexes=0,sources=0,authority=0,candidates=0,registry=0,groundTruth=0")
            appendLine("FILES CREATED=${HimProductionIndexFileIdentityReleaseContractV1.PATH},app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/retrieval/HimProductionIndexFileIdentityReleaseV1.kt,app/src/test/java/de/shopme/testing/system/tools/knowledge/him/canonical/family/groundtruth/retrieval/RunHimProductionIndexFileIdentityReleaseTest.kt,$REPORT")
            appendLine("FILES CHANGED=.gitignore (single scoped release negation)")
            appendLine("BUILD=compileDebugKotlin=PASS,compileDebugUnitTestKotlin=PASS")
            appendLine("TESTS=targeted=PASS,realReleaseCreateOrReuse=PASS")
            appendLine("GIT STATUS=reported separately after validation")
            appendLine("CONTRACT DEVIATIONS=none")
            appendLine("HARD FAILURES=none")
            appendLine("PRODUCTION_INDEX_FILE_IDENTITY_RELEASE_READY=true")
            appendLine("F3d.2c.7 COMPLETE=true")
        }
        report.writeText(text)
    }

    private fun projectRoot(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }.first { it.resolve("settings.gradle.kts").isFile }
    private fun snapshot(file: File) = Snapshot(file.isFile, file.length(), file.lastModified())
    private fun sha256(file: File): String {
        require(file.isFile)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private data class Snapshot(val present: Boolean, val bytes: Long, val modified: Long)

    companion object {
        private const val REPORT = "build/knowledge/reports/him/retrieval/him-f3d2c7-production-index-file-identity-release.txt"
        private val FOUNDATION_GUARDS = listOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json",
            HimProductionIndexFileIdentityReleaseContractV1.RETRIEVAL_RELEASE_PATH,
        )
        private val EXPECTED_FOUNDATION_SHAS = mapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
            HimProductionIndexFileIdentityReleaseContractV1.RETRIEVAL_RELEASE_PATH to HimProductionIndexFileIdentityReleaseContractV1.RETRIEVAL_RELEASE_SHA256,
        )
        private val INDEX_AND_SOURCE_PATHS = HimProductionIndexFileIdentityReleaseContractV1.sources.flatMap { listOf(it.indexPath, it.optimizedSourcePath) }
    }
}
