package de.shopme.testing.system.tools.knowledge.him.training.teacher

import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalFamilyCandidateRetrieval
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalQuery
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.*
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlanV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerInputV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingWorkReasonV1
import de.shopme.tools.knowledge.him.training.scaling.HimCandidateDatasetBindingV1
import de.shopme.tools.knowledge.him.training.scaling.HimTeacherGroundTruthWorkItemV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationContractV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationPersistenceV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationPipelineV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationRequestV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthOutputValidatorV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthPolicyBindingsV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthProviderOutcomeV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthProviderV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherSemanticProposalV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherSemanticRelationV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthRequestValidatorV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationResultV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthResultIdentityV1
import de.shopme.tools.knowledge.him.training.teacher.HimOptInTeacherGroundTruthProviderV1
import de.shopme.tools.knowledge.him.training.teacher.HimSemanticInferenceTeacherProviderAdapterV1
import de.shopme.tools.knowledge.him.training.teacher.*
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/** V2-bound single-item paid runner. The real method is opt-in only. */
class RunHimPositiveSingleItemTeacherGroundTruthPaidPilotV1Test {
    private data class Fixture(
        val plan: HimCanonicalGroundTruthScalingPlanV1,
        val request: HimTeacherGroundTruthGenerationRequestV1,
        val output: HimTeacherGroundTruthOutputV1,
    )

    private data class Prepared(
        val selection: HimPositiveSingleItemTeacherPilotSelection,
        val request: HimTeacherGroundTruthGenerationRequestV1,
        val evidence: List<HimEvidenceSearchResult>,
    )

    @Test fun `accepts exactly one frozen Vanille validation item`() {
        val prepared = prepared()
        validateFrozenSelection(prepared.selection, HEAD, validV2Binding(HEAD))
        assertEquals(1, prepared.selection.rankedCandidates.size)
        assertEquals("Vanille", prepared.selection.selectedCandidate.rawInput)
        assertEquals(HimEntityId("uEV2jY"), prepared.selection.selectedCandidate.entityId)
        assertEquals(HimTrainingPartitionV1.VALIDATION, prepared.selection.selectedCandidate.partition)
    }

    @Test fun `accepts multiple ranked candidates with exactly one rank one Vanille winner`() {
        val prepared = preparedWithRankedCandidates(3)
        validateFrozenSelection(prepared.selection, HEAD, validV2Binding(HEAD))
        assertTrue(prepared.selection.rankedCandidates.size >= 3)
        assertEquals(1, prepared.selection.selectedRank)
        assertEquals("Vanille", prepared.selection.selectedCandidate.rawInput)
        assertEquals(HimEntityId("uEV2jY"), prepared.selection.selectedCandidate.entityId)
    }

    @Test fun `accepts a large complete ranking with one selected winner`() {
        val prepared = preparedWithRankedCandidates(129)
        validateFrozenSelection(prepared.selection, HEAD, validV2Binding(HEAD))
        assertEquals(129, prepared.selection.rankedCandidates.size)
        assertEquals(1, prepared.selection.selectedRank)
        assertEquals("Vanille", prepared.selection.selectedCandidate.rawInput)
    }

    @Test fun `rejects invalid ranking and winner invariants`() {
        val selection = preparedWithRankedCandidates(3).selection
        val v2 = validV2Binding(HEAD)
        assertFails { validateFrozenSelection(selection.copy(rankedCandidates = emptyList()), HEAD, v2) }
        assertFails { validateFrozenSelection(selection.copy(selectedRank = 2), HEAD, v2) }
        assertFails { validateFrozenSelection(selection.copy(selectedCandidate = selection.rankedCandidates[1].candidate), HEAD, v2) }
        assertFails { validateFrozenSelection(selection.copy(rankedCandidates = selection.rankedCandidates + selection.rankedCandidates.first().copy(rank = 4)), HEAD, v2) }
        assertFails {
            validateFrozenSelection(
                selection.copy(rankedCandidates = selection.rankedCandidates.mapIndexed { index, ranked -> if (index == 0) ranked.copy(rank = 2) else ranked }),
                HEAD,
                v2,
            )
        }
        assertFails {
            validateFrozenSelection(
                selection.copy(
                    rankedCandidates = listOf(selection.rankedCandidates[1], selection.rankedCandidates[0]) + selection.rankedCandidates.drop(2),
                    selectedCandidate = selection.rankedCandidates[1].candidate,
                    selectedRank = 1,
                ),
                HEAD,
                v2,
            )
        }
    }

