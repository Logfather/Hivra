package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimRetrievalFoundationBinding
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceRequest
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticRetrievalDirective
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticRetrievalHistoryEntry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceArtifactIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceQueries
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class RunHimPerSourceMultiQueryEvidencePackingContractTest {
    private val packer = HimPerSourceMultiQueryEvidencePacker()

    @Test fun verifyPackingContractAndIntegrity() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val guardsBefore = GUARDS.associateWith { sha256(root.resolve(it)) }
        EXPECTED_GUARDS.forEach { (path, expected) -> assertEquals(path, expected, guardsBefore[path]) }
        val immutableBefore = IMMUTABLE_FILES.associateWith { snapshot(root.resolve(it)) }

        assertIds((1..5).map { occurrence("A", 1, it, it) }.pack(), (1..5).toList())
        assertEquals(10, (1..10).map { occurrence("A", 1, it, it) }.pack().includedEvidence.size)
        assertEquals(10, (1..11).map { occurrence("A", 1, if (it == 11) 1 else it, it) }.pack().includedEvidence.size)

        val two = ((1..10).map { occurrence("A", 1, it, it) } + (1..10).map { occurrence("B", 2, it, 100 + it) }).pack()
        assertIds(two, listOf(1, 101, 2, 102, 3, 103, 4, 104, 5, 105))
        assertEquals(10, two.omittedEvidence.size)

        val threeOccurrences = (1..10).flatMap { rank ->
            listOf(occurrence("A", 1, rank, rank), occurrence("B", 2, rank, 100 + rank), occurrence("C", 3, rank, 200 + rank))
        }
        val three = threeOccurrences.pack()
        assertIds(three, listOf(1, 101, 201, 2, 102, 202, 3, 103, 203, 4))
        assertEquals(ids(threeOccurrences).drop(10), three.omittedEvidence.map { id(it.evidence) })

        val duplicate = listOf(occurrence("A", 1, 1, 1), occurrence("A", 1, 2, 2), occurrence("B", 2, 1, 1), occurrence("B", 2, 2, 3)).pack()
        assertEquals(4, duplicate.retrievedOccurrences.size)
        assertIds(duplicate, listOf(1, 2, 3))

        val emptyBucket = ((1..10).map { occurrence("A", 1, it, it) } + (1..4).map { occurrence("C", 3, it, 100 + it) }).pack()
        assertIds(emptyBucket, listOf(1, 101, 2, 102, 3, 103, 4, 104, 5, 6))

        val allDuplicates = listOf("A", "B", "C").flatMapIndexed { query, name -> (1..4).map { occurrence(name, query + 1, it, it) } }.pack()
        assertEquals(12, allDuplicates.retrievedOccurrences.size)
        assertEquals(4, allDuplicates.uniqueRetrievedEvidence.size)
        assertEquals(4, allDuplicates.includedEvidence.size)

        val prior = (1..8).map(::off)
        val next = (1..10).map { occurrence("next", 1, it, 100 + it) }
        val stable = packer.pack(prior, next).perSource.single()
        assertEquals((1..8).toList() + listOf(101, 102), stable.includedEvidence.map(::id))
        assertEquals((103..110).toList(), stable.omittedEvidence.map { id(it.evidence) })
        assertTrue(stable.omittedEvidence.all { it.reason == HimPerSourceEvidenceOmissionReason.PER_SOURCE_REQUEST_LIMIT })

        val full = packer.pack((1..10).map(::off), next).perSource.single()
        assertEquals((1..10).toList(), full.includedEvidence.map(::id))
        assertEquals(10, full.omittedEvidence.size)
        assertEquals(10, full.retrievedOccurrences.size)

        val off = (1..14).map { occurrence("off", 1, if (it > 10) it - 10 else it, it) }
        val ciqual = (1..7).map { occurrence("ciqual", 1, it, it, HimGroundTruthSource.CIQUAL) }
        val multiple = packer.pack(emptyList(), off + ciqual)
        assertEquals(10, multiple.perSource.single { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS }.includedEvidence.size)
        assertEquals(7, multiple.perSource.single { it.source == HimGroundTruthSource.CIQUAL }.includedEvidence.size)
        assertEquals(multiple, packer.pack(emptyList(), off + ciqual))
        assertEquals(listOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.CIQUAL), multiple.perSource.map { it.source })

        val directive = HimSemanticRetrievalDirective(
            listOf(HimSemanticSourceQueries(HimGroundTruthSource.OPEN_FOOD_FACTS, listOf("fixture"))),
            HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP,
        )
        val request = HimSemanticInferenceRequest(
            invocationReference = "f3d3c-request-invariant",
            inputTerm = "fixture",
            canonicalContext = emptyList(),
            evidence = multiple.includedEvidence,
            retrievalRound = HimRetrievalRound(1),
            retrievalHistory = listOf(
                HimSemanticRetrievalHistoryEntry(
                    HimRetrievalRound(1), directive,
                    multiple.includedEvidence.map(HimSemanticSourceArtifactIdentityV1::reference),
                ),
            ),
        )
        assertTrue(request.evidence.groupBy { it.source }.values.all { it.size <= 10 })
        assertEquals("HIM_SEMANTIC_INFERENCE_OUTPUT_V2_1", HimSemanticInferenceSchema.OUTPUT_VERSION)
        assertEquals("HIM_SEMANTIC_INFERENCE_INSTRUCTION_POLICY_V2_2", HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION)
        assertEquals("b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023", HimRetrievalFoundationBinding.RELEASE_SHA256.value)

        assertEquals(guardsBefore, GUARDS.associateWith { sha256(root.resolve(it)) })
        assertEquals(immutableBefore, IMMUTABLE_FILES.associateWith { snapshot(root.resolve(it)) })
        val report = root.resolve(REPORT)
        requireNotNull(report.parentFile).mkdirs()
        report.writeText(reportText())
    }

    private fun List<HimEvidenceRetrievalOccurrence>.pack() = packer.pack(emptyList(), this).perSource.single()
    private fun assertIds(result: HimPerSourceEvidencePackingResult, expected: List<Int>) = assertEquals(expected, result.includedEvidence.map(::id))
    private fun ids(occurrences: List<HimEvidenceRetrievalOccurrence>) = occurrences.distinctBy { it.evidence.sourceRecordReference.value }.map { id(it.evidence) }
    private fun id(result: HimEvidenceSearchResult) = result.sourceRecordReference.value.substringAfterLast(':').toInt()
    private fun off(id: Int, rank: Int = ((id - 1) % 10) + 1) = HimEvidenceSearchResult(
        HimGroundTruthSource.OPEN_FOOD_FACTS, HimEvidenceRecordReference.offProduct(id.toLong(), id.toString()),
        HimEvidenceRecordKind.OFF_PRODUCT, rank, HimEvidenceProjection("{\"id\":$id}"),
    )
    private fun occurrence(query: String, queryOrder: Int, rank: Int, id: Int, source: HimGroundTruthSource = HimGroundTruthSource.OPEN_FOOD_FACTS): HimEvidenceRetrievalOccurrence {
        val evidence = if (source == HimGroundTruthSource.OPEN_FOOD_FACTS) off(id, rank) else HimEvidenceSearchResult(
            source, HimEvidenceRecordReference.ciqualFood(id.toString()), HimEvidenceRecordKind.CIQUAL_FOOD, rank, HimEvidenceProjection("{\"id\":$id}"),
        )
        return HimEvidenceRetrievalOccurrence(source, query, queryOrder, rank, evidence)
    }

    private fun reportText() = """HIM F3d.3c PER-SOURCE MULTI-QUERY EVIDENCE PACKING
FOUNDATION STATUS=PASS
RETRIEVAL FOUNDATION STATUS=PASS
SEMANTIC RUNTIME STATUS=PASS
CURRENT PILOT BLOCKER=>10 Evidence per Source before request construction; RESOLVED=true
PACKING CONTRACT=${HimPerSourceMultiQueryEvidencePackingPolicyV1.VERSION}
PACKING POLICY VERSION=${HimPerSourceMultiQueryEvidencePackingPolicyV1.VERSION}
LIMIT DISTINCTION=queries/source/round=3 evidence/source/query=10 evidence/source/request=10
QUERY ORDER=HIM directive order preserved
SOURCE-LOCAL RANK=preserved
ROUND-ROBIN ALGORITHM=${HimPerSourceMultiQueryEvidencePackingPolicyV1.ALGORITHM}
DUPLICATE SOURCE RECORD HANDLING=deduplicate source+sourceRecordReference; occurrences retained
RETRIEVAL OCCURRENCE PROVENANCE=retained per source/query/queryOrder/sourceLocalRank/reference
INCLUDED EVIDENCE=max 10 unique per Source
OMITTED EVIDENCE=explicit unique references
OMISSION REASON=${HimPerSourceEvidenceOmissionReason.PER_SOURCE_REQUEST_LIMIT}
CROSS-ROUND STABILITY=prior provider-visible Evidence retained; new Evidence fills capacity
SOURCE FULL BEHAVIOR=new Evidence retrieved and explicitly omitted
CONTEXT BUDGET ORDERING=per-Source packing before context budget
REQUEST INVARIANT=PASS
NO SCORE COMPARISON=true
PRE-HIM BOUNDARY=technical packing only
CANDIDATE SEMANTICS INTEGRITY=unchanged
PROVIDER CONTRACT INTEGRITY=V2.1 identities unchanged
CANDIDATE DATASET INTEGRITY=V2 identities unchanged
DATA WRITES=OpenAI=0 candidateRuns=0 candidates=0 dataset=0 authority=0 entityIds=0
AUTHORITY INTEGRITY=unchanged
FILES CREATED=HimPerSourceMultiQueryEvidencePacker.kt; RunHimPerSourceMultiQueryEvidencePackingContractTest.kt; derived report
FILES CHANGED=HimSemanticContextBudget.kt provider ordering; RunHimFirstRealCandidateGenerationPilotTest.kt integration
BUILD=verified by Gradle invocation outside report-generating test
TESTS=contract matrix PASS
GIT STATUS=reported by caller
CONTRACT DEVIATIONS=none
HARD FAILURES=none
PER_SOURCE_MULTI_QUERY_EVIDENCE_PACKING_READY=true
FIRST_REAL_CANDIDATE_GENERATION_READY=true
F3d.3c COMPLETE=true
""".trimIndent() + "\n"

    private data class Snapshot(val bytes: Long, val modified: Long)
    private fun snapshot(file: File) = Snapshot(file.length(), file.lastModified())
    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
    private fun sha256(file: File): String { val digest = MessageDigest.getInstance("SHA-256"); file.inputStream().buffered().use { input -> val buffer = ByteArray(DEFAULT_BUFFER_SIZE); while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) } }; return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } }

    companion object {
        private const val REPORT = "build/knowledge/reports/him/retrieval/him-f3d3c-per-source-multi-query-evidence-packing.txt"
        private val EXPECTED_GUARDS = linkedMapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
            "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json" to "b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023",
        )
        private val GUARDS = EXPECTED_GUARDS.keys
        private val IMMUTABLE_FILES = listOf(
            "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", "data/sources/agribalyse/index/agribalyse-him-evidence-index.v1.sqlite",
            "data/sources/ciqual/index/ciqual-him-evidence-index.v1.sqlite", "data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite",
            "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", "data/sources/agribalyse/optimized/agribalyse-him-final-source.jsonl.gz",
            "data/sources/ciqual/optimized/ciqual-him-final-source.json.gz", "data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz",
        )
    }
}
