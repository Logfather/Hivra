package de.shopme.tools.knowledge.him.training.teacher

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object HimPositiveSingleItemTeacherPilotSelectionV1Contract {
    const val VERSION = "HIM_POSITIVE_SINGLE_ITEM_TEACHER_PAID_PILOT_SELECTION_V1"
    const val PURPOSE = "POSITIVE_SINGLE_ITEM_PAID_PILOT"
    const val MISSION_ID = "him-positive-single-item-teacher-paid-pilot-v1"
    const val V2_ARTIFACT_PATH = HimTeacherPaidPilotOfflinePreflightV2Contract.ARTIFACT
    const val OLD_MISSION_PATH = "build/knowledge/reports/him/training/him-f3-8g4-small-multi-item-teacher-paid-pilot.selection.v1.json"
    const val OLD_MISSION_SHA256 = "b418ef2ada657d8eb18d4dfc986026ef9656b870f304f13e2917d24be9730320"
    const val OLD_MISSION_CONTRACT = "HIM_SMALL_MULTI_ITEM_TEACHER_PAID_PILOT_V1"
    const val PROVIDER_CONTRACT = "OPENAI"
    const val REQUEST_SCHEMA = HimTeacherGroundTruthGenerationContractV1.REQUEST_SCHEMA_VERSION
    const val OUTPUT_SCHEMA = HimTeacherGroundTruthGenerationContractV1.OUTPUT_SCHEMA_VERSION
    const val INDEX_VALIDATION_PASS = "PASS"

    val REQUIRED_SOURCES = HimGroundTruthSource.entries.map { it.name }.sorted()
    val EXCLUDED_WORK_ITEM_REFERENCES = listOf(
        "teacher-work:v1:008b98dedc437b349ec438e8aa90d29bb7139ae6528af7d3207f2dfe00e83192",
        "teacher-work:v1:01a250090599f76309687986bfd1a7c0273f3c74f20a5a04f82ec6dd41b023ee",
        "teacher-work:v1:03c60a01f03b183ce09b2ba5822b6e0b3cc02bd6770bea4761db881679593a70",
    )
}

data class HimPositiveSingleItemTeacherPilotCheckpointBinding(
    val headSha256: String,
    val expectedHeadSha256: String,
    val implementationManifestDigest: HimSha256,
) {
    init {
        require(headSha256.matches(Regex("[0-9a-f]{40}")))
        require(expectedHeadSha256 == headSha256)
    }
}

data class HimPositiveSingleItemTeacherPilotPreflightBinding(
    val artifactPath: String,
    val artifactSha256: HimSha256,
    val contractVersion: String,
    val state: String,
    val validationResult: String,
    val runtimeEvaluation: String,
    val boundHeadSha256: String,
    val logicalArtifactDigest: HimSha256,
    val implementationManifestDigest: HimSha256,
    val canonicalCatalogDigest: HimSha256,
) {
    init {
        require(artifactPath == HimPositiveSingleItemTeacherPilotSelectionV1Contract.V2_ARTIFACT_PATH)
        require(contractVersion == HimTeacherPaidPilotOfflinePreflightV2Contract.VERSION)
        require(boundHeadSha256.matches(Regex("[0-9a-f]{40}")))
    }

    companion object {
        fun from(artifact: HimTeacherPaidPilotOfflinePreflightV2Artifact, artifactSha256: HimSha256) =
            HimPositiveSingleItemTeacherPilotPreflightBinding(
                HimTeacherPaidPilotOfflinePreflightV2Contract.ARTIFACT,
                artifactSha256,
                artifact.contractVersion,
                artifact.state,
                artifact.validationResult,
                artifact.runtimeEvaluation,
                artifact.checkpoint.headSha256,
                HimSha256(artifact.logicalArtifactDigest),
                HimSha256(artifact.implementationManifest.digest),
                HimSha256(artifact.canonicalCatalog.artifact.sha256),
            )
    }
}

data class HimPositiveSingleItemTeacherPilotOldMissionBinding(
    val artifactPath: String,
    val artifactSha256: HimSha256,
    val missionContract: String,
    val excludedWorkItemReferences: List<String>,
)

data class HimPositiveSingleItemTeacherPilotExecutionIdentity(
    val groundTruthRelease: HimGroundTruthReleaseIdentityV1,
    val providerContractId: String,
    val modelId: String,
    val requestSchemaVersion: String,
    val outputSchemaVersion: String,
    val instructionPolicyVersion: String,
    val providerFingerprint: HimSha256,
)

data class HimPositiveSingleItemTeacherPilotEvidenceIndexBinding(
    val retrievalBinding: HimTeacherPaidPilotOfflinePreflightV2RetrievalBinding,
    val releaseValidationStatus: String,
)

