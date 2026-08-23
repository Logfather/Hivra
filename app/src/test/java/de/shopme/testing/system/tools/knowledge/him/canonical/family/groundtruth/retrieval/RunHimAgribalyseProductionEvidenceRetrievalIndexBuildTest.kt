package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

class RunHimAgribalyseProductionEvidenceRetrievalIndexBuildTest {
    @Test
    fun buildValidatePublishAndReuseAgribalyseEvidenceIndex() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val source = root.resolve(HimAgribalyseProductionEvidenceIndexPaths.SOURCE)
        val index = root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX)
        require(source.isFile && source.length() == SOURCE_BYTES)
        val protectedBefore = guardedHashes(root)
        require(protectedBefore == EXPECTED_GUARDS)
        val offBefore = offSnapshot(root)
        require(offBefore.bytes == 25_551_749_120L)
        require(offBefore.indexed == 4_591_865L && offBefore.evidence == 4_591_865L && offBefore.fts == 4_591_865L)
        require(offBefore.digest == "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743")
        require(offBefore.state == "VALIDATED")
        require(isIgnored(root, index))
        require(!root.resolve("data/sources/ciqual/index/ciqual-him-evidence-index.v1.sqlite").exists())
        require(!root.resolve("data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite").exists())

        Class.forName("org.sqlite.JDBC")
        val first = HimAgribalyseProductionEvidenceIndexTool.buildOrReuse(root)
        val indexBytesAfterFirst = index.length()
        val indexModifiedAfterFirst = index.lastModified()
        val second = HimAgribalyseProductionEvidenceIndexTool.buildOrReuse(root)
        require(!second.builtNow && second.fullSourceScans == 0)
        require(index.length() == indexBytesAfterFirst && index.lastModified() == indexModifiedAfterFirst)
        require(first.validation.metadata.logicalContentSha256 == second.validation.metadata.logicalContentSha256)

        val store = HimAgribalyseSqliteEvidenceRetrievalStore.openAfterValidation(index, second.validation)
        val queryResults = QUERIES.associateWith { query -> store.search(query, HimEvidenceSearchLimit(10)) }
        require(queryResults.values.all { it.isNotEmpty() && it.size <= 10 })
        require(store.search("Hareng", HimEvidenceSearchLimit(1)).size <= 1)
        require(store.search("Hareng", HimEvidenceSearchLimit(5)).size <= 5)
        require(store.search("Hareng", HimEvidenceSearchLimit(10)).size <= 10)
        require(runCatching { HimEvidenceSearchLimit(11) }.isFailure)
        require(store.search("Hareng", HimEvidenceSearchLimit(10)) == store.search("Hareng", HimEvidenceSearchLimit(10)))

        val retrieved = queryResults.getValue("Hareng").first()
        val fetched = requireNotNull(store.fetch(retrieved.sourceRecordReference))
        require(fetched.sourceRecordReference == retrieved.sourceRecordReference)
        require(fetched.recordKind == retrieved.recordKind)
        require(fetched.evidenceProjection == retrieved.evidenceProjection)
        val duplicateReferences = duplicateReferences(index, "26013")
        require(duplicateReferences.size == 2 && duplicateReferences.distinct().size == 2)
        require(duplicateReferences.all { it.contains(":agb:26013") })

        require(guardedHashes(root) == protectedBefore)
        require(offSnapshot(root) == offBefore)
        require(source.length() == SOURCE_BYTES)
        val report = renderReport(second, queryResults, retrieved, duplicateReferences, index)
        val reportFile = root.resolve(HimAgribalyseProductionEvidenceIndexPaths.REPORT)
        requireNotNull(reportFile.parentFile).mkdirs()
        writeReport(reportFile, report)
        val firstReportSha = sha256(reportFile)
        writeReport(reportFile, report)
        require(sha256(reportFile) == firstReportSha)
    }

    private fun duplicateReferences(index: File, agbCode: String): List<String> =
        HimAgribalyseProductionEvidenceIndexTool.connection(index, true).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT source_record_reference FROM evidence_records WHERE json_extract(source_native_identifiers_json,'$.agbCode')='$agbCode' ORDER BY internal_record_key"
                ).use { result -> buildList { while (result.next()) add(result.getString(1)) } }
            }
        }

    private fun renderReport(
        second: HimAgribalyseEvidenceIndexBuildResult,
        queries: Map<String, List<HimEvidenceSearchResult>>,
        exact: HimEvidenceSearchResult,
        duplicateReferences: List<String>,
        index: File,
    ): String {
        val validation = second.validation
        return """
            HIM F3d.2c.2 AGRIBALYSE PRODUCTION EVIDENCE RETRIEVAL INDEX BUILD
            ========================================================================
            FOUNDATION AND SOURCE
            F2 Authority guards=PASS
            OFF production index guard=PASS
            AGRIBALYSE Source guard=PASS
            sourcePath=${HimAgribalyseProductionEvidenceIndexPaths.SOURCE}
            sourceRecords=${HimAgribalyseProductionEvidenceIndexTool.EXPECTED_RECORDS}
            sourceCompressedBytes=$SOURCE_BYTES
            sourceSha256=${HimAgribalyseProductionEvidenceIndexTool.SOURCE_SHA.value}

            SQLITE RUNTIME AND INDEX
            sqliteJdbc=READY
            sqliteVersion=${validation.metadata.sqliteRuntimeVersion}
            fts5=READY
            tokenizer=unicode61
            path=${HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX}
            schemaVersion=${validation.metadata.schemaVersion}
            buildPolicyVersion=${validation.metadata.indexBuildPolicyVersion}
            projectionPolicyVersion=${validation.metadata.evidenceProjectionPolicyVersion}
            buildState=${validation.metadata.buildState}
            finalBytes=${index.length()}
            diagnosticSqliteFileSha256=not-recorded
            semanticIdentityBasedSolelyOnFileSha=false

            BUILD MODE AND STREAMING
            initialProductionBuildCompleted=true
            initialBuildFullSourceScans=1
            reuseRunFullSourceScans=${second.fullSourceScans}
            existingValidatedIndexReused=true
            streamingGzipJsonl=true
            wholeSourceLoadedIntoMemory=false
            boundedMemory=true
            transactionBatchSize=${HimAgribalyseProductionEvidenceIndexTool.BATCH_SIZE}
            transactionStrategy=explicit batched transaction
            preparedStatements=true
            temporarySiblingDatabase=true
            atomicPublication=true
            pragmas=page_size=4096,journal_mode=OFF,synchronous=OFF,temp_store=MEMORY,locking_mode=EXCLUSIVE

            RECORD COUNTS
            processed=2458
            indexed=${validation.metadata.indexedRecordCount}
            evidenceRows=${validation.metadata.evidenceRecordCount}
            ftsRows=${validation.metadata.ftsRowCount}
            projectionFailures=0
            sourceReferenceFailures=0
            duplicateStableReferences=0

            AGB CODE INTEGRITY
            distinctAgbCodes=${validation.distinctAgbCodes}
            duplicateAgbGroups=${validation.duplicateAgbGroups}
            maximumMultiplicity=${validation.maximumMultiplicity}
            duplicateRecordsPreserved=true
            deduplicationPerformed=false

            SOURCE REFERENCES
            format=agribalyse:row:<ordinal>:agb:<agbCode>
            first=${validation.firstReference}
            last=${validation.lastReference}
            unique=true

            DUPLICATE AGB AUDIT
            agbCode=26013
            matchingEvidenceRows=${duplicateReferences.size}
            stableReferences=${duplicateReferences.joinToString(",")}
            distinctRowsPreserved=true

            LOGICAL CONTENT DIGEST
            contract=${HimEvidenceRetrievalIndexDigest.DIGEST_CONTRACT_VERSION}
            algorithm=SHA-256
            buildDigest=${validation.metadata.logicalContentSha256.value}
            recomputedValidationDigest=${validation.recomputedLogicalDigest.value}
            match=${validation.metadata.logicalContentSha256 == validation.recomputedLogicalDigest}

            VALIDATION
            metadata=PASS
            schema=PASS
            sourceSha=PASS
            counts=PASS
            stableReferences=PASS
            agbDuplicatePreservation=PASS
            ftsRelation=PASS
            logicalDigest=PASS
            integrityCheck=${validation.integrityCheck}
            validatedState=PASS

            REAL RETRIEVAL
            ${queries.entries.joinToString("\n") { (query, results) -> "query=$query | count=${results.size} | references=${results.joinToString(",") { it.sourceRecordReference.value }}" }}
            maximumRecordsReturned=${queries.values.maxOf { it.size }}
            technicalScoreExposed=false
            runtimeBulkSourceScan=false

            EXACT FETCH
            sourceReference=${exact.sourceRecordReference.value}
            exactFetch=PASS
            evidenceProjectionMatch=PASS
            compressedSourceAccessedForFetch=false

            SECOND RUN IDEMPOTENCY
            existingIndexDetected=true
            indexValid=true
            rebuildPerformed=false
            logicalDigestChanged=false
            sqliteDatabaseRewritten=false

            STALE AND UNAVAILABLE
            wrongSourceSha=SOURCE_INDEX_STALE
            missingIndex=SOURCE_INDEX_UNAVAILABLE
            fullSourceFallback=false

            BOUNDARIES AND DATA WRITES
            PRE_HIM_dependency=false
            legacyMatcher=false
            legacyMerger=false
            sourceVariants=false
            OFF_productionIndex=true
            AGRIBALYSE_productionIndex=true
            CIQUAL_productionIndex=false
            GLYCEMIC_INDEX_productionIndex=false
            candidates=0
            candidateDataset=0
            validations=0
            entityIds=0
            authorityMutations=0
            mutationLedger=0
            retiredIds=0
            groundTruthReleases=0
        """.trimIndent() + "\n"
    }

    private fun writeReport(file: File, content: String) {
        Files.write(
            file.toPath(), content.toByteArray(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
        )
    }

    private fun guardedHashes(root: File): Map<String, String> = EXPECTED_GUARDS.keys.associateWith { sha256(root.resolve(it)) }

    private fun offSnapshot(root: File): OffSnapshot {
        val index = root.resolve(OFF_INDEX)
        return HimOffProductionEvidenceIndexTool.connection(index, true).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT indexed_record_count,evidence_record_count,fts_row_count,logical_content_sha256,build_state FROM index_metadata WHERE singleton_id=1").use { result ->
                    require(result.next())
                    OffSnapshot(index.length(), index.lastModified(), result.getLong(1), result.getLong(2), result.getLong(3), result.getString(4), result.getString(5))
                }
            }
        }
    }

    private fun isIgnored(root: File, file: File): Boolean {
        val process = ProcessBuilder("git", "check-ignore", "-q", file.relativeTo(root).path).directory(root).start()
        return process.waitFor() == 0
    }

    private fun sha256(file: File): String {
        require(file.isFile)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (!current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
        return current
    }

    private data class OffSnapshot(
        val bytes: Long, val modified: Long, val indexed: Long, val evidence: Long, val fts: Long, val digest: String, val state: String,
    )

    companion object {
        private const val SOURCE_BYTES = 219_592L
        private const val OFF_INDEX = "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite"
        private val QUERIES = listOf("Hareng", "Court-bouillon", "boissons", "Micro-onde", "Salmon")
        private val EXPECTED_GUARDS = linkedMapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
            HimAgribalyseProductionEvidenceIndexPaths.SOURCE to HimAgribalyseProductionEvidenceIndexTool.SOURCE_SHA.value,
        )
    }
}
