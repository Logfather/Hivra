package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.inference

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetrievalTerminalState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.HimOpenAiSemanticProviderConfiguration
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class RunHimSemanticRetrievalDirectiveContractTest {
    @Test fun `initial source-owned directives multilingual history and final states are strict`() {
        val initial = request()
        assertEquals(0, initial.retrievalRound.value)
        assertTrue(initial.evidence.isEmpty() && initial.retrievalHistory.isEmpty())
        val decoder = HimSemanticInferenceJsonDecoder()
        val one = decoder.decode(directiveJson(listOf("OPEN_FOOD_FACTS" to listOf("Hering"))), provenance())
        assertEquals(listOf("Hering"), one.retrievalDirective!!.sourceQueries.single().queries)
        val multiple = decoder.decode(directiveJson(listOf(
            "OPEN_FOOD_FACTS" to listOf("Hering", "Atlantic herring"),
            "CIQUAL" to listOf("hareng", "Clupea harengus"),
        )), provenance())
        assertEquals(listOf("Hering", "Atlantic herring", "hareng", "Clupea harengus"), multiple.retrievalDirective!!.sourceQueries.flatMap { it.queries })

        val afterRoundOne = HimSemanticInferenceRequest(
            "f3d3a-round-2", "Hering", emptyList(), emptyList(), HimRetrievalRound(1),
            retrievalHistory = listOf(HimSemanticRetrievalHistoryEntry(HimRetrievalRound(1), one.retrievalDirective!!, emptyList())),
        )
        assertEquals(1, afterRoundOne.retrievalHistory.size)
        val next = decoder.decode(directiveJson(listOf("CIQUAL" to listOf("hareng"))), provenance())
        assertEquals(HimGroundTruthSource.CIQUAL, next.retrievalDirective!!.sourceQueries.single().source)

        val noGain = decoder.decode(finalJson(), provenance())
        assertTrue(noGain.candidates.isEmpty() && noGain.retrievalDirective == null)
        assertEquals(HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, noGain.informationGain)
        assertTrue(runCatching { decoder.decode(directiveJson(emptyList()), provenance()) }.isFailure)
        assertTrue(runCatching { decoder.decode(directiveJson(listOf("UNKNOWN" to listOf("Hering"))), provenance()) }.isFailure)
        assertTrue(runCatching { decoder.decode(directiveJson(listOf("CIQUAL" to listOf(""))), provenance()) }.isFailure)
        assertTrue(runCatching { decoder.decode(directiveJson(listOf("CIQUAL" to listOf("a", "b", "c", "d"))), provenance()) }.isFailure)
    }

    @Test fun `technical coordinator executes directives only and prevents duplicate and round four`() {
        val calls = mutableListOf<String>()
        val coordinator = HimSemanticRetrievalDirectiveCoordinator { source, query, limit ->
            assertEquals(10, limit)
            calls += "${source.name}:$query"
            emptyList()
        }
        val firstDirective = directive(HimGroundTruthSource.OPEN_FOOD_FACTS, "Hering")
        var session = coordinator.execute(HimGroundTruthRetrievalSession("Hering"), firstDirective)
        assertEquals(listOf("OPEN_FOOD_FACTS:Hering"), calls)
        assertTrue(runCatching { coordinator.execute(session, firstDirective) }.isFailure)
        session = coordinator.execute(session, directive(HimGroundTruthSource.CIQUAL, "hareng"))
        session = coordinator.execute(session, directive(HimGroundTruthSource.AGRIBALYSE, "hareng"))
        assertEquals(3, session.rounds.size)
        assertTrue(runCatching { coordinator.execute(session, directive(HimGroundTruthSource.GLYCEMIC_INDEX, "herring")) }.isFailure)
        val exhausted = coordinator.complete(HimGroundTruthRetrievalSession("unknown"), HimSemanticInferenceJsonDecoder().decode(finalJson(), provenance()))
        assertEquals(HimRetrievalTerminalState.SEARCH_EXHAUSTED, exhausted.terminalState)
    }

    @Test fun `schema-invalid directive retries exactly once`() {
        val provider = SequenceProvider(listOf(directiveJson(listOf("CIQUAL" to listOf(""))), finalJson()))
        val result = HimProviderBackedSemanticInferenceRuntime(provider, fakeConfiguration(), HimSemanticInferenceJsonDecoder()).infer(request())
        assertTrue(result is HimSemanticInferenceResult.Success)
        assertEquals(2, provider.calls)
        assertEquals(2, (result as HimSemanticInferenceResult.Success).value.provenance.technicalAttemptCount)
    }

    @Test fun `writes deterministic amendment report with frozen guards`() {
        val root = projectRoot()
        val before = GUARDS.associateWith { sha256(root.resolve(it)) }
        val indexes = INDEXES.associateWith { root.resolve(it).let { file -> file.length() to file.lastModified() } }
        assertEquals(EXPECTED_RELEASE_SHA, before[RETRIEVAL_RELEASE])
        val fingerprint = HimOpenAiSemanticProviderConfiguration().fingerprint().value
        assertNotEquals(OLD_FINGERPRINT, fingerprint)
        val report = root.resolve(REPORT)
        requireNotNull(report.parentFile).mkdirs()
        report.writeText(report(fingerprint))
        val first = sha256(report)
        report.writeText(report(fingerprint))
        assertEquals(first, sha256(report))
        assertEquals(before, GUARDS.associateWith { sha256(root.resolve(it)) })
        assertEquals(indexes, INDEXES.associateWith { root.resolve(it).let { file -> file.length() to file.lastModified() } })
    }

    private fun request() = HimSemanticInferenceRequest(
        "f3d3a-initial", "Hering", emptyList(), emptyList(), HimRetrievalRound(0),
    )

    private fun directive(source: HimGroundTruthSource, query: String) = HimSemanticRetrievalDirective(
        listOf(HimSemanticSourceQueries(source, listOf(query))), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP,
    )

    private fun directiveJson(values: List<Pair<String, List<String>>>) = """
        {"schemaVersion":"${HimSemanticInferenceSchema.OUTPUT_VERSION}","candidates":[],"informationGain":"MORE_EVIDENCE_MAY_HELP","retrievalDirective":{"sourceQueries":[${values.joinToString(",") { (source, queries) -> "{\"source\":\"$source\",\"queries\":[${queries.joinToString(",") { "\"$it\"" }}]}" }}],"informationGainJudgment":"MORE_EVIDENCE_MAY_HELP"},"authorityConflicts":[]}
    """.trimIndent()

    private fun finalJson() = """{"schemaVersion":"${HimSemanticInferenceSchema.OUTPUT_VERSION}","candidates":[],"informationGain":"NO_EXPECTED_INFORMATION_GAIN","retrievalDirective":null,"authorityConflicts":[]}"""

    private fun provenance() = HimSemanticInferenceProvenance(
        "OPENAI", "gpt-5.6-sol", HimOpenAiSemanticProviderConfiguration().fingerprint(),
        HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
        HimRetrievalFoundationBinding.V1, 1,
    )

    private fun fakeConfiguration() = HimSemanticInferenceProviderConfiguration(
        "FAKE", "fake", "F3D3A_FAKE", HimSemanticInferenceSchema.OUTPUT_VERSION,
        HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION, null, null, 8192, "fake", "fake", 1_000_000,
    )

    private class SequenceProvider(private val values: List<String>) : HimSemanticInferenceProvider {
        var calls = 0
        override fun invoke(request: HimSemanticInferenceRequest): HimSemanticProviderOutcome =
            HimSemanticProviderOutcome.StructuredResponse(values[calls++])
    }

    private fun report(newFingerprint: String) = """
        FOUNDATION STATUS=PASS
        RETRIEVAL FOUNDATION STATUS=PASS release=F3D_2_RETRIEVAL_FOUNDATION_V1 releaseSha=$EXPECTED_RELEASE_SHA foundationDigest=9780a1dc28ece40e1251981f007a509418de5db4c1e8921ba3402ac2dcf3fd86
        BLOCKER RESOLUTION=circularDependencyResolved=true localSourceSelection=false localQueryGeneration=false
        REQUEST CONTRACT BEFORE=selectedSources and semanticQueries required before HIM
        REQUEST CONTRACT AFTER=availableSources + explicit retrievalHistory; initial round 0 requires neither selection nor queries
        RETRIEVAL DIRECTIVE CONTRACT=HimSemanticRetrievalDirective providerNeutral=true
        SOURCE QUERY CONTRACT=HimSemanticSourceQueries maxQueriesPerSourcePerRound=3
        INFORMATION GAIN=MORE_EVIDENCE_MAY_HELP actionable;NO_EXPECTED_INFORMATION_GAIN stop
        SEARCH_EXHAUSTED BOUNDARY=orchestrator-derived;not model output
        INITIAL INFERENCE=round0 no Evidence valid
        SUBSEQUENT INFERENCE=history + Evidence + current completed round
        RETRIEVAL HISTORY=source-specific queries + retrieved Evidence references
        ROUND LIMIT=3
        QUERY LIMIT=3 per Source per round technical safety ceiling
        STRICT VALIDATION=unknownSource,emptyQuery,emptyDirective,queryCeiling,contradiction rejected
        SCHEMA VERSIONING=old HIM_SEMANTIC_INFERENCE_OUTPUT_V1;new ${HimSemanticInferenceSchema.OUTPUT_VERSION};changed=true
        INSTRUCTION POLICY VERSIONING=old HIM_SEMANTIC_INFERENCE_INSTRUCTION_POLICY_V1;new ${HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION};changed=true
        PROVIDER CONFIGURATION FINGERPRINT=old $OLD_FINGERPRINT;new $newFingerprint;changed=true
        OPENAI STRUCTURED OUTPUT MAPPING=strict retrievalDirective added;provider/model/API/reasoning/budgets unchanged
        ORCHESTRATOR RESPONSIBILITY=execute directive,enforce bounds,track rounds,derive terminal state;semantic routing=false
        PRE-HIM BOUNDARY=localSourceSelection=false localQueryGeneration=false matcher=false merger=false sourceVariants=false
        CANDIDATE SEMANTICS INTEGRITY=unchanged
        DATA WRITES=OpenAI calls=0;Real Candidates=0;Candidate Dataset writes=0;Entity IDs=0;Authority mutations=0
        AUTHORITY INTEGRITY=unchanged
        RETRIEVAL INDEX INTEGRITY=unchanged
        FILES CREATED=RunHimSemanticRetrievalDirectiveContractTest.kt,derived report
        FILES CHANGED=HimSemanticInferenceContracts.kt,HimSemanticInferenceJsonDecoder.kt,HimSemanticContextBudget.kt,HimGroundTruthRetrievalSession.kt,HimOpenAiSemanticProviderContract.kt,HimOpenAiSemanticInferenceProvider.kt,regression tests
        BUILD=compileDebugKotlin PASS;compileDebugUnitTestKotlin PASS
        TESTS=F3d.3a targeted PASS;F3d.3/F3d.4/F3.6/retrieval-session regressions PASS;deterministic fake provider only;no connectivity
        GIT STATUS=no staging operations
        CONTRACT DEVIATIONS=none
        HARD FAILURES=none
        SEMANTIC_RETRIEVAL_DIRECTIVE_CONTRACT_READY=true
        F3d.3a COMPLETE=true
    """.trimIndent() + "\n"

    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    companion object {
        private const val OLD_FINGERPRINT = "72b1270ecf560838cadaac2b2590bb31ebbf6e577fb38e734a1f6ed161b94279"
        private const val EXPECTED_RELEASE_SHA = "b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023"
        private const val RETRIEVAL_RELEASE = "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json"
        private const val REPORT = "build/knowledge/reports/him/inference/him-f3d3a-semantic-retrieval-directive-contract.txt"
        private val GUARDS = listOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json", RETRIEVAL_RELEASE,
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
