package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionExactFetchPortV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailure
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionProvenanceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1Test {
    @Test
    fun contractAndProvenanceAreFrozenWithoutSourceLogicalDigest() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DIRECT_EVIDENCE_SOURCE_PROJECTION_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.VERSION)
        val provenance = validProvenance()
        assertEquals("RELEASED", provenance.releaseState)
        assertEquals("VALIDATED", provenance.indexState)
        assertEquals(null, sourceInput().sourceOriginArtifact.logicalDigest)
        assertTrue(provenance.sourceArtifactPath.startsWith("data/"))
        assertTrue(provenance.indexArtifactPath.startsWith("data/"))
    }

    @Test
    fun validFrozenProvenanceIsAccepted() {
        val result = resolve()
        assertEquals(4, result.size)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.FROZEN_BINDINGS.map { it.reviewUnitId },
            result.map { it.reviewUnitId },
        )
    }

    @Test
    fun everyProvenanceComponentIsValidated() {
        val mutations = listOf<(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionProvenanceV1) -> HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionProvenanceV1>(
            { it.copy(sourceArtifactPath = "wrong.gz") },
            { it.copy(sourceArtifactSha256 = "0".repeat(64)) },
            { it.copy(indexArtifactPath = "wrong.sqlite") },
            { it.copy(indexArtifactSha256 = "0".repeat(64)) },
            { it.copy(indexLogicalDigest = "0".repeat(64)) },
            { it.copy(indexSchema = "WRONG") },
            { it.copy(projectionPolicy = "WRONG") },
            { it.copy(indexState = "BUILDING") },
            { it.copy(releaseState = "DRAFT") },
        )
        mutations.forEach { mutate ->
            assertFailsWith<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailure> {
                resolve(provenance = mutate(validProvenance()))
            }
        }
        assertReason(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.UNSAFE_ARTIFACT_PATH) {
            resolve(provenance = validProvenance().copy(indexArtifactPath = "/tmp/index.sqlite"))
        }
        val failure = assertFailsWith<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailure> {
            resolve(provenance = validProvenance().copy(sourceArtifactPath = "../source.gz"))
        }
        assertFalse(failure.message.orEmpty().contains("source.gz"))
        assertFalse(failure.message.orEmpty().contains("/tmp"))
    }

    @Test
    fun fetchesExactlyFourFrozenReferencesInMissionOrder() {
        val port = RecordingPort(records())
        val result = resolve(port)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.FROZEN_BINDINGS.map { it.sourceRecordReference },
            port.calls,
        )
        assertEquals(4, port.calls.size)
        assertEquals(port.calls.distinct().size, port.calls.size)
        assertEquals(port.calls, result.map { it.recordReference })
    }

    @Test
    fun fetchedFieldsAreCompleteSortedAndByteExact() {
        val input = resolve().first()
        assertEquals(listOf("identity.productName", "identity.productNameEnglish", "taxonomy.categories"), input.fields.map { it.fieldReference })
        assertEquals("Artischocken Herzen 🌿", input.fields[0].fullValue)
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1.sha256("Artischocken Herzen 🌿"), input.fields[0].fullValueSha256)
        assertEquals(
            input.projectionLogicalDigest,
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementRuntimeV1.projectionLogicalDigest(input),
        )
    }

    @Test
    fun outputUsesFrozenReviewUnitTargetsAndPositions() {
        val result = resolve()
        assertEquals(listOf("ZuhV5V", "rVnyq7", "ZuhV5V", "rVnyq7"), result.map { it.canonicalEntityId })
        assertEquals(
            listOf(
                "a98f67c7aa8af729e27d402585df4c78d2a1a9c3f85e636dd7d257cba1810ebd",
                "ec4d7ccf39c1b9dbe184a0af90190cbc5d6a9e96ce3e3af121f5f59258eb5e13",
                "6db7a77b0a3ff2471b8001b1644bc7a957efa52369df722899298c87d286ed71",
                "f4957e490b1714a1e48ca5d43ae762564603ac712842b3c2714b9f36bf2c41df",
            ),
            result.map { it.stableEntryId },
        )
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION,
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.FROZEN_BINDINGS.map { it.position },
        )
        assertTrue(result.all { it.recordKind == HimEvidenceRecordKind.OFF_PRODUCT })
        assertTrue(result.all { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS })
    }

    @Test
    fun repeatedExecutionIsDeterministicAndIndependentOfFixtureOrder() {
        val first = resolve(records = records().reversed())
        val second = resolve(records = records())
        assertEquals(first, second)
        assertEquals(first.map { it.projectionLogicalDigest }, second.map { it.projectionLogicalDigest })
        assertNotEquals(first.first().recordReference, first[1].recordReference)
    }

    @Test
    fun missingAndUnexpectedReferencesFailClosed() {
        assertReason(MISSING_SOURCE_RECORD) { resolve(records = records().dropLast(1)) }
        val wrong = records().toMutableList().also { it[0] = it[0].copy(sourceRecordReference = "off:product:row:999:code:wrong") }
        assertReason(SOURCE_REFERENCE_MISMATCH) {
            resolve(port = RecordingPort(wrong, returnFirstRegardlessOfRequestedReference = true), records = wrong)
        }
    }

    @Test
    fun wrongRecordKindAndSourceFailClosed() {
        val wrongKind = records().toMutableList().also { it[0] = it[0].copy(recordKind = HimEvidenceRecordKind.CIQUAL_FOOD, source = HimGroundTruthSource.CIQUAL) }
        assertReason(INVALID_SOURCE_RECORD_KIND) { resolve(records = wrongKind) }
        val wrongSource = records().toMutableList().also { it[0] = it[0].copy(source = HimGroundTruthSource.AGRIBALYSE) }
        assertReason(INVALID_SOURCE_RECORD_KIND) { resolve(records = wrongSource) }
    }

    @Test
    fun invalidFieldsFailClosed() {
        val duplicate = records().toMutableList().also { it[0] = it[0].copy(fields = it[0].fields + it[0].fields.first()) }
        assertReason(DUPLICATE_FIELD_NAME) { resolve(records = duplicate) }
        val digest = records().toMutableList().also { it[0] = it[0].copy(fields = listOf(field("identity.productName", "changed" to "0".repeat(64)))) }
        assertReason(FIELD_VALUE_DIGEST_MISMATCH) { resolve(records = digest) }
        val oversized = records().toMutableList().also { it[0] = it[0].copy(fields = listOf(field("identity.productName", "x".repeat(16 * 1024 + 1)))) }
        assertReason(FIELD_VALUE_TOO_LARGE) { resolve(records = oversized) }
        val empty = records().toMutableList().also { it[0] = it[0].copy(fields = emptyList()) }
        assertReason(INVALID_SOURCE_PROJECTION) { resolve(records = empty) }
    }

    @Test
    fun unicodeAndFullValuesRemainIntact() {
        val result = resolve().first()
        assertTrue(result.fields.any { it.fullValue.contains("🌿") })
        assertFalse(result.fields.any { it.fullValue.contains('\u0000') })
    }

    @Test
    fun onlySourceProjectionInputIsProducedAndNoDecisionSemanticsExist() {
        val result = resolve()
        assertTrue(result.all { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS })
        assertTrue(result.all { it.recordKind == HimEvidenceRecordKind.OFF_PRODUCT })
        val names = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1::class.java.declaredMethods.map { it.name }
        assertTrue(names.none { it.contains("search", true) || it.contains("scan", true) || it.contains("sqlite", true) })
        assertTrue(names.none { it.contains("decision", true) || it.contains("reviewer", true) || it.contains("gold", true) })
    }

    private fun resolve(
        port: RecordingPort = RecordingPort(records()),
        provenance: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionProvenanceV1 = validProvenance(),
        records: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1> = records(),
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.resolve(
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRequestV1(provenance, port.also { it.replace(records) }),
    )

    private fun assertReason(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1,
        block: () -> Unit,
    ) {
        assertEquals(reason, assertFailsWith<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailure>(block = block).reason)
    }

    private fun validProvenance() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionProvenanceV1(
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.SOURCE_ARTIFACT_PATH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.SOURCE_ARTIFACT_SHA256,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_ARTIFACT_PATH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_ARTIFACT_SHA256,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_LOGICAL_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_SCHEMA,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.PROJECTION_POLICY,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.INDEX_STATE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.RELEASE_STATE,
    )

    private fun records() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionV1.FROZEN_BINDINGS.mapIndexed { index, binding ->
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1(
            HimGroundTruthSource.OPEN_FOOD_FACTS,
            HimEvidenceRecordKind.OFF_PRODUCT,
            binding.sourceRecordReference,
            listOf(
                field("taxonomy.categories", "de:artischocken-herzen"),
                field("identity.productNameEnglish", if (index % 2 == 0) "Artichoke Hearts" else "Brie double crème"),
                field("identity.productName", if (index % 2 == 0) "Artischocken Herzen 🌿" else "Brie double crème"),
            ),
        )
    }

    private fun sourceInput() = resolve().first()

    private fun field(reference: String, value: String) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1(
        reference,
        value,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1.sha256(value),
    )

    private fun field(reference: String, value: Pair<String, String>) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1(
        reference,
        value.first,
        value.second,
    )

    private class RecordingPort(
        private var values: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1>,
        private val returnFirstRegardlessOfRequestedReference: Boolean = false,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionExactFetchPortV1 {
        val calls = mutableListOf<String>()

        fun replace(records: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1>) {
            values = records
            calls.clear()
        }

        override fun fetchExact(sourceRecordReference: String): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionRecordV1? {
            calls += sourceRecordReference
            return if (returnFirstRegardlessOfRequestedReference && calls.size == 1) {
                values.firstOrNull()
            } else {
                values.singleOrNull { it.sourceRecordReference == sourceRecordReference }
            }
        }
    }

    private companion object {
        val MISSING_SOURCE_RECORD = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.MISSING_SOURCE_RECORD
        val SOURCE_REFERENCE_MISMATCH = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.SOURCE_REFERENCE_MISMATCH
        val INVALID_SOURCE_RECORD_KIND = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_SOURCE_RECORD_KIND
        val DUPLICATE_FIELD_NAME = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.DUPLICATE_FIELD_NAME
        val FIELD_VALUE_DIGEST_MISMATCH = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.FIELD_VALUE_DIGEST_MISMATCH
        val FIELD_VALUE_TOO_LARGE = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.FIELD_VALUE_TOO_LARGE
        val INVALID_SOURCE_PROJECTION = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSourceProjectionFailureReasonV1.INVALID_SOURCE_PROJECTION
    }
}
