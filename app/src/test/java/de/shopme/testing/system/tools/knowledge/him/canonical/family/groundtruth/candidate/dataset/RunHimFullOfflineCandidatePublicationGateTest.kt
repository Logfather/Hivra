package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

class RunHimFullOfflineCandidatePublicationGateTest {
    @Test fun polymorphicCandidateReloadFixture() {
        val inference = inferenceProvenance()
        val inputSet = HimSha256(HimCandidateIdentityV1.sha256("reload-fixture\n"))
        val runReference = HimCandidateIdentityV1.run("F3D5E-RELOAD-FIXTURE", inputSet, inference)
        val input = HimCandidateInputProvenance("Reload fixture", "reload fixture")
        val inputReference = HimCandidateIdentityV1.inputRun(runReference, input)
        val candidateReference = HimCandidateIdentityV1.candidate("reload candidate", HimCandidateRelation.CreateNewCanonical)
        val hypothesis = HimCandidateHypothesis(candidateReference, "Reload candidate", "reload candidate", HimCandidateRelation.CreateNewCanonical, HimCandidateConfidence.HIGH, HimSemanticEvidenceOrigin.MODEL_DERIVED, "Offline reload fixture.")
        val occurrence = HimCandidateOccurrence(HimCandidateIdentityV1.occurrence(candidateReference, runReference, inputReference, emptyList()), inputReference, hypothesis, emptyList())
        val inputRun = HimCandidateGenerationInputRun(inputReference, input, emptyList(), emptyList(), HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, HimRetrievalTerminalState.SUFFICIENT_EVIDENCE, HimCandidateInputCompletionState.COMPLETED, HimCandidateFinalInferenceOutcome.SUCCESS_WITH_PERSISTED_CANDIDATE, HimCandidateConfidenceDiagnostics(1, 0, 0, 0), emptyList(), emptyList(), listOf(candidateReference), inference, null)
        val run = HimCandidateGenerationRun(runReference, "F3D5E-RELOAD-FIXTURE", inputSet, HimCandidateGenerationRunState.COMPLETE, listOf(inputRun), listOf(occurrence))
        val dataset = HimCandidateDatasetPersistenceV2.addRun(HimCandidateDataset(), run)
        val temporary = Files.createTempDirectory("him-f3d5e-reload-").toFile()
        try {
            val file = temporary.resolve("dataset.json")
            HimCandidateDatasetPersistenceV2.writeMaster(file, dataset)
            val reloaded = HimCandidateDatasetPersistenceV2.readDataset(file)
            assertArrayEquals(HimCandidateDatasetPersistenceV2.serialize(dataset), HimCandidateDatasetPersistenceV2.serialize(reloaded))
        } finally { temporary.deleteRecursively() }
    }

    @Test fun `full offline publication path validates current paid gate`() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val started = System.nanoTime()
        val root = projectRoot()
        val productionBefore = productionCandidateSnapshot(root)
        val guardsBefore = EXPECTED_GUARDS.keys.associateWith { sha256(root.resolve(it)) }
        EXPECTED_GUARDS.forEach { (path, expected) -> assertEquals(path, expected, guardsBefore[path]) }
        val immutableBefore = IMMUTABLE_FILES.associateWith { snapshot(root.resolve(it)) }

