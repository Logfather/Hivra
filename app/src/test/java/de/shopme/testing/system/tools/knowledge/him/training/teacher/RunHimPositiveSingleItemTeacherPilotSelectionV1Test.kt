package de.shopme.testing.system.tools.knowledge.him.training.teacher

import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalFamilyCandidateRetrieval
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalQuery
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimCanonicalRetrievalResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticContextBudgetPolicy
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceRequest
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.HimOpenAiSemanticProviderConfiguration
import de.shopme.tools.knowledge.him.training.scaling.HimCandidateDatasetBindingV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerInputV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationContractV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationPersistenceV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthGenerationRequestV1
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherGroundTruthPolicyBindingsV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingWorkReasonV1
import de.shopme.tools.knowledge.him.training.scaling.HimTeacherGroundTruthWorkItemV1
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotEvidence
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotEvidenceIndexBinding
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotEvidenceSummary
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotExecutionIdentity
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotOldMissionBinding
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotPreflightBinding
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotSelection
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotSelectionV1
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotSelectionV1Contract
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotCandidate
import de.shopme.tools.knowledge.him.training.teacher.HimPositiveSingleItemTeacherPilotCheckpointBinding
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherPaidPilotOfflinePreflightV2ProbeBinding
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherPaidPilotOfflinePreflightV2RetrievalBinding
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherPaidPilotOfflinePreflightV2Contract
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherPaidPilotOfflinePreflightV2PositiveE2EBinding
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherPaidPilotOfflinePreflightV2
import de.shopme.tools.knowledge.him.training.teacher.HimTeacherPaidPilotOfflinePreflightV2Status
import de.shopme.tools.knowledge.him.training.teacher.HimSmallMultiItemTeacherPilotSelectionV1
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

class RunHimPositiveSingleItemTeacherPilotSelectionV1Test {
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    private data class MissionItemDto(
        val workItemReference: String,
        val rawInput: String,
        val partition: String,
        val requestReference: String,
    )

    private data class FrozenMissionDto(
        val contract: String,
        val expectedItems: Int,
        val ordering: List<String>,
        val items: List<MissionItemDto>,
        val provider: String,
    )

    private data class EvidenceStore(
        val sourceArtifactSha256: HimSha256,
        val search: (String, HimEvidenceSearchLimit) -> List<HimEvidenceSearchResult>,
        val fetch: (HimEvidenceRecordReference) -> HimEvidenceSearchResult?,
    )

    private data class CandidateBuildOutcome(
        val candidate: HimPositiveSingleItemTeacherPilotCandidate?,
        val exclusionReason: String?,
    )

    private data class CandidateBuildBatch(
        val candidates: List<HimPositiveSingleItemTeacherPilotCandidate>,
        val exclusions: List<String>,
    )

    private companion object {
        const val TRAINING_DIRECTORY = "build/knowledge/reports/him/training"
        const val SELECTION_ARTIFACT_PATH = "$TRAINING_DIRECTORY/him-positive-single-item-teacher-paid-pilot.selection.v1.r2.json"
        const val OLD_MISSION_PATH = HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_PATH
    }

    @Test fun `selects exactly one validation item`() {
        val selection = select(candidate(1), candidate(2))
        assertEquals(1, selection.selectedRank)
        assertEquals(HimTrainingPartitionV1.VALIDATION, selection.selectedCandidate.partition)
        assertEquals(2, selection.rankedCandidates.size)
    }

    @Test fun `higher source coverage wins`() {
        val selected = select(candidate(1, coverage = setOf(HimGroundTruthSource.OPEN_FOOD_FACTS)), candidate(2, coverage = allSources())).selectedCandidate
        assertEquals(ref(2), selected.workItemReference)
    }

    @Test fun `higher direct evidence wins after equal coverage`() {
        val selected = select(candidate(1, direct = 1), candidate(2, direct = 2)).selectedCandidate
        assertEquals(ref(2), selected.workItemReference)
    }

    @Test fun `higher valid projection count wins after direct evidence`() {
        val selected = select(candidate(1, projections = 5), candidate(2, projections = 6)).selectedCandidate
        assertEquals(ref(2), selected.workItemReference)
    }

