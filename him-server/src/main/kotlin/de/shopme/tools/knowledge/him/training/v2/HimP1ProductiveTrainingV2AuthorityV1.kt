package de.shopme.tools.knowledge.him.training.v2

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Server-owned, file-backed authority for the frozen P1 Productive Training V2 run.
 * It binds immutable inputs and prepares an A100 packet; it never loads a model or
 * starts training.
 */
object HimP1ProductiveTrainingV2AuthorityV1 {
    const val REQUEST_CONTRACT_ID = "HIM_P1_PRODUCTIVE_TRAINING_REQUEST_V2"
    const val REQUEST_VERSION = 2
    const val REQUEST_STATE = "PRODUCTIVE_TRAINING_REQUEST_V2_AUTHORIZED"
    const val MANIFEST_CONTRACT_ID = "HIM_P1_PRODUCTIVE_TRAINING_MANIFEST_V2"
    const val MANIFEST_VERSION = 2
    const val MANIFEST_STATE = "PRODUCTIVE_TRAINING_MANIFEST_V2_PERSISTED"
    const val READINESS_CONTRACT_ID = "HIM_P1_PRODUCTIVE_TRAINING_READINESS_V2"
    const val READINESS_VERSION = 2
    const val READINESS_STATE = "READY"

    const val CORPUS_LOGICAL_DIGEST = "487eb3389e0e12bff3f009d4cfb7edb4fb78192f7a2e8c7f96a4782e27ae5f03"
    const val CORPUS_PHYSICAL_DIGEST = "a25450391fb813ee9b94fe945a0847105f4237a932c5e422b6f6c6afbaa4ecee"
    const val PARTITION_LOGICAL_DIGEST = "b5efda60aea6aae92164df8cbcd59ce78f34214eca0f0a4fdf94adbb82688b46"
    const val PARTITION_PHYSICAL_DIGEST = "bafbb046308088f848b39d0f15ece45b874e3178e54d65c9e0f1d3443ecc7f41"
    const val EVALUATION_READINESS_LOGICAL_DIGEST = "19f8eb29c2d8b651cb378614d0632216836b07f23b26cd2eee4a8484d373af3d"
    const val EVALUATION_READINESS_PHYSICAL_DIGEST = "419fab00ba9394ffabe7ed9b808c31f62e924b8387ef40e93521856ab013efbe"
    const val MODEL_REVISION = "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
    const val MODEL_ID = "FacebookAI/xlm-roberta-base"
    const val MODEL_WEIGHTS_SHA256 = "6fd4797bc397c3b8b55d6bb5740366b57e6a3ce91c04c77f22aafc0c128e6feb"
    const val MODEL_BINDING_DIGEST = "f9def079a6c046b2e4a11288a7b141185872907bd956b444b81fd0ab56afaaf5"
    const val TOKENIZER_ID = "xlm-roberta-base-tokenizer"
    const val TOKENIZER_ARTIFACT_SHA256 = "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"
    const val RUNTIME_IMAGE = "ghcr.io/logfather/him-a100-reference-runtime@sha256:9c8bd248de40b13b5275c7afcc20fc0b365c6f73f3b86d3edd9a1232d2894edb"
    const val CUDA_DEVICE = "cuda:0"
    const val TRAINING_OUTPUT_ROOT = "training/him/output/p1-v2/<productive-request-digest>"
    const val TRAINING_STEPS = 12
    const val TRAINING_EPOCHS = 3
    const val TRAINING_SEED = 7L
    const val TRAINING_MICRO_BATCH_SIZE = 8
    const val TRAINING_GRADIENT_ACCUMULATION_STEPS = 1
    const val TRAINING_LEARNING_RATE = "0.0001"
    const val CHECKPOINT_CONTRACT_VERSION = 1

    private const val CORPUS_PATH = "data/knowledge/him/training/evaluation/v2/p1-evaluation-dataset-v2/corpus.v2.json"
    private const val PARTITION_PATH = "data/knowledge/him/training/evaluation/v2/p1-evaluation-dataset-v2/partition.v2.json"
    private const val EVALUATION_READINESS_PATH = "data/knowledge/him/training/evaluation/v2/p1-evaluation-dataset-v2/evaluation-readiness.v2.json"
    private const val MODEL_ROOT = "training/him/models/xlm-roberta-base/$MODEL_REVISION"
    private const val REQUEST_FILE = "productive-training-request.v2.json"
    private const val MANIFEST_FILE = "productive-training-manifest.v2.json"
    private const val READINESS_FILE = "productive-training-readiness.v2.json"
    private const val INVENTORY_FILE = "transfer-inventory.v2.json"

