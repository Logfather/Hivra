package de.shopme.testing.system.tools.knowledge.him.training.corpus

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalIdentity
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalVariant
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthMutationType
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
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateRunReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionProposalFactoryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionProposalV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionTargetV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCanonicalFamilyChildMutationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimDeterministicPromotionExecutionResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecision
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationRecord
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin
import de.shopme.tools.knowledge.him.training.corpus.HimGroundTruthTrainingExampleProjectionInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimGroundTruthTrainingExampleProjectionResultV1
import de.shopme.tools.knowledge.him.training.corpus.HimGroundTruthTrainingExampleProjectionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleLeakageValidatorV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleValidatorV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.MessageDigest

class RunHimGroundTruthTrainingExampleProjectionV1Test {

    @Test
    fun `real Hering lineage projects to variant without leakage`() {
        val source = fixture(RelationKind.VARIANT_CANONICAL)
        val example = project(source)

        assertEquals("Hering eingelegt", example.input.observedTerm)
        assertEquals("hering eingelegt", example.input.normalizedObservedTerm)
        assertEquals(HimTrainingClassificationV1.VARIANT, example.target.classification)
        assertEquals(
            HimFamilyEntityReference.Canonical(HERRING_ID),
            (example.target as HimTrainingTargetV1.Variant).scope,
        )
        assertEquals(EXPECTED_CANDIDATE_REFERENCE, example.provenance.candidateReference?.value)
        assertEquals(source.promotion.promotionReference, example.provenance.promotionReference)
        assertEquals(source.mutation!!.mutationReference, example.provenance.mutationReference)
        assertEquals(HimEntityId("bvoJLp"), example.provenance.promotedEntityId)
        assertEquals(EXPECTED_EVIDENCE, example.input.evidence.map { it.reference.sourceRecordIdentity })
        assertFalse(example.modelInput().toString().contains("bvoJLp"))
        assertFalse(example.modelInput().toString().contains("validation:v1:"))
        assertFalse(example.modelInput().toString().contains("promotion:v1:"))
        assertFalse(example.modelInput().toString().contains("mutation:v1:"))
    }

    @Test
    fun `same source lineage produces same example and reference`() {
        val first = project(fixture(RelationKind.VARIANT_CANONICAL))
        val second = project(fixture(RelationKind.VARIANT_CANONICAL))
        assertEquals(first, second)
        assertEquals(first.exampleReference, second.exampleReference)
    }

    @Test
    fun `mutation type mismatch hard fails`() {
        val source = fixture(RelationKind.VARIANT_CANONICAL)
        val invalid = source.copy(
            mutation = source.mutation!!.copy(
                mutationType = HimGroundTruthMutationType.ADD_IDENTITY,
                entityType = HimEntityType.IDENTITY,
            ),
        )
        assertFails { project(invalid) }
    }

    @Test
    fun `scope mismatch hard fails`() {
        val source = fixture(RelationKind.VARIANT_CANONICAL)
        assertFails {
            project(source.copy(mutation = source.mutation!!.copy(parent = HimFamilyEntityReference.Canonical(HimEntityId("Abc123")))))
        }
    }

    @Test
    fun `validation binding mismatch hard fails`() {
        val source = fixture(RelationKind.VARIANT_CANONICAL)
        val wrongValidation = source.validation.copy(
            validationReference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference("validation:v1:${"f".repeat(64)}"),
        )
        assertFails { project(source.copy(validation = wrongValidation)) }
    }

    @Test
    fun `candidate binding mismatch hard fails`() {
        val source = fixture(RelationKind.VARIANT_CANONICAL)
        val wrongCandidate = source.candidate.copy(candidateReference = HimCandidateReference("candidate:v1:${"e".repeat(64)}"))
        assertFails { project(source.copy(candidate = wrongCandidate)) }
    }

    @Test
    fun `resulting Entity ID mismatch hard fails`() {
        val source = fixture(RelationKind.VARIANT_CANONICAL)
        assertFails {
            project(source.copy(promotionExecution = source.promotionExecution!!.copy(newEntityId = HimEntityId("Abc123"))))
        }
    }

