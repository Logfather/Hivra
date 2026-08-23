package de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetrievalTerminalState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HimGroundTruthRetrievalInfrastructureTest {

    @Test
    fun allSourcesExistAsSelectableOptionsWithoutBeingMandatory() {
        assertEquals(4, HimGroundTruthSource.entries.size)
        assertEquals(setOf(HimGroundTruthSource.CIQUAL), selected(HimGroundTruthSource.CIQUAL))
        assertEquals(
            setOf(HimGroundTruthSource.CIQUAL, HimGroundTruthSource.AGRIBALYSE),
            selected(HimGroundTruthSource.CIQUAL, HimGroundTruthSource.AGRIBALYSE),
        )
        assertFalse(selected(HimGroundTruthSource.CIQUAL).size == 4)
    }

    @Test
    fun boundedRetrieverRejectsMoreThanTenEvidenceRecords() {
        val request = request(HimGroundTruthSource.CIQUAL)
        val valid = HimBoundedGroundTruthSourceRetriever { (1..10).map { record(request, it) } }
        val invalid = HimBoundedGroundTruthSourceRetriever { (1..11).map { record(request, it) } }

        assertEquals(10, valid.retrieve(request).size)
        assertFailsWith<IllegalArgumentException> { invalid.retrieve(request) }
    }

    @Test
    fun supportsThreeRoundsAndRejectsRoundFour() {
        var session = HimGroundTruthRetrievalSession("input")
        (1..3).forEach { round -> session = session.addRound(roundState(round)) }

        assertEquals(listOf(1, 2, 3), session.rounds.map { it.round.value })
        assertFailsWith<IllegalArgumentException> { HimRetrievalRound(4) }
        assertFailsWith<IllegalArgumentException> { session.addRound(roundState(3)) }
    }

    @Test
    fun bothTerminalStatesAreRepresentableWithoutTruthDecision() {
        val session = HimGroundTruthRetrievalSession("input").addRound(roundState(1))

        assertEquals(
            HimRetrievalTerminalState.SUFFICIENT_EVIDENCE,
            session.complete(HimRetrievalTerminalState.SUFFICIENT_EVIDENCE).terminalState,
        )
        assertEquals(
            HimRetrievalTerminalState.SEARCH_EXHAUSTED,
            session.complete(HimRetrievalTerminalState.SEARCH_EXHAUSTED).terminalState,
        )
        assertTrue(
            HimRetrievalTerminalState::class.java.declaredFields.none {
                it.name.contains("REJECT", ignoreCase = true) ||
                        it.name.contains("FALSE", ignoreCase = true)
            }
        )
    }

    private fun selected(vararg sources: HimGroundTruthSource) = sources.toSet()

    private fun request(source: HimGroundTruthSource) =
        HimSourceRetrievalRequest(
            source = source,
            sourceArtifactSha256 = HimSha256("a".repeat(64)),
            semanticQuery = "query",
        )

    private fun record(request: HimSourceRetrievalRequest, rank: Int) =
        HimSourceEvidenceRecord(
            evidenceReference =
                HimEvidenceReference(
                    source = request.source.name,
                    sourceArtifactSha256 = request.sourceArtifactSha256,
                    sourceRecordIdentity = "record-$rank",
                ),
            sourceArtifactPath = request.source.artifactPath,
            retrievalRank = rank,
        )

    private fun roundState(round: Int) =
        HimGroundTruthRetrievalRoundState(
            round = HimRetrievalRound(round),
            selectedSources = setOf(HimGroundTruthSource.CIQUAL),
            semanticQueries = listOf("query-$round"),
            sourceSteps = emptyList(),
        )
}