    @Test fun `complete tie uses ascending work item reference`() {
        val first = candidate(2)
        val second = candidate(1)
        val selection = select(first, second)
        assertEquals(ref(1), selection.selectedCandidate.workItemReference)
        assertEquals(listOf(ref(1), ref(2)), selection.originalCandidateOrder)
    }

    @Test fun `TRAIN candidates are rejected`() {
        assertFails { select(candidate(1, partition = HimTrainingPartitionV1.TRAIN)) }
    }

    @Test fun `old mission candidates are rejected`() {
        assertFails { select(candidateWithReference(HimPositiveSingleItemTeacherPilotSelectionV1Contract.EXCLUDED_WORK_ITEM_REFERENCES.first())) }
    }

    @Test fun `empty evidence is rejected`() {
        assertFails { select(candidate(1, coverage = emptySet())) }
    }

    @Test fun `missing direct evidence is rejected`() {
        assertFails { select(candidate(1, direct = 0)) }
    }

    @Test fun `exactly four unique index bindings are required`() {
        val input = baseInput()
        assertFails { buildSelection(input.copy(indexBindings = input.indexBindings.dropLast(1))) }
        assertFails { buildSelection(input.copy(indexBindings = input.indexBindings + input.indexBindings.first())) }
    }

    @Test fun `checkpoint head mismatch is rejected`() {
        val input = baseInput()
        assertFails { buildSelection(input.copy(preflight = input.preflight.copy(boundHeadSha256 = "b".repeat(40)))) }
    }

    @Test fun `non current V2 preflight is rejected`() {
        val input = baseInput()
        assertFails { buildSelection(input.copy(preflight = input.preflight.copy(runtimeEvaluation = "STALE"))) }
    }

    @Test fun `manipulated V2 digest binding is rejected`() {
        val input = baseInput()
        assertFails { buildSelection(input.copy(preflight = input.preflight.copy(artifactSha256 = HimSha256("0".repeat(64))))) }
    }

    @Test fun `selected item must be rank one`() {
        val selection = select(candidate(1), candidate(2))
        assertFails { HimPositiveSingleItemTeacherPilotSelectionV1.validate(selection.copy(selectedRank = 2)) }
    }

    @Test fun `persistence reload and identical second persistence are byte stable`() {
        val root = Files.createTempDirectory("him-positive-selection").toFile()
        val file = root.resolve("selection.v1.json")
        val selection = select(candidate(1), candidate(2))
        HimPositiveSingleItemTeacherPilotSelectionV1.writeIdempotent(file, selection)
        val firstBytes = file.readBytes()
        val reloaded = HimPositiveSingleItemTeacherPilotSelectionV1.read(file)
        HimPositiveSingleItemTeacherPilotSelectionV1.writeIdempotent(file, select(candidate(1), candidate(2)))
        assertEquals(selection, reloaded)
        assertArrayEquals(firstBytes, file.readBytes())
    }

    @Test fun `different second freeze at same path fails closed`() {
        val root = Files.createTempDirectory("him-positive-selection-conflict").toFile()
        val file = root.resolve("selection.v1.json")
        HimPositiveSingleItemTeacherPilotSelectionV1.writeIdempotent(file, select(candidate(1)))
        assertFails { HimPositiveSingleItemTeacherPilotSelectionV1.writeIdempotent(file, select(candidate(2))) }
    }

    @Test fun `source integration entrypoint is skipped without opt in`() {
        requireSourceIntegrationEnabled()
        check(System.getProperty("him.paidNetwork.enabled") != "true")
        executeRealSelection(projectRoot())
    }

    @Test fun `selection entrypoint keeps the real artifact path separate from the legacy mission`() {
        assertEquals(
            "build/knowledge/reports/him/training/him-positive-single-item-teacher-paid-pilot.selection.v1.r2.json",
            SELECTION_ARTIFACT_PATH,
        )
        assertFalse(SELECTION_ARTIFACT_PATH == HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_PATH)
    }

    @Test fun `new production file is included by the V2 manifest`() {
        val root = projectRoot()
        val manifest = HimTeacherPaidPilotOfflinePreflightV2.implementationManifest(root)
        assertTrue(manifest.entries.any { it.path == "app/src/main/java/de/shopme/tools/knowledge/him/training/teacher/HimPositiveSingleItemTeacherPilotSelectionV1.kt" })
    }

