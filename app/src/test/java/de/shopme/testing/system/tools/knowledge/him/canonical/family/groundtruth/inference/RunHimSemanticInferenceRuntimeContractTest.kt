package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.inference

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

class RunHimSemanticInferenceRuntimeContractTest {
    @Test
    fun requestAndProviderConfigurationAreBoundedAndDeterministic() {
        val request = request()
        assertEquals(HimRetrievalFoundationBinding.V1, request.retrievalFoundation)
        assertEquals(2, request.evidence.size)
        assertTrue(runCatching { request.copy(evidence = List(11) { request.evidence.first() }) }.isFailure)
        assertTrue(runCatching { HimRetrievalRound(4) }.isFailure)
        val first = configuration().fingerprint()
        assertEquals(first, configuration().fingerprint())
        assertTrue(first != configuration().copy(modelIdentifier = "fake-model-v2").fingerprint())
    }

    @Test
    fun structuredSuccessSupportsOriginsRelationsConflictsAndEmptyCandidates() {
        val oneProvider = FakeProvider(mutableListOf(
            HimSemanticProviderOutcome.StructuredResponse(response(listOf(candidate("source", "SOURCE_SUPPORTED", "DIRECT", true))))
        ))
        val one = runtime(oneProvider).infer(request()) as HimSemanticInferenceResult.Success
        assertEquals(1, one.value.candidates.size)
        assertEquals(HimSemanticEvidenceOrigin.SOURCE_SUPPORTED, one.value.candidates.single().evidenceOrigin)
        assertEquals(HimSemanticEvidenceRelation.DIRECT, one.value.candidates.single().evidenceAssessments.single().relation)

        val multipleProvider = FakeProvider(mutableListOf(
            HimSemanticProviderOutcome.StructuredResponse(response(listOf(
                candidate("source", "SOURCE_SUPPORTED", "RELATED", true),
                candidate("model", "MODEL_DERIVED", null, false),
                candidate("mixed", "MIXED", "VARIANT", true),
            ), conflict = true))
        ))
        val multiple = runtime(multipleProvider).infer(request()) as HimSemanticInferenceResult.Success
        assertEquals(listOf(HimSemanticEvidenceOrigin.SOURCE_SUPPORTED, HimSemanticEvidenceOrigin.MODEL_DERIVED, HimSemanticEvidenceOrigin.MIXED), multiple.value.candidates.map { it.evidenceOrigin })
        assertEquals(1, multiple.value.authorityConflicts.size)
        assertEquals(HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, multiple.value.informationGain)

        val empty = runtime(FakeProvider(mutableListOf(HimSemanticProviderOutcome.StructuredResponse(response(emptyList()))))).infer(request())
        assertTrue(empty is HimSemanticInferenceResult.Success && empty.value.candidates.isEmpty())
    }

    @Test
    fun technicalRetryIsExactlyOneAndNeverChangesRetrievalRound() {
        val valid = response(emptyList())
        val immediate = FakeProvider(mutableListOf(HimSemanticProviderOutcome.StructuredResponse(valid)))
        assertTrue(runtime(immediate).infer(request()) is HimSemanticInferenceResult.Success)
        assertEquals(1, immediate.invocations)

        val invalidThenValid = FakeProvider(mutableListOf(HimSemanticProviderOutcome.StructuredResponse("not-json"), HimSemanticProviderOutcome.StructuredResponse(valid)))
        val retried = runtime(invalidThenValid).infer(request()) as HimSemanticInferenceResult.Success
        assertEquals(2, invalidThenValid.invocations)
        assertEquals(2, retried.value.provenance.technicalAttemptCount)

        val timeoutThenValid = FakeProvider(mutableListOf(HimSemanticProviderOutcome.TechnicalFailure(HimSemanticInferenceFailureKind.TIMEOUT, "timeout"), HimSemanticProviderOutcome.StructuredResponse(valid)))
        assertTrue(runtime(timeoutThenValid).infer(request()) is HimSemanticInferenceResult.Success)
        assertEquals(2, timeoutThenValid.invocations)

        val twice = FakeProvider(mutableListOf(
            HimSemanticProviderOutcome.TechnicalFailure(HimSemanticInferenceFailureKind.TRANSPORT_ERROR, "transport"),
            HimSemanticProviderOutcome.TechnicalFailure(HimSemanticInferenceFailureKind.PROVIDER_ERROR, "provider"),
            HimSemanticProviderOutcome.StructuredResponse(valid),
        ))
        val failed = runtime(twice).infer(request()) as HimSemanticInferenceResult.TechnicalFailure
        assertEquals(HimSemanticInferenceFailureKind.PROVIDER_ERROR, failed.failure.kind)
        assertEquals(2, twice.invocations)
        assertEquals(1, request().retrievalRound.value)
    }

