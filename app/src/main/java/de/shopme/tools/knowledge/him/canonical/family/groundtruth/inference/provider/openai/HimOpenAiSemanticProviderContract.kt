package de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticContextBudgetPolicy
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInstructionPolicyV2
import java.security.MessageDigest

object HimOpenAiSemanticProviderDecisionV1 {
    const val PROVIDER = "OPENAI"
    const val MODEL = "gpt-5.6-sol"
    const val MODEL_VERSION_BEHAVIOR = "NO_DATED_SNAPSHOT_DOCUMENTED_USE_EXACT_MODEL_ID"
    const val API_MODE = "RESPONSES_API_V1"
    const val STRUCTURED_OUTPUT_MODE = "JSON_SCHEMA_STRICT"
    const val CONFIGURATION_VERSION = "HIM_OPENAI_SEMANTIC_PROVIDER_CONFIGURATION_V1"
    const val OUTPUT_BUDGET_POLICY_VERSION = "HIM_SEMANTIC_OUTPUT_BUDGET_POLICY_V1"
    const val TIMEOUT_POLICY_VERSION = "HIM_OPENAI_REASONING_TIMEOUT_POLICY_V1"
    const val TIMEOUT_MILLISECONDS = 180_000L
    const val MAX_OUTPUT_TOKENS = 8_192
    const val REASONING_EFFORT = "medium"
}

data class HimOpenAiSemanticProviderConfiguration(
    val provider: String = HimOpenAiSemanticProviderDecisionV1.PROVIDER,
    val model: String = HimOpenAiSemanticProviderDecisionV1.MODEL,
    val configurationVersion: String = HimOpenAiSemanticProviderDecisionV1.CONFIGURATION_VERSION,
    val apiMode: String = HimOpenAiSemanticProviderDecisionV1.API_MODE,
    val structuredOutputMode: String = HimOpenAiSemanticProviderDecisionV1.STRUCTURED_OUTPUT_MODE,
    val inferenceSchemaVersion: String = HimSemanticInferenceSchema.OUTPUT_VERSION,
    val instructionPolicyVersion: String = HimSemanticInstructionPolicyV2.VERSION,
    val contextBudgetPolicyVersion: String = HimSemanticContextBudgetPolicy.VERSION,
    val evidenceViewPolicyVersion: String = "F3D_2_SOURCE_FAITHFUL_EVIDENCE_PROJECTION_AS_IS_V1",
    val outputBudgetPolicyVersion: String = HimOpenAiSemanticProviderDecisionV1.OUTPUT_BUDGET_POLICY_VERSION,
    val timeoutPolicyVersion: String = HimOpenAiSemanticProviderDecisionV1.TIMEOUT_POLICY_VERSION,
    val timeoutMilliseconds: Long = HimOpenAiSemanticProviderDecisionV1.TIMEOUT_MILLISECONDS,
    val maxOutputTokens: Int = HimOpenAiSemanticProviderDecisionV1.MAX_OUTPUT_TOKENS,
    val reasoningEffort: String = HimOpenAiSemanticProviderDecisionV1.REASONING_EFFORT,
    val maximumTechnicalRetries: Int = 1,
    val storeProviderResponse: Boolean = false,
) {
    init {
        require(provider == "OPENAI" && model == "gpt-5.6-sol")
        require(inferenceSchemaVersion == HimSemanticInferenceSchema.OUTPUT_VERSION)
        require(instructionPolicyVersion == HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION)
        require(timeoutMilliseconds > 0 && maxOutputTokens > 0 && maximumTechnicalRetries == 1)
        require(!storeProviderResponse)
    }

    fun fingerprint(): HimSha256 {
        val context = HimSemanticContextBudgetPolicy.OPENAI_GPT_5_6_SOL_V1
        val canonical = listOf(
            "fingerprint-contract=HIM_SEMANTIC_PROVIDER_CONFIGURATION_FINGERPRINT_V1",
            "provider=$provider", "model=$model", "configuration-version=$configurationVersion",
            "api-mode=$apiMode", "structured-output-mode=$structuredOutputMode",
            "inference-schema-version=$inferenceSchemaVersion", "instruction-policy-version=$instructionPolicyVersion",
            "context-budget-policy-version=$contextBudgetPolicyVersion", "evidence-view-policy-version=$evidenceViewPolicyVersion",
            "provider-context-capacity-tokens=${context.providerContextCapacityTokens}",
            "authorized-maximum-input-bytes=${context.authorizedMaximumInputBytes}",
            "conservative-input-token-ceiling=${context.conservativeInputTokenCeiling}",
            "reserved-output-tokens=${context.reservedOutputTokens}",
            "provider-framing-and-estimation-reserve-tokens=${context.providerFramingAndEstimationReserveTokens}",
            "output-budget-policy-version=$outputBudgetPolicyVersion", "timeout-policy-version=$timeoutPolicyVersion",
            "timeout-milliseconds=$timeoutMilliseconds", "max-output-tokens=$maxOutputTokens",
            "reasoning-effort=$reasoningEffort", "maximum-technical-retries=$maximumTechnicalRetries",
            "store-provider-response=$storeProviderResponse",
        ).joinToString(separator = "\n", postfix = "\n")
        return HimSha256(MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) })
    }
}
