package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionLeakageDiagnosticV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionValidationResultV1
import java.security.MessageDigest

/**
 * The first Training-V1 authorization boundary. It creates no trainer, model,
 * checkpoint, persistence, or external execution side effect.
 */
object HimTrainingMissionV1 {
    const val CONTRACT_ID = "HIM_TRAINING_MISSION_V1"
    const val VERSION = "1"
    const val STATE = "TRAINING_MISSION_AUTHORIZED"
    const val TRAINING_MISSION_MUST_REQUIRE_READY = "YES"

    private const val MISSION_REFERENCE_PREFIX = "training-mission:v1:"
    private val ZERO_DIGEST = "0".repeat(64)

    data class TrainingConfigurationReference(
        val digest: HimSha256,
    ) {
        init {
            require(digest.value != HimTrainingMissionV1.ZERO_DIGEST) { "Training configuration binding must be concrete." }
        }
    }

    data class ModelBindingReference(
        val digest: HimSha256,
    ) {
        init {
            require(digest.value != HimTrainingMissionV1.ZERO_DIGEST) { "Model binding must be concrete." }
        }
    }

    data class ImplementationBindingReference(
        val fingerprint: HimSha256,
    ) {
        init {
            require(fingerprint.value != HimTrainingMissionV1.ZERO_DIGEST) { "Implementation binding must be concrete." }
        }
    }

    data class Request(
        val readiness: HimZeroCandidateRecoveryHumanReviewP1TrainingReadinessV1.Result.Ready,
        val trainingConfiguration: TrainingConfigurationReference,
        val modelBinding: ModelBindingReference,
        val implementationBinding: ImplementationBindingReference,
    )

    data class ReadinessBinding(
        val snapshotBinding:
            HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionSourceBindingV1.SnapshotBindingV1,
        val partitionManifestLogicalDigest: HimSha256,
        val partitionCounters:
            de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters,
        val partitionPolicyVersion: String,
        val leakageValidationValid: Boolean,
        val leakageValidationDigest: HimSha256,
    ) {
        init {
            require(partitionPolicyVersion == HimTrainingPartitionContractV1.POLICY_VERSION)
            require(partitionCounters.total > 0)
            require(partitionCounters.train > 0)
            require(snapshotBinding.corpusLogicalDigest.value != HimTrainingMissionV1.ZERO_DIGEST)
            require(partitionManifestLogicalDigest.value != HimTrainingMissionV1.ZERO_DIGEST)
            require(leakageValidationDigest.value != HimTrainingMissionV1.ZERO_DIGEST)
        }
    }

    /** Publicly consumable mission proof; no public constructor or alternate factory exists. */
    sealed interface Mission {
        val contractId: String
        val version: String
        val state: String
        val missionReference: String
        val logicalDigest: HimSha256
        val readinessBinding: ReadinessBinding
        val trainingConfiguration: TrainingConfigurationReference
        val modelBinding: ModelBindingReference
        val implementationBinding: ImplementationBindingReference
    }

    private data class CreatedMission(
        override val contractId: String,
        override val version: String,
        override val state: String,
        override val missionReference: String,
        override val logicalDigest: HimSha256,
        override val readinessBinding: ReadinessBinding,
        override val trainingConfiguration: TrainingConfigurationReference,
        override val modelBinding: ModelBindingReference,
        override val implementationBinding: ImplementationBindingReference,
    ) : Mission

    fun create(request: Request): Mission {
        val ready = request.readiness
        val validation = ready.leakageValidationResult
        require(validation.valid && validation.diagnostics.none { it.fatal }) {
            "Training readiness must contain a valid leakage authorization."
        }
        require(ready.partitionPolicyVersion == HimTrainingPartitionContractV1.POLICY_VERSION) {
            "Training readiness policy binding is invalid."
        }
        require(ready.partitionCounters.total > 0 && ready.partitionCounters.train > 0) {
            "Training readiness has no usable TRAIN partition."
        }

        val readinessBinding = ReadinessBinding(
            snapshotBinding = ready.snapshotBinding,
            partitionManifestLogicalDigest = ready.partitionManifestLogicalDigest,
            partitionCounters = ready.partitionCounters,
            partitionPolicyVersion = ready.partitionPolicyVersion,
            leakageValidationValid = validation.valid,
            leakageValidationDigest = leakageValidationDigest(validation),
        )
        val logicalDigest = missionDigest(
            readinessBinding = readinessBinding,
            trainingConfiguration = request.trainingConfiguration,
            modelBinding = request.modelBinding,
            implementationBinding = request.implementationBinding,
        )
        return CreatedMission(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            missionReference = "$MISSION_REFERENCE_PREFIX${logicalDigest.value}",
            logicalDigest = logicalDigest,
            readinessBinding = readinessBinding,
            trainingConfiguration = request.trainingConfiguration,
            modelBinding = request.modelBinding,
            implementationBinding = request.implementationBinding,
        )
    }

    private fun missionDigest(
        readinessBinding: ReadinessBinding,
        trainingConfiguration: TrainingConfigurationReference,
        modelBinding: ModelBindingReference,
        implementationBinding: ImplementationBindingReference,
    ): HimSha256 {
        val canonical = buildString {
            field("contract", CONTRACT_ID)
            field("version", VERSION)
            field("state", STATE)
            field("snapshot-id", readinessBinding.snapshotBinding.snapshotId)
            field("corpus-logical-digest", readinessBinding.snapshotBinding.corpusLogicalDigest.value)
            field("partition-manifest-logical-digest", readinessBinding.partitionManifestLogicalDigest.value)
            field("partition-total", readinessBinding.partitionCounters.total.toString())
            field("partition-train", readinessBinding.partitionCounters.train.toString())
            field("partition-validation", readinessBinding.partitionCounters.validation.toString())
            field("partition-holdout", readinessBinding.partitionCounters.holdout.toString())
            field("partition-policy", readinessBinding.partitionPolicyVersion)
            field("leakage-valid", readinessBinding.leakageValidationValid.toString())
            field("leakage-digest", readinessBinding.leakageValidationDigest.value)
            field("training-configuration", trainingConfiguration.digest.value)
            field("model-binding", modelBinding.digest.value)
            field("implementation-binding", implementationBinding.fingerprint.value)
        }
        return HimSha256(sha256(canonical))
    }

    private fun leakageValidationDigest(validation: HimTrainingPartitionValidationResultV1): HimSha256 {
        val canonical = buildString {
            field("contract", "HIM_TRAINING_PARTITION_VALIDATION_RESULT_V1")
            field("valid", validation.valid.toString())
            validation.diagnostics
                .sortedWith(
                    compareBy<HimTrainingPartitionLeakageDiagnosticV1>(
                        { it.level.name },
                        { it.fatal },
                        { it.key },
                        { it.recordReferences.joinToString("\u0000") },
                        { it.message },
                    ),
                )
                .forEachIndexed { index, diagnostic ->
                    field("diagnostic-$index-level", diagnostic.level.name)
                    field("diagnostic-$index-fatal", diagnostic.fatal.toString())
                    field("diagnostic-$index-key", diagnostic.key)
                    field("diagnostic-$index-records", diagnostic.recordReferences.sorted().joinToString("\u0000"))
                    field("diagnostic-$index-message", diagnostic.message)
                }
        }
        return HimSha256(sha256(canonical))
    }

    private fun StringBuilder.field(key: String, value: String) {
        append(key).append('=').append(value.length).append(':').append(value).append('\n')
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