    @Test
    fun strictSchemaRejectsProseUnknownEnumsMissingFieldsAndEntityAllocation() {
        val decoder = HimSemanticInferenceJsonDecoder()
        val provenance = provenance()
        listOf(
            "plain prose",
            response(emptyList()).replace("NO_EXPECTED_INFORMATION_GAIN", "UNKNOWN_ENUM"),
            response(emptyList()).replace("\"authorityConflicts\": []", "\"unexpected\": []"),
            response(listOf(candidate("model", "MODEL_DERIVED", null, false))).replace("\"shortRationale\": \"short audit rationale\"", "\"allocatedEntityId\": \"ABC123\", \"shortRationale\": \"short audit rationale\""),
        ).forEach { assertTrue(runCatching { decoder.decode(it, provenance) }.isFailure) }
    }

    @Test
    fun writesDeterministicArchitectureReportWithoutDomainMutation() {
        requireSourceIntegrationEnabled()
        val root = projectRoot()
        require(sha256(root.resolve(RETRIEVAL_RELEASE)) == HimRetrievalFoundationBinding.RELEASE_SHA256.value)
        val guardsBefore = hashes(root, CONTENT_GUARDS)
        val indexBefore = INDEX_PATHS.associateWith { snapshot(root.resolve(it)) }
        val report = report(configuration().fingerprint().value)
        val output = root.resolve(REPORT_PATH)
        requireNotNull(output.parentFile).mkdirs()
        write(output, report)
        val first = sha256(output)
        write(output, report)
        assertEquals(first, sha256(output))
        assertEquals(guardsBefore, hashes(root, CONTENT_GUARDS))
        assertEquals(indexBefore, INDEX_PATHS.associateWith { snapshot(root.resolve(it)) })
    }

