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
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionPolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.scaling.*
import de.shopme.tools.knowledge.him.training.teacher.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class RunHimTeacherGroundTruthGenerationPipelineV1Test {
    @Test fun `same authorized work item produces same request bytes and reference`() {
        val request = request()
        val bytes = HimTeacherGroundTruthGenerationPersistenceV1.serializeRequest(request)
        val reloaded = Files.createTempDirectory("teacher-request").resolve("request.json").toFile().also { file ->
            HimTeacherGroundTruthGenerationPersistenceV1.writeNewRequest(file, request)
        }.let(HimTeacherGroundTruthGenerationPersistenceV1::readRequest)

        assertArrayEquals(bytes, HimTeacherGroundTruthGenerationPersistenceV1.serializeRequest(reloaded))
        assertEquals(request.requestReference, reloaded.requestReference)
        assertEquals(request, reloaded)
    }

    @Test fun `evidence and context changes change request identity`() {
        val request = request()
        val changedEvidence = request.inferenceRequest.copy(evidence = listOf(evidence().copy(evidenceProjection = HimEvidenceProjection("{\"name\":\"changed\"}"))))
        val changed = rebind(request, changedEvidence)
        assertNotEquals(request.requestReference, changed.requestReference)

        val changedContext = request.inferenceRequest.copy(
            canonicalContext = listOf(HimCanonicalRetrievalResult.Compact(1, HimEntityId("OzlByp"), "Hering changed")),
        )
        val contextChanged = rebind(request, changedContext)
        assertNotEquals(request.requestReference, contextChanged.requestReference)
    }

    @Test fun `all supported Teacher classifications and confidence variants parse`() {
        val pipeline = HimTeacherGroundTruthGenerationPipelineV1()
        val outputs = listOf(
            proposal("existing", "Hering", HimTeacherSemanticRelationV1.ExistingCanonical(HimEntityId("OzlByp")), HimCandidateConfidence.HIGH),
            proposal("identity", "Atlantikhering", HimTeacherSemanticRelationV1.Identity(HimEntityId("OzlByp")), HimCandidateConfidence.MEDIUM),
            proposal("variant", "Hering geräuchert", HimTeacherSemanticRelationV1.Variant(HimFamilyEntityReference.Canonical(HimEntityId("OzlByp"))), HimCandidateConfidence.LOW),
            proposal("alias", "Sild", HimTeacherSemanticRelationV1.Alias(HimFamilyEntityReference.Canonical(HimEntityId("OzlByp"))), HimCandidateConfidence.NO_CONFIDENCE),
            proposal("new", "Seefisch neu", HimTeacherSemanticRelationV1.NewCanonical("Seefisch neu"), HimCandidateConfidence.HIGH),
        )
        outputs.forEach { value ->
            val output = HimTeacherGroundTruthOutputV1(HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION, listOf(value), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP)
            val result = pipeline.generate(plan(), request(), provider(output))
            assertEquals(value.relation.classification(), result.output.proposals.single().relation.classification())
        }
    }

    @Test fun `preserves semantic candidates when no additional information gain is expected`() {
        var runtimeInvocations = 0
        val semanticCandidate = HimSemanticCandidateProposal(
            proposalReference = "semantic-proposal-1",
            candidateTerm = "Makrelen",
            relation = HimCandidateRelation.Identity(HimEntityId("OzlByp")),
            confidence = HimCandidateConfidence.HIGH,
            evidenceOrigin = HimSemanticEvidenceOrigin.SOURCE_SUPPORTED,
            evidenceAssessments = listOf(HimSemanticEvidenceAssessment(evidenceReference(), HimSemanticEvidenceRelation.DIRECT)),
            shortRationale = "offline adapter regression fixture",
        )
        val runtime = HimSemanticInferenceRuntime {
            runtimeInvocations++
            HimSemanticInferenceResult.Success(
                HimSemanticInferenceSuccess(
                    candidates = listOf(semanticCandidate),
                    informationGain = HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN,
                    retrievalDirective = null,
                    authorityConflicts = emptyList(),
                    provenance = provenance(),
                ),
            )
        }

        val outcome = HimSemanticInferenceTeacherProviderAdapterV1(runtime).invoke(request())
        assertTrue(outcome is HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse)
        val output = HimTeacherGroundTruthOutputDecoderV1.decode((outcome as HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse).json)
        val proposal = output.proposals.single()

        assertEquals(1, runtimeInvocations)
        assertEquals(semanticCandidate.proposalReference, proposal.proposalReference)
        assertEquals(semanticCandidate.candidateTerm, proposal.candidateTerm)
        assertEquals(HimTeacherSemanticRelationV1.Identity(HimEntityId("OzlByp")), proposal.relation)
        assertEquals(semanticCandidate.confidence, proposal.confidence)
        assertEquals(semanticCandidate.evidenceOrigin, proposal.evidenceOrigin)
        assertEquals(semanticCandidate.evidenceAssessments, proposal.evidenceAssessments)
        assertEquals(semanticCandidate.shortRationale, proposal.shortRationale)
        assertEquals(HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, output.informationGain)
    }

    @Test fun `malformed schema unsupported evidence invalid scope and duplicate output hard fail`() {
        val pipeline = HimTeacherGroundTruthGenerationPipelineV1()
        assertFails { pipeline.generate(plan(), request(), rawProvider("{}")) }

        val unknownEvidence = proposal("unknown", "Hering", HimTeacherSemanticRelationV1.Identity(HimEntityId("OzlByp")), HimCandidateConfidence.HIGH, evidence = listOf(HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("f".repeat(64)), "off:product:row:99:code:missing")))
        assertFails { pipeline.generate(plan(), request(), provider(HimTeacherGroundTruthOutputV1(HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION, listOf(unknownEvidence), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP))) }

        val invalidScope = proposal("scope", "Hering eingelegt", HimTeacherSemanticRelationV1.Variant(HimFamilyEntityReference.Canonical(HimEntityId("Other1"))), HimCandidateConfidence.HIGH)
        assertFails { pipeline.generate(plan(), request(), provider(HimTeacherGroundTruthOutputV1(HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION, listOf(invalidScope), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP))) }

        val duplicate = proposal("duplicate", "Hering geräuchert", HimTeacherSemanticRelationV1.Variant(HimFamilyEntityReference.Canonical(HimEntityId("OzlByp"))), HimCandidateConfidence.HIGH)
        val duplicateOutput = HimTeacherGroundTruthOutputV1(HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION, listOf(duplicate, duplicate.copy(proposalReference = "duplicate-2")), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP)
        assertFails { pipeline.generate(plan(), request(), provider(duplicateOutput)) }
    }

    @Test fun `offline preflight persists reloads and is idempotent without production paths`() {
        val directory = Files.createTempDirectory("him-teacher-preflight").toFile()
        val output = HimTeacherGroundTruthOutputV1(HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION, listOf(proposal("variant", "Hering geräuchert", HimTeacherSemanticRelationV1.Variant(HimFamilyEntityReference.Canonical(HimEntityId("OzlByp"))), HimCandidateConfidence.HIGH)), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP)
        val result = HimTeacherGroundTruthOfflinePreflightV1.run(plan(), request(), provider(output), directory)

        assertTrue(result.ready)
        assertEquals(listOf("request-serialization-reload", "result-persistence-reload-digest", "idempotency"), result.diagnostics)
        assertEquals(HimTrainingPartitionV1.VALIDATION, HimTeacherGroundTruthGenerationPipelineV1().generate(plan(), request(), provider(output)).partition)
    }

    @Test fun `holdout work item remains holdout and paid provider is impossible without opt in`() {
        val holdoutId = listOf("Aaa333", "Bbb555").map(::HimEntityId).first { id ->
            HimTrainingPartitionPolicyV1.partitionForGroup(HimTrainingFamilyGroupReferenceV1.canonical(id)) == HimTrainingPartitionV1.HOLDOUT
        }
        val holdoutPlan = plan(holdoutId, "Holdout fish")
        val holdoutRequest = request(holdoutPlan, holdoutId, "Holdout fish")
        val output = HimTeacherGroundTruthOutputV1(HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION, emptyList(), HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN)
        assertEquals(HimTrainingPartitionV1.HOLDOUT, HimTeacherGroundTruthGenerationPipelineV1().generate(holdoutPlan, holdoutRequest, provider(output)).partition)

        var calls = 0
        val guarded = HimOptInTeacherGroundTruthProviderV1(provider(output), paidOptIn = { false }, apiKeyPresent = { calls++ > 0 })
        assertFailsAny { guarded.invoke(request()) }
        assertEquals(0, calls)
    }

    private fun provider(output: HimTeacherGroundTruthOutputV1) = HimTeacherGroundTruthProviderV1 {
        HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse(
            String(HimTeacherGroundTruthOutputCodecV1.serialize(output), Charsets.UTF_8),
            provenance(),
        )
    }

    private fun rawProvider(json: String) = HimTeacherGroundTruthProviderV1 {
        HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse(json, provenance())
    }

    private fun proposal(reference: String, term: String, relation: HimTeacherSemanticRelationV1, confidence: HimCandidateConfidence, evidence: List<HimEvidenceReference> = listOf(evidenceReference())) = HimTeacherSemanticProposalV1(
        reference, term, relation, confidence, HimSemanticEvidenceOrigin.SOURCE_SUPPORTED,
        evidence.map { HimSemanticEvidenceAssessment(it, HimSemanticEvidenceRelation.DIRECT) }, "fixture rationale",
    )

    private fun request(plan: HimCanonicalGroundTruthScalingPlanV1 = plan(), canonicalId: HimEntityId = HimEntityId("OzlByp"), term: String = "Hering"): HimTeacherGroundTruthGenerationRequestV1 {
        val context = listOf(HimCanonicalRetrievalResult.Compact(1, canonicalId, term))
        val evidence = evidence()
        val history = listOf(HimSemanticRetrievalHistoryEntry(HimRetrievalRound(1), HimSemanticRetrievalDirective(listOf(HimSemanticSourceQueries(HimGroundTruthSource.OPEN_FOOD_FACTS, listOf(term))), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP), listOf(evidenceReference())))
        val inference = HimSemanticInferenceRequest("teacher-fixture:${canonicalId.value}", term, context, listOf(evidence), HimRetrievalRound(1), setOf(HimGroundTruthSource.OPEN_FOOD_FACTS), history)
        return HimTeacherGroundTruthGenerationRequestV1.create(plan.workItems.single { it.canonicalId == canonicalId }, term, context.map { HimCandidateCanonicalContext(it.rank, it.canonicalId, it.canonicalName, null) }, inference, HimTeacherGroundTruthPolicyBindingsV1(providerConfigurationFingerprint = HimSha256("a".repeat(64)), contextBudgetPolicyVersion = HimSemanticContextBudgetPolicy.VERSION))
    }

    private fun rebind(request: HimTeacherGroundTruthGenerationRequestV1, inference: HimSemanticInferenceRequest): HimTeacherGroundTruthGenerationRequestV1 {
        val draft = request.copy(requestReference = "teacher-request:v1:${"0".repeat(64)}", inferenceRequest = inference, canonicalContext = inference.canonicalContext.map { HimCandidateCanonicalContext(it.rank, it.canonicalId, it.canonicalName, null) })
        return draft.copy(requestReference = HimTeacherGroundTruthRequestIdentityV1.reference(draft))
    }

    private fun evidence(record: String = "off:product:row:1:code:fixture") = HimEvidenceSearchResult(HimGroundTruthSource.OPEN_FOOD_FACTS, HimEvidenceRecordReference.parse(HimGroundTruthSource.OPEN_FOOD_FACTS, record), HimEvidenceRecordKind.OFF_PRODUCT, 1, HimEvidenceProjection("{\"name\":\"Hering\"}"))
    private fun evidenceReference() = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236"), "off:product:row:1:code:fixture")
    private fun provenance() = HimSemanticInferenceProvenance("OFFLINE_FAKE", "DETERMINISTIC", HimSha256("a".repeat(64)), HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION, HimRetrievalFoundationBinding.V1, 1)

    private fun plan(canonicalId: HimEntityId = HimEntityId("OzlByp"), name: String = "Hering"): HimCanonicalGroundTruthScalingPlanV1 {
        val catalog = HimProductOnlyCanonicalMaster(CATALOG_PATH, CATALOG_SHA, listOf(HimProductOnlyCanonical(name, name.lowercase(), listOf(listOf("food", name.lowercase())))))
        val family = HimCanonicalFamily(canonicalId, name, name.lowercase(), listOf(listOf("food", name.lowercase())), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList())
        val authority = HimCanonicalFamilyAuthority("1", HimCanonicalFamilySourceCatalog(CATALOG_PATH, CATALOG_SHA, 1), listOf(family))
        return HimCanonicalGroundTruthScalingPlannerV1().plan(HimCanonicalGroundTruthScalingPlannerInputV1(catalog, authority, RELEASE, candidateDatasetBinding = null, childLineage = emptyList()))
    }

    private fun assertFails(block: () -> Unit) {
        try { block(); fail("Expected hard failure") } catch (_: IllegalArgumentException) { }
    }

    private fun assertFailsAny(block: () -> Unit) {
        try { block(); fail("Expected hard failure") } catch (_: RuntimeException) { }
    }

    private companion object {
        const val CATALOG_PATH = "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json"
        val CATALOG_SHA = "a".repeat(64)
        val RELEASE = de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1("release:v1:${"d".repeat(64)}")
    }
}
