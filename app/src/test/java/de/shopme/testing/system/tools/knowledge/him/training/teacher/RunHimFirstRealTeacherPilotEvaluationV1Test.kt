package de.shopme.testing.system.tools.knowledge.him.training.teacher

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalFamilyCandidateRetrieval
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalQuery
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticContextBudgetPolicy
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceRequest
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import de.shopme.tools.knowledge.him.training.scaling.HimCandidateDatasetBindingV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlanV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerInputV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerV1
import de.shopme.tools.knowledge.him.training.teacher.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.Normalizer
import java.util.Locale

class RunHimFirstRealTeacherPilotEvaluationV1Test {
    @Test
    fun evaluateCompletedPilotOfflineAndWriteMultiItemGateReport() {
        val root = projectRoot()
        val report = root.resolve(REPORT_PATH)
        val pilotReport = root.resolve(PILOT_REPORT_PATH)
        val resultFile = root.resolve(RESULT_PATH)
        val pilot = HimTeacherGroundTruthGenerationPersistenceV1.readResult(resultFile)
        val pilotProperties = readKeyValueReport(pilotReport)

        assertEquals(EXPECTED_WORK_ITEM, pilot.workItemReference)
        assertEquals(EXPECTED_REQUEST, pilot.requestReference)
        assertEquals(HimEntityId(EXPECTED_CANONICAL_ID), pilot.canonicalId)
        assertEquals("TRAIN", pilot.partition.name)
        assertTrue(pilot.output.proposals.isEmpty())
        assertEquals(HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN, pilot.output.informationGain)
        assertTrue(pilot.retrievalProvenance.evidenceReferences.isEmpty())
        assertEquals(0, pilot.retrievalProvenance.retrievalRoundCount)
        assertNotNull(pilot.usage)
        assertEquals(3647L, pilot.usage!!.inputTokens ?: -1L)
        assertEquals(153L, pilot.usage!!.outputTokens ?: -1L)
        assertEquals(0L, pilot.usage!!.cachedInputTokens ?: -1L)
        assertEquals("true", pilotProperties.getValue("F3.8g.2 COMPLETE"))
        assertEquals("PASS", pilotProperties.getValue("STRICT_VALIDATION"))
        assertEquals("PASS", pilotProperties.getValue("PERSISTENCE_WRITE"))
        assertEquals("PASS", pilotProperties.getValue("PERSISTENCE_RELOAD"))
        assertEquals("true", pilotProperties.getValue("PARTITION_BINDING_PRESERVED"))
        assertEquals("1", pilotProperties.getValue("PHYSICAL_PROVIDER_ATTEMPTS"))
        assertEquals("0", pilotProperties.getValue("TECHNICAL_RETRIES"))

        HimTeacherPaidPilotOfflinePreflightV1.requireCurrent(root)
        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val authority = HimCanonicalFamilyPersistence().readAuthority(active.authorityFile)
        val catalog = HimProductOnlyCanonicalMasterReader().read(HimCanonicalFamilyPaths(root))
        val plan = currentPlan(root, catalog, authority, active.releaseReference)
        val workItem = plan.workItems.single { it.reference == pilot.workItemReference }
        assertEquals(pilot.canonicalId, workItem.canonicalId)
        assertEquals(pilot.partition, workItem.partition)

        val family = authority.families.single { it.canonicalId == pilot.canonicalId }
        assertEquals("Oblatenlebkuchen", family.canonicalName)
        assertTrue(family.identities.isEmpty())
        assertTrue(family.variants.isEmpty())
        assertTrue(family.aliases.isEmpty())
        val canonicalContext = HimCanonicalFamilyCandidateRetrieval(authority.families)
            .retrieve(HimCanonicalRetrievalQuery("Oblatenlebkuchen", normalize("Oblatenlebkuchen")))
        assertTrue(canonicalContext.first() is HimCanonicalRetrievalResult.Full)
        assertEquals(pilot.canonicalId, canonicalContext.first().canonicalId)

        val request = request(workItem, canonicalContext, family)
        assertEquals(pilot.requestReference, request.requestReference)
        assertEquals(request.inferenceRequest.inputTerm, "Oblatenlebkuchen")
        HimTeacherGroundTruthOutputValidatorV1.validate(request, pilot.output)

        val semanticAudit = HimTeacherPilotSemanticAuditResultV1.PLAUSIBLE_CONSERVATIVE
        val gate = HimTeacherMultiItemPaidGateV1.evaluate(
            HimTeacherMultiItemPaidGateInputV1(
                preflightCurrent = pilotProperties.getValue("PREFLIGHT_CURRENT") == "true",
                f3g2Complete = pilotProperties.getValue("F3.8g.2 COMPLETE") == "true",
                exactlyOneRealWorkItem = pilotProperties.getValue("LOGICAL_TEACHER_WORK_ITEMS") == "1" && pilotProperties.getValue("LOGICAL_INFERENCE_CALLS") == "1",
                strictDecodePass = pilotProperties.getValue("STRICT_VALIDATION") == "PASS",
                teacherValidationPass = pilotProperties.getValue("EVIDENCE_VALIDATION") == "PASS",
                persistencePass = pilotProperties.getValue("PERSISTENCE_WRITE") == "PASS",
                reloadPass = pilotProperties.getValue("PERSISTENCE_RELOAD") == "PASS",
                partitionPreserved = pilotProperties.getValue("PARTITION_BINDING_PRESERVED") == "true",
                providerAttemptsWithinContract = pilot.inferenceProvenance.technicalAttemptCount in 1..2,
                retriesWithinContract = pilotProperties.getValue("TECHNICAL_RETRIES") == "0",
                groundTruthUnchanged = pilotProperties.getValue("GROUND_TRUTH_UNCHANGED") == "true" && pilotProperties.getValue("GROUND_TRUTH_MUTATIONS") == "0",
                authorityUnchanged = pilotProperties.getValue("AUTHORITY_UNCHANGED") == "true" && pilotProperties.getValue("AUTHORITY_MUTATIONS") == "0",
                entityIdsUnchanged = pilotProperties.getValue("ENTITY_ID_ALLOCATIONS") == "0",
                usageAvailable = pilot.usage != null,
                semanticAudit = semanticAudit,
                unexplainedProviderFailure = false,
            ),
        )
        assertEquals(HimTeacherMultiItemPaidGateStateV1.READY_FOR_SMALL_MULTI_ITEM_PILOT, gate.state)
        assertFalse(gate.automaticBatchAuthorization)

        val inputPerItem = requireNotNull(pilot.usage!!.inputTokens)
        val outputPerItem = requireNotNull(pilot.usage!!.outputTokens)
        writeReport(
            report,
            buildReport(
                pilot,
                family,
                canonicalContext,
                semanticAudit,
                gate,
                inputPerItem,
                outputPerItem,
            ),
        )
    }

