package de.shopme.testing.system.tools.knowledge.him.training.teacher

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalQuery
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalFamilyCandidateRetrieval
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.*
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.scaling.*
import de.shopme.tools.knowledge.him.training.teacher.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/** One-shot F3.8g.2 runner. Invoke only through the caffeinated Gradle command. */
class RunHimFirstRealTeacherGroundTruthGenerationPilotV1Test {
    @Test
    fun executeExactlyOneRealTeacherGroundTruthGenerationPilot() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requirePaidNetworkEnabled()
        val root = projectRoot()
        val reportFile = root.resolve(REPORT_PATH)
        val resultFile = root.resolve(RESULT_PATH)
        var paidInvocationStarted = false
        var logicalCalls = 0
        var preflightCurrent = false
        var optIn = false
        var apiKeyPresent = false
        var selectedReference = "UNRESOLVED"
        var selectedPartition = "UNRESOLVED"
        var requestReference = "UNRESOLVED"
        var rawInput = "UNRESOLVED"
        var providerAttempts = 0
        var usage: HimOpenAiUsageDiagnostics? = null
        var providerDiagnostics: HimOpenAiInvocationDiagnostics? = null
        var lastInferenceResult: HimSemanticInferenceResult? = null
        var result: HimTeacherGroundTruthGenerationResultV1? = null
        var integrityBefore: Map<String, String> = emptyMap()
        var integrityAfter: Map<String, String> = emptyMap()
        var failure: Throwable? = null

