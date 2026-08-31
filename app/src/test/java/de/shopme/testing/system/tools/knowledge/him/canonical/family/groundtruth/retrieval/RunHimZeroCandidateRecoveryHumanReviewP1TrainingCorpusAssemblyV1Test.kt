package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
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
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1Test {
    @Test
    fun validPositiveMemberCompletes() {
        val corpus = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(positiveMember()),
                ),
            ),
        )

        assertEquals(1, corpus.members.size)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.POSITIVE,
            corpus.members.single().polarity,
        )
    }

    @Test
    fun positiveMemberPreservesExactTrainingInput() {
        val example = Fixture.positive()
        val member = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PositiveMember(example),
                    ),
                ),
            ),
        ).members.single()

        assertEquals(example.input, member.modelInput)
    }

    @Test
    fun positiveTargetIsSupervisionAndNotModelInput() {
        val example = Fixture.positive()
        val member = completed(assemble(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(positiveMembers = listOf(positiveMember())))).members.single()
        val supervision = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Positive>(member.supervision)

        assertEquals(example.target, supervision.target)
        assertTrue(member.modelInput.toString().contains(example.input.observedTerm))
        assertTrue(!member.modelInput.toString().contains(example.target.toString()))
    }

    @Test
    fun positiveMembershipIdentityIsDeterministicAndBoundToReference() {
        val first = completed(assemble(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(positiveMembers = listOf(positiveMember())))).members.single()
        val second = completed(assemble(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(positiveMembers = listOf(positiveMember())))).members.single()

        assertEquals(first.membershipReference, second.membershipReference)
        assertEquals(first.auditBinding, second.auditBinding)
    }

    @Test
    fun duplicatePositiveMembershipFailsClosed() {
        val result = assemble(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                positiveMembers = listOf(positiveMember(), positiveMember()),
            ),
        )

        assertFailure(
            result,
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.FailureReasonV1.DUPLICATE_MEMBERSHIP,
        )
    }

    @Test
    fun validNegativeMemberCompletes() {
        val corpus = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    negativeMembers = listOf(negativeMember()),
                ),
            ),
        )

        assertEquals(1, corpus.counters.total)
        assertEquals(1, corpus.counters.negative)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.NEGATIVE,
            corpus.members.single().polarity,
        )
    }

    @Test
    fun negativeMemberUsesEmbeddedPositiveModelInput() {
        val record = Fixture.negativeRecord()
        val member = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    negativeMembers = listOf(
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.NegativeMember(record),
                    ),
                ),
            ),
        ).members.single()

        assertEquals(record.genericNegativeExample.positiveExample.input, member.modelInput)
    }

    @Test
    fun rejectedTargetIsExplicitNegativeSupervision() {
        val record = Fixture.negativeRecord()
        val supervision = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    negativeMembers = listOf(
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.NegativeMember(record),
                    ),
                ),
            ),
        ).members.single().supervision

        val negative = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.SupervisionV1.Negative>(supervision)
        assertEquals(record.genericNegativeExample.rejectedTarget, negative.rejectedTarget)
        assertEquals(record.genericNegativeExample.boundaryType, negative.boundaryType)
    }

    @Test
    fun negativeMembershipIdentityUsesP1MaterializationIdentity() {
        val first = completed(assemble(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(negativeMembers = listOf(negativeMember('f'))))).members.single()
        val second = completed(assemble(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(negativeMembers = listOf(negativeMember('e'))))).members.single()

        assertNotEquals(first.membershipReference, second.membershipReference)
        val firstBinding = assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Negative>(first.auditBinding)
        assertTrue(firstBinding.p1MaterializationId.value.contains("negative-materialization:v1:"))
        assertNotEquals(firstBinding.p1MaterializationId, assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Negative>(second.auditBinding).p1MaterializationId)
    }

    @Test
    fun duplicateNegativeMembershipFailsClosed() {
        val record = Fixture.negativeRecord()
        val result = assemble(
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                negativeMembers = listOf(
                    HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.NegativeMember(record),
                    HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.NegativeMember(record),
                ),
            ),
        )

        assertFailure(
            result,
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.FailureReasonV1.DUPLICATE_MEMBERSHIP,
        )
    }

    @Test
    fun positiveAndNegativeMembersCompleteTogether() {
        val corpus = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(positiveMember()),
                    negativeMembers = listOf(negativeMember()),
                ),
            ),
        )

        assertEquals(2, corpus.counters.total)
        assertEquals(1, corpus.counters.positive)
        assertEquals(1, corpus.counters.negative)
    }

    @Test
    fun multipleNegativesMayShareOnePositiveAndGenericContent() {
        val corpus = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(positiveMember()),
                    negativeMembers = listOf(negativeMember('f'), negativeMember('e')),
                ),
            ),
        )

        assertEquals(3, corpus.members.size)
        assertEquals(1, corpus.members.count { it.polarity == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.POSITIVE })
        assertEquals(2, corpus.members.count { it.polarity == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.NEGATIVE })
        val negativeBindings = corpus.members
            .filter { it.polarity == HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.NEGATIVE }
            .map { assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.AuditBindingV1.Negative>(it.auditBinding).positiveExampleReference }
        assertEquals(listOf(Fixture.positive().exampleReference, Fixture.positive().exampleReference), negativeBindings)
    }

    @Test
    fun inputOrderDoesNotChangeMembersCountersOrDigest() {
        val positive = positiveMember()
        val firstNegative = negativeMember('f')
        val secondNegative = negativeMember('e')
        val first = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(positive),
                    negativeMembers = listOf(firstNegative, secondNegative),
                ),
            ),
        )
        val second = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(positive),
                    negativeMembers = listOf(secondNegative, firstNegative),
                ),
            ),
        )

        assertEquals(first, second)
        assertEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun invalidPositiveMemberFailsClosed() {
        val invalid = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PositiveMember(
            example = Fixture.positive(),
            modelInput = Fixture.alteredInput(),
        )

        assertFailure(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(invalid),
                ),
            ),
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.FailureReasonV1.INVALID_POSITIVE_MEMBER,
        )
    }

    @Test
    fun invalidNegativeMemberFailsClosed() {
        val invalid = Fixture.negativeRecord().copy(recordLogicalDigest = "0".repeat(64))

        assertFailure(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    negativeMembers = listOf(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.NegativeMember(invalid)),
                ),
            ),
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.FailureReasonV1.INVALID_NEGATIVE_MEMBER,
        )
    }

    @Test
    fun positiveBindingMismatchFailsClosed() {
        val wrongReference = HimTrainingExampleReference("example:v1:${"0".repeat(64)}")

        assertFailure(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PositiveMember(
                            Fixture.positive(),
                            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PositiveSourceBinding(wrongReference),
                        ),
                    ),
                ),
            ),
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.FailureReasonV1.POSITIVE_BINDING_MISMATCH,
        )
    }

    @Test
    fun contradictoryPositiveSupervisionFailsClosed() {
        val first = Fixture.positive()
        val contradictory = HimTrainingExampleV1.create(
            taskType = first.taskType,
            input = first.input,
            target = HimTrainingTargetV1.NewCanonical("different accepted answer"),
            provenance = first.provenance,
        )

        assertFailure(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PositiveMember(first),
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PositiveMember(contradictory),
                    ),
                ),
            ),
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.FailureReasonV1.CONTRADICTORY_SUPERVISION,
        )
    }

    @Test
    fun emptyRequestCompletesAsEmptyButNotTrainingReady() {
        val first = completed(assemble(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request()))
        val second = completed(assemble(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request()))

        assertEquals(0, first.counters.total)
        assertEquals(0, first.counters.positive)
        assertEquals(0, first.counters.negative)
        assertEquals(first, second)
    }

    @Test
    fun countersAreExactlyDerivedFromMembers() {
        val counters = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(positiveMember()),
                    negativeMembers = listOf(negativeMember('f'), negativeMember('e')),
                ),
            ),
        ).counters

        assertEquals(3, counters.total)
        assertEquals(1, counters.positive)
        assertEquals(2, counters.negative)
    }

    @Test
    fun outputUsesExplicitPolarityOrdering() {
        val corpus = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    negativeMembers = listOf(negativeMember('f')),
                    positiveMembers = listOf(positiveMember()),
                ),
            ),
        )

        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.POSITIVE,
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PolarityV1.NEGATIVE,
            ),
            corpus.members.map { it.polarity },
        )
    }

    @Test
    fun digestIncludesSupervisionAndAuditBindingButNotFilesystemState() {
        val positive = positiveMember()
        val first = completed(assemble(HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(positiveMembers = listOf(positive))))
        val changed = completed(
            assemble(
                HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request(
                    positiveMembers = listOf(
                        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PositiveMember(
                            Fixture.positive(target = HimTrainingTargetV1.NewCanonical("new answer")),
                        ),
                    ),
                ),
            ),
        )

        assertNotEquals(first.logicalDigest, changed.logicalDigest)
        assertTrue(first.logicalDigest.value.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun publicAssemblySurfaceHasNoStoreOrPartitionOperation() {
        val names = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1::class.java.declaredMethods.map { it.name }

        assertTrue(names.none { name -> name.contains("read", ignoreCase = true) || name.contains("partition", ignoreCase = true) })
        assertTrue(names.none { name -> name.contains("write", ignoreCase = true) || name.contains("scan", ignoreCase = true) })
    }

    private fun assemble(
        request: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Request,
    ) = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.assemble(request)

    private fun completed(
        result: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Result,
    ): HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.CorpusV1 =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Result.Completed>(result).corpus

    private fun assertFailure(
        result: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Result,
        expected: HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.FailureReasonV1,
    ) {
        assertEquals(expected, assertIs<HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.Result.Failed>(result).reason)
    }

    private fun positiveMember() = HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.PositiveMember(Fixture.positive())

    private fun negativeMember(seed: Char = 'f') =
        HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusAssemblyV1.NegativeMember(Fixture.negativeRecord(seed))

    private object Fixture {
        private val canonicalId = HimEntityId("Abc123")
        private val evidenceReference = HimEvidenceReference(
            source = HimGroundTruthSource.OPEN_FOOD_FACTS.name,
            sourceArtifactSha256 = HimSha256("a".repeat(64)),
            sourceRecordIdentity = "off:product:fixture:123",
        )
        private val input = HimTrainingInputV1(
            observedTerm = "fixture food",
            normalizedObservedTerm = "fixture food",
            canonicalContext = listOf(HimCandidateCanonicalContext(1, canonicalId, "Fixture Food", null)),
            evidence = listOf(HimTrainingEvidenceInputV1(evidenceReference, "FOOD", 1)),
        )

        fun positive(target: HimTrainingTargetV1 = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(canonicalId))) =
            HimTrainingExampleV1.create(
                taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
                input = input,
                target = target,
                provenance = HimTrainingProvenanceV1(sourceEvidenceReferences = listOf(evidenceReference)),
            )

        fun alteredInput() = input.copy(observedTerm = "different fixture food")

        fun negativeRecord(seed: Char = 'f'): HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Record {
            val positive = positive()
            val rejectedTarget = HimTrainingTargetV1.NewCanonical("rejected fixture")
            val negative = HimNegativeTrainingExampleV1.create(
                positiveExample = positive,
                rejectedTarget = rejectedTarget,
                boundaryType = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
            )
            val recordId = seed.toString().repeat(64)
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
                rejectedTarget = rejectedTarget,
                boundaryType = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
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
            val unsigned = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Record(
                contractId = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.CONTRACT_ID,
                version = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.VERSION,
                state = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.STATE,
                p1MaterializationId = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1(materializationId),
                genericNegativeExample = negative,
                p1Lineage = lineage,
                evidenceBindings = listOf(
                    HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding(
                        "negative-evidence-1",
                        HimTrainingEvidenceInputV1(evidenceReference, "FOOD", 1),
                    ),
                ),
                recordLogicalDigest = "",
            )
            return unsigned.copy(
                recordLogicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.recordLogicalDigest(unsigned),
            )
        }

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