    private val modelFiles = listOf(
        ArtifactSpec("config.json", 615, "d66ed8cd4f2a93b358c245e50736fa389ed4f35c0bae7aad0b32abb20c62b579", "model-config"),
        ArtifactSpec("model.safetensors", 1_115_567_652, MODEL_WEIGHTS_SHA256, "model-weights"),
        ArtifactSpec("sentencepiece.bpe.model", 5_069_051, "cfc8146abe2a0488e9e2a0c56de7952f7c11ab059eca145a0a727afce0db2865", "tokenizer-model"),
        ArtifactSpec("tokenizer.json", 9_096_718, TOKENIZER_ARTIFACT_SHA256, "tokenizer-definition"),
        ArtifactSpec("tokenizer_config.json", 25, "994f46754c5bf4014f1aa92d34b1374319c3a6b3f702105cd5b742beaecd18ce", "tokenizer-config"),
    )
    private val trainerFiles = listOf(
        "__init__.py", "__main__.py", "a100_validation_v1.py", "execution_device_v1.py",
        "point12_protocol_v1.py", "point12_token_tensor_builder_v1.py", "point13_forward_rng_contract_v1.py",
        "point13_loss_contract_v1.py", "point13_loss_v1.py", "point13_model_forward_v1.py",
        "point13_optimizer_construction_v1.py", "point13_optimizer_execution_policy_v1.py",
        "point13_rng_v1.py", "point13_trainability_policy_v1.py", "point13_trainability_projection_v1.py",
        "protocol_v1.py",
    )
    private val gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()

    data class Materialization(
        val requestDigest: String,
        val requestPath: File,
        val manifestPath: File,
        val readinessPath: File,
        val inventoryPath: File,
        val packetRoot: File,
        val packetFileCount: Int,
        val packetTotalBytes: Long,
        val inventoryDigest: String,
    )

    private data class ArtifactSpec(val name: String, val size: Long, val sha256: String, val role: String)
    private data class PacketEntry(val relativePath: String, val source: File, val role: String, val authority: String)

    /** Materializes only deterministic JSON authorities and the self-contained packet. */
    fun materialize(repositoryRoot: File): Materialization {
        val root = repositoryRoot.canonicalFile
        val inputs = verifyFrozenInputs(root)
        val requestFields = requestIdentityFields(inputs)
        val requestDigest = digestCanonical(requestFields)
        val requestJson = requestJson(requestFields, requestDigest)
        val requestPath = root.resolve("data/knowledge/him/training/requests/v2/$requestDigest/$REQUEST_FILE")
        writeImmutable(requestPath, jsonBytes(requestJson))
        validateRequest(requestPath)

        val manifestFields = manifestIdentityFields(requestDigest, inputs)
        val manifestDigest = digestCanonical(manifestFields)
        val manifestJson = manifestJson(manifestFields, manifestDigest)
        val manifestPath = root.resolve("data/knowledge/him/training/manifests/v2/$requestDigest/$MANIFEST_FILE")
        writeImmutable(manifestPath, jsonBytes(manifestJson))
        validateManifest(manifestPath, requestDigest)

        val packetRoot = root.resolve("data/knowledge/him/training/a100-transfer/v2/$requestDigest/packet")
        val entries = packetEntries(root, requestPath, manifestPath, inputs)
        entries.forEach { entry ->
            val target = packetRoot.resolve(entry.relativePath)
            copyImmutable(entry.source, target)
        }
        val readinessFields = readinessIdentityFields(requestDigest, manifestDigest)
        val readinessDigest = digestCanonical(readinessFields)
        val readinessJson = readinessJson(readinessFields, readinessDigest)
        val readinessPath = root.resolve("data/knowledge/him/training/readiness/v2/$requestDigest/$READINESS_FILE")
        writeImmutable(readinessPath, jsonBytes(readinessJson))
        writeImmutable(packetRoot.resolve("readiness/$READINESS_FILE"), jsonBytes(readinessJson))
        val inventoryEntries = entries + PacketEntry("readiness/$READINESS_FILE", readinessPath, "productive-readiness", "productive-training-readiness:v2")
        val inventoryJson = inventoryJson(inventoryEntries)
        val inventoryPath = root.resolve("data/knowledge/him/training/a100-transfer/v2/$requestDigest/$INVENTORY_FILE")
        writeImmutable(inventoryPath, jsonBytes(inventoryJson))
        val inventoryDigest = sha256(inventoryPath)
        copyImmutable(inputs.evaluationReadiness, packetRoot.resolve("readiness/evaluation-readiness.v2.json"))
        validateReadiness(readinessPath, requestDigest, manifestDigest)

        val result = Materialization(requestDigest, requestPath, manifestPath, readinessPath, inventoryPath, packetRoot, entries.size + 1, packetBytes(packetRoot), inventoryDigest)
        validateTransfer(result)
        return result
    }

