package de.shopme.tools.knowledge.him.training.checkpoint

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.Gson
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/** Host-side atomic persistence and fail-closed reload for one P1 checkpoint. */
object HimTrainingCheckpointPersistenceV1 {
    enum class PersistenceStatusV1 {
        CREATED,
        ALREADY_PRESENT_IDENTICAL,
    }

    enum class FailureReasonV1 {
        INVALID_REQUEST,
        UNSAFE_PATH,
        ARTIFACT_MISSING,
        MALFORMED_MANIFEST,
        EXISTING_CHECKPOINT_CONFLICT,
        CHECKPOINT_VERSION_UNSUPPORTED,
        CHECKPOINT_DIGEST_MISMATCH,
        ARTIFACT_DIGEST_MISMATCH,
        BINDING_MISMATCH,
        COMPLETED_STEP_COUNT_MISMATCH,
        ATOMIC_PUBLICATION_FAILED,
        RELOAD_VALIDATION_FAILED,
    }

    class PersistenceFailure(
        val reason: FailureReasonV1,
        val safeContext: String,
    ) : IllegalArgumentException("${reason.name}:$safeContext")

    data class RequestV1(
        val checkpointDirectory: File,
        val binding: HimTrainingCheckpointContractV1.BindingV1,
        val modelStateBytes: ByteArray,
        val optimizerStateBytes: ByteArray,
    )

    data class PersistedV1(
        val status: PersistenceStatusV1,
        val checkpointDirectory: File,
        val manifest: HimTrainingCheckpointContractV1.ManifestV1,
        val manifestBytes: ByteArray,
        val modelStateBytes: ByteArray,
        val optimizerStateBytes: ByteArray,
    )

    data class ReloadedV1(
        val checkpointDirectory: File,
        val manifest: HimTrainingCheckpointContractV1.ManifestV1,
        val manifestBytes: ByteArray,
        val modelStateBytes: ByteArray,
        val optimizerStateBytes: ByteArray,
    )

    sealed interface ResultV1 {
        data class Completed(val value: PersistedV1) : ResultV1

        data class Failed(
            val reason: FailureReasonV1,
            val safeContext: String,
        ) : ResultV1
    }

    fun execute(request: RequestV1): ResultV1 {
        return try {
        val prepared = prepare(request)
        val target = safeTarget(request.checkpointDirectory)
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(target)) {
                throw PersistenceFailure(FailureReasonV1.UNSAFE_PATH, "checkpoint")
            }
            val existing = try {
                load(request.checkpointDirectory, request.binding)
            } catch (_: PersistenceFailure) {
                throw PersistenceFailure(FailureReasonV1.EXISTING_CHECKPOINT_CONFLICT, "checkpoint")
            }
            if (
                existing.manifest == prepared.manifest &&
                existing.modelStateBytes.contentEquals(request.modelStateBytes) &&
                existing.optimizerStateBytes.contentEquals(request.optimizerStateBytes)
            ) {
                return ResultV1.Completed(
                    PersistedV1(
                        PersistenceStatusV1.ALREADY_PRESENT_IDENTICAL,
                        request.checkpointDirectory,
                        existing.manifest,
                        existing.manifestBytes,
                        existing.modelStateBytes,
                        existing.optimizerStateBytes,
                    ),
                )
            }
            throw PersistenceFailure(FailureReasonV1.EXISTING_CHECKPOINT_CONFLICT, "checkpoint")
        }

