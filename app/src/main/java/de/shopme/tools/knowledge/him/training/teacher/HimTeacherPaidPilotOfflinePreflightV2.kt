package de.shopme.tools.knowledge.him.training.teacher

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyFoundationReleaseBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceArtifactIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object HimTeacherPaidPilotOfflinePreflightV2Contract {
    const val VERSION = "HIM_TEACHER_PAID_PILOT_OFFLINE_PREFLIGHT_V2"
    const val ARTIFACT = "build/knowledge/reports/him/training/him-teacher-paid-pilot-offline-preflight.v2.json"
    const val MANIFEST_VERSION = "HIM_IMPLEMENTATION_MANIFEST_V1"
    const val CHECKPOINT_VERSION = "HIM_REPRODUCIBLE_CHECKPOINT_V1"
    const val CANONICAL_CATALOG_BINDING = "HIM_CANONICAL_CATALOG_RELEASE_BINDING_V1"
    const val PERSISTENT_ARTIFACT_BINDING = "HIM_PERSISTENT_ARTIFACT_BINDING_V1"
    const val RETRIEVAL_BINDING = "HIM_REAL_RETRIEVAL_BINDING_V1"
    const val PROBE_CONTRACT = "HIM_BOUNDED_EVIDENCE_PROBE_V1"
    const val POSITIVE_GATE = "de.shopme.testing.system.tools.knowledge.him.training.teacher.RunHimTeacherPositiveProposalEndToEndV1Test"
    const val POSITIVE_GATE_DIGEST = "HIM_POSITIVE_OFFLINE_E2E_PASS_V1"
    const val RETRIEVAL_RELEASE_PATH = "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json"
    const val PRODUCTION_INDEX_RELEASE_PATH = "data/knowledge/him/retrieval/master/him-production-index-file-identity-release.v1.json"
    const val CANDIDATE_DATASET_PATH = "data/knowledge/him/candidates/master/candidate-dataset.v2.json"
    const val CANDIDATE_RUNS_DIRECTORY = "data/knowledge/him/candidates/runs"
    const val CATALOG_PATH = HimCanonicalFamilyPaths.PRODUCT_ONLY_MASTER_PATH
    const val CATALOG_SHA256 = "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f"

    val PERSISTENT_PATHS = listOf(
        "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json",
        "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json",
        "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json",
        CANDIDATE_DATASET_PATH,
        "data/knowledge/him/candidates/runs/6d45b0fc9d2182961f89a5a4aea7602acd790565ce09629ff0d2cb93b2692ff7.candidate-generation-run.v2.json",
        "data/knowledge/him/candidates/runs/c1ab93c23dde76444a3585afd80b592038ecf9c14e39442846ddbaf7671a0823.candidate-generation-run.v2.json",
        RETRIEVAL_RELEASE_PATH,
        PRODUCTION_INDEX_RELEASE_PATH,
    )
}

data class HimTeacherPaidPilotOfflinePreflightV2Checkpoint(
    val checkpointContract: String,
    val headSha256: String,
    val relevantWorktreeState: String,
)

data class HimTeacherPaidPilotOfflinePreflightV2ManifestEntry(
    val path: String,
    val byteSize: Long,
    val sha256: String,
)

data class HimTeacherPaidPilotOfflinePreflightV2ImplementationManifest(
    val manifestContract: String,
    val entries: List<HimTeacherPaidPilotOfflinePreflightV2ManifestEntry>,
    val digest: String,
)

data class HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding(
    val path: String,
    val byteSize: Long,
    val sha256: String,
    val identity: String,
    val logicalDigest: String?,
)

data class HimTeacherPaidPilotOfflinePreflightV2CanonicalBinding(
    val contract: String,
    val artifact: HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding,
    val releaseVersion: String,
    val foundationIdentity: String,
    val authorityIdentity: String,
    val entityIdRegistryIdentity: String,
)

data class HimTeacherPaidPilotOfflinePreflightV2ProbeBinding(
    val source: String,
    val probeKind: String,
    val evidenceReference: String,
    val evidenceRelationType: String,
    val projectionDigest: String,
    val returnedHitCount: Int,
    val loadedProjectionCount: Int,
    val validationResult: String,
)