    fun validateAll(repositoryRoot: File): Materialization {
        val root = repositoryRoot.canonicalFile
        val requestRoot = root.resolve("data/knowledge/him/training/requests/v2")
        val requestDir = requestRoot.listFiles()?.singleOrNull { it.isDirectory } ?: error("REQUEST_V2_DIRECTORY_INVALID")
        val requestPath = requestDir.resolve(REQUEST_FILE)
        validateRequest(requestPath)
        val request = parse(requestPath)
        val requestDigest = required(request, "logicalDigest")
        val manifestPath = root.resolve("data/knowledge/him/training/manifests/v2/$requestDigest/$MANIFEST_FILE")
        validateManifest(manifestPath, requestDigest)
        val manifest = parse(manifestPath)
        val manifestDigest = required(manifest, "logicalDigest")
        val inventoryPath = root.resolve("data/knowledge/him/training/a100-transfer/v2/$requestDigest/$INVENTORY_FILE")
        val inventoryDigest = sha256(inventoryPath)
        val readinessPath = root.resolve("data/knowledge/him/training/readiness/v2/$requestDigest/$READINESS_FILE")
        validateReadiness(readinessPath, requestDigest, manifestDigest)
        val packetRoot = root.resolve("data/knowledge/him/training/a100-transfer/v2/$requestDigest/packet")
        val result = Materialization(requestDigest, requestPath, manifestPath, readinessPath, inventoryPath, packetRoot, packetRootFiles(packetRoot).size, packetBytes(packetRoot), inventoryDigest)
        validateTransfer(result)
        return result
    }

    fun validateRequest(path: File) {
        val json = parse(path)
        require(required(json, "contractId") == REQUEST_CONTRACT_ID) { "REQUEST_CONTRACT" }
        require(json.get("version").asInt == REQUEST_VERSION) { "REQUEST_VERSION" }
        require(required(json, "state") == REQUEST_STATE) { "REQUEST_STATE" }
        require(required(json, "logicalDigest") == digestCanonical(requestFieldsFrom(json))) { "REQUEST_DIGEST" }
        require(required(json, "requestReference") == "productive-training-request:v2:${required(json, "logicalDigest")}") { "REQUEST_REFERENCE" }
        require(required(json, "corpusLogicalDigest") == CORPUS_LOGICAL_DIGEST) { "REQUEST_CORPUS" }
        require(required(json, "partitionLogicalDigest") == PARTITION_LOGICAL_DIGEST) { "REQUEST_PARTITION" }
        require(required(json, "evaluationReadinessLogicalDigest") == EVALUATION_READINESS_LOGICAL_DIGEST) { "REQUEST_READINESS" }
        require(required(json, "modelRevision") == MODEL_REVISION) { "REQUEST_MODEL_REVISION" }
        require(required(json, "modelWeightsSha256") == MODEL_WEIGHTS_SHA256) { "REQUEST_MODEL_SHA" }
        require(required(json, "modelWeightsByteCount").toLong() == 1_115_567_652L) { "REQUEST_MODEL_SIZE" }
        require(required(json, "tokenizerId") == TOKENIZER_ID) { "REQUEST_TOKENIZER" }
        require(required(json, "runtimeImage") == RUNTIME_IMAGE) { "REQUEST_RUNTIME" }
        require(required(json, "cudaDevice") == CUDA_DEVICE) { "REQUEST_DEVICE" }
        require(json.get("holdoutTrainingEnabled").asBoolean.not()) { "REQUEST_HOLDOUT_TRAINING" }
        require(json.get("holdoutValidationEnabled").asBoolean.not()) { "REQUEST_HOLDOUT_VALIDATION" }
        require(json.get("holdoutModelSelectionEnabled").asBoolean.not()) { "REQUEST_HOLDOUT_MODEL_SELECTION" }
        require(json.get("trainCount").asInt == 32) { "REQUEST_TRAIN_COUNT" }
        require(json.get("validationCount").asInt == 6) { "REQUEST_VALIDATION_COUNT" }
        require(json.get("holdoutCount").asInt == 2) { "REQUEST_HOLDOUT_COUNT" }
        require(json.get("leakageCount").asInt == 0) { "REQUEST_LEAKAGE" }
    }

    fun validateManifest(path: File, requestDigest: String) {
        val json = parse(path)
        require(required(json, "contractId") == MANIFEST_CONTRACT_ID) { "MANIFEST_CONTRACT" }
        require(json.get("version").asInt == MANIFEST_VERSION) { "MANIFEST_VERSION" }
        require(required(json, "state") == MANIFEST_STATE) { "MANIFEST_STATE" }
        require(required(json, "productiveRequestDigest") == requestDigest) { "MANIFEST_REQUEST" }
        require(required(json, "productiveRequestReference") == "productive-training-request:v2:$requestDigest") { "MANIFEST_REQUEST_REFERENCE" }
        require(required(json, "corpusPhysicalDigest") == CORPUS_PHYSICAL_DIGEST) { "MANIFEST_CORPUS" }
        require(required(json, "partitionPhysicalDigest") == PARTITION_PHYSICAL_DIGEST) { "MANIFEST_PARTITION" }
        require(required(json, "evaluationReadinessPhysicalDigest") == EVALUATION_READINESS_PHYSICAL_DIGEST) { "MANIFEST_READINESS" }
        require(required(json, "modelBindingDigest") == MODEL_BINDING_DIGEST) { "MANIFEST_MODEL" }
        require(required(json, "tokenizerId") == TOKENIZER_ID) { "MANIFEST_TOKENIZER" }
        require(required(json, "runtimeImage") == RUNTIME_IMAGE) { "MANIFEST_RUNTIME" }
        require(required(json, "cudaDevice") == CUDA_DEVICE) { "MANIFEST_DEVICE" }
        require(json.get("holdoutTrainingEnabled").asBoolean.not()) { "MANIFEST_HOLDOUT_TRAINING" }
        require(required(json, "checkpointContractVersion").toInt() == CHECKPOINT_CONTRACT_VERSION) { "MANIFEST_CHECKPOINT" }
        require(required(json, "logicalDigest") == digestCanonical(manifestFieldsFrom(json))) { "MANIFEST_DIGEST" }
        require(required(json, "manifestReference") == "productive-training-manifest:v2:${required(json, "logicalDigest")}") { "MANIFEST_REFERENCE" }
    }

