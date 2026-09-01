package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.encoding.HimTrainingTargetEncodingV1
import de.shopme.tools.knowledge.him.training.objective.HimTrainingObjectiveV1
import java.math.BigDecimal
import java.security.MessageDigest

/**
 * Technology-neutral wire-domain boundary for a future external trainer.
 * It accepts only an already authorized [HimTrainerPortV1.Request] and never
 * performs process, model, token, tensor, persistence, or training work.
 */
class HimTrainerProtocolV1 private constructor(
    val contractId: String,
    val version: String,
    val state: String,
    val objectiveDigest: HimSha256,
    val objectiveReference: String,
    val targetEncodingDigest: HimSha256,
    val targetEncodingReference: String,
    val logicalDigest: HimSha256,
    val protocolReference: String,
    private val objective: HimTrainingObjectiveV1,
    private val targetEncoding: HimTrainingTargetEncodingV1,
) {
    init {
        require(contractId == CONTRACT_ID)
        require(version == VERSION)
        require(state == STATE)
        require(objectiveReference == "training-objective:v1:${objectiveDigest.value}")
        require(targetEncodingReference == "training-target-encoding:v1:${targetEncodingDigest.value}")
        require(logicalDigest == protocolDigest(objectiveDigest, targetEncodingDigest))
        require(protocolReference == "$PROTOCOL_REFERENCE_PREFIX${logicalDigest.value}")
        require(targetEncoding.objectiveDigest == objectiveDigest)
        require(targetEncoding.objectiveReference == objectiveReference)
    }

    /** The only protocol request factory: it requires the authorized Trainer Port request. */
    fun createRequest(portRequest: HimTrainerPortV1.Request): Request {
        require(portRequest.trainAssignments.isNotEmpty()) { "TRAIN_PROTOCOL_INPUT_EMPTY" }
        require(portRequest.trainAssignments.all { it.partition == HimTrainingPartitionV1.TRAIN }) {
            "TRAIN_PROTOCOL_PARTITION_MISMATCH"
        }
        require(portRequest.requestReference == "trainer-request:v1:${portRequest.logicalDigest.value}") {
            "TRAINER_PORT_REQUEST_REFERENCE_MISMATCH"
        }
        require(portRequest.configuration.logicalDigest == portRequest.mission.trainingConfiguration.digest) {
            "MISSION_CONFIGURATION_BINDING_MISMATCH"
        }
        require(portRequest.modelBinding.logicalDigest == portRequest.mission.modelBinding.digest) {
            "MISSION_MODEL_BINDING_MISMATCH"
        }

        val records = portRequest.trainAssignments.mapIndexed { index, assignment ->
            protocolRecord(index, assignment.record, assignment.groupReference.value)
        }
        require(records.size == portRequest.trainAssignments.size) { "TRAIN_PROTOCOL_COVERAGE_MISMATCH" }
        require(records.map { it.assignmentIndex } == records.indices.toList()) {
            "TRAIN_PROTOCOL_ORDER_MISMATCH"
        }
        val configuration = Configuration.from(portRequest.configuration)
        val modelBinding = ModelBinding.from(portRequest.modelBinding)
        val requestDigest = requestDigest(
            trainerPortRequest = portRequest,
            configuration = configuration,
            modelBinding = modelBinding,
            records = records,
        )
        return Request.create(
            protocolReference = protocolReference,
            trainerPortRequestDigest = portRequest.logicalDigest,
            trainerPortRequestReference = portRequest.requestReference,
            trainingMissionDigest = portRequest.mission.logicalDigest,
            trainingMissionReference = portRequest.mission.missionReference,
            configuration = configuration,
            modelBinding = modelBinding,
            objectiveDigest = objectiveDigest,
            objectiveReference = objectiveReference,
            targetEncodingDigest = targetEncodingDigest,
            targetEncodingReference = targetEncodingReference,
            implementationFingerprint = portRequest.mission.implementationBinding.fingerprint,
            records = records,
            logicalDigest = requestDigest,
            requestReference = "$REQUEST_REFERENCE_PREFIX${requestDigest.value}",
        )
    }

    private fun protocolRecord(
        index: Int,
        record: HimTrainingPartitionRecordV1,
        groupReference: String,
    ): TrainRecord {
        val positiveExample = record.positiveExample
        val input = Input(
            observedTerm = positiveExample.input.observedTerm,
            normalizedObservedTerm = positiveExample.input.normalizedObservedTerm,
            canonicalContext = positiveExample.input.canonicalContext.map { context ->
                CanonicalContext(
                    rank = context.rank,
                    canonicalId = context.canonicalId.value,
                    canonicalName = context.canonicalName,
                    fullRecordCanonicalJson = context.fullRecordCanonicalJson,
                )
            },
            evidence = positiveExample.input.evidence.map { evidence ->
                Evidence(
                    source = evidence.reference.source,
                    sourceArtifactSha256 = evidence.reference.sourceArtifactSha256,
                    sourceRecordIdentity = evidence.reference.sourceRecordIdentity,
                    recordKind = evidence.recordKind,
                    retrievalRank = evidence.retrievalRank,
                )
            },
        )
        val (polarity, encodedTarget) = when (record) {
            is HimTrainingPartitionRecordV1.Positive ->
                PolarityV1.POSITIVE to targetEncoding.encodePositive(
                    objective.projectPositive(record.positiveExample),
                )
            is HimTrainingPartitionRecordV1.Negative ->
                PolarityV1.NEGATIVE to targetEncoding.encodeNegative(
                    objective.projectNegative(record.negativeExample),
                )
        }
        require(encodedTarget.objectiveDigest == objectiveDigest) { "OBJECTIVE_TARGET_BINDING_MISMATCH" }
        require(encodedTarget.encodingContractId == HimTrainingTargetEncodingV1.CONTRACT_ID) {
            "TARGET_ENCODING_BINDING_MISMATCH"
        }
        require(encodedTarget.encodingVersion == HimTrainingTargetEncodingV1.VERSION) {
            "TARGET_ENCODING_BINDING_MISMATCH"
        }
        val digest = recordDigest(
            index,
            record.recordReference,
            positiveExample.exampleReference.value,
            groupReference,
            polarity,
            input,
            encodedTarget,
        )
        return TrainRecord(
            assignmentIndex = index,
            recordReference = record.recordReference,
            positiveExampleReference = positiveExample.exampleReference.value,
            groupReference = groupReference,
            partition = TRAINER_PROTOCOL_PARTITION,
            polarity = polarity,
            input = input,
            encodedTarget = encodedTarget,
            logicalDigest = digest,
        )
    }

    private fun requestDigest(
        trainerPortRequest: HimTrainerPortV1.Request,
        configuration: Configuration,
        modelBinding: ModelBinding,
        records: List<TrainRecord>,
    ): HimSha256 = sha256(buildString {
        field("contract", CONTRACT_ID)
        field("version", VERSION)
        field("state", STATE)
        field("protocol", logicalDigest.value)
        field("protocol-reference", protocolReference)
        field("trainer-port-request", trainerPortRequest.logicalDigest.value)
        field("trainer-port-reference", trainerPortRequest.requestReference)
        field("mission", trainerPortRequest.mission.logicalDigest.value)
        field("mission-reference", trainerPortRequest.mission.missionReference)
        field("configuration", configuration.logicalDigest.value)
        field("configuration-reference", configuration.configurationReference)
        field("seed", configuration.seed.toString())
        field("epochs", configuration.epochs.toString())
        field("micro-batch-size", configuration.microBatchSize.toString())
        field("gradient-accumulation-steps", configuration.gradientAccumulationSteps.toString())
        field("learning-rate", configuration.learningRate)
        field("optimizer-id", configuration.optimizerId)
        field("model-binding", modelBinding.logicalDigest.value)
        field("model-binding-reference", modelBinding.modelBindingReference)
        field("model-family-id", modelBinding.modelFamilyId)
        field("base-model-id", modelBinding.baseModelId)
        field("base-model-artifact", modelBinding.baseModelArtifactDigest.value)
        field("tokenizer-id", modelBinding.tokenizerId)
        field("tokenizer-artifact", modelBinding.tokenizerArtifactDigest.value)
        field("model-configuration-artifact", modelBinding.modelConfigurationArtifactDigest.value)
        field("objective", objectiveDigest.value)
        field("objective-reference", objectiveReference)
        field("target-encoding", targetEncodingDigest.value)
        field("target-encoding-reference", targetEncodingReference)
        field("implementation-fingerprint", trainerPortRequest.mission.implementationBinding.fingerprint.value)
        records.forEach { record ->
            field("record-${record.assignmentIndex}-index", record.assignmentIndex.toString())
            field("record-${record.assignmentIndex}-reference", record.recordReference)
            field("record-${record.assignmentIndex}-positive-example", record.positiveExampleReference)
            field("record-${record.assignmentIndex}-group", record.groupReference)
            field("record-${record.assignmentIndex}-partition", record.partition)
            field("record-${record.assignmentIndex}-polarity", record.polarity.name)
            field("record-${record.assignmentIndex}-digest", record.logicalDigest.value)
            field("record-${record.assignmentIndex}-target", record.encodedTarget.logicalDigest.value)
        }
    })

    fun completed(request: Request): Result.Completed {
        require(request.protocolReference == protocolReference) { "PROTOCOL_REQUEST_BINDING_MISMATCH" }
        val resultDigest = resultDigest(request)
        return Result.Completed(
            requestDigest = request.logicalDigest,
            resultLogicalDigest = resultDigest,
            resultReference = "$RESULT_REFERENCE_PREFIX${resultDigest.value}",
        )
    }

    fun failed(request: Request, safeContext: String): Result.Failed {
        require(request.protocolReference == protocolReference) { "PROTOCOL_REQUEST_BINDING_MISMATCH" }
        require(safeContext.isNotBlank())
        require(safeContext.none { it.isISOControl() })
        return Result.Failed(
            requestDigest = request.logicalDigest,
            reason = FailureReasonV1.PROTOCOL_RUNTIME_FAILED,
            safeContext = safeContext,
        )
    }

    data class Configuration(
        val seed: Long,
        val epochs: Int,
        val microBatchSize: Int,
        val gradientAccumulationSteps: Int,
        val learningRate: String,
        val optimizerId: String,
        val logicalDigest: HimSha256,
        val configurationReference: String,
    ) {
        init {
            require(epochs > 0)
            require(microBatchSize > 0)
            require(gradientAccumulationSteps > 0)
            require(BigDecimal(learningRate).stripTrailingZeros().toPlainString() == learningRate)
            require(BigDecimal(learningRate).signum() > 0)
            require(configurationReference == "training-configuration:v1:${logicalDigest.value}")
        }

        companion object {
            fun from(configuration: HimTrainingConfigurationV1) = Configuration(
                seed = configuration.seed,
                epochs = configuration.epochs,
                microBatchSize = configuration.microBatchSize,
                gradientAccumulationSteps = configuration.gradientAccumulationSteps,
                learningRate = configuration.learningRate.toPlainString(),
                optimizerId = configuration.optimizerId,
                logicalDigest = configuration.logicalDigest,
                configurationReference = configuration.configurationReference,
            )
        }
    }

    data class ModelBinding(
        val modelFamilyId: String,
        val baseModelId: String,
        val baseModelArtifactDigest: HimSha256,
        val tokenizerId: String,
        val tokenizerArtifactDigest: HimSha256,
        val modelConfigurationArtifactDigest: HimSha256,
        val logicalDigest: HimSha256,
        val modelBindingReference: String,
    ) {
        init {
            require(modelFamilyId.isNotBlank() && baseModelId.isNotBlank() && tokenizerId.isNotBlank())
            require(modelBindingReference == "model-binding:v1:${logicalDigest.value}")
        }

        companion object {
            fun from(binding: HimModelBindingV1) = ModelBinding(
                modelFamilyId = binding.modelFamilyId,
                baseModelId = binding.baseModelId,
                baseModelArtifactDigest = binding.baseModelArtifactDigest,
                tokenizerId = binding.tokenizerId,
                tokenizerArtifactDigest = binding.tokenizerArtifactDigest,
                modelConfigurationArtifactDigest = binding.modelConfigurationArtifactDigest,
                logicalDigest = binding.logicalDigest,
                modelBindingReference = binding.modelBindingReference,
            )
        }
    }

    data class Input(
        val observedTerm: String,
        val normalizedObservedTerm: String,
        val canonicalContext: List<CanonicalContext>,
        val evidence: List<Evidence>,
    ) {
        init {
            require(observedTerm.isNotBlank() && normalizedObservedTerm.isNotBlank())
            require(canonicalContext.map { it.rank } == (1..canonicalContext.size).toList())
            require(canonicalContext.size <= 10)
            require(evidence.size <= 40)
        }
    }

    data class CanonicalContext(
        val rank: Int,
        val canonicalId: String,
        val canonicalName: String,
        val fullRecordCanonicalJson: String?,
    )

    data class Evidence(
        val source: String,
        val sourceArtifactSha256: HimSha256,
        val sourceRecordIdentity: String,
        val recordKind: String,
        val retrievalRank: Int,
    )

    enum class PolarityV1 {
        POSITIVE,
        NEGATIVE,
    }

    data class TrainRecord(
        val assignmentIndex: Int,
        val recordReference: String,
        val positiveExampleReference: String,
        val groupReference: String,
        val partition: String,
        val polarity: PolarityV1,
        val input: Input,
        val encodedTarget: HimTrainingTargetEncodingV1.Target,
        val logicalDigest: HimSha256,
    ) {
        init {
            require(assignmentIndex >= 0)
            require(recordReference.isNotBlank() && positiveExampleReference.isNotBlank())
            require(partition == TRAINER_PROTOCOL_PARTITION)
        }
    }

    class Request private constructor(
        val protocolReference: String,
        val trainerPortRequestDigest: HimSha256,
        val trainerPortRequestReference: String,
        val trainingMissionDigest: HimSha256,
        val trainingMissionReference: String,
        val configuration: Configuration,
        val modelBinding: ModelBinding,
        val objectiveDigest: HimSha256,
        val objectiveReference: String,
        val targetEncodingDigest: HimSha256,
        val targetEncodingReference: String,
        val implementationFingerprint: HimSha256,
        val records: List<TrainRecord>,
        val logicalDigest: HimSha256,
        val requestReference: String,
    ) {
        init {
            require(records.isNotEmpty()) { "TRAIN_PROTOCOL_INPUT_EMPTY" }
            require(records.map { it.assignmentIndex } == records.indices.toList()) {
                "TRAIN_PROTOCOL_ORDER_MISMATCH"
            }
            require(records.all { it.partition == TRAINER_PROTOCOL_PARTITION })
            require(trainerPortRequestReference == "trainer-request:v1:${trainerPortRequestDigest.value}")
            require(requestReference == "$REQUEST_REFERENCE_PREFIX${logicalDigest.value}")
        }

        companion object {
            internal fun create(
                protocolReference: String,
                trainerPortRequestDigest: HimSha256,
                trainerPortRequestReference: String,
                trainingMissionDigest: HimSha256,
                trainingMissionReference: String,
                configuration: Configuration,
                modelBinding: ModelBinding,
                objectiveDigest: HimSha256,
                objectiveReference: String,
                targetEncodingDigest: HimSha256,
                targetEncodingReference: String,
                implementationFingerprint: HimSha256,
                records: List<TrainRecord>,
                logicalDigest: HimSha256,
                requestReference: String,
            ) = Request(
                protocolReference,
                trainerPortRequestDigest,
                trainerPortRequestReference,
                trainingMissionDigest,
                trainingMissionReference,
                configuration,
                modelBinding,
                objectiveDigest,
                objectiveReference,
                targetEncodingDigest,
                targetEncodingReference,
                implementationFingerprint,
                records,
                logicalDigest,
                requestReference,
            )
        }
    }

    sealed interface Result {
        data class Completed(
            val requestDigest: HimSha256,
            val resultLogicalDigest: HimSha256,
            val resultReference: String,
        ) : Result

        data class Failed(
            val requestDigest: HimSha256,
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
        PROTOCOL_RUNTIME_FAILED,
    }

    companion object {
        const val CONTRACT_ID = "HIM_TRAINER_PROTOCOL_V1"
        const val VERSION = "1"
        const val STATE = "TRAINER_PROTOCOL_DEFINED"
        const val TRAINER_PROTOCOL_REQUIRES_TRAINER_PORT_REQUEST = "YES"
        const val TRAINER_PROTOCOL_PARTITION = "TRAIN_ONLY"
        const val POLARITY_PRESERVED = "YES"
        const val NEGATIVE_BOUNDARY_PRESERVED = "YES"
        const val MANY_TO_ONE_COLLAPSE = 0
        const val PROTOCOL_SERIALIZATION = "DOMAIN_ONLY"
        const val PROCESS_EXECUTION = 0
        const val MODEL_ARTIFACT_RESOLUTION = 0
        const val OPTIMIZER_MAPPING = 0
        const val TOKENIZATION_EXECUTION = 0
        const val TENSORIZATION = 0
        const val OBJECTIVE_REIMPLEMENTATION = 0
        const val TARGET_ENCODING_REIMPLEMENTATION = 0
        const val PERSISTENCE_WRITES = 0
        const val NUMERICAL_TRAINING_EXECUTION = 0

        private const val PROTOCOL_REFERENCE_PREFIX = "trainer-protocol:v1:"
        private const val REQUEST_REFERENCE_PREFIX = "trainer-request:v1:"
        private const val RESULT_REFERENCE_PREFIX = "trainer-protocol-result:v1:"

        fun create(
            objective: HimTrainingObjectiveV1 = HimTrainingObjectiveV1.create(),
            targetEncoding: HimTrainingTargetEncodingV1 = HimTrainingTargetEncodingV1.create(objective),
        ): HimTrainerProtocolV1 {
            require(targetEncoding.objectiveDigest == objective.logicalDigest) {
                "OBJECTIVE_ENCODING_BINDING_MISMATCH"
            }
            require(targetEncoding.objectiveReference == objective.objectiveReference) {
                "OBJECTIVE_ENCODING_REFERENCE_MISMATCH"
            }
            val digest = protocolDigest(objective.logicalDigest, targetEncoding.logicalDigest)
            return HimTrainerProtocolV1(
                contractId = CONTRACT_ID,
                version = VERSION,
                state = STATE,
                objectiveDigest = objective.logicalDigest,
                objectiveReference = objective.objectiveReference,
                targetEncodingDigest = targetEncoding.logicalDigest,
                targetEncodingReference = targetEncoding.encodingReference,
                logicalDigest = digest,
                protocolReference = "$PROTOCOL_REFERENCE_PREFIX${digest.value}",
                objective = objective,
                targetEncoding = targetEncoding,
            )
        }

        private fun protocolDigest(
            objectiveDigest: HimSha256,
            targetEncodingDigest: HimSha256,
        ): HimSha256 = sha256(buildString {
            field("contract", CONTRACT_ID)
            field("version", VERSION)
            field("state", STATE)
            field("objective", objectiveDigest.value)
            field("objective-reference", "training-objective:v1:${objectiveDigest.value}")
            field("target-encoding", targetEncodingDigest.value)
            field("target-encoding-reference", "training-target-encoding:v1:${targetEncodingDigest.value}")
            field("partition", TRAINER_PROTOCOL_PARTITION)
            field("polarity", POLARITY_PRESERVED)
            field("negative-boundary", NEGATIVE_BOUNDARY_PRESERVED)
            field("many-to-one-collapse", MANY_TO_ONE_COLLAPSE.toString())
            field("serialization", PROTOCOL_SERIALIZATION)
        })

        private fun recordDigest(
            index: Int,
            recordReference: String,
            positiveExampleReference: String,
            groupReference: String,
            polarity: PolarityV1,
            input: Input,
            encodedTarget: HimTrainingTargetEncodingV1.Target,
        ): HimSha256 = sha256(buildString {
            field("contract", CONTRACT_ID)
            field("record-index", index.toString())
            field("record-reference", recordReference)
            field("positive-example-reference", positiveExampleReference)
            field("group-reference", groupReference)
            field("partition", TRAINER_PROTOCOL_PARTITION)
            field("polarity", polarity.name)
            field("observed-term", input.observedTerm)
            field("normalized-observed-term", input.normalizedObservedTerm)
            input.canonicalContext.forEach { context ->
                field("context-${context.rank}-id", context.canonicalId)
                field("context-${context.rank}-name", context.canonicalName)
                field("context-${context.rank}-record", context.fullRecordCanonicalJson ?: "<null>")
            }
            input.evidence.forEachIndexed { evidenceIndex, evidence ->
                field("evidence-$evidenceIndex-source", evidence.source)
                field("evidence-$evidenceIndex-artifact", evidence.sourceArtifactSha256.value)
                field("evidence-$evidenceIndex-record", evidence.sourceRecordIdentity)
                field("evidence-$evidenceIndex-kind", evidence.recordKind)
                field("evidence-$evidenceIndex-rank", evidence.retrievalRank.toString())
            }
            field("target-reference", encodedTarget.targetReference)
            field("target-digest", encodedTarget.logicalDigest.value)
        })

        private fun resultDigest(request: Request): HimSha256 = sha256(buildString {
            field("contract", CONTRACT_ID)
            field("version", VERSION)
            field("request", request.logicalDigest.value)
            field("status", "COMPLETED")
        })

        private fun sha256(value: String): HimSha256 = HimSha256(
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )

        private fun StringBuilder.field(key: String, value: String) {
            append(key).append('=').append(value.length).append(':').append(value).append('\n')
        }
    }
}