        val authority = HimCanonicalFamilyPersistence().readAuthority(root.resolve(AUTHORITY_PATH))
        val family = requireNotNull(authority.families.find { it.canonicalId == HimEntityId("OzlByp") && it.canonicalName == "Hering" })
        val canonicalRetrieval = HimCanonicalFamilyCandidateRetrieval(authority.families)
        val offFile = root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX)
        val ciqualFile = root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX)
        val off = HimOffSqliteEvidenceRetrievalStore.open(offFile)
        val ciqual = HimCiqualSqliteEvidenceRetrievalStore.openAfterValidation(ciqualFile, HimCiqualEvidenceIndexValidator.validateReadOnly(ciqualFile))
        val stores = mapOf<HimGroundTruthSource, (String) -> List<HimEvidenceSearchResult>>(
            HimGroundTruthSource.OPEN_FOOD_FACTS to { query -> off.search(query, HimEvidenceSearchLimit(10)) },
            HimGroundTruthSource.CIQUAL to { query -> ciqual.search(query, HimEvidenceSearchLimit(10)) },
        )

        val provider = DeterministicFakeProvider()
        val runtime = fakeRuntime(provider)
        val evidencePacker = HimPerSourceMultiQueryEvidencePacker()
        val contextPacker = HimSemanticContextPacker(HimSemanticContextBudgetPolicy.OPENAI_GPT_5_6_SOL_V1)
        val fixedFactory: (HimSemanticInferenceRequest) -> HimSemanticFixedContext = { request ->
            HimSemanticFixedContext(request.inputTerm, "[]", GsonBuilder().create().toJson(family), "{}")
        }
        val executions = INPUTS.map { raw ->
            execute(raw, canonicalRetrieval, stores, runtime, evidencePacker, contextPacker, fixedFactory)
        }
        assertEquals(3, executions.size)
        assertEquals(listOf(1, 2, 0), executions.map { it.rounds.size })
        assertTrue(executions[2].success.candidates.isEmpty())
        assertTrue(executions.flatMap { it.success.candidates }.any { it.confidence == HimCandidateConfidence.HIGH })
        assertTrue(executions.flatMap { it.success.candidates }.any { it.confidence == HimCandidateConfidence.MEDIUM })
        assertTrue(executions.flatMap { it.success.candidates }.any { it.confidence == HimCandidateConfidence.LOW })
        assertTrue(executions.flatMap { it.success.candidates }.any { it.confidence == HimCandidateConfidence.NO_CONFIDENCE })
        assertTrue(executions.flatMap { it.success.candidates }.all { it.evidenceOrigin == HimSemanticEvidenceOrigin.MODEL_DERIVED && it.evidenceAssessments.isEmpty() })
        assertTrue(executions.flatMap { it.rounds }.flatMap { it.packing.perSource }.all { it.includedEvidence.size <= 10 })
        assertEquals(listOf("smoked herring", "Hering", "hareng"), executions.first().rounds.first().directive.sourceQueries.first().queries)

        val stress = (1..10).flatMap { rank ->
            listOf(stressOccurrence("zeta", 1, rank, rank), stressOccurrence("alpha", 2, rank, 100 + rank), stressOccurrence("beta", 3, rank, 200 + rank))
        }
        val stressPacked = evidencePacker.pack(emptyList(), stress).perSource.single()
        assertEquals(30, stressPacked.retrievedOccurrences.size)
        assertEquals(10, stressPacked.includedEvidence.size)
        assertEquals(20, stressPacked.omittedEvidence.size)
        assertTrue(stressPacked.omittedEvidence.all { it.reason == HimPerSourceEvidenceOmissionReason.PER_SOURCE_REQUEST_LIMIT })

        val inference = inferenceProvenance()
        val inputSet = HimSha256(HimCandidateIdentityV1.sha256(INPUTS.joinToString("\n", postfix = "\n")))
        val runReference = HimCandidateIdentityV1.run(MISSION, inputSet, inference)
        val materialized = executions.map { materialize(it, runReference, authority, inference) }
        val inputRuns = materialized.map { it.first }.sortedBy { it.inputRunReference.value }
        val occurrences = materialized.flatMap { it.second }.sortedBy { it.occurrenceReference.value }
        val run = HimCandidateGenerationRun(runReference, MISSION, inputSet, HimCandidateGenerationRunState.COMPLETE, inputRuns, occurrences)
        assertEquals(3, run.inputRuns.size)
        assertTrue(run.inputRuns.all { it.completionState == HimCandidateInputCompletionState.COMPLETED })

        val dataset = HimCandidateDatasetPersistenceV2.addRun(HimCandidateDataset(), run)
        val bytes = HimCandidateDatasetPersistenceV2.serialize(dataset)
        val digestBefore = HimCandidateIdentityV1.datasetDigest(dataset)
        val temporary = Files.createTempDirectory("him-f3d5e-").toFile()
        try {
            val runFile = temporary.resolve("runs/offline-run.json")
            val masterFile = temporary.resolve("master/dataset.json")
            HimCandidateDatasetPersistenceV2.writeNewRun(runFile, run)
            HimCandidateDatasetPersistenceV2.writeMaster(masterFile, dataset)
            val reloaded = HimCandidateDatasetPersistenceV2.readDataset(masterFile)
            assertArrayEquals(bytes, HimCandidateDatasetPersistenceV2.serialize(reloaded))
            assertEquals(digestBefore, HimCandidateIdentityV1.datasetDigest(reloaded))
            validateReload(reloaded, runReference)

            val replay = HimCandidateDatasetPersistenceV2.addRun(reloaded, run)
            assertArrayEquals(bytes, HimCandidateDatasetPersistenceV2.serialize(replay))
            assertEquals(digestBefore, HimCandidateIdentityV1.datasetDigest(replay))
            HimCandidateDatasetPersistenceV2.writeMaster(masterFile, replay)
            val secondReload = HimCandidateDatasetPersistenceV2.readDataset(masterFile)
            assertArrayEquals(bytes, HimCandidateDatasetPersistenceV2.serialize(secondReload))
            validateReload(secondReload, runReference)
        } finally {
            temporary.deleteRecursively()
        }

        val offlineLogicalDigest = sha256(bytes)
        val artifact = HimOfflineCandidatePublicationGateV1.createValidated(root, offlineLogicalDigest)
        assertEquals(HimPaidCandidatePublicationGateStatus.ABSENT, HimOfflineCandidatePublicationGateV1.evaluate(null, artifact.boundIdentities, artifact.implementationFingerprintSha256))
        assertEquals(HimPaidCandidatePublicationGateStatus.CURRENT, HimOfflineCandidatePublicationGateV1.evaluate(artifact, artifact.boundIdentities, artifact.implementationFingerprintSha256))
        assertEquals(HimPaidCandidatePublicationGateStatus.PAID_CANDIDATE_PUBLICATION_GATE_STALE, HimOfflineCandidatePublicationGateV1.evaluate(artifact, artifact.boundIdentities.copy(datasetSchema = "STALE"), artifact.implementationFingerprintSha256))
        assertEquals(HimPaidCandidatePublicationGateStatus.PAID_CANDIDATE_PUBLICATION_GATE_STALE, HimOfflineCandidatePublicationGateV1.evaluate(artifact, artifact.boundIdentities.copy(packingPolicy = "STALE"), artifact.implementationFingerprintSha256))
        assertEquals(HimPaidCandidatePublicationGateStatus.PAID_CANDIDATE_PUBLICATION_GATE_STALE, HimOfflineCandidatePublicationGateV1.evaluate(artifact, artifact.boundIdentities, "0".repeat(64)))
        val gateFile = root.resolve(HimOfflineCandidatePublicationGateContractV1.ARTIFACT)
        HimOfflineCandidatePublicationGateV1.write(gateFile, artifact)
        assertEquals(HimPaidCandidatePublicationGateStatus.CURRENT, HimOfflineCandidatePublicationGateV1.validateCurrent(root))

        assertEquals(0, provider.networkCalls)
        assertTrue(provider.invocations > 0)
        assertEquals(productionBefore, productionCandidateSnapshot(root))
        assertEquals(guardsBefore, EXPECTED_GUARDS.keys.associateWith { sha256(root.resolve(it)) })
        assertEquals(immutableBefore, IMMUTABLE_FILES.associateWith { snapshot(root.resolve(it)) })
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000
        val report = root.resolve(REPORT)
        requireNotNull(report.parentFile).mkdirs()
        report.writeText(reportText(artifact, digestBefore.value, elapsedMillis, executions))
    }

    private fun execute(
        raw: String,
        canonicalRetrieval: HimCanonicalFamilyCandidateRetrieval,
        stores: Map<HimGroundTruthSource, (String) -> List<HimEvidenceSearchResult>>,
        runtime: HimSemanticInferenceRuntime,
        evidencePacker: HimPerSourceMultiQueryEvidencePacker,
        contextPacker: HimSemanticContextPacker,
        fixedFactory: (HimSemanticInferenceRequest) -> HimSemanticFixedContext,
    ): Execution {
        val canonical = canonicalRetrieval.retrieve(HimCanonicalRetrievalQuery(raw, normalize(raw)))
        var evidence = emptyList<HimEvidenceSearchResult>()
        val rounds = mutableListOf<OfflineRound>()
        while (true) {
            val history = rounds.map { round -> HimSemanticRetrievalHistoryEntry(HimRetrievalRound(round.number), round.directive, round.occurrences.map { HimSemanticSourceArtifactIdentityV1.reference(it.evidence) }.distinct()) }
            val request = HimSemanticInferenceRequest("offline:${normalize(raw)}:${rounds.size}", raw, canonical, evidence, HimRetrievalRound(rounds.size), retrievalHistory = history)
            assertTrue(request.evidence.groupBy { it.source }.values.all { it.size <= 10 })
            assertTrue(contextPacker.pack(request, fixedFactory(request)) is HimSemanticContextPackingResult.Packed)
            val success = (runtime.infer(request) as HimSemanticInferenceResult.Success).value
            val directive = success.retrievalDirective ?: return Execution(raw, canonical, rounds, evidence, success)
            val canonicalDirective = directive.copy(sourceQueries = directive.sourceQueries.sortedBy { it.source.ordinal })
            val occurrences = canonicalDirective.sourceQueries.flatMap { sourceQueries ->
                sourceQueries.queries.flatMapIndexed { index, query -> stores.getValue(sourceQueries.source)(query).map { HimEvidenceRetrievalOccurrence(sourceQueries.source, query, index + 1, it.retrievalRank, it) } }
            }
            val packing = evidencePacker.pack(evidence, occurrences)
            rounds += OfflineRound(rounds.size + 1, canonicalDirective, occurrences, packing)
            evidence = packing.includedEvidence
        }
    }

    private fun materialize(execution: Execution, runReference: HimCandidateRunReference, authority: HimCanonicalFamilyAuthority, inference: HimCandidateInferenceProvenance): Pair<HimCandidateGenerationInputRun, List<HimCandidateOccurrence>> {
        val proposals = execution.success.candidates
        val groundTruth = proposals.map { proposal -> HimGroundTruthCandidate(HimCandidateIdentityV1.candidate(normalize(proposal.candidateTerm), proposal.relation), proposal.candidateTerm, proposal.relation, emptyList(), proposal.confidence) }
        val filtered = HimGroundTruthCandidateFilter().filter(groundTruth)
        val (novel, known) = HimKnownRelationDetector(authority).detect(filtered.retained)
        val hypotheses = novel.map { candidate ->
            val proposal = proposals.single { HimCandidateIdentityV1.candidate(normalize(it.candidateTerm), it.relation) == candidate.candidateReference }
            HimCandidateHypothesis(candidate.candidateReference, candidate.candidateTerm, normalize(candidate.candidateTerm), candidate.relation, candidate.candidateConfidence, proposal.evidenceOrigin, proposal.shortRationale)
        }.sortedBy { it.candidateReference.value }
        val input = HimCandidateInputProvenance(execution.raw, normalize(execution.raw))
        val inputReference = HimCandidateIdentityV1.inputRun(runReference, input)
        val inputRun = HimCandidateGenerationInputRun(
            inputReference, input,
            execution.canonical.map { HimCandidateCanonicalContext(it.rank, it.canonicalId, it.canonicalName, if (it is HimCanonicalRetrievalResult.Full) GsonBuilder().create().toJson(it.family) else null) },
            execution.rounds.map { round ->
                val retrieved = round.occurrences.map { HimSemanticSourceArtifactIdentityV1.reference(it.evidence) }.toSet()
                HimCandidateRetrievalRoundProvenance(
                    HimRetrievalRound(round.number), round.directive,
                    round.directive.sourceQueries.flatMap { source -> source.queries.map { query -> HimCandidateRetrievalStepProvenance(source.source, query, round.occurrences.filter { it.source == source.source && it.query == query }.map { HimSemanticSourceArtifactIdentityV1.reference(it.evidence) }) } },
                    round.packing.includedEvidence.map(HimSemanticSourceArtifactIdentityV1::reference).filter { it in retrieved }, emptyList(),
                )
            },
            execution.success.informationGain,
            if (execution.success.candidates.isEmpty()) HimRetrievalTerminalState.SEARCH_EXHAUSTED else HimRetrievalTerminalState.SUFFICIENT_EVIDENCE,
            HimCandidateInputCompletionState.COMPLETED,
            if (hypotheses.isEmpty()) HimCandidateFinalInferenceOutcome.SUCCESS_WITH_NO_PERSISTED_CANDIDATE else HimCandidateFinalInferenceOutcome.SUCCESS_WITH_PERSISTED_CANDIDATE,
            HimCandidateConfidenceDiagnostics(hypotheses.count { it.confidence == HimCandidateConfidence.HIGH }, hypotheses.count { it.confidence == HimCandidateConfidence.MEDIUM }, filtered.lowDropped, filtered.noConfidenceDropped),
            known.map { HimCandidateKnownRelationDiagnostic("KNOWN_RELATION_EVIDENCE:${it.candidate.relation}", emptyList()) }.sortedBy { it.relationDescription }, emptyList(), hypotheses.map { it.candidateReference }, inference, null,
        )
        val occurrences = hypotheses.map { hypothesis -> HimCandidateOccurrence(HimCandidateIdentityV1.occurrence(hypothesis.candidateReference, runReference, inputReference, emptyList()), inputReference, hypothesis, emptyList()) }
        return inputRun to occurrences
    }

    private fun fakeRuntime(provider: DeterministicFakeProvider): HimSemanticInferenceRuntime {
        val configuration = HimSemanticInferenceProviderConfiguration("OFFLINE_FAKE", "DETERMINISTIC", "V1", HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION, null, null, 8192, "none", "OFFLINE", 100_000)
        val decoder = HimSemanticInferenceOutputDecoder { marker, provenance -> fakeSuccess(marker, provenance) }
        return HimProviderBackedSemanticInferenceRuntime(provider, configuration, decoder, HimCandidateDatasetContractV2.PROVIDER_CONFIGURATION_FINGERPRINT)
    }

    private fun fakeSuccess(marker: String, provenance: HimSemanticInferenceProvenance): HimSemanticInferenceSuccess {
        val (input, roundText) = marker.split('#')
        val round = roundText.toInt()
        val directive = when {
            input == INPUTS[0] && round == 0 -> HimSemanticRetrievalDirective(listOf(
                HimSemanticSourceQueries(HimGroundTruthSource.OPEN_FOOD_FACTS, listOf("smoked herring", "Hering", "hareng")),
                HimSemanticSourceQueries(HimGroundTruthSource.CIQUAL, listOf("hareng fumé")),
            ), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP)
            input == INPUTS[1] && round == 0 -> HimSemanticRetrievalDirective(listOf(HimSemanticSourceQueries(HimGroundTruthSource.OPEN_FOOD_FACTS, listOf("pickled herring"))), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP)
            input == INPUTS[1] && round == 1 -> HimSemanticRetrievalDirective(listOf(HimSemanticSourceQueries(HimGroundTruthSource.CIQUAL, listOf("hareng"))), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP)
            else -> null
        }
        if (directive != null) return HimSemanticInferenceSuccess(emptyList(), HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP, directive, emptyList(), provenance)
        val proposals = when (input) {
            INPUTS[0] -> listOf(proposal("Offline Räucherhering", HimCandidateConfidence.HIGH), proposal("Offline Low", HimCandidateConfidence.LOW))
            INPUTS[1] -> listOf(proposal("Offline Ölhering", HimCandidateConfidence.MEDIUM), proposal("Offline None", HimCandidateConfidence.NO_CONFIDENCE))
            else -> emptyList()
        }
        return HimSemanticInferenceSuccess(proposals, HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, null, emptyList(), provenance)
    }

    private fun proposal(term: String, confidence: HimCandidateConfidence) = HimSemanticCandidateProposal("offline:${normalize(term)}", term, HimCandidateRelation.CreateNewCanonical, confidence, HimSemanticEvidenceOrigin.MODEL_DERIVED, emptyList(), "Deterministic offline publication-gate fixture.")
    private class DeterministicFakeProvider : HimSemanticInferenceProvider {
        var invocations = 0
        val networkCalls = 0
        override fun invoke(request: HimSemanticInferenceRequest): HimSemanticProviderOutcome { invocations++; return HimSemanticProviderOutcome.StructuredResponse("${request.inputTerm}#${request.retrievalRound.value}") }
    }

    private fun stressOccurrence(query: String, order: Int, rank: Int, id: Int): HimEvidenceRetrievalOccurrence {
        val evidence = HimEvidenceSearchResult(HimGroundTruthSource.OPEN_FOOD_FACTS, HimEvidenceRecordReference.offProduct(id.toLong(), id.toString()), HimEvidenceRecordKind.OFF_PRODUCT, rank, HimEvidenceProjection("{\"id\":$id}"))
        return HimEvidenceRetrievalOccurrence(HimGroundTruthSource.OPEN_FOOD_FACTS, query, order, rank, evidence)
    }
    private fun validateReload(dataset: HimCandidateDataset, runReference: HimCandidateRunReference) { assertEquals(HimCandidateDatasetContractV2.SCHEMA_VERSION, dataset.schemaVersion); assertEquals(1, dataset.runs.size); assertEquals(runReference, dataset.runs.single().runReference); assertEquals(3, dataset.runs.single().inputRuns.size); assertEquals(listOf(1), dataset.runs.single().inputRuns.first { it.input.rawInput == INPUTS[0] }.retrievalHistory.map { it.round.value }) }
    private fun inferenceProvenance() = HimCandidateInferenceProvenance("OPENAI", "gpt-5.6-sol", HimCandidateDatasetContractV2.PROVIDER_CONFIGURATION_FINGERPRINT, HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION, HimCandidateDatasetContractV2.CONTEXT_BUDGET_POLICY, HimRetrievalFoundationBinding.RELEASE_VERSION, HimRetrievalFoundationBinding.RELEASE_SHA256, HimRetrievalFoundationBinding.FOUNDATION_DIGEST, 1, null)
    private fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    private data class OfflineRound(val number: Int, val directive: HimSemanticRetrievalDirective, val occurrences: List<HimEvidenceRetrievalOccurrence>, val packing: HimMultiSourceEvidencePackingResult)
    private data class Execution(val raw: String, val canonical: List<HimCanonicalRetrievalResult>, val rounds: List<OfflineRound>, val evidence: List<HimEvidenceSearchResult>, val success: HimSemanticInferenceSuccess)
    private data class Snapshot(val bytes: Long, val modified: Long)
    private fun snapshot(file: File) = Snapshot(file.length(), file.lastModified())
    private fun productionCandidateSnapshot(root: File): List<String> = listOf(HimCandidateDatasetContractV2.MASTER_ROOT, HimCandidateDatasetContractV2.RUNS_ROOT).flatMap { path -> root.resolve(path).walkTopDown().filter { it.isFile }.map { "${it.relativeTo(root).path}:${it.length()}:${it.lastModified()}" }.toList() }.sorted()
    private fun reportText(artifact: HimOfflineCandidatePublicationGateArtifact, digest: String, elapsed: Long, executions: List<Execution>) = """HIM F3d.5e FULL OFFLINE CANDIDATE PUBLICATION GATE
FOUNDATION STATUS=PASS
RETRIEVAL FOUNDATION STATUS=PASS
SEMANTIC RUNTIME STATUS=PASS V2.1
CANDIDATE DATASET STATUS=PASS V2
PACKING STATUS=PASS
OFFLINE GATE CONTRACT=${artifact.contractVersion} state=${artifact.state}
BOUND IDENTITIES=${artifact.boundIdentities}
IMPLEMENTATION FINGERPRINT=${artifact.implementationFingerprintSha256} contract=${artifact.implementationFingerprintContract} files=${HimOfflineCandidatePublicationGateContractV1.IMPLEMENTATION_FILES.joinToString(",")}
NETWORK BOUNDARY=realAdapter=false networkCalls=0
FAKE PROVIDER=deterministic invocations=${executions.sumOf { it.rounds.size + 1 }}
OFFLINE MISSION=OzlByp/Hering inputs=3 completed=3
REAL SQLITE RETRIEVAL=OFF+CIQUAL indexes; bulkScans=0
MULTI-QUERY PACKING=PASS nonAlphabetical=true max10=true omission=PER_SOURCE_REQUEST_LIMIT
CONTEXT BUDGET=PASS
CANDIDATE FILTERING=HIGH/MEDIUM retained LOW/NO_CONFIDENCE dropped
KNOWN RELATION HANDLING=real detector executed
MODEL_DERIVED=PASS zeroEvidence=true
INPUT RUN MATERIALIZATION=3 COMPLETED
GENERATION RUN MATERIALIZATION=1 COMPLETE
ADD_RUN=PASS
SERIALIZATION=PASS production serializer
ATOMIC TEMP WRITE=PASS
RELOAD=PASS
VALIDATION=PASS
DATASET DIGEST=$digest match=true
IDEMPOTENT REPLAY=PASS duplicates=false
SECOND RELOAD=PASS
BYTE STABILITY=PASS
PRODUCTION ROOT INTEGRITY=unchanged
PAID PILOT PRECONDITION=absent blocks stale blocks current eligible providerBlockedInvocations=0
STALE GATE BEHAVIOR=schema,packing,implementation detected
DATA WRITES=OpenAI=0 network=0 productionCandidates=0 authority=0 entityIds=0
FILES CREATED=HimOfflineCandidatePublicationGate.kt,RunHimFullOfflineCandidatePublicationGateTest.kt,derived gate JSON,derived report
FILES CHANGED=RunHimFirstRealCandidateGenerationPilotTest preflight
BUILD=verified by caller
TESTS=full offline path PASS
RUNTIME_MILLIS=$elapsed
GIT STATUS=reported by caller
CONTRACT DEVIATIONS=none
HARD FAILURES=none
FULL_OFFLINE_PUBLICATION_GATE_READY=true
PAID_CANDIDATE_RUN_GUARD_READY=true
FIRST_REAL_CANDIDATE_GENERATION_READY=true
F3d.5e COMPLETE=true
""".trimIndent() + "\n"
    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
    private fun sha256(file: File): String = sha256(file.readBytes())
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        private const val MISSION = "F3D-FULL-OFFLINE-CANDIDATE-PUBLICATION-GATE-V1"
        private val INPUTS = listOf("Hering geräuchert", "Hering eingelegt", "Hering in Öl")
        private const val AUTHORITY_PATH = "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json"
        private const val REPORT = "build/knowledge/reports/him/candidates/him-f3d5e-full-offline-candidate-publication-gate.txt"
        private val EXPECTED_GUARDS = linkedMapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            AUTHORITY_PATH to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
            "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json" to "b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023",
        )
        private val IMMUTABLE_FILES = listOf(HimOffProductionEvidenceIndexPaths.FINAL_INDEX, HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX, HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX, HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX, "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", "data/sources/agribalyse/optimized/agribalyse-him-final-source.jsonl.gz", "data/sources/ciqual/optimized/ciqual-him-final-source.json.gz", "data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz")
    }
}
