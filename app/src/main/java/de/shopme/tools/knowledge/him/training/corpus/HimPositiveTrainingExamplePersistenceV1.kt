package de.shopme.tools.knowledge.him.training.corpus

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimMutationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputRunReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateRunReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/** Immutable, content-addressed persistence for general HIM positive examples. */
object HimPositiveTrainingExamplePersistenceV1 {
    const val CONTRACT_ID = "HIM_POSITIVE_TRAINING_EXAMPLE_PERSISTENCE_V1"
    const val VERSION = "1"
    const val STATE = "POSITIVE_TRAINING_EXAMPLE_PERSISTED"
    const val DURABLE_ROOT = "data/knowledge/him/training/positive-examples/v1"
    const val FILE_SUFFIX = ".training-example.v1.json"

    private const val ZERO_DIGEST = "0000000000000000000000000000000000000000000000000000000000000000"
    private val REFERENCE_PATTERN = Regex("example:v1:([0-9a-f]{64})")
    private val DIGEST_PATTERN = Regex("[0-9a-f]{64}")

    fun execute(request: Request): Result {
        if (!request.enabled) return Result.Disabled
        return try {
            val example = canonicalizeExample(request.example)
            validateExampleReference(example)
            val record = buildRecord(example)
            val bytes = serializeRecord(record)
            val path = pathFor(record.exampleReference, request.durableRoot)
            publishOrReuse(path, record, bytes)
        } catch (failure: PersistenceFailure) {
            Result.Failed(failure.reason, failure.safeContext)
        } catch (_: IllegalArgumentException) {
            Result.Failed(FailureReason.INVALID_TRAINING_EXAMPLE, "example")
        } catch (_: Throwable) {
            Result.Failed(FailureReason.WRITE_FAILED, "store")
        }
    }

