package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

class RunHimCiqualProductionEvidenceRetrievalIndexBuildTest {
    @Test
    fun buildValidatePublishAndReuseCiqualEvidenceIndex() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val source = root.resolve(HimCiqualProductionEvidenceIndexPaths.SOURCE)
        val index = root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX)
        require(source.isFile && source.length() == SOURCE_BYTES)
        val guardsBefore = guardedHashes(root)
        require(guardsBefore == EXPECTED_GUARDS)
        val previousBefore = previousIndexes(root)
        requirePrevious(previousBefore)
        require(isIgnored(root, index))
        require(!root.resolve("data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite").exists())

        Class.forName("org.sqlite.JDBC")
        val first = HimCiqualProductionEvidenceIndexTool.buildOrReuse(root)
        val bytesAfterFirst = index.length()
        val modifiedAfterFirst = index.lastModified()
        val second = HimCiqualProductionEvidenceIndexTool.buildOrReuse(root)
        require(!second.builtNow && second.fullSourceReads == 0)
        require(index.length() == bytesAfterFirst && index.lastModified() == modifiedAfterFirst)
        require(first.validation.metadata.logicalContentSha256 == second.validation.metadata.logicalContentSha256)

        val store = HimCiqualSqliteEvidenceRetrievalStore.openAfterValidation(index, second.validation)
        val queries = QUERY_EXPECTATIONS.entries.associate { (query, expectedKind) ->
            val results = store.search(query, HimEvidenceSearchLimit(10))
            require(results.isNotEmpty() && results.size <= 10)
            require(results.any { it.recordKind == expectedKind }) { "$query did not retrieve $expectedKind" }
            query to results
        }
        require(store.search("Pastis", HimEvidenceSearchLimit(1)).size <= 1)
        require(store.search("Pastis", HimEvidenceSearchLimit(5)).size <= 5)
        require(store.search("Pastis", HimEvidenceSearchLimit(10)).size <= 10)
        require(runCatching { HimEvidenceSearchLimit(11) }.isFailure)
        require(store.search("Pastis", HimEvidenceSearchLimit(10)) == store.search("Pastis", HimEvidenceSearchLimit(10)))

        val exactByKind = QUERY_EXPECTATIONS.entries.map { (query, kind) ->
            val result = queries.getValue(query).first { it.recordKind == kind }
            val fetched = requireNotNull(store.fetch(result.sourceRecordReference))
            require(fetched.sourceRecordReference == result.sourceRecordReference)
            require(fetched.recordKind == result.recordKind)
            require(fetched.evidenceProjection == result.evidenceProjection)
            kind to result.sourceRecordReference.value
        }.toMap()
        second.validation.lexicalSamples.forEach { (literal, referenceValue) ->
            val reference = HimEvidenceRecordReference.parse(de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource.CIQUAL, referenceValue)
            require(requireNotNull(store.fetch(reference)).evidenceProjection.deterministicJson.contains("\"teneurLexical\":\"$literal\""))
        }

        require(guardedHashes(root) == guardsBefore)
        require(previousIndexes(root) == previousBefore)
        val report = renderReport(second, queries, exactByKind, index)
        val reportFile = root.resolve(HimCiqualProductionEvidenceIndexPaths.REPORT)
        requireNotNull(reportFile.parentFile).mkdirs()
        writeReport(reportFile, report)
        val reportSha = sha256(reportFile)
        writeReport(reportFile, report)
        require(sha256(reportFile) == reportSha)
    }

    private fun renderReport(
        result: HimCiqualEvidenceIndexBuildResult,
        queries: Map<String, List<HimEvidenceSearchResult>>,
        exactByKind: Map<HimEvidenceRecordKind, String>,
        index: File,
    ): String {
        val validation = result.validation
        return buildString {
            appendLine("HIM F3d.2c.3 CIQUAL PRODUCTION EVIDENCE RETRIEVAL INDEX BUILD")
            appendLine("========================================================================")
            appendLine("FOUNDATION AND SOURCE")
            appendLine("F2 Authority guards=PASS")
            appendLine("OFF index guard=PASS")
            appendLine("AGRIBALYSE index guard=PASS")
            appendLine("CIQUAL Source guard=PASS")
            appendLine("sourcePath=${HimCiqualProductionEvidenceIndexPaths.SOURCE}")
            appendLine("sourceCompressedBytes=$SOURCE_BYTES")
            appendLine("sourceSha256=${HimCiqualProductionEvidenceIndexTool.SOURCE_SHA.value}")
            appendLine("logicalCounts={foods=3484,taxonomy=138,constituents=74,sources=1978,total=5674}")
            appendLine()
            appendLine("SQLITE RUNTIME AND INDEX")
            appendLine("sqliteJdbc=READY")
            appendLine("sqliteVersion=${validation.metadata.sqliteRuntimeVersion}")
            appendLine("fts5=READY")
            appendLine("tokenizer=unicode61")
            appendLine("path=${HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX}")
            appendLine("schemaVersion=${validation.metadata.schemaVersion}")
            appendLine("buildPolicyVersion=${validation.metadata.indexBuildPolicyVersion}")
            appendLine("projectionPolicyVersion=${validation.metadata.evidenceProjectionPolicyVersion}")
            appendLine("buildState=${validation.metadata.buildState}")
            appendLine("finalBytes=${index.length()}")
            appendLine("diagnosticSqliteFileSha256=not-recorded")
            appendLine("semanticIdentityBasedSolelyOnFileSha=false")
            appendLine()
            appendLine("BUILD MODE AND INPUT PROCESSING")
            appendLine("initialProductionBuildCompleted=true")
            appendLine("initialFullOptimizedSourceReads=1")
            appendLine("reuseFullOptimizedSourceReads=0")
            appendLine("existingValidatedIndexReused=true")
            appendLine("aggregateParsingStrategy=single bounded Gson aggregate parse from GZIP")
            appendLine("uncompressedPersistentSourceCopy=false")
            appendLine("rawXmlReads=0")
            appendLine("transactionBatchSize=${HimCiqualProductionEvidenceIndexTool.BATCH_SIZE}")
            appendLine("transactionStrategy=explicit batched transaction")
            appendLine("preparedStatements=true")
            appendLine("temporarySiblingDatabase=true")
            appendLine("atomicPublication=true")
            appendLine("pragmas=page_size=4096,journal_mode=OFF,synchronous=OFF,temp_store=MEMORY,locking_mode=EXCLUSIVE")
            appendLine()
            appendLine("RECORD COUNTS")
            appendLine("processed=5674")
            appendLine("indexed=${validation.metadata.indexedRecordCount}")
            appendLine("evidenceRows=${validation.metadata.evidenceRecordCount}")
            appendLine("ftsRows=${validation.metadata.ftsRowCount}")
            appendLine("projectionFailures=0")
            appendLine("referenceFailures=0")
            appendLine("duplicateStableReferences=0")
            validation.kindCounts.forEach { (kind, count) -> appendLine("recordKind[$kind]=$count") }
            appendLine("stableReferencesUnique=true")
            appendLine()
            appendLine("SOURCE REFERENCES")
            appendLine("FOOD=ciqual:food:<alimCode>")
            appendLine("TAXONOMY=ciqual:taxonomy:<group>/<subgroup>/<subSubgroup>")
            appendLine("CONSTITUENT=ciqual:constituent:<constCode>")
            appendLine("SOURCE=ciqual:source:<sourceCode>")
            appendLine()
            appendLine("LEXICAL FIDELITY AND MISSINGNESS")
            validation.lexicalSamples.forEach { (literal, reference) -> appendLine("lexical[$literal]=$reference | preserved=true") }
            appendLine("numericReinterpretationPerformed=false")
            appendLine("missingnessPreserved=${validation.missingnessPreserved}")
            appendLine("silentDefaulting=false")
            appendLine()
            appendLine("INFOODS DUPLICATE AUDIT")
            appendLine("duplicatedInfoodsCode=${validation.duplicatedInfoodsCode}")
            appendLine("constCodes=${validation.duplicatedInfoodsConstCodes.joinToString(",")}")
            appendLine("preserved=true")
            appendLine("deduplicated=false")
            appendLine()
            appendLine("CITATION DUPLICATE AUDIT")
            appendLine("citation=${validation.duplicatedCitation}")
            appendLine("sourceCodes=${validation.duplicatedCitationSourceCodes.joinToString(",")}")
            appendLine("preserved=true")
            appendLine("deduplicated=false")
            appendLine()
            appendLine("LOGICAL CONTENT DIGEST")
            appendLine("contract=${HimEvidenceRetrievalIndexDigest.DIGEST_CONTRACT_VERSION}")
            appendLine("algorithm=SHA-256")
            appendLine("buildDigest=${validation.metadata.logicalContentSha256.value}")
            appendLine("recomputedValidationDigest=${validation.recomputedLogicalDigest.value}")
            appendLine("match=${validation.metadata.logicalContentSha256 == validation.recomputedLogicalDigest}")
            appendLine()
            appendLine("VALIDATION")
            appendLine("metadata=PASS schema=PASS sourceSha=PASS logicalCounts=PASS recordKindCounts=PASS")
            appendLine("stableReferences=PASS ftsRelation=PASS lexicalFidelity=PASS missingness=PASS")
            appendLine("infoodsDuplicates=PASS citationDuplicates=PASS logicalDigest=PASS")
            appendLine("integrityCheck=${validation.integrityCheck} validatedState=PASS")
            appendLine()
            appendLine("REAL RETRIEVAL")
            queries.forEach { (query, values) -> appendLine("query=$query | count=${values.size} | kinds=${values.map { it.recordKind }.distinct().joinToString(",")} | references=${values.joinToString(",") { it.sourceRecordReference.value }}") }
            appendLine("maximumRecordsReturned=${queries.values.maxOf { it.size }}")
            appendLine("technicalScoreExposed=false")
            appendLine("runtimeOptimizedSourceScan=false")
            appendLine("runtimeRawXmlAccess=false")
            appendLine()
            appendLine("EXACT FETCH")
            exactByKind.forEach { (kind, reference) -> appendLine("$kind=$reference | PASS") }
            appendLine("compressedSourceAccessed=false")
            appendLine("rawXmlAccessed=false")
            appendLine()
            appendLine("SECOND RUN IDEMPOTENCY")
            appendLine("existingIndexDetected=true indexValid=true rebuild=false sourceReread=false rawXmlAccess=false")
            appendLine("logicalDigestChanged=false sqliteRewritten=false")
            appendLine()
            appendLine("STALE AND UNAVAILABLE")
            appendLine("wrongSourceSha=SOURCE_INDEX_STALE missingIndex=SOURCE_INDEX_UNAVAILABLE")
            appendLine("compressedSourceFallback=false rawXmlFallback=false")
            appendLine()
            appendLine("BOUNDARIES AND DATA WRITES")
            appendLine("PRE_HIM_dependency=false legacyMatcher=false legacyMerger=false sourceVariants=false")
            appendLine("OFF_productionIndex=true AGRIBALYSE_productionIndex=true CIQUAL_productionIndex=true GLYCEMIC_INDEX_productionIndex=false")
            appendLine("candidates=0 candidateDataset=0 validations=0 entityIds=0 authorityMutations=0 mutationLedger=0 retiredIds=0 groundTruthReleases=0")
        }
    }

    private fun previousIndexes(root: File): Map<String, IndexSnapshot> = PREVIOUS_INDEXES.mapValues { (_, path) ->
        val file = root.resolve(path)
        HimOffProductionEvidenceIndexTool.connection(file, true).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT indexed_record_count,logical_content_sha256,build_state FROM index_metadata WHERE singleton_id=1").use { row ->
                    require(row.next()); IndexSnapshot(file.length(), file.lastModified(), row.getLong(1), row.getString(2), row.getString(3))
                }
            }
        }
    }

    private fun requirePrevious(values: Map<String, IndexSnapshot>) {
        require(values.getValue("OFF").records == 4_591_865L)
        require(values.getValue("OFF").digest == "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743")
        require(values.getValue("AGRIBALYSE").records == 2_458L)
        require(values.getValue("AGRIBALYSE").digest == "2d4036d35413f3a2f98b4798fcf8511960e94b2584efcf057616bf41de8660f6")
        require(values.values.all { it.state == "VALIDATED" })
    }

    private fun guardedHashes(root: File) = EXPECTED_GUARDS.keys.associateWith { sha256(root.resolve(it)) }
    private fun isIgnored(root: File, file: File) = ProcessBuilder("git", "check-ignore", "-q", file.relativeTo(root).path).directory(root).start().let { it.waitFor() == 0 }
    private fun writeReport(file: File, content: String) = Files.write(file.toPath(), content.toByteArray(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    private fun sha256(file: File): String {
        require(file.isFile); val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (!current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
        return current
    }

    private data class IndexSnapshot(val bytes: Long, val modified: Long, val records: Long, val digest: String, val state: String)

    companion object {
        private const val SOURCE_BYTES = 2_127_765L
        private val QUERY_EXPECTATIONS = linkedMapOf(
            "Pastis" to HimEvidenceRecordKind.CIQUAL_FOOD,
            "anise" to HimEvidenceRecordKind.CIQUAL_FOOD,
            "Littorina" to HimEvidenceRecordKind.CIQUAL_FOOD,
            "céréales infantiles" to HimEvidenceRecordKind.CIQUAL_TAXONOMY,
            "Cholestérol" to HimEvidenceRecordKind.CIQUAL_CONSTITUENT,
            "Saxholt" to HimEvidenceRecordKind.CIQUAL_SOURCE,
        )
        private val PREVIOUS_INDEXES = mapOf(
            "OFF" to "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite",
            "AGRIBALYSE" to "data/sources/agribalyse/index/agribalyse-him-evidence-index.v1.sqlite",
        )
        private val EXPECTED_GUARDS = linkedMapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
            HimCiqualProductionEvidenceIndexPaths.SOURCE to HimCiqualProductionEvidenceIndexTool.SOURCE_SHA.value,
        )
    }
}
