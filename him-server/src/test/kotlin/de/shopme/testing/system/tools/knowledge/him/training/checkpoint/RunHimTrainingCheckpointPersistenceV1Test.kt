package de.shopme.testing.system.tools.knowledge.him.training.checkpoint

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.checkpoint.HimTrainingCheckpointContractV1
import de.shopme.tools.knowledge.him.training.checkpoint.HimTrainingCheckpointPersistenceV1
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimTrainingCheckpointPersistenceV1Test {
    @Test fun contractIdentityIsV1() {
        assertEquals("HIM_TRAINING_CHECKPOINT_V1", HimTrainingCheckpointContractV1.CONTRACT_ID)
        assertEquals("1", HimTrainingCheckpointContractV1.VERSION)
        assertEquals("TRAINING_CHECKPOINT_VALIDATED", HimTrainingCheckpointContractV1.STATE)
    }

    @Test fun modelAndOptimizerFormatsAreExplicit() {
        assertEquals("PYTORCH_SAFE_TENSOR_STATE_V1", HimTrainingCheckpointContractV1.MODEL_STATE_FORMAT)
        assertEquals("PYTORCH_COMPATIBLE_OPTIMIZER_STATE_V1", HimTrainingCheckpointContractV1.OPTIMIZER_STATE_FORMAT)
    }

    @Test fun digestIsDeterministic() {
        assertEquals(manifest().checkpointDigest, manifest().checkpointDigest)
    }

    @Test fun digestChangesWhenBindingChanges() {
        assertNotEquals(manifest().checkpointDigest, manifest(binding = binding(seed = 8)).checkpointDigest)
    }

    @Test fun manifestBindsAllFrozenAuthorityFields() {
        val value = manifest().binding
        assertEquals("productive:v1:${"a".repeat(64)}", value.productiveRequestReference)
        assertEquals("FacebookAI/xlm-roberta-base", value.baseModelReference)
        assertEquals("e73636d4f797dec63c3081bb6ed5c7b0bb3f2089", value.baseModelRevision)
        assertEquals("xlm-roberta-base-tokenizer", value.tokenizerId)
        assertEquals("CUDA:0", value.cudaDeviceAuthority)
        assertEquals(7, value.seed)
        assertEquals(3, value.completedOptimizerSteps)
    }

    @Test fun localRoundTripPreservesBothOpaqueStates() = withCheckpoint { request ->
        val created = assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        val reloaded = HimTrainingCheckpointPersistenceV1.load(request.checkpointDirectory, request.binding)
        assertArrayEquals(request.modelStateBytes, reloaded.modelStateBytes)
        assertArrayEquals(request.optimizerStateBytes, reloaded.optimizerStateBytes)
        assertEquals(created.manifest, reloaded.manifest)
        assertEquals(3, reloaded.manifest.binding.completedOptimizerSteps)
    }

    @Test fun identicalSecondWriteIsIdempotent() = withCheckpoint { request ->
        assertEquals(
            HimTrainingCheckpointPersistenceV1.PersistenceStatusV1.CREATED,
            assertCreated(HimTrainingCheckpointPersistenceV1.execute(request)).status,
        )
        assertEquals(
            HimTrainingCheckpointPersistenceV1.PersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
            assertCreated(HimTrainingCheckpointPersistenceV1.execute(request)).status,
        )
    }

    @Test fun wrongCheckpointDigestFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        val file = request.checkpointDirectory.resolve("checkpoint-manifest.v1.json").toPath()
        val text = Files.readString(file)
        Files.writeString(file, text.replace(requestDigest(request), "0".repeat(64)))
        expectLoadFailure(request)
    }

    @Test fun wrongProductiveRequestDigestFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        expectLoadFailure(request.copy(binding = binding(productive = "b".repeat(64))))
    }

    @Test fun wrongManifestShaFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        expectLoadFailure(request.copy(binding = binding(manifest = "e".repeat(64))))
    }

    @Test fun wrongBaseModelDigestFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        expectLoadFailure(request.copy(binding = binding(modelWeights = "b".repeat(64))))
    }

    @Test fun wrongTokenizerIdFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        expectLoadFailure(request.copy(binding = binding(tokenizer = "other-tokenizer")))
    }

    @Test fun wrongCorpusIdentityFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        expectLoadFailure(request.copy(binding = binding(corpus = "corpus:v1:other")))
    }

    @Test fun wrongPartitionIdentityFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        expectLoadFailure(request.copy(binding = binding(partition = "partition:v1:other")))
    }

    @Test fun wrongSeedFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        expectLoadFailure(request.copy(binding = binding(seed = 8)))
    }

    @Test fun wrongCompletedStepCountFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        expectLoadFailure(request.copy(binding = binding(steps = 2)))
    }

    @Test fun missingModelStateFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        request.checkpointDirectory.resolve("model-state.bin").delete()
        expectLoadFailure(request)
    }

    @Test fun modifiedModelStateFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        val path = request.checkpointDirectory.resolve("model-state.bin").toPath()
        Files.write(path, Files.readAllBytes(path) + byteArrayOf(0x01))
        expectLoadFailure(request)
    }

    @Test fun missingOptimizerStateFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        request.checkpointDirectory.resolve("optimizer-state.bin").delete()
        expectLoadFailure(request)
    }

    @Test fun modifiedOptimizerStateFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        val path = request.checkpointDirectory.resolve("optimizer-state.bin").toPath()
        Files.write(path, Files.readAllBytes(path) + byteArrayOf(0x02))
        expectLoadFailure(request)
    }

    @Test fun unsupportedVersionFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        val path = request.checkpointDirectory.resolve("checkpoint-manifest.v1.json").toPath()
        Files.writeString(path, Files.readString(path).replace("\"version\":\"1\"", "\"version\":\"2\""))
        expectLoadFailure(request)
    }

    @Test fun partialCheckpointFailsClosed() = withTempRoot { root ->
        val directory = root.resolve("checkpoint").toFile()
        directory.mkdirs()
        Files.write(directory.toPath().resolve("model-state.bin"), byteArrayOf(1))
        expectLoadFailure(request(directory))
    }

    @Test fun conflictingExistingFinalCheckpointFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        val result = HimTrainingCheckpointPersistenceV1.execute(request.copy(modelStateBytes = byteArrayOf(9, 9, 9)))
        assertEquals(HimTrainingCheckpointPersistenceV1.FailureReasonV1.EXISTING_CHECKPOINT_CONFLICT, (result as HimTrainingCheckpointPersistenceV1.ResultV1.Failed).reason)
    }

    @Test fun timestampAndAbsolutePathAreAbsentFromDigestInput() {
        val payload = HimTrainingCheckpointContractV1.canonicalDigestPayload(binding(), artifact("model-state.bin"), artifact("optimizer-state.bin"))
        assertTrue("timestamp" !in payload.lowercase())
        assertTrue("/Users/" !in payload)
        assertTrue("/workspace/" !in payload)
    }

    @Test fun atomicPolicyIsExplicit() {
        assertEquals("TEMPORARY_SIBLING_FSYNC_VERIFY_ATOMIC_PROMOTION", HimTrainingCheckpointContractV1.ATOMIC_PERSISTENCE_POLICY)
        assertEquals("FAIL_CLOSED", HimTrainingCheckpointContractV1.OVERWRITE_POLICY)
    }

    @Test fun zeroCompletedStepsAreRejectedForPersistence() = withTempRoot { root ->
        val result = HimTrainingCheckpointPersistenceV1.execute(request(root.resolve("checkpoint").toFile(), binding(steps = 0)))
        assertEquals(HimTrainingCheckpointPersistenceV1.FailureReasonV1.INVALID_REQUEST, (result as HimTrainingCheckpointPersistenceV1.ResultV1.Failed).reason)
    }

    @Test fun wrongPoint9IdentityFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        expectLoadFailure(request.copy(binding = binding(point9 = "point9:v1:other")))
    }

    @Test fun wrongPoint13IdentityFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        expectLoadFailure(request.copy(binding = binding(point13 = "point13:v1:other")))
    }

    @Test fun wrongNumericalPolicyFailsClosed() = withCheckpoint { request ->
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request))
        expectLoadFailure(request.copy(binding = binding(numerical = "optimizer-policy:v1:other")))
    }

    private fun manifest(
        binding: HimTrainingCheckpointContractV1.BindingV1 = binding(),
    ) = HimTrainingCheckpointContractV1.createManifest(binding, artifact("model-state.bin"), artifact("optimizer-state.bin"))

    private fun artifact(path: String) = HimTrainingCheckpointContractV1.ArtifactV1(
        path,
        3,
        HimSha256("c".repeat(64)),
    )

    private fun binding(
        productive: String = "a".repeat(64),
        manifest: String = "b".repeat(64),
        modelWeights: String = "c".repeat(64),
        tokenizer: String = "xlm-roberta-base-tokenizer",
        corpus: String = "corpus:v1:61fe8ce3",
        partition: String = "partition:v1:62dc7471",
        point9: String = "point9:v1:conditioning",
        point13: String = "point13:v1:numerical",
        seed: Long = 7,
        numerical: String = "optimizer-policy:v1:9f4858",
        steps: Int = 3,
    ) = HimTrainingCheckpointContractV1.BindingV1(
        HimSha256(productive),
        "productive:v1:$productive",
        HimSha256(manifest),
        "FacebookAI/xlm-roberta-base",
        "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089",
        HimSha256(modelWeights),
        HimSha256("d".repeat(64)),
        tokenizer,
        corpus,
        partition,
        point9,
        point13,
        "CUDA:0",
        seed,
        numerical,
        steps,
        "controlled-real-training:v1:run",
    )

    private fun request(directory: java.io.File, value: HimTrainingCheckpointContractV1.BindingV1 = binding()) =
        HimTrainingCheckpointPersistenceV1.RequestV1(directory, value, byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))

    private fun requestDigest(request: HimTrainingCheckpointPersistenceV1.RequestV1): String =
        assertCreated(HimTrainingCheckpointPersistenceV1.execute(request)).manifest.checkpointDigest.value

    private fun assertCreated(result: HimTrainingCheckpointPersistenceV1.ResultV1): HimTrainingCheckpointPersistenceV1.PersistedV1 {
        assertTrue("Unexpected checkpoint result: $result", result is HimTrainingCheckpointPersistenceV1.ResultV1.Completed)
        return (result as HimTrainingCheckpointPersistenceV1.ResultV1.Completed).value
    }

    private fun expectLoadFailure(request: HimTrainingCheckpointPersistenceV1.RequestV1) {
        try {
            HimTrainingCheckpointPersistenceV1.load(request.checkpointDirectory, request.binding)
            throw AssertionError("Expected checkpoint reload failure")
        } catch (_: HimTrainingCheckpointPersistenceV1.PersistenceFailure) {
            // Expected fail-closed result.
        }
    }

    private fun withCheckpoint(block: (HimTrainingCheckpointPersistenceV1.RequestV1) -> Unit) = withTempRoot { root ->
        block(request(root.resolve("checkpoint").toFile()))
    }

    private fun withTempRoot(block: (java.nio.file.Path) -> Unit) {
        val root = Files.createTempDirectory("him-checkpoint-")
        try {
            block(root)
        } finally {
            Files.walk(root).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