    @Test
    fun `candidate evidence is projected exactly once and in source order`() {
        val example = project(fixture(RelationKind.VARIANT_CANONICAL))
        assertEquals(EXPECTED_EVIDENCE.size, example.input.evidence.size)
        assertEquals(EXPECTED_EVIDENCE, example.input.evidence.map { it.reference.sourceRecordIdentity })
        assertEquals(EXPECTED_EVIDENCE.toSet(), example.provenance.sourceEvidenceReferences.map { it.sourceRecordIdentity }.toSet())
    }

    @Test
    fun `identity promotion maps to identity target and canonical scope`() {
        val example = project(fixture(RelationKind.IDENTITY))
        assertEquals(HimTrainingClassificationV1.IDENTITY, example.target.classification)
        assertEquals(HERRING_ID, (example.target as HimTrainingTargetV1.Identity).parentCanonicalId)
    }

    @Test
    fun `alias promotion maps to alias target and preserves scope`() {
        val example = project(fixture(RelationKind.ALIAS_CANONICAL))
        assertEquals(HimTrainingClassificationV1.ALIAS, example.target.classification)
        assertEquals(
            HimFamilyEntityReference.Canonical(HERRING_ID),
            (example.target as HimTrainingTargetV1.Alias).equivalentEntity,
        )
    }

    @Test
    fun `identity-scoped variant preserves identity scope`() {
        val example = project(fixture(RelationKind.VARIANT_IDENTITY))
        assertEquals(
            HimFamilyEntityReference.Identity(HERRING_ID, IDENTITY_ID),
            (example.target as HimTrainingTargetV1.Variant).scope,
        )
    }

    @Test
    fun `CREATE_CANONICAL is explicitly not yet projectable`() {
        val source = fixture(RelationKind.VARIANT_CANONICAL)
        val target = HimCandidatePromotionTargetV1.CreateCanonical("New fish")
        val reference = HimCandidatePromotionIdentityV1.promotion(
            candidateReference = source.candidate.candidateReference,
            validationReference = source.validation.validationReference,
            candidateDatasetDigest = source.candidateDatasetDigest,
            authorityDigestBefore = source.authorityDigestBefore,
            entityIdRegistryDigestBefore = source.registryDigestBefore,
            target = target,
        )
        val result = HimGroundTruthTrainingExampleProjectionV1().project(
            source.copy(promotion = source.promotion.copy(promotionReference = reference, target = target)).input(),
        )
        assertTrue(result is HimGroundTruthTrainingExampleProjectionResultV1.NotYetProjectable)
    }

    @Test
    fun `historical release mismatch hard fails`() {
        val source = fixture(RelationKind.VARIANT_CANONICAL)
        assertFails {
            project(source.copy(groundTruthReleaseReference = HimGroundTruthReleaseIdentityV1("release:v1:${"c".repeat(64)}")))
        }
    }

    @Test
    fun `every projected example passes the F3 8a validator`() {
        val example = project(fixture(RelationKind.VARIANT_CANONICAL))
        HimTrainingExampleValidatorV1.validate(example)
        HimTrainingExampleLeakageValidatorV1.validateModelInput(example.modelInput())
    }

    private fun project(source: Fixture): HimTrainingExampleV1 =
        HimGroundTruthTrainingExampleProjectionV1().projectOrThrow(source.input())

