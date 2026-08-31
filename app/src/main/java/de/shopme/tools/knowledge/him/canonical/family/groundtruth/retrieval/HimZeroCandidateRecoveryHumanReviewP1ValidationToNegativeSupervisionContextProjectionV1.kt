package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/**
 * Additive, training-neutral proof boundary between validated negative
 * supervision and later downstream consumers. It does not create training,
 * reopen historical stores, or perform source retrieval.
 */
object HimZeroCandidateRecoveryHumanReviewP1ValidationToNegativeSupervisionContextProjectionV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_VALIDATION_TO_NEGATIVE_SUPERVISION_CONTEXT_PROJECTION_V1"
    const val VERSION = "1"
    const val STATE = "PROVEN"
    const val DURABLE_ROOT =
        "data/knowledge/him/canonical-family/human-review/zero-candidate-recovery/v1/negative-supervision-context-proofs"
    const val DURABLE_FILE_SUFFIX = ".context-proof.v1.json"

    private const val BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_CONTEXT_PROOF_BINDING_V1"
    private const val LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_CONTEXT_PROOF_LOGICAL_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")
    private val SAFE_ID = Regex("[0-9a-f]{64}")
    private val OCCURRENCE = Regex("occurrence:v1:[0-9a-f]{64}")
    private val TRAINING_REFERENCE = Regex("example:v1:[0-9a-f]{64}")

    fun evaluate(request: Request): Result {
        val reason = validateRequest(request)
        if (reason != null) return Result.Failed(reason, "context")
        return Result.Completed(buildProofRecord(request))
    }

    fun execute(request: PersistenceRequest): PersistenceResult {
        return when (val projection = evaluate(request.projection)) {
            is Result.Failed -> PersistenceResult.Failed(projection.reason, projection.safeContext)
            is Result.Completed -> persist(projection.value, request.durableRoot)
        }
    }

    fun persist(record: ProofRecord, durableRoot: File = File(DURABLE_ROOT)): PersistenceResult {
        return try {
            validateRecord(record)
            val path = pathFor(record.negativeSupervisionRecordId, durableRoot)
            val bytes = serializeRecord(record)
            publishOrReuse(path, record, bytes)
        } catch (failure: ProofFailure) {
            PersistenceResult.Failed(failure.reason, failure.safeContext)
        } catch (_: Throwable) {
            PersistenceResult.Failed(FailureReason.PERSISTENCE_WRITE_FAILED, "proof")
        }
    }

    fun serializeRecord(record: ProofRecord): ByteArray {
        validateRecord(record)
        return (encodeRecord(record).toString() + "\n").toByteArray(StandardCharsets.UTF_8)
    }

    fun deserializeRecord(bytes: ByteArray): ProofRecord {
        return try {
            requireSingleTrailingLf(bytes)
            val root = parseStrictJson(bytes.dropLast(1).toByteArray())
            requireKeys(root, ROOT_KEYS)
            requireKeys(root.getAsJsonObject("identityProof"), IDENTITY_KEYS)
            validateTargetKeys(root.getAsJsonObject("rejectedTarget"))
            root.getAsJsonArray("evidenceBindings").forEach {
                requireKeys(it.asJsonObject, EVIDENCE_KEYS)
            }
            val record = decodeRecord(root)
            validateRecord(record)
            record
        } catch (failure: ProofFailure) {
            throw failure
        } catch (_: Throwable) {
            throw ProofFailure(FailureReason.MALFORMED_ARTIFACT, "proof")
        }
    }

    fun readByNegativeSupervisionRecordId(
        recordId: String,
        durableRoot: File = File(DURABLE_ROOT),
    ): ProofRecord {
        requireSafeId(recordId)
        val path = pathFor(recordId, durableRoot).toPath()
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw ProofFailure(FailureReason.ARTIFACT_MISSING, "proof")
        }
        return try {
            val record = deserializeRecord(Files.readAllBytes(path))
            if (record.negativeSupervisionRecordId != recordId) {
                throw ProofFailure(FailureReason.IDENTITY_MISMATCH, "proof")
            }
            record
        } catch (failure: ProofFailure) {
            throw failure
        } catch (_: Throwable) {
            throw ProofFailure(FailureReason.PERSISTENCE_READ_FAILED, "proof")
        }
    }

    fun pathFor(recordId: String, durableRoot: File = File(DURABLE_ROOT)): File {
        requireSafeId(recordId)
        val root = safeRoot(durableRoot)
        val target = root.resolve("$recordId$DURABLE_FILE_SUFFIX").normalize()
        if (!target.startsWith(root) || hasSymlinkComponentWithin(target, root)) {
            throw ProofFailure(FailureReason.UNSAFE_TARGET_PATH, "target")
        }
        return target.toFile()
    }

    fun bindingDigest(record: ProofRecord): String = sha256(
        buildString {
            appendLine(BINDING_DIGEST_DOMAIN)
            appendLine("negativeSupervisionRecordId=${record.negativeSupervisionRecordId}")
            appendLine("negativeCandidateId=${record.negativeCandidateId}")
            appendLine("validationRecordId=${record.validationRecordId}")
            appendLine("reviewUnitId=${record.reviewUnitId}")
            appendLine("stableEntryId=${record.stableEntryId}")
            appendLine("canonicalEntityId=${record.canonicalEntityId}")
            appendLine("readinessDecisionId=${record.readinessDecisionId}")
            appendLine("eligibilityDecisionId=${record.eligibilityDecisionId}")
            appendLine("validationBatchId=${record.validationBatchId}")
            appendLine("validationBatchBindingDigest=${record.validationBatchBindingDigest}")
            appendLine("validationBatchLogicalDigest=${record.validationBatchLogicalDigest}")
            appendLine("eligibilityBatchId=${record.eligibilityBatchId}")
            appendLine("eligibilityInputBindingDigest=${record.eligibilityInputBindingDigest}")
            appendLine("originalReviewerRef=${record.originalReviewerRef}")
            appendLine("validatorReviewerRef=${record.validatorReviewerRef}")
            appendLine("validationRound=${record.validationRound}")
            appendLine("validationRevision=${record.validationRevision}")
            appendLine("positiveTrainingExampleReference=${record.positiveTrainingExampleReference.value}")
            appendLine("materializationDecisionId=${record.materializationDecisionId}")
            appendLine("occurrenceReference=${record.occurrenceReference}")
            appendLine("projectionInputBindingDigest=${record.projectionInputBindingDigest}")
            appendLine("rejectedTarget=${targetKey(record.rejectedTarget)}")
            appendLine("boundaryType=${record.boundaryType.name}")
            record.evidenceBindings.forEach { binding ->
                appendLine("evidenceId=${binding.negativeEvidenceReferenceId}")
                appendLine("evidence=${evidenceKey(binding.evidence)}")
            }
        },
    )

    fun logicalDigest(record: ProofRecord): String = sha256(
        buildString {
            appendLine(LOGICAL_DIGEST_DOMAIN)
            appendLine("contractId=${record.contractId}")
            appendLine("version=${record.version}")
            appendLine("state=${record.state}")
            appendLine("bindingDigest=${record.bindingDigest}")
            appendLine("negativeSupervisionRecordId=${record.negativeSupervisionRecordId}")
            appendLine("negativeCandidateId=${record.negativeCandidateId}")
            appendLine("readinessDecisionId=${record.readinessDecisionId}")
            appendLine("eligibilityDecisionId=${record.eligibilityDecisionId}")
            appendLine("validationBatchId=${record.validationBatchId}")
            appendLine("validationBatchBindingDigest=${record.validationBatchBindingDigest}")
            appendLine("validationBatchLogicalDigest=${record.validationBatchLogicalDigest}")
            appendLine("eligibilityBatchId=${record.eligibilityBatchId}")
            appendLine("eligibilityInputBindingDigest=${record.eligibilityInputBindingDigest}")
            appendLine("validationRecordId=${record.validationRecordId}")
            appendLine("reviewUnitId=${record.reviewUnitId}")
            appendLine("stableEntryId=${record.stableEntryId}")
            appendLine("canonicalEntityId=${record.canonicalEntityId}")
            appendLine("originalReviewerRef=${record.originalReviewerRef}")
            appendLine("validatorReviewerRef=${record.validatorReviewerRef}")
            appendLine("validationRound=${record.validationRound}")
            appendLine("validationRevision=${record.validationRevision}")
            appendLine("positiveTrainingExampleReference=${record.positiveTrainingExampleReference.value}")
            appendLine("materializationDecisionId=${record.materializationDecisionId}")
            appendLine("occurrenceReference=${record.occurrenceReference}")
            appendLine("projectionInputBindingDigest=${record.projectionInputBindingDigest}")
            appendLine("rejectedTarget=${targetKey(record.rejectedTarget)}")
            appendLine("boundaryType=${record.boundaryType.name}")
            record.identityProof.appendCanonical(this)
            record.evidenceBindings.forEach { binding ->
                appendLine("evidenceId=${binding.negativeEvidenceReferenceId}")
                appendLine("evidence=${evidenceKey(binding.evidence)}")
            }
        },
    )

    fun validateRecord(record: ProofRecord) {
        if (record.contractId != CONTRACT_ID) fail(FailureReason.INVALID_CONTRACT, "proof")
        if (record.version != VERSION) fail(FailureReason.INVALID_VERSION, "proof")
        if (record.state != STATE) fail(FailureReason.INVALID_STATE, "proof")
        requireSafeId(record.negativeSupervisionRecordId)
        requireSafeId(record.negativeCandidateId)
        requireSafeId(record.readinessDecisionId)
        requireSafeId(record.eligibilityDecisionId)
        requireSafeId(record.validationRecordId)
        requireSafeId(record.validationBatchBindingDigest)
        requireSafeId(record.validationBatchLogicalDigest)
        requireSafeId(record.eligibilityInputBindingDigest)
        requireSafeId(record.projectionInputBindingDigest)
        requireSafeId(record.materializationDecisionId)
        requireSafeId(record.bindingDigest)
        requireSafeId(record.logicalDigest)
        requireNonBlank(record.validationBatchId, "validationBatchId")
        requireNonBlank(record.eligibilityBatchId, "eligibilityBatchId")
        requireNonBlank(record.reviewUnitId, "reviewUnitId")
        requireNonBlank(record.stableEntryId, "stableEntryId")
        require(HimEntityId(record.canonicalEntityId).value == record.canonicalEntityId) { "canonicalEntityId" }
        requireNonBlank(record.originalReviewerRef, "originalReviewerRef")
        requireNonBlank(record.validatorReviewerRef, "validatorReviewerRef")
        require(record.validationRound > 0 && record.validationRevision > 0) { "lineage" }
        require(record.positiveTrainingExampleReference.value.matches(TRAINING_REFERENCE)) { "trainingExampleReference" }
        requireOccurrence(record.occurrenceReference)
        validateTarget(record.rejectedTarget)
        validateIdentityProof(record.identityProof)
        if (record.identityProof.negativeSupervisionRecordId != record.negativeSupervisionRecordId ||
            record.identityProof.validationRecordId != record.validationRecordId ||
            record.identityProof.reviewUnitId != record.reviewUnitId ||
            record.identityProof.stableEntryId != record.stableEntryId ||
            record.identityProof.canonicalEntityId != record.canonicalEntityId ||
            record.identityProof.occurrenceReference != record.occurrenceReference ||
            record.identityProof.materializationDecisionId != record.materializationDecisionId ||
            record.identityProof.positiveTrainingExampleReference != record.positiveTrainingExampleReference.value
        ) fail(FailureReason.IDENTITY_MISMATCH, "identity")
        val evidenceIds = record.evidenceBindings.map { it.negativeEvidenceReferenceId }
        require(evidenceIds.isNotEmpty()) { "evidence" }
        if (evidenceIds.any(String::isBlank)) fail(FailureReason.MISSING_EVIDENCE_BINDING, "evidence")
        if (evidenceIds.distinct().size != evidenceIds.size) fail(FailureReason.DUPLICATE_EVIDENCE_BINDING, "evidence")
        record.evidenceBindings.forEach { validateEvidence(it) }
        if (record.bindingDigest != bindingDigest(record)) fail(FailureReason.BINDING_DIGEST_MISMATCH, "digest")
        if (record.logicalDigest != logicalDigest(record)) fail(FailureReason.LOGICAL_DIGEST_MISMATCH, "digest")
    }

    private fun validateRequest(request: Request): FailureReason? {
        val record = request.negativeSupervisionRecord
        if (record.candidateState != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE ||
            record.originalDecision != HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION
        ) return FailureReason.INVALID_NEGATIVE_SUPERVISION_CONTEXT
        if (record.recordId != request.referenceBinding.bindingKey ||
            record.recordId != request.referenceBinding.negativeSupervisionRecordId
        ) return FailureReason.INVALID_NEGATIVE_SUPERVISION_CONTEXT
        val validation = request.validationRecord
        if (validation.validationRecordId != record.validationRecordId ||
            validation.originalDecision.reviewUnitId != record.reviewUnitId ||
            validation.originalDecision.stableEntryId != record.stableEntryId ||
            validation.originalDecision.canonicalEntityId != record.canonicalEntityId ||
            validation.originalDecision.decision != record.originalDecision ||
            validation.validatorReviewerRef != record.validatorReviewerRef ||
            validation.validationRound != record.validationRound ||
            validation.validationRevision != record.validationRevision ||
            validation.evidenceReferenceIds.toSet() != record.evidenceReferenceIds.toSet()
        ) return FailureReason.VALIDATION_LINEAGE_MISMATCH
        if (!request.reviewUnit.validate().valid ||
            request.reviewUnit.reviewUnitId != record.reviewUnitId ||
            request.reviewUnit.stableEntryId != record.stableEntryId ||
            request.reviewUnit.canonicalEntityId != record.canonicalEntityId
        ) return FailureReason.REVIEW_LINEAGE_MISMATCH
        val materialization = request.materializationDecision
        val positive = request.positiveTrainingExample
        if (positive.input.canonicalContext.none { it.canonicalId.value == record.canonicalEntityId }) {
            return FailureReason.INPUT_IDENTITY_NOT_PROVABLE
        }
        if (materialization.state != HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationState.PROJECTED ||
            materialization.projectedTrainingExample != positive ||
            materialization.trainingExampleReference != positive.exampleReference ||
            materialization.preMaterializationIdentity.value != materialization.occurrenceReference.value
        ) return FailureReason.MATERIALIZATION_LINEAGE_MISMATCH
        val binding = request.referenceBinding
        if (binding.state != HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1.BindingState.BOUND ||
            binding.reasons.isNotEmpty() ||
            binding.bindingKey != record.recordId ||
            binding.negativeSupervisionRecordId != record.recordId ||
            binding.materializationDecisionId != materialization.decisionId ||
            binding.trainingExampleReference != positive.exampleReference
        ) return FailureReason.POSITIVE_REFERENCE_MISMATCH
        if (request.rejectedTarget == positive.target ||
            canonicalId(request.rejectedTarget)?.value == record.canonicalEntityId
        ) return FailureReason.REJECTED_TARGET_INVALID_OR_UNBOUND
        try {
            val admissibility = HimNegativeTrainingExamplePolicyV1.evaluate(
                positiveExample = positive,
                rejectedTarget = request.rejectedTarget,
                boundaryType = request.boundaryType,
            )
            if (!admissibility.admissible) return FailureReason.INVALID_BOUNDARY_TYPE
        } catch (_: Throwable) {
            return FailureReason.INVALID_BOUNDARY_TYPE
        }
        val negativeIds = record.evidenceReferenceIds
        val suppliedIds = request.evidenceBindings.map { it.negativeEvidenceReferenceId }
        if (suppliedIds.any(String::isBlank)) return FailureReason.MISSING_EVIDENCE_BINDING
        if (suppliedIds.distinct().size != suppliedIds.size) return FailureReason.DUPLICATE_EVIDENCE_BINDING
        if ((suppliedIds.toSet() - negativeIds.toSet()).isNotEmpty()) return FailureReason.EXTRA_EVIDENCE_BINDING
        if ((negativeIds.toSet() - suppliedIds.toSet()).isNotEmpty()) return FailureReason.MISSING_EVIDENCE_BINDING
        val positiveEvidence = positive.input.evidence.map { it.reference }.toSet()
        if (request.evidenceBindings.any { it.evidence !in positiveEvidence }) return FailureReason.EVIDENCE_REFERENCE_MISMATCH
        return null
    }

    private fun buildProofRecord(request: Request): ProofRecord {
        val record = request.negativeSupervisionRecord
        val identity = IdentityProof(
            negativeSupervisionRecordId = record.recordId,
            validationRecordId = record.validationRecordId,
            reviewUnitId = record.reviewUnitId,
            stableEntryId = record.stableEntryId,
            canonicalEntityId = record.canonicalEntityId,
            occurrenceReference = request.materializationDecision.occurrenceReference.value,
            materializationDecisionId = request.materializationDecision.decisionId,
            positiveTrainingExampleReference = request.positiveTrainingExample.exampleReference.value,
        )
        val unsigned = ProofRecord(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            negativeSupervisionRecordId = record.recordId,
            negativeCandidateId = record.negativeCandidateId,
            readinessDecisionId = record.readinessDecisionId,
            eligibilityDecisionId = record.eligibilityDecisionId,
            validationBatchId = record.validationBatchId,
            validationBatchBindingDigest = record.validationBatchBindingDigest,
            validationBatchLogicalDigest = record.validationBatchLogicalDigest,
            eligibilityBatchId = record.eligibilityBatchId,
            eligibilityInputBindingDigest = record.eligibilityInputBindingDigest,
            validationRecordId = record.validationRecordId,
            reviewUnitId = record.reviewUnitId,
            stableEntryId = record.stableEntryId,
            canonicalEntityId = record.canonicalEntityId,
            originalReviewerRef = record.originalReviewerRef,
            validatorReviewerRef = record.validatorReviewerRef,
            validationRound = record.validationRound,
            validationRevision = record.validationRevision,
            positiveTrainingExampleReference = request.positiveTrainingExample.exampleReference,
            materializationDecisionId = request.materializationDecision.decisionId,
            occurrenceReference = request.materializationDecision.occurrenceReference.value,
            projectionInputBindingDigest = request.materializationDecision.projectionInputBindingDigest.value,
            rejectedTarget = request.rejectedTarget,
            boundaryType = request.boundaryType,
            identityProof = identity,
            evidenceBindings = request.evidenceBindings.sortedWith(compareBy({ it.negativeEvidenceReferenceId }, { evidenceKey(it.evidence) })),
            bindingDigest = "0".repeat(64),
            logicalDigest = "0".repeat(64),
        )
        val bound = unsigned.copy(bindingDigest = bindingDigest(unsigned))
        return bound.copy(logicalDigest = logicalDigest(bound))
    }

    private fun validateIdentityProof(identity: IdentityProof) {
        requireSafeId(identity.negativeSupervisionRecordId)
        requireSafeId(identity.validationRecordId)
        requireNonBlank(identity.reviewUnitId, "reviewUnitId")
        requireNonBlank(identity.stableEntryId, "stableEntryId")
        requireNonBlank(identity.canonicalEntityId, "canonicalEntityId")
        requireSafeId(identity.materializationDecisionId)
        requireOccurrence(identity.occurrenceReference)
        require(identity.positiveTrainingExampleReference.matches(TRAINING_REFERENCE)) { "positiveReference" }
    }

    private fun validateTarget(target: HimTrainingTargetV1) {
        when (target) {
            is HimTrainingTargetV1.ExistingCanonical -> Unit
            is HimTrainingTargetV1.Identity -> Unit
            is HimTrainingTargetV1.Variant -> validateFamilyReference(target.scope)
            is HimTrainingTargetV1.Alias -> validateFamilyReference(target.equivalentEntity)
            is HimTrainingTargetV1.NewCanonical -> require(!target.proposedCanonicalName.isNullOrBlank()) { "target" }
        }
    }

    private fun validateFamilyReference(reference: HimFamilyEntityReference) {
        require(reference.canonicalId.value.matches(Regex("[0-9A-Za-z]{6}"))) { "family" }
        if (reference is HimFamilyEntityReference.Identity) {
            require(reference.identityId.value.matches(Regex("[0-9A-Za-z]{6}"))) { "family" }
        }
    }

    private fun validateEvidence(binding: EvidenceBinding) {
        require(binding.negativeEvidenceReferenceId.isNotBlank()) { "evidenceId" }
        require(binding.evidence.source.isNotBlank()) { "source" }
        require(SHA256.matches(binding.evidence.sourceArtifactSha256.value)) { "artifact" }
        require(binding.evidence.sourceRecordIdentity.isNotBlank()) { "record" }
    }

    private fun targetKey(target: HimTrainingTargetV1): String = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> "EXISTING_CANONICAL|${target.canonicalId.value}"
        is HimTrainingTargetV1.Identity -> "IDENTITY|${target.parentCanonicalId.value}"
        is HimTrainingTargetV1.Variant -> "VARIANT|${familyKey(target.scope)}"
        is HimTrainingTargetV1.Alias -> "ALIAS|${familyKey(target.equivalentEntity)}"
        is HimTrainingTargetV1.NewCanonical -> "NEW_CANONICAL|${target.proposedCanonicalName.orEmpty()}"
    }

    private fun canonicalId(target: HimTrainingTargetV1): HimEntityId? = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> target.canonicalId
        is HimTrainingTargetV1.Identity -> target.parentCanonicalId
        is HimTrainingTargetV1.Variant -> target.scope.canonicalId
        is HimTrainingTargetV1.Alias -> target.equivalentEntity.canonicalId
        is HimTrainingTargetV1.NewCanonical -> null
    }

    private fun familyKey(reference: HimFamilyEntityReference): String = when (reference) {
        is HimFamilyEntityReference.Canonical -> "CANONICAL|${reference.canonicalId.value}"
        is HimFamilyEntityReference.Identity -> "IDENTITY|${reference.canonicalId.value}|${reference.identityId.value}"
    }

    private fun evidenceKey(evidence: HimEvidenceReference): String =
        "${evidence.source}|${evidence.sourceArtifactSha256.value}|${evidence.sourceRecordIdentity}"

    private fun ProofRecord.appendTarget(builder: StringBuilder) {
        builder.appendLine("target=${targetKey(rejectedTarget)}")
    }

    private fun IdentityProof.appendCanonical(builder: StringBuilder) {
        builder.appendLine("identity.negativeSupervisionRecordId=$negativeSupervisionRecordId")
        builder.appendLine("identity.validationRecordId=$validationRecordId")
        builder.appendLine("identity.reviewUnitId=$reviewUnitId")
        builder.appendLine("identity.stableEntryId=$stableEntryId")
        builder.appendLine("identity.canonicalEntityId=$canonicalEntityId")
        builder.appendLine("identity.occurrenceReference=$occurrenceReference")
        builder.appendLine("identity.materializationDecisionId=$materializationDecisionId")
        builder.appendLine("identity.positiveTrainingExampleReference=$positiveTrainingExampleReference")
    }

    private fun publishOrReuse(path: File, record: ProofRecord, bytes: ByteArray): PersistenceResult {
        val root = requireNotNull(path.parentFile) { "root" }
        val rootPath = root.toPath()
        val target = path.toPath()
        val temporary = root.resolve(".${path.name}.tmp").toPath()
        if (Files.isSymbolicLink(rootPath)) fail(FailureReason.UNSAFE_DURABLE_ROOT, "root")
        if (Files.isSymbolicLink(target)) fail(FailureReason.UNSAFE_TARGET_PATH, "target")
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) fail(FailureReason.PARTIAL_ARTIFACT, "temporary")
            val existing = Files.readAllBytes(target)
            val existingRecord = deserializeRecord(existing)
            if (!existing.contentEquals(bytes) || existingRecord != record) {
                fail(FailureReason.EXISTING_ARTIFACT_CONFLICT, "proof")
            }
            return PersistenceResult.Completed(PersistenceStatus.ALREADY_PRESENT_IDENTICAL, record, path, existing)
        }
        if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) fail(FailureReason.PARTIAL_ARTIFACT, "temporary")
        try {
            Files.createDirectories(rootPath)
            Files.write(temporary, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            FileOutputStream(temporary.toFile(), true).use { it.fd.sync() }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target)
            }
        } catch (_: FileAlreadyExistsException) {
            fail(FailureReason.EXISTING_ARTIFACT_CONFLICT, "proof")
        } catch (_: Throwable) {
            fail(FailureReason.ATOMIC_PUBLICATION_FAILED, "write")
        } finally {
            Files.deleteIfExists(temporary)
        }
        val persisted = Files.readAllBytes(target)
        if (!persisted.contentEquals(bytes)) fail(FailureReason.BYTE_IDENTITY_MISMATCH, "reload")
        if (deserializeRecord(persisted) != record) fail(FailureReason.RELOAD_MISMATCH, "reload")
        return PersistenceResult.Completed(PersistenceStatus.CREATED, record, path, persisted)
    }

    private fun encodeRecord(record: ProofRecord) = JsonObject().apply {
        addProperty("contractId", record.contractId)
        addProperty("version", record.version)
        addProperty("state", record.state)
        addProperty("negativeSupervisionRecordId", record.negativeSupervisionRecordId)
        addProperty("negativeCandidateId", record.negativeCandidateId)
        addProperty("readinessDecisionId", record.readinessDecisionId)
        addProperty("eligibilityDecisionId", record.eligibilityDecisionId)
        addProperty("validationBatchId", record.validationBatchId)
        addProperty("validationBatchBindingDigest", record.validationBatchBindingDigest)
        addProperty("validationBatchLogicalDigest", record.validationBatchLogicalDigest)
        addProperty("eligibilityBatchId", record.eligibilityBatchId)
        addProperty("eligibilityInputBindingDigest", record.eligibilityInputBindingDigest)
        addProperty("validationRecordId", record.validationRecordId)
        addProperty("reviewUnitId", record.reviewUnitId)
        addProperty("stableEntryId", record.stableEntryId)
        addProperty("canonicalEntityId", record.canonicalEntityId)
        addProperty("originalReviewerRef", record.originalReviewerRef)
        addProperty("validatorReviewerRef", record.validatorReviewerRef)
        addProperty("validationRound", record.validationRound)
        addProperty("validationRevision", record.validationRevision)
        addProperty("positiveTrainingExampleReference", record.positiveTrainingExampleReference.value)
        addProperty("materializationDecisionId", record.materializationDecisionId)
        addProperty("occurrenceReference", record.occurrenceReference)
        addProperty("projectionInputBindingDigest", record.projectionInputBindingDigest)
        add("rejectedTarget", encodeTarget(record.rejectedTarget))
        addProperty("boundaryType", record.boundaryType.name)
        add("identityProof", encodeIdentity(record.identityProof))
        add("evidenceBindings", JsonArray().apply {
            record.evidenceBindings.forEach { binding -> add(encodeEvidence(binding)) }
        })
        addProperty("bindingDigest", record.bindingDigest)
        addProperty("logicalDigest", record.logicalDigest)
    }

    private fun encodeIdentity(identity: IdentityProof) = JsonObject().apply {
        addProperty("negativeSupervisionRecordId", identity.negativeSupervisionRecordId)
        addProperty("validationRecordId", identity.validationRecordId)
        addProperty("reviewUnitId", identity.reviewUnitId)
        addProperty("stableEntryId", identity.stableEntryId)
        addProperty("canonicalEntityId", identity.canonicalEntityId)
        addProperty("occurrenceReference", identity.occurrenceReference)
        addProperty("materializationDecisionId", identity.materializationDecisionId)
        addProperty("positiveTrainingExampleReference", identity.positiveTrainingExampleReference)
    }

    private fun encodeEvidence(binding: EvidenceBinding) = JsonObject().apply {
        addProperty("negativeEvidenceReferenceId", binding.negativeEvidenceReferenceId)
        addProperty("source", binding.evidence.source)
        addProperty("sourceArtifactSha256", binding.evidence.sourceArtifactSha256.value)
        addProperty("sourceRecordIdentity", binding.evidence.sourceRecordIdentity)
    }

    private fun encodeTarget(target: HimTrainingTargetV1) = JsonObject().apply {
        when (target) {
            is HimTrainingTargetV1.ExistingCanonical -> {
                addProperty("kind", "EXISTING_CANONICAL")
                addProperty("canonicalId", target.canonicalId.value)
            }
            is HimTrainingTargetV1.Identity -> {
                addProperty("kind", "IDENTITY")
                addProperty("canonicalId", target.parentCanonicalId.value)
            }
            is HimTrainingTargetV1.Variant -> {
                addProperty("kind", "VARIANT")
                add("scope", encodeFamily(target.scope))
            }
            is HimTrainingTargetV1.Alias -> {
                addProperty("kind", "ALIAS")
                add("equivalentEntity", encodeFamily(target.equivalentEntity))
            }
            is HimTrainingTargetV1.NewCanonical -> {
                addProperty("kind", "NEW_CANONICAL")
                addProperty("proposedCanonicalName", target.proposedCanonicalName)
            }
        }
    }

    private fun encodeFamily(reference: HimFamilyEntityReference) = JsonObject().apply {
        when (reference) {
            is HimFamilyEntityReference.Canonical -> {
                addProperty("kind", "CANONICAL")
                addProperty("canonicalId", reference.canonicalId.value)
            }
            is HimFamilyEntityReference.Identity -> {
                addProperty("kind", "IDENTITY")
                addProperty("canonicalId", reference.canonicalId.value)
                addProperty("identityId", reference.identityId.value)
            }
        }
    }

    private fun decodeRecord(root: JsonObject): ProofRecord {
        val identity = root.getAsJsonObject("identityProof")
        val evidence = root.getAsJsonArray("evidenceBindings").map { value ->
            val item = value.asJsonObject
            EvidenceBinding(
                requiredString(item, "negativeEvidenceReferenceId"),
                HimEvidenceReference(
                    requiredString(item, "source"),
                    HimSha256(requiredString(item, "sourceArtifactSha256")),
                    requiredString(item, "sourceRecordIdentity"),
                ),
            )
        }
        return ProofRecord(
            contractId = requiredString(root, "contractId"),
            version = requiredString(root, "version"),
            state = requiredString(root, "state"),
            negativeSupervisionRecordId = requiredString(root, "negativeSupervisionRecordId"),
            negativeCandidateId = requiredString(root, "negativeCandidateId"),
            readinessDecisionId = requiredString(root, "readinessDecisionId"),
            eligibilityDecisionId = requiredString(root, "eligibilityDecisionId"),
            validationBatchId = requiredString(root, "validationBatchId"),
            validationBatchBindingDigest = requiredString(root, "validationBatchBindingDigest"),
            validationBatchLogicalDigest = requiredString(root, "validationBatchLogicalDigest"),
            eligibilityBatchId = requiredString(root, "eligibilityBatchId"),
            eligibilityInputBindingDigest = requiredString(root, "eligibilityInputBindingDigest"),
            validationRecordId = requiredString(root, "validationRecordId"),
            reviewUnitId = requiredString(root, "reviewUnitId"),
            stableEntryId = requiredString(root, "stableEntryId"),
            canonicalEntityId = requiredString(root, "canonicalEntityId"),
            originalReviewerRef = requiredString(root, "originalReviewerRef"),
            validatorReviewerRef = requiredString(root, "validatorReviewerRef"),
            validationRound = requiredInt(root, "validationRound"),
            validationRevision = requiredInt(root, "validationRevision"),
            positiveTrainingExampleReference = HimTrainingExampleReference(requiredString(root, "positiveTrainingExampleReference")),
            materializationDecisionId = requiredString(root, "materializationDecisionId"),
            occurrenceReference = requiredString(root, "occurrenceReference"),
            projectionInputBindingDigest = requiredString(root, "projectionInputBindingDigest"),
            rejectedTarget = decodeTarget(root.getAsJsonObject("rejectedTarget")),
            boundaryType = enumValue<HimNegativeBoundaryTypeV1>(root, "boundaryType"),
            identityProof = IdentityProof(
                requiredString(identity, "negativeSupervisionRecordId"),
                requiredString(identity, "validationRecordId"),
                requiredString(identity, "reviewUnitId"),
                requiredString(identity, "stableEntryId"),
                requiredString(identity, "canonicalEntityId"),
                requiredString(identity, "occurrenceReference"),
                requiredString(identity, "materializationDecisionId"),
                requiredString(identity, "positiveTrainingExampleReference"),
            ),
            evidenceBindings = evidence,
            bindingDigest = requiredString(root, "bindingDigest"),
            logicalDigest = requiredString(root, "logicalDigest"),
        )
    }

    private fun decodeTarget(root: JsonObject): HimTrainingTargetV1 = when (requiredString(root, "kind")) {
        "EXISTING_CANONICAL" -> HimTrainingTargetV1.ExistingCanonical(HimEntityId(requiredString(root, "canonicalId")))
        "IDENTITY" -> HimTrainingTargetV1.Identity(HimEntityId(requiredString(root, "canonicalId")))
        "VARIANT" -> HimTrainingTargetV1.Variant(decodeFamily(root.getAsJsonObject("scope")))
        "ALIAS" -> HimTrainingTargetV1.Alias(decodeFamily(root.getAsJsonObject("equivalentEntity")))
        "NEW_CANONICAL" -> HimTrainingTargetV1.NewCanonical(requiredString(root, "proposedCanonicalName"))
        else -> throw ProofFailure(FailureReason.MALFORMED_ARTIFACT, "target")
    }

    private fun decodeFamily(root: JsonObject): HimFamilyEntityReference = when (requiredString(root, "kind")) {
        "CANONICAL" -> HimFamilyEntityReference.Canonical(HimEntityId(requiredString(root, "canonicalId")))
        "IDENTITY" -> HimFamilyEntityReference.Identity(
            HimEntityId(requiredString(root, "canonicalId")),
            HimEntityId(requiredString(root, "identityId")),
        )
        else -> throw ProofFailure(FailureReason.MALFORMED_ARTIFACT, "family")
    }

    private fun parseStrictJson(bytes: ByteArray): JsonObject {
        val reader = JsonReader(StringReader(bytes.toString(StandardCharsets.UTF_8)))
        reader.isLenient = false
        val root = readJson(reader)
        if (reader.peek() != JsonToken.END_DOCUMENT || !root.isJsonObject) throw ProofFailure(FailureReason.MALFORMED_ARTIFACT, "json")
        return root.asJsonObject
    }

    private fun readJson(reader: JsonReader): JsonElement = when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> {
            reader.beginObject()
            val result = JsonObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (result.has(name)) throw ProofFailure(FailureReason.DUPLICATE_JSON_FIELD, "json")
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
        JsonToken.STRING -> com.google.gson.JsonPrimitive(reader.nextString())
        JsonToken.NUMBER -> com.google.gson.JsonPrimitive(BigDecimal(reader.nextString()))
        JsonToken.BOOLEAN -> com.google.gson.JsonPrimitive(reader.nextBoolean())
        JsonToken.NULL -> { reader.nextNull(); com.google.gson.JsonNull.INSTANCE }
        else -> throw ProofFailure(FailureReason.MALFORMED_ARTIFACT, "json")
    }

    private fun requireKeys(root: JsonObject, expected: Set<String>) {
        if (root.keySet() != expected) throw ProofFailure(FailureReason.UNKNOWN_OR_MISSING_FIELD, "json")
    }

    private fun validateTargetKeys(root: JsonObject) {
        val kind = requiredString(root, "kind")
        val expected = when (kind) {
            "EXISTING_CANONICAL", "IDENTITY" -> setOf("kind", "canonicalId")
            "VARIANT" -> setOf("kind", "scope")
            "ALIAS" -> setOf("kind", "equivalentEntity")
            "NEW_CANONICAL" -> setOf("kind", "proposedCanonicalName")
            else -> throw ProofFailure(FailureReason.MALFORMED_ARTIFACT, "target")
        }
        requireKeys(root, expected)
        if (kind == "VARIANT") validateFamilyKeys(root.getAsJsonObject("scope"))
        if (kind == "ALIAS") validateFamilyKeys(root.getAsJsonObject("equivalentEntity"))
    }

    private fun validateFamilyKeys(root: JsonObject) {
        when (requiredString(root, "kind")) {
            "CANONICAL" -> requireKeys(root, setOf("kind", "canonicalId"))
            "IDENTITY" -> requireKeys(root, setOf("kind", "canonicalId", "identityId"))
            else -> throw ProofFailure(FailureReason.MALFORMED_ARTIFACT, "family")
        }
    }

    private fun requiredString(root: JsonObject, key: String): String {
        val value = root.get(key)
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) {
            throw ProofFailure(FailureReason.MALFORMED_ARTIFACT, key)
        }
        return value.asString
    }

    private fun requiredInt(root: JsonObject, key: String): Int {
        val value = root.get(key)
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) {
            throw ProofFailure(FailureReason.MALFORMED_ARTIFACT, key)
        }
        return value.asInt
    }

    private inline fun <reified T : Enum<T>> enumValue(root: JsonObject, key: String): T = try {
        enumValueOf(requiredString(root, key))
    } catch (_: Throwable) {
        throw ProofFailure(FailureReason.MALFORMED_ARTIFACT, key)
    }

    private fun requireSingleTrailingLf(bytes: ByteArray) {
        if (bytes.isEmpty() || bytes.last() != '\n'.code.toByte() || bytes.dropLast(1).contains('\n'.code.toByte())) {
            throw ProofFailure(FailureReason.MALFORMED_ARTIFACT, "wire")
        }
    }

    private fun safeRoot(root: File): Path {
        val path = root.toPath().toAbsolutePath().normalize()
        if (Files.isSymbolicLink(path) || (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))) {
            fail(FailureReason.UNSAFE_DURABLE_ROOT, "root")
        }
        return path
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

    private fun requireSafeId(value: String) {
        if (!SAFE_ID.matches(value)) fail(FailureReason.INVALID_IDENTITY, "id")
    }

    private fun requireOccurrence(value: String) {
        if (!OCCURRENCE.matches(value)) fail(FailureReason.INVALID_IDENTITY, "occurrence")
    }

    private fun requireNonBlank(value: String, context: String) {
        if (value.isBlank() || value.contains('\n') || value.contains('\r')) fail(FailureReason.INVALID_PROOF, context)
    }

    private fun fail(reason: FailureReason, safeContext: String): Nothing = throw ProofFailure(reason, safeContext)

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private val ROOT_KEYS = setOf(
        "contractId", "version", "state", "negativeSupervisionRecordId", "negativeCandidateId",
        "readinessDecisionId", "eligibilityDecisionId", "validationBatchId", "validationBatchBindingDigest",
        "validationBatchLogicalDigest", "eligibilityBatchId", "eligibilityInputBindingDigest", "validationRecordId",
        "reviewUnitId", "stableEntryId", "canonicalEntityId", "originalReviewerRef", "validatorReviewerRef",
        "validationRound", "validationRevision", "positiveTrainingExampleReference", "materializationDecisionId",
        "occurrenceReference", "projectionInputBindingDigest", "rejectedTarget", "boundaryType", "identityProof",
        "evidenceBindings", "bindingDigest", "logicalDigest",
    )
    private val IDENTITY_KEYS = setOf(
        "negativeSupervisionRecordId", "validationRecordId", "reviewUnitId", "stableEntryId", "canonicalEntityId",
        "occurrenceReference", "materializationDecisionId", "positiveTrainingExampleReference",
    )
    private val EVIDENCE_KEYS = setOf("negativeEvidenceReferenceId", "source", "sourceArtifactSha256", "sourceRecordIdentity")

    data class Request(
        val negativeSupervisionRecord: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        val validationRecord: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1,
        val reviewUnit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1,
        val positiveTrainingExample: HimTrainingExampleV1,
        val materializationDecision: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationDecision,
        val referenceBinding: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingPersistenceV1.BindingRecord,
        val rejectedTarget: HimTrainingTargetV1,
        val boundaryType: HimNegativeBoundaryTypeV1,
        val evidenceBindings: List<EvidenceBinding>,
    )

    data class PersistenceRequest(val projection: Request, val durableRoot: File = File(DURABLE_ROOT))

    data class EvidenceBinding(val negativeEvidenceReferenceId: String, val evidence: HimEvidenceReference)

    data class IdentityProof(
        val negativeSupervisionRecordId: String,
        val validationRecordId: String,
        val reviewUnitId: String,
        val stableEntryId: String,
        val canonicalEntityId: String,
        val occurrenceReference: String,
        val materializationDecisionId: String,
        val positiveTrainingExampleReference: String,
    )

    data class ProofRecord(
        val contractId: String,
        val version: String,
        val state: String,
        val negativeSupervisionRecordId: String,
        val negativeCandidateId: String,
        val readinessDecisionId: String,
        val eligibilityDecisionId: String,
        val validationBatchId: String,
        val validationBatchBindingDigest: String,
        val validationBatchLogicalDigest: String,
        val eligibilityBatchId: String,
        val eligibilityInputBindingDigest: String,
        val validationRecordId: String,
        val reviewUnitId: String,
        val stableEntryId: String,
        val canonicalEntityId: String,
        val originalReviewerRef: String,
        val validatorReviewerRef: String,
        val validationRound: Int,
        val validationRevision: Int,
        val positiveTrainingExampleReference: HimTrainingExampleReference,
        val materializationDecisionId: String,
        val occurrenceReference: String,
        val projectionInputBindingDigest: String,
        val rejectedTarget: HimTrainingTargetV1,
        val boundaryType: HimNegativeBoundaryTypeV1,
        val identityProof: IdentityProof,
        val evidenceBindings: List<EvidenceBinding>,
        val bindingDigest: String,
        val logicalDigest: String,
    )

    enum class PersistenceStatus { CREATED, ALREADY_PRESENT_IDENTICAL }

    enum class FailureReason {
        INVALID_NEGATIVE_SUPERVISION_CONTEXT,
        VALIDATION_LINEAGE_MISMATCH,
        REVIEW_LINEAGE_MISMATCH,
        POSITIVE_REFERENCE_MISMATCH,
        MATERIALIZATION_LINEAGE_MISMATCH,
        REJECTED_TARGET_INVALID_OR_UNBOUND,
        INVALID_BOUNDARY_TYPE,
        INPUT_IDENTITY_NOT_PROVABLE,
        MISSING_EVIDENCE_BINDING,
        DUPLICATE_EVIDENCE_BINDING,
        EXTRA_EVIDENCE_BINDING,
        EVIDENCE_REFERENCE_MISMATCH,
        INVALID_PROOF,
        INVALID_IDENTITY,
        IDENTITY_MISMATCH,
        INVALID_CONTRACT,
        INVALID_VERSION,
        INVALID_STATE,
        BINDING_DIGEST_MISMATCH,
        LOGICAL_DIGEST_MISMATCH,
        MALFORMED_ARTIFACT,
        DUPLICATE_JSON_FIELD,
        UNKNOWN_OR_MISSING_FIELD,
        ARTIFACT_MISSING,
        UNSAFE_DURABLE_ROOT,
        UNSAFE_TARGET_PATH,
        PARTIAL_ARTIFACT,
        EXISTING_ARTIFACT_CONFLICT,
        ATOMIC_PUBLICATION_FAILED,
        BYTE_IDENTITY_MISMATCH,
        RELOAD_MISMATCH,
        PERSISTENCE_READ_FAILED,
        PERSISTENCE_WRITE_FAILED,
    }

    sealed interface Result {
        data class Completed(val value: ProofRecord) : Result
        data class Failed(val reason: FailureReason, val safeContext: String) : Result
    }

    sealed interface PersistenceResult {
        data class Completed(
            val status: PersistenceStatus,
            val record: ProofRecord,
            val path: File,
            val bytes: ByteArray,
        ) : PersistenceResult
        data class Failed(val reason: FailureReason, val safeContext: String) : PersistenceResult
    }

    private class ProofFailure(val reason: FailureReason, val safeContext: String) : IllegalArgumentException()
}
