package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

object HimProductionIndexFileIdentityReleaseContractV1 {
    const val VERSION = "HIM_PRODUCTION_INDEX_FILE_IDENTITY_RELEASE_V1"
    const val STATE = "RELEASED"
    const val DIGEST_CONTRACT = "HIM_PRODUCTION_INDEX_FILE_IDENTITY_RELEASE_DIGEST_V1"
    const val HASH_ALGORITHM = "SHA-256"
    const val PATH = "data/knowledge/him/retrieval/master/him-production-index-file-identity-release.v1.json"
    const val RETRIEVAL_RELEASE_PATH = "data/knowledge/him/retrieval/master/him-retrieval-foundation-release.v1.json"
    const val RETRIEVAL_RELEASE = "F3D_2_RETRIEVAL_FOUNDATION_V1"
    const val RETRIEVAL_RELEASE_SHA256 = "b4814fbd8141c43b04b97bd0f87c5e01d5128b7143ff74b80ed873a8f0068023"
    const val RETRIEVAL_FOUNDATION_DIGEST = "9780a1dc28ece40e1251981f007a509418de5db4c1e8921ba3402ac2dcf3fd86"
    const val INDEX_FILE_IDENTITY_CONTRACT = "HIM_PRODUCTION_SQLITE_FILE_SHA256_V1"

    val sourceOrder = listOf(
        HimGroundTruthSource.OPEN_FOOD_FACTS,
        HimGroundTruthSource.AGRIBALYSE,
        HimGroundTruthSource.CIQUAL,
        HimGroundTruthSource.GLYCEMIC_INDEX,
    )

    val sources = listOf(
        expected(HimGroundTruthSource.OPEN_FOOD_FACTS, "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", "63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236", "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", "80c7da8c2b0a94ee0b12fb03e095f50429d2c0b300ce70dc22d0b5095aabf5df", 25_551_749_120, "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743", 4_591_865),
        expected(HimGroundTruthSource.AGRIBALYSE, "data/sources/agribalyse/optimized/agribalyse-him-final-source.jsonl.gz", "9068c89fa887ef087e87dcc51f758dd29e9623b93d9bd0a2277a4faae57c2297", "data/sources/agribalyse/index/agribalyse-him-evidence-index.v1.sqlite", "ce45823f93e02257e5f251e623ffb595d3b6ee93e15a5d2163b637da15645557", 4_087_808, "2d4036d35413f3a2f98b4798fcf8511960e94b2584efcf057616bf41de8660f6", 2_458),
        expected(HimGroundTruthSource.CIQUAL, "data/sources/ciqual/optimized/ciqual-him-final-source.json.gz", "807d16c222f0e812831c10bdd89f1a5ebbc74c95e8a4944723db486add96dcff", "data/sources/ciqual/index/ciqual-him-evidence-index.v1.sqlite", "6acedb365098d95395a364e54f4cd471051bbf7c47f99a9c1c65875732733aec", 80_211_968, "b9f2d2a162093234729a53855804042debe8a2b290a04a4db1654e54fa3489bc", 5_674),
        expected(HimGroundTruthSource.GLYCEMIC_INDEX, "data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz", "6891c2ff2ab3734a1d2768339c1f660f7093a31b97a856c82bf9b8c3c88422b5", "data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite", "4195a90d51a021507bfed349ee66bcf6d50e92aaf3ecb5b4805521fec6dc3bbe", 3_305_472, "03cd3c46e7153f915d3043095a22434aeba3b0e2a36e5c28a56d2d3d9e57935a", 2_254),
    )

    private fun expected(source: HimGroundTruthSource, sourcePath: String, sourceSha: String, indexPath: String, sqliteSha: String, bytes: Long, logicalDigest: String, rows: Long) =
        HimProductionIndexFileIdentitySourceV1(source.name, sourcePath, sourceSha, indexPath, sqliteSha, bytes, logicalDigest,
            HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION, HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION,
            HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION, rows, rows)
}

data class HimProductionIndexFileIdentitySourceV1(
    val source: String,
    val optimizedSourcePath: String,
    val optimizedSourceSha256: String,
    val indexPath: String,
    val sqliteFileSha256: String,
    val sqliteFileBytes: Long,
    val logicalIndexDigest: String,
    val schemaVersion: String,
    val buildPolicyVersion: String,
    val projectionPolicyVersion: String,
    val evidenceRows: Long,
    val ftsRows: Long,
)

data class HimProductionIndexFileIdentityReleaseV1(
    val releaseVersion: String,
    val state: String,
    val retrievalFoundationRelease: String,
    val retrievalFoundationReleaseSha256: String,
    val retrievalFoundationDigest: String,
    val indexFileIdentityContract: String,
    val hashAlgorithm: String,
    val digestContract: String,
    val canonicalSourceOrder: List<String>,
    val sources: List<HimProductionIndexFileIdentitySourceV1>,
    val logicalDigestSha256: String,
)