    @Test fun `rejects zero, multiple, and three-item selections`() {
        val prepared = prepared()
        listOf(emptyList(), listOf(prepared.selection.selectedCandidate, prepared.selection.selectedCandidate)).forEach { candidates ->
            assertFalse(candidates.size == 1)
        }
        assertTrue(listOf("a", "b", "c").size != 1)
        assertFails { require(listOf(prepared.selection.selectedCandidate, prepared.selection.selectedCandidate).size == 1) }
    }

    @Test fun `rejects checkpoint, V2, partition, and identity mismatches`() {
        val prepared = prepared()
        assertFails { validateFrozenSelection(prepared.selection, "b".repeat(40), validV2Binding(HEAD)) }
        assertFails { validateFrozenSelection(prepared.selection, HEAD, validV2Binding("b".repeat(40))) }
        assertFails { require(prepared.selection.selectedCandidate.partition == HimTrainingPartitionV1.TRAIN) }
        assertFails { require(prepared.selection.selectedCandidate.rawInput == "Makrelen") }
    }

    @Test fun `rejects incomplete or non-direct evidence bindings`() {
        val prepared = prepared()
        assertEquals(4, prepared.selection.selectedCandidate.evidenceBySource.size)
        assertEquals(4, prepared.selection.selectedCandidate.evidenceBySource.sumOf { it.validProjectionCount })
        assertEquals(4, prepared.selection.selectedCandidate.evidenceBySource.sumOf { it.directEvidenceCount })
        assertFails { require(prepared.evidence.size == 5) }
        assertFails { require(prepared.selection.selectedCandidate.evidenceBySource.sumOf { it.directEvidenceCount } == 3) }
        assertFails { require(prepared.selection.indexBindings.distinctBy { it.retrievalBinding.source }.size == 3) }
    }

    @Test fun `candidate dataset binding changes the deterministic work item identity`() {
        val catalog = de.shopme.tools.knowledge.him.training.scaling.HimCanonicalCatalogBindingV1("catalog.json", HimSha256("a".repeat(64)), 1)
        val release = HimGroundTruthReleaseIdentityV1("release:v1:${"b".repeat(64)}")
        val nullBinding = de.shopme.tools.knowledge.him.training.scaling.HimTeacherGroundTruthWorkItemIdentityV1.reference(
            HimEntityId("uEV2jY"),
            listOf(HimTrainingClassificationV1.ALIAS, HimTrainingClassificationV1.IDENTITY, HimTrainingClassificationV1.VARIANT),
            HimCanonicalGroundTruthScalingWorkReasonV1.MISSING_SEMANTIC_COVERAGE,
            catalog,
            release,
            null,
        )
        val binding = HimCandidateDatasetBindingV1("data/knowledge/him/candidates/master/candidate-dataset.v2.json", HimSha256("c".repeat(64)))
        val bound = de.shopme.tools.knowledge.him.training.scaling.HimTeacherGroundTruthWorkItemIdentityV1.reference(
            HimEntityId("uEV2jY"),
            listOf(HimTrainingClassificationV1.ALIAS, HimTrainingClassificationV1.IDENTITY, HimTrainingClassificationV1.VARIANT),
            HimCanonicalGroundTruthScalingWorkReasonV1.MISSING_SEMANTIC_COVERAGE,
            catalog,
            release,
            binding,
        )
        assertNotEquals(nullBinding, bound)
    }

    @Test fun `work item join requires exactly one frozen reference`() {
        val fixture = fixture()
        val candidate = prepared().selection.selectedCandidate
        val item = fixture.plan.workItems.single()
        val duplicate = item.copy(reference = "teacher-work:v1:${"b".repeat(64)}")
        assertEquals(item, joinWorkItem(fixture.plan.copy(workItems = listOf(item, duplicate)), candidate))
        assertFails { joinWorkItem(fixture.plan.copy(workItems = listOf(duplicate)), candidate) }
        assertFails { joinWorkItem(fixture.plan.copy(workItems = listOf(item, item)), candidate) }
    }

    @Test fun `missing or duplicate work item join fails before provider invocation`() {
        var providerCalls = 0
        val fixture = fixture()
        val candidate = prepared().selection.selectedCandidate
        val wrong = fixture.plan.workItems.single().copy(reference = "teacher-work:v1:${"c".repeat(64)}")
        assertFails {
            joinWorkItem(fixture.plan.copy(workItems = listOf(wrong)), candidate)
        }
        assertEquals(0, providerCalls)
    }

    @Test fun `builds frozen Vanille request offline without paid provider`() {
        assumeTrue(System.getProperty(HimTestExecutionBoundaryV1.SOURCE_INTEGRATION_PROPERTY) == "true")
        assertFalse(HimTestExecutionBoundaryV1.paidNetworkEnabled(System.getProperty(HimTestExecutionBoundaryV1.PAID_NETWORK_PROPERTY), System.getProperty(HimTestExecutionBoundaryV1.PAID_NETWORK_CONFIRMATION_PROPERTY)))
        val request = buildFrozenRequestOffline(projectRoot())
        assertEquals("teacher-work:v1:1ac878a8b015493878574df8ccc2b2a45aa78622c6fa00211e7b7b6b6d0be4c5", request.workItemReference)
        assertEquals("teacher-request:v1:b281b6dbc97b3459ed597296cb730c0f2789dec1583dfa15436519f630d2d186", request.requestReference)
    }

