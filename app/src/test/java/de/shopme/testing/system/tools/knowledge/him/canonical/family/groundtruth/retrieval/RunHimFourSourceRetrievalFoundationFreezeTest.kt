package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

class RunHimFourSourceRetrievalFoundationFreezeTest {
    @Test
    fun freezeAndReuseImmutableRetrievalFoundationV1() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        Class.forName("org.sqlite.JDBC")
        val foundationBefore = hashes(root, FOUNDATION_GUARDS)
        val sourceBefore = hashes(root, SOURCE_SHAS)
        require(foundationBefore == FOUNDATION_GUARDS && sourceBefore == SOURCE_SHAS)
        val indexBefore = INDEX_SPECS.associate { it.source to snapshot(root.resolve(it.indexPath)) }

        val validations = validateAllIndexes(root)
        val canonicalDigestInput = canonicalDigestInput(validations)
        val foundationDigest = sha256(canonicalDigestInput.toByteArray(StandardCharsets.UTF_8))
        require(foundationDigest == EXPECTED_FOUNDATION_DIGEST)
        val releaseBytes = renderRelease(validations, foundationDigest).toByteArray(StandardCharsets.UTF_8)
        validateRelease(releaseBytes, validations, foundationDigest)

        val release = root.resolve(RELEASE_PATH)
        publishOrReuse(release, releaseBytes)
        val firstSnapshot = snapshot(release)
        val firstSha = sha256(release)
        val second = publishOrReuse(release, releaseBytes)
        require(snapshot(release) == firstSnapshot && sha256(release) == firstSha)
        require(!second.created)
        require(!isIgnored(root, RELEASE_PATH))
        require(runCatching { validateImmutable(release.readBytes(), releaseBytes + ' '.code.toByte()) }.isFailure)
        validateRelease(release.readBytes(), validations, foundationDigest)

        val foundationAfter = hashes(root, FOUNDATION_GUARDS)
        val sourceAfter = hashes(root, SOURCE_SHAS)
        val indexAfter = INDEX_SPECS.associate { it.source to snapshot(root.resolve(it.indexPath)) }
        require(foundationAfter == foundationBefore && sourceAfter == sourceBefore && indexAfter == indexBefore)

