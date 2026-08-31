package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimMutationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputRunReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateRunReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTeacherProvenanceV1
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

/** Durable, immutable persistence for a validated P1 negative training example. */
object HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_TRAINING_EXAMPLE_PERSISTENCE_V1"
    const val VERSION = "1"
    const val STATE = "P1_NEGATIVE_TRAINING_EXAMPLE_PERSISTED"
    const val DURABLE_ROOT = "data/knowledge/him/training/negative-examples/v1"
    const val FILE_SUFFIX = ".negative-training-example.v1.json"

    private val ZERO_DIGEST = "0".repeat(64)
    private val SHA256 = Regex("[0-9a-f]{64}")
    private val MATERIALIZATION_ID = Regex("negative-materialization:v1:([0-9a-f]{64})")

    fun execute(request: Request): Result {
        if (!request.enabled) return Result.Disabled
        return try {
            val record = recordFrom(request.materialized)
            publishOrReuse(pathFor(record.p1MaterializationId, request.durableRoot), record, serializeRecord(record))
        } catch (failure: PersistenceFailure) {
            Result.Failed(failure.reason, failure.safeContext)
        } catch (_: IllegalArgumentException) {
            Result.Failed(FailureReason.INVALID_MATERIALIZED_INPUT, "materialized")
        } catch (_: Throwable) {
            Result.Failed(FailureReason.WRITE_FAILED, "persistence")
        }
    }

    fun recordFrom(
        materialized: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.Materialized,
    ): Record {
        val example = canonicalizeExample(materialized.negativeTrainingExample)
        require(example.provenance.positiveExampleReference == example.positiveExample.exampleReference)
        require(materialized.p1Lineage.positiveTrainingExampleReference == example.positiveExample.exampleReference)
        require(materialized.p1Lineage.rejectedTarget == example.rejectedTarget)
        require(materialized.p1Lineage.boundaryType == example.boundaryType)
        require(materialized.materializationId == expectedMaterializationId(materialized.p1Lineage.negativeSupervisionRecordId, example.reference.value))
        val bindings = materialized.evidenceBindings.sortedWith(evidenceBindingComparator)
        require(bindings.isNotEmpty())
        require(bindings.map { it.persistedEvidenceReferenceId }.distinct().size == bindings.size)
        require(bindings.map { it.persistedEvidenceReferenceId }.toSet() == materialized.p1Lineage.evidenceReferenceIds.toSet())
        require(bindings.all { it.trainingEvidence.reference in example.positiveExample.input.evidence.map { evidence -> evidence.reference } })
        return Record(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            p1MaterializationId = materialized.materializationId,
            genericNegativeExample = example,
            p1Lineage = materialized.p1Lineage,
            evidenceBindings = bindings,
            recordLogicalDigest = "",
        ).let { unsigned -> unsigned.copy(recordLogicalDigest = recordLogicalDigest(unsigned)) }
    }

    fun serializeRecord(record: Record): ByteArray {
        val canonical = canonicalizeRecord(record)
        validateRecord(canonical)
        return (encodeRecord(canonical).toString() + "\n").toByteArray(StandardCharsets.UTF_8)
    }

    fun deserializeRecord(bytes: ByteArray): Record {
        return try {
            requireSingleTrailingLf(bytes)
            val root = parseStrictJson(bytes.copyOf(bytes.size - 1))
            requireKeys(root, RECORD_KEYS)
            val record = Record(
                contractId = requiredString(root, "contractId"),
                version = requiredString(root, "version"),
                state = requiredString(root, "state"),
                p1MaterializationId = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1(
                    requiredString(root, "p1MaterializationId"),
                ),
                genericNegativeExample = decodeNegativeExample(requiredObject(root, "genericNegativeExample")),
                p1Lineage = decodeLineage(requiredObject(root, "p1Lineage")),
                evidenceBindings = requiredArray(root, "evidenceBindings").map { decodeEvidenceBinding(it.asJsonObject) },
                recordLogicalDigest = requiredString(root, "recordLogicalDigest"),
            )
            val canonical = canonicalizeRecord(record)
            require(encodeRecord(canonical).toString() + "\n" == bytes.toString(StandardCharsets.UTF_8))
            validateRecord(canonical)
            canonical
        } catch (failure: PersistenceFailure) {
            throw failure
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReason.MALFORMED_EXISTING_ARTIFACT, "record")
        }
    }

    fun readByMaterializationId(
        materializationId: String,
        durableRoot: File = File(DURABLE_ROOT),
    ): Record {
        val id = try {
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1(materializationId)
        } catch (_: Throwable) {
            throw PersistenceFailure(FailureReason.INVALID_MATERIALIZATION_ID, "materialization")
        }
        val path = pathFor(id, durableRoot).toPath()
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw PersistenceFailure(FailureReason.ARTIFACT_MISSING, "record")
        }
        val record = deserializeRecord(Files.readAllBytes(path))
        if (record.p1MaterializationId != id) {
            throw PersistenceFailure(FailureReason.MALFORMED_EXISTING_ARTIFACT, "record")
        }
        return record
    }

    fun pathFor(
        materializationId: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1,
        durableRoot: File = File(DURABLE_ROOT),
    ): File {
        require(MATERIALIZATION_ID.matches(materializationId.value))
        val root = safeRoot(durableRoot)
        val digest = MATERIALIZATION_ID.matchEntire(materializationId.value)!!.groupValues[1]
        val target = root.resolve("negative-materialization-v1-$digest$FILE_SUFFIX").normalize()
        if (!target.startsWith(root) || hasSymlinkComponentWithin(target, root)) {
            throw PersistenceFailure(FailureReason.UNSAFE_PATH, "target")
        }
        return target.toFile()
    }

    fun recordLogicalDigest(record: Record): String = sha256(encodeRecord(record.copy(recordLogicalDigest = ZERO_DIGEST)).toString())

    fun validateRecord(record: Record) {
        require(record.contractId == CONTRACT_ID)
        require(record.version == VERSION)
        require(record.state == STATE)
        require(MATERIALIZATION_ID.matches(record.p1MaterializationId.value))
        require(record.p1MaterializationId == expectedMaterializationId(record.p1Lineage.negativeSupervisionRecordId, record.genericNegativeExample.reference.value))
        val example = canonicalizeExample(record.genericNegativeExample)
        require(example == record.genericNegativeExample)
        require(example.provenance.positiveExampleReference == example.positiveExample.exampleReference)
        require(example.reference == expectedNegativeReference(example))
        require(record.p1Lineage.positiveTrainingExampleReference == example.positiveExample.exampleReference)
        require(record.p1Lineage.rejectedTarget == example.rejectedTarget)
        require(record.p1Lineage.boundaryType == example.boundaryType)
        require(record.p1Lineage.canonicalEntityId == canonicalId(example.positiveExample.target))
        require(validLineage(record.p1Lineage))
        require(record.evidenceBindings == record.evidenceBindings.sortedWith(evidenceBindingComparator))
        require(record.evidenceBindings.map { it.persistedEvidenceReferenceId }.distinct().size == record.evidenceBindings.size)
        require(record.evidenceBindings.map { it.persistedEvidenceReferenceId }.toSet() == record.p1Lineage.evidenceReferenceIds.toSet())
        val inputReferences = example.positiveExample.input.evidence.map { it.reference }.toSet()
        val provenanceReferences = example.positiveExample.provenance.sourceEvidenceReferences.toSet()
        require(record.evidenceBindings.all { it.trainingEvidence.reference in inputReferences && it.trainingEvidence.reference in provenanceReferences })
        require(SHA256.matches(record.recordLogicalDigest))
        require(record.recordLogicalDigest == recordLogicalDigest(record))
    }

    private fun publishOrReuse(path: File, record: Record, bytes: ByteArray): Result.Completed {
        val root = requireNotNull(path.parentFile) { "PERSISTENCE_PARENT_REQUIRED" }
        val rootPath = root.toPath()
        if (Files.isSymbolicLink(rootPath)) throw PersistenceFailure(FailureReason.UNSAFE_PATH, "root")
        Files.createDirectories(rootPath)
        if (hasSymlinkComponentWithin(rootPath, rootPath)) throw PersistenceFailure(FailureReason.UNSAFE_PATH, "root")
        val target = path.toPath()
        val temporary = root.resolve(".${path.name}.tmp").toPath()
        if (Files.isSymbolicLink(target)) throw PersistenceFailure(FailureReason.UNSAFE_PATH, "target")
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) throw PersistenceFailure(FailureReason.TEMPORARY_FILE_CONFLICT, "temporary")
            val existing = try { Files.readAllBytes(target) } catch (_: Throwable) {
                throw PersistenceFailure(FailureReason.MALFORMED_EXISTING_ARTIFACT, "record")
            }
            val existingRecord = deserializeRecord(existing)
            if (!existing.contentEquals(bytes) || existingRecord != record) {
                throw PersistenceFailure(FailureReason.EXISTING_ARTIFACT_CONFLICT, "record")
            }
            return completed(PersistenceStatus.ALREADY_PRESENT_IDENTICAL, record, path, existing)
        }
        if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) throw PersistenceFailure(FailureReason.TEMPORARY_FILE_CONFLICT, "temporary")
        try {
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
            throw PersistenceFailure(FailureReason.WRITE_FAILED, "write")
        } finally {
            Files.deleteIfExists(temporary)
        }
        val persisted = try { Files.readAllBytes(target) } catch (_: Throwable) {
            throw PersistenceFailure(FailureReason.RELOAD_VALIDATION_FAILED, "record")
        }
        if (!persisted.contentEquals(bytes)) throw PersistenceFailure(FailureReason.RELOAD_VALIDATION_FAILED, "reload")
        val reloaded = deserializeRecord(persisted)
        if (reloaded != record) throw PersistenceFailure(FailureReason.RELOAD_VALIDATION_FAILED, "record")
        return completed(PersistenceStatus.CREATED, record, path, persisted)
    }

    private fun completed(status: PersistenceStatus, record: Record, path: File, bytes: ByteArray) = Result.Completed(
        status = status,
        record = record,
        relativePath = path.name,
        byteSize = bytes.size.toLong(),
        sha256 = sha256(bytes),
    )

    private fun canonicalizeRecord(record: Record): Record = record.copy(
        genericNegativeExample = canonicalizeExample(record.genericNegativeExample),
        evidenceBindings = record.evidenceBindings.sortedWith(evidenceBindingComparator),
    )

    private fun canonicalizeExample(example: HimNegativeTrainingExampleV1): HimNegativeTrainingExampleV1 {
        val positive = example.positiveExample
        val input = positive.input.copy(evidence = positive.input.evidence.sortedWith(trainingEvidenceComparator))
        val provenance = positive.provenance.copy(
            sourceEvidenceReferences = positive.provenance.sourceEvidenceReferences.sortedWith(referenceComparator),
            sourceArtifactDigests = positive.provenance.sourceArtifactDigests.sortedBy { it.value },
        )
        return example.copy(positiveExample = positive.copy(input = input, provenance = provenance))
    }

    private fun expectedNegativeReference(example: HimNegativeTrainingExampleV1) = HimNegativeTrainingExampleV1.create(
        positiveExample = example.positiveExample,
        rejectedTarget = example.rejectedTarget,
        boundaryType = example.boundaryType,
    ).reference

    private fun canonicalId(target: HimTrainingTargetV1): String? = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> target.canonicalId.value
        is HimTrainingTargetV1.Identity -> target.parentCanonicalId.value
        is HimTrainingTargetV1.Variant -> target.scope.canonicalId.value
        is HimTrainingTargetV1.Alias -> target.equivalentEntity.canonicalId.value
        is HimTrainingTargetV1.NewCanonical -> null
    }

    private fun validLineage(
        lineage: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.P1Lineage,
    ): Boolean =
        SHA256.matches(lineage.negativeSupervisionRecordId) &&
            SHA256.matches(lineage.negativeCandidateId) &&
            SHA256.matches(lineage.readinessDecisionId) &&
            SHA256.matches(lineage.eligibilityInputBindingDigest) &&
            SHA256.matches(lineage.eligibilityDecisionId) &&
            SHA256.matches(lineage.validationBatchBindingDigest) &&
            SHA256.matches(lineage.validationBatchLogicalDigest) &&
            SHA256.matches(lineage.validationRecordId) &&
            lineage.reviewUnitId.isNotBlank() && lineage.stableEntryId.isNotBlank() &&
            lineage.canonicalEntityId.isNotBlank() && lineage.originalReviewerRef.isNotBlank() &&
            lineage.validatorReviewerRef.isNotBlank() && lineage.validationRound >= 1 &&
            lineage.validationRevision >= 1 && lineage.validationReasonCodes.isNotEmpty() &&
            lineage.evidenceReferenceIds.isNotEmpty() &&
            lineage.evidenceReferenceIds.distinct().size == lineage.evidenceReferenceIds.size &&
            lineage.candidateState == HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE &&
            lineage.originalDecision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION &&
            lineage.downstreamRoute == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE &&
            lineage.occurrenceReference.isNotBlank() &&
            SHA256.matches(lineage.materializationDecisionId) &&
            SHA256.matches(lineage.projectionInputBindingDigest) &&
            SHA256.matches(lineage.proofBindingDigest) &&
            SHA256.matches(lineage.proofLogicalDigest)

    private fun expectedMaterializationId(recordId: String, negativeReference: String) =
        HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1(
            "negative-materialization:v1:${sha256(buildString {
                appendLine("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_TRAINING_EXAMPLE_MATERIALIZATION_ID_V1")
                appendLine("negativeSupervisionRecordId=$recordId")
                appendLine("negativeExampleReference=$negativeReference")
            })}",
        )

    private fun encodeRecord(record: Record) = JsonObject().apply {
        addProperty("contractId", record.contractId)
        addProperty("version", record.version)
        addProperty("state", record.state)
        addProperty("p1MaterializationId", record.p1MaterializationId.value)
        add("genericNegativeExample", encodeNegativeExample(record.genericNegativeExample))
        add("p1Lineage", encodeLineage(record.p1Lineage))
        add("evidenceBindings", JsonArray().also { array -> record.evidenceBindings.forEach { array.add(encodeEvidenceBinding(it)) } })
        addProperty("recordLogicalDigest", record.recordLogicalDigest)
    }

    private fun encodeNegativeExample(example: HimNegativeTrainingExampleV1) = JsonObject().apply {
        addProperty("reference", example.reference.value)
        addProperty("contractVersion", example.contractVersion)
        add("positiveExample", encodeExample(example.positiveExample))
        add("rejectedTarget", encodeTarget(example.rejectedTarget))
        addProperty("boundaryType", example.boundaryType.name)
        add("provenance", JsonObject().apply {
            addProperty("positiveExampleReference", example.provenance.positiveExampleReference.value)
            addProperty("derivationPolicyVersion", example.provenance.derivationPolicyVersion)
            addProperty("rejectedAlternativeReference", example.provenance.rejectedAlternativeReference.value)
        })
    }

    private fun encodeExample(example: HimTrainingExampleV1) = JsonObject().apply {
        addProperty("exampleReference", example.exampleReference.value)
        addProperty("contractVersion", example.contractVersion)
        addProperty("taskType", example.taskType.name)
        add("input", JsonObject().apply {
            addProperty("observedTerm", example.input.observedTerm)
            addProperty("normalizedObservedTerm", example.input.normalizedObservedTerm)
            add("canonicalContext", JsonArray().also { a -> example.input.canonicalContext.forEach { c -> a.add(JsonObject().apply {
                addProperty("rank", c.rank)
                addProperty("canonicalId", c.canonicalId.value)
                addProperty("canonicalName", c.canonicalName)
                addNullable("fullRecordCanonicalJson", c.fullRecordCanonicalJson)
            }) } })
            add("evidence", JsonArray().also { a -> example.input.evidence.forEach { e -> a.add(encodeTrainingEvidence(e)) } })
        })
        add("target", encodeTarget(example.target))
        add("provenance", encodeTrainingProvenance(example.provenance))
    }

    private fun encodeTrainingEvidence(evidence: HimTrainingEvidenceInputV1) = JsonObject().apply {
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
        is HimTrainingTargetV1.ExistingCanonical -> JsonObject().apply { addProperty("targetType", "ExistingCanonical"); addProperty("canonicalId", target.canonicalId.value) }
        is HimTrainingTargetV1.Identity -> JsonObject().apply { addProperty("targetType", "Identity"); addProperty("parentCanonicalId", target.parentCanonicalId.value) }
        is HimTrainingTargetV1.Variant -> JsonObject().apply { addProperty("targetType", "Variant"); add("scope", encodeFamilyReference(target.scope)) }
        is HimTrainingTargetV1.Alias -> JsonObject().apply { addProperty("targetType", "Alias"); add("equivalentEntity", encodeFamilyReference(target.equivalentEntity)) }
        is HimTrainingTargetV1.NewCanonical -> JsonObject().apply { addProperty("targetType", "NewCanonical"); addNullable("proposedCanonicalName", target.proposedCanonicalName) }
    }

    private fun encodeFamilyReference(reference: HimFamilyEntityReference) = JsonObject().apply {
        when (reference) {
            is HimFamilyEntityReference.Canonical -> { addProperty("referenceType", "Canonical"); addProperty("canonicalId", reference.canonicalId.value) }
            is HimFamilyEntityReference.Identity -> { addProperty("referenceType", "Identity"); addProperty("canonicalId", reference.canonicalId.value); addProperty("identityId", reference.identityId.value) }
        }
    }

    private fun encodeTrainingProvenance(provenance: HimTrainingProvenanceV1) = JsonObject().apply {
        addNullable("candidateReference", provenance.candidateReference?.value)
        addNullable("generationRunReference", provenance.generationRunReference?.value)
        addNullable("inputRunReference", provenance.inputRunReference?.value)
        addNullable("validationReference", provenance.validationReference?.value)
        addNullable("promotionReference", provenance.promotionReference?.value)
        addNullable("mutationReference", provenance.mutationReference?.value)
        addNullable("groundTruthReleaseReference", provenance.groundTruthReleaseReference?.value)
        addNullable("promotedEntityId", provenance.promotedEntityId?.value)
        addNullable("promotedEntityType", provenance.promotedEntityType?.name)
        add("sourceEvidenceReferences", JsonArray().also { a -> provenance.sourceEvidenceReferences.forEach { a.add(encodeEvidenceReference(it)) } })
        add("sourceArtifactDigests", JsonArray().also { a -> provenance.sourceArtifactDigests.forEach { a.add(it.value) } })
        addNullable("retrievalFoundationRelease", provenance.retrievalFoundationRelease)
        addNullable("retrievalFoundationReleaseSha256", provenance.retrievalFoundationReleaseSha256?.value)
        addNullable("retrievalFoundationDigest", provenance.retrievalFoundationDigest?.value)
        add("teacher", provenance.teacher?.let { t -> JsonObject().apply { addProperty("provider", t.provider); addProperty("model", t.model); addNullable("configurationFingerprint", t.configurationFingerprint?.value) } } ?: JsonNull.INSTANCE)
    }

    private fun encodeLineage(lineage: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.P1Lineage) = JsonObject().apply {
        addProperty("negativeSupervisionRecordId", lineage.negativeSupervisionRecordId)
        addProperty("negativeCandidateId", lineage.negativeCandidateId)
        addProperty("readinessDecisionId", lineage.readinessDecisionId)
        addProperty("readinessState", lineage.readinessState.name)
        addProperty("eligibilityBatchId", lineage.eligibilityBatchId)
        addProperty("eligibilityInputBindingDigest", lineage.eligibilityInputBindingDigest)
        addProperty("eligibilityDecisionId", lineage.eligibilityDecisionId)
        addProperty("eligibilityState", lineage.eligibilityState.name)
        addProperty("validationBatchId", lineage.validationBatchId)
        addProperty("validationBatchBindingDigest", lineage.validationBatchBindingDigest)
        addProperty("validationBatchLogicalDigest", lineage.validationBatchLogicalDigest)
        addProperty("validationRecordId", lineage.validationRecordId)
        addProperty("reviewUnitId", lineage.reviewUnitId)
        addProperty("stableEntryId", lineage.stableEntryId)
        addProperty("canonicalEntityId", lineage.canonicalEntityId)
        addProperty("originalReviewerRef", lineage.originalReviewerRef)
        addProperty("validatorReviewerRef", lineage.validatorReviewerRef)
        addProperty("validationRound", lineage.validationRound)
        addProperty("validationRevision", lineage.validationRevision)
        addProperty("originalDecision", lineage.originalDecision.name)
        addProperty("assessment", lineage.assessment.name)
        addEnumArray("validationReasonCodes", lineage.validationReasonCodes.map { it.name })
        addStringArray("evidenceReferenceIds", lineage.evidenceReferenceIds)
        addProperty("downstreamRoute", lineage.downstreamRoute.name)
        addProperty("candidateState", lineage.candidateState.name)
        add("rejectedTarget", encodeTarget(lineage.rejectedTarget))
        addProperty("boundaryType", lineage.boundaryType.name)
        addProperty("occurrenceReference", lineage.occurrenceReference)
        addProperty("positiveTrainingExampleReference", lineage.positiveTrainingExampleReference.value)
        addProperty("materializationDecisionId", lineage.materializationDecisionId)
        addProperty("projectionInputBindingDigest", lineage.projectionInputBindingDigest)
        addProperty("proofBindingDigest", lineage.proofBindingDigest)
        addProperty("proofLogicalDigest", lineage.proofLogicalDigest)
    }

    private fun encodeEvidenceBinding(binding: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding) = JsonObject().apply {
        addProperty("persistedEvidenceReferenceId", binding.persistedEvidenceReferenceId)
        add("trainingEvidence", encodeTrainingEvidence(binding.trainingEvidence))
    }

    private fun decodeNegativeExample(root: JsonObject): HimNegativeTrainingExampleV1 {
        requireKeys(root, setOf("reference", "contractVersion", "positiveExample", "rejectedTarget", "boundaryType", "provenance"))
        val provenance = requiredObject(root, "provenance")
        requireKeys(provenance, setOf("positiveExampleReference", "derivationPolicyVersion", "rejectedAlternativeReference"))
        val example = decodeExample(requiredObject(root, "positiveExample"))
        val negative = HimNegativeTrainingExampleV1.create(example, decodeTarget(requiredObject(root, "rejectedTarget")), enumValue(requiredString(root, "boundaryType")))
        require(negative.reference.value == requiredString(root, "reference"))
        require(negative.contractVersion == requiredString(root, "contractVersion"))
        require(negative.provenance.positiveExampleReference.value == requiredString(provenance, "positiveExampleReference"))
        require(negative.provenance.derivationPolicyVersion == requiredString(provenance, "derivationPolicyVersion"))
        require(negative.provenance.rejectedAlternativeReference.value == requiredString(provenance, "rejectedAlternativeReference"))
        return negative
    }

    private fun decodeExample(root: JsonObject): HimTrainingExampleV1 {
        requireKeys(root, setOf("exampleReference", "contractVersion", "taskType", "input", "target", "provenance"))
        val input = requiredObject(root, "input")
        requireKeys(input, setOf("observedTerm", "normalizedObservedTerm", "canonicalContext", "evidence"))
        val context = requiredArray(input, "canonicalContext").map { c ->
            val o = c.asJsonObject; requireKeys(o, setOf("rank", "canonicalId", "canonicalName", "fullRecordCanonicalJson"))
            HimCandidateCanonicalContext(requiredInt(o, "rank"), HimEntityId(requiredString(o, "canonicalId")), requiredString(o, "canonicalName"), nullableString(o, "fullRecordCanonicalJson"))
        }
        val trainingInput = HimTrainingInputV1(
            observedTerm = requiredString(input, "observedTerm"),
            normalizedObservedTerm = requiredString(input, "normalizedObservedTerm"),
            canonicalContext = context,
            evidence = requiredArray(input, "evidence").map { decodeTrainingEvidence(it.asJsonObject) },
        )
        val provenance = decodeTrainingProvenance(requiredObject(root, "provenance"))
        val result = HimTrainingExampleV1(
            exampleReference = HimTrainingExampleReference(requiredString(root, "exampleReference")),
            contractVersion = requiredString(root, "contractVersion"),
            taskType = enumValue(requiredString(root, "taskType")),
            input = trainingInput,
            target = decodeTarget(requiredObject(root, "target")),
            provenance = provenance,
        )
        require(result.exampleReference == HimTrainingExampleIdentityV1.example(result.taskType, result.input, result.target, result.provenance))
        return result
    }

    private fun decodeTrainingEvidence(root: JsonObject): HimTrainingEvidenceInputV1 {
        requireKeys(root, setOf("reference", "recordKind", "retrievalRank"))
        return HimTrainingEvidenceInputV1(decodeEvidenceReference(requiredObject(root, "reference")), requiredString(root, "recordKind"), requiredInt(root, "retrievalRank"))
    }

    private fun decodeEvidenceReference(root: JsonObject): HimEvidenceReference {
        requireKeys(root, setOf("source", "sourceArtifactSha256", "sourceRecordIdentity"))
        return HimEvidenceReference(requiredString(root, "source"), HimSha256(requiredString(root, "sourceArtifactSha256")), requiredString(root, "sourceRecordIdentity"))
    }

    private fun decodeTarget(root: JsonObject): HimTrainingTargetV1 = when (requiredString(root, "targetType")) {
        "ExistingCanonical" -> { requireKeys(root, setOf("targetType", "canonicalId")); HimTrainingTargetV1.ExistingCanonical(HimEntityId(requiredString(root, "canonicalId"))) }
        "Identity" -> { requireKeys(root, setOf("targetType", "parentCanonicalId")); HimTrainingTargetV1.Identity(HimEntityId(requiredString(root, "parentCanonicalId"))) }
        "Variant" -> { requireKeys(root, setOf("targetType", "scope")); HimTrainingTargetV1.Variant(decodeFamilyReference(requiredObject(root, "scope"))) }
        "Alias" -> { requireKeys(root, setOf("targetType", "equivalentEntity")); HimTrainingTargetV1.Alias(decodeFamilyReference(requiredObject(root, "equivalentEntity"))) }
        "NewCanonical" -> { requireKeys(root, setOf("targetType", "proposedCanonicalName")); HimTrainingTargetV1.NewCanonical(nullableString(root, "proposedCanonicalName")) }
        else -> throw IllegalArgumentException("target")
    }

    private fun decodeFamilyReference(root: JsonObject): HimFamilyEntityReference = when (requiredString(root, "referenceType")) {
        "Canonical" -> { requireKeys(root, setOf("referenceType", "canonicalId")); HimFamilyEntityReference.Canonical(HimEntityId(requiredString(root, "canonicalId"))) }
        "Identity" -> { requireKeys(root, setOf("referenceType", "canonicalId", "identityId")); HimFamilyEntityReference.Identity(HimEntityId(requiredString(root, "canonicalId")), HimEntityId(requiredString(root, "identityId"))) }
        else -> throw IllegalArgumentException("family")
    }

    private fun decodeTrainingProvenance(root: JsonObject): HimTrainingProvenanceV1 {
        requireKeys(root, PROVENANCE_KEYS)
        val teacher = root.get("teacher").takeUnless { it.isJsonNull }?.let { t ->
            val o = t.asJsonObject; requireKeys(o, setOf("provider", "model", "configurationFingerprint"))
            HimTrainingTeacherProvenanceV1(requiredString(o, "provider"), requiredString(o, "model"), nullableString(o, "configurationFingerprint")?.let(::HimSha256))
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
            sourceArtifactDigests = requiredStringArray(root, "sourceArtifactDigests").map(::HimSha256),
            retrievalFoundationRelease = nullableString(root, "retrievalFoundationRelease"),
            retrievalFoundationReleaseSha256 = nullableString(root, "retrievalFoundationReleaseSha256")?.let(::HimSha256),
            retrievalFoundationDigest = nullableString(root, "retrievalFoundationDigest")?.let(::HimSha256),
            teacher = teacher,
        )
    }

    private fun decodeLineage(root: JsonObject): HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.P1Lineage {
        requireKeys(root, LINEAGE_KEYS)
        return HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.P1Lineage(
            negativeSupervisionRecordId = requiredString(root, "negativeSupervisionRecordId"), negativeCandidateId = requiredString(root, "negativeCandidateId"), readinessDecisionId = requiredString(root, "readinessDecisionId"), readinessState = enumValue(requiredString(root, "readinessState")), eligibilityBatchId = requiredString(root, "eligibilityBatchId"), eligibilityInputBindingDigest = requiredString(root, "eligibilityInputBindingDigest"), eligibilityDecisionId = requiredString(root, "eligibilityDecisionId"), eligibilityState = enumValue(requiredString(root, "eligibilityState")), validationBatchId = requiredString(root, "validationBatchId"), validationBatchBindingDigest = requiredString(root, "validationBatchBindingDigest"), validationBatchLogicalDigest = requiredString(root, "validationBatchLogicalDigest"), validationRecordId = requiredString(root, "validationRecordId"), reviewUnitId = requiredString(root, "reviewUnitId"), stableEntryId = requiredString(root, "stableEntryId"), canonicalEntityId = requiredString(root, "canonicalEntityId"), originalReviewerRef = requiredString(root, "originalReviewerRef"), validatorReviewerRef = requiredString(root, "validatorReviewerRef"), validationRound = requiredInt(root, "validationRound"), validationRevision = requiredInt(root, "validationRevision"), originalDecision = enumValue(requiredString(root, "originalDecision")), assessment = enumValue(requiredString(root, "assessment")), validationReasonCodes = requiredStringArray(root, "validationReasonCodes").map { enumValue<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1>(it) }, evidenceReferenceIds = requiredStringArray(root, "evidenceReferenceIds"), downstreamRoute = enumValue(requiredString(root, "downstreamRoute")), candidateState = enumValue(requiredString(root, "candidateState")), rejectedTarget = decodeTarget(requiredObject(root, "rejectedTarget")), boundaryType = enumValue(requiredString(root, "boundaryType")), occurrenceReference = requiredString(root, "occurrenceReference"), positiveTrainingExampleReference = HimTrainingExampleReference(requiredString(root, "positiveTrainingExampleReference")), materializationDecisionId = requiredString(root, "materializationDecisionId"), projectionInputBindingDigest = requiredString(root, "projectionInputBindingDigest"), proofBindingDigest = requiredString(root, "proofBindingDigest"), proofLogicalDigest = requiredString(root, "proofLogicalDigest"),
        )
    }

    private fun decodeEvidenceBinding(root: JsonObject) = run {
        requireKeys(root, setOf("persistedEvidenceReferenceId", "trainingEvidence"))
        HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding(requiredString(root, "persistedEvidenceReferenceId"), decodeTrainingEvidence(requiredObject(root, "trainingEvidence")))
    }

    private fun parseStrictJson(bytes: ByteArray): JsonObject {
        val reader = JsonReader(StringReader(bytes.toString(StandardCharsets.UTF_8))).also { it.isLenient = false }
        val root = readJson(reader)
        require(reader.peek() == JsonToken.END_DOCUMENT && root.isJsonObject)
        return root.asJsonObject
    }

    private fun readJson(reader: JsonReader): JsonElement = when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> { reader.beginObject(); val result = JsonObject(); while (reader.hasNext()) { val name = reader.nextName(); require(!result.has(name)); result.add(name, readJson(reader)) }; reader.endObject(); result }
        JsonToken.BEGIN_ARRAY -> { reader.beginArray(); val result = JsonArray(); while (reader.hasNext()) result.add(readJson(reader)); reader.endArray(); result }
        JsonToken.STRING -> JsonPrimitive(reader.nextString())
        JsonToken.NUMBER -> JsonPrimitive(BigDecimal(reader.nextString()))
        JsonToken.BOOLEAN -> JsonPrimitive(reader.nextBoolean())
        JsonToken.NULL -> { reader.nextNull(); JsonNull.INSTANCE }
        else -> throw IllegalArgumentException("json")
    }

    private fun safeRoot(file: File): Path {
        val root = file.toPath().toAbsolutePath().normalize()
        var current: Path? = root
        while (current != null) { if (Files.isSymbolicLink(current)) throw PersistenceFailure(FailureReason.UNSAFE_PATH, "root"); current = current.parent }
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw PersistenceFailure(FailureReason.UNSAFE_PATH, "root")
        return root
    }

    private fun hasSymlinkComponentWithin(path: Path, boundary: Path): Boolean {
        var current: Path? = path
        while (current != null) { if (Files.isSymbolicLink(current)) return true; if (current == boundary) return false; current = current.parent }
        return true
    }

    private fun requireSingleTrailingLf(bytes: ByteArray) { require(bytes.isNotEmpty() && bytes.last() == '\n'.code.toByte() && (bytes.size == 1 || bytes[bytes.size - 2] != '\n'.code.toByte()) && !bytes.contains('\r'.code.toByte())) }
    private fun requireKeys(root: JsonObject, expected: Set<String>) { require(root.keySet() == expected) }
    private fun requiredObject(root: JsonObject, key: String): JsonObject { val value = root.get(key); require(value != null && value.isJsonObject); return value.asJsonObject }
    private fun requiredArray(root: JsonObject, key: String): JsonArray { val value = root.get(key); require(value != null && value.isJsonArray); return value.asJsonArray }
    private fun requiredStringArray(root: JsonObject, key: String): List<String> = requiredArray(root, key).map { value -> require(value.isJsonPrimitive && value.asJsonPrimitive.isString); value.asString }
    private fun requiredString(root: JsonObject, key: String): String { val value = root.get(key); require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString); return value.asString }
    private fun nullableString(root: JsonObject, key: String): String? { val value = root.get(key); require(value != null); if (value.isJsonNull) return null; require(value.isJsonPrimitive && value.asJsonPrimitive.isString); return value.asString }
    private fun requiredInt(root: JsonObject, key: String): Int { val value = root.get(key); require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isNumber); return value.asInt }
    private inline fun <reified T : Enum<T>> enumValue(value: String): T = enumValueOf(value)
    private fun JsonObject.addNullable(key: String, value: String?) { add(key, value?.let(::JsonPrimitive) ?: JsonNull.INSTANCE) }
    private fun JsonObject.addStringArray(key: String, values: List<String>) { add(key, JsonArray().also { a -> values.forEach { a.add(it) } }) }
    private fun JsonObject.addEnumArray(key: String, values: List<String>) = addStringArray(key, values)
    private fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private val RECORD_KEYS = setOf("contractId", "version", "state", "p1MaterializationId", "genericNegativeExample", "p1Lineage", "evidenceBindings", "recordLogicalDigest")
    private val PROVENANCE_KEYS = setOf("candidateReference", "generationRunReference", "inputRunReference", "validationReference", "promotionReference", "mutationReference", "groundTruthReleaseReference", "promotedEntityId", "promotedEntityType", "sourceEvidenceReferences", "sourceArtifactDigests", "retrievalFoundationRelease", "retrievalFoundationReleaseSha256", "retrievalFoundationDigest", "teacher")
    private val LINEAGE_KEYS = setOf("negativeSupervisionRecordId", "negativeCandidateId", "readinessDecisionId", "readinessState", "eligibilityBatchId", "eligibilityInputBindingDigest", "eligibilityDecisionId", "eligibilityState", "validationBatchId", "validationBatchBindingDigest", "validationBatchLogicalDigest", "validationRecordId", "reviewUnitId", "stableEntryId", "canonicalEntityId", "originalReviewerRef", "validatorReviewerRef", "validationRound", "validationRevision", "originalDecision", "assessment", "validationReasonCodes", "evidenceReferenceIds", "downstreamRoute", "candidateState", "rejectedTarget", "boundaryType", "occurrenceReference", "positiveTrainingExampleReference", "materializationDecisionId", "projectionInputBindingDigest", "proofBindingDigest", "proofLogicalDigest")
    private val referenceComparator = compareBy<HimEvidenceReference>({ it.source }, { it.sourceArtifactSha256.value }, { it.sourceRecordIdentity })
    private val trainingEvidenceComparator = compareBy<HimTrainingEvidenceInputV1>({ it.reference.source }, { it.reference.sourceArtifactSha256.value }, { it.reference.sourceRecordIdentity }, { it.recordKind }, { it.retrievalRank })
    private val evidenceBindingComparator = compareBy<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding>({ it.persistedEvidenceReferenceId }, { it.trainingEvidence.reference.source }, { it.trainingEvidence.reference.sourceArtifactSha256.value }, { it.trainingEvidence.reference.sourceRecordIdentity })

    data class Request(val materialized: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.Materialized, val durableRoot: File = File(DURABLE_ROOT), val enabled: Boolean = true)
    data class Record(val contractId: String, val version: String, val state: String, val p1MaterializationId: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1, val genericNegativeExample: HimNegativeTrainingExampleV1, val p1Lineage: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.P1Lineage, val evidenceBindings: List<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding>, val recordLogicalDigest: String)
    enum class PersistenceStatus { CREATED, ALREADY_PRESENT_IDENTICAL }
    enum class FailureReason { INVALID_MATERIALIZED_INPUT, INVALID_MATERIALIZATION_ID, ARTIFACT_MISSING, MALFORMED_EXISTING_ARTIFACT, EXISTING_ARTIFACT_CONFLICT, UNSAFE_PATH, TEMPORARY_FILE_CONFLICT, RELOAD_VALIDATION_FAILED, WRITE_FAILED }
    class PersistenceFailure(val reason: FailureReason, val safeContext: String) : IllegalArgumentException("${reason.name} $safeContext")
    sealed interface Result {
        data class Completed(val status: PersistenceStatus, val record: Record, val relativePath: String, val byteSize: Long, val sha256: String) : Result
        data object Disabled : Result
        data class Failed(val reason: FailureReason, val safeContext: String) : Result
    }
}