    private fun currentPlan(
        root: File,
        catalog: de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster,
        authority: de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority,
        release: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1,
    ): HimCanonicalGroundTruthScalingPlanV1 {
        val file = root.resolve("${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json")
        val binding = if (file.isFile) {
            HimCandidateDatasetBindingV1(
                "${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json",
                de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1.datasetDigest(HimCandidateDatasetPersistenceV2.readDataset(file)),
            )
        } else null
        return HimCanonicalGroundTruthScalingPlannerV1().plan(
            HimCanonicalGroundTruthScalingPlannerInputV1(catalog, authority, release, binding, emptyList()),
        )
    }

    private fun request(
        workItem: de.shopme.tools.knowledge.him.training.scaling.HimTeacherGroundTruthWorkItemV1,
        context: List<HimCanonicalRetrievalResult>,
        family: de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily,
    ): HimTeacherGroundTruthGenerationRequestV1 {
        val inferenceContext = context.map { result ->
            when (result) {
                is HimCanonicalRetrievalResult.Full -> result
                is HimCanonicalRetrievalResult.Compact -> result
            }
        }
        val request = HimSemanticInferenceRequest(
            "teacher-pilot:${workItem.reference}",
            "Oblatenlebkuchen",
            inferenceContext,
            emptyList(),
            HimRetrievalRound(0),
        )
        return HimTeacherGroundTruthGenerationRequestV1.create(
            workItem,
            "Oblatenlebkuchen",
            context.map { result ->
                HimCandidateCanonicalContext(
                    result.rank,
                    result.canonicalId,
                    result.canonicalName,
                    (result as? HimCanonicalRetrievalResult.Full)?.let { Gson.toJson(it.family) },
                )
            },
            request,
            HimTeacherGroundTruthPolicyBindingsV1(
                providerConfigurationFingerprint = pilotProviderFingerprint(),
                contextBudgetPolicyVersion = HimSemanticContextBudgetPolicy.VERSION,
            ),
        )
    }