    fun validateReadiness(path: File, requestDigest: String, manifestDigest: String) {
        val json = parse(path)
        require(required(json, "contractId") == READINESS_CONTRACT_ID) { "READINESS_CONTRACT" }
        require(json.get("version").asInt == READINESS_VERSION) { "READINESS_VERSION" }
        require(required(json, "state") == READINESS_STATE) { "READINESS_STATE" }
        require(required(json, "productiveRequestDigest") == requestDigest) { "READINESS_REQUEST" }
        require(required(json, "manifestDigest") == manifestDigest) { "READINESS_MANIFEST" }
        listOf("requestIdentityValid", "manifestValid", "datasetReady", "modelAuthorityPass", "tokenizerAuthorityPass", "runtimeAuthorityPass", "cudaDeviceAuthorityPass", "trainingConfigurationAuthorityPass", "checkpointAuthorityPass", "transferPacketFeasible").forEach { key ->
            require(json.get(key).asBoolean) { "READINESS_$key" }
        }
        require(json.get("trainCount").asInt > 0 && json.get("validationCount").asInt > 0 && json.get("holdoutCount").asInt > 0) { "READINESS_PARTITION_COUNTS" }
        require(json.get("leakageCount").asInt == 0) { "READINESS_LEAKAGE" }
        require(required(json, "logicalDigest") == digestCanonical(readinessFieldsFrom(json))) { "READINESS_DIGEST" }
        require(required(json, "readinessReference") == "productive-training-readiness:v2:${required(json, "logicalDigest")}") { "READINESS_REFERENCE" }
    }

    fun validateTransfer(materialization: Materialization) {
        val root = materialization.packetRoot
        require(materialization.inventoryPath.isFile) { "PACKET_INVENTORY" }
        require(root.isDirectory) { "PACKET_ROOT" }
        require(Files.walk(root.toPath()).use { it.anyMatch { path -> Files.isSymbolicLink(path) } }.not()) { "PACKET_SYMLINK" }
        val files = packetRootFiles(root)
        require(files.size == 27) { "PACKET_FILE_COUNT" }
        require(files.none { it.name.startsWith(".") }) { "PACKET_TEMP_FILE" }
        require(files.count { it.toRelativeString(root).startsWith("model/") } == 5) { "PACKET_MODEL_COUNT" }
        require(files.count { it.toRelativeString(root).startsWith("trainer/") } == 16) { "PACKET_TRAINER_COUNT" }
        require(files.count { it.toRelativeString(root).startsWith("request/") } == 1) { "PACKET_REQUEST_COUNT" }
        require(files.count { it.toRelativeString(root).startsWith("manifest/") } == 1) { "PACKET_MANIFEST_COUNT" }
        require(files.count { it.toRelativeString(root).startsWith("dataset/corpus") } == 1) { "PACKET_CORPUS_COUNT" }
        require(files.count { it.toRelativeString(root).startsWith("dataset/partition") } == 1) { "PACKET_PARTITION_COUNT" }
        require(files.count { it.toRelativeString(root).startsWith("readiness/") } == 2) { "PACKET_READINESS_COUNT" }
        require(materialization.packetFileCount == 27) { "PACKET_RESULT_COUNT" }
        require(materialization.packetTotalBytes == packetBytes(root)) { "PACKET_RESULT_BYTES" }
        val inventory = parse(materialization.inventoryPath)
        val inventoryDigest = required(inventory, "logicalDigest")
        require(inventoryDigest.length == 64 && inventoryDigest.all { it in "0123456789abcdef" }) { "PACKET_INVENTORY_DIGEST" }
        require(required(parse(root.resolve("request/$REQUEST_FILE")), "logicalDigest") == materialization.requestDigest) { "PACKET_REQUEST_BINDING" }
        require(sha256(root.resolve("dataset/corpus.v2.json")) == CORPUS_PHYSICAL_DIGEST) { "PACKET_CORPUS_BYTES" }
        require(sha256(root.resolve("dataset/partition.v2.json")) == PARTITION_PHYSICAL_DIGEST) { "PACKET_PARTITION_BYTES" }
        require(sha256(root.resolve("readiness/evaluation-readiness.v2.json")) == EVALUATION_READINESS_PHYSICAL_DIGEST) { "PACKET_READINESS_BYTES" }
        modelFiles.forEach { spec -> requireArtifact(root.resolve("model/$MODEL_ID/$MODEL_REVISION/${spec.name}"), spec) }
        trainerFiles.forEach { name -> require(Files.isRegularFile(root.resolve("trainer/him_trainer/$name").toPath())) { "PACKET_TRAINER_$name" } }
    }