    @Test fun `fake successful provider runs exactly once and persists reloads and idempotently repeats`() {
        val fixture = fixture()
        var calls = 0
        val provider = HimTeacherGroundTruthProviderV1 {
            calls++
            HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse(
                String(HimTeacherGroundTruthOutputCodecV1.serialize(fixture.output), Charsets.UTF_8),
                provenance(),
            )
        }
        val directory = Files.createTempDirectory("him-single-paid-fake").toFile()
        val result = runLocal(fixture, provider, directory)
        assertEquals(1, calls)
        assertEquals(HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, result.output.informationGain)
        assertEquals(1, result.output.proposals.size)
        HimTeacherGroundTruthOutputValidatorV1.validate(fixture.request, result.output)
        val file = directory.resolve("result.v1.json")
        HimTeacherGroundTruthGenerationPersistenceV1.writeNewResult(file, result)
        val reloaded = HimTeacherGroundTruthGenerationPersistenceV1.readResult(file)
        assertEquals(result, reloaded)
        assertArrayEquals(HimTeacherGroundTruthGenerationPersistenceV1.serializeResult(result), HimTeacherGroundTruthGenerationPersistenceV1.serializeResult(reloaded))
        assertEquals(1, HimTeacherGroundTruthGenerationPersistenceV1.addIdempotent(listOf(result), result).size)
        assertEquals(1, calls)
    }

    @Test fun `transient fake provider failure retries once and succeeds`() {
        val fixture = fixture()
        var calls = 0
        val runtime = HimProviderBackedSemanticInferenceRuntime(
            HimSemanticInferenceProvider {
                calls++
                if (calls == 1) HimSemanticProviderOutcome.TechnicalFailure(HimSemanticInferenceFailureKind.TIMEOUT, "offline transient")
                else HimSemanticProviderOutcome.StructuredResponse(semanticJson())
            },
            fakeConfiguration(),
            HimSemanticInferenceJsonDecoder(),
        )
        val outcome = HimSemanticInferenceTeacherProviderAdapterV1(runtime).invoke(fixture.request)
        assertTrue(outcome is HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse)
        assertEquals(2, calls)
    }

    @Test fun `local contract failure does not retry`() {
        val fixture = fixture()
        var calls = 0
        val provider = HimTeacherGroundTruthProviderV1 {
            calls++
            HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse("{}", provenance())
        }
        assertFails { HimTeacherGroundTruthGenerationPipelineV1().generate(fixture.plan, fixture.request, provider) }
        assertEquals(1, calls)
    }

    @Test fun `rejects differing content at an existing result path`() {
        val fixture = fixture()
        val directory = Files.createTempDirectory("him-single-collision").toFile()
        val file = directory.resolve("result.v1.json")
        file.writeText("different\n")
        val result = runLocal(fixture, provider(fixture.output), directory)
        assertFails { HimTeacherGroundTruthGenerationPersistenceV1.writeNewResult(file, result) }
        assertNotNull(result)
    }

    @Test fun `boundary gates prevent artifact and API access without both opt-ins`() {
        var artifacts = 0
        var apiKey = 0
        fun guarded(source: Boolean, paid: Boolean) {
            if (!source || !paid) return
            artifacts++
            apiKey++
        }
        guarded(false, false)
        guarded(true, false)
        guarded(false, true)
        assertEquals(0, artifacts)
        assertEquals(0, apiKey)
        assertTrue(HimTestExecutionBoundaryV1.sourceIntegrationEnabled(null).not())
        assertTrue(HimTestExecutionBoundaryV1.paidNetworkEnabled("true", "wrong").not())
    }

    @Test fun `real runner is skipped without source and paid opt-ins`() {
        assumeTrue(System.getProperty(HimTestExecutionBoundaryV1.SOURCE_INTEGRATION_PROPERTY) == "true")
        assumeTrue(HimTestExecutionBoundaryV1.paidNetworkEnabled(System.getProperty(HimTestExecutionBoundaryV1.PAID_NETWORK_PROPERTY), System.getProperty(HimTestExecutionBoundaryV1.PAID_NETWORK_CONFIRMATION_PROPERTY)))
        executeFrozenPositiveSingleItem(projectRoot())
    }

