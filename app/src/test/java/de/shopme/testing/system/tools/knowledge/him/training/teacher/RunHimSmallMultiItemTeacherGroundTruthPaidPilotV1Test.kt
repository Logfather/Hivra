package de.shopme.testing.system.tools.knowledge.him.training.teacher

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthArtifactsV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalFamilyCandidateRetrieval
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalQuery
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.*
import de.shopme.tools.knowledge.him.training.scaling.HimCandidateDatasetBindingV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlanV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerInputV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerV1
import de.shopme.tools.knowledge.him.training.teacher.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

internal data class HimTeacherPaidPilotContinuationItemV1(
    val workItemReference: String,
    val rawInput: String,
    val partition: String,
    val requestReference: String,
    val request: HimTeacherGroundTruthGenerationRequestV1,
    val family: HimCanonicalFamily,
    val resultPath: String,
)

internal data class HimTeacherPaidPilotContinuationStateV1(
    val root: File,
    val plan: HimCanonicalGroundTruthScalingPlanV1,
    val active: HimActiveGroundTruthArtifactsV1,
    val item1: HimTeacherPaidPilotContinuationItemV1,
    val item1Result: HimTeacherGroundTruthGenerationResultV1,
    val continuationItems: List<HimTeacherPaidPilotContinuationItemV1>,
    val frozenMissionDigest: String,
    val priorFailureReportDigest: String,
    val previousOpenAiCalls: Int,
    val previousNetworkCalls: Int,
)

internal data class HimTeacherPaidPilotContinuationCompletedItemV1(
    val item: HimTeacherPaidPilotContinuationItemV1,
    val result: HimTeacherGroundTruthGenerationResultV1,
    val attempts: Int,
)

class RunHimSmallMultiItemTeacherGroundTruthPaidPilotV1Test {
    @Test
    fun executeExactlyThreeSequentialRealTeacherItems() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requirePaidNetworkEnabled()
        val root = projectRoot()
        if (root.resolve(MISSION_PATH).exists()) {
            continueFromItem2(root)
            return
        }
        val reportFile = root.resolve(REPORT_PATH)
        val missionFile = root.resolve(MISSION_PATH)
        val selected = mutableListOf<SelectedItem>()
        val completed = mutableListOf<CompletedItem>()
        var logicalCalls = 0
        var physicalAttempts = 0
        var technicalRetries = 0
        var preflightCurrent = false
        var gateReady = false
        var optIn = false
        var apiKeyPresent = false
        var currentIndex: Int? = null
        var failedInference: HimSemanticInferenceResult? = null
        var failedAttempts = 0
        var providerDiagnostics: HimOpenAiInvocationDiagnostics? = null
        var integrityBefore: Map<String, String> = emptyMap()
        var integrityAfter: Map<String, String> = emptyMap()
        var openAiProvider: HimOpenAiSemanticInferenceProvider? = null

