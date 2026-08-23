package de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256

fun interface HimSemanticInferenceOutputDecoder {
    @Throws(HimSemanticInferenceSchemaException::class)
    fun decode(json: String, provenance: HimSemanticInferenceProvenance): HimSemanticInferenceSuccess
}

class HimSemanticInferenceSchemaException(message: String) : IllegalArgumentException(message)

fun interface HimSemanticInferenceRuntime {
    fun infer(request: HimSemanticInferenceRequest): HimSemanticInferenceResult
}

class HimProviderBackedSemanticInferenceRuntime(
    private val provider: HimSemanticInferenceProvider,
    private val configuration: HimSemanticInferenceProviderConfiguration,
    private val decoder: HimSemanticInferenceOutputDecoder,
    private val providerConfigurationFingerprint: HimSha256 = configuration.fingerprint(),
) : HimSemanticInferenceRuntime {

    override fun infer(request: HimSemanticInferenceRequest): HimSemanticInferenceResult {
        var lastFailure: HimSemanticInferenceFailure? = null
        val attemptDiagnostics = mutableListOf<HimSemanticProviderAttemptDiagnostic>()
        for (attempt in 1..MAX_ATTEMPTS) {
            when (val outcome = provider.invoke(request)) {
                is HimSemanticProviderOutcome.TechnicalFailure -> {
                    attemptDiagnostics += HimSemanticProviderAttemptDiagnostic(attempt, outcome.kind, outcome.safeProviderDiagnostic)
                    lastFailure = HimSemanticInferenceFailure(outcome.kind, outcome.safeMessage, attempt, attemptDiagnostics.toList())
                    if (!outcome.kind.retryable) return HimSemanticInferenceResult.TechnicalFailure(lastFailure)
                }
                is HimSemanticProviderOutcome.StructuredResponse -> {
                    if (outcome.json.toByteArray(Charsets.UTF_8).size > configuration.technicalMaximumResponseBytes) {
                        attemptDiagnostics += HimSemanticProviderAttemptDiagnostic(attempt, HimSemanticInferenceFailureKind.SCHEMA_INVALID, null)
                        lastFailure = HimSemanticInferenceFailure(HimSemanticInferenceFailureKind.SCHEMA_INVALID, "Structured response exceeds configured technical byte ceiling", attempt, attemptDiagnostics.toList())
                    } else {
                        val provenance = provenance(request, attempt)
                        val decoded = runCatching { decoder.decode(outcome.json, provenance) }
                        if (decoded.isSuccess) {
                            val success = decoded.getOrThrow()
                            val validation = runCatching { HimSemanticEvidenceReferenceValidator.validate(request, success) }
                            if (validation.isSuccess) return HimSemanticInferenceResult.Success(success)
                        }
                        attemptDiagnostics += HimSemanticProviderAttemptDiagnostic(attempt, HimSemanticInferenceFailureKind.SCHEMA_INVALID, null)
                        lastFailure = HimSemanticInferenceFailure(HimSemanticInferenceFailureKind.SCHEMA_INVALID, "Provider response failed strict HIM schema decoding", attempt, attemptDiagnostics.toList())
                    }
                }
            }
        }
        return HimSemanticInferenceResult.TechnicalFailure(requireNotNull(lastFailure))
    }

    private fun provenance(request: HimSemanticInferenceRequest, attempt: Int) = HimSemanticInferenceProvenance(
        configuration.providerIdentifier,
        configuration.modelIdentifier,
        providerConfigurationFingerprint,
        configuration.inferenceSchemaVersion,
        configuration.instructionPolicyVersion,
        request.retrievalFoundation,
        attempt,
    )

    companion object {
        const val MAX_TECHNICAL_RETRIES = 1
        const val MAX_ATTEMPTS = MAX_TECHNICAL_RETRIES + 1
    }
}