    /** Reads only the exact content-addressed record for [reference]. */
    fun read(
        reference: HimTrainingExampleReference,
        durableRoot: File = File(DURABLE_ROOT),
    ): HimTrainingExampleV1 {
        return try {
            validateReference(reference)
            val path = pathFor(reference, durableRoot).toPath()
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
                throw PersistenceFailure(FailureReason.ARTIFACT_MISSING, "record")
            }
            val record = deserializeRecord(Files.readAllBytes(path))
            if (record.exampleReference != reference) {
                throw PersistenceFailure(FailureReason.REFERENCE_CONTENT_MISMATCH, "record")
            }
            record.example
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReason.READ_FAILED, "record")
        }
    }

    fun pathFor(
        reference: HimTrainingExampleReference,
        durableRoot: File = File(DURABLE_ROOT),
    ): File {
        validateReference(reference)
        val digest = REFERENCE_PATTERN.matchEntire(reference.value)!!.groupValues[1]
        return File(durableRoot, "$digest$FILE_SUFFIX")
    }

    fun buildRecord(example: HimTrainingExampleV1): PositiveTrainingExampleRecordV1 {
        val canonicalExample = canonicalizeExample(example)
        validateExampleReference(canonicalExample)
        val unsigned = PositiveTrainingExampleRecordV1(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            exampleReference = canonicalExample.exampleReference,
            example = canonicalExample,
            logicalDigest = ZERO_DIGEST,
        )
        return unsigned.copy(logicalDigest = logicalDigest(unsigned))
    }

    fun serializeRecord(record: PositiveTrainingExampleRecordV1): ByteArray {
        val canonical = record.copy(example = canonicalizeExample(record.example))
        validateRecord(canonical)
        return (encodeRecord(canonical) + "\n").toByteArray(StandardCharsets.UTF_8)
    }

    fun deserializeRecord(bytes: ByteArray): PositiveTrainingExampleRecordV1 {
        return try {
            require(bytes.isNotEmpty() && bytes.last() == '\n'.code.toByte())
            val text = bytes.toString(StandardCharsets.UTF_8)
            require(text.endsWith("\n"))
            val record = decodeRecord(JsonParser.parseString(text.dropLast(1)).asJsonObject)
            if (record.exampleReference != record.example.exampleReference) {
                throw PersistenceFailure(FailureReason.REFERENCE_CONTENT_MISMATCH, "record")
            }
            validateRecord(record)
            record
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReason.MALFORMED_ARTIFACT, "record")
        }
    }

    fun logicalDigest(record: PositiveTrainingExampleRecordV1): String {
        val canonical = record.copy(
            example = canonicalizeExample(record.example),
            logicalDigest = ZERO_DIGEST,
        )
        return sha256(encodeRecord(canonical).toByteArray(StandardCharsets.UTF_8))
    }

    fun validateRecord(record: PositiveTrainingExampleRecordV1) {
        require(record.contractId == CONTRACT_ID)
        require(record.version == VERSION)
        require(record.state == STATE)
        validateReference(record.exampleReference)
        validateExampleReference(record.example)
        require(record.exampleReference == record.example.exampleReference)
        HimTrainingExampleValidatorV1.validate(record.example)
        require(DIGEST_PATTERN.matches(record.logicalDigest))
        require(record.logicalDigest == logicalDigest(record))
    }

    private fun publishOrReuse(
        path: File,
        record: PositiveTrainingExampleRecordV1,
        bytes: ByteArray,
    ): Result {
        val root = requireNotNull(path.parentFile) { "PERSISTENCE_PARENT_REQUIRED" }
        val rootPath = root.toPath()
        require(!Files.isSymbolicLink(rootPath)) { "UNSAFE_DURABLE_ROOT" }
        val target = path.toPath()
        val temporary = root.resolve(".${path.name}.tmp").toPath()
        if (Files.isSymbolicLink(target)) {
            throw PersistenceFailure(FailureReason.UNSAFE_TARGET_PATH, "record")
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
                throw PersistenceFailure(FailureReason.PARTIAL_ARTIFACT, "temporary")
            }
            val existingBytes = try {
                Files.readAllBytes(target)
            } catch (_: Throwable) {
                throw PersistenceFailure(FailureReason.READ_FAILED, "existing")
            }
            val existingRecord = deserializeRecord(existingBytes)
            if (existingRecord != record || !existingBytes.contentEquals(bytes)) {
                throw PersistenceFailure(FailureReason.EXISTING_ARTIFACT_CONFLICT, "record")
            }
            return completed(ResultStatus.ALREADY_PRESENT_IDENTICAL, record, path, existingBytes)
        }
        if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
            throw PersistenceFailure(FailureReason.PARTIAL_ARTIFACT, "temporary")
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
            throw PersistenceFailure(FailureReason.EXISTING_ARTIFACT_CONFLICT, "record")
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReason.ATOMIC_PUBLICATION_FAILED, "write")
        } finally {
            Files.deleteIfExists(temporary)
        }

        val persisted = try {
            Files.readAllBytes(target)
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReason.RELOAD_FAILED, "record")
        }
        require(persisted.contentEquals(bytes)) { "BYTE_IDENTITY_MISMATCH" }
        val reloaded = deserializeRecord(persisted)
        require(reloaded == record) { "RELOAD_MISMATCH" }
        return completed(ResultStatus.CREATED, record, path, persisted)
    }

    private fun completed(
        status: ResultStatus,
        record: PositiveTrainingExampleRecordV1,
        path: File,
        bytes: ByteArray,
    ) = when (status) {
        ResultStatus.CREATED -> Result.Created(
            reference = record.exampleReference,
            relativePath = path.name,
            byteSize = bytes.size.toLong(),
            sha256 = sha256(bytes),
            record = record,
        )
        ResultStatus.ALREADY_PRESENT_IDENTICAL -> Result.AlreadyPresentIdentical(
            reference = record.exampleReference,
            relativePath = path.name,
            byteSize = bytes.size.toLong(),
            sha256 = sha256(bytes),
            record = record,
        )
    }

    private fun validateReference(reference: HimTrainingExampleReference) {
        require(REFERENCE_PATTERN.matches(reference.value))
    }

    private fun validateExampleReference(example: HimTrainingExampleV1) {
        val expected = HimTrainingExampleIdentityV1.example(
            taskType = example.taskType,
            input = example.input,
            target = example.target,
            provenance = example.provenance,
        )
        require(example.exampleReference == expected)
    }

    private fun canonicalizeExample(example: HimTrainingExampleV1): HimTrainingExampleV1 {
        val canonicalEvidence = example.input.evidence.sortedWith(
            compareBy({ it.reference.source }, { it.reference.sourceArtifactSha256.value }, { it.reference.sourceRecordIdentity }, { it.recordKind }, { it.retrievalRank }),
        )
        val canonicalProvenance = example.provenance.copy(
            sourceEvidenceReferences = example.provenance.sourceEvidenceReferences.sortedWith(
                compareBy({ it.source }, { it.sourceArtifactSha256.value }, { it.sourceRecordIdentity }),
            ),
            sourceArtifactDigests = example.provenance.sourceArtifactDigests.sortedBy { it.value },
        )
        return example.copy(
            input = example.input.copy(evidence = canonicalEvidence),
            provenance = canonicalProvenance,
        )
    }

    private fun encodeRecord(record: PositiveTrainingExampleRecordV1): String {
        val root = JsonObject()
        root.addProperty("contractId", record.contractId)
        root.addProperty("version", record.version)
        root.addProperty("state", record.state)
        root.addProperty("exampleReference", record.exampleReference.value)
        root.add("example", encodeExample(record.example))
        root.addProperty("logicalDigest", record.logicalDigest)
        return root.toString()
    }

    private fun encodeExample(example: HimTrainingExampleV1): JsonObject = JsonObject().apply {
        addProperty("exampleReference", example.exampleReference.value)
        addProperty("contractVersion", example.contractVersion)
        addProperty("taskType", example.taskType.name)
        add("input", encodeInput(example.input))
        add("target", encodeTarget(example.target))
        add("provenance", encodeProvenance(example.provenance))
    }

    private fun encodeInput(input: HimTrainingInputV1): JsonObject = JsonObject().apply {
        addProperty("observedTerm", input.observedTerm)
        addProperty("normalizedObservedTerm", input.normalizedObservedTerm)
        add("canonicalContext", JsonArray().also { array -> input.canonicalContext.forEach { array.add(encodeContext(it)) } })
        add("evidence", JsonArray().also { array -> input.evidence.forEach { array.add(encodeEvidence(it)) } })
    }

    private fun encodeContext(context: de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext) = JsonObject().apply {
        addProperty("rank", context.rank)
        addProperty("canonicalId", context.canonicalId.value)
        addProperty("canonicalName", context.canonicalName)
        addNullable("fullRecordCanonicalJson", context.fullRecordCanonicalJson)
    }

    private fun encodeEvidence(evidence: HimTrainingEvidenceInputV1) = JsonObject().apply {
        add("reference", encodeEvidenceReference(evidence.reference))
        addProperty("recordKind", evidence.recordKind)
        addProperty("retrievalRank", evidence.retrievalRank)
    }

    private fun encodeEvidenceReference(reference: HimEvidenceReference) = JsonObject().apply {
        addProperty("source", reference.source)
        addProperty("sourceArtifactSha256", reference.sourceArtifactSha256.value)
        addProperty("sourceRecordIdentity", reference.sourceRecordIdentity)
    }

    private fun encodeTarget(target: HimTrainingTargetV1): JsonObject = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> JsonObject().apply {
            addProperty("targetType", "ExistingCanonical")
            addProperty("canonicalId", target.canonicalId.value)
        }
        is HimTrainingTargetV1.Identity -> JsonObject().apply {
            addProperty("targetType", "Identity")
            addProperty("parentCanonicalId", target.parentCanonicalId.value)
        }
        is HimTrainingTargetV1.Variant -> JsonObject().apply {
            addProperty("targetType", "Variant")
            add("scope", encodeFamilyReference(target.scope))
        }
        is HimTrainingTargetV1.Alias -> JsonObject().apply {
            addProperty("targetType", "Alias")
            add("equivalentEntity", encodeFamilyReference(target.equivalentEntity))
        }
        is HimTrainingTargetV1.NewCanonical -> JsonObject().apply {
            addProperty("targetType", "NewCanonical")
            addNullable("proposedCanonicalName", target.proposedCanonicalName)
        }
    }

    private fun encodeFamilyReference(reference: HimFamilyEntityReference) = JsonObject().apply {
        when (reference) {
            is HimFamilyEntityReference.Canonical -> {
                addProperty("referenceType", "Canonical")
                addProperty("canonicalId", reference.canonicalId.value)
            }
            is HimFamilyEntityReference.Identity -> {
                addProperty("referenceType", "Identity")
                addProperty("canonicalId", reference.canonicalId.value)
                addProperty("identityId", reference.identityId.value)
            }
        }
    }

    private fun encodeProvenance(provenance: HimTrainingProvenanceV1) = JsonObject().apply {
        addNullable("candidateReference", provenance.candidateReference?.value)
        addNullable("generationRunReference", provenance.generationRunReference?.value)
        addNullable("inputRunReference", provenance.inputRunReference?.value)
        addNullable("validationReference", provenance.validationReference?.value)
        addNullable("promotionReference", provenance.promotionReference?.value)
        addNullable("mutationReference", provenance.mutationReference?.value)
        addNullable("groundTruthReleaseReference", provenance.groundTruthReleaseReference?.value)
        addNullable("promotedEntityId", provenance.promotedEntityId?.value)
        addNullable("promotedEntityType", provenance.promotedEntityType?.name)
        add("sourceEvidenceReferences", JsonArray().also { array -> provenance.sourceEvidenceReferences.forEach { array.add(encodeEvidenceReference(it)) } })
        add("sourceArtifactDigests", JsonArray().also { array -> provenance.sourceArtifactDigests.forEach { array.add(it.value) } })
        addNullable("retrievalFoundationRelease", provenance.retrievalFoundationRelease)
        addNullable("retrievalFoundationReleaseSha256", provenance.retrievalFoundationReleaseSha256?.value)
        addNullable("retrievalFoundationDigest", provenance.retrievalFoundationDigest?.value)
        add("teacher", provenance.teacher?.let { teacher -> JsonObject().apply {
            addProperty("provider", teacher.provider)
            addProperty("model", teacher.model)
            addNullable("configurationFingerprint", teacher.configurationFingerprint?.value)
        }} ?: JsonNull.INSTANCE)
    }

    private fun decodeRecord(root: JsonObject): PositiveTrainingExampleRecordV1 {
        requireKeys(root, setOf("contractId", "version", "state", "exampleReference", "example", "logicalDigest"))
        return PositiveTrainingExampleRecordV1(
            contractId = requiredString(root, "contractId"),
            version = requiredString(root, "version"),
            state = requiredString(root, "state"),
            exampleReference = HimTrainingExampleReference(requiredString(root, "exampleReference")),
            example = decodeExample(requiredObject(root, "example")),
            logicalDigest = requiredString(root, "logicalDigest"),
        )
    }

    private fun decodeExample(root: JsonObject): HimTrainingExampleV1 {
        requireKeys(root, setOf("exampleReference", "contractVersion", "taskType", "input", "target", "provenance"))
        return HimTrainingExampleV1(
            exampleReference = HimTrainingExampleReference(requiredString(root, "exampleReference")),
            contractVersion = requiredString(root, "contractVersion"),
            taskType = enumValue<HimTrainingTaskTypeV1>(requiredString(root, "taskType")),
            input = decodeInput(requiredObject(root, "input")),
            target = decodeTarget(requiredObject(root, "target")),
            provenance = decodeProvenance(requiredObject(root, "provenance")),
        )
    }

    private fun decodeInput(root: JsonObject): HimTrainingInputV1 {
        requireKeys(root, setOf("observedTerm", "normalizedObservedTerm", "canonicalContext", "evidence"))
        return HimTrainingInputV1(
            observedTerm = requiredString(root, "observedTerm"),
            normalizedObservedTerm = requiredString(root, "normalizedObservedTerm"),
            canonicalContext = requiredArray(root, "canonicalContext").map { decodeContext(it.asJsonObject) },
            evidence = requiredArray(root, "evidence").map { decodeEvidence(it.asJsonObject) },
        )
    }

    private fun decodeContext(root: JsonObject) = run {
        requireKeys(root, setOf("rank", "canonicalId", "canonicalName", "fullRecordCanonicalJson"))
        de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(
            rank = requiredInt(root, "rank"),
            canonicalId = HimEntityId(requiredString(root, "canonicalId")),
            canonicalName = requiredString(root, "canonicalName"),
            fullRecordCanonicalJson = nullableString(root, "fullRecordCanonicalJson"),
        )
    }

    private fun decodeEvidence(root: JsonObject) = run {
        requireKeys(root, setOf("reference", "recordKind", "retrievalRank"))
        HimTrainingEvidenceInputV1(
            reference = decodeEvidenceReference(requiredObject(root, "reference")),
            recordKind = requiredString(root, "recordKind"),
            retrievalRank = requiredInt(root, "retrievalRank"),
        )
    }

    private fun decodeEvidenceReference(root: JsonObject): HimEvidenceReference {
        requireKeys(root, setOf("source", "sourceArtifactSha256", "sourceRecordIdentity"))
        return HimEvidenceReference(
            source = requiredString(root, "source"),
            sourceArtifactSha256 = HimSha256(requiredString(root, "sourceArtifactSha256")),
            sourceRecordIdentity = requiredString(root, "sourceRecordIdentity"),
        )
    }

    private fun decodeTarget(root: JsonObject): HimTrainingTargetV1 {
        val type = requiredString(root, "targetType")
        return when (type) {
            "ExistingCanonical" -> {
                requireKeys(root, setOf("targetType", "canonicalId"))
                HimTrainingTargetV1.ExistingCanonical(HimEntityId(requiredString(root, "canonicalId")))
            }
            "Identity" -> {
                requireKeys(root, setOf("targetType", "parentCanonicalId"))
                HimTrainingTargetV1.Identity(HimEntityId(requiredString(root, "parentCanonicalId")))
            }
            "Variant" -> {
                requireKeys(root, setOf("targetType", "scope"))
                HimTrainingTargetV1.Variant(decodeFamilyReference(requiredObject(root, "scope")))
            }
            "Alias" -> {
                requireKeys(root, setOf("targetType", "equivalentEntity"))
                HimTrainingTargetV1.Alias(decodeFamilyReference(requiredObject(root, "equivalentEntity")))
            }
            "NewCanonical" -> {
                requireKeys(root, setOf("targetType", "proposedCanonicalName"))
                HimTrainingTargetV1.NewCanonical(nullableString(root, "proposedCanonicalName"))
            }
            else -> error("Unknown training target type")
        }
    }

    private fun decodeFamilyReference(root: JsonObject): HimFamilyEntityReference {
        val type = requiredString(root, "referenceType")
        return when (type) {
            "Canonical" -> {
                requireKeys(root, setOf("referenceType", "canonicalId"))
                HimFamilyEntityReference.Canonical(HimEntityId(requiredString(root, "canonicalId")))
            }
            "Identity" -> {
                requireKeys(root, setOf("referenceType", "canonicalId", "identityId"))
                HimFamilyEntityReference.Identity(
                    canonicalId = HimEntityId(requiredString(root, "canonicalId")),
                    identityId = HimEntityId(requiredString(root, "identityId")),
                )
            }
            else -> error("Unknown family reference type")
        }
    }

    private fun decodeProvenance(root: JsonObject): HimTrainingProvenanceV1 {
        requireKeys(root, setOf(
            "candidateReference", "generationRunReference", "inputRunReference", "validationReference",
            "promotionReference", "mutationReference", "groundTruthReleaseReference", "promotedEntityId",
            "promotedEntityType", "sourceEvidenceReferences", "sourceArtifactDigests", "retrievalFoundationRelease",
            "retrievalFoundationReleaseSha256", "retrievalFoundationDigest", "teacher",
        ))
        val teacherElement = root.get("teacher")
        val teacher = if (teacherElement.isJsonNull) {
            null
        } else {
            val teacherRoot = teacherElement.asJsonObject
            requireKeys(teacherRoot, setOf("provider", "model", "configurationFingerprint"))
            HimTrainingTeacherProvenanceV1(
                provider = requiredString(teacherRoot, "provider"),
                model = requiredString(teacherRoot, "model"),
                configurationFingerprint = nullableString(teacherRoot, "configurationFingerprint")?.let(::HimSha256),
            )
        }
        return HimTrainingProvenanceV1(
            candidateReference = nullableString(root, "candidateReference")?.let(::HimCandidateReference),
            generationRunReference = nullableString(root, "generationRunReference")?.let(::HimCandidateRunReference),
            inputRunReference = nullableString(root, "inputRunReference")?.let(::HimCandidateInputRunReference),
            validationReference = nullableString(root, "validationReference")?.let(::HimCandidateValidationDecisionReference),
            promotionReference = nullableString(root, "promotionReference")?.let(::HimCandidatePromotionReference),
            mutationReference = nullableString(root, "mutationReference")?.let(::HimMutationReference),
            groundTruthReleaseReference = nullableString(root, "groundTruthReleaseReference")?.let(::HimGroundTruthReleaseIdentityV1),
            promotedEntityId = nullableString(root, "promotedEntityId")?.let(::HimEntityId),
            promotedEntityType = nullableString(root, "promotedEntityType")?.let { enumValue<HimEntityType>(it) },
            sourceEvidenceReferences = requiredArray(root, "sourceEvidenceReferences").map { decodeEvidenceReference(it.asJsonObject) },
            sourceArtifactDigests = requiredArray(root, "sourceArtifactDigests").map { HimSha256(it.asString) },
            retrievalFoundationRelease = nullableString(root, "retrievalFoundationRelease"),
            retrievalFoundationReleaseSha256 = nullableString(root, "retrievalFoundationReleaseSha256")?.let(::HimSha256),
            retrievalFoundationDigest = nullableString(root, "retrievalFoundationDigest")?.let(::HimSha256),
            teacher = teacher,
        )
    }

    private fun requireKeys(root: JsonObject, expected: Set<String>) {
        require(root.keySet() == expected)
    }

    private fun requiredString(root: JsonObject, key: String): String {
        val value = root.get(key)
        require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString)
        return value.asString
    }

    private fun nullableString(root: JsonObject, key: String): String? {
        val value = root.get(key)
        require(value != null)
        if (value.isJsonNull) return null
        require(value.isJsonPrimitive && value.asJsonPrimitive.isString)
        return value.asString
    }

    private fun requiredInt(root: JsonObject, key: String): Int {
        val value = root.get(key)
        require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isNumber)
        return value.asInt
    }

    private fun requiredObject(root: JsonObject, key: String): JsonObject {
        val value = root.get(key)
        require(value != null && value.isJsonObject)
        return value.asJsonObject
    }

    private fun requiredArray(root: JsonObject, key: String): JsonArray {
        val value = root.get(key)
        require(value != null && value.isJsonArray)
        return value.asJsonArray
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String): T = enumValueOf(value)

    private fun JsonObject.addNullable(key: String, value: String?) {
        add(key, value?.let(::JsonPrimitive) ?: JsonNull.INSTANCE)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    data class Request(
        val example: HimTrainingExampleV1,
        val durableRoot: File = File(DURABLE_ROOT),
        val enabled: Boolean = true,
    )

    data class PositiveTrainingExampleRecordV1(
        val contractId: String,
        val version: String,
        val state: String,
        val exampleReference: HimTrainingExampleReference,
        val example: HimTrainingExampleV1,
        val logicalDigest: String,
    )

    enum class ResultStatus {
        CREATED,
        ALREADY_PRESENT_IDENTICAL,
    }

    enum class FailureReason {
        INVALID_TRAINING_EXAMPLE,
        INVALID_REFERENCE,
        ARTIFACT_MISSING,
        MALFORMED_ARTIFACT,
        PARTIAL_ARTIFACT,
        REFERENCE_CONTENT_MISMATCH,
        EXISTING_ARTIFACT_CONFLICT,
        UNSAFE_TARGET_PATH,
        READ_FAILED,
        WRITE_FAILED,
        ATOMIC_PUBLICATION_FAILED,
        RELOAD_FAILED,
    }

    class PersistenceFailure(
        val reason: FailureReason,
        val safeContext: String,
    ) : IllegalArgumentException("${reason.name} $safeContext")

    sealed interface Result {
        data class Created(
            val reference: HimTrainingExampleReference,
            val relativePath: String,
            val byteSize: Long,
            val sha256: String,
            val record: PositiveTrainingExampleRecordV1,
        ) : Result

        data class AlreadyPresentIdentical(
            val reference: HimTrainingExampleReference,
            val relativePath: String,
            val byteSize: Long,
            val sha256: String,
            val record: PositiveTrainingExampleRecordV1,
        ) : Result

        data object Disabled : Result

        data class Failed(
            val reason: FailureReason,
            val safeContext: String,
        ) : Result
    }
}
