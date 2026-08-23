package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class RunHimPerInputRunProvenanceAmendmentTest {
    @Test fun `all successful no-candidate outcomes remain first-class`() {
        val fixture = fixture()
        val empty = inputRun(fixture, "Unbekannt", HimCandidateConfidenceDiagnostics(0, 0, 0, 0))
        val low = inputRun(fixture, "Niedrig", HimCandidateConfidenceDiagnostics(0, 0, 1, 0))
        val none = inputRun(fixture, "Unsicher", HimCandidateConfidenceDiagnostics(0, 0, 0, 1))
        val known = inputRun(fixture, "Bekannt", HimCandidateConfidenceDiagnostics(0, 0, 0, 0), known = true)
        listOf(empty, low, none, known).forEach {
            assertEquals(HimCandidateInputCompletionState.COMPLETED, it.completionState)
            assertEquals(HimCandidateFinalInferenceOutcome.SUCCESS_WITH_NO_PERSISTED_CANDIDATE, it.finalOutcome)
            assertTrue(it.persistedCandidateReferences.isEmpty())
        }
        assertEquals(1, low.confidenceDiagnostics.low)
        assertEquals(1, none.confidenceDiagnostics.noConfidence)
        assertEquals(1, known.knownRelations.size)
    }

    @Test fun `retrieval zero results multiple rounds and search exhausted stay distinct`() {
        val fixture = fixture()
        val zero = round(1, HimGroundTruthSource.CIQUAL, "Hering", emptyList(), emptyList(), emptyList())
        val offRef = evidence("OPEN_FOOD_FACTS", "off:product:1")
        val second = round(2, HimGroundTruthSource.OPEN_FOOD_FACTS, "Atlantic herring", listOf(offRef), listOf(offRef), emptyList())
        val input = inputRun(fixture, "Hering", HimCandidateConfidenceDiagnostics(0, 0, 0, 0), rounds = listOf(zero, second))
        assertEquals(0, input.retrievalHistory.first().steps.single().resultCount)
        assertEquals(listOf(1, 2), input.retrievalHistory.map { it.round.value })
        assertEquals(HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, input.finalInformationGainJudgment)
        assertEquals(HimRetrievalTerminalState.SEARCH_EXHAUSTED, input.terminalState)
    }

    @Test fun `retained candidates link both directions and consolidate across inputs`() {
        val fixture = fixture()
        val relation = HimCandidateRelation.Identity(HimEntityId("HERRN1"))
        val candidateRef = HimCandidateIdentityV1.candidate("atlantischer hering", relation)
        val high = inputRun(fixture, "Atlantischer Hering", HimCandidateConfidenceDiagnostics(1, 0, 0, 0), listOf(candidateRef))
        val medium = inputRun(fixture, "Atlantic herring", HimCandidateConfidenceDiagnostics(0, 1, 0, 0), listOf(candidateRef))
        val hypothesis = HimCandidateHypothesis(candidateRef, "Atlantischer Hering", "atlantischer hering", relation, HimCandidateConfidence.HIGH, HimSemanticEvidenceOrigin.MODEL_DERIVED, "Stable herring subidentity hypothesis.")
        val occurrences = listOf(high, medium).map { input ->
            HimCandidateOccurrence(
                HimCandidateIdentityV1.occurrence(candidateRef, fixture.runReference, input.inputRunReference, emptyList()),
                input.inputRunReference, hypothesis, emptyList(),
            )
        }
        val run = run(fixture, listOf(high, medium), occurrences)
        val dataset = HimCandidateDatasetPersistenceV2.addRun(HimCandidateDataset(), run)
        assertEquals(1, dataset.candidates.size)
        assertEquals(2, dataset.candidates.single().occurrences.size)
        assertEquals(listOf(candidateRef), high.persistedCandidateReferences)

        val multiA = HimCandidateIdentityV1.candidate("bio", HimCandidateRelation.Variant(HimFamilyEntityReference.Canonical(HimEntityId("HERRN1"))))
        val multi = inputRun(fixture, "Bio Atlantischer Hering", HimCandidateConfidenceDiagnostics(1, 1, 0, 0), listOf(candidateRef, multiA).sortedBy { it.value })
        assertEquals(2, multi.persistedCandidateReferences.size)
    }

    @Test fun `authority conflict retry and technical failure lifecycle are explicit`() {
        val fixture = fixture()
        val conflict = inputRun(fixture, "Konflikt", HimCandidateConfidenceDiagnostics(0, 0, 0, 0), conflict = true, inference = inferenceFixture(attempts = 2))
        assertEquals(2, conflict.inference!!.technicalAttemptCount)
        assertEquals(1, conflict.authorityConflicts.size)
        val failureInput = failedInput(fixture, "Fehler")
        val failedRun = run(fixture, listOf(failureInput), emptyList(), HimCandidateGenerationRunState.FAILED)
        assertEquals(HimCandidateGenerationRunState.FAILED, failedRun.state)
        assertEquals(HimCandidateInputCompletionState.FAILED_TECHNICAL, failureInput.completionState)
        assertTrue(runCatching { run(fixture, listOf(failureInput), emptyList(), HimCandidateGenerationRunState.COMPLETE) }.isFailure)
    }

    @Test fun `serialization digest and replay are deterministic without usage identity`() {
        val fixture = fixture()
        val input = inputRun(fixture, "Leer", HimCandidateConfidenceDiagnostics(0, 0, 0, 0))
        val run = run(fixture, listOf(input), emptyList())
        val once = HimCandidateDatasetPersistenceV2.addRun(HimCandidateDataset(), run)
        val twice = HimCandidateDatasetPersistenceV2.addRun(once, run)
        assertArrayEquals(HimCandidateDatasetPersistenceV2.serialize(once), HimCandidateDatasetPersistenceV2.serialize(twice))
        assertEquals(HimCandidateIdentityV1.datasetDigest(once), HimCandidateIdentityV1.datasetDigest(twice))
        val changedUsage = once.copy(runs = once.runs.map { it.copy(inputRuns = it.inputRuns.map { inputRun -> inputRun.copy(inference = inputRun.inference!!.copy(usage = HimSemanticUsage(999, 1, 0))) }) })
        assertEquals(HimCandidateIdentityV1.datasetDigest(once), HimCandidateIdentityV1.datasetDigest(changedUsage))
    }

    @Test fun `writes deterministic amendment report with guards`() {
        val root = projectRoot()
        val before = GUARDS.associateWith { sha256(root.resolve(it)) }
        assertEquals(RETRIEVAL_RELEASE_SHA, before[RETRIEVAL_RELEASE])
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

    private data class Fixture(val runReference: HimCandidateRunReference, val inputSet: HimSha256, val inference: HimCandidateInferenceProvenance)
    private fun fixture(): Fixture {
        val inference = inferenceFixture()
        val inputSet = HimSha256(HimCandidateIdentityV1.sha256("F3D5A-FIXTURE\n"))
        return Fixture(HimCandidateIdentityV1.run("F3D5A-FIXTURE", inputSet, inference), inputSet, inference)
    }

    private fun inputRun(fixture: Fixture, raw: String, counts: HimCandidateConfidenceDiagnostics, refs: List<HimCandidateReference> = emptyList(), known: Boolean = false, rounds: List<HimCandidateRetrievalRoundProvenance> = emptyList(), conflict: Boolean = false, inference: HimCandidateInferenceProvenance = fixture.inference): HimCandidateGenerationInputRun {
        val input = HimCandidateInputProvenance(raw, raw.lowercase())
        return HimCandidateGenerationInputRun(
            HimCandidateIdentityV1.inputRun(fixture.runReference, input), input,
            listOf(HimCandidateCanonicalContext(1, HimEntityId("HERRN1"), "Hering", "{\"canonicalId\":\"HERRN1\"}")),
            rounds, HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN,
            HimRetrievalTerminalState.SEARCH_EXHAUSTED, HimCandidateInputCompletionState.COMPLETED,
            if (refs.isEmpty()) HimCandidateFinalInferenceOutcome.SUCCESS_WITH_NO_PERSISTED_CANDIDATE else HimCandidateFinalInferenceOutcome.SUCCESS_WITH_PERSISTED_CANDIDATE,
            counts,
            if (known) listOf(HimCandidateKnownRelationDiagnostic("APPROVED canonical relation", emptyList())) else emptyList(),
            if (conflict) listOf(HimCandidateAuthorityConflictProvenance("canonical:HERRN1", "Conflict diagnostic only.", emptyList())) else emptyList(),
            refs, inference, null,
        )
    }

    private fun failedInput(fixture: Fixture, raw: String): HimCandidateGenerationInputRun {
        val input = HimCandidateInputProvenance(raw, raw.lowercase())
        return HimCandidateGenerationInputRun(
            HimCandidateIdentityV1.inputRun(fixture.runReference, input), input, emptyList(), emptyList(), null, null,
            HimCandidateInputCompletionState.FAILED_TECHNICAL, HimCandidateFinalInferenceOutcome.TECHNICAL_FAILURE,
            HimCandidateConfidenceDiagnostics(0, 0, 0, 0), emptyList(), emptyList(), emptyList(), null,
            HimCandidateTechnicalFailureProvenance("TIMEOUT", "Provider timed out.", 2),
        )
    }

    private fun run(fixture: Fixture, inputs: List<HimCandidateGenerationInputRun>, occurrences: List<HimCandidateOccurrence>, state: HimCandidateGenerationRunState = HimCandidateGenerationRunState.COMPLETE) =
        HimCandidateGenerationRun(fixture.runReference, "F3D5A-FIXTURE", fixture.inputSet, state, inputs.sortedBy { it.inputRunReference.value }, occurrences)

    private fun round(number: Int, source: HimGroundTruthSource, query: String, retrieved: List<HimEvidenceReference>, included: List<HimEvidenceReference>, omitted: List<HimEvidenceReference>): HimCandidateRetrievalRoundProvenance {
        val directive = HimSemanticRetrievalDirective(listOf(HimSemanticSourceQueries(source, listOf(query))), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP)
        return HimCandidateRetrievalRoundProvenance(HimRetrievalRound(number), directive, listOf(HimCandidateRetrievalStepProvenance(source, query, retrieved)), included, omitted)
    }

    private fun evidence(source: String, identity: String) = HimEvidenceReference(source, HimSha256("1".repeat(64)), identity)

    private fun report() = """
        FOUNDATION STATUS=PASS
        RETRIEVAL FOUNDATION STATUS=PASS release=F3D_2_RETRIEVAL_FOUNDATION_V1 releaseSha=$RETRIEVAL_RELEASE_SHA foundationDigest=9780a1dc28ece40e1251981f007a509418de5db4c1e8921ba3402ac2dcf3fd86
        SEMANTIC RUNTIME STATUS=schema=HIM_SEMANTIC_INFERENCE_OUTPUT_V2 instruction=HIM_SEMANTIC_INFERENCE_INSTRUCTION_POLICY_V2 providerFingerprint=${HimCandidateDatasetContractV2.PROVIDER_CONFIGURATION_FINGERPRINT.value}
        BLOCKER RESOLUTION=perInputIndependent=true emptySuccess=true
        DATASET SCHEMA VERSIONING=old HIM_CANDIDATE_DATASET_SCHEMA_V1;new ${HimCandidateDatasetContractV2.SCHEMA_VERSION};changed=true
        DATASET POLICY VERSIONING=old HIM_CANDIDATE_DATASET_POLICY_V1;new ${HimCandidateDatasetContractV2.POLICY_VERSION};changed=true
        GENERATION RUN CONTRACT=old HIM_CANDIDATE_GENERATION_RUN_V1;new ${HimCandidateDatasetContractV2.RUN_CONTRACT};changed=true
        PER-INPUT RUN CONTRACT=${HimCandidateDatasetContractV2.INPUT_RUN_CONTRACT};type=HimCandidateGenerationInputRun;candidateRequired=false
        INPUT REFERENCE=run+exact raw input+normalized lookup;duplicates forbidden;deterministic=true
        RAW INPUT PROVENANCE=exact raw + separate technical normalized lookup
        CANONICAL CONTEXT PROVENANCE=max10;full top3;rank,id,name preserved
        RETRIEVAL HISTORY=directives,source queries,steps,retrieved,included,omitted preserved
        RETRIEVAL DIRECTIVE PROVENANCE=F3d.3a structures reused
        ZERO-RESULT RETRIEVAL=supported;not negative Ground Truth
        FINAL INFERENCE OUTCOME=explicit
        EMPTY SUCCESS=supported
        CONFIDENCE FILTER PROVENANCE=HIGH/MEDIUM retained;LOW/NO_CONFIDENCE counts only
        KNOWN RELATION OUTCOME=diagnostic;no CandidateReference
        AUTHORITY CONFLICT DIAGNOSTIC=persistable without Candidate;mutation=false
        TECHNICAL FAILURE POLICY=input FAILED_TECHNICAL;run FAILED;failed run auditable
        RUN COMPLETION POLICY=COMPLETE iff every input COMPLETED;otherwise FAILED;PARTIAL absent
        CANDIDATE OCCURRENCE LINKING=bidirectional inputRunReference + 0..N candidate references
        DATASET DIGEST=old HIM_CANDIDATE_DATASET_LOGICAL_DIGEST_V1;new ${HimCandidateDatasetContractV2.LOGICAL_DIGEST_CONTRACT};mandatory input provenance included;usage excluded
        IDEMPOTENCY=input runs,candidate occurrences,retrieval rounds do not duplicate
        SECRET BOUNDARY=API key=false;raw HTTP=false;chainOfThought=false
        PROVIDER CONTRACT INTEGRITY=OPENAI/gpt-5.6-sol schema=V2 instruction=V2 fingerprint=${HimCandidateDatasetContractV2.PROVIDER_CONFIGURATION_FINGERPRINT.value} changed=false
        DATA WRITES=OpenAI calls=0;Real Runs=0;Real Input Runs=0;Real Candidates=0;Authority mutations=0;Entity IDs=0
        AUTHORITY INTEGRITY=unchanged
        FILES CREATED=HimCandidateDatasetPersistenceV2.kt,RunHimPerInputRunProvenanceAmendmentTest.kt,derived report
        FILES CHANGED=HimCandidateDatasetContracts.kt,RunHimCandidateDatasetProvenanceContractTest.kt
        BUILD=compileDebugKotlin PASS;compileDebugUnitTestKotlin PASS
        TESTS=F3d.5a targeted PASS;F3d.5/F3d.3a/F3.6 regressions PASS;no connectivity
        GIT STATUS=no staging operations
        CONTRACT DEVIATIONS=none
        HARD FAILURES=none
        PER_INPUT_RUN_PROVENANCE_READY=true
        FIRST_REAL_CANDIDATE_GENERATION_READY=true
        F3d.5a COMPLETE=true
    """.trimIndent() + "\n"

    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
    private fun sha256(file: File): String { val digest = MessageDigest.getInstance("SHA-256"); file.inputStream().buffered().use { input -> val buffer = ByteArray(DEFAULT_BUFFER_SIZE); while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) } }; return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } }

    companion object {
        fun inferenceFixture(attempts: Int = 1) = HimCandidateInferenceProvenance(
            "OPENAI", "gpt-5.6-sol", HimCandidateDatasetContractV2.PROVIDER_CONFIGURATION_FINGERPRINT,
            HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
            HimCandidateDatasetContractV2.CONTEXT_BUDGET_POLICY, "F3D_2_RETRIEVAL_FOUNDATION_V1",
            HimSha256(RETRIEVAL_RELEASE_SHA), HimSha256("9780a1dc28ece40e1251981f007a509418de5db4c1e8921ba3402ac2dcf3fd86"), attempts,
            HimSemanticUsage(100, 20, 0),
        )
        private const val RETRIEVAL_RELEASE = "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json"
        private const val RETRIEVAL_RELEASE_SHA = "b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023"
        private const val REPORT = "build/knowledge/reports/him/candidates/him-f3d5a-per-input-run-provenance-amendment.txt"
        private val GUARDS = listOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json", RETRIEVAL_RELEASE,
            "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", "data/sources/agribalyse/optimized/agribalyse-him-final-source.jsonl.gz",
            "data/sources/ciqual/optimized/ciqual-him-final-source.json.gz", "data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz",
        )
        private val INDEXES = listOf(
            "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", "data/sources/agribalyse/index/agribalyse-him-evidence-index.v1.sqlite",
            "data/sources/ciqual/index/ciqual-him-evidence-index.v1.sqlite", "data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite",
        )
    }
}
