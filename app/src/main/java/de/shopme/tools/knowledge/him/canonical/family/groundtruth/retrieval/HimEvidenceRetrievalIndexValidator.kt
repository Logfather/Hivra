package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource

sealed interface HimEvidenceRetrievalIndexEligibility {
    data object Unavailable : HimEvidenceRetrievalIndexEligibility
    data object Ready : HimEvidenceRetrievalIndexEligibility
    data class Stale(val reasons: Set<HimEvidenceRetrievalIndexStaleReason>) : HimEvidenceRetrievalIndexEligibility
    data class Corrupt(val reasons: Set<HimEvidenceRetrievalIndexCorruptReason>) : HimEvidenceRetrievalIndexEligibility
}

enum class HimEvidenceRetrievalIndexStaleReason {
    SOURCE_ARTIFACT_SHA_MISMATCH,
    SCHEMA_VERSION_MISMATCH,
    BUILD_POLICY_VERSION_MISMATCH,
    PROJECTION_POLICY_VERSION_MISMATCH,
}

enum class HimEvidenceRetrievalIndexCorruptReason {
    BUILD_NOT_VALIDATED,
    SOURCE_MISMATCH,
    SOURCE_PATH_MISMATCH,
    COUNT_MISMATCH,
}

data class HimExpectedEvidenceRetrievalIndex(
    val source: HimGroundTruthSource,
    val sourceArtifactSha256: HimSha256,
    val schemaVersion: String = HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION,
    val indexBuildPolicyVersion: String = HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION,
    val evidenceProjectionPolicyVersion: String = HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION,
)

object HimEvidenceRetrievalIndexValidator {
    fun runtimeEligibility(
        metadata: HimEvidenceRetrievalIndexMetadata,
        expected: HimExpectedEvidenceRetrievalIndex,
    ): HimEvidenceRetrievalIndexEligibility {
        val corrupt = buildSet {
            if (metadata.buildState != HimEvidenceRetrievalIndexBuildState.VALIDATED) {
                add(HimEvidenceRetrievalIndexCorruptReason.BUILD_NOT_VALIDATED)
            }
            if (metadata.source != expected.source) add(HimEvidenceRetrievalIndexCorruptReason.SOURCE_MISMATCH)
            if (metadata.sourceArtifactPath != expected.source.artifactPath) {
                add(HimEvidenceRetrievalIndexCorruptReason.SOURCE_PATH_MISMATCH)
            }
            if (metadata.indexedRecordCount != metadata.evidenceRecordCount ||
                metadata.evidenceRecordCount != metadata.ftsRowCount
            ) {
                add(HimEvidenceRetrievalIndexCorruptReason.COUNT_MISMATCH)
            }
        }
        if (corrupt.isNotEmpty()) return HimEvidenceRetrievalIndexEligibility.Corrupt(corrupt)

        val stale = buildSet {
            if (metadata.sourceArtifactSha256 != expected.sourceArtifactSha256) {
                add(HimEvidenceRetrievalIndexStaleReason.SOURCE_ARTIFACT_SHA_MISMATCH)
            }
            if (metadata.schemaVersion != expected.schemaVersion) {
                add(HimEvidenceRetrievalIndexStaleReason.SCHEMA_VERSION_MISMATCH)
            }
            if (metadata.indexBuildPolicyVersion != expected.indexBuildPolicyVersion) {
                add(HimEvidenceRetrievalIndexStaleReason.BUILD_POLICY_VERSION_MISMATCH)
            }
            if (metadata.evidenceProjectionPolicyVersion != expected.evidenceProjectionPolicyVersion) {
                add(HimEvidenceRetrievalIndexStaleReason.PROJECTION_POLICY_VERSION_MISMATCH)
            }
        }
        return if (stale.isEmpty()) HimEvidenceRetrievalIndexEligibility.Ready
        else HimEvidenceRetrievalIndexEligibility.Stale(stale)
    }
}
