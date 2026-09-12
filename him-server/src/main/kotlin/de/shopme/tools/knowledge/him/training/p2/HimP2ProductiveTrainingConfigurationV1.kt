package de.shopme.tools.knowledge.him.training.p2

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Explicit, server-owned numerical and trajectory authority for the P2 pilot. */
object HimP2ProductiveTrainingConfigurationV1 {
    const val CONTRACT_ID = "HIM_P2_PRODUCTIVE_TRAINING_CONFIGURATION_V1"
    const val VERSION = 1
    const val STATE = "P2_PRODUCTIVE_TRAINING_CONFIGURATION_AUTHORIZED"

    private const val LINEAGE = "p2-lineage:v1:53a946c4aaefe1a27e5dfdc78dea7aad678e7fd212c2d66d40ec9a72bc9fa110"
    private const val CATALOG = "canonical-catalog:v1:922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f"
    private const val CORPUS_LOGICAL = "fd65bcdfcf5811dd497722a80e68bb3bf0b1819e1c771f0ef9e9a01eabbfc8ca"
    private const val CORPUS_PHYSICAL = "9ae2b711b39270d1c6a2ecb5b2dc6163f254d1707bca1803a6cefa049a5b43cc"
    private const val PARTITION_LOGICAL = "5f31d055ad9f05f9a978f25011a6dfc8abc292fad5b9612e328896a617713d9d"
    private const val PARTITION_PHYSICAL = "809536cd9c8f22f3f122c6d96ab37d6e15dd9fe59d113de791b515bb9b91ac71"
    private const val LEAKAGE_LOGICAL = "e38a5cf75f37a77f9fb49c9224d6184494cd69e5f508d1cbcbebab46a82ce073"
    private const val EVALUATION_READINESS_LOGICAL = "e8ba616849b2725ec370b4d3551b882ef91f08619dfb7d0131a0de9144c16f37b"

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    data class Authority(
        val fields: Map<String, String>,
        val logicalDigest: String,
        val reference: String,
    ) {
        init {
            require(fields == fields.toSortedMap())
            require(logicalDigest == digest(fields))
            require(reference == "p2-training-configuration:v1:$logicalDigest")
            require(fields["contractId"] == CONTRACT_ID)
            require(fields["version"] == VERSION.toString())
        }

        fun serialize(): String {
            val json = JsonObject().apply {
                addProperty("contractId", CONTRACT_ID)
                addProperty("version", VERSION)
                addProperty("state", STATE)
                add("fields", gson.toJsonTree(fields))
                addProperty("logicalDigest", logicalDigest)
                addProperty("reference", reference)
            }
            return gson.toJson(json) + "\n"
        }
    }

    fun current(): Authority {
        val fields = sortedMapOf(
            "catalogReference" to CATALOG,
            "contractId" to CONTRACT_ID,
            "corpusLogicalDigest" to CORPUS_LOGICAL,
            "corpusPhysicalDigest" to CORPUS_PHYSICAL,
            "evaluationReadinessLogicalDigest" to EVALUATION_READINESS_LOGICAL,
            "expectedBackwardCount" to "12",
            "expectedForwardCountTrain" to "12",
            "expectedHoldoutForwardCount" to "0",
            "expectedOptimizerStepCount" to "12",
            "expectedValidationForwardCount" to "1",
            "epochs" to "3",
            "gradientAccumulationSteps" to "1",
            "holdoutExampleCount" to "0",
            "holdoutEvidenceState" to "EMPTY_BY_PARTITION",
            "holdoutFamilyCount" to "0",
            "holdoutMetricCount" to "0",
            "holdoutPredictionCount" to "0",
            "learningRate" to "0.0001",
            "microBatchSize" to "8",
            "optimizerId" to "optimizer:adamw:v1",
            "p2LineageReference" to LINEAGE,
            "partitionLogicalDigest" to PARTITION_LOGICAL,
            "partitionPhysicalDigest" to PARTITION_PHYSICAL,
            "precision" to "FP32",
            "checkpointContract" to "HIM_TRAINING_CHECKPOINT_CONTRACT_V1",
            "checkpointReloadGate" to "MODEL_STATE_RELOAD_EQUIVALENCE,OPTIMIZER_STATE_RELOAD_EQUIVALENCE,STEP_RELOAD_EQUIVALENCE",
            "runEvidenceContract" to "HIM_P2_PRODUCTIVE_TRAINING_RUN_EVIDENCE_V1",
            "validationEvidenceContract" to "HIM_P2_VALIDATION_EVIDENCE_V1",
            "holdoutEvidenceArtifactRequired" to "NO",
            "runtimeCudaBuild" to "13.0",
            "runtimeInterpreter" to "/opt/him/runtime/bin/python",
            "runtimePython" to "3.13.14",
            "runtimePyTorch" to "2.14.0+cu130",
            "runtimeSentencePiece" to "0.2.2",
            "runtimeTokenizers" to "0.23.1",
            "seed" to "7",
            "sequenceLength" to "128",
            "secondaryOnlyTrainCount" to "9",
            "secondaryRejectTarget" to "REJECT",
            "trainCompatibleCount" to "17",
            "trainExampleCount" to "26",
            "trainFamilyCount" to "9",
            "trainIdentityCount" to "3",
            "trainPrimaryActiveCount" to "17",
            "trainSecondaryActiveCount" to "26",
            "trainVariantCount" to "14",
            "trainingConfigurationRationale" to "Explicit P2 pilot decision: reuse P1 numerical continuity by new P2 authority; FP32 is retained and no AMP mode is introduced.",
            "validationCompatibleCount" to "6",
            "validationExampleCount" to "6",
            "validationFamilyCount" to "3",
            "validationIdentityCount" to "2",
            "validationRejectCount" to "0",
            "validationRejectGeneralizationMeasurable" to "NO",
            "validationSecondaryActiveCount" to "6",
            "validationVariantCount" to "4",
            "validationPolicy" to "EACH_EPOCH_AND_FINAL",
            "weightDecay" to "0.01",
            "version" to VERSION.toString(),
        )
        val digest = digest(fields)
        return Authority(fields, digest, "p2-training-configuration:v1:$digest")
    }

    fun fromJson(json: String): Authority {
        val root = JsonParser.parseString(json).asJsonObject
        require(root.get("contractId").asString == CONTRACT_ID)
        require(root.get("version").asInt == VERSION)
        require(root.get("state").asString == STATE)
        val fields = root.getAsJsonObject("fields").entrySet().associate { it.key to it.value.asString }.toSortedMap()
        return Authority(fields, root.get("logicalDigest").asString, root.get("reference").asString)
    }

    private fun digest(fields: Map<String, String>): String = MessageDigest.getInstance("SHA-256").digest(
        fields.entries.joinToString("") { "${it.key}=${it.value.length}:\n${it.value}\n" }
            .toByteArray(StandardCharsets.UTF_8),
    ).joinToString("") { "%02x".format(it) }
}