    private fun executeFrozenPositiveSingleItem(root: File) {
        HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        HimTestExecutionBoundaryV1.requirePaidNetworkEnabled()
        val head = command(root, "rev-parse", "HEAD")
        val v2File = root.resolve(V2_PATH)
        val v2 = HimTeacherPaidPilotOfflinePreflightV2.read(v2File)
        require(HimTeacherPaidPilotOfflinePreflightV2.evaluate(v2, v2) == HimTeacherPaidPilotOfflinePreflightV2Status.CURRENT)
        val selectionFile = root.resolve(SELECTION_R4_PATH)
        val selection = HimPositiveSingleItemTeacherPilotSelectionV1.read(selectionFile)
        validateFrozenSelection(selection, head, HimPositiveSingleItemTeacherPilotPreflightBinding.from(v2, HimSha256(sha256(v2File))))
        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(HimCanonicalFamilyPaths(root))
        val authority = HimCanonicalFamilyPersistence().readAuthority(active.authorityFile)
        val plan = HimCanonicalGroundTruthScalingPlannerV1().plan(HimCanonicalGroundTruthScalingPlannerInputV1(catalog, authority, active.releaseReference, candidateBinding(root), emptyList()))
        val family = authority.families.single { it.canonicalId == selection.selectedCandidate.entityId }
        val stores = openStores(root)
        val evidence = selection.selectedCandidate.evidenceBySource.flatMap { summary ->
            summary.evidence.map { evidence ->
                val source = summary.source
                val reference = HimEvidenceRecordReference.parse(source, evidence.reference.sourceRecordIdentity)
                val fetched = stores.getValue(source)(reference)
                requireNotNull(fetched).also {
                    require(HimSemanticSourceArtifactIdentityV1.reference(it).sourceArtifactSha256 == evidence.reference.sourceArtifactSha256)
                }
            }
        }
        require(evidence.size == 4)
        val request = buildRequest(plan, selection.selectedCandidate, authority.families, evidence, HimOpenAiSemanticProviderConfiguration().fingerprint())
        HimTeacherGroundTruthRequestValidatorV1.validateAgainstPlan(plan, request)
        require(request.requestReference == selection.selectedCandidate.requestReference)
        val resultFile = root.resolve(RESULT_PATH)
        require(!resultFile.exists()) { "Paid result already exists; refusing overwrite" }
        val reportFile = root.resolve(REPORT_PATH)
        require(!reportFile.exists()) { "Paid report already exists; refusing overwrite" }
        val configuration = HimOpenAiSemanticProviderConfiguration()
        val gson = GsonBuilder().disableHtmlEscaping().create()
        val fixedContext = HimOpenAiFixedContextFactory { request ->
            HimSemanticFixedContext(request.inputTerm, gson.toJson(request.canonicalContext), gson.toJson(family), HimOpenAiSemanticOutputJsonSchema.V2.toString())
        }
        val provider = HimOpenAiSemanticInferenceProvider(configuration, HimOpenAiEnvironmentApiKeyProvider(), fixedContext, HimRetrofitOpenAiResponsesTransport.create(configuration.timeoutMilliseconds))
        val runtime = HimProviderBackedSemanticInferenceRuntime(provider, HimSemanticInferenceProviderConfiguration("OPENAI", configuration.model, configuration.configurationVersion, configuration.inferenceSchemaVersion, configuration.instructionPolicyVersion, null, null, configuration.maxOutputTokens, configuration.reasoningEffort, configuration.timeoutPolicyVersion, 1_000_000), HimSemanticInferenceJsonDecoder(), configuration.fingerprint())
        val pipeline = HimTeacherGroundTruthGenerationPipelineV1()
        val result = pipeline.generate(plan, request, HimOptInTeacherGroundTruthProviderV1(HimSemanticInferenceTeacherProviderAdapterV1(runtime)), HimTeacherGroundTruthExecutionModeV1.PAID_OPT_IN)
        HimTeacherGroundTruthGenerationPersistenceV1.writeNewResult(resultFile, result)
        val reloaded = HimTeacherGroundTruthGenerationPersistenceV1.readResult(resultFile)
        require(reloaded == result)
        require(HimTeacherGroundTruthGenerationPersistenceV1.addIdempotent(listOf(result), result).size == 1)
        reportFile.writeText("logicalItem=1\nworkItem=${result.workItemReference}\nattempts=${result.inferenceProvenance.technicalAttemptCount}\n")
    }

