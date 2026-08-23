package de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimRetrievalFoundationBinding
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimPerSourceMultiQueryEvidencePackingPolicyV1
import java.io.File
import java.security.MessageDigest

object HimOfflineCandidatePublicationGateContractV1 {
    const val VERSION = "HIM_FULL_OFFLINE_CANDIDATE_PUBLICATION_GATE_V1"
    const val IMPLEMENTATION_FINGERPRINT_CONTRACT = "HIM_PAID_CANDIDATE_PUBLICATION_IMPLEMENTATION_FINGERPRINT_V1"
    const val ARTIFACT = "build/knowledge/reports/him/candidates/him-paid-candidate-publication-gate.v1.json"
    val IMPLEMENTATION_FILES = listOf(
        "app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/candidate/dataset/HimCandidateDatasetContracts.kt",
        "app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/candidate/dataset/HimCandidateDatasetPersistenceV2.kt",
        "app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/candidate/dataset/HimOfflineCandidatePublicationGate.kt",
        "app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/inference/HimSemanticContextBudget.kt",
        "app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/inference/HimSemanticInferenceContracts.kt",
        "app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/retrieval/HimPerSourceMultiQueryEvidencePacker.kt",
        "app/src/test/java/de/shopme/testing/system/tools/knowledge/him/canonical/family/groundtruth/candidate/RunHimFirstRealCandidateGenerationPilotTest.kt",
        "app/src/test/java/de/shopme/testing/system/tools/knowledge/him/canonical/family/groundtruth/candidate/dataset/RunHimFullOfflineCandidatePublicationGateTest.kt",
    ).sorted()
}

data class HimOfflineCandidatePublicationGateBindings(
    val inferenceSchema: String,
    val instructionPolicy: String,
    val providerFingerprint: String,
    val retrievalFoundation: String,
    val packingPolicy: String,
    val datasetSchema: String,
    val datasetPolicy: String,
    val generationRunContract: String,
    val inputRunContract: String,
    val datasetDigestContract: String,
    val candidateReferenceContract: String,
) {
    companion object {
        fun current() = HimOfflineCandidatePublicationGateBindings(
            HimSemanticInferenceSchema.OUTPUT_VERSION,
            HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
            HimCandidateDatasetContractV2.PROVIDER_CONFIGURATION_FINGERPRINT.value,
            HimRetrievalFoundationBinding.RELEASE_VERSION,
            HimPerSourceMultiQueryEvidencePackingPolicyV1.VERSION,
            HimCandidateDatasetContractV2.SCHEMA_VERSION,
            HimCandidateDatasetContractV2.POLICY_VERSION,
            HimCandidateDatasetContractV2.RUN_CONTRACT,
            HimCandidateDatasetContractV2.INPUT_RUN_CONTRACT,
            HimCandidateDatasetContractV2.LOGICAL_DIGEST_CONTRACT,
            HimCandidateDatasetContractV2.CANDIDATE_REFERENCE_CONTRACT,
        )
    }
}

data class HimOfflineCandidatePublicationGateArtifact(
    val contractVersion: String,
    val state: String,
    val boundIdentities: HimOfflineCandidatePublicationGateBindings,
    val implementationFingerprintContract: String,
    val implementationFingerprintSha256: String,
    val offlineTestLogicalDigest: String,
    val validationResult: String,
    val logicalGateDigest: String,
)

enum class HimPaidCandidatePublicationGateStatus {
    CURRENT,
    ABSENT,
    INVALID,
    PAID_CANDIDATE_PUBLICATION_GATE_STALE,
}

object HimOfflineCandidatePublicationGateV1 {
    private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()

    fun implementationFingerprint(root: File): String {
        val canonical = buildString {
            appendLine("contract=${HimOfflineCandidatePublicationGateContractV1.IMPLEMENTATION_FINGERPRINT_CONTRACT}")
            HimOfflineCandidatePublicationGateContractV1.IMPLEMENTATION_FILES.forEach { path ->
                appendLine("file=$path sha256=${sha256(root.resolve(path).readBytes())}")
            }
        }
        return sha256(canonical.toByteArray(Charsets.UTF_8))
    }

    fun createValidated(root: File, offlineTestLogicalDigest: String): HimOfflineCandidatePublicationGateArtifact {
        require(SHA256.matches(offlineTestLogicalDigest))
        val unsigned = HimOfflineCandidatePublicationGateArtifact(
            HimOfflineCandidatePublicationGateContractV1.VERSION,
            "VALIDATED",
            HimOfflineCandidatePublicationGateBindings.current(),
            HimOfflineCandidatePublicationGateContractV1.IMPLEMENTATION_FINGERPRINT_CONTRACT,
            implementationFingerprint(root),
            offlineTestLogicalDigest,
            "PASS",
            "",
        )
        return unsigned.copy(logicalGateDigest = digest(unsigned))
    }

    fun write(file: File, artifact: HimOfflineCandidatePublicationGateArtifact) {
        require(evaluate(artifact, artifact.boundIdentities, artifact.implementationFingerprintSha256) == HimPaidCandidatePublicationGateStatus.CURRENT)
        requireNotNull(file.parentFile).mkdirs()
        file.writeText(gson.toJson(artifact) + "\n")
    }

    fun read(file: File): HimOfflineCandidatePublicationGateArtifact? =
        if (!file.isFile) null else runCatching { gson.fromJson(file.readText(), HimOfflineCandidatePublicationGateArtifact::class.java) }.getOrNull()

    fun validateCurrent(root: File): HimPaidCandidatePublicationGateStatus =
        evaluate(read(root.resolve(HimOfflineCandidatePublicationGateContractV1.ARTIFACT)), HimOfflineCandidatePublicationGateBindings.current(), implementationFingerprint(root))

    fun evaluate(
        artifact: HimOfflineCandidatePublicationGateArtifact?,
        currentBindings: HimOfflineCandidatePublicationGateBindings,
        currentImplementationFingerprint: String,
    ): HimPaidCandidatePublicationGateStatus {
        if (artifact == null) return HimPaidCandidatePublicationGateStatus.ABSENT
        if (artifact.contractVersion != HimOfflineCandidatePublicationGateContractV1.VERSION ||
            artifact.state != "VALIDATED" || artifact.validationResult != "PASS" ||
            artifact.implementationFingerprintContract != HimOfflineCandidatePublicationGateContractV1.IMPLEMENTATION_FINGERPRINT_CONTRACT ||
            !SHA256.matches(artifact.implementationFingerprintSha256) || !SHA256.matches(artifact.offlineTestLogicalDigest) ||
            artifact.logicalGateDigest != digest(artifact.copy(logicalGateDigest = ""))
        ) return HimPaidCandidatePublicationGateStatus.INVALID
        if (artifact.boundIdentities != currentBindings || artifact.implementationFingerprintSha256 != currentImplementationFingerprint) {
            return HimPaidCandidatePublicationGateStatus.PAID_CANDIDATE_PUBLICATION_GATE_STALE
        }
        return HimPaidCandidatePublicationGateStatus.CURRENT
    }

    fun requireCurrent(root: File) {
        val status = validateCurrent(root)
        require(status == HimPaidCandidatePublicationGateStatus.CURRENT) { "Paid Candidate publication gate is not current: $status" }
    }

    private fun digest(value: HimOfflineCandidatePublicationGateArtifact): String = sha256((gson.toJson(value) + "\n").toByteArray(Charsets.UTF_8))
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private val SHA256 = Regex("[0-9a-f]{64}")
}
