package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimPositiveTrainingExamplePersistenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import java.io.File
import java.nio.file.Files

/** Binds frozen corpus members to their complete immutable training records. */
object HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_PARTITION_SOURCE_BINDING_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "P1_TRAINING_CORPUS_PARTITION_SOURCE_BOUND_IN_MEMORY"

    private const val POSITIVE_MEMBER_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_POSITIVE_MEMBER_V1"
    private const val NEGATIVE_MEMBER_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_NEGATIVE_MEMBER_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")

    data class Request(
        val snapshot: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Record,
        val positiveDurableRoot: File = File(HimPositiveTrainingExamplePersistenceV1.DURABLE_ROOT),
        val negativeDurableRoot: File = File(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.DURABLE_ROOT,
        ),
    )

    data class SnapshotBindingV1(
        val snapshotId: String,
        val corpusLogicalDigest: HimSha256,
    ) {
        init {
            require(snapshotId == "training-corpus:v1:${corpusLogicalDigest.value}")
            require(SHA256.matches(corpusLogicalDigest.value))
        }
    }

    sealed interface SourceBindingV1 {
        val membershipReference: String
        val snapshotBinding: SnapshotBindingV1

        data class Positive(
            override val membershipReference: String,
            override val snapshotBinding: SnapshotBindingV1,
            val positiveExample: HimTrainingExampleV1,
            val durableReference: HimTrainingExampleReference,
            val durableRecordLogicalDigest: HimSha256,
        ) : SourceBindingV1

        data class Negative(
            override val membershipReference: String,
            override val snapshotBinding: SnapshotBindingV1,
            val negativeExample: HimNegativeTrainingExampleV1,
            val p1MaterializationId:
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1,
            val durableRecordLogicalDigest: HimSha256,
        ) : SourceBindingV1
    }

    data class SourceBindingSetV1(
        val snapshotBinding: SnapshotBindingV1,
        val bindings: List<SourceBindingV1>,
    ) {
        init {
            require(bindings.map { it.membershipReference }.distinct().size == bindings.size)
            require(bindings.all { it.snapshotBinding == snapshotBinding })
        }

        fun validateAgainst(
            snapshot: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Record,
        ) {
            val expected = SnapshotBindingV1(snapshot.snapshotId, snapshot.corpusLogicalDigest)
            require(snapshotBinding == expected) { "SNAPSHOT_BINDING_MISMATCH" }
            require(bindings.size == snapshot.members.size) { "MEMBERSHIP_COVERAGE_MISMATCH" }
            require(bindings.map { it.membershipReference } == snapshot.members.map { it.membershipReference }) {
                "MEMBERSHIP_COVERAGE_MISMATCH"
            }
        }
    }

    sealed interface Result {
        data class Completed(
            val snapshotBinding: SnapshotBindingV1,
            val sourceBindings: SourceBindingSetV1,
        ) : Result

        data class Failed(
            val reason: FailureReasonV1,
            val safeContext: String,
        ) : Result
    }

    enum class FailureReasonV1 {
        INVALID_CORPUS_SNAPSHOT,
        POSITIVE_RECORD_NOT_FOUND,
        NEGATIVE_RECORD_NOT_FOUND,
        POSITIVE_BINDING_MISMATCH,
        NEGATIVE_BINDING_MISMATCH,
        MODEL_INPUT_MISMATCH,
        SUPERVISION_MISMATCH,
        DURABLE_RECORD_DIGEST_MISMATCH,
    }

    fun execute(request: Request): Result {
        val snapshot = request.snapshot
        if (!validSnapshot(snapshot)) {
            return Result.Failed(FailureReasonV1.INVALID_CORPUS_SNAPSHOT, "snapshot")
        }

        val snapshotBinding = SnapshotBindingV1(snapshot.snapshotId, snapshot.corpusLogicalDigest)
        val bindings = mutableListOf<SourceBindingV1>()
        snapshot.members.forEach { member ->
            when (member.polarity) {
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.POSITIVE -> {
                    val binding = member.auditBinding as?
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Positive
                        ?: return Result.Failed(FailureReasonV1.POSITIVE_BINDING_MISMATCH, "member")
                    val supervision = member.supervision as?
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Positive
                        ?: return Result.Failed(FailureReasonV1.POSITIVE_BINDING_MISMATCH, "member")
                    val record = try {
                        loadPositiveRecord(binding.exampleReference, request.positiveDurableRoot)
                    } catch (_: Throwable) {
                        return Result.Failed(FailureReasonV1.POSITIVE_RECORD_NOT_FOUND, "member")
                    }
                    if (record.exampleReference != binding.exampleReference ||
                        record.exampleReference != record.example.exampleReference ||
                        member.membershipReference != positiveMembershipReference(record.exampleReference)
                    ) {
                        return Result.Failed(FailureReasonV1.POSITIVE_BINDING_MISMATCH, "member")
                    }
                    if (record.example.input != member.modelInput) {
                        return Result.Failed(FailureReasonV1.MODEL_INPUT_MISMATCH, "member")
                    }
                    if (record.example.target != supervision.target) {
                        return Result.Failed(FailureReasonV1.SUPERVISION_MISMATCH, "member")
                    }
                    bindings += SourceBindingV1.Positive(
                        membershipReference = member.membershipReference,
                        snapshotBinding = snapshotBinding,
                        positiveExample = record.example,
                        durableReference = record.exampleReference,
                        durableRecordLogicalDigest = HimSha256(record.logicalDigest),
                    )
                }

                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.NEGATIVE -> {
                    val binding = member.auditBinding as?
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Negative
                        ?: return Result.Failed(FailureReasonV1.NEGATIVE_BINDING_MISMATCH, "member")
                    val supervision = member.supervision as?
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Negative
                        ?: return Result.Failed(FailureReasonV1.NEGATIVE_BINDING_MISMATCH, "member")
                    val record = try {
                        HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.readByMaterializationId(
                            binding.p1MaterializationId.value,
                            request.negativeDurableRoot,
                        )
                    } catch (_: Throwable) {
                        return Result.Failed(FailureReasonV1.NEGATIVE_RECORD_NOT_FOUND, "member")
                    }
                    val negative = record.genericNegativeExample
                    if (record.p1MaterializationId != binding.p1MaterializationId ||
                        negative.reference != binding.negativeExampleReference ||
                        negative.positiveExample.exampleReference != binding.positiveExampleReference ||
                        member.membershipReference != negativeMembershipReference(record.p1MaterializationId.value)
                    ) {
                        return Result.Failed(FailureReasonV1.NEGATIVE_BINDING_MISMATCH, "member")
                    }
                    if (record.recordLogicalDigest != binding.negativeRecordLogicalDigest.value) {
                        return Result.Failed(FailureReasonV1.DURABLE_RECORD_DIGEST_MISMATCH, "member")
                    }
                    if (negative.positiveExample.input != member.modelInput) {
                        return Result.Failed(FailureReasonV1.MODEL_INPUT_MISMATCH, "member")
                    }
                    if (negative.rejectedTarget != supervision.rejectedTarget ||
                        negative.boundaryType != supervision.boundaryType
                    ) {
                        return Result.Failed(FailureReasonV1.SUPERVISION_MISMATCH, "member")
                    }
                    bindings += SourceBindingV1.Negative(
                        membershipReference = member.membershipReference,
                        snapshotBinding = snapshotBinding,
                        negativeExample = negative,
                        p1MaterializationId = record.p1MaterializationId,
                        durableRecordLogicalDigest = HimSha256(record.recordLogicalDigest),
                    )
                }
            }
        }

        val sourceBindings = SourceBindingSetV1(snapshotBinding, bindings)
        sourceBindings.validateAgainst(snapshot)
        return Result.Completed(snapshotBinding, sourceBindings)
    }

    private fun loadPositiveRecord(
        reference: HimTrainingExampleReference,
        durableRoot: File,
    ): HimPositiveTrainingExamplePersistenceV1.PositiveTrainingExampleRecordV1 {
        val example = HimPositiveTrainingExamplePersistenceV1.read(reference, durableRoot)
        val record = HimPositiveTrainingExamplePersistenceV1.deserializeRecord(
            Files.readAllBytes(HimPositiveTrainingExamplePersistenceV1.pathFor(reference, durableRoot).toPath()),
        )
        require(record.example == example)
        return record
    }

    private fun validSnapshot(
        snapshot: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Record,
    ): Boolean = try {
        require(snapshot.contractId == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.CONTRACT_ID)
        require(snapshot.version == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.VERSION)
        require(snapshot.state == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.STATE)
        require(snapshot.assemblyContractId == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CONTRACT_ID)
        require(snapshot.assemblyVersion == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.VERSION)
        require(snapshot.assemblyState == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.STATE)
        SnapshotBindingV1(snapshot.snapshotId, snapshot.corpusLogicalDigest)
        require(SHA256.matches(snapshot.recordLogicalDigest))
        require(snapshot.counters.total == snapshot.members.size)
        require(snapshot.counters.positive == snapshot.members.count {
            it.polarity == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.POSITIVE
        })
        require(snapshot.counters.negative == snapshot.members.count {
            it.polarity == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.NEGATIVE
        })
        require(snapshot.members.map { it.membershipReference }.distinct().size == snapshot.members.size)
        require(snapshot.members.zipWithNext().all { (previous, next) ->
            previous.polarity.ordinal < next.polarity.ordinal ||
                previous.polarity == next.polarity && previous.membershipReference <= next.membershipReference
        })
        require(snapshot.members.all { member ->
            when (member.polarity) {
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.POSITIVE ->
                    member.auditBinding is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Positive &&
                        member.supervision is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Positive
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.NEGATIVE ->
                    member.auditBinding is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Negative &&
                        member.supervision is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Negative
            }
        })
        true
    } catch (_: Throwable) {
        false
    }

    private fun positiveMembershipReference(reference: HimTrainingExampleReference): String =
        "corpus-member:v1:positive:${sha256(canonicalIdentity(POSITIVE_MEMBER_DOMAIN, reference.value))}"

    private fun negativeMembershipReference(materializationId: String): String =
        "corpus-member:v1:negative:${sha256(canonicalIdentity(NEGATIVE_MEMBER_DOMAIN, materializationId))}"

    private fun canonicalIdentity(domain: String, value: String) = buildString {
        append("domain=").append(domain.length).append(':').append(domain).append('\n')
        append("value=").append(value.length).append(':').append(value).append('\n')
    }

    private fun sha256(value: String): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
