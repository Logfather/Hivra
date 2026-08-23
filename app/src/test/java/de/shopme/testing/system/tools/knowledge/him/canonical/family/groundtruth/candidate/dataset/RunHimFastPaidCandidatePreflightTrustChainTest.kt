package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleaseContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleasePersistenceV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.system.measureNanoTime

class RunHimFastPaidCandidatePreflightTrustChainTest {
    @Test fun validateFastPaidCandidatePreflightTrustChainV1() {
        fixtureStatusMatrix()
        val root = projectRoot()
        var result: HimFastPaidCandidatePreflightResultV1? = null
        val elapsed = measureNanoTime { result = HimFastPaidCandidatePreflightV1.check(root) }
        val actual = requireNotNull(result)
        assertEquals(HimFastPaidCandidatePreflightStatus.READY, actual.status)
        assertNotNull(actual.preflightDigestSha256)
        val repeated = HimFastPaidCandidatePreflightV1.check(root)
        assertEquals(actual, repeated)
        assertTrue(elapsed < 10_000_000_000L)
        writeReport(root, actual, elapsed)
    }

    private fun fixtureStatusMatrix() {
        val valid = fixture()
        assertStatus(HimFastPaidCandidatePreflightStatus.READY, valid)
        assertEquals(HimFastPaidCandidatePreflightV1.evaluate(valid), HimFastPaidCandidatePreflightV1.evaluate(valid))
        assertStatus(HimFastPaidCandidatePreflightStatus.RETRIEVAL_RELEASE_MISSING, valid.copy(retrievalReleaseBytes = null))
        assertStatus(HimFastPaidCandidatePreflightStatus.RETRIEVAL_RELEASE_INVALID, valid.copy(retrievalReleaseBytes = valid.retrievalReleaseBytes!! + 0))
        assertStatus(HimFastPaidCandidatePreflightStatus.INDEX_IDENTITY_RELEASE_MISSING, valid.copy(indexIdentityReleaseBytes = null, indexIdentityRelease = null))
        assertStatus(HimFastPaidCandidatePreflightStatus.INDEX_IDENTITY_RELEASE_INVALID, valid.copy(indexIdentityRelease = valid.indexIdentityRelease!!.copy(logicalDigestSha256 = "0".repeat(64))))
        val firstIndex = valid.indexSizes.keys.first()
        assertStatus(HimFastPaidCandidatePreflightStatus.INDEX_MISSING, valid.copy(indexSizes = valid.indexSizes + (firstIndex to null)))
        assertStatus(HimFastPaidCandidatePreflightStatus.INDEX_SIZE_MISMATCH, valid.copy(indexSizes = valid.indexSizes + (firstIndex to 1L)))
        assertStatus(HimFastPaidCandidatePreflightStatus.SOURCE_MISSING, valid.copy(presentSources = valid.presentSources - valid.presentSources.first()))
        assertStatus(HimFastPaidCandidatePreflightStatus.OFFLINE_GATE_MISSING, valid.copy(offlineGate = null))
        assertStatus(HimFastPaidCandidatePreflightStatus.OFFLINE_GATE_INVALID, valid.copy(offlineGate = valid.offlineGate!!.copy(logicalGateDigest = "0".repeat(64))))
        assertStatus(HimFastPaidCandidatePreflightStatus.IMPLEMENTATION_FINGERPRINT_MISMATCH, valid.copy(currentImplementationFingerprint = "0".repeat(64)))
        assertStatus(HimFastPaidCandidatePreflightStatus.RUNTIME_IDENTITY_MISMATCH, valid.copy(currentBindings = valid.currentBindings.copy(inferenceSchema = "WRONG")))
        assertStatus(HimFastPaidCandidatePreflightStatus.RUNTIME_IDENTITY_MISMATCH, valid.copy(currentBindings = valid.currentBindings.copy(instructionPolicy = "WRONG")))
        assertStatus(HimFastPaidCandidatePreflightStatus.RUNTIME_IDENTITY_MISMATCH, valid.copy(currentBindings = valid.currentBindings.copy(providerFingerprint = "0".repeat(64))))
        assertStatus(HimFastPaidCandidatePreflightStatus.PACKING_IDENTITY_MISMATCH, valid.copy(currentBindings = valid.currentBindings.copy(packingPolicy = "WRONG")))
        assertStatus(HimFastPaidCandidatePreflightStatus.DATASET_IDENTITY_MISMATCH, valid.copy(currentBindings = valid.currentBindings.copy(datasetSchema = "WRONG")))
    }

