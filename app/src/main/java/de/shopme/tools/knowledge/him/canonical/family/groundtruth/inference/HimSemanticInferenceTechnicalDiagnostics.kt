package de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound

enum class HimSemanticInferencePhase { INITIAL, AFTER_RETRIEVAL }

data class HimSemanticInferenceTechnicalDiagnostic(
    val inputReference: String,
    val inferencePhase: HimSemanticInferencePhase,
    val retrievalRound: HimRetrievalRound,
    val logicalInferenceCallNumber: Int,
    val physicalAttemptCount: Int,
    val technicalRetryCount: Int,
    val finalTechnicalFailureType: HimSemanticInferenceFailureKind?,
    val completedSuccessfully: Boolean,
    val lastSuccessfulRetrievalRound: HimRetrievalRound,
    val attemptDiagnostics: List<HimSemanticProviderAttemptDiagnostic> = emptyList(),
) {
    init {
        require(inputReference.isNotBlank())
        require(logicalInferenceCallNumber > 0)
        require(physicalAttemptCount in 0..HimProviderBackedSemanticInferenceRuntime.MAX_ATTEMPTS)
        require(technicalRetryCount == (physicalAttemptCount - 1).coerceAtLeast(0))
        require((retrievalRound.value == 0) == (inferencePhase == HimSemanticInferencePhase.INITIAL))
        require(lastSuccessfulRetrievalRound.value <= retrievalRound.value)
        require(completedSuccessfully == (finalTechnicalFailureType == null))
        require(finalTechnicalFailureType != HimSemanticInferenceFailureKind.CONTEXT_BUDGET_EXCEEDED || physicalAttemptCount == 0)
        require(attemptDiagnostics.size <= physicalAttemptCount)
    }

    companion object {
        fun fromResult(
            inputReference: String,
            retrievalRound: HimRetrievalRound,
            logicalInferenceCallNumber: Int,
            result: HimSemanticInferenceResult,
            lastSuccessfulRetrievalRound: HimRetrievalRound,
        ): HimSemanticInferenceTechnicalDiagnostic {
            val attempts = when (result) {
                is HimSemanticInferenceResult.Success -> result.value.provenance.technicalAttemptCount
                is HimSemanticInferenceResult.TechnicalFailure -> result.failure.technicalAttemptCount
            }
            return HimSemanticInferenceTechnicalDiagnostic(
                inputReference,
                if (retrievalRound.value == 0) HimSemanticInferencePhase.INITIAL else HimSemanticInferencePhase.AFTER_RETRIEVAL,
                retrievalRound,
                logicalInferenceCallNumber,
                attempts,
                attempts - 1,
                (result as? HimSemanticInferenceResult.TechnicalFailure)?.failure?.kind,
                result is HimSemanticInferenceResult.Success,
                lastSuccessfulRetrievalRound,
                (result as? HimSemanticInferenceResult.TechnicalFailure)?.failure?.attemptDiagnostics.orEmpty(),
            )
        }

        fun contextBudgetExceeded(
            inputReference: String,
            retrievalRound: HimRetrievalRound,
            logicalInferenceCallNumber: Int,
            lastSuccessfulRetrievalRound: HimRetrievalRound,
        ) = HimSemanticInferenceTechnicalDiagnostic(
            inputReference,
            if (retrievalRound.value == 0) HimSemanticInferencePhase.INITIAL else HimSemanticInferencePhase.AFTER_RETRIEVAL,
            retrievalRound,
            logicalInferenceCallNumber,
            0,
            0,
            HimSemanticInferenceFailureKind.CONTEXT_BUDGET_EXCEEDED,
            false,
            lastSuccessfulRetrievalRound,
            emptyList(),
        )
    }
}

data class HimCandidateInputTechnicalAggregate(
    val rawInput: String,
    val inputReference: String,
    val completed: Boolean,
    val finalSemanticOutcome: String?,
    val technicalFailureType: HimSemanticInferenceFailureKind?,
    val logicalInferenceCalls: Int,
    val physicalProviderAttempts: Int,
    val technicalRetries: Int,
    val retrievalRoundsExecuted: Int,
    val lastSuccessfulRetrievalRound: HimRetrievalRound,
    val persistentCandidateCount: Int,
    val persistenceEligible: Boolean,
    val invocations: List<HimSemanticInferenceTechnicalDiagnostic>,
) {
    init {
        require(rawInput.isNotBlank() && inputReference.isNotBlank())
        require(logicalInferenceCalls == invocations.size)
        require(physicalProviderAttempts == invocations.sumOf { it.physicalAttemptCount })
        require(technicalRetries == invocations.sumOf { it.technicalRetryCount })
        require(retrievalRoundsExecuted in 0..HimRetrievalRound.MAX_ROUNDS)
        require(persistentCandidateCount >= 0)
        require(completed == (technicalFailureType == null))
        require(!persistenceEligible || completed)
    }
}

data class HimCandidateRunTechnicalAggregate(
    val inputCount: Int,
    val completedInputCount: Int,
    val failedTechnicalInputCount: Int,
    val logicalInferenceCallsTotal: Int,
    val physicalProviderAttemptsTotal: Int,
    val technicalRetriesTotal: Int,
    val retrievalRoundsTotal: Int,
    val persistentCandidateCountBeforePublicationGate: Int,
    val publicationAllowed: Boolean,
    val inputs: List<HimCandidateInputTechnicalAggregate>,
) {
    init {
        require(inputCount >= inputs.size)
        require(completedInputCount == inputs.count { it.completed })
        require(failedTechnicalInputCount == inputs.count { !it.completed })
        require(logicalInferenceCallsTotal == inputs.sumOf { it.logicalInferenceCalls })
        require(physicalProviderAttemptsTotal == inputs.sumOf { it.physicalProviderAttempts })
        require(technicalRetriesTotal == inputs.sumOf { it.technicalRetries })
        require(retrievalRoundsTotal == inputs.sumOf { it.retrievalRoundsExecuted })
        require(persistentCandidateCountBeforePublicationGate == inputs.sumOf { it.persistentCandidateCount })
        require(publicationAllowed == (inputs.size == inputCount && failedTechnicalInputCount == 0))
    }

    companion object {
        fun from(expectedInputCount: Int, inputs: List<HimCandidateInputTechnicalAggregate>) = HimCandidateRunTechnicalAggregate(
            expectedInputCount,
            inputs.count { it.completed },
            inputs.count { !it.completed },
            inputs.sumOf { it.logicalInferenceCalls },
            inputs.sumOf { it.physicalProviderAttempts },
            inputs.sumOf { it.technicalRetries },
            inputs.sumOf { it.retrievalRoundsExecuted },
            inputs.sumOf { it.persistentCandidateCount },
            inputs.size == expectedInputCount && inputs.all { it.completed },
            inputs,
        )
    }
}