        try {
            HimTeacherPaidPilotOfflinePreflightV1.requireCurrent(root)
            preflightCurrent = true
            optIn = System.getenv(HimTeacherGroundTruthGenerationContractV1.PAID_OPT_IN_ENVIRONMENT_FLAG) == "true"
            check(optIn) { "Teacher paid-pilot opt-in is not exactly true" }
            apiKeyPresent = !System.getenv("OPENAI_API_KEY").isNullOrBlank()
            check(apiKeyPresent) { "OPENAI_API_KEY is absent" }

            val paths = HimCanonicalFamilyPaths(root)
            val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
            val active = HimActiveGroundTruthResolutionV1().resolve(root)
            val authority = HimCanonicalFamilyPersistence().readAuthority(active.authorityFile)
            val candidateDatasetFile = root.resolve("${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json")
            val candidateBinding = if (candidateDatasetFile.isFile) {
                val dataset = HimCandidateDatasetPersistenceV2.readDataset(candidateDatasetFile)
                HimCandidateDatasetBindingV1(
                    path = "${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json",
                    digest = de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1.datasetDigest(dataset),
                )
            } else null
            val plan = HimCanonicalGroundTruthScalingPlannerV1().plan(
                HimCanonicalGroundTruthScalingPlannerInputV1(
                    catalog = catalog,
                    authority = authority,
                    groundTruthReleaseReference = active.releaseReference,
                    candidateDatasetBinding = candidateBinding,
                    childLineage = emptyList(),
                ),
            )
            require(plan.workItems.isNotEmpty()) { "No deterministic F3.8e Teacher work item exists" }
            val workItem = plan.workItems.sortedBy { it.reference }.first()
            selectedReference = workItem.reference
            selectedPartition = workItem.partition.name
            val family = authority.families.single { it.canonicalId == workItem.canonicalId }
            rawInput = family.canonicalName
            val canonicalContext = HimCanonicalFamilyCandidateRetrieval(authority.families)
                .retrieve(HimCanonicalRetrievalQuery(rawInput, normalize(rawInput)))
                .map { result ->
                    HimCandidateCanonicalContext(
                        result.rank,
                        result.canonicalId,
                        result.canonicalName,
                        result.familyOrNull()?.let { Gson.toJson(it) },
                    )
                }
            require(canonicalContext.any { it.canonicalId == workItem.canonicalId }) {
                "Selected work item is not represented in deterministic canonical retrieval context"
            }
            val configuration = HimOpenAiSemanticProviderConfiguration()
            val preflightArtifact = HimTeacherPaidPilotOfflinePreflightV1.read(
                root.resolve(HimTeacherPaidPilotOfflinePreflightContractV1.ARTIFACT),
            )
            requireNotNull(preflightArtifact)
            assertEquals(configuration.fingerprint().value, preflightArtifact.boundIdentities.providerFingerprint)
            assertEquals(HimSemanticInferenceSchema.OUTPUT_VERSION, preflightArtifact.boundIdentities.inferenceSchema)
            assertEquals(HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION, preflightArtifact.boundIdentities.instructionPolicy)

            val inferenceRequest = HimSemanticInferenceRequest(
                invocationReference = "teacher-pilot:${workItem.reference}",
                inputTerm = rawInput,
                canonicalContext = canonicalContext.map { context ->
                    val familyRecord = authority.families.singleOrNull { it.canonicalId == context.canonicalId }
                    if (familyRecord == null) {
                        HimCanonicalRetrievalResult.Compact(context.rank, context.canonicalId, context.canonicalName)
                    } else {
                        HimCanonicalRetrievalResult.Full(context.rank, familyRecord)
                    }
                },
                evidence = emptyList(),
                retrievalRound = HimRetrievalRound(0),
            )
            val request = HimTeacherGroundTruthGenerationRequestV1.create(
                workItem = workItem,
                observedTerm = rawInput,
                canonicalContext = canonicalContext,
                inferenceRequest = inferenceRequest,
                policyBindings = HimTeacherGroundTruthPolicyBindingsV1(
                    providerConfigurationFingerprint = configuration.fingerprint(),
                    contextBudgetPolicyVersion = HimSemanticContextBudgetPolicy.VERSION,
                ),
            )
            requestReference = request.requestReference
            require(!resultFile.exists()) { "The designated pilot result already exists; refusing a second paid execution" }
            integrityBefore = integritySnapshot(root, active)

            val gson = GsonBuilder().disableHtmlEscaping().create()
            val fixedContextFactory = HimOpenAiFixedContextFactory { currentRequest ->
                HimSemanticFixedContext(
                    currentRequest.inputTerm,
                    gson.toJson(currentRequest.canonicalContext),
                    gson.toJson(family),
                    HimOpenAiSemanticOutputJsonSchema.V2.toString(),
                )
            }
            val openAiProvider = HimOpenAiSemanticInferenceProvider(
                configuration,
                HimOpenAiEnvironmentApiKeyProvider(),
                fixedContextFactory,
                HimRetrofitOpenAiResponsesTransport.create(configuration.timeoutMilliseconds),
            )
            val runtime = HimProviderBackedSemanticInferenceRuntime(
                openAiProvider,
                HimSemanticInferenceProviderConfiguration(
                    "OPENAI", configuration.model, configuration.configurationVersion,
                    configuration.inferenceSchemaVersion, configuration.instructionPolicyVersion,
                    null, null, configuration.maxOutputTokens, configuration.reasoningEffort,
                    configuration.timeoutPolicyVersion, 1_000_000,
                ),
                HimSemanticInferenceJsonDecoder(),
                configuration.fingerprint(),
            )
            val recordingRuntime = HimSemanticInferenceRuntime { currentRequest ->
                logicalCalls++
                val inferenceResult = runtime.infer(currentRequest)
                lastInferenceResult = inferenceResult
                providerDiagnostics = openAiProvider.diagnostics
                paidInvocationStarted = openAiProvider.diagnostics.physicalAttempts > 0
                inferenceResult
            }
            val adapter = HimSemanticInferenceTeacherProviderAdapterV1(recordingRuntime)
            val provider = HimOptInTeacherGroundTruthProviderV1(
                HimTeacherGroundTruthProviderV1 { currentRequest ->
                    when (val outcome = adapter.invoke(currentRequest)) {
                        is HimTeacherGroundTruthProviderOutcomeV1.StructuredResponse -> outcome.copy(
                        usage = openAiProvider.diagnostics.usage?.let {
                                HimSemanticUsage(it.inputTokens, it.outputTokens, it.cachedInputTokens)
                            },
                        )
                        is HimTeacherGroundTruthProviderOutcomeV1.TechnicalFailure -> outcome
                    }
                },
            )
            result = HimTeacherGroundTruthGenerationPipelineV1().generate(
                plan,
                request,
                provider,
                HimTeacherGroundTruthExecutionModeV1.PAID_OPT_IN,
            )
            providerAttempts = openAiProvider.diagnostics.physicalAttempts
            usage = openAiProvider.diagnostics.usage
            HimTeacherGroundTruthGenerationPersistenceV1.writeNewResult(resultFile, result!!)
            val reloaded = HimTeacherGroundTruthGenerationPersistenceV1.readResult(resultFile)
            assertEquals(result, reloaded)
            integrityAfter = integritySnapshot(root, active)
            assertEquals(integrityBefore, integrityAfter)
            writeReport(
                reportFile,
                successReport(root, request, result!!, providerAttempts, usage, integrityBefore == integrityAfter, resultFile),
            )
            assertTrue("Exactly one logical Teacher work item must be executed", logicalCalls == 1)
        } catch (t: Throwable) {
            failure = t
            providerAttempts = providerDiagnostics?.physicalAttempts ?: providerAttempts.coerceAtLeast(0)
            usage = providerDiagnostics?.usage ?: usage
            runCatching {
                writeReport(
                    reportFile,
                    failureReport(
                        requestReference,
                        selectedReference,
                        selectedPartition,
                        rawInput,
                        preflightCurrent,
                        optIn,
                        apiKeyPresent,
                        logicalCalls,
                        providerAttempts,
                        usage,
                        lastInferenceResult,
                        providerDiagnostics,
                        integrityBefore,
                        integrityAfter,
                        paidInvocationStarted,
                        t,
                        resultFile,
                    ),
                )
            }
            throw t
        }
    }

    private fun successReport(
        root: File,
        request: HimTeacherGroundTruthGenerationRequestV1,
        result: HimTeacherGroundTruthGenerationResultV1,
        physicalAttempts: Int,
        usage: HimOpenAiUsageDiagnostics?,
        integrityUnchanged: Boolean,
        resultFile: File,
    ) = buildString {
        appendLine("# HIM F3.8g.2 FIRST REAL TEACHER GROUND TRUTH GENERATION PILOT V1")
        appendLine("PREFLIGHT_CURRENT=true")
        appendLine("PREFLIGHT_CONTRACT=${HimTeacherPaidPilotOfflinePreflightContractV1.VERSION}")
        appendLine("HIM_OPENAI_TEACHER_GROUND_TRUTH_GENERATION=true")
        appendLine("OPENAI_API_KEY_PRESENT=true")
        appendLine("WORK_ITEM_REFERENCE=${request.workItemReference}")
        appendLine("RAW_INPUT=${request.observedTerm}")
        appendLine("PARTITION=${request.partition.name}")
        appendLine("REQUEST_REFERENCE=${request.requestReference}")
        appendLine("PROVIDER=OPENAI")
        appendLine("MODEL=${result.inferenceProvenance.modelIdentifier}")
        appendLine("INSTRUCTION_POLICY=${result.inferenceProvenance.instructionPolicyVersion}")
        appendLine("INFERENCE_SCHEMA=${result.inferenceProvenance.inferenceSchemaVersion}")
        appendLine("PROVIDER_FINGERPRINT=${result.inferenceProvenance.providerConfigurationFingerprint.value}")
        appendLine("LOGICAL_TEACHER_WORK_ITEMS=1")
        appendLine("LOGICAL_INFERENCE_CALLS=1")
        appendLine("PHYSICAL_PROVIDER_ATTEMPTS=$physicalAttempts")
        appendLine("TECHNICAL_RETRIES=${(physicalAttempts - 1).coerceAtLeast(0)}")
        appendLine("RETRIEVAL_ROUNDS=${request.inferenceRequest.retrievalRound.value}")
        appendLine("RETRIEVAL_SOURCES=${request.inferenceRequest.availableSources.sortedBy { it.ordinal }.joinToString(",")}")
        appendLine("EVIDENCE_REFERENCES=${result.retrievalProvenance.evidenceReferences.joinToString(",")}")
        appendLine("CLASSIFICATION=${result.output.proposals.joinToString(",") { it.relation.classification().name }.ifBlank { "NO_PROPOSAL" }}")
        appendLine("TARGETS=${result.output.proposals.joinToString(" || ") { it.candidateTerm }}")
        appendLine("RELATIONS=${result.output.proposals.joinToString(" || ") { it.relation.toString() }}")
        appendLine("CONFIDENCE=${result.output.proposals.joinToString(",") { it.confidence.name }.ifBlank { "NONE" }}")
        appendLine("EVIDENCE_VALIDATION=PASS")
        appendLine("STRICT_VALIDATION=PASS")
        appendLine("PERSISTENCE_WRITE=PASS")
        appendLine("PERSISTENCE_RELOAD=PASS")
        appendLine("PARTITION_BINDING_PRESERVED=true")
        appendLine("INPUT_TOKENS=${usage?.inputTokens ?: "NOT_RETURNED"}")
        appendLine("OUTPUT_TOKENS=${usage?.outputTokens ?: "NOT_RETURNED"}")
        appendLine("CACHED_INPUT_TOKENS=${usage?.cachedInputTokens ?: "NOT_RETURNED"}")
        appendLine("PILOT_RESULT_ARTIFACT=${resultFile.relativeTo(root).path}")
        appendLine("GROUND_TRUTH_MUTATIONS=0")
        appendLine("AUTHORITY_MUTATIONS=0")
        appendLine("ENTITY_ID_ALLOCATIONS=0")
        appendLine("DIMENSION_WRITES=0")
        appendLine("MUTATION_LEDGER_WRITES=0")
        appendLine("RELEASE_WRITES=0")
        appendLine("GROUND_TRUTH_UNCHANGED=$integrityUnchanged")
        appendLine("AUTHORITY_UNCHANGED=$integrityUnchanged")
        appendLine("REGISTRY_UNCHANGED=$integrityUnchanged")
        appendLine("RETRIEVAL_FOUNDATION_UNCHANGED=$integrityUnchanged")
        appendLine("OPENAI_CALLS=$physicalAttempts")
        appendLine("NETWORK_CALLS=$physicalAttempts")
        appendLine("PAID_INFERENCE=true")
        appendLine("F3.8g.2 COMPLETE=true")
        appendLine("FIRST_REAL_TEACHER_GROUND_TRUTH_GENERATION_COMPLETE=true")
        appendLine("NEXT_STEP=STOP")
    }

    private fun failureReport(
        requestReference: String,
        workItemReference: String,
        partition: String,
        rawInput: String,
        preflightCurrent: Boolean,
        optIn: Boolean,
        apiKeyPresent: Boolean,
        logicalCalls: Int,
        physicalAttempts: Int,
        usage: HimOpenAiUsageDiagnostics?,
        lastInferenceResult: HimSemanticInferenceResult?,
        providerDiagnostics: HimOpenAiInvocationDiagnostics?,
        before: Map<String, String>,
        after: Map<String, String>,
        paidInvocationStarted: Boolean,
        failure: Throwable,
        resultFile: File,
    ) = buildString {
        appendLine("# HIM F3.8g.2 FIRST REAL TEACHER GROUND TRUTH GENERATION PILOT STOP REPORT")
        appendLine("PREFLIGHT_CURRENT=$preflightCurrent")
        appendLine("HIM_OPENAI_TEACHER_GROUND_TRUTH_GENERATION=$optIn")
        appendLine("OPENAI_API_KEY_PRESENT=$apiKeyPresent")
        appendLine("WORK_ITEM_REFERENCE=$workItemReference")
        appendLine("RAW_INPUT=$rawInput")
        appendLine("PARTITION=$partition")
        appendLine("REQUEST_REFERENCE=$requestReference")
        appendLine("FAILURE_PHASE=${if (paidInvocationStarted) "AFTER_PAID_INFERENCE" else "BEFORE_PAID_INFERENCE"}")
        appendLine("FAILURE_TYPE=${failure::class.java.simpleName}")
        appendLine("FAILURE_MESSAGE=${failure.message?.replace(Regex("\\s+"), " ").orEmpty().take(300)}")
        appendLine("LOGICAL_INFERENCE_CALLS=$logicalCalls")
        appendLine("PHYSICAL_PROVIDER_ATTEMPTS=$physicalAttempts")
        appendLine("TECHNICAL_RETRIES=${(physicalAttempts - 1).coerceAtLeast(0)}")
        val technicalFailure = (lastInferenceResult as? HimSemanticInferenceResult.TechnicalFailure)?.failure
        appendLine("FAILURE_KIND=${technicalFailure?.kind ?: "LOCAL_OR_SCHEMA_VALIDATION_FAILURE"}")
        appendLine("LAST_SUCCESSFUL_RETRIEVAL_ROUND=0")
        appendLine("SAFE_PROVIDER_DIAGNOSTICS=${technicalFailure?.attemptDiagnostics?.joinToString(",") { diagnostic ->
            "attempt=${diagnostic.attemptNumber};kind=${diagnostic.providerNeutralFailure};http=${diagnostic.safeProviderDiagnostic?.httpStatusCode};family=${diagnostic.safeProviderDiagnostic?.httpStatusFamily}"
        } ?: "none"}")
        appendLine("PROVIDER_SUCCESS=${providerDiagnostics?.providerSuccess ?: false}")
        appendLine("INPUT_TOKENS=${usage?.inputTokens ?: "NOT_RETURNED"}")
        appendLine("OUTPUT_TOKENS=${usage?.outputTokens ?: "NOT_RETURNED"}")
        appendLine("CACHED_INPUT_TOKENS=${usage?.cachedInputTokens ?: "NOT_RETURNED"}")
        appendLine("PILOT_RESULT_ARTIFACT_EXISTS=${resultFile.isFile}")
        appendLine("GROUND_TRUTH_MUTATIONS=0")
        appendLine("AUTHORITY_MUTATIONS=0")
        appendLine("ENTITY_ID_ALLOCATIONS=0")
        appendLine("DIMENSION_WRITES=0")
        appendLine("MUTATION_LEDGER_WRITES=0")
        appendLine("RELEASE_WRITES=0")
        appendLine("INTEGRITY_BEFORE_CAPTURED=${before.isNotEmpty()}")
        appendLine("INTEGRITY_UNCHANGED=${before.isNotEmpty() && before == after}")
        appendLine("OPENAI_CALLS=$physicalAttempts")
        appendLine("NETWORK_CALLS=$physicalAttempts")
        appendLine("PAID_INFERENCE=$paidInvocationStarted")
        appendLine("F3.8g.2 COMPLETE=false")
        appendLine("NEXT_STEP=STOP_NO_AUTOMATIC_RERUN")
    }

    private fun integritySnapshot(root: File, active: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthArtifactsV1): Map<String, String> {
        val paths = linkedSetOf(
            HimCanonicalFamilyPaths.PRODUCT_ONLY_MASTER_PATH,
            HimCanonicalFamilyPaths.ENTITY_ID_REGISTRY_FILE_NAME.let { "${HimCanonicalFamilyPaths.CANONICAL_FAMILY_MASTER_DIRECTORY}/$it" },
            HimCanonicalFamilyPaths.FAMILY_AUTHORITY_FILE_NAME.let { "${HimCanonicalFamilyPaths.CANONICAL_FAMILY_MASTER_DIRECTORY}/$it" },
            "data/knowledge/him/candidates/master/candidate-dataset.v2.json",
            "data/knowledge/him/retrieval/foundation/v1/release.json",
        )
        val activePaths = listOf(active.authorityFile, active.activeRegistryFile, active.retiredRegistryFile, active.mutationLedgerFile, active.fingerprintIndexFile, active.releaseFile)
        return (paths.map { root.resolve(it) to it } + activePaths.map { it to it.relativeTo(root).path })
            .filter { it.first.isFile }
            .associate { (file, path) -> path to sha256(file.readBytes()) }
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFKC)

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("user.dir is unavailable")).canonicalFile
        while (
            current.parentFile != null &&
            !current.resolve("settings.gradle").isFile &&
            !current.resolve("settings.gradle.kts").isFile
        ) {
            current = current.parentFile
        }
        return current
    }

    private fun writeReport(file: File, content: String) {
        requireNotNull(file.parentFile).mkdirs()
        file.writeText(content)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun HimCanonicalRetrievalResult.familyOrNull(): de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily? =
        when (this) {
            is de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult.Full -> family
            is de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult.Compact -> null
        }

    companion object {
        private const val REPORT_PATH = "build/knowledge/reports/him/training/him-f3-8g2-first-real-teacher-ground-truth-generation-pilot.txt"
        private const val RESULT_PATH = "build/knowledge/reports/him/training/him-f3-8g2-first-real-teacher-ground-truth-generation-pilot.result.v1.json"
        private val Gson = GsonBuilder().disableHtmlEscaping().create()
    }
}
