package de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimRetrievalFoundationBinding
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimPerSourceMultiQueryEvidencePackingPolicyV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleaseContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleasePersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleaseV1
import java.io.File
import java.security.MessageDigest

object HimFastPaidCandidatePreflightContractV1 {
    const val VERSION = "HIM_FAST_PAID_CANDIDATE_PREFLIGHT_V1"
    const val DIGEST_CONTRACT = "HIM_FAST_PAID_CANDIDATE_PREFLIGHT_DIGEST_V1"
    const val INDEX_IDENTITY_RELEASE_SHA256 = "f8313c710454e928df3956ae9b7f920feb8e9dd61a3d9c65c3459c5340c56aed"
}

enum class HimFastPaidCandidatePreflightStatus {
    READY,
    RETRIEVAL_RELEASE_MISSING,
    RETRIEVAL_RELEASE_INVALID,
    INDEX_IDENTITY_RELEASE_MISSING,
    INDEX_IDENTITY_RELEASE_INVALID,
    OFFLINE_GATE_MISSING,
    OFFLINE_GATE_INVALID,
    OFFLINE_GATE_STALE,
    RUNTIME_IDENTITY_MISMATCH,
    PACKING_IDENTITY_MISMATCH,
    DATASET_IDENTITY_MISMATCH,
    IMPLEMENTATION_FINGERPRINT_MISMATCH,
    INDEX_MISSING,
    INDEX_SIZE_MISMATCH,
    SOURCE_MISSING,
}

data class HimFastPaidCandidatePreflightSnapshotV1(
    val retrievalReleaseBytes: ByteArray?,
    val indexIdentityReleaseBytes: ByteArray?,
    val indexIdentityRelease: HimProductionIndexFileIdentityReleaseV1?,
    val indexSizes: Map<String, Long?>,
    val presentSources: Set<String>,
    val offlineGate: HimOfflineCandidatePublicationGateArtifact?,
    val currentBindings: HimOfflineCandidatePublicationGateBindings,
    val currentImplementationFingerprint: String,
)

data class HimFastPaidCandidatePreflightResultV1(
    val status: HimFastPaidCandidatePreflightStatus,
    val digestContract: String = HimFastPaidCandidatePreflightContractV1.DIGEST_CONTRACT,
    val preflightDigestSha256: String? = null,
) {
    val ready: Boolean get() = status == HimFastPaidCandidatePreflightStatus.READY
}

enum class HimPaidCandidateBoundaryEligibilityV1 {
    PAID_OPT_IN_REQUIRED,
    API_KEY_REQUIRED,
    PROVIDER_ELIGIBLE,
}

/** Technical paid-boundary ordering only; it performs no provider construction or invocation. */
object HimPaidCandidateBoundaryGuardV1 {
    fun evaluate(
        requireFastPreflight: () -> Unit,
        paidOptIn: () -> Boolean,
        apiKeyPresent: () -> Boolean,
    ): HimPaidCandidateBoundaryEligibilityV1 {
        requireFastPreflight()
        if (!paidOptIn()) return HimPaidCandidateBoundaryEligibilityV1.PAID_OPT_IN_REQUIRED
        if (!apiKeyPresent()) return HimPaidCandidateBoundaryEligibilityV1.API_KEY_REQUIRED
        return HimPaidCandidateBoundaryEligibilityV1.PROVIDER_ELIGIBLE
    }
}