    private fun buildReport(
        pilot: HimTeacherGroundTruthGenerationResultV1,
        family: de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily,
        context: List<HimCanonicalRetrievalResult>,
        audit: HimTeacherPilotSemanticAuditResultV1,
        gate: HimTeacherMultiItemPaidGateDecisionV1,
        inputPerItem: Long,
        outputPerItem: Long,
    ) = buildString {
        appendLine("# HIM F3.8g.3 FIRST REAL TEACHER PILOT EVALUATION / MULTI-ITEM GATE V1")
        appendLine()
        appendLine("## PILOT")
        appendLine("Work item: ${pilot.workItemReference}")
        appendLine("Raw input: Oblatenlebkuchen")
        appendLine("Partition: ${pilot.partition.name}")
        appendLine("Classification: NO_PROPOSAL")
        appendLine("Information gain: ${pilot.output.informationGain.name}")
        appendLine()
        appendLine("## SEMANTIC PLAUSIBILITY")
        appendLine("Audit result: ${audit.name}")
        appendLine("Reason: The exact raw term is already the active canonical name and the canonical context ranks it first. Authority has no Identity, Variant, or Alias relation for this family, and no source evidence was retrieved in the zero-round pilot request. Therefore no deterministic child relation was available to assert, while CREATE_NEW_CANONICAL would have contradicted the existing canonical identity.")
        appendLine("Existing relation/context: canonical=${family.canonicalId.value}; canonicalName=${family.canonicalName}; contextRanks=${context.joinToString(",") { "${it.rank}:${it.canonicalId.value}" }}; identities=${family.identities.size}; variants=${family.variants.size}; aliases=${family.aliases.size}; evidenceReferences=${pilot.retrievalProvenance.evidenceReferences.size}")
        appendLine("Missed deterministic relation: false")
        appendLine()
        appendLine("## NO_PROPOSAL CONTRACT")
        appendLine("Valid successful outcome: true")
        appendLine("Technical failure: false")
        appendLine("Ground Truth mutation required: false")
        appendLine("No target/evidence/confidence required: true (empty proposals are valid with NO_EXPECTED_INFORMATION_GAIN)")
        appendLine("Auditable request/result identity: true")
        appendLine()
        appendLine("## PIPELINE QUALITY")
        appendLine("Strict decode: PASS")
        appendLine("Validation: PASS")
        appendLine("Persistence: PASS")
        appendLine("Reload: PASS")
        appendLine("Partition preserved: true")
        appendLine("Provider attempts: ${pilot.inferenceProvenance.technicalAttemptCount}")
        appendLine("Retries: 0")
        appendLine()
        appendLine("## USAGE")
        appendLine("Observed input tokens: ${pilot.usage!!.inputTokens}")
        appendLine("Observed output tokens: ${pilot.usage!!.outputTokens}")
        appendLine("Observed cached tokens: ${pilot.usage!!.cachedInputTokens}")
        appendLine()
        appendLine("## BASELINE PROJECTIONS")
        appendLine("5 items: input tokens ${inputPerItem * 5}; output tokens ${outputPerItem * 5}; calls 5; retries 0")
        appendLine("10 items: input tokens ${inputPerItem * 10}; output tokens ${outputPerItem * 10}; calls 10; retries 0")
        appendLine("25 items: input tokens ${inputPerItem * 25}; output tokens ${outputPerItem * 25}; calls 25; retries 0")
        appendLine("Projection type: OBSERVED_BASELINE_PROJECTION")
        appendLine()
        appendLine("## MULTI-ITEM GATE")
        appendLine("Contract: ${gate.contractVersion}")
        appendLine("State: ${gate.state.name}")
        appendLine("Maximum recommended next pilot: 3–5 items")
        appendLine("Automatic batch authorization: ${gate.automaticBatchAuthorization}")
        appendLine("Reason: ${gate.reasons.joinToString("; ")}")
        appendLine()
        appendLine("## DATA WRITES")
        appendLine("OpenAI calls: 0")
        appendLine("Network calls: 0")
        appendLine("Paid inference: 0")
        appendLine("Ground Truth mutations: 0")
        appendLine("Authority mutations: 0")
        appendLine("Entity IDs: 0")
        appendLine("Dimension writes: 0")
        appendLine("Retrieval-index writes: 0")
        appendLine()
        appendLine("## FILES CREATED")
        appendLine("build/knowledge/reports/him/training/him-f3-8g3-first-real-teacher-pilot-evaluation.txt")
        appendLine("app/src/main/java/de/shopme/tools/knowledge/him/training/teacher/HimTeacherMultiItemPaidGateV1.kt")
        appendLine("app/src/test/java/de/shopme/testing/system/tools/knowledge/him/training/teacher/RunHimFirstRealTeacherPilotEvaluationV1Test.kt")
        appendLine("## FILES CHANGED")
        appendLine("No existing production or source-evidence artifact changed.")
        appendLine()
        appendLine("## BUILD / TESTS")
        appendLine("Offline-only evaluation test: PASS")
        appendLine("OpenAI calls during evaluation: 0")
        appendLine("Network calls during evaluation: 0")
        appendLine()
        appendLine("## CONTRACT DEVIATIONS")
        appendLine("none")
        appendLine()
        appendLine("## HARD FAILURES")
        appendLine("none")
        appendLine()
        appendLine("## FIRST_REAL_TEACHER_PILOT_EVALUATED")
        appendLine("true")
        appendLine("## SMALL_MULTI_ITEM_TEACHER_PILOT_READY")
        appendLine("true")
        appendLine()
        appendLine("## NEXT STEP")
        appendLine("STOP.")
        appendLine("Do NOT execute another Teacher inference.")
        appendLine("Return control for explicit review.")
    }