data class HimPositiveSingleItemTeacherPilotEvidence(
    val reference: HimEvidenceReference,
    val relation: HimSemanticEvidenceRelation,
)

data class HimPositiveSingleItemTeacherPilotEvidenceSummary(
    val source: HimGroundTruthSource,
    val evidence: List<HimPositiveSingleItemTeacherPilotEvidence>,
    val validProjectionCount: Int,
    val directEvidenceCount: Int,
)

data class HimPositiveSingleItemTeacherPilotCandidate(
    val workItemReference: String,
    val entityId: HimEntityId,
    val rawInput: String,
    val partition: HimTrainingPartitionV1,
    val requestReference: String,
    val groundTruthRelease: HimGroundTruthReleaseIdentityV1,
    val evidenceBySource: List<HimPositiveSingleItemTeacherPilotEvidenceSummary>,
    val sourceCoverageCount: Int,
    val validProjectionCount: Int,
    val directEvidenceCount: Int,
)

data class HimPositiveSingleItemTeacherPilotRankedCandidate(
    val rank: Int,
    val candidate: HimPositiveSingleItemTeacherPilotCandidate,
)

data class HimPositiveSingleItemTeacherPilotSelection(
    val contractVersion: String,
    val purpose: String,
    val missionId: String,
    val checkpoint: HimPositiveSingleItemTeacherPilotCheckpointBinding,
    val preflight: HimPositiveSingleItemTeacherPilotPreflightBinding,
    val oldMission: HimPositiveSingleItemTeacherPilotOldMissionBinding,
    val executionIdentity: HimPositiveSingleItemTeacherPilotExecutionIdentity,
    val indexBindings: List<HimPositiveSingleItemTeacherPilotEvidenceIndexBinding>,
    val originalCandidateOrder: List<String>,
    val originalCandidateOrderDigest: HimSha256,
    val rankedCandidates: List<HimPositiveSingleItemTeacherPilotRankedCandidate>,
    val rankedCandidatesDigest: HimSha256,
    val selectedRank: Int,
    val selectedCandidate: HimPositiveSingleItemTeacherPilotCandidate,
    val logicalDigest: HimSha256,
)

object HimPositiveSingleItemTeacherPilotSelectionV1 {
    private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    private val sha256Pattern = Regex("[0-9a-f]{64}")
    private val workItemPattern = Regex("teacher-work:v1:[0-9a-f]{64}")
    private val requestPattern = Regex("teacher-request:v1:[0-9a-f]{64}")

    fun select(
        checkpoint: HimPositiveSingleItemTeacherPilotCheckpointBinding,
        preflight: HimPositiveSingleItemTeacherPilotPreflightBinding,
        oldMission: HimPositiveSingleItemTeacherPilotOldMissionBinding,
        executionIdentity: HimPositiveSingleItemTeacherPilotExecutionIdentity,
        indexBindings: List<HimPositiveSingleItemTeacherPilotEvidenceIndexBinding>,
        candidates: List<HimPositiveSingleItemTeacherPilotCandidate>,
    ): HimPositiveSingleItemTeacherPilotSelection {
        val canonicalCandidates = candidates.sortedBy { it.workItemReference }
        val ranked = rankCandidates(canonicalCandidates, preflight, oldMission, indexBindings)
        require(ranked.isNotEmpty()) { "No evidence-rich VALIDATION candidate is eligible" }
        val unsigned = HimPositiveSingleItemTeacherPilotSelection(
            HimPositiveSingleItemTeacherPilotSelectionV1Contract.VERSION,
            HimPositiveSingleItemTeacherPilotSelectionV1Contract.PURPOSE,
            HimPositiveSingleItemTeacherPilotSelectionV1Contract.MISSION_ID,
            checkpoint,
            preflight,
            oldMission.copy(excludedWorkItemReferences = oldMission.excludedWorkItemReferences.sorted()),
            executionIdentity,
            indexBindings.sortedBy { it.retrievalBinding.source },
            canonicalCandidates.map { it.workItemReference },
            orderDigest(canonicalCandidates.map { it.workItemReference }),
            ranked,
            orderDigest(ranked.map { it.candidate.workItemReference }),
            1,
            ranked.first().candidate,
            HimSha256("0".repeat(64)),
        )
        validate(unsigned.copy(logicalDigest = HimSha256(digest(unsigned))))
        return unsigned.copy(logicalDigest = HimSha256(digest(unsigned)))
    }