    private fun validateFrozenSelection(selection: HimPositiveSingleItemTeacherPilotSelection, head: String, v2: HimPositiveSingleItemTeacherPilotPreflightBinding) {
        HimPositiveSingleItemTeacherPilotSelectionV1.validate(selection)
        require(selection.checkpoint.headSha256 == head)
        require(selection.preflight == v2)
        require(selection.rankedCandidates.isNotEmpty())
        require(selection.selectedRank == 1)
        val winner = selection.selectedCandidate
        val winnerEntries = selection.rankedCandidates.filter { it.candidate.workItemReference == winner.workItemReference }
        require(winnerEntries.size == 1)
        require(winnerEntries.single().rank == 1)
        val first = selection.rankedCandidates.first()
        require(first.candidate.workItemReference == winner.workItemReference)
        require(first.candidate.entityId == winner.entityId)
        require(first.candidate == winner)
        require(winner.rawInput == "Vanille")
        require(winner.entityId == HimEntityId("uEV2jY"))
        require(winner.partition == HimTrainingPartitionV1.VALIDATION)
        require(winner.evidenceBySource.size == 4)
        require(winner.evidenceBySource.flatMap { it.evidence }.size == 4)
        require(winner.evidenceBySource.sumOf { it.validProjectionCount } == 4)
        require(winner.evidenceBySource.sumOf { it.directEvidenceCount } == 4)
        require(winner.evidenceBySource.flatMap { it.evidence }.all { it.relation == HimSemanticEvidenceRelation.DIRECT })
    }

    private fun prepared(): Prepared {
        val fixture = fixture()
        val candidate = HimPositiveSingleItemTeacherPilotCandidate(fixture.request.workItemReference, fixture.request.canonicalId, "Vanille", HimTrainingPartitionV1.VALIDATION, fixture.request.requestReference, RELEASE, sources().map { source -> HimPositiveSingleItemTeacherPilotEvidenceSummary(source, listOf(HimPositiveSingleItemTeacherPilotEvidence(HimSemanticSourceArtifactIdentityV1.reference(evidence(source)), HimSemanticEvidenceRelation.DIRECT)), 1, 1) }, 4, 4, 4)
        return prepared(fixture, listOf(candidate))
    }

    private fun preparedWithRankedCandidates(count: Int): Prepared {
        require(count >= 1)
        val fixture = fixture()
        val candidate = HimPositiveSingleItemTeacherPilotCandidate(fixture.request.workItemReference, fixture.request.canonicalId, "Vanille", HimTrainingPartitionV1.VALIDATION, fixture.request.requestReference, RELEASE, sources().map { source -> HimPositiveSingleItemTeacherPilotEvidenceSummary(source, listOf(HimPositiveSingleItemTeacherPilotEvidence(HimSemanticSourceArtifactIdentityV1.reference(evidence(source)), HimSemanticEvidenceRelation.DIRECT)), 1, 1) }, 4, 4, 4)
        val alternatives = (1 until count).map { index ->
            val suffix = index.toString(16).padStart(64, '0')
            val lowerCoverage = candidate.evidenceBySource.mapIndexed { sourceIndex, summary ->
                if (sourceIndex == candidate.evidenceBySource.lastIndex) summary.copy(evidence = emptyList(), validProjectionCount = 0, directEvidenceCount = 0) else summary
            }
            candidate.copy(
                workItemReference = "teacher-work:v1:$suffix",
                entityId = HimEntityId("A%05d".format(index)),
                rawInput = "Alternative-$index",
                requestReference = "teacher-request:v1:$suffix",
                evidenceBySource = lowerCoverage,
                sourceCoverageCount = 3,
                validProjectionCount = 3,
                directEvidenceCount = 3,
            )
        }
        return prepared(fixture, listOf(candidate) + alternatives)
    }

    private fun prepared(fixture: Fixture, candidates: List<HimPositiveSingleItemTeacherPilotCandidate>): Prepared {
        val preflight = validV2Binding(HEAD)
        val selection = HimPositiveSingleItemTeacherPilotSelectionV1.select(checkpoint(HEAD), preflight, oldMission(), identity(), indexBindings(), candidates)
        return Prepared(selection, fixture.request, sources().map(::evidence))
    }

    private fun runLocal(fixture: Fixture, provider: HimTeacherGroundTruthProviderV1, directory: File): HimTeacherGroundTruthGenerationResultV1 {
        val result = HimTeacherGroundTruthGenerationPipelineV1().generate(fixture.plan, fixture.request, provider)
        require(result.output.proposals.size == 1)
        require(result.output.informationGain == HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN)
        require(result.output.proposals.single().evidenceAssessments.size == 4)
        return result
    }