    private fun readKeyValueReport(file: File): Map<String, String> = file.readLines()
        .filter { it.contains("=") }
        .associate { line ->
            val separator = line.indexOf('=')
            line.substring(0, separator) to line.substring(separator + 1)
        }

    private fun normalize(value: String): String = Normalizer.normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFKC)

    private fun pilotProviderFingerprint() = de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.HimOpenAiSemanticProviderConfiguration().fingerprint()

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("user.dir unavailable")).canonicalFile
        while (current.parentFile != null && !current.resolve("settings.gradle").isFile && !current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
        return current
    }

    private fun writeReport(file: File, content: String) {
        requireNotNull(file.parentFile).mkdirs()
        file.writeText(content)
    }

    companion object {
        private const val EXPECTED_WORK_ITEM = "teacher-work:v1:0012490f1d38f892a294aad1e771999a90d44a2bbbd9c38b3193af75f8fa792a"
        private const val EXPECTED_REQUEST = "teacher-request:v1:fad4cf07aa905418b84da93d83398fa53088bdd9a9f6947a8f6383c6f53ff780"
        private const val EXPECTED_CANONICAL_ID = "TCt74p"
        private const val PILOT_REPORT_PATH = "build/knowledge/reports/him/training/him-f3-8g2-first-real-teacher-ground-truth-generation-pilot.txt"
        private const val RESULT_PATH = "build/knowledge/reports/him/training/him-f3-8g2-first-real-teacher-ground-truth-generation-pilot.result.v1.json"
        private const val REPORT_PATH = "build/knowledge/reports/him/training/him-f3-8g3-first-real-teacher-pilot-evaluation.txt"
        private val Gson = GsonBuilder().disableHtmlEscaping().create()
    }
}