data class HimTeacherPaidPilotOfflinePreflightV2RetrievalBinding(
    val source: String,
    val releaseIdentity: String,
    val index: HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding,
    val frozenIndexSha256: String,
    val schemaVersion: String,
    val buildPolicyVersion: String,
    val projectionPolicyVersion: String,
    val logicalIndexDigest: String,
    val evidenceRows: Long,
    val ftsRows: Long,
    val probe: HimTeacherPaidPilotOfflinePreflightV2ProbeBinding,
)

data class HimTeacherPaidPilotOfflinePreflightV2PositiveE2EBinding(
    val testClass: String,
    val result: String,
    val assertions: List<String>,
    val fixtureContractDigest: String,
)

data class HimTeacherPaidPilotOfflinePreflightV2Artifact(
    val contractVersion: String,
    val state: String,
    val validationResult: String,
    val runtimeEvaluation: String,
    val checkpoint: HimTeacherPaidPilotOfflinePreflightV2Checkpoint,
    val implementationManifest: HimTeacherPaidPilotOfflinePreflightV2ImplementationManifest,
    val canonicalCatalog: HimTeacherPaidPilotOfflinePreflightV2CanonicalBinding,
    val persistentArtifacts: List<HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding>,
    val retrievalBindings: List<HimTeacherPaidPilotOfflinePreflightV2RetrievalBinding>,
    val positiveOfflineE2E: HimTeacherPaidPilotOfflinePreflightV2PositiveE2EBinding,
    val logicalArtifactDigest: String,
)

enum class HimTeacherPaidPilotOfflinePreflightV2Status { CURRENT, INVALID, STALE_IMPLEMENTATION_CHECKPOINT, STALE_CANONICAL_BINDING, STALE_RETRIEVAL_BINDING }

object HimTeacherPaidPilotOfflinePreflightV2 {
    private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    private val sha256Pattern = Regex("[0-9a-f]{64}")
    private val headPattern = Regex("[0-9a-f]{40}")

    fun implementationManifest(root: File): HimTeacherPaidPilotOfflinePreflightV2ImplementationManifest {
        val paths = buildList {
            root.resolve("app/src/main/java/de/shopme/tools/knowledge/him").walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { add(relative(root, it)) }
            add("app/src/main/java/de/shopme/tools/knowledge/data/KnowledgeDataDirectories.kt")
        }.distinct().sorted()
        require(paths.isNotEmpty())
        val entries = paths.map { path ->
            val file = root.resolve(path)
            require(file.isFile) { "Implementation manifest file is missing: $path" }
            HimTeacherPaidPilotOfflinePreflightV2ManifestEntry(path, file.length(), sha256(file.readBytes()))
        }
        return HimTeacherPaidPilotOfflinePreflightV2ImplementationManifest(
            HimTeacherPaidPilotOfflinePreflightV2Contract.MANIFEST_VERSION,
            entries,
            manifestDigest(entries),
        )
    }

    fun artifactBinding(root: File, path: String, identity: String, logicalDigest: String? = null): HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding {
        require(isRepositoryRelative(path)) { "Artifact path must be repository-relative: $path" }
        val file = root.resolve(path).canonicalFile
        val canonicalRoot = root.canonicalFile
        require(file.path == canonicalRoot.path || file.path.startsWith(canonicalRoot.path + File.separator))
        require(file.isFile && file.canRead()) { "Artifact is unavailable: $path" }
        return HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding(path, file.length(), sha256(file.readBytes()), identity, logicalDigest)
    }