        val parent = requireNotNull(target.parent)
        ensureSafeParent(parent)
        Files.createDirectories(parent)
        val temporary = Files.createTempDirectory(parent, ".${target.fileName}.")
        try {
            writeDurably(temporary.resolve(HimTrainingCheckpointContractV1.MODEL_STATE_FILE_NAME), request.modelStateBytes)
            writeDurably(temporary.resolve(HimTrainingCheckpointContractV1.OPTIMIZER_STATE_FILE_NAME), request.optimizerStateBytes)
            val manifestBytes = serializeManifest(prepared.manifest)
            writeDurably(temporary.resolve(HimTrainingCheckpointContractV1.MANIFEST_FILE_NAME), manifestBytes)
            val verified = load(request.checkpointDirectory.copyWithPath(temporary), request.binding)
            require(verified.manifest == prepared.manifest)
            require(verified.modelStateBytes.contentEquals(request.modelStateBytes))
            require(verified.optimizerStateBytes.contentEquals(request.optimizerStateBytes))
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target)
            }
            forceDirectory(parent)
            val reloaded = load(request.checkpointDirectory, request.binding)
            ResultV1.Completed(
                PersistedV1(
                    PersistenceStatusV1.CREATED,
                    request.checkpointDirectory,
                    reloaded.manifest,
                    reloaded.manifestBytes,
                    reloaded.modelStateBytes,
                    reloaded.optimizerStateBytes,
                ),
            )
        } catch (_: FileAlreadyExistsException) {
            throw PersistenceFailure(FailureReasonV1.EXISTING_CHECKPOINT_CONFLICT, "checkpoint")
        } finally {
            if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
                deleteTree(temporary)
            }
        }
    } catch (failure: PersistenceFailure) {
        ResultV1.Failed(failure.reason, failure.safeContext)
    } catch (_: IllegalArgumentException) {
        ResultV1.Failed(FailureReasonV1.INVALID_REQUEST, "request")
    } catch (_: Throwable) {
        ResultV1.Failed(FailureReasonV1.ATOMIC_PUBLICATION_FAILED, "checkpoint")
        }
    }

    fun load(
        checkpointDirectory: File,
        expectedBinding: HimTrainingCheckpointContractV1.BindingV1,
    ): ReloadedV1 {
        return try {
            val directory = safeTarget(checkpointDirectory)
            if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory)) {
                throw PersistenceFailure(FailureReasonV1.ARTIFACT_MISSING, "checkpoint")
            }
            val manifestPath = regularFile(directory, HimTrainingCheckpointContractV1.MANIFEST_FILE_NAME)
            val modelPath = regularFile(directory, HimTrainingCheckpointContractV1.MODEL_STATE_FILE_NAME)
            val optimizerPath = regularFile(directory, HimTrainingCheckpointContractV1.OPTIMIZER_STATE_FILE_NAME)
            val manifestBytes = Files.readAllBytes(manifestPath)
            val manifest = deserializeManifest(manifestBytes)
            if (manifest.binding != expectedBinding) {
                throw PersistenceFailure(FailureReasonV1.BINDING_MISMATCH, "binding")
            }
            if (manifest.binding.completedOptimizerSteps != expectedBinding.completedOptimizerSteps) {
                throw PersistenceFailure(FailureReasonV1.COMPLETED_STEP_COUNT_MISMATCH, "steps")
            }
            val modelBytes = Files.readAllBytes(modelPath)
            val optimizerBytes = Files.readAllBytes(optimizerPath)
            verifyArtifact(manifest.modelState, modelBytes, HimTrainingCheckpointContractV1.MODEL_STATE_FILE_NAME)
            verifyArtifact(manifest.optimizerState, optimizerBytes, HimTrainingCheckpointContractV1.OPTIMIZER_STATE_FILE_NAME)
            if (manifest.checkpointDigest != HimTrainingCheckpointContractV1.digestFor(manifest.binding, manifest.modelState, manifest.optimizerState)) {
                throw PersistenceFailure(FailureReasonV1.CHECKPOINT_DIGEST_MISMATCH, "checkpoint")
            }
            ReloadedV1(checkpointDirectory, manifest, manifestBytes, modelBytes, optimizerBytes)
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReasonV1.RELOAD_VALIDATION_FAILED, "checkpoint")
        }
    }

    fun serializeManifest(manifest: HimTrainingCheckpointContractV1.ManifestV1): ByteArray {
        val json = JsonObject()
        json.addProperty("contractId", manifest.contractId)
        json.addProperty("version", manifest.version)
        json.addProperty("state", manifest.state)
        json.addProperty("productiveRequestDigest", manifest.binding.productiveRequestDigest.value)
        json.addProperty("productiveRequestReference", manifest.binding.productiveRequestReference)
        json.addProperty("durableManifestSha256", manifest.binding.durableManifestSha256.value)
        json.addProperty("baseModelReference", manifest.binding.baseModelReference)
        json.addProperty("baseModelRevision", manifest.binding.baseModelRevision)
        json.addProperty("baseModelWeightsSha256", manifest.binding.baseModelWeightsSha256.value)
        json.addProperty("modelBindingDigest", manifest.binding.modelBindingDigest.value)
        json.addProperty("tokenizerId", manifest.binding.tokenizerId)
        json.addProperty("corpusIdentity", manifest.binding.corpusIdentity)
        json.addProperty("partitionIdentity", manifest.binding.partitionIdentity)
        json.addProperty("point9Identity", manifest.binding.point9Identity)
        json.addProperty("point13Identity", manifest.binding.point13Identity)
        json.addProperty("cudaDeviceAuthority", manifest.binding.cudaDeviceAuthority)
        json.addProperty("seed", manifest.binding.seed)
        json.addProperty("numericalPolicyIdentity", manifest.binding.numericalPolicyIdentity)
        json.addProperty("completedOptimizerSteps", manifest.binding.completedOptimizerSteps)
        json.addProperty("trainingRunId", manifest.binding.trainingRunId)
        json.addProperty("modelStatePath", manifest.modelState.relativePath)
        json.addProperty("modelStateByteCount", manifest.modelState.byteCount)
        json.addProperty("modelStateSha256", manifest.modelState.sha256.value)
        json.addProperty("optimizerStatePath", manifest.optimizerState.relativePath)
        json.addProperty("optimizerStateByteCount", manifest.optimizerState.byteCount)
        json.addProperty("optimizerStateSha256", manifest.optimizerState.sha256.value)
        json.addProperty("checkpointDigest", manifest.checkpointDigest.value)
        return (Gson().toJson(json) + "\n").toByteArray(StandardCharsets.UTF_8)
    }

    private fun prepare(request: RequestV1): PreparedV1 {
        require(request.checkpointDirectory.path.isNotBlank())
        require(request.binding.completedOptimizerSteps > 0)
        val model = HimTrainingCheckpointContractV1.ArtifactV1(
            HimTrainingCheckpointContractV1.MODEL_STATE_FILE_NAME,
            request.modelStateBytes.size.toLong(),
            sha256(request.modelStateBytes),
        )
        val optimizer = HimTrainingCheckpointContractV1.ArtifactV1(
            HimTrainingCheckpointContractV1.OPTIMIZER_STATE_FILE_NAME,
            request.optimizerStateBytes.size.toLong(),
            sha256(request.optimizerStateBytes),
        )
        return PreparedV1(
            HimTrainingCheckpointContractV1.createManifest(request.binding, model, optimizer),
        )
    }

    private data class PreparedV1(val manifest: HimTrainingCheckpointContractV1.ManifestV1)

    private fun deserializeManifest(bytes: ByteArray): HimTrainingCheckpointContractV1.ManifestV1 {
        val root = try {
            JsonParser.parseString(bytes.toString(StandardCharsets.UTF_8)).asJsonObject
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReasonV1.MALFORMED_MANIFEST, "manifest")
        }
        val version = value(root, "version")
        if (version != HimTrainingCheckpointContractV1.VERSION) {
            throw PersistenceFailure(FailureReasonV1.CHECKPOINT_VERSION_UNSUPPORTED, "version")
        }
        return try {
            val binding = HimTrainingCheckpointContractV1.BindingV1(
                HimSha256(value(root, "productiveRequestDigest")),
                value(root, "productiveRequestReference"),
                HimSha256(value(root, "durableManifestSha256")),
                value(root, "baseModelReference"),
                value(root, "baseModelRevision"),
                HimSha256(value(root, "baseModelWeightsSha256")),
                HimSha256(value(root, "modelBindingDigest")),
                value(root, "tokenizerId"),
                value(root, "corpusIdentity"),
                value(root, "partitionIdentity"),
                value(root, "point9Identity"),
                value(root, "point13Identity"),
                value(root, "cudaDeviceAuthority"),
                root.get("seed").asLong,
                value(root, "numericalPolicyIdentity"),
                root.get("completedOptimizerSteps").asInt,
                value(root, "trainingRunId"),
            )
            val model = HimTrainingCheckpointContractV1.ArtifactV1(
                value(root, "modelStatePath"),
                root.get("modelStateByteCount").asLong,
                HimSha256(value(root, "modelStateSha256")),
            )
            val optimizer = HimTrainingCheckpointContractV1.ArtifactV1(
                value(root, "optimizerStatePath"),
                root.get("optimizerStateByteCount").asLong,
                HimSha256(value(root, "optimizerStateSha256")),
            )
            HimTrainingCheckpointContractV1.ManifestV1(
                value(root, "contractId"),
                version,
                value(root, "state"),
                binding,
                model,
                optimizer,
                HimSha256(value(root, "checkpointDigest")),
            )
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReasonV1.MALFORMED_MANIFEST, "manifest")
        }
    }

    private fun value(root: JsonObject, name: String): String =
        root.get(name)?.takeUnless { it.isJsonNull }?.asString
            ?: throw PersistenceFailure(FailureReasonV1.MALFORMED_MANIFEST, name)

    private fun verifyArtifact(
        artifact: HimTrainingCheckpointContractV1.ArtifactV1,
        bytes: ByteArray,
        expectedPath: String,
    ) {
        if (artifact.relativePath != expectedPath || artifact.byteCount != bytes.size.toLong()) {
            throw PersistenceFailure(FailureReasonV1.ARTIFACT_DIGEST_MISMATCH, expectedPath)
        }
        if (artifact.sha256 != sha256(bytes)) {
            throw PersistenceFailure(FailureReasonV1.ARTIFACT_DIGEST_MISMATCH, expectedPath)
        }
    }

    private fun safeTarget(directory: File): Path {
        val target = directory.canonicalFile.toPath().normalize()
        if (target.fileName == null || target.fileName.toString().isBlank()) {
            throw PersistenceFailure(FailureReasonV1.UNSAFE_PATH, "checkpoint")
        }
        return target
    }

    private fun ensureSafeParent(parent: Path) {
        if (Files.exists(parent, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(parent)) {
            throw PersistenceFailure(FailureReasonV1.UNSAFE_PATH, "parent")
        }
    }

    private fun regularFile(directory: Path, name: String): Path {
        val path = directory.resolve(name)
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw PersistenceFailure(FailureReasonV1.ARTIFACT_MISSING, name)
        }
        return path
    }

    private fun writeDurably(path: Path, bytes: ByteArray) {
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        FileOutputStream(path.toFile(), true).use { it.fd.sync() }
    }

    private fun forceDirectory(directory: Path) {
        try {
            java.nio.channels.FileChannel.open(directory, StandardOpenOption.READ).use { it.force(true) }
        } catch (_: UnsupportedOperationException) {
            // Directory fsync is not available on every supported filesystem.
        }
    }

    private fun deleteTree(root: Path) {
        Files.walk(root).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }

    private fun sha256(bytes: ByteArray): HimSha256 = HimSha256(
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    private fun File.copyWithPath(path: Path): File = path.toFile()
}
