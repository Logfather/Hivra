package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.util.SortedMap
import java.util.TreeMap

enum class HimEvidenceRetrievalIndexBuildState {
    BUILDING,
    VALIDATED,
}

enum class HimEvidenceRecordKind(
    val source: HimGroundTruthSource,
) {
    OFF_PRODUCT(HimGroundTruthSource.OPEN_FOOD_FACTS),
    AGRIBALYSE_RECORD(HimGroundTruthSource.AGRIBALYSE),
    CIQUAL_FOOD(HimGroundTruthSource.CIQUAL),
    CIQUAL_TAXONOMY(HimGroundTruthSource.CIQUAL),
    CIQUAL_CONSTITUENT(HimGroundTruthSource.CIQUAL),
    CIQUAL_SOURCE(HimGroundTruthSource.CIQUAL),
    GI_MEASUREMENT(HimGroundTruthSource.GLYCEMIC_INDEX),
    GI_MEAN_SUMMARY(HimGroundTruthSource.GLYCEMIC_INDEX),
    GI_CATEGORY_NOTE(HimGroundTruthSource.GLYCEMIC_INDEX),
    GI_FOOTNOTE(HimGroundTruthSource.GLYCEMIC_INDEX),
}

data class HimEvidenceRetrievalIndexMetadata(
    val schemaVersion: String,
    val source: HimGroundTruthSource,
    val sourceArtifactPath: String,
    val sourceArtifactSha256: HimSha256,
    val sourceRecordCount: Long,
    val logicalRecordCounts: SortedMap<String, Long>,
    val indexBuildPolicyVersion: String,
    val evidenceProjectionPolicyVersion: String,
    val indexedRecordCount: Long,
    val evidenceRecordCount: Long,
    val ftsRowCount: Long,
    val logicalContentSha256: HimSha256,
    val buildState: HimEvidenceRetrievalIndexBuildState,
    val sqliteRuntimeVersion: String?,
    val sqliteFileSha256: HimSha256? = null,
) {
    init {
        require(schemaVersion.isNotBlank())
        require(sourceArtifactPath == source.artifactPath)
        require(sourceRecordCount >= 0)
        require(logicalRecordCounts.keys.all(String::isNotBlank))
        require(logicalRecordCounts.values.all { it >= 0 })
        require(indexBuildPolicyVersion.isNotBlank())
        require(evidenceProjectionPolicyVersion.isNotBlank())
        require(indexedRecordCount >= 0)
        require(evidenceRecordCount >= 0)
        require(ftsRowCount >= 0)
        require(sqliteRuntimeVersion == null || sqliteRuntimeVersion.isNotBlank())
    }

    companion object {
        fun sortedLogicalCounts(values: Map<String, Long>): SortedMap<String, Long> =
            TreeMap(values)
    }
}

data class HimEvidenceProjection(
    val deterministicJson: String,
) {
    init {
        require(deterministicJson.isNotBlank())
        require(!deterministicJson.contains('\n'))
    }
}

data class HimEvidenceSearchText(
    val primaryName: String,
    val secondaryNames: String,
    val taxonomyText: String,
    val ingredientText: String,
    val contextText: String,
)

data class HimEvidenceRetrievalIndexRecord(
    val internalRecordKey: Long,
    val sourceRecordReference: HimEvidenceRecordReference,
    val recordKind: HimEvidenceRecordKind,
    val sourceNativeIdentifiersJson: String,
    val evidenceProjection: HimEvidenceProjection,
    val searchText: HimEvidenceSearchText,
) {
    init {
        require(internalRecordKey > 0)
        require(recordKind.source == sourceRecordReference.source)
        require(sourceNativeIdentifiersJson.isNotBlank())
        require(!sourceNativeIdentifiersJson.contains('\n'))
    }
}

data class HimEvidenceSearchLimit(
    val value: Int,
) {
    init {
        require(value in 1..MAX_RESULTS)
    }

    companion object {
        const val MAX_RESULTS = 10
    }
}

data class HimEvidenceSearchResult(
    val source: HimGroundTruthSource,
    val sourceRecordReference: HimEvidenceRecordReference,
    val recordKind: HimEvidenceRecordKind,
    val retrievalRank: Int,
    val evidenceProjection: HimEvidenceProjection,
) {
    init {
        require(source == sourceRecordReference.source)
        require(recordKind.source == source)
        require(retrievalRank in 1..HimEvidenceSearchLimit.MAX_RESULTS)
    }
}

data class HimEvidenceRetrievalIndexIdentity(
    val source: HimGroundTruthSource,
    val sourceArtifactSha256: HimSha256,
    val schemaVersion: String,
    val indexBuildPolicyVersion: String,
    val evidenceProjectionPolicyVersion: String,
    val logicalContentSha256: HimSha256,
)