    fun buildArtifact(
        checkpoint: HimTeacherPaidPilotOfflinePreflightV2Checkpoint,
        manifest: HimTeacherPaidPilotOfflinePreflightV2ImplementationManifest,
        canonicalCatalog: HimTeacherPaidPilotOfflinePreflightV2CanonicalBinding,
        persistentArtifacts: List<HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding>,
        retrievalBindings: List<HimTeacherPaidPilotOfflinePreflightV2RetrievalBinding>,
        positiveOfflineE2E: HimTeacherPaidPilotOfflinePreflightV2PositiveE2EBinding,
    ): HimTeacherPaidPilotOfflinePreflightV2Artifact {
        validateBindings(checkpoint, manifest, canonicalCatalog, persistentArtifacts, retrievalBindings, positiveOfflineE2E)
        val unsigned = HimTeacherPaidPilotOfflinePreflightV2Artifact(
            HimTeacherPaidPilotOfflinePreflightV2Contract.VERSION,
            "VALIDATED",
            "PASS",
            "CURRENT",
            checkpoint,
            manifest,
            canonicalCatalog,
            persistentArtifacts.sortedBy { it.path },
            retrievalBindings.sortedBy { it.source },
            positiveOfflineE2E.copy(assertions = positiveOfflineE2E.assertions.sorted()),
            "",
        )
        return unsigned.copy(logicalArtifactDigest = digest(unsigned))
    }

    fun evaluate(artifact: HimTeacherPaidPilotOfflinePreflightV2Artifact, current: HimTeacherPaidPilotOfflinePreflightV2Artifact): HimTeacherPaidPilotOfflinePreflightV2Status {
        if (artifact.contractVersion != HimTeacherPaidPilotOfflinePreflightV2Contract.VERSION ||
            artifact.state != "VALIDATED" || artifact.validationResult != "PASS" || artifact.runtimeEvaluation != "CURRENT" ||
            artifact.logicalArtifactDigest != digest(artifact.copy(logicalArtifactDigest = ""))
        ) return HimTeacherPaidPilotOfflinePreflightV2Status.INVALID
        if (artifact.checkpoint.headSha256 != current.checkpoint.headSha256 || artifact.checkpoint.relevantWorktreeState != current.checkpoint.relevantWorktreeState ||
            artifact.implementationManifest.digest != current.implementationManifest.digest || artifact.implementationManifest != current.implementationManifest
        ) return HimTeacherPaidPilotOfflinePreflightV2Status.STALE_IMPLEMENTATION_CHECKPOINT
        if (artifact.canonicalCatalog != current.canonicalCatalog) return HimTeacherPaidPilotOfflinePreflightV2Status.STALE_CANONICAL_BINDING
        if (artifact.retrievalBindings != current.retrievalBindings) return HimTeacherPaidPilotOfflinePreflightV2Status.STALE_RETRIEVAL_BINDING
        return HimTeacherPaidPilotOfflinePreflightV2Status.CURRENT
    }

    fun serialize(artifact: HimTeacherPaidPilotOfflinePreflightV2Artifact): ByteArray = (gson.toJson(artifact) + "\n").toByteArray(Charsets.UTF_8)

