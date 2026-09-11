package de.shopme.testing.system.tools.knowledge.him.training.v2

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.training.v2.HimP1ProductiveTrainingV2AuthorityV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class RunHimP1ProductiveTrainingV2AuthorityV1Test {
    companion object {
        private lateinit var materialization: HimP1ProductiveTrainingV2AuthorityV1.Materialization
        private lateinit var repositoryRoot: File

        @JvmStatic
        @BeforeClass
        fun materializeOnce() {
            repositoryRoot = generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
                .first { it.resolve("gradlew").isFile }
            materialization = HimP1ProductiveTrainingV2AuthorityV1.materialize(repositoryRoot)
        }
    }

    private fun tempJson(source: File, mutate: (com.google.gson.JsonObject) -> Unit): File {
        val json = JsonParser.parseString(Files.readString(source.toPath(), StandardCharsets.UTF_8)).asJsonObject
        mutate(json)
        val target = Files.createTempFile("him-p1-v2-", ".json").toFile()
        Files.writeString(target.toPath(), json.toString() + "\n", StandardCharsets.UTF_8)
        return target
    }

    private fun invalidRequest(mutate: (com.google.gson.JsonObject) -> Unit) {
        val file = tempJson(materialization.requestPath, mutate)
        assertThrows(IllegalArgumentException::class.java) { HimP1ProductiveTrainingV2AuthorityV1.validateRequest(file) }
    }

    private fun invalidManifest(mutate: (com.google.gson.JsonObject) -> Unit) {
        val file = tempJson(materialization.manifestPath, mutate)
        assertThrows(IllegalArgumentException::class.java) {
            HimP1ProductiveTrainingV2AuthorityV1.validateManifest(file, materialization.requestDigest)
        }
    }

    private fun invalidReadiness(mutate: (com.google.gson.JsonObject) -> Unit) {
        val file = tempJson(materialization.readinessPath, mutate)
        val manifest = JsonParser.parseString(Files.readString(materialization.manifestPath.toPath(), StandardCharsets.UTF_8)).asJsonObject
        assertThrows(IllegalArgumentException::class.java) {
            HimP1ProductiveTrainingV2AuthorityV1.validateReadiness(
                file,
                materialization.requestDigest,
                manifest.get("logicalDigest").asString,
            )
        }
    }

    @Test
    fun exactFrozenAuthoritiesAreAccepted() {
        assertEquals(27, materialization.packetFileCount)
        assertTrue(materialization.packetTotalBytes > 1_100_000_000L)
        HimP1ProductiveTrainingV2AuthorityV1.validateAll(repositoryRoot)
    }

    @Test
    fun exactCorpusPartitionAndReadinessAreAccepted() {
        assertEquals(HimP1ProductiveTrainingV2AuthorityV1.CORPUS_PHYSICAL_DIGEST, sha(materialization.packetRoot.resolve("dataset/corpus.v2.json")))
        assertEquals(HimP1ProductiveTrainingV2AuthorityV1.PARTITION_PHYSICAL_DIGEST, sha(materialization.packetRoot.resolve("dataset/partition.v2.json")))
        assertEquals(HimP1ProductiveTrainingV2AuthorityV1.EVALUATION_READINESS_PHYSICAL_DIGEST, sha(materialization.packetRoot.resolve("readiness/evaluation-readiness.v2.json")))
        assertNotNull(materialization.readinessPath)
        assertEquals(64, JsonParser.parseString(Files.readString(materialization.inventoryPath.toPath(), StandardCharsets.UTF_8)).asJsonObject.get("logicalDigest").asString.length)
    }

    @Test
    fun modelTokenizerRuntimeDeviceConfigurationAndCheckpointAuthoritiesAreAccepted() {
        val request = JsonParser.parseString(Files.readString(materialization.requestPath.toPath(), StandardCharsets.UTF_8)).asJsonObject
        assertEquals(HimP1ProductiveTrainingV2AuthorityV1.MODEL_REVISION, request.get("modelRevision").asString)
        assertEquals(HimP1ProductiveTrainingV2AuthorityV1.TOKENIZER_ID, request.get("tokenizerId").asString)
        assertEquals(HimP1ProductiveTrainingV2AuthorityV1.RUNTIME_IMAGE, request.get("runtimeImage").asString)
        assertEquals("cuda:0", request.get("cudaDevice").asString)
        assertEquals(12, request.get("trainingSteps").asInt)
        assertEquals(1, request.get("checkpointContractVersion").asInt)
    }

    @Test fun wrongCorpusV2DigestIsRejected() = invalidRequest { it.addProperty("corpusLogicalDigest", "0".repeat(64)) }
    @Test fun wrongPartitionV2DigestIsRejected() = invalidRequest { it.addProperty("partitionLogicalDigest", "0".repeat(64)) }
    @Test fun wrongEvaluationReadinessDigestIsRejected() = invalidRequest { it.addProperty("evaluationReadinessLogicalDigest", "0".repeat(64)) }
    @Test fun wrongModelShaIsRejected() = invalidRequest { it.addProperty("modelWeightsSha256", "0".repeat(64)) }
    @Test fun wrongModelSizeIsRejected() = invalidRequest { it.addProperty("modelWeightsByteCount", 1) }
    @Test fun wrongModelRevisionIsRejected() = invalidRequest { it.addProperty("modelRevision", "stale") }
    @Test fun wrongTokenizerIdIsRejected() = invalidRequest { it.addProperty("tokenizerId", "wrong-tokenizer") }
    @Test fun wrongTokenizerArtifactIsRejected() = invalidRequest { it.addProperty("tokenizerArtifactSha256", "0".repeat(64)) }
    @Test fun wrongRuntimeOciDigestIsRejected() = invalidRequest { it.addProperty("runtimeImage", "ghcr.io/example@sha256:${"0".repeat(64)}") }
    @Test fun wrongCudaDeviceIsRejected() = invalidRequest { it.addProperty("cudaDevice", "cuda:1") }
    @Test fun staleV1RequestBindingIsRejected() = invalidRequest { it.addProperty("contractId", "HIM_P1_PRODUCTIVE_TRAINING_REQUEST_V1") }
    @Test fun emptyValidationIsRejected() = invalidRequest { it.addProperty("validationCount", 0) }
    @Test fun leakageNonZeroIsRejected() = invalidRequest { it.addProperty("leakageCount", 1) }
    @Test fun wrongTrainingConfigurationIsRejected() = invalidRequest { it.addProperty("trainingSteps", 3) }
    @Test fun undefinedTrajectoryIsRejected() = invalidRequest { it.addProperty("epochs", 0) }
    @Test fun holdoutEnabledForTrainingIsRejected() = invalidRequest { it.addProperty("holdoutTrainingEnabled", true) }
    @Test fun missingCheckpointAuthorityIsRejected() = invalidRequest { it.remove("checkpointContractVersion") }
    @Test fun missingTrainerSourceIsRejected() = invalidManifest { it.addProperty("trainerSourceCount", 15) }
    @Test fun wrongTransferPacketFileIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { HimP1ProductiveTrainingV2AuthorityV1.validateTransfer(materialization.copy(packetFileCount = 26)) }
    }
    @Test fun packetByteMismatchIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { HimP1ProductiveTrainingV2AuthorityV1.validateTransfer(materialization.copy(packetTotalBytes = materialization.packetTotalBytes + 1)) }
    }
    @Test fun transferDigestMismatchIsRejected() = invalidReadiness { it.addProperty("manifestDigest", "0".repeat(64)) }
    @Test fun missingModelArtifactIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { HimP1ProductiveTrainingV2AuthorityV1.validateTransfer(materialization.copy(packetRoot = File("missing-model-packet"))) }
    }
    @Test fun wrongRemotePacketRootIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { HimP1ProductiveTrainingV2AuthorityV1.validateTransfer(materialization.copy(packetRoot = File("/Users/logfather/AndroidStudioProjects/ShopMe/training/him/output"))) }
    }
    @Test fun hardcodedLocalMacPathIsRejected() {
        assertFalse(HimP1ProductiveTrainingV2AuthorityV1.TRAINING_OUTPUT_ROOT.contains("/Users/"))
        assertFalse(HimP1ProductiveTrainingV2AuthorityV1.TRAINING_OUTPUT_ROOT.contains("AndroidStudioProjects"))
    }
    @Test fun requestManifestMismatchIsRejected() = invalidManifest { it.addProperty("productiveRequestDigest", "0".repeat(64)) }

    private fun sha(file: File): String = java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath())).joinToString("") { "%02x".format(it) }
}
