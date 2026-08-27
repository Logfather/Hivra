package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource

/**
 * The source projection boundary for the frozen P1 direct-evidence units.
 * Source SHA-256 binds the unchanged compressed source bytes; the index logical
 * digest binds the published indexed content; the projection policy binds the
 * allowed transformation; and RELEASED binds the approved index generation.
 * A source full-scan logical digest is deliberately not part of V1.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SOURCE_PROJECTION_V1"
    const val VERSION = "1"

    const val SOURCE_ARTIFACT_PATH =
        "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz"
    const val SOURCE_ARTIFACT_SHA256 =
        "63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236"
    const val INDEX_ARTIFACT_PATH =
        "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite"
    const val INDEX_ARTIFACT_SHA256 =
        "80c7da8c2b0a94ee0b12fb03e095f50429d2c0b300ce70dc22d0b5095aabf5df"
    const val INDEX_LOGICAL_DIGEST =
        "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743"
    const val INDEX_SCHEMA = "HIM_EVIDENCE_RETRIEVAL_INDEX_SCHEMA_V1"
    const val PROJECTION_POLICY = "HIM_EVIDENCE_PROJECTION_V1"
    const val INDEX_STATE = "VALIDATED"
    const val RELEASE_STATE = "RELEASED"
    const val INDEX_ARTIFACT_BYTES = 25_551_749_120L

    /** The source size is intentionally not frozen; zero means unknown here. */
    private const val UNKNOWN_SOURCE_ARTIFACT_BYTES = 0L

    val FROZEN_BINDINGS = listOf(
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionBindingV1(
            "off:product:row:431650:code:4002239680509",
            "a98f67c7aa8af729e27d402585df4c78d2a1a9c3f85e636dd7d257cba1810ebd",
            "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e",
            "ZuhV5V",
            HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION,
        ),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionBindingV1(
            "off:product:row:3272579:code:0061483010917",
            "ec4d7ccf39c1b9dbe184a0af90190cbc5d6a9e96ce3e3af121f5f59258eb5e13",
            "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7",
            "rVnyq7",
            HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION,
        ),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionBindingV1(
            "off:product:row:1551407:code:4013200552046",
            "6db7a77b0a3ff2471b8001b1644bc7a957efa52369df722899298c87d286ed71",
            "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70",
            "ZuhV5V",
            HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION,
        ),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionBindingV1(
            "off:product:row:3322623:code:2026088009283",
            "f4957e490b1714a1e48ca5d43ae762564603ac712842b3c2714b9f36bf2c41df",
            "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50",
            "rVnyq7",
            HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION,
        ),
    )

    fun resolve(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRequestV1,
    ): List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1> {
        validateRequest(request)
        return FROZEN_BINDINGS.map { binding ->
            val record = request.exactFetchPort.fetchExact(binding.sourceRecordReference)
                ?: fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.MISSING_SOURCE_RECORD)
            validateRecord(binding, record)
            val input = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementSourceProjectionInputV1(
                stableEntryId = binding.stableEntryId,
                reviewUnitId = binding.reviewUnitId,
                canonicalEntityId = binding.canonicalEntityId,
                source = HimGroundTruthSource.OPEN_FOOD_FACTS,
                recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
                artifact = indexArtifact(),
                sourceOriginArtifact = sourceArtifact(),
                recordReference = binding.sourceRecordReference,
                fields = record.fields.sortedBy { it.fieldReference },
                projectionLogicalDigest = "",
            )
            input.copy(
                projectionLogicalDigest =
                    HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1
                        .projectionLogicalDigest(input),
            )
        }
    }

    private fun validateRequest(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRequestV1,
    ) {
        val provenance = request.provenance
        if (unsafePath(provenance.sourceArtifactPath)) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.UNSAFE_ARTIFACT_PATH)
        }
        if (provenance.sourceArtifactPath != SOURCE_ARTIFACT_PATH) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_SOURCE_ARTIFACT_BINDING)
        }
        if (provenance.sourceArtifactSha256 != SOURCE_ARTIFACT_SHA256) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_SOURCE_SHA256)
        }
        if (unsafePath(provenance.indexArtifactPath)) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.UNSAFE_ARTIFACT_PATH)
        }
        if (provenance.indexArtifactPath != INDEX_ARTIFACT_PATH) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_INDEX_ARTIFACT_BINDING)
        }
        if (provenance.indexArtifactSha256 != INDEX_ARTIFACT_SHA256) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_INDEX_SHA256)
        }
        if (provenance.indexLogicalDigest != INDEX_LOGICAL_DIGEST) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_INDEX_LOGICAL_DIGEST)
        }
        if (provenance.indexSchema != INDEX_SCHEMA) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_INDEX_SCHEMA)
        }
        if (provenance.projectionPolicy != PROJECTION_POLICY) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_PROJECTION_POLICY)
        }
        if (provenance.indexState != INDEX_STATE) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_INDEX_STATE)
        }
        if (provenance.releaseState != RELEASE_STATE) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_RELEASE_STATE)
        }
        if (FROZEN_BINDINGS.map { it.sourceRecordReference }.distinct().size != FROZEN_BINDINGS.size) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.DUPLICATE_SOURCE_REFERENCE)
        }
        if (FROZEN_BINDINGS.any { it.source != HimGroundTruthSource.OPEN_FOOD_FACTS }) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_PILOT_SCOPE)
        }
    }

    private fun validateRecord(
        binding: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionBindingV1,
        record: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1,
    ) {
        if (record.sourceRecordReference != binding.sourceRecordReference) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.SOURCE_REFERENCE_MISMATCH)
        }
        if (record.source != HimGroundTruthSource.OPEN_FOOD_FACTS || record.recordKind != HimEvidenceRecordKind.OFF_PRODUCT) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_SOURCE_RECORD_KIND)
        }
        if (record.fields.isEmpty()) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_SOURCE_PROJECTION)
        }
        if (record.fields.map { it.fieldReference }.distinct().size != record.fields.size) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.DUPLICATE_FIELD_NAME)
        }
        record.fields.forEach { field ->
            when (field.validate()) {
                null -> Unit
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.FIELD_VALUE_TOO_LARGE ->
                    fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.FIELD_VALUE_TOO_LARGE)
                HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementFailureReasonV1.FIELD_VALUE_DIGEST_MISMATCH ->
                    fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.FIELD_VALUE_DIGEST_MISMATCH)
                else -> fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_FIELD_VALUE)
            }
        }
        val projectionBytes = record.sourceRecordReference.toByteArray(Charsets.UTF_8).size +
            record.fields.sumOf { field ->
                field.fieldReference.toByteArray(Charsets.UTF_8).size +
                    field.fullValue.toByteArray(Charsets.UTF_8).size +
                    field.fullValueSha256.length
            }
        if (projectionBytes > HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.MAX_PROJECTION_BYTES) {
            fail(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_SOURCE_PROJECTION)
        }
    }

    private fun unsafePath(path: String): Boolean =
        path.startsWith('/') || path.contains('\\') ||
            path.split('/').any { it.isBlank() || it == "." || it == ".." }

    private fun sourceArtifact() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1(
        relativePath = SOURCE_ARTIFACT_PATH,
        byteSize = UNKNOWN_SOURCE_ARTIFACT_BYTES,
        sha256 = SOURCE_ARTIFACT_SHA256,
        logicalDigest = null,
    )

    private fun indexArtifact() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1(
        relativePath = INDEX_ARTIFACT_PATH,
        byteSize = INDEX_ARTIFACT_BYTES,
        sha256 = INDEX_ARTIFACT_SHA256,
        logicalDigest = INDEX_LOGICAL_DIGEST,
    )

    private fun fail(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1,
    ): Nothing = throw HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailure(reason)
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionProvenanceV1(
    val sourceArtifactPath: String,
    val sourceArtifactSha256: String,
    val indexArtifactPath: String,
    val indexArtifactSha256: String,
    val indexLogicalDigest: String,
    val indexSchema: String,
    val projectionPolicy: String,
    val indexState: String,
    val releaseState: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1(
    val source: HimGroundTruthSource,
    val recordKind: HimEvidenceRecordKind,
    val sourceRecordReference: String,
    val fields: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1>,
)

fun interface HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionExactFetchPortV1 {
    fun fetchExact(sourceRecordReference: String): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1?
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRequestV1(
    val provenance: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionProvenanceV1,
    val exactFetchPort: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionExactFetchPortV1,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionBindingV1(
    val sourceRecordReference: String,
    val stableEntryId: String,
    val reviewUnitId: String,
    val canonicalEntityId: String,
    val position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
) {
    val source: HimGroundTruthSource = HimGroundTruthSource.OPEN_FOOD_FACTS
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1 {
    INVALID_CONTRACT_ID,
    INVALID_CONTRACT_VERSION,
    INVALID_PROVENANCE_BINDING,
    INVALID_SOURCE_ARTIFACT_BINDING,
    INVALID_SOURCE_SHA256,
    INVALID_INDEX_ARTIFACT_BINDING,
    INVALID_INDEX_SHA256,
    INVALID_INDEX_LOGICAL_DIGEST,
    INVALID_INDEX_SCHEMA,
    INVALID_INDEX_STATE,
    INVALID_PROJECTION_POLICY,
    INVALID_RELEASE_STATE,
    UNSAFE_ARTIFACT_PATH,
    INVALID_PILOT_SCOPE,
    DUPLICATE_SOURCE_REFERENCE,
    UNEXPECTED_SOURCE_REFERENCE,
    MISSING_SOURCE_RECORD,
    SOURCE_REFERENCE_MISMATCH,
    INVALID_SOURCE_RECORD_KIND,
    INVALID_SOURCE_PROJECTION,
    DUPLICATE_FIELD_NAME,
    INVALID_FIELD_VALUE,
    FIELD_VALUE_TOO_LARGE,
    FIELD_VALUE_DIGEST_MISMATCH,
    INVALID_EVIDENCE_KIND,
    INVALID_EVIDENCE_DIRECTNESS,
    INVALID_EVIDENCE_POSITION,
    OUTPUT_SCOPE_MISMATCH,
}

class HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailure(
    val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1,
) : IllegalArgumentException(reason.name)
