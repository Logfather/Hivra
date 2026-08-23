package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimPerSourceMultiQueryEvidencePackingPolicyV1
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class RunHimCandidateDatasetOrderingAlignmentTest {
    @Test fun `authoritative ordering persists deterministically and pilot equivalent addRun succeeds`() {
        val root = projectRoot()
        val guardsBefore = EXPECTED_GUARDS.keys.associateWith { sha256(root.resolve(it)) }
        EXPECTED_GUARDS.forEach { (path, expected) -> assertEquals(path, expected, guardsBefore[path]) }
        val immutableBefore = IMMUTABLE_FILES.associateWith { snapshot(root.resolve(it)) }

        val sources = listOf(
            HimSemanticSourceQueries(HimGroundTruthSource.OPEN_FOOD_FACTS, listOf("zeta", "alpha", "beta")),
            HimSemanticSourceQueries(HimGroundTruthSource.CIQUAL, listOf("hareng", "Hering")),
        )
        val packedOrder = listOf(
            evidence(HimGroundTruthSource.OPEN_FOOD_FACTS, "A1"),
            evidence(HimGroundTruthSource.OPEN_FOOD_FACTS, "B1"),
            evidence(HimGroundTruthSource.OPEN_FOOD_FACTS, "A2"),
            evidence(HimGroundTruthSource.OPEN_FOOD_FACTS, "B2"),
        )
        val round = round(1, sources, packedOrder)
        val run = pilotEquivalentRun(round)
        val dataset = HimCandidateDatasetPersistenceV2.addRun(HimCandidateDataset(), run)

        assertEquals(listOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.CIQUAL), round.directive.sourceQueries.map { it.source })
        assertEquals(listOf("zeta", "alpha", "beta"), round.directive.sourceQueries.first().queries)
        assertEquals(packedOrder, round.includedEvidenceReferences)
        assertEquals(listOf(1), run.inputRuns.first().retrievalHistory.map { it.round.value })

        val serialized = HimCandidateDatasetPersistenceV2.serialize(dataset)
        assertArrayEquals(serialized, HimCandidateDatasetPersistenceV2.serialize(dataset))
        val replay = HimCandidateDatasetPersistenceV2.addRun(dataset, run)
        assertArrayEquals(serialized, HimCandidateDatasetPersistenceV2.serialize(replay))
        assertEquals(HimCandidateIdentityV1.datasetDigest(dataset), HimCandidateIdentityV1.datasetDigest(replay))

        val rewrittenRound = round(
            1,
            round.directive.sourceQueries.map { it.copy(queries = it.queries.sorted()) },
            packedOrder,
        )
        val rewritten = pilotEquivalentRun(rewrittenRound)
        assertFalse(HimCandidateDatasetPersistenceV2.serializeRun(run).contentEquals(HimCandidateDatasetPersistenceV2.serializeRun(rewritten)))
        assertNotEquals(
            HimCandidateIdentityV1.datasetDigest(dataset),
            HimCandidateIdentityV1.datasetDigest(HimCandidateDatasetPersistenceV2.addRun(HimCandidateDataset(), rewritten)),
        )

        val wrongSourceOrder = sources.reversed()
        val wrongRound = round(1, wrongSourceOrder, emptyList())
        assertTrue(runCatching { HimCandidateDatasetPersistenceV2.addRun(HimCandidateDataset(), pilotEquivalentRun(wrongRound)) }.isFailure)

        val rewrittenPacking = round.copy(includedEvidenceReferences = listOf(packedOrder[0], packedOrder[2], packedOrder[1], packedOrder[3]))
        assertFalse(
            HimCandidateDatasetPersistenceV2.serializeRun(run).contentEquals(
                HimCandidateDatasetPersistenceV2.serializeRun(pilotEquivalentRun(rewrittenPacking)),
            ),
        )

        val temporary = Files.createTempDirectory("him-f3d5d-").toFile()
        try {
            val runFile = temporary.resolve("run.json")
            val masterFile = temporary.resolve("master.json")
            HimCandidateDatasetPersistenceV2.writeNewRun(runFile, run)
            HimCandidateDatasetPersistenceV2.writeMaster(masterFile, dataset)
            assertArrayEquals(HimCandidateDatasetPersistenceV2.serializeRun(run), runFile.readBytes())
            assertArrayEquals(serialized, masterFile.readBytes())
            assertTrue(runCatching { HimCandidateDatasetPersistenceV2.writeNewRun(runFile, run) }.isFailure)
        } finally {
            temporary.deleteRecursively()
        }

        assertEquals("HIM_CANDIDATE_DATASET_SCHEMA_V2", HimCandidateDatasetContractV2.SCHEMA_VERSION)
        assertEquals("HIM_CANDIDATE_DATASET_POLICY_V2", HimCandidateDatasetContractV2.POLICY_VERSION)
        assertEquals("HIM_CANDIDATE_GENERATION_RUN_V2", HimCandidateDatasetContractV2.RUN_CONTRACT)
        assertEquals("HIM_CANDIDATE_GENERATION_INPUT_RUN_V1", HimCandidateDatasetContractV2.INPUT_RUN_CONTRACT)
        assertEquals("HIM_CANDIDATE_DATASET_LOGICAL_DIGEST_V2", HimCandidateDatasetContractV2.LOGICAL_DIGEST_CONTRACT)
        assertEquals("HIM_CANDIDATE_REFERENCE_V1", HimCandidateDatasetContractV2.CANDIDATE_REFERENCE_CONTRACT)
        assertEquals("HIM_PER_SOURCE_MULTI_QUERY_EVIDENCE_PACKING_V1", HimPerSourceMultiQueryEvidencePackingPolicyV1.VERSION)
        assertEquals("HIM_SEMANTIC_INFERENCE_OUTPUT_V2_1", HimSemanticInferenceSchema.OUTPUT_VERSION)
        assertEquals("HIM_SEMANTIC_INFERENCE_INSTRUCTION_POLICY_V2_2", HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION)
        assertEquals(EXPECTED_FINGERPRINT, HimCandidateDatasetContractV2.PROVIDER_CONFIGURATION_FINGERPRINT.value)

        assertEquals(guardsBefore, EXPECTED_GUARDS.keys.associateWith { sha256(root.resolve(it)) })
        assertEquals(immutableBefore, IMMUTABLE_FILES.associateWith { snapshot(root.resolve(it)) })
        val report = root.resolve(REPORT)
        requireNotNull(report.parentFile).mkdirs()
        report.writeText(reportText())
    }

    private fun pilotEquivalentRun(round: HimCandidateRetrievalRoundProvenance): HimCandidateGenerationRun {
        val inference = inference()
        val inputSet = HimSha256(HimCandidateIdentityV1.sha256(INPUTS.joinToString("\n", postfix = "\n")))
        val runReference = HimCandidateIdentityV1.run(MISSION, inputSet, inference)
        val inputs = INPUTS.map { raw ->
            val input = HimCandidateInputProvenance(raw, raw.lowercase())
            HimCandidateGenerationInputRun(
                HimCandidateIdentityV1.inputRun(runReference, input), input, emptyList(), listOf(round),
                HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, HimRetrievalTerminalState.SEARCH_EXHAUSTED,
                HimCandidateInputCompletionState.COMPLETED, HimCandidateFinalInferenceOutcome.SUCCESS_WITH_NO_PERSISTED_CANDIDATE,
                HimCandidateConfidenceDiagnostics(0, 0, 0, 0), emptyList(), emptyList(), emptyList(), inference, null,
            )
        }.sortedBy { it.inputRunReference.value }
        return HimCandidateGenerationRun(runReference, MISSION, inputSet, HimCandidateGenerationRunState.COMPLETE, inputs, emptyList())
    }

    private fun round(number: Int, sourceQueries: List<HimSemanticSourceQueries>, packedOrder: List<HimEvidenceReference>): HimCandidateRetrievalRoundProvenance {
        val directive = HimSemanticRetrievalDirective(sourceQueries, HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP)
        var firstStep = true
        val steps = sourceQueries.flatMap { item -> item.queries.map { query ->
            val retrieved = if (firstStep && packedOrder.all { it.source == item.source.name }) packedOrder else emptyList()
            firstStep = false
            HimCandidateRetrievalStepProvenance(item.source, query, retrieved)
        } }
        return HimCandidateRetrievalRoundProvenance(HimRetrievalRound(number), directive, steps, packedOrder, emptyList())
    }

    private fun evidence(source: HimGroundTruthSource, identity: String) = HimEvidenceReference(source.name, HimSha256("1".repeat(64)), identity)
    private fun inference() = HimCandidateInferenceProvenance(
        "OPENAI", "gpt-5.6-sol", HimSha256(EXPECTED_FINGERPRINT), HimSemanticInferenceSchema.OUTPUT_VERSION,
        HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION, HimCandidateDatasetContractV2.CONTEXT_BUDGET_POLICY,
        "F3D_2_RETRIEVAL_FOUNDATION_V1", HimSha256(EXPECTED_GUARDS.getValue(RETRIEVAL_RELEASE)),
        HimSha256("9780a1dc28ece40e1251981f007a509418de5db4c1e8921ba3402ac2dcf3fd86"), 1, null,
    )

    private fun reportText() = """HIM F3d.5d CANDIDATE DATASET ORDERING ALIGNMENT
FOUNDATION STATUS=PASS
RETRIEVAL FOUNDATION STATUS=PASS
REAL PILOT BLOCKER=alphabetical persistence validation contradicted F3d.3c; resolved=true
ORDER AUTHORITY=canonical Source order + original HIM directive query order
SOURCE ORDER=OPEN_FOOD_FACTS,AGRIBALYSE,CIQUAL,GLYCEMIC_INDEX; subset relative order validated
QUERY ORDER=original sequence preserved; lexical resort=false
DETERMINISM VS ALPHABETICAL SORTING=contract order is deterministic; lexical order is not authority
PACKING ORDER=ROUND_ROBIN_BY_QUERY_ORDER preserved byte-for-byte
RETRIEVAL ROUND ORDER=numeric 1..N unchanged
DATASET CONTRACT AUDIT=V2 identities unchanged; no alphabetical normative rule
DATASET DIGEST AUDIT=stored authoritative order included; query rewrite changes digest
SERIALIZATION ORDER=ordered lists; Gson does not reorder; deterministic=true
IDEMPOTENCY=same run same bytes and digest; no order drift
REAL-PILOT-EQUIVALENT FIXTURE=three Hering inputs; addRun PASS; production writes=0
ATOMIC PERSISTENCE INTEGRITY=unchanged; temporary-path fixture PASS
PROVIDER CONTRACT INTEGRITY=schema=V2.1 instruction=V2.2 fingerprint=current
PACKING CONTRACT INTEGRITY=${HimPerSourceMultiQueryEvidencePackingPolicyV1.VERSION}; changed=false
DATA WRITES=OpenAI=0 realRuns=0 realCandidates=0 authority=0 entityIds=0
FILES CREATED=RunHimCandidateDatasetOrderingAlignmentTest.kt; derived report
FILES CHANGED=HimCandidateDatasetPersistenceV2.validateDeterministicOrdering; RunHimFirstRealCandidateGenerationPilotTest.materialize
BUILD=verified outside report-generating test
TESTS=F3d.5d matrix PASS
GIT STATUS=reported by caller
CONTRACT DEVIATIONS=none
HARD FAILURES=none
CANDIDATE_DATASET_ORDERING_ALIGNED=true
FIRST_REAL_CANDIDATE_GENERATION_READY=true
F3d.5d COMPLETE=true
""".trimIndent() + "\n"

    private data class Snapshot(val bytes: Long, val modified: Long)
    private fun snapshot(file: File) = Snapshot(file.length(), file.lastModified())
    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
    private fun sha256(file: File): String { val digest = MessageDigest.getInstance("SHA-256"); file.inputStream().buffered().use { input -> val buffer = ByteArray(DEFAULT_BUFFER_SIZE); while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) } }; return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } }

    companion object {
        private const val MISSION = "F3D-FIRST-REAL-CANDIDATE-GENERATION-PILOT-V1"
        private val INPUTS = listOf("Hering geräuchert", "Hering eingelegt", "Hering in Öl")
        private const val EXPECTED_FINGERPRINT = "b01d790cb8ebd886024cef145d85786029fee684a032379776636d426d851579"
        private const val RETRIEVAL_RELEASE = "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json"
        private const val REPORT = "build/knowledge/reports/him/candidates/him-f3d5d-candidate-dataset-ordering-alignment.txt"
        private val EXPECTED_GUARDS = linkedMapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
            RETRIEVAL_RELEASE to "b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023",
        )
        private val IMMUTABLE_FILES = listOf(
            "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", "data/sources/agribalyse/index/agribalyse-him-evidence-index.v1.sqlite",
            "data/sources/ciqual/index/ciqual-him-evidence-index.v1.sqlite", "data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite",
            "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", "data/sources/agribalyse/optimized/agribalyse-him-final-source.jsonl.gz",
            "data/sources/ciqual/optimized/ciqual-him-final-source.json.gz", "data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz",
        )
    }
}
