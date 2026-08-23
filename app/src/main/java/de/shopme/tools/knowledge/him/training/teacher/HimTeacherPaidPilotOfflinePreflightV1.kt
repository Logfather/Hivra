package de.shopme.tools.knowledge.him.training.teacher

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimRetrievalFoundationBinding
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticContextBudgetPolicy
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.HimOpenAiSemanticProviderConfiguration
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimPerSourceMultiQueryEvidencePackingPolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionContractV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingContractV1
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object HimTeacherPaidPilotOfflinePreflightContractV1 {
    const val VERSION = "HIM_TEACHER_PAID_PILOT_OFFLINE_PREFLIGHT_V1"
    const val IMPLEMENTATION_FINGERPRINT_CONTRACT = "HIM_TEACHER_PAID_PILOT_IMPLEMENTATION_FINGERPRINT_V1"
    const val ARTIFACT = "build/knowledge/reports/him/training/him-teacher-paid-pilot-offline-preflight.v1.json"
    const val OUTPUT_VALIDATOR_CONTRACT = "HIM_TEACHER_GROUND_TRUTH_OUTPUT_VALIDATOR_V1"
    val IMPLEMENTATION_FILES = listOf(
        "app/src/main/java/de/shopme/tools/knowledge/him/training/teacher/HimTeacherGroundTruthGenerationContractV1.kt",
        "app/src/main/java/de/shopme/tools/knowledge/him/training/teacher/HimTeacherGroundTruthGenerationPipelineV1.kt",
        "app/src/main/java/de/shopme/tools/knowledge/him/training/teacher/HimTeacherGroundTruthGenerationPersistenceV1.kt",
    )
}

data class HimTeacherPaidPilotOfflinePreflightBindingsV1(
    val workItemContract: String,
    val teacherContract: String,
    val inferenceSchema: String,
    val instructionPolicy: String,
    val providerFingerprint: String,
    val contextBudgetPolicy: String,
    val retrievalFoundation: String,
    val retrievalFoundationDigest: String,
    val evidencePackingPolicy: String,
    val partitionContract: String,
    val partitionPolicy: String,
    val outputValidatorContract: String,
) {
    companion object {
        fun current() = HimTeacherPaidPilotOfflinePreflightBindingsV1(
            workItemContract = HimCanonicalGroundTruthScalingContractV1.WORK_ITEM_REFERENCE_CONTRACT,
            teacherContract = HimTeacherGroundTruthGenerationContractV1.VERSION,
            inferenceSchema = HimSemanticInferenceSchema.OUTPUT_VERSION,
            instructionPolicy = HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
            providerFingerprint = HimOpenAiSemanticProviderConfiguration().fingerprint().value,
            contextBudgetPolicy = HimSemanticContextBudgetPolicy.VERSION,
            retrievalFoundation = HimRetrievalFoundationBinding.RELEASE_VERSION,
            retrievalFoundationDigest = HimRetrievalFoundationBinding.FOUNDATION_DIGEST.value,
            evidencePackingPolicy = HimPerSourceMultiQueryEvidencePackingPolicyV1.VERSION,
            partitionContract = HimTrainingPartitionContractV1.VERSION,
            partitionPolicy = HimTrainingPartitionContractV1.POLICY_VERSION,
            outputValidatorContract = HimTeacherPaidPilotOfflinePreflightContractV1.OUTPUT_VALIDATOR_CONTRACT,
        )
    }
}

data class HimTeacherPaidPilotOfflinePreflightArtifactV1(
    val contractVersion: String,
    val state: String,
    val boundIdentities: HimTeacherPaidPilotOfflinePreflightBindingsV1,
    val implementationFingerprintContract: String,
    val implementationFingerprintSha256: String,
    val offlineTestLogicalDigest: String,
    val validationResult: String,
    val logicalArtifactDigest: String,
)

enum class HimTeacherPaidPilotOfflinePreflightStatusV1 {
    CURRENT,
    ABSENT,
    INVALID,
    STALE,
    CONTRACT_MISMATCH,
}

object HimTeacherPaidPilotOfflinePreflightV1 {
    private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    private val sha256Pattern = Regex("[0-9a-f]{64}")

    fun implementationFingerprint(root: File): String {
        val canonical = buildString {
            appendLine("contract=${HimTeacherPaidPilotOfflinePreflightContractV1.IMPLEMENTATION_FINGERPRINT_CONTRACT}")
            HimTeacherPaidPilotOfflinePreflightContractV1.IMPLEMENTATION_FILES.forEach { path ->
                appendLine("file=$path sha256=${sha256(root.resolve(path).readBytes())}")
            }
        }
        return sha256(canonical.toByteArray(Charsets.UTF_8))
    }