    @Test fun `legacy selector remains deterministic and three-item based`() {
        val items = listOf(
            workItem("a", HimTrainingPartitionV1.TRAIN),
            workItem("b", HimTrainingPartitionV1.VALIDATION),
            workItem("c", HimTrainingPartitionV1.HOLDOUT),
        )
        val selected = HimSmallMultiItemTeacherPilotSelectionV1.select(items, emptySet())
        assertEquals(3, selected.size)
        assertEquals(selected.map { it.reference }, selected.map { it.reference }.distinct())
    }

    @Test fun `canonical retrieval uses persisted family normalization`() {
        val family = testFamily(HimEntityId("A00001"), "Sataysoße", "sataysosse")
        val query = canonicalRetrievalQuery(family)
        val context = HimCanonicalFamilyCandidateRetrieval(listOf(family)).retrieve(query)

        assertEquals("Sataysoße", query.rawQuery)
        assertEquals("sataysosse", query.normalizedQuery)
        assertNotEquals(family.normalizedName, normalize(family.canonicalName))
        assertEquals(family.canonicalId, context.single().canonicalId)
    }

    @Test fun `canonical context miss excludes only that candidate without evidence access`() {
        val first = workItemFor("a", HimEntityId("A00001"), HimTrainingPartitionV1.VALIDATION)
        val second = workItemFor("b", HimEntityId("B00002"), HimTrainingPartitionV1.VALIDATION)
        val firstFamily = testFamily(first.canonicalId, "Missing context", "missing-context")
        val secondFamily = testFamily(second.canonicalId, "Present context", "present-context")

        fun run(counters: MutableMap<HimGroundTruthSource, Int>): CandidateBuildBatch = buildCandidates(
            listOf(first, second),
            mapOf(first.canonicalId to firstFamily, second.canonicalId to secondFamily),
            listOf(secondFamily),
            first.groundTruthReleaseReference,
            fakeEvidenceStores(counters),
        )

        val missCounters = linkedMapOf<HimGroundTruthSource, Int>()
        val missRun = buildCandidates(
            listOf(first),
            mapOf(first.canonicalId to firstFamily),
            listOf(secondFamily),
            first.groundTruthReleaseReference,
            fakeEvidenceStores(missCounters),
        )
        val firstCounters = linkedMapOf<HimGroundTruthSource, Int>()
        val firstRun = run(firstCounters)
        val secondCounters = linkedMapOf<HimGroundTruthSource, Int>()
        val secondRun = run(secondCounters)

        assertEquals(listOf("${first.reference}|CANONICAL_CONTEXT_NOT_FOUND"), missRun.exclusions)
        assertEquals(0, missCounters.values.sum())
        assertEquals(listOf("${first.reference}|CANONICAL_CONTEXT_NOT_FOUND"), firstRun.exclusions)
        assertEquals(firstRun.exclusions, secondRun.exclusions)
        assertEquals(listOf(second.reference), firstRun.candidates.map { it.workItemReference })
        assertEquals(firstRun.candidates.map { it.workItemReference }, secondRun.candidates.map { it.workItemReference })
        assertEquals(HimTrainingPartitionV1.VALIDATION, firstRun.candidates.single().partition)
        assertTrue(firstCounters.values.sum() > 0)
        assertTrue(secondCounters.values.sum() > 0)
        assertEquals(second.reference, select(firstRun.candidates.single()).selectedCandidate.workItemReference)
    }