    fun rankCandidates(
        candidates: List<HimPositiveSingleItemTeacherPilotCandidate>,
        preflight: HimPositiveSingleItemTeacherPilotPreflightBinding,
        oldMission: HimPositiveSingleItemTeacherPilotOldMissionBinding,
        indexBindings: List<HimPositiveSingleItemTeacherPilotEvidenceIndexBinding>,
    ): List<HimPositiveSingleItemTeacherPilotRankedCandidate> {
        val unique = candidates.sortedBy { it.workItemReference }
        require(unique.map { it.workItemReference }.distinct().size == unique.size)
        unique.forEach { validateCandidate(it, preflight, oldMission, indexBindings) }
        return unique.sortedWith(
            compareByDescending<HimPositiveSingleItemTeacherPilotCandidate> { it.sourceCoverageCount }
                .thenByDescending { it.directEvidenceCount }
                .thenByDescending { it.validProjectionCount }
                .thenBy { it.workItemReference },
        ).mapIndexed { index, candidate -> HimPositiveSingleItemTeacherPilotRankedCandidate(index + 1, candidate) }
    }

    fun validate(selection: HimPositiveSingleItemTeacherPilotSelection) {
        require(selection.contractVersion == HimPositiveSingleItemTeacherPilotSelectionV1Contract.VERSION)
        require(selection.purpose == HimPositiveSingleItemTeacherPilotSelectionV1Contract.PURPOSE)
        require(selection.missionId == HimPositiveSingleItemTeacherPilotSelectionV1Contract.MISSION_ID)
        require(selection.checkpoint.headSha256 == selection.checkpoint.expectedHeadSha256)
        require(selection.preflight.boundHeadSha256 == selection.checkpoint.headSha256)
        require(sha256Pattern.matches(selection.preflight.artifactSha256.value))
        require(selection.preflight.state == "VALIDATED" && selection.preflight.validationResult == "PASS" && selection.preflight.runtimeEvaluation == "CURRENT")
        require(sha256Pattern.matches(selection.preflight.logicalArtifactDigest.value))
        require(sha256Pattern.matches(selection.preflight.implementationManifestDigest.value))
        require(sha256Pattern.matches(selection.preflight.canonicalCatalogDigest.value))
        require(selection.preflight.implementationManifestDigest == selection.checkpoint.implementationManifestDigest)
        require(selection.oldMission.artifactPath == HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_PATH)
        require(selection.oldMission.artifactSha256.value == HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_SHA256)
        require(selection.oldMission.missionContract == HimPositiveSingleItemTeacherPilotSelectionV1Contract.OLD_MISSION_CONTRACT)
        require(selection.oldMission.excludedWorkItemReferences == HimPositiveSingleItemTeacherPilotSelectionV1Contract.EXCLUDED_WORK_ITEM_REFERENCES)
        require(selection.executionIdentity.providerContractId == HimPositiveSingleItemTeacherPilotSelectionV1Contract.PROVIDER_CONTRACT)
        require(selection.executionIdentity.requestSchemaVersion == HimPositiveSingleItemTeacherPilotSelectionV1Contract.REQUEST_SCHEMA)
        require(selection.executionIdentity.outputSchemaVersion == HimPositiveSingleItemTeacherPilotSelectionV1Contract.OUTPUT_SCHEMA)
        validateIndexBindings(selection.indexBindings)
        require(selection.originalCandidateOrder == selection.originalCandidateOrder.distinct().sorted())
        require(selection.originalCandidateOrderDigest == orderDigest(selection.originalCandidateOrder))
        require(selection.rankedCandidates.map { it.rank } == (1..selection.rankedCandidates.size).toList())
        require(selection.rankedCandidates.map { it.candidate.workItemReference }.toSet() == selection.originalCandidateOrder.toSet())
        require(selection.rankedCandidatesDigest == orderDigest(selection.rankedCandidates.map { it.candidate.workItemReference }))
        selection.rankedCandidates.forEach { validateCandidate(it.candidate, selection.preflight, selection.oldMission, selection.indexBindings) }
        val expected = selection.rankedCandidates.map { it.candidate }.sortedWith(comparator())
        require(selection.rankedCandidates.map { it.candidate } == expected)
        require(selection.selectedRank == 1)
        require(selection.selectedCandidate == selection.rankedCandidates.first().candidate)
        require(selection.selectedCandidate.partition == HimTrainingPartitionV1.VALIDATION)
        require(selection.logicalDigest == HimSha256(digest(selection.copy(logicalDigest = HimSha256("0".repeat(64))))) )
    }

    fun serialize(selection: HimPositiveSingleItemTeacherPilotSelection): ByteArray = (gson.toJson(selection) + "\n").toByteArray(Charsets.UTF_8)

