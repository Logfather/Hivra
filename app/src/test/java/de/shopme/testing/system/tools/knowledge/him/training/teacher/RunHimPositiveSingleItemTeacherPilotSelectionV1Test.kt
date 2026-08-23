package de.shopme.testing.system.tools.knowledge.him.training.teacher

import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
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
import de.shopme.tools.knowledge.him.training.teacher.HimSmallMultiItemTeacherPilotSelectionV1
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class RunHimPositiveSingleItemTeacherPilotSelectionV1Test {
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
        error("Real four-store selection is reserved for the separate freeze mission")
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
    private fun workItem(value: String, partition: HimTrainingPartitionV1) = HimTeacherGroundTruthWorkItemV1(
        "teacher-work:v1:${value.repeat(64)}",
        HimEntityId("W${value}0000"),
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
