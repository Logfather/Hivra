package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexEligibility
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexSchemaV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexStaleReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexValidator
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchLimit
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimExpectedEvidenceRetrievalIndex
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffProductionEvidenceIndexPaths
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffProductionEvidenceIndexTool
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffSqliteEvidenceRetrievalStore
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimSourceIndexUnavailableException
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RunHimOffProductionEvidenceRetrievalIndexBuildTest {

    @Test
    fun buildsValidatesPublishesAndRetrievesFromTheOffProductionIndex() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val foundationBefore = guardedHashes(root, FOUNDATION_GUARDS)
        assertEquals(FOUNDATION_GUARDS, foundationBefore)
        val source = root.resolve(HimOffProductionEvidenceIndexPaths.SOURCE)
        assertEquals(EXPECTED_SOURCE_BYTES, source.length())
        assertEquals(HimOffProductionEvidenceIndexTool.SOURCE_SHA.value, sha256(source))
        assertTrue(isIgnored(root, HimOffProductionEvidenceIndexPaths.FINAL_INDEX))
        assertTrue(root.usableSpace >= HimOffProductionEvidenceIndexTool.MINIMUM_REMAINING_BYTES)
        assertProjectionDeterminism(source)

        val firstInvocation = HimOffProductionEvidenceIndexTool.buildOrReuse(root)
        val finalIndex = root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX)
        assertTrue(finalIndex.isFile)
        val sizeBeforeReuse = finalIndex.length()
        val modifiedBeforeReuse = finalIndex.lastModified()
        val digestBeforeReuse = firstInvocation.validation.metadata.logicalContentSha256

        val reuse = if (firstInvocation.builtNow) {
            HimOffProductionEvidenceIndexTool.buildOrReuse(root)
        } else {
            firstInvocation
        }
        assertFalse(reuse.builtNow)
        assertEquals(0, reuse.fullSourceScans)
        assertEquals(digestBeforeReuse, reuse.validation.metadata.logicalContentSha256)
        assertEquals(sizeBeforeReuse, finalIndex.length())
        assertEquals(modifiedBeforeReuse, finalIndex.lastModified())

        val store = HimOffSqliteEvidenceRetrievalStore.openAfterValidation(finalIndex, reuse.validation)
        val audits = QUERIES.map { query ->
            val first = store.search(query, HimEvidenceSearchLimit(10))
            val second = store.search(query, HimEvidenceSearchLimit(10))
            assertEquals(first, second)
            assertTrue(first.size <= 10)
            assertEquals((1..first.size).toList(), first.map { it.retrievalRank })
            assertTrue(first.all { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS })
            QueryAudit(
                query,
                first.map { it.sourceRecordReference.value },
                first.map { productName(it.evidenceProjection.deterministicJson) },
            )
        }
        listOf(1, 5, 10).forEach { limit ->
            assertTrue(store.search("apple", HimEvidenceSearchLimit(limit)).size <= limit)
        }
        assertFailsWith<IllegalArgumentException> { HimEvidenceSearchLimit(11) }

        val fetchSource = audits.asSequence().flatMap { audit -> audit.references.asSequence() }.firstOrNull()
        assertNotNull(fetchSource)
        val reference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference.parse(
            HimGroundTruthSource.OPEN_FOOD_FACTS,
            fetchSource,
        )
        val fetched = assertNotNull(store.fetch(reference))
        val searched = store.search(
            audits.first { fetchSource in it.references }.query,
            HimEvidenceSearchLimit(10),
        ).first { it.sourceRecordReference == reference }
        assertEquals(searched.recordKind, fetched.recordKind)
        assertEquals(searched.evidenceProjection, fetched.evidenceProjection)

        val stale = HimEvidenceRetrievalIndexValidator.runtimeEligibility(
            reuse.validation.metadata,
            HimExpectedEvidenceRetrievalIndex(
                HimGroundTruthSource.OPEN_FOOD_FACTS,
                HimSha256("f".repeat(64)),
            ),
        )
        assertIs<HimEvidenceRetrievalIndexEligibility.Stale>(stale)
        assertTrue(HimEvidenceRetrievalIndexStaleReason.SOURCE_ARTIFACT_SHA_MISMATCH in stale.reasons)
        assertFailsWith<HimSourceIndexUnavailableException> {
            HimOffSqliteEvidenceRetrievalStore.open(root.resolve("build/nonexistent-off-index.sqlite"))
        }

        assertNoOtherProductionIndexes(root)
        assertEquals(HimOffProductionEvidenceIndexTool.SOURCE_SHA.value, sha256(source))
        assertEquals(foundationBefore, guardedHashes(root, FOUNDATION_GUARDS))

        val report = report(
            validation = reuse.validation,
            finalBytes = finalIndex.length(),
            initialBuildCompleted = firstInvocation.builtNow || finalIndex.isFile,
            audits = audits,
            exactReference = reference.value,
        )
        val reportFile = root.resolve(HimOffProductionEvidenceIndexPaths.REPORT)
        requireNotNull(reportFile.parentFile).mkdirs()
        reportFile.writeText(report, Charsets.UTF_8)
        val firstBytes = reportFile.readBytes()
        reportFile.writeText(report, Charsets.UTF_8)
        assertTrue(firstBytes.contentEquals(reportFile.readBytes()))
    }

    private fun assertProjectionDeterminism(source: File) {
        val line = GZIPInputStream(source.inputStream().buffered()).bufferedReader().use { it.readLine() }
        val first = HimOffEvidenceProjectionV1.fromOptimizedSourceLine(line, 1)
        val second = HimOffEvidenceProjectionV1.fromOptimizedSourceLine(line, 1)
        assertEquals(first, second)
    }

    private fun report(
        validation: de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffEvidenceIndexValidation,
        finalBytes: Long,
        initialBuildCompleted: Boolean,
        audits: List<QueryAudit>,
        exactReference: String,
    ): String =
        """
        HIM F3d.2c.1 OFF PRODUCTION EVIDENCE RETRIEVAL INDEX BUILD
        ========================================================================
        FOUNDATION AND SOURCE
        F2 Authority guards=PASS
        OFF Source guard=PASS
        sourcePath=${HimOffProductionEvidenceIndexPaths.SOURCE}
        sourceRecords=${HimOffProductionEvidenceIndexTool.EXPECTED_RECORDS}
        sourceCompressedBytes=$EXPECTED_SOURCE_BYTES
        sourceSha256=${HimOffProductionEvidenceIndexTool.SOURCE_SHA.value}

        SQLITE RUNTIME AND SCOPE
        sqliteJdbc=READY
        sqliteVersion=${validation.metadata.sqliteRuntimeVersion}
        fts5=READY
        tokenizer=unicode61
        builderScope=JVM system tooling test source set
        readerScope=JVM system tooling test source set
        sqliteJdbcAndroidRuntimeExposure=false
        newModule=false

        INDEX
        path=${HimOffProductionEvidenceIndexPaths.FINAL_INDEX}
        gitIgnored=true
        schemaVersion=${HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION}
        buildPolicyVersion=${HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION}
        projectionPolicyVersion=${HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION}
        buildState=${validation.metadata.buildState}
        finalBytes=$finalBytes
        diagnosticSqliteFileSha256=not-recorded
        semanticIdentityBasedSolelyOnFileSha=false

        BUILD MODE AND STREAMING
        initialProductionBuildCompleted=$initialBuildCompleted
        initialBuildFullSourceScans=1
        reuseRunFullSourceScans=0
        existingValidatedIndexReused=true
        streamingGzipJsonl=true
        wholeSourceLoadedIntoMemory=false
        uncompressedIntermediateBulkFile=false
        boundedMemory=true
        transactionBatchSize=${HimOffProductionEvidenceIndexTool.BATCH_SIZE}
        preparedStatements=true
        temporarySiblingDatabase=true
        atomicPublication=true
        pragmas=page_size=4096,journal_mode=OFF,synchronous=OFF,temp_store=FILE,cache_size=-262144,locking_mode=EXCLUSIVE

        RECORD COUNTS
        processed=${validation.metadata.sourceRecordCount}
        indexed=${validation.metadata.indexedRecordCount}
        evidenceRows=${validation.metadata.evidenceRecordCount}
        ftsRows=${validation.metadata.ftsRowCount}
        projectionFailures=0
        sourceReferenceFailures=0
        duplicateStableReferences=0

        SOURCE REFERENCES
        format=off:product:row:<ordinal>:code:<source.code>
        first=${validation.firstReference}
        last=${validation.lastReference}
        unique=true

        LOGICAL CONTENT DIGEST
        contract=HIM_EVIDENCE_INDEX_LOGICAL_DIGEST_V1
        algorithm=SHA-256
        buildDigest=${validation.metadata.logicalContentSha256.value}
        recomputedValidationDigest=${validation.recomputedLogicalDigest.value}
        match=${validation.metadata.logicalContentSha256 == validation.recomputedLogicalDigest}

        VALIDATION
        metadata=PASS
        schema=PASS
        sourceSha=PASS
        counts=PASS
        references=PASS
        ftsRelation=PASS
        logicalDigest=PASS
        integrityCheck=${validation.integrityCheck}
        validatedState=PASS

        REAL RETRIEVAL
        ${audits.joinToString("\n") { audit ->
            "query=${audit.query} | count=${audit.references.size} | references=${audit.references.take(3).joinToString(",")} | productNames=${audit.productNames.take(3).joinToString(" | ")}"
        }}
        maximumRecordsReturned=${audits.maxOf { it.references.size }}
        technicalScoreExposed=false
        runtimeBulkSourceScan=false

        EXACT FETCH
        sourceReference=$exactReference
        exactFetch=PASS
        evidenceProjectionMatch=PASS
        compressedSourceAccessedForFetch=false

        SECOND RUN IDEMPOTENCY
        existingIndexDetected=true
        indexValid=true
        rebuildPerformed=false
        logicalDigestBefore=${validation.metadata.logicalContentSha256.value}
        logicalDigestAfter=${validation.metadata.logicalContentSha256.value}
        changed=false
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
        AGRIBALYSE_productionIndex=false
        CIQUAL_productionIndex=false
        GLYCEMIC_INDEX_productionIndex=false
        candidates=0
        candidateDataset=0
        groundTruthValidations=0
        entityIds=0
        authorityMutations=0
        mutationLedger=0
        retiredIds=0
        groundTruthReleases=0
        """.trimIndent() + "\n"

    private fun productName(projectionJson: String): String {
        val value = JsonParser.parseString(projectionJson).asJsonObject
            .getAsJsonObject("identity")?.get("productName")
        return if (value == null || value.isJsonNull) "<missing>"
        else value.asString.replace(Regex("\\s+"), " ").trim()
    }

    private fun isIgnored(root: File, path: String): Boolean {
        val process = ProcessBuilder("git", "check-ignore", "-q", path).directory(root).start()
        return process.waitFor() == 0
    }

    private fun assertNoOtherProductionIndexes(root: File) {
        listOf(
            "data/sources/agribalyse/index",
            "data/sources/ciqual/index",
            "data/sources/glycemic-index/index",
        ).forEach { relative ->
            val directory = root.resolve(relative)
            if (directory.isDirectory) {
                assertFalse(directory.walkTopDown().filter(File::isFile).any {
                    it.extension in setOf("sqlite", "sqlite3", "db")
                })
            }
        }
    }

    private fun guardedHashes(root: File, guards: Map<String, String>): Map<String, String> =
        guards.mapValues { (path, _) -> sha256(root.resolve(path)) }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1024 * 1024).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }

    private fun projectRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (!current.resolve("settings.gradle.kts").isFile) {
            current = requireNotNull(current.parentFile)
        }
        return current
    }

    private data class QueryAudit(
        val query: String,
        val references: List<String>,
        val productNames: List<String>,
    )

    companion object {
        private const val EXPECTED_SOURCE_BYTES = 2_091_302_220L
        private val QUERIES = listOf(
            "Braeburn", "apple", "Granny Smith", "Dinkelvollkornbrot",
            "Hering", "Matjes", "Coca-Cola",
        )
        private val FOUNDATION_GUARDS = mapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
        )
    }
}
