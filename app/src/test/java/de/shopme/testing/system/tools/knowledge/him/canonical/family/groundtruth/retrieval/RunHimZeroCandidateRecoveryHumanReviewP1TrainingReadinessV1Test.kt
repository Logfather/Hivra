package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1Test {
    @Test
    fun validLeakageValidatedPartitionProducesReadyResult() {
        val partition = Fixture.partition(Fixture.sourceBinding(Fixture.positive("ready")))
        val leakage = Fixture.leakage(partition)

        val result = ready(leakage, partition)

        assertEquals(partition.snapshotBinding, result.snapshotBinding)
        assertEquals(partition.partitionManifest.logicalDigest, result.partitionManifestLogicalDigest)
        assertEquals(partition.counters, result.partitionCounters)
        assertEquals(partition.partitionManifest.policyVersion, result.partitionPolicyVersion)
        assertEquals(leakage.validationResult, result.leakageValidationResult)
        assertTrue(result.leakageValidationResult.valid)
    }

    @Test
    fun repeatedReadyEvaluationIsDeterministic() {
        val partition = Fixture.partition(Fixture.sourceBinding(Fixture.positive("deterministic")))
        val leakage = Fixture.leakage(partition)
        val request = HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Request(leakage, partition)

        val first = HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.execute(request)
        val second = HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.execute(request)

        assertEquals(first, second)
    }

    @Test
    fun emptyCorpusIsNotReady() {
        val partition = Fixture.partition(Fixture.sourceBinding())
        val leakage = Fixture.leakage(partition)

        val result = notReady(leakage, partition)

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Reason.TRAINING_CORPUS_EMPTY,
            result.reason,
        )
    }

    @Test
    fun emptyTrainPartitionIsNotReady() {
        val id = Fixture.idFor(HimTrainingPartitionV1.VALIDATION)
        val partition = Fixture.partition(Fixture.sourceBinding(Fixture.positive("no-train", id)))
        val leakage = Fixture.leakage(partition)

        assertEquals(0, partition.counters.train)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Reason.TRAIN_PARTITION_EMPTY,
            notReady(leakage, partition).reason,
        )
    }

    @Test
    fun emptyValidationPartitionIsAllowed() {
        val id = Fixture.idFor(HimTrainingPartitionV1.TRAIN)
        val partition = Fixture.partition(Fixture.sourceBinding(Fixture.positive("no-validation", id)))
        val leakage = Fixture.leakage(partition)

        assertEquals(0, partition.counters.validation)
        assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Request(leakage, partition),
            ),
        )
    }

    @Test
    fun emptyHoldoutPartitionIsAllowed() {
        val id = Fixture.idFor(HimTrainingPartitionV1.TRAIN)
        val partition = Fixture.partition(Fixture.sourceBinding(Fixture.positive("no-holdout", id)))
        val leakage = Fixture.leakage(partition)

        assertEquals(0, partition.counters.holdout)
        assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Request(leakage, partition),
            ),
        )
    }

    @Test
    fun tinyTrainOnlyCorpusIsReadyWithoutArtificialMinimum() {
        val id = Fixture.idFor(HimTrainingPartitionV1.TRAIN)
        val partition = Fixture.partition(Fixture.sourceBinding(Fixture.positive("tiny", id)))
        val leakage = Fixture.leakage(partition)

        assertEquals(1, partition.counters.total)
        assertEquals(1, partition.counters.train)
        assertEquals(0, partition.counters.validation)
        assertEquals(0, partition.counters.holdout)
        assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Request(leakage, partition),
            ),
        )
    }

    @Test
    fun failedLeakageValidationIsNotReady() {
        val partition = Fixture.partition(Fixture.sourceBinding(Fixture.positive("failed-leakage")))
        val leakage = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Result.Failed(
            reason = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.FailureReasonV1.LEAKAGE_DETECTED,
            safeContext = "partition-manifest",
        )

        val result = notReady(leakage, partition)

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Reason.INVALID_LEAKAGE_VALIDATION_RESULT,
            result.reason,
        )
    }

    @Test
    fun staleSnapshotBindingIsNotReady() {
        val partition = Fixture.partition(Fixture.sourceBinding(Fixture.positive("stale-snapshot")))
        val leakage = Fixture.leakage(partition)
        val stalePartition = partition.copy(snapshotBinding = Fixture.snapshot("alternate"))

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Reason.TRAINING_BINDING_MISMATCH,
            notReady(leakage, stalePartition).reason,
        )
    }

    @Test
    fun staleManifestBindingIsNotReady() {
        val partition = Fixture.partition(Fixture.sourceBinding(Fixture.positive("stale-manifest")))
        val leakage = Fixture.leakage(partition)
        val staleManifest = partition.partitionManifest.copy(logicalDigest = HimSha256("d".repeat(64)))
        val stalePartition = partition.copy(partitionManifest = staleManifest)

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Reason.TRAINING_BINDING_MISMATCH,
            notReady(leakage, stalePartition).reason,
        )
    }

    @Test
    fun counterMismatchIsNotReady() {
        val partition = Fixture.partition(Fixture.sourceBinding(Fixture.positive("stale-counters")))
        val leakage = Fixture.leakage(partition)
        val staleCounters = partition.counters.copy(
            total = partition.counters.total + 1,
            validation = partition.counters.validation + 1,
        )

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Reason.TRAINING_BINDING_MISMATCH,
            notReady(leakage, partition.copy(counters = staleCounters)).reason,
        )
    }

    @Test
    fun nonFatalLeakageDiagnosticsRemainReadyAndArePreserved() {
        val trainId = Fixture.idFor(HimTrainingPartitionV1.TRAIN)
        val otherId = Fixture.idForNot(HimTrainingPartitionV1.TRAIN)
        val first = Fixture.positive(
            "context-first",
            trainId,
            contextIds = listOf(trainId, otherId),
        )
        val second = Fixture.positive("context-second", otherId)
        val partition = Fixture.partition(Fixture.sourceBinding(first, second))
        val leakage = Fixture.leakage(partition)
        val result = ready(leakage, partition)

        assertTrue(leakage.validationResult.valid)
        assertTrue(leakage.validationResult.diagnostics.isNotEmpty())
        assertTrue(leakage.validationResult.diagnostics.all { !it.fatal })
        assertEquals(leakage.validationResult, result.leakageValidationResult)
    }

    @Test
    fun readyResultUsesExistingPartitionPolicyWithoutBalancing() {
        val id = Fixture.idFor(HimTrainingPartitionV1.TRAIN)
        val partition = Fixture.partition(Fixture.sourceBinding(Fixture.positive("policy", id)))
        val result = ready(Fixture.leakage(partition), partition)

        assertEquals("HIM_TRAINING_PARTITION_POLICY_V1", result.partitionPolicyVersion)
        assertEquals(partition.counters, result.partitionCounters)
    }

    private fun ready(
        leakage: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Result,
        partition: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed,
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready = assertIs<
        HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready
    >(
        HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Request(leakage, partition),
        ),
    )

    private fun notReady(
        leakage: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Result,
        partition: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed,
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.NotReady = assertIs<
        HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.NotReady
    >(
        HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Request(leakage, partition),
        ),
    )

    private object Fixture {
        private val DEFAULT_SNAPSHOT = snapshot("default")

        fun positive(
            seed: String,
            canonicalId: HimEntityId = HimEntityId("AbCd12"),
            contextIds: List<HimEntityId> = listOf(canonicalId),
        ): HimTrainingExampleV1 {
            val evidence = HimEvidenceReference(
                "OPEN_FOOD_FACTS",
                HimSha256("c".repeat(64)),
                "off:product:row:$seed",
            )
            val input = HimTrainingInputV1(
                observedTerm = "unknown food $seed",
                normalizedObservedTerm = "unknown food $seed",
                canonicalContext = contextIds.mapIndexed { index, id ->
                    HimCandidateCanonicalContext(index + 1, id, "Canonical ${id.value}", null)
                },
                evidence = listOf(
                    de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1(evidence, "FOOD", 1),
                ),
            )
            return HimTrainingExampleV1.create(
                taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
                input = input,
                target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(canonicalId)),
                provenance = HimTrainingProvenanceV1(sourceEvidenceReferences = listOf(evidence)),
            )
        }

        fun sourceBinding(
            vararg examples: HimTrainingExampleV1,
        ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed =
            sourceBinding(
                examples.map { HimTrainingPartitionRecordV1.Positive(it) },
                DEFAULT_SNAPSHOT,
            )

        private fun sourceBinding(
            records: List<HimTrainingPartitionRecordV1>,
            snapshot: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1,
        ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed {
            val bindings = records.mapIndexed { index, record ->
                when (record) {
                    is HimTrainingPartitionRecordV1.Positive ->
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Positive(
                            membershipReference = "member:positive:$index",
                            snapshotBinding = snapshot,
                            positiveExample = record.positiveExample,
                            durableReference = record.positiveExample.exampleReference,
                            durableRecordLogicalDigest = HimSha256("a".repeat(64)),
                        )
                    is HimTrainingPartitionRecordV1.Negative ->
                        error("negative fixture is not required")
                }
            }
            val set = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingSetV1(
                snapshot,
                bindings,
            )
            return HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed(snapshot, set)
        }

        fun partition(
            source: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed,
        ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed = assertIs<
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed
        >(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Request(source),
            ),
        )

        fun leakage(
            partition: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed,
        ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Result.Completed = assertIs<
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Result.Completed
        >(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Request(
                    partition,
                    sourceBindingFromPartition(partition),
                ),
            ),
        )

        private fun sourceBindingFromPartition(
            partition: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed,
        ) = sourceBinding(
            partition.partitionManifest.assignments.map { it.record },
            partition.snapshotBinding,
        )

        fun idFor(expected: HimTrainingPartitionV1): HimEntityId =
            generateSequence(1) { it + 1 }
                .map { HimEntityId("A${it.toString().padStart(5, '0')}") }
                .first { partitionFor(it) == expected }

        fun idForNot(unexpected: HimTrainingPartitionV1): HimEntityId =
            generateSequence(1) { it + 1 }
                .map { HimEntityId("B${it.toString().padStart(5, '0')}") }
                .first { partitionFor(it) != unexpected }

        private fun partitionFor(id: HimEntityId): HimTrainingPartitionV1 =
            de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionPolicyV1.partitionForGroup(
                de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1.canonical(id),
            )

        fun snapshot(seed: String): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1 {
            val digest = HimSha256(sha256(seed))
            return HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1(
                "training-corpus:v1:${digest.value}",
                digest,
            )
        }

        private fun sha256(value: String): String =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
