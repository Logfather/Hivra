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
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleBindingResolutionContractV1 as contract
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleBindingResolutionContractV1Test {
    @Test
    fun contractIdentityAndContextOnlyStateAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_BINDING_RESOLUTION_CONTRACT_V1",
            contract.CONTRACT_ID,
        )
        assertEquals("1", contract.VERSION)
        assertEquals("POSITIVE_TRAINING_EXAMPLE_BINDING_RESOLUTION_CONTEXT_ONLY", contract.STATE)
    }

    @Test
    fun explicitPairIsValidatedButSameInputRemainsNotYetResolved() {
        val fixture = fixture()
        val decision = completed(request(fixture)).decisions.single()

        assertEquals(contract.ResolutionState.NOT_YET_RESOLVED, decision.state)
        assertEquals(
            listOf(
                contract.FailureReason.INPUT_IDENTITY_NOT_PROVABLE,
                contract.FailureReason.EVIDENCE_RELATION_NOT_PROVABLE,
            ),
            decision.reasons,
        )
        assertEquals(fixture.positive.exampleReference, decision.positiveExampleReference)
        assertEquals(fixture.positive.target, decision.positiveTarget)
    }

    @Test
    fun missingPositiveExampleIsNotYetResolved() {
        val fixture = fixture()
        val decision = completed(
            request(fixture, positive = null),
        ).decisions.single()

        assertEquals(contract.ResolutionState.NOT_YET_RESOLVED, decision.state)
        assertEquals(listOf(contract.FailureReason.MISSING_POSITIVE_TRAINING_EXAMPLE), decision.reasons)
        assertEquals(null, decision.positiveExampleReference)
    }

    @Test
    fun positiveReferenceIsTheExistingDeterministicTrainingReference() {
        val fixture = fixture()
        val expected = HimTrainingExampleIdentityV1.example(
            taskType = fixture.positive.taskType,
            input = fixture.positive.input,
            target = fixture.positive.target,
            provenance = fixture.positive.provenance,
        )

        assertEquals(expected, fixture.positive.exampleReference)
        assertEquals(expected, completed(request(fixture)).decisions.single().positiveExampleReference)
    }

    @Test
    fun invalidTrainingExampleReferenceIsRejectedByTheExistingModel() {
        val fixture = fixture()

        assertFails {
            HimTrainingExampleV1(
                exampleReference = HimTrainingExampleReference("example:v1:${"0".repeat(64)}"),
                taskType = fixture.positive.taskType,
                input = fixture.positive.input,
                target = fixture.positive.target,
                provenance = fixture.positive.provenance,
            )
        }
    }

    @Test
    fun invalidTrainingInputFailsClosedBeforeBinding() {
        assertFails { HimTrainingInputV1(observedTerm = "", normalizedObservedTerm = "valid") }
    }

    @Test
    fun invalidPositiveTargetFailsClosedBeforeBinding() {
        assertFails { HimTrainingTargetV1.NewCanonical("") }
    }

    @Test
    fun invalidTrainingProvenanceFailsClosedBeforeBinding() {
        val reference = evidence(1).reference

        assertFails {
            HimTrainingProvenanceV1(sourceEvidenceReferences = listOf(reference, reference))
        }
    }

    @Test
    fun sameInputCompatibilityIsExplicitlyNotProvableFromPersistedRecord() {
        val decision = completed(request(fixture())).decisions.single()

        assertEquals(contract.InputIdentity.NOT_PROVABLE, decision.inputIdentity)
        assertTrue(contract.FailureReason.INPUT_IDENTITY_NOT_PROVABLE in decision.reasons)
    }

    @Test
    fun evidenceCompatibilityIsExplicitlyNotProvableFromOpaquePersistedIds() {
        val decision = completed(request(fixture())).decisions.single()

        assertEquals(contract.EvidenceRelationship.NOT_PROVABLE, decision.evidenceRelationship)
        assertTrue(contract.FailureReason.EVIDENCE_RELATION_NOT_PROVABLE in decision.reasons)
    }

    @Test
    fun incompatibleCanonicalTargetFailsClosed() {
        val recordFixture = fixture(canonical = CANONICAL)
        val positiveFixture = fixture(index = 2, canonical = OTHER_CANONICAL)
        val decision = completed(
            request(recordFixture, positive = positiveFixture.positive),
        ).decisions.single()

        assertEquals(contract.ResolutionState.NOT_YET_RESOLVED, decision.state)
        assertEquals(listOf(contract.FailureReason.POSITIVE_EXAMPLE_RECORD_MISMATCH), decision.reasons)
    }

    @Test
    fun newCanonicalTargetCannotBeComparedToPersistedCanonical() {
        val fixture = fixture()
        val positive = HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = fixture.positive.input.copy(
                observedTerm = "Unknown food",
                normalizedObservedTerm = "unknown food",
                canonicalContext = emptyList(),
                evidence = emptyList(),
            ),
            target = HimTrainingTargetV1.NewCanonical("Unknown food"),
        )
        val decision = completed(request(fixture, positive)).decisions.single()

        assertEquals(contract.FailureReason.POSITIVE_TARGET_NOT_COMPARABLE, decision.reasons.single())
        assertEquals(contract.ResolutionState.NOT_YET_RESOLVED, decision.state)
    }

    @Test
    fun foreignNegativeRecordBindingFailsClosed() {
        val recordFixture = fixture(canonical = CANONICAL)
        val foreign = fixture(index = 2, canonical = OTHER_CANONICAL)
        val decision = completed(
            request(recordFixture, positive = foreign.positive),
        ).decisions.single()

        assertTrue(contract.FailureReason.POSITIVE_EXAMPLE_RECORD_MISMATCH in decision.reasons)
    }

    @Test
    fun duplicateRecordBindingFailsClosed() {
        val fixture = fixture()
        val result = contract.evaluate(
            contract.Request(
                records = listOf(fixture.record),
                bindings = listOf(
                    contract.Binding(fixture.record.recordId, fixture.positive),
                    contract.Binding(fixture.record.recordId, null),
                ),
            ),
        )

        assertEquals(contract.FailureReason.DUPLICATE_RECORD_BINDING, assertIs<contract.Result.Failed>(result).reason)
    }

    @Test
    fun duplicatePositiveExampleBindingFailsClosed() {
        val first = fixture(index = 1)
        val second = fixture(index = 2)
        val result = contract.evaluate(
            contract.Request(
                records = listOf(first.record, second.record),
                bindings = listOf(
                    contract.Binding(first.record.recordId, first.positive),
                    contract.Binding(second.record.recordId, first.positive),
                ),
            ),
        )

        assertEquals(
            contract.FailureReason.DUPLICATE_POSITIVE_EXAMPLE_BINDING,
            assertIs<contract.Result.Failed>(result).reason,
        )
    }

    @Test
    fun missingBindingProducesNotYetResolvedDecision() {
        val fixture = fixture()
        val decision = completed(
            contract.Request(records = listOf(fixture.record), bindings = emptyList()),
        ).decisions.single()

        assertEquals(listOf(contract.FailureReason.MISSING_POSITIVE_TRAINING_EXAMPLE), decision.reasons)
    }

    @Test
    fun extraBindingFailsClosed() {
        val first = fixture(index = 1)
        val second = fixture(index = 2)
        val result = contract.evaluate(
            contract.Request(
                records = listOf(first.record),
                bindings = listOf(contract.Binding(second.record.recordId, second.positive)),
            ),
        )

        assertEquals(contract.FailureReason.EXTRA_BINDING, assertIs<contract.Result.Failed>(result).reason)
    }

    @Test
    fun persistedRecordOrderIsPreserved() {
        val first = fixture(index = 1)
        val second = fixture(index = 2)
        val batch = completed(
            contract.Request(
                records = listOf(second.record, first.record),
                bindings = listOf(
                    contract.Binding(first.record.recordId, first.positive),
                    contract.Binding(second.record.recordId, second.positive),
                ),
            ),
        )

        assertEquals(
            listOf(second.record.recordId, first.record.recordId),
            batch.decisions.map { it.recordId },
        )
    }

    @Test
    fun reasonCodeOrderIsStable() {
        val first = completed(request(fixture())).decisions.single().reasons
        val second = completed(request(fixture())).decisions.single().reasons

        assertEquals(first, second)
        assertEquals(
            listOf(
                contract.FailureReason.INPUT_IDENTITY_NOT_PROVABLE,
                contract.FailureReason.EVIDENCE_RELATION_NOT_PROVABLE,
            ),
            first,
        )
    }

    @Test
    fun decisionIdsAreDeterministicAndUniquePerRecord() {
        val first = fixture(index = 1)
        val second = fixture(index = 2)
        val request = contract.Request(
            records = listOf(first.record, second.record),
            bindings = listOf(
                contract.Binding(first.record.recordId, first.positive),
                contract.Binding(second.record.recordId, second.positive),
            ),
        )

        val one = completed(request)
        val two = completed(request)

        assertEquals(one.decisions.map { it.decisionId }, two.decisions.map { it.decisionId })
        assertEquals(2, one.decisions.map { it.decisionId }.distinct().size)
    }

    @Test
    fun batchLogicalDigestIsDeterministicAndRecomputable() {
        val batch = completed(request(fixture()))

        assertEquals(contract.batchLogicalDigest(batch.copy(logicalDigest = "")), batch.logicalDigest)
        assertEquals(batch, completed(request(fixture())))
    }

    @Test
    fun countersAreDerivedFromTypedResolutionStates() {
        val fixture = fixture()
        val batch = completed(
            contract.Request(
                records = listOf(fixture.record),
                bindings = listOf(contract.Binding(fixture.record.recordId, null)),
            ),
        )

        assertEquals(1, batch.counters.totalRecords)
        assertEquals(0, batch.counters.resolvedPositiveTrainingExampleBindings)
        assertEquals(1, batch.counters.notYetResolved)
    }

    @Test
    fun outputDoesNotExposeNegativeExamplesPartitionsOrPersistence() {
        val fieldNames = completed(request(fixture()))::class.java.declaredFields.map { it.name }.toSet()

        assertFalse(fieldNames.contains("negativeTrainingExample"))
        assertFalse(fieldNames.contains("partition"))
        assertFalse(fieldNames.contains("persistence"))
        assertFalse(fieldNames.contains("corpus"))
    }

    @Test
    fun emptyRequestFailsClosed() {
        val result = contract.evaluate(contract.Request(records = emptyList(), bindings = emptyList()))

        assertEquals(contract.FailureReason.INVALID_BINDING_CONTEXT, assertIs<contract.Result.Failed>(result).reason)
    }

    @Test
    fun malformedPersistedRecordFailsClosed() {
        val fixture = fixture()
        val result = contract.evaluate(
            contract.Request(
                records = listOf(fixture.record.copy(canonicalEntityId = "")),
                bindings = listOf(contract.Binding(fixture.record.recordId, fixture.positive)),
            ),
        )

        assertEquals(contract.FailureReason.INVALID_PERSISTED_RECORD, assertIs<contract.Result.Failed>(result).reason)
    }

    @Test
    fun positiveTargetAndReferenceArePreservedWithoutSemanticCoercion() {
        val fixture = fixture()
        val decision = completed(request(fixture)).decisions.single()

        assertEquals(fixture.positive.target, decision.positiveTarget)
        assertEquals(fixture.positive.exampleReference, decision.positiveExampleReference)
    }

    @Test
    fun publicApiHasNoSourceFilesystemSearchOrPersistenceCapability() {
        val publicMethods = contract::class.java.declaredMethods
            .filter { java.lang.reflect.Modifier.isPublic(it.modifiers) }
        val names = publicMethods.map { it.name }.toSet()

        assertTrue(names.containsAll(setOf("evaluate", "validate", "batchLogicalDigest")))
        assertTrue(names.none { name ->
            listOf("search", "fetch", "write", "persist", "sqlite", "fts", "partition", "train")
                .any { forbidden -> name.contains(forbidden, ignoreCase = true) }
        })
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

    @Test
    fun sourceAndFilesystemAreNotTouchedByRepeatedEvaluation() {
        val fixture = fixture()
        val first = completed(request(fixture))
        val second = completed(request(fixture))

        assertEquals(first, second)
        assertEquals(contract.ResolutionState.NOT_YET_RESOLVED, first.decisions.single().state)
    }

    @Test
    fun resolvedStateRemainsTypedEvenWhenCurrentRecordCannotReachIt() {
        assertTrue(contract.ResolutionState.entries.contains(contract.ResolutionState.RESOLVED_POSITIVE_TRAINING_EXAMPLE_BINDING))
        assertTrue(contract.ResolutionState.entries.contains(contract.ResolutionState.NOT_YET_RESOLVED))
    }

    private fun completed(request: contract.Request): contract.Batch =
        assertIs<contract.Result.Completed>(contract.evaluate(request)).value

    private fun request(
        fixture: Fixture,
        positive: HimTrainingExampleV1? = fixture.positive,
    ) = contract.Request(
        records = listOf(fixture.record),
        bindings = listOf(contract.Binding(fixture.record.recordId, positive)),
    )

    private data class Fixture(
        val record: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record,
        val positive: HimTrainingExampleV1,
    )

    private fun fixture(
        index: Int = 1,
        canonical: HimEntityId = CANONICAL,
    ): Fixture {
        val evidence = evidence(index)
        val input = HimTrainingInputV1(
            observedTerm = "Crème double $index",
            normalizedObservedTerm = "creme double $index",
            canonicalContext = listOf(HimCandidateCanonicalContext(1, canonical, "Crème double", null)),
            evidence = listOf(evidence.trainingEvidence),
        )
        val positive = HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = input,
            target = HimTrainingTargetV1.ExistingCanonical(canonical),
            provenance = HimTrainingProvenanceV1(sourceEvidenceReferences = listOf(evidence.reference)),
        )
        val reason = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
            .DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION
        val record = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Record(
            recordId = digest(index),
            negativeCandidateId = digest(100 + index),
            readinessDecisionId = digest(200 + index),
            readinessState = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
            eligibilityBatchId = "fixture-eligibility-batch",
            eligibilityInputBindingDigest = digest(300 + index),
            eligibilityDecisionId = digest(400 + index),
            eligibilityState = HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE,
            validationBatchId = "fixture-validation-batch",
            validationBatchBindingDigest = digest(500 + index),
            validationBatchLogicalDigest = digest(600 + index),
            validationRecordId = digest(700 + index),
            reviewUnitId = digest(800 + index),
            stableEntryId = digest(900 + index),
            canonicalEntityId = canonical.value,
            originalReviewerRef = "reviewer:fixture:v1",
            validatorReviewerRef = "validator:fixture:v1",
            validationRound = 1,
            validationRevision = 1,
            originalDecision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
            validationReasonCodes = listOf(reason),
            evidenceReferenceIds = listOf(digest(1000 + index)),
            downstreamRoute = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE,
            candidateState = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE,
        )
        return Fixture(record, positive)
    }

    private data class EvidenceFixture(val trainingEvidence: HimTrainingEvidenceInputV1) {
        val reference: HimEvidenceReference
            get() = trainingEvidence.reference
    }

    private fun evidence(index: Int): EvidenceFixture = EvidenceFixture(
        HimTrainingEvidenceInputV1(
            reference = HimEvidenceReference(
                source = "OPEN_FOOD_FACTS",
                sourceArtifactSha256 = HimSha256("a".repeat(64)),
                sourceRecordIdentity = "off:fixture:$index",
            ),
            recordKind = "FOOD",
            retrievalRank = 1,
        ),
    )

    private fun digest(seed: Int): String =
        seed.toString().padStart(64, '0').takeLast(64)

    companion object {
        private val CANONICAL = HimEntityId("rVnyq7")
        private val OTHER_CANONICAL = HimEntityId("C00202")
    }
}
