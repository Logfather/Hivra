package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1 as Contract
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleReferenceBindingContractV1Test {
    @Test
    fun validProjectedMaterializationWithMatchingP1SubjectBinds() {
        val fixture = Fixture.valid()
        val batch = completed(fixture)
        val decision = batch.decisions.single()

        assertEquals(Contract.BindingState.BOUND, decision.state)
        assertEquals(fixture.record.recordId, decision.bindingKey)
        assertEquals(fixture.example.exampleReference, decision.trainingExampleReference)
        assertEquals(fixture.materialization.decisionId, decision.materializationDecisionId)
        assertTrue(decision.reasons.isEmpty())
    }

    @Test
    fun referenceIsRecomputedFromProjectedExample() {
        val fixture = Fixture.valid()
        val expected = HimTrainingExampleIdentityV1.example(
            taskType = fixture.example.taskType,
            input = fixture.example.input,
            target = fixture.example.target,
            provenance = fixture.example.provenance,
        )

        assertEquals(expected, fixture.materialization.trainingExampleReference)
        assertEquals(expected, completed(fixture).decisions.single().trainingExampleReference)
    }

    @Test
    fun repeatedEvaluationIsDeterministic() {
        val fixture = Fixture.valid()

        assertEquals(
            Contract.evaluate(fixture.request()),
            Contract.evaluate(fixture.request()),
        )
    }

    @Test
    fun bindingDecisionIdIsDeterministic() {
        val fixture = Fixture.valid()
        val first = completed(fixture).decisions.single().bindingDecisionId
        val second = completed(fixture).decisions.single().bindingDecisionId

        assertEquals(first, second)
        assertEquals(64, first.length)
    }

    @Test
    fun batchLogicalDigestIsDeterministic() {
        val fixture = Fixture.valid()
        val first = completed(fixture).logicalDigest
        val second = completed(fixture).logicalDigest

        assertEquals(first, second)
        assertEquals(64, first.length)
    }

    @Test
    fun callerOrderIsPreserved() {
        val first = Fixture.valid(1)
        val second = Fixture.valid(2)
        val result = Contract.evaluate(
            Contract.Request(
                listOf(first.bindingRequest(), second.bindingRequest()),
            ),
        )
        val batch = assertIs<Contract.Result.Completed>(result).batch

        assertEquals(listOf(first.record.recordId, second.record.recordId), batch.decisions.map { it.bindingKey })
    }

    @Test
    fun invalidNegativeSupervisionRecordIsNotBindable() {
        val fixture = Fixture.valid()
        val broken = fixture.record.copy(canonicalEntityId = "")
        val result = Contract.evaluate(fixture.request(record = broken))

        assertEquals(Contract.Result.Failed(Contract.FailureReason.INVALID_NEGATIVE_SUPERVISION_RECORD), result)
    }

    @Test
    fun materializationNotYetProjectableIsNotBindable() {
        val fixture = Fixture.valid()
        val notProjected = fixture.materialization.copy(
            state = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationState.NOT_YET_PROJECTABLE,
            reasons = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.FailureReason.MISSING_CANONICAL_CONTEXT),
            projectedTrainingExample = null,
            trainingExampleReference = null,
        )
        val result = Contract.evaluate(fixture.request(notProjected))
        val binding = assertIs<Contract.Result.Completed>(result).batch.decisions.single()

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, binding.state)
        assertTrue(Contract.FailureReason.MATERIALIZATION_NOT_PROJECTED in binding.reasons)
    }

    @Test
    fun absentMaterializationIsNotBindableWithoutProjectedExample() {
        val fixture = Fixture.valid()
        val binding = decision(fixture, null)

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, binding.state)
        assertTrue(Contract.FailureReason.MATERIALIZATION_NOT_PROJECTED in binding.reasons)
        assertEquals(null, binding.trainingExampleReference)
    }

    @Test
    fun absentMaterializationIsNotBindableWithoutReference() {
        val fixture = Fixture.valid()
        val binding = decision(fixture, null)

        assertEquals(null, binding.materializationDecisionId)
        assertEquals(null, binding.trainingExampleReference)
        assertTrue(binding.reasons.isNotEmpty())
    }

    @Test
    fun materializationModelRejectsMismatchedProjectedReference() {
        val fixture = Fixture.valid()

        assertFailsWith<IllegalArgumentException> {
            fixture.materialization.copy(
                trainingExampleReference = Fixture.valid(99).example.exampleReference,
            )
        }
    }

    @Test
    fun callerSuppliedP1BindingKeyMustMatchRecordId() {
        val fixture = Fixture.valid()
        val binding = decision(fixture, fixture.materialization, bindingKey = "wrong-key")

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, binding.state)
        assertTrue(Contract.FailureReason.P1_BINDING_IDENTITY_MISMATCH in binding.reasons)
    }

    @Test
    fun canonicalLineageMismatchFailsClosed() {
        val fixture = Fixture.valid()
        val otherCanonical = Fixture.valid(2, HimEntityId("C00999")).example
        val materialization = fixture.materialization.copy(
            projectedTrainingExample = otherCanonical,
            trainingExampleReference = otherCanonical.exampleReference,
        )
        val binding = decision(fixture, materialization)

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, binding.state)
        assertTrue(Contract.FailureReason.CANONICAL_LINEAGE_MISMATCH in binding.reasons)
    }

    @Test
    fun materializationOccurrenceLineageMismatchFailsClosed() {
        val fixture = Fixture.valid()
        val materialization = fixture.materialization.copy(
            preMaterializationIdentity = Fixture.valid(999).materialization.occurrenceReference,
        )
        val binding = decision(fixture, materialization)

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, binding.state)
        assertTrue(Contract.FailureReason.MATERIALIZATION_LINEAGE_MISMATCH in binding.reasons)
    }

    @Test
    fun unsupportedNewCanonicalTargetIsNotBound() {
        val fixture = Fixture.valid()
        val newCanonical = HimTrainingExampleV1.create(
            taskType = fixture.example.taskType,
            input = fixture.example.input,
            target = HimTrainingTargetV1.NewCanonical("New food"),
            provenance = fixture.example.provenance,
        )
        val materialization = fixture.materialization.copy(
            projectedTrainingExample = newCanonical,
            trainingExampleReference = newCanonical.exampleReference,
        )
        val binding = decision(fixture, materialization)

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, binding.state)
        assertTrue(Contract.FailureReason.UNSUPPORTED_LINEAGE_RELATION in binding.reasons)
    }

    @Test
    fun duplicateSameKeyAndReferenceFailsClosed() {
        val fixture = Fixture.valid()
        val result = Contract.evaluate(
            Contract.Request(listOf(fixture.bindingRequest(), fixture.bindingRequest())),
        )

        assertEquals(Contract.Result.Failed(Contract.FailureReason.DUPLICATE_BINDING), result)
    }

    @Test
    fun duplicateSameKeyAndDifferentReferenceIsAmbiguous() {
        val first = Fixture.valid()
        val second = Fixture.valid(2)
        val result = Contract.evaluate(
            Contract.Request(
                listOf(
                    first.bindingRequest(bindingKey = first.record.recordId),
                    second.bindingRequest(bindingKey = first.record.recordId),
                ),
            ),
        )

        assertEquals(Contract.Result.Failed(Contract.FailureReason.AMBIGUOUS_BINDING), result)
    }

    @Test
    fun differentP1KeysMayShareContentAddressedReference() {
        val first = Fixture.valid()
        val secondRecord = Fixture.valid(2).record.copy(canonicalEntityId = first.record.canonicalEntityId)
        val second = first.bindingRequest(record = secondRecord)
        val batch = assertIs<Contract.Result.Completed>(
            Contract.evaluate(Contract.Request(listOf(first.bindingRequest(), second))),
        ).batch

        assertEquals(2, batch.counters.bound)
        assertEquals(1, batch.decisions.map { it.trainingExampleReference }.distinct().size)
    }

    @Test
    fun canonicalEntityIdAloneDoesNotDefineBindingIdentity() {
        val first = Fixture.valid(1)
        val second = Fixture.valid(2, HimEntityId("C00101"))
        val batch = assertIs<Contract.Result.Completed>(
            Contract.evaluate(Contract.Request(listOf(first.bindingRequest(), second.bindingRequest()))),
        ).batch

        assertNotEquals(first.record.recordId, second.record.recordId)
        assertEquals(2, batch.decisions.map { it.bindingKey }.distinct().size)
    }

    @Test
    fun textIsNotUsedAsBindingHeuristic() {
        val first = Fixture.valid(1)
        val second = Fixture.valid(2, first.canonical)
        val batch = assertIs<Contract.Result.Completed>(
            Contract.evaluate(Contract.Request(listOf(first.bindingRequest(), second.bindingRequest()))),
        ).batch

        assertEquals(2, batch.counters.bound)
        assertNotEquals(first.record.recordId, second.record.recordId)
    }

    @Test
    fun evidenceOverlapIsNotUsedAsIdentityHeuristic() {
        val first = Fixture.valid(1)
        val second = Fixture.valid(2, first.canonical, evidenceSuffix = "different")
        val batch = assertIs<Contract.Result.Completed>(
            Contract.evaluate(Contract.Request(listOf(first.bindingRequest(), second.bindingRequest()))),
        ).batch

        assertEquals(2, batch.counters.bound)
    }

    @Test
    fun emptyRequestFailsClosed() {
        assertEquals(
            Contract.Result.Failed(Contract.FailureReason.INVALID_BINDING_CONTEXT),
            Contract.evaluate(Contract.Request(emptyList())),
        )
    }

    @Test
    fun blankCallerBindingKeyFailsClosed() {
        val fixture = Fixture.valid()
        val binding = decision(fixture, fixture.materialization, bindingKey = "")

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, binding.state)
        assertTrue(Contract.FailureReason.P1_BINDING_IDENTITY_MISMATCH in binding.reasons)
    }

    @Test
    fun blankRecordIdFailsClosed() {
        val fixture = Fixture.valid()
        val result = Contract.evaluate(fixture.request(record = fixture.record.copy(recordId = "")))

        assertEquals(Contract.Result.Failed(Contract.FailureReason.INVALID_NEGATIVE_SUPERVISION_RECORD), result)
    }

    @Test
    fun nonNegativeCandidateStateFailsClosed() {
        val fixture = Fixture.valid()
        val nonNegative = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.entries
            .first { it != HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE }
        val result = Contract.evaluate(fixture.request(record = fixture.record.copy(candidateState = nonNegative)))

        assertEquals(Contract.Result.Failed(Contract.FailureReason.INVALID_NEGATIVE_SUPERVISION_RECORD), result)
    }

    @Test
    fun confirmedOriginalDecisionFailsClosed() {
        val fixture = Fixture.valid()
        val result = Contract.evaluate(
            fixture.request(
                record = fixture.record.copy(
                    originalDecision = HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                ),
            ),
        )

        assertEquals(Contract.Result.Failed(Contract.FailureReason.INVALID_NEGATIVE_SUPERVISION_RECORD), result)
    }

    @Test
    fun nonCandidateDownstreamRouteFailsClosed() {
        val fixture = Fixture.valid()
        val route = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.entries
            .first { it != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE }
        val result = Contract.evaluate(fixture.request(record = fixture.record.copy(downstreamRoute = route)))

        assertEquals(Contract.Result.Failed(Contract.FailureReason.INVALID_NEGATIVE_SUPERVISION_RECORD), result)
    }

    @Test
    fun countersAreDerivedFromBindingStates() {
        val first = Fixture.valid(1)
        val second = Fixture.valid(2)
        val broken = second.materialization.copy(
            state = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationState.NOT_YET_PROJECTABLE,
            reasons = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.FailureReason.MISSING_CANONICAL_CONTEXT),
            projectedTrainingExample = null,
            trainingExampleReference = null,
        )
        val batch = assertIs<Contract.Result.Completed>(
            Contract.evaluate(Contract.Request(listOf(first.bindingRequest(), second.bindingRequest(broken)))),
        ).batch

        assertEquals(2, batch.counters.totalBindings)
        assertEquals(1, batch.counters.bound)
        assertEquals(1, batch.counters.notYetBindable)
    }

    @Test
    fun malformedBatchDigestFailsClosed() {
        val batch = completed(Fixture.valid())
        assertFailsWith<IllegalArgumentException> {
            Contract.validate(batch.copy(logicalDigest = "0".repeat(64)))
        }
    }

    @Test
    fun malformedBindingDecisionIdFailsClosed() {
        val batch = completed(Fixture.valid())
        val brokenDecision = batch.decisions.single().copy(bindingDecisionId = "not-a-digest")
        val broken = batch.copy(
            decisions = listOf(brokenDecision),
            logicalDigest = Contract.batchLogicalDigest(batch.copy(decisions = listOf(brokenDecision), logicalDigest = "")),
        )

        assertFailsWith<IllegalArgumentException> { Contract.validate(broken) }
    }

    @Test
    fun materializationDecisionIdentityIsRetained() {
        val fixture = Fixture.valid()
        val decision = completed(fixture).decisions.single()

        assertEquals(fixture.materialization.decisionId, decision.materializationDecisionId)
    }

    @Test
    fun notYetBindableNeverExposesReference() {
        val fixture = Fixture.valid()
        val binding = decision(fixture, fixture.materialization.copy(
            preMaterializationIdentity = Fixture.valid(999).materialization.occurrenceReference,
        ))

        assertEquals(Contract.BindingState.NOT_YET_BINDABLE, binding.state)
        assertEquals(null, binding.trainingExampleReference)
    }

    @Test
    fun reasonsAreUniqueAndStable() {
        val fixture = Fixture.valid()
        val binding = decision(
            fixture,
            fixture.materialization.copy(
                preMaterializationIdentity = Fixture.valid(999).materialization.occurrenceReference,
            ),
            bindingKey = "wrong",
        )

        assertEquals(binding.reasons.distinct(), binding.reasons)
        assertEquals(
            listOf(
                Contract.FailureReason.P1_BINDING_IDENTITY_MISMATCH,
                Contract.FailureReason.MATERIALIZATION_LINEAGE_MISMATCH,
            ),
            binding.reasons,
        )
    }

    @Test
    fun publicApiHasNoFilesystemOrRetrievalOperation() {
        val methodNames = Contract::class.java.declaredMethods.map { it.name }

        assertTrue(methodNames.none { name ->
            name.contains("file", ignoreCase = true) ||
                name.contains("path", ignoreCase = true) ||
                name.contains("read", ignoreCase = true) ||
                name.contains("search", ignoreCase = true) ||
                name.contains("fetch", ignoreCase = true)
        })
    }

    @Test
    fun resultDoesNotCreateNegativeTrainingExample() {
        val result = completed(Fixture.valid())

        assertNotNull(result.decisions.single().trainingExampleReference)
        assertEquals(Contract.BindingState.BOUND, result.decisions.single().state)
    }

    @Test
    fun resultContainsNoPersistencePath() {
        val batch = completed(Fixture.valid())

        assertTrue(batch.decisions.all { it.materializationDecisionId != null })
        assertTrue(batch.logicalDigest.isNotBlank())
    }

    @Test
    fun onlyReferenceNotFullExampleIsPublishedInDecision() {
        val decision = completed(Fixture.valid()).decisions.single()
        val propertyNames = decision::class.java.declaredFields.map { it.name }

        assertTrue("trainingExampleReference" in propertyNames)
        assertTrue("projectedTrainingExample" !in propertyNames)
    }

    @Test
    fun sameReferenceCanBeSharedWithoutDuplicateReferenceRejection() {
        val first = Fixture.valid(1)
        val second = Fixture.valid(2, first.canonical).copy(
            example = first.example,
            materialization = first.materialization,
        )
        val batch = assertIs<Contract.Result.Completed>(
            Contract.evaluate(Contract.Request(listOf(first.bindingRequest(), second.bindingRequest()))),
        ).batch

        assertEquals(first.example.exampleReference, second.example.exampleReference)
        assertEquals(2, batch.counters.bound)
    }

    private fun completed(fixture: Fixture): Contract.Batch =
        assertIs<Contract.Result.Completed>(Contract.evaluate(fixture.request())).batch

    private fun decision(
        fixture: Fixture,
        materialization: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationDecision?,
        record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record = fixture.record,
        bindingKey: String = record.recordId,
    ): Contract.BindingDecision = assertIs<Contract.Result.Completed>(
        Contract.evaluate(
            Contract.Request(
                listOf(Contract.BindingRequest(record, materialization, bindingKey)),
            ),
        ),
    ).batch.decisions.single()

    private data class Fixture(
        val index: Int,
        val canonical: HimEntityId,
        val record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        val example: HimTrainingExampleV1,
        val materialization: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationDecision,
    ) {
        fun request(
            decision: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationDecision? = materialization,
            record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record = this.record,
            bindingKey: String = record.recordId,
        ) = Contract.Request(listOf(bindingRequest(decision, record, bindingKey)))

        fun bindingRequest(
            decision: HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationDecision? = materialization,
            record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record = this.record,
            bindingKey: String = record.recordId,
        ) = Contract.BindingRequest(record, decision, bindingKey)

        companion object {
            fun valid(
                index: Int = 1,
                canonical: HimEntityId = HimEntityId("C00101"),
                evidenceSuffix: String = "same",
            ): Fixture {
                val evidence = HimEvidenceReference(
                    source = "OPEN_FOOD_FACTS",
                    sourceArtifactSha256 = HimSha256("b".repeat(64)),
                    sourceRecordIdentity = "off:fixture:$evidenceSuffix:$index",
                )
                val trainingEvidence = de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1(
                    reference = evidence,
                    recordKind = "FOOD",
                    retrievalRank = 1,
                )
                val example = HimTrainingExampleV1.create(
                    taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
                    input = HimTrainingInputV1(
                        observedTerm = "Artichoke $index",
                        normalizedObservedTerm = "artichoke-$index",
                        canonicalContext = listOf(HimCandidateCanonicalContext(1, canonical, "Artichoke", null)),
                        evidence = listOf(trainingEvidence),
                    ),
                    target = HimTrainingTargetV1.ExistingCanonical(canonical),
                    provenance = HimTrainingProvenanceV1(sourceEvidenceReferences = listOf(evidence)),
                )
                val record = record(index, canonical)
                val occurrence = HimCandidateOccurrenceReference("occurrence:v1:${digest(index)}")
                val materialization = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationDecision(
                    decisionId = digest(900 + index),
                    preMaterializationIdentity = occurrence,
                    occurrenceReference = occurrence,
                    projectionInputBindingDigest = HimSha256(digest(1000 + index)),
                    state = HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1.MaterializationState.PROJECTED,
                    reasons = emptyList(),
                    projectedTrainingExample = example,
                    trainingExampleReference = example.exampleReference,
                )
                return Fixture(index, canonical, record, example, materialization)
            }

            private fun record(index: Int, canonical: HimEntityId) =
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record(
                    recordId = digest(index),
                    negativeCandidateId = digest(100 + index),
                    readinessDecisionId = digest(200 + index),
                    readinessState = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
                    eligibilityBatchId = "fixture-eligibility",
                    eligibilityInputBindingDigest = digest(300 + index),
                    eligibilityDecisionId = digest(400 + index),
                    eligibilityState = HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE,
                    validationBatchId = "fixture-validation",
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
                    validationReasonCodes = listOf(
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION,
                    ),
                    evidenceReferenceIds = listOf("persisted-evidence-$index"),
                    downstreamRoute = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE,
                    candidateState = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE,
                )

            private fun digest(value: Int): String = value.toString().padStart(64, '0')
        }
    }
}