    fun write(file: File, artifact: HimTeacherPaidPilotOfflinePreflightV2Artifact) {
        require(artifact.logicalArtifactDigest == digest(artifact.copy(logicalArtifactDigest = "")))
        val parent = requireNotNull(file.parentFile)
        require(parent.exists() || parent.mkdirs())
        val temporary = Files.createTempFile(parent.toPath(), ".${file.name}.", ".tmp")
        try {
            Files.write(temporary, serialize(artifact))
            Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    fun read(file: File): HimTeacherPaidPilotOfflinePreflightV2Artifact = requireNotNull(gson.fromJson(file.readText(), HimTeacherPaidPilotOfflinePreflightV2Artifact::class.java))

    fun digest(artifact: HimTeacherPaidPilotOfflinePreflightV2Artifact): String = sha256((gson.toJson(artifact) + "\n").toByteArray(Charsets.UTF_8))

    fun manifestDigest(entries: List<HimTeacherPaidPilotOfflinePreflightV2ManifestEntry>): String {
        require(entries.map { it.path } == entries.map { it.path }.distinct().sorted())
        val canonical = entries.joinToString("\n") { "${it.path}|${it.byteSize}|${it.sha256}" } + "\n"
        return sha256(canonical.toByteArray(Charsets.UTF_8))
    }

    private fun validateBindings(
        checkpoint: HimTeacherPaidPilotOfflinePreflightV2Checkpoint,
        manifest: HimTeacherPaidPilotOfflinePreflightV2ImplementationManifest,
        canonicalCatalog: HimTeacherPaidPilotOfflinePreflightV2CanonicalBinding,
        persistentArtifacts: List<HimTeacherPaidPilotOfflinePreflightV2ArtifactBinding>,
        retrievalBindings: List<HimTeacherPaidPilotOfflinePreflightV2RetrievalBinding>,
        positiveOfflineE2E: HimTeacherPaidPilotOfflinePreflightV2PositiveE2EBinding,
    ) {
        require(checkpoint.checkpointContract == HimTeacherPaidPilotOfflinePreflightV2Contract.CHECKPOINT_VERSION)
        require(headPattern.matches(checkpoint.headSha256))
        require(checkpoint.relevantWorktreeState == "CLEAN")
        require(manifest.manifestContract == HimTeacherPaidPilotOfflinePreflightV2Contract.MANIFEST_VERSION)
        require(manifest.entries.isNotEmpty() && manifest.entries.map { it.path } == manifest.entries.map { it.path }.distinct().sorted())
        require(manifest.entries.all { isRepositoryRelative(it.path) && it.byteSize >= 0 && sha256Pattern.matches(it.sha256) })
        require(manifest.digest == manifestDigest(manifest.entries))
        require(canonicalCatalog.contract == HimTeacherPaidPilotOfflinePreflightV2Contract.CANONICAL_CATALOG_BINDING)
        require(canonicalCatalog.artifact.path == HimTeacherPaidPilotOfflinePreflightV2Contract.CATALOG_PATH)
        require(canonicalCatalog.artifact.sha256 == HimTeacherPaidPilotOfflinePreflightV2Contract.CATALOG_SHA256)
        require(canonicalCatalog.releaseVersion.isNotBlank() && canonicalCatalog.foundationIdentity.isNotBlank() && canonicalCatalog.authorityIdentity.isNotBlank() && canonicalCatalog.entityIdRegistryIdentity.isNotBlank())
        require(persistentArtifacts.map { it.path }.distinct().size == persistentArtifacts.size)
        require(persistentArtifacts.map { it.path }.toSet() == HimTeacherPaidPilotOfflinePreflightV2Contract.PERSISTENT_PATHS.toSet())
        persistentArtifacts.forEach { require(isRepositoryRelative(it.path) && it.byteSize > 0 && sha256Pattern.matches(it.sha256) && it.identity.isNotBlank()) }
        require(retrievalBindings.size == 4)
        require(retrievalBindings.map { it.source }.toSet() == HimGroundTruthSource.entries.map { it.name }.toSet())
        retrievalBindings.forEach {
            require(it.index.path.isNotBlank() && isRepositoryRelative(it.index.path) && it.index.byteSize > 0 && sha256Pattern.matches(it.index.sha256))
            require(sha256Pattern.matches(it.frozenIndexSha256) && it.index.sha256 == it.frozenIndexSha256)
            require(it.schemaVersion.isNotBlank() && it.buildPolicyVersion.isNotBlank() && it.projectionPolicyVersion.isNotBlank())
            require(sha256Pattern.matches(it.logicalIndexDigest) && it.evidenceRows > 0 && it.ftsRows == it.evidenceRows)
            require(it.probe.source == it.source && it.probe.returnedHitCount == 1 && it.probe.loadedProjectionCount == 1 && it.probe.validationResult == "PASS")
            require(HimEvidenceRecordReference.parse(HimGroundTruthSource.valueOf(it.source), it.probe.evidenceReference).source.name == it.source)
            require(sha256Pattern.matches(it.probe.projectionDigest))
        }
        require(positiveOfflineE2E.testClass == HimTeacherPaidPilotOfflinePreflightV2Contract.POSITIVE_GATE)
        require(positiveOfflineE2E.result == "PASS" && positiveOfflineE2E.assertions.isNotEmpty() && positiveOfflineE2E.fixtureContractDigest == HimTeacherPaidPilotOfflinePreflightV2Contract.POSITIVE_GATE_DIGEST)
    }

    private fun relative(root: File, file: File): String = root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')
    private fun isRepositoryRelative(path: String): Boolean = path.isNotBlank() && !path.startsWith('/') && !path.contains("..") && !path.contains('\\')
    private fun sha256(file: File): String = sha256(file.readBytes())
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