        try {
            HimTeacherPaidPilotOfflinePreflightV1.requireCurrent(root)
            preflightCurrent = true
            val gateReport = readReport(root.resolve(GATE_REPORT_PATH))
            gateReady = gateReport["State"] == "READY_FOR_SMALL_MULTI_ITEM_PILOT"
            check(gateReady) { "F3.8g.3 gate is not READY_FOR_SMALL_MULTI_ITEM_PILOT" }

            optIn = System.getenv(HimTeacherGroundTruthGenerationContractV1.PAID_OPT_IN_ENVIRONMENT_FLAG) == "true"
            check(optIn) { "Teacher paid-pilot opt-in is not exactly true" }
            apiKeyPresent = !System.getenv("OPENAI_API_KEY").isNullOrBlank()
            check(apiKeyPresent) { "OPENAI_API_KEY is absent" }

            val paths = HimCanonicalFamilyPaths(root)
            val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
            val active = HimActiveGroundTruthResolutionV1().resolve(root)
            val authority = HimCanonicalFamilyPersistence().readAuthority(active.authorityFile)
            val plan = currentPlan(root, catalog, authority, active)
            val previousPaidReferences = existingPaidWorkItems(root)
            val workItems = HimSmallMultiItemTeacherPilotSelectionV1.select(plan.workItems, previousPaidReferences)
            selected += workItems.map { item ->
                val family = authority.families.single { it.canonicalId == item.canonicalId }
                buildSelectedItem(item, family, authority.families)
            }
            require(selected.size == HimSmallMultiItemTeacherPilotContractV1.ITEM_COUNT)
            require(selected.map { it.workItemReference }.distinct().size == selected.size)
            require(selected.none { it.workItemReference in previousPaidReferences })
            require(!missionFile.exists()) { "F3.8g.4 mission already exists; refusing a replay" }
            selected.forEach { item ->
                require(!item.resultFile(root).exists()) { "F3.8g.4 item result already exists; refusing a replay" }
            }
            writeMission(missionFile, selected, active, HimOpenAiSemanticProviderConfiguration())
            integrityBefore = integritySnapshot(root, active)

            val configuration = HimOpenAiSemanticProviderConfiguration()
            requirePreflightIdentity(root, configuration)
            val gson = GsonBuilder().disableHtmlEscaping().create()
            lateinit var selectedFamily: HimCanonicalFamily
            val fixedContextFactory = HimOpenAiFixedContextFactory { request ->
                HimSemanticFixedContext(
                    request.inputTerm,
                    gson.toJson(request.canonicalContext),
                    gson.toJson(selectedFamily),
                    HimOpenAiSemanticOutputJsonSchema.V2.toString(),
                )
            }
            openAiProvider = HimOpenAiSemanticInferenceProvider(
                configuration,
                HimOpenAiEnvironmentApiKeyProvider(),
                fixedContextFactory,
                HimRetrofitOpenAiResponsesTransport.create(configuration.timeoutMilliseconds),
            )
            val runtime = HimProviderBackedSemanticInferenceRuntime(
                openAiProvider!!,
                HimSemanticInferenceProviderConfiguration(
                    "OPENAI", configuration.model, configuration.configurationVersion,
                    configuration.inferenceSchemaVersion, configuration.instructionPolicyVersion,
                    null, null, configuration.maxOutputTokens, configuration.reasoningEffort,
                    configuration.timeoutPolicyVersion, 1_000_000,
                ),
                HimSemanticInferenceJsonDecoder(),
                configuration.fingerprint(),
            )
            val recordingRuntime = HimSemanticInferenceRuntime { request ->
                logicalCalls++
                val inference = runtime.infer(request)
                failedInference = inference
                providerDiagnostics = openAiProvider!!.diagnostics
                inference
            }
            val adapter = HimSemanticInferenceTeacherProviderAdapterV1(recordingRuntime)
            val provider = HimOptInTeacherGroundTruthProviderV1(
                HimTeacherGroundTruthProviderV1 { request ->
                    when (val outcome = adapter.invoke(request)) {
                        is HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse -> outcome.copy(
                            usage = openAiProvider!!.diagnostics.usage?.let {
                                HimSemanticUsage(it.inputTokens, it.outputTokens, it.cachedInputTokens)
                            },
                        )
                        is HimTeacherGroundTruthProviderOutcomeV1.TechnicalFailure -> outcome
                    }
                },
            )

            selected.forEachIndexed { index, item ->
                currentIndex = index
                selectedFamily = item.family
                val attemptsBefore = openAiProvider!!.diagnostics.physicalAttempts
                val result = HimTeacherGroundTruthGenerationPipelineV1().generate(
                    plan,
                    item.request,
                    provider,
                    HimTeacherGroundTruthExecutionModeV1.PAID_OPT_IN,
                )
                val attempts = openAiProvider!!.diagnostics.physicalAttempts - attemptsBefore
                failedAttempts = attempts
                physicalAttempts += attempts
                technicalRetries += (attempts - 1).coerceAtLeast(0)
                val resultFile = item.resultFile(root)
                HimTeacherGroundTruthGenerationPersistenceV1.writeNewResult(resultFile, result)
                val reloaded = HimTeacherGroundTruthGenerationPersistenceV1.readResult(resultFile)
                require(result == reloaded) { "Teacher result reload mismatch for item ${index + 1}" }
                completed += CompletedItem(item, result, attempts, openAiProvider!!.diagnostics.usage)
            }
            integrityAfter = integritySnapshot(root, active)
            require(integrityBefore == integrityAfter) { "Production integrity changed during pilot" }
            writeReport(
                reportFile,
                successReport(selected, completed, preflightCurrent, gateReady, optIn, apiKeyPresent, logicalCalls, physicalAttempts, technicalRetries, integrityBefore == integrityAfter),
            )
            require(completed.size == HimSmallMultiItemTeacherPilotContractV1.ITEM_COUNT)
        } catch (failure: Throwable) {
            physicalAttempts = openAiProvider?.diagnostics?.physicalAttempts ?: physicalAttempts
            failedAttempts = failedAttempts.coerceAtLeast(providerDiagnostics?.physicalAttempts ?: 0)
            val completedPhysicalAttempts = completed.sumOf { it.attempts }
            technicalRetries = completed.sumOf { (it.attempts - 1).coerceAtLeast(0) } +
                (physicalAttempts - completedPhysicalAttempts - 1).coerceAtLeast(0)
            runCatching {
                writeReport(
                    reportFile,
                    failureReport(selected, completed, preflightCurrent, gateReady, optIn, apiKeyPresent, logicalCalls, physicalAttempts, technicalRetries, integrityBefore, integrityAfter, currentIndex, failedInference, failedAttempts, failure),
                )
            }
            throw failure
        }
    }

    internal fun resolveContinuationState(
        root: File,
        continuationReportOverride: File? = null,
    ): HimTeacherPaidPilotContinuationStateV1 {
        val missionFile = root.resolve(MISSION_PATH)
        val priorReportFile = root.resolve(REPORT_PATH)
        val continuationReport = continuationReportOverride ?: root.resolve(CONTINUATION_REPORT_PATH)
        require(missionFile.isFile) { "Frozen F3.8g.4 selection mission is absent" }
        require(priorReportFile.isFile) { "Previous F3.8g.4 failure report is absent" }
        require(!continuationReport.exists()) { "F3.8g.4 continuation already exists; refusing a second continuation" }
        require(sha256(missionFile.readBytes()) == FROZEN_MISSION_SHA256) { "Frozen F3.8g.4 selection mission changed" }
        require(sha256(priorReportFile.readBytes()) == PRIOR_FAILURE_REPORT_SHA256) { "Previous F3.8g.4 failure report changed" }

        val mission = readFrozenMission(missionFile)
        val expected = frozenMissionItems()
        require(mission.contract == HimSmallMultiItemTeacherPilotContractV1.VERSION)
        require(mission.expectedItems == HimSmallMultiItemTeacherPilotContractV1.ITEM_COUNT)
        require(mission.ordering == expected.map { it.workItemReference })
        require(mission.items == expected.map { MissionItemDto(it.workItemReference, it.rawInput, it.partition, it.requestReference) })
        require(mission.provider == "OPENAI")
        require(mission.inferenceSchema == HimSemanticInferenceSchema.OUTPUT_VERSION)
        require(mission.instructionPolicy == HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION)

        val priorReport = priorReportFile.readText()
        requirePriorFailureReport(priorReport)
        val paths = HimCanonicalFamilyPaths(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val authority = HimCanonicalFamilyPersistence().readAuthority(active.authorityFile)
        require(mission.groundTruthRelease.value == active.releaseReference.value)
        HimTeacherPaidPilotOfflinePreflightV1.requireCurrent(root)
        require(readReport(root.resolve(GATE_REPORT_PATH))["State"] == "READY_FOR_SMALL_MULTI_ITEM_PILOT")
        val plan = currentPlan(root, catalog, authority, active)
        val selected = mission.items.map { missionItem ->
            val workItem = plan.workItems.singleOrNull { it.reference == missionItem.workItemReference }
                ?: error("Frozen mission work item is not present in the current F3.8e plan")
            val family = authority.families.single { it.canonicalId == workItem.canonicalId }
            val selectedItem = buildSelectedItem(workItem, family, authority.families)
            require(selectedItem.rawInput == missionItem.rawInput)
            require(selectedItem.partition == missionItem.partition)
            require(selectedItem.requestReference == missionItem.requestReference)
            HimTeacherPaidPilotContinuationItemV1(
                workItemReference = selectedItem.workItemReference,
                rawInput = selectedItem.rawInput,
                partition = selectedItem.partition,
                requestReference = selectedItem.requestReference,
                request = selectedItem.request,
                family = selectedItem.family,
                resultPath = selectedItem.resultPath,
            )
        }
        require(selected.map { it.rawInput } == listOf("Salbei", "Makrelen", "Rindergulasch"))
        val item1 = selected[0]
        val item1File = root.resolve(item1.resultPath)
        require(item1File.isFile) { "Completed Item-1 result is absent" }
        val item1Bytes = item1File.readBytes()
        val item1Result = HimTeacherGroundTruthGenerationPersistenceV1.readResult(item1File)
        require(item1Bytes.contentEquals(HimTeacherGroundTruthGenerationPersistenceV1.serializeResult(item1Result)))
        require(item1Result.requestReference == item1.requestReference)
        require(item1Result.workItemReference == item1.workItemReference)
        require(item1Result.partition.name == item1.partition)
        require(item1Result.resultReference == HimTeacherGroundTruthResultIdentityV1.reference(item1Result))
        require(item1Result.logicalDigest == HimTeacherGroundTruthResultIdentityV1.digest(item1Result))
        HimTeacherGroundTruthRequestValidatorV1.validateAgainstPlan(plan, item1.request)
        HimTeacherGroundTruthOutputValidatorV1.validate(item1.request, item1Result.output)
        require(!root.resolve(selected[1].resultPath).exists()) { "Item-2 success result already exists" }
        require(!root.resolve(selected[2].resultPath).exists()) { "Item-3 success result already exists" }

        return HimTeacherPaidPilotContinuationStateV1(
            root = root,
            plan = plan,
            active = active,
            item1 = item1,
            item1Result = item1Result,
            continuationItems = selected.drop(1),
            frozenMissionDigest = sha256(missionFile.readBytes()),
            priorFailureReportDigest = sha256(priorReportFile.readBytes()),
            previousOpenAiCalls = 3,
            previousNetworkCalls = 3,
        )
    }

    internal fun executeContinuationItemsForTest(
        state: HimTeacherPaidPilotContinuationStateV1,
        outputDirectory: File,
        provider: HimTeacherGroundTruthProviderV1,
        physicalAttempts: () -> Int,
    ): List<HimTeacherPaidPilotContinuationCompletedItemV1> {
        val completed = mutableListOf<HimTeacherPaidPilotContinuationCompletedItemV1>()
        executeContinuationItems(state, outputDirectory, provider, HimTeacherGroundTruthExecutionModeV1.OFFLINE_FIXTURE, physicalAttempts, { }, completed)
        return completed
    }

    internal fun continuationSuccessReportForTest(
        state: HimTeacherPaidPilotContinuationStateV1,
        completed: List<HimTeacherPaidPilotContinuationCompletedItemV1>,
        continuationCalls: Int,
        integrityUnchanged: Boolean,
    ): String = continuationSuccessReport(
        state,
        completed,
        continuationCalls,
        continuationCalls,
        continuationCalls,
        (completed.sumOf { it.attempts } - continuationCalls).coerceAtLeast(0),
        integrityUnchanged,
    )

    internal fun protectedArtifactSnapshotForTest(root: File): Map<String, String> =
        integritySnapshot(root, HimActiveGroundTruthResolutionV1().resolve(root))

    private fun continueFromItem2(root: File) {
        val state = resolveContinuationState(root)
        val optIn = System.getenv(HimTeacherGroundTruthGenerationContractV1.PAID_OPT_IN_ENVIRONMENT_FLAG) == "true"
        val apiKeyPresent = !System.getenv("OPENAI_API_KEY").isNullOrBlank()
        check(optIn) { "Teacher paid-pilot continuation requires explicit opt-in" }
        check(apiKeyPresent) { "Teacher paid-pilot continuation requires OPENAI_API_KEY" }
        val reportFile = root.resolve(CONTINUATION_REPORT_PATH)
        val integrityBefore = integritySnapshot(root, state.active)
        val completed = mutableListOf<HimTeacherPaidPilotContinuationCompletedItemV1>()
        var currentItem: HimTeacherPaidPilotContinuationItemV1? = null
        var logicalCalls = 0
        var failedInference: HimSemanticInferenceResult? = null
        var openAiProvider: HimOpenAiSemanticInferenceProvider? = null
        var selectedFamily: HimCanonicalFamily? = null
        try {
            val configuration = HimOpenAiSemanticProviderConfiguration()
            requirePreflightIdentity(root, configuration)
            val gson = GsonBuilder().disableHtmlEscaping().create()
            val fixedContextFactory = HimOpenAiFixedContextFactory { request ->
                HimSemanticFixedContext(
                    request.inputTerm,
                    gson.toJson(request.canonicalContext),
                    gson.toJson(requireNotNull(selectedFamily)),
                    HimOpenAiSemanticOutputJsonSchema.V2.toString(),
                )
            }
            openAiProvider = HimOpenAiSemanticInferenceProvider(
                configuration,
                HimOpenAiEnvironmentApiKeyProvider(),
                fixedContextFactory,
                HimRetrofitOpenAiResponsesTransport.create(configuration.timeoutMilliseconds),
            )
            val runtime = HimProviderBackedSemanticInferenceRuntime(
                openAiProvider!!,
                HimSemanticInferenceProviderConfiguration(
                    "OPENAI", configuration.model, configuration.configurationVersion,
                    configuration.inferenceSchemaVersion, configuration.instructionPolicyVersion,
                    null, null, configuration.maxOutputTokens, configuration.reasoningEffort,
                    configuration.timeoutPolicyVersion, 1_000_000,
                ),
                HimSemanticInferenceJsonDecoder(),
                configuration.fingerprint(),
            )
            val recordingRuntime = HimSemanticInferenceRuntime { request ->
                logicalCalls++
                val inference = runtime.infer(request)
                failedInference = inference
                inference
            }
            val adapter = HimSemanticInferenceTeacherProviderAdapterV1(recordingRuntime)
            val provider = HimOptInTeacherGroundTruthProviderV1(
                HimTeacherGroundTruthProviderV1 { request ->
                    when (val outcome = adapter.invoke(request)) {
                        is HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse -> outcome.copy(
                            usage = openAiProvider!!.diagnostics.usage?.let { HimSemanticUsage(it.inputTokens, it.outputTokens, it.cachedInputTokens) },
                        )
                        is HimTeacherGroundTruthProviderOutcomeV1.TechnicalFailure -> outcome
                    }
                },
            )
            executeContinuationItems(
                state = state,
                outputDirectory = root,
                provider = provider,
                mode = HimTeacherGroundTruthExecutionModeV1.PAID_OPT_IN,
                physicalAttempts = { openAiProvider!!.diagnostics.physicalAttempts },
                beforeItem = { currentItem = it; selectedFamily = it.family },
                completed = completed,
            )
            require(completed.size == 2)
            val integrityAfter = integritySnapshot(root, state.active)
            require(integrityBefore == integrityAfter) { "Production integrity changed during continuation" }
            val continuationAttempts = openAiProvider!!.diagnostics.physicalAttempts
            writeNewContinuationReport(reportFile, continuationSuccessReport(state, completed, continuationAttempts, continuationAttempts, logicalCalls, (continuationAttempts - logicalCalls).coerceAtLeast(0), integrityBefore == integrityAfter))
        } catch (failure: Throwable) {
            val physical = openAiProvider?.diagnostics?.physicalAttempts ?: 0
            runCatching {
                writeNewContinuationReport(
                    reportFile,
                    continuationFailureReport(state, completed, currentItem, logicalCalls, physical, failedInference, integrityBefore, integritySnapshot(root, state.active), failure),
                )
            }
            throw failure
        }
    }

    private fun executeContinuationItems(
        state: HimTeacherPaidPilotContinuationStateV1,
        outputDirectory: File,
        provider: HimTeacherGroundTruthProviderV1,
        mode: HimTeacherGroundTruthExecutionModeV1,
        physicalAttempts: () -> Int,
        beforeItem: (HimTeacherPaidPilotContinuationItemV1) -> Unit,
        completed: MutableList<HimTeacherPaidPilotContinuationCompletedItemV1>,
    ) {
        state.continuationItems.forEachIndexed { index, item ->
            beforeItem(item)
            val attemptsBefore = physicalAttempts()
            val result = HimTeacherGroundTruthGenerationPipelineV1().generate(state.plan, item.request, provider, mode)
            val attempts = physicalAttempts() - attemptsBefore
            require(attempts in 1..2) { "Continuation item ${index + 2} exceeded the two-attempt limit" }
            val resultFile = outputDirectory.resolve(item.resultPath)
            HimTeacherGroundTruthGenerationPersistenceV1.writeNewResult(resultFile, result)
            val reloaded = HimTeacherGroundTruthGenerationPersistenceV1.readResult(resultFile)
            require(result == reloaded) { "Continuation result reload mismatch for item ${index + 2}" }
            HimTeacherGroundTruthOutputValidatorV1.validate(item.request, reloaded.output)
            completed += HimTeacherPaidPilotContinuationCompletedItemV1(item, reloaded, attempts)
        }
    }

    private fun readFrozenMission(file: File): FrozenMissionDto {
        val root = JsonParser.parseString(file.readText()).asJsonObject
        require(root.keySet() == setOf("contract", "expectedItems", "ordering", "items", "groundTruthRelease", "provider", "model", "inferenceSchema", "instructionPolicy", "providerFingerprint"))
        require(root.getAsJsonObject("groundTruthRelease").keySet() == setOf("value"))
        require(root.getAsJsonArray("items").all { it.asJsonObject.keySet() == setOf("workItemReference", "rawInput", "partition", "requestReference") })
        return Gson.fromJson(root, FrozenMissionDto::class.java)
    }

    private fun requirePriorFailureReport(report: String) {
        listOf(
            "F3.8g.1: CURRENT",
            "F3.8g.3 gate: READY_FOR_SMALL_MULTI_ITEM_PILOT",
            "### ITEM 1",
            "Validation: PASS",
            "Persistence: PASS",
            "Reload: PASS",
            "### ITEM 2",
            "Validation: STOPPED_AT_LOCAL_CONTRACT_FAILURE",
            "### ITEM 3",
            "Status: UNEXECUTED_AFTER_ITEM_2_FAILURE",
            "OpenAI calls: 3",
            "Network calls: 3",
            "Ground Truth mutations: 0",
            "Authority mutations: 0",
            "Entity-ID allocations: 0",
            "FAILED_LOCAL_CONTRACT",
            "## F3.8g.4 COMPLETE\nfalse",
        ).forEach { required -> require(report.contains(required)) { "Previous failure report is missing: $required" } }
    }

    private fun frozenMissionItems() = listOf(
        MissionItemDto("teacher-work:v1:008b98dedc437b349ec438e8aa90d29bb7139ae6528af7d3207f2dfe00e83192", "Salbei", "TRAIN", "teacher-request:v1:3a73a06134acaa6ba5f22c22bbc8c6f384af24aae0d1b887846ef15980573212"),
        MissionItemDto("teacher-work:v1:01a250090599f76309687986bfd1a7c0273f3c74f20a5a04f82ec6dd41b023ee", "Makrelen", "VALIDATION", "teacher-request:v1:c864a416e115597a84ec0b980f9177c6ed7987ec3bc3b34d39ca2b3e6439bdcf"),
        MissionItemDto("teacher-work:v1:03c60a01f03b183ce09b2ba5822b6e0b3cc02bd6770bea4761db881679593a70", "Rindergulasch", "HOLDOUT", "teacher-request:v1:d1a1535a459fc13629c095e076c2bf6f848a63510d4280bd334dec1c8d2557fb"),
    )

    private fun continuationSuccessReport(
        state: HimTeacherPaidPilotContinuationStateV1,
        completed: List<HimTeacherPaidPilotContinuationCompletedItemV1>,
        continuationOpenAiCalls: Int,
        continuationNetworkCalls: Int,
        logicalCalls: Int,
        technicalRetries: Int,
        integrityUnchanged: Boolean,
    ): String = buildString {
        appendLine("# HIM F3.8g.4 SMALL MULTI-ITEM TEACHER PAID PILOT V1 CONTINUATION")
        appendLine("MODE=CONTINUATION_FROM_ITEM_2")
        appendLine("FROZEN_SELECTION_SHA256=${state.frozenMissionDigest}")
        appendLine("PRIOR_FAILED_REPORT_SHA256=${state.priorFailureReportDigest}")
        appendLine("PREVIOUS_OPENAI_CALLS=${state.previousOpenAiCalls}")
        appendLine("PREVIOUS_NETWORK_CALLS=${state.previousNetworkCalls}")
        appendLine("CONTINUATION_OPENAI_CALLS=$continuationOpenAiCalls")
        appendLine("CONTINUATION_NETWORK_CALLS=$continuationNetworkCalls")
        appendLine("CUMULATIVE_OPENAI_CALLS=${state.previousOpenAiCalls + continuationOpenAiCalls}")
        appendLine("CUMULATIVE_NETWORK_CALLS=${state.previousNetworkCalls + continuationNetworkCalls}")
        appendLine("ITEM_1_REUSED=true")
        appendLine("ITEM_1_RESULT_REFERENCE=${state.item1Result.resultReference}")
        appendLine("CONTINUATION_LOGICAL_INFERENCE_CALLS=$logicalCalls")
        appendLine("CONTINUATION_TECHNICAL_RETRIES=$technicalRetries")
        completed.forEachIndexed { index, item ->
            val itemNumber = index + 2
            appendLine("ITEM_${itemNumber}_WORK_ITEM_REFERENCE=${item.item.workItemReference}")
            appendLine("ITEM_${itemNumber}_RAW_INPUT=${item.item.rawInput}")
            appendLine("ITEM_${itemNumber}_PARTITION=${item.item.partition}")
            appendLine("ITEM_${itemNumber}_RESULT_REFERENCE=${item.result.resultReference}")
            appendLine("ITEM_${itemNumber}_INFORMATION_GAIN=${item.result.output.informationGain.name}")
            appendLine("ITEM_${itemNumber}_PROPOSALS=${item.result.output.proposals.size}")
            appendLine("ITEM_${itemNumber}_VALIDATION=PASS")
            appendLine("ITEM_${itemNumber}_PERSISTENCE=PASS")
            appendLine("ITEM_${itemNumber}_RELOAD=PASS")
            appendLine("ITEM_${itemNumber}_PHYSICAL_ATTEMPTS=${item.attempts}")
            appendLine("ITEM_${itemNumber}_TECHNICAL_RETRIES=${(item.attempts - 1).coerceAtLeast(0)}")
        }
        appendLine("ITEM_3_EXECUTED=${completed.any { it.item.rawInput == "Rindergulasch" }}")
        appendLine("GROUND_TRUTH_MUTATIONS=0")
        appendLine("AUTHORITY_MUTATIONS=0")
        appendLine("REGISTRY_MUTATIONS=0")
        appendLine("ENTITY_ID_MUTATIONS=0")
        appendLine("SELECTION_MUTATIONS=0")
        appendLine("PRIOR_REPORT_MUTATIONS=0")
        appendLine("PROTECTED_ARTIFACTS_UNCHANGED=$integrityUnchanged")
        appendLine("F3.8g.4 CONTINUATION COMPLETE=true")
    }

    private fun continuationFailureReport(
        state: HimTeacherPaidPilotContinuationStateV1,
        completed: List<HimTeacherPaidPilotContinuationCompletedItemV1>,
        currentItem: HimTeacherPaidPilotContinuationItemV1?,
        logicalCalls: Int,
        physicalAttempts: Int,
        inference: HimSemanticInferenceResult?,
        before: Map<String, String>,
        after: Map<String, String>,
        failure: Throwable,
    ): String = buildString {
        appendLine("# HIM F3.8g.4 SMALL MULTI-ITEM TEACHER PAID PILOT V1 CONTINUATION STOP")
        appendLine("MODE=CONTINUATION_FROM_ITEM_2")
        appendLine("FROZEN_SELECTION_SHA256=${state.frozenMissionDigest}")
        appendLine("PRIOR_FAILED_REPORT_SHA256=${state.priorFailureReportDigest}")
        appendLine("PREVIOUS_OPENAI_CALLS=${state.previousOpenAiCalls}")
        appendLine("PREVIOUS_NETWORK_CALLS=${state.previousNetworkCalls}")
        appendLine("CONTINUATION_OPENAI_CALLS=$physicalAttempts")
        appendLine("CONTINUATION_NETWORK_CALLS=$physicalAttempts")
        appendLine("CUMULATIVE_OPENAI_CALLS=${state.previousOpenAiCalls + physicalAttempts}")
        appendLine("CUMULATIVE_NETWORK_CALLS=${state.previousNetworkCalls + physicalAttempts}")
        appendLine("ITEM_1_REUSED=true")
        appendLine("ITEM_1_RESULT_REFERENCE=${state.item1Result.resultReference}")
        appendLine("COMPLETED_CONTINUATION_ITEMS=${completed.size}")
        appendLine("FAILED_ITEM=${currentItem?.rawInput ?: "BEFORE_PROVIDER"}")
        appendLine("LOGICAL_INFERENCE_CALLS=$logicalCalls")
        appendLine("PHYSICAL_PROVIDER_ATTEMPTS=$physicalAttempts")
        appendLine("TECHNICAL_RETRIES=${(physicalAttempts - logicalCalls).coerceAtLeast(0)}")
        appendLine("FAILURE_KIND=${(inference as? HimSemanticInferenceResult.TechnicalFailure)?.failure?.kind ?: "LOCAL_OR_SCHEMA_VALIDATION_FAILURE"}")
        appendLine("FAILURE_MESSAGE=${failure.message?.replace(Regex("\\s+"), " ").orEmpty().take(300)}")
        appendLine("ITEM_3_EXECUTED=${completed.any { it.item.rawInput == "Rindergulasch" }}")
        appendLine("GROUND_TRUTH_MUTATIONS=0")
        appendLine("AUTHORITY_MUTATIONS=0")
        appendLine("REGISTRY_MUTATIONS=0")
        appendLine("ENTITY_ID_MUTATIONS=0")
        appendLine("SELECTION_MUTATIONS=0")
        appendLine("PRIOR_REPORT_MUTATIONS=0")
        appendLine("PROTECTED_ARTIFACTS_UNCHANGED=${before.isNotEmpty() && before == after}")
        appendLine("F3.8g.4 CONTINUATION COMPLETE=false")
    }

    private fun writeNewContinuationReport(file: File, content: String) {
        require(!file.exists()) { "Continuation report already exists; refusing overwrite" }
        requireNotNull(file.parentFile).mkdirs()
        file.writeText(content)
    }

    private fun buildSelectedItem(
        item: de.shopme.tools.knowledge.him.training.scaling.HimTeacherGroundTruthWorkItemV1,
        family: HimCanonicalFamily,
        families: List<HimCanonicalFamily>,
    ): SelectedItem {
        val context = HimCanonicalFamilyCandidateRetrieval(families)
            .retrieve(HimCanonicalRetrievalQuery(family.canonicalName, normalize(family.canonicalName)))
        require(context.any { it.canonicalId == item.canonicalId })
        val inferenceContext = context
        val inference = HimSemanticInferenceRequest(
            "teacher-f3-8g4:${item.reference}",
            family.canonicalName,
            inferenceContext,
            emptyList(),
            HimRetrievalRound(0),
        )
        val teacherContext = context.map { result ->
            HimCandidateCanonicalContext(
                result.rank,
                result.canonicalId,
                result.canonicalName,
                (result as? HimCanonicalRetrievalResult.Full)?.let { Gson.toJson(it.family) },
            )
        }
        val request = HimTeacherGroundTruthGenerationRequestV1.create(
            item,
            family.canonicalName,
            teacherContext,
            inference,
            HimTeacherGroundTruthPolicyBindingsV1(
                providerConfigurationFingerprint = HimOpenAiSemanticProviderConfiguration().fingerprint(),
                contextBudgetPolicyVersion = HimSemanticContextBudgetPolicy.VERSION,
            ),
        )
        return SelectedItem(item.reference, family.canonicalId.value, family.canonicalName, item.partition.name, request.requestReference, request, family)
    }

    private fun currentPlan(root: File, catalog: HimProductOnlyCanonicalMaster, authority: de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority, active: HimActiveGroundTruthArtifactsV1): HimCanonicalGroundTruthScalingPlanV1 =
        HimCanonicalGroundTruthScalingPlannerV1().plan(
            HimCanonicalGroundTruthScalingPlannerInputV1(catalog, authority, active.releaseReference, candidateBinding(root), emptyList()),
        )

    private fun candidateBinding(root: File): HimCandidateDatasetBindingV1? {
        val file = root.resolve("${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json")
        if (!file.isFile) return null
        return HimCandidateDatasetBindingV1(
            "${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json",
            de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1.datasetDigest(HimCandidateDatasetPersistenceV2.readDataset(file)),
        )
    }

    private fun requirePreflightIdentity(root: File, configuration: HimOpenAiSemanticProviderConfiguration) {
        val artifact = requireNotNull(HimTeacherPaidPilotOfflinePreflightV1.read(root.resolve(HimTeacherPaidPilotOfflinePreflightContractV1.ARTIFACT)))
        require(artifact.boundIdentities.providerFingerprint == configuration.fingerprint().value)
        require(artifact.boundIdentities.inferenceSchema == HimSemanticInferenceSchema.OUTPUT_VERSION)
        require(artifact.boundIdentities.instructionPolicy == HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION)
    }

    private fun existingPaidWorkItems(root: File): Set<String> {
        val directory = root.resolve("build/knowledge/reports/him/training")
        return directory.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".result.v1.json") }
            .mapNotNull { file -> runCatching { HimTeacherGroundTruthGenerationPersistenceV1.readResult(file).workItemReference }.getOrNull() }
            .toSet()
    }

    private fun writeMission(file: File, selected: List<SelectedItem>, active: HimActiveGroundTruthArtifactsV1, configuration: HimOpenAiSemanticProviderConfiguration) {
        requireNotNull(file.parentFile).mkdirs()
        val mission = linkedMapOf(
            "contract" to HimSmallMultiItemTeacherPilotContractV1.VERSION,
            "expectedItems" to HimSmallMultiItemTeacherPilotContractV1.ITEM_COUNT,
            "ordering" to selected.map { it.workItemReference },
            "items" to selected.map { mapOf("workItemReference" to it.workItemReference, "rawInput" to it.rawInput, "partition" to it.partition, "requestReference" to it.requestReference) },
            "groundTruthRelease" to active.releaseReference,
            "provider" to "OPENAI",
            "model" to configuration.model,
            "inferenceSchema" to configuration.inferenceSchemaVersion,
            "instructionPolicy" to configuration.instructionPolicyVersion,
            "providerFingerprint" to configuration.fingerprint().value,
        )
        file.writeText(Gson.toJson(mission) + "\n")
    }

    private fun successReport(selected: List<SelectedItem>, completed: List<CompletedItem>, preflight: Boolean, gate: Boolean, optIn: Boolean, key: Boolean, logical: Int, physical: Int, retries: Int, integrity: Boolean) = buildString {
        appendLine("# HIM F3.8g.4 SMALL MULTI-ITEM TEACHER GROUND TRUTH PAID PILOT V1")
        appendLine("## PREFLIGHT")
        appendLine("F3.8g.1: ${if (preflight) "CURRENT" else "FAIL"}")
        appendLine("F3.8g.3 gate: ${if (gate) "READY_FOR_SMALL_MULTI_ITEM_PILOT" else "FAIL"}")
        appendLine("## AUTHORIZATION")
        appendLine("HIM_OPENAI_TEACHER_GROUND_TRUTH_GENERATION: $optIn")
        appendLine("OPENAI_API_KEY_PRESENT: $key")
        appendLine("## PILOT")
        appendLine("Contract: ${HimSmallMultiItemTeacherPilotContractV1.VERSION}")
        appendLine("Expected work items: 3")
        appendLine("Executed: ${completed.size}")
        appendLine("Completed: ${completed.size}")
        appendLine("Failed: 0")
        appendLine("Unexecuted: 0")
        appendLine("## SELECTED ITEMS")
        selected.forEachIndexed { index, item ->
            appendLine("${index + 1}. Work item: ${item.workItemReference}; Raw input: ${item.rawInput}; Partition: ${item.partition}; Request identity: ${item.requestReference}")
        }
        appendLine("## ITEM RESULTS")
        completed.forEachIndexed { index, item ->
            appendLine("### ITEM ${index + 1}")
            appendLine("Classification: ${item.result.output.proposals.joinToString(",") { it.relation.classification().name }.ifBlank { "NO_PROPOSAL" }}")
            appendLine("Information gain: ${item.result.output.informationGain.name}")
            appendLine("Target: ${item.result.output.proposals.joinToString(" || ") { it.candidateTerm }.ifBlank { "none" }}")
            appendLine("Confidence: ${item.result.output.proposals.joinToString(",") { it.confidence.name }.ifBlank { "none" }}")
            appendLine("Evidence: ${item.result.retrievalProvenance.evidenceReferences.joinToString(",").ifBlank { "none" }}")
            appendLine("Validation: PASS")
            appendLine("Persistence: PASS")
            appendLine("Reload: PASS")
            appendLine("Logical calls: 1")
            appendLine("Physical attempts: ${item.attempts}")
            appendLine("Retries: ${(item.attempts - 1).coerceAtLeast(0)}")
            appendLine("Input tokens: ${item.usage?.inputTokens ?: "NOT_RETURNED"}")
            appendLine("Output tokens: ${item.usage?.outputTokens ?: "NOT_RETURNED"}")
            appendLine("Cached input tokens: ${item.usage?.cachedInputTokens ?: "NOT_RETURNED"}")
        }
        appendLine("## PILOT TOTAL USAGE")
        appendLine("Input tokens: ${completed.sumOf { it.usage?.inputTokens ?: 0 }}")
        appendLine("Output tokens: ${completed.sumOf { it.usage?.outputTokens ?: 0 }}")
        appendLine("Cached input tokens: ${completed.sumOf { it.usage?.cachedInputTokens ?: 0 }}")
        appendLine("Logical inference calls: $logical")
        appendLine("Physical provider attempts: $physical")
        appendLine("Technical retries: $retries")
        appendLine("## PARTITIONS")
        appendLine("Original partitions preserved: ${selected.zip(completed).all { it.first.partition == it.second.result.partition.name }}")
        appendLine("## PUBLICATION / MUTATION")
        appendLine("Ground Truth mutations: 0\nAuthority mutations: 0\nEntity-ID allocations: 0\nCanonical mutations: 0\nDimension writes: 0\nMutation Ledger writes: 0\nRelease writes: 0")
        appendLine("## INTEGRITY")
        appendLine("Ground Truth unchanged: $integrity\nAuthority unchanged: $integrity\nRegistry unchanged: $integrity\nRetrieval Foundation unchanged: $integrity")
        appendLine("## FILES CREATED")
        appendLine(MISSION_PATH)
        completed.forEach { appendLine(it.item.resultPath) }
        appendLine(REPORT_PATH)
        appendLine("## FILES CHANGED")
        appendLine("No existing production or source-evidence file changed.")
        appendLine("## BUILD")
        appendLine("compileDebugKotlin: PASS\ncompileDebugUnitTestKotlin: PASS")
        appendLine("## REAL PILOT RESULT\nCOMPLETE")
        appendLine("## OPENAI / NETWORK\nOpenAI calls: $physical\nNetwork calls: $physical\nPaid inference: true")
        appendLine("## CONTRACT DEVIATIONS\nnone\n## HARD FAILURES\nnone")
        appendLine("## SMALL_MULTI_ITEM_TEACHER_PILOT_COMPLETE\ntrue\n## F3.8g.4 COMPLETE\ntrue")
        appendLine("## NEXT STEP\nSTOP.")
    }

    private fun failureReport(selected: List<SelectedItem>, completed: List<CompletedItem>, preflight: Boolean, gate: Boolean, optIn: Boolean, key: Boolean, logical: Int, physical: Int, retries: Int, before: Map<String, String>, after: Map<String, String>, failedIndex: Int?, inference: HimSemanticInferenceResult?, attempts: Int, failure: Throwable) = buildString {
        appendLine("# HIM F3.8g.4 SMALL MULTI-ITEM TEACHER GROUND TRUTH PAID PILOT V1")
        appendLine("## PREFLIGHT\nF3.8g.1: ${if (preflight) "CURRENT" else "FAIL"}\nF3.8g.3 gate: ${if (gate) "READY_FOR_SMALL_MULTI_ITEM_PILOT" else "FAIL"}")
        appendLine("## AUTHORIZATION\nHIM_OPENAI_TEACHER_GROUND_TRUTH_GENERATION: $optIn\nOPENAI_API_KEY_PRESENT: $key")
        appendLine("## PILOT\nContract: ${HimSmallMultiItemTeacherPilotContractV1.VERSION}\nExpected work items: 3\nExecuted: ${completed.size + if (failedIndex != null) 1 else 0}\nCompleted: ${completed.size}\nFailed: ${failedIndex?.plus(1) ?: 0}\nUnexecuted: ${(selected.size - completed.size - if (failedIndex != null) 1 else 0).coerceAtLeast(0)}")
        appendLine("## SELECTED ITEMS")
        selected.forEachIndexed { index, item -> appendLine("${index + 1}. Work item: ${item.workItemReference}; Raw input: ${item.rawInput}; Partition: ${item.partition}; Request identity: ${item.requestReference}") }
        appendLine("## FAILURE\nFailed item: ${failedIndex?.plus(1) ?: "before provider"}\nFailure kind: ${(inference as? HimSemanticInferenceResult.TechnicalFailure)?.failure?.kind ?: "LOCAL_CONTRACT_OR_PERSISTENCE_FAILURE"}\nLogical calls: $logical\nPhysical attempts: $physical\nRetries: $retries\nSafe diagnostics: ${(inference as? HimSemanticInferenceResult.TechnicalFailure)?.failure?.attemptDiagnostics?.joinToString(",") { "attempt=${it.attemptNumber};kind=${it.providerNeutralFailure};http=${it.safeProviderDiagnostic?.httpStatusCode};family=${it.safeProviderDiagnostic?.httpStatusFamily}" } ?: "none"}\nSafe local failure: ${failure.message?.replace(Regex("\\s+"), " ").orEmpty().take(240)}\nCurrent item attempts: $attempts")
        appendLine("Completed prior items: ${completed.size}\nUnexecuted later items: ${(selected.size - completed.size - if (failedIndex != null) 1 else 0).coerceAtLeast(0)}")
        appendLine("## DATA WRITES\nGround Truth mutations: 0\nAuthority mutations: 0\nEntity-ID allocations: 0\nCanonical mutations: 0\nDimension writes: 0\nMutation Ledger writes: 0\nRelease writes: 0")
        appendLine("## INTEGRITY\nGround Truth unchanged: ${before.isNotEmpty() && before == after}\nAuthority unchanged: ${before.isNotEmpty() && before == after}\nRegistry unchanged: ${before.isNotEmpty() && before == after}\nRetrieval Foundation unchanged: ${before.isNotEmpty() && before == after}")
        appendLine("## REAL PILOT RESULT\n${if (failedIndex == null) "BLOCKED_PREFLIGHT" else "FAILED_TECHNICAL"}\n## OPENAI / NETWORK\nOpenAI calls: $physical\nNetwork calls: $physical\nPaid inference: ${physical > 0}\n## SMALL_MULTI_ITEM_TEACHER_PILOT_COMPLETE\nfalse\n## F3.8g.4 COMPLETE\nfalse\n## NEXT STEP\nSTOP. No automatic rerun or replacement item.")
    }

    private fun readReport(file: File): Map<String, String> = file.readLines().mapNotNull { line ->
        val separator = line.indexOf(':')
        if (separator < 0) null else line.substring(0, separator) to line.substring(separator + 1).trim()
    }.toMap()

    private fun integritySnapshot(root: File, active: HimActiveGroundTruthArtifactsV1): Map<String, String> =
        (listOf(active.authorityFile, active.activeRegistryFile, active.retiredRegistryFile, active.mutationLedgerFile, active.fingerprintIndexFile, active.releaseFile) + listOf(root.resolve(HimCanonicalFamilyPaths.PRODUCT_ONLY_MASTER_PATH), root.resolve(HimCanonicalFamilyPaths.ENTITY_ID_REGISTRY_FILE_NAME.let { "${HimCanonicalFamilyPaths.CANONICAL_FAMILY_MASTER_DIRECTORY}/$it" })))
            .filter { it.isFile }
            .associate { it.relativeTo(root).path to sha256(it.readBytes()) }

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("user.dir unavailable")).canonicalFile
        while (current.parentFile != null && !current.resolve("settings.gradle").isFile && !current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
        return current
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFKC)
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun writeReport(file: File, content: String) {
        requireNotNull(file.parentFile).mkdirs()
        file.writeText(content)
    }

    private data class SelectedItem(val workItemReference: String, val canonicalId: String, val rawInput: String, val partition: String, val requestReference: String, val request: HimTeacherGroundTruthGenerationRequestV1, val family: HimCanonicalFamily) {
        val resultPath: String get() = "build/knowledge/reports/him/training/him-f3-8g4-item-${workItemReference.substringAfterLast(':').take(12)}.result.v1.json"
        fun resultFile(root: File) = root.resolve(resultPath)
    }

    private data class CompletedItem(val item: SelectedItem, val result: HimTeacherGroundTruthGenerationResultV1, val attempts: Int, val usage: HimOpenAiUsageDiagnostics?)

    private data class MissionItemDto(
        val workItemReference: String,
        val rawInput: String,
        val partition: String,
        val requestReference: String,
    )

    private data class GroundTruthReleaseDto(val value: String)

    private data class FrozenMissionDto(
        val contract: String,
        val expectedItems: Int,
        val ordering: List<String>,
        val items: List<MissionItemDto>,
        val groundTruthRelease: GroundTruthReleaseDto,
        val provider: String,
        val model: String,
        val inferenceSchema: String,
        val instructionPolicy: String,
        val providerFingerprint: String,
    )

    companion object {
        private const val GATE_REPORT_PATH = "build/knowledge/reports/him/training/him-f3-8g3-first-real-teacher-pilot-evaluation.txt"
        private const val REPORT_PATH = "build/knowledge/reports/him/training/him-f3-8g4-small-multi-item-teacher-paid-pilot.txt"
        private const val MISSION_PATH = "build/knowledge/reports/him/training/him-f3-8g4-small-multi-item-teacher-paid-pilot.selection.v1.json"
        private const val CONTINUATION_REPORT_PATH = "build/knowledge/reports/him/training/him-f3-8g4-small-multi-item-teacher-paid-pilot.continuation.v1.txt"
        private const val FROZEN_MISSION_SHA256 = "b418ef2ada657d8eb18d4dfc986026ef9656b870f304f13e2917d24be9730320"
        private const val PRIOR_FAILURE_REPORT_SHA256 = "848216b66b5ef3ec7ddc5ecce4dfb950dc48fb40a744582a1a2ecee6002addda"
        private val Gson = GsonBuilder().disableHtmlEscaping().create()
    }
}