    fun writeIdempotent(file: File, selection: HimPositiveSingleItemTeacherPilotSelection) {
        validate(selection)
        val bytes = serialize(selection)
        if (file.exists()) {
            require(file.readBytes().contentEquals(bytes)) { "Existing selection artifact differs; refusing freeze-path overwrite" }
            return
        }
        val parent = requireNotNull(file.parentFile)
        require(parent.exists() || parent.mkdirs())
        val temporary = Files.createTempFile(parent.toPath(), ".${file.name}.", ".tmp")
        try {
            Files.write(temporary, bytes)
            Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    fun read(file: File): HimPositiveSingleItemTeacherPilotSelection {
        val selection = requireNotNull(gson.fromJson(file.readText(), HimPositiveSingleItemTeacherPilotSelection::class.java))
        validate(selection)
        return selection
    }

    fun digest(selection: HimPositiveSingleItemTeacherPilotSelection): String = sha256(gson.toJson(selection) + "\n")

    private fun validateIndexBindings(bindings: List<HimPositiveSingleItemTeacherPilotEvidenceIndexBinding>) {
        require(bindings.map { it.retrievalBinding.source }.distinct().size == bindings.size)
        require(bindings.map { it.retrievalBinding.source }.sorted() == HimPositiveSingleItemTeacherPilotSelectionV1Contract.REQUIRED_SOURCES)
        bindings.forEach { binding ->
            val retrieval = binding.retrievalBinding
            require(binding.releaseValidationStatus == HimPositiveSingleItemTeacherPilotSelectionV1Contract.INDEX_VALIDATION_PASS)
            require(retrieval.index.path.isNotBlank() && retrieval.index.byteSize > 0 && sha256Pattern.matches(retrieval.index.sha256))
            require(retrieval.index.sha256 == retrieval.frozenIndexSha256)
            require(retrieval.probe.source == retrieval.source && retrieval.probe.validationResult == "PASS")
            require(retrieval.probe.returnedHitCount == 1 && retrieval.probe.loadedProjectionCount == 1)
        }
    }

    private fun validateCandidate(
        candidate: HimPositiveSingleItemTeacherPilotCandidate,
        preflight: HimPositiveSingleItemTeacherPilotPreflightBinding,
        oldMission: HimPositiveSingleItemTeacherPilotOldMissionBinding,
        indexBindings: List<HimPositiveSingleItemTeacherPilotEvidenceIndexBinding>,
    ) {
        require(workItemPattern.matches(candidate.workItemReference))
        require(candidate.partition == HimTrainingPartitionV1.VALIDATION)
        require(candidate.workItemReference !in oldMission.excludedWorkItemReferences)
        require(candidate.rawInput.isNotBlank() && requestPattern.matches(candidate.requestReference))
        require(candidate.groundTruthRelease.value.isNotBlank())
        val indexSources = indexBindings.map { it.retrievalBinding.source }.toSet()
        require(candidate.evidenceBySource.map { it.source.name }.sorted() == HimPositiveSingleItemTeacherPilotSelectionV1Contract.REQUIRED_SOURCES)
        val references = candidate.evidenceBySource.flatMap { summary ->
            require(summary.validProjectionCount >= 0)
            require(summary.validProjectionCount == summary.evidence.size)
            require(summary.directEvidenceCount == summary.evidence.count { it.relation == HimSemanticEvidenceRelation.DIRECT })
            require(summary.source.name in indexSources)
            summary.evidence.map { evidence ->
                require(evidence.reference.source == summary.source.name)
                HimEvidenceRecordReference.parse(summary.source, evidence.reference.sourceRecordIdentity)
                evidence.reference
            }
        }
        require(references.isNotEmpty())
        require(references.distinct().size == references.size)
        require(candidate.sourceCoverageCount == candidate.evidenceBySource.count { it.validProjectionCount > 0 })
        require(candidate.validProjectionCount == candidate.evidenceBySource.sumOf { it.validProjectionCount })
        require(candidate.directEvidenceCount == candidate.evidenceBySource.sumOf { it.directEvidenceCount })
        require(candidate.sourceCoverageCount >= 1 && candidate.directEvidenceCount >= 1)
        require(preflight.state == "VALIDATED" && preflight.validationResult == "PASS" && preflight.runtimeEvaluation == "CURRENT")
    }

    private fun comparator() = compareByDescending<HimPositiveSingleItemTeacherPilotCandidate> { it.sourceCoverageCount }
        .thenByDescending { it.directEvidenceCount }
        .thenByDescending { it.validProjectionCount }
        .thenBy { it.workItemReference }

    private fun orderDigest(values: List<String>): HimSha256 = HimSha256(sha256(values.joinToString("\n") + "\n"))
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