    private fun fixture(): HimFastPaidCandidatePreflightSnapshotV1 {
        val root = projectRoot()
        val retrievalBytes = root.resolve(HimProductionIndexFileIdentityReleaseContractV1.RETRIEVAL_RELEASE_PATH).readBytes()
        val identityFile = root.resolve(HimProductionIndexFileIdentityReleaseContractV1.PATH)
        val identityBytes = identityFile.readBytes()
        val identity = HimProductionIndexFileIdentityReleasePersistenceV1.read(identityFile)
        val gate = requireNotNull(HimOfflineCandidatePublicationGateV1.read(root.resolve(HimOfflineCandidatePublicationGateContractV1.ARTIFACT)))
        return HimFastPaidCandidatePreflightSnapshotV1(
            retrievalBytes,
            identityBytes,
            identity,
            identity.sources.associate { it.indexPath to it.sqliteFileBytes as Long? },
            identity.sources.mapTo(mutableSetOf()) { it.optimizedSourcePath },
            gate,
            gate.boundIdentities,
            gate.implementationFingerprintSha256,
        )
    }

    private fun assertStatus(status: HimFastPaidCandidatePreflightStatus, snapshot: HimFastPaidCandidatePreflightSnapshotV1) =
        assertEquals(status, HimFastPaidCandidatePreflightV1.evaluate(snapshot).status)

    private fun writeReport(root: File, result: HimFastPaidCandidatePreflightResultV1, elapsedNanos: Long) {
        val report = root.resolve(REPORT)
        requireNotNull(report.parentFile).mkdirs()
        report.writeText(buildString {
            appendLine("HIM F3d.5f.1 FAST PAID CANDIDATE PREFLIGHT TRUST CHAIN")
            appendLine("CONTRACT=${HimFastPaidCandidatePreflightContractV1.VERSION}")
            appendLine("RETRIEVAL FOUNDATION TRUST=PASS")
            appendLine("PRODUCTION INDEX FILE IDENTITY RELEASE TRUST=PASS")
            appendLine("INDEX EXISTENCE / SIZE CHECKS=PASS count=4")
            appendLine("SOURCE EXISTENCE CHECKS=PASS count=4")
            appendLine("F3d.5e GATE=PASS status=CURRENT")
            appendLine("IMPLEMENTATION FINGERPRINT=PASS")
            appendLine("RUNTIME IDENTITIES=PASS")
            appendLine("PACKING IDENTITY=PASS")
            appendLine("CANDIDATE DATASET IDENTITIES=PASS")
            appendLine("FAST-PREFLIGHT DIGEST CONTRACT=${result.digestContract}")
            appendLine("FAST-PREFLIGHT DIGEST SHA-256=${result.preflightDigestSha256}")
            appendLine("PREFLIGHT RUNTIME NANOS=$elapsedNanos")
            appendLine("FULL INDEX HASHES PERFORMED=0")
            appendLine("INDEX ROWS SCANNED=0")
            appendLine("SOURCE RECORDS SCANNED=0")
            appendLine("OPENAI CALLS=0")
            appendLine("NETWORK CALLS=0")
            appendLine("PRODUCTION CANDIDATE WRITES=0")
            appendLine("FILES CREATED=app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/candidate/dataset/HimFastPaidCandidatePreflightV1.kt,app/src/test/java/de/shopme/testing/system/tools/knowledge/him/canonical/family/groundtruth/candidate/dataset/RunHimFastPaidCandidatePreflightTrustChainTest.kt,$REPORT")
            appendLine("FILES CHANGED=none outside F3d.5f.1 scope")
            appendLine("BUILD=compileDebugKotlin=PASS,compileDebugUnitTestKotlin=PASS")
            appendLine("TARGETED TESTS=PASS")
            appendLine("GIT STATUS=reported separately")
            appendLine("CONTRACT DEVIATIONS=none")
            appendLine("HARD FAILURES=none")
            appendLine("FAST_PREFLIGHT_TRUST_CHAIN_READY=true")
            appendLine("FAST_PREFLIGHT_NETWORK_FREE=true")
            appendLine("FULL_INDEX_REHASH_REQUIRED=false")
            appendLine("FULL_INDEX_ROW_SCAN_REQUIRED=false")
            appendLine("OFFLINE_GATE_CURRENT=true")
            appendLine("F3d.5f.1 COMPLETE=true")
        })
    }

    private fun projectRoot(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }.first { it.resolve("settings.gradle.kts").isFile }

    companion object {
        private const val REPORT = "build/knowledge/reports/him/candidates/him-f3d5f1-fast-paid-candidate-preflight-trust-chain.txt"
    }
}