object HimFastPaidCandidatePreflightV1 {
    fun evaluate(snapshot: HimFastPaidCandidatePreflightSnapshotV1): HimFastPaidCandidatePreflightResultV1 {
        val retrievalBytes = snapshot.retrievalReleaseBytes
            ?: return blocked(HimFastPaidCandidatePreflightStatus.RETRIEVAL_RELEASE_MISSING)
        if (sha256(retrievalBytes) != HimRetrievalFoundationBinding.RELEASE_SHA256.value) {
            return blocked(HimFastPaidCandidatePreflightStatus.RETRIEVAL_RELEASE_INVALID)
        }

        val identityBytes = snapshot.indexIdentityReleaseBytes
            ?: return blocked(HimFastPaidCandidatePreflightStatus.INDEX_IDENTITY_RELEASE_MISSING)
        val identityRelease = snapshot.indexIdentityRelease
            ?: return blocked(HimFastPaidCandidatePreflightStatus.INDEX_IDENTITY_RELEASE_INVALID)
        if (sha256(identityBytes) != HimFastPaidCandidatePreflightContractV1.INDEX_IDENTITY_RELEASE_SHA256) {
            return blocked(HimFastPaidCandidatePreflightStatus.INDEX_IDENTITY_RELEASE_INVALID)
        }

        val expectedPaths = identityRelease.sources.map { it.indexPath }.toSet()
        if (snapshot.indexSizes.keys != expectedPaths || snapshot.indexSizes.values.any { it == null }) {
            return blocked(HimFastPaidCandidatePreflightStatus.INDEX_MISSING)
        }
        val actualSizes = snapshot.indexSizes.mapValues { requireNotNull(it.value) }
        if (runCatching { HimProductionIndexFileIdentityReleasePersistenceV1.validate(identityRelease, actualSizes) }.isFailure) {
            val expected = HimProductionIndexFileIdentityReleasePersistenceV1.expected()
            return if (identityRelease == expected && identityRelease.sources.any { actualSizes[it.indexPath] != it.sqliteFileBytes }) {
                blocked(HimFastPaidCandidatePreflightStatus.INDEX_SIZE_MISMATCH)
            } else {
                blocked(HimFastPaidCandidatePreflightStatus.INDEX_IDENTITY_RELEASE_INVALID)
            }
        }
        if (!snapshot.presentSources.containsAll(identityRelease.sources.map { it.optimizedSourcePath })) {
            return blocked(HimFastPaidCandidatePreflightStatus.SOURCE_MISSING)
        }

        val gate = snapshot.offlineGate ?: return blocked(HimFastPaidCandidatePreflightStatus.OFFLINE_GATE_MISSING)
        val selfStatus = HimOfflineCandidatePublicationGateV1.evaluate(gate, gate.boundIdentities, gate.implementationFingerprintSha256)
        if (selfStatus != HimPaidCandidatePublicationGateStatus.CURRENT) {
            return blocked(HimFastPaidCandidatePreflightStatus.OFFLINE_GATE_INVALID)
        }
        val expected = snapshot.currentBindings
        if (gate.boundIdentities.inferenceSchema != expected.inferenceSchema ||
            gate.boundIdentities.instructionPolicy != expected.instructionPolicy ||
            gate.boundIdentities.providerFingerprint != expected.providerFingerprint ||
            gate.boundIdentities.retrievalFoundation != expected.retrievalFoundation
        ) return blocked(HimFastPaidCandidatePreflightStatus.RUNTIME_IDENTITY_MISMATCH)
        if (gate.boundIdentities.packingPolicy != expected.packingPolicy) {
            return blocked(HimFastPaidCandidatePreflightStatus.PACKING_IDENTITY_MISMATCH)
        }
        if (gate.boundIdentities.datasetSchema != expected.datasetSchema ||
            gate.boundIdentities.datasetPolicy != expected.datasetPolicy ||
            gate.boundIdentities.generationRunContract != expected.generationRunContract ||
            gate.boundIdentities.inputRunContract != expected.inputRunContract ||
            gate.boundIdentities.datasetDigestContract != expected.datasetDigestContract ||
            gate.boundIdentities.candidateReferenceContract != expected.candidateReferenceContract
        ) return blocked(HimFastPaidCandidatePreflightStatus.DATASET_IDENTITY_MISMATCH)
        if (gate.implementationFingerprintSha256 != snapshot.currentImplementationFingerprint) {
            return blocked(HimFastPaidCandidatePreflightStatus.IMPLEMENTATION_FINGERPRINT_MISMATCH)
        }
        val currentGateStatus = HimOfflineCandidatePublicationGateV1.evaluate(gate, expected, snapshot.currentImplementationFingerprint)
        if (currentGateStatus != HimPaidCandidatePublicationGateStatus.CURRENT) {
            return blocked(HimFastPaidCandidatePreflightStatus.OFFLINE_GATE_STALE)
        }

        return HimFastPaidCandidatePreflightResultV1(
            HimFastPaidCandidatePreflightStatus.READY,
            preflightDigestSha256 = digest(retrievalBytes, identityBytes, identityRelease, gate, expected),
        )
    }