    private fun fixture(kind: RelationKind): Fixture {
        val relation = when (kind) {
            RelationKind.IDENTITY -> HimCandidateRelation.Identity(HERRING_ID)
            RelationKind.VARIANT_CANONICAL -> HimCandidateRelation.Variant(HimFamilyEntityReference.Canonical(HERRING_ID))
            RelationKind.VARIANT_IDENTITY -> HimCandidateRelation.Variant(HimFamilyEntityReference.Identity(HERRING_ID, IDENTITY_ID))
            RelationKind.ALIAS_CANONICAL -> HimCandidateRelation.Alias(HimFamilyEntityReference.Canonical(HERRING_ID))
        }
        val candidateTerm = when (kind) {
            RelationKind.IDENTITY -> "Braeburn"
            RelationKind.ALIAS_CANONICAL -> "Heringsfisch"
            else -> "Hering eingelegt"
        }
        val candidateReference = HimCandidateIdentityV1.candidate(candidateTerm.lowercase(), relation)
        val evidence = EXPECTED_EVIDENCE.mapIndexed { index, identity ->
            HimCandidateEvidenceProvenance(
                reference = evidenceReference(identity),
                recordKind = "real-pilot-evidence",
                retrievalRank = index + 1,
                includedInProviderContext = true,
                omittedDueToContextBudget = false,
                himAssignedRelation = null,
            )
        }
        val hypothesis = HimCandidateHypothesis(
            candidateReference = candidateReference,
            candidateTerm = candidateTerm,
            normalizedCandidateTerm = candidateTerm.lowercase(),
            relation = relation,
            confidence = HimCandidateConfidence.HIGH,
            evidenceOrigin = HimSemanticEvidenceOrigin.MIXED,
            shortRationale = "Validated synthetic lineage fixture.",
        )
        val inference = HimCandidateInferenceProvenance(
            provider = "OPENAI",
            model = "gpt-5.6-sol",
            providerConfigurationFingerprint = HimSha256("3".repeat(64)),
            inferenceSchema = "HIM_SEMANTIC_INFERENCE_OUTPUT_V21",
            instructionPolicy = "HIM_SEMANTIC_INSTRUCTION_POLICY_V22",
            contextBudgetPolicy = HimCandidateDatasetContractV2.CONTEXT_BUDGET_POLICY,
            retrievalFoundationRelease = "F3D_2_RETRIEVAL_FOUNDATION_V1",
            retrievalFoundationReleaseSha256 = HimSha256("4".repeat(64)),
            retrievalFoundationDigest = HimSha256("5".repeat(64)),
            technicalAttemptCount = 1,
            usage = null,
        )
        val runReference = HimCandidateIdentityV1.run("projection-fixture", HimSha256("2".repeat(64)), inference)
        val inputProvenance = HimCandidateInputProvenance(candidateTerm, candidateTerm.lowercase())
        val inputRunReference = HimCandidateIdentityV1.inputRun(runReference, inputProvenance)
        val inputRun = HimCandidateGenerationInputRun(
            inputRunReference = inputRunReference,
            input = inputProvenance,
            canonicalContext = emptyList(),
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
        val occurrenceReference = HimCandidateIdentityV1.occurrence(
            candidate = candidateReference,
            run = runReference,
            inputRun = inputRunReference,
            evidence = evidence,
        )
        val occurrence = HimCandidateOccurrence(occurrenceReference, inputRunReference, hypothesis, evidence)
        val generationRun = HimCandidateGenerationRun(
            runReference = runReference,
            generationMissionReference = "projection-fixture",
            runInputSetIdentity = HimSha256("2".repeat(64)),
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
            rationale = "Validated synthetic lineage fixture.",
        )
        val beforeAuthority = authorityBefore(kind)
        val beforeRegistry = registryBefore(kind)
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
        val entityId = entityId(kind)
        val mutation = HimCanonicalFamilyChildMutationV1().apply(
            authorityBefore = beforeAuthority,
            registryBefore = beforeRegistry,
            promotion = promotion,
            validationReference = HimValidationReference(validationReference.value),
            newEntityId = entityId,
            authoritySha256Before = authorityDigestBefore,
            registrySha256Before = registryDigestBefore,
        ).mutationLedgerEntry
        val mutationResult = HimCanonicalFamilyChildMutationV1().apply(
            authorityBefore = beforeAuthority,
            registryBefore = beforeRegistry,
            promotion = promotion,
            validationReference = HimValidationReference(validationReference.value),
            newEntityId = entityId,
            authoritySha256Before = authorityDigestBefore,
            registrySha256Before = registryDigestBefore,
        )
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
        )
    }