    private data class FrozenInputs(val corpus: File, val partition: File, val evaluationReadiness: File)

    private fun verifyFrozenInputs(root: File): FrozenInputs {
        val inputs = FrozenInputs(root.resolve(CORPUS_PATH), root.resolve(PARTITION_PATH), root.resolve(EVALUATION_READINESS_PATH))
        require(sha256(inputs.corpus) == CORPUS_PHYSICAL_DIGEST) { "CORPUS_PHYSICAL_DIGEST" }
        require(sha256(inputs.partition) == PARTITION_PHYSICAL_DIGEST) { "PARTITION_PHYSICAL_DIGEST" }
        require(sha256(inputs.evaluationReadiness) == EVALUATION_READINESS_PHYSICAL_DIGEST) { "READINESS_PHYSICAL_DIGEST" }
        require(required(parse(inputs.evaluationReadiness), "state") == "READY") { "EVALUATION_READINESS_STATE" }
        return inputs
    }

    private fun requestIdentityFields(inputs: FrozenInputs): Map<String, String> = linkedMapOf(
        "schema" to REQUEST_CONTRACT_ID, "version" to REQUEST_VERSION.toString(),
        "corpusLogicalDigest" to CORPUS_LOGICAL_DIGEST, "corpusPhysicalDigest" to CORPUS_PHYSICAL_DIGEST,
        "partitionLogicalDigest" to PARTITION_LOGICAL_DIGEST, "partitionPhysicalDigest" to PARTITION_PHYSICAL_DIGEST,
        "evaluationReadinessLogicalDigest" to EVALUATION_READINESS_LOGICAL_DIGEST,
        "evaluationReadinessPhysicalDigest" to EVALUATION_READINESS_PHYSICAL_DIGEST,
        "modelId" to MODEL_ID, "modelRevision" to MODEL_REVISION, "modelWeightsSha256" to MODEL_WEIGHTS_SHA256,
        "modelWeightsByteCount" to "1115567652",
        "modelBindingDigest" to MODEL_BINDING_DIGEST, "tokenizerId" to TOKENIZER_ID,
        "tokenizerArtifactSha256" to TOKENIZER_ARTIFACT_SHA256, "trainingSeed" to TRAINING_SEED.toString(),
        "epochs" to TRAINING_EPOCHS.toString(), "microBatchSize" to TRAINING_MICRO_BATCH_SIZE.toString(),
        "gradientAccumulationSteps" to TRAINING_GRADIENT_ACCUMULATION_STEPS.toString(),
        "learningRate" to TRAINING_LEARNING_RATE, "optimizerId" to "optimizer:adamw:v1",
        "sequenceLength" to "128", "trainingSteps" to TRAINING_STEPS.toString(), "cudaDevice" to CUDA_DEVICE,
        "runtimeImage" to RUNTIME_IMAGE, "checkpointContractVersion" to CHECKPOINT_CONTRACT_VERSION.toString(),
        "holdoutTrainingEnabled" to "false", "holdoutValidationEnabled" to "false", "holdoutModelSelectionEnabled" to "false",
        "validationPolicy" to "EACH_EPOCH_AND_FINAL", "provenancePolicy" to "SOURCE_AND_LINEAGE_PRESERVED",
    )

    private fun manifestIdentityFields(requestDigest: String, inputs: FrozenInputs): Map<String, String> = linkedMapOf(
        "schema" to MANIFEST_CONTRACT_ID, "version" to MANIFEST_VERSION.toString(),
        "productiveRequestDigest" to requestDigest, "corpusLogicalDigest" to CORPUS_LOGICAL_DIGEST,
        "corpusPhysicalDigest" to CORPUS_PHYSICAL_DIGEST, "partitionLogicalDigest" to PARTITION_LOGICAL_DIGEST,
        "partitionPhysicalDigest" to PARTITION_PHYSICAL_DIGEST, "evaluationReadinessLogicalDigest" to EVALUATION_READINESS_LOGICAL_DIGEST,
        "evaluationReadinessPhysicalDigest" to EVALUATION_READINESS_PHYSICAL_DIGEST, "modelId" to MODEL_ID,
        "modelRevision" to MODEL_REVISION, "modelWeightsSha256" to MODEL_WEIGHTS_SHA256,
        "modelBindingDigest" to MODEL_BINDING_DIGEST, "tokenizerId" to TOKENIZER_ID,
        "tokenizerArtifactSha256" to TOKENIZER_ARTIFACT_SHA256, "runtimeImage" to RUNTIME_IMAGE,
        "cudaDevice" to CUDA_DEVICE, "trainingSeed" to TRAINING_SEED.toString(), "trainingSteps" to TRAINING_STEPS.toString(),
        "trainingEpochs" to TRAINING_EPOCHS.toString(), "checkpointContractVersion" to CHECKPOINT_CONTRACT_VERSION.toString(),
        "validationPolicy" to "EACH_EPOCH_AND_FINAL", "holdoutPolicy" to "EXCLUDED_FROM_TRAINING_VALIDATION_MODEL_SELECTION",
        "trainerSourceCount" to trainerFiles.size.toString(), "checkpointAuthority" to "HIM_TRAINING_CHECKPOINT_CONTRACT_V1",
    )

