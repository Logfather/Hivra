package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1 as MaterializationContract
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimValidationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateConfidenceDiagnostics
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDataset
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateEvidenceProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateFinalInferenceOutcome
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationInputRun
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationRun
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationRunState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateHypothesis
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInferenceProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputCompletionState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionProposalFactoryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionTargetV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCanonicalFamilyChildMutationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimDeterministicPromotionExecutionResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecision
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationRecord
import de.shopme.tools.knowledge.him.training.corpus.HimGroundTruthTrainingExampleProjectionInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleValidatorV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import java.lang.reflect.Modifier
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RunHimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1Test {

    @Test
    fun contractIdentityAndContextAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_MATERIALIZATION_CONTRACT_V1",
            MaterializationContract.CONTRACT_ID,
        )
        assertEquals("1", MaterializationContract.VERSION)
        assertEquals("POSITIVE_TRAINING_EXAMPLE_MATERIALIZATION_CONTEXT_ONLY", MaterializationContract.STATE)
    }

    @Test
    fun completeHistoricalLineageProjects() {
        val decision = MaterializationContract.materialize(Fixture.base().request())
        assertEquals(MaterializationContract.MaterializationState.PROJECTED, decision.state)
        assertTrue(decision.reasons.isEmpty())
        assertNotNull(decision.projectedTrainingExample)
        assertNotNull(decision.trainingExampleReference)
    }

    @Test
    fun projectedExamplePassesValidator() {
        val example = requireNotNull(MaterializationContract.materialize(Fixture.base().request()).projectedTrainingExample)
        HimTrainingExampleValidatorV1.validate(example)
        assertEquals(HimTrainingTargetV1.Variant::class, example.target::class)
    }

    @Test
    fun projectedExampleReferenceIsReproducible() {
        val example = requireNotNull(MaterializationContract.materialize(Fixture.base().request()).projectedTrainingExample)
        assertEquals(
            example.exampleReference,
            de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleIdentityV1.example(
                taskType = example.taskType,
                input = example.input,
                target = example.target,
                provenance = example.provenance,
            ),
        )
    }

    @Test
    fun repeatedMaterializationIsIdentical() {
        val fixture = Fixture.base()
        assertEquals(MaterializationContract.materialize(fixture.request()), MaterializationContract.materialize(fixture.request()))
    }

    @Test
    fun batchPreservesCallerOrder() {
        val first = Fixture.base("first")
        val second = Fixture.base("second")
        val batch = completed(MaterializationContract.materializeBatch(listOf(second.request(), first.request())))
        assertEquals(
            listOf(second.occurrence.occurrenceReference, first.occurrence.occurrenceReference),
            batch.decisions.map { it.occurrenceReference },
        )
    }

    @Test
    fun decisionIdsAreDeterministic() {
        val fixture = Fixture.base()
        assertEquals(
            MaterializationContract.materialize(fixture.request()).decisionId,
            MaterializationContract.materialize(fixture.request()).decisionId,
        )
    }

    @Test
    fun batchDigestIsDeterministic() {
        val fixture = Fixture.base()
        val first = completed(MaterializationContract.materializeBatch(listOf(fixture.request())))
        val second = completed(MaterializationContract.materializeBatch(listOf(fixture.request())))
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first, second)
    }

    @Test
    fun missingOccurrenceBindingIsTyped() {
        val fixture = Fixture.base()
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(HimCandidateOccurrenceReference("occurrence:v1:${"a".repeat(64)}"), fixture.input()),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.MISSING_OCCURRENCE_BINDING)
    }

    @Test
    fun missingCanonicalContextIsTyped() {
        val fixture = Fixture.base()
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(
                fixture.occurrence.occurrenceReference,
                fixture.input(canonicalContext = emptyList()),
            ),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.MISSING_CANONICAL_CONTEXT)
    }

    @Test
    fun missingTypedEvidenceIsTyped() {
        val fixture = Fixture.base()
        val modelDerivedCandidate = fixture.candidate.copy(
            evidenceOrigin = de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin.MODEL_DERIVED,
        )
        val occurrence = fixture.occurrence.copy(
            occurrenceReference = HimCandidateIdentityV1.occurrence(
                candidate = modelDerivedCandidate.candidateReference,
                run = fixture.generationRun.runReference,
                inputRun = fixture.occurrence.inputRunReference,
                evidence = emptyList(),
            ),
            candidate = modelDerivedCandidate,
            evidence = emptyList(),
        )
        val generationRun = fixture.generationRun.copy(occurrences = listOf(occurrence))
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(
                occurrence.occurrenceReference,
                fixture.input(occurrence, generationRun).copy(candidate = modelDerivedCandidate),
            ),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.MISSING_TYPED_EVIDENCE)
    }

    @Test
    fun missingValidationLineageIsTyped() {
        val fixture = Fixture.base()
        val broken = fixture.validation.copy(candidateReference = otherCandidate())
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(validation = broken)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.MISSING_VALIDATION_LINEAGE)
    }

    @Test
    fun missingPromotionLineageIsTyped() {
        val fixture = Fixture.base()
        val broken = fixture.promotion.copy(candidateReference = otherCandidate())
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(promotion = broken)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.MISSING_PROMOTION_LINEAGE)
    }

    @Test
    fun missingMutationLineageIsTyped() {
        val fixture = Fixture.base()
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(mutation = null)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.MISSING_MUTATION_LINEAGE)
    }

    @Test
    fun missingExecutionLineageIsTyped() {
        val fixture = Fixture.base()
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(promotionExecution = null)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.MISSING_EXECUTION_LINEAGE)
    }

    @Test
    fun missingAuthoritySnapshotIsTyped() {
        val fixture = Fixture.base()
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(resultingAuthority = null)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.MISSING_RESULTING_AUTHORITY_SNAPSHOT)
    }

    @Test
    fun missingRegistrySnapshotIsTyped() {
        val fixture = Fixture.base()
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(resultingEntityIdRegistry = null)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.MISSING_RESULTING_REGISTRY_SNAPSHOT)
    }

    @Test
    fun candidateOccurrenceMismatchFailsClosed() {
        val expected = Fixture.base("expected")
        val actual = Fixture.base("actual")
        val input = expected.input().copy(
            occurrence = actual.occurrence,
            generationRun = actual.generationRun,
        )
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(actual.occurrence.occurrenceReference, input),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.INCONSISTENT_CANDIDATE_LINEAGE)
    }

    @Test
    fun occurrenceNotInGenerationRunFailsClosed() {
        val fixture = Fixture.base()
        val generationRun = fixture.generationRun.copy(occurrences = emptyList())
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(generationRun = generationRun)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.INCONSISTENT_OCCURRENCE_LINEAGE)
    }

    @Test
    fun validationMismatchFailsClosed() {
        val fixture = Fixture.base()
        val broken = fixture.validation.copy(candidateDatasetDigest = HimSha256("f".repeat(64)))
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(validation = broken)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.MISSING_VALIDATION_LINEAGE)
    }

    @Test
    fun promotionTargetMismatchFailsClosed() {
        val fixture = Fixture.base()
        val target = HimCandidatePromotionTargetV1.AddVariant(HimFamilyEntityReference.Canonical(HimEntityId("Abc123")), "Other")
        val promotion = fixture.promotion.copy(target = target)
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(promotion = promotion)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.INVALID_PROJECTED_TRAINING_EXAMPLE)
    }

    @Test
    fun targetLineageMismatchFailsClosed() {
        val fixture = Fixture.base()
        val target = HimCandidatePromotionTargetV1.AddAlias(
            HimFamilyEntityReference.Canonical(HERRING_ID),
            fixture.candidate.candidateTerm,
        )
        val promotionReference = HimCandidatePromotionIdentityV1.promotion(
            candidateReference = fixture.candidate.candidateReference,
            validationReference = fixture.validation.validationReference,
            candidateDatasetDigest = fixture.candidateDatasetDigest,
            authorityDigestBefore = fixture.authorityDigestBefore,
            entityIdRegistryDigestBefore = fixture.registryDigestBefore,
            target = target,
        )
        val promotion = fixture.promotion.copy(promotionReference = promotionReference, target = target)
        val entityId = HimEntityId("cdef12")
        val beforeAuthority = fixture.resultingAuthority!!.copy(
            families = fixture.resultingAuthority!!.families.map { family ->
                family.copy(
                    variants = family.variants.filterNot {
                        it.variantId == fixture.promotionExecution!!.newEntityId
                    },
                )
            },
        )
        val beforeRegistry = fixture.resultingEntityIdRegistry!!.copy(
            entries = fixture.resultingEntityIdRegistry!!.entries.filterNot {
                it.entityId == fixture.promotionExecution!!.newEntityId
            },
        )
        val mutationResult = HimCanonicalFamilyChildMutationV1().apply(
            authorityBefore = beforeAuthority,
            registryBefore = beforeRegistry,
            promotion = promotion,
            validationReference = HimValidationReference(fixture.validation.validationReference.value),
            newEntityId = entityId,
            authoritySha256Before = fixture.authorityDigestBefore,
            registrySha256Before = fixture.registryDigestBefore,
        )
        val mutation = mutationResult.mutationLedgerEntry
        val execution = HimDeterministicPromotionExecutionResultV1(
            previousReleaseReference = HimGroundTruthReleaseIdentityV1("release:v1:${"a".repeat(64)}"),
            newReleaseReference = fixture.groundTruthReleaseReference,
            newEntityId = entityId,
            mutationReference = mutation.mutationReference,
            promotionType = promotion.target.promotionType,
            targetType = mutation.entityType,
            createdNewRelease = true,
            activeReleaseVerified = true,
        )
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(
                fixture.occurrence.occurrenceReference,
                fixture.input(
                    promotion = promotion,
                    mutation = mutation,
                    promotionExecution = execution,
                    resultingAuthority = mutationResult.authorityAfter,
                    resultingEntityIdRegistry = mutationResult.registryAfter,
                ),
            ),
        )
        assertEquals(MaterializationContract.MaterializationState.NOT_YET_PROJECTABLE, decision.state)
        assertEquals(
            listOf(MaterializationContract.FailureReason.INCONSISTENT_TARGET_LINEAGE),
            decision.reasons,
        )
        assertNull(decision.projectedTrainingExample)
        assertNull(decision.trainingExampleReference)
    }

    @Test
    fun mutationMismatchFailsClosed() {
        val fixture = Fixture.base()
        val mutation = fixture.mutation!!.copy(newEntityId = HimEntityId("Abc123"))
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(mutation = mutation)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.INVALID_PROJECTED_TRAINING_EXAMPLE)
    }

    @Test
    fun releaseMismatchFailsClosed() {
        val fixture = Fixture.base()
        val release = HimGroundTruthReleaseIdentityV1("release:v1:${"c".repeat(64)}")
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(groundTruthReleaseReference = release)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.INVALID_PROJECTED_TRAINING_EXAMPLE)
    }

    @Test
    fun evidenceMismatchFailsClosed() {
        val fixture = Fixture.base()
        val evidence = listOf(fixture.evidence.first(), fixture.evidence.first())
        val occurrence = fixture.occurrence.copy(evidence = evidence)
        val generationRun = fixture.generationRun.copy(occurrences = listOf(occurrence))
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(occurrence.occurrenceReference, fixture.input(occurrence, generationRun)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.INCONSISTENT_EVIDENCE_LINEAGE)
    }

    @Test
    fun duplicateOccurrenceBindingFailsClosed() {
        val fixture = Fixture.base()
        val failure = failed(MaterializationContract.materializeBatch(listOf(fixture.request(), fixture.request())))
        assertEquals(MaterializationContract.FailureReason.DUPLICATE_PRE_MATERIALIZATION_IDENTITY, failure.reason)
    }

    @Test
    fun duplicateMaterializedReferenceFailsClosed() {
        val first = Fixture.base("first")
        val secondOccurrence = first.occurrence.copy(
            occurrenceReference = HimCandidateOccurrenceReference("occurrence:v1:${"b".repeat(64)}"),
        )
        val secondRun = first.generationRun.copy(occurrences = listOf(secondOccurrence))
        val second = first.request(secondOccurrence, secondRun)
        val failure = failed(MaterializationContract.materializeBatch(listOf(first.request(), second)))
        assertEquals(MaterializationContract.FailureReason.DUPLICATE_MATERIALIZED_TRAINING_EXAMPLE, failure.reason)
    }

    @Test
    fun duplicateProjectionInputBindingFailsClosed() {
        val fixture = Fixture.base()
        val failure = failed(
            MaterializationContract.materializeBatch(
                listOf(
                    fixture.request(),
                    MaterializationContract.Request(
                        HimCandidateOccurrenceReference("occurrence:v1:${"b".repeat(64)}"),
                        fixture.input(),
                    ),
                ),
            ),
        )
        assertEquals(MaterializationContract.FailureReason.DUPLICATE_PROJECTION_INPUT_BINDING, failure.reason)
    }

    @Test
    fun duplicateMaterializedReferenceIsNotSilentlyAccepted() {
        val first = Fixture.base("first")
        val secondOccurrence = first.occurrence.copy(
            occurrenceReference = HimCandidateOccurrenceReference("occurrence:v1:${"b".repeat(64)}"),
        )
        val secondRun = first.generationRun.copy(occurrences = listOf(secondOccurrence))
        val second = first.request(secondOccurrence, secondRun)
        val result = MaterializationContract.materializeBatch(listOf(first.request(), second))
        assertTrue(result is MaterializationContract.BatchResult.Failed)
    }

    @Test
    fun createCanonicalIsNotYetProjectable() {
        val fixture = Fixture.base()
        val target = HimCandidatePromotionTargetV1.CreateCanonical("New fish")
        val promotionReference = HimCandidatePromotionIdentityV1.promotion(
            candidateReference = fixture.candidate.candidateReference,
            validationReference = fixture.validation.validationReference,
            candidateDatasetDigest = fixture.candidateDatasetDigest,
            authorityDigestBefore = fixture.authorityDigestBefore,
            entityIdRegistryDigestBefore = fixture.registryDigestBefore,
            target = target,
        )
        val promotion = fixture.promotion.copy(promotionReference = promotionReference, target = target)
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(promotion = promotion)),
        )
        assertNotYet(decision, MaterializationContract.FailureReason.UNSUPPORTED_CREATE_CANONICAL_PROJECTION)
        assertNull(decision.projectedTrainingExample)
    }

    @Test
    fun notYetProjectableNeverContainsPartialExample() {
        val fixture = Fixture.base()
        val decision = MaterializationContract.materialize(
            MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(mutation = null)),
        )
        assertNull(decision.projectedTrainingExample)
        assertNull(decision.trainingExampleReference)
    }

    @Test
    fun countersMatchDecisionStates() {
        val projected = Fixture.base("projected")
        val missing = Fixture.base("missing")
        val batch = completed(
            MaterializationContract.materializeBatch(
                listOf(
                    projected.request(),
                    missing.requestOf(missing.input(mutation = null)),
                ),
            ),
        )
        assertEquals(2, batch.counters.totalRecords)
        assertEquals(1, batch.counters.projected)
        assertEquals(1, batch.counters.notYetProjectable)
    }

    @Test
    fun manipulatedBatchDigestFailsClosed() {
        val batch = completed(MaterializationContract.materializeBatch(listOf(Fixture.base().request())))
        assertIllegalArgument { MaterializationContract.validate(batch.copy(logicalDigest = "a".repeat(64))) }
    }

    @Test
    fun inputValuesRemainHistorical() {
        val fixture = Fixture.base()
        val example = requireNotNull(MaterializationContract.materialize(fixture.request()).projectedTrainingExample)
        val inputRun = fixture.generationRun.inputRuns.single()
        assertEquals(inputRun.input.rawInput, example.input.observedTerm)
        assertEquals(inputRun.input.normalizedLookup, example.input.normalizedObservedTerm)
        assertEquals(HERRING_ID, example.input.canonicalContext.single().canonicalId)
    }

    @Test
    fun evidenceIsProjectedWithoutLookup() {
        val fixture = Fixture.base()
        val example = requireNotNull(MaterializationContract.materialize(fixture.request()).projectedTrainingExample)
        assertEquals(fixture.evidence.map { it.reference.sourceRecordIdentity }, example.input.evidence.map { it.reference.sourceRecordIdentity })
        assertEquals(example.input.evidence.map { it.reference }, example.provenance.sourceEvidenceReferences)
    }

    @Test
    fun materializedTargetComesFromPromotionTarget() {
        val example = requireNotNull(MaterializationContract.materialize(Fixture.base().request()).projectedTrainingExample)
        assertEquals(HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HERRING_ID)), example.target)
    }

    @Test
    fun provenanceRetainsHistoricalLineage() {
        val fixture = Fixture.base()
        val example = requireNotNull(MaterializationContract.materialize(fixture.request()).projectedTrainingExample)
        assertEquals(fixture.candidate.candidateReference, example.provenance.candidateReference)
        assertEquals(fixture.generationRun.runReference, example.provenance.generationRunReference)
        assertEquals(fixture.validation.validationReference, example.provenance.validationReference)
        assertEquals(fixture.promotion.promotionReference, example.provenance.promotionReference)
        assertEquals(fixture.groundTruthReleaseReference, example.provenance.groundTruthReleaseReference)
    }

    @Test
    fun decisionReasonsAreStableAndTyped() {
        val fixture = Fixture.base()
        val first = MaterializationContract.materialize(MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(mutation = null)))
        val second = MaterializationContract.materialize(MaterializationContract.Request(fixture.occurrence.occurrenceReference, fixture.input(mutation = null)))
        assertEquals(first.reasons, second.reasons)
        assertTrue(first.reasons.contains(MaterializationContract.FailureReason.MISSING_MUTATION_LINEAGE))
    }

    @Test
    fun publicApiHasNoOperationalAccess() {
        val forbidden = setOf("File", "Path", "Files", "Connection")
        val publicMethods = MaterializationContract::class.java.methods
            .filter { Modifier.isPublic(it.modifiers) }
            .map { it.name }
        assertTrue(publicMethods.none { it.contains("search", ignoreCase = true) })
        assertTrue(publicMethods.none { it.contains("fetch", ignoreCase = true) })
        assertTrue(publicMethods.none { it.contains("persist", ignoreCase = true) })
        assertTrue(forbidden.none { word -> publicMethods.any { it.contains(word) } })
    }

    @Test
    fun materializationDoesNotProduceNegativePartitionOrCorpusTypes() {
        val result = MaterializationContract.materialize(Fixture.base().request())
        val type = result.projectedTrainingExample?.javaClass
        assertTrue(type == null || !de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1::class.java.isAssignableFrom(type))
        assertTrue(type == null || !de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1::class.java.isAssignableFrom(type))
    }

    @Test
    fun actualProductionTypesRejectMissingRawInput() {
        assertIllegalArgument { HimCandidateInputProvenance("", "value") }
    }

    @Test
    fun actualProductionTypesRejectMissingNormalizedInput() {
        assertIllegalArgument { HimCandidateInputProvenance("value", "") }
    }

    @Test
    fun actualGenerationRunRejectsUnboundOccurrence() {
        val fixture = Fixture.base()
        val unbound = fixture.occurrence.copy(inputRunReference = otherInputRun())
        assertIllegalArgument {
            HimCandidateGenerationRun(
                runReference = fixture.generationRun.runReference,
                generationMissionReference = "materialization-fixture",
                runInputSetIdentity = fixture.generationRun.runInputSetIdentity,
                state = HimCandidateGenerationRunState.COMPLETE,
                inputRuns = fixture.generationRun.inputRuns,
                occurrences = listOf(unbound),
            )
        }
    }

    @Test
    fun batchValidationRejectsDuplicateDecisionIds() {
        val batch = completed(MaterializationContract.materializeBatch(listOf(Fixture.base().request())))
        assertIllegalArgument {
            MaterializationContract.validate(batch.copy(decisions = batch.decisions + batch.decisions.single()))
        }
    }

    @Test
    fun batchValidationRejectsIncorrectCounters() {
        val batch = completed(MaterializationContract.materializeBatch(listOf(Fixture.base().request())))
        assertIllegalArgument {
            MaterializationContract.validate(batch.copy(counters = MaterializationContract.Counters(0, 0, 0)))
        }
    }

    @Test
    fun batchResultIsCompletedEvenWhenOneRecordIsNotProjectable() {
        val fixture = Fixture.base()
        val batch = completed(MaterializationContract.materializeBatch(listOf(fixture.requestOf(fixture.input(mutation = null)))))
        assertEquals(MaterializationContract.MaterializationState.NOT_YET_PROJECTABLE, batch.decisions.single().state)
    }

    private fun completed(result: MaterializationContract.BatchResult): MaterializationContract.Batch = when (result) {
        is MaterializationContract.BatchResult.Completed -> result.batch
        is MaterializationContract.BatchResult.Failed ->
            kotlin.test.fail("Unexpected ${result.reason} ${result.safeContext}")
    }

    private fun failed(result: MaterializationContract.BatchResult): MaterializationContract.BatchResult.Failed = when (result) {
        is MaterializationContract.BatchResult.Failed -> result
        is MaterializationContract.BatchResult.Completed ->
            kotlin.test.fail("Expected typed batch failure")
    }

    private fun assertNotYet(
        decision: MaterializationContract.MaterializationDecision,
        reason: MaterializationContract.FailureReason,
    ) {
        assertEquals(MaterializationContract.MaterializationState.NOT_YET_PROJECTABLE, decision.state)
        assertTrue(decision.reasons.contains(reason))
    }

    private fun assertIllegalArgument(block: () -> Unit) {
        try {
            block()
            fail("Expected fail-closed IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private fun otherCandidate() = HimCandidateReference("candidate:v1:${"e".repeat(64)}")

    private fun otherInputRun() =
        de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputRunReference(
            "input-run:v1:${"e".repeat(64)}",
        )

    private data class Fixture(
        val candidate: HimCandidateHypothesis,
        val occurrence: HimCandidateOccurrence,
        val generationRun: HimCandidateGenerationRun,
        val candidateDatasetDigest: HimSha256,
        val validation: HimCandidateValidationRecord,
        val promotion: de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionProposalV1,
        val mutation: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerEntry?,
        val promotionExecution: HimDeterministicPromotionExecutionResultV1?,
        val resultingAuthority: HimCanonicalFamilyAuthority?,
        val resultingEntityIdRegistry: HimEntityIdRegistry?,
        val groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1,
        val authorityDigestBefore: HimSha256,
        val registryDigestBefore: HimSha256,
        val evidence: List<HimCandidateEvidenceProvenance>,
    ) {
        fun input(
            occurrence: HimCandidateOccurrence = this.occurrence,
            generationRun: HimCandidateGenerationRun = this.generationRun,
            validation: HimCandidateValidationRecord = this.validation,
            promotion: de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionProposalV1 = this.promotion,
            mutation: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerEntry? = this.mutation,
            promotionExecution: HimDeterministicPromotionExecutionResultV1? = this.promotionExecution,
            resultingAuthority: HimCanonicalFamilyAuthority? = this.resultingAuthority,
            resultingEntityIdRegistry: HimEntityIdRegistry? = this.resultingEntityIdRegistry,
            groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1 = this.groundTruthReleaseReference,
            canonicalContext: List<de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext> = this.generationRun.inputRuns.single().canonicalContext,
        ) = HimGroundTruthTrainingExampleProjectionInputV1(
            candidate = candidate,
            occurrence = occurrence,
            generationRun = generationRun.copy(
                inputRuns = generationRun.inputRuns.map { it.copy(canonicalContext = canonicalContext) },
            ),
            candidateDatasetDigest = candidateDatasetDigest,
            validation = validation,
            promotion = promotion,
            mutation = mutation,
            promotionExecution = promotionExecution,
            resultingAuthority = resultingAuthority,
            resultingEntityIdRegistry = resultingEntityIdRegistry,
            groundTruthReleaseReference = groundTruthReleaseReference,
        )

        fun request() = requestOf(input())

        fun request(occurrence: HimCandidateOccurrence, generationRun: HimCandidateGenerationRun) =
            requestOf(input(occurrence = occurrence, generationRun = generationRun))

        fun requestOf(input: HimGroundTruthTrainingExampleProjectionInputV1) =
            MaterializationContract.Request(input.occurrence.occurrenceReference, input)

        companion object {
            fun base(suffix: String = "base"): Fixture {
                val relation = HimCandidateRelation.Variant(HimFamilyEntityReference.Canonical(HERRING_ID))
                val candidateTerm = "Hering eingelegt $suffix"
                val candidateReference = HimCandidateIdentityV1.candidate(candidateTerm.lowercase(), relation)
                val evidence = listOf(
                    HimCandidateEvidenceProvenance(
                        reference = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("1".repeat(64)), "off:product:$suffix"),
                        recordKind = "food",
                        retrievalRank = 1,
                        includedInProviderContext = true,
                        omittedDueToContextBudget = false,
                        himAssignedRelation = null,
                    ),
                    HimCandidateEvidenceProvenance(
                        reference = HimEvidenceReference("CIQUAL", HimSha256("2".repeat(64)), "ciqual:food:$suffix"),
                        recordKind = "food",
                        retrievalRank = 2,
                        includedInProviderContext = true,
                        omittedDueToContextBudget = false,
                        himAssignedRelation = null,
                    ),
                )
                val hypothesis = HimCandidateHypothesis(
                    candidateReference = candidateReference,
                    candidateTerm = candidateTerm,
                    normalizedCandidateTerm = candidateTerm.lowercase(),
                    relation = relation,
                    confidence = HimCandidateConfidence.HIGH,
                    evidenceOrigin = de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin.MIXED,
                    shortRationale = "Complete immutable materialization fixture.",
                )
                val inference = HimCandidateInferenceProvenance(
                    provider = "fixture-provider",
                    model = "fixture-model",
                    providerConfigurationFingerprint = HimSha256("3".repeat(64)),
                    inferenceSchema = "fixture-schema",
                    instructionPolicy = "fixture-policy",
                    contextBudgetPolicy = HimCandidateDatasetContractV2.CONTEXT_BUDGET_POLICY,
                    retrievalFoundationRelease = "fixture-retrieval-release",
                    retrievalFoundationReleaseSha256 = HimSha256("4".repeat(64)),
                    retrievalFoundationDigest = HimSha256("5".repeat(64)),
                    technicalAttemptCount = 1,
                    usage = null,
                )
                val runReference = HimCandidateIdentityV1.run("materialization-fixture-$suffix", HimSha256("6".repeat(64)), inference)
                val inputProvenance = HimCandidateInputProvenance(candidateTerm, candidateTerm.lowercase())
                val inputRunReference = HimCandidateIdentityV1.inputRun(runReference, inputProvenance)
                val context = listOf(
                    de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(
                        rank = 1,
                        canonicalId = HERRING_ID,
                        canonicalName = "Hering",
                        fullRecordCanonicalJson = "{\"canonicalId\":\"${HERRING_ID.value}\"}",
                    ),
                )
                val inputRun = HimCandidateGenerationInputRun(
                    inputRunReference = inputRunReference,
                    input = inputProvenance,
                    canonicalContext = context,
                    retrievalHistory = emptyList(),
                    finalInformationGainJudgment = null,
                    terminalState = null,
                    completionState = HimCandidateInputCompletionState.COMPLETED,
                    finalOutcome = HimCandidateFinalInferenceOutcome.SUCCESS_WITH_PERSISTED_CANDIDATE,
                    confidenceDiagnostics = HimCandidateConfidenceDiagnostics(1, 0, 0, 0),
                    knownRelations = emptyList(),
                    authorityConflicts = emptyList(),
                    persistedCandidateReferences = listOf(candidateReference),
                    inference = inference,
                    technicalFailure = null,
                )
                val occurrenceReference = HimCandidateIdentityV1.occurrence(candidateReference, runReference, inputRunReference, evidence)
                val occurrence = HimCandidateOccurrence(occurrenceReference, inputRunReference, hypothesis, evidence)
                val generationRun = HimCandidateGenerationRun(
                    runReference = runReference,
                    generationMissionReference = "materialization-fixture-$suffix",
                    runInputSetIdentity = HimSha256("6".repeat(64)),
                    state = HimCandidateGenerationRunState.COMPLETE,
                    inputRuns = listOf(inputRun),
                    occurrences = listOf(occurrence),
                )
                val candidateDatasetDigest = HimCandidateIdentityV1.datasetDigest(HimCandidateDataset(runs = listOf(generationRun)))
                val validationReference = HimCandidateValidationIdentityV1.validation(
                    candidateReference = candidateReference,
                    candidateDatasetDigest = candidateDatasetDigest,
                    decision = HimCandidateValidationDecision.APPROVE,
                    reason = HimCandidateValidationReason.SEMANTICALLY_CORRECT,
                    supersededByCandidateReference = null,
                )
                val validation = HimCandidateValidationRecord(
                    validationReference = validationReference,
                    candidateReference = candidateReference,
                    candidateDatasetDigest = candidateDatasetDigest,
                    decision = HimCandidateValidationDecision.APPROVE,
                    reason = HimCandidateValidationReason.SEMANTICALLY_CORRECT,
                    rationale = "Complete immutable materialization fixture.",
                )
                val beforeAuthority = HimCanonicalFamilyAuthority(
                    schemaVersion = "1",
                    sourceCatalog = HimCanonicalFamilySourceCatalog("catalog.json", "a".repeat(64), 1),
                    families = listOf(
                        HimCanonicalFamily(
                            canonicalId = HERRING_ID,
                            canonicalName = "Hering",
                            normalizedName = "hering",
                            taxonomyPaths = emptyList(),
                            lifecycleStatus = HimLifecycleStatus.ACTIVE,
                            identities = emptyList(),
                            variants = emptyList(),
                            aliases = emptyList(),
                        ),
                    ),
                )
                val beforeRegistry = HimEntityIdRegistry(
                    listOf(
                        HimEntityIdRegistryEntry(
                            HERRING_ID,
                            HimEntityType.CANONICAL,
                            HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED,
                            "hering",
                        ),
                    ),
                )
                val authorityDigestBefore = digest(HimCanonicalFamilyPersistence().serialize(beforeAuthority))
                val registryDigestBefore = digest(HimCanonicalFamilyPersistence().serialize(beforeRegistry))
                val eligibility = HimCandidatePromotionEligibilityResultV1(
                    candidateReference = candidateReference,
                    status = HimCandidatePromotionEligibilityStatus.PROMOTION_ELIGIBLE,
                    validationReference = validationReference,
                )
                val promotion = HimCandidatePromotionProposalFactoryV1.create(
                    candidateReference = candidateReference,
                    candidateRelation = relation,
                    candidateTerm = candidateTerm,
                    candidateDatasetDigest = candidateDatasetDigest,
                    authorityDigestBefore = authorityDigestBefore,
                    entityIdRegistryDigestBefore = registryDigestBefore,
                    eligibility = eligibility,
                )
                val entityId = HimEntityId("bvoJLp")
                val mutationResult = HimCanonicalFamilyChildMutationV1().apply(
                    authorityBefore = beforeAuthority,
                    registryBefore = beforeRegistry,
                    promotion = promotion,
                    validationReference = HimValidationReference(validationReference.value),
                    newEntityId = entityId,
                    authoritySha256Before = authorityDigestBefore,
                    registrySha256Before = registryDigestBefore,
                )
                val mutation = mutationResult.mutationLedgerEntry
                val release = HimGroundTruthReleaseIdentityV1("release:v1:${"b".repeat(64)}")
                val execution = HimDeterministicPromotionExecutionResultV1(
                    previousReleaseReference = HimGroundTruthReleaseIdentityV1("release:v1:${"a".repeat(64)}"),
                    newReleaseReference = release,
                    newEntityId = entityId,
                    mutationReference = mutation.mutationReference,
                    promotionType = promotion.target.promotionType,
                    targetType = mutation.entityType,
                    createdNewRelease = true,
                    activeReleaseVerified = true,
                )
                return Fixture(
                    candidate = hypothesis,
                    occurrence = occurrence,
                    generationRun = generationRun,
                    candidateDatasetDigest = candidateDatasetDigest,
                    validation = validation,
                    promotion = promotion,
                    mutation = mutation,
                    promotionExecution = execution,
                    resultingAuthority = mutationResult.authorityAfter,
                    resultingEntityIdRegistry = mutationResult.registryAfter,
                    groundTruthReleaseReference = release,
                    authorityDigestBefore = authorityDigestBefore,
                    registryDigestBefore = registryDigestBefore,
                    evidence = evidence,
                )
            }

            private fun digest(bytes: ByteArray) = HimSha256(
                MessageDigest.getInstance("SHA-256")
                    .digest(bytes)
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) },
            )
        }
    }

    private companion object {
        val HERRING_ID = HimEntityId("OzlByp")
    }
}
