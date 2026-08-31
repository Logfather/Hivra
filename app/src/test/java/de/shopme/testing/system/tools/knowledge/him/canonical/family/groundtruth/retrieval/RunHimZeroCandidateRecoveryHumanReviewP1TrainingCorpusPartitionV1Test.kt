package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionPolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RunHimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1Test {
    @Test
    fun singlePositiveProducesOneAuthoritativePartitionRecord() {
        val positive = Fixture.positive()
        val result = completed(sourceBinding(positiveBinding("positive", positive)))

        val assignment = result.partitionManifest.assignments.single()
        assertIs<de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1.Positive>(assignment.record)
        assertEquals(positive.exampleReference.value, assignment.record.recordReference)
        assertEquals(Fixture.groupReference(Fixture.CANONICAL_ID), assignment.groupReference)
        assertEquals(HimTrainingPartitionPolicyV1.partitionForGroup(assignment.groupReference), assignment.partition)
        assertEquals(snapshotBinding(), result.snapshotBinding)
    }

    @Test
    fun singleNegativePreservesFullRecordAndPositiveFamily() {
        val positive = Fixture.positive()
        val negative = Fixture.negative(positive, "negative")
        val result = completed(sourceBinding(negativeBinding("negative", negative)))

        val assignment = result.partitionManifest.assignments.single()
        val record = assertIs<de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1.Negative>(assignment.record)
        assertEquals(negative, record.negativeExample)
        assertEquals(negative.reference.value, record.recordReference)
        assertEquals(Fixture.groupReference(Fixture.CANONICAL_ID), assignment.groupReference)
        assertEquals(HimTrainingPartitionPolicyV1.partitionForGroup(assignment.groupReference), assignment.partition)
    }

    @Test
    fun positiveAndNegativeShareFamilyPartition() {
        val positive = Fixture.positive()
        val negative = Fixture.negative(positive, "negative")
        val result = completed(
            sourceBinding(
                positiveBinding("positive", positive),
                negativeBinding("negative", negative),
            ),
        )

        assertEquals(2, result.partitionManifest.assignments.size)
        assertEquals(1, result.partitionManifest.assignments.map { it.groupReference }.distinct().size)
        assertEquals(1, result.partitionManifest.assignments.map { it.partition }.distinct().size)
    }

    @Test
    fun manyToOnePreservesSeparateNegativeRecords() {
        val positive = Fixture.positive()
        val first = Fixture.negative(positive, "first")
        val second = Fixture.negative(positive, "second")
        val completed = completed(
            sourceBinding(
                positiveBinding("positive", positive),
                negativeBinding("first", first),
                negativeBinding("second", second),
            ),
        )

        val assignments = completed.partitionManifest.assignments

        assertEquals(3, assignments.size)

        val positiveAssignments = assignments.filter {
            it.record is HimTrainingPartitionRecordV1.Positive
        }

        val negativeAssignments = assignments.filter {
            it.record is HimTrainingPartitionRecordV1.Negative
        }

        assertEquals(1, positiveAssignments.size)
        assertEquals(2, negativeAssignments.size)

        assertEquals(
            2,
            negativeAssignments.map { it.record.recordReference }.distinct().size,
        )

        assertEquals(
            1,
            assignments.map { it.groupReference }.distinct().size,
        )

        assertEquals(
            1,
            assignments.map { it.partition }.distinct().size,
        )
    }


    @Test
    fun canonicalIdentityVariantAndAliasShareOneFamily() {
        val records = listOf(
            positiveBinding(
                "canonical",
                Fixture.positive("canonical", HimTrainingTargetV1.ExistingCanonical(Fixture.CANONICAL_ID)),
            ),
            positiveBinding(
                "identity",
                Fixture.positive("identity", HimTrainingTargetV1.Identity(Fixture.CANONICAL_ID)),
            ),
            positiveBinding(
                "variant",
                Fixture.positive(
                    "variant",
                    HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(Fixture.CANONICAL_ID)),
                ),
            ),
            positiveBinding(
                "alias",
                Fixture.positive(
                    "alias",
                    HimTrainingTargetV1.Alias(HimFamilyEntityReference.Canonical(Fixture.CANONICAL_ID)),
                ),
            ),
        )

        val result = completed(sourceBinding(*records.toTypedArray()))
        assertEquals(1, result.partitionManifest.assignments.map { it.groupReference }.distinct().size)
        assertEquals(1, result.partitionManifest.assignments.map { it.partition }.distinct().size)
    }

    @Test
    fun differentFamiliesRemainDistinctAndPolicyDeterminesEachPartition() {
        val first = positiveBinding("first", Fixture.positive("first", Fixture.variant(Fixture.CANONICAL_ID)))
        val second = positiveBinding("second", Fixture.positive("second", Fixture.variant(Fixture.OTHER_ID)))
        val result = completed(sourceBinding(first, second))

        assertEquals(2, result.partitionManifest.assignments.map { it.groupReference }.distinct().size)
        result.partitionManifest.assignments.forEach { assignment ->
            assertEquals(HimTrainingPartitionPolicyV1.partitionForGroup(assignment.groupReference), assignment.partition)
        }
    }

    @Test
    fun manifestUsesExistingCanonicalOrderingAndPolicy() {
        val positive = Fixture.positive()
        val negative = Fixture.negative(positive, "negative")
        val result = completed(
            sourceBinding(
                negativeBinding("negative", negative),
                positiveBinding("positive", positive),
            ),
        )

        assertEquals(
            result.partitionManifest.assignments.sortedWith(
                de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionOrderingV1.assignmentComparator,
            ),
            result.partitionManifest.assignments,
        )
        result.partitionManifest.assignments.forEach { assignment ->
            assertEquals(HimTrainingPartitionPolicyV1.partitionForGroup(assignment.groupReference), assignment.partition)
        }
    }

    @Test
    fun inputOrderDoesNotAffectManifestOrDigest() {
        val positive = Fixture.positive()
        val first = positiveBinding("positive", positive)
        val second = negativeBinding("negative", Fixture.negative(positive, "negative"))

        val firstResult = completed(sourceBinding(first, second))
        val secondResult = completed(sourceBinding(second, first))

        assertEquals(firstResult, secondResult)
        assertEquals(firstResult.partitionManifest.logicalDigest, secondResult.partitionManifest.logicalDigest)
    }

    @Test
    fun repeatedExecutionIsDeterministic() {
        val positive = Fixture.positive()
        val source = sourceBinding(
            positiveBinding("positive", positive),
            negativeBinding("negative", Fixture.negative(positive, "negative")),
        )

        val first = completed(source)
        val second = completed(source)
        assertEquals(first, second)
    }

    @Test
    fun emptySourceBindingProducesEmptyPartitionAndZeroCounters() {
        val result = completed(sourceBinding())

        assertEquals(emptyList(), result.partitionManifest.assignments)
        assertEquals(0, result.counters.total)
        assertEquals(0, result.counters.train)
        assertEquals(0, result.counters.validation)
        assertEquals(0, result.counters.holdout)
    }

    @Test
    fun oneTwoAndThreeFamilyGroupsRemainFullyCoveredWithoutRebalancing() {
        (1..3).forEach { count ->
            val bindings = (1..count).map { index ->
                val id = when (index) {
                    1 -> Fixture.CANONICAL_ID
                    2 -> Fixture.OTHER_ID
                    else -> Fixture.THIRD_ID
                }
                val positive = Fixture.positive("family-$index", Fixture.variant(id))
                positiveBinding("family-$index", positive)
            }
            val result = completed(sourceBinding(*bindings.toTypedArray()))
            assertEquals(count, result.partitionManifest.assignments.size)
            assertEquals(count, result.partitionManifest.assignments.map { it.groupReference }.distinct().size)
            assertEquals(count, result.counters.total)
        }
    }

    @Test
    fun countersAreDerivedFromAssignments() {
        val positive = Fixture.positive()
        val result = completed(
            sourceBinding(
                positiveBinding("positive", positive),
                negativeBinding("negative", Fixture.negative(positive, "negative")),
            ),
        )

        assertEquals(2, result.counters.total)
        assertEquals(
            result.counters.total,
            result.counters.train + result.counters.validation + result.counters.holdout,
        )
        assertEquals(
            result.partitionManifest.assignments.count { it.partition == HimTrainingPartitionV1.TRAIN },
            result.counters.train,
        )
    }

    @Test
    fun partitionResultCannotValidateAgainstAnotherSnapshot() {
        val positive = Fixture.positive()
        val firstSnapshot = snapshotBinding("a")
        val secondSnapshot = snapshotBinding("b")
        val first = sourceBindingWithSnapshot(
            listOf(positiveBinding("positive", positive, firstSnapshot)),
            firstSnapshot,
        )
        val second = sourceBindingWithSnapshot(
            listOf(positiveBinding("positive", positive, secondSnapshot)),
            secondSnapshot,
        )
        val result = completed(first)

        assertFailsWith<IllegalArgumentException> {
            result.validateAgainst(second)
        }
    }

    @Test
    fun mismatchedSourceBindingResultFailsClosed() {
        val source = sourceBinding(positiveBinding("positive", Fixture.positive()))
        val otherBinding = snapshotBinding("c")
        val malformed =
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed(
                otherBinding,
                source.sourceBindings,
            )

        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Failed>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Request(malformed),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.FailureReasonV1.INVALID_SOURCE_BINDING,
            failure.reason,
        )
    }

    @Test
    fun newCanonicalCannotBeGroupedAndFailsClosed() {
        val positive = Fixture.positive("new", HimTrainingTargetV1.NewCanonical("new canonical"))
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Failed>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Request(
                    sourceBinding(positiveBinding("new", positive)),
                ),
            ),
        )

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.FailureReasonV1.PARTITION_RECORD_MAPPING_FAILED,
            failure.reason,
        )
    }

    @Test
    fun duplicateRecordReferenceFailsClosedWithoutDeduplication() {
        val positive = Fixture.positive()
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Failed>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Request(
                    sourceBinding(
                        positiveBinding("first", positive),
                        positiveBinding("second", positive),
                    ),
                ),
            ),
        )

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.FailureReasonV1.INCOMPLETE_PARTITION_COVERAGE,
            failure.reason,
        )
    }

    private fun completed(
        sourceBinding: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed,
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Request(sourceBinding),
            ),
        )

    private fun sourceBinding(
        vararg bindings: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1,
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed =
        sourceBindingWithSnapshot(bindings.toList(), snapshotBinding())

    private fun sourceBindingWithSnapshot(
        bindings: List<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1>,
        snapshotBinding: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1,
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed {
        val set = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingSetV1(
            snapshotBinding,
            bindings,
        )
        return HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed(
            snapshotBinding,
            set,
        )
    }

    private fun snapshotBinding(
        seed: String = "default",
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1 {
        val digest = HimSha256(sha256(seed))
        return HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1(
            "training-corpus:v1:${digest.value}",
            digest,
        )
    }

    private fun positiveBinding(
        membership: String,
        example: HimTrainingExampleV1,
        bindingSnapshot: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1 =
            snapshotBinding(),
    ) = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Positive(
        membershipReference = "member:positive:$membership",
        snapshotBinding = bindingSnapshot,
        positiveExample = example,
        durableReference = example.exampleReference,
        durableRecordLogicalDigest = HimSha256("a".repeat(64)),
    )

    private fun negativeBinding(
        membership: String,
        example: HimNegativeTrainingExampleV1,
    ) = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Negative(
        membershipReference = "member:negative:$membership",
        snapshotBinding = snapshotBinding(),
        negativeExample = example,
        p1MaterializationId =
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1(
                "negative-materialization:v1:${sha256(membership)}",
            ),
        durableRecordLogicalDigest = HimSha256("b".repeat(64)),
    )

    private object Fixture {
        val CANONICAL_ID = HimEntityId("AbCd12")
        val OTHER_ID = HimEntityId("EfGh34")
        val THIRD_ID = HimEntityId("IjKl56")
        private val evidence = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("c".repeat(64)), "off:product:row:1")

        fun variant(id: HimEntityId) = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(id))

        fun groupReference(id: HimEntityId) = HimTrainingFamilyGroupReferenceV1.canonical(id)

        fun positive(
            seed: String = "base",
            target: HimTrainingTargetV1 = variant(CANONICAL_ID),
        ) = HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = HimTrainingInputV1(
                observedTerm = "unknown food $seed",
                normalizedObservedTerm = "unknown food $seed",
                canonicalContext = listOf(HimCandidateCanonicalContext(1, CANONICAL_ID, "Canonical", null)),
                evidence = listOf(HimTrainingEvidenceInputV1(evidence, "FOOD", 1)),
            ),
            target = target,
            provenance = HimTrainingProvenanceV1(sourceEvidenceReferences = listOf(evidence)),
        )

        fun negative(
            positive: HimTrainingExampleV1,
            seed: String,
        ) = HimNegativeTrainingExampleV1.create(
            positiveExample = positive,
            rejectedTarget = HimTrainingTargetV1.NewCanonical("rejected $seed"),
            boundaryType = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