object HimProductionIndexFileIdentityReleasePersistenceV1 {
    private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    private val sha256Pattern = Regex("[0-9a-f]{64}")

    fun expected(): HimProductionIndexFileIdentityReleaseV1 {
        val unsigned = HimProductionIndexFileIdentityReleaseV1(
            HimProductionIndexFileIdentityReleaseContractV1.VERSION,
            HimProductionIndexFileIdentityReleaseContractV1.STATE,
            HimProductionIndexFileIdentityReleaseContractV1.RETRIEVAL_RELEASE,
            HimProductionIndexFileIdentityReleaseContractV1.RETRIEVAL_RELEASE_SHA256,
            HimProductionIndexFileIdentityReleaseContractV1.RETRIEVAL_FOUNDATION_DIGEST,
            HimProductionIndexFileIdentityReleaseContractV1.INDEX_FILE_IDENTITY_CONTRACT,
            HimProductionIndexFileIdentityReleaseContractV1.HASH_ALGORITHM,
            HimProductionIndexFileIdentityReleaseContractV1.DIGEST_CONTRACT,
            HimProductionIndexFileIdentityReleaseContractV1.sourceOrder.map { it.name },
            HimProductionIndexFileIdentityReleaseContractV1.sources,
            "",
        )
        return unsigned.copy(logicalDigestSha256 = logicalDigest(unsigned))
    }

    fun serialize(release: HimProductionIndexFileIdentityReleaseV1): ByteArray =
        (gson.toJson(release) + "\n").toByteArray(Charsets.UTF_8)

    fun read(file: File): HimProductionIndexFileIdentityReleaseV1 =
        gson.fromJson(file.readText(Charsets.UTF_8), HimProductionIndexFileIdentityReleaseV1::class.java)

    fun validate(release: HimProductionIndexFileIdentityReleaseV1, actualIndexSizes: Map<String, Long>) {
        require(release == expected()) { "Production index file identity release differs from frozen V1 content" }
        require(sha256Pattern.matches(release.logicalDigestSha256))
        require(release.logicalDigestSha256 == logicalDigest(release.copy(logicalDigestSha256 = "")))
        require(actualIndexSizes.keys == release.sources.map { it.indexPath }.toSet())
        release.sources.forEach { source ->
            require(sha256Pattern.matches(source.optimizedSourceSha256))
            require(sha256Pattern.matches(source.sqliteFileSha256))
            require(sha256Pattern.matches(source.logicalIndexDigest))
            require(actualIndexSizes[source.indexPath] == source.sqliteFileBytes) { "Production index size differs: ${source.indexPath}" }
        }
    }

    fun publishOrReuse(file: File, release: HimProductionIndexFileIdentityReleaseV1): Boolean {
        val expectedBytes = serialize(release)
        if (file.exists()) {
            require(file.isFile && file.readBytes().contentEquals(expectedBytes)) { "Existing immutable production index identity release differs" }
            return false
        }
        requireNotNull(file.parentFile).mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.publishing")
        require(!temporary.exists()) { "Stale publication temporary exists: ${temporary.path}" }
        try {
            Files.write(temporary.toPath(), expectedBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } finally {
            if (temporary.exists()) require(temporary.delete())
        }
        require(file.readBytes().contentEquals(expectedBytes))
        return true
    }

    fun logicalDigest(release: HimProductionIndexFileIdentityReleaseV1): String {
        val canonical = buildString {
            appendLine("digest-contract=${release.digestContract}")
            appendLine("release-version=${release.releaseVersion}")
            appendLine("state=${release.state}")
            appendLine("retrieval-foundation-release=${release.retrievalFoundationRelease}")
            appendLine("retrieval-foundation-release-sha256=${release.retrievalFoundationReleaseSha256}")
            appendLine("retrieval-foundation-digest=${release.retrievalFoundationDigest}")
            appendLine("index-file-identity-contract=${release.indexFileIdentityContract}")
            appendLine("hash-algorithm=${release.hashAlgorithm}")
            appendLine("canonical-source-order=${release.canonicalSourceOrder.joinToString(",")}")
            release.sources.forEach { source ->
                appendLine("source=${source.source}")
                appendLine("optimized-source-path=${source.optimizedSourcePath}")
                appendLine("optimized-source-sha256=${source.optimizedSourceSha256}")
                appendLine("index-path=${source.indexPath}")
                appendLine("sqlite-file-sha256=${source.sqliteFileSha256}")
                appendLine("sqlite-file-bytes=${source.sqliteFileBytes}")
                appendLine("logical-index-digest=${source.logicalIndexDigest}")
                appendLine("schema-version=${source.schemaVersion}")
                appendLine("build-policy-version=${source.buildPolicyVersion}")
                appendLine("projection-policy-version=${source.projectionPolicyVersion}")
                appendLine("evidence-rows=${source.evidenceRows}")
                appendLine("fts-rows=${source.ftsRows}")
            }
        }
        return sha256(canonical.toByteArray(Charsets.UTF_8))
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
