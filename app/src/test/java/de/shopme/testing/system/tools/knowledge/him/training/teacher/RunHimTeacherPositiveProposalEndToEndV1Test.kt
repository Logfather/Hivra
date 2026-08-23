package de.shopme.testing.system.tools.knowledge.him.training.teacher

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimRetrievalFoundationBinding
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticCandidateProposal
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceAssessment
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceRequest
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceRuntime
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSuccess
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticContextBudgetPolicy
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticRetrievalDirective
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticRetrievalHistoryEntry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceArtifactIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceQueries
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceReferenceValidator
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceProjection
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlanV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerInputV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerV1
import de.shopme.tools.knowledge.him.training.teacher.*
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class RunHimTeacherPositiveProposalEndToEndV1Test {
    @Test
    fun `processes an evidence grounded positive proposal through persistence reload validation and an idempotent rerun`() {
        val fixture = fixture()
        val evidenceReference = HimSemanticSourceArtifactIdentityV1.reference(fixture.evidence)
        val semanticCandidate = HimSemanticCandidateProposal(
            proposalReference = "offline-positive-proposal-1",
            candidateTerm = "Atlantikhering",
            relation = HimCandidateRelation.Identity(HimEntityId("OzlByp")),
            confidence = HimCandidateConfidence.HIGH,
            evidenceOrigin = HimSemanticEvidenceOrigin.SOURCE_SUPPORTED,
            evidenceAssessments = listOf(
                HimSemanticEvidenceAssessment(evidenceReference, HimSemanticEvidenceRelation.DIRECT),
            ),
            shortRationale = "The local evidence projection identifies Atlantikhering under Hering.",
        )
        val semanticSuccess = HimSemanticInferenceSuccess(
            candidates = listOf(semanticCandidate),
            informationGain = HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN,
            retrievalDirective = null,
            authorityConflicts = emptyList(),
            provenance = fixture.provenance,
        )
        HimSemanticEvidenceReferenceValidator.validate(fixture.request.inferenceRequest, semanticSuccess)
        assertEquals("{\"canonicalName\":\"Hering\",\"identityName\":\"Atlantikhering\",\"relation\":\"IDENTITY\"}", fixture.evidence.evidenceProjection.deterministicJson)

        var runtimeInvocations = 0
        val runtime = HimSemanticInferenceRuntime { request ->
            runtimeInvocations++
            assertEquals(fixture.request.inferenceRequest, request)
            HimSemanticInferenceResult.Success(semanticSuccess)
        }
        val provider = HimSemanticInferenceTeacherProviderAdapterV1(runtime)
        val pipeline = HimTeacherGroundTruthGenerationPipelineV1()
        val first = pipeline.generate(fixture.plan, fixture.request, provider)

        HimTeacherGroundTruthRequestValidatorV1.validateAgainstPlan(fixture.plan, fixture.request)
        HimTeacherGroundTruthOutputValidatorV1.validate(fixture.request, first.output)
        assertEquals(1, first.output.proposals.size)
        assertEquals(1, first.output.proposals.single().evidenceAssessments.size)
        assertEquals(HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, first.output.informationGain)
        assertEquals(evidenceReference, first.output.proposals.single().evidenceAssessments.single().evidenceReference)
        assertEquals(HimTrainingPartitionV1.VALIDATION, first.partition)

        val resultDirectory = Files.createTempDirectory("him-positive-proposal-end-to-end").toFile()
        val resultFile = resultDirectory.resolve("positive-proposal.result.v1.json")
        HimTeacherGroundTruthGenerationPersistenceV1.writeNewResult(resultFile, first)
        val firstBytes = resultFile.readBytes()
        val reloaded = HimTeacherGroundTruthGenerationPersistenceV1.readResult(resultFile)

        HimTeacherGroundTruthOutputValidatorV1.validate(fixture.request, reloaded.output)
        assertEquals(first, reloaded)
        assertEquals(first.output.proposals.single(), reloaded.output.proposals.single())
        assertEquals(listOf(evidenceReference), reloaded.retrievalProvenance.evidenceReferences)
        assertEquals(evidenceReference, reloaded.output.proposals.single().evidenceAssessments.single().evidenceReference)
        assertEquals(HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, reloaded.output.informationGain)
        assertEquals(first.logicalDigest, reloaded.logicalDigest)
        assertEquals(1, resultDirectory.listFiles().orEmpty().size)
        assertTrue(resultFile.isFile)

        val second = pipeline.generate(fixture.plan, fixture.request, provider)
        HimTeacherGroundTruthOutputValidatorV1.validate(fixture.request, second.output)
        val idempotentResults = HimTeacherGroundTruthGenerationPersistenceV1.addIdempotent(listOf(first), second)
        val secondBytes = resultFile.readBytes()

        assertEquals(2, runtimeInvocations)
        assertEquals(1, idempotentResults.size)
        assertEquals(first, idempotentResults.single())
        assertEquals(1, idempotentResults.single().output.proposals.size)
        assertEquals(1, idempotentResults.single().output.proposals.single().evidenceAssessments.size)
        assertEquals(listOf(evidenceReference), idempotentResults.single().retrievalProvenance.evidenceReferences)
        assertEquals(first.output.proposals, second.output.proposals)
        assertEquals(first.retrievalProvenance, second.retrievalProvenance)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.resultReference, second.resultReference)
        assertArrayEquals(firstBytes, secondBytes)
        assertEquals(1, resultDirectory.listFiles().orEmpty().size)
    }

    private fun fixture(): Fixture {
        val plan = plan()
        val canonicalId = HimEntityId("OzlByp")
        val evidence = HimEvidenceSearchResult(
            source = HimGroundTruthSource.OPEN_FOOD_FACTS,
            sourceRecordReference = HimEvidenceRecordReference.offProduct(1, "positive-proposal"),
            recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
            retrievalRank = 1,
            evidenceProjection = HimEvidenceProjection("{\"canonicalName\":\"Hering\",\"identityName\":\"Atlantikhering\",\"relation\":\"IDENTITY\"}"),
        )
        val context = listOf(HimCanonicalRetrievalResult.Compact(1, canonicalId, "Hering"))
        val evidenceReference = HimSemanticSourceArtifactIdentityV1.reference(evidence)
        val history = listOf(
            HimSemanticRetrievalHistoryEntry(
                HimRetrievalRound(1),
                HimSemanticRetrievalDirective(
                    listOf(HimSemanticSourceQueries(HimGroundTruthSource.OPEN_FOOD_FACTS, listOf("Atlantikhering"))),
                    HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP,
                ),
                listOf(evidenceReference),
            ),
        )
        val inferenceRequest = HimSemanticInferenceRequest(
            invocationReference = "offline-positive-proposal-1",
            inputTerm = "Atlantikhering",
            canonicalContext = context,
            evidence = listOf(evidence),
            retrievalRound = HimRetrievalRound(1),
            availableSources = setOf(HimGroundTruthSource.OPEN_FOOD_FACTS),
            retrievalHistory = history,
        )
        val policyBindings = HimTeacherGroundTruthPolicyBindingsV1(
            providerConfigurationFingerprint = HimSha256("a".repeat(64)),
            contextBudgetPolicyVersion = HimSemanticContextBudgetPolicy.VERSION,
        )
        val request = HimTeacherGroundTruthGenerationRequestV1.create(
            workItem = plan.workItems.single { it.canonicalId == canonicalId },
            observedTerm = "Atlantikhering",
            canonicalContext = context.map { HimCandidateCanonicalContext(it.rank, it.canonicalId, it.canonicalName, null) },
            inferenceRequest = inferenceRequest,
            policyBindings = policyBindings,
        )
        return Fixture(
            plan = plan,
            request = request,
            evidence = evidence,
            provenance = HimSemanticInferenceProvenance(
                providerIdentifier = "OFFLINE_POSITIVE_PROPOSAL_FAKE",
                modelIdentifier = "DETERMINISTIC",
                providerConfigurationFingerprint = policyBindings.providerConfigurationFingerprint,
                inferenceSchemaVersion = HimSemanticInferenceSchema.OUTPUT_VERSION,
                instructionPolicyVersion = HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
                retrievalFoundation = HimRetrievalFoundationBinding.V1,
                technicalAttemptCount = 1,
            ),
        )
    }

    private fun plan(): HimCanonicalGroundTruthScalingPlanV1 {
        val catalog = HimProductOnlyCanonicalMaster(
            path = "offline/catalog/product-only.json",
            contentSha256 = "a".repeat(64),
            records = listOf(HimProductOnlyCanonical("Hering", "hering", listOf(listOf("food", "hering")))),
        )
        val family = HimCanonicalFamily(
            canonicalId = HimEntityId("OzlByp"),
            canonicalName = "Hering",
            normalizedName = "hering",
            taxonomyPaths = listOf(listOf("food", "hering")),
            lifecycleStatus = HimLifecycleStatus.ACTIVE,
            identities = emptyList(),
            variants = emptyList(),
            aliases = emptyList(),
        )
        val authority = HimCanonicalFamilyAuthority(
            schemaVersion = "1",
            sourceCatalog = HimCanonicalFamilySourceCatalog(
                path = catalog.path,
                contentSha256 = catalog.contentSha256,
                recordCount = catalog.records.size,
            ),
            families = listOf(family),
        )
        return HimCanonicalGroundTruthScalingPlannerV1().plan(
            HimCanonicalGroundTruthScalingPlannerInputV1(
                catalog = catalog,
                authority = authority,
                groundTruthReleaseReference = HimGroundTruthReleaseIdentityV1("release:v1:${"d".repeat(64)}"),
                candidateDatasetBinding = null,
                childLineage = emptyList(),
            ),
        )
    }

    private data class Fixture(
        val plan: HimCanonicalGroundTruthScalingPlanV1,
        val request: HimTeacherGroundTruthGenerationRequestV1,
        val evidence: HimEvidenceSearchResult,
        val provenance: HimSemanticInferenceProvenance,
    )
}