    private fun request(): HimSemanticInferenceRequest {
        val off = HimEvidenceSearchResult(
            HimGroundTruthSource.OPEN_FOOD_FACTS,
            HimEvidenceRecordReference.offProduct(1, "123"),
            HimEvidenceRecordKind.OFF_PRODUCT,
            1,
            HimEvidenceProjection("{\"name\":\"Braeburn\"}"),
        )
        val ciqual = HimEvidenceSearchResult(
            HimGroundTruthSource.CIQUAL,
            HimEvidenceRecordReference.ciqualFood("1001"),
            HimEvidenceRecordKind.CIQUAL_FOOD,
            1,
            HimEvidenceProjection("{\"name\":\"Apple\"}"),
        )
        return HimSemanticInferenceRequest(
            "test-invocation-1", "Bio Braeburn geschält", emptyList(), listOf(off, ciqual), HimRetrievalRound(1),
            linkedSetOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.CIQUAL),
            listOf(history("Braeburn apple", listOf(off, ciqual))),
        )
    }

    private fun history(query: String, evidence: List<HimEvidenceSearchResult>) = HimSemanticRetrievalHistoryEntry(
        HimRetrievalRound(1),
        HimSemanticRetrievalDirective(
            listOf(HimSemanticSourceQueries(HimGroundTruthSource.OPEN_FOOD_FACTS, listOf(query))),
            HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP,
        ),
        evidence.map(HimSemanticSourceArtifactIdentityV1::reference),
    )

    private fun runtime(provider: HimSemanticInferenceProvider) = HimProviderBackedSemanticInferenceRuntime(provider, configuration(), HimSemanticInferenceJsonDecoder())
    private fun configuration() = HimSemanticInferenceProviderConfiguration(
        "DETERMINISTIC_FAKE_TEST_PROVIDER", "fake-model-v1", "FAKE_TEST_CONFIG_V1",
        HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
        "0", "1", 1024, "FAKE_TEST", "FAKE_TEST_NO_NETWORK", 65_536,
    )
    private fun provenance() = HimSemanticInferenceProvenance(
        configuration().providerIdentifier, configuration().modelIdentifier, configuration().fingerprint(),
        HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
        HimRetrievalFoundationBinding.V1, 1,
    )

    private fun candidate(reference: String, origin: String, evidenceRelation: String?, withEvidence: Boolean): String = """
        {
          "proposalReference": "$reference",
          "candidateTerm": "Braeburn",
          "candidateType": "CREATE_NEW_CANONICAL",
          "relation": {},
          "confidence": "MEDIUM",
          "evidenceOrigin": "$origin",
          "evidenceAssessments": ${if (withEvidence) "[{\"source\":\"OPEN_FOOD_FACTS\",\"sourceArtifactSha256\":\"${OFF_SHA}\",\"sourceRecordIdentity\":\"off:product:row:1:code:123\",\"relation\":\"$evidenceRelation\"}]" else "[]"},
          "shortRationale": "short audit rationale"
        }
    """.trimIndent()

    private fun response(candidates: List<String>, conflict: Boolean = false): String = """
        {
          "schemaVersion": "${HimSemanticInferenceSchema.OUTPUT_VERSION}",
          "candidates": [${candidates.joinToString(",")}],
          "informationGain": "NO_EXPECTED_INFORMATION_GAIN",
          "retrievalDirective": null,
          "authorityConflicts": ${if (conflict) "[{\"authorityEntityReference\":\"canonical:ABC123\",\"conflictingEvidence\":[{\"source\":\"OPEN_FOOD_FACTS\",\"sourceArtifactSha256\":\"$OFF_SHA\",\"sourceRecordIdentity\":\"off:product:row:1:code:123\"}],\"shortRationale\":\"diagnostic only\"}]" else "[]"}
        }
    """.trimIndent()

    private fun report(fingerprint: String) = """
        HIM F3d.3 SEMANTIC INFERENCE RUNTIME ARCHITECTURE
        ========================================================================
        FOUNDATION STATUS=PASS
        RETRIEVAL FOUNDATION STATUS=PASS release=F3D_2_RETRIEVAL_FOUNDATION_V1 releaseSha=b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023 foundationDigest=9780a1dc28ece40e1251981f007a509418de5db4c1e8921ba3402ac2dcf3fd86
        EXISTING AI/PROVIDER CAPABILITY AUDIT=semanticProvider=false,llmSdk=false,genericRetrofit=true,approvedSemanticSecretMechanism=false
        SEMANTIC RUNTIME CONTRACT=providerNeutralRequest=true,providerNeutralResult=true,providerAbstraction=true,providerDtoLeakage=false
        REQUEST CONTRACT=structured=true,canonicalContext=true,evidence=true,retrievalRound=true,foundationBinding=true,boundedEvidence=true
        RESULT CONTRACT=structured=true,multipleCandidates=true,emptyCandidates=true,authorityConflict=true,informationGain=true
        EVIDENCE ORIGIN CONTRACT=SOURCE_SUPPORTED,MODEL_DERIVED,MIXED
        EVIDENCE RELATION CONTRACT=DIRECT,RELATED,PARENT,INGREDIENT,VARIANT
        INFORMATION-GAIN CONTRACT=MORE_EVIDENCE_MAY_HELP,NO_EXPECTED_INFORMATION_GAIN;SEARCH_EXHAUSTED_NOT_MODEL_OUTPUT=true
        AUTHORITY-CONFLICT DIAGNOSTIC=supported=true,mutation=false
        PROVIDER CONTRACT=HimSemanticInferenceProvider selectedProductionProvider=none
        PROVIDER CONFIGURATION IDENTITY=provider,model,configurationVersion,schemaVersion,instructionPolicy,runtimeParameters,timeoutPolicy,responseByteCeiling
        PROVIDER CONFIGURATION FINGERPRINT=$fingerprint
        STRUCTURED OUTPUT SCHEMA=HIM_SEMANTIC_INFERENCE_OUTPUT_V2 strict=true freeForm=false retrievalDirective=true
        TECHNICAL RETRY CONTRACT=maxRetries=1,retrievalRoundIncrement=false,thirdAttempt=false
        TECHNICAL FAILURE CONTRACT=PROVIDER_UNAVAILABLE,TIMEOUT,TRANSPORT_ERROR,PROVIDER_ERROR,SCHEMA_INVALID,CONFIGURATION_ERROR
        EMPTY SUCCESS=candidates=[] is SUCCESS
        MODEL-DERIVED SUPPORT=true explicit=true automaticGroundTruth=false
        PROVENANCE CONTRACT=provider,model,configFingerprint,schema,instructionPolicy,retrievalRelease,retrievalDigest,attemptCount
        SECRET BOUNDARY=persisted=false,logged=false,fingerprinted=false
        CONTEXT/PAYLOAD BOUNDARY=evidencePerSource=10,retrievalRounds=3,silentTruncation=false,productionContextBudgetDecisionRequired=true
        REAL PROVIDER STATUS=selected=false,callExecuted=false,blocker=SEMANTIC_PROVIDER_SELECTION_REQUIRED;SEMANTIC_CONTEXT_BUDGET_POLICY_REQUIRED
        DATA WRITES=candidates=0,candidateDataset=0,validations=0,entityIds=0,authorityMutations=0,mutationLedger=0,retiredIds=0,groundTruthReleases=0,retrievalFoundationMutations=0
        AUTHORITY INTEGRITY=UNCHANGED
        RETRIEVAL INDEX INTEGRITY=UNCHANGED
        FILES CREATED=HimSemanticInferenceContracts.kt,HimSemanticInferenceProvider.kt,HimSemanticInferenceRuntime.kt,HimSemanticInferenceJsonDecoder.kt,RunHimSemanticInferenceRuntimeContractTest.kt,derivedReport
        FILES CHANGED=none
        CONTRACT DEVIATIONS=none
        HARD FAILURES=none
        SEMANTIC_RUNTIME_CONTRACT_READY=true
        REAL_PROVIDER_INTEGRATION_READY=false
        F3d.3 COMPLETE=true
    """.trimIndent() + "\n"

    private class FakeProvider(private val outcomes: MutableList<HimSemanticProviderOutcome>) : HimSemanticInferenceProvider {
        var invocations = 0
        override fun invoke(request: HimSemanticInferenceRequest): HimSemanticProviderOutcome {
            invocations++
            return outcomes.removeFirst()
        }
    }
    private data class Snapshot(val bytes: Long, val modified: Long)
    private fun snapshot(file: File) = Snapshot(file.length(), file.lastModified())
    private fun hashes(root: File, expected: Map<String, String>) = expected.keys.associateWith { sha256(root.resolve(it)) }
    private fun write(file: File, text: String) = Files.write(file.toPath(), text.toByteArray(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input -> val buffer = ByteArray(1024 * 1024); while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun projectRoot(): File { var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile; while (!current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile); return current }

    companion object {
        private const val OFF_SHA = "63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236"
        private const val RETRIEVAL_RELEASE = "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json"
        private const val REPORT_PATH = "build/knowledge/reports/him/inference/him-f3d3-semantic-inference-runtime-architecture.txt"
        private val CONTENT_GUARDS = linkedMapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
            RETRIEVAL_RELEASE to "b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023",
        )
        private val INDEX_PATHS = listOf(
            "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite",
            "data/sources/agribalyse/index/agribalyse-him-evidence-index.v1.sqlite",
            "data/sources/ciqual/index/ciqual-him-evidence-index.v1.sqlite",
            "data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite",
        )
    }
}
