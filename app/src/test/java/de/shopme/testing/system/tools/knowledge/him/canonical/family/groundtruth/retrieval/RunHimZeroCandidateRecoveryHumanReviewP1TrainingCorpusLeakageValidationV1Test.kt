package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionAssignmentV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionDiagnosticsV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionLeakageLevelV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionManifestV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionPolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1Test {
    @Test
    fun cleanPartitionProducesCompletedValidation() {
        val positive = Fixture.positive("clean-positive")
        val negative = Fixture.negative(positive, "clean-negative")
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(positive),
            HimTrainingPartitionRecordV1.Negative(negative),
        )

        val result = completed(Fixture.partition(source))

        assertTrue(result.validationResult.valid)
        assertTrue(result.validationResult.diagnostics.none { it.fatal })
        assertEquals(source.snapshotBinding, result.snapshotBinding)
        assertEquals(Fixture.partition(source).partitionManifest.logicalDigest, result.partitionManifestLogicalDigest)
    }

    @Test
    fun exactExampleLeakageIsTypedAndPreserved() {
        val positive = Fixture.positive("duplicate")
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(positive),
            HimTrainingPartitionRecordV1.Positive(positive),
        )
        val group = Fixture.group(Fixture.CANONICAL_ID)
        val manifest = Fixture.manifest(
            listOf(
                HimTrainingPartitionAssignmentV1(
                    HimTrainingPartitionRecordV1.Positive(positive), group, Fixture.partitionFor(Fixture.CANONICAL_ID),
                ),
                HimTrainingPartitionAssignmentV1(
                    HimTrainingPartitionRecordV1.Positive(positive), group, Fixture.partitionFor(Fixture.CANONICAL_ID),
                ),
            ),
        )

        val failure = failed(Fixture.partitionResult(source, manifest))

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.FailureReasonV1.LEAKAGE_DETECTED,
            failure.reason,
        )
        assertTrue(failure.validationResult!!.diagnostics.any {
            it.level == HimTrainingPartitionLeakageLevelV1.EXACT_EXAMPLE_LEAKAGE && it.fatal
        })
    }

    @Test
    fun derivedNegativeCrossPartitionLeakageIsTyped() {
        val positive = Fixture.positive("derived-positive")
        val negative = Fixture.negative(positive, "derived-negative")
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(positive),
            HimTrainingPartitionRecordV1.Negative(negative),
        )
        val group = Fixture.group(Fixture.CANONICAL_ID)
        val result = failed(
            Fixture.partitionResult(
                source,
                Fixture.manifest(
                    listOf(
                        HimTrainingPartitionAssignmentV1(
                            HimTrainingPartitionRecordV1.Positive(positive), group, Fixture.partitionFor(Fixture.CANONICAL_ID),
                        ),
                        HimTrainingPartitionAssignmentV1(
                            HimTrainingPartitionRecordV1.Negative(negative), group, Fixture.oppositePartition(Fixture.CANONICAL_ID),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.FailureReasonV1.LEAKAGE_DETECTED,
            result.reason,
        )
        assertTrue(result.validationResult!!.diagnostics.any {
            it.level == HimTrainingPartitionLeakageLevelV1.DERIVED_NEGATIVE_LEAKAGE
        })
    }

    @Test
    fun canonicalFamilyCrossPartitionLeakageIsTyped() {
        val first = Fixture.positive("family-first")
        val second = Fixture.positive("family-second")
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(first),
            HimTrainingPartitionRecordV1.Positive(second),
        )
        val group = Fixture.group(Fixture.CANONICAL_ID)
        val failure = failed(
            Fixture.partitionResult(
                source,
                Fixture.manifest(
                    listOf(
                        HimTrainingPartitionAssignmentV1(
                            HimTrainingPartitionRecordV1.Positive(first), group, Fixture.partitionFor(Fixture.CANONICAL_ID),
                        ),
                        HimTrainingPartitionAssignmentV1(
                            HimTrainingPartitionRecordV1.Positive(second), group, Fixture.oppositePartition(Fixture.CANONICAL_ID),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(failure.validationResult!!.diagnostics.any {
            it.level == HimTrainingPartitionLeakageLevelV1.CANONICAL_FAMILY_LEAKAGE
        })
    }

    @Test
    fun lineageCrossPartitionLeakageIsTyped() {
        val provenance = HimTrainingProvenanceV1(candidateReference = HimCandidateReference("shared-lineage"))
        val first = Fixture.positive("lineage-first", Fixture.CANONICAL_ID, provenance = provenance)
        val secondId = Fixture.distinctOtherId()
        val second = Fixture.positive("lineage-second", secondId, provenance = provenance)
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(first),
            HimTrainingPartitionRecordV1.Positive(second),
        )
        val failure = failed(
            Fixture.partitionResult(
                source,
                Fixture.manifest(
                    listOf(
                        HimTrainingPartitionAssignmentV1(
                            HimTrainingPartitionRecordV1.Positive(first), Fixture.group(Fixture.CANONICAL_ID), Fixture.partitionFor(Fixture.CANONICAL_ID),
                        ),
                        HimTrainingPartitionAssignmentV1(
                            HimTrainingPartitionRecordV1.Positive(second), Fixture.group(secondId), Fixture.partitionFor(secondId),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(failure.validationResult!!.diagnostics.any {
            it.level == HimTrainingPartitionLeakageLevelV1.LINEAGE_LEAKAGE
        })
    }

    @Test
    fun duplicateSemanticTargetLeakageIsTypedAndRetained() {
        val first = Fixture.positive(
            seed = "semantic",
            provenance = HimTrainingProvenanceV1(candidateReference = HimCandidateReference("semantic-one")),
        )
        val second = Fixture.positive(
            seed = "semantic",
            provenance = HimTrainingProvenanceV1(candidateReference = HimCandidateReference("semantic-two")),
        )
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(first),
            HimTrainingPartitionRecordV1.Positive(second),
        )
        val failure = failed(
            Fixture.partitionResult(
                source,
                Fixture.manifest(
                    listOf(
                        HimTrainingPartitionAssignmentV1(
                            HimTrainingPartitionRecordV1.Positive(first), Fixture.group(Fixture.CANONICAL_ID), Fixture.partitionFor(Fixture.CANONICAL_ID),
                        ),
                        HimTrainingPartitionAssignmentV1(
                            HimTrainingPartitionRecordV1.Positive(second), Fixture.group(Fixture.CANONICAL_ID), Fixture.oppositePartition(Fixture.CANONICAL_ID),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(failure.validationResult!!.diagnostics.any {
            it.level == HimTrainingPartitionLeakageLevelV1.DUPLICATE_SEMANTIC_TARGET_LEAKAGE
        })
    }

    @Test
    fun relatedPositiveAndNegativesInOnePartitionRemainValid() {
        val positive = Fixture.positive("same-partition-positive")
        val first = Fixture.negative(positive, "same-partition-first")
        val second = Fixture.negative(positive, "same-partition-second")
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(positive),
            HimTrainingPartitionRecordV1.Negative(first),
            HimTrainingPartitionRecordV1.Negative(second),
        )

        val result = completed(Fixture.partition(source))

        assertTrue(result.validationResult.valid)
        assertTrue(result.validationResult.diagnostics.none { it.fatal })
    }

    @Test
    fun unrelatedFamiliesInDifferentPartitionsRemainValid() {
        val first = Fixture.positive("unrelated-first", Fixture.CANONICAL_ID)
        val secondId = Fixture.distinctOtherId()
        val second = Fixture.positive("unrelated-second", secondId)
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(first),
            HimTrainingPartitionRecordV1.Positive(second),
        )
        val manifest = Fixture.manifest(
            listOf(
                HimTrainingPartitionAssignmentV1(
                    HimTrainingPartitionRecordV1.Positive(first), Fixture.group(Fixture.CANONICAL_ID), Fixture.partitionFor(Fixture.CANONICAL_ID),
                ),
                HimTrainingPartitionAssignmentV1(
                    HimTrainingPartitionRecordV1.Positive(second), Fixture.group(secondId), Fixture.partitionFor(secondId),
                ),
            ),
        )

        val result = completed(Fixture.partitionResult(source, manifest))

        assertTrue(result.validationResult.valid)
    }

    @Test
    fun contextOnlyCrossPartitionReferenceIsNonFatal() {
        val secondId = Fixture.distinctOtherId()
        val first = Fixture.positive(
            "context-first",
            Fixture.CANONICAL_ID,
            contextIds = listOf(Fixture.CANONICAL_ID, secondId),
        )
        val second = Fixture.positive("context-second", secondId)
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(first),
            HimTrainingPartitionRecordV1.Positive(second),
        )
        val manifest = Fixture.manifest(
            listOf(
                HimTrainingPartitionAssignmentV1(
                    HimTrainingPartitionRecordV1.Positive(first), Fixture.group(Fixture.CANONICAL_ID), Fixture.partitionFor(Fixture.CANONICAL_ID),
                ),
                HimTrainingPartitionAssignmentV1(
                    HimTrainingPartitionRecordV1.Positive(second), Fixture.group(secondId), Fixture.partitionFor(secondId),
                ),
            ),
        )

        val result = completed(Fixture.partitionResult(source, manifest))

        assertTrue(result.validationResult.valid)
        assertTrue(result.validationResult.diagnostics.any {
            it.level == HimTrainingPartitionLeakageLevelV1.CONTEXT_ONLY_CROSS_PARTITION_REFERENCE && !it.fatal
        })
    }

    @Test
    fun emptyManifestIsLeakageValidWithoutReadinessClaim() {
        val source = Fixture.sourceBinding()

        val result = completed(Fixture.partition(source))

        assertTrue(result.validationResult.valid)
        assertTrue(result.validationResult.diagnostics.isEmpty())
        assertEquals(0, result.partitionCounters.total)
    }

    @Test
    fun smallCorpusWithEmptySplitsRemainsLeakageValid() {
        val first = Fixture.positive("small-one", Fixture.CANONICAL_ID)
        val second = Fixture.positive("small-two", Fixture.CANONICAL_ID)
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(first),
            HimTrainingPartitionRecordV1.Positive(second),
        )
        val manifest = Fixture.manifest(
            listOf(
                HimTrainingPartitionAssignmentV1(
                    HimTrainingPartitionRecordV1.Positive(first), Fixture.group(Fixture.CANONICAL_ID), Fixture.partitionFor(Fixture.CANONICAL_ID),
                ),
                HimTrainingPartitionAssignmentV1(
                    HimTrainingPartitionRecordV1.Positive(second), Fixture.group(Fixture.CANONICAL_ID), Fixture.partitionFor(Fixture.CANONICAL_ID),
                ),
            ),
        )

        assertTrue(completed(Fixture.partitionResult(source, manifest)).validationResult.valid)
    }

    @Test
    fun invalidSnapshotFailsBeforeLeakageValidation() {
        val source = Fixture.sourceBinding()
        val partition = Fixture.partition(source)
        val broken = partition.copy(snapshotBinding = Fixture.snapshot("alternate"))

        val failure = failed(broken, source)

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.FailureReasonV1.INVALID_PARTITION_RESULT,
            failure.reason,
        )
        assertEquals(null, failure.validationResult)
    }

    @Test
    fun staleManifestDigestFailsBeforeLeakageValidation() {
        val positive = Fixture.positive("stale-manifest")
        val source = Fixture.sourceBinding(HimTrainingPartitionRecordV1.Positive(positive))
        val partition = Fixture.partition(source)
        val brokenManifest = partition.partitionManifest.copy(logicalDigest = HimSha256("d".repeat(64)))

        val failure = failed(partition.copy(partitionManifest = brokenManifest))

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.FailureReasonV1.INVALID_PARTITION_RESULT,
            failure.reason,
        )
    }

    @Test
    fun inconsistentPartitionCountersFailBeforeLeakageValidation() {
        val positive = Fixture.positive("counter-mismatch")
        val source = Fixture.sourceBinding(HimTrainingPartitionRecordV1.Positive(positive))
        val partition = Fixture.partition(source)
        val brokenCounters = partition.counters.copy(total = partition.counters.total + 1, train = partition.counters.train + 1)

        val failure = failed(partition.copy(counters = brokenCounters))

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.FailureReasonV1.INVALID_PARTITION_RESULT,
            failure.reason,
        )
    }

    @Test
    fun multipleDiagnosticsArePreservedExactly() {
        val positive = Fixture.positive("multi-diagnostic")
        val negative = Fixture.negative(positive, "multi-diagnostic-negative")
        val otherId = Fixture.distinctOtherId()
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(positive),
            HimTrainingPartitionRecordV1.Negative(negative),
        )
        val manifest = Fixture.manifest(
            listOf(
                HimTrainingPartitionAssignmentV1(
                    HimTrainingPartitionRecordV1.Positive(positive), Fixture.group(Fixture.CANONICAL_ID), HimTrainingPartitionV1.TRAIN,
                ),
                HimTrainingPartitionAssignmentV1(
                    HimTrainingPartitionRecordV1.Negative(negative), Fixture.group(otherId), Fixture.partitionFor(otherId),
                ),
            ),
        )
        val failure = failed(Fixture.partitionResult(source, manifest))
        val diagnostics = failure.validationResult!!.diagnostics

        assertTrue(diagnostics.any {
            it.level == HimTrainingPartitionLeakageLevelV1.DERIVED_NEGATIVE_LEAKAGE && it.fatal
        })
        assertTrue(diagnostics.any {
            it.level == HimTrainingPartitionLeakageLevelV1.CANONICAL_FAMILY_LEAKAGE && it.fatal
        })
        diagnostics.forEach { diagnostic ->
            assertTrue(diagnostic.key.isNotBlank())
            assertTrue(diagnostic.recordReferences.isNotEmpty())
            assertTrue(diagnostic.message.isNotBlank())
        }
    }

    @Test
    fun repeatedValidExecutionIsDeterministic() {
        val positive = Fixture.positive("deterministic")
        val negative = Fixture.negative(positive, "deterministic-negative")
        val source = Fixture.sourceBinding(
            HimTrainingPartitionRecordV1.Positive(positive),
            HimTrainingPartitionRecordV1.Negative(negative),
        )
        val partition = Fixture.partition(source)

        val first = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Request(partition, source),
        )
        val second = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Request(partition, source),
        )

        assertEquals(first, second)
    }

    @Test
    fun completedResultCarriesExactPartitionEvidence() {
        val positive = Fixture.positive("binding")
        val source = Fixture.sourceBinding(HimTrainingPartitionRecordV1.Positive(positive))
        val partition = Fixture.partition(source)
        val result = completed(partition)

        assertEquals(partition.snapshotBinding, result.snapshotBinding)
        assertEquals(partition.partitionManifest.logicalDigest, result.partitionManifestLogicalDigest)
        assertEquals(partition.counters, result.partitionCounters)
        assertEquals(
            HimTrainingPartitionDiagnosticsV1.from(partition.partitionManifest.assignments),
            partition.partitionManifest.diagnostics,
        )
    }

    private fun completed(
        partition: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed,
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Result.Completed = assertIs<
        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Result.Completed
    >(
        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Request(
                partition,
                Fixture.sourceBindingFromPartition(partition),
            ),
        ),
    )

    private fun failed(
        partition: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed,
        source: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed =
            Fixture.sourceBindingFromPartition(partition),
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Result.Failed = assertIs<
        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Result.Failed
    >(
        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusLeakageValidationV1.Request(partition, source),
        ),
    )

    private object Fixture {
        val CANONICAL_ID = HimEntityId("AbCd12")
        val OTHER_ID = HimEntityId("EfGh34")
        private val DEFAULT_SNAPSHOT = snapshot("default")

        fun positive(
            seed: String,
            canonicalId: HimEntityId = CANONICAL_ID,
            contextIds: List<HimEntityId> = listOf(canonicalId),
            provenance: HimTrainingProvenanceV1 = HimTrainingProvenanceV1(),
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
                    de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(
                        index + 1,
                        id,
                        "Canonical ${id.value}",
                        null,
                    )
                },
                evidence = listOf(HimTrainingEvidenceInputV1(evidence, "FOOD", 1)),
            )
            return HimTrainingExampleV1.create(
                taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
                input = input,
                target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(canonicalId)),
                provenance = provenance.copy(sourceEvidenceReferences = listOf(evidence)),
            )
        }

        fun negative(
            positive: HimTrainingExampleV1,
            seed: String,
        ): HimNegativeTrainingExampleV1 = HimNegativeTrainingExampleV1.create(
            positiveExample = positive,
            rejectedTarget = HimTrainingTargetV1.NewCanonical("rejected $seed"),
            boundaryType = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )

        fun sourceBinding(
            vararg records: HimTrainingPartitionRecordV1,
        ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed =
            sourceBinding(records.toList(), DEFAULT_SNAPSHOT)

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
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Negative(
                            membershipReference = "member:negative:$index",
                            snapshotBinding = snapshot,
                            negativeExample = record.negativeExample,
                            p1MaterializationId =
                                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1(
                                    "negative-materialization:v1:${sha256(index.toString())}",
                                ),
                            durableRecordLogicalDigest = HimSha256("b".repeat(64)),
                        )
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

        fun partitionResult(
            source: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed,
            manifest: HimTrainingPartitionManifestV1,
        ) = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed(
            snapshotBinding = source.snapshotBinding,
            partitionManifest = manifest,
            counters = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters.from(manifest.assignments),
        )

        fun manifest(assignments: List<HimTrainingPartitionAssignmentV1>) = HimTrainingPartitionManifestV1.create(assignments)

        fun sourceBindingFromPartition(
            partition: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Result.Completed,
        ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed {
            val records = partition.partitionManifest.assignments.map { it.record }
            return sourceBinding(records.toList(), partition.snapshotBinding)
        }

        fun group(id: HimEntityId) =
            de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1.canonical(id)

        fun partitionFor(id: HimEntityId): HimTrainingPartitionV1 =
            HimTrainingPartitionPolicyV1.partitionForGroup(group(id))

        fun oppositePartition(id: HimEntityId): HimTrainingPartitionV1 = when (val partition = partitionFor(id)) {
            HimTrainingPartitionV1.TRAIN -> HimTrainingPartitionV1.HOLDOUT
            HimTrainingPartitionV1.VALIDATION -> HimTrainingPartitionV1.TRAIN
            HimTrainingPartitionV1.HOLDOUT -> HimTrainingPartitionV1.TRAIN
        }

        fun distinctOtherId(): HimEntityId = listOf(
            OTHER_ID,
            HimEntityId("GhIj56"),
            HimEntityId("KlMn78"),
            HimEntityId("QrSt90"),
        ).first { partitionFor(it) != partitionFor(CANONICAL_ID) }

        fun snapshot(seed: String): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1 {
            val digest = HimSha256(sha256(seed))
            return HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1(
                "training-corpus:v1:${digest.value}",
                digest,
            )
        }

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