    private fun requestJson(fields: Map<String, String>, digest: String): JsonObject = JsonObject().apply {
        addProperty("contractId", REQUEST_CONTRACT_ID); addProperty("version", REQUEST_VERSION); addProperty("state", REQUEST_STATE)
        addProperty("logicalDigest", digest); addProperty("requestReference", "productive-training-request:v2:$digest")
        fields.forEach { (key, value) -> addProperty(key, value) }
        addProperty("holdoutTrainingEnabled", false); addProperty("holdoutValidationEnabled", false); addProperty("holdoutModelSelectionEnabled", false)
        addProperty("trainCount", 32); addProperty("validationCount", 6); addProperty("holdoutCount", 2); addProperty("leakageCount", 0)
    }

    private fun manifestJson(fields: Map<String, String>, digest: String): JsonObject = JsonObject().apply {
        addProperty("contractId", MANIFEST_CONTRACT_ID); addProperty("version", MANIFEST_VERSION); addProperty("state", MANIFEST_STATE)
        addProperty("logicalDigest", digest); addProperty("manifestReference", "productive-training-manifest:v2:$digest")
        fields.forEach { (key, value) -> addProperty(key, value) }
        addProperty("productiveRequestReference", "productive-training-request:v2:${fields["productiveRequestDigest"]}")
        addProperty("holdoutTrainingEnabled", false); addProperty("holdoutValidationEnabled", false); addProperty("holdoutModelSelectionEnabled", false)
    }

    private fun readinessIdentityFields(requestDigest: String, manifestDigest: String): Map<String, String> = linkedMapOf(
        "schema" to READINESS_CONTRACT_ID, "version" to READINESS_VERSION.toString(), "productiveRequestDigest" to requestDigest,
        "manifestDigest" to manifestDigest, "trainCount" to "32", "validationCount" to "6",
        "holdoutCount" to "2", "leakageCount" to "0", "requestIdentityValid" to "true", "manifestValid" to "true", "datasetReady" to "true",
        "modelAuthorityPass" to "true", "tokenizerAuthorityPass" to "true", "runtimeAuthorityPass" to "true", "cudaDeviceAuthorityPass" to "true",
        "trainingConfigurationAuthorityPass" to "true", "checkpointAuthorityPass" to "true", "transferPacketFeasible" to "true",
        "holdoutTrainingEnabled" to "false", "holdoutValidationEnabled" to "false", "holdoutModelSelectionEnabled" to "false",
    )

    private fun readinessJson(fields: Map<String, String>, digest: String): JsonObject = JsonObject().apply {
        addProperty("contractId", READINESS_CONTRACT_ID); addProperty("version", READINESS_VERSION); addProperty("state", READINESS_STATE)
        addProperty("logicalDigest", digest); addProperty("readinessReference", "productive-training-readiness:v2:$digest")
        fields.forEach { (key, value) ->
            when (key) { "trainCount", "validationCount", "holdoutCount", "leakageCount" -> addProperty(key, value.toInt())
                "requestIdentityValid", "manifestValid", "datasetReady", "modelAuthorityPass", "tokenizerAuthorityPass", "runtimeAuthorityPass", "cudaDeviceAuthorityPass", "trainingConfigurationAuthorityPass", "checkpointAuthorityPass", "transferPacketFeasible", "holdoutTrainingEnabled", "holdoutValidationEnabled", "holdoutModelSelectionEnabled" -> addProperty(key, value.toBoolean())
                else -> addProperty(key, value) }
        }
    }

