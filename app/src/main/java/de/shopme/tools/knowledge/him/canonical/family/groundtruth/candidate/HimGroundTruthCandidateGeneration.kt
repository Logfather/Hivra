package de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthCandidate
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetrievalTerminalState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimValidationDecision

data class HimGroundTruthCandidateGenerationRequest(
    val originalInput: String,
    val canonicalRetrievalContext: List<HimCanonicalRetrievalResult>,
    val retrievalRound: HimRetrievalRound,
    val selectedSources: Set<HimGroundTruthSource>,
    val semanticRetrievalQueries: List<String>,
    val sourceEvidence: List<HimSourceEvidenceRecord>,
    val previousCandidateHypotheses: List<HimGroundTruthCandidate> = emptyList(),
) {
    init {
        require(originalInput.isNotBlank())
        require(canonicalRetrievalContext.size <= HimCanonicalFamilyCandidateRetrieval.MAX_CANDIDATES)
        require(selectedSources.isNotEmpty())
        require(semanticRetrievalQueries.isNotEmpty())
        require(sourceEvidence.groupBy { it.evidenceReference.source }.values.all {
            it.size <= HimSourceRetrievalRequest.MAX_EVIDENCE_RECORDS
        })
    }
}

data class HimGroundTruthCandidateDiagnostics(
    val inputCount: Int,
    val canonicalRetrievalCount: Int,
    val sourceRetrievalCount: Int,
    val candidateCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowDroppedCount: Int,
    val noConfidenceDroppedCount: Int,
    val knownRelationCount: Int,
    val retrievalRoundsUsed: Int,
    val searchExhaustedCount: Int,
)

data class HimKnownRelationEvidence(
    val candidate: HimGroundTruthCandidate,
    val decision: HimValidationDecision = HimValidationDecision.KNOWN_RELATION_EVIDENCE,
) {
    init {
        require(decision == HimValidationDecision.KNOWN_RELATION_EVIDENCE)
    }
}

data class HimGroundTruthCandidateGenerationResult(
    val candidates: List<HimGroundTruthCandidate>,
    val knownRelations: List<HimKnownRelationEvidence>,
    val terminalState: HimRetrievalTerminalState?,
    val evidenceReferences: List<de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference>,
    val diagnostics: HimGroundTruthCandidateDiagnostics,
)

data class HimCandidateFilterResult(
    val retained: List<HimGroundTruthCandidate>,
    val lowDropped: Int,
    val noConfidenceDropped: Int,
)

class HimGroundTruthCandidateFilter {

    fun filter(candidates: List<HimGroundTruthCandidate>): HimCandidateFilterResult =
        HimCandidateFilterResult(
            retained = candidates.filter {
                it.candidateConfidence == HimCandidateConfidence.HIGH ||
                        it.candidateConfidence == HimCandidateConfidence.MEDIUM
            },
            lowDropped = candidates.count {
                it.candidateConfidence == HimCandidateConfidence.LOW
            },
            noConfidenceDropped = candidates.count {
                it.candidateConfidence == HimCandidateConfidence.NO_CONFIDENCE
            },
        )
}

class HimGroundTruthCandidateConsolidator {

    fun consolidate(candidates: List<HimGroundTruthCandidate>): List<HimGroundTruthCandidate> =
        candidates
            .groupBy { it.candidateReference }
            .map { (_, hypotheses) ->
                val first = hypotheses.first()
                require(hypotheses.all {
                    it.candidateTerm == first.candidateTerm &&
                            it.relation == first.relation &&
                            it.candidateConfidence == first.candidateConfidence
                }) {
                    "Only explicitly identical semantic hypotheses may be consolidated."
                }
                first.copy(
                    evidenceReferences =
                        hypotheses.flatMap { it.evidenceReferences }.distinct()
                )
            }
}