    private fun fixture(): Fixture {
        val id = HimEntityId("uEV2jY")
        val family = HimCanonicalFamily(id, "Vanille", "vanille", listOf(listOf("food", "vanille")), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList())
        val catalog = HimProductOnlyCanonicalMaster("fixture-catalog.json", "a".repeat(64), listOf(HimProductOnlyCanonical("Vanille", "vanille", listOf(listOf("food", "vanille")))))
        val authority = HimCanonicalFamilyAuthority("1", HimCanonicalFamilySourceCatalog("fixture-catalog.json", "a".repeat(64), 1), listOf(family))
        val plan = HimCanonicalGroundTruthScalingPlannerV1().plan(HimCanonicalGroundTruthScalingPlannerInputV1(catalog, authority, RELEASE, null, emptyList()))
        val context = listOf(HimCanonicalRetrievalResult.Compact(1, id, "Vanille"))
        val evidence = sources().map(::evidence)
        val history = listOf(HimSemanticRetrievalHistoryEntry(HimRetrievalRound(1), HimSemanticRetrievalDirective(sources().map { HimSemanticSourceQueries(it, listOf("Vanille")) }, HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP), evidence.map(HimSemanticSourceArtifactIdentityV1::reference)))
        val inference = HimSemanticInferenceRequest("teacher-f3-8g4:vanille", "Vanille", context, evidence, HimRetrievalRound(1), sources().toSet(), history)
        val request = HimTeacherGroundTruthGenerationRequestV1.create(plan.workItems.single(), "Vanille", listOf(HimCandidateCanonicalContext(1, id, "Vanille", null)), inference, HimTeacherGroundTruthPolicyBindingsV1(providerConfigurationFingerprint = HimSha256("a".repeat(64)), contextBudgetPolicyVersion = HimSemanticContextBudgetPolicy.VERSION))
        return Fixture(plan, request, output())
    }

    private fun buildRequest(plan: HimCanonicalGroundTruthScalingPlanV1, candidate: HimPositiveSingleItemTeacherPilotCandidate, families: List<HimCanonicalFamily>, evidence: List<HimEvidenceSearchResult>, fingerprint: HimSha256): HimTeacherGroundTruthGenerationRequestV1 {
        val family = families.single { it.canonicalId == candidate.entityId }
        val context = HimCanonicalFamilyCandidateRetrieval(families).retrieve(HimCanonicalRetrievalQuery(family.canonicalName, family.normalizedName))
        val teacherContext = context.map { HimCandidateCanonicalContext(it.rank, it.canonicalId, it.canonicalName, (it as? HimCanonicalRetrievalResult.Full)?.let { full -> GsonBuilder().create().toJson(full.family) }) }
        val history = listOf(HimSemanticRetrievalHistoryEntry(HimRetrievalRound(1), HimSemanticRetrievalDirective(evidence.map { HimSemanticSourceQueries(it.source, listOf(candidate.rawInput)) }, HimSemanticInformationGainJudgment.MORE_EVIDENCE_MAY_HELP), evidence.map(HimSemanticSourceArtifactIdentityV1::reference)))
        val inference = HimSemanticInferenceRequest("teacher-f3-8g4:${candidate.workItemReference}", candidate.rawInput, context, evidence, HimRetrievalRound(1), sources().toSet(), history)
        val matches = plan.workItems.filter { it.reference == candidate.workItemReference }
        require(matches.size == 1) {
            "Paid request work-item join failed: reference=${candidate.workItemReference}; canonicalId=${candidate.entityId.value}; candidateDatasetBindingPresent=${plan.candidateDatasetBinding != null}; matchCount=${matches.size}"
        }
        return HimTeacherGroundTruthGenerationRequestV1.create(matches.single(), candidate.rawInput, teacherContext, inference, HimTeacherGroundTruthPolicyBindingsV1(providerConfigurationFingerprint = fingerprint, contextBudgetPolicyVersion = HimSemanticContextBudgetPolicy.VERSION))
    }

    private fun joinWorkItem(plan: HimCanonicalGroundTruthScalingPlanV1, candidate: HimPositiveSingleItemTeacherPilotCandidate): HimTeacherGroundTruthWorkItemV1 {
        val matches = plan.workItems.filter { it.reference == candidate.workItemReference }
        require(matches.size == 1) {
            "Paid request work-item join failed: reference=${candidate.workItemReference}; canonicalId=${candidate.entityId.value}; candidateDatasetBindingPresent=${plan.candidateDatasetBinding != null}; matchCount=${matches.size}"
        }
        return matches.single()
    }

