package de.shopme.tools.knowledge.him.canonical.family

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

data class HimCanonicalFamilyFoundationRelease(
    val releaseVersion: String,
    val productOnlyMaster: HimCanonicalFamilyReleaseArtifact,
    val entityIdRegistry: HimCanonicalFamilyReleaseArtifact,
    val canonicalFamilyAuthority: HimCanonicalFamilyReleaseArtifact,
    val fingerprintIndex: HimCanonicalFamilyReleaseArtifact,
    val foundationState: HimCanonicalFamilyFoundationState,
)

data class HimCanonicalFamilyReleaseArtifact(
    val path: String,
    val sha256: String,
    val recordCount: Int,
)

enum class HimCanonicalFamilyFoundationState {
    RELEASED,
}

class HimCanonicalFamilyFoundationReleaseBuilder {

    fun build(): HimCanonicalFamilyFoundationRelease =
        HimCanonicalFamilyFoundationRelease(
            releaseVersion = RELEASE_VERSION,
            productOnlyMaster = artifact(PRODUCT_MASTER_PATH, PRODUCT_MASTER_SHA256),
            entityIdRegistry = artifact(ENTITY_ID_REGISTRY_PATH, ENTITY_ID_REGISTRY_SHA256),
            canonicalFamilyAuthority =
                artifact(CANONICAL_FAMILY_AUTHORITY_PATH, CANONICAL_FAMILY_AUTHORITY_SHA256),
            fingerprintIndex = artifact(FINGERPRINT_INDEX_PATH, FINGERPRINT_INDEX_SHA256),
            foundationState = HimCanonicalFamilyFoundationState.RELEASED,
        )

    private fun artifact(path: String, sha256: String) =
        HimCanonicalFamilyReleaseArtifact(
            path = path,
            sha256 = sha256,
            recordCount = RECORD_COUNT,
        )

    companion object {
        const val RELEASE_VERSION = "F2_V1"
        const val RECORD_COUNT = 1384
        const val PRODUCT_MASTER_PATH = HimCanonicalFamilyPaths.PRODUCT_ONLY_MASTER_PATH
        const val ENTITY_ID_REGISTRY_PATH =
            HimCanonicalFamilyPaths.CANONICAL_FAMILY_MASTER_DIRECTORY + "/" +
                    HimCanonicalFamilyPaths.ENTITY_ID_REGISTRY_FILE_NAME
        const val CANONICAL_FAMILY_AUTHORITY_PATH =
            HimCanonicalFamilyPaths.CANONICAL_FAMILY_MASTER_DIRECTORY + "/" +
                    HimCanonicalFamilyPaths.FAMILY_AUTHORITY_FILE_NAME
        const val FINGERPRINT_INDEX_PATH =
            "data/knowledge/him/canonical-family/index/" +
                    "him-entity-fingerprint-index.v1.json"
        const val RELEASE_RECORD_PATH =
            HimCanonicalFamilyPaths.CANONICAL_FAMILY_MASTER_DIRECTORY + "/" +
                    "canonical-family-foundation-release.v1.json"
        const val PRODUCT_MASTER_SHA256 =
            "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f"
        const val ENTITY_ID_REGISTRY_SHA256 =
            "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a"
        const val CANONICAL_FAMILY_AUTHORITY_SHA256 =
            "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184"
        const val FINGERPRINT_INDEX_SHA256 =
            "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951"
    }
}

class HimCanonicalFamilyFoundationReleaseValidator {

    fun validate(release: HimCanonicalFamilyFoundationRelease) {
        val expected = HimCanonicalFamilyFoundationReleaseBuilder().build()
        require(release == expected) {
            "Canonical Family Foundation Release Record differs from F2 V1."
        }
    }
}

class HimCanonicalFamilyFoundationReleasePersistence(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create(),
) {

    fun serialize(release: HimCanonicalFamilyFoundationRelease): ByteArray =
        (gson.toJson(release) + "\n").toByteArray(Charsets.UTF_8)

    fun read(file: File): HimCanonicalFamilyFoundationRelease {
        require(file.isFile) { "Foundation Release Record is missing: ${file.absolutePath}" }
        return requireNotNull(
            gson.fromJson(file.readText(), HimCanonicalFamilyFoundationRelease::class.java)
        ) { "Foundation Release Record is invalid JSON: ${file.absolutePath}" }
    }

    fun writeNewOrRequireIdentical(file: File, content: ByteArray) {
        if (file.exists()) {
            require(file.readBytes().contentEquals(content)) {
                "Existing Foundation Release Record differs: ${file.absolutePath}"
            }
            return
        }

        HimCanonicalFamilyPersistence().writeNew(file, content)
    }
}
