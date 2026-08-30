package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/** Durable, immutable persistence for already validated P1 binding decisions. */
object HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_REFERENCE_BINDING_PERSISTENCE_V1"
    const val VERSION = "1"
    const val STATE = "POSITIVE_TRAINING_EXAMPLE_REFERENCE_BINDING_PERSISTED"
    const val DURABLE_ROOT =
        "data/knowledge/him/canonical-family/human-review/zero-candidate-recovery/v1/positive-training-example-reference-bindings"
    const val DURABLE_FILE_SUFFIX = ".reference-binding.v1.json"

    private const val RECORD_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_REFERENCE_BINDING_RECORD_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")
    private val REFERENCE = Regex("example:v1:[0-9a-f]{64}")

    fun execute(request: Request): Result {
        if (!request.enabled) return Result.Disabled
        return try {
            val record = recordFrom(request.bindingDecision)
            val bytes = serializeRecord(record)
            publishOrReuse(pathFor(record.bindingKey, request.durableRoot), record, bytes)
        } catch (failure: PersistenceFailure) {
            Result.Failed(failure.reason, failure.safeContext)
        } catch (_: Throwable) {
            Result.Failed(FailureReason.WRITE_FAILED, "persistence")
        }
    }

    fun recordFrom(
        decision: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1.BindingDecision,
    ): BindingRecord {
        if (decision.state !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1.BindingState.BOUND
        ) {
            fail(FailureReason.NON_BOUND_DECISION, "decision")
        }
        if (decision.reasons.isNotEmpty()) fail(FailureReason.NON_EMPTY_REASONS, "decision")
        requireBindingKey(decision.bindingKey)
        if (decision.negativeSupervisionRecordId != decision.bindingKey) {
            fail(FailureReason.BINDING_IDENTITY_MISMATCH, "binding")
        }
        val materializationDecisionId = decision.materializationDecisionId
            ?: fail(FailureReason.MISSING_MATERIALIZATION_DECISION_ID, "materializationDecisionId")
        if (!SHA256.matches(materializationDecisionId)) {
            fail(FailureReason.INVALID_MATERIALIZATION_DECISION_ID, "materializationDecisionId")
        }
        val reference = decision.trainingExampleReference
            ?: fail(FailureReason.MISSING_TRAINING_EXAMPLE_REFERENCE, "reference")
        if (!REFERENCE.matches(reference.value)) {
            fail(FailureReason.INVALID_TRAINING_EXAMPLE_REFERENCE, "reference")
        }
        if (!SHA256.matches(decision.bindingDecisionId)) {
            fail(FailureReason.INVALID_BINDING_DECISION_ID, "bindingDecisionId")
        }
        return BindingRecord(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1.BindingState.BOUND,
            bindingKey = decision.bindingKey,
            negativeSupervisionRecordId = decision.negativeSupervisionRecordId,
            materializationDecisionId = materializationDecisionId,
            trainingExampleReference = reference,
            bindingDecisionId = decision.bindingDecisionId,
            reasons = decision.reasons,
            logicalDigest = "",
        ).let { unsigned -> unsigned.copy(logicalDigest = recordLogicalDigest(unsigned)) }
    }

    fun serializeRecord(record: BindingRecord): ByteArray {
        validateRecord(record)
        return (encodeRecord(record).toString() + "\n").toByteArray(StandardCharsets.UTF_8)
    }

    fun deserializeRecord(bytes: ByteArray): BindingRecord {
        return try {
            requireSingleTrailingLf(bytes)
            val root = parseStrictJson(bytes.dropLast(1).toByteArray())
            requireKeys(root, RECORD_KEYS)
            val record = BindingRecord(
                contractId = requiredString(root, "contractId"),
                version = requiredString(root, "version"),
                state = requiredEnum(root, "state"),
                bindingKey = requiredString(root, "bindingKey"),
                negativeSupervisionRecordId = requiredString(root, "negativeSupervisionRecordId"),
                materializationDecisionId = requiredString(root, "materializationDecisionId"),
                trainingExampleReference = requiredReference(root, "trainingExampleReference"),
                bindingDecisionId = requiredString(root, "bindingDecisionId"),
                reasons = requiredArray(root, "reasons").map { value ->
                    require(value.isJsonPrimitive && value.asJsonPrimitive.isString)
                    enumValueOf<HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1.FailureReason>(value.asString)
                },
                logicalDigest = requiredString(root, "logicalDigest"),
            )
            validateRecord(record)
            record
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            fail(FailureReason.MALFORMED_ARTIFACT, "record")
        }
    }

    fun readByNegativeSupervisionRecordId(
        recordId: String,
        durableRoot: File = File(DURABLE_ROOT),
    ): BindingRecord {
        return try {
            requireBindingKey(recordId)
            val path = pathFor(recordId, durableRoot).toPath()
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
                fail(FailureReason.ARTIFACT_MISSING, "record")
            }
            val record = deserializeRecord(Files.readAllBytes(path))
            if (record.bindingKey != recordId || record.negativeSupervisionRecordId != recordId) {
                fail(FailureReason.BINDING_IDENTITY_MISMATCH, "record")
            }
            record
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            fail(FailureReason.READ_FAILED, "record")
        }
    }

    fun pathFor(bindingKey: String, durableRoot: File = File(DURABLE_ROOT)): File {
        requireBindingKey(bindingKey)
        val root = safeRoot(durableRoot)
        val target = root.resolve("$bindingKey$DURABLE_FILE_SUFFIX").normalize()
        if (!target.startsWith(root) || hasSymlinkComponentWithin(target, root)) {
            fail(FailureReason.UNSAFE_TARGET_PATH, "target")
        }
        return target.toFile()
    }

    fun recordLogicalDigest(record: BindingRecord): String = sha256(
        buildString {
            appendLine(RECORD_DIGEST_DOMAIN)
            appendLine("contractId=${record.contractId}")
            appendLine("version=${record.version}")
            appendLine("state=${record.state.name}")
            appendLine("bindingKey=${record.bindingKey}")
            appendLine("negativeSupervisionRecordId=${record.negativeSupervisionRecordId}")
            appendLine("materializationDecisionId=${record.materializationDecisionId}")
            appendLine("trainingExampleReference=${record.trainingExampleReference.value}")
            appendLine("bindingDecisionId=${record.bindingDecisionId}")
            record.reasons.forEach { appendLine("reason=${it.name}") }
        },
    )

    fun validateRecord(record: BindingRecord) {
        if (record.contractId != CONTRACT_ID) fail(FailureReason.INVALID_CONTRACT, "record")
        if (record.version != VERSION) fail(FailureReason.INVALID_VERSION, "record")
        if (record.state !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1.BindingState.BOUND
        ) {
            fail(FailureReason.INVALID_STATE, "record")
        }
        requireBindingKey(record.bindingKey)
        if (record.negativeSupervisionRecordId != record.bindingKey) {
            fail(FailureReason.BINDING_IDENTITY_MISMATCH, "record")
        }
        if (!SHA256.matches(record.materializationDecisionId)) {
            fail(FailureReason.INVALID_MATERIALIZATION_DECISION_ID, "record")
        }
        if (!REFERENCE.matches(record.trainingExampleReference.value)) {
            fail(FailureReason.INVALID_TRAINING_EXAMPLE_REFERENCE, "record")
        }
        if (!SHA256.matches(record.bindingDecisionId)) {
            fail(FailureReason.INVALID_BINDING_DECISION_ID, "record")
        }
        if (record.reasons.isNotEmpty()) fail(FailureReason.NON_EMPTY_REASONS, "record")
        if (!SHA256.matches(record.logicalDigest)) fail(FailureReason.INVALID_LOGICAL_DIGEST, "record")
        if (record.logicalDigest != recordLogicalDigest(record.copy(logicalDigest = ""))) {
            fail(FailureReason.LOGICAL_DIGEST_MISMATCH, "record")
        }
    }

    private fun publishOrReuse(path: File, record: BindingRecord, bytes: ByteArray): Result.Completed {
        val root = requireNotNull(path.parentFile) { "PERSISTENCE_PARENT_REQUIRED" }
        val rootPath = root.toPath()
        val target = path.toPath()
        val temporary = root.resolve(".${path.name}.tmp").toPath()
        if (Files.isSymbolicLink(rootPath)) fail(FailureReason.UNSAFE_DURABLE_ROOT, "root")
        if (Files.isSymbolicLink(target)) fail(FailureReason.UNSAFE_TARGET_PATH, "target")

        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
                fail(FailureReason.PARTIAL_ARTIFACT, "temporary")
            }
            val existing = try {
                Files.readAllBytes(target)
            } catch (_: Throwable) {
                fail(FailureReason.READ_FAILED, "existing")
            }
            val existingRecord = deserializeRecord(existing)
            if (existingRecord != record || !existing.contentEquals(bytes)) {
                fail(FailureReason.EXISTING_ARTIFACT_CONFLICT, "record")
            }
            return completed(PersistenceStatus.ALREADY_PRESENT_IDENTICAL, record, path, existing)
        }
        if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
            fail(FailureReason.PARTIAL_ARTIFACT, "temporary")
        }
        try {
            Files.createDirectories(rootPath)
            Files.write(temporary, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            FileOutputStream(temporary.toFile(), true).use { it.fd.sync() }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target)
            }
        } catch (_: java.nio.file.FileAlreadyExistsException) {
            fail(FailureReason.EXISTING_ARTIFACT_CONFLICT, "record")
        } catch (_: Throwable) {
            fail(FailureReason.ATOMIC_PUBLICATION_FAILED, "write")
        } finally {
            Files.deleteIfExists(temporary)
        }

        val persisted = try {
            Files.readAllBytes(target)
        } catch (_: Throwable) {
            fail(FailureReason.RELOAD_FAILED, "record")
        }
        if (!persisted.contentEquals(bytes)) fail(FailureReason.BYTE_IDENTITY_MISMATCH, "reload")
        try {
            if (deserializeRecord(persisted) != record) fail(FailureReason.RELOAD_MISMATCH, "record")
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            fail(FailureReason.RELOAD_MISMATCH, "record")
        }
        return completed(PersistenceStatus.CREATED, record, path, persisted)
    }

    private fun completed(
        status: PersistenceStatus,
        record: BindingRecord,
        path: File,
        bytes: ByteArray,
    ) = Result.Completed(
        status = status,
        record = record,
        relativePath = path.name,
        byteSize = bytes.size.toLong(),
        sha256 = sha256(bytes),
    )

    private fun safeRoot(durableRoot: File): Path {
        val root = try {
            durableRoot.toPath().toAbsolutePath().normalize()
        } catch (_: Throwable) {
            fail(FailureReason.UNSAFE_DURABLE_ROOT, "root")
        }
        if (Files.isSymbolicLink(root)) fail(FailureReason.UNSAFE_DURABLE_ROOT, "root")
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            fail(FailureReason.UNSAFE_DURABLE_ROOT, "root")
        }
        return root
    }

    private fun hasSymlinkComponentWithin(path: Path, boundary: Path): Boolean {
        var current: Path? = path
        while (current != null) {
            if (Files.isSymbolicLink(current)) return true
            if (current == boundary) return false
            current = current.parent
        }
        return true
    }

    private fun requireBindingKey(value: String) {
        if (!SHA256.matches(value)) fail(FailureReason.INVALID_BINDING_KEY, "bindingKey")
    }

    private fun encodeRecord(record: BindingRecord) = JsonObject().apply {
        addProperty("contractId", record.contractId)
        addProperty("version", record.version)
        addProperty("state", record.state.name)
        addProperty("bindingKey", record.bindingKey)
        addProperty("negativeSupervisionRecordId", record.negativeSupervisionRecordId)
        addProperty("materializationDecisionId", record.materializationDecisionId)
        addProperty("trainingExampleReference", record.trainingExampleReference.value)
        addProperty("bindingDecisionId", record.bindingDecisionId)
        add("reasons", JsonArray().apply { record.reasons.forEach { add(it.name) } })
        addProperty("logicalDigest", record.logicalDigest)
    }

    private fun parseStrictJson(bytes: ByteArray): JsonObject {
        val reader = JsonReader(StringReader(bytes.toString(StandardCharsets.UTF_8)))
        reader.isLenient = false
        val root = readJson(reader)
        require(reader.peek() == JsonToken.END_DOCUMENT)
        return root.takeIf { it.isJsonObject }?.asJsonObject ?: throw IllegalArgumentException("root")
    }

    private fun readJson(reader: JsonReader): JsonElement = when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> {
            reader.beginObject()
            val result = JsonObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (result.has(name)) throw IllegalArgumentException("duplicate")
                result.add(name, readJson(reader))
            }
            reader.endObject()
            result
        }
        JsonToken.BEGIN_ARRAY -> {
            reader.beginArray()
            val result = JsonArray()
            while (reader.hasNext()) result.add(readJson(reader))
            reader.endArray()
            result
        }
        JsonToken.STRING -> JsonPrimitive(reader.nextString())
        JsonToken.NUMBER -> JsonPrimitive(BigDecimal(reader.nextString()))
        JsonToken.BOOLEAN -> JsonPrimitive(reader.nextBoolean())
        JsonToken.NULL -> {
            reader.nextNull()
            throw IllegalArgumentException("null")
        }
        else -> throw IllegalArgumentException("json")
    }

    private fun requireSingleTrailingLf(bytes: ByteArray) {
        if (bytes.isEmpty() || bytes.last() != '\n'.code.toByte() || (bytes.size > 1 && bytes[bytes.size - 2] == '\n'.code.toByte())) {
            throw IllegalArgumentException("lf")
        }
    }

    private fun requireKeys(root: JsonObject, expected: Set<String>) {
        if (root.keySet() != expected) throw IllegalArgumentException("fields")
    }

    private fun requiredString(root: JsonObject, key: String): String {
        val value = root.get(key)
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) {
            throw IllegalArgumentException("string")
        }
        return value.asString
    }

    private fun requiredReference(root: JsonObject, key: String): HimTrainingExampleReference {
        val value = requiredString(root, key)
        if (!REFERENCE.matches(value)) fail(FailureReason.INVALID_TRAINING_EXAMPLE_REFERENCE, "record")
        return HimTrainingExampleReference(value)
    }

    private inline fun <reified T : Enum<T>> requiredEnum(root: JsonObject, key: String): T =
        try {
            enumValueOf<T>(requiredString(root, key))
        } catch (_: Throwable) {
            throw IllegalArgumentException("enum")
        }

    private fun requiredArray(root: JsonObject, key: String): List<JsonElement> {
        val value = root.get(key)
        if (value == null || !value.isJsonArray) throw IllegalArgumentException("array")
        return value.asJsonArray.toList()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    private fun fail(reason: FailureReason, safeContext: String): Nothing =
        throw PersistenceFailure(reason, safeContext)

    private val RECORD_KEYS = setOf(
        "contractId",
        "version",
        "state",
        "bindingKey",
        "negativeSupervisionRecordId",
        "materializationDecisionId",
        "trainingExampleReference",
        "bindingDecisionId",
        "reasons",
        "logicalDigest",
    )

    data class BindingRecord(
        val contractId: String,
        val version: String,
        val state: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1.BindingState,
        val bindingKey: String,
        val negativeSupervisionRecordId: String,
        val materializationDecisionId: String,
        val trainingExampleReference: HimTrainingExampleReference,
        val bindingDecisionId: String,
        val reasons: List<HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1.FailureReason>,
        val logicalDigest: String,
    )

    data class Request(
        val bindingDecision: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1.BindingDecision,
        val durableRoot: File = File(DURABLE_ROOT),
        val enabled: Boolean = true,
    )

    enum class PersistenceStatus {
        CREATED,
        ALREADY_PRESENT_IDENTICAL,
    }

    enum class FailureReason {
        NON_BOUND_DECISION,
        NON_EMPTY_REASONS,
        INVALID_BINDING_KEY,
        BINDING_IDENTITY_MISMATCH,
        MISSING_MATERIALIZATION_DECISION_ID,
        INVALID_MATERIALIZATION_DECISION_ID,
        MISSING_TRAINING_EXAMPLE_REFERENCE,
        INVALID_TRAINING_EXAMPLE_REFERENCE,
        INVALID_BINDING_DECISION_ID,
        INVALID_CONTRACT,
        INVALID_VERSION,
        INVALID_STATE,
        INVALID_LOGICAL_DIGEST,
        LOGICAL_DIGEST_MISMATCH,
        MALFORMED_ARTIFACT,
        ARTIFACT_MISSING,
        PARTIAL_ARTIFACT,
        EXISTING_ARTIFACT_CONFLICT,
        UNSAFE_DURABLE_ROOT,
        UNSAFE_TARGET_PATH,
        ATOMIC_PUBLICATION_FAILED,
        RELOAD_FAILED,
        BYTE_IDENTITY_MISMATCH,
        RELOAD_MISMATCH,
        READ_FAILED,
        WRITE_FAILED,
    }

    sealed interface Result {
        data object Disabled : Result

        data class Completed(
            val status: PersistenceStatus,
            val record: BindingRecord,
            val relativePath: String,
            val byteSize: Long,
            val sha256: String,
        ) : Result

        data class Failed(
            val reason: FailureReason,
            val safeContext: String,
        ) : Result
    }

    private class PersistenceFailure(
        val reason: FailureReason,
        val safeContext: String,
    ) : IllegalArgumentException()
}
