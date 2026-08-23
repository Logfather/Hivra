package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.security.MessageDigest

object HimEvidenceRetrievalIndexDigest {

    class Accumulator internal constructor(
        private val digest: MessageDigest,
    ) {
        private var lastInternalRecordKey = 0L
        private var finished = false

        fun add(record: HimEvidenceRetrievalIndexRecord) {
            check(!finished)
            require(record.internalRecordKey > lastInternalRecordKey)
            lastInternalRecordKey = record.internalRecordKey
            addField(digest, "internal-record-key", record.internalRecordKey.toString())
            addField(digest, "source-record-reference", record.sourceRecordReference.value)
            addField(digest, "record-kind", record.recordKind.name)
            addField(digest, "source-native-identifiers-json", record.sourceNativeIdentifiersJson)
            addField(digest, "primary-name", record.searchText.primaryName)
            addField(digest, "secondary-names", record.searchText.secondaryNames)
            addField(digest, "taxonomy-text", record.searchText.taxonomyText)
            addField(digest, "ingredient-text", record.searchText.ingredientText)
            addField(digest, "context-text", record.searchText.contextText)
            addField(digest, "evidence-projection-json", record.evidenceProjection.deterministicJson)
        }

        fun finish(): HimSha256 {
            check(!finished)
            finished = true
            return HimSha256(digest.digest().joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            })
        }
    }

    fun newAccumulator(
        schemaVersion: String,
        source: HimGroundTruthSource,
        sourceArtifactSha256: HimSha256,
        logicalRecordCounts: Map<String, Long>,
        indexBuildPolicyVersion: String,
        evidenceProjectionPolicyVersion: String,
    ): Accumulator {
        require(schemaVersion.isNotBlank())
        require(indexBuildPolicyVersion.isNotBlank())
        require(evidenceProjectionPolicyVersion.isNotBlank())
        val digest = MessageDigest.getInstance("SHA-256")
        addField(digest, "digest-contract", DIGEST_CONTRACT_VERSION)
        addField(digest, "schema-version", schemaVersion)
        addField(digest, "source", source.name)
        addField(digest, "source-sha256", sourceArtifactSha256.value)
        addField(digest, "build-policy-version", indexBuildPolicyVersion)
        addField(digest, "projection-policy-version", evidenceProjectionPolicyVersion)
        logicalRecordCounts.toSortedMap().forEach { (name, count) ->
            addField(digest, "logical-count-name", name)
            addField(digest, "logical-count-value", count.toString())
        }
        return Accumulator(digest)
    }

    fun compute(
        schemaVersion: String,
        source: HimGroundTruthSource,
        sourceArtifactSha256: HimSha256,
        logicalRecordCounts: Map<String, Long>,
        indexBuildPolicyVersion: String,
        evidenceProjectionPolicyVersion: String,
        records: List<HimEvidenceRetrievalIndexRecord>,
    ): HimSha256 {
        require(schemaVersion.isNotBlank())
        require(indexBuildPolicyVersion.isNotBlank())
        require(evidenceProjectionPolicyVersion.isNotBlank())
        require(records.map { it.internalRecordKey }.distinct().size == records.size)
        require(records.map { it.sourceRecordReference.value }.distinct().size == records.size)
        require(records.all { it.sourceRecordReference.source == source })

        val accumulator = newAccumulator(
            schemaVersion,
            source,
            sourceArtifactSha256,
            logicalRecordCounts,
            indexBuildPolicyVersion,
            evidenceProjectionPolicyVersion,
        )
        records.sortedBy { it.internalRecordKey }.forEach { record ->
            accumulator.add(record)
        }
        return accumulator.finish()
    }

    private fun addField(digest: MessageDigest, label: String, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        digest.update(label.toByteArray(Charsets.UTF_8))
        digest.update(0)
        digest.update(bytes.size.toString().toByteArray(Charsets.US_ASCII))
        digest.update(':'.code.toByte())
        digest.update(bytes)
        digest.update('\n'.code.toByte())
    }

    const val DIGEST_CONTRACT_VERSION = "HIM_EVIDENCE_INDEX_LOGICAL_DIGEST_V1"
}