    private fun Fixture.input() = HimGroundTruthTrainingExampleProjectionInputV1(
        candidate = candidate,
        occurrence = occurrence,
        generationRun = generationRun,
        candidateDatasetDigest = candidateDatasetDigest,
        validation = validation,
        promotion = promotion,
        mutation = mutation,
        promotionExecution = promotionExecution,
        resultingAuthority = resultingAuthority,
        resultingEntityIdRegistry = resultingEntityIdRegistry,
        groundTruthReleaseReference = groundTruthReleaseReference,
    )

    private fun authorityBefore(kind: RelationKind): HimCanonicalFamilyAuthority {
        val identity = HimCanonicalIdentity(IDENTITY_ID, "Braeburn", "braeburn", HimLifecycleStatus.ACTIVE, emptyList(), emptyList())
        return HimCanonicalFamilyAuthority(
            schemaVersion = "1",
            sourceCatalog = HimCanonicalFamilySourceCatalog("catalog.json", "a".repeat(64), 1),
            families = listOf(
                HimCanonicalFamily(
                    canonicalId = HERRING_ID,
                    canonicalName = "Hering",
                    normalizedName = "hering",
                    taxonomyPaths = emptyList(),
                    lifecycleStatus = HimLifecycleStatus.ACTIVE,
                    identities = if (kind == RelationKind.VARIANT_IDENTITY) listOf(identity) else emptyList(),
                    variants = emptyList(),
                    aliases = emptyList(),
                ),
            ),
        )
    }

    private fun registryBefore(kind: RelationKind) = HimEntityIdRegistry(
        buildList {
            add(HimEntityIdRegistryEntry(HERRING_ID, HimEntityType.CANONICAL, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "hering"))
            if (kind == RelationKind.VARIANT_IDENTITY) {
                add(HimEntityIdRegistryEntry(IDENTITY_ID, HimEntityType.IDENTITY, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "braeburn"))
            }
        },
    )

    private fun evidenceReference(identity: String) =
        when {
            identity.startsWith("off:") -> HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("1".repeat(64)), identity)
            identity.startsWith("agribalyse:") -> HimEvidenceReference("AGRIBALYSE", HimSha256("1".repeat(64)), identity)
            else -> HimEvidenceReference("CIQUAL", HimSha256("1".repeat(64)), identity)
        }

    private fun entityId(kind: RelationKind) = when (kind) {
        RelationKind.IDENTITY -> HimEntityId("bvoJI1")
        RelationKind.VARIANT_CANONICAL -> HimEntityId("bvoJLp")
        RelationKind.VARIANT_IDENTITY -> HimEntityId("bvoJV1")
        RelationKind.ALIAS_CANONICAL -> HimEntityId("bvoJA1")
    }

    private fun digest(bytes: ByteArray) = HimSha256(
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    private fun assertFails(block: () -> Unit) {
        try {
            block()
            fail("Expected hard projection failure")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private enum class RelationKind {
        IDENTITY,
        VARIANT_CANONICAL,
        VARIANT_IDENTITY,
        ALIAS_CANONICAL,
    }

    private data class Fixture(
        val candidate: HimCandidateHypothesis,
        val occurrence: HimCandidateOccurrence,
        val generationRun: HimCandidateGenerationRun,
        val candidateDatasetDigest: HimSha256,
        val validation: HimCandidateValidationRecord,
        val promotion: HimCandidatePromotionProposalV1,
        val mutation: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerEntry?,
        val promotionExecution: HimDeterministicPromotionExecutionResultV1?,
        val resultingAuthority: HimCanonicalFamilyAuthority?,
        val resultingEntityIdRegistry: HimEntityIdRegistry?,
        val groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1,
        val authorityDigestBefore: HimSha256,
        val registryDigestBefore: HimSha256,
    )

    private companion object {
        val HERRING_ID = HimEntityId("OzlByp")
        val IDENTITY_ID = HimEntityId("Iden01")
        const val EXPECTED_CANDIDATE_REFERENCE = "candidate:v1:8df2826510366d3cacc78d81f1ad9b3d81d50ef61d7ff3b29195eaf1ac1fd2ef"
        val EXPECTED_EVIDENCE = listOf(
            "off:product:row:4530988:code:5701157480343",
            "agribalyse:row:2171:agb:26010",
            "ciqual:food:26010",
        )
    }
}
