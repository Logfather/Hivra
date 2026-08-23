package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource

object HimPerSourceMultiQueryEvidencePackingPolicyV1 {
    const val VERSION = "HIM_PER_SOURCE_MULTI_QUERY_EVIDENCE_PACKING_V1"
    const val MAX_INCLUDED_EVIDENCE_PER_SOURCE = 10
    const val ALGORITHM = "ROUND_ROBIN_BY_QUERY_ORDER"
}

enum class HimPerSourceEvidenceOmissionReason {
    PER_SOURCE_REQUEST_LIMIT,
}

data class HimEvidenceRetrievalOccurrence(
    val source: HimGroundTruthSource,
    val query: String,
    val queryOrder: Int,
    val sourceLocalRank: Int,
    val evidence: HimEvidenceSearchResult,
) {
    init {
        require(query.isNotBlank())
        require(queryOrder > 0)
        require(sourceLocalRank > 0)
        require(source == evidence.source)
        require(sourceLocalRank == evidence.retrievalRank)
    }
}

data class HimOmittedPerSourceEvidence(
    val evidence: HimEvidenceSearchResult,
    val reason: HimPerSourceEvidenceOmissionReason = HimPerSourceEvidenceOmissionReason.PER_SOURCE_REQUEST_LIMIT,
)

data class HimPerSourceEvidencePackingResult(
    val source: HimGroundTruthSource,
    val retrievedOccurrences: List<HimEvidenceRetrievalOccurrence>,
    val uniqueRetrievedEvidence: List<HimEvidenceSearchResult>,
    val includedEvidence: List<HimEvidenceSearchResult>,
    val omittedEvidence: List<HimOmittedPerSourceEvidence>,
    val packingPolicyVersion: String = HimPerSourceMultiQueryEvidencePackingPolicyV1.VERSION,
) {
    init {
        require(includedEvidence.size <= HimPerSourceMultiQueryEvidencePackingPolicyV1.MAX_INCLUDED_EVIDENCE_PER_SOURCE)
        require(retrievedOccurrences.all { it.source == source })
        require((uniqueRetrievedEvidence + includedEvidence + omittedEvidence.map { it.evidence }).all { it.source == source })
    }
}

data class HimMultiSourceEvidencePackingResult(
    val perSource: List<HimPerSourceEvidencePackingResult>,
) {
    val includedEvidence: List<HimEvidenceSearchResult> = perSource.flatMap { it.includedEvidence }
    val omittedEvidence: List<HimOmittedPerSourceEvidence> = perSource.flatMap { it.omittedEvidence }
}

/** Technical, score-independent packing between query-local retrieval and inference request construction. */
class HimPerSourceMultiQueryEvidencePacker {
    fun pack(
        priorIncludedEvidence: List<HimEvidenceSearchResult>,
        retrievedOccurrences: List<HimEvidenceRetrievalOccurrence>,
    ): HimMultiSourceEvidencePackingResult {
        val priorBySource = priorIncludedEvidence
            .distinctBy { it.source to it.sourceRecordReference.value }
            .groupBy { it.source }
        val occurrencesBySource = retrievedOccurrences.groupBy { it.source }
        val sources = HimGroundTruthSource.entries.filter { it in priorBySource || it in occurrencesBySource }
        return HimMultiSourceEvidencePackingResult(
            sources.map { source -> packSource(source, priorBySource[source].orEmpty(), occurrencesBySource[source].orEmpty()) },
        )
    }

    private fun packSource(
        source: HimGroundTruthSource,
        prior: List<HimEvidenceSearchResult>,
        occurrences: List<HimEvidenceRetrievalOccurrence>,
    ): HimPerSourceEvidencePackingResult {
        require(prior.size <= HimPerSourceMultiQueryEvidencePackingPolicyV1.MAX_INCLUDED_EVIDENCE_PER_SOURCE)
        val orderedOccurrences = occurrences.sortedWith(
            compareBy<HimEvidenceRetrievalOccurrence> { it.queryOrder }
                .thenBy { it.sourceLocalRank }
                .thenBy { it.evidence.sourceRecordReference.value },
        )
        val queryBuckets = orderedOccurrences.groupBy { it.queryOrder }.toSortedMap().values.toList()
        val traversal = buildList {
            val maximumDepth = queryBuckets.maxOfOrNull { it.size } ?: 0
            repeat(maximumDepth) { depth -> queryBuckets.forEach { bucket -> bucket.getOrNull(depth)?.let(::add) } }
        }
        val uniqueRetrieved = traversal.distinctBy { it.evidence.sourceRecordReference.value }.map { it.evidence }
        val included = prior.toMutableList()
        val includedKeys = included.mapTo(mutableSetOf()) { it.sourceRecordReference.value }
        val omitted = mutableListOf<HimOmittedPerSourceEvidence>()
        uniqueRetrieved.forEach { evidence ->
            if (evidence.sourceRecordReference.value in includedKeys) return@forEach
            if (included.size < HimPerSourceMultiQueryEvidencePackingPolicyV1.MAX_INCLUDED_EVIDENCE_PER_SOURCE) {
                included += evidence
                includedKeys += evidence.sourceRecordReference.value
            } else {
                omitted += HimOmittedPerSourceEvidence(evidence)
            }
        }
        return HimPerSourceEvidencePackingResult(source, orderedOccurrences, uniqueRetrieved, included, omitted)
    }
}
