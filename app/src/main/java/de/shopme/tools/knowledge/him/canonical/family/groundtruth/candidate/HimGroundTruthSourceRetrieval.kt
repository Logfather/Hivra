package de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.io.File

enum class HimGroundTruthSource(
    val artifactPath: String,
) {
    OPEN_FOOD_FACTS(
        "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz"
    ),
    AGRIBALYSE(
        "data/sources/agribalyse/optimized/agribalyse-him-final-source.jsonl.gz"
    ),
    CIQUAL(
        "data/sources/ciqual/optimized/ciqual-him-final-source.json.gz"
    ),
    GLYCEMIC_INDEX(
        "data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz"
    ),
    ;

    fun artifact(projectRoot: File): File = projectRoot.resolve(artifactPath)
}

data class HimSourceRetrievalRequest(
    val source: HimGroundTruthSource,
    val sourceArtifactSha256: HimSha256,
    val semanticQuery: String,
    val maxResults: Int = MAX_EVIDENCE_RECORDS,
) {
    init {
        require(semanticQuery.isNotBlank())
        require(maxResults in 1..MAX_EVIDENCE_RECORDS)
    }

    companion object {
        const val MAX_EVIDENCE_RECORDS = 10
    }
}

data class HimSourceEvidenceRecord(
    val evidenceReference: HimEvidenceReference,
    val sourceArtifactPath: String,
    val retrievalRank: Int,
) {
    init {
        require(sourceArtifactPath.isNotBlank())
        require(retrievalRank in 1..HimSourceRetrievalRequest.MAX_EVIDENCE_RECORDS)
    }
}

fun interface HimGroundTruthSourceRetriever {
    fun retrieve(request: HimSourceRetrievalRequest): List<HimSourceEvidenceRecord>
}

class HimBoundedGroundTruthSourceRetriever(
    private val delegate: HimGroundTruthSourceRetriever,
) : HimGroundTruthSourceRetriever {

    override fun retrieve(request: HimSourceRetrievalRequest): List<HimSourceEvidenceRecord> {
        val records = delegate.retrieve(request)
        require(records.size <= request.maxResults)
        require(records.map { it.retrievalRank } == (1..records.size).toList())
        require(records.all { it.sourceArtifactPath == request.source.artifactPath })
        require(records.all {
            it.evidenceReference.source == request.source.name &&
                    it.evidenceReference.sourceArtifactSha256 == request.sourceArtifactSha256
        })
        return records
    }
}