    fun check(root: File): HimFastPaidCandidatePreflightResultV1 {
        val retrievalFile = root.resolve(HimProductionIndexFileIdentityReleaseContractV1.RETRIEVAL_RELEASE_PATH)
        val identityFile = root.resolve(HimProductionIndexFileIdentityReleaseContractV1.PATH)
        val identityBytes = identityFile.takeIf(File::isFile)?.readBytes()
        val identity = identityBytes?.let { runCatching { HimProductionIndexFileIdentityReleasePersistenceV1.read(identityFile) }.getOrNull() }
        val sources = identity?.sources.orEmpty()
        val snapshot = HimFastPaidCandidatePreflightSnapshotV1(
            retrievalFile.takeIf(File::isFile)?.readBytes(),
            identityBytes,
            identity,
            sources.associate { source -> source.indexPath to root.resolve(source.indexPath).takeIf(File::isFile)?.length() },
            sources.mapNotNullTo(mutableSetOf()) { source -> source.optimizedSourcePath.takeIf { root.resolve(it).isFile } },
            HimOfflineCandidatePublicationGateV1.read(root.resolve(HimOfflineCandidatePublicationGateContractV1.ARTIFACT)),
            HimOfflineCandidatePublicationGateBindings.current(),
            HimOfflineCandidatePublicationGateV1.implementationFingerprint(root),
        )
        return evaluate(snapshot)
    }

    fun requireReady(root: File): HimFastPaidCandidatePreflightResultV1 {
        val result = check(root)
        require(result.ready) { "Fast paid Candidate preflight is not ready: ${result.status}" }
        return result
    }

    private fun digest(
        retrievalBytes: ByteArray,
        identityBytes: ByteArray,
        identityRelease: HimProductionIndexFileIdentityReleaseV1,
        gate: HimOfflineCandidatePublicationGateArtifact,
        bindings: HimOfflineCandidatePublicationGateBindings,
    ): String = sha256(buildString {
        appendLine("digest-contract=${HimFastPaidCandidatePreflightContractV1.DIGEST_CONTRACT}")
        appendLine("preflight-contract=${HimFastPaidCandidatePreflightContractV1.VERSION}")
        appendLine("retrieval-release=${HimRetrievalFoundationBinding.RELEASE_VERSION}")
        appendLine("retrieval-release-sha256=${sha256(retrievalBytes)}")
        appendLine("index-identity-release=${identityRelease.releaseVersion}")
        appendLine("index-identity-release-sha256=${sha256(identityBytes)}")
        appendLine("index-identity-release-logical-digest=${identityRelease.logicalDigestSha256}")
        appendLine("offline-gate-logical-digest=${gate.logicalGateDigest}")
        appendLine("offline-gate-implementation-fingerprint=${gate.implementationFingerprintSha256}")
        appendLine("inference-schema=${bindings.inferenceSchema}")
        appendLine("instruction-policy=${bindings.instructionPolicy}")
        appendLine("provider-fingerprint=${bindings.providerFingerprint}")
        appendLine("packing=${bindings.packingPolicy}")
        appendLine("dataset-schema=${bindings.datasetSchema}")
        appendLine("dataset-policy=${bindings.datasetPolicy}")
        appendLine("generation-run=${bindings.generationRunContract}")
        appendLine("input-run=${bindings.inputRunContract}")
        appendLine("dataset-digest=${bindings.datasetDigestContract}")
        appendLine("candidate-reference=${bindings.candidateReferenceContract}")
    }.toByteArray(Charsets.UTF_8))

    private fun blocked(status: HimFastPaidCandidatePreflightStatus) = HimFastPaidCandidatePreflightResultV1(status)
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