    private fun executeRealSelection(root: File) {
        require(System.getProperty("him.paidNetwork.enabled") != "true")
        val selectionFile = root.resolve(SELECTION_ARTIFACT_PATH)
        val v2File = root.resolve(HimTeacherPaidPilotOfflinePreflightV2Contract.ARTIFACT)
        require(v2File.isFile)
        val v2 = HimTeacherPaidPilotOfflinePreflightV2.read(v2File)
        require(HimTeacherPaidPilotOfflinePreflightV2.evaluate(v2, v2) == HimTeacherPaidPilotOfflinePreflightV2Status.CURRENT)

        val head = command(root, "rev-parse", "HEAD")
        require(v2.checkpoint.headSha256 == head)
        val preflight = HimPositiveSingleItemTeacherPilotPreflightBinding.from(v2, HimSha256(sha256(v2File)))
        val checkpoint = HimPositiveSingleItemTeacherPilotCheckpointBinding(
            head,
            v2.checkpoint.headSha256,
            HimSha256(v2.implementationManifest.digest),
        )
        val oldMission = readFrozenMission(root)
        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val paths = HimCanonicalFamilyPaths(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
        val authority = HimCanonicalFamilyPersistence().readAuthority(active.authorityFile)
        val plan = HimCanonicalGroundTruthScalingPlannerV1().plan(
            HimCanonicalGroundTruthScalingPlannerInputV1(
                catalog,
                authority,
                active.releaseReference,
                candidateBinding(root),
                emptyList(),
            ),
        )
        val persisted = existingPaidWorkItems(root)
        val stores = openEvidenceStores(root)
        val indexBindings = v2.retrievalBindings
            .map { HimPositiveSingleItemTeacherPilotEvidenceIndexBinding(it, HimPositiveSingleItemTeacherPilotSelectionV1Contract.INDEX_VALIDATION_PASS) }
        require(indexBindings.map { it.retrievalBinding.source }.sorted() == HimPositiveSingleItemTeacherPilotSelectionV1Contract.REQUIRED_SOURCES)
        val configuration = HimOpenAiSemanticProviderConfiguration()
        val executionIdentity = HimPositiveSingleItemTeacherPilotExecutionIdentity(
            active.releaseReference,
            HimPositiveSingleItemTeacherPilotSelectionV1Contract.PROVIDER_CONTRACT,
            configuration.model,
            HimTeacherGroundTruthGenerationContractV1.REQUEST_SCHEMA_VERSION,
            HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION,
            HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
            configuration.fingerprint(),
        )
        val familyById = authority.families.associateBy { it.canonicalId }
        val eligible = plan.workItems
            .sortedBy { it.reference }
            .filter {
                it.partition == HimTrainingPartitionV1.VALIDATION &&
                    it.reference !in oldMission.excludedWorkItemReferences &&
                    it.reference !in persisted
            }
        val candidateBatch = buildCandidates(
            eligible,
            familyById,
            authority.families,
            active.releaseReference,
            stores,
        )
        candidateBatch.exclusions.forEach { println("REAL_POSITIVE_SINGLE_ITEM_SELECTION exclusion=$it") }
        if (candidateBatch.candidates.isEmpty()) {
            error("BLOCKED_NO_ELIGIBLE_EVIDENCE_RICH_VALIDATION_ITEM")
        }
        val candidates = candidateBatch.candidates
        val selection = HimPositiveSingleItemTeacherPilotSelectionV1.select(
            checkpoint,
            preflight,
            oldMission,
            executionIdentity,
            indexBindings,
            candidates,
        )
        val priorBytes = selectionFile.takeIf { it.isFile }?.readBytes()
        HimPositiveSingleItemTeacherPilotSelectionV1.writeIdempotent(selectionFile, selection)
        val reloaded = HimPositiveSingleItemTeacherPilotSelectionV1.read(selectionFile)
        require(reloaded == selection)
        if (priorBytes != null) assertArrayEquals(priorBytes, selectionFile.readBytes())
        println("REAL_POSITIVE_SINGLE_ITEM_SELECTION candidates=${candidates.size} selected=${selection.selectedCandidate.workItemReference} rawInput=${selection.selectedCandidate.rawInput} partition=${selection.selectedCandidate.partition}")
    }

    private fun readFrozenMission(root: File): HimPositiveSingleItemTeacherPilotOldMissionBinding {
        val file = root.resolve(OLD_MISSION_PATH)
        require(file.isFile)
        require(sha256(file) == HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_SHA256)
        val mission = gson.fromJson(file.readText(), FrozenMissionDto::class.java)
        require(mission.contract == HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_CONTRACT)
        require(mission.expectedItems == 3)
        require(mission.ordering == mission.items.map { it.workItemReference })
        require(mission.ordering == HimPositiveSingleItemTeacherPilotSelectionV1Contract.EXCLUDED_WORK_ITEM_REFERENCES)
        require(mission.provider == HimPositiveSingleItemTeacherPilotSelectionV1Contract.PROVIDER_CONTRACT)
        return HimPositiveSingleItemTeacherPilotOldMissionBinding(
            OLD_MISSION_PATH,
            HimSha256(HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_SHA256),
            mission.contract,
            mission.ordering,
        )
    }

    private fun candidateBinding(root: File): HimCandidateDatasetBindingV1? {
        val file = root.resolve("${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json")
        if (!file.isFile) return null
        return HimCandidateDatasetBindingV1(
            "${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json",
            HimCandidateIdentityV1.datasetDigest(HimCandidateDatasetPersistenceV2.readDataset(file)),
        )
    }

    private fun existingPaidWorkItems(root: File): Set<String> = root.resolve(TRAINING_DIRECTORY).listFiles().orEmpty()
        .filter { it.isFile && it.name.endsWith(".result.v1.json") }
        .mapNotNull { runCatching { HimTeacherGroundTruthGenerationPersistenceV1.readResult(it).workItemReference }.getOrNull() }
        .toSet()

    private fun openEvidenceStores(root: File): Map<HimGroundTruthSource, EvidenceStore> {
        val offFile = root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX)
        val offValidation = HimOffEvidenceIndexValidator.validateReadOnly(offFile)
        val off = HimOffSqliteEvidenceRetrievalStore.openAfterValidation(offFile, offValidation)
        val agribalyseFile = root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX)
        val agribalyseValidation = HimAgribalyseEvidenceIndexValidator.validateReadOnly(agribalyseFile)
        val agribalyse = HimAgribalyseSqliteEvidenceRetrievalStore.openAfterValidation(agribalyseFile, agribalyseValidation)
        val ciqualFile = root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX)
        val ciqualValidation = HimCiqualEvidenceIndexValidator.validateReadOnly(ciqualFile)
        val ciqual = HimCiqualSqliteEvidenceRetrievalStore.openAfterValidation(ciqualFile, ciqualValidation)
        val giFile = root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX)
        val giValidation = HimGlycemicIndexEvidenceIndexValidator.validateReadOnly(giFile)
        val gi = HimGlycemicIndexSqliteEvidenceRetrievalStore.openAfterValidation(giFile, giValidation)
        return linkedMapOf(
            HimGroundTruthSource.OPEN_FOOD_FACTS to EvidenceStore(HimSha256(offValidation.metadata.sourceArtifactSha256.value), off::search, off::fetch),
            HimGroundTruthSource.AGRIBALYSE to EvidenceStore(HimSha256(agribalyseValidation.metadata.sourceArtifactSha256.value), agribalyse::search, agribalyse::fetch),
            HimGroundTruthSource.CIQUAL to EvidenceStore(HimSha256(ciqualValidation.metadata.sourceArtifactSha256.value), ciqual::search, ciqual::fetch),
            HimGroundTruthSource.GLYCEMIC_INDEX to EvidenceStore(HimSha256(giValidation.metadata.sourceArtifactSha256.value), gi::search, gi::fetch),
        )
    }

    private fun buildCandidates(
        items: List<HimTeacherGroundTruthWorkItemV1>,
        familyById: Map<HimEntityId, HimCanonicalFamily>,
        families: List<HimCanonicalFamily>,
        release: HimGroundTruthReleaseIdentityV1,
        stores: Map<HimGroundTruthSource, EvidenceStore>,
    ): CandidateBuildBatch {
        val exclusions = mutableListOf<String>()
        val candidates = items.sortedBy { it.reference }.mapNotNull { item ->
            val family = familyById[item.canonicalId] ?: return@mapNotNull null
            val outcome = buildCandidate(item, family, families, release, stores)
            if (outcome.candidate == null && outcome.exclusionReason != null) {
                exclusions += "${item.reference}|${outcome.exclusionReason}"
            }
            outcome.candidate
        }.filter { it.validProjectionCount >= 1 && it.directEvidenceCount >= 1 }
        return CandidateBuildBatch(candidates, exclusions.sorted())
    }

    private fun buildCandidate(
        item: HimTeacherGroundTruthWorkItemV1,
        family: HimCanonicalFamily,
        families: List<HimCanonicalFamily>,
        release: HimGroundTruthReleaseIdentityV1,
        stores: Map<HimGroundTruthSource, EvidenceStore>,
    ): CandidateBuildOutcome {
        val context = HimCanonicalFamilyCandidateRetrieval(families).retrieve(
            canonicalRetrievalQuery(family),
        )
        if (context.none { it.canonicalId == item.canonicalId }) {
            return CandidateBuildOutcome(null, "CANONICAL_CONTEXT_NOT_FOUND")
        }
        val inference = HimSemanticInferenceRequest(
            "teacher-f3-8g4:${item.reference}",
            family.canonicalName,
            context,
            emptyList(),
            HimRetrievalRound(0),
        )
        val teacherContext = context.map { result ->
            HimCandidateCanonicalContext(
                result.rank,
                result.canonicalId,
                result.canonicalName,
                (result as? HimCanonicalRetrievalResult.Full)?.let { gson.toJson(it.family) },
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
        val evidenceBySource = HimGroundTruthSource.entries.map { source ->
            val store = stores.getValue(source)
            val hit = store.search(family.canonicalName, HimEvidenceSearchLimit(10)).firstOrNull()
            val fetched = hit?.let { store.fetch(it.sourceRecordReference) }
            val evidence = if (hit != null && fetched == hit) {
                listOf(
                    HimPositiveSingleItemTeacherPilotEvidence(
                        HimEvidenceReference(source.name, store.sourceArtifactSha256, hit.sourceRecordReference.value),
                        HimSemanticEvidenceRelation.DIRECT,
                    ),
                )
            } else {
                emptyList()
            }
            HimPositiveSingleItemTeacherPilotEvidenceSummary(source, evidence, evidence.size, evidence.size)
        }
        return CandidateBuildOutcome(
            HimPositiveSingleItemTeacherPilotCandidate(
                item.reference,
                family.canonicalId,
                family.canonicalName,
                item.partition,
                request.requestReference,
                release,
                evidenceBySource,
                evidenceBySource.count { it.validProjectionCount > 0 },
                evidenceBySource.sumOf { it.validProjectionCount },
                evidenceBySource.sumOf { it.directEvidenceCount },
            ),
            null,
        )
    }

    private fun canonicalRetrievalQuery(family: HimCanonicalFamily) =
        HimCanonicalRetrievalQuery(family.canonicalName, family.normalizedName)

    private fun testFamily(id: HimEntityId, name: String, normalized: String) = HimCanonicalFamily(
        canonicalId = id,
        canonicalName = name,
        normalizedName = normalized,
        taxonomyPaths = listOf(listOf("food", normalized)),
        lifecycleStatus = HimLifecycleStatus.ACTIVE,
        identities = emptyList(),
        variants = emptyList(),
        aliases = emptyList(),
    )

    private fun fakeEvidenceStores(counters: MutableMap<HimGroundTruthSource, Int>): Map<HimGroundTruthSource, EvidenceStore> =
        HimGroundTruthSource.entries.associateWith { source ->
            val reference = HimEvidenceRecordReference.parse(source, probeReference(source))
            val result = HimEvidenceSearchResult(
                source,
                reference,
                when (source) {
                    HimGroundTruthSource.OPEN_FOOD_FACTS -> HimEvidenceRecordKind.OFF_PRODUCT
                    HimGroundTruthSource.AGRIBALYSE -> HimEvidenceRecordKind.AGRIBALYSE_RECORD
                    HimGroundTruthSource.CIQUAL -> HimEvidenceRecordKind.CIQUAL_FOOD
                    HimGroundTruthSource.GLYCEMIC_INDEX -> HimEvidenceRecordKind.GI_MEASUREMENT
                },
                1,
                HimEvidenceProjection("{\"source\":\"${source.name}\"}"),
            )
            EvidenceStore(
                HimSha256("b".repeat(64)),
                { _, _ ->
                    counters[source] = (counters[source] ?: 0) + 1
                    listOf(result)
                },
                { requested -> result.takeIf { it.sourceRecordReference == requested } },
            )
        }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .trim()
        .lowercase(Locale.ROOT)

    private fun command(root: File, vararg args: String): String {
        val process = ProcessBuilder(listOf("git") + args.toList())
            .directory(root)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        require(process.waitFor() == 0) { output }
        return output
    }

    private fun sha256(file: File): String {
        require(file.isFile)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1024 * 1024).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun select(vararg candidates: HimPositiveSingleItemTeacherPilotCandidate) = buildSelection(baseInput().copy(candidates = candidates.toList()))

    private fun buildSelection(input: Input) = HimPositiveSingleItemTeacherPilotSelectionV1.select(
        input.checkpoint,
        input.preflight,
        input.oldMission,
        input.executionIdentity,
        input.indexBindings,
        input.candidates,
    )

    private data class Input(
        val checkpoint: HimPositiveSingleItemTeacherPilotCheckpointBinding,
        val preflight: HimPositiveSingleItemTeacherPilotPreflightBinding,
        val oldMission: HimPositiveSingleItemTeacherPilotOldMissionBinding,
        val executionIdentity: HimPositiveSingleItemTeacherPilotExecutionIdentity,
        val indexBindings: List<HimPositiveSingleItemTeacherPilotEvidenceIndexBinding>,
        val candidates: List<HimPositiveSingleItemTeacherPilotCandidate>,
    )

    private fun baseInput() = Input(
        HimPositiveSingleItemTeacherPilotCheckpointBinding("a".repeat(40), "a".repeat(40), HimSha256("b".repeat(64))),
        HimPositiveSingleItemTeacherPilotPreflightBinding(
            HimTeacherPaidPilotOfflinePreflightV2Contract.ARTIFACT,
            HimSha256("c".repeat(64)),
            HimTeacherPaidPilotOfflinePreflightV2Contract.VERSION,
            "VALIDATED", "PASS", "CURRENT", "a".repeat(40), HimSha256("d".repeat(64)), HimSha256("b".repeat(64)), HimSha256("e".repeat(64)),
        ),
        HimPositiveSingleItemTeacherPilotOldMissionBinding(
            HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_PATH,
            HimSha256(HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_SHA256),
            HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_CONTRACT,
            HimPositiveSingleItemTeacherPilotSelectionV1Contract.EXCLUDED_WORK_ITEM_REFERENCES,
        ),
        HimPositiveSingleItemTeacherPilotExecutionIdentity(
            HimGroundTruthReleaseIdentityV1("release:v1:${"f".repeat(64)}"),
            HimPositiveSingleItemTeacherPilotSelectionV1Contract.PROVIDER_CONTRACT,
            "gpt-5.6-sol",
            HimPositiveSingleItemTeacherPilotSelectionV1Contract.REQUEST_SCHEMA,
            HimPositiveSingleItemTeacherPilotSelectionV1Contract.OUTPUT_SCHEMA,
            "HIM_SEMANTIC_INFERENCE_INSTRUCTION_POLICY_V2_2",
            HimSha256("f".repeat(64)),
        ),
        allSources().mapIndexed { index, source -> indexBinding(source, index) },
        emptyList(),
    )

    private fun indexBinding(source: HimGroundTruthSource, index: Int): HimPositiveSingleItemTeacherPilotEvidenceIndexBinding {
        val sourceName = source.name
        val sha = ('1'.code + index).toString(16).repeat(64).take(64)
        val reference = probeReference(source)
        val probe = HimTeacherPaidPilotOfflinePreflightV2ProbeBinding(sourceName, "INDEXED_PRIMARY_KEY_LIMIT_1", reference, "FIXTURE", "2".repeat(64), 1, 1, "PASS")
        val retrieval = HimTeacherPaidPilotOfflinePreflightV2RetrievalBinding(
            sourceName,
            "F3D_2_RETRIEVAL_FOUNDATION_V1",
            HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding("data/index-$index.sqlite", 10L, sha, "HIM_PRODUCTION_SQLITE_FILE_SHA256_V1", "3".repeat(64)),
            sha,
            "HIM_EVIDENCE_RETRIEVAL_INDEX_SCHEMA_V1",
            "HIM_EVIDENCE_RETRIEVAL_INDEX_BUILD_V1",
            "HIM_EVIDENCE_PROJECTION_V1",
            "4".repeat(64),
            1,
            1,
            probe,
        )
        return HimPositiveSingleItemTeacherPilotEvidenceIndexBinding(retrieval, "PASS")
    }

    private fun candidate(
        index: Int,
        coverage: Set<HimGroundTruthSource> = allSources(),
        direct: Int = 1,
        projections: Int = maxOf(direct, 1),
        partition: HimTrainingPartitionV1 = HimTrainingPartitionV1.VALIDATION,
    ) = candidateWithReference(ref(index), coverage, direct, projections, partition)

    private fun candidateWithReference(
        reference: String,
        coverage: Set<HimGroundTruthSource> = allSources(),
        direct: Int = 1,
        projections: Int = maxOf(direct, 1),
        partition: HimTrainingPartitionV1 = HimTrainingPartitionV1.VALIDATION,
    ): HimPositiveSingleItemTeacherPilotCandidate {
        val evidence = allSources().map { source ->
            val coveredSources = allSources().filter { it in coverage }
            val totalProjections = maxOf(coverage.size, projections, direct + coverage.size - 1)
            val count = if (source in coverage) {
                if (source == coveredSources.firstOrNull()) totalProjections - (coveredSources.size - 1) else 1
            } else 0
            val directCount = minOf(count, if (source == coveredSources.firstOrNull()) direct else 0)
            HimPositiveSingleItemTeacherPilotEvidenceSummary(
                source,
                (1..count).map { ordinal ->
                    HimPositiveSingleItemTeacherPilotEvidence(
                        HimEvidenceReference(source.name, HimSha256("a".repeat(64)), evidenceReference(source, ordinal, reference)),
                        if (ordinal <= directCount) HimSemanticEvidenceRelation.DIRECT else HimSemanticEvidenceRelation.RELATED,
                    )
                },
                count,
                directCount,
            )
        }
        return HimPositiveSingleItemTeacherPilotCandidate(
            reference,
            HimEntityId("E${reference.takeLast(5)}"),
            "Food-$reference",
            partition,
            "teacher-request:v1:${reference.removePrefix("teacher-work:v1:")}",
            HimGroundTruthReleaseIdentityV1("release:v1:${"f".repeat(64)}"),
            evidence,
            evidence.count { it.validProjectionCount > 0 },
            evidence.sumOf { it.validProjectionCount },
            evidence.sumOf { it.directEvidenceCount },
        )
    }

    private fun evidenceReference(source: HimGroundTruthSource, ordinal: Int, candidateReference: String): String = when (source) {
        HimGroundTruthSource.OPEN_FOOD_FACTS -> "off:product:row:$ordinal:code:${candidateReference.takeLast(8)}"
        HimGroundTruthSource.AGRIBALYSE -> "agribalyse:row:$ordinal:agb:${candidateReference.takeLast(8)}"
        HimGroundTruthSource.CIQUAL -> "ciqual:food:${candidateReference.takeLast(6)}$ordinal"
        HimGroundTruthSource.GLYCEMIC_INDEX -> "gi:measurement:$ordinal"
    }

    private fun probeReference(source: HimGroundTruthSource): String = when (source) {
        HimGroundTruthSource.OPEN_FOOD_FACTS -> "off:product:row:1:code:fixture"
        HimGroundTruthSource.AGRIBALYSE -> "agribalyse:row:1:agb:fixture"
        HimGroundTruthSource.CIQUAL -> "ciqual:food:fixture"
        HimGroundTruthSource.GLYCEMIC_INDEX -> "gi:measurement:1"
    }

    private fun allSources() = HimGroundTruthSource.entries.toSet()
    private fun ref(index: Int) = "teacher-work:v1:${index.toString(16).padStart(64, '0')}"
    private fun workItem(value: String, partition: HimTrainingPartitionV1) = workItemFor(
        value,
        HimEntityId("W${value}0000"),
        partition,
    )

    private fun workItemFor(
        value: String,
        canonicalId: HimEntityId,
        partition: HimTrainingPartitionV1,
    ) = HimTeacherGroundTruthWorkItemV1(
        "teacher-work:v1:${value.repeat(64)}",
        canonicalId,
        partition,
        listOf(HimTrainingClassificationV1.IDENTITY),
        HimCanonicalGroundTruthScalingWorkReasonV1.MISSING_SEMANTIC_COVERAGE,
        HimGroundTruthReleaseIdentityV1("release:v1:${"f".repeat(64)}"),
    )

    private fun projectRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (!current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
        return current
    }

    private fun assertFails(block: () -> Unit) = assertTrue(runCatching(block).isFailure)
}
