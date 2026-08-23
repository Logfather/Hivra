package de.shopme.tools.knowledge.him.training.teacher

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticSourceArtifactIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceProjection
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexBuildState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexMetadata
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexValidator
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimExpectedEvidenceRetrievalIndex
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimProductionIndexFileIdentityReleaseContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchResult
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.security.MessageDigest
import java.util.TreeMap

data class HimTeacherPaidPilotRealProbeResult(
    val source: String,
    val probeKind: String,
    val evidenceReference: String,
    val evidenceRelationType: String,
    val projectionDigest: String,
    val returnedHitCount: Int,
    val loadedProjectionCount: Int,
    val validationResult: String,
)

class HimTeacherPaidPilotRealBindingProbeV1 {
    fun probe(
        indexFile: File,
        expectedSource: HimGroundTruthSource,
        expectedSourceArtifactSha256: String,
        expectedRows: Long,
        expectedLogicalIndexDigest: String,
        expectedIndexSha256: String,
    ): HimTeacherPaidPilotRealProbeResult {
        require(indexFile.isFile && indexFile.length() > 0)
        require(indexFile.length() == expectedIndexFileBytes(expectedSource))
        require(expectedIndexSha256.matches(Regex("[0-9a-f]{64}")))
        require(sha256(indexFile) == expectedIndexSha256)
        Class.forName("org.sqlite.JDBC")
        return connection(indexFile).use { connection ->
            require(userVersion(connection) == 1)
            val metadata = readMetadata(connection)
            require(metadata.source == expectedSource)
            require(metadata.sourceArtifactSha256.value == expectedSourceArtifactSha256)
            require(metadata.logicalContentSha256.value == expectedLogicalIndexDigest)
            require(metadata.indexedRecordCount == expectedRows && metadata.evidenceRecordCount == expectedRows && metadata.ftsRowCount == expectedRows)
            require(HimEvidenceRetrievalIndexValidator.runtimeEligibility(metadata, HimExpectedEvidenceRetrievalIndex(expectedSource, HimSha256(expectedSourceArtifactSha256))) is de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexEligibility.Ready)
            val tables = connection.createStatement().use { statement ->
                statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('index_metadata','evidence_records','evidence_search')").use { rows ->
                    buildSet { while (rows.next()) add(rows.getString(1)) }
                }
            }
            require(tables == setOf("index_metadata", "evidence_records", "evidence_search"))
            val record = connection.createStatement().use { statement ->
                statement.executeQuery("SELECT internal_record_key, source_record_reference, record_kind, evidence_projection_json FROM evidence_records ORDER BY internal_record_key LIMIT 1").use { rows ->
                    require(rows.next())
                    listOf(rows.getLong(1), rows.getString(2), rows.getString(3), rows.getString(4))
                }
            }
            val reference = HimEvidenceRecordReference.parse(expectedSource, record[1] as String)
            val kind = HimEvidenceRecordKind.valueOf(record[2] as String)
            require(kind.source == expectedSource)
            val projection = HimEvidenceProjection(record[3] as String)
            val result = HimEvidenceSearchResult(expectedSource, reference, kind, 1, projection)
            val evidenceReference = HimSemanticSourceArtifactIdentityV1.reference(result)
            require(evidenceReference.source == expectedSource.name)
            HimTeacherPaidPilotRealProbeResult(
                expectedSource.name,
                "INDEXED_PRIMARY_KEY_LIMIT_1",
                reference.value,
                kind.name,
                sha256(projection.deterministicJson.toByteArray(Charsets.UTF_8)),
                1,
                1,
                "PASS",
            )
        }
    }

    private fun connection(file: File): Connection = DriverManager.getConnection("jdbc:sqlite:file:${file.toPath().toAbsolutePath()}?mode=ro")

    private fun userVersion(connection: Connection): Int = connection.createStatement().use { statement -> statement.executeQuery("PRAGMA user_version").use { rows -> require(rows.next()); rows.getInt(1) } }

    private fun readMetadata(connection: Connection): HimEvidenceRetrievalIndexMetadata = connection.prepareStatement("SELECT schema_version, source, source_artifact_path, source_artifact_sha256, source_record_count, logical_record_counts_json, index_build_policy_version, evidence_projection_policy_version, indexed_record_count, evidence_record_count, fts_row_count, logical_content_sha256, build_state, sqlite_runtime_version, sqlite_file_sha256 FROM index_metadata WHERE singleton_id=1").use { statement ->
        statement.executeQuery().use { rows ->
            require(rows.next())
            val counts: Map<String, Long> = Gson().fromJson(rows.getString(6), object : TypeToken<Map<String, Long>>() {}.type)
            HimEvidenceRetrievalIndexMetadata(
                rows.getString(1), HimGroundTruthSource.valueOf(rows.getString(2)), rows.getString(3), HimSha256(rows.getString(4)), rows.getLong(5), TreeMap(counts), rows.getString(7), rows.getString(8), rows.getLong(9), rows.getLong(10), rows.getLong(11), HimSha256(rows.getString(12)), HimEvidenceRetrievalIndexBuildState.valueOf(rows.getString(13)), rows.getString(14), rows.getString(15)?.let(::HimSha256),
            )
        }
    }

    private fun expectedIndexFileBytes(source: HimGroundTruthSource): Long =
        HimProductionIndexFileIdentityReleaseContractV1.sources
            .first { it.source == source.name }
            .sqliteFileBytes

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").let { digest -> file.inputStream().buffered(1024 * 1024).use { input -> val buffer = ByteArray(1024 * 1024); while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) } }; digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } }
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