    private fun packetEntries(root: File, request: File, manifest: File, inputs: FrozenInputs): List<PacketEntry> {
        val entries = mutableListOf<PacketEntry>()
        entries += PacketEntry("request/$REQUEST_FILE", request, "productive-request", "productive-training-request:v2")
        entries += PacketEntry("manifest/$MANIFEST_FILE", manifest, "productive-manifest", "productive-training-manifest:v2")
        entries += PacketEntry("readiness/evaluation-readiness.v2.json", inputs.evaluationReadiness, "evaluation-readiness", "evaluation-readiness:v2")
        entries += PacketEntry("dataset/corpus.v2.json", inputs.corpus, "corpus", "corpus:v2")
        entries += PacketEntry("dataset/partition.v2.json", inputs.partition, "partition", "partition:v2")
        modelFiles.forEach { spec ->
            val source = root.resolve("$MODEL_ROOT/${spec.name}")
            requireArtifact(source, spec)
            entries += PacketEntry("model/$MODEL_ID/$MODEL_REVISION/${spec.name}", source, spec.role, "model:$MODEL_REVISION")
        }
        trainerFiles.forEach { name ->
            val source = root.resolve("training/him/src/him_trainer/$name")
            require(Files.isRegularFile(source.toPath())) { "TRAINER_SOURCE_$name" }
            entries += PacketEntry("trainer/him_trainer/$name", source, "trainer-source", "him_trainer:v1")
        }
        return entries
    }

    private fun inventoryJson(entries: List<PacketEntry>): JsonObject = JsonObject().apply {
        val logicalDigest = inventoryLogicalDigest(entries)
        addProperty("contractId", "HIM_P1_A100_TRANSFER_INVENTORY_V2"); addProperty("version", 2); addProperty("fileCount", entries.size)
        addProperty("logicalDigest", logicalDigest); addProperty("inventoryReference", "a100-transfer-inventory:v2:$logicalDigest")
        val files = com.google.gson.JsonArray()
        entries.sortedBy { it.relativePath }.forEach { entry ->
            files.add(JsonObject().apply {
                addProperty("relativePath", entry.relativePath); addProperty("size", Files.size(entry.source.toPath()))
                addProperty("sha256", sha256(entry.source)); addProperty("role", entry.role); addProperty("authority", entry.authority)
            })
        }
        add("files", files)
    }

    private fun inventoryLogicalDigest(entries: List<PacketEntry>): String = sha256Bytes(
        entries.sortedBy { it.relativePath }.joinToString("") { entry ->
            "${entry.relativePath}\n${Files.size(entry.source.toPath())}\n${sha256(entry.source)}\n${entry.role}\n${entry.authority}\n"
        }.toByteArray(StandardCharsets.UTF_8),
    )

    private fun requestFieldsFrom(json: JsonObject): Map<String, String> = linkedMapOf(
        "schema" to required(json, "schema"), "version" to required(json, "version"), "corpusLogicalDigest" to required(json, "corpusLogicalDigest"),
        "corpusPhysicalDigest" to required(json, "corpusPhysicalDigest"), "partitionLogicalDigest" to required(json, "partitionLogicalDigest"),
        "partitionPhysicalDigest" to required(json, "partitionPhysicalDigest"), "evaluationReadinessLogicalDigest" to required(json, "evaluationReadinessLogicalDigest"),
        "evaluationReadinessPhysicalDigest" to required(json, "evaluationReadinessPhysicalDigest"), "modelId" to required(json, "modelId"),
        "modelRevision" to required(json, "modelRevision"), "modelWeightsSha256" to required(json, "modelWeightsSha256"), "modelWeightsByteCount" to required(json, "modelWeightsByteCount"), "modelBindingDigest" to required(json, "modelBindingDigest"),
        "tokenizerId" to required(json, "tokenizerId"), "tokenizerArtifactSha256" to required(json, "tokenizerArtifactSha256"), "trainingSeed" to required(json, "trainingSeed"),
        "epochs" to required(json, "epochs"), "microBatchSize" to required(json, "microBatchSize"), "gradientAccumulationSteps" to required(json, "gradientAccumulationSteps"),
        "learningRate" to required(json, "learningRate"), "optimizerId" to required(json, "optimizerId"), "sequenceLength" to required(json, "sequenceLength"),
        "trainingSteps" to required(json, "trainingSteps"), "cudaDevice" to required(json, "cudaDevice"), "runtimeImage" to required(json, "runtimeImage"),
        "checkpointContractVersion" to required(json, "checkpointContractVersion"), "holdoutTrainingEnabled" to required(json, "holdoutTrainingEnabled"),
        "holdoutValidationEnabled" to required(json, "holdoutValidationEnabled"), "holdoutModelSelectionEnabled" to required(json, "holdoutModelSelectionEnabled"),
        "validationPolicy" to required(json, "validationPolicy"), "provenancePolicy" to required(json, "provenancePolicy"),
    )

