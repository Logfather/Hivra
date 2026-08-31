package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimPositiveTrainingExamplePersistenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1Test {
    @Test
    fun positiveRecordIsBoundWithFullAuthoritativeExample() = withRoots { positiveRoot, negativeRoot ->
        val example = Fixture.positive()
        val persisted = persistPositive(positiveRoot, example)
        val snapshot = snapshot(positive = example)
        val completed = complete(snapshot, positiveRoot, negativeRoot)
        val binding = completed.sourceBindings.bindings.single() as
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Positive

        assertEquals(snapshot.snapshotId, completed.snapshotBinding.snapshotId)
        assertEquals(snapshot.corpusLogicalDigest, completed.snapshotBinding.corpusLogicalDigest)
        assertEquals(example, binding.positiveExample)
        assertEquals(example.exampleReference, binding.durableReference)
        assertEquals(HimSha256(persisted.logicalDigest), binding.durableRecordLogicalDigest)
    }

    @Test
    fun positiveBindingCapturesPersistedRecordDigest() = withRoots { positiveRoot, negativeRoot ->
        val example = Fixture.positive()
        val persisted = persistPositive(positiveRoot, example)
        val binding = completedBinding(snapshot(positive = example), positiveRoot, negativeRoot)
            .bindings.single() as HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Positive

        assertEquals(HimSha256(persisted.logicalDigest), binding.durableRecordLogicalDigest)
        assertTrue(binding.durableRecordLogicalDigest.value.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun negativeRecordIsBoundWithP1IdentityAndFullExample() = withRoots { positiveRoot, negativeRoot ->
        val positive = Fixture.positive()
        val negative = persistNegative(negativeRoot, positive, "a")
        val snapshot = snapshot(negative = listOf(negative))
        val completed = complete(snapshot, positiveRoot, negativeRoot)
        val binding = completed.sourceBindings.bindings.single() as
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Negative

        assertEquals(negative.genericNegativeExample, binding.negativeExample)
        assertEquals(negative.p1MaterializationId, binding.p1MaterializationId)
        assertEquals(HimSha256(negative.recordLogicalDigest), binding.durableRecordLogicalDigest)
        assertEquals(snapshot.members.single().membershipReference, binding.membershipReference)
    }

    @Test
    fun mixedSnapshotPreservesFrozenMemberOrder() = withRoots { positiveRoot, negativeRoot ->
        val positive = Fixture.positive()
        persistPositive(positiveRoot, positive)
        val negative = persistNegative(negativeRoot, positive, "b")
        val snapshot = snapshot(positive, listOf(negative))
        val completed = complete(snapshot, positiveRoot, negativeRoot)

        assertEquals(
            snapshot.members.map { it.membershipReference },
            completed.sourceBindings.bindings.map { it.membershipReference },
        )
        assertEquals(
            snapshot.members.map { it.polarity },
            completed.sourceBindings.bindings.map { binding ->
                when (binding) {
                    is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Positive ->
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.POSITIVE
                    is HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Negative ->
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.NEGATIVE
                }
            },
        )
    }

    @Test
    fun manyToOneKeepsNegativeMembershipsSeparate() = withRoots { positiveRoot, negativeRoot ->
        val positive = Fixture.positive()
        persistPositive(positiveRoot, positive)
        val first = persistNegative(negativeRoot, positive, "c")
        val second = persistNegative(negativeRoot, positive, "d")
        val snapshot = snapshot(positive, listOf(first, second))
        val bindings = complete(snapshot, positiveRoot, negativeRoot).sourceBindings.bindings

        assertEquals(3, bindings.size)
        assertEquals(2, bindings.filterIsInstance<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Negative>().size)
        assertEquals(
            setOf(first.p1MaterializationId, second.p1MaterializationId),
            bindings.filterIsInstance<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingV1.Negative>()
                .map { it.p1MaterializationId }.toSet(),
        )
    }

    @Test
    fun everyFrozenMemberHasExactlyOneBinding() = withRoots { positiveRoot, negativeRoot ->
        val positive = Fixture.positive()
        persistPositive(positiveRoot, positive)
        val negative = persistNegative(negativeRoot, positive, "e")
        val snapshot = snapshot(positive, listOf(negative))
        val completed = complete(snapshot, positiveRoot, negativeRoot)

        completed.sourceBindings.validateAgainst(snapshot)
        assertEquals(snapshot.members.size, completed.sourceBindings.bindings.size)
        assertEquals(
            snapshot.members.map { it.membershipReference }.toSet(),
            completed.sourceBindings.bindings.map { it.membershipReference }.toSet(),
        )
    }

    @Test
    fun repeatedBindingIsDeterministic() = withRoots { positiveRoot, negativeRoot ->
        val positive = Fixture.positive()
        persistPositive(positiveRoot, positive)
        val negative = persistNegative(negativeRoot, positive, "f")
        val snapshot = snapshot(positive, listOf(negative))

        val first = complete(snapshot, positiveRoot, negativeRoot)
        val second = complete(snapshot, positiveRoot, negativeRoot)
        assertEquals(first, second)
    }

    @Test
    fun missingPositiveRecordFailsClosed() = withRoots { positiveRoot, negativeRoot ->
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Failed>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Request(
                    snapshot(positive = Fixture.positive()), positiveRoot, negativeRoot,
                ),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.FailureReasonV1.POSITIVE_RECORD_NOT_FOUND,
            failure.reason,
        )
    }

    @Test
    fun missingNegativeRecordFailsClosed() = withRoots { positiveRoot, negativeRoot ->
        val negative = persistNegative(negativeRoot, Fixture.positive(), "a")
        val snapshot = snapshot(negative = listOf(negative))
        negativeRoot.deleteRecursively()

        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Failed>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Request(
                    snapshot, positiveRoot, negativeRoot,
                ),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.FailureReasonV1.NEGATIVE_RECORD_NOT_FOUND,
            failure.reason,
        )
    }

    @Test
    fun positiveReferenceMismatchFailsClosed() = withRoots { positiveRoot, negativeRoot ->
        val original = Fixture.positive("original")
        val alternate = Fixture.positive("alternate")
        persistPositive(positiveRoot, original)
        persistPositive(positiveRoot, alternate)
        val base = snapshot(positive = original)
        val member = base.members.single()
        val broken = base.copy(
            members = listOf(
                member.copy(
                    auditBinding = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Positive(
                        alternate.exampleReference,
                    ),
                ),
            ),
        )

        val failure = failure(broken, positiveRoot, negativeRoot)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.FailureReasonV1.POSITIVE_BINDING_MISMATCH,
            failure.reason,
        )
    }

    @Test
    fun negativeReferenceMismatchFailsClosed() = withRoots { positiveRoot, negativeRoot ->
        val positive = Fixture.positive()
        val first = persistNegative(negativeRoot, positive, "a")
        val second = persistNegative(negativeRoot, positive, "b")
        val base = snapshot(negative = listOf(first))
        val alternateBinding = snapshot(negative = listOf(second)).members.single().auditBinding
        val broken = base.copy(members = listOf(base.members.single().copy(auditBinding = alternateBinding)))

        val failure = failure(broken, positiveRoot, negativeRoot)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.FailureReasonV1.NEGATIVE_BINDING_MISMATCH,
            failure.reason,
        )
    }

    @Test
    fun positiveModelInputMismatchFailsClosed() = withRoots { positiveRoot, negativeRoot ->
        val positive = Fixture.positive()
        persistPositive(positiveRoot, positive)
        val base = snapshot(positive = positive)
        val broken = base.copy(
            members = listOf(base.members.single().copy(modelInput = base.members.single().modelInput.copy(observedTerm = "changed"))),
        )

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.FailureReasonV1.MODEL_INPUT_MISMATCH,
            failure(broken, positiveRoot, negativeRoot).reason,
        )
    }

    @Test
    fun negativeModelInputMismatchFailsClosed() = withRoots { positiveRoot, negativeRoot ->
        val positive = Fixture.positive()
        val negative = persistNegative(negativeRoot, positive, "c")
        val base = snapshot(negative = listOf(negative))
        val broken = base.copy(
            members = listOf(base.members.single().copy(modelInput = base.members.single().modelInput.copy(observedTerm = "changed"))),
        )

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.FailureReasonV1.MODEL_INPUT_MISMATCH,
            failure(broken, positiveRoot, negativeRoot).reason,
        )
    }

    @Test
    fun positiveSupervisionMismatchFailsClosed() = withRoots { positiveRoot, negativeRoot ->
        val positive = Fixture.positive()
        persistPositive(positiveRoot, positive)
        val base = snapshot(positive = positive)
        val broken = base.copy(
            members = listOf(
                base.members.single().copy(
                    supervision = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Positive(
                        HimTrainingTargetV1.NewCanonical("different"),
                    ),
                ),
            ),
        )

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.FailureReasonV1.SUPERVISION_MISMATCH,
            failure(broken, positiveRoot, negativeRoot).reason,
        )
    }

    @Test
    fun negativeSupervisionMismatchFailsClosed() = withRoots { positiveRoot, negativeRoot ->
        val positive = Fixture.positive()
        val negative = persistNegative(negativeRoot, positive, "d")
        val base = snapshot(negative = listOf(negative))
        val broken = base.copy(
            members = listOf(
                base.members.single().copy(
                    supervision = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Negative(
                        HimTrainingTargetV1.NewCanonical("different"),
                        HimNegativeBoundaryTypeV1.WRONG_SCOPE,
                    ),
                ),
            ),
        )

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.FailureReasonV1.SUPERVISION_MISMATCH,
            failure(broken, positiveRoot, negativeRoot).reason,
        )
    }

    @Test
    fun negativeDigestMismatchFailsClosed() = withRoots { positiveRoot, negativeRoot ->
        val positive = Fixture.positive()
        val negative = persistNegative(negativeRoot, positive, "e")
        val base = snapshot(negative = listOf(negative))
        val original = base.members.single()
        val originalBinding = original.auditBinding as HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Negative
        val broken = base.copy(
            members = listOf(
                original.copy(
                    auditBinding = originalBinding.copy(negativeRecordLogicalDigest = HimSha256("a".repeat(64))),
                ),
            ),
        )

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.FailureReasonV1.DURABLE_RECORD_DIGEST_MISMATCH,
            failure(broken, positiveRoot, negativeRoot).reason,
        )
    }

    @Test
    fun invalidSnapshotFailsClosed() = withRoots { positiveRoot, negativeRoot ->
        val broken = snapshot(positive = Fixture.positive()).copy(contractId = "wrong")
        val failure = failure(broken, positiveRoot, negativeRoot)

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.FailureReasonV1.INVALID_CORPUS_SNAPSHOT,
            failure.reason,
        )
    }

    @Test
    fun sourceBindingsCannotBeAppliedToAnotherSnapshot() = withRoots { positiveRoot, negativeRoot ->
        val first = Fixture.positive("first")
        val second = Fixture.positive("second")
        persistPositive(positiveRoot, first)
        persistPositive(positiveRoot, second)
        val firstSnapshot = snapshot(positive = first)
        val secondSnapshot = snapshot(positive = second)
        val completed = complete(firstSnapshot, positiveRoot, negativeRoot)

        assertFailsWith<IllegalArgumentException> {
            completed.sourceBindings.validateAgainst(secondSnapshot)
        }
    }

    private fun complete(
        snapshot: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Record,
        positiveRoot: File,
        negativeRoot: File,
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Request(
                    snapshot, positiveRoot, negativeRoot,
                ),
            ),
        )

    private fun completedBinding(
        snapshot: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Record,
        positiveRoot: File,
        negativeRoot: File,
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SourceBindingSetV1 =
        complete(snapshot, positiveRoot, negativeRoot).sourceBindings

    private fun failure(
        snapshot: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Record,
        positiveRoot: File,
        negativeRoot: File,
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Failed =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Result.Failed>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.Request(
                    snapshot, positiveRoot, negativeRoot,
                ),
            ),
        )

    private fun snapshot(
        positive: HimTrainingExampleV1? = null,
        negative: List<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Record> = emptyList(),
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.Record {
        val assembly = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Result.Completed>(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = positive?.let {
                        listOf(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PositiveMember(it))
                    }.orEmpty(),
                    negativeMembers = negative.map {
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.NegativeMember(it)
                    },
                ),
            ),
        ).corpus
        return HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPersistenceV1.buildRecord(assembly)
    }

    private fun persistPositive(
        root: File,
        example: HimTrainingExampleV1,
    ): HimPositiveTrainingExamplePersistenceV1.PositiveTrainingExampleRecordV1 = when (
        val result = HimPositiveTrainingExamplePersistenceV1.execute(
            HimPositiveTrainingExamplePersistenceV1.Request(example, root),
        )
    ) {
        is HimPositiveTrainingExamplePersistenceV1.Result.Created -> result.record
        is HimPositiveTrainingExamplePersistenceV1.Result.AlreadyPresentIdentical -> result.record
        else -> error("positive persistence failed")
    }

    private fun persistNegative(
        root: File,
        positive: HimTrainingExampleV1,
        seed: String,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Record {
        val materialized = Fixture.materialized(positive, seed)
        return when (
            val result = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Request(materialized, root),
            )
        ) {
            is HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Result.Completed -> result.record
            else -> error("negative persistence failed")
        }
    }

    private fun withRoots(block: (File, File) -> Unit) {
        val root = Files.createTempDirectory("him-partition-source-binding-").toFile().canonicalFile
        val positiveRoot = File(root, "positive")
        val negativeRoot = File(root, "negative")
        try {
            block(positiveRoot, negativeRoot)
        } finally {
            root.deleteRecursively()
        }
    }

    private object Fixture {
        private val canonicalId = HimEntityId("AbCd12")
        private val evidenceReference = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("d".repeat(64)), "off:product:row:1")
        private val boundary = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY

        fun positive(seed: String = "base"): HimTrainingExampleV1 = HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = HimTrainingInputV1(
                observedTerm = "unknown food $seed",
                normalizedObservedTerm = "unknown food $seed",
                canonicalContext = listOf(HimCandidateCanonicalContext(1, canonicalId, "Canonical", null)),
                evidence = listOf(HimTrainingEvidenceInputV1(evidenceReference, "FOOD", 1)),
            ),
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(canonicalId)),
            provenance = HimTrainingProvenanceV1(sourceEvidenceReferences = listOf(evidenceReference)),
        )

        fun materialized(
            positive: HimTrainingExampleV1,
            seed: String,
        ): HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.Materialized {
            val recordId = seed.repeat(64)
            val negative = HimNegativeTrainingExampleV1.create(
                positive,
                HimTrainingTargetV1.NewCanonical("new canonical"),
                boundary,
            )
            val lineage = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.P1Lineage(
                negativeSupervisionRecordId = recordId,
                negativeCandidateId = "a".repeat(64),
                readinessDecisionId = "b".repeat(64),
                readinessState = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
                eligibilityBatchId = "eligibility-batch",
                eligibilityInputBindingDigest = "c".repeat(64),
                eligibilityDecisionId = "c".repeat(64),
                eligibilityState = HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE,
                validationBatchId = "validation-batch",
                validationBatchBindingDigest = "d".repeat(64),
                validationBatchLogicalDigest = "e".repeat(64),
                validationRecordId = "e".repeat(64),
                reviewUnitId = "review-unit",
                stableEntryId = "f".repeat(64),
                canonicalEntityId = canonicalId.value,
                originalReviewerRef = "reviewer:original",
                validatorReviewerRef = "reviewer:validator",
                validationRound = 1,
                validationRevision = 1,
                originalDecision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
                assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
                validationReasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION),
                evidenceReferenceIds = listOf("negative-evidence-1"),
                downstreamRoute = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE,
                candidateState = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE,
                rejectedTarget = negative.rejectedTarget,
                boundaryType = negative.boundaryType,
                occurrenceReference = "occurrence:v1:${"b".repeat(64)}",
                positiveTrainingExampleReference = positive.exampleReference,
                materializationDecisionId = "1".repeat(64),
                projectionInputBindingDigest = "2".repeat(64),
                proofBindingDigest = "3".repeat(64),
                proofLogicalDigest = "4".repeat(64),
            )
            val materializationId = "negative-materialization:v1:${sha256(buildString {
                appendLine("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_TRAINING_EXAMPLE_MATERIALIZATION_ID_V1")
                appendLine("negativeSupervisionRecordId=$recordId")
                appendLine("negativeExampleReference=${negative.reference.value}")
            })}"
            return HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.Materialized(
                materializationId = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1(materializationId),
                negativeTrainingExample = negative,
                p1Lineage = lineage,
                evidenceBindings = listOf(
                    HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding(
                        "negative-evidence-1", HimTrainingEvidenceInputV1(evidenceReference, "FOOD", 1),
                    ),
                ),
            )
        }

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
