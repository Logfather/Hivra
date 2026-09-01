package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionAssignmentV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionManifestIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionManifestV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolutionV1
import java.security.MessageDigest

/** Technology-agnostic boundary between an authorized mission and a future trainer. */
fun interface HimTrainerPortV1 {
    fun execute(request: Request): Result

    companion object {
        const val CONTRACT_ID = "HIM_TRAINER_PORT_V1"
        const val VERSION = "1"
        const val STATE = "TRAINER_EXECUTION_BOUNDARY_DEFINED"
        const val TRAINER_PORT_MUST_REQUIRE_TRAINING_MISSION = "YES"
        const val TRAINER_REQUEST_PARTITION = "TRAIN_ONLY"
        const val VALIDATION_INPUTS_IN_TRAINER_REQUEST = 0
        const val HOLDOUT_INPUTS_IN_TRAINER_REQUEST = 0
    }

    class Request private constructor(
        val mission: HimTrainingMissionV1.Mission,
        val configuration: HimTrainingConfigurationV1,
        val modelBinding: HimModelBindingV1,
        val trainAssignments: List<HimTrainingPartitionAssignmentV1>,
        val logicalDigest: HimSha256,
        val requestReference: String,
    ) {
        init {
            require(trainAssignments.isNotEmpty()) { "TRAIN_INPUT_EMPTY" }
            require(trainAssignments.all { it.partition == HimTrainingPartitionV1.TRAIN }) {
                "TRAINER_REQUEST_PARTITION_MISMATCH"
            }
            require(requestReference == "trainer-request:v1:${logicalDigest.value}") {
                "TRAINER_REQUEST_REFERENCE_MISMATCH"
            }
        }

        companion object {
            fun create(
                mission: HimTrainingMissionV1.Mission,
                configuration: HimTrainingConfigurationV1,
                modelBinding: HimModelBindingV1,
                partitionManifest: HimTrainingPartitionManifestV1,
            ): Request {
                require(configuration.logicalDigest == mission.trainingConfiguration.digest) {
                    "MISSION_CONFIGURATION_BINDING_MISMATCH"
                }
                require(modelBinding.logicalDigest == mission.modelBinding.digest) {
                    "MISSION_MODEL_BINDING_MISMATCH"
                }
                require(partitionManifest.policyVersion == HimTrainingPartitionContractV1.POLICY_VERSION) {
                    "PARTITION_POLICY_MISMATCH"
                }
                require(
                    partitionManifest.logicalDigest ==
                        mission.readinessBinding.partitionManifestLogicalDigest,
                ) {
                    "MISSION_PARTITION_BINDING_MISMATCH"
                }
                require(
                    partitionManifest.logicalDigest ==
                        HimTrainingPartitionManifestIdentityV1.digest(
                            partitionManifest.policyVersion,
                            partitionManifest.assignments,
                        ),
                ) {
                    "PARTITION_MANIFEST_DIGEST_MISMATCH"
                }
                require(
                    HimZeroCandidateRecoveryHumanReviewP1TrainingCorpusPartitionV1.Counters.from(
                        partitionManifest.assignments,
                    ) == mission.readinessBinding.partitionCounters,
                ) {
                    "MISSION_PARTITION_COUNTERS_MISMATCH"
                }
                partitionManifest.assignments.forEach { assignment ->
                    val resolved = when (val record = assignment.record) {
                        is HimTrainingPartitionRecordV1.Positive ->
                            de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolverV1.resolve(
                                record.positiveExample,
                            )
                        is HimTrainingPartitionRecordV1.Negative ->
                            de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolverV1.resolve(
                                record.negativeExample,
                            )
                    }
                    require(
                        resolved is HimTrainingFamilyGroupResolutionV1.Resolved &&
                            resolved.groupReference == assignment.groupReference,
                    ) {
                        "PARTITION_GROUP_BINDING_MISMATCH"
                    }
                }
                val trainAssignments = partitionManifest.assignments
                    .filter { it.partition == HimTrainingPartitionV1.TRAIN }
                    .toList()
                require(trainAssignments.isNotEmpty()) { "TRAIN_INPUT_EMPTY" }
                val logicalDigest = digest(mission, configuration, modelBinding, partitionManifest, trainAssignments)
                return Request(
                    mission = mission,
                    configuration = configuration,
                    modelBinding = modelBinding,
                    trainAssignments = trainAssignments,
                    logicalDigest = logicalDigest,
                    requestReference = "trainer-request:v1:${logicalDigest.value}",
                )
            }

            private fun digest(
                mission: HimTrainingMissionV1.Mission,
                configuration: HimTrainingConfigurationV1,
                modelBinding: HimModelBindingV1,
                partitionManifest: HimTrainingPartitionManifestV1,
                trainAssignments: List<HimTrainingPartitionAssignmentV1>,
            ): HimSha256 {
                val canonical = buildString {
                    field("contract", CONTRACT_ID)
                    field("version", VERSION)
                    field("state", STATE)
                    field("mission", mission.logicalDigest.value)
                    field("configuration", configuration.logicalDigest.value)
                    field("model-binding", modelBinding.logicalDigest.value)
                    field("implementation", mission.implementationBinding.fingerprint.value)
                    field("partition-manifest", partitionManifest.logicalDigest.value)
                    trainAssignments.forEachIndexed { index, assignment ->
                        field("train-$index-group", assignment.groupReference.value)
                        field("train-$index-partition", assignment.partition.name)
                        field("train-$index-kind", recordKind(assignment.record))
                        field("train-$index-record", assignment.record.recordReference)
                        field(
                            "train-$index-positive",
                            assignment.record.positiveExample.exampleReference.value,
                        )
                    }
                }
                return HimSha256(
                    MessageDigest.getInstance("SHA-256")
                        .digest(canonical.toByteArray(Charsets.UTF_8))
                        .joinToString("") { "%02x".format(it.toInt() and 0xff) },
                )
            }

            private fun recordKind(record: HimTrainingPartitionRecordV1): String =
                when (record) {
                    is HimTrainingPartitionRecordV1.Positive -> "POSITIVE"
                    is HimTrainingPartitionRecordV1.Negative -> "NEGATIVE"
                }

            private fun StringBuilder.field(key: String, value: String) {
                append(key).append('=').append(value.length).append(':').append(value).append('\n')
            }
        }
    }

    sealed interface Result {
        data class Completed(
            val requestDigest: HimSha256,
        ) : Result

        data class Failed(
            val reason: FailureReasonV1,
            val safeContext: String,
        ) : Result {
            init {
                require(safeContext.isNotBlank())
                require(safeContext.none { it.isISOControl() })
            }
        }
    }

    enum class FailureReasonV1 {
        TRAINER_EXECUTION_FAILED,
    }
}
