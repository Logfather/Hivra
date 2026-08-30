package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.lang.reflect.Modifier
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1 as Persistence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1 as Contract
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1Test {
    @Test
    fun contractIdentityAndTrainingNeutralStateAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_SUPERVISION_TO_TRAINING_BINDING_CONTEXT_CONTRACT_V1",
            Contract.CONTRACT_ID,
        )
        assertEquals("1", Contract.VERSION)
        assertEquals("NEGATIVE_SUPERVISION_TRAINING_BINDING_CONTEXT_ONLY", Contract.STATE)
    }

    @Test
    fun completeExplicitBindingIsReady() {
        val decision = completed(fixture().request).value.decisions.single()

        assertEquals(Contract.BindingState.READY_FOR_NEGATIVE_TRAINING_EXAMPLE_ADAPTER, decision.state)
        assertTrue(decision.reasons.isEmpty())
        assertNotNull(decision.positiveExampleReference)
        assertNotNull(decision.rejectedTarget)
        assertEquals(HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION, decision.boundaryType)
        assertEquals(2, decision.evidenceBindings.size)
        assertNotNull(decision.trainingProvenance)
    }

    @Test
    fun missingPositiveExampleIsNotYetBindable() {
        val result = completed(fixture().request.copy(bindings = listOf(fixture().binding(positiveTrainingExample = null))))

        assertNotYetBindable(result, Contract.FailureReason.MISSING_POSITIVE_TRAINING_EXAMPLE_BINDING)
    }

    @Test
    fun trainingInputIsOwnedByTheExplicitPositiveExample() {
        val result = completed(fixture().request)
        val decision = result.value.decisions.single()

        assertEquals(fixture().positive.input, fixture().positive.modelInput())
        assertEquals(Contract.BindingState.READY_FOR_NEGATIVE_TRAINING_EXAMPLE_ADAPTER, decision.state)
        assertTrue(decision.reasons.isEmpty())
    }

    @Test
    fun missingRejectedTargetIsNotYetBindable() {
        val result = completed(fixture().request.copy(bindings = listOf(fixture().binding(rejectedTarget = null))))

        assertNotYetBindable(result, Contract.FailureReason.MISSING_REJECTED_TARGET_BINDING)
    }

    @Test
    fun missingBoundaryTypeIsNotYetBindable() {
        val result = completed(fixture().request.copy(bindings = listOf(fixture().binding(boundaryType = null))))

        assertNotYetBindable(result, Contract.FailureReason.MISSING_NEGATIVE_BOUNDARY_TYPE_BINDING)
    }

    @Test
    fun missingEvidenceBindingIsNotYetBindable() {
        val result = completed(fixture().request.copy(bindings = listOf(fixture().binding(evidenceBindings = emptyList()))))

        assertNotYetBindable(result, Contract.FailureReason.MISSING_REQUIRED_EVIDENCE_BINDING)
    }

    @Test
    fun missingTrainingProvenanceIsNotYetBindable() {
        val result = completed(fixture().request.copy(bindings = listOf(fixture().binding(trainingProvenance = null))))

        assertNotYetBindable(result, Contract.FailureReason.MISSING_TRAINING_PROVENANCE_BINDING)
    }

    @Test
    fun canonicalTargetMismatchFailsClosed() {
        val binding = fixture().binding(
            rejectedTarget = HimTrainingTargetV1.Identity(HimEntityId("C00202")),
        )
        val decision = completed(fixture().request.copy(bindings = listOf(binding))).value.decisions.single()

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, decision.state)
        assertTrue(Contract.FailureReason.CANONICAL_TARGET_MISMATCH in decision.reasons)
    }

    @Test
    fun evidenceReferenceMismatchFailsClosed() {
        val binding = fixture().binding(
            evidenceBindings = fixture().evidenceBindings.mapIndexed { index, evidence ->
                if (index == 0) evidence.copy(persistedEvidenceReferenceId = "foreign-evidence") else evidence
            },
        )
        val decision = completed(fixture().request.copy(bindings = listOf(binding))).value.decisions.single()

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, decision.state)
        assertTrue(Contract.FailureReason.EVIDENCE_BINDING_MISMATCH in decision.reasons)
    }

    @Test
    fun positiveExampleMismatchFailsClosed() {
        val other = HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = HimTrainingInputV1(
                observedTerm = "Other",
                normalizedObservedTerm = "other",
                canonicalContext = listOf(
                    HimCandidateCanonicalContext(1, HimEntityId("C00202"), "Other", null),
                ),
                evidence = fixture().positive.input.evidence,
            ),
            target = HimTrainingTargetV1.ExistingCanonical(HimEntityId("C00202")),
            provenance = fixture().positive.provenance,
        )
        val decision = completed(
            fixture().request.copy(bindings = listOf(fixture().binding(positiveTrainingExample = other))),
        ).value.decisions.single()

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, decision.state)
        assertTrue(Contract.FailureReason.POSITIVE_EXAMPLE_MISMATCH in decision.reasons)
    }

    @Test
    fun bindingFromAnotherRecordFailsClosed() {
        val first = fixture()
        val second = fixture(2)
        val foreignBinding = first.binding(record = second.record)
        val decision = completed(first.request.copy(bindings = listOf(foreignBinding))).value.decisions.single()

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, decision.state)
        assertTrue(Contract.FailureReason.EVIDENCE_BINDING_MISMATCH in decision.reasons)
    }

    @Test
    fun duplicateRecordBindingFailsClosed() {
        val fixture = fixture()
        val result = Contract.evaluate(Contract.Request(listOf(fixture.defaultBinding, fixture.defaultBinding)))
        val failure = assertIs<Contract.Result.Failed>(result)

        assertEquals(Contract.FailureReason.DUPLICATE_RECORD_BINDING, failure.reason)
    }

    @Test
    fun contradictoryTrainingProvenanceFailsClosed() {
        val result = completed(
            fixture().request.copy(
                bindings = listOf(fixture().binding(trainingProvenance = HimTrainingProvenanceV1())),
            ),
        )
        val decision = result.value.decisions.single()

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, decision.state)
        assertTrue(Contract.FailureReason.CONTRADICTORY_TRAINING_PROVENANCE in decision.reasons)
    }

    @Test
    fun unsupportedBoundarySemanticsFailsClosed() {
        val fixture = fixture()
        val decision = completed(
            fixture.request.copy(
                bindings = listOf(
                    fixture.binding(
                        rejectedTarget = HimTrainingTargetV1.ExistingCanonical(CANONICAL),
                        boundaryType = HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION,
                    ),
                ),
            ),
        ).value.decisions.single()

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, decision.state)
        assertTrue(Contract.FailureReason.UNSUPPORTED_BOUNDARY_SEMANTICS in decision.reasons)
    }

    @Test
    fun malformedPersistedRecordIsNotYetBindable() {
        val fixture = fixture()
        val decision = completed(
            fixture.request.copy(bindings = listOf(fixture.binding(record = fixture.record.copy(canonicalEntityId = "")))),
        ).value.decisions.single()

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, decision.state)
        assertTrue(Contract.FailureReason.INVALID_PERSISTED_RECORD in decision.reasons)
    }

    @Test
    fun extraEvidenceBindingFailsClosed() {
        val fixture = fixture()
        val extra = Contract.EvidenceBinding(
            persistedEvidenceReferenceId = "extra-evidence",
            trainingEvidence = evidence(fixture.index, 3).trainingEvidence,
        )
        val decision = completed(
            fixture.request.copy(
                bindings = listOf(fixture.binding(evidenceBindings = fixture.evidenceBindings + extra)),
            ),
        ).value.decisions.single()

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, decision.state)
        assertTrue(Contract.FailureReason.EVIDENCE_BINDING_MISMATCH in decision.reasons)
    }

    @Test
    fun deterministicRepeatProducesIdenticalBatchAndDigest() {
        val fixture = fixture()
        val first = completed(fixture.request).value
        val second = completed(fixture.request).value

        assertEquals(first, second)
        assertEquals(Contract.batchLogicalDigest(first.copy(logicalDigest = "")), first.logicalDigest)
    }

    @Test
    fun recordInputOrderIsPreservedInOutput() {
        val first = fixture(1)
        val second = fixture(2)
        val result = completed(
            Contract.Request(listOf(second.defaultBinding, first.defaultBinding)),
        ).value

        assertEquals(listOf(second.record.recordId, first.record.recordId), result.decisions.map { it.recordId })
    }

    @Test
    fun evidenceOutputIsCanonicalAndIndependentOfBindingInputOrder() {
        val fixture = fixture()
        val reversed = fixture.binding(evidenceBindings = fixture.evidenceBindings.reversed())
        val normal = completed(fixture.request).value
        val reordered = completed(fixture.request.copy(bindings = listOf(reversed))).value

        assertEquals(normal, reordered)
    }

    @Test
    fun noNegativeTrainingExampleOrPartitionIsExposedByOutput() {
        val batch = completed(fixture().request).value
        val fieldNames = batch::class.java.declaredFields.map { it.name }.toSet()

        assertFalse(fieldNames.contains("negativeTrainingExample"))
        assertFalse(fieldNames.contains("partition"))
        assertFalse(fieldNames.contains("manifest"))
    }

    @Test
    fun emptyRequestFailsClosedWithoutDiscoveringBindings() {
        val result = Contract.evaluate(Contract.Request(emptyList()))
        val failure = assertIs<Contract.Result.Failed>(result)

        assertEquals(Contract.FailureReason.INVALID_BINDING_CONTEXT, failure.reason)
    }

    @Test
    fun countersAreDerivedFromTypedBindingStates() {
        val base = fixture()
        val second = fixture(2)
        val result = completed(
            Contract.Request(
                listOf(
                    base.defaultBinding,
                    base.binding(positiveTrainingExample = null, record = second.record),
                ),
            ),
        ).value

        assertEquals(2, result.counters.totalRecords)
        assertEquals(1, result.counters.readyForNegativeTrainingExampleAdapter)
        assertEquals(1, result.counters.notYetBindable)
    }

    @Test
    fun sourceAndFilesystemAreNotPartOfTheBindingApi() {
        val publicMethods =
            Contract::class.java.declaredMethods
                .filter { Modifier.isPublic(it.modifiers) }
        val names = publicMethods.map { it.name }.toSet()

        assertFalse(names.any { it.contains("search", ignoreCase = true) })
        assertFalse(names.any { it.contains("fetch", ignoreCase = true) })
        assertFalse(names.any { it.contains("write", ignoreCase = true) })
        assertFalse(names.any { it.contains("persist", ignoreCase = true) })

        val forbiddenTypes = setOf(
            java.io.File::class.java,
            java.nio.file.Path::class.java,
            java.nio.file.Files::class.java,
        )
        publicMethods.forEach { method ->
            assertTrue(method.returnType !in forbiddenTypes)
            assertTrue(method.parameterTypes.none { it in forbiddenTypes })
        }
    }

    private fun completed(request: Contract.Request): Contract.Result.Completed =
        assertIs<Contract.Result.Completed>(Contract.evaluate(request))

    private fun assertNotYetBindable(
        result: Contract.Result.Completed,
        reason: Contract.FailureReason,
    ) {
        val decision = result.value.decisions.single()
        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, decision.state)
        assertTrue(reason in decision.reasons)
    }

    private data class Fixture(
        val index: Int,
        val record: Persistence.Record,
        val positive: HimTrainingExampleV1,
        val evidenceBindings: List<Contract.EvidenceBinding>,
    ) {
        val defaultBinding: Contract.RecordBinding
            get() = binding()

        val request: Contract.Request
            get() = Contract.Request(listOf(defaultBinding))

        fun binding(
            record: Persistence.Record = this.record,
            positiveTrainingExample: HimTrainingExampleV1? = positive,
            rejectedTarget: HimTrainingTargetV1? = defaultRejectedTarget(),
            boundaryType: HimNegativeBoundaryTypeV1? = HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION,
            evidenceBindings: List<Contract.EvidenceBinding> = this.evidenceBindings,
            trainingProvenance: HimTrainingProvenanceV1? = positive.provenance,
        ) = Contract.RecordBinding(
            record = record,
            positiveTrainingExample = positiveTrainingExample,
            rejectedTarget = rejectedTarget,
            boundaryType = boundaryType,
            evidenceBindings = evidenceBindings,
            trainingProvenance = trainingProvenance,
        )

        private fun defaultRejectedTarget() = HimTrainingTargetV1.Identity(HimEntityId(record.canonicalEntityId))
    }

    private fun fixture(index: Int = 1): Fixture {
        val canonical = CANONICAL
        val first = evidence(index, 1)
        val second = evidence(index, 2)
        val positive = HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = HimTrainingInputV1(
                observedTerm = "Artichoke $index",
                normalizedObservedTerm = "artichoke-$index",
                canonicalContext = listOf(HimCandidateCanonicalContext(1, canonical, "Artichoke", null)),
                evidence = listOf(first.trainingEvidence, second.trainingEvidence),
            ),
            target = HimTrainingTargetV1.ExistingCanonical(canonical),
            provenance = HimTrainingProvenanceV1(
                sourceEvidenceReferences = listOf(
                    first.trainingEvidence.reference,
                    second.trainingEvidence.reference,
                ),
            ),
        )
        return Fixture(
            index = index,
            record = record(index, canonical, listOf(first.persistedEvidenceReferenceId, second.persistedEvidenceReferenceId)),
            positive = positive,
            evidenceBindings = listOf(first, second),
        )
    }

    private fun evidence(index: Int, rank: Int): Contract.EvidenceBinding {
        val reference = HimEvidenceReference(
            source = "OPEN_FOOD_FACTS",
            sourceArtifactSha256 = HimSha256("b".repeat(64)),
            sourceRecordIdentity = "off:fixture:$index:$rank",
        )
        return Contract.EvidenceBinding(
            persistedEvidenceReferenceId = "persisted-evidence-$index-$rank",
            trainingEvidence = HimTrainingEvidenceInputV1(reference, "FOOD", rank),
        )
    }

    private fun record(index: Int, canonical: HimEntityId, evidence: List<String>): Persistence.Record {
        val reason = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
            .DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION
        return Persistence.Record(
            recordId = digest(index),
            negativeCandidateId = digest(100 + index),
            readinessDecisionId = digest(200 + index),
            readinessState = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
            eligibilityBatchId = "fixture-batch",
            eligibilityInputBindingDigest = digest(300 + index),
            eligibilityDecisionId = digest(400 + index),
            eligibilityState = HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE,
            validationBatchId = "fixture-validation-batch",
            validationBatchBindingDigest = digest(500 + index),
            validationBatchLogicalDigest = digest(600 + index),
            validationRecordId = digest(700 + index),
            reviewUnitId = "unit-$index",
            stableEntryId = "entry-$index",
            canonicalEntityId = canonical.value,
            originalReviewerRef = "reviewer:fixture:v1",
            validatorReviewerRef = "validator:fixture:v1",
            validationRound = 1,
            validationRevision = 1,
            originalDecision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
            validationReasonCodes = listOf(reason),
            evidenceReferenceIds = evidence,
            downstreamRoute = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE,
            candidateState = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE,
        )
    }

    private fun digest(value: Int): String = value.toString().padStart(64, '0')

    private val CANONICAL = HimEntityId("C00101")
}