    private fun manifestFieldsFrom(json: JsonObject): Map<String, String> = linkedMapOf(
        "schema" to required(json, "schema"), "version" to required(json, "version"), "productiveRequestDigest" to required(json, "productiveRequestDigest"),
        "corpusLogicalDigest" to required(json, "corpusLogicalDigest"), "corpusPhysicalDigest" to required(json, "corpusPhysicalDigest"),
        "partitionLogicalDigest" to required(json, "partitionLogicalDigest"), "partitionPhysicalDigest" to required(json, "partitionPhysicalDigest"),
        "evaluationReadinessLogicalDigest" to required(json, "evaluationReadinessLogicalDigest"), "evaluationReadinessPhysicalDigest" to required(json, "evaluationReadinessPhysicalDigest"),
        "modelId" to required(json, "modelId"), "modelRevision" to required(json, "modelRevision"), "modelWeightsSha256" to required(json, "modelWeightsSha256"),
        "modelBindingDigest" to required(json, "modelBindingDigest"), "tokenizerId" to required(json, "tokenizerId"), "tokenizerArtifactSha256" to required(json, "tokenizerArtifactSha256"),
        "runtimeImage" to required(json, "runtimeImage"), "cudaDevice" to required(json, "cudaDevice"), "trainingSeed" to required(json, "trainingSeed"),
        "trainingSteps" to required(json, "trainingSteps"), "trainingEpochs" to required(json, "trainingEpochs"), "checkpointContractVersion" to required(json, "checkpointContractVersion"),
        "validationPolicy" to required(json, "validationPolicy"), "holdoutPolicy" to required(json, "holdoutPolicy"), "trainerSourceCount" to required(json, "trainerSourceCount"),
        "checkpointAuthority" to required(json, "checkpointAuthority"),
    )

    private fun readinessFieldsFrom(json: JsonObject): Map<String, String> = linkedMapOf(
        "schema" to required(json, "schema"), "version" to required(json, "version"), "productiveRequestDigest" to required(json, "productiveRequestDigest"),
        "manifestDigest" to required(json, "manifestDigest"), "trainCount" to required(json, "trainCount"),
        "validationCount" to required(json, "validationCount"), "holdoutCount" to required(json, "holdoutCount"), "leakageCount" to required(json, "leakageCount"),
        "requestIdentityValid" to required(json, "requestIdentityValid"), "manifestValid" to required(json, "manifestValid"), "datasetReady" to required(json, "datasetReady"),
        "modelAuthorityPass" to required(json, "modelAuthorityPass"), "tokenizerAuthorityPass" to required(json, "tokenizerAuthorityPass"), "runtimeAuthorityPass" to required(json, "runtimeAuthorityPass"),
        "cudaDeviceAuthorityPass" to required(json, "cudaDeviceAuthorityPass"), "trainingConfigurationAuthorityPass" to required(json, "trainingConfigurationAuthorityPass"),
        "checkpointAuthorityPass" to required(json, "checkpointAuthorityPass"), "transferPacketFeasible" to required(json, "transferPacketFeasible"),
        "holdoutTrainingEnabled" to required(json, "holdoutTrainingEnabled"), "holdoutValidationEnabled" to required(json, "holdoutValidationEnabled"), "holdoutModelSelectionEnabled" to required(json, "holdoutModelSelectionEnabled"),
    )

    private fun digestCanonical(fields: Map<String, String>): String = sha256Bytes(fields.toSortedMap().entries.joinToString("") { "${it.key}=${it.value.length}:\n${it.value}\n" }.toByteArray(StandardCharsets.UTF_8))

    private fun parse(path: File): JsonObject = JsonParser.parseString(Files.readString(path.toPath(), StandardCharsets.UTF_8)).asJsonObject
    private fun required(json: JsonObject, key: String): String = json.get(key)?.let { if (it.isJsonPrimitive) it.asString else throw IllegalArgumentException("FIELD_$key") } ?: throw IllegalArgumentException("MISSING_$key")
    private fun jsonBytes(json: JsonObject): ByteArray = (gson.toJson(json) + "\n").toByteArray(StandardCharsets.UTF_8)

    private fun writeImmutable(path: File, bytes: ByteArray) {
        path.parentFile.mkdirs()
        if (path.exists()) require(Files.readAllBytes(path.toPath()).contentEquals(bytes)) { "IMMUTABLE_CONFLICT_${path.name}" }
        else Files.write(path.toPath(), bytes)
    }

    private fun copyImmutable(source: File, target: File) {
        target.parentFile.mkdirs()
        if (target.exists()) {
            require(target.length() == source.length() && sha256(target) == sha256(source)) { "IMMUTABLE_CONFLICT_${target.name}" }
        } else {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        }
    }

    private fun requireArtifact(path: File, spec: ArtifactSpec) {
        require(path.isFile) { "MISSING_ARTIFACT_${spec.name}" }
        require(path.length() == spec.size) { "ARTIFACT_SIZE_${spec.name}" }
        require(sha256(path) == spec.sha256) { "ARTIFACT_SHA_${spec.name}" }
    }

    private fun packetRootFiles(root: File): List<File> = Files.walk(root.toPath()).use { stream -> stream.filter { Files.isRegularFile(it) }.map(Path::toFile).toList() }
    private fun packetBytes(root: File): Long = packetRootFiles(root).sumOf { it.length() }
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file.toPath()).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun sha256Bytes(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun File.toRelativeString(base: File): String = base.toPath().relativize(toPath()).toString().replace(File.separatorChar, '/')
}
