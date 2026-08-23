package de.shopme.testing.system.tools.knowledge.him.training.teacher

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.HimOpenAiSemanticProviderConfiguration
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionPolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.scaling.*
import de.shopme.tools.knowledge.him.training.teacher.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class RunHimTeacherPaidPilotOfflinePreflightV1Test {
    @Test fun `full offline preflight binds current implementation and writes only derived artifact`() {
        val root = projectRoot()
        val preflightDirectory = Files.createTempDirectory("teacher-paid-preflight").toFile()
        val fixturePlan = plan()
        val fixtureRequest = request(fixturePlan)
        var providerCalls = 0
        val preflight = HimTeacherGroundTruthOfflinePreflightV1.run(
            fixturePlan,
            fixtureRequest,
            fakeProvider { providerCalls++ },
            preflightDirectory,
        )

        assertTrue(preflight.ready)
        assertEquals(2, providerCalls) // initial materialization plus deterministic replay/idempotency pass
        val artifact = HimTeacherPaidPilotOfflinePreflightV1.createValidated(root, preflight)
        assertEquals(
            HimTeacherPaidPilotOfflinePreflightStatusV1.CURRENT,
            HimTeacherPaidPilotOfflinePreflightV1.evaluate(
                artifact,
                HimTeacherPaidPilotOfflinePreflightBindingsV1.current(),
                HimTeacherPaidPilotOfflinePreflightV1.implementationFingerprint(root),
            ),
        )

        val artifactFile = root.resolve(HimTeacherPaidPilotOfflinePreflightContractV1.ARTIFACT)
        HimTeacherPaidPilotOfflinePreflightV1.write(artifactFile, artifact)
        val reloaded = requireNotNull(HimTeacherPaidPilotOfflinePreflightV1.read(artifactFile))
        assertArrayEquals(HimTeacherPaidPilotOfflinePreflightV1.serialize(artifact), HimTeacherPaidPilotOfflinePreflightV1.serialize(reloaded))
        assertEquals(HimTrainingPartitionV1.VALIDATION, HimTeacherGroundTruthGenerationPipelineV1().generate(fixturePlan, fixtureRequest, fakeProvider()).partition)
    }

    @Test fun `absent invalid stale and contract-mismatched preflight block before delegate`() {
        val artifact = validArtifact()
        val currentBindings = HimTeacherPaidPilotOfflinePreflightBindingsV1.current()
        val currentImplementation = "a".repeat(64)
        val cases = listOf(
            HimTeacherPaidPilotOfflinePreflightStatusV1.ABSENT to null,
            HimTeacherPaidPilotOfflinePreflightStatusV1.INVALID to artifact.copy(logicalArtifactDigest = "bad"),
            HimTeacherPaidPilotOfflinePreflightStatusV1.STALE to artifact,
            HimTeacherPaidPilotOfflinePreflightStatusV1.CONTRACT_MISMATCH to artifact,
        )
        cases.forEach { (expected, candidate) ->
            val status = when (expected) {
                HimTeacherPaidPilotOfflinePreflightStatusV1.ABSENT -> HimTeacherPaidPilotOfflinePreflightV1.evaluate(null, currentBindings, currentImplementation)
                HimTeacherPaidPilotOfflinePreflightStatusV1.INVALID -> HimTeacherPaidPilotOfflinePreflightV1.evaluate(candidate, currentBindings, currentImplementation)
                HimTeacherPaidPilotOfflinePreflightStatusV1.STALE -> HimTeacherPaidPilotOfflinePreflightV1.evaluate(candidate, currentBindings, "b".repeat(64))
                HimTeacherPaidPilotOfflinePreflightStatusV1.CONTRACT_MISMATCH -> HimTeacherPaidPilotOfflinePreflightV1.evaluate(candidate, currentBindings.copy(partitionPolicy = "MISMATCH"), currentImplementation)
                HimTeacherPaidPilotOfflinePreflightStatusV1.CURRENT -> error("not a blocked case")
            }
            assertEquals(expected, status)
            var delegateCalls = 0
            val guarded = HimTeacherPaidPilotOfflinePreflightGuardedProviderV1(
                preflightCheck = { require(status == HimTeacherPaidPilotOfflinePreflightStatusV1.CURRENT) },
                delegate = HimTeacherGroundTruthProviderV1 { delegateCalls++; error("delegate must not run") },
            )
            assertFails { guarded.invoke(request(plan())) }
            assertEquals(0, delegateCalls)
        }
    }

    @Test fun `existing semantic runtime diagnostics remain available without provider access`() {
        val diagnostic = HimSemanticInferenceTechnicalDiagnostic(
            inputReference = "teacher-work:v1:${"a".repeat(64)}",
            inferencePhase = HimSemanticInferencePhase.AFTER_RETRIEVAL,
            retrievalRound = HimRetrievalRound(1),
            logicalInferenceCallNumber = 1,
            physicalAttemptCount = 2,
            technicalRetryCount = 1,
            finalTechnicalFailureType = HimSemanticInferenceFailureKind.RATE_LIMITED,
            completedSuccessfully = false,
            lastSuccessfulRetrievalRound = HimRetrievalRound(0),
        )
        assertEquals(1, diagnostic.technicalRetryCount)
        assertEquals(HimSemanticInferenceFailureKind.RATE_LIMITED, diagnostic.finalTechnicalFailureType)
    }

    private fun validArtifact(): HimTeacherPaidPilotOfflinePreflightArtifactV1 {
        val root = projectRoot()
        val preflight = HimTeacherGroundTruthOfflinePreflightResultV1(
            ready = true,
            diagnostics = listOf("request-serialization-reload", "result-persistence-reload-digest", "idempotency"),
            requestBytesSha256 = HimSha256("a".repeat(64)),
            resultReference = "teacher-result:v1:${"b".repeat(64)}",
        )
        return HimTeacherPaidPilotOfflinePreflightV1.createValidated(root, preflight)
    }

    private fun fakeProvider(onInvoke: () -> Unit = {}) = HimTeacherGroundTruthProviderV1 { request ->
        onInvoke()
        val evidence = request.inferenceRequest.evidence.single()
        val evidenceReference = HimSemanticSourceArtifactIdentityV1.reference(evidence)
        val output = HimTeacherGroundTruthOutputV1(
            HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION,
            listOf(
                HimTeacherSemanticProposalV1(
                    "pilot-variant",
                    "Hering geräuchert",
                    HimTeacherSemanticRelationV1.Variant(HimFamilyEntityReference.Canonical(HimEntityId("OzlByp"))),
                    HimCandidateConfidence.HIGH,
                    HimSemanticEvidenceOrigin.SOURCE_SUPPORTED,
                    listOf(HimSemanticEvidenceAssessment(evidenceReference, HimSemanticEvidenceRelation.DIRECT)),
                    "offline pilot fixture",
                ),
            ),
            HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP,
        )
        HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse(
            String(HimTeacherGroundTruthOutputCodecV1.serialize(output), Charsets.UTF_8),
            HimSemanticInferenceProvenance(
                "OPENAI_OFFLINE_FIXTURE",
                HimOpenAiSemanticProviderConfiguration().model,
                HimOpenAiSemanticProviderConfiguration().fingerprint(),
                HimSemanticInferenceSchema.OUTPUT_VERSION,
                HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
                HimRetrievalFoundationBinding.V1,
                1,
            ),
        )
    }

    private fun request(plan: HimCanonicalGroundTruthScalingPlanV1): HimTeacherGroundTruthGenerationRequestV1 {
        val context = listOf(HimCanonicalRetrievalResult.Compact(1, HimEntityId("OzlByp"), "Hering"))
        val sourceRecord = HimEvidenceRecordReference.parse(HimGroundTruthSource.OPEN_FOOD_FACTS, "off:product:row:1:code:fixture")
        val sourceEvidence = HimEvidenceSearchResult(HimGroundTruthSource.OPEN_FOOD_FACTS, sourceRecord, HimEvidenceRecordKind.OFF_PRODUCT, 1, HimEvidenceProjection("{\"name\":\"Hering\"}"))
        val history = listOf(HimSemanticRetrievalHistoryEntry(HimRetrievalRound(1), HimSemanticRetrievalDirective(listOf(HimSemanticSourceQueries(HimGroundTruthSource.OPEN_FOOD_FACTS, listOf("Hering"))), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP), listOf(HimSemanticSourceArtifactIdentityV1.reference(sourceEvidence))))
        val inference = HimSemanticInferenceRequest("teacher-paid-pilot-fixture", "Hering", context, listOf(sourceEvidence), HimRetrievalRound(1), setOf(HimGroundTruthSource.OPEN_FOOD_FACTS), history)
        return HimTeacherGroundTruthGenerationRequestV1.create(
            plan.workItems.single(),
            "Hering",
            listOf(HimCandidateCanonicalContext(1, HimEntityId("OzlByp"), "Hering", null)),
            inference,
            HimTeacherGroundTruthPolicyBindingsV1(
                providerConfigurationFingerprint = HimOpenAiSemanticProviderConfiguration().fingerprint(),
                contextBudgetPolicyVersion = HimSemanticContextBudgetPolicy.VERSION,
            ),
        )
    }

    private fun plan(): HimCanonicalGroundTruthScalingPlanV1 {
        val catalogPath = "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json"
        val catalogSha = "a".repeat(64)
        val catalog = HimProductOnlyCanonicalMaster(catalogPath, catalogSha, listOf(HimProductOnlyCanonical("Hering", "hering", listOf(listOf("food", "hering")))))
        val family = HimCanonicalFamily(HimEntityId("OzlByp"), "Hering", "hering", listOf(listOf("food", "hering")), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList())
        val authority = HimCanonicalFamilyAuthority("1", HimCanonicalFamilySourceCatalog(catalogPath, catalogSha, 1), listOf(family))
        return HimCanonicalGroundTruthScalingPlannerV1().plan(HimCanonicalGroundTruthScalingPlannerInputV1(catalog, authority, de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1("release:v1:${"d".repeat(64)}")))
    }

    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }

    private fun assertFails(block: () -> Unit) {
        try { block(); fail("Expected preflight gate failure") } catch (_: IllegalArgumentException) { }
    }
}
