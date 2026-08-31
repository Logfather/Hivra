package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleReferenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleValidatorV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Pure, explicit-membership assembly of the P1 training corpus boundary. */
object HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_ASSEMBLY_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "P1_TRAINING_CORPUS_ASSEMBLED_IN_MEMORY"

    private const val POSITIVE_MEMBER_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_POSITIVE_MEMBER_V1"
    private const val NEGATIVE_MEMBER_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_NEGATIVE_MEMBER_V1"
    private const val CORPUS_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_TRAINING_CORPUS_LOGICAL_DIGEST_V1"
    private val ZERO_DIGEST = "0".repeat(64)
    private val MEMBER_REFERENCE = Regex("corpus-member:v1:(positive|negative):[0-9a-f]{64}")

    enum class PolarityV1 {
        POSITIVE,
        NEGATIVE,
    }

    data class PositiveMember(
        val example: HimTrainingExampleV1,
        val sourceBinding: PositiveSourceBinding = PositiveSourceBinding(example.exampleReference),
        val modelInput: HimTrainingInputV1 = example.input,
    )

    data class PositiveSourceBinding(
        val exampleReference: HimTrainingExampleReference,
    )

    data class NegativeMember(
        val record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Record,
    )

    data class Request(
        val positiveMembers: List<PositiveMember> = emptyList(),
        val negativeMembers: List<NegativeMember> = emptyList(),
    )

    sealed interface SupervisionV1 {
        data class Positive(
            val target: HimTrainingTargetV1,
        ) : SupervisionV1

        data class Negative(
            val rejectedTarget: HimTrainingTargetV1,
            val boundaryType: HimNegativeBoundaryTypeV1,
        ) : SupervisionV1
    }

    sealed interface AuditBindingV1 {
        data class Positive(
            val exampleReference: HimTrainingExampleReference,
        ) : AuditBindingV1

        data class Negative(
            val p1MaterializationId:
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1,
            val negativeExampleReference: HimNegativeTrainingExampleReferenceV1,
            val positiveExampleReference: HimTrainingExampleReference,
            val negativeRecordLogicalDigest: HimSha256,
        ) : AuditBindingV1
    }

    data class MemberV1(
        val polarity: PolarityV1,
        val membershipReference: String,
        val modelInput: HimTrainingInputV1,
        val supervision: SupervisionV1,
        val auditBinding: AuditBindingV1,
    ) {
        init {
            require(MEMBER_REFERENCE.matches(membershipReference))
        }
    }

    data class CountersV1(
        val total: Int,
        val positive: Int,
        val negative: Int,
    ) {
        init {
            require(total >= 0 && positive >= 0 && negative >= 0)
            require(total == positive + negative)
        }

        companion object {
            fun from(members: List<MemberV1>) = CountersV1(
                total = members.size,
                positive = members.count { it.polarity == PolarityV1.POSITIVE },
                negative = members.count { it.polarity == PolarityV1.NEGATIVE },
            )
        }
    }

    data class CorpusV1(
        val members: List<MemberV1>,
        val counters: CountersV1,
        val logicalDigest: HimSha256,
    )

    sealed interface Result {
        data class Completed(
            val corpus: CorpusV1,
        ) : Result

        data class Failed(
            val reason: FailureReasonV1,
        ) : Result
    }

    enum class FailureReasonV1 {
        INVALID_POSITIVE_MEMBER,
        INVALID_NEGATIVE_MEMBER,
        DUPLICATE_MEMBERSHIP,
        POSITIVE_BINDING_MISMATCH,
        CONTRADICTORY_SUPERVISION,
    }

    fun assemble(request: Request): Result {
        val positiveFailure = request.positiveMembers.firstOrNull { member ->
            !validPositive(member)
        }
        if (positiveFailure != null) return Result.Failed(FailureReasonV1.INVALID_POSITIVE_MEMBER)

        val positiveBindingMismatch = request.positiveMembers.any { member ->
            member.sourceBinding.exampleReference != member.example.exampleReference
        }
        if (positiveBindingMismatch) return Result.Failed(FailureReasonV1.POSITIVE_BINDING_MISMATCH)

        val negativeFailure = request.negativeMembers.firstOrNull { member ->
            !validNegative(member.record)
        }
        if (negativeFailure != null) return Result.Failed(FailureReasonV1.INVALID_NEGATIVE_MEMBER)

        val positiveReferences = request.positiveMembers.map { it.example.exampleReference.value }
        val negativeReferences = request.negativeMembers.map { it.record.p1MaterializationId.value }
        if (positiveReferences.distinct().size != positiveReferences.size ||
            negativeReferences.distinct().size != negativeReferences.size
        ) {
            return Result.Failed(FailureReasonV1.DUPLICATE_MEMBERSHIP)
        }

        val members = buildList {
            request.positiveMembers.forEach { member ->
                add(
                    MemberV1(
                        polarity = PolarityV1.POSITIVE,
                        membershipReference = positiveMembershipReference(member.example.exampleReference),
                        modelInput = member.example.modelInput(),
                        supervision = SupervisionV1.Positive(member.example.target),
                        auditBinding = AuditBindingV1.Positive(member.example.exampleReference),
                    ),
                )
            }
            request.negativeMembers.forEach { member ->
                val record = member.record
                val negative = record.genericNegativeExample
                add(
                    MemberV1(
                        polarity = PolarityV1.NEGATIVE,
                        membershipReference = negativeMembershipReference(record.p1MaterializationId.value),
                        modelInput = negative.modelInput(),
                        supervision = SupervisionV1.Negative(negative.rejectedTarget, negative.boundaryType),
                        auditBinding = AuditBindingV1.Negative(
                            p1MaterializationId = record.p1MaterializationId,
                            negativeExampleReference = negative.reference,
                            positiveExampleReference = negative.positiveExample.exampleReference,
                            negativeRecordLogicalDigest = HimSha256(record.recordLogicalDigest),
                        ),
                    ),
                )
            }
        }.sortedWith(memberComparator)

        if (hasPositiveBindingMismatch(members)) {
            return Result.Failed(FailureReasonV1.POSITIVE_BINDING_MISMATCH)
        }
        if (hasContradictorySupervision(members)) {
            return Result.Failed(FailureReasonV1.CONTRADICTORY_SUPERVISION)
        }

        val counters = CountersV1.from(members)
        val unsigned = CorpusV1(members, counters, HimSha256(ZERO_DIGEST))
        return Result.Completed(unsigned.copy(logicalDigest = corpusLogicalDigest(unsigned)))
    }

    private fun validPositive(member: PositiveMember): Boolean = try {
        HimTrainingExampleValidatorV1.validate(member.example)
        member.modelInput == member.example.input
    } catch (_: Throwable) {
        false
    }

    private fun validNegative(
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Record,
    ): Boolean = try {
        HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.validateRecord(record)
        true
    } catch (_: Throwable) {
        false
    }

    private fun positiveMembershipReference(reference: HimTrainingExampleReference): String =
        "corpus-member:v1:positive:${sha256(canonicalIdentity(POSITIVE_MEMBER_DOMAIN, reference.value))}"

    private fun negativeMembershipReference(materializationId: String): String =
        "corpus-member:v1:negative:${sha256(canonicalIdentity(NEGATIVE_MEMBER_DOMAIN, materializationId))}"

    private fun canonicalIdentity(domain: String, value: String) = buildString {
        field("domain", domain)
        field("value", value)
    }

    private val memberComparator = compareBy<MemberV1>({ it.polarity.ordinal }, { it.membershipReference })

    private fun hasPositiveBindingMismatch(members: List<MemberV1>): Boolean {
        val positives = members
            .filter { it.polarity == PolarityV1.POSITIVE }
            .associateBy { (it.auditBinding as AuditBindingV1.Positive).exampleReference }
        return members
            .filter { it.polarity == PolarityV1.NEGATIVE }
            .any { negative ->
                val binding = negative.auditBinding as AuditBindingV1.Negative
                val positive = positives[binding.positiveExampleReference]
                positive != null && (
                    positive.modelInput != negative.modelInput ||
                        positive.auditBinding != AuditBindingV1.Positive(binding.positiveExampleReference)
                    )
            }
    }

    private fun hasContradictorySupervision(members: List<MemberV1>): Boolean {
        val positives = members.filter { it.polarity == PolarityV1.POSITIVE }
        val positiveTargetsByInput = positives.groupBy { inputKey(it.modelInput) }
        if (positiveTargetsByInput.values.any { group ->
                group.map { member ->
                    targetKey((member.supervision as SupervisionV1.Positive).target)
                }.distinct().size > 1
            }
        ) return true

        return members
            .filter { it.polarity == PolarityV1.NEGATIVE }
            .any { negative ->
                val supervision = negative.supervision as SupervisionV1.Negative
                positives.any { positive ->
                    inputKey(positive.modelInput) == inputKey(negative.modelInput) &&
                        targetKey((positive.supervision as SupervisionV1.Positive).target) ==
                        targetKey(supervision.rejectedTarget)
                }
            }
    }

    private fun corpusLogicalDigest(corpus: CorpusV1): HimSha256 = HimSha256(
        sha256(buildString {
            field("domain", CORPUS_DIGEST_DOMAIN)
            field("contractId", CONTRACT_ID)
            field("version", VERSION)
            field("state", STATE)
            corpus.members.forEachIndexed { index, member ->
                field("member-$index", memberKey(member))
            }
            field("total", corpus.counters.total.toString())
            field("positive", corpus.counters.positive.toString())
            field("negative", corpus.counters.negative.toString())
        }),
    )

    private fun memberKey(member: MemberV1) = buildString {
        field("polarity", member.polarity.name)
        field("membershipReference", member.membershipReference)
        field("input", inputKey(member.modelInput))
        field("supervision", supervisionKey(member.supervision))
        field("auditBinding", auditBindingKey(member.auditBinding))
    }

    private fun supervisionKey(supervision: SupervisionV1) = when (supervision) {
        is SupervisionV1.Positive -> "POSITIVE|${targetKey(supervision.target)}"
        is SupervisionV1.Negative -> "NEGATIVE|${targetKey(supervision.rejectedTarget)}|${supervision.boundaryType.name}"
    }

    private fun auditBindingKey(binding: AuditBindingV1) = when (binding) {
        is AuditBindingV1.Positive -> "POSITIVE|${binding.exampleReference.value}"
        is AuditBindingV1.Negative ->
            "NEGATIVE|${binding.p1MaterializationId.value}|${binding.negativeExampleReference.value}|" +
                "${binding.positiveExampleReference.value}|${binding.negativeRecordLogicalDigest.value}"
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
        is HimTrainingTargetV1.Variant ->
            "VARIANT|${target.scope.canonicalId.value}|${target.scope::class.simpleName}|${target.scope.toString()}"
        is HimTrainingTargetV1.Alias ->
            "ALIAS|${target.equivalentEntity.canonicalId.value}|${target.equivalentEntity::class.simpleName}|${target.equivalentEntity.toString()}"
        is HimTrainingTargetV1.NewCanonical -> "NEW_CANONICAL|${target.proposedCanonicalName.orEmpty()}"
    }

    private fun encodedField(name: String, value: String): String =
        "$name=${value.length}:$value\n"

    private fun StringBuilder.field(name: String, value: String) {
        append(encodedField(name, value))
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
