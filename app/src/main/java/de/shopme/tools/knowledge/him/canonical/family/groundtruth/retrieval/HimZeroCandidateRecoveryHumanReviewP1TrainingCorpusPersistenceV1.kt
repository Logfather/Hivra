package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/** Immutable, content-addressed persistence for an already assembled P1 corpus. */
object HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_PERSISTENCE_V1"
    const val VERSION = "1"
    const val STATE = "P1_TRAINING_CORPUS_PERSISTED"
    const val DURABLE_ROOT = "data/knowledge/him/training/corpora/v1"
    const val FILE_SUFFIX = ".training-corpus.v1.json"

    private const val SNAPSHOT_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_SNAPSHOT_V1"
    private const val POSITIVE_MEMBER_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_POSITIVE_MEMBER_V1"
    private const val NEGATIVE_MEMBER_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_NEGATIVE_MEMBER_V1"
    private const val CORPUS_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_LOGICAL_DIGEST_V1"
    private val ZERO_DIGEST = "0".repeat(64)
    private val SHA256 = Regex("[0-9a-f]{64}")
    private val SNAPSHOT_ID = Regex("training-corpus:v1:([0-9a-f]{64})")
    private val MEMBER_REFERENCE = Regex("corpus-member:v1:(positive|negative):[0-9a-f]{64}")
    private val JSON_KEYS = setOf(
        "contractId", "version", "state", "assemblyContractId", "assemblyVersion",
        "assemblyState", "snapshotId", "corpusLogicalDigest", "members", "counters",
        "recordLogicalDigest",
    )

    fun execute(request: Request): Result {
        return try {
            val record = buildRecord(request.corpus)
            val bytes = serializeRecord(record)
            publishOrReuse(pathFor(record.corpusLogicalDigest.value, request.durableRoot), record, bytes)
        } catch (failure: PersistenceFailure) {
            Result.Failed(failure.reason, failure.safeContext)
        } catch (_: IllegalArgumentException) {
            Result.Failed(FailureReasonV1.INVALID_ASSEMBLED_CORPUS, "corpus")
        } catch (_: Throwable) {
            Result.Failed(FailureReasonV1.WRITE_FAILED, "persistence")
        }
    }

    fun buildRecord(
        corpus: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1,
    ): Record {
        validateCorpus(corpus)
        val unsigned = Record(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            assemblyContractId = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CONTRACT_ID,
            assemblyVersion = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.VERSION,
            assemblyState = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.STATE,
            snapshotId = expectedSnapshotId(corpus.logicalDigest),
            corpusLogicalDigest = corpus.logicalDigest,
            members = corpus.members,
            counters = corpus.counters,
            recordLogicalDigest = ZERO_DIGEST,
        )
        return unsigned.copy(recordLogicalDigest = recordLogicalDigest(unsigned))
    }

    fun serializeRecord(record: Record): ByteArray {
        validateRecord(record)
        return (encodeRecord(record).toString() + "\n").toByteArray(StandardCharsets.UTF_8)
    }

    fun deserializeRecord(bytes: ByteArray): Record {
        return try {
            requireSingleTrailingLf(bytes)
            ensureNoDuplicateJsonFields(bytes.copyOf(bytes.size - 1))
            val root = JsonParser.parseString(bytes.copyOf(bytes.size - 1).toString(StandardCharsets.UTF_8))
                .asJsonObject
            val record = decodeRecord(root)
            require(encodeRecord(record).toString() + "\n" == bytes.toString(StandardCharsets.UTF_8))
            validateRecord(record)
            record
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReasonV1.MALFORMED_EXISTING_ARTIFACT, "record")
        }
    }

    fun readByCorpusLogicalDigest(
        corpusLogicalDigest: String,
        durableRoot: File = File(DURABLE_ROOT),
    ): Record {
        if (!SHA256.matches(corpusLogicalDigest)) {
            throw PersistenceFailure(FailureReasonV1.INVALID_CORPUS_IDENTITY, "digest")
        }
        val path = pathFor(corpusLogicalDigest, durableRoot).toPath()
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw PersistenceFailure(FailureReasonV1.ARTIFACT_MISSING, "record")
        }
        val record = deserializeRecord(Files.readAllBytes(path))
        require(record.corpusLogicalDigest.value == corpusLogicalDigest) {
            "CORPUS_IDENTITY_MISMATCH"
        }
        return record
    }

    fun pathFor(
        corpusLogicalDigest: String,
        durableRoot: File = File(DURABLE_ROOT),
    ): File {
        if (!SHA256.matches(corpusLogicalDigest)) {
            throw PersistenceFailure(FailureReasonV1.INVALID_CORPUS_IDENTITY, "digest")
        }
        val root = safeRoot(durableRoot)
        val target = root.resolve(corpusLogicalDigest + FILE_SUFFIX).normalize()
        if (!target.startsWith(root) || hasSymlinkComponentWithin(target, root)) {
            throw PersistenceFailure(FailureReasonV1.UNSAFE_PATH, "target")
        }
        return target.toFile()
    }

    fun corpusLogicalDigest(
        corpus: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1,
    ): HimSha256 = HimSha256(
        sha256(buildString {
            field("domain", CORPUS_DIGEST_DOMAIN)
            field("contractId", HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CONTRACT_ID)
            field("version", HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.VERSION)
            field("state", HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.STATE)
            corpus.members.forEachIndexed { index, member -> field("member-$index", memberKey(member)) }
            field("total", corpus.counters.total.toString())
            field("positive", corpus.counters.positive.toString())
            field("negative", corpus.counters.negative.toString())
        }),
    )

    fun recordLogicalDigest(record: Record): String =
        sha256(encodeRecord(record.copy(recordLogicalDigest = ZERO_DIGEST)).toString())

    fun validateCorpus(corpus: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1) {
        require(corpus.members == canonicalMembers(corpus.members))
        require(corpus.counters == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CountersV1.from(corpus.members))
        require(corpus.logicalDigest == corpusLogicalDigest(corpus))
        require(corpus.members.map { it.membershipReference }.distinct().size == corpus.members.size)
        corpus.members.forEach(::validateMember)
    }

    fun validateRecord(record: Record) {
        require(record.contractId == CONTRACT_ID)
        require(record.version == VERSION)
        require(record.state == STATE)
        require(record.assemblyContractId == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CONTRACT_ID)
        require(record.assemblyVersion == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.VERSION)
        require(record.assemblyState == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.STATE)
        require(SNAPSHOT_ID.matches(record.snapshotId))
        require(record.snapshotId == expectedSnapshotId(record.corpusLogicalDigest))
        require(SHA256.matches(record.corpusLogicalDigest.value))
        val corpus = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1(
            members = record.members,
            counters = record.counters,
            logicalDigest = record.corpusLogicalDigest,
        )
        validateCorpus(corpus)
        require(SHA256.matches(record.recordLogicalDigest))
        require(record.recordLogicalDigest == recordLogicalDigest(record))
    }

    private fun validateMember(
        member: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.MemberV1,
    ) {
        require(MEMBER_REFERENCE.matches(member.membershipReference))
        require(member.modelInput.observedTerm.isNotBlank())
        require(member.modelInput.normalizedObservedTerm.isNotBlank())
        when (member.polarity) {
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.POSITIVE -> {
                val supervision = member.supervision as? HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Positive
                    ?: error("POSITIVE_SUPERVISION_REQUIRED")
                val binding = member.auditBinding as? HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Positive
                    ?: error("POSITIVE_AUDIT_BINDING_REQUIRED")
                require(member.membershipReference == positiveMembershipReference(binding.exampleReference))
                require(binding.exampleReference.value.startsWith("example:v1:"))
                validateTarget(supervision.target)
            }
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.NEGATIVE -> {
                val supervision = member.supervision as? HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Negative
                    ?: error("NEGATIVE_SUPERVISION_REQUIRED")
                val binding = member.auditBinding as? HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Negative
                    ?: error("NEGATIVE_AUDIT_BINDING_REQUIRED")
                require(member.membershipReference == negativeMembershipReference(binding.p1MaterializationId.value))
                require(binding.p1MaterializationId.value.matches(Regex("negative-materialization:v1:[0-9a-f]{64}")))
                require(binding.negativeExampleReference.value.matches(Regex("negative-example:v1:[0-9a-f]{64}")))
                require(binding.positiveExampleReference.value.startsWith("example:v1:"))
                require(SHA256.matches(binding.negativeRecordLogicalDigest.value))
                validateTarget(supervision.rejectedTarget)
            }
        }
    }

    private fun validateTarget(target: HimTrainingTargetV1) {
        when (target) {
            is HimTrainingTargetV1.ExistingCanonical -> require(target.canonicalId.value.isNotBlank())
            is HimTrainingTargetV1.Identity -> require(target.parentCanonicalId.value.isNotBlank())
            is HimTrainingTargetV1.Variant -> require(target.scope.canonicalId.value.isNotBlank())
            is HimTrainingTargetV1.Alias -> require(target.equivalentEntity.canonicalId.value.isNotBlank())
            is HimTrainingTargetV1.NewCanonical -> require(target.proposedCanonicalName == null || target.proposedCanonicalName.isNotBlank())
        }
    }

    private fun canonicalMembers(
        members: List<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.MemberV1>,
    ) = members.sortedWith(compareBy({ it.polarity.ordinal }, { it.membershipReference }))

    private fun publishOrReuse(path: File, record: Record, bytes: ByteArray): Result.Completed {
        val root = requireNotNull(path.parentFile) { "PERSISTENCE_PARENT_REQUIRED" }
        val rootPath = root.toPath()
        if (Files.isSymbolicLink(rootPath)) throw PersistenceFailure(FailureReasonV1.UNSAFE_PATH, "root")
        try {
            Files.createDirectories(rootPath)
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReasonV1.WRITE_FAILED, "root")
        }
        if (hasSymlinkComponentWithin(rootPath, rootPath)) {
            throw PersistenceFailure(FailureReasonV1.UNSAFE_PATH, "root")
        }
        val target = path.toPath()
        val temporary = root.resolve(".${path.name}.tmp").toPath()
        if (Files.isSymbolicLink(target)) throw PersistenceFailure(FailureReasonV1.UNSAFE_PATH, "target")
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
                throw PersistenceFailure(FailureReasonV1.TEMPORARY_FILE_CONFLICT, "temporary")
            }
            val existing = try {
                Files.readAllBytes(target)
            } catch (_: Throwable) {
                throw PersistenceFailure(FailureReasonV1.MALFORMED_EXISTING_ARTIFACT, "record")
            }
            val existingRecord = deserializeRecord(existing)
            if (!existing.contentEquals(bytes) || existingRecord != record) {
                throw PersistenceFailure(FailureReasonV1.EXISTING_ARTIFACT_CONFLICT, "record")
            }
            return completed(PersistenceStatusV1.ALREADY_PRESENT_IDENTICAL, record, path, existing)
        }
        if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
            throw PersistenceFailure(FailureReasonV1.TEMPORARY_FILE_CONFLICT, "temporary")
        }
        try {
            Files.write(temporary, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            FileOutputStream(temporary.toFile(), true).use { it.fd.sync() }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target)
            }
        } catch (_: FileAlreadyExistsException) {
            throw PersistenceFailure(FailureReasonV1.EXISTING_ARTIFACT_CONFLICT, "record")
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReasonV1.WRITE_FAILED, "write")
        } finally {
            Files.deleteIfExists(temporary)
        }
        val persisted = try {
            Files.readAllBytes(target)
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReasonV1.RELOAD_VALIDATION_FAILED, "record")
        }
        if (!persisted.contentEquals(bytes)) throw PersistenceFailure(FailureReasonV1.RELOAD_VALIDATION_FAILED, "reload")
        val reloaded = deserializeRecord(persisted)
        if (reloaded != record) throw PersistenceFailure(FailureReasonV1.RELOAD_VALIDATION_FAILED, "record")
        return completed(PersistenceStatusV1.CREATED, record, path, persisted)
    }

    private fun completed(
        status: PersistenceStatusV1,
        record: Record,
        path: File,
        bytes: ByteArray,
    ) = Result.Completed(status, record, path.name, bytes.size.toLong(), sha256(bytes))

    private fun encodeRecord(record: Record) = JsonObject().apply {
        addProperty("contractId", record.contractId)
        addProperty("version", record.version)
        addProperty("state", record.state)
        addProperty("assemblyContractId", record.assemblyContractId)
        addProperty("assemblyVersion", record.assemblyVersion)
        addProperty("assemblyState", record.assemblyState)
        addProperty("snapshotId", record.snapshotId)
        addProperty("corpusLogicalDigest", record.corpusLogicalDigest.value)
        add("members", JsonArray().also { array -> record.members.forEach { array.add(encodeMember(it)) } })
        add("counters", JsonObject().apply {
            addProperty("total", record.counters.total)
            addProperty("positive", record.counters.positive)
            addProperty("negative", record.counters.negative)
        })
        addProperty("recordLogicalDigest", record.recordLogicalDigest)
    }

    private fun encodeMember(member: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.MemberV1) = JsonObject().apply {
        addProperty("polarity", member.polarity.name)
        addProperty("membershipReference", member.membershipReference)
        add("modelInput", encodeInput(member.modelInput))
        add("supervision", encodeSupervision(member.supervision))
        add("auditBinding", encodeAuditBinding(member.auditBinding))
    }

    private fun encodeSupervision(supervision: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1) = when (supervision) {
        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Positive -> JsonObject().apply {
            addProperty("kind", "Positive")
            add("target", encodeTarget(supervision.target))
        }
        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Negative -> JsonObject().apply {
            addProperty("kind", "Negative")
            add("rejectedTarget", encodeTarget(supervision.rejectedTarget))
            addProperty("boundaryType", supervision.boundaryType.name)
        }
    }

    private fun encodeAuditBinding(binding: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1) = when (binding) {
        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Positive -> JsonObject().apply {
            addProperty("kind", "Positive")
            addProperty("exampleReference", binding.exampleReference.value)
        }
        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Negative -> JsonObject().apply {
            addProperty("kind", "Negative")
            addProperty("p1MaterializationId", binding.p1MaterializationId.value)
            addProperty("negativeExampleReference", binding.negativeExampleReference.value)
            addProperty("positiveExampleReference", binding.positiveExampleReference.value)
            addProperty("negativeRecordLogicalDigest", binding.negativeRecordLogicalDigest.value)
        }
    }

    private fun encodeInput(input: HimTrainingInputV1) = JsonObject().apply {
        addProperty("observedTerm", input.observedTerm)
        addProperty("normalizedObservedTerm", input.normalizedObservedTerm)
        add("canonicalContext", JsonArray().also { array -> input.canonicalContext.forEach { context ->
            array.add(JsonObject().apply {
                addProperty("rank", context.rank)
                addProperty("canonicalId", context.canonicalId.value)
                addProperty("canonicalName", context.canonicalName)
                addNullable("fullRecordCanonicalJson", context.fullRecordCanonicalJson)
            })
        } })
        add("evidence", JsonArray().also { array -> input.evidence.forEach { evidence ->
            array.add(JsonObject().apply {
                add("reference", encodeEvidenceReference(evidence.reference))
                addProperty("recordKind", evidence.recordKind)
                addProperty("retrievalRank", evidence.retrievalRank)
            })
        } })
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

    private fun decodeRecord(root: JsonObject): Record {
        requireKeys(root, JSON_KEYS)
        val countersRoot = requiredObject(root, "counters")
        requireKeys(countersRoot, setOf("total", "positive", "negative"))
        return Record(
            contractId = requiredString(root, "contractId"),
            version = requiredString(root, "version"),
            state = requiredString(root, "state"),
            assemblyContractId = requiredString(root, "assemblyContractId"),
            assemblyVersion = requiredString(root, "assemblyVersion"),
            assemblyState = requiredString(root, "assemblyState"),
            snapshotId = requiredString(root, "snapshotId"),
            corpusLogicalDigest = HimSha256(requiredString(root, "corpusLogicalDigest")),
            members = requiredArray(root, "members").map { decodeMember(it.asJsonObject) },
            counters = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CountersV1(
                total = requiredInt(countersRoot, "total"),
                positive = requiredInt(countersRoot, "positive"),
                negative = requiredInt(countersRoot, "negative"),
            ),
            recordLogicalDigest = requiredString(root, "recordLogicalDigest"),
        )
    }

    private fun decodeMember(root: JsonObject): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.MemberV1 {
        requireKeys(root, setOf("polarity", "membershipReference", "modelInput", "supervision", "auditBinding"))
        return HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.MemberV1(
            polarity = enumValue(requiredString(root, "polarity")),
            membershipReference = requiredString(root, "membershipReference"),
            modelInput = decodeInput(requiredObject(root, "modelInput")),
            supervision = decodeSupervision(requiredObject(root, "supervision")),
            auditBinding = decodeAuditBinding(requiredObject(root, "auditBinding")),
        )
    }

    private fun decodeSupervision(root: JsonObject) = when (val kind = requiredString(root, "kind")) {
        "Positive" -> {
            requireKeys(root, setOf("kind", "target"))
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Positive(
                decodeTarget(requiredObject(root, "target")),
            )
        }
        "Negative" -> {
            requireKeys(root, setOf("kind", "rejectedTarget", "boundaryType"))
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Negative(
                rejectedTarget = decodeTarget(requiredObject(root, "rejectedTarget")),
                boundaryType = enumValue(requiredString(root, "boundaryType")),
            )
        }
        else -> error("Unknown supervision kind: $kind")
    }

    private fun decodeAuditBinding(root: JsonObject) = when (val kind = requiredString(root, "kind")) {
        "Positive" -> {
            requireKeys(root, setOf("kind", "exampleReference"))
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Positive(
                HimTrainingExampleReference(requiredString(root, "exampleReference")),
            )
        }
        "Negative" -> {
            requireKeys(root, setOf("kind", "p1MaterializationId", "negativeExampleReference", "positiveExampleReference", "negativeRecordLogicalDigest"))
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Negative(
                p1MaterializationId = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1(requiredString(root, "p1MaterializationId")),
                negativeExampleReference = de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleReferenceV1(requiredString(root, "negativeExampleReference")),
                positiveExampleReference = HimTrainingExampleReference(requiredString(root, "positiveExampleReference")),
                negativeRecordLogicalDigest = HimSha256(requiredString(root, "negativeRecordLogicalDigest")),
            )
        }
        else -> error("Unknown audit binding kind: $kind")
    }

    private fun decodeInput(root: JsonObject): HimTrainingInputV1 {
        requireKeys(root, setOf("observedTerm", "normalizedObservedTerm", "canonicalContext", "evidence"))
        return HimTrainingInputV1(
            observedTerm = requiredString(root, "observedTerm"),
            normalizedObservedTerm = requiredString(root, "normalizedObservedTerm"),
            canonicalContext = requiredArray(root, "canonicalContext").map { element ->
                val context = element.asJsonObject
                requireKeys(context, setOf("rank", "canonicalId", "canonicalName", "fullRecordCanonicalJson"))
                HimCandidateCanonicalContext(
                    rank = requiredInt(context, "rank"),
                    canonicalId = HimEntityId(requiredString(context, "canonicalId")),
                    canonicalName = requiredString(context, "canonicalName"),
                    fullRecordCanonicalJson = nullableString(context, "fullRecordCanonicalJson"),
                )
            },
            evidence = requiredArray(root, "evidence").map { element ->
                val evidence = element.asJsonObject
                requireKeys(evidence, setOf("reference", "recordKind", "retrievalRank"))
                HimTrainingEvidenceInputV1(
                    reference = decodeEvidenceReference(requiredObject(evidence, "reference")),
                    recordKind = requiredString(evidence, "recordKind"),
                    retrievalRank = requiredInt(evidence, "retrievalRank"),
                )
            },
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

    private fun decodeTarget(root: JsonObject): HimTrainingTargetV1 = when (val type = requiredString(root, "targetType")) {
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
        else -> error("Unknown target type: $type")
    }

    private fun decodeFamilyReference(root: JsonObject): HimFamilyEntityReference = when (val type = requiredString(root, "referenceType")) {
        "Canonical" -> {
            requireKeys(root, setOf("referenceType", "canonicalId"))
            HimFamilyEntityReference.Canonical(HimEntityId(requiredString(root, "canonicalId")))
        }
        "Identity" -> {
            requireKeys(root, setOf("referenceType", "canonicalId", "identityId"))
            HimFamilyEntityReference.Identity(
                HimEntityId(requiredString(root, "canonicalId")),
                HimEntityId(requiredString(root, "identityId")),
            )
        }
        else -> error("Unknown family reference type: $type")
    }

    private fun memberKey(member: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.MemberV1) = buildString {
        field("polarity", member.polarity.name)
        field("membershipReference", member.membershipReference)
        field("input", inputKey(member.modelInput))
        field("supervision", supervisionKey(member.supervision))
        field("auditBinding", auditBindingKey(member.auditBinding))
    }

    private fun supervisionKey(supervision: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1) = when (supervision) {
        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Positive -> "POSITIVE|${targetKey(supervision.target)}"
        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Negative -> "NEGATIVE|${targetKey(supervision.rejectedTarget)}|${supervision.boundaryType.name}"
    }

    private fun auditBindingKey(binding: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1) = when (binding) {
        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Positive -> "POSITIVE|${binding.exampleReference.value}"
        is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Negative ->
            "NEGATIVE|${binding.p1MaterializationId.value}|${binding.negativeExampleReference.value}|${binding.positiveExampleReference.value}|${binding.negativeRecordLogicalDigest.value}"
    }

    private fun inputKey(input: HimTrainingInputV1) = buildString {
        field("observedTerm", input.observedTerm)
        field("normalizedObservedTerm", input.normalizedObservedTerm)
        input.canonicalContext.forEachIndexed { index, context ->
            field("context-$index-rank", context.rank.toString())
            field("context-$index-id", context.canonicalId.value)
            field("context-$index-name", context.canonicalName)
            field("context-$index-record", context.fullRecordCanonicalJson.orEmpty())
        }
        input.evidence.sortedWith(compareBy({ it.reference.source }, { it.reference.sourceRecordIdentity }, { it.retrievalRank }))
            .forEachIndexed { index, evidence ->
                field("evidence-$index-source", evidence.reference.source)
                field("evidence-$index-artifact", evidence.reference.sourceArtifactSha256.value)
                field("evidence-$index-record", evidence.reference.sourceRecordIdentity)
                field("evidence-$index-kind", evidence.recordKind)
                field("evidence-$index-rank", evidence.retrievalRank.toString())
            }
    }

    private fun targetKey(target: HimTrainingTargetV1): String = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> "EXISTING_CANONICAL|${target.canonicalId.value}"
        is HimTrainingTargetV1.Identity -> "IDENTITY|${target.parentCanonicalId.value}"
        is HimTrainingTargetV1.Variant -> "VARIANT|${target.scope.canonicalId.value}|${target.scope::class.simpleName}|${target.scope}"
        is HimTrainingTargetV1.Alias -> "ALIAS|${target.equivalentEntity.canonicalId.value}|${target.equivalentEntity::class.simpleName}|${target.equivalentEntity}"
        is HimTrainingTargetV1.NewCanonical -> "NEW_CANONICAL|${target.proposedCanonicalName.orEmpty()}"
    }

    private fun positiveMembershipReference(reference: HimTrainingExampleReference) =
        "corpus-member:v1:positive:${sha256(canonicalIdentity(POSITIVE_MEMBER_DOMAIN, reference.value))}"

    private fun negativeMembershipReference(materializationId: String) =
        "corpus-member:v1:negative:${sha256(canonicalIdentity(NEGATIVE_MEMBER_DOMAIN, materializationId))}"

    private fun expectedSnapshotId(digest: HimSha256) = "training-corpus:v1:${digest.value}"

    private fun canonicalIdentity(domain: String, value: String) = buildString {
        field("domain", domain)
        field("value", value)
    }

    private fun StringBuilder.field(name: String, value: String) {
        append(name).append('=').append(value.length).append(':').append(value).append('\n')
    }

    private fun requireSingleTrailingLf(bytes: ByteArray) {
        require(bytes.isNotEmpty() && bytes.last() == '\n'.code.toByte())
        require(bytes.size == 1 || bytes[bytes.size - 2] != '\n'.code.toByte())
        require(bytes.dropLast(1).none { it == '\r'.code.toByte() || it == '\n'.code.toByte() })
    }

    private fun ensureNoDuplicateJsonFields(bytes: ByteArray) {
        val reader = JsonReader(InputStreamReader(ByteArrayInputStream(bytes), StandardCharsets.UTF_8))
        reader.isLenient = false
        consume(reader)
        require(reader.peek() == JsonToken.END_DOCUMENT)
    }

    private fun consume(reader: JsonReader) {
        when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                val names = mutableSetOf<String>()
                while (reader.peek() != JsonToken.END_OBJECT) {
                    require(names.add(reader.nextName()))
                    consume(reader)
                }
                reader.endObject()
            }
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.peek() != JsonToken.END_ARRAY) consume(reader)
                reader.endArray()
            }
            JsonToken.STRING, JsonToken.NUMBER -> reader.nextString()
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NULL -> reader.nextNull()
            else -> error("Invalid JSON")
        }
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

    private fun safeRoot(durableRoot: File): Path {
        val absolute = durableRoot.toPath().toAbsolutePath().normalize()
        require(absolute.parent != null) { "UNSAFE_ROOT" }
        var cursor: Path? = absolute
        while (cursor != null) {
            if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(cursor)) {
                throw PersistenceFailure(FailureReasonV1.UNSAFE_PATH, "root")
            }
            cursor = cursor.parent
        }
        return absolute.toFile().canonicalFile.toPath()
    }

    private fun hasSymlinkComponentWithin(path: Path, boundary: Path): Boolean {
        var cursor: Path? = path
        while (cursor != null && cursor.startsWith(boundary)) {
            if (cursor != boundary && Files.isSymbolicLink(cursor)) return true
            cursor = cursor.parent
        }
        return false
    }

    private fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    data class Request(
        val corpus: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1,
        val durableRoot: File = File(DURABLE_ROOT),
    )

    data class Record(
        val contractId: String,
        val version: String,
        val state: String,
        val assemblyContractId: String,
        val assemblyVersion: String,
        val assemblyState: String,
        val snapshotId: String,
        val corpusLogicalDigest: HimSha256,
        val members: List<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.MemberV1>,
        val counters: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CountersV1,
        val recordLogicalDigest: String,
    )

    enum class PersistenceStatusV1 {
        CREATED,
        ALREADY_PRESENT_IDENTICAL,
    }

    enum class FailureReasonV1 {
        INVALID_ASSEMBLED_CORPUS,
        INVALID_CORPUS_IDENTITY,
        ARTIFACT_MISSING,
        MALFORMED_EXISTING_ARTIFACT,
        EXISTING_ARTIFACT_CONFLICT,
        UNSAFE_PATH,
        TEMPORARY_FILE_CONFLICT,
        RELOAD_VALIDATION_FAILED,
        WRITE_FAILED,
    }

    sealed interface Result {
        data class Completed(
            val status: PersistenceStatusV1,
            val record: Record,
            val relativePath: String,
            val byteSize: Long,
            val sha256: String,
        ) : Result

        data class Failed(
            val reason: FailureReasonV1,
            val safeContext: String,
        ) : Result
    }

    class PersistenceFailure(
        val reason: FailureReasonV1,
        val safeContext: String,
    ) : IllegalArgumentException("${reason.name} $safeContext")
}
