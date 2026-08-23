package de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.security.MessageDigest

data class HimSemanticInferenceProviderConfiguration(
    val providerIdentifier: String,
    val modelIdentifier: String,
    val configurationVersion: String,
    val inferenceSchemaVersion: String,
    val instructionPolicyVersion: String,
    val temperature: String?,
    val topP: String?,
    val maxOutputTokens: Int?,
    val reasoningMode: String?,
    val timeoutPolicyIdentifier: String,
    val technicalMaximumResponseBytes: Int,
) {
    init {
        listOf(providerIdentifier, modelIdentifier, configurationVersion, inferenceSchemaVersion, instructionPolicyVersion, timeoutPolicyIdentifier).forEach { require(it.isNotBlank()) }
        require(inferenceSchemaVersion == HimSemanticInferenceSchema.OUTPUT_VERSION)
        require(technicalMaximumResponseBytes > 0)
        require(maxOutputTokens == null || maxOutputTokens > 0)
        require(!providerIdentifier.contains("key", ignoreCase = true))
    }

    fun fingerprint(): HimSha256 {
        val canonical = buildString {
            appendLine("fingerprint-contract=HIM_SEMANTIC_PROVIDER_CONFIGURATION_FINGERPRINT_V1")
            appendLine("provider=$providerIdentifier")
            appendLine("model=$modelIdentifier")
            appendLine("configuration-version=$configurationVersion")
            appendLine("inference-schema-version=$inferenceSchemaVersion")
            appendLine("instruction-policy-version=$instructionPolicyVersion")
            appendLine("temperature=${temperature.orEmpty()}")
            appendLine("top-p=${topP.orEmpty()}")
            appendLine("max-output-tokens=${maxOutputTokens?.toString().orEmpty()}")
            appendLine("reasoning-mode=${reasoningMode.orEmpty()}")
            appendLine("timeout-policy=$timeoutPolicyIdentifier")
            appendLine("technical-maximum-response-bytes=$technicalMaximumResponseBytes")
        }
        val value = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return HimSha256(value)
    }
}

data class HimSemanticInferenceProvenance(
    val providerIdentifier: String,
    val modelIdentifier: String,
    val providerConfigurationFingerprint: HimSha256,
    val inferenceSchemaVersion: String,
    val instructionPolicyVersion: String,
    val retrievalFoundation: HimRetrievalFoundationBinding,
    val technicalAttemptCount: Int,
) {
    init {
        require(providerIdentifier.isNotBlank() && modelIdentifier.isNotBlank())
        require(inferenceSchemaVersion == HimSemanticInferenceSchema.OUTPUT_VERSION)
        require(instructionPolicyVersion.isNotBlank())
        require(technicalAttemptCount in 1..2)
    }
}

enum class HimSemanticInferenceFailureKind {
    PROVIDER_UNAVAILABLE,
    TIMEOUT,
    TRANSPORT_ERROR,
    PROVIDER_ERROR,
    RATE_LIMITED,
    SCHEMA_INVALID,
    CONFIGURATION_ERROR,
    CONTEXT_BUDGET_EXCEEDED,
    ;

    val retryable: Boolean
        get() = this in setOf(TIMEOUT, TRANSPORT_ERROR, PROVIDER_ERROR, RATE_LIMITED, SCHEMA_INVALID)
}

data class HimSemanticInferenceFailure(
    val kind: HimSemanticInferenceFailureKind,
    val safeMessage: String,
    val technicalAttemptCount: Int,
    val attemptDiagnostics: List<HimSemanticProviderAttemptDiagnostic> = emptyList(),
) {
    init {
        require(safeMessage.isNotBlank() && technicalAttemptCount in 1..2)
        require(attemptDiagnostics.size <= technicalAttemptCount)
        require(attemptDiagnostics.map { it.attemptNumber } == (1..attemptDiagnostics.size).toList())
    }
}

data class HimSemanticProviderErrorDiagnostic(
    val provider: String,
    val httpStatusCode: Int?,
    val httpStatusFamily: Int?,
    val providerErrorType: String?,
    val providerErrorCode: String?,
    val providerErrorParam: String?,
    val requestReachedProvider: Boolean?,
    val providerResponseReceived: Boolean,
    val usageReceived: Boolean,
) {
    init {
        require(provider.isNotBlank())
        require(httpStatusCode == null || httpStatusCode in 100..599)
        require(httpStatusFamily == httpStatusCode?.div(100))
        require(!providerResponseReceived || requestReachedProvider == true)
        require(listOfNotNull(providerErrorType, providerErrorCode).all { SAFE_IDENTIFIER.matches(it) && it.length <= 128 })
        require(providerErrorParam == null || SAFE_PARAM.matches(providerErrorParam) && providerErrorParam.length <= 256)
    }

    companion object {
        private val SAFE_IDENTIFIER = Regex("[A-Za-z0-9_-]+")
        private val SAFE_PARAM = Regex("[A-Za-z0-9_.\\[\\]-]+")
    }
}

data class HimSemanticProviderAttemptDiagnostic(
    val attemptNumber: Int,
    val providerNeutralFailure: HimSemanticInferenceFailureKind?,
    val safeProviderDiagnostic: HimSemanticProviderErrorDiagnostic?,
) {
    init { require(attemptNumber in 1..2) }
}

sealed interface HimSemanticProviderOutcome {
    data class StructuredResponse(val json: String) : HimSemanticProviderOutcome
    data class TechnicalFailure(
        val kind: HimSemanticInferenceFailureKind,
        val safeMessage: String,
        val safeProviderDiagnostic: HimSemanticProviderErrorDiagnostic? = null,
    ) : HimSemanticProviderOutcome
}

fun interface HimSemanticInferenceProvider {
    fun invoke(request: HimSemanticInferenceRequest): HimSemanticProviderOutcome
}
