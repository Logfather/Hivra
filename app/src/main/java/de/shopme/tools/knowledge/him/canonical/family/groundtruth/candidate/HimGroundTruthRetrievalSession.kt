package de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetrievalTerminalState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSuccess
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticRetrievalDirective

data class HimRetrievalRound(
    val value: Int,
) {
    init {
        require(value in 0..MAX_ROUNDS)
    }

    companion object {
        const val MAX_ROUNDS = 3
    }
}

data class HimSourceRetrievalStep(
    val source: HimGroundTruthSource,
    val semanticQuery: String,
    val evidence: List<HimSourceEvidenceRecord>,
) {
    init {
        require(semanticQuery.isNotBlank())
        require(evidence.size <= HimSourceRetrievalRequest.MAX_EVIDENCE_RECORDS)
    }
}

data class HimGroundTruthRetrievalRoundState(
    val round: HimRetrievalRound,
    val selectedSources: Set<HimGroundTruthSource>,
    val semanticQueries: List<String>,
    val sourceSteps: List<HimSourceRetrievalStep>,
) {
    init {
        require(selectedSources.isNotEmpty())
        require(semanticQueries.isNotEmpty() && semanticQueries.all { it.isNotBlank() })
        require(sourceSteps.all { it.source in selectedSources })
    }
}

data class HimGroundTruthRetrievalSession(
    val originalInput: String,
    val rounds: List<HimGroundTruthRetrievalRoundState> = emptyList(),
    val terminalState: HimRetrievalTerminalState? = null,
) {
    init {
        require(originalInput.isNotBlank())
        require(rounds.size <= HimRetrievalRound.MAX_ROUNDS)
        require(rounds.map { it.round.value } == (1..rounds.size).toList())
    }

    fun addRound(roundState: HimGroundTruthRetrievalRoundState): HimGroundTruthRetrievalSession {
        require(terminalState == null)
        require(rounds.size < HimRetrievalRound.MAX_ROUNDS)
        require(roundState.round.value == rounds.size + 1)
        return copy(rounds = rounds + roundState)
    }

    fun complete(state: HimRetrievalTerminalState): HimGroundTruthRetrievalSession {
        require(terminalState == null)
        return copy(terminalState = state)
    }
}

fun interface HimSemanticRetrievalDirectiveExecutor {
    fun execute(source: HimGroundTruthSource, semanticQuery: String, maximumEvidence: Int): List<HimSourceEvidenceRecord>
}

class HimSemanticRetrievalDirectiveCoordinator(
    private val executor: HimSemanticRetrievalDirectiveExecutor,
) {
    fun execute(session: HimGroundTruthRetrievalSession, directive: HimSemanticRetrievalDirective): HimGroundTruthRetrievalSession {
        require(session.terminalState == null)
        require(session.rounds.size < HimRetrievalRound.MAX_ROUNDS)
        val identity = identity(directive)
        require(session.rounds.none { identity(it) == identity }) { "Identical semantic retrieval directive was already executed." }
        val steps = directive.sourceQueries.flatMap { sourceQueries ->
            sourceQueries.queries.map { query ->
                val evidence = executor.execute(sourceQueries.source, query, HimSourceRetrievalRequest.MAX_EVIDENCE_RECORDS)
                require(evidence.size <= HimSourceRetrievalRequest.MAX_EVIDENCE_RECORDS)
                HimSourceRetrievalStep(sourceQueries.source, query, evidence)
            }
        }
        return session.addRound(
            HimGroundTruthRetrievalRoundState(
                round = HimRetrievalRound(session.rounds.size + 1),
                selectedSources = directive.sourceQueries.map { it.source }.toSet(),
                semanticQueries = directive.sourceQueries.flatMap { it.queries },
                sourceSteps = steps,
            )
        )
    }

    fun complete(session: HimGroundTruthRetrievalSession, result: HimSemanticInferenceSuccess): HimGroundTruthRetrievalSession {
        require(result.informationGain == HimSemanticInformationGainJudgment.NO_EXPECTED_INFORMATION_GAIN)
        return session.complete(
            if (result.candidates.isEmpty()) HimRetrievalTerminalState.SEARCH_EXHAUSTED
            else HimRetrievalTerminalState.SUFFICIENT_EVIDENCE
        )
    }

    private fun identity(directive: HimSemanticRetrievalDirective): List<String> =
        directive.sourceQueries.flatMap { sourceQueries -> sourceQueries.queries.map { "${sourceQueries.source.name}\u0000$it" } }.sorted()

    private fun identity(round: HimGroundTruthRetrievalRoundState): List<String> =
        round.sourceSteps.map { "${it.source.name}\u0000${it.semanticQuery}" }.sorted()
}