    fun createValidated(
        root: File,
        preflight: HimTeacherGroundTruthOfflinePreflightResultV1,
        bindings: HimTeacherPaidPilotOfflinePreflightBindingsV1 = HimTeacherPaidPilotOfflinePreflightBindingsV1.current(),
    ): HimTeacherPaidPilotOfflinePreflightArtifactV1 {
        require(preflight.ready) { "Cannot bind an unsuccessful Teacher offline preflight" }
        val requestDigest = requireNotNull(preflight.requestBytesSha256)
        val resultReference = requireNotNull(preflight.resultReference)
        val offlineDigest = sha256(buildString {
            appendLine("contract=${HimTeacherPaidPilotOfflinePreflightContractV1.VERSION}")
            appendLine("request-bytes-sha256=${requestDigest.value}")
            appendLine("result-reference=$resultReference")
            appendLine("diagnostics=${preflight.diagnostics.joinToString("|")}")
        }.toByteArray(Charsets.UTF_8))
        val unsigned = HimTeacherPaidPilotOfflinePreflightArtifactV1(
            contractVersion = HimTeacherPaidPilotOfflinePreflightContractV1.VERSION,
            state = "VALIDATED",
            boundIdentities = bindings,
            implementationFingerprintContract = HimTeacherPaidPilotOfflinePreflightContractV1.IMPLEMENTATION_FINGERPRINT_CONTRACT,
            implementationFingerprintSha256 = implementationFingerprint(root),
            offlineTestLogicalDigest = offlineDigest,
            validationResult = "PASS",
            logicalArtifactDigest = "",
        )
        return unsigned.copy(logicalArtifactDigest = digest(unsigned))
    }

    fun serialize(artifact: HimTeacherPaidPilotOfflinePreflightArtifactV1): ByteArray =
        (gson.toJson(artifact) + "\n").toByteArray(Charsets.UTF_8)

    fun write(file: File, artifact: HimTeacherPaidPilotOfflinePreflightArtifactV1) {
        require(evaluate(artifact, artifact.boundIdentities, artifact.implementationFingerprintSha256) == HimTeacherPaidPilotOfflinePreflightStatusV1.CURRENT)
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

    fun read(file: File): HimTeacherPaidPilotOfflinePreflightArtifactV1? =
        if (!file.isFile) null else runCatching { gson.fromJson(file.readText(), HimTeacherPaidPilotOfflinePreflightArtifactV1::class.java) }.getOrNull()

    fun evaluate(
        artifact: HimTeacherPaidPilotOfflinePreflightArtifactV1?,
        currentBindings: HimTeacherPaidPilotOfflinePreflightBindingsV1,
        currentImplementationFingerprint: String,
    ): HimTeacherPaidPilotOfflinePreflightStatusV1 {
        if (artifact == null) return HimTeacherPaidPilotOfflinePreflightStatusV1.ABSENT
        if (artifact.contractVersion != HimTeacherPaidPilotOfflinePreflightContractV1.VERSION ||
            artifact.state != "VALIDATED" ||
            artifact.validationResult != "PASS" ||
            artifact.implementationFingerprintContract != HimTeacherPaidPilotOfflinePreflightContractV1.IMPLEMENTATION_FINGERPRINT_CONTRACT ||
            !sha256Pattern.matches(artifact.implementationFingerprintSha256) ||
            !sha256Pattern.matches(artifact.offlineTestLogicalDigest) ||
            artifact.logicalArtifactDigest != digest(artifact.copy(logicalArtifactDigest = ""))
        ) return HimTeacherPaidPilotOfflinePreflightStatusV1.INVALID
        if (artifact.boundIdentities != currentBindings) return HimTeacherPaidPilotOfflinePreflightStatusV1.CONTRACT_MISMATCH
        if (artifact.implementationFingerprintSha256 != currentImplementationFingerprint) return HimTeacherPaidPilotOfflinePreflightStatusV1.STALE
        return HimTeacherPaidPilotOfflinePreflightStatusV1.CURRENT
    }

    fun validateCurrent(root: File): HimTeacherPaidPilotOfflinePreflightStatusV1 = evaluate(
        read(root.resolve(HimTeacherPaidPilotOfflinePreflightContractV1.ARTIFACT)),
        HimTeacherPaidPilotOfflinePreflightBindingsV1.current(),
        implementationFingerprint(root),
    )

    fun requireCurrent(root: File) {
        val status = validateCurrent(root)
        require(status == HimTeacherPaidPilotOfflinePreflightStatusV1.CURRENT) {
            "Teacher paid pilot offline preflight is not current: $status"
        }
    }

    private fun digest(artifact: HimTeacherPaidPilotOfflinePreflightArtifactV1): String =
        sha256((gson.toJson(artifact) + "\n").toByteArray(Charsets.UTF_8))

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

/**
 * Future F3.8g.2 entry points can wrap the opt-in provider with this guard.
 * The preflight is checked before the delegate can inspect opt-in or secrets.
 */
class HimTeacherPaidPilotOfflinePreflightGuardedProviderV1(
    private val preflightCheck: () -> Unit,
    private val delegate: HimTeacherGroundTruthProviderV1,
) : HimTeacherGroundTruthProviderV1 {
    constructor(projectRoot: File, delegate: HimTeacherGroundTruthProviderV1) : this(
        preflightCheck = { HimTeacherPaidPilotOfflinePreflightV1.requireCurrent(projectRoot) },
        delegate = delegate,
    )

    override fun invoke(request: HimTeacherGroundTruthGenerationRequestV1): HimTeacherGroundTruthProviderOutcomeV1 {
        preflightCheck()
        return delegate.invoke(request)
    }
}