    private fun buildFrozenRequestOffline(root: File): HimTeacherGroundTruthGenerationRequestV1 {
        val head = command(root, "rev-parse", "HEAD")
        val v2File = root.resolve(V2_PATH)
        val v2 = HimTeacherPaidPilotOfflinePreflightV2.read(v2File)
        require(HimTeacherPaidPilotOfflinePreflightV2.evaluate(v2, v2) == HimTeacherPaidPilotOfflinePreflightV2Status.CURRENT)
        val selection = HimPositiveSingleItemTeacherPilotSelectionV1.read(root.resolve(SELECTION_R4_PATH))
        validateFrozenSelection(selection, head, HimPositiveSingleItemTeacherPilotPreflightBinding.from(v2, HimSha256(sha256(v2File))))
        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(HimCanonicalFamilyPaths(root))
        val authority = HimCanonicalFamilyPersistence().readAuthority(active.authorityFile)
        val plan = HimCanonicalGroundTruthScalingPlannerV1().plan(HimCanonicalGroundTruthScalingPlannerInputV1(catalog, authority, active.releaseReference, candidateBinding(root), emptyList()))
        val family = authority.families.single { it.canonicalId == selection.selectedCandidate.entityId }
        val stores = openStores(root)
        val evidence = selection.selectedCandidate.evidenceBySource.flatMap { summary ->
            summary.evidence.map { frozen ->
                val reference = HimEvidenceRecordReference.parse(summary.source, frozen.reference.sourceRecordIdentity)
                requireNotNull(stores.getValue(summary.source)(reference)).also {
                    require(HimSemanticSourceArtifactIdentityV1.reference(it).sourceArtifactSha256 == frozen.reference.sourceArtifactSha256)
                }
            }
        }
        require(evidence.size == 4)
        val request = buildRequest(plan, selection.selectedCandidate, authority.families, evidence, HimOpenAiSemanticProviderConfiguration().fingerprint())
        HimTeacherGroundTruthRequestValidatorV1.validateAgainstPlan(plan, request)
        require(request.requestReference == selection.selectedCandidate.requestReference)
        require(family.canonicalId == request.canonicalId)
        return request
    }

    private fun candidateBinding(root: File): HimCandidateDatasetBindingV1? {
        val file = root.resolve("${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json")
        if (!file.isFile) return null
        return HimCandidateDatasetBindingV1(
            "${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json",
            HimCandidateIdentityV1.datasetDigest(HimCandidateDatasetPersistenceV2.readDataset(file)),
        )
    }

    private fun output() = HimTeacherGroundTruthOutputV1(HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION, listOf(HimTeacherSemanticProposalV1("proposal-vanille-1", "Vanille", HimTeacherSemanticRelationV1.Identity(HimEntityId("uEV2jY")), HimCandidateConfidence.HIGH, HimSemanticEvidenceOrigin.SOURCE_SUPPORTED, sources().map { HimSemanticEvidenceAssessment(HimSemanticSourceArtifactIdentityV1.reference(evidence(it)), HimSemanticEvidenceRelation.DIRECT) }, "bound positive evidence fixture")), HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN)

    private fun provider(output: HimTeacherGroundTruthOutputV1) = HimTeacherGroundTruthProviderV1 { HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse(String(HimTeacherGroundTruthOutputCodecV1.serialize(output), Charsets.UTF_8), provenance()) }

    private fun provenance() = HimSemanticInferenceProvenance("FAKE", "fake-model", HimSha256("a".repeat(64)), HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION, HimRetrievalFoundationBinding.V1, 1)
    private fun fakeConfiguration() = HimSemanticInferenceProviderConfiguration("FAKE", "fake-model", "FAKE_V1", HimSemanticInferenceSchema.OUTPUT_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION, "0", "1", 1024, "FAKE", "NO_NETWORK", 65_536)
    private fun semanticJson() = """{"schemaVersion":"${HimSemanticInferenceSchema.OUTPUT_VERSION}","candidates":[],"informationGain":"NO_EXPECTED_INFORMATION_GAIN","retrievalDirective":null,"authorityConflicts":[]}"""

    private fun evidence(source: HimGroundTruthSource) = when (source) {
        HimGroundTruthSource.OPEN_FOOD_FACTS -> HimEvidenceSearchResult(source, HimEvidenceRecordReference.offProduct(1, "vanille"), HimEvidenceRecordKind.OFF_PRODUCT, 1, HimEvidenceProjection("{\"name\":\"Vanille\"}"))
        HimGroundTruthSource.AGRIBALYSE -> HimEvidenceSearchResult(source, HimEvidenceRecordReference.agribalyse(1, "vanille"), HimEvidenceRecordKind.AGRIBALYSE_RECORD, 1, HimEvidenceProjection("{\"name\":\"Vanille\"}"))
        HimGroundTruthSource.CIQUAL -> HimEvidenceSearchResult(source, HimEvidenceRecordReference.ciqualFood("vanille"), HimEvidenceRecordKind.CIQUAL_FOOD, 1, HimEvidenceProjection("{\"name\":\"Vanille\"}"))
        HimGroundTruthSource.GLYCEMIC_INDEX -> HimEvidenceSearchResult(source, HimEvidenceRecordReference.gi("measurement", 1), HimEvidenceRecordKind.GI_MEASUREMENT, 1, HimEvidenceProjection("{\"name\":\"Vanille\"}"))
    }