class HimKnownRelationDetector(
    private val authority: HimCanonicalFamilyAuthority,
) {

    fun detect(candidates: List<HimGroundTruthCandidate>): Pair<
            List<HimGroundTruthCandidate>,
            List<HimKnownRelationEvidence>,
            > {
        val known = candidates.filter(::isKnown).map(::HimKnownRelationEvidence)
        val novel = candidates.filterNot(::isKnown)
        return novel to known
    }

    private fun isKnown(candidate: HimGroundTruthCandidate): Boolean =
        when (val relation = candidate.relation) {
            is HimCandidateRelation.Identity ->
                authority.families
                    .find { it.canonicalId == relation.parentCanonicalId }
                    ?.identities
                    ?.any {
                        it.identityName == candidate.candidateTerm ||
                                it.normalizedName == candidate.candidateTerm
                    } == true

            is HimCandidateRelation.Variant ->
                variantsFor(relation.scope).any {
                    it.variantName == candidate.candidateTerm ||
                            it.normalizedName == candidate.candidateTerm
                }

            is HimCandidateRelation.Alias ->
                aliasesFor(relation.equivalentEntity).any {
                    it.aliasName == candidate.candidateTerm ||
                            it.normalizedName == candidate.candidateTerm
                }

            HimCandidateRelation.CreateNewCanonical -> false
        }

    private fun variantsFor(reference: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference) =
        when (reference) {
            is de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference.Canonical ->
                authority.families.find { it.canonicalId == reference.canonicalId }?.variants.orEmpty()
            is de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference.Identity ->
                authority.families.find { it.canonicalId == reference.canonicalId }
                    ?.identities?.find { it.identityId == reference.identityId }?.variants.orEmpty()
        }

    private fun aliasesFor(reference: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference) =
        when (reference) {
            is de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference.Canonical ->
                authority.families.find { it.canonicalId == reference.canonicalId }?.aliases.orEmpty()
            is de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference.Identity ->
                authority.families.find { it.canonicalId == reference.canonicalId }
                    ?.identities?.find { it.identityId == reference.identityId }?.aliases.orEmpty()
        }
}

class HimGroundTruthCandidateGenerationOrchestrator(
    private val consolidator: HimGroundTruthCandidateConsolidator =
        HimGroundTruthCandidateConsolidator(),
    private val filter: HimGroundTruthCandidateFilter = HimGroundTruthCandidateFilter(),
) {

    fun assembleDryRun(
        request: HimGroundTruthCandidateGenerationRequest,
        semanticHypotheses: List<HimGroundTruthCandidate>,
        knownRelationDetector: HimKnownRelationDetector,
        terminalState: HimRetrievalTerminalState?,
    ): HimGroundTruthCandidateGenerationResult {
        val consolidated = consolidator.consolidate(semanticHypotheses)
        val filtered = filter.filter(consolidated)
        val (novel, known) = knownRelationDetector.detect(filtered.retained)
        return HimGroundTruthCandidateGenerationResult(
            candidates = novel,
            knownRelations = known,
            terminalState = terminalState,
            evidenceReferences = request.sourceEvidence.map { it.evidenceReference }.distinct(),
            diagnostics =
                HimGroundTruthCandidateDiagnostics(
                    inputCount = 1,
                    canonicalRetrievalCount = request.canonicalRetrievalContext.size,
                    sourceRetrievalCount = request.sourceEvidence.size,
                    candidateCount = novel.size,
                    highCount = novel.count {
                        it.candidateConfidence == HimCandidateConfidence.HIGH
                    },
                    mediumCount = novel.count {
                        it.candidateConfidence == HimCandidateConfidence.MEDIUM
                    },
                    lowDroppedCount = filtered.lowDropped,
                    noConfidenceDroppedCount = filtered.noConfidenceDropped,
                    knownRelationCount = known.size,
                    retrievalRoundsUsed = request.retrievalRound.value,
                    searchExhaustedCount =
                        if (terminalState == HimRetrievalTerminalState.SEARCH_EXHAUSTED) 1 else 0,
                ),
        )
    }
}