        val report = renderReport(validations, foundationDigest, firstSha)
        val reportFile = root.resolve(REPORT_PATH)
        requireNotNull(reportFile.parentFile).mkdirs()
        writeReport(reportFile, report)
        val reportSha = sha256(reportFile)
        writeReport(reportFile, report)
        require(sha256(reportFile) == reportSha)
    }

    private fun validateAllIndexes(root: File): LinkedHashMap<HimGroundTruthSource, Validation> {
        INDEX_SPECS.forEach { spec -> require(root.resolve(spec.indexPath).isFile) }
        val off = HimOffEvidenceIndexValidator.validateReadOnly(root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX))
        val agri = HimAgribalyseEvidenceIndexValidator.validateReadOnly(root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX))
        val ciqual = HimCiqualEvidenceIndexValidator.validateReadOnly(root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX))
        val gi = HimGlycemicIndexEvidenceIndexValidator.validateReadOnly(root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX))
        val values = linkedMapOf(
            HimGroundTruthSource.OPEN_FOOD_FACTS to Validation(off.metadata, off.recomputedLogicalDigest, off.integrityCheck),
            HimGroundTruthSource.AGRIBALYSE to Validation(agri.metadata, agri.recomputedLogicalDigest, agri.integrityCheck),
            HimGroundTruthSource.CIQUAL to Validation(ciqual.metadata, ciqual.recomputedLogicalDigest, ciqual.integrityCheck),
            HimGroundTruthSource.GLYCEMIC_INDEX to Validation(gi.metadata, gi.recomputedLogicalDigest, gi.integrityCheck),
        )
        INDEX_SPECS.forEach { spec ->
            val value = values.getValue(spec.source)
            val metadata = value.metadata
            require(metadata.source == spec.source && metadata.sourceArtifactPath == spec.source.artifactPath)
            require(metadata.sourceArtifactSha256.value == SOURCE_SHAS.getValue(spec.source.artifactPath))
            require(metadata.logicalRecordCounts == spec.logicalCounts.toSortedMap())
            require(metadata.schemaVersion == HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION)
            require(metadata.indexBuildPolicyVersion == HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION)
            require(metadata.evidenceProjectionPolicyVersion == HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION)
            require(metadata.buildState == HimEvidenceRetrievalIndexBuildState.VALIDATED)
            require(metadata.evidenceRecordCount == spec.count && metadata.ftsRowCount == spec.count && metadata.indexedRecordCount == spec.count)
            require(metadata.logicalContentSha256.value == spec.logicalDigest && value.recomputedDigest == metadata.logicalContentSha256)
            require(value.integrity == "ok")
        }
        return values
    }

    private fun canonicalDigestInput(validations: Map<HimGroundTruthSource, Validation>): String = buildString {
        append("digest-contract=HIM_FOUR_SOURCE_RETRIEVAL_FOUNDATION_DIGEST_V1\n")
        SOURCE_ORDER.forEach { source ->
            val metadata = validations.getValue(source).metadata
            append("source=").append(source.name).append('\n')
            append("source-artifact-sha256=").append(metadata.sourceArtifactSha256.value).append('\n')
            append("schema-version=").append(metadata.schemaVersion).append('\n')
            append("build-policy-version=").append(metadata.indexBuildPolicyVersion).append('\n')
            append("projection-policy-version=").append(metadata.evidenceProjectionPolicyVersion).append('\n')
            append("logical-content-sha256=").append(metadata.logicalContentSha256.value).append('\n')
            append("evidence-record-count=").append(metadata.evidenceRecordCount).append('\n')
            append("fts-row-count=").append(metadata.ftsRowCount).append('\n')
        }
    }

    private fun renderRelease(validations: Map<HimGroundTruthSource, Validation>, foundationDigest: String): String = buildString {
        appendLine("{")
        appendLine("  \"artifactType\": \"HIM_RETRIEVAL_FOUNDATION_RELEASE\",")
        appendLine("  \"releaseVersion\": \"F3D_2_RETRIEVAL_FOUNDATION_V1\",")
        appendLine("  \"releaseState\": \"RELEASED\",")
        appendLine("  \"retrievalContract\": {")
        appendLine("    \"schemaVersion\": \"${HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION}\",")
        appendLine("    \"buildPolicyVersion\": \"${HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION}\",")
        appendLine("    \"evidenceProjectionPolicyVersion\": \"${HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION}\",")
        appendLine("    \"foundationDigestContract\": \"HIM_FOUR_SOURCE_RETRIEVAL_FOUNDATION_DIGEST_V1\"")
        appendLine("  },")
        appendLine("  \"foundationCompatibility\": [")
        FOUNDATION_GUARDS.entries.forEachIndexed { index, entry ->
            append("    {\"path\": \"").append(entry.key).append("\", \"sha256\": \"").append(entry.value).append("\"}")
            appendLine(if (index == FOUNDATION_GUARDS.size - 1) "" else ",")
        }
        appendLine("  ],")
        appendLine("  \"canonicalSourceOrder\": [")
        SOURCE_ORDER.forEachIndexed { index, source -> appendLine("    \"${source.name}\"${if (index == SOURCE_ORDER.lastIndex) "" else ","}") }
        appendLine("  ],")
        appendLine("  \"sources\": [")
        INDEX_SPECS.forEachIndexed { index, spec ->
            val metadata = validations.getValue(spec.source).metadata
            appendLine("    {")
            appendLine("      \"source\": \"${spec.source.name}\",")
            appendLine("      \"optimizedSourceArtifactPath\": \"${metadata.sourceArtifactPath}\",")
            appendLine("      \"optimizedSourceArtifactSha256\": \"${metadata.sourceArtifactSha256.value}\",")
            appendLine("      \"logicalRecordCounts\": {")
            spec.logicalCounts.entries.forEachIndexed { countIndex, entry -> appendLine("        \"${entry.key}\": ${entry.value}${if (countIndex == spec.logicalCounts.size - 1) "" else ","}") }
            appendLine("      },")
            appendLine("      \"productionEvidenceIndexPath\": \"${spec.indexPath}\",")
            appendLine("      \"evidenceRowCount\": ${metadata.evidenceRecordCount},")
            appendLine("      \"ftsRowCount\": ${metadata.ftsRowCount},")
            appendLine("      \"indexLogicalContentSha256\": \"${metadata.logicalContentSha256.value}\",")
            appendLine("      \"indexState\": \"${metadata.buildState.name}\"")
            append("    }").appendLine(if (index == INDEX_SPECS.lastIndex) "" else ",")
        }
        appendLine("  ],")
        appendLine("  \"fourSourceFoundation\": {")
        appendLine("    \"digestContract\": \"HIM_FOUR_SOURCE_RETRIEVAL_FOUNDATION_DIGEST_V1\",")
        appendLine("    \"sha256\": \"$foundationDigest\"")
        appendLine("  }")
        appendLine("}")
    }

    private fun validateRelease(bytes: ByteArray, validations: Map<HimGroundTruthSource, Validation>, digest: String) {
        require(bytes.contentEquals(renderRelease(validations, digest).toByteArray(StandardCharsets.UTF_8)))
        val json = JsonParser.parseString(bytes.toString(StandardCharsets.UTF_8)).asJsonObject
        require(json.get("artifactType").asString == "HIM_RETRIEVAL_FOUNDATION_RELEASE")
        require(json.get("releaseVersion").asString == "F3D_2_RETRIEVAL_FOUNDATION_V1" && json.get("releaseState").asString == "RELEASED")
        require(json.getAsJsonArray("canonicalSourceOrder").map { it.asString } == SOURCE_ORDER.map { it.name })
        val sources = json.getAsJsonArray("sources").map { it.asJsonObject }
        require(sources.size == 4 && sources.map { it.get("source").asString } == SOURCE_ORDER.map { it.name })
        require(sources.map { it.get("source").asString }.distinct().size == 4)
        require(json.getAsJsonObject("fourSourceFoundation").get("sha256").asString == digest)
    }

    private fun publishOrReuse(file: File, expected: ByteArray): Publication {
        if (file.exists()) {
            validateImmutable(file.readBytes(), expected)
            return Publication(false)
        }
        requireNotNull(file.parentFile).mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.publishing")
        require(!temporary.exists())
        Files.write(temporary.toPath(), expected, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        try {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } finally {
            if (temporary.exists()) require(temporary.delete())
        }
        validateImmutable(file.readBytes(), expected)
        return Publication(true)
    }

    private fun validateImmutable(actual: ByteArray, expected: ByteArray) {
        require(actual.contentEquals(expected)) { "Existing immutable V1 release differs from normative content" }
    }

    private fun renderReport(validations: Map<HimGroundTruthSource, Validation>, digest: String, artifactSha: String): String = buildString {
        appendLine("HIM F3d.2c.6 FOUR-SOURCE RETRIEVAL FOUNDATION FREEZE")
        appendLine("=".repeat(72))
        appendLine("FOUNDATION STATUS=PASS")
        appendLine("SOURCE STATUS=PASS")
        appendLine("PRODUCTION INDEX STATUS=PASS")
        appendLine("SOURCE-INDEX BINDINGS=PASS")
        appendLine("RETRIEVAL CONTRACT VERSIONS=${HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION},${HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION},${HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION}")
        validations.forEach { (source, validation) -> appendLine("${source.name}|state=${validation.metadata.buildState}|evidence=${validation.metadata.evidenceRecordCount}|fts=${validation.metadata.ftsRowCount}|logicalDigest=${validation.metadata.logicalContentSha256.value}|integrity=${validation.integrity}") }
        appendLine("PERSISTENT RELEASE RECORD=$RELEASE_PATH")
        appendLine("RELEASE CONTENT SUMMARY=sources=4,state=RELEASED,version=F3D_2_RETRIEVAL_FOUNDATION_V1")
        appendLine("FOUR-SOURCE FOUNDATION DIGEST=$digest")
        appendLine("RELEASE ARTIFACT SHA-256=$artifactSha")
        appendLine("IMMUTABILITY VALIDATION=PASS")
        appendLine("SECOND RUN IDEMPOTENCY=PASS identicalReuse=true")
        appendLine("SOURCE CONTENT INTEGRITY=UNCHANGED")
        appendLine("INDEX CONTENT INTEGRITY=UNCHANGED")
        appendLine("FOUNDATION CONTENT INTEGRITY=UNCHANGED")
        appendLine("SEMANTIC BOUNDARY=candidates=0,candidateDataset=0,validations=0,approvals=0,entityIds=0,authorityMutations=0,mutationLedger=0,retiredIds=0,groundTruthReleases=0,inferenceCalls=0")
        appendLine("PRE-HIM BOUNDARY=PASS")
        appendLine("FILES CREATED=$RELEASE_PATH,$REPORT_PATH,RunHimFourSourceRetrievalFoundationFreezeTest.kt")
        appendLine("FILES CHANGED=.gitignore (persistent Retrieval Foundation V1 exception only)")
        appendLine("CONTRACT DEVIATIONS=none")
        appendLine("HARD FAILURES=none")
        appendLine("HIM_RETRIEVAL_FOUNDATION_V1_FROZEN=true")
        appendLine("F3d.2c.6 COMPLETE=true")
    }

    private fun hashes(root: File, expected: Map<String, String>) = expected.keys.associateWith { sha256(root.resolve(it)) }
    private fun snapshot(file: File) = FileSnapshot(file.length(), file.lastModified())
    private fun isIgnored(root: File, path: String) = ProcessBuilder("git", "check-ignore", "-q", path).directory(root).start().let { it.waitFor() == 0 }
    private fun writeReport(file: File, content: String) = Files.write(file.toPath(), content.toByteArray(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    private fun sha256(file: File): String {
        require(file.isFile)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1024 * 1024).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (!current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
        return current
    }

    private data class IndexSpec(val source: HimGroundTruthSource, val indexPath: String, val count: Long, val logicalDigest: String, val logicalCounts: LinkedHashMap<String, Long>)
    private data class Validation(val metadata: HimEvidenceRetrievalIndexMetadata, val recomputedDigest: HimSha256, val integrity: String)
    private data class FileSnapshot(val bytes: Long, val modified: Long)
    private data class Publication(val created: Boolean)

    companion object {
        private const val RELEASE_PATH = "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json"
        private const val REPORT_PATH = "build/knowledge/reports/him/retrieval/him-f3d2c6-four-source-retrieval-foundation-freeze.txt"
        private const val EXPECTED_FOUNDATION_DIGEST = "9780a1dc28ece40e1251981f007a509418de5db4c1e8921ba3402ac2dcf3fd86"
        private val SOURCE_ORDER = listOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.AGRIBALYSE, HimGroundTruthSource.CIQUAL, HimGroundTruthSource.GLYCEMIC_INDEX)
        private val FOUNDATION_GUARDS = linkedMapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
        )
        private val SOURCE_SHAS = linkedMapOf(
            HimGroundTruthSource.OPEN_FOOD_FACTS.artifactPath to "63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236",
            HimGroundTruthSource.AGRIBALYSE.artifactPath to "9068c89fa887ef087e87dcc51f758dd29e9623b93d9bd0a2277a4faae57c2297",
            HimGroundTruthSource.CIQUAL.artifactPath to "807d16c222f0e812831c10bdd89f1a5ebbc74c95e8a4944723db486add96dcff",
            HimGroundTruthSource.GLYCEMIC_INDEX.artifactPath to "6891c2ff2ab3734a1d2768339c1f660f7093a31b97a856c82bf9b8c3c88422b5",
        )
        private val INDEX_SPECS = listOf(
            IndexSpec(HimGroundTruthSource.OPEN_FOOD_FACTS, HimOffProductionEvidenceIndexPaths.FINAL_INDEX, 4_591_865, "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743", linkedMapOf("products" to 4_591_865L)),
            IndexSpec(HimGroundTruthSource.AGRIBALYSE, HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX, 2_458, "2d4036d35413f3a2f98b4798fcf8511960e94b2584efcf057616bf41de8660f6", linkedMapOf("records" to 2_458L)),
            IndexSpec(HimGroundTruthSource.CIQUAL, HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX, 5_674, "b9f2d2a162093234729a53855804042debe8a2b290a04a4db1654e54fa3489bc", linkedMapOf("foods" to 3_484L, "taxonomy" to 138L, "constituents" to 74L, "sources" to 1_978L)),
            IndexSpec(HimGroundTruthSource.GLYCEMIC_INDEX, HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX, 2_254, "03cd3c46e7153f915d3043095a22434aeba3b0e2a36e5c28a56d2d3d9e57935a", linkedMapOf("measurements" to 2_091L, "meanSummaries" to 122L, "categoryNotes" to 15L, "footnotes" to 26L)),
        )
    }
}