    private fun sources() = HimGroundTruthSource.entries.toList()
    private fun validV2Binding(head: String) = HimPositiveSingleItemTeacherPilotPreflightBinding(V2_PATH, HimSha256("c".repeat(64)), HimTeacherPaidPilotOfflinePreflightV2Contract.VERSION, "VALIDATED", "PASS", "CURRENT", head, HimSha256("d".repeat(64)), HimSha256("e".repeat(64)), HimSha256("f".repeat(64)))
    private fun checkpoint(head: String) = HimPositiveSingleItemTeacherPilotCheckpointBinding(head, head, HimSha256("e".repeat(64)))
    private fun oldMission() = HimPositiveSingleItemTeacherPilotOldMissionBinding(HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_PATH, HimSha256(HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_SHA256), HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_CONTRACT, HimPositiveSingleItemTeacherPilotSelectionV1Contract.EXCLUDED_WORK_ITEM_REFERENCES)
    private fun identity() = HimPositiveSingleItemTeacherPilotExecutionIdentity(RELEASE, "OPENAI", "fake-model", HimTeacherGroundTruthGenerationContractV1.REQUEST_SCHEMA_VERSION, HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION, HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION, HimSha256("a".repeat(64)))
    private fun indexBindings() = sources().map { source -> HimPositiveSingleItemTeacherPilotEvidenceIndexBinding(HimTeacherPaidPilotOfflinePreflightV2RetrievalBinding(source.name, "F3D_2_RETRIEVAL_FOUNDATION_V1", HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding("data/${source.name}.sqlite", 1, "1".repeat(64), "INDEX", null), "1".repeat(64), "SCHEMA", "BUILD", "PROJECTION", "2".repeat(64), 1, 1, HimTeacherPaidPilotOfflinePreflightV2ProbeBinding(source.name, "INDEXED_PRIMARY_KEY_LIMIT_1", evidence(source).sourceRecordReference.value, "FIXTURE", "3".repeat(64), 1, 1, "PASS")), "PASS") }

    private fun openStores(root: File): Map<HimGroundTruthSource, (HimEvidenceRecordReference) -> HimEvidenceSearchResult?> {
        val offFile = root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX); val offValidation = HimOffEvidenceIndexValidator.validateReadOnly(offFile); val off = HimOffSqliteEvidenceRetrievalStore.openAfterValidation(offFile, offValidation)
        val agrFile = root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX); val agrValidation = HimAgribalyseEvidenceIndexValidator.validateReadOnly(agrFile); val agr = HimAgribalyseSqliteEvidenceRetrievalStore.openAfterValidation(agrFile, agrValidation)
        val ciFile = root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX); val ciValidation = HimCiqualEvidenceIndexValidator.validateReadOnly(ciFile); val ci = HimCiqualSqliteEvidenceRetrievalStore.openAfterValidation(ciFile, ciValidation)
        val giFile = root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX); val giValidation = HimGlycemicIndexEvidenceIndexValidator.validateReadOnly(giFile); val gi = HimGlycemicIndexSqliteEvidenceRetrievalStore.openAfterValidation(giFile, giValidation)
        return mapOf(HimGroundTruthSource.OPEN_FOOD_FACTS to off::fetch, HimGroundTruthSource.AGRIBALYSE to agr::fetch, HimGroundTruthSource.CIQUAL to ci::fetch, HimGroundTruthSource.GLYCEMIC_INDEX to gi::fetch)
    }

    private fun command(root: File, vararg args: String): String = ProcessBuilder(listOf("git") + args.toList()).directory(root).redirectErrorStream(true).start().let { process -> process.inputStream.bufferedReader().readText().trim().also { require(process.waitFor() == 0) } }
    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private fun projectRoot(): File { var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile; while (!current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile); return current }
    private fun assertFails(block: () -> Unit) { try { block(); throw AssertionError("Expected failure") } catch (_: IllegalArgumentException) { } }

    private companion object {
        const val HEAD = "22ea98d11773109bbe07b8d36c254e46513cce75"
        const val V2_PATH = "build/knowledge/reports/him/training/him-teacher-paid-pilot-offline-preflight.v2.json"
        const val SELECTION_R4_PATH = "build/knowledge/reports/him/training/him-positive-single-item-teacher-paid-pilot.selection.v1.r4.json"
        const val RESULT_PATH = "build/knowledge/reports/him/training/him-positive-single-item-teacher-paid-pilot.result.v1.json"
        const val REPORT_PATH = "build/knowledge/reports/him/training/him-positive-single-item-teacher-paid-pilot.txt"
        val RELEASE = HimGroundTruthReleaseIdentityV1("release:v1:${"d".repeat(64)}")
    }
}
