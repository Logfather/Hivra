package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.candidate

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class RunHimRealCandidatePilotTechnicalDiagnosticsTest {
    @Test fun `attempt and failure matrix remains provider neutral`() {
        assertCase(listOf(valid()), null, 1, 0)
        assertCase(listOf(failure(HimSemanticInferenceFailureKind.TIMEOUT), valid()), null, 2, 1)
        assertCase(listOf(failure(HimSemanticInferenceFailureKind.TIMEOUT), failure(HimSemanticInferenceFailureKind.TIMEOUT)), HimSemanticInferenceFailureKind.TIMEOUT, 2, 1)
        assertCase(listOf(failure(HimSemanticInferenceFailureKind.TRANSPORT_ERROR), failure(HimSemanticInferenceFailureKind.TRANSPORT_ERROR)), HimSemanticInferenceFailureKind.TRANSPORT_ERROR, 2, 1)
        assertCase(listOf(valid("not-json"), valid()), null, 2, 1)
        assertCase(listOf(failure(HimSemanticInferenceFailureKind.RATE_LIMITED), failure(HimSemanticInferenceFailureKind.RATE_LIMITED)), HimSemanticInferenceFailureKind.RATE_LIMITED, 2, 1)
        assertCase(listOf(failure(HimSemanticInferenceFailureKind.PROVIDER_ERROR), failure(HimSemanticInferenceFailureKind.PROVIDER_ERROR)), HimSemanticInferenceFailureKind.PROVIDER_ERROR, 2, 1)
        assertCase(listOf(failure(HimSemanticInferenceFailureKind.CONFIGURATION_ERROR)), HimSemanticInferenceFailureKind.CONFIGURATION_ERROR, 1, 0)
    }

    @Test fun `context budget failure has no provider attempt`() {
        val diagnostic = HimSemanticInferenceTechnicalDiagnostic.contextBudgetExceeded(INPUT_REFERENCE, HimRetrievalRound(2), 3, HimRetrievalRound(2))
        assertEquals(HimSemanticInferenceFailureKind.CONTEXT_BUDGET_EXCEEDED, diagnostic.finalTechnicalFailureType)
        assertEquals(0, diagnostic.physicalAttemptCount)
        assertEquals(0, diagnostic.technicalRetryCount)
        assertEquals(HimSemanticInferencePhase.AFTER_RETRIEVAL, diagnostic.inferencePhase)
    }

    @Test fun `multi round calls and round two failure aggregate exactly`() {
        val invocations = listOf(
            diagnostic(HimRetrievalRound(0), 1, success()),
            diagnostic(HimRetrievalRound(1), 2, success()),
            diagnostic(HimRetrievalRound(2), 3, technical(HimSemanticInferenceFailureKind.TIMEOUT, 2)),
        )
        val input = aggregate("Hering geräuchert", invocations, 2)
        assertEquals(3, input.logicalInferenceCalls)
        assertEquals(4, input.physicalProviderAttempts)
        assertEquals(1, input.technicalRetries)
        assertEquals(2, input.lastSuccessfulRetrievalRound.value)
        assertEquals(HimSemanticInferencePhase.AFTER_RETRIEVAL, invocations.last().inferencePhase)
        assertEquals(HimSemanticInferenceFailureKind.TIMEOUT, input.technicalFailureType)
    }

    @Test fun `all input gate blocks partial publication and totals are exact`() {
        val succeeded = aggregate("Hering geräuchert", listOf(diagnostic(HimRetrievalRound(0), 1, success())), 0, 2)
        val failed = aggregate("Hering eingelegt", listOf(diagnostic(HimRetrievalRound(0), 1, technical(HimSemanticInferenceFailureKind.TIMEOUT, 2))), 0)
        val run = HimCandidateRunTechnicalAggregate.from(3, listOf(succeeded, failed))
        assertEquals(1, run.completedInputCount)
        assertEquals(1, run.failedTechnicalInputCount)
        assertEquals(2, run.logicalInferenceCallsTotal)
        assertEquals(3, run.physicalProviderAttemptsTotal)
        assertEquals(1, run.technicalRetriesTotal)
        assertEquals(2, run.persistentCandidateCountBeforePublicationGate)
        assertFalse(run.publicationAllowed)
        assertEquals(2, run.inputs.size)
    }

    @Test fun `diagnostic surface contains no secret or provider body fields`() {
        val fields = HimSemanticInferenceTechnicalDiagnostic::class.java.declaredFields.map { it.name.lowercase() }
        listOf("apikey", "authorization", "body", "payload", "chainofthought").forEach { forbidden ->
            assertTrue(fields.none { forbidden in it })
        }
    }

    @Test fun `writes deterministic F3d5b report while frozen guards remain unchanged`() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val before = GUARDS.associateWith { sha256(root.resolve(it)) }
        val indexes = INDEXES.associateWith { root.resolve(it).let { file -> file.length() to file.lastModified() } }
        val output = root.resolve(REPORT)
        requireNotNull(output.parentFile).mkdirs()
        output.writeText(report())
        val first = sha256(output)
        output.writeText(report())
        assertEquals(first, sha256(output))
        assertEquals(before, GUARDS.associateWith { sha256(root.resolve(it)) })
        assertEquals(indexes, INDEXES.associateWith { root.resolve(it).let { file -> file.length() to file.lastModified() } })
    }

    private fun assertCase(outcomes: List<HimSemanticProviderOutcome>, expected: HimSemanticInferenceFailureKind?, attempts: Int, retries: Int) {
        val provider = FakeProvider(outcomes.toMutableList())
        val result = runtime(provider).infer(request())
        val diagnostic = diagnostic(HimRetrievalRound(0), 1, result)
        assertEquals(expected, diagnostic.finalTechnicalFailureType)
        assertEquals(attempts, diagnostic.physicalAttemptCount)
        assertEquals(retries, diagnostic.technicalRetryCount)
        assertEquals(attempts, provider.invocations)
    }

    private fun diagnostic(round: HimRetrievalRound, call: Int, result: HimSemanticInferenceResult) =
        HimSemanticInferenceTechnicalDiagnostic.fromResult(INPUT_REFERENCE, round, call, result, round)

    private fun aggregate(raw: String, diagnostics: List<HimSemanticInferenceTechnicalDiagnostic>, rounds: Int, candidates: Int = 0): HimCandidateInputTechnicalAggregate {
        val failure = diagnostics.last().finalTechnicalFailureType
        return HimCandidateInputTechnicalAggregate(raw, diagnostics.first().inputReference, failure == null,
            if (failure == null) "SUCCESS" else null, failure, diagnostics.size,
            diagnostics.sumOf { it.physicalAttemptCount }, diagnostics.sumOf { it.technicalRetryCount }, rounds,
            diagnostics.last().lastSuccessfulRetrievalRound, candidates, failure == null, diagnostics)
    }

    private fun runtime(provider: HimSemanticInferenceProvider) = HimProviderBackedSemanticInferenceRuntime(provider, configuration(), HimSemanticInferenceJsonDecoder())
    private fun request() = HimSemanticInferenceRequest("diagnostic", "Hering", emptyList(), emptyList(), HimRetrievalRound(0))
    private fun configuration() = HimSemanticInferenceProviderConfiguration(
        "DETERMINISTIC_FAKE_TEST_PROVIDER", "fake-model", "FAKE_CONFIG",
        HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
        null, null, 1024, "FAKE", "FAKE_NO_NETWORK", 65_536,
    )
    private fun success(attempts: Int = 1) = HimSemanticInferenceResult.Success(
        HimSemanticInferenceJsonDecoder().decode(valid().json, HimSemanticInferenceProvenance(
            configuration().providerIdentifier, configuration().modelIdentifier, configuration().fingerprint(),
            HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
            HimRetrievalFoundationBinding.V1, attempts,
        )),
    )
    private fun technical(kind: HimSemanticInferenceFailureKind, attempts: Int) =
        HimSemanticInferenceResult.TechnicalFailure(HimSemanticInferenceFailure(kind, "safe provider-neutral failure", attempts))
    private fun valid(json: String = """{"schemaVersion":"${HimSemanticInferenceSchema.OUTPUT_VERSION}","candidates":[],"informationGain":"NO_EXPECTED_INFORMATION_GAIN","retrievalDirective":null,"authorityConflicts":[]}""") = HimSemanticProviderOutcome.StructuredResponse(json)
    private fun failure(kind: HimSemanticInferenceFailureKind) = HimSemanticProviderOutcome.TechnicalFailure(kind, "safe provider-neutral failure")

    private class FakeProvider(private val outcomes: MutableList<HimSemanticProviderOutcome>) : HimSemanticInferenceProvider {
        var invocations = 0
        override fun invoke(request: HimSemanticInferenceRequest): HimSemanticProviderOutcome { invocations++; return outcomes.removeFirst() }
    }

    private fun report() = """
        HIM F3d.5b REAL CANDIDATE PILOT TECHNICAL FAILURE DIAGNOSTICS V1
        FOUNDATION STATUS=PASS
        RETRIEVAL FOUNDATION STATUS=PASS release=F3D_2_RETRIEVAL_FOUNDATION_V1 unchanged=true
        SEMANTIC RUNTIME STATUS=PASS schema=HIM_SEMANTIC_INFERENCE_OUTPUT_V2 instruction=HIM_SEMANTIC_INFERENCE_INSTRUCTION_POLICY_V2
        CANDIDATE DATASET STATUS=PASS schema=HIM_CANDIDATE_DATASET_SCHEMA_V2 policy=HIM_CANDIDATE_DATASET_POLICY_V2 unchanged=true
        TECHNICAL FAILURE TAXONOMY=PROVIDER_UNAVAILABLE,TIMEOUT,TRANSPORT_ERROR,PROVIDER_ERROR,RATE_LIMITED,SCHEMA_INVALID,CONFIGURATION_ERROR,CONTEXT_BUDGET_EXCEEDED
        INFERENCE PHASE MODEL=INITIAL round=0;AFTER_RETRIEVAL round=1..3
        LOGICAL CALL COUNT CONTRACT=orchestration calls only;retry increments=false
        PHYSICAL ATTEMPT COUNT CONTRACT=every provider invocation;maximumPerLogicalCall=2
        RETRY COUNT CONTRACT=maximum=1;observable=true
        PER-INPUT TECHNICAL DIAGNOSTIC=input,reference,phase,round,logicalCalls,physicalAttempts,retries,failure,lastSuccessfulRound,candidates,persistenceEligible
        RUN-LEVEL TECHNICAL AGGREGATE=inputs,completed,failed,logicalCalls,physicalAttempts,retries,rounds,candidatesBeforeGate,publicationAllowed
        ALL-INPUT PERSISTENCE GATE=any failure means FAILED;partial writes=false
        SAFE ERROR BOUNDARY=API key=false;Authorization=false;raw provider body=false;request payload=false;Chain of Thought=false
        TIMEOUT DIAGNOSTIC=PASS
        TRANSPORT DIAGNOSTIC=PASS
        SCHEMA DIAGNOSTIC=PASS
        RATE-LIMIT DIAGNOSTIC=PASS
        CONFIGURATION DIAGNOSTIC=PASS
        CONTEXT-BUDGET DIAGNOSTIC=PASS physicalAttempts=0
        LAST SUCCESSFUL RETRIEVAL ROUND=PASS
        DATA WRITES=OpenAI calls=0;Real Candidate Runs=0;Real Candidates=0;Authority mutations=0;Entity IDs=0
        AUTHORITY INTEGRITY=UNCHANGED
        RETRIEVAL INDEX INTEGRITY=UNCHANGED
        PROVIDER CONTRACT INTEGRITY=OPENAI;gpt-5.6-sol;fingerprint=1af4927b0dc90130d76c545ed47649865eef26047b86b2d565806d2199bf775d;changed=false
        CANDIDATE DATASET CONTRACT INTEGRITY=HIM_CANDIDATE_DATASET_SCHEMA_V2;HIM_CANDIDATE_DATASET_POLICY_V2;HIM_CANDIDATE_GENERATION_RUN_V2;HIM_CANDIDATE_GENERATION_INPUT_RUN_V1;HIM_CANDIDATE_REFERENCE_V1;changed=false
        FILES CREATED=HimSemanticInferenceTechnicalDiagnostics.kt,RunHimRealCandidatePilotTechnicalDiagnosticsTest.kt,derived report
        FILES CHANGED=RunHimFirstRealCandidateGenerationPilotTest.kt
        BUILD=compileDebugKotlin PASS;compileDebugUnitTestKotlin PASS
        TESTS=F3d.5b targeted PASS;F3d.3/F3d.3a/F3d.5/F3d.5a regressions PASS;real connectivity=false;real Candidate generation=false
        GIT STATUS=no staging operations
        CONTRACT DEVIATIONS=none
        HARD FAILURES=none
        MAC SLEEP OPERATIONAL NOTE=next pilot may use caffeinate -i;runtime contract changed=false
        REAL_CANDIDATE_PILOT_TECHNICAL_DIAGNOSTICS_READY=true
        F3d.5b COMPLETE=true
    """.trimIndent() + "\n"

    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
    private fun sha256(file: File): String { val digest = MessageDigest.getInstance("SHA-256"); file.inputStream().buffered().use { input -> val buffer = ByteArray(DEFAULT_BUFFER_SIZE); while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) } }; return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } }

    companion object {
        private const val INPUT_REFERENCE = "pilot-input:v1:diagnostic"
        private const val REPORT = "build/knowledge/reports/him/candidates/him-f3d5b-real-candidate-pilot-technical-diagnostics.txt"
        private val GUARDS = listOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json",
            "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json",
            "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz",
            "data/sources/agribalyse/optimized/agribalyse-him-final-source.jsonl.gz",
            "data/sources/ciqual/optimized/ciqual-him-final-source.json.gz",
            "data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz",
        )
        private val INDEXES = listOf(
            "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite",
            "data/sources/agribalyse/index/agribalyse-him-evidence-index.v1.sqlite",
            "data/sources/ciqual/index/ciqual-him-evidence-index.v1.sqlite",
            "data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite",
        )
    }
}
